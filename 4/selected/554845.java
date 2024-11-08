package de.tud.kom.nat.im.model;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.IMessageHandler;
import de.tud.kom.nat.comm.IMessageProcessor;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IMessage;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.comm.msg.Peer;
import de.tud.kom.nat.comm.msg.StayAliveMessage;
import de.tud.kom.nat.comm.msg.UDPPing;
import de.tud.kom.nat.comm.msg.UDPPong;
import de.tud.kom.nat.im.model.files.FileTransfer;
import de.tud.kom.nat.im.model.files.FileTransferManager;
import de.tud.kom.nat.im.model.msg.ChatMessage;
import de.tud.kom.nat.im.model.msg.HostInfo;
import de.tud.kom.nat.im.model.msg.KnownHostsMessage;
import de.tud.kom.nat.im.model.msg.LeaveChatMessage;
import de.tud.kom.nat.nattrav.broker.ConnectionManagerFactory;
import de.tud.kom.nat.nattrav.broker.IApplicationCallback;
import de.tud.kom.nat.nattrav.broker.IConnectionBroker;
import de.tud.kom.nat.nattrav.conn.NatConnector;
import de.tud.kom.nat.util.InetAddressUtils;
import de.tud.kom.nat.util.Logger;

/**
 * The chat model implements all functionality described in the interface
 * <tt>IChatModel</tt>. It provides an abstraction of the chat room.
 * 
 * @author Matthias Weinert
 */
public class ChatModel implements IChatModel, IMessageHandler {

    /** Mapping from addresses to chatpartners. */
    private HashMap<IPeerID, ChatPartner> chatPartners = new HashMap<IPeerID, ChatPartner>();

    /** The user interface. */
    private IChatGUI userInterface = null;

    /** The user UDP channel. */
    private DatagramChannel udpChan;

    /** The commFacade. */
    private final ICommFacade commFacade;

    /** Thats me [information about my nick, userid]. */
    private ChatPartner myself = null;

    /** The <tt>FileTransferManager</tt> which deals with filetransfers. */
    private FileTransferManager fileManager = null;

    /** The agent which keeps track of who can relay whom. */
    private ChatApplicationCallback relayAgent;

    /** The connection manager. */
    private IConnectionBroker connBroker;

    /** The port on which the UDP socket runs. */
    private int port;

    /** The Bootstrap addresses. */
    private Collection<InetSocketAddress> bootstraps;

    /** The sender of all stay-alives. */
    private StayAliveSender stayAliveSender;

    /** Responsible for determining when guys come online / go offline. */
    private final AvailabilityManager availabilityManager;

    /**
	 * Creates a ChatModel using the given port.
	 * 
	 * @param port
	 *            port for udp socket
	 * @throws BindException
	 */
    public ChatModel(int port) throws BindException {
        this(port, null);
    }

    /**
	 * Creates a ChatModel using the given port which sends join messages to all
	 * given bootstrap addresses.
	 * 
	 * @param port
	 *            port for udp socket
	 * @param bootstraps
	 *            bootstrap addresses
	 * @throws BindException
	 */
    public ChatModel(int port, Collection<InetSocketAddress> bootstraps) throws BindException {
        this.port = port;
        this.bootstraps = bootstraps;
        createMyself();
        relayAgent = new ChatApplicationCallback(myself.getPeerID());
        connBroker = ConnectionManagerFactory.createConnectionBroker(relayAgent);
        commFacade = connBroker.getCommFacade();
        relayAgent.setCommFacade(commFacade);
        stayAliveSender = new StayAliveSender(commFacade);
        createDatagramChannel(port);
        availabilityManager = new AvailabilityManager(this, udpChan);
        registerMessageTypes();
        fileManager = new FileTransferManager(commFacade, port, this);
        connBroker.registerUDPChannel(udpChan);
        Logger.log("INITIALIZED Chat Model: " + getMyself().getPeer());
    }

    private void createMyself() {
        String nickname = "Peer " + new java.util.Random().nextInt(10000);
        InetAddress local = InetAddressUtils.getMostProbableExternalAddress();
        InetSocketAddress localSocketAddr = new InetSocketAddress(local, port);
        myself = new ChatPartner(nickname, PeerIDAdapter.getPeerID(UUID.randomUUID()), localSocketAddr, null);
    }

