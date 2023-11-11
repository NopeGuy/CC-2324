package pack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public static void FileSender(String filePath) {
        try {
            DatagramSocket socket = new DatagramSocket();
            int receiverPort = 9090;

            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);

            byte[] fileData = new byte[1024]; // Reading a sequence of 1024 bytes
            int bytesRead = fileInputStream.read(fileData);

            if (bytesRead >= 30) {
                // Extract the initial 30 bytes as "ip;data_to_send"
                byte[] ipDataBytes = new byte[30];
                byte[] dataToSendBytes = new byte[bytesRead - 30];
                System.arraycopy(fileData, 0, ipDataBytes, 0, 30);                System.arraycopy(fileData, 0, ipDataBytes, 0, 30);
                System.arraycopy(fileData, 31, dataToSendBytes, 0, bytesRead - 30);
                String ipData = new String(ipDataBytes); // Convert the bytes to a string

                // Perform other operations with the extracted dataToSend and IP
                // Example: Sending a UDP packet with the data to send

                InetAddress receiverAddress = InetAddress.getByName(ipData);
                DatagramPacket packet = new DatagramPacket(dataToSendBytes, dataToSendBytes.length, receiverAddress, receiverPort);
                socket.send(packet);
                System.out.println("Sent packet with the data to send to IP: " + ipData);

            } else {
                System.out.println("File doesn't contain enough bytes to extract IP and data to send.");
            }

            fileInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void FileReceiver(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(9090);
            byte[] buffer = new byte[1024];

            File file = new File("receivedFile.txt");
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                fileOutputStream.write(packet.getData(), 0, packet.getLength());

                if (packet.getLength() < buffer.length) {
                    break;
                }
            }

            fileOutputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String generateMD5(String filePath) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        byte[] hashBytes = md.digest(fileBytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
