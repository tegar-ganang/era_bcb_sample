import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;

/**
 * Player info kept in the server.
 */
public class ServerPlayer implements Serializable, ManagedObject, ClientSessionListener {

    private static final long serialVersionUID = 1L;

    /**
     * The {@code ClientSession} for this player, or null if logged out.
     */
    private ManagedReference<ClientSession> currentSessionRef = null;

    /**
     * Logging/trace.
     */
    private static final Logger log = Logger.getLogger(ServerPlayer.class.getName());

    private String id;

    private float x;

    private float y;

    private float z;

    private float orientation;

    private float red;

    private float green;

    private float blue;

    public static ServerPlayer loggedIn(ClientSession session) {
        String playerId = session.getName();
        DataManager dataMgr = AppContext.getDataManager();
        ServerPlayer player;
        try {
            player = (ServerPlayer) dataMgr.getBinding(playerId);
        } catch (NameNotBoundException e) {
            float red = RandomUtil.randColorVal();
            float green = RandomUtil.randColorVal();
            float blue = RandomUtil.randColorVal();
            player = new ServerPlayer(playerId, 0.0f, 0.0f, 0.0f, 0.0f, red, green, blue);
            dataMgr.setBinding(playerId, player);
            try {
                PlayersList playerList = (PlayersList) dataMgr.getBinding(MundojavaServer.PLAYERS_LIST_NAME);
                playerList.add(player);
            } catch (NameNotBoundException e1) {
                log.log(Level.SEVERE, "The players list does not exist.", e1);
            } catch (IOException e1) {
                log.log(Level.SEVERE, "Error unpacking the message", e1);
            }
        }
        player.setSession(session);
        ChannelManager channelMgr = AppContext.getChannelManager();
        Channel baseChannel = channelMgr.getChannel(MundojavaServer.BASE_CHANNEL_NAME);
        baseChannel.join(session);
        try {
            Event event = new Event(Event.PLAYER_CREATED, "id", player.getId(), "x", player.getX(), "y", player.getY(), "z", player.getZ(), "orientation", player.getOrientation(), "red", player.getRed(), "green", player.getGreen(), "blue", player.getBlue());
            baseChannel.send(null, event.toByteBuffer());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not sent the PLAYER_CREATED event", e);
        }
        return player;
    }

    private ServerPlayer(String id, float x, float y, float z, float orientation, float red, float green, float blue) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.orientation = orientation;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /**
     * {@inheritDoc}
     */
    public void receivedMessage(ByteBuffer message) {
        try {
            Event event = Event.read(message);
            int eventType = event.type();
            log.log(Level.INFO, "Request from {0}: {1}", new Object[] { id, eventType });
            switch(eventType) {
                case Event.REQUEST_WHERE_AM_I:
                    sendBackMyInfo();
                    break;
                case Event.REQUEST_WHO_IS_THERE:
                    sendBackPlayersInfo();
                    break;
                default:
                    log.log(Level.INFO, "Request from {0} is unknown", new Object[] { id });
                    break;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unexpected Exception", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void disconnected(boolean graceful) {
        setSession(null);
        try {
            ChannelManager channelMgr = AppContext.getChannelManager();
            Channel baseChannel = channelMgr.getChannel(MundojavaServer.BASE_CHANNEL_NAME);
            Event event = new Event(Event.PLAYER_DESTROYED, "id", id);
            baseChannel.send(null, event.toByteBuffer());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not sent the PLAYER_DESTROYED event", e);
        }
        String grace = graceful ? "graceful" : "forced";
        log.log(Level.INFO, "User {0} has logged out {1}", new Object[] { id, grace });
    }

    /**
     * Sends back to the client the player´s info.
     */
    private void sendBackMyInfo() {
        ClientSession session = getSession();
        sendBackPlayerInfo(session, this);
    }

    /**
     * Send to the client the information on all the users.
     */
    private void sendBackPlayersInfo() {
        ClientSession session = getSession();
        DataManager dataMgr = AppContext.getDataManager();
        try {
            PlayersList playerList = (PlayersList) dataMgr.getBinding(MundojavaServer.PLAYERS_LIST_NAME);
            List<ManagedReference<ServerPlayer>> players = playerList.getPlayersRefs();
            for (ManagedReference<ServerPlayer> playerRef : players) {
                ServerPlayer player = playerRef.get();
                if (id.equals(player.getId())) {
                    continue;
                }
                if (player.currentSessionRef != null) {
                    sendBackPlayerInfo(session, playerRef.get());
                }
            }
        } catch (NameNotBoundException e1) {
            log.log(Level.SEVERE, "The players list does not exist.", e1);
        }
    }

    /**
     * Send a players info to the client.
     *
     * @param session Connection to the client.
     * @param player The player info we want to send.
     */
    private void sendBackPlayerInfo(ClientSession session, ServerPlayer player) {
        try {
            Event event = new Event(Event.PLAYER_CREATED, "id", player.getId(), "x", player.getX(), "y", player.getY(), "z", player.getZ(), "orientation", player.getOrientation(), "red", player.getRed(), "green", player.getGreen(), "blue", player.getBlue());
            session.send(event.toByteBuffer());
        } catch (IOException e1) {
            log.log(Level.SEVERE, "Error packing the message", e1);
        }
    }

    /**
     * Returns the session for this listener.
     *
     * @return the session for this listener
     */
    protected ClientSession getSession() {
        if (currentSessionRef == null) {
            return null;
        }
        return currentSessionRef.get();
    }

    /**
     * Mark this player as logged in on the given session.
     *
     * @param session the session this player is logged in on
     */
    protected void setSession(ClientSession session) {
        DataManager dataMgr = AppContext.getDataManager();
        dataMgr.markForUpdate(this);
        if (session == null) {
            currentSessionRef = null;
        } else {
            currentSessionRef = dataMgr.createReference(session);
        }
        log.log(Level.INFO, "Set session for {0} to {1}", new Object[] { this, session });
    }

    public void move(float newX, float newY, float newZ, float newOrientation) {
        x = newX;
        y = newY;
        z = newZ;
        orientation = newOrientation;
    }

    public String getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getOrientation() {
        return orientation;
    }

    public float getRed() {
        return red;
    }

    public float getGreen() {
        return green;
    }

    public float getBlue() {
        return blue;
    }
}
