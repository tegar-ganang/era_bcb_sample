package com.liusoft.dlog4j.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import com.liusoft.dlog4j.CapacityExceedException;
import com.liusoft.dlog4j.base.Orderable;
import com.liusoft.dlog4j.beans.MusicBean;
import com.liusoft.dlog4j.beans.MusicBoxBean;
import com.liusoft.dlog4j.beans.SiteBean;
import com.liusoft.dlog4j.search.SearchDataProvider;

/**
 * ������ݷ��ʽӿ�
 * @author Winter Lau
 */
public class MusicDAO extends DAO implements SearchDataProvider {

    public static final int MAX_MUSICBOX_COUNT = 20;

    /**
	 * �������ֵ����ʹ���
	 * @param music_id
	 * @param incCount
	 * @return
	 */
    public static int incViewCount(int incCount, int[] music_ids) {
        StringBuffer hql = new StringBuffer("UPDATE MusicBean d SET d.viewCount=d.viewCount+? WHERE d.id IN (");
        for (int i = 0; i < music_ids.length; i++) {
            if (i > 0) hql.append(',');
            hql.append('?');
        }
        hql.append(')');
        try {
            Session ssn = getSession();
            beginTransaction();
            Query q = ssn.createQuery(hql.toString());
            q.setInteger(0, incCount);
            for (int i = 1; i <= music_ids.length; i++) {
                q.setParameter(i, new Integer(music_ids[i - 1]));
            }
            int er = q.executeUpdate();
            commit();
            return er;
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ��ݱ�Ż�ȡ������Ϣ
	 * @param ids
	 * @return
	 */
    public static List listSongs(List ids) {
        Session ssn = getSession();
        StringBuffer hql = new StringBuffer("FROM MusicBean m WHERE m.id IN (");
        int i = 0;
        for (; i < ids.size(); i++) {
            hql.append("?,");
        }
        hql.append("?) ORDER BY m.id DESC");
        Query q = ssn.createQuery(hql.toString());
        for (i = 0; i < ids.size(); i++) {
            int id = ((Number) ids.get(i)).intValue();
            q.setInteger(i, id);
        }
        q.setInteger(i, -1);
        return q.list();
    }

    /**
	 * ��������
	 * @param site
	 * @param key
	 * @return
	 */
    public static List search(String key) {
        Session ssn = getSession();
        Query q = ssn.getNamedQuery("SEARCH_MUSIC");
        String pattern = '%' + key + '%';
        q.setString("key", pattern);
        q.setMaxResults(20);
        List res = q.list();
        List songs = new ArrayList();
        for (int i = 0; res != null && i < res.size(); i++) {
            Object[] objs = (Object[]) res.get(i);
            MusicBean mbean = new MusicBean();
            mbean.setTitle((String) objs[0]);
            mbean.setUrl((String) objs[1]);
            songs.add(mbean);
        }
        return songs;
    }

    /**
	 * �г�ĳ��վ�����µ�N�׸���
	 * @param siteid
	 * @param maxCount
	 * @return
	 */
    public static List listNewSongs(int siteid, int maxCount) {
        return executeNamedQuery("LIST_NEW_MUSIC", -1, maxCount, siteid);
    }

    /**
	 * �г�ĳ��վ�����µ�N�׸���
	 * @param siteid
	 * @param maxCount
	 * @return
	 */
    public static List listSongsWithoutBox(int siteid) {
        return findNamedAll("LIST_MUSIC_WITHOUT_BOX", siteid);
    }

    /**
	 * ��Ӹ���
	 * @param mbean
	 */
    public static void addMusic(MusicBean mbean) {
        Session ssn = getSession();
        try {
            beginTransaction();
            if (mbean.getStatus() == MusicBean.STATUS_NORMAL) {
                if (mbean.getMusicBox() != null) mbean.getMusicBox().incMusicCount(1);
            }
            ssn.save(mbean);
            commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            rollback();
            throw e;
        }
    }

    /**
	 * ɾ�����
	 * @param mbean
	 */
    public static void deleteMusic(MusicBean mbean) {
        Session ssn = getSession();
        try {
            beginTransaction();
            if (mbean.getStatus() == MusicBean.STATUS_NORMAL) {
                if (mbean.getMusicBox() != null) mbean.getMusicBox().incMusicCount(-1);
            }
            String hql = "UPDATE DiaryBean d SET d.bgSound = ? WHERE d.bgSound.id=?";
            executeUpdate(hql, new Object[] { null, new Integer(mbean.getId()) });
            ssn.delete(mbean);
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ɾ�����ֺ�
	 * @param mbean
	 */
    public static void deleteMusicBox(MusicBoxBean mbean) {
        delete(mbean);
    }

    /**
	 * ����ɾ�����
	 * @param mbean
	 */
    public static void deleteMusics(int siteid, int[] ids) {
        Session ssn = getSession();
        try {
            StringBuffer hql = new StringBuffer("FROM MusicBean m WHERE m.site.id=? AND m.id IN (");
            StringBuffer hql2 = new StringBuffer("UPDATE DiaryBean d SET d.bgSound=? WHERE d.bgSound.id IN (");
            int i = 0;
            for (; i < ids.length; i++) {
                hql.append("?,");
                hql2.append("?,");
            }
            hql.append("?)");
            hql2.append("?)");
            Query q = ssn.createQuery(hql.toString());
            q.setInteger(0, siteid);
            i = 0;
            for (; i < ids.length; i++) {
                q.setInteger(i + 1, ids[i]);
            }
            q.setInteger(i + 1, ids[0]);
            List musics = q.list();
            if (musics.size() > 0) {
                beginTransaction();
                Query q2 = ssn.createQuery(hql2.toString());
                i = 0;
                q2.setParameter(0, null);
                for (; i < ids.length; i++) {
                    q2.setInteger(i + 1, ids[i]);
                }
                q2.setInteger(i + 1, ids[0]);
                q2.executeUpdate();
                for (i = 0; i < musics.size(); i++) {
                    MusicBean mbean = (MusicBean) musics.get(i);
                    if (mbean.getMusicBox() != null) mbean.getMusicBox().incMusicCount(-1);
                    ssn.delete(mbean);
                }
                commit();
            }
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * �������ֺ�
	 * @param mbox
	 * @param pos
	 * @param up
	 * @throws CapacityExceedException 
	 * @throws SQLException  
	 */
    public static void createBox(MusicBoxBean mbox, int pos, boolean up) throws CapacityExceedException {
        Session ssn = getSession();
        int order_value = 1;
        if (pos > 0) {
            MusicBoxBean friend = (MusicBoxBean) ssn.get(MusicBoxBean.class, new Integer(pos));
            order_value = friend.getSortOrder();
        }
        mbox.setSortOrder(order_value - (up ? 1 : 0));
        try {
            beginTransaction();
            ssn.save(mbox);
            List links = findNamedAll("LIST_MUSICBOXES", mbox.getSite().getId());
            if (links.size() >= ConfigDAO.intValue(mbox.getSite().getId(), "MAX_MUSICBOX_COUNT", MAX_MUSICBOX_COUNT)) throw new CapacityExceedException(links.size());
            if (links.size() > 1) {
                for (int i = 0; i < links.size(); i++) {
                    Orderable lb = (Orderable) links.get(i);
                    executeNamedUpdate("UPDATE_MUSICBOX_ORDER", (i + 1), lb.getId());
                }
            }
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ����ĳ��վ��ĸ�����
	 * @param site
	 * @return
	 */
    public static int getMusicCount(SiteBean site) {
        if (site == null) return -1;
        return executeNamedStatAsInt("MUSIC_COUNT", site.getId());
    }

    /**
	 * ������ֱ�Ż�ȡ��ϸ����
	 * @param mid
	 * @return
	 */
    public static MusicBean getMusicByID(int mid) {
        if (mid <= 0) return null;
        return (MusicBean) getBean(MusicBean.class, mid);
    }

    /**
	 * ������ֺеı�Ż�ȡ��ϸ��Ϣ
	 * @param mboxid
	 * @return
	 */
    public static MusicBoxBean getMusicBoxByID(int mboxid) {
        if (mboxid < 1) return null;
        return (MusicBoxBean) getBean(MusicBoxBean.class, mboxid);
    }

    public List fetchAfter(Date beginTime) throws Exception {
        return findNamedAll("LIST_MUSIC_AFTER", new Object[] { beginTime, MusicBean.I_STATUS_NORMAL });
    }
}
