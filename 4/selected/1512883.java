package de.fau.cs.dosis.util.board;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

class StreamGobbler extends Thread {

    InputStream is;

    StringBuilder buffer;

    boolean forceStop;

    public boolean isFinished = false;

    StreamGobbler(InputStream is) {
        this.is = is;
        this.buffer = new StringBuilder();
        this.forceStop = false;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                this.buffer.append(line).append("\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        this.isFinished = true;
    }

    public void quit() {
        forceStop = true;
    }

    public String getBuffer() {
        return this.buffer.toString();
    }
}

/**
 * Wrapps SteamGobbler and Process and Timeout
 * 
 *  makes sure the Process gets his Stream Buffers emptied
 *  and the Process will timeout after the specified amount of time
 *
 */
public class ConvenientProcess {

    private String stdOut = "";

    private String stdErr = "";

    private int MAXRETRIES = 20;

    private ProcessBuilder processBuilder;

    private InputStream is;

    public ConvenientProcess(ProcessBuilder process) {
        this.processBuilder = process;
    }

    public ConvenientProcess(ProcessBuilder process, InputStream is) {
        this.processBuilder = process;
        this.is = is;
    }

    public int execute() throws IOException {
        Process process = processBuilder.start();
        StreamGobbler stdOutGobbler = new StreamGobbler(process.getInputStream());
        StreamGobbler stdErrGobbler = new StreamGobbler(process.getErrorStream());
        stdOutGobbler.start();
        stdErrGobbler.start();
        if (null != is) {
            byte[] buffer = new byte[16384];
            int read = 0;
            OutputStream out = null;
            try {
                out = process.getOutputStream();
                while (true) {
                    read = is.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    out.write(buffer, 0, read);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } finally {
                        if (out != null) {
                            out.close();
                        }
                    }
                }
            }
        }
        boolean wait = true;
        int exitValue = 0;
        int retries = 0;
        while (wait && (this.MAXRETRIES == -1 || retries < this.MAXRETRIES)) {
            try {
                exitValue = process.exitValue();
                wait = false;
            } catch (IllegalThreadStateException e) {
                wait = true;
                retries++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        process.destroy();
        while (!stdOutGobbler.isFinished) ;
        while (!stdErrGobbler.isFinished) ;
        this.stdErr = stdErrGobbler.getBuffer();
        this.stdOut = stdOutGobbler.getBuffer();
        return exitValue;
    }

    public void setTimeout(int deziSeconds) {
        this.MAXRETRIES = deziSeconds;
    }

    public String getStdOut() {
        return this.stdOut;
    }

    public String getStdErr() {
        return this.stdErr;
    }
}
