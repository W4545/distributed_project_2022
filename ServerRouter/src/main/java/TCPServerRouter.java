import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class TCPServerRouter {

    public static final RouterRecord[] routerRecords = {
            new RouterRecord('B', "192.168.1.227")
    };

    public static final List<RoutingTableRecord> routingTable = Collections.synchronizedList(new ArrayList<>());

    public static volatile char groupID;

    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();

        properties.load(TCPServerRouter.class.getResourceAsStream("config.properties"));

        groupID = properties.getProperty("groupID").charAt(0);
        int socketPort = 5555 + (((int) groupID) - 65); // port number
        boolean running = true;

        //Accepting connections
        ServerSocket serverSocket = null; // server socket for accepting connections
        try {
            serverSocket = new ServerSocket(socketPort);
            System.out.println("ServerRouter is Listening on port: " + socketPort + ".");
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + socketPort + ".");
            System.exit(1);
        }

        // Creating threads with accepted connections
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                SThread t = new SThread(clientSocket); // creates a thread with a random port
                t.start(); // starts the thread
                System.out.println("ServerRouter connected with Client/Router: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                System.err.println("Client/Router failed to connect.");
                System.exit(1);
            }
        }//end while

        //closing connections
        synchronized (routingTable) {
            for (RoutingTableRecord routingTableRecord : routingTable) {
                routingTableRecord.getClientSocket().close();
            }
        }
        serverSocket.close();

    }
}