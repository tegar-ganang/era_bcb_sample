package com.hongbo.cobweb.nmr.runtime.impl;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.jbi.servicedesc.ServiceEndpoint;
import com.hongbo.cobweb.nmr.api.Channel;
import com.hongbo.cobweb.nmr.api.Endpoint;
import com.hongbo.cobweb.nmr.api.Exchange;
import com.hongbo.cobweb.nmr.CobwebException;
import com.hongbo.cobweb.nmr.core.ChannelImpl;

/**
 */
public class EndpointImpl extends ServiceEndpointImpl implements Endpoint {

    private Channel channel;

    private BlockingQueue<Exchange> queue;

    public EndpointImpl(Map<String, ?> properties) {
        super(properties);
    }

    public void process(Exchange exchange) {
        if (exchange.getProperty(ServiceEndpoint.class) == null) {
            exchange.setProperty(ServiceEndpoint.class, this);
        }
        try {
            queue.offer(exchange, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new CobwebException(e);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        if (channel instanceof ChannelImpl) {
            ((ChannelImpl) channel).setShouldRunSynchronously(true);
        }
    }

    public BlockingQueue<Exchange> getQueue() {
        return queue;
    }

    public void setQueue(BlockingQueue<Exchange> queue) {
        this.queue = queue;
    }

    public boolean equals(Object o) {
        return this == o;
    }

    public int hashCode() {
        return super.hashCode();
    }
}
