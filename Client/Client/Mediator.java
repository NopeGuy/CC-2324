package Client;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mediator implements Runnable {
    private InputStream clientInput;
    private DatagramSocket udpSocket;

    public Mediator(InputStream clientInputStream) {
        this.clientInput = clientInputStream;

        try {
            this.udpSocket = new DatagramSocket(9090);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("Mediator is listening...");

            // Create a worker thread to handle UDP reception
            new Thread(() -> {
                while (true) {
                    try {
                        byte[] udpBuffer = new byte[1024];
                        DatagramPacket udpPacket = new DatagramPacket(udpBuffer, udpBuffer.length);
                        udpSocket.receive(udpPacket);

                        // Create a new worker thread for each UDP connection
                        Thread udpWorkerThread = new Thread(new Worker(udpPacket.getData()));
                        udpWorkerThread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            while (true) {
                byte[] buffer = new byte[1024];
                int bytesRead = clientInput.read(buffer);

                if (bytesRead != -1) {
                    String firstByte = new String(buffer, 0, 1, StandardCharsets.UTF_8);

                    // Debug
                    System.out.println("First Byte: " + firstByte);

                    if (firstByte.equals("1")) {
                        // Handle request type "1" - Message from Server
                        String receivedData = new String(buffer, 2, bytesRead - 2, StandardCharsets.UTF_8);
                        Thread.sleep(100);
                        System.out.println("Message from Server: " + receivedData);
                    } else if (firstByte.equals("2")) {
                        // Handle request type "2" - Update blocks information
                        String receivedData = new String(buffer, 2, bytesRead - 2, StandardCharsets.UTF_8);
                        System.out.println("Received data: " + receivedData);

                        // Parse blocks to update the list of people with blocks
                        String[] blocks = receivedData.split("\\|");
                        Map<String, List<String>> clientsWithBlocks = new HashMap<>();

                        for (String block : blocks) {
                            String[] blockInfo = block.split("/");
                            if (blockInfo.length == 2) {
                                String blockNumber = blockInfo[0];
                                String ipAddress = blockInfo[1]; // Extracting IP address

                                // Store blocks associated with IP addresses
                                clientsWithBlocks.computeIfAbsent(ipAddress, k -> new ArrayList<>()).add(blockNumber);
                            }
                        }

                        // Now clientsWithBlocks contains the IP addresses and their associated blocks
                        System.out.println("Blocks Information Updated: " + clientsWithBlocks);

                        // Test change later to get the IP address of the best nodes to download each block
                        // Iterate to send the best for each block
                        //__________________________________________________________________________________
                        String IP = "010.000.000.002";
                        String SenderIP = "010.000.000.001";
                        // transform IP to inet address
                        java.net.InetAddress Inetip = java.net.InetAddress.getByName(SenderIP);
                        String blockName = "diogo.txt«0001";
                        //__________________________________________________________________________________

                        // Send the IP address and block name to the other node
                        String toReceive = "2" + IP + blockName;
                        byte[] receive = toReceive.getBytes(StandardCharsets.UTF_8);
                        // Send message to other node to start up the sending process
                        DatagramPacket packet = new DatagramPacket(receive, receive.length, Inetip, 9090);
                        udpSocket.send(packet);
                        //__________________________________________________________________________________
                        
                    } else {
                        System.out.println("Invalid header format.");
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
