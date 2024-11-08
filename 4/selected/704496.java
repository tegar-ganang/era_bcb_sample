package jaxlib.io.stream.concurrent;

import java.io.IOException;
import java.nio.CharBuffer;
import jaxlib.io.stream.XWriter;

/**
 * A threadsafe view of a <tt>XWriter</tt> instance.
 * <p>
 * Note: The {@link #closeInstance() closeInstance()} method calls the same method if the underlying stream!
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: SynchronizedXWriter.java 2267 2007-03-16 08:33:33Z joerg_wassmer $
 */
public class SynchronizedXWriter extends XWriter {

    private final XWriter delegate;

    public SynchronizedXWriter(XWriter delegate) {
        super();
        if (delegate == null) throw new NullPointerException("delegate");
        this.delegate = delegate;
        this.lock = this;
    }

    public SynchronizedXWriter(XWriter delegate, Object lock) {
        super();
        if (delegate == null) throw new NullPointerException("delegate");
        this.delegate = delegate;
        this.lock = (lock == null) ? this : lock;
    }

    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            this.delegate.close();
        }
    }

    @Override
    public void closeInstance() throws IOException {
        synchronized (this.lock) {
            this.delegate.closeInstance();
        }
    }

    @Override
    public boolean isOpen() {
        synchronized (this.lock) {
            return this.delegate.isOpen();
        }
    }

    @Override
    public void write(int c) throws IOException {
        synchronized (this.lock) {
            this.delegate.write(c);
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (this.lock) {
            this.delegate.flush();
        }
    }

    @Override
    public void write(char[] source) throws IOException {
        synchronized (this.lock) {
            this.delegate.write(source);
        }
    }

    @Override
    public void write(char[] source, int off, int len) throws IOException {
        synchronized (this.lock) {
            this.delegate.write(source, off, len);
        }
    }

    @Override
    public int write(CharBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.write(source);
        }
    }

    @Override
    public void write(String source) throws IOException {
        synchronized (this.lock) {
            this.delegate.write(source);
        }
    }

    @Override
    public void write(String source, int off, int len) throws IOException {
        synchronized (this.lock) {
            this.delegate.write(source, off, len);
        }
    }

    @Override
    public int writeCodePoint(int c) throws IOException {
        synchronized (this.lock) {
            return this.delegate.writeCodePoint(c);
        }
    }

    @Override
    public SynchronizedXWriter print(char[] v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(char[] v, int off, int len) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v, off, len);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(Object v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(CharSequence v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(CharSequence v, int off, int len) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v, off, len);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(boolean v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(char v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(double v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(float v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(int v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(long v) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(int v, int radix) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v, radix);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter print(long v, int radix) throws IOException {
        synchronized (this.lock) {
            this.delegate.print(v, radix);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println() throws IOException {
        synchronized (this.lock) {
            this.delegate.println();
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(char[] v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(char[] v, int off, int len) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v, off, len);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(Object v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(CharSequence v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(final CharSequence v, final int offs, final int len) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v, offs, len);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(boolean v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(char v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(double v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(float v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(int v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(long v) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(int v, int radix) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v, radix);
        }
        return this;
    }

    @Override
    public SynchronizedXWriter println(long v, int radix) throws IOException {
        synchronized (this.lock) {
            this.delegate.println(v, radix);
        }
        return this;
    }

    @Override
    public String toString() {
        synchronized (this.lock) {
            return this.delegate.toString();
        }
    }

    @Override
    public long transferFrom(Readable in, long maxCount) throws IOException {
        synchronized (this.lock) {
            return this.delegate.transferFrom(in, maxCount);
        }
    }
}
