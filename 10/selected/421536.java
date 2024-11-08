package kosoft.dbgen.generate;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;

class ExtractClasses {

    static boolean isOracle = false;

    static boolean isFirebird = true;

    Connection con;

    String sUser;

    String sPassword;

    public static boolean SetUp() {
        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            return true;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public void Connect() throws SQLException {
        String url = "jdbc:odbc:TissBank_Local";
        con = DriverManager.getConnection(url, "Admin", "");
        System.out.println("Trans  isol is: " + con.getTransactionIsolation());
        System.out.println("AutoCommit  is: " + con.getAutoCommit());
    }

    public void Connect(String sURL, String sUser, String sPassword) throws SQLException {
        String url = sURL;
        con = DriverManager.getConnection(url, sUser, sPassword);
        System.out.println("Trans  isol is: " + con.getTransactionIsolation());
        System.out.println("AutoCommit  is: " + con.getAutoCommit());
    }

    public void Disconnect() {
        try {
            con.close();
        } catch (SQLException e) {
        }
    }

    public void BuildClassesForTables() throws SQLException {
        TableDesc[] oTables = GetListOfTables();
        for (int i = 0; i < oTables.length; i++) {
            BuildClassForTable(oTables[i]);
        }
        for (int i = 0; i < oTables.length; i++) {
            oTables[i].DeterminePrimaryKey();
            oTables[i].Display(System.out);
        }
    }

    public TableDesc[] GetListOfTables() throws SQLException {
        DatabaseMetaData dma = con.getMetaData();
        System.out.println("\nConnected to " + dma.getURL());
        System.out.println("Driver       " + dma.getDriverName());
        System.out.println("Version      " + dma.getDriverVersion());
        System.out.println("");
        String[] types = { "TABLE" };
        ResultSet oRes = dma.getTables(null, null, "%", types);
        ResultSetMetaData rsmd = oRes.getMetaData();
        int numCols = rsmd.getColumnCount();
        boolean more = oRes.next();
        Vector oNames = new Vector(20);
        while (more) {
            TableDesc oTable = new TableDesc();
            oTable.sTableName = oRes.getString("TABLE_NAME");
            oNames.addElement(oTable);
            more = oRes.next();
        }
        TableDesc[] oNameList = new TableDesc[oNames.size()];
        oNames.copyInto(oNameList);
        oRes.close();
        return oNameList;
    }

    void BuildClassForTable(TableDesc oTable) throws SQLException {
        oTable.oFields = GetRowDataForTable(oTable.sTableName);
        GetIndexesForTable(oTable);
    }

    FieldDesc[] GetRowDataForTable(String sTable) throws SQLException {
        DatabaseMetaData dma = con.getMetaData();
        String[] types = { "TABLE" };
        ResultSet oRes = dma.getColumns(null, null, sTable, "%");
        ResultSetMetaData rsmd = oRes.getMetaData();
        int numCols = rsmd.getColumnCount();
        PrintColumnHeadings(oRes);
        boolean more = oRes.next();
        Vector oNames = new Vector(20);
        while (more) {
            PrintValues(oRes);
            if (printDetails) System.out.println();
            FieldDesc oField = new FieldDesc();
            oField.sColumnName = oRes.getString("COLUMN_NAME");
            oField.iColType = oRes.getShort("DATA_TYPE");
            oField.sTypeName = oRes.getString("TYPE_NAME");
            if (oField.sTypeName.startsWith("BLOB")) {
                if (oField.sTypeName.equals("BLOB SUB_TYPE 1")) {
                    oField.sTypeName = "LONGVARCHAR";
                } else {
                    oField.sTypeName = "LONGVARBINARY";
                }
            }
            oField.iColSize = oRes.getInt("COLUMN_SIZE");
            oField.iDecimalDigits = oRes.getInt("DECIMAL_DIGITS");
            oField.iNullable = oRes.getInt("NULLABLE");
            oNames.addElement(oField);
            more = oRes.next();
        }
        FieldDesc[] oNameList = new FieldDesc[oNames.size()];
        oNames.copyInto(oNameList);
        oRes.close();
        return oNameList;
    }

    void GetIndexesForTable(TableDesc oTable) throws SQLException {
        DatabaseMetaData dma = con.getMetaData();
        int iGenTableNum = 0;
        boolean bFirstIndexFlg = true;
        ResultSet oRes = dma.getIndexInfo(null, null, oTable.sTableName, false, false);
        if (printDetails) {
            System.out.println("Loading indexes for table : " + oTable.sTableName);
        }
        ResultSetMetaData rsmd = oRes.getMetaData();
        int numCols = rsmd.getColumnCount();
        PrintColumnHeadings(oRes);
        boolean more = oRes.next();
        Vector oNames = new Vector(20);
        String sCurrentIndexName = "_X";
        HashMap indexFieldLookup = new HashMap();
        boolean bUniqueFlg = false;
        while (more) {
            PrintValues(oRes);
            String sIndexName = oRes.getString("INDEX_NAME");
            if (sIndexName == null) sIndexName = "";
            String sColumnName = oRes.getString("COLUMN_NAME");
            int ordinalPosition = oRes.getInt("ORDINAL_POSITION");
            if (sColumnName != null) {
                if (!sIndexName.equals(sCurrentIndexName)) {
                    if (indexFieldLookup.size() != 0) {
                        TableIndexDesc oIndex = new TableIndexDesc();
                        if (sCurrentIndexName.startsWith("{")) {
                            iGenTableNum++;
                            String sNewIndexName = "Gen" + iGenTableNum;
                            System.out.println("Replacing generated name " + sCurrentIndexName + " with " + sNewIndexName);
                            sCurrentIndexName = sNewIndexName;
                        }
                        oIndex.sIndexName = sCurrentIndexName;
                        oIndex.bUniqueFlg = bUniqueFlg;
                        int offset = (indexFieldLookup.get(new Integer(0)) != null) ? 0 : 1;
                        oIndex.oFields = new FieldDesc[indexFieldLookup.size()];
                        for (int i = 0; i < indexFieldLookup.size(); i++) {
                            oIndex.oFields[i] = (FieldDesc) indexFieldLookup.get(new Integer(i + offset));
                        }
                        oNames.addElement(oIndex);
                        indexFieldLookup.clear();
                    }
                    sCurrentIndexName = sIndexName;
                }
                indexFieldLookup.put(new Integer(ordinalPosition), oTable.GetField(sColumnName));
                if (isOracle && bFirstIndexFlg) {
                    bUniqueFlg = true;
                }
                if (!isOracle) {
                    bUniqueFlg = !oRes.getBoolean("NON_UNIQUE");
                }
                if (bFirstIndexFlg) {
                    bFirstIndexFlg = false;
                }
            }
            more = oRes.next();
        }
        if (indexFieldLookup.size() != 0) {
            TableIndexDesc oIndex = new TableIndexDesc();
            if (sCurrentIndexName.startsWith("{")) {
                iGenTableNum++;
                String sNewIndexName = "Gen" + iGenTableNum;
                System.out.println("Replacing generated name " + sCurrentIndexName + " with " + sNewIndexName);
                sCurrentIndexName = sNewIndexName;
            }
            oIndex.sIndexName = sCurrentIndexName;
            oIndex.bUniqueFlg = bUniqueFlg;
            oIndex.oFields = new FieldDesc[indexFieldLookup.size()];
            int offset = (indexFieldLookup.get(new Integer(0)) != null) ? 0 : 1;
            for (int i = 0; i < indexFieldLookup.size(); i++) {
                oIndex.oFields[i] = (FieldDesc) indexFieldLookup.get(new Integer(i + offset));
            }
            oNames.addElement(oIndex);
            indexFieldLookup.clear();
        }
        oTable.oIndexes = new TableIndexDesc[oNames.size()];
        oNames.copyInto(oTable.oIndexes);
        oRes.close();
    }

    static RuntimeProperties properties = new RuntimeProperties();

    static void readParameters() throws Exception {
        properties.setFileName("jdbcgen.properties");
        properties.load();
    }

    public static void main(String args[]) throws Exception {
        readParameters();
        if (properties.getProperty("DebugSQL", "No").toLowerCase().startsWith("y")) {
            System.out.println("Setting log stream");
            DriverManager.setLogStream(System.out);
        }
        String host = "localhost";
        String databaseFile = "/tmp/sysstates.gdb";
        if (args.length == 1) {
            databaseFile = args[0];
        }
        String databaseURL = properties.getProperty("dbpath", "jdbc:firebirdsql:" + databaseFile);
        System.out.println("connect " + databaseURL);
        String user = properties.getProperty("dbuser", "sysdba");
        String password = properties.getProperty("dbpassword", "masterkey");
        String driverName = properties.getProperty("dbdriver", "org.firebirdsql.jdbc.FBDriver");
        java.sql.Connection dbcon = null;
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            System.out.println("Driver " + driverName + " not found in class path");
            System.out.println(e.getMessage());
            throw e;
        }
        String query = "SELECT * FROM requisition";
        ExtractClasses oSel = new ExtractClasses();
        try {
            oSel.SetUp();
            System.out.println(" user " + user + ", " + password);
            oSel.Connect(databaseURL, user, password);
            Connection con = oSel.con;
            checkForWarning(con.getWarnings());
            oSel.BuildClassesForTables();
            oSel.Disconnect();
        } catch (SQLException ex) {
            System.out.println("\n*** SQLException caught ***\n");
            while (ex != null) {
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("Message:  " + ex.getMessage());
                System.out.println("Vendor:   " + ex.getErrorCode());
                ex = ex.getNextException();
                System.out.println("");
            }
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
        }
    }

