package org.simpleframework.http.transport;

import java.nio.channels.SocketChannel;
import org.simpleframework.util.select.Operation;
import org.simpleframework.util.select.Reactor;

/**
 * The <code>SocketFlusher</code> flushes bytes to the underlying
 * socket channel. This allows asynchronous writes to the socket
 * to be managed in such a way that there is order to the way data
 * is delivered over the socket. This uses a selector to dispatch
 * flush invocations to the underlying socket when the socket is
 * read ready. This allows the writing thread to continue without
 * having to wait for all the data to be written to the socket.
 *
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.transport.Controller
 */
class SocketFlusher implements Flusher {

    /**
    * This is the signaller used to determine when to flush.
    */
    private Signaller signaller;

    /**
    * This is the scheduler used to block and signal the writer.
    */
    private Scheduler scheduler;

    /**
    * This is the coalescer used to queue the packets written.
    */
    private Writer coalescer;

    /**
    * This is used to determine if the socket flusher is closed.
    */
    private boolean closed;

    /**
    * Constructor for the <code>SocketFlusher</code> object. This is
    * used to flush buffers to the underlying socket asynchronously.
    * When finished flushing all of the buffered data this signals
    * any threads that are blocking waiting for the write to finish.
    *
    * @param reactor this is used to perform asynchronous writes
    * @param coalescer this is used to write the buffered packets
    */
    public SocketFlusher(Reactor reactor, Writer coalescer) throws Exception {
        this.signaller = new Signaller(coalescer);
        this.scheduler = new Scheduler(reactor, signaller, this);
        this.coalescer = coalescer;
    }

    /**
    * Here in this method we schedule a flush when the underlying
    * writer is write ready. This allows the writer thread to return
    * without having to fully flush the content to the underlying
    * transport. To block the other flush method can be used.
    */
    public synchronized void flush() throws Exception {
        flush(false);
    }

    /**
    * Here in this method we schedule a flush when the underlying
    * writer is write ready. This allows the writer thread to return
    * without having to fully flush the content to the underlying
    * transport. To block this should invoked with true.
    *
    * @param block determines if the flush method should block
    */
    public synchronized void flush(boolean block) throws Exception {
        if (closed) {
            throw new TransportException("Flusher is closed");
        }
        scheduler.schedule(block);
    }

    /**
    * This is executed when the flusher is to write all of the data to
    * the underlying socket. In this situation the writes are attempted
    * in a non blocking way, if the task does not complete then this
    * will simply enqueue the writing task for OP_WRITE and leave the
    * method. This returns true if all the buffers are written.
    */
    private synchronized void execute() throws Exception {
        boolean ready = coalescer.flush();
        if (!ready) {
            scheduler.repeat();
        } else {
            scheduler.ready();
        }
    }

    /**
    * This is invoked when the flush operation times out. Expiration
    * is a result of waiting for read readiness beyond the allocated
    * time period. On such an occasion the socket is closed and the
    * data remains unsent. This avoids the situation where a thread
    * blocks on the write operation too long.
    */
    private synchronized void close() throws Exception {
        closed = true;
        scheduler.ready();
        coalescer.close();
    }

    /**
    * The <code>Signaller</code> is an operation that performs the
    * write operation asynchronously. This will basically determine
    * if the socket is write ready and drain each queued buffer to
    * the socket until there are no more pending buffers.
    *
    * @author Niall Gallagher
    */
    private class Signaller implements Operation {

        /**
       * This is the coalescer that is used to write the data.
       */
        private final Writer coalescer;

        /**
       * Constructor for the <code>Signaller</code> object. This will
       * create an operation that is used to flush the packet queue
       * to the underlying socket. This ensures that the data is
       * written to the socket in the queued order.
       *
       * @param pipeline this is the pipeline to flush the data to
       */
        public Signaller(Writer coalescer) {
            this.coalescer = coalescer;
        }

        /**
       * This returns the socket channel for the connected pipeline. It
       * is this channel that is used to determine if there are bytes
       * that can be written. When closed this is no longer selectable.
       *
       * @return this returns the connected channel for the pipeline
       */
        public SocketChannel getChannel() {
            return coalescer.getSocket();
        }

        /**
       * This is used to perform the drain of the pending buffer
       * queue. This will drain each pending queue if the socket is
       * write ready. If the socket is not write ready the operation
       * is enqueued for selection and this returns. This ensures
       * that all the data will eventually be delivered.
       */
        public void run() {
            try {
                execute();
            } catch (Exception e) {
                cancel();
            }
        }

        /**
       * This is used to cancel the operation if it has timed out.
       * If the delegate is waiting too long to flush the contents
       * of the buffers to the underlying transport then the socket
       * is closed and the flusher times out to avoid deadlock.
       */
        public void cancel() {
            try {
                close();
            } catch (Exception e) {
                return;
            }
        }
    }
}
