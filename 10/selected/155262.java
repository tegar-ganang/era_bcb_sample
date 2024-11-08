package com.apelon.dts.db.subset.expression;

import com.apelon.common.util.db.dao.GeneralDAO;
import com.apelon.common.log4j.Categories;
import com.apelon.common.sql.SQL;
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
public class SqlServerResultsAccumulator extends ResultsAccumulator {

    public SqlServerResultsAccumulator(GeneralDAO dao, Connection con, int subsetId) {
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
            if (!this.tempIncTableName.equals("")) {
                SQL.dropTable(this.conn, this.tempIncTableName);
            }
            if (!this.tempExcTableName.equals("")) {
                SQL.dropTable(this.conn, this.tempExcTableName);
            }
            if (!this.tempResTableName.equals("")) {
                SQL.dropTable(this.conn, this.tempResTableName);
            }
            SQL.dropTable(this.conn, "##t1");
            SQL.dropTable(this.conn, "##t2");
            SQL.dropTable(this.conn, "##t3");
            SQL.dropTable(this.conn, "##t4");
        } catch (Exception ex) {
            Categories.dataDb().error("Error while cleaning up temp tables.");
        }
    }

    /**
    * Computes the resultset (list of concept gids) of an subset expression for an expression (for sql2000 kb)
    * Note: Since there are no set operators like MINUS, INTERSECT in SQL2000, the set operations on the
    * nodes are accomplished via 2 stored procedures (INTERSECT and MINUS) and few temp tables.
    */
    protected ArrayList computeConceptList(ArrayList treePaths) throws SQLException, DTSValidationException {
        prepareSql2k();
        String minusSp = this.dao.getStatement("SUBSET_DB", "MINUS_SP_NAME");
        String intersectSp = this.dao.getStatement("SUBSET_DB", "INTERSECT_SP_NAME");
        ArrayList finalConList = new ArrayList(0);
        for (int i = 0; i < treePaths.size(); i++) {
            TreePathWalker tpw = (TreePathWalker) treePaths.get(i);
            TreePathStmtProcessor tpsp = new TreePathStmtProcessor(tpw, this.dao, this.conn);
            tpsp.process();
            int namespaceId = tpsp.getTreeNamespaceId();
            if (!this.namespaces.contains(new Integer(namespaceId)) && (namespaceId > 0)) {
                this.namespaces.add(new Integer(namespaceId));
            }
            ArrayList treePathResults = getTreePathResults(tpsp, minusSp, intersectSp);
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

    private void prepareSql2k() throws SQLException {
        for (int i = 1; i <= 4; i++) {
            String tableName = "##t" + i;
            if (SQL.checkTableExists(this.conn, tableName)) {
                try {
                    SQL.dropTable(this.conn, tableName);
                } catch (SQLException ex) {
                    Categories.dataDb().debug(" Error while dropping sp temp table [" + tableName + "] :\n" + ex.getMessage());
                }
            }
        }
        String randomIdentifier = (Double.toString(Math.random())).substring(2, 6);
        this.tempIncTableName = String.valueOf("#INC_" + System.currentTimeMillis() + randomIdentifier);
        this.setUpTables(this.tempIncTableName);
        this.selectAllFromTempInc = this.dao.getStatement("SUBSET_DB", "SELECT_FROM_TABLE");
        this.selectAllFromTempInc = this.dao.getStatement(this.selectAllFromTempInc, 1, this.tempIncTableName);
        this.tempExcTableName = String.valueOf("#EXC_" + (System.currentTimeMillis() + 1) + randomIdentifier);
        this.setUpTables(this.tempExcTableName);
        this.selectAllFromTempExc = this.dao.getStatement("SUBSET_DB", "SELECT_FROM_TABLE");
        this.selectAllFromTempExc = this.dao.getStatement(this.selectAllFromTempExc, 1, this.tempExcTableName);
        this.tempResTableName = String.valueOf("#RES_" + (System.currentTimeMillis() + 1) + randomIdentifier);
        this.setUpTables(this.tempResTableName);
    }

    private ArrayList getTreePathResults(TreePathStmtProcessor tpsp, String minusSp, String intersectSp) throws SQLException, DTSValidationException {
        ArrayList results = new ArrayList(0);
        ArrayList includeStmts = tpsp.getIncludeStmts();
        ArrayList includeCons = new ArrayList(0);
        long beg = System.currentTimeMillis();
        if (includeStmts.size() > 0) {
            includeCons = getAllStmtResults(intersectSp, includeStmts, this.tempIncTableName);
            long end = System.currentTimeMillis();
            Categories.dataDb().debug("Number of include concepts computed = [" + includeCons.size() + "] " + " (time taken = " + (end - beg) / 1000.00 + " secs)");
        }
        ArrayList excludeStmts = tpsp.getExludeStmts();
        beg = System.currentTimeMillis();
        if (excludeStmts.size() > 0) {
            ArrayList excludeCons = getAllStmtResults(intersectSp, excludeStmts, this.tempExcTableName);
            long end = System.currentTimeMillis();
            Categories.dataDb().debug("Number of exclude concepts computed = [" + excludeCons.size() + "] " + " (time taken = " + (end - beg) / 1000.00 + " secs)");
            Categories.dataDb().debug("Computing final results using spMINUS...");
            ArrayList cons = runStoreProc(minusSp, this.selectAllFromTempInc, this.selectAllFromTempExc);
            results = cons;
        } else {
            results = includeCons;
        }
        return results;
    }

    private ArrayList getAllStmtResults(String intersectSp, ArrayList stmtArray, String tempTableName) throws SQLException, DTSValidationException {
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
                    ArrayList nextCons = runStoreProc(intersectSp, selectFromTempTable, nextStmt);
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
            String createTableStmt = "CREATE TABLE " + tempTableName + " (con numeric NOT NULL)";
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
   * Executes a stored procedure (minus or intersect) with the given 2 queries.
   * Note: each query has a limit of VARCHAR_ALLOWED_LENGTH=8000 chars
   */
    private ArrayList runStoreProc(String spName, String query1, String query2) throws SQLException, DTSValidationException {
        CallableStatement cs = null;
        Categories.dataDb().debug("Running " + spName + " with following 2 statements \n " + " [ " + query1 + "]\n" + " [ " + query2 + "] ");
        query1 = Utilities.compactQuery(query1);
        Categories.dataDb().debug("Original query length=" + query2.length());
        query2 = Utilities.compactQuery(query2);
        Categories.dataDb().debug("Compact query length=" + query2.length());
        String query2overflow = "";
        if (query2.length() > Utilities.VARCHAR_ALLOWED_LENGTH) {
            Categories.dataDb().debug("Chopping compacted query...");
            query2overflow = query2.substring(Utilities.VARCHAR_ALLOWED_LENGTH - 100, query2.length());
            query2 = query2.substring(0, Utilities.VARCHAR_ALLOWED_LENGTH - 100);
            Categories.dataDb().debug("  First seqment length=" + query2.length());
            Categories.dataDb().debug("  Second segment length=" + query2overflow.length());
            if (query2overflow.length() > Utilities.VARCHAR_ALLOWED_LENGTH) {
                Object[] args = { String.valueOf(query2overflow.length()), spName, String.valueOf(Utilities.VARCHAR_ALLOWED_LENGTH) };
                String exMsg = ApelMsgHandler.getInstance().getMsgText("DTS-0024", args);
                throw new DTSValidationException(exMsg);
            }
        }
        try {
            cs = conn.prepareCall("{? = call " + spName + " (?,?,?)}");
            cs.setString(2, query1);
            cs.setString(3, query2);
            cs.setString(4, query2overflow);
            cs.registerOutParameter(1, Types.NUMERIC);
            ArrayList cons = new ArrayList(10);
            ResultSet rs = cs.executeQuery();
            while (rs.next()) {
                long conGid = rs.getLong(1);
                cons.add(new Long(conGid));
            }
            return cons;
        } catch (SQLException ex) {
            String errorMsg = "Problem executing stored procedure[" + spName + "] (error: + " + ex.getMessage() + ")";
            Categories.dataDb().error(errorMsg);
            throw new SQLException(errorMsg + ":\n" + ex);
        } finally {
            if (cs != null) {
                cs.close();
            }
        }
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
}
