import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "127.0.0.1"; // Server IP address
    private static final int SERVER_PORT = 9090; // Server port

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             OutputStream outputStream = socket.getOutputStream()) {

            System.out.println("Connected to server.");

            //ter IP e enviar ao server
            String clientIp = socket.getLocalAddress().toString();
            String message = clientIp + ";" + "1" + ";" + "file1" + ";" + "file2";
            byte[] bytesToSend = message.getBytes(StandardCharsets.UTF_8);
            outputStream.write(bytesToSend);
            outputStream.flush();


            //consola de cliente para pedir ficheiros (A FAZER)
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("q to quit:");
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("q")) {
                    break;
                }

            }

            System.out.println("Closing connection.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
