package com.completex.objective.components.persistency.core.impl;

import com.completex.objective.components.OdalRuntimeException;
import com.completex.objective.components.log.Log;
import com.completex.objective.components.persistency.AbstractParameters;
import com.completex.objective.components.persistency.AbstractPersistentObject;
import com.completex.objective.components.persistency.BasicQuery;
import com.completex.objective.components.persistency.Call;
import com.completex.objective.components.persistency.CallFactory;
import com.completex.objective.components.persistency.CallParameter;
import com.completex.objective.components.persistency.CallParameters;
import com.completex.objective.components.persistency.ColumnType;
import com.completex.objective.components.persistency.DeleteQuery;
import com.completex.objective.components.persistency.DeleteQueryFactory;
import com.completex.objective.components.persistency.DuplicateRecordException;
import com.completex.objective.components.persistency.LifeCycleController;
import com.completex.objective.components.persistency.Link;
import com.completex.objective.components.persistency.LockType;
import com.completex.objective.components.persistency.LockedRecordException;
import com.completex.objective.components.persistency.MetaColumn;
import com.completex.objective.components.persistency.OdalPersistencyException;
import com.completex.objective.components.persistency.OdalRuntimePersistencyException;
import com.completex.objective.components.persistency.Parameter;
import com.completex.objective.components.persistency.Parameters;
import com.completex.objective.components.persistency.Parent;
import com.completex.objective.components.persistency.Persistency;
import com.completex.objective.components.persistency.PersistentObject;
import com.completex.objective.components.persistency.PersistentObjectFactory;
import com.completex.objective.components.persistency.Query;
import com.completex.objective.components.persistency.QueryCtl;
import com.completex.objective.components.persistency.QueryFactory;
import com.completex.objective.components.persistency.QuickPersistency;
import com.completex.objective.components.persistency.Record;
import com.completex.objective.components.persistency.RecordValidationException;
import com.completex.objective.components.persistency.ResultableQueryManager;
import com.completex.objective.components.persistency.SelectQueryBuilder;
import com.completex.objective.components.persistency.State;
import com.completex.objective.components.persistency.UpdateQuery;
import com.completex.objective.components.persistency.UpdateQueryBuilder;
import com.completex.objective.components.persistency.UpdateQueryFactory;
import com.completex.objective.components.persistency.policy.DatabasePolicy;
import com.completex.objective.components.persistency.core.ResultSetCtl;
import com.completex.objective.components.persistency.core.ResultSetExecutorManager;
import com.completex.objective.components.persistency.core.ResultSetIterator;
import com.completex.objective.components.persistency.core.TypeHandlerRegistry;
import com.completex.objective.components.persistency.core.impl.query.AbstractQueryFactory;
import com.completex.objective.components.persistency.core.impl.query.CallFactoryImpl;
import com.completex.objective.components.persistency.core.impl.query.DeleteQueryFactoryImpl;
import com.completex.objective.components.persistency.core.impl.query.QueryContext;
import com.completex.objective.components.persistency.core.impl.query.QueryContextImpl;
import com.completex.objective.components.persistency.core.impl.query.QueryFactoryImpl;
import com.completex.objective.components.persistency.core.impl.query.UpdateQueryFactoryImpl;
import com.completex.objective.components.persistency.core.impl.query.UpdateQueryImpl;
import com.completex.objective.components.persistency.key.impl.AbstractSequenceKeyGenerator;
import com.completex.objective.components.persistency.key.impl.SimpleSequenceKeyGeneratorImpl;
import com.completex.objective.components.persistency.rule.RuntimeRuleException;
import com.completex.objective.components.persistency.transact.Transaction;
import com.completex.objective.components.persistency.transact.TransactionManager;
import com.completex.objective.components.persistency.type.BlobImpl;
import com.completex.objective.components.persistency.type.ClobImpl;
import com.completex.objective.components.persistency.type.DefaultTypeHandler;
import com.completex.objective.components.persistency.type.LobPostProcessing;
import com.completex.objective.components.persistency.type.LobPostProcessings;
import com.completex.objective.components.persistency.type.MultipartCollection;
import com.completex.objective.components.persistency.type.TracingCollection;
import com.completex.objective.components.persistency.type.TypeHandler;
import com.completex.objective.util.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Gennady Krizhevsky
 */
public abstract class BasicDatabasePersistencyImpl extends AbstractDatabasePersistency implements QuickPersistency, UpdateQueryBuilder.AddToUpdateBufferPlugin {

    private ResultSetExecutorManagerImpl defaultRsExecutorManager = new ResultSetExecutorManagerImpl();

    private ResultSetExecutorManager rsExecutorManager = defaultRsExecutorManager;

    private ResultSetCtlImpl defaultRsCtl = new ResultSetCtlImpl();

    private ResultSetCtl rsCtl = defaultRsCtl;

    private DatabasePolicy databasePolicy;

    private QueryFactory queryFactory;

    private CallFactory callFactory;

    private ThreadLocal queryContextLocal = new ThreadLocal();

    private Log logger = Log.NULL_LOGGER;

    private TransactionManager transactionManager;

    private AbstractSequenceKeyGenerator defaultKeyGenerator;

    public static final DefaultTypeHandler DEFAULT_TYPE_HANDLER = new DefaultTypeHandler();

    private boolean supportKeyUpdate;

    private boolean bindNulls = true;

    private SelectQueryBuilderImpl selectQueryBuilder = new SelectQueryBuilderImpl();

    private UpdateQueryBuilderImpl updateQueryBuilder;

    private DeleteQueryBuilderImpl deleteQueryBuilder;

    private TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistryImpl();

    private UpdateQueryFactoryImpl updateQueryFactory;

    private DeleteQueryFactoryImpl deleteQueryFactory;

    /**
     * @param databasePolicy
     * @param transactionManager
     * @param logger
     */
    public BasicDatabasePersistencyImpl(DatabasePolicy databasePolicy, TransactionManager transactionManager, Log logger) {
        this();
        this.databasePolicy = databasePolicy;
        this.transactionManager = transactionManager;
        setLogger(logger);
        setupDefaultKeyGenerator(databasePolicy, logger);
    }

    public BasicDatabasePersistencyImpl() {
        setupDefaults();
    }

    protected BasicDatabasePersistencyImpl(DatabasePolicy databasePolicy, Log logger) {
        this(databasePolicy, null, logger);
    }

    private void setupDefaults() {
        queryFactory = new QueryFactoryImpl(this, selectQueryBuilder);
        callFactory = new CallFactoryImpl(this, selectQueryBuilder);
        defaultRsExecutorManager.setPersistency(this);
        defaultRsCtl.setPersistency(this);
        updateQueryBuilder = new UpdateQueryBuilderImpl(true);
        deleteQueryBuilder = new DeleteQueryBuilderImpl(true);
        updateQueryFactory = new UpdateQueryFactoryImpl(this);
        deleteQueryFactory = new DeleteQueryFactoryImpl(this);
    }

    private void setupDefaultKeyGenerator(DatabasePolicy databasePolicy, Log logger) {
        SimpleSequenceKeyGeneratorImpl keyGenerator = new SimpleSequenceKeyGeneratorImpl();
        keyGenerator.setDatabasePolicy(databasePolicy);
        keyGenerator.setLogger(logger);
        defaultKeyGenerator = keyGenerator;
    }

    public void setUseBatchModify(boolean useBatchModify) {
        super.setUseBatchModify(useBatchModify);
    }

