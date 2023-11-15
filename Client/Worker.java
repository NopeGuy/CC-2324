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
        String ip, fileName;
        System.out.println("Worker activated");
        String request = new String(data, 0, 1, StandardCharsets.UTF_8);

        switch (request) {
            case "1": // RT;IP;Hash;FileName;Payload // Receive file

                // extract the IP address from the data
                ip = new String(data, 2, 15, StandardCharsets.UTF_8).trim();
                System.out.println("IP: " + ip);
                // extract the Hash from the data which is in bytes
                hash = new byte[16];
                System.arraycopy(data, 18, hash, 0, 16);
                // extract the file name from the data
                fileName = new String(data, 35, 15, StandardCharsets.UTF_8).trim();
                System.out.println(fileName);
                byte[] payload = new byte[data.length - 50];
                System.arraycopy(data, 51, payload, 0, data.length - 51);

                filePath = "./ClientFiles/" + fileName;
                FileReceiver(filePath, ip, hash, payload);
                break;

            case "2": // RT;IPSender;Hash;payload = 2+1+15+1+32+1+972 // send file
                // extract the IP address from the data
                ip = new String(data, 2, 15, StandardCharsets.UTF_8).trim();
                System.out.println("IP: " + ip);
                // extract the file name which is 30 bytes
                fileName = new String(data, 18, 15, StandardCharsets.UTF_8).trim();
                System.out.println("File Name: " + fileName);

                filePath = "./ClientFiles/" + fileName;
                FileSender(filePath, ip);
                break;

            default:
                System.out.println("Invalid byte.");
                break;
        }
    }
}