package org.xactor.tm.recovery;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.xactor.tm.XidFactoryBase;

/**
 * Simple implementation of <code>RecoveryLogReader</code> used at recovery
 * time. The <code>BatchRecoveryLogger</code>'s implementation of method
 * <code>getRecoveryLogs()</code> instantiates 
 * <code>BatchRecoveryLogReader</code>s for the existing recovery log files.
 * It returns an array containing those readers, which the recovery manager 
 * uses to get the information in the log files.
 * 
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 37634 $
 */
class BatchRecoveryLogReader implements RecoveryLogReader {

    /** Class <code>Logger</code>, for trace messages. */
    private static Logger errorLog = Logger.getLogger(BatchRecoveryLogReader.class);

    /** The log file read by this reader. */
    private File logFile;

    /** Xid factory for converting local transaction ids into global ids. */
    private XidFactoryBase xidFactory;

    /**
    * Constructs a <code>BatchRecoveryLogReader</code>.
    * 
    * @param logFile the log file that will be read by the reader 
    * @param xidFactory Xid factory that the reader will use to convert local 
    *                   transaction ids into global ids.
    */
    BatchRecoveryLogReader(File logFile, XidFactoryBase xidFactory) {
        this.logFile = logFile;
        this.xidFactory = xidFactory;
    }

