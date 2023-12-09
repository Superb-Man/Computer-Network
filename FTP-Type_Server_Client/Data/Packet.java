package Data;

import java.io.Serializable;

public class Packet implements Serializable {
    protected PacketType packetType;
    protected String from;
    public Packet(String from, PacketType packetType){
        this.from = from;
        this.packetType = packetType;
    }
    public void setFrom(String from) {
        this.from = from ;
    }
    public String getFrom(){
        return this.from ;
    }

    public void setPacketType(PacketType packetType) {
        this.packetType = packetType;
    }

    public PacketType getPacketType() {
        return packetType;
    }
}




