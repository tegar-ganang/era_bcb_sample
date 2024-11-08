package org.caleigo.core.service;

import java.util.*;
import java.sql.*;
import org.caleigo.core.*;
import org.caleigo.core.exception.*;
import org.caleigo.toolkit.log.*;

public class JDBCPreparedStatementDataService extends AbstractDataService {

    public static final int DEFAULT_MAX_CONNECTION_POOL_SIZE = 10;

    private static final int UNDEFINED_AUTO_INDEX = 0;

    private static final int JDBC_AUTO_INDEX = 1;

    private static final int SQLSERVER_AUTO_INDEX = 2;

    private static final int POSTGRE_AUTO_INDEX = 3;

    private static final int ORACLE_AUTO_INDEX = 4;

    private static final int HSQLDB_AUTO_INDEX = 5;

    private static final int UNSUPORTED_AUTO_INDEX = -1;

    private static Driver sDriver;

    private String mConnectionURL;

    private Properties mConnectionInfo;

    private SQLToolKitPS mSQLToolKit;

    private ConnectionPool mConnectionPool;

    private int mAutoIndexMethod = UNDEFINED_AUTO_INDEX;

    public static void setDriver(Driver driver) {
        sDriver = driver;
    }

    /** Default constructor for JDBCDataService.
     */
    public JDBCPreparedStatementDataService(IDataSourceDescriptor descriptor) {
        this(descriptor, descriptor.getSourceName(), "jdbc:odbc:" + descriptor.getSourceName(), new Properties(), DEFAULT_MAX_CONNECTION_POOL_SIZE);
    }

    public JDBCPreparedStatementDataService(IDataSourceDescriptor descriptor, Object serviceIdentity, String url) {
        this(descriptor, serviceIdentity, url, new Properties(), DEFAULT_MAX_CONNECTION_POOL_SIZE);
    }

    public JDBCPreparedStatementDataService(IDataSourceDescriptor descriptor, Object serviceIdentity, String url, String user, String password) {
        this(descriptor, serviceIdentity, url, new Properties(), DEFAULT_MAX_CONNECTION_POOL_SIZE);
        mConnectionInfo.put("user", user);
        mConnectionInfo.put("password", password);
    }

    public JDBCPreparedStatementDataService(IDataSourceDescriptor descriptor, Object serviceIdentity, String url, String user, String password, String catalog) {
        this(descriptor, serviceIdentity, url, user, password);
        if (catalog != null) mConnectionInfo.put("catalog", catalog);
    }

    public JDBCPreparedStatementDataService(IDataSourceDescriptor descriptor, Object serviceIdentity, String url, java.util.Properties info, int maxConnectionPoolSize) {
        super(descriptor.getCodeName(), serviceIdentity, descriptor);
        mConnectionURL = url;
        mConnectionInfo = info;
        mSQLToolKit = new SQLToolKitPS();
        mConnectionPool = new ConnectionPool(maxConnectionPoolSize);
        if (DataAccessManager.getManager().getAccessLevel(descriptor) == DataAccessManager.NONE) throw new SecurityException("No read access for " + descriptor + " data sources!");
    }

    public IDataTransaction newTransaction() {
        return new JDBCDataTransaction();
    }

    /** Should return true if the service is online and reponding to calls.
     */
    public boolean ping() {
        boolean responding = false;
        try {
            Connection connection = this.openConnection();
            if (connection != null) {
                this.closeConnection(connection);
                responding = true;
            }
        } catch (Exception e) {
        }
        return responding;
    }

    protected void executeLoad(Connection connection, IEntity entity, Qualifier qualifier) throws DataServiceException {
        try {
            if (DataAccessManager.getManager().getAccessLevel(entity.getEntityDescriptor()) == DataAccessManager.NONE) throw new SecurityException("No read access for " + entity.getEntityDescriptor() + " entities!");
            PreparedStatement preparedStatement = mSQLToolKit.buildSelectStatement(entity.getEntityDescriptor(), qualifier, connection);
            ResultSet set = preparedStatement.executeQuery();
            if (set.next()) {
                this.readResultSetRow(set, entity, false);
                if (!DataAccessManager.getManager().hasReadAccess(entity)) entity.clear(); else {
                    entity.setStatusFlag(IEntity.PERSISTENT);
                    entity.clearStatusFlag(IEntity.DIRTY | IEntity.EMPTY);
                }
            } else {
                entity.setStatusFlag(IEntity.EMPTY);
                entity.clearStatusFlag(IEntity.DIRTY | IEntity.PERSISTENT);
            }
            set.close();
        } catch (SQLException e) {
            throw new DataServiceException("Select command failed", e);
        }
    }

