package org.utagger.sdk.impl.persistence;

import org.apache.log4j.Logger;
import org.utagger.sdk.impl.TagImpl;
import org.utagger.sdk.impl.TaggableImpl;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

/**
 * @author Ben Verbeken
 */
public abstract class AbstractJDBCDao implements Dao {

    protected Connection connection;

    protected ResultSet resultSet;

    protected PreparedStatement preparedStatement;

    protected String dbFolderPath;

    protected String dbConnectionString;

    protected ResourceBundle queries;

    private static final Logger LOG = Logger.getLogger(AbstractJDBCDao.class);

    @Override
    public final void openConnection() {
        try {
            connection = DriverManager.getConnection(dbConnectionString);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
        }
    }

    @Override
    public final void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
        }
    }

    @Override
    public final void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
        }
    }

    @Override
    public void closeConnection() {
        if (resultSet != null) {
            try {
                resultSet.close();
                resultSet = null;
            } catch (SQLException e) {
                LOG.error("Failed to close ResultSet", e);
            }
        }
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
                preparedStatement = null;
            } catch (SQLException e) {
                LOG.error("Failed to close PreparedStatement", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                LOG.error("Failed to close Connection", e);
            }
        }
    }

    protected TaggableImpl createTaggable(ResultSet resultSet) throws SQLException {
        try {
            return new TaggableImpl(resultSet.getInt(Constants.ID), new URL(resultSet.getString(Constants.URL)), resultSet.getString(Constants.MIMETYPE));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Illegal URL in the db", e);
        }
    }

    protected TagImpl createTagImpl(ResultSet resultSet) throws SQLException {
        return new TagImpl(resultSet.getInt(Constants.ID), resultSet.getString(Constants.NAME));
    }

    protected void createDb() {
        File rootFolder = new File(dbFolderPath);
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }
        openConnection();
        try {
            Statement stat = connection.createStatement();
            ResourceBundle bundle = ResourceBundle.getBundle("uTaggerDb");
            for (String key : bundle.keySet()) {
                stat.executeUpdate(bundle.getString(key));
            }
            commit();
        } catch (SQLException e) {
            LOG.warn(e);
            rollback();
        }
    }

    protected ResultSet executeQuery(String key, SQLParam... params) throws SQLException {
        if (connection == null) {
            throw new IllegalStateException("Connection not open. Call dao.openConnection() first");
        }
        preparedStatement = connection.prepareStatement(queries.getString(key));
        if (params.length > 0) {
            for (int i = 1; i <= params.length; i++) {
                setQueryParam(i, params[i - 1]);
            }
        }
        resultSet = preparedStatement.executeQuery();
        return resultSet;
    }

    protected void executeUpdate(String key, SQLParam... params) throws SQLException {
        if (connection == null) {
            throw new IllegalStateException("Connection not open. Call dao.openConnection() first");
        }
        preparedStatement = connection.prepareStatement(queries.getString(key));
        if (params.length > 0) {
            for (int i = 1; i <= params.length; i++) {
                setQueryParam(i, params[i - 1]);
            }
        }
        preparedStatement.executeUpdate();
    }

    private void setQueryParam(int i, SQLParam param) throws SQLException {
        switch(param.getType()) {
            case STRING:
                preparedStatement.setString(i, (String) param.getValue());
                break;
            default:
                throw new UnsupportedOperationException("Objects of type '" + param.getClass() + "' not supported");
        }
    }
}
