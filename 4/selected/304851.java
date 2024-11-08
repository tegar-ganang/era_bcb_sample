package org.jmule.core.protocol.donkey;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.util.*;
import java.util.logging.*;
import org.jmule.core.*;

/** FIXME: yet another masterless class: its begs for a javadoc!
 * @author casper
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:44:16 $
 */
public class DonkeyConnectionListener implements ConnectionListener {

    static final Logger log = Logger.getLogger(DonkeyConnectionListener.class.getName());

    public DonkeyConnectionListener() {
        this.dContext = DonkeyProtocol.getInstance();
    }

    /** @see org.jmule.core.ConnectionListener#init() */
    public void init() throws Exception {
        try {
            newConnections = new LinkedList();
            selector = SelectorProvider.provider().openSelector();
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(dContext.getTCPPort()));
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            dContext.usedTCPPort = ssc.socket().getLocalPort();
            log.info("Now listening on " + ssc.socket().getLocalSocketAddress() + " for incoming edonkey connections.");
        } catch (UnknownHostException e) {
            log.warning(e.getMessage());
        }
    }

    /** @see org.jmule.core.ConnectionListener#hasNewConnection() */
    public boolean hasNewConnection() {
        try {
            int newAccepts = 0;
            if ((newAccepts = selector.selectNow()) > 0) {
                Set newKeys = selector.selectedKeys();
                Iterator i = newKeys.iterator();
                while (i.hasNext()) {
                    SelectionKey inKey = (SelectionKey) i.next();
                    if (inKey.isAcceptable()) {
                        i.remove();
                        ServerSocketChannel nextReady = (ServerSocketChannel) inKey.channel();
                        SocketChannel sc = nextReady.accept();
                        sc.configureBlocking(false);
                        log.fine("Incoming donkey connection from " + sc.socket().getInetAddress().toString());
                        newConnections.add(new DonkeyClientConnection(sc));
                    }
                }
            }
        } catch (IOException e) {
            log.warning("IO Exeption: " + e.getMessage());
        }
        return (newConnections.size() > 0);
    }

    /** @see org.jmule.core.ConnectionListener#getNextNewConnection() */
    public Connection getNextNewConnection() {
        Connection con = (Connection) newConnections.removeFirst();
        try {
            con.getChannel().socket().setReceiveBufferSize(65536);
        } catch (SocketException e) {
            log.log(Level.WARNING, "receive buffer size failed", e);
        }
        return con;
    }

    /** @see org.jmule.core.ConnectionListener#end() */
    public void end() {
    }

    protected Selector selector;

    protected ServerSocketChannel ssc;

    protected DonkeyProtocol dContext;

    protected LinkedList newConnections;
}
