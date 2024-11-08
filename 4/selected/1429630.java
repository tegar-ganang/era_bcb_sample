package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_USERONCHANNEL messages.
 * <p>
 * <b>Syntax:</b> {@code 443 &lt;nick&gt; &lt;channel&gt; "is already on channel"}
 * <p>
 * Returned when a client tries to invite a user to a channel they are already
 * on.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: UseronchannelError.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class UseronchannelError {

    /**
     * Instantiation is not allowed.
     */
    private UseronchannelError() {
    }

    /**
     * Creates a new ERR_USERONCHANNEL message.
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
        String[] args = new String[] { nick, channel, "is already on channel" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_USERONCHANNEL, args);
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
