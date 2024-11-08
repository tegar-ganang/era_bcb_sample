package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of ERR_TOOMANYCHANNELS messages.
 * <p>
 * <b>Syntax:</b> <code>405 &lt;channel name&gt; "You have joined too many channels"</code>
 * <p>
 * Sent to a user when they have joined the maximum number of allowed channels
 * and they try to join another channel.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: ToomanychannelsError.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class ToomanychannelsError {

    /**
     * Instantiation is not allowed.
     */
    private ToomanychannelsError() {
    }

    /**
     * Creates a new ERR_TOOMANYCHANNELS message.
     *
     * @param msgNick     String object containing the nick of the guy this
     *                    message comes from. Should usually be "".
     * @param msgUser     String object containing the user name of the guy this
     *                    message comes from. Should usually be "".
     * @param msgHost     String object containing the host name of the guy this
     *                    message comes from. Should usually be "".
     * @param channelname String containing the channel name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channelname) {
        String[] args = new String[] { channelname, "You have joined too many channels" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_TOOMANYCHANNELS, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannelname(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(0);
    }
}
