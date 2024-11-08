package net.hypotenubel.irc.msgutils;

import java.util.ArrayList;
import java.util.StringTokenizer;
import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of MSG_KICK messages.
 * <p>
 * <b>Syntax:</b> {@code KICK &lt;channel&gt; *( "," &lt;channel&gt; ) &lt;user&gt; *( "," &lt;user&gt; ) [ &lt;comment&gt; ]}
 * <p>
 * The KICK command can be used to request the forced removal of a user from a
 * channel. It causes the {@code user} to PART from the
 * {@code channel} by force. For the message to be syntactically correct,
 * there <b>must</b> be either one channel parameter and multiple user
 * parameter, or as many channel parameters as there are user parameters. If a
 * {@code comment} is given, this will be sent instead of the default
 * message, the nickname of the user issuing the KICK.
 * <p>
 * The server <b>must not</b> send KICK messages with multiple channels or users
 * to clients. This is necessarily to maintain backward compatibility with old
 * client software.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: KickMessage.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class KickMessage {

    /**
     * Instantiation is not allowed.
     */
    private KickMessage() {
    }

    /**
     * Creates a new MSG_KICK message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param channel String object containing the channel name.
     * @param user    String object containing the user name.
     * @param comment String object containing the kick message. May be "".
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, String user, String comment) {
        if (comment.equals("")) {
            String[] args = new String[] { channel, user };
            return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_KICK, args);
        } else {
            String[] args = new String[] { channel, user, comment };
            return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_KICK, args);
        }
    }

    /**
     * Creates a new MSG_KICK message.
     *
     * @param msgNick  String object containing the nick of the guy this message
     *                 comes from. Should usually be "".
     * @param msgUser  String object containing the user name of the guy this
     *                 message comes from. Should usually be "".
     * @param msgHost  String object containing the host name of the guy this
     *                 message comes from. Should usually be "".
     * @param channels ArrayList containing the channel names.
     * @param users    ArrayList containing the user names.
     * @param comment  String object containing the kick message. May be "".
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, ArrayList channels, ArrayList users, String comment) {
        StringBuffer chn = new StringBuffer((String) channels.get(0));
        StringBuffer usr = new StringBuffer((String) users.get(0));
        for (int i = 1; i < channels.size(); i++) chn.append("," + (String) channels.get(i));
        for (int i = 1; i < users.size(); i++) usr.append("," + (String) users.get(i));
        if (comment.equals("")) {
            String[] args = new String[] { chn.toString(), usr.toString() };
            return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_KICK, args);
        } else {
            String[] args = new String[] { chn.toString(), usr.toString(), comment };
            return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_KICK, args);
        }
    }

    /**
     * Indicates if a KICK message contains a channel list instead of a single
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
     * @return ArrayList containing the channels.
     */
    public static ArrayList<String> getChannels(IRCMessage msg) {
        StringTokenizer t = new StringTokenizer(msg.getArgs().get(0), ",", false);
        ArrayList<String> list = new ArrayList<String>(20);
        while (t.hasMoreTokens()) list.add(t.nextToken());
        return list;
    }

    /**
     * Indicates if a KICK message contains a user list instead of a single
     * user.
     *
     * @return {@code boolean} indicating whether there's a user list
     *         ({@code true}) or not ({@code false}).
     */
    public static boolean containsUserList(IRCMessage msg) {
        return (msg.getArgs().get(1)).indexOf(",") >= 0;
    }

    /**
     * Returns the user name.
     *
     * @return String containing the user name.
     */
    public static String getUser(IRCMessage msg) {
        return msg.getArgs().get(1);
    }

    /**
     * Returns the user list.
     *
     * @return ArrayList containing the user names.
     */
    public static ArrayList<String> getUsers(IRCMessage msg) {
        StringTokenizer t = new StringTokenizer(msg.getArgs().get(1), ",", false);
        ArrayList<String> list = new ArrayList<String>(20);
        while (t.hasMoreTokens()) list.add(t.nextToken());
        return list;
    }

    /**
     * Returns the comment, if any.
     *
     * @return String containing the comment or "" if none is given.
     */
    public static String getComment(IRCMessage msg) {
        try {
            return msg.getArgs().get(2);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
