package Server;

import Data.Packet;
import Util.MyConnection;
import Util.NetworkUtil;
import com.sun.jdi.event.ExceptionEvent;

import java.net.Socket;

public class ClientConnection extends Thread {
    private NetworkUtil networkUtil ;
    private String id ;
    private Server server ;

    public ClientConnection(Socket socket , Server server) {
        try{
            this.networkUtil = new NetworkUtil(socket) ;
            Packet packet = (Packet) networkUtil.read() ;
            this.id = packet.getFrom() ;
        }catch(Exception e) {
            e.printStackTrace() ;
            print("Could not get input/output streams in client listener","") ;
        }

        this.server  = server ;
    }
    public String getID() {
        return id ;
    }

    public void setId(String id) {
        this.id = id;
    }
    public Server getServer() {
        return server;
    }

    protected void print(String msg1, String msg2) {
        MyConnection.getConnection().log(msg1) ;
        System.out.println(msg2) ;
    }

    public void send(Packet packet) {
        try {
            //MyConnection.getConnection().log(msg);
            networkUtil.write(packet);
        } catch (Exception e) {
            print("something went wrong with writing for id "+id ,"");
            e.printStackTrace() ;
        }
    }

    public void closeConnection() {
        try{
            networkUtil.closeConnection() ;
            this.interrupt() ;
        }catch(Exception e) {
            e.printStackTrace() ;
            print("Couldnot Close Connection for id "+id,"") ;
        }
    }

    @Override
    public void run() {
        while(true) {
            try{
                Packet packet = (Packet) networkUtil.read() ;
                print("Received packet from id "+id, "") ;

                synchronized (server.getmBuffer()) {
                    //this is Synchronised because its need to be thread safe
                    server.getmBuffer().add(packet) ;
                    //notify others to access this Buffer!
                    server.getmBuffer().notify() ;

                    print("Packet sent from id "+id + " to server", "") ;
                }
            }catch(Exception e) {
                server.remove(this) ;
                break ;

            }
        }

    }


}
