package net.hypotenubel.irc.msgutils;

import java.text.MessageFormat;
import java.text.ParseException;
import net.hypotenubel.irc.*;

/**
 * Wrapper for easy handling of ERR_UNKNOWNMODE messages.
 * <p>
 * <b>Syntax:</b> {@code 472 &lt;char&gt; "is unknown mode char to me for &lt;channel&gt;"}
 * <p>
 * No description available.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: UnknownmodeError.java 91 2006-07-21 13:41:43Z captainnuss $
 */
public class UnknownmodeError {

    /**
     * Instantiation is not allowed.
     */
    private UnknownmodeError() {
    }

    /**
     * Creates a new ERR_UNKNOWNMODE message.
     *
     * @param msgNick    String object containing the nick of the guy this
     *                   message comes from. Should usually be "".
     * @param msgUser    String object containing the user name of the guy this
     *                   message comes from. Should usually be "".
     * @param msgHost    String object containing the host name of the guy this
     *                   message comes from. Should usually be "".
     * @param character  {@code char} containing the character.
     * @param channel    String containing the channel name.
     */
    public static IRCMessage createMessage(String msgNick, String msgUser, String msgHost, char character, String channel) {
        String[] args = new String[] { String.valueOf(character), "is unknown mode char to me for " + channel };
        return new IRCMessage(msgNick, msgUser, msgHost, IRCMessageTypes.ERR_UNKNOWNMODE, args);
    }

    /**
     * Returns the character.
     *
     * @return String containing the character.
     */
    public static String getCharacter(IRCMessage msg) {
        return msg.getArgs().get(0);
    }

    /**
     * Returns the channel name.
     *
     * @return String containing the user name or "" if it doesn't comply with
     *         RFC 2818.
     */
    public static String getChannel(IRCMessage msg) {
        try {
            MessageFormat format = new MessageFormat("is unknown mode char to me for {0}");
            Object[] stuff = format.parse(msg.getArgs().get(1));
            return (String) stuff[0];
        } catch (ParseException e) {
            return "";
        }
    }
}
