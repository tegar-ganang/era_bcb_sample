package net.sourceforge.jcoupling2.persistence;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.sourceforge.jcoupling2.control.Util;
import net.sourceforge.jcoupling2.exception.JCouplingException;
import net.sourceforge.jcoupling2.peer.property.ChooseClause;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import org.apache.log4j.Logger;

public class DataMapper {

    private static final Logger logger = Logger.getLogger(DataMapper.class);

    /**
	 * add a new filter instance to database
	 *
	 * @param fInstance object
	 * @param OracleConnection which is used for the process
	 * @param OracleCallableStatement used for the process
	 * @return unique requestKey which is set in the FilterInstance object (for mapping issues)
	 * @throws SQLException
	 */
    private Integer registerFilterInstance(FilterInstance fInstance, OracleConnection con, OracleCallableStatement callableStatement) throws SQLException, JCouplingException {
        Integer requestKey = null;
        ARRAY whereClauseValuesArray = null;
        List<String> whereClauseValuesObject = fInstance.getWhereClauseValues();
        whereClauseValuesArray = ((OracleConnection) con).createARRAY("STRINGARRAY", whereClauseValuesObject.toArray());
        String callString = new String("{? = call pkg_filterinstance.register_( ? , ? )}");
        callableStatement = (OracleCallableStatement) con.prepareCall(callString);
        callableStatement.registerOutParameter(1, OracleTypes.NUMBER);
        callableStatement.setString(2, fInstance.getFilterModelName());
        callableStatement.setARRAY(3, whereClauseValuesArray);
        logger.debug(logCallableStatement(callString, new Object[] { fInstance.getFilterModelName(), whereClauseValuesObject }));
        callableStatement.executeQuery();
        requestKey = callableStatement.getInt(1);
        return requestKey;
    }

    /**
	 * lock a filter instance
	 *
	 * @param RequestKey which clearly identifies the FilterInstance 
	 * @param OracleConnection which is used for the process
	 * @param OracleCallableStatement used for the process
	 * @return
	 * @throws SQLException
	 * @deprecated in stored procedure verlagert
	 */
    private void lockFilterInstance(Integer requestKey, OracleConnection con, OracleCallableStatement callableStatement) throws SQLException {
        String callString = new String("{call pkg_filterinstance.LOCK_(?)}");
        callableStatement = (OracleCallableStatement) con.prepareCall(callString);
        callableStatement.setInt(1, requestKey);
        logger.debug(logCallableStatement(callString, new Object[] { requestKey }));
        callableStatement.executeQuery();
    }

    /**
  * lock a mapping
  *
  * @param requestKey
  * @param OracleConnection which is used for the process
  * @param OracleCallableStatement used for the process
  * @return
  * @throws JCouplingException
	 * @deprecated in stored procedure verlagert
  */
    private void lockMapping(String WorkItemID, OracleConnection con, OracleCallableStatement callableStatement) throws SQLException {
        String callString = new String("{call pkg_mapping.LOCK_( ? )}");
        callableStatement = (OracleCallableStatement) con.prepareCall(callString);
        setParameter(callableStatement, 1, WorkItemID, null);
        logger.debug(logCallableStatement(callString, new Object[] { WorkItemID }));
        callableStatement.executeQuery();
    }

    /**
	 * Register a filter instance
	 * Lock it subsequently
	 * Save the mapping from work item ID and the requestKey which is returned by the registering process
	 * lock the mapping subsequently
	 *
	 * @param fInstance
	 * @return fInstance
	 * @throws JCouplingException
	 */
    public FilterInstance registerYAWLRequest(FilterInstance fInstance) throws JCouplingException {
        Integer requestKey = null;
        ARRAY whereClauseValuesArray = null;
        List<String> whereClauseValuesObject = fInstance.getWhereClauseValues();
        OracleConnection connection = null;
        OracleCallableStatement callableStatement = null;
        Mapping mapping = fInstance.getMapping();
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            if (mapping.getRequestKey() == null) {
                whereClauseValuesArray = ((OracleConnection) connection).createARRAY("STRINGARRAY", whereClauseValuesObject.toArray());
                String callString = new String("{? = call pkg_yawl.REGISTER_YAWL_REQUEST( ? , ? , ? , ?)}");
                callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
                callableStatement.registerOutParameter(1, OracleTypes.NUMBER);
                setParameter(callableStatement, 2, fInstance.getFilterModelName(), null);
                callableStatement.setARRAY(3, whereClauseValuesArray);
                setParameter(callableStatement, 4, mapping.getWorkItemId(), null);
                setParameter(callableStatement, 5, mapping.getWorkItemStatus(), null);
                logger.debug(logCallableStatement(callString, new Object[] { fInstance.getFilterModelName(), whereClauseValuesObject, mapping.getWorkItemId(), mapping.getWorkItemStatus() }));
                callableStatement.executeQuery();
                requestKey = callableStatement.getInt(1);
                mapping.setRequestKey(requestKey);
            }
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot register YAWL request", sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return fInstance;
    }

