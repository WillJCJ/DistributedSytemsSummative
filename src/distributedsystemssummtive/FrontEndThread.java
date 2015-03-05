package distributedsystemssummtive;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FrontEndThread implements Runnable{
    private ArrayList<String> filmData;
    private ArrayList<String> results;
    private String request;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private ArrayList<String> serverList;
    private Socket clientSocket;
    private Socket serverSocket;
    
    public FrontEndThread(Socket socket){
        clientSocket = socket;
        serverList = new ArrayList<String>();
        filmData = new ArrayList<String> ();
        results = new ArrayList<String> ();
        serverList = readServers("/Users/Will/Documents/CS/NetworksSummative/servers.txt");
        try{
            inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToClient = new DataOutputStream(clientSocket.getOutputStream());
        }
        catch(Exception e){
            System.err.println("Error creating input/output streams: "+e);
        }
    }
    
    public void run(){
        int noServers = serverList.size()/2;
        String ip;
        int port;
        String output;
        try{
            request = inFromClient.readLine();
            System.out.println("Received '"+request+"' from client.");
        }
        catch(Exception e){
            System.err.println("Error receiving from client: "+e);
        }
        for (int i = 0; i < noServers; i++){
            ip = serverList.get(2*i);
            port = Integer.parseInt(serverList.get(2*i+1));
            try{
                serverSocket = new Socket (ip,port);
                System.out.println("Connected to "+ip+":"+port);
                output = sendAndReceive();
                System.out.println("Received back from server "+(i+1)+": "+output);
                filmData.add(output);
            }
            catch(Exception e){
                System.err.println("Error connecting to server: "+e);
            }
        }
        int titleIndex;
        int urlIndex;
        int descIndex;
        int endIndex;
        String title;
        String url;
        String desc;
        String result;
        for (String data : filmData){
            while (data.length() > 4){
                titleIndex = data.indexOf("$FILM");
                urlIndex = data.indexOf("$URL");
                descIndex = data.indexOf("$DESC");
                endIndex = data.indexOf("$END");
                title = data.substring(titleIndex+5,urlIndex);
                url = data.substring(urlIndex+4,descIndex);
                desc = data.substring(descIndex+5,endIndex);
                data = data.substring(endIndex+5); //Try again to see if there is more than one movie in the data
                result = ("Title: "+title+"NEWLINEWebsite: "+url+"NEWLINEDescription: "+desc);
                results.add(result);
            }
        }
        
        String resultString = "";
        int resultSize = results.size();
        if(resultSize==0){
            resultString = "No results found";
        }
        for (int r = 0; r < resultSize; r++){
            for (int s = (r+1); s<resultSize; s++){
                if(results.get(s).equals(results.get(r))){
                    results.remove(s);
                    resultSize = resultSize-1;
                }
            }
            resultString = resultString + (r+1) + ".NEWLINE" + results.get(r) + "NEWLINENEWLINE";
        }
        try {
            outToClient.writeBytes(resultString+"\n");
            System.out.println("Sent "+resultString+" back");
        } catch (IOException e) {
            System.out.println("Error sending data to client: "+e);
        }
    }
    
    public ArrayList<String> readServers(String path){
        String line;
        try{
            BufferedReader reader = new BufferedReader(new FileReader(path));
            while((line = reader.readLine()) != null){
                serverList.add(line);
            }
        }
        catch(Exception e){
            System.err.println("Error parsing servers.");
            System.err.println("Error: "+e);
        }
        return serverList;
    }
    
    public String sendAndReceive(){
        String output = "";
        try {
            outToServer = new DataOutputStream(serverSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error creating output stream: "+e);
        }
        try {
            inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error creating input stream: "+e);
        }
        try {
            outToServer.writeBytes(request+ '\n');
            System.out.println("Sent '"+request+"' to server.");
            
        } catch (Exception e) {
            System.err.println("Error sending request to server: "+e);
        }
        try {
            output = inFromServer.readLine();
        } catch (IOException e) {
            System.err.println("Error reading output from server: "+e);
        }
        try {
            inFromServer.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: "+e);
        }
        return (output);
    }
}
