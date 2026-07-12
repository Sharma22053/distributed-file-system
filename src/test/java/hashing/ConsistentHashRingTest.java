package hashing;

import common.Constants;
import common.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsistentHashRingTest {

    private ConsistentHashRing ring;
    private Node node1;
    private Node node2;

    @BeforeEach
    void setUp() {
        ring = new ConsistentHashRing();
        node1 = new Node("127.0.0.1", 8080);
        node2 = new Node("127.0.0.2", 9090);
    }

    @Test
    void testAddNodeIncreasesSize() {
        ring.addNode(node1);
        assertEquals(Constants.VIRTUAL_NODE_COUNT, ring.size());
    }

    @Test
    void testRemoveNodeDecreasesSize() {
        ring.addNode(node1);
        ring.removeNode(node1);
        assertEquals(0, ring.size());
    }

    @Test
    void testGetOwnerNodeReturnsCorrectNode() {
        ring.addNode(node1);
        ring.addNode(node2);

        Node owner = ring.getOwnerNode("myKey");
        assertNotNull(owner);
        assertTrue(owner.equals(node1) || owner.equals(node2));
    }

    @Test
    void testGetOwnerNodeReturnsNullWhenEmpty() {
        assertNull(ring.getOwnerNode("anyKey"));
    }

    @Test
    void testGetReplicaNodesRespectsReplicationFactor() {
        ring.addNode(node1);
        ring.addNode(node2);

        List<Node> replicas = ring.getReplicaNodes("replicaKey", 2);
        assertEquals(2, replicas.size());
        assertTrue(replicas.contains(node1));
        assertTrue(replicas.contains(node2));
    }

    @Test
    void testGetReplicaNodesEmptyRing() {
        List<Node> replicas = ring.getReplicaNodes("replicaKey", 2);
        assertTrue(replicas.isEmpty());
    }

    @Test
    void testGetReplicaNodesReplicationFactorGreaterThanNodes() {
        ring.addNode(node1);

        List<Node> replicas = ring.getReplicaNodes("replicaKey", 5);
        assertEquals(1, replicas.size());
        assertEquals(node1, replicas.get(0));
    }
}

