package net.shopxx.schedule;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

/**
 * 数据库操作管理类
 * 
 * @author 李飞飞
 * 
 */
public class DBManager {

    private static Logger logger = Logger.getLogger(DBManager.class);

    BoneCP connectionPool = null;

    private Connection con;

    private Statement stmt;

    private PreparedStatement pstmt;

    private ResultSet rs;

    /** ***********************手动设置的连接参数********************************* */
    private static String _DRIVER = "com.mysql.jdbc.Driver";

    private static String _URL = "jdbc:mysql://192.168.15.2:3307/spider?useUnicode=true&characterEncoding=UTF-8";

    private static String _USER_NA = "spider";

    private static String _PASSWORD = "123";

    public DBManager() {
    }

    /**
	 * 得到一个默认的数据库连接[从 com.hospital.dao.tools.db.properties文件初始化]
	 * 
	 * @throws SQLException
	 * 
	 * @throws Exception
	 */
    public void getConnection() {
        try {
            logger.info("###############open:::::从默认的配置文件得到一个数据库连接");
            BoneCPConfig config = new BoneCPConfig();
            config.setPoolName(_DRIVER);
            config.setJdbcUrl(_URL);
            config.setUsername(_USER_NA);
            config.setPassword(_PASSWORD);
            config.setMinConnectionsPerPartition(5);
            config.setMaxConnectionsPerPartition(10);
            config.setPartitionCount(1);
            connectionPool = new BoneCP(config);
            con = connectionPool.getConnection();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 执行SQL语句操作(更新数据 无参数)
	 * 
	 * @param strSql
	 *            SQL语句
	 * @throws Exception
	 */
    public boolean executeUpdate(String strSql) throws SQLException {
        getConnection();
        boolean flag = false;
        stmt = con.createStatement();
        logger.info("###############::执行SQL语句操作(更新数据 无参数):" + strSql);
        try {
            if (0 < stmt.executeUpdate(strSql)) {
                close_DB_Object();
                flag = true;
                con.commit();
            }
        } catch (SQLException ex) {
            logger.info("###############Error DBManager Line126::执行SQL语句操作(更新数据 无参数):" + strSql + "失败!");
            flag = false;
            con.rollback();
            throw ex;
        }
        return flag;
    }

    /**
	 * 执行SQL语句操作(更新数据 有参数)
	 * 
	 * @param strSql
	 *            sql指令
	 * @param prams
	 *            参数列表
	 * @return
	 * @throws SQLException
	 */
    public boolean executeUpdate(String strSql, HashMap<Integer, Object> prams) throws SQLException, ClassNotFoundException {
        getConnection();
        boolean flag = false;
        try {
            pstmt = con.prepareStatement(strSql);
            setParamet(pstmt, prams);
            logger.info("###############::执行SQL语句操作(更新数据 有参数):" + strSql);
            if (0 < pstmt.executeUpdate()) {
                close_DB_Object();
                flag = true;
                con.commit();
            }
        } catch (SQLException ex) {
            logger.info("###############Error DBManager Line121::执行SQL语句操作(更新数据 无参数):" + strSql + "失败!");
            flag = false;
            con.rollback();
            throw ex;
        } catch (ClassNotFoundException ex) {
            logger.info("###############Error DBManager Line152::执行SQL语句操作(更新数据 无参数):" + strSql + "失败! 参数设置类型错误!");
            con.rollback();
            throw ex;
        }
        return flag;
    }

    /**
	 * 执行SQL语句操作(查询数据 有参数)
	 * 
	 * @param strSql
	 *            SQL语句
	 * @param prams
	 *            参数列表
	 * @return 数组对象列表
	 * @throws Exception
	 */
    public PreparedStatement executeSql(String strSql) throws Exception {
        getConnection();
        pstmt = con.prepareStatement(strSql);
        return pstmt;
    }

    /**
	 * 关闭数据对象
	 */
    public void close_DB_Object() {
        logger.info("###############close:::::关闭连接对象，语句对象，记录集对象");
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException ex) {
                rs = null;
            }
        }
        if (null != stmt) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                stmt = null;
            }
        }
        if (null != pstmt) {
            try {
                pstmt.close();
            } catch (SQLException ex) {
                pstmt = null;
            }
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * 设置Sql 指令参数
	 * 
	 * @param p_stmt
	 *            PreparedStatement
	 * @param pramets
	 *            HashMap
	 */
    private PreparedStatement setParamet(PreparedStatement p_stmt, HashMap<Integer, Object> pramets) throws ClassNotFoundException, SQLException {
        if (null != pramets) {
            if (0 <= pramets.size()) {
                for (int i = 1; i <= pramets.size(); i++) {
                    try {
                        if (pramets.get(i).getClass() == Class.forName("java.lang.String")) {
                            p_stmt.setString(i, pramets.get(i).toString());
                        }
                        if (pramets.get(i).getClass() == Class.forName("java.sql.Date")) {
                            p_stmt.setDate(i, java.sql.Date.valueOf(pramets.get(i).toString()));
                        }
                        if (pramets.get(i).getClass() == Class.forName("java.lang.Boolean")) {
                            p_stmt.setBoolean(i, (Boolean) (pramets.get(i)));
                        }
                        if (pramets.get(i).getClass() == Class.forName("java.lang.Integer")) {
                            p_stmt.setInt(i, (Integer) pramets.get(i));
                        }
                        if (pramets.get(i).getClass() == Class.forName("java.lang.Float")) {
                            p_stmt.setFloat(i, (Float) pramets.get(i));
                        }
                        if (pramets.get(i).getClass() == Class.forName("java.lang.Double")) {
                            p_stmt.setDouble(i, (Double) pramets.get(i));
                        }
                    } catch (ClassNotFoundException ex) {
                        throw ex;
                    } catch (SQLException ex) {
                        throw ex;
                    }
                }
            }
        }
        return p_stmt;
    }
}
