package org.ice.db.adapters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import org.ice.Config;
import org.ice.utils.FieldUtils;
import org.ice.utils.LogUtils;

/**
 * Base class for typical adapters. Providing some common
 * functionalities and utilities
 * 
 * @author dungba
 */
public abstract class AbstractAdapter implements IAdapter {

    protected Connection connection;

    protected boolean autoCommit;

    protected Exception lastError = null;

    protected Statement batchStmt;

    public Exception getLastError() {
        return lastError;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
	 * Executes a SELECT statement in Ice Query Syntax and returns 
	 * the <code>ResultSet</code> object generated by the query
	 * @param query the SELECT statement
	 * @param data the data associated with the statement
	 * @return a <code>ResultSet</code> object that contains the 
	 * 		data produced by the query;
	 * @throws SQLException
	 */
    protected ResultSet executeSelect(String query, Object data) throws SQLException {
        ParsedQuery parsed = parseQuery(query);
        if (Config.debugMode) debugSql(parsed, data);
        PreparedStatement statement = connection.prepareStatement(parsed.query);
        for (int i = 0; i < parsed.params.size(); i++) {
            statement.setObject(i + 1, FieldUtils.getValue(data, parsed.params.get(i)));
        }
        return statement.executeQuery();
    }

    /**
	 * Executes an UPDATE statement in Ice Query Syntax and returns 
	 * the row count for the statement
	 * @param query the UPDATE statement
	 * @param data the data associated with the statement
	 * @return the row count for the statement
	 * @throws SQLException
	 */
    protected int executeUpdate(String query, Object data) throws SQLException {
        ParsedQuery parsed = parseQuery(query);
        if (Config.debugMode) debugSql(parsed, data);
        PreparedStatement statement = connection.prepareStatement(parsed.query);
        for (int i = 0; i < parsed.params.size(); i++) {
            statement.setObject(i + 1, FieldUtils.getValue(data, parsed.params.get(i)));
        }
        return this.doExecuteUpdate(statement);
    }

    protected int doExecuteUpdate(PreparedStatement statement) throws SQLException {
        connection.setAutoCommit(isAutoCommit());
        int rs = -1;
        try {
            lastError = null;
            rs = statement.executeUpdate();
            if (!isAutoCommit()) connection.commit();
        } catch (Exception ex) {
            if (!isAutoCommit()) {
                lastError = ex;
                connection.rollback();
                LogUtils.log(Level.SEVERE, "Transaction is being rollback. Error: " + ex.toString());
            }
        } finally {
            if (statement != null) statement.close();
        }
        return rs;
    }

    /**
	 * Executes an INSERT statement in Ice Query Syntax and returns 
	 * the row count for the statement
	 * @param query the INSERT statement
	 * @param data the data associated with the statement
	 * @return the row count for the statement
	 * @throws SQLException
	 */
    protected int executeInsert(String query, Table data) throws SQLException {
        ParsedQuery parsed = parseQuery(query);
        if ((Boolean) Config.get("debugMode")) debugSql(parsed, data);
        PreparedStatement statement = connection.prepareStatement(parsed.query, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < parsed.params.size(); i++) {
            statement.setObject(i + 1, FieldUtils.getValue(data, parsed.params.get(i)));
        }
        return this.doExecuteInsert(statement, data);
    }

    protected int doExecuteInsert(PreparedStatement statement, Table data) throws SQLException {
        ResultSet rs = null;
        int result = -1;
        try {
            lastError = null;
            result = statement.executeUpdate();
            if (!isAutoCommit()) connection.commit();
            rs = statement.getGeneratedKeys();
            while (rs.next()) {
                FieldUtils.setValue(data, data.key, rs.getObject(1));
            }
        } catch (SQLException ex) {
            if (!isAutoCommit()) {
                lastError = ex;
                connection.rollback();
                LogUtils.log(Level.SEVERE, "Transaction is being rollback. Error: " + ex.toString());
            } else {
                throw ex;
            }
        } finally {
            if (statement != null) statement.close();
            if (rs != null) rs.close();
        }
        return result;
    }

    /**
	 * Debugs a query
	 * @param parsed the query to be printed
	 * @param data the data associated with the query
	 */
    private void debugSql(ParsedQuery parsed, Object data) {
        StringBuilder builder = new StringBuilder(parsed.query);
        if (!parsed.params.isEmpty()) {
            builder.append(" (");
            for (String p : parsed.params) {
                Object obj = FieldUtils.getValue(data, p);
                builder.append("'" + obj + "',");
            }
            if (builder.charAt(builder.length() - 1) == ',') {
                builder.setCharAt(builder.length() - 1, ')');
            }
        }
        LogUtils.log(Level.INFO, builder.toString());
    }

    /**
	 * Extends an object using given <code>ResultSet</code>
	 * @param rs the result set, containing data used for extending object
	 * @param obj the object to be extended
	 * @return the extended object
	 * @throws SQLException
	 */
    protected Object extendObject(ResultSet rs, Object obj) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int count = rsmd.getColumnCount();
        for (int i = 1; i <= count; i++) {
            try {
                String columnName = rsmd.getColumnLabel(i);
                FieldUtils.setValue(obj, columnName, rs.getObject(columnName));
            } catch (Exception ex) {
            }
        }
        return obj;
    }

    /**
	 * Parses a query to extract parameters
	 * @param query the query to be parsed
	 * @return the parsed query
	 */
    protected ParsedQuery parseQuery(String query) {
        String parsedQuery = "";
        ArrayList<String> params = new ArrayList<String>();
        String[] frag = query.split(" ");
        for (String f : frag) {
            f = f.trim();
            if (f.isEmpty()) continue;
            if (f.charAt(0) == '?') {
                params.add(f.substring(1));
                f = "?";
            }
            parsedQuery += f + " ";
        }
        return new ParsedQuery(parsedQuery, params);
    }

    class ParsedQuery {

        public String query;

        public ArrayList<String> params;

        public ParsedQuery(String query, ArrayList<String> params) {
            this.query = query;
            this.params = params;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception ex) {
            LogUtils.log(Level.WARNING, "Cannot close connection: " + ex.toString());
        }
    }

    public void startBatch() throws SQLException {
        connection.setAutoCommit(isAutoCommit());
        batchStmt = connection.createStatement();
    }

    public void addBatch(String sql) throws SQLException {
        batchStmt.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        batchStmt.clearBatch();
    }

    public void batchUpdate() throws SQLException {
        try {
            batchStmt.executeBatch();
            if (isAutoCommit()) connection.commit();
        } catch (SQLException ex) {
            if (isAutoCommit()) connection.rollback();
            throw ex;
        } finally {
            try {
                batchStmt.close();
                batchStmt = null;
            } catch (Exception ex) {
            }
        }
    }
}