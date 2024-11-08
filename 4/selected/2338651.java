package org.jmule.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import org.jmule.core.event.DownloadEventsListener;
import org.jmule.core.event.TrafficStatisticsUpdatedEvent;
import org.jmule.core.protocol.donkey.DonkeyConnection;
import org.jmule.core.protocol.donkey.DonkeyDownLoadLimiter;
import org.jmule.core.protocol.donkey.DonkeyUpLoadLimiter;
import org.jmule.util.LogUtil;

/** The ConnectionManager handles the networking part of creating, and most generaly
 * managing the connections to other peers.

 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:43:45 $
 */
public class ConnectionManager {

    Logger log = Logger.getLogger(ConnectionManager.class.getName());

    public static final int defaultMaxConcurrentConnections = 200;

    int maxConcurrentConnections = defaultMaxConcurrentConnections;

    public int getMaxConcurrentConnections() {
        return maxConcurrentConnections;
    }

    public void setMaxConcurrentConnections(int conCount) throws IllegalArgumentException {
        if (conCount < 1) throw new IllegalArgumentException("Parameter maxConcurrentConnections cannot be less than 1.");
        if (conCount > 65000) throw new IllegalArgumentException("Parameter maxConcurrentConnections cannot be greater than 65000.");
        maxConcurrentConnections = conCount;
    }

    /** Default constructor for Connection Manager.*/
    private ConnectionManager() {
        try {
            readSelector = SelectorProvider.provider().openSelector();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unexpected exeption: ", e);
            System.exit(1);
        }
        channelWrappers = new LinkedList();
        connectionListeners = new LinkedList();
        pendingConnections = new LinkedList();
        pendingConnectionListeners = new LinkedList();
        ConfigurationManager.getInstance().registerCategory(this.getClass());
    }

