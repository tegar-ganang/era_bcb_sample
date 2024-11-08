package remote;

import infrastructure.exceptions.ServerFataError;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents an network server waiting for incomming connections
 * @author sashas
 *
 */
public class ConnectionManager {

    /** Keeps instance of the connection manager*/
    private static ConnectionManager theInstance = new ConnectionManager();

    /** Keeps connection manager thread that deals with all connections */
    private Thread connectionManagerThread;

    /** Keeps selector for all communications with the server*/
    private Selector connectionsSelector;

    /** pre-allocated buffer for processng input*/
    private final ByteBuffer buffer = ByteBuffer.allocate(14048576);

    private ServerSocket acceptSocket = null;

    private ServerSocketChannel channel = null;

    /** keeps pool of worker threads*/
    ExecutorService threadPool;

    private ConnectionManager() {
        connectionManagerThread = new Thread(new Runnable() {

            public void run() {
                handleNetwork();
            }
        });
        threadPool = Executors.newCachedThreadPool();
    }

    public static ConnectionManager getInstance() {
        return theInstance;
    }

    /**
	 * @param clientSocket - client socket which connection has to be added to the connection manager
	 */
    public void addConnection(Socket socket) throws IOException {
        SocketChannel chanel = socket.getChannel();
        chanel.configureBlocking(false);
        chanel.register(this.connectionsSelector, SelectionKey.OP_READ);
    }

    public void startManager() throws IOException {
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        acceptSocket = channel.socket();
        InetSocketAddress isa = new InetSocketAddress(1777);
        acceptSocket.bind(isa);
        connectionsSelector = Selector.open();
        channel.register(connectionsSelector, SelectionKey.OP_ACCEPT);
        connectionManagerThread.start();
    }

    public void stopManager() {
        connectionManagerThread.stop();
    }

    public void handleNetwork() {
        while (true) {
            try {
                int num = connectionsSelector.select();
                if (num == 0) {
                    continue;
                }
                Set<SelectionKey> activeConnections = connectionsSelector.selectedKeys();
                Iterator<SelectionKey> activeConnectionsIter = activeConnections.iterator();
                while (activeConnectionsIter.hasNext()) {
                    SelectionKey key = activeConnectionsIter.next();
                    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        Socket s = acceptSocket.accept();
                        SocketChannel sc = s.getChannel();
                        sc.configureBlocking(false);
                        sc.register(connectionsSelector, SelectionKey.OP_READ);
                        sc.finishConnect();
                    } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        if (!handleReadOp(channel)) {
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Closing connection with client '" + channel.socket().getInetAddress() + "'");
                            key.cancel();
                            try {
                                if (channel.isOpen()) {
                                    channel.close();
                                }
                            } catch (IOException closeEx) {
                                Logger.getLogger(this.getClass().getName()).logp(Level.WARNING, this.getClass().getName(), "handleNetwork", "Network error - failed to close connection", closeEx);
                            }
                        }
                    }
                }
                activeConnections.clear();
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).logp(Level.SEVERE, this.getClass().getName(), "handleNetwork", "Network error", ex);
            }
        }
    }

    private boolean handleReadOp(SocketChannel channel) {
        try {
            byte messageLengthBuf[] = new byte[4];
            ByteBuffer headerBuffer = ByteBuffer.wrap(messageLengthBuf);
            int readBytes = 0;
            readBytes = channel.read(headerBuffer);
            if (readBytes == 4) {
                headerBuffer.rewind();
                IntBuffer ib = ((ByteBuffer) headerBuffer.rewind()).asIntBuffer();
                int messageLength = ib.get();
                int messageBytes = 0;
                buffer.clear();
                do {
                    readBytes = channel.read(buffer);
                    messageBytes += readBytes;
                } while ((messageLength != messageBytes) && (-1 != readBytes) && (buffer.hasRemaining()));
                if (-1 != readBytes) {
                    buffer.flip();
                    if (0 == buffer.limit()) {
                        return false;
                    } else if (messageBytes == buffer.capacity()) {
                        ByteBuffer axilaryBuf = ByteBuffer.allocate(1);
                        if (0 != channel.read(axilaryBuf)) {
                            axilaryBuf.clear();
                            while (0 != channel.read(axilaryBuf)) {
                                axilaryBuf.clear();
                            }
                            throw new ServerFataError("Request is too long .. ");
                        }
                    }
                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.array()));
                    RemoteInvocation request = (RemoteInvocation) in.readObject();
                    dispatchRequest(request, channel);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).logp(Level.SEVERE, this.getClass().getName(), "handleNetwork", "Network error", ex);
            return handleNetworkError(ex, channel);
        } catch (ClassNotFoundException e) {
            Logger.getLogger(this.getClass().getName()).logp(Level.SEVERE, this.getClass().getName(), "handleReadOp", "Network error", e);
            return handleNetworkError(e, channel);
        } catch (Exception fatalEx) {
            Logger.getLogger(this.getClass().getName()).logp(Level.SEVERE, this.getClass().getName(), "handleReadOp", "Fatal error, Network error", fatalEx);
            return handleNetworkError(fatalEx, channel);
        }
    }

    private void dispatchRequest(RemoteInvocation request, SocketChannel channel) {
        threadPool.execute(new ConnectionHandler(request, channel));
    }

    protected void unreg(SelectionKey sk, int readyOps) {
        reg(sk, sk.interestOps() & (~readyOps));
    }

    protected void reg(SelectionKey sk, int intops) {
        sk.interestOps(intops);
    }

    /**
     * This method writes an error report to the socket, that is caused because the request cannot be displatched, 
     * for some reason
     * @param ex 	- error tp write 
     * @param socket - socket to write to
     * @throws IOException if an error could not be reprted for some reason 
     */
    protected void writeNetworkError(Exception ex, SocketChannel socket) throws IOException {
        Logger.getLogger(this.getClass().getName()).info("Writing an error to client");
        writeToSocket(ex, socket);
    }

    protected boolean handleNetworkError(Exception ex, SocketChannel socket) {
        try {
            writeNetworkError(ex, socket);
            return true;
        } catch (Exception fatalEx) {
            Logger.getLogger(this.getClass().getName()).logp(Level.SEVERE, this.getClass().getName(), "handleReadOp", "Fatal error, Could not report error .. socket closing", fatalEx);
            try {
                socket.close();
            } catch (IOException e) {
                Logger.getLogger(this.getClass().getName()).logp(Level.SEVERE, this.getClass().getName(), "handleReadOp", "Fatal error, Could not close socket", e);
            }
            return false;
        }
    }

    public static void writeToSocket(Serializable responce, SocketChannel chanel) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bytesOut));
        out.writeObject(responce);
        out.flush();
        ByteBuffer bufferOut = ByteBuffer.wrap(bytesOut.toByteArray());
        int writtenBytes = 0;
        while (bufferOut.hasRemaining()) {
            writtenBytes += chanel.write(bufferOut);
        }
        Logger.getLogger(ConnectionManager.class.getName()).log(Level.INFO, "Written  '" + writtenBytes + "' to socket");
    }
}
