package org.simpleframework.http.core;

import java.nio.channels.SocketChannel;
import org.simpleframework.util.select.Operation;

/**
 * The <code>Retry</code> object is used to retry to collect the
 * bytes to form a request entity. In order to retry the operation
 * the socket must be read ready. This is determined using the socket
 * object, which is registered with a selector. If at any point the
 * collection results in an error the operation is canceled and the
 * collector is closed, which shuts down the connection.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.util.select.Reactor
 */
class Retry implements Operation {

    /**
    * This is the selector used to process the collection events.
    */
    private final Selector source;

    /**
    * This is the collector used to consume the entity bytes.
    */
    private final Collector task;

    /**
    * This is the channel object associated with the collector.
    */
    private final Channel channel;

    /**
    * Constructor for the <code>Retry</code> object. This requires 
    * a selector and a collector object in order to consume the data
    * from the connected socket which forms a HTTP request entity.
    * 
    * @param source the selector object used to process events
    * @param task this is the task used to collect the entity
    */
    public Retry(Selector source, Collector task) {
        this.channel = task.getChannel();
        this.source = source;
        this.task = task;
    }

    /**
    * This is the <code>SocketChannel</code> used to determine if the
    * connection has some bytes that can be read. If it contains any
    * data then that data is read from and is used to compose the 
    * request entity, which consists of a HTTP header and body.
    * 
    * @return this returns the socket for the connected pipeline
    */
    public SocketChannel getChannel() {
        return task.getSocket();
    }

    /**
    * This <code>run</code> method is used to collect the bytes from
    * the connected channel. If a sufficient amount of data is read
    * from the socket to form a HTTP entity then the collector uses
    * the <code>Selector</code> object to dispatch the request. This
    * is sequence of events that occur for each transaction.
    */
    public void run() {
        try {
            task.collect(source);
        } catch (Throwable e) {
            cancel();
        }
    }

    /**
    * This is used to cancel the operation if it has timed out. If 
    * the retry is waiting too long to read content from the socket
    * then the retry is canceled and the underlying transport is 
    * closed. This helps to clean up occupied resources.     
    */
    public void cancel() {
        try {
            channel.close();
        } catch (Throwable e) {
            return;
        }
    }
}
