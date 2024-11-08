package com.jcorporate.expresso.core.dataobjects.jdbc;

import com.jcorporate.expresso.core.dataobjects.DataException;
import com.jcorporate.expresso.core.dataobjects.DataFieldMetaData;
import com.jcorporate.expresso.core.db.DBConnection;
import com.jcorporate.expresso.core.db.DBConnectionPool;
import com.jcorporate.expresso.core.db.DBException;
import com.jcorporate.expresso.core.db.TypeMapper;
import com.jcorporate.expresso.kernel.util.FastStringBuffer;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class provides a low level BLOB capability while still keeping the
 * programmer isolated from SQL syntax details.
 * <p>The general usage is as follows:<br/>
 * <code>
 * &nbsp;MyDBObject myObj.setField("key1",1);<br/>
 * &nbsp;LobField query = new LobField();<br/>
 * &nbsp;query.setCriteria(myObj);<br/>
 * &nbsp;java.io.InputStream inputStream = getBlobStream("blobFieldName");
 * &nbsp; //Do whatever you want with the stream <br/>
 * &nbsp; inputStream.flush();
 * &nbsp; inputStream.close();
 * &nbsp; query.close();
 * </code>
 * </p>
 * <p>This class requires a JDBC 2 compliant driver for the BLOB/CLOB data types.
 * Some drivers do not support these features, at which point you'll want
 * to use the getBlobStream/getClobStream/getClobAsciiStream methods instead
 * </p>
 *
 * @author Michael Rimov
 * @version $Revision: 3 $ on  $Date: 2006-03-01 06:17:08 -0500 (Wed, 01 Mar 2006) $
 * @since Expresso 5.1
 */
public class LobField {

    /**
     * The dataobject to use to build the SQL search/update statements.
     */
    protected JDBCDataObject criteria = null;

    /**
     * The log4j Logger to use.
     */
    private Logger log = Logger.getLogger(LobField.class);

    protected DBConnection myConnection = null;

    /**
     * Default constructor.  Currently does nothing.
     */
    public LobField() {
    }

    /**
     * Set the search criteria for the blob. All key fields must be present
     * as this does a full retrieve() rather than a search on the data.  Otherwise
     * the object will throw an exception .
     *
     * @param newCriteria a filled out JDBCObject (DBObject derived classes work)
     * with all keys present
     * @throws DataException if all keys are not present.
     */
    public void setCriteria(JDBCDataObject newCriteria) throws DataException {
        if (newCriteria == null) {
            throw new IllegalArgumentException("Criteria cannot be null");
        }
        criteria = newCriteria;
    }

    /**
     * Protected method to get at the criteria object from any derived classes
     *
     * @return JDBCDataObject or null if no criteria has been set
     */
    protected JDBCDataObject getCriteria() {
        return criteria;
    }

    /**
     * Retrieves a <code>java.sql.Blob</code> object given the criteria object
     * set previously.
     * @param fieldName the name of the field to retrieve
     * @return java.sql.Blob for the field
     * @throws DataException if there is an error finding the object, an error
     * retrieving the Blob from the system, or other database communication
     * errors.
     */
    public Blob getBlob(String fieldName) throws DataException {
        if (getCriteria() == null) {
            throw new IllegalArgumentException("Criteria must be set before calling getBLob");
        }
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        return getBLOB(getCriteria(), fieldName, myConnection);
    }

