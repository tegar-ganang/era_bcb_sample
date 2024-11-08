package uk.org.ogsadai.activity.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityTerminatedException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.MatchedIterativeActivity;
import uk.org.ogsadai.activity.extension.ResourceActivity;
import uk.org.ogsadai.activity.io.ActivityInput;
import uk.org.ogsadai.activity.io.ActivityPipeProcessingException;
import uk.org.ogsadai.activity.io.BlockWriter;
import uk.org.ogsadai.activity.io.PipeClosedException;
import uk.org.ogsadai.activity.io.PipeIOException;
import uk.org.ogsadai.activity.io.PipeTerminatedException;
import uk.org.ogsadai.activity.io.TupleListActivityInput;
import uk.org.ogsadai.activity.io.TupleListIterator;
import uk.org.ogsadai.activity.io.TypedActivityInput;
import uk.org.ogsadai.common.msgs.DAILogger;
import uk.org.ogsadai.metadata.MetadataWrapper;
import uk.org.ogsadai.resource.ResourceAccessor;
import uk.org.ogsadai.resource.dataresource.jdbc.JDBCConnectionProvider;
import uk.org.ogsadai.resource.dataresource.jdbc.JDBCConnectionUseException;
import uk.org.ogsadai.tuple.Tuple;
import uk.org.ogsadai.tuple.TupleMetadata;

/**
 * Bulk loads data lists of tuples into tables and returns the number of tuples
 * inserted.
 * <p>
 * Activity inputs:
 * </p>
 * <ul>
 * <li> <code>tableName</code>. Type: {@link java.lang.String}. The table
 * name to insert the data to.This input is mandatory. 
 * </li>
 * <li> <code>data</code>. Type: OGSA-DAI list of
 * {@link uk.org.ogsadai.tuple.Tuple} with the first item in the list an
 * instance of {@link uk.org.ogsadai.metadata.MetadataWrapper} containing a
 * {@link uk.org.ogsadai.tuple.TupleMetadata} object. This input is mandatory. A
 * list of tuples each one of them is to be inserted to the provided table.
 * </li>
 * </ul>
 * <p>
 * Activity outputs:
 * </p>
 * <ul>
 * <li> <code>result</code>. Type: {@link java.lang.Integer}. The result
 * from the SQL bulk load, i.e. the number of tuples inserted successfully to
 * the table. </li>
 * </ul>
 * <p>
 * Configuration parameters: none.
 * </p>
 * <p>
 * Activity input/output ordering:
 * </p>
 * <ul>
 * <li> In each iteration the activity first reads one block from the
 * <code>tableName</code> input and then iterates through a complete list of
 * tuples from the <code>data</code> input. </li>
 * </ul>
 * <p>
 * Activity contracts: none.
 * </p>
 * <p>
 * Target data resource:
 * </p>
 * <ul>
 * <li>{@link uk.org.ogsadai.resource.dataresource.jdbc.JDBCConnectionProvider}.
 * </li>
 * </ul>
 * <p>
 * Behaviour:
 * </p>
 * <ul>
 * <li> This activity bulk loads lists of tuples into tables. 
 * An insert statement is performed for each tuple of the input data and 
 * the bulk load is performed transactionally, meaning that all previously completed 
 * inserts will be rolled back if any insertion fails. The activity outputs
 * the number of successfully inserted tuples.
 * </li>
 * </ul>
 * 
 * @author The OGSA-DAI Project Team.
 */
public class SQLBulkLoadTupleActivity extends MatchedIterativeActivity implements ResourceActivity {

    /** Copyright notice. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh,  2007-2008";

    /** Logger. */
    private static final DAILogger LOG = DAILogger.getLogger(SQLBulkLoadTupleActivity.class);

    /** 
     * Activity input name(<code>data</code>) - 
     * Tuples to be bulk loaded. 
     * (OGSA-DAI tuple list).
     */
    public static final String INPUT_TUPLES = "data";

