import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.Scanner;

import Client.Mediator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import pack.FMethods;

public class Client {

    private static final String SERVER_ADDRESS = "10.0.0.10"; // Server IP address
    private static final int SERVER_PORT = 9090; // Server port

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String message;

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream()) {

            System.out.println("Connected to server.");

            // Get the client IP
            String clientIp = socket.getInetAddress().getHostAddress();
            clientIp = FMethods.transformToFullIP(clientIp);

            // RT ; IP ; Payload ?
            // RT -> Request Type = 1 byte
            // IP = 15 bytes
            // Payload -> File_name ! nº_blocks : File_name ! nº_blocks
            // ? -> Delimitador Final

            StringBuilder messageBuilder = new StringBuilder();
            StringBuilder messageBuilderBlocks = new StringBuilder();
            messageBuilder.append("1").append(";").append(clientIp).append(";");
            messageBuilderBlocks.append("2").append(";").append(clientIp).append(";");

            File clientFilesFolder = new File("ClientFiles");
            File[] files = clientFilesFolder.listFiles();

            // Fragmentação dos ficheiros em blocos

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && !file.getName().contains("«")) {
                        String fileName = file.getName();
                        String path = "./ClientFiles/" + fileName;
                        Path pathFile = Paths.get("./ClientFiles/" + fileName);
                        long fileSize = Files.size(pathFile);

                        int numBlocks;
                        if (fileSize % 1007 == 0) {
                            numBlocks = (int) (fileSize / 1007);
                        } else {
                            numBlocks = ((int) (fileSize / 1007)) + 1;
                        }

                        int blocksNumber = FMethods.fileSplitter(fileName, path, numBlocks);
                        System.out.println("O ficheiro " + fileName + " foi fragmentado em " + blocksNumber + " blocos");

                    }
                }
            }

            // -------------------------------------------------------------
            // Parse and send the client's files to the server
            files = clientFilesFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    // checks if file name contains »« so it considers them as a block of a file
                    if (file.isFile() && !file.getName().contains("«")) {
                        String fileName = file.getName();
                        Path path = Paths.get("./ClientFiles/" + fileName);
                        long fileSize = Files.size(path);
                        int numBlocks;
                        if (fileSize % 1007 == 0) {
                            numBlocks = (int) (fileSize / 1007);
                        } else {
                            numBlocks = ((int) (fileSize / 1007)) + 1;
                        }

                        messageBuilder.append(fileName).append("!").append(numBlocks).append(":");
                    } else {
                        String fileName = file.getName();
                        messageBuilderBlocks.append(fileName).append("|");
                    }
                }

                messageBuilder.deleteCharAt(messageBuilder.length() - 1);

                message = messageBuilder.toString();

                // debug
                System.out.println(message);

                byte[] ack = message.getBytes(StandardCharsets.UTF_8);
                outputStream.write(ack);
                outputStream.flush();

                message = messageBuilderBlocks.toString();

                // debug
                System.out.println(message);

                byte[] ack2 = message.getBytes(StandardCharsets.UTF_8);
                outputStream.write(ack2);
                outputStream.flush();

            } else {
                System.out.println("No files found in the 'ClientFiles' folder.");
            }

            // Create a thread for the Mediator functionality
            Thread mediatorThread = new Thread(new Mediator(inputStream));
            mediatorThread.start(); // Start the thread
            // -------------------------------------------------------------

            // Start a loop to allow the user to choose options
            while (true) {
                System.out.println("Menu:");
                System.out.println("1. Ask for a file location");
                System.out.println("2. Download a file");
                System.out.println("3. Exit");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                if (choice == 1) {
                    System.out.println("Enter the file name:");
                    String file = scanner.nextLine();
                    message = "4" + ";" + clientIp + ";" + file;
                    byte[] userRequestBytes = message.getBytes(StandardCharsets.UTF_8);
                    outputStream.write(userRequestBytes);
                    outputStream.flush();
                } else if (choice == 2) {
                    // Add code for file download option
                    System.out.println("Enter the file name:");
                    String file = scanner.nextLine();
                    message = "3" + ";" + clientIp + ";" + file;
                    byte[] userRequestBytes = message.getBytes(StandardCharsets.UTF_8);
                    outputStream.write(userRequestBytes);
                    outputStream.flush();
                } else if (choice == 3) {
                    break;
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            }

            System.out.println("Closing connection.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
