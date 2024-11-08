package org.freehold.jukebox.logger;

import java.net.URL;
import org.freehold.jukebox.conf.Configurable;
import org.freehold.jukebox.conf.Configuration;
import org.freehold.jukebox.conf.ConfigurationChangeListener;
import org.freehold.jukebox.conf.ConfigurableHelper;

/**
 * This class provides the implementation for the common features of all the
 * syslogs, such as configuration support, start/stop control and filtering
 * facility.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2000
 * @version $Id: AbstractLogTarget.java,v 1.17 2001-10-16 23:39:04 vtt Exp $
 * @since Jukebox v4 2.0.p10
 */
public abstract class AbstractLogTarget extends ConfigurableHelper implements LogTarget, LogLevels, ConfigurationChangeListener {

    /**
     * True if enabled.
     *
     * @see #open
     * @see #close
     */
    private boolean enabled = false;

    /**
     * Facility filter.
     *
     * May be <code>null</code>, in this case no filtering happens at all.
     */
    private LogFilter filter = null;

    /**
     * Create a new instance.
     *
     * This instance would be useless until {@link #configure configure()}d.
     */
    public AbstractLogTarget() {
    }

    /**
     * Create a configured instance.
     *
     * This instance will be immediately ready to be {@link #open open()}ed.
     */
    public AbstractLogTarget(String configurationRoot, Configuration conf) {
        configure(configurationRoot, conf);
    }

    public void configure() {
        String filterConfigurationRoot = getConfigurationRoot() + ".filter";
        String filterClassName = getConfiguration().getString(filterConfigurationRoot + ".class");
        if (filterClassName != null) {
            try {
                Class filterClass = Class.forName(filterClassName);
                filter = (LogFilter) filterClass.newInstance();
                if (filter instanceof Configurable) {
                    ((Configurable) filter).configure(getConfigurationRoot(), getConfiguration());
                }
            } catch (Throwable t) {
                System.err.println("Unrecoverable exception, filter is not set:");
                t.printStackTrace();
            }
        }
    }

    /**
     * Start working.
     *
     * @exception IllegalStateException if not configured yet.
     */
    public synchronized void open() {
        if (enabled) {
            logMessage(new LogRecord(null, Thread.currentThread(), this, LOG_DEBUG, LogAware.CH_LOGGER, null, new IllegalStateException("open(): already open")));
            return;
        }
        getConfiguration();
        try {
            start();
            enabled = true;
            if (filter != null) {
                logMessage(new LogRecord(null, Thread.currentThread(), this, LOG_INFO, LogAware.CH_LOGGER, "Filter: " + filter.getClass().getName() + "#" + Integer.toHexString(filter.hashCode()), null));
            }
        } catch (Throwable t) {
            System.err.println(getConfigurationRoot() + ": can't open, cause:");
            t.printStackTrace();
        }
    }

    /**
     * Stop accepting the messages and close whatever media we were writing
     * to.
     */
    public synchronized void close() {
        enabled = false;
        stop();
    }

    /**
     * Only the subclasses have a business to know if we're enabled or not.
     *
     * @return <code>true</code> if we're enabled.
     */
    protected boolean isEnabled() {
        return enabled;
    }

    /**
     * Log the message.
     *
     * @param lr Log record to log.
     */
    public final void logMessage(LogRecord lr) {
        if (!enabled) {
            new IllegalStateException("logger not enabled for " + lr.target.getClass().getName() + "@" + Integer.toHexString(lr.target.hashCode()) + ", logging the message anyway").printStackTrace();
        }
        if (lr == null) {
            write(new LogRecord(null, Thread.currentThread(), this, LOG_ALERT, LogAware.CH_LOGGER, "null log record was supplied, you may want to check what's going on", new IllegalArgumentException("Trace this to see where the null record originated from")));
            return;
        }
        if (filter == null || filter.isEnabled(lr)) {
            write(lr);
        }
    }

    public void configurationChanged(URL confURL) {
    }

    protected abstract void start();

    protected abstract void stop();

    protected abstract void write(LogRecord lr);
}
