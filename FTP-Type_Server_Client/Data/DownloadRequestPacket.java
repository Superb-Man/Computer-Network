package Data;

public class DownloadRequestPacket extends Packet {
    private String user;
    private String filename;
    public DownloadRequestPacket(String from, String user, String filename){
        super(from, PacketType.DownloadRequest);
        this.user = user;
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
