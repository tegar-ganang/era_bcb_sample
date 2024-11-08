package com.gargoylesoftware.base.trace;

import com.gargoylesoftware.base.util.DetailedNullPointerException;
import com.gargoylesoftware.base.util.StringUtil;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * <p style="color: orange">Internal use only.</p>.
 * <p>A dispatcher for TraceItems</p>
 *
 * @version $Revision: 1.8 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 */
public class TraceItemDispatcher implements Runnable {

    private static final Format TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm");

    private static final int BUFFER_ENABLED = 1;

    private static final int BUFFER_SHUTTING_DOWN = 2;

    private static final int BUFFER_DISABLED = 3;

    private static int bufferStatus_ = BUFFER_DISABLED;

    private final TraceItemQueue traceQueue_ = new TraceItemQueue();

    private TraceItemQueue cacheTraceItemQueue_ = new TraceItemQueue();

    private int cacheMaxSize_ = 50;

    /**
     *
     */
    public TraceItemDispatcher() {
        new Thread(this, "TraceItemDispatcher Thread").start();
        final Runtime runtime = Runtime.getRuntime();
        try {
            final Method method = runtime.getClass().getMethod("addShutdownHook", new Class[] { Thread.class });
            final Thread thread = new Thread() {

                public void run() {
                    Trace.getController().setBufferingEnabled(false);
                }
            };
            method.invoke(runtime, new Object[] { thread });
        } catch (NoSuchMethodException e) {
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.getTargetException().printStackTrace();
        }
    }

    /**
     *
     */
    public void run() {
        TraceItem item;
        bufferStatus_ = BUFFER_ENABLED;
        try {
            while (true) {
                try {
                    item = traceQueue_.pop();
                    if (item == null) {
                        Thread.sleep(500);
                    } else {
                        dumpTraceElement(item);
                    }
                } catch (final Exception e) {
                    System.out.print("Exception when printing debug information e=");
                    e.printStackTrace();
                }
            }
        } catch (final Throwable t) {
            System.out.print("Exception when printing debug information e=");
            t.printStackTrace();
        }
    }

    /**
     * Format an item and print it to standard out.
     * @param item the item to print.
     */
    private void dumpTraceElement(final TraceItem item) {
        assertNotNull("item", item);
        final TraceChannel channel = item.getChannel();
        if (channel != null && channel.isEnabled() && item.containsText()) {
            final Set traceWriters = channel.getTraceWriters();
            synchronized (traceWriters) {
                final Iterator iterator = traceWriters.iterator();
                if (iterator.hasNext() == false) {
                    defaultTraceWriter(item);
                } else {
                    while (iterator.hasNext()) {
                        ((TraceWriter) iterator.next()).write(item);
                    }
                }
            }
        }
        final Object lock = item.getLock();
        if (lock != null) {
            synchronized (lock) {
                lock.notify();
            }
            return;
        }
        disposeTraceItem(item);
    }

    /**
     * Provide default behaviour
     * @param item The item to print
     */
    private static void defaultTraceWriter(final TraceItem item) {
        final PrintStream outStream = Trace.getController().getRealSystemOut();
        final StringBuffer prefixBuffer = new StringBuffer();
        prefixBuffer.append("[");
        final Date timestamp = item.getTime();
        if (timestamp != null) {
            prefixBuffer.append(TIMESTAMP_FORMAT.format(timestamp));
            prefixBuffer.append(" ");
        }
        final String threadName = item.getThreadName();
        if (threadName != null) {
            prefixBuffer.append(threadName);
        }
        prefixBuffer.append("] ");
        final String prefix = prefixBuffer.toString();
        final String message = item.getMessage();
        if (message != null) {
            outStream.print(prefix);
            outStream.print(StringUtil.expandTabs(message, 3));
        }
        final Throwable throwable = item.getThrowable();
        if (throwable != null) {
            int i;
            final String strings[] = Trace.throwableToStringArray(throwable);
            outStream.print(prefix);
            outStream.println(strings[0]);
            final String blanks = StringUtil.nCopies(prefix.length(), ' ');
            for (i = 1; i < strings.length; i++) {
                outStream.print(blanks);
                outStream.println(StringUtil.expandTabs(strings[i], 3));
            }
        }
    }

    /**
     * Get the queue.
     * @return The queue.
     */
    public TraceItemQueue getTraceItemQueue() {
        return traceQueue_;
    }

    /**
     * Add an item to the trace queue.
     * @param item The item to add.
     */
    public void dispatch(final TraceItem item) {
        item.setThread(Thread.currentThread());
        item.setTime(new Date());
        switch(bufferStatus_) {
            case BUFFER_ENABLED:
                traceQueue_.push(item);
                break;
            case BUFFER_SHUTTING_DOWN:
                flush();
                dumpTraceElement(item);
                break;
            case BUFFER_DISABLED:
                dumpTraceElement(item);
                break;
            default:
                throw new IllegalStateException("Unexpected value: bufferStatus_=" + bufferStatus_);
        }
    }

    private synchronized void waitForQueueToEmpty() {
        while (traceQueue_.size() != 0) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Set whether or not to buffer the output of the trace calls.  Buffering
     * will increase perceived performance significantly.
     * @param enabled True if buffering should be enabled
     */
    public synchronized void setBufferingEnabled(final boolean enabled) {
        if (enabled) {
            if (bufferStatus_ == BUFFER_SHUTTING_DOWN) {
                flush();
            }
            if (bufferStatus_ == BUFFER_DISABLED) {
                bufferStatus_ = BUFFER_ENABLED;
            }
        } else {
            if (bufferStatus_ == BUFFER_ENABLED) {
                bufferStatus_ = BUFFER_SHUTTING_DOWN;
                flush();
                bufferStatus_ = BUFFER_DISABLED;
            }
        }
    }

    /**
     * Return true if buffering is enabled.
     * @return true if buffering is enabled.
     */
    public boolean isBufferingEnabled() {
        return bufferStatus_ == BUFFER_ENABLED;
    }

    /**
     *
     */
    public void flush() {
        switch(bufferStatus_) {
            case BUFFER_ENABLED:
                final TraceItem item = getNewTraceItem();
                final Object lock = new Object();
                item.setLock(lock);
                try {
                    synchronized (lock) {
                        dispatch(item);
                        lock.wait();
                    }
                } catch (final InterruptedException e) {
                }
                break;
            case BUFFER_SHUTTING_DOWN:
                waitForQueueToEmpty();
                break;
            case BUFFER_DISABLED:
                return;
            default:
                throw new IllegalStateException("Unexpected value: bufferStatus_=" + bufferStatus_);
        }
    }

    /**
     * Return a trace item
     * @return The new trace item.
     */
    public TraceItem getNewTraceItem() {
        TraceItem item = cacheTraceItemQueue_.pop();
        if (item == null) {
            item = new TraceItem();
        }
        return item;
    }

    /**
     * Dispose of a trace item.  Disposing will put the trace item back on a queue for reuse.
     * @param item The item to dispose.
     */
    public void disposeTraceItem(final TraceItem item) {
        if (cacheTraceItemQueue_.size() < cacheMaxSize_) {
            item.clear();
            cacheTraceItemQueue_.push(item);
        }
    }

    /**
     * Verify that the specified value is not null.  If it is then throw an exception
     *
     * @param fieldName The name of the field to check
     * @param fieldValue The value of the field to check
     * @exception DetailedNullPointerException If fieldValue is null
     */
    protected final void assertNotNull(final String fieldName, final Object fieldValue) throws DetailedNullPointerException {
        if (fieldValue == null) {
            throw new DetailedNullPointerException(fieldName);
        }
    }
}
