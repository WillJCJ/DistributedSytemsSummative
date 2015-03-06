package distributedsystemssummtive;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import org.json.*;

public class ReplicaThread implements Runnable{
    private ArrayList<String> serverList;
    private Boolean isPrimary;
    private String filmFile;
    private String filmFilePath;
    private String fileVersion;
    private String request;
    private BufferedReader inFromFront;
    private DataOutputStream backToFront;
    private Socket connectionSocket;
    private JSONObject movieObj;
    private String primaryFrontEnd = "primary"; // for print statements, is either primary or front end, depending on isPrimary Boolean
    private int sleepAmount = 1000; //Time between checking for new request.
    
    public ReplicaThread(String inPath, Socket socket, Boolean primary){
        isPrimary = primary;
        if(isPrimary){
            primaryFrontEnd = "front end";
        }
        filmFilePath = inPath;
        System.out.println("New thread running.");
        filmFile = readFile(filmFilePath);
        serverList = parseServers("servers.txt");
        try{
            movieObj = new JSONObject(filmFile);
        } catch (JSONException e) {
            System.err.println("Could not parse JSON object from file string: "+ e);
        }
        connectionSocket = socket;
        try{
            inFromFront = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            backToFront = new DataOutputStream(connectionSocket.getOutputStream());
        }
        catch(Exception e){
            System.err.println("Error creating input/output streams: "+e);
        }
        System.out.println("Film file found");
    }
    
    public void run(){
        while(true) {
            try{
                request = inFromFront.readLine();
                if (request == null){
                    Thread.sleep(sleepAmount); //Waits for a request, checking every 1 second
                }
                else{
                    System.out.println("Received '"+request+"' from "+primaryFrontEnd+" server.");
                    if(request.toLowerCase().equals("close") || request.toLowerCase().equals("end") || request.toLowerCase().equals("exit")){
                        connectionSocket.close();
                        break;
                    }
                    else{
                        System.out.println("Finding a matching film.");
                        String output = findFilm();
                        System.out.println("Sending back: "+output);
                        backToFront.writeBytes(output+"\n");
                        System.out.println("Waiting for request from "+primaryFrontEnd+" server.");
                    }
                }
            }
            catch(IOException e){
                System.err.println("Error sending data back to "+primaryFrontEnd+" server: "+e);
                System.err.println("Terminating thread.");
                break;
            }
            catch(InterruptedException e){
                System.err.println("Thread's sleep interupted: "+e);
                System.err.println("Terminating thread.");
                break;
            }
        }
    }
    
    public void togglePrimary(){
        isPrimary = !isPrimary;
    }
    
    public String findFilm(){
        request = request.toLowerCase();
        String info = "";
        String title;
        String url;
        String desc;
        JSONArray movieArr = new JSONArray();
        try {
            movieArr = movieObj.getJSONArray("Movies");
        } catch (JSONException e) {
            System.err.println("Could not retrieve Movie array from file object: "+ e);
        }
        JSONObject currentMovie = new JSONObject();
        for (int i = 0; i < movieArr.length(); i++)
        {
            try{
                currentMovie = movieArr.getJSONObject(i);
            } catch (JSONException e) {
                System.err.println("Could not retrieve Movie object from movie array: "+ e);
            }
            try{
                title = currentMovie.getString("Title");
                if(title.toLowerCase().contains(request)){
                    url = currentMovie.getString("Url");
                    desc = currentMovie.getString("Desc");
                    info = rebuildJSON(title, url, desc);
                    break;
                }
            } catch (JSONException e) {
                System.err.println("Could not read movie data ("+i+")from movie array: "+ e);
            }
        }
        if(info.equals("") && isPrimary){
            String sIP;
            int sPort;
            for (String server : serverList) {
                String[] parts = server.split(":");
                sIP = parts[0];
                sPort = Integer.parseInt(parts[1]);
                info = connect(sIP, sPort);
                if(!(info.equals(""))){
                    writeJSONToFile(info); //Slave server has a movie primary doesn't so primary will write it to their file.
                    break;
                }
            }
            if(info.equals("")){
                info = (searchWeb(request));
            }
        }
        return info;
    }
    
