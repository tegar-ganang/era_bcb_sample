package com.continuent.tungsten.replicator.thl.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.continuent.tungsten.replicator.ReplicatorException;
import com.continuent.tungsten.replicator.thl.THLException;
import com.continuent.tungsten.replicator.thl.serializer.ProtobufSerializer;
import com.continuent.tungsten.replicator.thl.serializer.Serializer;

/**
 * This class implements a multi-thread disk log store.
 * 
 * @author <a href="mailto:stephane.giron@continuent.com">Stephane Giron</a>
 * @version 1.0
 */
public class DiskLog {

    static Logger logger = Logger.getLogger(DiskLog.class);

    private static long FIRST = 0;

    LogCursorManager cursorManager;

    private Serializer eventSerializer = null;

    private File logDir;

    private LogConnectionManager connectionManager = new LogConnectionManager();

    private LogIndex index = null;

    private long fileIndex = 1;

    private static final int fileIndexSize = Integer.toString(Integer.MAX_VALUE).length();

    private static final String DATA_FILENAME_PREFIX = "thl.data.";

    /** Store and compare checksum values on the log. */
    private boolean doChecksum = true;

    /** Name of the log directory. */
    protected String logDirName = "/opt/tungsten/logs";

    /** Name of the class used to serialize events. */
    protected String eventSerializerClass = ProtobufSerializer.class.getName();

    /** Log file maximum size in bytes. */
    protected int logFileSize = 1000000000;

    /** Wait timeout. This is used for testing to prevent infinite timeouts. */
    protected int timeoutMillis = Integer.MAX_VALUE;

    /**
     * Special timeout when waiting for a new log file after a rotate log. This
     * timeout will normally only expire when there is a corrupt log.
     */
    protected int logRotateMillis = 60000;

    /** Number of milliseconds to retain old logs. */
    protected long logFileRetainMillis = 0;

    /**
     * Number of milliseconds before timing out idle log connections. Defaults
     * to 8 hours.
     */
    protected int logConnectionTimeoutMillis = 28800000;

    /**
     * I/O buffer size for log file access. Larger is better.
     */
    protected int bufferSize = 65536;

    /** Write lock to prevent log file corruption by concurrent access. */
    protected WriteLock writeLock;

    /** Indicates whether access should be read only or not */
    protected boolean readOnly = true;

    /**
     * Flush data after this many milliseconds. 0 flushes after every write.
     */
    private long flushIntervalMillis = 0;

    /**
     * If true, fsync when flushing.
     */
    private boolean fsyncOnFlush = false;

    /**
     * Log flush task; enabled if asynchronous flush interval is greater than 0.
     */
    private LogFlushTask logSyncTask;

    private Thread logSyncThread;

    /**
     * Creates a new log instance.
     */
    public DiskLog() {
    }

    /**
     * Sets the directory that will be used to store the log files
     * 
     * @param path directory to be used. Last / is optional.
     */
    public void setLogDir(String path) {
        this.logDirName = path.trim();
        if (this.logDirName.charAt(this.logDirName.length() - 1) != '/') {
            this.logDirName = this.logDirName.concat("/");
        }
    }

    /**
     * Sets the log file size. This is approximate as rotation will occur after
     * storing an event that made the file grow above the given limit.
     * 
     * @param size file size
     */
    public void setLogFileSize(int size) {
        this.logFileSize = size;
    }

    /**
     * Returns the log file size.
     */
    public int getLogFileSize() {
        return logFileSize;
    }

    /**
     * Determines whether to checksum log records.
     * 
     * @param doChecksum If true use checksums
     */
    public void setDoChecksum(boolean doChecksum) {
        this.doChecksum = doChecksum;
    }

    /**
     * Return true if checksums are enabled.
     */
    public boolean isDoChecksum() {
        return this.doChecksum;
    }

    /**
     * Set the number of milliseconds to retain old log files.
     * 
     * @param logFileRetainMillis If other than 0, logs are retained for this
     *            amount of time
     */
    public void setLogFileRetainMillis(long logFileRetainMillis) {
        this.logFileRetainMillis = logFileRetainMillis;
    }