    protected void executeQuery(Connection connection, DataQuery query, ISelection selection) throws DataServiceException {
        try {
            if (DataAccessManager.getManager().getAccessLevel(query.getEntityDescriptor()) == DataAccessManager.NONE) throw new SecurityException("No read access for " + query.getEntityDescriptor() + " entities!");
            PreparedStatement statement = mSQLToolKit.buildSelectStatement(query.getEntityDescriptor(), query.getQualifier(), connection);
            ResultSet set = statement.executeQuery();
            IEntity entity;
            while (set.next()) {
                entity = query.getEntityDescriptor().createEntity();
                this.readResultSetRow(set, entity, true);
                entity.setStatusFlag(IEntity.PERSISTENT);
                entity.clearStatusFlag(IEntity.DIRTY | IEntity.EMPTY);
                if (DataAccessManager.getManager().hasReadAccess(entity)) selection.addEntity(entity);
            }
            set.close();
        } catch (SQLException e) {
            throw new DataServiceException("Select command failed", e);
        }
    }

    protected void executeInsert(Connection connection, IEntity entity) throws DataServiceException {
        String sql = null;
        try {
            this.checkEntityAsStorable(entity);
            PreparedStatement statement = mSQLToolKit.buildInsertStatement(entity, connection, true);
            Log.print(this, "Performing Insert: " + sql);
            int count = -1;
            if ((mAutoIndexMethod == JDBC_AUTO_INDEX || mAutoIndexMethod == UNDEFINED_AUTO_INDEX) && (mConnectionURL.indexOf("hsqldb") == -1)) {
                try {
                    count = statement.executeUpdate();
                } catch (Exception e) {
                }
            }
            if (count < 0) {
                statement = mSQLToolKit.buildInsertStatement(entity, connection, false);
                count = statement.executeUpdate();
            }
            if (count != 1) throw new DataServiceException("Multiple insert commands are dissabled, affect count " + count + " caused rejection.");
            if (this.hasAutoIndexField(entity.getEntityDescriptor())) this.updateAutoIndex(statement, entity);
        } catch (SQLException e) {
            throw new DataServiceException("Insert command failed", e, sql);
        }
    }

    protected void executeUpdate(Connection connection, IEntity entity, Qualifier qualifier) throws DataServiceException {
        try {
            this.checkEntityAsStorable(entity);
            if (!entity.isDirty()) {
                Log.printWarning(this, "Ignored update of non-dirty entity: " + entity);
                return;
            }
            PreparedStatement statement = mSQLToolKit.buildUpdateStatement(entity, qualifier, connection);
            int count = statement.executeUpdate();
            if (count != 1) throw new DataServiceException("Multiple update commands are dissabled, affect count " + count + " caused rejection.");
        } catch (SQLException e) {
            throw new DataServiceException("Update command failed", e);
        }
    }

    protected void executeDelete(Connection connection, IEntity entity) throws DataServiceException {
        try {
            this.checkEntityAsDeletable(entity);
            PreparedStatement statement = mSQLToolKit.buildDeleteStatement(entity.getEntityDescriptor(), entity.getOriginQualifier(), connection);
            int count = statement.executeUpdate();
            if (count > 1) throw new DataServiceException("Multiple delete commands are dissabled, affect count " + count + " caused rejection.");
        } catch (SQLException e) {
            throw new DataServiceException("Delete command failed", e);
        }
    }

    protected Connection openConnection() throws DataServiceException {
        return mConnectionPool.getConnection();
    }

    protected void closeConnection(Connection connection) throws DataServiceException {
        mConnectionPool.releaseConnection(connection);
    }

