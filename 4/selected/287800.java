package wtanaka.praya.irc;

import java.util.Date;
import wtanaka.praya.Protocol;
import wtanaka.praya.Recipient;

/**
 * Message received when a user joins a channel.
 **/
public class IRCJoinMessage extends IRCMessage {

    String prefixNick;

    String username;

    String hostname;

    String channel;

    private Date happenedAt = new Date();

    public IRCJoinMessage(String prefixNick, String username, String hostname, String channel, Protocol generatedBy) {
        super(generatedBy);
        this.prefixNick = prefixNick;
        this.username = username;
        this.hostname = hostname;
        this.channel = channel;
    }

    public String getContents() {
        String usernameString = "unknown";
        if (!(username == null && hostname == null)) usernameString = username + "@" + hostname;
        return " (" + usernameString + ") joins (" + happenedAt + ")";
    }

    public String getNickname() {
        return prefixNick;
    }

    public String getChannel() {
        return channel;
    }

    public String getSubject() {
        return channel;
    }

    public String getFrom() {
        return prefixNick;
    }

    /**
    * The recipient corresponding to the channel.
    **/
    public Recipient replyRecipient() {
        return new IRCChannelRecipient(generatedBy, getChannel());
    }
}
