package xbird.util.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is equivalent to <code>java.io.PipedOutputStream</code>. In the
 * interface it only adds a constructor which allows for specifying the buffer
 * size. Its implementation, however, is much simpler and a lot more efficient
 * than its equivalent. It doesn't rely on polling. Instead it uses proper
 * synchronization with its counterpart <code>be.re.io.PipedInputStream</code>.
 *
 * Multiple writers can write in this stream concurrently. The block written
 * by a writer is put in completely. Other writers can't come in between.
 * 
 * @author WD
 * @link http://developer.java.sun.com/developer/bugParade/bugs/4404700.html
 * @see FastPipedOutputStream
 */
public class FastPipedOutputStream extends OutputStream {

    FastPipedInputStream sink;

    /**
     * Creates an unconnected PipedOutputStream.
     */
    public FastPipedOutputStream() {
        super();
    }

    /**
     * Creates a PipedOutputStream with a default buffer size and connects it to
     * <code>sink</code>.
     * @exception IOException It was already connected.
     */
    public FastPipedOutputStream(FastPipedInputStream sink) throws IOException {
        this(sink, 65536);
    }

    /**
     * Creates a PipedOutputStream with buffer size <code>bufferSize</code> and
     * connects it to <code>sink</code>.
     * @exception IOException It was already connected.
     */
    public FastPipedOutputStream(FastPipedInputStream sink, int bufferSize) throws IOException {
        super();
        if (sink != null) {
            connect(sink);
            sink.buffer = new byte[bufferSize];
        }
    }

    /**
     * @exception IOException The pipe is not connected.
     */
    @Override
    public void close() throws IOException {
        if (sink == null) {
            throw new IOException("Unconnected pipe");
        }
        synchronized (sink.buffer) {
            sink.closed = true;
            flush();
        }
    }

    /**
     * @exception IOException The pipe is already connected.
     */
    public void connect(FastPipedInputStream sink) throws IOException {
        if (this.sink != null) {
            throw new IOException("Pipe already connected");
        }
        this.sink = sink;
        sink.source = this;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    @Override
    public void flush() throws IOException {
        synchronized (sink.buffer) {
            sink.buffer.notifyAll();
        }
    }

    public void write(int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * @exception IOException The pipe is not connected or a reader has closed it.
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (sink == null) {
            throw new IOException("Unconnected pipe");
        }
        if (sink.closed) {
            throw new IOException("Broken pipe");
        }
        synchronized (sink.buffer) {
            if (sink.writePosition == sink.readPosition && sink.writeLaps > sink.readLaps) {
                try {
                    sink.buffer.wait();
                } catch (InterruptedException e) {
                    throw new IOException(e.getMessage());
                }
                write(b, off, len);
                return;
            }
            int amount = Math.min(len, (sink.writePosition < sink.readPosition ? sink.readPosition : sink.buffer.length) - sink.writePosition);
            System.arraycopy(b, off, sink.buffer, sink.writePosition, amount);
            sink.writePosition += amount;
            if (sink.writePosition == sink.buffer.length) {
                sink.writePosition = 0;
                ++sink.writeLaps;
            }
            if (amount < len) {
                write(b, off + amount, len - amount);
            } else {
                sink.buffer.notifyAll();
            }
        }
    }
}