    protected void readResultSetRow(ResultSet set, IEntity entity, boolean useFastSetData) throws DataServiceException {
        try {
            IEntityDescriptor descriptor = entity.getEntityDescriptor();
            SQLToolKitPS.IDataTypeConverter converter = null;
            DataType dataType = null;
            Object data = null;
            for (int j = 0; j < set.getMetaData().getColumnCount(); j++) {
                dataType = descriptor.getFieldDescriptor(j).getDataType();
                converter = mSQLToolKit.getDataTypeConverter(dataType);
                data = this.getSetData(set, j + 1, dataType);
                if (converter != null) data = converter.convertFromDB(data); else if (data != null && data.getClass() != dataType.getDataClass()) data = dataType.convertFrom(data);
                if (useFastSetData) this.setEntityData(entity, j, data); else entity.setData(descriptor.getFieldDescriptor(j), data);
            }
        } catch (Exception e) {
            throw new DataServiceException("Select command failed", e);
        }
    }

    protected Object getSetData(ResultSet set, int index, DataType dataType) {
        try {
            Object data = set.getObject(index);
            if (data == null && !set.wasNull()) {
                if (dataType == DataType.STRING) data = set.getString(index); else if (dataType == DataType.BYTE) data = new Byte(set.getByte(index)); else if (dataType == DataType.SHORT) data = new Short(set.getShort(index)); else if (dataType == DataType.INTEGER) data = new Integer(set.getInt(index)); else if (dataType == DataType.LONG) data = new Long(set.getLong(index)); else if (dataType == DataType.FLOAT) data = new Float(set.getFloat(index)); else if (dataType == DataType.DOUBLE) data = new Double(set.getDouble(index)); else if (dataType == DataType.BOOLEAN) data = new Boolean(set.getBoolean(index)); else if (dataType instanceof DataType.BinaryType) data = dataType.convertFrom(set.getBinaryStream(index));
            }
            return data;
        } catch (Exception e) {
            throw new DataServiceException("Select command failed", e);
        }
    }

