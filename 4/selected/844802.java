package com.knowgate.dataobjs;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.StringBufferInputStream;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import com.knowgate.debug.*;
import com.knowgate.jdc.*;

/**
 * <p>A database table as a Java Object</p>
 * @author Sergio Montoro Ten
 * @version 2.1
 */
public final class DBTable {

    /**
   *
   * @param sCatalogName Database catalog name
   * @param sSchemaName Database schema name
   * @param sTableName Database table name (not qualified)
   * @param iIndex Ordinal number identifier for table
   */
    public DBTable(String sCatalogName, String sSchemaName, String sTableName, int iIndex) {
        sCatalog = sCatalogName;
        sSchema = sSchemaName;
        sName = sTableName;
        iHashCode = iIndex;
    }

    /**
   * @return Column Count for this table
   */
    public int columnCount() {
        return oColumns.size();
    }

    /**
   * <p>Load a single table register into a Java HashMap</p>
   * @param oConn Database Connection
   * @param PKValues Primary key values of register to be readed, in the same order as they appear in table source.
   * @param AllValues Output parameter. Readed values.
   * @return <b>true</b> if register was found <b>false</b> otherwise.
   * @throws NullPointerException If all objects in PKValues array are null (only debug version)
   * @throws SQLException
   */
    public boolean loadRegister(JDCConnection oConn, Object[] PKValues, HashMap AllValues) throws SQLException, NullPointerException {
        int c;
        boolean bFound;
        Object oVal;
        DBColumn oDBCol;
        ListIterator oColIterator;
        PreparedStatement oStmt = null;
        ResultSet oRSet = null;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBTable.loadRegister([Connection], Object[], [HashMap])");
            DebugFile.incIdent();
            boolean bAllNull = true;
            for (int n = 0; n < PKValues.length; n++) bAllNull &= (PKValues[n] == null);
            if (bAllNull) throw new NullPointerException(sName + " cannot retrieve register, value supplied for primary key is NULL.");
        }
        if (sSelect == null) {
            throw new SQLException("Primary key not found", "42S12");
        }
        AllValues.clear();
        bFound = false;
        try {
            if (DebugFile.trace) DebugFile.writeln("  Connection.prepareStatement(" + sSelect + ")");
            oStmt = oConn.prepareStatement(sSelect);
            try {
                oStmt.setQueryTimeout(10);
            } catch (SQLException sqle) {
                if (DebugFile.trace) DebugFile.writeln("Error at PreparedStatement.setQueryTimeout(10)" + sqle.getMessage());
            }
            for (int p = 0; p < oPrimaryKeys.size(); p++) {
                if (DebugFile.trace) DebugFile.writeln("binding primary key " + PKValues[p] + ".");
                oStmt.setObject(p + 1, PKValues[p]);
            }
            if (DebugFile.trace) DebugFile.writeln("  Connection.executeQuery()");
            oRSet = oStmt.executeQuery();
            if (oRSet.next()) {
                if (DebugFile.trace) DebugFile.writeln("  ResultSet.next()");
                bFound = true;
                oColIterator = oColumns.listIterator();
                c = 1;
                while (oColIterator.hasNext()) {
                    oVal = oRSet.getObject(c++);
                    oDBCol = (DBColumn) oColIterator.next();
                    if (null != oVal) AllValues.put(oDBCol.getName(), oVal);
                }
            }
            if (DebugFile.trace) DebugFile.writeln("  ResultSet.close()");
            oRSet.close();
            oRSet = null;
            oStmt.close();
            oStmt = null;
        } catch (SQLException sqle) {
            try {
                if (null != oRSet) oRSet.close();
                if (null != oStmt) oStmt.close();
            } catch (Exception ignore) {
            }
            throw new SQLException(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBTable.loadRegister() : " + (bFound ? "true" : "false"));
        }
        return bFound;
    }

    private String typeName(int iSQLtype) {
        switch(iSQLtype) {
            case Types.BIGINT:
                return "BIGINT";
            case Types.BINARY:
                return "BINARY";
            case Types.BIT:
                return "BIT";
            case Types.BLOB:
                return "BLOB";
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.CHAR:
                return "CHAR";
            case Types.CLOB:
                return "CLOB";
            case Types.DATE:
                return "DATE";
            case Types.DECIMAL:
                return "DECIMAL";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.FLOAT:
                return "FLOAT";
            case Types.INTEGER:
                return "INTEGER";
            case Types.LONGVARBINARY:
                return "LONGVARBINARY";
            case Types.LONGVARCHAR:
                return "LONGVARCHAR";
            case Types.NULL:
                return "NULL";
            case Types.NUMERIC:
                return "NUMERIC";
            case Types.REAL:
                return "REAL";
            case Types.SMALLINT:
                return "SMALLINT";
            case Types.TIME:
                return "TIME";
            case Types.TIMESTAMP:
                return "TIMESTAMP";
            case Types.TINYINT:
                return "TINYINT";
            case Types.VARBINARY:
                return "VARBINARY";
            case Types.VARCHAR:
                return "VARCHAR";
            default:
                return "OTHER";
        }
    }

