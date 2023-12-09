package Server;

import Data.*;

import java.io.IOException;

public class ServerResponse {
    public static void main(String[] args) throws IOException {
        Server server = new Server() ;
        server.start() ;

        while(true) {
            Packet packet = server.bufferRead() ;
            PacketType type = packet.getPacketType() ;
            ClientConnection cl = server.getConnections().get(packet.getFrom()) ;
            if(type == PacketType.FileChunk) {
                FileChunkPacket fileChunkPacket = (FileChunkPacket) packet ;
                if(!server.getFileReceivers().containsKey(((FileChunkPacket) packet).getFileId())){
                    server.print("received unknown chunk, fileId: "+((FileChunkPacket) packet).getFileId()+" , chunkid: "+((FileChunkPacket) packet).getFileId(),"");
                }
                else {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    server.getFileReceivers().get(fileChunkPacket.getFileId()).addChunk(fileChunkPacket, server);
                    //cl.send(new FileChunkAcknowledgePacket("server", fileChunkPacket.getFileId(), fileChunkPacket.getChunkId()));
                }
            }
            else if(type == PacketType.ListOfOwnfilesRequest) {
                server.print("Own files request","") ;
                cl.send(new DataPacket("server",PacketType.ListResponse,server.getUserFiles(packet.getFrom())));
            }
            else if(type == PacketType.ListOfUserFilesRequest) {
                UserFilesRequestPacket received = (UserFilesRequestPacket) packet ;
                String str = "Received request of files of "+ received.getUser()+" from"+ received.getFrom() ;
                server.print(str,"") ;
                cl.send(new DataPacket("server",PacketType.ListResponse,server.getUserFiles(received.getUser())[0])) ;
            }
            else if(type == PacketType.ListOfUserRequest) {
                server.print("Received request for from "+packet.getFrom(),"") ;
                cl.send(new DataPacket("server",PacketType.ListResponse,server.getConnectionList()));
                server.print("Sent list to"+packet.getFrom(),"") ;
            }
            else if(type == PacketType.FileRequest) {
                //Taking the broadcasting messages
                DataPacket received = (DataPacket) packet ;
                Request request = (Request) received.getData() ;

                //Now all the request is retrieved
                server.print("Received file request for "+request.getDescription()+" from "+ request.getFrom(),"") ;
                //Global requests id setting, irrespective of Clients
                request.setId(server.getAllReqs().size());

                server.getAllReqs().add(request) ;

                //need to check in all connection
                for(var id : server.getAll()) {
                    if(!id.equalsIgnoreCase(request.getFrom())) {
                        //adding into map of queue differentiated by Client_ID
                        server.getReqs().get(id).add(request) ;
                    }
                }
                server.print("added request","") ;
            }
            else if(type == PacketType.UnreadMessageRequest) {
                server.print("Received request for unread messages of "+packet.getFrom(),"") ;
                cl.send(new DataPacket("server",PacketType.ListResponse,server.unreadMessages(packet.getFrom()))) ;
                server.print("Sent Unread messages", "") ;
            }

            else if(type == PacketType.FileTransferRequest) {
                server.print("Received request for FileTransfer of "+packet.getFrom(),"") ;
                FileTransferRequestPacket received  = (FileTransferRequestPacket) packet ;
                server.print("Received file upload request, name :"+received.getFilename()+" size : "+received.getFilesize()+" public : "+ received.isPublic()+ "Request id : "+received.getRequestId(),"") ;
                //now need to handlefile_upload
                server.uploadHandling(received) ;
            }

            else if(type == PacketType.FileTrasferComplete) {
                TransferCompletePacket transferCompletePacket = (TransferCompletePacket) packet ;
                if(!server.getFileReceivers().containsKey(transferCompletePacket.getFid())){
                    server.print("received unknown completion packet, fileId: "+transferCompletePacket.getFid(),"");
                }
                server.print("Received complete packet for file "+transferCompletePacket.getFid(),"") ;
                int yes = server.getFileReceivers().get(transferCompletePacket.getFid()).finish(false,server) ;
                cl.send(new TransferCompleteResponse("server",yes)) ;
            }

            else if(type == PacketType.TransferTimeout) {
                //print("TransferTimeout packet received","") ;
                TimeOutPacket timeOutPacket = (TimeOutPacket) packet ;

                if(!server.getFileReceivers().containsKey(timeOutPacket.getFileId())){
                    server.print("received unknown timeout packet, fileId: "+timeOutPacket.getFileId(),"");
                }
                else{
                    server.print("Received timeout packet for " + timeOutPacket.getFileId(), "");
                    server.getFileReceivers().get(timeOutPacket.getFileId()).finish(true, server);
                }

            }

            else if(type == PacketType.DownloadRequest) {
                DownloadRequestPacket downloadRequestPacket = (DownloadRequestPacket) packet ;
                server.print("Received Download request packet for "+ downloadRequestPacket.getFilename()+" of "+downloadRequestPacket.getUser()+" from "+downloadRequestPacket.getFrom(),"");
                server.send(downloadRequestPacket.getFilename(),downloadRequestPacket.getUser(),downloadRequestPacket.getFrom()) ;
            }
        }
    }
}
