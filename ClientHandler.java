import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Map<String, List<String>> clientFilesMap;
    private Map<String, List<String>> clientBlockFilesMap;
    List<String> files = new ArrayList<>();
    List<String> blocks = new ArrayList<>();

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

                // files
                if (requestType.equals("1")) {
                    String[] fileswithsize = requestInfo.split(":");
                    for (String file : fileswithsize) {
                        String[] filesName = file.split("!");
                        files.add(filesName[0]);
                    }
                    clientFilesMap.put(ip, files);
                    // debug
                    System.out.println("Received files for IP " + ip + ": " + files);
                }

                if (requestType.equals("2")) {

                }

                // info request
                // Inside the run() method where requestType 3 is handled
                if (requestType.equals("3")) {
                    String requestInfoToFind = requestInfo; // Request info to find

                    for (Map.Entry<String, List<String>> entry : clientFilesMap.entrySet()) {
                        String clientIP = entry.getKey();
                        List<String> clientFiles = entry.getValue();

                        // Check if the client has the requested file
                        if (clientFiles.contains(requestInfoToFind)) {
                            // Get the blocks associated with the requested file
                            List<String> fileBlocks = clientBlockFilesMap.get(requestInfoToFind);
                            String message;

                            if (fileBlocks != null && !fileBlocks.isEmpty()) {
                                // Prepare a message with file blocks information
                                message = "\nClient IP: " + clientIP + " has the requested file: " + requestInfoToFind
                                        + "\n";
                                message += "Blocks: " + fileBlocks + "\n";
                            } else {
                                message = "\nClient IP: " + clientIP
                                        + " has the requested file but no associated blocks info available: "
                                        + requestInfoToFind + "\n";
                            }

                            // Send the message containing file existence and blocks info to the client
                            byte[] bytesToSend = message.getBytes(StandardCharsets.UTF_8);
                            outputStream.write(bytesToSend);
                            outputStream.flush();
                        } else {
                            String message = "\nThere's no file with the name: " + requestInfoToFind + "\n";
                            byte[] bytesToSend = message.getBytes(StandardCharsets.UTF_8);
                            outputStream.write(bytesToSend);
                            outputStream.flush();
                        }
                    }
                }

                // blocks
                // Inside the run() method where requestType 4 is handled
                if (requestType.equals("4")) {
                    String[] blockInfo = requestInfo.split("\\|");

                    for (String block : blockInfo) {
                        String[] blockParts = block.split("«");
                        String fileName = blockParts[0];
                        String blockNumber = blockParts[1];

                        // Check if the file already exists in the map, if not, create a new entry
                        if (!clientBlockFilesMap.containsKey(fileName)) {
                            clientBlockFilesMap.put(fileName, new ArrayList<>());
                        }

                        // Get the blocks associated with the file and add the new block
                        List<String> blocks = clientBlockFilesMap.get(fileName);
                        blocks.add(blockNumber);

                        // Update the block information for this file
                        clientBlockFilesMap.put(fileName, blocks);
                    }

                    // debug
                    System.out.println("Received blocks information for IP " + ip + ": " + clientBlockFilesMap);
                }
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
