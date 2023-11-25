package CPackage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import Client.Worker;

public class UDPMethods {

    public static void DownloadStart(String myIP, String fileName, Map<String, List<String>> clientsWithBlocks, DatagramSocket udpSocket) throws InterruptedException, IOException {
        long minTripTime = 100000;
        String senderIP = "010.000.000.001";
        String toReceive;

        // Iterate through the map to send a request to each block number
        for (Map.Entry<String, List<String>> blockEntry : clientsWithBlocks.entrySet()) {
            String blockNumber = blockEntry.getKey();
            List<String> ipAddresses = blockEntry.getValue();
            senderIP = ipAddresses.get(0);

            // Check if the block already exists in the Blocks folder
            String blockFileName = fileName + "«" + blockNumber;
            if (blockExists(blockFileName)) {
                System.out.println("Block " + blockFileName + " already exists. Skipping download.");
                continue;
            }

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
            String blockName = fileName + "«" + blockNumber;
            toReceive = "2" + myIP + blockName;
            byte[] receive = toReceive.getBytes(StandardCharsets.UTF_8);

            InetAddress Inetip = InetAddress.getByName(senderIP);
            DatagramPacket packet = new DatagramPacket(receive, receive.length, Inetip, 9090);
            udpSocket.send(packet);
        }
    }

    // Helper method to check if the block file already exists
    private static boolean blockExists(String blockFileName) {
        File file = new File("Blocks", blockFileName);
        return file.exists();
    }

    public static void FileSender(String filePath, String ip) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress receiverAddress = InetAddress.getByName(ip);

            // Read file into bytes
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));

            // Hash the file content
            byte[] hashCode = FileMethods.generateMD5(fileData, fileData.length);

            // Extract the filename from the full path
            String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);

            // Ensure the filename length is exactly 30 characters
            fileName = GenericMethods.padString(fileName, 30);

            // Create header with requestType;IP;HashCode;Filename
            byte[] ipBytes = Arrays.copyOf(ip.getBytes(StandardCharsets.UTF_8), 15);
            byte[] fileNameBytes = Arrays.copyOf(fileName.getBytes(StandardCharsets.UTF_8), 30);

            // Combine header and file data
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

            // Write requestType
            dataOutputStream.writeByte('1');

            // Write IP
            dataOutputStream.write(ipBytes);

            // Write fileName
            dataOutputStream.write(fileNameBytes);

            // Write hashCode
            dataOutputStream.write(hashCode);

            // Write fileData
            dataOutputStream.write(fileData);

            byte[] dataToSendBytes = byteArrayOutputStream.toByteArray();
            // Size of the data to send
            int dataToSendLength = dataToSendBytes.length;
            System.out.println("Data to send length: " + dataToSendLength);

            // Send UDP packet with file data
            DatagramPacket packet = new DatagramPacket(dataToSendBytes, dataToSendBytes.length, receiverAddress, 9090);
            socket.send(packet);

            System.out.println("Sent file to IP: " + ip);

            socket.close();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static Boolean FileReceiver(String filePath, byte[] hashCode, byte[] payload) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(payload);
            System.out.println("File received and saved: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check if the received hash code matches the expected hash code
        try {
            if (Arrays.equals(FileMethods.generateMD5(payload, payload.length), hashCode)) {
                System.out.println("Received file hash code is gucci.");
                return true;
            } else {
                System.out.println("Received file hash code doesn't match the expected hash code.");
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Boolean parseFileReceiveRequest(byte[] data) {
        String fileName = new String(data, 16, 30, StandardCharsets.UTF_8).trim();

        byte[] hash = new byte[16];
        System.arraycopy(data, 46, hash, 0, 16);

        int payloadLength = data.length - 62;
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, 62, payload, 0, payloadLength);

        String filePath = "./Blocks/" + fileName;
        return FileReceiver(filePath, hash, payload);
    }

    public static void parseFileSendRequest(byte[] data) {
        String ip = new String(data, 1, 15, StandardCharsets.UTF_8).trim();

        String fileName = new String(data, 16, data.length - 16, StandardCharsets.UTF_8).trim();

        String filePath = "./Blocks/" + fileName;
        FileSender(filePath, ip);
    }

    public static void RTTRequest(byte[] data) throws SocketException {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            try {
                // Assuming the data contains the IP address of the receiver
                String ReturnIP = new String(data, 1, 15, StandardCharsets.UTF_8);
                String MyIP = new String(data, 16, 15, StandardCharsets.UTF_8);

                // Create an RTTRequest packet with the sender's IP and current time
                String requestType = "4";
                long currentTime = System.currentTimeMillis();
                byte[] currentTimeBytes = GenericMethods.longToBytes(currentTime);
                String packetData = requestType + MyIP;

                byte[] requestData = new byte[24];
                System.arraycopy(packetData.getBytes(StandardCharsets.UTF_8), 0, requestData, 0, packetData.length());
                System.arraycopy(currentTimeBytes, 0, requestData, 16, 8);

                DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length,
                        InetAddress.getByName(ReturnIP), 9090);

                // Send the RTTRequest packet
                udpSocket.send(requestPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static long RTTResponse(byte[] data) {
        long tripTime = -1;
        try {
            // Assuming the data contains the IP address of the sender and a timestamp
            // String ipAddress = new String(data, 1, 15, StandardCharsets.UTF_8);
            byte[] timestampBytes = new byte[8];
            System.arraycopy(data, 16, timestampBytes, 0, 8);
            long timestamp = GenericMethods.bytesToLong(timestampBytes);

            // Calculate the round-trip time
            tripTime = System.currentTimeMillis() - timestamp;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tripTime;
    }
}
