package com.bargain.model;

import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import java.util.*;
import com.bargain.utils.Pager;

public class Schema {

    private static SessionFactory _sessions = null;

    public Schema() {
    }

    public Session getSession() {
        if (_sessions == null) {
            _sessions = new Configuration().configure().buildSessionFactory();
        }
        return _sessions.openSession();
    }

    public Query getQueryObject(String hql) {
        Session session = getSession();
        return session.createQuery(hql);
    }

    public Boolean isUnique(String name) {
        return true;
    }

    public Boolean isUnique(String name, int id) {
        return true;
    }

    public Schema create() {
        Session session = getSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(this);
            tx.commit();
            tx = null;
            session.refresh(this);
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
        return this;
    }

    public void update() {
        Session session = getSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(this);
            tx.commit();
            tx = null;
            session.refresh(this);
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public Schema getObject(Query query) {
        Session session = getSession();
        Schema object = null;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            object = (Schema) query.uniqueResult();
            tx.commit();
            tx = null;
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return object;
    }

    public int executeQuery(Query query) {
        Session session = getSession();
        int affectedRows = 0;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            affectedRows = query.executeUpdate();
            tx.commit();
            tx = null;
            session.refresh(this);
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return affectedRows;
    }

    public List getObjectList(Query query) {
        Session session = getSession();
        List results = null;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            results = query.list();
            tx.commit();
            tx = null;
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return results;
    }

    public List getPagedObjectList(Query query, Pager pager) {
        query.setFirstResult(pager.getFirstResult());
        query.setMaxResults(pager.getRowsPerPage());
        return getObjectList(query);
    }
}
