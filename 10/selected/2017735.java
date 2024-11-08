package org.monet.kernel.agents;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.monet.kernel.configuration.Configuration;
import org.monet.kernel.constants.Common;
import org.monet.kernel.constants.Database;
import org.monet.kernel.constants.ErrorCode;
import org.monet.kernel.constants.SiteFiles;
import org.monet.kernel.constants.Strings;
import org.monet.kernel.exceptions.DatabaseException;
import org.monet.kernel.exceptions.FilesystemException;
import org.monet.kernel.exceptions.SystemException;
import org.monet.kernel.model.Context;
import org.monet.kernel.model.DatabaseRepositoryQuery;
import org.monet.kernel.model.MonetResultSet;
import org.monet.kernel.sql.NamedParameterStatement;
import org.monet.kernel.sql.QueryBuilder;
import org.monet.kernel.utils.BufferedQuery;
import org.monet.kernel.utils.Resources;
import org.monet.kernel.utils.StreamHelper;

public abstract class AgentDatabase {

    private static final String DATABASE_RESOURCES_PATH = "/kernel/database";

    String error;

    Properties queryRepository;

    String queryFieldPrefix;

    String queryFieldBindPrefix;

    String queryFieldSuffix;

    String queryFieldBindSuffix;

    String idRoot;

    Context context;

    Configuration configuration;

    String connectionChain;

    DataSource dataSource;

    AgentLogger logger;

    protected static AgentDatabase instance;

    protected static String TYPE;

    private static final String QUERY_ESCAPED_SEMICOLON = "::SEMICOLON::";

    protected AgentDatabase() {
        this.error = Strings.EMPTY;
        this.queryRepository = new Properties();
        this.context = Context.getInstance();
        this.configuration = Configuration.getInstance();
        this.connectionChain = "";
        this.dataSource = null;
        this.logger = AgentLogger.getInstance();
    }

    public abstract class ColumnTypes {

        public static final String BOOLEAN = "boolean";

        public static final String DATE = "date";

        public static final String TEXT = "text";

        public static final String INTEGER = "integer";

        public static final String FLOAT = "float";
    }

    public String getOrderMode(String mode) {
        if (mode.equals(Common.OrderMode.ASCENDANT)) return "asc"; else if (mode.equals(Common.OrderMode.DESCENDANT)) return "desc";
        return "asc";
    }

    public abstract String getDateAsText(Date date);

    public Timestamp getDateAsTimestamp(Date date) {
        return date != null ? new Timestamp(date.getTime()) : null;
    }

    public abstract String getColumnDefinition(String type);

    public abstract int getQueryStartPos(int startPos);

    public static synchronized AgentDatabase getInstance() {
        if (TYPE == null) throw new DatabaseException(ErrorCode.DATABASE_CONNECTION, null);
        if (instance == null) {
            if (TYPE.equalsIgnoreCase(Database.Types.MYSQL)) instance = new AgentDatabaseMysql(); else if (TYPE.equalsIgnoreCase(Database.Types.ORACLE)) instance = new AgentDatabaseOracle();
        }
        return instance;
    }

    public static synchronized boolean setType(String newType) {
        AgentDatabase.TYPE = newType;
        return true;
    }

    public boolean initialize(String dataSourceName) {
        String repositoryQueriesFilename = DATABASE_RESOURCES_PATH + Strings.BAR45 + TYPE + SiteFiles.Suffix.QUERIES;
        InputStream repositoryQueriesInputStream = null;
        javax.naming.Context context;
        javax.naming.Context envContext;
        this.queryRepository = new Properties();
        try {
            context = new InitialContext();
            repositoryQueriesInputStream = Resources.getAsStream(repositoryQueriesFilename);
            this.queryRepository.load(repositoryQueriesInputStream);
            this.queryFieldPrefix = this.queryRepository.getProperty(Database.QueryFields.PREFIX);
            this.queryFieldBindPrefix = this.queryRepository.getProperty(Database.QueryFields.PREFIX_BIND);
            this.queryFieldSuffix = this.queryRepository.getProperty(Database.QueryFields.SUFFIX);
            this.queryFieldBindSuffix = this.queryRepository.getProperty(Database.QueryFields.SUFFIX_BIND);
            this.idRoot = this.queryRepository.getProperty(Database.QueryFields.DATA_ID_ROOT);
            envContext = (javax.naming.Context) context.lookup("java:comp/env");
            this.dataSource = (DataSource) envContext.lookup(dataSourceName);
        } catch (NamingException e) {
            throw new FilesystemException(ErrorCode.FILESYSTEM_READ_FILE, repositoryQueriesFilename, e);
        } catch (IOException e) {
            throw new FilesystemException(ErrorCode.FILESYSTEM_READ_FILE, repositoryQueriesFilename, e);
        } finally {
            StreamHelper.close(repositoryQueriesInputStream);
        }
        return true;
    }

