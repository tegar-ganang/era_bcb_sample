package game.network;

import game.core.CoreManagedObjects;
import game.database.player.vo.Player;
import game.systems.Room;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;

/**
 * 
 * @author Michel Montenegro
 *
 */
public class ManagerSessionPlayer extends CoreManagedObjects implements ClientSessionListener {

    private static final long serialVersionUID = -6143746028573880418L;

    private static final Logger logger = Logger.getLogger(ManagerSessionPlayer.class.getName());

    /** The message encoding. */
    public static final String MESSAGE_CHARSET = "UTF-8";

    /** The prefix for player bindings in the {@code DataManager}. */
    protected static final String PLAYER_BIND_PREFIX = "Player.";

    /** The {@code ClientSession} for this player, or null if logged out. */
    private ManagedReference<ClientSession> currentSessionRef = null;

    /** The {@link Room} this player is in, or null if none. */
    private ManagedReference<Room> currentRoomRef = null;

    private ManagedReference<Player> playerRef = null;

    /**
     * Find or create the player object for the given session, and mark
     * the player as logged in on that session.
     *
     * @param session which session to find or create a player for
     * @return a player for the given session
     */
    public static ManagerSessionPlayer loggedIn(ClientSession session) {
        String playerBinding = PLAYER_BIND_PREFIX + session.getName();
        DataManager dataMgr = AppContext.getDataManager();
        ManagerSessionPlayer player;
        try {
            player = (ManagerSessionPlayer) dataMgr.getBinding(playerBinding);
        } catch (NameNotBoundException ex) {
            player = new ManagerSessionPlayer(playerBinding, "");
            logger.log(Level.INFO, "New player created: {0}", player);
            dataMgr.setBinding(playerBinding, player);
        }
        player.setSession(session);
        return player;
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
        if (session != null) {
            DataManager dataMgr = AppContext.getDataManager();
            dataMgr.markForUpdate(this);
            currentSessionRef = dataMgr.createReference(session);
            logger.log(Level.INFO, "Set session for {0} to {1}", new Object[] { this, session });
        }
    }

    /**
     * Handles a player entering a room.
     *
     * @param room the room for this player to enter
     */
    public void enter(Room room, Player player) {
        logger.log(Level.INFO, "{0} enters {1} and {2}", new Object[] { this, room, player });
        room.addPlayerSession(this);
        setRoom(room);
        setPlayer(player);
    }

    /** {@inheritDoc} */
    public void receivedMessage(ByteBuffer message) {
        String command = decodeString(message);
        logger.log(Level.INFO, "{0} received command: {1} " + command, new Object[] { this, command });
        if (command.equalsIgnoreCase("look")) {
            String reply = getRoom().look(this);
            getSession().send(encodeString(reply));
        } else {
            logger.log(Level.WARNING, "{0} unknown command: {1}", new Object[] { this, command });
            disconnected(true);
        }
    }

    /** {@inheritDoc} */
    public void disconnected(boolean graceful) {
        Channel channel = AppContext.getChannelManager().getChannel("map_" + playerRef.get().getMapId());
        channel.send(null, encodeString("m/loggout/" + playerRef.get().getLoginId() + "/"));
        setSession(null);
        logger.log(Level.INFO, "Disconnected: {0}", this);
        getRoom().removePlayer(this);
        setRoom(null);
        playerRef = null;
    }

    /**
     * Returns the room this player is currently in, or {@code null} if
     * this player is not in a room.
     * <p>
     * @return the room this player is currently in, or {@code null}
     */
    protected Room getRoom() {
        if (currentRoomRef == null) {
            return null;
        }
        return currentRoomRef.get();
    }

    /**
     * Sets the room this player is currently in.  If the room given
     * is null, marks the player as not in any room.
     * <p>
     * @param room the room this player should be in, or {@code null}
     */
    protected void setRoom(Room room) {
        DataManager dataManager = AppContext.getDataManager();
        dataManager.markForUpdate(this);
        if (room == null) {
            currentRoomRef = null;
            return;
        }
        currentRoomRef = dataManager.createReference(room);
    }

    protected void setPlayer(Player player) {
        DataManager dataManager = AppContext.getDataManager();
        dataManager.markForUpdate(this);
        if (player == null) {
            currentRoomRef = null;
            return;
        }
        playerRef = dataManager.createReference(player);
    }

    /**
     * Encodes a {@code String} into a {@link ByteBuffer}.
     *
     * @param s the string to encode
     * @return the {@code ByteBuffer} which encodes the given string
     */
    protected static ByteBuffer encodeString(String s) {
        try {
            return ByteBuffer.wrap(s.getBytes(MESSAGE_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new Error("Required character set " + MESSAGE_CHARSET + " not found", e);
        }
    }

    /**
     * Decodes a message into a {@code String}.
     *
     * @param message the message to decode
     * @return the decoded string
     */
    protected static String decodeString(ByteBuffer message) {
        try {
            byte[] bytes = new byte[message.remaining()];
            message.get(bytes);
            return new String(bytes, MESSAGE_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new Error("Required character set " + MESSAGE_CHARSET + " not found", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getObjectName());
        buf.append('@');
        if (getSession() == null) {
            buf.append("null");
        } else {
            buf.append(currentSessionRef.getId());
        }
        return buf.toString();
    }

    private int tempPosX;

    private int tempPosY;

    public int getTempPosX() {
        return tempPosX;
    }

    public void setTempPosX(int tempPosX) {
        this.tempPosX = tempPosX;
    }

    public int getTempPosY() {
        return tempPosY;
    }

    public void setTempPosY(int tempPosY) {
        this.tempPosY = tempPosY;
    }

    /**
     * Creates a new {@code Player} with the given name.
     *
     * @param name the name of this player
     */
    public ManagerSessionPlayer(String objetcName, String objectDescription) {
        super(objetcName, objectDescription);
    }

    public ManagedReference<Player> getPlayerRef() {
        return playerRef;
    }

    public void setPlayerRef(ManagedReference<Player> playerRef) {
        this.playerRef = playerRef;
    }
}
