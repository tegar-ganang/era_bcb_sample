package net.teqlo.queue;

import java.sql.*;
import java.util.HashMap;
import net.teqlo.TeqloException;
import net.teqlo.bus.messages.mail.ActionQueueEntry;
import net.teqlo.jdbc.JDBCDatabase;
import net.teqlo.jdbc.JDBCTransactionWrapper;
import net.teqlo.util.Loggers;

public class ActionDatabase extends JDBCDatabase {

    private static final String PROPS_BASE = "net.teqlo.actionDatabase";

    private static final int ALIAS_LENGTH = 1000;

    private static ActionDatabase theDatabase = null;

    private HashMap<Connection, PreparedStatement> storeStmts = new HashMap<Connection, PreparedStatement>();

    public ActionDatabase() throws TeqloException {
        super(PROPS_BASE);
    }

    /**
     * Returns the stats database. Opens it if necessary.
     *
     * @return a <code>StatsDatabase</code> value
     * @exception TeqloException if an error occurs
     */
    public static synchronized ActionDatabase getInstance() throws TeqloException {
        if (theDatabase == null) theDatabase = new ActionDatabase();
        return theDatabase;
    }

    public static ActionDatabase getAvailableInstance() {
        return theDatabase;
    }

    public String checkSchema(String version) throws SQLException, TeqloException {
        if (version == null || version.length() == 0) {
            executeSimpleUpdate("createTable", "CREATE TABLE tq_actions (" + "seq BIGINT NOT NULL " + getAutoIncrement() + ", " + "aliasName VARCHAR(" + ALIAS_LENGTH + ") NOT NULL, " + "queueTime TIMESTAMP NOT NULL, " + "fromAlias VARCHAR(" + ALIAS_LENGTH + ") NOT NULL, " + "toAlias VARCHAR(" + ALIAS_LENGTH + ") NOT NULL, " + "topic VARCHAR(200) NOT NULL, " + "channel VARCHAR(200) NOT NULL, " + "actionData " + getCLOBType() + ", PRIMARY KEY(seq))" + getCreateTableSuffix());
            executeSimpleUpdate("createIndex", "CREATE INDEX tq_actions_index ON tq_actions (aliasName)");
            return "1.0";
        }
        return null;
    }

    public synchronized void store(final String alias, final ActionQueueEntry action) throws TeqloException {
        try {
            wrapWithDeadlockRetry("store", new JDBCTransactionWrapper<Object>() {

                public String run(Connection connection) throws SQLException {
                    PreparedStatement stmt = storeStmts.get(connection);
                    if (stmt == null) {
                        stmt = connection.prepareStatement("INSERT INTO tq_actions (aliasName, queueTime, fromAlias, toAlias, topic, channel, actionData) VALUES (?, ?, ?, ?, ?, ?, ?)");
                        storeStmts.put(connection, stmt);
                    }
                    long when = System.currentTimeMillis();
                    stmt.setString(1, alias);
                    stmt.setTimestamp(2, new Timestamp(when));
                    stmt.setString(3, action.getFromAlias());
                    stmt.setString(4, action.getToAlias());
                    stmt.setString(5, action.getTopic());
                    stmt.setString(6, action.getChannel());
                    stmt.setString(7, action.getData());
                    stmt.executeUpdate();
                    return null;
                }
            });
        } catch (SQLException e) {
            throw new TeqloException(this, alias, e, "Could not insert record into table.");
        }
    }

    public synchronized void pullActions(final String alias, final ActionQueue queue) throws TeqloException {
        try {
            wrapWithDeadlockRetry("pullActions", new JDBCTransactionWrapper<Object>() {

                public Object run(Connection connection) throws SQLException {
                    ResultSet rs = null;
                    Statement stmt = null;
                    PreparedStatement delStmt = null;
                    try {
                        stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                        delStmt = connection.prepareStatement("DELETE FROM tq_actions WHERE seq = ?");
                        rs = stmt.executeQuery("SELECT fromAlias, toAlias, topic, channel, actionData, seq FROM tq_actions WHERE aliasName = '" + alias + "' ORDER BY seq");
                        while (rs.next()) {
                            try {
                                queue.handleMessage(new ActionQueueEntry(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
                                delStmt.setLong(1, rs.getLong(6));
                                delStmt.executeUpdate();
                            } catch (TeqloException e) {
                                Loggers.QUEUES.error("Could not process queued action for " + alias, e);
                            }
                        }
                    } finally {
                        if (rs != null) rs.close();
                        if (delStmt != null) delStmt.close();
                        if (stmt != null) stmt.close();
                    }
                    return null;
                }
            });
        } catch (SQLException e) {
            throw new TeqloException(this, alias, e, "Could not pull queued actions from database.");
        }
    }

    public synchronized boolean hasActions(final String alias) throws TeqloException {
        Boolean result;
        try {
            result = wrapWithDeadlockRetry("hasActions", new JDBCTransactionWrapper<Boolean>() {

                public Boolean run(Connection connection) throws SQLException {
                    ResultSet rs = null;
                    Statement stmt = null;
                    int value = 0;
                    try {
                        stmt = connection.createStatement();
                        rs = stmt.executeQuery("SELECT count(*) FROM tq_actions WHERE aliasName = '" + alias + "'");
                        while (rs.next()) {
                            value = rs.getInt(1);
                        }
                    } finally {
                        if (rs != null) rs.close();
                        if (stmt != null) stmt.close();
                    }
                    return new Boolean(value > 0);
                }
            });
        } catch (SQLException e) {
            throw new TeqloException(this, alias, e, "Could not check for queued actions in database.");
        }
        return result.booleanValue();
    }
}
