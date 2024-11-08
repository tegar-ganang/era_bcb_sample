package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_INVITEONLYCHAN messages.
 * <p>
 * <b>Syntax:</b> {@code 473 &lt;channel&gt; "Cannot join channel (+i)"}
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: InviteonlychanError.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class InviteonlychanError {

    /**
     * Instantiation is not allowed.
     */
    private InviteonlychanError() {
    }

    /**
     * Creates a new ERR_INVITEONLYCHAN message.
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
        String[] args = new String[] { channel, "Cannot join channel (+i)" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_INVITEONLYCHAN, args);
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
