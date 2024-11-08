package com.pbonhomme.xf.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class CommandRunner {

    protected static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    private static final int BUFFER_SIZE = 1024;

    protected static final long DELAY = 50L;

    protected String name;

    protected int id = 0;

    protected static int counter = 0;

    private ProcessFactory processFactory;

    private Process childProcess;

    protected String[] args;

    protected String[] commandArgs;

    public CommandRunner(String name) {
        this.name = name;
        this.processFactory = ProcessFactory.getFactory();
        this.childProcess = null;
        counter++;
        this.id = counter;
        this.commandArgs = null;
    }

    public void run(String command) throws RuntimeException {
        try {
            this.commandArgs = new String[] { command };
            if (logger.isDebugEnabled()) logCommandLine(commandArgs);
            this.childProcess = processFactory.exec(command);
        } catch (IOException e) {
            throw new RuntimeException("An I/O error occured: " + e.getMessage());
        } catch (SecurityException e) {
            throw new RuntimeException("A security manager exists and its checkExec method " + "doesn't allow creation of a subprocess.");
        } catch (NullPointerException e) {
            throw new RuntimeException("Command is null.");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Command is empty.");
        }
    }

    public void run(String[] commandArgs) throws RuntimeException {
        try {
            this.commandArgs = commandArgs;
            if (logger.isDebugEnabled()) logCommandLine(commandArgs);
            this.childProcess = processFactory.exec(commandArgs);
        } catch (IOException e) {
            throw new RuntimeException("An I/O error occured: " + e.getMessage());
        } catch (SecurityException e) {
            throw new RuntimeException("A security manager exists and its checkExec method " + "doesn't allow creation of a subprocess.");
        } catch (NullPointerException e) {
            throw new RuntimeException("Command is null.");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Command is empty.");
        }
    }

    /**
     * Execute a command
     *
     * @param command path of comand
     * @param args arguments after the command
     * @throws RuntimeException if an error occurs
     */
    public void run(String command, String[] args) throws RuntimeException {
        this.commandArgs = buildCommandArray(command, args);
        run(commandArgs);
    }

    public void run(String[] commandArgs, String[] env) throws RuntimeException {
        try {
            this.commandArgs = commandArgs;
            if (logger.isDebugEnabled()) logCommandLine(commandArgs);
            this.childProcess = processFactory.exec(commandArgs, env);
        } catch (IOException e) {
            throw new RuntimeException("An I/O error occured: " + e.getMessage());
        } catch (SecurityException e) {
            throw new RuntimeException("A security manager exists and its checkExec method " + "doesn't allow creation of a subprocess.");
        } catch (NullPointerException e) {
            throw new RuntimeException("Command is null.");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Command is empty.");
        }
    }

    /**
     * Execute a command
     *
     * @param command path of comand
     * @param args arguments after the comand
     * @param env environment name value pairs
     * 
     * @throws RuntimeException if an error occurs
     */
    public void run(String command, String[] args, String[] env) throws RuntimeException {
        commandArgs = buildCommandArray(command, args);
        run(commandArgs, env);
    }

    public void run(String[] commandArgs, String[] env, String workingDirectory) throws RuntimeException {
        try {
            this.commandArgs = commandArgs;
            if (logger.isDebugEnabled()) logCommandLine(commandArgs);
            this.childProcess = processFactory.exec(commandArgs, env, workingDirectory);
        } catch (IOException e) {
            throw new RuntimeException("An I/O error occured: " + e.getMessage());
        } catch (SecurityException e) {
            throw new RuntimeException("A security manager exists and its checkExec method " + "doesn't allow creation of a subprocess.");
        } catch (NullPointerException e) {
            throw new RuntimeException("Command is null.");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Command is empty.");
        }
    }

    /**
     * Execute a command.
     *
     * @param command path of comand
     * @param args arguments after the comand
     * @param env environment name value pairs
     * @param workingDirectory The working directory
     * 
     * @throws RuntimeException if an error occurs
     */
    public void run(String command, String[] args, String[] env, String workingDirectory) throws RuntimeException {
        commandArgs = buildCommandArray(command, args);
        run(commandArgs, env, workingDirectory);
    }

    /**
     * Reads output and error from the external process to the streams.
     *
     * @param output process stdout is written to this stream
     * @param err process stderr is written to this stream
     * 
     * @throws RuntimeException if process not yet executed
     */
    public void waitAndRead(OutputStream out, OutputStream err) throws RuntimeException {
        if (childProcess == null) {
            throw new RuntimeException("Process not yet executed");
        }
        ProcessReader reader = new ProcessReader(name, childProcess, null, out, err);
        reader.readBlocking();
    }

    /**
     * Reads error output from the external process to the streams.
     *
     * @param err process stderr is written to this stream
     * 
     * @throws RuntimeException if process not yet executed
     */
    public void waitAndRead(OutputStream err) throws RuntimeException {
        if (childProcess == null) {
            throw new RuntimeException("Process not yet executed");
        }
        ProcessReader reader = new ProcessReader(name, childProcess, null, null, err);
        reader.readBlocking();
    }

    /**
     * Reads output from the external process to the streams. A progress monitor
     * is polled to test for cancellation. Destroys the process if the monitor
     * becomes cancelled
     *
     * @param output process stdout is written to this stream
     * @param err process stderr is written to this stream
     * @param monitor monitor to receive progress info and to cancel
     *    the  external process
     * @throws RuntimeException if process not yet executed or if process
     * cancelled
     */
    public void waitAndRead(OutputStream output, OutputStream err, CommandRunnerMonitoring monitor) throws RuntimeException {
        if (childProcess == null) {
            throw new RuntimeException("Process not yet executed");
        }
        PipedOutputStream errOutPipe = new PipedOutputStream();
        PipedOutputStream outputPipe = new PipedOutputStream();
        PipedInputStream errInPipe, inputPipe;
        try {
            errInPipe = new PipedInputStream(errOutPipe);
            inputPipe = new PipedInputStream(outputPipe);
        } catch (IOException e) {
            throw new RuntimeException("Command canceled");
        }
        ProcessReader _closure = new ProcessReader(name, childProcess, null, outputPipe, errOutPipe);
        _closure.readNonBlocking();
        processStreams(_closure, output, inputPipe, err, errInPipe, monitor);
    }

    /**
     * Writes input to and reads output from the external process to the streams.
     *
     * @param in process stdin is read from this stream
     * @param output process stdout is written to this stream
     * @param err process stderr is written to this stream
     * @throws ProcessException if process not yet executed
     */
    public void waitAndWrite(InputStream in, OutputStream out, OutputStream err) throws RuntimeException {
        if (childProcess == null) {
            throw new RuntimeException("Process not yet executed");
        }
        ProcessReader _reader = new ProcessReader(name, childProcess, in, out, err);
        _reader.writeBlocking();
    }

    /**
     * Writes input to and reads output from the external process to the streams.
     * A progress monitor is polled to test for cancellation. Destroys the
     * process if the monitor becomes cancelled
     *
     * @param in process stdin is read from this stream
     * @param output process stdout is written to this stream
     * @param err process stderr is written to this stream
     * @param monitor monitor to receive progress info and to cancel the  external process
     * @throws RuntimeException if process not yet executed or if process cancelled
     */
    public void waitAndWrite(InputStream in, OutputStream output, OutputStream err, CommandRunnerMonitoring monitor) throws RuntimeException {
        if (childProcess == null) {
            throw new RuntimeException("Process not yet executed");
        }
        PipedOutputStream errOutPipe = new PipedOutputStream();
        PipedOutputStream outputPipe = new PipedOutputStream();
        PipedInputStream errInPipe, inputPipe;
        try {
            errInPipe = new PipedInputStream(errOutPipe);
            inputPipe = new PipedInputStream(outputPipe);
        } catch (IOException e) {
            throw new RuntimeException("Command canceled");
        }
        ProcessReader closure = new ProcessReader(name, childProcess, in, outputPipe, errOutPipe);
        closure.readNonBlocking();
        closure.writeNonBlocking();
        processStreams(closure, output, inputPipe, err, errInPipe, monitor);
    }

    /**
     * process the Streams.while the external process returns bytes. Cancellation
     * is possible by the ProgressMonitor
     *
     * @param closure process closure object which handles the interaction with
     *    the  external process
     * @param output process stdout is written to this stream
     * @param inputPipe piped stream to other thread for the stdout
     * @param err process stderr is written to this stream
     * @param errInPipe piped stream to other thread for the stderr
     * @param monitor monitor to receive progress info and to cancel
     *    the   external process
     * @throws RuntimeException if process cancelled
     */
    protected void processStreams(ProcessReader closure, OutputStream output, PipedInputStream inputPipe, OutputStream err, PipedInputStream errInPipe, CommandRunnerMonitoring monitor) throws RuntimeException {
        monitor.begin();
        byte buffer[] = new byte[BUFFER_SIZE];
        int nbytes;
        while (!monitor.isCanceled() && closure.isAlive()) {
            nbytes = 0;
            try {
                if (errInPipe.available() > 0) {
                    nbytes = errInPipe.read(buffer);
                    err.write(buffer, 0, nbytes);
                    err.flush();
                }
                if (inputPipe.available() > 0) {
                    nbytes = inputPipe.read(buffer);
                    output.write(buffer, 0, nbytes);
                    output.flush();
                }
            } catch (IOException e) {
            }
            if (nbytes == 0) {
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException ie) {
                }
            } else {
                monitor.worked();
            }
        }
        if (monitor.isCanceled()) {
            closure.terminate();
            throw new RuntimeException("Command canceled");
        }
        try {
            childProcess.waitFor();
        } catch (InterruptedException e) {
        }
        try {
            while (errInPipe.available() > 0 || inputPipe.available() > 0) {
                nbytes = 0;
                if (errInPipe.available() > 0) {
                    nbytes = errInPipe.read(buffer);
                    err.write(buffer, 0, nbytes);
                    err.flush();
                }
                if (inputPipe.available() > 0) {
                    nbytes = inputPipe.read(buffer);
                    output.write(buffer, 0, nbytes);
                    output.flush();
                }
                if (nbytes != 0) {
                    monitor.worked();
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                errInPipe.close();
            } catch (IOException e) {
            }
            try {
                inputPipe.close();
            } catch (IOException e) {
            }
        }
        monitor.done();
    }

    public void finalize() {
        counter--;
    }

    /**
     * Builds a command array that will be passed to the process
     *
     * @param commandPath path of comand
     * @param commandArgs arguments after the command
     */
    private final String[] buildCommandArray(String command, String[] commandArgs) {
        String[] _args = new String[1 + commandArgs.length];
        _args[0] = command;
        System.arraycopy(commandArgs, 0, _args, 1, commandArgs.length);
        return _args;
    }

    /**
     * Log the command line.
     *
     * @param commandArgs array of comand and args
     */
    private final void logCommandLine(String[] commandArgs) {
        StringBuffer _buf = new StringBuffer();
        for (int i = 0; i < commandArgs.length; i++) {
            _buf.append(commandArgs[i]);
            _buf.append(' ');
        }
        _buf.append(LINE_SEPARATOR);
        logger.debug(_buf.toString());
        _buf = null;
    }

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CommandRunner.class);
}
