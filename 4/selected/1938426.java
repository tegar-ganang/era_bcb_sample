package rabbit.webserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import java.text.*;
import rabbit.cache.*;
import rabbit.http.*;
import rabbit.io.*;
import rabbit.util.*;

/** This is a very simple webserver used for some simple testing of RabbIT2
 */
public class SimpleTestServer extends Thread {

    private ServerSocket ss;

    private static String basedir = null;

    /** Start a proxy. 
     *  Parse flags and read the config, then starts the proxy.
     * @param args the command-line flags given.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("basedir has to be specified");
            System.exit(-1);
        }
        basedir = args[0];
        new SimpleTestServer();
    }

    private SimpleTestServer() {
        super("RabbIT2 simple webserver");
        try {
            ss = new ServerSocket(8080);
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                Socket s = ss.accept();
                HTTPInputStream in = new HTTPInputStream(new BufferedInputStream(s.getInputStream()), null);
                OutputStream os = s.getOutputStream();
                HTTPHeader header = in.readHTTPHeader();
                String requesturi = header.getRequestURI();
                System.err.println(header.toString());
                FileInputStream fis = new FileInputStream(basedir + File.separator + requesturi);
                byte[] buf = new byte[1024];
                int read;
                while ((read = fis.read(buf)) > 0) os.write(buf, 0, read);
                in.close();
                os.close();
                s.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
