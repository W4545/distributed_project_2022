import java.util.Objects;

public class RouterRecord {

    private final char groupID;
    private final String ipAddress;

    public RouterRecord(char groupID, String ipAddress) {
        this.groupID = groupID;
        this.ipAddress = ipAddress;
    }

    public char getGroupID() {
        return groupID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouterRecord that = (RouterRecord) o;
        return groupID == that.groupID && ipAddress.equals(that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupID, ipAddress);
    }

    @Override
    public String toString() {
        return "RouterRecord{" +
                "groupID='" + groupID + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
