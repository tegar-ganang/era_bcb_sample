package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_NOTONCHANNEL messages.
 * <p>
 * <b>Syntax:</b> {@code 442 &lt;channel&gt; "You're not on that channel"}
 * <p>
 * Returned by the server whenever a client tries to perform a channel affecting
 * command for which the client isn�t a member.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: NotonchannelError.java 96 2006-09-13 22:20:24Z captainnuss $
 */
public class NotonchannelError {

    /**
     * Instantiation is not allowed.
     */
    private NotonchannelError() {
    }

    /**
     * Creates a new ERR_NOTONCHANNEL message.
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
        String[] args = new String[] { channel, "You're not on that channel" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_NOTONCHANNEL, args);
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
