package CPackage;

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

public class UDPMethods {

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
            System.out.println("Filename inside FileSender: " + fileName);

            // Ensure the filename length is exactly 30 characters
            fileName = GenericMethods.padString(fileName, 30);
            System.out.println("Filename inside after padding FileSender: " + fileName);

            // Create header with requestType;IP;HashCode;Filename
            byte[] ipBytes = Arrays.copyOf(ip.getBytes(StandardCharsets.UTF_8), 15);
            byte[] fileNameBytes = Arrays.copyOf(fileName.getBytes(StandardCharsets.UTF_8), 30);

            // Calculate the total length of the header
            int headerLength = 1 + ipBytes.length + hashCode.length + fileNameBytes.length;
            System.out.println("Header length: " + headerLength);

            // Combine header and file data
            byte[] dataToSendBytes = new byte[headerLength + fileData.length];

            // Add requestType
            dataToSendBytes[0] = '1';

            // Add IP
            System.arraycopy(ipBytes, 0, dataToSendBytes, 1, 15);

            // Add fileName
            System.arraycopy(fileNameBytes, 0, dataToSendBytes, 16, 30);

            // Add hashCode
            System.arraycopy(hashCode, 0, dataToSendBytes, 46, 16);

            // Add fileData
            System.arraycopy(fileData, 0, dataToSendBytes, headerLength, fileData.length);

            // Send UDP packet with file data
            DatagramPacket packet = new DatagramPacket(dataToSendBytes, dataToSendBytes.length, receiverAddress, 9090);
            socket.send(packet);

            System.out.println("Sent file to IP: " + ip);

            socket.close();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void FileReceiver(String filePath, byte[] hashCode, byte[] payload) {
        try {
            // Find the actual length of the payload by searching for the first zero byte
            int payloadLength = FileMethods.findNullByteIndex(payload);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(payload, 0, payloadLength);
                System.out.println("File received and saved: " + filePath);
            }

            // Check if the received hash code matches the expected hash code
            if (Arrays.equals(FileMethods.generateMD5(payload, payloadLength), hashCode)) {
                System.out.println("Received file hash code is gucci.");
            } else {
                System.out.println("Received file hash code doesn't match the expected hash code.");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void parseFileReceiveRequest(byte[] data) {
        String ip = new String(data, 1, 15, StandardCharsets.UTF_8).trim();
        System.out.println("IP: " + ip);

        String fileName = new String(data, 16, 30, StandardCharsets.UTF_8).trim();
        System.out.println("FileName: " + fileName);

        byte[] hash = new byte[16];
        System.arraycopy(data, 46, hash, 0, 16);

        int payloadLength = data.length - 62;
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, 62, payload, 0, payloadLength);

        String teste = new String(payload, 0, 20, StandardCharsets.UTF_8).trim();
        System.out.println("Teste: " + teste);

        String filePath = "./ClientFiles/" + fileName;
        FileReceiver(filePath, hash, payload);
    }

    public static void parseFileSendRequest(byte[] data) {
        String ip = new String(data, 1, 15, StandardCharsets.UTF_8).trim();
        System.out.println("IP: " + ip);

        String fileName = new String(data, 16, data.length - 16, StandardCharsets.UTF_8).trim();
        System.out.println("File Name in worker request 2: " + fileName);

        String filePath = "./ClientFiles/" + fileName;
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
            String ipAddress = new String(data, 1, 15, StandardCharsets.UTF_8);
            byte[] timestampBytes = new byte[8];
            System.arraycopy(data, 16, timestampBytes, 0, 8);
            long timestamp = GenericMethods.bytesToLong(timestampBytes);

            // Calculate the round-trip time
            tripTime = System.currentTimeMillis() - timestamp;

            // Print the round-trip time
            System.out.println("RTT for IP " + ipAddress + ": " + tripTime + " milliseconds");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tripTime;
    }
}
