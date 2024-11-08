package com.handjoys;

import java.util.Timer;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.zip.Deflater;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import com.handjoys.socket.SelectorFactory;
import com.handjoys.socket.DirectBufferFactory;
import com.handjoys.socket.ConnectionCleanerTask;
import com.handjoys.socket.GSession;
import com.handjoys.socket.RServerFactory;
import com.handjoys.socket.GBBServer;
import com.handjoys.conf.ConfigReader;
import com.handjoys.conf.ConfigParam;
import com.handjoys.logger.FileLogger;
import com.handjoys.startup.Startup;
import com.handjoys.startup.ShutdownManager;
import com.handjoys.packet.MessagePacket;
import com.handjoys.packet.MessageParser;
import com.handjoys.gcm.GCManager;
import com.handjoys.console.GameState;
import com.handjoys.console.MovableObject;

public class GameServer extends Thread {

    private static final int CONNECTION_CLEANER_INTERVAL = 5 * 60;

    private static final int SESSION_INITIALCAPACITY = 100000;

    private static final float SESSION_LOADFACTOR = 0.5f;

    private static GameServer instance;

    private GCManager gcm;

    private ServerSocketChannel sSockChan;

    private LinkedList<Startup> startups;

    private SelectionKey acceptKey;

    private Selector acceptSelector;

    private Selector readSelector;

    private long serverStartTime = 0;

    public boolean IS_SHUTTING_DOWN = false;

    private Queue<SocketChannel> clients;

    private LinkedBlockingQueue<GSession> workersQueue;

    private Map<String, LinkedList<String>> channelEvents;

    private int receiveBufferSize;

    private ByteBuffer readBuffer;

    private CharsetDecoder utfDecoder;

    private Charset utfEncoder;

    private EventReader eventReader;

    private EventWriter eventWriter;

    private Timer connectionCleanerTimer;

    private byte[] receiveBytes;

    private byte[] sendBytes;

    private HashMap<SocketChannel, StringBuffer> invalidXml;

    public GameServer() {
        super("GameServer");
        startups = new LinkedList<Startup>();
        clients = new ConcurrentLinkedQueue<SocketChannel>();
        workersQueue = new LinkedBlockingQueue<GSession>();
        channelEvents = new ConcurrentHashMap<String, LinkedList<String>>(SESSION_INITIALCAPACITY, SESSION_LOADFACTOR, 2);
        receiveBufferSize = ((Integer) (ConfigReader.getParam(ConfigParam.RECEIVEBUFFERSIZE))).intValue();
        readBuffer = ByteBuffer.allocateDirect(receiveBufferSize);
        utfDecoder = Charset.forName("UTF-8").newDecoder();
        utfEncoder = Charset.forName("UTF-8");
        receiveBytes = new byte[receiveBufferSize];
        sendBytes = new byte[receiveBufferSize];
        invalidXml = new HashMap<SocketChannel, StringBuffer>();
        welcomeMessage();
        showSystemInfo();
        ConfigReader.print();
        loadStartUp();
        gcm = new GCManager();
        gcm.init();
        GameState.init();
        ShutdownManager shutDownManager = new ShutdownManager();
        Runtime.getRuntime().addShutdownHook(shutDownManager);
    }

    public static void main(String[] args) {
        instance = new GameServer();
        getInstance().start();
    }

    public void run() {
        initSocketServer();
        Broadcaster.init(this);
        RServerFactory.init(this);
        GBBServer.init(this);
        eventReader = new EventReader(this);
        eventWriter = new EventWriter(this);
        eventWriter.initWriter();
        while (!IS_SHUTTING_DOWN) {
            acceptNewConnections();
            try {
                Thread.sleep(20L);
            } catch (InterruptedException ie) {
                FileLogger.error(Thread.currentThread().getName() + " InterruptedException during Accept task");
            }
        }
    }

    private void acceptNewConnections() {
        SocketChannel clientChannel = null;
        try {
            acceptSelector.select();
            Set readyKeys = acceptSelector.selectedKeys();
            for (Iterator i = readyKeys.iterator(); i.hasNext(); ) {
                SelectionKey key = (SelectionKey) i.next();
                i.remove();
                clientChannel = ((ServerSocketChannel) key.channel()).accept();
                clientChannel.configureBlocking(false);
                clientChannel.socket().setTcpNoDelay(true);
                SelectionKey skey = clientChannel.register(readSelector, SelectionKey.OP_READ, null);
                clients.add(clientChannel);
                FileLogger.info("accept new connection:" + clientChannel.socket().toString());
            }
        } catch (Exception e) {
            FileLogger.error("Generic Exception in acceptNewConnections():" + e);
            e.printStackTrace();
        }
    }

