package com.google.code.sagetvaddons.sagealert.server;

import gkusnick.sagetv.api.API;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.mortbay.jetty.security.Password;
import com.google.code.sagetvaddons.sagealert.shared.Client;
import com.google.code.sagetvaddons.sagealert.shared.IsDataStoreSerializable;
import com.google.code.sagetvaddons.sagealert.shared.NotificationServerSettings;
import com.google.code.sagetvaddons.sagealert.shared.SageAlertEventMetadata;
import com.google.code.sagetvaddons.sagealert.shared.SmtpSettings;

/**
 * @author dbattams
 *
 */
public final class DataStore {

    private static final Logger LOG = Logger.getLogger(DataStore.class);

    private static final Logger SQL_LOG = Logger.getLogger("com.google.code.sagetvaddons.sagealert.server.SQLLogger");

    private static final String SAGEX_ALIAS_PROP = "sagex/uicontexts/%s/name";

    private static final int SCHEMA_VERSION = 1;

    private static final ThreadLocal<DataStore> THREAD_DATA_STORES = new ThreadLocal<DataStore>() {

        @Override
        protected DataStore initialValue() {
            try {
                return new DataStore();
            } catch (Exception e) {
                LOG.fatal("DataStoreAssignmentError", e);
                return null;
            }
        }
    };

    /**
	 * Return the thread local instance of the data store; create it if necessary
	 * @return An instance of the data store
	 */
    public static final DataStore getInstance() {
        return THREAD_DATA_STORES.get();
    }

    private static final String ERR_MISSING_TABLE = "^no such table.+";

    public static final String CLNT_SETTING_PREFIX = "sage.client.";

    private static final String SQL_ERROR = "SQL error";

    private static final String SMTP_SETTINGS = "SmtpSettings";

    private static final void logQry(String qry, Object... params) {
        if (SQL_LOG.isTraceEnabled()) {
            if (params != null && params.length > 0) qry = qry.concat(" " + Arrays.toString(params));
            SQL_LOG.trace(qry);
        }
    }

    private static boolean dbInitialized = false;

    private Connection conn;

    private File dataStore;

    private DataStore() throws ClassNotFoundException, IOException {
        Class.forName("org.sqlite.JDBC");
        dataStore = new File("plugins/sagealert/sagealert.sqlite");
        openConnection();
        synchronized (DataStore.class) {
            if (!dbInitialized) {
                loadDDL();
                upgradeSchema();
                dbInitialized = true;
                LOG.debug("Using '" + dataStore.getAbsolutePath() + "' as SQLite database file.");
            }
        }
    }

    private void openConnection() throws IOException {
        try {
            if (conn != null && !conn.isClosed()) return;
            conn = DriverManager.getConnection("jdbc:sqlite:" + dataStore.getAbsolutePath());
        } catch (SQLException e) {
            String msg = "Error opening data store";
            LOG.fatal(msg, e);
            throw new IOException(msg, e);
        }
    }

