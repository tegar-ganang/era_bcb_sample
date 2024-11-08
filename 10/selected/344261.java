package net.infian.framework.db.pooling;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a wrapper for a java.sql.Connection object that maintains a cache of PreparedStatements as well as has convenience methods for ease of use.
 */
public final class PooledConnection {

    private final ConnectionPool pool;

    private Map<String, PreparedStatement> cache = new HashMap<String, PreparedStatement>(1, 1);

    private Connection con;

    PooledConnection(final ConnectionPool pool) throws SQLException {
        this.pool = pool;
        con = pool.getConnection();
        con.setAutoCommit(false);
    }

    private final void reconnect() throws SQLException {
        cache = new HashMap<String, PreparedStatement>(1, 1);
        con.close();
        con = pool.getConnection();
        con.setAutoCommit(false);
    }

    private final PreparedStatement getPreparedStatement(final String sql, final Object... params) throws SQLException {
        PreparedStatement ps = cache.get(sql);
        if (ps == null) {
            try {
                ps = con.prepareStatement(sql);
            } catch (SQLException e) {
                reconnect();
                ps = con.prepareStatement(sql);
            }
            cache.put(sql, ps);
        }
        for (int i = params.length; --i >= 0; ) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }

    /**
	 * This method forms a PreparedStatement, sets it's parameters, and executes it in the form of a query.
	 * @param sql SQL statement in PreparedStatement form to execute
	 * @param params parameters to replace question marks in the SQL
	 * @return ResultSet returned from the query
	 * @throws SQLException if unable to execute the query
	 */
    public final ResultSet executeQuery(final String sql, final Object... params) throws SQLException {
        ResultSet rs;
        PreparedStatement ps = getPreparedStatement(sql, params);
        try {
            rs = ps.executeQuery();
        } catch (SQLException e) {
            reconnect();
            ps = getPreparedStatement(sql, params);
            rs = ps.executeQuery();
        }
        return rs;
    }

    /**
	 * This method forms a PreparedStatement, sets it's parameters, and executes it in the form of an update.
	 * @param commit TRUE if it is to be committed upon completion.
	 * @param sql SQL statement in PreparedStatement form to execute
	 * @param params parameters to replace question marks in the SQL
	 * @return number of records updated
	 * @throws SQLException if unable to execute the update
	 */
    public final int executeUpdate(final boolean commit, final String sql, final Object... params) throws SQLException {
        int updated;
        PreparedStatement ps = getPreparedStatement(sql, params);
        try {
            updated = ps.executeUpdate();
        } catch (SQLException e) {
            reconnect();
            ps = getPreparedStatement(sql, params);
            updated = ps.executeUpdate();
        }
        if (commit) {
            commit();
        }
        return updated;
    }

    /**
	 * This method forms a PreparedStatement, sets it's parameters, and executes it in the form of an update.
	 * @param sql SQL statement in PreparedStatement form to execute
	 * @param params parameters to replace question marks in the SQL
	 * @return number of records updated
	 * @throws SQLException if unable to execute the update
	 */
    public final int executeUpdate(final String sql, final Object... params) throws SQLException {
        return executeUpdate(true, sql, params);
    }

    /**
	 * This executes the statements of a Transaction object returning the number of rows affected.  It will rollback before throwing an exception if an error occurs.
	 * @param commit TRUE if transaction is to be committed upon successfully completing
	 * @param transaction Transaction to be executed
	 * @throws SQLException if any of the statements fail
	 */
    public final int executeTransaction(final boolean commit, final Transaction transaction) throws SQLException {
        int updated = 0;
        try {
            for (Statement statement : transaction.statements) {
                updated += executeUpdate(false, statement.sql, statement.params);
            }
            if (commit) {
                commit();
            }
        } catch (SQLException e) {
            rollback();
            throw e;
        }
        return updated;
    }

    /**
	 * This executes the statements of a Transaction object returning the number of rows affected.  It will rollback before throwing an exception if an error occurs.  If not, it will commit the changes.
	 * @param transaction Transaction to be executed
	 * @throws SQLException if any of the statements fail
	 */
    public final int executeTransaction(final Transaction transaction) throws SQLException {
        return executeTransaction(true, transaction);
    }

    /**
	 * Commits the transaction.
	 * @throws SQLException if it cannot be committed
	 */
    public final void commit() throws SQLException {
        con.commit();
    }

    /**
	 * Rolls back the transaction.
	 * @throws SQLException if it cannot be rolled back
	 */
    public final void rollback() throws SQLException {
        con.rollback();
    }

    /**
	 * This returns the PooledConnection to the ConnectionPool.
	 */
    public final void close() {
        pool.returnConnection(this);
    }

    /**
	 * This returns the java.sql.Connection object underlying the PooledConnection for any additional functionality not directly provided, such as methods for CallableStatements.
	 * @return underlying Connection object
	 */
    public final Connection getUnderlyingConnection() {
        return con;
    }
}
