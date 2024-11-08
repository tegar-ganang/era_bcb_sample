package com.knowgate.datacopy;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.sql.CallableStatement;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringBufferInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.StringReader;
import java.lang.ArrayIndexOutOfBoundsException;
import java.lang.ClassNotFoundException;
import java.math.BigDecimal;
import java.util.Vector;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Set;
import java.util.Iterator;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.apache.oro.text.regex.*;
import com.knowgate.debug.DebugFile;

/**
 * <p>Copier for Complex Data Structures stored at database.</p>
 * This is not a general purpose module, but a custom class tailored to the
 * design constraints of hipergate standard datamodel.
 * @author Sergio Montoro Ten
 * @version 0.5 alpha
 */
public class DataStruct extends DefaultHandler implements ContentHandler {

    public DataStruct() {
        initMembers();
    }

    public DataStruct(String sPathXMLFile) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, SAXException {
        initMembers();
        parse(sPathXMLFile);
    }

    private void initMembers() {
        cTables = 0;
        bOrPrepared = false;
        bTrPrepared = false;
        iTrStatus = iOrStatus = DISCONNECTED;
        DataRowSets = new Vector();
        FieldMaps = new Vector();
        FieldDefs = new Vector();
        Transformations = new Vector();
        Before = new Vector();
        After = new Vector();
    }

    public void setAutoCommit(boolean bAutoCommit) throws SQLException {
        oTrConn.setAutoCommit(bAutoCommit);
    }

    public void commit() throws SQLException {
        oTrConn.commit();
    }

    public void rollback() throws SQLException {
        oTrConn.rollback();
    }

    public void connectOrigin(String sDriver, String sURL, String sUsr, String sPwd, String sSchema) throws SQLException, ClassNotFoundException {
        if (DebugFile.trace) DebugFile.writeln("Begin DataStruct.connectOrigin(" + sDriver + "," + sURL + "," + sUsr + "," + sPwd + ")");
        Class oDriver = Class.forName(sDriver);
        if (DebugFile.trace) DebugFile.writeln("  " + sDriver + " JDBC driver loaded");
        oOrConn = DriverManager.getConnection(sURL, sUsr, sPwd);
        iOrStatus = CONNECTED;
        if (DebugFile.trace) DebugFile.writeln("End DataStruct.connectOrigin()");
    }

    public void connectTarget(String sDriver, String sURL, String sUsr, String sPwd, String sSchema) throws SQLException, ClassNotFoundException {
        if (DebugFile.trace) DebugFile.writeln("Begin DataStruct.connectTarget(" + sDriver + "," + sURL + "," + sUsr + "," + sPwd + ")");
        Class oDriver = Class.forName(sDriver);
        if (DebugFile.trace) DebugFile.writeln("  " + sDriver + " JDBC driver loaded");
        oTrConn = DriverManager.getConnection(sURL, sUsr, sPwd);
        iTrStatus = CONNECTED;
        if (DebugFile.trace) DebugFile.writeln("End DataStruct.connectTarget()");
    }

    public Connection getOriginConnection() {
        return oOrConn;
    }

    public Connection getTargetConnection() {
        return oTrConn;
    }

    public void setOriginConnection(Connection oConn) {
        oOrConn = oConn;
        iOrStatus = REFERENCED;
    }

    public void setTargetConnection(Connection oConn) {
        oTrConn = oConn;
        iTrStatus = REFERENCED;
    }

    public void clear() throws SQLException {
        int t;
        if (DebugFile.trace) DebugFile.writeln("Begin DataStruct.clear()");
        if (bOrPrepared) {
            for (t = 0; t < cTables; t++) {
                if (null != OrStatements[t]) OrStatements[t].close();
                OrStatements[t] = null;
                if (null != UpStatements[t]) UpStatements[t].close();
                UpStatements[t] = null;
            }
            OrMetaData = null;
            bOrPrepared = false;
        }
        if (bTrPrepared) {
            for (t = 0; t < cTables; t++) {
                if (null != TrStatements[t]) TrStatements[t].close();
                TrStatements[t] = null;
                if (null != DlStatements[t]) DlStatements[t].close();
                DlStatements[t] = null;
            }
            TrMetaData = null;
            bTrPrepared = false;
        }
        cTables = 0;
        if (null != After) After.clear();
        if (null != Before) Before.clear();
        if (null != Transformations) Transformations.clear();
        if (null != FieldDefs) FieldDefs.clear();
        if (null != FieldMaps) FieldMaps.clear();
        if (null != DataRowSets) DataRowSets.clear();
        if (DebugFile.trace) DebugFile.writeln("End DataStruct.clear()");
    }

    public void disconnectAll() throws SQLException {
        int t;
        if (DebugFile.trace) DebugFile.writeln("Begin DataStruct.disconnectAll()");
        clear();
        if (CONNECTED == iOrStatus) {
            oOrConn.close();
            iOrStatus = DISCONNECTED;
        }
        if (CONNECTED == iTrStatus) {
            oTrConn.close();
            iTrStatus = DISCONNECTED;
        }
        if (DebugFile.trace) DebugFile.writeln("End DataStruct.disconnectAll()");
    }

    protected DataRowSet getRowSet(int i) {
        return (DataRowSet) DataRowSets.get(i);
    }

    protected Object getResult(int iRow, int iCol) {
        return ((Vector) oResults.get(iRow)).get(iCol);
    }

    private boolean isEmpty(String sStr) {
        if (null == sStr) return true; else if (0 == sStr.length()) return true; else return false;
    }

