package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of MSG_INVITE messages.
 * <p>
 * <b>Syntax:</b> {@code &lt;nickname&gt; &lt;channel&gt;}
 * <p>
 * The INVITE command is used to invite a user to a channel. The parameter
 * {@code nickname} is the nickname of the person to be invited to the
 * target channel {@code channel}. There is no requirement that the channel
 * the target user is being invited to must exist or be a valid channel.
 * However, if the channel exists, only members of the channel are allowed to
 * invite other users. When the channel has invite-only flag set, only channel
 * operators may issue INVITE command.
 * <p>
 * Only the user inviting and the user being invited will receive notification
 * of the invitation. Other channel members are not notified. (This is unlike
 * the MODE changes, and is occasionally the source of trouble for users.)
 *
 * @author Christoph Daniel Schulze
 * @version $Id: InviteMessage.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class InviteMessage {

    /**
     * Instantiation is not allowed.
     */
    private InviteMessage() {
    }

    /**
     * Creates a new MSG_INVITE message with the supplied information.
     *
     * @param msgNick   String object containing the nick of the guy this
     *                  message comes from. Should usually be "".
     * @param msgUser   String object containing the user name of the guy this
     *                  message comes from. Should usually be "".
     * @param msgHost   String object containing the host name of the guy this
     *                  message comes from. Should usually be "".
     * @param nickname  String object containing the nick name.
     * @param channel   String object containing the channel.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String nickname, String channel) {
        String[] args = new String[] { nickname, channel };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_INVITE, args);
    }

    /**
     * Returns the nick name.
     *
     * @return String containing the nick name.
     */
    public static String getNickname(IRCMessage msg) {
        return msg.getArgs().get(0);
    }

    /**
     * Returns the channel.
     *
     * @return String containing the channel.
     */
    public static String getChannel(IRCMessage msg) {
        return msg.getArgs().get(1);
    }
}