    public void setDefaultKeyGenerator(AbstractSequenceKeyGenerator defaultKeyGenerator) {
        this.defaultKeyGenerator = defaultKeyGenerator;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Registers default type handler for read and bind for given ColumnType
     *
     * @param columnType  ColumnType
     * @param typeHandler TypeHandler
     */
    public void registerTypeHandler(ColumnType columnType, TypeHandler typeHandler) {
        typeHandlerRegistry.registerTypeHandler(columnType, typeHandler);
    }

    /**
     * Returns default type handler for read and bind for given ColumnType
     *
     * @param columnType
     * @return default type handler for read and bind for given ColumnType
     */
    protected TypeHandler getTypeHandler(ColumnType columnType) {
        TypeHandler typeHandler = typeHandlerRegistry.getTypeHandler(columnType);
        if (typeHandler == null) {
            typeHandler = DEFAULT_TYPE_HANDLER;
        }
        return typeHandler;
    }

    static TypeHandler getTypeHandler(ColumnType columnType, Map typeHandlers) {
        TypeHandler typeHandler = null;
        if (columnType != null) {
            if (columnType.getCustomTypeHandler() != null) {
                typeHandler = columnType.getCustomTypeHandler();
            } else {
                typeHandler = (TypeHandler) typeHandlers.get(columnType);
            }
        }
        if (typeHandler == null) {
            typeHandler = DEFAULT_TYPE_HANDLER;
        }
        return typeHandler;
    }

    public long getNextSeqValue(Transaction transaction, Persistency persistency, Record record, String name) throws OdalPersistencyException {
        if (defaultKeyGenerator == null) {
            throw new NullPointerException("defaultKeyGenerator is not set");
        }
        return ((Number) defaultKeyGenerator.getNextKey(transaction, persistency, record, name)).longValue();
    }

    /**
     * Registers TypeHandler only for bind. It will be used only if default
     * type handler for read and bind is not found or the column type is not specified
     *
     * @param clazz       Class
     * @param typeHandler TypeHandler
     */
    public void registerBindTypeHandler(Class clazz, TypeHandler typeHandler) {
        typeHandlerRegistry.registerBindTypeHandler(clazz, typeHandler);
    }

    /**
     * Returns TypeHandler only for bind by given class. It will be used only if default
     * type handler for read and bind is not found or the column type is not specified
     *
     * @param clazz Class
     * @return TypeHandler only for bind. It will be used only if default
     *         type handler for read and bind is not found or the column type is not specified
     */
    public TypeHandler getBindTypeHandler(Class clazz) {
        TypeHandler typeHandler = typeHandlerRegistry.getBindTypeHandler(clazz);
        if (typeHandler == null) {
            typeHandler = DEFAULT_TYPE_HANDLER;
        }
        return typeHandler;
    }

    protected void writeLob(Transaction transaction, LobPostProcessing postProcessing) throws OdalPersistencyException {
        if (postProcessing == null || postProcessing.getPersistentObject() == null) {
            return;
        }
        PersistentObject persistentObject = postProcessing.getPersistentObject();
        Record record = persistentObject.record();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        OutputStream outputStream = null;
        Writer writer = null;
        InputStream inputStream = null;
        StringBuffer bindBuffer = new StringBuffer();
        String sql = null;
        try {
            Query query = queryFactory.newQuery();
            query.setSelect(new String[] { record.getColumn(postProcessing.getLobFieldIndex()).getColumnName() });
            query.setFrom(new String[] { record.getTableName() });
            query.addToWhere(SelectQueryBuilderImpl.whereByRecordValues(record, true));
            query.setParameters(primaryKeyValues(record));
            transaction.flush();
            statement = prepareStatement(transaction, query, bindBuffer);
            resultSet = statement.executeQuery();
            boolean hasNext = resultSet.next();
            if (hasNext) {
                Object lobObject = resultSet.getObject(1);
                if (lobObject instanceof Blob) {
                    outputStream = databasePolicy.getBinaryOutputStream((Blob) lobObject);
                    inputStream = (InputStream) postProcessing.getValue();
                    BlobImpl.writeBlob(inputStream, outputStream);
                } else if (lobObject instanceof Clob) {
                    if (postProcessing.getValue() instanceof Reader) {
                        writer = databasePolicy.getCharacterOutputStream((Clob) lobObject);
                        Reader reader = (Reader) postProcessing.getValue();
                        ClobImpl.writeClob(reader, writer);
                    } else if (postProcessing.getValue() instanceof InputStream) {
                        outputStream = databasePolicy.getAsciiOutputStream((Clob) lobObject);
                        inputStream = (InputStream) postProcessing.getValue();
                        ClobImpl.writeClob(inputStream, outputStream);
                    } else {
                        throw new OdalPersistencyException("Expected value of types Reader or InputStream, received: " + postProcessing.getValue());
                    }
                } else {
                    throw new OdalPersistencyException("This is not Blob & not Clob " + lobObject);
                }
            }
        } catch (SQLException e) {
            handleSqlException(e, sql, bindBuffer, transaction);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
            closeAll(transaction, resultSet, statement);
        }
    }

    protected void debug(String s) {
        logger.debug(s);
    }

    protected void error(String message, Exception e) {
        error(logger, message, e);
    }

    protected void warn(String message) {
        warn(logger, message);
    }

    static void error(Log logger, String message, Exception e) {
        if (logger != null) {
            logger.error(message, e);
        }
    }

    static void warn(Log logger, String message, Exception e) {
        if (logger != null) {
            logger.warn(message, e);
        }
    }

    static void warn(Log logger, String message) {
        if (logger != null) {
            logger.warn(message);
        }
    }

    protected Statement createStatement(Transaction transaction) throws OdalPersistencyException {
        try {
            return transaction.createStatement();
        } catch (SQLException e) {
            throw new OdalPersistencyException(e);
        }
    }

    protected PreparedStatement prepareStatement(Transaction transaction, String sql) throws OdalPersistencyException {
        try {
            return transaction.prepareStatement(sql);
        } catch (SQLException e) {
            throw new OdalPersistencyException(e);
        }
    }

    protected void releaseStatement(Statement statement) throws OdalPersistencyException {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            throw new OdalPersistencyException(e);
        }
    }

    /**
     * insert 1 row - adjusted for null processing
     */
    protected final int insert0(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller) throws OdalPersistencyException {
        return insert0(transaction, persistentObject, controller, false);
    }

    /**
     * insert 1 row - adjusted for null processing
     */
    protected final int insert0(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller, boolean fromUpdate) throws OdalPersistencyException {
        controller = resolveNull(controller);
        PreparedStatementWrapper preparedStatementWrapper = null;
        int rowNumber = 0;
        String sql = null;
        StringBuffer bindBuffer = new StringBuffer();
        try {
            Record record = persistentObject.record();
            if (record.isInitialized()) {
                if (fromUpdate) {
                    return 0;
                } else if (!record.isForceModify()) {
                    throw new OdalRuntimePersistencyException("Cannot insert saved record");
                }
            } else if (record.isDeleted()) {
                throw new OdalRuntimePersistencyException("Cannot insert deleted record");
            }
            try {
                controller.beforeInsert(persistentObject);
            } catch (RuntimeRuleException e) {
                throw new RecordValidationException(e);
            }
            if (record.isSkipInsertForKeysOnly() && !record.hasDirtyNonKeyFields() && !record.isForceModify()) {
                return RC_NON_DIRTY;
            }
            LobPostProcessings postProcessings = new LobPostProcessings();
            for (int i = 0; i < record.size(); i++) {
                insertAutogenerated(transaction, record, i);
            }
            if (!record.isDirty() && !record.isForceModify()) {
                return RC_NON_DIRTY;
            }
            StringBuffer insert = new StringBuffer("INSERT INTO ").append(record.getTableName()).append(" (");
            StringBuffer valuesString = new StringBuffer(") values (");
            int count = 0;
            for (int i = 0; i < record.size(); i++) {
                if (considerDirtyForInsert(record, i)) {
                    if (count > 0) {
                        insert.append(", ");
                        valuesString.append(", ");
                    }
                    count++;
                    insert.append(record.getColumn(i).getColumnName());
                    String placeHolder = resolveModifyPlaceHolder(record, i, bindNulls);
                    valuesString.append(placeHolder);
                }
            }
            insert.append(valuesString.toString()).append(")");
            sql = insert.toString();
            PreparedStatement statement = prepareStatement(transaction, sql);
            for (int i = 0, index = 0; i < record.size(); i++) {
                if (considerDirtyForInsert(record, i)) {
                    Object value = record.getObject(i);
                    index = bindParameterForModify(value, index, statement, postProcessings, persistentObject, i, bindBuffer);
                }
            }
            debug(bindBuffer.toString());
            preparedStatementWrapper = createPreparedStatementWrapper(((DatabaseTransaction) transaction), statement, sql);
            executeModify(preparedStatementWrapper, ((DatabaseTransaction) transaction), sql, bindBuffer);
            rowNumber = preparedStatementWrapper.getLastRc();
            updatePostProcessings(transaction, postProcessings);
            List primaryKey = record.getPrimaryKey();
            record.setState(State.NEW_INITIALIZING);
            for (int i = 0; i < primaryKey.size(); i++) {
                int index = ((Integer) primaryKey.get(i)).intValue();
                record.setObject(index, record.getObject(index));
            }
            databasePolicy.handlePostInsert(transaction, this, record, logger);
            resetPersistentObject(persistentObject);
            controller.afterInsert(persistentObject);
        } catch (RuntimeException e) {
            if (sql != null) {
                throw new OdalRuntimePersistencyException(e.getMessage() + ";  sql = " + sql + "; bind parameters: " + (bindBuffer == null ? "null" : bindBuffer.toString()), e);
            } else {
                throw e;
            }
        } catch (SQLException e) {
            handleSqlException(e, sql, bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, preparedStatementWrapper);
        }
        return rowNumber;
    }

    private boolean considerDirtyForInsert(Record record, int i) {
        return record.isFieldDirty(i) || record.isForceModify();
    }

    static String resolveModifyPlaceHolder(Record record, int i, boolean bindNulls) {
        Object value = record.getObject(i);
        return resolveModifyPlaceHolder(value, bindNulls);
    }

