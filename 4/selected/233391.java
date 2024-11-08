package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of RPL_UNIQOPIS messages.
 * <p>
 * <b>Syntax:</b> {@code 325 &lt;channel&gt; &lt;nickname&gt;}
 * <p>
 * No documentation available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: UniqopisReply.java 91 2006-07-21 13:41:43Z captainnuss $
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
        return msg.getArgs().get(msg.getArgs().size() - 2);
    }

    /**
     * Returns the nick name.
     *
     * @return String containing the nick name.
     */
    public static String getNickname(IRCMessage msg) {
        return msg.getArgs().get(msg.getArgs().size() - 1);
    }
}
