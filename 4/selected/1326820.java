package de.tud.kom.nat.nattrav.broker;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import de.tud.kom.nat.comm.CommFacade;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.IMessageProcessor;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.comm.util.BlockingHook;
import de.tud.kom.nat.nattrav.conn.IControlChannelManager;
import de.tud.kom.nat.nattrav.conn.IMappedAddrResolver;
import de.tud.kom.nat.nattrav.conn.NatConnector;
import de.tud.kom.nat.nattrav.conn.msg.RelayAnswerACK;
import de.tud.kom.nat.nattrav.conn.msg.RelayAnswerMessage;
import de.tud.kom.nat.nattrav.conn.msg.RelayStartMessage;
import de.tud.kom.nat.nattrav.conn.msg.RequestTCPRelayMessage;
import de.tud.kom.nat.nattrav.conn.msg.RequestUDPRelayMessage;
import de.tud.kom.nat.nattrav.mechanism.IConnectionInfo;
import de.tud.kom.nat.nattrav.mechanism.IEstablishChanMechanism;
import de.tud.kom.nat.nattrav.mechanism.MechanismManager;
import de.tud.kom.nat.util.Logger;

/**
 * Implements all the required functionality of the <tt>IConnectionBroker</tt>.
 *
 * @author Matthias Weinert
 */
class ConnectionBroker implements IConnectionBroker {

    /** The mechanism manager. */
    private final MechanismManager mechanismManager;

    /** The application callback. */
    private final IApplicationCallback callback;

    /** Commcenter for all communication of the mechanisms. */
    private final ICommFacade commFacade;

    /** The NatConnector which deals with incoming requests for relay/punching/... */
    private final NatConnector natConnector;

    /**
	 * This creates a <tt>ConnectionBroker</tt> which uses the given callback.
	 * @param callback the application callback
	 */
    public ConnectionBroker(IApplicationCallback callback) {
        this.callback = callback;
        commFacade = new CommFacade();
        mechanismManager = new MechanismManager(this);
        natConnector = new NatConnector(this);
        commFacade.startSelectionProcess();
    }

    public SocketChannel requestTCPChannel(IPeer to) {
        return requestTCPChannel(to, ConnectionRequestType.NO_RESTRICTIONS, null);
    }

    public SocketChannel requestTCPChannel(IPeer to, ConnectionRequestType type) {
        return requestTCPChannel(to, type, null);
    }

    public SocketChannel requestTCPChannel(IPeer to, ConnectionRequestType type, InetSocketAddress bindTo) {
        Iterator<IEstablishChanMechanism<SocketChannel>> it = mechanismManager.getTCPMechanisms();
        while (it.hasNext()) {
            IEstablishChanMechanism<SocketChannel> mech = it.next();
            mech.setBindAddress(bindTo);
            IConnectionInfo<SocketChannel> info = mech.establishConn(to, type);
            SocketChannel sc = (info != null ? info.getChannel() : null);
            if (sc != null && sc.isConnected()) {
                if (!sc.socket().getRemoteSocketAddress().equals(to.getAddress())) onEstablishedRelay(sc, (InetSocketAddress) sc.socket().getRemoteSocketAddress(), to.getAddress());
                return sc;
            } else closeWithoutException(sc);
        }
        return null;
    }

