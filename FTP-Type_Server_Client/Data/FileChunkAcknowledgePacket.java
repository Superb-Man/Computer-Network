package Data;

public class FileChunkAcknowledgePacket extends Packet {
    private int fileId;
    private int chunkId;
    public FileChunkAcknowledgePacket(String from, int fileId, int chunkId){
        super(from, PacketType.FileChunkAcknowledgement);
        this.fileId = fileId;
        this.chunkId = chunkId;
    }

    public void setChunkId(int chunkId) {
        this.chunkId = chunkId;
    }

    public int getChunkId() {
        return chunkId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getFileId() {
        return fileId;
    }
}
