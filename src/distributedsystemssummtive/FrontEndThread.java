package distributedsystemssummtive;

import java.io.*;
import java.net.*;
import org.json.*;

public class FrontEndThread implements Runnable{
    private String movieData;
    private String request;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    private DataOutputStream outToReplica;
    private BufferedReader inFromReplica;
    private String serverIP;
    private int serverPort;
    private Socket clientSocket;
    private Socket replicaSocket;
    
    public FrontEndThread(Socket socket){
        clientSocket = socket;
        readServer("server.txt");
        try{
            inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToClient = new DataOutputStream(clientSocket.getOutputStream());
        }
        catch(Exception e){
            System.err.println("Error creating input/output streams: "+e);
        }
    }
    
    public void run(){
        while (true) {
            try {
                request = inFromClient.readLine();
                System.out.println("Received '" + request + "' from client.");
            } catch (Exception e) {
                System.err.println("Error receiving from client: " + e);
            }
            String resultString = "";
            if(request.toLowerCase().equals("quit") || request.toLowerCase().equals("close") || request.toLowerCase().equals("exit")){
                resultString = "Closing connection.  Thank you for using this service.";
                try {
                    outToClient.writeBytes(resultString + "\n");
                    System.out.println("Sent " + resultString + " back");
                } catch (IOException e) {
                    System.out.println("Error sending data to client: " + e);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing connection with client: " + e);
                }
                break; //End the while loop
            }
            try {
                System.out.println(serverIP);
                System.out.println(serverPort);
                replicaSocket = new Socket(serverIP, serverPort);
                System.out.println("Connected to " + serverIP + ":" + serverPort);
                movieData = sendAndReceive();
                System.out.println("Received back from replica: " + movieData);
            } catch (Exception e) {
                System.err.println("Error connecting to replica: " + e);
            }
            
            String title;
            String url;
            String desc;
            String id;
            JSONObject currentMovie = new JSONObject();
            
            try{
                currentMovie = new JSONObject(movieData);
            } catch (JSONException e) {
                System.err.println("Could not parse movie object from replica: "+ e);
            }
            
            try{
                //Check title, url and desc can be extracted.
                title = currentMovie.getString("Title");
                url = currentMovie.getString("Url");
                desc = currentMovie.getString("Desc");
            } catch (JSONException e) {
                movieData = "Error";
                System.err.println("Error, movie data ("+movieData+") is not in suitable JSON format: "+ e);
            }
            
            try {
                outToClient.writeBytes(movieData + "\n");
                System.out.println("Sent " + movieData + " back");
            } catch (IOException e) {
                System.out.println("Error sending data to client: " + e);
            }
        }
    }
    
    public void readServer(String path){
        String line;
        try{
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String[] parts = new String[2];
            while((line = reader.readLine()) != null){
                parts = line.split(":");
            }
            serverIP = parts[0]; // IP
            serverPort = Integer.parseInt(parts[1]); // Port
        }
        catch(Exception e){
            System.err.println("Error parsing replica location: "+e);
        }
    }
    
    public String sendAndReceive(){
        String output = "";
        try {
            outToReplica = new DataOutputStream(replicaSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error creating output stream: "+e);
        }
        try {
            inFromReplica = new BufferedReader(new InputStreamReader(replicaSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error creating input stream: "+e);
        }
        try {
            outToReplica.writeBytes(request+ '\n');
            System.out.println("Sent '"+request+"' to replica.");
            
        } catch (Exception e) {
            System.err.println("Error sending request to replica: "+e);
        }
        try {
            output = inFromReplica.readLine();
        } catch (IOException e) {
            System.err.println("Error reading output from replica: "+e);
        }
        try {
            inFromReplica.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: "+e);
        }
        return (output);
    }
}