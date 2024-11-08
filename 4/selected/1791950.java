package org.nwn.common;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Threaded stream copying. Was designed to copy from stdout/stderr of processes to
 * main process's stdout/stderr.
 *
 * It will stop on the first IOException or when the stream closes.
 * By default, this will not close the streams it uses, since it is assumed that the out
 * stream is stdout and the in stream is from a process. The former is closed when the whole
 * application is closed and the latter is closed when the process is done/killed.
 *
 *
 * @author Niels "Moon" Thykier
 * @author Morten "Starswifter" Sørensen
 */
public class StreamCopier extends Thread {

    private InputStream in;

    private OutputStream out;

    private boolean unbuffered;

    private boolean closeInStream = false;

    private boolean closeOutStream = false;

    private IOException except;

    public StreamCopier(InputStream in, OutputStream out) {
        this(in, out, false);
    }

    public StreamCopier(InputStream in, OutputStream out, boolean unbuffered) {
        this.in = in;
        this.out = out;
        this.unbuffered = unbuffered;
    }

    /**
	 * @param closeIn if true, the input stream will be closed when the thread is done.
	 * @param closeOut if true, the out stream will be closed when the thread is done.
	 */
    public void closeStreams(boolean closeIn, boolean closeOut) {
        this.closeInStream = closeIn;
        this.closeOutStream = closeOut;
    }

    @Override
    public void run() {
        byte buffer[] = new byte[512];
        int read;
        if (unbuffered) {
            try {
                int avail;
                while ((read = in.read(buffer, 0, 1)) != -1) {
                    out.write(buffer, 0, read);
                    while ((avail = in.available()) > 0) {
                        read = in.read(buffer, 0, avail);
                        out.write(buffer, 0, read);
                    }
                }
            } catch (IOException e) {
                except = e;
            }
        } else {
            BufferedInputStream buffered = new BufferedInputStream(in);
            try {
                while ((read = buffered.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (IOException e) {
                except = e;
            }
        }
        if (closeInStream) {
            try {
                in.close();
            } catch (IOException e) {
                if (except == null) {
                    except = e;
                }
            }
        }
        if (closeOutStream) {
            try {
                out.close();
            } catch (IOException e) {
                if (except == null) {
                    except = e;
                }
            }
        }
    }

    /**
	 * @return true if there were no IOExceptions during execution.
	 * @throws IllegalStateException if the thread is still alive.
	 */
    public boolean isSuccessful() {
        if (isAlive()) {
            throw new IllegalStateException();
        }
        return except != null;
    }

    /**
	 * @return The exception that interrupted the copy or null if no such exception has occurred.
	 */
    public IOException getException() {
        return except;
    }
}
