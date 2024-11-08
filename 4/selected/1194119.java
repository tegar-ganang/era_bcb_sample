package com.gargoylesoftware.base.trace;

import com.gargoylesoftware.base.util.DetailedNullPointerException;
import java.util.Date;

/**
 * An item of data to be written.
 *
 * @version $Revision: 1.4 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 */
public class TraceItem implements Cloneable {

    private TraceItem prevItem_;

    private TraceItem nextItem_;

    private TraceChannel channel_;

    private String message_;

    private Throwable throwable_;

    private Date time_;

    private Thread thread_;

    private Object lock_;

    /**
     * Create a new item.
     */
    public TraceItem() {
    }

    void clear() {
        message_ = null;
        throwable_ = null;
        time_ = null;
        thread_ = null;
        lock_ = null;
        channel_ = null;
    }

    /** @return true if the message has no content */
    boolean isClear() {
        return message_ == null && throwable_ == null && time_ == null && thread_ == null && lock_ == null && channel_ == null;
    }

    /**
     * Return a string representation of this object for testing purposes.
     * @return The string representation
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer(200);
        buffer.append("TraceItem[message=[");
        buffer.append(message_);
        buffer.append("] throwable=[");
        buffer.append(throwable_);
        buffer.append("] time=[");
        buffer.append(time_);
        buffer.append("] thread=[");
        buffer.append(thread_);
        buffer.append("] lock=[");
        buffer.append(lock_);
        buffer.append("] channel=[");
        buffer.append(channel_);
        buffer.append("]]");
        return buffer.toString();
    }

    /**
     * Return true if this item contains any text.
     * @return true if this item contains any text.
     */
    public boolean containsText() {
        return message_ != null | throwable_ != null;
    }

    /**
     * Return the message.
    * @return the message.
     */
    public String getMessage() {
        return message_;
    }

    /** @param string The message */
    void setMessage(final String string) {
        message_ = string;
    }

    /**
     * Return the throwable.
     * @return the throwable.
     */
    public Throwable getThrowable() {
        return throwable_;
    }

    /** @param t The throwable */
    void setThrowable(final Throwable t) {
        throwable_ = t;
    }

    /**
     * Return the time that the item was written to Trace
     * @return the time.
     */
    public Date getTime() {
        return time_;
    }

    /** @param time The time */
    void setTime(final Date time) {
        assertNotNull("time", time);
        time_ = time;
    }

    /** @return The thread */
    Thread getThread() {
        return thread_;
    }

    /** @param thread The thread */
    void setThread(final Thread thread) {
        assertNotNull("thread", thread);
        thread_ = thread;
    }

    /**
     * Return the name of the thread that called Trace.
     * @return the name of the thread.
     */
    public String getThreadName() {
        if (thread_ == null) {
            return null;
        }
        try {
            return thread_.getName();
        } catch (final NullPointerException e) {
            return "Unknown thread[hash=" + thread_.hashCode() + "]";
        }
    }

    /**
     * Return the lock.
     * @return the lock.
     */
    public Object getLock() {
        return lock_;
    }

    /** @param lock The lock */
    void setLock(final Object lock) {
        lock_ = lock;
    }

    /**
     * Return the previous item.
     * @return the previous item.
     */
    public TraceItem getPrevItem() {
        return prevItem_;
    }

    /** @param item The previous item */
    void setPrevItem(final TraceItem item) {
        prevItem_ = item;
    }

    /**
     * Return the next item.
     * @return the next item.
     */
    public TraceItem getNextItem() {
        return nextItem_;
    }

    /** @param item The next item */
    void setNextItem(final TraceItem item) {
        nextItem_ = item;
    }

    /**
     * Return the channel.
     * @return the channel.
     */
    public TraceChannel getChannel() {
        return channel_;
    }

    /** @param channel The channel */
    void setChannel(final TraceChannel channel) {
        assertNotNull("channel", channel);
        channel_ = channel;
    }

    /**
     * Return a copy of this object.
     * @return A copy.
     * @throws CloneNotSupportedException If this object is not cloneable.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
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
