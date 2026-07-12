package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import common.MessageType;
import common.Node;

public class TcpRequestClient {
    private static final int CONNECTION_TIMEOUT_MS = 3000;

    public boolean replicaPut(Node target, String key, byte[] data){
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.host(), target.tcpPort()), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(CONNECTION_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out.writeUTF(MessageType.REPLICA_PUT.name());
            out.writeUTF(key);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
            String response = in.readUTF();
            return "OK".equalsIgnoreCase(response);
        } catch (IOException e) {
            System.err.println("[TcpRequestClient] PUT failed on node " + target + " for key: " + key + ". Error: "
                    + e.getMessage());
            return false;
        }
    }

    public boolean put(Node target, String key, byte[] data) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.host(), target.tcpPort()), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(CONNECTION_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out.writeUTF(MessageType.PUT.name());
            out.writeUTF(key);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
            String response = in.readUTF();
            return "OK".equalsIgnoreCase(response);
        } catch (IOException e) {
            System.err.println("[TcpRequestClient] PUT failed on node " + target + " for key: " + key + ". Error: "
                    + e.getMessage());
            return false;
        }
    }

    public byte[] get(Node target, String key) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.host(), target.tcpPort()), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(CONNECTION_TIMEOUT_MS);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF(MessageType.GET.name());
            out.writeUTF(key);
            out.flush();

            String status = in.readUTF();
            if ("OK".equalsIgnoreCase(status)) {
                int dataLength = in.readInt();
                if (dataLength < 0)
                    return null;

                byte[] data = new byte[dataLength];

                in.readFully(data);
                return data;
            } else {
                System.err.println("[TcpRequestClient] GET failed. Server responded: " + status);
                return null;
            }

        } catch (IOException e) {
            System.err.println("[TcpRequestClient] GET failed on node " + target + " for key: " + key + ". Error: "
                    + e.getMessage());
            return null;
        }
    }

    public boolean delete(Node target, String key) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.host(), target.tcpPort()), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(CONNECTION_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out.writeUTF(MessageType.DELETE.name());
            out.writeUTF(key);
            out.flush();

            String response = in.readUTF();
            return "OK".equalsIgnoreCase(response);

        } catch (IOException e) {
            System.err.println("[TcpRequestClient] DELETE failed on node " + target + " for key: " + key + ". Error: "
                    + e.getMessage());
            return false;
        }
    }

    public boolean exists(Node target, String key) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.host(), target.tcpPort()), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(CONNECTION_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out.writeUTF(MessageType.EXISTS.name());
            out.writeUTF(key);
            out.flush();

            String response = in.readUTF();
            return "TRUE".equalsIgnoreCase(response);

        } catch (IOException e) {
            System.err.println("[TcpRequestClient] EXISTS check failed on node " + target + " for key: " + key
                    + ". Error: " + e.getMessage());
            return false;
        }
    }
}
