package pack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FMethods {
    public static int fileSplitter(String fileName, String filePath, int numBlocks) {
        String outputDir = "./ClientFiles/";
        int blockSize = 962;

        File inputFile = new File(filePath);

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            byte[] buffer = new byte[blockSize];
            int bytesRead;
            int blockNumber = 1;

            long fileSize = inputFile.length();

            for (int i = 0; i < numBlocks - 1; i++) {
                bytesRead = fis.read(buffer, 0, blockSize);

                String blockFileName = String.format("%s«%04d", fileName, blockNumber);
                try (FileOutputStream fos = new FileOutputStream(outputDir + blockFileName)) {
                    fos.write(buffer, 0, bytesRead);
                }

                blockNumber++;
            }

            int remainingBytes = (int) (fileSize % blockSize);
            bytesRead = fis.read(buffer, 0, remainingBytes);

            String lastBlockFileName = String.format("%s«%04d", fileName, blockNumber);
            try (FileOutputStream fos = new FileOutputStream(outputDir + lastBlockFileName)) {
                fos.write(buffer, 0, bytesRead);
            }

            System.out.println("File split into " + numBlocks + " blocks.");
            return numBlocks;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void FileSender(String filePath, String ip) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress receiverAddress = InetAddress.getByName(ip);

            // Read file into bytes
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));

            // Hash the file content
            byte[] hashCode = generateMD5(fileData, fileData.length);
            
            // Extract the filename from the full path
            String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
            System.out.println("Filename inside FileSender: " + fileName);

            // Ensure the filename length is exactly 30 characters
            fileName = padString(fileName, 30);
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
            int payloadLength = findNullByteIndex(payload);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(payload, 0, payloadLength);
                System.out.println("File received and saved: " + filePath);
            }

            // Check if the received hash code matches the expected hash code
            if (Arrays.equals(generateMD5(payload, payloadLength), hashCode)) {
                System.out.println("Received file hash code is gucci.");
            } else {
                System.out.println("Received file hash code doesn't match the expected hash code.");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // Helper method to find the index of the first zero byte in a byte array
    private static int findNullByteIndex(byte[] array) {
        int index = 0;
        while (index < array.length && array[index] != 0) {
            index++;
        }
        return index;
    }

    // Helper method to generate MD5 hash
    private static byte[] generateMD5(byte[] data, int length) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data, 0, length); // Update with only the actual data
        return md.digest();
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

    public static String padString(String originalString, int fixedSize) {
        // Ensure the string is not longer than the fixed size
        if (originalString.length() > fixedSize) {
            throw new IllegalArgumentException("String is too long");
        }

        // Pad the string to the right with spaces
        return String.format("%-" + fixedSize + "s", originalString);
    }
}
