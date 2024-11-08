package org.bing.zion.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class Session {

    private AbstractChannelService service;

    private SocketChannel channel;

    private ProtocolFilterChain filterChain;

    private MessageHandlerChain handlerChain;

    private Map<String, Object> attributes = new HashMap<String, Object>();

    private boolean closed = false;

    public Session(AbstractChannelService service, SocketChannel channel, ProtocolFilterChain fc, MessageHandlerChain hc) {
        this.service = service;
        this.channel = channel;
        this.filterChain = fc;
        this.handlerChain = hc;
    }

    public AbstractChannelService getService() {
        return service;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public String getClientIp() {
        return channel.socket().getInetAddress().getHostAddress();
    }

    public ProtocolFilterChain getFilterChain() {
        return filterChain;
    }

    public MessageHandlerChain getHandlerChain() {
        return handlerChain;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public void write(Object msg) {
        this.service.write(this, msg);
    }

    public void flush(ByteBuffer buf) {
        try {
            this.service.flush(this, buf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }
}
