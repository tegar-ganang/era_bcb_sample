package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of RPL_LIST messages.
 * <p>
 * <b>Syntax:</b> <code>322 &lt;channel&gt; &lt;# visible&gt; &lt;topic&gt;</code>
 * <p>
 * Replies RPL_LIST, RPL_LISTEND mark the actual replies with data and end of
 * the server�s response to a LIST command. If there are no channels available
 * to return, only the end reply <b>must</b> be sent.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: ListReply.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class ListReply {

    /**
     * Instantiation is not allowed.
     */
    private ListReply() {
    }

    /**
     * Creates a new RPL_LIST message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channel    String containing the channel name.
     * @param visible    <code>int</code> containing the amount of people in a
     *                   channel.
     * @param topic      String containing the channel topic.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, int visible, String topic) {
        String[] args = new String[] { channel, String.valueOf(visible), topic };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_LIST, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(msg.getArgs().size() - 3);
    }

    /**
     * Returns the number of people on the channel.
     *
     * @return <code>int</code> containing the number of people.
     */
    public static int getVisible(IRCMessage msg) {
        try {
            return Integer.parseInt((String) msg.getArgs().elementAt(msg.getArgs().size() - 2));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns the channel topic.
     *
     * @return String containing the channel topic.
     */
    public static String getTopic(IRCMessage msg) {
        try {
            return (String) msg.getArgs().elementAt(msg.getArgs().size() - 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }
}
