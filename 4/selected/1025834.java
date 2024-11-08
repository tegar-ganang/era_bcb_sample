package org.freehold.jukebox.logger;

import java.util.LinkedList;

/**
 * Buffered logger.
 *
 * Takes up the log records, queues them and disposes of them in the background thread.
 *
 * <h3>IMPORTANT</h3>
 *
 * Make sure you don't have these loggers hanging around - since they're not daemon threads, your JVM will never exit.
 *
 * @version $Id: BufferedLogger.java,v 1.2 2001-01-17 23:01:02 vtt Exp $
 */
public class BufferedLogger implements Logger {

    /**
     * The log target this logger writes to.
     *
     * May be <code>null</code>.
     */
    protected LogTarget target;

    /**
     * This logger's channel.
     */
    private LogChannel logChannel;

    /**
     * The buffer.
     */
    private LinkedList theQueue = new LinkedList();

    private boolean enabled;

    public BufferedLogger(LogChannel logChannel) {
        this(null, logChannel);
    }

    public BufferedLogger(LogTarget target, LogChannel logChannel) {
        this.target = target;
        this.logChannel = logChannel;
        enabled = true;
        (new Buffer()).start();
    }

    /**
     * Accept a log message.
     *
     * @param originator The object on whose behalf the message is logged.
     *
     * @param ll The severity level.
     *
     * @param channel The channel the message belongs to.
     *
     * @param message The message itself.
     *
     * @param t The exception logged along with the message.
     */
    public synchronized void complain(Object originator, LogLevel ll, LogChannel channel, Object message, Throwable t) {
        if (target == null) {
            throw new IllegalStateException("You must attach() to the log target before using the logger");
        }
        theQueue.addFirst(new LogRecord(logChannel, Thread.currentThread(), originator, ll, channel, message, t));
        notify();
    }

    /**
     * Get this logger's channel.
     *
     * @return The logger channel.
     */
    public LogChannel getChannel() {
        return logChannel;
    }

    /**
     * Attach the logger to the log target.
     *
     * The logger will be useless unless attached to the log target.
     */
    public void attach(LogTarget target) {
        if (target != null) {
            theQueue.addFirst(new LogRecord(logChannel, Thread.currentThread(), this, LogLevels.LOG_NOTICE, logChannel, "Further messages redirected to: " + target, null));
        }
        this.target = target;
    }

    public void flush() {
        while (!theQueue.isEmpty()) {
            target.logMessage((LogRecord) theQueue.removeLast());
        }
    }

    protected synchronized LogRecord getNextRecord() throws InterruptedException {
        while (theQueue.isEmpty() && enabled) {
            wait();
        }
        if (!enabled) {
            return null;
        }
        return (LogRecord) theQueue.removeLast();
    }

    public synchronized void close() {
        enabled = false;
        flush();
        notify();
    }

    protected class Buffer extends Thread {

        public void run() {
            try {
                while (enabled) {
                    LogRecord lr = getNextRecord();
                    if (lr != null) {
                        target.logMessage(lr);
                    }
                }
                target.logMessage(new LogRecord(logChannel, Thread.currentThread(), this, LogLevels.LOG_NOTICE, logChannel, "Logging stopped", null));
            } catch (InterruptedException iex) {
                target.logMessage(new LogRecord(logChannel, Thread.currentThread(), this, LogLevels.LOG_WARNING, logChannel, "Logging interrupted - closing, cause:", iex));
            }
            close();
        }
    }
}
