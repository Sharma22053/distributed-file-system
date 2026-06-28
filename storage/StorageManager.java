package storage;

public class StorageManager {
    private final LocalDiskService localDiskService;
    private final FileLockManager fileLockManager;

    public StorageManager(LocalDiskService localDiskService, FileLockManager fileLockManager) {
        this.localDiskService = localDiskService;
        this.fileLockManager = fileLockManager;
    }

    public void put(String key,byte[] data){
        var lock = fileLockManager.writeLock(key);
        lock.lock();
        try{
            localDiskService.put(key, data);
            System.out.println("[StorageManager] Stored key: " + key);
        } finally{
            lock.unlock();
        }
    }

    public byte[] get(String key){
        var lock = fileLockManager.readLock(key);
        lock.lock();
        try{
            return localDiskService.get(key);
        } finally{
            lock.unlock();
        }
    }

    public void delete(String key){
        var lock = fileLockManager.writeLock(key);
        lock.lock();
        try{
            localDiskService.delete(key);
             System.out.println("[StorageManager] Deleted key: " + key);
        } finally{
            lock.unlock();
        }
    }

    public boolean exists(String key){
        var lock = fileLockManager.readLock(key);
        lock.lock();
        try{
            return localDiskService.exists(key);
        } finally{
            lock.unlock();
        }
    }
}
