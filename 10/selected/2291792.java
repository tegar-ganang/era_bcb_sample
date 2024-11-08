package ch.ethz.dcg.spamato.activity;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import ch.ethz.dcg.plugin.*;
import ch.ethz.dcg.spamato.activity.cache.Cache;
import ch.ethz.dcg.spamato.activity.runtime.RuntimeMonitor;
import ch.ethz.dcg.spamato.base.common.filter.*;
import ch.ethz.dcg.spamato.db.DBMain;
import ch.ethz.dcg.spamato.factory.common.Mail;
import ch.ethz.dcg.spamato.logging.LogFacility;
import ch.ethz.dcg.thread.*;

/**
 * @author keno
 */
public class ActivityManager extends AbstractFilterProcessMonitor implements Disposable, PostChecker, PostReporter, PostRevoker {

    private static final String SQL_SELECT_RESULTS = "SELECT * FROM results WHERE msgID=?";

    private static final String SQL_SELECT_DATA_RESULTS = "SELECT * FROM data_results WHERE msgID=?";

    private static final String SQL_UPDATE_RESULTS = "UPDATE results SET spam=?,subject=?,sender=?,activity=?,activity_text=?,date=? WHERE msgID=?";

    private static final String SQL_UPDATE_FILTER_RESULTS = "UPDATE filter_results SET filter_key=?,spam=? WHERE msgID=?";

    private static final String SQL_UPDATE_DATA_RESULTS = "UPDATE data_results SET result=? WHERE msgID=?";

    private static final String SQL_INSERT_RESULTS = "INSERT INTO results VALUES(?,?,?,?,?,?,?)";

    private static final String SQL_INSERT_FILTER_RESULTS = "INSERT INTO filter_results VALUES(?,?,?)";

    private static final String SQL_INSERT_DATA_RESULTS = "INSERT INTO data_results VALUES(?,?)";

    private static final String SQL_DELETE = "DELETE FROM results WHERE msgID=?";

    private static final String SQL_DELETE_DATE = "DELETE FROM results WHERE date <= {fn TIMESTAMPADD(SQL_TSI_DAY, ?, CURRENT_TIMESTAMP)}";

    private static final long CLEANUP_START = 60 * 60 * 1000;

    private static final long CLEANUP_INTERVAL = 1 * 60 * 60 * 1000;

    private static final int CACHE_SIZE = 1000;

    protected enum ConfigTypes {

        VERSION("0.1"), DECAY("true"), DECAY_DAYS("7"), SHOW_IMAGES("false"), STORE_EMAILS("true");

        private String defaultValue;

