package org.apache.hadoop.hdfs.server.namenode;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.Math;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory;
import org.apache.hadoop.hdfs.server.namenode.FSImage.NameNodeDirType;
import org.apache.hadoop.hdfs.server.namenode.metrics.NameNodeMetrics;
import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.permission.*;

/**
 * FSEditLog maintains a log of the namespace modifications.
 * 
 */
public class FSEditLog {

    private static final byte OP_INVALID = -1;

    private static final byte OP_ADD = 0;

    private static final byte OP_RENAME = 1;

    private static final byte OP_DELETE = 2;

    private static final byte OP_MKDIR = 3;

    private static final byte OP_SET_REPLICATION = 4;

    @Deprecated
    private static final byte OP_DATANODE_ADD = 5;

    @Deprecated
    private static final byte OP_DATANODE_REMOVE = 6;

    private static final byte OP_SET_PERMISSIONS = 7;

    private static final byte OP_SET_OWNER = 8;

    private static final byte OP_CLOSE = 9;

    private static final byte OP_SET_GENSTAMP = 10;

    private static final byte OP_SET_NS_QUOTA = 11;

    private static final byte OP_CLEAR_NS_QUOTA = 12;

    private static final byte OP_TIMES = 13;

    private static final byte OP_SET_QUOTA = 14;

    private static int sizeFlushBuffer = 512 * 1024;

    private ArrayList<EditLogOutputStream> editStreams = null;

    private FSImage fsimage = null;

    private long txid = 0;

    private long synctxid = 0;

    private long lastPrintTime;

    private boolean isSyncRunning;

    private long numTransactions;

    private long totalTimeTransactions;

    private NameNodeMetrics metrics;

    private static class TransactionId {

        public long txid;

        TransactionId(long value) {
            this.txid = value;
        }
    }

    private static final ThreadLocal<TransactionId> myTransactionId = new ThreadLocal<TransactionId>() {

        protected synchronized TransactionId initialValue() {
            return new TransactionId(Long.MAX_VALUE);
        }
    };

    /**
   * An implementation of the abstract class {@link EditLogOutputStream},
   * which stores edits in a local file.
   */
    private static class EditLogFileOutputStream extends EditLogOutputStream {

        private File file;

        private FileOutputStream fp;

        private FileChannel fc;

        private DataOutputBuffer bufCurrent;

        private DataOutputBuffer bufReady;

        static ByteBuffer fill = ByteBuffer.allocateDirect(512);

        EditLogFileOutputStream(File name) throws IOException {
            super();
            file = name;
            bufCurrent = new DataOutputBuffer(sizeFlushBuffer);
            bufReady = new DataOutputBuffer(sizeFlushBuffer);
            RandomAccessFile rp = new RandomAccessFile(name, "rw");
            fp = new FileOutputStream(rp.getFD());
            fc = rp.getChannel();
            fc.position(fc.size());
        }

        @Override
        String getName() {
            return file.getPath();
        }

        /** {@inheritDoc} */
        @Override
        public void write(int b) throws IOException {
            bufCurrent.write(b);
        }

        /** {@inheritDoc} */
        @Override
        void write(byte op, Writable... writables) throws IOException {
            write(op);
            for (Writable w : writables) {
                w.write(bufCurrent);
            }
        }

        /**
     * Create empty edits logs file.
     */
        @Override
        void create() throws IOException {
            fc.truncate(0);
            fc.position(0);
            bufCurrent.writeInt(FSConstants.LAYOUT_VERSION);
            setReadyToFlush();
            flush();
        }

        @Override
        public void close() throws IOException {
            int bufSize = bufCurrent.size();
            if (bufSize != 0) {
                throw new IOException("FSEditStream has " + bufSize + " bytes still to be flushed and cannot " + "be closed.");
            }
            bufCurrent.close();
            bufReady.close();
            fc.truncate(fc.position());
            fp.close();
            bufCurrent = bufReady = null;
        }

        /**
     * All data that has been written to the stream so far will be flushed.
     * New data can be still written to the stream while flushing is performed.
     */
        @Override
        void setReadyToFlush() throws IOException {
            assert bufReady.size() == 0 : "previous data is not flushed yet";
            write(OP_INVALID);
            DataOutputBuffer tmp = bufReady;
            bufReady = bufCurrent;
            bufCurrent = tmp;
        }