    /**
    * Reads the header object written out to the beginning of the log file via
    * Java serialization.
    * 
    * @param dis a <code>DataInputStream</code> view of the log file
    * @return the header object read from the log file.
    * @throws IOException if there is an error reading the header object 
    * @throws ClassNotFoundException if the class of the header object is not
    *                     available.
    */
    private Object readHeaderObject(DataInputStream dis) throws IOException, ClassNotFoundException {
        int num = dis.readInt();
        byte[] bytes = new byte[num];
        dis.read(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    /**
    * Gets the name of the underlying log file.
    * 
    * @return the name of the log file.
    */
    public String getLogFileName() {
        return logFile.toString();
    }

    /**
    * Gets the branch qualifier string stored in the log file header.
    * 
    * @return the branch qualifier read from the log file header.
    */
    public String getBranchQualifier() {
        FileInputStream fis;
        DataInputStream dis;
        try {
            fis = new FileInputStream(logFile);
            dis = new DataInputStream(fis);
            try {
                return (String) readHeaderObject(dis);
            } finally {
                fis.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * Recovers transaction information from the log file. 
    * 
    * @param committedSingleTmTransactions a <code>List</code> of 
    *           <code>LogRecord.Data</code> instances with one element per
    *           committed single-TM transaction logged to the log file
    * @param committedMultiTmTransactions a <code>List</code> of 
    *           <code>LogRecord.Data</code> instances with one element per
    *           committed  multi-TM transaction that has not yet completed the
    *           second phase of the 2PC protocol when the server crashed 
    * @param inDoubtTransactions a <code>List</code> of 
    *           <code>LogRecord.Data</code> instances with one element per
    *           foreign transaction that arrived at the server via DTM/OTS
    *           context propagation and was in the in-doubt state (i.e.,
    *           replied to prepare with a commit vote but has not yet received
    *           information on the transaction outcome) when the server crashed
    * @param inDoubtJcaTransactions a <code>List</code> of 
    *           <code>LogRecord.Data</code> instances with one element per
    *           foreign transaction that arrived at the server via JCA 
    *           transaction inflow and was in the in-doubt state (i.e., replied
    *           to prepare with a commit vote and was waiting for information 
    *           on the transaction outcome) when the server crashed.
    */
    public void recover(List committedSingleTmTransactions, List committedMultiTmTransactions, List inDoubtTransactions, List inDoubtJcaTransactions) {
        Map activeMultiTmTransactions = new HashMap();
        FileInputStream fis;
        DataInputStream dis;
        CorruptedLogRecordException corruptedLogRecordException = null;
        try {
            fis = new FileInputStream(logFile);
            dis = new DataInputStream(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            readHeaderObject(dis);
            if (fis.available() < LogRecord.FULL_HEADER_LEN) return;
            FileChannel channel = fis.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(LogRecord.FULL_HEADER_LEN);
            channel.read(buf);
            LogRecord.Data data;
            int len = LogRecord.getNextRecordLength(buf, 0);
            while (len > 0) {
                buf = ByteBuffer.allocate(len + LogRecord.FULL_HEADER_LEN);
                if (channel.read(buf) < len) {
                    errorLog.info("Unexpected end of file in transaction log file " + logFile.getName());
                    break;
                }
                buf.flip();
                data = new LogRecord.Data();
                try {
                    LogRecord.getData(buf, len, data);
                } catch (CorruptedLogRecordException e) {
                    if (corruptedLogRecordException == null) corruptedLogRecordException = e;
                    long corruptedRecordPos = channel.position() - buf.limit() - LogRecord.FULL_HEADER_LEN;
                    long nextPos = scanForward(corruptedRecordPos + 1);
                    if (nextPos == 0) {
                        errorLog.info("LOG CORRUPTION AT THE END OF LOG FILE " + logFile.getName());
                        break;
                    } else {
                        errorLog.info("LOG CORRUPTION IN THE MIDDLE OF LOG FILE " + logFile.getName() + ". Skipping " + (nextPos - corruptedRecordPos) + " bytes" + ". Disabling presumed rollback.");
                        channel.position(nextPos);
                        buf = ByteBuffer.allocate(LogRecord.FULL_HEADER_LEN);
                        channel.read(buf);
                        len = LogRecord.getNextRecordLength(buf, 0);
                        corruptedLogRecordException.disablePresumedRollback = true;
                        continue;
                    }
                }
                switch(data.recordType) {
                    case LogRecord.TX_COMMITTED:
                        data.globalTransactionId = xidFactory.localIdToGlobalId(data.localTransactionId);
                        committedSingleTmTransactions.add(data);
                        break;
                    case LogRecord.MULTI_TM_TX_COMMITTED:
                        data.globalTransactionId = xidFactory.localIdToGlobalId(data.localTransactionId);
                    case LogRecord.TX_PREPARED:
                    case LogRecord.JCA_TX_PREPARED:
                        activeMultiTmTransactions.put(new Long(data.localTransactionId), data);
                        break;
                    case LogRecord.TX_END:
                        activeMultiTmTransactions.remove(new Long(data.localTransactionId));
                        break;
                    default:
                        errorLog.warn("INVALID TYPE IN LOG RECORD.");
                        break;
                }
                try {
                    len = LogRecord.getNextRecordLength(buf, len);
                } catch (CorruptedLogRecordException e) {
                    if (corruptedLogRecordException == null) corruptedLogRecordException = e;
                    long corruptedRecordPos = channel.position() - buf.limit() - LogRecord.FULL_HEADER_LEN;
                    long nextPos = scanForward(corruptedRecordPos + 1);
                    if (nextPos == 0) {
                        errorLog.info("LOG CORRUPTION AT THE END OF LOG FILE " + logFile.getName());
                        len = 0;
                    } else {
                        errorLog.info("LOG CORRUPTION IN THE MIDDLE OF LOG FILE " + logFile.getName() + ". Skipping " + (nextPos - corruptedRecordPos) + " bytes" + ". Disabling presumed rollback.");
                        channel.position(nextPos);
                        buf = ByteBuffer.allocate(LogRecord.FULL_HEADER_LEN);
                        channel.read(buf);
                        len = LogRecord.getNextRecordLength(buf, 0);
                        corruptedLogRecordException.disablePresumedRollback = true;
                    }
                }
            }
            Iterator iter = activeMultiTmTransactions.values().iterator();
            while (iter.hasNext()) {
                data = (LogRecord.Data) iter.next();
                switch(data.recordType) {
                    case LogRecord.MULTI_TM_TX_COMMITTED:
                        committedMultiTmTransactions.add(data);
                        break;
                    case LogRecord.TX_PREPARED:
                        inDoubtTransactions.add(data);
                        break;
                    case LogRecord.JCA_TX_PREPARED:
                        inDoubtJcaTransactions.add(data);
                        break;
                    default:
                        errorLog.warn("INCONSISTENT STATE.");
                        break;
                }
            }
            if (corruptedLogRecordException != null) throw corruptedLogRecordException;
        } catch (IOException e) {
            errorLog.warn("Unexpected exception in recover:", e);
        } catch (ClassNotFoundException e) {
            errorLog.warn("Unexpected exception in recover:", e);
        }
        try {
            fis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * Removes the log file.
    */
    public void finishRecovery() {
        logFile.delete();
    }

    /**
    * Scans the log file for a valid log record. This is a helper method for
    * log file corruption handling.  
    * 
    * @param pos the file position where the forward scan should start. 
    * @return the file position in which a valid log record was found, or
    *         0 if end of file was reached and no valid log record was found. 
    */
    private long scanForward(long pos) {
        errorLog.trace("entering scanForward");
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(logFile, "r");
            while (pos + LogRecord.FULL_HEADER_LEN < logFile.length()) {
                if (match(file, pos, LogRecord.HEADER)) {
                    errorLog.trace("scanForward: match at pos=" + pos);
                    file.seek(pos + LogRecord.HEADER_LEN);
                    short recLen = file.readShort();
                    errorLog.trace("scanForward: recLen=" + recLen);
                    if (pos + LogRecord.FULL_HEADER_LEN + recLen < logFile.length()) {
                        byte[] buf = new byte[recLen];
                        file.seek(pos + LogRecord.FULL_HEADER_LEN);
                        file.read(buf, 0, recLen);
                        if (LogRecord.hasValidChecksum(buf)) {
                            errorLog.trace("scanForward: returning " + pos);
                            return pos;
                        } else {
                            errorLog.trace("scanForward: " + "bad checksum in record at pos=" + pos);
                            pos += LogRecord.HEADER_LEN;
                        }
                    } else pos = +LogRecord.HEADER_LEN;
                } else pos++;
            }
            errorLog.trace("scanForward: returning 0");
            return 0;
        } catch (FileNotFoundException e) {
            errorLog.warn("Unexpected exception in scanForward:", e);
            return 0;
        } catch (IOException e) {
            errorLog.warn("Unexpected exception in scanForward:", e);
            return 0;
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    errorLog.warn("Unexpected exception in scanForward:", e);
                }
            }
        }
    }

    /**
    * Returns true if the byte sequence that starts at given position of a
    * file matches a given byte array.
    * 
    * @param file a random access file open for reading
    * @param pos the file position of the byte sequence to be compared against 
    *            the <code>pattern</code> byte array 
    * @param pattern the byte pattern to match.
    * @return true if the pattern appears at the specified position of the
    *         file, and false otherwise/
    * @throws IOException if there is a problem reading the file.
    */
    private static boolean match(RandomAccessFile file, long pos, byte[] pattern) throws IOException {
        for (int i = 0; i < pattern.length; i++) {
            file.seek(pos + i);
            if (file.readByte() != pattern[i]) return false;
        }
        return true;
    }
}
