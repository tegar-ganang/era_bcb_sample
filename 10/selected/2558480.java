package com.apelon.dts.db.subset.expression;

import com.apelon.common.util.db.dao.GeneralDAO;
import com.apelon.common.sql.SQL;
import com.apelon.common.log4j.Categories;
import com.apelon.dts.common.DTSValidationException;
import com.apelon.beans.apelmsg.ApelMsgHandler;
import java.sql.*;
import java.util.ArrayList;

/**
 * <p>Title: ResultsAccumulator </p>
 * <p>Description: This class executes a series of statements to get the results of the subset expression (which
 * is composed of several treepaths) </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Apelon Inc. </p>
 *
 * @author Apelon Inc.
 * @version DTS 3.4.0
 * @since 3.4.0
 */
public class CacheDBResultsAccumulator extends ResultsAccumulator {

    public CacheDBResultsAccumulator(GeneralDAO dao, Connection con, int subsetId) {
        super(dao, con, subsetId);
    }

    /**
   * Returns an ArrayList of namespaces involved in the list of TreePathWalker provided.
   * Note: This will get an empty ArrayList when called before getExpressionResults()
   * @return an arraylist of namespaces associated with the tree
   */
    public ArrayList getNamespaces() {
        return namespaces;
    }

    public void cleanup() throws SQLException {
        try {
            dropTempTable(tempIncTableName);
            dropTempTable(tempExcTableName);
            dropTempTable(tempResTableName);
            dropTempTable(tempIntersectTbl1);
            dropTempTable(tempIntersectTbl2);
            dropTempTable(tempMinusTbl1);
            dropTempTable(tempMinusTbl2);
        } catch (Exception ex) {
            Categories.dataDb().error("Error while cleaning up temp tables.");
        }
    }

    private void dropTempTable(String tableName) {
        if (!tableName.equals("")) {
            try {
                SQL.dropTable(this.conn, tableName);
            } catch (SQLException e) {
                Categories.dataDb().warn("Unable to drop temp table " + tableName);
            }
        }
    }

    /**
    * Computes the resultset (list of concept gids) of an subset expression for an expression (for sql2000 kb)
    * Note: Since there are no set operators like MINUS, INTERSECT in SQL2000, the set operations on the
    * nodes are accomplished via 2 stored procedures (INTERSECT and MINUS) and few temp tables.
    */
    protected ArrayList computeConceptList(ArrayList treePaths) throws SQLException, DTSValidationException {
        prepare();
        ArrayList finalConList = new ArrayList(0);
        for (int i = 0; i < treePaths.size(); i++) {
            TreePathWalker tpw = (TreePathWalker) treePaths.get(i);
            TreePathStmtProcessor tpsp = new TreePathStmtProcessor(tpw, this.dao, this.conn);
            tpsp.process();
            int namespaceId = tpsp.getTreeNamespaceId();
            if (!this.namespaces.contains(new Integer(namespaceId)) && (namespaceId > 0)) {
                this.namespaces.add(new Integer(namespaceId));
            }
            ArrayList treePathResults = getTreePathResults(tpsp);
            Categories.dataDb().debug(" # of matches [" + treePathResults.size() + "]");
            for (int r = 0; r < treePathResults.size(); r++) {
                Long con = (Long) treePathResults.get(r);
                if (!finalConList.contains(con)) {
                    finalConList.add(con);
                }
            }
        }
        return finalConList;
    }

