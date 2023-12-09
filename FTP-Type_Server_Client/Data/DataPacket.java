package Data;

public class DataPacket extends Packet {
    private Object data ;
    public DataPacket(String from , PacketType packetType ,Object data) {
        super(from,packetType) ;
        setData(data) ;
    }

    public void setData(Object data) {
        this.data = data ;
    }

    public Object getData() {
        return data;
    }
}