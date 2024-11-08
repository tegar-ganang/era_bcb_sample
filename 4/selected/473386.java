package com.knowgate.datacopy;

import com.knowgate.debug.DebugFile;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * <p>Internal use class only. Keeps information about each table definition at origin and target.</p>
 * @author Sergio Montoro Ten
 * @version 0.5 alpha
 */
class DataTblDef {

    public DataTblDef() {
    }

    private void alloc(int cCols) {
        ColCount = cCols;
        ColNames = new String[cCols];
        ColTypes = new int[cCols];
        ColSizes = new int[cCols];
        PrimaryKeyMarks = new boolean[cCols];
        for (int c = 0; c < cCols; c++) PrimaryKeyMarks[c] = false;
    }

    public void readMetaData(Connection oConn, String sTable, String sPK) throws SQLException {
        int lenPK;
        int iCurr;
        int cCols;
        Statement oStmt = null;
        ResultSet oRSet = null;
        ResultSetMetaData oMDat = null;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DataTblDef.readMetaData([Connection], \"" + sTable + "\",\"" + sPK + "\")");
            DebugFile.incIdent();
        }
        BaseTable = sTable;
        try {
            oStmt = oConn.createStatement();
            if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(SELECT * FROM " + sTable + " WHERE 1=0)");
            oRSet = oStmt.executeQuery("SELECT * FROM " + sTable + " WHERE 1=0");
            oMDat = oRSet.getMetaData();
            cCols = oMDat.getColumnCount();
            alloc(cCols);
            for (int c = 0; c < cCols; c++) {
                ColNames[c] = oMDat.getColumnName(c + 1);
                ColTypes[c] = oMDat.getColumnType(c + 1);
                ColSizes[c] = oMDat.getPrecision(c + 1);
                if (DebugFile.trace) DebugFile.writeln(ColNames[c] + " SQLType " + String.valueOf(ColTypes[c]) + " precision " + ColSizes[c]);
            }
            oMDat = null;
        } catch (SQLException sqle) {
            throw new SQLException(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
        } finally {
            if (null != oRSet) oRSet.close();
            if (null != oStmt) oStmt.close();
        }
        if (null != sPK) {
            lenPK = sPK.length() - 1;
            cPKs = 1;
            for (int i = 1; i <= lenPK; i++) if (sPK.charAt(i) == ',') cPKs++;
            PrimaryKeys = new String[cPKs];
            iCurr = 0;
            PrimaryKeys[0] = "";
            for (int j = 0; j <= lenPK; j++) if (sPK.charAt(j) != ',') {
                PrimaryKeys[iCurr] += sPK.charAt(j);
            } else {
                if (DebugFile.trace) DebugFile.writeln("PrimaryKeys[" + String.valueOf(iCurr) + "]=" + PrimaryKeys[iCurr]);
                PrimaryKeys[++iCurr] = "";
            }
            if (DebugFile.trace) DebugFile.writeln("PrimaryKeys[" + String.valueOf(iCurr) + "]=" + PrimaryKeys[iCurr]);
            for (int l = 0; l < ColCount; l++) PrimaryKeyMarks[l] = false;
            for (int k = 0; k < cPKs; k++) {
                for (int f = 0; f < ColCount; f++) {
                    PrimaryKeyMarks[f] |= PrimaryKeys[k].equalsIgnoreCase(ColNames[f]);
                }
            }
        } else {
            cPKs = 0;
            PrimaryKeys = null;
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataTblDef.readMetaData()");
        }
    }

    public String getPrimaryKeys(Connection oConn, String sSchema, String sCatalog, String sTable) throws SQLException {
        String sPKCols = null;
        DatabaseMetaData oMDat = oConn.getMetaData();
        ResultSet oRSet = oMDat.getPrimaryKeys(sCatalog, sSchema, sTable);
        while (oRSet.next()) {
            if (null == sPKCols) sPKCols = oRSet.getString(4); else sPKCols += "," + oRSet.getString(4);
        }
        oRSet.close();
        return sPKCols;
    }

