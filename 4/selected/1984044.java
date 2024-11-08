package genericirc.irccore;

import java.util.EventObject;
import java.util.List;

/**
 * Contains a list of users for a channel
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-28
 */
public class UserListEvent extends EventObject {

    private String channel;

    private List<User> userList;

    /**
     * Creates a new UserListEvent object
     * @param source The source of the event
     * @param channel The channel the list belongs to
     * @param userList The list of Users
     */
    public UserListEvent(Object source, String channel, List<User> userList) {
        super(source);
        this.channel = channel;
        this.userList = userList;
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @return the userList
     */
    public List<User> getUserList() {
        return userList;
    }
}
