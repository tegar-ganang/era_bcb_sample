package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of RPL_ENDOFINVITELIST messages.
 * <p>
 * <b>Syntax:</b> <code>347 &lt;channel&gt; "End of channel invite list"</code>
 * <p>
 * When listing the <i>invitations masks</i> for a given channel, a server is
 * required to send the list back using the RPL_INVITELIST and
 * RPL_ENDOFINVITELIST messages. A separate RPL_INVITELIST is sent for each
 * active mask. After the masks have been listed (or if none present) a
 * RPL_ENDOFINVITELIST <b>must</b> be sent.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: EndofinvitelistReply.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class EndofinvitelistReply {

    /**
     * Instantiation is not allowed.
     */
    private EndofinvitelistReply() {
    }

    /**
     * Creates a new RPL_ENDOFINVITELIST message.
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
        String[] args = new String[] { channel, "End of channel invite list" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_ENDOFINVITELIST, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 2);
    }
}