    /**
	 * This method tries to create the datagram channel and bind it to given port.
	 * @param port port for udp socket
	 * @throws BindException
	 */
    private void createDatagramChannel(int port) throws BindException {
        try {
            udpChan = commFacade.openUDPSocket(port);
        } catch (BindException e) {
            throw e;
        } catch (IOException e) {
            Logger.logError(e, "Could not initialize chat system! Exiting...");
            throw new IllegalStateException("Error during chat system initialization!");
        }
    }

    /**
	 * Here, all message types for the chatmodel are registered.
	 * 
	 * @param port
	 *            port for udp socket
	 * @throws BindException
	 */
    private void registerMessageTypes() {
        IMessageProcessor msgProc = commFacade.getMessageProcessor();
        msgProc.registerMessageHandler(ChatMessage.class, this);
        msgProc.registerMessageHandler(KnownHostsMessage.class, this);
        msgProc.registerMessageHandler(UDPPing.class, this);
        msgProc.registerMessageHandler(UDPPong.class, this);
        msgProc.registerMessageHandler(StayAliveMessage.class, this);
        msgProc.setHighPriorityMessageType(UDPPing.class);
        msgProc.setHighPriorityMessageType(UDPPong.class);
    }

    /**
	 * Sends a join to all given addresses.
	 * @param bootstraps addresses
	 */
    private void sendJoin(Collection<InetSocketAddress> addresses) {
        if (addresses == null) return;
        for (InetSocketAddress addr : addresses) {
            sendJoin(addr);
        }
    }

    /**
	 * Sends a join to a given address.
	 * @param addr address of a possible chatter
	 */
    public void sendJoin(InetSocketAddress addr) {
        availabilityManager.sendJoin(addr);
    }

    /**
	 * Sends a join to a given address, possibly using the ID to establish relayed or punched connections.
	 * @param addr address of a possible chatte, if we already know
	 */
    public void sendJoin(InetSocketAddress addr, IPeerID peerID) {
        if (peerID != null) availabilityManager.sendJoin(new Peer(peerID, addr)); else availabilityManager.sendJoin(addr);
    }

    /**
	 * Sends a join to the given address using the given relay host (this is only possible is we KNOW
	 * the relay host - we do NOT ask the application callback for anything!).
	 * @param address target address
	 * @param relay UDP relay host
	 */
    public void sendJoin(IPeer peer, InetSocketAddress relay) {
        availabilityManager.sendJoin(peer, relay);
    }

    public void setUserInterface(IChatGUI obs) {
        this.userInterface = obs;
    }

    public Collection<IChatPartner> getChatPartners() {
        return new ArrayList<IChatPartner>(chatPartners.values());
    }

    public void sendChatMessage(String content) {
        IPeerID myID = getMyself().getPeerID();
        for (ChatPartner cp : chatPartners.values()) {
            ChatMessage cm = new ChatMessage(myID, cp.getPeerID(), content, false);
            try {
                sendMessage(cp, cm);
            } catch (IOException e) {
                Logger.logError(e, "Could not send chat message to chat partner: " + cp);
            }
        }
    }

    public void sendChatMessage(IChatPartner chatter, String content) {
        IPeerID myID = getMyself().getPeerID();
        ChatMessage cm = new ChatMessage(myID, chatter.getPeerID(), content, true);
        ChatPartner cp = (ChatPartner) chatter;
        try {
            sendMessage(cp, cm);
        } catch (IOException e) {
            Logger.logError(e, "Could not send chat message to chat partner: " + cp);
        }
    }

