package jaxlib.io.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.lang.Ints;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckBounds;

/**
 * An enhanced reimplementation of <tt>java.io.InputStreamReader</tt>.
 * <p>
 * <tt>InputStreamXReader</tt> produces the same result as
 * {@link java.io.InputStreamReader java.io.InputStreamReader}, but avoids thread synchronization, implements
 * some minor improvements to gain performance, and provides additional functionality. The performance gain
 * compared to <tt>java.io.InputStreamReader</tt> is between 10% and 20%, roughly speaking, compared against
 * <tt>InputStreamReader</tt> of <i>JDK 1.5</i> (HotSpot Client), using equal input stream,
 * decoder (<i>ISO-8859-1</i>) and buffering.
 * </p><p>
 * <i>Note:</i> You do not need to buffer the input stream a <tt>InputStreamXReader</tt> reads from.
 * Instead choose a custom internal input stream buffer capacity via {@link #setByteBufferCapacity(int)}.
 * The default capacity is <tt>8192</tt>.
 * </p><p>
 * <tt>InputStreamXReader</tt> uses by default nearly no buffering for decoded characters. Thus you may
 * consider to wrap a buffering reader around the <tt>InputStreamXReader</tt>. Alternatively you can control
 * the size of the internal character buffer via {@link #setCharBufferCapacity(int)}. Latter is recommended
 * if you have no need for additional functionality provided by the wrapping solution.
 * </p><p>
 * You can reuse an <tt>InputStreamXReader</tt> for multiple input sources by calling the
 * {@link #clearBuffers()} method and changing the input source by using an <tt>InputStream</tt>
 * implementation which allows that.
 * </p><p>
 * The memory for the buffers gets allocated when the first read operation occurs.
 * </p><p>
 * If the underlying stream of an <tt>InputStreamXReader</tt> is an instanceof {@link ByteBufferInputStream}
 * with an array buffer, then the reader will not use an internal bytebuffer. Instead it will access the
 * byte array of the <tt>ByteBufferInputStream</tt> directly. This improves performance dramatically when
 * decoding in-memory sources.
 * </p><p>
 * <tt>InputStreamXReader</tt> does not support the {@link #mark(int) mark} operation.
 * </p><p>
 * This class is <b>not</b> threadsafe.
 * If you need a threadsafe stream you may use {@link jaxlib.io.stream.concurrent.SynchronizedXReader}.
 * </p>
 *
 * @see ByteChannelWriter
 * @see BufferedXReader
 * @see OutputStreamXWriter
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: InputStreamXReader.java 3042 2012-01-30 12:56:05Z joerg_wassmer $
 */
public class InputStreamXReader extends XReader {

    static final int DEFAULT_INPUT_BUFFER_CAPACITY = 8192;

    private static final int MIN_INPUT_BUFFER_CAPACITY = 16;

    private static final int DEFAULT_OUTPUT_BUFFER_CAPACITY = 32;

    /**
   * Used by transferTo()
   */
    private static void append(final Appendable out, final CharBuffer a, final int len) throws IOException {
        if (len == 1) {
            out.append(a.get());
        } else if (len != 0) {
            assert (a.arrayOffset() == 0) : a.arrayOffset();
            final int pos = a.position();
            if (out instanceof StringBuilder) ((StringBuilder) out).append(a.array(), pos, len); else if (out instanceof StringBuffer) ((StringBuffer) out).append(a.array(), pos, len); else if (out instanceof Writer) ((Writer) out).write(a.array(), pos, len); else {
                final int alim = a.limit();
                try {
                    a.limit(pos + len);
                    out.append(a);
                } finally {
                    a.limit(alim);
                }
            }
            a.position(pos + len);
        }
    }

    /**
   * Also used by ByteChannelReader.
   */
    static Charset charset(@Nullable final String encoding) throws UnsupportedEncodingException {
        try {
            if (encoding == null) return Charset.defaultCharset(); else return Charset.forName(encoding);
        } catch (final UnsupportedCharsetException ex) {
            throw (UnsupportedEncodingException) new UnsupportedEncodingException().initCause(ex);
        }
    }

