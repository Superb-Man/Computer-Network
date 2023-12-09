package Data;

public class FileTransferRequestPacket extends Packet {
    private String filename;
    private int filesize;
    private boolean isPublic;
    private int requestId;
    public FileTransferRequestPacket(String from, String filename, int filesize, boolean isPublic, int requestId) {
        super(from, PacketType.FileTransferRequest);
        this.filename = filename;
        this.filesize = filesize;
        this.isPublic = isPublic;
        this.requestId = requestId;
    }

    public void setFilesize(int filesize) {
        this.filesize = filesize;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public int getFilesize() {
        return filesize;
    }

    public String getFilename() {
        return filename;
    }

    public int getRequestId() {
        return requestId;
    }
}
