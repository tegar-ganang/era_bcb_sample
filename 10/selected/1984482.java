package com.zenark.zsql;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.zenark.zsql.*;

public class TransactionImpl implements Transaction {

    /** Internal connection pool to use for allocating connections */
    private String _poolName = null;

    /** Vector of statements for this transaction */
    private ArrayList _statements = new ArrayList();

    /**
     * Constructor
     * @param poolName is the name of the Turbine connection pool to use.
     */
    public TransactionImpl(String poolName) {
        _poolName = poolName;
    }

    /**
     * Add a statement to this transaction.
     * @param statement is the query string including ? parameters
     * @param params is an ArrayList containing strings representing each of the
     * required parameters (in order).
     */
    public void addStatement(String statement, ArrayList params) throws TransactionException {
        StatementData sd = new StatementData();
        sd.statement = statement;
        sd.params = params;
        _statements.add(sd);
    }

    /**
     * Commit all statements to the database.
     * @return The number of rows affected.
     */
    public int commit() throws TransactionException, SQLException, ConnectionFactoryException {
        Connection conn = ConnectionFactoryProxy.getInstance().getConnection(_poolName);
        conn.setAutoCommit(false);
        int numRowsUpdated = 0;
        try {
            Iterator statements = _statements.iterator();
            while (statements.hasNext()) {
                StatementData sd = (StatementData) statements.next();
                PreparedStatement ps = conn.prepareStatement(sd.statement);
                Iterator params = sd.params.iterator();
                int index = 1;
                while (params.hasNext()) {
                    ps.setString(index++, (String) params.next());
                }
                numRowsUpdated += ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ex) {
            System.err.println("com.zenark.zsql.TransactionImpl.commit() failed: Queued Statements follow");
            Iterator statements = _statements.iterator();
            while (statements.hasNext()) {
                StatementData sd = (StatementData) statements.next();
                System.err.println("+--Statement: " + sd.statement + " with " + sd.params.size() + " parameters");
                for (int loop = 0; loop < sd.params.size(); loop++) {
                    System.err.println("+--Param    : " + (String) sd.params.get(loop));
                }
            }
            throw ex;
        } finally {
            _statements.clear();
            conn.rollback();
            conn.clearWarnings();
            ConnectionFactoryProxy.getInstance().releaseConnection(conn);
        }
        return numRowsUpdated;
    }

    public void rollback() throws TransactionException {
        _statements.clear();
    }

    /** Inner class (structure) to store the statement details until commit */
    private class StatementData {

        public String statement;

        public ArrayList params;
    }
}
