package com.pioneer.app.proview;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Transaction;
import config.BaseHibernateDAO;

public class RoleProcessDAO extends BaseHibernateDAO {

    private static final Log log = LogFactory.getLog(RoleProcess.class);

    public RoleProcess findById(java.lang.Integer id) {
        log.debug("getting RoleProcess instance with id: " + id);
        try {
            RoleProcess instance = (RoleProcess) getSession().get("com.pioneer.app.proview.RoleProcess", id);
            return instance;
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public void delete(RoleProcess persistentInstance) {
        log.debug("deletting RoleProcess persistentInstance  ");
        try {
            getSession().delete(persistentInstance);
            log.debug("delete successful");
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public void add(RoleProcess persistentInstance) {
        log.debug("deletting RoleProcess persistentInstance  ");
        try {
            getSession().save(persistentInstance);
            log.debug("add successful");
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public void update(RoleProcess persistentInstance) {
        log.debug("deletting RoleProcess persistentInstance  ");
        try {
            getSession().update(persistentInstance);
            log.debug("update successful");
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public List findByCondition(String condition) {
        log.debug("finding by condition " + condition);
        try {
            String queryString = null;
            if (null != condition) {
                queryString = "from RoleProcess where " + condition;
            } else {
                queryString = "from RoleProcess";
            }
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setMaxResults(500);
            return queryObject.list();
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public void deleteObjs(java.lang.Integer[] ids) {
        log.debug("deleting by objects ");
        try {
            if (null != ids) {
                RoleProcess obj = null;
                java.lang.Integer id = null;
                for (int i = 0; i < ids.length; i++) {
                    id = ids[i];
                    obj = this.findById(id);
                    this.delete(obj);
                }
            }
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    /****************条件删除****************/
    public void deleteByCondition(String condition) {
        Transaction tx = getSession().beginTransaction();
        try {
            String hql = "delete RoleProcess ";
            if (null != condition) {
                hql += "where ";
                hql += condition;
            }
            getSession().createQuery(hql).executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        }
    }

    /****************根据属性查找****************/
    public List findByProperty(String propertyName, Object value) {
        try {
            String queryString = "from RoleProcess as model where model." + propertyName + "= ?";
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, value);
            return queryObject.list();
        } catch (RuntimeException re) {
            throw re;
        }
    }
}
