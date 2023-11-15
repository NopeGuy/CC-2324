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
        int blockSize = 1007; // Tamanho do bloco em bytes

        File inputFile = new File(filePath);

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            byte[] buffer = new byte[blockSize];
            int bytesRead;
            int blockNumber = 1; // Inicia o número do bloco em 1

            long fileSize = inputFile.length(); // Obtém o tamanho real do arquivo

            for (int i = 0; i < numBlocks - 1; i++) {
                bytesRead = fis.read(buffer, 0, blockSize);

                String blockFileName = String.format(outputDir + "%s«%04d", fileName, blockNumber);
                try (FileOutputStream fos = new FileOutputStream(blockFileName)) {
                    fos.write(buffer, 0, bytesRead);
                }

                blockNumber++;
            }

            // Trata o último bloco
            int remainingBytes = (int) (fileSize % blockSize);
            bytesRead = fis.read(buffer, 0, remainingBytes);

            String lastBlockFileName = String.format(outputDir + "%s«%04d", fileName, blockNumber);
            try (FileOutputStream fos = new FileOutputStream(lastBlockFileName)) {
                fos.write(buffer, 0, bytesRead);
            }

            System.out.println("File split into " + numBlocks + " blocks.");
            return numBlocks;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return (-1);
    }

    public static void FileSender(String filePath, String ip) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress receiverAddress = InetAddress.getByName(ip);

            // Read file into bytes
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));

            // Hash the file content
            byte[] hashCode = generateMD5(fileData);

            //String Filename
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            //fill the name until it occupies 30 bytes
            while (fileName.length() < 15) {
                fileName += " ";
            }

            // Create header with requestType;IP;HashCode;Filename
            String header = String.format("1;%s;%s;%s;", ip, hashCode, fileName);

            // Combine header and file data
            byte[] dataToSendBytes = new byte[header.length() + fileData.length];
            System.arraycopy(header.getBytes(StandardCharsets.UTF_8), 0, dataToSendBytes, 0, header.length());
            System.arraycopy(fileData, 0, dataToSendBytes, header.length(), fileData.length);

            // Send UDP packet with file data
            DatagramPacket packet = new DatagramPacket(dataToSendBytes, dataToSendBytes.length, receiverAddress, 9090);
            socket.send(packet);
		
	    System.out.println(fileName);
            System.out.println("Sent file to IP: " + ip);

            socket.close();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void FileReceiver(String filePath, String ip, byte[] hashCode, byte[] payload) {
        try {
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(payload);
                    System.out.println("File received and saved: " + filePath);
                }

            // Check if the received hash code matches the expected hash code
            if (Arrays.equals(generateMD5(payload), hashCode)) {
                // Write payload (file content) to the specified file path

            } else {
                System.out.println("Received file hash code doesn't match the expected hash code.");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // Helper method to generate MD5 hash
    private static byte[] generateMD5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(data);
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
