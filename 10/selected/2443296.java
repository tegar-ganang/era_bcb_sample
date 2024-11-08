package com.jcorporate.expresso.core.dbobj;

import com.jcorporate.expresso.core.db.DBConnection;
import com.jcorporate.expresso.core.db.DBConnectionPool;
import com.jcorporate.expresso.core.db.DBException;
import com.jcorporate.expresso.core.misc.StringUtil;
import com.jcorporate.expresso.kernel.util.FastStringBuffer;
import org.apache.log4j.Logger;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Many databases support arbitrarily large objects.  The problem with large objects
 * is that their symantics are significantly different compared to other standard
 * database fields.  Specifically some highlighted differences are:
 * <ul>
 * <li><b>Size -</b> The arbitrary size of LOB objects can significantly consume system
 * resources if you automatically load all LOBS along with the rest of the
 * data object</li>
 * <li><b>JDBC Interface -</b> Many JDBC Drivers require you to use streams to retrieve LOBs from a database
 * as opposed to standard objects loaded from a connection.</li>
 * <li><b>Transaction Requirements -</b>Many JDBC Drivers require to update BLOBS within
 * an explicitly committed transaction</li>
 * <li><b>Object Oriented Wrapping -</b> LOBs by definition are of unknown objects
 * Therefore, it is difficult for an Object Oriented System to deal with the
 * arbitrariness of the object.  Some sort of data type mapping must be supplied
 * </li>
 * </ul>
 *
 * <p>To support this, LOB Support is designed to provide several facilities to
 * assist with this:<p>
 *
 * <p><b>Delayed Loading:</b> The LOBSupport class is activated by DBObjects only
 * when a LOB data field's data is requested.</p>
 *
 * <p><b>Separate command execution</b> The LOBSupport executes a second statement
 * to the database in the form of a PreparedStatement to retrieve all the Large
 * Objects</p>
 *
 * <p><b>Transaction usage IF supported</b> If the JDBC driver indicates it supports
 * transactions, LOBSupport will automatically attempt to setAutoCommit(false) and
 * call explitic commit()s.  If you are already within a transaction, LOBSupport
 * will not attempt to get another transaction</p>
 *
 * <h4>Usage</h4>
 * <p>Most often you would not use this class directly, but instead call the
 * similar <code>DBObject</code> method calls.  However, sometimes access to
 * the lower level capabilities of these functions are useful for performance
 * sake and therefore...</p>
 *
 * @author Michael Rimov
 * @deprecated since Expresso 5.3 the ways it worked on LOBs wasn't remotely
 * efficient and has been replaced by the implementation in com.jcorporate.expresso.
 * core.dataobjects.jdbc.LOBField
 * @see com.jcorporate.expresso.core.dataobjcets.jdbc.LOBField
 * @see com.jcorporate.expresso.services.dbobj.MediaDBObject
 */
public class LOBSupport {

    protected static LOBSupport theInstance = null;

    private static Logger log = Logger.getLogger("com.jcorporate" + ".expresso.core.dbobj.LOBSupport");

    /**
     * Default Constructor
     */
    protected LOBSupport() {
    }

    /**
     * Singleton implementation. Use this to get to the LOB helper functions
     * @return an instantiated LOBSupport class
     */
    public static synchronized LOBSupport getInstance() {
        if (theInstance == null) {
            theInstance = new LOBSupport();
        }
        return theInstance;
    }

    public String getCLOB(DBObject baseObject, String fieldName) throws DBException {
        DBConnectionPool aPool = DBConnectionPool.getInstance(baseObject.getDataContext());
        DBConnection localConnection = aPool.getConnection();
        String returnValue = null;
        try {
            returnValue = this.getCLOB(baseObject, fieldName, localConnection);
        } finally {
            aPool.release(localConnection);
        }
        return returnValue;
    }

