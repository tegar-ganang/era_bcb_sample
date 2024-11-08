package jaxlib.arc.zip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipException;
import jaxlib.io.stream.XOutputStream;
import jaxlib.util.CheckArg;
import jaxlib.util.CheckBounds;

/**
 * An output stream which takes uncompressed input and writes it compressed to another stream.
 * <p>
 * In addition to {@link java.util.zip.DeflaterOutputStream} this stream implementation supports the special
 * {@link #setFlushMode(DeflaterFlushMode) flush modes} defined by {@code ZLib}. The output produced by
 * {@code DeflatedOutputStream} is compatible with {@link java.util.zip.InflaterInputStream}.
 * </p><p>
 * In difference to {@link java.util.zip.DeflaterOutputStream} this stream implementation is not using an
 * intermediate buffer when writing compressed blocks to the underlying stream. Instead it sends the internal
 * block byte buffer directly. Usually these blocks are one or more kilobytes in size.
 * </p><p>
 * When writing blocks of bytes to a {@code DeflatedOutputStream} then there is no need to put a buffering
 * stream around. However, for single-byte operations you should use at least a small buffer.
 * </p><p>
 * {@code DeflatedOutputStream} is not thread-safe. If multiple threads are accessing the same instance of
 * this class then access has to be synchronized externally.
 * </p><p>
 * <b>Note:</b> As of <i>JDK 1.7</i> the classes <i>java.util.zip.DeflaterOutputStream</i> and
 * <i>java.util.zip.GZIPOutputStream</i> are supporting <i>SYNC_FLUSH</i>. Thus you may have no need to use the
 * <i>jaxlib.arc.zip</i> API.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: DeflatedOutputStream.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class DeflatedOutputStream extends XOutputStream {

    private boolean autoFlush;

    private Deflater deflater;

    private DeflaterFlushMode flushMode;

    private OutputStream out;

    private byte[] singleByte;

    private long totalIn;

    private long totalOut;

    /**
   * Creates a new {@code DeflatedOutputStream} which writes deflated data to the specified stream using
   * the default compression level (6), the default window size level (6) and the default strategy.
   *
   * @param out
   *  the stream to write to.
   *
   * @throws NullPointerException
   *  if {@code out == null}.
   *
   * @since JaXLib 1.0
   */
    public DeflatedOutputStream(final OutputStream out) {
        this(out, new DeflaterProperties());
    }

    /**
   * Creates a new {@code DeflatedOutputStream} which writes deflated data to the specified stream using
   * the specified compression level and the default window size level (6) and the default strategy.
   *
   * @param out
   *  the stream to write to.
   * @param compressionLevel
   *  the compression level of the deflated data.
   *
   * @throws NullPointerException
   *  if {@code out == null}.
   * @throws IllegalArgumentException
   *  if {@code (level < -1) || (level > 9)}.
   *
   * @since JaXLib 1.0
   */
    public DeflatedOutputStream(final OutputStream out, final int compressionLevel) {
        this(out, new DeflaterProperties(compressionLevel));
    }

    /**
   * Creates a new {@code DeflatedOutputStream} which writes deflated data to the specified stream using
   * the specified compression level and strategy and the default window size level (6).
   *
   * @param out
   *  the stream to write to.
   * @param properties
   *  the configuration for the deflater engine.
   *
   * @throws NullPointerException
   *  if {@code (out == null) || (properties == null)}.
   *
   * @since JaXLib 1.0
   */
    public DeflatedOutputStream(final OutputStream out, final DeflaterProperties properties) {
        super();
        CheckArg.notNull(out, "out");
        CheckArg.notNull(properties, "properties");
        this.deflater = new Deflater(out, properties.getCompressionLevel(), 9 + properties.getWindowSizeLevel() - 1, 8, properties.getStrategy().id, properties.isNoWrap());
        this.flushMode = properties.getFlushMode();
        this.out = out;
    }

    private void autoFlush(final Deflater deflater, final OutputStream out) throws IOException {
        if (this.autoFlush) {
            final int flushMode = this.flushMode.id;
            if (flushMode != DeflaterFlushMode.Z_NO_FLUSH) flushIntern(deflater, flushMode);
            out.flush();
        }
    }

    private Deflater deflater() throws IOException {
        final Deflater deflater = this.deflater;
        if (deflater == null) throw new IOException("closed");
        return deflater;
    }

    private static void flushIntern(final Deflater deflater, final int flushMode) throws IOException {
        try {
            do {
                deflater.deflate(flushMode);
                deflater.flush();
            } while ((deflater.avail_in > 0) || (deflater.avail_out == 0) || (deflater.pending > 0));
        } catch (final RuntimeException ex) {
            throw (ZipException) new ZipException().initCause(ex);
        }
    }

    private OutputStream out() throws IOException {
        final OutputStream out = this.out;
        if (out != null) return out; else throw new IOException("closed");
    }

    protected void reinit(final OutputStream out) throws IOException {
        if (out == null) throw new NullPointerException("out");
        if (out == this) throw new IllegalArgumentException("out == this");
        reinit();
        this.out = out;
    }

    @Override
    @SuppressWarnings("finally")
    public void close() throws IOException {
        final OutputStream out = this.out;
        if (out != null) {
            try {
                closeInstance();
            } catch (final IOException ex) {
                try {
                    out.close();
                } finally {
                    throw ex;
                }
            } finally {
                this.out = null;
            }
            out.close();
        }
    }

    @Override
    public void closeInstance() throws IOException {
        final Deflater deflater = this.deflater;
        final OutputStream out = this.out;
        if ((deflater != null) && (out != null)) {
            this.deflater = null;
            this.out = null;
            this.singleByte = null;
            try {
                flushIntern(deflater, DeflaterFlushMode.Z_FINISH);
                out.flush();
            } finally {
                this.totalIn = deflater.total_in;
                this.totalOut = deflater.total_out;
            }
        }
    }

    /**
   * This implementation calls {@code flush(getFlushMode())}.
   *
   * @see #flush(DeflaterFlushMode)
   */
    @Override
    public final void flush() throws IOException {
        flush(this.flushMode);
    }

    /**
   * Flushed the stream applying the specified flush mode.
   * This call ignores the current flush mode set via {@link #setFlushMode(DeflaterFlushMode)}.
   *
   * @throws IOException
   *  if an I/O error occurs.
   *
   * @since JaXLib 1.0
   */
    public void flush(final DeflaterFlushMode flushMode) throws IOException {
        final OutputStream out = out();
        if (flushMode.id != DeflaterFlushMode.Z_NO_FLUSH) flushIntern(deflater(), flushMode.id);
        out.flush();
    }

    /**
   * Returns the total number of bytes output so far.
   *
   * @since JaXLib 1.0
   */
    public final long getCompressedSize() {
        final Deflater deflater = this.deflater;
        return (deflater != null) ? deflater.total_out : this.totalOut;
    }

    /**
   *
   * @since JaXLib 1.0
   */
    public final DeflaterFlushMode getFlushMode() {
        return this.flushMode;
    }

    /**
   * Returns the total number of bytes input so far.
   *
   * @since JaXLib 1.0
   */
    public final long getRawSize() {
        final Deflater deflater = this.deflater;
        return (deflater != null) ? deflater.total_in : this.totalIn;
    }

    public final boolean isAutoFlush() {
        return this.autoFlush;
    }

    @Override
    public boolean isOpen() {
        return this.out != null;
    }

    public void reinit() throws IOException {
        flushIntern(deflater(), DeflaterFlushMode.Z_FINISH);
        deflater().reset();
    }

    /**
   * If true and the flush mode of this stream is not set to {@code NONE} then this stream will write its
   * buffered bytes immediately to the underlying stream whenever some are available.
   *
   * @since JaXLib 1.0
   */
    public void setAutoFlush(final boolean b) {
        this.autoFlush = b;
    }

    /**
   *
   * @since JaXLib 1.0
   */
    public void setFlushMode(final DeflaterFlushMode flushMode) {
        CheckArg.notNull(flushMode, "flushMode");
        this.flushMode = flushMode;
    }

    @Override
    public long transferFrom(final InputStream in, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        if (maxCount < 0) maxCount = Long.MAX_VALUE;
        final Deflater deflater = deflater();
        final OutputStream out = out();
        deflater.next_in_stream = in;
        deflater.avail_in = maxCount;
        final long total_in = deflater.total_in;
        try {
            do {
                deflater.deflate(DeflaterFlushMode.Z_NO_FLUSH);
            } while ((deflater.avail_in > 0) || (deflater.avail_out == 0));
        } catch (final DeflaterException ex) {
            throw (ZipException) new ZipException(ex.getMessage()).initCause(ex);
        } finally {
            deflater.avail_in = 0;
            deflater.next_in_stream = null;
        }
        autoFlush(deflater, out);
        return deflater.total_in - total_in;
    }

    @Override
    public final void write(final int b) throws IOException {
        byte[] singleByte = this.singleByte;
        if (singleByte == null) {
            deflater();
            this.singleByte = singleByte = new byte[1];
        }
        singleByte[0] = (byte) b;
        write(singleByte, 0, 1);
    }

    @Override
    public final void write(final byte[] src) throws IOException {
        write(src, 0, src.length);
    }

    @Override
    public final void write(final byte[] src, final int off, final int len) throws IOException {
        CheckBounds.offset(src, off, len);
        if (len > 0) {
            final Deflater deflater = deflater();
            final OutputStream out = out();
            deflater.next_in_array = src;
            deflater.next_in_index = off;
            deflater.avail_in = len;
            try {
                do {
                    deflater.deflate(DeflaterFlushMode.Z_NO_FLUSH);
                } while ((deflater.avail_in > 0) || (deflater.avail_out == 0));
            } catch (final DeflaterException ex) {
                throw (ZipException) new ZipException(ex.getMessage()).initCause(ex);
            } finally {
                deflater.avail_in = 0;
                deflater.next_in_array = null;
            }
            autoFlush(deflater, out);
        }
    }

    @Override
    public final int write(final ByteBuffer src) throws IOException {
        final int available = src.remaining();
        if (available <= 0) return 0;
        final Deflater deflater = deflater();
        final OutputStream out = out();
        deflater.next_in_buffer = src;
        deflater.avail_in = available;
        try {
            do {
                deflater.deflate(DeflaterFlushMode.Z_NO_FLUSH);
            } while ((deflater.avail_in == available) || (deflater.avail_out == 0));
        } catch (final DeflaterException ex) {
            throw (ZipException) new ZipException(ex.getMessage()).initCause(ex);
        } finally {
            deflater.avail_in = 0;
            deflater.next_in_buffer = null;
        }
        autoFlush(deflater, out);
        return available - src.remaining();
    }

    @Override
    public final int writeFully(final ByteBuffer src) throws IOException {
        final int count = src.remaining();
        if (count > 0) {
            final Deflater deflater = deflater();
            final OutputStream out = out();
            deflater.next_in_buffer = src;
            deflater.avail_in = count;
            try {
                do {
                    deflater.deflate(DeflaterFlushMode.Z_NO_FLUSH);
                } while ((deflater.avail_in > 0) || (deflater.avail_out == 0));
            } catch (final DeflaterException ex) {
                throw (ZipException) new ZipException(ex.getMessage()).initCause(ex);
            } finally {
                deflater.avail_in = 0;
                deflater.next_in_buffer = null;
            }
            autoFlush(deflater, out);
        }
        return count;
    }
}
