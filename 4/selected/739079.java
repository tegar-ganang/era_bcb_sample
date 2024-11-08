package com.faunos.skwish.sys.mgr;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.faunos.skwish.SegmentStoreException;
import com.faunos.skwish.sys.BaseSegment;
import com.faunos.util.io.SubFileChannel;
import com.faunos.util.io.Word;
import com.faunos.util.io.SubFileChannel.AccessMode;

/**
 * A directory on the file system containing a unit of work data. The work data
 * may include an [offset] index file, an entry [contents] file (which together
 * make a {@link BaseSegment}), and a deleted ID file.
 * 
 * @see FileConventions
 * 
 * @author Babak Farhang
 */
public class UnitDir {

    /**
     * A <code>BaseSegment</code> backed with files in the enclosing
     * instance's directory.
     * 
     * 
     * @author Babak Farhang
     */
    public class Seg extends BaseSegment {

        private final Lock appendSourceLock = new ReentrantLock();

        private volatile boolean purgeable;

        private final Lock appendLock = new ReentrantLock();

        /**
         * Access protected constructor.
         */
        protected Seg(FileChannel offsetFile, FileChannel entryFile) throws IOException {
            super(offsetFile, entryFile);
        }

        /**
         * Appends the given source segment while holding locks
         * on both this, the destination, and source segments.
         * This method assumes that the necessary locks are not
         * already owned by another; if not, a {@link SegmentStoreException}
         * is thrown.  Therefore, the caller will typically already own the
         * <a href="#locksheld">locks to be held</a> before invoking this
         * method. On return, the locks are released, and if successful (no
         * exception raised), the argument <code>seg</code> instance's
         * {@linkplain #isPurgeable() purgeable} property is set to <code>
         * true</code>.
         * <p/>
         * This append implementation is designed to prevent a segment from
         * being appended (as either source or destination) while it is
         * concurrently involved in another append operation, <em>and</em>
         * from concurrent deletes in the source of an append.
         * 
         * <a name="locksheld"/>
         * <h4>Locks held</h4>
         * Locks are acquired in the following order.  First, the
         * <code>src</code> instance's append lock is acquired,
         * then <em>this</em> (destination) instance's append lock,
         * and finally the <code>src</code> instance's append-<em>source</em>
         * lock.
         * <p/>
         * The locks held are categorized below (not in the order they are acquired).
         * <ul>
         * <li> This instance (the destination segment):
         * <ol>
         * <li>{@linkplain #getAppendLock() appendLock}</li>
         * </ol>
         * </li>
         * <li> The argument instance (the source segment):
         * <ol>
         * <li>{@linkplain #getAppendLock() appendLock}</li>
         * <li>{@linkplain #getAppendSourceLock() appendSourceLock}</li>
         * </ol>
         * </li>
         * </ul>
         * 
         * @see #isPurgeable()
         * @see #getAppendSourceLock()
         * @see #delete(long, int, boolean)
         */
        public void appendWithLock(Seg seg) throws IOException {
            validator.isTrue(seg != this, "assertion failure: attempt to append to self");
            usageCount.incrementAndGet();
            appendLock.lock();
            if (!seg.appendLock.tryLock()) {
                usageCount.decrementAndGet();
                appendLock.unlock();
                validator.fail("assertion failure: deadlock detected");
            }
            seg.appendSourceLock.lock();
            try {
                validator.isTrue(!isPurgeable(), "assertion failure: purged destination segment");
                validator.isTrue(!seg.isPurgeable(), "assertion failure: purged source segment");
                super.append(seg, 1024);
                seg.purgeable = true;
            } finally {
                seg.appendSourceLock.unlock();
                seg.appendLock.unlock();
                appendLock.unlock();
                usageCount.decrementAndGet();
            }
        }

