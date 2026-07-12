package client;

import java.util.Scanner;

public class CLI {
    private final DFSClient dfsClient;
    private volatile boolean running = true;

    public CLI(DFSClient dfsClient) {
        this.dfsClient = dfsClient;
    }

    public void start() {
        printWelcomeMenu();

        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                System.out.print("dfs> ");
                if (!scanner.hasNextLine()) break;

                String inputLine = scanner.nextLine().trim();
                if (inputLine.isEmpty()) continue;

                // Split inputs cleanly by handling varying spaces
                String[] tokens = inputLine.split("\\s+",3);
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
                        case "HELP":
                            printHelpMenu();
                            break;
                        case "EXIT":
                        case "QUIT":
                            System.out.println("Exiting client shell. Goodbye!");
                            running = false;
                            break;
                        default:
                            System.out.println("Unknown command. Type 'HELP' to view syntax guidelines.");
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("Command Processing Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void handlePut(String[] tokens) {
        if (tokens.length < 3) {
            System.out.println("Usage: PUT <key> <string_data>");
            return;
        }

        String key = tokens[1];
        
        String dataContent = tokens[2];

        System.out.println("Invoking cluster client write path for key: " + key + "...");
        boolean success = dfsClient.putFile(key, dataContent);
        
        if (success) {
            System.out.println("Success: PUT completed successfully");
        } else {
            System.err.println("Error: Failed to process distributed client put for key [" + key + "].");
        }
    }

    private void handleGet(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Usage: GET <key>");
            return;
        }

        String key = tokens[1];
        System.out.println("Requesting key path mapping lookup for: " + key + "...");
        String content = dfsClient.getFile(key);

        if (content == null) {
            System.out.println("Result: Key target [" + key + "] not found or currently unreachable.");
        } else {
            System.out.println("-------------------------------------------");
            System.out.println("Key   : " + key);
            System.out.println("Value : " + content);
            System.out.println("-------------------------------------------");
        }
    }

    private void handleDelete(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Usage: DELETE <key>");
            return;
        }

        String key = tokens[1];
        System.out.println("Requesting cluster-wide key purge for: " + key + "...");
        boolean success = dfsClient.deleteFile(key);

        if (success) {
            System.out.println("Success: Key path [" + key + "] deleted successfully from primary owner.");
        } else {
            System.err.println("Error: Cluster failed to execute file removal command.");
        }
    }

    private void handleExists(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Usage: EXISTS <key>");
            return;
        }

        String key = tokens[1];
        boolean status = dfsClient.fileExists(key);
        System.out.println("Storage Metadata Response: File status present -> " + status);
    }

    private void printWelcomeMenu() {
        System.out.println("===========================================");
        System.out.println("     Distributed File System Client (CLI)  ");
        System.out.println("===========================================");
        System.out.println("Type 'HELP' to output available commands.");
        System.out.println("Type 'EXIT' or 'QUIT' to close the terminal.");
        System.out.println("===========================================");
    }

    private void printHelpMenu() {
        System.out.println("\nAvailable Operations:");
        System.out.println("  PUT <key> <data>   - Writes text block payload against a path key.");
        System.out.println("  GET <key>          - Performs network retrieval of a target file key.");
        System.out.println("  DELETE <key>       - Instructs primary file owner to remove data node.");
        System.out.println("  EXISTS <key>       - Checks whether a key exists.");
        System.out.println("  HELP               - Displays available commands");
        System.out.println("  EXIT / QUIT        - Exits the active process session environment.\n");
    }
}
