package webirc.client.commands;

import webirc.client.Channel;
import webirc.client.User;

/**
 * @author Ayzen
 * @version 1.0 19.08.2006 0:32:56
 */
public class KickCommand extends IRCCommand {

    public static String getName() {
        return "KICK";
    }

    private Channel channel;

    private User user;

    private String kickMessage;

    public KickCommand(Channel channel, User user, String partMessage) {
        name = getName();
        params = ' ' + channel.toString() + ' ' + user.toString() + partMessage != null ? " :" + partMessage : "";
    }

    public KickCommand(String prefix, String command, String params) {
        super(prefix, command, params);
        name = getName();
        channel = new Channel(nextParam());
        user = new User(nextParam());
        kickMessage = lastParam();
    }

    public Channel getChannel() {
        return channel;
    }

    public User getUser() {
        return user;
    }

    public String getKickMessage() {
        return kickMessage;
    }
}
