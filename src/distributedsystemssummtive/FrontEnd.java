package distributedsystemssummtive;

import java.net.ServerSocket;
import java.net.Socket;

public class FrontEnd{
    
    public static void main(String[] args){
        try{
            int port = 18300;
            create(port);
        }
        catch(Exception e){
            System.err.println("Error creating front end server. Check port is clear.");
        }
    }
    public static void create(int port) throws Exception{     
        String request;
        ServerSocket welcomeSocket;
        Socket connectionSocket;
        welcomeSocket = new ServerSocket(port);
        System.out.println("Front end server created on port: "+port);
        while(true) { 
            connectionSocket = welcomeSocket.accept();
            Thread thread = new Thread(new FrontEndThread(connectionSocket));
            thread.start();
        }
    }
}