    /**
     * Retrieves a <code>java.sql.Clob</code> object given the criteria object
     * set previously.
     * @param fieldName the name of the field to retrieve
     * @return java.sql.Clob for the field
     * @throws DataException if there is an error finding the object, an error
     * retrieving the Clob from the system, or other database communication
     * errors.
     */
    public Clob getClob(String fieldName) throws DataException {
        if (getCriteria() == null) {
            throw new IllegalArgumentException("Criteria must be set before calling getClob");
        }
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for CLOB Retrieval", ex);
        }
        return getCLOB(getCriteria(), fieldName, myConnection);
    }

    /**
     * Retrieve an input stream for a binary object stored in the database.
     * @param fieldName the field name to retrieve.
     * @return java.io.InputStream representing the BLOB object
     * @throws DataException upon error
     */
    public InputStream getBlobStream(String fieldName) throws DataException {
        if (getCriteria() == null) {
            throw new IllegalArgumentException("Criteria must be set before calling getBLob");
        }
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        try {
            prepSelectResultSet(getCriteria(), fieldName, myConnection);
            if (myConnection.next()) {
                return myConnection.getBinaryStream(1);
            } else {
                return null;
            }
        } catch (DBException ex) {
            throw new DataException("Error getting binary stream", ex);
        }
    }

    /**
     * Retrieve a java.io.Reader a.k.a Unicode stream for a CLOB field.
     * @param fieldName the name of the field to retrieve.
     * @return java.io.Reader for the Unicode CLOB stream stored in the database
     * @throws DataException upon error retrieving the CLOB
     */
    public java.io.Reader getClobStream(String fieldName) throws DataException {
        if (getCriteria() == null) {
            throw new IllegalArgumentException("Criteria must be set before calling getBLob");
        }
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        return this.getCLOBReader(getCriteria(), fieldName, myConnection);
    }

    /**
     * CLOB convenience method.  Reads the entire stream into a string.  Note
     * that if your field is large, this may take large amounts of memory
     * to perform this operation.  It is recommended to use getClobStream()
     * for most purposes.
     * <p>Note that this method is not supported by InterBase/InterClient 2 which
     * does not support Unicode streams.  Use getClobAsciiStream instead</p>
     * @param fieldName the name of the field to retrieve
     * @return java.lang.String containing the entire contents of the CLOB
     * field.
     * @throws DataException upon error.
     */
    public String getClobString(String fieldName) throws DataException {
        java.io.Reader is = this.getClobStream(fieldName);
        if (is == null) {
            return null;
        }
        FastStringBuffer fsb = FastStringBuffer.getInstance();
        try {
            char[] buf = new char[1024];
            int bytesRead;
            while ((bytesRead = is.read(buf)) != -1) {
                fsb.append(buf, 0, bytesRead);
            }
            return fsb.toString();
        } catch (java.io.IOException ex) {
            throw new DataException("I/O Exception reading Character Stream");
        } finally {
            fsb.release();
        }
    }

    /**
     * Retrieve a java.io.InputStream a.k.a. ASCII stream for a CLOB field.
     * @param fieldName the name of the field to retrieve.
     * @return java.io.Reader for the ASCII CLOB stream stored in the database
     * @throws DataException upon error retrieving the CLOB
     */
    public InputStream getClobAsciiStream(String fieldName) throws DataException {
        try {
            Clob theClob = getClob(fieldName);
            return theClob.getAsciiStream();
        } catch (SQLException ex) {
            throw new DataException("Error getting clob ascii stream: " + fieldName, ex);
        }
    }

    /**
     * Saves an InputStream into the database given the criteria and the fieldname
     * (Criteria should have been previously set).
     * @param fieldName The name of the field to save the Stream to.
     * @param data a java.io.InputStream object to save to the field.  May be null
     * if you want the field to be null.
     * @param dataSize the length of the stream to save.
     * @throws DataException upon database communications error.
     * @throws IllegalArgumentException if fieldName is null.
     */
    public void saveBlob(String fieldName, InputStream data, int dataSize) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
                if ("org.hsqldb.jdbcDriver".equals(myConnection.getDBDriver())) {
                    if (dataSize > 1024 * 200) {
                        throw new DataException("HSQLDB can only store maxium of 200K files");
                    }
                }
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        if ("oracle.jdbc.driver.OracleDriver".equals(myConnection.getDBDriver())) {
            oracleSaveBlob(fieldName, data, dataSize);
            return;
        }
        PreparedStatement preparedStatement = prepUpdate(getCriteria(), fieldName, myConnection);
        try {
            if (data == null) {
                DataFieldMetaData metaData = getCriteria().getFieldMetaData(fieldName);
                int typeCode = TypeMapper.getInstance(getCriteria().getMappedDataContext()).getJavaSQLType(metaData.getTypeString());
                preparedStatement.setNull(1, typeCode);
            } else {
                preparedStatement.setBinaryStream(1, data, dataSize);
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to set CharacterStream to CLOB object.", ex);
        } catch (DBException ex) {
            throw new DataException("Unable to get Type Mapping information for Clob field", ex);
        }
        finalizeUpdate(myConnection);
    }

    /**
     * Saves a <code>java.sql.Blob</code> to the record matching the criteria
     * earlier set.
     * @param fieldName the name of the field to save to
     * @param data the <code>java.sql.Blob</code> based object to save to the
     * database.
     * @throws DataException upon database communication error
     * @throws IllegalArgumentException if data is null
     */
    public void saveBlob(String fieldName, Blob data) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        PreparedStatement preparedStatement = prepUpdate(getCriteria(), fieldName, myConnection);
        try {
            if (data == null) {
                DataFieldMetaData metaData = getCriteria().getFieldMetaData(fieldName);
                int typeCode = TypeMapper.getInstance(getCriteria().getMappedDataContext()).getJavaSQLType(metaData.getTypeString());
                preparedStatement.setNull(1, typeCode);
            } else {
                preparedStatement.setBlob(1, data);
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to set CharacterStream to CLOB object.", ex);
        } catch (DBException ex) {
            throw new DataException("Unable to get type mapping information", ex);
        }
        finalizeUpdate(myConnection);
    }

    /**
     * Saves an InputStream into the database given the criteria and the fieldname
     * (Criteria should have been previously set).
     * @param fieldName The name of the field to save the Stream to.
     * @param data a java.io.InputStream object to save to the field.  May be null
     * if you want the field to be null.
     * @param length The size of the CLOB stream to save to the database
     * @throws IllegalArgumentException if fieldName is null.
     * @throws DataException upon database communications error.
     */
    public void saveClob(String fieldName, InputStream data, int length) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        if ("oracle.jdbc.driver.OracleDriver".equals(myConnection.getDBDriver())) {
            oracleSaveClob(fieldName, data, length);
            return;
        }
        PreparedStatement preparedStatement = prepUpdate(getCriteria(), fieldName, myConnection);
        try {
            if (data == null) {
                DataFieldMetaData metaData = getCriteria().getFieldMetaData(fieldName);
                int typeCode = TypeMapper.getInstance(getCriteria().getMappedDataContext()).getJavaSQLType(metaData.getTypeString());
                preparedStatement.setNull(1, typeCode);
            } else {
                preparedStatement.setAsciiStream(1, data, length);
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to set CharacterStream to CLOB object.", ex);
        } catch (DBException ex) {
            throw new DataException("Unable to get type mapping information for CLOB object", ex);
        }
        finalizeUpdate(myConnection);
    }

    /**
     * Saves an InputStream into the database given the criteria and the fieldname
     * (Criteria should have been previously set).
     * @param fieldName The name of the field to save the Stream to.
     * @param data a java.io.Reader object to save to the field.  May be null
     * if you want the field to be null.
     * @param length The size of the data stream to save to the database
     * @throws DataException upon database communications error.
     * @throws IllegalArgumentException if fieldName is null.
     */
    public void saveClob(String fieldName, java.io.Reader data, int length) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        if ("interbase.interclient.Driver".equals(myConnection.getDBDriver())) {
            throw new DataException("InterBase Interclient 2 cannot support unicode data. " + " Must use saveClob(String, InputStream, length)" + "instead");
        }
        if ("oracle.jdbc.driver.OracleDriver".equals(myConnection.getDBDriver())) {
            if (data == null) {
                oracleSaveClob(fieldName, (StringReader) null, 0);
            } else {
                oracleSaveClob(fieldName, data, length);
            }
            return;
        }
        PreparedStatement preparedStatement = prepUpdate(getCriteria(), fieldName, myConnection);
        try {
            if (data == null) {
                DataFieldMetaData metaData = getCriteria().getFieldMetaData(fieldName);
                int typeCode = TypeMapper.getInstance(getCriteria().getMappedDataContext()).getJavaSQLType(metaData.getTypeString());
                preparedStatement.setNull(1, typeCode);
            } else {
                preparedStatement.setCharacterStream(1, data, length);
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to set CharacterStream to CLOB object.", ex);
        } catch (DBException ex) {
            throw new DataException("Unable to get type mapping information for CLOB field", ex);
        }
        finalizeUpdate(myConnection);
    }

    /**
     * Saves a string to a CLOB field.
     * @param fieldName the name of the field to save to.
     * @param data the String value to save to the field.
     * @throws DataException upon error
     */
    public void saveClob(String fieldName, String data) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        if ("oracle.jdbc.driver.OracleDriver".equals(myConnection.getDBDriver())) {
            if (data == null) {
                oracleSaveClob(fieldName, (StringReader) null, 0);
            } else {
                oracleSaveClob(fieldName, new StringReader(data), data.length());
            }
            return;
        }
        PreparedStatement preparedStatement = prepUpdate(getCriteria(), fieldName, myConnection);
        try {
            if (data == null) {
                DataFieldMetaData metaData = getCriteria().getFieldMetaData(fieldName);
                int typeCode = TypeMapper.getInstance(getCriteria().getMappedDataContext()).getJavaSQLType(metaData.getTypeString());
                preparedStatement.setNull(1, typeCode);
            } else {
                if ("interbase.interclient.Driver".equals(myConnection.getDBDriver())) {
                    byte[] dataArray = data.getBytes();
                    java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(dataArray);
                    preparedStatement.setAsciiStream(1, bis, dataArray.length);
                } else {
                    java.io.Reader r = new java.io.StringReader(data);
                    preparedStatement.setCharacterStream(1, r, data.length());
                }
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to set CharacterStream to CLOB object.", ex);
        } catch (DBException ex) {
            throw new DataException("Unable to get type information for CLOB field", ex);
        }
        finalizeUpdate(myConnection);
    }

    /**
     * Saves an InputStream into the database given the criteria and the fieldname
     * (Criteria should have been previously set).
     * @param fieldName The name of the field to save the Stream to.
     * @param data a java.io.Reader object to save to the field.  May be null
     * if you want the field to be null.
     * @throws DataException upon database communications error.
     * @throws IllegalArgumentException if fieldName is null.
     */
    public void saveClob(String fieldName, Clob data) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        PreparedStatement preparedStatement = prepUpdate(getCriteria(), fieldName, myConnection);
        try {
            if (data == null) {
                DataFieldMetaData metaData = getCriteria().getFieldMetaData(fieldName);
                int typeCode = TypeMapper.getInstance(getCriteria().getMappedDataContext()).getJavaSQLType(metaData.getTypeString());
                preparedStatement.setNull(1, typeCode);
            } else {
                preparedStatement.setClob(1, data);
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to set CharacterStream to CLOB object.", ex);
        } catch (DBException ex) {
            throw new DataException("Unable to get type mapping information for CLOB field", ex);
        }
        finalizeUpdate(myConnection);
    }

    /**
     * Close the query resources held by the object.  This should be wrapped
     * in a try/finally block so that database connection resources are not
     * left floating in limbo.
     */
    public void close() {
        if (myConnection != null) {
            if (log.isDebugEnabled()) {
                log.debug("Closing and releasing LOB connection");
            }
            myConnection.release();
            myConnection = null;
        }
    }

    /**
     * Override of base object finalization to make sure that the database
     * resources are closed if for some reason they haven't had this done
     * to them already.
     * @throws java.lang.Throwable
     */
    protected void finalize() throws java.lang.Throwable {
        if (myConnection != null) {
            log.warn("LobField was not closed before Garbage Collection!");
            close();
        }
        super.finalize();
    }

    /**
     * Retrieves the CLOB data for the specified DBObject and the specified
     * fieldName
     * @param baseObject The dbobject containing all the table information, and
     * key fields set.
     * @param fieldName The name of the field to retrieve as a LOB.
     * @param theConnection a <code>DBConnection</code> object that has already
     * been retrieved from the <code>DBConnectionPool</code>
     * @return a java.sql.CLOB object.
     * @throws DataException if there's an error executing the statement
     */
    private Clob getCLOB(JDBCDataObject baseObject, String fieldName, DBConnection theConnection) throws DataException {
        prepSelectResultSet(baseObject, fieldName, theConnection);
        try {
            if (theConnection.next()) {
                return theConnection.getClob(fieldName);
            }
        } catch (DBException ex) {
            throw new DataException("Error getting CLOB object from connection", ex);
        }
        return null;
    }

    /**
     * Retrieves the CLOB data for the specified DBObject and the specified
     * fieldName
     * @param baseObject The dbobject containing all the table information, and
     * key fields set.
     * @param fieldName The name of the field to retrieve as a LOB.
     * @param theConnection a <code>DBConnection</code> object that has already
     * been retrieved from the <code>DBConnectionPool</code>
     * @return a Reader object.
     * @throws DataException if there's an error executing the statement
     */
    private java.io.Reader getCLOBReader(JDBCDataObject baseObject, String fieldName, DBConnection theConnection) throws DataException {
        prepSelectResultSet(baseObject, fieldName, theConnection);
        ResultSet rs = theConnection.getResultSet();
        try {
            if (rs.next()) {
                try {
                    return rs.getCharacterStream(1);
                } catch (SQLException ex) {
                    return new java.io.StringReader(rs.getString(1));
                }
            }
        } catch (java.sql.SQLException ex) {
            throw new DataException("Error getting CLOB object from connection", ex);
        }
        return null;
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
     * @throws DataException if there's an error executing the statement
     */
    private Blob getBLOB(JDBCDataObject baseObject, String fieldName, DBConnection theConnection) throws DataException {
        try {
            prepSelectResultSet(baseObject, fieldName, theConnection);
            if (theConnection.next()) {
                return theConnection.getBlob(1);
            } else {
                return null;
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Blob Field", ex);
        }
    }

    /**
     * Internal helper function that does the guts of the work
     * @param baseObject The object that contains the metadata for this BLOB
     * @param fieldName the name of the field that is the BLOB field
     * @param theConnection an already allocated DBConnection object.  <b>This
     * function modifies the state of theConnection by allocating a prepared
     * statement</b>
     * @throws DataException upon error
     */
    protected void prepSelectResultSet(JDBCDataObject baseObject, String fieldName, DBConnection theConnection) throws DataException {
        try {
            FastStringBuffer prepStatement = FastStringBuffer.getInstance();
            prepStatement.append("SELECT ");
            prepStatement.append(fieldName);
            prepStatement.append(" from ");
            prepStatement.append(baseObject.getJDBCMetaData().getTargetTable());
            String whereClause = JDBCUtil.getInstance().buildWhereClause(baseObject, false);
            prepStatement.append(whereClause);
            String thePrepString = prepStatement.toString();
            prepStatement.release();
            if (log.isDebugEnabled()) {
                log.debug("Preparing prepared statement:  " + thePrepString);
            }
            PreparedStatement prep = theConnection.createPreparedStatement(thePrepString);
            if (prep == null) {
                throw new DataException("Unable to create prepared statement for CLOB retrieval." + "  Check DBConnection log for details");
            }
            theConnection.execute();
            if (log.isDebugEnabled()) {
                log.debug("Succesfully executed prepared statement");
            }
        } catch (DBException ex) {
            throw new DataException("Error prepping SELECT ResultSet", ex);
        }
    }

    /**
     * Internal helper function to prepare a LOB update
     * @param baseObject The object that contains the metadata for this BLOB
     * @param fieldName the name of the field that is the BLOB field
     * @param theConnection an already allocated DBConnection object
     * @return a created PreparedStatement object
     * @throws DataException
     */
    protected PreparedStatement prepUpdate(JDBCDataObject baseObject, String fieldName, DBConnection theConnection) throws DataException {
        try {
            String whereClause = JDBCUtil.getInstance().buildWhereClause(baseObject, false);
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
        } catch (DBException ex) {
            throw new DataException("Error prepping LOB update", ex);
        }
    }

    protected void finalizeUpdate(DBConnection theConnection) throws DataException {
        try {
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
        } catch (DBException ex) {
            throw new DataException("Error finalizing LOB update", ex);
        }
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
    private void setBLOB(JDBCDataObject baseObject, String fieldName, InputStream theData, int dataLength, DBConnection theConnection) throws DBException {
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
     * Writes a LONG Character string to the database.
     * @param baseObject The object that contains the metadata for this BLOB
     * @param fieldName the name of the field that is the BLOB field
     * @param theData the data in a <code>String</code>
     * @param theConnection an already allocated DBConnection object Currently
     * may <b>not</b> be null
     * @throws DBException upon error
     */
    private void setCLOB(JDBCDataObject baseObject, String fieldName, String theData, DBConnection theConnection) throws DBException {
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
	 * Saves an InputStream into the database given the criteria and the fieldname
	 * (Criteria should have been previously set).
	 *
	 * Specific to Oracle. Oracle (as of 9.2) does not support standard jdbc LOB
	 * functionality.
	 *
	 * @param fieldName The name of the field to save the Stream to.
	 * @param data a java.io.InputStream object to save to the field.  May be null
	 * if you want the field to be null.
	 * @param dataSize the length of the stream to save.
	 * @throws DataException upon database communications error.
	 * @throws IllegalArgumentException if fieldName is null.
	 */
    private void oracleSaveBlob(String fieldName, InputStream data, int dataSize) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for BLOB Retrieval", ex);
        }
        oraclePrepUpdateEmptyLob(getCriteria(), fieldName, myConnection);
        try {
            if (data != null) {
                oraclePrepSelectForUpdate(getCriteria(), fieldName, myConnection);
                if (myConnection.next()) {
                    Class oracleResultSetClass = Class.forName("oracle.jdbc.driver.OracleResultSet");
                    Class[] parameterTypes = new Class[] { int.class };
                    Object[] arguments = new Object[] { new Integer(1) };
                    Method getBlobMethod = oracleResultSetClass.getMethod("getBlob", parameterTypes);
                    Object blob = getBlobMethod.invoke((Object) myConnection.getResultSet(), arguments);
                    parameterTypes = new Class[] {};
                    arguments = new Object[] {};
                    Class oracleBlobClass = Class.forName("oracle.sql.BLOB");
                    Method getBinaryOutputStreamMethod = oracleBlobClass.getMethod("getBinaryOutputStream", parameterTypes);
                    OutputStream oBlob = (OutputStream) getBinaryOutputStreamMethod.invoke(blob, arguments);
                    Method getChunkSizeMethod = oracleBlobClass.getMethod("getChunkSize", parameterTypes);
                    byte[] chunk = new byte[((Integer) getChunkSizeMethod.invoke(blob, arguments)).intValue()];
                    int i = -1;
                    while ((i = data.read(chunk)) != -1) oBlob.write(chunk, 0, i);
                    oBlob.close();
                    data.close();
                    myConnection.commit();
                } else {
                    throw new DataException("Error SELECTing record for update.");
                }
            }
        } catch (DBException ex) {
            throw new DataException("Error SELECTing record for update.", ex);
        } catch (NoSuchMethodException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (IllegalAccessException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (InvocationTargetException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (ClassNotFoundException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (IOException ex) {
            throw new DataException("Error reading from InputStream.", ex);
        }
    }

    /**
	 * Saves an InputStream into the database given the criteria and the fieldname
	 * (Criteria should have been previously set).
	 *
	 * Specific to Oracle. Oracle (as of 9.2) does not support standard jdbc LOB
	 * functionality.
	 *
	 * @param fieldName The name of the field to save the Stream to.
	 * @param data a java.io.InputStream object to save to the field.  May be null
	 * if you want the field to be null.
	 * @param length The size of the CLOB stream to save to the database
	 * @throws IllegalArgumentException if fieldName is null.
	 * @throws DataException upon database communications error.
	 */
    private void oracleSaveClob(String fieldName, InputStream data, int length) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for CLOB Retrieval", ex);
        }
        if (data == null) {
            oraclePrepUpdateNullClob(getCriteria(), fieldName, myConnection);
            return;
        }
        oraclePrepUpdateEmptyLob(getCriteria(), fieldName, myConnection);
        try {
            oraclePrepSelectForUpdate(getCriteria(), fieldName, myConnection);
            if (myConnection.next()) {
                Class oracleResultSetClass = Class.forName("oracle.jdbc.driver.OracleResultSet");
                Class[] parameterTypes = new Class[] { int.class };
                Object[] arguments = new Object[] { new Integer(1) };
                Method getClobMethod = oracleResultSetClass.getMethod("getClob", parameterTypes);
                Object clob = getClobMethod.invoke((Object) myConnection.getResultSet(), arguments);
                parameterTypes = new Class[] {};
                arguments = new Object[] {};
                Class oracleClobClass = Class.forName("oracle.sql.CLOB");
                Method getAsciiOutputStreamMethod = oracleClobClass.getMethod("getAsciiOutputStreamMethod", parameterTypes);
                OutputStream oClob = (OutputStream) getAsciiOutputStreamMethod.invoke(clob, arguments);
                Method getChunkSizeMethod = oracleClobClass.getMethod("getChunkSize", parameterTypes);
                byte[] chunk = new byte[((Integer) getChunkSizeMethod.invoke(clob, arguments)).intValue()];
                int i = -1;
                while ((i = data.read(chunk)) != -1) oClob.write(chunk, 0, i);
                oClob.close();
                data.close();
                myConnection.commit();
            } else {
                throw new DataException("Error SELECTing record for update.");
            }
        } catch (DBException ex) {
            throw new DataException("Error SELECTing record for update.", ex);
        } catch (NoSuchMethodException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (IllegalAccessException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (InvocationTargetException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (ClassNotFoundException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (IOException ex) {
            throw new DataException("Error reading from InputStream.", ex);
        }
    }

    /**
	 * Saves a Reader into the database given the criteria and the fieldname
	 *
	 * Specific to Oracle. Oracle (as of 9.2) does not support standard jdbc LOB
	 * functionality.
	 *
	 * (Criteria should have been previously set).
	 * @param fieldName The name of the field to save the Stream to.
	 * @param data a java.io.Reader object to save to the field.  May be null
	 * if you want the field to be null.
	 * @param length The size of the data stream to save to the database
	 * @throws DataException upon database communications error.
	 * @throws IllegalArgumentException if fieldName is null.
	 */
    private void oracleSaveClob(String fieldName, java.io.Reader data, int length) throws DataException {
        try {
            if (myConnection == null) {
                myConnection = DBConnectionPool.getInstance(getCriteria().getMappedDataContext()).getConnection("LOB Field Connection");
            }
        } catch (DBException ex) {
            throw new DataException("Error getting Database" + " Connection for CLOB Retrieval", ex);
        }
        if (data == null) {
            oraclePrepUpdateNullClob(getCriteria(), fieldName, myConnection);
            return;
        }
        oraclePrepUpdateEmptyLob(getCriteria(), fieldName, myConnection);
        try {
            oraclePrepSelectForUpdate(getCriteria(), fieldName, myConnection);
            if (myConnection.next()) {
                Class oracleResultSetClass = Class.forName("oracle.jdbc.driver.OracleResultSet");
                Class[] parameterTypes = new Class[] { int.class };
                Object[] arguments = new Object[] { new Integer(1) };
                Method getClobMethod = oracleResultSetClass.getMethod("getClob", parameterTypes);
                Object clob = getClobMethod.invoke((Object) myConnection.getResultSet(), arguments);
                parameterTypes = new Class[] {};
                arguments = new Object[] {};
                Class oracleClobClass = Class.forName("oracle.sql.CLOB");
                Method getCharacterOutputStream = oracleClobClass.getMethod("getCharacterOutputStream", parameterTypes);
                Writer oClob = (Writer) getCharacterOutputStream.invoke(clob, arguments);
                Method getChunkSizeMethod = oracleClobClass.getMethod("getChunkSize", parameterTypes);
                char[] chunk = new char[((Integer) getChunkSizeMethod.invoke(clob, arguments)).intValue()];
                int i = -1;
                while ((i = data.read(chunk)) != -1) oClob.write(chunk, 0, i);
                oClob.close();
                data.close();
                myConnection.commit();
            } else {
                throw new DataException("Error SELECTing record for update.");
            }
        } catch (DBException ex) {
            throw new DataException("Error SELECTing record for update.", ex);
        } catch (NoSuchMethodException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (IllegalAccessException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (InvocationTargetException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (ClassNotFoundException ex) {
            throw new DataException("Reflection error on oracle classes.", ex);
        } catch (IOException ex) {
            throw new DataException("Error reading from InputStream.", ex);
        }
    }

    /**
	 * Internal helper function to prepare a LOB update for Oracle.
	 * Updates the record, setting the BLOB to to empty_blob() or CLOB to empty_clob() because inserts without
	 * setting the LOB will result in a LOB Locator not being present, which will prevent
	 * the LOB from being added to the record.
	 *
	 * @param baseObject The object that contains the metadata for this BLOB
	 * @param fieldName the name of the field that is the BLOB field
	 * @param theConnection an already allocated DBConnection object
	 * @throws DataException
	 */
    private void oraclePrepUpdateEmptyLob(JDBCDataObject baseObject, String fieldName, DBConnection theConnection) throws DataException {
        try {
            String whereClause = JDBCUtil.getInstance().buildWhereClause(baseObject, false);
            FastStringBuffer prepStatement = FastStringBuffer.getInstance();
            String theSQL = null;
            try {
                prepStatement.append("UPDATE ");
                prepStatement.append(baseObject.getJDBCMetaData().getTargetTable());
                prepStatement.append(" SET ");
                prepStatement.append(fieldName);
                if (baseObject.getDataField(fieldName).getFieldMetaData().isCharacterLongObjectType()) {
                    prepStatement.append(" = empty_clob() ");
                } else {
                    prepStatement.append(" = empty_blob() ");
                }
                prepStatement.append(whereClause);
                theSQL = prepStatement.toString();
            } finally {
                prepStatement.release();
                prepStatement = null;
            }
            theConnection.createPreparedStatement(theSQL);
            finalizeUpdate(theConnection);
        } catch (DBException ex) {
            throw new DataException("Error prepping LOB update", ex);
        }
    }

    /**
	 * Internal helper function to prepare a BLOB update for Oracle.
	 * Updates the record, setting the BLOB to to empty_blob() because inserts without
	 * setting the BLOB will result in a LOB Locator not being present, which will prevent
	 * the BLOB from being added to the record.
	 *
	 * @param baseObject The object that contains the metadata for this BLOB
	 * @param fieldName the name of the field that is the BLOB field
	 * @param theConnection an already allocated DBConnection object
	 * @throws DataException
	 */
    private void oraclePrepUpdateNullClob(JDBCDataObject baseObject, String fieldName, DBConnection theConnection) throws DataException {
        try {
            String whereClause = JDBCUtil.getInstance().buildWhereClause(baseObject, false);
            FastStringBuffer prepStatement = FastStringBuffer.getInstance();
            String theSQL = null;
            try {
                prepStatement.append("UPDATE ");
                prepStatement.append(baseObject.getJDBCMetaData().getTargetTable());
                prepStatement.append(" SET ");
                prepStatement.append(fieldName);
                prepStatement.append(" = null ");
                prepStatement.append(whereClause);
                theSQL = prepStatement.toString();
            } finally {
                prepStatement.release();
                prepStatement = null;
            }
            theConnection.createPreparedStatement(theSQL);
            finalizeUpdate(theConnection);
        } catch (DBException ex) {
            throw new DataException("Error prepping CLOB update", ex);
        }
    }

    /**
	 * Internal helper function to prepare a BLOB update for Oracle.
	 * Updates the record, setting the BLOB to to empty_blob() because inserts without
	 * setting the BLOB will result in a LOB Locator not being present, which will prevent
	 * the BLOB from being added to the record.
	 *
	 * @param baseObject The object that contains the metadata for this BLOB
	 * @param fieldName the name of the field that is the BLOB field
	 * @param theConnection an already allocated DBConnection object
	 * @throws DataException
	 */
    protected void oraclePrepSelectForUpdate(JDBCDataObject baseObject, String fieldName, DBConnection theConnection) throws DataException {
        try {
            FastStringBuffer prepStatement = FastStringBuffer.getInstance();
            prepStatement.append("SELECT ");
            prepStatement.append(fieldName);
            prepStatement.append(" from ");
            prepStatement.append(baseObject.getJDBCMetaData().getTargetTable());
            String whereClause = JDBCUtil.getInstance().buildWhereClause(baseObject, false);
            prepStatement.append(whereClause);
            prepStatement.append(" FOR UPDATE");
            String thePrepString = prepStatement.toString();
            prepStatement.release();
            if (log.isDebugEnabled()) {
                log.debug("Preparing prepared statement:  " + thePrepString);
            }
            PreparedStatement prep = theConnection.createPreparedStatement(thePrepString);
            if (prep == null) {
                throw new DataException("Unable to create prepared statement for CLOB retrieval." + "  Check DBConnection log for details");
            }
            theConnection.setAutoCommit(false);
            theConnection.execute();
            if (log.isDebugEnabled()) {
                log.debug("Succesfully executed prepared statement");
            }
        } catch (DBException ex) {
            throw new DataException("Error prepping SELECT ResultSet", ex);
        }
    }
}
