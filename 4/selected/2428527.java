package sk.naive.talker.tcpadapter;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.rmi.*;
import java.util.*;
import sk.naive.talker.util.Utils;
import sk.naive.talker.callback.CallbackException;
import sk.naive.talker.adapter.*;
import sk.naive.talker.Main;

/**
 * TCP connection adapter (for telnet clients).
 *
 * @author <a href="mailto:virgo@naive.deepblue.sk">Richard "Virgo" Richter</a>
 * @version $Revision: 1.63 $ $Date: 2005/01/25 21:57:05 $
 */
public class TCPAdapter extends AbstractAdapter implements Runnable {

    private int port;

    private Map socketUser = new HashMap();

    private Selector selector;

    private ServerSocketChannel serverSocketChannel;

    /**
	 * Constructor initialize name of the adapter and port according
	 * to preferencies.
	 */
    public TCPAdapter(String name) throws RemoteException, NotBoundException {
        this.name = name;
        port = Main.getConfiguration().getInt(name + ".port", 4444);
        init("sk/naive/talker/tcpadapter/PropertyResources", "sk/naive/talker/tcpadapter/UserPropertyResources");
    }

    public void run() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.config("Listening on port " + port);
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            while (true) {
                try {
                    if (selector.select() > 0) {
                        Iterator it = selector.selectedKeys().iterator();
                        while (it.hasNext()) {
                            SelectionKey rsk = (SelectionKey) it.next();
                            it.remove();
                            int rskOps = rsk.readyOps();
                            if ((rskOps & SelectionKey.OP_ACCEPT) > 0) {
                                Socket socket = ((ServerSocketChannel) rsk.channel()).accept().socket();
                                logger.fine("Accepted connection: host='" + socket.getInetAddress().getHostName() + "', port=" + socket.getPort() + ", IP=" + socket.getInetAddress().getHostAddress());
                                talker.processSystemMessage("accept: " + socket.getInetAddress().getHostName());
                                TCPUser u = new TCPUser(socket, this, talker);
                                socketUser.put(socket, u);
                                SocketChannel sc = socket.getChannel();
                                sc.configureBlocking(false);
                                sc.register(selector, SelectionKey.OP_READ);
                                u.handshake();
                            } else if ((rskOps & SelectionKey.OP_READ) > 0) {
                                read(rsk, buffer);
                            }
                        }
                    }
                } catch (IOException e) {
                    if (e.getMessage().equals("Interrupted system call")) {
                        logger.fine("Interrupted select(). OK...");
                    } else {
                        Utils.unexpectedExceptionWarning(e);
                    }
                } catch (CallbackException e) {
                    Utils.unexpectedExceptionWarning(e);
                } catch (Exception e) {
                    Utils.unexpectedExceptionWarning(e);
                }
                if (shutdown) {
                    try {
                        logger.severe("TCPAdapter shutdown... OK...");
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            selector.close();
        } catch (IOException e) {
            Utils.unexpectedExceptionWarning(e);
        } catch (Throwable t) {
            Utils.unexpectedExceptionWarning(t);
            try {
                shutdown();
            } catch (IOException e) {
                Utils.unexpectedExceptionWarning(e);
            }
        }
    }

    private void read(SelectionKey rsk, ByteBuffer buffer) throws CallbackException, RemoteException {
        SocketChannel ch = (SocketChannel) rsk.channel();
        Socket socket = ch.socket();
        TCPUser u = (TCPUser) socketUser.get(socket);
        String disconnectReason = null;
        try {
            buffer.clear();
            ch.read(buffer);
            buffer.flip();
            int len = buffer.limit();
            if (len == 0) {
                logger.fine("Closing on 0 read");
                disconnectReason = "client closed";
            } else if (buffer.get(0) != -1) {
                logger.finest("Receiving from " + socket + " (user " + u.getId() + "):\n" + Utils.hexaString(buffer.array(), buffer.limit(), true));
                u.processBuffer(buffer);
            } else {
                u.processCommand(buffer);
            }
        } catch (IOException e) {
            logger.fine("Disconnection of user id=" + u.getId() + " caused by: " + e.getMessage());
            disconnectReason = "error - " + e.getMessage();
        }
        if (disconnectReason != null) {
            u.disconnect(disconnectReason);
        }
    }

    /**
	 * Odhlasenie usera z TCPAdapteru.
	 * Vykonava len veci spojene s TCP spojenim.
	 *
	 * @param user odhlasovany pouzivatel
	 * @throws RemoteException
	 */
    protected void disconnectAbstractUser(AbstractUser user) throws RemoteException {
        TCPUser tcpUser = (TCPUser) user;
        try {
            Socket s = tcpUser.getSocket();
            socketUser.remove(s);
            s.getChannel().keyFor(selector).cancel();
            selector.wakeup();
        } catch (Exception e) {
            throw new RemoteException(null, e);
        }
    }

    public void shutdown() throws IOException {
        super.shutdown();
        selector.wakeup();
        serverSocketChannel.close();
    }
}
