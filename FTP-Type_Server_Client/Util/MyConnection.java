package Util;

import java.util.Date;

public class MyConnection {

    //use of swe 3-1 LOL
    private boolean activated = true ;
    public static MyConnection connection = null ;

    //Only need a connection to establish
    //private constructor
    private MyConnection() {

    }

    public void swtichmode() {
        activated = !activated ;
    }

    public void log(String msg) {
        synchronized (this) {
            if (activated) {
                System.err.println("[ " + new Date() + " ] " + msg);
            }

            return;
        }
    }

    public void print(String msg1, String msg2) {
        log(msg1) ;
        System.out.print(msg2) ;
        if(msg2 != "") System.out.println() ;
    }



    public static MyConnection getConnection(){
        if(connection == null) {
            //creating only one connection instance for all
            connection = new MyConnection() ;
        }

        return connection ;
    }
}