    private void bindParameter(JDCConnection oConn, PreparedStatement oStmt, int iParamIndex, Object oParamValue, int iSQLType) throws SQLException {
        Class oParamClass;
        switch(oConn.getDataBaseProduct()) {
            case JDCConnection.DBMS_ORACLE:
                if (oParamValue != null) {
                    oParamClass = oParamValue.getClass();
                    if (DebugFile.trace) DebugFile.writeln("binding " + oParamClass.getName() + " as SQL " + typeName(iSQLType));
                    if (oParamClass.equals(Short.TYPE) || oParamClass.equals(Integer.TYPE) || oParamClass.equals(Float.TYPE) || oParamClass.equals(Double.TYPE)) oStmt.setBigDecimal(iParamIndex, new java.math.BigDecimal(oParamValue.toString())); else if (oParamClass.getName().equals("java.sql.Timestamp") && iSQLType == Types.DATE) {
                        try {
                            Class[] aTimestamp = new Class[1];
                            aTimestamp[0] = Class.forName("java.sql.Timestamp");
                            Class cDATE = Class.forName("oracle.sql.DATE");
                            java.lang.reflect.Constructor cNewDATE = cDATE.getConstructor(aTimestamp);
                            Object oDATE = cNewDATE.newInstance(new Object[] { oParamValue });
                            oStmt.setObject(iParamIndex, oDATE, iSQLType);
                        } catch (ClassNotFoundException cnf) {
                            throw new SQLException("ClassNotFoundException oracle.sql.DATE " + cnf.getMessage());
                        } catch (NoSuchMethodException nsm) {
                            throw new SQLException("NoSuchMethodException " + nsm.getMessage());
                        } catch (IllegalAccessException iae) {
                            throw new SQLException("IllegalAccessException " + iae.getMessage());
                        } catch (InstantiationException ine) {
                            throw new SQLException("InstantiationException " + ine.getMessage());
                        } catch (java.lang.reflect.InvocationTargetException ite) {
                            throw new SQLException("InvocationTargetException " + ite.getMessage());
                        }
                    } else oStmt.setObject(iParamIndex, oParamValue, iSQLType);
                } else oStmt.setObject(iParamIndex, null, iSQLType);
            default:
                oStmt.setObject(iParamIndex, oParamValue, iSQLType);
        }
    }

