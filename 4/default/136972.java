import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author  arun
 */
public class FileServer {

    ServerSocket s;

    /** Creates a new instance of FileServer */
    public FileServer(int port) {
        s = null;
        try {
            s = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void acceptConnection() {
        try {
            while (true) {
                System.out.println("waiting for client");
                Socket m = s.accept();
                System.out.println("connected to client");
                Service t = new Service(m);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        FileServer server = new FileServer(2711);
        server.acceptConnection();
    }
}

class Service extends Thread {

    private Socket s;

    public Service(Socket t) {
        s = t;
    }

    private InputStream in;

    private OutputStream out;

    public void getStreams() {
        in = null;
        out = null;
        try {
            in = s.getInputStream();
            out = s.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(byte b[]) {
        try {
            out.write(b);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String file) {
        try {
            File f = new File(file);
            if (!f.exists()) send("not found".getBytes()); else {
                byte b[] = new byte[(int) f.length()];
                new FileInputStream(f).read(b);
                System.out.println("length=" + b.length);
                send(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readRequest() {
        try {
            ByteArrayOutputStream m = new ByteArrayOutputStream();
            byte b;
            while ((b = (byte) in.read()) != -1) m.write(b);
            return m.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void run() {
        try {
            getStreams();
            while (true) {
                String req = readRequest();
                StringTokenizer st = new StringTokenizer(req);
                System.out.println(req);
                String cmd = st.nextToken();
                if (cmd.equalsIgnoreCase("send")) {
                    String filename = st.nextToken();
                    sendFile(filename);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
