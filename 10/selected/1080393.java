package org.syrup.sql;

import org.syrup.Context;
import org.syrup.Data;
import org.syrup.LogEntry;
import org.syrup.LogEntryTemplate;
import org.syrup.PTask;
import org.syrup.PTaskTemplate;
import org.syrup.helpers.EndPoint;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility to implement generic WorkSpace functions using JDBC.
 * 
 * @author Robbert van Dalen
 */
public class GenericFunctions extends Functions {

    static final String COPYRIGHT = "Copyright 2005 Robbert van Dalen." + "At your option, you may copy, distribute, or make derivative works under " + "the terms of The Artistic License. This License may be found at " + "http://www.opensource.org/licenses/artistic-license.php. " + "THERE IS NO WARRANTY; USE THIS PRODUCT AT YOUR OWN RISK.";

    private static final Logger logger = Logger.getLogger("org.syrup.sql.GenericFunctions");

    /**
     * Constructor for the GenericFunctions object
     * 
     * @param sqlImpl
     *            The SQLImpl that is held by the Function instance.
     */
    public GenericFunctions(SQLImpl sqlImpl) {
        super(sqlImpl);
    }

    /**
     * Sets the first input by executing SQL statements over a Connection..
     * 
     * @param data
     *            The Data that is set on the first input.
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     */
    public void set_in_1(Data data, SyrupConnection con) throws Exception {
        try {
            EndPoint p = sqlImpl().queryFunctions().readOutEndPoint("1", false, con);
            if (p.data != null) {
                throw new Exception("in1 is filled");
            }
            sqlImpl().updateFunctions().updateOutLink("1", false, data, con);
            sqlImpl().loggingFunctions().log("1", LogEntry.EVENT | LogEntry.IN_1, con);
            sqlImpl().updateFunctions().checkAndUpdateDone("1", con);
            con.commit();
        } finally {
            con.rollback();
        }
    }

    /**
     * Sets the second input by executing SQL statements over a Connection..
     * 
     * @param data
     *            The Data that is set on the second input.
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     */
    public void set_in_2(Data data, SyrupConnection con) throws Exception {
        try {
            EndPoint p = sqlImpl().queryFunctions().readOutEndPoint("1", true, con);
            if (p.data != null) {
                throw new Exception("in2 is filled");
            }
            sqlImpl().updateFunctions().updateOutLink("1", true, data, con);
            sqlImpl().loggingFunctions().log("1", LogEntry.EVENT | LogEntry.IN_2, con);
            sqlImpl().updateFunctions().checkAndUpdateDone("1", con);
            con.commit();
        } finally {
            con.rollback();
        }
    }

    /**
     * Returns the first output by executing SQL statements over a Connection..
     * 
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     * @return The Data removed from the first output.
     */
    public Data get_out_1(SyrupConnection con) throws Exception {
        try {
            EndPoint p = sqlImpl().queryFunctions().readInEndPoint("1", false, con);
            if (p.data != null) {
                sqlImpl().updateFunctions().updateInLink("1", false, null, con);
                sqlImpl().loggingFunctions().log("1", LogEntry.EVENT | LogEntry.OUT_1, con);
                sqlImpl().updateFunctions().checkAndUpdateDone("1", con);
                con.commit();
                return p.data;
            } else {
                throw new Exception("out1 is empty");
            }
        } finally {
            con.rollback();
        }
    }

    /**
     * Returns the second output by executing SQL statements over a Connection..
     * 
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     * @return The Data removed from the second output.
     */
    public Data get_out_2(SyrupConnection con) throws Exception {
        try {
            EndPoint p = sqlImpl().queryFunctions().readInEndPoint("1", true, con);
            if (p.data != null) {
                sqlImpl().updateFunctions().updateInLink("1", true, null, con);
                sqlImpl().loggingFunctions().log("1", LogEntry.EVENT | LogEntry.OUT_2, con);
                sqlImpl().updateFunctions().checkAndUpdateDone("1", con);
                con.commit();
                return p.data;
            } else {
                throw new Exception("out2 is empty");
            }
        } finally {
            con.rollback();
        }
    }

