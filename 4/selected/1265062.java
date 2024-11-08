package sf2.io.impl.nio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import sf2.core.Config;
import sf2.core.ConfigException;
import sf2.core.Event;
import sf2.io.MessageClient;
import sf2.io.MessageException;
import sf2.io.MessageServer;
import sf2.io.event.ConnEvent;
import sf2.io.event.ConnFailedEvent;
import sf2.io.event.SendEvent;
import sf2.io.event.SendFailedEvent;
import sf2.io.impl.MessageCommon;
import sf2.io.impl.StreamObjectOutputStream;
import sf2.log.Logging;

public class NioMessageClient implements MessageClient, MessageCommon {

    protected BlockingQueue<Event> clientQ = new LinkedBlockingQueue<Event>();

    protected Map<InetAddress, ConnEntry> connPool = new HashMap<InetAddress, ConnEntry>();

    protected BlockingQueue<InetAddress> connQ = new LinkedBlockingQueue<InetAddress>();

    protected Map<SocketChannel, InetAddress> resolveMap = new HashMap<SocketChannel, InetAddress>();

    protected List<BlockingQueue<Event>> failQueues = new LinkedList<BlockingQueue<Event>>();

    protected MessageServer server;

    protected Selector selector;

    protected DatagramChannel bcastChannel;

    protected ExecutorService connExecutor;

    protected ExecutorService clientExecutor;

    protected boolean keepSilent;

    protected int port, bcastPort;

    protected int numWorkers;

    protected int maxUdpLen;

    protected InetAddress localAddr;

    protected Logging logging;

    public NioMessageClient() {
    }

    public void configure(boolean prioHigh, MessageServer server) throws MessageException {
        try {
            logging = Logging.getInstance();
            this.server = server;
            Config config = Config.search();
            keepSilent = config.getBoolean(PROP_KEEP_SILENT, DEFAULT_KEEP_SILENT);
            numWorkers = config.getInt(PROP_NUM_CLIENT_WORKERS, DEFAULT_NUM_CLIENT_WORKERS);
            maxUdpLen = config.getInt(PROP_MAX_BCAST_LENGTH, DEFAULT_MAX_BCAST_LENGTH);
            if (prioHigh) {
                port = config.getInt(PROP_PRIO_PORT, DEFAULT_PRIO_PORT);
                bcastPort = config.getInt(PROP_PRIO_UDP_PORT, DEFAULT_PRIO_UDP_PORT);
            } else {
                port = config.getInt(PROP_PORT, DEFAULT_PORT);
                bcastPort = config.getInt(PROP_UDP_PORT, DEFAULT_UDP_PORT);
            }
            logging.config(LOG_NAME, "NIO client: port=" + port + " (" + (prioHigh ? "high" : "normal") + ")");
            logging.config(LOG_NAME, "NIO client: udpPort=" + bcastPort + " (" + (prioHigh ? "high" : "normal") + ")");
            logging.config(LOG_NAME, "NIO client: numWorkers=" + numWorkers);
            logging.config(LOG_NAME, "NIO client: keepSilent=" + keepSilent);
            localAddr = server.getLocalHost();
            createSelector();
            createWorkers();
        } catch (ConfigException e) {
            throw new MessageException(e);
        } catch (UnknownHostException e) {
            throw new MessageException(e);
        } catch (IOException e) {
            throw new MessageException(e);
        }
    }

    public void shutdown() {
        logging.warning(LOG_NAME, "shutdown() is not tested.");
        connExecutor.shutdown();
        clientExecutor.shutdown();
        try {
            for (ConnEntry ent : connPool.values()) {
                ent.getChannel().close();
            }
            selector.close();
        } catch (IOException e) {
        }
    }

    public void addConnMonitor(BlockingQueue<Event> q) {
        synchronized (failQueues) {
            failQueues.add(q);
        }
    }

    public void removeFailureMonitor(BlockingQueue<Event> q) {
        synchronized (failQueues) {
            failQueues.remove(q);
        }
    }

    protected void createSelector() throws IOException {
        selector = Selector.open();
        bcastChannel = DatagramChannel.open();
        bcastChannel.socket().setBroadcast(true);
    }

    protected void createWorkers() throws IOException {
        connExecutor = Executors.newSingleThreadExecutor();
        clientExecutor = Executors.newFixedThreadPool(numWorkers);
        connExecutor.execute(new ConnTask());
        for (int i = 0; i < numWorkers; i++) {
            clientExecutor.execute(new ClientTask());
        }
    }

    public void send(InetAddress addr, int tag, Object msg, BlockingQueue<Event> q) {
        send(addr, tag, msg, q, false);
    }

    public void broadcast(InetAddress addr, int tag, Object msg, BlockingQueue<Event> q) {
        broadcast(addr, tag, msg, q, false);
    }

