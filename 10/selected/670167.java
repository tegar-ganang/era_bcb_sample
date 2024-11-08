package com.creawor.generator;

import com.creawor.cicf.connection.ConnectionManager;
import com.creawor.hz_market.servlet.LoadMapInfoAjax;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

public class SqlQueryBean {

    private static final Logger logger = Logger.getLogger(LoadMapInfoAjax.class);

    public SqlQueryBean() {
        connparm = new SqlConnParms();
        friendlyColumnNames = null;
        QryTitle = null;
        page = null;
        row = null;
        intpagesize = 10;
        record_count = 0;
        field_count = 0;
        ds = null;
        Config_bean cb = new Config_bean();
        connparm.jdbcURL = Config_bean.url;
        connparm.driverName = Config_bean.driverName;
        connparm.username = Config_bean.uid;
        connparm.password = Config_bean.pwd;
        connparm.datasource = Config_bean.datasource;
    }

    public int connect() {
        try {
            conn = cm.getConnection();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void connect(String driverName1, String jdbcURL1, String username1, String password1) {
        try {
            if (conn == null) {
                Class.forName(driverName1).newInstance();
                conn = DriverManager.getConnection(jdbcURL1, username1, password1);
            }
        } catch (Exception e) {
        }
    }

    public static void printDataSourceStats(DataSource ds) throws SQLException {
        BasicDataSource bds = (BasicDataSource) ds;
    }

    public static void shutdownDataSource(DataSource ds) throws SQLException {
        BasicDataSource bds = (BasicDataSource) ds;
        bds.close();
    }

    public int disconnect() {
        try {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (conn != null && !conn.isClosed()) {
                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            return 0;
        }
        return 1;
    }

    public ResultSet executeQuery() {
        try {
            return executeQuery(SQLstr);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ResultSet executeQuery(String s) throws SQLException {
        if (conn == null || conn.isClosed()) throw new SQLException("* This connection has not been established yet.");
        if (s == null) throw new SQLException("SQL-statement is null.");
        conn.setAutoCommit(true);
        stmt = conn.createStatement(1005, 1008);
        int i = s.indexOf("from ");
        if (i < 0) i = s.indexOf("FROM ");
        if (i < 0) i = s.indexOf("From ");
        if (i < 0) throw new SQLException("ERROR: Can not find 'from' in sqlstr!");
        String s1 = s.substring(i);
        i = s1.indexOf("where ");
        if (i < 0) i = s1.indexOf("WHERE ");
        if (i < 0) i = s1.indexOf("Where ");
        if (i < 0) tablename = s1.substring(5); else tablename = s1.substring(5, i);
        s1 = (new StringBuilder("select count(*) ")).append(s1).toString();
        rs = stmt.executeQuery(s1);
        if (rs.next()) record_count = rs.getInt(1);
        rs.close();
        rs = stmt.executeQuery(s);
        rsmd = rs.getMetaData();
        field_count = rsmd.getColumnCount();
        return rs;
    }

    public int getRecordNum() {
        return record_count;
    }

    public int getColumnCount() {
        return field_count;
    }

    public String[] setColumnNames() throws SQLException {
        if (rsmd == null) throw new SQLException("ResultSet is null.");
        columnNames = new String[getColumnCount()];
        for (int i = 1; i <= columnNames.length; i++) columnNames[i - 1] = rsmd.getColumnName(i);
        if (friendlyColumnNames != null && !"".equals(friendlyColumnNames.trim())) {
            String sarray[] = friendlyColumnNames.split(",");
            for (int i = 0; i < sarray.length; i++) columnNames[i] = sarray[i];
        }
        return columnNames;
    }

    public void setFriendlyColumnNames(String friendColumnNames1) throws Exception {
        friendlyColumnNames = friendColumnNames1;
        setColumnNames();
        String sarray[] = friendColumnNames1.split(",");
        if (sarray.length != getColumnCount()) {
            logger.debug(getColumnCount());
            logger.debug(sarray.length);
            logger.debug((new StringBuilder("[")).append(friendColumnNames1).append("]").toString());
            logger.debug("***Error:friendColumnNames's length is not right!\n");
            throw new Exception("Error:friendColumnNames's length is not right!");
        } else {
            return;
        }
    }

    public String getFriendlyColumnName(int i) throws SQLException {
        return columnNames[i - 1];
    }

    public String getFieldName(int i) throws SQLException {
        ResultSetMetaData resultsetmetadata = rs.getMetaData();
        return rsmd.getColumnName(i);
    }

    public void execUpdate(String sqlStmts[]) throws SQLException {
        if (conn == null || conn.isClosed()) throw new SQLException("The connection has not been established yet.");
        if (sqlStmts == null || sqlStmts.length == 0) throw new SQLException("SQL-statement is null.");
        conn.setAutoCommit(false);
        try {
            for (int i = 0; i < sqlStmts.length; i++) {
                stmt = conn.createStatement();
                stmt.executeUpdate(sqlStmts[i]);
                logger.debug(sqlStmts[i]);
                stmt.close();
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        }
    }

    public void execUpdate(String sqlStmts) throws SQLException {
        String ssss[] = new String[1];
        ssss[0] = sqlStmts;
        execUpdate(ssss);
    }

    public void setFieldValue(int column, String valueString) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null.");
        switch(rsmd.getColumnType(column)) {
            case -5:
                rs.updateLong(column, Long.parseLong(valueString));
            case -7:
            case -2:
                rs.updateBoolean(column, Boolean.valueOf(valueString).booleanValue());
            case 1:
                rs.updateString(column, valueString);
            case 91:
                rs.updateDate(column, Date.valueOf(valueString));
            case 3:
                rs.updateBigDecimal(column, new BigDecimal(valueString));
            case 8:
                rs.updateDouble(column, Double.parseDouble(valueString));
            case 6:
                rs.updateDouble(column, Double.parseDouble(valueString));
            case 4:
                rs.updateInt(column, Integer.parseInt(valueString));
            case -4:
            case -1:
                rs.updateString(column, valueString);
            case 0:
            case 2:
                rs.updateBigDecimal(column, new BigDecimal(valueString));
            case 7:
                rs.updateFloat(column, Float.parseFloat(valueString));
            case 5:
                rs.updateShort(column, Short.parseShort(valueString));
            case 92:
                rs.updateTime(column, Time.valueOf(valueString));
            case 93:
                rs.updateTimestamp(column, Timestamp.valueOf(valueString));
            case -6:
                rs.updateByte(column, Byte.parseByte(valueString));
            case -3:
            case 12:
                rs.updateString(column, valueString);
            default:
                rs.updateString(column, valueString);
                break;
        }
    }

    public int getFieldType(int column) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null."); else return rsmd.getColumnType(column);
    }

    public int getFieldType(String fieldName) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null."); else return rsmd.getColumnType(rs.findColumn(fieldName));
    }

    public String getFieldTypeStr(int column) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null."); else return rsmd.getColumnTypeName(column);
    }

    public String getFieldTypeStr(String fieldName) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null."); else return rsmd.getColumnTypeName(rs.findColumn(fieldName));
    }

    public int getFieldScale(int column) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null."); else return rsmd.getColumnDisplaySize(column);
    }

