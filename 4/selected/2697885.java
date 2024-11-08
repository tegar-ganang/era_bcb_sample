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
 * An input stream which reads data written by a {@code BlockOutputStream}.
 * <p>
 * Unlike {@link BlockOutputStream} a {@code BlockInputStream} is not using an internal buffer. Thus you may
 * consider to wrap a {@code BlockInputStream} by a buffered stream.
 * </p>
 *
 * @see BlockOutputStream
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: BlockInputStream.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class BlockInputStream extends XInputStream {

    /**
   * The underlying stream.
   */
    private InputStream in;

    /**
   * Number of bytes remaining in current block.
   * -1 if the end of the stream has been reached, but this stream has not been closed.
   */
    private int remainingInBlock = 0;

    private int remainingInMarkedBlock = -1;

    public BlockInputStream(final InputStream in) {
        super();
        if (in == null) throw new NullPointerException("in");
        this.in = in;
    }

    private InputStream ensureOpen() throws IOException {
        final InputStream in = this.in;
        if (in == null) throw new IOException("closed");
        return in;
    }

    private int remainingInBlock() throws IOException {
        int remainingInBlock = this.remainingInBlock;
        if (remainingInBlock > 0) return remainingInBlock;
        if (remainingInBlock == 0) {
            final InputStream in = ensureOpen();
            final int b1 = in.read();
            if (b1 >= 0) {
                final int b0 = in.read();
                if (b0 >= 0) {
                    remainingInBlock = ((b1 << 8) | b0);
                    if (remainingInBlock > 0) return remainingInBlock;
                    this.remainingInBlock = -1;
                    eof();
                    return 0;
                }
            }
            throw new EOFException("truncated block");
        } else if (remainingInBlock == -1) return 0; else throw new IOException("closed");
    }

    /**
   * Called when the end of the last block has been reached.
   * The default implementation does nothing.
   */
    protected void eof() throws IOException {
    }

    @Override
    public int available() throws IOException {
        final int remainingInBlock = remainingInBlock();
        this.remainingInBlock = remainingInBlock;
        return Math.min(ensureOpen().available(), remainingInBlock);
    }

    @Override
    public void close() throws IOException {
        final InputStream in = this.in;
        if (in != null) {
            closeInstance();
            in.close();
        }
    }

    @Override
    public void closeInstance() throws IOException {
        this.in = null;
        this.remainingInBlock = 0;
    }

    @Override
    public boolean isOpen() {
        return this.in != null;
    }

    @Override
    public void mark(final int readLimit) {
        CheckArg.notNegative(readLimit, "readLimit");
        final InputStream in = this.in;
        if (in != null) {
            final int remainingInBlock = this.remainingInBlock;
            if (remainingInBlock != -1) {
                in.mark((int) Math.min(Integer.MAX_VALUE, ((long) readLimit) * 3));
                this.remainingInMarkedBlock = remainingInBlock;
            }
        }
    }

    @Override
    public boolean markSupported() {
        InputStream in = this.in;
        return (in != null) && in.markSupported();
    }

    @Override
    public void reset() throws IOException {
        final int remainingInMarkedBlock = this.remainingInMarkedBlock;
        if (remainingInMarkedBlock == -1) throw new IOException("not marked");
        ensureOpen().reset();
        this.remainingInBlock = remainingInMarkedBlock;
        this.remainingInMarkedBlock = -1;
    }

    @Override
    public int read() throws IOException {
        int remainingInBlock = remainingInBlock();
        if (remainingInBlock > 0) {
            InputStream in = ensureOpen();
            int b = in.read();
            if (b >= 0) {
                this.remainingInBlock = remainingInBlock - 1;
                return b;
            } else {
                throw new EOFException("truncated block");
            }
        } else {
            return -1;
        }
    }

    @Override
    public int read(final byte[] dest, final int offs, final int len) throws IOException {
        CheckBounds.offset(dest, offs, len);
        if (len == 0) {
            ensureOpen();
            return 0;
        }
        final int remainingInBlock = remainingInBlock();
        if (remainingInBlock <= 0) return -1;
        final int maxStep = Math.min(len, remainingInBlock);
        final int step = ensureOpen().read(dest, offs, maxStep);
        if (step < 0) throw new EOFException("truncated block");
        if (step > maxStep) throw new ReturnValueException(this.in, "read(byte[],int,int)", step, "<=", maxStep);
        this.remainingInBlock = remainingInBlock - step;
        return step;
    }

    @Override
    public int read(final ByteBuffer dest) throws IOException {
        int remaining = dest.remaining();
        if (remaining <= 0) return 0;
        final InputStream in = this.in;
        final ReadableByteChannel inChannel;
        if (in instanceof ReadableByteChannel) inChannel = (ReadableByteChannel) in; else if (in instanceof FileInputStream) inChannel = ((FileInputStream) in).getChannel(); else return super.read(dest);
        CheckArg.writable(dest);
        int remainingInBlock = remainingInBlock();
        int count = 0;
        if (remainingInBlock > 0) {
            ByteBuffer dupe = null;
            while (true) {
                final int maxStep = Math.min(remaining, remainingInBlock);
                if (maxStep <= 0) break;
                final ByteBuffer buf;
                if (remaining >= maxStep) buf = dest; else {
                    if (dupe == null) dupe = dest.duplicate();
                    dupe.limit(dest.position() + maxStep);
                    buf = dupe;
                }
                final int step = inChannel.read(buf);
                if (step > 0) {
                    if (buf != dest) dest.position(buf.position());
                    if (step > maxStep) throw new ReturnValueException(inChannel, "read(ByteBuffer)", step, "<=", maxStep);
                    this.remainingInBlock = remainingInBlock - step;
                    remaining -= step;
                    count += step;
                    if (remaining <= 0) break;
                    remainingInBlock = remainingInBlock();
                    continue;
                } else if (step == 0) return count + super.read(dest); else break;
            }
        }
        return (count > 0) ? count : -1;
    }

    @Override
    public long skip(long maxCount) throws IOException {
        CheckArg.count(maxCount);
        final int remainingInBlock = remainingInBlock();
        this.remainingInBlock = remainingInBlock;
        if ((maxCount == 0) || (remainingInBlock == 0)) return 0;
        maxCount = Math.min(maxCount, remainingInBlock);
        long count = (int) ensureOpen().skip(maxCount);
        if (count < 0) throw new ReturnValueException(this.in, "skip", count, ">= 0");
        if (count > maxCount) throw new ReturnValueException(this.in, "skip", count, "<=", maxCount);
        this.remainingInBlock = remainingInBlock - (int) count;
        if (count == remainingInBlock) eof();
        return count;
    }

    @Override
    public long transferTo(final OutputStream dest, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        int remainingInBlock = remainingInBlock();
        this.remainingInBlock = remainingInBlock;
        if ((maxCount == 0) || (remainingInBlock == 0)) return 0;
        maxCount = (maxCount < 0) ? remainingInBlock : Math.min(maxCount, remainingInBlock);
        int count = (int) IOStreams.transfer(ensureOpen(), dest, maxCount);
        this.remainingInBlock = remainingInBlock - count;
        if (count == remainingInBlock) eof();
        return count;
    }

    @Override
    public long transferToByteChannel(final WritableByteChannel dest, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        final int remainingInBlock = remainingInBlock();
        this.remainingInBlock = remainingInBlock;
        if ((maxCount == 0) || (remainingInBlock == 0)) return 0;
        maxCount = (maxCount < 0) ? remainingInBlock : Math.min(maxCount, remainingInBlock);
        final int count = (int) IOStreams.transferToByteChannel(ensureOpen(), dest, maxCount);
        this.remainingInBlock = remainingInBlock - count;
        if (count == remainingInBlock) eof();
        return count;
    }
}
