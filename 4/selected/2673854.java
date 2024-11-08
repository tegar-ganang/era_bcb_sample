package de.teamwork.irc.msgutils;

import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of MSG_TOPIC messages.
 * <p>
 * <b>Syntax:</b> <code>TOPIC &lt;channel&gt; [ &lt;topic&gt; ]</code>
 * <p>
 * The TOPIC command is used to change or view the topic of a channel. The topic
 * for channel <code>channel</code> is returned if there is no
 * <code>topic</code> given. If the <code>topic</code> parameter is present, the
 * topic for that channel will be changed, if this action is allowed for the
 * user requesting it. If the <code>topic</code> parameter is an empty string,
 * the topic for that channel will be removed.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: TopicMessage.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class TopicMessage {

    /**
     * Instantiation is not allowed.
     */
    private TopicMessage() {
    }

    /**
     * Creates a new MSG_TOPIC message with the supplied information.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param channel String containing the channel name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel) {
        String[] args = new String[] { channel };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_TOPIC, args);
    }

    /**
     * Creates a new MSG_TOPIC message with the supplied information.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param channel String containing the channel name.
     * @param topic   String containing the new topic.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String topic) {
        String[] args = new String[] { channel, topic };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_TOPIC, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(0);
    }

    /**
     * Returns the topic, if any.
     *
     * @return String containing the topic or "" if none is given (or,
     *         certainly, if the topic is "").
     */
    public static String getTopic(IRCMessage msg) {
        try {
            return (String) msg.getArgs().elementAt(1);
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }
}
