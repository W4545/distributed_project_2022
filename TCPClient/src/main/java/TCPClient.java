import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class TCPClient {

    public static void log(long transTime, long size, String notes) throws IOException {

        File logs = new File("client_logs.csv");

        if (logs.length() > 0) {
            FileWriter logWriter = new FileWriter(logs, true);
            logWriter.write(Float.toString(transTime));
            logWriter.write(",");
            logWriter.write(String.valueOf(size));
            logWriter.write(",");
            logWriter.write(notes);
            logWriter.write("\n");
            logWriter.close();
        } else {
            FileWriter logWriter = new FileWriter(logs, false);
            logWriter.write("Server to Client Transmission time (ms)");
            logWriter.write(",");
            logWriter.write("Server to Client Message Size (bytes)");
            logWriter.write(",");
            logWriter.write("Notes");
            logWriter.write("\n");
            logWriter.write(Float.toString(transTime));
            logWriter.write(",");
            logWriter.write(String.valueOf(size));
            logWriter.write(",");
            logWriter.write(notes);
            logWriter.write("\n");
            logWriter.close();
        }
    }

    /**
     * @param myID
     * @param portNum
     * @param send    Writes <clientID> <port> to outputstream for server-router.
     */
    public static void logOn(String myID, int portNum, PrintWriter send) {
        send.println("LOGON: " + myID + " " + String.valueOf(portNum));
        System.out.println("This Client said: LOGON: " + myID + " " + String.valueOf(portNum));
    }


    /**
     * @param clientID
     * @param send     Sends request to server-router containing a clientID.
     *                 Utilizes pre-existing open Printwriter stream to send.
     */
    public static void sendRequest(String clientID, PrintWriter send) {
        send.println("CLIENTIPREQUEST: " + clientID);
        System.out.println("This Client said: CLIENTIPREQUEST: " + clientID);
    }

    /**
     * @param response
     * @return
     * @throws IOException Checks server-router response for "IDGOOD" or "IDBAD".
     */
    public static Boolean logOnStatus(BufferedReader response) throws IOException {
        if (response.readLine().equals("IDGOOD")) {
            System.out.println("Router said: IDGOOD");
            return true;
        } else {
            System.out.println("Router said: IDBAD");
            return false;
        }
    }

    /**
     * @param in
     * @return String: clientIP
     * @throws IOException Reads a string from server-router.
     *                     Expectation is to recieve InetAddress object converted to String.
     */
    public static String getResponse(BufferedReader in) throws IOException {

        String response = in.readLine();
        System.out.println("Router said: " + response);

        return response;

    }

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
        String groupID = config.getProperty("groupID");
        String myID = (config.getProperty("groupID").toString() + config.getProperty("clientID").toString());
        String destinationID = config.getProperty("destinationID").toString();

        int SockNum = 5555 + (groupID.charAt(0) - 65); // ROUTERPORTNUM port number
        int clientPort = SockNum;

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

        logOn(myID, clientPort, out); //begin logon process
        if (logOnStatus(in) == true) //if IDGOOD
        {
            PrintWriter clientOut = null;
            BufferedReader clientIn = null;

            if (!config.getProperty("status").equals("listening")) //if client is requesting
            {
                sendRequest(destinationID, out);//request destinationID to server-router
                String otherClientLocation = getResponse(in); //expects destinationIP and portnumber

                String[] split = otherClientLocation.split(" ");
                String clientIP = split[1];
                //clientIP is parsed from first space, to second space
                String portNum = split[2];
                //portnum is parsed from after the second space


                Socket dataSendSocket = null;
                //open communication with other client
                try {
                    dataSendSocket = new Socket(clientIP, Integer.parseInt(portNum));
                    clientOut = new PrintWriter(dataSendSocket.getOutputStream(), true);
                    clientIn = new BufferedReader(new InputStreamReader(dataSendSocket.getInputStream()));

                } catch (UnknownHostException e) {
                    System.err.println("Don't know about requested client: " + clientIP);
                    System.exit(1);
                } catch (IOException e) {
                    System.err.println("Couldn't get I/O for the connection to: " + clientIP);
                    System.exit(1);
                }

                //variables for message passing
                dialog.setTitle("Select file to transfer");
                dialog.setVisible(true);


                Reader reader = new FileReader(dialog.getFiles()[0]);
                BufferedReader fromFile = new BufferedReader(reader); // reader for the string file

                String fromClient; //messages sent from other client
                String fromThis; // messages sent to Client


                fromClient = clientIn.readLine();

                System.out.println("Other Client said: " + fromClient);

                //log(t, fromClient.getBytes().length, "OTHER_CLIENT_MESSAGE");


                //System.out.println("Cycle time: " + t);


                DataOutputStream dataOutputStream = new DataOutputStream(dataSendSocket.getOutputStream());
                FileInputStream fileInputStream = new FileInputStream(dialog.getFiles()[0]);


                clientOut.println("STARTFILE " + dialog.getFiles()[0].getName());
                long fileSize = dialog.getFiles()[0].length();
                dataOutputStream.writeLong(fileSize);

                byte[] buffer = new byte[8 * 1024];
                int dataReceived = 0;

                while (fileSize > 0 && (dataReceived = fileInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    dataOutputStream.write(buffer, 0, dataReceived);

                    fileSize -= dataReceived;
                }

                dataOutputStream.flush();
                fileInputStream.close();
                //t0 = System.currentTimeMillis();
                //close connections
                dataSendSocket.close();
                Socket.close();
            } else { //else this client is listening

                //Accepting connections
                ServerSocket serverSocket = null; // server socket for accepting connections
                Socket clientSocket = null;
                boolean running = true;


                try {
                    serverSocket = new ServerSocket(clientPort);
                    System.out.println("This Client is Listening on port: " + String.valueOf(clientPort));
                } catch (IOException e) {
                    System.err.println("Could not listen on port: " + String.valueOf(clientPort));
                    System.exit(1);
                }
                String fromClient;
                String fromMe;
                int transferCount = 0;

                clientSocket = serverSocket.accept();
                System.out.println("Socket connected: ");

                clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                clientOut.println("Connection established");

                fromClient = clientIn.readLine();

                System.out.println("Client-" + destinationID + " said: " + fromClient);
                // long t1 = System.currentTimeMillis();
                //long t = t1 - t0;

                //log(t, fromClient.getBytes().length, "SERVER_MESSAGE");

                if (fromClient.contains("STARTFILE")) {
                    transferCount += 1;
                    String filePath = dialog.getFiles()[0].getParentFile() + "\\" + fromClient.substring(fromClient.indexOf(' '));
                    System.out.println(filePath);
                    File file = new File(filePath);

                    if (file.exists())
                        file.delete();
                    FileOutputStream fileOutputStream = new FileOutputStream(filePath);

                    DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                    long fileSize = dataInputStream.readLong();

                    byte[] buffer = new byte[8 * 1024];
                    int dataReceived = 0;
                    //t0 = System.currentTimeMillis();
                    while (fileSize > 0 && (dataReceived = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                        // t1 = System.currentTimeMillis();

                        // t = t1 - t0;
                        // log(t, dataReceived, "FILE_TRANSFER_CHUNK");
                        fileOutputStream.write(buffer, 0, dataReceived);
                        fileSize -= dataReceived;
                        // t0 = System.currentTimeMillis();
                    }

                    fileOutputStream.close();
                    out.println("Transfer Complete.");

                    //close connections once communication is finished
                    clientSocket.close();
                    serverSocket.close();
                }

            }
        }


    }


}
