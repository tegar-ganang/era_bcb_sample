package com.javaeye.lonlysky.lforum.service;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.javaeye.lonlysky.lforum.GlobalsKeys;
import com.javaeye.lonlysky.lforum.comm.MD5;
import com.javaeye.lonlysky.lforum.comm.utils.PropertiesUtil;
import com.javaeye.lonlysky.lforum.config.impl.ConfigLoader;
import com.javaeye.lonlysky.lforum.entity.global.Config;
import com.javaeye.lonlysky.lforum.exception.ServiceException;

/**
 * 系统安装向导业务
 * 
 * @author 黄磊
 * 
 */
@Service
public class InstallManager {

    private static final Logger logger = LoggerFactory.getLogger(InstallManager.class);

    /**
	 * 获取数据库连接
	 * 
	 * @param webPath
	 *            系统路径
	 * @param dataType
	 *            数据类型
	 * @param dataSource
	 *            数据源
	 * @param dataPort
	 *            数据端口
	 * @param dataBase
	 *            数据库名称
	 * @param db_user
	 *            数据库用户
	 * @param db_pwd
	 *            数据库密码
	 * @return
	 */
    public Connection getConnection(String webPath, String dataType, String dataSource, String dataPort, String dataBase, String db_user, String db_pwd) {
        String db_driver = "", db_url = "";
        try {
            String dbConfig = webPath + "WEB-INF/database/" + dataType + "/dbconfig.properties";
            db_driver = PropertiesUtil.getStringValue(dbConfig, GlobalsKeys.DATABASE_DRIVER);
            db_url = PropertiesUtil.getStringValue(dbConfig, GlobalsKeys.DATABASE_URL);
            db_url = db_url.replaceFirst("DB_HOST", dataSource);
            db_url = db_url.replaceFirst("DB_PORT", dataPort);
            db_url = db_url.replaceFirst("DB_NAME", dataBase);
            if (logger.isDebugEnabled()) {
                logger.debug("当前驱动：" + db_driver + ",连接字符串：" + db_url + ",用户名：" + db_user + ",密码：" + db_pwd);
            }
            Class.forName(db_driver);
            Connection connection = DriverManager.getConnection(db_url, db_user, db_pwd);
            String db_dialect = PropertiesUtil.getStringValue(dbConfig, GlobalsKeys.DATABASE_DIALECT);
            String appxml = webPath + "WEB-INF/config/applicationContext.xml";
            String configStr = FileUtils.readFileToString(new File(appxml), "UTF-8");
            configStr = configStr.replaceFirst("DB_DRIVER", db_driver);
            configStr = configStr.replaceFirst("DB_URL", db_url);
            configStr = configStr.replaceFirst("DB_USER", db_user);
            configStr = configStr.replaceFirst("DB_PWD", db_pwd);
            configStr = configStr.replaceFirst("DB_DIALECT", db_dialect);
            String newFile = webPath + "WEB-INF/classes/applicationContext.xml";
            FileUtils.writeStringToFile(new File(newFile), configStr, "UTF-8");
            FileUtils.copyFile(new File(webPath + "WEB-INF/config/web.xml"), new File(webPath + "WEB-INF/web.xml"));
            FileUtils.copyFile(new File(webPath + "WEB-INF/config/index.html"), new File(webPath + "index.html"));
            return connection;
        } catch (ClassNotFoundException e) {
            logger.error("加载驱动：" + db_driver + "异常", e);
            throw new ServiceException("找不到相应的连接驱动", e);
        } catch (SQLException e) {
            logger.error("连接数据库异常：" + db_url, e);
            throw new ServiceException("数据库连接失败," + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("不可能发生的错误", e);
            throw new ServiceException("不可能发生的错误," + e.getMessage(), e);
        }
    }

    /**
	 * 创建数据库表
	 * 
	 * @param sqlLst
	 *            SQL语句集合
	 * @param connection
	 *            连接对象
	 */
    public void createTable(List<String> sqlLst, Connection connection) {
        for (String sqlstr : sqlLst) {
            if (logger.isDebugEnabled()) {
                logger.debug("即将执行SQL语句：" + sqlstr);
            }
            if (sqlstr == null || "".equals(sqlstr.trim())) {
                continue;
            }
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute(sqlstr);
            } catch (SQLException e) {
                logger.error("安装向导创建表异常", e);
                throw new ServiceException("创建数据库表失败,请检查设置或重试", e);
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                } catch (SQLException e) {
                    throw new ServiceException("关闭连接载体失败", e);
                }
            }
        }
    }

    /**
	 * 插入系统初始管理员
	 * 
	 * @param name
	 *            用户名
	 * @param pwd
	 *            密码
	 * @param ip
	 *            IP地址
	 * 
	 * @param connection
	 */
    public void insertAdmin(String webPath, String dataType, String name, String pwd, String ip, Connection connection) {
        String dbConfig = webPath + "WEB-INF/database/" + dataType + "/dbconfig.properties";
        String sql = PropertiesUtil.getStringValue(dbConfig, GlobalsKeys.DATABASE_ADMIN);
        if (logger.isDebugEnabled()) {
            logger.debug("插入管理员SQL语句：" + sql + ",用户名：" + name + ",密码：" + pwd);
        }
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, name);
            ps.setString(3, MD5.encode(pwd));
            ps.setString(4, ip);
            ps.execute();
            sql = PropertiesUtil.getStringValue(dbConfig, GlobalsKeys.DATABASE_UFIELD);
            if (logger.isDebugEnabled()) {
                logger.debug("管理员扩展信息SQL语句：" + sql);
            }
            ps = connection.prepareStatement(sql);
            ps.execute();
        } catch (Exception e) {
            logger.error("初始化管理员异常", e);
            throw new ServiceException("初始化系统管理员失败：" + e.getMessage());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                throw new ServiceException("关闭连接载体失败");
            }
        }
    }

    /**
	 * 保存系统配置信息
	 * 
	 * @param config
	 *            系统配置
	 */
    public void saveConfig(Config config) {
        try {
            ConfigLoader loader = new ConfigLoader(config.getWebpath());
            loader.saveConfig(config);
        } catch (Exception e) {
            logger.error("保存系统配置信息异常", e);
            throw new ServiceException("保存系统配置信息失败," + e.getMessage());
        }
    }
}
