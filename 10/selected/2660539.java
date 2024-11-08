package edu.nctu.csie.jichang.database.model.connection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.nctu.csie.jichang.database.dbinfo.DataBaseInfo;
import edu.nctu.csie.jichang.database.dbinfo.LoginInfo;
import edu.nctu.csie.jichang.database.model.builder.AbstractSQLBuilder;
import edu.nctu.csie.jichang.database.model.builder.ISQLBuilder;
import edu.nctu.csie.jichang.database.model.cell.ColumnFullInfo;
import edu.nctu.csie.jichang.database.model.cell.ColumnInfo;
import edu.nctu.csie.jichang.database.model.cell.ColumnInfoValue;
import edu.nctu.csie.jichang.database.model.cell.ColumnType;
import edu.nctu.csie.jichang.database.model.cell.DBRow;
import edu.nctu.csie.jichang.database.model.cell.NameMap;
import edu.nctu.csie.jichang.database.model.cell.NameMapCell;
import edu.nctu.csie.jichang.database.model.cell.RefTableMapping;
import edu.nctu.csie.jichang.database.model.cell.SchemaTable;
import edu.nctu.csie.jichang.database.util.DBException;
import edu.nctu.csie.jichang.database.util.SqlTokenizer;
import edu.nctu.csie.jichang.database.util.StringUtil;

