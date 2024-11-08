package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of ERR_CHANOPRIVSNEEDED messages.
 * <p>
 * <b>Syntax:</b> <code>482 &lt;channel&gt; "You�re not channel operator"</code>
 * <p>
 * Any command requiring <i>chanop</i> privileges (such as MODE messages)
 * <b>must</b> return this error if the client making the attempt is not a
 * chanop on the specified channel.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: ChanoprivsneededError.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class ChanoprivsneededError {

    /**
     * Instantiation is not allowed.
     */
    private ChanoprivsneededError() {
    }

    /**
     * Creates a new ERR_CHANOPRIVSNEEDED message.
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
        String[] args = new String[] { channel, "You�re not channel operator" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_CHANOPRIVSNEEDED, args);
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
