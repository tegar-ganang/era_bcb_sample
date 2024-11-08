package org.freehold.jukebox.logger;

public class SimpleLogger implements Logger {

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

    public SimpleLogger(LogChannel logChannel) {
        this(null, logChannel);
    }

    public SimpleLogger(LogTarget target, LogChannel logChannel) {
        this.target = target;
        this.logChannel = logChannel;
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
    public void complain(Object originator, LogLevel ll, LogChannel channel, Object message, Throwable t) {
        if (target == null) {
            throw new IllegalStateException("You must attach() to the log target before using the logger");
        }
        target.logMessage(new LogRecord(logChannel, Thread.currentThread(), originator, ll, channel, message, t));
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
            target.logMessage(new LogRecord(logChannel, Thread.currentThread(), this, LogLevels.LOG_NOTICE, logChannel, "Further messages redirected to: " + target, null));
        }
        this.target = target;
    }

    public void flush() {
    }

    public void close() {
    }
}
