package distributedsystemssummtive;

import java.io.*;
import java.net.*;

public class Replica{
   private final static String filmFilePath = "movies.json";
    public static void main(String[] args){
        String serverNum = "";
        try {
            BufferedReader inputs = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please enter the number of this replica (1 is primary):");
            serverNum = inputs.readLine();
        }
        catch (Exception e) {
            System.err.println("Error reading user input: " + e);
        }
        int serverInt = Integer.parseInt(serverNum);
        try{
            create(18300+serverInt, filmFilePath);
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