        /**
     * Flush ready buffer to persistent store.
     * currentBuffer is not flushed as it accumulates new log records
     * while readyBuffer will be flushed and synced.
     */
        @Override
        protected void flushAndSync() throws IOException {
            preallocate();
            bufReady.writeTo(fp);
            bufReady.reset();
            fc.force(false);
            fc.position(fc.position() - 1);
        }

        /**
     * Return the size of the current edit log including buffered data.
     */
        @Override
        long length() throws IOException {
            return fc.size() + bufReady.size() + bufCurrent.size();
        }

        private void preallocate() throws IOException {
            long position = fc.position();
            if (position + 4096 >= fc.size()) {
                FSNamesystem.LOG.debug("Preallocating Edit log, current size " + fc.size());
                long newsize = position + 1024 * 1024;
                fill.position(0);
                int written = fc.write(fill, newsize);
                FSNamesystem.LOG.debug("Edit log size is now " + fc.size() + " written " + written + " bytes " + " at offset " + newsize);
            }
        }

        /**
     * Returns the file associated with this stream
     */
        File getFile() {
            return file;
        }
    }

    static class EditLogFileInputStream extends EditLogInputStream {

        private File file;

        private FileInputStream fStream;

        EditLogFileInputStream(File name) throws IOException {
            file = name;
            fStream = new FileInputStream(name);
        }

        @Override
        String getName() {
            return file.getPath();
        }

        @Override
        public int available() throws IOException {
            return fStream.available();
        }

        @Override
        public int read() throws IOException {
            return fStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return fStream.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            fStream.close();
        }

        @Override
        long length() throws IOException {
            return file.length();
        }
    }

    FSEditLog(FSImage image) {
        fsimage = image;
        isSyncRunning = false;
        metrics = NameNode.getNameNodeMetrics();
        lastPrintTime = FSNamesystem.now();
    }

    private File getEditFile(StorageDirectory sd) {
        return fsimage.getEditFile(sd);
    }

    private File getEditNewFile(StorageDirectory sd) {
        return fsimage.getEditNewFile(sd);
    }

