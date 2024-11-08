package org.exist.storage.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import org.exist.storage.DBBroker;

/**
 * Read log entries from a log file. This class is used during recovery to scan the
 * last log file. It uses a memory-mapped byte buffer on the file.
 * Log entries can be read forward (during redo) or backward (during undo). 
 * 
 * @author wolf
 *
 */
public class LogReader {

    private static final Logger LOG = Logger.getLogger(LogReader.class);

    private MappedByteBuffer mapped;

    private FileChannel fc;

    private int fileNumber;

    private DBBroker broker;

    /**
     * Opens the specified file for reading.
     * 
     * @param broker
     * @param file
     * @param fileNumber
     * @throws LogException
     */
    public LogReader(DBBroker broker, File file, int fileNumber) throws LogException {
        this.broker = broker;
        this.fileNumber = fileNumber;
        try {
            FileInputStream is = new FileInputStream(file);
            fc = is.getChannel();
            mapped = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        } catch (IOException e) {
            throw new LogException("Failed to read log file " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Returns the next entry found from the current position.
     * 
     * @return the next entry
     * @throws LogException if an entry could not be read due to an inconsistency on disk.
     */
    public Loggable nextEntry() throws LogException {
        if (mapped.position() + LogManager.LOG_ENTRY_BASE_LEN > mapped.capacity()) return null;
        return readEntry();
    }

    /**
     * Returns the previous entry found by scanning backwards from the current position.
     * 
     * @return the previous entry
     * @throws LogException if an entry could not be read due to an inconsistency on disk.
     */
    public Loggable previousEntry() throws LogException {
        if (mapped.position() == 0) return null;
        mapped.position(mapped.position() - 2);
        final short prevLink = mapped.getShort();
        final int prevStart = mapped.position() - 2 - prevLink;
        mapped.position(prevStart);
        final Loggable loggable = readEntry();
        mapped.position(prevStart);
        return loggable;
    }

    /**
     * Read a single entry.
     * 
     * @return
     * @throws LogException
     */
    private Loggable readEntry() throws LogException {
        final long lsn = Lsn.create(fileNumber, mapped.position() + 1);
        final byte entryType = mapped.get();
        final long transactId = mapped.getLong();
        final short size = mapped.getShort();
        if (mapped.position() + size > mapped.capacity()) throw new LogException("Invalid length");
        final Loggable loggable = LogEntryTypes.create(entryType, broker, transactId);
        if (loggable == null) throw new LogException("Invalid log entry: " + entryType + "; size: " + size + "; id: " + transactId + "; at: " + Lsn.dump(lsn));
        loggable.setLsn(lsn);
        loggable.read(mapped);
        final short prevLink = mapped.getShort();
        if (prevLink != size + LogManager.LOG_ENTRY_HEADER_LEN) {
            LOG.warn("Bad pointer to previous: prevLink = " + prevLink + "; size = " + size + "; transactId = " + transactId);
            throw new LogException("Bad pointer to previous in entry: " + loggable.dump());
        }
        return loggable;
    }

    /**
     * Re-position the file position so it points to the start of the entry
     * with the given LSN.
     * 
     * @param lsn
     */
    public void position(long lsn) {
        mapped.position((int) Lsn.getOffset(lsn) - 1);
    }

    public void close() {
        try {
            fc.close();
        } catch (IOException e) {
        }
        mapped = null;
    }
}
