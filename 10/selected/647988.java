package com.google.code.sagetvaddons.sre3.server;

import gkusnick.sagetv.api.API;
import gkusnick.sagetv.api.AiringAPI;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import com.google.code.sagetvaddons.sre3.plugin.SrePlugin;
import com.google.code.sagetvaddons.sre3.shared.AiringMonitorStatus;

/**
 * The SRE data store; stores details about overrides and monitor status info for scheduled recordings
 * @version $Id: DataStore.java 1415 2011-03-05 15:55:05Z derek@battams.ca $
 */
public final class DataStore {

    private static final Logger LOG = Logger.getLogger(DataStore.class);

    private static final int SCHEMA_VERSION = 1;

    private static final String SQL_ERROR = "SQL error";

    private static boolean dbInitialized = false;

    private static ThreadLocal<DataStore> THREAD_DB_CONNS = new ThreadLocal<DataStore>() {

        @Override
        protected DataStore initialValue() {
            try {
                return new DataStore();
            } catch (Exception e) {
                LOG.fatal("Exception during data store init", e);
                return null;
            }
        }
    };

    /**
	 * <p>Get a connection to the data store</p>
	 * 
	 * <p>If the return value is non-null then the object is guaranteed to be connected to the data store
	 * on the SageTV server.  The connection will be initialized, connected and ready to use.  If called
	 * from a SageTV server, pseudo-client, extender or placeshifter then the connection is to the local
	 * data store on the same physical host.  If the call is made from a SageTV client then it connects
	 * to the data store located on the SageTV server the client is connected to.  If an error is
	 * encountered during object construction then this method will log the error and return null.</p>
	 * 
	 * @return A valid, connected and ready to use connection to the data store or null in case of error
	 */
    public static DataStore getInstance() {
        DataStore ds = THREAD_DB_CONNS.get();
        int retries = 5;
        while (retries-- > 0 && !ds.isValid()) ds = THREAD_DB_CONNS.get();
        return ds;
    }

    private static String ERR_MISSING_TABLE = "Table \"OVERRIDES\" not found";

    private PreparedStatement overrideRmQry = null;

    private PreparedStatement overrideInsQry = null;

    private PreparedStatement overrideGetAllQry = null;

    private PreparedStatement overrideGetQry = null;

    private PreparedStatement monitorRmQry = null;

    private PreparedStatement monitorInsQry = null;

    private PreparedStatement monitorExpQry = null;

    private PreparedStatement monitorStatusQry = null;

    private PreparedStatement settingsRmQry = null;

    private PreparedStatement settingsInsQry = null;

    private PreparedStatement settingsGetQry = null;

    private Collection<Statement> stmts;

    private String dataStore;

    private Connection conn;