    public void readIncomingMessages() {
        try {
            readSelector.selectNow();
            Set readyKeys = readSelector.selectedKeys();
            if (readyKeys.size() == 0) return;
            for (Iterator i = readyKeys.iterator(); i.hasNext(); ) {
                SelectionKey key = (SelectionKey) i.next();
                i.remove();
                if (!key.isValid()) {
                    key.cancel();
                    FileLogger.debug(" readIncomingMessages key.isValid  lostConnection");
                    continue;
                }
                SocketChannel channel = (SocketChannel) key.channel();
                try {
                    readBuffer.clear();
                    long nbytes = channel.read(readBuffer);
                    if (nbytes == -1L) {
                        FileLogger.debug("readIncomingMessages channel.read(readBuffer) = -1L  lostConnection");
                        lostConnection(channel);
                        continue;
                    }
                    if (nbytes <= 0) {
                        continue;
                    }
                    readBuffer.position((int) nbytes - 1);
                    readBuffer.flip();
                    String msg = utfDecoder.decode(readBuffer).toString();
                    FileLogger.debug(channel.socket().getRemoteSocketAddress() + ": readIncomingMessages=" + msg);
                    String[] moreMsg = msg.split("\n");
                    for (int x = 0; x < moreMsg.length; x++) {
                        String oneMsg = moreMsg[x];
                        if (oneMsg.equals("")) continue;
                        if (!oneMsg.endsWith("</msg>") || !oneMsg.startsWith("<msg")) {
                            StringBuffer oldXml = invalidXml.get(channel);
                            if (oldXml != null) {
                                oldXml.append(oneMsg);
                                String newXml = oldXml.toString();
                                if (!oneMsg.endsWith("</msg>") || !oneMsg.startsWith("<msg")) {
                                    invalidXml.put(channel, oldXml);
                                    continue;
                                } else {
                                    oneMsg = newXml;
                                    invalidXml.remove(channel);
                                }
                            } else {
                                invalidXml.put(channel, new StringBuffer(oneMsg));
                                continue;
                            }
                        }
                        int sAttr_start = oneMsg.indexOf("s='");
                        if (sAttr_start == -1) continue;
                        sAttr_start += 3;
                        int sAttr_end = oneMsg.indexOf('\'', sAttr_start);
                        if (sAttr_end == -1) continue;
                        String sessionID = oneMsg.substring(sAttr_start, sAttr_end);
                        LinkedList<String> q = (LinkedList<String>) channelEvents.get(sessionID);
                        if (q == null) {
                            q = new LinkedList<String>();
                            q.add(oneMsg);
                            channelEvents.put(sessionID, q);
                        } else {
                            synchronized (q) {
                                q.add(oneMsg);
                            }
                        }
                        workersQueue.add(new GSession(sessionID, channel));
                        FileLogger.debug("sessionList size=" + q.size());
                    }
                } catch (NotYetConnectedException nye) {
                    if (!key.isValid()) {
                        key.cancel();
                        key = null;
                    }
                    FileLogger.debug("readIncomingMessages NotYetConnectedException");
                    nye.printStackTrace();
                } catch (AsynchronousCloseException ace) {
                    if (!key.isValid()) {
                        key.cancel();
                        key = null;
                    }
                    FileLogger.debug("readIncomingMessages AsynchronousCloseException");
                    ace.printStackTrace();
                } catch (IllegalStateException ise) {
                    if (!key.isValid()) {
                        key.cancel();
                        key = null;
                    }
                    FileLogger.debug("readIncomingMessages IllegalStateException");
                    ise.printStackTrace();
                } catch (Exception e) {
                    if (!key.isValid()) {
                        key.cancel();
                        key = null;
                    }
                    FileLogger.debug("readIncomingMessages Exception");
                    e.printStackTrace();
                    lostConnection(channel);
                }
            }
        } catch (ClosedSelectorException cse) {
            FileLogger.error("readIncomingMessages ClosedSelectorException: ");
            cse.printStackTrace();
            try {
                readSelector = Selector.open();
            } catch (IOException ie) {
                FileLogger.error("readIncomingMessages  readSelector = Selector.open() IOException ");
                ie.printStackTrace();
            }
        } catch (IOException ioe) {
            FileLogger.error("readIncomingMessages I/O problems while reading socket > ");
            ioe.printStackTrace();
        }
    }

