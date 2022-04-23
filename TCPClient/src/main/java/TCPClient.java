import java.io.*;
import java.net.*;
import java.util.*;

public class TCPClient {

    public static final Logger messageLogger = new Logger(new File("messageLogger.csv"), "message type", "message", "message size (bytes)");

    public static final Logger fileLogger = new Logger(new File("file_transfer_times.csv"), "file name", "file size (bytes)", "transfer time (ms)", "number of transfer chunks (8KB data chunks)");

    /**
     * @param myID
     * @param portNum
     * @param send    Writes <clientID> <port> to outputstream for server-router.
     */
    public static void logOn(String myID, int portNum, PrintWriter send) {
        String message = "LOGON: " + myID + " " + portNum;
        send.println(message);
        messageLogger.log("COMMAND_REQUEST", message, message.getBytes().length);
        System.out.println("This Client said: LOGON: " + myID + " " + String.valueOf(portNum));
    }


    /**
     * @param clientID
     * @param send     Sends request to server-router containing a clientID.
     *                 Utilizes pre-existing open Printwriter stream to send.
     */
    public static void sendRequest(String clientID, PrintWriter send) {
        String message = "CLIENTIPREQUEST: " + clientID;
        send.println(message);
        messageLogger.log("COMMAND_REQUEST", message, message.getBytes().length);
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
        messageLogger.log("RESPONSE", response, response.getBytes().length);

        return response;

    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("This program requires two arguments.");
            System.exit(1);
        }

        Properties config = new Properties();

        config.load(new FileInputStream(args[0]));

        // Variables for setting up connection and communication
        Socket routerSocket = null; // socket to connect with ServerRouter
        PrintWriter routerOut = null; // for writing to ServerRouter
        BufferedReader routerIn = null; // for reading form ServerRouter
        InetAddress addr = InetAddress.getLocalHost();
        String host = addr.getHostAddress(); // Client machine's IP
        String routerName = config.getProperty("routerIP"); // ServerRouter host name
        String groupID = config.getProperty("groupID");
        String myID = (config.getProperty("groupID").toString() + config.getProperty("clientID").toString());
        String destinationID = config.getProperty("destinationID").toString();

        int SockNum = 5555 + (groupID.charAt(0) - 65); // ROUTERPORTNUM port number
        int clientPort = Integer.parseInt(config.getProperty("listenPort"));

        // Tries to connect to the ServerRouter
        try {
            routerSocket = new Socket(routerName, SockNum);
            routerOut = new PrintWriter(routerSocket.getOutputStream(), true);
            routerIn = new BufferedReader(new InputStreamReader(routerSocket.getInputStream()));

        } catch (UnknownHostException e) {
            System.err.println("Don't know about router: " + routerName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + routerName);
            System.exit(1);
        }

        logOn(myID, clientPort, routerOut); //begin logon process
        if (logOnStatus(routerIn)) //if IDGOOD
        {
            if (!config.getProperty("status").equals("listening")) //if client is requesting
            {
                executeFileSend(destinationID, routerOut, routerIn, args[1]);
            } else { //else this client is listening
                executeFileReceive(clientPort, destinationID, args[1]);
            }
        }

        routerSocket.close();
    }

    public static void executeFileSend(String destinationID, PrintWriter routerOut, BufferedReader routerIn, String sourceFile) {
        PrintWriter clientOut = null;
        BufferedReader clientIn = null;

        sendRequest(destinationID, routerOut);//request destinationID to server-router
        String otherClientLocation = null; //expects destinationIP and portnumber
        try {
            otherClientLocation = getResponse(routerIn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] split = otherClientLocation.split(" ");
        String clientIP = split[1];
        //clientIP is parsed from first space, to second space
        String portNum = split[2];
        //portnum is parsed from after the second space


        try (Socket dataSendSocket = new Socket(clientIP, Integer.parseInt(portNum))) {
            System.out.println("Data socket connection established.");
            clientOut = new PrintWriter(dataSendSocket.getOutputStream(), true);
            clientIn = new BufferedReader(new InputStreamReader(dataSendSocket.getInputStream()));

            File file = new File(sourceFile);


            String fromClient; //messages sent from other client
            String fromThis; // messages sent to Client


            fromClient = clientIn.readLine();
            messageLogger.log("CLIENT_HELLO", fromClient, fromClient.getBytes().length);

            System.out.println("Other Client said: " + fromClient);

            DataOutputStream dataOutputStream = new DataOutputStream(dataSendSocket.getOutputStream());
            FileInputStream fileInputStream = new FileInputStream(file);

            String fileStartMessage = "STARTFILE " + file.getName();
            messageLogger.log("DATA_COMMAND_REQUEST", fileStartMessage, fileStartMessage.getBytes().length);
            System.out.println(fileStartMessage);
            clientOut.println(fileStartMessage);
            long fileSize = file.length();
            dataOutputStream.writeLong(fileSize);

            byte[] buffer = new byte[8 * 1024];
            int dataReceived = 0;


            while (fileSize > 0 && (dataReceived = fileInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {

                dataOutputStream.write(buffer, 0, dataReceived);

                fileSize -= dataReceived;
            }

            dataOutputStream.flush();

            fileInputStream.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("Don't know about requested client: " + clientIP);
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't get I/O for the connection to: " + clientIP);
            System.exit(1);
        }
    }

    public static void executeFileReceive(int clientPort, String destinationID, String storeLocation) {

        // server socket for accepting connections
        try (ServerSocket serverSocket = new ServerSocket(clientPort)) {
            System.out.println("This Client is Listening on port: " + String.valueOf(clientPort));

            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Connection established");

                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                clientOut.println("Connection established");

                String fromClient = clientIn.readLine();

                System.out.println("Client-" + destinationID + " said: " + fromClient);


                if (fromClient.contains("STARTFILE")) {
                    String filePath = storeLocation + "\\" + fromClient.substring(fromClient.indexOf(' ') + 1);
                    System.out.println(filePath);
                    File file = new File(filePath);

                    if (file.exists())
                        file.delete();
                    FileOutputStream fileOutputStream = new FileOutputStream(filePath);

                    DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                    long fileSize = dataInputStream.readLong();

                    byte[] buffer = new byte[8 * 1024];
                    int dataReceived = 0;

                    int chunkCount = 0;
                    long startTime = System.currentTimeMillis();

                    while (fileSize > 0 && (dataReceived = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {

                        chunkCount += 1;

                        fileOutputStream.write(buffer, 0, dataReceived);
                        fileSize -= dataReceived;

                    }


                    long endTime = System.currentTimeMillis();

                    long transferTime = endTime - startTime;
                    messageLogger.log(file.getName(), file.length(), transferTime, chunkCount);

                    fileOutputStream.flush();

                    fileOutputStream.close();
                    clientOut.println("Transfer Complete.");

                    //close connections once communication is finished
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("IO error occurred while sending.");
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + String.valueOf(clientPort));
            System.exit(1);
        }
    }

}
