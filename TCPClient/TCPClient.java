import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class TCPClient {
    public static void main(String[] args) throws IOException {
        Properties config = new Properties();

        FileDialog dialog = new FileDialog((Frame) null);
        dialog.setTitle("Select Client Settings File file");
        dialog.setVisible(true);

        config.load(new FileInputStream(dialog.getFiles()[0]));

        // Variables for setting up connection and communication
        Socket Socket = null; // socket to connect with ServerRouter
        PrintWriter out = null; // for writing to ServerRouter
        BufferedReader in = null; // for reading form ServerRouter
        InetAddress addr = InetAddress.getLocalHost();
        String host = addr.getHostAddress(); // Client machine's IP
        String routerName = config.getProperty("routerIP"); // ServerRouter host name
        int SockNum = 5555; // port number

        // Tries to connect to the ServerRouter
        try {
            Socket = new Socket(routerName, SockNum);
            out = new PrintWriter(Socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about router: " + routerName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + routerName);
            System.exit(1);
        }

        // Variables for message passing

        dialog.setTitle("Select file to transfer");
        dialog.setVisible(true);


        Reader reader = new FileReader(dialog.getFiles()[0]);
        BufferedReader fromFile = new BufferedReader(reader); // reader for the string file
        String fromServer; // messages received from ServerRouter
        String fromUser; // messages sent to ServerRouter
        String address = config.getProperty("serverIP"); // destination IP (Server)
        long t0, t1, t;

        // Communication process (initial sends/receives
        out.println(address);// initial send (IP of the destination Server)
        fromServer = in.readLine();//initial receive from router (verification of connection)
        System.out.println("ServerRouter: " + fromServer);
        out.println(host); // Client sends the IP of its machine as initial send
        t0 = System.currentTimeMillis();

        int sendCount = 0;
        File logs = new File("client_logs.csv");

        // Communication while loop
        while ((fromServer = in.readLine()) != null) {
            System.out.println("Server: " + fromServer);
            t1 = System.currentTimeMillis();
            if (fromServer.equals("Bye.")) // exit statement
                break;
            t = t1 - t0;

            if(logs.length() > 0){
                FileWriter logWriter = new FileWriter(logs,true);
                logWriter.write(Float.toString(t) + " ms");
                logWriter.write(",");
                logWriter.write(Integer.toString(fromServer.getBytes().length) + " bytes");
                logWriter.write("/n");
            }
            else
            {
                FileWriter logWriter = new FileWriter(logs,false);
                logWriter.write("Server to Client Transmission time");
                logWriter.write(",");
                logWriter.write("Server to Client Message Size");
                logWriter.write("\n");
                logWriter.write(Float.toString(t) + " ms");
                logWriter.write(",");
                logWriter.write(Integer.toString(fromServer.getBytes().length) + " bytes");
                logWriter.write("\n");

            }
            System.out.println("Cycle time: " + t);

            DataOutputStream dataOutputStream = new DataOutputStream(Socket.getOutputStream());
            FileInputStream fileInputStream = new FileInputStream(dialog.getFiles()[0]);


            sendCount += 1;
            out.println("STARTFILE " + sendCount + dialog.getFiles()[0].getName());
            long fileSize = dialog.getFiles()[0].length();
            dataOutputStream.writeLong(fileSize);

            byte[] buffer = new byte[8 * 1024];
            int dataReceived = 0;

            while (fileSize > 0 && (dataReceived = fileInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                dataOutputStream.write(buffer, 0, dataReceived);

                fileSize -= dataReceived;
            }

            fileInputStream.close();
            t0 = System.currentTimeMillis();
//            fromUser = fromFile.readLine(); // reading strings from a file
//            if (fromUser != null) {
//                System.out.println("Client: " + fromUser);
//                out.println(fromUser); // sending the strings to the Server via ServerRouter
//                t0 = System.currentTimeMillis();
//            }
        }

        // closing connections
        out.close();
        in.close();
        Socket.close();
        dialog.dispose();
    }
}