    private static final void close(Connection connection) {
        if (connection != null) try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
        }
    }

    private static final void close(NamedParameterStatement statement) {
        if (statement != null) try {
            statement.close();
        } catch (SQLException e) {
        }
    }

    private static final void close(Statement statement) {
        if (statement != null) try {
            statement.close();
        } catch (SQLException e) {
        }
    }

    private ResultSet doSelectQuery(Connection connection, NamedParameterStatement statement) {
        ResultSet result;
        result = null;
        try {
            result = statement.executeQuery();
        } catch (SQLException exception) {
            throw new DatabaseException(ErrorCode.DATABASE_SELECT_QUERY, statement.getQuery(), exception);
        }
        return result;
    }

    private int doUpdateQuery(Connection connection, NamedParameterStatement statement) {
        int result;
        try {
            result = statement.executeUpdate();
        } catch (Exception exception) {
            throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, statement.getQuery(), exception);
        } finally {
            close(statement);
        }
        return result;
    }

    private String doUpdateQueryAndGetGeneratedKey(Connection connection, NamedParameterStatement statement) {
        ResultSet result = null;
        try {
            result = statement.executeUpdateAndGetGeneratedKeys();
            result.next();
            return result.getString(1);
        } catch (Exception exception) {
            throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, statement.getQuery(), exception);
        } finally {
            this.closeQuery(result);
            close(statement);
        }
    }

    private NamedParameterStatement getPreparedStatement(Connection connection, String query) {
        NamedParameterStatement result = null;
        try {
            result = new NamedParameterStatement(connection, query);
        } catch (SQLException oException) {
            throw new DatabaseException(ErrorCode.QUERY_FAILED, query, oException);
        }
        return result;
    }

    public NamedParameterStatement getRepositoryPreparedStatement(Connection connection, String name, HashMap<String, Object> parameters, HashMap<String, String> subQueries) {
        NamedParameterStatement preparedStatement = null;
        String query;
        QueryBuilder queryBuilder;
        if (!this.queryRepository.containsKey(name)) {
            throw new DatabaseException(ErrorCode.UNKOWN_DATABASE_QUERY, name);
        }
        query = (String) this.queryRepository.get(name);
        try {
            queryBuilder = new QueryBuilder(query);
            if (subQueries != null) for (Entry<String, String> subQuery : subQueries.entrySet()) queryBuilder.insertSubQuery(subQuery.getKey(), subQuery.getValue());
            preparedStatement = new NamedParameterStatement(connection, queryBuilder.build());
            if (parameters != null) for (Entry<String, Object> parameter : parameters.entrySet()) {
                try {
                    preparedStatement.setObject(parameter.getKey(), parameter.getValue());
                } catch (SQLException ex) {
                    throw new SQLException(ex.getMessage() + " on column: " + parameter.getKey(), ex);
                }
            }
        } catch (SQLException oException) {
            throw new DatabaseException(ErrorCode.QUERY_FAILED, query, oException);
        }
        return preparedStatement;
    }

    public NamedParameterStatement getRepositoryPreparedStatement(Connection connection, String name, int returnGeneratedKeys) {
        return this.getRepositoryPreparedStatement(connection, name, null, null, returnGeneratedKeys);
    }

    public NamedParameterStatement getRepositoryPreparedStatement(Connection connection, String name, HashMap<String, Object> parameters, int returnGeneratedKeys) {
        return this.getRepositoryPreparedStatement(connection, name, parameters, null, returnGeneratedKeys);
    }

    public NamedParameterStatement getRepositoryPreparedStatement(Connection connection, String name, HashMap<String, Object> parameters, HashMap<String, String> subQueries, int returnGeneratedKeys) {
        NamedParameterStatement preparedStatement = null;
        String query;
        QueryBuilder queryBuilder;
        if (!this.queryRepository.containsKey(name)) {
            throw new DatabaseException(ErrorCode.UNKOWN_DATABASE_QUERY, name);
        }
        query = (String) this.queryRepository.get(name);
        try {
            queryBuilder = new QueryBuilder(query);
            if (subQueries != null) for (Entry<String, String> subQuery : subQueries.entrySet()) queryBuilder.insertSubQuery(subQuery.getKey(), subQuery.getValue());
            preparedStatement = new NamedParameterStatement(connection, queryBuilder.build(), returnGeneratedKeys);
            if (parameters != null) for (Entry<String, Object> parameter : parameters.entrySet()) preparedStatement.setObject(parameter.getKey(), parameter.getValue());
        } catch (SQLException oException) {
            throw new DatabaseException(ErrorCode.QUERY_FAILED, query, oException);
        }
        return preparedStatement;
    }

    public NamedParameterStatement[] getRepositoryPreparedStatements(Connection connection, DatabaseRepositoryQuery[] queries) {
        NamedParameterStatement[] result = new NamedParameterStatement[queries.length];
        int pos = 0;
        for (pos = 0; pos < queries.length; pos++) {
            result[pos] = this.getRepositoryPreparedStatement(connection, queries[pos].getName(), queries[pos].getParameters(), queries[pos].getSubQueries());
        }
        return result;
    }

    public String getRepositoryProperty(String name) {
        if (!this.queryRepository.containsKey(name)) {
            throw new DatabaseException(ErrorCode.UNKOWN_PROPERTY, name);
        }
        return (String) this.queryRepository.get(name);
    }

    public String getRepositoryQuery(String name) {
        String query;
        if (!this.queryRepository.containsKey(name)) {
            throw new DatabaseException(ErrorCode.UNKOWN_DATABASE_QUERY, name);
        }
        query = (String) this.queryRepository.get(name);
        return query;
    }

    public String[] getRepositoryQueries(String[] queries) {
        int pos;
        String[] result = new String[queries.length];
        for (pos = 0; pos < queries.length; pos++) {
            result[pos] = this.getRepositoryQuery(queries[pos]);
        }
        return result;
    }

    public boolean executeRepositoryUpdateTransaction(DatabaseRepositoryQuery[] queries) {
        Connection connection = null;
        int pos = 0;
        NamedParameterStatement[] queriesArray;
        boolean autoCommit = true;
        if (queries.length == 0) {
            return true;
        }
        try {
            connection = this.dataSource.getConnection();
            queriesArray = this.getRepositoryPreparedStatements(connection, queries);
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            for (pos = 0; pos < queriesArray.length; pos++) {
                try {
                    queriesArray[pos].executeUpdate();
                } finally {
                    queriesArray[pos].close();
                }
            }
            if (autoCommit) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException oException) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException oRollbackException) {
                throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, Strings.ROLLBACK, oRollbackException);
            }
        } finally {
            close(connection);
        }
        return true;
    }

    public boolean executeRepositoryQueries(DatabaseRepositoryQuery[] queries) {
        Connection connection = null;
        String query = null;
        NamedParameterStatement statement = null;
        boolean autoCommit = true;
        try {
            connection = this.dataSource.getConnection();
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            for (DatabaseRepositoryQuery dbQuery : queries) {
                query = this.getRepositoryQuery(dbQuery.getName());
                QueryBuilder queryBuilder = new QueryBuilder(query);
                for (Entry<String, String> subQuery : dbQuery.getSubQueries().entrySet()) queryBuilder.insertSubQuery(subQuery.getKey(), subQuery.getValue());
                String[] singleQueries = queryBuilder.build().split("::SEMICOLON::");
                for (String sinlgeQuery : singleQueries) {
                    statement = new NamedParameterStatement(connection, sinlgeQuery);
                    for (Entry<String, Object> param : dbQuery.getParameters().entrySet()) statement.setObject(param.getKey(), param.getValue());
                    statement.execute();
                }
            }
            if (autoCommit) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException oRollbackException) {
                throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, Strings.ROLLBACK, oRollbackException);
            }
            throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, query, exception);
        } finally {
            close(connection);
        }
        return true;
    }

    public MonetResultSet executeRepositorySelectQuery(String name) {
        return this.executeRepositorySelectQuery(name, null, null);
    }

    public MonetResultSet executeRepositorySelectQuery(String name, HashMap<String, Object> parameters) {
        return this.executeRepositorySelectQuery(name, parameters, null);
    }

    public MonetResultSet executeRepositorySelectQuery(String name, HashMap<String, Object> parameters, HashMap<String, String> subQueries) {
        Connection connection = null;
        NamedParameterStatement statement;
        MonetResultSet result = null;
        try {
            connection = this.dataSource.getConnection();
            statement = this.getRepositoryPreparedStatement(connection, name, parameters, subQueries);
            result = new MonetResultSet(connection, this.doSelectQuery(connection, statement));
        } catch (SQLException ex) {
            throw new DatabaseException(ErrorCode.DATABASE_CONNECTION, name, ex);
        }
        return result;
    }

    public int executeRepositoryUpdateQuery(String name) {
        return this.executeRepositoryUpdateQuery(name, null, null);
    }

    public int executeRepositoryUpdateQuery(String name, HashMap<String, Object> parameters) {
        return this.executeRepositoryUpdateQuery(name, parameters, null);
    }

    public int executeRepositoryUpdateQuery(String name, HashMap<String, Object> parameters, HashMap<String, String> subQueries) {
        Connection connection = null;
        NamedParameterStatement statement = null;
        int result = -1;
        try {
            connection = this.dataSource.getConnection();
            statement = this.getRepositoryPreparedStatement(connection, name, parameters, subQueries);
            result = this.doUpdateQuery(connection, statement);
        } catch (SQLException ex) {
            throw new DatabaseException(ErrorCode.QUERY_FAILED, statement != null ? statement.getQuery() : null, ex);
        } finally {
            close(connection);
        }
        return result;
    }

    public String executeRepositoryUpdateQueryAndGetGeneratedKey(String name, HashMap<String, Object> parameters) {
        return executeRepositoryUpdateQueryAndGetGeneratedKey(name, parameters, null);
    }

    public String executeRepositoryUpdateQueryAndGetGeneratedKey(String name, HashMap<String, Object> parameters, HashMap<String, String> subQueries) {
        Connection connection = null;
        NamedParameterStatement statement = null;
        String result = "";
        try {
            connection = this.dataSource.getConnection();
            statement = this.getRepositoryPreparedStatement(connection, name, parameters, subQueries, Statement.RETURN_GENERATED_KEYS);
            result = this.doUpdateQueryAndGetGeneratedKey(connection, statement);
        } catch (SQLException ex) {
            throw new DatabaseException(ErrorCode.QUERY_FAILED, statement != null ? statement.getQuery() : null, ex);
        } finally {
            close(connection);
        }
        return result;
    }

    public boolean executeUpdateTransaction(BufferedQuery bufferedQuery) {
        Connection connection = null;
        boolean autoCommit = true;
        String query;
        Statement statement = null;
        try {
            connection = this.dataSource.getConnection();
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            while ((query = bufferedQuery.readQuery()) != null) {
                statement.execute(query);
            }
            if (autoCommit) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException oRollbackException) {
                throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, Strings.ROLLBACK, oRollbackException);
            }
        } finally {
            close(statement);
            close(connection);
        }
        return true;
    }

    public String getRootId() {
        return this.idRoot;
    }

    public String getConnector() {
        if (AgentDatabase.TYPE == null) return Strings.EMPTY;
        if (AgentDatabase.TYPE.equalsIgnoreCase(Database.Types.MYSQL)) return "com.mysql.jdbc.Driver"; else if (AgentDatabase.TYPE.equalsIgnoreCase(Database.Types.ORACLE)) return "oracle.jdbc.driver.OracleDriver";
        return Strings.EMPTY;
    }

    public MonetResultSet executeSelectQuery(String query) {
        Connection connection = null;
        NamedParameterStatement statement;
        try {
            connection = this.dataSource.getConnection();
            statement = this.getPreparedStatement(connection, query);
            return new MonetResultSet(connection, this.doSelectQuery(connection, statement));
        } catch (SQLException ex) {
            throw new DatabaseException(ErrorCode.DATABASE_CONNECTION, query, ex);
        }
    }

    public boolean executeBatchQueries(String batchQueries) {
        Connection connection = null;
        Statement statement = null;
        boolean autoCommit = true;
        String[] batchQueriesArray = batchQueries.split(Strings.SEMICOLON);
        int pos;
        if (batchQueriesArray.length == 0) {
            return true;
        }
        try {
            connection = this.dataSource.getConnection();
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            for (pos = 0; pos < batchQueriesArray.length; pos++) {
                batchQueriesArray[pos] = batchQueriesArray[pos].trim();
                if (batchQueriesArray[pos].equals(Strings.EMPTY)) continue;
                batchQueriesArray[pos] = batchQueriesArray[pos].replace(AgentDatabase.QUERY_ESCAPED_SEMICOLON, Strings.SEMICOLON);
                statement.addBatch(batchQueriesArray[pos].trim());
            }
            statement.executeBatch();
            if (autoCommit) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException oRollbackException) {
                throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, Strings.ROLLBACK, oRollbackException);
            }
        } finally {
            close(statement);
            close(connection);
        }
        return true;
    }

    public boolean closeQuery(ResultSet result) {
        if (result == null) return false;
        try {
            if (result.getStatement() != null) result.getStatement().close();
            result.close();
            result = null;
        } catch (Exception exception) {
            throw new SystemException(ErrorCode.CLOSE_QUERY, null, exception);
        }
        return true;
    }
}
