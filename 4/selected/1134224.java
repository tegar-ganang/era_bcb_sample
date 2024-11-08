package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of RPL_BANLIST messages.
 * <p>
 * <b>Syntax:</b> {@code 367 &lt;channel&gt; &lt;banmask&gt;}
 * <p>
 * When listing the active <i>bans</i> for a given channel, a server is required
 * to send the list back using the RPL_BANLIST and RPL_ENDOFBANLIST messages. A
 * separate RPL_BANLIST is sent for each active banmask. After the banmasks have
 * been listed (or if none present) a RPL_ENDOFBANLIST <b>must</b> be sent.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: BanlistReply.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class BanlistReply {

    /**
     * Instantiation is not allowed.
     */
    private BanlistReply() {
    }

    /**
     * Creates a new RPL_BANLIST message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channel    String containing the channel name.
     * @param banmask    String containing the ban mask.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String banmask) {
        String[] args = new String[] { channel, banmask };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_BANLIST, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return msg.getArgs().get(msg.getArgs().size() - 2);
    }

    /**
     * Returns the ban mask.
     *
     * @return String containing the ban mask.
     */
    public static String getBanmask(IRCMessage msg) {
        return msg.getArgs().get(msg.getArgs().size() - 1);
    }
}
