package com.pioneer.app.proview;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Transaction;
import config.BaseHibernateDAO;

public class RoleUserDAO extends BaseHibernateDAO {

    private static final Log log = LogFactory.getLog(RoleUser.class);

    public RoleUser findById(java.lang.Integer id) {
        log.debug("getting RoleUser instance with id: " + id);
        try {
            RoleUser instance = (RoleUser) getSession().get("com.pioneer.app.proview.RoleUser", id);
            return instance;
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public void delete(RoleUser persistentInstance) {
        log.debug("deletting RoleUser persistentInstance  ");
        try {
            getSession().delete(persistentInstance);
            log.debug("delete successful");
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public void add(RoleUser persistentInstance) {
        log.debug("deletting RoleUser persistentInstance  ");
        try {
            getSession().save(persistentInstance);
            log.debug("add successful");
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }

    public void update(RoleUser persistentInstance) {
        log.debug("deletting RoleUser persistentInstance  ");
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
                queryString = "from RoleUser where " + condition;
            } else {
                queryString = "from RoleUser";
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
                RoleUser obj = null;
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
            String hql = "delete RoleUser ";
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
            String queryString = "from RoleUser as model where model." + propertyName + "= ?";
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, value);
            return queryObject.list();
        } catch (RuntimeException re) {
            throw re;
        }
    }
}