    private static boolean checkForWarning(SQLWarning warn) throws SQLException {
        boolean rc = false;
        if (warn != null) {
            System.out.println("\n *** Warning ***\n");
            rc = true;
            while (warn != null) {
                System.out.println("SQLState: " + warn.getSQLState());
                System.out.println("Message:  " + warn.getMessage());
                System.out.println("Vendor:   " + warn.getErrorCode());
                System.out.println("");
                warn = warn.getNextWarning();
            }
        }
        return rc;
    }

    void ProcessRequests(DataInputStream in, PrintStream out) {
        String sQuery;
        while (true) {
            try {
                sQuery = in.readLine();
                if (sQuery == null) return;
                if (sQuery.startsWith("select")) {
                    ProcessQuery(sQuery, out);
                } else if (sQuery.startsWith("insert")) {
                    Statement stmt = con.createStatement();
                    stmt.executeUpdate(sQuery);
                } else if (sQuery.startsWith("update")) {
                    Statement stmt = con.createStatement();
                    stmt.executeUpdate(sQuery);
                } else if (sQuery.startsWith("commit")) {
                    con.commit();
                } else if (sQuery.startsWith("autocommit true")) {
                    con.setAutoCommit(true);
                } else if (sQuery.startsWith("autocommit false")) {
                    con.setAutoCommit(false);
                } else if (sQuery.startsWith("delete")) {
                    Statement stmt = con.createStatement();
                    stmt.executeUpdate(sQuery);
                } else if (sQuery.startsWith("rollback")) {
                    con.rollback();
                } else {
                    out.println("What ?");
                }
                out.println(">");
            } catch (SQLException ex) {
                out.println("\n*** SQLException caught ***\n");
                while (ex != null) {
                    out.println("SQLState: " + ex.getSQLState());
                    out.println("Message:  " + ex.getMessage());
                    out.println("Vendor:   " + ex.getErrorCode());
                    ex = ex.getNextException();
                    out.println("");
                }
            } catch (java.lang.Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    void ProcessQuery(String sQuery, PrintStream out) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sQuery);
        dispResultSet(rs, out);
        rs.close();
        stmt.close();
    }

    private static void dispResultSet(ResultSet rs, PrintStream out) throws SQLException {
        int i;
        ResultSetMetaData rsmd = rs.getMetaData();
        int numCols = rsmd.getColumnCount();
        out.println("BeginQuery");
        boolean more = rs.next();
        while (more) {
            out.println("BeginRow");
            for (i = 1; i <= numCols; i++) {
                out.print(rs.getString(i));
                out.println("<br>");
            }
            out.println("EndRow");
            out.println("");
            more = rs.next();
        }
        out.println("EndQuery");
    }

    static boolean printDetails = false;

    void PrintColumnHeadings(ResultSet oRes) throws SQLException {
        if (!printDetails) return;
        ResultSetMetaData rsmd = oRes.getMetaData();
        int numCols = rsmd.getColumnCount();
        for (int i = 1; i <= numCols; i++) {
            if (i > 1) System.out.print(",");
            System.out.print(rsmd.getColumnLabel(i));
        }
        System.out.println();
    }

    void PrintValues(ResultSet oRes) throws SQLException {
        if (!printDetails) return;
        ResultSetMetaData rsmd = oRes.getMetaData();
        int numCols = rsmd.getColumnCount();
        for (int i = 1; i <= numCols; i++) {
            if (i > 1) System.out.print(",");
            System.out.print(oRes.getString(i));
        }
        System.out.println("");
    }
}

class TableDesc {

