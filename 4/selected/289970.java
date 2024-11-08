package com.quikj.server.framework;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;

public class AceSQL implements AceCompareMessageInterface {

    class AceSQLThread extends AceThread {

        private boolean quitThread = false;

        private AceThread parent;

        private String[] sqlStatements = null;

        private Object userParm;

        private Statement[] statements = null;

        private Statement currentStatement = null;

        private int operationId;

        public AceSQLThread(int operation_id, Statement[] statements, String[] sql_statements, AceThread cthread, Object user_parm) throws IOException {
            super("AceSQLThread", true);
            this.statements = statements;
            sqlStatements = sql_statements;
            userParm = user_parm;
            parent = cthread;
            operationId = operation_id;
        }

        public void dispose() {
            if (quitThread == false) {
                quitThread = true;
                try {
                    if (currentStatement != null) {
                        currentStatement.cancel();
                    }
                } catch (SQLException ex) {
                    ;
                }
            }
            super.dispose();
        }

        public void run() {
            pendingOperations.put(operationId, this);
            int num_executed = 0;
            try {
                boolean result_available = false;
                ArrayList<ResultSet> results = new ArrayList<ResultSet>();
                int num_affected_rows = 0;
                int num_to_execute = (sqlStatements == null) ? statements.length : sqlStatements.length;
                for (; num_executed < num_to_execute; num_executed++) {
                    currentStatement = (statements.length == 1) ? statements[0] : statements[num_executed];
                    if ((currentStatement instanceof PreparedStatement) == true) {
                        result_available = ((PreparedStatement) currentStatement).execute();
                    } else {
                        result_available = currentStatement.execute(sqlStatements[num_executed]);
                    }
                    if (result_available == true) {
                        ResultSet rs = currentStatement.getResultSet();
                        results.add(rs);
                    } else {
                        num_affected_rows = currentStatement.getUpdateCount();
                    }
                }
                ResultSet r = null;
                ResultSet[] multiple_r = null;
                if (results.size() > 0) {
                    r = ((ResultSet) (results.get(results.size() - 1)));
                    if (statements.length > 1) {
                        multiple_r = new ResultSet[results.size()];
                        results.toArray(multiple_r);
                    }
                }
                if ((quit == false) && (quitThread == false)) {
                    if (parent.sendMessage(new AceSQLMessage(AceSQLMessage.SQL_EXECUTED, operationId, r, multiple_r, num_affected_rows, parent, this, num_executed, userParm)) == false) {
                        AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, parent.getName() + "- AceSQL.AceSQLThread.run() -- Error sending SQL executed message : " + getErrorMessage());
                    }
                }
            } catch (SQLException ex) {
                if ((quit == false) && (quitThread == false)) {
                    AceLogger.Instance().log(AceLogger.WARNING, AceLogger.SYSTEM_LOG, parent.getName() + " -- Unexpected database result : " + ex.getMessage());
                    if (parent.sendMessage(new AceSQLMessage(AceSQLMessage.SQL_ERROR, operationId, parent, this, num_executed, userParm)) == false) {
                        AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, parent.getName() + "- AceSQL.AceSQLThread.run() -- Error sending SQL error message : " + getErrorMessage());
                    }
                }
            }
            currentStatement = null;
            dispose();
            pendingOperations.remove(new Integer(operationId));
            return;
        }
    }

    private boolean quit = false;

    private Connection dbConnection = null;

    private Hashtable<Integer, AceSQLThread> pendingOperations = new Hashtable<Integer, AceSQLThread>();

    private static Object nextOperationIdLock = new Object();

    private static int nextOperationId = 0;

    public AceSQL(Connection db_conn) {
        dbConnection = db_conn;
    }

    public boolean cancelSQL(int id) {
        return cancelSQL(id, null);
    }

    public boolean cancelSQL(int id, AceThread cthread) {
        boolean ret = false;
        AceSQLThread thr = (AceSQLThread) pendingOperations.get(new Integer(id));
        if (thr != null) {
            thr.dispose();
            ret = true;
        }
        if (cthread == null) {
            Thread cur_thr = Thread.currentThread();
            if ((cur_thr instanceof AceThread) == true) {
                cthread = (AceThread) cur_thr;
            } else {
                return ret;
            }
        }
        ret = cthread.removeMessage(new AceSQLMessage(0, id, cthread, cthread, 0, null), this);
        return ret;
    }

    public void dispose() {
        if (quit == false) {
            quit = true;
            try {
                if (dbConnection != null) {
                    dbConnection.close();
                    dbConnection = null;
                }
            } catch (SQLException ex) {
                AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "AceSQL.dispose() -- Database error while closing connection : " + ex.getMessage());
            }
        }
    }

    public int executeSQL(Statement statement, String sql_statement, Object user_parm) {
        Statement[] statements = new Statement[1];
        statements[0] = statement;
        String[] sql_statements = new String[1];
        sql_statements[0] = sql_statement;
        return executeSQL(statements, sql_statements, null, user_parm, false);
    }

    public int executeSQL(Statement statement, String[] sql_statements, Object user_parm) {
        Statement[] statements = new Statement[1];
        statements[0] = statement;
        return executeSQL(statements, sql_statements, null, user_parm, false);
    }

    public int executeSQL(Statement statement, String[] sql_statements, Object user_parm, boolean return_multiple_query_results) {
        Statement[] statements = new Statement[1];
        statements[0] = statement;
        return executeSQL(statements, sql_statements, null, user_parm, return_multiple_query_results);
    }

    public int executeSQL(Statement[] statements, String[] sql_statements, AceThread cthread, Object user_parm, boolean return_multiple_query_results) {
        int next_id = -1;
        Thread caller = cthread;
        if (caller == null) {
            caller = Thread.currentThread();
        }
        if ((caller instanceof AceThread) == false) {
            writeErrorMessage("The calling thread must be an instance of AceThread", null);
            return -1;
        }
        try {
            if (statements == null) {
                if (return_multiple_query_results == true) {
                    statements = new Statement[sql_statements.length];
                    for (int i = 0; i < statements.length; i++) {
                        statements[i] = dbConnection.createStatement();
                    }
                } else {
                    statements = new Statement[1];
                    statements[0] = dbConnection.createStatement();
                }
            }
        } catch (SQLException ex) {
            writeErrorMessage("Failed to create an SQL Statement object : " + ex.getMessage(), ex);
            return -1;
        }
        try {
            synchronized (nextOperationIdLock) {
                next_id = nextOperationId++;
            }
            AceSQLThread sql = new AceSQLThread(next_id, statements, sql_statements, (AceThread) caller, user_parm);
            sql.start();
        } catch (IOException ex1) {
            writeErrorMessage("Could not create thread to execute the SQL statement : " + ex1.getMessage(), ex1);
            return -1;
        }
        return next_id;
    }

    public int executeSQL(Statement[] statements, String[] sql_statements, Object user_parm) {
        return executeSQL(statements, sql_statements, null, user_parm, true);
    }

    public int executeSQL(String sql_statement, AceThread cthread, Object user_parm) {
        String[] sql_statements = new String[1];
        sql_statements[0] = sql_statement;
        return executeSQL(null, sql_statements, cthread, user_parm, false);
    }

    public int executeSQL(String sql_statement, Object user_parm) {
        String[] sql_statements = new String[1];
        sql_statements[0] = sql_statement;
        return executeSQL(null, sql_statements, null, user_parm, false);
    }

    public int executeSQL(String[] sql_statements, AceThread cthread, Object user_parm, boolean return_multiple_query_results) {
        return executeSQL(null, sql_statements, cthread, user_parm, return_multiple_query_results);
    }

    public int executeSQL(String[] sql_statements, Object user_parm, boolean return_multiple_query_results) {
        return executeSQL(null, sql_statements, null, user_parm, return_multiple_query_results);
    }

    public Connection getConnection() {
        return dbConnection;
    }

    public boolean same(AceMessageInterface obj1, AceMessageInterface obj2) {
        boolean ret = false;
        if (((obj1 instanceof AceSQLMessage) == true) && ((obj2 instanceof AceSQLMessage) == true)) {
            if (((AceSQLMessage) obj1).getOperationId() == ((AceSQLMessage) obj2).getOperationId()) {
                ret = true;
            }
        }
        return ret;
    }

    public AceMessageInterface waitSQLResult(int id) {
        Thread thr = Thread.currentThread();
        if ((thr instanceof AceThread) == false) {
            writeErrorMessage("This method is not being called from an object which is a sub-class of type AceThread", null);
            return null;
        }
        AceThread cthread = (AceThread) thr;
        while (true) {
            AceMessageInterface msg = cthread.waitMessage();
            if ((msg instanceof AceSQLMessage) == true) {
                if (((AceSQLMessage) msg).getOperationId() == id) {
                    return msg;
                }
            } else if ((msg instanceof AceSignalMessage) == true) {
                return msg;
            }
        }
    }

    private void writeErrorMessage(String error, Throwable e) {
        Thread cthread = Thread.currentThread();
        if ((cthread instanceof AceThread) == true) {
            ((AceThread) cthread).dispatchErrorMessage(error, e);
        } else {
            AceLogger.Instance().log(AceLogger.ERROR, AceLogger.SYSTEM_LOG, "AceSQL.writeErrorMessage() : " + error, e);
        }
    }
}
