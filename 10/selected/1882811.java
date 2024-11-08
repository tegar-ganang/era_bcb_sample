package com.germinus.xpression.cms.hibernate;

import java.util.List;
import java.util.Iterator;
import com.germinus.xpression.cms.PersistedObject;
import com.germinus.xpression.cms.hibernate.HQLCondition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Query;

/**
 * Manages all the data manipulations within Hibernate.
 *
 * @author Acheca
 */
public class HibernateStorage {

    private static Log log = LogFactory.getLog(HibernateStorage.class);

    public static final int LIST_LIMIT_TO_PREVENT_OUT_OF_MEMORY = 100000;

    private static HibernateStorage hibernateStorage;

    public static HibernateStorage getInstance() {
        if (hibernateStorage == null) {
            hibernateStorage = new HibernateStorage();
        }
        return hibernateStorage;
    }

    public Object load(Class objectClass, Long id) throws HibernateException {
        Session session = HibernateUtil.currentSession();
        return session.load(objectClass, id);
    }

    public void delete(Object object) throws HibernateException {
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            session.delete(object);
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            throw new HibernateException(e);
        }
    }

    public void save(Object object) throws HibernateException {
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            session.saveOrUpdate(object);
            session.flush();
            session.refresh(object);
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            throw new HibernateException(e);
        }
    }

    public List executeHQL(String hql) throws HibernateException {
        List noConditions = null;
        return executeHQL(hql, noConditions);
    }

    public List executeHQL(String hql, List conditionsList) throws HibernateException {
        if (log.isDebugEnabled()) log.debug("Hibernate query: " + hql);
        if (log.isDebugEnabled()) log.debug("with conditions: " + conditionsList);
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            Query query = session.createQuery(hql);
            if (conditionsList != null) {
                for (Iterator it = conditionsList.iterator(); it.hasNext(); ) {
                    HQLCondition cond = (HQLCondition) it.next();
                    if (cond.getOperation().equals(HQLCondition.LIKE)) {
                        query.setParameter(cond.getField(), "%" + cond.getValue() + "%");
                    } else {
                        query.setParameter(cond.getField(), cond.getValue());
                    }
                }
            }
            query.setMaxResults(LIST_LIMIT_TO_PREVENT_OUT_OF_MEMORY);
            List result = query.list();
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            tx.rollback();
            log.error("Error Executing HQL Sentence in Hibernate", e);
            throw new HibernateException(e);
        }
    }

    public List executeHQL(String hql, List conditionsList, int offset, int numOfResults) throws HibernateException {
        if (log.isDebugEnabled()) log.debug("Hibernate query: " + hql);
        if (log.isDebugEnabled()) log.debug("with conditions: " + conditionsList);
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            Query query = session.createQuery(hql);
            if (conditionsList != null) {
                for (Iterator it = conditionsList.iterator(); it.hasNext(); ) {
                    HQLCondition cond = (HQLCondition) it.next();
                    if (cond.getOperation().equals(HQLCondition.LIKE)) {
                        query.setParameter(cond.getField(), "%" + cond.getValue() + "%");
                    } else {
                        query.setParameter(cond.getField(), cond.getValue());
                    }
                }
            }
            query.setFirstResult(offset);
            query.setMaxResults(numOfResults);
            List result = query.list();
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            tx.rollback();
            log.error("Error Executing HQL Sentence in Hibernate", e);
            throw new HibernateException(e);
        }
    }

    public int getCountResult(Criteria criteria) {
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            List result = criteria.list();
            if (result.size() > 0) {
                tx.commit();
                return ((Integer) result.get(0)).intValue();
            } else {
                tx.commit();
                return 0;
            }
        } catch (RuntimeException e) {
            tx.rollback();
            log.error("Error Executing HQL Sentence in Hibernate", e);
            throw new HibernateException(e);
        }
    }

    public int updateHQL(String hql) throws HibernateException {
        return updateHQL(hql, null);
    }

    public int updateHQL(String hql, List conditionsList) throws HibernateException {
        if (log.isDebugEnabled()) log.debug("Hibernate update query: " + hql);
        if (log.isDebugEnabled()) log.debug("with conditions: " + conditionsList);
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            Query updateQuery = session.createQuery(hql);
            if (conditionsList != null) {
                for (Iterator it = conditionsList.iterator(); it.hasNext(); ) {
                    HQLCondition cond = (HQLCondition) it.next();
                    if (cond.getOperation().equals(HQLCondition.LIKE)) {
                        updateQuery.setParameter(cond.getField(), "%" + cond.getValue() + "%");
                    } else {
                        updateQuery.setParameter(cond.getField(), cond.getValue());
                    }
                }
            }
            int num = updateQuery.executeUpdate();
            tx.commit();
            return num;
        } catch (RuntimeException e) {
            tx.rollback();
            throw new HibernateException(e);
        }
    }

    public int getCountResult(String query) {
        return getCountResult(query, null);
    }

    public int getCountResult(String query, List arrayConditions) {
        List result = null;
        try {
            result = executeHQL(query, arrayConditions);
            if (result.size() == 0) return 0;
            Object firstResult = result.get(0);
            if (firstResult instanceof Long) {
                return ((Long) firstResult).intValue();
            }
            return ((Integer) firstResult);
        } catch (HibernateException e) {
            throw new HibernateException("Error counting results with " + query, e);
        }
    }

    public Criteria createCriteria(Class<? extends PersistedObject> persistentClass) {
        return HibernateUtil.currentSession().createCriteria(persistentClass);
    }
}
