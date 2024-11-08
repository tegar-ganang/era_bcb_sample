package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_KEYSET messages.
 * <p>
 * <b>Syntax:</b> {@code 467 &lt;channel&gt; "Channel key already set"}
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: KeysetError.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class KeysetError {

    /**
     * Instantiation is not allowed.
     */
    private KeysetError() {
    }

    /**
     * Creates a new ERR_KEYSET message.
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
        String[] args = new String[] { channel, "Channel key already set" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_KEYSET, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return msg.getArgs().get(0);
    }
}
