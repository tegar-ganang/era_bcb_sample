package org.dfdaemon.il2.test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;

public class MyPipedReader extends Reader {

    boolean _closedByWriter;

    boolean _closedByReader;

    boolean _connected;

    Thread _readSide;

    Thread _writeSide;

    /**
     * The size of the pipe's circular input buffer.
     */
    private static final int DEFAULT_PIPE_SIZE = 1024;

    /**
     * The circular buffer into which incoming data is placed.
     */
    char _buffer[];

    /**
     * The index of the position in the circular buffer at which the
     * next character of data will be stored when received from the connected
     * piped writer. <code>in&lt;0</code> implies the buffer is empty,
     * <code>in==out</code> implies the buffer is full
     */
    int _in = -1;

    /**
     * The index of the position in the circular buffer at which the next
     * character of data will be read by this piped reader.
     */
    int _out;

    /**
     * Creates a <code>MyPipedReader</code> so
     * that it is connected to the piped writer
     * <code>src</code>. Data written to <code>src</code>
     * will then be available as input from this stream.
     *
     * @param src the stream to connect to.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public MyPipedReader(MyPipedWriter src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }

    /**
     * Creates a <code>MyPipedReader</code> so that it is connected
     * to the piped writer <code>src</code> and uses the specified
     * pipe size for the pipe's buffer. Data written to <code>src</code>
     * will then be  available as input from this stream.
     *
     * @param src      the stream to connect to.
     * @param pipeSize the size of the pipe's buffer.
     * @throws java.io.IOException      if an I/O error occurs.
     * @throws IllegalArgumentException if <code>pipeSize <= 0</code>.
     * @since 1.6
     */
    public MyPipedReader(MyPipedWriter src, int pipeSize) throws IOException {
        initPipe(pipeSize);
        connect(src);
    }

    /**
     * Creates a <code>MyPipedReader</code> so
     * that it is not yet {@linkplain #connect(java.io.PipedWriter)
     * connected}. It must be {@linkplain java.io.PipedWriter#connect(
     *java.io.PipedReader) connected} to a <code>MyPipedWriter</code>
     * before being used.
     */
    public MyPipedReader() {
        initPipe(DEFAULT_PIPE_SIZE);
    }

    /**
     * Creates a <code>MyPipedReader</code> so that it is not yet
     * {@link #connect(java.io.PipedWriter) connected} and uses
     * the specified pipe size for the pipe's buffer.
     * It must be  {@linkplain java.io.PipedWriter#connect(
     *java.io.PipedReader) connected} to a <code>MyPipedWriter</code>
     * before being used.
     *
     * @param pipeSize the size of the pipe's buffer.
     * @throws IllegalArgumentException if <code>pipeSize <= 0</code>.
     * @since 1.6
     */
    public MyPipedReader(int pipeSize) {
        initPipe(pipeSize);
    }

