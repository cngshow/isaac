package gov.vha.isaac.ochre.api.collections;

import org.apache.mahout.math.function.IntObjectProcedure;
import org.apache.mahout.math.map.OpenIntObjectHashMap;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import gov.vha.isaac.ochre.api.DataSerializer;

/**
 * Created by kec on 12/18/14.
 */
public class ConcurrentIntObjectMap<T> {
	
	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock read = rwl.readLock();
	private final Lock write = rwl.writeLock();

    OpenIntObjectHashMap<byte[]> map = new OpenIntObjectHashMap<>();

    DataSerializer<T> serializer;

    private boolean changed = false;

    public ConcurrentIntObjectMap(DataSerializer<T> serializer) {
        this.serializer = serializer;
    }

    public boolean containsKey(int key) {
        try {
        	read.lock();
            return map.containsKey(key);
        } finally {
        	if (read != null)
        		read.unlock();
        }
    }

    public Optional<T> get(int key) {
        byte[] data;
        try {
        	read.lock();
            data = map.get(key);
        } finally {
            if (read != null)
            	read.unlock();
        }
        if (data == null) {
            return Optional.empty();
        }
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            return Optional.of(serializer.deserialize(dis));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean put(int key, T value) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            serializer.serialize(new DataOutputStream(baos), value);
            changed = true;
            
            try {
            	write.lock();
                return map.put(key, baos.toByteArray());
            } finally {
            	if (write != null)
            		write.unlock();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int size() {
        return map.size();
    }

    public boolean forEachPair(IntObjectProcedure<T> procedure) {

        map.forEachPair((int first, byte[] data) -> {
            try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
                return procedure.apply(first, serializer.deserialize(dis));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return true;
    }

}
