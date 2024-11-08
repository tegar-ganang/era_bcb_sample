package jaxlib.io.stream.concurrent;

import java.io.DataInput;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.io.stream.XDataOutput;
import jaxlib.io.stream.XOutputStream;

/**
 * A threadsafe view of a <tt>XDataOutput</tt> instance.
 * <p>
 * Note: The {@link #closeInstance() closeInstance()} method calls the same method if the underlying stream!
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: SynchronizedXOutputStream.java 2321 2007-04-16 05:34:05Z joerg_wassmer $
 */
public class SynchronizedXOutputStream extends XOutputStream implements XDataOutput, WritableByteChannel {

    private final XDataOutput delegate;

    protected final Object lock;

    public SynchronizedXOutputStream(XDataOutput delegate) {
        super();
        if (delegate == null) throw new NullPointerException("delegate");
        this.delegate = delegate;
        this.lock = this;
    }

    public SynchronizedXOutputStream(XDataOutput delegate, Object lock) {
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
    public void flush() throws IOException {
        synchronized (this.lock) {
            this.delegate.flush();
        }
    }

    @Override
    public boolean isOpen() {
        synchronized (this.lock) {
            return this.delegate.isOpen();
        }
    }

    @Override
    public long transferFrom(final InputStream in, final long maxCount) throws IOException {
        synchronized (this.lock) {
            return this.delegate.transferFrom(in, maxCount);
        }
    }

    @Override
    public long transferFromByteChannel(final ReadableByteChannel in, final long maxCount) throws IOException {
        synchronized (this.lock) {
            return this.delegate.transferFromByteChannel(in, maxCount);
        }
    }

    @Override
    public void transferBytesFullyFrom(final DataInput in, final long maxCount) throws IOException {
        synchronized (this.lock) {
            this.delegate.transferBytesFullyFrom(in, maxCount);
        }
    }

    @Override
    public int write(final ByteBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.write(source);
        }
    }

    @Override
    public int writeFully(final ByteBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.writeFully(source);
        }
    }

    @Override
    public int write(final CharBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.write(source);
        }
    }

    @Override
    public int write(final DoubleBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.write(source);
        }
    }

    @Override
    public int write(final FloatBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.write(source);
        }
    }

    @Override
    public int write(final IntBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.write(source);
        }
    }

    @Override
    public int write(final LongBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.write(source);
        }
    }

    @Override
    public int write(final ShortBuffer source) throws IOException {
        synchronized (this.lock) {
            return this.delegate.write(source);
        }
    }

    @Override
    public void write(final int b) throws IOException {
        synchronized (this.lock) {
            this.delegate.write(b);
        }
    }

    @Override
    public void write(final byte[] source) throws IOException {
        synchronized (this.lock) {
            this.delegate.write(source);
        }
    }

    @Override
    public void write(final byte[] source, final int off, final int len) throws IOException {
        synchronized (this.lock) {
            this.delegate.write(source, off, len);
        }
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeBoolean(v);
        }
    }

    @Override
    public void writeByte(final int v) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeByte(v);
        }
    }

    @Override
    public void writeShort(final int v) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeShort(v);
        }
    }

    @Override
    public void writeChar(final int v) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeChar(v);
        }
    }

    @Override
    public void writeInt(final int v) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeInt(v);
        }
    }

    @Override
    public void writeLong(final long v) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeLong(v);
        }
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeFloat(v);
        }
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeDouble(v);
        }
    }

    @Override
    public void writeBytes(final String s) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeBytes(s);
        }
    }

    @Override
    public void writeChars(final String s) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeChars(s);
        }
    }

    @Override
    public void writeUTF(final String str) throws IOException {
        synchronized (this.lock) {
            this.delegate.writeUTF(str);
        }
    }
}
