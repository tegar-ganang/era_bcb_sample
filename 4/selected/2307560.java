package openrpg2.common.core.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import openrpg2.common.core.ORPGMessage;
import openrpg2.common.core.ORPGMessageQueue;
import openrpg2.common.module.NetworkedModule;

/**
 *
 * @author snowdog
 */
public class NetworkClient implements NetworkMessageRelay, NetworkModuleProvider {

    private ORPGMessageQueue inQueue;

    private ORPGMessageQueue outQueue;

    private boolean runFlag = true;

    private Object monitorLock = new Object();

    private NetworkConnection connection;

    private Selector selector = null;

    private Thread readThread = null;

    private Thread writeThread = null;

    /**
     * Network API for a client side network connection.
     */
    public NetworkClient() {
        inQueue = new ORPGMessageQueue();
        outQueue = new ORPGMessageQueue();
    }

    /**
     * Attempts to establish a network connection to the default server (localhost).
     * @return true if connection established
     */
    public boolean connect() {
        return connect(new InetSocketAddress(NetworkConnection.DEFAULT_HOST, NetworkConnection.DEFAULT_PORT));
    }

    /**
     * Attempts to establish a network connection to a specified server.
     * @param serverAddress The address and port of the server to connect to
     * @return true if connection established
     */
    public boolean connect(InetSocketAddress serverAddress) {
        connection = new NetworkConnection(0);
        selector = null;
        readThread = null;
        writeThread = null;
        if (connection.connect(serverAddress)) {
            networkThreads();
            return true;
        } else {
            return false;
        }
    }

    /**
     * check if this NetworkClient is connected to a server
     * @return true if connected to a server
     */
    public boolean isConnected() {
        if (connection == null) {
            return false;
        }
        return connection.isConnected();
    }

    /**
     * Disconnect from an OpenRPG2 server and restore the NetworkClient object to a state ready for a new connection to be made.
     */
    public void disconnect() {
        System.out.println("[DEBUG] NetworkClient::disconnect() called.");
        runFlag = false;
        synchronized (monitorLock) {
            monitorLock.notify();
        }
        if (selector != null) {
            selector.wakeup();
        }
        try {
            connection.flushOutboundBuffer();
            connection.disconnect();
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
        }
        while (inQueue.hasRemaining()) {
            inQueue.pullMessage();
        }
        while (outQueue.hasRemaining()) {
            outQueue.pullMessage();
        }
        connection = null;
    }

    /**
     * Attempts to insert a message into the outbound message queue for delivery to the server (if connected)
     * @param msg The ORPGMessage to send to the server
     * @return true if message can be sent (i.e. connected to server)
     * if false message has been discarded
     */
    public boolean sendMessage(ORPGMessage msg) {
        if (!isConnected()) {
            return false;
        }
        outQueue.putMessage(msg);
        synchronized (monitorLock) {
            monitorLock.notify();
        }
        return true;
    }

    /**
     * Determine if any ORPGMessage objects have been retrieved from the server and
     * are waiting for handling
     * @return true if ORPGMessage(s) are available
     */
    public boolean messageAvailable() {
        if (inQueue.hasRemaining()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves ORPGMessages from the internal queue.
     * @return null if no messages are available otherwise will return the first ORPGMessage
     * object available in the internal FIFO queue
     */
    public ORPGMessage readMessage() {
        ORPGMessage msg = null;
        if (inQueue.hasRemaining()) {
            msg = inQueue.pullMessage();
        }
        return msg;
    }

    private void networkThreads() {
        readThread();
        writeThread();
    }

    private void readThread() {
        try {
            selector = Selector.open();
            connection.getSocketChannel().configureBlocking(false);
            connection.getSocketChannel().register(selector, SelectionKey.OP_READ);
        } catch (ClosedChannelException cce) {
            cce.printStackTrace();
            disconnect();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            disconnect();
        }
        readThread = new Thread() {

            public void run() {
                int readyKeys = 0;
                while (runFlag) {
                    try {
                        readyKeys = selector.select(1000);
                    } catch (ClosedSelectorException e) {
                        System.err.println("CLOSED SELECTOR EXCEPTION IN NETWORKCLIENT: " + e.getMessage());
                        e.printStackTrace();
                        disconnect();
                    } catch (IOException ioe) {
                        System.err.println("EXCEPTION IN NETWORKCLIENT: " + ioe.getMessage());
                        ioe.printStackTrace();
                        disconnect();
                    }
                    if ((readyKeys > 0) && (runFlag)) {
                        Set readySet = selector.selectedKeys();
                        Iterator i = readySet.iterator();
                        i.next();
                        i.remove();
                        NetworkMessage nm = null;
                        try {
                            nm = connection.read();
                        } catch (IOException e) {
                            System.err.println("EXCEPTION IN NETWORKCLIENT READ: " + e.getMessage());
                            e.printStackTrace();
                            disconnect();
                        }
                        if (nm != null) {
                            ORPGMessage msg = new ORPGMessage(0, nm);
                            synchronized (inQueue) {
                                inQueue.putMessage(msg);
                            }
                        }
                    } else {
                    }
                }
                System.out.println("[DEBUG] NetworkClient - readThread terminating");
            }
        };
        readThread.start();
    }

    private void writeThread() {
        writeThread = new Thread() {

            public void run() {
                while (runFlag) {
                    try {
                        synchronized (monitorLock) {
                            monitorLock.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        this.interrupted();
                    }
                    while (outQueue.hasRemaining()) {
                        ORPGMessage m = outQueue.pullMessage();
                        try {
                            connection.write(m.asNetworkMessage());
                            connection.flushOutboundBuffer();
                        } catch (IOException e) {
                            System.out.println("Exception in monitorThread.writeThread write error " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("[DEBUG] NetworkClient - Monitor thread terminating");
            }
        };
        writeThread.start();
    }

    public Observable addMessageQueueObserver(Observer o) {
        inQueue.addObserver(o);
        return inQueue;
    }

    /**
     * Method to allow an ORPGMessage object to be accepted for processing
     * @param msg ORPGMessage Object
     * @return boolean. True on success, False on failure
     */
    public boolean putORPGMessage(ORPGMessage msg) {
        return sendMessage(msg);
    }

    /**
     * Retrieve first available ORPGMessage object
     * @throws openrpg2.common.core.network.NoMessageAvailableException Thrown if no messages are available for retrieval
     * @return ORPGMessage object
     */
    public ORPGMessage getORPGMessage() throws NoMessageAvailableException {
        ORPGMessage msg = readMessage();
        if (msg == null) {
            throw new NoMessageAvailableException();
        }
        return msg;
    }

    /**
     * Check if ORPGMessage can be retrieved with getORPGMessage() method
     * @return True if 1 or more ORPGMessages are ready for retrieval otherwise false.
     */
    public boolean hasORPGMessage() {
        return messageAvailable();
    }

    public NetworkedModule getNetworkModule() {
        return new NetworkClientModule(this);
    }
}
