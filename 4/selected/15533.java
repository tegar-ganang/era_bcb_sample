package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of ERR_KEYSET messages.
 * <p>
 * <b>Syntax:</b> <code>467 &lt;channel&gt; "Channel key already set"</code>
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: KeysetError.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class KeysetError {

    /**
     * Instantiation is not allowed.
     */
    private KeysetError() {
    }

    /**
     * Creates a new ERR_KEYSET message.
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
        String[] args = new String[] { channel, "Channel key already set" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_KEYSET, args);
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
