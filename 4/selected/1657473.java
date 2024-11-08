package net.hypotenubel.irc.msgutils;

import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of RPL_LIST messages.
 * <p>
 * <b>Syntax:</b> {@code 322 &lt;channel&gt; &lt;# visible&gt; &lt;topic&gt;}
 * <p>
 * Replies RPL_LIST, RPL_LISTEND mark the actual replies with data and end of
 * the server's response to a LIST command. If there are no channels available
 * to return, only the end reply <b>must</b> be sent.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: ListReply.java 96 2006-09-13 22:20:24Z captainnuss $
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
     * @param visible    {@code int} containing the amount of people in a
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
        return msg.getArgs().get(msg.getArgs().size() - 3);
    }

    /**
     * Returns the number of people on the channel.
     *
     * @return {@code int} containing the number of people.
     */
    public static int getVisible(IRCMessage msg) {
        try {
            return Integer.parseInt(msg.getArgs().get(msg.getArgs().size() - 2));
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
            return msg.getArgs().get(msg.getArgs().size() - 1);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
