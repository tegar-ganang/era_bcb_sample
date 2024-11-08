package org.jucetice.javascript.utils.log;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.logging.Log;

/**
 * A simple logger that writes to a PrintStream such as System.out.
 */
public class Logger implements Log {

    List entries = Collections.synchronizedList(new LinkedList());

    PrintWriter writer;

    String canonicalName;

    String logName;

    static DateFormat dformat = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss] ");

    static long dateLastRendered;

    static String dateCache;

    public static final int TRACE = 1;

    public static final int DEBUG = 2;

    public static final int INFO = 3;

    public static final int WARN = 4;

    public static final int ERROR = 5;

    public static final int FATAL = 6;

    int logLevel = TRACE;

    long lastMessage = System.currentTimeMillis();

    private Log sedatedLog = new SedatedLog();

    /**
     * zero argument constructor, only here for FileLogger subclass
     */
    public Logger() {
    }

    /**
     * one argument constructor, only here for FileLogger subclass
     */
    public Logger(int newLogLevel) {
        logLevel = newLogLevel;
        logName = "";
    }

    /**
     * Create a logger for a PrintStream, such as System.out.
     * @param out the output stream
     */
    public Logger(PrintStream out) {
        writer = new PrintWriter(out);
        canonicalName = out.toString();
        logName = "";
    }

    /**
     * Create a logger for a PrintStream, such as System.out.
     * @param out the output stream
     */
    public Logger(PrintStream out, int newLogLevel) {
        logLevel = newLogLevel;
        writer = new PrintWriter(out);
        canonicalName = out.toString();
        logName = "";
    }

    /**
     * Get the current log level.
     * @return the current log level
     */
    public int getLogLevel() {
        return logLevel;
    }

    /**
     * Set the log level for this logger.
     * @param logLevel the new log level
     */
    public void setLogLevel(int level) {
        this.logLevel = level;
    }

    /**
     * Get loglevel from System properties
     */
    public void setLogLevel(String level) {
        if ("trace".equalsIgnoreCase(level)) logLevel = TRACE; else if ("debug".equalsIgnoreCase(level)) logLevel = DEBUG; else if ("info".equalsIgnoreCase(level)) logLevel = INFO; else if ("warn".equalsIgnoreCase(level)) logLevel = WARN; else if ("error".equalsIgnoreCase(level)) logLevel = ERROR; else if ("fatal".equalsIgnoreCase(level)) logLevel = FATAL;
    }

    /**
     * Return a string representation of this Logger
     */
    public String toString() {
        return new StringBuilder(getClass().getName()).append("[").append(canonicalName).append(",").append(logLevel).append("]").toString();
    }

    /**
     * Return an object  which identifies  this logger.
     * @return the canonical name of this logger
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * Get the current log level.
     * @return the current log level
     */
    public String getLogName() {
        return logName;
    }

    /**
     * Set the log level for this logger.
     * @param logLevel the new log level
     */
    public void setLogName(String name) {
        this.logName = name;
    }

    /**
     * Append a message to the log.
     * @param level a string representing the log level
     * @param msg the log message
     * @param exception an exception, or null
     */
    protected void log(String level, Object msg, Throwable exception) {
        lastMessage = System.currentTimeMillis();
        if ((lastMessage - 1000) > dateLastRendered) {
            renderDate();
        }
        if (entries.size() < 2000) {
            String message = msg == null ? "null" : msg.toString();
            entries.add(new Entry(dateCache, level, message, "", exception));
        }
    }

    /**
     * This is called by the runner thread to perform actual output.
     */
    protected synchronized void write() {
        if (entries.isEmpty()) {
            return;
        }
        try {
            ensureOpen();
            int l = entries.size();
            for (int i = 0; i < l; i++) {
                Entry entry = (Entry) entries.remove(0);
                writer.print(entry.date);
                writer.print(entry.level);
                writer.print(entry.threadId);
                writer.println(entry.message);
                if (entry.exception != null) entry.exception.printStackTrace(writer);
            }
            writer.flush();
        } catch (Exception x) {
            int size = entries.size();
            if (size > 1000) {
                System.err.println("Error writing log file " + this + ": " + x);
                System.err.println("Discarding " + size + " log entries.");
                entries.clear();
            }
        }
    }

    /**
     * This is called by the runner thread to to make sure we have an open writer.
     */
    protected void ensureOpen() {
    }

    protected static synchronized void renderDate() {
        Date date = new Date();
        dateCache = dformat.format(date);
        dateLastRendered = date.getTime();
    }

    public boolean isTraceEnabled() {
        return logLevel <= TRACE;
    }

    public boolean isDebugEnabled() {
        return logLevel <= DEBUG;
    }

    public boolean isInfoEnabled() {
        return logLevel <= INFO;
    }

    public boolean isWarnEnabled() {
        return logLevel <= WARN;
    }

    public boolean isErrorEnabled() {
        return logLevel <= ERROR;
    }

    public boolean isFatalEnabled() {
        return logLevel <= FATAL;
    }

    public void trace(Object parm1) {
        if (logLevel <= TRACE) log("[" + logName + "][TRACE] ", parm1, null);
    }

    public void trace(Object parm1, Throwable parm2) {
        if (logLevel <= TRACE) log("[" + logName + "][TRACE] ", parm1, parm2);
    }

    public void debug(Object parm1) {
        if (logLevel <= DEBUG) log("[" + logName + "][DEBUG] ", parm1, null);
    }

    public void debug(Object parm1, Throwable parm2) {
        if (logLevel <= DEBUG) log("[" + logName + "][DEBUG] ", parm1, parm2);
    }

    public void info(Object parm1) {
        if (logLevel <= INFO) log("[" + logName + "][INFO]  ", parm1, null);
    }

    public void info(Object parm1, Throwable parm2) {
        if (logLevel <= INFO) log("[" + logName + "][INFO]  ", parm1, parm2);
    }

    public void warn(Object parm1) {
        if (logLevel <= WARN) log("[" + logName + "][WARN]  ", parm1, null);
    }

    public void warn(Object parm1, Throwable parm2) {
        if (logLevel <= WARN) log("[" + logName + "][WARN]  ", parm1, parm2);
    }

    public void error(Object parm1) {
        if (logLevel <= ERROR) log("[" + logName + "][ERROR] ", parm1, null);
    }

    public void error(Object parm1, Throwable parm2) {
        if (logLevel <= ERROR) log("[" + logName + "][ERROR] ", parm1, parm2);
    }

    public void fatal(Object parm1) {
        if (logLevel <= FATAL) log("[" + logName + "][FATAL] ", parm1, null);
    }

    public void fatal(Object parm1, Throwable parm2) {
        if (logLevel <= FATAL) log("[" + logName + "][FATAL] ", parm1, parm2);
    }

    public static String getStackTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        writer.close();
        return stringWriter.toString();
    }

    class Entry {

        final String date, level, message, threadId;

        final Throwable exception;

        Entry(String newDate, String newLevel, String newMessage, String newThreadId, Throwable newException) {
            this.date = newDate;
            this.level = newLevel;
            this.message = newMessage;
            this.threadId = newThreadId;
            this.exception = newException;
        }
    }

    /**
     * return a "quiet" version of this log that routes debug() output to trace()
     * @return a possibly less verbose version of this log.
     */
    protected Log getSedatedLog() {
        return sedatedLog;
    }

    class SedatedLog implements Log {

        public void debug(Object o) {
            Logger.this.trace(o);
        }

        public void debug(Object o, Throwable t) {
            Logger.this.trace(o, t);
        }

        public void error(Object o) {
            Logger.this.error(o);
        }

        public void error(Object o, Throwable t) {
            Logger.this.error(o, t);
        }

        public void fatal(Object o) {
            Logger.this.fatal(o);
        }

        public void fatal(Object o, Throwable t) {
            Logger.this.fatal(o, t);
        }

        public void info(Object o) {
            Logger.this.info(o);
        }

        public void info(Object o, Throwable t) {
            Logger.this.info(o, t);
        }

        public void trace(Object o) {
        }

        public void trace(Object o, Throwable t) {
        }

        public void warn(Object o) {
            Logger.this.warn(o);
        }

        public void warn(Object o, Throwable t) {
            Logger.this.warn(o, t);
        }

        public boolean isDebugEnabled() {
            return Logger.this.isTraceEnabled();
        }

        public boolean isErrorEnabled() {
            return Logger.this.isErrorEnabled();
        }

        public boolean isFatalEnabled() {
            return Logger.this.isFatalEnabled();
        }

        public boolean isInfoEnabled() {
            return Logger.this.isInfoEnabled();
        }

        public boolean isTraceEnabled() {
            return false;
        }

        public boolean isWarnEnabled() {
            return Logger.this.isWarnEnabled();
        }
    }
}
