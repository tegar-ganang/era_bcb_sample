package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_TOOMANYCHANNELS messages.
 * <p>
 * <b>Syntax:</b> {@code 405 &lt;channel name&gt; "You have joined too many channels"}
 * <p>
 * Sent to a user when they have joined the maximum number of allowed channels
 * and they try to join another channel.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: ToomanychannelsError.java 91 2006-07-21 13:41:43Z captainnuss $
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
        return msg.getArgs().get(0);
    }
}
