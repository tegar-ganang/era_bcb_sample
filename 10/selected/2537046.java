package com.google.code.sagetvaddons.sre.server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * The SRE data store; stores details about overrides for scheduled recordings
 * @version $Id: DataStore.java 758 2010-01-15 04:11:28Z derek@battams.ca $
 */
class DataStore {

    private static final Logger LOG = Logger.getLogger(DataStore.class);

    private static final int SCHEMA_VERSION = 6;

    private static final String SQL_ERROR = "SQL error";

    public static final String WORKER_STATE = "OnOff";

    public static final boolean DEFAULT_WORKER_STATE = true;

    public static final String LAST_STATUS_PURGE = "LastStatusPurge";

    public static final String DEFAULT_LAST_STATUS_PURGE = "0";

    public static final String LAST_SCAN = "LastScan";

    public static final String DEFAULT_LAST_SCAN = "0";

    public static final String SLEEP_TIME = "SleepTime";

    public static final String DEFAULT_SLEEP_TIME = "300000";

    public static final String TEST_MODE = "TestMode";

    public static final String DEFAULT_TEST_MODE = "false";

    public static final String TEST_MODE_DONE = "TestModeDone";

    public static final String DEFAULT_TEST_MODE_DONE = "false";

    public static final String PADDING = "Padding";

    public static final String DEFAULT_PADDING = "0";

    public static final String UNMARK_FAVS = "UnmarkFavs";

    public static final String DEFAULT_UNMARK_FAVS = "true";

    public static final String ENABLE_SAFETY_NET = "UseSafetyNet";

    public static final String DEFAULT_ENABLE_SAFETY_NET = "true";

    public static final String ALLOW_EARLY_END = "AllowEarlyEnd";

    public static final String DEFAULT_ALLOW_EARLY_END = "false";

    public static final String MAX_EXT_LENGTH = "MaxExtLength";

    public static final String DEFAULT_MAX_EXT_LENGTH = "28800000";

    public static final String IGNORE_B2B = "IgnoreB2B";

    public static final String DEFAULT_IGNORE_B2B = "false";

    public static final String NOTIFY_SAGE_ALERT = "NotifySageAlert";

    public static final String DEFAULT_NOTIFY_SAGE_ALERT = "false";

    public static final String SAGE_ALERT_ID = "SageAlertId";

    public static final String SAGE_ALERT_PWD = "SageAlertPwd";

    public static final String SAGE_ALERT_URL = "SageAlertUrl";

    public static final String EXIT_URL = "ExitURL";

    private static boolean dbInitialized = false;

    private static ThreadLocal<DataStore> THREAD_DB_CONNS = new ThreadLocal<DataStore>() {

        @Override
        protected DataStore initialValue() {
            try {
                return new DataStore();
            } catch (Exception e) {
                LOG.trace("Exception during data store init", e);
                LOG.fatal(e);
                return null;
            }
        }
    };

    static DataStore getInstance() {
        return THREAD_DB_CONNS.get();
    }

    private static String ERR_MISSING_TABLE = "^no such table.+";

    private File dataStore;

    private Connection conn;

    private DataStore() throws ClassNotFoundException, IOException {
        Class.forName("org.sqlite.JDBC");
        dataStore = new File("sre.sqlite");
        openConnection();
        synchronized (DataStore.class) {
            if (!dbInitialized) {
                LOG.info("Reading data store from '" + dataStore.getAbsolutePath() + "'");
                loadDDL();
                upgradeSchema();
                dbInitialized = true;
            }
        }
    }

    private void openConnection() throws IOException {
        try {
            if (conn != null && !conn.isClosed()) return;
            conn = DriverManager.getConnection("jdbc:sqlite:" + dataStore.getAbsolutePath());
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.fatal(e);
            throw new IOException("Error opening data store", e);
        }
        return;
    }