    private static int chooseInputBufferCapacity(final int given) {
        if (given >= 0) return Math.max(MIN_INPUT_BUFFER_CAPACITY, given); else return DEFAULT_INPUT_BUFFER_CAPACITY;
    }

    /**
   * Also used by ByteChannelReader.
   */
    static CharsetDecoder decoder(final Charset cs) {
        if (cs == null) throw new NullPointerException("charset");
        final CharsetDecoder decoder = cs.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        return decoder;
    }

    private InputStream in;

    private ReadableByteChannel inChannel;

    private CharsetDecoder decoder;

    /**
   * Initialized by {@link #decode(CharBuffer)}.
   */
    private ByteBuffer inputBuffer;

    private int inputBufferCapacity = chooseInputBufferCapacity(-1);

    /**
   * Initialized by {@link #ensureOpen()}.
   */
    private CharBuffer outputBuffer;

    private int outputBufferCapacity = DEFAULT_OUTPUT_BUFFER_CAPACITY;

    private final int maxCharsPerByte;

    /**
   * Creates a new reader using the platform's default encoding.
   *
   * @param in the input stream to decode.
   *
   * @throws NullPointerException if <code>in == null</code>.
   *
   * @see Charset#defaultCharset()
   *
   * @since JaXLib 1.0
   */
    public InputStreamXReader(final InputStream in) {
        this(in, Charset.defaultCharset());
    }

    /**
   * Creates a new reader using the specified encoding.
   *
   * @param in        the input stream to decode.
   * @param encoding  the encoding to use, or <tt>null</tt> to use the platform's default encoding.
   *
   * @throws UnsupportedEncodingException if there is no charset with specified name.
   * @throws NullPointerException         if <code>in == null</code>.
   *
   * @see Charset#forName(String)
   *
   * @since JaXLib 1.0
   */
    public InputStreamXReader(final InputStream in, final String encoding) throws UnsupportedEncodingException {
        this(in, charset(encoding));
    }

    /**
   * Creates a new reader using the specified charset.
   *
   * @param in        the input stream to decode.
   * @param charset   the charset to use for decoding.
   *
   * @throws NullPointerException  if <code>(in == null) || (charset == null)</code>.
   *
   * @see Charset#forName(String)
   *
   * @since JaXLib 1.0
   */
    public InputStreamXReader(final InputStream in, final Charset charset) {
        this(in, decoder(charset));
    }

    /**
   * Creates a new reader using the specified charset decoder.
   * You have to ensure that the decoder is used by the new reader exlusively while the reader is in use.
   * The new reader does not {@link CharsetDecoder#reset() reset} the decoder before using it.
   *
   * @param in        the input stream to decode.
   * @param decoder   the decoder to use.
   *
   * @throws NullPointerException  if <code>(in == null) || (decoder == null)</code>.
   *
   * @since JaXLib 1.0
   */
    public InputStreamXReader(final InputStream in, final CharsetDecoder decoder) {
        super();
        if (in == null) throw new NullPointerException("in");
        if (decoder == null) throw new NullPointerException("decoder");
        this.in = in;
        this.decoder = decoder;
        this.maxCharsPerByte = Ints.max(2, (int) Math.ceil(decoder.maxCharsPerByte()), decoder.replacement().length());
        setCharBufferCapacity0(DEFAULT_OUTPUT_BUFFER_CAPACITY);
        if (in instanceof FileInputStream) this.inChannel = ((FileInputStream) in).getChannel(); else if (in instanceof ReadableByteChannel) this.inChannel = (ReadableByteChannel) in;
    }

