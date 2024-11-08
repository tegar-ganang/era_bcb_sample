package com.expantion.metier.impl;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.expantion.dao.ChatDao;
import com.expantion.dao.ServerDaoFactoryImpl;
import com.expantion.metier.ChatManager;
import com.expantion.metier.IMessageManager;
import com.expantion.model.chat.ChatMessage;

public abstract class AbstractChatManager extends MessageManager implements ChatManager, IMessageManager {

    private static Logger logger = Logger.getLogger(ChatManager.class);

    @Override
    public abstract ArrayList<ChatMessage> getChatMessages(String channelId, int startMessageId);

    @Override
    public void sendMessage(ChatMessage pMessage) {
        logger.info("envoit d'un message");
        ChatDao dao = ServerDaoFactoryImpl.getInstance().getChatDao();
        if (dao != null) {
            dao.sendMessage(pMessage);
        } else {
            logger.error("dao null");
        }
        synchronizeMessage(pMessage, pMessage.getChannel());
    }
}
