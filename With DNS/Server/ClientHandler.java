import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Map<String, List<String>> clientFilesMap;
    private Map<String, List<String>> clientBlockFilesMap;
    private Map<String, Integer> FileBlockNumber;

    public ClientHandler(Socket clientSocket, Map<String, List<String>> clientFilesMap,
                         Map<String, List<String>> clientBlockFilesMap, Map<String, Integer> FileBlockNumber) {
        this.clientSocket = clientSocket;
        this.clientFilesMap = clientFilesMap;
        this.clientBlockFilesMap = clientBlockFilesMap;
        this.FileBlockNumber = FileBlockNumber;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            StringBuilder messageBuilder = new StringBuilder();
            String ip = "";
            // Get the IP of the client through the socket
            ip = clientSocket.getInetAddress().getHostName();

            int currentChar;
            while ((currentChar = reader.read()) != -1) {
                char currentCharAsChar = (char) currentChar;

                if (currentCharAsChar == '$') {
                    // End of message, process it
                    processMessage(messageBuilder.toString(), ip);
                    messageBuilder.setLength(0);  // Clear the StringBuilder for the next message
                } else {
                    messageBuilder.append(currentCharAsChar);
                }
            }

            // Process any remaining content in the StringBuilder
            if (messageBuilder.length() > 0) {
                processMessage(messageBuilder.toString(), ip);
            }

            // Delete all the info of clientBlockFilesMap from that IP
            removeExistingEntriesForIP(ip);
            System.out.println("Updated clientBlockFilesMap: " + clientBlockFilesMap);

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(String receivedString, String ip) {
        String[] parts = receivedString.split(";");
        if (parts.length < 2) {
            System.err.println("Invalid header format.");
            return;
        }

        String requestType = parts[0];

        String requestInfo = parts[1];

        if (requestType.equals("1")) {
            String[] fileswithsize = requestInfo.split(":");
            for (String file : fileswithsize) {
                String[] filesInfo = file.split("!");

                if (!clientFilesMap.containsKey(filesInfo[0])) {
                    clientFilesMap.put(filesInfo[0], new ArrayList<>());
                }
                List<String> files = clientFilesMap.get(filesInfo[0]);
                files.add(ip);
                clientFilesMap.put(filesInfo[0], files);

                // Set the number of blocks for the file
                if (filesInfo.length > 1) {
                    int numberOfBlocks = Integer.parseInt(filesInfo[1]);
                    FileBlockNumber.put(filesInfo[0], numberOfBlocks);
                }
            }
            System.out.println("Received files for IP " + ip + ": " + clientFilesMap);
        }

        if (requestType.equals("2")) {
            String[] blockInfo = requestInfo.split("\\|");

            for (String block : blockInfo) {
                String[] blockParts = block.split("«");
                String fileName = blockParts[0];
                String blockNumber = blockParts[1];

                // Update clientBlockFilesMap to associate files with blocks and IPs
                if (!clientBlockFilesMap.containsKey(fileName)) {
                    clientBlockFilesMap.put(fileName, new ArrayList<>());
                }
                List<String> blocks = clientBlockFilesMap.get(fileName);
                blocks.add(blockNumber + "/" + ip);
                clientBlockFilesMap.put(fileName, blocks);
            }

            System.out.println("Received blocks information for IP " + ip + ": " + clientBlockFilesMap);
            System.out.println("FileBlockNumber: " + FileBlockNumber);
        }

        if (requestType.equals("3")) {
            String requestedFile = requestInfo;
            StringBuilder clientsWithBlocks = new StringBuilder();

            if (clientBlockFilesMap.containsKey(requestedFile)) {
                clientsWithBlocks.append("2" + ";");
                List<String> blocks = clientBlockFilesMap.get(requestedFile);
                for (String block : blocks) {
                    clientsWithBlocks.append(block).append("|");
                }
                clientsWithBlocks.append("%");
                clientsWithBlocks.append(requestedFile);
                clientsWithBlocks.append("%");
                clientsWithBlocks.append(ip);
                clientsWithBlocks.append("%");
                // Append the number of blocks for the requested file
                if (FileBlockNumber.containsKey(requestedFile)) {
                    clientsWithBlocks.append(FileBlockNumber.get(requestedFile));
                }
                clientsWithBlocks.append("$");
                System.out.println("Clients with blocks: " + clientsWithBlocks);
            } else {
                clientsWithBlocks.append("1;" + "No clients have blocks of file ").append(requestedFile).append("$");
            }

            // debug
            System.out.println("Clients with blocks: " + clientsWithBlocks);
            try {
                OutputStream outputStream = clientSocket.getOutputStream();
                byte[] responseBytes = clientsWithBlocks.toString().getBytes(StandardCharsets.UTF_8);
                outputStream.write(responseBytes);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestType.equals("4")) {
            String requestInfoToFind = requestInfo; // Request info to find

            if (clientFilesMap.containsKey(requestInfoToFind)) {
                String message = "1;" + "Client IPs having the file " + requestInfoToFind + ":\n";
                List<String> ips = clientFilesMap.get(requestInfoToFind);
                for (String clientIP : ips) {
                    message += clientIP + " : " + clientBlockFilesMap.get(requestInfoToFind) + "\n";
                }
                try {
                    message += "$";
                    OutputStream outputStream = clientSocket.getOutputStream();
                    byte[] bytesToSend = message.getBytes(StandardCharsets.UTF_8);
                    outputStream.write(bytesToSend);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                String message = "1;" + "No clients have the file with the name: " + requestInfoToFind + "$";
                try {
                    OutputStream outputStream = clientSocket.getOutputStream();
                    byte[] bytesToSend = message.getBytes(StandardCharsets.UTF_8);
                    outputStream.write(bytesToSend);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(requestType.equals("5")){
            removeExistingEntriesForIP(ip);
            System.out.println("Updated clientBlockFilesMap: " + clientBlockFilesMap);
        }
    }

    private void removeExistingEntriesForIP(String ip) {
        clientBlockFilesMap.values().forEach(blocksList -> blocksList.removeIf(block -> block.endsWith("/" + ip)));
        clientBlockFilesMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}