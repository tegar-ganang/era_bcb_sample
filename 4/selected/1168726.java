package vavix.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

/**
 * An efficient connected stream pair for communicating between the threads of
 * an application. This provides a less-strict contract than the standard piped
 * streams, resulting in much-improved performance. Also supports non-blocking
 * operation.
 * 
 * @author Copyright (c) 2002 Merlin Hughes <merlin@merlin.org>
 */
public class AdvancedPipedInputStream extends InputStream {

    /** */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** */
    private static final float DEFAULT_HYSTERESIS = 0.75f;

    /** */
    private static final int DEFAULT_TIMEOUT_MS = 1000;

    /** flag indicates whether method applies to reader or writer */
    private static final boolean READER = false, WRITER = true;

    /** internal pipe buffer */
    private byte[] buffer;

    /** read/write index */
    private int readx, writex;

    /** pipe capacity, hysteresis level */
    private int capacity, level;

    /** flags */
    private boolean eof, closed, sleeping, nonBlocking;

    /** reader/writer thread */
    private Thread reader, writer;

    /** pending exception */
    private IOException exception;

    /** deadlock-breaking timeout */
    private int timeout = DEFAULT_TIMEOUT_MS;

    /** */
    public AdvancedPipedInputStream() {
        this(DEFAULT_BUFFER_SIZE, DEFAULT_HYSTERESIS);
    }

    /** */
    public AdvancedPipedInputStream(int bufferSize) {
        this(bufferSize, DEFAULT_HYSTERESIS);
    }

    /**
     * e.g., hysteresis .75 means sleeping reader/writer is not immediately
     * woken until the buffer is 75% full/empty
     */
    public AdvancedPipedInputStream(int bufferSize, float hysteresis) {
        if ((hysteresis < 0.0) || (hysteresis > 1.0)) throw new IllegalArgumentException("Hysteresis: " + hysteresis);
        capacity = bufferSize;
        buffer = new byte[capacity];
        level = (int) (bufferSize * hysteresis);
    }

    /** */
    public void setTimeout(int ms) {
        this.timeout = ms;
    }

    /** */
    public void setNonBlocking(boolean nonBlocking) {
        this.nonBlocking = nonBlocking;
    }

    /** */
    private byte[] one = new byte[1];

    public int read() throws IOException {
        int amount = read(one, 0, 1);
        return (amount < 0) ? -1 : one[0] & 0xff;
    }

    public synchronized int read(byte data[], int offset, int length) throws IOException {
        if (reader == null) reader = Thread.currentThread();
        if (data == null) {
            throw new NullPointerException();
        } else if ((offset < 0) || (offset + length > data.length) || (length < 0)) {
            throw new IndexOutOfBoundsException();
        } else {
            closedCheck();
            exceptionCheck();
            if (length <= 0) {
                return 0;
            } else {
                int available = checkedAvailable(READER);
                if (available < 0) return -1;
                int contiguous = capacity - (readx % capacity);
                int amount = (length > available) ? available : length;
                if (amount > contiguous) {
                    System.arraycopy(buffer, readx % capacity, data, offset, contiguous);
                    System.arraycopy(buffer, 0, data, offset + contiguous, amount - contiguous);
                } else {
                    System.arraycopy(buffer, readx % capacity, data, offset, amount);
                }
                processed(READER, amount);
                return amount;
            }
        }
    }

    public synchronized long skip(long amount) throws IOException {
        if (reader == null) reader = Thread.currentThread();
        closedCheck();
        exceptionCheck();
        if (amount <= 0) {
            return 0;
        } else {
            int available = checkedAvailable(READER);
            if (available < 0) return 0;
            if (amount > available) amount = available;
            processed(READER, (int) amount);
            return amount;
        }
    }

    /** */
    private void processed(boolean rw, int amount) {
        if (rw == READER) {
            readx = (readx + amount) % (capacity * 2);
        } else {
            writex = (writex + amount) % (capacity * 2);
        }
        if (sleeping && (available(!rw) >= level)) {
            notify();
            sleeping = false;
        }
    }

    public synchronized int available() throws IOException {
        closedCheck();
        exceptionCheck();
        int amount = available(READER);
        return (amount < 0) ? 0 : amount;
    }