    private int getNumStorageDirs() {
        int numStorageDirs = 0;
        for (Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS); it.hasNext(); it.next()) numStorageDirs++;
        return numStorageDirs;
    }

    synchronized int getNumEditStreams() {
        return editStreams == null ? 0 : editStreams.size();
    }

    boolean isOpen() {
        return getNumEditStreams() > 0;
    }

    /**
   * Create empty edit log files.
   * Initialize the output stream for logging.
   * 
   * @throws IOException
   */
    public synchronized void open() throws IOException {
        numTransactions = totalTimeTransactions = 0;
        if (editStreams == null) editStreams = new ArrayList<EditLogOutputStream>();
        for (Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS); it.hasNext(); ) {
            StorageDirectory sd = it.next();
            File eFile = getEditFile(sd);
            try {
                EditLogOutputStream eStream = new EditLogFileOutputStream(eFile);
                editStreams.add(eStream);
            } catch (IOException e) {
                FSNamesystem.LOG.warn("Unable to open edit log file " + eFile);
                it.remove();
            }
        }
    }

    public synchronized void createEditLogFile(File name) throws IOException {
        EditLogOutputStream eStream = new EditLogFileOutputStream(name);
        eStream.create();
        eStream.close();
    }

    /**
   * Create edits.new if non existent.
   */
    synchronized void createNewIfMissing() throws IOException {
        for (Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS); it.hasNext(); ) {
            File newFile = getEditNewFile(it.next());
            if (!newFile.exists()) createEditLogFile(newFile);
        }
    }

    /**
   * Shutdown the file store.
   */
    public synchronized void close() throws IOException {
        while (isSyncRunning) {
            try {
                wait(1000);
            } catch (InterruptedException ie) {
            }
        }
        if (editStreams == null) {
            return;
        }
        printStatistics(true);
        numTransactions = totalTimeTransactions = 0;
        for (int idx = 0; idx < editStreams.size(); idx++) {
            EditLogOutputStream eStream = editStreams.get(idx);
            try {
                eStream.setReadyToFlush();
                eStream.flush();
                eStream.close();
            } catch (IOException e) {
                processIOError(idx);
                idx--;
            }
        }
        editStreams.clear();
    }

    /**
   * If there is an IO Error on any log operations, remove that
   * directory from the list of directories.
   * If no more directories remain, then exit.
   */
    synchronized void processIOError(int index) {
        if (editStreams == null || editStreams.size() <= 1) {
            FSNamesystem.LOG.fatal("Fatal Error : All storage directories are inaccessible.");
            Runtime.getRuntime().exit(-1);
        }
        assert (index < getNumStorageDirs());
        assert (getNumStorageDirs() == editStreams.size());
        File parentStorageDir = ((EditLogFileOutputStream) editStreams.get(index)).getFile().getParentFile().getParentFile();
        editStreams.remove(index);
        fsimage.processIOError(parentStorageDir);
    }

    /**
   * If there is an IO Error on any log operations on storage directory,
   * remove any stream associated with that directory 
   */
    synchronized void processIOError(StorageDirectory sd) {
        if (!sd.getStorageDirType().isOfType(NameNodeDirType.EDITS)) return;
        if (editStreams == null || editStreams.size() <= 1) {
            FSNamesystem.LOG.fatal("Fatal Error : All storage directories are inaccessible.");
            Runtime.getRuntime().exit(-1);
        }
        for (int idx = 0; idx < editStreams.size(); idx++) {
            File parentStorageDir = ((EditLogFileOutputStream) editStreams.get(idx)).getFile().getParentFile().getParentFile();
            if (parentStorageDir.getName().equals(sd.getRoot().getName())) editStreams.remove(idx);
        }
    }

    /**
   * The specified streams have IO errors. Remove them from logging
   * new transactions.
   */
    private void processIOError(ArrayList<EditLogOutputStream> errorStreams) {
        if (errorStreams == null) {
            return;
        }
        for (int idx = 0; idx < errorStreams.size(); idx++) {
            EditLogOutputStream eStream = errorStreams.get(idx);
            int j = 0;
            for (j = 0; j < editStreams.size(); j++) {
                if (editStreams.get(j) == eStream) {
                    break;
                }
            }
            if (j == editStreams.size()) {
                FSNamesystem.LOG.error("Unable to find sync log on which " + " IO error occured. " + "Fatal Error.");
                Runtime.getRuntime().exit(-1);
            }
            processIOError(j);
        }
        fsimage.incrementCheckpointTime();
    }

    /**
   * check if ANY edits.new log exists
   */
    boolean existsNew() throws IOException {
        for (Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS); it.hasNext(); ) {
            if (getEditNewFile(it.next()).exists()) {
                return true;
            }
        }
        return false;
    }

    /**
   * Load an edit log, and apply the changes to the in-memory structure
   * This is where we apply edits that we've been writing to disk all
   * along.
   */
    static int loadFSEdits(EditLogInputStream edits) throws IOException {
        FSNamesystem fsNamesys = FSNamesystem.getFSNamesystem();
        FSDirectory fsDir = fsNamesys.dir;
        int numEdits = 0;
        int logVersion = 0;
        String clientName = null;
        String clientMachine = null;
        String path = null;
        int numOpAdd = 0, numOpClose = 0, numOpDelete = 0, numOpRename = 0, numOpSetRepl = 0, numOpMkDir = 0, numOpSetPerm = 0, numOpSetOwner = 0, numOpSetGenStamp = 0, numOpTimes = 0, numOpOther = 0;
        long startTime = FSNamesystem.now();
        DataInputStream in = new DataInputStream(new BufferedInputStream(edits));
        try {
            in.mark(4);
            boolean available = true;
            try {
                logVersion = in.readByte();
            } catch (EOFException e) {
                available = false;
            }
            if (available) {
                in.reset();
                logVersion = in.readInt();
                if (logVersion < FSConstants.LAYOUT_VERSION) throw new IOException("Unexpected version of the file system log file: " + logVersion + ". Current version = " + FSConstants.LAYOUT_VERSION + ".");
            }
            assert logVersion <= Storage.LAST_UPGRADABLE_LAYOUT_VERSION : "Unsupported version " + logVersion;
            while (true) {
                long timestamp = 0;
                long mtime = 0;
                long atime = 0;
                long blockSize = 0;
                byte opcode = -1;
                try {
                    opcode = in.readByte();
                    if (opcode == OP_INVALID) {
                        FSNamesystem.LOG.info("Invalid opcode, reached end of edit log " + "Number of transactions found " + numEdits);
                        break;
                    }
                } catch (EOFException e) {
                    break;
                }
                numEdits++;
                switch(opcode) {
                    case OP_ADD:
                    case OP_CLOSE:
                        {
                            int length = in.readInt();
                            if (-7 == logVersion && length != 3 || -17 < logVersion && logVersion < -7 && length != 4 || logVersion <= -17 && length != 5) {
                                throw new IOException("Incorrect data format." + " logVersion is " + logVersion + " but writables.length is " + length + ". ");
                            }
                            path = FSImage.readString(in);
                            short replication = adjustReplication(readShort(in));
                            mtime = readLong(in);
                            if (logVersion <= -17) {
                                atime = readLong(in);
                            }
                            if (logVersion < -7) {
                                blockSize = readLong(in);
                            }
                            Block blocks[] = null;
                            if (logVersion <= -14) {
                                blocks = readBlocks(in);
                            } else {
                                BlockTwo oldblk = new BlockTwo();
                                int num = in.readInt();
                                blocks = new Block[num];
                                for (int i = 0; i < num; i++) {
                                    oldblk.readFields(in);
                                    blocks[i] = new Block(oldblk.blkid, oldblk.len, Block.GRANDFATHER_GENERATION_STAMP);
                                }
                            }
                            if (-8 <= logVersion && blockSize == 0) {
                                if (blocks.length > 1) {
                                    blockSize = blocks[0].getNumBytes();
                                } else {
                                    long first = ((blocks.length == 1) ? blocks[0].getNumBytes() : 0);
                                    blockSize = Math.max(fsNamesys.getDefaultBlockSize(), first);
                                }
                            }
                            PermissionStatus permissions = fsNamesys.getUpgradePermission();
                            if (logVersion <= -11) {
                                permissions = PermissionStatus.read(in);
                            }
                            if (opcode == OP_ADD && logVersion <= -12) {
                                clientName = FSImage.readString(in);
                                clientMachine = FSImage.readString(in);
                                if (-13 <= logVersion) {
                                    readDatanodeDescriptorArray(in);
                                }
                            } else {
                                clientName = "";
                                clientMachine = "";
                            }
                            if (FSNamesystem.LOG.isDebugEnabled()) {
                                FSNamesystem.LOG.debug(opcode + ": " + path + " numblocks : " + blocks.length + " clientHolder " + clientName + " clientMachine " + clientMachine);
                            }
                            fsDir.unprotectedDelete(path, mtime);
                            INodeFile node = (INodeFile) fsDir.unprotectedAddFile(path, permissions, blocks, replication, mtime, atime, blockSize);
                            if (opcode == OP_ADD) {
                                numOpAdd++;
                                INodeFileUnderConstruction cons = new INodeFileUnderConstruction(node.getLocalNameBytes(), node.getReplication(), node.getModificationTime(), node.getPreferredBlockSize(), node.getBlocks(), node.getPermissionStatus(), clientName, clientMachine, null);
                                fsDir.replaceNode(path, node, cons);
                                fsNamesys.leaseManager.addLease(cons.clientName, path);
                            }
                            break;
                        }
                    case OP_SET_REPLICATION:
                        {
                            numOpSetRepl++;
                            path = FSImage.readString(in);
                            short replication = adjustReplication(readShort(in));
                            fsDir.unprotectedSetReplication(path, replication, null);
                            break;
                        }
                    case OP_RENAME:
                        {
                            numOpRename++;
                            int length = in.readInt();
                            if (length != 3) {
                                throw new IOException("Incorrect data format. " + "Mkdir operation.");
                            }
                            String s = FSImage.readString(in);
                            String d = FSImage.readString(in);
                            timestamp = readLong(in);
                            FileStatus dinfo = fsDir.getFileInfo(d);
                            fsDir.unprotectedRenameTo(s, d, timestamp);
                            fsNamesys.changeLease(s, d, dinfo);
                            break;
                        }
                    case OP_DELETE:
                        {
                            numOpDelete++;
                            int length = in.readInt();
                            if (length != 2) {
                                throw new IOException("Incorrect data format. " + "delete operation.");
                            }
                            path = FSImage.readString(in);
                            timestamp = readLong(in);
                            fsDir.unprotectedDelete(path, timestamp);
                            break;
                        }
                    case OP_MKDIR:
                        {
                            numOpMkDir++;
                            PermissionStatus permissions = fsNamesys.getUpgradePermission();
                            int length = in.readInt();
                            if (-17 < logVersion && length != 2 || logVersion <= -17 && length != 3) {
                                throw new IOException("Incorrect data format. " + "Mkdir operation.");
                            }
                            path = FSImage.readString(in);
                            timestamp = readLong(in);
                            if (logVersion <= -17) {
                                atime = readLong(in);
                            }
                            if (logVersion <= -11) {
                                permissions = PermissionStatus.read(in);
                            }
                            fsDir.unprotectedMkdir(path, permissions, timestamp);
                            break;
                        }
                    case OP_SET_GENSTAMP:
                        {
                            numOpSetGenStamp++;
                            long lw = in.readLong();
                            fsDir.namesystem.setGenerationStamp(lw);
                            break;
                        }
                    case OP_DATANODE_ADD:
                        {
                            numOpOther++;
                            FSImage.DatanodeImage nodeimage = new FSImage.DatanodeImage();
                            nodeimage.readFields(in);
                            break;
                        }
                    case OP_DATANODE_REMOVE:
                        {
                            numOpOther++;
                            DatanodeID nodeID = new DatanodeID();
                            nodeID.readFields(in);
                            break;
                        }
                    case OP_SET_PERMISSIONS:
                        {
                            numOpSetPerm++;
                            if (logVersion > -11) throw new IOException("Unexpected opcode " + opcode + " for version " + logVersion);
                            fsDir.unprotectedSetPermission(FSImage.readString(in), FsPermission.read(in));
                            break;
                        }
                    case OP_SET_OWNER:
                        {
                            numOpSetOwner++;
                            if (logVersion > -11) throw new IOException("Unexpected opcode " + opcode + " for version " + logVersion);
                            fsDir.unprotectedSetOwner(FSImage.readString(in), FSImage.readString_EmptyAsNull(in), FSImage.readString_EmptyAsNull(in));
                            break;
                        }
                    case OP_SET_NS_QUOTA:
                        {
                            if (logVersion > -16) {
                                throw new IOException("Unexpected opcode " + opcode + " for version " + logVersion);
                            }
                            fsDir.unprotectedSetQuota(FSImage.readString(in), readLongWritable(in), FSConstants.QUOTA_DONT_SET);
                            break;
                        }
                    case OP_CLEAR_NS_QUOTA:
                        {
                            if (logVersion > -16) {
                                throw new IOException("Unexpected opcode " + opcode + " for version " + logVersion);
                            }
                            fsDir.unprotectedSetQuota(FSImage.readString(in), FSConstants.QUOTA_RESET, FSConstants.QUOTA_DONT_SET);
                            break;
                        }
                    case OP_SET_QUOTA:
                        fsDir.unprotectedSetQuota(FSImage.readString(in), readLongWritable(in), readLongWritable(in));
                        break;
                    case OP_TIMES:
                        {
                            numOpTimes++;
                            int length = in.readInt();
                            if (length != 3) {
                                throw new IOException("Incorrect data format. " + "times operation.");
                            }
                            path = FSImage.readString(in);
                            mtime = readLong(in);
                            atime = readLong(in);
                            fsDir.unprotectedSetTimes(path, mtime, atime, true);
                            break;
                        }
                    default:
                        {
                            throw new IOException("Never seen opcode " + opcode);
                        }
                }
            }
        } finally {
            in.close();
        }
        FSImage.LOG.info("Edits file " + edits.getName() + " of size " + edits.length() + " edits # " + numEdits + " loaded in " + (FSNamesystem.now() - startTime) / 1000 + " seconds.");
        if (FSImage.LOG.isDebugEnabled()) {
            FSImage.LOG.debug("numOpAdd = " + numOpAdd + " numOpClose = " + numOpClose + " numOpDelete = " + numOpDelete + " numOpRename = " + numOpRename + " numOpSetRepl = " + numOpSetRepl + " numOpMkDir = " + numOpMkDir + " numOpSetPerm = " + numOpSetPerm + " numOpSetOwner = " + numOpSetOwner + " numOpSetGenStamp = " + numOpSetGenStamp + " numOpTimes = " + numOpTimes + " numOpOther = " + numOpOther);
        }
        if (logVersion != FSConstants.LAYOUT_VERSION) numEdits++;
        return numEdits;
    }

    private static final LongWritable longWritable = new LongWritable();

    /** Read an integer from an input stream */
    private static long readLongWritable(DataInputStream in) throws IOException {
        synchronized (longWritable) {
            longWritable.readFields(in);
            return longWritable.get();
        }
    }

    static short adjustReplication(short replication) {
        FSNamesystem fsNamesys = FSNamesystem.getFSNamesystem();
        short minReplication = fsNamesys.getMinReplication();
        if (replication < minReplication) {
            replication = minReplication;
        }
        short maxReplication = fsNamesys.getMaxReplication();
        if (replication > maxReplication) {
            replication = maxReplication;
        }
        return replication;
    }

    /**
   * Write an operation to the edit log. Do not sync to persistent
   * store yet.
   */
    synchronized void logEdit(byte op, Writable... writables) {
        assert this.getNumEditStreams() > 0 : "no editlog streams";
        long start = FSNamesystem.now();
        for (int idx = 0; idx < editStreams.size(); idx++) {
            EditLogOutputStream eStream = editStreams.get(idx);
            try {
                eStream.write(op, writables);
            } catch (IOException ie) {
                processIOError(idx);
            }
        }
        txid++;
        TransactionId id = myTransactionId.get();
        id.txid = txid;
        long end = FSNamesystem.now();
        numTransactions++;
        totalTimeTransactions += (end - start);
        if (metrics != null) metrics.transactions.inc((end - start));
    }

    public void logSync() throws IOException {
        ArrayList<EditLogOutputStream> errorStreams = null;
        long syncStart = 0;
        TransactionId id = myTransactionId.get();
        long mytxid = id.txid;
        synchronized (this) {
            assert this.getNumEditStreams() > 0 : "no editlog streams";
            printStatistics(false);
            while (mytxid > synctxid && isSyncRunning) {
                try {
                    wait(1000);
                } catch (InterruptedException ie) {
                }
            }
            if (mytxid <= synctxid) {
                return;
            }
            syncStart = txid;
            isSyncRunning = true;
            for (int idx = 0; idx < editStreams.size(); idx++) {
                EditLogOutputStream eStream = editStreams.get(idx);
                eStream.setReadyToFlush();
            }
        }
        long start = FSNamesystem.now();
        for (int idx = 0; idx < editStreams.size(); idx++) {
            EditLogOutputStream eStream = editStreams.get(idx);
            try {
                eStream.flush();
            } catch (IOException ie) {
                if (errorStreams == null) {
                    errorStreams = new ArrayList<EditLogOutputStream>(1);
                }
                errorStreams.add(eStream);
                FSNamesystem.LOG.error("Unable to sync edit log. " + "Fatal Error.");
            }
        }
        long elapsed = FSNamesystem.now() - start;
        synchronized (this) {
            processIOError(errorStreams);
            synctxid = syncStart;
            isSyncRunning = false;
            this.notifyAll();
        }
        if (metrics != null) metrics.syncs.inc(elapsed);
    }

    private void printStatistics(boolean force) {
        long now = FSNamesystem.now();
        if (lastPrintTime + 60000 > now && !force) {
            return;
        }
        if (editStreams == null) {
            return;
        }
        lastPrintTime = now;
        StringBuffer buf = new StringBuffer();
        buf.append("Number of transactions: " + numTransactions + " Total time for transactions(ms): " + totalTimeTransactions);
        buf.append(" Number of syncs: " + editStreams.get(0).getNumSync());
        buf.append(" SyncTimes(ms): ");
        for (int idx = 0; idx < editStreams.size(); idx++) {
            EditLogOutputStream eStream = editStreams.get(idx);
            buf.append(eStream.getTotalSyncTime());
            buf.append(" ");
        }
        FSNamesystem.LOG.info(buf);
    }

    /** 
   * Add open lease record to edit log. 
   * Records the block locations of the last block.
   */
    public void logOpenFile(String path, INodeFileUnderConstruction newNode) throws IOException {
        UTF8 nameReplicationPair[] = new UTF8[] { new UTF8(path), FSEditLog.toLogReplication(newNode.getReplication()), FSEditLog.toLogLong(newNode.getModificationTime()), FSEditLog.toLogLong(newNode.getAccessTime()), FSEditLog.toLogLong(newNode.getPreferredBlockSize()) };
        logEdit(OP_ADD, new ArrayWritable(UTF8.class, nameReplicationPair), new ArrayWritable(Block.class, newNode.getBlocks()), newNode.getPermissionStatus(), new UTF8(newNode.getClientName()), new UTF8(newNode.getClientMachine()));
    }

    /** 
   * Add close lease record to edit log.
   */
    public void logCloseFile(String path, INodeFile newNode) {
        UTF8 nameReplicationPair[] = new UTF8[] { new UTF8(path), FSEditLog.toLogReplication(newNode.getReplication()), FSEditLog.toLogLong(newNode.getModificationTime()), FSEditLog.toLogLong(newNode.getAccessTime()), FSEditLog.toLogLong(newNode.getPreferredBlockSize()) };
        logEdit(OP_CLOSE, new ArrayWritable(UTF8.class, nameReplicationPair), new ArrayWritable(Block.class, newNode.getBlocks()), newNode.getPermissionStatus());
    }

    /** 
   * Add create directory record to edit log
   */
    public void logMkDir(String path, INode newNode) {
        UTF8 info[] = new UTF8[] { new UTF8(path), FSEditLog.toLogLong(newNode.getModificationTime()), FSEditLog.toLogLong(newNode.getAccessTime()) };
        logEdit(OP_MKDIR, new ArrayWritable(UTF8.class, info), newNode.getPermissionStatus());
    }

    /** 
   * Add rename record to edit log
   * TODO: use String parameters until just before writing to disk
   */
    void logRename(String src, String dst, long timestamp) {
        UTF8 info[] = new UTF8[] { new UTF8(src), new UTF8(dst), FSEditLog.toLogLong(timestamp) };
        logEdit(OP_RENAME, new ArrayWritable(UTF8.class, info));
    }

    /** 
   * Add set replication record to edit log
   */
    void logSetReplication(String src, short replication) {
        logEdit(OP_SET_REPLICATION, new UTF8(src), FSEditLog.toLogReplication(replication));
    }

    /** Add set namespace quota record to edit log
   * 
   * @param src the string representation of the path to a directory
   * @param quota the directory size limit
   */
    void logSetQuota(String src, long nsQuota, long dsQuota) {
        logEdit(OP_SET_QUOTA, new UTF8(src), new LongWritable(nsQuota), new LongWritable(dsQuota));
    }

    /**  Add set permissions record to edit log */
    void logSetPermissions(String src, FsPermission permissions) {
        logEdit(OP_SET_PERMISSIONS, new UTF8(src), permissions);
    }

    /**  Add set owner record to edit log */
    void logSetOwner(String src, String username, String groupname) {
        UTF8 u = new UTF8(username == null ? "" : username);
        UTF8 g = new UTF8(groupname == null ? "" : groupname);
        logEdit(OP_SET_OWNER, new UTF8(src), u, g);
    }

    /** 
   * Add delete file record to edit log
   */
    void logDelete(String src, long timestamp) {
        UTF8 info[] = new UTF8[] { new UTF8(src), FSEditLog.toLogLong(timestamp) };
        logEdit(OP_DELETE, new ArrayWritable(UTF8.class, info));
    }

    /** 
   * Add generation stamp record to edit log
   */
    void logGenerationStamp(long genstamp) {
        logEdit(OP_SET_GENSTAMP, new LongWritable(genstamp));
    }

    /** 
   * Add access time record to edit log
   */
    void logTimes(String src, long mtime, long atime) {
        UTF8 info[] = new UTF8[] { new UTF8(src), FSEditLog.toLogLong(mtime), FSEditLog.toLogLong(atime) };
        logEdit(OP_TIMES, new ArrayWritable(UTF8.class, info));
    }

    private static UTF8 toLogReplication(short replication) {
        return new UTF8(Short.toString(replication));
    }

    private static UTF8 toLogLong(long timestamp) {
        return new UTF8(Long.toString(timestamp));
    }

    /**
   * Return the size of the current EditLog
   */
    synchronized long getEditLogSize() throws IOException {
        assert (getNumStorageDirs() == editStreams.size());
        long size = 0;
        for (int idx = 0; idx < editStreams.size(); idx++) {
            long curSize = editStreams.get(idx).length();
            assert (size == 0 || size == curSize) : "All streams must be the same";
            size = curSize;
        }
        return size;
    }

    /**
   * Closes the current edit log and opens edits.new. 
   * Returns the lastModified time of the edits log.
   */
    synchronized void rollEditLog() throws IOException {
        if (existsNew()) {
            for (Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS); it.hasNext(); ) {
                File editsNew = getEditNewFile(it.next());
                if (!editsNew.exists()) {
                    throw new IOException("Inconsistent existance of edits.new " + editsNew);
                }
            }
            return;
        }
        close();
        for (Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS); it.hasNext(); ) {
            StorageDirectory sd = it.next();
            try {
                EditLogFileOutputStream eStream = new EditLogFileOutputStream(getEditNewFile(sd));
                eStream.create();
                editStreams.add(eStream);
            } catch (IOException e) {
                processIOError(sd);
                it.remove();
            }
        }
    }

    /**
   * Removes the old edit log and renamed edits.new as edits.
   * Reopens the edits file.
   */
    synchronized void purgeEditLog() throws IOException {
        if (!existsNew()) {
            throw new IOException("Attempt to purge edit log " + "but edits.new does not exist.");
        }
        close();
        for (Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS); it.hasNext(); ) {
            StorageDirectory sd = it.next();
            if (!getEditNewFile(sd).renameTo(getEditFile(sd))) {
                getEditFile(sd).delete();
                if (!getEditNewFile(sd).renameTo(getEditFile(sd))) {
                    it.remove();
                }
            }
        }
        open();
    }

    /**
   * Return the name of the edit file
   */
    synchronized File getFsEditName() throws IOException {
        StorageDirectory sd = null;
        for (Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS); it.hasNext(); ) sd = it.next();
        return getEditFile(sd);
    }

    /**
   * Returns the timestamp of the edit log
   */
    synchronized long getFsEditTime() {
        Iterator<StorageDirectory> it = fsimage.dirIterator(NameNodeDirType.EDITS);
        if (it.hasNext()) return getEditFile(it.next()).lastModified();
        return 0;
    }

    static void setBufferCapacity(int size) {
        sizeFlushBuffer = size;
    }

    /**
   * A class to read in blocks stored in the old format. The only two
   * fields in the block were blockid and length.
   */
    static class BlockTwo implements Writable {

        long blkid;

        long len;

        static {
            WritableFactories.setFactory(BlockTwo.class, new WritableFactory() {

                public Writable newInstance() {
                    return new BlockTwo();
                }
            });
        }

        BlockTwo() {
            blkid = 0;
            len = 0;
        }

        public void write(DataOutput out) throws IOException {
            out.writeLong(blkid);
            out.writeLong(len);
        }

        public void readFields(DataInput in) throws IOException {
            this.blkid = in.readLong();
            this.len = in.readLong();
        }
    }

    /** This method is defined for compatibility reason. */
    private static DatanodeDescriptor[] readDatanodeDescriptorArray(DataInput in) throws IOException {
        DatanodeDescriptor[] locations = new DatanodeDescriptor[in.readInt()];
        for (int i = 0; i < locations.length; i++) {
            locations[i] = new DatanodeDescriptor();
            locations[i].readFieldsFromFSEditLog(in);
        }
        return locations;
    }

    private static short readShort(DataInputStream in) throws IOException {
        return Short.parseShort(FSImage.readString(in));
    }

    private static long readLong(DataInputStream in) throws IOException {
        return Long.parseLong(FSImage.readString(in));
    }

    private static Block[] readBlocks(DataInputStream in) throws IOException {
        int numBlocks = in.readInt();
        Block[] blocks = new Block[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            blocks[i] = new Block();
            blocks[i].readFields(in);
        }
        return blocks;
    }
}
