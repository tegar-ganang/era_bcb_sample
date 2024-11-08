package com.faunos.skwish.sys;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import com.faunos.skwish.Segment;
import com.faunos.skwish.SegmentException;
import com.faunos.util.io.ChannelUtil;
import com.faunos.util.io.SubFileChannel;
import com.faunos.util.io.Word;
import com.faunos.util.io.WorkAroundFileChannel;
import com.faunos.util.io.SubFileChannel.AccessMode;

/**
 * The base implementation that is backed by an offset (index) and a contents file.
 * 
 * @see <a href="../package-summary.html#filestartposition">Implementation Notes: On where files begin</a>
 * @author Babak Farhang
 *
 */
public class BaseSegment extends Segment {

    protected final Index index;

    protected final FileChannel entryFile;

    protected static final ChannelUtil<SegmentException> helper = new ChannelUtil<SegmentException>(SegmentException.class);

    private volatile boolean closed;

    /**
     * Reads and returns a new instance using the given offset (index)
     * file and associated entry [contents] file.
     * 
     * @param offsetFile  the offset (index) file.  Must be positioned
     *                    at the start of the index (which need not be
     *                    at the beginning of the file)
     * @param entryFile   the associated file containing the entry contents.
     *                    The file position doesn't matter.
     */
    public BaseSegment(FileChannel offsetFile, FileChannel entryFile) throws IOException {
        this(new Index(offsetFile), entryFile);
    }

    /**
     * Creates a new instance.
     * 
     * @see #writeNewSegment(FileChannel, FileChannel, long, Word) pseudo-constructor
     */
    public BaseSegment(Index index, FileChannel entryFile) throws IOException {
        this.index = index;
        this.entryFile = entryFile;
        validator.notNull(index, "null index");
        validator.notNull(entryFile, "null entry file");
        if (entryFile.size() < index.getLastOffset()) validator.fail("Entry file too small.  Required " + index.getLastOffset() + " bytes; actual was " + entryFile.size());
    }

    /**
     * Writes and returns a new segment to the specified offset (index) and entry [contents] files.
     * Both files are assumed to be positioned at the start of where the segment will
     * begin (which need not necessarily be at the beginning of the files).
     */
    public static BaseSegment writeNewSegment(FileChannel offsetFile, FileChannel entryFile, long baseId, Word type) throws IOException {
        Index index = Index.writeNewIndex(offsetFile, type, baseId, entryFile.position());
        return new BaseSegment(index, entryFile);
    }

    /**
     * @return <code>false</code>
     */
    public boolean isReadOnly() {
        return false;
    }

    public long getBaseId() {
        return index.getMetrics().getBaseId();
    }

    public long getEntryCount() {
        return index.getEntryCount();
    }

    public long getNextId() {
        return index.getNextId();
    }

    public long killNext(int count) throws IOException {
        return index.killNext(count);
    }

    /**
     * Returns an estimate of the byte size of the total entry contents
     * contained in this segment.  This is an estimate because deleted
     * entries <em>may</em> contribute to this size.  Also, the returned
     * size does not include the index overhead per entry (typically 4
     * bytes).
     * 
     * @see #getTotalSize()
     */
    public long getContentsSize() {
        return index.getContentsSize();
    }

    /**
     * Returns an estimate of the total byte size of this segment, accounting
     * for the index overhead.
     */
    public long getTotalSize() {
        return index.getIndexSize() + getContentsSize();
    }

    public long insertEntry(ByteBuffer entry) throws IOException {
        checkInsert();
        final int entrySize = entry.remaining();
        final long offset = index.getLastOffset();
        helper.writeRemaining(entryFile, entry, offset);
        return index.pushNext(entrySize);
    }

    public long insertEntry(ReadableByteChannel entry) throws IOException {
        checkInsert();
        long pos = index.getLastOffset();
        FileChannel entryFile = new WorkAroundFileChannel(this.entryFile);
        while (true) {
            long amtTransfered = entryFile.transferFrom(entry, pos, 0x1000000);
            if (amtTransfered == 0) break;
            pos += amtTransfered;
        }
        return index.pushNext(pos - index.getLastOffset());
    }

    private boolean openInsertionChannel;

    private void checkInsert() {
        if (openInsertionChannel) throw new IllegalStateException("an entry insertion channel is still open");
    }

