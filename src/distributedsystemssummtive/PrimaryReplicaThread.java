package distributedsystemssummtive;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import org.json.*;

public class PrimaryReplicaThread implements Runnable{
    
    private String filmFile;
    private String fileVersion;
    private String request;
    private BufferedReader inFromFront;
    private DataOutputStream backToFront;
    private Socket connectionSocket;
    private JSONObject movieObj;
    private int sleepAmount = 1000; //Time between checking for new request.
    
    public PrimaryReplicaThread(String inPath, Socket socket){
        System.out.println("New thread running.");
        filmFile = readFile(inPath);
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
                    System.out.println("Received '"+request+"' from front end server.");
                    if(request.toLowerCase().equals("close") || request.toLowerCase().equals("end") || request.toLowerCase().equals("exit")){
                        connectionSocket.close();
                        break;
                    }
                    else{
                        System.out.println("Finding matching films.");
                        String output = findFilm();
                        System.out.println("Sending back: "+output);
                        backToFront.writeBytes(output+"\n");
                        System.out.println("Waiting for request from front end.");
                    }
                }
            }
            catch(Exception e){
                System.err.println("Error sending data back to front end server: "+e);
                System.err.println("Terminating thread.");
                break;
            }
        }
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
        String id;
        if(info.equals("")){
            info = (searchWeb(request));
        }
        System.out.println(info);
        return info;
    }
    
    /**
     *
     * @param title
     * @param url
     * @param desc
     * @return Standard JSON object to add to the ArrayList sent back to front end server
     */
    public String rebuildJSON(String title, String url, String desc){
        String str = ("{\"Title\":\""+title+"\",\"Url\":\""+url+"\",\"Desc\":\""+desc+"\"}");
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
}


