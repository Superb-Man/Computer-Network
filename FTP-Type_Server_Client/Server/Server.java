package Server;

import Data.*;
import Util.MyConnection;

import javax.net.ssl.SSLServerSocket;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class FileUtil {
    private int filId;
    private int filSize;
    private int receivedChId;
    private String from;
    private File file;
    private BufferedOutputStream out;
    private int requestId;

    protected void print(String msg1, String msg2) {
        MyConnection.getConnection().log(msg1);
        System.out.println(msg2);
    }


    public FileUtil(String from,int filSize,File file, int requestId,int filId) {
        this.filId = filId;
        this.filSize = filSize;
        this.file = file;
        this.from = from;
        this.requestId = requestId;
        if (file.exists()) {
            print("WARNING:::overwriting file " + file.getPath(), "");
        }
        this.receivedChId = 0;
        try {
            this.out = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            print("Something wrong with setting outptustream", "");
            e.printStackTrace();
        }
        print("Created file receiver for " + filId, "");
    }

    public String getFrom() {
        return from;
    }

    public void addChunk(FileChunkPacket chunkPacket, Server server) {
        try {
            synchronized(this){
                if(chunkPacket.getChunkId() != receivedChId ) {
                    print("ERROR:::chunk index does not match","") ;
                    return  ;
                }

                out.write(chunkPacket.getData(), 0, chunkPacket.getData().length);
                //For safe file transfering we are using a recieved Chunk ID for the filereciving object
                receivedChId++;
                //sending the ACK packet to recieve next data
                server.getConnections().get(chunkPacket.getFrom()).send(new FileChunkAcknowledgePacket("server", filId, chunkPacket.getChunkId()));
                print("Sending ack for chunk "+chunkPacket.getChunkId(),"");
            }
        } catch (Exception e) {
            //error while saving data
            print("Something wrong with saving data","LOL") ;
            e.printStackTrace();
        }
    }

    public int finish(boolean delete,Server server) {
        print("Receiving of file "+filId+" Ended","");
        try {
            out.close();
        } catch (Exception e) {
            print("buffer-Closing exception","") ;
            e.printStackTrace();
        }
        server.getFileReceivers().remove(filId) ;
        synchronized (server.bSize) {
            if (delete || file.length() != filSize) {
                print("Deleted file" + file.getPath(), "");
                server.bSize = server.bSize - filSize;
                file.delete();
                //server.getConnections().get(from).send(null) ;
                return 0 ;
            }
        }
        if(requestId != -1)     server.requestSuccess(requestId, from, file.getName());

        return 1 ;
    }
}





//Server Class Implementation
public class Server extends Thread {
    private Queue<Packet> mBuffer = new LinkedList<>();
    private int fileCount = 0;
    public Integer bSize = 0;
    //The name and socket map for each client
    private HashMap<String, ClientConnection> connections = new HashMap<>();
    //To store all the same id entrance once ;
    private HashSet<String> all = new HashSet<>();
    //for each client there should be a queue of requests for unread message
    private HashMap<String, Queue<Request>> reqs = new HashMap<>();
    private HashMap<Integer, FileUtil> fileReceivers = new HashMap<>();
    //Listing all requests!
    private List<Request> allReqs = new LinkedList<>();
    private ServerSocket serverSocket;

    public Queue<Packet> getmBuffer() {
        return mBuffer;
    }

    public HashMap<Integer, FileUtil> getFileReceivers() {
        return fileReceivers;
    }

    protected Packet bufferRead() {
        try {
            synchronized (mBuffer) {
                if (mBuffer.isEmpty())
                    mBuffer.wait();
                return mBuffer.poll();
            }
        }catch (InterruptedException e) {
            e.printStackTrace(); ;
        } catch (Exception e) {
            print("read from buffer failed", "");
            e.printStackTrace();
        }
        return null;
    }

    public List<Request> getAllReqs() {
        return allReqs;
    }

    public HashMap<String, Queue<Request>> getReqs() {
        return reqs;
    }

    public HashSet<String> getAll() {
        return all;
    }

    public void print(String msg1, String msg2) {
        MyConnection.getConnection().log(msg1);
        System.out.println(msg2);
    }

    protected void createDirectory(String path) {
        File directory = new File(path);
        //Now check if a folder already exists!
        if (directory.exists()) {
            print("Already exists", "");
            return;
        }
        if (directory.mkdir()) {
            print("Created " + path + " Successfully", "");
            return;
        }
        print("Creation failed", "");
    }