    /**
   * Creates a new reader using the platform's default encoding.
   * <p>
   * The new reader will use an input byte buffer equal to the size the file but not greater than
   * <tt>8192</tt>.
   * </p>
   *
   * @param in the file to decode.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if <code>in == null</code>.
   *
   * @see Charset#defaultCharset()
   *
   * @since JaXLib 1.0
   */
    public InputStreamXReader(final File in) throws IOException {
        this(in, Charset.defaultCharset());
    }

    /**
   * Creates a new reader using the specified encoding.
   * <p>
   * The new reader will use an input byte buffer equal to the size the file but not greater than
   * <tt>8192</tt>.
   * </p>
   *
   * @param in        the file to decode.
   * @param encoding  the encoding to use, or <tt>null</tt> to use the platform's default encoding.
   *
   * @throws UnsupportedEncodingException if there is no charset with specified name.
   * @throws IOException                  if an I/O error occurs.
   * @throws NullPointerException         if <code>in == null</code>.
   *
   * @see Charset#forName(String)
   *
   * @since JaXLib 1.0
   */
    public InputStreamXReader(final File in, final String encoding) throws IOException, UnsupportedEncodingException {
        this(in, charset(encoding));
    }

    /**
   * Creates a new reader using the specified charset.
   * <p>
   * The new reader will use an input byte buffer equal to the size the file but not greater than
   * <tt>8192</tt>.
   * </p>
   *
   * @param in        the file to decode.
   * @param charset   the charset to use for decoding.
   *
   * @throws NullPointerException  if <code>(in == null) || (charset == null)</code>.
   *
   * @see Charset#forName(String)
   *
   * @since JaXLib 1.0
   */
    public InputStreamXReader(final File in, final Charset charset) throws IOException {
        this(in, decoder(charset));
    }

    /**
   * Creates a new reader using the specified charset decoder.
   * You have to ensure that the decoder is used by the new reader exlusively while the reader is in use.
   * The new reader does not {@link CharsetDecoder#reset() reset} the decoder before using it.
   * <p>
   * The new reader will use an input byte buffer equal to the size the file but not greater than
   * <tt>8192</tt>.
   * </p>
   *
   * @param in        the file to decode.
   * @param decoder   the decoder to use.
   *
   * @throws NullPointerException  if <code>(in == null) || (decoder == null)</code>.
   *
   * @since JaXLib 1.0
   */
    public InputStreamXReader(final File in, final CharsetDecoder decoder) throws IOException {
        this(new FileInputStream(in).getChannel(), decoder);
        setByteBufferCapacity0((int) Math.min(in.length(), InputStreamXReader.DEFAULT_INPUT_BUFFER_CAPACITY));
    }

    /**
   * Package private constructor accessed by ByteChannelXReader.
   */
    InputStreamXReader(final ReadableByteChannel in, final CharsetDecoder decoder) {
        super();
        if (in == null) throw new NullPointerException("in");
        if (decoder == null) throw new NullPointerException("decoder");
        this.inChannel = in;
        this.decoder = decoder;
        this.maxCharsPerByte = Ints.max(2, (int) Math.ceil(decoder.maxCharsPerByte()), decoder.replacement().length());
        setCharBufferCapacity0(DEFAULT_OUTPUT_BUFFER_CAPACITY);
    }

