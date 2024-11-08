package webirc.client.synchronization;

import com.google.gwt.sockets.client.*;
import com.google.gwt.sockets.client.impl.JavaBinarySocketImpl;
import com.google.gwt.user.client.Window;
import webirc.client.MainSystem;
import java.util.List;

/**
 * @author Ayzen
 */
public class SocketSynchronizer extends Synchronizer {

    private static final String PING = "PING";

    private static final String PONG = "PONG";

    private BinarySocket socket;

    public SocketSynchronizer(SynchronizeListener listener, String policyFile) {
        super(listener);
        if (policyFile != null && policyFile.trim().length() > 0) socket = SocketsFactory.createBinarySocket(new SocketEventsHandler(), policyFile); else socket = new JavaBinarySocketImpl(new SocketEventsHandler());
        if (socket == null) throw new NullPointerException();
    }

    public void connect(String host, int port) {
        try {
            socket.connect(host, port);
        } catch (SocketException e) {
            MainSystem.showError(e.getMessage());
        }
    }

    public void synchronize(String message) {
        try {
            socket.writeStringLine(message, "cp1251");
            socket.flush();
        } catch (IOException e) {
            MainSystem.showError(e.getMessage());
        }
    }

    public void stop() {
        try {
            if (socket.isConnected()) socket.disconnect();
        } catch (IOException e) {
            MainSystem.showError("Exception occured when closing socket connection:\n" + e.getMessage());
        }
    }

    private class SocketEventsHandler implements SocketListener {

        public void onReady() {
            fireReady();
        }

        public void onConnect() {
            fireConnected();
        }

        public void onCloseConnection() {
            MainSystem.showFatalError("Connection is closed.");
        }

        public void onData(int loadedBytes) {
            try {
                String readLine = socket.readStringLine("cp1251");
                while (readLine != null) {
                    if (readLine.startsWith(PING)) {
                        socket.writeStringLine(PONG + " :" + readLine.substring(readLine.indexOf(":") + 1), "cp1251");
                        socket.flush();
                    } else {
                        List list = parser.parseMessages(readLine);
                        notifyCommandListeners(list.iterator());
                    }
                    readLine = socket.readStringLine("cp1251");
                }
            } catch (IOException e) {
                Window.alert("Error");
            }
        }

        public void onSecurityError(String message) {
            MainSystem.showFatalError("Security error:\n" + message);
        }

        public void onIOError(String message) {
            if (!socket.isConnected()) fireNotConnected(); else MainSystem.showFatalError("Input/output error:\n" + message);
        }
    }
}
