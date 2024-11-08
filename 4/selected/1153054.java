package edu.regis.jprobe.model;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger {

    private static Logger instance;

    private PrintStream outPrintStream;

    protected PrintStream errPrintStream;

    private PrintStream oldErrPrintStream;

    private PrintStream oldOutPrintStream;

    protected DataOutputStream out = null;

    protected LoggerThread loggerThread;

    protected final BlockingQueue<String> queue;

    protected static final String PATH_SEP = System.getProperty("path.separator");

    protected static final String FILE_SEP = System.getProperty("file.separator");

    protected static final String NEWLINE = System.getProperty("line.separator");

    private boolean echoToOut = false;

    private static boolean lockLogFile = false;

    private int loggingLevel = 1;

    private int onlineLogCapacity = 0;

    private List<String> logList;

    public static final int DEBUG = 0;

    public static final int INFO = 1;

    public static final int WARNING = 2;

    public static final int ERROR = 3;

    public static final int SEVERE = 4;

    public static final int STDOUT = 5;

    public static final int STDERR = 6;

    public static final int ALLWAYS = 7;

    public static final int CONSOLE = 8;

    private static final String LEVELNAMES[] = { "DBUG", "INFO", "WARN", "EROR", "SEVR", "SOUT", "SERR", "ALLW", "CONS" };

    private static final String SETABLE_LEVELNAMES[] = { "Debug", "Informational", "Warning", "Error", "Severe" };

    private Logger() {
        int capacity = 1000;
        lockLogFile = false;
        onlineLogCapacity = 100;
        queue = new LinkedBlockingQueue<String>(capacity);
        oldErrPrintStream = System.err;
        oldOutPrintStream = System.out;
        outPrintStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream(), this, STDOUT));
        errPrintStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream(), this, STDERR));
        logList = Collections.synchronizedList(new ArrayList<String>(onlineLogCapacity));
        try {
            out = getLogFile();
        } catch (LoggerException e1) {
            oldErrPrintStream.println("Unable to Initialize Log File, Error is " + e1.getMessage() + " Cause: " + e1.getCauseMessage());
            out = null;
        }
        allways("Initializing the logger, Max Queued Depth is " + capacity);
        echoToOut = false;
        loggingLevel = DEBUG;
        if (loggingLevel > SEVERE || loggingLevel < DEBUG) {
            error("Invalid level Specified, Defaulting Logging Level set to INFO");
            loggingLevel = INFO;
        }
        allways("Logging Level is " + getLevelName(loggingLevel));
        loggerThread = new LoggerThread(out, queue);
        loggerThread.setDaemon(true);
        loggerThread.start();
        Runtime.getRuntime().addShutdownHook(new LoggerShutdownHook(loggerThread));
    }

    private DataOutputStream getLogFile() throws LoggerException {
        FileOutputStream fos = null;
        String uniqueId = "";
        int tries = 0;
        String logDirectory = null;
        String filePrefix = null;
        String fileSuffix = null;
        filePrefix = "JProbe";
        fileSuffix = "log";
        while (true) {
            String logFileName = filePrefix + "." + fileSuffix;
            try {
                fos = new FileOutputStream(logFileName, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new LoggerException("Error Creating File Stream", e.getMessage());
            }
            if (!lockLogFile) break;
            FileChannel channel = fos.getChannel();
            try {
                FileLock fl = channel.tryLock();
                if (fl != null) break;
            } catch (IOException e) {
                e.printStackTrace();
                throw new LoggerException("Error Locking Log File", e.getMessage());
            }
            uniqueId = "(" + ++tries + ")";
            if (tries > 50) throw new LoggerException("Max Retries Exceeded", "");
        }
        return new DataOutputStream(fos);
    }

    public static synchronized Logger getLogger() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public static void setLockLogFile(boolean val) {
        lockLogFile = val;
    }

    public void simpleLog(String str) {
        try {
            queue.put(str + NEWLINE);
        } catch (InterruptedException e1) {
        }
    }

    public void log(int level, String str) {
        if (level < loggingLevel) return;
        String outStr = formatMsg(level, str);
        logList.add(outStr);
        if (logList.size() > onlineLogCapacity) logList.remove(0);
        if (loggerThread != null) {
            if (loggerThread.isLoggerThreadReady()) {
                if (!loggerThread.isAlive()) {
                    oldErrPrintStream.println("Logger Thread Is Not Active - Message(" + outStr + ")");
                    return;
                }
            }
        }
        if (queue.remainingCapacity() == 0) {
            oldErrPrintStream.println("Log Que is Full - Message(" + outStr + ")");
            return;
        }
        if (out != null) {
            try {
                queue.put(outStr + NEWLINE);
            } catch (InterruptedException e1) {
            }
        } else {
            oldErrPrintStream.println(outStr);
        }
        if (echoToOut) oldOutPrintStream.println(outStr);
    }

    public void debug(String str) {
        log(DEBUG, str);
    }

    public void warning(String str) {
        log(WARNING, str);
    }

    public void info(String str) {
        log(INFO, str);
    }

    public void error(String str) {
        log(ERROR, str);
    }

    public void severe(String str) {
        log(SEVERE, str);
    }

    public void allways(String str) {
        log(ALLWAYS, str);
    }

    public void console(String str) {
        oldOutPrintStream.println(formatMsg(CONSOLE, str));
        log(CONSOLE, str);
    }

    public void logCaller() {
        Exception e = new Exception();
        StackTraceElement[] stack = e.getStackTrace();
        log(DEBUG, stack[1].getFileName() + ":" + stack[1].getLineNumber() + "() - Called by " + stack[2].toString());
    }

    public static String getLevelName(int lvl) {
        if (lvl < 0 || lvl > LEVELNAMES.length - 1) return "Invalid(" + lvl + ")";
        return LEVELNAMES[lvl];
    }

    public static String getSetableLevelName(int lvl) {
        if (lvl < 0 || lvl > SETABLE_LEVELNAMES.length - 1) return "Invalid(" + lvl + ")";
        return SETABLE_LEVELNAMES[lvl];
    }

    public boolean setLevelByName(String name) {
        if (name == null) {
            error("Specified Name is null, request Ignored");
            return false;
        }
        for (int i = 0; i < SETABLE_LEVELNAMES.length; i++) {
            if (name.equalsIgnoreCase(SETABLE_LEVELNAMES[i])) {
                return setLogLevel(i);
            }
        }
        error("Invalid Level Name Specified(" + name + ") Request Ignored");
        return false;
    }

    public static int getNumberOfLevels() {
        return SETABLE_LEVELNAMES.length;
    }

    public static String[] getLevelNames() {
        return LEVELNAMES;
    }

    public int getLogLevel() {
        return loggingLevel;
    }

    public boolean setLogLevel(int newLevel) {
        if (newLevel > SEVERE || newLevel < DEBUG) {
            error("Invalid level(" + newLevel + ") Specified, Logging Level Not Changed");
            return false;
        }
        allways("Changing Log Level From " + getLevelName(loggingLevel) + " TO " + getLevelName(newLevel));
        loggingLevel = newLevel;
        return true;
    }

    protected static String formatMsg(int level, String inStr) {
        String outStr = inStr;
        String thdId = "[" + Thread.currentThread().getName() + ":" + Thread.currentThread().getId() + "]";
        outStr = getDateTime("HH:mm:ss.SSS") + " [" + getLevelName(level) + "]" + thdId + outStr;
        return outStr;
    }

    public static void setLoggerOption(String key, String value) {
        System.setProperty(key, value);
    }

    /**
	 * This method will return the current date and time
	 * @return String, formatted date and time 
	 */
    protected static synchronized String getDateTime(String pattern) {
        GregorianCalendar target = new GregorianCalendar();
        SimpleDateFormat output = new SimpleDateFormat();
        Date date = target.getTime();
        output.applyPattern(pattern);
        return output.format(date);
    }

    public void logException(Throwable t, Object by) {
        String excString = "";
        excString = "Diagnostic Data for Exception " + t.getClass().getName() + "\n";
        if (by != null) excString += "\tDetected by: " + by.getClass().getName() + "\n";
        excString += "\tThread: " + Thread.currentThread().getName() + " - [" + Thread.currentThread().getId() + "]\n";
        excString += "\tDescription: " + t.getLocalizedMessage() + "\n";
        if (t.getCause() != null) {
            excString += "\tCause: " + t.getCause().getMessage() + "\n";
        }
        excString += "\tStackTrace\n";
        int offset = 0;
        StackTraceElement[] ste = t.getStackTrace();
        for (int i = 0; i < ste.length; i++) {
            String steStr = (offset == 0 ? " " : "-");
            excString += "\t\t(" + steStr + offset++ + ") - " + ste[i].toString() + "\n";
        }
        error(excString);
    }

    public void logStackTrace(String msg, Object by) {
        String excString = "";
        excString = "Dump Of Stack Trace  \n";
        if (by != null) excString += "\tRequested by: " + by.getClass().getName() + "\n";
        excString += "\tThread: " + Thread.currentThread().getName() + " - [" + Thread.currentThread().getId() + "]\n";
        excString += "\tDescription: " + msg + "\n";
        excString += "\tStackTrace\n";
        int offset = 0;
        Exception e = new Exception();
        StackTraceElement[] ste = e.getStackTrace();
        for (int i = 0; i < ste.length; i++) {
            String steStr = (offset == 0 ? " " : "-");
            excString += "\t\t(" + steStr + offset++ + ") - " + ste[i].toString() + "\n";
        }
        debug(excString);
    }

    public String[] getLogData() {
        String out[] = new String[logList.size()];
        for (int i = 0; i < logList.size(); i++) {
            out[i] = logList.get(i);
        }
        return out;
    }

    public void clearLog() {
        logList.clear();
    }

    public void logStackTrace(boolean thisThreadOnly) {
        String excString = "";
        excString = "Thread Stack Dump\n";
        excString += "\tRequesting Thread: " + Thread.currentThread().getName() + " - [" + Thread.currentThread().getId() + "]\n";
        Thread myThread = Thread.currentThread();
        long mytid = myThread.getId();
        Map stackMap = Thread.getAllStackTraces();
        Set keys = new HashSet();
        keys = stackMap.keySet();
        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            Thread thd = (Thread) iter.next();
            long tid = thd.getId();
            if (mytid != tid && thisThreadOnly) {
                continue;
            }
            excString += "\n\tStack Trace for [" + thd.getName() + ":ID=" + thd.getId() + "]\n";
            StackTraceElement ste[] = (StackTraceElement[]) stackMap.get(thd);
            int offset = 0;
            for (int i = 0; i < ste.length; i++) {
                String steStr = "\t\t(";
                steStr += (offset == 0 ? " " : "-");
                steStr += offset++;
                steStr += ") - ";
                steStr += ste[i].toString();
                excString += steStr + "\n";
            }
        }
        error(excString);
    }
}