    /**
     * Retrieves the CLOB data for the specified DBObject and the specified
     * fieldName
     * @param baseObject The dbobject containing all the table information, and
     * key fields set.
     * @param fieldName The name of the field to retrieve as a LOB.
     * @param theConnection a <code>DBConnection</code> object that has already
     * been retrieved from the <code>DBConnectionPool</code>  May be null although
     * it's not recommended for performance.
     * @return an array of bytes that will contain the BLOB
     * @throws DBException if there's an error executing the statement
     */
    public String getCLOB(DBObject baseObject, String fieldName, DBConnection theConnection) throws DBException {
        prepSelectResultSet(baseObject, fieldName, theConnection);
        if (theConnection.next()) {
            return StringUtil.notNull(theConnection.getString(1));
        }
        return "";
    }

    /**
     * Writes a LONG Character string to the database.
     * @param baseObject The object that contains the metadata for this BLOB
     * @param fieldName the name of the field that is the BLOB field
     * @param theData the data in a <code>String</code>
     * @param theConnection an already allocated DBConnection object Currently
     * may <b>not</b> be null
     * @throws DBException upon error
     */
    public void setCLOB(DBObject baseObject, String fieldName, String theData, DBConnection theConnection) throws DBException {
        PreparedStatement preparedStatement = prepUpdate(baseObject, fieldName, theConnection);
        try {
            if ("interbase.interclient.Driver".equals(theConnection.getDBDriver())) {
                byte[] data = theData.getBytes();
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(data);
                preparedStatement.setAsciiStream(1, bis, data.length);
            } else {
                java.io.Reader r = new java.io.StringReader(theData);
                preparedStatement.setCharacterStream(1, r, theData.length());
            }
        } catch (SQLException ex) {
            throw new DBException("Unable to set CharacterStream to CLOB object.", ex);
        }
        finalizeUpdate(theConnection);
    }

    /**
     * Returns a single stream for a LOB object.  This single LOB object is
     * not wrapped by any javax.activation code and therefore is strictly up to
     * the code creator on how to use the Input Stream.
     * @param baseObject The dbobject containing all the table information, and
     * key fields set.
     * @param fieldName The name of the field to retrieve as a LOB.
     * @param theConnection a <code>DBConnection</code> object that has already
     * been retrieved from the <code>DBConnectionPool</code>
     * @return an array of bytes that will contain the BLOB
     * @throws DBException if there's an error executing the statement
     */
    public byte[] getBLOB(DBObject baseObject, String fieldName, DBConnection theConnection) throws DBException {
        prepSelectResultSet(baseObject, fieldName, theConnection);
        if (theConnection.next()) {
            byte retVal[] = theConnection.getBytes(1);
            if (retVal == null) {
                return new byte[0];
            } else {
                return retVal;
            }
        }
        return new byte[0];
    }

    /**
     * Updates the underlying table with a BLOB object.
     * @param baseObject The object that contains the metadata for this BLOB
     * @param fieldName the name of the field that is the BLOB field
     * @param theData the data in a byte array
     * @param theConnection an already allocated DBConnection object
     * @throws DBException upon error
     */
    public void setBLOB(DBObject baseObject, String fieldName, byte theData[], DBConnection theConnection) throws DBException {
        PreparedStatement preparedStatement = prepUpdate(baseObject, fieldName, theConnection);
        try {
            preparedStatement.setBytes(1, theData);
        } catch (SQLException ex) {
            throw new DBException("Error setting BLOB object", ex);
        }
        finalizeUpdate(theConnection);
        return;
    }

