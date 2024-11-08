package org.monet.docservice.docprocessor.worker.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.monet.docservice.core.exceptions.ApplicationException;
import org.monet.docservice.core.log.Logger;
import org.monet.docservice.core.sql.NamedParameterStatement;
import org.monet.docservice.docprocessor.data.DataSourceProvider;
import org.monet.docservice.docprocessor.data.QueryStore;
import org.monet.docservice.docprocessor.worker.WorkQueue;
import org.monet.docservice.docprocessor.worker.WorkQueueItem;
import org.monet.docservice.docprocessor.worker.WorkQueueRepository;
import com.google.inject.Inject;

public class WorkQueueImpl implements WorkQueue, WorkQueueRepository {

    private QueryStore queryStore;

    private Logger logger;

    private DataSource dataSource;

    @Inject
    public void injectQueryStore(QueryStore queryStore) {
        this.queryStore = queryStore;
    }

    @Inject
    public void injectLogger(Logger logger) {
        this.logger = logger;
    }

    @Inject
    public void injectDataSourceProvider(DataSourceProvider dataSourceProvider) throws NamingException {
        this.dataSource = dataSourceProvider.get();
    }

    public boolean documentHasPendingOperations(String documentId) {
        logger.debug("documentHasPendingOperations(%s)", documentId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_WORK_QUEUE_DOCUMENT_HAS_PENDING_OPERATIONS));
            statement.setString(QueryStore.SELECT_WORK_QUEUE_DOCUMENT_HAS_PENDING_OPERATIONS_PARAM_ID_DOCUMENT, documentId);
            resultSet = statement.executeQuery();
            boolean result = resultSet != null && resultSet.next() && resultSet.getInt(QueryStore.SELECT_WORK_QUEUE_DOCUMENT_HAS_PENDING_OPERATIONS_RESULTSET_COUNT) > 0;
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
    }

    public long queueNewWorkItem(WorkQueueItem item) {
        logger.debug("queueNewWorkItem(%s)", item);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.INSERT_WORK_QUEUE_ITEM), Statement.RETURN_GENERATED_KEYS);
            statement.setString(QueryStore.INSERT_WORK_QUEUE_ITEM_PARAM_ID_DOCUMENT, item.getDocumentId());
            statement.setInt(QueryStore.INSERT_WORK_QUEUE_ITEM_PARAM_OPERATION, item.getOperation());
            statement.setInt(QueryStore.INSERT_WORK_QUEUE_ITEM_PARAM_STATE, item.getState());
            statement.setBinaryStream(QueryStore.INSERT_WORK_QUEUE_ITEM_PARAM_EXTRA_DATA, item.getExtraDataInputStream());
            resultSet = statement.executeUpdateAndGetGeneratedKeys();
            resultSet.next();
            long workQueueItemId = resultSet.getLong(1);
            return workQueueItemId;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
    }

    public long calculateEstimatedTimeToFinish(long workQueueItemId) {
        logger.debug("calculateEstimatedTimeToFinish(%s)", workQueueItemId);
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_DOCUMENT_ESTIMATED_TIME));
            statement.setLong(QueryStore.SELECT_DOCUMENT_ESTIMATED_TIME_PARAM_ID_DOCUMENT, workQueueItemId);
            resultSet = statement.executeQuery();
            long result;
            if (resultSet != null && resultSet.next()) {
                result = resultSet.getLong(QueryStore.SELECT_DOCUMENT_ESTIMATED_TIME_RESULTSET_TIME);
            } else {
                result = Long.MAX_VALUE;
            }
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
    }

    public void resetAllInProgress() {
        logger.debug("resetAllInProgress()");
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.RESET_WORK_QUEUE_ITEMS_IN_PROGRESS));
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public List<WorkQueueItem> getNotStartedOperations() {
        logger.debug("getNotStartedOperations()");
        List<WorkQueueItem> results = new ArrayList<WorkQueueItem>();
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_NOT_STARTED_WORK_QUEUE_ITEMS));
            resultSet = statement.executeQuery();
            if (resultSet != null) {
                while (resultSet.next()) {
                    WorkQueueItem item = new WorkQueueItem(resultSet.getLong(QueryStore.SELECT_NOT_STARTED_WORK_QUEUE_ITEMS_RESULTSET_ID));
                    item.setDocumentId(resultSet.getString(QueryStore.SELECT_NOT_STARTED_WORK_QUEUE_ITEMS_RESULTSET_ID_DOCUMENT));
                    item.setOperation(resultSet.getInt(QueryStore.SELECT_NOT_STARTED_WORK_QUEUE_ITEMS_RESULTSET_OPERATION));
                    item.setQueueDate(resultSet.getDate(QueryStore.SELECT_NOT_STARTED_WORK_QUEUE_ITEMS_RESULTSET_QUEUE_DATE));
                    item.setState(resultSet.getInt(QueryStore.SELECT_NOT_STARTED_WORK_QUEUE_ITEMS_RESULTSET_STATE));
                    results.add(item);
                }
            }
            return results;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
    }

    public void updateWorkQueueItemToError(long id, String message) {
        logger.debug("updateWorkQueueItemToError(%s, %s)", id, message);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_ERROR));
            statement.setLong(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_ERROR_PARAM_ID, id);
            statement.setInt(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_ERROR_PARAM_STATE, WorkQueueItem.STATE_ERROR);
            statement.setString(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_ERROR_PARAM_ERROR_MSG, message);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public void updateWorkQueueItemToFinished(long id) {
        logger.debug("updateWorkQueueItemToFinished(%s)", id);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_FINISH));
            statement.setLong(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_FINISH_PARAM_ID, id);
            statement.setInt(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_FINISH_PARAM_STATE, WorkQueueItem.STATE_FINISH);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public void updateWorkQueueItemToPending(long id) {
        logger.debug("updateWorkQueueItemToPending(%s)", id);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_PENDING));
            statement.setLong(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_PENDING_PARAM_ID, id);
            statement.setInt(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_PENDING_PARAM_STATE, WorkQueueItem.STATE_PENDING);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public void updateWorkQueueItemToInProgress(long id) {
        logger.debug("updateWorkQueueItemToInProgress(%s)", id);
        Connection connection = null;
        NamedParameterStatement statement = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_IN_PROGRESS));
            statement.setLong(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_IN_PROGRESS_PARAM_ID, id);
            statement.setInt(QueryStore.UPDATE_WORK_QUEUE_ITEM_STATE_TO_IN_PROGRESS_PARAM_STATE, WorkQueueItem.STATE_IN_PROGRESS);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(statement);
            close(connection);
        }
    }

    public InputStream getWorkQueueItemExtraData(long id) {
        logger.debug("getWorkQueueItemExtraData(%s)", id);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        Connection connection = null;
        NamedParameterStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            statement = new NamedParameterStatement(connection, this.queryStore.get(QueryStore.SELECT_WORK_QUEUE_ITEM_EXTRA_DATA));
            statement.setLong(QueryStore.SELECT_WORK_QUEUE_ITEM_EXTRA_DATA_PARAM_ID, id);
            InputStream result = null;
            resultSet = statement.executeQuery();
            if (resultSet != null && resultSet.next()) {
                result = resultSet.getBinaryStream(QueryStore.SELECT_WORK_QUEUE_ITEM_EXTRA_DATA_RESULTSET_EXTRA_DATA);
                if (result != null) {
                    copyData(result, data);
                    result = new ByteArrayInputStream(data.toByteArray());
                }
            }
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException(e.getMessage());
        } finally {
            close(resultSet);
            close(statement);
            close(connection);
        }
    }

    private static final void copyData(InputStream input, OutputStream output) throws IOException {
        int len;
        byte[] buff = new byte[16384];
        while ((len = input.read(buff)) > 0) output.write(buff, 0, len);
    }

    private static final void close(Connection connection) {
        if (connection != null) try {
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

    private static final void close(ResultSet documentDataCount) {
        if (documentDataCount != null) try {
            documentDataCount.close();
        } catch (SQLException e) {
        }
    }
}
