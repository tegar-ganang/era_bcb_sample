package com.knowgate.dataobjs;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Date;
import com.knowgate.debug.DebugFile;
import com.knowgate.jdc.JDCConnection;
import com.knowgate.misc.Gadgets;
import com.knowgate.misc.CSVParser;

/**
 *
 * <p>A bidimensional array representing data readed from a database table.</p>
 * The DBSubset object is used for reading a collection of registers from the database
 * and kept them in memory for inmediate access.
 * As hipergate reuses database connections by recycling them at the {@link JDCConnectionPool},
 * it is a good programming practice to read just the needed number of registers in a single
 * step operation and then free the connection as soon as possible for being used by another processs.
 * Another reason for this "read fast and leave" tactic is that some JDBC drivers have problems
 * managing multiple open resultsets with pending results in a single connection.
 * @author Sergio Montoro Ten
 * @version 2.1
 */
public final class DBSubset {

    /**
   * </p>Contructs a DBSubset.</p>
   * @param sTableName Base table or tables, ie. "k_products" or "k_products p, k_x_cat_objs x"
   * @param sColumnList Column list to be retirved from base tables i.e "p.gu_product,p.nm_product"
   * @param sFilterClause SQL filter clause or <b>null</b> if there is no filter clause to be applied.
   * @param iFetchSize Space for number of rows initailly allocated. Is DBSubset later loads more rows
   * the buffer is automatically expanded. This parameter may also have a great effect on reducing
   * network round trips as the ResultSet.setFetchSize(iFetchSize) method is called prior to fetching
   * rows. Fetching rows in batches is much faster than doing so one by one. When iFetchSize is set,
   * the JDBC driver may optimize accesses to fetched rows by reading bursts and
   * caching rows at client side.
   */
    public DBSubset(String sTableName, String sColumnList, String sFilterClause, int iFetchSize) {
        if (DebugFile.trace) DebugFile.writeln("new DBSubset(" + sTableName + "," + sColumnList + "," + sFilterClause + "," + String.valueOf(iFetchSize));
        sTable = sTableName;
        sColList = sColumnList;
        if (null != sFilterClause) {
            sFilter = sFilterClause;
            if (sFilter.length() > 0) sSelect = "SELECT " + sColList + " FROM " + sTable + " WHERE " + sFilter; else sSelect = "SELECT " + sColList + " FROM " + sTable;
        } else {
            sFilter = "";
            sSelect = "SELECT " + sColList + " FROM " + sTable;
        }
        if (DebugFile.trace) DebugFile.writeln(sSelect);
        oResults = null;
        sInsert = "";
        iFetch = iFetchSize;
        iColCount = 0;
        iMaxRows = -1;
        iTimeOut = 60;
        bEOF = true;
        sColDelim = "`";
        sRowDelim = "�";
        sTxtQualifier = "\"";
        oShortDate = null;
    }

