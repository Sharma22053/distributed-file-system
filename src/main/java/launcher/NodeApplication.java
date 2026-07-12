package launcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import common.Node;
import common.NodeConfig;
import membership.MembershipService;
import membership.FailureDetector;
import membership.HeartbeatManager;
import hashing.ConsistentHashRing;
import network.TcpRequestServer;
import network.TcpRequestClient;
import network.UdpMembershipBroadcaster;
import network.UdpMembershipListener;
import storage.LocalDiskService;
import storage.FileLockManager;
import storage.StorageManager;
import storage.DistributedStorageService;
import replication.ReplicationManager;
import rebalance.RebalancingManager;
import client.CLI;
import client.DFSClient;
import cluster.ClusterManager;

public class NodeApplication {
    private ExecutorService networkExecutor;
    private ScheduledExecutorService schedulerExecutor;
    private TcpRequestServer tcpServer;
    private UdpMembershipListener udpListener;
    private UdpMembershipBroadcaster udpBroadcaster;
    private HeartbeatManager heartbeatManager;
    private FailureDetector failureDetector;
    private ReplicationManager replicationManager;

    public void start(String[] args) {
        System.out.println("[NodeApplication] Initializing DFS Core Node Application...");

        String host = "127.0.0.1";
        int tcpPort = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        int udpPort = args.length > 1 ? Integer.parseInt(args[1]) : 9091;
        Path storageDir = Paths.get("./dfs_storage_" + tcpPort);
        int replicationFactor = 3;

        Node self = new Node(host, tcpPort);
        NodeConfig nodeConfig = new NodeConfig(self, udpPort, storageDir);
        System.out.println("[NodeApplication] Storage Directory: " + storageDir.toAbsolutePath());

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        this.networkExecutor = Executors.newFixedThreadPool(availableProcessors);
        this.schedulerExecutor = Executors.newScheduledThreadPool(2);

        MembershipService membershipService = new MembershipService();
        ConsistentHashRing hashRing = new ConsistentHashRing();
        ClusterManager clusterManager = new ClusterManager(membershipService, hashRing);

        LocalDiskService diskService = new LocalDiskService(storageDir);
        FileLockManager lockManager = new FileLockManager();
        StorageManager storageManager = new StorageManager(diskService, lockManager);

        TcpRequestClient tcpClient = new TcpRequestClient();

        this.replicationManager = new ReplicationManager(clusterManager, tcpClient, replicationFactor);
        this.udpBroadcaster = new UdpMembershipBroadcaster(udpPort);

        DistributedStorageService distributedStorageService = new DistributedStorageService(
                self, clusterManager, storageManager, this.replicationManager, tcpClient);
        this.tcpServer = new TcpRequestServer(nodeConfig, storageManager, networkExecutor, this.replicationManager);

        RebalancingManager rebalancingManager = new RebalancingManager(self, clusterManager, storageManager, tcpClient);

        this.failureDetector = new FailureDetector(
                self,
                node -> {
                    failureDetector.onNodeLeft(node);
                    clusterManager.onNodeLeft(node);
                    rebalancingManager.rebalance();
                    rebalancingManager.retryFailedMigrations();
                    replicationManager.ensureReplication(storageManager);

                },
                schedulerExecutor);

        this.udpListener = new UdpMembershipListener(
                nodeConfig,
                networkExecutor,

                // JOIN
                node -> {
                    boolean changed = clusterManager.onNodeJoined(node);

                    failureDetector.recordHeartbeat(node);

                    if (changed) {
                        rebalancingManager.rebalance();
                        rebalancingManager.retryFailedMigrations();
                        replicationManager.ensureReplication(storageManager);
                    }
                },
                node -> {
                    boolean changed = clusterManager.onNodeLeft(node);
                    failureDetector.onNodeLeft(node);
                    if (changed) {
                        rebalancingManager.rebalance();
                        rebalancingManager.retryFailedMigrations();
                        replicationManager.ensureReplication(storageManager);
                    }
                },
                node -> {
                    failureDetector.recordHeartbeat(node);
                });
        this.heartbeatManager = new HeartbeatManager(self, udpBroadcaster, schedulerExecutor);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[ShutdownHook] Graceful termination loop triggered. Cleaning OS allocations...");
            shutdownAllServices(self);
        }));

        networkExecutor.submit(tcpServer);
        networkExecutor.submit(udpListener);

        clusterManager.onNodeJoined(self);
        udpBroadcaster.broadcastJoin(self);

        heartbeatManager.start();
        failureDetector.recordHeartbeat(self);
        failureDetector.start();

        System.out.println(
                "[NodeApplication] Bootstrapping completed successfully. Launching client console interface...");

        DFSClient dfsClient = new DFSClient(distributedStorageService,clusterManager,storageManager,hashRing);
        CLI cli = new CLI(dfsClient,tcpPort);
        cli.start();

    }

    private void shutdownAllServices(Node self) {
        try {

            if (udpBroadcaster != null) {
                udpBroadcaster.broadcastLeave(self);
                udpBroadcaster.close();
            }

            if (heartbeatManager != null)
                heartbeatManager.stop();
            if (failureDetector != null)
                failureDetector.stop();

            if (tcpServer != null)
                tcpServer.stop();
            if (udpListener != null)
                udpListener.stop();
            networkExecutor.shutdown();
            schedulerExecutor.shutdown();

            try {
                networkExecutor.awaitTermination(5, TimeUnit.SECONDS);
                schedulerExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            networkExecutor.shutdownNow();
            schedulerExecutor.shutdownNow();

            System.out.println("[ShutdownHook] Global infrastructure variables released cleanly.");
        } catch (Exception e) {
            System.err.println("Exception encountered finalizing cluster context shutdown: " + e.getMessage());
        }
    }
}