    public void send(InetAddress addr, int tag, Object msg, BlockingQueue<Event> q, boolean notifyDone) {
        try {
            clientQ.put(new SendRequestEvent(false, addr, tag, msg, q, notifyDone));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(InetAddress addr, int tag, Object msg, BlockingQueue<Event> q, boolean notifyDone) {
        try {
            clientQ.put(new SendRequestEvent(true, addr, tag, msg, q, notifyDone));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class ClientTask implements Runnable {

        ByteArrayOutputStream out;

        ObjectOutputStream objOut;

        public ClientTask() throws IOException {
            out = new ByteArrayOutputStream(maxUdpLen);
            objOut = new ObjectOutputStream(out);
        }

        public void run() {
            try {
                while (true) {
                    Event event = clientQ.take();
                    if (event instanceof SendRequestEvent) {
                        SendRequestEvent sendEvent = (SendRequestEvent) event;
                        if (sendEvent.isBroadcast()) handleBroadcast(sendEvent); else handleRequest(sendEvent);
                    } else if (event instanceof ConnectedEvent) {
                        handleConnected((ConnectedEvent) event);
                    } else if (event instanceof ConnectFailedEvent) {
                        handleConnFailed((ConnectFailedEvent) event);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        protected void handleConnected(ConnectedEvent connEvent) {
            InetAddress dest = null;
            ConnEntry ent = null;
            try {
                SocketChannel channel = connEvent.getChannel();
                synchronized (resolveMap) {
                    dest = resolveMap.get(channel);
                }
                synchronized (connPool) {
                    ent = connPool.get(dest);
                }
                logging.debug(LOG_NAME, "NIO client: conn established dest=" + dest);
                synchronized (ent) {
                    ent.established(channel);
                    for (Iterator<SendRequestEvent> i = ent.getPendings().iterator(); i.hasNext(); ) {
                        SendRequestEvent se = i.next();
                        sendRequest(ent.getOutputStream(), se, channel);
                        i.remove();
                    }
                }
                ConnEvent ce = new ConnEvent(dest);
                for (BlockingQueue<Event> q : failQueues) q.put(ce);
            } catch (IOException e) {
                if (keepSilent) logging.warning(LOG_NAME, "conn failed dest=" + dest); else e.printStackTrace();
                try {
                    for (SendRequestEvent se : ent.getPendings()) {
                        se.getQueue().put(new SendFailedEvent(se.getDest(), se.getTag(), se.getMessage(), false));
                    }
                } catch (InterruptedException e2) {
                }
                discardConnection(connEvent.getChannel());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        protected void handleRequest(SendRequestEvent se) {
            ConnEntry ent = null;
            boolean needEstablish = false;
            synchronized (connPool) {
                ent = connPool.get(se.getDest());
                if (ent == null) {
                    ent = connPool.put(se.getDest(), new ConnEntry(se));
                    needEstablish = true;
                }
            }
            if (needEstablish) {
                prepareConnection(se);
                return;
            }
            synchronized (ent) {
                if (ent.isConnected()) {
                    try {
                        sendRequest(ent.getOutputStream(), se, ent.getChannel());
                    } catch (IOException e) {
                        if (keepSilent) logging.warning(LOG_NAME, "send failed dest=" + se.getDest()); else e.printStackTrace();
                        try {
                            se.getQueue().put(new SendFailedEvent(se.getDest(), se.getTag(), se.getMessage(), false));
                        } catch (InterruptedException e2) {
                        }
                        discardConnection(ent.getChannel());
                    }
                } else {
                    ent.add(se);
                }
            }
        }

        protected void sendRequest(StreamObjectOutputStream out, SendRequestEvent se, SocketChannel channel) throws IOException {
            logging.debug(LOG_NAME, "SEND dest=" + se.getDest() + ", tag=" + se.getTag() + ", msg=" + se.getMessage());
            out.reset();
            out.writeUnshared(se.getTag());
            out.writeUnshared(se.getMessage());
            out.writeFutures();
            if (se.needNotifyDone()) {
                try {
                    se.getQueue().put(new SendEvent(se.getDest(), se.getTag(), se.getMessage(), false));
                } catch (InterruptedException e) {
                }
            }
        }

        protected void prepareConnection(SendRequestEvent sendEvent) {
            try {
                connQ.put(sendEvent.getDest());
                selector.wakeup();
            } catch (InterruptedException e) {
            }
        }

        protected void discardConnection(SocketChannel channel) {
            try {
                if (channel == null) return;
                InetAddress dest = null;
                synchronized (resolveMap) {
                    dest = resolveMap.remove(channel);
                }
                if (dest != null) {
                    synchronized (connPool) {
                        connPool.remove(dest);
                    }
                }
                channel.close();
                ConnFailedEvent fe = new ConnFailedEvent(dest);
                for (BlockingQueue<Event> q : failQueues) q.put(fe);
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
        }

        protected void handleConnFailed(ConnectFailedEvent fe) {
            InetAddress dest = null;
            synchronized (resolveMap) {
                dest = resolveMap.get(fe.getChannel());
            }
            if (dest != null) {
                logging.warning(LOG_NAME, "connection failed to dest=" + dest);
                ConnEntry ent = null;
                synchronized (connPool) {
                    ent = connPool.get(dest);
                }
                if (ent != null) {
                    try {
                        for (SendRequestEvent se : ent.getPendings()) {
                            se.getQueue().put(new SendFailedEvent(se.getDest(), se.getTag(), se.getMessage(), false));
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
            discardConnection(fe.getChannel());
        }

        protected void handleBroadcast(SendRequestEvent se) {
            logging.debug(LOG_NAME, "NIO client: handleBroadcast()");
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream objOut = new ObjectOutputStream(out);
                objOut.writeUnshared(se.getTag());
                objOut.writeUnshared(se.getMessage());
                byte[] array = out.toByteArray();
                DatagramPacket packet = new DatagramPacket(array, array.length, se.getDest(), bcastPort);
                bcastChannel.socket().send(packet);
                if (se.needNotifyDone()) {
                    se.getQueue().put(new SendEvent(se.getDest(), se.getTag(), se.getMessage(), true));
                }
            } catch (IOException e) {
                if (keepSilent) logging.warning(LOG_NAME, "broadcast failed dest=" + se.getDest()); else e.printStackTrace();
            } catch (InterruptedException e) {
            }
        }
    }

    class ConnTask implements Runnable {

        public void run() {
            try {
                while (true) {
                    if (selector.select() > 0) {
                        for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                            SelectionKey key = i.next();
                            i.remove();
                            handleKey(key);
                        }
                    }
                    establishConnections();
                }
            } catch (IOException e) {
                if (keepSilent) logging.warning(LOG_NAME, "ConnTask@client failed"); else e.printStackTrace();
            }
        }

        protected void establishConnections() {
            InetAddress dest;
            while ((dest = connQ.poll()) != null) {
                SocketChannel channel = null;
                try {
                    channel = SocketChannel.open();
                    synchronized (resolveMap) {
                        resolveMap.put(channel, dest);
                    }
                    channel.configureBlocking(false);
                    channel.connect(new InetSocketAddress(dest, port));
                    channel.register(selector, SelectionKey.OP_CONNECT);
                } catch (IOException e) {
                    if (keepSilent) logging.warning(LOG_NAME, "establish conn failed dest = " + dest); else e.printStackTrace();
                    try {
                        clientQ.put(new ConnectFailedEvent(channel));
                    } catch (InterruptedException e2) {
                    }
                }
            }
        }

        protected void handleKey(SelectionKey key) {
            try {
                if (key.isConnectable()) {
                    key.cancel();
                    SocketChannel channel = (SocketChannel) key.channel();
                    if (channel.isConnectionPending()) channel.finishConnect();
                    channel.configureBlocking(true);
                    clientQ.put(new ConnectedEvent(channel));
                }
            } catch (IOException e) {
                if (keepSilent) ; else e.printStackTrace();
                try {
                    clientQ.put(new ConnectFailedEvent((SocketChannel) key.channel()));
                } catch (InterruptedException e2) {
                }
            } catch (InterruptedException e) {
            }
        }
    }

    class ConnEntry {

        protected StreamObjectOutputStream out;

        protected SocketChannel channel;

        protected boolean connected;

        protected List<SendRequestEvent> pendings = new LinkedList<SendRequestEvent>();

        public ConnEntry(SendRequestEvent se) {
            connected = false;
            pendings.add(se);
        }

        public StreamObjectOutputStream getOutputStream() {
            return out;
        }

        public void established(SocketChannel channel) throws IOException {
            this.channel = channel;
            connected = true;
            out = new StreamObjectOutputStream(channel.socket().getOutputStream(), channel);
        }

        public boolean isConnected() {
            return connected;
        }

        public List<SendRequestEvent> getPendings() {
            return pendings;
        }

        public SocketChannel getChannel() {
            return channel;
        }

        public void add(SendRequestEvent se) {
            pendings.add(se);
        }
    }

    class SendRequestEvent implements Event {

        protected boolean bcast;

        protected InetAddress dest;

        protected int tag;

        protected Object msg;

        protected BlockingQueue<Event> q;

        protected boolean notifyDone;

        public SendRequestEvent(boolean bcast, InetAddress dest, int tag, Object msg, BlockingQueue<Event> q, boolean notifyDone) {
            this.bcast = bcast;
            this.dest = dest;
            this.tag = tag;
            this.msg = msg;
            this.q = q;
            this.notifyDone = notifyDone;
        }

        public boolean isBroadcast() {
            return bcast;
        }

        public InetAddress getDest() {
            return dest;
        }

        public int getTag() {
            return tag;
        }

        public Object getMessage() {
            return msg;
        }

        public BlockingQueue<Event> getQueue() {
            return q;
        }

        public boolean needNotifyDone() {
            return notifyDone;
        }
    }

    class ConnectedEvent implements Event {

        protected SocketChannel channel;

        public ConnectedEvent(SocketChannel channel) {
            this.channel = channel;
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }

    class ConnectFailedEvent implements Event {

        protected SocketChannel channel;

        public ConnectFailedEvent(SocketChannel channel) {
            this.channel = channel;
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }
}
