/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.vha.isaac.ochre.commit.manager;

import gov.vha.isaac.ochre.api.collections.NativeIntIntHashMap;
import gov.vha.isaac.ochre.api.externalizable.StampAlias;
import gov.vha.isaac.ochre.api.externalizable.StampComment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Spliterator;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.SIZED;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.mahout.math.list.IntArrayList;

/**
 *
 * @author kec
 */
public class StampAliasMap {

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock read = rwl.readLock();
	private final Lock write = rwl.writeLock();

    NativeIntIntHashMap stampAliasMap = new NativeIntIntHashMap();
    NativeIntIntHashMap aliasStampMap = new NativeIntIntHashMap();

    public int getSize() {
        assert stampAliasMap.size() == aliasStampMap.size() : "stampAliasMap.size() = "
                + stampAliasMap.size() + " aliasStampMap.size() = " + aliasStampMap.size();
        return aliasStampMap.size();
    }

    public void addAlias(int stamp, int alias) {
        try {
        	write.lock();
            if (!stampAliasMap.containsKey(stamp)) {
                stampAliasMap.put(stamp, alias);
                aliasStampMap.put(alias, stamp);
            } else if (stampAliasMap.get(stamp) == alias) {
                // already added...
            } else {
                // add an additional alias
                aliasStampMap.put(alias, stamp);
            }
        } finally {
        	if (write != null)
        		write.unlock();
        }
    }

    /**
     * 
     * @param stamp
     * @return array of unique aliases, which do not include the stamp itself. 
     */
    public int[] getAliases(int stamp) {
        try {
        	read.lock();
            IntStream.Builder builder = IntStream.builder();
            getAliasesForward(stamp, builder);
            getAliasesReverse(stamp, builder);
            return builder.build().distinct().toArray();
        } finally {
        	if (read != null)
        		read.unlock();
        }
    }

    private void getAliasesForward(int stamp, IntStream.Builder builder) {
        if (stampAliasMap.containsKey(stamp)) {
            int alias = stampAliasMap.get(stamp);
            builder.add(alias);
            getAliasesForward(alias, builder);
        }
    }
    private void getAliasesReverse(int stamp, IntStream.Builder builder) {
        if (aliasStampMap.containsKey(stamp)) {
            int alias = aliasStampMap.get(stamp);
            builder.add(alias);
            getAliasesReverse(alias, builder);
        }
    }

    public void write(File mapFile) throws IOException {
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mapFile)))) {
            output.writeInt(stampAliasMap.size());
            stampAliasMap.forEachPair((int stampSequence, int aliasSequence) -> {
                try {
                    output.writeInt(stampSequence);
                    output.writeInt(aliasSequence);
                    return true;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            output.writeInt(aliasStampMap.size());
            aliasStampMap.forEachPair((int aliasSequence, int stampSequence) -> {
                try {
                    output.writeInt(aliasSequence);
                    output.writeInt(stampSequence);
                    return true;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
    }

    public void read(File mapFile) throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(mapFile)))) {
            int size = input.readInt();
            stampAliasMap.ensureCapacity(size);
            for (int i = 0; i < size; i++) {
                stampAliasMap.put(input.readInt(), input.readInt());
            }
            size = input.readInt();
            aliasStampMap.ensureCapacity(size);
            for (int i = 0; i < size; i++) {
                aliasStampMap.put(input.readInt(), input.readInt());
            }

        }
    }
    private class StampAliasSpliterator extends IndexedStampSequenceSpliterator<StampAlias> {

        public StampAliasSpliterator() {
            super(aliasStampMap.keys());
        }

        @Override
        public boolean tryAdvance(Consumer<? super StampAlias> action) {
            if (getIterator().hasNext()) {
                int alias = getIterator().nextInt();
                StampAlias stampAlias = new StampAlias(aliasStampMap.get(alias), alias);
                action.accept(stampAlias);
                return true;
            }
            return false;
        }

        
    }
   public Stream<StampAlias> getStampAliasStream() {
        return StreamSupport.stream(new StampAliasSpliterator(), false);
    }

}