    private int decode(final CharBuffer cb) throws IOException {
        final int startPos = cb.position();
        final InputStream in = this.in;
        final CharsetDecoder decoder = this.decoder;
        ByteBuffer bb = this.inputBuffer;
        boolean bbFixed = false;
        if (bb == null) {
            if (in instanceof ByteBufferInputStream) {
                bbFixed = true;
                final ByteBufferInputStream bbin = (ByteBufferInputStream) in;
                bb = bbin.ensureOpen();
                if (bb.isDirect()) {
                    this.inputBuffer = bb = ByteBuffer.allocate(this.inputBufferCapacity);
                    bb.flip();
                }
            } else {
                this.inputBuffer = bb = ByteBuffer.allocate(this.inputBufferCapacity);
                bb.flip();
            }
        }
        boolean eof = false;
        while (true) {
            final CoderResult cr = decoder.decode(bb, cb, eof);
            if (cr.isUnderflow()) {
                if (eof || !cb.hasRemaining() || ((cb.position() > startPos) && ((in != null) && (in.available() <= 0)))) break; else if (!bbFixed && (readBytes(bb) >= 0)) continue; else {
                    eof = true;
                    if ((cb.position() == startPos) && (!bb.hasRemaining())) break; else {
                        decoder.reset();
                        continue;
                    }
                }
            } else if (cr.isOverflow()) {
                assert cb.position() > startPos;
                break;
            } else {
                cr.throwException();
                throw new AssertionError();
            }
        }
        final int n = cb.position() - startPos;
        if (eof) {
            decoder.reset();
            return (n != 0) ? n : -1;
        } else {
            assert (n != 0);
            return n;
        }
    }

    @Nonnull
    private CharBuffer ensureOpen() throws IOException {
        CharBuffer cb = this.outputBuffer;
        if (cb == null) {
            if (this.decoder == null) throw new IOException("closed");
            this.outputBuffer = cb = CharBuffer.allocate(this.outputBufferCapacity);
            cb.flip();
        }
        return cb;
    }

    private void fillCharBuffer(final CharBuffer cb) throws IOException {
        assert !cb.hasRemaining();
        cb.clear();
        decode(cb);
        cb.flip();
    }

    private int readBytes(final ByteBuffer bb) throws IOException {
        bb.compact();
        try {
            final ReadableByteChannel inChannel = this.inChannel;
            if (inChannel == null) {
                final InputStream in = this.in;
                final int pos = bb.position();
                final int rem = bb.limit() - pos;
                final int n = in.read(bb.array(), bb.arrayOffset() + pos, rem);
                if ((n > 0) && (n <= rem)) bb.position(pos + n); else if (n < 0) return -1; else throw new ReturnValueException(in, "read(char[],int,int)", n, "> 0 && <=", rem);
            } else {
                final int step = inChannel.read(bb);
                if (step < 0) return -1; else if (step == 0) throw new IllegalBlockingModeException();
            }
        } finally {
            bb.flip();
        }
        assert (bb.remaining() != 0) : bb.remaining();
        return bb.remaining();
    }

    /**
   * Clears both, the internal input bytebuffer and the charbuffer, and resets the chardecoder.
   * The next read operation will behave like it would be the first one.
   * <p>
   * This method does not release any system resources.
   * </p>
   *
   * @throws IOException if this reader has been closed.
   *
   * @see CharsetDecoder#reset()
   *
   * @since JaXLib 1.0
   */
    public void clearBuffers() throws IOException {
        if (this.decoder == null) throw new IOException("closed");
        if (this.inputBuffer != null) this.inputBuffer.position(this.inputBuffer.limit());
        if (this.outputBuffer != null) this.outputBuffer.position(this.outputBuffer.limit());
        this.decoder.reset();
    }

    /**
   * Returns the capacity of the input bytebuffer.
   *
   * @throws IOException if this stream is closed.
   *
   * @see #setByteBufferCapacity(int)
   *
   * @since JaXLib 1.0
   */
    public int getByteBufferCapacity() throws IOException {
        if (this.decoder == null) throw new IOException("closed");
        return this.inputBufferCapacity;
    }

    /**
   * Returns the capacity of the internal charbuffer.
   *
   * @throws IOException if this stream is closed.
   *
   * @see #setCharBufferCapacity(int)
   *
   * @since JaXLib 1.0
   */
    public int getCharBufferCapacity() throws IOException {
        if (this.decoder == null) throw new IOException("closed");
        return this.outputBufferCapacity;
    }

