package org.soda.dpws.transport;

import org.soda.dpws.exchange.InMessage;
import org.soda.dpws.internal.DPWSContextImpl;

/**
 * 
 * 
 */
public abstract class AbstractChannel implements Channel {

    protected ChannelEndpoint receiver;

    private Transport transport;

    private String uri;

    protected String messageSenderClassName = null;

    public String getUri() {
        return uri;
    }

    /**
   * @param uri
   */
    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setEndpoint(ChannelEndpoint receiver) {
        this.receiver = receiver;
    }

    public void setMessageSenderClassName(String messageSenderClassName) {
        this.messageSenderClassName = messageSenderClassName;
    }

    public ChannelEndpoint getEndpoint() {
        return receiver;
    }

    public void receive(DPWSContextImpl context, InMessage message) {
        if (message.getChannel() == null) message.setChannel(this);
        getEndpoint().onReceive(context, message);
    }

    public Transport getTransport() {
        return transport;
    }

    /**
   * @param transport
   */
    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public boolean isAsync() {
        return true;
    }

    public void close() {
        transport.close(this);
    }
}
