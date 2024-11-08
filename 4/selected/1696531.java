package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_USERNOTINCHANNEL messages.
 * <p>
 * <b>Syntax:</b> {@code 441 &lt;nick&gt; &lt;channel&gt; "They aren't on that channel"}
 * <p>
 * Returned by the server to indicate that the target user of the command is not
 * on the given channel.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: UsernotinchannelError.java 96 2006-09-13 22:20:24Z captainnuss $
 */
public class UsernotinchannelError {

    /**
     * Instantiation is not allowed.
     */
    private UsernotinchannelError() {
    }

    /**
     * Creates a new ERR_USERNOTINCHANNEL message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param nick       String containing the nick name.
     * @param channel    String containing the channel name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String nick, String channel) {
        String[] args = new String[] { nick, channel, "They aren't on that channel" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_USERNOTINCHANNEL, args);
    }

    /**
     * Returns the nick name.
     *
     * @return String containing the nick name.
     */
    public static String getNick(IRCMessage msg) {
        return msg.getArgs().get(0);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return msg.getArgs().get(1);
    }
}
