package org.xactor.tm.recovery;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import javax.transaction.xa.Xid;
import org.xactor.tm.XidFactoryBase;

/**
 * This <code>RecoveryLogger</code> implementation 
 * uses <code>BatchWriter</code>s that batch log write requests 
 * in order to minimize disk forcing activity. A 
 * <code>BatchRecoveryLogger</code> instance is the "main object" of the
 * the recovery logger service, which is simply an MBean wrapper for that
 * instance.
 *   
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 37634 $
 */
public class BatchRecoveryLogger implements RecoveryLogger {

    /** Array of names of directories that contain recovery log files. */
    private String[] stringDirectoryList;

    /** Array of directories that contain recovery log files. */
    private File[] directoryList;

    /** The constant size of a recovery log file. */
    private int logFileSize;

    /** Array of recovery log files found at recovery time.  */
    private File[] existingRecoveryLogFiles;

    /** Array of log writers (one per directory that contains log files). */
    private BatchWriter[] writers;

    /** Number of log writers (number of directories that contain log files). */
    private int numWriters;

    /** Sequential number used to choose a log writer */
    private volatile int seqNo = 0;

    /** Name of the directory that contains heuristic status log files. */
    private String heuristicStatusLogDirectoryName;

    /** Directory that contain heuristic status log files. */
    private File heuristicStatusLogDirectory;

    /** Array of heuristic status log files found at recovery time.  */
    private File[] existingHeuristicStatusLogFiles;

    /** Heuristic status log writer. */
    private HeuristicStatusLog heuristicStatusLogger;

    /** The server's Xid factory. */
    private XidFactoryBase xidFactory;

    /** For asynchronously restarting <code>BatchLog</code> instances. */
    private LogRestarter logCleaner;

    String[] getDirectoryList() {
        return stringDirectoryList;
    }

    /**
    * Sets the names of directories that contain recovery log files.
    * <p />
    * This method used to be package protected. It was changed to public just
    * for testing purposes.
    * 
    * @param directoryList array of names of directories that contain recovery 
    *                      log files.
    * 
    */
    public void setDirectoryList(String[] directoryList) {
        this.stringDirectoryList = directoryList;
        File[] list = new File[directoryList.length];
        for (int i = 0; i < directoryList.length; i++) {
            list[i] = new File(directoryList[i]);
            list[i] = list[i].getAbsoluteFile();
            if (!list[i].exists()) {
                if (!list[i].mkdirs()) {
                    throw new RuntimeException("Unable to create recovery directory: " + directoryList[i]);
                }
            }
        }
        this.directoryList = list;
    }

    /**
    * Gets the constant size of a log file.
    * 
    * @return the constans size of a log file, in bytes.
    */
    int getLogFileSize() {
        return logFileSize;
    }

    /**
    * Sets the constant size of a log file.
    * <p />
    * This method used to be package protected. It was changed to public just
    * for testing purposes.
    * 
    * @param logFileSize the constant size of a log file, in bytes.
    */
    public void setLogFileSize(int logFileSize) {
        this.logFileSize = logFileSize;
    }

    /**
    * Gets the name of the directory that contains heuristic status log files.
    *
    * @return the name of the directory that contains heuristic status log 
    *         files.
    */
    String getHeuristicStatusLogDirectory() {
        return heuristicStatusLogDirectoryName;
    }

    /**
    * Sets the name of the directory that contains heuristic status log files.
    * <p />
    * This method used to be package protected. It was changed to public just
    * for testing purposes.
    * 
    * @param heuristicStatusLogDirectoryName the name of the directory that 
    *              contains heuristic status log files.
    */
    public void setHeuristicStatusLogDirectory(String heuristicStatusLogDirectoryName) {
        this.heuristicStatusLogDirectoryName = heuristicStatusLogDirectoryName;
        heuristicStatusLogDirectory = new File(heuristicStatusLogDirectoryName);
        heuristicStatusLogDirectory = heuristicStatusLogDirectory.getAbsoluteFile();
        if (!heuristicStatusLogDirectory.exists()) {
            if (!heuristicStatusLogDirectory.mkdirs()) {
                throw new RuntimeException("Unable to create heuristic status " + "log directory: " + heuristicStatusLogDirectoryName);
            }
        }
    }

    /**
    * Gets the Xid factory.
    * 
    * @return the Xid factory.
    */
    XidFactoryBase getXidFactory() {
        return xidFactory;
    }

    /**
    * Sets the Xid factory
    * <p />
    * This method used to be package protected. It was changed to public just
    * for testing purposes.
    * 
    * @param xidFactory the Xid factory.
    */
    public void setXidFactory(XidFactoryBase xidFactory) {
        this.xidFactory = xidFactory;
    }

    /**
    * Starts this <code>BatchRecoveryLoggger</code>.
    * <p />
    * This method used to be package protected. It was changed to public just
    * for testing purposes.
    */
    public void start() throws Exception {
        ArrayList list = new ArrayList();
        for (int i = 0; i < directoryList.length; i++) {
            File dir = directoryList[i];
            File[] files = dir.listFiles();
            for (int j = 0; j < files.length; j++) {
                list.add(files[j]);
            }
        }
        existingRecoveryLogFiles = (File[]) list.toArray(new File[list.size()]);
        logCleaner = new LogRestarter();
        new Thread(logCleaner, "Log file cleaner").start();
        writers = new BatchWriter[directoryList.length];
        String branchQualifier = xidFactory.getBranchQualifier();
        for (int i = 0; i < directoryList.length; i++) {
            writers[i] = new BatchWriter(branchQualifier, logFileSize / 128, directoryList[i], logFileSize, logCleaner);
            new Thread(writers[i], "Batch Recovery Log " + i).start();
        }
        numWriters = writers.length;
        existingHeuristicStatusLogFiles = heuristicStatusLogDirectory.listFiles();
        heuristicStatusLogger = new HeuristicStatusLog(heuristicStatusLogDirectory);
    }

