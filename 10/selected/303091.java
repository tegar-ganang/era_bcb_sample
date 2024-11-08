package org.syrup.sql;

import org.syrup.Context;
import org.syrup.Link;
import org.syrup.LogEntry;
import org.syrup.PTask;
import org.syrup.Result;
import org.syrup.Task;
import org.syrup.Workflow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Utility functions to execute Syrup PTasks using JDBC.
 * 
 * @author Robbert van Dalen
 */
public class ExecutionFunctions extends Functions {

    static final String COPYRIGHT = "Copyright 2005 Robbert van Dalen." + "At your option, you may copy, distribute, or make derivative works under " + "the terms of The Artistic License. This License may be found at " + "http://www.opensource.org/licenses/artistic-license.php. " + "THERE IS NO WARRANTY; USE THIS PRODUCT AT YOUR OWN RISK.";

    private static final Logger logger = Logger.getLogger("org.syrup.sql.ExecutionFunctions");

    /**
     * Constructor for the ExecutionFunctions object
     * 
     * @param sqlImpl
     *            The SQLImpl that is held by the Function instance.
     */
    public ExecutionFunctions(SQLImpl sqlImpl) {
        super(sqlImpl);
    }

    /**
     * Starts the execution of a PTask by executing SQL statements over a
     * Connection. Returns the associated Context from the WorkSpace.
     * 
     * @param pt
     *            The Ptask to be executed.
     * @param w
     *            The Worker address (URL) that requested execution.
     * @param con
     *            The SyrupConnection over which SQL statements are executed.
     * @return The associated Context.
     */
    protected Context start(PTask pt, String w, SyrupConnection con) throws Exception {
        PreparedStatement s = null;
        ResultSet result = null;
        try {
            s = con.prepareStatementFromCache(sqlImpl().sqlStatements().checkIsExecutableTaskStatement());
            s.setString(1, pt.key());
            result = s.executeQuery();
            if (result.next()) {
                java.util.Date dd = new java.util.Date();
                PreparedStatement s2 = null;
                s2 = con.prepareStatementFromCache(sqlImpl().sqlStatements().updateWorkerStatement());
                s2.setString(1, w);
                s2.setString(2, pt.key());
                s2.executeUpdate();
                sqlImpl().loggingFunctions().log(pt.key(), LogEntry.STARTED, con);
                Context c = sqlImpl().queryFunctions().readContext(pt, con);
                con.commit();
                return c;
            }
        } finally {
            con.rollback();
            sqlImpl().genericFunctions().close(result);
        }
        return null;
    }

    /**
     * Commits the Result of an execution by executing SQL statements over a
     * Connection. Returns the executed PTask that produced the Result.
     * 
     * @param r
     *            The Result to be commited.
     * @param con
     *            The SyrupConnection over which SQL statements are executed.
     * @return The executed PTask that produced the Result.
     */
    protected PTask commit_result(Result r, SyrupConnection con) throws Exception {
        try {
            int logAction = LogEntry.ENDED;
            String kk = r.context().task().key();
            if (r.in_1_consumed() && r.context().in_1_link() != null) {
                sqlImpl().updateFunctions().updateInLink(kk, false, null, con);
                logAction = logAction | LogEntry.IN_1;
            }
            if (r.in_2_consumed() && r.context().in_2_link() != null) {
                sqlImpl().updateFunctions().updateInLink(kk, true, null, con);
                logAction = logAction | LogEntry.IN_2;
            }
            if (r.out_1_result() != null && r.context().out_1_link() != null) {
                sqlImpl().updateFunctions().updateOutLink(kk, false, r.out_1_result(), con);
                logAction = logAction | LogEntry.OUT_1;
            }
            if (r.out_2_result() != null && r.context().out_2_link() != null) {
                sqlImpl().updateFunctions().updateOutLink(kk, true, r.out_2_result(), con);
                logAction = logAction | LogEntry.OUT_2;
            }
            sqlImpl().loggingFunctions().log(r.context().task().key(), logAction, con);
            boolean isParent = r.context().task().isParent();
            if (r instanceof Workflow) {
                Workflow w = (Workflow) r;
                Task[] tt = w.tasks();
                Link[] ll = w.links();
                Hashtable tkeyMap = new Hashtable();
                for (int i = 0; i < tt.length; i++) {
                    String key = sqlImpl().creationFunctions().newTask(tt[i], r.context().task(), con);
                    tkeyMap.put(tt[i], key);
                }
                for (int j = 0; j < ll.length; j++) {
                    sqlImpl().creationFunctions().newLink(ll[j], tkeyMap, con);
                }
                String in_link_1 = sqlImpl().queryFunctions().readInTask(kk, false, con);
                String in_link_2 = sqlImpl().queryFunctions().readInTask(kk, true, con);
                String out_link_1 = sqlImpl().queryFunctions().readOutTask(kk, false, con);
                String out_link_2 = sqlImpl().queryFunctions().readOutTask(kk, true, con);
                sqlImpl().updateFunctions().rewireInLink(kk, false, w.in_1_binding(), tkeyMap, con);
                sqlImpl().updateFunctions().rewireInLink(kk, true, w.in_2_binding(), tkeyMap, con);
                sqlImpl().updateFunctions().rewireOutLink(kk, false, w.out_1_binding(), tkeyMap, con);
                sqlImpl().updateFunctions().rewireOutLink(kk, true, w.out_2_binding(), tkeyMap, con);
                for (int k = 0; k < tt.length; k++) {
                    String kkey = (String) tkeyMap.get(tt[k]);
                    sqlImpl().updateFunctions().checkAndUpdateDone(kkey, con);
                }
                sqlImpl().updateFunctions().checkAndUpdateDone(in_link_1, con);
                sqlImpl().updateFunctions().checkAndUpdateDone(in_link_2, con);
                sqlImpl().updateFunctions().checkAndUpdateDone(out_link_1, con);
                sqlImpl().updateFunctions().checkAndUpdateDone(out_link_2, con);
                for (int k = 0; k < tt.length; k++) {
                    String kkey = (String) tkeyMap.get(tt[k]);
                    sqlImpl().updateFunctions().checkAndUpdateTargetExecutable(kkey, con);
                }
                sqlImpl().updateFunctions().checkAndUpdateTargetExecutable(in_link_1, con);
                sqlImpl().updateFunctions().checkAndUpdateTargetExecutable(in_link_2, con);
                sqlImpl().updateFunctions().checkAndUpdateTargetExecutable(out_link_1, con);
                sqlImpl().updateFunctions().checkAndUpdateTargetExecutable(out_link_2, con);
                isParent = true;
            }
            sqlImpl().updateFunctions().checkAndUpdateDone(kk, con);
            sqlImpl().updateFunctions().checkAndUpdateTargetExecutable(kk, con);
            PreparedStatement s3 = null;
            s3 = con.prepareStatementFromCache(sqlImpl().sqlStatements().updateTaskModificationStatement());
            java.util.Date dd = new java.util.Date();
            s3.setLong(1, dd.getTime());
            s3.setBoolean(2, isParent);
            s3.setString(3, r.context().task().key());
            s3.executeUpdate();
            sqlImpl().loggingFunctions().log(kk, LogEntry.ENDED, con);
            con.commit();
            return sqlImpl().queryFunctions().readPTask(kk, con);
        } finally {
            con.rollback();
        }
    }
}
