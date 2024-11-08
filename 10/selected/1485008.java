package com.liusoft.dlog4j.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import com.liusoft.dlog4j.beans.MessageBean;
import com.liusoft.dlog4j.beans.UserBean;

/**
 * վ�ڶ���Ϣ֮��ݿ���ʽӿ�
 * @author Winter Lau
 */
public class MessageDAO extends DAO {

    /**
	 * Ⱥ������
	 * @param club
	 * @param only_admin
	 * @param content
	 * @param sender
	 */
    public static void SendMsgs(List users, String content, UserBean sender) {
        try {
            MessageBean msg = new MessageBean();
            msg.setContent(content);
            msg.setSendTime(new Date());
            msg.setFromUser(sender);
            msg.setStatus(MessageBean.STATUS_NEW);
            Session ssn = getSession();
            beginTransaction();
            for (int i = 0; i < users.size(); i++) {
                UserBean user = (UserBean) users.get(i);
                msg.setToUser(user);
                ssn.save(msg);
                if (i % 20 == 0) {
                    ssn.flush();
                    ssn.clear();
                }
            }
            commit();
        } catch (HibernateException e) {
            rollback();
        }
    }

    /**
	 * �ظ���ɾ����ظ��Ķ���Ϣ
	 * @param old_msg_id
	 * @param msg
	 */
    public static void replyAndDeleteMessage(int old_msg_id, MessageBean msg) {
        try {
            Session ssn = getSession();
            beginTransaction();
            ssn.save(msg);
            if (old_msg_id > 0) executeNamedUpdate("DELETE_MESSAGE", old_msg_id, msg.getFromUser().getId());
            commit();
        } catch (HibernateException e) {
            rollback();
        }
    }

    /**
	 * �Ķ�������Ϣ
	 * @param msg
	 */
    public static void readMsg(MessageBean msg) {
        if (msg != null) {
            commitNamedUpdate("READ_ONE_MESSAGE", new Object[] { MessageBean.I_STATUS_READ, new Date(), new Integer(msg.getId()), MessageBean.I_STATUS_NEW });
        }
    }

    /**
	 * �����¶���Ϣ״̬Ϊ�Ѷ�
	 * @param userid
	 * @return
	 */
    public static int readNewMsgs(int userid) {
        return commitNamedUpdate("READ_MESSAGE", new Object[] { MessageBean.I_STATUS_READ, new Timestamp(System.currentTimeMillis()), new Integer(userid), MessageBean.I_STATUS_NEW });
    }

    /**
	 * ��ȡ����Ϣ
	 * @param msg_id
	 * @return
	 */
    public static MessageBean getMsg(int msg_id) {
        if (msg_id < 0) return null;
        return (MessageBean) getBean(MessageBean.class, msg_id);
    }

    /**
	 * �ж��Ƿ����¶���Ϣ
	 * @param userid
	 * @return
	 */
    public static boolean hasNewMessage(int userid) {
        return getNewMessageCount(userid) > 0;
    }

    /**
	 * ����ĳ���û����¶���Ϣ��(δ������Ϣ)
	 * @param userid
	 * @return
	 */
    public static int getNewMessageCount(int userid) {
        return executeNamedStatAsInt("NEW_MESSAGE_COUNT_OF_STATUS", new Object[] { new Integer(userid), MessageBean.I_STATUS_NEW, new Date() });
    }

    /**
	 * ����ĳ���û��Ķ���Ϣ��
	 * @param userid
	 * @return
	 */
    public static int getMessageCount(int userid) {
        return executeNamedStatAsInt("MESSAGE_COUNT", userid, MessageBean.STATUS_DELETED);
    }

    /**
	 * �г�ĳ���û����յ��Ķ���Ϣ
	 * @param userid
	 * @param fromId
	 * @param count
	 * @return
	 */
    public static List listMsgs(int userid, int fromIdx, int count) {
        return executeNamedQuery("LIST_MESSAGE", fromIdx, count, userid);
    }

    /**
	 * ɾ��ĳ�����Ϣ
	 * @param userid
	 * @param status
	 * @param commit
	 * @return
	 * @throws SQLException 
	 */
    public static int deleteMsgs(int userid, int status) {
        return commitNamedUpdate("DELETE_MESSAGE_BY_STATUS", userid, status);
    }

    /**
	 * ɾ��ĳ������Ϣ
	 * @param userid
	 * @param msgid
	 * @param commit
	 * @return
	 */
    public static int deleteMsg(int userid, int msgid) {
        return commitNamedUpdate("DELETE_MESSAGE", msgid, userid);
    }

    /**
	 * ɾ��ĳЩ����Ϣ
	 * @param ownerId
	 * @param friendId
	 */
    public static int deleteMsgs(int ownerId, String[] msgIds) {
        if (msgIds == null || msgIds.length == 0) return 0;
        int max_msg_count = Math.min(msgIds.length, 50);
        StringBuffer hql = new StringBuffer("DELETE FROM MessageBean AS f WHERE f.toUser.id=? AND f.id IN (");
        for (int i = 0; i < max_msg_count; i++) {
            hql.append("?,");
        }
        hql.append("?)");
        Session ssn = getSession();
        try {
            beginTransaction();
            Query q = ssn.createQuery(hql.toString());
            q.setInteger(0, ownerId);
            int i = 0;
            for (; i < max_msg_count; i++) {
                String s_id = (String) msgIds[i];
                int id = -1;
                try {
                    id = Integer.parseInt(s_id);
                } catch (Exception e) {
                }
                q.setInteger(i + 1, id);
            }
            q.setInteger(i + 1, -1);
            int er = q.executeUpdate();
            commit();
            return er;
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }
}
