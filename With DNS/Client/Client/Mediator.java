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
import java.util.concurrent.Semaphore;

import CPackage.FileMethods;
import CPackage.GenericMethods;
import CPackage.UDPMethods;

public class Mediator implements Runnable {
    private InputStream clientInput;
    private DatagramSocket udpSocket;
    private Thread udpWorkerThread;
        private Semaphore requestSemaphore;

    public Mediator(InputStream clientInputStream, Semaphore requestSemaphore) {
        this.clientInput = clientInputStream;
        this.requestSemaphore = requestSemaphore;
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
                byte[] buffer = new byte[20000];
                int bytesRead = clientInput.read(buffer);

                if (bytesRead != -1) {
                    String firstByte = new String(buffer, 0, 1, StandardCharsets.UTF_8);

                    if (firstByte.equals("1")) {
                        // Handle request type "1" - Message from Server
                        String receivedData = new String(buffer, 2, bytesRead - 2, StandardCharsets.UTF_8);
                        Thread.sleep(100);
                        System.out.println("Message from Server: " + receivedData);
                        requestSemaphore.release();

                    } else if (firstByte.equals("2")) {
                        // Handle request type "2" - Update blocks information
                        String receivedData = new String(buffer, 2, bytesRead - 2, StandardCharsets.UTF_8);
                        int tries = 0;
                        boolean tryAgain = true;
                        Map<String, Long> averageSpeeds = new HashMap<>();

                        // Parse blocks to update the list of people with blocks
                        System.out.println("Received blocks information: " + receivedData);
                        String[] data = receivedData.split("%");
                        String fileName = data[1];
                        String DNS = data[2];

                        // Transform DNS to IP
                        String myIP = GenericMethods.transformToFullIP(InetAddress.getByName(DNS).getHostAddress());

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

                        while (tries < 10 && tryAgain == true) {
                            tryAgain = false;

                            // Check all the download speeds and choose the fastest one
                            averageSpeeds = UDPMethods.calculateAverageTripTime(myIP, fileName, clientsWithBlocks, udpSocket);
                            Thread.sleep(3000);
                            System.out.println("Average speeds: " + averageSpeeds);

                            if (tries != 0) {
                                System.out.println("Download failed, retrying to download the file...\n");
                            }

                            tryAgain = UDPMethods.DownloadStart(myIP, fileName, clientsWithBlocks, averageSpeeds, udpSocket);

                            if (!tryAgain) {
                                tryAgain = !FileMethods.recreateFile(fileName, totalBlocks);
                            }

                            if (tryAgain && tries == 5) {
                                System.out.println("Download failed, maximum number of tries reached.\n");
                            }

                            tries++;
                        }
                        requestSemaphore.release();
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
