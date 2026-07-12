package membership;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import common.Node;
import network.UdpMembershipBroadcaster;

public class HeartbeatManager {
    private final Node self;
    private final UdpMembershipBroadcaster udpMembershipBroadcaster;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public HeartbeatManager(Node self, UdpMembershipBroadcaster udpMembershipBroadcaster,
            ScheduledExecutorService scheduler) {
        this.self = self;
        this.udpMembershipBroadcaster = udpMembershipBroadcaster;
        this.scheduler = scheduler;
    }

    public synchronized void start() {
        if (running)
            return;
        running = true;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (running) {
                    udpMembershipBroadcaster.broadCastHeartBeat(self);
                }
            } catch (Exception e) {
                System.err.println("[heartBeatManager] failed to broadcast heartbeat: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
        System.out.println("[heartBeatManager] periodic heartbeat loop started (1 sec interval)");
    }

    public synchronized void stop() {
        if (!running)
            return;
        running = false;
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[HeartbeatManager] Heartbeat loop stopped cleanly.");
    }
}