        /**
         * Determines whether this instance is purgeable. Once a segment has
         * been appended to another segment, it is considered purgeable.
         * 
         * @return <code>true</code>, if and only if this instance has been
         *         appended to another <code>UnitDir.Seg</code> instance.
         */
        public boolean isPurgeable() {
            return purgeable;
        }

        /**
         * Returns the lock held when this instance is the source of an append.
         * <p/>
         * We acquire this lock when deleting entries from the segment: we
         * don't want to be deleting from this segment while it is being
         * appended to another segment.
         * 
         * @see #appendWithLock(com.faunos.skwish.sys.mgr.UnitDir.Seg)
         * @see #isPurgeable()
         */
        public Lock getAppendSourceLock() {
            return appendSourceLock;
        }

        /**
         * Returns the lock held when this instance is either the source or
         * destination of an append.
         * 
         * @see #appendWithLock(com.faunos.skwish.sys.mgr.UnitDir.Seg)
         */
        public Lock getAppendLock() {
            return appendLock;
        }

        /**
         * Checks that {@linkplain #getAppendSourceLock() appendSourceLock},
         * is not owned by another thread, and also that the instance is not
         * {@linkplain #isPurgeable() purgeable}, and then proceeds with the
         * base delete implementation.
         * 
         * @see #getAppendSourceLock()
         */
        @Override
        public void delete(long id, int count, boolean purge) throws IOException {
            if (!appendSourceLock.tryLock()) validator.fail("dead lock detected");
            usageCount.incrementAndGet();
            try {
                validator.isTrue(!isPurgeable(), "assertion failure: attempt to delete in purged segment");
                super.delete(id, count, purge);
            } finally {
                appendSourceLock.unlock();
                usageCount.decrementAndGet();
            }
        }

        @Override
        public void append(BaseSegment segment, int maxIdGap) throws IOException {
            appendWithLock((Seg) segment);
        }

        /**
         * @see UnitDir#getUsageCount()
         */
        @Override
        public void getEntry(long id, ByteBuffer out, ByteBuffer workBuffer) throws IOException {
            usageCount.incrementAndGet();
            try {
                super.getEntry(id, out, workBuffer);
            } finally {
                usageCount.decrementAndGet();
            }
        }

        /**
         * @see UnitDir#getUsageCount()
         */
        @Override
        public long getEntrySize(long id, ByteBuffer workBuffer) throws IOException {
            usageCount.incrementAndGet();
            try {
                return super.getEntrySize(id, workBuffer);
            } finally {
                usageCount.decrementAndGet();
            }
        }

        /**
         * Returns a <code>SubFileChannel</code> that updates the usage count.
         * 
         * @see UnitDir#getUsageCount()
         */
        @Override
        protected SubFileChannel newSubFileChannelImpl(FileChannel entryFile, long start, long end, boolean update) throws IOException {
            return new EntryChannel(entryFile, update ? AccessMode.BOUNDED_UPDATE : AccessMode.READ_ONLY, start, end);
        }

        /**
         * Returns the enclosing <code>UnitDir</code> instance that created
         * this segment.
         */
        public UnitDir getUnitDir() {
            return UnitDir.this;
        }

        /**
         * Determines whether this instance covers the given
         * <code>segment</code>. This instance covers another if the range of
         * IDs in the other segment is also contained in this segment, and if
         * this segment's number of entries is greater than that of the other.
         * <p/> A better way to say this: a segment <em>covers</em> another
         * segment if its set of entry IDs is a proper superset of that of the
         * other.
         */
        public boolean covers(Seg segment) {
            if (getNextId() < segment.getNextId()) return false;
            if (getBaseId() > segment.getBaseId()) return false;
            assert getEntryCount() != segment.getEntryCount() || segment == this;
            return getEntryCount() > segment.getEntryCount();
        }

        public String getName() {
            return getUnitDir().getDir().getName();
        }

        public String toString() {
            return getName() + "[" + getBaseId() + "," + getNextId() + "]";
        }
    }

