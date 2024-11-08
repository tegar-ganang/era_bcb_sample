package com.faunos.skwish.sys.filters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import com.faunos.skwish.Segment;

/**
 * A read-only, empty segment.  The only thing that distinguishes
 * one instance from another is its {@linkplain #getBaseId() baseId}.
 *
 * @author Babak Farhang
 */
public final class EmptySegment extends Segment {

    /**
     * The default empty segment with baseId set to zero.
     */
    public static final Segment DEFAULT = new EmptySegment(0);

    private final long baseId;

    private EmptySegment(long baseId) {
        this.baseId = baseId;
    }

    /**
     * Creates and returns a new empty segment with the given
     * <code>baseId</code>.
     * 
     * @param baseId  must not be negative
     * @see #getBaseId()
     */
    public static Segment forBaseId(long baseId) {
        validator.nonNegative(baseId);
        return new EmptySegment(baseId);
    }

    @Override
    public void delete(long id, int count, boolean purge) throws IOException {
        failOutOfRange(id);
    }

    private long failOutOfRange(long id) {
        validator.fail("out of range arguments [id,this]: " + id + "," + this);
        return 0;
    }

    /**
     * Returns the base ID.  Note this is the only property that
     * distinguishes one empty, read-only segment from another
     * (whatever the implementation class).
     */
    @Override
    public long getBaseId() {
        return baseId;
    }

    @Override
    public void getEntry(long id, ByteBuffer out, ByteBuffer workBuffer) throws IOException {
        failOutOfRange(id);
    }

    @Override
    public FileChannel getEntryChannel(long id) throws IOException {
        failOutOfRange(id);
        return null;
    }

    @Override
    public long getEntryCount() {
        return 0;
    }

    @Override
    public long getEntrySize(long id, ByteBuffer workBuffer) throws IOException {
        failOutOfRange(id);
        return 0;
    }

    @Override
    public long getNextId() {
        return baseId;
    }

    @Override
    public long insertEntry(ByteBuffer entry) throws IOException {
        return 0;
    }

    @Override
    public long insertEntry(ReadableByteChannel entry) throws IOException {
        return failWrite();
    }

    /**
     * Returns <code>true</code>.
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public long killNext(int count) throws IOException {
        return failWrite();
    }

    private long failWrite() {
        validator.fail("attempt to write to read-only instance [this]: " + this);
        return 0;
    }

    @Override
    public int getEntryPart(long id, long position, ByteBuffer out, ByteBuffer workBuffer) throws IOException {
        return -1;
    }
}
