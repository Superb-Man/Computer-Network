package Data;

public class FileTransferResponsePacket extends Packet {
    private int chunkSize;
    private int fileId;
    private int fileSize;
    public FileTransferResponsePacket(String from, int chunkSize, int fileId, int fileSize){
        super(from, PacketType.FileTransferResponse);
        this.chunkSize = chunkSize;
        this.fileId = fileId;
        this.fileSize = fileSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getFileId() {
        return fileId;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }
}
