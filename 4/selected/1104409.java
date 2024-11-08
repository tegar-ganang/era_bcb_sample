package tool.dtf4j.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to trace a provided input stream, in which text is still being written,
 * for the presence of a given text pattern.
 */
class OutputTracer extends Thread {

    /** The logger */
    private static Logger logger_ = Logger.getLogger("DTFLogger");

    /** The text to look for in case of a VM crash */
    private static String VM_CRASH_TEXT = "# HotSpot Virtual Machine Error";

    /** The stream. */
    private BufferedReader stream_;

    /** The trace. */
    private PrintWriter trace_;

    /** Indicates if output should be displayed on the console. */
    private boolean showOutput_;

    /** Indicates if thread is running. */
    private boolean thread_running_;

    /** Indicates, whether interrupt was ever called or not. */
    private boolean interrupt_called_ = false;

    /** The file which to trace. */
    private File traceFile_;

    /** The FileOutputStream of the trace file - used to do sync(). */
    private FileOutputStream fileOut_;

    /** The pattern to search for in the trace file. */
    private String pattern_;

    /** The exec process. */
    private ProcessExecutor execProcess_;

    /** The name (stderr, stdout). */
    private String name_;

    /** Indicates if file logging should be enabled/disabled. */
    private boolean fileLogging_;

    /** Synchronization object used between run() and interrupt(). */
    private final Object syncObj_ = new Object();

    /** Flag to inform interrupt() that run() has returned. */
    private boolean done_ = false;

    /**
     * Constructs a stream reader, and gets ready to read it.
     * @param s the stream we want to read
     */
    OutputTracer(String name, ProcessExecutor execProcess, InputStream s, boolean mode, boolean fileLogging, File traceFile, String pattern) throws IOException {
        fileLogging_ = fileLogging;
        name_ = name;
        execProcess_ = execProcess;
        pattern_ = pattern;
        traceFile_ = traceFile;
        stream_ = new BufferedReader(new InputStreamReader(s));
        fileOut_ = new FileOutputStream(traceFile);
        trace_ = new PrintWriter(fileOut_);
        showOutput_ = mode;
        thread_running_ = true;
        if (!fileLogging_) {
            trace_.println("Logging to file disabled on client.");
            trace_.flush();
        }
    }

    /**
     * Starts reading the actual thread.
     */
    public void run() {
        try {
            String nextLine = null;
            while (true) {
                if (stream_ == null) return;
                if (stream_.ready()) {
                    nextLine = stream_.readLine();
                    if (nextLine.startsWith(VM_CRASH_TEXT)) execProcess_.vmcrash();
                    if (nextLine != null) {
                        if (fileLogging_) {
                            trace_.println(nextLine);
                            trace_.flush();
                        }
                        if (!(pattern_.equals("")) && (nextLine.indexOf(pattern_) >= 0)) {
                            fileOut_.getFD().sync();
                            logger_.log(Level.FINE, "Pattern found: " + pattern_);
                            execProcess_.patternMatchFound(traceFile_);
                        }
                        if (showOutput_) logger_.log(Level.FINE, name_ + ": " + nextLine);
                    } else {
                        return;
                    }
                } else {
                    synchronized (syncObj_) {
                        if (!thread_running_) return;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        } catch (IOException ex) {
        } finally {
            synchronized (syncObj_) {
                done_ = true;
                syncObj_.notifyAll();
            }
        }
    }

    /**
     * Stops the reader thread
     */
    public void interrupt() {
        synchronized (syncObj_) {
            if (interrupt_called_) {
                logger_.log(Level.FINE, "OutputTracer already interrupted: " + this);
                return;
            }
            interrupt_called_ = true;
        }
        try {
            stream_.close();
        } catch (IOException ex) {
        }
        synchronized (syncObj_) {
            thread_running_ = false;
            while (!done_) {
                try {
                    syncObj_.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        trace_.flush();
        trace_.close();
    }
}
