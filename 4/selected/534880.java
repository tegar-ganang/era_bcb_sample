package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_BADCHANNELKEY messages.
 * <p>
 * <b>Syntax:</b> {@code 475 &lt;channel&gt; "Cannot join channel (+k)"}
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: BadchannelkeyError.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class BadchannelkeyError {

    /**
     * Instantiation is not allowed.
     */
    private BadchannelkeyError() {
    }

    /**
     * Creates a new ERR_BADCHANNELKEY message.
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
        String[] args = new String[] { channel, "Cannot join channel (+k)" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_BADCHANNELKEY, args);
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