    public int clear(Connection oConn, Object[] aFilterValues) throws SQLException {
        int iAffected = 0;
        PreparedStatement oStmt;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.clear([Connection], Object[])");
            DebugFile.incIdent();
        }
        if (sFilter.length() > 0) oStmt = oConn.prepareStatement("DELETE FROM " + sTable + " WHERE " + sFilter); else oStmt = oConn.prepareStatement("DELETE FROM " + sTable);
        try {
            oStmt.setQueryTimeout(iTimeOut);
        } catch (SQLException sqle) {
            if (DebugFile.trace) DebugFile.writeln("Error at PreparedStatement.setQueryTimeout(" + String.valueOf(iTimeOut) + ")" + sqle.getMessage());
        }
        for (int c = 0; c < aFilterValues.length; c++) oStmt.setObject(c + 1, aFilterValues[c]);
        iAffected = oStmt.executeUpdate();
        oStmt.close();
        oResults = null;
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.clear()");
        }
        return iAffected;
    }

    /**
   * @return <b>true</b> if call() or load() methods readed all available rows,
   * <b>false</b> if more rows where pending of reading when getMaxRows() was
   * reached.<br>
   * Also, after calling store(), eof() is <b>true</b> if all rows were stored
   * successfully or <b>false</b> if any row failed to be inserted or updated.
   */
    public boolean eof() {
        return bEOF;
    }

    /**
   * @return Maximum number of rows to be readed from database
   */
    public int getMaxRows() {
        return iMaxRows;
    }

    /**
   * <p>Maximum number of rows to be readed from database</p>
   * <p>The exact behavior of this property depends on the RDBMS used.</p>
   * <p>For <b>PostgreSQL</b> [LIMIT n] clause is appended to DBSubset query and all returned rows are readed.</p>
   * <p>For <b>Microsoft SQL Server</b> [OPTION FAST(n)] clause is appended to DBSubset query
   * and then the number of readed rows is limited at client side fetching.</p>
   * <p>For <b>Oracle</b> there is no standard way of limiting the resultset size.
   * The number of readed rows is just limited at client side fetching.</p>
   */
    public void setMaxRows(int iMax) {
        iMaxRows = iMax;
    }

    /**
   * @return Maximum amount of time in seconds that a query may run before being cancelled.
   */
    public int getQueryTimeout() {
        return iTimeOut;
    }

    /**
   *
   * @param iMaxSeconds
   */
    public void setQueryTimeout(int iMaxSeconds) {
        iTimeOut = iMaxSeconds;
    }

    private void setFetchSize(JDCConnection oConn, ResultSet oRSet) throws SQLException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.setFetchSize()");
            DebugFile.incIdent();
        }
        if (oConn.getDataBaseProduct() == JDCConnection.DBMS_POSTGRESQL) iFetch = 1; else {
            try {
                if (0 != iFetch) oRSet.setFetchSize(iFetch); else iFetch = oRSet.getFetchSize();
            } catch (SQLException e) {
                if (DebugFile.trace) DebugFile.writeln(e.getMessage());
                iFetch = 1;
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.setFetchSize() : " + iFetch);
        }
    }

    private int fetchResultSet(ResultSet oRSet, int iSkip) throws SQLException, ArrayIndexOutOfBoundsException {
        Vector oRow;
        int iCol;
        int iRetVal = 0;
        int iMaxRow = iMaxRows < 0 ? 2147483647 : iMaxRows;
        long lFetchTime = 0;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.fetchResultSet([ResultSet], " + String.valueOf(iSkip) + ")");
            DebugFile.incIdent();
            DebugFile.writeln("column count = " + String.valueOf(iColCount));
            DebugFile.writeln("max. rows = " + String.valueOf(iMaxRows));
            DebugFile.writeln("new Vector(" + String.valueOf(iFetch) + "," + String.valueOf(iFetch) + ")");
            lFetchTime = System.currentTimeMillis();
        }
        oResults = new Vector(iFetch, iFetch);
        if (0 != iSkip) {
            oRSet.next();
            if (DebugFile.trace) DebugFile.writeln("ResultSet.relative(" + String.valueOf(iSkip - 1) + ")");
            oRSet.relative(iSkip - 1);
        }
        boolean bHasNext = oRSet.next();
        while (bHasNext && iRetVal < iMaxRow) {
            iRetVal++;
            oRow = new Vector(iColCount);
            for (iCol = 1; iCol <= iColCount; iCol++) oRow.add(oRSet.getObject(iCol));
            oResults.add(oRow);
            bHasNext = oRSet.next();
        }
        if (0 == iRetVal || iRetVal < iMaxRow) {
            bEOF = true;
            if (DebugFile.trace) DebugFile.writeln("readed " + String.valueOf(iRetVal) + " rows eof() = true");
        } else {
            bEOF = !bHasNext;
            if (DebugFile.trace) DebugFile.writeln("readed max " + String.valueOf(iMaxRow) + " rows eof() = " + String.valueOf(bEOF));
        }
        if (DebugFile.trace) {
            DebugFile.writeln("fetching done in " + String.valueOf(System.currentTimeMillis() - lFetchTime) + " ms");
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.fetchResultSet() : " + String.valueOf(iRetVal));
        }
        return iRetVal;
    }

    /**
   * <p>Execute a stored procedure returning a ResultSet</p>
   * @param oConn Database Connection
   * @return Number of rows retrieved
   * @throws SQLException
   */
    public int call(JDCConnection oConn) throws SQLException {
        return call(oConn, 0);
    }

    /**
   * <p>Execute a stored procedure returning a ResultSet</p>
   * @param oConn Database Connection
   * @param iSkip Number of rows to be skipped before reading
   * @return Number of rows retrieved
   * @throws SQLException
   * @throws IllegalArgumentException if iSkip<0
   * @throws ArrayIndexOutOfBoundsException
   */
    public int call(JDCConnection oConn, int iSkip) throws SQLException, IllegalArgumentException, ArrayIndexOutOfBoundsException {
        CallableStatement oStmt;
        ResultSet oRSet;
        ResultSetMetaData oMDat;
        int iRows = 0;
        int iType = (iSkip == 0 ? ResultSet.TYPE_FORWARD_ONLY : ResultSet.TYPE_SCROLL_INSENSITIVE);
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.call([Connection]," + iSkip + ")");
            DebugFile.incIdent();
        }
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareCall({call " + sTable + "()}");
        oStmt = oConn.prepareCall("{call " + sTable + "()}", iType, ResultSet.CONCUR_READ_ONLY);
        if (DebugFile.trace) DebugFile.writeln("Connection.executeQuery(" + sTable + ")");
        oRSet = oStmt.executeQuery();
        oMDat = oRSet.getMetaData();
        iColCount = oMDat.getColumnCount();
        ColNames = new String[iColCount];
        for (int c = 1; c <= iColCount; c++) {
            ColNames[c - 1] = oMDat.getColumnName(c).toLowerCase();
        }
        oMDat = null;
        setFetchSize(oConn, oRSet);
        iRows = fetchResultSet(oRSet, iSkip);
        oRSet.close();
        oRSet = null;
        oStmt.close();
        oStmt = null;
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.call()");
        }
        return iRows;
    }

    /**
   * <p>Execute a stored procedure returning a ResultSet</p>
   * @param oConn Database Connection
   * @param aFilterValues Values to be binded and JDBC PreparedStatement query paramenters.
   * @return Number of rows retrieved
   * @throws SQLException
   */
    public int call(JDCConnection oConn, Object[] aFilterValues) throws SQLException {
        return call(oConn, aFilterValues, 0);
    }

    /**
   * <p>Execute a stored procedure returning a ResultSet</p>
   * @param oConn Database Connection
   * @param aFilterValues Values to be binded and JDBC PreparedStatement query paramenters.
   * @param iSkip Number of rows to be skipped before reading
   * @return Number of rows retrieved,
   * the maximum number of rows to be retrieved is determined by calling method
   * setMaxRows(), if setMaxRows() is not called before call() then all rows existing are retrieved.
   * @throws SQLException
   * @throws IllegalArgumentException
   * @throws ArrayIndexOutOfBoundsException
   */
    public int call(JDCConnection oConn, Object[] aFilterValues, int iSkip) throws SQLException, IllegalArgumentException, ArrayIndexOutOfBoundsException {
        CallableStatement oStmt;
        ResultSet oRSet;
        ResultSetMetaData oMDat;
        Vector oRow;
        int iRows = 0;
        int iType = (iSkip == 0 ? ResultSet.TYPE_FORWARD_ONLY : ResultSet.TYPE_SCROLL_INSENSITIVE);
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.call([Connection], Object[]," + iSkip + ")");
            DebugFile.incIdent();
        }
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareCall({call " + sTable + "()}");
        oStmt = oConn.prepareCall("{call " + sTable + "()}", iType, ResultSet.CONCUR_READ_ONLY);
        for (int p = 0; p < aFilterValues.length; p++) oStmt.setObject(p + 1, aFilterValues[p]);
        if (DebugFile.trace) DebugFile.writeln("Connection.executeQuery()");
        oRSet = oStmt.executeQuery();
        oMDat = oRSet.getMetaData();
        iColCount = oMDat.getColumnCount();
        ColNames = new String[iColCount];
        for (int c = 1; c <= iColCount; c++) {
            ColNames[c - 1] = oMDat.getColumnName(c).toLowerCase();
        }
        oMDat = null;
        setFetchSize(oConn, oRSet);
        iRows = fetchResultSet(oRSet, iSkip);
        oRSet.close();
        oRSet = null;
        oStmt.close();
        oStmt = null;
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.call()");
        }
        return iRows;
    }

    /**
   * <p>Execute a JDBC Statement and load query ResultSet in an internal bidimensional matrix</p>
   * @param oConn Database Connection
   * @return Number of rows retrieved
   * the maximum number of rows to be retrieved is determined by calling method
   * setMaxRows(), if setMaxRows() is not called before call() then all rows existing are retrieved.
   * @throws SQLException
   */
    public int load(JDCConnection oConn) throws SQLException, ArrayIndexOutOfBoundsException, NullPointerException {
        return load(oConn, 0);
    }

    /**
   * <p>Execute a JDBC Statement and load query ResultSet in an internal bidimensional matrix</p>
   * @param oConn Database Connection
   * @param iSkip Number of rows to be skipped before reading. On database systems that support an
   * OFFSET clause (such as PostgreSQL) the native offset feature of the DBMS is used, in case that
   * the DBMS does not provide offset capabilities, the data is fetched and discarded at client side
   * before returning the DBSubset. Care must be taken when skipping a large number of rows in client
   * side mode as it may cause heavy network traffic and round trips to the database.
   * @return Number of rows retrieved
   * the maximum number of rows to be retrieved is determined by calling method
   * setMaxRows(), if setMaxRows() is not called before call() then all rows existing are retrieved.
   * @throws SQLException
   * @throws IllegalArgumentException if iSkip<0
   * @throws ArrayIndexOutOfBoundsException
   * @throws NullPointerException
   */
    public int load(JDCConnection oConn, int iSkip) throws SQLException, IllegalArgumentException, ArrayIndexOutOfBoundsException, NullPointerException {
        Statement oStmt = null;
        ResultSet oRSet = null;
        ResultSetMetaData oMDat;
        int iRows = 0;
        int iType = (iSkip == 0 ? ResultSet.TYPE_FORWARD_ONLY : ResultSet.TYPE_SCROLL_INSENSITIVE);
        long lQueryTime = 0;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.load([Connection]," + iSkip + ")");
            lQueryTime = System.currentTimeMillis();
        }
        if (iSkip < 0) throw new IllegalArgumentException("row offset must be equal to or greater than zero");
        if (DebugFile.trace) DebugFile.incIdent();
        try {
            oStmt = oConn.createStatement(iType, ResultSet.CONCUR_READ_ONLY);
            if (iMaxRows > 0) {
                switch(oConn.getDataBaseProduct()) {
                    case JDCConnection.DBMS_MSSQL:
                        if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSelect + " OPTION (FAST " + String.valueOf(iMaxRows) + ")" + ")");
                        oRSet = oStmt.executeQuery(sSelect + " OPTION (FAST " + String.valueOf(iMaxRows) + ")");
                        break;
                    case JDCConnection.DBMS_POSTGRESQL:
                    case JDCConnection.DBMS_MYSQL:
                        if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSelect + " LIMIT " + String.valueOf(iMaxRows + 2) + " OFFSET " + String.valueOf(iSkip) + ")");
                        oRSet = oStmt.executeQuery(sSelect + " LIMIT " + String.valueOf(iMaxRows + 2) + " OFFSET " + String.valueOf(iSkip));
                        iSkip = 0;
                        break;
                    default:
                        if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSelect + ")");
                        oRSet = oStmt.executeQuery(sSelect);
                }
            } else {
                switch(oConn.getDataBaseProduct()) {
                    case JDCConnection.DBMS_POSTGRESQL:
                        if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSelect + " OFFSET " + String.valueOf(iSkip) + ")");
                        oRSet = oStmt.executeQuery(sSelect + " OFFSET " + String.valueOf(iSkip));
                        iSkip = 0;
                        break;
                    default:
                        if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSelect + ")");
                        oRSet = oStmt.executeQuery(sSelect);
                }
            }
            if (DebugFile.trace) {
                DebugFile.writeln("query executed in " + String.valueOf(System.currentTimeMillis() - lQueryTime) + " ms");
                DebugFile.writeln("ResultSet.getMetaData()");
            }
            oMDat = oRSet.getMetaData();
            iColCount = oMDat.getColumnCount();
            ColNames = new String[iColCount];
            for (int c = 1; c <= iColCount; c++) {
                ColNames[c - 1] = oMDat.getColumnName(c).toLowerCase();
            }
            oMDat = null;
            setFetchSize(oConn, oRSet);
            iRows = fetchResultSet(oRSet, iSkip);
            if (DebugFile.trace) DebugFile.writeln("ResultSet.close()");
            oRSet.close();
            oRSet = null;
            if (DebugFile.trace) DebugFile.writeln("PreparedStatement.close()");
            oStmt.close();
            oStmt = null;
        } catch (SQLException sqle) {
            try {
                if (null != oRSet) oRSet.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            try {
                if (null != oStmt) oStmt.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            throw new SQLException(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
        } catch (ArrayIndexOutOfBoundsException aiob) {
            try {
                if (null != oRSet) oRSet.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            try {
                if (null != oStmt) oStmt.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            throw new ArrayIndexOutOfBoundsException("DBSubset.load() " + aiob.getMessage());
        } catch (NullPointerException npe) {
            try {
                if (null != oRSet) oRSet.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            try {
                if (null != oStmt) oStmt.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            throw new NullPointerException("DBSubset.load()");
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.load()");
        }
        return iRows;
    }

    /**
   * <p>Execute a JDBC Statement and load query ResultSet in an internal bidimensional matrix</p>
   * @param oConn Database Connection
   * @param aFilterValues Values to be binded and JDBC PreparedStatement query paramenters.
   * @return Number of rows retrieved
   * the maximum number of rows to be retrieved is determined by calling method
   * setMaxRows(), if setMaxRows() is not called before call() then all rows existing are retrieved.
   * @throws SQLException
   */
    public int load(JDCConnection oConn, Object[] aFilterValues) throws SQLException, ArrayIndexOutOfBoundsException, NullPointerException {
        return load(oConn, aFilterValues, 0);
    }

    public int load(JDCConnection oConn, Object[] aFilterValues, int iSkip) throws SQLException, IllegalArgumentException, ArrayIndexOutOfBoundsException, NullPointerException {
        PreparedStatement oStmt = null;
        ResultSet oRSet = null;
        ResultSetMetaData oMDat;
        Vector oRow;
        int iRows = 0;
        int iType = (iSkip == 0 ? ResultSet.TYPE_FORWARD_ONLY : ResultSet.TYPE_SCROLL_INSENSITIVE);
        long lQueryTime = 0;
        if (DebugFile.trace) DebugFile.writeln("Begin DBSubset.load([Connection], Object[]," + iSkip + ")");
        if (iSkip < 0) throw new IllegalArgumentException("row offset must be equal to or greater than zero");
        if (DebugFile.trace) DebugFile.incIdent();
        try {
            if (iMaxRows > 0) {
                switch(oConn.getDataBaseProduct()) {
                    case JDCConnection.DBMS_MSSQL:
                        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSelect + " OPTION (FAST " + String.valueOf(iMaxRows) + ")" + ")");
                        oStmt = oConn.prepareStatement(sSelect + " OPTION (FAST " + String.valueOf(iMaxRows) + ")", iType, ResultSet.CONCUR_READ_ONLY);
                        break;
                    case JDCConnection.DBMS_POSTGRESQL:
                    case JDCConnection.DBMS_MYSQL:
                        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSelect + " LIMIT " + String.valueOf(iMaxRows + 2) + " OFFSET " + String.valueOf(iSkip) + ")");
                        oStmt = oConn.prepareStatement(sSelect + " LIMIT " + String.valueOf(iMaxRows + 2) + " OFFSET " + String.valueOf(iSkip), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                        iSkip = 0;
                        break;
                    default:
                        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSelect + ")");
                        oStmt = oConn.prepareStatement(sSelect, iType, ResultSet.CONCUR_READ_ONLY);
                }
            } else {
                switch(oConn.getDataBaseProduct()) {
                    case JDCConnection.DBMS_POSTGRESQL:
                        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSelect + " OFFSET " + String.valueOf(iSkip) + ")");
                        oStmt = oConn.prepareStatement(sSelect + " OFFSET " + String.valueOf(iSkip), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                        iSkip = 0;
                        break;
                    default:
                        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSelect + ")");
                        oStmt = oConn.prepareStatement(sSelect, iType, ResultSet.CONCUR_READ_ONLY);
                }
            }
            try {
                oStmt.setQueryTimeout(iTimeOut);
            } catch (SQLException sqle) {
                if (DebugFile.trace) DebugFile.writeln("Error at PreparedStatement.setQueryTimeout(" + String.valueOf(iTimeOut) + ")" + sqle.getMessage());
            }
            for (int p = 0; p < aFilterValues.length; p++) {
                Object oParam = aFilterValues[p];
                if (DebugFile.trace) {
                    if (null == oParam) DebugFile.writeln("PreparedStatement.setObject(" + String.valueOf(p + 1) + ",null)"); else DebugFile.writeln("PreparedStatement.setObject(" + String.valueOf(p + 1) + "," + oParam.toString() + ")");
                }
                oStmt.setObject(p + 1, oParam);
            }
            if (DebugFile.trace) {
                DebugFile.writeln("PreparedStatement.executeQuery()");
                lQueryTime = System.currentTimeMillis();
            }
            oRSet = oStmt.executeQuery();
            if (DebugFile.trace) {
                DebugFile.writeln("query executed in " + String.valueOf(System.currentTimeMillis() - lQueryTime) + " ms");
                DebugFile.writeln("ResultSet.getMetaData()");
            }
            oMDat = oRSet.getMetaData();
            iColCount = oMDat.getColumnCount();
            ColNames = new String[iColCount];
            for (int c = 1; c <= iColCount; c++) {
                ColNames[c - 1] = oMDat.getColumnName(c).toLowerCase();
            }
            oMDat = null;
            setFetchSize(oConn, oRSet);
            iRows = fetchResultSet(oRSet, iSkip);
            if (DebugFile.trace) DebugFile.writeln("ResultSet.close()");
            oRSet.close();
            oRSet = null;
            if (DebugFile.trace) DebugFile.writeln("PreparedStatement.close()");
            oStmt.close();
            oStmt = null;
        } catch (SQLException sqle) {
            try {
                if (null != oRSet) oRSet.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            try {
                if (null != oStmt) oStmt.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            throw new SQLException(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
        } catch (ArrayIndexOutOfBoundsException aiob) {
            try {
                if (null != oRSet) oRSet.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            try {
                if (null != oStmt) oStmt.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            throw new ArrayIndexOutOfBoundsException("DBSubset.load() " + aiob.getMessage());
        } catch (NullPointerException npe) {
            try {
                if (null != oRSet) oRSet.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            try {
                if (null != oStmt) oStmt.close();
            } catch (Exception logit) {
                if (DebugFile.trace) DebugFile.writeln(logit.getClass().getName() + " " + logit.getMessage());
            }
            throw new NullPointerException("DBSubset.load()");
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.load()");
        }
        return iRows;
    }

    /**
   * <p>Find a value in a given column<p>
   * Value is searched by brute force from the begining to the end of the column.<br>
   * Trying to find a <b>null</b> value is allowed.<br>
   * Find is case sensitive.
   * @param iCol Column to be searched [0..getColumnCount()-1]
   * @param oVal Value searched
   * @return Row where seached value was found or -1 is value was not found.
   */
    public int find(int iCol, Object oVal) {
        int iFound = -1;
        int iRowCount;
        Object objCol;
        if (DebugFile.trace) {
            if (null == oVal) DebugFile.writeln("Begin DBSubset.find(" + String.valueOf(iCol) + ", null)"); else DebugFile.writeln("Begin DBSubset.find(" + String.valueOf(iCol) + ", " + oVal.toString() + ")");
            DebugFile.incIdent();
        }
        if (oResults != null) iRowCount = oResults.size(); else iRowCount = -1;
        if (DebugFile.trace) DebugFile.writeln("row count is " + String.valueOf(iRowCount));
        for (int iRow = 0; iRow < iRowCount; iRow++) {
            objCol = get(iCol, iRow);
            if (null != objCol) {
                if (null != oVal) if (objCol.equals(oVal)) {
                    iFound = iRow;
                    break;
                } else if (DebugFile.trace) {
                    DebugFile.writeln("row " + String.valueOf(iRow) + " equals " + objCol.toString());
                }
            } else if (null == oVal) {
                iFound = iRow;
                break;
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("Begin DBSubset.find() : " + String.valueOf(iFound));
        }
        return iFound;
    }

    /**
   * @return Column delimiter for print() and toString() methods.
   */
    public String getColumnDelimiter() {
        return sColDelim;
    }

    /**
   * @param sDelim Column delimiter for print() and toString() methods.
   * The default delimiter is '&grave;' character
   */
    public void setColumnDelimiter(String sDelim) {
        sColDelim = sDelim;
    }

    /**
   * @return Row delimiter for print() and toString() methods.
   */
    public String getRowDelimiter() {
        return sRowDelim;
    }

    /**
   * @param sDelim Row delimiter for print() and toString() methods.
   * The default delimiter is '&uml;' character
   */
    public void setRowDelimiter(String sDelim) {
        sRowDelim = sDelim;
    }

    /**
   * @return Text qualifier for quoting fields in print() and toString() methods.
   */
    public String getTextQualifier() {
        return sTxtQualifier;
    }

    /**
   * @param sQualifier Text qualifier for quoting fields in print() and toString() methods.
   */
    public void setTextQualifier(String sQualifier) {
        sTxtQualifier = sQualifier;
    }

    /**
   * @return Number of columns retrieved.
   */
    public int getColumnCount() {
        return iColCount;
    }

    /**
   * @param sColumnName Name of column witch position is to be returned. Column names are case insensitive.
   * @return Column position or -1 if no column with such name exists.
   */
    public int getColumnPosition(String sColumnName) {
        int iColPos = -1;
        for (int iCol = 0; iCol < iColCount; iCol++) {
            if (sColumnName.equalsIgnoreCase(ColNames[iCol])) {
                iColPos = iCol;
                break;
            }
        }
        return iColPos;
    }

    /**
   * @return number of rows retrieved by last call() or load() method invocation.
   */
    public int getRowCount() {
        int iRows;
        if (null == oResults) iRows = 0; else iRows = oResults.size();
        return iRows;
    }

    /**
   * <p>Get pre-loaded field</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   */
    public Object get(int iCol, int iRow) {
        return ((Vector) oResults.get(iRow)).get(iCol);
    }

    /**
   * <p>Get pre-loaded field by name</p>
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @throws ArrayIndexOutOfBoundsException If no column with such name was found
   */
    public Object get(String sCol, int iRow) throws ArrayIndexOutOfBoundsException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        return ((Vector) oResults.get(iRow)).get(iCol);
    }

    /**
   * <p>Get pre-loaded value for a Boolean field</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @return <b>boolean</b> value for field.
   * @throws ClassCastException
   * @throws ArrayIndexOutOfBoundsException
   * @throws NullPointerException
   */
    public boolean getBoolean(int iCol, int iRow) throws ClassCastException, ArrayIndexOutOfBoundsException, NullPointerException {
        boolean bRetVal;
        Object oObj = get(iCol, iRow);
        if (oObj.getClass().equals(Integer.TYPE)) bRetVal = (((Integer) oObj).intValue() != 0 ? true : false); else if (oObj.getClass().equals(Short.TYPE)) bRetVal = (((Short) oObj).shortValue() != (short) 0 ? true : false); else bRetVal = ((Boolean) get(iCol, iRow)).booleanValue();
        return bRetVal;
    }

    /**
   * <p>Get pre-loaded value for a Date field</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @throws ClassCastException
   * @throws ArrayIndexOutOfBoundsException
   */
    public java.util.Date getDate(int iCol, int iRow) throws ClassCastException, ArrayIndexOutOfBoundsException {
        Object oDt = ((Vector) oResults.get(iRow)).get(iCol);
        if (null != oDt) {
            if (oDt.getClass().equals(ClassUtilDate)) return (java.util.Date) oDt; else if (oDt.getClass().equals(ClassTimestamp)) return new java.util.Date(((java.sql.Timestamp) oDt).getTime()); else if (oDt.getClass().equals(ClassSQLDate)) return new java.util.Date(((java.sql.Date) oDt).getYear(), ((java.sql.Date) oDt).getMonth(), ((java.sql.Date) oDt).getDate()); else throw new ClassCastException("Cannot cast " + oDt.getClass().getName() + " to Date");
        } else return null;
    }

    /**
   * <p>Get pre-loaded value for a Date field</p>
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @throws ClassCastException
   * @throws ArrayIndexOutOfBoundsException if column is not found
   */
    public java.util.Date getDate(String sCol, int iRow) throws ArrayIndexOutOfBoundsException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        return getDate(iCol, iRow);
    }

    /**
   * <p>Get pre-loaded value for a Date field formated as a short Date "yyyy-MM-dd"</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @return String with format "yyyy-MM-dd" or <b>null</b>.
   * @throws ClassCastException
   */
    public String getDateShort(int iCol, int iRow) {
        java.util.Date oDt = getDate(iCol, iRow);
        if (null == oShortDate) oShortDate = new SimpleDateFormat("yyyy-MM-dd");
        if (null != oDt) return oShortDate.format(oDt); else return null;
    }

    /**
   * <p>Get pre-loaded value for a Date field formated as a DateTime "yyyy-MM-dd HH:mm:ss"</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @return String with format "yyyy-MM-dd HH:mm:ss" or <b>null</b>.
   * @throws ClassCastException
   * @since 2.1
   */
    public String getDateTime24(int iCol, int iRow) {
        java.util.Date oDt = getDate(iCol, iRow);
        SimpleDateFormat oDateTime24 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (null != oDt) return oDateTime24.format(oDt); else return null;
    }

    /**
   * <p>Get pre-loaded value for a Date field formated as a DateTime "yyyy-MM-dd hh:mm:ss"</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @throws ClassCastException
   * @throws ArrayIndexOutOfBoundsException
   * @return String with format "yyyy-MM-dd hh:mm:ss" or <b>null</b>.
   */
    public String getDateTime(int iCol, int iRow) throws ClassCastException, ArrayIndexOutOfBoundsException {
        java.util.Date oDt = getDate(iCol, iRow);
        if (null == oDateTime) oDateTime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        if (null != oDt) return oDateTime.format(oDt); else return null;
    }

    /**
   * <p>Get pre-loaded value for a Date field formated with a used defind formar</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @throws ClassCastException
   * @throws ArrayIndexOutOfBoundsException
   * @return Date value formated as String.
   * @see java.text.SimpleDateFormat
   */
    public String getDateFormated(int iCol, int iRow, String sFormat) throws ArrayIndexOutOfBoundsException, ClassCastException {
        java.util.Date oDt = getDate(iCol, iRow);
        SimpleDateFormat oSimpleDate;
        if (null != oDt) {
            oSimpleDate = new SimpleDateFormat(sFormat);
            return oSimpleDate.format(oDt);
        } else return null;
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a Short</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @throws NullPointerException If field value is <b>null</b>
   * @throws ArrayIndexOutOfBoundsException
   */
    public short getShort(int iCol, int iRow) throws NullPointerException, ArrayIndexOutOfBoundsException {
        Object oVal = (((Vector) oResults.get(iRow)).get(iCol));
        Class oCls;
        short iRetVal;
        oCls = oVal.getClass();
        try {
            if (oCls.equals(Short.TYPE)) iRetVal = ((Short) oVal).shortValue(); else if (oCls.equals(Integer.TYPE)) iRetVal = (short) ((Integer) oVal).intValue(); else if (oCls.equals(Class.forName("java.math.BigDecimal"))) iRetVal = (short) ((java.math.BigDecimal) oVal).intValue(); else if (oCls.equals(Float.TYPE)) iRetVal = (short) ((Float) oVal).intValue(); else if (oCls.equals(Double.TYPE)) iRetVal = (short) ((Double) oVal).intValue(); else iRetVal = new Short(oVal.toString()).shortValue();
        } catch (ClassNotFoundException cnfe) {
            iRetVal = (short) 0;
        }
        return iRetVal;
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a int</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @throws NullPointerException If field value is <b>null</b>
   * @throws ArrayIndexOutOfBoundsException
   */
    public int getInt(int iCol, int iRow) throws NullPointerException, ArrayIndexOutOfBoundsException {
        Object oVal = (((Vector) oResults.get(iRow)).get(iCol));
        if (oVal.getClass().equals(Integer.TYPE)) return ((Integer) oVal).intValue(); else return getInteger(iCol, iRow).intValue();
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a Short</p>
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @throws ArrayIndexOutOfBoundsException if column is not found
   * @throws NullPointerException If field value is <b>null</b>
   */
    public int getInt(String sCol, int iRow) throws ArrayIndexOutOfBoundsException, NullPointerException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        Object oVal = (((Vector) oResults.get(iRow)).get(iCol));
        if (oVal.getClass().equals(Integer.TYPE)) return ((Integer) oVal).intValue(); else return getInteger(iCol, iRow).intValue();
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a double</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @throws NullPointerException If field value is <b>null</b>
   * @throws ArrayIndexOutOfBoundsException
   */
    public double getDouble(int iCol, int iRow) throws NullPointerException, ArrayIndexOutOfBoundsException {
        Object oVal = (((Vector) oResults.get(iRow)).get(iCol));
        Class oCls;
        double dRetVal;
        oCls = oVal.getClass();
        try {
            if (oCls.equals(Short.TYPE)) dRetVal = (double) ((Short) oVal).shortValue(); else if (oCls.equals(Integer.TYPE)) dRetVal = (double) ((Integer) oVal).intValue(); else if (oCls.equals(Class.forName("java.math.BigDecimal"))) dRetVal = ((java.math.BigDecimal) oVal).doubleValue(); else if (oCls.equals(Float.TYPE)) dRetVal = ((Float) oVal).doubleValue(); else if (oCls.equals(Double.TYPE)) dRetVal = ((Double) oVal).doubleValue(); else dRetVal = new Double(Gadgets.removeChar(oVal.toString(), ',')).doubleValue();
        } catch (ClassNotFoundException cnfe) {
            dRetVal = 0d;
        }
        return dRetVal;
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a float</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @throws NullPointerException If field value is <b>null</b>
   * @throws ArrayIndexOutOfBoundsException
   */
    public float getFloat(int iCol, int iRow) throws NullPointerException, ArrayIndexOutOfBoundsException {
        Object oVal = (((Vector) oResults.get(iRow)).get(iCol));
        Class oCls;
        float fRetVal;
        oCls = oVal.getClass();
        try {
            if (oCls.equals(Short.TYPE)) fRetVal = (float) ((Short) oVal).shortValue(); else if (oCls.equals(Integer.TYPE)) fRetVal = (float) ((Integer) oVal).intValue(); else if (oCls.equals(Class.forName("java.math.BigDecimal"))) fRetVal = ((java.math.BigDecimal) oVal).floatValue(); else if (oCls.equals(Float.TYPE)) fRetVal = ((Float) oVal).floatValue(); else if (oCls.equals(Double.TYPE)) fRetVal = ((Double) oVal).floatValue(); else fRetVal = new Float(Gadgets.removeChar(oVal.toString(), ',')).floatValue();
        } catch (ClassNotFoundException cnfe) {
            fRetVal = 0f;
        }
        return fRetVal;
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a Short</p>
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @throws ArrayIndexOutOfBoundsException if column is not found
   * @throws NullPointerException If field value is <b>null</b>
   */
    public float getFloat(String sCol, int iRow) throws NullPointerException, ArrayIndexOutOfBoundsException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        return getFloat(iCol, iRow);
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a float</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @param iDecimals Decimal places for float value
   * @throws ArrayIndexOutOfBoundsException if column is not found
   * @throws NullPointerException If field value is <b>null</b>
   */
    public float getFloat(int iCol, int iRow, int iDecimals) throws NullPointerException, ArrayIndexOutOfBoundsException {
        float p, f = getFloat(iCol, iRow);
        int i;
        if (0 == iDecimals) return (float) ((int) f); else {
            p = 10f;
            for (int d = 0; d < iDecimals; d++) p *= 10;
            i = (int) (f * p);
            return ((float) i) / p;
        }
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a float</p>
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @param iDecimals Decimal places for float value
   * @throws ArrayIndexOutOfBoundsException if column is not found
   * @throws NullPointerException If field value is <b>null</b>
   */
    public float getFloat(String sCol, int iRow, int iDecimals) throws ArrayIndexOutOfBoundsException, NullPointerException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        return getFloat(iCol, iRow, iDecimals);
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into an Integer</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @return Field value converted to Integer or <b>null</b> if field was NULL.
   */
    public Integer getInteger(int iCol, int iRow) throws ArrayIndexOutOfBoundsException {
        Object oVal = (((Vector) oResults.get(iRow)).get(iCol));
        Class oCls;
        Integer iRetVal;
        if (null == oVal) return null;
        oCls = oVal.getClass();
        try {
            if (oCls.equals(Short.TYPE)) iRetVal = new Integer(((Short) oVal).intValue()); else if (oCls.equals(Integer.TYPE)) iRetVal = (Integer) oVal; else if (oCls.equals(Class.forName("java.math.BigDecimal"))) iRetVal = new Integer(((java.math.BigDecimal) oVal).intValue()); else if (oCls.equals(Float.TYPE)) iRetVal = new Integer(((Float) oVal).intValue()); else if (oCls.equals(Double.TYPE)) iRetVal = new Integer(((Double) oVal).intValue()); else iRetVal = new Integer(oVal.toString());
        } catch (ClassNotFoundException cnfe) {
            iRetVal = null;
        }
        return iRetVal;
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into an Integer</p>
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @return Field value converted to Integer or <b>null</b> if field was NULL.
   * @throws ArrayIndexOutOfBoundsException if column is not found
   */
    public Integer getInteger(String sCol, int iRow) throws ArrayIndexOutOfBoundsException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        return getInteger(iCol, iRow);
    }

    /**
   * <p>Get pre-loaded value and tries to convert it into a BigDecimal</p>
   * If column is NULL then <b>null</b> value is returned.<BR>
   * If base columnn is of type String then thsi function will try to parse the
   * value into a BigDecimal. A single dot '.' is used as decimal delimiter no
   * matter which is the current locale. All comma characters ',' are removed
   * before parsing String into BigDecimal.
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @return Field value converted to BigDecimal or <b>null</b> if field was NULL.
   * @throws java.lang.ClassCastException
   * @throws java.lang.NumberFormatException
   */
    public BigDecimal getDecimal(int iCol, int iRow) throws java.lang.ClassCastException, java.lang.NumberFormatException {
        Object oVal = (((Vector) oResults.get(iRow)).get(iCol));
        Class oCls;
        BigDecimal oDecVal;
        if (oVal == null) return null;
        oCls = oVal.getClass();
        if (oCls.equals(Short.TYPE)) oDecVal = new BigDecimal(((Short) oVal).doubleValue()); else if (oCls.equals(Integer.TYPE)) oDecVal = new BigDecimal(((Integer) oVal).doubleValue()); else if (oCls.equals(Float.TYPE)) oDecVal = new BigDecimal(((Float) oVal).doubleValue()); else if (oCls.equals(Double.TYPE)) oDecVal = new BigDecimal(((Double) oVal).doubleValue()); else if (oCls.getName().equalsIgnoreCase("java.lang.String")) oDecVal = new BigDecimal(Gadgets.removeChar((String) oVal, ',')); else {
            try {
                oDecVal = (BigDecimal) oVal;
            } catch (ClassCastException cce) {
                throw new ClassCastException("Cannot cast column of type " + oVal.getClass().getName() + " to BigDecimal");
            }
        }
        return oDecVal;
    }

    /**
   * <p>Get toString() form of pre-loaded value</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @return Field value converted to String or <b>null</b> if field was NULL.
   * @throws ArrayIndexOutOfBoundsException
   */
    public String getString(int iCol, int iRow) throws ArrayIndexOutOfBoundsException {
        Object obj = (((Vector) oResults.get(iRow)).get(iCol));
        if (null != obj) return obj.toString(); else return null;
    }

    /**
   * <p>Get toString() form of pre-loaded value</p>
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @param sDef Default value
   * @return Field value converted to String default value sDef if field was NULL.
   */
    public String getStringNull(int iCol, int iRow, String sDef) throws ArrayIndexOutOfBoundsException {
        String str = getString(iCol, iRow);
        return (null != str ? str : sDef);
    }

    /**
   * <p>Get toString() form of pre-loaded value</p>
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @return Field value converted to String or <b>null</b> if field was NULL.
   * @throws ArrayIndexOutOfBoundsException if column is not found
   */
    public String getString(String sCol, int iRow) throws ArrayIndexOutOfBoundsException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        Object obj = (((Vector) oResults.get(iRow)).get(iCol));
        if (null != obj) return obj.toString(); else return null;
    }

    /**
   * <p>Get toString() form of pre-loaded value</p>
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @param sDef Default value
   * @return Field value converted to String default value sDef if field was NULL.
   * @throws ArrayIndexOutOfBoundsException if column is not found
   */
    public String getStringNull(String sCol, int iRow, String sDef) throws ArrayIndexOutOfBoundsException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        String str = getString(iCol, iRow);
        return (null != str ? str : sDef);
    }

    /**
   * Set an element for a loaded DBSubset
   * @param oObj Object Reference
   * @param iCol Column Index [0..getColumnCount()-1]
   * @param iRow Row Index [0..getColumnCount()-1]
   * @throws ArrayIndexOutOfBoundsException
   */
    public void setElementAt(Object oObj, int iCol, int iRow) throws ArrayIndexOutOfBoundsException {
        if (null == oResults) oResults = new Vector(iFetch, 1);
        Vector oRow;
        Object oRaw = oResults.get(iRow);
        if (null == oRaw) {
            oRow = new Vector(iCol, 1);
            oResults.add(iRow, oRow);
        } else {
            oRow = (Vector) oRaw;
        }
        oRow.setElementAt(oObj, iCol);
    }

    /**
   * Set an element for a loaded DBSubset
   * @param oObj Object Reference
   * @param sCol Column Name
   * @param iRow Row Index [0..getColumnCount()-1]
   * @throws ArrayIndexOutOfBoundsException
   */
    public void setElementAt(Object oObj, String sCol, int iRow) throws ArrayIndexOutOfBoundsException {
        setElementAt(oObj, getColumnPosition(sCol), iRow);
    }

    /**
   * @param iCol Column position [0..getColumnCount()-1]
   * @param iRow Row position [0..getRowCount()-1]
   * @return <b>true</b> if pre-load field is <b>null</b>, <b>false</b> otherwise.
   * @throws ArrayIndexOutOfBoundsException
   */
    public boolean isNull(int iCol, int iRow) throws ArrayIndexOutOfBoundsException {
        Object obj = (((Vector) oResults.get(iRow)).get(iCol));
        return (null == obj);
    }

    /**
   * @param sCol Column name
   * @param iRow Row position [0..getRowCount()-1]
   * @return <b>true</b> if pre-load field is <b>null</b>, <b>false</b> otherwise.
   * @throws ArrayIndexOutOfBoundsException if column is not found
   */
    public boolean isNull(String sCol, int iRow) throws ArrayIndexOutOfBoundsException {
        int iCol = getColumnPosition(sCol);
        if (iCol == -1) throw new ArrayIndexOutOfBoundsException("Column " + sCol + " not found");
        Object obj = (((Vector) oResults.get(iRow)).get(iCol));
        return (null == obj);
    }

    /**
   * <p>Write DBSubset to a delimited text string using the column and row delimiters
   * stablished at setColumnDelimiter() and setRowDelimiter() properties.</p>
   * @return String dump of the whole DBSubset pre-loaded data.
   */
    public String toString() {
        Vector vRow;
        int iCol;
        int iRowCount;
        StringBuffer strBuff;
        if (oResults == null) return "";
        iRowCount = oResults.size();
        if (iRowCount == 0) return "";
        strBuff = new StringBuffer(64 * iRowCount);
        for (int iRow = 0; iRow < iRowCount; iRow++) {
            vRow = (Vector) oResults.get(iRow);
            iCol = 0;
            while (iCol < iColCount) {
                strBuff.append(vRow.get(iCol));
                if (++iCol < iColCount) strBuff.append(sColDelim);
            }
            strBuff.append(sRowDelim);
        }
        return strBuff.toString();
    }

    /**
   * <p>Write DBSubset to an XML string</p>
   * @param sIdent Initial space identations on the left for fields
   * @param sNode Name of top parent node. If <b>null</b> then main table name
   * for this DBSubset is used.
   * @return XML string dump of the whole DBSubset pre-loaded data.
   */
    public String toXML(String sIdent, String sNode) {
        Vector vRow;
        int iCol;
        int iDot;
        int iRowCount;
        int iTokCount;
        StringBuffer strBuff;
        StringTokenizer strTok;
        String sLabel;
        String sNodeName;
        Object oColValue;
        Class oColClass, ClassString = null, ClassDate = null;
        SimpleDateFormat oXMLDate = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        if (DebugFile.trace) {
            DebugFile.incIdent();
            DebugFile.writeln("Begin DBSubset.toXML(" + sNode + ")");
        }
        try {
            ClassString = Class.forName("java.lang.String");
            ClassDate = Class.forName("java.util.Date");
        } catch (ClassNotFoundException ignore) {
        }
        if (oResults != null) {
            sNodeName = (null != sNode ? sNode : sTable);
            iRowCount = oResults.size();
            strBuff = new StringBuffer(256 * iRowCount);
            strTok = new StringTokenizer(sColList, ",");
            iTokCount = strTok.countTokens();
            String[] Labels = new String[iTokCount];
            for (int iTok = 0; iTok < iTokCount; iTok++) {
                sLabel = strTok.nextToken();
                iDot = sLabel.indexOf('.');
                if (-1 != iDot) sLabel = sLabel.substring(++iDot);
                Labels[iTok] = sLabel;
            }
            for (int iRow = 0; iRow < iRowCount; iRow++) {
                vRow = (Vector) oResults.get(iRow);
                iCol = 0;
                strBuff.append(sIdent + "<" + sNodeName + ">\n");
                while (iCol < iColCount) {
                    strBuff.append(sIdent + "  <" + Labels[iCol] + ">");
                    oColValue = vRow.get(iCol);
                    if (null != oColValue) {
                        oColClass = oColValue.getClass();
                        if (oColClass.equals(ClassString) && !Labels[iCol].startsWith("gu_")) strBuff.append("<![CDATA[" + oColValue + "]]>"); else if (oColClass.equals(ClassDate)) strBuff.append(oXMLDate.format((java.util.Date) oColValue)); else strBuff.append(oColValue);
                    }
                    strBuff.append("</" + Labels[iCol] + ">\n");
                    iCol++;
                }
                strBuff.append(sIdent + "</" + sNodeName + ">\n");
            }
        } else strBuff = new StringBuffer();
        if (DebugFile.trace) {
            DebugFile.writeln("End DBSubset.toXML() : " + String.valueOf(strBuff.length()));
            DebugFile.decIdent();
        }
        return strBuff.toString();
    }

    /**
   * <p>Print DBSubset to an output stream<p>
   * This method is quite different in behavior from toString() and toXML().
   * In toString() and toXML() methods data is first pre-loaded by invoking
   * call() or load() methods and then written to a string buffer.
   * For toString() and toXML() memory consumption depends on how many rows
   * are pre-loaded in memory.
   * print() method directly writes readed data to the output stream without creating
   * the bidimimensional internal array for holding readed data.
   * This way data is directly piped from database to output stream.
   * @param oConn Database Connection
   * @param oOutStrm Output Stream
   * @throws SQLException
   */
    public void print(Connection oConn, OutputStream oOutStrm) throws SQLException {
        String sCol;
        int iRows;
        int iCol;
        short jCol;
        float fCol;
        double dCol;
        Date dtCol;
        BigDecimal bdCol;
        Object oCol;
        boolean bQualify = sTxtQualifier.length() > 0;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.print([Connection], [Object])");
            DebugFile.incIdent();
        }
        Statement oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSelect + ")");
        ResultSet oRSet = oStmt.executeQuery(sSelect);
        if (DebugFile.trace) DebugFile.writeln("ResultSet.getMetaData()");
        ResultSetMetaData oMDat = oRSet.getMetaData();
        int iCols = oMDat.getColumnCount();
        if (DebugFile.trace) DebugFile.writeln("column count = " + String.valueOf(iCols));
        PrintWriter oWriter = new PrintWriter(oOutStrm);
        iRows = 0;
        while (oRSet.next()) {
            for (int c = 1; c <= iCols; c++) {
                switch(oMDat.getColumnType(c)) {
                    case Types.VARCHAR:
                    case Types.CHAR:
                        sCol = oRSet.getString(c);
                        if (!oRSet.wasNull()) {
                            sCol = sCol.replace('\n', ' ');
                            if (bQualify) oWriter.write(sTxtQualifier + sCol + sTxtQualifier); else oWriter.write(sCol);
                        }
                        break;
                    case Types.DATE:
                        dtCol = oRSet.getDate(c);
                        if (!oRSet.wasNull()) oWriter.write(dtCol.toString());
                        break;
                    case Types.INTEGER:
                        iCol = oRSet.getInt(c);
                        if (!oRSet.wasNull()) oWriter.print(iCol);
                        break;
                    case Types.SMALLINT:
                        jCol = oRSet.getShort(c);
                        if (!oRSet.wasNull()) oWriter.print(jCol);
                        break;
                    case Types.FLOAT:
                        fCol = oRSet.getFloat(c);
                        if (!oRSet.wasNull()) oWriter.print(fCol);
                        break;
                    case Types.REAL:
                        dCol = oRSet.getDouble(c);
                        if (!oRSet.wasNull()) oWriter.print(dCol);
                        break;
                    case Types.DECIMAL:
                        bdCol = oRSet.getBigDecimal(c);
                        if (!oRSet.wasNull()) oWriter.write(bdCol.toString());
                        break;
                    default:
                        oCol = oRSet.getObject(c);
                        if (!oRSet.wasNull()) oWriter.write(oCol.toString());
                        break;
                }
                if (c < iCols) oWriter.write(getColumnDelimiter());
            }
            oWriter.write(getRowDelimiter());
            iRows++;
        }
        oWriter.flush();
        oWriter.close();
        oWriter = null;
        oRSet.close();
        oRSet = null;
        oStmt.close();
        oStmt = null;
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.print() : " + String.valueOf(iRows));
        }
    }

    private String removeQuotes(String sStr) {
        final int iLen = sStr.length();
        StringBuffer oStr = new StringBuffer(iLen);
        char c;
        for (int i = 0; i < iLen; i++) {
            c = sStr.charAt(i);
            if (c != '"' && c != ' ' && c != '\n' && c != '\t' && c != '\r') oStr.append(c);
        }
        return oStr.toString();
    }

    /**
   * <p>Store full contents of this DBSubset at base table</p>
   * <p>This method takes all the dat contained in memory for this DBSubsets and
   * stores it at the database. For each row, if it does not exist then it is
   * inserted, if it exists then it is updated.
   * @param oConn JDBC Database Connection
   * @param oDBPersistSubclass DBPersist subclass for rows. DBSubset will call the
   * proper DBPersist.store() derived method for each row, executing specific code
   * for the subclass such as automatic GUID at modification date generation.
   * @param bStopOnError <b>true</b> if process should stop if any SQLException is
   * thrown, <b>false</b> if process must continue upon an SQLException and leave
   * return addional information throught SQLException[] array.
   * @return An array with a SQLException object per stored row, if no SQLException
   * was trown for a row then the entry at the array for that row is <b>null</b>.<br>
   * eof() property is set to <b>true</b> if all rows were inserted successfully,
   * and, thus, all entries of the returned SQLException array are null; if any row
   * failed to be inserted or updated then eof() is set to <b>false</b>
   * @throws SQLException Only if bStopOnError is <b>true</b>
   * @trhows ArrayIndexOutOfBoundsException If a table column is not found by its name
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
    public SQLException[] store(JDCConnection oConn, Class oDBPersistSubclass, boolean bStopOnError) throws SQLException, IllegalAccessException, InstantiationException, ArrayIndexOutOfBoundsException {
        DBPersist oDBP;
        DBTable oTbl;
        Object oFld;
        Statement oStmt;
        ResultSet oRSet;
        ResultSetMetaData oMDat;
        SQLException[] aExceptions;
        int iExceptions = 0;
        int iType = Types.NULL;
        if (DebugFile.trace) {
            if (null == oDBPersistSubclass) DebugFile.writeln("Begin DBSubset.store([Connection],null"); else DebugFile.writeln("Begin DBSubset.store([Connection],[" + oDBPersistSubclass.getName() + "]");
            DebugFile.incIdent();
        }
        String[] aCols = Gadgets.split(removeQuotes(sColList), ',');
        iColCount = aCols.length;
        if (oDBPersistSubclass != null) {
            oDBP = (DBPersist) oDBPersistSubclass.newInstance();
            oTbl = oDBP.getTable();
            sColList = "";
            for (int c = 0; c < iColCount; c++) if (null != oTbl.getColumnByName(aCols[c])) sColList += (c == 0 ? "" : ",") + aCols[c]; else sColList += (c == 0 ? "" : ",") + "'void' AS " + aCols[c];
        }
        final int iRowCount = getRowCount();
        if (bStopOnError) aExceptions = null; else aExceptions = new SQLException[iRowCount];
        oStmt = oConn.createStatement();
        if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(SELECT " + sColList + " FROM " + sTable + " WHERE 1=0)");
        oRSet = oStmt.executeQuery("SELECT " + sColList + " FROM " + sTable + " WHERE 1=0");
        oMDat = oRSet.getMetaData();
        int[] aTypes = new int[oMDat.getColumnCount()];
        ColNames = new String[oMDat.getColumnCount()];
        for (int t = 1; t <= iColCount; t++) {
            ColNames[t - 1] = oMDat.getColumnName(t).toLowerCase();
            aTypes[t - 1] = oMDat.getColumnType(t);
        }
        oMDat = null;
        oRSet.close();
        oStmt.close();
        if (oDBPersistSubclass != null) oDBP = (DBPersist) oDBPersistSubclass.newInstance(); else oDBP = new DBPersist(sTable, sTable);
        for (int r = 0; r < iRowCount; r++) {
            if (DebugFile.trace) DebugFile.writeln("processing row " + String.valueOf(r));
            for (int c = 0; c < iColCount; c++) {
                oFld = get(c, r);
                if (null != oFld) iType = aTypes[c];
                if (iType == Types.BLOB) iType = Types.LONGVARBINARY;
                if (iType == Types.CLOB) iType = Types.LONGVARCHAR;
                try {
                    if (oFld.toString().length() > 0 && !oDBP.AllVals.containsKey(aCols[c])) {
                        oDBP.put(aCols[c], oFld.toString(), aTypes[c]);
                    }
                } catch (FileNotFoundException e) {
                }
            }
            if (bStopOnError) {
                oDBP.store(oConn);
            } else {
                try {
                    oDBP.store(oConn);
                    aExceptions[r] = null;
                } catch (SQLException sqle) {
                    iExceptions++;
                    aExceptions[r] = sqle;
                }
            }
            oDBP.clear();
        }
        ColNames = null;
        aTypes = null;
        bEOF = (0 == iExceptions);
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.store() : " + String.valueOf(iExceptions));
        }
        return aExceptions;
    }

    private BigDecimal sumDecimal(int iCol) {
        BigDecimal oRetVal = new BigDecimal(0);
        final int iRows = getRowCount();
        for (int r = 0; r < iRows; r++) if (!isNull(iCol, r)) oRetVal.add(getDecimal(iCol, r));
        return oRetVal;
    }

    private Integer sumInteger(int iCol) {
        int iRetVal = 0;
        final int iRows = getRowCount();
        for (int r = 0; r < iRows; r++) if (!isNull(iCol, r)) iRetVal += getInt(iCol, r);
        return new Integer(iRetVal);
    }

    private Short sumShort(int iCol) {
        short iRetVal = 0;
        final int iRows = getRowCount();
        for (int r = 0; r < iRows; r++) if (!isNull(iCol, r)) iRetVal += getShort(iCol, r);
        return new Short(iRetVal);
    }

    private Float sumFloat(int iCol) {
        float fRetVal = 0;
        final int iRows = getRowCount();
        for (int r = 0; r < iRows; r++) if (!isNull(iCol, r)) fRetVal += getFloat(iCol, r);
        return new Float(fRetVal);
    }

    private Double sumDouble(int iCol) {
        double dRetVal = 0;
        final int iRows = getRowCount();
        for (int r = 0; r < iRows; r++) if (!isNull(iCol, r)) dRetVal += getDouble(iCol, r);
        return new Double(dRetVal);
    }

    public Object sum(int iCol) throws NumberFormatException {
        final int iRows = getRowCount();
        if (0 == iRows) return null;
        Object oFirst = null;
        int r = 0;
        do oFirst = get(iCol, 0); while ((null == oFirst) && (r < iRows));
        if (null == oFirst) return new BigDecimal(0);
        if (oFirst.getClass().getName().equals("java.math.BigDecimal")) return sumDecimal(iCol); else if (oFirst.getClass().getName().equals("java.lang.Integer")) return sumInteger(iCol); else if (oFirst.getClass().getName().equals("java.lang.Short")) return sumShort(iCol); else if (oFirst.getClass().getName().equals("java.lang.Float")) return sumFloat(iCol); else if (oFirst.getClass().getName().equals("java.lang.Double")) return sumDouble(iCol); else throw new NumberFormatException("Column " + String.valueOf(iCol) + " is not a suitable type for sum()");
    }

    /**
   * <p>Parse a delimited text file into DBSubset bi-dimensional array</p>
   * The parsed file must have the same column structure as the column list set when the DBSubset constructor was called.
   * @param sFilePath File Path
   * @param sCharSet Character set encoding for file
   * @throws IOException
   * @throws FileNotFoundException
   * @throws ArrayIndexOutOfBoundsException Delimited values for a file is greater
   * than columns specified at descriptor.
   * @throws RuntimeException If delimiter is not one of { ',' ';' or '\t' }
   * @throws NullPointerException if sFileDescriptor is <b>null</b>
   * @throws IllegalArgumentException if sFileDescriptor is ""
   */
    public void parseCSV(String sFilePath, String sCharSet) throws ArrayIndexOutOfBoundsException, IOException, FileNotFoundException, RuntimeException, NullPointerException, IllegalArgumentException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.parseCSV(" + sFilePath + ")");
            DebugFile.incIdent();
        }
        Vector oRow;
        String[] aCols = Gadgets.split(removeQuotes(sColList), ',');
        iColCount = aCols.length;
        CSVParser oParser = new CSVParser(sCharSet);
        oParser.parseFile(sFilePath, sColList.replace(',', sColDelim.charAt(0)));
        final int iRowCount = oParser.getLineCount();
        oResults = new Vector(iRowCount, 1);
        for (int r = 0; r < iRowCount; r++) {
            oRow = new Vector(iColCount);
            for (int c = 0; c < iColCount; c++) oRow.add(oParser.getField(c, r));
            oResults.add(oRow);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.parseCSV()");
        }
    }

    /**
   * <p>Parse a delimited text file into DBSubset bi-dimensional array</p>
   * The parsed file must have the same column structure as the column list set when the DBSubset constructor was called.
   * @param sFilePath File Path
   */
    public void parseCSV(String sFilePath) throws ArrayIndexOutOfBoundsException, IOException, FileNotFoundException, RuntimeException, NullPointerException, IllegalArgumentException {
        parseCSV(sFilePath, null);
    }

    /**
   * <p>Parse character data into DBSubset bi-dimensional array</p>
   * The parsed file must have the same column structure as the column list set when the DBSubset constructor was called.
   * @param sFilePath Character Data to be parsed
   * @param sCharSet Character set encoding for file
   */
    public void parseCSV(char[] aData, String sCharSet) throws ArrayIndexOutOfBoundsException, RuntimeException, NullPointerException, IllegalArgumentException, UnsupportedEncodingException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBSubset.parseCSV(char[], " + sCharSet + ")");
            DebugFile.incIdent();
        }
        Vector oRow;
        String[] aCols = Gadgets.split(removeQuotes(sColList), ',');
        CSVParser oParser = new CSVParser(sCharSet);
        oParser.parseData(aData, sColList.replace(',', sColDelim.charAt(0)));
        final int iRowCount = oParser.getLineCount();
        iColCount = aCols.length;
        oResults = new Vector(iRowCount, 1);
        for (int r = 0; r < iRowCount; r++) {
            oRow = new Vector(iColCount);
            for (int c = 0; c < iColCount; c++) oRow.add(oParser.getField(c, r));
            oResults.add(oRow);
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBSubset.parseCSV()");
        }
    }

    /**
   * <p>Parse character data into DBSubset bi-dimensional array</p>
   * The parsed file must have the same column structure as the column list set when the DBSubset constructor was called.
   * @param sFilePath Character Data to be parsed
   */
    public void parseCSV(char[] aData) throws ArrayIndexOutOfBoundsException, RuntimeException, NullPointerException, IllegalArgumentException, UnsupportedEncodingException {
        parseCSV(aData, null);
    }

    private static Class getClassForName(String sClassName) {
        Class oRetVal;
        try {
            oRetVal = Class.forName(sClassName);
        } catch (ClassNotFoundException cnfe) {
            oRetVal = null;
        }
        return oRetVal;
    }

    private static Class ClassLangString = getClassForName("java.lang.String");

    private static Class ClassUtilDate = getClassForName("java.util.Date");

    private static Class ClassSQLDate = getClassForName("java.sql.Date");

    private static Class ClassTimestamp = getClassForName("java.sql.Timestamp");

    private int iFetch;

    private int iTimeOut;

    private int iColCount;

    private int iMaxRows;

    private boolean bEOF;

    private String sTable;

    private String sColList;

    private String sFilter;

    private String sSelect;

    private String sInsert;

    private String sColDelim;

    private String sRowDelim;

    private String sTxtQualifier;

    private Vector oResults;

    private String ColNames[];

    private SimpleDateFormat oShortDate;

    private SimpleDateFormat oDateTime;
}