    @Override
    protected void finalize() {
        try {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.trace(SQL_ERROR, e);
                    LOG.error(SQL_ERROR, e);
                }
            }
        } finally {
            try {
                super.finalize();
            } catch (Throwable t) {
                LOG.trace(SQL_ERROR, t);
            }
        }
        return;
    }

    private void loadDDL() throws IOException {
        try {
            conn.createStatement().executeQuery("SELECT * FROM overrides").close();
        } catch (SQLException e) {
            Statement stmt = null;
            if (!e.getMessage().matches(ERR_MISSING_TABLE)) {
                LOG.trace(SQL_ERROR, e);
                LOG.fatal(e);
                throw new IOException("Error on initial data store read", e);
            }
            String[] qry = { "CREATE TABLE overrides (id INT NOT NULL, title VARCHAR(255) NOT NULL, subtitle VARCHAR(255) NOT NULL, PRIMARY KEY(id))", "CREATE TABLE settings (var VARCHAR(32) NOT NULL, val VARCHAR(255) NOT NULL, PRIMARY KEY(var))", "INSERT INTO settings (var, val) VALUES ('schema', '1')" };
            try {
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                for (String q : qry) stmt.executeUpdate(q);
                conn.commit();
            } catch (SQLException e2) {
                try {
                    conn.rollback();
                } catch (SQLException e3) {
                    LOG.trace(SQL_ERROR, e3);
                    LOG.error(e3);
                }
                LOG.trace(SQL_ERROR, e2);
                throw new IOException("Error initializing data store", e2);
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e4) {
                        LOG.trace(SQL_ERROR, e4);
                        LOG.error(e4);
                        throw new IOException("Unable to cleanup data store resources", e4);
                    }
                }
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e3) {
                    LOG.trace(SQL_ERROR, e3);
                    LOG.error(e3);
                    throw new IOException("Unable to reset data store auto commit", e3);
                }
            }
        }
        return;
    }

    private void upgradeSchema() throws IOException {
        Statement stmt = null;
        try {
            int i = getSchema();
            if (i < SCHEMA_VERSION) {
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                while (i < SCHEMA_VERSION) {
                    String qry;
                    switch(i) {
                        case 1:
                            qry = "CREATE TABLE log (id INTEGER PRIMARY KEY, context VARCHAR(16) NOT NULL, level VARCHAR(16) NOT NULL, time LONG INT NOT NULL, msg LONG VARCHAR NOT NULL, parent INT)";
                            stmt.executeUpdate(qry);
                            qry = "UPDATE settings SET val = '2' WHERE var = 'schema'";
                            stmt.executeUpdate(qry);
                            break;
                        case 2:
                            qry = "CREATE TABLE monitor (id INTEGER PRIMARY KEY NOT NULL, status INTEGER NOT NULL)";
                            stmt.executeUpdate(qry);
                            qry = "UPDATE settings SET val = '3' WHERE var = 'schema'";
                            stmt.executeUpdate(qry);
                            break;
                        case 3:
                            qry = "CREATE TABLE favs (id INTEGER PRIMARY KEY NOT NULL)";
                            stmt.executeUpdate(qry);
                            qry = "UPDATE settings SET val = '4' WHERE var = 'schema'";
                            stmt.executeUpdate(qry);
                            break;
                        case 4:
                            qry = "DROP TABLE log";
                            stmt.executeUpdate(qry);
                            qry = "UPDATE settings SET val = '5' WHERE var = 'schema'";
                            stmt.executeUpdate(qry);
                            break;
                        case 5:
                            qry = "UPDATE settings SET val = '120000' WHERE var = 'SleepTime'";
                            stmt.executeUpdate(qry);
                            qry = "UPDATE settings set val = '6' WHERE var = 'schema'";
                            stmt.executeUpdate(qry);
                            break;
                    }
                    i++;
                }
                conn.commit();
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e2) {
                LOG.trace(SQL_ERROR, e2);
                LOG.error(e2);
            }
            LOG.trace(SQL_ERROR, e);
            LOG.fatal(e);
            throw new IOException("Error upgrading data store", e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.trace(SQL_ERROR, e);
                throw new IOException("Unable to cleanup SQL resources", e);
            }
        }
    }

    private int getSchema() throws IOException {
        String qry = "SELECT val FROM settings WHERE var = 'schema'";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(qry);
            return rs.getInt(1);
        } catch (SQLException e) {
            if (!e.getMessage().matches(ERR_MISSING_TABLE)) {
                LOG.trace(SQL_ERROR, e);
                LOG.error(e);
            }
            return 0;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.trace(SQL_ERROR, e);
                LOG.error(e);
                throw new IOException("Unable to cleanup SQL resources", e);
            }
        }
    }

    boolean addOverride(int airingId, String title, String subtitle) {
        String insQry = "REPLACE INTO overrides (id, title, subtitle) VALUES (?, ?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(insQry);
            pstmt.setInt(1, airingId);
            pstmt.setString(2, title);
            pstmt.setString(3, subtitle);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
            return false;
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    LOG.trace(SQL_ERROR, e);
                    LOG.error(e);
                }
            }
        }
        return true;
    }

    String[] getOverride(int airingId) {
        String qry = "SELECT title, subtitle FROM overrides WHERE id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String[] retVal = null;
        try {
            pstmt = conn.prepareStatement(qry);
            pstmt.setInt(1, airingId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                retVal = new String[2];
                retVal[0] = rs.getString(1);
                retVal[1] = rs.getString(2);
            }
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                LOG.trace(SQL_ERROR, e);
                LOG.error(e);
            }
        }
        return retVal;
    }

    boolean deleteOverride(int airingId) {
        String qry = "DELETE FROM overrides WHERE id = ?";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            pstmt.setInt(1, airingId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
            return false;
        } finally {
            setMonitorStatus(airingId, SageRecordingExtender.getMonitorStatus(airingId));
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    LOG.trace(SQL_ERROR, e);
                    LOG.error(e);
                }
            }
        }
    }

    boolean clearOverrides() {
        String qry = "DELETE FROM overrides";
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(qry);
            return true;
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
            return false;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOG.trace(SQL_ERROR, e);
                    LOG.error(e);
                }
            }
        }
    }

    void clearStatus() {
        String qry = "DELETE FROM monitor";
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(qry);
            return;
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.trace(SQL_ERROR, e);
                LOG.error(e);
            }
        }
    }

    void setMonitorStatus(int id, int status) {
        String qry = "REPLACE INTO monitor (id, status) VALUES (" + id + ", " + status + ")";
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(qry);
            stmt.close();
            return;
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
        }
    }

    int getMonitorStatus(int id) {
        String qry = "SELECT status FROM monitor WHERE id = " + id;
        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(qry);
            if (rs.next()) return rs.getInt(1);
            return -1;
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
            return -1;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.trace(SQL_ERROR, e);
                LOG.error(e);
            }
        }
    }

    void registerFavourite(int id) {
        String qry = "REPLACE INTO favs (id) VALUES (" + id + ")";
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(qry);
            stmt.close();
            return;
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
        }
    }

    Integer[] getFavourites() {
        String qry = "SELECT id FROM favs";
        List<Integer> ids = new ArrayList<Integer>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(qry);
            while (rs.next()) ids.add(rs.getInt(1));
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
        }
        return ids.toArray(new Integer[ids.size()]);
    }

    void setSetting(String var, String val) {
        String qry = "REPLACE INTO settings (var, val) VALUES ('" + var + "', '" + val + "')";
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(qry);
            stmt.close();
            return;
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
        }
        return;
    }

    String getSetting(String var) {
        String qry = "SELECT val FROM settings WHERE var = '" + var + "'";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(qry);
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.trace(SQL_ERROR, e);
                LOG.error(e);
            }
        }
        return null;
    }

    String getSetting(String var, String defaultVal) {
        String val = getSetting(var);
        if (val == null) return defaultVal;
        return val;
    }

    void deleteFavourite(int id) {
        String qry = "DELETE FROM favs WHERE id = " + id;
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(qry);
            stmt.close();
            return;
        } catch (SQLException e) {
            LOG.trace(SQL_ERROR, e);
            LOG.error(e);
        }
    }
}
