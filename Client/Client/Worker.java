package Client;
import CPackage.*;

import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class Worker implements Runnable {
    private byte[] data;
    private static long tripTime;
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
            case "3":
                try {
                    UDPMethods.RTTRequest(data);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                break;
            case "4":
                tripTime = UDPMethods.RTTResponse(data);
                setTripTime(tripTime);
                break;
            default:
                System.out.println("Invalid byte.");
                break;
        }
    }

    // New method to set the tripTime field
    public void setTripTime(long tripTime) {
        Worker.tripTime = tripTime;
    }

    // New method to get the tripTime value
    public static long getTripTime() {
        return tripTime;
    }
}
