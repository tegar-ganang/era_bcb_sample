package net.hypotenubel.irc.msgutils;

import java.util.ArrayList;
import java.util.StringTokenizer;
import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of MSG_JOIN messages.
 * <p>
 * <b>Syntax:</b> {@code JOIN ( &lt;channel&gt; *( "," &lt;channel&gt; ) [ &lt;key&gt; *( "," &lt;key&gt; ) ] ) / "0"}
 * <p>
 * The JOIN command is used by a user to request to start listening to the
 * specific channel. Servers <b>must</b> be able to parse arguments in the form
 * of a list of target, but <b>should not</b> use lists when sending JOIN
 * messages to clients.
 * <p>
 * Once a user has joined a channel, he receives information about all commands
 * his server receives affecting the channel. This includes JOIN, MODE, KICK,
 * PART, QUIT and of course PRIVMSG/NOTICE. This allows channel members to keep
 * track of the other channel members, as well as channel modes.
 * <p>
 * If a JOIN is successful, the user receives a JOIN message as confirmation and
 * is then sent the channel's topic (using RPL_TOPIC) and the list of users who
 * are on the channel (using RPL_NAMREPLY), which <b>must</b> include the user
 * joining.
 * <p>
 * Note that this message accepts a special argument ("0"), which is a special
 * request to leave all channels the user is currently a member of. The server
 * will process this message as if the user had sent a PART command for each
 * channel he is a member of.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: JoinMessage.java 150 2006-10-04 22:11:12Z captainnuss $
 */
public class JoinMessage {

    /**
     * Instantiation is not allowed.
     */
    private JoinMessage() {
    }

    /**
     * Creates a new MSG_JOIN message.
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
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_JOIN, args);
    }

    /**
     * Creates a new MSG_JOIN message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param channel String object containing the channel name.
     * @param key     String object containing the channel key.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String key) {
        if (key.length() == 0) {
            return createMessage(msgNick, msgUser, msgHost, channel);
        }
        String[] args = new String[] { channel, key };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_JOIN, args);
    }

    /**
     * Creates a new MSG_JOIN message.
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
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_JOIN, args);
    }

    /**
     * Creates a new MSG_JOIN message.
     *
     * @param msgNick  String object containing the nick of the guy this message
     *                 comes from. Should usually be "".
     * @param msgUser  String object containing the user name of the guy this
     *                 message comes from. Should usually be "".
     * @param msgHost  String object containing the host name of the guy this
     *                 message comes from. Should usually be "".
     * @param channels ArrayList containing the channel names.
     * @param keys     ArrayList containing the channel keys.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, ArrayList channels, ArrayList keys) {
        StringBuffer chn = new StringBuffer((String) channels.get(0));
        StringBuffer key = new StringBuffer((String) keys.get(0));
        for (int i = 1; i < channels.size(); i++) chn.append("," + (String) channels.get(i));
        for (int i = 1; i < keys.size(); i++) key.append("," + (String) keys.get(i));
        String[] args = new String[] { chn.toString(), key.toString() };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_JOIN, args);
    }

    /**
     * Indicates if a JOIN message contains a channel list instead of a single
     * channel.
     *
     * @return {@code boolean} indicating whether there's a channel list
     *         ({@code true}) or not ({@code false}).
     */
    public static boolean containsChannelList(IRCMessage msg) {
        return (msg.getArgs().get(0)).indexOf(",") >= 0;
    }

    /**
     * Returns the channel String.
     *
     * @return String containing the channel.
     */
    public static String getChannel(IRCMessage msg) {
        return msg.getArgs().get(0);
    }

    /**
     * Returns the channel list.
     *
     * @return ArrayList containing the channel.
     */
    public static ArrayList<String> getChannels(IRCMessage msg) {
        StringTokenizer t = new StringTokenizer(msg.getArgs().get(0), ",", false);
        ArrayList<String> list = new ArrayList<String>(20);
        while (t.hasMoreTokens()) list.add(t.nextToken());
        return list;
    }

    /**
     * Returns the channel key, if any.
     *
     * @return String containing the channel key or "" if none is given.
     */
    public static String getKey(IRCMessage msg) {
        try {
            return msg.getArgs().get(1);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Returns the channel key list, if any.
     *
     * @return ArrayList containing the channel keys or an empty list if none
     *         are given.
     */
    public static ArrayList<String> getKeys(IRCMessage msg) {
        ArrayList<String> list = new ArrayList<String>(5);
        try {
            StringTokenizer t = new StringTokenizer(msg.getArgs().get(1), ",", false);
            while (t.hasMoreTokens()) list.add(t.nextToken());
            return list;
        } catch (IndexOutOfBoundsException e) {
            return list;
        }
    }
}
