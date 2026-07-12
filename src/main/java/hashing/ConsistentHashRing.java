
package hashing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import common.Constants;
import common.Node;

public class ConsistentHashRing {

    private final ConcurrentSkipListMap<String, Node> ring = new ConcurrentSkipListMap<>();

    public void addNode(Node node) {
        for (int i = 0; i < Constants.VIRTUAL_NODE_COUNT; i++) {
            ring.putIfAbsent(virtualNodeHash(node, i), node);
        }
    }

    public void removeNode(Node node) {

        for (int i = 0; i < Constants.VIRTUAL_NODE_COUNT; i++) {
            ring.remove(virtualNodeHash(node, i));
        }
    }

    public Node getOwnerNode(String key) {
        if (ring.isEmpty())
            return null;

        String keyHash = HashUtil.hash(key);
        SortedMap<String, Node> tailMap = ring.tailMap(keyHash);
        String targetHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(targetHash);

    }

    public int size() {
        return ring.size();
    }

    public List<Node> getReplicaNodes(String key, int replicationFactor) {
        Set<Node> replicas = new LinkedHashSet<>();

        if (ring.isEmpty() || replicationFactor <= 0) {
            return Collections.emptyList();
        }

        int actualFactor = Math.min(replicationFactor,
                getPhysicalNodeCount());
        String keyHash = HashUtil.hash(key);

        SortedMap<String, Node> tailMap = ring.tailMap(keyHash);

        for (Node node : tailMap.values()) {
            replicas.add(node);
            if (replicas.size() == actualFactor) {
                return new ArrayList<>(replicas);
            }
        }

        for (Node node : ring.values()) {
            replicas.add(node);
            if (replicas.size() == actualFactor) {
                break;
            }
        }

        return new ArrayList<>(replicas);
    }

    private String virtualNodeHash(Node node, int virtualIndex) {
        return HashUtil.hash(
                node.host()
                        + ":"
                        + node.tcpPort()
                        + ":VN:"
                        + virtualIndex);
    }

    private int getPhysicalNodeCount() {
        return new LinkedHashSet<>(ring.values()).size();
    }

}
