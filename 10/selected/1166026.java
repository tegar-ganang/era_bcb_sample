package au.gov.naa.digipres.rollingchecker;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import au.gov.naa.digipres.dpr.model.job.JobStatus;
import au.gov.naa.digipres.rollingchecker.model.DPRSettings;
import au.gov.naa.digipres.rollingchecker.model.GeneralSettings;
import au.gov.naa.digipres.rollingchecker.util.Checksum;

/**
 * The class that actually performs checksum checking for the Rolling Checksum Checker.
 * 
 * When started, the thread will iterate through all AIPs in the database and obtain a reference to the
 * physical file on the repository. It will compare the checksum of the physical file to the checksum 
 * stored in the database, and if they do not match it will mark that down as an error and send out
 * notification to any listeners. When all AIPs have been checked, an end-of-run event will be fired to
 * all listeners with a summary report of the run.
 * 
 * The checksum checker is also used as a statistics collector, for use with DPR reports. Information about the
 * number and volume of AIPs and their MIME types is gathered and stored in the database.
 * 
 * The checksum checker is configured to run with a certain frequency (eg once per day). At the start of each run
 * the checker calculates the time that the next run should start. At the end of each run, if the time of the next run
 * has not yet been reached the thread sleeps until it is time to start again. If the time has been reached then the checker
 * will start a new run immediately, and will now be checking checksums constantly.
 *  
 * @author Justin Waddell
 *
 */
public class CheckerThread extends Thread {

    public static final int RUNNING = 1;

    public static final int PAUSED = 2;

    public static final int SCHEDULED_PAUSE = 3;

    public static final int STOPPED = 4;

    public static final String PF_DATA_DIR_NAME = "pf_data";

    public static final String NORMALISED_DIR_NAME = "normalised";

    public static final String BINARIED_DIR_NAME = "binary";

    private static final int DEFAULT_FETCH_SIZE = 500;

    private static final int MAXIMUM_RESULTS_TO_KEEP = 15;

    public int runningState = STOPPED;

    private Calendar nextScheduledRun;

    private DPRSettings dprSettings;

    private GeneralSettings generalSettings;

    private Connection dbConnection;

    private List<CheckerListener> checkerListeners = new ArrayList<CheckerListener>();

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private CheckerResults checkerResults;

    private List<CheckerResults> previousResults = new ArrayList<CheckerResults>();

    private String currentAIPName = "";

    private DecimalFormat numberFormatter = new DecimalFormat("#,###");

    public CheckerThread(DPRSettings dprSettings, GeneralSettings generalSettings) throws SQLException {
        this.dprSettings = dprSettings;
        this.generalSettings = generalSettings;
        initDatabase(dprSettings);
    }

    private void initDatabase(DPRSettings dprInfo) throws SQLException {
        if (dbConnection != null) {
            dbConnection.close();
        }
        try {
            Class.forName(dprInfo.getDbDriverClassName());
        } catch (Exception e) {
            throw new SQLException("Failed to load driver for the DPR database");
        }
        dbConnection = DriverManager.getConnection(dprInfo.getDbURL(), dprInfo.getDbUsername(), dprInfo.getDbPassword());
        dbConnection.setAutoCommit(false);
    }

    public void addCheckerEventsListener(CheckerListener listener) {
        checkerListeners.add(listener);
    }

