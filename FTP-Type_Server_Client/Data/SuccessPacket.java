package Data;

public class SuccessPacket extends Packet {
    private boolean success ;

    public SuccessPacket(String from , boolean success) {
        super(from,PacketType.BooleanResponse) ;
        this.success = success ;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
