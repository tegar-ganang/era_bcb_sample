import java.net.*;
import java.io.*;
import javax.servlet.*;

/**
 * This class is made to test GET, PUT and DELETE requests to the
 * wiki server.
 */
public class Request {

    static String servUrl = "http://localhost:8080/Wiki/Server";

    public static void main(String[] args) {
        testGet();
        testPut("exampleDocument");
        testGet();
    }

    /**
     * Test the empty GET request
     */
    public static void testGet() {
        try {
            String req = servUrl;
            System.out.println("\nGET " + req);
            HttpURLConnection con = (HttpURLConnection) (new URL(req)).openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) System.out.println(inputLine);
            in.close();
            System.out.println("ResponseCode: " + con.getResponseCode() + "\n");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Test the GET request with parameter
     */
    public static void testGetWithParameter(String word) {
        try {
            String req = servUrl + "?word=" + word;
            System.out.println("\nGET " + req);
            HttpURLConnection con = (HttpURLConnection) (new URL(req)).openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) System.out.println(inputLine);
            in.close();
            System.out.println("ResponseCode: " + con.getResponseCode() + "\n");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Test the PUT request
     */
    public static void testPut(String word) {
        try {
            String req = servUrl + "?word=" + word;
            System.out.println("\nPUT " + req);
            HttpURLConnection con = (HttpURLConnection) (new URL(req)).openConnection();
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.setRequestProperty("ContentType", "text/xml");
            OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
            File file = new File(word + ".xml");
            FileReader reader = new FileReader(file);
            int character;
            while ((character = reader.read()) != -1) out.write(character);
            out.close();
            reader.close();
            int responseCode = con.getResponseCode();
            InputStream err = con.getErrorStream();
            int c;
            if (err != null) while ((c = err.read()) != -1) {
                System.out.print(new Character((char) c));
            }
            System.out.println("ResponseCode: " + responseCode + "\n");
            con.disconnect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Test the DELETE request
     */
    public static void testDelete(String word) {
        try {
            String req = servUrl + "?word=" + word;
            System.out.println("\nDELETE " + req);
            HttpURLConnection con = (HttpURLConnection) (new URL(req)).openConnection();
            con.setRequestMethod("DELETE");
            int responseCode = con.getResponseCode();
            System.out.println("ResponseCode: " + responseCode + "\n");
            con.disconnect();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
