package Client;
import CPackage.*;

import java.nio.charset.StandardCharsets;

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
                UDPMethods.parseFileReceiveRequest(data);
                break;
            case "2":
                UDPMethods.parseFileSendRequest(data);
                break;
            default:
                System.out.println("Invalid byte.");
                break;
        }
    }
}