    private void loadDDL() throws IOException {
        try {
            conn.createStatement().executeQuery("SELECT * FROM reporters").close();
        } catch (SQLException e) {
            Statement stmt = null;
            if (!e.getMessage().matches(ERR_MISSING_TABLE)) {
                String msg = "Error on initial data store read";
                LOG.fatal(msg, e);
                throw new IOException(msg, e);
            }
            String[] qry = { "CREATE TABLE reporters (type LONG VARCHAR NOT NULL, key LONG VARCHAR NOT NULL, data LONG VARCHAR, PRIMARY KEY(type, key))", "CREATE TABLE listeners (event VARCHAR(255) NOT NULL, type LONG VARCHAR NOT NULL, key LONG VARCHAR NOT NULL, PRIMARY KEY(event, type, key))", "CREATE TABLE settings (var VARCHAR(32) NOT NULL, val VARCHAR(255) NOT NULL, PRIMARY KEY(var))", "INSERT INTO settings (var, val) VALUES ('schema', '1')" };
            try {
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                for (String q : qry) {
                    logQry(q);
                    stmt.executeUpdate(q);
                }
                conn.commit();
            } catch (SQLException e2) {
                String msg = "Error initializing data store";
                try {
                    conn.rollback();
                } catch (SQLException e3) {
                    LOG.fatal(msg, e3);
                }
                LOG.fatal(msg, e2);
                throw new IOException(msg);
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e4) {
                        String msg = "Unable to cleanup data store resources";
                        LOG.fatal(msg, e4);
                        throw new IOException(msg);
                    }
                }
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e3) {
                    String msg = "Unable to reset data store auto commit";
                    LOG.fatal(msg, e3);
                    throw new IOException(msg);
                }
            }
        }
        return;
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

    private void upgradeSchema() throws IOException {
        Statement stmt = null;
        try {
            int i = getSchema();
            if (i < SCHEMA_VERSION) {
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                while (i < SCHEMA_VERSION) {
                    switch(i) {
                        case 1:
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

    /**
	 * Save a reporter to the data store
	 * @param reporter The reporter to be saved
	 */
    public void saveReporter(NotificationServerSettings reporter) {
        String qry = "REPLACE INTO reporters (type, key, data) VALUES (?, ?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            String type = reporter.getClass().getCanonicalName();
            String key = Password.obfuscate(reporter.getDataStoreKey());
            String data = reporter.getDataStoreData();
            if (data == null) data = "";
            data = Password.obfuscate(data);
            logQry(qry, type, key, data);
            pstmt.setString(1, type);
            pstmt.setString(2, key);
            pstmt.setString(3, data);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error("pStmt close error", e);
            }
        }
    }

    /**
	 * Get a list of all reporters of a given type
	 * @param type The type (FQ class name) of reporters to retrieve
	 * @return A list of all reporters of the given type; this list may be empty, but not null
	 */
    public List<NotificationServerSettings> getReporters(String type) {
        String qry = "SELECT key, data FROM reporters WHERE type = ?";
        List<NotificationServerSettings> objs = new ArrayList<NotificationServerSettings>();
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        try {
            pstmt = conn.prepareStatement(qry);
            logQry(qry, type);
            pstmt.setString(1, type);
            rset = pstmt.executeQuery();
            while (rset.next()) objs.add(reflectReporter(type, rset.getString(1), rset.getString(2)));
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                if (rset != null) rset.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return objs;
    }

    /**
	 * Delete all reporters of a given type
	 * @param type The type of reporters to delete
	 */
    public void deleteReporters(Class<?> type) {
        String qry = "DELETE FROM reporters WHERE type = ?";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            String clsName = type.getCanonicalName();
            logQry(qry, clsName);
            pstmt.setString(1, clsName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Delete a specific reporter
	 * @param type The type of reporter to be deleted (FQ class name)
	 * @param key The key that describe the unique reporter to be deleted
	 */
    public void deleteReporter(String type, String key) {
        String qry = "DELETE FROM reporters WHERE type = ? AND key = ?";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            logQry(qry, type, key);
            pstmt.setString(1, type);
            pstmt.setString(2, Password.obfuscate(key));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Get a list of all reporters stored in the data store
	 * @return A list of all reporters in the data store; the list may be empty, but not null
	 */
    public List<NotificationServerSettings> getAllReporters() {
        String qry = "SELECT type, key, data FROM reporters";
        List<NotificationServerSettings> reporters = new ArrayList<NotificationServerSettings>();
        Statement stmt = null;
        ResultSet rset = null;
        try {
            stmt = conn.createStatement();
            logQry(qry);
            rset = stmt.executeQuery(qry);
            while (rset.next()) reporters.add(reflectReporter(rset.getString(1), rset.getString(2), rset.getString(3)));
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                if (rset != null) rset.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return reporters;
    }

    /**
	 * Get all the registered handlers for a given event
	 * @param event A description of the event
	 * @return A list of all handlers for the given event description
	 */
    public List<NotificationServerSettings> getHandlers(String event) {
        String qry = "SELECT l.type, l.key, r.data FROM listeners AS l OUTER JOIN reporters AS r ON (l.type = r.type AND l.key = r.key) WHERE event = '" + event + "'";
        List<NotificationServerSettings> handlers = new ArrayList<NotificationServerSettings>();
        Statement stmt = null;
        ResultSet rset = null;
        try {
            stmt = conn.createStatement();
            logQry(qry);
            rset = stmt.executeQuery(qry);
            while (rset.next()) handlers.add(reflectReporter(rset.getString(1), rset.getString(2), rset.getString(3)));
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                if (rset != null) rset.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return handlers;
    }

    private NotificationServerSettings reflectReporter(String clsName, String key, String data) {
        NotificationServerSettings obj = null;
        try {
            Class<?> cls = Class.forName(clsName);
            Constructor<?> ctor = cls.getConstructor((Class<?>[]) null);
            ctor.setAccessible(true);
            obj = (NotificationServerSettings) ctor.newInstance((Object[]) null);
            Method method = cls.getDeclaredMethod("unserialize", new Class<?>[] { String.class, String.class });
            method.setAccessible(true);
            method.invoke(obj, Password.deobfuscate(key), Password.deobfuscate(data));
        } catch (ClassNotFoundException e) {
            LOG.error("Class not found", e);
        } catch (NoSuchMethodException e) {
            LOG.error("No such method", e);
        } catch (InvocationTargetException e) {
            LOG.error("Invocation target exception", e);
        } catch (InstantiationException e) {
            LOG.error("Instantiation exception", e);
        } catch (IllegalAccessException e) {
            LOG.error("Illegal access", e);
        }
        return obj;
    }

    /**
	 * Register the given handlers for the given event
	 * @param event The event name to attach the handlers to
	 * @param handlers The list of handlers to attach to the given event
	 */
    public void registerHandlers(String event, List<NotificationServerSettings> handlers) {
        String qry = "REPLACE INTO listeners (event, type, key) VALUES (?, ?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            pstmt.setString(1, event);
            for (IsDataStoreSerializable obj : handlers) {
                String clsName = obj.getClass().getCanonicalName();
                String key = obj.getDataStoreKey();
                logQry(qry, event, clsName, key);
                pstmt.setString(2, clsName);
                pstmt.setString(3, Password.obfuscate(key));
                pstmt.addBatch();
            }
            if (handlers.size() > 0) pstmt.executeBatch();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Remove the given handlers from the given event
	 * @param event The event name to detach the handlers from
	 * @param handlers The list of handlers to detach from the given event
	 */
    public void removeHandlers(String event, List<NotificationServerSettings> handlers) {
        String qry = "DELETE FROM listeners WHERE event = ? AND type = ? AND key = ?";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            for (IsDataStoreSerializable s : handlers) {
                String clsName = s.getClass().getCanonicalName();
                String key = s.getDataStoreKey();
                logQry(qry, clsName, key);
                pstmt.setString(1, event);
                pstmt.setString(2, clsName);
                pstmt.setString(3, Password.obfuscate(key));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Remove the handlers from all events; this only removes the links in the data store, it does NOT remove them from the in memory manager, you must also remove them from the manager for events to stop firing to them
	 * @param handlers The list of handlers to delete
	 */
    public void removeHandlers(List<NotificationServerSettings> handlers) {
        String qry = "DELETE FROM listeners WHERE type = ? AND key = ?";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            for (IsDataStoreSerializable s : handlers) {
                String clsName = s.getClass().getCanonicalName();
                String key = s.getDataStoreKey();
                logQry(qry, clsName, key);
                pstmt.setString(1, clsName);
                pstmt.setString(2, Password.obfuscate(key));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Remove all handlers from a given event; this only removes them from the data store; active handlers remain active until the handler manager is told otherwise
	 * @param event The event for which all handlers should be removed
	 */
    public void removeAllHandlers(String event) {
        String qry = "DELETE FROM listeners WHERE event = '" + event + "'";
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            logQry(qry);
            stmt.executeUpdate(qry);
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (stmt != null) try {
                stmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    public String[] getRegisteredEvents() {
        String qry = "SELECT DISTINCT event FROM listeners";
        Statement stmt = null;
        ResultSet rs = null;
        ArrayList<String> list = new ArrayList<String>();
        try {
            stmt = conn.createStatement();
            logQry(qry);
            rs = stmt.executeQuery(qry);
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
	 * Retrieve a setting from the data store
	 * @param var The name of the setting to get
	 * @param defaultVal The default value to return if the setting does not exist in the data store; null is allowed
	 * @return The value of the given setting or defaultVal if the setting is not in the data store
	 */
    public String getSetting(String var, String defaultVal) {
        String qry = "SELECT val FROM settings WHERE var = ?";
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        try {
            pstmt = conn.prepareStatement(qry);
            logQry(qry, var);
            pstmt.setString(1, var);
            rset = pstmt.executeQuery();
            if (rset.next()) return rset.getString(1);
            return defaultVal;
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
            return defaultVal;
        } finally {
            try {
                if (rset != null) rset.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Get a setting from the data store
	 * @param var The setting to get
	 * @return The value of the setting or null if the setting does not exist in the data store
	 */
    public String getSetting(String var) {
        return getSetting(var, null);
    }

    /**
	 * Store a setting in the data store; replaces the value if the setting name already exists in the data store
	 * @param var The name of the setting
	 * @param val The value of the setting
	 */
    public void setSetting(String var, String val) {
        String qry = "REPLACE INTO settings (var, val) VALUES (?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            logQry(qry);
            pstmt.setString(1, var);
            pstmt.setString(2, val);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Retrieve the collection of all clients stored in the data store
	 * @return A Collection of Client instances; the list may be empty, but not null
	 */
    public List<Client> getClients() {
        String qry = "SELECT var, val FROM settings WHERE var LIKE '" + CLNT_SETTING_PREFIX + "%'";
        List<Client> list = new ArrayList<Client>();
        Statement stmt = null;
        ResultSet rset = null;
        try {
            stmt = conn.createStatement();
            logQry(qry);
            rset = stmt.executeQuery(qry);
            while (rset.next()) list.add(getClient(rset.getString(1).substring(CLNT_SETTING_PREFIX.length())));
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            try {
                if (rset != null) rset.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
        return list;
    }

    /**
	 * Retrieve a client by id
	 * @param id The unique client id to be retrieved
	 * @return A Client instance representing the given Client id
	 */
    public Client getClient(String id) {
        id = massageClientId(id);
        String alias = API.apiNullUI.configuration.GetServerProperty(String.format(SAGEX_ALIAS_PROP, id), id);
        return new Client(id, alias);
    }

    /**
	 * Save a Client to the data store
	 * @param c The Client instance to be saved
	 */
    public void saveClient(Client c) {
        API.apiNullUI.configuration.SetServerProperty(String.format(SAGEX_ALIAS_PROP, c.getId()), c.getAlias());
        setSetting(CLNT_SETTING_PREFIX + c.getId(), c.getAlias());
    }

    /**
	 * Delete the list of clients from the data store
	 * @param clients The list of clients to be deleted
	 */
    public void deleteClients(Collection<Client> clients) {
        String qry = "DELETE FROM settings WHERE var = ?";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            for (Client c : clients) {
                pstmt.setString(1, CLNT_SETTING_PREFIX + c.getId());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    /**
	 * Register a new client with SageAlert
	 * @param id The client id
	 * @return True if the new client was registered or false if the client already existed in the data store
	 */
    public boolean registerClient(String id) {
        id = massageClientId(id);
        if (getSetting(CLNT_SETTING_PREFIX + id) != null) return false;
        saveClient(getClient(id));
        return true;
    }

    private static String massageClientId(String id) {
        Pattern p = Pattern.compile("[^\\d]*(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}).*");
        Matcher m = p.matcher(id);
        if (m.matches()) {
            LOG.debug("Client id [" + id + "] looks like an IP address, converted id to: '" + m.group(1) + "'");
            id = m.group(1);
        }
        return id;
    }

    /**
	 * Unserialize and construct the SmtpServer settings object
	 * @return The SmtpServer settings object
	 */
    public SmtpSettings getSmtpSettings() {
        String jsonEnc = getSetting(SMTP_SETTINGS);
        if (jsonEnc == null) {
            LOG.error("No SMTP settings found; you will not be able to send email based alerts until you configure your SMTP settings!");
            return new SmtpSettings("", -1, "", "", "", false);
        }
        JSONObject jobj = null;
        try {
            jobj = new JSONObject(Password.deobfuscate(jsonEnc));
            String host, user, pwd, from;
            int port;
            boolean ssl;
            if (jobj.has("host")) host = jobj.getString("host"); else host = "";
            if (jobj.has("port")) port = jobj.getInt("port"); else port = -1;
            if (jobj.has("user")) user = jobj.getString("user"); else user = "";
            if (jobj.has("pwd")) pwd = jobj.getString("pwd"); else pwd = "";
            if (jobj.has("from")) from = jobj.getString("from"); else from = "";
            if (jobj.has("ssl")) ssl = jobj.getBoolean("ssl"); else ssl = false;
            return new SmtpSettings(host, port, user, pwd, from, ssl);
        } catch (JSONException e) {
            LOG.error("JSON error", e);
            return null;
        }
    }

    /**
	 * Serialize and save the SmtpServer settings object to the data store
	 * @param settings The object to be written to the data store
	 */
    public void saveSmtpSettings(SmtpSettings settings) {
        JSONObject jobj = new JSONObject();
        try {
            jobj.put("host", settings.getHost());
            jobj.put("port", settings.getPort());
            jobj.put("user", settings.getUser());
            jobj.put("pwd", settings.getPwd());
            jobj.put("from", settings.getSenderAddress());
            jobj.put("ssl", settings.useSsl());
            setSetting(SMTP_SETTINGS, Password.obfuscate(jobj.toString()));
        } catch (JSONException e) {
            LOG.error("JSON error", e);
        }
    }

    public NotificationServerSettings reloadSettings(NotificationServerSettings settings) {
        if (settings == null) return settings;
        for (NotificationServerSettings s : getReporters(settings.getClass().getCanonicalName())) if (s.getDataStoreKey().equals(settings.getDataStoreKey())) return s;
        return null;
    }

    public void clearSettingsStartingWith(String prefix) {
        String qry = "DELETE FROM settings WHERE var LIKE ? ESCAPE '\\'";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            pstmt.setString(1, prefix.replace("_", "\\_").replace("%", "\\%") + "%");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    public void clearSettingsEndingWith(String suffix) {
        String qry = "DELETE FROM settings WHERE var LIKE ? ESCAPE '\\'";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            pstmt.setString(1, "%" + suffix.replace("_", "\\_").replace("%", "\\%"));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error(SQL_ERROR, e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (SQLException e) {
                LOG.error(SQL_ERROR, e);
            }
        }
    }

    public void resetAllAlertMessages() {
        String[] keys = new String[] { SageAlertEventMetadata.SUBJ_SUFFIX, SageAlertEventMetadata.SHORT_SUFFIX, SageAlertEventMetadata.MED_SUFFIX, SageAlertEventMetadata.LONG_SUFFIX };
        for (String k : keys) clearSettingsEndingWith(k);
    }
}