    protected void connectUser() {
        //Nothing
    }

    protected void remove(ClientConnection client) {
        if (connections.containsKey(client.getID())) {
            //removing if its in connection map
            connections.remove(client.getID());
            print("Removing Client of listener id " + client.getID() + " from server", "");
        }
        //need to handle this
        //file recieving problem may occur
        //at the time of removing connection server needs to stop recieving -->

        for (var id : fileReceivers.keySet()) {
            if (fileReceivers.get(id).getFrom().equalsIgnoreCase(client.getID()))
                fileReceivers.get(id).finish(true, this);
        }

        client.closeConnection();
    }

    public HashMap<String, ClientConnection> getConnections() {
        return connections;
    }

    //Constructor
    public Server() throws IOException {
        this.serverSocket = new ServerSocket(6262);
        createDirectory("data/");
    }

    public void closeServer() {
        for (ClientConnection cl : connections.values()) {
            cl.closeConnection();
            try {
                cl.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        try {
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                //Rifat Sir's code lol
                Socket socket = serverSocket.accept();
                ClientConnection client = new ClientConnection(socket, this);

                print("new Connection request from " + client.getID(), "");
                if (connections.containsKey(client.getID())) {

                    SuccessPacket packet = new SuccessPacket("server", false);
                    client.send(packet);
                    print("Already Connected id " + client.getID(), "");
                } else {
                    System.out.println("Connected Error Checking");
                    SuccessPacket packet = new SuccessPacket("server", true);
                    //sending success connection packet
                    client.send(packet);
                    print("Newly Connected id " + client.getID(), "");
                    //adding in hashmap
                    connections.put(client.getID(), client);
                    //starting thread
                    client.start();

                    /**
                     * Now we have to check if its already in conection that means directory was
                     * created before
                     */
                    if (all.contains(client.getID())) {
                        print(client.getID() + " is an old user", "");
                        continue;
                    }
                    print(client.getID() + " is a new user", "");
                    createDirectory("data/" + client.getID());
                    createDirectory("data/" + client.getID() + "/public");
                    createDirectory("data/" + client.getID() + "/private");

                    //Now insert the clientId in set
                    all.add(client.getID());
                    //Now again adding all the requests back in the request queue map
                    //Because all the requests are saved in all requests for active /inactive users according to ID

                    reqs.put(client.getID(), new LinkedList<>());
                    for (Request request : allReqs) {
                        Queue<Request> requests = reqs.get(client.getId());
                        reqs.get(client.getId()).add(request);
                    }

                }

            } catch (SocketException e) {
                print("Stopping Server", "");
                return;
            } catch (Exception e) {
                print("Something went Fishy", "");
                e.printStackTrace();
            }

        }
    }

    public Request[] unreadMessages(String id) {
        //Need to return array of requests
        List<Request> ur = new ArrayList<>();
        //synchronised block to noty access here while getting tha data
        //synchronized (this) {
        //delete from the reqs map and add it into the array
        while (!reqs.get(id).isEmpty()) {
            //request queue  for entry of id is not empty
            //removing the head of queue of entry(ID)
            ur.add(reqs.get(id).poll());
        }
        //}
        Request[] messages = new Request[ur.size()];
        return ur.toArray(messages);
    }

    public String[][] getUserFiles(String id) {
        if (!all.contains(id)) {
            //is not even registered!
            return new String[2][0];
        }
        //creating two array of strings
        //one for public
        //one for private
        String[][] res = new String[2][];

        int idx = 0;

        File publicDirectory = new File("data/" + id + "/public");
        res[0] = new String[publicDirectory.listFiles().length];
        for (var file : publicDirectory.listFiles()) {
            res[0][idx++] = file.getName();
        }

        File privateDirectory = new File("data/" + id + "/private");
        idx = 0;

        res[1] = new String[privateDirectory.listFiles().length];
        for (var file : privateDirectory.listFiles()) {
            res[1][idx++] = file.getName();
        }


        //returning the whole object, we can differentiate by 0,1 in request checking
        return res;
    }

    public String[][] getConnectionList() {
        String res[][] = new String[2][];
        //One for online
        //One for offline
        res[0] = new String[connections.size()];
        int idx;
        idx = 0;
        for (var id : connections.keySet()) {
            res[0][idx++] = id;
        }
        idx = 0;
        res[1] = new String[all.size() - connections.size()];
        for (var id : all) {
            if (!connections.containsKey(id)) {
                res[1][idx++] = id;
            }
        }


        return res;


    }

    public void send(String filename, String from, String to) {
        File file = new File("data/" + from + "/public/" + filename);

        if (file.exists()) {
            print("File exists", "");
            print("Found Path + " + file.getPath(), "");
        } else if (from.equalsIgnoreCase(to)) {
            file = new File("data/" + from + "/private/" + filename);
            if (!file.exists()) {
                //file doesn't exist
                print("Can't send file", "");
                //sending transferResponse to client
                FileTransferResponsePacket packet = new FileTransferResponsePacket("server", -1, -1, -1);
                ClientConnection cl = connections.get(to);
                cl.send(packet);

                return;
            }
            print("File exists", "");
            print("Found Path + " + file.getPath(), "");
        } else {
            //file doesn't exist
            print("Can't send file", "");
            //sending transferResponse to client
            FileTransferResponsePacket packet = new FileTransferResponsePacket("server", -1, -1, -1);
            ClientConnection cl = connections.get(to);
            cl.send(packet);

            return;
        }

        int totSize = (int) file.length();
        //if the file exist proceed
        int filId = fileCount++;

        ClientConnection cl = connections.get(to);
        cl.send(new FileTransferResponsePacket("server", 6000, filId, totSize));

        //granted downlaod persmission from sever LOL
        print("sent permission to dwonload " + filename + " of " + from + " , fileid: " + filId, "");
//
//        FileTransferResponsePacket packet = (FileTransferResponsePacket) bufferRead() ;
//
//        if(packet.getFileId() == -1) {
//            MyConnection.getConnection().log("File upload request for file name "+name+" rejected") ;
//            //System.out.println("File Upload request Rejected sad") ;
//            return ;
//        }
//
//        print("File upload request for file name "+name+" accepted ","File Upload request Accepted") ;


        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            int chID = 0;
            for (int sent_file_size = 0; sent_file_size < totSize; ) {
                int size;
                if (totSize - sent_file_size < 6000) {
                    size = totSize - sent_file_size;
                } else {
                    size = 6000;
                }

                byte[] content = new byte[size];
                in.read(content, 0, size);


                cl.send(new FileChunkPacket("server", filId, content, chID));
                print("Sent file chunk " + chID + " of fileId " + filId, "");

                chID++;
                sent_file_size += size;
            }
            in.close();

            cl.send(new TransferCompletePacket("server", filId));
            print("sending compleition message", "");

            //TransferCompleteResponse response = (TransferCompleteResponse) bufferRead() ;
        } catch (Exception e) {
            e.printStackTrace();
            print("Something wrong with sending file", "Upload failed");
        }
    }

