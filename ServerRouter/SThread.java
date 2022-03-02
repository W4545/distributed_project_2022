import java.io.*;
import java.net.*;
import java.lang.Exception;

	
public class SThread extends Thread
{
	private Object [][] RTable; // routing table
	private PrintWriter out, outTo; // writers (for writing back to the machine and to destination)
   private BufferedReader in; // reader (for reading from the machine connected to)
	private String inputLine, outputLine, destination, addr; // communication strings
	private Socket outSocket; // socket for communicating with a destination
	private Socket inSocket;
	private int ind; // indext in the routing table

	// Constructor
	SThread(Object [][] Table, Socket toClient, int index) throws IOException
	{
			out = new PrintWriter(toClient.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(toClient.getInputStream()));
			inSocket = toClient;
			RTable = Table;
			addr = toClient.getInetAddress().getHostAddress();
			RTable[index][0] = addr; // IP addresses 
			RTable[index][1] = toClient; // sockets for communication
			ind = index;
	}
	File logs = new File("Rtable_logs.csv");
	// Run method (will run for each machine that connects to the ServerRouter)
	public void run()
	{
		try
		{
		// Initial sends/receives
		destination = in.readLine(); // initial read (the destination for writing)
		System.out.println("Forwarding to " + destination);
		out.println("Connected to the router."); // confirmation of connection
		
		// waits 10 seconds to let the routing table fill with all machines' information
		try{
    		Thread.currentThread().sleep(10000); 
	   }
		catch(InterruptedException ie){
		System.out.println("Thread interrupted");
		}
		long t0 = System.currentTimeMillis();
		// loops through the routing table to find the destination
		for ( int i=0; i<10; i++) 
				{
					if (destination.equals((String) RTable[i][0])){
						outSocket = (Socket) RTable[i][1]; // gets the socket for communication from the table
						System.out.println("Found destination: " + destination);
						outTo = new PrintWriter(outSocket.getOutputStream(), true); // assigns a writer
				}}
		long t1 = System.currentTimeMillis();
		long t = t1 - t0;
		File log = new File("rtable_log.csv");
		if(log.length() > 0){
			FileWriter logWriter = new FileWriter(log,true);
			logWriter.write(Float.toString(t));
			logWriter.write("\n");
			logWriter.close();
		}
		else
		{
			FileWriter logWriter = new FileWriter(log);
			logWriter.write("Routing table look up time (ms)");
			logWriter.write("\n");
			logWriter.write(String.valueOf(t));
			logWriter.write("\n");
			logWriter.close();
		}

		// Communication loop	
		while ((inputLine = in.readLine()) != null) {
            System.out.println("Client/Server said: " + inputLine);

			if (inputLine.contains("STARTFILE")) {

				outTo.println(inputLine);
				DataInputStream dataInputStream = new DataInputStream(inSocket.getInputStream());
				DataOutputStream dataOutputSteam = new DataOutputStream(outSocket.getOutputStream());
				long fileSize = dataInputStream.readLong();
				dataOutputSteam.writeLong(fileSize);

				byte[] buffer = new byte[8 * 1024];
				int dataReceived = 0;

				while (fileSize > 0 && (dataReceived = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
					dataOutputSteam.write(buffer, 0, dataReceived);

					fileSize -= dataReceived;
				}

			} else {
				outputLine = inputLine; // passes the input from the machine to the output string for the destination

				if ( outSocket != null){
					outTo.println(outputLine); // writes to the destination
				}
				if (inputLine.equals("Bye.")) // exit statement
					break;
			}
       }// end while		 
		 }// end try
			catch (IOException e) {
               System.err.println("Could not listen to socket.");
               System.exit(1);
         }
	}
}