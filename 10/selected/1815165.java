package org.mc.app;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.StackKeyedObjectPoolFactory;
import org.mc.Const;
import org.eg.Utils;
import org.mc.catalog.Product;
import org.mc.catalog.Vendor;
import org.mc.content.Retailer;
import org.mc.db.DB;
import org.mc.admin.UserViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContextListener implements ServletContextListener {

    private final Logger log = LoggerFactory.getLogger(ContextListener.class);

    private static DB.Col col = new DB.Col();

    private int getRootNodeId(DataSource dataSource) throws SQLException {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        String query = null;
        try {
            conn = dataSource.getConnection();
            st = conn.createStatement();
            query = "select " + col.id + " from " + DB.Tbl.tree + " where " + col.parentId + " is null";
            rs = st.executeQuery(query);
            while (rs.next()) {
                return rs.getInt(col.id);
            }
            query = "insert into " + DB.Tbl.tree + "(" + col.lKey + ", " + col.rKey + ", " + col.level + ") values(1,2,0)";
            st.executeUpdate(query, new String[] { col.id });
            rs = st.getGeneratedKeys();
            while (rs.next()) {
                int genId = rs.getInt(1);
                rs.close();
                conn.commit();
                return genId;
            }
            throw new SQLException("Не удается создать корневой элемент для дерева.");
        } finally {
            try {
                rs.close();
            } catch (Exception e) {
            }
            try {
                st.close();
            } catch (Exception e) {
            }
            try {
                conn.rollback();
            } catch (Exception e) {
            }
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
    }

    private DataSource setUpDataSourse(ServletContext context) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String dbDriver = Utils.getFromConfig(context, "dbDriver", null);
        String url = Utils.getFromConfig(context, "dbConnString", null);
        String user = Utils.getFromConfig(context, "dbUser", null);
        String pass = Utils.getFromConfig(context, "dbPass", null);
        Class.forName(dbDriver).newInstance();
        GenericObjectPool.Config config = new GenericObjectPool.Config();
        config.timeBetweenEvictionRunsMillis = 60000;
        config.testWhileIdle = true;
        GenericObjectPool connectionPool = new GenericObjectPool(null, config);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, user, pass);
        boolean autoCommit = false;
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, new StackKeyedObjectPoolFactory(), DB.Pool.validationQuery, false, autoCommit);
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        return dataSource;
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext context = servletContextEvent.getServletContext();
        DataSource dataSource = null;
        try {
            dataSource = setUpDataSourse(context);
        } catch (Exception e) {
            log.error("Ошибка при создании источника данных.", e);
            return;
        }
        int rootNodeId = -1;
        try {
            rootNodeId = getRootNodeId(dataSource);
        } catch (Exception e) {
            log.error("Ошибка при получении rootNodeId.", e);
            return;
        }
        App.init(dataSource, context, rootNodeId);
        long appCleanPeriod = Utils.getFromConfig(context, "app-clean-period", 120);
        appCleanPeriod = appCleanPeriod * 1000;
        VendorViewer.init(appCleanPeriod);
        RetailerViewer.init(appCleanPeriod);
        context.setAttribute(Const.VENDOR_VIEWER_BEAN, VendorViewer.getInstance());
        context.setAttribute(Const.RETAILER_VIEWER_BEAN, RetailerViewer.getInstance());
        context.setAttribute(Const.USER_VIEWER_BEAN, UserViewer.getInstance());
        context.setAttribute(Const.APP_BEAN, App.getInstance());
        Inittable.init();
        Product.init();
        Vendor.init();
        Retailer.init();
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
