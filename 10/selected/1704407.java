package rafaelortis.dbsmartcopy.metadataviewer.dbanalizer;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.sql.BLOB;
import rafaelortis.dbsmartcopy.metadataviewer.IOHelper;

/**
 *
 * @author ortis
 */
public class DBUtil {

    /**
     *
     * @param tableMetaData
     * @param filter Data Filter
     * @throws Exception
     */
    public static void readTableData(TableMetaData tableMetaData, String filter) throws Exception {
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        try {
            String sql = "SELECT * FROM " + tableMetaData.getTableName();
            if (filter != null) {
                sql += " WHERE " + filter;
            }
            IOHelper.writeInfo("SELECT" + sql);
            ps = tableMetaData.getConn().prepareStatement(sql);
            rs = ps.executeQuery();
            rsmd = ps.getMetaData();
            int columnCnt = 1;
            while (rs.next()) {
                Row r = new Row();
                for (columnCnt = 1; columnCnt <= rsmd.getColumnCount(); columnCnt++) {
                    if (tableMetaData.getConn() instanceof OracleConnection) {
                        if (rsmd.getColumnTypeName(columnCnt).equalsIgnoreCase("BLOB")) {
                            BLOB blob = ((OracleResultSet) rs).getBLOB(columnCnt);
                            InputStream is = blob.getBinaryStream();
                            byte data[] = new byte[is.available()];
                            is.read(data);
                            is.close();
                            r.getRowData().put(rsmd.getColumnName(columnCnt), data);
                        } else if (rsmd.getColumnTypeName(columnCnt).equalsIgnoreCase("LONG")) {
                            r.getRowData().put(rsmd.getColumnName(columnCnt), rs.getBytes(columnCnt));
                        }
                    } else {
                        r.getRowData().put(rsmd.getColumnName(columnCnt), rs.getObject(columnCnt));
                    }
                }
                tableMetaData.getData().add(r);
            }
            if (tableMetaData.getData().isEmpty()) IOHelper.writeInfo("No data Found");
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    public static void insertTableData(Connection dest, TableMetaData tableMetaData) throws Exception {
        PreparedStatement ps = null;
        try {
            dest.setAutoCommit(false);
            String sql = "INSERT INTO " + tableMetaData.getSchema() + "." + tableMetaData.getTableName() + " (";
            for (String columnName : tableMetaData.getColumnsNames()) {
                sql += columnName + ",";
            }
            sql = sql.substring(0, sql.length() - 1);
            sql += ") VALUES (";
            for (String columnName : tableMetaData.getColumnsNames()) {
                sql += "?" + ",";
            }
            sql = sql.substring(0, sql.length() - 1);
            sql += ")";
            IOHelper.writeInfo(sql);
            ps = dest.prepareStatement(sql);
            for (Row r : tableMetaData.getData()) {
                try {
                    int param = 1;
                    for (String columnName : tableMetaData.getColumnsNames()) {
                        if (dest instanceof OracleConnection) {
                            if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("BLOB")) {
                                BLOB blob = new BLOB((OracleConnection) dest, (byte[]) r.getRowData().get(columnName));
                                ((OraclePreparedStatement) ps).setBLOB(param, blob);
                            } else if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("CLOB")) {
                                ((OraclePreparedStatement) ps).setStringForClob(param, (String) r.getRowData().get(columnName));
                            } else if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("LONG")) {
                                ps.setBytes(param, (byte[]) r.getRowData().get(columnName));
                            }
                        } else {
                            IOHelper.writeInfo(columnName + " = " + r.getRowData().get(columnName));
                            ps.setObject(param, r.getRowData().get(columnName));
                        }
                        param++;
                    }
                    if (ps.executeUpdate() != 1) {
                        dest.rollback();
                        updateTableData(dest, tableMetaData, r);
                    }
                } catch (Exception ex) {
                    try {
                        dest.rollback();
                        updateTableData(dest, tableMetaData, r);
                    } catch (Exception ex2) {
                        IOHelper.writeError("Error in update " + sql, ex2);
                    }
                }
                ps.clearParameters();
            }
            dest.commit();
            dest.setAutoCommit(true);
        } finally {
            if (ps != null) ps.close();
        }
    }

    public static void updateTableData(Connection dest, TableMetaData tableMetaData) throws Exception {
        PreparedStatement ps = null;
        try {
            dest.setAutoCommit(false);
            String sql = "UPDATE " + tableMetaData.getSchema() + "." + tableMetaData.getTableName() + " SET ";
            for (String columnName : tableMetaData.getColumnsNames()) {
                sql += columnName + " = ? ,";
            }
            sql = sql.substring(0, sql.length() - 1);
            sql += " WHERE ";
            for (String pkColumnName : tableMetaData.getPkColumns()) {
                sql += pkColumnName + " = ? AND ";
            }
            sql = sql.substring(0, sql.length() - 4);
            ps = dest.prepareStatement(sql);
            for (Row r : tableMetaData.getData()) {
                int param = 1;
                for (String columnName : tableMetaData.getColumnsNames()) {
                    if (dest instanceof OracleConnection) {
                        if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("BLOB")) {
                            BLOB blob = new BLOB((OracleConnection) dest, (byte[]) r.getRowData().get(columnName));
                            ((OraclePreparedStatement) ps).setBLOB(param, blob);
                        } else if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("CLOB")) {
                            ((OraclePreparedStatement) ps).setStringForClob(param, (String) r.getRowData().get(columnName));
                        } else if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("LONG")) {
                            ps.setBytes(param, (byte[]) r.getRowData().get(columnName));
                        }
                    } else {
                        ps.setObject(param, r.getRowData().get(columnName));
                    }
                    param++;
                }
                for (String pkColumnName : tableMetaData.getPkColumns()) {
                    ps.setObject(param, r.getRowData().get(pkColumnName));
                    param++;
                }
                if (ps.executeUpdate() != 1) {
                    dest.rollback();
                    throw new Exception();
                }
                ps.clearParameters();
            }
            dest.commit();
            dest.setAutoCommit(true);
        } finally {
            if (ps != null) ps.close();
        }
    }

    public static void updateTableData(Connection dest, TableMetaData tableMetaData, Row r) throws Exception {
        PreparedStatement ps = null;
        try {
            dest.setAutoCommit(false);
            String sql = "UPDATE " + tableMetaData.getSchema() + "." + tableMetaData.getTableName() + " SET ";
            for (String columnName : tableMetaData.getColumnsNames()) {
                sql += columnName + " = ? ,";
            }
            sql = sql.substring(0, sql.length() - 1);
            sql += " WHERE ";
            for (String pkColumnName : tableMetaData.getPkColumns()) {
                sql += pkColumnName + " = ? AND ";
            }
            sql = sql.substring(0, sql.length() - 4);
            System.out.println("UPDATE: " + sql);
            ps = dest.prepareStatement(sql);
            int param = 1;
            for (String columnName : tableMetaData.getColumnsNames()) {
                if (dest instanceof OracleConnection) {
                    if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("BLOB")) {
                        BLOB blob = new BLOB((OracleConnection) dest, (byte[]) r.getRowData().get(columnName));
                        ((OraclePreparedStatement) ps).setBLOB(param, blob);
                    } else if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("CLOB")) {
                        ((OraclePreparedStatement) ps).setStringForClob(param, (String) r.getRowData().get(columnName));
                    } else if (tableMetaData.getColumnsTypes().get(columnName).equalsIgnoreCase("LONG")) {
                        ps.setBytes(param, (byte[]) r.getRowData().get(columnName));
                    }
                } else {
                    ps.setObject(param, r.getRowData().get(columnName));
                }
                param++;
            }
            for (String pkColumnName : tableMetaData.getPkColumns()) {
                ps.setObject(param, r.getRowData().get(pkColumnName));
                param++;
            }
            if (ps.executeUpdate() != 1) {
                dest.rollback();
                throw new Exception("Erro no update");
            }
            ps.clearParameters();
            dest.commit();
            dest.setAutoCommit(true);
        } finally {
            if (ps != null) ps.close();
        }
    }

    public static void readTableDataByReference(TableMetaData tableMetaData, String referencedBy, List<Row> relatedData, boolean imported) throws Exception {
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        List<Object> values = new ArrayList<Object>();
        if ("assuntos".equals(tableMetaData.getTableName())) {
            IOHelper.writeInfo("Reading assuntos");
        }
        try {
            String sql = "SELECT * FROM " + tableMetaData.getTableName();
            List<ForeignKey> fksList = new ArrayList<ForeignKey>();
            if (tableMetaData.getType() == TableMetaData.MD_FROM_IMPORTED_KEY || imported) fksList.addAll(tableMetaData.getExportedKeys()); else if (tableMetaData.getType() == TableMetaData.MD_FROM_EXPORTED_KEY) fksList.addAll(tableMetaData.getImportedKeys());
            for (ForeignKey fk : fksList) {
                if (sql.contains(" WHERE ")) {
                    sql += " OR ";
                } else {
                    sql += " WHERE ";
                }
                if (fk.getReferenceTable().equalsIgnoreCase(referencedBy)) {
                    for (Row r : relatedData) {
                        sql += "( ";
                        for (String column : tableMetaData.getColumnsNames()) {
                            if (fk.getReference().containsKey(column)) {
                                sql += column + " = ? AND ";
                                values.add(r.getRowData().get(fk.getReference().get(column)));
                            }
                        }
                        sql = sql.substring(0, sql.length() - 4);
                        sql += ") OR ";
                    }
                }
                if (sql.endsWith(" WHERE ")) {
                    sql = sql.substring(0, sql.length() - 6);
                } else {
                    sql = sql.substring(0, sql.length() - 3);
                }
            }
            IOHelper.writeInfo("Select: " + sql);
            if (sql.contains("WHERE")) {
                ps = tableMetaData.getConn().prepareStatement(sql);
                int cnt = 1;
                for (Object obj : values) {
                    ps.setObject(cnt, obj);
                    cnt++;
                }
                rs = ps.executeQuery();
                rsmd = ps.getMetaData();
                int columnCnt = 1;
                while (rs.next()) {
                    Row r = new Row();
                    for (columnCnt = 1; columnCnt <= rsmd.getColumnCount(); columnCnt++) {
                        if (tableMetaData.getConn() instanceof OracleConnection) {
                            if (rsmd.getColumnTypeName(columnCnt).equalsIgnoreCase("BLOB")) {
                                BLOB blob = ((OracleResultSet) rs).getBLOB(columnCnt);
                                InputStream is = blob.getBinaryStream();
                                byte data[] = new byte[is.available()];
                                is.read(data);
                                is.close();
                                r.getRowData().put(rsmd.getColumnName(columnCnt), data);
                            } else if (rsmd.getColumnTypeName(columnCnt).equalsIgnoreCase("LONG")) {
                                r.getRowData().put(rsmd.getColumnName(columnCnt), rs.getBytes(columnCnt));
                            }
                        } else {
                            r.getRowData().put(rsmd.getColumnName(columnCnt), rs.getObject(columnCnt));
                        }
                    }
                    if (!tableMetaData.getData().contains(r)) tableMetaData.getData().add(r);
                }
                IOHelper.writeInfo("Rows retrieved :" + tableMetaData.getData().size());
            } else {
                IOHelper.writeInfo("No Rows retrieved :" + tableMetaData.getTableName());
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }
}
