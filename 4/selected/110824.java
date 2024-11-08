package oxygen.tool.automationagent;

import java.io.*;
import java.net.*;
import oxygen.util.*;

/**
 * @author Ugorji
 */
public class AAFileWriter {

    /**
   * Code to connect to a server running at host and port, and 
   * write the file f into it
   */
    public static void writeFileToSocket(String host, int port, File f) throws Exception {
        FileInputStream fis = null;
        OutputStream os = null;
        Socket sock = null;
        try {
            sock = new Socket(host, port);
            fis = new FileInputStream(f);
            os = sock.getOutputStream();
            byte[] b = new byte[1024];
            int bytesread = -1;
            while ((bytesread = fis.read(b, 0, b.length)) != -1) {
                os.write(b, 0, bytesread);
            }
        } finally {
            CloseUtils.close(os);
            CloseUtils.close(fis);
            CloseUtils.close(sock);
        }
    }

    /**
   * Code to wait for a client to connect to the server socket, and 
   * then read the contents from the socket into the file passed.
   */
    public static void receiveFileFromSocket(ServerSocket ss, File f) throws Exception {
        Socket sock = null;
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            ss.setSoTimeout(5000);
            sock = ss.accept();
            if (sock == null) {
                throw new Exception("No socket connection received");
            }
            File dir = f.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            fos = new FileOutputStream(f);
            is = sock.getInputStream();
            byte[] b = new byte[1024];
            int bytesread = -1;
            while ((bytesread = is.read(b, 0, b.length)) != -1) {
                fos.write(b, 0, bytesread);
            }
        } finally {
            CloseUtils.close(fos);
            CloseUtils.close(is);
            CloseUtils.close(sock);
            CloseUtils.close(ss);
        }
    }

    /**
   * Get a runnable object which can be used in a thread to 
   * receive a file from a socket
   */
    public static Runnable getReceiveFileRunnable(final ServerSocket ss, final File f) {
        Runnable r = new Runnable() {

            public void run() {
                try {
                    AAFileWriter.receiveFileFromSocket(ss, f);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        };
        return r;
    }
}