    private void initPipe(int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe size <= 0");
        }
        _buffer = new char[pipeSize];
    }

    /**
     * Causes this piped reader to be connected
     * to the piped  writer <code>src</code>.
     * If this object is already connected to some
     * other piped writer, an <code>IOException</code>
     * is thrown.
     * <p/>
     * If <code>src</code> is an
     * unconnected piped writer and <code>snk</code>
     * is an unconnected piped reader, they
     * may be connected by either the call:
     * <p/>
     * <pre><code>snk.connect(src)</code> </pre>
     * <p/>
     * or the call:
     * <p/>
     * <pre><code>src.connect(snk)</code> </pre>
     * <p/>
     * The two
     * calls have the same effect.
     *
     * @param src The piped writer to connect to.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public void connect(MyPipedWriter src) throws IOException {
        src.connect(this);
    }

    /**
     * Receives a char of data. This method will block if no input is
     * available.
     *
     * @param c amount
     * @throws java.io.IOException on error
     */
    synchronized void receive(int c) throws IOException {
        if (!_connected) {
            throw new IOException("Pipe not connected");
        } else if (_closedByWriter || _closedByReader) {
            throw new IOException("Pipe closed");
        } else if (_readSide != null && !_readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
        _writeSide = Thread.currentThread();
        while (_in == _out) {
            if ((_readSide != null) && !_readSide.isAlive()) {
                throw new IOException("Pipe broken");
            }
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        if (_in < 0) {
            _in = 0;
            _out = 0;
        }
        _buffer[_in++] = (char) c;
        if (_in >= _buffer.length) {
            _in = 0;
        }
    }

    /**
     * Receives data into an array of characters.  This method will
     * block until some input is available.
     *
     * @param c   buffer
     * @param off .
     * @param len .
     * @throws java.io.IOException .
     */
    synchronized void receive(char c[], int off, int len) throws IOException {
        while (--len >= 0) {
            receive(c[off++]);
        }
    }

    /**
     * Notifies all waiting threads that the last character of data has been
     * received.
     */
    synchronized void receivedLast() {
        _closedByWriter = true;
        notifyAll();
    }

    /**
     * Reads the next character of data from this piped stream.
     * If no character is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned.
     * This method blocks until input data is available, the end of
     * the stream is detected, or an exception is thrown.
     *
     * @return the next character of data, or <code>-1</code> if the end of the
     *         stream is reached.
     * @throws java.io.IOException if the pipe is
     *                             <a href=PipedInputStream.html#BROKEN> <code>broken</code></a>,
     *                             {@link #connect(java.io.PipedWriter) unconnected}, closed,
     *                             or an I/O error occurs.
     */
    public synchronized int read() throws IOException {
        if (!_connected) {
            throw new IOException("Pipe not connected");
        } else if (_closedByReader) {
            throw new IOException("Pipe closed");
        } else if (_writeSide != null && !_writeSide.isAlive() && !_closedByWriter && (_in < 0)) {
            throw new IOException("Write end dead");
        }
        _readSide = Thread.currentThread();
        int trials = 2;
        while (_in < 0) {
            if (_closedByWriter) {
                return -1;
            }
            if ((_writeSide != null) && (!_writeSide.isAlive()) && (--trials < 0)) {
                throw new IOException("Pipe broken");
            }
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        int ret = _buffer[_out++];
        if (_out >= _buffer.length) {
            _out = 0;
        }
        if (_in == _out) {
            _in = -1;
        }
        return ret;
    }

    /**
     * Reads up to <code>len</code> characters of data from this piped
     * stream into an array of characters. Less than <code>len</code> characters
     * will be read if the end of the data stream is reached or if
     * <code>len</code> exceeds the pipe's buffer size. This method
     * blocks until at least one character of input is available.
     *
     * @param cbuf the buffer into which the data is read.
     * @param off  the start offset of the data.
     * @param len  the maximum number of characters read.
     * @return the total number of characters read into the buffer, or
     *         <code>-1</code> if there is no more data because the end of
     *         the stream has been reached.
     * @throws java.io.IOException if the pipe is
     *                             <a href=PipedInputStream.html#BROKEN> <code>broken</code></a>,
     *                             {@link #connect(java.io.PipedWriter) unconnected}, closed,
     *                             or an I/O error occurs.
     */
    public synchronized int read(char cbuf[], int off, int len) throws IOException {
        if (!_connected) {
            throw new IOException("Pipe not connected");
        } else if (_closedByReader) {
            throw new IOException("Pipe closed");
        } else if (_writeSide != null && !_writeSide.isAlive() && !_closedByWriter && (_in < 0)) {
            throw new IOException("Write end dead");
        }
        if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        int c = read();
        if (c < 0) {
            return -1;
        }
        cbuf[off] = (char) c;
        int rlen = 1;
        while ((_in >= 0) && (--len > 0)) {
            cbuf[off + rlen] = _buffer[_out++];
            rlen++;
            if (_out >= _buffer.length) {
                _out = 0;
            }
            if (_in == _out) {
                _in = -1;
            }
        }
        return rlen;
    }

    /**
     * Tell whether this stream is ready to be read.  A piped character
     * stream is ready if the circular buffer is not empty.
     *
     * @throws java.io.IOException if the pipe is
     *                             <a href=PipedInputStream.html#BROKEN> <code>broken</code></a>,
     *                             {@link #connect(java.io.PipedWriter) unconnected}, or closed.
     */
    public synchronized boolean ready() throws IOException {
        if (!_connected) {
            throw new IOException("Pipe not connected");
        } else if (_closedByReader) {
            throw new IOException("Pipe closed");
        } else if (_writeSide != null && !_writeSide.isAlive() && !_closedByWriter && (_in < 0)) {
            throw new IOException("Write end dead");
        }
        return _in >= 0;
    }

    /**
     * Closes this piped stream and releases any system resources
     * associated with the stream.
     *
     * @throws java.io.IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        _in = -1;
        _closedByReader = true;
    }
}
