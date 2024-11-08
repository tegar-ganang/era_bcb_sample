package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of RPL_INVITING messages.
 * <p>
 * <b>Syntax:</b> <code>341 &lt;channel&gt; &lt;nick&gt;</code>
 * <p>
 * Returned by the server to indicate that the attempted INVITE message was
 * successful and is being passed onto the end client.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: InvitingReply.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class InvitingReply {

    /**
     * Instantiation is not allowed.
     */
    private InvitingReply() {
    }

    /**
     * Creates a new RPL_INVITING message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channel    String containing the channel name.
     * @param nick       String containing the nick name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String nick) {
        String[] args = new String[] { channel, nick };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_INVITING, args);
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
    public static String getNick(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 1);
    }
}