    /**
   * Returns the charset used for decoding the input stream.
   *
   * @return the non-null charset.
   *
   * @throws IOException if this stream is closed.
   *
   * @since JaXLib 1.0
   */
    public Charset getCharset() throws IOException {
        final CharsetDecoder decoder = this.decoder;
        if (decoder == null) throw new IOException("closed");
        return decoder.charset();
    }

    /**
   * Sets the capacity of the input bytebuffer.
   * The <tt>InputStreamXReader</tt> may choose a slightly different value. The capacity will not
   * be changed if there currently are more characters in the buffer than specified.
   * <p>
   * The default bytebuffer capacity is <tt>8192</tt>.
   * </p>
   *
   * @throws IOException              if this reader has been closed.
   * @throws IllegalArgumentException if <code>byteBufferCapacity &lt; -1</code>.
   *
   * @since JaXLib 1.0
   */
    public void setByteBufferCapacity(final int byteBufferCapacity) throws IOException {
        if (this.decoder == null) throw new IOException("closed");
        setByteBufferCapacity0(byteBufferCapacity);
    }

    private void setByteBufferCapacity0(int byteBufferCapacity) throws IOException {
        if (byteBufferCapacity < -1) throw new IllegalArgumentException("byteBufferCapacity(" + byteBufferCapacity + ") < -1");
        if (byteBufferCapacity != this.inputBufferCapacity) {
            byteBufferCapacity = chooseInputBufferCapacity(byteBufferCapacity);
            if (byteBufferCapacity != this.inputBufferCapacity) {
                ByteBuffer bb = this.inputBuffer;
                if (bb != null) {
                    byteBufferCapacity = Math.max(bb.remaining(), byteBufferCapacity);
                    if (byteBufferCapacity != this.inputBufferCapacity) {
                        if (!bb.hasRemaining()) this.inputBuffer = bb = null; else {
                            final ByteBuffer nb = ByteBuffer.allocate(byteBufferCapacity);
                            nb.put(bb);
                            bb = null;
                            nb.flip();
                            this.inputBuffer = nb;
                        }
                    }
                }
                this.inputBufferCapacity = byteBufferCapacity;
            }
        }
    }

    /**
   * Sets the capacity of the internal character buffer.
   * The <tt>InputStreamXReader</tt> may choose a slightly different value. The capacity will not
   * be changed if there currently are more characters in the buffer than specified.
   * <p>
   * The default charbuffer capacity is <tt>32</tt>.
   * </p>
   *
   * @throws IOException              if this reader has been closed.
   * @throws IllegalArgumentException if <code>charBufferCapacity &lt; -1</code>.
   *
   * @since JaXLib 1.0
   */
    public void setCharBufferCapacity(int charBufferCapacity) throws IOException {
        if (this.decoder == null) throw new IOException("closed");
        setCharBufferCapacity0(charBufferCapacity);
    }

    private void setCharBufferCapacity0(int charBufferCapacity) {
        if (charBufferCapacity < -1) throw new IllegalArgumentException("charBufferCapacity(" + charBufferCapacity + ") < -1");
        if (charBufferCapacity != this.outputBufferCapacity) {
            charBufferCapacity = Math.max(this.maxCharsPerByte, charBufferCapacity);
            charBufferCapacity += charBufferCapacity % this.maxCharsPerByte;
            if (charBufferCapacity != this.outputBufferCapacity) {
                CharBuffer cb = this.outputBuffer;
                if (cb != null) {
                    charBufferCapacity = Math.max(cb.remaining(), charBufferCapacity);
                    if (charBufferCapacity != this.outputBufferCapacity) {
                        if (!cb.hasRemaining()) this.outputBuffer = cb = null; else {
                            final CharBuffer nb = CharBuffer.allocate(charBufferCapacity);
                            nb.put(cb);
                            cb = null;
                            nb.flip();
                            this.outputBuffer = nb;
                        }
                    }
                }
                this.outputBufferCapacity = charBufferCapacity;
            }
        }
    }

