package com.entelience.sql;

import org.apache.log4j.Logger;
import com.entelience.util.Logs;
import java.util.Map;
import java.util.HashMap;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Factory class for database connections.
 * 
 * Database connections are keyed on the (shortname) of the company
 * for which they're implemented.
 *
 * A null database connection string will be interpreted as returning the
 * main database.
 */
public class DynamicConnections {

    private DynamicConnections() {
    }

    private static final Logger _logger = Logs.getLogger();

    private static final Object lock = new Object();

    private static Map<String, DbPool> readOnly = null;

    private static Map<String, DbPool> readWrite = null;

    private static boolean configured = false;

    private static Map<Integer, DbAuth> byCompanyId = null;

    private static Map<String, DbAuth> byShortName = null;

    /**
     * Fill in the byCompanyId and byShortName maps.
     *
     * Called once only.
     */
    private static void configureLookupMaps(Db mainDb, boolean update) throws Exception {
        if (configured && update == false) return;
        synchronized (lock) {
            try {
                _logger.info("Configuring the database lookup map from database (" + mainDb + ")");
                mainDb.enter();
                if (configured == false) {
                    readOnly = new HashMap<String, DbPool>();
                    readWrite = new HashMap<String, DbPool>();
                    byCompanyId = new HashMap<Integer, DbAuth>();
                    byShortName = new HashMap<String, DbAuth>();
                } else {
                    if (readOnly == null || readWrite == null || byCompanyId == null || byShortName == null) throw new IllegalStateException("DynamicConnections is in weird state, at least one key cache item is null");
                }
                {
                    PreparedStatement pst = mainDb.prepareStatement("SELECT e_company_id, connection_url, shortname, username, password FROM e_company WHERE deleted IS FALSE");
                    ResultSet rs = mainDb.executeQuery(pst);
                    if (rs.next()) {
                        do {
                            String connection = rs.getString(2);
                            String shortName = rs.getString(3).toLowerCase();
                            int companyId = rs.getInt(1);
                            String password = rs.getString(5);
                            if (password == null) _logger.warn("Using a null password for connection (" + connection + ")");
                            if (update && byCompanyId.get(companyId) != null) {
                                _logger.debug("Skipping company (" + companyId + ") already in database lookup map");
                                continue;
                            }
                            DbAuth auth = null;
                            if (connection != null) {
                                auth = new DbAuth(shortName, rs.getString(4), rs.getString(5), connection, companyId);
                                _logger.info("Adding a connection (" + connection + ") for company (" + companyId + ") with short name (" + shortName + ")");
                            } else {
                                _logger.warn("Company (" + companyId + ") has a null connectionURL");
                            }
                            byCompanyId.put(rs.getInt(1), auth);
                            _logger.info("Company (" + shortName + ") database connection is configured for lookup by its company id (" + companyId + ")");
                        } while (rs.next());
                    }
                }
                {
                    PreparedStatement pst = mainDb.prepareStatement("SELECT shortname, connection_url, username, password, e_company_id FROM e_company WHERE deleted IS FALSE");
                    ResultSet rs = mainDb.executeQuery(pst);
                    if (rs.next()) {
                        do {
                            String shortName = rs.getString(1).toLowerCase();
                            String connection = rs.getString(2);
                            int companyId = rs.getInt(5);
                            if (update && byShortName.get(shortName) != null) {
                                _logger.debug("Skipping company (" + shortName + ") already in database lookup map");
                                continue;
                            }
                            DbAuth auth = null;
                            if (connection != null) {
                                auth = new DbAuth(shortName, rs.getString(3), rs.getString(4), connection, companyId);
                                _logger.info("Adding a connection (" + connection + ") for company (" + companyId + ") with short name (" + shortName + ")");
                            }
                            byShortName.put(shortName, auth);
                            _logger.info("Company (" + shortName + ") database connection is configured for lookup by its shortname");
                        } while (rs.next());
                    }
                }
                configured = true;
                _logger.info("The companies database loookup map is configured");
            } finally {
                if (mainDb != null) mainDb.exit();
            }
        }
    }

    /**
	 * This will trigger a refresh of the cache. Must be called when a company is
	 * created, updated or deleted.
	 */
    public static void refresh(Db mainDb) throws Exception {
        configureLookupMaps(mainDb, true);
    }

    /**
     * Get the connection data by company id
     */
    private static DbAuth getAuth(Db mainDb, int companyId) throws Exception {
        if (!configured) configureLookupMaps(mainDb, false);
        synchronized (lock) {
            DbAuth auth = byCompanyId.get(companyId);
            if (auth == null) _logger.warn("Could not retrieve a connection (" + byCompanyId.size() + ") for id (" + companyId + ")");
            return auth;
        }
    }

