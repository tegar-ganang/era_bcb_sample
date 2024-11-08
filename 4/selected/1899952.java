package jaxlib.io.stream;

import java.io.DataInput;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.WritableByteChannel;
import javax.annotation.Nonnull;
import jaxlib.io.channel.IOChannels;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckBounds;

/**
 * A buffered byte output stream.
 * <p>
 * This class is <b>not</b> threadsafe.
 * If you need a threadsafe stream you may use {@link jaxlib.io.stream.concurrent.SynchronizedXOutputStream}.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: BufferedXOutputStream.java 3051 2012-02-13 01:37:48Z joerg_wassmer $
 */
public class BufferedXOutputStream extends XOutputStream implements XDataOutput {

    private ByteBuffer buffer;

    private int capacity;

    private ByteOrder order = ByteOrder.BIG_ENDIAN;

    private OutputStream out;

    private WritableByteChannel outChannel;

    private ByteBuffer intermediateBuffer;

    private CharBuffer intermediateCharBuffer;

    private int intermediateBufferCapacity;

    private boolean closed;

    private int maximumCapacity;

    /**
   * Creates a new <tt>BufferedXOutputStream</tt> using an initial buffer capacity of <tt>8192</tt>.
   * <p>
   * The buffer will be allocated when the first write operation occurs.
   * </p>
   *
   * @param OutputStream
   *  the stream to write to; may be null to create a disconnected buffer or to create destination stream
   *  on demand (see {@link #createStream()}).
   *
   * @since JaXLib 1.0
   */
    public BufferedXOutputStream(final OutputStream out) {
        this(out, 8192);
    }

    /**
   * Creates a new <tt>BufferedXOutputStream</tt> using specified initial buffer capacity.
   * <p>
   * This class may choose a slightly bigger value than specified.
   * The buffer will be allocated when the first write operation occurs.
   * </p>
   *
   * @param OutputStream
   *  the stream to write to; may be null to create a disconnected buffer or to create destination stream
   *  on demand (see {@link #createStream()}).
   *
   * @throws IllegalArgumentException if <code>initialBufferCapacity < 0</code>.
   *
   * @since JaXLib 1.0
   */
    public BufferedXOutputStream(final OutputStream out, final int initialBufferCapacity) {
        super();
        initOut(out);
        initBuffer(initialBufferCapacity);
        this.intermediateBufferCapacity = this.capacity;
        this.maximumCapacity = this.capacity;
    }

    /**
   * Creates a new <tt>BufferedXOutputStream</tt> which uses the specified array for buffering.
   * <p>
   * The array will be used as long as its length fits the needs for requested operations.
   * </p><p>
   * The specified array will not be used at all if its length does not fit the needs of this class.
   * The array has to be of length <tt>8</tt> at least and its length has to be dividable by <tt>8</tt>.
   * </p>
   *
   * @param OutputStream
   *  the stream to write to; may be null to create a disconnected buffer or to create destination stream
   *  on demand (see {@link #createStream()}).
   *
   * @throws NullPointerException
   *  if {@code initialBuffer == null}.
   *
   * @since JaXLib 1.0
   */
    public BufferedXOutputStream(final OutputStream out, final byte[] initialBuffer) {
        super();
        initOut(out);
        if (initialBuffer == null) {
            throw new NullPointerException("initialBuffer");
        } else if ((initialBuffer.length >= 8) && (initialBuffer.length % 8 == 0)) {
            this.buffer = ByteBuffer.wrap(initialBuffer);
            this.capacity = initialBuffer.length;
        } else {
            initBuffer(initialBuffer.length);
        }
        this.intermediateBufferCapacity = this.capacity;
        this.maximumCapacity = this.capacity;
    }

    public BufferedXOutputStream(final OutputStream out, final int initialBufferCapacity, int maximumBufferCapacity) {
        super();
        if (maximumBufferCapacity != -1) {
            CheckArg.le(initialBufferCapacity, maximumBufferCapacity, "initialBufferCapacity", "maximumBufferCapacity");
        }
        initOut(out);
        initBuffer(initialBufferCapacity);
        this.intermediateBufferCapacity = this.capacity;
        maximumBufferCapacity = Math.max(8, maximumBufferCapacity + (maximumBufferCapacity % 8));
        this.maximumCapacity = Math.max(this.capacity, maximumBufferCapacity);
    }

    private void initBuffer(final int bufferSize) {
        if (bufferSize < 0) throw new IllegalArgumentException("initialBufferCapacity(" + bufferSize + ") < 0.");
        this.capacity = Math.max(8, bufferSize + (bufferSize % 8));
    }

    private void initOut(final OutputStream out) {
        this.out = out;
        if (out instanceof WritableByteChannel) this.outChannel = (WritableByteChannel) out; else if (out instanceof FileOutputStream) this.outChannel = ((FileOutputStream) out).getChannel(); else this.outChannel = null;
    }

    private ByteBuffer intermediateBuffer() throws IOException {
        ByteBuffer buf = this.intermediateBuffer;
        if (buf == null) {
            if (this.out != null) this.intermediateBuffer = buf = ByteBuffer.allocate(this.intermediateBufferCapacity); else throw new IOException("closed");
        } else {
            buf.clear();
        }
        return buf;
    }

    private CharBuffer intermediateCharBuffer() throws IOException {
        CharBuffer buf = this.intermediateCharBuffer;
        if (buf == null) this.intermediateCharBuffer = buf = intermediateBuffer().asCharBuffer(); else buf.clear();
        return buf;
    }

