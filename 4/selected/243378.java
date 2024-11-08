package wtanaka.praya.irc;

import java.util.Date;
import wtanaka.praya.Protocol;
import wtanaka.praya.Recipient;

public class IRCPartMessage extends IRCMessage {

    String prefixNick;

    String username;

    String hostname;

    String channel;

    private Date happenedAt = new Date();

    public IRCPartMessage(String prefixNick, String username, String hostname, String channel, Protocol generatedBy) {
        super(generatedBy);
        this.prefixNick = prefixNick;
        this.username = username;
        this.hostname = hostname;
        this.channel = channel;
    }

    public String getSubject() {
        return channel;
    }

    public String getContents() {
        String usernameString = "unknown";
        if (!(username == null && hostname == null)) usernameString = username + "@" + hostname;
        return "(" + usernameString + ") leaves";
    }

    public String getNickname() {
        return prefixNick;
    }

    public String getChannel() {
        return channel;
    }

    public String getFrom() {
        return prefixNick;
    }

    /**
    * private message to the user who just left the channel.
    * @bug nyi
    **/
    public Recipient replyRecipient() {
        return new IRCUserViaPrivMsg(generatedBy, prefixNick);
    }
}
