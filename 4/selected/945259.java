package pnc.client;

import java.io.*;
import java.net.*;
import java.util.*;

/** 
 * This class allows the worker to become a socket's server
 * using the port 2000 (alterable)
 *
 * @author pncTeam.
 * @version $Revision: 1.2 $
 */
public class WorkerServer extends Thread {

    /** The buffer used to transfer incoming and outgoing datas through the socket */
    protected byte[] buffer = new byte[BUFFER_SIZE];

    /** Constant of the buffer's size used by the socket */
    protected static final int BUFFER_SIZE = 100000;

    /** The socket's server itself */
    private ServerSocket serverSocket;

    /** This constant defines the port used by the server */
    public static final int SERVER_PORT = 2000;

    /**
   * Constructor.
   * Starts a socket's server on port 2000
   */
    public WorkerServer() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** This method starts listening of incoming connections on server's port */
    public void run() {
        if (serverSocket == null) return;
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                File fileWorker = new File("/tmp/worker.xml");
                BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(fileWorker));
                int read = 0;
                while (read != -1) {
                    read = fileStream.read(buffer, 0, BUFFER_SIZE);
                    if (read != -1) {
                        output.write(buffer, 0, read);
                        output.flush();
                    }
                }
                output.close();
                fileStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
