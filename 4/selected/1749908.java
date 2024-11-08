package net.sf.jrelay.connect;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import net.sf.jrelay.adaptor.JDBCAdaptor;
import net.sf.jrelay.adaptor.JrAdaptor;
import net.sf.jrelay.common.JrConstants;
import net.sf.jrelay.config.JrConfig;
import net.sf.jrelay.exception.JrConfigException;
import net.sf.jrelay.exception.JrConnectException;
import net.sf.jrelay.exception.JrCouldNotCreateTypeMapException;
import net.sf.jrelay.exception.JrException;

public final class JrConnector implements JrConstants {

    private static Logger sLogger = Logger.getLogger(JrConnector.class.getName());

    /**
     * The adaptor-class-name out of the jrelay.properties file.
     * There may exist one adaptor for every database type, p.e. Ora9,JDataStore7,Sybase,...
     */
    protected static JrAdaptor sAdaptor = null;

    /**
     *The data-source-name of a database-pool. if it is null,
     *jrelay tries to use jdbc. 
     */
    protected String mDataSourceName = null;

    /**
     * If no pool is available, jrelay uses plain jdbc.
     * @see jrelay.properties for the parameters. 
     */
    protected JrConnection mJdbcConnection = null;

    /**
     * singleton data: connection to the database instance;
     * adminDriver, adminUsr, adminPwd are needed to initialize adminCon
     */
    private static JrConnector sInstance = new JrConnector();

    /**
     * <code>firstCallGetConnection</code>
     * This is only a flag, marking, if getConnection() was called the first time.
     * After the first call, this flag will be set down to false.
     * This flag is only used to create an info message.
     */
    protected static boolean firstCallGetConnection = true;

    /**
     * The dataSource singleton
     */
    protected static DataSource sDataSource = null;

    /**
     * Refresh dataSource, if set to null or sRefreshDataSource = true 
     */
    protected static boolean sRefreshDataSource = false;

    /**
     * Debugging counter 
     */
    protected static int sConnectionCounter = 0;

    /**
     * Is this thread running inside a (tomcat)-server?
     * After the first request, the value will be stored here.
     * The variable will be used by the isServerSide method.
     * states:  null - not yet initialised
     * 			true - we run inside a server
     * 			false - there was no server at the first call 
     */
    protected static Boolean sRunningInsideServer = null;

    /**
     * DbConnection constructor is private because of singleton pattern.
     */
    private JrConnector() {
    }

    public static JrConnector getInstance() {
        return sInstance;
    }

    /**
     * DbConnection now uses BEA-Oracle-Pool.<br>
     * This function returns a connection object from this pool.<br>
     * If pool is not available or pool cannot return a connection, the method 
     * will try to use plain jdbc connection and return it.
     * Jdbc-Connections will be singletons and running until close().
     * <b>Use Connection.close() after using this connection!!!!!</b><br>
     * The connections autocommit will be set to false!!
     * @return java.sql.Connection    a valid connection or an exception is thrown<br>
     * @exception java.lang.NullPointerException    couldn't get a connection object from BEA<br>
     */
    public static java.sql.Connection getConnection() throws JrConnectException {
        return getInstance().getDbConnection();
    }

    /**
     * @return
     * @throws JrConnectException
     * @see net.sf.jrelay.adaptorJrConnector#getConnection()
     */
    protected java.sql.Connection getDbConnection() throws JrConnectException {
        Connection jCon = null;
        String confDataSourceName = JrConfig.getInstance().getProperty(DATASOURCE_PROPERTY);
        boolean useThreadTx = getThreadTxProperty();
        boolean useMultipleConnections = getMultipleConnectionsProperty();
        if (useMultipleConnections) {
            if (firstCallGetConnection) {
                sLogger.info("Will use pool local connections from datasource " + confDataSourceName + ".");
            }
            if (useThreadTx) {
                jCon = getThreadsConnection();
            } else {
                jCon = getJdbcOrPoolConnection();
            }
        } else {
            synchronized (JrConnector.class) {
                try {
                    if ((mJdbcConnection == null) || (mJdbcConnection.isClosed())) {
                        mJdbcConnection = null;
                        Connection con2 = getJdbcOrPoolConnection();
                        mJdbcConnection = new JrConnection(con2, true);
                    }
                } catch (SQLException e) {
                    sLogger.log(Level.SEVERE, "Error while creating JDBC-connection!", e);
                    throw new JrConnectException("Error while creating JDBC-connection!", e);
                }
                jCon = mJdbcConnection;
            }
        }
        firstCallGetConnection = false;
        return jCon;
    }

