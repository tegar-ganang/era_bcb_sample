package genericirc.irccore;

import java.util.EventObject;

/**
 * Contains information about a parting user
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-28
 */
public class UserPartedEvent extends EventObject {

    private String user;

    private String message;

    private String channel;

    /**
     * Creates a new UserPartedEvent object
     * @param source The source of the event
     * @param user The user that is parting
     * @param message The part message, if there is one
     * @param channel The channel the user parted from
     */
    public UserPartedEvent(Object source, String user, String message, String channel) {
        super(source);
        this.user = user;
        this.message = message;
        this.channel = channel;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }
}
