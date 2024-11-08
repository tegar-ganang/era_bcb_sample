package org.light.portal.distribute.impl;

import static org.light.portal.util.Constants._REPLICATION_BUS_HOST;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.engine.SessionFactoryImplementor;
import org.light.portal.core.dao.PortalDao;
import org.light.portal.core.model.Entity;
import org.light.portal.distribute.Event;
import org.light.portal.distribute.Message;
import org.light.portal.distribute.ReplicationMessage;
import org.light.portal.logger.Logger;
import org.light.portal.logger.LoggerFactory;
import org.light.portal.user.service.UserService;

/**
 * 
 * @author Jianmin Liu
 **/
public abstract class ReplicationAbstractImpl {

    protected abstract void process(Message message);

    protected void processMessages(String server) {
        List<ReplicationMessage> replicationMessages = retrieveMessages(server);
        for (ReplicationMessage message : replicationMessages) {
            removeMessage(message);
            processMessage(message);
        }
    }

    protected List<ReplicationMessage> retrieveMessages(String targetHost) {
        logger.info(String.format("retrieve replication messages for host %s", targetHost));
        ConnectionProvider cp = null;
        Connection conn = null;
        PreparedStatement ps = null;
        List<ReplicationMessage> messages = new LinkedList<ReplicationMessage>();
        try {
            SessionFactoryImplementor impl = (SessionFactoryImplementor) portalDao.getSessionFactory();
            cp = impl.getConnectionProvider();
            conn = cp.getConnection();
            conn.setAutoCommit(false);
            if (_REPLICATION_BUS_HOST) {
                ps = conn.prepareStatement("select id,server,event,message from light_replication_message where server=? order by id");
                ps.setString(1, targetHost);
            } else {
                ps = conn.prepareStatement("select id,server,event,message from light_replication_message order by id");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ReplicationMessage message = new ReplicationMessage(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBytes(4));
                messages.add(message);
            }
            conn.commit();
            ps.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(String.format("retrieve replication messsages failed: %s", e.getMessage()));
            try {
                conn.rollback();
                ps.close();
                conn.close();
            } catch (Exception se) {
                e.printStackTrace();
                logger.error(String.format("retrieve replication messsages failed: %s", e.getMessage()));
            }
        }
        return messages;
    }

    protected void removeMessage(ReplicationMessage message) {
        logger.info(String.format("remove replication message: %d", message.getId()));
        ConnectionProvider cp = null;
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            SessionFactoryImplementor impl = (SessionFactoryImplementor) portalDao.getSessionFactory();
            cp = impl.getConnectionProvider();
            conn = cp.getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement("delete from light_replication_message where id=?");
            ps.setLong(1, message.getId());
            ps.executeUpdate();
            conn.commit();
            ps.close();
            conn.close();
        } catch (Exception e) {
            try {
                conn.rollback();
                ps.close();
                conn.close();
            } catch (Exception se) {
            }
        }
    }

    protected boolean processMessage(ReplicationMessage message) {
        try {
            byte[] buf = message.getMessage();
            if (buf != null) {
                ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
                Object obj = objectIn.readObject();
                if (obj instanceof Message) {
                    logger.info(String.format("process replication message: %d, %s", message.getId(), obj.toString()));
                    Message m = (Message) obj;
                    m.setTargetHost(message.getServer());
                    process(m);
                }
                return true;
            }
        } catch (Exception e) {
            logger.error(String.format("process replication messsage failed: %s", e.getMessage()));
        }
        return false;
    }

    protected void syncMessages(Message message) {
        if (message.getEvent() == Event.CONNECT || message.getEvent() == Event.SYNC_DONE) return;
        logger.info(String.format("remove stale replication messages: %s", message.toString()));
        String className = "";
        long classId = 0;
        if (message.getBody() instanceof Entity) {
            Entity entity = (Entity) message.getBody();
            className = entity.getClass().getName();
            classId = entity.getId();
        }
        ConnectionProvider cp = null;
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            SessionFactoryImplementor impl = (SessionFactoryImplementor) portalDao.getSessionFactory();
            cp = impl.getConnectionProvider();
            conn = cp.getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement("delete from light_replication_message where event=? and className=? and classId=?");
            ps.setString(1, message.getEvent().toString());
            ps.setString(2, className);
            ps.setLong(3, classId);
            ps.executeUpdate();
            conn.commit();
            ps.close();
            conn.close();
        } catch (Exception e) {
            try {
                conn.rollback();
                ps.close();
                conn.close();
            } catch (Exception se) {
            }
        }
    }

    public PortalDao getPortalDao() {
        return portalDao;
    }

    public void setPortalDao(PortalDao portalDao) {
        this.portalDao = portalDao;
    }

    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private PortalDao portalDao;

    private UserService userService;

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