    /**
     * Get PTasks matching the PTaskTemplate.
     * 
     * @param template
     *            The PTaskTemplate to be matched.
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     * @return An array of PTasks that match the PTaskTemplate.
     */
    public PTask[] match(PTaskTemplate template, SyrupConnection con) throws Exception {
        PTask p[] = new PTask[0];
        String qq = null;
        List qParts = new LinkedList();
        qParts.add(new QueryOperation(template, "description", "description"));
        qParts.add(new QueryOperation(template, "name", "name"));
        qParts.add(new QueryOperation(template, "function_class", "functionClass"));
        qParts.add(new QueryOperation(template, "environment", "environment"));
        qParts.add(new QueryOperation(template, "parameter", "parameter"));
        qParts.add(new QueryOperation(template, "or_type", "orType"));
        qParts.add(new QueryOperation(template, "parent_key", "parentKey"));
        qParts.add(new QueryOperation(template, "key_", "key"));
        qParts.add(new QueryOperation(template, "modifications", "modifications"));
        qParts.add(new QueryOperation(template, "modification_time", "modificationTime"));
        qParts.add(new QueryOperation(template, "creation_time", "creationTime"));
        qParts.add(new QueryOperation(template, "executable", "executable"));
        qParts.add(new QueryOperation(template, "done", "done"));
        qParts.add(new QueryOperation(template, "worker", "worker"));
        qParts.add(new QueryOperation(template, "is_parent", "isParent"));
        Iterator qIterator = qParts.iterator();
        while (qIterator.hasNext()) {
            QueryOperation op = (QueryOperation) qIterator.next();
            if (op.sqlOp != null) {
                if (qq != null) {
                    qq = qq + " and ";
                } else {
                    qq = " ";
                }
                qq = qq + op.sqlAtt + " " + op.sqlOp + " ?";
            }
        }
        if (qq == null) {
            qq = sqlImpl().sqlStatements().selectTasksStatement();
        } else {
            qq = sqlImpl().sqlStatements().selectTasksStatement() + "where" + qq;
        }
        PreparedStatement ps = null;
        ps = con.prepareStatement(qq);
        qIterator = qParts.iterator();
        int ii = 1;
        while (qIterator.hasNext()) {
            QueryOperation op = (QueryOperation) qIterator.next();
            if (op.sqlOp != null) {
                ps.setObject(ii++, op.value);
            }
        }
        ResultSet result = null;
        try {
            result = ps.executeQuery();
            ArrayList ptasks = new ArrayList();
            while (result.next()) {
                PTask t = sqlImpl().queryFunctions().readPTask(result);
                ptasks.add(t);
            }
            p = new PTask[ptasks.size()];
            Object[] o = ptasks.toArray();
            for (int i = 0; i < o.length; i++) {
                p[i] = (PTask) o[i];
            }
        } finally {
            close(result);
        }
        return p;
    }

