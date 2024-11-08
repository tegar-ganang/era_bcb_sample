package jaxlib.io.stream.embedded;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jaxlib.io.stream.IOStreams;
import jaxlib.io.stream.XOutputStream;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckBounds;

/**
 * An output stream to ensure a specific number of bytes are written to another stream.
 * A {@code FixedLengthOutputStream} is not closing the underlying stream. The {@link #close()} method throws
 * an {@link IOException} if bytes are missing.
 *
 * @see FixedLengthInputStream
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: FixedLengthOutputStream.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class FixedLengthOutputStream extends XOutputStream {

    private OutputStream out;

    private long remaining;

    public FixedLengthOutputStream(@Nullable final OutputStream out, final long length) {
        super();
        CheckArg.notNegative(length, "length");
        this.out = out;
        this.remaining = length;
    }

    /**
   * Called when the last byte has been written.
   * The default implementation does nothing.
   */
    protected void eof() throws IOException {
    }

    @Override
    public void close() throws IOException {
        closeInstance();
    }

    @Override
    public void closeInstance() throws IOException {
        if (this.out != null) {
            if (this.remaining != 0) throw new IOException(this.remaining + " bytes remaining.");
            this.out = null;
        }
    }

    @Override
    public boolean isOpen() {
        return this.out != null;
    }

    public final long remaining() {
        return this.remaining;
    }

    @Override
    public void write(final int b) throws IOException {
        final long remaining = this.remaining;
        if (remaining > 0) {
            ensureOpen().write(b);
            this.remaining = remaining - 1;
            if (remaining == 1) eof();
        } else if (this.out == null) {
            throw new IOException("closed");
        } else {
            throw new IOException("length has been reached");
        }
    }

    @Override
    public void write(final byte[] src, final int offs, final int len) throws IOException {
        CheckBounds.offset(src, offs, len);
        if (len > 0) {
            long remaining = this.remaining;
            if (len <= remaining) {
                ensureOpen().write(src, offs, len);
                this.remaining = remaining - len;
                if (remaining == len) eof();
            } else {
                throw new IOException("Can not write " + len + " bytes, only " + remaining + " remaining.");
            }
        }
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        final int count = src.remaining();
        final long remaining = this.remaining;
        if (count > 0) {
            if (count <= remaining) {
                int written = 0;
                try {
                    final OutputStream out = ensureOpen();
                    final WritableByteChannel outChannel;
                    if (out instanceof WritableByteChannel) outChannel = (WritableByteChannel) out; else if (out instanceof FileOutputStream) outChannel = ((FileOutputStream) out).getChannel(); else outChannel = null;
                    if (outChannel == null) {
                        if (src.hasArray()) {
                            out.write(src.array(), src.arrayOffset() + src.position(), count);
                            src.position(src.limit());
                            written = count;
                        } else {
                            while (written < count) {
                                out.write(src.get());
                                written++;
                            }
                        }
                    } else {
                        while (written < count) {
                            int step = outChannel.write(src);
                            if (step >= 0) {
                                if (step <= count - written) {
                                    if (step != 0) {
                                        written += count;
                                        continue;
                                    } else {
                                        this.remaining = remaining - written;
                                        written = 0;
                                        super.write(src);
                                        return count;
                                    }
                                } else {
                                    throw new ReturnValueException(outChannel, "write(ByteBuffer)", step, "<= ", count - written);
                                }
                            } else {
                                throw new ReturnValueException(outChannel, "write(ByteBuffer)", step, ">= 0");
                            }
                        }
                    }
                } finally {
                    this.remaining = remaining - written;
                }
            } else {
                throw new IOException("Can not write " + count + " bytes, only " + remaining + " remaining.");
            }
        }
        if (remaining == count) eof();
        return count;
    }

    @Override
    public long transferFrom(final InputStream in, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        final long remaining = this.remaining;
        if ((remaining == 0) || (maxCount == 0)) {
            return 0;
        } else {
            maxCount = (maxCount < 0) ? remaining : Math.min(maxCount, remaining);
            final long count = IOStreams.transfer(in, ensureOpen(), maxCount);
            this.remaining = remaining - count;
            if (remaining == count) eof();
            return count;
        }
    }

    @Nonnull
    private OutputStream ensureOpen() throws ClosedChannelException {
        final OutputStream out = this.out;
        if (out == null) throw new ClosedChannelException();
        return out;
    }
}
