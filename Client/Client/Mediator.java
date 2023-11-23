package Client;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import CPackage.FileMethods;
import CPackage.GenericMethods;

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

                    if (firstByte.equals("1")) {
                        // Handle request type "1" - Message from Server
                        String receivedData = new String(buffer, 2, bytesRead - 2, StandardCharsets.UTF_8);
                        Thread.sleep(100);
                        System.out.println("Message from Server: " + receivedData);
                    } else if (firstByte.equals("2")) {
                        // Handle request type "2" - Update blocks information
                        String receivedData = new String(buffer, 2, bytesRead - 2, StandardCharsets.UTF_8);

                        // Parse blocks to update the list of people with blocks
                        String[] data = receivedData.split("%");
                        String fileName = data[1];
                        String myIP = data[2];
                        String totalBlocksString = data[3];
                        totalBlocksString = totalBlocksString.replaceAll("\n", "");
                        if (totalBlocksString.equals("")) {
                            System.out.println("File can't be downloaded because there's not enough info on the server.");
                            continue;
                        }
                        int totalBlocks = Integer.parseInt(totalBlocksString);
                        String[] blocks = data[0].split("\\|");
                        Map<String, List<String>> clientsWithBlocks = new HashMap<>();

                        for (String block : blocks) {
                            String[] blockInfo = block.split("/");
                            if (blockInfo.length == 2) {
                                String blockNumber = blockInfo[0];
                                String ipAddress = blockInfo[1]; // Extracting IP address

                                // Store IP addresses associated with block numbers
                                clientsWithBlocks.computeIfAbsent(blockNumber, k -> new ArrayList<>()).add(ipAddress);
                            }
                        }

                        System.out.println("Blocks Information Updated: " + clientsWithBlocks);

                        String blockName = "diogo.txt«0001";

                        // choose best IP

                        long minTripTime = 100000;
                        String senderIP = "010.000.000.001";
                        java.net.InetAddress Inetip;
                        String toReceive="";
                        // remove the first char in myIP
                        myIP = myIP.substring(1);

                        // Iterate through the map to send a request to each block number
                        for (Map.Entry<String, List<String>> blockEntry : clientsWithBlocks.entrySet()) {
                            String blockNumber = blockEntry.getKey();
                            List<String> ipAddresses = blockEntry.getValue();
                            senderIP = ipAddresses.get(0);

                            // Send request to each IP that has the block
                            for (String ipAddress : ipAddresses) {
                                System.out.println("Sending request to IP: " + ipAddress);
                                myIP = GenericMethods.transformToFullIP(myIP);
                                ipAddress = GenericMethods.transformToFullIP(ipAddress);
                                toReceive = "3" + myIP + ipAddress;
                                // remove all /n from toReceive
                                toReceive = toReceive.replaceAll("\n", "");
                                InetAddress inetAddress = InetAddress.getByName(ipAddress);
                                byte[] receive = toReceive.getBytes(StandardCharsets.UTF_8);
                                DatagramPacket packet = new DatagramPacket(receive, receive.length, inetAddress, 9090);
                                udpSocket.send(packet);

                                Thread.sleep(50);

                                // Access the tripTime value immediately after sending the datagram
                                long tripTime = Worker.getTripTime();
                                System.out.println("Round-trip time received in Mediator: " + tripTime + " milliseconds");

                                // Update the minimum trip time and corresponding IP
                                if (tripTime < minTripTime) {
                                    minTripTime = tripTime;
                                    senderIP = ipAddress;
                                }
                            }

                            // Send the IP address and block name to the other node
                            blockName = fileName + "«" + blockNumber;
                            toReceive = "2" + myIP + blockName;
                            byte[] receive = toReceive.getBytes(StandardCharsets.UTF_8);

                            Inetip = java.net.InetAddress.getByName(senderIP);
                            DatagramPacket packet = new DatagramPacket(receive, receive.length, Inetip, 9090);
                            udpSocket.send(packet);
                        }

                        Thread.sleep(200);
                        // Count if there's all the blocks then recreate the file
                        FileMethods.recreateFile(fileName, totalBlocks);

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