    /**
     * Set the number of milliseconds before timing out idle log connections.
     * 
     * @param logConnectionTimeoutMillis Time in milliseconds
     */
    public void setLogConnectionTimeoutMillis(int logConnectionTimeoutMillis) {
        this.logConnectionTimeoutMillis = logConnectionTimeoutMillis;
    }

    /**
     * Sets the event serializer class name.
     */
    public void setEventSerializerClass(String eventSerializerClass) {
        this.eventSerializerClass = eventSerializerClass;
    }

    /**
     * Returns the event serializer instance.
     */
    public Serializer getEventSerializer() {
        return eventSerializer;
    }

    /**
     * Sets the timeout value for blocking reads. This value is only changed
     * when testing.
     */
    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Returns the current timeout value for blocking reads.
     */
    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Sets the timeout value for reading a new file after a log rotation.
     */
    public void setLogRotateMillis(int logRotateMillis) {
        this.logRotateMillis = logRotateMillis;
    }

    /**
     * Returns the current timeout value for log rotation.
     */
    public int getLogRotateMillis() {
        return logRotateMillis;
    }

    /**
     * Sets the log buffer size.
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Set write flush interval in milliseconds. 0 means flush on every write.
     * This lowers latency.
     */
    public void setFlushIntervalMillis(long flushIntervalMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
    }

    /**
     * Return flush interval in milliseconds.
     */
    public long getFlushIntervalMillis() {
        return flushIntervalMillis;
    }

