package org.cyberaide.gridshell.commands.runtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

public class RuntimeAdapter {

    public static final int STDERR = 0;

    public static final int STDOUT = 1;

    private boolean verbose;

    private boolean error = false;

    private String command = null;

    private Writer outwriter = null;

    private Thread inreader = null;

    private Thread errreader = null;

    private RuntimeListener listener = null;

    /**
	 * Constructor.
	 * 
	 * @param command - command to execute
	 * @param listener - the RuntimeListener
	 */
    public RuntimeAdapter(String command, RuntimeListener listener) {
        this.command = command;
        this.listener = listener;
    }

    public String getCommand() {
        return command;
    }

    public void setError(boolean err) {
        System.out.println("setError: " + err);
        error = err;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean hasError() {
        return error;
    }

    public RuntimeListener getRuntimeListener() {
        return listener;
    }

    /**
	 * Execute a command using the Runtime class.
	 * 
	 * @param verbose - true if execution status messages should be printed out
	 */
    public void exec(boolean verbose) {
        this.verbose = verbose;
        if (isVerbose()) System.out.println("Executing: " + command);
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            execWindows();
        } else if (osName.startsWith("Mac OS")) {
            execUnix();
        } else {
            execUnix();
        }
    }

    /**
	 * Execute a command on a Unix machine.
	 */
    private void execUnix() {
        try {
            Process process = null;
            process = Runtime.getRuntime().exec(command);
            process.getOutputStream();
            if (listener == null || listener instanceof BasicRuntimeListener) {
                errreader = new PassiveReaderThread(process.getErrorStream(), this);
                inreader = new PassiveReaderThread(process.getInputStream(), this);
            } else {
                outwriter = new Writer(process.getOutputStream(), this);
                errreader = new ActiveReaderThread(process.getErrorStream(), STDERR, this, outwriter);
                inreader = new ActiveReaderThread(process.getInputStream(), STDOUT, this, outwriter);
            }
            inreader.start();
            errreader.start();
            errreader.join();
            inreader.join();
            if (listener instanceof BasicRuntimeListener) {
                ((BasicRuntimeListener) listener).setOutput(((PassiveReaderThread) inreader).getOutput());
                ((BasicRuntimeListener) listener).setErrors(((PassiveReaderThread) errreader).getOutput());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Execute a command on a Windows machine.
	 */
    private void execWindows() {
        System.out.println("TODO - Implement Windows Runtime functionality");
    }
}

class PassiveReaderThread extends Thread {

    private BufferedReader reader = null;

    private InputStream in = null;

    private RuntimeAdapter runner;

    private String output = "";

    public PassiveReaderThread(InputStream in, RuntimeAdapter runner) {
        this.in = in;
        this.runner = runner;
    }

    public String getOutput() {
        return output;
    }

    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            int c;
            String line = "";
            while ((c = reader.read()) != -1) {
                line += (char) c;
                if ((char) c == '\n') {
                    if (line.trim().length() > 0) {
                        if (runner.isVerbose()) System.out.print(line);
                        output += line;
                    }
                    line = "";
                }
            }
        } catch (Exception ex) {
            runner.setError(true);
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class ActiveReaderThread extends Thread {

    private int type;

    private BufferedReader reader = null;

    private InputStream in = null;

    private Writer outwriter;

    private RuntimeAdapter runner;

    private RuntimeListener listener;

    public ActiveReaderThread(InputStream in, int type, RuntimeAdapter runner, Writer outwriter) {
        this.in = in;
        this.type = type;
        this.outwriter = outwriter;
        this.runner = runner;
        this.listener = runner.getRuntimeListener();
    }

    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            List<String> tomatchList = listener.getStringsToMatch(type);
            Iterator<String> tomatchIter = tomatchList.iterator();
            String lastmatched = null;
            String tomatch = null;
            boolean matched = true;
            boolean contmatching = true;
            boolean waitfornewline = false;
            int c;
            String line = "";
            if (tomatchIter.hasNext()) {
                matched = false;
                tomatch = tomatchIter.next();
            }
            while ((c = reader.read()) != -1) {
                line += (char) c;
                if (contmatching) {
                    if (matched) {
                        matched = false;
                        if (tomatchIter.hasNext()) {
                            tomatch = tomatchIter.next();
                        } else {
                            contmatching = false;
                        }
                    }
                    if (contmatching) {
                        if (line.contains(tomatch)) {
                            outwriter.writeToCommand(listener.getNextInput(type, tomatch));
                            if (runner.isVerbose()) System.err.print(line);
                            lastmatched = line;
                            line = "";
                            matched = true;
                            waitfornewline = true;
                        } else if ((char) c == '\n') {
                            if (line.trim().length() > 0) {
                                if (runner.isVerbose()) System.err.print(line);
                                if (waitfornewline) {
                                    waitfornewline = false;
                                    listener.setMatchedLine(type, lastmatched, line.trim());
                                }
                            }
                            line = "";
                        }
                    }
                } else {
                    if ((char) c == '\n') {
                        if (line.trim().length() > 0) {
                            if (runner.isVerbose()) System.err.print(line);
                            if (waitfornewline) {
                                waitfornewline = false;
                                listener.setMatchedLine(type, lastmatched, line.trim());
                            }
                        }
                        line = "";
                    }
                }
            }
        } catch (Exception ex) {
            runner.setError(true);
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Writer {

    private BufferedWriter writer = null;

    private RuntimeAdapter runner;

    public Writer(OutputStream out, RuntimeAdapter runner) {
        this.runner = runner;
        writer = new BufferedWriter(new OutputStreamWriter(out));
    }

    public void writeToCommand(String line) {
        try {
            writer.write(line + "\n");
            writer.flush();
        } catch (Exception ex) {
            runner.setError(true);
            ex.printStackTrace();
        }
    }
}
