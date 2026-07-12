package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import common.Node;

public class UdpMembershipBroadcaster implements AutoCloseable {

    private final DatagramSocket datagramSocket;
    private final InetAddress destinationAddress;
    private final int targetUdpPort;

    public UdpMembershipBroadcaster(int targetUdpPort) {
        this.targetUdpPort = targetUdpPort;
        try {
            this.datagramSocket = new DatagramSocket();
            this.datagramSocket.setBroadcast(true);
            this.destinationAddress = InetAddress.getByName("255.255.255.255");
        } catch (SocketException e) {
            throw new RuntimeException("failed to open the persistent UDP broadcast socket", e);
        } catch (UnknownHostException e) {
            throw new RuntimeException("failed to resolve broadcast address", e);
        }
    }

    public void broadcastJoin(Node node) {
        String payload = createMessage("JOIN", node);
        sendPacket(payload);
    }

    public void broadcastLeave(Node node) {
        String payload = createMessage("LEAVE", node);
        sendPacket(payload);
    }

    private void sendPacket(String payload) {
        try {
            byte[] buffer = payload.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destinationAddress, targetUdpPort);
            datagramSocket.send(packet);
            if (!payload.startsWith("HEARTBEAT")) {
                System.out.println("[UDP Broadcaster] Sent: " + payload
                        + " -> "
                        + destinationAddress
                        + ":"
                        + targetUdpPort);
            }
        } catch (IOException e) {
            System.err.println("Error sending UDP payload [" + payload + "]: " + e.getMessage());
        }
    }

    private String createMessage(String command, Node node) {
        return command + " " + node.host() + " " + node.tcpPort();
    }

    public void broadCastHeartBeat(Node node) {
        String payload = createMessage("HEARTBEAT", node);
        sendPacket(payload);
    }

    @Override
    public void close() throws Exception {
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
            System.out.println("persistent UDP broadcaster socket closed cleanly");
        }
    }

}