    static String resolveModifyPlaceHolder(Object value, boolean bindNulls) {
        String placeHolder = "?";
        if (!bindNulls) {
            placeHolder = value != null ? "?" : "NULL";
        }
        return placeHolder;
    }

    static LifeCycleController resolveNull(LifeCycleController controller) {
        controller = controller == null ? LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER : controller;
        return controller;
    }

    protected int insert0(Transaction transaction, PersistentObject persistentObject) throws OdalPersistencyException {
        return insert0(transaction, persistentObject, LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER);
    }

    protected int insertLocal(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller, boolean fromUpdate) throws OdalPersistencyException {
        int rc = 0;
        if (persistentObject.compound()) {
            for (int i = 0; i < persistentObject.compoundSize(); i++) {
                if (i > 0) {
                    persistentObject.populateCompoundEntries(i);
                }
                PersistentObject entry = persistentObject.compoundEntry(i);
                if (entry.compoundCascadeInsert()) {
                    int rc1 = insert0(transaction, entry, controller, fromUpdate);
                    if (i == 0) {
                        rc = rc1;
                    }
                }
            }
        } else {
            rc = insert0(transaction, persistentObject, controller, fromUpdate);
        }
        return rc;
    }

    public int insert(Transaction transaction, PersistentObject persistentObject) throws OdalPersistencyException {
        return insert(transaction, persistentObject, false);
    }

    protected int insert(Transaction transaction, PersistentObject persistentObject, boolean fromUpdate) throws OdalPersistencyException {
        return insertLocal(transaction, persistentObject, LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER, fromUpdate);
    }

    private void autogenerated(Transaction transaction, Record record, int i, boolean isInsert, boolean complexDirty) throws OdalPersistencyException {
        updateQueryBuilder.autogenerated(transaction, record, i, isInsert, complexDirty, this);
    }

    private void insertAutogenerated(Transaction transaction, Record record, int i) throws OdalPersistencyException {
        autogenerated(transaction, record, i, true, false);
    }

    public void beforeAddToUpdateBuffer(Transaction transaction, Record record, int i, boolean complexDirty) throws OdalPersistencyException {
        updateAutogenerated(transaction, record, i, complexDirty);
    }

    private void updateAutogenerated(Transaction transaction, Record record, int i, boolean complexDirty) throws OdalPersistencyException {
        autogenerated(transaction, record, i, false, complexDirty);
    }

    public void handleSqlException(SQLException e, String sql, StringBuffer bindBuffer, Transaction transaction) throws OdalPersistencyException {
        if (e != null) {
            String message = e.getMessage();
            String additionalMessage = null;
            if (sql != null) {
                additionalMessage = ";  sql = " + sql + "; bind parameters: " + (bindBuffer == null ? "null" : bindBuffer.toString()) + "; STATISTIC: " + "connection " + transaction.getConnection() + "; " + StringUtil.toString(getTransactionManager());
                message += additionalMessage;
            }
            if (databasePolicy.isDuplicate(e)) {
                throw new DuplicateRecordException(message, e);
            } else if (databasePolicy.isLocked(e)) {
                throw new LockedRecordException("Record is locked by another process", e);
            } else if (e instanceof RecordValidationException) {
                error(message, additionalMessage, e);
                throw new RecordValidationException(additionalMessage, ((RecordValidationException) e));
            } else if (e instanceof OdalPersistencyException) {
                throw ((OdalPersistencyException) e);
            } else {
                error(message, additionalMessage, e);
                throw new OdalPersistencyException(message, e);
            }
        }
    }

    private void error(String message, String additionalMessage, SQLException e) {
        if (additionalMessage != null) {
            error(message, e);
        } else {
            error("", e);
        }
    }

