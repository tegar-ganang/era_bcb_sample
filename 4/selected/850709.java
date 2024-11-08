package ru.beta2.testyard.engine.points;

/**
 * User: Inc
 * Date: 19.06.2008
 * Time: 6:18:16
 */
public class MessageEvent extends PlayerEvent {

    public static final String MESSAGE_FROM_SESSION = "messageFromSession";

    public static final String MESSAGE_FROM_CHANNEL = "messageFromChannel";

    private final String channel;

    private final Object message;

    public MessageEvent(int player, Object message) {
        super(MESSAGE_FROM_SESSION, player);
        this.message = message;
        channel = null;
    }

    public MessageEvent(int player, String channel, Object message) {
        super(MESSAGE_FROM_CHANNEL, player);
        this.channel = channel;
        this.message = message;
    }

    public String getChannel() {
        return channel;
    }

    public Object getMessage() {
        return message;
    }
}
