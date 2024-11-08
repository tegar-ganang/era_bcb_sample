package org.timothyb89.jtelirc.message;

import org.timothyb89.jtelirc.channel.Channel;
import org.timothyb89.jtelirc.server.Server;
import org.timothyb89.jtelirc.user.User;
import org.timothyb89.jtelirc.util.ListUtil;

/**
 * Defines a UserMessage.
 * @author timothyb89
 */
public class UserMessage extends Message {

    /**
	 * The user that sent this message.
	 */
    private User user;

    /**
	 * This message's command (ex. privmsg, notice, etc).
	 */
    private String command;

    /**
	 * This message's destination (the user or channel it was sent to).
	 */
    private String destination;

    /**
	 * The context of this message (public, private).
	 */
    private MessageContext context;

    /**
	 * The text of this message.
	 */
    private String text;

    /**
	 * Constructs UserMessage
	 * @param server The server from which this message was sent
	 * @param raw The raw text of the message
	 */
    public UserMessage(Server server, String raw) {
        super(server, raw);
    }

    /**
	 * Parses the message and allocates variables.
	 * This should be called automatically in the constructor.
	 */
    @Override
    public void parse() {
        String[] sptext = getRaw().split(" ", 4);
        command = sptext[1];
        if (command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("join")) {
            sptext = getRaw().split(" ", 3);
            text = sptext[2].substring(1);
            destination = null;
        } else {
            if (command.equalsIgnoreCase("mode")) {
                text = sptext[3];
            } else if (sptext.length >= 4) {
                text = sptext[3].substring(1);
            }
            destination = sptext[2];
        }
        user = getServer().getUserList().getUser(sptext[0].substring(1));
        context = new MessageContext(destination, user.getNick());
        if (command.equalsIgnoreCase("nick")) {
            user.processNickChange(sptext[2]);
        }
    }

    /**
	 * Gets the command used when sending this message, such as PRIVMSG, NOTICE,
	 * QUIT, JOIN, PART, etc.
	 * @return This message's command
	 */
    public String getCommand() {
        return command;
    }

    /**
	 * Gets the destination to which this Message was sent. This should be a
	 * channel or a user.
	 * If this message does not have a destination, this will return null.
	 * @return This message's destination.
	 */
    public String getDestination() {
        return destination;
    }

    /**
	 * Gets the channel this message was sent to, if any.
	 * If this message was not sent to a channel, this method will return null.
	 * @return The channel this message was sent to
	 */
    public Channel getChannel() {
        if (getDestination() != null && getContext().isPublic()) {
            return getServer().getChannel(getDestination());
        }
        return null;
    }

    /**
	 * Gets the text of this message. For example, in:
	 *     <blockquote>
	 *     :somenick!someuser@somehost PRIVMSG #somechannel :Some text...
	 *     </blockquote>
	 * This method will return "Some text..."
	 * @return The text of the message
	 */
    public String getText() {
        return text;
    }

    /**
	 * Gets the User who sent this message.
	 * @return the User that sent this message.
	 */
    public User getUser() {
        return user;
    }

    /**
	 * Replys to this message by sending a message to the
	 * <code>destination</code>.
	 * <p>If <code>address</code> is true, the sender's name will be prefixed to
	 * the reply (ex: <user>: <reply>) in public places (channels)</p>
	 * <p>If the text is empty, no message will be sent.</p>
	 * @param text The text to send (may be prefixed- see above)
	 * @param address address the user?
	 */
    public void reply(String text, boolean address) {
        if (text.trim().isEmpty()) {
            return;
        }
        if (context.isPublic() && address) {
            text = getUser().getNick() + ": " + text;
        }
        getServer().sendMessage(context.getReplyDestination(), text);
    }

    /**
	 * Replys to the message, addressing the user. See reply(text, address) for
	 * more information.
	 * @param text The text to send.
	 */
    public void reply(String text) {
        reply(text, true);
    }

