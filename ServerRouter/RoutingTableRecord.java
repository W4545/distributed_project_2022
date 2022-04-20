import java.net.Socket;
import java.util.Objects;

public class RoutingTableRecord {

    private final String ipAddress;
    private final Socket clientSocket;

    private final String clientID;

    private final String listenPort;

    public RoutingTableRecord(String ipAddress, Socket clientSocket, String clientID, String listenPort) {
        this.ipAddress = ipAddress;
        this.clientSocket = clientSocket;
        this.clientID = clientID;
        this.listenPort = listenPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public String getClientID() {
        return clientID;
    }

    public String getListenPort() {
        return listenPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutingTableRecord that = (RoutingTableRecord) o;
        return ipAddress.equals(that.ipAddress) && clientSocket.equals(that.clientSocket) && clientID.equals(that.clientID) && listenPort.equals(that.listenPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, clientSocket, clientID, listenPort);
    }

    @Override
    public String toString() {
        return "RoutingTableRecord{" +
                "ipAddress='" + ipAddress + '\'' +
                ", clientSocket=" + clientSocket +
                ", clientID='" + clientID + '\'' +
                ", listenPort='" + listenPort + '\'' +
                '}';
    }
}
