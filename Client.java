import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "127.0.0.1"; // Server IP address
    private static final int SERVER_PORT = 9090; // Server port

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream()) {

            System.out.println("Connected to server.");

            // Get the client IP
            String clientIp = socket.getInetAddress().getHostAddress();

            String message = "1" + ";" + clientIp + ";" + "file1" + ";" + "file2";
            byte[] ack = message.getBytes(StandardCharsets.UTF_8);
            outputStream.write(ack);
            outputStream.flush();

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
                    message = "2" + ";" + clientIp + ";" + file;
                    byte[] userRequestBytes = message.getBytes(StandardCharsets.UTF_8);
                    outputStream.write(userRequestBytes);
                    outputStream.flush();

                    // Read and display the server's response
                    byte[] responseBuffer = new byte[1024];
                    int bytesRead = inputStream.read(responseBuffer);

                    if (bytesRead > 0) {
                        String response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
                        System.out.println(response);
                    }
                } else if (choice == 2) {
                    // Add code for file download option
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