    /** 
     * Activity input name(<code>tableName</code>) - 
     * Table name. 
     * ({@link java.lang.String}).
     */
    public static final String INPUT_TABLE_NAME = "tableName";

    /** 
     * Activity output name(<code>result</code>) - 
     * Number of inserted tuples. 
     * ({@link java.lang.Integer}).
     */
    public static final String OUTPUT_RESULT = "result";

    /** The JDBC connection provider */
    private JDBCConnectionProvider mResource;

    /** The database connection. */
    private Connection mConnection;

    /** The statement to be used for the execution of updates. */
    private PreparedStatement mStatement;

    /** The only output of the activity. */
    private BlockWriter mOutput;

    /**
     * {@inheritDoc}
     */
    public Class getTargetResourceAccessorClass() {
        return JDBCConnectionProvider.class;
    }

    /**
     * {@inheritDoc}
     */
    public void setTargetResourceAccessor(final ResourceAccessor targetResource) {
        mResource = (JDBCConnectionProvider) targetResource;
    }

    /**
     * {@inheritDoc}
     */
    protected ActivityInput[] getIterationInputs() {
        return new ActivityInput[] { new TypedActivityInput(INPUT_TABLE_NAME, String.class), new TupleListActivityInput(INPUT_TUPLES) };
    }

    /**
     * {@inheritDoc}
     */
    protected void preprocess() throws ActivityUserException, ActivityProcessingException, ActivityTerminatedException {
        validateOutput(OUTPUT_RESULT);
        mOutput = getOutput();
        try {
            mConnection = mResource.getConnection();
            mConnection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new ActivitySQLException(e);
        } catch (JDBCConnectionUseException e) {
            throw new ActivitySQLException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void processIteration(final Object[] iterationData) throws ActivityProcessingException, ActivityTerminatedException, ActivityUserException {
        final String tableName = (String) iterationData[0];
        final TupleListIterator tuples = (TupleListIterator) iterationData[1];
        final MetadataWrapper metadataWrapper = tuples.getMetadataWrapper();
        final TupleMetadata metadata = (TupleMetadata) metadataWrapper.getMetadata();
        final String sql = SQLUtilities.createInsertStatementSQL(tableName, metadata);
        try {
            mStatement = mConnection.prepareStatement(sql);
            int totalCount = 0;
            Tuple tuple = null;
            while ((tuple = (Tuple) tuples.nextValue()) != null) {
                SQLUtilities.setStatementParameters(mStatement, tuple, metadata);
                totalCount += mStatement.executeUpdate();
            }
            mOutput.write(new Integer(totalCount));
            mConnection.commit();
            mStatement.close();
        } catch (SQLException e) {
            rollback();
            throw new ActivitySQLUserException(e);
        } catch (PipeClosedException e) {
            rollback();
            iterativeStageComplete();
        } catch (PipeIOException e) {
            rollback();
            throw new ActivityPipeProcessingException(e);
        } catch (PipeTerminatedException e) {
            rollback();
            throw new ActivityTerminatedException();
        } catch (ActivityUserException e) {
            rollback();
            throw e;
        } catch (ActivityProcessingException e) {
            rollback();
            throw e;
        } catch (ActivityTerminatedException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw new ActivityProcessingException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void postprocess() throws ActivityUserException, ActivityProcessingException, ActivityTerminatedException {
        try {
            mConnection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new ActivitySQLUserException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void cleanUp() throws Exception {
        super.cleanUp();
        if (mStatement != null) {
            mStatement.close();
        }
        if (mConnection != null && !mConnection.getAutoCommit()) {
            mConnection.rollback();
            mConnection.setAutoCommit(true);
        }
        if (mResource != null) {
            mResource.releaseConnection(mConnection);
        }
    }

    /**
     * Rolls back the database state in case of an error.
     */
    private void rollback() {
        try {
            mConnection.rollback();
            mConnection.setAutoCommit(true);
        } catch (SQLException e) {
            LOG.warn(e);
        }
    }
}
