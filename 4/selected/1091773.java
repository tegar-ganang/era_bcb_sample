package soht.client.java.core;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.io.*;
import soht.client.java.configuration.ConfigurationManager;

/**
 * Handles the incoming data from the remote host.
 *
 * @author Eric Daugherty
 */
public class ProxyReader extends BaseProxy {

    /** The output stream to write to the local client */
    private OutputStream out;

    public ProxyReader(String name, ConfigurationManager configurationManager, long connectionId, Socket socket) throws IOException {
        super(name, configurationManager, connectionId, socket);
        this.out = socket.getOutputStream();
        ;
    }

    /**
     * Reads from the remote server until the connection closes.
     */
    public void run() {
        try {
            HttpURLConnection urlConnection = configurationManager.getURLConnection();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
            out.write("action=read");
            out.write("&");
            out.write("id=" + connectionId);
            out.flush();
            out.close();
            urlConnection.connect();
            InputStream in = null;
            try {
                in = urlConnection.getInputStream();
                byte[] bytes = new byte[1024];
                int count = 0;
                boolean isFirst = true;
                int startIndex = 1;
                while (true) {
                    count = in.read(bytes);
                    if (count == -1 || (isFirst && count > 0 && bytes[0] == 0)) {
                        out.close();
                        socket.close();
                        break;
                    }
                    startIndex = isFirst ? 1 : 0;
                    try {
                        this.out.write(bytes, startIndex, count - startIndex);
                    } catch (IOException e) {
                        closeServer();
                    }
                    isFirst = false;
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                urlConnection.disconnect();
            }
        } catch (IOException ioe) {
            if (out != null) {
                try {
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing output stream to client.");
                }
            }
            System.out.println("IOException in ProxyReader.");
            ioe.printStackTrace();
        }
    }
}
