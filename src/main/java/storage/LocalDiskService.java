package storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import hashing.HashUtil;

public class LocalDiskService {
    private final Path rootStorageDir;
    private static final String META_EXT = ".meta";

    public LocalDiskService(Path rootStorageDir) {
        this.rootStorageDir = rootStorageDir;
        try {
            Files.createDirectories(rootStorageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize storage root directory", e);
        }
    }

    private Path resolveSafePath(String key) {
        String safeFileName = HashUtil.hash(key);
        return rootStorageDir.resolve(safeFileName);
    }

    private Path resolveMetaPath(String key) {
        String safeFileName = HashUtil.hash(key);
        return rootStorageDir.resolve(safeFileName + META_EXT);
    }

    public void put(String key, byte[] data) {
        Path targetPath = resolveSafePath(key);
        Path metaPath = resolveMetaPath(key);
        try {
            Files.writeString(metaPath, key, StandardCharsets.UTF_8);
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
        Path targetPath = resolveSafePath(key);
        Path metaPath = resolveMetaPath(key);
        try {
            Files.deleteIfExists(targetPath);
            Files.deleteIfExists(metaPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete key [" + key + "] from disk", e);
        }
    }

    public boolean exists(String key) {
        Path targetPath = resolveSafePath(key);
        return Files.exists(targetPath);
    }

    public List<String> listStoredKeys() {
        List<String> keys = new ArrayList<>();
    
        try (Stream<Path> stream = Files.list(rootStorageDir)) {
            stream.filter(path -> path.toString().endsWith(META_EXT))
                  .forEach(metaPath -> {
                      try {
                          String originalKey = Files.readString(metaPath, StandardCharsets.UTF_8).trim();
                          if (!originalKey.isEmpty()) {
                              keys.add(originalKey);
                          }
                      } catch (IOException e) {
                          System.err.println("[LocalDiskService] Corrupt metadata file skipped: " + metaPath);
                      }
                  });
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan disk directory for metadata keys", e);
        }
        return keys;
    }
    
}
