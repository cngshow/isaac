/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.vha.isaac.ochre.model.waitfree;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import gov.vha.isaac.ochre.api.externalizable.ByteArrayDataBuffer;
import gov.vha.isaac.ochre.model.ObjectChronologyImpl;
import gov.vha.isaac.ochre.model.WaitFreeComparable;
import gov.vha.isaac.ochre.model.waitfree.WaitFreeMergeSerializer;

/**
 *
 * @author kec
 * @author darmbrust
 * @param <T>
 */
public class MVCasSequenceObjectMap<T extends WaitFreeComparable>
{
	private static final int WRITE_SEQUENCES = 64;

	private static final AtomicIntegerArray writeSequences = new AtomicIntegerArray(WRITE_SEQUENCES);

	private static int getWriteSequence(int componentSequence)
	{
		return writeSequences.incrementAndGet(componentSequence % WRITE_SEQUENCES);
	}

	ReentrantLock expandLock = new ReentrantLock();

	private final String filePrefix;
	private final String fileSuffix;
	private final Path dbFolderPath;
	WaitFreeMergeSerializer<T> elementSerializer;
	MVStore s;
	MVMap<Integer, byte[]> m;

	public MVCasSequenceObjectMap(WaitFreeMergeSerializer<T> elementSerializer, Path dbFolderPath, String filePrefix, String fileSuffix)
	{
		this.elementSerializer = elementSerializer;
		this.dbFolderPath = dbFolderPath;
		this.filePrefix = filePrefix;
		this.fileSuffix = fileSuffix;
		//TODO clean this mess up
		File f = new File(dbFolderPath.toAbsolutePath().toString() + filePrefix + "_MVStore_" + fileSuffix + "junk");
		f.delete();
		s = MVStore.open(f.toString());
		s.setCacheSize(500);
		s.setVersionsToKeep(0);
		m = s.<Integer, byte[]> openMap("map");
	}

	/**
	 * Read from disk
	 *
	 */
	public void initialize()
	{
		if (s != null)
		{
			s.close();
		}
		s = MVStore.open(dbFolderPath.toAbsolutePath().toString() + filePrefix + "_MVStore_" + fileSuffix);
		m = s.<Integer, byte[]> openMap("map");
	}

	public void write()
	{
		s.commit();
	}

	public Stream<T> getStream()
	{
		return m.keySet().stream().map(sequence -> getQuick(sequence));
	}

	public Stream<T> getParallelStream()
	{
		return m.keySet().parallelStream().map(sequence -> getQuick(sequence));
	}

	/**
	 * Provides no range or null checking. For use with a stream that already
	 * filters out null values and out of range sequences.
	 *
	 * @param sequence
	 * @return
	 */
	public T getQuick(int sequence)
	{

		ByteArrayDataBuffer buff = new ByteArrayDataBuffer(m.get(sequence));
		return elementSerializer.deserialize(buff);
	}

	public int getSize()
	{
		return m.size();
	}

	public boolean containsKey(int sequence)
	{
		return m.containsKey(sequence);
	}

	public Optional<T> get(int sequence)
	{

		byte[] data = m.get(sequence);
		if (data == null)
		{
			return Optional.empty();
		}

		ByteArrayDataBuffer buff = new ByteArrayDataBuffer(data);
		return Optional.of(elementSerializer.deserialize(buff));
	}

	public boolean put(int sequence, @NotNull T value)
	{
		T incomingValue = value;
		
		boolean replaced = false;
		
		while (!replaced)
		{
			byte[] oldValue = m.get(sequence);
			
			int oldWriteSequence = oldValue == null ? value.getWriteSequence() : getWriteSequence(oldValue);
			if (oldWriteSequence != value.getWriteSequence())
			{
				ByteArrayDataBuffer oldDataBuffer = new ByteArrayDataBuffer(oldValue);
				T oldObject = elementSerializer.deserialize(oldDataBuffer);
				value = elementSerializer.merge(value, oldObject, oldWriteSequence);
			}
			value.setWriteSequence(getWriteSequence(sequence));
			
			ByteArrayDataBuffer newDataBuffer = new ByteArrayDataBuffer(oldValue == null ? 512 : oldValue.length + 512);
			elementSerializer.serialize(newDataBuffer, value);
			newDataBuffer.trimToSize();
			byte[] dataToStore = newDataBuffer.getData();
			
			replaced = m.replace(sequence, oldValue, dataToStore);
			if (replaced)
			{
				if (incomingValue != value && value instanceof ObjectChronologyImpl)
				{
					@SuppressWarnings("rawtypes")
					ObjectChronologyImpl objc = (ObjectChronologyImpl) incomingValue;
					objc.setWrittenData(dataToStore);
					objc.setWriteSequence(value.getWriteSequence());
				}
			}
		}
		
		return true;
	}

	private int getWriteSequence(byte[] data)
	{
		return (((data[0]) << 24) | ((data[1] & 0xff) << 16) | ((data[2] & 0xff) << 8) | ((data[3] & 0xff)));
	}
}
