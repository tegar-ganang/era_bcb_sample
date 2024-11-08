package net.hypotenubel.irc.msgutils;

import java.util.ArrayList;
import java.util.StringTokenizer;
import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of MSG_PART messages.
 * <p>
 * <b>Syntax:</b> {@code PART &lt;channel&gt; *( "," &lt;channel&gt; ) [ &lt;part message&gt; ]}
 * <p>
 * The PART command causes the user sending the message to be removed from the
 * list of active members for all given channels listed in the parameter string.
 * If a {@code part message} is given, this will be sent instead of the
 * default message, the nickname. This request is always granted by the server.
 * <p>
 * Servers <b>must</b> be able to parse arguments in the form of a list of
 * target, but <b>should not</b> use lists when sending PART messages to
 * clients.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: PartMessage.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class PartMessage {

    /**
     * Instantiation is not allowed.
     */
    private PartMessage() {
    }

    /**
     * Creates a new MSG_PART message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param channel String object containing the channel name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel) {
        String[] args = new String[] { channel };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_PART, args);
    }

    /**
     * Creates a new MSG_PART message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param channel String object containing the channel name.
     * @param partmsg String object containing the part message.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String partmsg) {
        String[] args = new String[] { channel, partmsg };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_PART, args);
    }

    /**
     * Creates a new MSG_PART message.
     *
     * @param msgNick  String object containing the nick of the guy this message
     *                 comes from. Should usually be "".
     * @param msgUser  String object containing the user name of the guy this
     *                 message comes from. Should usually be "".
     * @param msgHost  String object containing the host name of the guy this
     *                 message comes from. Should usually be "".
     * @param channels ArrayList containing the channel names.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, ArrayList channels) {
        StringBuffer chn = new StringBuffer((String) channels.get(0));
        for (int i = 1; i < channels.size(); i++) chn.append("," + (String) channels.get(i));
        String[] args = new String[] { chn.toString() };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_PART, args);
    }

    /**
     * Creates a new MSG_PART message with the supplied information.
     *
     * @param msgNick  String object containing the nick of the guy this message
     *                 comes from. Should usually be "".
     * @param msgUser  String object containing the user name of the guy this
     *                 message comes from. Should usually be "".
     * @param msgHost  String object containing the host name of the guy this
     *                 message comes from. Should usually be "".
     * @param channels ArrayList containing the channel names.
     * @param partmsg  String object containing the part message.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, ArrayList channels, String partmsg) {
        StringBuffer chn = new StringBuffer((String) channels.get(0));
        for (int i = 1; i < channels.size(); i++) chn.append("," + (String) channels.get(i));
        String[] args = new String[] { chn.toString(), partmsg };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_PART, args);
    }

    /**
     * Indicates if a PART message contains a channel list instead of a single
     * channel.
     *
     * @return {@code boolean} indicating whether there's a channel list
     *         ({@code true}) or not ({@code false}).
     */
    public static boolean containsChannelList(IRCMessage msg) {
        return (msg.getArgs().get(0)).indexOf(",") >= 0;
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
     * Returns the channel names.
     *
     * @return ArrayList containing the channel names.
     */
    public static ArrayList<String> getChannels(IRCMessage msg) {
        StringTokenizer t = new StringTokenizer(msg.getArgs().get(0), ",", false);
        ArrayList<String> list = new ArrayList<String>(20);
        while (t.hasMoreTokens()) list.add(t.nextToken());
        return list;
    }

    /**
     * Returns the part message, if any.
     *
     * @return String containing the part message or "" if none is given.
     */
    public static String getMessage(IRCMessage msg) {
        try {
            return msg.getArgs().get(1);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