    /** This method updates autogenerated primary key field values.
     * Note that this method can only be used directly after a insert and
     * should still not be considered safe in an environment with frequent 
     * inserts to the entity's table.
     */
    protected void updateAutoIndex(Statement statement, IEntity entity) throws DataServiceException {
        String com = null;
        IFieldDescriptor field = null;
        if (mAutoIndexMethod == UNDEFINED_AUTO_INDEX) Log.print(this, "Evaluating usable auto index method.");
        if ((mAutoIndexMethod == JDBC_AUTO_INDEX || mAutoIndexMethod == UNDEFINED_AUTO_INDEX) && (mConnectionURL.indexOf("hsqldb") == -1)) {
            try {
                ResultSet set = statement.getGeneratedKeys();
                for (int j = 0; j < entity.getEntityDescriptor().getFieldCount(); j++) {
                    field = entity.getEntityDescriptor().getFieldDescriptor(j);
                    if (field.isAutoGenerated() && field.isIdentityField()) {
                        if (set.next()) {
                            Object data = field.getDataType().convertFrom(set.getObject(1));
                            entity.setData(field, data);
                            mAutoIndexMethod = JDBC_AUTO_INDEX;
                        }
                    }
                }
                while (set.next()) ;
            } catch (Exception e) {
                if (mAutoIndexMethod == JDBC_AUTO_INDEX) throw new DataServiceException("Update of auto generated index failed!", e, com);
            }
            if (mAutoIndexMethod == UNDEFINED_AUTO_INDEX) Log.print(this, "JDBC method not applicable.");
        }
        if (mAutoIndexMethod == POSTGRE_AUTO_INDEX || mAutoIndexMethod == UNDEFINED_AUTO_INDEX) {
            try {
                for (int j = 0; j < entity.getEntityDescriptor().getFieldCount(); j++) {
                    field = entity.getEntityDescriptor().getFieldDescriptor(j);
                    if (field.isAutoGenerated() && field.isIdentityField()) {
                        com = "SELECT currval('" + field.getEntityDescriptor().getSourceName() + "_" + field.getSourceName() + "_seq') ";
                        Statement identityStatement = statement.getConnection().createStatement();
                        Log.print(this, "Key retrieval: " + com);
                        ResultSet set = identityStatement.executeQuery(com);
                        if (set.next()) {
                            Object data = field.getDataType().convertFrom(set.getObject(1));
                            entity.setData(field, data);
                            mAutoIndexMethod = POSTGRE_AUTO_INDEX;
                        }
                        while (set.next()) ;
                    }
                }
            } catch (Exception e) {
                Log.printWarning(this, "Postgre method failed!");
                if (mAutoIndexMethod == POSTGRE_AUTO_INDEX) throw new DataServiceException("Update of auto generated index failed!", e, com);
            }
            if (mAutoIndexMethod == UNDEFINED_AUTO_INDEX) Log.print(this, "Postgre method not applicable.");
        }
        if (mAutoIndexMethod == SQLSERVER_AUTO_INDEX || mAutoIndexMethod == UNDEFINED_AUTO_INDEX) {
            try {
                for (int j = 0; j < entity.getEntityDescriptor().getFieldCount(); j++) {
                    field = entity.getEntityDescriptor().getFieldDescriptor(j);
                    if (field.isAutoGenerated() && field.isIdentityField()) {
                        com = "SELECT @@IDENTITY FROM " + field.getEntityDescriptor().getSourceName();
                        Statement identityStatement = statement.getConnection().createStatement();
                        Log.print(this, "Key retrieval: " + com);
                        ResultSet set = identityStatement.executeQuery(com);
                        if (set.next()) {
                            Object data = field.getDataType().convertFrom(set.getObject(1));
                            entity.setData(field, data);
                            mAutoIndexMethod = SQLSERVER_AUTO_INDEX;
                        }
                        while (set.next()) ;
                    }
                }
            } catch (Exception e) {
                if (mAutoIndexMethod == SQLSERVER_AUTO_INDEX) throw new DataServiceException("Update of auto generated index failed!", e, com);
            }
            if (mAutoIndexMethod == UNDEFINED_AUTO_INDEX) Log.print(this, "SQL-Server method not applicable.");
        }
        if (mAutoIndexMethod == ORACLE_AUTO_INDEX || mAutoIndexMethod == UNDEFINED_AUTO_INDEX) {
            try {
                for (int j = 0; j < entity.getEntityDescriptor().getFieldCount(); j++) {
                    field = entity.getEntityDescriptor().getFieldDescriptor(j);
                    if (field.isAutoGenerated() && field.isIdentityField()) {
                        com = "SELECT " + field.getEntityDescriptor().getSourceName() + "_" + field.getSourceName() + "_seq.currval FROM dual " + field.getEntityDescriptor().getSourceName();
                        Statement identityStatement = statement.getConnection().createStatement();
                        Log.print(this, "Key retrieval: " + com);
                        ResultSet set = identityStatement.executeQuery(com);
                        if (set.next()) {
                            Object data = field.getDataType().convertFrom(set.getObject(1));
                            entity.setData(field, data);
                            mAutoIndexMethod = ORACLE_AUTO_INDEX;
                        }
                        while (set.next()) ;
                    }
                }
            } catch (Exception e) {
                if (mAutoIndexMethod == ORACLE_AUTO_INDEX) throw new DataServiceException("Update of auto generated index failed!", e, com);
            }
            if (mAutoIndexMethod == UNDEFINED_AUTO_INDEX) Log.print(this, "Oracle method not applicable.");
        }
        if (mAutoIndexMethod == HSQLDB_AUTO_INDEX || mAutoIndexMethod == UNDEFINED_AUTO_INDEX) {
            try {
                for (int j = 0; j < entity.getEntityDescriptor().getFieldCount(); j++) {
                    field = entity.getEntityDescriptor().getFieldDescriptor(j);
                    if (field.isAutoGenerated() && field.isIdentityField()) {
                        com = "call identity()";
                        Statement identityStatement = statement.getConnection().createStatement();
                        Log.print(this, "Key retrieval: " + com);
                        ResultSet set = identityStatement.executeQuery(com);
                        if (set.next()) {
                            Object data = field.getDataType().convertFrom(set.getObject(1));
                            entity.setData(field, data);
                            mAutoIndexMethod = HSQLDB_AUTO_INDEX;
                        }
                        while (set.next()) ;
                    }
                }
            } catch (Exception e) {
                if (mAutoIndexMethod == HSQLDB_AUTO_INDEX) throw new DataServiceException("Update of auto generated index failed!", e, com);
            }
            if (mAutoIndexMethod == UNDEFINED_AUTO_INDEX) Log.print(this, "hsqldb method not applicable.");
        }
        if (mAutoIndexMethod == UNSUPORTED_AUTO_INDEX || mAutoIndexMethod == UNDEFINED_AUTO_INDEX) {
            mAutoIndexMethod = UNSUPORTED_AUTO_INDEX;
            throw new DataServiceException("Used JDBC Driver does not support extraction of Generated Keys!");
        }
    }

