package DaDTC;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;

/**
 *
 * @author Dennis
 */
public class ClientHub {

    Socket socket = null;

    BufferedReader bRead = null;

    BufferedWriter bWrite = null;

    String user = null;

    /** Creates a new instance of Client */
    public ClientHub(Socket socket) {
        this.socket = socket;
        try {
            bRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bWrite = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BufferedReader getChannelRead() {
        return bRead;
    }

    public BufferedWriter getChannelWrite() {
        return bWrite;
    }

    public void setUsername(String user) {
        this.user = user;
    }

    public String getUsername() {
        return user;
    }
}