    /** */
    private int checkedAvailable(boolean rw) throws IOException {
        try {
            int available;
            while ((available = available(rw)) == 0) {
                if (rw == READER) {
                    exceptionCheck();
                } else {
                    closedCheck();
                }
                brokenCheck(rw);
                if (!nonBlocking) {
                    if (sleeping) notify();
                    sleeping = true;
                    wait(timeout);
                } else {
                    throw new InterruptedIOException("Pipe " + (rw ? "full" : "empty"));
                }
            }
            return available;
        } catch (InterruptedException ex) {
            throw new InterruptedIOException(ex.getMessage());
        }
    }

    /** */
    private int available(boolean rw) {
        int used = (writex + capacity * 2 - readx) % (capacity * 2);
        if (rw == WRITER) {
            return capacity - used;
        } else {
            return (eof && (used == 0)) ? -1 : used;
        }
    }

    public void close() throws IOException {
        close(READER);
    }

    private synchronized void close(boolean rw) throws IOException {
        if (rw == READER) {
            closed = true;
        } else if (!eof) {
            eof = true;
            if (available(READER) > 0) {
                closedCheck();
                brokenCheck(WRITER);
            }
        }
        if (sleeping) {
            notify();
            sleeping = false;
        }
    }

    /** */
    private void exceptionCheck() throws IOException {
        if (exception != null) {
            IOException ex = exception;
            exception = null;
            throw ex;
        }
    }

    /** */
    private void closedCheck() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    /** */
    private void brokenCheck(boolean rw) throws IOException {
        Thread thread = (rw == WRITER) ? reader : writer;
        if ((thread != null) && !thread.isAlive()) {
            throw new IOException("Broken pipe");
        }
    }

    /** */
    private synchronized void writeImpl(byte[] data, int offset, int length) throws IOException {
        if (writer == null) {
            writer = Thread.currentThread();
        }
        if (eof || closed) {
            throw new IOException("Stream closed");
        } else {
            int written = 0;
            try {
                do {
                    int available = checkedAvailable(WRITER);
                    int contiguous = capacity - (writex % capacity);
                    int amount = (length > available) ? available : length;
                    if (amount > contiguous) {
                        System.arraycopy(data, offset, buffer, writex % capacity, contiguous);
                        System.arraycopy(data, offset + contiguous, buffer, 0, amount - contiguous);
                    } else {
                        System.arraycopy(data, offset, buffer, writex % capacity, amount);
                    }
                    processed(WRITER, amount);
                    written += amount;
                } while (written < length);
            } catch (InterruptedIOException ex) {
                ex.bytesTransferred = written;
                throw ex;
            }
        }
    }

    /** */
    private synchronized void setException(IOException ex) throws IOException {
        if (exception != null) {
            throw new IOException("Exception already set: " + exception);
        }
        brokenCheck(WRITER);
        this.exception = ex;
        if (sleeping) {
            notify();
            sleeping = false;
        }
    }

    /** return an OutputStreamImpl associated with this pipe */
    public OutputStreamEx getOutputStream() {
        return new OutputStreamImpl();
    }

    /** */
    private class OutputStreamImpl extends OutputStreamEx {

        /** */
        @SuppressWarnings("hiding")
        private byte[] one = new byte[1];

        /** */
        public void write(int datum) throws IOException {
            one[0] = (byte) datum;
            write(one, 0, 1);
        }

        /** */
        public void write(byte[] data, int offset, int length) throws IOException {
            if (data == null) {
                throw new NullPointerException();
            } else if ((offset < 0) || (offset + length > data.length) || (length < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (length > 0) {
                AdvancedPipedInputStream.this.writeImpl(data, offset, length);
            }
        }

        /** */
        public void close() throws IOException {
            AdvancedPipedInputStream.this.close(WRITER);
        }

        /** */
        public void setException(IOException ex) throws IOException {
            AdvancedPipedInputStream.this.setException(ex);
        }
    }

    /** static OutputStream extension with setException() method */
    public abstract static class OutputStreamEx extends OutputStream {

        /** */
        public abstract void setException(IOException ex) throws IOException;
    }
}
