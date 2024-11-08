package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of RPL_ENDOFBANLIST messages.
 * <p>
 * <b>Syntax:</b> {@code 368 &lt;channel&gt; "End of channel ban list"}
 * <p>
 * When listing the active <i>bans</i> for a given channel, a server is required
 * to send the list back using the RPL_BANLIST and RPL_ENDOFBANLIST messages. A
 * separate RPL_BANLIST is sent for each active banmask. After the banmasks have
 * been listed (or if none present) a RPL_ENDOFBANLIST <b>must</b> be sent.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: EndofbanlistReply.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class EndofbanlistReply {

    /**
     * Instantiation is not allowed.
     */
    private EndofbanlistReply() {
    }

    /**
     * Creates a new RPL_ENDOFBANLIST message.
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
        String[] args = new String[] { channel, "End of channel invite list" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_ENDOFBANLIST, args);
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
