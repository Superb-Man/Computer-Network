package Data;

public class TransferCompletePacket extends Packet{
    private int fid;
    public TransferCompletePacket(String from, int fid){
        super(from, PacketType.FileTrasferComplete);
        this.fid = fid ;
    }

    public int getFid() {
        return fid;
    }

    public void setFid(int fid) {
        this.fid = fid;
    }
}