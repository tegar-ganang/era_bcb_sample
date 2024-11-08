package org.cojen.tupl;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * 
 *
 * @author Brian S O'Neill
 */
final class RedoLog implements Closeable {

    private static final long MAGIC_NUMBER = 431399725605778814L;

    private static final int ENCODING_VERSION = 20120105;

    private static final byte OP_TIMESTAMP = 1, OP_SHUTDOWN = 2, OP_CLOSE = 3, OP_END_FILE = 4, OP_TXN_ROLLBACK = 9, OP_TXN_ROLLBACK_CHILD = 10, OP_TXN_COMMIT = 11, OP_TXN_COMMIT_CHILD = 12, OP_STORE = 16, OP_DELETE = 17, OP_TXN_STORE = 19, OP_TXN_STORE_COMMIT = 20, OP_TXN_STORE_COMMIT_CHILD = 21, OP_TXN_DELETE = 22, OP_TXN_DELETE_COMMIT = 23, OP_TXN_DELETE_COMMIT_CHILD = 24;

    private final File mBaseFile;

    private final byte[] mBuffer;

    private int mBufferPos;

    private long mLogId;

    private FileOutputStream mOut;

    private FileChannel mChannel;

    private boolean mReplayMode;

    private boolean mAlwaysFlush;

    private Thread mShutdownHook;

