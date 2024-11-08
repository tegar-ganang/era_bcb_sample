package mn.more.wits.server.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import mn.more.foundation.bean.BeanUtil;
import mn.more.wits.server.exception.ConfigurationException;
import org.apache.log4j.Logger;

/**
 * @author $Author: mikeliucc $
 * @version $Id: AbstractDAO.java 5 2008-09-01 12:08:42Z mikeliucc $
 */
public abstract class AbstractDAO {

    public static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final SimpleDateFormat DT_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    protected static final Logger LOG = Logger.getLogger(AbstractDAO.class);

    protected DataSource dataSource;

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected boolean update(String sql, int requiredRows) throws SQLException {
        return update(sql, requiredRows, -1);
    }

    protected boolean update(String sql, int requiredRows, int maxRows) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("executing " + sql + "...");
        }
        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = dataSource.getConnection();
            connection.clearWarnings();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            int rowsAffected = statement.executeUpdate(sql);
            if (requiredRows != -1 && rowsAffected < requiredRows) {
                LOG.warn("(" + rowsAffected + ") less than " + requiredRows + " rows affected, rolling back...");
                connection.rollback();
                return false;
            }
            if (maxRows != -1 && rowsAffected > maxRows) {
                LOG.warn("(" + rowsAffected + ") more than " + maxRows + " rows affected, rolling back...");
                connection.rollback();
                return false;
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            LOG.error("Unable to update database using: " + sql, e);
            throw e;
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(oldAutoCommit);
                    connection.close();
                }
            } catch (SQLException e) {
                LOG.error("Unable to close connection: " + e, e);
            }
        }
    }

    protected List retrieveList(String sql, Class beanClass) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("querying " + sql + "...");
        }
        List<Object> list = new ArrayList<Object>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.clearWarnings();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            ResultSetMetaData metadata = rs.getMetaData();
            while (rs.next()) {
                try {
                    Object bean = beanClass.newInstance();
                    for (int i = 0; i < metadata.getColumnCount(); i++) {
                        String propertyName = metadata.getColumnLabel(i + 1);
                        try {
                            BeanUtil.setProperty(bean, propertyName, rs.getObject(i + 1));
                        } catch (Throwable e) {
                            LOG.warn("Unable to  set property '" + propertyName + "' on bean " + bean + ": " + e);
                        }
                    }
                    list.add(bean);
                } catch (Throwable e) {
                    LOG.fatal("Unable to instantiate from class " + beanClass.getName(), e);
                    throw new ConfigurationException("Unable to instantiate from class " + beanClass.getName(), e);
                }
            }
            return list;
        } catch (SQLException e) {
            LOG.error("Unable to retrive data via: " + sql, e);
            throw e;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOG.error("Unable to close connection: " + e, e);
            }
        }
    }

    protected List<List<Object>> retrieveAsList(String sql) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("querying " + sql + "...");
        }
        List<List<Object>> list = new ArrayList<List<Object>>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.clearWarnings();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            ResultSetMetaData metadata = rs.getMetaData();
            while (rs.next()) {
                List<Object> oneRow = new ArrayList<Object>();
                for (int i = 0; i < metadata.getColumnCount(); i++) {
                    oneRow.add(rs.getObject(i + 1));
                }
                list.add(oneRow);
            }
            return list;
        } catch (SQLException e) {
            LOG.error("Unable to retrive data via: " + sql, e);
            throw e;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOG.error("Unable to close connection: " + e, e);
            }
        }
    }

    protected Object retrieveBean(String sql, Object bean) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("querying " + sql + "...");
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (!rs.next()) {
                LOG.warn("No row returned, thus a empty bean is returned.");
                return bean;
            } else {
                ResultSetMetaData metadata = rs.getMetaData();
                for (int i = 0; i < metadata.getColumnCount(); i++) {
                    String propertyName = metadata.getColumnLabel(i + 1);
                    try {
                        BeanUtil.setProperty(bean, propertyName, rs.getObject(i + 1));
                    } catch (Throwable e) {
                        LOG.warn("Unable to  set property '" + propertyName + "' on bean " + bean + ": " + e);
                    }
                }
                if (rs.next()) {
                    LOG.warn("One row expected, but more than one row returned from sql: " + sql);
                }
                return bean;
            }
        } catch (SQLException e) {
            LOG.error("Error while calling retrieve(): " + e, e);
            throw e;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOG.error("Unable to close connection: " + e, e);
            }
        }
    }

    protected Object retrieveOneValue(String sql, String columnName) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("querying " + sql + "...");
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (!rs.next()) {
                return null;
            } else {
                return rs.getObject(columnName);
            }
        } catch (SQLException e) {
            LOG.error("Error while calling retrieve(): " + e, e);
            throw e;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOG.error("Unable to close connection: " + e, e);
            }
        }
    }

    protected Object retrieveOneValue(String sql, int columnIndex) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("querying " + sql + "...");
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (!rs.next()) {
                return null;
            } else {
                return rs.getObject(columnIndex);
            }
        } catch (SQLException e) {
            LOG.error("Error while calling retrieve(): " + e, e);
            throw e;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOG.error("Unable to close connection: " + e, e);
            }
        }
    }
}
