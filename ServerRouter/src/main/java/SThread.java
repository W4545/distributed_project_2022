import java.io.*;
import java.net.*;


public class SThread extends Thread {
// routing table
    private final PrintWriter socketOut;
    private final BufferedReader socketIn; // reader (for reading from the machine connected to)
    private final String address; // communication strings
    private final Socket inSocket;

    private static final Logger commandLogger = new Logger(new File("command_exec_times.csv"), "command", "time (ms)");

    private static final Logger messageLogger = new Logger(new File("messageLogger.csv"), "message type", "message", "message size (bytes)");

    private RoutingTableRecord routingTableRecord;

    // Constructor
    SThread(Socket toClient) throws IOException {
        socketOut = new PrintWriter(toClient.getOutputStream(), true);
        socketIn = new BufferedReader(new InputStreamReader(toClient.getInputStream()));
        inSocket = toClient;
        address = toClient.getInetAddress().getHostAddress();
    }

    File logs = new File("Rtable_logs.csv");

    // Run method (will run for each machine that connects to the ServerRouter)
    public void run() {

        try {
            boolean run = true;
            while (run) {
                // Initial sends/receives
                String command = socketIn.readLine(); // initial read (the destination for writing)
                if (command == null) {
                    threadPrintErr("Null string received from socket");
                    break;
                }
                logMessage("COMMAND_REQUEST", command);
                long start = System.currentTimeMillis();
                threadPrint("Starting execution of command \"" + command + "\"");
                if (command.contains("LOGON:")) {
                    String[] splice = command.split(" ");
                    String clientID = splice[1];
                    String portNum = splice[2];

                    if (clientID.charAt(0) == TCPServerRouter.groupID && clientLookup(clientID) == null) {
                        RoutingTableRecord routingTableRecord = new RoutingTableRecord(address, inSocket, clientID, portNum);
                        TCPServerRouter.routingTable.add(routingTableRecord);
                        this.routingTableRecord = routingTableRecord;
                        sendOutbound("IDGOOD");
                    } else {
                        sendOutbound("IDBAD");
                    }
                } else if (command.contains("CLIENTIPREQUEST:")) {
                    String clientID = command.substring(command.indexOf(' ') + 1);

                    if (clientID.charAt(0) == TCPServerRouter.groupID) {
                        RoutingTableRecord routingTableRecord = clientLookup(clientID);

                        if (routingTableRecord == null)
                            sendOutbound("IPNORESPONSE");
                        else
                            sendOutbound("IPRESPONSE: " + routingTableRecord.getIpAddress() + " " + routingTableRecord.getListenPort());
                    } else {
                        executeCrossRouterConnection(clientID.charAt(0), clientID);
                    }

                } else if (command.contains("ROUTERIPREQUEST: ")) {
                    String lookup = command.substring(command.indexOf(' ') + 1);

                    RoutingTableRecord routingTableRecord = clientLookup(lookup);

                    if (routingTableRecord == null)
                        sendOutbound("IPNORESPONSE");
                    else
                        sendOutbound("IPRESPONSE: " + routingTableRecord.getIpAddress() + " " + routingTableRecord.getListenPort());
                } else if (command.contains("GOODBYE")) {
                    run = false;
                } else {
                    System.out.println("Unknown command \"" + command + "\" on thread " + this.getName() + ". Ignoring");
                }
                long stop = System.currentTimeMillis();

                long duration = stop - start;

                threadPrint("Execution complete of command \"" + command + "\". Duration " + duration + "ms.");
                synchronized (commandLogger) {
                    commandLogger.log(command, duration);
                }
            }

            inSocket.close();
            threadPrint("Socket closed. ending thread execution");



//            System.out.println("Forwarding to " + destination);
//            out.println("Connected to the router."); // confirmation of connection
//
//            // waits 10 seconds to let the routing table fill with all machines' information
//            try {
//                Thread.currentThread().sleep(10000);
//            } catch (InterruptedException ie) {
//                System.out.println("Thread interrupted");
//            }
//            long t0 = System.currentTimeMillis();
//            // loops through the routing table to find the destination
//            for (int i = 0; i < 10; i++) {
//                if (destination.equals((String) RTable[i][0])) {
//                    outSocket = (Socket) RTable[i][1]; // gets the socket for communication from the table
//                    System.out.println("Found destination: " + destination);
//                    outTo = new PrintWriter(outSocket.getOutputStream(), true); // assigns a writer
//                }
//            }
//            long t1 = System.currentTimeMillis();
//            long t = t1 - t0;
//            File log = new File("rtable_log.csv");
//            if (log.length() > 0) {
//                FileWriter logWriter = new FileWriter(log, true);
//                logWriter.write(Float.toString(t));
//                logWriter.write("\n");
//                logWriter.close();
//            } else {
//                FileWriter logWriter = new FileWriter(log);
//                logWriter.write("Routing table look up time (ms)");
//                logWriter.write("\n");
//                logWriter.write(String.valueOf(t));
//                logWriter.write("\n");
//                logWriter.close();
//            }
//
//            // Communication loop
//            while ((inputLine = in.readLine()) != null) {
//                System.out.println("Client/Server said: " + inputLine);
//
//                if (inputLine.contains("STARTFILE")) {
//
//                    outTo.println(inputLine);
//                    DataInputStream dataInputStream = new DataInputStream(inSocket.getInputStream());
//                    DataOutputStream dataOutputSteam = new DataOutputStream(outSocket.getOutputStream());
//                    long fileSize = dataInputStream.readLong();
//                    dataOutputSteam.writeLong(fileSize);
//
//                    byte[] buffer = new byte[8 * 1024];
//                    int dataReceived = 0;
//
//                    while (fileSize > 0 && (dataReceived = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
//                        dataOutputSteam.write(buffer, 0, dataReceived);
//
//                        fileSize -= dataReceived;
//                    }
//
//                } else {
//                    outputLine = inputLine; // passes the input from the machine to the output string for the destination
//
//                    if (outSocket != null) {
//                        outTo.println(outputLine); // writes to the destination
//                    }
//                    if (inputLine.equals("Bye.")) // exit statement
//                        break;
//                }
//            }// end while


        }// end try
        catch (RuntimeException | IOException e) {
            e.printStackTrace();
            threadPrintErr("Error occurred during thread execution. Socket closing");
            if (!inSocket.isClosed()) {
                try {
                    inSocket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        } finally {
            TCPServerRouter.routingTable.remove(routingTableRecord);
            threadPrint("Routing record removed from server, current length of records: " + TCPServerRouter.routingTable.size());
        }
    }

    public RoutingTableRecord clientLookup(String clientID) {
        for (RoutingTableRecord routingTableRecord : TCPServerRouter.routingTable) {
            if (routingTableRecord.getClientID().equals(clientID))
                return routingTableRecord;
        }

        return null;
    }

    public void executeCrossRouterConnection(char groupID, String clientID) {
        for (RouterRecord routerRecord : TCPServerRouter.routerRecords) {
            if (routerRecord.getGroupID() == groupID) {
                try (Socket socket = new Socket(routerRecord.getIpAddress(), 5555 + (((int) groupID) - 65))) {
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

                    sendOutbound(printWriter, "ROUTERIPREQUEST: " + clientID);

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String response = in.readLine();
                    logMessage("ROUTER_RESPONSE", response);

                    if (!response.contains("IPRESPONSE: ") && !response.contains("IPNORESPONSE"))
                        throw new RuntimeException("Illegal response from cross-router connection (" + groupID + "): " + response);

                    sendOutbound(response);
                    sendOutbound(printWriter, "GOODBYE");
                } catch (IOException exception) {
                    exception.printStackTrace();
                    sendOutbound("IPNORESPONSE");
                }
                return;
            }
        }
        sendOutbound("IPNORESPONSE");
        threadPrintErr("failed to find the specified client ID's server in the lookup table (" + groupID + ")");
    }
    public void threadPrint(String string) {
        System.out.println(getName() + ": " + string);
    }

    public void threadPrintErr(String string) {
        System.err.println(getName() + ": " + string);
    }

    public void sendOutbound(String string) {
        sendOutbound(socketOut, string);
    }

    public void sendOutbound(PrintWriter printWriter, String string) {
        printWriter.println(string);
        logMessage("OUTBOUND", string);
    }

    public void logMessage(String messageType, String message) {
        threadPrint("MESSAGE, " + messageType + ", " + message);
        synchronized (messageLogger) {
            messageLogger.log(messageType, message, message.getBytes().length);
        }
    }
}