    public int truncate(Transaction transaction, String tableName) throws OdalPersistencyException {
        Statement statement = null;
        int rc = 0;
        String sql = null;
        StringBuffer bindBuffer = new StringBuffer();
        try {
            sql = "TRUNCATE TABLE " + tableName;
            debug("sql: " + sql);
            statement = createStatement(transaction);
            rc = statement.executeUpdate(sql);
        } catch (RuntimeException e) {
            if (sql != null) {
                throw new RuntimeException(e.getMessage() + ";  sql = " + sql + "; bind parameters: " + (bindBuffer == null ? "null" : bindBuffer.toString()), e);
            } else {
                throw e;
            }
        } catch (SQLException e) {
            handleSqlException(e, sql, bindBuffer, transaction);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    error("Cannot close statement", e);
                }
            }
        }
        return rc;
    }

    protected QueryCtl makeInlineQuery(PersistentObject persistentObject) {
        QueryCtl query;
        markPkDirtyNonPkNonDirty(persistentObject);
        PersistentObject clonedPo;
        try {
            clonedPo = (PersistentObject) persistentObject.clone();
        } catch (Exception e) {
            throw new OdalRuntimePersistencyException("Cannot clone persistentObject ", e);
        }
        clearNonInlinedChildren(clonedPo, persistentObject);
        query = (QueryCtl) queryFactory.newQueryByExample(clonedPo);
        inlineQuery(query, clonedPo);
        return query;
    }

    private void clearNonInlinedChildren(PersistentObject clonedPo, PersistentObject persistentObject) {
        clonedPo.record().clearNonInlinedChildren();
        for (LinkIterator it = persistentObject.linkIterator(); it.hasNext(); ) {
            Link child = it.nextLink();
            if (child.isInline()) {
                Query childQuery = child.getQuery();
                PersistentObjectFactory singularResultFactory = childQuery.getSingularResultFactory();
                AbstractPersistentObject abstractPersistentObject = singularResultFactory.newPersistentInstance();
                clonedPo.setChildObject(child.getName(), abstractPersistentObject);
            }
        }
    }

    private void markPkDirtyNonPkNonDirty(PersistentObject persistentObject) {
        Record record = persistentObject.record();
        for (int i = 0; i < record.size(); i++) {
            if (record.getColumn(i).isPrimaryKey()) {
                record.markFieldDirty(i);
            } else {
                record.unmarkFieldDirty(i);
            }
        }
    }

    protected Parameters toAutoParameters(PersistentObject persistentObject, boolean primaryKeyOnly) {
        if (bindNulls) {
            return persistentObject.toNotNullParameters(primaryKeyOnly);
        } else {
            return persistentObject.toParameters(primaryKeyOnly);
        }
    }

    public Object loadForUpdate(final Transaction transaction, final PersistentObject persistentObject) throws OdalPersistencyException {
        return load(transaction, persistentObject, LockType.FOR_UPDATE);
    }

    protected Collection select01(final Transaction transaction, final PersistentObject persistentObject) throws OdalPersistencyException {
        return select01(transaction, persistentObject, null);
    }

    protected Collection select01(final Transaction transaction, final PersistentObject persistentObject, LifeCycleController controller) throws OdalPersistencyException {
        return select01(transaction, persistentObject, controller, null);
    }

    protected Collection select01(final Transaction transaction, final PersistentObject persistentObject, LifeCycleController controller, LockType lock) throws OdalPersistencyException {
        return select01(transaction, persistentObject, controller, lock, NO_TIMEOUT);
    }

    protected Collection select01(final Transaction transaction, final PersistentObject persistentObject, LifeCycleController controller, LockType lock, long timeout) throws OdalPersistencyException {
        controller = resolveNull(controller);
        QueryCtl query;
        if (persistentObject.compound()) {
            query = (QueryCtl) queryFactory.newQueryByCompoundExample(persistentObject);
        } else {
            query = (QueryCtl) queryFactory.newQueryByExample(persistentObject);
        }
        if (lock != null) {
            query.setLocked(lock);
            query.setTimeout(timeout);
        }
        query.setUseColumnNames(persistentObject.usingColumnNames());
        query.setUseSelectStar(persistentObject.usingSelectStar());
        return select0(transaction, query, controller, getCurrentQueryContext());
    }

    protected void inlineQuery(Query originalQuery, Parent inlineCandidate) {
        LinkQueryBuilder.deepInlineLinkQueries(originalQuery, inlineCandidate, databasePolicy);
    }

    protected void inlineQuery(Query originalQuery) {
        inlineQuery(originalQuery, ((Parent) originalQuery));
    }

    protected Collection select0(final Transaction transaction, final Query query, final LifeCycleController controller, final QueryContext queryContext) throws OdalPersistencyException {
        if (query.isSelectConnectedForwardPageQuery()) {
            return selectQueryChunk(transaction, query, controller, queryContext);
        } else if (query.isSelectDisconnectedPageQuery()) {
            return selectPage(transaction, query, controller, queryContext);
        } else {
            return selectWithClose(transaction, ((QueryCtl) query), controller, queryContext);
        }
    }

    private Collection selectWithClose(final Transaction transaction, final QueryCtl query, LifeCycleController controller, QueryContext queryContext) throws OdalPersistencyException {
        if (query.isClosed()) {
            throw new OdalRuntimePersistencyException("Cannot re-use closed query: create a new instance");
        }
        controller = resolveNull(controller);
        inlineQuery(query);
        PreparedStatement statement = null;
        StringBuffer bindBuffer = new StringBuffer();
        QueryCtl queryToExecute;
        try {
            queryToExecute = (QueryCtl) resolveQueryToExecute(query);
            statement = prepareStatement(transaction, queryToExecute, bindBuffer);
            Collection collection = rsExecutorManager.executeSingleRsQuery(transaction, statement, queryToExecute, rsCtl, controller, queryContext);
            if (hasGeneralizedInlineQuery(query)) {
                postProcessInlineResults(query, collection, queryContext);
            }
            return collection;
        } catch (SQLException e) {
            handleSqlException(e, query.getSql(), bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, statement);
            query.close();
        }
        return null;
    }

    protected boolean hasGeneralizedInlineQuery(QueryCtl query) {
        return query.hasInlineQuery() || query.hasCombinedInlineQuery();
    }

    public void postProcessInlineResults(QueryCtl queryParent, Collection collection, QueryContext queryContext) {
        LinkQueryBuilder.distributeInlineResults(queryParent, collection, queryContext);
    }

    private Collection selectSingleRsCallWithClose(final Transaction transaction, final Call query, LifeCycleController controller) throws OdalPersistencyException {
        if (query.isClosed()) {
            throw new OdalRuntimePersistencyException("Cannot re-use closed query: create a new instance");
        }
        controller = resolveNull(controller);
        CallableStatement statement = null;
        StringBuffer bindBuffer = new StringBuffer();
        try {
            statement = prepareCall(transaction, query, bindBuffer);
            Collection collection = rsExecutorManager.executeSingleRsCall(transaction, statement, query, rsCtl, controller);
            populateCallOutParameters(query, statement);
            return collection;
        } catch (SQLException e) {
            handleSqlException(e, query.getSql(), bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, statement);
            query.close();
        }
        return null;
    }

    private MultipartCollection selectMultiRsCallWithClose(final Transaction transaction, final Call query, LifeCycleController controller) throws OdalPersistencyException {
        if (query.isClosed()) {
            throw new OdalRuntimePersistencyException("Cannot re-use closed query: create a new instance");
        }
        controller = resolveNull(controller);
        CallableStatement statement = null;
        StringBuffer bindBuffer = new StringBuffer();
        try {
            statement = prepareCall(transaction, query, bindBuffer);
            MultipartCollection collection = rsExecutorManager.executeMultiRsCall(transaction, statement, query, rsCtl, controller);
            populateCallOutParameters(query, statement);
            return collection;
        } catch (SQLException e) {
            handleSqlException(e, query.getSql(), bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, statement);
            query.close();
        }
        return null;
    }

    public MultipartCollection selectMultiPartResultCall(final Transaction transaction, final Call query, final LifeCycleController controller) throws OdalPersistencyException {
        if (query.isSelectConnectedForwardPageQuery()) {
            throw new OdalPersistencyException("Select chunk query cannot be retrieved with this method");
        } else {
            return selectMultiRsCallWithClose(transaction, query, controller);
        }
    }

    public Collection selectSinglePartResultCall(final Transaction transaction, final Call query, final LifeCycleController controller) throws OdalPersistencyException {
        if (query.isSelectConnectedForwardPageQuery()) {
            return selectCallChunk(transaction, query, controller);
        } else {
            return selectSingleRsCallWithClose(transaction, query, controller);
        }
    }

    private Collection selectCallChunk(final Transaction transaction, final Call query, LifeCycleController controller) throws OdalPersistencyException {
        Collection collection = null;
        CallableStatement statement = null;
        StringBuffer bindBuffer = new StringBuffer();
        if (!query.isSelectConnectedForwardPageQuery()) {
            throw new OdalRuntimePersistencyException("This is not a SelectChunkQuery");
        }
        controller = resolveNull(controller);
        try {
            DatabaseTransactionImpl databaseTransaction = (DatabaseTransactionImpl) transaction;
            if (query.getState() == BasicQuery.STATE_NEW) {
                statement = prepareCall(transaction, query, bindBuffer);
                PreparedStatementWrapper statementWrapper = new PreparedStatementWrapper(statement, transaction);
                databaseTransaction.addMultiPageStatement(statementWrapper);
                query.setStatementWrapper(statementWrapper);
            }
            collection = executeCallFetch(databaseTransaction, query, controller);
            populateCallOutParameters(query, statement);
        } catch (SQLException e) {
            handleSqlException(e, query.getSql(), bindBuffer, transaction);
        } finally {
            if (query.getRetrievedCount() == 0) {
                query.close();
            }
        }
        return collection;
    }

    void populateCallOutParameters(Call query, CallableStatement statement) throws SQLException {
        if (query.getParameters() != null) {
            CallParameters parameters = query.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                CallParameter parameter = (CallParameter) parameters.get(i);
                int index = i + 1;
                if (parameter.isOut() && !parameter.isSkipBackPopulation()) {
                    ColumnType columnType = parameter.getType();
                    TypeHandler typeHandler = getTypeHandler(columnType);
                    Object value = typeHandler.handleOutParamRead(statement, index, databasePolicy);
                    if (parameter.getValue() == null) {
                        parameter.setOutValue(value);
                    }
                }
            }
        }
    }

    protected static Query resolveQueryToExecute(final QueryCtl query) {
        return query.resolveQueryToExecute();
    }

    public PreparedStatement prepareStatement(final Transaction transaction, final Query query, StringBuffer bindBuffer) throws OdalPersistencyException {
        String sql = query.compile(databasePolicy).getSql();
        PreparedStatement statement = prepareStatement(transaction, sql);
        bindValues(query.getParameters(), statement, bindBuffer);
        debug(bindBuffer.toString());
        return statement;
    }

    /**
     * Executes stored procedure call that doe not return result set
     *
     * @param transaction transaction
     * @param call        Call object
     * @throws SQLException
     */
    public void executeCall(final Transaction transaction, final Call call) throws SQLException {
        StringBuffer bindBuffer = new StringBuffer();
        CallableStatement statement = null;
        String sql = call.getSql();
        try {
            statement = prepareCall(transaction, call, bindBuffer);
            statement.execute();
        } catch (SQLException e) {
            handleSqlException(e, sql, bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, statement);
        }
    }

    public CallableStatement prepareCall(final Transaction transaction, final Call query, StringBuffer bindBuffer) throws SQLException {
        String sql = query.compile(databasePolicy).getSql();
        debug("sql: " + sql);
        CallableStatement statement = transaction.prepareCall(sql);
        CallParameters parameters = query.getParameters();
        bindCallValues(parameters, statement, bindBuffer);
        debug(bindBuffer.toString());
        return statement;
    }

    protected PreparedStatementWrapper createPreparedStatementWrapper(final DatabaseTransaction transaction, final PreparedStatement statement, final String sql) {
        PreparedStatementWrapper preparedStatementWrapper;
        if (transaction.isUseBatchModify()) {
            preparedStatementWrapper = transaction.prepareBatchModifyStatement(statement, this, sql);
        } else {
            preparedStatementWrapper = new PreparedStatementWrapper(statement, transaction);
        }
        return preparedStatementWrapper;
    }

    protected void executeModify(PreparedStatementWrapper preparedStatementWrapper, final DatabaseTransaction transaction, final String sql, final StringBuffer bindBuffer) throws SQLException {
        if (transaction.isUseBatchModify()) {
            preparedStatementWrapper.setSql(sql);
            preparedStatementWrapper.setBindBuffer(bindBuffer);
            preparedStatementWrapper.setBatchModify(true);
            transaction.addModifyStatement(preparedStatementWrapper);
        } else {
            preparedStatementWrapper.executeUpdate();
        }
    }

    /**
     *
     * @param transaction
     * @param persistentObject
     * @param extraWhere
     * @param extraParameters
     * @param controller
     * @param complexDirty - when true even if record is not dirty update will proceed since some child records may be
     * dirty or collections got modified
     * @return
     * @throws OdalPersistencyException
     */
    protected final int update0(final Transaction transaction, PersistentObject persistentObject, String extraWhere, Parameters extraParameters, LifeCycleController controller, boolean complexDirty) throws OdalPersistencyException {
        controller = resolveNull(controller);
        PreparedStatementWrapper preparedStatementWrapper = null;
        int rowNumber = 0;
        String sql = null;
        StringBuffer bindBuffer = new StringBuffer();
        try {
            validatePkStrict(persistentObject);
            Record record = persistentObject.record();
            if (record.isDeleted()) {
                throw new OdalRuntimePersistencyException("Cannot update deleted record");
            } else if (record.getPrimaryKey().size() == 0) {
                throw new OdalRuntimePersistencyException("Cannot update record in table " + record.getTableName() + " since it has no primary key");
            }
            if (nonDirtyForUpdate(record) && !complexDirty) {
                return RC_NON_DIRTY;
            }
            if (record.getOptLockKey().size() > 0 && !record.isInitialized()) {
                throw new OdalPersistencyException("Cannot update record with optimistic lock fields " + "and not coming from or saved in database");
            }
            try {
                controller.beforeUpdate(persistentObject);
            } catch (RuntimeRuleException e) {
                throw new RecordValidationException(e);
            }
            LobPostProcessings postProcessings = new LobPostProcessings();
            StringBuffer update = getUpdateSqlHeader(record.getTableName());
            int countUpdated = 0;
            for (int i = 0; i < record.size(); i++) {
                countUpdated = updateQueryBuilder.addToHeaderUpdateBuffer(transaction, countUpdated, update, record, i, supportKeyUpdate, getLogger(), complexDirty, this);
            }
            if (countUpdated == 0) {
                return RC_NON_DIRTY;
            }
            update.append(whereByKeyValues(persistentObject));
            if (extraWhere != null) {
                update.append(" ").append(extraWhere);
            }
            sql = update.toString();
            PreparedStatement statement = prepareStatement(transaction, sql);
            int index = bindUpdateSet(persistentObject, statement, postProcessings, bindBuffer);
            try {
                index = bindValues(index, primaryKeyValues(record), statement, bindBuffer);
                index = bindValues(index, optLockKeyValues(record), statement, bindBuffer);
                bindValues(index, extraParameters, statement, bindBuffer);
            } catch (RecordValidationException e) {
                String message = e.getMessage() + "; record: " + record;
                throw new RecordValidationException(message, e);
            }
            debug("bindBuffer = " + bindBuffer);
            preparedStatementWrapper = createPreparedStatementWrapper(((DatabaseTransaction) transaction), statement, sql);
            executeModify(preparedStatementWrapper, ((DatabaseTransaction) transaction), sql, bindBuffer);
            rowNumber = preparedStatementWrapper.getLastRc();
            updatePostProcessings(transaction, postProcessings);
            resetPersistentObject(persistentObject);
            controller.afterUpdate(persistentObject);
        } catch (SQLException e) {
            handleSqlException(e, sql, bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, preparedStatementWrapper);
        }
        return rowNumber;
    }

    private boolean nonDirtyForUpdate(Record record) {
        return !record.isDirty() && !record.isForceModify();
    }

    protected final int update0(final Transaction transaction, PersistentObject persistentObject, String extraWhere, Object[] extraParameters, LifeCycleController controller, boolean bypassDirtyCheck) throws OdalPersistencyException {
        Parameters extras = elaborateParameters(extraParameters);
        return update0(transaction, persistentObject, extraWhere, extras, controller, bypassDirtyCheck);
    }

    public static Parameters elaborateParameters(Object[] extraParameters) {
        return extraParameters == null ? null : new Parameters(extraParameters);
    }

    private void resetPersistentObject(PersistentObject persistentObject) {
        persistentObject.toBeanFields();
        persistentObject.record().resetAfterModify();
    }

    protected final int update0(final Transaction transaction, PersistentObject persistentObject, String extraWhere, boolean bypassDirtyCheck) throws OdalPersistencyException {
        return update0(transaction, persistentObject, extraWhere, ((Parameters) null), null, bypassDirtyCheck);
    }

    /**
     * @param transaction
     * @param persistentObject
     * @param controller
     * @return number of affected records
     * @throws OdalPersistencyException
     */
    protected final int update0(final Transaction transaction, PersistentObject persistentObject, LifeCycleController controller, boolean bypassDirtyCheck) throws OdalPersistencyException {
        return update0(transaction, persistentObject, ((String) null), ((Parameters) null), controller, bypassDirtyCheck);
    }

    private StringBuffer whereByKeyValues(PersistentObject persistentObject) {
        Record record = persistentObject.record();
        StringBuffer where = new StringBuffer();
        int whereIndex = 0;
        whereIndex = SelectQueryBuilderImpl.whereByKeyValues(whereIndex, where, record.getPrimaryKey(), record, true);
        SelectQueryBuilderImpl.whereByKeyValues(whereIndex, where, record.getOptLockKey(), record, false);
        return where;
    }

    protected final int update0(final Transaction transaction, PersistentObject persistentObject, boolean bypassDirtyCheck) throws OdalPersistencyException {
        return update0(transaction, persistentObject, LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER, bypassDirtyCheck);
    }

    private int updateLocal(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller, boolean bypassDirtyCheck) throws OdalPersistencyException {
        int rc = 0;
        if (persistentObject.compound()) {
            for (int i = 0; i < persistentObject.compoundSize(); i++) {
                int rc1 = update0(transaction, persistentObject.compoundEntry(i), controller, bypassDirtyCheck);
                if (i == 0) {
                    persistentObject.populateCompoundEntries();
                    rc = rc1;
                }
            }
        } else {
            rc = update0(transaction, persistentObject, controller, bypassDirtyCheck);
        }
        return rc;
    }

    public int update(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller, boolean bypassDirtyCheck) throws OdalPersistencyException {
        return updateLocal(transaction, persistentObject, controller, bypassDirtyCheck);
    }

    public int update(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller) throws OdalPersistencyException {
        return update(transaction, persistentObject, controller, false);
    }

    protected int update(Transaction transaction, PersistentObject persistentObject, boolean bypassDirtyCheck) throws OdalPersistencyException {
        return updateLocal(transaction, persistentObject, LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER, bypassDirtyCheck);
    }

    public int update(Transaction transaction, PersistentObject persistentObject) throws OdalPersistencyException {
        return update(transaction, persistentObject, false);
    }

    public int update(Transaction transaction, PersistentObject persistentObject, String extraWhere, Object[] extraParameters, LifeCycleController controller) throws OdalPersistencyException {
        return update0(transaction, persistentObject, extraWhere, extraParameters, controller, false);
    }

    public int update(Transaction transaction, PersistentObject persistentObject, String extraWhere, Parameters extraParameters, LifeCycleController controller) throws OdalPersistencyException {
        return update0(transaction, persistentObject, extraWhere, extraParameters, controller, false);
    }

    public int[] update(Transaction transaction, Collection collection) throws OdalPersistencyException {
        return update(transaction, collection, LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER);
    }

    public int[] update(Transaction transaction, TracingCollection collection, Collection source, LifeCycleController lifeCycleController) throws OdalPersistencyException {
        collection.trace(source);
        return update(transaction, collection, lifeCycleController);
    }

    /**
     * @param transaction
     * @param collection
     * @param lifeCycleController
     * @return array of summary result codes: [0] - Sum(inserted); [1] - Sum(deleted); [2] - Sum(updated)
     * @throws OdalPersistencyException
     */
    public int[] update(Transaction transaction, Collection collection, LifeCycleController lifeCycleController) throws OdalPersistencyException {
        int[] rcs = { -1, -1, -1 };
        if (collection == null) {
            return null;
        } else {
            if (collection instanceof TracingCollection) {
                HashSet seen = new HashSet();
                TracingCollection tracingCollection = (TracingCollection) collection;
                List inserted = tracingCollection.getInserted();
                for (Iterator iterator = inserted.iterator(); iterator.hasNext(); ) {
                    PersistentObject persistentObject = (PersistentObject) iterator.next();
                    int rc = insertLocal(persistentObject, lifeCycleController, true);
                    rcs[0] += (rc <= 0 ? 0 : rc);
                }
                List deleted = tracingCollection.getDeleted();
                for (Iterator iterator = deleted.iterator(); iterator.hasNext(); ) {
                    PersistentObject persistentObject = (PersistentObject) iterator.next();
                    int rc = delete(persistentObject, lifeCycleController);
                    rcs[1] += (rc <= 0 ? 0 : rc);
                }
                List updated = tracingCollection.getUpdated();
                for (Iterator iterator = updated.iterator(); iterator.hasNext(); ) {
                    PersistentObject persistentObject = (PersistentObject) iterator.next();
                    int rc = update(persistentObject, lifeCycleController);
                    rcs[2] += (rc <= 0 ? 0 : rc);
                }
                seen.addAll(inserted);
                seen.addAll(deleted);
                seen.addAll(updated);
                for (Iterator it = collection.iterator(); it.hasNext(); ) {
                    PersistentObject persistentObject = (PersistentObject) it.next();
                    if (!seen.contains(persistentObject)) {
                        int rc = update(transaction, persistentObject, lifeCycleController);
                        rcs[2] += (rc <= 0 ? 0 : rc);
                    }
                }
                tracingCollection.clearTrace();
            } else {
                if (collection.isEmpty()) {
                    return rcs;
                }
                for (Iterator it = collection.iterator(); it.hasNext(); ) {
                    PersistentObject persistentObject = (PersistentObject) it.next();
                    if (!persistentObject.record().isDeleted()) {
                        int rc = update(transaction, persistentObject, lifeCycleController);
                        rcs[2] += (rc <= 0 ? 0 : rc);
                    }
                }
            }
            return rcs;
        }
    }

    public int update(Transaction transaction, UpdateQuery updateQuery) throws OdalPersistencyException {
        UpdateQueryImpl query = (UpdateQueryImpl) updateQuery;
        updateQuery.compile(databasePolicy);
        return executeUpdate(transaction, query.getSql(), query.getParameters());
    }

    public int executeUpdate(Transaction transaction, String sql, Parameters parameters) throws OdalPersistencyException {
        PreparedStatement statement = null;
        StringBuffer bindBuffer = new StringBuffer();
        int rc = 0;
        try {
            statement = prepareStatement(transaction, sql);
            bindValues(parameters, statement, bindBuffer);
            debug(bindBuffer.toString());
            rc = statement.executeUpdate();
        } catch (SQLException e) {
            handleSqlException(e, sql, bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, statement);
        }
        return rc;
    }

    public int executeUpdate(Transaction transaction, String sql) throws OdalPersistencyException {
        return executeUpdate(transaction, sql, null);
    }

    public int deleteByPk(Transaction transaction, PersistentObject persistentObject) throws OdalPersistencyException {
        return deleteByPk(transaction, persistentObject, LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER);
    }

    public int deleteByPk(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller) throws OdalPersistencyException {
        PreparedStatementWrapper preparedStatementWrapper = null;
        int rowNumber = 0;
        String sql = null;
        StringBuffer bindBuffer = new StringBuffer();
        try {
            validatePkStrict(persistentObject);
            Record record = persistentObject.record();
            if (record.isDeleted()) {
                throw new OdalRuntimePersistencyException("Cannot delete deleted record");
            } else if (record.getPrimaryKey().size() == 0) {
                throw new OdalRuntimePersistencyException("Cannot delete record " + record.getTableName() + " since it has no primary key");
            }
            StringBuffer delete = new StringBuffer("DELETE FROM ").append(record.getTableName()).append(SelectQueryBuilderImpl.whereByRecordValues(record, true, true));
            sql = delete.toString();
            PreparedStatement statement = prepareStatement(transaction, sql);
            Parameters parameters = primaryKeyValues(record);
            bindValues(0, parameters, statement, bindBuffer);
            debug(bindBuffer.toString());
            preparedStatementWrapper = createPreparedStatementWrapper(((DatabaseTransaction) transaction), statement, sql);
            executeModify(preparedStatementWrapper, ((DatabaseTransaction) transaction), sql, bindBuffer);
            rowNumber = preparedStatementWrapper.getLastRc();
            record.setState(State.DELETED);
        } catch (SQLException e) {
            handleSqlException(e, sql, bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, preparedStatementWrapper);
        }
        return rowNumber;
    }

    public final int deleteByExample(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller) throws OdalPersistencyException {
        return deleteByExample(transaction, persistentObject, controller, 0);
    }

    public final int deleteByExample(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller, int limit) throws OdalPersistencyException {
        PreparedStatement statement = null;
        int rowNumber = 0;
        String sql = null;
        StringBuffer bindBuffer = new StringBuffer();
        try {
            Record record = persistentObject.record();
            if (record.isDeleted()) {
                throw new OdalRuntimePersistencyException("Cannot delete deleted record");
            } else if (record.getPrimaryKey().size() == 0) {
                throw new OdalRuntimePersistencyException("Cannot delete record in table " + record.getTableName() + " since it has no primary key");
            }
            boolean primaryKeyOnly = false;
            String where = SelectQueryBuilderImpl.whereByRecordValues(record, primaryKeyOnly, true);
            Parameters parameters = toAutoParameters(persistentObject, primaryKeyOnly);
            StringBuffer delete = new StringBuffer("DELETE FROM ").append(record.getTableName()).append(where);
            if (limit > 0) {
                String limitWhere = where == null ? Query.WHERE : Query.AND;
                String limitClause = databasePolicy.getLimitSql(limit);
                delete.append(limitWhere).append(limitClause);
            }
            sql = delete.toString();
            statement = prepareStatement(transaction, sql);
            bindValues(0, parameters, statement, bindBuffer);
            debug(bindBuffer.toString());
            rowNumber = statement.executeUpdate();
            record.setState(State.DELETED);
        } catch (SQLException e) {
            handleSqlException(e, sql, bindBuffer, transaction);
        } finally {
            closeAll(transaction, null, statement);
        }
        return rowNumber;
    }

    public final int deleteByExample(Transaction transaction, DeleteQuery deleteQuery) throws OdalPersistencyException {
        String sql = deleteQuery.compile(databasePolicy).getSql();
        Parameters parameters = deleteQuery.getParameters();
        return executeUpdate(sql, parameters);
    }

    public final int deleteByExample(Transaction transaction, PersistentObject persistentObject) throws OdalPersistencyException {
        return deleteByExample(transaction, persistentObject, LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER);
    }

    private int deleteLocal(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller) throws OdalPersistencyException {
        int rc = 0;
        if (persistentObject.compound()) {
            for (int i = 0; i < persistentObject.compoundSize(); i++) {
                int k = persistentObject.compoundSize() - i - 1;
                int rc1 = deleteByPk(transaction, persistentObject.compoundEntry(k), controller);
                if (k == 0) {
                    rc = rc1;
                }
            }
        } else {
            rc = deleteByPk(transaction, persistentObject, controller);
        }
        return rc;
    }

    public int delete(Transaction transaction, PersistentObject persistentObject, LifeCycleController controller) throws OdalPersistencyException {
        return deleteLocal(transaction, persistentObject, controller);
    }

    public int delete(Transaction transaction, PersistentObject persistentObject) throws OdalPersistencyException {
        return deleteLocal(transaction, persistentObject, LifeCycleController.NULL_LIFE_CYCLE_CONTROLLER);
    }

    protected int bindUpdateSet(PersistentObject persistentObject, PreparedStatement statement, LobPostProcessings postProcessings, StringBuffer bindBuffer) throws SQLException {
        int index = 0;
        Record record = persistentObject.record();
        for (int i = 0; i < record.size(); i++) {
            if (considerDirtyForUpdate(record, i)) {
                Object value = record.getObject(i);
                index = bindParameterForModify(value, index, statement, postProcessings, persistentObject, i, bindBuffer);
            }
        }
        return index;
    }

    private boolean considerDirtyForUpdate(Record record, int i) throws OdalPersistencyException {
        return updateQueryBuilder.considerDirtyForUpdate(record, i, supportKeyUpdate, logger);
    }

    public boolean selectExists(final Transaction transaction, final PersistentObject persistentObject) throws OdalPersistencyException {
        Query query;
        if (persistentObject.compound()) {
            query = getQueryFactory().newQueryByCompoundExample(persistentObject);
        } else {
            query = getQueryFactory().newQuery(persistentObject);
            query.addToWhere(SelectQueryBuilderImpl.whereByRecordValues(persistentObject.record(), false, false));
            boolean primaryKeyOnly = false;
            Parameters parameters = toAutoParameters(persistentObject, primaryKeyOnly);
            query.setParameters(parameters);
        }
        return selectExists(transaction, query);
    }

    public boolean selectExists(final Transaction transaction, final Query query) throws OdalPersistencyException {
        Query queryOut = cloneQuery(query);
        String sql = selectQueryBuilder.getSqlExists(query, databasePolicy);
        queryOut.setSql(sql);
        Number value = (Number) selectSingle(transaction, queryOut);
        return value != null && value.longValue() > 0;
    }

    private Query cloneQuery(final Query query) {
        try {
            return (Query) query.clone();
        } catch (CloneNotSupportedException e) {
            throw new OdalRuntimeException("<<BUG>>: CloneNotSupportedException is caught", e);
        }
    }

    public long selectCount(final Transaction transaction, final Query query) throws OdalPersistencyException {
        Query queryOut = cloneQuery(query);
        String sql = selectQueryBuilder.getSqlCount(query, databasePolicy);
        queryOut.setSql(sql);
        ((QueryCtl) queryOut).setCount(true);
        Number value = (Number) selectSingle(transaction, queryOut);
        return value == null ? 0 : value.longValue();
    }

    public long selectCount(final Transaction transaction, final PersistentObject persistentObject) throws OdalPersistencyException {
        Query query;
        if (persistentObject.compound()) {
            query = getQueryFactory().newQueryByCompoundExample(persistentObject);
        } else {
            query = getQueryFactory().newQuery(persistentObject);
            query.addToWhere(SelectQueryBuilderImpl.whereByRecordValues(persistentObject.record(), false, false));
            boolean primaryKeyOnly = false;
            Parameters parameters = toAutoParameters(persistentObject, primaryKeyOnly);
            query.setParameters(parameters);
        }
        return selectCount(transaction, query);
    }

    public Object selectSingle(final Transaction transaction, final Query query) throws OdalPersistencyException {
        query.compile(databasePolicy);
        query.nullifySingularResultFactory();
        PersistentObject persistentObject = (PersistentObject) selectFirst(transaction, query);
        return persistentObject == null ? null : persistentObject.record().getObject(0);
    }

    private Parameters primaryKeyValues(Record record) {
        return updateQueryBuilder.primaryKeyValues(record);
    }

    private Parameters optLockKeyValues(Record record) {
        return updateQueryBuilder.optLockKeyValues(record);
    }

    private StringBuffer getUpdateSqlHeader(String tableName) {
        return updateQueryBuilder.getUpdateSqlHeader(tableName);
    }

    private void updatePostProcessings(Transaction transaction, LobPostProcessings postProcessings) throws OdalPersistencyException {
        if (postProcessings != null) {
            for (int i = 0; i < postProcessings.size(); i++) {
                LobPostProcessing pp = postProcessings.get(i);
                writeLob(transaction, pp);
            }
        }
    }

    protected void validatePkStrict(PersistentObject persistentObject) throws RecordValidationException {
        List primaryKey = persistentObject.record().getPrimaryKey();
        if (primaryKey.size() == 0) {
            throw new RecordValidationException("Primary key is required for the operation while primary key size is 0");
        }
        for (int i = 0; i < primaryKey.size(); i++) {
            int index = ((Integer) primaryKey.get(i)).intValue();
            if (persistentObject.record().getObject(index) == null) {
                throw new RecordValidationException("Primary key is required for the operation while value of [" + persistentObject.record().getColumn(index).getColumnName() + "] is null");
            }
        }
    }

    private void bindCallValues(CallParameters parameters, CallableStatement statement, StringBuffer bindBuffer) throws SQLException {
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                int index = i + 1;
                CallParameter parameter = (CallParameter) parameters.get(i);
                if (parameter.isIn()) {
                    bindCallParameter(index, parameter, statement, bindBuffer);
                }
                if (parameter.isOut()) {
                    String jdbcTypeName = parameter.getJdbcTypeName(databasePolicy);
                    int jdbcType = parameter.getJdbcType(databasePolicy);
                    bindBuffer.append("; registered OUT param [").append(index).append("] of jdbcType = [").append(jdbcType).append("]");
                    if (jdbcTypeName != null && jdbcType != 0) {
                        statement.registerOutParameter(index, jdbcType, jdbcTypeName);
                    } else if (parameter.getJdbcScale() >= 0) {
                        statement.registerOutParameter(index, jdbcType, parameter.getJdbcScale());
                    } else {
                        statement.registerOutParameter(index, jdbcType);
                    }
                }
            }
        }
    }

    private void bindValues(Parameters parameters, PreparedStatement statement, StringBuffer bindBuffer) throws OdalPersistencyException {
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                bindParameter(i + 1, parameters.get(i), statement, bindBuffer);
            }
        }
    }

    private int bindValues(int index, Parameters parameters, PreparedStatement statement, StringBuffer bindBuffer) throws OdalPersistencyException {
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                Parameter parameter = parameters.get(i);
                if (parameter != null) {
                    index += 1;
                    bindParameter(index, parameter.getType(), parameter.getValue(), statement, bindBuffer);
                } else {
                    throw new RecordValidationException("Key parameter is null for update at index [" + index + "]");
                }
            }
        }
        return index;
    }

    private void bindParameter(int jdbcColIndex, Parameter parameter, PreparedStatement statement, StringBuffer bindBuffer) throws OdalPersistencyException {
        ColumnType columnType = parameter == null ? null : parameter.getType();
        Object value = parameter == null ? null : parameter.getValue();
        bindParameter(jdbcColIndex, columnType, value, statement, bindBuffer);
    }

    private void bindParameter(int jdbcColIndex, ColumnType columnType, Object value, PreparedStatement statement, StringBuffer bindBuffer) throws OdalPersistencyException {
        bindParameter0(jdbcColIndex, columnType, value, statement, bindBuffer);
    }

    private void bindParameter0(int jdbcColIndex, ColumnType columnType, Object value, PreparedStatement statement, StringBuffer bindBuffer) throws OdalPersistencyException {
        try {
            TypeHandler typeHandler = getBindTypeHandler(columnType, value);
            appendToBindBuffer(bindBuffer, jdbcColIndex, value);
            if (value != null) {
                typeHandler.handleBind(statement, jdbcColIndex, value);
            } else if (this.bindNulls) {
                typeHandler.handleBindNull(statement, jdbcColIndex, columnType, "Set null for parameter of index = " + jdbcColIndex);
            }
        } catch (OdalPersistencyException e) {
            throw e;
        } catch (SQLException e) {
            String message = "Error while binding parameter with index [" + jdbcColIndex + "], value [" + value + "]";
            throw new OdalPersistencyException(message, e);
        }
    }

    private void bindCallParameter(int index, Parameter parameter, CallableStatement statement, StringBuffer bindBuffer) throws OdalPersistencyException {
        ColumnType columnType = parameter == null ? null : parameter.getType();
        Object value = parameter == null ? null : parameter.getValue();
        bindParameter(index, columnType, value, statement, bindBuffer);
    }

    protected TypeHandler getBindTypeHandler(ColumnType columnType, Object value) {
        TypeHandler typeHandler = null;
        if (columnType != null) {
            typeHandler = getTypeHandler(columnType);
        } else if (value != null) {
            typeHandler = getBindTypeHandler(value.getClass());
            if (typeHandler == null) {
                if (value instanceof Date) {
                    typeHandler = getBindTypeHandler(Date.class);
                } else if (value instanceof InputStream) {
                    typeHandler = getBindTypeHandler(InputStream.class);
                }
            }
        }
        if (typeHandler == null) {
            typeHandler = DEFAULT_TYPE_HANDLER;
        }
        return typeHandler;
    }

    /**
     * Binds parameter for insert & update (header portion)
     *
     * @param value
     * @param index
     * @param statement
     * @param postProcessings
     * @param persistentObject
     * @param fieldIndex
     * @param bindBuffer
     * @return
     * @throws OdalPersistencyException
     */
    private int bindParameterForModify(Object value, int index, PreparedStatement statement, LobPostProcessings postProcessings, PersistentObject persistentObject, int fieldIndex, StringBuffer bindBuffer) throws SQLException {
        MetaColumn column = persistentObject.record().getColumn(fieldIndex);
        ColumnType columnType = column.getType();
        TypeHandler typeHandler = getBindTypeHandler(columnType, value);
        if (value != null) {
            if (columnType == ColumnType.CLOB_STRING) {
                value = new ClobImpl(((String) value));
            }
            if (typeHandler != null) {
                ++index;
                appendToBindBuffer(bindBuffer, index, value);
                typeHandler.handleBind(statement, index, value, databasePolicy, postProcessings, persistentObject, fieldIndex);
            }
        } else if (bindNulls) {
            ++index;
            appendToBindBuffer(bindBuffer, index, value);
            typeHandler.handleBindNull(statement, index, columnType, column.getColumnName());
        }
        return index;
    }

    private void appendToBindBuffer(StringBuffer bindBuffer, int index, Object parameter) {
        bindBuffer.append("; parameter[").append(index).append("] = [").append(parameter).append("] -> parameter Class = ").append((parameter == null ? "null" : parameter.getClass().getName()));
    }

    public Collection selectQueryChunk(final Transaction transaction, final Query aquery, final LifeCycleController controller, final QueryContext queryContext) throws OdalPersistencyException {
        Collection collection = null;
        PreparedStatement statement;
        StringBuffer bindBuffer = new StringBuffer();
        QueryCtl query = (QueryCtl) aquery;
        if (!query.isSelectConnectedForwardPageQuery()) {
            throw new OdalRuntimePersistencyException("This is not a 'SelectConnectedForwardPage' Query");
        }
        try {
            DatabaseTransactionImpl databaseTransaction = (DatabaseTransactionImpl) transaction;
            if (query.getState() == BasicQuery.STATE_NEW) {
                statement = prepareStatement(transaction, query, bindBuffer);
                PreparedStatementWrapper statementWrapper = new PreparedStatementWrapper(statement, transaction);
                databaseTransaction.addMultiPageStatement(statementWrapper);
                query.setStatementWrapper(statementWrapper);
            }
            collection = executeQueryFetch(databaseTransaction, query, controller, queryContext);
        } catch (SQLException e) {
            handleSqlException(e, query.getSql(), bindBuffer, transaction);
        } finally {
            if (query.getRetrievedCount() < query.getPageSize()) {
                query.close();
            }
        }
        return collection;
    }

    public Collection selectPage(final Transaction transaction, final Query aquery, final LifeCycleController controller, QueryContext queryContext) throws OdalPersistencyException {
        Query query = databasePolicy.getPaginatedWrapperQuery(queryFactory, aquery);
        return selectWithClose(transaction, ((QueryCtl) query), controller, queryContext);
    }

    private Collection executeQueryFetch(DatabaseTransactionImpl transaction, ResultableQueryManager query, LifeCycleController controller, QueryContext queryContext) throws SQLException {
        return rsExecutorManager.executeQueryFetch(transaction, query, rsCtl, controller, queryContext);
    }

    private Collection executeCallFetch(DatabaseTransactionImpl transaction, ResultableQueryManager query, LifeCycleController controller) throws SQLException {
        return rsExecutorManager.executeCallFetch(transaction, query, rsCtl, controller);
    }

    void closeAll(Transaction transaction, ResultSet resultSet, PreparedStatement stmt) {
        closeRs(resultSet, logger);
        if (stmt != null) {
            try {
                releaseStatement(stmt);
            } catch (SQLException e) {
                error("Cannot release statement", e);
            }
        }
    }

    static void closeRs(ResultSet resultSet, Log logger) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                error(logger, "Cannot close result set.", e);
            }
        }
    }

    private void closeAll(Transaction transaction, ResultSet resultSet, PreparedStatementWrapper stmt) throws OdalPersistencyException {
        closeRs(resultSet, logger);
        closeStmt(stmt, transaction);
    }

    private void closeStmt(PreparedStatementWrapper stmt, Transaction transaction) throws OdalPersistencyException {
        if (stmt != null && stmt.getStatement() != null) {
            if (transaction != null) {
                DatabaseTransaction databaseTransaction = (DatabaseTransaction) transaction;
                if (!databaseTransaction.isUseBatchModify()) {
                    releaseStatement(stmt.getStatement());
                }
            }
        }
    }

    public int[] insert(Transaction transaction, Collection persistentObjects) throws OdalPersistencyException {
        return insert(transaction, persistentObjects, false);
    }

    protected int[] insert(Transaction transaction, Collection persistentObjects, boolean fromUpdate) throws OdalPersistencyException {
        if (persistentObjects == null) {
            return null;
        } else if (persistentObjects.isEmpty()) {
            return new int[0];
        } else {
            int[] rcs = new int[persistentObjects.size()];
            int i = 0;
            for (Iterator it = persistentObjects.iterator(); it.hasNext(); i++) {
                PersistentObject persistentObject = (PersistentObject) it.next();
                rcs[i] = insert(transaction, persistentObject, fromUpdate);
            }
            return rcs;
        }
    }

    public int[] delete(Transaction transaction, Collection persistentObjects) throws OdalPersistencyException {
        if (persistentObjects == null) {
            return null;
        } else if (persistentObjects.isEmpty()) {
            return new int[0];
        } else {
            int[] rcs = new int[persistentObjects.size()];
            int i = 0;
            for (Iterator it = persistentObjects.iterator(); it.hasNext(); i++) {
                PersistentObject persistentObject = (PersistentObject) it.next();
                rcs[i] = delete(transaction, persistentObject);
            }
            return rcs;
        }
    }

    public ResultSetIterator resultSetIterator(PreparedStatement statement, AbstractParameters parameters) throws SQLException {
        return rsExecutorManager.resultSetIterator(statement, parameters);
    }

    public Object load(PersistentObject persistentObject) throws OdalPersistencyException {
        return load(getCurrentTransaction(), persistentObject);
    }

    public Object load(PersistentObject persistentObject, LifeCycleController controller) throws OdalPersistencyException {
        return load(getCurrentTransaction(), persistentObject, controller);
    }

    public Object load(PersistentObject persistentObject, LockType lockType) throws OdalPersistencyException {
        return load(getCurrentTransaction(), persistentObject, lockType);
    }

    public Object load(PersistentObject persistentObject, LockType lockType, long timeout) throws OdalPersistencyException {
        return load(getCurrentTransaction(), persistentObject, lockType, timeout);
    }

    public Object load(PersistentObject persistentObject, LifeCycleController controller, LockType lockType, long timeout) throws OdalPersistencyException {
        return load(getCurrentTransaction(), persistentObject, controller, lockType, timeout);
    }

    public Object loadForUpdate(PersistentObject persistentObject) throws OdalPersistencyException {
        return loadForUpdate(getCurrentTransaction(), persistentObject);
    }

    protected Transaction getCurrentTransaction() {
        return getTransactionManager().getCurrentTransaction();
    }

    BasicDatabasePersistencyImpl persistency() {
        return this;
    }

    public void setDatabasePolicy(DatabasePolicy databasePolicy) {
        this.databasePolicy = databasePolicy;
    }

    public DatabasePolicy getDatabasePolicy() {
        return databasePolicy;
    }

    public QueryFactory getQueryFactory() {
        return queryFactory;
    }

    public UpdateQueryFactory getUpdateQueryFactory() {
        return updateQueryFactory;
    }

    public UpdateQueryBuilder getUpdateQueryBuilder() {
        return updateQueryBuilder;
    }

    public DeleteQueryFactory getDeleteQueryFactory() {
        return deleteQueryFactory;
    }

    public DeleteQueryBuilderImpl getDeleteQueryBuilder() {
        return deleteQueryBuilder;
    }

    public CallFactory getCallFactory() {
        return callFactory;
    }

    public SelectQueryBuilder getSelectQueryBuilder() {
        return selectQueryBuilder;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setLogger(Log logger) {
        if (logger != null) {
            this.logger = logger;
        }
        defaultRsExecutorManager.setLogger(this.logger);
        defaultRsCtl.setLogger(this.logger);
        if (updateQueryBuilder != null) {
            updateQueryBuilder.setLogger(this.logger);
        }
        if (deleteQueryBuilder != null) {
            deleteQueryBuilder.setLogger(this.logger);
        }
    }

    public Log getLogger() {
        return logger;
    }

    public boolean isSupportKeyUpdate() {
        return supportKeyUpdate;
    }

    public void setSupportKeyUpdate(boolean supportKeyUpdate) {
        this.supportKeyUpdate = supportKeyUpdate;
    }

    public boolean isBindNulls() {
        return bindNulls;
    }

    public void setBindNulls(boolean bindNulls) {
        this.bindNulls = bindNulls;
        updateQueryBuilder.setBindNulls(bindNulls);
        if (queryFactory instanceof AbstractQueryFactory) {
            ((AbstractQueryFactory) queryFactory).setBindNulls(bindNulls);
        }
    }

    public ResultSetExecutorManager getRsExecutorManager() {
        return rsExecutorManager;
    }

    public void setRsExecutorManager(ResultSetExecutorManager rsExecutorManager) {
        this.rsExecutorManager = rsExecutorManager;
    }

    public ResultSetCtl getRsCtl() {
        return rsCtl;
    }

    public void setRsCtl(ResultSetCtl rsCtl) {
        this.rsCtl = rsCtl;
    }

    protected int insertLocal(PersistentObject persistentObject, LifeCycleController controller, boolean fromUpdate) throws OdalPersistencyException {
        return insertLocal(getTransactionManager().getCurrentTransaction(), persistentObject, controller, fromUpdate);
    }

    public TypeHandlerRegistry getTypeHandlerRegistry() {
        return typeHandlerRegistry;
    }

    public void setTypeHandlerRegistry(TypeHandlerRegistry typeHandlerRegistry) {
        this.typeHandlerRegistry = typeHandlerRegistry;
    }

    public QueryContext getCurrentQueryContext() {
        return (QueryContext) queryContextLocal.get();
    }

    public void setCurrentQueryContext(QueryContext queryContext) {
        queryContextLocal.set(queryContext);
    }

    public void clearCurrentQueryContext() {
        queryContextLocal.set(null);
    }

    public void shutdown() {
    }

    public static class PostProcessing {

        PersistentObject persistentObject;

        int lobFieldIndex;

        Object value;

        public PostProcessing(PersistentObject persistentObject, int lobFieldIndex, InputStream value) {
            this.persistentObject = persistentObject;
            this.lobFieldIndex = lobFieldIndex;
            this.value = value;
        }

        public PostProcessing(PersistentObject persistentObject, int lobFieldIndex, String value) {
            this.persistentObject = persistentObject;
            this.lobFieldIndex = lobFieldIndex;
            this.value = value;
        }

        public PostProcessing(PersistentObject persistentObject, int lobFieldIndex, Reader value) {
            this.persistentObject = persistentObject;
            this.lobFieldIndex = lobFieldIndex;
            this.value = value;
        }
    }

    static class RetrieveResult {

        public RetrieveResult(int index, PersistentObject persistentObject) {
            this.index = index;
            this.persistentObject = persistentObject;
        }

        private int index;

        private PersistentObject persistentObject;

        public int getIndex() {
            return index;
        }

        public PersistentObject getPersistentObject() {
            return persistentObject;
        }
    }

    class BindParametersResult {

        int index;

        StringBuffer bind = new StringBuffer();
    }
}