    protected boolean hasAutoIndexField(IEntityDescriptor entityDescriptor) {
        IFieldDescriptor autoIndexField = null;
        for (int j = 0; autoIndexField == null && j < entityDescriptor.getFieldCount(); j++) if (entityDescriptor.getFieldDescriptor(j).isAutoGenerated() && entityDescriptor.getFieldDescriptor(j).isIdentityField()) autoIndexField = entityDescriptor.getFieldDescriptor(j);
        return autoIndexField != null;
    }

    public String getURL() {
        return mConnectionURL;
    }

    public String getUser() {
        return mConnectionInfo.getProperty("user", null);
    }

    public String getPassword() {
        return mConnectionInfo.getProperty("password", null);
    }

    public SQLToolKitPS getSQLToolKit() {
        return mSQLToolKit;
    }

    public void setSQLToolKit(SQLToolKitPS kit) {
        mSQLToolKit = kit;
    }

    protected class JDBCDataTransaction extends AbstractDataTransaction {

        private Connection mConnection;

        public JDBCDataTransaction() {
            super(getTimeout());
        }

        /** Commit performs all the stored operations in the transaction. 
         * If any of the operations fail a rollback on all operations will be
         * automatically performed and a TransactionFailedException will be thrown.
         */
        public void commit() throws DataServiceException {
            long commitStartTime = System.currentTimeMillis();
            mConnection = openConnection();
            if (System.currentTimeMillis() - commitStartTime > 50) Log.printWarning(this, "Long connection open time: " + (System.currentTimeMillis() - commitStartTime) + " ms");
            try {
                try {
                    if (mConnectionInfo.containsKey("catalog")) mConnection.setCatalog((String) mConnectionInfo.get("catalog"));
                } catch (SQLException e) {
                }
                mConnection.setAutoCommit(false);
                Enumeration dataOperations = this.getOperations();
                while (dataOperations.hasMoreElements()) {
                    DataOperation operation = (DataOperation) dataOperations.nextElement();
                    switch(operation.getOperationType()) {
                        case DataOperation.LOAD:
                            executeLoad(mConnection, operation.getEntity(), operation.getQualifier());
                            break;
                        case DataOperation.QUERY:
                            executeQuery(mConnection, operation.getDataQuery(), operation.getEntitySelection());
                            break;
                        case DataOperation.STORE:
                            if (operation.getEntity().isPersistent()) executeUpdate(mConnection, operation.getEntity(), operation.getQualifier()); else executeInsert(mConnection, operation.getEntity());
                            break;
                        case DataOperation.DELETE:
                            executeDelete(mConnection, operation.getEntity());
                            break;
                        case DataOperation.REFRESH:
                            executeLoad(mConnection, operation.getEntity(), operation.getQualifier());
                            break;
                    }
                }
                mConnection.commit();
                Log.print(this, "Commit time: " + (System.currentTimeMillis() - commitStartTime) + " ms");
                dataOperations = this.getOperations();
                while (dataOperations.hasMoreElements()) {
                    DataOperation operation = (DataOperation) dataOperations.nextElement();
                    switch(operation.getOperationType()) {
                        case DataOperation.DELETE:
                            operation.getEntity().clearStatusFlag(IEntity.DIRTY | IEntity.EMPTY | IEntity.PERSISTENT);
                            break;
                        case DataOperation.STORE:
                            operation.getEntity().setStatusFlag(IEntity.PERSISTENT);
                            operation.getEntity().clearStatusFlag(IEntity.DIRTY | IEntity.EMPTY);
                            break;
                        case DataOperation.REFRESH:
                        case DataOperation.LOAD:
                        case DataOperation.QUERY:
                            break;
                    }
                }
            } catch (Exception e) {
                String rollbackMsg;
                try {
                    mConnection.rollback();
                    rollbackMsg = "rollback succesfull";
                } catch (SQLException eSQL) {
                    rollbackMsg = "rollback failed";
                }
                if (e instanceof DataServiceException) throw new DataServiceException("Transaction failed, " + rollbackMsg, e, ((DataServiceException) e).getDescription()); else throw new DataServiceException("Transaction failed, " + rollbackMsg + ": " + e.getClass().getName() + " - " + e.getMessage(), e);
            } finally {
                closeConnection(mConnection);
                mConnection = null;
            }
        }

