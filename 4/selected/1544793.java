package com.dbxml.db.common.btree;

import java.io.*;
import com.dbxml.db.core.DBException;
import com.dbxml.db.core.FaultCodes;
import com.dbxml.db.core.transaction.Transaction;
import com.dbxml.db.core.transaction.TransactionException;
import com.dbxml.db.core.transaction.TransactionLog;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * PagedLog
 */
final class PagedLog implements TransactionLog {

    private static final boolean CHECKPOINT_TRUNCATE = true;

    private static final int BUFFER_SIZE = 4096;

    public static final byte EVENT_START = 0;

    public static final byte EVENT_WRITE = 1;

    public static final byte EVENT_COMMIT = 2;

    public static final byte EVENT_CANCEL = 3;

    public static final byte EVENT_CHKPNT = 4;

    private Set transactions = Collections.synchronizedSet(new HashSet());

    private Paged parent;

    private File file;

    private FileChannel fc;

    private FileLock lock;

    private DataOutputStream dos;

    private boolean opened;

    public PagedLog(Paged parent, File file) {
        this.parent = parent;
        this.file = file;
    }

    public synchronized void start(Transaction tx) throws TransactionException {
        if (!transactions.contains(tx)) transactions.add(tx);
        try {
            dos.writeByte(EVENT_START);
            dos.writeLong(tx.getID());
        } catch (IOException e) {
            throw new TransactionException(FaultCodes.GEN_CRITICAL_ERROR, "Error writing " + file.getName(), e);
        }
    }

    public synchronized void commit(Transaction tx) throws TransactionException {
        try {
            parent.flush(tx);
            dos.writeByte(EVENT_COMMIT);
            dos.writeLong(tx.getID());
            dos.flush();
        } catch (DBException e) {
            throw new TransactionException(FaultCodes.GEN_CRITICAL_ERROR, "Error flushing buffers", e);
        } catch (IOException e) {
            throw new TransactionException(FaultCodes.GEN_CRITICAL_ERROR, "Error writing " + file.getName(), e);
        } finally {
            transactions.remove(tx);
        }
        if (transactions.isEmpty()) checkpoint();
    }

    public synchronized void cancel(Transaction tx) throws TransactionException {
        try {
            parent.flush(tx);
            dos.writeByte(EVENT_CANCEL);
            dos.writeLong(tx.getID());
        } catch (DBException e) {
            throw new TransactionException(FaultCodes.GEN_CRITICAL_ERROR, "Error flushing buffers", e);
        } catch (IOException e) {
            throw new TransactionException(FaultCodes.GEN_CRITICAL_ERROR, "Error writing " + file.getName(), e);
        } finally {
            transactions.remove(tx);
        }
        if (transactions.isEmpty()) checkpoint();
    }

    synchronized void write(Transaction tx, long offset, byte[] buffer) throws TransactionException {
        try {
            dos.writeByte(EVENT_WRITE);
            dos.writeLong(tx.getID());
            dos.writeLong(offset);
            dos.writeInt(buffer.length);
            dos.write(buffer);
        } catch (IOException e) {
            throw new TransactionException(FaultCodes.GEN_CRITICAL_ERROR, "Error writing " + file.getName(), e);
        }
    }

    synchronized void checkpoint() throws TransactionException {
        try {
            dos.writeByte(EVENT_CHKPNT);
            if (CHECKPOINT_TRUNCATE) truncate(); else dos.flush();
        } catch (IOException e) {
            throw new TransactionException(FaultCodes.GEN_CRITICAL_ERROR, "Error writing " + file.getName(), e);
        }
    }

    public void truncate() throws TransactionException {
        try {
            dos.flush();
            fc.truncate(0);
        } catch (IOException e) {
            throw new TransactionException(FaultCodes.GEN_CRITICAL_ERROR, "Error writing " + file.getName(), e);
        }
    }

    public int getTransactionCount() throws TransactionException {
        return transactions.size();
    }

    public Iterator getTransactions() throws TransactionException {
        return transactions.iterator();
    }

    protected void checkOpened() throws DBException {
        if (!opened) throw new DBException(FaultCodes.COL_COLLECTION_CLOSED, "Transaction Log is closed");
    }

    public boolean exists() {
        return file.exists();
    }

    private void reset() {
        lock = null;
        fc = null;
        dos = null;
    }

    public boolean create() throws DBException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.close();
            reset();
            return true;
        } catch (Exception e) {
            throw new DBException(FaultCodes.GEN_CRITICAL_ERROR, "Error creating " + file.getName(), e);
        }
    }

    public boolean open() throws DBException {
        try {
            if (!opened) {
                FileOutputStream fos = new FileOutputStream(file);
                fc = fos.getChannel();
                lock = fc.tryLock();
                if (lock == null) {
                    System.err.println("FATAL ERROR: Cannot open '" + file.getName() + "' for exclusive access");
                    System.exit(1);
                }
                BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
                dos = new DataOutputStream(bos);
                opened = true;
            }
            return opened;
        } catch (Exception e) {
            throw new DBException(FaultCodes.GEN_CRITICAL_ERROR, "Error opening " + file.getName(), e);
        }
    }

    public synchronized boolean close() throws DBException {
        try {
            if (opened) {
                lock.release();
                dos.close();
                fc.close();
                opened = false;
                reset();
                return true;
            } else return false;
        } catch (Exception e) {
            throw new DBException(FaultCodes.GEN_CRITICAL_ERROR, "Error closing " + file.getName(), e);
        }
    }

    public boolean isOpened() {
        return opened;
    }

    public boolean drop() throws DBException {
        try {
            close();
            if (exists()) return file.delete(); else return true;
        } catch (Exception e) {
            throw new DBException(FaultCodes.COL_CANNOT_DROP, "Can't drop " + file.getName(), e);
        }
    }

    /**
    * playback plays back the entries in the log to a PagedLogPlayback
    * implementation.
    *
    * @param callback The Log callback
    */
    public void playback(PagedLogPlayback callback) throws DBException, TransactionException {
        try {
            callback.beginPlayback();
            if (exists()) {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                FileChannel fc = raf.getChannel();
                long fileLen = raf.length();
                while (raf.getFilePointer() < fileLen) {
                    byte event = raf.readByte();
                    switch(event) {
                        case EVENT_START:
                            callback.start(raf.readLong());
                            break;
                        case EVENT_WRITE:
                            long transactionID = raf.readLong();
                            long offset = raf.readLong();
                            int size = raf.readInt();
                            byte[] b = new byte[size];
                            raf.read(b);
                            ByteBuffer buffer = ByteBuffer.wrap(b);
                            callback.write(transactionID, offset, buffer);
                            break;
                        case EVENT_COMMIT:
                            callback.commit(raf.readLong());
                            break;
                        case EVENT_CANCEL:
                            callback.cancel(raf.readLong());
                            break;
                        case EVENT_CHKPNT:
                            callback.checkpoint();
                            break;
                    }
                }
                raf.close();
            }
        } catch (IOException e) {
            throw new DBException(FaultCodes.GEN_CRITICAL_ERROR, "Error playing back " + file.getName(), e);
        } finally {
            callback.endPlayback();
        }
    }
}