    public void onMessageReceived(IEnvelope env) {
        if ((env.getMessage() instanceof UDPPing) || (env.getMessage() instanceof UDPPong)) {
            onReceivedPingPong(env);
            return;
        } else if (env.getMessage() instanceof StayAliveMessage) {
            if (((StayAliveMessage) env.getMessage()).needAnswer()) {
                try {
                    IMessage msg = new StayAliveMessage(myself.getPeerID(), env.getSender().getPeerID(), false);
                    commFacade.sendUDPMessage((DatagramChannel) env.getChannel(), msg, env.getSender().getAddress());
                } catch (IOException e) {
                }
            }
            return;
        }
        IMessage msg = env.getMessage();
        if (msg.getSenderPeerID().equals(getMyself().getPeerID())) return;
        if (msg instanceof ChatMessage) {
            onReceivedChatMsg(env, msg);
        } else if (msg instanceof KnownHostsMessage) {
            onReceiveKnownHosts(env, msg);
        }
    }

    public synchronized void addChatPartner(ChatPartner partner) {
        if (chatPartners.containsKey(partner.getPeerID())) return;
        Iterator<ChatPartner> it = chatPartners.values().iterator();
        while (it.hasNext()) {
            ChatPartner cp = it.next();
            if (cp.getPublicAddress().equals(partner.getPublicAddress())) it.remove();
        }
        chatPartners.put(partner.getPeerID(), partner);
        if (userInterface != null) userInterface.onChatterAdded(partner);
        distributeKnownHosts(partner);
    }

    private void onReceiveKnownHosts(IEnvelope env, IMessage msg) {
        KnownHostsMessage khm = (KnownHostsMessage) msg;
        Iterator<HostInfo> it = khm.getHostInfos().iterator();
        while (it.hasNext()) {
            HostInfo info = it.next();
            if (getMyself().getPeerID().equals(info.userID)) continue;
            if (!chatPartners.containsKey(info.userID)) {
                if (info.relayAddress == null) {
                    relayAgent.setRelayHost(info.userID, env.getSender().getAddress());
                } else {
                    relayAgent.setRelayHost(info.userID, info.relayAddress);
                }
                if (info.userID.compareTo(getMyself().getPeerID()) >= 1) {
                    if (getCallback().testUDPConnectivity(udpChan, info.publicAddress)) {
                        sendJoin(info.publicAddress);
                    } else sendJoin(info.publicAddress, info.userID);
                }
            }
        }
    }

    public synchronized void removeChatpartner(IChatPartner cp) {
        if (cp != null) {
            Object key = chatPartners.remove(cp.getPeerID());
            if (userInterface != null && key != null) userInterface.onChatterRemoved(cp);
        }
    }

    private void distributeKnownHosts(ChatPartner cp) {
        sendAllKnownHosts(cp);
        HostInfo hi = new HostInfo();
        hi.publicAddress = cp.getPublicAddress();
        if (cp.getRelayHost() == null) hi.relayAddress = new InetSocketAddress(getMyself().getPeer().getAddress().getAddress(), NatConnector.DEFAULT_PORT); else hi.relayAddress = cp.getRelayHost();
        hi.userID = cp.getPeerID();
        Collection<HostInfo> hosts = new ArrayList<HostInfo>(1);
        hosts.add(hi);
        for (ChatPartner other : chatPartners.values()) {
            if (cp.equals(other)) continue;
            KnownHostsMessage msg = new KnownHostsMessage(getMyself().getPeerID(), cp.getPeerID(), hosts);
            try {
                sendMessage(other, msg);
            } catch (IOException e) {
                Logger.logError(e, "Error sending KnownHostMessage to " + other);
            }
        }
    }

    private void sendAllKnownHosts(ChatPartner cp) {
        KnownHostsMessage khm = new KnownHostsMessage(getMyself().getPeerID(), cp.getPeerID());
        for (ChatPartner known : chatPartners.values()) {
            if (known.getPeerID().equals(cp.getPeerID())) continue;
            HostInfo hi = new HostInfo();
            hi.publicAddress = known.getPublicAddress();
            hi.relayAddress = known.getRelayHost();
            if (hi.relayAddress == null) {
                hi.relayAddress = new InetSocketAddress(getMyself().getPeer().getAddress().getAddress(), NatConnector.DEFAULT_PORT);
            }
            hi.userID = known.getPeerID();
            khm.getHostInfos().add(hi);
        }
        if (khm.getHostInfos().isEmpty()) return;
        try {
            sendMessage(cp, khm);
        } catch (IOException e) {
            Logger.logError(e, "Could not write a KnownHostMessage to " + cp);
        }
    }

