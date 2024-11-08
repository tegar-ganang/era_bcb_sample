package org.simpleframework.http.transport;

import java.nio.channels.SocketChannel;
import org.simpleframework.util.select.Operation;

/**
 * The <code>Dispatcher</code> operation is used transfer a transport
 * to the negotiator so it can be processed. This is uses so that
 * when a pipeline is given to the processor it can be dispatched
 * in another thread to the transporter. This is needed so that the
 * connection thread is occupied only briefly.
 * 
 * @author Niall Gallagher
 */
class Dispatcher implements Operation {

    /**
    * This is the negotiator used to transfer the transport to. 
    */
    private final Negotiator negotiator;

    /**
    * This is the transport to be passed to the negotiator.
    */
    private final Transport transport;

    /**
    * Constructor for the <code>Dispatcher</code> object. This is used
    * to transfer a transport to a negotiator. Transferring the
    * transport using an operation ensures that the thread that is
    * used to process the pipeline is not occupied for long.
    * 
    * @param transport this is the transport this exchange uses
    * @param negotiator this is the negotiation to dispatch to
    */
    public Dispatcher(Transport transport, Negotiator negotiator) {
        this.negotiator = negotiator;
        this.transport = transport;
    }

    /**
    * This is the <code>SelectableChannel</code> which is used to 
    * determine if the operation should be executed. If the channel   
    * is ready for a given I/O event it can be run. For instance if
    * the operation is used to perform some form of read operation
    * it can be executed when ready to read data from the channel.
    *
    * @return this returns the channel used to govern execution
    */
    public SocketChannel getChannel() {
        return transport.getSocket();
    }

    /**
    * This is used to transfer the transport to the negotiator. This
    * will typically be executed asynchronously so that it does not
    * delay the thread that passes the <code>Pipeline</code> to the
    * transport processor, ensuring quicker processing.
    */
    public void run() {
        try {
            negotiator.process(transport);
        } catch (Exception e) {
            cancel();
        }
    }

    /**
    * This is used to cancel the operation if it has timed out. This
    * is typically invoked when it has been waiting in a selector for
    * an extended duration of time without any active operations on
    * it. In such a case the reactor must purge the operation to free
    * the memory and open channels associated with the operation.
    */
    public void cancel() {
        try {
            transport.close();
        } catch (Exception e) {
            return;
        }
    }
}
