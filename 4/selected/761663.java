package com.sshtools.j2ssh.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

/**
 * <p>
 * This class provides an alternative method of storing data, used within the
 * API where Piped Streams could have been used. We found that Piped streams
 * would lock if a thread attempted to read to data when the OutputStream attached
     * was not being read; since we have no control over when the user will actually
 * read the data, this behaviour led us to develop this dynamic buffer which
 * will automatically grow if the buffer is full.
 * </p>
 *  *
 * @author Lee David Painter
 * @version $Revision: 1.20 $
 */
public class DynamicBuffer {

    private static Log log = LogFactory.getLog(DynamicBuffer.class);

    /** Buffer size when the dynamic buffer is opened */
    protected static final int DEFAULT_BUFFER_SIZE = 32768;

    /** The buffer */
    protected byte[] buf;

    /** The current write position */
    protected int writepos = 0;

    /** The current read position */
    protected int readpos = 0;

    /** This buffers InputStream */
    protected InputStream in;

    /** This buffers OutputStream */
    protected OutputStream out;

    private boolean closed = false;

    private int interrupt = 5000;

    /**
     * Creates a new DynamicBuffer object.
     */
    public DynamicBuffer() {
        buf = new byte[DEFAULT_BUFFER_SIZE];
        in = new DynamicBufferInputStream();
        out = new DynamicBufferOutputStream();
    }

    /**
     * Get the InputStream of this buffer. Use the stream to read data from
     * this buffer.
     *
     * @return
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * Get the OutputStream of the buffer. Use this stream to write data to
     * the buffer.
     *
     * @return
     */
    public OutputStream getOutputStream() {
        return out;
    }

    private synchronized void verifyBufferSize(int count) {
        if (count > (buf.length - writepos)) {
            System.arraycopy(buf, readpos, buf, 0, writepos - readpos);
            writepos -= readpos;
            readpos = 0;
        }
        if (count > (buf.length - writepos)) {
            byte[] tmp = new byte[buf.length + DEFAULT_BUFFER_SIZE];
            System.arraycopy(buf, 0, tmp, 0, writepos - readpos);
            buf = tmp;
        }
    }

    /**
     * Return the number of bytes of data available to be read from the buffer
     * @return
     */
    protected synchronized int available() {
        return writepos - readpos;
    }

    private synchronized void block() throws InterruptedException {
        if (log.isDebugEnabled()) {
            log.debug("Buffer size: " + String.valueOf(buf.length));
            log.debug("Unread data: " + String.valueOf(writepos - readpos));
        }
        if (!closed) {
            while ((readpos >= writepos) && !closed) {
                wait(interrupt);
            }
        }
    }

    /**
     * Closes the buffer
     */
    public synchronized void close() {
        if (!closed) {
            closed = true;
            notifyAll();
        }
    }

    /**
     * Write a byte array to the buffer
     *
     * @param b
     *
     * @throws IOException
     */
    protected synchronized void write(int b) throws IOException {
        if (closed) {
            throw new IOException("The buffer is closed");
        }
        verifyBufferSize(1);
        buf[writepos] = (byte) b;
        writepos++;
        notifyAll();
    }

    /**
     *
     *
     * @param data
     * @param offset
     * @param len
     *
     * @throws IOException
     */
    protected synchronized void write(byte[] data, int offset, int len) throws IOException {
        if (closed) {
            throw new IOException("The buffer is closed");
        }
        verifyBufferSize(len);
        System.arraycopy(data, offset, buf, writepos, len);
        writepos += len;
        notifyAll();
    }

    public void setBlockInterrupt(int interrupt) {
        this.interrupt = interrupt;
    }

    /**
     * Read a byte from the buffer
     *
     * @return
     *
     * @throws IOException
     * @throws InterruptedIOException
     */
    protected synchronized int read() throws IOException {
        try {
            block();
        } catch (InterruptedException ex) {
            throw new InterruptedIOException("The blocking operation was interrupted");
        }
        if (closed && (available() <= 0)) {
            return -1;
        }
        return (int) buf[readpos++];
    }

    /**
     * Read a byte array from the buffer
     *
     * @param data
     * @param offset
     * @param len
     *
     * @return
     *
     * @throws IOException
     * @throws InterruptedIOException
     */
    protected synchronized int read(byte[] data, int offset, int len) throws IOException {
        try {
            block();
        } catch (InterruptedException ex) {
            throw new InterruptedIOException("The blocking operation was interrupted");
        }
        if (closed && (available() <= 0)) {
            return -1;
        }
        int read = (len > (writepos - readpos)) ? (writepos - readpos) : len;
        System.arraycopy(buf, readpos, data, offset, read);
        readpos += read;
        return read;
    }

    /**
     * Flush data
     *
     * @throws IOException
     */
    protected synchronized void flush() throws IOException {
        notifyAll();
    }

    class DynamicBufferInputStream extends InputStream {

        public int read() throws IOException {
            return DynamicBuffer.this.read();
        }

        public int read(byte[] data, int offset, int len) throws IOException {
            return DynamicBuffer.this.read(data, offset, len);
        }

        public int available() {
            return DynamicBuffer.this.available();
        }

        public void close() {
            DynamicBuffer.this.close();
        }
    }

    class DynamicBufferOutputStream extends OutputStream {

        public void write(int b) throws IOException {
            DynamicBuffer.this.write(b);
        }

        public void write(byte[] data, int offset, int len) throws IOException {
            DynamicBuffer.this.write(data, offset, len);
        }

        public void flush() throws IOException {
            DynamicBuffer.this.flush();
        }

        public void close() {
            DynamicBuffer.this.close();
        }
    }
}