class FilteredStream extends FilterOutputStream {

    private Logger logger;

    private int from;

    private String newLine = System.getProperty("line.separator");

    public FilteredStream(OutputStream stream, Logger logger, int from) {
        super(stream);
        this.logger = logger;
        this.from = from;
    }

    public void write(byte b[]) throws IOException {
        String out = new String(b);
        if (!out.equals(newLine)) {
            logger.log(from, out);
        }
    }

    public void write(byte b[], int off, int len) throws IOException {
        String out = new String(b, off, len);
        if (!out.equals(newLine)) {
            logger.log(from, out);
        }
    }
}

class LoggerThread extends Thread {

    private DataOutputStream out;

    private BlockingQueue<String> queue;

    private int msgCount;

    private int maxDepth = 0;

    public static final long NANOS_PER_MILLI = 1000000;

    private volatile boolean loggerThreadReady = false;

    private String newLine = System.getProperty("line.separator");

    private static final String RUN_DELIM = "#######################################" + "#######################################" + "#######################################" + "#######################################";

    public LoggerThread(DataOutputStream out, BlockingQueue<String> queue) {
        this.queue = queue;
        this.out = out;
    }

    public void run() {
        if (out == null) return;
        Thread.currentThread().setName("LoggerThread");
        write(Logger.formatMsg(Logger.INFO, "Starting Logger Thread..." + newLine));
        loggerThreadReady = true;
        try {
            while (true) {
                write(queue.take());
                msgCount++;
                int depth = queue.size();
                if (depth > maxDepth) maxDepth = depth;
            }
        } catch (InterruptedException e) {
            try {
                ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
                long cpu = threadBean.getThreadCpuTime(this.getId());
                long cpums = cpu / NANOS_PER_MILLI;
                if (cpums < 1) cpums = 1;
                write(Logger.formatMsg(Logger.INFO, "Logger Thread Ending, " + msgCount + " Messages Processed, Max Queued Messages was " + maxDepth + " CPU Used: " + cpums + "ms" + newLine));
                write(RUN_DELIM + newLine);
                out.close();
            } catch (IOException e1) {
            }
        }
    }

    protected void write(String data) {
        try {
            out.write(data.getBytes());
        } catch (IOException e) {
        }
    }

    /**
	 * @return the loggerThreadReady
	 */
    public boolean isLoggerThreadReady() {
        return loggerThreadReady;
    }
}

class LoggerShutdownHook extends Thread {

    private LoggerThread logger;

    public LoggerShutdownHook(LoggerThread logger) {
        this.logger = logger;
        setName("LoggerShutdownHook");
    }

    public void run() {
        logger.write(Logger.formatMsg(Logger.INFO, "ShutDown Hook Interrupting The Logger Thread" + Logger.NEWLINE));
        logger.interrupt();
    }
}

class LoggerException extends Exception {

    private String msg;

    private String origMsg;

    public LoggerException(String msg, String origMsg) {
        super(msg);
        this.msg = msg;
        this.origMsg = msg;
    }

    public String getMessage() {
        return msg;
    }

    public String getCauseMessage() {
        return origMsg;
    }
}
