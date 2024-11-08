package de.tud.kom.nat.nattrav.conn;

import java.io.Closeable;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.IMessageHandler;
import de.tud.kom.nat.comm.IMessageProcessor;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.nattrav.broker.IConnectionBroker;
import de.tud.kom.nat.nattrav.broker.RelayHostTools;
import de.tud.kom.nat.nattrav.conn.msg.ControlIntroductionAnswer;
import de.tud.kom.nat.nattrav.conn.msg.ControlIntroductionMessage;
import de.tud.kom.nat.util.InetAddressUtils;
import de.tud.kom.nat.util.Logger;

/**
 * The <tt>NatConnector</tt> is responsible for running the daemon which
 * receives control messages of NatTrav, e.g. messages instructing to punch
 * holes or to open a relay connection.
 *
 * @author Matthias Weinert
 */
public class NatConnector implements IMessageHandler, IControlChannelManager {

    /** The connection broker. */
    private IConnectionBroker connBroker;

    /** The communication facade. */
    private ICommFacade commFacade;

    /** The server where clients can ask for NAT stuff [punching etc]. */
    private ServerSocketChannel ssc;

    /** The NAT message handler. */
    private NatMessageHandler msgHandler;

    /** Normal control connections, these can be closed when a task is finished. */
    private HashMap<IPeerID, SocketChannel> openControlConnections = new HashMap<IPeerID, SocketChannel>();

    /** We are relayed by these hosts! */
    private HashMap<IPeerID, SocketChannel> relayPeers = new HashMap<IPeerID, SocketChannel>();

    /** Mappings from PeerID to the TCP channel [SERVICE: we relay FOR them]. */
    private HashMap<IPeerID, SocketChannel> controlChannelMappings = new HashMap<IPeerID, SocketChannel>();

    /** Mappings from portNr to UDP-Channel that can punch out. */
    private HashMap<Integer, DatagramChannel> udpMappings = new HashMap<Integer, DatagramChannel>();

    /** Thats my peer ID. */
    private IPeerID myself;

    public NatConnector(IConnectionBroker connBroker) {
        this(connBroker, DEFAULT_PORT);
    }

    public NatConnector(IConnectionBroker connBroker, int port) {
        this.connBroker = connBroker;
        this.commFacade = connBroker.getCommFacade();
        myself = connBroker.getCallback().getOwnPeerID();
        msgHandler = new NatMessageHandler(this, connBroker);
        new RelayMessageHandler(this, connBroker);
        createChannels(port);
        registerMessageTypes();
    }

    public IMappedAddrResolver getMappedAddrResolver() {
        return msgHandler;
    }

    public SelectableChannel getServerChannel() {
        return ssc;
    }

    private void createChannels(int port) {
        try {
            ssc = commFacade.openTCPServerSocket(port);
        } catch (BindException e) {
            if (!disableLogWhenBound) Logger.logWarning("Could not bind NatConnector. Hoping that it is already open...");
            disableLogWhenBound = true;
            closeWithoutException(ssc);
        } catch (IOException e) {
            Logger.logError(e, "Unable to initialize NatConnector! Shutting down...");
            System.exit(1);
        }
    }

    private void registerMessageTypes() {
        IMessageProcessor msgProc = commFacade.getMessageProcessor();
        msgProc.registerMessageHandler(ControlIntroductionMessage.class, this);
        msgProc.registerMessageHandler(ControlIntroductionAnswer.class, this);
    }

    public void createControlChannel(IPeer to) {
        if (InetAddressUtils.isLocalAddress(to.getAddress().getAddress())) return;
        if (to.getPeerID() != null && relayPeers.containsKey(to.getPeerID())) return;
        InetSocketAddress addr = new InetSocketAddress(to.getAddress().getAddress(), DEFAULT_PORT);
        SocketChannel sc = startTCPConn(addr);
        if (sc == null || !sc.isOpen()) return; else if (!sc.isConnected()) {
            try {
                sc.close();
            } catch (IOException e) {
            }
            return;
        }
        registerControlChannel(sc, false);
    }

    public boolean registerControlChannel(SocketChannel sc) {
        return registerControlChannel(sc, true);
    }

    private boolean registerControlChannel(SocketChannel sc, boolean deregisterSelectors) {
        commFacade.getChannelManager().registerChannel(sc);
        sendIntroduction(sc);
        Logger.log("Established TCP connection for NAT-traversal to " + sc.socket().getRemoteSocketAddress());
        return true;
    }

    public void registerUDPChannel(DatagramChannel channel) {
        if (channel.socket().getLocalAddress() == null) throw new IllegalStateException("Channel must be bound to a local address when registering!");
        udpMappings.put(channel.socket().getLocalPort(), channel);
    }