    private static ConnectionManager singleton = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return singleton;
    }

    private boolean maxConnectionsReached = false;

    private long then = 0;

    private final void checkTiming() {
        long now = System.currentTimeMillis();
        long delta = now - then;
        if (delta > 100) log.info("delta = " + delta);
        this.then = now;
    }

    private long teststart = 0;

    private long tensecondstart = 0;

    private long loopcounter = 0;

    private long shortloopcounter = 0;

    private boolean testselectmoderuns = false;

    public long getLoopCount() {
        return loopcounter;
    }

    private void testselectmode() {
        loopcounter++;
        shortloopcounter++;
        long now = System.currentTimeMillis();
        if (!testselectmoderuns) {
            teststart = now;
            tensecondstart = now;
            testselectmoderuns = true;
        }
        if (tensecondstart + 10000 < now) {
            long shortloops = shortloopcounter / ((now - tensecondstart) / 1000);
            long loops = loopcounter / ((now - teststart) / 1000);
            tensecondstart = now;
            shortloopcounter = 0;
            log.finest(" main loops last 10 sec " + shortloops + "/s overall averange: " + loops + "/s" + " ed2k udp out: " + org.jmule.core.protocol.donkey.DonkeyUdpListener.callCounterOut + " ed2k udp in: " + org.jmule.core.protocol.donkey.DonkeyUdpListener.callCounterIn + " ed2k tcp out: " + org.jmule.core.protocol.donkey.DonkeyConnectionSkeleton.callCounterOut + " ed2k tcp in: " + org.jmule.core.protocol.donkey.DonkeyConnectionSkeleton.callCounterIn);
        }
    }

    long lastNetIoTime = 0;

    int i = 0;

    /** 
	 *Called frequently from the jMule main application, FOREVER loop ( org.jmule.core.Main#start() ).
	 * It does the following:
	 * <p>until an already registred ConnectionListener reports some new connection to add ( returning true in hasNewConnection() ) <b>AND</b> the 
	 * maximum of the configured/allowed connections is *not* reached, the new connections will get prepared for reading ( that will optionally occur later on here ).
	 * <p>what are pendingConnections and how they differ from the listener.getNextNewConnection()s ?
	 * <p>the recently added new IConnectionListeners ( by using the addConnectionListener() ) get initialized and added to the internal list, so their get processed next time in the step 1)
	 * <p>all connections prepared/ready for reading are selected, optionaly the connection is finished ( in the network meaning ), and than reading.
	 * split/refactor this rather long method into some 
	 * <p> connection.check() and speed stistic are now in checkAllConnections() to get faster select cycles.
	 */
    public void doNetIo() {
        Iterator listenersIt = connectionListeners.iterator();
        while (listenersIt.hasNext()) {
            ConnectionListener listener = (ConnectionListener) listenersIt.next();
            while (numConnections() < getMaxConcurrentConnections() && listener.hasNewConnection()) {
                Connection c = listener.getNextNewConnection();
                SocketChannel sc = c.getChannel();
                try {
                    c.setSelectionKey(sc.register(readSelector, SelectionKey.OP_READ, c));
                    channelWrappers.add(c);
                    c.setConnected(true);
                    log.fine("Added new connection... " + c.getConnectionNumber());
                } catch (ClosedChannelException e) {
                    log.fine(c.toString() + "|" + e.getMessage());
                }
            }
            if (numConnections() >= getMaxConcurrentConnections()) {
                if (!maxConnectionsReached) {
                    log.info("maximum allowed number of connections reached ( (" + channelWrappers.size() + "+" + pendingConnections.size() + ")/" + getMaxConcurrentConnections() + ")");
                }
                maxConnectionsReached = true;
            } else maxConnectionsReached = false;
        }
        synchronized (pendingConnections) {
            while (!pendingConnections.isEmpty()) {
                Connection c = (Connection) pendingConnections.removeFirst();
                newConnection(c);
            }
        }
        synchronized (pendingConnectionListeners) {
            while (!pendingConnectionListeners.isEmpty()) {
                ConnectionListener cl = null;
                try {
                    cl = (ConnectionListener) pendingConnectionListeners.removeFirst();
                    cl.init();
                    connectionListeners.add(cl);
                } catch (Exception e) {
                    log.warning(e.getClass().getName() + ":" + cl.toString() + "|" + e.getMessage());
                }
            }
        }
        int newKeys;
        try {
            long now = System.currentTimeMillis();
            long selecttime = 100L - now + lastNetIoTime;
            if (selecttime <= 0) {
                selecttime = 1;
            }
            lastNetIoTime = now;
            newKeys = readSelector.select(selecttime);
            testselectmode();
            if (newKeys > 0) {
                Set keys = readSelector.selectedKeys();
                Iterator i = keys.iterator();
                while (i.hasNext()) {
                    SelectionKey nextKey = (SelectionKey) i.next();
                    i.remove();
                    if (nextKey.isConnectable()) {
                        Connection c = (Connection) nextKey.attachment();
                        SocketChannel sc = (SocketChannel) nextKey.channel();
                        try {
                            log.finest("finishConnect start");
                            boolean result = sc.finishConnect();
                            assert result == true;
                            log.finest("finishConnect end");
                            nextKey.interestOps(SelectionKey.OP_READ);
                            c.setSelectionKey(nextKey);
                            channelWrappers.add(c);
                            c.setConnected(true);
                        } catch (ConnectException e) {
                            if (e.getMessage().startsWith("Connection refused")) {
                                c.setConnected(false);
                            }
                            if (e.getMessage().startsWith("Connection timed out")) ;
                            log.log(Level.FINE, "connect failed to " + c.getPeerAddress(), e);
                            c.close();
                        } catch (NoRouteToHostException e) {
                            log.log(Level.FINE, "connect failed (no route to host) to " + c.getPeerAddress(), e);
                            c.close();
                        } catch (java.net.BindException e) {
                            if (e.getMessage().startsWith("Cannot assign requested address: ")) {
                                c.setConnected(false);
                                log.fine("connection to " + c.getPeerAddress() + " faild:  " + e.toString());
                            } else {
                                log.config(e.getMessage());
                                log.warning(c.getConnectionNumber() + " Error making connection to " + c.getPeerAddress() + ": " + e.toString());
                            }
                        } catch (SocketException e) {
                            log.log(Level.WARNING, "connect failed to " + c.getPeerAddress(), e);
                            c.close();
                        }
                    } else {
                        if (nextKey.isReadable()) {
                            ChannelWrapper c = (ChannelWrapper) nextKey.attachment();
                            if (c instanceof DonkeyConnection) {
                                DonkeyDownLoadLimiter.Limiter.incConnections();
                                reading.add(c);
                            } else {
                                try {
                                    if (!c.processInput()) {
                                        log.fine(c.hashCode() + " Channel closed.");
                                        c.close();
                                        channelWrappers.remove(c);
                                    }
                                } catch (IOException ioe) {
                                    c.close();
                                    channelWrappers.remove(c);
                                }
                            }
                        } else if (!nextKey.isWritable()) {
                            ChannelWrapper c = (ChannelWrapper) nextKey.attachment();
                            log.warning("connection going into limbo: " + c.getClass().getName() + ((c instanceof Connection) ? " " + ((Connection) c).getConnectionNumber() : ""));
                        } else {
                            ChannelWrapper c = (ChannelWrapper) nextKey.attachment();
                            if (c instanceof DonkeyConnection) {
                                DonkeyUpLoadLimiter.Limiter.incConnections();
                                writeing.add(c);
                            } else {
                                c.processOutput();
                            }
                        }
                    }
                }
                while (!writeing.isEmpty()) {
                    ChannelWrapper c = (ChannelWrapper) writeing.remove(0);
                    c.processOutput();
                }
                while (!reading.isEmpty()) {
                    ChannelWrapper c = (ChannelWrapper) reading.remove(0);
                    try {
                        if (!c.processInput()) {
                            log.fine(c.hashCode() + " Channel closed.");
                            c.close();
                            channelWrappers.remove(c);
                        }
                    } catch (ClosedChannelException e) {
                        log.fine(c.hashCode() + " Channel closed.");
                        c.close();
                        channelWrappers.remove(c);
                    } catch (IOException e) {
                        log.fine(c.hashCode() + " Error reading bytes: " + e.getMessage());
                        c.close();
                        channelWrappers.remove(c);
                    }
                }
            }
        } catch (IOException e) {
            log.warning("ConnectionManager read/connect error: " + e.getClass() + " : " + e.getMessage());
        }
    }

    public SelectionKey addToIOSelector(DatagramChannelWrapper dcw, int ops) throws java.nio.channels.ClosedChannelException {
        SelectionKey key = dcw.getSelectableChannel().register(readSelector, ops, dcw);
        channelWrappers.add(dcw);
        return key;
    }

    public void checkAllConnections() {
        i++;
        Iterator it = channelWrappers.iterator();
        while (it.hasNext()) {
            ChannelWrapper channelWrapper = (ChannelWrapper) it.next();
            if (!channelWrapper.check(i)) {
                it.remove();
                log.fine(channelWrapper.hashCode() + " Removed connection.");
            }
        }
        if (i % 100 == 0 && (oldallrecvspeed != allrecvspeed || oldallsendspeed != allsendspeed)) {
            oldallrecvspeed = allrecvspeed;
            oldallsendspeed = allsendspeed;
            updateTrafficListeners(new TrafficStatisticsUpdatedEvent(this, allrecvspeed, allsendspeed));
        }
        if (i == 1000) i = 0;
        calcoverallspeed();
    }

    ArrayList trafficListeners = new ArrayList();

    public void addTrafficStatisticsEventListener(DownloadEventsListener listener) {
        trafficListeners.add(listener);
    }

    public void removeTrafficStatisticsEventListener(DownloadEventsListener listener) {
        trafficListeners.remove(listener);
    }

    void updateTrafficListeners(TrafficStatisticsUpdatedEvent event) {
        for (Iterator iter = trafficListeners.iterator(); iter.hasNext(); ) {
            DownloadEventsListener listener = (DownloadEventsListener) iter.next();
            listener.trafficStatisticsUpdatedFired(event);
        }
    }

    public synchronized void addConnection(Connection connection) {
        synchronized (pendingConnections) {
            pendingConnections.add(connection);
        }
    }

    public synchronized void addConnectionListener(ConnectionListener connectionListener) {
        synchronized (pendingConnectionListeners) {
            pendingConnectionListeners.add(connectionListener);
        }
    }

    /** this method actually sets up a connection c, initializes it and put it to the lits of 
	* activly maintained connections. 
	* Note, that this method seems to be rather protected/private helper ConnectionManager method. The only other
	* place it is called out, is the org/jmule/core/protocol/donkey/DonkeyServerConnection.java.
	 */
    public synchronized void newConnection(Connection c) {
        LogUtil.entering(log);
        InetSocketAddress address = c.getPeerAddress();
        try {
            SocketChannel sc = SocketChannel.open();
            try {
                sc.socket().setReceiveBufferSize(65536);
            } catch (SocketException e) {
                log.log(Level.WARNING, "receive buffer size failed", e);
            }
            sc.configureBlocking(false);
            c.setChannel(sc);
            c.setLastActivity(System.currentTimeMillis());
            if (sc.connect(address)) {
                log.finest(c.getConnectionNumber() + " connected at once to " + address.getHostName());
                channelWrappers.add(c);
                c.setConnected(true);
                c.setSelectionKey(sc.register(readSelector, SelectionKey.OP_READ, c));
            } else {
                SelectionKey connectKey = sc.register(readSelector, SelectionKey.OP_CONNECT, c);
            }
        } catch (java.net.BindException e) {
            if (e.getMessage().startsWith("Cannot assign requested address: ")) {
                c.setConnected(false);
                log.fine("connection to " + address.toString() + " faild:  " + e.toString());
            } else {
                log.config(e.getMessage());
                log.warning(c.getConnectionNumber() + " Error making connection to " + address.toString() + ": " + e.toString());
            }
        } catch (ClosedChannelException e) {
            log.warning(c.getConnectionNumber() + " Error making connection to " + address.toString() + ": " + e.toString());
        } catch (IOException e) {
            log.warning(c.getConnectionNumber() + " Error making connection to " + address.toString() + ": " + e.toString());
        }
    }

    /** Returns the number of connections managed by the ConnectionManger.
	 * @return number connections + pending connections
	 */
    public int numConnections() {
        return (channelWrappers.size() + pendingConnections.size());
    }

    protected Selector readSelector;

    private List writeing = new ArrayList(10);

    private List reading = new ArrayList(10);

    private static final int SPEEDINTERVALLSIZE = 100;

    private long[] overallsendspeed = new long[SPEEDINTERVALLSIZE];

    private long[] overallrecvspeed = new long[SPEEDINTERVALLSIZE];

    private long[] clockintervall = new long[SPEEDINTERVALLSIZE];

    private long allsendspeed = 0;

    private long allrecvspeed = 0;

    private long oldallsendspeed = -1;

    private long oldallrecvspeed = -1;

    private long actsendbytes = 0;

    private long actrecvbytes = 0;

    private int overallclock = 0;

    private long lastcalltime = System.currentTimeMillis();

    private boolean onecycle = false;

    /**
	* To avoid downloading more bytes than is allowed by the user call this in all p2p-protocols to count overall send bytes.
	*/
    public void addBytesRecievedfromInternet(long bytes) {
        actrecvbytes += bytes;
    }

    /**
	* To avoid uploading more bytes than is allowed by the user call this in all p2p-protocols to count overall send bytes.
	*/
    public void addBytesSendtoInternet(long bytes) {
        actsendbytes += bytes;
    }

    /**
	* To avoid uploading more bytes than is allowed by the user call this in all p2p-protocols to get overall sendspeed.
	* @return speed in bytes per second.
	*/
    public long getActSendSpeed() {
        long result;
        result = allsendspeed;
        return result;
    }

    private void calcoverallspeed() {
        long time = System.currentTimeMillis();
        if (time > lastcalltime + 50) {
            overallsendspeed[overallclock] = actsendbytes;
            overallrecvspeed[overallclock] = actrecvbytes;
            actrecvbytes = 0;
            int limit = onecycle ? SPEEDINTERVALLSIZE - 1 : overallclock;
            long sendspeed = 0;
            long recvspeed = 0;
            long tarnstime = 0;
            for (int j = 0; j <= limit; j++) {
                sendspeed += overallsendspeed[j];
                recvspeed += overallrecvspeed[j];
            }
            if (onecycle) {
                tarnstime = time - clockintervall[overallclock];
            } else {
                tarnstime = 50 * overallclock + 50;
            }
            clockintervall[overallclock] = time;
            actsendbytes = 0;
            allsendspeed = (sendspeed * 1000) / tarnstime;
            allrecvspeed = (recvspeed * 1000) / tarnstime;
            overallclock++;
            if (overallclock >= SPEEDINTERVALLSIZE) {
                overallclock = 0;
                onecycle = true;
            }
            lastcalltime = time;
        }
    }

    private LinkedList channelWrappers;

    protected LinkedList connectionListeners;

    protected LinkedList pendingConnections;

    protected LinkedList pendingConnectionListeners;
}
