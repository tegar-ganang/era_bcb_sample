package com.liusoft.dlog4j.dao;

import java.util.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import com.liusoft.dlog4j.beans.BookmarkBean;

/**
 * ��ǩ��ص���ݿ��������
 * @author Winter Lau
 */
public class BookmarkDAO extends DAO {

    /**
	 * ������ǩ
	 * @param bean
	 */
    public static boolean save(BookmarkBean bookmark) {
        if (exists(bookmark.getOwner().getId(), bookmark.getParentId(), bookmark.getParentType())) return false;
        Session ssn = getSession();
        try {
            beginTransaction();
            if (bookmark.getCreateTime() == null) bookmark.setCreateTime(new Date());
            bookmark.getOwner().getCount().incBookmarkCount(1);
            ssn.save(bookmark);
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
        return true;
    }

    /**
	 * ɾ��ĳ����ǩ
	 * @param siteid
	 * @param userid
	 * @param bookmark_id
	 * @return
	 * @throws SQLException
	 */
    public static boolean delete(int userid, int bookmark_id) {
        return commitNamedUpdate("DELETE_BOOKMARK", bookmark_id, userid) > 0;
    }

    /**
	 * ɾ������ǩ
	 * @param ownerId
	 * @param bookmarkIds
	 */
    public static int deleteBookmarks(int ownerId, String[] bookmarkIds) {
        if (bookmarkIds == null || bookmarkIds.length == 0) return 0;
        StringBuffer hql = new StringBuffer("DELETE FROM BookmarkBean AS f WHERE f.owner=? AND f.id IN (");
        for (int i = 0; i < bookmarkIds.length; i++) {
            hql.append("?,");
        }
        hql.append("?)");
        Session ssn = getSession();
        try {
            beginTransaction();
            Query q = ssn.createQuery(hql.toString());
            q.setInteger(0, ownerId);
            int i = 0;
            for (; i < bookmarkIds.length; i++) {
                String s_id = (String) bookmarkIds[i];
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
        } finally {
            hql = null;
        }
    }

    /**
	 * ���г�ĳ���û���������ǩ
	 * @param user_id
	 * @return
	 */
    public static List list(int user_id) {
        return findNamedAll("LIST_BOOKMARK", user_id);
    }

    /**
	 * �ж�ĳһ��ǩ�Ƿ����
	 * @param user_id
	 * @param parent_id
	 * @param parent_type
	 * @return
	 */
    public static boolean exists(int user_id, int parent_id, int parent_type) {
        if (user_id < 1 || parent_id < 0) return false;
        return executeNamedStatAsInt("CHECK_BOOKMARK", user_id, parent_id, parent_type) > 0;
    }
}
