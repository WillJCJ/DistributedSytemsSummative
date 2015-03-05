package distributedsystemssummtive;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ReplicaThread implements Runnable{
    
    private ArrayList<String> filmFile;
    private String request;
    private BufferedReader inFromMiddle;
    private DataOutputStream backToMiddle;
    private Socket connectionSocket;
    
    public ReplicaThread(String inPath, Socket socket){
        System.out.println("New thread running.");
        filmFile = readFile(inPath);
        connectionSocket = socket;
        try{
            inFromMiddle = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            backToMiddle = new DataOutputStream(connectionSocket.getOutputStream());
        }
        catch(Exception e){
            System.err.println("Error creating input/output streams: "+e);
        }
        System.out.println("Film file found");
    }
    
    public void run(){
        while(true) {
            try{
                System.out.println("waiting for input");
                request = inFromMiddle.readLine();
                System.out.println("Received '"+request+"' from client.");
                if(request.toLowerCase().equals("close") || request.toLowerCase().equals("end") || request.toLowerCase().equals("exit")){
                    connectionSocket.close();
                    break;
                }
                else{
                    System.out.println("Finding matching films.");
                    ArrayList<String> output = findFilm(filmFile);
                    System.out.println("Sending back: "+output.toString());
                    backToMiddle.writeBytes(output.toString()+"\n");
                }
            }
            catch(Exception e){
                System.err.println("Error sending data back to client: "+e);
                break;
            }
        }
    }
    
    public ArrayList<String> findFilm(ArrayList<String> movieFile){
        request = request.toLowerCase();
        ArrayList<String> info = new ArrayList<String>();
        String line;
        String data;
        for (int i = 0; i < movieFile.size(); i++){
            line = movieFile.get(i);
            if(line.length() > 4){
                if (line.substring(0,5).equals("$FILM")){
                    if (line.substring(5).toLowerCase().contains(request)){
                        data = "";
                        data = data + (movieFile.get(i));
                        data = data + (movieFile.get(i+1));
                        data = data + (movieFile.get(i+2)+"$END");
                        info.add(data);
                    }
                }
            }
        }
        return info;
    }
    
    
    public ArrayList<String> readFile(String path){
        ArrayList<String> lines;
        lines = new ArrayList<String>();
        String line;
        try{
            BufferedReader reader = new BufferedReader(new FileReader(path));
            while((line = reader.readLine()) != null){
                lines.add(line);
            }
        }
        catch(Exception e){
            
        }
        return lines;
    }
}


