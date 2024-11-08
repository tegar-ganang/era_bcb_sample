package net.sf.jsequnit.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helps to executes an external process synchronously
 * through the use of {@code Threads} to forward
 * input and error streams.
 */
public class ProcessController {

    private boolean finished;

    private Process process;

    /**
	 * Create an instance for the given representation
	 * of the external process.
	 * 
	 * @param process
	 *            The java.lang instance of the native OS process.
	 */
    public ProcessController(Process process) {
        this.process = process;
    }

    /**
	 * Start the threads to forward the input and error stream
	 * of the external process to the streams of the current process.
	 */
    public void startStreamForwarding() {
        forwardStream("stderr", process.getErrorStream(), System.err);
        forwardStream("stdout", process.getInputStream(), System.out);
    }

    private void forwardStream(final String name, final InputStream in, final OutputStream out) {
        new Thread("Stream forwarder [" + name + "]") {

            public void run() {
                try {
                    while (!isFinished()) {
                        while (in.available() > 0) out.write(in.read());
                        synchronized (this) {
                            this.wait(100);
                        }
                    }
                    out.flush();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }.start();
    }

    private synchronized boolean isFinished() {
        return finished;
    }

    /**
	 * Stop stream forwarding and notify all pending threads.
	 */
    public synchronized void markFinished() {
        finished = true;
        notifyAll();
    }
}
