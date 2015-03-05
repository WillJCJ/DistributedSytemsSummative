package distributedsystemssummtive;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Replica{
   
    public static void main(String[] args){
        String serverNum = "";
        try {
            BufferedReader inputs = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please enter the number of this replicas:");
            serverNum = "1";//inputs.readLine();
        }
        catch (Exception e) {
            System.err.println("Error reading user input: " + e);
        }
        int serverInt = Integer.parseInt(serverNum);
        try{
            create(18300+serverInt, "movies.json");
        }
        catch(Exception e){
            System.out.println("Error creating replica: " +e);
        }
    }
    
    public static void create(int port, String filePath) throws IOException{     
        String request;
        ServerSocket welcomeSocket;
        Socket connectionSocket;
        welcomeSocket = new ServerSocket(port);
        System.out.println("Server created on port: "+port+".  Waiting for front end server to connect.");
        while(true) { 
            connectionSocket = welcomeSocket.accept();
            System.out.println("Accepted connection.  Creating new thread.");
            Thread thread = new Thread(new ReplicaThread(filePath, connectionSocket));
            thread.start();
            System.out.println("Created thread.");
        }
    }
}
