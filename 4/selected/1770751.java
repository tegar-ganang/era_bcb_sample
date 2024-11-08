package com.hyk.proxy.framework.event;

import java.util.EventObject;
import org.jboss.netty.channel.Channel;

/**
 *
 */
public class HttpProxyEvent extends EventObject {

    /**
	 * 
	 */
    Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public HttpProxyEventType getType() {
        return type;
    }

    HttpProxyEventType type;

    Object attachment;

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public HttpProxyEvent(HttpProxyEventType type, Object source, Channel channel) {
        super(source);
        this.type = type;
        this.channel = channel;
    }
}
