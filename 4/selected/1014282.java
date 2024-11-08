package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of ERR_BADCHANMASK messages.
 * <p>
 * <b>Syntax:</b> <code>476 &lt;channel&gt; "Bad Channel Mask"</code>
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: BadchanmaskError.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class BadchanmaskError {

    /**
     * Instantiation is not allowed.
     */
    private BadchanmaskError() {
    }

    /**
     * Creates a new ERR_BADCHANMASK message.
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
        String[] args = new String[] { channel, "Bad Channel Mask" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_BADCHANMASK, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(0);
    }
}