    private DataStore() throws ClassNotFoundException, IOException {
        stmts = new ArrayList<Statement>();
        Class.forName("org.h2.Driver");
        dataStore = SrePlugin.RESOURCE_DIR + "/" + SrePlugin.DB_FILE;
        openConnection();
        synchronized (DataStore.class) {
            if (!dbInitialized && !API.apiNullUI.global.IsClient()) {
                loadDDL();
                upgradeSchema();
                dbInitialized = true;
            }
        }
        try {
            initPreparedStatements();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private void initPreparedStatements() throws SQLException {
        overrideRmQry = conn.prepareStatement("DELETE FROM overrides WHERE id = ?");
        stmts.add(overrideRmQry);
        overrideInsQry = conn.prepareStatement("INSERT INTO overrides (id, title, subtitle, enable) VALUES (?, ?, ?, ?)");
        stmts.add(overrideInsQry);
        overrideGetAllQry = conn.prepareStatement("SELECT id FROM overrides");
        stmts.add(overrideGetAllQry);
        overrideGetQry = conn.prepareStatement("SELECT title, subtitle, enable FROM overrides WHERE id = ?");
        stmts.add(overrideGetQry);
        monitorRmQry = conn.prepareStatement("DELETE FROM monitor WHERE id = ?");
        stmts.add(monitorRmQry);
        monitorInsQry = conn.prepareStatement("INSERT INTO monitor (id, status, next_update) VALUES (?, ?, ?)");
        stmts.add(monitorInsQry);
        monitorExpQry = conn.prepareStatement("SELECT id FROM monitor WHERE next_update <= ?");
        stmts.add(monitorExpQry);
        monitorStatusQry = conn.prepareStatement("SELECT status FROM monitor WHERE id = ?");
        stmts.add(monitorStatusQry);
        settingsRmQry = conn.prepareStatement("DELETE FROM settings WHERE var = ?");
        stmts.add(settingsRmQry);
        settingsInsQry = conn.prepareStatement("INSERT INTO settings (var, val) VALUES (?, ?)");
        stmts.add(settingsInsQry);
        settingsGetQry = conn.prepareStatement("SELECT val FROM settings WHERE var = ?");
        stmts.add(settingsGetQry);
    }

    private void openConnection() throws IOException {
        try {
            if (conn != null && !conn.isClosed()) return;
            String tcpPort = API.apiNullUI.configuration.GetServerProperty("h2/tcp_port", "9092");
            String jdbcUrl = "jdbc:h2:tcp://" + API.apiNullUI.global.GetServerAddress() + ":" + tcpPort + "/" + dataStore;
            LOG.info("Using DB: " + jdbcUrl);
            conn = DriverManager.getConnection(jdbcUrl, "admin", "admin");
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
            close();
        } finally {
            try {
                super.finalize();
            } catch (Throwable t) {
                LOG.error("FinalizeError", t);
            }
        }
        return;
    }

    public void close() {
        for (Statement s : stmts) if (s != null) try {
            s.close();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        }
        stmts.clear();
        if (conn != null) try {
            conn.close();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        }
    }

    private void loadDDL() throws IOException {
        try {
            conn.createStatement().executeQuery("SELECT * FROM overrides").close();
        } catch (SQLException e) {
            Statement stmt = null;
            if (!e.getMessage().startsWith(ERR_MISSING_TABLE)) {
                LOG.fatal(SQL_ERROR, e);
                throw new IOException("Error on initial data store read", e);
            }
            String[] qry = { "CREATE TABLE monitor (id INTEGER PRIMARY KEY NOT NULL, status VARCHAR(32) NOT NULL, next_update TIMESTAMP NOT NULL)", "CREATE TABLE overrides (id INT NOT NULL, title VARCHAR(255) NOT NULL, subtitle VARCHAR(255) NOT NULL, enable BOOLEAN NOT NULL DEFAULT TRUE, PRIMARY KEY(id))", "CREATE TABLE settings (var VARCHAR(32) NOT NULL, val VARCHAR(255) NOT NULL, PRIMARY KEY(var))", "INSERT INTO settings (var, val) VALUES ('schema', '1')" };
            try {
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                for (String q : qry) stmt.executeUpdate(q);
                conn.commit();
            } catch (SQLException e2) {
                try {
                    conn.rollback();
                } catch (SQLException e3) {
                    LOG.fatal(SQL_ERROR, e3);
                }
                LOG.fatal(SQL_ERROR, e2);
                throw new IOException("Error initializing data store", e2);
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e4) {
                        LOG.fatal(SQL_ERROR, e4);
                        throw new IOException("Unable to cleanup data store resources", e4);
                    }
                }
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e3) {
                    LOG.fatal(SQL_ERROR, e3);
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
            LOG.info("DB is currently at schema " + i);
            if (i < SCHEMA_VERSION) {
                LOG.info("Upgrading from schema " + i + " to schema " + SCHEMA_VERSION);
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                while (i < SCHEMA_VERSION) {
                    String qry;
                    switch(i) {
                        case 1:
                            qry = "UPDATE settings SET val = '2' WHERE var = 'schema'";
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
                LOG.error(SQL_ERROR, e2);
            }
            LOG.fatal(SQL_ERROR, e);
            throw new IOException("Error upgrading data store", e);
        } finally {
            try {
                if (stmt != null) stmt.close();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
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
            if (rs.next()) return Integer.parseInt(rs.getString(1)); else {
                LOG.fatal("Unable to find schema version!");
                return 0;
            }
        } catch (SQLException e) {
            if (!e.getMessage().matches(ERR_MISSING_TABLE)) {
                LOG.error(SQL_ERROR, e);
            }
            return 0;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
                throw new IOException("Unable to cleanup SQL resources", e);
            }
        }
    }

    /**
	 * <p>Determine if this data store connection is still in a valid state (i.e. it's still connected to the database server)</p>
	 * 
	 * <p>If isValid() returns false then the data store connection is removed from the thread local storage such that the next
	 * call to getInstance() will return a new, freshly initiated data store connection for use.</p>
	 * @return True if the connection is still valid or false otherwise
	 */
    public boolean isValid() {
        try {
            boolean retVal = conn.isValid(3);
            if (!retVal) THREAD_DB_CONNS.remove();
            return retVal;
        } catch (SQLException e) {
            THREAD_DB_CONNS.remove();
            LOG.error(SQL_ERROR, e);
            return false;
        }
    }

    /**
	 * Convenience method for determining if this installation of SRE has successfully configured a Google Account for access to livepvrdata.com
	 * @return True if the Google Account has been properly configured or false otherwise
	 */
    public boolean isGoogleAccountRegistered() {
        AppEnv env = AppEnv.getInstance();
        return env.getOAuthToken() != null && env.getOAuthToken().length() > 0 && env.getOAuthSecret() != null && env.getOAuthSecret().length() > 0;
    }

    /**
	 * <p>Save a new override for an airing if and only if livepvrdata verifies the override</p>
	 * 
	 * <p>This is a convenience method for consumers of this API (web UI and STVi) and saves the need for
	 * having to call OverrideValidator.check() and DataStore.addOverride() in sequence</p>
	 * 
	 * @param airingId The SageTV airing ID this override is for
	 * @param title The override title
	 * @param subtitle The override subtitle (episode)
	 * @param active True if this monitor is active or false if this monitor is disabled
	 * @return The status of the override
	 * @throws IllegalArgumentException Thrown if the airing id is not found in the SageTV database
	 */
    public AiringMonitorStatus newOverride(int airingId, String title, String subtitle, boolean active) {
        AiringAPI.Airing a = API.apiNullUI.airingAPI.GetAiringForID(airingId);
        if (a != null) {
            AiringMonitorStatus status = OverrideValidator.check(airingId, title, subtitle, new String[0], "", a.GetAiringStartTime(), true);
            if ((status == AiringMonitorStatus.VALID || status == AiringMonitorStatus.UNKNOWN) && !addOverride(airingId, title, subtitle, active)) throw new RuntimeException("Unable to save override!");
            return status;
        } else throw new IllegalArgumentException("Invalid airing id! [" + airingId + "]");
    }

    /**
	 * <p>Save/update an override in the data store</p>
	 * 
	 * <p>Save a new override or update an existing override in the data store.  If an override already
	 * exists for the given airing id then it is overwritten.</p>
	 * 
	 * @param airingId The airing id this override is for
	 * @param title The override title
	 * @param subtitle The override subtitle (episode)
	 * @param active If true, this airing will be monitored; if false this airing will not be monitored
	 * @return True on successful update of data store or false on error
	 */
    boolean addOverride(int airingId, String title, String subtitle, boolean active) {
        boolean autoCommit = false;
        try {
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            overrideRmQry.setInt(1, airingId);
            overrideRmQry.executeUpdate();
            overrideInsQry.setInt(1, airingId);
            overrideInsQry.setString(2, title);
            overrideInsQry.setString(3, subtitle);
            overrideInsQry.setBoolean(4, active);
            overrideInsQry.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                LOG.error(SQL_ERROR, e1);
            }
            LOG.error(SQL_ERROR, e);
            return false;
        } finally {
            try {
                conn.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return true;
    }

    /**
	 * Get an array of all defined overrides currently in the data store.
	 * @return An array of all currently defined overrides in the data store.
	 */
    public AiringOverride[] getOverrides() {
        ResultSet rs = null;
        Collection<AiringOverride> overrides = new ArrayList<AiringOverride>();
        try {
            rs = overrideGetAllQry.executeQuery();
            while (rs.next()) overrides.add(getOverride(rs.getInt(1)));
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return overrides.toArray(new AiringOverride[overrides.size()]);
    }

    /**
	 * Get the override for the given airing id
	 * @param airingId The SageTV airing id to look up
	 * @return The override for the given airing id or null if no such override exists
	 */
    public AiringOverride getOverride(int airingId) {
        ResultSet rs = null;
        try {
            overrideGetQry.setInt(1, airingId);
            rs = overrideGetQry.executeQuery();
            if (rs.next()) return new AiringOverride(airingId, rs.getString(1), rs.getString(2), rs.getBoolean(3));
            return null;
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * <p>Delete the override defined for the given airing id</p>
	 * 
	 * <p>Calling this method with a non-existant/invalid airing id does no harm.</p>
	 * 
	 * @param airingId The SageTV airing id override to delete
	 * @return True on success or false on error
	 */
    public boolean deleteOverride(int airingId) {
        try {
            overrideRmQry.setInt(1, airingId);
            if (overrideRmQry.executeUpdate() == 1) {
                LOG.info("Resetting monitor status for deleted override on airing id " + airingId);
                AiringAPI.Airing a = API.apiNullUI.airingAPI.GetAiringForID(airingId);
                if (a != null) OverrideValidator.check(airingId, a.GetAiringTitle(), a.GetShow().GetShowEpisode(), a.GetShow().GetPeopleListInShowInRole("Team"), "", a.GetAiringStartTime(), false); else LOG.error("Airing is null! [" + airingId + "]");
            }
            return true;
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
            return false;
        }
    }

    void setMonitorStatus(int id, AiringMonitorStatus status, long start, boolean fireRecSchedEvent) {
        AiringMonitorStatus prev = getMonitorStatus(id);
        long nextUpdate = Math.min(start - 1800000, System.currentTimeMillis() + 86400000);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.setTimeInMillis(start);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 15);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() > System.currentTimeMillis()) nextUpdate = Math.min(nextUpdate, cal.getTimeInMillis());
        boolean autoCommit = true;
        try {
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            monitorRmQry.setInt(1, id);
            monitorRmQry.executeUpdate();
            monitorInsQry.setInt(1, id);
            monitorInsQry.setString(2, status.toString());
            monitorInsQry.setTimestamp(3, new Timestamp(nextUpdate));
            monitorInsQry.executeUpdate();
            conn.commit();
            if (fireRecSchedEvent && status != prev && (API.apiNullUI.global.IsServerUI() || API.apiNullUI.global.IsRemoteUI())) RecSchedServiceImpl.recSchedChanged();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                LOG.error(SQL_ERROR, e1);
            }
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                conn.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    Integer[] getMonitorStatusExpiredIds() {
        ResultSet rs = null;
        List<Integer> ids = new ArrayList<Integer>();
        try {
            monitorExpQry.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            rs = monitorExpQry.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return ids.toArray(new Integer[ids.size()]);
    }

    /**
	 * Get the monitor status for the given airing id
	 * @param id The SageTV airing id to lookup
	 * @return The monitor status for the given id; if the airing id is invalid then this method will return {@link com.google.code.sagetvaddons.sre3.shared.AiringMonitorStatus#UNKNOWN AiringMonitorStatus.UNKNOWN}
	 */
    public AiringMonitorStatus getMonitorStatus(int id) {
        ResultSet rs = null;
        try {
            monitorStatusQry.setInt(1, id);
            rs = monitorStatusQry.executeQuery();
            if (rs.next()) return AiringMonitorStatus.valueOf(rs.getString(1));
            return AiringMonitorStatus.UNKNOWN;
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
            return AiringMonitorStatus.UNKNOWN;
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Determine if a status exists for the given airing id in the data store
	 * @param airingId The SageTV airing id to look up
	 * @return True if a status exists in the data store for the airing id or false otherwise
	 */
    public boolean monitorStatusExists(int airingId) {
        ResultSet rs = null;
        try {
            monitorStatusQry.setInt(1, airingId);
            rs = monitorStatusQry.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
            return false;
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    void setSetting(String var, String val) {
        boolean autoCommit = true;
        try {
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            settingsRmQry.setString(1, var);
            settingsRmQry.executeUpdate();
            settingsInsQry.setString(1, var);
            settingsInsQry.setString(2, val);
            settingsInsQry.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                LOG.error(SQL_ERROR, e1);
            }
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                conn.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    String getSetting(String var) {
        ResultSet rs = null;
        try {
            settingsGetQry.setString(1, var);
            rs = settingsGetQry.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return null;
    }

    String getSetting(String var, String defaultVal) {
        String val = getSetting(var);
        if (val == null) return defaultVal;
        return val;
    }

    boolean deleteMonitorStatus(int id) {
        try {
            monitorRmQry.setInt(1, id);
            monitorRmQry.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
            return false;
        }
    }
}