    /**
     * Updates the underlying table with a BLOB object.  Same as the byte[] method
     * but uses InputStreams intead
     * @param baseObject The object that contains the metadata for this BLOB
     * @param fieldName the name of the field that is the BLOB field
     * @param theData the data as an InputStream
     * @param dataLength the total length to be read from the InputStream
     * @param theConnection an already allocated DBConnection object
     * @throws DBException upon error
     */
    public void setBLOB(DBObject baseObject, String fieldName, InputStream theData, int dataLength, DBConnection theConnection) throws DBException {
        PreparedStatement preparedStatement = prepUpdate(baseObject, fieldName, theConnection);
        try {
            preparedStatement.setBinaryStream(1, theData, dataLength);
        } catch (SQLException ex) {
            throw new DBException("Error setting BLOB object", ex);
        }
        finalizeUpdate(theConnection);
        return;
    }

    /**
     * Internal helper function that does the guts of the work
     * @param baseObject The object that contains the metadata for this BLOB
     * @param fieldName the name of the field that is the BLOB field
     * @param theConnection an already allocated DBConnection object.  <b>This
     * function modifies the state of theConnection by allocating a prepared
     * statement</b>
     * @throws DBException upon error
     */
    protected void prepSelectResultSet(DBObject baseObject, String fieldName, DBConnection theConnection) throws DBException {
        FastStringBuffer prepStatement = FastStringBuffer.getInstance();
        try {
            prepStatement.append("SELECT ");
            prepStatement.append(fieldName);
            prepStatement.append(" from ");
            prepStatement.append(baseObject.getJDBCMetaData().getTargetTable());
            String whereClause = baseObject.buildWhereClause(false);
            prepStatement.append(whereClause);
            String thePrepString = prepStatement.toString();
            if (log.isDebugEnabled()) {
                log.debug("Preparing prepared statement:  " + thePrepString);
            }
            PreparedStatement prep = theConnection.createPreparedStatement(thePrepString);
            if (prep == null) {
                throw new DBException("Unable to create prepared statement for CLOB retrieval." + "  Check DBConnection log for details");
            }
            theConnection.execute();
            if (log.isDebugEnabled()) {
                log.debug("Succesfully executed prepared statement");
            }
        } finally {
            prepStatement.release();
            prepStatement = null;
        }
    }

    /**
     * Internal helper function to prepare a LOB update
     * @param baseObject The object that contains the metadata for this BLOB
     * @param fieldName the name of the field that is the BLOB field
     * @param theConnection an already allocated DBConnection object
     * @return a created PreparedStatement object
     * @throws DBException
     */
    protected PreparedStatement prepUpdate(DBObject baseObject, String fieldName, DBConnection theConnection) throws DBException {
        String whereClause = baseObject.buildWhereClause(false);
        FastStringBuffer prepStatement = FastStringBuffer.getInstance();
        String theSQL = null;
        try {
            prepStatement.append("UPDATE ");
            prepStatement.append(baseObject.getJDBCMetaData().getTargetTable());
            prepStatement.append(" SET ");
            prepStatement.append(fieldName);
            prepStatement.append(" = ? ");
            prepStatement.append(whereClause);
            theSQL = prepStatement.toString();
        } finally {
            prepStatement.release();
            prepStatement = null;
        }
        return theConnection.createPreparedStatement(theSQL);
    }

    protected void finalizeUpdate(DBConnection theConnection) throws DBException {
        boolean alreadyInTransaction = !(theConnection.getAutoCommit());
        boolean success = false;
        if (!alreadyInTransaction && theConnection.supportsTransactions()) {
            if (log.isDebugEnabled()) {
                log.debug("Turning off auto-commit");
            }
            theConnection.setAutoCommit(false);
        }
        try {
            theConnection.executeUpdate(null);
            success = true;
        } finally {
            if (success == false) {
                if (!alreadyInTransaction && theConnection.supportsTransactions()) {
                    if (log.isDebugEnabled()) {
                        log.debug("rolling back");
                    }
                    theConnection.rollback();
                }
            }
            if (!alreadyInTransaction && theConnection.supportsTransactions()) {
                if (log.isDebugEnabled()) {
                    log.debug("Finishing commit and turning auto-commit back to true");
                }
                theConnection.commit();
                theConnection.setAutoCommit(true);
            }
        }
    }
}