    String sTableName;

    FieldDesc[] oFields;

    TableIndexDesc[] oIndexes;

    void DeterminePrimaryKey() {
        for (int i = 0; i < oIndexes.length; i++) {
            if (ExtractClasses.isFirebird) {
                if (oIndexes[i].sIndexName.startsWith("RDB$PRIMARY")) {
                    oIndexes[i].bPrimaryKeyFlg = true;
                    return;
                }
            } else if (oIndexes[i].bUniqueFlg) {
                oIndexes[i].bPrimaryKeyFlg = true;
                return;
            }
        }
        System.out.println("Table " + sTableName + " does not have a unique index");
    }

    void Display(PrintStream out) {
        out.println("Table\t" + sTableName);
        out.println("stmt\t" + "select * from " + sTableName);
        out.println("objtype\ttable\n");
        for (int i = 0; i < oFields.length; i++) {
            oFields[i].Display(out);
        }
        for (int i = 0; i < oIndexes.length; i++) {
            oIndexes[i].Display(out);
        }
        out.println("end_table\n");
    }

    FieldDesc GetField(String sNewFieldName) {
        for (int i = 0; i < oFields.length; i++) {
            if (sNewFieldName.equals(oFields[i].sColumnName)) {
                return oFields[i];
            }
        }
        return null;
    }
}

class FieldDesc {

