package com.faunos.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import com.faunos.util.Validator;

/**
 * A facade base class for subclassing and chaining specialized <code>FileChannel</code>
 * functionality.  This class just delegates to its inner instance.
 *
 * @author Babak Farhang
 */
public class FilterFileChannel extends FileChannel {

    /**
     * The underlying file channel.
     */
    protected final FileChannel inner;

    /**
     * Creates a new instance using the given instance.
     * 
     * @param inner
     *        the underlying channel, must not be <code>null</code>
     */
    protected FilterFileChannel(FileChannel inner) {
        this.inner = inner;
        Validator.defaultInstance().notNull(inner);
    }

    @Override
    public void force(boolean metaData) throws IOException {
        checkWriteAccess();
        inner.force(metaData);
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        if (shared) checkReadAccess(); else checkWriteAccess();
        FileLock innerLock = inner.lock(position, size, shared);
        return implCreateFileLock(innerLock);
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        if (mode == MapMode.READ_WRITE) checkWriteAccess(); else checkReadAccess();
        return inner.map(mode, position, size);
    }

    @Override
    public long position() throws IOException {
        checkReadAccess();
        return inner.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        checkWriteAccess();
        return inner.position(newPosition);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkReadAccess();
        return inner.read(dst);
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        checkReadAccess();
        return inner.read(dst, position);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        checkReadAccess();
        return inner.read(dsts, offset, length);
    }

    @Override
    public long size() throws IOException {
        checkReadAccess();
        return inner.size();
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        checkWriteAccess();
        return inner.transferFrom(src, position, count);
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        checkReadAccess();
        return inner.transferTo(position, count, target);
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        checkWriteAccess();
        inner.truncate(size);
        return this;
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        if (shared) checkReadAccess(); else checkWriteAccess();
        FileLock lock = inner.tryLock(position, size, shared);
        if (lock != null) lock = implCreateFileLock(lock);
        return lock;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkWriteAccess();
        return inner.write(src);
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        checkWriteAccess();
        return inner.write(src, position);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        checkWriteAccess();
        return inner.write(srcs, offset, length);
    }

    /**
     * Invokes the <code>close()</code> method on the underlying channel.
     */
    @Override
    protected void implCloseChannel() throws IOException {
        inner.close();
    }

    /**
     * Returns the implementation-specific <code>FileLock</code>.
     */
    protected FilterFileLock implCreateFileLock(final FileLock innerLock) throws IOException {
        return new FilterFileLock(innerLock, this);
    }

    protected void checkReadAccess() throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
    }

    protected void checkWriteAccess() throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
    }

    /**
     * A <code>FileLock</code> filter.  The main point about this
     * implementation is that it returns the correct <code>FileChannel</code>.
     *
     * @author Babak Farhang
     */
    protected static class FilterFileLock extends FileLock {

        protected final FileLock innerLock;

        protected FilterFileLock(FileLock innerLock, FilterFileChannel channel) {
            this(innerLock, channel, innerLock.position(), innerLock.size(), innerLock.isShared());
        }

        protected FilterFileLock(FileLock innerLock, FilterFileChannel channel, long position, long size, boolean shared) {
            super(channel, position, size, shared);
            this.innerLock = innerLock;
            Validator.defaultInstance().notNull(innerLock, "null *innerLock*");
        }

        /**
         * Delegates to the underlying lock.
         */
        @Override
        public boolean isValid() {
            return innerLock.isValid();
        }

        /**
         * Delegates to the underlying lock.
         */
        @Override
        public void release() throws IOException {
            innerLock.release();
        }
    }
}
