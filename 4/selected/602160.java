package de.tud.kom.nat.im.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.IMessageHandler;
import de.tud.kom.nat.comm.IMessageProcessor;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IMessage;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.im.model.msg.JoinChatAnswer;
import de.tud.kom.nat.im.model.msg.JoinChatMessage;
import de.tud.kom.nat.im.model.msg.LeaveChatMessage;
import de.tud.kom.nat.nattrav.broker.IConnectionBroker;
import de.tud.kom.nat.util.InetAddressUtils;
import de.tud.kom.nat.util.Logger;

/**
 * All issues about joining and leaving the chatroom are handled here.
 * TODO timeouts
 *
 * @author Matthias Weinert
 */
public class AvailabilityManager implements IMessageHandler {

    /** The communication facade. */
    private final ICommFacade commFacade;

    /** The IConnectionBroker. */
    private final IConnectionBroker connBroker;

    /** The ChatModel. */
    private final ChatModel chatModel;

    /** The udp channel. */
    private final DatagramChannel udpChan;

    /** Info about myself. */
    private final IChatPartner myself;

    public AvailabilityManager(ChatModel model, DatagramChannel udpChan) {
        this.chatModel = model;
        this.connBroker = model.getConnectionBroker();
        this.commFacade = connBroker.getCommFacade();
        this.udpChan = udpChan;
        myself = chatModel.getMyself();
        registerMessageTypes();
    }

    private void registerMessageTypes() {
        IMessageProcessor msgProc = commFacade.getMessageProcessor();
        msgProc.registerMessageHandler(JoinChatMessage.class, this);
        msgProc.registerMessageHandler(JoinChatAnswer.class, this);
        msgProc.registerMessageHandler(LeaveChatMessage.class, this);
    }

    public void onMessageReceived(IEnvelope env) {
        IMessage msg = env.getMessage();
        if (msg instanceof JoinChatMessage) receiveJoinMessage(env, (JoinChatMessage) msg); else if (msg instanceof JoinChatAnswer) receiveJoinAnswer(env, (JoinChatAnswer) msg); else if (msg instanceof LeaveChatMessage) receiveLeaveMessage(env, (LeaveChatMessage) msg);
    }

    public void sendJoin(InetSocketAddress address) {
        IMessage msg = new JoinChatMessage(myself.getPeerID(), null, myself.getNickname(), myself.getPeer().getAddress());
        try {
            commFacade.sendUDPMessage(udpChan, msg, address);
        } catch (IOException e) {
            Logger.logError(e, "Failed to send join to " + address);
        }
    }

    public void sendJoin(IPeer peer) {
        if (chatModel.getChatPartner(peer.getPeerID()) != null) {
            Logger.logWarning("Not sending join, since we already know " + peer);
            return;
        }
        if (connBroker.getCallback().testUDPConnectivity(udpChan, peer.getAddress())) sendJoin(peer.getAddress());
        IMessage msg = new JoinChatMessage(myself.getPeerID(), null, myself.getNickname(), myself.getPeer().getAddress());
        DatagramChannel chan = connBroker.requestUDPChannel(peer);
        if (chan != null) {
            try {
                commFacade.sendUDPMessage(chan, msg);
            } catch (IOException e) {
                Logger.logError(e, "Failed to send join to " + peer);
            }
        } else {
            Logger.logWarning("Sending join failed, since we didnt get a channel to " + peer);
        }
    }

    public void sendJoin(IPeer peer, InetSocketAddress relayHostAddr) {
        IMessage msg = new JoinChatMessage(myself.getPeerID(), null, myself.getNickname(), myself.getPeer().getAddress());
        DatagramChannel chan = connBroker.requestRelayedUDPChannel(peer, relayHostAddr);
        if (chan != null) {
            try {
                commFacade.sendUDPMessage(chan, msg);
            } catch (IOException e) {
                Logger.logError(e, "Failed to send join to " + peer);
            }
        } else {
            Logger.logWarning("Sending join failed, since we didnt get a relayed channel to " + peer);
        }
    }

    /**
	 * Received a join. Just create a <tt>IChatPartner</tt> for the remote guy and pass 
	 * the information to the <tt>IChatModel</tt>. 
	 * @param env envelope
	 * @param message message
	 */
    private void receiveJoinMessage(IEnvelope env, JoinChatMessage message) {
        if (chatModel.getChatPartner(env.getSender().getPeerID()) == null) {
            ChatPartner cp = new ChatPartner(message.getNickname(), env.getSender().getPeerID(), message.getAddress(), env.getSender().getAddress());
            cp.setChannel((DatagramChannel) env.getChannel());
            chatModel.addChatPartner(cp);
        }
        IChatPartner me = chatModel.getMyself();
        IMessage msg = new JoinChatAnswer(myself.getPeerID(), env.getSender().getPeerID(), me.getNickname(), me.getPeer().getAddress(), env.getSender().getAddress());
        try {
            commFacade.sendUDPMessage((DatagramChannel) env.getChannel(), msg, env.getSender().getAddress());
        } catch (IOException e) {
            Logger.logError(e, "Could not send JoinAnswer back.");
        }
    }

    private void receiveJoinAnswer(IEnvelope env, JoinChatAnswer message) {
        ChatPartner cp = new ChatPartner(message.getNickname(), env.getSender().getPeerID(), message.getAddress(), env.getSender().getAddress());
        cp.setChannel((DatagramChannel) env.getChannel());
        chatModel.addChatPartner(cp);
        chatModel.addToStayAlive(cp);
        if (!message.getRecognizedAddress().getAddress().equals(InetAddressUtils.getMostProbableExternalAddress().getAddress())) connBroker.getControlChannelManager().createControlChannel(env.getSender());
    }

    private void receiveLeaveMessage(IEnvelope env, LeaveChatMessage message) {
        IChatPartner cp = chatModel.getChatPartner(message.getSenderPeerID());
        if (cp != null) chatModel.removeChatpartner(cp);
    }
}