    /**
	 * unlock a filter instance
	 *
	 * @param requestKey
	 * @param OracleConnection which is used for the process
	 * @param OracleCallableStatement used for the process
	 * @return
	 * @throws SQLException
	 * @deprecated in stored procedure verlagert
	 */
    private void unlockFilterInstance(Integer requestKey, OracleConnection con, OracleCallableStatement callableStatement) throws SQLException {
        String callString = new String("{call pkg_filterinstance.UNLOCK_(?)}");
        callableStatement = (OracleCallableStatement) con.prepareCall(callString);
        callableStatement.setInt(1, requestKey);
        logger.debug(logCallableStatement(callString, new Object[] { requestKey }));
        callableStatement.executeQuery();
    }

    /**
	 * delete a filterinstance from database 
	 * 
	 * @param requestKey
	 * @param OracleConnection which is used for the process
	 * @param OracleCallableStatement used for the process
	 * @return
	 * @throws SQLException
	 */
    private void unregisterFilterInstance(Integer requestKey, OracleConnection con, OracleCallableStatement callableStatement) throws SQLException {
        String callString = new String("{call pkg_filterinstance.unregister_( ? )}");
        callableStatement = (OracleCallableStatement) con.prepareCall(callString);
        callableStatement.setInt(1, requestKey);
        logger.debug(logCallableStatement(callString, new Object[] { requestKey }));
        callableStatement.executeQuery();
    }

    /**
	 * removing messages from database  
	 * 
	 * @param a list of message ids
	 * @param OracleConnection which is used for the process
	 * @param OracleCallableStatement used for the process
	 * @return
	 * @throws SQLException
	 */
    private void removeLockedMessages(List<Integer> messages, OracleConnection con, OracleCallableStatement callableStatement) throws SQLException {
        ARRAY msgIDArray = null;
        Object[] msgIDTmp = messages.toArray();
        msgIDArray = con.createARRAY("NUMBERARRAY", msgIDTmp);
        String callString = new String("{call pkg_message.REMOVE_LOCKED(?)}");
        callableStatement = (OracleCallableStatement) con.prepareCall(callString);
        callableStatement.setARRAY(1, msgIDArray);
        logger.debug(logCallableStatement(callString, new Object[] { messages }));
        callableStatement.executeQuery();
    }

