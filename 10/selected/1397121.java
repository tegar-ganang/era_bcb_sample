package com.liusoft.dlog4j.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import com.liusoft.dlog4j.beans.FriendBean;
import com.liusoft.dlog4j.beans.MyBlackListBean;
import com.liusoft.dlog4j.beans.UserBean;

/**
 * �û���ص���ݿ���ʽӿ�
 * @author liudong
 */
public class UserDAO extends DAO {

    /**
	 * ����ĳ��վ���ע���û���,���û��ָ��վ���򷵻�ע���û�����
	 * @param site
	 * @return
	 */
    public static int getUserCount(int site) {
        String hql = "SELECT COUNT(*) FROM UserBean d WHERE d.status=?";
        if (site > 0) {
            hql += " AND d.site.id=?";
            return executeStatAsInt(hql, UserBean.STATUS_NORMAL, site);
        }
        return executeStatAsInt(hql, UserBean.STATUS_NORMAL);
    }

    /**
	 * ��Ӻ���
	 * @param friend
	 */
    public static boolean addFriend(FriendBean friend) {
        if (namedUniqueResult("FRIEND", friend.getOwner(), friend.getFriend().getId()) == null) {
            save(friend);
            return true;
        }
        return false;
    }

    /**
	 * ��Ӻ��� 
	 * @param myId
	 * @param otherId
	 * @param type
	 * @return
	 */
    public static boolean addBlackList(int myId, int otherId, int type) {
        if (namedUniqueResult("BLACKLIST", myId, otherId) == null) {
            MyBlackListBean fbean = new MyBlackListBean();
            fbean.setAddTime(new Date());
            fbean.setMyId(myId);
            fbean.setOther(new UserBean(otherId));
            fbean.setType(type);
            save(fbean);
            return true;
        }
        return false;
    }

    /**
	 * �Ӻ�����ɾ��ĳ���û�
	 * @param myId
	 * @param otherId
	 * @return
	 */
    public static boolean delBlackList(int myId, int otherId) {
        return commitNamedUpdate("DELETE_BLACKLIST", myId, otherId) > 0;
    }

    /**
	 * �ж�ĳ�û��Ƿ�����ĺ�����
	 * @param myId
	 * @param otherId
	 * @return
	 */
    public static boolean isUserInBlackList(int myId, int otherId) {
        return namedUniqueResult("BLACKLIST", myId, otherId) != null;
    }

    /**
	 * ��ѯ�����û�
	 * @param site_id
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static List listOnlineUsers(int site_id, int fromIdx, int count) {
        return executeNamedQuery("ONLINE_USERS", fromIdx, count, site_id, UserBean.STATUS_ONLINE);
    }

    /**
	 * ��ѯ�����û���
	 * @param site_id
	 * @return
	 */
    public static int getOnlineUserCount(int site_id) {
        return executeNamedStat("ONLINE_USER_COUNT", site_id, UserBean.STATUS_ONLINE).intValue();
    }

    /**
	 * �ж������Ƿ����
	 * @param ownerId
	 * @param friendId
	 * @return
	 */
    public static FriendBean getFriend(int ownerId, int friendId) {
        return (FriendBean) namedUniqueResult("FRIEND", ownerId, friendId);
    }

    /**
	 * ����ĳ�˵ĺ�����
	 * @param ownerId
	 * @return
	 */
    public static int getFriendCount(int ownerId) {
        return executeNamedStat("FRIEND_COUNT", ownerId).intValue();
    }

    /**
	 * �г�ĳ�˵ĺ���
	 * @param ownerId
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static List listFriends(int ownerId, int fromIdx, int count) {
        return executeNamedQuery("LIST_FRIEND", fromIdx, count, ownerId);
    }

    /**
	 * ����ĳ�˵ĺ����û���
	 * @param ownerId
	 * @return
	 */
    public static int getBlackUserCount(int ownerId) {
        return executeNamedStat("BLACKLIST_COUNT", ownerId).intValue();
    }

