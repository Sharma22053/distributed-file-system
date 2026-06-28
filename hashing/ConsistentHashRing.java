/**
 * Returns the node responsible for the given key
 * according to the consistent hash ring.
 */
package hashing;

import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import common.Node;

public class ConsistentHashRing {
    public ConsistentHashRing() {
    }

    // The ring maps a hash position(String) to a Node name(String)
    private final ConcurrentSkipListMap<String, Node> ring = new ConcurrentSkipListMap<>();

    public void addNode(Node node) {
        String nodeHash = HashUtil.hash(node.host() + ":" + node.tcpPort());
    
        ring.putIfAbsent(nodeHash, node);
    }

    public void removeNode(Node node) {
        String nodeHash = HashUtil.hash(node.host() + ":" + node.tcpPort());
    
        ring.remove(nodeHash);
    }

    public Node getOwnerNode(String key) {
        if (ring.isEmpty())
            return null;

        String keyHash = HashUtil.hash(key);

        // tailMap gets all nodes with a hash GREATER than or EQUAL to the file's hash
        // This is the equivalent of "looking clockwise" on the ring!
        SortedMap<String, Node> tailMap = ring.tailMap(keyHash);

        // If the tailMap is empty, it means we wrapped around the clock past the last
        // node.
        // In that case, the clockwise choice is the absolute first node in the ring.
        String targetHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(targetHash);

    }

    public int size(){
        return ring.size();
    }
}
