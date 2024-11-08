package org.smapcore.smap.core.server;

import java.io.InputStream;
import org.smapcore.smap.core.*;
import org.smapcore.smap.transport.SMAPChannel;

public class SMAPRequestEnvelope extends SMAPEnvelope {

    private SMAPChannel channel;

    public SMAPRequestEnvelope(SMAPParsedEnvelope packet) {
        this.header = packet.getHeader();
        this.credentials = packet.getCredentials();
        this.messages = packet.getMessages();
        this.channel = packet.getChannel();
    }

    public SMAPReplyEnvelope getReplyEnvelope() {
        return (getReplyEnvelope(-1));
    }

    public SMAPReplyEnvelope getReplyEnvelope(int numberMessages) {
        return (new SMAPReplyEnvelope(this.header.getVersion(), this.header.getURL(), numberMessages, this.channel));
    }

    public int getNumberMessages() {
        return (this.header.getMessageCount());
    }

    public boolean hasNextMessage() {
        return (this.messages.hasNext());
    }

    public SMAPMessage getNextMessage() {
        return ((SMAPMessage) this.messages.next());
    }
}
