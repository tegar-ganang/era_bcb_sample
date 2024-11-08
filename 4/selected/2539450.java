package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of RPL_LUSERCHANNELS messages.
 * <p>
 * <b>Syntax:</b> <code>254 &lt;integer&gt; "channels formed"</code>
 * <p>
 * In processing an LUSERS message, the server sends a set of replies from
 * RPL_LUSERCLIENT, RPL_LUSEROP, RPL_USERUNKNOWN, RPL_LUSERCHANNELS and
 * RPL_LUSERME. When replying, a server <b>must</b> send back RPL_LUSERCLIENT
 * and RPL_LUSERME. The other replies are only sent back if a non-zero count is
 * found for them.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: LuserchannelsReply.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class LuserchannelsReply {

    /**
     * Instantiation is not allowed.
     */
    private LuserchannelsReply() {
    }

    /**
     * Creates a new RPL_LUSERCHANNELS message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channels   <code>int</code> containing the number of channels.
     *                   Must be greater then or equal to 0.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, int channels) {
        if (channels < 0) channels = 0;
        String[] args = new String[] { String.valueOf(channels), "channels formed" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_LUSERCHANNELS, args);
    }

    /**
     * Returns the number of channels.
     *
     * @return <code>int</code> specifying the number of channels.
     */
    public static int getChannels(IRCMessage msg) {
        try {
            return Integer.parseInt((String) msg.getArgs().elementAt(msg.getArgs().size() - 2));
        } catch (NumberFormatException f) {
            return 0;
        }
    }
}