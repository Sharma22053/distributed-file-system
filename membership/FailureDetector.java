package membership;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cluster.ClusterManager;
import common.Node;

public class FailureDetector {
    private final ClusterManager clusterManager;
    Node self = new Node("127.0.0.1", 6001);
    private final Map<Node, Long> lastSeenTimeStamps = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    private static final long TIMEOUT_THRESHOLD_MS = 4000;
    private static final long MONITOR_INTERVAL_MS = 1000;

    public FailureDetector(Node self, ClusterManager clusterManager, ScheduledExecutorService scheduler) {
        this.self = self;
        this.clusterManager = clusterManager;
        this.scheduler = scheduler;
    }

    public void recordHeartbeat(Node node) {
        lastSeenTimeStamps.put(node, System.currentTimeMillis());
    }

    public synchronized void start() {
        scheduler.scheduleAtFixedRate(this::checkNodeFailures, MONITOR_INTERVAL_MS, MONITOR_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        System.err.println("[failureDetector] background monitoring started");
    }

    public void checkNodeFailures() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Node, Long> entry : lastSeenTimeStamps.entrySet()) {

            Node node = entry.getKey();
            long lastSeen = entry.getValue();
            if (node.equals(self)) {
                continue;
            }

            if ((now - lastSeen) > TIMEOUT_THRESHOLD_MS) {
                System.err.println("[FailureDetector] Node " + node + " missed heartbeats. Marking as FAILED.");

                lastSeenTimeStamps.remove(node);
                clusterManager.onNodeLeft(node);
            }
        }
    }

    public synchronized void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[FailureDetector] Monitoring stopped cleanly.");
    }

}