    /**
    * Stops this <code>BatchRecoveryLoggger</code>.
    * <p />
    * This method used to be package protected. It was changed to public just
    * for testing purposes.
    */
    public void stop() throws Exception {
        for (int i = 0; i < writers.length; i++) {
            writers[i].stop();
        }
        logCleaner.stop();
        heuristicStatusLogger.close();
    }

    /**
   * @see org.xactor.tm.recovery.RecoveryLogger#saveCommitDecision(
   *            long, java.lang.String[])
   */
    public TxCompletionHandler saveCommitDecision(long localTransactionId, short[] resTrmiMechIds, String[] resources) {
        ByteBuffer buffer;
        if (resources == null || resources.length == 0) {
            buffer = LogRecord.createTxCommittedRecord(localTransactionId);
            return writers[++seqNo % numWriters].addBatch(buffer, false);
        } else {
            buffer = LogRecord.createTxCommittedRecord(localTransactionId, resTrmiMechIds, resources);
            return writers[++seqNo % numWriters].addBatch(buffer, true);
        }
    }

    /**
    * @see org.xactor.tm.recovery.RecoveryLogger#savePrepareDecision(
    *            long, int, byte[], java.lang.String, java.lang.String[])
    */
    public TxCompletionHandler savePrepareDecision(long localTransactionId, int inboundFormatId, byte[] globalTransactionId, short recCoorTrmiMechId, String recoveryCoordinator, short[] resTrmiMechIds, String[] resources) {
        ByteBuffer buffer = LogRecord.createTxPreparedRecord(localTransactionId, inboundFormatId, globalTransactionId, recCoorTrmiMechId, recoveryCoordinator, resTrmiMechIds, resources);
        return writers[++seqNo % numWriters].addBatch(buffer, true);
    }

    /**
    * @see org.xactor.tm.recovery.RecoveryLogger#savePrepareDecision(
    *            long, javax.transaction.xa.Xid, java.lang.String[])
    */
    public TxCompletionHandler savePrepareDecision(long localTransactionId, Xid inboundXid, short[] resTrmiMechIds, String[] resources) {
        ByteBuffer buffer = LogRecord.createJcaTxPreparedRecord(localTransactionId, inboundXid, resTrmiMechIds, resources);
        return writers[++seqNo % numWriters].addBatch(buffer, true);
    }

    /**
    * @see org.xactor.tm.recovery.RecoveryLogger#saveHeuristicStatus(
    *            long, boolean, int, byte[], byte[], int, int, boolean int[], 
    *            org.xactor.tm.recovery.HeuristicStatus[])
    */
    public void saveHeuristicStatus(long localTransactionId, boolean foreignTx, int formatId, byte[] globalTransactionId, byte[] inboundBranchQualifier, int transactionStatus, int heurStatusCode, boolean locallyDetectedHeuristicHazard, int[] xaResourceHeuristics, HeuristicStatus[] remoteResourceHeuristics) {
        ByteBuffer buffer = LogRecord.createHeurStatusRecord(localTransactionId, foreignTx, formatId, globalTransactionId, inboundBranchQualifier, transactionStatus, heurStatusCode, locallyDetectedHeuristicHazard, xaResourceHeuristics, remoteResourceHeuristics);
        heuristicStatusLogger.write(buffer);
    }

    /**
    * @see org.xactor.tm.recovery.RecoveryLogger#clearHeuristicStatus(long)
    */
    public void clearHeuristicStatus(long localTransactionId) {
        ByteBuffer buffer = LogRecord.createHeurForgottenRecord(localTransactionId);
        heuristicStatusLogger.write(buffer);
    }

    /**
    * @see org.xactor.tm.recovery.RecoveryLogger#getRecoveryLogs()
    */
    public RecoveryLogReader[] getRecoveryLogs() {
        if (existingRecoveryLogFiles == null || existingRecoveryLogFiles.length == 0) return null;
        RecoveryLogReader[] readers = new RecoveryLogReader[existingRecoveryLogFiles.length];
        for (int i = 0; i < readers.length; i++) {
            readers[i] = new BatchRecoveryLogReader(existingRecoveryLogFiles[i], xidFactory);
        }
        return readers;
    }

    /**
    * @see org.xactor.tm.recovery.RecoveryLogger#getHeuristicStatusLogs()
    */
    public HeuristicStatusLogReader[] getHeuristicStatusLogs() {
        if (existingHeuristicStatusLogFiles == null || existingHeuristicStatusLogFiles.length == 0) return null;
        HeuristicStatusLogReader[] readers = new HeuristicStatusLogReader[existingHeuristicStatusLogFiles.length];
        for (int i = 0; i < readers.length; i++) {
            readers[i] = new SimpleHeuristicStatusLogReader(existingHeuristicStatusLogFiles[i]);
        }
        return readers;
    }
}
