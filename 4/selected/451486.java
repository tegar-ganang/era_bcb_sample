package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of ERR_CHANNELISFULL messages.
 * <p>
 * <b>Syntax:</b> <code>471 &lt;channel&gt; "Cannot join channel (+l)"</code>
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: ChannelisfullError.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class ChannelisfullError {

    /**
     * Instantiation is not allowed.
     */
    private ChannelisfullError() {
    }

    /**
     * Creates a new ERR_CHANNELISFULL message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channel    String containing the channel name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel) {
        String[] args = new String[] { channel, "Cannot join channel (+l)" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_CHANNELISFULL, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(0);
    }
}
