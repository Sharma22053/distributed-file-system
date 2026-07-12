package client;

import java.util.List;
import java.util.Scanner;

public class CLI {
    private final DFSClient dfsClient;
    private volatile boolean running = true;
    private final int nodePort;

    public CLI(DFSClient dfsClient, int nodePort) {
        this.dfsClient = dfsClient;
        this.nodePort = nodePort;
    }

    public void start() {
        printWelcomeMenu();

        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                System.out.print(ConsoleColors.CYAN + "dfs@" + nodePort + " > " + ConsoleColors.RESET);
                if (!scanner.hasNextLine())
                    break;

                String inputLine = scanner.nextLine().trim();
                if (inputLine.isEmpty())
                    continue;

                String[] tokens = inputLine.split("\\s+", 3);
                String command = tokens[0].toUpperCase();

                try {
                    switch (command) {
                        case "PUT":
                            handlePut(tokens);
                            break;
                        case "GET":
                            handleGet(tokens);
                            break;
                        case "DELETE":
                            handleDelete(tokens);
                            break;
                        case "EXISTS":
                            handleExists(tokens);
                            break;
                        case "NODES":
                            handleNodes();
                            break;
                        case "STATUS":
                            handleStatus();
                            break;
                        case "RING":
                            handleRing();
                            break;
                        case "OWNER":
                            handleOwner(tokens);
                            break;
                        case "FILES":
                            handleFiles();
                            break;
                        case "HELP":
                            printHelpMenu();
                            break;
                        case "EXIT":
                        case "QUIT":
                            System.out.println(ConsoleColors.YELLOW + "Goodbye! 👋" + ConsoleColors.RESET);
                            running = false;
                            break;
                        default:
                            System.out.println(
                                    ConsoleColors.RED + "❌ Unknown command. Type 'HELP'." + ConsoleColors.RESET);
                            break;
                    }
                } catch (Exception e) {
                    System.err.println(
                            ConsoleColors.RED_BOLD + "⚠️ Command Error: " + e.getMessage() + ConsoleColors.RESET);
                    e.printStackTrace();
                }
            }
        }
    }

    private void handlePut(String[] tokens) {
        if (tokens.length < 3) {
            System.out.println(ConsoleColors.YELLOW + "Usage: PUT <key> <string_data>" + ConsoleColors.RESET);
            return;
        }

        String key = tokens[1];
        String dataContent = tokens[2];

        long startTime = System.nanoTime();
        System.out.println(
                ConsoleColors.BLUE + "📤 Invoking cluster write path for key: " + key + " ..." + ConsoleColors.RESET);
        boolean success = dfsClient.putFile(key, dataContent);
        long endTime = System.nanoTime();
        long timeTaken = (endTime - startTime) / 1_000_000;

        if (success) {
            System.out.println(ConsoleColors.GREEN_BOLD + "✔ PUT successful" + ConsoleColors.RESET);
            System.out.println("   Time Taken : " + timeTaken + " ms");
            System.out.println("   Primary Owner : " + dfsClient.getPrimaryOwner(key));

            List<String> replicas = dfsClient.getReplicas(key);

            if (replicas.isEmpty()) {
                System.out.println("   Replicas      : None");
            } else {
                System.out.println("   Replicas      : " + String.join(", ", replicas));
            }
        } else {
            System.err.println(ConsoleColors.RED_BOLD + "✘ PUT failed for key [" + key + "]." + ConsoleColors.RESET);
        }
    }

    private void handleGet(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println(ConsoleColors.YELLOW + "Usage: GET <key>" + ConsoleColors.RESET);
            return;
        }

        String key = tokens[1];
        System.out.println(
                ConsoleColors.BLUE + "📥 Requesting key path mapping lookup for: " + key + "..." + ConsoleColors.RESET);
        String content = dfsClient.getFile(key);

        if (content == null) {
            System.out.println(ConsoleColors.RED + "✘ Key target [" + key + "] not found or currently unreachable."
                    + ConsoleColors.RESET);
        } else {
            System.out.println(ConsoleColors.GREEN + "✔ Key found!" + ConsoleColors.RESET);
            System.out.println("┌─────────────────────────────────────┐");
            System.out.printf("│ %-10s : %-25s │%n", "Key", key);
            System.out.printf("│ %-10s : %-25s │%n", "Value", content);
            System.out.println("└─────────────────────────────────────┘");
        }
    }

    private void handleDelete(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println(ConsoleColors.YELLOW + "Usage: DELETE <key>" + ConsoleColors.RESET);
            return;
        }

        String key = tokens[1];
        System.out.println(
                ConsoleColors.BLUE + "🗑️ Requesting cluster-wide key purge for: " + key + "..." + ConsoleColors.RESET);
        boolean success = dfsClient.deleteFile(key);

        if (success) {
            System.out.println(ConsoleColors.GREEN_BOLD + "✔ Key path [" + key
                    + "] deleted successfully from primary owner." + ConsoleColors.RESET);
        } else {
            System.err.println(
                    ConsoleColors.RED_BOLD + "✘ Cluster failed to execute file removal command." + ConsoleColors.RESET);
        }
    }

    private void handleExists(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println(ConsoleColors.YELLOW + "Usage: EXISTS <key>" + ConsoleColors.RESET);
            return;
        }

        String key = tokens[1];
        boolean status = dfsClient.fileExists(key);
        String statusStr = status ? ConsoleColors.GREEN + "✔ TRUE" : ConsoleColors.RED + "✘ FALSE";
        System.out.println("Storage Metadata Response: File status present -> " + statusStr + ConsoleColors.RESET);
    }

    private void handleNodes() {

        System.out.println(ConsoleColors.CYAN_BOLD + "\nActive Cluster Nodes" + ConsoleColors.RESET);
        System.out.println("--------------------------------");

        List<String> nodes = dfsClient.getActiveNodes();

        if (nodes.isEmpty()) {
            System.out.println("No active nodes.");
            return;
        }

        for (String node : nodes) {
            System.out.println("  ✓ " + node);
        }

        System.out.println();
        System.out.println("Total Nodes : " + nodes.size());
        System.out.println();
    }

    private void handleStatus() {
        System.out.println(
                ConsoleColors.CYAN_BOLD + "\n================ Cluster Status ================" + ConsoleColors.RESET);
        System.out.println();
        System.out.println("Self Node");
        System.out.println("---------");
        System.out.println("  127.0.0.1:" + nodePort);
        System.out.println();
        System.out.println("Active Nodes");
        System.out.println("------------");
        List<String> activeNodes = dfsClient.getActiveNodes();

        for (String node : activeNodes) {
            System.out.println("  ✓ " + node);
        }
        System.out.println();
        System.out.println("Cluster Size       : " + dfsClient.getClusterSize());
        System.out.println("Replication Factor : 3");
        System.out.println("===============================================");
        System.out.println();

    }

    private void handleRing() {
        System.out.println(ConsoleColors.CYAN_BOLD + "\nConsistent Hash Ring" + ConsoleColors.RESET);
        System.out.println("-----------------------------------------------");
        List<String> entries = dfsClient.getRingEntries();

        if (entries.isEmpty()) {
            System.out.println("Hash ring is empty.");
            return;
        }

        for (String entry : entries) {
            System.out.println(entry);
        }

        System.out.println();
    }

    private void handleOwner(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println(ConsoleColors.YELLOW + "Usage: OWNER <key>" + ConsoleColors.RESET);
            return;
        }
        String key = tokens[1];
        System.out.println(ConsoleColors.CYAN_BOLD + "\nKey" + ConsoleColors.RESET);
        System.out.println("  " + key);
        System.out.println();
        System.out.println(ConsoleColors.CYAN_BOLD + "Primary" + ConsoleColors.RESET);
        System.out.println("  " + dfsClient.getPrimaryOwner(key));
        System.out.println();
        System.out.println(ConsoleColors.CYAN_BOLD + "Replicas" + ConsoleColors.RESET);
        List<String> replicas = dfsClient.getReplicas(key);

        if (replicas.isEmpty()) {
            System.out.println("  None");
        } else {
            replicas.forEach(replica -> System.out.println("  " + replica));
        }
        System.out.println();
    }

    private void handleFiles() {

        System.out.println(ConsoleColors.CYAN_BOLD + "\nStored Keys" + ConsoleColors.RESET);

        List<String> keys = dfsClient.listLocalKeys();

        if (keys.isEmpty()) {
            System.out.println("  No files stored locally.");
        } else {
            keys.forEach(key -> System.out.println("  " + key));
        }

        System.out.println();
        System.out.println("Total : " + keys.size());
        System.out.println();
    }
    // ===================== MENUS =====================

    private void printWelcomeMenu() {
        System.out.println(ConsoleColors.CYAN_BOLD + "╔════════════════════════════════════════════════════════╗");
        System.out.println("║        Distributed File System (DFS) v1.0             ║");
        System.out.println("║        Consistent Hashing • Replication • HA          ║");
        System.out.println("╚════════════════════════════════════════════════════════╝" + ConsoleColors.RESET);
        System.out.println();
        System.out.println("Connected Node : 127.0.0.1:" + nodePort);
        System.out.println("Cluster Size   : " + dfsClient.getClusterSize());
        System.out.println("Storage Path   : ./dfs_storage_" + nodePort);
        System.out.println();
        System.out.println(ConsoleColors.CYAN + "Type HELP to list available commands." + ConsoleColors.RESET);
        System.out.println();
    }

    private void printHelpMenu() {
        System.out.println(ConsoleColors.CYAN_BOLD + "\nAvailable Commands" + ConsoleColors.RESET);
        System.out.println();
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "PUT <key> <value>" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Store a value" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "GET <key>" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Retrieve value" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "DELETE <key>" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Delete key" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "EXISTS <key>" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Check existence" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "NODES" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Show active cluster members" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "STATUS" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Show cluster status" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "RING" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Visualize hash ring" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "OWNER <key>" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Show owner & replicas" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "FILES" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Show local files" + ConsoleColors.RESET);
        System.out.printf("  %-20s %s%n", ConsoleColors.WHITE + "EXIT" + ConsoleColors.RESET,
                ConsoleColors.GREEN + "Shutdown node" + ConsoleColors.RESET);
        System.out.println();
    }
}