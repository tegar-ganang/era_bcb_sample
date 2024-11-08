package webirc.client.commands;

import webirc.client.Channel;

/**
 * @author Ayzen
 * @version 1.0 19.07.2006 12:28:05
 */
public class JoinCommand extends IRCCommand {

    public static String getName() {
        return "JOIN";
    }

    private Channel channel;

    public JoinCommand(String channels) {
        name = getName();
        params = ' ' + channels;
    }

    public JoinCommand(Channel channel) {
        name = getName();
        params = ' ' + channel.toString();
    }

    public JoinCommand(Channel channel, String key) {
        name = getName();
        params = ' ' + channel.toString() + ' ' + key;
    }

    public JoinCommand(String prefix, String command, String params) {
        super(prefix, command, params);
        name = getName();
        channel = new Channel(lastParam());
    }

    public Channel getChannel() {
        return channel;
    }
}
