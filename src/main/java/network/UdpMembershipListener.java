package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import common.Node;
import common.NodeConfig;
import membership.MembershipService;

public class UdpMembershipListener implements Runnable {
    private final NodeConfig nodeConfig;

    private DatagramSocket datagramSocket;
    private final ExecutorService workerPool;
    private volatile boolean running = true;
    private final Consumer<Node> onNodeJoined;
    private final Consumer<Node> onNodeLeft;
    private final Consumer<Node> onHeartbeat;

    public UdpMembershipListener(NodeConfig nodeConfig,
            ExecutorService workerPool, Consumer<Node> onNodeJoined,
            Consumer<Node> onNodeLeft, Consumer<Node> onHeartbeat) {
        this.nodeConfig = nodeConfig;
        this.workerPool = workerPool;
        this.onNodeJoined = onNodeJoined;
        this.onNodeLeft = onNodeLeft;
        this.onHeartbeat = onHeartbeat;
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(nodeConfig.udpPort());
            System.out.println("udp membership listener started on :" + nodeConfig.udpPort());

            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                byte[] packetData = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), packetData, 0, packet.getLength());
                workerPool.submit(() -> processPacket(packetData));
            }
        } catch (SocketException e) {
            if (running) {
                System.err.println("udp socket error: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }

        finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }
    }

    private void processPacket(byte[] data) {
        try {
            String message = new String(data, StandardCharsets.UTF_8).trim();

            String[] tokens = message.trim().split("\\s+");
            if (tokens.length < 4)
                return;

            String command = tokens[0];
            String host = tokens[1];
            int tcpPort = Integer.parseInt(tokens[2]);
            Node node = new Node(host, tcpPort);

            if ("JOIN".equalsIgnoreCase(command)) {

                onNodeJoined.accept(node);
                System.out.println("Node joined: " + node);

            } else if ("LEAVE".equalsIgnoreCase(command)) {

                onNodeLeft.accept(node);
                System.out.println("Node left: " + node);
                

            } else if ("HEARTBEAT".equalsIgnoreCase(command)) {

                onHeartbeat.accept(node);

            }
        } catch (Exception e) {
            System.err.println("failed to process membership packet: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        datagramSocket.close();
        workerPool.shutdown();
    }
}
