import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TCPClient {

    public static void log(long transTime, long size, String notes) throws IOException {

        File logs = new File("client_logs.csv");

        if(logs.length() > 0){
            FileWriter logWriter = new FileWriter(logs,true);
            logWriter.write(Float.toString(transTime));
            logWriter.write(",");
            logWriter.write(String.valueOf(size));
            logWriter.write(",");
            logWriter.write(notes);
            logWriter.write("\n");
            logWriter.close();
        }
        else
        {
            FileWriter logWriter = new FileWriter(logs,false);
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
     *
     * @param myID
     * @param portNum
     * @param send
     *
     * Writes <clientID> <port> to outputstream for server-router.
     */
    public static void logOn(String myID, int portNum, PrintWriter send)
    {
        send.println("LOGON: " + myID +" "+ String.valueOf(portNum));
        System.out.println("This Client said: LOGON: "+ myID + " " + String.valueOf(portNum));
    }


    /**
     *
     * @param clientID
     * @param send
     * Sends request to server-router containing a clientID.
     * Utilizes pre-existing open Printwriter stream to send.
     */
    public static void sendRequest(String clientID, PrintWriter send)
    {
        send.println("CLIENTIPREQUEST: "+ clientID);
        System.out.println("This Client said: CLIENTIPREQUEST: " + clientID);
    }

    /**
     *
     * @param response
     * @return
     * @throws IOException
     * Checks server-router response for "IDGOOD" or "IDBAD".
     */
    public static Boolean logOnStatus(BufferedReader response) throws IOException {
        if (response.readLine().equals("IDGOOD"))
        {
            System.out.println("Router said: IDGOOD");
            return true;
        }
        else
        {
            System.out.println("Router said: IDBAD");
            return false;
        }
    }

    /**
     *
     * @param in
     * @return String: clientIP
     * @throws IOException
     *
     * Reads a string from server-router.
     * Expectation is to recieve InetAddress object converted to String.
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

        logOn(myID, SockNum, out); //begin logon process
        if (logOnStatus(in) == true) //if IDGOOD
        {
            PrintWriter clientOut = null;
            BufferedReader clientIn = null;

            if (!config.getProperty("status").equals("listening")) //if client is requesting
            {
                sendRequest(destinationID, out);//request destinationID to server-router
                String otherClientLocation = getResponse(in); //expects destinationIP and portnumber

                String clientIP = otherClientLocation.substring(otherClientLocation.indexOf(" ") + 1, otherClientLocation.indexOf(" ", 15));
                //clientIP is parsed from first space, to second space
                String portNum = otherClientLocation.substring(otherClientLocation.indexOf(" ", 15));
                //portnum is parsed from after the second space


                //open communication with other client
                try {
                    Socket = new Socket(clientIP, Integer.parseInt(portNum));
                    clientOut = new PrintWriter(Socket.getOutputStream(), true);
                    clientIn = new BufferedReader(new InputStreamReader(Socket.getInputStream()));

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


                int sendCount = 0;

                // Communication while loop for sending files to a client
                while ((fromClient = clientIn.readLine()) != null) {
                    System.out.println("Other Client said: " + fromClient);

                    //log(t, fromClient.getBytes().length, "OTHER_CLIENT_MESSAGE");

                    if (fromClient.equals("Bye.")) // exit statement
                        break;

                    //System.out.println("Cycle time: " + t);

                    if (sendCount > 5) {
                        System.out.println("Send count reached");
                        clientOut.println("Bye.");
                        break;
                    }

                    DataOutputStream dataOutputStream = new DataOutputStream(Socket.getOutputStream());
                    FileInputStream fileInputStream = new FileInputStream(dialog.getFiles()[0]);


                    sendCount += 1;
                    clientOut.println("STARTFILE " + sendCount + dialog.getFiles()[0].getName());
                    long fileSize = dialog.getFiles()[0].length();
                    dataOutputStream.writeLong(fileSize);

                    byte[] buffer = new byte[8 * 1024];
                    int dataReceived = 0;

                    while (fileSize > 0 && (dataReceived = fileInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                        dataOutputStream.write(buffer, 0, dataReceived);

                        fileSize -= dataReceived;
                    }

                    fileInputStream.close();
                    //t0 = System.currentTimeMillis();
                }
                //close connections
                Socket.close();
            } else { //else this client is listening

                //Accepting connections
                ServerSocket serverSocket = null; // server socket for accepting connections
                Socket clientSocket = null;
                boolean running = true;

                try {
                    serverSocket = new ServerSocket(SockNum);
                    System.out.println("This Client is Listening on port: " + String.valueOf(SockNum));
                } catch (IOException e) {
                    System.err.println("Could not listen on port: " + String.valueOf(SockNum));
                    System.exit(1);
                }
                String fromClient;
                String fromMe;
                int transferCount = 0;

                // Creating threads with accepted connections
                while (running) {
                    try {
                        clientSocket = serverSocket.accept();

                        // Communication while loop
                        while ((fromClient = clientIn.readLine()) != null) {
                            System.out.println("Client-" +destinationID+ " said: " + fromClient);
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

                                DataInputStream dataInputStream = new DataInputStream(Socket.getInputStream());
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
                            } else {

                                fromMe = fromClient.toUpperCase(); // converting received message to upper case
                                System.out.println("This Client said: " + fromMe);

                                if (fromClient.equals("Bye.")) // exit statement
                                    break;


                                clientOut.println(fromClient); // sending the converted message back to the Client via ServerRouter
                            }

                            //t0 = System.currentTimeMillis();
                            //communication loop ended, end outer while loop
                            running = false;
                        }

                        //close connections once communication is finished
                        clientSocket.close();
                        serverSocket.close();
                    }
                    catch (IOException e) {
                        System.err.println("Client/Client failed to connect.");
                        System.exit(1);
                        running = false;
                    }
                }//end while

            }
        }


    }


}
