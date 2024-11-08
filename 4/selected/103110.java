package com.faunos.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * An in-memory FileChannel.  The implementation is backed by a
 * {@link ByteBuffer}.  As the <tt>FileChannel</tt>'s size is
 * increased beyond the capacity of the backing buffer, a larger
 * backing buffer is allocated.
 * <p/>
 * <h3>Unsupported Operations</h3>
 * A number of the <tt>FileChannel</tt> methods are not implemented here.
 * They are listed below.
 * <ul>
 * <li>{@link #lock()}</li>
 * <li>{@link #tryLock()}</li>
 * <li>{@link #lock(long, long, boolean)}</li>
 * <li>{@link #tryLock(long, long, boolean)}</li>
 * <li>{@link #map(java.nio.channels.FileChannel.MapMode, long, long)}</li>
 * </ul>
 * Invoking any of these methods raises an
 * <tt>UnsupportedOperationException</tt>
 * 
 * @author Babak Farhang
 */
public class MemoryFileChannel extends FileChannel {

    private ByteBuffer buffer;

    private static final int BLOCK_SIZE = 32;

    private final boolean direct;

    /**
     * Creates a new instance initially backed by the given <tt>buffer</tt>.
     * This constructor was specifically introduced in order to provide
     * a <em>read</em>-view of the existing contents of a buffer.
     */
    public MemoryFileChannel(ByteBuffer buffer) {
        this.buffer = buffer;
        this.direct = buffer.isDirect();
    }

    /**
     * Creates a new empty instance with the specified initial
     * <tt>capacity</tt>.  The backing buffer will be direct.
     */
    public MemoryFileChannel(int initCapacity) {
        this(initCapacity, true);
    }

    /**
     * Creates a new empty instance with the specified initial
     * <tt>capacity</tt> and buffer allocation policy.
     * 
     * @param direct
     *        if <tt>true</tt>, then the backing buffer will be allocated
     *        directly.
     * 
     * @see ByteBuffer#allocate(int)
     * @see ByteBuffer#allocateDirect(int)
     */
    public MemoryFileChannel(int initCapacity, boolean direct) {
        super();
        if (initCapacity <= 0) throw new IllegalArgumentException(String.valueOf(initCapacity));
        {
            int mod = initCapacity % BLOCK_SIZE;
            if (mod != 0) initCapacity += (BLOCK_SIZE - mod);
        }
        this.direct = direct;
        this.buffer = direct ? ByteBuffer.allocateDirect(initCapacity) : ByteBuffer.allocate(initCapacity);
        this.buffer.limit(0);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        final int bufferRemaining = buffer.remaining();
        if (bufferRemaining == 0) return -1;
        final int dstRemaining = dst.remaining();
        if (dstRemaining == 0) return 0;
        int amountRead;
        if (dstRemaining >= bufferRemaining) {
            amountRead = bufferRemaining;
            dst.put(this.buffer);
        } else {
            amountRead = dstRemaining;
            final int savedLimit = this.buffer.limit();
            this.buffer.limit(buffer.position() + dstRemaining);
            dst.put(this.buffer);
            this.buffer.limit(savedLimit);
        }
        return amountRead;
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if (buffer.remaining() == 0) return -1;
        long amountRead = 0;
        while (length-- > 0) {
            final int subAmountRead = read(dsts[offset]);
            if (subAmountRead == -1) {
                if (amountRead == 0) amountRead = -1;
                break;
            }
            amountRead += subAmountRead;
            ++offset;
        }
        return amountRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        final int srcRemaining = src.remaining();
        prepareWrite(srcRemaining);
        this.buffer.put(src);
        return srcRemaining;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (offset + length > srcs.length) throw new IndexOutOfBoundsException("offset [" + offset + "] + length [" + length + "] > srcs.length [" + srcs.length);
        long amountWritten = 0;
        while (length-- > 0) amountWritten += write(srcs[offset++]);
        return amountWritten;
    }

    @Override
    public long position() throws IOException {
        return this.buffer.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        prepareWrite((int) newPosition - this.buffer.position());
        this.buffer.position((int) newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return this.buffer.limit();
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        if (size < this.buffer.limit()) this.buffer.limit((int) size);
        return this;
    }

    @Override
    public void force(boolean metaData) throws IOException {
    }

    @Override
    public long transferTo(final long position, final long count, WritableByteChannel target) throws IOException {
        if (count <= 0) return 0;
        final ByteBuffer viewBuffer = this.buffer.duplicate();
        viewBuffer.position((int) position);
        final int bufferLimit = this.buffer.limit();
        final int argLimit = (int) (position + count);
        if (argLimit < bufferLimit) viewBuffer.limit(argLimit);
        return target.write(viewBuffer);
    }

    @Override
    public long transferFrom(ReadableByteChannel src, final long position, final long count) throws IOException {
        if (count <= 0) return 0;
        final int bufferLimit = this.buffer.limit();
        final int argLimit = (int) (position + count);
        if (position > bufferLimit) return 0; else if (position == bufferLimit) {
            prepareWrite(Math.min(argLimit - this.buffer.position(), this.buffer.capacity()));
            this.buffer.limit(bufferLimit);
        }
        final ByteBuffer viewBuffer = this.buffer.duplicate();
        viewBuffer.limit(Math.min(argLimit, viewBuffer.capacity()));
        viewBuffer.position((int) position);
        src.read(viewBuffer);
        if (viewBuffer.position() > bufferLimit) this.buffer.limit(viewBuffer.position());
        return viewBuffer.position() - position;
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        if (position >= this.buffer.limit()) return -1;
        final ByteBuffer viewBuffer = this.buffer.duplicate();
        viewBuffer.position((int) position);
        final int dstRemaining = dst.remaining();
        if (dstRemaining < viewBuffer.remaining()) viewBuffer.limit((int) position + dstRemaining);
        dst.put(viewBuffer);
        return viewBuffer.position() - (int) position;
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        final int srcRemaining = src.remaining();
        prepareWrite(srcRemaining + (int) position - this.buffer.position());
        ByteBuffer viewBuffer = this.buffer.duplicate();
        viewBuffer.limit(viewBuffer.capacity()).position((int) position);
        viewBuffer.put(src);
        if (viewBuffer.position() > this.buffer.limit()) this.buffer.limit(viewBuffer.position());
        return srcRemaining;
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void implCloseChannel() throws IOException {
    }

    private void prepareWrite(int srcRemaining) {
        final int bufferRemaining = this.buffer.remaining();
        if (bufferRemaining >= srcRemaining) return;
        final int deficit = srcRemaining - bufferRemaining;
        final int bufferLimit = this.buffer.limit();
        final int bufferCapacity = this.buffer.capacity();
        if (bufferCapacity - bufferLimit >= deficit) {
            this.buffer.limit(bufferLimit + deficit);
            return;
        }
        int newCapacity = this.buffer.capacity() * 2;
        while (newCapacity < bufferLimit + deficit) newCapacity *= 2;
        ByteBuffer newBuffer = direct ? ByteBuffer.allocateDirect(newCapacity) : ByteBuffer.allocate(newCapacity);
        final int position = this.buffer.position();
        this.buffer.position(0);
        newBuffer.put(this.buffer).position(position).limit(bufferLimit + deficit);
        this.buffer = newBuffer;
    }
}
