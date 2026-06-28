package storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import hashing.HashUtil;

public class LocalDiskService {
    private final Path rootStorageDir;

    public LocalDiskService(Path rootStorageDir) {
        this.rootStorageDir = rootStorageDir;
    }

    private Path resolveSafePath(String key) {
        String safeFileName = HashUtil.hash(key);
        return rootStorageDir.resolve(safeFileName);
    }

    public void put(String key, byte[] data) {
        Path targetPath = resolveSafePath(key);
        try {
            Files.write(targetPath, data);
        } catch (IOException e) {
            throw new RuntimeException("failed to write key [" + key + "] to disk" + e);
        }

    }

    public byte[] get(String key) {
        Path targetPath = resolveSafePath(key);
        try {
            if (!Files.exists(targetPath)) {
                return null;
            }
            return Files.readAllBytes(targetPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read key [" + key + "] from disk", e);
        }
    }

    public void delete(String key) {
        Path targePath = resolveSafePath(key);
        try {
            Files.deleteIfExists(targePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete key [" + key + "] from disk", e);
        }
    }

    public boolean exists(String key) {
        Path targePath = resolveSafePath(key);
        return Files.exists(targePath);
    }
}