    /**
     * Return an output stream (<tt>FileChannel</tt>) for inserting the
     * next entry.  The next entry is actually created on invoking the
     * <tt>close</tt> method on the returned FileChannel. If another
     * insert operation (including this one) is invoked on this instance before
     * the returned stream is closed, then an <tt>IllegalStateException</tt>
     * is raised.
     * 
     */
    public FileChannel getEntryInsertionChannel() throws IOException {
        checkInsert();
        openInsertionChannel = true;
        class InsertionChannel extends SubFileChannel {

            InsertionChannel(long entryStartPos) throws IOException {
                super(entryFile, AccessMode.WRITE, entryStartPos, entryStartPos);
            }

            @Override
            protected void implCloseChannel() throws IOException {
                index.pushNext(size());
                openInsertionChannel = false;
            }
        }
        return new InsertionChannel(index.getLastOffset());
    }

    public void delete(long id, int count, boolean purge) throws IOException {
        if (count < 1) return;
        long endOffset = index.delete(id, count);
        if (purge) {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            index.readOffsets(id, buffer);
            buffer.flip();
            long startOffset = IndexMetrics.toAbsolute(buffer.getLong());
            endOffset = IndexMetrics.toAbsolute(endOffset);
            long diff = endOffset - startOffset;
            if (diff > 0) {
                long pos = startOffset;
                for (long z = diff / Word.LONG.getWidth(); z-- > 0; ) {
                    long noise = generateNoise();
                    buffer.clear();
                    buffer.putLong(noise).flip();
                    helper.writeRemaining(entryFile, buffer, pos);
                    pos += Word.LONG.getWidth();
                }
                if (pos < endOffset) {
                    long noise = generateNoise();
                    buffer.clear();
                    buffer.putLong(noise).flip().limit((int) (endOffset - pos));
                    helper.writeRemaining(entryFile, buffer, pos);
                }
            } else validator.isTrue(diff == 0, "Index integrity failure");
        }
    }

    /**
     * Returns a numeric value used to overwrite entries that are to be purged.
     * This base implementation always returns 0.  A subclass might
     * use a random number generator to better mask purged entries.
     */
    protected long generateNoise() {
        return 0;
    }

    /**
     * Truncates the entry count to the specified amount, discarding
     * any existing "higher" count entries in the process.
     * <p/>
     * Note this method must generally be invoked while the segment
     * is "offline" (no concurrent readers); if there <em>are</em>
     * concurrent readers, then the caller must somehow guarantee that
     * no to-be-discarded entries are being (or will be) read.
     */
    public void truncateEntryCount(long count) throws IOException {
        index.truncateEntryCount(count);
        entryFile.truncate(index.getLastOffset());
    }

    /**
     * Truncates the entry count to the specified amount, discarding
     * any existing "higher" count entries in the process.
     * <p/>
     * Note this method must generally be invoked while the segment
     * is "offline" (no concurrent readers); if there <em>are</em>
     * concurrent readers, then the caller must somehow guarantee that
     * no to-be-discarded entries are being (or will be) read.
     * 
     * @param trim
     *        if <tt>true</tt> (the default), then the backing files are
     *        also truncated to minimum size.
     */
    public void truncateEntryCount(long count, boolean trim) throws IOException {
        index.truncateEntryCount(count, trim);
        if (trim) {
            entryFile.truncate(index.getLastOffset());
        }
    }

    /**
     * Trims the backing file to minimum size. This method is only useful if
     * {@linkplain #truncateEntryCount(long, boolean)} was called with <tt>false</tt>.
     */
    public void trim() throws IOException {
        index.trimToCount();
        entryFile.truncate(index.getLastOffset());
    }

    public long getEntrySize(long id, ByteBuffer workBuffer) throws IOException {
        workBuffer.clear();
        index.readOffsets(id, workBuffer);
        workBuffer.flip();
        long start = workBuffer.getLong();
        if (IndexMetrics.isDeleted(start)) return -1;
        long end = IndexMetrics.toAbsolute(workBuffer.getLong());
        return end - start;
    }

