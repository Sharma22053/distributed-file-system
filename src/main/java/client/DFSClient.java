package client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cluster.ClusterManager;
import common.Node;
import hashing.ConsistentHashRing;
import storage.DistributedStorageService;
import storage.StorageManager;

public class DFSClient {
    private final DistributedStorageService storageService;
    private final ClusterManager clusterManager;
    private final StorageManager storageManager;
    private final ConsistentHashRing hashRing;

    public DFSClient(DistributedStorageService storageService,
            ClusterManager clusterManager,
            StorageManager storageManager,
            ConsistentHashRing hashRing) {

        this.storageService = storageService;
        this.clusterManager = clusterManager;
        this.storageManager = storageManager;
        this.hashRing = hashRing;
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

    public List<String> getActiveNodes() {
        return clusterManager.getAllNodes()
                .stream()
                .map(node -> node.host() + ":" + node.tcpPort())
                .toList();
    }

    public int getClusterSize() {
        return clusterManager.size();
    }

    public String getPrimaryOwner(String key) {

        Node owner = clusterManager.routeKey(key);

        if (owner == null) {
            return "N/A";
        }

        return owner.host() + ":" + owner.tcpPort();
    }

    public List<String> getReplicas(String key) {

        return clusterManager
                .getReplicaNodes(key, 3)
                .stream()
                .skip(1)
                .map(node -> node.host() + ":" + node.tcpPort())
                .toList();
    }

    public List<String> listLocalKeys() {
        return storageManager.listStoredKeys();
    }

    public List<String> getRingEntries() {

        List<String> entries = new ArrayList<>();

        for (Map.Entry<String, Node> entry : hashRing.getRingSnapshot().entrySet()) {

            String hash = entry.getKey().substring(0, 8);
            Node node = entry.getValue();
            entries.add(hash + " -> " + node.tcpPort());
        }

        return entries;
    }
}
