package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

import common.Node;
import common.NodeConfig;
import membership.MembershipService;

public class UdpMembershipListener implements Runnable {
    private final NodeConfig nodeConfig;
    private final MembershipService membershipService;
    private DatagramSocket datagramSocket;
    private final ExecutorService workerPool;
    private volatile boolean running = true;

    public UdpMembershipListener(NodeConfig nodeConfig, MembershipService membershipService,
            ExecutorService workerPool) {
        this.nodeConfig = nodeConfig;
        this.membershipService = membershipService;
        this.workerPool = workerPool;
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(nodeConfig.udpPort());
            System.out.println("udp membership listener started on :" + nodeConfig.udpPort());

            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // blocks until a packet arrives
                datagramSocket.receive(packet);

                // Extract data safely out of the buffer immediately
                byte[] packetData = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), packetData, 0, packet.getLength());
                // Hand off raw data off to the thread pool instantly so the loop can resume
                // receiving
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
                membershipService.addNode(node);
                System.out.println("node joined" + node);
            } else if ("LEAVE".equalsIgnoreCase(command)) {
                membershipService.removeNode(node);
                System.out.println("node left cleanly: " + node);
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
