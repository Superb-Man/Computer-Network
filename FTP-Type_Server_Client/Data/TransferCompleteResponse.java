package Data;

public class TransferCompleteResponse extends Packet {
    private int success ;
    public TransferCompleteResponse(String from,int success) {
        super(from,PacketType.FileTrasferComplete) ;
        this.success = success ;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getSuccess() {
        return success;
    }
}
