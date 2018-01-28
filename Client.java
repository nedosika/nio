import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
 
public class Client {
 
    public static void main(String args[]) {
        String host = "127.0.0.1";
        int port = 4444;
        new Client(host, port);
    }
 
    public Client(String host, int port) {
        try {
            String serverHostname = new String("127.0.0.1");
 
            System.out.println("Connecting to host " + serverHostname + " on port " + port + ".");
 
            Socket echoSocket = null;
            PrintWriter out = null;
            BufferedReader in = null;
 
            try {
                echoSocket = new Socket(serverHostname, 4444);
                out = new PrintWriter(echoSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
            } catch (UnknownHostException e) {
                System.err.println("Unknown host: " + serverHostname);
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Unable to get streams from server");
                System.exit(1);
            }
 
            /** {@link UnknownHost} object used to read from console */
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
 
            while (true) {
                System.out.print("client: ");
                String userInput = stdIn.readLine();
                /** Exit on 'q' char sent */
                if ("q".equals(userInput)) {
                    break;
                }
                out.println(userInput);
                System.out.println("server: " + in.readLine());
            }
 
            /** Closing all the resources */
            out.close();
            in.close();
            stdIn.close();
            echoSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
