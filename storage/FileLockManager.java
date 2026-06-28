package storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileLockManager {
    private final ConcurrentHashMap<String,ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();
    private ReentrantReadWriteLock getLock(String key){
        return locks.computeIfAbsent(key, k-> new ReentrantReadWriteLock());
    }
    public ReentrantReadWriteLock.ReadLock readLock(String key){
        return getLock(key).readLock();
    }
    public ReentrantReadWriteLock.WriteLock writeLock(String key){
        return getLock(key).writeLock();
    }

}
