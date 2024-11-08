package org.hsqldb.jdbc;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.ParameterMetaData;
import org.hsqldb.Column;
import org.hsqldb.HsqlDateTime;
import org.hsqldb.HsqlException;
import org.hsqldb.Result;
import org.hsqldb.ResultConstants;
import org.hsqldb.Trace;
import org.hsqldb.Types;
import org.hsqldb.lib.ArrayUtil;
import org.hsqldb.lib.HsqlByteArrayOutputStream;
import org.hsqldb.lib.Iterator;
import org.hsqldb.lib.StringConverter;
import org.hsqldb.types.Binary;
import org.hsqldb.types.JavaObject;

/**
 * <!-- start generic documentation -->
 *
 * An object that represents a precompiled SQL statement. <p>
 *
 * An SQL statement is precompiled and stored in a
 * <code>PreparedStatement</code> object. This object can then be used to
 * efficiently execute this statement multiple times.
 *
 * <P><B>Note:</B> The setter methods (<code>setShort</code>,
 * <code>setString</code>, and so on) for setting IN parameter values
 * must specify types that are compatible with the defined SQL type of
 * the input parameter. For instance, if the IN parameter has SQL type
 * <code>INTEGER</code>, then the method <code>setInt</code> should be
 * used. <p>
 *
 * If arbitrary parameter type conversions are required, the method
 * <code>setObject</code> should be used with a target SQL type.
 * <P>
 * In the following example of setting a parameter, <code>con</code>
 * represents an active connection:
 * <PRE>
 * PreparedStatement pstmt = con.prepareStatement("UPDATE EMPLOYEES
 *                               SET SALARY = ? WHERE ID = ?");
 * pstmt.setBigDecimal(1, 153833.00)
 * pstmt.setInt(2, 110592)
 * </PRE> <p>
 * <!-- end generic documentation -->
 * <!-- start Release-specific documentation -->
 * <div class="ReleaseSpecificDocumentation">
 * <h3>HSQLDB-Specific Information:</h3> <p>
 *
 * Starting with HSQLDB 1.7.2, jdbcPreparedStatement objects are backed by
 * a true compiled parameteric representation. Hence, there are now significant
 * performance gains to be had by using a jdbcPreparedStatement object in
 * preference to a jdbcStatement object, if a short-running SQL statement is
 * to be executed more than a small number of times. <p>
 *
 * When it can be otherwise avoided, it is to be considered poor
 * practice to fully prepare (construct), parameterize, execute, fetch and
 * close a jdbcPreparedStatement object for each execution cycle. Indeed, under
 * HSQLDB 1.8.0, this practice is likely to be noticably <em>less</em>
 * performant for short-running statements than the equivalent process using
 * jdbcStatement objects, albeit far more convenient, less error prone and
 * certainly much less resource-intensive, especially when large binary and
 * character values are involved, due to the optimized parameterization
 * facility. <p>
 *
 * Instead, when developing an application that is not totally oriented toward
 * the execution of ad hoc SQL, it is recommended to expend some effort toward
 * identifing the SQL statements that are good candidates for regular reuse and
 * adapting the structure of the application accordingly. Often, this is done
 * by recording the text of candidate SQL statements in an application resource
 * object (which has the nice side-benefit of isolating and hiding differences
 * in SQL dialects across different drivers) and caching for possible reuse the
 * PreparedStatement objects derived from the recorded text. <p>
 *
 * <b>Multi thread use:</b> <p>
 *
 * A PreparedStatement object is stateful and should not normally be shared
 * by multiple threads. If it has to be shared, the calls to set the
 * parameters, calls to add batch statements, the execute call and any
 * post-execute calls should be made within a block synchronized on the
 * PreparedStatement Object.<p>
 *
 * <b>JRE 1.1.x Notes:</b> <p>
 *
 * In general, JDBC 2 support requires Java 1.2 and above, and JDBC3 requires
 * Java 1.4 and above. In HSQLDB, support for methods introduced in different
 * versions of JDBC depends on the JDK version used for compiling and building
 * HSQLDB.<p>
 *
 * Since 1.7.0, it is possible to build the product so that
 * all JDBC 2 methods can be called while executing under the version 1.1.x
 * <em>Java Runtime Environment</em><sup><font size="-2">TM</font></sup>.
 * However, in addition to requiring explicit casts to the org.hsqldb.jdbcXXX
 * interface implementations, some of these method calls require
 * <code>int</code> values that are defined only in the JDBC 2 or greater
 * version of
 * <a href="http://java.sun.com/j2se/1.4/docs/api/java/sql/ResultSet.html">
 * <code>ResultSet</code></a> interface.  For this reason, when the
 * product is compiled under JDK 1.1.x, these values are defined in
 * {@link jdbcResultSet jdbcResultSet}.<p>
 *
 * In a JRE 1.1.x environment, calling JDBC 2 methods that take or return the
 * JDBC2-only <code>ResultSet</code> values can be achieved by referring
 * to them in parameter specifications and return value comparisons,
 * respectively, as follows: <p>
 *
 * <pre class="JavaCodeExample">
 * jdbcResultSet.FETCH_FORWARD
 * jdbcResultSet.TYPE_FORWARD_ONLY
 * jdbcResultSet.TYPE_SCROLL_INSENSITIVE
 * jdbcResultSet.CONCUR_READ_ONLY
 * // etc.
 * </pre>
 *
 * However, please note that code written in such a manner will not be
 * compatible for use with other JDBC 2 drivers, since they expect and use
 * <code>ResultSet</code>, rather than <code>jdbcResultSet</code>.  Also
 * note, this feature is offered solely as a convenience to developers
 * who must work under JDK 1.1.x due to operating constraints, yet wish to
 * use some of the more advanced features available under the JDBC 2
 * specification.<p>
 *
 * (fredt@users)<br>
 * (boucherb@users)
 * </div>
 * <!-- end Release-specific documentation -->
 *
 * @author boucherb@users
 * @author fredt@users
 * @version 1.8.0
 * @see jdbcConnection#prepareStatement
 * @see jdbcResultSet
 */
