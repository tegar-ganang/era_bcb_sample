package com.jspx.sober.util;

import static java.lang.System.arraycopy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.sql.*;
import java.util.Map;
import java.util.HashMap;
import java.io.Reader;
import java.io.StringWriter;
import java.io.StringReader;
import com.jspx.sober.config.SoberColumn;
import com.jspx.sober.SoberORM;

/**
 * Created by IntelliJ IDEA.
 * User:chenYuan (mail:cayurain@21cn.com)
 * Date: 2007-1-6
 * Time: 18:09:10
 *
 */
public abstract class JdbcUtil {

    /**
     * Constant that indicates an unknown (or unspecified) SQL type.
     *
     * @see java.sql.Types
     */
    private static final Log logger = LogFactory.getLog(JdbcUtil.class);

    public static Object[] appendArray(Object[] array, Object append) {
        if (array == null) {
            array = new Object[1];
            array[0] = append;
            return array;
        }
        Object[] result = new Object[array.length + 1];
        arraycopy(array, 0, result, 0, array.length);
        result[array.length] = append;
        return result;
    }

    public static Object[] appendArray(Object[] array, Object[] append) {
        if (array == null) {
            return append;
        }
        if (append == null) {
            return array;
        }
        int length = array.length + append.length;
        Object[] result = new Object[length];
        System.arraycopy(array, 0, result, 0, array.length);
        System.arraycopy(append, array.length - array.length, result, array.length, length - array.length);
        return result;
    }

