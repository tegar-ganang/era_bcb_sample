package org.xactor.tm.recovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

/**
 * Simple implementation of <code>HeuristicStatusLogReader</code> used at 
 * recovery time. The <code>BatchRecoveryLogger</code>'s implementation of 
 * method <code>getHeuristicStatusLogs()</code> instantiates 
 * <code>SimpleHeuristicStatusLogReader</code>s for the existing heuristic
 * status log files. It returns an array containing those readers, which the 
 * recovery manager uses to get information on heuristically completed
 * transactions.
 * 
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 37634 $
 */
public class SimpleHeuristicStatusLogReader implements HeuristicStatusLogReader {

    /** The underlying heuristic status log file. */
    private File logFile;

    /**
    * Constructs a <code>SimpleHeuristicStatusLogReader</code>.
    * 
    * @param logFile the heuristic status log file to read.
    */
    public SimpleHeuristicStatusLogReader(File logFile) {
        this.logFile = logFile;
    }

    /**
    * Gets the name of the heuristic status log file.
    * 
    * @return the name of the heuristic status log file.
    */
    public String getLogFileName() {
        return logFile.toString();
    }

    /**
    * Recovers information on heuristically completed transactions from 
    * the heuristic status log file.
    *
    * @param heuristicallyCompletedTransactions a <code>Map</code> to which
    *              this method will one entry per heuristically completed
    *              transaction. The map keys are <code>Long</code> values 
    *              containing local transaction ids. The map values are
    *              <code>LogRecord.HeurData</code> objects with information
    *              on heuristically completed transactions.
    */
    public void recover(Map heuristicallyCompletedTransactions) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(logFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            if (fis.available() < LogRecord.FULL_HEADER_LEN) return;
            FileChannel channel = fis.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(LogRecord.FULL_HEADER_LEN);
            channel.read(buf);
            int len = LogRecord.getNextRecordLength(buf, 0);
            LogRecord.HeurData data = new LogRecord.HeurData();
            while (len > 0) {
                buf = ByteBuffer.allocate(len + LogRecord.FULL_HEADER_LEN);
                if (channel.read(buf) < len) break;
                buf.flip();
                LogRecord.getHeurData(buf, len, data);
                switch(data.recordType) {
                    case LogRecord.HEUR_STATUS:
                        heuristicallyCompletedTransactions.put(new Long(data.localTransactionId), data);
                        break;
                    case LogRecord.HEUR_FORGOTTEN:
                        heuristicallyCompletedTransactions.remove(new Long(data.localTransactionId));
                        break;
                    default:
                        break;
                }
                len = LogRecord.getNextRecordLength(buf, len);
            }
        } catch (IOException ignore) {
        }
        try {
            fis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * Removes the heuristic status log file.
    */
    public void finishRecovery() {
        logFile.delete();
    }
}
