package Client;

import Data.*;
import Util.MyConnection;
import Util.NetworkUtil;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

public class ClientThread extends Thread {
    private String id ;
    private NetworkUtil networkUtil;
    private Queue<Packet> buffer = new LinkedList<>();
    boolean timeout = false ;

    public ClientThread(String id) {
        this.id = new String(id);
        try {
            this.networkUtil = new NetworkUtil("localhost",6262) ;
            send(new Packet(id,PacketType.ConnectionRequest));

            SuccessPacket response = (SuccessPacket) networkUtil.read() ;

//            if(networkUtil.getSocket()!=null) {
//                System.out.println("Not Null") ;
//            }
            System.out.println(response.isSuccess());

            if(response.isSuccess()) {
                //System.out.println("I'm logged in") ;
                print("Connection Accepted by Server","");
            }
            else {
                //System.out.println("Packet false");
                networkUtil.setSocket(null) ;
                print("Connection Rejected","");
            }
            print("Here",""); ;
        }catch(Exception e) {
            print("Error Occured","");
            e.printStackTrace();
        }
        //thread.start() ;
    }

    public NetworkUtil getNetworkUtil() {
        return networkUtil;
    }

    protected void print(String msg1, String msg2) {
        MyConnection.getConnection().log(msg1) ;
        System.out.print(msg2) ;
        if(msg2 != "") System.out.println() ;
    }

    protected Packet AckRead() throws SocketTimeoutException,InterruptedException{
        synchronized (buffer) {
            if (buffer.isEmpty())
                buffer.wait();
            return buffer.poll();
        }
    }


