package com.liusoft.dlog4j.dao;

import java.io.Serializable;
import java.sql.DatabaseMetaData;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import com.liusoft.dlog4j.db.HibernateUtils;

/**
 * ������ݿ���ʽӿڵĻ���
 * 
 * ��֮�������ں�Ϊ��ʦ
 * 
 * @author liudong
 */
public abstract class DAO extends _DAOBase {

    public static final int MAX_TAG_COUNT = 5;

    public static final int MAX_TAG_LENGTH = 20;

    /**
	 * ��ȡ��ݿ��Ԫ��Ϣ 
	 * @return
	 */
    public static DatabaseMetaData metadata() {
        try {
            return getSession().connection().getMetaData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * ��Ӷ���
	 * @param cbean
	 */
    public static void save(Object cbean) {
        try {
            Session ssn = getSession();
            beginTransaction();
            ssn.save(cbean);
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ��Ӷ���
	 * @param cbean
	 */
    protected static void saveOrUpdate(Object cbean) {
        try {
            Session ssn = getSession();
            beginTransaction();
            ssn.saveOrUpdate(cbean);
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ɾ�����
	 * @param cbean
	 */
    protected static void delete(Object cbean) {
        try {
            Session ssn = getSession();
            beginTransaction();
            ssn.delete(cbean);
            commit();
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * �������ɾ��ĳ������
	 * @param objClass
	 * @param key
	 * @return
	 */
    protected static int delete(Class objClass, Serializable key) {
        StringBuffer hql = new StringBuffer("DELETE FROM ");
        hql.append(objClass.getName());
        hql.append(" AS t WHERE t.id=?");
        return commitUpdate(hql.toString(), new Object[] { key });
    }

    protected static int delete(Class objClass, int key) {
        return delete(objClass, new Integer(key));
    }

    /**
	 * д����ݵ���ݿ�
	 */
    public static void flush() {
        try {
            Session ssn = getSession();
            if (ssn.isDirty()) {
                beginTransaction();
                ssn.flush();
                commit();
            }
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    /**
	 * ���������ض���
	 * @param beanClass
	 * @param ident
	 * @return
	 */
    protected static Object getBean(Class beanClass, int id) {
        return getSession().get(beanClass, new Integer(id));
    }

    /**
	 * ִ��ͳ�Ʋ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static Number executeStat(String hql, Object[] args) {
        return (Number) uniqueResult(hql, args);
    }

    /**
	 * ִ��ͳ�Ʋ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static int executeStatAsInt(String hql, Object[] args) {
        return (executeStat(hql, args)).intValue();
    }

    protected static int executeStatAsInt(String hql, int parm1) {
        return executeStatAsInt(hql, new Object[] { new Integer(parm1) });
    }

    protected static int executeStatAsInt(String hql, int parm1, int parm2) {
        return executeStatAsInt(hql, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    protected static int executeStatAsInt(String hql, int parm1, int parm2, int parm3, int parm4) {
        return executeStatAsInt(hql, new Object[] { new Integer(parm1), new Integer(parm2), new Integer(parm3), new Integer(parm4) });
    }

    /**
	 * ִ��ͳ�Ʋ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static long executeStatAsLong(String hql, Object[] args) {
        return (executeStat(hql, args)).longValue();
    }

    /**
	 * ִ����ͨ��ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static List findAll(String hql, Object[] args) {
        return executeQuery(hql, -1, -1, args);
    }

    /**
	 * ִ����ͨ��ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static List executeQuery(String hql, int fromIdx, int fetchCount, Object[] args) {
        Session ssn = getSession();
        Query q = ssn.createQuery(hql);
        for (int i = 0; args != null && i < args.length; i++) {
            q.setParameter(i, args[i]);
        }
        if (fromIdx > 0) q.setFirstResult(fromIdx);
        if (fetchCount > 0) q.setMaxResults(fetchCount);
        return q.list();
    }

    protected static List executeQuery(String hql, int fromIdx, int fetchCount, int parm1) {
        return executeQuery(hql, fromIdx, fetchCount, new Object[] { new Integer(parm1) });
    }

    protected static List executeQuery(String hql, int fromIdx, int fetchCount, int parm1, int parm2) {
        return executeQuery(hql, fromIdx, fetchCount, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    protected static List executeQuery(String hql, int fromIdx, int fetchCount, int parm1, int parm2, int parm3) {
        return executeQuery(hql, fromIdx, fetchCount, new Object[] { new Integer(parm1), new Integer(parm2), new Integer(parm3) });
    }

    /**
	 * ִ�и������
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static int executeUpdate(String hql, Object[] args) {
        Session ssn = getSession();
        Query q = ssn.createQuery(hql);
        for (int i = 0; args != null && i < args.length; i++) {
            q.setParameter(i, args[i]);
        }
        return q.executeUpdate();
    }

    protected static int executeUpdate(String hql, int parm1) {
        return executeUpdate(hql, new Object[] { new Integer(parm1) });
    }

    /**
	 * ִ�и������
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static int commitUpdate(String hql, Object[] args) {
        try {
            Session ssn = getSession();
            beginTransaction();
            Query q = ssn.createQuery(hql);
            for (int i = 0; args != null && i < args.length; i++) {
                q.setParameter(i, args[i]);
            }
            int er = q.executeUpdate();
            commit();
            return er;
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    protected static int commitUpdate(String hql, int parm1, int parm2) {
        return commitUpdate(hql, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    /**
	 * ִ�з��ص�һ���Ĳ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static Object uniqueResult(String hql, Object[] args) {
        Session ssn = getSession();
        Query q = ssn.createQuery(hql);
        for (int i = 0; args != null && i < args.length; i++) {
            q.setParameter(i, args[i]);
        }
        q.setMaxResults(1);
        return q.uniqueResult();
    }

    /**
	 * ִ��ͳ�Ʋ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static Number executeNamedStat(String hql, Object[] args) {
        return (Number) namedUniqueResult(hql, args);
    }

    protected static Number executeNamedStat(String hql, int parm1) {
        return executeNamedStat(hql, new Object[] { new Integer(parm1) });
    }

    protected static Number executeNamedStat(String hql, int parm1, int parm2) {
        return executeNamedStat(hql, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    /**
	 * ִ��ͳ�Ʋ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static int executeNamedStatAsInt(String hql, Object[] args) {
        return (executeNamedStat(hql, args)).intValue();
    }

    /**
	 * ִ��ͳ�Ʋ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static int executeNamedStatAsInt(String hql, int parm1) {
        return executeNamedStatAsInt(hql, new Object[] { new Integer(parm1) });
    }

    protected static int executeNamedStatAsInt(String hql, String parm1) {
        return executeNamedStatAsInt(hql, new Object[] { parm1 });
    }

    protected static int executeNamedStatAsInt(String hql, int parm1, int parm2) {
        return executeNamedStatAsInt(hql, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    protected static int executeNamedStatAsInt(String hql, int parm1, int parm2, int parm3) {
        return executeNamedStatAsInt(hql, new Object[] { new Integer(parm1), new Integer(parm2), new Integer(parm3) });
    }

    /**
	 * ִ��ͳ�Ʋ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static long executeNamedStatAsLong(String hql, Object[] args) {
        return (executeNamedStat(hql, args)).longValue();
    }

    /**
	 * ִ����ͨ��ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static List executeNamedQuery(String hql, int fromIdx, int fetchCount, Object[] args) {
        Session ssn = getSession();
        Query q = ssn.getNamedQuery(hql);
        for (int i = 0; args != null && i < args.length; i++) {
            q.setParameter(i, args[i]);
        }
        if (fromIdx > 0) q.setFirstResult(fromIdx);
        if (fetchCount > 0) q.setMaxResults(fetchCount);
        return q.list();
    }

    protected static List executeNamedQuery(String hql, int fromIdx, int fetchCount, int parm1) {
        return executeNamedQuery(hql, fromIdx, fetchCount, new Object[] { new Integer(parm1) });
    }

    protected static List executeNamedQuery(String hql, int fromIdx, int fetchCount, int parm1, int parm2) {
        return executeNamedQuery(hql, fromIdx, fetchCount, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    protected static List executeNamedQuery(String hql, int fromIdx, int fetchCount, int parm1, int parm2, int parm3) {
        return executeNamedQuery(hql, fromIdx, fetchCount, new Object[] { new Integer(parm1), new Integer(parm2), new Integer(parm3) });
    }

    /**
	 * ִ����ͨ��ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static List findNamedAll(String hql, Object[] args) {
        return executeNamedQuery(hql, -1, -1, args);
    }

    protected static List findNamedAll(String hql, Object arg) {
        return findNamedAll(hql, new Object[] { arg });
    }

    protected static List findNamedAll(String hql, int arg) {
        return findNamedAll(hql, new Object[] { new Integer(arg) });
    }

    protected static List findNamedAll(String hql, int parm1, int parm2) {
        return findNamedAll(hql, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    protected static List findNamedAll(String hql, int parm1, int parm2, int parm3) {
        return findNamedAll(hql, new Object[] { new Integer(parm1), new Integer(parm2), new Integer(parm3) });
    }

    /**
	 * ִ�з��ص�һ���Ĳ�ѯ���
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static Object namedUniqueResult(String hql, Object[] args) {
        Session ssn = getSession();
        Query q = ssn.getNamedQuery(hql);
        for (int i = 0; args != null && i < args.length; i++) {
            q.setParameter(i, args[i]);
        }
        q.setMaxResults(1);
        return q.uniqueResult();
    }

    protected static Object namedUniqueResult(String hql, int parm1) {
        return namedUniqueResult(hql, new Object[] { new Integer(parm1) });
    }

    protected static Object namedUniqueResult(String hql, String parm1) {
        return namedUniqueResult(hql, new Object[] { parm1 });
    }

    protected static Object namedUniqueResult(String hql, int parm1, int parm2) {
        return namedUniqueResult(hql, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    protected static Object namedUniqueResult(String hql, int parm1, int parm2, int parm3) {
        return namedUniqueResult(hql, new Object[] { new Integer(parm1), new Integer(parm2), new Integer(parm3) });
    }

    /**
	 * ִ�и������
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static int executeNamedUpdate(String hql, Object[] args) {
        Session ssn = getSession();
        Query q = ssn.getNamedQuery(hql);
        for (int i = 0; args != null && i < args.length; i++) {
            q.setParameter(i, args[i]);
        }
        return q.executeUpdate();
    }

    protected static int executeNamedUpdate(String hql, int parm1) {
        return executeNamedUpdate(hql, new Object[] { new Integer(parm1) });
    }

    protected static int executeNamedUpdate(String hql, int parm1, int parm2) {
        return executeNamedUpdate(hql, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    protected static int executeNamedUpdate(String hql, int parm1, int parm2, int parm3) {
        return executeNamedUpdate(hql, new Object[] { new Integer(parm1), new Integer(parm2), new Integer(parm3) });
    }

    /**
	 * ִ�и������
	 * @param hql
	 * @param args
	 * @return
	 */
    protected static int commitNamedUpdate(String hql, Object[] args) {
        try {
            Session ssn = getSession();
            beginTransaction();
            Query q = ssn.getNamedQuery(hql);
            for (int i = 0; args != null && i < args.length; i++) {
                q.setParameter(i, args[i]);
            }
            int er = q.executeUpdate();
            commit();
            return er;
        } catch (HibernateException e) {
            rollback();
            throw e;
        }
    }

    protected static int commitNamedUpdate(String hql, int parm1, int parm2) {
        return commitNamedUpdate(hql, new Object[] { new Integer(parm1), new Integer(parm2) });
    }

    protected static int commitNamedUpdate(String hql, int parm1, int parm2, int parm3) {
        return commitNamedUpdate(hql, new Object[] { new Integer(parm1), new Integer(parm2), new Integer(parm2) });
    }
}

/**
 * ���ڲ���Hibernate��һЩ����
 * @author Winter Lau
 */
abstract class _DAOBase {

    /**
	 * Get a instance of hibernate's session
	 * @return
	 * @throws HibernateException
	 */
    protected static Session getSession() {
        return HibernateUtils.getSession();
    }

    /**
	 * Start a new database transaction.
	 */
    protected static void beginTransaction() {
        HibernateUtils.beginTransaction();
    }

    /**
	 * Commit the database transaction.
	 */
    protected static void commit() {
        HibernateUtils.commit();
    }

    /**
	 * Rollback the database transaction.
	 */
    protected static void rollback() {
        HibernateUtils.rollback();
    }
}
