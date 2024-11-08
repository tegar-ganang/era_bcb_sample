package nl.dualit.clazzified.store.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import nl.dualit.clazzified.store.StoreConfiguration;
import org.apache.log4j.Logger;

public class HSQLDBHandler extends AbstractDbHandler {

    private Connection connection;

    private final Logger log = Logger.getLogger(HSQLDBHandler.class);

    private Connection getConnection() {
        try {
            if (connection == null) connect();
            if (connection.isClosed()) connect();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return connection;
    }

    private void connect() {
        try {
            try {
                Class.forName("org.hsqldb.jdbcDriver");
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
            connection = DriverManager.getConnection("jdbc:hsqldb:file:" + StoreConfiguration.getConnectionString() + ";shutdown=true");
            if (connection == null) throw new NullPointerException("connection is null");
            if (connection.isClosed()) throw new RuntimeException("Cannot connect to database ");
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
	 * Return the id of the last inserted row.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
    protected long getInsertId(Statement statement) throws SQLException {
        String sql = "CALL IDENTITY()";
        ResultSet rs = statement.executeQuery(sql);
        rs.next();
        return rs.getLong(1);
    }

    @Override
    public int deleteStatement(String sql) {
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            int result = statement.executeUpdate(sql.toString());
            if (result == 0) log.warn(sql + " result row count is 0");
            getConnection().commit();
            return result;
        } catch (SQLException e) {
            try {
                getConnection().rollback();
            } catch (SQLException e1) {
                log.error(e1.getMessage(), e1);
            }
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        } finally {
            try {
                statement.close();
                getConnection().close();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public long insertStatement(String sql) {
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            long result = statement.executeUpdate(sql.toString());
            if (result == 0) log.warn(sql + " result row count is 0");
            getConnection().commit();
            return getInsertId(statement);
        } catch (SQLException e) {
            try {
                getConnection().rollback();
            } catch (SQLException e1) {
                log.error(e1.getMessage(), e1);
            }
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        } finally {
            try {
                statement.close();
                getConnection().close();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public ResultSet selectStatement(String sql) {
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            ResultSet result = statement.executeQuery(sql);
            return result;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return null;
        } finally {
            try {
                statement.close();
                getConnection().close();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public int updateStatement(String sql) {
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            return statement.executeUpdate(sql);
        } catch (SQLException e) {
            try {
                getConnection().rollback();
            } catch (SQLException e1) {
                log.error(e1.getMessage(), e1);
            }
            log.error(e.getMessage(), e);
            return 0;
        } finally {
            try {
                statement.close();
                getConnection().close();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
