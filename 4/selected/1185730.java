package org.smapcore.smap.core;

import org.smapcore.smap.transport.SMAPChannel;

public class SMAPParsedEnvelope extends SMAPEnvelope {

    private SMAPChannel channel = null;

    private Object returnHandler = null;

    public SMAPParsedEnvelope() {
    }

    public SMAPParsedEnvelope(SMAPEnvelope envelope) {
        this.header = envelope.getHeader();
        this.messages = envelope.getMessages();
    }

    public SMAPChannel getChannel() {
        return (this.channel);
    }

    public void setChannel(SMAPChannel channel) {
        this.channel = channel;
        return;
    }

    public Object getReturnHandler() {
        return (this.returnHandler);
    }

    public void setReturnHandler(Object handler) {
        this.returnHandler = handler;
        return;
    }

    public SMAPHeader getHeader() {
        return (this.header);
    }

    public void setHeader(SMAPHeader header) {
        this.header = header;
        this.messages.setSize(header.getMessageCount());
        return;
    }

    public SMAPCredential getCredentials() {
        return (this.credentials);
    }

    public void setCredentials(SMAPCredential credential) {
        this.credentials = credential;
        return;
    }

    public MessageIterator getMessages() {
        return (this.messages);
    }

    public boolean hasNextMessage() {
        return (this.messages.hasNext());
    }

    public SMAPMessage getNextMessage() {
        return ((SMAPMessage) this.messages.next());
    }

    public void addMessage(SMAPMessage message) {
        this.messages.add(message);
        return;
    }

    public void endMessages() {
        this.messages.finished();
        return;
    }
}
