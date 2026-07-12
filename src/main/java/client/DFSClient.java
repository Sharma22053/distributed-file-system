package client;

import java.nio.charset.StandardCharsets;
import storage.DistributedStorageService;

public class DFSClient {
    private final DistributedStorageService storageService;

    public DFSClient(DistributedStorageService storageService) {
        this.storageService = storageService;
    }

    public boolean putFile(String key, String content) {
        if (content == null || key == null || key.trim().isEmpty()) {
            return false;
        }
        byte[] dataBytes = content.getBytes(StandardCharsets.UTF_8);
        return storageService.put(key, dataBytes);
    }

    public String getFile(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        byte[] dataBytes = storageService.get(key);
        if (dataBytes == null) {
            return null;
        }
        return new String(dataBytes, StandardCharsets.UTF_8);
    }

    public boolean deleteFile(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        return storageService.delete(key);
    }

    public boolean fileExists(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        return storageService.exists(key);
    }
}
