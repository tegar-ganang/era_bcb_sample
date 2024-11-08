package jreceiver.server.stream.capture;

import java.io.*;
import org.apache.commons.logging.*;

/**
 * This stream reads data in advance of it being requested. It
 * does this asynchronously.
 *
 * @author Philip Gladstone
 * @version $Revision: 1.6 $ $Date: 2002/07/20 03:15:39 $
 *
 */
public final class ReadAheadInputStream extends InputStream {

    /**
     * The number of allocated bytes in the buffer
     */
    private int bufferSize;

    /**
     * The actual input buffer
     */
    private byte[] buffer;

    /**
     * The offset in the buffer of the next byte to read out
     */
    private int readPtr;

    /**
     * The offset in the buffer of the next byte to write to
     */
    private int writePtr;

    /**
     * Marks when the underlying stream has reached EOF
     */
    private boolean noMore;

    /**
     * Marks when the reader is blocked
     */
    private boolean readerBlocked;

    /**
     * Marks when the write is blocked
     */
    private boolean writerBlocked;

    class ReadAheadThread extends Thread {

        private ReadAheadInputStream stream;

        private InputStream src;

        private boolean stopping;

        ReadAheadThread(ReadAheadInputStream s, InputStream is) {
            stream = s;
            src = is;
            stopping = false;
        }

        public void pleaseStop() {
            stopping = true;
        }

        public InputStream getInputStream() {
            return src;
        }

        public void run() {
            byte[] b = new byte[4096];
            try {
                while (!stopping) {
                    int l = src.read(b);
                    if (l <= 0) break;
                    stream.write(b, 0, l);
                }
            } catch (IOException e) {
            }
            stream.writeDone();
            try {
                src.close();
            } catch (IOException e) {
            }
        }
    }

    ReadAheadThread rat;

    /**
     * Creates a new ReadAheadInputStream.
     *
     * @param s the raw stream to be read
     */
    public ReadAheadInputStream(InputStream s) {
        readPtr = 0;
        writePtr = 0;
        noMore = false;
        bufferSize = 0;
        setBufferSize(65536);
        readerBlocked = false;
        writerBlocked = false;
        rat = new ReadAheadThread(this, s);
        rat.start();
    }

    /**
     * Standard toString method
     *
     * @return the name of the object
     */
    public String toString() {
        return "ReadAheadInputStream(" + rat.getInputStream() + ")";
    }

    /**
     * This allows the buffer size to be set. Essentially the entire buffer will
     * be used to prebuffer the data stream. The buffer cannot be shrunk -- this
     * function will fail silently under those circumstances.
     *
     * @param size the size of the buffer in bytes.
     */
    public synchronized void setBufferSize(int size) {
        if (size <= bufferSize) return;
        byte[] b = new byte[size];
        if (readPtr < writePtr) {
            int l = writePtr - readPtr;
            System.arraycopy(buffer, readPtr, b, 0, l);
            readPtr = 0;
            writePtr = l;
        } else if (readPtr > writePtr) {
            int l;
            System.arraycopy(buffer, readPtr, b, 0, bufferSize - readPtr);
            System.arraycopy(buffer, 0, b, bufferSize - readPtr, writePtr);
            writePtr = bufferSize - readPtr + writePtr;
            readPtr = 0;
        }
        buffer = b;
        bufferSize = size;
    }

    /**
     * The standard read function. It returns the data.
     *
     * @param b the byte array to read into
     * @param offset the offset to read into
     * @param len the maximum length to read
     * @returns the number of bytes actually read
     * @exception IOException if something bad happens
     */
    public synchronized int read(byte[] b, int offset, int len) {
        while (!noMore && readPtr == writePtr) {
            try {
                readerBlocked = true;
                log.debug(this + ": Waiting for data");
                wait();
                readerBlocked = false;
            } catch (InterruptedException e) {
            }
        }
        if (writerBlocked) notify();
        int l = -1;
        if (readPtr < writePtr) {
            l = Math.min(len, writePtr - readPtr);
        } else if (readPtr > writePtr) {
            l = Math.min(len, bufferSize - readPtr);
        }
        if (l > 0) {
            System.arraycopy(buffer, readPtr, b, offset, l);
            readPtr += l;
            if (readPtr == bufferSize) {
                readPtr = 0;
            }
        }
        return l;
    }

    /**
     * Standard read function
     *
     * @param b the byte array to read into
     * @exception IOException if anything bad happens
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * The standard single byte read function
     *
     * @returns the byte read
     * @exception IOException if something bad happens
     */
    public synchronized int read() throws IOException {
        if (readPtr != writePtr) {
            byte b = buffer[readPtr];
            readPtr++;
            if (readPtr == bufferSize) {
                readPtr = 0;
            }
            return b;
        }
        if (!noMore) {
            byte b[] = new byte[1];
            int l = read(b);
            if (l == 1) return b[0];
        }
        return -1;
    }

    /**
     * The standard available function -- returns the number of bytes that
     * can be read without blocking. This is not to say that if you try and read
     * this many, you will get them back in one go, but you will not block.
     * Note that once the source stream is closed, this number goes to a large value
     *
     * @returns the number of bytes that can be read without blocking
     */
    public synchronized int available() {
        int l = 0;
        if (noMore) return Integer.MAX_VALUE;
        if (readPtr < writePtr) {
            l = writePtr - readPtr;
        } else if (readPtr > writePtr) {
            l = bufferSize - readPtr + writePtr;
        }
        return l;
    }

    /**
     * This closes the stream and release resources. It stops the readahead thread
     * and will (eventually) close the underlying stream
     */
    public synchronized void close() {
        log.debug(this + ": ReadAheadInputStream closing");
        noMore = true;
        rat.pleaseStop();
        notifyAll();
    }

    /**
     * This looks like the standard write method for an OutputStream. It is used
     * to write data into the readAhead buffer.
     *
     * @param b the byte array
     * @param off the offset into the array to write from
     * @param len the number of bytes to write
     * @exception IOException when something bad happens
     */
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            int l;
            if (noMore) {
                throw new IOException("writing after close");
            }
            if (readPtr <= writePtr) {
                l = Math.min(bufferSize - writePtr - (readPtr == 0 ? 1 : 0), len);
            } else {
                l = Math.min(readPtr - writePtr - 1, len);
            }
            if (l == 0) {
                try {
                    writerBlocked = true;
                    log.debug(this + "Waiting for space");
                    wait();
                    writerBlocked = false;
                } catch (InterruptedException e) {
                }
            } else {
                System.arraycopy(b, off, buffer, writePtr, l);
                writePtr += l;
                len -= l;
                off += l;
            }
            if (writePtr == bufferSize) writePtr = 0;
            if (readerBlocked) notify();
        }
    }

    /**
     * This signals that the last write method call has been made. This is done
     * by the readAhead thread when the underlying stream comes to an end
     */
    public synchronized void writeDone() {
        noMore = true;
        notifyAll();
    }

    /**
     * logging object
     */
    protected static Log log = LogFactory.getLog(ReadAheadInputStream.class);
}