    /**
     * Get LogEntries matching the LogEntryTemplate.
     * 
     * @param template
     *            The LogEntryTemplate to be matched.
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     * @return An array of LogEntries that match the LogEntryTemplate.
     */
    public LogEntry[] match(LogEntryTemplate template, SyrupConnection con) throws Exception {
        LogEntry l[] = new LogEntry[0];
        String qq = null;
        List qParts = new LinkedList();
        qParts.add(new QueryOperation(template, "creation_time", "time"));
        qParts.add(new QueryOperation(template, "key_", "key"));
        qParts.add(new QueryOperation(template, "event", "event"));
        qParts.add(new QueryOperation(template, "worker", "worker"));
        Iterator qIterator = qParts.iterator();
        while (qIterator.hasNext()) {
            QueryOperation op = (QueryOperation) qIterator.next();
            if (op.sqlOp != null) {
                if (qq != null) {
                    qq = qq + " and ";
                } else {
                    qq = " ";
                }
                qq = qq + op.sqlAtt + " " + op.sqlOp + " ?";
            }
        }
        if (qq == null) {
            qq = sqlImpl().sqlStatements().selectLogEntriesStatement();
        } else {
            qq = sqlImpl().sqlStatements().selectLogEntriesStatement() + "where" + qq;
        }
        PreparedStatement ps = null;
        ps = con.prepareStatement(qq);
        qIterator = qParts.iterator();
        int ii = 1;
        while (qIterator.hasNext()) {
            QueryOperation op = (QueryOperation) qIterator.next();
            if (op.sqlOp != null) {
                ps.setObject(ii++, op.value);
            }
        }
        ResultSet result = null;
        try {
            result = ps.executeQuery();
            ArrayList logs = new ArrayList();
            while (result.next()) {
                LogEntry t = sqlImpl().queryFunctions().readLogEntry(result);
                logs.add(t);
            }
            l = new LogEntry[logs.size()];
            Object[] o = logs.toArray();
            for (int i = 0; i < o.length; i++) {
                l[i] = (LogEntry) o[i];
            }
        } finally {
            close(result);
        }
        return l;
    }

    /**
     * Get the Contexts of PTasks that match the PTaskTemplate.
     * 
     * @param t
     *            The PTaskTemplate to be matched.
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     * @return An array of Contexts surrounding the PTasks that match the
     *         PTaskTemplate.
     */
    public Context[] get(PTaskTemplate t, SyrupConnection con) throws Exception {
        PTask[] tasks = match(t, con);
        Context[] c = new Context[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            c[i] = sqlImpl().queryFunctions().readContext(tasks[i], con);
        }
        return c;
    }

    /**
     * Resets the WorkSpace to the initial state.
     * 
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     */
    public void reset(SyrupConnection con) throws Exception {
        sqlImpl().creationFunctions().reset(con);
        sqlImpl().creationFunctions().createTables(con);
    }

    /**
     * Stop non-progressing executions of a PTask.
     * 
     * @param task
     *            The PTask to be stopped.
     * @param con
     *            The SyrupConnection over which SQL statements are executed
     * @return The stopped PTask.
     */
    public PTask stop(PTask task, SyrupConnection con) throws Exception {
        PreparedStatement s = null;
        ResultSet result = null;
        try {
            s = con.prepareStatementFromCache(sqlImpl().sqlStatements().checkWorkerStatement());
            s.setString(1, task.key());
            result = s.executeQuery();
            con.commit();
            if (result.next()) {
                String url = result.getString("worker");
                InputStream i = null;
                try {
                    Object b = new URL(url).getContent();
                    if (b instanceof InputStream) {
                        i = (InputStream) b;
                        byte[] bb = new byte[256];
                        int ll = i.read(bb);
                        String k = new String(bb, 0, ll);
                        if (k.equals(task.key())) {
                            return task;
                        }
                    }
                } catch (Exception e) {
                } finally {
                    if (i != null) {
                        i.close();
                    }
                }
                PreparedStatement s2 = null;
                s2 = con.prepareStatementFromCache(sqlImpl().sqlStatements().resetWorkerStatement());
                s2.setString(1, task.key());
                s2.executeUpdate();
                task = sqlImpl().queryFunctions().readPTask(task.key(), con);
                sqlImpl().loggingFunctions().log(task.key(), LogEntry.STOPPED, con);
                con.commit();
            }
        } finally {
            con.rollback();
            close(result);
        }
        return task;
    }

    /**
     * Utility method that closes the Connection.
     * 
     * @param con
     *            The SyrupConnection to be closed.
     */
    public final void close(SyrupConnection con) throws Exception {
        if (con != null) {
            if (!con.isClosed()) {
                con.rollback();
                con.close();
            }
        }
    }

    /**
     * Utility method that resets the ResultSet.
     * 
     * @param result
     *            The ResultSet to be resetted.
     */
    public final void close(ResultSet result) throws Exception {
        if (result != null) {
            result.close();
        }
    }
}