    @Override
    public void close() throws IOException {
        final InputStream in = this.in;
        final ReadableByteChannel inChannel = this.inChannel;
        if ((in != null) || (inChannel != null)) {
            closeInstance();
            if (inChannel == null) in.close(); else inChannel.close();
        }
    }

    @Override
    public void closeInstance() throws IOException {
        this.decoder = null;
        this.in = null;
        this.inChannel = null;
        this.inputBuffer = null;
        this.outputBuffer = null;
    }

    @Override
    public final boolean isOpen() {
        return this.in != null;
    }

    @Override
    public final int read() throws IOException, CharacterCodingException {
        final CharBuffer cb = ensureOpen();
        if (cb.hasRemaining()) return cb.get(); else {
            fillCharBuffer(cb);
            if (cb.hasRemaining()) return cb.get(); else return -1;
        }
    }

    @Override
    public final int read(final char[] dest, final int offs, final int len) throws IOException, CharacterCodingException {
        final CharBuffer cb = ensureOpen();
        CheckBounds.offset(dest, offs, len);
        if (len == 0) return 0;
        int readed = 0;
        if (cb.hasRemaining()) {
            readed = Math.min(cb.remaining(), len);
            cb.get(dest, offs, readed);
        }
        if (readed == len) {
        } else if (len - readed <= this.outputBufferCapacity) {
            fillCharBuffer(cb);
            if (cb.hasRemaining()) {
                final int step = Math.min(cb.remaining(), len - readed);
                cb.get(dest, offs + readed, step);
                readed += step;
            }
        } else {
            final int step = decode(CharBuffer.wrap(dest, offs + readed, len - readed));
            if (step >= 0) readed += step;
        }
        return (readed > 0) ? readed : -1;
    }

    @Override
    public final int read(final CharBuffer dest) throws IOException, CharacterCodingException {
        final CharBuffer cb = ensureOpen();
        int dr = dest.remaining();
        if (dr == 0) return 0;
        CheckArg.writable(dest);
        final int startPos = dest.position();
        int cr = cb.remaining();
        if (cr > 0) {
            final int step = Math.min(cr, dr);
            final int lim = cb.limit();
            cb.limit(cb.position() + step);
            dest.put(cb);
            cb.limit(lim);
            dr -= step;
        }
        if (dr == 0) {
        } else if ((dr == 1) || (dr < this.outputBufferCapacity >> 1)) {
            fillCharBuffer(cb);
            cr = cb.remaining();
            if (cr > 0) {
                final int step = Math.min(cr, dr);
                final int lim = cb.limit();
                cb.limit(cb.position() + step);
                dest.put(cb);
                cb.limit(lim);
            }
        } else {
            decode(dest);
        }
        final int pos = dest.position();
        return (startPos == pos) ? -1 : (pos - startPos);
    }

    @Override
    public boolean ready() throws IOException {
        final CharBuffer cb = this.outputBuffer;
        if ((cb != null) && cb.hasRemaining()) return true;
        final ByteBuffer bb = this.inputBuffer;
        if ((bb != null) && bb.hasRemaining()) return true;
        final InputStream in = this.in;
        if (in != null) {
            if (in.available() > 0) return true;
        } else {
            final ReadableByteChannel ch = this.inChannel;
            if (ch instanceof FileChannel) {
                final FileChannel fch = (FileChannel) ch;
                if (fch.position() < fch.size()) return true;
            }
        }
        return false;
    }

    @Override
    public long transferTo(final Appendable out, final long maxCount) throws IOException, CharacterCodingException {
        final CharBuffer cb = ensureOpen();
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        long count = 0;
        while ((count < maxCount) || (maxCount < 0)) {
            int br = cb.remaining();
            if (br == 0) {
                fillCharBuffer(cb);
                br = cb.remaining();
                if (br == 0) break;
            }
            final int step = (maxCount < 0) ? br : Math.min(br, (int) (maxCount - count));
            append(out, cb, step);
            count += step;
        }
        return count;
    }
}
