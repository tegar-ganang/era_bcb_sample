package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of MSG_TOPIC messages.
 * <p>
 * <b>Syntax:</b> {@code TOPIC &lt;channel&gt; [ &lt;topic&gt; ]}
 * <p>
 * The TOPIC command is used to change or view the topic of a channel. The topic
 * for channel {@code channel} is returned if there is no
 * {@code topic} given. If the {@code topic} parameter is present, the
 * topic for that channel will be changed, if this action is allowed for the
 * user requesting it. If the {@code topic} parameter is an empty string,
 * the topic for that channel will be removed.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: TopicMessage.java 91 2006-07-21 13:41:43Z captainnuss $
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
        return msg.getArgs().get(0);
    }

    /**
     * Returns the topic, if any.
     *
     * @return String containing the topic or "" if none is given (or,
     *         certainly, if the topic is "").
     */
    public static String getTopic(IRCMessage msg) {
        try {
            return msg.getArgs().get(1);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
