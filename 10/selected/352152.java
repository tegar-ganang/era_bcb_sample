package com.aide.simplification.global.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DataBase {

    private static final Log log = LogFactory.getLog(DataBase.class);

    private boolean debug = true;

    private String databasetype = "mssql";

    private String poolname = "sorc";

    public DataBase(String poolname) {
        this.poolname = poolname;
    }

    public DataBase() {
    }

    /**
	 * 获得数据库连接
	 * 
	 * @return 数据库连接
	 */
    public Connection getConnection() {
        try {
            Class.forName("org.logicalcobwebs.proxool.ProxoolDriver");
            return DriverManager.getConnection("proxool." + poolname);
        } catch (Exception e) {
            log.error("连接数据库失败！", e);
            System.out.println("连接数据库失败！");
            return null;
        }
    }

    public int executeUpdateJT(String sqlList[], Object[][] paramsList) {
        Connection connection = null;
        connection = this.getConnection();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        PreparedStatement preparedStatement = null;
        try {
            for (int i = 0; i < sqlList.length; i++) {
                System.out.println(sqlList[i]);
                if (connection != null && !connection.isClosed()) {
                    preparedStatement = connection.prepareStatement(sqlList[i]);
                    InputStream is = null;
                    int size = paramsList[i].length;
                    int curr = 0;
                    if (paramsList[i].length > 0) {
                        for (int j = 0; j < size; j++) {
                            Object obj = paramsList[i][j];
                            if (obj != null) {
                                curr++;
                                if (obj.getClass().equals(Class.forName("java.io.File"))) {
                                    File file = (File) obj;
                                    is = new FileInputStream(file);
                                    preparedStatement.setBinaryStream(curr, is, (int) file.length());
                                } else if (obj.getClass().equals(Class.forName("java.util.Date"))) {
                                    java.text.SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    preparedStatement.setString(curr, sdf.format((Date) obj));
                                } else {
                                    preparedStatement.setObject(curr, obj);
                                }
                            }
                        }
                    }
                    preparedStatement.executeUpdate();
                    if (is != null) {
                        is.close();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("发生错误，数据回滚！");
            e.printStackTrace();
            try {
                connection.rollback();
                return 0;
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        try {
            connection.commit();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
                e.printStackTrace();
            }
        }
        return -1;
    }

    public int executeUpdateJT(String sql, Object[][] paramsList) {
        Connection connection = null;
        connection = this.getConnection();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            for (int i = 0; i < paramsList.length; i++) {
                if (connection != null && !connection.isClosed()) {
                    InputStream is = null;
                    if (paramsList[i].length > 0) {
                        for (int j = 0; j < paramsList[i].length; j++) {
                            Object obj = paramsList[i][j];
                            if (obj.getClass().equals(Class.forName("java.io.File"))) {
                                File file = (File) obj;
                                is = new FileInputStream(file);
                                preparedStatement.setBinaryStream(j + 1, is, (int) file.length());
                            } else if (obj.getClass().equals(Class.forName("java.util.Date"))) {
                                java.text.SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                preparedStatement.setString(j + 1, sdf.format((Date) obj));
                            } else {
                                preparedStatement.setObject(j + 1, obj);
                            }
                        }
                    }
                    preparedStatement.executeUpdate();
                    if (is != null) {
                        is.close();
                    }
                    ;
                }
            }
        } catch (Exception e) {
            System.out.println("发生错误，数据回滚！");
            e.printStackTrace();
            try {
                connection.rollback();
                return 0;
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        try {
            connection.commit();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                preparedStatement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
	 * 执行一条sql语句（更新，删除）
	 * 
	 * @param sql
	 *            要执行的sql语句
	 * @param params
	 *            参数列表
	 * @return 影响行数
	 */
    public int executeUpdate(String sql, Object[] params) {
        if (debug) {
            System.out.println(sql);
        }
        Connection connection = null;
        connection = this.getConnection();
        int temp = -1;
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                InputStream is = null;
                if (params.length > 0) {
                    for (int i = 0; i < params.length; i++) {
                        Object obj = params[i];
                        if (obj.getClass().equals(Class.forName("java.io.File"))) {
                            File file = (File) obj;
                            is = new FileInputStream(file);
                            preparedStatement.setBinaryStream(i + 1, is, (int) file.length());
                        } else {
                            preparedStatement.setObject(i + 1, obj);
                        }
                    }
                }
                temp = preparedStatement.executeUpdate();
                if (is != null) {
                    is.close();
                }
                preparedStatement.close();
                return temp;
            } else {
                log.error("空连接或关闭的连接！");
                System.out.println("空连接或关闭的连接！");
                return temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("执行sql语句错误！", e);
            System.out.println(this.inEncodeing(sql));
            System.out.println("执行sql语句错误！");
            return temp;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("关闭连接失败！", e);
                System.out.println("关闭连接失败！");
            }
        }
    }

    /**
	 * 执行一条sql语句（查询）
	 * 
	 * @param sql
	 *            要执行的sql语句
	 * @return 结果集
	 */
    public List<Map<String, Object>> queryForList(String sql, Object[] params) {
        if (this.debug) {
            System.out.println(sql);
        }
        Connection connection = null;
        connection = this.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (connection != null && !connection.isClosed()) {
                ps = connection.prepareStatement(sql);
                if (params.length > 0) {
                    for (int i = 0; i < params.length; i++) {
                        String param = params[i].toString();
                        ps.setObject(i + 1, param);
                    }
                }
                rs = ps.executeQuery();
                return this.resultSetToMapList(rs);
            }
        } catch (SQLException e) {
            log.error("执行查询语句错误！", e);
            System.out.println(sql);
            System.out.println("执行查询语句错误！");
        } finally {
            try {
                ps.close();
                connection.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
            }
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
                e.printStackTrace();
            }
        }
        return null;
    }

    public Map<String, Object> queryForMap(String sql, Object[] params) {
        if (this.debug) {
            System.out.println(sql);
        }
        List<Map<String, Object>> list = this.queryForList(sql, params);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public int queryForInt(String sql, Object[] params) {
        if (this.debug) {
            System.out.println(sql);
        }
        Connection connection = null;
        connection = this.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            int count = -1;
            if (connection != null && !connection.isClosed()) {
                ps = connection.prepareStatement(sql);
                if (params.length > 0) {
                    for (int i = 0; i < params.length; i++) {
                        String param = params[i].toString();
                        ps.setObject(i + 1, param);
                    }
                }
                rs = ps.executeQuery();
                while (rs.next()) {
                    count = rs.getInt(1);
                    rs.close();
                    break;
                }
            }
            return count;
        } catch (SQLException e) {
            log.error("执行查询语句错误！", e);
            System.out.println(sql);
            System.out.println("执行查询语句错误！");
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
            }
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
                e.printStackTrace();
            }
        }
        return -1;
    }

    public String queryForString(String sql, Object[] params) {
        if (this.debug) {
            System.out.println(sql);
        }
        Connection connection = null;
        connection = this.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String str = null;
            if (connection != null && !connection.isClosed()) {
                ps = connection.prepareStatement(sql);
                if (params.length > 0) {
                    for (int i = 0; i < params.length; i++) {
                        String param = params[i].toString();
                        ps.setObject(i + 1, param);
                    }
                }
                rs = ps.executeQuery();
                while (rs.next()) {
                    str = rs.getString(1);
                    rs.close();
                    break;
                }
            }
            return str;
        } catch (SQLException e) {
            log.error("执行查询语句错误！", e);
            System.out.println(sql);
            System.out.println("执行查询语句错误！");
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
            }
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
	 * 批量执行sql语句（增删改）
	 * 
	 * @param list
	 *            sql集
	 * @param num
	 *            批量执行条数
	 * @return
	 * @throws SQLException
	 */
    public boolean Batch(List<Map<String, Object>> list, int num) throws SQLException {
        Connection connection = null;
        try {
            connection = this.getConnection();
            if (!connection.isClosed()) {
                Statement stm = connection.createStatement();
                for (int i = 1; i < list.size() + 1; i++) {
                    stm.addBatch(this.inEncodeing(list.get(i - 1).toString()));
                    if (i % num == 0) {
                        stm.executeBatch();
                    }
                }
                stm.executeBatch();
                stm.close();
            }
            return true;
        } catch (Exception e) {
            log.error("批量执行sql语句错误！", e);
            System.out.println(list);
            System.out.println("批量执行sql语句错误！");
            return false;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
            }
        }
    }

    public String initPageSql(int pagenum, int pagesize, String indexKey, String sql) {
        String sqltemp = "";
        if (databasetype.equals("mssql")) {
            if (pagenum == 1) {
                sqltemp = "select top " + pagesize + " * from (" + sql + ") as t1 ";
            } else {
                sqltemp = "select top " + pagesize + " * from (" + sql + ") as t1 " + " where (t1." + indexKey + " > (select max(t3." + indexKey + ")" + " from (select top " + (pagenum - 1) * pagesize + " t2." + indexKey + " from (" + sql + ") as t2 order by t2." + indexKey + ") as t3" + " )) order by t1." + indexKey;
            }
        } else if (databasetype.equals("mysql")) {
            sqltemp = sql + " limit " + (pagenum - 1) * pagesize + "," + pagesize;
        }
        return sqltemp;
    }

    public String getPageSql(int pagenum, int pagesize, String keys, String sql) {
        String sqltemp = sql;
        sqltemp = sqltemp.substring(sqltemp.indexOf(" from ") + 6);
        if (sqltemp.indexOf(" where ") != -1) {
        } else if (sqltemp.indexOf(" order by ") != -1) {
        } else {
        }
        return sql;
    }

    public String getPageSql(String tablename, String showcoll, String where, String keys, int page, int pagesize) {
        return this.getPageSqlBase(tablename, showcoll, where, keys, "", page, pagesize);
    }

    public String getPageSql(String tablename, String where, String keys, int page, int pagesize) {
        return this.getPageSqlBase(tablename, "", where, keys, "", page, pagesize);
    }

    public String getPageSql(String tablename, String keys, int page, int pagesize) {
        return this.getPageSqlBase(tablename, "", "", keys, "", page, pagesize);
    }

    public String getPageSql(Map<String, String> map, int page, int pagesize) {
        String tablename = "";
        if (map.get("tablename") != null) {
            tablename = map.get("tablename");
        }
        String keys = "";
        if (map.get("keys") != null) {
            tablename = map.get("keys");
        }
        String where = "";
        if (map.get("where") != null) {
            where = map.get("where");
        }
        String showcoll = "";
        if (map.get("showcoll") != null) {
            showcoll = map.get("showcoll");
        }
        String order = "";
        if (map.get("order") != null) {
            order = map.get("order");
        }
        return this.getPageSqlBase(tablename, showcoll, where, keys, order, page, pagesize);
    }

    public String getPageSqlBase(String tablename, String showcoll, String where, String keys, String order, int page, int pagesize) {
        StringBuffer sqltemp = new StringBuffer();
        if (databasetype.equals("mssql")) {
            if (page == 1) {
                sqltemp.append("select top " + pagesize + " ");
                if (showcoll.equals("")) {
                    sqltemp.append("*");
                } else {
                    sqltemp.append(showcoll);
                }
                sqltemp.append(" from " + tablename + " ");
                if (!where.equals("")) {
                    sqltemp.append(" where " + where);
                }
                sqltemp.append(" order by " + keys + order);
                return sqltemp.toString();
            } else {
                sqltemp.append("select top " + pagesize + " ");
                if (showcoll.equals("")) {
                    sqltemp.append("*");
                } else {
                    sqltemp.append(showcoll);
                }
                sqltemp.append(" from " + tablename + " ");
                sqltemp.append(" where " + keys + ">(select max (" + keys + ") from (select top " + (page - 1) * pagesize + " " + keys + " from " + tablename + " ");
                if (!where.equals("")) {
                    sqltemp.append(" where " + where);
                }
                sqltemp.append(" order by " + keys + order);
                sqltemp.append(") as sorc)");
                if (!where.equals("")) {
                    sqltemp.append(" and " + where);
                }
                sqltemp.append(" order by " + keys);
            }
        } else if (databasetype.equals("mysql")) {
            sqltemp.append("select ");
            if (showcoll.equals("")) {
                sqltemp.append("*");
            } else {
                sqltemp.append(showcoll);
            }
            sqltemp.append(" from " + tablename + " ");
            if (!where.equals("")) {
                sqltemp.append(where);
            }
            sqltemp.append(" order by " + keys + order);
            sqltemp.append(" limit ");
            sqltemp.append((page - 1) * pagesize);
            sqltemp.append(",");
            sqltemp.append(pagesize);
        }
        return sqltemp.toString();
    }

    /**
	 * 执行存储过程（查询）
	 * 
	 * @param sql
	 *            要执行的sql语句
	 * @return 结果集
	 */
    public List<Map<String, Object>> prepareCallForList(String sql, Object obj[]) {
        if (debug) System.out.println(sql);
        Connection connection = null;
        connection = this.getConnection();
        CallableStatement cstmt = null;
        try {
            if (connection != null && !connection.isClosed()) {
                cstmt = connection.prepareCall(sql);
                for (int i = 0; i < obj.length; i++) {
                    cstmt.setString(i + 1, obj[i].toString());
                }
                cstmt.executeQuery();
                if (cstmt.getMoreResults()) {
                    return this.resultSetToMapList(cstmt.getResultSet());
                }
                return this.resultSetToMapList(cstmt.getResultSet());
            }
        } catch (SQLException e) {
            log.error("执行查询语句错误！", e);
            System.out.println(sql);
            System.out.println("执行查询语句错误！");
        } finally {
            try {
                cstmt.close();
                connection.close();
            } catch (SQLException e) {
                log.error("未能正确关闭数据库连接！", e);
                System.out.println("未能正确关闭数据库连接！");
            }
        }
        return null;
    }

    /**
	 * 入库转码 在用
	 * 
	 * @param str
	 * @return
	 */
    public String inEncodeing(String str) {
        return str;
    }

    /**
	 * 出库转码 在用
	 * 
	 * @param str
	 * @return
	 */
    public String outEncodeing(String str) {
        return str;
    }

    /**
	 * 将结果集转为List<Map<String, Object>>
	 * 
	 * @param rs
	 *            结果集
	 * @return 存放结果Bean的List
	 * @throws SQLException
	 */
    protected List<Map<String, Object>> resultSetToMapList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (rs != null && rs.getMetaData() != null) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();
            while (rs.next()) {
                Map<String, Object> hm = new HashMap<String, Object>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = rsmd.getColumnName(i).toLowerCase();
                    int colType = rsmd.getColumnType(i);
                    switch(colType) {
                        case Types.VARCHAR:
                            String colVal = rs.getString(colName);
                            if (colVal != null) {
                                hm.put(colName, outEncodeing(colVal));
                            } else {
                                hm.put(colName, "");
                            }
                            break;
                        case Types.INTEGER:
                            hm.put(colName, rs.getInt(i));
                            break;
                        case Types.DATE:
                            Date date = rs.getDate(i);
                            SimpleDateFormat sdfdate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            hm.put(colName, sdfdate.format(date));
                            break;
                        case Types.TIME:
                            Time time = rs.getTime(i);
                            SimpleDateFormat sdftime = new SimpleDateFormat("hh:mm:ss");
                            hm.put(colName, sdftime.format(time));
                            break;
                        case Types.TIMESTAMP:
                            Timestamp timestamp = rs.getTimestamp(i);
                            SimpleDateFormat sdfstamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            hm.put(colName, sdfstamp.format(timestamp));
                            break;
                        case Types.DECIMAL:
                            hm.put(colName, rs.getBigDecimal(i));
                            break;
                        case Types.DOUBLE:
                            hm.put(colName, rs.getDouble(i));
                            break;
                        case Types.BINARY:
                            hm.put(colName, rs.getBinaryStream(i));
                            break;
                        default:
                            hm.put(colName, rs.getObject(i));
                            break;
                    }
                }
                list.add(hm);
            }
        }
        return list;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public String getDatabasetype() {
        return this.databasetype;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    protected void setDatabasetype(String databasetype) {
        this.databasetype = databasetype;
    }

    public String getPoolname() {
        return poolname;
    }

    protected void setPoolname(String poolname) {
        this.poolname = poolname;
    }
}
