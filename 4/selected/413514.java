package de.teamwork.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import de.teamwork.irc.*;

/**
 * Wrapper for easy handling of ERR_BANLISTFULL messages.
 * <p>
 * <b>Syntax:</b> <code>467 &lt;channel&gt; &lt;char&gt; "Channel list is full"</code>
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: BanlistfullError.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public class BanlistfullError {

    /**
     * Instantiation is not allowed.
     */
    private BanlistfullError() {
    }

    /**
     * Creates a new ERR_BANLISTFULL message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param channel    String containing the channel name.
     * @param character  <code>char</code> containing the character.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, String channel, char character) {
        String[] args = new String[] { channel, String.valueOf(character), "Channel list is full" };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_BANLISTFULL, args);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the channel name.
     */
    public static String getChannel(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(0);
    }

    /**
     * Returns the character.
     *
     * @return String containing the character.
     */
    public static String getCharacter(IRCMessage msg) {
        return (String) msg.getArgs().elementAt(1);
    }
}
