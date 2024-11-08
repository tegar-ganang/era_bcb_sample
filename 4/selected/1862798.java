package org.xactor.tm.recovery;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.jboss.logging.Logger;

/**
 * Class that encapsulates a log file opened for writing. It provides 
 * <code>write</code> methods that append log records to the log file.
 * A <code>BatchLog</code> instance belongs to some <code>BatchWriter</code>
 * and its write methods are called only from the <code>BatchWriter</code>
 * thread. 
 * <p>
 * To avoid changes in its metadata, the log file is created with a fixed
 * length, which should never change until the file is closed. 
 * <p>
 * A newly created log file is clean: it contains a header object followed
 * by a sequence of null bytes that takes the entire lenght of the file. Log
 * files are reusable, but they must be cleaned up for reuse. Restarting a
 * <code>BatchLog</code> means overwriting with null bytes all the data that 
 * follows the header and returning the <code>BatchLog</code> to the pool of 
 * clean <code>BatchLog</code> instances maintained by a 
 * <code>BatchWriter</code>.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 37634 $
 */
class BatchLog implements TxCompletionHandler {

    /**
    * Class <code>Logger</code>, for trace messages.
    */
    private static Logger errorLog = Logger.getLogger(BatchLog.class);

    /** 
    * Length of the null-filled buffer used to clean log file. 
    */
    private static final int CLEAN_LENGTH = 16 * 1024;

    /** 
    * Buffer used to fill the log file with null bytes. 
    */
    private static byte[] nulls = new byte[CLEAN_LENGTH];

    /** 
    * The log file. 
    */
    private File logFile;

    /**
    *  A <code>RandomAccessFile</code> view of the log file.
    */
    private RandomAccessFile os;

    /**
    * The <code>RandomAccessFile</code>'s underlying <code>FileChannel</code>.
    */
    private FileChannel channel;

    /**
    * Number of transactions logged. Counts commit, multi-TM commit, prepare, 
    * and JCA prepare records written out to the log. Only the 
    * <code>BatchWriter</code> thread updates this field, which is declared 
    * as volatile so that its updates are seen by any threads that call
    * <code>handleTxCompletion</code>.
    */
    private volatile int numLoggedTransactions;

    /**
    * Number of end records written out to the log. Only the 
    * <code>BatchWriter</code> thread updates this field, which is declared 
    * as volatile so that its updates are seen by any threads that call
    * <code>handleTxCompletion</code>.
    */
    private volatile int numEndRecords;

    /**
    * Number of completed transactions that need no end records. Access to this
    * field is synchronized, as it is concurrently updated via calls to 
    * <code>handleTxCompletion</code>.
    */
    private int numLocalTransactionsCompleted;

    /**
    * Indicates that this <code>BatchLog</code> will not receive additional
    * records for new transactions. This <code>BatchLog</code> should be 
    * cleaned up and returned to the <code>BatchWriter</code>'s pool of clean
    * <code>BatchLog</code> instances as soon as every outstanding transaction
    * signals its completion either by writing out an end record or by invoking
    * <code>handleTxCompletion</code>.  
    */
    private boolean markedForRestart;

    /** 
    * Header object written to the beginning of the log file via Java 
    * serialization. It is preceded by four bytes (an int value) with the 
    * length of the serialized header.
    */
    private Object header;

    /**
    * Log file position that follows the four bytes with the header lenght and
    * the header object. (Its value is headerLenght + 4.)
    */
    private long topFp;

    /**
    * The <code>BatchWriter</code> that owns this <code>BatchLog</code>.
    */
    private BatchWriter writer;

    /**
    * Auxiliary buffer used to fill up with null bytes the part of the file 
    * that follows the header.
    */
    private ByteBuffer cleanBuffer = ByteBuffer.wrap(nulls);

    /**
    * Constructs a new <code>BatchLog</code>.
    * 
    * @param writer   the <code>BatchWriter</code> that will own 
    *                 the new <code>BatchLog</code> 
    * @param header   an object to be written at the beginning of the log file
    * @param dir      the directory in which the log file will be created
    * @param fileSize the fixed size of the log file
    * @throws IOException
    */
    BatchLog(BatchWriter writer, Object header, File dir, int fileSize) throws IOException {
        this.writer = writer;
        this.header = header;
        logFile = File.createTempFile("TX_RECOVERY_LOG", ".log", dir);
        os = new RandomAccessFile(logFile, "rw");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(header);
        byte[] bytes = baos.toByteArray();
        os.setLength(fileSize);
        os.writeInt(bytes.length);
        os.write(bytes);
        channel = os.getChannel();
        channel.force(true);
        topFp = channel.position();
        cleanUpLogFile();
    }

    /**
    * Gets the <code>BatchWriter</code> that owns this <code>BatchLog</code>.
    *    
    * @return the <code>BatchWriter</code> that issues write calls on this
    *         this <code>BatchLog</code>.   
    */
    BatchWriter getBatchWriter() {
        return writer;
    }

    /**
    * Gets this <code>BatchLog</code>'s current position, which is also the
    * number of currently used bytes of its log file.
    *   
    * @return the offset from the beginning of the log file, in bytes, 
    *         at which the next write will happen.
    * @throws IOException
    */
    int getPosition() throws IOException {
        return (int) channel.position();
    }

