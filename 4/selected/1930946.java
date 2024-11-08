package expectj;

import java.util.Date;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;

/**
 * This class runs the process and streams the input and output of the
 * process so as to allow execution of the process in a different thread. 
 *
 * @author	Sachin Shekar Shetty  
 * @version 1.0
 */
public class ProcessRunner implements TimerEventListener {

    private String commandLine = null;

    private long timeOut = 0;

    private ProcessThread process = null;

    private Timer tm = null;

    private PipedInputStream readSystemOut = null;

    private PipedInputStream readSystemErr = null;

    private PipedOutputStream writeSystemOut = null;

    private PipedOutputStream writeSystemErr = null;

    private StreamPiper processOutToSystemOut = null;

    private StreamPiper processErrToSystemErr = null;

    private Debugger debug = new Debugger("ProcessRunner", true);

    /**
     * Private constructor
     *
     * @param commandLine process command to be executed
     * @param timeOut time interval in seconds to be allowed for process
     * execution; not used as of now.
     */
    private void initialize(String commandLine, long timeOut) throws ExpectJException {
        if (timeOut < -1) {
            throw new IllegalArgumentException("Time-out is invalid");
        }
        if (commandLine == null || commandLine.trim().equals("")) {
            throw new IllegalArgumentException("Command: " + commandLine + " is null/empty");
        }
        this.timeOut = timeOut;
        this.commandLine = commandLine;
        process = new ProcessThread(commandLine);
        if (timeOut != -1) tm = new Timer(timeOut, this);
    }

    /**
     * Constructor
     *
     * @param commandLine process command to be executed 
     * @param timeOut time interval in seconds to be allowed for process
     * execution; not used as of now.
     */
    ProcessRunner(String commandLine, long timeOut) throws ExpectJException {
        initialize(commandLine, timeOut);
    }

    /**
     * This constructor allows to run a process with indefinate time-out
     * @param commandLine process command to be executed 
     */
    ProcessRunner(String commandLine) throws ExpectJException {
        initialize(commandLine, -1);
    }

    /**
     * Time callback method
     * This method is invoked when the time-out occurr
     */
    public void timerTimedOut() {
        process.stop();
    }

    /**
     * This method stops the spawned process.
     */
    public void stop() {
        process.stop();
    }

    /**
     * Timer callback method
     * This method is invoked by the Timer, when the timer thread
     * receives an interrupted exception
     */
    public void timerInterrupted(InterruptedException ioe) {
    }

    /**
     * This method is used to Stop all the piper object from copying the
     * content to standard out. This is used after interact command.
     */
    synchronized void stopPipingToStandardOut() {
        processOutToSystemOut.stopPipingToStandardOut();
        processErrToSystemErr.stopPipingToStandardOut();
    }

    synchronized void startPipingToStandardOut() {
        processOutToSystemOut.startPipingToStandardOut();
        processErrToSystemErr.startPipingToStandardOut();
    }

    /**
     * This method executes the given command within the specified time
     * limit. It starts the process thread and also the timer when
     * enabled. It starts the piped streams to enable copying of process
     * stream contents to standard streams.
     */
    void start() throws ExpectJException {
        try {
            process.start();
            if (tm != null) tm.startTimer();
            readSystemOut = new PipedInputStream();
            writeSystemOut = new PipedOutputStream(readSystemOut);
            processOutToSystemOut = new StreamPiper(System.out, process.process.getInputStream(), writeSystemOut);
            processOutToSystemOut.start();
            readSystemErr = new PipedInputStream();
            writeSystemErr = new PipedOutputStream(readSystemErr);
            processErrToSystemErr = new StreamPiper(System.err, process.process.getErrorStream(), writeSystemErr);
            processErrToSystemErr.start();
        } catch (Exception exp) {
            throw new ExpectJException("Error in ProcessRunner.start()", exp);
        }
    }

    /**
     * This method returns the input stream of the process.
     */
    InputStream getInputStream() {
        return readSystemOut;
    }

    /**
     * This method returns the output stream of the process.
     */
    OutputStream getOutputStream() {
        return process.process.getOutputStream();
    }

    /**
     * This method returns the error stream of the process.
     */
    InputStream getErrorStream() {
        return readSystemErr;
    }

    /**
     * This method returns true if the process has already exited.
     */
    boolean isClosed() {
        return process.isClosed;
    }

    /**
     * If the process representes by this object has already exited, it
     * returns the exit code. isClosed() should be used in conjunction
     * with this method.
     */
    int getExitValue() {
        return process.exitValue;
    }

    /**
     * This class is responsible for executing the process in a seperate
     * thread.
     */
    class ProcessThread implements Runnable {

        String commandLine = null;

        Process process = null;

        Thread processThread = null;

        volatile boolean isClosed = false;

        int exitValue;

        /**
         * Constructor
         */
        ProcessThread(String commandLine) {
            this.commandLine = commandLine;
        }

        /**
         * This method spawns the thread and runs the process within the
         * thread
         */
        public void start() throws ExpectJException {
            System.out.println("Process Started at:" + new Date());
            processThread = new Thread(this);
            try {
                process = Runtime.getRuntime().exec(commandLine);
            } catch (Exception exp) {
                throw new ExpectJException("Error in ProcessThread.start", exp);
            }
            processThread.start();
        }

        /**
         * This method executes the process, Thread Run Method
         *
         */
        public void run() {
            try {
                process.waitFor();
                exitValue = process.exitValue();
                isClosed = true;
            } catch (InterruptedException ie) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * This method interrupts and stops the thread.
         */
        public void stop() {
            debug.print("Process '" + commandLine + "' Killed at:" + new Date());
            processThread.interrupt();
            process.destroy();
        }
    }
}
