package com.usoog.commons.gamecore.message;

import com.usoog.commons.network.message.AbstractMessage;

/**
 * When a user joins a game this message tells the user what the state of the game
 * is, and what the role of the user is (player, observer, etc).
 *
 * @author Jimmy Axenhus
 * @author Hylke van der Schaaf
 */
public class MessageUserInGame extends AbstractMessage {

    /**
	 * The unique KEY for this Message.
	 */
    public static final String KEY = "UIG";

    /**
	 * The game id of the game the user joined.
	 */
    private int gameId;

    /**
	 * The Player id the user has in this game.
	 */
    private int playerId;

    /**
	 * Indicating if the game is currently running.
	 */
    private boolean running;

    /**
	 * The last tick the game has seen.
	 */
    private int lastTick;

    /**
	 * Default Constructor. Not to be called by the user.
	 */
    public MessageUserInGame() {
        super(KEY);
    }

    /**
	 * Constructor with all the required arguments.
	 *
	 * @param gameId The game the user joined.
	 * @param playerId The PlayerId the user has in this game.
	 * @param running If the game is running.
	 * @param lastTick The last tick the game has seen.
	 */
    public MessageUserInGame(int gameId, int playerId, boolean running, int lastTick) {
        super(KEY);
        this.gameId = gameId;
        this.playerId = playerId;
        this.running = running;
        this.lastTick = lastTick;
    }

    /**
	 * Method to get the Channel id.
	 *
	 * @return The channel id.
	 */
    public int getChannelId() {
        return gameId;
    }

    /**
	 * Method to get the Player id.
	 *
	 * @return The Player id.
	 */
    public int getPlayerId() {
        return playerId;
    }

    /**
	 * Method to get the last tick.
	 *
	 * @return The last tick.
	 */
    public int getLastTick() {
        return lastTick;
    }

    /**
	 * Method to see if the game is running.
	 *
	 * @return True if the game is running.
	 */
    public boolean isRunning() {
        return running;
    }

    @Override
    public String getMessage() {
        return formatMessage(gameId, playerId, running, lastTick);
    }

    @Override
    public void initFromString(String message) {
        String[] parts = message.split(" ");
        gameId = Integer.parseInt(parts[1]);
        playerId = Integer.parseInt(parts[2]);
        if ("r".equalsIgnoreCase(parts[3])) {
            running = true;
        }
        lastTick = Integer.parseInt(parts[4]);
    }

    /**
	 * Create a message string while avoiding object creation. Since this
	 * message is never stored any way, we don't need to create an object on the
	 * server for it.
	 *
	 * @param gameId The game the user just joined.
	 * @param playerId The PlayerId the user has in this game.
	 * @param running Whether the game is already running or not.
	 * @param lastTick The last tick the running game has seen.
	 * @return The formatted message.
	 */
    public static String formatMessage(int gameId, int playerId, boolean running, int lastTick) {
        StringBuilder s = new StringBuilder(KEY);
        s.append(" ");
        s.append(gameId);
        s.append(" ");
        s.append(playerId);
        s.append(" ");
        if (running) {
            s.append("r");
        } else {
            s.append("s");
        }
        s.append(" ");
        s.append(lastTick);
        return s.toString();
    }
}
