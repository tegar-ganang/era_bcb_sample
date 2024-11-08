package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_NOCHANMODES messages.
 * <p>
 * <b>Syntax:</b> {@code 477 &lt;channel&gt; "Channel doesn't support modes"}
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: NochanmodesError.java 96 2006-09-13 22:20:24Z captainnuss $
 */
public class NochanmodesError {

    /**
     * Instantiation is not allowed.
     */
    private NochanmodesError() {
    }

    /**
     * Creates a new ERR_NOCHANMODES message.
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
        String[] args = new String[] { channel, "Channel doesn't support modes" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_NOCHANMODES, args);
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
