package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Mediator implements Runnable {
    private BufferedReader clientInput;

    public Mediator(InputStream clientInputStream) {
        this.clientInput = new BufferedReader(new InputStreamReader(clientInputStream));
    }

    @Override
    public void run() {
        try {
            Thread.sleep(100);
            System.out.println("Mediator is listening...");

            while (true) {
                String message = clientInput.readLine(); // Read the message from the server
                if (message != null) {
                    Thread.sleep(20);
                    System.out.println("Message from Server: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
