import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Map<String, List<String>> clientFilesMap;
    private Map<String, List<String>> clientBlockFilesMap;

    public ClientHandler(Socket clientSocket, Map<String, List<String>> clientFilesMap,
            Map<String, List<String>> clientBlockFilesMap) {
        this.clientSocket = clientSocket;
        this.clientFilesMap = clientFilesMap;
        this.clientBlockFilesMap = clientBlockFilesMap;
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String receivedString = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                String[] parts = receivedString.split(";");

                if (parts.length < 3) {
                    System.err.println("Invalid header format.");
                    continue;
                }

                String requestType = parts[0];
                String ip = parts[1];
                String requestInfo = parts[2];

                if (requestType.equals("1")) {
                    String[] fileswithsize = requestInfo.split(":");
                    for (String file : fileswithsize) {
                        String[] filesName = file.split("!");
                        
                        // Update clientFilesMap to associate IP with files
                        if (!clientFilesMap.containsKey(filesName[0])) {
                            clientFilesMap.put(filesName[0], new ArrayList<>());
                        }
                        List<String> files = clientFilesMap.get(filesName[0]);
                        files.add(ip);
                        clientFilesMap.put(filesName[0], files);
                    }
                    System.out.println("1;" + "Received files for IP " + ip + ": " + clientFilesMap);
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

                    System.out.println("1;" + "Received blocks information for IP " + ip + ": " + clientBlockFilesMap);
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
                        clientsWithBlocks.append("|");
                        clientsWithBlocks.deleteCharAt(clientsWithBlocks.length() - 2); // Remove the last ", "
                        clientsWithBlocks.append("\n");
                    } else {
                        clientsWithBlocks.append("1;" + "No clients have blocks of file ").append(requestedFile).append("\n");
                    }

                    //debug
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

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
