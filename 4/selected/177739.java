package net.sf.openforge.util.exec;

import java.io.*;

/**
 * <code>Drain</code> is a supervisor class that empties a stream that
 * is being fed by another process.  To perform this task,
 * Drain creates a separate thread that continually calls
 * {@link InputStream#readLine()} on a given input stream and forwards the
 * read data to the a given {@link PrintWriter} (if no writer is specified,
 * the data is discarded).
 * This class works well to handle IO generated by an
 * exec'd process, since the process will hang if IO is
 * generated and the stream it uses fills up.
 * <P>
 * A new {@link Thread} that performs the transfer begins
 * running as soon as the Drain's constructor is called.  The state
 * of this thread may be tested with {@link Drain#isAlive()}.  To wait
 * in the current thread for the Drain's thread to complete, use
 * {@link Drain#waitFor()}.
 *
 * @author <a href="mailto:Jonathan.Harris@xilinx.com">Jonathan C. Harris</a>
 * @version $Id
 */
public class Drain {

    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    /** The anonymous thread in which the i/o occurs */
    private Thread thread;

    /**
     * Creates a new <code>Drain</code> instance.
     *
     * @param stream an <code>InputStream</code> to read data from.
     * @param pw a <code>PrintWriter</code> to forward the data to.
     * Supply <code>null</code> to discard all data read.
     */
    public Drain(InputStream stream, PrintWriter pw) {
        if (pw == null) {
            drain(stream, null);
        } else {
            drain(stream, pw);
        }
    }

    /**
     * Creates a new <code>Drain</code> instance.
     *
     * @param stream an <code>InputStream</code> to read data from.
     * @param ps a <code>PrintStream</code> to forward the data to.
     * Supply <code>null</code> to discard all data read.
     */
    public Drain(InputStream stream, PrintStream ps) {
        if (ps == null) {
            drain(stream, null);
        } else {
            drain(stream, new PrintWriter(ps));
        }
    }

    /**
     * Creates a new <code>Drain</code> instance that will discard all
     * data read.
     *
     * @param stream an <code>InputStream</code> to read data from.
     */
    public Drain(InputStream stream) {
        drain(stream, null);
    }

    /**
     * @return <code>true</code> if this <code>Drain's</code> thread is
     * still running, <code>false</code> if it has completed.  The
     * <code>Drain</code> thread runs until an
     * <code>IOException</code> occurs while reading data from the
     * <code>InputStream</code>.  An <code>IOException</code> will
     * occur once the stream is closed by the source process.
     */
    public boolean isAlive() {
        return thread.isAlive();
    }

    /**
     * Calls {@link Thread#join()} on this Drain's thread.
     *
     * @exception InterruptedException if another thread interrupts the caller's
     *              thread while it's waiting
     */
    public void waitFor() throws InterruptedException {
        thread.join();
    }

    /**
     * Drains the supplied stream and forwards it to a given writer.  A new
     * thread is started to drive the transfer.
     *
     * @param stream the input stream from which data is read
     * @param pw the receiver of the data from the stream; if null, the
     *          data is discarded
     */
    private void drain(final InputStream stream, final PrintWriter pw) {
        this.thread = new Thread() {

            public void run() {
                InputStreamReader reader = new InputStreamReader(stream);
                BufferedReader buffer = new BufferedReader(reader);
                try {
                    while (true) {
                        String nextLine = buffer.readLine();
                        if (nextLine == null) {
                            break;
                        } else {
                            if (pw != null) {
                                pw.println(nextLine);
                            }
                        }
                    }
                } catch (IOException eIO) {
                }
                if (pw != null) {
                    pw.flush();
                }
            }
        };
        this.thread.start();
    }
}
