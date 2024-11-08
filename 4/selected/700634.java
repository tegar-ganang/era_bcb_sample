package com.faunos.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import com.faunos.util.Validator;

/**
 * A view of a region of an underlying <code>FileChannel</code>.
 * The {@linkplain #position() position} of the this view is independent
 * of that of the underlying <code>FileChannel</code>, except for the
 * case involving {@linkplain #truncate(long) truncation}.
 * <a name="accessmodes"/>
 * <h3>Access modes</h3>
 * Three {@linkplain AccessMode access modes} are supported.  They are
 * <ol>
 * <li>
 * <em>{@linkplain AccessMode#READ_ONLY READ_ONLY}</em>. The instance
 * can only read from the channel.
 * </li><li>
 * <em>{@linkplain AccessMode#BOUNDED_UPDATE BOUNDED_UPDATE}</em>. The
 * instance can read and write to the channel, but may not change the
 * file size.
 * </li><li>
 * <em>{@linkplain AccessMode#WRITE WRITE}</em>.  The instance can read
 * and write to to the channel, may grow the file size, or alternatively
 * truncate it.  This is the least protected view of the underlying
 * <code>FileChannel</code>.
 * </li>
 * </ol>
 *
 * @author Babak Farhang
 */
public class SubFileChannel extends CloseProtectedFileChannel {

    private static Validator<IllegalArgumentException> validator = Validator.defaultInstance();

    /**
     * Defines the access modes supported by <code>SubFileChannel</code>.
     *
     * @see <a href="SubFileChannel.html#accessmodes">Access modes</a>
     * @author Babak Farhang
     */
    public static enum AccessMode {

        READ_ONLY, BOUNDED_UPDATE, WRITE
    }

    private final AccessMode accessMode;

    private final long startPos;

    private long pos;

    private long endPos;

    public static SubFileChannel newReadOnlyView(FileChannel file, long startPosition, long endPosition) throws IOException {
        return new SubFileChannel(file, AccessMode.READ_ONLY, startPosition, endPosition);
    }

    /**
     * Creates a new instance with the specified underlying <code>file</code>
     * channel, access mode, and view boundaries.
     * 
     * @param startPosition     the lower bounds of the view
     * @param endPosition       the upper bounds of the view
     */
    public SubFileChannel(FileChannel file, AccessMode mode, long startPosition, long endPosition) throws IOException {
        super(file);
        this.accessMode = mode;
        this.startPos = startPosition;
        this.pos = startPos;
        this.endPos = endPosition;
        validator.notNull(mode, "null *mode*");
        validator.nonNegative(endPos, "Negative start position");
        if (endPos < startPos) validator.fail("*endPosition* < *startPosition* : " + startPos + "," + endPos);
        final long filesize = file.size();
        if (startPos > filesize) validator.fail("*startPosition* is beyond end of file");
        if (mode == AccessMode.READ_ONLY && endPos > filesize) validator.fail("*endPosition* beyond end of file. mode: " + AccessMode.READ_ONLY);
    }

    @Override
    public void force(boolean metaData) throws IOException {
        checkWriteAccess();
        inner.force(metaData);
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        if (shared) checkAccess(); else checkWriteAccess();
        final long nativePosition = toNativePosition(position);
        checkNegativePosition(nativePosition);
        FileLock innerLock = inner.lock(nativePosition, size, shared);
        return implCreateFileLock(innerLock);
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        checkAccess();
        if (this.accessMode == AccessMode.READ_ONLY && mode == MapMode.READ_WRITE) failIllegalWriteAccess();
        final long nativePosition = toNativePosition(position);
        checkEofForWrite(nativePosition + size);
        return inner.map(mode, nativePosition, size);
    }

    @Override
    public long position() throws IOException {
        checkAccess();
        return toLocalPosition(pos);
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        checkAccess();
        checkNegativeLocalPosition(newPosition);
        this.pos = toNativePosition(newPosition);
        return this;
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        checkAccess();
        checkNegativeLocalPosition(position);
        final long nativePosition = toNativePosition(position);
        final long remainingBytesInChannel = endPos - nativePosition;
        if (remainingBytesInChannel <= 0) return -1;
        final int savedLimit;
        if (dst.remaining() > remainingBytesInChannel) {
            savedLimit = dst.limit();
            dst.limit(dst.position() + (int) remainingBytesInChannel);
        } else savedLimit = -1;
        final int amountRead = inner.read(dst, nativePosition);
        if (savedLimit != -1) dst.limit(savedLimit);
        return amountRead;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkAccess();
        final int amount = read(dst, position());
        if (amount > 0) pos += amount;
        return amount;
    }

    @Override
    public long read(ByteBuffer[] dsts, final int offset, final int length) throws IOException {
        checkAccess();
        long amountRead = 0;
        final int end = offset + length;
        for (int i = offset; i < end; ++i) {
            ByteBuffer dst = dsts[i];
            if (!dst.hasRemaining()) continue;
            final int amt = read(dst);
            if (amt != -1 || amountRead == 0) amountRead += amt;
            if (dst.hasRemaining()) break;
        }
        return amountRead;
    }

