package sqledit;

import java.sql.*;

/**
 *
 * @author alosh
 */
public class QueryHandeler implements QueryManager {

    /** Creates a new instance of Main */
    public QueryHandeler(QueryIDE qIde) {
        action = null;
        updateCount = 0;
        resultsAvailable = false;
        metaAvailable = false;
        planAvailable = false;
        ide = qIde;
        lockConn = null;
        lockPoint = null;
    }

    public void executeQuery(Connection connection, String query) {
        action = null;
        updateCount = 0;
        resultsAvailable = false;
        metaAvailable = false;
        planAvailable = false;
        if (connection == null) {
            ide.setStatus("not connected");
            return;
        }
        cleanUp();
        try {
            ide.setStatus("Executing query");
            stmt = connection.createStatement();
            if (query.toLowerCase().startsWith("select")) {
                result = stmt.executeQuery(query);
                resultsAvailable = true;
                action = "select";
            } else if (query.toLowerCase().startsWith("update")) {
                updateCount = stmt.executeUpdate(query);
                action = "update";
            } else if (query.toLowerCase().startsWith("delete")) {
                updateCount = stmt.executeUpdate(query);
                action = "delete";
            } else if (query.toLowerCase().startsWith("insert")) {
                updateCount = stmt.executeUpdate(query);
                action = "insert";
            } else if (query.toLowerCase().startsWith("commit")) {
                connection.commit();
                action = "commit";
            } else if (query.toLowerCase().startsWith("rollback")) {
                connection.rollback();
                action = "rollback";
            } else if (query.toLowerCase().startsWith("create")) {
                updateCount = stmt.executeUpdate(query);
                action = "create";
            } else if (query.toLowerCase().startsWith("drop")) {
                updateCount = stmt.executeUpdate(query);
                action = "drop";
            } else if (query.toLowerCase().startsWith("desc ")) {
                String objectName = query.substring(query.indexOf(' '), query.length());
                query = "select * from (" + objectName + ") where rownum < 1";
                descQuery(connection, query);
            } else if (query.toLowerCase().startsWith("explain plan for ")) {
                explainQuery(connection, query);
            } else {
                result = stmt.executeQuery(query);
                resultsAvailable = true;
                action = "select";
            }
            ide.setStatus("executed query");
        } catch (Exception e) {
            ide.setStatus(e.getMessage());
        }
    }

    public void descQuery(Connection connection, String query) {
        ResultSet rset;
        action = null;
        updateCount = 0;
        resultsAvailable = false;
        metaAvailable = false;
        planAvailable = false;
        if (connection == null) {
            ide.setStatus("not connected");
            return;
        }
        cleanUp();
        try {
            stmt = connection.createStatement();
            query = "select * from (" + query + ") where rownum < 1";
            rset = stmt.executeQuery(query);
            resultMetaData = rset.getMetaData();
            metaAvailable = true;
            ide.setStatus("query described");
        } catch (Exception e) {
            ide.setStatus(e.getMessage());
        }
    }

    public void explainQuery(Connection connection, String query) {
        action = null;
        updateCount = 0;
        resultsAvailable = false;
        metaAvailable = false;
        planAvailable = false;
        String planQuery = "select substr(lpad(' ', (level-1)*2) || operation || ' (' || options || ')',1,200 ) Operation, ";
        planQuery = planQuery + "object_name Object, cost, cardinality, time from plan_table ";
        planQuery = planQuery + " start with id = 0 connect by prior id=parent_id";
        if (connection == null) {
            ide.setStatus("not connected");
            return;
        }
        cleanUp();
        if (!query.toLowerCase().startsWith("explain plan")) query = "explain plan for (" + query + ")";
        try {
            stmt = connection.createStatement();
            Savepoint savepoint = connection.setSavepoint();
            stmt.executeUpdate("delete from plan_table");
            stmt.executeQuery(query);
            result = stmt.executeQuery(planQuery);
            planAvailable = true;
            ide.setStatus("query explained");
            lockConn = connection;
            lockPoint = savepoint;
        } catch (Exception e) {
            System.out.println("" + e);
            ide.setStatus(e.getMessage());
        }
    }

    public void displayResults() {
        if (resultsAvailable) displayTable(); else if (metaAvailable) displayMeta(); else if (planAvailable) displayPlan(); else {
            ide.displayUpdateMesg(action, updateCount);
        }
    }

    private void displayTable() {
        ide.displayTableResults(result);
    }

    private void displayMeta() {
        ide.displayQueryMeta(resultMetaData);
    }

    private void displayPlan() {
        System.out.println("calling plan display");
        ide.displayQueryPlan(result);
        if (lockConn != null && lockPoint != null) {
            try {
                lockConn.rollback(lockPoint);
                lockConn = null;
                lockPoint = null;
            } catch (Exception e) {
                System.out.println("error releasing lock: " + e);
            }
        }
    }

    public void cleanUp() {
        try {
            if (stmt != null) stmt.close();
        } catch (Exception e) {
            ide.setStatus(e.getMessage());
        }
    }

    public void closeConnection(Connection conn) {
        try {
            conn.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        ide.closeEnv();
    }

    private QueryIDE ide;

    private Statement stmt;

    private ResultSet result;

    private ResultSetMetaData resultMetaData;

    private boolean resultsAvailable;

    private boolean metaAvailable;

    private boolean planAvailable;

    private int updateCount;

    private String action;

    private Connection lockConn;

    private Savepoint lockPoint;
}
