package org.enerj.server.pageserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Properties;
import java.util.logging.Logger;
import org.enerj.server.logentry.BeginTransactionLogEntry;
import org.enerj.server.logentry.CheckpointTransactionLogEntry;
import org.enerj.server.logentry.CommitTransactionLogEntry;
import org.enerj.server.logentry.EndDatabaseCheckpointLogEntry;
import org.enerj.server.logentry.LogEntry;
import org.enerj.util.StringUtil;
import org.odmg.DatabaseOpenException;
import org.odmg.ODMGException;

/**
 * An Archiving RedoLogServer implementation. Logs are optionally archived once a 
 * checkpoint has been reached.
 * <p>
 * Things we know about accessing the long file.
 * <ol>
 * <li>Only one instance of this class will be writing to the log at any one time. Hence, we can track
 *     where the EOF is for appending records, since we always know the size after opening it.</li>
 * <li>Once written to, the content of the file will never change. 
 *     Hence, we can keep portions of the file buffered.</li>
 * <li>Only one instance of this class will be reading the log at any one time.</li>
 * <li>It is possible that one thread may be reading while another is writing. But at most one thread will
 *     be reading and one thread will writing at any given time.</li>
 * </ol>
 *
 *
 * @version $Id: ArchivingRedoLogServer.java,v 1.4 2006/05/05 13:47:14 dsyrstad Exp $
 * @author <a href="mailto:dsyrstad@ener-j.org">Dan Syrstad</a>
 */
public class ArchivingRedoLogServer implements RedoLogServer {

    private static Logger sLogger = Logger.getLogger(ArchivingRedoLogServer.class.getName());

    private String mLogFileName;

    private BufferedRandomAccessLogFile mRandomAccessLogFile = null;

    /**
     * Construct an ArchivingRedoLogServer.
     *
     * @param someProperties properties which specify the connect parameters. See {@link #connect(Properties)}.
     *
     * @throws ODMGException if an error occurs.
     */
    private ArchivingRedoLogServer(Properties someProperties) throws ODMGException {
        String logFileName = someProperties.getProperty("ArchivingRedoLogServer.logName");
        if (logFileName == null) {
            throw new ODMGException("Cannot find ArchivingRedoLogServer.logName property");
        }
        logFileName = StringUtil.substituteMacros(logFileName, someProperties);
        init(logFileName);
    }

    /**
     * Construct an ArchivingRedoLogServer.
     *
     * @param aLogFileName the name of the log file.
     *
     * @throws ODMGException if an error occurs.
     */
    public ArchivingRedoLogServer(String aLogFileName) throws ODMGException {
        init(aLogFileName);
    }

    /**
     * Common Constructor initialization.
     *
     * @param aLogFileName the name of the log file.
     *
     * @throws ODMGException if an error occurs.
     */
    private void init(String aLogFileName) throws ODMGException {
        mLogFileName = aLogFileName;
        try {
            sLogger.fine("Opening ArchivingRedoLogServer on log file " + mLogFileName);
            mRandomAccessLogFile = new BufferedRandomAccessLogFile(mLogFileName, "rw");
            if (mRandomAccessLogFile.getChannel().tryLock() == null) {
                throw new DatabaseOpenException("Log file " + mLogFileName + " is in use by another process.");
            }
        } catch (IOException e) {
            throw new ODMGException("Unable to open log file: " + mLogFileName + ": " + e, e);
        }
    }

    /**
     * Flushes pending output physically to disk.
     *
     * @throws ODMGException if an errors occurs.
     */
    private void flush() throws ODMGException {
        synchronized (mRandomAccessLogFile) {
            try {
                mRandomAccessLogFile.flush();
                mRandomAccessLogFile.getChannel().force(false);
            } catch (IOException e) {
                throw new ODMGException("Error flushing log to disk: " + e, e);
            }
        }
    }

