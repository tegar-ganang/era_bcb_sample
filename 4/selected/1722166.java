package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_NOSUCHCHANNEL messages.
 * <p>
 * <b>Syntax:</b> {@code 403 &lt;channel name&gt; "No such channel"}
 * <p>
 * Used to indicate the given channel name is invalid.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: NosuchchannelError.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class NosuchchannelError {

    /**
     * Instantiation is not allowed.
     */
    private NosuchchannelError() {
    }

    /**
     * Creates a new ERR_NOSUCHCHANNEL message.
     *
     * @param msgNick     String object containing the nick of the guy this
     *                    message comes from. Should usually be "".
     * @param msgUser     String object containing the user name of the guy this
     *                    message comes from. Should usually be "".
     * @param msgHost     String object containing the host name of the guy this
     *                    message comes from. Should usually be "".
     * @param channelname String containing the channel name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channelname) {
        String[] args = new String[] { channelname, "No such channel" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_NOSUCHCHANNEL, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannelname(IRCMessage msg) {
        return msg.getArgs().get(0);
    }
}