    /**
     * Close the given JDBC Statement and ignore any thrown exception.
     * This is useful for typical finally blocks in manual JDBC code.
     *
     * @param stmt the JDBC Statement to close (may be <code>null</code>)
     */
    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.clearBatch();
                stmt.clearWarnings();
                stmt.close();
            } catch (SQLException ex) {
                logger.warn("Could not close JDBC Statement", ex);
            } catch (Throwable ex) {
                logger.error("Unexpected exception on closing JDBC Statement", ex);
            }
        }
    }

    /**
     * Close the given JDBC ResultSet and ignore any thrown exception.
     * This is useful for typical finally blocks in manual JDBC code.
     *
     * @param rs the JDBC ResultSet to close (may be <code>null</code>)
     */
    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                logger.warn("Could not close JDBC ResultSet", ex);
            } catch (Throwable ex) {
                logger.error("Unexpected exception on closing JDBC ResultSet", ex);
            }
        }
    }

    /**
     * Retrieve a JDBC column value from a ResultSet, using the most appropriate
     * value type. The returned value should be a detached value object, not having
     * any ties to the active ResultSet: in particular, it should not be a Blob or
     * Clob object but rather a byte array respectively String representation.
     * <p>Uses the <code>getObject(index)</code> method, but includes additional "hacks"
     * to get around Oracle 10g returning a non-standard object for its TIMESTAMP
     * datatype and a <code>java.sql.Date</code> for DATE columns leaving out the
     * time portion: These columns will explicitly be extracted as standard
     * <code>java.sql.Timestamp</code> object.
     *
     * @param rs is the ResultSet holding the data
     * @return the value object
     * @see java.sql.Blob
     * @see java.sql.Clob
     * @see java.sql.Timestamp
     */
    public static String getLargeTextField(ResultSet rs, int columnIndex) throws SQLException {
        Reader bodyReader;
        String value;
        try {
            bodyReader = rs.getCharacterStream(columnIndex);
            if (bodyReader == null) return null;
            char buf[] = new char[256];
            StringWriter out = new StringWriter(255);
            int i;
            while ((i = bodyReader.read(buf)) >= 0) out.write(buf, 0, i);
            value = out.toString();
            out.close();
            bodyReader.close();
        } catch (Exception e) {
            return rs.getString(columnIndex);
        }
        if (value == null) return "";
        return value;
    }

    public static String getLargeTextField(ResultSet rs, String columnName) throws SQLException {
        Reader bodyReader;
        String value;
        try {
            bodyReader = rs.getCharacterStream(columnName);
            if (bodyReader == null) return null;
            char buf[] = new char[256];
            StringWriter out = new StringWriter(255);
            int i;
            while ((i = bodyReader.read(buf)) >= 0) out.write(buf, 0, i);
            value = out.toString();
            out.close();
            bodyReader.close();
        } catch (Exception e) {
            return rs.getString(columnName);
        }
        if (value == null) return "";
        return value;
    }

    public static void setLargeTextField(PreparedStatement pstmt, int parameterIndex, String value) throws SQLException {
        Reader bodyReader;
        try {
            bodyReader = new StringReader(value);
            pstmt.setCharacterStream(parameterIndex, bodyReader, value.length());
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException("Failed to set text field.");
        }
    }

    /**
     * Return whether the given JDBC driver supports JDBC 2.0 batch updates.
     * <p>Typically invoked right before execution of a given set of statements:
     * to decide whether the set of SQL statements should be executed through
     * the JDBC 2.0 batch mechanism or simply in a traditional one-by-one fashion.
     * <p>Logs a warning if the "supportsBatchUpdates" methods throws an exception
     * and simply returns false in that case.
     *
     * @param con the Connection to check
     * @return whether JDBC 2.0 batch updates are supported
     * @see java.sql.DatabaseMetaData#supportsBatchUpdates
     */
    public static boolean supportsBatchUpdates(Connection con) {
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            if (dbmd != null) {
                if (dbmd.supportsBatchUpdates()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("JDBC driver supports batch updates");
                    }
                    return true;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("JDBC driver does not support batch updates");
                    }
                }
            }
        } catch (SQLException ex) {
            logger.warn("JDBC driver 'supportsBatchUpdates' method threw exception", ex);
        } catch (AbstractMethodError err) {
            logger.warn("JDBC driver does not support JDBC 2.0 'supportsBatchUpdates' method", err);
        }
        return false;
    }

    /**
     * 得到字段信息
     *
     * @param resultSet
     * @return Map<String, ColumnType>
     */
    public static Map<String, SoberColumn> getFieldType(final ResultSet resultSet) {
        Map<String, SoberColumn> result = new HashMap<String, SoberColumn>();
        try {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                SoberColumn soberColumn = new SoberColumn();
                soberColumn.setName(rsmd.getColumnName(i));
                result.put(soberColumn.getName(), soberColumn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new HashMap<String, SoberColumn>();
        }
        return result;
    }

    /**
     * 得到数据库名
     * @param con   连接
     * @return  String
     * @throws SQLException 连接错误
     */
    public static String getDatabaseName(Connection con) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        String dbName = metaData.getDatabaseProductName().toLowerCase();
        String driverName = metaData.getDriverName().toLowerCase();
        if (driverName.toLowerCase().indexOf("oracle") != -1 || dbName.toLowerCase().indexOf("oracle") != -1) {
            return SoberORM.ORACLE;
        }
        if (driverName.toLowerCase().indexOf("postgre") != -1 || dbName.toLowerCase().indexOf("postgre") != -1) {
            return SoberORM.POSTGRE;
        }
        if (driverName.toLowerCase().indexOf("interbase") != -1 || dbName.toLowerCase().indexOf("interbase") != -1) {
            return SoberORM.INTERBASE;
        }
        if (driverName.toLowerCase().indexOf("sqlserver") != -1 || driverName.toLowerCase().indexOf("sql server") != -1) {
            return SoberORM.MSSQL;
        }
        if (driverName.toLowerCase().indexOf("mysql") != -1 || dbName.toLowerCase().indexOf("mysql") != -1) {
            return SoberORM.MYSQL;
        }
        if (driverName.toLowerCase().indexOf("db2") != -1 || dbName.toLowerCase().indexOf("db2") != -1) {
            return SoberORM.DB2;
        }
        if (driverName.toLowerCase().indexOf("firebird") != -1 || dbName.toLowerCase().indexOf("firebird") != -1) {
            return SoberORM.Firebird;
        }
        if (driverName.toLowerCase().indexOf("smalldb") != -1 || dbName.toLowerCase().indexOf("smalldb") != -1) {
            return SoberORM.Smalldb;
        }
        return SoberORM.General;
    }
}
