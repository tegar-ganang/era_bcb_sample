package enigma.easyserver;

import java.io.IOException;
import java.util.Vector;
import javax.microedition.io.SocketConnection;

public class BroadcastSocket extends SocketThread {

    public BroadcastSocket(SocketConnection socket, ServerThread server) {
        super(socket, server);
    }

    protected void onRecv(byte[] b, int off, int len) {
        Vector setSocket = getServerThread().getSocketSet();
        synchronized (setSocket) {
            for (int i = 0; i < setSocket.size(); i++) {
                try {
                    ((SocketThread) setSocket.elementAt(i)).getOutputStream().write(b, off, len);
                } catch (IOException e) {
                }
            }
        }
    }
}
