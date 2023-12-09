package Data;

public class UserFilesRequestPacket extends Packet {
    private String user;
    public UserFilesRequestPacket(String from, String user){
        super(from, PacketType.ListOfUserFilesRequest);
        this.user = user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }
}
