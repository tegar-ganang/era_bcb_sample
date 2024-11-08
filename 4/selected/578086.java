package org.xactor.tm.recovery;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.jboss.logging.Logger;

/**
 * Simple class that appends <code>ByteBuffer</code>s (each of which contains
 * a heuristic status log record) to a log file. Information on heuristically 
 * completed transactions does not go to the recovery logs -- it goes to a 
 * separate log file. Since heuristic decisions are very rare, there is no need 
 * to batch heuristic log writes together. 
 * 
 * TODO: In this implementation the heuristic status log file grows whenever
 * a record is written to it. This is bad for robustness and should be replaced
 * by an implementation in which the log file has constant lenght (such as the 
 * recovery log implementation).  
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 37634 $
 */
public class HeuristicStatusLog {

    /** 
    * Class <code>Logger</code>, for trace messages.
    */
    private static Logger errorLog = Logger.getLogger(HeuristicStatusLog.class);

    /** 
    * True if trace messages should be logged.
    */
    private static boolean traceEnabled = errorLog.isTraceEnabled();

    /** 
    * The log file. 
    */
    private File logFile;

    /**
    *  A <code>RandomAccessFile</code> view of the log file.
    */
    RandomAccessFile os;

    /**
    * The <code>RandomAccessFile</code>'s underlying <code>FileChannel</code>.
    */
    private FileChannel channel;

    /**
    * Constructs a new <code>HeuristicStatusLog</code>.
    * 
    * @param dir      the directory in which the log file will be created.
    * @throws IOException
    */
    HeuristicStatusLog(File dir) throws IOException {
        logFile = File.createTempFile("HEURISTIC_STATUS_LOG", ".log", dir);
        os = new RandomAccessFile(logFile, "rw");
        channel = os.getChannel();
        channel.force(true);
    }

    /**
    * Writes one record at the current position of this 
    * <code>HeuristicStatusLog</code>.
    * 
    * @param record  a buffer with the record to be written
    */
    void write(ByteBuffer record) {
        if (traceEnabled) {
            errorLog.trace("Heuristic status log record:" + HexDump.fromBuffer(record.array()));
            errorLog.trace(LogRecord.toString(record));
        }
        try {
            channel.write(record);
            channel.force(true);
        } catch (IOException e) {
            errorLog.error("Error writing heuristic status log " + logFile.getName(), e);
        }
    }

    /**
    * Closes this <code>HeuristicStatusLog</code>. 
    */
    void close() {
        try {
            os.close();
        } catch (IOException e) {
            errorLog.error("Error closing heuristic status log " + logFile.getName(), e);
        }
    }
}