    @Override
    public int getEntryPart(long id, long position, ByteBuffer out, ByteBuffer workBuffer) throws IOException {
        final long start;
        final long readableAmount;
        {
            workBuffer.clear();
            index.readOffsets(id, workBuffer);
            workBuffer.flip();
            start = workBuffer.getLong();
            if (IndexMetrics.isDeleted(start)) return -1;
            long end = IndexMetrics.toAbsolute(workBuffer.getLong());
            long entrySize = end - start;
            validator.isTrue(entrySize >= 0, "assertion failure: negative entry size");
            readableAmount = entrySize - position;
            if (readableAmount < 0 || position < 0) validator.fail("illegal position (" + position + "); entry size =" + entrySize + "; entry id = " + id);
        }
        final int savedOutLimit = out.limit();
        try {
            if (out.remaining() > readableAmount) out.limit(out.position() + (int) readableAmount);
            int amountRead = entryFile.read(out, start + position);
            validator.isTrue(amountRead >= 0);
            return amountRead;
        } finally {
            if (out.limit() != savedOutLimit) out.limit(savedOutLimit);
        }
    }

    public void getEntry(long id, ByteBuffer out, ByteBuffer workBuffer) throws IOException {
        workBuffer.clear();
        index.readOffsets(id, workBuffer);
        workBuffer.flip();
        long start = workBuffer.getLong();
        if (IndexMetrics.isDeleted(start)) return;
        long end = IndexMetrics.toAbsolute(workBuffer.getLong());
        final int entrySize = (int) (end - start);
        if (out.remaining() < entrySize) validator.fail("The out-buffer's remaining bytes are too few. " + "Needed " + entrySize + "; got " + out.remaining() + " (id = " + id + ", this = " + this + ")");
        final int savedOutLimit = out.limit();
        try {
            out.limit(out.position() + entrySize);
            helper.readRemaining(entryFile, out, start);
        } finally {
            out.limit(savedOutLimit);
        }
    }

    /**
     * @see #newSubFileChannelImpl(FileChannel, long, long, boolean)
     */
    public FileChannel getEntryChannel(long id) throws IOException {
        return getEntryChannelImpl(id, false);
    }

    public FileChannel getEntryUpdateChannel(long id) throws IOException {
        return getEntryChannelImpl(id, true);
    }

    private FileChannel getEntryChannelImpl(long id, boolean update) throws IOException {
        ByteBuffer workBuffer = ByteBuffer.allocate(16);
        index.readOffsets(id, workBuffer);
        workBuffer.flip();
        long start = workBuffer.getLong();
        if (IndexMetrics.isDeleted(start)) return null;
        long end = IndexMetrics.toAbsolute(workBuffer.getLong());
        return newSubFileChannelImpl(entryFile, start, end, update);
    }

    /**
     * Returns the implementation-specific <code>SubFileChannel</code>.
     * This method provides a hook for subclasses.
     * 
     * @see #getEntryChannel(long)
     */
    protected SubFileChannel newSubFileChannelImpl(FileChannel entryFile, long start, long end, boolean update) throws IOException {
        AccessMode mode = update ? AccessMode.BOUNDED_UPDATE : AccessMode.READ_ONLY;
        return new SubFileChannel(entryFile, mode, start, end);
    }

    /**
     * Closes this segment and its underlying streams.  Note if this instance's underlying
     * streams are shared by other instances of this class, then invoking this method
     * will effectively close those instances as well.
     * <p/>
     * It's OK to invoke this method multiple times.
     */
    public void close() throws IOException {
        this.entryFile.force(true);
        this.entryFile.close();
        this.index.close();
        closed = true;
    }

    /**
     * Determines whether the instance is still alive.  An instance is no longer
     * alive once closed.
     */
    public boolean isAlive() {
        return !closed;
    }

    /**
     * Flushes out the backing storage streams.
     */
    public void flush() throws IOException {
        this.entryFile.force(false);
        this.index.flush();
    }

    /**
     * Determines whether the instance is in auto-commit mode.  When <em>not</em>
     * in auto-commit mode, entry insertions (and also kills) are not committed to the backing files
     * until {@link #commit()} is called; deletions <em>may</em> be committed right away.
     * <p/>
     * By default, <code>BaseSegment</code>s are created in auto-commit mode.
     * 
     * @see #commit()
     * @see #setAutoCommit(boolean)
     */
    public boolean isAutoCommit() {
        return index.isAutoCommit();
    }

    /**
     * Sets the auto-commit property.
     */
    public void setAutoCommit(boolean autoCommit) {
        index.setAutoCommit(autoCommit);
    }

    /**
     * Commits the entry count.  This only matters if the instance is <em>not</em> in
     * auto-commit mode.
     * 
     * @see #isAutoCommit()
     * @see #setAutoCommit(boolean)
     */
    public void commit() throws IOException {
        index.commit();
    }

