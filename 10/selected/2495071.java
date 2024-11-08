package bbd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This loads the driver and creates a connection.
 * 
 * This encapsulates the JDBC API and transforms the results to objects that are
 * independant of the JDBC and the database schema.
 * 
 * 
 * @param B
 *            any POJO bean
 * @param L
 *            lists of beans
 * @author james gamber
 */
class BBDBeanConnection<B, L extends BBDBeanArrayList<B>> implements IBBDBeanConnection<B, L> {

    /**
     * String value for boolean 'true'.
     */
    private static final String TRUE = "true";

    /** Database JDBC driver class name */
    private static Class driverClass = null;

    /** Timer to periodically refresh cached SQL */
    protected static final Timer refreshSQL = new Timer();

    /** Database where SQL is stored */
    protected static final String BBD = BBDProperties.get(BBDProperties.BBDDatabase);

    /**
     * Cache of SQL execute statements The saves selecting on the
     * bbd.storedprocedure table after the first time. Caches are cleared
     * periodically by the BBDRefreshTimer.
     */
    protected static final Map<String, String> apiMap = Collections.synchronizedMap(new HashMap<String, String>());

    /**
     * Cache of PreparedStatement Meta Data Using cached data saves round trips
     * through the network to the database. Caches are cleared periodically by
     * the BBDRefreshTimer.
     */
    private static final Map<String, BBDParameterMetaData[]> pmdMap = Collections.synchronizedMap(new HashMap<String, BBDParameterMetaData[]>());

    /** Refresh time period in milliseconds */
    protected static final long refreshPeriod = new Long(BBDProperties.get(BBDProperties.RefreshCachePeriod));

    /** Logger */
    private static final Logger log = Logger.getLogger(BBDBeanConnection.class.getName());

    static {
        log.setLevel(Level.WARNING);
    }

    /**
     * Principal used to access the BBD database tables
     *
     * Package private to use in the BBD layer
     */
    static final BBDAPIPrincipal bbdDBPrincipal = new BBDAPIPrincipal(BBDProperties.get(BBDProperties.BBDUser), BBDProperties.get(BBDProperties.BBDPassword));

    private static final boolean bbdPrincipalIsDefault = TRUE.equalsIgnoreCase(BBDProperties.get(BBDProperties.BBDPrincipalIsDefault)) ? Boolean.TRUE : Boolean.FALSE;

    private static final Map<BBDAPIPrincipal, Connection> conns = Collections.synchronizedMap(new HashMap<BBDAPIPrincipal, Connection>());

    static {
        refreshSQL.scheduleAtFixedRate(new BBDRefreshTimerTask(), refreshPeriod, refreshPeriod);
    }