public class jdbcPreparedStatement extends jdbcStatement implements PreparedStatement {

    /** The parameter values for the next non-batch execution. */
    protected Object[] parameterValues;

    /** Flags for bound variables. */
    protected boolean[] parameterSet;

    /** Flags for bound stream variables. */
    protected boolean[] parameterStream;

    /** The SQL types of the parameters. */
    protected int[] parameterTypes;

    /** The (IN, IN OUT, or OUT) modes of parameters */
    protected int[] parameterModes;

    /** Lengths for streams. */
    protected int[] streamLengths;

    /** Has a stream or CLOB / BLOB parameter value. */
    protected boolean hasStreams;

    /**
     * Description of result set metadata. <p>
     */
    protected Result rsmdDescriptor;

    /** Description of parameter metadata. */
    protected Result pmdDescriptor;

    /** This object's one and one ResultSetMetaData object. */
    protected jdbcResultSetMetaData rsmd;

    /** This object's one and only ParameterMetaData object. */
    protected Object pmd;

    /** The SQL character sequence that this object represents. */
    protected String sql;

    /**
     * The id with which this object's corresponding
     * {@link org.hsqldb.CompiledStatement CompiledStatement}
     * object is registered in the engine's
     * {@link org.hsqldb.CompiledStatementManager CompiledStatementManager}
     * object.
     */
    protected int statementID;

    /**
     * Whether this statement generates only a single row update count in
     * response to execution.
     */
    protected boolean isRowCount;

    /**
     * <!-- start generic documentation -->
     * Sets escape processing on or off. <p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Since 1.7.0, the implementation follows the standard
     * behaviour by overriding the same method in jdbcStatement
     * class. <p>
     *
     * In other words, calling this method has no effect.
     * </div>
     * <!-- end release-specific documentation -->
     *
     * @param enable <code>true</code> to enable escape processing;
     *     <code>false</code> to disable it
     * @exception SQLException if a database access error occurs
     */
    public void setEscapeProcessing(boolean enable) throws SQLException {
        checkClosed();
    }

    /**
     * <!-- start generic documentation -->
     * Executes the SQL statement in this <code>PreparedStatement</code>
     * object, which may be any kind of SQL statement.
     * Some prepared statements return multiple results; the
     * <code>execute</code> method handles these complex statements as well
     * as the simpler form of statements handled by the methods
     * <code>executeQuery</code>and <code>executeUpdate</code>. <p>
     *
     * The <code>execute</code> method returns a <code>boolean</code> to
     * indicate the form of the first result.  You must call either the method
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result; you must call <code>getMoreResults</code> to
     * move to any subsequent result(s). <p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Including 1.8.0, prepared statements do not generate
     * multiple fetchable results. <p>
     *
     * In future versions, it will be possible that statements
     * generate multiple fetchable results under certain conditions.
     * </div>
     *
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *    object; <code>false</code> if the first result is an update
     *    count or there is no result
     * @exception SQLException if a database access error occurs or an argument
     *       is supplied to this method
     * @see jdbcStatement#execute
     * @see jdbcStatement#getResultSet
     * @see jdbcStatement#getUpdateCount
     * @see jdbcStatement#getMoreResults
     */
    public boolean execute() throws SQLException {
        checkClosed();
        connection.clearWarningsNoCheck();
        resultIn = null;
        try {
            resultOut.setMaxRows(maxRows);
            resultOut.setParameterData(parameterValues);
            resultIn = connection.sessionProxy.execute(resultOut);
        } catch (HsqlException e) {
            throw Util.sqlException(e);
        }
        if (resultIn.isError()) {
            Util.throwError(resultIn);
        }
        return resultIn.isData();
    }

    /**
     * <!-- start generic documentation -->
     * Executes the SQL query in this <code>PreparedStatement</code> object
     * and returns the <code>ResultSet</code> object generated by the query.<p>
     * <!-- end generic documentation -->
     *
     * @return a <code>ResultSet</code> object that contains the data produced
     *    by the query; never <code>null</code>
     * @exception SQLException if a database access error occurs or the SQL
     *       statement does not return a <code>ResultSet</code> object
     */
    public ResultSet executeQuery() throws SQLException {
        checkClosed();
        connection.clearWarningsNoCheck();
        checkIsRowCount(false);
        checkParametersSet();
        resultIn = null;
        try {
            resultOut.setMaxRows(maxRows);
            resultOut.setParameterData(parameterValues);
            resultIn = connection.sessionProxy.execute(resultOut);
        } catch (HsqlException e) {
            throw Util.sqlException(e);
        }
        if (resultIn.isError()) {
            Util.throwError(resultIn);
        } else if (!resultIn.isData()) {
            String msg = "Expected but did not recieve a result set";
            throw Util.sqlException(Trace.UNEXPECTED_EXCEPTION, msg);
        }
        return new jdbcResultSet(this, resultIn, connection.connProperties, connection.isNetConn);
    }

