package distributedsystemssummtive;

import java.io.*;
import java.net.*;
import org.json.*;

public class Client {
    private static Socket clientSocket;
    
    public static void main(String[] args){
        String sentence;
        String received;
        BufferedReader inFromUser;
        inFromUser = new BufferedReader(new InputStreamReader(System.in));
        try {
            connect("localhost", 18300); //Change this line depending on where you're connecting
        } catch (Exception e) {
            System.out.println("Error connecting to front end server.");
            System.out.println("Press enter to try and connect again.");
            try{
                inFromUser.readLine(); // Wait for user to press enter before trying to reconnect
            }
            catch(IOException ex){
                System.err.println("Error reading line: " + ex);
            }
            main(new String[0]); // Call main method again to attempt to reconnect
        }
        while(true){ // once connection is established, the while true loop means the client can keep sending requests
            sentence = "";
            received = "No results";
            System.out.println("Please enter your search: ");
            try{
                sentence = inFromUser.readLine();
                }
            catch(IOException ex){
                System.err.println("Error reading line: " + ex);
            }
            try{
                received = sendAndReceive(sentence); // send off 'sentence' to the front end server and get back 'received'
            }
            catch(Exception e){
                System.err.println("Error sending sentence to server, no message received: " + e);
            }
            System.out.println("Your search, '"+sentence+"', returned:\n"+received);
        }
    }
    
    public static void connect(String ip, int port) throws Exception{
        System.out.println("Connecting to "+ip+":"+port);
        clientSocket = new Socket (ip,port);
    }
    
    public static String sendAndReceive(String sentence) throws Exception{
        String output;
        DataOutputStream outToServer =
            new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = 
            new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes(sentence + '\n');
        output = inFromServer.readLine();
        return (parseOutput(output));
    }
    
    // Front end server returns a JSON object, so it must be parsed to a form the client can read easily
    public static String parseOutput(String FEOutput){
        String result;
        String title = "No movies found";
        String url = "";
        String desc = "";
        try{
            JSONObject jsonOutput = new JSONObject(FEOutput);
            title = jsonOutput.getString("Title");
            url = jsonOutput.getString("Url");
            desc = jsonOutput.getString("Desc");
        }catch(JSONException e){
            //System.err.println("Error parsing json object returned by server: " + e); //Commented out so the client doesn't see the error, but left in for debugging.
        }
        return ("Title: "+title+"\nUrl: "+url+"\nDesc: "+desc+"\n");
    }
}
