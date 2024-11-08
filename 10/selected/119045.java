package br.ind.nikon.services.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.type.Type;

public abstract class BaseManager {

    protected final Session _session;

    protected BaseManager(Session session) {
        this._session = session;
    }

    protected Serializable save(Object obj) throws HibernateException {
        Serializable id = null;
        try {
            _session.beginTransaction();
            id = _session.save(obj);
            _session.getTransaction().commit();
            return id;
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }

    protected void update(Object obj) throws HibernateException {
        try {
            _session.beginTransaction();
            _session.update(obj);
            _session.getTransaction().commit();
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }

    protected void saveOrUpdate(Object obj) throws HibernateException {
        try {
            _session.beginTransaction();
            _session.saveOrUpdate(obj);
            _session.getTransaction().commit();
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }

    protected Object get(Class clazz, Serializable id) {
        try {
            _session.beginTransaction();
            Object obj = _session.load(clazz, id);
            _session.getTransaction().commit();
            return obj;
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }

    protected List checkProperty(Class clazz, String propertyName, Object propertyValue) throws HibernateException {
        try {
            _session.beginTransaction();
            final List result = _session.createCriteria(clazz).add(Expression.eq(propertyName, propertyValue)).list();
            _session.getTransaction().commit();
            return result;
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }

    protected List list(Class clazz) throws HibernateException {
        try {
            _session.beginTransaction();
            List list = _session.createCriteria(clazz).list();
            _session.getTransaction().commit();
            return list;
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }

    protected void delete(Class clazz, Serializable id) throws HibernateException {
        delete(get(clazz, id));
    }

    protected void delete(Object obj) throws HibernateException {
        try {
            _session.beginTransaction();
            _session.delete(obj);
            _session.getTransaction().commit();
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }

    protected int delete(String queryName, String[] params, Object[] values, Type[] types) throws HibernateException {
        try {
            _session.beginTransaction();
            Query q = _session.getNamedQuery(queryName);
            for (int i = 0; i < params.length; i++) q.setParameter(params[i], values[i], types[i]);
            int deletedEntities = q.executeUpdate();
            _session.getTransaction().commit();
            return deletedEntities;
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    protected List namedQuery(String queryName, String[] params, Object[] values, Type[] types) throws HibernateException {
        try {
            final List elements = new ArrayList();
            _session.beginTransaction();
            Query q = _session.getNamedQuery(queryName);
            for (int i = 0; i < params.length; i++) q.setParameter(params[i], values[i], types[i]);
            System.out.println(q.getQueryString());
            elements.addAll(q.list());
            _session.getTransaction().commit();
            return elements;
        } catch (HibernateException e) {
            _session.getTransaction().rollback();
            throw e;
        }
    }
}