    /**
	 * �г�ĳ�˺����е��û�
	 * @param ownerId
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static List listBlackUsers(int ownerId, int fromIdx, int count) {
        return executeNamedQuery("LIST_BLACKLIST", fromIdx, count, ownerId);
    }

    /**
	 * ɾ�������˵ĺ��ѹ�ϵ
	 * @param ownerId
	 * @param friendId
	 */
    public static int deleteFriend(int ownerId, String[] friendIds) {
        if (friendIds == null || friendIds.length == 0) return 0;
        StringBuffer hql = new StringBuffer("DELETE FROM FriendBean f WHERE f.owner=? AND f.friend.id IN (");
        for (int i = 0; i < friendIds.length; i++) {
            hql.append("?,");
        }
        hql.append("?)");
        Session ssn = getSession();
        try {
            beginTransaction();
            Query q = ssn.createQuery(hql.toString());
            q.setInteger(0, ownerId);
            int i = 0;
            for (; i < friendIds.length; i++) {
                String s_id = (String) friendIds[i];
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

    /**
	 * ɾ�����
	 * @param ownerId
	 * @param friendId
	 */
    public static int deleteBlacklist(int ownerId, String[] otherIds) {
        if (otherIds == null || otherIds.length == 0) return 0;
        StringBuffer hql = new StringBuffer("DELETE FROM MyBlackListBean f WHERE f.myId=? AND f.other.id IN (");
        for (int i = 0; i < otherIds.length; i++) {
            hql.append("?,");
        }
        hql.append("?)");
        Session ssn = getSession();
        try {
            beginTransaction();
            Query q = ssn.createQuery(hql.toString());
            q.setInteger(0, ownerId);
            int i = 0;
            for (; i < otherIds.length; i++) {
                String s_id = (String) otherIds[i];
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

    /**
	 * ����ĳ��վ��ע���û�
	 * @param site
	 * @param searchKey
	 * @return
	 */
    public static List searchUser(String searchKey) {
        String key = searchKey + '%';
        return executeNamedQuery("SEARCH_USER", -1, 20, new Object[] { key, key });
    }

    /**
	 * �������û�
	 * @param user
	 * @param commit
	 * @return
	 */
    public static boolean createUser(UserBean user) {
        if (user == null) return false;
        Timestamp ct = new Timestamp(System.currentTimeMillis());
        user.setRegTime(ct);
        user.setLastTime(ct);
        user.setStatus(UserBean.STATUS_NORMAL);
        save(user);
        return true;
    }

    /**
	 * �г���ĳ����վ��ע����û�(_xxx_top_info.vm, users.vm)
	 * @param site_id
	 * @param fromIdx
	 * @param count
	 * @return
	 */
    public static List listUsersFromSite(int site_id, int fromIdx, int count) {
        return executeNamedQuery("LIST_REGUSERS_OF_SITE", fromIdx, count, site_id);
    }

    /**
	 * �г���ĳ����վ��ע����û���(users.vm)
	 * @param site_id
	 * @return
	 */
    public static int getUserCountFromSite(int site_id) {
        return executeNamedStat("REGUSER_COUNT_OF_SITE", site_id).intValue();
    }

    /**
	 * �����û�����
	 * @param user
	 * @param commit
	 * @throws HibernateException
	 */
    public static void updateUser(UserBean user) {
        flush();
    }

    /**
	 * ���keep_days�ֶ�ֵ,����ע��ʱ�����
	 * @param userid
	 * @param lastLogin ���һ�ε�¼��ʱ��
	 * @param manual_logout �Ƿ��ֹ�ע��
	 * @return
	 */
    public static int userLogout(int userid, Timestamp lastLogin, boolean manual_logout) {
        Session ssn = getSession();
        if (ssn == null) return -1;
        try {
            beginTransaction();
            Query q = ssn.getNamedQuery(manual_logout ? "USER_LOGOUT_1" : "USER_LOGOUT_2");
            q.setInteger("online_status", UserBean.STATUS_OFFLINE);
            if (manual_logout) q.setInteger("keep_day", 0);
            q.setInteger("user_id", userid);
            q.setTimestamp("last_time", lastLogin);
            int er = q.executeUpdate();
            commit();
            return er;
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ����û���Ż�ȡ�û���ϸ��Ϣ
	 * @param user_id
	 * @return
	 * @throws HibernateException
	 */
    public static UserBean getUserByID(int user_id) {
        if (user_id < 0) return null;
        return (UserBean) getBean(UserBean.class, user_id);
    }

    /**
	 * ����û�������û�����,�����û��ĵ�¼
	 * @param username
	 * @return
	 * @throws HibernateException
	 */
    public static UserBean getUserByName(String username) {
        return (UserBean) namedUniqueResult("GET_USER_BY_NAME", username);
    }

    /**
	 * ����û��ǳƼ����û�����,����ע��ʱ������ͬ�����
	 * @param nickname
	 * @return
	 * @throws HibernateException
	 */
    public static UserBean getUserByNickname(String nickname) {
        return (UserBean) namedUniqueResult("GET_USER_BY_NICKNAME", nickname);
    }
}
