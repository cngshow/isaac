package gov.vha.isaac.ochre.api.collections;

import org.apache.mahout.math.map.OpenObjectIntHashMap;

import java.util.OptionalInt;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.ObjIntConsumer;
import org.apache.mahout.math.function.ObjectIntProcedure;

/**
 * Created by kec on 12/18/14.
 * @param <T> Type of object in map. 
 */
public class ConcurrentObjectIntMap<T> {
    
	
	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock read = rwl.readLock();
	private final Lock write = rwl.writeLock();

    OpenObjectIntHashMap<T> backingMap = new OpenObjectIntHashMap<>();

    public void forEachPair(ObjIntConsumer<T> consumer) {
        backingMap.forEachPair((T first, int second) -> {
            consumer.accept(first, second);
            return true;
        });
        
    }

    public boolean containsKey(T key) {
        try {
        	read.lock();
            return backingMap.containsKey(key);
        } finally {
        	if (read != null)
        		read.unlock();
        }
    }

    public OptionalInt get(T key) {
        try {
        	read.lock();
            if (backingMap.containsKey(key)) {
                return OptionalInt.of(backingMap.get(key));
            }
            return OptionalInt.empty();
        } finally {
        	if (read != null)
        		read.unlock();
        }
    }

    public boolean put(T key, int value) {
        try {
        	write.lock();
            return backingMap.put(key, value);
        } finally {
        	if (write != null)
        		write.unlock();
        }
    }

    public int size() {
        return backingMap.size();
    }

}