    public SocketChannel requestRelayedTCPChannel(IPeer to, InetSocketAddress relayHost) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open(relayHost);
            sc.configureBlocking(false);
            commFacade.getChannelManager().registerChannel(sc);
            RequestTCPRelayMessage msg = new RequestTCPRelayMessage(getCallback().getOwnPeerID(), null, to);
            BlockingHook bh = BlockingHook.createAwaitMessageHook(sc, RelayAnswerMessage.class);
            commFacade.getMessageProcessor().installHook(bh, bh.getPredicate());
            IEnvelope env = null;
            try {
                commFacade.sendTCPMessage(sc, msg);
                env = bh.waitForMessage();
            } finally {
                commFacade.getMessageProcessor().removeHook(bh);
            }
            if (env == null) {
                Logger.logError(null, "Received no answer of RequestTCPRelayMessage, TCP relay wont work!");
                return null;
            }
            RelayAnswerMessage answer = (RelayAnswerMessage) env.getMessage();
            if (answer.isRelayEstablished()) return sc; else return null;
        } catch (IOException e) {
            if (sc != null) {
                closeWithoutException(sc);
            }
            return null;
        }
    }

    public DatagramChannel requestUDPChannel(IPeer to) {
        return requestUDPChannel(to, ConnectionRequestType.NO_RESTRICTIONS, null, true);
    }

    public DatagramChannel requestUDPChannel(IPeer to, ConnectionRequestType type) {
        return requestUDPChannel(to, type, null, true);
    }

    public DatagramChannel requestUDPChannel(IPeer to, ConnectionRequestType type, InetSocketAddress bindTo) {
        return requestUDPChannel(to, type, bindTo, true);
    }

    /**
	 * This method requests a <tt>DatagramChannel</tt> using all the given
	 * parameters. If the <tt>executeAnswerTest</tt> variable is <tt>false</tt>,
	 * no UDP connectivity test will be performed; the first mechanism which
	 * returns a channel will be returned.<br>
	 * In most cases, this means that a direct, untested connection is returned.
	 * 
	 * @param to target peer
	 * @param type request type
	 * @param bindTo bindaddress
	 * @param executeAnswerTest whether to test connectivity
	 * @return channel to peer or null, if we got nothing
	 */
    private DatagramChannel requestUDPChannel(IPeer to, ConnectionRequestType type, InetSocketAddress bindTo, boolean executeAnswerTest) {
        Iterator<IEstablishChanMechanism<DatagramChannel>> it = mechanismManager.getUDPMechanisms();
        while (it.hasNext()) {
            IEstablishChanMechanism<DatagramChannel> mech = it.next();
            mech.setBindAddress(bindTo);
            IConnectionInfo<DatagramChannel> info = mech.establishConn(to, type);
            if (info == null) continue;
            DatagramChannel dc = info.getChannel();
            if (!dc.isConnected()) {
                closeWithoutException(dc);
            }
            try {
                if (dc.isBlocking()) dc.configureBlocking(false);
            } catch (IOException e) {
            }
            boolean isEstablished = true;
            if (executeAnswerTest) isEstablished = getCallback().testUDPConnectivity(dc);
            if (isEstablished) {
                if (!dc.socket().getRemoteSocketAddress().equals(to.getAddress())) onEstablishedRelay(dc, (InetSocketAddress) dc.socket().getRemoteSocketAddress(), to.getAddress());
                return dc;
            } else {
                closeWithoutException(dc);
            }
        }
        return null;
    }

    public DatagramChannel requestRelayedUDPChannel(IPeer to, InetSocketAddress relayHost) {
        return requestRelayedUDPChannel(to, relayHost, null);
    }

    public DatagramChannel requestRelayedUDPChannel(IPeer targetPeer, InetSocketAddress relayPeerAddr, InetSocketAddress bindTo) {
        Logger.log("Trying to relay to " + targetPeer + " over " + relayPeerAddr);
        InetSocketAddress useAddress = null;
        ICommFacade commFacade = getCommFacade();
        IPeerID myPeerID = getCallback().getOwnPeerID();
        final SocketChannel controlConnection = getControlChannelManager().getControlChannelWith(relayPeerAddr.getAddress());
        if (controlConnection == null) return null;
        BlockingHook bh = BlockingHook.createAwaitMessageHook(controlConnection, RelayAnswerMessage.class);
        commFacade.getMessageProcessor().installHook(bh, bh.getPredicate());
        RelayAnswerMessage answer = null;
        try {
            try {
                commFacade.sendTCPMessage(controlConnection, new RequestUDPRelayMessage(myPeerID, null, targetPeer));
            } catch (IOException e) {
            }
            IEnvelope env = bh.waitForMessage();
            answer = (env != null ? (RelayAnswerMessage) env.getMessage() : null);
        } finally {
            commFacade.getMessageProcessor().removeHook(bh);
        }
        if (answer == null) {
            Logger.logWarning("Did not receive answer of RequestUDPRelayMessage, returning null");
            return null;
        }
        if (answer.isRelayEstablished()) {
            useAddress = answer.getUseAddress();
            Logger.log("Received RelayAnswerMessage: " + answer);
        } else {
            Logger.logWarning("Relay could not be established: " + answer.getReason());
        }
        closeWithoutException(controlConnection);
        if (useAddress != null) {
            try {
                DatagramChannel result = DatagramChannel.open();
                result.socket().bind(null);
                result.configureBlocking(false);
                commFacade.getChannelManager().registerChannel(result);
                commFacade.sendUDPMessage(result, new RelayAnswerACK(myPeerID, answer.getSenderPeerID()), useAddress);
                waitForComplete(result);
                if (!result.isConnected()) {
                    closeWithoutException(result);
                    return null;
                }
                return result;
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private void waitForComplete(final DatagramChannel dc) {
        BlockingHook bh = BlockingHook.createAwaitMessageHook(dc, RelayStartMessage.class);
        IMessageProcessor msgProc = getCommFacade().getMessageProcessor();
        msgProc.installHook(bh, bh.getPredicate());
        try {
            IEnvelope env = bh.waitForMessage();
            if (env != null) {
                try {
                    dc.connect(env.getSender().getAddress());
                } catch (IOException e) {
                }
            }
            if (env == null) Logger.logWarning("Received no RelayStart-Message!");
        } finally {
            msgProc.removeHook(bh);
        }
    }

    /**
	 * Closes w channel without showing a possible IOException.
	 * @param channel channel to close
	 */
    private void closeWithoutException(Closeable channel) {
        try {
            if (channel != null) channel.close();
        } catch (IOException e) {
        }
    }

    /**
	 * This method is invoked when we established a relay connection.
	 * @param chan the channel which uses a relayed connection
	 * @param relay the relay host
	 * @param target the relay target
	 */
    private void onEstablishedRelay(SelectableChannel chan, InetSocketAddress relay, InetSocketAddress target) {
        ConnectionTargetRequestor.getInstance().addRelayedConnection(chan, relay);
    }

    /**
	 * Returns the application callback.
	 * @return application callback
	 */
    public IApplicationCallback getCallback() {
        return callback;
    }

    /**
	 * Returns the communication center which is used by the library to relay etc. It can
	 * be used by the application, too.
	 * 
	 * @return ICommCenter which is used by the library
	 */
    public ICommFacade getCommFacade() {
        return commFacade;
    }

    public void registerUDPChannel(DatagramChannel channel) {
        natConnector.registerUDPChannel(channel);
    }

    public boolean registerControlConnection(SocketChannel socketChan) {
        return natConnector.registerControlChannel(socketChan);
    }

    public void setupControlConnection(IPeer peer) {
        natConnector.createControlChannel(peer);
    }

    public IMappedAddrResolver getMappedAddrResolver() {
        return natConnector.getMappedAddrResolver();
    }

    public NatConnector getNatConnector() {
        return natConnector;
    }

    public IControlChannelManager getControlChannelManager() {
        return natConnector;
    }
}