    /**
     * If set to true, perform an fsync with every flush. Warning: fsync is very
     * slow, so you want a long flush interval in this case.
     */
    public synchronized void setFsyncOnFlush(boolean fsyncOnFlush) {
        this.fsyncOnFlush = fsyncOnFlush;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Prepare the log for use, which includes ensuring that the log is created
     * automatically on first use and building an index of log file contents.
     */
    public void prepare() throws ReplicatorException, InterruptedException {
        logger.info(String.format("Using directory '%s' for replicator logs", logDirName));
        logger.info("Checksums enabled for log records: " + doChecksum);
        if (logger.isDebugEnabled()) {
            logger.debug("logFileSize = " + logFileSize);
        }
        logDir = new File(logDirName);
        if (!logDir.exists()) {
            if (readOnly) {
                throw new ReplicatorException("Log directory does not exist : " + logDir.getAbsolutePath());
            } else {
                logger.info("Log directory does not exist; creating now:" + logDir.getAbsolutePath());
                if (!logDir.mkdirs()) {
                    throw new ReplicatorException("Unable to create log directory: " + logDir.getAbsolutePath());
                }
            }
        }
        if (!logDir.isDirectory()) {
            throw new ReplicatorException("Log directory is not a directory: " + logDir.getAbsolutePath());
        }
        if (readOnly) {
            logger.info("Using read-only log connection");
        } else {
            if (!logDir.canWrite()) {
                throw new ReplicatorException("Log directory is not writable: " + logDir.getAbsolutePath());
            }
            File lockFile = new File(logDir, "disklog.lck");
            if (logger.isDebugEnabled()) {
                logger.debug("Attempting to acquire lock on write lock file: " + lockFile.getAbsolutePath());
            }
            writeLock = new WriteLock(lockFile);
            writeLock.acquire();
            if (writeLock.isLocked()) logger.info("Acquired write lock; log is writable"); else logger.info("Unable to acquire write lock; log is read-only");
        }
        try {
            eventSerializer = (Serializer) Class.forName(eventSerializerClass).newInstance();
        } catch (Exception e) {
            throw new ReplicatorException("Unable to load event serializer class: " + eventSerializerClass, e);
        }
        logger.info("Loaded event serializer class: " + eventSerializer.getClass().getName());
        if (listLogFiles(logDir, DATA_FILENAME_PREFIX).length == 0) {
            if (readOnly) {
                throw new ReplicatorException("Attempting to read a non-existent log; is log initialized? dirName=" + logDir.getAbsolutePath());
            } else {
                String logFileName = getDataFileName(fileIndex);
                LogFile logFile = new LogFile(logDir, logFileName);
                logFile.setBufferSize(bufferSize);
                logger.info("Initializing logs: logDir=" + logDir.getAbsolutePath() + " file=" + logFile.getFile().getName());
                logFile.create(-1);
                logFile.close();
            }
        }
        if (logger.isDebugEnabled()) logger.debug("Preparing index");
        index = new LogIndex(logDir, DATA_FILENAME_PREFIX, logFileRetainMillis, bufferSize);
        LogFile logFile = openLastFile(readOnly);
        String logFileName = logFile.getFile().getName();
        int logFileIndexPos = logFile.getFile().getName().lastIndexOf(".");
        fileIndex = Long.valueOf(logFileName.substring(logFileIndexPos + 1));
        long maxSeqno = logFile.getBaseSeqno();
        long lastCompleteEventOffset = logFile.getOffset();
        boolean lastFrag = true;
        if (logger.isDebugEnabled()) logger.debug("Starting max seqno is " + maxSeqno);
        try {
            logger.info("Validating last log file: " + logFile.getFile().getAbsolutePath());
            LogRecord currentRecord = null;
            currentRecord = logFile.readRecord(0);
            byte lastRecordType = -1;
            while (!currentRecord.isEmpty()) {
                lastRecordType = currentRecord.getData()[0];
                if (lastRecordType == LogRecord.EVENT_REPL) {
                    LogEventReplReader eventReader = new LogEventReplReader(currentRecord, eventSerializer, doChecksum);
                    lastFrag = eventReader.isLastFrag();
                    if (lastFrag) {
                        maxSeqno = eventReader.getSeqno();
                        lastCompleteEventOffset = logFile.getOffset();
                    }
                    eventReader.done();
                } else if (lastRecordType == LogRecord.EVENT_ROTATE) {
                    String fileName = logFile.getFile().getName();
                    logger.info("Last log file ends on rotate log event: " + fileName);
                    logFile.close();
                    if (!readOnly) {
                        index.setMaxIndexedSeqno(maxSeqno);
                        logFileIndexPos = fileName.lastIndexOf(".");
                        fileIndex = Long.valueOf(fileName.substring(logFileIndexPos + 1));
                        fileIndex = (fileIndex + 1) % Integer.MAX_VALUE;
                        logFile = this.startNewLogFile(maxSeqno + 1);
                    }
                    break;
                }
                currentRecord = logFile.readRecord(0);
            }
            index.setMaxIndexedSeqno(maxSeqno);
            if (!readOnly && currentRecord.isTruncated()) {
                if (writeLock.isLocked()) {
                    logger.warn("Log file contains partially written record: offset=" + currentRecord.getOffset() + " partially written bytes=" + (logFile.getLength() - currentRecord.getOffset()));
                    logFile.setLength(currentRecord.getOffset());
                    logger.info("Log file truncated to end of last good record: length=" + logFile.getLength());
                } else {
                    logger.warn("Log ends with a partially written record " + "at end, but this log is read-only.  " + "It is possible that the process that " + "owns the write lock is still writing it.");
                }
            }
            if (!readOnly && !lastFrag) {
                if (writeLock.isLocked()) {
                    logger.warn("Log file contains partially written transaction; " + "truncating to last full transaction: seqno=" + maxSeqno + " length=" + lastCompleteEventOffset);
                    logFile.setLength(lastCompleteEventOffset);
                } else {
                    logger.warn("Log ends with a partially written " + "transaction, but this log is read-only.  " + "It is possible that the process that " + "owns the write lock is still writing it.");
                }
            }
        } catch (IOException e) {
            throw new ReplicatorException("I/O error while scanning log file: name=" + logFile.getFile().getAbsolutePath() + " offset=" + logFile.getOffset(), e);
        } finally {
            if (logFile != null) logFile.close();
        }
        logger.info("Setting up log flush policy: fsyncIntervalMillis=" + flushIntervalMillis + " fsyncOnFlush=" + this.fsyncOnFlush);
        if (!this.readOnly) {
            startLogSyncTask();
        }
        this.cursorManager = new LogCursorManager();
        cursorManager.setTimeoutMillis(logConnectionTimeoutMillis);
        logger.info(String.format("Idle log connection timeout: %dms", logConnectionTimeoutMillis));
        logger.info("Log preparation is complete");
    }

    /**
     * Releases the log resources. This should be called after use to ensure log
     * sync task termination.
     */
    public void release() throws ReplicatorException, InterruptedException {
        connectionManager.releaseAll();
        if (!readOnly) writeLock.release();
        stopLogSyncTask();
    }

    private void startLogSyncTask() {
        if (flushIntervalMillis > 0) {
            logSyncTask = new LogFlushTask(flushIntervalMillis);
            logSyncThread = new Thread(logSyncTask, "log-sync-" + logDir.getName());
            logSyncThread.start();
            logger.info("Started deferred log sync thread: " + logSyncThread.getName());
        }
    }

    private void stopLogSyncTask() throws InterruptedException {
        if (logSyncThread != null) {
            logger.info("Stopping deferred log sync thread: " + logSyncThread.getName());
            logSyncTask.cancel();
            logSyncThread.interrupt();
            try {
                logSyncThread.join(5000);
            } finally {
                if (logSyncThread.isAlive()) logger.warn("Unable to terminate log sync thread: " + logSyncThread.getName());
                logSyncThread = null;
            }
        }
    }

    /**
     * Ensure the log sync tasks is running.
     */
    void checkLogSyncTask() throws InterruptedException {
        if (flushIntervalMillis > 0 && logSyncTask.isFinished()) {
            stopLogSyncTask();
            startLogSyncTask();
        }
    }

    /**
     * Updates the active sequence number. Log files will be retained if they
     * contain this number or above.
     */
    public void setActiveSeqno(long activeSeqno) {
        index.setActiveSeqno(activeSeqno);
    }

    /**
     * Returns the active sequence number.
     */
    public long getActiveSeqno() {
        if (index != null) return index.getActiveSeqno(); else return -1;
    }

    /**
     * Return the maximum sequence number stored in the log.
     */
    public long getMaxSeqno() {
        if (logger.isDebugEnabled()) logger.debug("Getting max seqno for thread " + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") using " + this);
        return index.getMaxIndexedSeqno();
    }

    /**
     * Sets the maximum sequence number stored in the log.
     */
    public void setMaxSeqno(long seqno) {
        index.setMaxIndexedSeqno(seqno);
    }

    /**
     * Return the minimum sequence number stored in the log.
     */
    public long getMinSeqno() {
        return index.getMinIndexedSeqno();
    }

    /**
     * Returns the count of files in the log.
     */
    public int fileCount() {
        return index.size();
    }

    /**
     * Returns an array of log files.
     */
    public String[] getLogFileNames() {
        return index.getFileNames();
    }

    /**
     * Returns true if this log is writable.
     */
    public boolean isWritable() {
        return (!readOnly && writeLock.isLocked());
    }

    /**
     * Creates a new log connection.
     * 
     * @param readonly If true, for read only. Only one active connection may
     *            write at any given time.
     * @return A new log client.
     */
    public LogConnection connect(boolean readonly) throws ReplicatorException {
        LogConnection client = new LogConnection(this, readonly);
        if (logger.isDebugEnabled()) logger.debug("Client connect to log: connection=" + client.toString());
        connectionManager.store(client);
        return client;
    }

    /**
     * Releases a log connection.
     * 
     * @param connection Connection to release
     */
    public void release(LogConnection connection) {
        connectionManager.release(connection);
    }

    /**
     * Rotate to the next file to store data : write the rotate event, close the
     * file and prepare the new one, if it does not exists
     * 
     * @dataFile Data file to be rotated
     * @seqno Sequence number of first event in new file
     */
    LogFile rotate(LogFile dataFile, long seqno) throws IOException, ReplicatorException, InterruptedException {
        fileIndex = (fileIndex + 1) % Integer.MAX_VALUE;
        try {
            LogEventRotateWriter writer = new LogEventRotateWriter(fileIndex, doChecksum);
            LogRecord logRec = writer.write();
            dataFile.writeRecord(logRec, 0);
        } catch (IOException e) {
            throw new THLException("Error writing rotate log event to log file: name=" + dataFile.getFile().getName(), e);
        }
        return startNewLogFile(seqno);
    }

    /**
     * Returns the log file containing a particular seqno or null if it does not
     * exist.
     */
    LogFile getLogFile(long seqno) {
        String name;
        if (seqno == FIRST) name = index.getFirstFile(); else name = index.getFile(seqno);
        if (name == null) return null; else {
            LogFile logFile = new LogFile(logDir, name);
            logFile.setBufferSize(bufferSize);
            return logFile;
        }
    }

    /**
     * Returns the log file corresponding to the log file name.
     */
    LogFile getLogFile(String name) {
        if (index.fileNameExists(name)) {
            LogFile logFile = new LogFile(logDir, name);
            logFile.setBufferSize(bufferSize);
            return logFile;
        } else return null;
    }

    /**
     * Returns the name of a log file based on an index
     * 
     * @return a file name corresponding to the given index
     */
    String getDataFileName(long index) {
        return DATA_FILENAME_PREFIX + String.format("%0" + fileIndexSize + "d", index);
    }

    /**
     * Opens a log file for reading if it exists. Caller must release the log
     * file.
     * 
     * @param newFileName Name of the file null if the file does not exist
     * @throws ReplicatorException If file exists but cannot be opened
     * @throws InterruptedException Thrown if we are interrupted
     */
    LogFile getLogFileForReading(String newFileName) throws ReplicatorException, InterruptedException {
        File newFile = new File(logDir, newFileName);
        if (newFile.exists()) {
            LogFile logFile = new LogFile(newFile);
            logFile.setBufferSize(bufferSize);
            logFile.openRead();
            return logFile;
        } else {
            return null;
        }
    }

    /**
     * Validates the log to ensure there are no inconsistencies.
     * 
     * @throws LogConsistencyException Thrown if log is not consistent
     */
    public void validate() throws LogConsistencyException {
        index.validate(logDir);
    }

    /**
     * Deletes a portion of the log. This operation requires a file lock to
     * accomplish.
     * 
     * @param client Disk log client used for deletion
     * @param low Sequence number specifying the beginning of the range. Leave
     *            null to start from the current beginning of the log.
     * @param high Sequence number specifying the end of the range. Leave null
     *            to delete to the end of the log.
     * @throws ReplicatorException Thrown if delete fails
     */
    public void delete(LogConnection client, Long low, Long high) throws ReplicatorException, InterruptedException {
        if (readOnly || !writeLock.isLocked()) {
            throw new THLException("Attempt to delete from read-only log");
        }
        long lowSeqno;
        long highSeqno;
        if (low == null) lowSeqno = index.getMinIndexedSeqno(); else lowSeqno = low;
        if (high == null) highSeqno = index.getMaxIndexedSeqno(); else highSeqno = high;
        if (highSeqno != index.getMaxIndexedSeqno() && lowSeqno != index.getMinIndexedSeqno()) {
            throw new THLException("Deletion range invalid; " + "must include one or both log end points: low seqno=" + lowSeqno + " high seqno=" + highSeqno);
        }
        for (LogIndexEntry lie : index.getIndexCopy()) {
            if (lie.startSeqno >= lowSeqno && lie.endSeqno <= highSeqno) {
                logger.info("Deleting log file: " + lie.toString());
                purgeFile(lie);
            } else if (lie.startSeqno < lowSeqno && lie.endSeqno >= lowSeqno) {
                logger.info("Truncating log file at seqno " + lowSeqno + ": " + lie.toString());
                truncateFile(client, lie, lowSeqno);
            }
        }
    }

    private void purgeFile(LogIndexEntry entry) {
        index.removeFile(entry.fileName);
        File f = new File(logDir, entry.fileName);
        if (!f.delete()) {
            logger.warn("Unable to delete log file: " + f.getAbsolutePath());
        }
    }

    private void truncateFile(LogConnection client, LogIndexEntry entry, long seqno) throws ReplicatorException, InterruptedException {
        LogFile logFile = null;
        try {
            cursorManager.releaseConnection(client);
            logFile = openFile(entry.fileName, false);
            long offset = logFile.getOffset();
            LogRecord currentRecord = logFile.readRecord(0);
            while (!currentRecord.isEmpty()) {
                byte recordType = currentRecord.getData()[0];
                if (recordType == LogRecord.EVENT_REPL) {
                    LogEventReplReader eventReader = new LogEventReplReader(currentRecord, eventSerializer, doChecksum);
                    long currentSeqno = eventReader.getSeqno();
                    eventReader.done();
                    if (currentSeqno >= seqno) {
                        logger.info("Truncating log file after sequence number: file=" + entry.fileName + " seqno=" + seqno);
                        logFile.setLength(offset);
                        index.setMaxIndexedSeqno(seqno - 1);
                        break;
                    }
                } else if (recordType == LogRecord.EVENT_ROTATE) {
                    logger.warn("Unable to truncate log file at intended sequence number: file=" + entry.fileName + " seqno=" + seqno);
                    break;
                }
                offset = logFile.getOffset();
                currentRecord = logFile.readRecord(0);
            }
        } catch (IOException e) {
            throw new THLException("Unable to read log file: " + entry.fileName, e);
        } catch (ReplicatorException e) {
            throw new THLException("Unable to process log file: " + entry.fileName, e);
        } finally {
            if (logFile != null) logFile.close();
        }
    }

    /**
     * Open the last log file for writing. The file is assumed to exist as the
     * log must be initialized at this point.
     * 
     * @param readOnly
     * @return a {@link LogFile} object referencing the last indexed file
     * @throws ReplicatorException if an error occurs
     * @throws InterruptedException Thrown if we are interrupted
     */
    LogFile openLastFile(boolean readOnly) throws ReplicatorException, InterruptedException {
        String logFileName = index.getLastFile();
        return openFile(logFileName, readOnly);
    }

    /**
     * Open a specific log file for writing.
     * 
     * @param logFileName Log file name
     * @param readOnly
     * @return a {@link LogFile} object referencing the last indexed file
     * @throws ReplicatorException if an error occurs
     * @throws InterruptedException Thrown if we are interrupted
     */
    private LogFile openFile(String logFileName, boolean readOnly) throws ReplicatorException, InterruptedException {
        LogFile data = new LogFile(logDir, logFileName);
        if (!readOnly) {
            data.setLogSyncTask(logSyncTask);
            data.setFlushIntervalMillis(flushIntervalMillis);
            data.setFsyncOnFlush(readOnly);
        }
        data.setBufferSize(bufferSize);
        if (!data.getFile().exists()) {
            throw new ReplicatorException("Last log file does not exist; index may be corrupt: " + data.getFile().getName());
        }
        if (logger.isDebugEnabled()) logger.debug("Opening log file: " + data.getFile().getAbsolutePath());
        if (readOnly) data.openRead(); else data.openWrite();
        return data;
    }

    /**
     * Start a new log file.
     * 
     * @seqno Sequence number of first event in the file
     */
    private LogFile startNewLogFile(long seqno) throws ReplicatorException, IOException, InterruptedException {
        String logFileName = getDataFileName(fileIndex);
        LogFile dataFile = new LogFile(logDir, logFileName);
        dataFile.setBufferSize(bufferSize);
        if (dataFile.getFile().exists()) {
            throw new THLException("New log file exists already: " + dataFile.getFile().getName());
        }
        dataFile.create(seqno);
        index.addNewFile(seqno, logFileName);
        return dataFile;
    }

    /**
     * getIndex returns a String representation of the index, built from the
     * configured log directory.
     * 
     * @return a string representation of the index
     */
    public String getIndex() {
        return index.toString();
    }

    /**
     * Returns a sorted list of log files.
     * 
     * @param logDir Directory containing logs
     * @param logFilePrefix Prefix for log file names
     * @return Array of logfiles (zero-length if log is not initialized)
     */
    public static File[] listLogFiles(File logDir, String logFilePrefix) {
        ArrayList<File> logFiles = new ArrayList<File>();
        for (File f : logDir.listFiles()) {
            if (!f.isDirectory() && f.getName().startsWith(logFilePrefix)) {
                logFiles.add(f);
            }
        }
        File[] logFileArray = new File[logFiles.size()];
        return logFiles.toArray(logFileArray);
    }
}
