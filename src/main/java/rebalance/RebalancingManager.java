package rebalance;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cluster.ClusterManager;
import common.Node;
import network.TcpRequestClient;
import storage.StorageManager;

public class RebalancingManager {
    private final Node self;
    private final ClusterManager clusterManager;
    private final StorageManager storageManager;
    private final TcpRequestClient tcpRequestClient;
    private final Queue<String> retryQueue = new ConcurrentLinkedQueue<>();

    public RebalancingManager(Node self,
            ClusterManager clusterManager,
            StorageManager storageManager,
            TcpRequestClient tcpRequestClient) {
        this.self = self;
        this.clusterManager = clusterManager;
        this.storageManager = storageManager;
        this.tcpRequestClient = tcpRequestClient;
    }

    public void rebalance() {
        System.out.println("[RebalancingManager] Starting local cluster rebalancing scan...");

        List<String> localKeys = storageManager.listStoredKeys();

        int examined = 0;
        int migrated = 0;
        int failed = 0;
        int skipped = 0;

        for (String key : localKeys) {
            examined++;
            System.out.println("[RebalancingManager] Checking key: " + key);

            Node correctOwner = clusterManager.routeKey(key);
            if (correctOwner == null) {
                System.err.println("[RebalancingManager] No owner found for key: " + key);
                failed++;
                continue;
            }

            System.out.println("[RebalancingManager] Current owner: " + self + " New owner: " + correctOwner);
            if (self.equals(correctOwner)) {
                skipped++;
                continue;
            }

            byte[] fileData = storageManager.get(key);
            if (fileData == null) {
                System.err.println("[RebalancingManager] Error: Key data missing on disk while reading: " + key);
                failed++;
                continue;
            }

            boolean migrationSuccess = tcpRequestClient.put(correctOwner, key, fileData);

            if (migrationSuccess) {
                storageManager.delete(key);
                migrated++;
                System.out.println("[RebalancingManager] Key successfully migrated and purged locally: " + key);
            } else {
                failed++;
                retryQueue.offer(key);
                System.err.println("[RebalancingManager] Migration failed over network link to node: " + correctOwner);

            }
        }

        System.out.println("[RebalancingManager] Rebalancing execution completed.");
        System.out.println("===========================================");
        System.out.println("[RebalancingManager] Rebalance Completed");
        System.out.println(String.format("Examined : %-5d", examined));
        System.out.println(String.format("Migrated : %-5d", migrated));
        System.out.println(String.format("Skipped  : %-5d", skipped));
        System.out.println(String.format("Failed   : %-5d", failed));
        System.out.println("===========================================");

    }

    public void retryFailedMigrations() {

    if (retryQueue.isEmpty()) {
        return;
    }

    System.out.println(
            "[RebalancingManager] Retrying failed migrations...");

    int retryCount = retryQueue.size();

    for (int i = 0; i < retryCount; i++) {

        String key = retryQueue.poll();

        if (key == null) {
            continue;
        }

        Node correctOwner = clusterManager.routeKey(key);

        if (correctOwner == null || correctOwner.equals(self)) {
            continue;
        }

        byte[] data = storageManager.get(key);

        if (data == null) {
            continue;
        }

        boolean success =
                tcpRequestClient.put(correctOwner, key, data);

        if (success) {

            storageManager.delete(key);

            System.out.println(
                    "[RebalancingManager] Retry succeeded for key: " + key);

        } else {

            retryQueue.offer(key);

            System.err.println(
                    "[RebalancingManager] Retry failed again for key: " + key);
        }
    }
}
}
