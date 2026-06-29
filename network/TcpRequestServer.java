package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import common.MessageType;
import common.NodeConfig;
import storage.StorageManager;

public class TcpRequestServer implements Runnable {
    private final NodeConfig nodeConfig;
    private final StorageManager storageManager;
    private final ExecutorService workerPool;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public TcpRequestServer(NodeConfig nodeConfig, StorageManager storageManager, ExecutorService workerPool) {
        this.nodeConfig = nodeConfig;
        this.storageManager = storageManager;
        this.workerPool = workerPool;
    }

    @Override
    public void run() {
        try {
            this.serverSocket = new ServerSocket(nodeConfig.node().tcpPort());
            System.out.println("[TCPRequestServer] listening for data transfer requests on TCP port: " + nodeConfig.node().tcpPort());

            while (running) {
                Socket clientSocket = serverSocket.accept();
                workerPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[TcpRequestServer] Server crash error: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket socket) {
        try(DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())){

                String commandStr = in.readUTF();
                MessageType type = MessageType.valueOf(commandStr.toUpperCase());

                switch (type) {
                    case PUT:
                        handlePut(in,out);
                        break;
                    case GET:
                        handleGet(in,out);
                        break;
                    case DELETE:
                        handleDelete(in,out);
                        break;
                    case EXISTS:
                        handleExists(in,out);
                        break;
                    default:
                        out.writeUTF("ERROR: unsupported TCP operation");
                        break;
                }
                out.flush();
            } catch(IOException e){
                System.err.println("[TcpRequestServer] Failed handling client request: " + e.getMessage());
            }
    }
    private void handlePut(DataInputStream in, DataOutputStream out) throws IOException {
        String key = in.readUTF();
        int dataLength = in.readInt();
        
        if (dataLength < 0) {
            out.writeUTF("ERROR: Invalid payload length");
            return;
        }

        byte[] data = new byte[dataLength];

        in.readFully(data);

       
        storageManager.put(key, data);
        
       
        out.writeUTF("OK");
    }

    private void handleGet(DataInputStream in, DataOutputStream out) throws IOException {
        String key = in.readUTF();
        byte[] data = storageManager.get(key);

        if (data == null) {
            out.writeUTF("ERROR: Key not found");
        } else {
            out.writeUTF("OK");
            out.writeInt(data.length);
            out.write(data);
        }
    }

    private void handleDelete(DataInputStream in, DataOutputStream out) throws IOException {
        String key = in.readUTF();
        storageManager.delete(key);
        out.writeUTF("OK");
    }

    private void handleExists(DataInputStream in, DataOutputStream out) throws IOException {
        String key = in.readUTF();
        boolean exists = storageManager.exists(key);
        out.writeUTF(exists ? "TRUE" : "FALSE");
    }

    public synchronized void stop() {
        this.running = false;
        workerPool.shutdown();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        System.out.println("[TcpRequestServer] Stopped cleanly.");
    }
}