        public void abortTransaction() throws DataServiceException {
            try {
                if (mConnection != null) {
                    if (mConnection.getAutoCommit() == false) mConnection.rollback();
                    mConnection.close();
                    mConnection = null;
                }
            } catch (SQLException e) {
                throw new DataServiceException("Failed to abort transaction", e);
            }
        }
    }

    protected class ConnectionPool {

        private int mMaxSize;

        private Connection[] mConnectionPool;

        private boolean[] mFreeConnectionFlags;

        private List mWaitingThreads;

        /** Creates a connection pool with the maximum size <code>maxSize</code>.
         * Setting the maximum size to zero means that every call to <code>getConnection</code>
         * will create a new connection.
         */
        public ConnectionPool(int maxSize) {
            mMaxSize = maxSize;
            if (maxSize > 0) {
                mConnectionPool = new Connection[maxSize];
                mFreeConnectionFlags = new boolean[maxSize];
                mWaitingThreads = new ArrayList();
            }
        }

        public Connection getConnection() throws DataServiceException {
            if (mMaxSize == 0) {
                try {
                    if (sDriver != null) return sDriver.connect(mConnectionURL, mConnectionInfo);
                    if (mConnectionInfo.get("user") != null) return DriverManager.getConnection(mConnectionURL, mConnectionInfo); else return DriverManager.getConnection(mConnectionURL);
                } catch (SQLException e) {
                    throw new DataServiceException("Failed to open database connection: " + e.getMessage());
                }
            }
            try {
                return this.getConnectionFromPool();
            } catch (Exception e) {
                throw new DataServiceException("Failed to open database connection: " + e.getMessage());
            }
        }

        public int getMaxSize() {
            return mMaxSize;
        }

        public synchronized void releaseConnection(Connection connection) throws DataServiceException {
            if (connection == null) return;
            if (mMaxSize == 0) {
                try {
                    connection.close();
                    return;
                } catch (SQLException e) {
                    throw new DataServiceException("Failed to open database connection: " + e.getMessage());
                }
            }
            int index = 0;
            for (; index < mConnectionPool.length && mConnectionPool[index] != connection; index++) ;
            if (index < mConnectionPool.length) {
                mFreeConnectionFlags[index] = true;
                this.notifyAll();
            }
        }

        private synchronized Connection getConnectionFromPool() throws SQLException, InterruptedException {
            Connection conn = null;
            if (mWaitingThreads.size() == 0) conn = this.getNextFreeConnectionFromPool();
            if (conn == null) mWaitingThreads.add(Thread.currentThread());
            while (conn == null) {
                Log.print(this, "Waiting for free connection.");
                this.wait();
                if (mWaitingThreads.get(0) == Thread.currentThread()) conn = this.getNextFreeConnectionFromPool();
            }
            if (mWaitingThreads.contains(Thread.currentThread())) ;
            mWaitingThreads.remove(Thread.currentThread());
            return conn;
        }

        private synchronized Connection getNextFreeConnectionFromPool() throws SQLException {
            int index = 0;
            for (; index < mConnectionPool.length && mConnectionPool[index] != null && !mFreeConnectionFlags[index]; index++) ;
            if (index >= mConnectionPool.length) return null;
            if (mConnectionPool[index] != null) {
                mFreeConnectionFlags[index] = false;
                if (mConnectionPool[index].isClosed()) mConnectionPool[index] = this.openNewConnection();
                return mConnectionPool[index];
            }
            mConnectionPool[index] = this.openNewConnection();
            Log.print(this, "Extending connection pool for " + getServiceIdentity() + " to " + (index + 1) + " connections.");
            return mConnectionPool[index];
        }

        private Connection openNewConnection() throws SQLException {
            if (sDriver != null) return sDriver.connect(mConnectionURL, mConnectionInfo);
            if (mConnectionInfo.get("user") != null) return DriverManager.getConnection(mConnectionURL, mConnectionInfo); else return DriverManager.getConnection(mConnectionURL);
        }
    }
}
