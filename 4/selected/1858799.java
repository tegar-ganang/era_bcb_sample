package com.faunos.skwish.sys.mgr;

import static com.faunos.skwish.sys.mgr.FileConventions.ABORTED_DIR;
import static com.faunos.skwish.sys.mgr.FileConventions.COMMITTED_DIR;
import static com.faunos.skwish.sys.mgr.FileConventions.LOCK_FILE;
import static com.faunos.skwish.sys.mgr.FileConventions.PENDING_DIR;
import static com.faunos.skwish.sys.mgr.FileConventions.TXN_GAP_FILE;
import static com.faunos.skwish.sys.mgr.FileConventions.UNIT_DIR_FILTER;
import static com.faunos.skwish.sys.mgr.FileConventions.VERSION_FILE;
import static com.faunos.skwish.sys.mgr.FileConventions.VERSION_PREFIX;
import static com.faunos.skwish.sys.mgr.FileConventions.abortedTxnsTimestamp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.faunos.skwish.NotFoundException;
import com.faunos.skwish.Segment;
import com.faunos.skwish.SegmentStore;
import com.faunos.skwish.SegmentStoreException;
import com.faunos.skwish.TxnSegment;
import com.faunos.skwish.sys.BaseSegment;
import com.faunos.skwish.sys.filters.DeleteSetFilterSegment;
import com.faunos.skwish.sys.filters.UnionSegment;
import com.faunos.util.Validator;
import com.faunos.util.Version;
import com.faunos.util.cc.XprocLock;
import com.faunos.util.io.ChannelUtil;
import com.faunos.util.io.Word;
import com.faunos.util.test.AbbreviatedFilepath;

/**
 * The <code>SegmentStore</code> implementation.
 *
 * @author Babak Farhang
 */
public class Store extends SegmentStore {

    public enum StartMode {

        LOAD, LOAD_OR_CREATE, CREATE;

        boolean mustExist() {
            return this == LOAD;
        }

        boolean mustNotExist() {
            return this == CREATE;
        }
    }

    static Validator<SegmentStoreException> validator() {
        return validator;
    }

    public static final Version VERSION = new Version(0, 1, 0);

    protected static final ChannelUtil<SegmentStoreException> channelHelper = new ChannelUtil<SegmentStoreException>(SegmentStoreException.class);

    private static final Logger logger = Logger.getLogger(Store.class.getName());

    private final File rootDir;

    private final XprocLock xprocLock;

    private final Collector collector;

    private final TxnGapTable txnGapTable;

    private final File pendingDir;

    private boolean closed;

    /**
     * The lock (synchronization object) used to serialize
     * concurrent updates.
     */
    private final Object updateLock = new Object();

    public Version getFileVersion() throws IOException {
        File versionFile = new File(rootDir, VERSION_FILE);
        FileChannel channel = new FileInputStream(versionFile).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(64);
        buffer.limit(VERSION_PREFIX.length());
        channelHelper.readRemaining(channel, buffer);
        buffer.flip();
        String prefix = new String(buffer.array(), 0, buffer.limit());
        validator.isTrue(prefix.equals(VERSION_PREFIX), "expected prefix \"" + VERSION_PREFIX + "\" not found");
        buffer.clear().limit(8);
        channelHelper.readRemaining(channel, buffer);
        channel.close();
        buffer.flip();
        return new Version(buffer.getLong());
    }

    private final String logName;