    private void onReceivedPingPong(IEnvelope envelope) {
        if (envelope.getMessage() instanceof UDPPong) {
            Logger.log("RECEIVED a pong from " + envelope.getSender() + "!");
            return;
        }
        DatagramChannel dc = (DatagramChannel) envelope.getChannel();
        InetSocketAddress sender = envelope.getSender().getAddress();
        try {
            getCommFacade().sendUDPMessage(dc, new UDPPong(myself.getPeerID(), envelope.getSender().getPeerID()), sender);
        } catch (IOException e) {
            Logger.logError(e, "Error answering to UDPPing of " + envelope.getSender());
        }
        return;
    }

    private void onReceivedChatMsg(IEnvelope env, IMessage msg) {
        ChatMessage cm = (ChatMessage) msg;
        ChatPartner partner = chatPartners.get(cm.getSenderPeerID());
        if (partner == null) return;
        if (userInterface != null) userInterface.onReceivedChatMessage(partner, cm.isPrivate(), cm.getContent());
    }

    public void sendFile(IChatPartner chatter, File file) {
        IPeer peer = chatter.getPeer();
        fileManager.sendFile(peer, file);
    }

    public void sendFile(IChatPartner chatter, File file, InetSocketAddress relayHostAddr) {
        fileManager.sendFile(chatter.getPeer(), file, relayHostAddr);
    }

    /**
	 * Starts the selection process - this means that we start to read 
	 * socket events like read and write data or accept connections.
	 */
    public void startSelectionProcess() {
        commFacade.startSelectionProcess();
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }
        sendJoin(bootstraps);
    }

    /**
	 * Returns the commFacade of the chatModel.
	 * @return the ICommFacade
	 */
    public ICommFacade getCommFacade() {
        return commFacade;
    }

    public File onSendFileRequest(IPeer from, String nickname, String filename, long size) {
        if (userInterface == null) return null;
        Iterator<ChatPartner> it = chatPartners.values().iterator();
        while (it.hasNext()) {
            ChatPartner cp = it.next();
            boolean isOK = true;
            if (!cp.getNickname().equalsIgnoreCase(nickname)) isOK = false;
            if (isOK) return userInterface.onFileSendRequest(cp, filename, size);
        }
        Logger.log("Denied file request because we dont know the requesting peer! [nick+addr]");
        return null;
    }

    public IChatPartner getMyself() {
        return myself;
    }

    public void onStartedFileTransfer(FileTransfer ft) {
        if (userInterface != null) userInterface.onStartedFileTransfer(ft);
    }

    public void waitForUI() {
        while (userInterface == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

    public void shutdown() {
        Iterator<ChatPartner> it = chatPartners.values().iterator();
        while (it.hasNext()) {
            ChatPartner cp = it.next();
            LeaveChatMessage lcm = new LeaveChatMessage(getMyself().getPeerID(), cp.getPeerID());
            try {
                sendMessage(cp, lcm);
            } catch (Exception e) {
            }
        }
        commFacade.shutdown();
    }

    public IChatPartner getChatPartner(String nick) {
        for (ChatPartner cp : chatPartners.values()) {
            if (cp.getNickname().equalsIgnoreCase(nick)) return cp;
        }
        return null;
    }

    public IConnectionBroker getConnectionBroker() {
        return connBroker;
    }

    private void sendMessage(ChatPartner cp, IMessage msg) throws IOException {
        if (!cp.getChannel().isOpen()) {
            Logger.logWarning("Channel to " + cp + " has been closed. Telling UI...");
            removeChatpartner(cp);
            return;
        }
        if (cp.getChannel().isConnected()) {
            commFacade.sendUDPMessage(cp.getChannel(), msg);
        } else {
            commFacade.sendUDPMessage(cp.getChannel(), msg, cp.getUsedAddress());
        }
    }

    public IApplicationCallback getCallback() {
        return relayAgent;
    }

    void addToStayAlive(ChatPartner cp) {
        stayAliveSender.addStayAlive(cp.getChannel(), cp.getUsedAddress());
    }

    IChatPartner getChatPartner(IPeerID id) {
        return chatPartners.get(id);
    }
}
