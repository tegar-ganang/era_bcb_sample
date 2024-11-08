package net.hypotenubel.irc.msgutils;

import java.util.ArrayList;
import java.util.StringTokenizer;
import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of MSG_LIST messages.
 * <p>
 * <b>Syntax:</b> {@code LIST [ &lt;channel&gt; *( "," &lt;channel&gt; ) [ &lt;target&gt; ] ]}
 * <p>
 * The list command is used to list channels and their topics. If the
 * {@code channel} parameter is used, only the status of that channel is
 * displayed.
 * <p>
 * If the {@code target} parameter is specified, the request is forwarded
 * to that server which will generate the reply.
 * <p>
 * Wildcards are allowed in the {@code target} parameter.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: ListMessage.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class ListMessage {

    /**
     * Instantiation is not allowed.
     */
    private ListMessage() {
    }

    /**
     * Creates a new MSG_LIST message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost) {
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_LIST, null);
    }

    /**
     * Creates a new MSG_LIST message.
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
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_LIST, args);
    }

    /**
     * Creates a new MSG_LIST message with the supplied information.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param channel String object containing the channel name.
     * @param target  String object containing the target server.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String target) {
        String[] args = new String[] { channel, target };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_LIST, args);
    }

    /**
     * Creates a new MSG_LIST message.
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
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_LIST, args);
    }

    /**
     * Creates a new MSG_LIST message.
     *
     * @param msgNick  String object containing the nick of the guy this message
     *                 comes from. Should usually be "".
     * @param msgUser  String object containing the user name of the guy this
     *                 message comes from. Should usually be "".
     * @param msgHost  String object containing the host name of the guy this
     *                 message comes from. Should usually be "".
     * @param channels ArrayList containing the channel names.
     * @param target   String object containing the target server.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, ArrayList channels, String target) {
        StringBuffer chn = new StringBuffer((String) channels.get(0));
        for (int i = 1; i < channels.size(); i++) chn.append("," + (String) channels.get(i));
        String[] args = new String[] { chn.toString(), target };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_LIST, args);
    }

    /**
     * Indicates if a LIST message contains a channel list instead of a single
     * channel.
     *
     * @return {@code boolean} indicating whether there's a channel list
     *         ({@code true}) or not ({@code false}).
     */
    public static boolean containsChannelList(IRCMessage msg) {
        try {
            return (msg.getArgs().get(0)).indexOf(",") >= 0;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Returns the channel name, if any.
     *
     * @return String containing the channel name or "" if none is given.
     */
    public static String getChannel(IRCMessage msg) {
        try {
            return msg.getArgs().get(0);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Returns the channel names.
     *
     * @return ArrayList containing the channel names or an empty list if none
     *         is given.
     */
    public static ArrayList<String> getChannels(IRCMessage msg) {
        ArrayList<String> list = new ArrayList<String>(20);
        try {
            StringTokenizer t = new StringTokenizer(msg.getArgs().get(0), ",", false);
            while (t.hasMoreTokens()) list.add(t.nextToken());
            return list;
        } catch (IndexOutOfBoundsException e) {
            return list;
        }
    }

    /**
     * Returns the target server, if any.
     *
     * @return String containing the target server or "" if none is given.
     */
    public static String getTarget(IRCMessage msg) {
        try {
            return msg.getArgs().get(1);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