    /**
	 * Checks if the text of this message is addressing the given nick. To do
	 * so, the first word must match one of the following:
	 * <ul>
	 *     <li>nick: text</li>
	 *     <li>nick, text</li>
	 *     <li>nick- text</li>
	 * </ul>
	 * @param nick The nick to check
	 * @return True if the given nick is being addressed, false if not.
	 */
    public boolean isAddressing(String nick) {
        String word = ListUtil.safeSplit(getText(), " ").get(0).toLowerCase();
        return word.matches(nick.toLowerCase() + "[,:\\-]");
    }

    /**
	 * Checks if the text of this Message is addressing the given user. See
	 * isAddressing(string) for more information on how this is determined.
	 * @param user The user to check
	 * @return True if the given user is being addressed, false if not.
	 */
    public boolean isAddressing(User user) {
        return isAddressing(user.getNick());
    }

    /**
	 * Creates an HTML representation of this UserMessage.
	 * Note that the channel/sender is assumed, and that CTCP ACTIONs will be
	 * processed.
	 * @return an HTML / displayable representation of this message.
	 */
    @Override
    public String format() {
        String nick = user.getNick();
        if (command.equalsIgnoreCase("privmsg")) {
            if (isCTCP()) {
                if (getCTCPCommand().equalsIgnoreCase("action")) {
                    return "*** " + getUser().getNick() + " " + getCTCPArguments();
                }
            }
            return "&lt;" + nick + "&gt; " + getText();
        } else if (command.equalsIgnoreCase("quit")) {
            return "*** " + getUser().getNick() + " has quit: " + getText();
        } else if (command.equalsIgnoreCase("join")) {
            return "*** " + nick + " has joined " + getText();
        } else if (command.equalsIgnoreCase("part")) {
            return "*** " + nick + " has left " + context.getReplyDestination() + ": " + getText();
        } else if (command.equalsIgnoreCase("topic")) {
            return "*** " + nick + " has changed the topic to: " + getText();
        } else {
            return super.format();
        }
    }

    /**
	 * Returns true when both the first and last characters of this message
	 * start with the character 0x01 ("\001").
	 * @return true when the text starts and ends with 0x01.
	 */
    public boolean isCTCP() {
        return getText().startsWith("\001") && getText().endsWith("\001");
    }

    /**
	 * Gets the CTCP text, or a CTCP command minus the \001 characters.
	 * @return The CTCP text, or null if this is not a CTCP message.
	 */
    public String getCTCPText() {
        if (isCTCP()) {
            return getText().replace("\001", "").trim();
        } else {
            return null;
        }
    }

    /**
	 * Gets the CTCP command. For example in the CTCP message:
	 *		\001ACTION tests\001
	 * Null is returned when this is not a CTCP message.
	 * @return The CTCP command, if any.
	 */
    public String getCTCPCommand() {
        if (isCTCP()) {
            String ct = getCTCPText();
            if (ct.contains(" ")) {
                return ct.substring(0, ct.indexOf(" ")).trim();
            } else {
                return ct;
            }
        } else {
            return null;
        }
    }

    /**
	 * Gets the text, if any, after the first word.
	 * If the text does not exist, and empty string is returned. If the text
	 * starts with a colon (":"), it will be removed.
	 * If this is not a CTCP message, null is returned.
	 * @return Any CTCP arguments, or additional text.
	 */
    public String getCTCPArguments() {
        if (isCTCP()) {
            String ct = getCTCPText();
            if (ct.contains(" ")) {
                return ct.substring(ct.indexOf(" ") + 1, ct.length());
            } else {
                return "";
            }
        } else {
            return null;
        }
    }

    /**
	 * Gets the MessageContext of this message, used to generate correct reply
	 * destinations, and such.
	 * @return This message's MessageContext
	 */
    public MessageContext getContext() {
        return context;
    }
}
