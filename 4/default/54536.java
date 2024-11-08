import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FileListenThread implements Runnable {

    private ServerSocket ss;

    private int port;

    public FileListenThread(int port) {
        this.port = port;
    }

    public void run() {
        try {
            ss = new ServerSocket(port);
            System.out.println("Listening on " + ss + " and ready to write files");
            while (true) {
                Socket s = ss.accept();
                System.out.println("Connection from " + s);
                InputStream in = s.getInputStream();
                new Thread(new FileWorkerThread(in)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
