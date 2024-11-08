package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of RPL_UNIQOPIS messages.
 * <p>
 * <b>Syntax:</b> <code>325 &lt;channel&gt; &lt;nickname&gt;</code>
 * <p>
 * No documentation available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: UniqopisReply.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class UniqopisReply {

    /**
     * Instantiation is not allowed.
     */
    private UniqopisReply() {
    }

    /**
     * Creates a new RPL_UNIQOPIS message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channel    String containing the channel name.
     * @param nickname   String containing the nick name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String nickname) {
        String[] args = new String[] { channel, nickname };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_UNIQOPIS, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 2);
    }

    /**
     * Returns the nick name.
     *
     * @return String containing the nick name.
     */
    public static String getNickname(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 1);
    }
}
