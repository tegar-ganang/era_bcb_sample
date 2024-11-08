package org.syrup.sql;

import org.syrup.Link;
import org.syrup.LogEntry;
import org.syrup.PTask;
import org.syrup.Task;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility functions to create Syrup Objects using JDBC.
 * 
 * @author Robbert van Dalen
 */
public class CreationFunctions extends Functions {

    static final String COPYRIGHT = "Copyright 2005 Robbert van Dalen." + "At your option, you may copy, distribute, or make derivative works under " + "the terms of The Artistic License. This License may be found at " + "http://www.opensource.org/licenses/artistic-license.php. " + "THERE IS NO WARRANTY; USE THIS PRODUCT AT YOUR OWN RISK.";

    private static final Logger logger = Logger.getLogger("org.syrup.sql.CreationFunctions");

    /**
     * Constructor for the CreationFunctions object
     * 
     * @param sqlImpl
     *            The SQLImpl that is held by the Function instance.
     */
    public CreationFunctions(SQLImpl sqlImpl) {
        super(sqlImpl);
    }

    /**
     * Creates a new Task by executing SQL statements over a Connection. Returns
     * the new Task identifier.
     * 
     * @param t
     *            The new Task to be created.
     * @param parent
     *            The PTask that is the parent of the Task to be created
     * @param con
     *            The SyrupConnection over which SQL statements are executed.
     * @return The new Task identifier.
     */
    protected String newTask(Task t, PTask parent, SyrupConnection con) throws Exception {
        String key = null;
        PreparedStatement s = null;
        ResultSet r = null;
        try {
            s = con.prepareStatementFromCache(sqlImpl().sqlStatements().selectTaskKeyStatement());
            int i = 0;
            while (i++ < 5) {
                double d = Math.random() * ((double) (1 << 62));
                key = "" + ((long) (d));
                s.setString(1, key);
                r = s.executeQuery();
                if (!r.next()) {
                    PreparedStatement s2 = null;
                    s2 = con.prepareStatementFromCache(sqlImpl().sqlStatements().createNewTaskStatement());
                    java.util.Date dd = new java.util.Date();
                    s2.setString(1, key);
                    s2.setString(2, parent.key());
                    s2.setString(3, t.name());
                    s2.setString(4, t.functionClass());
                    s2.setBoolean(5, t.orType());
                    s2.setString(6, t.description());
                    s2.setString(7, t.parameter());
                    s2.setString(8, t.environment());
                    s2.setLong(9, dd.getTime());
                    s2.setLong(10, dd.getTime());
                    s2.executeUpdate();
                    sqlImpl().loggingFunctions().log(key, LogEntry.NEW, con);
                    return key;
                }
            }
        } finally {
            sqlImpl().genericFunctions().close(r);
        }
        throw new Exception("exhausted task id generation (5)");
    }

    /**
     * Creates a new Link by executing SQL statements over a Connection.
     * 
     * @param l
     *            The new Link to be created.
     * @param map
     *            The Map that holds [Task -> Task-key].
     * @param con
     *            The SyrupConnection over which SQL statements are executed.
     */
    protected void newLink(Link l, Hashtable map, SyrupConnection con) throws Exception {
        PreparedStatement s = null;
        s = con.prepareStatementFromCache(sqlImpl().sqlStatements().createNewLinkStatement());
        if (l.from().task() == null & l.to().task() == null) {
            throw new Exception("Link must have at least one endpoint");
        }
        if (l.from().task() != null) {
            s.setString(1, (String) map.get(l.from().task()));
            s.setBoolean(2, l.from().isSecond());
        } else {
            s.setNull(1, Types.VARCHAR);
            s.setBoolean(2, false);
        }
        if (l.to().task() != null) {
            s.setString(3, (String) map.get(l.to().task()));
            s.setBoolean(4, l.to().isSecond());
        } else {
            s.setNull(3, Types.VARCHAR);
            s.setBoolean(4, false);
        }
        if (l.content() != null) {
            byte[] array = l.content().bytes();
            if (array != null) {
                s.setBinaryStream(5, new ByteArrayInputStream(array), array.length);
            } else {
                s.setNull(5, Types.LONGVARBINARY);
            }
        } else {
            s.setNull(5, Types.LONGVARBINARY);
        }
        s.executeUpdate();
    }

    /**
     * Empties the Workspace by executing SQL statements over a Connection.
     * 
     * @param con
     *            The SyrupConnection over which SQL statements are executed.
     */
    public void reset(SyrupConnection con) throws Exception {
        update(sqlImpl().sqlStatements().dropTaskTableStatement(), con, false);
        update(sqlImpl().sqlStatements().dropLinkTableStatement(), con, false);
    }

    /**
     * Creates an initial Workspace by executing SQL statements over a
     * Connection.
     * 
     * @param con
     *            The SyrupConnection over which SQL statements are executed.
     */
    public void createTables(SyrupConnection con) throws Exception {
        update(sqlImpl().sqlStatements().createTaskTableStatement(), con, false);
        update(sqlImpl().sqlStatements().createLinkTableStatement(), con, false);
        update(sqlImpl().sqlStatements().createLogTableStatement(), con, false);
        update(sqlImpl().sqlStatements().clearTaskTableStatement(), con, true);
        update(sqlImpl().sqlStatements().clearLinkTableStatement(), con, true);
        update(sqlImpl().sqlStatements().createExternalStatement(), con, true);
        update(sqlImpl().sqlStatements().createLauncherStatement(), con, true);
        update(sqlImpl().sqlStatements().createLink1Statement(), con, true);
        update(sqlImpl().sqlStatements().createLink2Statement(), con, true);
        update(sqlImpl().sqlStatements().createLink3Statement(), con, true);
        update(sqlImpl().sqlStatements().createLink4Statement(), con, true);
    }

    /**
     * Utility function to execute a 'reset' update SQL statement over a
     * Connection. If the update fails, the Exception is not thrown but instead
     * is logged when requested.
     * 
     * @param statement
     *            The update statement to be executed.
     * @param con
     *            The SyrupConnection over which SQL statements are executed.
     * @param do_log
     *            The logging parameter when an exception is thrown.
     */
    private void update(String statement, SyrupConnection con, boolean do_log) throws Exception {
        Statement s = null;
        try {
            s = con.createStatement();
            s.executeUpdate(statement);
            con.commit();
        } catch (Throwable e) {
            if (do_log) {
                logger.log(Level.INFO, "Update failed. Transaction is rolled back", e);
            }
            con.rollback();
        }
    }
}