    /**
     * Entry file channel that increments the usage count on instantiation, and
     * decrements it on closing.
     */
    private class EntryChannel extends SubFileChannel {

        public EntryChannel(FileChannel file, AccessMode mode, long startPosition, long endPosition) throws IOException {
            super(file, mode, startPosition, endPosition);
            usageCount.incrementAndGet();
        }

        private final Object closeLock = new Object();

        private volatile boolean closed;

        @Override
        protected void implCloseChannel() throws IOException {
            if (closed) return;
            synchronized (closeLock) {
                if (closed) return;
                closed = true;
                usageCount.decrementAndGet();
            }
        }

        @Override
        protected void checkAccess() throws IOException {
            if (closed) throw new ClosedChannelException();
            super.checkAccess();
        }

        @Override
        protected void checkWriteAccess() throws IOException {
            if (closed) throw new ClosedChannelException();
            super.checkWriteAccess();
        }
    }

    private final File dir;

    private final AtomicInteger usageCount = new AtomicInteger();

    public UnitDir(File dir) {
        if (!dir.isDirectory()) Store.validator().fail("file arg is not a directory: " + dir);
        this.dir = dir;
        getUnitNumber();
    }

    /**
     * Returns the directory this unit lives in.
     */
    public File getDir() {
        return dir;
    }

    /**
     * Returns the name of the directory this unit lives in.
     */
    public String getName() {
        return getDir().getName();
    }

    /**
     * Returns the usage count.  Used to track lingering users of this units
     * segment after it becomes eligible for purging.
     */
    public int getUsageCount() {
        return usageCount.get();
    }

    /**
     * Returns the index file.
     * 
     * @see FileConventions#INDEX_FILE
     */
    public File getIndexFile() {
        return new File(dir, FileConventions.INDEX_FILE);
    }

    /**
     * Returns the entry file.
     * 
     * @see FileConventions#ENTRY_FILE
     */
    public File getEntryFile() {
        return new File(dir, FileConventions.ENTRY_FILE);
    }

    /**
     * Returns the detete-set file.
     * 
     * @see FileConventions#DELETE_SET_FILE
     */
    public File getDeleteSetFile() {
        return new File(dir, FileConventions.DELETE_SET_FILE);
    }

    /**
     * Purges (deletes) the segment files in the correct order.
     */
    public void purgeSegment() {
        File indexFile = getIndexFile();
        indexFile.delete();
        if (!indexFile.exists()) getEntryFile().delete();
    }

    /**
     * Determines whether the segment files have been purged (deleted). (The
     * instance may never have had a segment.)
     */
    public boolean isSegmentPurged() {
        return !getIndexFile().exists() && !getEntryFile().exists();
    }

    /**
     * Determines whether the instance has a segment.
     */
    public boolean hasSegment() {
        return getIndexFile().exists() && getEntryFile().exists();
    }

    /**
     * Purges (deletes) the delete-set file.
     */
    public void purgeDeleteSet() {
        getDeleteSetFile().delete();
    }

    /**
     * Determines whether the delete-set file exists.
     */
    public boolean hasDeleteSet() {
        return getDeleteSetFile().exists();
    }

    /**
     * Determines whether this unit is purgeable. An instance is purgeable if it
     * neither has a segment, nor delete-set.
     */
    public boolean isPurgeable() {
        return isSegmentPurged() && !hasDeleteSet();
    }

    /**
     * Purges (deletes) this entire unit directory provided it is purgeable.
     * 
     * @return <code>true</code>, if and only if this instance was
     *         {@linkplain #isPurgeable() purgeable}, and has not been already
     *         {@linkplain #isPurged() purged}
     */
    public boolean purge() {
        return isPurgeable() && getDir().delete();
    }

    /**
     * Determines whether this unit has been purged. A unit is purged once its
     * directory no longer exists on the file system.
     */
    public boolean isPurged() {
        return !getDir().isDirectory();
    }

