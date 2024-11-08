package net.hypotenubel.irc.msgutils;

import java.util.StringTokenizer;
import java.util.ArrayList;
import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of RPL_WHOISCHANNELS messages.
 * <p>
 * <b>Syntax:</b> {@code 319 "&lt;nick&gt; "*( ( "@" / "+" ) &lt;channel&gt; " " )"}
 * <p>
 * Replies 311 - 313, 317 - 319 are all replies generated in response to a WHOIS
 * message. Given that there are enough parameters present, the answering server
 * <b>must</b> either formulate a reply out of the above numerics (if the query
 * nick is found) or return an error reply. The '*' in RPL_WHOISSERVER is there as
 * the literal character and not as a wild card. For each reply set, only
 * RPL_WHOISCHANNELS may appear more than once (for long lists of channel
 * names). The '@' and '+' characters next to the channel name indicate whether
 * a client is a channel operator or has been granted permission to speak on a
 * moderated channel. The RPL_ENDOFWHOIS reply is used to mark the end of
 * processing a WHOIS message.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: WhoischannelsReply.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class WhoischannelsReply {

    /**
     * Instantiation is not allowed.
     */
    private WhoischannelsReply() {
    }

    /**
     * Creates a new RPL_WHOISCHANNELS message.
     *
     * @param msgNick String object containing the nick of the guy this
     *                message comes from. Should usually be "".
     * @param msgUser String object containing the user name of the guy this
     *                message comes from. Should usually be "".
     * @param msgHost String object containing the host name of the guy this
     *                message comes from. Should usually be "".
     * @param nick    String object containing the nick name.
     * @param channel ArrayList containing the nick names. May contain 0
     *                elements.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String nick, ArrayList channel) {
        if (channel.size() == 0) {
            String[] args = new String[] { nick };
            return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_WHOISCHANNELS, args);
        }
        StringBuffer chn = new StringBuffer((String) channel.get(0));
        for (int i = 1; i < channel.size(); i++) chn.append(" " + (String) channel.get(i));
        String[] args = new String[] { nick, chn.toString() };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.RPL_WHOISCHANNELS, args);
    }

    /**
     * Returns the nick name.
     *
     * @return String containing the nick name.
     */
    public static String getNick(IRCMessage msg) {
        return msg.getArgs().get(0);
    }

    /**
     * Returns the channel names.
     *
     * @return ArrayList containing the channel names or an empty list if no
     *         names are given.
     */
    public static ArrayList<String> getChannels(IRCMessage msg) {
        ArrayList<String> list = new ArrayList<String>(20);
        if (msg.getArgs().size() == 0) return list;
        String reply = msg.getArgs().get(1);
        if (reply.equals("")) return list;
        StringTokenizer t = new StringTokenizer(reply, " ", false);
        while (t.hasMoreTokens()) list.add(t.nextToken());
        return list;
    }
}