    /**
     * Get an instance of a RedoLogServer.
     *
     * @param someProperties properties which specify the connect parameters.
     * The properties may have the following keys:<br>
     * <ul>
     * <li> ArchivingRedoLogServer.logName - a string representing the log name. NOTE: If
     *      this is not set, <em>logging</em> will not be active and database recovery will not
     *      be possible. Also, the size of all combined transactions will be limited to
     *      what can be kept in memory.
     * <li> ArchivingRedoLogServer.shouldArchive - "true" or "false" whether logs should be archived online.
     * <li> ArchivingRedoLogServer.requestedLogSize - requested maximum log size. After
     *      the log grows to this size, no more transactions may start, the log will be
     *      fully checkpointed, and a new log will start. Because active transactions need
     *      to complete, the log may grow larger than this value. 
     * </ul>
     *
     * @return a RedoLogServer.
     *
     * @throws ODMGException in the event of an error. 
     */
    public static RedoLogServer connect(Properties someProperties) throws ODMGException {
        return new ArchivingRedoLogServer(someProperties);
    }

    public void disconnect() throws ODMGException {
        synchronized (mRandomAccessLogFile) {
            try {
                if (mRandomAccessLogFile != null) {
                    flush();
                    mRandomAccessLogFile.close();
                }
            } catch (IOException e) {
                throw new ODMGException("Error closing log " + mLogFileName + ": " + e, e);
            } finally {
                mRandomAccessLogFile = null;
            }
        }
        sLogger.fine("ArchivingRedoLogServer disconnected");
    }

    public void append(LogEntry aLogEntry) throws ODMGException {
        synchronized (mRandomAccessLogFile) {
            try {
                long entryPosition = mRandomAccessLogFile.length();
                aLogEntry.setLogPosition(entryPosition);
                if (aLogEntry instanceof BeginTransactionLogEntry) {
                    aLogEntry.setTransactionId(entryPosition);
                }
                aLogEntry.appendToLog(mRandomAccessLogFile);
                if (aLogEntry instanceof CommitTransactionLogEntry || aLogEntry instanceof CheckpointTransactionLogEntry || aLogEntry instanceof EndDatabaseCheckpointLogEntry) {
                    flush();
                }
            } catch (IOException e) {
                throw new ODMGException("Error writing log entry: " + e, e);
            }
        }
    }

    public long getFirstLogEntryPosition() throws ODMGException {
        return 0L;
    }

    /**
     * Reads an entry to the log.
     *
     * @param aLogPosition the position of the entry in the log.
     *
     * @return a LogEntry. LogEntry.getNextLogEntryPosition() will provide
     *  the position of the next log entry.
     *
     * @throws ODMGException in the event of an error. 
     */
    public LogEntry read(long aLogPosition) throws ODMGException {
        synchronized (mRandomAccessLogFile) {
            try {
                mRandomAccessLogFile.seek(aLogPosition);
                return LogEntry.createFromLog(mRandomAccessLogFile, aLogPosition);
            } catch (IOException e) {
                throw new ODMGException("Error reading log entry at " + aLogPosition + ": " + e, e);
            }
        }
    }

    /**
     * Buffered version of RandomAccessFile specifically designed to support the log. 
     * It's not general purpose because writes do not write through to the read buffer.
     * Note that this code originated from SwiftVis by Mark Lewis at http://www.cs.trinity.edu/~mlewis/SwiftVis/
     * on 10/13/2006. The site says "SwiftVis is an open source work in progress.", but does not
     * claim a copyright nor license on any of the source distribution files.
     * I contacted Mark about the status and he has given permission to use BufferedRandomAccessFile. 
     * <p>
     * 
     * @version $Id: $
     * @author <a href="mailto:dsyrstad@ener-j.org">Dan Syrstad </a>
     */
    private static final class BufferedRandomAccessLogFile extends RandomAccessFile {

        private static final int DEFAULT_READ_BUFFER_SIZE = 8192;

        private static final int DEFAULT_WRITE_BUFFER_SIZE = 5 * 8192;

