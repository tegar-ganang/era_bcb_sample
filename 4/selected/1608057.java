package de.teamwork.irc.msgutils;

import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of MSG_SUMMON messages.
 * <p>
 * <b>Syntax:</b> <code>SUMMON &lt;user&gt; [ &lt;target&gt; [ &lt;channel&gt; ] ]</code>
 * <p>
 * The SUMMON command can be used to give users who are on a host running an IRC
 * server a message asking them to please join IRC. This message is only sent if
 * the target server (a) has SUMMON enabled, (b) the user is logged in and (c)
 * the server process can write to the user's tty (or similar).
 * <p>
 * If no <code>server</code> parameter is given it tries to summon
 * <code>user</code> from the server the client is connected to is assumed as
 * the target.
 * If summon is not enabled in a server, it <b>must</b> return the
 * ERR_SUMMONDISABLED numeric.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: SummonMessage.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class SummonMessage {

    /**
     * Instantiation is not allowed.
     */
    private SummonMessage() {
    }

    /**
     * Creates a new MSG_SUMMON message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param user    String containing the user name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String user) {
        String[] args = new String[] { user };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_SUMMON, args);
    }

    /**
     * Creates a new MSG_SUMMON message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param user    String containing the user name.
     * @param target  String containing the target name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String user, String target) {
        String[] args = new String[] { user, target };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_SUMMON, args);
    }

    /**
     * Creates a new MSG_SUMMON message.
     *
     * @param msgNick String object containing the nick of the guy this message
     *                comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param user    String containing the user name.
     * @param target  String containing the target name.
     * @param channel String containing the channel name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String user, String target, String channel) {
        String[] args = new String[] { user, target, channel };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.MSG_SUMMON, args);
    }

    /**
     * Returns the user name.
     *
     * @return String containing the user name.
     */
    public static String getUser(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(0);
    }

    /**
     * Returns the target name, if any.
     *
     * @return String containing the target name or "" if none is given.
     */
    public static String getTarget(IRCMessage msg) {
        try {
            return (String) msg.getArgs().elementAt(1);
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Returns the channel name, if any.
     *
     * @return String containing the channel name or "" if none is given.
     */
    public static String getChannel(IRCMessage msg) {
        try {
            return (String) msg.getArgs().elementAt(2);
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }
}
