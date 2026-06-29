package storage;

import cluster.ClusterManager;
import common.Node;

import network.TcpRequestClient;
import replication.ReplicationManager;

public class DistributedStorageService {
    private final Node self;
    private final ClusterManager clusterManager;
    private final StorageManager storageManager;
    private final ReplicationManager replicationManager;
    private final TcpRequestClient tcpRequestClient;

    public DistributedStorageService(Node self, 
                                     ClusterManager clusterManager, 
                                     StorageManager storageManager, 
                                     ReplicationManager replicationManager, 
                                     TcpRequestClient tcpRequestClient) {
        this.self = self;
        this.clusterManager = clusterManager;
        this.storageManager = storageManager;
        this.replicationManager = replicationManager;
        this.tcpRequestClient = tcpRequestClient;
    }


    public boolean put(String key, byte[] data) {
        Node primaryOwner = getOwner(key);

        if (isLocalOwner(primaryOwner)) {
           
            System.out.println("[DistributedStorage] Storing locally (Primary): " + key);
            storageManager.put(key, data);
            
            
            replicationManager.replicate(key, data);
            return true;
        } else {
           
            System.out.println("[DistributedStorage] Forwarding PUT to remote owner " + primaryOwner + " for key: " + key);
            return tcpRequestClient.put(primaryOwner, key, data);
        }
    }

    public byte[] get(String key) {
        Node primaryOwner = getOwner(key);

        if (isLocalOwner(primaryOwner)) {
            System.out.println("[DistributedStorage] Serving GET locally for key: " + key);
            return storageManager.get(key);
        } else {
            System.out.println("[DistributedStorage] Fetching GET from remote owner " + primaryOwner + " for key: " + key);
            return tcpRequestClient.get(primaryOwner, key);
        }
    }

    public boolean delete(String key) {
        Node primaryOwner = getOwner(key);

        if (isLocalOwner(primaryOwner)) {
            System.out.println("[DistributedStorage] Deleting locally (Primary): " + key);
            storageManager.delete(key);
            
            return true;
        } else {
            System.out.println("[DistributedStorage] Forwarding DELETE to remote owner " + primaryOwner + " for key: " + key);
            return tcpRequestClient.delete(primaryOwner, key);
        }
    }

    public boolean exists(String key) {
        Node primaryOwner = getOwner(key);

        if (isLocalOwner(primaryOwner)) {
            return storageManager.exists(key);
        } else {
            return tcpRequestClient.exists(primaryOwner, key);
        }
    }

    private Node getOwner(String key) {
        Node owner = clusterManager.routeKey(key);
        if (owner == null) {
            throw new IllegalStateException("CRITICAL: Hash ring is empty! No nodes available to route key: " + key);
        }
        return owner;
    }

    private boolean isLocalOwner(Node owner) {
        return self.equals(owner);
    }
}