    /**
     * <!-- start generic documentation -->
     * Executes the SQL statement in this <code>PreparedStatement</code>
     * object, which must be an SQL <code>INSERT</code>,
     * <code>UPDATE</code> or <code>DELETE</code> statement; or an SQL
     * statement that returns nothing, such as a DDL statement.<p>
     * <!-- end generic documentation -->
     *
     * @return either (1) the row count for <code>INSERT</code>,
     *     <code>UPDATE</code>, or <code>DELETE</code>
     *     statements or (2) 0 for SQL statements that
     *     return nothing
     * @exception SQLException if a database access error occurs or the SQL
     *        statement returns a <code>ResultSet</code> object
     */
    public int executeUpdate() throws SQLException {
        checkClosed();
        connection.clearWarningsNoCheck();
        checkIsRowCount(true);
        checkParametersSet();
        resultIn = null;
        try {
            resultOut.setParameterData(parameterValues);
            resultIn = connection.sessionProxy.execute(resultOut);
        } catch (HsqlException e) {
            throw Util.sqlException(e);
        }
        if (resultIn.isError()) {
            Util.throwError(resultIn);
        } else if (resultIn.mode != ResultConstants.UPDATECOUNT) {
            String msg = "Expected but did not recieve a row update count";
            throw Util.sqlException(Trace.UNEXPECTED_EXCEPTION, msg);
        }
        return resultIn.getUpdateCount();
    }

