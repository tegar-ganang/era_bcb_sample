package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of ERR_BANNEDFROMCHAN messages.
 * <p>
 * <b>Syntax:</b> <code>474 &lt;channel&gt; "Cannot join channel (+b)"</code>
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: BannedfromchanError.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class BannedfromchanError {

    /**
     * Instantiation is not allowed.
     */
    private BannedfromchanError() {
    }

    /**
     * Creates a new ERR_BANNEDFROMCHAN message.
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
        String[] args = new String[] { channel, "Cannot join channel (+b)" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_BANNEDFROMCHAN, args);
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
