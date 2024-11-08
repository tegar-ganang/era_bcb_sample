package spaghettiserver;

import java.io.IOException;
import java.io.OutputStream;

/**
* This class is equivalent to java.io.PipedOutputStream. In the
* interface it only adds a constructor which allows for specifying the buffer
* size. Its implementation, however, is much simpler and a lot more efficient
* than its equivalent. It doesn't rely on polling. Instead it uses proper
* synchronization with its counterpart PipedInputStream.
*
* Multiple writers can write in this stream concurrently. The block written
* by a writer is put in completely. Other writers can't come in between.
*/
public class PipedOutputStream extends OutputStream {

    PipedInputStream sink;

    /**
	* Creates an unconnected PipedOutputStream.
	* @exception IOException
	*/
    public PipedOutputStream() throws IOException {
        this(null);
    }

    /**
	* Creates a PipedOutputStream with a default buffer size and connects it to
	* <code>sink</code>.
	* @exception IOException It was already connected.
	*/
    public PipedOutputStream(PipedInputStream sink) throws IOException {
        this(sink, 0x10000);
    }

    /**
	* Creates a PipedOutputStream with buffer size <code>bufferSize</code> and
	* connects it to <code>sink</code>.
	* @exception IOException It was already connected.
	*/
    public PipedOutputStream(PipedInputStream sink, int bufferSize) throws IOException {
        if (sink != null) {
            connect(sink);
            sink.buffer = new byte[bufferSize];
        }
    }

    /**
	* Closes the input stream.
	* @exception IOException The pipe is not connected.
	*/
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
	* Connects the output stream to an input stream.
	* @exception IOException The pipe is already connected.
	*/
    public void connect(PipedInputStream sink) throws IOException {
        if (this.sink != null) {
            throw new IOException("Pipe already connected");
        }
        this.sink = sink;
        sink.source = this;
    }

    /**
	* Closes the output stream if it is open.
	*/
    protected void finalize() throws Throwable {
        close();
    }

    /**
	* forces any buffered data to be written.
	* @exception IOException
	*/
    public void flush() throws IOException {
        synchronized (sink.buffer) {
            sink.buffer.notifyAll();
        }
    }

    /**
	* writes a byte of data to the output stream.
	* @exception IOException
	*/
    public void write(int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    /**
	* Writes a buffer of data to the output stream.
	* @exception IOException
	*/
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
	* writes data to the output stream from a buffer, starting at the named offset,
	* and for the named length.
	* @exception IOException The pipe is not connected or a reader has closed
	* it.
	*/
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