    public int getFieldScale(String columnname) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null."); else return getFieldScale(rs.findColumn(columnname));
    }

    public String getJavaType(String fieldName) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null."); else return getJavaType(rs.findColumn(fieldName));
    }

    public String getJavaType(int column) {
        String javaType = "String";
        try {
            switch(rsmd.getColumnType(column)) {
                case -7:
                    javaType = "boolean";
                    break;
                case -6:
                    javaType = "short";
                    break;
                case 93:
                    javaType = "java.util.Date";
                    break;
                case 92:
                    javaType = "java.util.Date";
                    break;
                case 91:
                    javaType = "java.util.Date";
                    break;
                case -1:
                    javaType = "String";
                    break;
                case 12:
                    javaType = "String";
                    break;
                case 1:
                    javaType = "String";
                    break;
                case 3:
                    javaType = "java.math.BigDecimal";
                    break;
                case 2:
                    if (rsmd.getScale(column) > 0) javaType = "double"; else javaType = "long";
                    break;
                case 8:
                    javaType = "double";
                    break;
                case 7:
                    javaType = "float";
                    break;
                case 6:
                    javaType = "double";
                    break;
                case -5:
                    javaType = "long";
                    break;
                case 4:
                    javaType = "int";
                    break;
                case 5:
                    javaType = "short";
                    break;
                case 2005:
                    javaType = "java.sql.Clob";
                    break;
                case 2004:
                    javaType = "java.sql.Blob";
                    break;
                default:
                    javaType = "String";
                    break;
            }
        } catch (SQLException e) {
            javaType = "String";
            e.printStackTrace();
        }
        return javaType;
    }

    public Object getField(int column, boolean convertToString) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null.");
        switch(rsmd.getColumnType(column)) {
            case -5:
                if (convertToString) return String.valueOf(rs.getLong(column)); else return new Long(rs.getLong(column));
            case -2:
                if (convertToString) return Byte.toString(rs.getByte(column)); else return new Byte(rs.getByte(column));
            case -7:
                if (convertToString) return String.valueOf(rs.getBoolean(column)); else return new Boolean(rs.getBoolean(column));
            case 1:
                return rs.getString(column);
            case 91:
                if (convertToString) return rs.getDate(column).toString(); else return rs.getDate(column);
            case 3:
                if (convertToString) return rs.getBigDecimal(column).toString(); else return rs.getBigDecimal(column);
            case 8:
                if (convertToString) return String.valueOf(rs.getDouble(column)); else return new Double(rs.getDouble(column));
            case 6:
                if (convertToString) return String.valueOf(rs.getDouble(column)); else return new Float(rs.getDouble(column));
            case 4:
                if (convertToString) return String.valueOf(rs.getInt(column)); else return new Integer(rs.getInt(column));
            case -4:
                if (convertToString) return rs.getBinaryStream(column).toString(); else return rs.getBinaryStream(column);
            case -1:
                return rs.getString(column);
            case 0:
                if (convertToString) return "NULL"; else return null;
            case 2:
                if (convertToString) return rs.getBigDecimal(column).toString(); else return rs.getBigDecimal(column);
            case 7:
                if (convertToString) return String.valueOf(rs.getFloat(column)); else return new Float(rs.getFloat(column));
            case 5:
                if (convertToString) return String.valueOf(rs.getShort(column)); else return new Short(rs.getShort(column));
            case 92:
                if (convertToString) return rs.getTime(column).toString(); else return rs.getTime(column);
            case 93:
                if (convertToString) return rs.getTimestamp(column).toString(); else return rs.getTimestamp(column);
            case -6:
                if (convertToString) return String.valueOf(rs.getByte(column)); else return new Byte(rs.getByte(column));
            case -3:
                if (convertToString) return rs.getBytes(column).toString(); else return rs.getBytes(column);
            case 12:
                return rs.getString(column);
        }
        if (convertToString) return rs.getObject(column).toString(); else return rs.getObject(column);
    }

    public Object getField(int column) throws SQLException {
        return getField(column, false);
    }

    public Object getField(String fieldName) throws SQLException {
        return getField(rs.findColumn(fieldName), false);
    }

    public String getFieldString(int column) {
        try {
            if (getField(column, true) == null) return ""; else return (String) getField(column, true);
        } catch (Exception e) {
            return "";
        }
    }

    public String getFieldString(String fieldName) throws SQLException {
        return (String) getField(rs.findColumn(fieldName), true);
    }

    public String getFieldFmtstr(int column) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null.");
        switch(rsmd.getColumnType(column)) {
            case -5:
                return String.valueOf(rs.getLong(column));
            case -2:
                return Byte.toString(rs.getByte(column));
            case -7:
                return String.valueOf(rs.getBoolean(column));
            case 1:
                return (new StringBuilder("'")).append(rs.getString(column)).append("'").toString();
            case 91:
                return (new StringBuilder("'")).append(rs.getDate(column).toString()).append("'").toString();
            case 3:
                return rs.getBigDecimal(column).toString();
            case 8:
                return String.valueOf(rs.getDouble(column));
            case 6:
                return String.valueOf(rs.getDouble(column));
            case 4:
                return String.valueOf(rs.getInt(column));
            case -4:
                return rs.getBinaryStream(column).toString();
            case -1:
                return (new StringBuilder("'")).append(rs.getString(column)).append("'").toString();
            case 0:
                return "";
            case 2:
                return rs.getBigDecimal(column).toString();
            case 7:
                return String.valueOf(rs.getFloat(column));
            case 5:
                return String.valueOf(rs.getShort(column));
            case 92:
                return (new StringBuilder("'")).append(rs.getTime(column).toString()).append("'").toString();
            case 93:
                return rs.getTimestamp(column).toString();
            case -6:
                return String.valueOf(rs.getByte(column));
            case -3:
                return rs.getBytes(column).toString();
            case 12:
                return (new StringBuilder("'")).append(rs.getString(column)).append("'").toString();
        }
        return rs.getObject(column).toString();
    }

    public boolean isMustFmt(int column) throws SQLException {
        if (rs == null || rsmd == null) throw new SQLException("ResultSet is null.");
        switch(rsmd.getColumnType(column)) {
            case -5:
                return false;
            case -2:
                return false;
            case -7:
                return false;
            case 1:
                return true;
            case 91:
                return true;
            case 3:
                return false;
            case 8:
                return false;
            case 6:
                return false;
            case 4:
                return false;
            case -4:
                return false;
            case -1:
                return true;
            case 0:
                return false;
            case 2:
                return false;
            case 7:
                return false;
            case 5:
                return false;
            case 92:
                return true;
            case 93:
                return false;
            case -6:
                return false;
            case -3:
                return false;
            case 12:
                return true;
        }
        return false;
    }

    public boolean nextRow() throws SQLException {
        if (rs == null) throw new SQLException("ResultSet is null."); else return rs.next();
    }

    public boolean firstRow() throws SQLException {
        if (rs == null) throw new SQLException("ResultSet is null."); else return rs.first();
    }

    public ArrayList findAll() {
        return findAll(SQLstr);
    }

    public ArrayList findAll(String sql) {
        ArrayList list = new ArrayList();
        try {
            executeQuery(sql);
            int numCols = rsmd.getColumnCount();
            String tempValue = "";
            HashMap hm;
            for (; rs.next(); list.add(hm)) {
                hm = new HashMap();
                for (int i = 1; i <= numCols; i++) {
                    tempValue = getFieldString(i);
                    if (tempValue == null) tempValue = "";
                    logger.debug((new StringBuilder(String.valueOf(rsmd.getColumnLabel(i).toLowerCase()))).append("=").toString());
                    logger.debug(tempValue);
                    hm.put(rsmd.getColumnLabel(i).toLowerCase(), tempValue);
                }
            }
        } catch (Exception e) {
            System.out.println((new StringBuilder()).append(e).append("\n\n=======the sql is:\n").append(sql).toString());
        }
        if (rs != null) try {
            rs.close();
            rs = null;
        } catch (Throwable ex) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷resultSet时锟斤拷?");
        }
        if (stmt != null) try {
            stmt.close();
            stmt = null;
        } catch (Throwable ex) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷Statement时锟斤拷?");
        }
        if (conn != null) try {
            if (!conn.isClosed()) conn.close();
            conn = null;
        } catch (Throwable ex1) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷l锟斤拷时锟斤拷?");
        }
        if (rs != null) try {
            rs.close();
            rs = null;
        } catch (Throwable ex) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷resultSet时锟斤拷?");
        }
        if (stmt != null) try {
            stmt.close();
            stmt = null;
        } catch (Throwable ex) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷Statement时锟斤拷?");
        }
        if (conn != null) try {
            if (!conn.isClosed()) conn.close();
            conn = null;
        } catch (Throwable ex1) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷l锟斤拷时锟斤拷?");
        }
        return list;
    }

    public ArrayList findAll_page(int pageno, int pagesize) {
        return findAll_page(SQLstr, pageno, pagesize);
    }

    public ArrayList findAll_page(String sql, int pageno, int pagesize) {
        ArrayList list = new ArrayList();
        try {
            executeQuery(sql);
            int numCols = rsmd.getColumnCount();
            String tempValue = "";
            for (int j = 1; j <= pagesize; j++) if (rs.absolute((pageno - 1) * pagesize + j)) {
                HashMap hm = new HashMap();
                for (int i = 1; i <= numCols; i++) {
                    tempValue = getFieldString(i);
                    if (tempValue == null) tempValue = "";
                    hm.put(rsmd.getColumnLabel(i).toLowerCase(), tempValue);
                }
                list.add(hm);
            }
        } catch (Exception e) {
            logger.debug((new StringBuilder()).append(e).append("\n\n=======the sql is:\n").append(sql).toString());
        }
        if (rs != null) try {
            rs.close();
            rs = null;
        } catch (Throwable ex) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷resultSet时锟斤拷?");
        }
        if (stmt != null) try {
            stmt.close();
            stmt = null;
        } catch (Throwable ex) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷Statement时锟斤拷?");
        }
        if (rs != null) try {
            rs.close();
            rs = null;
        } catch (Throwable ex) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷resultSet时锟斤拷?");
        }
        if (stmt != null) try {
            stmt.close();
            stmt = null;
        } catch (Throwable ex) {
            logger.debug("beanHome::find(String,String)锟斤拷锟截憋拷Statement时锟斤拷?");
        }
        return list;
    }

    public boolean lastRow() throws SQLException {
        if (rs == null) throw new SQLException("ResultSet is null."); else return rs.last();
    }

    public void setConnection(Connection conn1) {
        conn = conn1;
    }

    public void returnConnToPool() {
        try {
            conn.close();
        } catch (Exception exception) {
        }
    }

    public static final String BIT = "boolean";

    public static final String TINYINT = "short";

    public static final String SMALLINT = "short";

    public static final String INTEGER = "int";

    public static final String BIGINT = "long";

    public static final String LONG = "long";

    public static final String FLOAT = "double";

    public static final String REAL = "float";

    public static final String DOUBLE = "double";

    public static final String NUMERIC = "java.math.BigDecimal";

    public static final String DECIMAL = "java.math.BigDecimal";

    public static final String CHAR = "String";

    public static final String VARCHAR = "String";

    public static final String LONGVARCHAR = "String";

    public static final String DATE = "java.util.Date";

    public static final String TIME = "java.util.Date";

    public static final String TIMESTAMP = "java.util.Date";

    public static final String CLOB = "java.sql.Clob";

    public static final String BLOB = "java.sql.Blob";

    public Connection conn;

    public SqlConnParms connparm;

    public static ConnectionManager cm = new ConnectionManager();

    public String friendlyColumnNames;

    private String columnNames[];

    public ResultSet rs;

    private ResultSetMetaData rsmd;

    private Statement stmt;

    public String QryTitle;

    public String page;

    public String row;

    public int intpagesize;

    public int record_count;

    public int field_count;

    public String SQLstr;

    public String tablename;

    public DataSource ds;
}
