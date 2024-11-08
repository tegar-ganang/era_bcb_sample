package robocode.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Mathew A. Nelson (original)
 */
public class BufferedPipedOutputStream extends OutputStream {

    byte buf[];

    int readIndex;

    int writeIndex;

    boolean waiting;

    private BufferedPipedInputStream in;

    private boolean closed;

    private boolean skipLines;

    private BufferedPipedOutputStream() {
    }

    public BufferedPipedOutputStream(int bufferSize, boolean skipLines, boolean blocking) {
        this.buf = new byte[bufferSize];
        this.skipLines = skipLines;
    }

    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed.");
        }
        synchronized (this) {
            buf[writeIndex++] = (byte) (b & 0xff);
            if (writeIndex == buf.length) {
                writeIndex = 0;
            }
            if (writeIndex == readIndex) {
                if (skipLines) {
                    setReadIndexToNextLine();
                } else {
                    throw new IOException("Buffer is full.");
                }
            }
            if (waiting) {
                notifyAll();
            }
        }
    }

    private void setReadIndexToNextLine() {
        while (buf[readIndex] != '\n') {
            readIndex++;
            if (readIndex == buf.length) {
                readIndex = 0;
            }
            if (readIndex == writeIndex) {
                return;
            }
        }
        readIndex++;
        if (readIndex == buf.length) {
            readIndex = 0;
        }
    }

    protected synchronized int read() throws IOException {
        while (readIndex == writeIndex) {
            waiting = true;
            try {
                if (!closed) {
                    wait(10000);
                }
                if (closed) {
                    return -1;
                }
            } catch (InterruptedException e) {
                throw new IOException("read interrupted");
            }
        }
        int result = (int) buf[readIndex++];
        if (readIndex == buf.length) {
            readIndex = 0;
        }
        return result;
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        int first = read();
        if (first == -1) {
            return -1;
        }
        synchronized (this) {
            b[off] = (byte) (first & 0xff);
            int count = 1;
            for (int i = 1; readIndex != writeIndex && i < len; i++) {
                b[off + i] = buf[readIndex++];
                count++;
                if (readIndex == buf.length) {
                    readIndex = 0;
                }
            }
            return count;
        }
    }

    protected int available() {
        if (writeIndex == readIndex) {
            return 0;
        } else if (writeIndex > readIndex) {
            return writeIndex - readIndex;
        } else {
            return buf.length - readIndex + writeIndex;
        }
    }

    public BufferedPipedInputStream getInputStream() {
        if (in == null) {
            in = new BufferedPipedInputStream(this);
        }
        return in;
    }

    public synchronized void close() {
        closed = true;
        if (waiting) {
            notifyAll();
        }
    }
}
