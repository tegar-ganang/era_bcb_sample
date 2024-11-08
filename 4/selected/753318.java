package org.hypergraphdb.app.dataflow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * A port establishes the link between a channel and a processor. {@link InputPort}s
 * are for reading by {@link Processor}s from specific {@link Channel}s 
 * while {@link OutputPort}s are for writing.    
 * </p>
 * 
 * @author muriloq
 * 
 * @param <V>
 */
public class Port<V> {

    private static Log log = LogFactory.getLog(Port.class);

    private boolean isOpen;

    protected Channel<V> channel;

    protected Port() {
    }

    Port(Channel<V> channel) {
        this.channel = channel;
    }

    public Channel<V> getChannel() {
        return channel;
    }

    public void setChannel(Channel<V> channel) {
        this.channel = channel;
    }

    public synchronized void close() throws InterruptedException {
        if (!isOpen) return;
        isOpen = false;
        log.debug(this + " closed");
    }

    public synchronized void open() {
        isOpen = true;
        log.debug(this + " opened");
        notifyAll();
    }

    public synchronized void await() throws InterruptedException {
        while (!isOpen) wait();
    }

    public synchronized boolean isOpen() {
        return isOpen;
    }
}
