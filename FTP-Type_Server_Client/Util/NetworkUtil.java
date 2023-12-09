package Util;

import Data.FileChunkAcknowledgePacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

public class NetworkUtil {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public NetworkUtil(String s, int port) throws IOException {
        this.socket = new Socket(s, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public NetworkUtil(Socket s) throws IOException {
        this.socket = s;
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public Object read() throws IOException, ClassNotFoundException{
        return ois.readObject();
    }

    public void write(Object o) throws IOException {
        oos.writeObject(o);
    }

    public void closeConnection() throws IOException {
        socket.close() ;
        ois.close();
        oos.close();
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
//    private boolean waitForAcknowledgement(int chID) {
//
//        ExecutorService executor = Executors.newCachedThreadPool();
//        Callable<FileChunkAcknowledgePacket> task = new Callable<FileChunkAcknowledgePacket>() {
//            public FileChunkAcknowledgePacket call() {
//                return (FileChunkAcknowledgePacket)bufferRead();
//            }
//        };
//        Future<FileChunkAcknowledgePacket> future = executor.submit(task);
//        try {
//            FileChunkAcknowledgePacket result = future.get(WAIT, TimeUnit.SECONDS);
//            if(result.getChunkId() != chID){
//                MyConnection.getConnection().log("ERROR::: "+result.getChunkId() +", "+chID +" chunkid's dont match");
//            }
//            else{
//                MyConnection.getConnection().log("received acknowledgement for "+chID);
//                return true;
//            }
//        } catch (TimeoutException ex) {
//            MyConnection.getConnection().log("Timeout occured");
//        } catch (InterruptedException e) {
//            MyConnection.getConnection().log("Interrupt in timeout");
//        } catch (ExecutionException e) {
//            MyConnection.getConnection().log("Executor problem in timeout");
//        } finally {
//            MyConnection.getConnection().log("Cancelling");
//            future.cancel(true);
//            executor.shutdown();
//        }
//        return false;
//    }
}
