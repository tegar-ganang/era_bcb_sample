package org.owasp.jxt.runtime;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.jsp.JspWriter;

/**
 * JxtJspWriter
 *
 * @author Jeffrey Ichnowski
 * @version $Revision: 8 $
 */
public class JxtJspWriter extends JspWriter {

    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    private Writer _out;

    private char[] _buffer;

    private int _offset;

    private boolean _closed;

    private boolean _flushed;

    private String _lineSeparator;

    public JxtJspWriter(Writer out, int bufferSize, boolean autoFlush) {
        super(bufferSize, autoFlush);
        _out = out;
        _offset = 0;
        switch(bufferSize) {
            case UNBOUNDED_BUFFER:
            case DEFAULT_BUFFER:
                _buffer = new char[DEFAULT_BUFFER_SIZE];
                break;
            case NO_BUFFER:
                _buffer = new char[0];
                break;
            default:
                _buffer = new char[bufferSize];
                break;
        }
        try {
            _lineSeparator = System.getProperty("line.separator");
        } catch (SecurityException ignore) {
            _lineSeparator = "\n";
        }
    }

    /** {@inheritDoc} */
    public final void write(final char[] data, int off, int len) throws IOException {
        if (_closed) {
            throw new IOException("writer already closed");
        }
        while (len > 0) {
            int n = _buffer.length - _offset;
            if (n > 0) {
                if (len < n) {
                    n = len;
                }
                System.arraycopy(data, off, _buffer, _offset, n);
                _offset += n;
                if (n == len) {
                    break;
                }
                off += n;
                len -= n;
            }
            if (autoFlush) {
                flush();
            } else if (bufferSize == UNBOUNDED_BUFFER) {
                int targetLength = _offset + len;
                int newLength = _buffer.length * 2;
                while (newLength < targetLength) {
                    newLength *= 2;
                }
                char[] tmp = new char[newLength];
                System.arraycopy(_buffer, 0, tmp, 0, _offset);
                _buffer = tmp;
            } else {
                throw new IOException("buffer overflowed");
            }
        }
    }

    /** {@inheritDoc} */
    public final void println(final String str) throws IOException {
        print(str);
        println();
    }

    /** {@inheritDoc} */
    public final void println(final char[] data) throws IOException {
        print(data);
        println();
    }

    /** {@inheritDoc} */
    public final void println(final Object obj) throws IOException {
        print(obj);
        println();
    }

    /** {@inheritDoc} */
    public final void println(final long value) throws IOException {
        print(value);
        println();
    }

    /** {@inheritDoc} */
    public final void println(final double value) throws IOException {
        print(value);
        println();
    }

    /** {@inheritDoc} */
    public final void println(final float value) throws IOException {
        print(value);
        println();
    }

    /** {@inheritDoc} */
    public final void println(final int value) throws IOException {
        print(value);
        println();
    }

    /** {@inheritDoc} */
    public final void println(final char value) throws IOException {
        print(value);
        println();
    }

    /** {@inheritDoc} */
    public final void println(final boolean value) throws IOException {
        print(value);
        println();
    }

    /** {@inheritDoc} */
    public final void println() throws IOException {
        newLine();
    }

    /** {@inheritDoc} */
    public final void clear() throws IOException {
        if (_flushed) {
            throw new IOException("buffer has already been flushed");
        }
        _offset = 0;
    }

    /** {@inheritDoc} */
    public final void print(final String str) throws IOException {
        write(str == null ? "null" : str);
    }

    /** {@inheritDoc} */
    public final void print(final Object obj) throws IOException {
        write(String.valueOf(obj));
    }

    /** {@inheritDoc} */
    public final void print(final char[] value) throws IOException {
        write(value);
    }

    /** {@inheritDoc} */
    public final void print(final double value) throws IOException {
        write(String.valueOf(value));
    }

    /** {@inheritDoc} */
    public final void print(final float value) throws IOException {
        write(String.valueOf(value));
    }

    /** {@inheritDoc} */
    public final void print(final long value) throws IOException {
        write(String.valueOf(value));
    }

    /** {@inheritDoc} */
    public final void print(final int value) throws IOException {
        write(String.valueOf(value));
    }

    /** {@inheritDoc} */
    public final void print(final char value) throws IOException {
        write(String.valueOf(value));
    }

    /** {@inheritDoc} */
    public final void print(final boolean value) throws IOException {
        write(String.valueOf(value));
    }

    /** {@inheritDoc} */
    public final void newLine() throws IOException {
        write(_lineSeparator);
    }

    /** {@inheritDoc} */
    public final void close() throws IOException {
        if (!_closed) {
            flush();
            _closed = true;
            _out.close();
        }
    }

    /** {@inheritDoc} */
    public final void flush() throws IOException {
        if (_closed) {
            throw new IOException("writer already closed");
        }
        _out.write(_buffer, 0, _offset);
        _offset = 0;
        _flushed = true;
    }

    /** {@inheritDoc} */
    public final int getRemaining() {
        return _buffer.length - _offset;
    }

    /** {@inheritDoc} */
    public final void clearBuffer() throws IOException {
        _offset = 0;
    }
}
