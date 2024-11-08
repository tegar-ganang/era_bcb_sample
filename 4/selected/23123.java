package taskgraph.pipes;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import taskgraph.Config;

/**
 * An input pipe for {@code long} values. This pipe, the sink, connects to a 
 * {@code LongOutputPipe}, the source, for a sequential transmission of long
 * integers. Code in one thread makes write calls to an instance of an output
 * pipe, where the data is sent to this sink. Code in another thread makes 
 * read calls to this input pipe in order to get the longs as a FIFO buffer.
 * 
 * @author Armando Blancas
 */
public class LongInputPipe implements Closeable {

    private static final int DEFAULT_PIPE_SIZE = Config.get().channelCapacity();

    private static final int TIMEOUT_PERIOD = 500;

    private final long[] buffer;

    boolean connected = false;

    boolean closedByWriter = false;

    volatile boolean closedByReader = false;

    Thread readSide;

    Thread writeSide;

    int in = -1;

    int out = 0;

    /**
     * LongInputPipe default constructor.
     * 
     * Creates a {@code LongInputPipe} so that it is not yet 
     * {@linkplain #connect(taskgraph.pipes.LongOutputPipe) connected}. 
     * It must be {@linkplain taskgraph.pipes.LongOutputPipe#connect(
     * taskgraph.pipes.LongInputPipe) connected} to a
     * {@code LongOutputPipe} before being used.
     */
    public LongInputPipe() {
        this(DEFAULT_PIPE_SIZE);
    }

    /**
     * LongInputPipe constructor.
     * 
     * Creates a {@code LongInputPipe} so that it is connected to the output
     * pipe {@code source}. Long ints written to {@code source} will then be
     * available as input from this pipe.
     *
     * @param source The {@code long} input pipe to connect to.
     * @throws  IOException If an I/O error occurs.
     */
    public LongInputPipe(final LongOutputPipe source) throws IOException {
        this(source, DEFAULT_PIPE_SIZE);
    }

    /**
     * LongInputPipe constructor.
     * 
     * Creates a{@code LongInputPipe} so that it is connected to the 
     * output pipe {@code source} and uses the specified pipe size for
     * the pipe's buffer. Long ints written to {@code source} will then
     * be available as input from this pipe.
     *
     * @param source   The piped {@code long} stream to connect to.
     * @param pipeSize The size of the pipe's buffer.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalArgumentException If {@code pipeSize <= 0}.
     */
    public LongInputPipe(final LongOutputPipe source, final int pipeSize) throws IOException {
        this(pipeSize);
        connect(source);
    }

    /**
     * LongInputPipe constructor.
     * 
     * Creates a {@code LongInputPipe} so that it is not yet 
     * {@linkplain #connect(taskgraph.pipes.LongOutputPipe) connected}. 
     * It must be {@linkplain taskgraph.pipes.LongOutputPipe#connect(
     * taskgraph.pipes.LongInputPipe) connected} to a
     * {@code LongOutputPipe} before being used.
     *
     * @param pipeSize the size of the pipe's buffer.
     * @throws IllegalArgumentException if {@code pipeSize <= 0}.
     */
    public LongInputPipe(final int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe Size <= 0");
        }
        buffer = new long[pipeSize];
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
     * @param source The long integer output pipe to connect to.
     * @throws IOException If an I/O error occurs.
     */
    public void connect(LongOutputPipe source) throws IOException {
        source.connect(this);
    }

    /**
     * Receives a {@code long} integer of data. This method will block 
     * if no input is available.
     * 
     * @param l The {@code long} value being received.
     * @throws IOException If the pipe is {@code broken},
     *		   {@link #connect(taskgraph.pipes.LongOutputPipe) unconnected},
     *		   closed, or if an I/O error occurs.
     */
    protected synchronized void receive(long l) throws IOException {
        checkStateForReceive();
        writeSide = Thread.currentThread();
        if (in == out) awaitSpace();
        if (in < 0) {
            in = 0;
            out = 0;
        }
        buffer[in++] = l;
        if (in >= buffer.length) {
            in = 0;
        }
    }

