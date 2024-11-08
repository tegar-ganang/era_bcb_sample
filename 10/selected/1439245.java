package org.epo.jdist.dbinterface;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.epo.jdist.dl.JDistException;
import org.epo.jdist.heart.JDistMessage;
import org.epo.jdist.heart.JDistSysParam;
import org.epoline.jsf.utils.Log4jManager;
import com.infotel.util.FlaggedThread;

/**
 * DBMS context related to a valid connection that enables to perform various
 * DB related actions.
 * Creation date: 10/2001
 * @author Infotel Conseil
 */
public class DBContext {

    private Connection connection = null;

    private Statement statement = null;

    private JDistSysParam sysParam;

    private DBConnectionPool dbConnectionPool;

    /**
 * DbContext constructor: get a connection from the pool.
 * @param theSysParam org.epo.jdist.heart.JDistSysParam
 * @param theDbConnectionPool org.epo.jdist.dbinterface.DBConnectionPool
 * @exception org.epo.jdist.dl.JDistException
 */
    public DBContext(JDistSysParam theSysParam, DBConnectionPool theDbConnectionPool) throws JDistException {
        super();
        setSysParam(theSysParam);
        setDbConnectionPool(theDbConnectionPool);
        setConnection(getDbConnectionPool().borrowConnection());
    }

