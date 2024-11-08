package net.hypotenubel.irc.msgutils;

import java.util.ArrayList;
import java.util.StringTokenizer;
import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of MSG_NAMES messages.
 * <p>
 * <b>Syntax:</b> {@code NAMES [ &lt;channel&gt; *( "," &lt;channel&gt; ) [ &lt;target&gt; ] ]}
 * <p>
 * By using the NAMES command, a user can list all nicknames that are visible to
 * him. For more details on what is visible and what is not, see "Internet Relay
 * Chat: Channel Management" [IRC-CHAN]. The {@code channel} parameter
 * specifies which channel(s) to return information about. There is no error
 * reply for bad channel names.
 * <p>
 * If no {@code channel} parameter is given, a list of all channels and
 * their occupants is returned. At the end of this list, a list of users who are
 * visible but either not on any channel or not on a visible channel are listed
 * as being on <i>channel</i> "*".
 * <p>
 * If the {@code target} parameter is specified, the request is forwarded
 * to that server which will generate the reply.
 * <p>
 * Wildcards are allowed in the {@code target} parameter.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: NamesMessage.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class NamesMessage {

    /**
     * Instantiation is not allowed.
     */
    private NamesMessage() {
    }

    /**
     * Creates a new MSG_NAMES message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost) {
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_NAMES, null);
    }

    /**
     * Creates a new MSG_NAMES messagen.
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
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_NAMES, args);
    }

    /**
     * Creates a new MSG_NAMES message.
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
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_NAMES, args);
    }

    /**
     * Creates a new MSG_NAMES message.
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
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_NAMES, args);
    }

    /**
     * Creates a new MSG_NAMES message.
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
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_NAMES, args);
    }

    /**
     * Indicates if a NAMES message contains a channel list instead of a single
     * channel name.
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
     * Returns the channel names, if any.
     *
     * @return ArrayList containing the channel names or an empty list if none
     *         are given.
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
     * Returns the target server name, if any.
     *
     * @return String containing the target server name or "" if none is given.
     */
    public static String getTarget(IRCMessage msg) {
        try {
            return msg.getArgs().get(1);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