public abstract class AbstractDBConnection implements IDBConnection {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDBConnection.class);

    protected ISQLBuilder builder = null;

    protected String SCHEMA_PATTERN = null;

    protected int UNIQUE_COLUMN_TYPE = 0;

    protected Connection conn = null;

    protected LoginInfo loginInfo = null;

    protected abstract void createConnection();

    public static IDBConnection getInstance(LoginInfo pLoginUserInfo) {
        if (pLoginUserInfo == null || pLoginUserInfo.getDatabaseInfo() == null) {
            throw new RuntimeException("LoginUserInfo is null or DataBaseInfo is null");
        }
        DataBaseInfo tDataBaseInfo = pLoginUserInfo.getDatabaseInfo();
        switch(tDataBaseInfo.getType()) {
            case DataBaseInfo.TYPE_SQLSERVER:
                return new DBConnSQLServer(pLoginUserInfo);
            case DataBaseInfo.TYPE_POSTGRESQL:
                return new DBConnPostgreSQL(pLoginUserInfo);
            case DataBaseInfo.TYPE_ORACLE:
            case DataBaseInfo.TYPE_MYSQL:
        }
        throw new RuntimeException("Unknow type = " + tDataBaseInfo.getType());
    }

    protected AbstractDBConnection(LoginInfo pLoginInfo) {
        loginInfo = pLoginInfo;
        setBuilder(AbstractSQLBuilder.getSQLBuilder(pLoginInfo));
    }

    public Connection getConnection() {
        createConnection();
        return conn;
    }

    public List<String> getAllTables() {
        List<String> tOut = new ArrayList<String>();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getTables(loginInfo.getSchema(), SCHEMA_PATTERN, null, new String[] { "TABLE" });
            while (tRS.next()) {
                String tTable = tRS.getString("TABLE_NAME");
                if (!tOut.contains(tTable)) {
                    tOut.add(tTable);
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public NameMap getReference(String pTableName) {
        NameMap tOut = new NameMap();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getImportedKeys(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName);
            while (tRS.next()) {
                String tFkName = tRS.getString("FK_NAME").intern();
                String tFkColumnName = tRS.getString("FKCOLUMN_NAME").intern();
                String tRefTableName = tRS.getString("PKTABLE_NAME").intern();
                String tRefColumnName = tRS.getString("PKCOLUMN_NAME").intern();
                tOut.addColumn(tFkName, new NameMapCell(tFkColumnName, tRefTableName, tRefColumnName));
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public List<String> getRefTable(String pTableName) {
        List<String> tOut = new ArrayList<String>();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getImportedKeys(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName);
            while (tRS.next()) {
                String tTable = tRS.getString("PKTABLE_NAME");
                if (!tOut.contains(tTable)) {
                    tOut.add(tTable);
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public static void main(String[] args) {
        DataBaseInfo tDataBaseInfo = new DataBaseInfo(DataBaseInfo.TYPE_SQLSERVER, "sa", "123456", "127.0.0.1", "1433");
        LoginInfo tLoginInfos = new LoginInfo(tDataBaseInfo, "ienc_system");
        IDBConnection tConn = AbstractDBConnection.getInstance(tLoginInfos);
        tConn.getRefTable(new SchemaTable(tLoginInfos.getSchema(), "c_schedule"));
        tConn.close();
    }

    public List<SchemaTable> getRefTable(SchemaTable pTableName) {
        List<SchemaTable> tOut = new ArrayList<SchemaTable>();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getImportedKeys(pTableName.getSchema(), SCHEMA_PATTERN, pTableName.getTable());
            while (tRS.next()) {
                SchemaTable tTable = new SchemaTable(tRS.getString("PKTABLE_CAT"), tRS.getString("PKTABLE_NAME"));
                if (!tOut.contains(tTable)) {
                    tOut.add(tTable);
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public List<RefTableMapping> getRefTableMapping(SchemaTable pTable1, SchemaTable pTable2) {
        List<RefTableMapping> tOut = new ArrayList<RefTableMapping>();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getCrossReference(pTable2.getSchema(), null, pTable2.getTable(), pTable1.getSchema(), null, pTable1.getTable());
            while (tRS.next()) {
                tOut.add(new RefTableMapping(tRS.getString("FKTABLE_NAME").intern(), tRS.getString("FKCOLUMN_NAME").intern(), tRS.getString("PKTABLE_NAME").intern(), tRS.getString("PKCOLUMN_NAME").intern()));
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public List<String> getReferenceColumn(String pTableName) {
        List<String> tOut = new ArrayList<String>();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getImportedKeys(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName);
            while (tRS.next()) {
                String tTable = tRS.getString("FKCOLUMN_NAME");
                if (!tOut.contains(tTable)) {
                    tOut.add(tTable);
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public String getInsertOrderString() {
        List<String> tMyTable = getAllTables();
        if (tMyTable.size() == 0) return StringUtil.EMPTY;
        StringBuffer tOutput = new StringBuffer();
        String tTableName = tMyTable.get(0);
        String tNextTable = null;
        boolean isCanInsert = true;
        while (!tMyTable.isEmpty()) {
            List<String> tRefTable = getRefTable(tTableName);
            isCanInsert = true;
            for (String s : tRefTable) {
                if (!s.equals(tTableName)) {
                    tNextTable = s;
                    if (tMyTable.contains(tNextTable)) {
                        tTableName = tNextTable;
                        isCanInsert = false;
                        break;
                    }
                }
            }
            if (isCanInsert) {
                if (tMyTable.remove(tTableName)) {
                    tOutput.append(tTableName).append(";");
                }
                if (tMyTable.size() > 0) {
                    tTableName = tMyTable.get(0);
                }
            }
        }
        return tOutput.toString();
    }

    public List<String> getInsertOrder() {
        return StringUtil.arrayToList(getInsertOrderString().split(";"));
    }

    public NameMap getIndexs(String pTableName) {
        List<String> tPkColumn = getPrimaryKeyColumn(pTableName);
        NameMap tOut = new NameMap();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getIndexInfo(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName, false, false);
            while (tRS.next()) {
                if (tRS.getBoolean("NON_UNIQUE")) {
                    String tColumn = tRS.getString("COLUMN_NAME").intern();
                    if (StringUtil.isNotEmptyOrSpace(tColumn) && !tPkColumn.contains(tColumn.trim())) {
                        tOut.addColumn(tRS.getString("INDEX_NAME").intern(), new NameMapCell(tColumn, tRS.getString("ASC_OR_DESC").intern()));
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public List<String> getIndexColumn(String pTableName) {
        Set<String> tSet = new HashSet<String>();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getIndexInfo(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName, false, false);
            while (tRS.next()) {
                if (tRS.getBoolean("NON_UNIQUE")) {
                    tSet.add(tRS.getString("COLUMN_NAME").intern());
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return new ArrayList<String>(tSet);
    }

    /**
	 * 取得uk 但是不含 pk
	 */
    public NameMap getUniques(String pTableName) {
        List<String> tPkColumn = getPrimaryKeyColumn(pTableName);
        NameMap tOut = new NameMap();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getIndexInfo(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName, true, false);
            while (tRS.next()) {
                if (tRS.getInt("TYPE") != 0) {
                    String tColumn = tRS.getString("COLUMN_NAME").intern();
                    if (StringUtil.isNotEmptyOrSpace(tColumn) && !tPkColumn.contains(tColumn.trim())) {
                        tOut.addColumn(tRS.getString("INDEX_NAME").intern(), new NameMapCell(tColumn, tRS.getString("ASC_OR_DESC").intern()));
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public List<String> getUniqueColumn(String pTableName) {
        List<String> tPkColumn = getPrimaryKeyColumn(pTableName);
        Set<String> tSet = new HashSet<String>();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getIndexInfo(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName, true, false);
            while (tRS.next()) {
                if (tRS.getInt("TYPE") == UNIQUE_COLUMN_TYPE) {
                    String tColumn = tRS.getString("COLUMN_NAME").intern();
                    if (!tPkColumn.contains(tColumn)) {
                        tSet.add(tColumn);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return new ArrayList<String>(tSet);
    }

    public NameMap getPrimaryKeys(String pTableName) {
        NameMap tOut = new NameMap();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getPrimaryKeys(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName);
            while (tRS.next()) {
                tOut.addColumn(tRS.getString("PK_NAME").intern(), new NameMapCell(tRS.getString("COLUMN_NAME").intern()));
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public List<String> getPrimaryKeyColumn(String pTableName) {
        Set<String> tSet = new HashSet<String>();
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getPrimaryKeys(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName);
            while (tRS.next()) {
                tSet.add(tRS.getString("COLUMN_NAME").intern());
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return new ArrayList<String>(tSet);
    }

    public ColumnInfo getColumnInfo(String pTableName, String pColumnName) {
        ColumnInfo tOut = null;
        ResultSet tRS = null;
        try {
            tRS = getConnection().getMetaData().getColumns(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName, pColumnName);
            if (tRS.next()) {
                tOut = new ColumnInfo(tRS.getString("COLUMN_NAME"), tRS.getInt("DATA_TYPE"), tRS.getString("TYPE_NAME"), tRS.getInt("COLUMN_SIZE"), tRS.getInt("NULLABLE") == 1, tRS.getString("COLUMN_DEF"));
            }
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public List<ColumnInfo> getColumnInfo(String pTableName) {
        List<ColumnInfo> tOut = new ArrayList<ColumnInfo>();
        ResultSet tRS = null;
        String tCurrentName = null;
        try {
            tRS = getConnection().getMetaData().getColumns(loginInfo.getSchema(), SCHEMA_PATTERN, pTableName, null);
            while (tRS.next()) {
                tCurrentName = tRS.getString("COLUMN_NAME");
                tOut.add(new ColumnInfo(tRS.getString("COLUMN_NAME"), tRS.getInt("DATA_TYPE"), tRS.getString("TYPE_NAME"), tRS.getInt("COLUMN_SIZE"), tRS.getInt("NULLABLE") == 1, tRS.getString("COLUMN_DEF")));
            }
        } catch (Exception e) {
            throw new DBException(pTableName + " " + tCurrentName, e);
        } finally {
            close(tRS);
        }
        return tOut;
    }

    public ColumnFullInfo getColumnFullInfo(String pTableName, String pColumnName) {
        for (ColumnFullInfo f : getColumnFullInfo(pTableName)) {
            if (f.getName().equalsIgnoreCase(pColumnName)) {
                return f;
            }
        }
        return null;
    }

    public List<ColumnFullInfo> getColumnFullInfo(String pTableName) {
        List<ColumnInfo> tColumnInfos = getColumnInfo(pTableName);
        List<ColumnFullInfo> tOut = new ArrayList<ColumnFullInfo>(tColumnInfos.size());
        List<String> tPrimaryKeyColumn = getPrimaryKeyColumn(pTableName);
        List<String> tUniqueColumn = getUniqueColumn(pTableName);
        List<String> tIndexColumn = getIndexColumn(pTableName);
        List<String> tReferenceColumn = getReferenceColumn(pTableName);
        for (ColumnInfo c : tColumnInfos) {
            ColumnFullInfo tInfo = new ColumnFullInfo(c);
            tOut.add(tInfo);
            if (tPrimaryKeyColumn.contains(tInfo.getName())) {
                tInfo.setPrimaryKey(true);
            }
            if (tUniqueColumn.contains(tInfo.getName())) {
                tInfo.setUnique(true);
            }
            if (tIndexColumn.contains(tInfo.getName())) {
                tInfo.setIndex(true);
            }
            if (tReferenceColumn.contains(tInfo.getName())) {
                tInfo.setReference(true);
            }
        }
        return initNullableName(pTableName, tOut);
    }

    /**
	 * 要在子類別考慮有些資料庫有設定這個限制
	 * @param pTableName
	 * @param pOut
	 * @return
	 */
    protected List<ColumnFullInfo> initNullableName(String pTableName, List<ColumnFullInfo> pOut) {
        return pOut;
    }

    public boolean cleanAllTablesData() {
        Statement tStat = getStatement();
        setAutoCommit(false);
        try {
            List<String> tAllTables = getInsertOrder();
            String tDelete = null;
            for (int i = tAllTables.size() - 1; i >= 0; i--) {
                tDelete = "Delete From ".intern() + tAllTables.get(i).intern();
                executeUpdate(tStat, tDelete);
            }
            commit();
            return true;
        } catch (Exception e) {
            rollback();
            LOG.error("", e);
        } finally {
            close(tStat);
            setAutoCommit(true);
        }
        return false;
    }

    public Statement getStatement() {
        try {
            return getConnection().createStatement();
        } catch (Exception e) {
            throw new DBException(e);
        }
    }

    public ResultSet getQueryResult(Statement pState, String pSQL) {
        ResultSet tRs = null;
        try {
            tRs = pState.executeQuery(pSQL);
        } catch (Exception e) {
            throw new DBException(e);
        }
        return tRs;
    }

    public void executeUpdate(Statement pState, String pSQL) {
        try {
            pState.executeUpdate(pSQL);
        } catch (Exception e) {
            throw new DBException(e);
        }
    }

    public void executeUpdate(String pSQL) {
        Statement tState = null;
        try {
            tState = getStatement();
            executeUpdate(tState, pSQL);
        } catch (Exception e) {
            throw new DBException(pSQL, e);
        } finally {
            close(tState);
        }
    }

    public void executeBatchUpdate(String pSQL) {
        if (StringUtil.isNotEmptyOrSpace(pSQL)) {
            Statement tState = null;
            String tCmd = null;
            try {
                tState = getStatement();
                SqlTokenizer tSqlTokenizer = new SqlTokenizer(pSQL);
                while (tSqlTokenizer.hasMoreStatements()) {
                    tCmd = tSqlTokenizer.getNextStatement().trim();
                    if (StringUtil.isNotEmptyOrSpace(tCmd)) {
                        executeUpdate(tState, tCmd);
                    }
                }
            } catch (Exception e) {
                throw new DBException(pSQL, e);
            } finally {
                close(tState);
            }
        }
    }

    public ResultSet getTableData(Statement pState, String pTableName) {
        return getQueryResult(pState, "select * from ".intern() + pTableName);
    }

    public void setAutoCommit(boolean pCommit) {
        try {
            getConnection().setAutoCommit(pCommit);
        } catch (Exception e) {
            throw new DBException(e);
        }
    }

    public void commit() {
        try {
            getConnection().commit();
        } catch (Exception e) {
            throw new DBException(e);
        }
    }

    public void rollback() {
        try {
            getConnection().rollback();
        } catch (Exception e) {
            throw new DBException(e);
        }
    }

    public boolean isHasTable(String pTableName) {
        return getAllTables().contains(pTableName);
    }

    public boolean isHasRow(String pTableName, List<ColumnInfoValue> pValues) {
        boolean tOutput = false;
        Statement tState = null;
        ResultSet tRs = null;
        try {
            StringBuffer tSQL = new StringBuffer();
            tSQL.append("Select count(*) as count From ").append(pTableName).append(" where 1=1 ");
            for (ColumnInfoValue n : pValues) {
                if (n.getColumnInfo().getColumnType() == ColumnType.Date) {
                    continue;
                }
                tSQL.append(" and ").append(n.getColumnInfo().getName());
                if (StringUtil.isEmptyOrSpace(n.toSQL())) {
                    tSQL.append(" is ");
                } else {
                    tSQL.append(" = ");
                }
                tSQL.append(n.toSQL());
            }
            tState = getStatement();
            tRs = getQueryResult(tState, tSQL.toString());
            if (tRs.next()) {
                if (tRs.getInt("count") > 0) {
                    tOutput = true;
                }
            }
        } catch (Exception e) {
            throw new DBException(e);
        } finally {
            close(tRs, tState);
        }
        return tOutput;
    }

    protected void showReslutSet(ResultSet pResultSet) {
        try {
            for (int i = 0; i < pResultSet.getMetaData().getColumnCount(); i++) {
                System.out.print(pResultSet.getMetaData().getColumnName(i + 1) + ", ");
            }
            System.out.println("");
            while (pResultSet.next()) {
                for (int i = 0; i < pResultSet.getMetaData().getColumnCount(); i++) {
                    System.out.print(pResultSet.getString(i + 1) + " ,");
                }
                System.out.println("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean deleteByColumn(String pTableName, ColumnInfoValue pColumn) {
        String tSQL = "";
        try {
            tSQL = "delete from " + pTableName + " where " + pColumn.getColumnInfo().getName() + "=" + pColumn.toSQL();
            executeUpdate(tSQL);
            return true;
        } catch (Exception e) {
            LOG.error(tSQL, e);
        }
        return false;
    }

    public void close(ResultSet pRS) {
        try {
            if (pRS != null) {
                pRS.close();
            }
        } catch (Exception e) {
        }
    }

    public void close(Statement pState) {
        try {
            if (pState != null) {
                pState.close();
            }
        } catch (Exception e) {
        }
    }

    public void close(ResultSet pRS, Statement pState) {
        close(pRS);
        close(pState);
    }

    public void close() {
        close(getConnection());
    }

    public void close(Connection pConn) {
        try {
            if (pConn != null && !pConn.isClosed()) {
                pConn.close();
            }
        } catch (Exception e) {
        }
    }

    public DBRow getRow(ResultSet pRs) {
        return null;
    }

    public ISQLBuilder getBuilder() {
        return builder;
    }

    public void setBuilder(ISQLBuilder builder) {
        this.builder = builder;
    }
}
