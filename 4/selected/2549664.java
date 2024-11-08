package org.exist.storage.journal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import org.exist.storage.DBBroker;

/**
 * Read log entries from the journal file. This class is used during recovery to scan the
 * last journal file. It uses a memory-mapped byte buffer on the file.
 * Journal entries can be read forward (during redo) or backward (during undo). 
 * 
 * @author wolf
 *
 */
public class JournalReader {

    private static final Logger LOG = Logger.getLogger(JournalReader.class);

    private FileChannel fc;

    private ByteBuffer header = ByteBuffer.allocateDirect(Journal.LOG_ENTRY_HEADER_LEN);

    private ByteBuffer payload = ByteBuffer.allocateDirect(8192);

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
    public JournalReader(DBBroker broker, File file, int fileNumber) throws LogException {
        this.broker = broker;
        this.fileNumber = fileNumber;
        try {
            FileInputStream is = new FileInputStream(file);
            fc = is.getChannel();
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
        try {
            if (fc.position() + Journal.LOG_ENTRY_BASE_LEN > fc.size()) return null;
            return readEntry();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the previous entry found by scanning backwards from the current position.
     * 
     * @return the previous entry
     * @throws LogException if an entry could not be read due to an inconsistency on disk.
     * @throws LogException 
     */
    public Loggable previousEntry() throws LogException {
        try {
            if (fc.position() == 0) return null;
            fc.position(fc.position() - 2);
            header.clear().limit(2);
            int bytes = fc.read(header);
            if (bytes < 2) throw new LogException("Incomplete log entry found!");
            header.flip();
            final short prevLink = header.getShort();
            final long prevStart = fc.position() - 2 - prevLink;
            fc.position(prevStart);
            final Loggable loggable = readEntry();
            fc.position(prevStart);
            return loggable;
        } catch (IOException e) {
            throw new LogException("Fatal error while reading journal entry: " + e.getMessage(), e);
        }
    }

    public Loggable lastEntry() throws LogException {
        try {
            fc.position(fc.size());
            return previousEntry();
        } catch (IOException e) {
            throw new LogException("Fatal error while reading journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Read a single entry.
     * 
     * @return The entry
     * @throws LogException
     */
    private Loggable readEntry() throws LogException {
        try {
            final long lsn = Lsn.create(fileNumber, (int) fc.position() + 1);
            header.clear();
            int bytes = fc.read(header);
            if (bytes <= 0) return null;
            if (bytes < Journal.LOG_ENTRY_HEADER_LEN) throw new LogException("Incomplete log entry header found: " + bytes);
            header.flip();
            final byte entryType = header.get();
            final long transactId = header.getLong();
            final short size = header.getShort();
            if (fc.position() + size > fc.size()) throw new LogException("Invalid length");
            final Loggable loggable = LogEntryTypes.create(entryType, broker, transactId);
            if (loggable == null) throw new LogException("Invalid log entry: " + entryType + "; size: " + size + "; id: " + transactId + "; at: " + Lsn.dump(lsn));
            loggable.setLsn(lsn);
            if (size + 2 > payload.capacity()) {
                payload = ByteBuffer.allocate(size + 2);
            }
            payload.clear().limit(size + 2);
            bytes = fc.read(payload);
            if (bytes < size + 2) throw new LogException("Incomplete log entry found!");
            payload.flip();
            loggable.read(payload);
            final short prevLink = payload.getShort();
            if (prevLink != size + Journal.LOG_ENTRY_HEADER_LEN) {
                LOG.warn("Bad pointer to previous: prevLink = " + prevLink + "; size = " + size + "; transactId = " + transactId);
                throw new LogException("Bad pointer to previous in entry: " + loggable.dump());
            }
            return loggable;
        } catch (Exception e) {
            throw new LogException(e.getMessage(), e);
        }
    }

    /**
     * Re-position the file position so it points to the start of the entry
     * with the given LSN.
     * 
     * @param lsn
     * @throws LogException 
     */
    public void position(long lsn) throws LogException {
        try {
            fc.position((int) Lsn.getOffset(lsn) - 1);
        } catch (IOException e) {
            throw new LogException("Fatal error while reading journal: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            fc.close();
        } catch (IOException e) {
        }
        fc = null;
    }
}