    /**
   * Make room in the buffer.
   * If minFree is <= 8 then at least minFree bytes will be free. For greater values the limit is
   * the maximum capacity. If no maximum capacity has been specified at construction time, then
   * the buffer will not grow.
   * <p>
   * Note we ensured at construction time that the buffer has a capacity of at least 8 bytes (to hold
   * the bytes of a long or double value).
   * </p>
   */
    final ByteBuffer ensureOpen(final int minFree) throws IOException {
        ByteBuffer buffer = this.buffer;
        if (buffer != null) {
            if ((minFree > 0) && (buffer.remaining() < minFree)) {
                if (this.maximumCapacity > buffer.capacity()) {
                    int newCapacity = Math.min(buffer.capacity() << 1, this.maximumCapacity);
                    if (newCapacity < buffer.capacity()) newCapacity = this.maximumCapacity;
                    if (this.closed) throw new IOException("closed");
                    final ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity).order(this.order);
                    buffer.limit(buffer.position()).position(0);
                    newBuffer.put(buffer);
                    this.buffer = buffer = newBuffer;
                }
                if (buffer.remaining() < minFree) flushBuffer(buffer, Math.min(buffer.capacity(), minFree));
            }
            return buffer;
        } else if (!this.closed) {
            this.buffer = buffer = ByteBuffer.allocate(this.capacity).order(this.order);
            return buffer;
        } else {
            throw new IOException("closed");
        }
    }

    protected final OutputStream out() throws IOException {
        OutputStream out = this.out;
        if (out == null) {
            if (this.closed) throw new IOException("closed");
            out = createOut();
            if (out == null) {
                closeInstance();
                throw new IOException("closed");
            }
            initOut(out);
            this.out = out;
        }
        return out;
    }

    /**
   * Called the first time more buffer space is required if this buffered stream has been constructed
   * with a {@code null} destination stream and has not been closed.
   * <p>
   * If this method returns {@code null} then operations will fail with an {@code IOException}. The default
   * implementation returns {@code null}.
   * </p><p>
   * <b>Calls to {@link #flush()} and {@link #close()} do not cause the stream to be created!</b>
   * </p>
   *
   * @since JaXLib 1.0
   */
    protected OutputStream createOut() throws IOException {
        return null;
    }

    /**
   * Transfers bytes from the internal buffer to the underlying stream, making at least the specified number
   * of bytes free in the buffer.
   * <p>
   * After this call the {@link ByteBuffer#position()} of the buffer is the first index in the array to be
   * filled by subsequent write operations. The sequence between index zero and the new position are the
   * bytes which have to be written to the underlying stream later.
   * </p><p>
   * Subclasses must take care not to modify the {@link ByteBuffer#limit()} of the specified buffer.
   * The limit must always be equal to the {@link ByteBuffer#capacity()}.
   * </p>
   *
   * @param buffer
   *  The internal buffer of this output stream. Must be backed by an array.
   * @param minFree
   *  The minimum number of bytes to make free in the buffer. If negative then the method decides the amount.
   *
   * @throws IOException
   *  if an I/O error occurs.
   * @throws IllegalArgumentException
   *  if {@code minFree > buffer.capacity()}.
   * @throws NullPointerException
   *  if {@code buffer == null}.
   *
   * @since JaXLib 1.0
   */
    protected void flushBuffer(final ByteBuffer buffer, int minFree) throws IOException {
        int pos = buffer.position();
        if (minFree < 0) minFree = pos; else if (buffer.remaining() >= minFree) return; else if (minFree > buffer.capacity()) throw new IllegalArgumentException("minFree(" + minFree + ") > capacity(" + buffer.capacity() + ")");
        if (pos == 0) return;
        OutputStream out = out();
        WritableByteChannel outChannel = this.outChannel;
        if (outChannel == null) {
            out.write(buffer.array(), 0, pos);
            buffer.clear();
        } else if (minFree >= pos) {
            buffer.flip();
            IOChannels.writeFully(outChannel, buffer);
            buffer.clear();
        } else {
            buffer.flip();
            pos = 0;
            minFree = Math.min(minFree, buffer.remaining());
            try {
                while (pos < minFree) {
                    final int step = outChannel.write(buffer);
                    final int newPos = buffer.position();
                    if (newPos != (pos + step)) {
                        throw new ReturnValueException(outChannel, "write(CharBuffer)", step, "buffer.position() - initial position");
                    }
                    if (step != 0) {
                        pos = newPos;
                        continue;
                    } else {
                        this.outChannel = outChannel = null;
                        out.write(buffer.array(), buffer.position(), buffer.remaining());
                        buffer.clear();
                        break;
                    }
                }
            } finally {
                buffer.compact();
            }
        }
    }

    /**
   * Called by XOutputStream.writeSecurely()
   */
    @Override
    final void writeSecurelyImpl(final byte[] source, int off, int len) throws IOException {
        assert !((source == null) || (off < 0) || (off > source.length) || (len < 0) || ((off + len) > source.length) || ((off + len) < 0));
        final ByteBuffer buffer = ensureOpen(len);
        if (buffer.remaining() < len) {
            final int bufferSize = buffer.capacity();
            if (bufferSize >= len) {
                flushBuffer(buffer, len);
                buffer.put(source, off, len);
            } else {
                while (len > 0) {
                    flushBuffer(buffer, Math.min(buffer.position(), len));
                    int step = Math.min(len, buffer.remaining());
                    buffer.put(source, off, step);
                    off += step;
                    len -= step;
                }
            }
        } else {
            buffer.put(source, off, len);
        }
    }

    /**
   * Returns the capacity of the buffer.
   *
   * @since JaXLib 1.0
   */
    public final int bufferCapacity() {
        return this.capacity;
    }

    /**
   * The current number of buffered bytes.
   */
    public final int countBytesInBuffer() {
        final ByteBuffer buf = this.buffer;
        return (buf == null) ? 0 : buf.position();
    }

    /**
   * Returns the actual buffer.
   * <p>
   * The returned buffer instance is identical to the instance used internally by this stream. The buffered
   * bytes are located between index zero and the actual position of the buffer. Changes to the buffer
   * are visible to the stream.
   * </p><p>
   * Overwriting this method has no effect inside {@code BufferedXOutputStream}.
   * </p>
   *
   * @return the actual buffer, <code>null</code> if this stream is closed.
   *
   * @since JaXLib 1.0
   */
    protected ByteBuffer getBuffer() {
        ByteBuffer buffer = this.buffer;
        if ((buffer == null) && !this.closed) this.buffer = buffer = ByteBuffer.allocate(this.capacity).order(this.order);
        return buffer;
    }

    /**
   * Returns the byte order this streams uses for numbers.
   *
   * @see #setByteOrder(ByteOrder)
   *
   * @since JaXLib 1.0
   */
    @Nonnull
    public final ByteOrder getByteOrder() {
        return this.order;
    }

    @Nonnull
    protected ByteBuffer getIntermediateBuffer() throws IOException {
        return intermediateBuffer();
    }

    @Nonnull
    protected CharBuffer getIntermediateCharBuffer() throws IOException {
        return intermediateCharBuffer();
    }

    public final int getIntermediateBufferCapacity() {
        return this.intermediateBufferCapacity;
    }

    /**
   * Returns the maximum capacity of the buffer.
   *
   * @since JaXLib 1.0
   */
    public final int getMaxBufferCapacity() {
        return Math.max(this.capacity, this.maximumCapacity);
    }

    protected OutputStream getOut() {
        return this.out;
    }

    protected void setMaxBufferCapacity(final int max) throws IOException {
        CheckArg.notNegative(max, "max");
        ByteBuffer buffer = this.buffer;
        if ((buffer != null) && (buffer.position() > max)) {
            buffer = null;
            flushBuffer();
            buffer = this.buffer;
            if ((buffer != null) && (buffer.capacity() > max)) {
                final ByteBuffer newBuffer = ByteBuffer.allocate(max);
                buffer.limit(buffer.position()).position(0);
                newBuffer.put(buffer);
                this.buffer = buffer = newBuffer;
            }
        }
        this.maximumCapacity = max;
    }

    /**
   * Sets the byte order for numbers written to this stream.
   * The default order is {@link ByteOrder#BIG_ENDIAN big endian}.
   *
   * @see #writeChar(int)
   * @see #writeDouble(double)
   * @see #writeFloat(float)
   * @see #writeInt(int)
   * @see #writeLong(long)
   * @see #writeShort(int)
   * @see #writeUTF(String)
   *
   * @since JaXLib 1.0
   */
    protected void setByteOrder(final ByteOrder order) {
        if (order == null) throw new NullPointerException("order");
        this.order = order;
        if (this.buffer != null) this.buffer.order(order);
    }

    protected void setIntermediateBufferCapacity(int capacity) {
        CheckArg.notNegative(capacity, "capacity");
        capacity = Math.max(8, capacity);
        if ((capacity & 1) != 0) capacity++;
        if (capacity != this.intermediateBufferCapacity) {
            this.intermediateBuffer = null;
            this.intermediateCharBuffer = null;
            this.intermediateBufferCapacity = capacity;
        }
    }

    /**
   * Set the underlying stream to write to.
   *
   * @param out
   *  the new stream to write to.
   * @param flush
   *  whether to write the current buffer content to the current output stream.
   * @param closeCurrent
   *  whether to close the current output stream.
   *
   * @since JaXLib 1.0
   */
    protected void setOut(final OutputStream out, final boolean flush, final boolean closeCurrent) throws IOException {
        CheckArg.notNull(out, "out");
        ensureOpen(0);
        if (flush) flush();
        if (closeCurrent) {
            final OutputStream oldOut = this.out;
            if (oldOut != null) {
                oldOut.close();
                this.out = null;
            }
        }
        initOut(out);
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            final OutputStream out = this.out;
            IOException ex = null;
            try {
                closeInstance();
            } catch (final IOException sex) {
                ex = sex;
            } finally {
                try {
                    if (out != null) out.close();
                } catch (final IOException sex) {
                    if (ex == null) ex = sex;
                }
                if (ex != null) throw ex;
            }
        }
    }

    @Override
    public void closeInstance() throws IOException {
        if (!this.closed) {
            try {
                flushBuffer();
            } finally {
                this.closed = true;
                this.buffer = null;
                this.intermediateBuffer = null;
                this.intermediateCharBuffer = null;
                this.out = null;
                this.outChannel = null;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        final OutputStream out = this.out;
        if (out != null) out.flush();
    }

    /**
   * Flushes the buffer but does not flush the underlying stream.
   *
   * @throws IOException
   *  if an I/O error occurs.
   *
   * @see #flush()
   *
   * @since JaXLib 1.0
   */
    public void flushBuffer() throws IOException {
        final ByteBuffer buffer = this.buffer;
        if ((buffer != null) && (buffer.position() > 0) && (this.out != null)) flushBuffer(buffer, buffer.capacity()); else if (this.closed) throw new IOException("closed");
    }

    @Override
    public final boolean isOpen() {
        return !this.closed;
    }

    @Override
    public void transferBytesFullyFrom(final DataInput in, long count) throws IOException {
        CheckArg.count(count);
        while (count > 0) {
            final ByteBuffer buffer = ensureOpen((int) Math.min(8192, count));
            final int step = Math.min(buffer.remaining(), (int) Math.min(Integer.MAX_VALUE, count));
            in.readFully(buffer.array(), buffer.position(), step);
            count -= step;
            buffer.position(buffer.position() + step);
        }
    }

    public long transferCharsFrom(final Readable in, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) {
            ensureOpen(0);
            return 0;
        } else if (in instanceof CharBuffer) {
            CharBuffer src = (CharBuffer) in;
            if ((maxCount < 0) || (maxCount >= src.remaining())) {
                return write(src);
            } else {
                src = src.duplicate();
                src.limit(src.position() + (int) maxCount);
                return write(src);
            }
        } else {
            long count = 0;
            LOOP: while ((count < maxCount) || (maxCount < 0)) {
                ByteBuffer bb = ensureOpen((maxCount < 0) ? 8192 : (int) Math.min(8192, (maxCount < 2) ? 2 : maxCount));
                int maxStep = Math.min(bb.remaining() >> 1, (int) Math.max(0, maxCount - count));
                if ((maxStep <= 16) && (in instanceof Reader)) {
                    bb = null;
                    final Reader rin = (Reader) in;
                    while ((count < maxCount) || (maxCount < 0)) {
                        final int c = rin.read();
                        if (c < 0) break LOOP;
                        count++;
                        writeChar(c);
                    }
                    break LOOP;
                } else if ((bb.position() & 1) == 0) {
                    final int pos = bb.position();
                    bb.position(0);
                    final CharBuffer cb = bb.asCharBuffer();
                    bb.position(pos);
                    OPTIMIZED: while ((count < maxCount) || (maxCount < 0)) {
                        cb.position(bb.position() >> 1);
                        maxStep = Math.min(cb.remaining(), (int) Math.max(0, maxCount - count));
                        final int step = in.read(cb);
                        if (step < 0) break LOOP; else if (step > maxStep) throw new ReturnValueException(in, "read(byte[],int,int)", step, "<=", maxStep); else {
                            count += step;
                            bb.position(bb.position() + (step << 1));
                            if (!bb.hasRemaining() && ((ensureOpen(bb.capacity()) != bb) || ((bb.position() & 1) != 0))) continue LOOP;
                            continue OPTIMIZED;
                        }
                    }
                } else {
                    CharBuffer cb = bb.asCharBuffer();
                    cb.limit(maxStep);
                    final int step = in.read(cb);
                    cb = null;
                    if (step < 0) {
                        break LOOP;
                    } else if (step <= maxStep) {
                        count += step;
                        bb.position(bb.position() + (step << 1));
                        continue LOOP;
                    } else {
                        throw new ReturnValueException(in, "read(byte[],int,int)", step, "<=", maxStep);
                    }
                }
            }
            return count;
        }
    }

    public long transferCharsUTFFrom(final Readable in, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) {
            ensureOpen(0);
            return 0;
        } else if (in instanceof CharBuffer) {
            final CharBuffer src = (CharBuffer) in;
            final int count = src.remaining();
            if ((maxCount < 0) || (maxCount >= count) && src.hasArray()) {
                writeCharsUTF(src.array(), src.arrayOffset() + src.position(), count);
                src.position(src.limit());
            } else {
                int pos = src.position();
                final int lim = src.limit();
                try {
                    while (pos < lim) {
                        writeCharUTF(src.get(pos));
                        pos++;
                    }
                } finally {
                    src.position(pos);
                }
            }
            return count;
        } else if ((maxCount == 1) && (in instanceof Reader)) {
            final int c = ((Reader) in).read();
            if (c < 0) return 0;
            writeCharUTF(c);
            return 1;
        } else {
            long count = 0;
            while ((count < maxCount) || (maxCount < 0)) {
                final CharBuffer cb = intermediateCharBuffer();
                final int step = in.read(cb);
                if (step < 0) break;
                transferCharsUTFFrom(this.intermediateBuffer.array(), 0, cb.position() << 1);
                count += cb.position();
            }
            return count;
        }
    }

    private void transferCharsUTFFrom(final byte[] in, final int fromIndex, final int toIndex) throws IOException {
        ByteBuffer buffer = ensureOpen(0);
        int free = buffer.remaining();
        final boolean bigEndian = this.order == ByteOrder.BIG_ENDIAN;
        while (fromIndex < toIndex) {
            final int c = bigEndian ? ((in[fromIndex] << 8) | in[fromIndex + 1]) : (in[fromIndex] | (in[fromIndex + 1] << 8));
            if ((c >= 0x0001) && (c <= 0x007F)) {
                if (free < 1) {
                    buffer = null;
                    buffer = ensureOpen(1);
                    free = buffer.remaining();
                }
                buffer.put((byte) c);
                free--;
            } else if (c <= 0x07FF) {
                if (free < 2) {
                    buffer = null;
                    buffer = ensureOpen(2);
                    free = buffer.remaining();
                }
                buffer.put((byte) (0xC0 | ((c >> 6) & 0x1F)));
                buffer.put((byte) (0x80 | (c & 0x3F)));
                free -= 2;
            } else {
                if (free < 3) {
                    buffer = null;
                    buffer = ensureOpen(3);
                    free = buffer.remaining();
                }
                buffer.put((byte) (0xE0 | ((c >> 12) & 0x0F)));
                buffer.put((byte) (0x80 | ((c >> 6) & 0x3F)));
                buffer.put((byte) (0x80 | (c & 0x3F)));
                free -= 3;
            }
        }
    }

    @Override
    public long transferFrom(final InputStream in, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        long count = 0;
        while ((count < maxCount) || (maxCount < 0)) {
            final ByteBuffer buffer = ensureOpen((maxCount < 0) ? 8192 : (int) Math.min(8192, maxCount));
            final int maxStep = Math.min(buffer.remaining(), (int) Math.max(0, maxCount - count));
            final int step = in.read(buffer.array(), buffer.position(), maxStep);
            if (step < 0) {
                break;
            } else if (step <= maxStep) {
                count += step;
                buffer.position(buffer.position() + step);
            } else {
                throw new ReturnValueException(in, "read(byte[],int,int)", step, "<=", maxStep);
            }
        }
        return count;
    }

    @Override
    public final void write(final int b) throws IOException {
        ensureOpen(1).put((byte) b);
    }

    @Override
    public void write(final byte[] source, int off, int len) throws IOException {
        CheckBounds.offset(source.length, off, len);
        if (len > 0) {
            final ByteBuffer buffer = ensureOpen(len);
            final int remaining = buffer.remaining();
            if (remaining > len) {
                buffer.put(source, off, len);
            } else {
                final int capacity = buffer.capacity();
                if (remaining != capacity) {
                    buffer.put(source, off, remaining);
                    flushBuffer(buffer, capacity);
                    off += remaining;
                    len -= remaining;
                }
                if (len < capacity) {
                    buffer.put(source, off, len);
                } else {
                    final int keep = len % capacity;
                    out().write(source, off, len - keep);
                    buffer.put(source, off + (len - keep), keep);
                }
            }
        }
    }

    @Override
    public int write(final ByteBuffer source) throws IOException {
        return writeBuffer(source);
    }

    private int writeBuffer(final ByteBuffer source) throws IOException {
        final int count = source.remaining();
        ByteBuffer buffer = ensureOpen(count);
        if (count > 0) {
            if (buffer.remaining() > source.remaining()) {
                buffer.put(source);
            } else {
                final int capacity = buffer.capacity();
                if (buffer.position() < capacity) {
                    source.get(buffer.array(), buffer.position(), capacity - buffer.position());
                    flushBuffer(buffer, capacity);
                }
                final int sourceRemaining = source.remaining();
                if (sourceRemaining < capacity) {
                    buffer.put(source);
                } else {
                    final OutputStream out = out();
                    int keep = sourceRemaining % capacity;
                    if (source.hasArray()) {
                        out.write(source.array(), source.arrayOffset() + source.position(), sourceRemaining - keep);
                        source.position(source.position() + (sourceRemaining - keep));
                    } else {
                        WritableByteChannel outChannel = this.outChannel;
                        if (outChannel == null) {
                            final byte[] a = buffer.array();
                            final int lim = source.limit();
                            int pos = source.position();
                            int rem = lim - pos;
                            try {
                                while (rem > keep) {
                                    source.get(a, 0, capacity);
                                    buffer.position(capacity);
                                    flushBuffer(buffer, capacity);
                                    rem -= capacity;
                                    pos += capacity;
                                }
                            } finally {
                                source.limit(lim);
                                source.position(pos);
                            }
                        } else {
                            ByteBuffer slice = source.slice();
                            slice.limit(source.remaining() - keep);
                            try {
                                while (slice.remaining() + keep > capacity) {
                                    if (outChannel.write(slice) == 0) {
                                        this.outChannel = outChannel = null;
                                        source.position(source.position() + slice.position());
                                        slice = null;
                                        buffer = null;
                                        writeBuffer(source);
                                        return count;
                                    }
                                }
                            } finally {
                                outChannel = null;
                                if (slice != null) source.position(source.position() + slice.position());
                            }
                            keep += slice.remaining();
                        }
                    }
                    if (keep > 0) buffer.put(source);
                }
            }
        }
        return count;
    }

    public final void write(final boolean[] src, int offs, final int len) throws IOException {
        CheckBounds.offset(src.length, offs, len);
        if (len > 0) {
            final int toIndex = offs + len;
            do {
                final ByteBuffer buf = ensureOpen(toIndex - offs);
                final int step = Math.min(toIndex - offs, buf.remaining());
                final int pos = buf.position();
                for (int i = 0; i < step; i++) buf.put(pos + i, src[offs + i] ? (byte) 1 : 0);
                buf.position(pos + step);
                offs += step;
            } while (offs < toIndex);
        }
    }

    @Override
    public final int write(final CharBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; r > 0; ) {
            final ByteBuffer buffer = ensureOpen(Math.max(2, r << 1));
            final int step = Math.min(buffer.remaining() >> 1, r);
            if (step > 2048 >> 1) {
                final int lim = source.limit();
                source.limit(source.position() + step);
                buffer.asCharBuffer().put(source);
                source.limit(lim);
            } else {
                for (int i = step; i > 0; i--) buffer.putChar(source.get());
            }
            r -= step;
        }
        return count;
    }

    public final void write(final char[] src, int offs, final int len) throws IOException {
        CheckBounds.offset(src.length, offs, len);
        final int toIndex = offs + len;
        while (offs < toIndex) {
            final ByteBuffer buf = ensureOpen(Math.max(2, (toIndex - offs) << 1));
            final int step = Math.min(toIndex - offs, buf.remaining() >> 1);
            final int pos = buf.position();
            for (int i = 0; i < step; i++) buf.putChar(pos + (i << 1), src[offs + i]);
            buf.position(pos + (step << 1));
            offs += step;
        }
    }

    @Override
    public final int write(final DoubleBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; r > 0; ) {
            final ByteBuffer buffer = ensureOpen(Math.max(8, r << 3));
            final int step = Math.min(buffer.remaining() >> 3, r);
            if (step > 2048 >> 3) {
                final int lim = source.limit();
                source.limit(source.position() + step);
                buffer.asDoubleBuffer().put(source);
                source.limit(lim);
            } else {
                for (int i = step; i > 0; i--) buffer.putDouble(source.get());
            }
            r -= step;
        }
        return count;
    }

    public final void write(final double[] src, int offs, final int len) throws IOException {
        CheckBounds.offset(src.length, offs, len);
        final int toIndex = offs + len;
        while (offs < toIndex) {
            final ByteBuffer buf = ensureOpen(Math.max(8, (toIndex - offs) << 3));
            final int step = Math.min(toIndex - offs, buf.remaining() >> 3);
            final int pos = buf.position();
            for (int i = 0; i < step; i++) buf.putDouble(pos + (i << 3), src[offs + i]);
            buf.position(pos + (step << 3));
            offs += step;
        }
    }

    @Override
    public final int write(final FloatBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; r > 0; ) {
            final ByteBuffer buffer = ensureOpen(Math.max(4, r << 2));
            final int step = Math.min(buffer.remaining() >> 2, r);
            if (step > 2048 >> 2) {
                final int lim = source.limit();
                source.limit(source.position() + step);
                buffer.asFloatBuffer().put(source);
                source.limit(lim);
            } else {
                for (int i = step; i > 0; i--) buffer.putFloat(source.get());
            }
            r -= step;
        }
        return count;
    }

    public final void write(final float[] src, int offs, final int len) throws IOException {
        CheckBounds.offset(src.length, offs, len);
        final int toIndex = offs + len;
        while (offs < toIndex) {
            final ByteBuffer buf = ensureOpen(Math.max(4, (toIndex - offs) << 2));
            final int step = Math.min(toIndex - offs, buf.remaining() >> 2);
            final int pos = buf.position();
            for (int i = 0; i < step; i++) buf.putFloat(pos + (i << 2), src[offs + i]);
            buf.position(pos + (step << 2));
            offs += step;
        }
    }

    @Override
    public final int write(final IntBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; r > 0; ) {
            final ByteBuffer buffer = ensureOpen(Math.max(4, r << 2));
            final int step = Math.min(buffer.remaining() >> 2, r);
            if (step > 2048 >> 2) {
                final int lim = source.limit();
                source.limit(source.position() + step);
                buffer.asIntBuffer().put(source);
                source.limit(lim);
            } else {
                for (int i = step; i > 0; i--) buffer.putInt(source.get());
            }
            r -= step;
        }
        return count;
    }

    public final void write(final int[] src, int offs, final int len) throws IOException {
        CheckBounds.offset(src.length, offs, len);
        final int toIndex = offs + len;
        while (offs < toIndex) {
            final ByteBuffer buf = ensureOpen(Math.max(4, (toIndex - offs) << 2));
            final int step = Math.min(toIndex - offs, buf.remaining() >> 2);
            final int pos = buf.position();
            for (int i = 0; i < step; i++) buf.putInt(pos + (i << 2), src[offs + i]);
            buf.position(pos + (step << 2));
            offs += step;
        }
    }

    @Override
    public final int write(final LongBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; r > 0; ) {
            final ByteBuffer buffer = ensureOpen(Math.max(8, r << 3));
            final int step = Math.min(buffer.remaining() >> 3, r);
            if (step > 2048 >> 3) {
                final int lim = source.limit();
                source.limit(source.position() + step);
                buffer.asLongBuffer().put(source);
                source.limit(lim);
            } else {
                for (int i = step; i > 0; i--) buffer.putLong(source.get());
            }
            r -= step;
        }
        return count;
    }

    public final void write(final long[] src, int offs, final int len) throws IOException {
        CheckBounds.offset(src.length, offs, len);
        final int toIndex = offs + len;
        while (offs < toIndex) {
            final ByteBuffer buf = ensureOpen(Math.max(8, (toIndex - offs) << 3));
            final int step = Math.min(toIndex - offs, buf.remaining() >> 3);
            final int pos = buf.position();
            for (int i = 0; i < step; i++) buf.putLong(pos + (i << 3), src[offs + i]);
            buf.position(pos + (step << 3));
            offs += step;
        }
    }

    @Override
    public final int write(final ShortBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; r > 0; ) {
            final ByteBuffer buffer = ensureOpen(Math.max(2, r << 1));
            final int step = Math.min(buffer.remaining() >> 1, r);
            if (step > 2048 >> 1) {
                final int lim = source.limit();
                source.limit(source.position() + step);
                buffer.asShortBuffer().put(source);
                source.limit(lim);
            } else {
                for (int i = step; i > 0; i--) buffer.putShort(source.get());
            }
            r -= step;
        }
        return count;
    }

    public final void write(final short[] src, int offs, final int len) throws IOException {
        CheckBounds.offset(src.length, offs, len);
        final int toIndex = offs + len;
        while (offs < toIndex) {
            final ByteBuffer buf = ensureOpen(Math.max(2, (toIndex - offs) << 1));
            final int step = Math.min(toIndex - offs, buf.remaining() >> 1);
            final int pos = buf.position();
            for (int i = 0; i < step; i++) buf.putShort(pos + (i << 1), src[offs + i]);
            buf.position(pos + (step << 1));
            offs += step;
        }
    }

    @Override
    public final void writeBoolean(final boolean v) throws IOException {
        ensureOpen(1).put((byte) (v ? 1 : 0));
    }

    @Override
    public final void writeByte(final int v) throws IOException {
        ensureOpen(1).put((byte) v);
    }

    @Override
    public final void writeChar(final int v) throws IOException {
        ensureOpen(2).putChar((char) v);
    }

    @Override
    public final void writeDouble(final double v) throws IOException {
        ensureOpen(8).putDouble(v);
    }

    @Override
    public final void writeFloat(final float v) throws IOException {
        ensureOpen(4).putFloat(v);
    }

    @Override
    public final void writeInt(final int v) throws IOException {
        ensureOpen(4).putInt(v);
    }

    @Override
    public final void writeLong(final long v) throws IOException {
        ensureOpen(8).putLong(v);
    }

    @Override
    public final void writeShort(final int v) throws IOException {
        ensureOpen(2).putShort((short) v);
    }

    @Override
    public final void writeBytes(final String s) throws IOException {
        writeBytes(s, 0, s.length());
    }

    public final void writeBytes(final String s, int offs, final int len) throws IOException {
        CheckBounds.offset(s, offs, len);
        for (final int hi = offs + len; offs < hi; ) {
            final ByteBuffer buffer = ensureOpen(1);
            final int stepEnd = offs + Math.min(buffer.remaining(), len - offs);
            while (offs < stepEnd) buffer.put((byte) s.charAt(offs++));
        }
    }

    public final void writeBytes(final CharSequence source) throws IOException {
        writeBytes(source, 0, source.length());
    }

    public final void writeBytes(final CharSequence source, int offs, final int len) throws IOException {
        CheckBounds.offset(source.length(), offs, len);
        for (final int hi = offs + len; offs < hi; ) {
            final ByteBuffer buffer = ensureOpen(1);
            final int stepEnd = offs + Math.min(buffer.remaining(), len - offs);
            while (offs < stepEnd) buffer.put((byte) source.charAt(offs++));
        }
    }

    public final void writeBytes(final char[] source) throws IOException {
        writeBytes(source, 0, source.length);
    }

    public final void writeBytes(final char[] source, int offs, final int len) throws IOException {
        CheckBounds.offset(source, offs, len);
        for (final int hi = offs + len; offs < hi; ) {
            final ByteBuffer buffer = ensureOpen(1);
            final int stepEnd = offs + Math.min(buffer.remaining(), len - offs);
            while (offs < stepEnd) buffer.put((byte) source[offs++]);
        }
    }

    public final void writeChars(final CharSequence source) throws IOException {
        writeChars(source, 0, source.length());
    }

    public final void writeChars(final CharSequence source, int offs, final int len) throws IOException {
        CheckBounds.offset(source.length(), offs, len);
        for (final int hi = offs + len; offs < hi; ) {
            final ByteBuffer buffer = ensureOpen(2);
            final int stepEnd = offs + Math.min(buffer.remaining() >> 1, len - offs);
            while (offs < stepEnd) buffer.putChar(source.charAt(offs++));
        }
    }

    @Override
    public final void writeChars(final String source) throws IOException {
        writeChars(source, 0, source.length());
    }

    public final void writeChars(final String source, int offs, final int len) throws IOException {
        CheckBounds.offset(source, offs, len);
        for (final int hi = offs + len; offs < hi; ) {
            final ByteBuffer buffer = ensureOpen(2);
            final int stepEnd = offs + Math.min(buffer.remaining() >> 1, len - offs);
            while (offs < stepEnd) buffer.putChar(source.charAt(offs++));
        }
    }

    /**
   * Writes a single character {@code UTF} encoded.
   * This method writes no length information as {@link #writeUTF(String)} does.
   *
   * @since JaXLib 1.0
   */
    public final void writeCharUTF(final int c) throws IOException {
        if ((c >= 0x0001) && (c <= 0x007F)) {
            ensureOpen(1).put((byte) c);
        } else if (c <= 0x07FF) {
            final ByteBuffer buffer = ensureOpen(2);
            buffer.put((byte) (0xC0 | ((c >> 6) & 0x1F)));
            buffer.put((byte) (0x80 | (c & 0x3F)));
        } else {
            final ByteBuffer buffer = ensureOpen(3);
            buffer.put((byte) (0xE0 | ((c >> 12) & 0x0F)));
            buffer.put((byte) (0x80 | ((c >> 6) & 0x3F)));
            buffer.put((byte) (0x80 | (c & 0x3F)));
        }
    }

    /**
   * Writes the specified characters {@code UTF} encoded.
   * This method writes no length information as {@link #writeUTF(String)} does.
   *
   * @since JaXLib 1.0
   */
    public final void writeCharsUTF(final char[] str) throws IOException {
        writeCharsUTF(str, 0, str.length);
    }

    /**
   * Writes the specified characters {@code UTF} encoded.
   * This method writes no length information as {@link #writeUTF(String)} does.
   *
   * @since JaXLib 1.0
   */
    public final void writeCharsUTF(final char[] str, int offs, final int len) throws IOException {
        CheckBounds.offset(str, offs, len);
        ByteBuffer buffer = ensureOpen(len);
        int free = buffer.remaining();
        final int hi = offs + len;
        while (offs < hi) {
            final int c = str[offs++];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                if (free < 1) {
                    buffer = null;
                    buffer = ensureOpen(1);
                    free = buffer.remaining();
                }
                buffer.put((byte) c);
                free--;
            } else if (c <= 0x07FF) {
                if (free < 2) {
                    buffer = null;
                    buffer = ensureOpen(2);
                    free = buffer.remaining();
                }
                buffer.put((byte) (0xC0 | ((c >> 6) & 0x1F)));
                buffer.put((byte) (0x80 | (c & 0x3F)));
                free -= 2;
            } else {
                if (free < 3) {
                    buffer = null;
                    buffer = ensureOpen(3);
                    free = buffer.remaining();
                }
                buffer.put((byte) (0xE0 | ((c >> 12) & 0x0F)));
                buffer.put((byte) (0x80 | ((c >> 6) & 0x3F)));
                buffer.put((byte) (0x80 | (c & 0x3F)));
                free -= 3;
            }
        }
    }

    /**
   * Writes the specified characters {@code UTF} encoded.
   * This method writes no length information as {@link #writeUTF(String)} does.
   *
   * @since JaXLib 1.0
   */
    public final void writeCharsUTF(final CharSequence str) throws IOException {
        writeCharsUTF(str, 0, str.length());
    }

    /**
   * Writes the specified characters {@code UTF} encoded.
   * This method writes no length information as {@link #writeUTF(String)} does.
   *
   * @since JaXLib 1.0
   */
    public final void writeCharsUTF(final CharSequence str, int offs, final int len) throws IOException {
        if (str instanceof String) writeCharsUTF((String) str, offs, len); else {
            CheckBounds.offset(str.length(), offs, len);
            ByteBuffer buffer = ensureOpen(len);
            int free = buffer.remaining();
            final int hi = offs + len;
            while (offs < hi) {
                final int c = str.charAt(offs++);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    if (free < 1) {
                        buffer = null;
                        buffer = ensureOpen(1);
                        free = buffer.remaining();
                    }
                    buffer.put((byte) c);
                    free--;
                } else if (c <= 0x07FF) {
                    if (free < 2) {
                        buffer = null;
                        buffer = ensureOpen(2);
                        free = buffer.remaining();
                    }
                    buffer.put((byte) (0xC0 | ((c >> 6) & 0x1F)));
                    buffer.put((byte) (0x80 | (c & 0x3F)));
                    free -= 2;
                } else {
                    if (free < 3) {
                        buffer = null;
                        buffer = ensureOpen(3);
                        free = buffer.remaining();
                    }
                    buffer.put((byte) (0xE0 | ((c >> 12) & 0x0F)));
                    buffer.put((byte) (0x80 | ((c >> 6) & 0x3F)));
                    buffer.put((byte) (0x80 | (c & 0x3F)));
                    free -= 3;
                }
            }
        }
    }

    /**
   * Writes the specified characters {@code UTF} encoded.
   * This method writes no length information as {@link #writeUTF(String)} does.
   *
   * @since JaXLib 1.0
   */
    public final void writeCharsUTF(final String str) throws IOException {
        writeCharsUTF(str, 0, str.length());
    }

    /**
   * Writes the specified characters {@code UTF} encoded.
   * This method writes no length information as {@link #writeUTF(String)} does.
   *
   * @since JaXLib 1.0
   */
    public final void writeCharsUTF(final String str, int offs, final int len) throws IOException {
        CheckBounds.offset(str, offs, len);
        final int hi = offs + len;
        ByteBuffer buffer = ensureOpen(len);
        byte[] buf = buffer.array();
        int pos = buffer.position();
        int lim = buffer.limit();
        while (offs < hi) {
            final int c = str.charAt(offs++);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                if (lim - pos < 1) {
                    buffer.position(pos);
                    buffer = null;
                    buf = null;
                    buffer = ensureOpen(1);
                    pos = buffer.position();
                    lim = buffer.limit();
                    buf = buffer.array();
                }
                buf[pos++] = (byte) c;
                continue;
            } else if (c <= 0x07FF) {
                if (lim - pos < 2) {
                    buffer.position(pos);
                    buffer = null;
                    buf = null;
                    buffer = ensureOpen(2);
                    pos = buffer.position();
                    lim = buffer.limit();
                    buf = buffer.array();
                }
                buf[pos++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
                continue;
            } else {
                if (lim - pos < 3) {
                    buffer.position(pos);
                    buffer = null;
                    buf = null;
                    buffer = ensureOpen(3);
                    pos = buffer.position();
                    lim = buffer.limit();
                    buf = buffer.array();
                }
                buf[pos++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                buf[pos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
                continue;
            }
        }
        buffer.position(pos);
    }

    @Override
    public final void writeUTF(final String str) throws IOException {
        final int strlen = str.length();
        int utflen = 0;
        for (int i = 0; i < strlen; i++) {
            final int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) utflen++; else if (c > 0x07FF) utflen += 3; else utflen += 2;
        }
        if (utflen > 65535) throw new UTFDataFormatException("UTF encoded string would exceed maximum length (65535).");
        writeShort(utflen);
        writeCharsUTF(str, 0, strlen);
    }

    public final void writeUTF(final String str, final int offs, final int len) throws IOException {
        CheckBounds.offset(str, offs, len);
        int utflen = 0;
        for (int i = offs, hi = offs + len; i < hi; i++) {
            final int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) utflen++; else if (c > 0x07FF) utflen += 3; else utflen += 2;
        }
        if (utflen > 65535) throw new UTFDataFormatException("UTF encoded string would exceed maximum length (65535).");
        writeShort(utflen);
        writeCharsUTF(str, offs, len);
    }

    public final void writeUTF(final CharSequence str) throws IOException {
        writeUTF(str, 0, str.length());
    }

    public final void writeUTF(final CharSequence str, final int offs, final int len) throws IOException {
        if (str instanceof String) {
            writeUTF((String) str, offs, len);
        } else {
            CheckBounds.offset(str.length(), offs, len);
            int utflen = 0;
            for (int i = offs, hi = offs + len; i < hi; i++) {
                final int c = str.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) utflen++; else if (c > 0x07FF) utflen += 3; else utflen += 2;
            }
            if (utflen > 65535) throw new UTFDataFormatException("UTF encoded string would exceed maximum length (65535).");
            writeShort(utflen);
            writeCharsUTF(str, offs, len);
        }
    }

    public final void writeUTF(final char[] str) throws IOException {
        writeUTF(str, 0, str.length);
    }

    public final void writeUTF(final char[] str, final int offs, final int len) throws IOException {
        CheckBounds.offset(str, offs, len);
        int utflen = 0;
        for (int i = offs, hi = offs + len; i < hi; i++) {
            final int c = str[i];
            if ((c >= 0x0001) && (c <= 0x007F)) utflen++; else if (c > 0x07FF) utflen += 3; else utflen += 2;
        }
        if (utflen > 65535) throw new UTFDataFormatException("UTF encoded string would exceed maximum length (65535).");
        writeShort(utflen);
        writeCharsUTF(str, offs, len);
    }
}