    @Override
    public void run() {
        nextScheduledRun = new GregorianCalendar();
        nextScheduledRun.add(generalSettings.getPeriodUnit(), generalSettings.getCheckerPeriod());
        setRunningState(RUNNING);
        while (getRunningState() == RUNNING) {
            boolean databaseConnected = false;
            while (!databaseConnected) {
                try {
                    initDatabase(dprSettings);
                    databaseConnected = true;
                } catch (SQLException sqlex) {
                    logger.log(Level.SEVERE, "Could not connect to database. Will try again after 30 seconds.", sqlex);
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                    }
                }
            }
            logger.finer("Started checking run at " + new Date());
            System.out.println("Started checking run at " + new Date());
            fireCheckerRunStartedEvent();
            try {
                checkerResults = new CheckerResults();
                checkerResults.setStartTime(new java.util.Date());
                Map<String, AIPStatistics> mimeTypeRegister = new TreeMap<String, AIPStatistics>();
                long tjAIPCount = 0, rjAIPCount = 0;
                StringBuilder totalTJAIPsQuery = new StringBuilder();
                totalTJAIPsQuery.append("select count(aip.aip_id) from archival_information_package aip, transfer_job tj ");
                totalTJAIPsQuery.append("where aip.transfer_job_id = tj.transfer_job_id and tj.status = '");
                totalTJAIPsQuery.append(JobStatus.DR_PROCESSING_COMPLETE.getDescription());
                totalTJAIPsQuery.append("' and aip.deleted = 'FALSE'");
                PreparedStatement select = dbConnection.prepareStatement(totalTJAIPsQuery.toString());
                select.setFetchSize(DEFAULT_FETCH_SIZE);
                ResultSet result = select.executeQuery();
                if (result.next()) {
                    tjAIPCount = result.getLong(1);
                }
                StringBuilder totalRJAIPsQuery = new StringBuilder();
                totalRJAIPsQuery.append("select count(aip.aip_id) from archival_information_package aip, reprocessing_job rj ");
                totalRJAIPsQuery.append("where aip.reprocessing_job_id = rj.id and rj.status = '");
                totalRJAIPsQuery.append(JobStatus.DR_REPROCESSING_COMPLETE.getDescription());
                totalRJAIPsQuery.append("' " + "and aip.deleted = 'FALSE'");
                select = dbConnection.prepareStatement(totalRJAIPsQuery.toString());
                select.setFetchSize(DEFAULT_FETCH_SIZE);
                result = select.executeQuery();
                if (result.next()) {
                    rjAIPCount = result.getLong(1);
                }
                checkerResults.setTotalAIPCount(tjAIPCount + rjAIPCount);
                System.out.println("TJ Count: " + tjAIPCount + ", RJ Count: " + rjAIPCount);
                StringBuilder tjQuery = new StringBuilder();
                tjQuery.append("select distinct aip.output_resource_name, aip.checksum, aip.checksum_algorithm, aip.type, tj.full_job_number, aip_content.mimetype ");
                tjQuery.append("from archival_information_package aip, transfer_job tj, aip_content ");
                tjQuery.append("where aip.transfer_job_id = tj.transfer_job_id and tj.status = '");
                tjQuery.append(JobStatus.DR_PROCESSING_COMPLETE.getDescription());
                tjQuery.append("' and aip.top_content_id = aip_content.aip_content_id ");
                tjQuery.append("and aip.deleted = 'FALSE'");
                select = dbConnection.prepareStatement(tjQuery.toString());
                select.setFetchSize(DEFAULT_FETCH_SIZE);
                result = select.executeQuery();
                AIPStatistics tjAIPStats = scanAIPs(result, checkerResults, mimeTypeRegister);
                StringBuilder rjQuery = new StringBuilder();
                rjQuery.append("select distinct aip.output_resource_name, aip.checksum, aip.checksum_algorithm, aip.type, rj.full_job_number, aip_content.mimetype ");
                rjQuery.append("from archival_information_package aip, reprocessing_job rj, aip_content ");
                rjQuery.append("where aip.reprocessing_job_id = rj.id and rj.status = '");
                rjQuery.append(JobStatus.DR_REPROCESSING_COMPLETE.getDescription());
                rjQuery.append("' and aip.top_content_id = aip_content.aip_content_id ");
                rjQuery.append("and aip.deleted = 'FALSE'");
                select = dbConnection.prepareStatement(rjQuery.toString());
                select.setFetchSize(DEFAULT_FETCH_SIZE);
                result = select.executeQuery();
                AIPStatistics rjAIPStats = scanAIPs(result, checkerResults, mimeTypeRegister);
                select.close();
                result.close();
                recheckErrors();
                if (getRunningState() == RUNNING) {
                    writeStatsToDatabase(tjAIPStats.aipCount, rjAIPStats.aipCount, tjAIPStats.aipVolume, rjAIPStats.aipVolume, tjAIPStats.binaryAIPCount, mimeTypeRegister);
                }
                long aipsChecked = tjAIPStats.aipCount + rjAIPStats.aipCount;
                checkerResults.setEndTime(new java.util.Date());
                fireCheckerRunFinishedEvent(checkerResults, aipsChecked);
                saveResults(checkerResults);
                logger.finer("Finished checksum checking run. Checked " + aipsChecked + " AIPs. Found " + checkerResults.getTotalExceptions() + " errors.");
                System.out.println("Finished checksum checking run. Checked " + aipsChecked + " AIPs. Found " + checkerResults.getTotalExceptions() + " errors.");
            } catch (SQLException e1) {
                logger.log(Level.SEVERE, "Database Exception", e1);
            } finally {
                currentAIPName = "";
            }
            if (getRunningState() == RUNNING) {
                setRunningState(SCHEDULED_PAUSE);
                logger.fine("Checker is paused. Will resume at " + nextScheduledRun.getTime());
                System.out.println("Checker is paused. Will resume at " + nextScheduledRun.getTime());
                while (getRunningState() == SCHEDULED_PAUSE || getRunningState() == PAUSED) {
                    if (getRunningState() == SCHEDULED_PAUSE) {
                        Calendar currentTime = new GregorianCalendar();
                        if (currentTime.before(nextScheduledRun)) {
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                                return;
                            }
                        } else {
                            nextScheduledRun = new GregorianCalendar();
                            nextScheduledRun.add(generalSettings.getPeriodUnit(), generalSettings.getCheckerPeriod());
                            setRunningState(RUNNING);
                        }
                    } else {
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            }
        }
        try {
            dbConnection.close();
        } catch (SQLException e) {
            logger.warning("Could not close database connection");
        }
    }

