package com.cred.industries.platform.database;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * uses org.apache.commons.dbcp to create a Db connection pool
 * to use across the program.
 */
public final class DBConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(DBConnectionManager.class);

    private static final DBConnectionManager sInstance = new DBConnectionManager();

    private DataSource mDataSource;

    /**
	 * returns the singleton connection manager or creates and returns one if
	 * it does not exist. 
	 * @return singleton instance of DBConnectionManager
	 */
    public static DBConnectionManager getInstance() {
        return sInstance;
    }

    /**
	 * creates the DBConnectionManager
	 */
    private DBConnectionManager() {
        DBConfig dbConfig = new DBConfig();
        createDataSources(dbConfig.getDBDriverName(), dbConfig.getDBURI(), dbConfig.getDBUserName(), dbConfig.getDBPassword(), dbConfig.getDBPoolMinCon(), dbConfig.getDBPoolMaxCon());
    }

    /**
	 * Creates the db connection pool
	 * @param driverName name of the SQL driver
	 * @param uri path to connect to the SQl DB
	 * @param dbUserName user name to connect to the DB with
	 * @param dbPassword password for the user name to connect to the DB
	 * @param minCon min number of connections to keep alive with DB
	 * @param maxConn maximum number of connections to make with the DB
	 */
    private void createDataSources(String driverName, String uri, String dbUserName, String dbPassword, int minCon, int maxConn) {
        logger.info("initializing database connection manager");
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            logger.error("unable to load DB driver, game over");
            return;
        }
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(uri, dbUserName, dbPassword);
        GenericObjectPool readwriteConnectionPool = new GenericObjectPool(null);
        readwriteConnectionPool.setMinIdle(minCon);
        readwriteConnectionPool.setMaxActive(maxConn);
        @SuppressWarnings("unused") PoolableConnectionFactory readwritePoolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, readwriteConnectionPool, null, null, false, true);
        PoolingDataSource readwriteDataSource = new PoolingDataSource(readwriteConnectionPool);
        mDataSource = readwriteDataSource;
    }

    /**
	 * gets the data source for the connection manager
	 * @return the data source for the connection manager
	 */
    public DataSource getDataSource() {
        return mDataSource;
    }

    /**
	 * gets a connection from the pool
	 * @return gets a connection from the pool
	 * @throws SQLException
	 */
    public Connection getConnection() throws SQLException {
        return mDataSource.getConnection();
    }
}
