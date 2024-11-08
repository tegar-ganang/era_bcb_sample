package taskgraph.pipes;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import taskgraph.Config;

/**
 * An input pipe for object references as elements. This pipe, the sink, 
 * connects to an {@code OutputPipe}, the source, for a sequential 
 * transmission of elements. Code in one thread makes write calls to an 
 * instance of an output pipe, where the data is sent to this sink. Code 
 * in another thread makes read calls to this input pipe in order to get 
 * the elements as a FIFO buffer.
 * 
 * @author Armando Blancas
 * @param <E> The type of elements transported by this pipe.
 */
public class InputPipe<E> implements Closeable {

    private static final int DEFAULT_PIPE_SIZE = Config.get().channelCapacity();

    private static final int TIMEOUT_PERIOD = 500;

    private final E[] buffer;

    boolean connected = false;

    boolean closedByWriter = false;

    volatile boolean closedByReader = false;

    Thread readSide;

    Thread writeSide;

    int in = -1;

    int out = 0;

    /**
     * InputPipe default constructor.
     * 
     * Creates an {@code InputPipe} so that it is not yet 
     * {@linkplain #connect(taskgraph.pipes.OutputPipe) connected}. 
     * It must be {@linkplain taskgraph.pipes.OutputPipe#connect(
     * taskgraph.pipes.InputPipe) connected} to a
     * {@code OutputPipe} before being used.
     */
    public InputPipe() {
        this(DEFAULT_PIPE_SIZE);
    }

    /**
     * InputPipe constructor.
     * 
     * Creates an {@code InputPipe} so that it is connected to the output
     * pipe {@code source}. Elements written to {@code source} will then be
     * available as input from this pipe.
     *
     * @param source The source element pipe to connect to.
     * @throws  IOException If an I/O error occurs.
     */
    public InputPipe(final OutputPipe<E> source) throws IOException {
        this(source, DEFAULT_PIPE_SIZE);
    }

    /**
     * InputPipe constructor.
     * 
     * Creates an {@code InputPipe} so that it is connected to the 
     * output pipe {@code source} and uses the specified pipe size for
     * the pipe's buffer. Elements written to {@code source} will then
     * be available as input from this pipe.
     *
     * @param source   The source element stream to connect to.
     * @param pipeSize The size of the pipe's buffer.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalArgumentException If {@code pipeSize <= 0}.
     */
    public InputPipe(final OutputPipe<E> source, final int pipeSize) throws IOException {
        this(pipeSize);
        connect(source);
    }

