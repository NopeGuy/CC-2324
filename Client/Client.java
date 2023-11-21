import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.Scanner;

import Client.Mediator;

import java.io.File;

import CPackage.*;

public class Client {

    private static final String SERVER_ADDRESS = "10.0.0.10"; // Server IP address
    private static final int SERVER_PORT = 9090; // Server port

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream()) {

            System.out.println("Connected to server.");

            // Get the client IP
            String clientIp = socket.getLocalAddress().toString();
            clientIp = GenericMethods.transformToFullIP(clientIp);
            System.out.println("Client IP: " + clientIp);


            File clientFilesFolder = new File("ClientFiles");
            FileMethods.fragmentAndSendInfo(clientFilesFolder,clientIp, outputStream);

            // Create a thread for the Mediator functionality
            Thread mediatorThread = new Thread(new Mediator(inputStream));
            mediatorThread.start(); // Start the thread

            // Start a loop to allow the user to choose options
            while (true) {
                System.out.println("Menu:");
                System.out.println("1. Ask for a file location");
                System.out.println("2. Download a file");
                System.out.println("3. Exit");

                String message;
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
