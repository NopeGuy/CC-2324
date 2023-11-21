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
    private Thread udpWorkerThread;

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
            udpWorkerThread = new Thread(() -> {
                while (true) {
                    try {
                        byte[] udpBuffer = new byte[1024];
                        DatagramPacket udpPacket = new DatagramPacket(udpBuffer, udpBuffer.length);
                        udpSocket.receive(udpPacket);

                        // Pass UDP data directly to Worker class
                        new Worker(udpPacket.getData()).run();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            udpWorkerThread.start();

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
                            String[] blockInfo = block.split("//");
                            if (blockInfo.length == 2) {
                                String blockNumber = blockInfo[0];
                                String ipAddress = blockInfo[1]; // Extracting IP address

                                // Store blocks associated with IP addresses
                                clientsWithBlocks.computeIfAbsent(ipAddress, k -> new ArrayList<>()).add(blockNumber);
                            }
                        }

                        // Now clientsWithBlocks contains the IP addresses and their associated blocks
                        System.out.println("Blocks Information Updated: " + clientsWithBlocks);



                        // Iterate clients with blocks to get all the IP's that have and  send a request 3 to those addresses

                        
                        // Test change later to get the IP address of the best nodes to download each block
                        // Iterate to send the best for each block
                        //__________________________________________________________________________________
                        String IP = "010.000.000.002";
                        String blockName = "diogo.txt«0001";
                        //__________________________________________________________________________________

                        // choose best IP
                                                
                        long minTripTime = 100000;
                        String SenderIP = "010.000.000.001";
                        java.net.InetAddress Inetip;

                        for (Map.Entry<String, List<String>> entry : clientsWithBlocks.entrySet()) {
                            String key = entry.getKey();
                            List<String> value = entry.getValue();
                            System.out.println("Key: " + key + " Value: " + value);

                            String toReceive = "3" + IP + key;
                            Inetip = java.net.InetAddress.getByName(key);
                            byte[] receive = toReceive.getBytes(StandardCharsets.UTF_8);
                            DatagramPacket packet = new DatagramPacket(receive, receive.length, Inetip, 9090);
                            udpSocket.send(packet);

                            Thread.sleep(100);

                            // Access the tripTime value immediately after sending the datagram
                            long tripTime = Worker.getTripTime();
                            System.out.println("Round-trip time received in Mediator: " + tripTime + " milliseconds");

                            // Update the minimum trip time and corresponding IP
                            if (tripTime < minTripTime) {
                                minTripTime = tripTime;
                                SenderIP = key;
                            }
                        }
                        // Send the IP address and block name to the other node
                        String toReceive = "2" + IP + blockName;
                        byte[] receive = toReceive.getBytes(StandardCharsets.UTF_8);
                        // Send message to other node to start up the sending process
                        Inetip = java.net.InetAddress.getByName(SenderIP);
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
