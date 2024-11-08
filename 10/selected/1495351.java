package com.hilaver.dzmis.dao;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import com.hilaver.dzmis.Constants;
import com.hilaver.dzmis.accesscontrol.AcUser;
import com.hilaver.dzmis.sys.SysMbox;
import com.hilaver.dzmis.sys.SysMboxViewer;

public class BaseHibernateDAO {

    public Session getSession() {
        return HibernateSessionFactory.getSession();
    }

    public void saveOrUpdate(Object persistentObject) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            getSession().saveOrUpdate(persistentObject);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void delete(String className, int id) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String deleteString = "delete from " + className + " where id = ?";
            Query queryObject = getSession().createQuery(deleteString);
            queryObject.setParameter(0, id);
            queryObject.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void delete(String className, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String deleteString = "delete from " + className + getWhereStatement(filters);
            Query queryObject = getSession().createQuery(deleteString);
            queryObject.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List findByPropertyOrderById(String className, String propertyName, Object value) throws Exception {
        try {
            String queryString = "from " + className + " as model where model." + propertyName + "= ? order by id desc";
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, value);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List findByProperty(String className, String propertyName, Object value) throws Exception {
        try {
            String queryString = "from " + className + " as model where model." + propertyName + "= ?";
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, value);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List findByPropertyLike(String className, String propertyName, Object value) throws Exception {
        try {
            String queryString = "from " + className + " as model where model." + propertyName + " like ?";
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, value);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List findByExample(Object instance, String order, String dir) throws Exception {
        try {
            Criteria criteria = getSession().createCriteria(instance.getClass().getName());
            if (Constants.ORDER_ASC.equals(dir)) {
                criteria.addOrder(Order.asc(order));
            } else {
                criteria.addOrder(Order.desc(order));
            }
            Example example = Example.create(instance).excludeZeroes().ignoreCase().enableLike();
            List results = criteria.add(example).list();
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List getAllPagination(String className, int offset, int limit, String sort, String order) throws Exception {
        try {
            String queryString = "from " + className + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List getAllPagination(String className, int offset, int limit, String sort, String order, String[] filters) throws Exception {
        try {
            String queryString = "from " + className + getWhereStatement(filters) + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List getAll(String className, String sort, String order, String[] filters) throws Exception {
        try {
            String queryString = "from " + className + getWhereStatement(filters) + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List getAll(String className, String sort, String order) throws Exception {
        try {
            String queryString = "from " + className + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Object get(String className, int id) throws Exception {
        try {
            Object obj = getSession().get(className, id);
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Object get(String className, String id) throws Exception {
        try {
            Object obj = getSession().get(className, id);
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public int getCount(String className) throws Exception {
        try {
            String queryString = "select count(*) from " + className;
            Query queryObject = getSession().createQuery(queryString);
            return ((Integer) queryObject.iterate().next()).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public int getCount(String className, String[] filters) throws Exception {
        try {
            String queryString = "select count(*) from " + className + getWhereStatement(filters);
            Query queryObject = getSession().createQuery(queryString);
            return ((Integer) queryObject.iterate().next()).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void closeSession() {
        HibernateSessionFactory.closeSession();
    }

    private void deleteSysMbox(String className, int id) {
        String deleteMboxStr = "delete from " + SysMbox.class.getName() + " where className = ? and objId=?";
        Query mboxQueryObject = getSession().createQuery(deleteMboxStr);
        mboxQueryObject.setParameter(0, className);
        mboxQueryObject.setParameter(1, id);
        mboxQueryObject.executeUpdate();
    }

    protected static String getWhereStatement(String[] filters) {
        String whereStatement = "";
        if (filters == null || filters.length == 0) {
            return whereStatement;
        }
        whereStatement += " where ";
        for (int i = 0; i < filters.length; i++) {
            whereStatement += filters[i];
            if (i != filters.length - 1) {
                whereStatement += " and ";
            }
        }
        return whereStatement;
    }
}