    public void uploadHandling(FileTransferRequestPacket packet) {
        String name = packet.getFilename();
        //from which client server is receiviong
        ClientConnection cl = connections.get(packet.getFrom());
        synchronized (bSize) {
            if (bSize + packet.getFilesize() > 100_000_000) {
                print("Can't accept file :" + name + " \ncurrent bufferSize :" + bSize, "");
                //transfer fail in sever end..
                //receiver can't send
                cl.send(new FileTransferResponsePacket("server", -1, -1, -1));

                return;

            }

            bSize += packet.getFilesize();
            //print() ;
            print("Upload Possible\nNew Buffersize : " + bSize, "");
        }
        String path = "";
        path += "data/" + packet.getFrom() + "/";
        if (packet.isPublic()) {
            path += "public/";
        } else {
            path += "private/";
        }
        path += packet.getFilename();

        File file = new File(path);
        int chSize = new Random().nextInt(6000 - 5000 + 1) + 5000;
        int filId = fileCount++;


        //need to add something
        fileReceivers.put(filId, new FileUtil(packet.getFrom(), packet.getFilesize(), file, packet.getRequestId(), filId));

        //sending Filetranfer Response to Client
        cl.send(new FileTransferResponsePacket("server", chSize, filId, packet.getFilesize()));

    }

    public void requestSuccess(int reqId, String from, String filname) {
        Request request = new Request(from, "request " + reqId + " fulfilled with " + filname);
        reqs.get(allReqs.get(reqId).getFrom()).add(request);
    }
}