    /**
    * Gets the name of the underlying log file.
    *  
    * @return the name of this <code>BatchLog</code>'s log file. 
    */
    String getFilename() {
        return logFile.getName();
    }

    /**
    * Writes one record at the current position of this <code>BatchLog</code>.
    *  
    * @param record      a buffer with the record to be written
    * @param isEndRecord true if the record is an end record and false otherwise
    * @throws IOException
    */
    void write(ByteBuffer record, boolean isEndRecord) throws IOException {
        channel.write(record);
        if (isEndRecord) {
            numEndRecords++;
            synchronized (this) {
                if (markedForRestart == true && numLoggedTransactions == numLocalTransactionsCompleted + numEndRecords) {
                    writer.restartBatchLog(this);
                }
            }
        } else {
            channel.force(false);
            numLoggedTransactions++;
        }
    }

    /**
    * Writes a sequence of records to this <code>BatchLog</code>.
    * The sequence of records is taken from the given array of buffers, 
    * starting at the specified offset, and it is written at the current
    * position of this <code>BatchLog</code>.
    * 
    * @param records an array of buffers containing records that should be 
    *                written to the log
    * @param offset  the index of the first record of the <code>records</code> 
    *                array that should be written to the log
    * @param length  the number of records that should be written to the log
    * @param numTransactionRecords specifies how many of the <code>lenght</code>
    *                records are records for new transactions (commit, multi-TM
    *                commit, prepare and JCA prepare records). The remaining
    *                <code>length - numTransactionRecords</code> records are
    *                end-of-transaction records.
    * @throws IOException
    */
    void write(ByteBuffer[] records, int offset, int length, int numTransactionRecords) throws IOException {
        channel.write(records, offset, length);
        if (numTransactionRecords > 0) {
            channel.force(false);
            numLoggedTransactions += numTransactionRecords;
        }
        if (numTransactionRecords < length) {
            numEndRecords += (length - numTransactionRecords);
            synchronized (this) {
                if (markedForRestart == true && numLoggedTransactions == numLocalTransactionsCompleted + numEndRecords) {
                    writer.restartBatchLog(this);
                }
            }
        }
    }

    /**
    * Signals the end of the two-phase commit protocol for a committed 
    * transaction that does not need an end record to be logged. This method 
    * should be invoked when the second phase of the two-phase commit protocol 
    * completes successfully.
    *
    * @param localTransactionId the local id of the completed transaction.
    */
    public void handleTxCompletion(long localTransactionId) {
        synchronized (this) {
            numLocalTransactionsCompleted++;
            if (markedForRestart == true && numLoggedTransactions == numLocalTransactionsCompleted + numEndRecords) {
                writer.restartBatchLog(this);
            }
        }
    }

    /**
    * Marks this <code>BatchLog</code> for restart. The restart will occur
    * only when there are no more outstanding transactions, i.e, when
    * <code>numLocalTransactionsCompleted + numEndRecords</code> reaches
    * <code>numLoggedTransactions</code>.
    */
    void markForRestart() {
        synchronized (this) {
            markedForRestart = true;
            if (numLoggedTransactions == numLocalTransactionsCompleted + numEndRecords) {
                writer.restartBatchLog(this);
            }
        }
    }

    /**
    * Restarts this <code>BatchLog</code>. Overwrites with null bytes all 
    * log records in the log file, then returns the <code>BatchLog</code> 
    * to its <code>BatchWriter</code>s pool of clean <code>BatchLog</code>
    * instances. 
    * <p>
    * Only the <code>LogRestarter</code> calls this method.
    * 
    * @throws IOException
    */
    void restart() throws IOException {
        channel.position(topFp);
        cleanUpLogFile();
        writer.getNextLogs().add(this);
    }

    /**
    * Overwrites with null bytes all log records in the log file, without
    * changing the size of the file. 
    */
    void cleanUpLogFile() throws IOException {
        numLoggedTransactions = 0;
        numLocalTransactionsCompleted = 0;
        numEndRecords = 0;
        markedForRestart = false;
        cleanBuffer.limit(cleanBuffer.capacity());
        while (channel.position() <= channel.size() - cleanBuffer.limit()) {
            cleanBuffer.rewind();
            channel.write(cleanBuffer);
        }
        cleanBuffer.limit((int) (channel.size() - channel.position()));
        cleanBuffer.rewind();
        channel.write(cleanBuffer);
        channel.force(false);
        channel.position(topFp);
    }

    /**
    * Closes this <code>BatchLog</code>. If there are no outstanding
    * transactions then all log records in the log file are erased.
    */
    void close() {
        errorLog.info("Closing transaction log " + getFilename() + ", numLoggedTransactions=" + numLoggedTransactions + ", numLocalTransactionsCompleted=" + numLocalTransactionsCompleted + ", numEndRecords=" + numEndRecords);
        try {
            if (numLoggedTransactions == numLocalTransactionsCompleted + numEndRecords) {
                channel.position(topFp);
                channel.truncate(topFp);
            }
            os.close();
        } catch (IOException e) {
            errorLog.error("Error closing transaction log " + getFilename(), e);
        }
    }
}
