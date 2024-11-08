package com.expantion.dao.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.expantion.core.ConnectionProvider;
import com.expantion.dao.ChatDao;
import com.expantion.dao.utils.DaoUtils;
import com.expantion.model.chat.ChatMessage;

public class ChatDaoImpl implements ChatDao {

    private static Logger logger = Logger.getLogger(ChatDaoImpl.class);

    private static final String GET_MESSAGES_QUERY = "{call getChatMessages(?,?,?,?,?,?,?)}";

    private static final String SEND_MESSAGE_QUERY = "{call sendChatMessage(?,?,?,?)}";

    @Override
    public ArrayList<ChatMessage> getChatMessages(String channel, int startMessageId) {
        logger.debug("get messages");
        ArrayList<ChatMessage> result = new ArrayList<ChatMessage>();
        Connection sqlConnection = ConnectionProvider.getInstance().getConnection();
        CallableStatement statement = null;
        try {
            statement = sqlConnection.prepareCall(GET_MESSAGES_QUERY);
            statement.setString(1, channel);
            statement.setInt(2, startMessageId);
            ResultSet resultat = statement.executeQuery();
            while (resultat.next()) {
                ChatMessage msg = new ChatMessage();
                msg.setId(resultat.getInt(1));
                msg.setMessage(resultat.getString(2));
                msg.setAuteur(resultat.getString(3));
                msg.setChannel(resultat.getString(4));
                msg.setDate(resultat.getTimestamp(5));
                result.add(msg);
            }
            logger.debug("result : " + statement.getResultSet());
        } catch (SQLException pException) {
            logger.error("erreur sql :" + pException.getMessage());
        } catch (Exception e) {
            logger.error("erreur generic :" + e.getMessage());
        } finally {
            DaoUtils.killConnection(sqlConnection, statement);
        }
        logger.debug("result : " + result);
        return result;
    }

    @Override
    public boolean sendMessage(ChatMessage message) {
        logger.debug("storing message in database");
        boolean result = false;
        Connection sqlConnection = ConnectionProvider.getInstance().getConnection();
        CallableStatement statement = null;
        try {
            statement = sqlConnection.prepareCall(SEND_MESSAGE_QUERY);
            statement.setString(1, message.getChannel());
            statement.setString(2, message.getMessage());
            statement.setString(3, message.getAuteur());
            statement.registerOutParameter(4, Types.BOOLEAN);
            statement.execute();
            result = statement.getBoolean(4);
        } catch (SQLException pException) {
            logger.error("erreur sql :" + pException.getMessage());
        } catch (Exception e) {
            logger.error("erreur generic :" + e.getMessage());
        } finally {
            DaoUtils.killConnection(sqlConnection, statement);
        }
        logger.debug("result : " + result);
        return result;
    }
}
