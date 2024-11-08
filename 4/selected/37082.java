package blueprint4j.utils;

import java.io.*;

public abstract class Logging {

    public static final Level DEV = new Level("Dev");

    public static final Level TRACE = new Level("Trace");

    public static final Level DEBUG = new Level("Debug");

    public static final Level CRITICAL = new Level("Critical");

    public static final Level COMM = new Level("Comm");

    public static final Level MESSAGE = new Level("Message");

    boolean log_dev = false;

    boolean log_debug = true;

    boolean log_trace = true;

    boolean log_critical = true;

    boolean log_comm = false;

    boolean log_message = false;

    public Logging(boolean debug, boolean trace, boolean critical) {
        log_debug = debug;
        log_trace = trace;
        log_critical = critical;
    }

    public Logging(boolean debug, boolean trace, boolean critical, boolean comm) {
        log_debug = debug;
        log_trace = trace;
        log_critical = critical;
        log_comm = comm;
    }

    public Logging(boolean debug, boolean dev, boolean trace, boolean critical, boolean comm) {
        log_debug = debug;
        log_dev = dev;
        log_trace = trace;
        log_critical = critical;
        log_comm = comm;
    }

    public Logging(boolean debug, boolean dev, boolean trace, boolean critical, boolean comm, boolean message) {
        log_debug = debug;
        log_dev = dev;
        log_trace = trace;
        log_critical = critical;
        log_comm = comm;
        log_message = message;
    }

    public void setTrace(boolean flag) {
        log_trace = flag;
    }

    public void setDebug(boolean flag) {
        log_debug = flag;
    }

    public void setCritical(boolean flag) {
        log_critical = flag;
    }

    public void setComm(boolean flag) {
        log_comm = flag;
    }

    public void setMessage(boolean flag) {
        log_message = flag;
    }

    /**
	* close and flush any log destinations
	*/
    public void close() {
    }

    public final void writeLog(Level lvl, long thread_id, String description, Throwable exception) {
        String detail = "";
        StackTraceElement[] stack_traces = exception.getStackTrace();
        for (int i = 0; i < stack_traces.length; i++) {
            detail += stack_traces[i].toString() + "\r";
        }
        writeLog(lvl, thread_id, description, exception.getMessage() + "\r" + detail);
    }

    public final void writeLog(Level lvl, long thread_id, String description, String details) {
        if (lvl == DEV && log_dev) {
            write(DEV.toString(), thread_id, description, details);
        }
        if (lvl == TRACE && log_trace) {
            write(TRACE.toString(), thread_id, description, details);
        }
        if (lvl == DEBUG && log_debug) {
            write(DEBUG.toString(), thread_id, description, details);
        }
        if (lvl == CRITICAL && log_critical) {
            write(CRITICAL.toString(), thread_id, description, details);
            ThreadSchedule.haltBlocking();
        }
        if (lvl == COMM && log_comm) {
            write(COMM.toString(), thread_id, description, details);
        }
        if (lvl == MESSAGE && log_message) {
            write(MESSAGE.toString(), thread_id, description, details);
        }
    }

    public abstract void write(String level, long thread_id, String description, String details);

    public abstract LogDataUnitVector findLogs(Integer tid, java.util.Date from_date, java.util.Date to_date, Logging.Level levels[]) throws IOException;

    public static class Level {

        private String name;

        Level(String p_name) {
            name = p_name;
        }

        public String toString() {
            return name;
        }
    }
}