    private void prepare() throws SQLException {
        String randomIdentifier = (Double.toString(Math.random())).substring(2, 6);
        this.tempIncTableName = String.valueOf("TEMP_INC_" + System.currentTimeMillis() + randomIdentifier);
        this.setUpTables(this.tempIncTableName);
        this.selectAllFromTempInc = this.dao.getStatement("SUBSET_DB", "SELECT_FROM_TABLE");
        this.selectAllFromTempInc = this.dao.getStatement(this.selectAllFromTempInc, 1, this.tempIncTableName);
        this.tempExcTableName = String.valueOf("TEMP_EXC_" + (System.currentTimeMillis() + 1) + randomIdentifier);
        this.setUpTables(this.tempExcTableName);
        this.selectAllFromTempExc = this.dao.getStatement("SUBSET_DB", "SELECT_FROM_TABLE");
        this.selectAllFromTempExc = this.dao.getStatement(this.selectAllFromTempExc, 1, this.tempExcTableName);
        this.tempResTableName = String.valueOf("TEMP_RES_" + (System.currentTimeMillis() + 1) + randomIdentifier);
        this.setUpTables(this.tempResTableName);
        tempIntersectTbl1 = "TEMP_DTS_SUBSET_INTERSECT1";
        setUpTables(tempIntersectTbl1);
        tempIntersectTbl2 = "TEMP_DTS_SUBSET_INTERSECT2";
        setUpTables(tempIntersectTbl2);
        tempMinusTbl1 = "TEMP_DTS_SUBSET_MINUS1";
        setUpTables(tempMinusTbl1);
        tempMinusTbl2 = "TEMP_DTS_SUBSET_MINUS2";
        setUpTables(tempMinusTbl2);
    }

    private ArrayList getTreePathResults(TreePathStmtProcessor tpsp) throws SQLException, DTSValidationException {
        ArrayList results = new ArrayList(0);
        ArrayList includeStmts = tpsp.getIncludeStmts();
        ArrayList includeCons = new ArrayList(0);
        long beg = System.currentTimeMillis();
        if (includeStmts.size() > 0) {
            includeCons = getAllStmtResults(includeStmts, this.tempIncTableName);
            long end = System.currentTimeMillis();
            Categories.dataDb().debug("Number of include concepts computed = [" + includeCons.size() + "] " + " (time taken = " + (end - beg) / 1000.00 + " secs)");
        }
        ArrayList excludeStmts = tpsp.getExludeStmts();
        beg = System.currentTimeMillis();
        if (excludeStmts.size() > 0) {
            ArrayList excludeCons = getAllStmtResults(excludeStmts, this.tempExcTableName);
            long end = System.currentTimeMillis();
            Categories.dataDb().debug("Number of exclude concepts computed = [" + excludeCons.size() + "] " + " (time taken = " + (end - beg) / 1000.00 + " secs)");
            Categories.dataDb().debug("Computing final results using spMINUS...");
            ArrayList cons = doMinus(this.selectAllFromTempInc, this.selectAllFromTempExc);
            results = cons;
        } else {
            results = includeCons;
        }
        return results;
    }

    private ArrayList getAllStmtResults(ArrayList stmtArray, String tempTableName) throws SQLException, DTSValidationException {
        String selectFromTempTable = " SELECT con FROM " + tempTableName;
        int origTransLevel = Utilities.beginTransaction(this.conn);
        try {
            if (stmtArray.size() == 1) {
                ArrayList cons = this.executeQuery((String) stmtArray.get(0));
                this.saveToTempTable(cons, tempTableName, true);
            } else {
                int pos = 1;
                String stmt = (String) stmtArray.get(0);
                if (stmt != null && stmt.equals("")) {
                    stmt = (String) stmtArray.get(pos++);
                }
                ArrayList cons = this.executeQuery(stmt);
                this.saveToTempTable(cons, tempTableName, true);
                while (pos < stmtArray.size()) {
                    String nextStmt = (String) stmtArray.get(pos++);
                    ArrayList nextCons = doIntersect(selectFromTempTable, nextStmt);
                    this.saveToTempTable(nextCons, tempTableName, true);
                }
            }
            this.conn.commit();
        } catch (SQLException ex) {
            this.conn.rollback();
            throw ex;
        } finally {
            Utilities.endTransaction(this.conn, origTransLevel);
        }
        return this.executeQuery(" SELECT con FROM " + tempTableName);
    }