        private ConfigTypes(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    private HashMap<String, Cache> fingerprintCache = new HashMap<String, Cache>();

    private Timer cleanupTimer;

    private BufferedTaskWorker updateWorker;

    private PluginContext context;

    private Configuration config;

    private DBMain dbMain;

    private Logger logger;

    private RuntimeMonitor processMonitor;

    public ActivityManager(PluginContainer container, PluginContext context, Configuration config, DBMain dbMain) {
        this.context = context;
        this.config = config;
        this.dbMain = dbMain;
        logger = LogFacility.getLogger(context);
        initConfig();
        initDB();
        init(container);
        this.updateWorker = new BufferedTaskWorker();
        updateWorker.start();
    }

    private void initConfig() {
        boolean forceNewDefaults = config.get(ConfigTypes.VERSION.name()) == null || !config.get(ConfigTypes.VERSION.name()).equals(ConfigTypes.VERSION.getDefaultValue());
        if (forceNewDefaults) config.clear();
        for (ConfigTypes type : ConfigTypes.values()) {
            config.setDefault(type.name(), type.getDefaultValue());
        }
        if (forceNewDefaults) config.save();
    }

    private void initDB() {
        try {
            if (!dbMain.tableExists(context, "results")) {
                createTable();
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        logger.info("Creating " + context.getKey() + ".results table");
        StringBuilder query = new StringBuilder();
        query.append("CREATE TABLE results (");
        query.append("msgID VARCHAR(255) NOT NULL,");
        query.append("spam INT NOT NULL,");
        query.append("subject VARCHAR(255) NOT NULL,");
        query.append("sender VARCHAR(255) NOT NULL,");
        query.append("activity INT NOT NULL,");
        query.append("activity_text VARCHAR(255) NOT NULL,");
        query.append("date TIMESTAMP,");
        query.append("PRIMARY KEY (msgID)");
        query.append(")");
        dbMain.executeUpdate(context, query.toString());
        dbMain.executeUpdate(context, "CREATE INDEX spam ON results (spam)");
        dbMain.executeUpdate(context, "CREATE INDEX type ON results (activity)");
        dbMain.executeUpdate(context, "CREATE INDEX date ON results (date DESC)");
        logger.info("Creating " + context.getKey() + ".filter_results table");
        query = new StringBuilder();
        query.append("CREATE TABLE filter_results (");
        query.append("msgID VARCHAR(255) NOT NULL,");
        query.append("filter_key VARCHAR(255) NOT NULL,");
        query.append("spam INT NOT NULL,");
        query.append("CONSTRAINT msgID FOREIGN KEY (msgID) REFERENCES results (msgID) ON DELETE CASCADE");
        query.append(")");
        dbMain.executeUpdate(context, query.toString());
        dbMain.executeUpdate(context, "CREATE INDEX filter_filter_key ON filter_results (filter_key)");
        dbMain.executeUpdate(context, "CREATE INDEX filter_spam ON filter_results (spam)");
        logger.info("Creating " + context.getKey() + ".data_results table");
        query = new StringBuilder();
        query.append("CREATE TABLE data_results (");
        query.append("msgID VARCHAR(255) NOT NULL,");
        query.append("result BLOB,");
        query.append("CONSTRAINT data_msgID FOREIGN KEY (msgID) REFERENCES results (msgID) ON DELETE CASCADE");
        query.append(")");
        dbMain.executeUpdate(context, query.toString());
    }

    protected void init(PluginContainer container) {
        try {
            File mailFolder = new File(context.getProfileDirectory(), "mails");
            for (File file : mailFolder.listFiles()) {
                file.delete();
            }
            mailFolder.delete();
            new File(context.getProfileDirectory(), "mails.index").delete();
            new File(context.getProfileDirectory(), "mails.index.backup").delete();
        } catch (Exception e) {
        }
        cleanupTimer = new Timer();
        cleanupTimer.schedule(new TimerTask() {

            public void run() {
                cleanup();
            }
        }, ActivityManager.CLEANUP_START, ActivityManager.CLEANUP_INTERVAL);
        processMonitor = new RuntimeMonitor(container);
    }

    protected void cleanup() {
        if (isDecay()) {
            Connection connection = null;
            try {
                connection = dbMain.getConnection(context);
                PreparedStatement statement = connection.prepareStatement(ActivityManager.SQL_DELETE_DATE);
                statement.setInt(1, -getDecayDays());
                statement.executeUpdate();
            } catch (SQLException sqle) {
                LogFacility.logStackTrace(logger, Level.WARNING, sqle);
            } finally {
                DBMain.closeConnection(connection);
            }
        }
    }

    public void dispose() {
        updateWorker.stopWorker();
        cleanupTimer.cancel();
    }

    public DBMain getDBMain() {
        return dbMain;
    }

    public PluginContext getContext() {
        return this.context;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isDecay() {
        return Boolean.valueOf(config.get(ConfigTypes.DECAY.name()));
    }

    public int getDecayDays() {
        return Integer.valueOf(config.get(ConfigTypes.DECAY_DAYS.name()));
    }

    public boolean isShowImages() {
        return Boolean.valueOf(config.get(ConfigTypes.SHOW_IMAGES.name()));
    }

    public boolean isStoreEmails() {
        return Boolean.valueOf(config.get(ConfigTypes.STORE_EMAILS.name()));
    }

    /**
	 * On receiving this event, we just store the results for the mail.
	 * We ignore any existing results for the same mail that might have been
	 * made previously. Thus, our policy is "overwrite" -> we forget any previous
	 * results for the message checked.
	 */
    public void handlePostCheck(FilterProcessResult filterProcessResult, Decision decision, boolean firedBefore, boolean finished) {
        if (!finished) return;
        updateWorker.add(new PostCheckTask(this, filterProcessResult));
    }

    public String getPostCheckerID() {
        return context.getKey();
    }

    public String getPostCheckerName() {
        return context.getName();
    }

    /**
	 * On receiving this event, we just store the results for the mail.
	 * If this mail was handled before, we just mark it as "reported", otherwise
	 * we add a new ActivityResult entry.
	 */
    public void handlePostReport(Mail mail) {
        updateWorker.add(new PostReportTask(this, mail));
    }

    /**
	 * On receiving this event, we just store the results for the mail.
	 * If this mail was handled before, we just mark it as "revoked", otherwise
	 * we add a new ActivityResult entry.
	 */
    public void handlePostRevoke(Mail mail) {
        updateWorker.add(new PostRevokeTask(this, mail));
    }

    public ActivityResult getActivityResult(String msgID) {
        ActivityResult result = null;
        try {
            Connection connection = dbMain.getConnection(context);
            PreparedStatement statement = connection.prepareStatement(ActivityManager.SQL_SELECT_DATA_RESULTS);
            result = getActivityResultDB(statement, msgID);
            connection.close();
        } catch (SQLException sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        }
        return result;
    }

    private ActivityResult getActivityResultDB(PreparedStatement statement, String msgID) throws SQLException {
        statement.setString(1, msgID);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) return DBActivityResult.get(resultSet);
        return null;
    }

    public SimpleActivityResult getSimpleActivityResult(String msgID) {
        SimpleActivityResult result = null;
        try {
            Connection connection = dbMain.getConnection(context);
            PreparedStatement statement = connection.prepareStatement(ActivityManager.SQL_SELECT_RESULTS);
            result = getSimpleActivityResultDB(statement, msgID);
            connection.close();
        } catch (SQLException sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        }
        return result;
    }

    private SimpleActivityResult getSimpleActivityResultDB(PreparedStatement statement, String msgID) throws SQLException {
        statement.setString(1, msgID);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) return DBSimpleActivityResult.get(resultSet);
        return null;
    }

    /**
	 * Updates an existing ActivityResult or inserts a new one. We have to update
	 * all three tables, results, filter_results, and data_results (depending on
	 * the activity).
	 * 
	 * This is called from the PostChecker method.
	 */
    protected synchronized void updateActivityResult(ActivityResult result) {
        Connection connection = null;
        try {
            connection = dbMain.getConnection(context);
            PreparedStatement selectStatement = connection.prepareStatement(ActivityManager.SQL_SELECT_RESULTS);
            SimpleActivityResult simpleResult = getSimpleActivityResultDB(selectStatement, result.getMailID());
            if (simpleResult != null) {
                if (EnumSet.of(SimpleActivityResult.Activity.CHECK, SimpleActivityResult.Activity.PRECHECK).contains(result.getLastActivity())) {
                    PreparedStatement deleteResultsStatement = connection.prepareStatement(ActivityManager.SQL_DELETE);
                    deleteResultDB(deleteResultsStatement, simpleResult.getMailID());
                    insertResultDB(connection, result);
                } else if (EnumSet.of(SimpleActivityResult.Activity.REVOKE, SimpleActivityResult.Activity.REPORT).contains(result.getLastActivity())) {
                    PreparedStatement selectDataStatement = connection.prepareStatement(ActivityManager.SQL_SELECT_DATA_RESULTS);
                    ActivityResult oldResult = getActivityResultDB(selectDataStatement, simpleResult.getMailID());
                    if (oldResult != null) {
                        if (result.getLastActivity().equals(SimpleActivityResult.Activity.REPORT)) {
                            oldResult.markReported();
                        } else {
                            oldResult.markRevoked();
                        }
                        PreparedStatement updateResultsStatement = connection.prepareStatement(ActivityManager.SQL_UPDATE_RESULTS);
                        PreparedStatement updateDataStatement = connection.prepareStatement(ActivityManager.SQL_UPDATE_DATA_RESULTS);
                        updateActivityDB(connection, updateResultsStatement, updateDataStatement, oldResult);
                    } else {
                        PreparedStatement updateStatement = connection.prepareStatement(ActivityManager.SQL_UPDATE_RESULTS);
                        updateResultDB(updateStatement, result);
                    }
                }
            } else {
                insertResultDB(connection, result);
            }
        } catch (SQLException sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        } finally {
            DBMain.closeConnection(connection);
        }
    }

    protected synchronized void updateActivityResultsByID(Vector<String> msgIDs, SimpleActivityResult.Activity newActivity) {
        try {
            Connection connection = dbMain.getConnection(context);
            PreparedStatement selectDataStatement = connection.prepareStatement(ActivityManager.SQL_SELECT_DATA_RESULTS);
            PreparedStatement updateResultsStatement = connection.prepareStatement(ActivityManager.SQL_UPDATE_RESULTS);
            PreparedStatement updateDataStatement = connection.prepareStatement(ActivityManager.SQL_UPDATE_DATA_RESULTS);
            for (String msgID : msgIDs) {
                ActivityResult result = getActivityResultDB(selectDataStatement, msgID);
                if (result != null) {
                    if (result.getLastActivity().equals(newActivity)) continue;
                    switch(newActivity) {
                        case REPORT:
                            result.markReported();
                            break;
                        case REVOKE:
                            result.markRevoked();
                            break;
                        default:
                            throw new RuntimeException("Wrong activity for ActivityManager.updateResults(): " + newActivity.name());
                    }
                    updateActivityDB(connection, updateResultsStatement, updateDataStatement, result);
                }
            }
            connection.close();
        } catch (SQLException sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        }
    }

    /**
	 * This method updates the activity level for the given result. As we store
	 * the activity in the results as well as the data_results table, both have
	 * to be updated.
	 */
    private synchronized boolean updateActivityDB(Connection connection, PreparedStatement updateResultsStatement, PreparedStatement updateDataStatement, ActivityResult result) {
        boolean success = false;
        try {
            boolean innerSuccess = false;
            connection.setAutoCommit(false);
            innerSuccess = updateResultDB(updateResultsStatement, result);
            innerSuccess &= updateDataDB(updateDataStatement, result);
            connection.commit();
            connection.setAutoCommit(true);
            success = innerSuccess;
        } catch (Exception e) {
            LogFacility.logStackTrace(logger, Level.WARNING, e);
            try {
                connection.rollback();
            } catch (SQLException sqle) {
            }
        }
        return success;
    }

    private synchronized boolean updateResultDB(PreparedStatement statement, ActivityResult result) {
        boolean success = false;
        try {
            statement.setBoolean(1, result.isSpam());
            statement.setString(2, result.getSubject());
            statement.setString(3, result.getSender());
            statement.setInt(4, result.getLastActivity().getValue());
            statement.setString(5, result.getActivityText());
            statement.setTimestamp(6, new java.sql.Timestamp(result.getTimestamp()));
            statement.setString(7, result.getMailID());
            success = 1 == statement.executeUpdate();
        } catch (Exception sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        }
        return success;
    }

    private synchronized boolean updateDataDB(PreparedStatement statement, ActivityResult result) {
        boolean success = false;
        try {
            DBMain.setObject(statement, 1, result);
            statement.setString(2, result.getMailID());
            success = 1 == statement.executeUpdate();
        } catch (Exception sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        }
        return success;
    }

    private synchronized boolean insertResultDB(Connection connection, ActivityResult result) throws SQLException {
        PreparedStatement insertResultsStatement = connection.prepareStatement(ActivityManager.SQL_INSERT_RESULTS);
        PreparedStatement insertFiltersStatement = connection.prepareStatement(ActivityManager.SQL_INSERT_FILTER_RESULTS);
        PreparedStatement insertDataStatement = connection.prepareStatement(ActivityManager.SQL_INSERT_DATA_RESULTS);
        return insertResultDB(connection, insertResultsStatement, insertFiltersStatement, insertDataStatement, result);
    }

    private synchronized boolean insertResultDB(Connection connection, PreparedStatement insertResultsStatement, PreparedStatement insertFiltersStatement, PreparedStatement insertDataStatement, ActivityResult result) {
        boolean success = false;
        try {
            boolean innerSuccess = false;
            connection.setAutoCommit(false);
            insertResultsStatement.setString(1, result.getMailID());
            insertResultsStatement.setBoolean(2, result.isSpam());
            insertResultsStatement.setString(3, result.getSubject());
            insertResultsStatement.setString(4, Mail.extractEMailAddress(result.getSender(), Mail.SIMPLE_MAIL_EXTRACT_PATTERN));
            insertResultsStatement.setInt(5, result.getLastActivity().getValue());
            insertResultsStatement.setString(6, result.getActivityText());
            insertResultsStatement.setTimestamp(7, new java.sql.Timestamp(result.getTimestamp()));
            innerSuccess = 1 == insertResultsStatement.executeUpdate();
            if (result.hasAnyFilterResults()) {
                for (FilterResult filterResult : result.getFilterResults()) {
                    insertFiltersStatement.setString(1, result.getMailID());
                    insertFiltersStatement.setString(2, filterResult.getID());
                    insertFiltersStatement.setBoolean(3, filterResult.isSpam());
                    innerSuccess &= 1 == insertFiltersStatement.executeUpdate();
                }
            }
            insertDataStatement.setString(1, result.getMailID());
            DBMain.setObject(insertDataStatement, 2, result);
            innerSuccess &= 1 == insertDataStatement.executeUpdate();
            connection.commit();
            connection.setAutoCommit(true);
            success = innerSuccess;
        } catch (Exception e) {
            LogFacility.logStackTrace(logger, Level.WARNING, e);
            try {
                connection.rollback();
            } catch (Exception e2) {
            }
        }
        return success;
    }

    private void deleteResult(ActivityResult result) {
        deleteResult(result.getMailID());
    }

    private void deleteResult(String msgID) {
        deleteResultDB(msgID);
    }

    protected void deleteResultsByID(Vector<String> msgIDs) {
        Connection connection = null;
        try {
            connection = dbMain.getConnection(context);
            PreparedStatement deleteStatement = connection.prepareStatement(ActivityManager.SQL_DELETE);
            for (String msgID : msgIDs) {
                deleteResultDB(deleteStatement, msgID);
            }
            connection.close();
        } catch (SQLException sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        } finally {
            DBMain.closeConnection(connection);
        }
    }

    private synchronized void deleteResultDB(String msgID) {
        Connection connection = null;
        try {
            connection = dbMain.getConnection(context);
            PreparedStatement statement = connection.prepareStatement(ActivityManager.SQL_DELETE);
            deleteResultDB(statement, msgID);
        } catch (SQLException sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        } finally {
            DBMain.closeConnection(connection);
        }
    }

    private synchronized boolean deleteResultDB(PreparedStatement statement, String msgID) {
        boolean success = false;
        try {
            statement.setString(1, msgID);
            success = 1 == statement.executeUpdate();
        } catch (SQLException sqle) {
            LogFacility.logStackTrace(logger, Level.WARNING, sqle);
        }
        return success;
    }

    protected void updateFingerprintCache(ActivityResult result, double value) {
        if (result.getFilterResults() == null) return;
        synchronized (fingerprintCache) {
            for (FilterResult filterResult : result.getFilterResults()) {
                if ("".equals(filterResult.getFingerprints().trim())) continue;
                String[] fingerprints = filterResult.getFingerprints().split(",");
                for (String fingerprint : fingerprints) {
                    if ("".equals(fingerprint.trim())) continue;
                    String key = fingerprint.split("=")[0];
                    double cachedResult = getFromFingerprintCache(filterResult.getID(), key, Long.MAX_VALUE);
                    if (cachedResult == value) continue;
                    removeFromFingerprintCache(filterResult.getID(), key);
                }
            }
        }
    }

    protected void put2FingerprintCache(String filterID, String fingerprints) {
        Cache cache = fingerprintCache.get(filterID);
        if (cache == null) {
            cache = new Cache(ActivityManager.CACHE_SIZE);
            fingerprintCache.put(filterID, cache);
        }
        cache.put(fingerprints);
    }

    public double getFromFingerprintCache(String filterID, String fingerprint, long maxAge) {
        Cache cache = fingerprintCache.get(filterID);
        if (cache != null) return cache.getResult(fingerprint, maxAge);
        return FilterResult.NO_RESULT;
    }

    protected void removeFromFingerprintCache(String filterID, String fingerprint) {
        Cache cache = fingerprintCache.get(filterID);
        if (cache != null) cache.remove(fingerprint);
    }

    protected HashMap<String, Cache> getFingerprintCache() {
        return fingerprintCache;
    }

    public RuntimeMonitor getProcessMonitor() {
        return processMonitor;
    }

    public void beforePreChecker(PreChecker preChecker, Mail mail, boolean firstCheck) {
        processMonitor.beforePreChecker(preChecker, mail, firstCheck);
    }

    public void afterPreChecker(PreChecker preChecker, PreCheckResult preCheckResult, Mail mail, boolean firstCheck) {
        processMonitor.afterPreChecker(preChecker, preCheckResult, mail, firstCheck);
    }

    public void beforeFilter(SpamFilter filter, FilterProcessResult filterProcessResult, CommitFuture commitFuture) {
        processMonitor.beforeFilter(filter, filterProcessResult, commitFuture);
    }

    public void afterFilter(SpamFilter filter, FilterResult filterResult, FilterProcessResult filterProcessResult, CommitFuture commitFuture) {
        processMonitor.afterFilter(filter, filterResult, filterProcessResult, commitFuture);
    }

    public void beforePostChecker(PostChecker postChecker, FilterProcessResult result, Decision decision, boolean firedBefore, boolean finished) {
        processMonitor.beforePostChecker(postChecker, result, decision, firedBefore, finished);
    }

    public void afterPostChecker(PostChecker postChecker, FilterProcessResult result, Decision decision, boolean firedBefore, boolean finished) {
        processMonitor.afterPostChecker(postChecker, result, decision, firedBefore, finished);
    }
}

class PostCheckTask extends AbstractTask {

    ActivityManager activityManager;

    FilterProcessResult filterProcessResult;

    public PostCheckTask(ActivityManager activityManager, FilterProcessResult filterProcessResult) {
        this.activityManager = activityManager;
        this.filterProcessResult = filterProcessResult;
    }

    public void doTask() {
        ActivityResult result = new ActivityResult(filterProcessResult);
        if (!activityManager.isStoreEmails()) result.removeMail();
        activityManager.updateActivityResult(result);
        if (filterProcessResult.hasAnyFilterResults()) {
            synchronized (activityManager.getFingerprintCache()) {
                for (FilterResult filterResult : filterProcessResult.getAllFilterResults()) {
                    activityManager.put2FingerprintCache(filterResult.getID(), filterResult.getFingerprints());
                }
            }
        }
    }
}

class PostReportTask extends AbstractTask {

    ActivityManager activityManager;

    Mail mail;

    public PostReportTask(ActivityManager activityManager, Mail mail) {
        this.activityManager = activityManager;
        this.mail = mail;
    }

    public void doTask() {
        ActivityResult result = new ActivityResult(mail, SimpleActivityResult.Activity.REPORT);
        if (!activityManager.isStoreEmails()) result.removeMail();
        activityManager.updateActivityResult(result);
        activityManager.updateFingerprintCache(result, 1.0);
    }
}

class PostRevokeTask extends AbstractTask {

    ActivityManager activityManager;

    Mail mail;

    public PostRevokeTask(ActivityManager activityManager, Mail mail) {
        this.activityManager = activityManager;
        this.mail = mail;
    }

    public void doTask() {
        ActivityResult result = new ActivityResult(mail, SimpleActivityResult.Activity.REVOKE);
        if (!activityManager.isStoreEmails()) result.removeMail();
        activityManager.updateActivityResult(result);
        activityManager.updateFingerprintCache(result, 0.0);
    }
}
