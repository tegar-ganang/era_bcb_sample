package jaxlib.io.stream.embedded;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.io.stream.IOStreams;
import jaxlib.io.stream.XInputStream;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckBounds;

/**
 * An input stream which reads a specific number of bytes from another stream.
 * A {@code FixedLengthInputStream} is not closing the underlying stream. The {@link #close()} method skips
 * remaining bytes.
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: FixedLengthInputStream.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class FixedLengthInputStream extends XInputStream {

    private final boolean closeIn;

    private InputStream in;

    private long remaining;

    private long remainingAtMark = -1;

    public FixedLengthInputStream(final InputStream in, final long length) {
        this(in, length, false);
    }

    public FixedLengthInputStream(final InputStream in, final long length, final boolean closeIn) {
        super();
        CheckArg.notNegative(length, "length");
        if (length != 0) CheckArg.notNull(in, "in");
        this.closeIn = closeIn;
        this.in = in;
        this.remaining = length;
    }

    private InputStream ensureOpen() throws IOException {
        final InputStream in = this.in;
        if (in != null) return in; else throw new IOException("closed");
    }

    /**
   * Called when the end of the substream has been reached.
   * The default implementation does nothing.
   */
    protected void eof() throws IOException {
    }

    @Override
    public int available() throws IOException {
        final InputStream in = this.in;
        if (in != null) return (int) Math.min(this.remaining, in.available()); else throw new IOException("closed");
    }

    /**
   * This implementation just calls {@link #closeInstance()}.
   */
    @Override
    public void close() throws IOException {
        final InputStream in = this.in;
        IOException ex = null;
        try {
            closeInstance();
        } catch (final IOException x) {
            ex = x;
        }
        if ((in != null) && this.closeIn) {
            try {
                in.close();
            } catch (final IOException x) {
                if (ex == null) ex = x;
            }
        }
        if (ex != null) throw ex;
    }

    /**
   * This implementation skips all remaining bytes.
   *
   * @see #remaining()
   */
    @Override
    public void closeInstance() throws IOException {
        if (this.in != null) {
            try {
                skip(this.remaining);
            } finally {
                this.in = null;
            }
        }
    }

    @Override
    public boolean isOpen() {
        return this.in != null;
    }

    @Override
    public void mark(int readLimit) {
        final InputStream in = this.in;
        if (in != null) {
            in.mark(readLimit);
            this.remainingAtMark = this.remaining;
        } else {
            this.remainingAtMark = 0;
        }
    }

    @Override
    public boolean markSupported() {
        final InputStream in = this.in;
        return (in != null) && in.markSupported();
    }

    /**
   * Returns the number of remaining bytes to read.
   */
    public final long remaining() {
        return this.remaining;
    }

    @Override
    public void reset() throws IOException {
        final long remainingAtMark = this.remainingAtMark;
        if (remainingAtMark >= 0) {
            ensureOpen().reset();
            this.remaining = remainingAtMark;
        } else {
            throw new IOException("not marked");
        }
    }

    @Override
    public int read() throws IOException {
        final long remaining = this.remaining;
        if (remaining > 0) {
            final int b = ensureOpen().read();
            if (b >= 0) {
                this.remaining = remaining - 1;
                if (remaining == 1) eof();
                return b;
            } else {
                throw new EOFException("stream truncated");
            }
        } else {
            return -1;
        }
    }

    @Override
    public int read(final byte[] dest, final int offs, final int len) throws IOException {
        if (len > 0) {
            final long remaining = this.remaining;
            if (remaining > 0) {
                final int maxStep = (int) Math.min(remaining, len);
                final int step = ensureOpen().read(dest, offs, maxStep);
                if (step >= 0) {
                    if (step <= maxStep) {
                        this.remaining = remaining - step;
                        if (remaining == step) eof();
                        return step;
                    } else {
                        throw new ReturnValueException(this.in, "read(byte[],int,int)", step, "<=", maxStep);
                    }
                } else {
                    throw new EOFException("stream truncated");
                }
            } else {
                return -1;
            }
        } else {
            CheckBounds.offset(dest, offs, len);
            return 0;
        }
    }

    @Override
    public int read(final ByteBuffer dest) throws IOException {
        final int count = dest.remaining();
        if (count > 0) {
            long remaining = this.remaining;
            if (remaining > 0) {
                final InputStream in = this.in;
                final ReadableByteChannel inChannel;
                if (in instanceof ReadableByteChannel) inChannel = (ReadableByteChannel) in; else if (in instanceof FileInputStream) inChannel = ((FileInputStream) in).getChannel(); else return super.read(dest);
                ByteBuffer dupe = null;
                int readed = 0;
                while (true) {
                    final int maxStep = (int) Math.min(remaining, count - readed);
                    if (maxStep > 0) {
                        final ByteBuffer buf;
                        if (count - readed >= maxStep) {
                            dupe = null;
                            buf = dest;
                        } else {
                            if (dupe == null) dupe = dest.duplicate();
                            dupe.limit(dest.position() + maxStep);
                            buf = dupe;
                        }
                        final int step = inChannel.read(buf);
                        if (step > 0) {
                            if (buf != dest) dest.position(buf.position());
                            if (step <= maxStep) {
                                this.remaining = remaining -= step;
                                readed += step;
                                if (step == remaining) {
                                    eof();
                                    break;
                                } else {
                                    continue;
                                }
                            } else {
                                throw new ReturnValueException(inChannel, "read(ByteBuffer)", step, "<=", maxStep);
                            }
                        } else if (step == 0) {
                            return readed + super.read(dest);
                        } else {
                            throw new IOException("stream truncated");
                        }
                    } else {
                        break;
                    }
                }
                return readed;
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    @Override
    public long skip(long maxCount) throws IOException {
        CheckArg.count(maxCount);
        final long remaining = this.remaining;
        maxCount = Math.min(remaining, maxCount);
        if (maxCount != 0) {
            final long count = ensureOpen().skip(maxCount);
            if (count > 0) {
                if (count <= maxCount) {
                    this.remaining = remaining - count;
                    if (remaining == count) eof();
                } else {
                    throw new ReturnValueException(this.in, "skip(long)", count, "<=", maxCount);
                }
            } else if (count < 0) {
                throw new ReturnValueException(this.in, "skip(long)", count, ">= 0");
            }
            return count;
        } else {
            return 0;
        }
    }

    @Override
    public long transferTo(final OutputStream out, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        final long remaining = this.remaining;
        if ((remaining == 0) || (maxCount == 0)) {
            return 0;
        } else {
            maxCount = (maxCount < 0) ? remaining : Math.min(maxCount, remaining);
            final long count = IOStreams.transfer(ensureOpen(), out, maxCount);
            this.remaining = remaining - count;
            if (remaining == count) eof();
            return count;
        }
    }

    @Override
    public long transferToByteChannel(final WritableByteChannel out, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        final long remaining = this.remaining;
        if ((remaining == 0) || (maxCount == 0)) {
            return 0;
        } else {
            maxCount = (maxCount < 0) ? remaining : Math.min(maxCount, remaining);
            final long count = IOStreams.transferToByteChannel(ensureOpen(), out, maxCount);
            this.remaining = remaining - count;
            if (remaining == count) eof();
            return count;
        }
    }
}