    /**
     * <!-- start generic documentation -->
     * Submits a batch of commands to the database for execution and
     * if all commands execute successfully, returns an array of update counts.
     * The <code>int</code> elements of the array that is returned are ordered
     * to correspond to the commands in the batch, which are ordered
     * according to the order in which they were added to the batch.
     * The elements in the array returned by the method <code>executeBatch</code>
     * may be one of the following:
     * <OL>
     * <LI>A number greater than or equal to zero -- indicates that the
     * command was processed successfully and is an update count giving the
     * number of rows in the database that were affected by the command's
     * execution
     * <LI>A value of <code>SUCCESS_NO_INFO</code> -- indicates that the command was
     * processed successfully but that the number of rows affected is
     * unknown
     * <P>
     * If one of the commands in a batch update fails to execute properly,
     * this method throws a <code>BatchUpdateException</code>, and a JDBC
     * driver may or may not continue to process the remaining commands in
     * the batch.  However, the driver's behavior must be consistent with a
     * particular DBMS, either always continuing to process commands or never
     * continuing to process commands.  If the driver continues processing
     * after a failure, the array returned by the method
     * <code>BatchUpdateException.getUpdateCounts</code>
     * will contain as many elements as there are commands in the batch, and
     * at least one of the elements will be the following:
     * <P>
     * <LI>A value of <code>EXECUTE_FAILED</code> -- indicates that the command failed
     * to execute successfully and occurs only if a driver continues to
     * process commands after a command fails
     * </OL>
     * <P>
     * A driver is not required to implement this method.
     * The possible implementations and return values have been modified in
     * the Java 2 SDK, Standard Edition, version 1.3 to
     * accommodate the option of continuing to proccess commands in a batch
     * update after a <code>BatchUpdateException</code> obejct has been thrown. <p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Starting with HSQLDB 1.7.2, this feature is supported. <p>
     *
     * HSQLDB stops execution of commands in a batch when one of the commands
     * results in an exception. The size of the returned array equals the
     * number of commands that were executed successfully.<p>
     *
     * When the product is built under the JAVA1 target, an exception
     * is never thrown and it is the responsibility of the client software to
     * check the size of the  returned update count array to determine if any
     * batch items failed.  To build and run under the JAVA2 target, JDK/JRE
     * 1.3 or higher must be used.
     * </div>
     * <!-- end release-specific documentation -->
     *
     * @return an array of update counts containing one element for each
     * command in the batch.  The elements of the array are ordered according
     * to the order in which commands were added to the batch.
     * @exception SQLException if a database access error occurs or the
     * driver does not support batch statements. Throws
     * {@link java.sql.BatchUpdateException}
     * (a subclass of <code>java.sql.SQLException</code>) if one of the commands
     * sent  to the database fails to execute properly or attempts to return a
     * result set.
     * @since JDK 1.3 (JDK 1.1.x developers: read the new overview
     *   for jdbcStatement)
     */
    public int[] executeBatch() throws SQLException {
        if (batchResultOut == null) {
            batchResultOut = new Result(ResultConstants.BATCHEXECUTE, parameterTypes, statementID);
        }
        return super.executeBatch();
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to SQL <code>NULL</code>. <p>
     *
     * <B>Note:</B> You must specify the parameter's SQL type.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * HSQLDB ignores the sqlType argument.
     * </div>
     * <!-- end release-specific documentation -->
     *
     * @param paramIndex the first parameter is 1, the second is 2, ...
     * @param sqlType the SQL type code defined in <code>java.sql.Types</code>
     * @exception SQLException if a database access error occurs
     */
    public void setNull(int paramIndex, int sqlType) throws SQLException {
        setParameter(paramIndex, null);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java <code>boolean</code>
     * value.  The driver converts this to an SQL <code>BIT</code> value
     * when it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Since 1.7.2, HSQLDB uses the BOOLEAN type instead of BIT, as
     * per SQL 200n (SQL 3).
     * </div>
     * <!-- end release-specific documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        Boolean b = x ? Boolean.TRUE : Boolean.FALSE;
        setParameter(parameterIndex, b);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java <code>byte</code> value.
     * The driver converts this to an SQL <code>TINYINT</code> value when
     * it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setByte(int parameterIndex, byte x) throws SQLException {
        setIntParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java <code>short</code>
     * value. The driver converts this to an SQL <code>SMALLINT</code>
     * value when it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setShort(int parameterIndex, short x) throws SQLException {
        setIntParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java <code>int</code> value.
     * The driver converts this to an SQL <code>INTEGER</code> value when
     * it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setInt(int parameterIndex, int x) throws SQLException {
        setIntParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java <code>long</code> value.
     * The driver converts this to an SQL <code>BIGINT</code> value when
     * it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setLong(int parameterIndex, long x) throws SQLException {
        setLongParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java <code>float</code> value.
     * The driver converts this to an SQL <code>FLOAT</code> value when
     * it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Since 1.7.1, HSQLDB handles Java positive/negative Infinity
     * and NaN <code>float</code> values consistent with the Java Language
     * Specification; these <em>special</em> values are now correctly stored
     * to and retrieved from the database.
     * </div>
     * <!-- start release-specific documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setFloat(int parameterIndex, float x) throws SQLException {
        setDouble(parameterIndex, (double) x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java <code>double</code> value.
     * The driver converts this to an SQL <code>DOUBLE</code> value when it
     * sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Since 1.7.1, HSQLDB handles Java positive/negative Infinity
     * and NaN <code>double</code> values consistent with the Java Language
     * Specification; these <em>special</em> values are now correctly stored
     * to and retrieved from the database.
     * </div>
     * <!-- start release-specific documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setDouble(int parameterIndex, double x) throws SQLException {
        Double d = new Double(x);
        setParameter(parameterIndex, d);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given
     * <code>java.math.BigDecimal</code> value.
     * The driver converts this to an SQL <code>NUMERIC</code> value when
     * it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java <code>String</code> value.
     * The driver converts this
     * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
     * (depending on the argument's
     * size relative to the driver's limits on <code>VARCHAR</code> values)
     * when it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Including 1.7.2, HSQLDB stores all XXXCHAR values as java.lang.String
     * objects; there is no appreciable difference between
     * CHAR, VARCHAR and LONGVARCHAR.
     * </div>
     * <!-- start release-specific documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setString(int parameterIndex, String x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given Java array of bytes.
     * The driver converts this to an SQL <code>VARBINARY</code> or
     * <code>LONGVARBINARY</code> (depending on the argument's size relative
     * to the driver's limits on <code>VARBINARY</code> values) when it
     * sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Including 1.7.2, HSQLDB stores all XXXBINARY values the same way; there
     * is no appreciable difference between BINARY, VARBINARY and
     * LONGVARBINARY.
     * </div>
     * <!-- start release-specific documentation -->
     *
     * @param paramIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setBytes(int paramIndex, byte[] x) throws SQLException {
        setParameter(paramIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given
     * <code>java.sql.Date</code> value.  The driver converts this
     * to an SQL <code>DATE</code> value when it sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setDate(int parameterIndex, Date x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given <code>java.sql.Time</code>
     * value. The driver converts this to an SQL <code>TIME</code> value when it
     * sends it to the database.<p>
     * <!-- end generic documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setTime(int parameterIndex, Time x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given
     * <code>java.sql.Timestamp</code> value.  The driver converts this to
     * an SQL <code>TIMESTAMP</code> value when it sends it to the
     * database.<p>
     * <!-- end generic documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if a database access error occurs
     */
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format. <p>
     *
     * <b>Note:</b> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.<p>
     * <!-- end generic documentation -->
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * This method uses the default platform character encoding to convert bytes
     * from the stream into the characters of a String. In the future this is
     * likely to change to always treat the stream as ASCII.<p>
     *
     * Before HSQLDB 1.7.0, <code>setAsciiStream</code> and
     * <code>setUnicodeStream</code> were identical.
     * </div>
     * <!-- end release-specific documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the Java input stream that contains the ASCII parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if a database access error occurs
     */
    public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        checkSetParameterIndex(parameterIndex, true);
        String s;
        if (x == null) {
            s = "input stream is null";
            throw Util.sqlException(Trace.INVALID_JDBC_ARGUMENT, s);
        }
        try {
            s = StringConverter.inputStreamToString(x, length);
            setParameter(parameterIndex, s);
        } catch (IOException e) {
            throw Util.sqlException(Trace.INVALID_CHARACTER_ENCODING);
        }
    }

    public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        checkSetParameterIndex(parameterIndex, true);
        String msg = null;
        if (x == null) {
            msg = "input stream is null";
        } else if (length % 2 != 0) {
            msg = "odd length argument";
        }
        if (msg != null) {
            throw Util.sqlException(Trace.INVALID_JDBC_ARGUMENT, msg);
        }
        int chlen = length / 2;
        int chread = 0;
        StringBuffer sb = new StringBuffer();
        int hi;
        int lo;
        try {
            for (; chread < chlen; chread++) {
                hi = x.read();
                if (hi == -1) {
                    break;
                }
                lo = x.read();
                if (lo == -1) {
                    break;
                }
                sb.append((char) (hi << 8 | lo));
            }
        } catch (IOException e) {
            throw Util.sqlException(Trace.TRANSFER_CORRUPTED);
        }
        setParameter(parameterIndex, sb.toString());
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Since 1.7.2, this method works according to the standard.
     * </div>
     * <!-- end release-specific documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the java input stream which contains the binary parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if a database access error occurs
     */
    public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        checkSetParameterIndex(parameterIndex, true);
        if (x == null) {
            throw Util.sqlException(Trace.error(Trace.INVALID_JDBC_ARGUMENT, Trace.JDBC_NULL_STREAM));
        }
        HsqlByteArrayOutputStream out = null;
        try {
            out = new HsqlByteArrayOutputStream();
            int size = 2048;
            byte[] buff = new byte[size];
            for (int left = length; left > 0; ) {
                int read = x.read(buff, 0, left > size ? size : left);
                if (read == -1) {
                    break;
                }
                out.write(buff, 0, read);
                left -= read;
            }
            setParameter(parameterIndex, out.toByteArray());
        } catch (IOException e) {
            throw Util.sqlException(Trace.INPUTSTREAM_ERROR, e.toString());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * <!-- start generic documentation -->
     * Clears the current parameter values immediately. <p>
     *
     * In general, parameter values remain in force for repeated use of a
     * statement. Setting a parameter value automatically clears its
     * previous value.  However, in some cases it is useful to immediately
     * release the resources used by the current parameter values; this can
     * be done by calling the method <code>clearParameters</code>.<p>
     * <!-- end generic documentation -->
     *
     * @exception SQLException if a database access error occurs
     */
    public void clearParameters() throws SQLException {
        checkClosed();
        ArrayUtil.fillArray(parameterValues, null);
        ArrayUtil.clearArray(ArrayUtil.CLASS_CODE_BOOLEAN, parameterSet, 0, parameterSet.length);
        if (parameterStream != null) {
            ArrayUtil.clearArray(ArrayUtil.CLASS_CODE_BOOLEAN, parameterStream, 0, parameterStream.length);
        }
    }

    /**
     * <!-- start generic documentation -->
     * Sets the value of the designated parameter with the given object. <p>
     *
     * The second argument must be an object type; for integral values, the
     * <code>java.lang</code> equivalent objects should be used. <p>
     *
     * The given Java object will be converted to the given targetSqlType
     * before being sent to the database.
     *
     * If the object has a custom mapping (is of a class implementing the
     * interface <code>SQLData</code>),
     * the JDBC driver should call the method <code>SQLData.writeSQL</code> to
     * write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,
     * <code>Struct</code>, or <code>Array</code>, the driver should pass it
     * to the database as a value of the corresponding SQL type. <p>
     *
     * Note that this method may be used to pass database-specific
     * abstract data types.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Inculding 1.7.1,this method was identical to
     * {@link #setObject(int, Object, int) setObject(int, Object, int)}.
     * That is, this method simply called setObject(int, Object, int),
     * ignoring the scale specification. <p>
     *
     * Since 1.7.2, this method supports the conversions listed in the
     * conversion table B-5 of the JDBC 3 specification. The scale argument
     * is not used.
     * </div>
     * <!-- start release-specific documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     * sent to the database. The scale argument may further qualify this type.
     * @param scale for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
     *     this is the number of digits after the decimal point.  For all
     *     other types, this value will be ignored. <p>
     *
     *     Up to and including HSQLDB 1.7.0, this parameter is ignored.
     * @exception SQLException if a database access error occurs
     * @see java.sql.Types
     * @see #setObject(int,Object,int)
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
        setObject(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the value of the designated parameter with the given object.
     * This method is like the method <code>setObject</code>
     * above, except that it assumes a scale of zero. <p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * Since 1.7.2, this method supports conversions listed in the
     * conversion table B-5 of the JDBC 3 specification.
     * </div>
     * <!-- end release-specific documentation -->
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     *                sent to the database
     * @exception SQLException if a database access error occurs
     * @see #setObject(int,Object)
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        setObject(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the value of the designated parameter using the given object. <p>
     *
     * The second parameter must be of type <code>Object</code>; therefore,
     * the <code>java.lang</code> equivalent objects should be used for
     * built-in types. <p>
     *
     * The JDBC specification specifies a standard mapping from
     * Java <code>Object</code> types to SQL types.  The given argument
     * will be converted to the corresponding SQL type before being
     * sent to the database. <p>
     *
     * Note that this method may be used to pass datatabase-
     * specific abstract data types, by using a driver-specific Java
     * type.  If the object is of a class implementing the interface
     * <code>SQLData</code>, the JDBC driver should call the method
     * <code>SQLData.writeSQL</code> to write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,
     * <code>Struct</code>, or <code>Array</code>, the driver should pass
     * it to the database as a value of the corresponding SQL type. <p>
     *
     * This method throws an exception if there is an ambiguity, for
     * example, if the object is of a class implementing more than one
     * of the interfaces named above.<p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3><p>
     *
     * Since 1.7.2, this method supports conversions listed in the conversion
     * table B-5 of the JDBC 3 specification.<p>
     *
     * </div>
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @exception SQLException if a database access error occurs or the type
     *      of the given object is ambiguous
     */
    public void setObject(int parameterIndex, Object x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void addBatch() throws SQLException {
        checkClosed();
        int len = parameterValues.length;
        Object[] bpValues = new Object[len];
        checkParametersSet();
        System.arraycopy(parameterValues, 0, bpValues, 0, len);
        if (batchResultOut == null) {
            batchResultOut = new Result(ResultConstants.BATCHEXECUTE, parameterTypes, statementID);
        }
        batchResultOut.add(bpValues);
    }

    public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException {
        checkSetParameterIndex(parameterIndex, true);
        if (reader == null) {
            String msg = "reader is null";
            throw Util.sqlException(Trace.INVALID_JDBC_ARGUMENT, msg);
        }
        final StringBuffer sb = new StringBuffer();
        final int size = 2048;
        final char[] buff = new char[size];
        try {
            for (int left = length; left > 0; ) {
                final int read = reader.read(buff, 0, left > size ? size : left);
                if (read == -1) {
                    break;
                }
                sb.append(buff, 0, read);
                left -= read;
            }
        } catch (IOException e) {
            throw Util.sqlException(Trace.TRANSFER_CORRUPTED, e.toString());
        }
        setParameter(parameterIndex, sb.toString());
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given
     * <code>REF(&lt;structured-type&gt;)</code> value.
     * The driver converts this to an SQL <code>REF</code> value when it
     * sends it to the database. <p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * HSQLDB 1.7.2 does not support the SQL REF type. Calling this method
     * throws an exception.
     *
     * </div>
     * <!-- end release-specific documentation -->
     * @param i the first parameter is 1, the second is 2, ...
     * @param x an SQL <code>REF</code> value
     * @exception SQLException if a database access error occurs
     * @since JDK 1.2 (JDK 1.1.x developers: read the new overview for
     * jdbcPreparedStatement)
     */
    public void setRef(int i, Ref x) throws SQLException {
        throw Util.notSupported();
    }

    public void setBlob(int i, Blob x) throws SQLException {
        if (x instanceof jdbcBlob) {
            setParameter(i, ((jdbcBlob) x).data);
            return;
        } else if (x == null) {
            setParameter(i, null);
            return;
        }
        checkSetParameterIndex(i, false);
        final long length = x.length();
        if (length > Integer.MAX_VALUE) {
            String msg = "Maximum Blob input octet length exceeded: " + length;
            throw Util.sqlException(Trace.INPUTSTREAM_ERROR, msg);
        }
        HsqlByteArrayOutputStream out = null;
        try {
            out = new HsqlByteArrayOutputStream();
            java.io.InputStream in = x.getBinaryStream();
            int buffSize = 2048;
            byte[] buff = new byte[buffSize];
            for (int left = (int) length; left > 0; ) {
                int read = in.read(buff, 0, left > buffSize ? buffSize : left);
                if (read == -1) {
                    break;
                }
                out.write(buff, 0, read);
                left -= read;
            }
            setParameter(i, out.toByteArray());
        } catch (IOException e) {
            throw Util.sqlException(Trace.INPUTSTREAM_ERROR, e.toString());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void setClob(int i, Clob x) throws SQLException {
        if (x instanceof jdbcClob) {
            setParameter(i, ((jdbcClob) x).data);
            return;
        } else if (x == null) {
            setParameter(i, null);
            return;
        }
        checkSetParameterIndex(i, false);
        final long length = x.length();
        if (length > Integer.MAX_VALUE) {
            String msg = "Max Clob input character length exceeded: " + length;
            throw Util.sqlException(Trace.INPUTSTREAM_ERROR, msg);
        }
        java.io.Reader reader = x.getCharacterStream();
        final StringBuffer sb = new StringBuffer();
        final int size = 2048;
        final char[] buff = new char[size];
        try {
            for (int left = (int) length; left > 0; ) {
                final int read = reader.read(buff, 0, left > size ? size : left);
                if (read == -1) {
                    break;
                }
                sb.append(buff, 0, read);
                left -= read;
            }
        } catch (IOException e) {
            throw Util.sqlException(Trace.TRANSFER_CORRUPTED, e.toString());
        }
        setParameter(i, sb.toString());
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to the given <code>Array</code> object.
     * The driver converts this to an SQL <code>ARRAY</code> value when it
     * sends it to the database. <p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * HSQLDB 1.7.2 does not support the SQL ARRAY type. Calling this method
     * throws an exception.
     *
     * </div>
     * <!-- end release-specific documentation -->
     * @param i the first parameter is 1, the second is 2, ...
     * @param x an <code>Array</code> object that maps an SQL <code>ARRAY</code>
     *       value
     * @exception SQLException if a database access error occurs
     * @since JDK 1.2 (JDK 1.1.x developers: read the new overview for
     *   jdbcPreparedStatement)
     */
    public void setArray(int i, Array x) throws SQLException {
        throw Util.notSupported();
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        checkClosed();
        if (isRowCount) {
            return null;
        }
        if (rsmd == null) {
            rsmd = new jdbcResultSetMetaData(rsmdDescriptor, connection.connProperties);
        }
        return rsmd;
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        String s;
        try {
            s = HsqlDateTime.getDateString(x, cal);
        } catch (Exception e) {
            throw Util.sqlException(Trace.INVALID_ESCAPE, e.toString());
        }
        setParameter(parameterIndex, s);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        String s;
        try {
            s = HsqlDateTime.getTimeString(x, cal);
        } catch (Exception e) {
            throw Util.sqlException(Trace.INVALID_ESCAPE, e.toString());
        }
        setParameter(parameterIndex, s);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        checkSetParameterIndex(parameterIndex, false);
        if (cal != null && x != null) {
            int ns = x.getNanos();
            x = new Timestamp(HsqlDateTime.getTimeInMillis(x, cal, null));
            x.setNanos(ns);
        }
        setParameter(parameterIndex, x);
    }

    /**
     * <!-- start generic documentation -->
     * Sets the designated parameter to SQL <code>NULL</code>.
     * This version of the method <code>setNull</code> should
     * be used for user-defined types and REF type parameters.  Examples
     * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and
     * named array types.
     *
     * <P><B>Note:</B> To be portable, applications must give the
     * SQL type code and the fully-qualified SQL type name when specifying
     * a NULL user-defined or REF parameter.  In the case of a user-defined
     * type the name is the type name of the parameter itself.  For a REF
     * parameter, the name is the type name of the referenced type.  If
     * a JDBC driver does not need the type code or type name information,
     * it may ignore it.
     *
     * Although it is intended for user-defined and Ref parameters,
     * this method may be used to set a null parameter of any JDBC type.
     * If the parameter does not have a user-defined or REF type, the given
     * typeName is ignored. <p>
     * <!-- end generic documentation -->
     *
     * <!-- start release-specific documentation -->
     * <div class="ReleaseSpecificDocumentation">
     * <h3>HSQLDB-Specific Information:</h3> <p>
     *
     * HSQLDB ignores the sqlType and typeName arguments.
     * </div>
     * <!-- end release-specific documentation -->
     *
     * @param paramIndex the first parameter is 1, the second is 2, ...
     * @param sqlType a value from <code>java.sql.Types</code>
     * @param typeName the fully-qualified name of an SQL user-defined type;
     * ignored if the parameter is not a user-defined type or REF
     * @exception SQLException if a database access error occurs
     * @since JDK 1.2 (JDK 1.1.x developers: read the new overview for
     *   jdbcPreparedStatement)
     */
    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        setParameter(paramIndex, null);
    }

    public void setURL(int parameterIndex, java.net.URL x) throws SQLException {
        throw Util.notSupported();
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        checkClosed();
        if (pmd == null) {
            pmd = new jdbcParameterMetaData(pmdDescriptor);
        }
        return (ParameterMetaData) pmd;
    }

    /**
     * Constructs a statement that produces results of the requested
     * <code>type</code>. <p>
     *
     * A prepared statement must be a single SQL statement. <p>
     *
     * @param c the Connection used execute this statement
     * @param sql the SQL statement this object represents
     * @param type the type of result this statement will produce
     * @throws HsqlException if the statement is not accepted by the database
     * @throws SQLException if preprocessing by driver fails
     */
    jdbcPreparedStatement(jdbcConnection c, String sql, int type) throws HsqlException, SQLException {
        super(c, type);
        sql = c.nativeSQL(sql);
        resultOut.setResultType(ResultConstants.SQLPREPARE);
        resultOut.setMainString(sql);
        Result in = connection.sessionProxy.execute(resultOut);
        if (in.isError()) {
            Util.throwError(in);
        }
        Iterator i;
        i = in.iterator();
        try {
            Object[] row;
            row = (Object[]) i.next();
            statementID = ((Result) row[0]).getStatementID();
            row = (Object[]) i.next();
            rsmdDescriptor = (Result) row[0];
            isRowCount = rsmdDescriptor.mode == ResultConstants.UPDATECOUNT;
            row = (Object[]) i.next();
            pmdDescriptor = (Result) row[0];
            parameterTypes = pmdDescriptor.metaData.getParameterTypes();
            parameterValues = new Object[parameterTypes.length];
            parameterSet = new boolean[parameterTypes.length];
            parameterModes = pmdDescriptor.metaData.paramMode;
        } catch (Exception e) {
            throw Trace.error(Trace.GENERAL_ERROR, e.toString());
        }
        resultOut = new Result(ResultConstants.SQLEXECUTE, parameterTypes, statementID);
        this.sql = sql;
    }

    /**
     * Checks if execution does or does not generate a single row
     * update count, throwing if the argument, yes, does not match. <p>
     *
     * @param yes if true, check that execution generates a single
     *      row update count, else check that execution generates
     *      something other than a single row update count.
     * @throws SQLException if the argument, yes, does not match
     */
    protected void checkIsRowCount(boolean yes) throws SQLException {
        if (yes != isRowCount) {
            int msg = yes ? Trace.JDBC_STATEMENT_NOT_ROW_COUNT : Trace.JDBC_STATEMENT_NOT_RESULTSET;
            throw Util.sqlException(msg);
        }
    }

    /**
     * Checks if the specified parameter index value is valid in terms of
     * setting an IN or IN OUT parameter value. <p>
     *
     * @param i The parameter index to check
     * @throws SQLException if the specified parameter index is invalid
     */
    protected void checkSetParameterIndex(int i, boolean isStream) throws SQLException {
        int mode;
        String msg;
        checkClosed();
        if (i < 1 || i > parameterValues.length) {
            msg = "parameter index out of range: " + i;
            throw Util.sqlException(Trace.INVALID_JDBC_ARGUMENT, msg);
        }
        if (isStream) {
            if (parameterStream == null) {
                parameterStream = new boolean[parameterTypes.length];
            }
            parameterStream[i - 1] = true;
            parameterSet[i - 1] = false;
        } else {
            parameterSet[i - 1] = true;
        }
    }

    /**
     * Called just before execution or adding to batch, this ensures all the
     * parameters have been set.<p>
     *
     * If a parameter has been set using a stream method, it should be set
     * again for the next reuse. When set using other methods, the parameter
     * setting is retained for the next use.
     */
    private void checkParametersSet() throws SQLException {
        ;
    }

    /**
     * The internal parameter value setter always converts the parameter to
     * the Java type required for data transmission. Target BINARY and OTHER
     * types are converted directly. All other target types are converted
     * by Column.convertObject(). This also normalizes DATETIME values.
     *
     * @param i parameter index
     * @param o object
     * @throws SQLException if either argument is not acceptable.
     */
    private void setParameter(int i, Object o) throws SQLException {
        checkSetParameterIndex(i, false);
        i--;
        if (o == null) {
            parameterValues[i] = null;
            return;
        }
        int outType = parameterTypes[i];
        try {
            switch(outType) {
                case Types.OTHER:
                    o = new JavaObject((Serializable) o);
                    break;
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    if (!(o instanceof byte[])) {
                        throw Util.sqlException(Trace.error(Trace.INVALID_CONVERSION));
                    }
                    o = new Binary((byte[]) o, !connection.isNetConn);
                    break;
                case Types.DATE:
                    if (o instanceof java.util.Date) {
                        long t = HsqlDateTime.getNormalisedDate(((java.util.Date) o).getTime());
                        o = new Date(t);
                    } else {
                        o = Column.convertObject(o, outType);
                    }
                    break;
                case Types.TIME:
                    if (o instanceof java.util.Date) {
                        long m = HsqlDateTime.getNormalisedTime(((java.util.Date) o).getTime());
                        o = new Time(m);
                    } else {
                        o = Column.convertObject(o, outType);
                    }
                    break;
                case Types.TIMESTAMP:
                    if (o instanceof Timestamp) {
                        long m = ((Timestamp) o).getTime();
                        int n = ((Timestamp) o).getNanos();
                        o = new Timestamp(m);
                        ((Timestamp) o).setNanos(n);
                    } else {
                        o = Column.convertObject(o, outType);
                    }
                    break;
                default:
                    o = Column.convertObject(o, outType);
                    break;
            }
        } catch (HsqlException e) {
            Util.throwError(e);
        }
        parameterValues[i] = o;
    }

    /**
     * Used with int and narrower integral primitives
     * @param i parameter index
     * @param value object to set
     * @throws SQLException if either argument is not acceptable
     */
    private void setIntParameter(int i, int value) throws SQLException {
        checkSetParameterIndex(i, false);
        int outType = parameterTypes[i - 1];
        switch(outType) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                Object o = new Integer(value);
                parameterValues[i - 1] = o;
                break;
            default:
                setLongParameter(i, value);
        }
    }

    /**
     * Used with long and narrower integral primitives. Conversion to BINARY
     * or OTHER types will throw here and not passed to setParameter().
     *
     * @param i parameter index
     * @param value object to set
     * @throws SQLException if either argument is not acceptable
     */
    private void setLongParameter(int i, long value) throws SQLException {
        checkSetParameterIndex(i, false);
        int outType = parameterTypes[i - 1];
        switch(outType) {
            case Types.BIGINT:
                Object o = new Long(value);
                parameterValues[i - 1] = o;
                break;
            case Types.BINARY:
            case Types.OTHER:
                throw Util.sqlException(Trace.error(Trace.INVALID_CONVERSION));
            default:
                setParameter(i, new Long(value));
        }
    }

    /**
     * This method should always throw if called for a PreparedStatement or
     * CallableStatment.
     *
     * @param sql ignored
     * @throws SQLException always
     */
    public void addBatch(String sql) throws SQLException {
        throw Util.notSupported();
    }

    /**
     * This method should always throw if called for a PreparedStatement or
     * CallableStatment.
     *
     * @param sql ignored
     * @throws SQLException always
     * @return nothing
     */
    public ResultSet executeQuery(String sql) throws SQLException {
        throw Util.notSupported();
    }

    /**
     * This method should always throw if called for a PreparedStatement or
     * CallableStatment.
     *
     * @param sql ignored
     * @throws SQLException always
     * @return nothing
     */
    public boolean execute(String sql) throws SQLException {
        throw Util.notSupported();
    }

    /**
     * This method should always throw if called for a PreparedStatement or
     * CallableStatment.
     *
     * @param sql ignored
     * @throws SQLException always
     * @return nothing
     */
    public int executeUpdate(String sql) throws SQLException {
        throw Util.notSupported();
    }

    /**
     * Does the specialized work required to free this object's resources and
     * that of it's parent class. <p>
     *
     * @throws SQLException if a database access error occurs
     */
    public synchronized void close() throws SQLException {
        if (isClosed()) {
            return;
        }
        HsqlException he = null;
        try {
            if (!connection.isClosed) {
                connection.sessionProxy.execute(Result.newFreeStmtRequest(statementID));
            }
        } catch (HsqlException e) {
            he = e;
        }
        parameterValues = null;
        parameterSet = null;
        parameterStream = null;
        parameterTypes = null;
        parameterModes = null;
        rsmdDescriptor = null;
        pmdDescriptor = null;
        rsmd = null;
        pmd = null;
        super.close();
        if (he != null) {
            throw Util.sqlException(he);
        }
    }

    /**
     * Retrieves a String representation of this object.  <p>
     *
     * The representation is of the form: <p>
     *
     * class-name@hash[sql=[char-sequence], parameters=[p1, ...pi, ...pn]] <p>
     *
     * p1, ...pi, ...pn are the String representations of the currently set
     * parameter values that will be used with the non-batch execution
     * methods. <p>
     *
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String sql;
        Object[] pv;
        sb.append(super.toString());
        sql = this.sql;
        pv = parameterValues;
        if (sql == null || pv == null) {
            sb.append("[closed]");
            return sb.toString();
        }
        sb.append("[sql=[").append(sql).append("]");
        if (pv.length > 0) {
            sb.append(", parameters=[");
            for (int i = 0; i < pv.length; i++) {
                sb.append('[');
                sb.append(pv[i]);
                sb.append("], ");
            }
            sb.setLength(sb.length() - 2);
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }
}