    public void writeOutgoingMessage() {
        SelectionKey key = null;
        Selector writeSelector = null;
        int attempts = 0;
        int bytesProduced = 0;
        long writeTimeout = 500;
        GSession gSession = null;
        try {
            gSession = (GSession) workersQueue.take();
        } catch (InterruptedException ie) {
            return;
        }
        if (gSession == null) return;
        String sessionID = gSession.sessionID;
        SocketChannel socketChannel = gSession.channel;
        ByteBuffer buffer = DirectBufferFactory.getBuffer();
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(receiveBufferSize);
        }
        try {
            LinkedList q = channelEvents.get(sessionID);
            synchronized (q) {
                while (q != null && q.size() != 0) {
                    String msg = (String) q.remove();
                    String ret = null;
                    if (!msg.startsWith(Broadcaster.BROADSYMBOL)) {
                        MessagePacket packet = MessageParser.parseMessgae(msg);
                        if (packet.isValid()) {
                            ret = GCManager.executeLocal(packet, gSession);
                        }
                        if (ret == null) {
                            continue;
                        }
                        if (ret.equals("")) continue;
                        ret += "\n";
                    } else {
                        ret = msg.substring(2, msg.length());
                    }
                    System.out.println("ret=" + ret);
                    if (ret.indexOf("s='") == -1) {
                        int spos = ret.indexOf(">");
                        ret = ret.substring(0, spos) + " s='" + sessionID + "'" + ret.substring(spos);
                    }
                    FileLogger.debug("socketChannel.write = " + ret);
                    buffer.clear();
                    buffer.put(utfEncoder.encode(ret));
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        int len = socketChannel.write(buffer);
                        FileLogger.debug("socketChannel.write(buffer) = " + len);
                        attempts++;
                        if (len < 0) {
                            FileLogger.debug(" socketChannel.write(buffer) Exception lostConnection");
                            lostConnection(socketChannel);
                        }
                        bytesProduced += len;
                        if (len == 0) {
                            if (writeSelector == null) {
                                writeSelector = SelectorFactory.getSelector();
                                if (writeSelector == null) {
                                    continue;
                                }
                            }
                            key = socketChannel.register(writeSelector, key.OP_WRITE);
                            if (writeSelector.select(writeTimeout) == 0) {
                                if (attempts > 5) {
                                    FileLogger.debug(" attempts > 5  lostConnection");
                                    break;
                                }
                            } else {
                                attempts--;
                            }
                        } else {
                            attempts = 0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLogger.debug(" writeOutgoingMessage Exception lostConnection");
            e.printStackTrace();
        } finally {
            if (key != null) {
                key.cancel();
                key = null;
            }
            if (buffer != null) DirectBufferFactory.returnBuffer(buffer);
            if (writeSelector != null) {
                try {
                    writeSelector.selectNow();
                    SelectorFactory.returnSelector(writeSelector);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
    }

    public void lostConnection(SocketChannel sc) {
        RServerFactory.removeRServer(sc);
        String ip = null;
        if (sc.isConnected()) {
            ip = sc.socket().getInetAddress().toString();
            FileLogger.info(sc + " closed ");
            try {
                sc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        clients.remove(sc);
    }

    public static GameServer getInstance() {
        if (instance == null) {
            instance = new GameServer();
        }
        return instance;
    }

    private void welcomeMessage() {
        System.out.println("|------------------------------------------------------------|");
        System.out.println("|                  Handjoys Game Server                      |");
        System.out.println("|                Multiplayer Socket Server                   |");
        System.out.println("|                      version 1.1                           |");
        System.out.println("|                           ---                              |");
        System.out.println("|              (c) 2007 - 2009 handjoys()                    |");
        System.out.println("|                  www.handjoys.com                          |");
        System.out.println("|------------------------------------------------------------|\n");
    }

    private void showSystemInfo() {
        List<String> props = new ArrayList<String>();
        FileLogger.info("--- [ System Info ] ---\n");
        props.add("os.name");
        props.add("os.arch");
        props.add("os.version");
        props.add("java.version");
        props.add("java.vendor");
        props.add("java.vendor.url");
        props.add("java.vm.specification.version");
        props.add("java.vm.version");
        props.add("java.vm.vendor");
        props.add("java.vm.name");
        Runtime rt = Runtime.getRuntime();
        FileLogger.info("System CPU(s): " + rt.availableProcessors());
        FileLogger.info("VM Max memory: " + rt.maxMemory() / 0xf4240L + " MB");
        String prop;
        for (Iterator i = props.iterator(); i.hasNext(); FileLogger.info(prop + ": " + System.getProperty(prop))) prop = (String) i.next();
        FileLogger.info("\n\n--- [ Network Cards ] ---\n");
        try {
            for (Enumeration list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements(); ) {
                NetworkInterface iFace = (NetworkInterface) list.nextElement();
                FileLogger.info("Card:" + iFace.getDisplayName());
                InetAddress adr;
                for (Enumeration addresses = iFace.getInetAddresses(); addresses.hasMoreElements(); FileLogger.info(" -> " + adr.getHostAddress())) adr = (InetAddress) addresses.nextElement();
            }
        } catch (Exception se) {
            FileLogger.error("Failed discovering network cards!");
            System.exit(1);
        }
    }

    private void loadStartUp() {
        Vector v = (Vector) ConfigReader.getParam(ConfigParam.MODULES);
        FileLogger.info("\n\n--- [ StartUp Object Init ] ---\n");
        try {
            for (int i = 0; i < v.size(); i++) {
                String clsName = (String) v.elementAt(i);
                Class cls = Class.forName(clsName);
                Startup startUp = (Startup) cls.newInstance();
                startUp.start();
                startups.add(startUp);
            }
        } catch (ClassNotFoundException cfe) {
            FileLogger.error("Failed load Startup class!");
            cfe.printStackTrace();
            System.exit(1);
        } catch (InstantiationException ie) {
            FileLogger.error("Failed load Startup class!");
            ie.printStackTrace();
            System.exit(1);
        } catch (IllegalAccessException iae) {
            FileLogger.error("Failed load Startup class!");
            iae.printStackTrace();
            System.exit(1);
        }
    }

    public void initSocketServer() {
        String hostName = (String) ConfigReader.getParam(ConfigParam.SERVERIP);
        int port = ((Integer) (ConfigReader.getParam(ConfigParam.SERVERPORT))).intValue();
        int backlog = ((Integer) (ConfigReader.getParam(ConfigParam.BACKLOG))).intValue();
        try {
            sSockChan = ServerSocketChannel.open();
            sSockChan.configureBlocking(false);
            InetAddress addr = InetAddress.getByName(hostName);
            sSockChan.socket().setReceiveBufferSize(receiveBufferSize);
            sSockChan.socket().setReuseAddress(true);
            sSockChan.socket().bind(new InetSocketAddress(addr, port), backlog);
            readSelector = Selector.open();
            acceptSelector = Selector.open();
            acceptKey = sSockChan.register(acceptSelector, SelectionKey.OP_ACCEPT);
            FileLogger.info("\n\n--- [ Server Starting ] ---\n");
            FileLogger.info("Server address: " + addr.getHostAddress());
            FileLogger.info("Server port   : " + port + "\n");
            serverStartTime = System.currentTimeMillis();
        } catch (Exception e) {
            FileLogger.error("\n\n[ --> FATAL ERROR <-- ]: Error initializing server.\n\nCheck if server address and port are properly configured.\n");
            FileLogger.error("Exception caught in " + Thread.currentThread().getName());
            System.exit(1);
        }
    }

    public void addBroadEvent(String msg, GSession session) {
        LinkedList<String> q = (LinkedList<String>) channelEvents.get(session.sessionID);
        if (q != null) {
            q.add(msg + "\n");
        }
        workersQueue.add(session);
    }

    public void initRServer(String msg, GSession session) {
        clients.add(session.channel);
        LinkedList<String> q = (LinkedList<String>) channelEvents.get(session.sessionID);
        if (q == null) {
            q = new LinkedList<String>();
            channelEvents.put(session.sessionID, q);
        }
        try {
            session.channel.configureBlocking(false);
            session.channel.socket().setTcpNoDelay(true);
            SelectionKey skey = session.channel.register(readSelector, SelectionKey.OP_READ, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg != null) Broadcaster.write(msg, session);
    }

    public String printBuffer(ByteBuffer readBuffer) throws UnsupportedEncodingException {
        ByteBuffer test = readBuffer.duplicate();
        int c = 0;
        try {
            while (true) {
                receiveBytes[c++] = test.get();
            }
        } catch (BufferUnderflowException ee) {
        }
        return new String(receiveBytes, 0, c - 1, "utf-8");
    }

    public Queue getChannels() {
        return clients;
    }

    public void acceptNewPlayer(Long playerID, SocketChannel channel) {
    }
}