    protected Store(File rootDir, StartMode mode) throws IOException {
        validator.notNull(mode, "null mode");
        this.rootDir = rootDir;
        logName = "[" + new AbbreviatedFilepath(rootDir) + "]: ";
        logger.info(logName + "initializing segment store: " + mode);
        logger.finer(logName + "root directory: " + rootDir);
        if (!mode.mustExist()) {
            rootDir.mkdirs();
        }
        if (!rootDir.isDirectory()) {
            validator.fail(logName + "the specified root directory could not be " + (mode.mustExist() ? "created: " : "found: ") + rootDir);
        }
        this.xprocLock = new XprocLock(new File(rootDir, LOCK_FILE));
        xprocLock.lock();
        File versionFile = new File(rootDir, VERSION_FILE);
        boolean create;
        if (versionFile.exists()) {
            validator.isFalse(mode.mustNotExist(), logName + VERSION_FILE + " already exists");
            getFileVersion();
            create = false;
        } else {
            validator.isFalse(mode.mustExist(), logName + "could not find " + VERSION_FILE);
            createVersionFile(versionFile);
            create = true;
        }
        File committedDir = new File(rootDir, COMMITTED_DIR);
        this.collector = new Collector(committedDir, create);
        {
            File txnGapFile = new File(committedDir, TXN_GAP_FILE);
            FileChannel channel = new RandomAccessFile(txnGapFile, "rw").getChannel();
            this.txnGapTable = new TxnGapTable(channel, logName);
        }
        File pendingDirWork = new File(rootDir, PENDING_DIR);
        if (create) {
            pendingDirWork.mkdir();
            validator.isTrue(pendingDirWork.isDirectory(), "failed to create pending directory");
            validator.isTrue(pendingDirWork.list().length == 0, "non-empty pending directory");
        } else {
            validator.isTrue(pendingDirWork.isDirectory(), "pending directory not found");
            if (pendingDirWork.listFiles(UNIT_DIR_FILTER).length != 0) {
                logger.warning("Moving aborted transactions to " + ABORTED_DIR + " directory..");
                File abortedDir = new File(rootDir, ABORTED_DIR);
                abortedDir.mkdir();
                validator.isTrue(abortedDir.isDirectory(), "failed to create aborted directory");
                File backupDir = new File(abortedDir, abortedTxnsTimestamp());
                validator.isTrue(pendingDirWork.renameTo(backupDir), "failed to backup " + PENDING_DIR + " dir to " + new AbbreviatedFilepath(backupDir));
                pendingDirWork = new File(rootDir, PENDING_DIR);
                pendingDirWork.mkdir();
                validator.isTrue(pendingDirWork.isDirectory(), "failed to create empty " + PENDING_DIR + " dir");
            }
        }
        this.pendingDir = pendingDirWork;
    }

    private void createVersionFile(File versionFile) throws IOException {
        FileChannel channel = new FileOutputStream(versionFile).getChannel();
        ByteBuffer buffer = ByteBuffer.wrap(VERSION_PREFIX.getBytes());
        channelHelper.writeRemaining(channel, buffer);
        buffer = ByteBuffer.allocate(8);
        buffer.putLong(VERSION.toLong()).flip();
        channelHelper.writeRemaining(channel, buffer);
        channel.close();
    }

    public File getRootDirectory() {
        return rootDir;
    }

    public String getUri() {
        return rootDir.getPath();
    }

    @Override
    public Segment getReadOnlySegment() throws IOException {
        return collector.getReadOnlyView();
    }

    public boolean isOpen() {
        return !closed;
    }

    private class TSegment extends TxnSegment {

        private UnitDir txnUnit;

        private MutableBaseIdIndex index;

        private BaseSegment newSegment;

        private FileBackedDeleteSet deleteSet;

        private Segment implDelegate;

        private long txnCommitId = -1;

        private long txnId = -1;

        private long txnBaseId;

        public TSegment() throws IOException {
            final Segment readOnlySnapShotSegment = collector.getReadOnlySnapShot();
            this.txnBaseId = readOnlySnapShotSegment.getNextId();
            final long baseId = readOnlySnapShotSegment.getBaseId();
            this.txnUnit = UnitDir.writeNewInstance(pendingDir, collector.nextUnitId());
            {
                FileChannel entryChannel = new RandomAccessFile(txnUnit.getEntryFile(), "rw").getChannel();
                FileChannel indexChannel = new RandomAccessFile(txnUnit.getIndexFile(), "rw").getChannel();
                this.index = MutableBaseIdIndex.writeNewIndex(indexChannel, Word.INT, getTxnBaseId(), 0);
                newSegment = new BaseSegment(index, entryChannel);
            }
            {
                FileChannel deleteSetChannel = new RandomAccessFile(txnUnit.getDeleteSetFile(), "rw").getChannel();
                this.deleteSet = FileBackedDeleteSet.writeNewInstance(deleteSetChannel, baseId, getTxnBaseId());
            }
            {
                Segment[] segments = new Segment[] { new DeleteSetFilterSegment(readOnlySnapShotSegment, deleteSet), newSegment };
                this.implDelegate = new UnionSegment(segments);
            }
            if (logger.isLoggable(Level.FINER)) logger.finer(logName + "created new txn " + txnUnit.getDir().getName() + " (txn base id is " + txnBaseId + ")");
        }

        @Override
        public synchronized long getTxnId() throws IOException {
            if (!isAlive()) throw new IllegalStateException("instance not alive (committed or discarded)");
            if (txnId == -1) txnId = txnGapTable.newTxnId();
            return txnId;
        }