    private void setUpTables(String tempTableName) throws SQLException {
        if (!SQL.checkTableExists(conn, tempTableName)) {
            String createTableStmt = "CREATE GLOBAL TEMPORARY TABLE " + tempTableName + " (con numeric NOT NULL)";
            Statement stmt = null;
            try {
                stmt = this.conn.createStatement();
                int res = stmt.executeUpdate(createTableStmt);
                if (res > 0) {
                    Categories.dataDb().debug("Successfully created temp table [" + tempTableName + "]");
                }
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        } else {
            String createTableStmt = "TRUNCATE TABLE " + tempTableName;
            Statement stmt = null;
            try {
                stmt = this.conn.createStatement();
                int res = stmt.executeUpdate(createTableStmt);
                if (res > 0) {
                    Categories.dataDb().debug("Successfully truncated temp table [" + tempTableName + "]");
                }
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        }
    }

    /**
   * Executes intersect with the given 2 queries.
   */
    private ArrayList doIntersect(String query1, String query2) throws SQLException, DTSValidationException {
        Statement stmt = null;
        try {
            stmt = this.conn.createStatement();
            ResultSet rs = doIntersect(stmt, query1, query2);
            ArrayList cons = getConList(rs);
            rs.close();
            return cons;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
   * Executes minus with the given 2 queries.
   */
    private ArrayList doMinus(String query1, String query2) throws SQLException {
        Statement stmt = null;
        try {
            stmt = this.conn.createStatement();
            ResultSet rs = doMinus(stmt, query1, query2);
            ArrayList cons = getConList(rs);
            rs.close();
            return cons;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private ArrayList getConList(ResultSet rs) throws SQLException {
        ArrayList cons = new ArrayList(10);
        while (rs.next()) {
            long conGid = rs.getLong(1);
            cons.add(new Long(conGid));
        }
        return cons;
    }

    private ResultSet doIntersect(Statement stmt, String q1, String q2) throws SQLException {
        stmt.executeUpdate("INSERT INTO " + tempIntersectTbl1 + " " + q1);
        stmt.executeUpdate("INSERT INTO " + tempIntersectTbl2 + " " + q2);
        return stmt.executeQuery("SELECT t1.con FROM " + tempIntersectTbl1 + " t1 " + "WHERE EXISTS (SELECT t2.con FROM " + tempIntersectTbl2 + " t2 WHERE t2.con = t1.con)");
    }

    private ResultSet doMinus(Statement stmt, String q1, String q2) throws SQLException {
        stmt.executeUpdate("INSERT INTO " + tempMinusTbl1 + " " + q1);
        stmt.executeUpdate("INSERT INTO " + tempMinusTbl2 + " " + q2);
        return stmt.executeQuery("SELECT t1.con FROM " + tempMinusTbl1 + " t1 " + "WHERE NOT EXISTS (SELECT t2.con FROM " + tempMinusTbl2 + " t2 WHERE t2.con = t1.con)");
    }

    private int executeUpdate(String updateQuery) throws SQLException {
        Statement stmt = null;
        int rows = 0;
        try {
            stmt = this.conn.createStatement();
            rows = stmt.executeUpdate(updateQuery);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
        return rows;
    }

    private int saveToTempTable(ArrayList cons, String tempTableName, boolean truncateFirst) throws SQLException {
        if (truncateFirst) {
            this.executeUpdate("TRUNCATE TABLE " + tempTableName);
            Categories.dataDb().debug("TABLE " + tempTableName + " TRUNCATED.");
        }
        PreparedStatement ps = null;
        int rows = 0;
        try {
            String insert = "INSERT INTO " + tempTableName + " VALUES (?)";
            ps = this.conn.prepareStatement(insert);
            for (int i = 0; i < cons.size(); i++) {
                ps.setLong(1, ((Long) cons.get(i)).longValue());
                rows = ps.executeUpdate();
                if ((i % 500) == 0) {
                    this.conn.commit();
                }
            }
            this.conn.commit();
        } catch (SQLException sqle) {
            this.conn.rollback();
            throw sqle;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
        return rows;
    }

    private String tempIncTableName = "";

    private String tempExcTableName = "";

    private String tempResTableName = "";

    private String selectAllFromTempInc = "";

    private String selectAllFromTempExc = "";

    private String tempIntersectTbl1 = "";

    private String tempIntersectTbl2 = "";

    private String tempMinusTbl1 = "";

    private String tempMinusTbl2 = "";
}