    /**
  * handles the process after forwarding successfully matched message to an YAWL task
  * delete the filter instance
  * remove messages from database
  * delete the corresponding mapping
  * TODO@jku: Parameter 2 (NUMBERARRAY) entfernen, es werden immer alle zu konsumierenden Messages gel�scht
  * @param fInstance
  * @return
  * @throws JCouplingException
  */
    public void removeYAWLRequest(FilterInstance fInstance) throws JCouplingException {
        OracleConnection connection = null;
        OracleCallableStatement callableStatement = null;
        Mapping mapping = fInstance.getMapping();
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            String callString = new String("{call pkg_yawl.REMOVE_YAWL_REQUEST( ? , ? , ?  )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            setParameter(callableStatement, 1, mapping.getRequestKey(), null);
            setParameter(callableStatement, 2, null, "NUMBERARRAY");
            setParameter(callableStatement, 3, mapping.getWorkItemId(), null);
            logger.debug(logCallableStatement(callString, new Object[] { mapping.getRequestKey(), null, mapping.getWorkItemId() }));
            callableStatement.executeQuery();
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot remove YAWL request", sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
 * stores the messages in database
 *
 * @param Message Object
 * @return a message ID generated in database
 * @throws JCouplingException
 */
    public Integer addMessage(Message message) throws JCouplingException {
        OracleConnection connection = null;
        OracleCallableStatement callableStatement = null;
        Integer msgID = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            String callString = new String("{? = call pkg_message.ADD_( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.NUMBER);
            callableStatement.setString(2, message.getBody());
            logger.debug(logCallableStatement(callString, new Object[] { message.getBody() }));
            callableStatement.executeQuery();
            msgID = new Integer(callableStatement.getInt(1));
            if (msgID == null) {
                throw new SQLException("message has no body");
            }
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot add message", sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return msgID;
    }

    /**
	 * locks a message
	 *
	 * @param Message Object
	 * @return msgIsLocked: true if message is locked
	 * @throws JCouplingException
	 * @deprecated in stored procedure verlagert
	 */
    public boolean lockMessage(Message message) throws JCouplingException {
        OracleConnection connection = null;
        boolean msgIsLocked = false;
        OracleCallableStatement callableStatement = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_message.LOCK_( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.setInt(1, message.getID());
            logger.debug(logCallableStatement(callString, new Object[] { message.getID() }));
            callableStatement.executeQuery();
            msgIsLocked = true;
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot lock message " + message.getID(), sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return msgIsLocked;
    }

    /**
	 * unlocks a message
	 *
	 * @param Message Object
	 * @return msgIsLocked: false if message is unlocked
	 * @throws JCouplingException
	 * @deprecated in stored procedure verlagert
	 */
    public boolean unlockMessage(Message message) throws JCouplingException {
        OracleConnection connection = null;
        boolean msgIsLocked = true;
        OracleCallableStatement callableStatement = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_message.UNLOCK_( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.setInt(1, message.getID());
            logger.debug(logCallableStatement(callString, new Object[] { message.getID() }));
            callableStatement.executeQuery();
            msgIsLocked = false;
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot unlock message " + message.getID(), sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return msgIsLocked;
    }

    /**
	 * removes a message from database
	 *
	 * @param Message Object
	 * @return void
	 * @throws JCouplingException
	 */
    public void removeMessage(Message message) throws JCouplingException {
        Connection connection = null;
        OracleCallableStatement callableStatement = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_message.REMOVE_(?)}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.setInt(1, message.getID());
            logger.debug(logCallableStatement(callString, new Object[] { message.getID() }));
            callableStatement.executeQuery();
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot remove message " + message.getID(), sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * retrieves messages from database and sets up complete message objects
	 *
	 * @param list of message ids about to be retrieved
	 * @return list of Message objects
	 * @throws JCouplingException
	 */
    public ArrayList<Message> retrieveMessages(List<Integer> msgIDs) throws JCouplingException {
        ArrayList<Message> messages = new ArrayList<Message>();
        Message msg = null;
        ResultSet rs = null;
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        ARRAY MessageIDArray = null;
        Object[] messageIDs = msgIDs.toArray();
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            MessageIDArray = connection.createARRAY("NUMBERARRAY", messageIDs);
            callString = new String("{? = call pkg_message.RETRIEVE_(?)}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.CURSOR);
            callableStatement.setARRAY(2, MessageIDArray);
            logger.debug(logCallableStatement(callString, new Object[] { msgIDs }));
            callableStatement.executeQuery();
            rs = (ResultSet) callableStatement.getCursor(1);
            while (rs.next()) {
                String strBody = rs.getString("text");
                Integer channelID = rs.getInt("id");
                Integer messageID = rs.getInt("messageid");
                msg = new Message(messageID, channelID, getTimeStamp(messageID));
                msg.setBody(strBody);
                messages.add(msg);
            }
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot get messages for messageIDs", sqlex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("cannot close result set", e);
            }
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return messages;
    }

    /**
	 * gets the timestamp for message 
	 *
	 * @param message id
	 * @return Timestamp stored for the message id
	 * @throws JCouplingException
	 */
    public Timestamp getTimeStamp(Integer messageID) throws JCouplingException {
        Connection connection = null;
        ResultSet rs = null;
        OracleCallableStatement callableStatement = null;
        String callString = null;
        Timestamp timestamp_ = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_message.get_timestamp( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.TIMESTAMP);
            callableStatement.setInt(2, messageID);
            logger.debug(logCallableStatement(callString, new Object[] { messageID }));
            callableStatement.executeQuery();
            timestamp_ = callableStatement.getTimestamp(1);
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot getting timestamp from message ID " + messageID, sqlex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("cannot close result set", e);
            }
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return timestamp_;
    }

    /**
	 * TODO@jku: consume and retrieve messages in pkg_filterinstance.EXECUTE_ according to chooseClause
	 * 
	 * @param fInstance
	 * @return
	 * @throws JCouplingException
	 */
    public List<Integer> executeFilterInstance(FilterInstance fInstance) throws JCouplingException {
        OracleConnection connection = null;
        OracleCallableStatement callableStatement = null;
        Mapping mapping = fInstance.getMapping();
        String fInstanceStr = "filter instance " + mapping.getRequestKey();
        ChooseClause chooseClause = fInstance.getChooseClause();
        if (chooseClause == null) {
            throw new JCouplingException("choose clause of " + fInstanceStr + " is null");
        }
        try {
            List<Integer> msgIDs = new ArrayList<Integer>();
            connection = (OracleConnection) ConnectionManager.getConnection();
            String callString = new String("{? = call pkg_filterinstance.EXECUTE_( ? , ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.ARRAY, "NUMBERARRAY");
            callableStatement.setString(2, mapping.getWorkItemId());
            callableStatement.setInt(3, mapping.getRequestKey());
            logger.debug(logCallableStatement(callString, new Object[] { mapping.getWorkItemId(), mapping.getRequestKey() }));
            callableStatement.executeQuery();
            ARRAY matchedMessageIDs = callableStatement.getARRAY(1);
            if (matchedMessageIDs != null) {
                Object[] values = (Object[]) matchedMessageIDs.getArray();
                for (int i = 0; i < values.length; i++) {
                    msgIDs.add(new Integer(Integer.parseInt(values[i].toString())));
                    if (chooseClause.getRetrieveQuantity() != chooseClause.QUANTITY_ALL && chooseClause.getRetrieveQuantity() == i + 1) {
                        break;
                    }
                }
            }
            logger.info("consume " + chooseClause.getConsumeMode() + " (" + matchedMessageIDs.length() + ") and retrieve " + chooseClause.getRetrieveMode() + " (" + msgIDs.size() + ") MessageIDs for work item " + mapping.getWorkItemId());
            return msgIDs;
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot execute filterinstance " + mapping.getRequestKey(), sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * Saves or updates a mapping to the database for recovery of failed YAWL requests
	 * @param Mapping object
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    public void saveMapping(Mapping mapping) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_mapping.save_( ? , ? , ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            setParameter(callableStatement, 1, mapping.getWorkItemId(), null);
            setParameter(callableStatement, 2, mapping.getRequestKey(), null);
            setParameter(callableStatement, 3, mapping.getWorkItemStatus(), null);
            logger.debug(logCallableStatement(callString, new Object[] { mapping.getWorkItemId(), mapping.getRequestKey(), mapping.getWorkItemStatus() }));
            callableStatement.executeQuery();
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot saving mapping for work item " + mapping.getWorkItemId(), sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * unlock a mapping
	 * @param Mapping object
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    public void unlockMapping(Mapping mapping) {
        if (mapping != null) {
            ArrayList<Mapping> mappings = new ArrayList<Mapping>();
            mappings.add(mapping);
            unlockMappings(mappings);
        }
    }

    /**
	 * unlock mappings
	 * @param Mapping object
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    public void unlockMappings(ArrayList<Mapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_mapping.UNLOCK_( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            for (Mapping mapping : mappings) {
                logger.debug(logCallableStatement(callString, new Object[] { mapping.getWorkItemId() }));
                unlockMapping(connection, callableStatement, mapping);
            }
        } catch (Throwable e) {
            logger.error("cannot prepare unlock mappings", e);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * unlock a mapping
	 * @param Mapping object
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    private void unlockMapping(OracleConnection connection, OracleCallableStatement callableStatement, Mapping mapping) {
        if (mapping == null) {
            return;
        }
        try {
            setParameter(callableStatement, 1, mapping.getWorkItemId(), null);
            callableStatement.executeQuery();
        } catch (Throwable e) {
            logger.error("cannot unlock mapping for work item " + mapping.getWorkItemId(), e);
        }
    }

    /**
	 * unlock a mapping
	 * @param Mapping object
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    public void unlockMapping2(Mapping mapping) {
        Statement statement = null;
        OracleConnection connection = null;
        String callString = null;
        try {
            long time = System.currentTimeMillis();
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("UPDATE mapping SET islocked='N' WHERE workitemid='" + mapping.getWorkItemId() + "'");
            statement = connection.createStatement();
            logger.debug(logCallableStatement(callString, new Object[] {}));
            int count = statement.executeUpdate(callString);
            logger.warn("------unlockMapping " + count + " took " + (System.currentTimeMillis() - time));
        } catch (Throwable e) {
            logger.error("cannot unlock mapping for work item " + mapping.getWorkItemId(), e);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("cannot close statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * removes a mapping from database
	 * @param Mapping Object
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    public void removeMapping(Mapping mapping) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_mapping.remove_( ? , ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.setString(1, mapping.getWorkItemId());
            setParameter(callableStatement, 2, mapping.getRequestKey(), null);
            logger.debug(logCallableStatement(callString, new Object[] { mapping.getWorkItemId(), mapping.getRequestKey() }));
            callableStatement.executeQuery();
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot removing mapping " + mapping.getWorkItemId() + ", request key " + mapping.getRequestKey(), sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * Retrieves and locks all channels with its properties
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    public ArrayList<Mapping> retrieveMappings(String channelName) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_mapping.get_mappings( ? , ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(2, OracleTypes.ARRAY, "T_MAPPINGTAB");
            callableStatement.setString(1, channelName);
            logger.debug(logCallableStatement(callString, new Object[] { channelName }));
            callableStatement.executeQuery();
            ARRAY result = callableStatement.getARRAY(2);
            ArrayList<Mapping> mappings = new ArrayList<Mapping>();
            Object[] oa = (Object[]) result.getArray();
            for (int i = 0; i < oa.length; i++) {
                STRUCT os = (STRUCT) oa[i];
                Object[] o = (Object[]) os.getAttributes();
                Mapping mapping = new Mapping((String) o[0], getInteger((BigDecimal) o[1]), (String) o[3]);
                mappings.add(mapping);
            }
            return mappings;
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot retrieve mappings: ", sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * Retrieves and locks all channels with its properties
	 * TODO@tbe: ist schneller als retrieveMappings
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    public ArrayList<Mapping> retrieveMappings2(String channelName) throws JCouplingException {
        Statement statement = null;
        OracleConnection connection = null;
        String callString = null;
        ResultSet rs = null;
        try {
            long time = System.currentTimeMillis();
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("SELECT workitemid,requestkey,islocked,workitemstatus FROM MAPPING m1 " + "WHERE islocked = 'N' AND EXISTS " + "(SELECT 1 FROM mapping m2 " + "LEFT JOIN filterinstance fi ON fi.requestid = m2.requestkey " + "LEFT JOIN filtermodel fm ON fi.filtermodelname = fm.filtermodelname " + "LEFT JOIN channel ch ON ch.channelid = fm.channelid " + "WHERE m1.workitemid=m2.workitemid " + "AND (ch.channelname = 'TestChannel1' OR ch.channelname IS NULL))");
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            logger.debug(logCallableStatement(callString, new Object[] {}));
            statement.executeQuery(callString);
            rs = (ResultSet) statement.getResultSet();
            ArrayList<Mapping> mappings = new ArrayList<Mapping>();
            while (rs.next()) {
                rs.updateString("islocked", "Y");
                Mapping mapping = new Mapping(rs.getString("workitemid"), getInteger((BigDecimal) rs.getObject("requestkey")), rs.getString("workitemstatus"));
                mappings.add(mapping);
            }
            rs.updateRow();
            logger.debug("------get_mappings took " + (System.currentTimeMillis() - time));
            return mappings;
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot retrieve mappings: ", sqlex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("cannot close result set", e);
            }
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.warn("cannot close statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
   *
   * @param workItemID
   * @param requestKey
   * @return
   * @throws JCouplingException
   */
    public String getWorkitemID(Integer requestKey) throws JCouplingException {
        OracleConnection connection = null;
        OracleCallableStatement callableStatement = null;
        String callString = null;
        String workItemID = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_mapping.GET_WORKITEM_ID( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.VARCHAR);
            callableStatement.setInt(2, requestKey);
            logger.debug(logCallableStatement(callString, new Object[] { requestKey }));
            callableStatement.executeQuery();
            workItemID = callableStatement.getString(1);
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot get work item ID for request key " + requestKey, sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close callable statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return workItemID;
    }

    /**
	 * Adds a channel to the channel repository {@link ChannelRepository} and saves in persistently to the database. The
	 * attribute <code>channelID</code> must not be specified as it is supplied by the database via a sequence. Instead
	 * give the channel a unique name.
	 *
	 * @param channel
	 *          an instance of Channel with it's attributes set to their proper values
	 * @throws JCouplingException
	 *           if there appears an cannot adding the channel
	 */
    public Integer addChannel(Channel channel) throws JCouplingException {
        OracleConnection connection = null;
        OracleCallableStatement callableStatement = null;
        String callString = null;
        Integer channelid = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_channel.ADD_(? , ? , ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.NUMBER);
            callableStatement.setString(2, channel.getChannelName());
            callableStatement.setInt(3, channel.getMiddlewareAdapterID());
            String wsdlInfo = channel.getWsdlChannelUrl() + "," + channel.getWsdlChannelPortType() + "," + channel.getWsdlChannelOperationName();
            callableStatement.setString(4, wsdlInfo);
            logger.debug(logCallableStatement(callString, new Object[] { channel.getChannelName(), channel.getMiddlewareAdapterID(), wsdlInfo }));
            callableStatement.executeQuery();
            channelid = new Integer(callableStatement.getInt(1));
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot store channel", sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return channelid;
    }

    /**
	 * Permanently removes the specified channel from the database.
	 *
	 * @param channel
	 *          an instance of {@link Channel} that has at least unique channel name specified (hence may be
	 *          uniquely associated to a channel at the database level)
	 * @throws SQLException
	 *           if the channel cannot be removed, e.g. if other objects depend on the channel that cannot be removed
	 *           cascadingly.
	 */
    public void removeChannel(String channelname) throws JCouplingException {
        OracleConnection connection = null;
        OracleCallableStatement callableStatement = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_channel.DELETE_CHANNEL( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.setString(1, channelname);
            logger.debug(logCallableStatement(callString, new Object[] { channelname }));
            callableStatement.executeQuery();
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot remove channel " + channelname, sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * Retrieves all channels with its properties
	 * @return ArrayList<Channel>
	 * @throws SQLException
	 */
    public ArrayList<Channel> retrieveAllChannels() throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        ResultSet rs = null;
        ArrayList<Channel> channels = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_channel.get_channels()}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.CURSOR);
            logger.debug(logCallableStatement(callString, new Object[] {}));
            callableStatement.executeQuery();
            rs = (ResultSet) callableStatement.getCursor(1);
            channels = new ArrayList<Channel>();
            while (rs.next()) {
                Channel c = new Channel();
                c.setChannelID(rs.getInt("channelid"));
                c.setChannelName(rs.getString("channelname"));
                c.setMiddlewareAdapterID(new Integer(rs.getString("middlewareadapterid")));
                c.setMsgSchemaIn(rs.getString("msgschemain"));
                c.setMsgSchemaOut(rs.getString("channelmsgschemaout"));
                if (rs.getString("istimedecoupled").equals("Y")) c.setIsTimeDecoupled(true);
                if (rs.getString("supportsinbound").equals("Y")) c.setSupportsInbound(true);
                if (rs.getString("supportsinvoke").equals("Y")) c.setSupportsInvoke(true);
                if (rs.getString("iswsdlbacked").equals("Y")) {
                    c.setIsWSDLBacked(true);
                    c.setWsdlChannelOperationName(rs.getString("wsdlchanoperationname"));
                    c.setWsdlChannelPortType(rs.getString("wsdlchanporttype"));
                    c.setWsdlChannelUrl(rs.getString("wsdlchanurl"));
                }
                channels.add(c);
            }
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot retrieve all channels", sqlex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("cannot close result set", e);
            }
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return channels;
    }

    /**
	 * Retrieves one channel with its properties by channelID
	 * @return Channel
	 * @throws JCouplingException
	 */
    public Channel retrieveChannel(Integer channelID) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        ResultSet rs = null;
        Channel channel = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_channel.get_channel( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.CURSOR);
            callableStatement.setInt(2, channelID);
            logger.debug(logCallableStatement(callString, new Object[] { channelID }));
            callableStatement.executeQuery();
            rs = (ResultSet) callableStatement.getCursor(1);
            if (rs.next()) {
                channel = new Channel();
                channel.setChannelID(channelID);
                channel.setChannelName(rs.getString("channelname"));
                channel.setMiddlewareAdapterID(new Integer(rs.getString("middlewareadapterid")));
                channel.setMsgSchemaIn(rs.getString("msgschemain"));
                channel.setMsgSchemaOut(rs.getString("channelmsgschemaout"));
                if (rs.getString("istimedecoupled").equals("Y")) channel.setIsTimeDecoupled(true);
                if (rs.getString("supportsinbound").equals("Y")) channel.setSupportsInbound(true);
                if (rs.getString("supportsinvoke").equals("Y")) channel.setSupportsInvoke(true);
                if (rs.getString("iswsdlbacked").equals("Y")) {
                    channel.setIsWSDLBacked(true);
                    channel.setWsdlChannelOperationName(rs.getString("wsdlchanoperationname"));
                    channel.setWsdlChannelPortType(rs.getString("wsdlchanporttype"));
                    channel.setWsdlChannelUrl(rs.getString("wsdlchanurl"));
                }
            }
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot retrieve channel " + channelID, sqlex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("cannot close result set", e);
            }
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return channel;
    }

    /**
	 * Retrieves one channel with its properties by channelname
	 * @return Channel
	 * @throws JCouplingException
	 */
    public Channel retrieveChannel(String channelName) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        ResultSet rs = null;
        Channel channel = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_channel.get_channel( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.CURSOR);
            callableStatement.setString(2, channelName);
            logger.debug(logCallableStatement(callString, new Object[] { channelName }));
            callableStatement.executeQuery();
            rs = (ResultSet) callableStatement.getCursor(1);
            if (rs.next()) {
                channel = new Channel();
                channel.setChannelID(rs.getInt("channelid"));
                channel.setChannelName(channelName);
                channel.setMiddlewareAdapterID(new Integer(rs.getString("middlewareadapterid")));
                channel.setMsgSchemaIn(rs.getString("msgschemain"));
                channel.setMsgSchemaOut(rs.getString("channelmsgschemaout"));
                if (rs.getString("istimedecoupled").equals("Y")) channel.setIsTimeDecoupled(true);
                if (rs.getString("supportsinbound").equals("Y")) channel.setSupportsInbound(true);
                if (rs.getString("supportsinvoke").equals("Y")) channel.setSupportsInvoke(true);
                if (rs.getString("iswsdlbacked").equals("Y")) {
                    channel.setIsWSDLBacked(true);
                    channel.setWsdlChannelOperationName(rs.getString("wsdlchanoperationname"));
                    channel.setWsdlChannelPortType(rs.getString("wsdlchanporttype"));
                    channel.setWsdlChannelUrl(rs.getString("wsdlchanurl"));
                }
            }
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot retrieve channel " + channelName, sqlex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("cannot close result set", e);
            }
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return channel;
    }

    /**
	 * Adds a property to database
	 * @return Integer
	 * @throws JCouplingException
	 */
    public Integer addProperty(List<String> ColumnNames, List<String> ColumnValues) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        Integer propertyID = null;
        ARRAY AttributeNames = null;
        ARRAY AttributeValues = null;
        Object[] AttributeNamesArray = ColumnNames.toArray();
        Object[] AttributeValuesArray = ColumnValues.toArray();
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            AttributeNames = connection.createARRAY("STRINGARRAY", AttributeNamesArray);
            AttributeValues = connection.createARRAY("STRINGARRAY", AttributeValuesArray);
            callString = new String("{? = call pkg_property.ADD_PROPERTY( ? , ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.NUMBER);
            callableStatement.setARRAY(2, AttributeNames);
            callableStatement.setARRAY(3, AttributeValues);
            logger.debug(logCallableStatement(callString, new Object[] { ColumnNames, ColumnValues }));
            callableStatement.executeQuery();
            propertyID = new Integer(callableStatement.getInt(1));
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot add property", sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return propertyID;
    }

    /**
	 * Removes a property from database by Property name
	 * @return void
	 * @throws JCouplingException
	 */
    public void removeProperty(String propertyName) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_property.REMOVE_PROPERTY( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.setString(1, propertyName);
            logger.debug(logCallableStatement(callString, new Object[] { propertyName }));
            callableStatement.executeQuery();
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot remove property " + propertyName, sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * Retrieves all the properties for a particular channel 
	 * @return Property
	 * @throws JCouplingException
	 */
    public List<Property> retrieveProperty(String channelName) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        ResultSet rs = null;
        Property property = null;
        List<Property> propertyList = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_property.RETRIEVE_PROPERTY( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.CURSOR);
            callableStatement.setString(2, channelName);
            logger.debug(logCallableStatement(callString, new Object[] { channelName }));
            callableStatement.executeQuery();
            rs = (ResultSet) callableStatement.getCursor(1);
            propertyList = new ArrayList<Property>();
            while (rs.next()) {
                property = new Property(rs.getString("propertyname"), rs.getInt("id"), rs.getString("resulttype"), rs.getString("xpathpropexpression"));
                property.setDescription(rs.getString("description"));
                property.setID(rs.getInt("propertyid"));
                propertyList.add(property);
            }
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot retrieve properties for channel " + channelName, sqlex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("cannot close result set", e);
            }
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return propertyList;
    }

    /**
	 * Retrieves all the properties for a particular channel 
	 * @return Property
	 * @throws JCouplingException
	 */
    public List<Property> retrieveProperty(Integer channelID) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        ResultSet rs = null;
        Property property = null;
        List<Property> propertyList = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_property.RETRIEVE_PROPERTY( ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.CURSOR);
            callableStatement.setInt(2, channelID);
            logger.debug(logCallableStatement(callString, new Object[] { channelID }));
            callableStatement.executeQuery();
            rs = (ResultSet) callableStatement.getCursor(1);
            propertyList = new ArrayList<Property>();
            while (rs.next()) {
                property = new Property(rs.getString("propertyname"), channelID, rs.getString("resulttype"), rs.getString("xpathpropexpression"));
                property.setDescription(rs.getString("description"));
                property.setID(rs.getInt("propertyid"));
                propertyList.add(property);
            }
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot retrieve properties for channel ID " + channelID, sqlex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.warn("cannot close result set", e);
            }
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
        return propertyList;
    }

    /**
	 * Alters the descrption for particular property given by name
	 * @return void
	 * @throws JCouplingException
	 */
    public void alterPropertyDescription(String propertyName, String description) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_property.ALTER_DESCRIPTION( ? , ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.setString(1, propertyName);
            callableStatement.setString(2, description);
            logger.debug(logCallableStatement(callString, new Object[] { propertyName, description }));
            callableStatement.executeQuery();
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot alter description for property " + propertyName, sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    /**
	 * Alters the xpath expression for a particular property given by name
	 * @return void
	 * @throws JCouplingException
	 */
    public void alterPropertyXPath(String propertyName, String xpath) throws JCouplingException {
        OracleCallableStatement callableStatement = null;
        OracleConnection connection = null;
        String callString = null;
        try {
            connection = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{call pkg_property.ALTER_XPATH( ? , ? )}");
            callableStatement = (OracleCallableStatement) connection.prepareCall(callString);
            callableStatement.setString(1, propertyName);
            callableStatement.setString(2, xpath);
            logger.debug(logCallableStatement(callString, new Object[] { propertyName, xpath }));
            callableStatement.executeQuery();
        } catch (SQLException sqlex) {
            throw new JCouplingException("cannot alter xpath expression for property " + propertyName, sqlex);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("cannot close prepared statement", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.warn("cannot close connection", e);
            }
        }
    }

    public static String logCallableStatement(String callString, Object[] params) {
        int idx = callString.indexOf("call");
        if (idx < 0) return callString;
        String result = callString.substring(0, idx);
        String part = callString.substring(idx);
        for (int i = 0; i < params.length; i++) {
            idx = part.indexOf("?");
            String s = part.substring(0, idx);
            if (params[i] instanceof String) {
                s += "'" + params[i] + "'";
            } else if (params[i] instanceof List) {
                List list = (List) params[i];
                if (list.size() != 0) {
                    if (list.get(0) instanceof Integer) {
                        s += "numberarray(";
                        for (int j = 0; j < list.size(); j++) {
                            s += list.get(j) + ",";
                        }
                    } else {
                        s += "stringarray(";
                        for (int j = 0; j < list.size(); j++) {
                            s += "'" + list.get(j) + "',";
                        }
                    }
                    s = s.substring(0, s.length() - 1);
                    s += ")";
                } else s += "[]";
            } else {
                s += params[i];
            }
            s += part.substring(idx + 1);
            part = s;
        }
        return result + part;
    }

    private Integer getInteger(BigDecimal bd) throws SQLException {
        if (bd == null) return null; else return bd.intValue();
    }

    public void setParameter(OracleCallableStatement callableStatement, int index, Object obj, String typeName) throws SQLException {
        if (obj == null) {
            if (typeName == null) callableStatement.setNull(index, OracleTypes.NULL); else callableStatement.setNull(index, OracleTypes.ARRAY, typeName);
        } else {
            if (obj instanceof String) callableStatement.setString(index, (String) obj); else if (obj instanceof Integer) callableStatement.setInt(index, (Integer) obj); else if (obj instanceof ARRAY) callableStatement.setArray(index, (ARRAY) obj); else throw new SQLException("unknown parameter type: " + obj);
        }
    }
}
