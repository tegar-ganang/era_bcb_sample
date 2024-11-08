package de.tud.kom.nat.nattrav.conn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.IMessageHandler;
import de.tud.kom.nat.comm.IMessageProcessor;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.comm.msg.UDPPing;
import de.tud.kom.nat.comm.util.BlockingHook;
import de.tud.kom.nat.nattrav.broker.IConnectionBroker;
import de.tud.kom.nat.nattrav.conn.msg.RelayAnswerACK;
import de.tud.kom.nat.nattrav.conn.msg.RelayAnswerMessage;
import de.tud.kom.nat.nattrav.conn.msg.RelayStartMessage;
import de.tud.kom.nat.nattrav.conn.msg.RequestTCPRelayMessage;
import de.tud.kom.nat.nattrav.conn.msg.RequestUDPRelayMessage;
import de.tud.kom.nat.nattrav.conn.msg.TCPConnRequest;
import de.tud.kom.nat.nattrav.conn.msg.UDPPunchAnswer;
import de.tud.kom.nat.nattrav.conn.msg.UDPPunchRequest;
import de.tud.kom.nat.util.IPredicate;
import de.tud.kom.nat.util.InetAddressUtils;
import de.tud.kom.nat.util.Logger;

/**
 * Responsible for establishing relays between peers.
 *
 * @author Matthias Weinert
 */
public class RelayMessageHandler implements IMessageHandler {

    private IConnectionBroker connBroker;

    private ICommFacade commFacade;

    private IPeerID myPeerID;

    private RelayDaemon daemon;

    private NatConnector natConnector;

    public RelayMessageHandler(NatConnector natConnector, IConnectionBroker connBroker) {
        this.connBroker = connBroker;
        this.commFacade = connBroker.getCommFacade();
        this.myPeerID = connBroker.getCallback().getOwnPeerID();
        daemon = new RelayDaemon(commFacade);
        this.natConnector = natConnector;
        registerMessageTypes();
    }

    private void registerMessageTypes() {
        IMessageProcessor msgProc = connBroker.getCommFacade().getMessageProcessor();
        msgProc.registerMessageHandler(RequestTCPRelayMessage.class, this);
        msgProc.registerMessageHandler(RelayAnswerMessage.class, this);
        msgProc.registerMessageHandler(RelayAnswerACK.class, this);
        msgProc.registerMessageHandler(RequestUDPRelayMessage.class, this);
        msgProc.registerMessageHandler(RelayAnswerMessage.class, this);
        msgProc.registerMessageHandler(RelayAnswerACK.class, this);
    }

    public void onMessageReceived(IEnvelope env) {
        if (env.getMessage() instanceof RequestTCPRelayMessage) {
            handleRequest(env, (RequestTCPRelayMessage) env.getMessage());
        } else if (env.getMessage() instanceof RequestUDPRelayMessage) {
            handleRequest(env, (RequestUDPRelayMessage) env.getMessage());
        }
    }

    private void handleRequest(IEnvelope env, RequestTCPRelayMessage message) {
        IPeer peer = message.getTarget();
        IPeerID requestorID = env.getSender().getPeerID();
        SocketChannel sc = natConnector.getControlChannelWith(peer.getPeerID());
        if (sc == null) {
            RelayAnswerMessage ram = new RelayAnswerMessage(myPeerID, requestorID, false, "No control channel to target peer!", null);
            try {
                commFacade.sendTCPMessage((SocketChannel) env.getChannel(), ram);
            } catch (IOException e) {
            }
            return;
        }
        InetSocketAddress addr = new InetSocketAddress(InetAddressUtils.getMostProbableExternalAddress(), NatConnector.DEFAULT_PORT);
        TCPConnRequest request = new TCPConnRequest(myPeerID, peer.getPeerID(), addr);
        BlockingHook bh = BlockingHook.createMessageAnswerHook(request);
        commFacade.getMessageProcessor().installHook(bh, bh.getPredicate());
        IEnvelope answer = null;
        try {
            try {
                commFacade.sendTCPMessage(sc, request);
            } catch (IOException e) {
            }
            answer = bh.waitForMessage();
        } finally {
            commFacade.getMessageProcessor().removeHook(bh);
        }
        if (answer == null) {
            RelayAnswerMessage ram = new RelayAnswerMessage(myPeerID, requestorID, false, "TCPConnRequest not answered by target peer!", null);
            try {
                commFacade.sendTCPMessage((SocketChannel) env.getChannel(), ram);
            } catch (IOException e) {
            }
            return;
        }
        final SocketChannel requestTarget = (SocketChannel) answer.getChannel();
        RelayAnswerMessage relayAnswer = new RelayAnswerMessage(myPeerID, env.getSender().getPeerID(), true, "", null);
        try {
            commFacade.sendTCPMessage((SocketChannel) env.getChannel(), relayAnswer);
        } catch (IOException e) {
            Logger.logError(e, "Could not send (positive) relay answer!");
        }
        final SocketChannel requestor = (SocketChannel) env.getChannel();
        daemon.addMapping(requestor, requestTarget);
    }

