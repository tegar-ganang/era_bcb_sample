package com.kiasolutions.common.dalc.jpa.rl;

import com.kiasolutions.common.dalc.jpa.BadUpdateException;
import com.kiasolutions.common.dalc.jpa.ExceptionManager;
import com.kiasolutions.common.dalc.jpa.JPAUtil;
import com.kiasolutions.common.dalc.jpa.JpaOperation;
import com.kiasolutions.common.dalc.jpa.NotFoundException;
import com.kiasolutions.common.dalc.jpa.jta.JtaDao;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

/**
 *
 * @param <K> 
 * @param <T>
 * @author Rolando Steep Quezada Martínez <rquezada@kiasolutions.com>
 */
public abstract class RlDao<K, T> extends JtaDao<K, T> {

    /**
     *
     */
    public RlDao() {
    }

    public List<T> synchronizedFind(String jpql, int batchSize, int index, Map parameters) throws NotFoundException {
        List<T> list = null;
        try {
            Query consulta = jpql.contains(" ") ? createQuery(jpql, parameters) : createNamedQuery(jpql, parameters);
            list = JPAUtil.setRefresh(consulta, getEntityManager()).setMaxResults(batchSize).setFirstResult(index).setFlushMode(FlushModeType.AUTO).getResultList();
        } catch (Exception ex) {
            PersistenceException t = ExceptionManager.getJpaThrowable(JpaOperation.GET_RESULT_LIST, ex, getEntityManager(), null);
            throw new NotFoundException(t.getMessage(), t.getCause());
        }
        if (list == null || list.size() == 0) {
            list = new ArrayList<T>();
        }
        return list;
    }

    public List<T> find(String jpql, int batchSize, int index, Map parameters) throws NotFoundException {
        List<T> list = null;
        try {
            Query consulta = jpql.contains(" ") ? createQuery(jpql, parameters) : createNamedQuery(jpql, parameters);
            list = consulta.setMaxResults(batchSize).setFirstResult(index).setFlushMode(FlushModeType.AUTO).getResultList();
        } catch (Exception ex) {
            PersistenceException t = ExceptionManager.getJpaThrowable(JpaOperation.GET_RESULT_LIST, ex, getEntityManager(), null);
            throw new NotFoundException(t.getMessage(), t.getCause());
        }
        if (list == null || list.size() == 0) {
            list = new ArrayList<T>();
        }
        return list;
    }

    public List<T> synchronizedFind(String jpql, Map parameters) throws NotFoundException {
        List<T> list = null;
        try {
            Query consulta = jpql.contains(" ") ? createQuery(jpql, parameters) : createNamedQuery(jpql, parameters);
            list = JPAUtil.setRefresh(consulta, getEntityManager()).setFlushMode(FlushModeType.AUTO).getResultList();
        } catch (Exception ex) {
            PersistenceException t = ExceptionManager.getJpaThrowable(JpaOperation.GET_RESULT_LIST, ex, getEntityManager(), null);
            throw new NotFoundException(t.getMessage(), t.getCause());
        }
        if (list == null || list.size() == 0) {
            list = new ArrayList<T>();
        }
        return list;
    }

    public List<T> find(String jpql, Map parameters) throws NotFoundException {
        List<T> list = null;
        try {
            Query consulta = jpql.contains(" ") ? createQuery(jpql, parameters) : createNamedQuery(jpql, parameters);
            list = consulta.setFlushMode(FlushModeType.AUTO).getResultList();
        } catch (Exception ex) {
            PersistenceException t = ExceptionManager.getJpaThrowable(JpaOperation.GET_RESULT_LIST, ex, getEntityManager(), null);
            throw new NotFoundException(t.getMessage(), t.getCause());
        }
        if (list == null || list.size() == 0) {
            list = new ArrayList<T>();
        }
        return list;
    }

    public int update(String jpql, Map parameters) throws BadUpdateException, NotFoundException {
        int rpta = 0;
        getEntityManager().setFlushMode(FlushModeType.COMMIT);
        EntityTransaction tx = getEntityManager().getTransaction();
        try {
            tx.begin();
            Query consulta = jpql.contains(" ") ? createQuery(jpql, parameters) : createNamedQuery(jpql, parameters);
            rpta = consulta.executeUpdate();
            tx.commit();
        } catch (Exception ex) {
            PersistenceException t = ExceptionManager.getJpaThrowable(JpaOperation.EXECUTE_UPDATE, ex, getEntityManager(), null);
            throw new BadUpdateException(t.getMessage(), t.getCause());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        return rpta;
    }

    public int executeNativeUpdate(String jpql, Map parameters) throws NotFoundException {
        int rpta = 0;
        getEntityManager().setFlushMode(FlushModeType.COMMIT);
        EntityTransaction tx = getEntityManager().getTransaction();
        try {
            tx.begin();
            rpta = createNativeQuery(jpql, parameters).executeUpdate();
            tx.commit();
        } catch (Exception ex) {
            PersistenceException t = ExceptionManager.getJpaThrowable(JpaOperation.EXECUTE_UPDATE, ex, getEntityManager(), null);
            throw new NotFoundException(t.getMessage(), t.getCause());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        return rpta;
    }
}
