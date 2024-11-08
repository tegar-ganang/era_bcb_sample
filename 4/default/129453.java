import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import com.softserveinc.edu.training.webserver.requests.WrongFormatException;

public class Temp {

    /**
	 * @param args
	 * @throws IOException
	 * @throws WrongFormatException
	 * @throws URISyntaxException 
	 */
    public static void main(String[] args) throws IOException, WrongFormatException, URISyntaxException {
        System.out.println(new URI("google.com.ua.css").toString());
        Scanner scc = new Scanner(System.in);
        System.err.print(scc.nextLine().substring(1));
        ServerSocket s = new ServerSocket(5354);
        while (true) {
            Socket client = s.accept();
            InputStream iStream = client.getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(iStream));
            String line = "";
            while (!(line = bf.readLine()).equals("")) {
                System.out.println(line);
            }
            System.out.println("end of request");
            new PrintWriter(client.getOutputStream()).println("hi");
            bf.close();
        }
    }
}