        private byte[] mReadBuf = new byte[DEFAULT_READ_BUFFER_SIZE];

        private int mReadBufLength = 0;

        private int mReadBufCurrentOfs = 0;

        private byte[] mWriteBuf = new byte[DEFAULT_WRITE_BUFFER_SIZE];

        private int mWriteBufCurrentOfs = 0;

        public BufferedRandomAccessLogFile(File f, String mode) throws FileNotFoundException {
            super(f, mode);
        }

        public BufferedRandomAccessLogFile(String f, String mode) throws FileNotFoundException {
            super(f, mode);
        }

        public BufferedRandomAccessLogFile(File f, String mode, int bufSize) throws FileNotFoundException {
            super(f, mode);
            mReadBuf = new byte[bufSize];
        }

        @Override
        public int read() throws IOException {
            if (mReadBufCurrentOfs >= mReadBufLength) {
                fillBuffer();
                if (mReadBufLength < 1) return -1;
            }
            ++mReadBufCurrentOfs;
            return mReadBuf[mReadBufCurrentOfs - 1] & 0xff;
        }

        @Override
        public int read(byte[] buf) throws IOException {
            return read(buf, 0, buf.length);
        }

        @Override
        public int read(byte[] buf, int offset, int len) throws IOException {
            int intoOffset = offset;
            int endIdx = offset + len;
            int numBytesRead = 0;
            while (intoOffset < endIdx) {
                if (mReadBufCurrentOfs >= mReadBufLength) {
                    fillBuffer();
                    if (mReadBufLength < 1) return numBytesRead;
                }
                int numBytesLeftInBuf = len - intoOffset;
                int numBytesLeftInReadBuf = mReadBufLength - mReadBufCurrentOfs;
                int numBytesToCopy = (numBytesLeftInBuf < numBytesLeftInReadBuf ? numBytesLeftInBuf : numBytesLeftInReadBuf);
                System.arraycopy(mReadBuf, mReadBufCurrentOfs, buf, intoOffset, numBytesToCopy);
                intoOffset += numBytesToCopy;
                mReadBufCurrentOfs += numBytesToCopy;
                numBytesRead += numBytesToCopy;
            }
            return numBytesRead;
        }

        /**
         * Flushes the write buffer.
         *
         * @throws IOException
         */
        void flush() throws IOException {
            if (mWriteBufCurrentOfs > 0) {
                seek(super.length());
                super.write(mWriteBuf, 0, mWriteBufCurrentOfs);
                mWriteBufCurrentOfs = 0;
            }
        }

        @Override
        public void close() throws IOException {
            flush();
            super.close();
        }

        @Override
        public long length() throws IOException {
            return super.length() + mWriteBufCurrentOfs;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int numLeft = len;
            while (numLeft > 0) {
                if (mWriteBufCurrentOfs >= mWriteBuf.length) {
                    flush();
                }
                int numWriteBufLeft = mWriteBuf.length - mWriteBufCurrentOfs;
                int numToCopy = (numLeft > numWriteBufLeft ? numWriteBufLeft : numLeft);
                System.arraycopy(b, off, mWriteBuf, mWriteBufCurrentOfs, numToCopy);
                off += numToCopy;
                mWriteBufCurrentOfs += numToCopy;
                numLeft -= numToCopy;
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(int b) throws IOException {
            if (mWriteBufCurrentOfs >= mWriteBuf.length) {
                flush();
            }
            mWriteBuf[mWriteBufCurrentOfs] = (byte) (b & 0xff);
            ++mWriteBufCurrentOfs;
        }

        public void seek(long filePos) throws IOException {
            super.seek(filePos);
            mReadBufLength = 0;
        }

        public long getFilePointer() throws IOException {
            throw new IOException("Do not call this method because read & writes are buffered.");
        }

        private void fillBuffer() throws IOException {
            mReadBufCurrentOfs = 0;
            mReadBufLength = super.read(mReadBuf);
        }
    }
}
