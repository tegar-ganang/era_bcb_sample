package de.tud.kom.nat.nattrav.mechanism;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import de.tud.kom.nat.comm.IMessageProcessor;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IMessage;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.comm.util.BlockingHook;
import de.tud.kom.nat.nattrav.broker.ConnectionRequestType;
import de.tud.kom.nat.nattrav.broker.IConnectionBroker;
import de.tud.kom.nat.nattrav.conn.NatConnector;
import de.tud.kom.nat.nattrav.conn.msg.RelayMessage;
import de.tud.kom.nat.nattrav.conn.msg.TCPConnRequest;
import de.tud.kom.nat.util.InetAddressUtils;
import de.tud.kom.nat.util.Logger;

/**
 * This mechanism realizes TCP Connection Reversal. Basically, it means that when we are directly
 * connected with the internet (we know our external IP - NOT behind NAT!) and want to establish a
 * connection with a peer behind a NAT, we dont try to establish one, but say him to connect to us.
 * Since we are directly in the internet, that should works fine.
 *
 * The mechanism uses the <tt>ServerSocketChannel</tt> of the <tt>NatConnector</tt> (the component
 * which is responsible for all the traffic regarding the NAT message like punchs etc).
 *
 * @author Matthias Weinert
 */
public class TCPConnReversal extends AbstractEstablishChanMechanism<SocketChannel> {

    public TCPConnReversal(IConnectionBroker connBroker, int priority) {
        super(connBroker, priority);
    }

    @Override
    protected IConnectionInfo<SocketChannel> establishConnImpl(IPeer remotePeer, ConnectionRequestType type) {
        IPeerID myID = getConnBroker().getCallback().getOwnPeerID();
        InetSocketAddress natSSCAddress = new InetSocketAddress(InetAddressUtils.getMostProbableExternalAddress(), NatConnector.DEFAULT_PORT);
        IMessage msg = new TCPConnRequest(myID, remotePeer.getPeerID(), natSSCAddress);
        SocketChannel controlChannel = getConnBroker().getControlChannelManager().getControlChannelWith(remotePeer.getPeerID());
        if (controlChannel == null) {
            msg = new RelayMessage(myID, null, remotePeer.getPeerID(), msg);
            controlChannel = getConnBroker().getControlChannelManager().getControlChannelFor(remotePeer.getPeerID());
        }
        if (controlChannel == null) {
            Logger.logWarning("[TCPConnReversal] Neither got a direct nor a indirect control channel to the peer " + remotePeer + ", mechanism failed!");
            return null;
        }
        BlockingHook bh = BlockingHook.createMessageAnswerHook(msg);
        IMessageProcessor msgProc = getConnBroker().getCommFacade().getMessageProcessor();
        msgProc.installHook(bh, bh.getPredicate());
        IEnvelope answer = null;
        try {
            try {
                getConnBroker().getCommFacade().sendTCPMessage(controlChannel, msg);
            } catch (IOException e) {
                Logger.logError(e, "Error sending relay message with TCP conn request.");
            }
            answer = bh.waitForMessage();
        } finally {
            msgProc.removeHook(bh);
        }
        if (answer != null) {
            SocketChannel sc = (SocketChannel) answer.getChannel();
            System.out.println("[TCPConnReversal] Successfully established TCP connection to " + remotePeer);
            return new ConnectionInfo<SocketChannel>(sc, false, false, null, "ConnectionReversal");
        } else return null;
    }

    @Override
    protected boolean supportsType(ConnectionRequestType type) {
        return type.equals(ConnectionRequestType.FORCE_DIRECT) || type.equals(ConnectionRequestType.NO_RESTRICTIONS);
    }
}
