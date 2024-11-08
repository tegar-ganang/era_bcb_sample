package jaxlib.io.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import jaxlib.buffer.CharBuffers;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.lang.ExternalAssertionException;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckBounds;

/**
 * A character output stream which writes to a buffer and optionally grows the buffer if required.
 * <p>
 * By default the buffer stays in memory when the stream gets closed. This behaviour can be controlled
 * via {@link #setDisposeBufferOnClose(boolean)}.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: CharBufferWriter.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class CharBufferWriter extends XWriter {

    private CharBuffer buffer;

    private float growFactor;

    private boolean disposeBufferOnClose = false;

    private boolean open = true;

    /**
   * Creates a <tt>CharBufferWriter</tt> with initial capacity <tt>16</tt> and
   * growfactor <tt>0.5</tt>.
   */
    public CharBufferWriter() {
        this(CharBuffer.allocate(16), 0.5f);
    }

    /**
   * Creates a <tt>CharBufferWriter</tt> with specified initial capacity and growfactor <tt>0.5</tt>.
   *
   * @throws IllegalArgumentException if <tt>initialCapacity < 0</tt>.
   *
   * @since JaXLib 1.0
   */
    public CharBufferWriter(int initialCapacity) {
        this(CharBuffer.allocate(initialCapacity), 0.5f);
    }

    /**
   * Creates a <tt>CharBufferWriter</tt> with specified initial capacity and growfactor.
   *
   * @throws IllegalArgumentException if <tt>(initialCapacity < 0) || (growFactor < 0)</tt>.
   *
   * @since JaXLib 1.0
   */
    public CharBufferWriter(int initialCapacity, float growFactor) {
        this(CharBuffer.allocate(initialCapacity), growFactor);
    }

    /**
   * Creates a non-growable <tt>CharBufferWriter</tt>.
   *
   * @throws IllegalArgumentException if the specified buffer is {@link CharBuffer#isReadOnly() read-only}.
   *
   * @since JaXLib 1.0
   */
    public CharBufferWriter(CharBuffer buffer) {
        this(buffer, Float.NaN);
    }

    /**
   * Creates a <tt>CharBufferWriter</tt>.
   * The underlying buffer is growable if the specified <tt>growFactor</tt> is not <tt>NaN</tt>.
   *
   * @throws IllegalArgumentException if <tt>growFactor < 0</tt>.
   * @throws IllegalArgumentException if the specified buffer is {@link CharBuffer#isReadOnly() read-only}.
   *
   * @since JaXLib 1.0
   */
    public CharBufferWriter(CharBuffer buffer, float growFactor) {
        super();
        if (!Float.isNaN(growFactor)) CheckArg.growFactor(growFactor);
        CheckArg.writable(buffer);
        this.buffer = buffer;
        this.growFactor = growFactor;
    }

    private CharBuffer allocateBuffer(int capacity) throws IOException {
        ensureOpen();
        CharBuffer buffer = this.buffer;
        if (buffer.isDirect()) {
            int byteCapacity = capacity << 1;
            if (byteCapacity != (long) capacity * 2L) throw new IllegalStateException("Reached maximum capacity Integer.MAX_VALUE bytes");
            return ByteBuffer.allocateDirect(byteCapacity).asCharBuffer();
        } else {
            return CharBuffer.allocate(capacity);
        }
    }

    private void ensureOpen() throws IOException {
        if (!this.open) throw new IOException("closed");
    }

    private CharBuffer prepareBuffer(int minFreeCapacity) throws IOException {
        ensureOpen();
        CharBuffer buffer = this.buffer;
        if (buffer.remaining() < minFreeCapacity) {
            if (Float.isNaN(this.growFactor)) throw new IOException("buffer is full and not growable");
            CharBuffer newBuffer = allocateBuffer((int) (buffer.position() + (buffer.position() * this.growFactor) + minFreeCapacity));
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
            setBuffer(newBuffer);
        }
        return buffer;
    }

    /**
   * Returns the identical buffer instance used by this stream.
   * Thus all changes to the buffer are visible to this stream.
   * <p>
   * The content starts at zero and ends at the buffer's current position.
   * </p>
   *
   * @see CharBuffer#position()
   *
   * @since JaXLib 1.00
   */
    public CharBuffer getBuffer() {
        return this.buffer;
    }

    /**
   * Returns the grow factor of the underlying buffer as percent.
   *
   * @see #setGrowFactor(float)
   *
   * @since JaXLib 1.0
   */
    public final float getGrowFactor() {
        return this.growFactor;
    }

    /**
   * Returns whether this stream releases the reference to its underlying char buffer if it gets closed.
   *
   * @see #setDisposeBufferOnClose(boolean)
   *
   * @since JaXLib 1.0
   */
    public final boolean isDisposeBufferOnClose() {
        return this.disposeBufferOnClose;
    }

    /**
   * Sets buffer instance used by this stream.
   * The specified buffer instance is used as it is, this method neither copies nor duplicates it.
   *
   * @see #getBuffer()
   *
   * @since JaXLib 1.00
   */
    public void setBuffer(CharBuffer buffer) {
        if (buffer != this.buffer) {
            if (buffer == null) throw new NullPointerException("buffer");
            CheckArg.writable(buffer);
            this.buffer = buffer;
        }
    }

    /**
   * If set to {@code true} then this stream will release the reference to its underlying char buffer when
   * the stream gets closed.
   * Default is {@code false}.
   *
   * @see #close()
   *
   * @since JaXLib 1.0
   */
    public void setDisposeBufferOnClose(boolean b) {
        this.disposeBufferOnClose = b;
        if (b && !this.open) this.buffer = null;
    }

    /**
   * Sets the grow factor for the underlying buffer as percent.
   * If the specified value is {@code NaN} then this stream will throw an exception if a caller tries to
   * write a character and the buffer is full.
   *
   * @throws IllegalArgumentException if {@code growFactor < 0}.
   *
   * @since JaXLib 1.0
   */
    public void setGrowFactor(float growFactor) {
        if (!Float.isNaN(growFactor)) CheckArg.growFactor(growFactor);
        this.growFactor = growFactor;
    }

    public void setOpen(boolean b) throws IOException {
        if (this.open != b) {
            this.open = b;
            if (b) {
                if (this.buffer == null) {
                    if (Float.isNaN(this.growFactor)) throw new IOException("can not reopen stream because buffer is null and growFactor is NaN");
                    this.buffer = CharBuffer.allocate(16);
                }
            } else {
                closeInstance();
            }
        }
    }

    /**
   * Returns the currently buffered chars as a new array.
   *
   * @throws IOException if this stream has been closed.
   *
   * @since JaXLib 1.0
   */
    public final char[] toCharArray() throws IOException {
        ensureOpen();
        CharBuffer buffer = this.buffer;
        char[] a = new char[buffer.remaining()];
        int pos = buffer.position();
        int lim = buffer.limit();
        if (pos != lim) {
            try {
                buffer.flip();
                buffer.get(a);
            } finally {
                buffer.limit(lim);
                buffer.position(pos);
            }
        }
        return a;
    }

    /**
   * Returns the currently buffered chars as a new string.
   * If this stream is closed then this method returns an empty string.
   *
   * @since JaXLib 1.0
   */
    @Override
    public String toString() {
        CharBuffer buffer = this.buffer;
        if (buffer == null) return "";
        int pos = buffer.position();
        int lim = buffer.limit();
        if (pos == lim) {
            return "";
        } else {
            try {
                buffer.flip();
                return buffer.toString();
            } finally {
                buffer.limit(lim);
                buffer.position(pos);
            }
        }
    }

    @Override
    public void close() throws IOException {
        closeInstance();
    }

    @Override
    public void closeInstance() throws IOException {
        try {
            flush();
        } finally {
            if (this.disposeBufferOnClose) this.buffer = null;
            this.open = false;
        }
    }

    @Override
    public final boolean isOpen() {
        return this.open;
    }

    @Override
    public final CharBufferWriter append(char c) throws IOException {
        prepareBuffer(1).put(c);
        return this;
    }

    @Override
    public CharBufferWriter print(final CharSequence v, final int offs, final int len) throws IOException {
        CheckBounds.offset(v.length(), offs, len);
        CharBuffers.append(prepareBuffer(len), v, offs, offs + len);
        return this;
    }

    @Override
    public final void write(final int c) throws IOException {
        prepareBuffer(1).put((char) c);
    }

    @Override
    public final void write(final char[] buf, final int offs, final int len) throws IOException {
        CheckBounds.offset(buf, offs, len);
        prepareBuffer(len).put(buf, offs, len);
    }

    @Override
    public final int write(final CharBuffer source) throws IOException {
        final int remaining = source.remaining();
        prepareBuffer(remaining).put(source);
        return remaining;
    }

    @Override
    public final void write(final String source) throws IOException {
        prepareBuffer(source.length()).put(source);
    }

    @Override
    public final void write(final String source, final int off, final int len) throws IOException {
        CheckBounds.offset(source.length(), off, len);
        prepareBuffer(len).put(source, off, off + len);
    }

    @Override
    public long transferFrom(Readable in, long maxCount) throws IOException {
        ensureOpen();
        CharBuffer buf = this.buffer;
        CheckArg.maxCount(maxCount);
        long count = 0;
        while (true) {
            if (count == maxCount) break;
            int maxStep = buf.remaining();
            if (maxStep == 0) {
                buf = prepareBuffer((maxCount) < 0 ? 1 : (int) Math.min(buf.capacity() + 1, maxCount - count));
                maxStep = (maxCount < 0) ? buf.remaining() : (int) Math.min(buf.remaining(), maxCount - count);
            }
            int pos = buf.position();
            int lim = buf.limit();
            buf.limit(buf.position() + maxStep);
            int step;
            try {
                step = in.read(buf);
            } finally {
                buf.limit(lim);
            }
            if (step < 0) break; else if (step > maxStep) throw new ReturnValueException(in, "read(CharBuffer)", step, "<=", maxStep); else if (buf.position() != pos + step) throw new ExternalAssertionException(in, "read(CharBuffer)");
            count += step;
        }
        return count;
    }
}
