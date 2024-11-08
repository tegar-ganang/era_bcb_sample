package com.entelience.sql;

import java.sql.DriverManager;
import java.sql.Connection;
import org.apache.log4j.Logger;
import com.entelience.directory.Company;
import com.entelience.util.Config;
import com.entelience.util.Logs;
import com.entelience.util.StaticConfig;

/**
 * Factory class for database connections.
 */
public class DbConnection {

    private DbConnection() {
    }

    private static final Logger _logger = Logs.getLogger();

    public static final int txnIsolation = Connection.TRANSACTION_SERIALIZABLE;

    public static final boolean txnAutoCommit = true;

    /**
     * Get a non-pooled database connection specifying compatible
     * options;
     * - read/write
     * - auto-commit
     * - transaction isolation serializable
     * *DO NOT CALL THIS FROM OUTSIDE THIS CLASS OR FROM UNIT TESTS*
     */
    public static Connection oldDatabaseConnection() throws Exception {
        _logger.info("Creating old style database connection.");
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection(StaticConfig.mainDbConnection, StaticConfig.mainDbUsername, StaticConfig.mainDbPassword);
        c.setReadOnly(false);
        c.setAutoCommit(txnAutoCommit);
        c.setTransactionIsolation(txnIsolation);
        return c;
    }

    /**
     * Get a non-pooled database connection specifying compatible
     * options for external db
     * - read only
     * - auto-commit
     * - transaction isolation serializable
     */
    public static Connection getExternalDatabase(String username, String password, String driver, String url, boolean readonly) throws Exception {
        if (driver == null || "".equals(driver)) throw new Exception("Unspecified JDBC driver class");
        _logger.debug("Class.forName(" + driver + ")");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new Exception("Unable to load JDBC driver class " + driver, e);
        }
        Connection c = DriverManager.getConnection(url, username, password);
        c.setReadOnly(readonly);
        c.setAutoCommit(txnAutoCommit);
        c.setTransactionIsolation(txnIsolation);
        return c;
    }

    public static Connection getExternalDatabase(String username, String password, String driver, String url) throws Exception {
        return getExternalDatabase(username, password, driver, url, false);
    }

    /**
     * Get a database object, read write, for the main database
     * without using the pool.
     *
     * Database uses transactions.
     */
    public static Db npMainDbRW() throws Exception {
        _logger.debug("Returning non-pooled read/write db object for main database.");
        return new Db(new DbAuth(null, StaticConfig.mainDbUsername, StaticConfig.mainDbPassword, StaticConfig.mainDbConnection, 0), null, oldDatabaseConnection(), false, true);
    }

    /**
     * Get a database object, read write, for the main database
     *
     * Database uses transactions.
     */
    public static Db mainDbRW() throws Exception {
        return StaticConnections.getMainDbRW();
    }

    /**
     * Get a database object, read only, for the main database.
	 * Do not forget to call safeClose() on it.
     */
    public static Db mainDbRO() throws Exception {
        return StaticConnections.getMainDbRO();
    }

    public static Db npCompanyDbRW(String shortName) throws Exception {
        Db ro = null;
        try {
            ro = mainDbRO();
            return npCompanyDbRW(ro, shortName);
        } finally {
            if (ro != null) ro.safeClose();
        }
    }

    public static Db npCompanyDbRW(Db mainDb, String shortName) throws Exception {
        Db db = DynamicConnections.npCompanyDbRW(mainDb, shortName);
        if (db == null) throw new Exception("Unable to get a database npRW connection to company (" + shortName + ")");
        return db;
    }

    public static Db npCompanyDbRW(int company_id) throws Exception {
        Db ro = null;
        try {
            ro = mainDbRO();
            return npCompanyDbRW(ro, company_id);
        } finally {
            if (ro != null) ro.safeClose();
        }
    }

    public static Db npCompanyDbRW(Db mainDb, int company_id) throws Exception {
        Db db = DynamicConnections.npCompanyDbRW(mainDb, company_id);
        if (db == null) throw new Exception("Unable to get a database npRW connection to company (" + company_id + ")");
        return db;
    }

    public static Db companyDbRW(String shortName) throws Exception {
        Db ro = null;
        try {
            ro = mainDbRO();
            return companyDbRW(ro, shortName);
        } finally {
            if (ro != null) ro.safeClose();
        }
    }

    public static Db companyDbRW(Db mainDb, String shortName) throws Exception {
        Db db = DynamicConnections.companyDbRW(mainDb, shortName);
        if (db == null) throw new Exception("Unable to get a database RW connection to company (" + shortName + ")");
        return db;
    }

    public static Db companyDbRW(int company_id) throws Exception {
        Db ro = null;
        try {
            ro = mainDbRO();
            return companyDbRW(ro, company_id);
        } finally {
            if (ro != null) ro.safeClose();
        }
    }

    public static Db companyDbRW(Db mainDb, int company_id) throws Exception {
        Db db = DynamicConnections.companyDbRW(mainDb, company_id);
        if (db == null) throw new Exception("Unable to get a database RW connection to company (" + company_id + ")");
        return db;
    }

    public static Db companyDbRO(String shortName) throws Exception {
        Db ro = null;
        try {
            ro = mainDbRO();
            return companyDbRO(ro, shortName);
        } finally {
            if (ro != null) ro.safeClose();
        }
    }

    protected static Db companyDbRO(Db mainDb, String shortName) throws Exception {
        Db db = DynamicConnections.companyDbRO(mainDb, shortName);
        if (db == null) throw new Exception("Unable to get a database RW connection to company (" + shortName + ")");
        return db;
    }

    public static Db companyDbRO(int company_id) throws Exception {
        Db ro = null;
        try {
            ro = mainDbRO();
            return companyDbRO(ro, company_id);
        } finally {
            if (ro != null) ro.safeClose();
        }
    }

    protected static Db companyDbRO(Db mainDb, int company_id) throws Exception {
        Db db = DynamicConnections.companyDbRO(mainDb, company_id);
        if (db == null) throw new Exception("Unable to get a database RO connection to company (" + company_id + ")");
        return db;
    }

    /**
	 * Returns a database handler to the default company db
	 */
    public static Db defaultCieDbRO() throws Exception {
        Integer cieId = Company.getDefaultCompany();
        if (cieId == null) {
            _logger.warn("Cannot establish a RO connection to a default company (" + cieId + ") unknown");
            return null;
        }
        return companyDbRO(cieId);
    }

    public static Db defaultCieDbRW() throws Exception {
        Integer cieId = Company.getDefaultCompany();
        if (cieId == null) {
            _logger.warn("Cannot establish a RW connection to a default company (" + cieId + ") unknown");
            return null;
        }
        return companyDbRW(cieId);
    }
}