    private void handleRequest(IEnvelope env, RequestUDPRelayMessage message) {
        IPeer peer = message.getTarget();
        IPeerID requestorID = env.getSender().getPeerID();
        SocketChannel sc = natConnector.getControlChannelWith(peer.getPeerID());
        if (sc == null) {
            RelayAnswerMessage ram = new RelayAnswerMessage(myPeerID, requestorID, false, "No control channel to target peer!", null);
            try {
                commFacade.sendTCPMessage((SocketChannel) env.getChannel(), ram);
            } catch (IOException e) {
            }
            return;
        }
        final DatagramChannel requestor = openLocalUDPChannel();
        final DatagramChannel relayTarget = openLocalUDPChannel();
        final UDPPunchRequest udpPunchMsg = new UDPPunchRequest(myPeerID, requestorID, peer.getAddress().getPort(), false, new InetSocketAddress(InetAddressUtils.getMostProbableExternalAddress(), relayTarget.socket().getLocalPort()));
        BlockingHook waitForPunchAnswer = new BlockingHook(new IPredicate<IEnvelope>() {

            public boolean appliesTo(IEnvelope obj) {
                return (obj.getMessage() instanceof UDPPunchAnswer && ((UDPPunchAnswer) obj.getMessage()).getPunchMessageID().equals(udpPunchMsg.getMessageID()));
            }
        });
        BlockingHook waitForUDPPing = BlockingHook.createAwaitMessageHook(relayTarget, UDPPing.class);
        IEnvelope ping = null;
        UDPPunchAnswer answer = null;
        IEnvelope punchAnswerEnvelope = null;
        commFacade.getMessageProcessor().installHook(waitForPunchAnswer, waitForPunchAnswer.getPredicate());
        commFacade.getMessageProcessor().installHook(waitForUDPPing, waitForUDPPing.getPredicate());
        try {
            try {
                commFacade.sendTCPMessage(sc, udpPunchMsg);
            } catch (IOException e) {
            }
            punchAnswerEnvelope = waitForPunchAnswer.waitForMessage();
            answer = (punchAnswerEnvelope != null ? (UDPPunchAnswer) punchAnswerEnvelope.getMessage() : null);
            if (answer == null) {
                String reason = "Received no answer for the sent UDP Punch Message [to establish relay]!";
                Logger.logWarning(reason);
                sendRelayAnswer((SocketChannel) env.getChannel(), env.getSender(), false, reason, null);
                return;
            }
            if (!answer.isSuccessful()) {
                String reason = "Punching denied! Reason: " + answer.getReason();
                Logger.logWarning(reason);
                sendRelayAnswer((SocketChannel) env.getChannel(), env.getSender(), false, reason, null);
                return;
            }
            commFacade.getChannelManager().registerChannel(relayTarget);
            commFacade.getChannelManager().registerChannel(requestor);
            ping = waitForUDPPing.waitForMessage();
        } finally {
            commFacade.getMessageProcessor().removeHook(waitForPunchAnswer);
            commFacade.getMessageProcessor().removeHook(waitForUDPPing);
        }
        if (ping == null) {
            String reason = "Punching should be successful, but we couldnt receive the PING!";
            Logger.logWarning(reason);
            sendRelayAnswer((SocketChannel) env.getChannel(), env.getSender(), false, reason, null);
            return;
        }
        InetSocketAddress useAddress = new InetSocketAddress(InetAddressUtils.getMostProbableExternalAddress(), requestor.socket().getLocalPort());
        IEnvelope relayAnswerACK = waitForRelayAnswerACK((SocketChannel) env.getChannel(), requestor, env.getSender(), useAddress);
        if (relayAnswerACK == null) {
            String reason = "The requestor of the relay has not sent a RelayAnswerACK!";
            Logger.logWarning(reason);
            return;
        }
        try {
            relayTarget.connect(ping.getSender().getAddress());
            requestor.connect(relayAnswerACK.getSender().getAddress());
        } catch (IOException e) {
        }
        Logger.log("Successfully established relay: " + requestor.socket().getRemoteSocketAddress() + " -> " + " [ " + requestor.socket().getLocalSocketAddress() + " , " + relayTarget.socket().getLocalSocketAddress() + " ] " + relayTarget.socket().getRemoteSocketAddress());
        daemon.addMapping(requestor, relayTarget);
        try {
            commFacade.sendUDPMessage(requestor, new RelayStartMessage(myPeerID, requestorID));
        } catch (IOException e) {
        }
    }

    private IEnvelope waitForRelayAnswerACK(SocketChannel chan, final DatagramChannel receivingChan, IPeer to, InetSocketAddress useAddr) {
        IEnvelope relayAnswerEnvelope = null;
        BlockingHook waitForRelayAnswerACK = BlockingHook.createAwaitMessageHook(receivingChan, RelayAnswerACK.class);
        commFacade.getMessageProcessor().installHook(waitForRelayAnswerACK, waitForRelayAnswerACK.getPredicate());
        try {
            sendRelayAnswer(chan, to, true, "", useAddr);
            relayAnswerEnvelope = waitForRelayAnswerACK.waitForMessage();
        } finally {
            commFacade.getMessageProcessor().removeHook(waitForRelayAnswerACK);
        }
        return relayAnswerEnvelope;
    }

    private DatagramChannel openLocalUDPChannel() {
        DatagramChannel dc = null;
        try {
            dc = DatagramChannel.open();
            dc.socket().bind(new InetSocketAddress(0));
            dc.configureBlocking(false);
        } catch (IOException e) {
            Logger.logError(e, "Could not create and bind UDP socket for relaying!");
        }
        return dc;
    }

    private void sendRelayAnswer(SocketChannel dc, IPeer to, boolean successful, String reason, InetSocketAddress useAddress) {
        RelayAnswerMessage ram = new RelayAnswerMessage(myPeerID, to.getPeerID(), successful, reason, useAddress);
        try {
            commFacade.sendTCPMessage(dc, ram);
        } catch (IOException e) {
            Logger.logError(e, "Error sending relay answer!");
        }
    }
}
