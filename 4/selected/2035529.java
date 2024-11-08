package de.tud.kom.nat.nattrav.mechanism;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.IMessageProcessor;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.comm.msg.UDPPing;
import de.tud.kom.nat.comm.msg.UDPPong;
import de.tud.kom.nat.comm.util.BlockingHook;
import de.tud.kom.nat.nattrav.broker.ConnectionRequestType;
import de.tud.kom.nat.nattrav.broker.IConnectionBroker;
import de.tud.kom.nat.nattrav.conn.msg.PerformUDPPunchAnswer;
import de.tud.kom.nat.nattrav.conn.msg.PerformUDPPunchRequest;
import de.tud.kom.nat.util.Logger;

/**
 * This method implements UDP hole punching.
 *
 * @author Matthias Weinert
 */
public class UDPHolePunching extends AbstractEstablishChanMechanism<DatagramChannel> {

    public UDPHolePunching(IConnectionBroker connBroker, int priority) {
        super(connBroker, priority);
    }

    @Override
    protected IConnectionInfo<DatagramChannel> establishConnImpl(IPeer remotePeer, ConnectionRequestType type) {
        final boolean DETAILED_LOGGING = true;
        ICommFacade commFacade = getConnBroker().getCommFacade();
        IMessageProcessor msgProc = commFacade.getMessageProcessor();
        SocketChannel controlChannel = getConnBroker().getControlChannelManager().getControlChannelFor(remotePeer.getPeerID());
        IPeerID myself = getConnBroker().getCallback().getOwnPeerID();
        if (DETAILED_LOGGING) Logger.log("[PortRestrictedMech] Started PortRestricted Traversal Mechanism");
        if (controlChannel == null) {
            Logger.logWarning("[PortRestrictedMech] No control channel available, PortRestricted failed!");
            return null;
        }
        DatagramChannel dc = openDatagramChannel();
        commFacade.getChannelManager().registerChannel(dc);
        InetSocketAddress nextAddresses[] = getConnBroker().getMappedAddrResolver().getPossiblePorts(dc, remotePeer.getAddress());
        if (nextAddresses == null || nextAddresses.length == 0) {
            Logger.logWarning("[PortRestrictedMech] Port prediction did not work. UDPHolePunching fails!");
            return null;
        }
        BlockingHook waitForPunchAnswer = BlockingHook.createAwaitMessageHook(controlChannel, PerformUDPPunchAnswer.class);
        msgProc.installHook(waitForPunchAnswer, waitForPunchAnswer.getPredicate());
        PerformUDPPunchAnswer answer = null;
        if (DETAILED_LOGGING) Logger.log("[PortRestrictedMech] Preparing to send DoublePunch");
        try {
            PerformUDPPunchRequest punch = new PerformUDPPunchRequest(myself, remotePeer.getPeerID(), remotePeer.getPeerID(), remotePeer.getAddress().getPort(), nextAddresses);
            try {
                commFacade.sendTCPMessage(controlChannel, punch);
                IEnvelope env = waitForPunchAnswer.waitForMessage(1000);
                if (env != null) answer = (PerformUDPPunchAnswer) env.getMessage();
            } catch (IOException e) {
                Logger.logError(e, "[PortRestrictedMech] Unable to send DoublePunch-message to controlhost.");
                return null;
            }
        } finally {
            msgProc.removeHook(waitForPunchAnswer);
        }
        if (answer == null) {
            Logger.logWarning("[PortRestrictedMech] Did not receive answer of PerformUDPPunchRequest!");
            return null;
        }
        BlockingHook waitForPong = BlockingHook.createAwaitMessageHook(dc, UDPPong.class);
        msgProc.installHook(waitForPong, waitForPong.getPredicate());
        try {
            for (InetSocketAddress punchBack : answer.getPunchBackAddresses()) {
                try {
                    commFacade.sendUDPMessage(dc, new UDPPing(myself), punchBack);
                } catch (IOException e) {
                }
            }
            IEnvelope env = waitForPong.waitForMessage();
            if (env != null) {
                try {
                    Logger.log("[PortRestrictedMech] Got the UDP pong from " + env.getSender().getAddress());
                    dc.connect(env.getSender().getAddress());
                } catch (IOException e) {
                }
                System.out.println("[UDPHolePunching] Successfully established connection to " + remotePeer);
                return new ConnectionInfo<DatagramChannel>(dc, false, true, null, "UDP Holepunching");
            } else Logger.logWarning("[PortRestrictedMech] Got no UDP pong! Returning null...");
        } finally {
            msgProc.removeHook(waitForPong);
        }
        return null;
    }

    private DatagramChannel openDatagramChannel() {
        try {
            DatagramChannel dc = DatagramChannel.open();
            dc.configureBlocking(false);
            dc.socket().bind(null);
            return dc;
        } catch (IOException e) {
            Logger.logError(e, "[PortRestrictedMech] Could not create datagram channel for UDPHolePunching!");
            return null;
        }
    }

    @Override
    protected boolean supportsType(ConnectionRequestType type) {
        return type.equals(ConnectionRequestType.NO_RESTRICTIONS) || type.equals(ConnectionRequestType.FORCE_DIRECT);
    }
}