    /**
     * If you find that a connection is failing, it could be a lot of things,
     * such as a timeout of the connection.
     *
     * In order to remove the connection from the conns Map, disconnect the
     * connection.
     *
     * The you can return the database operation and a new connection will be
     * generated.
     *
     * @param logonPrincipal  user/pw credential
     * 
     */
    public static void disconnect(BBDAPIPrincipal logonPrincipal) {
        Connection conn = conns.get(logonPrincipal);
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException ex) {
            logExceptions(ex);
        }
        conns.remove(logonPrincipal);
    }

    static void clear() {
        apiMap.clear();
        pmdMap.clear();
        Set<Entry<BBDAPIPrincipal, Connection>> entries = conns.entrySet();
        for (Entry<BBDAPIPrincipal, Connection> entry : entries) {
            Connection conn = entry.getValue();
            try {
                conn.close();
            } catch (SQLException ex) {
                logExceptions(ex);
            }
        }
        conns.clear();
    }

    /** User Principal for Connection to database */
    private BBDAPIPrincipal currentPrinc = null;

    /** Creates a new instance of MyConnection */
    public BBDBeanConnection() {
    }

    /**
     * Used in this class to get a connection.
     *
     * This class is single threaded and contains one connection per instance.
     *
     * These connections are expensive, but in app servers (etc), a datasource
     * and a connection pool would be used.
     *
     * @param principal
     *            used to connect to database
     * @return JDBC Connection or throw exception if connection fails
     * @throws java.sql.SQLException
     */
    protected Connection getConnection(final BBDAPIPrincipal principal) throws SQLException {
        if (principal == null) {
            final SQLException e = new SQLException("specific database connection logon is required, but not supplied");
            e.fillInStackTrace();
            final StackTraceElement[] ste = e.getStackTrace();
            final StringBuffer sb = new StringBuffer("\n");
            for (final StackTraceElement st : ste) {
                sb.append(st.toString());
                sb.append("\n");
            }
            log.severe(e.getMessage() + "\n" + sb.toString());
            throw e;
        }
        Connection con = conns.get(principal);
        if (con != null) {
            if (currentPrinc == null) {
                currentPrinc = principal;
                principal.incrementUsage();
            }
            return con;
        }
        if (driverClass == null) {
            try {
                driverClass = Class.forName(BBDProperties.get(BBDProperties.DriverClass));
            } catch (final java.lang.ClassNotFoundException e) {
                log.severe(e.getMessage());
                throw new SQLException("Driver class not found " + BBDProperties.get(BBDProperties.DriverClass));
            }
        }
        try {
            String connectionString = BBDProperties.get(BBDProperties.BBDConnectionString);
            if (principal != null && !principal.equals(bbdDBPrincipal) && !bbdPrincipalIsDefault) {
                connectionString = BBDProperties.get(BBDProperties.UserConnectionString);
            }
            connectionString = String.format(connectionString, principal.getName(), principal.getPassword());
            con = DriverManager.getConnection(connectionString);
            conns.put(principal, con);
            currentPrinc = principal;
        } catch (final SQLException ex) {
            logExceptions(ex);
            throw ex;
        }
        return con;
    }

    /**
     * Execute the specified black box database API.
     *
     * @param storedProcedure
     *            BBDAPI contains storedProcure name, database, and deprecated
     *            status.
     * @param params
     *            optional parameters to place in stored procedure call that
     *            contains '?'. This is used with parameterized SQL suce sas
     *            call HelloWord2("Duke");
     * @return List an Array List of rows from the result set of the query.
     * @throws java.sql.SQLException
     *             JDBC thrown exception
     */
    @Override
    public L executeQuery(final IBBDBeanAPI storedProcedure, final Object... params) throws SQLException {
        ResultSet rs = null;
        Statement stmt = null;
        L sqlAL = (L) new BBDBeanArrayList<B>();
        BBDAPIPrincipal principal = (BBDAPIPrincipal) ((BBDBeanAPI) storedProcedure).getBbdPrincipal();
        try {
            if (params.length == 0) {
                stmt = getConnection(principal).createStatement();
                rs = stmt.executeQuery(getSQL(storedProcedure));
                logWarnings(stmt.getWarnings());
            } else {
                final PreparedStatement pstmt = getConnection(principal).prepareStatement(getSQL(storedProcedure));
                insertArguments(storedProcedure, params, pstmt);
                rs = pstmt.executeQuery();
                logWarnings(pstmt.getWarnings());
            }
            logWarnings(rs.getWarnings());
            sqlAL = (L) new BBDBeanArrayList<B>(rs, storedProcedure);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Throwable e) {
                log.severe(e.toString());
            }
        }
        return (L) sqlAL;
    }

    /**
     * Used to insert, update, or delete rows in the database.
     *
     * @param storedProcedure
     *            BBDAPI contains storedProcure name, database, and deprecated
     *            status.
     * @param params
     *            Variable argument list of substitution values. One value for
     *            each '?' in the SQL.
     * @throws java.sql.SQLException
     *             JDBC thrown exception
     * @return integer value of number of rows updated.
     */
    @Override
    public int executeUpdate(final IBBDBeanAPI storedProcedure, final Object... params) throws SQLException {
        int rowsUpdated = 0;
        Statement stmt = null;
        PreparedStatement pstmt = null;
        BBDAPIPrincipal principal = (BBDAPIPrincipal) ((BBDBeanAPI) storedProcedure).getBbdPrincipal();
        try {
            if (params.length == 0) {
                stmt = getConnection(principal).createStatement();
                rowsUpdated = stmt.executeUpdate(getSQL(storedProcedure));
                logWarnings(stmt.getWarnings());
            } else {
                pstmt = getConnection(principal).prepareStatement(getSQL(storedProcedure));
                insertArguments(storedProcedure, params, pstmt);
                rowsUpdated = pstmt.executeUpdate();
                logWarnings(pstmt.getWarnings());
            }
            if (!getConnection(principal).getAutoCommit()) {
                getConnection(principal).commit();
            }
        } catch (SQLException sqle) {
            logExceptions(sqle);
            if (!getConnection(principal).getAutoCommit()) {
                getConnection(principal).rollback();
            }
            throw sqle;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
        }
        return rowsUpdated;
    }

    /**
     * Used to insert, update, or delete rows in the database.
     *
     * For mySQL, he following is required in the sql of a stored procedure
     *
    declare rows int;

    insert...

    set rows = row_count();

    if rows > 0 then
    select rows,LAST_INSERT_ID();
    else
    select 0,-1;
    end if;


     *
     * @param storedProcedure
     *            BBDAPI contains storedProcure name, database, and deprecated
     *            status.
     * @param params
     *            Variable argument list of substitution values. One value for
     *            each '?' in the SQL.
     * @throws java.sql.SQLException
     *             JDBC thrown exception
     * @return List<integer> number of rows changed, auto seqence keys generated.
     */
    @Override
    public List<Integer> executeUpdateGetGeneratedKeys(IBBDBeanAPI storedProcedure, Object... params) throws SQLException {
        ArrayList<Integer> sqlAL = new ArrayList();
        Statement stmt = null;
        PreparedStatement pstmt = null;
        int updatedRows = 0;
        ResultSet rs = null;
        BBDAPIPrincipal principal = (BBDAPIPrincipal) ((BBDBeanAPI) storedProcedure).getBbdPrincipal();
        boolean mySQL = true;
        try {
            if (params.length == 0) {
                stmt = getConnection(principal).createStatement();
                updatedRows = stmt.executeUpdate(getSQL(storedProcedure), Statement.RETURN_GENERATED_KEYS);
                if (mySQL) {
                    rs = stmt.getResultSet();
                } else {
                    rs = stmt.getGeneratedKeys();
                }
            } else {
                pstmt = getConnection(principal).prepareStatement(getSQL(storedProcedure), Statement.RETURN_GENERATED_KEYS);
                insertArguments(storedProcedure, params, pstmt);
                updatedRows = pstmt.executeUpdate();
                if (mySQL) {
                    rs = pstmt.getResultSet();
                } else {
                    rs = pstmt.getGeneratedKeys();
                }
            }
            int rowsChanged = -1;
            int endOfKeysIndex = -1;
            if (mySQL) {
                while (rs.next()) {
                    rowsChanged = rs.getInt(1);
                    sqlAL.add(rowsChanged);
                    endOfKeysIndex = rowsChanged + 2;
                    for (int i = 2; i < endOfKeysIndex; i++) {
                        sqlAL.add(rs.getInt(i));
                    }
                }
                if (params.length == 0) {
                    while (stmt.getMoreResults()) {
                        rs = stmt.getResultSet();
                        while (rs.next()) {
                            rowsChanged = rs.getInt(1);
                            sqlAL.add(rowsChanged);
                            endOfKeysIndex = rowsChanged + 2;
                            for (int i = 2; i < endOfKeysIndex; i++) {
                                sqlAL.add(rs.getInt(i));
                            }
                        }
                    }
                } else {
                    while (pstmt.getMoreResults()) {
                        rs = pstmt.getResultSet();
                        while (rs.next()) {
                            rowsChanged = rs.getInt(1);
                            sqlAL.add(rowsChanged);
                            endOfKeysIndex = rowsChanged + 2;
                            for (int i = 2; i < endOfKeysIndex; i++) {
                                sqlAL.add(rs.getInt(i));
                            }
                        }
                    }
                }
            } else {
                sqlAL.add(updatedRows);
                while (rs.next()) {
                    for (int i = 2; i < updatedRows + 2; i++) {
                        sqlAL.add(rs.getInt(i));
                    }
                }
            }
            if (!getConnection(principal).getAutoCommit()) {
                getConnection(principal).commit();
            }
        } catch (SQLException sqle) {
            logExceptions(sqle);
            if (!getConnection(principal).getAutoCommit()) {
                getConnection(principal).rollback();
            }
            throw sqle;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
        return sqlAL;
    }

    /**
     * Arguments passed in are substituted in the SQL. Every place the SQL has a
     * '?' a value from params is substituted.
     *
     * If the database supports getParameterMetaData, then the data is validated
     * by type. For exammple, if a Integer is passed into as param, but the
     * field uses a TinyInt type, and the Integer is greater than the largest
     * possible TinyInt, an exception is thrown.
     *
     * @param api
     *            BBDAPI contains db and sp name.
     * @param params
     *            arguments to substitute into the SQL.
     * @param pstmt
     *            a JDBC prepared statement.
     *
     * @throws java.sql.SQLException
     */
    protected void insertArguments(final IBBDBeanAPI api, final Object params[], final PreparedStatement pstmt) throws SQLException {
        String pmdKey = api.getDatabaseUsed() + ":" + api.getStoredProcedureName();
        BBDParameterMetaData[] bbdPmd = pmdMap.get(pmdKey);
        if (bbdPmd == null) {
            ParameterMetaData pmd = pstmt.getParameterMetaData();
            bbdPmd = new BBDParameterMetaData[pmd.getParameterCount()];
            if (pmd.getParameterCount() != params.length) {
                String errorMsg = "API for " + pmdKey + " requires " + pmd.getParameterCount() + ", but " + params.length + " parameters where passed!";
                log.severe(errorMsg);
                throw new SQLException(errorMsg);
            }
            int paramIndex = 0;
            boolean hadNullType = false;
            for (int index = 1; index <= pmd.getParameterCount(); index++) {
                final int mode = pmd.getParameterMode(index);
                int type = BBDValidator.UNKNOWNTYPE;
                final String name = "api argument " + index;
                try {
                    type = pmd.getParameterType(index);
                } catch (final Exception e) {
                    if (params[paramIndex] == null) {
                        type = Types.NULL;
                        hadNullType = true;
                    } else if (params[paramIndex].getClass().getName().toLowerCase().contains("string")) {
                        type = Types.VARCHAR;
                    } else if (params[paramIndex].getClass().getName().toLowerCase().contains("integer")) {
                        type = Types.INTEGER;
                    } else if (params[paramIndex].getClass().getName().toLowerCase().contains("long")) {
                        type = Types.INTEGER;
                    } else if (params[paramIndex].getClass().getName().toLowerCase().contains("date")) {
                        type = Types.DATE;
                    } else if (params[paramIndex].getClass().getName().toLowerCase().contains("timestamp")) {
                        type = Types.TIMESTAMP;
                    } else if (params[paramIndex].getClass().getName().toLowerCase().contains("time")) {
                        type = Types.TIME;
                    } else if (params[paramIndex].getClass().getName().toLowerCase().contains("boolean")) {
                        type = Types.BOOLEAN;
                    }
                }
                BBDParameterMetaData bbdPMD = new BBDParameterMetaData(mode, name, type);
                bbdPmd[paramIndex++] = bbdPMD;
            }
            if (!hadNullType) {
                pmdMap.put(pmdKey, bbdPmd);
            }
        }
        if (bbdPmd.length != params.length) {
            String errorMsg = "API for " + pmdKey + " requires " + bbdPmd.length + ", but " + params.length + " parameters where passed!";
            log.severe(errorMsg);
            throw new SQLException(errorMsg);
        }
        int paramIndex = 0;
        for (int index = 0; index < bbdPmd.length; index++) {
            BBDParameterMetaData bbdPMD = bbdPmd[index];
            final int mode = bbdPMD.getMode();
            if (mode == ParameterMetaData.parameterModeIn || mode == ParameterMetaData.parameterModeInOut) {
                int type = bbdPMD.getType();
                if (type != BBDValidator.UNKNOWNTYPE) {
                    final String columnName = bbdPMD.getName();
                    if (!BBDValidator.isValidSqlData(params[paramIndex], type, columnName)) {
                        final String msg = "Invalid data [" + params[paramIndex] + "] at " + paramIndex;
                        log.severe(msg);
                        throw new SQLException(msg);
                    }
                } else {
                    log.warning("Unknown SQL data type was not validated, paramater " + paramIndex);
                }
                if (type == Types.NULL) {
                    pstmt.setNull(index + 1, Types.JAVA_OBJECT);
                } else {
                    pstmt.setObject(index + 1, params[paramIndex]);
                }
                paramIndex++;
            }
        }
    }

    /**
     * This finds the execute SQL by stored procedure name.
     *
     * Store procedure call statements are stored in the BBD database
     * StoreProcedure table.
     *
     * After finding the SQL, it is stored in a map so the next time is it
     * needed, it comes from memory rather than the database.
     *
     * @param storedProcedure
     *            stored Procedure API
     *
     * @return String containing the SQL to execute the stored procedure.
     *
     * @throws SQLExcption
     */
    protected <A extends IBBDBeanAPI> String getSQL(final A storedProcedure) throws SQLException {
        String sql = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int row = 0;
        final String apiKey = storedProcedure.getDatabaseUsed() + ":" + storedProcedure.getStoredProcedureName();
        synchronized (apiMap) {
            sql = apiMap.get(apiKey);
        }
        if (sql != null) {
            log.info("BBD API from cache for " + apiKey);
            useDatabase(storedProcedure);
            ((BBDBeanAPI) storedProcedure).setSQL(sql);
            return sql;
        }
        try {
            final String getAPI = BBDProperties.get(BBDProperties.GetAPI);
            pstmt = getConnection(bbdDBPrincipal).prepareStatement(getAPI);
            pstmt.setString(1, storedProcedure.getDatabaseUsed());
            pstmt.setString(2, storedProcedure.getStoredProcedureName());
            useBBD();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ((BBDBeanAPI) storedProcedure).setBBDAPI(rs);
                sql = storedProcedure.getSQL();
                if (storedProcedure.isDeprecated()) {
                    final Throwable th = new Throwable();
                    th.fillInStackTrace();
                    final StackTraceElement[] ste = th.getStackTrace();
                    final StringBuffer sb = new StringBuffer("\n");
                    for (final StackTraceElement st : ste) {
                        sb.append(st.toString());
                        sb.append("\n");
                    }
                    log.warning("BBD API from database for DEPRECATED " + apiKey + sb.toString());
                } else {
                    log.info("BBD API from database for " + apiKey);
                }
                row++;
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
        }
        if (storedProcedure.isTestOnly() && !TRUE.equalsIgnoreCase(BBDProperties.get(BBDProperties.UseTestAPI))) {
            log.severe("API row is for test " + apiKey);
            throw new SQLException("API row is for test " + apiKey);
        }
        boolean SQLPassThrough = false;
        if (TRUE.equalsIgnoreCase(BBDProperties.get(BBDProperties.SQLPassThrough))) {
            SQLPassThrough = true;
        }
        if (row != 1 && !SQLPassThrough) {
            log.severe("API row is missing " + apiKey);
            throw new SQLException("API row is missing for " + apiKey);
        } else if (row == 0 && SQLPassThrough) {
            sql = storedProcedure.getSQL();
            if (sql == null || sql.length() == 0) {
                log.severe("API row is missing " + apiKey);
                throw new SQLException("API row is missing for " + apiKey);
            }
        }
        if (TRUE.equalsIgnoreCase(BBDProperties.get(BBDProperties.CacheDeprecatedAPI))) {
            synchronized (apiMap) {
                apiMap.put(apiKey, sql);
            }
        }
        useDatabase(storedProcedure);
        return sql;
    }

    protected void useDatabase(final IBBDBeanAPI api) throws SQLException {
        if (BBDValidator.isValidDatabase(api.getDatabaseUsed())) {
            Statement stmt = null;
            BBDAPIPrincipal principal = (BBDAPIPrincipal) ((BBDBeanAPI) api).getBbdPrincipal();
            try {
                stmt = getConnection(principal).createStatement();
                stmt.execute("use " + api.getDatabaseUsed() + ";");
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        }
    }

    protected void useBBD() throws SQLException {
        Statement stmt = null;
        try {
            stmt = getConnection(bbdDBPrincipal).createStatement();
            stmt.execute("use " + BBD + ";");
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Close connection and clean up resources.
     *
     * @throws java.lang.Throwable
     *             Any expections during finalize.
     */
    @Override
    protected void finalize() throws Throwable {
        if (currentPrinc != null) {
            if (!currentPrinc.equals(bbdDBPrincipal)) {
                try {
                    synchronized (conns) {
                        if (currentPrinc.decrementUsage()) {
                            conns.get(currentPrinc).close();
                            conns.remove(currentPrinc);
                            log.config("Connection finalized for user " + currentPrinc.getName());
                        }
                    }
                } catch (final SQLException ex) {
                }
            } else {
                currentPrinc.decrementUsage();
                log.config("Connection finalized for root, current usages are " + currentPrinc.getUsages());
            }
        }
        super.finalize();
    }

    @Override
    public boolean isPrincipalValid(final IBBDAPIPrincipal principal) {
        try {
            getConnection((BBDAPIPrincipal) principal);
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }

    @Override
    public void setAutoCommit(final IBBDAPIPrincipal principal, boolean autoCommit) throws SQLException {
        getConnection((BBDAPIPrincipal) principal).setAutoCommit(autoCommit);
    }

    @Override
    public void commit(final IBBDAPIPrincipal principal) throws SQLException {
        BBDAPIPrincipal princ = (BBDAPIPrincipal) principal;
        if (!getConnection(princ).getAutoCommit()) {
            getConnection(princ).commit();
        } else {
            log.severe("You have not set autocommit to true, so JDBC already did the commit");
            throw new SQLException("JDBC is set to autocommit, so JDBC already did the commit");
        }
    }

    static void logWarnings(SQLWarning warn) {
        while (warn != null) {
            log.info("SQL Warning:");
            log.info(warn.getMessage());
            log.info("ANSI-92 SQL State: " + warn.getSQLState());
            log.info("Vendor Error Code: " + warn.getErrorCode());
            warn = warn.getNextWarning();
        }
    }

    static void logExceptions(SQLException e) {
        while (e != null) {
            log.severe("SQL Exception:");
            log.severe(e.getMessage());
            log.severe("ANSI-92 SQL State: " + e.getSQLState());
            log.severe("Vendor Error Code: " + e.getErrorCode());
            e = e.getNextException();
        }
    }
}