    /**
     * InputPipe constructor.
     * 
     * Creates an {@code InputPipe} so that it is not yet 
     * {@linkplain #connect(taskgraph.pipes.OutputPipe) connected}. 
     * It must be {@linkplain taskgraph.pipes.OutputPipe#connect(
     * taskgraph.pipes.InputPipe) connected} to a
     * {@code OutputPipe} before being used.
     *
     * @param pipeSize the size of the pipe's buffer.
     * @throws IllegalArgumentException if {@code pipeSize <= 0}.
     */
    @SuppressWarnings("unchecked")
    public InputPipe(final int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe Size <= 0");
        }
        buffer = (E[]) new Object[pipeSize];
    }

    /**
     * Connects to an output pipe.
     * 
     * Causes this pipe to be connected to the output pipe {@code source}.
     * If this object is already connected to some other output pipe, 
     * an {@code IOException} is thrown.
     * <p>If {@code source} is an unconnected output pipe and {@code sink}
     * is an unconnected input pipe, they may be connected by either the call:
     * <pre>sink.connect(source)</pre>
     * <p>or the call:
     * <pre>source.connect(sink)</pre>
     * <p>The two calls have the same effect.
     *
     * @param source The element source pipe to connect to.
     * @throws IOException If an I/O error occurs.
     */
    public void connect(OutputPipe<E> source) throws IOException {
        source.connect(this);
    }

    /**
     * Receives an element as data. This method will block if no input is
     * available.
     * 
     * @param e The element value being received.
     * @throws IOException If the pipe is {@code broken},
     *		   {@link #connect(taskgraph.pipes.OutputPipe) unconnected},
     *		   closed, or if an I/O error occurs.
     */
    protected synchronized void receive(E e) throws IOException {
        checkStateForReceive();
        writeSide = Thread.currentThread();
        if (in == out) awaitSpace();
        if (in < 0) {
            in = 0;
            out = 0;
        }
        buffer[in++] = e;
        if (in >= buffer.length) {
            in = 0;
        }
    }

    synchronized void receive(E[] e, int off, int len) throws IOException {
        checkStateForReceive();
        writeSide = Thread.currentThread();
        int bytesToTransfer = len;
        while (bytesToTransfer > 0) {
            if (in == out) awaitSpace();
            int nextTransferAmount = 0;
            if (out < in) {
                nextTransferAmount = buffer.length - in;
            } else if (in < out) {
                if (in == -1) {
                    in = out = 0;
                    nextTransferAmount = buffer.length - in;
                } else {
                    nextTransferAmount = out - in;
                }
            }
            if (nextTransferAmount > bytesToTransfer) nextTransferAmount = bytesToTransfer;
            assert nextTransferAmount > 0;
            System.arraycopy(e, off, buffer, in, nextTransferAmount);
            bytesToTransfer -= nextTransferAmount;
            off += nextTransferAmount;
            in += nextTransferAmount;
            if (in >= buffer.length) {
                in = 0;
            }
        }
    }

    private void checkStateForReceive() throws IOException {
        if (!connected) {
            throw new IOException("Pipe not connected");
        } else if (closedByWriter || closedByReader) {
            throw new IOException("Pipe closed");
        } else if (readSide != null && !readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
    }

    private void awaitSpace() throws IOException {
        while (in == out) {
            checkStateForReceive();
            notifyAll();
            try {
                wait(TIMEOUT_PERIOD);
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
    }

    synchronized void receivedLast() {
        closedByWriter = true;
        notifyAll();
    }

    /**
     * Reads the next element from this input pipe. 
     * This method blocks until input data is available, the output end 
     * of the pipe is closed, or an exception is thrown.
     *
     * @return The next element from this pipe.
     * @throws IOException If the pipe is {@code broken},
     *		   {@link #connect(taskgraph.pipes.OutputPipe) unconnected},
     *		   or if an I/O error occurs.
     * @throws EOFException If the output end of the pipe is closed.
     */
    public synchronized E read() throws IOException {
        if (!connected) {
            throw new IOException("Pipe not connected");
        } else if (closedByReader) {
            throw new IOException("Pipe closed");
        } else if (writeSide != null && !writeSide.isAlive() && !closedByWriter && (in < 0)) {
            throw new IOException("Write end dead");
        }
        readSide = Thread.currentThread();
        int trials = 2;
        while (in < 0) {
            if (closedByWriter) {
                throw new EOFException("Pipe closed");
            }
            if ((writeSide != null) && (!writeSide.isAlive()) && (--trials < 0)) {
                throw new IOException("Pipe broken");
            }
            notifyAll();
            try {
                wait(TIMEOUT_PERIOD);
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
        E ret = buffer[out];
        buffer[out++] = null;
        if (out >= buffer.length) {
            out = 0;
        }
        if (in == out) {
            in = -1;
        }
        return ret;
    }

    /**
     * Reads into an array of elements.
     * 
     * Reads some number of elements from this input pipe and stores them into
     * the element array {@code e}. The number of elements actually 
     * read is returned as an {@code int}. This method blocks until input data 
     * is available, end of file is detected, or an exception is thrown.
     *
     * <p>If the length of {@code e} is zero, then no elements are read and
     * {@code 0} is returned; otherwise, there is an attempt to read at
     * least one element. If no element is available because the output
     * end is closed, an {@code IOException} is thrown; otherwise, at least one
     * element is read and stored into {@code e}.
     *
     * <p>The first element read is stored into element {@code e[0]}, the
     * next one into {@code e[1]}, and so on. The number of elements read is,
     * at most, equal to the length of {@code e}. Let <i>k</i> be the
     * number of elements actually read; these elements will be stored in elements
     * {@code e[0]} through {@code e[k-1]}, leaving elements {@code e[k]} 
     * through {@code e[e.length-1]} unaffected.
     * 
     * <p>The general contract for {@code read(e)} is that it should have 
     * exactly the same effect as the call {@code read(e, 0, e.length)}.
     *
     * @param e The element array into which the data is read.
     * @return The total number of elements read into the array.
     * @throws IOException If the first element cannot be read,
     *         or if some other I/O error occurs.
     * @throws EOFException If the output end of the pipe is closed.
     * @throws NullPointerException If {@code e} is {@code null}.
     */
    public int read(E[] e) throws IOException {
        return read(e, 0, e.length);
    }

    /**
     * Reads into an array of elements.
     * 
     * Reads up to {@code len} elements from this input pipe into an array.
     * Less than {@code len} elements will be read if the output end of the 
     * data pipe is closed or if {@code len} exceeds the pipe's buffer size. 
     * If {@code len} is zero, then no elements are read and 0 is returned; 
     * otherwise, the method blocks until at least 1 element of input is 
     * available, the output end is closed, or an exception is thrown.
     *
     * @param e    The elements array into which the data is read.
     * @param off  The start offset in the destination array {@code e}.
     * @param len  The maximum number of elements to read.
     * @return     The total number of elements read into the array.
     * @throws NullPointerException If {@code e} is {@code null}.
     * @throws IndexOutOfBoundsException If {@code off} is negative, 
     *         {@code len} is negative, or {@code len} is greater than 
     *         {@code e.length - off}.
     * @throws IOException If the pipe is {@code broken},
     *		   {@link #connect(taskgraph.pipes.OutputPipe) unconnected},
     *		   or if an I/O error occurs.
     * @throws EOFException If the output end of the pipe is closed.
     */
    public synchronized int read(E[] e, int off, int len) throws IOException {
        if (e == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > e.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        E ch = read();
        e[off] = ch;
        int rlen = 1;
        while ((in >= 0) && (len > 1)) {
            int available;
            if (in > out) {
                available = Math.min((buffer.length - out), (in - out));
            } else {
                available = buffer.length - out;
            }
            if (available > (len - 1)) {
                available = len - 1;
            }
            System.arraycopy(buffer, out, e, off + rlen, available);
            Arrays.fill(buffer, out, out + available, null);
            out += available;
            rlen += available;
            len -= available;
            if (out >= buffer.length) {
                out = 0;
            }
            if (in == out) {
                in = -1;
            }
        }
        return rlen;
    }

    /**
     * Gets the number of elements in the buffer.
     * 
     * Returns the number of elements that can be read from this input
     * pipe without blocking.
     * <p>Returns {@code 0} if this input pipe has been closed by invoking its 
     * {@link #close()} method, or if the output end is
     * {@link #connect(taskgraph.pipes.OutputPipe) unconnected}, or
     * {@code broken}.
     *
     * @return The number of available elements, without blocking. 
     * @throws IOException If an I/O error occurs.
     */
    public synchronized int available() throws IOException {
        if (in < 0) return 0; else if (in == out) return buffer.length; else if (in > out) return in - out; else return in + buffer.length - out;
    }

    /**
     * Discards a number of elements from the buffer.
     * 
     * Makes an attempt to skip over <code>n</code> elements of data from the
     * channel, discarding the skipped ones. However, it may skip over some 
     * smaller number of elements, possibly zero. This may result from any of
     * a number of conditions; reaching end of channel before <code>n</code>
     * elements have been skipped is only one possibility. This method never 
     * throws an <code>EOFException</code>. 
     * The actual number of elements skipped is returned.
     *
     * @param n The number of elements to be skipped.
     * @return The number of elements actually skipped.
     * @throws IOException If an I/O error occurs.
     */
    @SuppressWarnings("unchecked")
    public synchronized int skipElements(int n) throws IOException {
        if (n < 0) throw new IllegalArgumentException("n is negative");
        if (n == 0) return 0;
        int toSkip = Math.min(n, available());
        E[] e = (E[]) new Object[toSkip];
        return read(e);
    }

    /**
     * Closes this input pipe and releases any system resources.
     */
    public void close() {
        closedByReader = true;
        synchronized (this) {
            in = -1;
        }
    }
}
