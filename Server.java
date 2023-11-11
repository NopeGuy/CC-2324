import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {

    private static final int PORT = 9090;
    private static volatile boolean isRunning = true;
    private static Map<String, List<String>> clientFilesMap = new HashMap<>();
    private static Map<String, List<String>> clientBlockFilesMap = new HashMap<>();

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            // Start a thread to handle the menu
            Thread menuThread = new Thread(Server::startMenu);
            menuThread.start();

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[DEBUG] Client connected: " + clientSocket);

                // Start a new thread to handle the client
                executorService.execute(new ClientHandler(clientSocket, clientFilesMap, clientBlockFilesMap));
            }

            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startMenu() {
        Scanner scanner = new Scanner(System.in);

        while (isRunning) {
            try {
                System.out.println("\nMenu:");
                System.out.println("1. Display client files");
                System.out.println("2. Exit\n");

                int choice = scanner.nextInt();
                scanner.nextLine();  // Consume newline

                switch (choice) {
                    case 1:
                        displayClientFiles();
                        break;
                    case 2:
                        System.out.println("Exiting the server.");
                        isRunning = false;
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }

    private static void displayClientFiles() {
        System.out.println("\n\n#################\n# Client Files and Blocks: #\n#################\n");
        for (Map.Entry<String, List<String>> entry : clientFilesMap.entrySet()) {
            System.out.println("Client: " + entry.getKey());
            System.out.println("Files: " + entry.getValue());

            // Retrieve blocks associated with the files
            List<String> blocks = new ArrayList<>();
            for (String file : entry.getValue()) {
                List<String> fileBlocks = clientBlockFilesMap.get(file);
                if (fileBlocks != null) {
                    blocks.add("File: " + file + " Blocks: " + fileBlocks);
                }
            }

            if (!blocks.isEmpty()) {
                System.out.println("Blocks:");
                for (String blockInfo : blocks) {
                    System.out.println(blockInfo);
                }
            } else {
                System.out.println("No blocks information available.");
            }
            System.out.println("--------");
        }
    }

}
