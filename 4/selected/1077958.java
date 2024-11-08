package raptor.chat;

/**
 * A class defining an inbound message received from a server.
 */
public class ChatEvent {

    protected String channel;

    protected String gameId;

    protected String message;

    protected String source;

    protected long time;

    protected ChatType type;

    protected boolean hasSoundBeenHandled;

    public ChatEvent() {
        time = System.currentTimeMillis();
    }

    public ChatEvent(String source, ChatType type, String message) {
        this();
        this.source = source;
        this.type = type;
        this.message = message;
    }

    public ChatEvent(String source, ChatType type, String message, String gameId) {
        this(source, type, message);
        this.gameId = gameId;
    }

    /**
	 * If the chat event represents a channel tell this contains the channel
	 * number. Otherwise it is null.
	 */
    public String getChannel() {
        return channel;
    }

    /**
	 * If the chat event represents a tell about a game, i.e.
	 * kibitz,whisper,etc, this is the game id. Otherwise it is null.
	 * 
	 * @return
	 */
    public String getGameId() {
        return gameId;
    }

    /**
	 * Returns the entire server message.
	 * 
	 * @return Entire message involved in this ChatEvent.
	 */
    public String getMessage() {
        return message;
    }

    /**
	 * This is the source of the ChatEvent. If it is a tell event, it is the
	 * user sending the tell. If it is a channel tell event, it is the user
	 * sending the tell to the channel. If its a shout or c-shout, its the
	 * person shouting. If its a kibitz or whisper, its the person kibitzing or
	 * whispering. If its a say, its the person sending the say.
	 * 
	 * @return The user name of the person involved in this ChatEvent.
	 */
    public String getSource() {
        return source;
    }

    /**
	 * The time in EPOC the chat event was created.
	 * 
	 * @return ChatEvent creation timestamp in EPOC millisconds.
	 */
    public long getTime() {
        return time;
    }

    /**
	 * @return The type of ChatEvent.
	 * @see ChatTypes.
	 */
    public ChatType getType() {
        return type;
    }

    /**
	 * Returns true if this event has been handled.
	 */
    public boolean hasSoundBeenHandled() {
        return hasSoundBeenHandled;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    /**
	 * Sets the flag denoting this event has been handled.
	 */
    public void setHasSoundBeenHandled(boolean hasSoundBeenHandled) {
        this.hasSoundBeenHandled = hasSoundBeenHandled;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
	 * @param time
	 *            The time that this chat event occurred.
	 */
    public void setTime(long time) {
        this.time = time;
    }

    /**
	 * @param type
	 *            The type of ChatEvent this is.
	 * @see ChatTypes.
	 */
    public void setType(ChatType type) {
        this.type = type;
    }

    /**
	 * Dumps information about this ChatEvent to a string.
	 */
    @Override
    public String toString() {
        return "ChatEvent: source=" + source + " type=" + type.name() + " gameId=" + gameId + " channel=" + channel + " message='" + message + "'";
    }
}
