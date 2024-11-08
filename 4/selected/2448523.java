package genericirc.irccore;

import java.util.EventObject;

/**
 * Contains information about incoming messages
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-28
 */
public class NewMessageEvent extends EventObject {

    private String hostmask;

    private String user;

    private String channel;

    private String message;

    /**
     * Creates a new NewMessageEvent object
     * @param source The source of this event
     * @param hostmask The hostmask of the user
     * @param user The user that sent the message
     * @param channel The channel the message was sent to. This can be the user's
     * name if the message comes from a query
     * @param message The actual message sent
     */
    public NewMessageEvent(Object source, String hostmask, String user, String channel, String message) {
        super(source);
        this.hostmask = hostmask;
        this.user = user;
        this.channel = channel;
        this.message = message;
    }

    /**
     * @return the hostmask
     */
    public String getHostmask() {
        return hostmask;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
}
