package CPackage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileMethods {

    public static int fileSplitter(String fileName, String filePath, int numBlocks) {
        String outputDir = "./Blocks/";
        int blockSize = 962;

        File inputFile = new File(filePath);

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // change later
            byte[] buffer = new byte[50000];
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

    public static void fragmentAndSendInfo(String clientIp, OutputStream outputStream)
            throws IOException {
        StringBuilder messageBuilder = new StringBuilder();
        StringBuilder messageBuilderBlocks = new StringBuilder();
        messageBuilder.append("1").append(";").append(clientIp).append(";");
        messageBuilderBlocks.append("2").append(";").append(clientIp).append(";");
        String message;
        File clientFilesFolder = new File("ClientFiles");
        File[] files = clientFilesFolder.listFiles();

        if (files != null && files.length > 0) {
            // First loop: Fragmentation without considering blocks
            for (File file : files) {
                if (file.isFile() && !file.getName().contains("«")) {
                    String fileName = file.getName();
                    String path = "./ClientFiles/" + fileName;
                    Path pathFile = Paths.get("./ClientFiles/" + fileName);
                    long fileSize = Files.size(pathFile);

                    int numBlocks;
                    if (fileSize % 962 == 0) {
                        numBlocks = (int) (fileSize / 962);
                    } else {
                        numBlocks = ((int) (fileSize / 962)) + 1;
                    }

                    int blocksNumber = FileMethods.fileSplitter(fileName, path, numBlocks);
                    System.out
                            .println("The file " + fileName + " has been fragmented into " + blocksNumber + " blocks");

                    // Append to the appropriate message builder
                    messageBuilder.append(fileName).append("!").append(numBlocks).append(":");
                }
            }

            // Send message for fragmentation
            if (messageBuilder.length() > 2) {
                messageBuilder.deleteCharAt(messageBuilder.length() - 1);
                message = messageBuilder.toString();
                System.out.println(message); // debug
                byte[] ack = message.getBytes(StandardCharsets.UTF_8);
                outputStream.write(ack);
                outputStream.flush();
            }

            clientFilesFolder = new File("Blocks");
            files = clientFilesFolder.listFiles();
            // Second loop: Search for blocks
            for (File file : files) {
                if (file.isFile() && file.getName().contains("«")) {
                    // Handle files with '«' in their name for block information
                    messageBuilderBlocks.append(file.getName()).append("|");
                }
            }

            // Send message for block information
            if (messageBuilderBlocks.length() > 2) {
                messageBuilderBlocks.deleteCharAt(messageBuilderBlocks.length() - 1);
                message = messageBuilderBlocks.toString();
                System.out.println(message); // debug
                byte[] ack2 = message.getBytes(StandardCharsets.UTF_8);
                outputStream.write(ack2);
                outputStream.flush();
            }
        } else {
            System.out.println("No files found in the 'ClientFiles' folder.");
        }
    }

    public static void recreateFile(String fileName, int numBlocks) {
        String inputDir = "./Blocks/";
        String outputDir = "./ClientFiles/";

        // Check if files with names from "filename«0001" up to "filename«blocknumber"
        // are present
        boolean allBlocksPresent = true;
        for (int i = 1; i <= numBlocks; i++) {
            String blockFileName = String.format("%s«%04d", fileName, i);
            File blockFile = new File(inputDir + blockFileName);
            if (!blockFile.exists()) {
                allBlocksPresent = false;
                break;
            }
        }

        if (allBlocksPresent) {
            try (FileOutputStream fos = new FileOutputStream(outputDir + fileName)) {
                for (int i = 1; i <= numBlocks; i++) {
                    String blockFileName = String.format("%s«%04d", fileName, i);
                    try (FileInputStream fis = new FileInputStream(inputDir + blockFileName)) {
                        byte[] buffer = new byte[962];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                System.out.println("File " + fileName + " recreated successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Error: Not all blocks of the file are present in the folder.");
        }
    }

    // // Delete all blocks of the file
    // for (File blockFile : files) {
    // if (!blockFile.delete()) {
    // System.err.println("Failed to delete block file: " + blockFile.getName());
    // }
    // }

    public static int findNullByteIndex(byte[] array) {
        int index = 0;
        while (index < array.length && array[index] != 0) {
            index++;
        }
        return index;
    }

    public static byte[] generateMD5(byte[] data, int length) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data, 0, length); // Update with only the actual data
        return md.digest();
    }
}
