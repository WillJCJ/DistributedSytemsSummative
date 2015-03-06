package distributedsystemssummtive;

import java.io.*;
import java.net.*;
import org.json.*;

/**
 *
 * @author spgw33
 */
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
                inFromUser.readLine();
            }
            catch(IOException ex){
                System.err.println("Error reading line: " + ex);
            }
            main(new String[0]);
        }
        while(true){
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
                received = sendAndReceive(sentence);
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
        //System.out.println("Sending '"+sentence+"' to server");
        outToServer.writeBytes(sentence + '\n');
        output = inFromServer.readLine();
        return (parseOutput(output));
    }
    
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
            //System.err.println("Error parsing json object returned by server: " + e);
        }
        return ("Title: "+title+"\nUrl: "+url+"\nDesc: "+desc+"\n");
    }
}
