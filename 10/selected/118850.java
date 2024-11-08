package org.paradise.dms.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.paradise.dms.pojo.Base;
import org.paradise.dms.pojo.BaseException;

/**
 * 删除一条（多条）记录，得到数据表名，得到下一条记录id（oracle）
 *
 */
public class BaseDaoImpl<T extends Base> {

    private String tableName;

    private Connection conn;

    private Logger log = Logger.getLogger(this.getClass());

    /**
	 * 连接数据库字符串
	 */
    protected String strConnection = "";

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setConnection(Connection conn) {
        this.conn = conn;
    }

    public Connection getConnection() {
        return conn;
    }

    /**
     * Description:保存对象T。
     *
     * @param  t 需要保存的对象
	 * @return 若保存成功，返回保存对象的主键;异常返回-1。
	 * @throws BaseException
     */
    public long add(T t) throws BaseException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        long result = -1L;
        boolean flag = false;
        try {
            conn = getConnection();
            if (conn != null) {
                flag = true;
            } else {
                conn = ConnectionManager.getConn(getStrConnection());
                conn.setAutoCommit(false);
            }
            pstmt = getAdd(conn, t, this.getTableName());
            pstmt.executeUpdate();
            result = t.getId();
        } catch (SQLException e) {
            try {
                if (!flag) {
                    conn.rollback();
                }
            } catch (Exception ex) {
                log.error("add(T " + t.toString() + ")回滚出错，错误信息：" + ex.getMessage());
            }
            log.error("add(T " + t.toString() + ")方法出错:" + e.getMessage());
        } catch (BaseException e) {
            throw e;
        } finally {
            try {
                if (!flag) {
                    conn.setAutoCommit(true);
                }
            } catch (Exception e) {
                log.error("add(T " + t.toString() + ")方法设置自动提交出错，信息为:" + e.getMessage());
            }
            ConnectionManager.closePreparedStatement(pstmt);
            if (!flag) {
                ConnectionManager.closeConn(conn);
            }
        }
        return result;
    }

    /**
	 * Description: 将rs转为相应java对象
	 * @param rs 
	 * @return 相应java对象
	 * @throws BaseException
	 */
    protected T dealRS(ResultSet rs) throws BaseException {
        return null;
    }

    /**
	 * Description: 将java对象写成处理新增的sql代码
	 * @param conn 数据连接
	 * @param t java对象
	 * @return PreparedStatement
	 * @throws BaseException
	 */
    protected PreparedStatement getAdd(Connection conn, T t, String tableName) throws BaseException {
        return null;
    }

    public String getStrConnection() {
        return strConnection;
    }

    public void setStrConnection(String strConnection) {
        this.strConnection = strConnection;
    }
}