    protected void execCommands(String sTime, int iTable, Object PK[], int cParams) throws SQLException {
        CallableStatement oCall;
        Statement oStmt;
        ResultSet rCount;
        int cAffected;
        String sSQL;
        String sTable;
        ListIterator oIter;
        if (DebugFile.trace) {
            if (iTable != -1) sTable = getRowSet(iTable).OriginTable; else sTable = "";
            DebugFile.writeln("Begin DataStruct.execCommands(" + sTime + ", " + sTable + ", ..., " + String.valueOf(cParams) + ")");
            DebugFile.incIdent();
        }
        if (-1 == iTable) {
            if (sTime.equals("INIT")) oIter = InitStmts.listIterator(); else oIter = TermStmts.listIterator();
        } else {
            if (sTime.equals("BEFORE")) oIter = ((LinkedList) Before.get(iTable)).listIterator(); else oIter = ((LinkedList) After.get(iTable)).listIterator();
        }
        while (oIter.hasNext()) {
            sSQL = oIter.next().toString();
            if (sSQL.startsWith("{") || sSQL.startsWith("k_sp")) {
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareCall(" + sSQL + ")");
                oCall = oTrConn.prepareCall(sSQL);
                for (int p = 0; p < cParams; p++) {
                    if (DebugFile.trace) DebugFile.writeln("CallableStatement.setObject(" + String.valueOf(p + 1) + "," + PK[p].toString() + ")");
                    oCall.setObject(p + 1, PK[p]);
                }
                if (DebugFile.trace) DebugFile.writeln("Connection.execute(" + sSQL + ")");
                oCall.execute();
                oCall.close();
                oCall = null;
            } else {
                oStmt = oTrConn.createStatement();
                if (DebugFile.trace) DebugFile.writeln("Connection.execute(" + sSQL + ")");
                oStmt.execute(sSQL);
                oStmt.close();
                oStmt = null;
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataStruct.execCommands()");
        }
    }

    public void prepareStatements() throws SQLException {
        HashMap oMap;
        String sSQL;
        boolean bIsMapped;
        boolean bHasDefault;
        int iTrCols;
        String sCol;
        int iCol;
        int c;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DataStruct.prepareStatements()");
            DebugFile.incIdent();
        }
        oInsertTexts = new HashMap(2 * cTables);
        oSelectTexts = new HashMap(2 * cTables);
        OrStatements = new PreparedStatement[cTables];
        TrStatements = new PreparedStatement[cTables];
        UpStatements = new PreparedStatement[cTables];
        DlStatements = new PreparedStatement[cTables];
        OrMetaData = new DataTblDef[cTables];
        TrMetaData = new DataTblDef[cTables];
        for (int s = 0; s < cTables; s++) {
            if (CONNECTED == iTrStatus || REFERENCED == iTrStatus) {
                TrMetaData[s] = new DataTblDef();
                if (DebugFile.trace) DebugFile.writeln("DataTblDef.readMetaData (TargetConnection, " + getRowSet(s).TargetTable + ", " + (String) oToPKs.get(getRowSet(s).TargetTable) + ")");
                if (oToPKs.get(getRowSet(s).TargetTable) != null) TrMetaData[s].readMetaData(oTrConn, getRowSet(s).TargetTable, oToPKs.get(getRowSet(s).TargetTable).toString()); else TrMetaData[s].readMetaData(oTrConn, getRowSet(s).TargetTable, null);
                sSQL = "DELETE FROM " + getRowSet(s).TargetTable;
                if (!isEmpty(getRowSet(s).EraseClause)) sSQL += " WHERE " + getRowSet(s).EraseClause; else if (!isEmpty(getRowSet(s).WhereClause)) sSQL += " WHERE " + getRowSet(s).WhereClause;
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                DlStatements[s] = oTrConn.prepareStatement(sSQL);
                iTrCols = TrMetaData[s].ColCount;
                sSQL = "INSERT INTO " + getRowSet(s).TargetTable + " VALUES (";
                for (c = iTrCols; c >= 1; c--) sSQL += (c != 1) ? "?," : "?)";
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                oInsertTexts.put(getRowSet(s).OriginTable, sSQL);
                TrStatements[s] = oTrConn.prepareStatement(sSQL);
                sSQL = "UPDATE " + getRowSet(s).TargetTable + " SET ";
                for (c = 0; c < iTrCols; c++) if (!TrMetaData[s].isPrimaryKey(c)) sSQL += TrMetaData[s].ColNames[c] + "=?,";
                sSQL = sSQL.substring(0, sSQL.length() - 1) + " WHERE ";
                for (c = 0; c < TrMetaData[s].cPKs; c++) sSQL += TrMetaData[s].PrimaryKeys[c] + "=? AND ";
                sSQL = sSQL.substring(0, sSQL.length() - 5);
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                UpStatements[s] = oTrConn.prepareStatement(sSQL);
            }
            if (CONNECTED == iOrStatus || REFERENCED == iOrStatus) {
                OrMetaData[s] = new DataTblDef();
                if (DebugFile.trace) DebugFile.writeln("DataTblDef.readMetaData (OriginConnection, " + getRowSet(s).OriginTable + ", " + (String) oToPKs.get(getRowSet(s).OriginTable) + ")");
                OrMetaData[s].readMetaData(oOrConn, getRowSet(s).OriginTable, (String) oFromPKs.get(getRowSet(s).OriginTable));
                if (CONNECTED == iTrStatus || REFERENCED == iTrStatus) iTrCols = TrMetaData[s].ColCount; else iTrCols = OrMetaData[s].ColCount;
                if (DebugFile.trace) DebugFile.writeln("Column count = " + String.valueOf(iTrCols));
                if (getRowSet(s).FieldList.compareTo("*") != 0) {
                    sSQL = "SELECT " + getRowSet(s).FieldList + " ";
                } else {
                    sSQL = "SELECT ";
                    for (c = 0; c < iTrCols; c++) {
                        sCol = TrMetaData[s].ColNames[c];
                        try {
                            oMap = (HashMap) FieldMaps.get(s);
                            bIsMapped = oMap.containsKey(sCol);
                            if (bIsMapped) sCol = (String) oMap.get(sCol); else {
                                bIsMapped = oMap.containsKey(sCol.toUpperCase());
                                if (bIsMapped) sCol = (String) oMap.get(sCol.toUpperCase()); else {
                                    bIsMapped = oMap.containsKey(sCol.toLowerCase());
                                    if (bIsMapped) sCol = (String) oMap.get(sCol.toLowerCase());
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            bIsMapped = false;
                        }
                        iCol = OrMetaData[s].findColumnPosition(sCol);
                        if (iCol != -1) sSQL += sCol + ((c < iTrCols - 1) ? "," : " "); else {
                            try {
                                oMap = (HashMap) FieldDefs.get(s);
                                bHasDefault = oMap.containsKey(sCol);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                bHasDefault = false;
                                oMap = null;
                            }
                            if (bHasDefault) sSQL += (String) oMap.get(sCol) + " AS " + TrMetaData[s].ColNames[c] + ((c < iTrCols - 1) ? "," : " "); else if (bIsMapped) sSQL += sCol + " AS " + TrMetaData[s].ColNames[c] + ((c < iTrCols - 1) ? "," : " "); else sSQL += "NULL AS " + TrMetaData[s].ColNames[c] + ((c < iTrCols - 1) ? "," : " ");
                        }
                    }
                }
                sSQL += "FROM " + getRowSet(s).OriginTable;
                if (!isEmpty(getRowSet(s).WhereClause)) {
                    if (getRowSet(s).WhereClause.trim().toUpperCase().startsWith("START")) sSQL += " " + getRowSet(s).WhereClause; else sSQL += " WHERE " + getRowSet(s).WhereClause;
                }
                if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
                oSelectTexts.put(getRowSet(s).OriginTable, sSQL);
                OrStatements[s] = oOrConn.prepareStatement(sSQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            }
        }
        if (CONNECTED == iOrStatus || REFERENCED == iOrStatus) bOrPrepared = true;
        if (CONNECTED == iTrStatus || REFERENCED == iTrStatus) bTrPrepared = true;
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataStruct.prepareStatements()");
        }
    }

    public Object convert(Object oValue, int iSQLType) {
        Object oRetVal;
        String sClass;
        if (null == oValue) {
            oRetVal = null;
        } else {
            sClass = oValue.getClass().getName();
            if (sClass.equals("java.lang.Short") || sClass.equals("java.lang.Integer") || sClass.equals("java.math.BigDecimal")) {
                if (java.sql.Types.VARCHAR == iSQLType || java.sql.Types.CHAR == iSQLType || java.sql.Types.LONGVARCHAR == iSQLType) oRetVal = oValue.toString(); else if (java.sql.Types.DECIMAL == iSQLType || java.sql.Types.NUMERIC == iSQLType) oRetVal = new BigDecimal(oValue.toString()); else if (java.sql.Types.INTEGER == iSQLType) oRetVal = new Integer(oValue.toString()); else if (java.sql.Types.SMALLINT == iSQLType) oRetVal = new Short(oValue.toString()); else oRetVal = oValue;
            } else if (sClass.equals("java.lang.String")) {
                if (java.sql.Types.DECIMAL == iSQLType || java.sql.Types.NUMERIC == iSQLType) oRetVal = new BigDecimal(oValue.toString()); else if (java.sql.Types.SMALLINT == iSQLType) oRetVal = new Short(oValue.toString()); else if (java.sql.Types.INTEGER == iSQLType) {
                    String str = oValue.toString();
                    oRetVal = new Integer(str);
                } else oRetVal = oValue;
            } else if (sClass.equals("java.sql.Timestamp")) {
                oRetVal = new java.sql.Date(((Timestamp) oValue).getTime());
            } else oRetVal = oValue;
        }
        return oRetVal;
    }

    public int mapType(int iSQLType) {
        int iRetType;
        switch(iSQLType) {
            case java.sql.Types.TIMESTAMP:
                iRetType = java.sql.Types.DATE;
                break;
            default:
                iRetType = iSQLType;
        }
        return iRetType;
    }

    protected void getRows(Object[] OrPK, Object[] TrPK, int cParams, int iTable) throws SQLException {
        int iPK;
        int iFetchBurst = 500;
        int iSQLType;
        int cTransforms;
        Vector oRow;
        HashMap oTransforms;
        Object oOriginalValue;
        Object oTransformedValue;
        ResultSet oRSet;
        ResultSetMetaData oRDat;
        DataRowSet oDatR;
        String sColName;
        DataTblDef oMDat = OrMetaData[iTable];
        DataTransformation oDatT;
        PreparedStatement oStmt = OrStatements[iTable];
        PreparedStatement oStmt2;
        oDatR = getRowSet(iTable);
        if (oDatR.WhereClause != null) {
            if (oDatR.WhereClause.indexOf("?") > 0) {
                for (int p = 0; p < cParams; p++) {
                    if (DebugFile.trace) DebugFile.writeln("binding query input parameter " + String.valueOf(p + 1));
                    oStmt.setObject(p + 1, OrPK[p]);
                }
            }
        }
        if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeQuery()");
        oRSet = oStmt.executeQuery();
        oDatR = null;
        if (DebugFile.trace) {
            if (DataRowSets.get(iTable) != null) DebugFile.writeln("FieldList=" + getRowSet(iTable).FieldList); else DebugFile.writeln("ERROR: getRowSet(" + String.valueOf(iTable) + ") == null");
        }
        if (getRowSet(iTable).FieldList.compareTo("*") != 0) iCols = oRSet.getMetaData().getColumnCount(); else iCols = TrMetaData[iTable].ColCount;
        if (DebugFile.trace) DebugFile.writeln("reading " + String.valueOf(iCols) + " columns");
        oResults = new Vector(iFetchBurst, iFetchBurst);
        if (DebugFile.trace) DebugFile.writeln("new Vector(" + String.valueOf(iFetchBurst) + ")");
        iRows = 0;
        try {
            oTransforms = (HashMap) Transformations.get(iTable);
            cTransforms = oTransforms.size();
        } catch (ArrayIndexOutOfBoundsException e) {
            if (DebugFile.trace) DebugFile.writeln("table has no transformation replacements");
            oTransforms = null;
            cTransforms = 0;
        }
        if (0 == cTransforms) {
            while (oRSet.next() && iRows < iFetchBurst) {
                iRows++;
                if (DebugFile.trace) DebugFile.writeln("caching row " + String.valueOf(iRows));
                oRow = new Vector(iCols);
                for (int c = 1; c <= iCols; c++) oRow.add(oRSet.getObject(c));
                oResults.add(oRow);
            }
        } else {
            oRDat = oRSet.getMetaData();
            while (oRSet.next() && iRows < iFetchBurst) {
                iRows++;
                if (DebugFile.trace) DebugFile.writeln("caching row " + String.valueOf(iRows));
                oRow = new Vector(iCols);
                iPK = 0;
                for (int c = 1; c <= iCols; c++) {
                    try {
                        sColName = oRDat.getColumnName(c);
                        oDatT = (DataTransformation) oTransforms.get(sColName);
                        if (null == oDatT) oRow.add(oRSet.getObject(c)); else {
                            oOriginalValue = oRSet.getObject(c);
                            oTransformedValue = oDatT.transform(getOriginConnection(), getTargetConnection(), oOriginalValue);
                            if (DebugFile.trace) DebugFile.writeln(sColName + " " + oOriginalValue + " transformed to " + (oTransformedValue != null ? oTransformedValue : "NULL"));
                            oRow.add(oTransformedValue);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        oRow.add(oRSet.getObject(c));
                    }
                }
                oResults.add(oRow);
            }
            oRDat = null;
        }
        if (DebugFile.trace) DebugFile.writeln("row count = " + String.valueOf(iRows));
        oRSet.close();
        oRSet = null;
    }

    public void insert(Object[] OrPK, Object[] TrPK, int cParams) throws SQLException {
        String sField;
        DataTblDef oMDat;
        PreparedStatement oInsrt;
        StringReader oReader;
        Object oValue;
        String sValue;
        int r;
        int q;
        int iPK;
        int iSQLType;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DataStruct.insert(OrPK[], TrPK[], " + String.valueOf(cParams) + ")");
            DebugFile.incIdent();
        }
        execCommands("INIT", -1, OrPK, cParams);
        if (!bTrPrepared || !bTrPrepared) prepareStatements();
        for (int s = 0; s < cTables; s++) {
            if (DebugFile.trace) DebugFile.writeln("processing rowset from " + getRowSet(s).OriginTable + " to " + getRowSet(s).TargetTable);
            execCommands("BEFORE", s, OrPK, cParams);
            getRows(OrPK, TrPK, cParams, s);
            oMDat = TrMetaData[s];
            oInsrt = TrStatements[s];
            for (r = 0; r < iRows; r++) {
                iPK = 0;
                for (q = 0; q < iCols; q++) {
                    sField = oMDat.ColNames[q];
                    iSQLType = oMDat.ColTypes[q];
                    oValue = getResult(r, q);
                    if (oMDat.isPrimaryKey(q)) {
                        if (iPK < cParams && oMDat.inheritsPK(TrMetaData[0])) if (null != TrPK[iPK]) {
                            if (oValue.getClass().equals(TrPK[iPK].getClass()) && oMDat.bestMatch(q, TrMetaData[0], iPK)) {
                                if (DebugFile.trace) DebugFile.writeln("swaping PK " + oValue.toString() + " to " + TrPK[iPK].toString() + " before insert");
                                oValue = TrPK[iPK];
                            }
                        }
                        iPK++;
                    }
                    if (DebugFile.trace) if (oValue != null) DebugFile.writeln("binding " + sField + "=" + oValue.toString() + " as SQLType " + String.valueOf(iSQLType)); else DebugFile.writeln("binding " + sField + "=NULL as SQLType " + String.valueOf(iSQLType));
                    if (iSQLType == java.sql.Types.LONGVARCHAR) {
                        sValue = oValue.toString() + " ";
                        oReader = new StringReader(sValue);
                        oInsrt.setCharacterStream(q + 1, oReader, sValue.length() - 1);
                    } else oInsrt.setObject(q + 1, convert(oValue, iSQLType), mapType(iSQLType));
                }
                if (DebugFile.trace) DebugFile.writeln("PreparedStatement.execute()");
                oInsrt.execute();
            }
            oResults.clear();
            oResults = null;
            execCommands("AFTER", s, OrPK, cParams);
        }
        execCommands("TERM", -1, OrPK, cParams);
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataStruct.insert()");
        }
    }

    public void update(Object[] OrPK, Object[] TrPK, int cParams) throws SQLException {
        String sField;
        DataTblDef oMDat;
        PreparedStatement oInsrt;
        PreparedStatement oUpdt;
        Object oValue;
        String sValue;
        StringReader oReader;
        int r;
        int q;
        int iPK;
        int cUpdated;
        int iSQLType;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DataStruct.update(OrPK[], TrPK[], " + String.valueOf(cParams) + ")");
            DebugFile.incIdent();
        }
        execCommands("INIT", -1, OrPK, cParams);
        if (!bTrPrepared || !bTrPrepared) prepareStatements();
        for (int s = 0; s < cTables; s++) {
            if (DebugFile.trace) DebugFile.writeln("processing rowset from " + getRowSet(s).OriginTable + " to " + getRowSet(s).TargetTable);
            execCommands("BEFORE", s, OrPK, cParams);
            getRows(OrPK, TrPK, cParams, s);
            oMDat = TrMetaData[s];
            oUpdt = UpStatements[s];
            for (r = 0; r < iRows; r++) {
                iPK = 0;
                if (oMDat.ColCount > oMDat.cPKs) {
                    for (q = 0; q < iCols; q++) {
                        sField = oMDat.ColNames[q];
                        iSQLType = oMDat.ColTypes[q];
                        if (oMDat.isPrimaryKey(q)) {
                            if (iPK < cParams && oMDat.inheritsPK(TrMetaData[0])) {
                                if (null != TrPK[iPK]) {
                                    if (getResult(r, q).getClass().equals(TrPK[iPK].getClass()) && oMDat.bestMatch(q, TrMetaData[0], iPK)) {
                                        if (DebugFile.trace) DebugFile.writeln("swaping PK " + getResult(r, q).toString() + " to " + TrPK[iPK].toString() + " before update");
                                        oValue = TrPK[iPK];
                                    } else {
                                        oValue = getResult(r, q);
                                    }
                                } else {
                                    oValue = getResult(r, q);
                                }
                            } else {
                                oValue = getResult(r, q);
                            }
                            if (DebugFile.trace) if (oValue == null) DebugFile.writeln("binding " + sField + "=null as SQLType " + String.valueOf(iSQLType) + " at parameter " + String.valueOf(iCols - oMDat.cPKs + iPK + 1)); else DebugFile.writeln("binding " + sField + "=" + oValue.toString() + " as SQLType " + String.valueOf(iSQLType) + " at parameter " + String.valueOf(iCols - oMDat.cPKs + iPK + 1));
                            oUpdt.setObject(iCols - oMDat.cPKs + iPK + 1, convert(oValue, iSQLType), mapType(iSQLType));
                            iPK++;
                        } else {
                            if (DebugFile.trace) DebugFile.writeln("binding " + sField + " as SQLType " + String.valueOf(iSQLType) + " at parameter " + String.valueOf(q + 1 - iPK));
                            if (iSQLType == java.sql.Types.LONGVARCHAR) {
                                sValue = getResult(r, q).toString() + " ";
                                oReader = new StringReader(sValue);
                                oUpdt.setCharacterStream(q + 1 - iPK, oReader, sValue.length() - 1);
                            } else oUpdt.setObject(q + 1 - iPK, convert(getResult(r, q), iSQLType), mapType(iSQLType));
                        }
                    }
                    if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeUpdate()");
                    cUpdated = oUpdt.executeUpdate();
                    if (DebugFile.trace) DebugFile.writeln(String.valueOf(cUpdated) + " rows updated");
                } else {
                    cUpdated = 0;
                    if (DebugFile.trace) DebugFile.writeln("pk count=" + String.valueOf(oMDat.cPKs) + " column count=" + String.valueOf(oMDat.ColCount) + " row not updated because no non-pk columns found");
                }
                if (0 == cUpdated) {
                    oInsrt = TrStatements[s];
                    iPK = 0;
                    for (q = 0; q < iCols; q++) {
                        sField = oMDat.ColNames[q];
                        iSQLType = oMDat.ColTypes[q];
                        oValue = getResult(r, q);
                        if (oMDat.isPrimaryKey(q)) {
                            if (iPK < cParams && oMDat.inheritsPK(TrMetaData[0])) {
                                if (null != TrPK[iPK]) {
                                    if (oValue.getClass().equals(TrPK[iPK].getClass()) && oMDat.bestMatch(q, TrMetaData[0], iPK)) {
                                        if (DebugFile.trace) DebugFile.writeln("swaping PK " + oValue.toString() + " to " + TrPK[iPK].toString());
                                        oValue = TrPK[iPK];
                                    }
                                }
                            }
                            iPK++;
                        }
                        if (DebugFile.trace) DebugFile.writeln("binding " + sField + " as SQLType " + String.valueOf(iSQLType));
                        if (iSQLType == java.sql.Types.LONGVARCHAR) {
                            sValue = oValue.toString() + " ";
                            oReader = new StringReader(sValue);
                            oInsrt.setCharacterStream(q + 1, oReader, sValue.length() - 1);
                        } else oInsrt.setObject(q + 1, convert(oValue, iSQLType), mapType(iSQLType));
                    }
                    if (DebugFile.trace) DebugFile.writeln("PreparedStatement.execute()");
                    oInsrt.execute();
                }
            }
            oResults.clear();
            oResults = null;
            execCommands("AFTER", s, OrPK, cParams);
        }
        execCommands("TERM", -1, OrPK, cParams);
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataStruct.update()");
        }
    }

    protected void seekReferal(DataTransformation oTransform) {
        DataTransformation oTransformRef;
        HashMap oTransformsRef;
        Set oTransformSet;
        Iterator oSetIterator;
        String sChildTable;
        String sReferedTable = oTransform.ReferedTable;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DataStruct.seekReferal(ReferedTable=" + sReferedTable + ", ReferedField=" + oTransform.ReferedField + ")");
            DebugFile.incIdent();
        }
        for (int r = 0; r < DataRowSets.size(); r++) {
            sChildTable = getRowSet(r).OriginTable;
            if (sChildTable.equalsIgnoreCase(sReferedTable)) {
                oTransformsRef = (HashMap) Transformations.get(r);
                oTransformSet = oTransformsRef.keySet();
                oSetIterator = oTransformSet.iterator();
                while (oSetIterator.hasNext()) {
                    oTransformRef = (DataTransformation) oTransformsRef.get(oSetIterator.next());
                    if (oTransformRef.OriginField.equalsIgnoreCase(oTransform.ReferedField)) {
                        oTransform.setReferedValues(oTransformRef);
                        if (DebugFile.trace) DebugFile.writeln(oTransform.OriginTable + " references " + oTransform.ReferedTable + "." + oTransform.ReferedField);
                    }
                }
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataStruct.seekReferal()");
        }
    }

    /** Start document. */
    public void startDocument() throws SAXException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DataStruct.startDocument()");
            DebugFile.incIdent();
        }
        fElements = 0;
        fCharacters = 0;
        sContext = "";
        sNode = "";
        oFromPKs = new HashMap(5, 3);
        oToPKs = new HashMap(5, 3);
        InitStmts = new LinkedList();
        TermStmts = new LinkedList();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataStruct.startDocument()");
        }
    }

    public void startElement(String uri, String local, String raw, Attributes attrs) throws SAXException {
        fElements++;
        sChars = "";
        if (local.equalsIgnoreCase("ROWSET")) {
            oCurrRowSet = new DataRowSet();
            DataRowSets.add(oCurrRowSet);
            cTables++;
            oCurrMap = new HashMap(13, 5);
            FieldMaps.add(oCurrMap);
            oCurrTransform = new HashMap(5, 3);
            Transformations.add(oCurrTransform);
            oCurrDef = new HashMap(13, 5);
            FieldDefs.add(oCurrDef);
            oCurrBef = new LinkedList();
            Before.add(oCurrBef);
            oCurrAft = new LinkedList();
            After.add(oCurrAft);
        }
        if (local.equalsIgnoreCase("ACTION") || local.equalsIgnoreCase("MAPPINGS") || local.equalsIgnoreCase("DEFVALS") || local.equalsIgnoreCase("NULLVALS") || local.equalsIgnoreCase("BEFORE") || local.equalsIgnoreCase("AFTER") || local.equalsIgnoreCase("INIT") || local.equalsIgnoreCase("TERM")) sNode = sContext = local.toUpperCase(); else sNode = local.toUpperCase();
        if ((sNode.equals("MAPPING") && attrs.getLength() > 0) || (sNode.equals("DEFVAL") && attrs.getLength() > 0)) sTransform = attrs.getValue(0); else sTransform = null;
    }

    public void endElement(String uri, String localName, String qname) throws SAXException {
        int iComma;
        String sOrFld;
        String sOrVal;
        String sTrFld;
        DataTransformation oTransform;
        if (sContext.equalsIgnoreCase("ACTION")) {
            if (sNode.equalsIgnoreCase("FROM")) oCurrRowSet.OriginTable = sChars.trim(); else if (sNode.equalsIgnoreCase("TO")) oCurrRowSet.TargetTable = sChars.trim(); else if (sNode.equalsIgnoreCase("JOIN")) oCurrRowSet.JoinTables = sChars; else if (sNode.equalsIgnoreCase("WHERE")) oCurrRowSet.WhereClause = sChars; else if (sNode.equalsIgnoreCase("ERASE")) oCurrRowSet.EraseClause = sChars; else if (sNode.equalsIgnoreCase("FIELDLIST")) oCurrRowSet.FieldList = sChars; else if (sNode.equalsIgnoreCase("FROM_PK")) oFromPKs.put(oCurrRowSet.OriginTable, sChars.trim()); else if (sNode.equalsIgnoreCase("TO_PK")) oToPKs.put(oCurrRowSet.TargetTable, sChars.trim());
        } else if (sContext.equalsIgnoreCase("MAPPINGS")) {
            if (sNode.equalsIgnoreCase("MAPPING")) {
                iComma = sChars.lastIndexOf(",");
                sOrFld = sChars.substring(0, iComma).trim();
                sTrFld = sChars.substring(iComma + 1).trim();
                oCurrMap.put(sTrFld, sOrFld);
                if (null != sTransform) {
                    oTransform = new DataTransformation(sTransform, oCurrRowSet.OriginTable, sOrFld, oCurrRowSet.TargetTable, sTrFld);
                    oCurrTransform.put(sOrFld, oTransform);
                    if (oTransform.OperationCode == DataTransformation.Operations.REFER) seekReferal(oTransform);
                }
            }
        } else if (sContext.equalsIgnoreCase("DEFVALS")) {
            if (sNode.equalsIgnoreCase("DEFVAL")) {
                iComma = sChars.indexOf(",");
                sTrFld = sChars.substring(0, iComma).trim();
                sOrFld = sChars.substring(iComma + 1).trim();
                oCurrDef.put(sTrFld, sOrFld);
                if (null != sTransform) {
                    oTransform = new DataTransformation(sTransform, oCurrRowSet.OriginTable, sTrFld, oCurrRowSet.TargetTable, sTrFld);
                    oCurrTransform.put(sTrFld, oTransform);
                    if (oTransform.OperationCode == DataTransformation.Operations.REFER) seekReferal(oTransform);
                }
            }
        } else if (sContext.equalsIgnoreCase("BEFORE")) {
            if (localName.equalsIgnoreCase("EXEC") || localName.equalsIgnoreCase("CALL")) oCurrBef.addLast(sChars);
        } else if (sContext.equalsIgnoreCase("AFTER")) {
            if (localName.equalsIgnoreCase("EXEC") || localName.equalsIgnoreCase("CALL")) oCurrAft.addLast(sChars);
        } else if (sContext.equalsIgnoreCase("INIT")) {
            if (localName.equalsIgnoreCase("EXEC") || localName.equalsIgnoreCase("CALL")) InitStmts.addLast(sChars);
        } else if (sContext.equalsIgnoreCase("TERM")) {
            if (localName.equalsIgnoreCase("EXEC") || localName.equalsIgnoreCase("CALL")) TermStmts.addLast(sChars);
        }
    }

    /** Characters. */
    public void characters(char ch[], int start, int length) throws SAXException {
        fCharacters += length;
        sChars += new String(ch, start, length);
    }

    /** Warning. */
    public void warning(SAXParseException ex) throws SAXException {
        if (DebugFile.trace) DebugFile.write(composeError("Warning", ex));
    }

    /** Error. */
    public void error(SAXParseException ex) throws SAXException {
        if (DebugFile.trace) DebugFile.write(composeError("Error", ex));
        throw ex;
    }

    /** Fatal error. */
    public void fatalError(SAXParseException ex) throws SAXException {
        if (DebugFile.trace) DebugFile.write(composeError("Fatal Error", ex));
        throw ex;
    }

    /** Compose the error message. */
    protected String composeError(String type, SAXParseException ex) {
        String sErrDesc = "";
        String systemId = null;
        int index;
        sErrDesc += "[SAX " + type + "] ";
        if (ex == null) sErrDesc += "!!!"; else systemId = ex.getSystemId();
        if (systemId != null) {
            index = systemId.lastIndexOf('/');
            if (index != -1) systemId = systemId.substring(index + 1);
            sErrDesc += systemId;
        }
        sErrDesc += " Line:" + ex.getLineNumber();
        sErrDesc += " Column:" + ex.getColumnNumber();
        sErrDesc += " Cause: " + ex.getMessage();
        sErrDesc += "\n";
        return sErrDesc;
    }

    public void parse(String sXMLFile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SAXException {
        Properties oProps = new Properties();
        parse(sXMLFile, oProps);
    }

    public void parse(String sXMLFile, Properties oProps) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SAXException {
        XMLReader parser;
        Parser sax1Parser;
        File oFile;
        FileReader oFileRead;
        BufferedReader oBuff;
        StringBufferInputStream oStrBuff;
        InputSource ioSrc;
        FileInputStream oStream;
        String sXMLSource;
        String sParam;
        Enumeration oEnum;
        Pattern oPattern;
        PatternMatcher oMatcher = new Perl5Matcher();
        PatternCompiler oCompiler = new Perl5Compiler();
        byte byBuffer[];
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DataStruct.parse(" + sXMLFile + ")");
            DebugFile.incIdent();
        }
        try {
            if (DebugFile.trace) DebugFile.writeln("XMLReaderFactory.createXMLReader(" + DEFAULT_PARSER_NAME + ")");
            parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
        } catch (Exception e) {
            if (DebugFile.trace) DebugFile.writeln("ParserFactory.makeParser(" + DEFAULT_PARSER_NAME + ")");
            sax1Parser = ParserFactory.makeParser(DEFAULT_PARSER_NAME);
            parser = new ParserAdapter(sax1Parser);
            if (DebugFile.trace) DebugFile.writeln("warning: Features and properties not supported on SAX1 parsers.");
        }
        try {
            parser.setFeature(NAMESPACES_FEATURE_ID, DEFAULT_NAMESPACES);
            parser.setFeature(VALIDATION_FEATURE_ID, DEFAULT_VALIDATION);
        } catch (SAXException e) {
        }
        parser.setContentHandler(this);
        parser.setErrorHandler(this);
        oEnum = oProps.keys();
        if (sXMLFile.startsWith("<?xml")) {
            while (oEnum.hasMoreElements()) {
                sParam = (String) oEnum.nextElement();
                try {
                    oPattern = oCompiler.compile("{#" + sParam + "}");
                } catch (MalformedPatternException e) {
                    oPattern = null;
                }
                sXMLFile = Util.substitute(oMatcher, oPattern, new Perl5Substitution(oProps.getProperty(sParam), Perl5Substitution.INTERPOLATE_ALL), sXMLFile, Util.SUBSTITUTE_ALL);
            }
            oStrBuff = new StringBufferInputStream(sXMLFile);
            ioSrc = new InputSource(oStrBuff);
            parser.parse(ioSrc);
            oStrBuff.close();
        } else {
            if (oProps.isEmpty()) {
                oFileRead = new FileReader(sXMLFile);
                oBuff = new BufferedReader(oFileRead, 32767);
                ioSrc = new InputSource(oBuff);
                parser.parse(ioSrc);
                oBuff.close();
                oFileRead.close();
            } else {
                oFile = new File(sXMLFile);
                byBuffer = new byte[new Long(oFile.length()).intValue()];
                oStream = new FileInputStream(oFile);
                oStream.read(byBuffer);
                sXMLSource = new String(byBuffer);
                oStream.close();
                while (oEnum.hasMoreElements()) {
                    sParam = (String) oEnum.nextElement();
                    try {
                        oPattern = oCompiler.compile("{#" + sParam + "}");
                    } catch (MalformedPatternException e) {
                        oPattern = null;
                    }
                    sXMLSource = Util.substitute(oMatcher, oPattern, new Perl5Substitution(oProps.getProperty(sParam), Perl5Substitution.INTERPOLATE_ALL), sXMLSource, Util.SUBSTITUTE_ALL);
                }
                oStrBuff = new StringBufferInputStream(sXMLSource);
                ioSrc = new InputSource(oStrBuff);
                parser.parse(ioSrc);
                oStrBuff.close();
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataStruct.parse()");
        }
    }

    public void createClassSource(String sPackage, String sSubClassName, String sFile) throws IOException {
        FileWriter oFile = new FileWriter(sFile);
        DataTransformation oDatT;
        Iterator oIter;
        LinkedList oList;
        Object oPKs;
        String sItem;
        oFile.write("package " + sPackage + ";\n");
        oFile.write("import java.util.Vector;\n");
        oFile.write("import java.util.LinkedList;\n");
        oFile.write("import java.util.ListIterator;\n");
        oFile.write("import java.util.HashMap;\n");
        oFile.write("import java.util.Iterator;\n");
        oFile.write("import com.knowgate.datacopy.DataRowSet;\n");
        oFile.write("import com.knowgate.datacopy.DataStruct;\n");
        oFile.write("import com.knowgate.datacopy.DataTransformation;\n");
        oFile.write("\n");
        oFile.write("public class " + sSubClassName + " extends DataStruct {\n");
        oFile.write("  public " + sSubClassName + "() {\n");
        oFile.write("    DataRowSet oRowSet;\n");
        oFile.write("    DataTransformation oTransForm;\n");
        oFile.write("    LinkedList oBefore;\n");
        oFile.write("    LinkedList oAfter;\n");
        oFile.write("    HashMap oMappings;\n");
        oFile.write("    HashMap oDefaults;\n");
        oFile.write("    HashMap oTransforms;\n\n");
        oFile.write("    InitStmts = new LinkedList();\n");
        oFile.write("    TermStmts = new LinkedList();\n");
        oFile.write("    oToPKs = new HashMap();\n");
        oFile.write("    oFromPKs = new HashMap();\n");
        oFile.write("    cTables = " + String.valueOf(DataRowSets.size()) + ";\n");
        oFile.write("\n");
        oIter = InitStmts.listIterator();
        while (oIter.hasNext()) oFile.write("    InitStmts.addLast(\"" + oIter.next().toString() + "\");\n");
        oFile.write("\n");
        oIter = TermStmts.listIterator();
        while (oIter.hasNext()) oFile.write("    TermStmts.addLast(\"" + oIter.next().toString() + "\");\n");
        oFile.write("\n");
        for (int c = 0; c < cTables; c++) {
            oCurrRowSet = getRowSet(c);
            oFile.write("    oRowSet = new DataRowSet(\"" + oCurrRowSet.OriginTable + "\",\"" + oCurrRowSet.TargetTable + "\",\"" + oCurrRowSet.JoinTables + "\",\"" + oCurrRowSet.WhereClause + "\",\"" + oCurrRowSet.EraseClause + "\");\n");
            oFile.write("    oRowSet.FieldList = \"" + oCurrRowSet.FieldList.trim() + "\";\n");
            oFile.write("    DataRowSets.add(oRowSet);\n");
            oFile.write("    oBefore = new LinkedList();\n");
            oFile.write("    oAfter = new LinkedList();\n");
            oFile.write("    oMappings = new HashMap();\n");
            oFile.write("    oDefaults = new HashMap();\n");
            oFile.write("    oTransforms = new HashMap();\n");
            oPKs = oFromPKs.get(oCurrRowSet.OriginTable);
            if (null != oPKs) oFile.write("    oFromPKs.put(\"" + oCurrRowSet.OriginTable + "\",\"" + oPKs.toString() + "\");\n");
            oPKs = oToPKs.get(oCurrRowSet.TargetTable);
            if (null != oPKs) oFile.write("    oToPKs.put(\"" + oCurrRowSet.TargetTable + "\",\"" + oPKs.toString() + "\");\n\n");
            oList = (LinkedList) Before.get(c);
            if (oList.size() > 0) {
                oIter = oList.iterator();
                while (oIter.hasNext()) oFile.write("    oBefore.addLast(\"" + oIter.next().toString() + "\");\n");
                oIter = null;
            }
            oList = null;
            oFile.write("    Before.add(oBefore);\n");
            oList = (LinkedList) After.get(c);
            if (oList.size() > 0) {
                oIter = oList.iterator();
                while (oIter.hasNext()) oFile.write("    oAfter.addLast(\"" + oIter.next().toString() + "\");\n");
                oIter = null;
            }
            oList = null;
            oFile.write("    After.add(oAfter);\n");
            try {
                oCurrMap = (HashMap) FieldMaps.get(c);
                oIter = oCurrMap.keySet().iterator();
                while (oIter.hasNext()) {
                    sItem = (String) oIter.next();
                    oFile.write("    oMappings.put(\"" + sItem + "\",\"" + oCurrMap.get(sItem).toString() + "\");\n");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
            oFile.write("    FieldMaps.add(oMappings);\n");
            try {
                oFile.write("    Transformations.add(oTransforms);\n");
                oCurrTransform = (HashMap) Transformations.get(c);
                oIter = oCurrTransform.keySet().iterator();
                while (oIter.hasNext()) {
                    sItem = oIter.next().toString();
                    oDatT = (DataTransformation) oCurrTransform.get(sItem);
                    oFile.write("    oTransForm = new DataTransformation(" + String.valueOf(oDatT.OperationCode) + "," + "\"" + oDatT.OriginTable + "\",\"" + oDatT.OriginField + "\"," + "\"" + oDatT.TargetTable + "\",\"" + oDatT.TargetField + "\"," + (null == oDatT.ReferedTable ? "null" : "\"" + oDatT.ReferedTable + "\"") + "," + (null == oDatT.ReferedField ? "null" : "\"" + oDatT.ReferedField + "\"") + "," + (null == oDatT.IfNullValue ? "null" : "\"" + oDatT.IfNullValue + "\"") + ");\n");
                    oFile.write("    oTransforms.put(\"" + sItem + "\", oTransForm);\n");
                    if (oDatT.OperationCode == DataTransformation.Operations.REFER) oFile.write("    seekReferal(oTransForm);\n");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
            try {
                oCurrDef = (HashMap) FieldDefs.get(c);
                oIter = oCurrDef.keySet().iterator();
                while (oIter.hasNext()) {
                    sItem = (String) oIter.next();
                    oFile.write("    oMappings.put(\"" + sItem + "\",\"" + oCurrDef.get(sItem).toString() + "\");\n");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
            oFile.write("    FieldDefs.add(oDefaults);\n");
            oFile.write("\n");
        }
        oFile.write("  }\n}");
        oFile.close();
        oFile = null;
    }

    private boolean bOrPrepared;

    private boolean bTrPrepared;

    private Connection oOrConn;

    private Connection oTrConn;

    private int iOrStatus;

    private int iTrStatus;

    protected Vector oResults;

    protected int iCols;

    protected int iRows;

    protected int cTables;

    protected Vector FieldMaps;

    protected Vector FieldDefs;

    protected Vector DataRowSets;

    protected Vector Before;

    protected Vector After;

    protected LinkedList InitStmts;

    protected LinkedList TermStmts;

    protected Vector Transformations;

    protected HashMap oFromPKs;

    protected HashMap oToPKs;

    protected PreparedStatement OrStatements[];

    protected PreparedStatement TrStatements[];

    protected PreparedStatement UpStatements[];

    protected PreparedStatement DlStatements[];

    protected DataTblDef OrMetaData[];

    protected DataTblDef TrMetaData[];

    public HashMap oInsertTexts;

    public HashMap oSelectTexts;

    private String sChars;

    private long fElements;

    private long fCharacters;

    private String sTransform;

    private String sContext;

    private String sNode;

    private DataRowSet oCurrRowSet;

    private HashMap oCurrMap;

    private HashMap oCurrDef;

    private HashMap oCurrTransform;

    private LinkedList oCurrBef;

    private LinkedList oCurrAft;

    private static final int DISCONNECTED = 0;

    private static final int CONNECTED = 1;

    private static final int REFERENCED = 2;

    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";

    protected static final String NAMESPACE_PREFIXES_FEATURE_ID = "http://xml.org/sax/features/namespace-prefixes";

    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";

    protected static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";

    protected static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    protected static final int DEFAULT_REPETITION = 1;

    protected static final boolean DEFAULT_NAMESPACES = true;

    protected static final boolean DEFAULT_NAMESPACE_PREFIXES = false;

    protected static final boolean DEFAULT_VALIDATION = false;

    protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;

    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;

    protected static final boolean DEFAULT_DYNAMIC_VALIDATION = false;

    protected static final boolean DEFAULT_MEMORY_USAGE = false;

    protected static final boolean DEFAULT_TAGGINESS = false;
}
