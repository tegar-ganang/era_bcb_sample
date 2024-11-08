package wtanaka.praya.irc;

import java.io.IOException;
import java.util.Hashtable;
import wtanaka.praya.NotSentException;
import wtanaka.praya.Protocol;
import wtanaka.praya.Recipient;
import wtanaka.praya.ResolvedRecipient;

/**
 * This recipient represents an IRC channel.
 * 
 * @author Wesley Tanaka
 * @version $Name:  $ $Date: 2003/12/17 01:27:21 $
 **/
public class IRCChannelRecipient extends ResolvedRecipient {

    String m_channelName;

    private Hashtable m_fields = new Hashtable();

    private static final String CHANNEL_KEY = "Channel";

    public IRCChannelRecipient(Protocol parent, String channelName) {
        super(parent);
        m_channelName = channelName;
        m_fields.put(CHANNEL_KEY, m_channelName);
    }

    public String getDescription() {
        return m_channelName;
    }

    public String getFullDescription() {
        return m_channelName + " via " + m_protocol;
    }

    public Hashtable getFieldNames() {
        return m_fields;
    }

    public Recipient withNewFields(Hashtable newFields) {
        String newChannel;
        if ((newChannel = (String) newFields.get(CHANNEL_KEY)) != null) return new IRCChannelRecipient(m_protocol, newChannel);
        return this;
    }

    /**
    * @exception NotSentException if there was an I/O exception while
    * trying to send the message.
    **/
    public void sendReply(Object reply) throws NotSentException {
        IRCClient ircc = ((IRCClient) m_protocol);
        String[] recipients = new String[] { m_channelName };
        IRCSelfChannelMessage msg = new IRCSelfChannelMessage(ircc.getNick(), this, String.valueOf(reply), ircc);
        try {
            ircc.sendMessage(msg);
        } catch (IOException e) {
            throw new NotSentException(e.toString());
        }
    }

    public String getChannelName() {
        return m_channelName;
    }
}
