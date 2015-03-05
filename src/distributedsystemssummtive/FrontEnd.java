package distributedsystemssummtive;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class FrontEnd{
    
    public static void main(String[] args){
        try{
            int port = 18300;
            create(port);
        }
        catch(Exception e){
            System.err.println("Error creating middle server. Check port is clear.");
        }
    }
    public static void create(int port) throws Exception{     
        String request;
        ServerSocket welcomeSocket;
        Socket connectionSocket;
        welcomeSocket = new ServerSocket(port);
        System.out.println("Middle server created on port: "+port);
        while(true) { 
            connectionSocket = welcomeSocket.accept();
            Thread thread = new Thread(new FrontEndThread(connectionSocket));
            thread.start();
        }
    }
}
