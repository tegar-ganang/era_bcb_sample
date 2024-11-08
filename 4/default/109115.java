import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author  arun
 */
public class FileClient {

    private Socket s;

    private InputStream in;

    private OutputStream out;

    /** Creates a new instance of FileClient */
    public FileClient() {
    }

    public void connect(String server, int port) {
        s = null;
        in = null;
        out = null;
        try {
            System.out.println("Trying to connect");
            s = new Socket(server, port);
            System.out.println("Connect to server");
            in = s.getInputStream();
            out = s.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(byte b[]) {
        try {
            out.write(b);
            out.write((byte) -1);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] receive() {
        try {
            ByteArrayOutputStream m = new ByteArrayOutputStream();
            byte b;
            while ((b = (byte) in.read()) != -1) m.write(b);
            return m.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void download(String filename, String path) {
        try {
            send(("send " + filename).getBytes());
            byte b[] = receive();
            if (new String(b).equalsIgnoreCase(" Not Found")) System.out.println(filename + "not found"); else {
                System.out.println(filename + "not found");
                System.out.println("Enter path to save");
                String paths = (new DataInputStream(System.in)).readLine();
                FileOutputStream f = new FileOutputStream(path);
                f.write(b);
                System.out.println("File Downloaded");
                f.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        FileClient client = new FileClient();
        client.connect("127.0.0.1", 2711);
        client.download("c:/img.bmp", "g:/a.bmp");
    }
}
