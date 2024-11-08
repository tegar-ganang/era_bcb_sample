package net.fortytwo.ripple.control;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import net.fortytwo.ripple.RippleException;

/**
 * An InputStream which optionally spawns a new thread to read bytes from a
 * source InputStream into a buffer as soon as they are available.  Otherwise,
 * it acts as a simple wrapper for the source InputStream, reading from the
 * source only when its own read() method is called.
 */
public class ThreadedInputStream extends InputStream {

    private final InputStream source;

    private final PipedInputStream writeIn;

    private final PipedOutputStream readOut;

    private boolean eager;

    private Task readerTask;

    public ThreadedInputStream(final InputStream is) throws RippleException {
        source = is;
        eager = false;
        readerTask = null;
        try {
            writeIn = new PipedInputStream();
            readOut = new PipedOutputStream(writeIn);
        } catch (java.io.IOException e) {
            throw new RippleException(e);
        }
    }

    public void setEager(final boolean eager) throws RippleException {
        if (eager) {
            if (!this.eager) {
                this.eager = true;
                if (null == readerTask) {
                    createTask();
                }
                Scheduler.add(readerTask);
            }
        } else {
            if (this.eager) {
                this.eager = false;
                readerTask.stop();
            }
        }
    }

    public int available() throws java.io.IOException {
        synchronized (writeIn) {
            return writeIn.available();
        }
    }

    public int read() throws java.io.IOException {
        if (0 == available()) {
            requestByte(true);
        }
        synchronized (writeIn) {
            return writeIn.read();
        }
    }

    synchronized void requestByte(final boolean forRead) throws java.io.IOException {
        if ((!forRead || !(writeIn.available() > 0)) && (forRead || eager)) {
            int c = source.read();
            synchronized (writeIn) {
                readOut.write(c);
            }
        }
    }

    private void createTask() {
        readerTask = new Task() {

            private boolean active = false;

            protected void executeProtected() throws RippleException {
                active = true;
                while (active) {
                    try {
                        requestByte(false);
                    } catch (java.io.IOException e) {
                        throw new RippleException(e);
                    }
                }
            }

            protected void stopProtected() {
                active = false;
            }
        };
    }
}
