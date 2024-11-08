package jaxlib.io.stream;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import jaxlib.closure.tchar.ClosureSupportCharSequence;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.lang.ReturnValueException;
import jaxlib.text.SimpleIntegerFormat;
import jaxlib.util.CheckBounds;

/**
 * A buffered writer.
 * <p>
 * This class is <b>not</b> threadsafe.
 * If you need a threadsafe stream you may use {@link jaxlib.io.stream.concurrent.SynchronizedXWriter}.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: BufferedXWriter.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class BufferedXWriter extends XWriter {

    private CharBuffer buffer;

    private int capacity;

    private Writer out;

    private XWriter xout;

    /**
   * Creates a new <tt>BufferedXWriter</tt> using an initial buffer capacity of <tt>8192</tt>.
   * <p>
   * The buffer will be allocated when the first write operation occurs.
   * </p>
   *
   * @throws NullPointerException if <code>out == null</code>.
   *
   * @since JaXLib 1.0
   */
    public BufferedXWriter(Writer out) {
        this(out, 8192);
    }

    /**
   * Creates a new <tt>BufferedXWriter</tt> using specified initial buffer capacity.
   * <p>
   * The buffer will be allocated when the first write operation occurs.
   * </p>
   *
   * @throws IllegalArgumentException if <code>initialBufferCapacity < 0</code>.
   * @throws NullPointerException     if <code>out == null</code>.
   *
   * @since JaXLib 1.0
   */
    public BufferedXWriter(Writer out, int initialBufferCapacity) {
        super();
        if (out == null) throw new NullPointerException("out");
        if (initialBufferCapacity < 0) throw new IllegalArgumentException("initialBufferCapacity(" + initialBufferCapacity + ") < 0.");
        this.capacity = initialBufferCapacity;
        this.out = out;
        if (out instanceof XWriter) this.xout = (XWriter) out;
    }

    /**
   * Creates a new <tt>BufferedXOutputStream</tt> which uses the specified array for buffering.
   * <p>
   * The array will be used as long as its length fits the needs for requested operations.
   * </p>
   *
   * @throws NullPointerException  if <code>(out == null) || (initialBuffer == null)</code>.
   *
   * @since JaXLib 1.0
   */
    public BufferedXWriter(Writer out, char[] initialBuffer) {
        super();
        if (out == null) throw new NullPointerException("out");
        if (initialBuffer == null) throw new NullPointerException("initialBuffer");
        this.out = out;
        if (out instanceof XWriter) this.xout = (XWriter) out;
        this.buffer = CharBuffer.wrap(initialBuffer);
        this.capacity = initialBuffer.length;
    }

    private CharBuffer ensureOpen(int minFree) throws IOException {
        CharBuffer buffer = this.buffer;
        if (buffer == null) {
            if (this.out == null) throw new ClosedChannelException(); else {
                this.capacity = Math.max(minFree, this.capacity);
                this.buffer = buffer = CharBuffer.allocate(this.capacity);
            }
        }
        if ((minFree > 0) && (buffer.remaining() < minFree)) {
            flushBuffer(buffer);
            if (this.capacity < minFree) {
                this.buffer = buffer = CharBuffer.allocate(minFree);
                this.capacity = minFree;
            }
        }
        return buffer;
    }

    /**
   * Called by XWriter.writeSecurely()
   */
    final void writeSecurelyImpl(char[] source, int off, int len) throws IOException {
        assert !((source == null) || ((off < 0) || (off > source.length) || (len < 0) || ((off + len) > source.length) || ((off + len) < 0)));
        CharBuffer buffer = ensureOpen(-1);
        if (buffer.remaining() < len) {
            int bufferSize = buffer.capacity();
            if (bufferSize >= len) {
                flushBuffer(buffer);
                buffer.put(source, off, len);
            } else {
                while (len > 0) {
                    flushBuffer(buffer);
                    int step = Math.min(len, bufferSize);
                    buffer.put(source, off, step);
                    off += step;
                    len -= step;
                }
            }
        } else buffer.put(source, off, len);
    }

    /**
   * Returns the actual buffer.
   * <p>
   * The returned buffer instance is identical to the instance used internally by this stream. The buffered
   * characters are located between index zero and the actual position of the buffer. Changes to the buffer
   * are visible to the stream.
   * </p>
   *
   * @return the actual buffer, may be <code>null</code>.
   *
   * @since JaXLib 1.0
   */
    public CharBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public final BufferedXWriter append(char c) throws IOException {
        ensureOpen(1).put(c);
        return this;
    }

    @Override
    public void close() throws IOException {
        Writer out = this.out;
        if (out != null) {
            IOException ex = null;
            try {
                closeInstance();
            } catch (final IOException sex) {
                ex = sex;
            } finally {
                try {
                    out.close();
                } catch (final IOException sex) {
                    if (ex == null) ex = sex;
                }
                if (ex != null) throw ex;
            }
        }
    }

    @Override
    public void closeInstance() throws IOException {
        try {
            flushBuffer();
        } finally {
            this.buffer = null;
            this.out = null;
            this.xout = null;
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        this.out.flush();
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
    public final void flushBuffer() throws IOException {
        if (this.buffer != null) flushBuffer(this.buffer); else if (this.out == null) throw new ClosedChannelException();
    }

    private void flushBuffer(CharBuffer buffer) throws IOException {
        buffer.flip();
        if (this.xout == null) this.out.write(buffer.array(), 0, buffer.position()); else this.xout.write(buffer);
        buffer.clear();
    }

    @Override
    public final boolean isOpen() {
        return this.out != null;
    }

    @Override
    public final void write(int c) throws IOException {
        ensureOpen(1).put((char) c);
    }

    @Override
    public final void write(char[] source, int off, int len) throws IOException {
        CharBuffer buffer = ensureOpen(-1);
        if (buffer.remaining() < len) {
            flushBuffer(buffer);
            if (buffer.remaining() >= len) buffer.put(source, off, len); else this.out.write(source, off, len);
        } else buffer.put(source, off, len);
    }

    @Override
    public final int write(CharBuffer source) throws IOException {
        final CharBuffer buffer = ensureOpen(-1);
        final int count = source.remaining();
        final int r = buffer.remaining();
        if (r < count) {
            int bufferSize = buffer.capacity();
            if (bufferSize > count) {
                flushBuffer(buffer);
                buffer.put(source);
                return count;
            } else if (this.xout != null) {
                flushBuffer(buffer);
                return this.xout.write(source);
            } else if (source.hasArray()) {
                flushBuffer(buffer);
                this.out.write(source.array(), source.arrayOffset() + source.position(), count);
                source.position(source.limit());
                return count;
            } else {
                flushBuffer(buffer);
                this.out.append(source);
                source.position(source.limit());
                return count;
            }
        } else {
            buffer.put(source);
            return count;
        }
    }

    @Override
    public final void write(final String s, int off, final int len) throws IOException {
        CheckBounds.offset(s, off, len);
        final CharBuffer buffer = ensureOpen(-1);
        if (len == 0) return;
        int rem = len;
        while (true) {
            int step = Math.min(buffer.remaining(), rem);
            int pos = buffer.position();
            s.getChars(off, off + step, buffer.array(), pos);
            buffer.position(pos + step);
            rem -= step;
            if (rem > 0) {
                flushBuffer(buffer);
                off += step;
            } else {
                break;
            }
        }
    }

    private void write(final StringBuffer s, int off, final int len) throws IOException {
        CheckBounds.offset(s.length(), off, len);
        final CharBuffer buffer = ensureOpen(-1);
        if (len == 0) return;
        int rem = len;
        while (true) {
            final int step = Math.min(buffer.remaining(), rem);
            final int pos = buffer.position();
            s.getChars(off, off + step, buffer.array(), pos);
            buffer.position(pos + step);
            rem -= step;
            if (rem > 0) {
                flushBuffer(buffer);
                off += step;
            } else {
                break;
            }
        }
    }

    private void write(final StringBuilder s, int off, final int len) throws IOException {
        CheckBounds.offset(s.length(), off, len);
        final CharBuffer buffer = ensureOpen(-1);
        if (len == 0) return;
        int rem = len;
        while (true) {
            final int step = Math.min(buffer.remaining(), rem);
            final int pos = buffer.position();
            s.getChars(off, off + step, buffer.array(), pos);
            buffer.position(pos + step);
            rem -= step;
            if (rem > 0) {
                flushBuffer(buffer);
                off += step;
            } else {
                break;
            }
        }
    }

    private void write(final ClosureSupportCharSequence s, int off, final int len) throws IOException {
        CheckBounds.offset(s.length(), off, len);
        final CharBuffer buffer = ensureOpen(-1);
        if (len == 0) return;
        int rem = len;
        while (rem > 0) {
            final int step = Math.min(buffer.remaining(), rem);
            final int pos = buffer.position();
            s.toArray(off, off + step, buffer.array(), pos);
            buffer.position(pos + step);
            rem -= step;
            off += step;
            if (rem > 0) flushBuffer(buffer);
        }
    }

    @Override
    public final BufferedXWriter print(boolean v) throws IOException {
        write(Boolean.toString(v));
        return this;
    }

    @Override
    public final BufferedXWriter print(char v) throws IOException {
        write(v);
        return this;
    }

    @Override
    public final BufferedXWriter print(double v) throws IOException {
        write(Double.toString(v));
        return this;
    }

    @Override
    public final BufferedXWriter print(float v) throws IOException {
        write(Float.toString(v));
        return this;
    }

    @Override
    public final BufferedXWriter print(int v) throws IOException {
        SimpleIntegerFormat f = SimpleIntegerFormat.DECIMAL;
        f.appendTo(v, ensureOpen(f.length(v)));
        return this;
    }

    @Override
    public final BufferedXWriter print(long v) throws IOException {
        SimpleIntegerFormat f = SimpleIntegerFormat.DECIMAL;
        f.appendTo(v, ensureOpen(f.length(v)));
        return this;
    }

    @Override
    public final BufferedXWriter print(int v, int radix) throws IOException {
        SimpleIntegerFormat f = SimpleIntegerFormat.getInstance(radix);
        f.appendTo(v, ensureOpen(f.length(v)));
        return this;
    }

    @Override
    public final BufferedXWriter print(long v, int radix) throws IOException {
        SimpleIntegerFormat f = SimpleIntegerFormat.getInstance(radix);
        f.appendTo(v, ensureOpen(f.length(v)));
        return this;
    }

    @Override
    public final BufferedXWriter print(CharSequence v, int off, int len) throws IOException {
        if (len > 8) {
            Class clazz = v.getClass();
            if (clazz == String.class) {
                write((String) v, off, len);
            } else if (clazz == StringBuffer.class) write((StringBuffer) v, off, len); else if (clazz == StringBuilder.class) write((StringBuilder) v, off, len); else if (v instanceof CharBuffer) {
                CheckBounds.offset(v.length(), off, len);
                CharBuffer b = (CharBuffer) v;
                if (b.hasArray()) write(b.array(), b.arrayOffset() + b.position() + off, len); else write(b.subSequence(off, off + len));
            } else if (v instanceof ClosureSupportCharSequence) write((ClosureSupportCharSequence) v, off, len); else printImpl(v, off, len);
        } else printImpl(v, off, len);
        return this;
    }

    private BufferedXWriter printImpl(CharSequence v, int off, int len) throws IOException {
        CheckBounds.offset(v.length(), off, len);
        CharBuffer buffer = ensureOpen(-1);
        boolean fullRange = (off == 0) && (len == v.length());
        if (!fullRange && (len < 32)) {
            int free = buffer.remaining();
            for (int hi = off + len; off < hi; off++) {
                buffer.put(v.charAt(off));
                if (--free == 0) {
                    flushBuffer(buffer);
                    free = buffer.remaining();
                }
            }
        } else if (buffer.remaining() < len) {
            int bufferSize = buffer.capacity();
            if (bufferSize >= len) {
                flushBuffer(buffer);
                if (fullRange) buffer.append(v); else buffer.append(v.subSequence(off, off + len));
            } else {
                while (len > 0) {
                    flushBuffer(buffer);
                    int step = Math.min(len, bufferSize);
                    buffer.append(v.subSequence(off, off + step));
                    off += step;
                    len -= step;
                }
            }
        } else {
            if (fullRange) buffer.append(v); else buffer.append(v.subSequence(off, off + len));
        }
        return this;
    }

    @Override
    public long transferFrom(Readable in, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        long count = 0;
        while ((count < maxCount) || (maxCount < 0)) {
            CharBuffer buffer = ensureOpen(1);
            int maxStep = Math.min(buffer.remaining(), (int) Math.max(0, maxCount - count));
            final int lim = buffer.limit();
            buffer.limit(buffer.position() + maxStep);
            try {
                int step = in.read(buffer);
                if (step < 0) break; else if (step <= maxStep) count += step; else throw new ReturnValueException(in, "read(CharBuffer)", step, "<=", maxStep);
            } finally {
                buffer.limit(lim);
            }
        }
        return count;
    }
}