    private Packet bufferRead() {
        try {
            synchronized (buffer) {
                if (buffer.isEmpty()) {
                    buffer.wait();
                }
                return buffer.poll();
            }
        }catch (InterruptedException e) {
            print("Interrupt","") ;
        }
        catch (Exception e) {
            print("read from buffer failed","");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Packet packet = (Packet) networkUtil.read();
                print(packet.getClass().getName(),"");
                print("received a packet", "") ;
                synchronized (buffer) {
                    buffer.add(packet);
                    buffer.notify();
                }
            }catch (Exception e) {
                print("Stopping Listening","");
                break;
            }
        }
    }

    public boolean isOpen() {
        if(networkUtil.getSocket() != null) {
            return true ;
        }
        else{
            return false ;
        }
    }


    public void send(Packet packet) {
        try {
            //MyConnection.getConnection().log(msg);
            networkUtil.write(packet);
        } catch (Exception e) {
            print("something went wrong with writing","");
            e.printStackTrace() ;
        }
    }

    public Packet recieve(String msg1,String msg2) {
        print(msg1,msg2) ;
        return bufferRead() ;
    }

    public String[][] requestConnections() {
        if(timeout == true) {
            bufferRead() ;
        }
        send(new Packet(id, PacketType.ListOfUserRequest)) ;
        print("Sending list of user request packet","");

        DataPacket packet = (DataPacket) bufferRead() ;
        print("received list of user packet","");
        return (String[][]) packet.getData() ;
    }


    public String[][] reqMyFiles() {
        if (timeout == true) {
            bufferRead() ;
        }
        send(new Packet(id, PacketType.ListOfOwnfilesRequest)) ;
        print("Sending list of own files request packet","");

        DataPacket packet = (DataPacket) bufferRead() ;
        print("received list of own files packet","");
        return (String[][]) packet.getData();
    }
    public String[] reqOtherFiles(String userId) {
        if (timeout == true) {
            bufferRead() ;
        }
        send(new UserFilesRequestPacket(id, userId)) ;
        print("Sending list of files of " + userId + " request packet","") ;

        DataPacket packet = (DataPacket) bufferRead() ;
        print("received list of files of " + userId + " packet","");
        return (String[]) packet.getData();
    }


    public void sendRequestForFile(String description) {
        if (timeout == true) {
            bufferRead() ;
        }
        //broadcast message
        send(new DataPacket(id, PacketType.FileRequest, new Request(id, description))) ;
        print("Sending Request for `"+description+"`","");

        return;
    }


    public Request[] getUnreadMesssages() {
        if (timeout == true) {
            bufferRead() ;
        }
        send(new Packet(id, PacketType.UnreadMessageRequest));

        DataPacket packet = (DataPacket) recieve("receieved unread messages","") ;
        return (Request[]) packet.getData();
    }


    public void upload (File file, boolean publicFile , int requestId) {
        if (timeout == true) {
            bufferRead() ;
        }

        if(!file.exists()) {
            print("File doesnot exist",""); ;
            return ;
        }

        String name = (String) file.getName() ;
        int totSize = (int) file.length() ;

        send(new FileTransferRequestPacket(id,name,totSize,publicFile,requestId)) ;
        print("Sending a request for uploading file "+ name,"");

        FileTransferResponsePacket packet = (FileTransferResponsePacket) bufferRead() ;

        if(packet.getFileId() == -1) {
            MyConnection.getConnection().log("File upload request for file name "+name+" rejected") ;
            //System.out.println("File Upload request Rejected sad") ;
            return ;
        }

        print("File upload request for file name "+name+" accepted ","File Upload request Accepted") ;


        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file)) ;
            int chID = 0 ;
            for (int sent_file_size = 0 ; sent_file_size < totSize ;){
                int size ;
                if(totSize - sent_file_size < packet.getChunkSize()) {
                    size = totSize - sent_file_size ;
                }
                else {
                    size = packet.getChunkSize() ;
                }

                byte[] content = new byte[size];
                in.read(content ,0, size);
                send(new FileChunkPacket(id, packet.getFileId(), content , chID));
                print("Sent file chunk "+ chID + " of fileId "+packet.getFileId(),"") ;
                if(wait(chID)) {
                    sent_file_size += size;
                    chID++ ;
                }
                else {


                    //  taking much time and not acknowledged...
                    //needs to exit the uploading with an error meassage
                    send(new TimeOutPacket(id,packet.getFileId())) ;
                    print("Sent timeout packet","Upload timedout");
                    timeout = true ;
                    in.close() ;

                    return;
                }

            }
            in.close();
            timeout = false ;

            send(new TransferCompletePacket(id, packet.getFileId())) ;
            print("sending compleition message","");

            TransferCompleteResponse response = (TransferCompleteResponse) bufferRead() ;
            print("File "+name+" upload is "+(response.getSuccess() == 1 ?"success":"failure"),"Upload successful");
        } catch (Exception e) {
            e.printStackTrace() ;
            print("Something wrong with sending file","Upload failed");
        }


    }

    private boolean wait(int chunkId) {

        //Rifat sirs code
        ExecutorService executor = Executors.newCachedThreadPool();

        //taking help of internet
        Callable<FileChunkAcknowledgePacket> task = new Callable<FileChunkAcknowledgePacket>() {
            public FileChunkAcknowledgePacket call() {
                return (FileChunkAcknowledgePacket) bufferRead() ;
            }
        };


        Future<FileChunkAcknowledgePacket> f = executor.submit(task);
        try {
            FileChunkAcknowledgePacket res = f.get(10, TimeUnit.SECONDS);
            if(res.getChunkId() == chunkId){
                print("received acknowledgement for "+chunkId,"LOL");
                return true;
            }
            else{
                print(res.getChunkId()+", "+chunkId+" chunkid's dont match","") ;
            }
        } catch (TimeoutException e) {
            print("Timeout occured","");
            e.printStackTrace();
        } catch (InterruptedException e) {
            print("Interrupted","") ;
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
//            e.printStackTrace();
        } finally {
            f.cancel(true);

            //shuting down
            executor.shutdown();
        }
        return false;
    }




    public void download(String trgtID, String name) {
        if (timeout == true) {
            bufferRead() ;
        }

        send(new DownloadRequestPacket(id, trgtID, name)) ;
        print("Sent download request for "+trgtID+", file: "+name,"");

        FileTransferResponsePacket response = (FileTransferResponsePacket) bufferRead();

        if(response.getFileId() == -1){
            print("File download request denied","File download request denied");
            return;
        }
        print("received file download permission","") ;




        //ceiling value ;
        int chnkCnt = (response.getFileSize() + response.getChunkSize() - 1)/response.getChunkSize() ;

        try {
            File file = new File(name);
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));

            while (chnkCnt > 0){
                FileChunkPacket chunk = (FileChunkPacket) bufferRead() ;
                print("received chunk "+chunk.getChunkId()+" and saving it",""); ;

                os.write(chunk.getData(), 0, chunk.getData().length);
                chnkCnt-- ;
            }

            TransferCompletePacket finalpacket = (TransferCompletePacket) bufferRead() ;
            print("Received download complete packet and saving file","Download complete") ;
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
            print("something wrong with downloading file","Download failed");
        }

    }

    public void closeConnection() {
        try{
            if(networkUtil.getSocket() != null) {
                this.interrupt() ; networkUtil.closeConnection() ;
            }
        }catch (Exception e){
            e.printStackTrace() ;
        }
    }

}