    /**
     * Appends the given <code>segment</code> to this segment.  The contents of entries
     * marked "deleted" are not appended to this segment.  The segment's
     * base ID is expected to be close to, but &ge; the next ID of
     * this segment.  Specifically, the segment's base ID may be no smaller than
     * this segment's <em>next ID</em>, and no larger than <em>next ID</em> + 1024.
     * <p/>
     * This method simply invokes {@link #append(BaseSegment, int) append(segment, 1024)}.
     *
     * @see #append(BaseSegment, int)
     */
    public void append(BaseSegment segment) throws IOException {
        append(segment, 1024);
    }

    /**
     * Appends the given <code>segment</code> to this segment.  The contents of entries
     * marked "deleted" are not appended to this segment.  The segment's
     * base ID is expected to be close to, but &ge; the next ID of
     * this segment.  The <code>maxIdGap</code> parameter specifies minimum requirements
     * for the proximity of IDs in the segment to be appended.
     * <p/>
     * The <code>append</code> method is designed to support merging segments.
     * <p/>
     * <h4>Implementation note:</h4>
     * This method changes the state of the underlying entry file.  Specifically,
     * on return from this method, the entry stream is positioned at the end of the
     * file; the state of the source segment, together with that of its underlying
     * streams, however, remains unchanged.
     * 
     * @see #append(BaseSegment)
     */
    public void append(BaseSegment segment, int maxIdGap) throws IOException {
        if (maxIdGap < 0) validator.fail("Illegal max gap arg: " + maxIdGap);
        final long idGap = segment.getBaseId() - getNextId();
        if (idGap < 0) validator.fail("ID overlap [this,segment]: [" + this + "," + segment + "]");
        if (idGap > maxIdGap) validator.fail("Segment too large. [maxGap, gap]: [" + maxIdGap + "," + idGap + "]");
        final boolean autoCommitMode = index.isAutoCommit();
        index.setAutoCommit(false);
        try {
            entryFile.position(index.getLastOffset());
            long id = idGap == 0 ? segment.getBaseId() : index.killNext((int) idGap);
            assert id == segment.getBaseId();
            if (segment.getEntryCount() != 0) {
                ByteBuffer buffer = allocateWorkBuffer();
                final int maxSweep = buffer.capacity() / Word.LONG.getWidth() - 1;
                final long UNSET_VALUE = -1;
                long unwrittenStartOffset = UNSET_VALUE;
                long previousReadOffset = UNSET_VALUE;
                buffer.limit(0);
                while (id <= segment.getNextId()) {
                    if (!buffer.hasRemaining()) {
                        buffer.clear();
                        int count = (int) Math.min(segment.getNextId() - id, maxSweep);
                        segment.index.readOffsets(id, buffer, count);
                        buffer.flip();
                    }
                    long lastReadOffset = buffer.getLong();
                    if (IndexMetrics.isDeleted(lastReadOffset)) {
                        if (previousReadOffset != UNSET_VALUE) {
                            assert unwrittenStartOffset != UNSET_VALUE;
                            final long endOffset = IndexMetrics.toAbsolute(lastReadOffset);
                            index.pushNext((int) (endOffset - previousReadOffset));
                            helper.transferTo(segment.entryFile, unwrittenStartOffset, endOffset - unwrittenStartOffset, entryFile);
                            unwrittenStartOffset = UNSET_VALUE;
                            previousReadOffset = UNSET_VALUE;
                        }
                        index.killNext();
                    } else {
                        if (unwrittenStartOffset == UNSET_VALUE) unwrittenStartOffset = lastReadOffset; else index.pushNext((int) (lastReadOffset - previousReadOffset));
                        previousReadOffset = lastReadOffset;
                    }
                    ++id;
                }
                if (unwrittenStartOffset != UNSET_VALUE) helper.transferTo(segment.entryFile, unwrittenStartOffset, segment.index.getLastOffset() - unwrittenStartOffset, entryFile);
            }
            if (autoCommitMode) index.commit();
        } finally {
            index.setAutoCommit(autoCommitMode);
        }
    }

    /**
     * Returns a work buffer for the current thread of execution.
     * Currently, only used by the {@link #append(BaseSegment, int) append}
     * method.  The base class returns a newly allocated 1kb direct buffer.
     * A better implementation may involve perhaps the use of larger
     * thread-local buffer.  Such refinements are deferred to subclasses.
     * 
     * @return the base implementation return a newly allocated 1kB direct buffer
     */
    protected ByteBuffer allocateWorkBuffer() {
        return ByteBuffer.allocateDirect(1024);
    }
}
