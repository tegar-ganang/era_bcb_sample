package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of RPL_INVITELIST messages.
 * <p>
 * <b>Syntax:</b> <code>346 &lt;channel&gt; &lt;invitemask&gt;</code>
 * <p>
 * When listing the <i>invitations masks</i> for a given channel, a server is
 * required to send the list back using the RPL_INVITELIST and
 * RPL_ENDOFINVITELIST messages. A separate RPL_INVITELIST is sent for each
 * active mask. After the masks have been listed (or if none present) a
 * RPL_ENDOFINVITELIST <b>must</b> be sent.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: InvitelistReply.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class InvitelistReply {

    /**
     * Instantiation is not allowed.
     */
    private InvitelistReply() {
    }

    /**
     * Creates a new RPL_INVITELIST message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channel    String containing the channel name.
     * @param invitemask String containing the invite mask.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String invitemask) {
        String[] args = new String[] { channel, invitemask };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_INVITELIST, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 2);
    }

    /**
     * Returns the invite mask.
     *
     * @return String containing the invite mask.
     */
    public static String getInvitemask(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 1);
    }
}
