package com.faunos.skwish.sys.mgr;

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
import com.faunos.skwish.Segment;
import com.faunos.skwish.SegmentManager;
import com.faunos.skwish.SegmentManagerException;
import com.faunos.skwish.TxnSegment;
import com.faunos.skwish.spi.Provider;
import com.faunos.skwish.sys.BaseSegment;
import com.faunos.skwish.sys.filters.DeleteSetFilterSegment;
import com.faunos.skwish.sys.filters.UnionSegment;
import com.faunos.util.Validator;
import com.faunos.util.Version;
import com.faunos.util.io.ChannelUtil;
import com.faunos.util.io.Word;

/**
 * The <code>SegmentManager</code> implementation.
 *
 * @author Babak Farhang
 */
public class SystemManager extends SegmentManager {

    static Validator<SegmentManagerException> validator() {
        return validator;
    }

    public static final Version VERSION = new Version(0, 1, 0);

    private static class ProviderHolder {

        static final Provider provider = new Provider() {

            public SegmentManager loadInstance(String uri) throws IOException {
                return new SystemManager(new File(uri));
            }

            public SegmentManager writeNewInstance(String uri) throws IOException {
                return new SystemManager(new File(uri), true);
            }
        };
    }

    public static Provider getProvider() {
        return ProviderHolder.provider;
    }

    protected static final ChannelUtil<SegmentManagerException> channelHelper = new ChannelUtil<SegmentManagerException>(SegmentManagerException.class);

    private static final Logger logger = Logger.getLogger(SystemManager.class.getName());

    private final File rootDir;

    private final Collector collector;

    private final File pendingDir;

    /**
     * The lock (synchronization object) used to serialize
     * concurrent updates.
     */
    private final Object updateLock = new Object();

    /**
     * Reads the given root directory and creates a new instance.
     */
    public SystemManager(File rootDir) throws IOException {
        this(rootDir, false);
    }

    public Version getFileVersion() throws IOException {
        File versionFile = new File(rootDir, FileConventions.VERSION_FILE);
        FileChannel channel = new FileInputStream(versionFile).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(64);
        buffer.limit(FileConventions.VERSION_PREFIX.length());
        channelHelper.readRemaining(channel, buffer);
        buffer.flip();
        String prefix = new String(buffer.array(), 0, buffer.limit());
        validator.isTrue(prefix.equals(FileConventions.VERSION_PREFIX), "expected prefix \"" + FileConventions.VERSION_PREFIX + "\" not found");
        buffer.clear().limit(8);
        channelHelper.readRemaining(channel, buffer);
        channel.close();
        buffer.flip();
        return new Version(buffer.getLong());
    }

    protected SystemManager(File rootDir, boolean create) throws IOException {
        this.rootDir = rootDir;
        logger.info((create ? "creating " : "loading ") + "managed segments under " + rootDir.getName());
        logger.finer("segment root directory: " + rootDir);
        if (create) rootDir.mkdirs();
        if (!rootDir.isDirectory()) validator.fail("the specified root directory could not be " + (create ? "created: " : "found: ") + rootDir);
        if (create) {
            File versionFile = new File(rootDir, FileConventions.VERSION_FILE);
            validator.isTrue(!versionFile.exists(), FileConventions.VERSION_FILE + " already exists");
            FileChannel channel = new FileOutputStream(versionFile).getChannel();
            ByteBuffer buffer = ByteBuffer.wrap(FileConventions.VERSION_PREFIX.getBytes());
            channelHelper.writeRemaining(channel, buffer);
            buffer = ByteBuffer.allocate(8);
            buffer.putLong(VERSION.toLong()).flip();
            channelHelper.writeRemaining(channel, buffer);
            channel.close();
        }
        this.collector = new Collector(new File(rootDir, FileConventions.COMMITED_DIR), create);
        this.pendingDir = new File(rootDir, FileConventions.PENDING_DIR);
        if (create) {
            pendingDir.mkdir();
            validator.isTrue(pendingDir.isDirectory(), "failed to create pending directory");
            validator.isTrue(pendingDir.list().length == 0, "non-empty pending directory");
        } else validator.isTrue(pendingDir.isDirectory(), "pending directory not found");
    }

    public File getRootDirectory() {
        return rootDir;
    }

    @Override
    public Segment getReadOnlySegment() throws IOException {
        return collector.getReadOnlyView();
    }

    private class TSegment extends TxnSegment {

        private UnitDir txnUnit;

        private MutableBaseIdIndex index;

        private BaseSegment newSegment;

        private FileBackedDeleteSet deleteSet;

        private Segment implDelegate;

        private long txnCommitId = -1;

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
            if (logger.isLoggable(Level.FINER)) logger.finer("created new txn " + txnUnit.getDir().getName() + " (txn base id is " + txnBaseId + ")");
        }

        @Override
        public synchronized long commit() throws IOException {
            logger.fine("committing " + txnUnit.getDir().getName());
            validator.isTrue(isAlive(), "txn not alive");
            this.implDelegate = collector.getReadOnlyView();
            final long txnCommitId;
            final long savedTxnBaseId = txnBaseId;
            txnBaseId = -1;
            synchronized (updateLock) {
                txnCommitId = collector.getReadOnlyView().getNextId();
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
            if (logger.isLoggable(Level.FINE)) logger.fine("committed " + txnUnit.getDir().getName() + " (txn base id = " + txnBaseId + ") (txn commit id = " + txnCommitId + ")");
            return txnCommitId;
        }

        @Override
        public synchronized void discard() throws IOException {
            logger.fine("discarding " + txnUnit.getDir().getName());
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
        public synchronized void getEntry(long id, ByteBuffer out, ByteBuffer workBuffer) throws IOException {
            implDelegate.getEntry(id, out, workBuffer);
        }

        @Override
        public synchronized FileChannel getEntryChannel(long id) throws IOException {
            return implDelegate.getEntryChannel(id);
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
        logger.fine("creating new transaction segment");
        return new TSegment();
    }

    public void close() throws IOException {
        logger.info("closing segments managed in " + getRootDirectory().getName());
        collector.close();
        logger.info("closed " + getRootDirectory().getName());
    }
}