    /**
     * Get a connection from pool.
     * @param initialContextName if set, the method tries to retrieve datasource out of this context.
     * This parameter may be null, the method will retrieve the datasource from the default context.
     * @param dataSourceName The jndi-name of the datasource to get a connection from
     * @return a Connection object. In case of errors, an exception will be thrown.
     * @throws JrConnectException any problems while establishing a connection from pool
     */
    protected java.sql.Connection getPoolConnection() throws JrConnectException {
        java.sql.Connection con = null;
        javax.sql.DataSource ds = null;
        String dataSourceName = JrConfig.getInstance().getProperty(DATASOURCE_PROPERTY);
        String initialContextName = JrConfig.getInstance().getProperty(JRELAY_INITIAL_CONTEXT);
        try {
            ds = getDataSource(initialContextName, dataSourceName);
            con = ds.getConnection();
            con.setAutoCommit(false);
            if (firstCallGetConnection) {
                sLogger.info("Using DataSource " + dataSourceName);
            } else {
                sLogger.fine("Using DataSource " + dataSourceName);
            }
        } catch (SQLException e) {
            sRefreshDataSource = true;
            throw new JrConnectException(e);
        } catch (RuntimeException e) {
            sRefreshDataSource = true;
            throw new JrConnectException(e);
        }
        return con;
    }

    /**
     * Get the DataSource of the connection pool.
     * If the first access works, the dataSource will be cached as singleton,
     * and all following request will only get the cached dataSource.
     * @param initialContextName a name of an optional initial context. will
     * be searched in the default initial context.
     * @param dataSourceName the jndi-name of the dataSource
     * @return the datasource object itself. if any error occurs, you will not get null,
     * but an exception
     * @throws JrConnectException any errors while try to get the dataSource 
     */
    protected synchronized DataSource getDataSource(String initialContextName, String dataSourceName) throws JrConnectException {
        if (sRefreshDataSource) {
            sRefreshDataSource = false;
            sDataSource = null;
        }
        if (sDataSource == null) {
            try {
                Context envContext = null;
                DataSource ds = null;
                Context initContext = new InitialContext();
                if (initContext == null) {
                    throw new JrConnectException("Could not fetch initial context!");
                }
                if ((initialContextName != null) && (!"".equals(initialContextName.trim()))) {
                    sLogger.fine("Will use initialContext " + initialContextName + " for datasource-lookup.");
                    Object envContextObj = initContext.lookup(initialContextName);
                    if ((envContextObj == null) || (!Context.class.isAssignableFrom(envContextObj.getClass()))) {
                        sLogger.severe("Initial context object is not Context but:" + envContextObj.getClass() + "," + envContextObj);
                        sLogger.info("try to use initialContext...");
                        envContext = initContext;
                    } else {
                        envContext = (Context) envContextObj;
                    }
                } else {
                    envContext = initContext;
                }
                if (envContext == null) {
                    throw new JrConnectException("Could not fetch environment context!");
                }
                Object dataSourceObj = envContext.lookup(dataSourceName);
                if ((dataSourceObj == null) || (!javax.sql.DataSource.class.isAssignableFrom(dataSourceObj.getClass()))) {
                    throw new JrConnectException("Could not find datasource " + dataSourceName + " while lookup!");
                }
                ds = (javax.sql.DataSource) dataSourceObj;
                sDataSource = ds;
            } catch (NamingException e) {
                throw new JrConnectException("Could not find datasource " + dataSourceName + " while lookup!");
            }
        }
        return sDataSource;
    }

