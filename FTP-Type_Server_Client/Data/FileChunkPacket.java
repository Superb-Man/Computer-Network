package Data;

public class FileChunkPacket extends Packet {
    private int fileId;
    private byte[] data;
    private int chunkId;

    public FileChunkPacket(String from, int fileId, byte[] data, int chunkId){
        super(from, PacketType.FileChunk);
        this.fileId = fileId;
        this.data = data;
        this.chunkId = chunkId;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getChunkId() {
        return chunkId;
    }

    public void setChunkId(int chunkId) {
        this.chunkId = chunkId;
    }
}