    public int findColumnPosition(String sColName) {
        int iCol = -1;
        for (int c = 0; (c < ColCount) && (iCol == -1); c++) if (sColName.equalsIgnoreCase(ColNames[c])) iCol = c;
        return iCol;
    }

    public int findColumnType(String sColName) {
        int iType = 0;
        for (int c = 0; c < ColCount; c++) if (sColName.equalsIgnoreCase(ColNames[c])) {
            iType = ColTypes[c];
            break;
        }
        return iType;
    }

    public boolean inheritsPK(DataTblDef oTblDef) throws ArrayIndexOutOfBoundsException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin " + BaseTable + " DataTblDef.inheritsPK(" + oTblDef.BaseTable + ")");
            DebugFile.incIdent();
            DebugFile.writeln(BaseTable + " has " + String.valueOf(cPKs) + " pk columns");
        }
        boolean bSamePK;
        int pc, fc;
        int iMatchCount = 0;
        if (DebugFile.trace) if (cPKs < oTblDef.cPKs) DebugFile.writeln(BaseTable + " does not inherit PK from " + oTblDef.BaseTable + " because " + oTblDef.BaseTable + " has " + String.valueOf(oTblDef.cPKs) + " PK columns and " + BaseTable + " has only " + String.valueOf(cPKs) + " PK columns");
        bSamePK = (cPKs >= oTblDef.cPKs);
        if (bSamePK) {
            for (int fk = 0; fk < cPKs; fk++) {
                if (DebugFile.trace) DebugFile.writeln("fk=" + String.valueOf(fk));
                fc = findColumnPosition(PrimaryKeys[fk]);
                if (DebugFile.trace && -1 == fc) DebugFile.writeln("cannot find column " + PrimaryKeys[fk] + " on " + BaseTable);
                if (-1 != fc) {
                    for (int pk = 0; pk < oTblDef.cPKs; pk++) {
                        if (DebugFile.trace) DebugFile.writeln("pk=" + String.valueOf(pk));
                        pc = oTblDef.findColumnPosition(oTblDef.PrimaryKeys[pk]);
                        if (DebugFile.trace && -1 == pc) DebugFile.writeln("cannot find column " + oTblDef.PrimaryKeys[pk] + " on " + oTblDef.BaseTable);
                        if (-1 != pc) {
                            if (DebugFile.trace) DebugFile.writeln("trying " + BaseTable + "." + ColNames[fc] + " and " + oTblDef.BaseTable + "." + oTblDef.ColNames[pc]);
                            if ((oTblDef.ColTypes[pc] == ColTypes[fc] && oTblDef.ColSizes[pc] == ColSizes[fc]) && ((cPKs == 1 && oTblDef.ColNames[pc].equalsIgnoreCase(ColNames[fc])) || (cPKs > 1))) {
                                if (DebugFile.trace) {
                                    if (cPKs > 1) DebugFile.writeln(BaseTable + "." + PrimaryKeys[fk] + " matches " + oTblDef.BaseTable + "." + oTblDef.PrimaryKeys[pk]); else DebugFile.writeln(BaseTable + "." + PrimaryKeys[fk] + " matches same column on " + oTblDef.BaseTable + "." + oTblDef.PrimaryKeys[pk]);
                                }
                                iMatchCount++;
                                break;
                            } else {
                                if (DebugFile.trace) {
                                    if (oTblDef.ColTypes[pc] != ColTypes[fc]) DebugFile.writeln(BaseTable + "." + PrimaryKeys[fk] + " has SQLType " + ColTypes[fc] + " and " + oTblDef.BaseTable + "." + oTblDef.PrimaryKeys[pk] + " has SQLType " + oTblDef.ColTypes[pc]); else if (oTblDef.ColSizes[pc] == ColSizes[fc]) DebugFile.writeln(BaseTable + "." + PrimaryKeys[fk] + " has size " + ColSizes[fc] + " and " + oTblDef.BaseTable + "." + oTblDef.PrimaryKeys[pk] + " has size " + oTblDef.ColSizes[pc]); else if (cPKs == 1 && !oTblDef.ColNames[pc].equalsIgnoreCase(ColNames[fc])) DebugFile.writeln(BaseTable + "." + PrimaryKeys[fk] + " as same SQLType and size as " + oTblDef.BaseTable + "." + oTblDef.PrimaryKeys[pk] + " but it is not considered match because " + PrimaryKeys[fk] + " is a single primary key column and they don't have the same name");
                                }
                            }
                        }
                    }
                    if (iMatchCount == oTblDef.cPKs) break;
                }
            }
        }
        if (DebugFile.trace) DebugFile.writeln("match count = " + String.valueOf(iMatchCount) + " , primary keys =" + String.valueOf(oTblDef.cPKs));
        if (iMatchCount < oTblDef.cPKs) bSamePK = false;
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End DataTblDef.inheritsPK() : " + String.valueOf(bSamePK));
        }
        return bSamePK;
    }

    public boolean bestMatch(int iThisCol, DataTblDef oTblDef, int iParentPK) {
        int[] aScores = new int[cPKs];
        int iPKPos;
        int iParentCol;
        int iPKRelativePos = 0;
        int iBestMatch = -1;
        int iBestScore = -1;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin DataTblDef.bestMatch(" + BaseTable + "." + ColNames[iThisCol] + " , " + oTblDef.BaseTable + "." + oTblDef.PrimaryKeys[iParentPK] + ")");
            DebugFile.incIdent();
        }
        iParentCol = oTblDef.findColumnPosition(oTblDef.PrimaryKeys[iParentPK]);
        for (int c = 0; c < this.ColCount & iPKRelativePos < cPKs; c++) if (PrimaryKeyMarks[c] && !ColNames[c].equalsIgnoreCase(ColNames[iThisCol])) iPKRelativePos++; else if (PrimaryKeyMarks[c] && ColNames[c].equalsIgnoreCase(ColNames[iThisCol])) break;
        for (int k = 0; k < cPKs; k++) {
            aScores[k] = 0;
            if (PrimaryKeys[k].equalsIgnoreCase(oTblDef.ColNames[iParentCol])) aScores[k] += 5;
            iPKPos = findColumnPosition(PrimaryKeys[k]);
            if (iPKPos > -1) if (ColTypes[iPKPos] == oTblDef.ColTypes[iParentCol] && ColSizes[iPKPos] == oTblDef.ColSizes[iParentCol]) aScores[k] += 1;
        }
        for (int k = 0; k < cPKs; k++) {
            if (aScores[k] > iBestScore) {
                iBestScore = aScores[k];
                iBestMatch = k;
            }
        }
        if (DebugFile.trace) {
            DebugFile.writeln("pk relative position is " + String.valueOf(iPKRelativePos) + ", best match relative position is " + String.valueOf(iBestMatch));
            DebugFile.decIdent();
            DebugFile.writeln("End DataTblDef.bestMatch() : " + String.valueOf(iPKRelativePos == iBestMatch));
        }
        return (iPKRelativePos == iBestMatch);
    }

    public boolean isPrimaryKey(int iCol) {
        boolean bRetVal = PrimaryKeyMarks[iCol];
        return bRetVal;
    }

    public boolean isPrimaryKey(String sCol) {
        boolean bRetVal;
        int iCol = findColumnPosition(sCol);
        if (-1 == iCol) bRetVal = false; else bRetVal = PrimaryKeyMarks[iCol];
        return bRetVal;
    }

    public int cPKs;

    public boolean bMayInheritPK;

    private boolean PrimaryKeyMarks[];

    public String PrimaryKeys[];

    public String ColNames[];

    public int ColTypes[];

    public int ColSizes[];

    public int ColCount;

    public String BaseTable;
}