    public ArrayList<String> parseServers(String path){
        ArrayList<String> lines = new ArrayList<String>();
        String line;
        try{
            BufferedReader reader = new BufferedReader(new FileReader(path));
            while((line = reader.readLine()) != null){
                lines.add(line);
            }
        }
        catch(Exception e){
            System.err.println("Error reading file: "+e);
        }
        return lines;
    }
    
    public String rebuildJSON(String title, String url, String desc){
        String str = ("{\"Title\":\""+title.replace("\"", "\\\"")+"\",\"Url\":\""+url.replace("\"", "\\\"")+"\",\"Desc\":\""+desc.replace("\"", "\\\"")+"\"}");
        return str;
    }
    
    public String readFile(String path){
        String lines;
        lines = "";
        String line;
        try{
            BufferedReader reader = new BufferedReader(new FileReader(path));
            while((line = reader.readLine()) != null){
                lines = lines + line;
            }
        }
        catch(Exception e){
            System.err.println("Error reading file: "+e);
        }
        return lines;
    }
    
    // Only called by primary.  Must communicate with slave server.
    public String connect(String serverIP, int serverPort){
        DataOutputStream outToSlave;
        BufferedReader inFromSlave;
        String output = "";
        try {
            Socket slaveSocket = new Socket(serverIP, serverPort);
            outToSlave = new DataOutputStream(slaveSocket.getOutputStream());
            inFromSlave = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()));
            outToSlave.writeBytes(request+ '\n');
            System.out.println("Sent '"+request+"' to slave replica.");
            output = inFromSlave.readLine();
            inFromSlave.close();
        } catch (Exception e) {
            System.err.println("Error communicating with slave replica: "+e);
        }
        return (output);
    }
    
    //http://rest.elkstein.org/2008/02/using-rest-in-java.html
    public static String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn =
        (HttpURLConnection) url.openConnection();

        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }

        // Buffer the result into a string
        BufferedReader rd = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();

        conn.disconnect();
        return sb.toString();
    }
    
    public String searchWeb(String req){
        String title;
        String id;
        String desc;
        String url;
        String urlRequest = req.replace(" ", "+");
        String searchURL = "http://www.omdbapi.com/?t=" + urlRequest + "&y=&plot=short&r=json";
        String responseJSON = rebuildJSON("No movies found", "", "");
        try{
            String omdbResponse = httpGet(searchURL);
            JSONObject parsedResponse = new JSONObject(omdbResponse);
            String responseString = parsedResponse.getString("Response");
            if(responseString.equals("True")){
                title = parsedResponse.getString("Title");
                id = parsedResponse.getString("imdbID");
                desc = parsedResponse.getString("Plot");
                url = "http://www.imdb.com/title/"+id;
                responseJSON = rebuildJSON(title, url, desc);
                writeJSONToFile(responseJSON);
            }
            else{
                throw new JSONException("Parse error, no valid response");
            }
        }catch(IOException e){
            System.err.println("Error searching OMDb for \"" + req + "\": " + e);
        }catch(JSONException e){
            System.err.println("Error parsing response from OMDb server: " + e);
        }
        return responseJSON;
    }
    
    public void writeJSONToFile(String jsonObject){
        PrintWriter writer = null;
        filmFile = readFile(filmFilePath); //Have to re-read file in case it has changed since it was last opened.
        if(filmFile.substring((filmFile.length()-3)).equals(",]}")){
            try {
                String noEnd = filmFile.substring(0,(filmFile.length()-2));
                noEnd += jsonObject + ",]}";
                filmFile = noEnd;
                writer = new PrintWriter(filmFilePath, "UTF-8");
                writer.print(filmFile);
                writer.close();
            } catch (FileNotFoundException e) {
                System.err.println("Error, file not found: " + e);
            } catch (UnsupportedEncodingException e) {
                System.err.println("Error, unsupported encoding used: " + e);
            } finally {
                writer.close();
            }
        }
        else{
            System.err.println("Could not append OMDb film to film file because file is not in correct format.");
        }
    }
}