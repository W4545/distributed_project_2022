import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class TCPServer {
    public static void main(String[] args) throws IOException {
        Properties config = new Properties();

        FileDialog dialog = new FileDialog((Frame) null);
        dialog.setTitle("Select Server Settings File file");
        dialog.setVisible(true);

        config.load(new FileInputStream(dialog.getFiles()[0]));

        // Variables for setting up connection and communication
        Socket Socket = null; // socket to connect with ServerRouter
        PrintWriter out = null; // for writing to ServerRouter
        BufferedReader in = null; // for reading form ServerRouter
        InetAddress addr = InetAddress.getLocalHost();
        String host = addr.getHostAddress(); // Server machine's IP
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
        String fromServer; // messages sent to ServerRouter
        String fromClient; // messages received from ServerRouter
        String address = config.getProperty("clientIP"); // destination IP (Client)
        //file to ber written to
        File logs = new File("server_logs.csv");

        // Communication process (initial sends/receives)
        out.println(address);// initial send (IP of the destination Client)
        fromClient = in.readLine();// initial receive from router (verification of connection)
        System.out.println("ServerRouter: " + fromClient);

        long t0 = System.currentTimeMillis();

        // Communication while loop
        while ((fromClient = in.readLine()) != null) {
            System.out.println("Client said: " + fromClient);
            long t1 = System.currentTimeMillis();
            long t = t1 - t0;

            if(logs.length() > 0)
            {
                //If no file exists, create one and log incoming message, else log incoming message
                FileWriter logWriter = new FileWriter(logs,true);
                logWriter.write(Float.toString(t) + " ms");
                logWriter.write(",");
                logWriter.write(Integer.toString(fromClient.getBytes().length) + " bytes");
                logWriter.write("\n");
            }
            else
            {
                FileWriter logWriter = new FileWriter(logs,false);
                logWriter.write("Client to Server Transmission time");
                logWriter.write(",");
                logWriter.write("Client to Server Message Size");
                logWriter.write("\n");
                logWriter.write(Float.toString(t) + " ms");
                logWriter.write(",");
                logWriter.write(Integer.toString(fromClient.getBytes().length) + " bytes");
                logWriter.write("\n");
            }

            if (fromClient.contains("STARTFILE")) {

                String filePath = dialog.getFiles()[0].getParentFile() + "\\" + fromClient.substring(fromClient.indexOf(' '));
                System.out.println(filePath);
                File file = new File(filePath);
                if (file.exists())
                    file.delete();
                FileOutputStream fileOutputStream = new FileOutputStream(filePath);

                DataInputStream dataInputStream = new DataInputStream(Socket.getInputStream());
                long fileSize = dataInputStream.readLong();

                byte[] buffer = new byte[8 * 1024];
                int dataReceived = 0;

                while (fileSize > 0 && (dataReceived = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    fileOutputStream.write(buffer, 0, dataReceived);
                    fileSize -= dataReceived;
                }

                fileOutputStream.close();

                out.println("Bye.");
                break;
            } else {
                if (fromClient.equals("Bye.")) // exit statement
                    break;
                fromServer = fromClient.toUpperCase(); // converting received message to upper case
                System.out.println("Server said: " + fromServer);


                out.println(fromServer); // sending the converted message back to the Client via ServerRouter
            }


        }

        // closing connections
        out.close();
        in.close();
        Socket.close();
        dialog.dispose();
    }
}
