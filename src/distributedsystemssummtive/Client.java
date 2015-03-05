package distributedsystemssummtive;

import java.io.*;
import java.net.*;

/**
 *
 * @author spgw33
 */
public class Client {
    private static Socket clientSocket;
    
    public static void main(String[] args) throws Exception{
        String sentence;
        String received;
        BufferedReader inFromUser;
        connect("localhost",18300); //Change this line depending on where you're connecting
        inFromUser = new BufferedReader(new InputStreamReader(System.in));
        while(true){
            sentence = inFromUser.readLine();
            received = sendAndReceive(sentence);
            if(sentence.toLowerCase().equals("close") || sentence.toLowerCase().equals("end") || sentence.toLowerCase().equals("exit")){
                System.out.println("Closing connection with server.");
                break;
            }
            received = received.replace("NEWLINE", "\n");
            System.out.print("All results containing your search, '"+sentence+"':\n"+received);
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
        System.out.println("Sending '"+sentence+"' to server");
        outToServer.writeBytes(sentence + '\n');
        output = inFromServer.readLine();
        return (output);
    }
}
