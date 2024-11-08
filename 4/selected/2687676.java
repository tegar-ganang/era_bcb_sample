package bman.tools.net;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * This is a component used by of the ByteStation class. 
 * Users of the ByteStation should not have to bother looking at this
 * class.
 * <p>
 * This class listens on on the SocketChannel for any incoming messages.
 * Incoming messages are presumed to be bytes sent by a SocketSession that
 * is connected to the SocketChannel held by this ByteListener.
 * <p>
 * The protocol that is followed here is:
 * <ol>
 * <li> The first int that is received is a notification that a transmission is incomming. </li>
 * <li> The ByteReceiver will then get notified and the SocketSession will be passed to it. </li>
 * <li> THIS IS THE DANGEROUSE BIT: The ByteReceiver shall be responsible for how many bytes is to be read from the SocektChannel </li>
 * </ol>
 * @author MrJacky
 *
 */
public class ByteListener extends Thread {

    Logger log = Logger.getLogger(SocketListener.class.getName());

    SocketSession session;

    ByteReceiver m_Receiver;

    boolean run = true;

    int errorCount = 0;

    public ByteListener(SocketSession session, ByteReceiver receiver) {
        this.session = session;
        m_Receiver = receiver;
        start();
    }

    public void run() {
        while (run) {
            try {
                DataInputStream dis = new DataInputStream(session.getChannel().socket().getInputStream());
                int read = dis.readInt();
                log.info("Read magic number: " + read + ". Notification received. Passing notification to receiver...");
                m_Receiver.receive(session);
                errorCount = 0;
            } catch (Exception e) {
                errorCount++;
                log.warning("Error reading from socket. Error message is: " + e.getMessage());
                e.printStackTrace();
                if (errorCount > 3) {
                    run = false;
                    log.severe("There are too many errors in this connection. Stopping SocketListener");
                }
            }
        }
    }
}