    /**
     * Loads and returns an existing <code>BaseSegment</code> from this
     * directory, if it exists; or <code>null</code>, if not found.
     */
    public Seg loadSegment() throws IOException {
        File indexFile = getIndexFile();
        if (!indexFile.exists()) return null;
        File entryFile = getEntryFile();
        FileChannel indexFileChannel = new RandomAccessFile(indexFile, "rw").getChannel();
        FileChannel entryFileChannel = new RandomAccessFile(entryFile, "rw").getChannel();
        Seg segment = new Seg(indexFileChannel, entryFileChannel);
        return segment;
    }

    /**
     * Loads the delete-set in this unit, if any, and returns it in read-write
     * mode; returns <code>null</code> if there is no delete-set file.
     */
    public FileBackedDeleteSet loadDeleteSet() throws IOException {
        return loadDeleteSet(false);
    }

    /**
     * Loads the delete-set in this unit, if any, and returns it in the
     * specified read/write mode; returns <code>null</code> if there is no
     * delete-set file.
     * 
     * @param readOnly
     *            <code>true</code>, if the delete-set is to be returned in
     *            read-only mode; <code>false</code>, if to be returned in
     *            read-write mode.
     */
    public FileBackedDeleteSet loadDeleteSet(boolean readOnly) throws IOException {
        File deleteSetFile = getDeleteSetFile();
        if (!deleteSetFile.exists()) return null;
        FileChannel deleteSetFileChannel = new RandomAccessFile(deleteSetFile, readOnly ? "r" : "rw").getChannel();
        return FileBackedDeleteSet.loadInstance(deleteSetFileChannel);
    }

    /**
     * Equality semantics are based on the directory location of the instance.
     * 
     * @return <code>true</code>, if and only if the other object is an
     *         instance of this class and its {@linkplain #getDir() directory}
     *         is the same as this one.
     */
    public boolean equals(Object obj) {
        return obj == this || obj instanceof UnitDir && dir.equals(((UnitDir) obj).dir);
    }

    /**
     * Returns a suitable hash code as per the general contract of <code>
     * java.lang.Object</code>.
     * 
     * @see #equals(Object)
     */
    public int hashCode() {
        return dir.hashCode();
    }

    public String toString() {
        return getClass().getSimpleName() + "[" + dir.getPath() + "]";
    }

    /**
     * Creates and returns a new unit in the given parent directory.
     * 
     * @param parentDir
     *            the parent directory in which the instance will be created;
     *            must be an existing directory
     * @param unitNumber
     *            the unit number associated with numbered naming convention for
     *            unit directories
     * 
     * @see FileConventions#toUnitDirname(int)
     */
    public static UnitDir writeNewInstance(File parentDir, int unitNumber) throws IOException {
        File dir = new File(parentDir, FileConventions.toUnitDirname(unitNumber));
        if (!dir.mkdir()) Store.validator().fail("failed to create new unit directory: " + dir);
        return new UnitDir(dir);
    }

    /**
     * Creates an initial empty segment in the given "committed" directory. The
     * segment files are closed on return.
     */
    public static void initInstance(File committedDir) throws IOException {
        File dir = new File(committedDir, FileConventions.toUnitDirname(0));
        Store.validator().isTrue(dir.mkdir(), "failed to create initial unit directory");
        UnitDir unit = new UnitDir(dir);
        File indexFile = unit.getIndexFile();
        File entryFile = unit.getEntryFile();
        FileChannel indexChannel = new RandomAccessFile(indexFile, "rw").getChannel();
        FileChannel entryChannel = new RandomAccessFile(entryFile, "rw").getChannel();
        BaseSegment.writeNewSegment(indexChannel, entryChannel, 0, Word.INT).close();
    }

    public int getUnitNumber() {
        return FileConventions.toUnitDirNumber(getDir().getName());
    }
}
