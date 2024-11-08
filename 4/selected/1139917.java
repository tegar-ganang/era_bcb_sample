package com.entelience.sql;

import org.apache.log4j.Logger;
import com.entelience.util.Logs;
import com.entelience.util.StaticConfig;

/**
 * Factory class for database connections.
 *
 * Static connection pools - the main database (read only and read write)
 * These methods should only be called via the public methods in DbConnection.
 *
 * They have been moved from DbConnection as we may not always wish to initialise
 * the connection pools.
 */
public class StaticConnections {

    private StaticConnections() {
    }

    private static final Logger _logger = Logs.getLogger();

    private static final DbPool poolMainRW;

    private static final DbPool poolMainRO;

    static {
        DbPool _poolMainRW = null;
        DbPool _poolMainRO = null;
        try {
            _logger.info("Initializing the database static connection pool");
            _poolMainRW = new DbPool("RWmain", DbConnection.txnAutoCommit, DbConnection.txnIsolation, StaticConfig.mainDbUsername, StaticConfig.mainDbPassword, StaticConfig.mainDbConnection, true, -1);
            _poolMainRO = new DbPool("ROmain", DbConnection.txnAutoCommit, DbConnection.txnIsolation, StaticConfig.mainDbUsername, StaticConfig.mainDbPassword, StaticConfig.mainDbConnection, false, -1);
        } catch (Exception e) {
            _logger.debug("Hiding exception during connection pool initialisation (" + e + ")");
            _poolMainRW = null;
            _poolMainRO = null;
        }
        poolMainRW = _poolMainRW;
        poolMainRO = _poolMainRO;
        _logger.info("The static connection pool is initialized");
    }

    /**
     * Get a database object, read write, for the main database
     * Call this from DbConnection only.
     *
     * Database uses transactions.
     */
    public static Db getMainDbRW() throws Exception {
        _logger.debug("Returning pooled read/write db object for main database.");
        return poolMainRW.getDb();
    }

    /**
     * Get a database object, read only, for the main database.
     * Call this from DbConnection only.
     */
    public static Db getMainDbRO() throws Exception {
        _logger.debug("Returning pooled read/only db object for main database.");
        return poolMainRO.getDb();
    }
}