    String sColumnName;

    int iColType;

    String sTypeName;

    int iColSize;

    int iDecimalDigits;

    int iNullable;

    public String toString() {
        return sColumnName;
    }

    String getTypeFromColType(int iColType) {
        switch(iColType) {
            case Types.ARRAY:
                return "ARRAY";
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
            case Types.DATALINK:
                return "DATALINK";
            case Types.DATE:
                return "DATE";
            case Types.DECIMAL:
                return "DECIMAL";
            case Types.DISTINCT:
                return "DISTINCT";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.FLOAT:
                return "FLOAT";
            case Types.INTEGER:
                return "INTEGER";
            case Types.JAVA_OBJECT:
                return "JAVA_OBJECT";
            case Types.LONGVARBINARY:
                return "LONGVARBINARY";
            case Types.LONGVARCHAR:
                return "LONGVARCHAR";
            case Types.NULL:
                return "NULL";
            case Types.NUMERIC:
                return "NUMERIC";
            case Types.OTHER:
                return "OTHER";
            case Types.REAL:
                return "REAL";
            case Types.REF:
                return "REF";
            case Types.SMALLINT:
                return "SMALLINT";
            case Types.STRUCT:
                return "STRUCT";
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
                return "Unknown";
        }
    }

    void Display(PrintStream out) {
        String displayType = sTypeName;
        if (displayType == null) {
            displayType = getTypeFromColType(iColType);
        }
        out.println("field\t" + sColumnName + "\t" + displayType + "\t" + iColType + "\t" + iColSize + "\t" + iDecimalDigits + "\t" + iNullable);
    }
}

class TableIndexDesc {

    String sIndexName;

    boolean bUniqueFlg;

    boolean bPrimaryKeyFlg = false;

    FieldDesc[] oFields;

    void Display(PrintStream out) {
        if (bPrimaryKeyFlg) {
            out.print("primaryquery");
        } else {
            out.print("query\t" + sIndexName + "\t");
            if (bUniqueFlg) out.print("1"); else out.print("n");
        }
        for (int i = 0; i < oFields.length; i++) {
            out.print("\t" + oFields[i].sColumnName);
        }
        out.println();
    }
}