        @Override
        public synchronized long commit() throws IOException {
            logger.fine(logName + "committing " + txnUnit.getDir().getName());
            validator.isTrue(isAlive(), "txn not alive");
            this.implDelegate = collector.getReadOnlyView();
            final long txnCommitId;
            final long savedTxnBaseId = txnBaseId;
            txnBaseId = -1;
            synchronized (updateLock) {
                txnCommitId = collector.getReadOnlyView().getNextId();
                if (txnId != -1) {
                    long txnCommitGap = txnCommitId - savedTxnBaseId;
                    txnGapTable.setGapValue(txnCommitGap, txnId);
                }
                final boolean hasNewSegment = newSegment.getEntryCount() > 0;
                if (hasNewSegment) {
                    index.setBaseId(txnCommitId);
                    newSegment.close();
                } else {
                    newSegment.close();
                    txnUnit.purgeSegment();
                }
                final boolean hasDeleteSet = deleteSet.getCount() > 0;
                deleteSet.close();
                if (!hasDeleteSet) {
                    txnUnit.purgeDeleteSet();
                    if (txnUnit.isPurgeable()) validator.isTrue(txnUnit.purge(), "failed to purge empty txn");
                }
                if (!txnUnit.isPurged()) collector.commitUnit(txnUnit);
            }
            this.txnBaseId = savedTxnBaseId;
            this.txnCommitId = txnCommitId;
            if (logger.isLoggable(Level.FINE)) logger.fine(logName + "committed " + txnUnit.getDir().getName() + " (txn base id = " + txnBaseId + ") (txn commit id = " + txnCommitId + ")");
            return txnCommitId;
        }

        @Override
        public synchronized void discard() throws IOException {
            logger.fine(logName + "discarding " + txnUnit.getDir().getName());
            if (isDiscarded()) return;
            validator.isTrue(!isCommitted(), "txn already committed");
            txnBaseId = -1;
            this.implDelegate = collector.getReadOnlyView();
            newSegment.close();
            deleteSet.close();
            txnUnit.purgeSegment();
            txnUnit.purgeDeleteSet();
            if (!txnUnit.purge()) validator.fail("failed to purge txn unit: " + txnUnit);
        }

        @Override
        public synchronized long getTxnBaseId() {
            return txnBaseId;
        }

        @Override
        public synchronized long getTxnCommitId() {
            return txnCommitId;
        }

        @Override
        public synchronized void delete(long id, int count, boolean purge) throws IOException {
            implDelegate.delete(id, count, purge);
        }

        @Override
        public synchronized long getBaseId() {
            return implDelegate.getBaseId();
        }

        @Override
        public synchronized int getEntryPart(long id, long position, ByteBuffer out, ByteBuffer workBuffer) throws IOException {
            return implDelegate.getEntryPart(id, position, out, workBuffer);
        }

        @Override
        public synchronized void getEntry(long id, ByteBuffer out, ByteBuffer workBuffer) throws IOException {
            implDelegate.getEntry(id, out, workBuffer);
        }

        @Override
        public synchronized FileChannel getEntryChannel(long id) throws IOException {
            return implDelegate.getEntryChannel(id);
        }

        public synchronized FileChannel getEntryUpdateChannel(long id) throws IOException {
            if (!isAlive()) validator().fail("txn is not alive");
            if (id < getTxnBaseId()) validator().fail("cannot update entry below txn base id (=" + getTxnBaseId() + "); id argument was " + id);
            return newSegment.getEntryUpdateChannel(id);
        }

        public synchronized FileChannel getEntryInsertionChannel() throws IOException {
            if (!isAlive()) validator().fail("txn is not alive");
            return newSegment.getEntryInsertionChannel();
        }

        @Override
        public synchronized long getEntryCount() {
            return implDelegate.getEntryCount();
        }

        @Override
        public synchronized long getEntrySize(long id, ByteBuffer workBuffer) throws IOException {
            return implDelegate.getEntrySize(id, workBuffer);
        }

        @Override
        public synchronized long getNextId() {
            return implDelegate.getNextId();
        }

        @Override
        public synchronized long insertEntry(ByteBuffer entry) throws IOException {
            return implDelegate.insertEntry(entry);
        }

        @Override
        public synchronized long insertEntry(ReadableByteChannel entry) throws IOException {
            return implDelegate.insertEntry(entry);
        }

        @Override
        public synchronized boolean isReadOnly() {
            return implDelegate.isReadOnly();
        }

        @Override
        public synchronized long killNext(int count) throws IOException {
            return implDelegate.killNext(count);
        }
    }

    @Override
    public TxnSegment newTransaction() throws IOException {
        logger.fine(logName + "creating new transaction segment");
        return new TSegment();
    }

    @Override
    public long getTxnCommitIdGap(long txnId) throws IOException, NotFoundException {
        return txnGapTable.getCommitIdGap(txnId);
    }

    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try {
            logger.info(logName + "closing segment store");
            collector.close();
            xprocLock.release();
            logger.info(logName + "closed.");
        } finally {
            StoreProvider.INSTANCE.notifyClosed(this);
        }
    }
}
