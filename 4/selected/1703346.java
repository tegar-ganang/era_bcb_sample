package jaxlib.io.stream;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.util.UnsafeStringConstructor;
import jaxlib.lang.Chars;
import jaxlib.lang.ExternalAssertionException;
import jaxlib.util.CheckBounds;

/**
 * A buffered byte input stream.
 * <p>
 * This class is <b>not</b> threadsafe.
 * If you need a threadsafe stream you may use {@link jaxlib.io.stream.concurrent.SynchronizedXInputStream}.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: BufferedXInputStream.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class BufferedXInputStream extends XInputStream implements XDataInput {

    private static final int NOT_MARKED = -1;

    private static final int MARK_INVALID = -2;

    private static final UnsafeStringConstructor unsafeStringConstructor = UnsafeStringConstructor.getInstance(true);

    private InputStream in;

    private XInputStream xin;

    private ReadableByteChannel inChannel;

    private ByteBuffer buffer;

    private int mark = NOT_MARKED;

    private int markLimit;

    private ByteBuffer bufferDupe;

    private ByteBuffer loadedBuffer;

    private int loadPos;

    private int loadLim;

    private int unreadSpace;

    /**
   * Creates a new <tt>BufferedXInputStream</tt> with a buffer size of <tt>8192</tt>.
   *
   * @throws NullPointerException     if <code>in == null</code>.
   *
   * @since JaXLib 1.0
   */
    public BufferedXInputStream(final InputStream in) {
        this(in, 8191, 1);
    }

    /**
   * Creates a new <tt>BufferedXInputStream</tt> with the specified buffer size.
   *
   * @throws IllegalArgumentException if <code>bufferSize <= 0</code>.
   * @throws NullPointerException     if <code>in == null</code>.
   *
   * @since JaXLib 1.0
   */
    public BufferedXInputStream(final InputStream in, final int bufferSize) {
        this(in, bufferSize, 1);
    }

    /**
   * Creates a new <tt>BufferedXInputStream</tt> with the specified buffer size and the specified additional
   * space for unreading.
   * The <tt>unreadSpace</tt> is just a hint used for performance reasons. You may unread more bytes.
   *
   * @throws IllegalArgumentException if <code>(bufferSize <= 0) || (unreadSpace < 0)</code>.
   * @throws NullPointerException     if <code>in == null</code>.
   *
   * @see #unread(int)
   *
   * @since JaXLib 1.0
   */
    public BufferedXInputStream(final InputStream in, final int bufferSize, final int unreadSpace) {
        super();
        CheckArg.notNull(in, "in");
        CheckArg.positive(bufferSize, "bufferSize");
        CheckArg.notNegative(unreadSpace, "unreadSpace");
        this.unreadSpace = unreadSpace;
        this.buffer = ByteBuffer.allocate(bufferSize + unreadSpace);
        this.buffer.position(this.buffer.limit());
        initIn(in);
    }

    /**
   * Creates a new <tt>BufferedXInputStream</tt> using the specified initial buffer.
   * Any initial bytes in the buffer are ignored. You may include them by adjusting the buffer's position
   * after this call.
   *
   * @throws NullPointerException
   *  if {@code in == null}.
   * @throws IllegalArgumentException
   *  if {@code !buffer.hasArray()}.
   *
   * @since JaXLib 1.0
   */
    public BufferedXInputStream(final InputStream in, final ByteBuffer initialBuffer) {
        this(in, initialBuffer, false);
    }

    public BufferedXInputStream(final InputStream in, final ByteBuffer initialBuffer, final boolean keepPosition) {
        super();
        CheckArg.notNull(in, "in");
        CheckArg.notNull(initialBuffer, "initialBuffer");
        CheckArg.isTrue(initialBuffer.hasArray(), "buffer.hasArray()");
        this.unreadSpace = 1;
        this.buffer = initialBuffer;
        if (!keepPosition) initialBuffer.position(initialBuffer.limit());
        initIn(in);
    }

    private ByteBuffer ensureOpen() throws IOException {
        final ByteBuffer b = this.buffer;
        if (b == null) throw new IOException("closed");
        this.loadedBuffer = null;
        return b;
    }

    private ByteBuffer fill() throws IOException {
        ByteBuffer buffer = ensureOpen();
        if (buffer.hasRemaining()) return buffer;
        final int pos = buffer.position();
        final int cap = buffer.capacity();
        final int markLimit = this.markLimit;
        final int unreadSpace = this.unreadSpace;
        int mark = this.mark;
        if ((mark >= 0) && (pos > this.markLimit)) this.mark = mark = MARK_INVALID;
        if (mark < 0) {
            buffer.limit(cap);
            buffer.position(unreadSpace);
            final int filled = Math.max(0, fillBuffer(buffer));
            buffer.limit(unreadSpace + filled);
            buffer.position(unreadSpace);
        } else if ((markLimit - mark) < cap) {
            final int keep = pos - mark;
            System.arraycopy(buffer.array(), mark, buffer.array(), 0, keep);
            buffer.limit(cap);
            buffer.position(keep);
            final int filled = Math.max(0, fillBuffer(buffer));
            buffer.limit(keep + filled);
            buffer.position(keep);
            this.mark = 0;
            this.markLimit = markLimit - mark;
        } else {
            final int newCapacity = cap + (cap >> 1) + 1;
            final ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
            newBuffer.position(unreadSpace);
            buffer.limit(pos);
            buffer.position(mark);
            newBuffer.put(buffer);
            this.buffer = newBuffer;
            this.bufferDupe = null;
            bufferReleased(buffer);
            buffer = newBuffer;
            final int filled = Math.max(0, fillBuffer(buffer));
            buffer.limit(unreadSpace + (pos - mark) + filled);
            buffer.position(buffer.limit() - filled);
            this.mark = unreadSpace;
            this.markLimit = unreadSpace + (markLimit - mark);
        }
        return buffer;
    }

    protected int fillBuffer(final ByteBuffer buffer) throws IOException {
        final ReadableByteChannel inChannel = this.inChannel;
        int filled = (inChannel == null) ? 0 : inChannel.read(buffer);
        if (filled == 0) {
            final InputStream in = this.in;
            if (in == null) throw new IOException("closed");
            filled = in.read(buffer.array(), buffer.position(), buffer.remaining());
            if (filled > 0) buffer.position(buffer.position() + filled);
        }
        return filled;
    }

    private void initIn(final InputStream in) {
        this.in = in;
        if (in instanceof XInputStream) {
            this.xin = (XInputStream) in;
            this.inChannel = this.xin;
        } else if (in instanceof ReadableByteChannel) this.inChannel = (ReadableByteChannel) in; else if (in instanceof FileInputStream) this.inChannel = ((FileInputStream) in).getChannel();
    }

    private int prepareUnread(final int unreadCount) throws IOException {
        final ByteBuffer buffer = ensureOpen();
        if (unreadCount == 0) return buffer.position();
        int pos = buffer.position();
        int mark = this.mark;
        if ((mark >= 0) && (pos - unreadCount < mark)) {
            this.mark = mark = MARK_INVALID;
        }
        if (pos >= unreadCount) {
            pos -= unreadCount;
            buffer.position(pos);
            return pos;
        } else {
            assert (mark < 0);
            final int limit = buffer.limit();
            final int capacity = buffer.capacity();
            final int freeAtEnd = capacity - limit;
            if (freeAtEnd >= unreadCount) {
                System.arraycopy(buffer.array(), pos, buffer.array(), pos + freeAtEnd, limit - pos);
                final int newPos = pos + freeAtEnd - unreadCount;
                buffer.limit(capacity);
                buffer.position(newPos);
                return newPos;
            } else {
                this.unreadSpace = Math.max(this.unreadSpace, unreadCount);
                final int newCapacity = capacity + this.unreadSpace;
                final ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
                int newPos = newCapacity - (limit - pos);
                newBuffer.position(newPos);
                newBuffer.put(buffer);
                this.buffer = newBuffer;
                this.bufferDupe = null;
                newPos -= unreadCount;
                newBuffer.position(newPos);
                bufferReleased(buffer);
                return newPos;
            }
        }
    }

    /**
   * Called by this stream instance for each buffer instance once it isn't used by this stream anymore.
   * The current position, limit and mark of the specified buffer are undefined.
   * The default implementation of this method does nothing.
   *
   * @since JaXLib 1.0
   */
    protected void bufferReleased(final ByteBuffer buffer) throws IOException {
    }

    protected InputStream getIn() {
        return this.in;
    }

    @Override
    public int available() throws IOException {
        final int a = ensureOpen().remaining();
        if (a > 0) return a;
        final InputStream in = this.in;
        if (in == null) throw new AsynchronousCloseException();
        return in.available();
    }

    /**
   * Returns the current capacity of the buffer.
   *
   * @since JaXLib 1.0
   */
    public final int bufferCapacity() throws IOException {
        final ByteBuffer b = this.buffer;
        if (b == null) throw new IOException("closed");
        return b.capacity();
    }

    /**
   * Returns the current number of bytes buffered.
   *
   * @since JaXLib 1.0
   */
    public final int bufferSize() throws IOException {
        final ByteBuffer b = this.buffer;
        if (b == null) throw new IOException("closed");
        return b.remaining();
    }

    /**
   * Clears the buffer used internally and returns a view of the internal buffer containing all the
   * remaining bytes before this call.
   * <p>
   * The returned buffer can be used the same way as described by the {@link #loadBuffer()} method.
   * </p>
   *
   * @throws IOException              if an I/O error occurs.
   *
   * @since JaXLib 1.0
   */
    public final ByteBuffer clearBuffer() throws IOException {
        final ByteBuffer buffer = ensureOpen();
        return getLoadBuffer(buffer.position(), buffer.limit());
    }

    public final void clearMark() throws IOException {
        this.mark = NOT_MARKED;
    }

    @Override
    public void close() throws IOException {
        final InputStream in = this.in;
        if (in != null) {
            try {
                closeInstance();
            } finally {
                in.close();
            }
        }
    }

    @Override
    public void closeInstance() throws IOException {
        final ByteBuffer buffer = this.buffer;
        this.buffer = null;
        this.bufferDupe = null;
        this.loadedBuffer = null;
        this.in = null;
        this.inChannel = null;
        this.xin = null;
        if (buffer != null) bufferReleased(buffer);
    }

    /**
   * Returns the number of characters the stream would jump back if it would be reset now.
   *
   * @see #mark(int)
   * @see #reset()
   *
   * @since JaXLib 1.0
   */
    public final int countBytesMarked() throws IOException {
        final int pos = ensureOpen().position();
        if ((this.mark >= 0) && (pos > this.markLimit)) this.mark = mark = MARK_INVALID;
        if (this.mark >= 0) return pos - this.mark;
        return 0;
    }

    /**
   * Returns true if the end of this stream has been reached.
   *
   * @throws IOException if an I/O error occurs, including the case this stream has been closed.
   *
   * @since JaXLib 1.0
   */
    public final boolean eof() throws IOException {
        ByteBuffer buffer = ensureOpen();
        if (!buffer.hasRemaining()) {
            buffer = null;
            buffer = fill();
        }
        return !buffer.hasRemaining();
    }

    public final ByteBuffer getFilledBuffer() throws IOException {
        ByteBuffer buffer = ensureOpen();
        if (buffer.hasRemaining()) return buffer;
        buffer = null;
        return fill();
    }

    @Override
    public final boolean isOpen() {
        return this.in != null;
    }

    @Override
    public final void mark(final int readLimit) {
        CheckArg.notNegative(readLimit, "readLimit");
        if (this.buffer == null) return;
        final int mark = this.buffer.position();
        final int markLimit = mark + readLimit;
        if (markLimit < 0) throw new IllegalArgumentException("readLimit(" + readLimit + ") too big.");
        if (readLimit == 0) this.mark = MARK_INVALID; else {
            this.mark = mark;
            this.markLimit = markLimit;
        }
    }

    @Override
    public final boolean markSupported() {
        return true;
    }

    @Override
    public final void reset() throws IOException {
        final ByteBuffer buffer = ensureOpen();
        final int mark = this.mark;
        if (mark == NOT_MARKED) throw new IOException("not marked.");
        if ((mark == MARK_INVALID) || (this.markLimit < buffer.position())) throw new IOException("mark invalid");
        buffer.position(mark);
    }

    /**
   * Fills the internal buffer with an unspecified number of bytes and returns a <tt>ByteBuffer</tt>
   * containing all buffered bytes.
   * </p><p>
   * The returned buffer has at least one remaining byte, except if the end of the stream has been reached.
   * </p><p>
   * The returned buffer is backed by the same byte array used by this stream for buffering. No additional
   * memory is allocated beside the buffer instance.
   * </p><p>
   * Neither the returned buffer nor its {@link ByteBuffer#array() array} should be used as argument for any
   * method of a <tt>BufferedXInputStream</tt> which created it, except the
   * {@link #recycleBuffer(ByteBuffer)} and {@link #unread(ByteBuffer)} methods.
   * </p><p>
   * This method moves the stream position forward for the returned count of bytes. To push back bytes back
   * to the stream, set the buffer's position to a value smaller than its limit, and call the <tt>unread</tt>
   * method with the identical buffer instance as argument.
   * </p><p>
   * The unread operation will cause an exception if the buffer's position is smaller than its initial
   * position, or when a read operation occured since the buffer was loaded.
   * </p><p>
   * Changes to the byte values in the buffer are visible to the stream after the buffer has been unreaded.
   * </p><p>
   * Two calls to this method may return the identical buffer instance, if the foremost loaded buffer was
   * recycled before the second call.
   * </p>
   *
   * @return the loaded bytes.
   *
   * @throws IOException
   *   if an I/O error occurs.
   *
   * @see #recycleBuffer(ByteBuffer)
   * @see #unread(ByteBuffer)
   *
   * @since JaXLib 1.0
   */
    public final ByteBuffer loadBuffer() throws IOException {
        final ByteBuffer buffer = fill();
        final int lim = buffer.limit();
        final ByteBuffer lb = getLoadBuffer(buffer.position(), lim);
        buffer.position(lim);
        return lb;
    }

    private ByteBuffer getLoadBuffer(final int pos, final int lim) throws IOException {
        this.loadPos = pos;
        this.loadLim = lim;
        ByteBuffer bufferDupe = this.bufferDupe;
        if (bufferDupe == null) bufferDupe = this.buffer.duplicate(); else this.bufferDupe = null;
        bufferDupe.limit(lim);
        bufferDupe.position(pos);
        this.loadedBuffer = bufferDupe;
        return bufferDupe;
    }

    /**
   * Marks the specified buffer instance retrieved via the <tt>loadBuffer()</tt> method as reusable for other
   * operations. After this call the next call to {@link #loadBuffer()} may return the identical buffer
   * instance than specified.
   * <p>
   * This method does not unread any bytes, as the {@link #unread(ByteBuffer) unread} method does.
   * The purpose of this method is to avoid object creation by reusing the specified buffer instance.
   * </p><p>
   * The behaviour of this stream is unspecified if the caller accesses the specified buffer after this call.
   * </p>
   *
   * @param loadedBuffer
   *  the buffer instance retrieved via the {@link #loadBuffer()} method.
   *
   * @throws IllegalArgumentException
   *  if the specified buffer was not retrieved via the <tt>loadBuffer()</tt> method.
   *
   * @see #loadBuffer()
   * @see #unread(ByteBuffer)
   *
   * @since JaXLib 1.0
   */
    public final void recycleBuffer(final ByteBuffer loadedBuffer) throws IOException {
        recycleBuffer(loadedBuffer, false);
    }

    private void recycleBuffer(final ByteBuffer loadedBuffer, final boolean unread) throws IOException {
        if (loadedBuffer == null) throw new NullPointerException("null buffer specified.");
        if (this.loadedBuffer != loadedBuffer) throw new IllegalArgumentException("Specified buffer was not retrieved via a load operation, or already was recycled, " + "or a read operation has been applied since the buffer was loaded.");
        final int lim = loadedBuffer.limit();
        if (unread) {
            final ByteBuffer buffer = ensureOpen();
            final int pos = loadedBuffer.position();
            if (pos < this.loadPos) {
                throw new IllegalArgumentException("Specified buffer's position is less than the position when the buffer was loaded.");
            }
            buffer.limit(lim).position(pos);
        }
        loadedBuffer.position(lim);
        if (this.buffer != null) this.bufferDupe = loadedBuffer;
    }

    /**
   * Set the underlying stream to read from.
   *
   * @param in
   *  the new stream to read from.
   * @param clearBuffer
   *  whether to discard all currently buffered bytes.
   * @param closeCurrent
   *  whether to close the current input stream.
   *
   * @since JaXLib 1.0
   */
    protected void setIn(final InputStream in, final boolean clearBuffer, final boolean closeCurrent) throws IOException {
        CheckArg.notNull(in, "in");
        if (clearBuffer) clearBuffer();
        if (closeCurrent) this.in.close();
        initIn(in);
    }

    @Override
    public final int read() throws IOException {
        ByteBuffer buffer = ensureOpen();
        if (buffer.hasRemaining()) return buffer.get() & 0xff;
        buffer = null;
        buffer = fill();
        return buffer.hasRemaining() ? (buffer.get() & 0xff) : -1;
    }

    @Override
    public final int read(final byte[] dest, final int off, int len) throws IOException {
        CheckBounds.offset(dest.length, off, len);
        ByteBuffer buffer = ensureOpen();
        if (len == 0) return 0;
        if (dest == buffer.array()) throw new IllegalArgumentException("specified array is the array used by this stream");
        final int pos = buffer.position();
        int rem = buffer.remaining();
        int mark = this.mark;
        if ((mark >= 0) && (pos > this.markLimit)) this.mark = mark = MARK_INVALID;
        if (rem > 0) {
            len = Math.min(len, rem);
            buffer.get(dest, off, len);
            return len;
        } else if ((mark >= 0) || (len < buffer.capacity() >> 1)) {
            buffer = null;
            buffer = fill();
            rem = buffer.remaining();
            if (rem <= 0) return -1;
            len = Math.min(len, rem);
            buffer.get(dest, off, len);
            return len;
        } else {
            buffer = null;
            return this.in.read(dest, off, len);
        }
    }

    @Override
    public final int read(final ByteBuffer dest) throws IOException {
        int len = dest.remaining();
        if (len == 0) return 0;
        CheckArg.writable(dest);
        ByteBuffer buffer = ensureOpen();
        if (dest.hasArray() && (dest.array() == buffer.array())) throw new IllegalArgumentException("specified buffer's array is the array used by this stream");
        int pos = buffer.position();
        int rem = buffer.remaining();
        int mark = this.mark;
        if ((mark >= 0) && (pos > this.markLimit)) this.mark = mark = MARK_INVALID;
        if (rem > 0) {
            len = Math.min(len, rem);
            buffer.limit(pos + len);
            dest.put(buffer);
            buffer.limit(pos + rem);
            return len;
        } else if ((mark >= 0) || (len < buffer.capacity() >> 1) || (!dest.hasArray() && (this.inChannel == null))) {
            buffer = null;
            buffer = fill();
            rem = buffer.remaining();
            if (rem <= 0) return -1;
            pos = buffer.position();
            len = Math.min(len, rem);
            buffer.limit(pos + len);
            dest.put(buffer);
            buffer.limit(pos + rem);
            return len;
        } else {
            buffer = null;
            if (this.inChannel != null) return this.inChannel.read(dest);
            len = this.in.read(dest.array(), dest.arrayOffset() + dest.position(), len);
            if (len > 0) dest.position(dest.position() + len);
            return len;
        }
    }

    @Override
    public final byte readByte() throws IOException {
        ByteBuffer buffer = ensureOpen();
        if (buffer.hasRemaining()) return buffer.get();
        buffer = null;
        buffer = fill();
        if (!buffer.hasRemaining()) throw new EOFException();
        return buffer.get();
    }

    public final String readBytesFully(final int count) throws IOException {
        CheckArg.notNegative(count, "count");
        if (count == 0) {
            ensureOpen();
            return "";
        } else if (count == 1) {
            return Chars.toString((char) readUnsignedByte());
        } else {
            ByteBuffer buf = ensureOpen();
            if (buf.remaining() >= count) {
                final String s = new String(buf.array(), buf.position(), count);
                buf.position(buf.position() + count);
                return s;
            } else {
                buf = null;
                final char[] a = new char[count];
                for (int i = 0; i < count; i++) a[i] = (char) readUnsignedByte();
                return unsafeStringConstructor.newStringUsingArray(a);
            }
        }
    }

    @Override
    public final char readChar() throws IOException {
        ByteBuffer buf = ensureOpen();
        if (buf.remaining() >= 2) return buf.getChar();
        buf = null;
        return (char) ((readUnsignedByte() << 8) | (readUnsignedByte() << 0));
    }

    public final String readCharsFully(final int count) throws IOException {
        CheckArg.notNegative(count, "count");
        if (count == 0) {
            ensureOpen();
            return "";
        }
        if (count == 1) return Chars.toString(readChar());
        CharBuffer buf = CharBuffer.allocate(count);
        readFully(buf);
        final char[] a = buf.array();
        buf = null;
        return unsafeStringConstructor.newStringUsingArray(a);
    }

    @Override
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public final int readInt() throws IOException {
        ByteBuffer buf = ensureOpen();
        if (buf.remaining() >= 4) return buf.getInt();
        buf = null;
        return ((readUnsignedByte() << 24) | (readUnsignedByte() << 16) | (readUnsignedByte() << 8) | (readUnsignedByte() << 0));
    }

    @Override
    public final long readLong() throws IOException {
        ByteBuffer buf = ensureOpen();
        if (buf.remaining() >= 8) return buf.getLong();
        buf = null;
        return ((long) (readInt()) << 32) | (readInt() & 0xFFFFFFFFL);
    }

    @Override
    public final short readShort() throws IOException {
        return (short) readChar();
    }

    @Override
    public final int readUnsignedByte() throws IOException {
        final int b = read();
        if (b < 0) throw new EOFException();
        return b;
    }

    @Override
    public final long readUnsignedInt() throws IOException {
        return readInt() & 0xFFFFFFFFL;
    }

    @Override
    public final int readUnsignedShort() throws IOException {
        return readChar();
    }

    @Override
    public final void readFully(final byte[] dest) throws IOException {
        readFully(dest, 0, dest.length);
    }

    @Override
    public final void readFully(final byte[] dest, int off, int len) throws IOException {
        while (len > 0) {
            final int step = read(dest, off, len);
            if (step < 0) throw new EOFException();
            off += step;
            len -= step;
        }
    }

    @Override
    public final void readFully(final ByteBuffer dest) throws IOException {
        while (dest.remaining() > 0) {
            if (read(dest) < 0) throw new EOFException();
        }
    }

    @Override
    public final void readFully(final CharBuffer dest) throws IOException {
        for (int remaining = dest.remaining(); remaining > 0; ) {
            ByteBuffer buf = fill();
            final int step = Math.min(remaining, buf.remaining() >> 1);
            if (step == 0) {
                buf = null;
                dest.put(readChar());
                remaining--;
            } else {
                if (step > 2048 >> 1) {
                    final CharBuffer cbuf = buf.asCharBuffer();
                    cbuf.limit(cbuf.position() + step);
                    dest.put(cbuf);
                } else {
                    for (int i = buf.position(), hi = i + (step << 1); i < hi; i += 2) dest.put(buf.getChar(i));
                }
                remaining -= step;
                buf.position(buf.position() + (step << 1));
            }
        }
    }

    @Override
    public final void readFully(final DoubleBuffer dest) throws IOException {
        for (int remaining = dest.remaining(); remaining > 0; ) {
            ByteBuffer buf = fill();
            final int step = Math.min(remaining, buf.remaining() >> 3);
            if (step == 0) {
                buf = null;
                dest.put(readDouble());
                remaining--;
            } else {
                if (step > 2048 >> 3) {
                    final DoubleBuffer cbuf = buf.asDoubleBuffer();
                    cbuf.limit(cbuf.position() + step);
                    dest.put(cbuf);
                } else {
                    for (int i = buf.position(), hi = i + (step << 3); i < hi; i += 8) dest.put(buf.getDouble(i));
                }
                remaining -= step;
                buf.position(buf.position() + (step << 3));
            }
        }
    }

    @Override
    public final void readFully(final FloatBuffer dest) throws IOException {
        for (int remaining = dest.remaining(); remaining > 0; ) {
            ByteBuffer buf = fill();
            final int step = Math.min(remaining, buf.remaining() >> 2);
            if (step == 0) {
                buf = null;
                dest.put(readFloat());
                remaining--;
            } else {
                if (step > 2048 >> 2) {
                    final FloatBuffer cbuf = buf.asFloatBuffer();
                    cbuf.limit(cbuf.position() + step);
                    dest.put(cbuf);
                } else {
                    for (int i = buf.position(), hi = i + (step << 2); i < hi; i += 4) dest.put(buf.getFloat(i));
                }
                remaining -= step;
                buf.position(buf.position() + (step << 2));
            }
        }
    }

    @Override
    public final void readFully(final IntBuffer dest) throws IOException {
        for (int remaining = dest.remaining(); remaining > 0; ) {
            ByteBuffer buf = fill();
            final int step = Math.min(remaining, buf.remaining() >> 2);
            if (step == 0) {
                buf = null;
                dest.put(readInt());
                remaining--;
            } else {
                if (step > 2048 >> 2) {
                    final IntBuffer cbuf = buf.asIntBuffer();
                    cbuf.limit(cbuf.position() + step);
                    dest.put(cbuf);
                } else {
                    for (int i = buf.position(), hi = i + (step << 2); i < hi; i += 4) dest.put(buf.getInt(i));
                }
                remaining -= step;
                buf.position(buf.position() + (step << 2));
            }
        }
    }

    @Override
    public final void readFully(final LongBuffer dest) throws IOException {
        for (int remaining = dest.remaining(); remaining > 0; ) {
            ByteBuffer buf = fill();
            final int step = Math.min(remaining, buf.remaining() >> 3);
            if (step == 0) {
                buf = null;
                dest.put(readLong());
                remaining--;
            } else {
                if (step > 2048 >> 3) {
                    final LongBuffer cbuf = buf.asLongBuffer();
                    cbuf.limit(cbuf.position() + step);
                    dest.put(cbuf);
                } else {
                    for (int i = buf.position(), hi = i + (step << 3); i < hi; i += 8) dest.put(buf.getLong(i));
                }
                remaining -= step;
                buf.position(buf.position() + (step << 3));
            }
        }
    }

    @Override
    public final void readFully(final ShortBuffer dest) throws IOException {
        for (int remaining = dest.remaining(); remaining > 0; ) {
            ByteBuffer buf = fill();
            final int step = Math.min(remaining, buf.remaining() >> 1);
            if (step == 0) {
                buf = null;
                dest.put(readShort());
                remaining--;
            } else {
                if (step > 2048 >> 1) {
                    final ShortBuffer cbuf = buf.asShortBuffer();
                    cbuf.limit(cbuf.position() + step);
                    dest.put(cbuf);
                } else {
                    for (int i = buf.position(), hi = i + (step << 1); i < hi; i += 2) dest.put(buf.getShort(i));
                }
                remaining -= step;
                buf.position(buf.position() + (step << 1));
            }
        }
    }

    /**
   * <tt>BufferedXInputStream</tt> implements the <tt>readLine</tt> method as specified by the
   * <tt>DataInput</tt> interface.
   *
   * @return
   *  the line readed, or <tt>null</tt> if the end of the stream has been reached before a newline character
   *  has been found.
   *
   * @throws IOException if an I/O error occurs.
   *
   * @see XDataInput#readLine()
   *
   * @since JaXLib 1.0
   */
    @Override
    public String readLine() throws IOException {
        final StringBuilder sb = new StringBuilder(256);
        int b;
        while (((b = read()) >= 0) && (b != '\n') && (b != '\r')) sb.append((char) b);
        if (b == '\r') {
            b = read();
            if ((b != '\n') && (b >= 0)) unread(b);
        }
        return (sb.length() == 0) ? "" : sb.toString();
    }

    public int readLine(final Appendable out) throws IOException {
        int b;
        int count = 0;
        while (true) {
            b = read();
            if (b < 0) return count;
            count++;
            if ((b == '\n') || (b == '\r')) break;
            out.append((char) b);
        }
        if (b == '\r') {
            b = read();
            if (b == '\n') count++; else if (b >= 0) unread(b);
        }
        return count;
    }

    public String readString(final int byteLength, final String encoding) throws IOException {
        CheckArg.notNegative(byteLength, "byteLength");
        ByteBuffer buffer = ensureOpen();
        if (byteLength == 0) {
            ensureOpen();
            return "";
        } else if (buffer.remaining() >= byteLength) {
            final String s = new String(buffer.array(), buffer.arrayOffset() + buffer.position(), byteLength, encoding);
            buffer.position(buffer.position() + byteLength);
            return s;
        } else {
            buffer = null;
            final byte[] a = new byte[byteLength];
            readFully(a);
            return new String(a, encoding);
        }
    }

    @Override
    public final long skip(final long maxCount) throws IOException {
        CheckArg.notNegative(maxCount, "maxCount");
        ByteBuffer buffer = ensureOpen();
        if (maxCount == 0) {
            return 0;
        } else if (buffer.hasRemaining()) {
            buffer = null;
            return skip0(maxCount);
        } else {
            if ((this.mark >= 0) && (buffer.position() > this.markLimit)) this.mark = MARK_INVALID;
            buffer = null;
            if (this.mark >= 0) return skip0(maxCount); else return this.in.skip(maxCount);
        }
    }

    private int skip0(final long count) throws IOException {
        final ByteBuffer buffer = fill();
        final int skipped = (int) Math.min(buffer.remaining(), Math.min(count, Integer.MAX_VALUE));
        buffer.position(buffer.position() + skipped);
        return skipped;
    }

    @Override
    public long transferBytesTo(final DataOutput dest, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) {
            ensureOpen();
            return 0;
        }
        int count = 0;
        while (true) {
            ByteBuffer buffer = fill();
            final int step = (maxCount < 0) ? buffer.remaining() : (int) Math.min(buffer.remaining(), maxCount - count);
            if (step == 0) break;
            dest.write(buffer.array(), buffer.position(), step);
            buffer.position(buffer.position() + step);
            count += step;
            if (maxCount < 0) {
                buffer = fill();
                if (!buffer.hasRemaining()) break;
            } else if (count < maxCount) buffer = fill(); else break;
        }
        return count;
    }

    @Override
    public long transferTo(final OutputStream dest, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) {
            ensureOpen();
            return 0;
        }
        int count = 0;
        while (true) {
            ByteBuffer buffer = fill();
            int step = (maxCount < 0) ? buffer.remaining() : (int) Math.min(buffer.remaining(), maxCount - count);
            if (step == 0) break;
            dest.write(buffer.array(), buffer.position(), step);
            buffer.position(buffer.position() + step);
            count += step;
            if (maxCount < 0) {
                buffer = fill();
                if (!buffer.hasRemaining()) break;
            } else if (count < maxCount) buffer = fill(); else break;
        }
        return count;
    }

    @Override
    public long transferToByteChannel(final WritableByteChannel dest, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) {
            ensureOpen();
            return 0;
        }
        long count = 0;
        while (true) {
            if ((maxCount >= 0) && (count >= maxCount)) break;
            final ByteBuffer buffer = fill();
            final int maxStep = (maxCount < 0) ? buffer.remaining() : (int) Math.min(buffer.remaining(), maxCount - count);
            if (maxStep == 0) break;
            final int oldLimit = buffer.limit();
            final int oldPos = buffer.position();
            buffer.limit(oldPos + maxStep);
            try {
                final int step = dest.write(buffer);
                if ((step > maxStep) || (buffer.position() != oldPos + step)) throw new ExternalAssertionException(dest, "write");
                count += step;
            } finally {
                buffer.limit(oldLimit);
            }
        }
        return count;
    }

    /**
   * Unreads the specified byte.
   *
   * @throws IOException               if an I/O error occurs.
   *
   * @since JaXLib 1.0
   */
    public final void unread(final int c) throws IOException {
        final int pos = prepareUnread(1);
        this.buffer.put(pos, (byte) c);
    }

    /**
   * Unreads the specified sequence of bytes.
   *
   * @throws IOException
   *  if an I/O error occurs.
   * @throws NullPointerException
   *  if <code>buf == null</code>.
   * @throws IllegalArgumentException
   *  if the specified array is identical to the array used by this stream for for buffering.
   *
   * @since JaXLib 1.0
   */
    public final void unread(final byte[] buf) throws IOException {
        unread(buf, 0, buf.length);
    }

    /**
   * Unreads the specified sequence of bytes.
   *
   * @throws IOException
   *  if an I/O error occurs.
   * @throws IndexOutOfBoundsException
   *  if <code>(offs < 0) || (len < 0) || (offs + len > buf.length)</code>.
   * @throws NullPointerException
   *  if <code>buf == null</code>.
   * @throws IllegalArgumentException
   *  if the specified array is identical to the array used by this stream for for buffering.
   *
   * @since JaXLib 1.0
   */
    public final void unread(final byte[] buf, final int offs, final int len) throws IOException {
        CheckBounds.offset(buf.length, offs, len);
        if (buf == ensureOpen().array()) throw new IllegalArgumentException("the array is identical to the array used by this stream");
        final int pos = prepareUnread(len);
        this.buffer.put(buf, offs, len);
        this.buffer.position(pos);
    }

    /**
   * Unreads all remaining bytes in the specified buffer.
   * After this call the buffer is empty.
   *
   * @throws IOException
   *  if an I/O error occurs.
   * @throws NullPointerException
   *  if <code>buf == null</code>.
   * @throws IllegalArgumentException
   *  if the specified buffer was retrieved via the {@link #loadBuffer()} method and its position is less
   *  than at the time it was loaded.
   *
   * @since JaXLib 1.0
   */
    public final void unread(final ByteBuffer buf) throws IOException {
        if ((buf == this.loadedBuffer) && (buf != null)) recycleBuffer(buf, true); else if (buf.hasArray() && (buf.array() == ensureOpen().array())) {
            throw new IllegalArgumentException("the buffer shares the array of this stream but was not loaded via this stream's loadBuffer() method");
        } else {
            final int pos = prepareUnread(buf.remaining());
            this.buffer.put(buf);
            this.buffer.position(pos);
        }
    }
}