    public SocketChannel getControlChannelFor(IPeerID peerID) {
        return getControlChannelFor(peerID, true);
    }

    protected SocketChannel getControlChannelFor(IPeerID remotePeerID, boolean createIfNec) {
        Queue<InetSocketAddress> relayHosts = RelayHostTools.getRatedRelayHosts(connBroker.getCallback(), remotePeerID);
        Iterator<SocketChannel> controlIt = getControlChannelIterator();
        while (controlIt.hasNext()) {
            SocketChannel someControlConn = controlIt.next();
            if (relayHosts.contains(someControlConn.socket().getRemoteSocketAddress())) return someControlConn;
        }
        if (createIfNec) {
            Iterator<InetSocketAddress> it = relayHosts.iterator();
            SocketChannel controlChannel = null;
            while (it.hasNext() && controlChannel == null) {
                controlChannel = getControlChannelWith(it.next().getAddress());
            }
            return controlChannel;
        }
        return null;
    }

    public SocketChannel getControlChannelWith(InetAddress remoteAddr) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open();
            sc.socket().bind(null);
            sc.connect(new InetSocketAddress(remoteAddr, NatConnector.DEFAULT_PORT));
            sc.configureBlocking(false);
            connBroker.getCommFacade().getChannelManager().registerChannel(sc);
            return sc;
        } catch (IOException e) {
            closeWithoutException(sc);
            return null;
        }
    }

    /**
	 * Just returns a control channel with the given peer if we currently have one open.
	 * @param peerID peer ID
	 * @return control channel to peer if one is open, null otherwise
	 */
    public SocketChannel getControlChannelWith(IPeerID peerID) {
        if (controlChannelMappings.containsKey(peerID)) return controlChannelMappings.get(peerID); else if (this.relayPeers.containsKey(peerID)) return relayPeers.get(peerID); else if (this.openControlConnections.containsKey(peerID)) return openControlConnections.get(peerID);
        return null;
    }

    public void onMessageReceived(IEnvelope msg) {
        if (msg.getMessage() instanceof ControlIntroductionMessage) onReceiveControlIntroduction(msg, (ControlIntroductionMessage) msg.getMessage()); else if (msg.getMessage() instanceof ControlIntroductionAnswer) onReceiveControlAnswer(msg, (ControlIntroductionAnswer) msg.getMessage());
    }

    public DatagramChannel getRegisteredUDPChannel(int port) {
        return udpMappings.get(port);
    }

    private void onReceiveControlIntroduction(IEnvelope env, ControlIntroductionMessage message) {
        if (controlChannelMappings.containsKey(message.getSenderPeerID())) {
            try {
                env.getChannel().close();
            } catch (IOException e) {
            }
            return;
        }
        controlChannelMappings.put(message.getSenderPeerID(), (SocketChannel) env.getChannel());
        ControlIntroductionAnswer answer = new ControlIntroductionAnswer(connBroker.getCallback().getOwnPeerID(), env.getSender().getPeerID());
        try {
            commFacade.sendTCPMessage((SocketChannel) env.getChannel(), answer);
        } catch (IOException e) {
        }
    }

    private void onReceiveControlAnswer(IEnvelope env, ControlIntroductionAnswer answer) {
        Logger.log(answer.getSenderPeerID() + " confirmed that he is our relay host!");
        relayPeers.put(answer.getSenderPeerID(), (SocketChannel) env.getChannel());
        System.out.println(answer.getSenderPeerID() + " confirmed that he is our relay host!!");
    }

    private void sendIntroduction(SocketChannel sc) {
        try {
            commFacade.sendTCPMessage(sc, new ControlIntroductionMessage(myself));
        } catch (IOException e) {
        }
    }

    private SocketChannel startTCPConn(InetSocketAddress addr) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open();
            sc.connect(addr);
            sc.configureBlocking(false);
        } catch (IOException e) {
            closeWithoutException(sc);
        }
        if (sc == null) {
            Logger.logWarning("Could not setup relay connection to " + addr + ", we got no TCP channel.");
            return null;
        }
        return sc;
    }

    private void closeWithoutException(Closeable c) {
        try {
            if (c != null) c.close();
        } catch (IOException e) {
        }
    }

    public Iterator<SocketChannel> getControlChannelIterator() {
        ArrayList<SocketChannel> controlChannels = new ArrayList<SocketChannel>(this.relayPeers.values());
        controlChannels.addAll(this.controlChannelMappings.values());
        controlChannels.addAll(this.openControlConnections.values());
        return controlChannels.iterator();
    }

    /** Default port of the NatConnector. */
    public static final int DEFAULT_PORT = 12222;

    /** Show only once that we could bind. */
    private static boolean disableLogWhenBound = false;
}
