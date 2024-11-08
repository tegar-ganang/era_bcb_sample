package jaxlib.io.stream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.UnsupportedCharsetException;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.lang.Ints;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckBounds;

/**
 * An enhanced reimplementation of <tt>java.io.OutputStreamWriter</tt>.
 * <p>
 * <tt>OutputStreamXWriter</tt> produces the same result as
 * {@link java.io.OutputStreamWriter java.io.OutputStreamWriter}, but avoids thread synchronization,
 * implements some minor improvements to gain performance, and provides additional functionality.
 * The performance gain compared to <tt>java.io.OutputStreamWriter</tt> is between 10% and 20%, roughly
 * speaking, compared against <tt>OutputStreamWriter</tt> of <i>JDK 1.5</i> (HotSpot Client), using equal
 * output stream, encoder (<i>ISO-8859-1</i>) and buffering.
 * </p><p>
 * <i>Note:</i> You do not need to buffer the output stream an <tt>OutputStreamXWriter</tt> writes to.
 * Instead choose a custom internal output stream buffer capacity via {@link #setByteBufferCapacity(int)}.
 * The default capacity is <tt>8192</tt>.
 * </p><p>
 * <tt>OutputStreamXWriter</tt> uses by default nearly no buffering for characters pending to be encoded.
 * Thus you may consider to wrap a buffering writer around the <tt>OutputStreamXWriter</tt>. Alternatively
 * you can control the size of the internal character buffer via {@link #setCharBufferCapacity(int)}. Latter
 * is recommended if you have no need for additional functionality provided by the wrapping solution.
 * </p><p>
 * You can reuse an <tt>OutputStreamXWriter</tt> for multiple output files by calling
 * {@link #flush(boolean) flush(true)} followed by {@link #clearBuffers()} and changing the output stream by
 * using an underlying <tt>OutputStream</tt> implementation which allows that.
 * </p><p>
 * The memory for the buffers gets allocated when the first write operation occurs.
 * </p><p>
 * This class is <b>not</b> threadsafe.
 * If you need a threadsafe stream you may use {@link jaxlib.io.stream.concurrent.SynchronizedXWriter}.
 * </p>
 *
 * @see ByteChannelWriter
 * @see BufferedXWriter
 * @see InputStreamXReader
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: OutputStreamXWriter.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class OutputStreamXWriter extends XWriter {

    private static final int DEFAULT_INPUT_BUFFER_CAPACITY = 32;

    private static final int MIN_INPUT_BUFFER_CAPACITY = 2;

    static final int DEFAULT_OUTPUT_BUFFER_CAPACITY = 8192;

    /**
   * Also used by ByteChannelWriter.
   */
    static Charset charset(final String encoding) throws UnsupportedEncodingException {
        try {
            if (encoding == null) return Charset.defaultCharset(); else return Charset.forName(encoding);
        } catch (final UnsupportedCharsetException ex) {
            throw (UnsupportedEncodingException) new UnsupportedEncodingException().initCause(ex);
        }
    }

    /**
   * Also used by ByteChannelWriter.
   */
    static CharsetEncoder encoder(final Charset cs) {
        if (cs == null) throw new NullPointerException("charset");
        return cs.newEncoder();
    }

    private OutputStream out;

    private WritableByteChannel outChannel;

    private CharsetEncoder encoder;

    private CharBuffer inBuffer;

    private int inBufferCapacity = DEFAULT_INPUT_BUFFER_CAPACITY;

    private ByteBuffer outBuffer;

    private int outBufferCapacity = DEFAULT_OUTPUT_BUFFER_CAPACITY;

    private final int maxBytesPerChar;

    /**
   * Creates a new writer using the platform's default encoding.
   *
   * @param out the output stream to write encoded characters to.
   *
   * @throws NullPointerException if <code>out == null</code>.
   *
   * @see Charset#defaultCharset()
   *
   * @since JaXLib 1.0
   */
    public OutputStreamXWriter(final OutputStream out) {
        this(out, Charset.defaultCharset());
    }

    /**
   * Creates a new writer using the specified encoding.
   *
   * @param out       the output stream to write encoded characters to.
   * @param encoding  the encoding to use, or <tt>null</tt> to use the platform's default encoding.
   *
   * @throws UnsupportedEncodingException if there is no charset with specified name.
   * @throws NullPointerException         if <code>out == null</code>.
   *
   * @see Charset#forName(String)
   *
   * @since JaXLib 1.0
   */
    public OutputStreamXWriter(final OutputStream out, final String encoding) throws UnsupportedEncodingException {
        this(out, charset(encoding));
    }

    /**
   * Creates a new writer using the specified charset.
   *
   * @param out       the output stream to write encoded characters to.
   * @param charset   the charset to use for encoding.
   *
   * @throws NullPointerException  if <code>(out == null) || (charset == null)</code>.
   *
   * @see Charset#forName(String)
   *
   * @since JaXLib 1.0
   */
    public OutputStreamXWriter(final OutputStream out, final Charset charset) {
        this(out, encoder(charset));
    }

    /**
   * Creates a new writer using the specified encoder.
   * You have to ensure that the encoder is used by the new writer exlusively while the writer is in use.
   * The new writer does not {@link CharsetEncoder#reset() reset} the encoder before using it.
   *
   * @param out       the output stream to write encoded characters to.
   * @param encoder   the encoder instance to use.
   *
   * @throws NullPointerException  if <code>(out == null) || (encoder == null)</code>.
   *
   * @see Charset#newEncoder()
   *
   * @since JaXLib 1.0
   */
    public OutputStreamXWriter(final OutputStream out, final CharsetEncoder encoder) {
        super();
        if (out == null) throw new NullPointerException("out");
        if (encoder == null) throw new NullPointerException("encoder");
        this.out = out;
        this.encoder = encoder;
        this.maxBytesPerChar = Ints.max(2, (int) Math.ceil(encoder.maxBytesPerChar()), encoder.replacement().length);
        setByteBufferCapacity0(DEFAULT_OUTPUT_BUFFER_CAPACITY);
        if (out instanceof FileOutputStream) this.outChannel = ((FileOutputStream) out).getChannel(); else if (out instanceof WritableByteChannel) this.outChannel = (WritableByteChannel) out;
    }

    /**
   * Creates a new writer using the platform's default encoding.
   * <p>
   * The new writer will use an output byte buffer equal to the size the file but not greater than
   * <tt>8192</tt>.
   * </p>
   *
   * @param out the file to write encoded characters to.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if <code>out == null</code>.
   *
   * @see Charset#defaultCharset()
   *
   * @since JaXLib 1.0
   */
    public OutputStreamXWriter(final File out) throws IOException {
        this(out, Charset.defaultCharset());
    }

    /**
   * Creates a new writer using the specified encoding.
   * <p>
   * The new writer will use an output byte buffer equal to the size the file but not greater than
   * <tt>8192</tt>.
   * </p>
   *
   * @param out       the file to write encoded characters to.
   * @param encoding  the encoding to use, or <tt>null</tt> to use the platform's default encoding.
   *
   * @throws UnsupportedEncodingException if there is no charset with specified name.
   * @throws IOException                  if an I/O error occurs.
   * @throws NullPointerException         if <code>out == null</code>.
   *
   * @see Charset#forName(String)
   *
   * @since JaXLib 1.0
   */
    public OutputStreamXWriter(final File out, final String encoding) throws IOException, UnsupportedEncodingException {
        this(out, charset(encoding));
    }

    /**
   * Creates a new writer using the specified charset.
   * <p>
   * The new writer will use an output byte buffer equal to the size the file but not greater than
   * <tt>8192</tt>.
   * </p>
   *
   * @param out       the file to write encoded characters to.
   * @param charset   the charset to use for encoding.
   *
   * @throws NullPointerException  if <code>(out == null) || (charset == null)</code>.
   *
   * @see Charset#forName(String)
   *
   * @since JaXLib 1.0
   */
    public OutputStreamXWriter(final File out, final Charset charset) throws IOException {
        this(out, encoder(charset));
    }

    /**
   * Creates a new writer using the specified charset encoder.
   * You have to ensure that the encoder is used by the new writer exlusively while the writer is in use.
   * The new writer does not {@link CharsetEncoder#reset() reset} the encoder before using it.
   * <p>
   * The new writer will use an output byte buffer equal to the size the file but not greater than
   * <tt>8192</tt>.
   * </p>
   *
   * @param out       the file to write encoded characters to.
   * @param encoder   the encoder to use.
   *
   * @throws NullPointerException  if <code>(out == null) || (encoder == null)</code>.
   *
   * @see Charset#newEncoder()
   *
   * @since JaXLib 1.0
   */
    public OutputStreamXWriter(final File out, final CharsetEncoder encoder) throws IOException {
        this(new FileOutputStream(out).getChannel(), encoder);
        setByteBufferCapacity0((int) Math.min(out.length(), OutputStreamXWriter.DEFAULT_OUTPUT_BUFFER_CAPACITY));
    }

    /**
   * Package private constructor accessed by ByteChannelXWriter.
   */
    OutputStreamXWriter(final WritableByteChannel out, final CharsetEncoder encoder) {
        super();
        if (out == null) throw new NullPointerException("out");
        if (encoder == null) throw new NullPointerException("encoder");
        this.outChannel = out;
        this.encoder = encoder;
        this.maxBytesPerChar = Ints.max(2, (int) Math.ceil(encoder.maxBytesPerChar()), encoder.replacement().length);
        setByteBufferCapacity0(DEFAULT_OUTPUT_BUFFER_CAPACITY);
    }

    private void encode(final CharBuffer cb, final boolean eof) throws IOException {
        final ByteBuffer bb = outBuffer();
        final CharsetEncoder encoder = this.encoder;
        while (cb.hasRemaining()) {
            final CoderResult cr = encoder.encode(cb, bb, eof);
            if (cr.isUnderflow()) {
                if (eof) encoder.reset();
                return;
            } else if (cr.isOverflow()) {
                assert bb.position() > 0;
                writeBytes(bb, eof);
                continue;
            } else if (!eof && (cb.remaining() == 1)) {
                return;
            } else {
                cr.throwException();
                throw new AssertionError();
            }
        }
        throw new AssertionError();
    }

    private void ensureOpen() throws IOException {
        if (this.encoder == null) throw new IOException("closed");
    }

    private CharBuffer inBuffer() throws IOException {
        CharBuffer cb = this.inBuffer;
        if (cb == null) {
            ensureOpen();
            this.inBuffer = cb = CharBuffer.allocate(this.inBufferCapacity);
        }
        return cb;
    }

    private ByteBuffer outBuffer() throws IOException {
        ByteBuffer bb = this.outBuffer;
        if (bb == null) {
            ensureOpen();
            this.outBuffer = bb = ByteBuffer.allocate(this.outBufferCapacity);
        }
        return bb;
    }

    private void writeBytes(final ByteBuffer bb, final boolean fully) throws IOException {
        bb.flip();
        if (!bb.hasRemaining()) bb.clear(); else {
            final WritableByteChannel outChannel = this.outChannel;
            if (outChannel == null) {
                this.out.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
                bb.clear();
            } else {
                while (bb.hasRemaining() && (fully || (bb.position() < this.maxBytesPerChar))) {
                    final int step = outChannel.write(bb);
                    if (step <= 0) {
                        this.out.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
                        bb.clear();
                        return;
                    }
                }
                bb.compact();
            }
        }
    }

    /**
   * Clears both, the bytebuffer and the charbuffer, and resets the charencoder.
   * The next write operation will behave like it would be the first one.
   * <p>
   * This method does not release any system resources.
   * </p>
   *
   * @throws IOException if this stream has been closed.
   *
   * @see CharsetEncoder#reset()
   *
   * @since JaXLib 1.0
   */
    public void clearBuffers() throws IOException {
        if (this.encoder == null) throw new IOException("closed");
        if (this.inBuffer != null) this.inBuffer.clear();
        if (this.outBuffer != null) this.outBuffer.clear();
        this.encoder.reset();
    }

    /**
   * Returns the capacity of the output bytebuffer.
   *
   * @throws IOException if this stream is closed.
   *
   * @see #setByteBufferCapacity(int)
   *
   * @since JaXLib 1.0
   */
    public int getByteBufferCapacity() throws IOException {
        if (this.encoder == null) throw new IOException("closed");
        return this.outBufferCapacity;
    }

    /**
   * Returns the capacity of the input charbuffer.
   *
   * @throws IOException if this stream is closed.
   *
   * @see #setCharBufferCapacity(int)
   *
   * @since JaXLib 1.0
   */
    public int getCharBufferCapacity() throws IOException {
        if (this.encoder == null) throw new IOException("closed");
        return this.inBufferCapacity;
    }

    /**
   * Returns the charset used for encoding characters.
   *
   * @return the non-null charset.
   *
   * @throws IOException if this stream is closed.
   *
   * @since JaXLib 1.0
   */
    public Charset getCharset() throws IOException {
        final CharsetEncoder encoder = this.encoder;
        if (encoder == null) throw new IOException("closed");
        return encoder.charset();
    }

    /**
   * Sets the capacity of the output byte buffer.
   * The <tt>OutputStreamXWriter</tt> may choose a slightly different value. The capacity will not
   * be changed if there currently are more bytes in the buffer than specified.
   * <p>
   * The default byte buffer capacity is <tt>8192</tt>.
   * </p>
   *
   * @throws IOException
   *  if this writer has been closed.
   * @throws IllegalArgumentException
   *  if <code>byteBufferCapacity &lt; -1</code>.
   *
   * @since JaXLib 1.0
   */
    public void setByteBufferCapacity(final int byteBufferCapacity) throws IOException {
        if (this.encoder == null) throw new IOException("closed");
        setByteBufferCapacity0(byteBufferCapacity);
    }

    private void setByteBufferCapacity0(int byteBufferCapacity) {
        if (byteBufferCapacity < -1) throw new IllegalArgumentException("byteBufferCapacity(" + byteBufferCapacity + ") < -1");
        if (byteBufferCapacity != this.outBufferCapacity) {
            byteBufferCapacity = Math.max(this.maxBytesPerChar, byteBufferCapacity);
            byteBufferCapacity += byteBufferCapacity % this.maxBytesPerChar;
            if (byteBufferCapacity != this.outBufferCapacity) {
                ByteBuffer bb = this.outBuffer;
                if (bb != null) {
                    byteBufferCapacity = Math.max(bb.position(), byteBufferCapacity);
                    if (byteBufferCapacity != this.outBufferCapacity) {
                        if (bb.position() == 0) this.outBuffer = bb = null; else {
                            final ByteBuffer nb = ByteBuffer.allocate(byteBufferCapacity);
                            bb.flip();
                            nb.put(bb);
                            bb = null;
                            this.outBuffer = nb;
                        }
                    }
                }
                this.outBufferCapacity = byteBufferCapacity;
            }
        }
    }

    /**
   * Sets the capacity of the internal character buffer.
   * The <tt>OutputStreamXWriter</tt> may choose a slightly different value. The capacity will not
   * be changed if there currently are more characters in the buffer than specified.
   * <p>
   * The default charbuffer capacity is <tt>32</tt>.
   * </p>
   *
   * @throws IOException              if this writer has been closed.
   * @throws IllegalArgumentException if <code>charBufferCapacity &lt; -1</code>.
   *
   * @since JaXLib 1.0
   */
    public void setCharBufferCapacity(int charBufferCapacity) throws IOException {
        if (this.encoder == null) throw new IOException("closed");
        if (charBufferCapacity < -1) throw new IllegalArgumentException("charBufferCapacity(" + charBufferCapacity + ") < -1");
        if (charBufferCapacity != this.inBufferCapacity) {
            charBufferCapacity = Math.max(MIN_INPUT_BUFFER_CAPACITY, charBufferCapacity);
            if (charBufferCapacity != this.inBufferCapacity) {
                CharBuffer cb = this.inBuffer;
                if (cb != null) {
                    charBufferCapacity = Math.max(cb.remaining(), charBufferCapacity);
                    if (charBufferCapacity != this.inBufferCapacity) {
                        if (!cb.hasRemaining()) this.inBuffer = cb = null; else {
                            final CharBuffer nb = CharBuffer.allocate(charBufferCapacity);
                            nb.put(cb);
                            cb = null;
                            nb.flip();
                            this.inBuffer = nb;
                        }
                    }
                }
                this.inBufferCapacity = charBufferCapacity;
            }
        }
    }

    @Override
    public void close() throws IOException {
        final OutputStream out = this.out;
        final WritableByteChannel outChannel = this.outChannel;
        if ((out != null) || (outChannel != null)) {
            try {
                closeInstance();
            } finally {
                if (out == null) outChannel.close(); else out.close();
            }
        }
    }

    @Override
    public void closeInstance() throws IOException {
        if (this.encoder != null) {
            try {
                flush(true);
            } finally {
                this.encoder = null;
                this.inBuffer = null;
                this.out = null;
                this.outBuffer = null;
                this.outChannel = null;
            }
        }
    }

    /**
   * Same as {@link #flush(boolean) flush(false)}.
   * <p>
   * Please note this method may not flush all characters, because of the needs of character encoding.
   * For flushing all characters you have to call {@link #flush(boolean) flush(true)}.
   * </p>
   */
    @Override
    public void flush() throws IOException {
        flush(false, true);
    }

    public final boolean flush(final boolean eof) throws IOException {
        return flush(eof, true);
    }

    public boolean flush(final boolean eof, final boolean flushDelegate) throws IOException {
        boolean success = true;
        if (this.encoder != null) {
            final CharBuffer cb = this.inBuffer;
            if ((cb != null) && (cb.position() > 0)) {
                cb.flip();
                encode(cb, eof);
                cb.compact();
                success = !eof || !cb.hasRemaining();
            }
            final ByteBuffer bb = this.outBuffer;
            if ((bb != null) && (bb.position() > 0)) {
                writeBytes(bb, eof);
                success = success && (!eof || !bb.hasRemaining());
            }
            if (flushDelegate) {
                if (this.out != null) this.out.flush(); else if (this.outChannel instanceof Flushable) ((Flushable) this.outChannel).flush();
            }
        }
        return success;
    }

    @Override
    public final boolean isOpen() {
        return this.encoder != null;
    }

    @Override
    public OutputStreamXWriter print(final CharSequence src, int offs, final int len) throws IOException, CharacterCodingException {
        ensureOpen();
        CheckBounds.offset(src.length(), offs, len);
        if (len == 0) {
            return this;
        } else if (len <= 16) {
            for (int hi = offs + len; offs < hi; offs++) write(src.charAt(offs));
        } else if (src instanceof CharBuffer) {
            final CharBuffer b = ((CharBuffer) src).duplicate();
            b.limit(b.position() + offs + len);
            b.position(b.position() + offs);
            write(b);
        } else {
            write(CharBuffer.wrap(src, offs, offs + len));
        }
        return this;
    }

    @Override
    public final void write(final int c) throws IOException, CharacterCodingException {
        ensureOpen();
        final CharBuffer cb = inBuffer();
        if (!cb.hasRemaining()) {
            cb.flip();
            encode(cb, false);
            cb.compact();
        }
        cb.put((char) c);
    }

    @Override
    public final void write(final char[] src, int offs, final int len) throws IOException, CharacterCodingException {
        ensureOpen();
        CheckBounds.offset(src, offs, len);
        if (len == 0) {
            return;
        } else if ((len <= this.inBufferCapacity) && ((len < 16) || (this.inBuffer != null))) {
            final CharBuffer cb = inBuffer();
            for (int hi = offs + len; offs < hi; ) {
                final int step = Math.min(hi - offs, cb.remaining());
                cb.put(src, offs, step);
                offs += step;
                if (!cb.hasRemaining()) {
                    cb.flip();
                    encode(cb, false);
                    cb.compact();
                }
            }
        } else {
            write(CharBuffer.wrap(src, offs, len));
        }
    }

    @Override
    public final int write(final CharBuffer src) throws IOException, CharacterCodingException {
        ensureOpen();
        final int count = src.remaining();
        if (count == 0) return 0;
        final CharBuffer cb = this.inBuffer;
        if (cb != null) {
            while ((cb.position() > 0) && src.hasRemaining()) {
                final int lim = src.limit();
                src.limit(src.position() + Math.min(cb.remaining(), src.remaining()));
                try {
                    cb.put(src);
                } finally {
                    src.limit(lim);
                }
                cb.flip();
                encode(cb, false);
                cb.compact();
            }
        }
        if (src.hasRemaining()) {
            encode(src, false);
            if (src.hasRemaining()) {
                assert (src.remaining() == 1) : src.remaining();
                inBuffer().put(src);
            }
        }
        return count;
    }

    @Override
    public final void write(final String src, int offs, final int len) throws IOException, CharacterCodingException {
        ensureOpen();
        CheckBounds.offset(src, offs, len);
        if (len == 0) return;
        final CharBuffer cb = this.inBuffer;
        if ((cb != null) && (cb.remaining() >= len)) {
            cb.put(src, offs, offs + len);
        } else if (len <= 16) {
            final int hi = offs + len;
            while (offs < hi) write(src.charAt(offs++));
        } else {
            write(CharBuffer.wrap(src, offs, offs + len));
        }
    }

    @Override
    public long transferFrom(final Readable in, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        long count = 0;
        final CharBuffer cb = inBuffer();
        try {
            while (true) {
                if (!cb.hasRemaining()) {
                    cb.flip();
                    encode(cb, false);
                    cb.compact();
                }
                final int maxStep = (maxCount < 0) ? cb.remaining() : (int) Math.min(cb.remaining(), maxCount - count);
                cb.limit(cb.position() + maxStep);
                final int step = in.read(cb);
                if (step < 0) break; else if (step <= maxStep) count += step; else throw new ReturnValueException(in, "read(CharBuffer)", step, "<=", maxStep);
                if ((maxCount > 0) && (count >= maxCount)) break;
            }
        } finally {
            cb.limit(cb.capacity());
        }
        return count;
    }

    /**
   * Write a single byte bypassing the char encoder.
   * This call assumes that the previously begun text sequence is finished.
   *
   * @throws IOException
   *  if the call {@link #flush(boolean,boolean) flush(true, false)} failed or returned {@code false}.
   *
   * @since JaXLib 1.0
   */
    public final void writeRawByte(final int b) throws IOException {
        if (!flush(true, false)) throw new IOException("Char encoder wasn't able to finish the previously begun char sequence");
        outBuffer().put((byte) b);
    }
}
