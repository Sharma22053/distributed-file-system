package cluster;

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

    public synchronized void onNodeJoined(Node node) {
        membershipService.addNode(node);
        consistentHashRing.addNode(node);
        System.out.println("[ClusterManager] Topology updated. Node JOINED: " + node);
    }

    public synchronized void onNodeLeft(Node node) {
        membershipService.removeNode(node);
        consistentHashRing.removeNode(node);
        System.out.println("[ClusterManager] Topology updated. Node LEFT/FAILED: " + node);
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
}