    /**
     * This method returns a String that represents the value of this object.
     * Creation date: (03.04.01 13:30:16)
     * @return java.lang.String a string representation of the receiver
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        Connection dbCon = null;
        s.append(getClass() + ": \n");
        try {
            dbCon = getConnection();
            s.append("AutoCommit=" + dbCon.getAutoCommit());
            s.append("IsolationLevel" + dbCon.getTransactionIsolation());
        } catch (SQLException e) {
            s.append("Can not get connection settings!!!" + e.getMessage());
        } catch (JrConnectException e) {
            s.append("Can not get connection settings!!!" + e.getMessage());
        } finally {
            try {
                if (dbCon != null) {
                    dbCon.close();
                }
            } catch (SQLException e) {
            }
        }
        s.append("\n");
        return s.toString();
    }

    /**
     * Get a new object id from a sequence.
     * @param sequenceName the sequence, which shall create the id
     * @return the int value of the new object id
     * @throws JrConnectException could not get a connection
     * @throws JrCouldNotCreateTypeMapException sql-problems with the statement, ...
     */
    public static long getId(String sequenceName) throws JrConnectException, JrCouldNotCreateTypeMapException {
        return getInstance().getSequenceId(sequenceName);
    }

    /**
     * Get a new object id from a sequence.
     * @param sequenceName the sequence, which shall create the id
     * @return the int value of the new object id
     * @throws JrConnectException could not get a connection
     * @throws JrCouldNotCreateTypeMapException sql-problems with the statement, ...
     */
    public long getSequenceId(String sequenceName) throws JrConnectException, JrCouldNotCreateTypeMapException {
        long id;
        Connection dbCon = null;
        StringBuffer str = new StringBuffer();
        ResultSet resultSet = null;
        Statement stmt = null;
        try {
            dbCon = getConnection();
            str.append("select ");
            str.append(sequenceName);
            str.append(".nextval from dual");
            sLogger.fine("SqlStatement: " + str);
            dbCon = getConnection();
            stmt = dbCon.createStatement();
            resultSet = stmt.executeQuery(str.toString());
            resultSet.next();
            id = resultSet.getLong(1);
            return id;
        } catch (SQLException e) {
            throw new JrCouldNotCreateTypeMapException(e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e1) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e1) {
                }
            }
            if (dbCon != null) {
                try {
                    dbCon.close();
                } catch (SQLException e1) {
                }
                dbCon = null;
            }
        }
    }

    /**
     * Establish a (really) new JDBC-Connection by using jrelay.properties
     * and give it back.
     * Also the method sets autocommit of the new connection=false!!!
     * @return a valid connection
     * @throws SQLException any problems out of the database
     * @throws JrConfigException any configuration problem,i.e. database-adaptor not found, 
     * 	other mandatory properties not found, ...
     */
    public synchronized Connection establishJdbcConnection() throws SQLException, JrConfigException {
        Connection con = null;
        String url;
        String usr;
        String pwd;
        String driverClassName = "";
        Driver driver;
        try {
            JrConfig conf = JrConfig.getInstance();
            driverClassName = conf.getProperty2(JDBC_DRIVER_CLASS_PROPERTY);
            Class cl = Class.forName(driverClassName);
            driver = (Driver) cl.newInstance();
            DriverManager.registerDriver(driver);
            pwd = conf.getProperty2(DB_PWD_PROPERTY);
            usr = conf.getProperty2(DB_USR_PROPERTY);
            url = conf.getProperty2(DB_URL_PROPERTY);
            sLogger.info("Try to connect to db:" + driver.getClass() + ',' + url + ',' + usr);
            con = DriverManager.getConnection(url, usr, pwd);
            con.setAutoCommit(false);
            return con;
        } catch (ClassNotFoundException e) {
            throw new JrConfigException("Could not load Sql-Driver class [" + driverClassName + "].", e);
        } catch (InstantiationException e) {
            throw new JrConfigException("Could not load Sql-Driver class [" + driverClassName + "].", e);
        } catch (IllegalAccessException e) {
            throw new JrConfigException("Could not load Sql-Driver class [" + driverClassName + "].", e);
        }
    }

    /**
     * Get the specialised adaptor, defined by configuration
     * @return
     */
    public static synchronized JrAdaptor getAdaptor() throws JrConfigException {
        try {
            if (sAdaptor == null) {
                String adaptorClassName = JrConfig.getInstance().getProperty2(DB_ADAPTORCLASS_PROPERTY);
                sLogger.info("The following adaptor class will be used: " + adaptorClassName);
                Class cl = Class.forName(adaptorClassName);
                sAdaptor = (JrAdaptor) cl.newInstance();
            }
        } catch (ClassNotFoundException e) {
            throw new JrConfigException("No valid jrelay-adaptor-class found (a class implementing DbAdaptor):", e);
        } catch (InstantiationException e) {
            throw new JrConfigException("No valid jrelay-adaptor-class found (a class implementing DbAdaptor):", e);
        } catch (IllegalAccessException e) {
            throw new JrConfigException("No valid jrelay-adaptor-class found (a class implementing DbAdaptor):", e);
        }
        return sAdaptor;
    }

    /**
     * Commit recent changes
     * @throws JrException
     */
    public static void commit() throws JrException {
        Connection sqlConnection = null;
        try {
            if ((!hasThreadLocalConnection()) || ((hasThreadLocalConnection() && (threadConnectionAlreadyUsed())))) {
                sqlConnection = getInstance().getDbConnection();
                sqlConnection.commit();
            } else {
                sLogger.info("Commit omitted, because there was not database access.");
            }
        } catch (SQLException e) {
            throw new JrConnectException(e);
        } finally {
            if (sqlConnection != null) {
                try {
                    sqlConnection.close();
                } catch (SQLException e1) {
                }
            }
        }
    }

    /**
     * Rollback recent changes
     * @throws JrException
     */
    public static void rollback() throws JrException {
        Connection sqlConnection = null;
        try {
            if ((!hasThreadLocalConnection()) || ((hasThreadLocalConnection() && (threadConnectionAlreadyUsed())))) {
                sqlConnection = getInstance().getDbConnection();
                sqlConnection.rollback();
            } else {
                sLogger.info("Rollback omitted, because there was not database access.");
            }
        } catch (SQLException e) {
            throw new JrConnectException(e);
        } finally {
            if (sqlConnection != null) {
                try {
                    sqlConnection.close();
                } catch (SQLException e1) {
                }
            }
        }
    }

    private static class ThreadLocalConnectionManager extends ThreadLocal {

        private static ThreadLocalConnectionManager sInstance = new ThreadLocalConnectionManager();

        public static ThreadLocalConnectionManager getInstance() {
            return sInstance;
        }

        private ThreadLocalConnectionManager() {
        }

        public void set(Object value) {
            super.set(value);
            String conStr = value == null ? "NULL" : value.toString();
            sLogger.fine("Set thread-connection " + conStr + " to thread " + Thread.currentThread().getName());
        }
    }

    /**
     * Release the connection, but only, if it is a thread-local-
     * connection.
     * If the connections is a singleton (not bound to a thread), this
     * method will do nothing.
     */
    public static void releaseThreadConnection() {
        if (getInstance().isServerSide()) {
            Object con = null;
            con = ThreadLocalConnectionManager.getInstance().get();
            if (con != null) {
                ThreadLocalConnectionManager.getInstance().set(null);
                try {
                    if (JrConnector.class.isAssignableFrom(con.getClass())) {
                        ((JrConnection) con).releaseExplicite();
                    } else {
                        ((Connection) con).close();
                    }
                } catch (SQLException e) {
                    sLogger.log(Level.WARNING, e.getMessage(), e);
                }
                sLogger.fine("Released connection in thread:" + Thread.currentThread().getName());
            } else {
                sLogger.fine("Warn: Tx-Connection to close is already NULL!!! " + "Reasons: Tx was not used by any read or write access or you called release twice! " + "Thread:" + Thread.currentThread().getName());
            }
        } else {
            sLogger.fine("ReleaseThreadConnection ignored, because having singleton JDBC-Connection.");
        }
    }

    /**
     * Try to get the threads connection.
     * Will call getJdbcOrPoolConnection inside!
     * @return the connection or null, if no connection is set in the thread or 
     * connection is closed.
    * @throws JrConnectException 
     */
    protected JrConnection getThreadsConnection() throws JrConnectException {
        JrConnection jCon = null;
        Object con = ThreadLocalConnectionManager.getInstance().get();
        if (con != null) {
            jCon = (JrConnection) con;
            try {
                if (jCon.isClosed()) {
                    sLogger.info("Existing thread-connection is closed, create new one." + jCon + " Thread:" + Thread.currentThread().getName());
                    jCon = null;
                }
            } catch (SQLException e) {
                jCon = null;
            }
        }
        if (jCon != null) {
            sLogger.fine("Re-use thread-connection " + jCon + " Thread:" + Thread.currentThread().getName());
        }
        if (jCon == null) {
            Connection con1 = getJdbcOrPoolConnection();
            jCon = new JrConnection(con1, true);
            jCon.setThreadConnection(true);
            incrementConnectionCounter();
            ThreadLocalConnectionManager.getInstance().set(jCon);
        }
        return jCon;
    }

    /**
     * Test, if an initial context exists and if the JrConfigured pool can be accessed.
     * If not, we are not running inside a server or the pool is not available.
     * @param initialContextName
     * @return true: running inside a server with initial context; false: running locally
     */
    protected boolean isServerSide() {
        boolean isServerSide = false;
        String initialContextName = JrConfig.getInstance().getProperty(JRELAY_INITIAL_CONTEXT);
        try {
            if ((initialContextName != null) && (!"".equals(initialContextName.trim()))) {
                Connection con1 = getPoolConnection();
                if (con1 != null) {
                    isServerSide = true;
                    try {
                        con1.close();
                    } catch (SQLException e) {
                        sLogger.log(Level.WARNING, "Cannot close test connection in isServerSide():", e);
                    }
                }
            }
        } catch (JrConnectException e) {
            isServerSide = false;
            sLogger.fine("isServerSide=false because:" + e.getMessage());
        }
        return isServerSide;
    }

    /**
     * Has the current thread a thread-local connection or
     * is the current connection global?
     * @return true: the current thread has an own connection
     * false: the used connection is global
     */
    public static boolean hasThreadLocalConnection() {
        boolean has = false;
        if (ThreadLocalConnectionManager.getInstance().get() != null) {
            has = true;
        }
        return has;
    }

    static int incrementConnectionCounter() {
        synchronized (JrConnector.class) {
            sConnectionCounter++;
        }
        sLogger.fine("###connectionCounter = " + sConnectionCounter + " (++) Thread:" + Thread.currentThread().getName());
        return sConnectionCounter;
    }

    static synchronized int decrementConnectionCounter() {
        synchronized (JrConnector.class) {
            sConnectionCounter--;
        }
        sLogger.fine("###connectionCounter = " + sConnectionCounter + " (--) Thread:" + Thread.currentThread().getName());
        return sConnectionCounter;
    }

    protected Connection getJdbcOrPoolConnection() throws JrConnectException {
        String confDataSourceName = JrConfig.getInstance().getProperty(DATASOURCE_PROPERTY);
        String confFallbackToJdbc = JrConfig.getInstance().getProperty(JRELAY_FALLBACK_TO_JDBC_ALLOWED, "true");
        boolean canFallbackToJdbc = !("false".equals(confFallbackToJdbc.toLowerCase()));
        Connection con1 = null;
        boolean dataSourceJrConfigured = false;
        if ((confDataSourceName != null) && (!"".equals(confDataSourceName.trim()))) {
            dataSourceJrConfigured = true;
        }
        if (!dataSourceJrConfigured && !canFallbackToJdbc) {
            throw new JrConnectException("Datasource not JrConfigured and access via JDBC not allowed!");
        }
        try {
            if (dataSourceJrConfigured) {
                con1 = getPoolConnection();
                incrementConnectionCounter();
            }
        } catch (JrConnectException e) {
            con1 = null;
            if (canFallbackToJdbc) {
                sLogger.log(Level.WARNING, "Could not get pool connection, try to establish JDBC-connection!" + e.getMessage(), e);
            } else {
                sLogger.log(Level.SEVERE, "Could not get pool connection!" + e.getMessage(), e);
                throw new JrConnectException("Datasource not accessible and fallback to JDBC not allowed!" + e.getMessage(), e);
            }
        }
        if ((con1 == null) && (canFallbackToJdbc)) {
            try {
                Connection con2 = establishJdbcConnection();
                con1 = con2;
            } catch (SQLException e) {
                sLogger.log(Level.SEVERE, "Error while creating JDBC-connection!", e);
                throw new JrConnectException("Error while creating JDBC-connection!", e);
            } catch (JrConfigException e) {
                sLogger.log(Level.SEVERE, "Error while creating JDBC-connection!", e);
                throw new JrConnectException("Error while creating JDBC-connection!", e);
            }
        }
        firstCallGetConnection = false;
        return con1;
    }

    protected boolean getThreadTxProperty() {
        Boolean use = null;
        String useThreadTxProp = JrConfig.getInstance().getProperty(JRELAY_USE_THREAD_TX);
        if ((useThreadTxProp != null) && ("true".equals(useThreadTxProp.toLowerCase()))) {
            use = Boolean.TRUE;
        }
        if ((useThreadTxProp != null) && ("false".equals(useThreadTxProp.toLowerCase()))) {
            use = Boolean.FALSE;
        }
        if (use == null) {
            String dataSourceNameProp = JrConfig.getInstance().getProperty(DATASOURCE_PROPERTY);
            if (isServerSide() && (dataSourceNameProp != null) && (!"".equals(dataSourceNameProp.trim()))) {
                use = Boolean.TRUE;
            } else {
                use = Boolean.FALSE;
            }
        }
        return use.booleanValue();
    }

    protected boolean getMultipleConnectionsProperty() {
        Boolean use = null;
        String useMultipleConnectionsProp = JrConfig.getInstance().getProperty(JRELAY_USE_MULTIPLE_CONNECTIONS);
        if ((useMultipleConnectionsProp != null) && ("true".equals(useMultipleConnectionsProp.toLowerCase()))) {
            use = Boolean.TRUE;
        }
        if ((useMultipleConnectionsProp != null) && ("false".equals(useMultipleConnectionsProp.toLowerCase()))) {
            use = Boolean.FALSE;
        }
        if (use == null) {
            String dataSourceNameProp = JrConfig.getInstance().getProperty(DATASOURCE_PROPERTY);
            if (isServerSide() && (dataSourceNameProp != null) && (!"".equals(dataSourceNameProp.trim()))) {
                use = Boolean.TRUE;
            } else {
                use = Boolean.FALSE;
            }
        }
        return use.booleanValue();
    }

    public static boolean threadConnectionAlreadyUsed() {
        boolean alreadyUsed = false;
        Object con = ThreadLocalConnectionManager.getInstance().get();
        if (con != null) {
            JrConnection jCon = (JrConnection) con;
            try {
                if (!jCon.isClosed()) {
                    alreadyUsed = true;
                }
            } catch (SQLException e) {
                alreadyUsed = false;
            }
        }
        sLogger.info("Thread already used " + (alreadyUsed ? "a" : "NO") + " connection:" + Thread.currentThread().getName());
        return alreadyUsed;
    }

    /**
     * @see JDBCAdaptor#closeJdbcObjects(Connection, Statement, ResultSet)
     * @param con can be null
     * @param stmt can be null
     * @param rs can be null
     */
    public static void closeJdbcObjects(Connection con, Statement stmt, ResultSet rs) {
        JDBCAdaptor.closeJdbcObjects(con, stmt, rs);
    }
}