    /**
     * RedoLog starts in replay mode.
     */
    RedoLog(File baseFile, long logId) throws IOException {
        mBaseFile = baseFile;
        mBuffer = new byte[4096];
        synchronized (this) {
            mLogId = logId;
            mReplayMode = true;
            mShutdownHook = new Thread() {

                @Override
                public void run() {
                    try {
                        shutdown(OP_SHUTDOWN);
                    } catch (Throwable e) {
                        Utils.rethrow(e);
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(mShutdownHook);
        }
    }

    synchronized boolean isReplayMode() {
        return mReplayMode;
    }

    /**
     * @return false if file not found and replay mode is deactivated
     */
    synchronized boolean replay(RedoLogVisitor visitor) throws IOException {
        if (!mReplayMode) {
            throw new IllegalStateException();
        }
        File file = fileFor(mLogId);
        DataIn in;
        try {
            in = new DataIn(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            mReplayMode = false;
            openFile(mLogId);
            return false;
        }
        try {
            replay(in, visitor);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        return true;
    }

    /**
     * @return old log file id, which is one less than new one
     */
    synchronized long openNewFile() throws IOException {
        if (mOut != null) {
            writeOp(OP_END_FILE, System.currentTimeMillis());
            doFlush();
        }
        long logId = mLogId;
        openFile(logId + 1);
        return logId;
    }

    void deleteOldFile(long logId) {
        fileFor(logId).delete();
    }

    private synchronized void openFile(long logId) throws IOException {
        final File file = fileFor(logId);
        if (file.exists()) {
            if (mReplayMode) {
                mLogId = logId;
                return;
            }
            throw new FileNotFoundException("Log file already exists: " + file.getPath());
        }
        mReplayMode = false;
        final FileOutputStream oldOut = mOut;
        final FileOutputStream out = new FileOutputStream(file);
        try {
            mOut = out;
            mChannel = out.getChannel();
            writeLong(MAGIC_NUMBER);
            writeInt(ENCODING_VERSION);
            writeLong(logId);
            timestamp();
            doFlush();
        } catch (IOException e) {
            mChannel = ((mOut = oldOut) == null) ? null : oldOut.getChannel();
            try {
                out.close();
            } catch (IOException e2) {
            }
            file.delete();
            throw e;
        }
        if (oldOut != null) {
            try {
                oldOut.close();
            } catch (IOException e) {
            }
        }
        mLogId = logId;
    }

    private File fileFor(long logId) {
        return new File(mBaseFile.getPath() + ".redo." + logId);
    }

    public synchronized void flush() throws IOException {
        doFlush();
    }

    public void sync() throws IOException {
        flush();
        mChannel.force(false);
    }

    public synchronized void close() throws IOException {
        shutdown(OP_CLOSE);
    }

    void shutdown(byte op) throws IOException {
        synchronized (this) {
            mAlwaysFlush = true;
            if (op == OP_CLOSE) {
                Thread hook = mShutdownHook;
                if (hook != null) {
                    try {
                        Runtime.getRuntime().removeShutdownHook(hook);
                    } catch (IllegalStateException e) {
                    }
                }
            }
            mShutdownHook = null;
            if (mChannel == null || !mChannel.isOpen()) {
                return;
            }
            writeOp(op, System.currentTimeMillis());
            doFlush();
            if (op == OP_CLOSE) {
                mChannel.force(true);
                mChannel.close();
                return;
            }
        }
        mChannel.force(true);
    }

    public void store(long indexId, byte[] key, byte[] value, DurabilityMode mode) throws IOException {
        if (key == null) {
            throw new NullPointerException("Key is null");
        }
        boolean sync;
        synchronized (this) {
            if (value == null) {
                writeOp(OP_DELETE, indexId);
                writeUnsignedVarInt(key.length);
                writeBytes(key);
            } else {
                writeOp(OP_STORE, indexId);
                writeUnsignedVarInt(key.length);
                writeBytes(key);
                writeUnsignedVarInt(value.length);
                writeBytes(value);
            }
            sync = conditionalFlush(mode);
        }
        if (sync) {
            mChannel.force(false);
        }
    }

    public synchronized void txnRollback(long txnId, long parentTxnId) throws IOException {
        if (parentTxnId == 0) {
            writeOp(OP_TXN_ROLLBACK, txnId);
        } else {
            writeOp(OP_TXN_ROLLBACK_CHILD, txnId);
            writeLong(parentTxnId);
        }
    }

    public void txnCommit(long txnId, long parentTxnId, DurabilityMode mode) throws IOException {
        boolean sync;
        synchronized (this) {
            if (parentTxnId == 0) {
                writeOp(OP_TXN_COMMIT, txnId);
                sync = conditionalFlush(mode);
            } else {
                writeOp(OP_TXN_COMMIT_CHILD, txnId);
                writeLong(parentTxnId);
                return;
            }
        }
        if (sync) {
            mChannel.force(false);
        }
    }

    public synchronized void txnStore(long txnId, long indexId, byte[] key, byte[] value) throws IOException {
        if (key == null) {
            throw new NullPointerException("Key is null");
        }
        if (value == null) {
            writeOp(OP_TXN_DELETE, txnId);
            writeLong(indexId);
            writeUnsignedVarInt(key.length);
            writeBytes(key);
        } else {
            writeOp(OP_TXN_STORE, txnId);
            writeLong(indexId);
            writeUnsignedVarInt(key.length);
            writeBytes(key);
            writeUnsignedVarInt(value.length);
            writeBytes(value);
        }
    }

    synchronized void timestamp() throws IOException {
        writeOp(OP_TIMESTAMP, System.currentTimeMillis());
    }

    private void writeInt(int v) throws IOException {
        byte[] buffer = mBuffer;
        int pos = mBufferPos;
        if (pos > buffer.length - 4) {
            doFlush(buffer, pos);
            pos = 0;
        }
        DataUtils.writeInt(buffer, pos, v);
        mBufferPos = pos + 4;
    }

    private void writeLong(long v) throws IOException {
        byte[] buffer = mBuffer;
        int pos = mBufferPos;
        if (pos > buffer.length - 8) {
            doFlush(buffer, pos);
            pos = 0;
        }
        DataUtils.writeLong(buffer, pos, v);
        mBufferPos = pos + 8;
    }

    private void writeOp(byte op, long operand) throws IOException {
        byte[] buffer = mBuffer;
        int pos = mBufferPos;
        if (pos >= buffer.length - 9) {
            doFlush(buffer, pos);
            pos = 0;
        }
        buffer[pos] = op;
        DataUtils.writeLong(buffer, pos + 1, operand);
        mBufferPos = pos + 9;
    }

    private void writeUnsignedVarInt(int v) throws IOException {
        byte[] buffer = mBuffer;
        int pos = mBufferPos;
        if (pos > buffer.length - 5) {
            doFlush(buffer, pos);
            pos = 0;
        }
        mBufferPos = DataUtils.writeUnsignedVarInt(buffer, pos, v);
    }

    private void writeBytes(byte[] bytes) throws IOException {
        writeBytes(bytes, 0, bytes.length);
    }

    private void writeBytes(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) {
            return;
        }
        byte[] buffer = mBuffer;
        int pos = mBufferPos;
        while (true) {
            if (pos <= buffer.length - length) {
                System.arraycopy(bytes, offset, buffer, pos, length);
                mBufferPos = pos + length;
                return;
            }
            int remaining = buffer.length - pos;
            System.arraycopy(bytes, offset, buffer, pos, remaining);
            doFlush(buffer, buffer.length);
            pos = 0;
            offset += remaining;
            length -= remaining;
        }
    }

    private boolean conditionalFlush(DurabilityMode mode) throws IOException {
        switch(mode) {
            default:
                return false;
            case NO_FLUSH:
                if (mAlwaysFlush) {
                    doFlush();
                }
                return false;
            case SYNC:
                doFlush();
                return true;
            case NO_SYNC:
                doFlush();
                return false;
        }
    }

    private void doFlush() throws IOException {
        doFlush(mBuffer, mBufferPos);
    }

    private void doFlush(byte[] buffer, int pos) throws IOException {
        mOut.write(buffer, 0, pos);
        mBufferPos = 0;
    }

    private void replay(DataIn in, RedoLogVisitor visitor) throws IOException {
        if (in.readLong() != MAGIC_NUMBER) {
            throw new DatabaseException("Incorrect magic number in log file");
        }
        int version = in.readInt();
        if (version != ENCODING_VERSION) {
            throw new DatabaseException("Unsupported encoding version: " + version);
        }
        long id = in.readLong();
        if (id != mLogId) {
            throw new DatabaseException("Expected log identifier of " + mLogId + ", but actual is: " + id);
        }
        int op;
        while ((op = in.read()) >= 0) {
            op = op & 0xff;
            switch(op) {
                default:
                    throw new DatabaseException("Unknown log operation: " + op);
                case OP_TIMESTAMP:
                    visitor.timestamp(in.readLong());
                    break;
                case OP_SHUTDOWN:
                    visitor.shutdown(in.readLong());
                    break;
                case OP_CLOSE:
                    visitor.close(in.readLong());
                    break;
                case OP_END_FILE:
                    visitor.endFile(in.readLong());
                    break;
                case OP_TXN_ROLLBACK:
                    visitor.txnRollback(in.readLong(), 0);
                    break;
                case OP_TXN_ROLLBACK_CHILD:
                    visitor.txnRollback(in.readLong(), in.readLong());
                    break;
                case OP_TXN_COMMIT:
                    visitor.txnCommit(in.readLong(), 0);
                    break;
                case OP_TXN_COMMIT_CHILD:
                    visitor.txnCommit(in.readLong(), in.readLong());
                    break;
                case OP_STORE:
                    visitor.store(in.readLong(), in.readBytes(), in.readBytes());
                    break;
                case OP_DELETE:
                    visitor.store(in.readLong(), in.readBytes(), null);
                    break;
                case OP_TXN_STORE:
                    visitor.txnStore(in.readLong(), in.readLong(), in.readBytes(), in.readBytes());
                    break;
                case OP_TXN_STORE_COMMIT:
                    long txnId = in.readLong();
                    visitor.txnStore(txnId, in.readLong(), in.readBytes(), in.readBytes());
                    visitor.txnCommit(txnId, 0);
                    break;
                case OP_TXN_STORE_COMMIT_CHILD:
                    txnId = in.readLong();
                    long parentTxnId = in.readLong();
                    visitor.txnStore(txnId, in.readLong(), in.readBytes(), in.readBytes());
                    visitor.txnCommit(txnId, parentTxnId);
                    break;
                case OP_TXN_DELETE:
                    visitor.txnStore(in.readLong(), in.readLong(), in.readBytes(), null);
                    break;
                case OP_TXN_DELETE_COMMIT:
                    txnId = in.readLong();
                    visitor.txnStore(txnId, in.readLong(), in.readBytes(), null);
                    visitor.txnCommit(txnId, 0);
                    break;
                case OP_TXN_DELETE_COMMIT_CHILD:
                    txnId = in.readLong();
                    parentTxnId = in.readLong();
                    visitor.txnStore(txnId, in.readLong(), in.readBytes(), null);
                    visitor.txnCommit(txnId, parentTxnId);
                    break;
            }
        }
    }
}
