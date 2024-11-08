package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of RPL_ENDOFNAMES messages.
 * <p>
 * <b>Syntax:</b> {@code 366 &lt;channel&gt; "End of NAMES list"}
 * <p>
 * To reply to a NAMES message, a reply pair consisting of RPL_NAMREPLY and
 * RPL_ENDOFNAMES is sent by the server back to the client. If there is no
 * channel found as in the query, then only RPL_ENDOFNAMES is returned. The
 * exception to this is when a NAMES message is sent with no parameters and all
 * visible channels and contents are sent back in a series of RPL_NAMEREPLY
 * messages with a RPL_ENDOFNAMES to mark the end.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: EndofnamesReply.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class EndofnamesReply {

    /**
     * Instantiation is not allowed.
     */
    private EndofnamesReply() {
    }

    /**
     * Creates a new RPL_ENDOFNAMES message.
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
        String[] args = new String[] { channel, "End of NAMES list" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_ENDOFNAMES, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return msg.getArgs().get(msg.getArgs().size() - 2);
    }
}
