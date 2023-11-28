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
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            // change later
            byte[] buffer = new byte[50000];
            int bytesRead;
            String ip = "";

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String receivedString = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                String[] parts = receivedString.split(";");

                if (parts.length < 3) {
                    System.err.println("Invalid header format.");
                    continue;
                }

                String requestType = parts[0];
                ip = parts[1];
                String requestInfo = parts[2];
                ip = transformToFullIP(ip);

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
                        clientsWithBlocks.append("\n");
                        System.out.println("Clients with blocks: " + clientsWithBlocks);
                    } else {
                        clientsWithBlocks.append("1;" + "No clients have blocks of file ").append(requestedFile).append("\n");
                    }

                    // debug
                    System.out.println("Clients with blocks: " + clientsWithBlocks);
                    byte[] responseBytes = clientsWithBlocks.toString().getBytes(StandardCharsets.UTF_8);
                    outputStream.write(responseBytes);
                    outputStream.flush();
                }

                if (requestType.equals("4")) {
                    String requestInfoToFind = requestInfo; // Request info to find

                    if (clientFilesMap.containsKey(requestInfoToFind)) {
                        String message = "1;" + "Client IPs having the file " + requestInfoToFind + ":\n";
                        List<String> ips = clientFilesMap.get(requestInfoToFind);
                        for (String clientIP : ips) {
                            message += clientIP + " : " + clientBlockFilesMap.get(requestInfoToFind) + "\n";
                        }
                        byte[] bytesToSend = message.getBytes(StandardCharsets.UTF_8);
                        outputStream.write(bytesToSend);
                        outputStream.flush();
                    } else {
                        String message = "1;" + "No clients have the file with the name: " + requestInfoToFind + "\n";
                        byte[] bytesToSend = message.getBytes(StandardCharsets.UTF_8);
                        outputStream.write(bytesToSend);
                        outputStream.flush();
                    }
                }
            }
            // Delete all the info of clientBlockFilesMap from that IP
            final String finalIp = ip;
            clientBlockFilesMap.values().forEach(blocksList -> blocksList.removeIf(block -> block.endsWith("/" + finalIp)));
            clientBlockFilesMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            System.out.println("Updated clientBlockFilesMap: " + clientBlockFilesMap);

            clientSocket.close();
            
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String transformToFullIP(String ip) {
        // Split the IP address into its segments
        String[] segments = ip.split("\\.");

        // Create a StringBuilder to build the transformed IP
        StringBuilder fullIP = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            // Pad each segment with leading zeros to make it three digits
            while (segment.length() < 3) {
                segment = "0" + segment;
            }

            // Append the formatted segment to the full IP
            fullIP.append(segment);

            // Add a dot to separate segments, but not after the last one
            if (i < segments.length - 1) {
                fullIP.append(".");
            }
        }

        return fullIP.toString();
    }
}
