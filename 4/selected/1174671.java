package server;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import server.factions.PlayableFaction;
import server.gameObjects.GameUniverse;
import server.gameObjects.ServerPlayerShip;
import server.player.Player;
import server.player.PlayerShipRecordHolder;
import server.tasks.CleanChannelTask;
import shared.network.ChatWhisperPacket;
import shared.network.ClientStateTransitionPacket;
import shared.network.JoinChannelPacket;
import shared.network.LeaveChannelPacket;
import shared.network.NetworkProtocol;
import shared.network.SimpleChatPacket;
import shared.network.WhisperSuccessPacket;
import shared.network.ClientStateTransitionPacket.ClientGameState;
import shared.network.NetworkProtocol.PacketType;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;

public class GravitasClientSessionListener implements Serializable, ClientSessionListener {

    private static final long serialVersionUID = 1L;

    private static final String clientSessionPrefix = "session-";

    private final ManagedReference<ClientSession> sessionReference;

    private final ManagedReference<Player> playerReference;

    private final String sessionName;

    public GravitasClientSessionListener(ClientSession session) {
        if (session == null) {
            throw new NullPointerException("null session");
        }
        sessionName = session.getName();
        System.out.println("Creating listener for : " + sessionName);
        DataManager datamanager = AppContext.getDataManager();
        sessionReference = datamanager.createReference(session);
        datamanager.setBinding(clientSessionPrefix + sessionName, session);
        ManagedReference<Player> tryRef = null;
        try {
            tryRef = Player.getPlayerByName(sessionName).getReference();
        } catch (NameNotBoundException e) {
            Player p = new Player(sessionName);
            tryRef = p.register();
        } finally {
            playerReference = tryRef;
        }
        playerReference.get().logon(sessionReference);
    }

    public static ClientSession getSessionByName(String name) {
        ClientSession ret;
        try {
            ret = (ClientSession) AppContext.getDataManager().getBinding(clientSessionPrefix + name);
        } catch (NameNotBoundException e) {
            ret = null;
        }
        return ret;
    }

    public ClientSession getSession() {
        return sessionReference.get();
    }

    public ManagedReference<Player> getPlayerReference() {
        return playerReference;
    }

    public void disconnected(boolean graceful) {
        System.out.println("Client " + sessionName + " has disconnected" + (graceful ? "." : " disgracefully."));
        playerReference.get().disconnect();
        AppContext.getDataManager().removeBinding(clientSessionPrefix + sessionName);
    }

    public void receivedMessage(ByteBuffer message) {
        PacketType packetType = NetworkProtocol.getPacketHeader(message);
        switch(packetType) {
            case JOIN_CHANNEL:
                {
                    ChannelManager channelManager = AppContext.getChannelManager();
                    String channelName = NetworkProtocol.chatChannelPrefix + JoinChannelPacket.decode(message);
                    try {
                        channelManager.getChannel(channelName).join(getSession());
                        System.out.println("Moved user into existing channel " + channelName);
                    } catch (NameNotBoundException e) {
                        channelManager.createChannel(channelName, new ChatChannelListener(), Delivery.UNRELIABLE).join(getSession());
                        System.out.println("Created new channel " + channelName);
                    }
                    break;
                }
            case LEAVE_CHANNEL:
                {
                    ChannelManager channelManager = AppContext.getChannelManager();
                    String channelName = NetworkProtocol.chatChannelPrefix + LeaveChannelPacket.decode(message);
                    try {
                        Channel channel = channelManager.getChannel(channelName);
                        channel.leave(getSession());
                        AppContext.getTaskManager().scheduleTask(new CleanChannelTask(channelName), 500);
                    } catch (NameNotBoundException e) {
                    }
                    break;
                }
            case WHISPER_CHAT:
                {
                    String[] contents = ChatWhisperPacket.decode(message);
                    String targetName = contents[0];
                    String messageContents = contents[1];
                    ClientSession targetSession = getSessionByName(targetName);
                    if (targetSession == null) getSession().send(SimpleChatPacket.create("Player " + targetName + " not found.")); else {
                        getSession().send(WhisperSuccessPacket.create(targetName, messageContents));
                        targetSession.send(ChatWhisperPacket.create(sessionName, messageContents));
                    }
                    break;
                }
            case PLAYER_INPUT:
                {
                    playerReference.get().getGameInputRef().getForUpdate().setByPacket(message);
                    break;
                }
            case CLIENT_STATE:
                {
                    ClientGameState currState = playerReference.get().getGameState();
                    if (currState == ClientGameState.FACTION_SELECT) {
                        ClientGameState reportedState = ClientStateTransitionPacket.decode(message);
                        if (reportedState != ClientGameState.FACTION_SELECT) throw new IllegalStateException("State Reported By Client does not Match Server Expectation");
                        byte factionSelect = ClientStateTransitionPacket.decodePBByteSelection(message);
                        ArrayList<ManagedReference<? extends PlayableFaction>> factionList = GameUniverse.getReadOnly().getPlayableFactions();
                        Player player = playerReference.getForUpdate();
                        player.setFaction(factionList.get(factionSelect));
                        player.transitionToShipSelection();
                    } else if (currState == ClientGameState.SHIP_SELECT || currState == ClientGameState.FRAGGED) {
                        ClientGameState reportedState = ClientStateTransitionPacket.decode(message);
                        if (reportedState != currState) throw new IllegalStateException("State Reported By Client does not Match Server Expectation");
                        byte shipSelect = ClientStateTransitionPacket.decodePBByteSelection(message);
                        Player player = playerReference.getForUpdate();
                        PlayerShipRecordHolder record = player.getShipRecords();
                        AppContext.getDataManager().markForUpdate(record);
                        ServerPlayerShip ship = player.getFaction().makeShipBySelection(shipSelect, playerReference);
                        record.associateShipWithRecord(ship);
                        player.transitionToInGame();
                    }
                    break;
                }
            case SERIAL_UNIVERSE:
                {
                    ArrayList<ByteBuffer> toSend = GameUniverse.getReadOnly().makeUniverseInformationPackets();
                    ClientSession session = getSession();
                    for (ByteBuffer b : toSend) session.send(b);
                    break;
                }
            case SERVER_DEBUG:
                {
                    break;
                }
            default:
                break;
        }
    }
}
