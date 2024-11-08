package wtanaka.praya.irc;

import wtanaka.praya.Protocol;
import wtanaka.praya.Recipient;

/**
 * Represents a message sent by you.
 **/
public class IRCSelfChannelMessage extends IRCMessage {

    IRCChannelRecipient m_recipient;

    String messageText;

    String from;

    public IRCSelfChannelMessage(String fromNick, IRCChannelRecipient toChannel, String messageText, Protocol generatedBy) {
        super(generatedBy);
        this.from = fromNick;
        m_recipient = toChannel;
        this.messageText = messageText;
    }

    public IRCChannelRecipient getRecipient() {
        return m_recipient;
    }

    public String getFrom() {
        return "<" + from + ">";
    }

    public String getSubject() {
        return m_recipient.getChannelName();
    }

    public String getMessageText() {
        return messageText;
    }

    public String getContents() {
        if (messageText.startsWith((char) 1 + "ACTION ") && messageText.endsWith("" + (char) 1)) {
            return messageText.substring(8, messageText.length() - 1);
        }
        return messageText;
    }

    public boolean isSelfMessage() {
        return true;
    }

    /**
    *
    **/
    public Recipient replyRecipient() {
        return m_recipient;
    }
}
