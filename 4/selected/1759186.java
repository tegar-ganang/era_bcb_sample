package spaghettiserver;

import java.io.IOException;
import java.io.InputStream;

/**
* This class is equivalent to <code>java.io.PipedInputStream</code>. In the
* interface it only adds a constructor which allows for specifying the buffer
* size. Its implementation, however, is much simpler and a lot more efficient
* than its equivalent. It doesn't rely on polling. Instead it uses proper
* synchronization with its counterpart PipedOutputStream.
*
* Multiple readers can read from this stream concurrently. The block asked for
* by a reader is delivered completely, or until the end of the stream if less
* is available. Other readers can't come in between.
*/
public class PipedInputStream extends InputStream {

    byte[] buffer;

    boolean closed = false;

    int readLaps = 0;

    int readPosition = 0;

    PipedOutputStream source;

    int writeLaps = 0;

    int writePosition = 0;

    /**
	* Creates an unconnected PipedInputStream with a default buffer size.
	* @exception IOException
	*/
    public PipedInputStream() throws IOException {
        this(null);
    }

    /**
	* Creates a PipedInputStream with a default buffer size and connects it to
	* source.
	* @exception IOException It was already connected.
	*/
    public PipedInputStream(PipedOutputStream source) throws IOException {
        this(source, 0x10000);
    }

    /**
	* Creates a PipedInputStream with buffer size <code>bufferSize</code> and
	* connects it to <code>source</code>.
	* @exception IOException It was already connected.
	*/
    public PipedInputStream(PipedOutputStream source, int bufferSize) throws IOException {
        if (source != null) {
            connect(source);
        }
        buffer = new byte[bufferSize];
    }

    /**
	* Return the number of bytes of data available from this stream without blocking.
	*/
    public int available() throws IOException {
        return writePosition > readPosition ? writePosition - readPosition : (writePosition < readPosition ? buffer.length - readPosition + 1 + writePosition : (writeLaps > readLaps ? buffer.length : 0));
    }

    /**
	* Closes the pipe.
	* @exception IOException The pipe is not connected.
	*/
    public void close() throws IOException {
        if (source == null) {
            throw new IOException("Unconnected pipe");
        }
        synchronized (buffer) {
            closed = true;
            buffer.notifyAll();
        }
    }

    /**
	* Connects this input stream to an output stream.
	* @exception IOException The pipe is already connected.
	*/
    public void connect(PipedOutputStream source) throws IOException {
        if (this.source != null) {
            throw new IOException("Pipe already connected");
        }
        this.source = source;
        source.sink = this;
    }

    /**
	* Closes the input stream if it is open.
	*/
    protected void finalize() throws Throwable {
        close();
    }

    /**
	* Unsupported - does nothing.
	*/
    public void mark(int readLimit) {
        return;
    }

    /**
	* returns whether or not mark is supported.
	*/
    public boolean markSupported() {
        return false;
    }

    /**
	* reads a byte of data from the input stream.
	* @return the byte read, or -1 if end-of-stream was reached.
	*/
    public int read() throws IOException {
        byte[] b = new byte[1];
        int result = read(b);
        return result == -1 ? -1 : 0xff & b[0];
    }

    /**
	* Reads data from the input stream into a buffer.
	* @exception IOException
	*/
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
	* Reads data from the input stream into a buffer, starting at the specified offset,
	* and for the length requested.
	* @exception IOException The pipe is not connected.
	*/
    public int read(byte[] b, int off, int len) throws IOException {
        if (source == null) {
            throw new IOException("Unconnected pipe");
        }
        synchronized (buffer) {
            if (writePosition == readPosition && writeLaps == readLaps) {
                if (closed) {
                    return -1;
                }
                try {
                    buffer.wait();
                } catch (InterruptedException e) {
                    throw new IOException(e.getMessage());
                }
                return read(b, off, len);
            }
            int amount = Math.min(len, (writePosition > readPosition ? writePosition : buffer.length) - readPosition);
            System.arraycopy(buffer, readPosition, b, off, amount);
            readPosition += amount;
            if (readPosition == buffer.length) {
                readPosition = 0;
                ++readLaps;
            }
            if (amount < len) {
                int second = read(b, off + amount, len - amount);
                return second == -1 ? amount : amount + second;
            } else {
                buffer.notifyAll();
            }
            return amount;
        }
    }
}