    @Override
    public long size() throws IOException {
        checkAccess();
        return endPos - startPos;
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        checkWriteAccess();
        final long nativePosition = toNativePosition(position);
        checkNativePositionForWrite(nativePosition);
        checkEofForWrite(nativePosition + count);
        final long amountWritten = inner.transferFrom(src, nativePosition, count);
        final long nativeEndPosition = nativePosition + amountWritten;
        if (nativeEndPosition > endPos) endPos = nativeEndPosition;
        return amountWritten;
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        checkAccess();
        validator.nonNegative(count, "negative *count*");
        checkNegativeLocalPosition(position);
        final long nativePosition = toNativePosition(position);
        if (nativePosition + count > endPos) count = endPos - nativePosition;
        if (count <= 0) return 0;
        return inner.transferTo(nativePosition, count, target);
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        checkAccess();
        validator.isTrue(accessMode == AccessMode.WRITE, "cannot truncate in non-write mode");
        validator.nonNegative(size);
        if (size < size()) {
            final long eof = startPos + size;
            inner.truncate(eof);
            endPos = eof;
            if (endPos < pos) pos = endPos;
        }
        return this;
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        if (shared) checkAccess(); else checkWriteAccess();
        final long nativePosition = toNativePosition(position);
        checkNegativePosition(nativePosition);
        FileLock lock = inner.tryLock(nativePosition, size, shared);
        if (lock != null) lock = implCreateFileLock(lock);
        return lock;
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        checkWriteAccess();
        final long nativePosition = toNativePosition(position);
        checkNativePositionForWrite(nativePosition);
        checkEofForWrite(nativePosition + src.remaining());
        final int amountWritten = inner.write(src, nativePosition);
        if (amountWritten > 0) {
            final long endPosition = nativePosition + amountWritten;
            if (endPosition > endPos) endPos = endPosition;
        }
        return amountWritten;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        final int amountWritten = write(src, position());
        pos += amountWritten;
        return amountWritten;
    }

    @Override
    public long write(ByteBuffer[] srcs, final int offset, final int length) throws IOException {
        checkWriteAccess();
        validator.nonNegative(offset, "negative *offset*");
        validator.nonNegative(length, "negative *length*");
        final int end = offset + length;
        validator.isTrue(end <= srcs.length, "out of bounds index args");
        final int indexOfHasRemaining;
        final long remaining;
        {
            int rx = -1;
            long remainingTally = 0;
            for (int i = offset; i < end; ++i) {
                remainingTally += srcs[i].remaining();
                if (rx == -1 && remainingTally > 0) rx = i;
            }
            indexOfHasRemaining = rx;
            remaining = remainingTally;
        }
        if (remaining == 0) return 0;
        checkEofForWrite(pos + remaining);
        long amountWritten = 0;
        for (int i = indexOfHasRemaining; i < end; ++i) {
            ByteBuffer src = srcs[i];
            if (!src.hasRemaining()) continue;
            amountWritten += write(src);
            if (src.hasRemaining()) break;
        }
        return amountWritten;
    }

    /**
     * Creates and returns a position-shifted <code>FileLock</code>.
     */
    @Override
    protected FilterFileLock implCreateFileLock(FileLock innerLock) throws IOException {
        return new SubFileLock(innerLock, this);
    }

    /**
     * Checks that the instance has write access.  Called on methods
     * that may attempt to change the contents of the channel.
     * <p/>
     * This implementation simply checks that the access mode at
     * instantiation was <em>not</em> {@link AccessMode#READ_ONLY};
     * o.w. an <code>java.io.IOException</code> is thrown.
     */
    protected void checkWriteAccess() throws IOException {
        super.checkWriteAccess();
        if (accessMode == AccessMode.READ_ONLY) failIllegalWriteAccess();
    }

    /**
     * Checks that the instance has read access.  Called on methods
     * that attempt to read from the channel.  The base implementation
     * does nothing.
     * <p/>
     * Subclasses overriding this method should ensure that if an
     * instance has write access ({@link #checkWriteAccess()} does
     * not throw an exception), then it also has read access.
     * 
     * @throws IOException
     *         thrown if the instance does not have read access
     */
    protected void checkAccess() throws IOException {
    }

    /**
     * Returns the given local file position as the native (underlying)
     * file position.
     */
    private long toNativePosition(long position) {
        return position + startPos;
    }

    private long toLocalPosition(long nativePosition) {
        return nativePosition - startPos;
    }

    private void checkNativePositionForWrite(long nativePosition) {
        checkNegativePosition(nativePosition);
        checkEofForWrite(nativePosition);
    }

    private void checkNegativeLocalPosition(long position) {
        if (position < 0) validator.fail("negative position: " + position);
    }

    private void checkNegativePosition(long nativePosition) {
        if (nativePosition < startPos) validator.fail("negative position: " + toLocalPosition(nativePosition));
    }

    private void checkEofForWrite(long nativePosition) {
        if (nativePosition > endPos && accessMode != AccessMode.WRITE) validator.fail("position beyond EOF: " + toLocalPosition(nativePosition));
    }

    private void failIllegalWriteAccess() throws IOException {
        throw new IOException("attempt to modify read-only instance");
    }

    private static class SubFileLock extends FilterFileLock {

        protected SubFileLock(FileLock innerLock, SubFileChannel channel) {
            super(innerLock, channel, innerLock.position() - channel.startPos, innerLock.size(), innerLock.isShared());
        }
    }
}
