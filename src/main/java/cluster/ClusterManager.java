package cluster;

import java.util.List;
import java.util.Set;

import common.Node;
import hashing.ConsistentHashRing;
import membership.MembershipService;

public class ClusterManager {
    private final MembershipService membershipService;
    private final ConsistentHashRing consistentHashRing;

    public ClusterManager(MembershipService membershipService, ConsistentHashRing consistentHashRing) {
        this.membershipService = membershipService;
        this.consistentHashRing = consistentHashRing;
    }

    public synchronized boolean onNodeJoined(Node node) {
        if (membershipService.contains(node)) {
        return false;
    }
        membershipService.addNode(node);
        consistentHashRing.addNode(node);
        System.out.println("[ClusterManager] Topology updated. Node JOINED: " + node);
        return true;
    }

    public synchronized boolean onNodeLeft(Node node) {
        if (!membershipService.contains(node)) {
        return false;
    }
        membershipService.removeNode(node);
        consistentHashRing.removeNode(node);
        System.out.println("[ClusterManager] Topology updated. Node LEFT/FAILED: " + node);
        return true;
    }

    public Node routeKey(String key) {
        return consistentHashRing.getOwnerNode(key);
    }

    public Set<Node> getAllNodes() {
        return membershipService.getAllNodes();
    }

    public boolean contains(Node node) {
        return membershipService.contains(node);
    }

    public int size() {
        return membershipService.size();
    }

    public List<Node> getReplicaNodes(String key,int replicationFactor) {
        return consistentHashRing.getReplicaNodes(
                key,
                replicationFactor);
    }
}
