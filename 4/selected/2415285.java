package genericirc.irccore;

import java.util.EventObject;

/**
 * Contains information about a user joining a channel
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-28
 */
public class UserJoinedEvent extends EventObject {

    private String user;

    private String hostmask;

    private String channel;

    /**
     * Creates a new UserJoinedEvent object
     * @param source The source of the event
     * @param user The user that is joining
     * @param hostmask The hostmask of the user
     * @param channel The channel the user is joining
     */
    public UserJoinedEvent(Object source, String user, String hostmask, String channel) {
        super(source);
        this.user = user;
        this.hostmask = hostmask;
        this.channel = channel;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the hostmask
     */
    public String getHostmask() {
        return hostmask;
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }
}
