package replication;

import java.util.List;

import cluster.ClusterManager;
import common.Node;
import network.TcpRequestClient;

public class ReplicationManager {
    private final ClusterManager clusterManager;
    private final TcpRequestClient tcpRequestClient;
    private final int replicationFactor;

    public ReplicationManager(ClusterManager clusterManager, 
                              TcpRequestClient tcpRequestClient, 
                              int replicationFactor) {
        this.clusterManager = clusterManager;
        this.tcpRequestClient = tcpRequestClient;
        this.replicationFactor = replicationFactor;
    }

    public int  replicate(String key, byte[] data) {
       
        List<Node> replicaNodes = clusterManager.getReplicaNodes(key, replicationFactor);

        
        if (replicaNodes == null ||replicaNodes.size() <= 1) {
            System.out.println("[ReplicationManager] No backup replica nodes available for key: " + key);
            return 0;
        }

        int successCount = 0;
        int targetsAttempted = 0;

 
        for (int i = 1; i < replicaNodes.size(); i++) {
            Node targetNode = replicaNodes.get(i);
            targetsAttempted++;

            System.out.println("[ReplicationManager] Attempting backup replication to node: " + targetNode + " for key: " + key);
            boolean success = tcpRequestClient.put(targetNode, key, data);

            if (success) {
                successCount++;
            } else {
                System.err.println("[ReplicationManager] Failed to replicate key [" + key + "] to node: " + targetNode);
            }
        }

        System.out.println("[ReplicationManager] Replication complete for key [" + key + "]. " +
                           "Successes: " + successCount + "/" + targetsAttempted);
        
        return successCount;
    }
}