    /**
   * <p>Store a single register at the database representing a Java Object</p>
   * for register containing LONGVARBINARY, IMAGE, BYTEA or BLOB fields use
   * storeRegisterLong() method.
   * Columns named "dt_created" are invisible for storeRegister() method so that
   * register creation timestamp is not altered by afterwards updates.
   * @param oConn Database Connection
   * @param AllValues Values to assign to fields.
   * @return <b>true</b> if register was inserted for first time, <false> if it was updated.
   * @throws SQLException
   */
    public boolean storeRegister(JDCConnection oConn, HashMap AllValues) throws SQLException {
        int c;
        boolean bNewRow = false;
        DBColumn oCol;
        String sCol;
        ListIterator oColIterator;
        int iAffected = 0;
        PreparedStatement oStmt = null;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBTable.storeRegister([Connection], {" + AllValues.toString() + "})");
            DebugFile.incIdent();
        }
        try {
            if (null != sUpdate) {
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sUpdate + ")");
                oStmt = oConn.prepareStatement(sUpdate);
                try {
                    oStmt.setQueryTimeout(10);
                } catch (SQLException sqle) {
                    if (DebugFile.trace) DebugFile.writeln("Error at PreparedStatement.setQueryTimeout(10)" + sqle.getMessage());
                }
                c = 1;
                oColIterator = oColumns.listIterator();
                while (oColIterator.hasNext()) {
                    oCol = (DBColumn) oColIterator.next();
                    sCol = oCol.getName().toLowerCase();
                    if (!oPrimaryKeys.contains(sCol) && (sCol.compareTo(DB.dt_created) != 0)) {
                        if (DebugFile.trace) {
                            if (oCol.getSqlType() == java.sql.Types.CHAR || oCol.getSqlType() == java.sql.Types.VARCHAR) {
                                if (AllValues.get(sCol) != null) {
                                    DebugFile.writeln("Binding " + sCol + "=" + AllValues.get(sCol).toString());
                                    if (AllValues.get(sCol).toString().length() > oCol.getPrecision()) DebugFile.writeln("ERROR: value for " + oCol.getName() + " exceeds columns precision of " + String.valueOf(oCol.getPrecision()));
                                } else DebugFile.writeln("Binding " + sCol + "=NULL");
                            }
                        }
                        try {
                            bindParameter(oConn, oStmt, c, AllValues.get(sCol), oCol.getSqlType());
                            c++;
                        } catch (ClassCastException e) {
                            if (AllValues.get(sCol) != null) throw new SQLException("ClassCastException at column " + sCol + " Cannot cast Java " + AllValues.get(sCol).getClass().getName() + " to SQL type " + oCol.getSqlTypeName(), "07006"); else throw new SQLException("ClassCastException at column " + sCol + " Cannot cast NULL to SQL type " + oCol.getSqlTypeName(), "07006");
                        }
                    }
                }
                oColIterator = oPrimaryKeys.listIterator();
                while (oColIterator.hasNext()) {
                    sCol = (String) oColIterator.next();
                    oCol = getColumnByName(sCol);
                    if (DebugFile.trace) DebugFile.writeln("PreparedStatement.setObject (" + String.valueOf(c) + "," + AllValues.get(sCol) + "," + oCol.getSqlTypeName() + ")");
                    bindParameter(oConn, oStmt, c, AllValues.get(sCol), oCol.getSqlType());
                    c++;
                }
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeUpdate()");
                iAffected = oStmt.executeUpdate();
                if (DebugFile.trace) DebugFile.writeln(String.valueOf(iAffected) + " affected rows");
                oStmt.close();
                oStmt = null;
            } else iAffected = 0;
            if (0 == iAffected) {
                bNewRow = true;
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sInsert + ")");
                oStmt = oConn.prepareStatement(sInsert);
                try {
                    oStmt.setQueryTimeout(10);
                } catch (SQLException sqle) {
                    if (DebugFile.trace) DebugFile.writeln("Error at PreparedStatement.setQueryTimeout(10)" + sqle.getMessage());
                }
                c = 1;
                oColIterator = oColumns.listIterator();
                while (oColIterator.hasNext()) {
                    oCol = (DBColumn) oColIterator.next();
                    sCol = oCol.getName();
                    if (DebugFile.trace) {
                        if (null != AllValues.get(sCol)) DebugFile.writeln("Binding " + sCol + "=" + AllValues.get(sCol).toString()); else DebugFile.writeln("Binding " + sCol + "=NULL");
                    }
                    bindParameter(oConn, oStmt, c, AllValues.get(sCol), oCol.getSqlType());
                    c++;
                }
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeUpdate()");
                iAffected = oStmt.executeUpdate();
                if (DebugFile.trace) DebugFile.writeln(String.valueOf(iAffected) + " affected rows");
                oStmt.close();
                oStmt = null;
            } else bNewRow = false;
        } catch (SQLException sqle) {
            try {
                if (null != oStmt) oStmt.close();
            } catch (Exception ignore) {
            }
            throw new SQLException(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBTable.storeRegister() : " + String.valueOf(bNewRow && (iAffected > 0)));
        }
        return bNewRow && (iAffected > 0);
    }

    /**
   * <p>Store a single register at the database representing a Java Object</p>
   * for register NOT containing LONGVARBINARY, IMAGE, BYTEA or BLOB fields use
   * storeRegister() method witch is faster than storeRegisterLong().
   * Columns named "dt_created" are invisible for storeRegisterLong() method so that
   * register creation timestamp is not altered by afterwards updates.
   * @param oConn Database Connection
   * @param AllValues Values to assign to fields.
   * @param BinaryLengths map of lengths for long fields.
   * @return <b>true</b> if register was inserted for first time, <false> if it was updated.
   * @throws SQLException
   */
    public boolean storeRegisterLong(JDCConnection oConn, HashMap AllValues, HashMap BinaryLengths) throws IOException, SQLException {
        int c;
        boolean bNewRow = false;
        DBColumn oCol;
        String sCol;
        ListIterator oColIterator;
        PreparedStatement oStmt;
        int iAffected;
        LinkedList oStreams;
        InputStream oStream;
        String sClassName;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBTable.storeRegisterLong([Connection], {" + AllValues.toString() + "})");
            DebugFile.incIdent();
        }
        oStreams = new LinkedList();
        if (null != sUpdate) {
            if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sUpdate + ")");
            oStmt = oConn.prepareStatement(sUpdate);
            try {
                oStmt.setQueryTimeout(10);
            } catch (SQLException sqle) {
                if (DebugFile.trace) DebugFile.writeln("Error at PreparedStatement.setQueryTimeout(10)" + sqle.getMessage());
            }
            c = 1;
            oColIterator = oColumns.listIterator();
            while (oColIterator.hasNext()) {
                oCol = (DBColumn) oColIterator.next();
                sCol = oCol.getName().toLowerCase();
                if (!oPrimaryKeys.contains(sCol) && (!sCol.equalsIgnoreCase(DB.dt_created))) {
                    if (DebugFile.trace) {
                        if (oCol.getSqlType() == java.sql.Types.CHAR || oCol.getSqlType() == java.sql.Types.VARCHAR) {
                            if (AllValues.get(sCol) != null) {
                                DebugFile.writeln("Binding " + sCol + "=" + AllValues.get(sCol).toString());
                                if (AllValues.get(sCol).toString().length() > oCol.getPrecision()) DebugFile.writeln("ERROR: value for " + oCol.getName() + " exceeds columns precision of " + String.valueOf(oCol.getPrecision()));
                            } else DebugFile.writeln("Binding " + sCol + "=NULL");
                        }
                    }
                    if (oCol.getSqlType() == java.sql.Types.LONGVARCHAR || oCol.getSqlType() == java.sql.Types.CLOB || oCol.getSqlType() == java.sql.Types.LONGVARBINARY || oCol.getSqlType() == java.sql.Types.BLOB) {
                        if (BinaryLengths.containsKey(sCol)) {
                            if (((Long) BinaryLengths.get(sCol)).intValue() > 0) {
                                sClassName = AllValues.get(sCol).getClass().getName();
                                if (sClassName.equals("java.io.File")) oStream = new FileInputStream((File) AllValues.get(sCol)); else if (sClassName.equals("[B")) oStream = new ByteArrayInputStream((byte[]) AllValues.get(sCol)); else if (sClassName.equals("[C")) oStream = new StringBufferInputStream(new String((char[]) AllValues.get(sCol))); else throw new SQLException("Invalid object binding for column " + sCol);
                                oStreams.addLast(oStream);
                                oStmt.setBinaryStream(c++, oStream, ((Long) BinaryLengths.get(sCol)).intValue());
                            } else oStmt.setObject(c++, null, oCol.getSqlType());
                        } else oStmt.setObject(c++, null, oCol.getSqlType());
                    } else bindParameter(oConn, oStmt, c++, AllValues.get(sCol), oCol.getSqlType());
                }
            }
            oColIterator = oPrimaryKeys.listIterator();
            while (oColIterator.hasNext()) {
                sCol = (String) oColIterator.next();
                oCol = getColumnByName(sCol);
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.setObject (" + String.valueOf(c) + "," + AllValues.get(sCol) + "," + oCol.getSqlTypeName() + ")");
                bindParameter(oConn, oStmt, c, AllValues.get(sCol), oCol.getSqlType());
                c++;
            }
            if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeUpdate()");
            iAffected = oStmt.executeUpdate();
            if (DebugFile.trace) DebugFile.writeln(String.valueOf(iAffected) + " affected rows");
            oStmt.close();
            oColIterator = oStreams.listIterator();
            while (oColIterator.hasNext()) ((InputStream) oColIterator.next()).close();
            oStreams.clear();
        } else iAffected = 0;
        if (0 == iAffected) {
            bNewRow = true;
            if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sInsert + ")");
            oStmt = oConn.prepareStatement(sInsert);
            c = 1;
            oColIterator = oColumns.listIterator();
            while (oColIterator.hasNext()) {
                oCol = (DBColumn) oColIterator.next();
                sCol = oCol.getName();
                if (DebugFile.trace) {
                    if (null != AllValues.get(sCol)) DebugFile.writeln("Binding " + sCol + "=" + AllValues.get(sCol).toString()); else DebugFile.writeln("Binding " + sCol + "=NULL");
                }
                if (oCol.getSqlType() == java.sql.Types.LONGVARCHAR || oCol.getSqlType() == java.sql.Types.CLOB || oCol.getSqlType() == java.sql.Types.LONGVARBINARY || oCol.getSqlType() == java.sql.Types.BLOB) {
                    if (BinaryLengths.containsKey(sCol)) {
                        if (((Long) BinaryLengths.get(sCol)).intValue() > 0) {
                            sClassName = AllValues.get(sCol).getClass().getName();
                            if (sClassName.equals("java.io.File")) oStream = new FileInputStream((File) AllValues.get(sCol)); else if (sClassName.equals("[B")) oStream = new ByteArrayInputStream((byte[]) AllValues.get(sCol)); else if (sClassName.equals("[C")) oStream = new StringBufferInputStream(new String((char[]) AllValues.get(sCol))); else throw new SQLException("Invalid object binding for column " + sCol);
                            oStreams.addLast(oStream);
                            oStmt.setBinaryStream(c++, oStream, ((Long) BinaryLengths.get(sCol)).intValue());
                        } else oStmt.setObject(c++, null, oCol.getSqlType());
                    } else oStmt.setObject(c++, null, oCol.getSqlType());
                } else bindParameter(oConn, oStmt, c++, AllValues.get(sCol), oCol.getSqlType());
            }
            if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeUpdate()");
            iAffected = oStmt.executeUpdate();
            if (DebugFile.trace) DebugFile.writeln(String.valueOf(iAffected) + " affected rows");
            oStmt.close();
            oColIterator = oStreams.listIterator();
            while (oColIterator.hasNext()) ((InputStream) oColIterator.next()).close();
            oStreams.clear();
        } else bNewRow = false;
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBTable.storeRegisterLong() : " + String.valueOf(bNewRow));
        }
        return bNewRow;
    }

    /**
   * <p>Delete a single register from this table at the database</p>
   * @param oConn Database connection
   * @param AllValues A Map with, at least, the primary key values for the register. Other Map values are ignored.
   * @return <b>true</b> if register was delete, <b>false</b> if register to be deleted was not found.
   * @throws SQLException
   */
    public boolean deleteRegister(JDCConnection oConn, HashMap AllValues) throws SQLException {
        int c;
        boolean bDeleted;
        ListIterator oColIterator;
        PreparedStatement oStmt;
        Object oPK;
        DBColumn oCol;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBTable.deleteRegister([Connection], {" + AllValues.toString() + "})");
            DebugFile.incIdent();
        }
        if (sDelete == null) {
            throw new SQLException("Primary key not found", "42S12");
        }
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sDelete + ")");
        oStmt = oConn.prepareStatement(sDelete);
        c = 1;
        oColIterator = oPrimaryKeys.listIterator();
        while (oColIterator.hasNext()) {
            oPK = oColIterator.next();
            oCol = getColumnByName((String) oPK);
            if (DebugFile.trace) DebugFile.writeln("PreparedStatement.setObject(" + String.valueOf(c) + "," + AllValues.get(oPK) + "," + oCol.getSqlTypeName() + ")");
            oStmt.setObject(c++, AllValues.get(oPK), oCol.getSqlType());
        }
        if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeUpdate()");
        bDeleted = (oStmt.executeUpdate() > 0);
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBTable.deleteRegister() : " + (bDeleted ? "true" : "false"));
        }
        return bDeleted;
    }

    /**
   * <p>Checks if register exists at this table</p>
   * @param oConn Database Connection
   * @param sQueryString Register Query String, as a SQL WHERE clause syntax
   * @return <b>true</b> if register exists, <b>false</b> otherwise.
   * @throws SQLException
   */
    public boolean existsRegister(JDCConnection oConn, String sQueryString) throws SQLException {
        Statement oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ResultSet oRSet = oStmt.executeQuery("SELECT NULL FROM " + getName() + " WHERE " + sQueryString);
        boolean bExists = oRSet.next();
        oRSet.close();
        oStmt.close();
        return bExists;
    }

    /**
   * <p>Checks if register exists at this table</p>
   * @param oConn Database Connection
   * @param sQueryString Register Query String, as a SQL WHERE clause syntax
   * @return <b>true</b> if register exists, <b>false</b> otherwise.
   * @throws SQLException
   */
    public boolean existsRegister(JDCConnection oConn, String sQueryString, Object[] oQueryParams) throws SQLException {
        PreparedStatement oStmt = oConn.prepareStatement("SELECT NULL FROM " + getName() + " WHERE " + sQueryString, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        if (oQueryParams != null) {
            for (int p = 0; p < oQueryParams.length; p++) oStmt.setObject(p + 1, oQueryParams[p]);
        }
        ResultSet oRSet = oStmt.executeQuery();
        boolean bExists = oRSet.next();
        oRSet.close();
        oStmt.close();
        return bExists;
    }

    /**
   * <p>Checks if register exists at this table</p>
   * @param oConn Database Connection
   * @return <b>true</b> if register exists, <b>false</b> otherwise.
   * @throws SQLException
   */
    public boolean existsRegister(JDCConnection oConn, HashMap AllValues) throws SQLException {
        int c;
        boolean bExists;
        PreparedStatement oStmt;
        ResultSet oRSet;
        ListIterator oColIterator;
        Object oPK;
        DBColumn oCol;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBTable.existsRegister([Connection], {" + AllValues.toString() + "})");
            DebugFile.incIdent();
        }
        oStmt = oConn.prepareStatement(sExists, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        c = 1;
        oColIterator = oPrimaryKeys.listIterator();
        while (oColIterator.hasNext()) {
            oPK = oColIterator.next();
            oCol = getColumnByName((String) oPK);
            oStmt.setObject(c++, AllValues.get(oPK), oCol.getSqlType());
        }
        oRSet = oStmt.executeQuery();
        bExists = oRSet.next();
        oRSet.close();
        oStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DBTable.existsRegister() : " + String.valueOf(bExists));
        }
        return bExists;
    }

    /**
   * @return List of {@link DBColumn} objects composing this table.
   */
    public LinkedList getColumns() {
        return oColumns;
    }

    /**
   * @return Columns names separated by commas
   */
    public String getColumnsStr() {
        ListIterator oColIterator = oColumns.listIterator();
        String sGetAllCols = new String("");
        while (oColIterator.hasNext()) sGetAllCols += ((DBColumn) oColIterator.next()).getName() + ",";
        return sGetAllCols.substring(0, sGetAllCols.length() - 1);
    }

    /**
   * <p>Get DBColumn by name</p>
   * @param sColumnName Column Name
   * @return Reference to DBColumn ot <b>null</b> if no column with such name was found.
   */
    public DBColumn getColumnByName(String sColumnName) {
        ListIterator oColIterator = oColumns.listIterator();
        DBColumn oCol = null;
        while (oColIterator.hasNext()) {
            oCol = (DBColumn) oColIterator.next();
            if (oCol.getName().compareToIgnoreCase(sColumnName) == 0) {
                break;
            }
            oCol = null;
        }
        return oCol;
    }

    /**
   * @return List of primary key columns
   */
    public LinkedList getPrimaryKey() {
        return oPrimaryKeys;
    }

    /**
   * @return Unqualified table name
   */
    public String getName() {
        return sName;
    }

    /**
   * @return Catalog name
   */
    public String getCatalog() {
        return sCatalog;
    }

    /**
   * @return Schema name
   */
    public String getSchema() {
        return sSchema;
    }

    public int hashCode() {
        return iHashCode;
    }

    /**
   * <p>Read DBColumn List from DatabaseMetaData</p>
   * This is primarily an internal initialization method for DBTable object.
   * Usually there is no need to call it from any other class.
   * @param oConn Database Connection
   * @param oMData DatabaseMetaData
   * @throws SQLException
   */
    public void readColumns(Connection oConn, DatabaseMetaData oMData) throws SQLException {
        int iErrCode;
        Statement oStmt;
        ResultSet oRSet;
        ResultSetMetaData oRData;
        DBColumn oCol;
        String sCol;
        int iCols;
        ListIterator oColIterator;
        String sColName;
        short iSQLType;
        String sTypeName;
        int iPrecision;
        int iDigits;
        int iNullable;
        int iColPos;
        int iDBMS;
        String sGetAllCols = "";
        String sSetPKCols = "";
        String sSetAllCols = "";
        String sSetNoPKCols = "";
        oColumns = new LinkedList();
        oPrimaryKeys = new LinkedList();
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DBTable.readColumns([DatabaseMetaData])");
            DebugFile.incIdent();
            DebugFile.writeln("DatabaseMetaData.getColumns(" + sCatalog + "," + sSchema + "," + sName + ",%)");
        }
        if (oConn.getMetaData().getDatabaseProductName().equals("PostgreSQL")) iDBMS = 2; else if (oConn.getMetaData().getDatabaseProductName().equals("Oracle")) iDBMS = 5; else iDBMS = 0;
        oStmt = oConn.createStatement();
        try {
            if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(SELECT * FROM " + sName + " WHERE 1=0)");
            oRSet = oStmt.executeQuery("SELECT * FROM " + sName + " WHERE 1=0");
            iErrCode = 0;
        } catch (SQLException sqle) {
            oStmt.close();
            oRSet = null;
            if (DebugFile.trace) DebugFile.writeln("SQLException " + sName + " " + sqle.getMessage());
            iErrCode = sqle.getErrorCode();
            if (iErrCode == 0) iErrCode = -1;
            if (!sqle.getSQLState().equals("42000")) throw new SQLException(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
        }
        if (0 == iErrCode) {
            if (DebugFile.trace) DebugFile.writeln("ResultSet.getMetaData()");
            oRData = oRSet.getMetaData();
            iCols = oRData.getColumnCount();
            if (DebugFile.trace) DebugFile.writeln("table has " + String.valueOf(iCols) + " columns");
            for (int c = 1; c <= iCols; c++) {
                sColName = oRData.getColumnName(c).toLowerCase();
                sTypeName = oRData.getColumnTypeName(c);
                iSQLType = (short) oRData.getColumnType(c);
                if (iDBMS == 2) switch(iSQLType) {
                    case Types.CHAR:
                    case Types.VARCHAR:
                        iPrecision = oRData.getColumnDisplaySize(c);
                        break;
                    default:
                        iPrecision = oRData.getPrecision(c);
                } else {
                    if (iSQLType == Types.BLOB || iSQLType == Types.CLOB) iPrecision = 2147483647; else iPrecision = oRData.getPrecision(c);
                }
                iDigits = oRData.getScale(c);
                iNullable = oRData.isNullable(c);
                iColPos = c;
                if (5 == iDBMS && iSQLType == Types.NUMERIC && iPrecision <= 6 && iDigits == 0) {
                    oCol = new DBColumn(sName, sColName, (short) Types.SMALLINT, sTypeName, iPrecision, iDigits, iNullable, iColPos);
                } else {
                    oCol = new DBColumn(sName, sColName, iSQLType, sTypeName, iPrecision, iDigits, iNullable, iColPos);
                }
                if (!sColName.equals(DB.dt_created)) oColumns.add(oCol);
            }
            if (DebugFile.trace) DebugFile.writeln("ResultSet.close()");
            oRSet.close();
            oRSet = null;
            oStmt.close();
            oStmt = null;
            if (5 == iDBMS) {
                oStmt = oConn.createStatement();
                if (DebugFile.trace) {
                    if (null == sSchema) DebugFile.writeln("Statement.executeQuery(SELECT NULL AS TABLE_CAT, COLS.OWNER AS TABLE_SCHEM, COLS.TABLE_NAME, COLS.COLUMN_NAME, COLS.POSITION AS KEY_SEQ, COLS.CONSTRAINT_NAME AS PK_NAME FROM USER_CONS_COLUMNS COLS, USER_CONSTRAINTS CONS WHERE CONS.OWNER=COLS.OWNER AND CONS.CONSTRAINT_NAME=COLS.CONSTRAINT_NAME AND CONS.CONSTRAINT_TYPE='P' AND CONS.TABLE_NAME='" + sName.toUpperCase() + "')"); else DebugFile.writeln("Statement.executeQuery(SELECT NULL AS TABLE_CAT, COLS.OWNER AS TABLE_SCHEM, COLS.TABLE_NAME, COLS.COLUMN_NAME, COLS.POSITION AS KEY_SEQ, COLS.CONSTRAINT_NAME AS PK_NAME FROM USER_CONS_COLUMNS COLS, USER_CONSTRAINTS CONS WHERE CONS.OWNER=COLS.OWNER AND CONS.CONSTRAINT_NAME=COLS.CONSTRAINT_NAME AND CONS.CONSTRAINT_TYPE='P' AND CONS.OWNER='" + sSchema.toUpperCase() + "' AND CONS.TABLE_NAME='" + sName.toUpperCase() + "')");
                }
                if (null == sSchema) oRSet = oStmt.executeQuery("SELECT NULL AS TABLE_CAT, COLS.OWNER AS TABLE_SCHEM, COLS.TABLE_NAME, COLS.COLUMN_NAME, COLS.POSITION AS KEY_SEQ, COLS.CONSTRAINT_NAME AS PK_NAME FROM USER_CONS_COLUMNS COLS, USER_CONSTRAINTS CONS WHERE CONS.OWNER=COLS.OWNER AND CONS.CONSTRAINT_NAME=COLS.CONSTRAINT_NAME AND CONS.CONSTRAINT_TYPE='P' AND CONS.TABLE_NAME='" + sName.toUpperCase() + "'"); else oRSet = oStmt.executeQuery("SELECT NULL AS TABLE_CAT, COLS.OWNER AS TABLE_SCHEM, COLS.TABLE_NAME, COLS.COLUMN_NAME, COLS.POSITION AS KEY_SEQ, COLS.CONSTRAINT_NAME AS PK_NAME FROM USER_CONS_COLUMNS COLS, USER_CONSTRAINTS CONS WHERE CONS.OWNER=COLS.OWNER AND CONS.CONSTRAINT_NAME=COLS.CONSTRAINT_NAME AND CONS.CONSTRAINT_TYPE='P' AND CONS.OWNER='" + sSchema.toUpperCase() + "' AND CONS.TABLE_NAME='" + sName.toUpperCase() + "'");
            } else {
                if (DebugFile.trace) DebugFile.writeln("DatabaseMetaData.getPrimaryKeys(" + sCatalog + "," + sSchema + "," + sName + ")");
                oRSet = oMData.getPrimaryKeys(sCatalog, sSchema, sName);
            }
            if (oRSet != null) {
                while (oRSet.next()) {
                    oPrimaryKeys.add(oRSet.getString(4).toLowerCase());
                    sSetPKCols += oRSet.getString(4) + "=? AND ";
                }
                if (DebugFile.trace) DebugFile.writeln("pk cols " + sSetPKCols);
                if (sSetPKCols.length() > 7) sSetPKCols = sSetPKCols.substring(0, sSetPKCols.length() - 5);
                if (DebugFile.trace) DebugFile.writeln("ResultSet.close()");
                oRSet.close();
                oRSet = null;
            }
            if (null != oStmt) {
                oStmt.close();
                oStmt = null;
            }
            oColIterator = oColumns.listIterator();
            while (oColIterator.hasNext()) {
                sCol = ((DBColumn) oColIterator.next()).getName();
                sGetAllCols += sCol + ",";
                sSetAllCols += "?,";
                if (!oPrimaryKeys.contains(sCol) && !sCol.equalsIgnoreCase(DB.dt_created)) sSetNoPKCols += sCol + "=?,";
            }
            if (DebugFile.trace) DebugFile.writeln("get all cols " + sGetAllCols);
            if (sGetAllCols.length() > 0) sGetAllCols = sGetAllCols.substring(0, sGetAllCols.length() - 1); else sGetAllCols = "*";
            if (DebugFile.trace) DebugFile.writeln("set all cols " + sSetAllCols);
            if (sSetAllCols.length() > 0) sSetAllCols = sSetAllCols.substring(0, sSetAllCols.length() - 1);
            if (DebugFile.trace) DebugFile.writeln("set no pk cols " + sSetNoPKCols);
            if (sSetNoPKCols.length() > 0) sSetNoPKCols = sSetNoPKCols.substring(0, sSetNoPKCols.length() - 1);
            if (DebugFile.trace) DebugFile.writeln("set pk cols " + sSetPKCols);
            if (sSetPKCols.length() > 0) {
                sSelect = "SELECT " + sGetAllCols + " FROM " + sName + " WHERE " + sSetPKCols;
                sInsert = "INSERT INTO " + sName + "(" + sGetAllCols + ") VALUES (" + sSetAllCols + ")";
                sUpdate = "UPDATE " + sName + " SET " + sSetNoPKCols + " WHERE " + sSetPKCols;
                sDelete = "DELETE FROM " + sName + " WHERE " + sSetPKCols;
                sExists = "SELECT NULL FROM " + sName + " WHERE " + sSetPKCols;
            } else {
                sSelect = null;
                sInsert = "INSERT INTO " + sName + "(" + sGetAllCols + ") VALUES (" + sSetAllCols + ")";
                sUpdate = null;
                sDelete = null;
                sExists = null;
            }
        }
        if (DebugFile.trace) {
            DebugFile.writeln(sSelect != null ? sSelect : "NO SELECT STATEMENT");
            DebugFile.writeln(sInsert != null ? sInsert : "NO INSERT STATEMENT");
            DebugFile.writeln(sUpdate != null ? sUpdate : "NO UPDATE STATEMENT");
            DebugFile.writeln(sDelete != null ? sDelete : "NO DELETE STATEMENT");
            DebugFile.writeln(sExists != null ? sExists : "NO EXISTS STATEMENT");
            DebugFile.decIdent();
            DebugFile.writeln("End DBTable.readColumns()");
        }
    }

    private String sCatalog;

    private String sSchema;

    private String sName;

    private int iHashCode;

    private LinkedList oColumns;

    private LinkedList oPrimaryKeys;

    private String sSelect;

    private String sInsert;

    private String sUpdate;

    private String sDelete;

    private String sExists;
}