    /**
     * Get the connection data by short name
     */
    private static DbAuth getAuth(Db mainDb, String shortName) throws Exception {
        if (shortName == null) {
            _logger.warn("Invalid company shortname (" + shortName + ") returning a null DbAuth");
            return null;
        }
        if (!configured) configureLookupMaps(mainDb, false);
        synchronized (lock) {
            DbAuth auth = byShortName.get(shortName.toLowerCase());
            if (auth == null) _logger.warn("Could not retrieve a connection (" + byShortName.size() + ") for id (" + shortName + ")");
            return auth;
        }
    }

    /**
     * Get the DbPool object for read/write connections
     */
    private static DbPool getPoolRW(DbAuth auth) throws Exception {
        synchronized (lock) {
            DbPool pool = readWrite.get(auth.getConnection());
            if (pool == null) {
                pool = new DbPool(auth.getShortName() + "RW", DbConnection.txnAutoCommit, DbConnection.txnIsolation, auth.getUsername(), auth.getPassword(), auth.getConnection(), true, auth.getCompanyId());
                readWrite.put(auth.getConnection(), pool);
            }
            return pool;
        }
    }

    /**
     * Get the DbPool object for read/only connections
     */
    private static DbPool getPoolRO(DbAuth auth) throws Exception {
        synchronized (lock) {
            DbPool pool = readOnly.get(auth.getConnection());
            if (pool == null) {
                pool = new DbPool(auth.getShortName() + "RO", DbConnection.txnAutoCommit, DbConnection.txnIsolation, auth.getUsername(), auth.getPassword(), auth.getConnection(), false, auth.getCompanyId());
                readOnly.put(auth.getConnection(), pool);
            }
            return pool;
        }
    }

    /**
     * Create a database object, read write, for a company's database.
     *
     * Database uses transactions.
     *
     * This does not come from the database pool.
     */
    protected static Db npCompanyDbRW(Db mainDb, String shortName) throws Exception {
        _logger.debug("Returning non-pooled read/write db object for company (" + shortName + ") database.");
        DbAuth auth = getAuth(mainDb, shortName);
        if (auth == null) {
            return null;
        } else {
            return new Db(auth, null, DbConnection.getExternalDatabase(auth.getUsername(), auth.getPassword(), "org.postgresql.Driver", auth.getConnection()), false, true);
        }
    }

    protected static Db npCompanyDbRW(Db mainDb, int companyId) throws Exception {
        _logger.debug("Returning non-pooled read/write db object for company id (" + companyId + ") database.");
        DbAuth auth = getAuth(mainDb, companyId);
        if (auth == null) {
            return null;
        } else {
            return new Db(auth, null, DbConnection.getExternalDatabase(auth.getUsername(), auth.getPassword(), "org.postgresql.Driver", auth.getConnection()), false, true);
        }
    }

    /**
     * Get a database object, read write, for a company's database
     *
     * Database uses transactions.
     */
    protected static Db companyDbRW(Db mainDb, String shortName) throws Exception {
        DbAuth auth = getAuth(mainDb, shortName);
        DbPool pool = null;
        if (auth != null) pool = getPoolRW(auth);
        _logger.debug("Returning pooled read/write db object (" + (pool != null ? pool.getPoolName() : null) + ") for company id (" + shortName + ") database.");
        return (pool == null ? null : pool.getDb());
    }

    protected static Db companyDbRW(Db mainDb, int companyId) throws Exception {
        DbAuth auth = getAuth(mainDb, companyId);
        DbPool pool = null;
        if (auth != null) pool = getPoolRW(auth);
        _logger.debug("Returning pooled read/write db object (" + (pool != null ? pool.getPoolName() : "null") + ") for company id (" + companyId + ") database.");
        return (pool == null ? null : pool.getDb());
    }

    /**
     * Get a database object, read only, for a company's database.
     */
    protected static Db companyDbRO(Db mainDb, String shortName) throws Exception {
        DbAuth auth = getAuth(mainDb, shortName);
        DbPool pool = null;
        if (auth != null) pool = getPoolRO(auth);
        _logger.debug("Returning pooled read/only db object (" + (pool != null ? pool.getPoolName() : null) + ") for company (" + shortName + ") database.");
        return (pool == null ? null : pool.getDb());
    }

    protected static Db companyDbRO(Db mainDb, int companyId) throws Exception {
        DbAuth auth = getAuth(mainDb, companyId);
        DbPool pool = null;
        if (auth != null) pool = getPoolRO(auth);
        _logger.debug("Returning pooled read/only db object (" + (pool != null ? pool.getPoolName() : null) + ") for company (" + companyId + ") database.");
        return (pool == null ? null : pool.getDb());
    }
}