    /**
	 * Re-check stored errors. Do not re-check if we have more total exceptions than MAX_EXCEPTIONS_TO_STORE.
	 */
    private void recheckErrors() {
        if (checkerResults != null) {
            List<AIPExceptionItem> exceptionList = checkerResults.getExceptionList();
            if (exceptionList != null && checkerResults.getTotalExceptions() <= CheckerResults.MAX_EXCEPTIONS_TO_STORE) {
                for (int i = exceptionList.size() - 1; i >= 0; i--) {
                    AIPExceptionItem exceptionItem = exceptionList.get(i);
                    File aipFile = new File(exceptionItem.getFilename());
                    if (aipFile.exists() && aipFile.isFile()) {
                        try {
                            String generatedChecksum = Checksum.getChecksum(exceptionItem.getAlgorithm(), aipFile);
                            if (generatedChecksum.equals(exceptionItem.getStoredChecksum())) {
                                checkerResults.removeException(i);
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
    }

    /**
	 * Save the results of a checker run. Only a certain number of previous runs are kept, so the oldest will be discarded.
	 * 
	 * @param checkerResultsParam
	 */
    private void saveResults(CheckerResults checkerResultsParam) {
        if (previousResults.size() >= MAXIMUM_RESULTS_TO_KEEP) {
            previousResults.remove(previousResults.size() - 1);
        }
        previousResults.add(0, checkerResultsParam);
    }

    /**
	 * Store statistical information collected during the checksum checker run. This includes the number, volume
	 * and type of the AIPs, as well as the date of the run (so that quarterly and yearly reports can be produced).
	 * 
	 * @param transferJobAIPCount
	 * @param reprocessingJobAIPCount
	 * @param transferJobAIPVolume
	 * @param reprocessingJobAIPVolume
	 * @param overallBinaryAIPCount
	 * @param mimeTypeRegister
	 * @throws SQLException
	 */
    private void writeStatsToDatabase(long transferJobAIPCount, long reprocessingJobAIPCount, long transferJobAIPVolume, long reprocessingJobAIPVolume, long overallBinaryAIPCount, Map<String, AIPStatistics> mimeTypeRegister) throws SQLException {
        int nextAIPStatsID;
        long nextMimetypeStatsID;
        Statement select = dbConnection.createStatement();
        String aipStatsQuery = "select max(aip_statistics_id) from aip_statistics";
        ResultSet result = select.executeQuery(aipStatsQuery);
        if (result.next()) {
            nextAIPStatsID = result.getInt(1) + 1;
        } else {
            throw new SQLException("Problem getting maximum AIP Statistics ID");
        }
        String mimetypeStatsQuery = "select max(mimetype_aip_statistics_id) from mimetype_aip_statistics";
        result = select.executeQuery(mimetypeStatsQuery);
        if (result.next()) {
            nextMimetypeStatsID = result.getLong(1) + 1;
        } else {
            throw new SQLException("Problem getting maximum MIME type AIP Statistics ID");
        }
        String insertAIPStatsEntryQuery = "insert into aip_statistics " + "(aip_statistics_id, tj_aip_count, tj_aip_volume, rj_aip_count, rj_aip_volume, " + "collation_date, binary_aip_count) " + "values (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement insert = dbConnection.prepareStatement(insertAIPStatsEntryQuery);
        insert.setInt(1, nextAIPStatsID);
        insert.setLong(2, transferJobAIPCount);
        insert.setLong(3, transferJobAIPVolume);
        insert.setLong(4, reprocessingJobAIPCount);
        insert.setLong(5, reprocessingJobAIPVolume);
        insert.setDate(6, new java.sql.Date(System.currentTimeMillis()));
        insert.setLong(7, overallBinaryAIPCount);
        int rowsAdded = insert.executeUpdate();
        if (rowsAdded != 1) {
            dbConnection.rollback();
            throw new SQLException("Could not insert row into AIP statistics table");
        }
        String insertMimeTypeStatsQuery = "insert into mimetype_aip_statistics " + "(mimetype_aip_statistics_id, aip_statistics_id, mimetype_aip_count, mimetype_aip_volume, mimetype) " + "values (?, ?, ?, ?, ?)";
        insert = dbConnection.prepareStatement(insertMimeTypeStatsQuery);
        insert.setInt(2, nextAIPStatsID);
        for (String mimeType : mimeTypeRegister.keySet()) {
            AIPStatistics mimeTypeStats = mimeTypeRegister.get(mimeType);
            insert.setLong(1, nextMimetypeStatsID);
            insert.setLong(3, mimeTypeStats.aipCount);
            insert.setLong(4, mimeTypeStats.aipVolume);
            insert.setString(5, mimeType);
            nextMimetypeStatsID++;
            rowsAdded = insert.executeUpdate();
            if (rowsAdded != 1) {
                dbConnection.rollback();
                throw new SQLException("Could not insert row into MIME Type AIP statistics table");
            }
        }
        dbConnection.commit();
    }

    private void fireCheckerRunFinishedEvent(CheckerResults checkerResultsParam, long filesChecked) {
        for (CheckerListener listener : checkerListeners) {
            listener.onCheckFinished(checkerResultsParam, filesChecked);
        }
    }

    private void fireCheckerRunStartedEvent() {
        for (CheckerListener listener : checkerListeners) {
            listener.onCheckStarted();
        }
    }

    private void fireFileCheckErrorEvent(File file, Exception ex) {
        for (CheckerListener listener : checkerListeners) {
            listener.onFileCheckError(file, ex);
        }
    }

    /**
	 * Retrieve AIP information from the result set, and use it to build the path to the AIP file. Then generate the
	 * checksum for this file and compare it to the checksum stored in the database. If the checksums do not match, or
	 * another error occurs, add the AIP and its associated error to the exception register returned by this method.
	 * @param result
	 * @return exception register - a map of AIP Files to Exceptions
	 * @throws SQLException
	 */
    private AIPStatistics scanAIPs(ResultSet result, CheckerResults checkerResultsParam, Map<String, AIPStatistics> mimeTypeRegister) throws SQLException {
        AIPStatistics overallAIPStats = new AIPStatistics();
        while (result.next()) {
            if (getRunningState() == STOPPED) {
                break;
            } else if (getRunningState() == PAUSED) {
                while (getRunningState() == PAUSED) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException iex) {
                    }
                }
            }
            String outputFile = result.getString("output_resource_name");
            String storedChecksum = result.getString("checksum");
            String algorithm = result.getString("checksum_algorithm");
            String type = result.getString("type");
            String jobNumber = result.getString("full_job_number");
            String mimeType = result.getString("mimetype");
            currentAIPName = outputFile;
            boolean isBinary = false;
            String typeStr = NORMALISED_DIR_NAME;
            if (type != null && type.toLowerCase().startsWith("binary")) {
                typeStr = BINARIED_DIR_NAME;
                isBinary = true;
            }
            File aipFile = new File(dprSettings.getRepositoryPath() + File.separator + jobNumber.substring(0, 4) + File.separator + jobNumber.substring(5) + File.separator + PF_DATA_DIR_NAME + File.separator + typeStr + File.separator + outputFile);
            if (!aipFile.exists() || !aipFile.isFile()) {
                logger.finest("Possible AIP error found for " + aipFile + ". Waiting 10 seconds and then retrying.");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
                if (!aipFile.exists() || !aipFile.isFile()) {
                    handleAIPError(checkerResultsParam, aipFile, "AIP File does not exist", storedChecksum, algorithm);
                }
            } else {
                try {
                    String generatedChecksum = Checksum.getChecksum(algorithm, aipFile);
                    if (!generatedChecksum.equals(storedChecksum)) {
                        logger.finest("Possible AIP error found for " + aipFile + ". Waiting 10 seconds and then retrying.");
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                        }
                        if (!generatedChecksum.equals(storedChecksum)) {
                            handleAIPError(checkerResultsParam, aipFile, "The stored checksum does not match the generated checksum for file", storedChecksum, algorithm);
                        } else {
                            logger.finest(aipFile + " passed");
                        }
                    } else {
                        logger.finest(aipFile + " passed");
                    }
                } catch (IOException e) {
                    checkerResultsParam.registerException(aipFile.getAbsolutePath(), e, storedChecksum, algorithm);
                    fireFileCheckErrorEvent(aipFile, e);
                }
                long aipFileSize = aipFile.length();
                if (!isBinary) {
                    AIPStatistics mimeTypeAIPStats;
                    if (mimeTypeRegister.containsKey(mimeType)) {
                        mimeTypeAIPStats = mimeTypeRegister.get(mimeType);
                    } else {
                        mimeTypeAIPStats = new AIPStatistics();
                        mimeTypeRegister.put(mimeType, mimeTypeAIPStats);
                    }
                    mimeTypeAIPStats.aipCount++;
                    mimeTypeAIPStats.aipVolume += aipFileSize;
                }
                overallAIPStats.aipVolume += aipFileSize;
            }
            overallAIPStats.aipCount++;
            if (isBinary) {
                overallAIPStats.binaryAIPCount++;
            }
            checkerResultsParam.setAipsChecked(checkerResultsParam.getAipsChecked() + 1);
        }
        return overallAIPStats;
    }

    /**
	 * @param checkerResultsParam
	 * @param aipFile
	 */
    private void handleAIPError(CheckerResults checkerResultsParam, File aipFile, String errorMessage, String storedChecksum, String algorithm) {
        logger.warning(errorMessage + " - " + aipFile);
        IOException iex = new IOException(errorMessage + " - " + aipFile);
        checkerResultsParam.registerException(aipFile.getAbsolutePath(), iex, storedChecksum, algorithm);
        fireFileCheckErrorEvent(aipFile, iex);
    }

    /**
	 * Return a String representation of the current status of the checksum checker.
	 * This includes the following information:
	 * 
	 * Running state (eg running, stopped, paused)
	 * Start Time
	 * Next Run Due
	 * Total AIP Count
	 * AIPs Checked
	 * Error Count
	 * Current AIP Name
	 * @return
	 */
    public String getStatus() {
        StringBuilder statusBuilder = new StringBuilder();
        String runningStateString = "";
        switch(getRunningState()) {
            case RUNNING:
                runningStateString = CheckerConstants.RUNNING_STATE_NAME;
                break;
            case STOPPED:
                runningStateString = CheckerConstants.STOPPED_STATE_NAME;
                break;
            case PAUSED:
                runningStateString = CheckerConstants.PAUSED_STATE_NAME;
                break;
            case SCHEDULED_PAUSE:
                runningStateString = CheckerConstants.SCHEDULED_PAUSE_STATE_NAME;
                break;
            default:
                runningStateString = CheckerConstants.UNKNOWN_STATE_NAME;
        }
        statusBuilder.append(CheckerConstants.STATUS_PROPERTY_NAME + CheckerConstants.PROPERTY_SEPARATOR);
        statusBuilder.append(runningStateString);
        statusBuilder.append("\r\n");
        if (getRunningState() == RUNNING) {
            statusBuilder.append(CheckerConstants.START_TIME_PROPERTY_NAME + CheckerConstants.PROPERTY_SEPARATOR);
            statusBuilder.append(checkerResults.getStartTime());
            statusBuilder.append("\r\n");
        }
        statusBuilder.append(CheckerConstants.TOTAL_AIP_COUNT_PROPERTY_NAME + CheckerConstants.PROPERTY_SEPARATOR);
        statusBuilder.append(numberFormatter.format(checkerResults.getTotalAIPCount()));
        statusBuilder.append("\r\n");
        statusBuilder.append(CheckerConstants.AIPS_CHECKED_PROPERTY_NAME + CheckerConstants.PROPERTY_SEPARATOR);
        statusBuilder.append(numberFormatter.format(checkerResults.getAipsChecked()));
        statusBuilder.append("\r\n");
        statusBuilder.append(CheckerConstants.ERROR_COUNT_PROPERTY_NAME + CheckerConstants.PROPERTY_SEPARATOR);
        statusBuilder.append(numberFormatter.format(checkerResults.getTotalExceptions()));
        statusBuilder.append("\r\n");
        statusBuilder.append(CheckerConstants.CURRENT_AIP_PROPERTY_NAME + CheckerConstants.PROPERTY_SEPARATOR);
        statusBuilder.append(currentAIPName);
        String nextScheduledRunStr = nextScheduledRun == null ? "" : nextScheduledRun.getTime().toString();
        if (getRunningState() == RUNNING || getRunningState() == SCHEDULED_PAUSE) {
            statusBuilder.append("\r\n");
            statusBuilder.append(CheckerConstants.NEXT_RUN_PROPERTY_NAME + CheckerConstants.PROPERTY_SEPARATOR);
            statusBuilder.append(nextScheduledRunStr);
        }
        return statusBuilder.toString();
    }

    /**
	 * @return the runningState
	 */
    public synchronized int getRunningState() {
        return runningState;
    }

    /**
	 * @param runningState the runningState to set
	 */
    public synchronized void setRunningState(int runningState) {
        this.runningState = runningState;
    }

    /**
	 * @return the previousResults
	 */
    public List<CheckerResults> getPreviousResults() {
        return previousResults;
    }

    /**
	 * Class to store AIP Statistics information for a checksum checker run
	 * @author Justin Waddell
	 *
	 */
    private class AIPStatistics {

        public long aipCount;

        public long aipVolume;

        public long binaryAIPCount;

        public AIPStatistics() {
            aipCount = 0;
            aipVolume = 0;
            binaryAIPCount = 0;
        }
    }
}
