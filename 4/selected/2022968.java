package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of RPL_TOPIC messages.
 * <p>
 * <b>Syntax:</b> <code>332 &lt;channel&gt; &lt;topic&gt;</code>
 * <p>
 * When sending a TOPIC message to determine the channel topic, one of two
 * replies is sent. If the topic is set, RPL_TOPIC is sent back else
 * RPL_NOTOPIC.
 * <p>
 * <b>Note:</b> Real IRC servers put the nick name of the user in front of the
 *              channel.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: TopicReply.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class TopicReply {

    /**
     * Instantiation is not allowed.
     */
    private TopicReply() {
    }

    /**
     * Creates a new RPL_TOPIC message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channel    String containing the channel name.
     * @param topic      String containing the channel topic.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String topic) {
        String[] args = new String[] { "-", channel, topic };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_TOPIC, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 2);
    }

    /**
     * Returns the channel topic.
     *
     * @return String containing the channel topic.
     */
    public static String getTopic(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 1);
    }
}
