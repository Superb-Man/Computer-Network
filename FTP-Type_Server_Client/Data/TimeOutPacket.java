package Data;

public class TimeOutPacket extends Packet {
    private int fileId;
    public TimeOutPacket(String from, int fileId) {
        super(from, PacketType.TransferTimeout);
        this.fileId = fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getFileId() {
        return fileId;
    }
}
