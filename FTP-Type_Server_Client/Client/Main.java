package Client;
import Data.Request;
import Data.SuccessPacket;
import Util.MyConnection;
import Util.NetworkUtil;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    public static void PrintCustom(int option,String[][] list) {
        for(String str : list[option]) {
            System.out.println(str) ;
        }
    }


    public static void main(String[] args) {

        //MyConnection.getConnection().swtichmode() ;
        Scanner scanner = new Scanner(System.in) ;
        System.out.print("Enter ID : ") ;
        String id ;
        id = scanner.next() ;

        ClientThread client = new ClientThread(id) ;
        if(client.isOpen()) {
            System.out.println("Connected to Server") ;
            client.start() ;
        }
        else {
            client.closeConnection() ;
            System.out.println("Shutdown,Rejected from server") ;
            return ;
        }



        loop:while(true) {
            System.out.println("\nOptions"+
                            "\n(L) list of connected users"+
                            "\n(M) see my files"+
                            "\n(O) see others files"+
                            "\n(R) request file"+
                            "\n(S) show unread messages"+
                            "\n(U) upload file"+
                            "\n(D) download file"+
                            "\n(E) exit"+
                            "\n> ") ;

            String cmd = scanner.next() ;


            scanner.nextLine() ;
            cmd = cmd.toUpperCase() ;
            switch (cmd) {
                case "L": {
                    System.out.println("Online:");
                    PrintCustom(0, client.requestConnections());
                    System.out.println("Offline:");
                    PrintCustom(1, client.requestConnections());
                }break ;

                case "M":{
                    System.out.println("My Public Files:");
                    PrintCustom(0, client.reqMyFiles());
                    System.out.println("My Private Files:");
                    PrintCustom(1, client.reqMyFiles());
                }break ;
                case "0": {
                    System.out.print("Enter User Id : ") ;
                    String userId = scanner.next();
                    scanner.nextLine() ;
                    System.out.println("public files of userId :"+userId);
                    for(String file : client.reqOtherFiles(userId)){
                        System.out.println(file) ;
                    }
                }break;
                case "R":{
                    System.out.print("Enter  description: ");
                    String des = scanner.nextLine() ;
                    client.sendRequestForFile(des) ;
                }break ;
                case "S":{
                    System.out.println("Unread messages are:") ;
                    for(Request request : client.getUnreadMesssages()) {
                        if(request.getId() == -1) {
                            System.out.println("from " +request.getFrom()+ " "+request.getDescription()) ;
                        }
                        else {
                            System.out.println(request.getFrom()+" requested for "+request.getDescription());
                        }
                    }
                }break ;
                case "U":{
                    System.out.print("Enter Path : ") ;
                    String filepath = scanner.nextLine();
                    File file = new File(filepath) ;

                    System.out.print("Upload public(Y/N) :") ;
                    boolean upload ;
                    upload = scanner.next().toUpperCase().equals("Y") ;
                    int reqId = -1 ;
                    //is true
                    if(upload) {
                        System.out.print("Is this upload request to some request ?(Y/N):");
                        boolean yes = scanner.next().toUpperCase().equals("Y") ;
                        if(yes) {
                            System.out.print("Enter Request Id : ") ;
                            reqId = scanner.nextInt() ;
                        }
                    }
                    scanner.nextLine() ;
                    try {
                        //client.getNetworkUtil().getSocket().setSoTimeout(30000);
                        client.upload(file, upload, reqId);
                   }catch (Exception e) {
                        System.out.println("TimeOut for upload");
                    }


                }break ;

                case "D":{
                    System.out.print("Enter userId whose file uou want to downlaod:") ;
                    String tId = scanner.next() ;
                    System.out.print("Enter File name:");
                    String name = scanner.next() ;
                    scanner.nextLine() ;
                    client.download(tId,name) ;
                }break ;

                case "E":
                    break loop ;
                default:
                    System.out.println("Command not valid") ;
                    break ;
            }

        }
        client.closeConnection();
        scanner.close() ;
    }

}