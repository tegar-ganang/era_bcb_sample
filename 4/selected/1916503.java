package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of RPL_ENDOFEXCEPTLIST messages.
 * <p>
 * <b>Syntax:</b> {@code 349 &lt;channel&gt; "End of channel exception list"}
 * <p>
 * When listing the <i>exception masks</i> for a given channel, a server is
 * required to send the list back using the RPL_EXCEPTLIST and
 * RPL_ENDOFEXCEPTLIST messages. A separate RPL_EXCEPTLIST is sent for each
 * active mask. After the masks have been listed (or if none present) a
 * RPL_ENDOFEXCEPTLIST <b>must</b> be sent.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: EndofexceptlistReply.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class EndofexceptlistReply {

    /**
     * Instantiation is not allowed.
     */
    private EndofexceptlistReply() {
    }

    /**
     * Creates a new RPL_ENDOFEXCEPTLIST message.
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
        String[] args = new String[] { channel, "End of channel exception list" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_ENDOFEXCEPTLIST, args);
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