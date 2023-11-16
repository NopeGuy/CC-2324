package Client;

import java.nio.charset.StandardCharsets;

import static pack.FMethods.FileReceiver;
import static pack.FMethods.FileSender;

public class Worker implements Runnable {
    private byte[] data;
    byte[] hash;
    String ip, filePath;

    public Worker(byte[] buffer) {
        this.data = buffer;
    }

    @Override
    public void run() {
        System.out.println("Worker activated");
        String request = new String(data, 0, 1, StandardCharsets.UTF_8);

        switch (request) {
            case "1":
                parseFileReceiveRequest();
                break;
            case "2":
                parseFileSendRequest();
                break;
            default:
                System.out.println("Invalid byte.");
                break;
        }
    }

    private void parseFileReceiveRequest() {
        // Extract the IP address from the data
        ip = new String(data, 1, 15, StandardCharsets.UTF_8).trim();
        System.out.println("IP: " + ip);

        // Extract the file name from the data
        String fileName = new String(data, 16, 30, StandardCharsets.UTF_8).trim();
        System.out.println("FileName: " + fileName);

        // Extract the Hash from the data which is in bytes
        hash = new byte[16];
        System.arraycopy(data, 46, hash, 0, 16);

        // Extract payload
        int payloadLength = data.length - 62;
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, 62, payload, 0, payloadLength);

        // Debug print
        String teste = new String(payload, 0, 20, StandardCharsets.UTF_8).trim();
        System.out.println("Teste: " + teste);

        filePath = "./ClientFiles/" + fileName;
        FileReceiver(filePath, hash, payload);
    }

    private void parseFileSendRequest() {
        // Extract the IP address from the data
        ip = new String(data, 1, 15, StandardCharsets.UTF_8).trim();
        System.out.println("IP: " + ip);

        // Extract the file name which is the rest of the data
        String fileName = new String(data, 16, data.length - 16, StandardCharsets.UTF_8).trim();
        System.out.println("File Name in worker request 2: " + fileName);

        filePath = "./ClientFiles/" + fileName;
        FileSender(filePath, ip);
    }
}