    /**
 * DbContext constructor: get a connection from the pool (a retry mechanism is
 * available in case a problem occurs).
 * @param theSysParam org.epo.jdist.heart.JDistSysParam
 * @param theDbConnectionPool org.epo.jdist.dbinterface.DBConnectionPool
 * @param theThreadCaller com.infotel.util.FlaggedThread
 */
    public DBContext(JDistSysParam theSysParam, DBConnectionPool theDbConnectionPool, FlaggedThread theThreadCaller) {
        super();
        setSysParam(theSysParam);
        setDbConnectionPool(theDbConnectionPool);
        boolean myDbDown = true;
        while (!theThreadCaller.isStopped() && myDbDown) {
            try {
                setConnection(getDbConnectionPool().borrowConnection());
                myDbDown = false;
            } catch (JDistException se) {
                myDbDown = true;
                try {
                    Thread.sleep(getSysParam().getDbAccessDelay());
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    /**
 * Create a Statement object for sending SQL statements to the database
 * @exception org.epo.jdist.dl.JDistException
 */
    public void createStatement() throws JDistException {
        try {
            setStatement(getConnection().createStatement());
        } catch (SQLException se) {
            throw new JDistException(JDistException.ERR_DB_SQL, se.getMessage());
        }
    }

    /**
 * Update the auto-commit mode of the connection
 * @exception org.epo.jdist.dl.JDistException
 */
    public void setAutoCommit(boolean theAutoCommit) throws JDistException {
        try {
            getConnection().setAutoCommit(theAutoCommit);
        } catch (SQLException se) {
            throw new JDistException(JDistException.ERR_DB_SQL, se.getMessage());
        }
    }

    /**
 * Check of a mandatory parameter in a properties file.
 * @return java.lang.String
 * @param thePropObj java.util.Properties
 * @param thePropName java.lang.String
 * @exception org.epo.jdist.dl.JDistException
 */
    private String checkGetProperty(Properties thePropObj, String thePropName) throws JDistException {
        String myValue = thePropObj.getProperty(thePropName);
        if ((myValue == null) || (myValue.trim().equals(""))) {
            String myErrorMsg = "Missing mandatory field in the properties file: " + thePropName;
            throw new JDistException(JDistException.ERR_MISSING_PARAM, myErrorMsg);
        }
        return myValue;
    }

    /**
 * Close the current statement.
 * @exception java.sql.SQLException.
 */
    public void close() throws JDistException {
        try {
            if (getStatement() != null) getStatement().close();
        } catch (SQLException se) {
            throw new JDistException(JDistException.ERR_DB_SQL, se.getMessage());
        } finally {
            if (getDbConnectionPool() != null) {
                getDbConnectionPool().returnConnection(getConnection());
            }
        }
    }

    /**
 * Standard accessor: returns the current Connection.
 * @return java.sql.Connection
 */
    public Connection getConnection() {
        return connection;
    }

    /**
 * Standard accessor.
 * @return org.epo.jdist.dbinterface.DBConnectionPool
 */
    private DBConnectionPool getDbConnectionPool() {
        return dbConnectionPool;
    }

    /**
 * Standard accessor: returns the current Statement.
 * @return java.sql.Statement
 */
    public Statement getStatement() {
        return statement;
    }

    /**
 * Standard accessor.
 * @return org.epo.jdist.heart.JDistSysParam
 */
    private JDistSysParam getSysParam() {
        return sysParam;
    }

    /**
 * Insert in the 'in process' table a message to be processed by jDist.
 * @param thePckId java.lang.String
 * @param theAppNum java.lang.String
 * @param theAppType java.lang.String
 * @param theDocCode java.lang.String
 * @param theMsgText java.lang.String
 * @return long The sequence number of the inserted message
 * @exception org.epo.jdist.dl.JDistException
 */
    public long insertInProcessMsg(String thePckId, String theAppNum, String theAppType, String theDocCode, String theMsgTxt) throws JDistException {
        String IN_PROCESS_MSGS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_TABLENAME_PRP);
        String SEQNUM = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_SEQNUM_PRP);
        String PXIDOCID = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_PXIDOCID_PRP);
        String APPNUM = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_APPNUM_PRP);
        String APPTYPE = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_APPTYPE_PRP);
        String DOCCODE = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_DOCCODE_PRP);
        String MSGTXT = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_MSGTXT_PRP);
        String STATUS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_STATUS_PRP);
        String UPDATETS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_UPDATETS_PRP);
        int myStatus = 0;
        long mySeqNum = 0;
        try {
            setAutoCommit(false);
            createStatement();
            mySeqNum = retrieveSeqNum();
            if (mySeqNum == 0) {
                try {
                    getConnection().rollback();
                } catch (SQLException se2) {
                    throw new JDistException(JDistException.ERR_DB_SQL, se2.getMessage());
                }
                throw new JDistException(JDistException.ERR_DB_SQL, "No sequence number found");
            }
            Timestamp myTimeStamp = new Timestamp(System.currentTimeMillis());
            String mySqlQuery = "INSERT INTO " + IN_PROCESS_MSGS + "(" + SEQNUM + "," + PXIDOCID + "," + APPNUM + "," + APPTYPE + "," + DOCCODE + "," + MSGTXT + "," + STATUS + "," + UPDATETS + ") VALUES (" + mySeqNum + ",'" + thePckId + "','" + theAppNum + "','" + theAppType + "','" + theDocCode + "','" + theMsgTxt + "'," + myStatus + ",{ts '" + myTimeStamp + "'})";
            getStatement().executeUpdate(mySqlQuery);
            getConnection().commit();
        } catch (SQLException se) {
            try {
                getConnection().rollback();
            } catch (SQLException se2) {
                throw new JDistException(JDistException.ERR_DB_SQL, se2.getMessage());
            }
            throw new JDistException(JDistException.ERR_DB_SQL, se.getMessage());
        } catch (JDistException e) {
            if (e.getReturnCode() == JDistException.ERR_DB_SQL) {
                try {
                    getConnection().rollback();
                } catch (SQLException se2) {
                    throw new JDistException(JDistException.ERR_DB_SQL, se2.getMessage());
                }
            }
            throw new JDistException(JDistException.ERR_DB_SQL, e.getMessage());
        } finally {
            try {
                setAutoCommit(true);
            } catch (JDistException je) {
            }
        }
        return mySeqNum;
    }

    private long retrieveSeqNum() throws SQLException, JDistException {
        long mySeqNum = 0;
        String JDIST_SEQ_NUMBER = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_SEQNUMBER_TABLENAME_PRP);
        String PRIMARYKEY = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_SEQNUMBER_COLUMN_PRIMARYKEY_PRP);
        String CURRENT_VALUE = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_SEQNUMBER_COLUMN_CURRVALUE_PRP);
        String myUpdateQuery = "UPDATE " + JDIST_SEQ_NUMBER + " SET " + CURRENT_VALUE + "=" + CURRENT_VALUE + "+1 WHERE " + PRIMARYKEY + "='MESSAGE'";
        String mySelectQuery = "SELECT " + CURRENT_VALUE + " FROM " + JDIST_SEQ_NUMBER + " WHERE " + PRIMARYKEY + "='MESSAGE'";
        getStatement().executeUpdate(myUpdateQuery);
        ResultSet myResultSet = getStatement().executeQuery(mySelectQuery);
        if (myResultSet.next()) {
            mySeqNum = myResultSet.getLong(CURRENT_VALUE);
        }
        return mySeqNum;
    }

    /**
 * Retrieve from the 'in process' table messages to be processed by jDist.
 * @return java.util.LinkedList
 * @exception org.epo.jdist.dl.JDistException
 */
    public LinkedList retrieveToBeProcessedMsgs(FlaggedThread theThreadCaller) throws JDistException {
        String IN_PROCESS_MSGS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_TABLENAME_PRP);
        String SEQNUM = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_SEQNUM_PRP);
        String PXIDOCID = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_PXIDOCID_PRP);
        String APPNUM = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_APPNUM_PRP);
        String APPTYPE = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_APPTYPE_PRP);
        String DOCCODE = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_DOCCODE_PRP);
        String MSGTXT = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_MSGTXT_PRP);
        String STATUS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_STATUS_PRP);
        ResultSet myResultSet = null;
        LinkedList myMsgList = new LinkedList();
        int myStatus = 0;
        String mySqlQuery = "SELECT " + SEQNUM + ", " + PXIDOCID + ", " + APPNUM + ", " + APPTYPE + ", " + DOCCODE + ", " + MSGTXT + " FROM " + IN_PROCESS_MSGS + " WHERE " + STATUS + "=" + myStatus;
        boolean myDbDown = true;
        while (!theThreadCaller.isStopped() && myDbDown) {
            try {
                setAutoCommit(true);
                createStatement();
                myResultSet = getStatement().executeQuery(mySqlQuery);
                myDbDown = false;
                while (myResultSet.next()) {
                    myMsgList.addLast(new JDistMessage(myResultSet.getLong(SEQNUM), myResultSet.getString(PXIDOCID), myResultSet.getString(APPNUM), myResultSet.getString(APPTYPE), myResultSet.getString(DOCCODE), myResultSet.getString(MSGTXT)));
                }
            } catch (SQLException se) {
                myDbDown = true;
                try {
                    Thread.sleep(getSysParam().getDbAccessDelay());
                } catch (InterruptedException ie) {
                }
            }
        }
        return myMsgList;
    }

    /**
 * Standard accessor: sets the Connection.
 * @param newValue java.sql.Connection
 */
    public void setConnection(Connection newValue) {
        this.connection = newValue;
    }

    /**
 * Standard accessor.
 * @param newDbConnectionPool org.epo.jdist.dbinterface.DBConnectionPool
 */
    private void setDbConnectionPool(DBConnectionPool newDbConnectionPool) {
        dbConnectionPool = newDbConnectionPool;
    }

    /**
 * Standard accessor.
 * @param newValue java.sql.Statement
 */
    private void setStatement(Statement newValue) {
        this.statement = newValue;
    }

    /**
 * Standard accessor.
 * @param newSysParam org.epo.jdist.heart.JDistSysParam
 */
    private void setSysParam(JDistSysParam newSysParam) {
        sysParam = newSysParam;
    }

    /**
 * When a message is unsuccessfully processed, this method is invoked to insert 
 * it in the 'in error' table and delete it from the 'in process' table
 * @param theSeqNum long
 * @param thePckId java.lang.String
 * @param theAppNum java.lang.String
 * @param theAppType java.lang.String
 * @param theDocCode java.lang.String
 * @param theMsgText java.lang.String
 * @param theErrorMsg java.lang.String
 * @exception org.epo.jdist.dl.JDistException
 */
    public void updateFailedMsg(long theSeqNum, String thePckId, String theAppNum, String theAppType, String theDocCode, String theMsgTxt, String theErrorMsg) throws JDistException {
        String IN_PROCESS_MSGS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_TABLENAME_PRP);
        String IN_ERROR_MSGS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_TABLENAME_PRP);
        String SEQNUM_P = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_SEQNUM_PRP);
        String SEQNUM_E = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_COLUMN_SEQNUM_PRP);
        String PXIDOCID_E = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_COLUMN_PXIDOCID_PRP);
        String APPNUM_E = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_COLUMN_APPNUM_PRP);
        String APPTYPE_E = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_COLUMN_APPTYPE_PRP);
        String DOCCODE_E = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_COLUMN_DOCCODE_PRP);
        String MSGTXT_E = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_COLUMN_MSGTXT_PRP);
        String ERRORMSG_E = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_COLUMN_ERRORMSG_PRP);
        String UPDATETS_E = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INERROR_COLUMN_UPDATETS_PRP);
        Timestamp myUpdateTS = new Timestamp(System.currentTimeMillis());
        String myErrorMsg;
        if ((theErrorMsg != null) && (theErrorMsg.length() > 79)) myErrorMsg = theErrorMsg.substring(0, 79); else myErrorMsg = theErrorMsg;
        String myMsg2Insert = "INSERT INTO " + IN_ERROR_MSGS + " (" + SEQNUM_E + "," + PXIDOCID_E + "," + APPNUM_E + "," + APPTYPE_E + "," + DOCCODE_E + "," + MSGTXT_E + "," + ERRORMSG_E + "," + UPDATETS_E + ") VALUES (" + theSeqNum + ",'" + thePckId + "','" + theAppNum + "','" + theAppType + "','" + theDocCode + "','" + theMsgTxt + "','" + myErrorMsg + "',{ts '" + myUpdateTS + "'})";
        String myMsg2Delete = "DELETE FROM " + IN_PROCESS_MSGS + " WHERE " + SEQNUM_P + "=" + theSeqNum;
        try {
            setAutoCommit(false);
            createStatement();
            getStatement().executeUpdate(myMsg2Insert);
            getStatement().executeUpdate(myMsg2Delete);
            getConnection().commit();
        } catch (SQLException se) {
            try {
                getConnection().rollback();
            } catch (SQLException se2) {
                throw new JDistException(JDistException.ERR_DB_SQL, se2.getMessage());
            }
            throw new JDistException(JDistException.ERR_DB_SQL, se.getMessage());
        } catch (JDistException e) {
            if (e.getReturnCode() == JDistException.ERR_DB_SQL) {
                try {
                    getConnection().rollback();
                } catch (SQLException se2) {
                    throw new JDistException(JDistException.ERR_DB_SQL, se2.getMessage());
                }
            }
            throw new JDistException(JDistException.ERR_DB_SQL, e.getMessage());
        } finally {
            try {
                setAutoCommit(true);
            } catch (JDistException je) {
            }
        }
    }

    /**
 * When a message is successfully processed, this method is invoked to set
 * the message status to 1 in the 'in process' table
 * @param theSeqNum long
 * @param theMbxTarget java.lang.String
 * @param theUserTarget java.lang.String
 * @exception org.epo.jdist.dl.JDistException 
 */
    public void updateSuccessfullyMsg(long theSeqNum, String theMbxTarget, String theUserTarget) throws org.epo.jdist.dl.JDistException {
        String IN_PROCESS_MSGS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_TABLENAME_PRP);
        String SEQNUM = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_SEQNUM_PRP);
        String STATUS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_STATUS_PRP);
        String UPDATETS = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_UPDATETS_PRP);
        String MBXTARGET = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_MBXTARGET_PRP);
        String USERTARGET = checkGetProperty(getSysParam().getProperties(), JDistSysParam.DB_INPROCESS_COLUMN_USERTARGET_PRP);
        Timestamp myUpdateTS = new Timestamp(System.currentTimeMillis());
        String mySqlQuery = "UPDATE " + IN_PROCESS_MSGS + " SET " + STATUS + "=1 , " + UPDATETS + "={ts '" + myUpdateTS + "'}," + MBXTARGET + "='" + theMbxTarget + "'," + USERTARGET + "='" + theUserTarget + "' WHERE (" + STATUS + "=0) AND (" + SEQNUM + "=" + theSeqNum + ")";
        try {
            setAutoCommit(true);
            createStatement();
            getStatement().executeUpdate(mySqlQuery);
        } catch (SQLException se) {
            throw new JDistException(JDistException.ERR_DB_SQL, se.getMessage());
        }
    }
}