    synchronized void receive(long[] l, int off, int len) throws IOException {
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
            System.arraycopy(l, off, buffer, in, nextTransferAmount);
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
     * Reads the next long integer from this input pipe. 
     * This method blocks until input data is available, the output end 
     * of the pipe is closed, or an exception is thrown.
     *
     * @return The next {@code long} from this pipe.
     * @throws IOException If the pipe is {@code broken},
     *		   {@link #connect(taskgraph.pipes.LongOutputPipe) unconnected},
     *		   or if an I/O error occurs.
     * @throws EOFException If the output end of the pipe is closed.
     */
    public synchronized long read() throws IOException {
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
        long ret = buffer[out++];
        if (out >= buffer.length) {
            out = 0;
        }
        if (in == out) {
            in = -1;
        }
        return ret;
    }

    /**
     * Reads into an array of long integers.
     * 
     * Reads some number of long integers from this input pipe and stores them
     * into the {@code long} array {@code l}. The number of longs actually read
     * is returned as an {@code int}. This method blocks until input data is
     * available, end of file is detected, or an exception is thrown.
     *
     * <p>If the length of {@code l} is zero, then no long integers are read 
     * and {@code 0} is returned; otherwise, there is an attempt to read at
     * least one {@code long}. If no long is available because the output end 
     * is closed, an {@code IOException} is thrown; otherwise, at least one 
     * long is read and stored into {@code l}.
     *
     * <p>The first long read is stored into element {@code l[0]}, the
     * next one into {@code l[1]}, and so on. The number of longs read is,
     * at most, equal to the length of {@code l}. Let <i>k</i> be the
     * number of longs actually read; these longs will be stored in elements
     * {@code l[0]} through {@code l[k-1]}, leaving elements {@code l[k]} 
     * through {@code l[l.length-1]} unaffected.
     * 
     * <p>The general contract for {@code read(l)} is that it should have 
     * exactly the same effect as the call {@code read(l, 0, l.length)}.
     *
     * @param l The {@code long} array into which the data is read.
     * @return The total number of longs read into the array.
     * @throws IOException If the first long cannot be read,
     *         or if some other I/O error occurs.
     * @throws EOFException If the output end of the pipe is closed.
     * @throws NullPointerException If {@code l} is {@code null}.
     */
    public int read(long[] l) throws IOException {
        return read(l, 0, l.length);
    }

    /**
     * Reads into an array of long integers.
     * 
     * Reads up to {@code len} long integers from this input pipe into an 
     * array. Less than {@code len} longs will be read if the output end of the
     * data pipe is closed or if {@code len} exceeds the pipe's buffer size. If 
     * {@code len} is zero, then no long integers are read and 0 is returned; 
     * otherwise, the method blocks until at least 1 {@code long} of input is 
     * available, the output end is closed, or an exception is thrown.
     *
     * @param l    The {@code long} array into which the data is read.
     * @param off  The start offset in the destination array {@code l}.
     * @param len  The maximum number of long integers to read.
     * @return     The total number of long integers read into the array.
     * @throws NullPointerException If {@code l} is {@code null}.
     * @throws IndexOutOfBoundsException If {@code off} is negative, 
     *         {@code len} is negative, or {@code len} is greater than 
     *         {@code l.length - off}.
     * @throws IOException If the pipe is {@code broken},
     *		   {@link #connect(taskgraph.pipes.LongOutputPipe) unconnected},
     *		   or if an I/O error occurs.
     * @throws EOFException If the output end of the pipe is closed.
     */
    public synchronized int read(long[] l, int off, int len) throws IOException {
        if (l == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > l.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        long ch = read();
        l[off] = ch;
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
            System.arraycopy(buffer, out, l, off + rlen, available);
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
     * Gets the number of longs in the buffer.
     * 
     * Returns the number of long integers that can be read from this input
     * pipe without blocking.
     * <p>Returns {@code 0} if this input pipe has been closed by invoking its 
     * {@link #close()} method, or if the output end is
     * {@link #connect(taskgraph.pipes.LongOutputPipe) unconnected}, or
     * {@code broken}.
     *
     * @return The number of available long integers, without blocking. 
     * @throws IOException If an I/O error occurs.
     */
    public synchronized int available() throws IOException {
        if (in < 0) return 0; else if (in == out) return buffer.length; else if (in > out) return in - out; else return in + buffer.length - out;
    }

    /**
     * Discards a number of longs from the buffer.
     * 
     * Makes an attempt to skip over <code>n</code> long integers of data from
     * the channel, discarding the skipped ones. However, it may skip over some 
     * smaller number of long integers, possibly zero. This may result from any 
     * of a number of conditions; reaching end of channel before <code>n</code>
     * long integers have been skipped is only one possibility. This method 
     * never throws an <code>EOFException</code>. 
     * The actual number of long integers skipped is returned.
     *
     * @param n The number of longs to be skipped.
     * @return The number of longs actually skipped.
     * @throws IOException If an I/O error occurs.
     */
    public synchronized int skipLongs(int n) throws IOException {
        if (n < 0) throw new IllegalArgumentException("n is negative");
        if (n == 0) return 0;
        int toSkip = Math.min(n, available());
        long[] l = new long[toSkip];
        return read(l);
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
