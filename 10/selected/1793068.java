package com.budee.crm.dao.core;

import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Example;
import com.budee.crm.dao.helper.IWhereStatementGenerator;
import com.budee.crm.dao.helper.NormalWhereStatementGenerator;
import com.budee.crm.pojo.accesscontrol.AcUser;

public class BaseHibernateDAO {

    protected IWhereStatementGenerator wsGenerator = new NormalWhereStatementGenerator();

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

    public void delete(String className, String[] ids) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer deleteSB = new StringBuffer();
            deleteSB.append("delete from " + className + " where id in (");
            for (int i = 0; i < ids.length; i++) {
                deleteSB.append(ids[i]);
                if (i < ids.length - 1) {
                    deleteSB.append(", ");
                }
            }
            deleteSB.append(")");
            Query queryObject = getSession().createQuery(deleteSB.toString());
            queryObject.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List findByPropertyOrderById(String className, String propertyName, Object value) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + className + " as model where model." + propertyName + "= ? order by id desc";
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, value);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List findByProperty(String className, String propertyName, Object value) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + className + " as model where model." + propertyName + "= ?";
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, value);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List findByProperty(String className, String propertyName, Object value, String order, String dir) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + className + " as model where model." + propertyName + "= '" + value + "' order by " + order + " " + dir;
            Query queryObject = getSession().createQuery(queryString);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List findByPropertyLike(String className, String propertyName, Object value) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + className + " as model where model." + propertyName + " like ?";
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, value);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List findByExample(Object instance) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            Criteria criteria = getSession().createCriteria(instance.getClass().getName());
            Example example = Example.create(instance).excludeZeroes().ignoreCase().enableLike();
            List results = criteria.add(example).list();
            tx.commit();
            return results;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List getAllPagination(String className, int offset, int limit, String sort, String order) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + className + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List results = queryObject.list();
            return results;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List getAllPagination(String className, int offset, int limit, String sort, String order, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + className + getWhereStatement(filters) + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List queryAllPagination(String className, String query, String[] fields, int offset, int limit, String sort, String order, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String whereStatement = getWhereStatement(filters);
            String queryString = "from " + className + whereStatement + ("".equals(whereStatement) ? "" : " and ") + getQueryStr(query, fields) + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public int queryCount(String className, String query, String[] fields, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String whereStatement = getWhereStatement(filters);
            String queryString = "select count(*) from " + className + whereStatement + ("".equals(whereStatement) ? "" : " and ") + getQueryStr(query, fields);
            Query queryObject = getSession().createQuery(queryString);
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List getAll(String className, String sort, String order, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + className + getWhereStatement(filters) + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List getAll(String className, String sort, String order) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + className + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Object get(String className, int id) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            Object obj = getSession().get(className, id);
            tx.commit();
            return obj;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Object get(String className, String id) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            Object obj = getSession().get(className, id);
            tx.commit();
            return obj;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public int getCount(String className) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "select count(*) from " + className;
            Query queryObject = getSession().createQuery(queryString);
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public int getCount(String className, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "select count(*) from " + className + getWhereStatement(filters);
            Query queryObject = getSession().createQuery(queryString);
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void closeSession() {
        HibernateSessionFactory.closeSession();
    }

    public List exeHql(String hql, int offset, int limit) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            Query queryObject = getSession().createQuery(hql);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List exeHql(String hql) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            Query rtn = getSession().createQuery(hql);
            List results = rtn.list();
            tx.commit();
            return results;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public int exeCountHql(String hql) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            Query queryObject = getSession().createQuery(hql);
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    protected static String getQueryStr(String query, String[] fields) {
        if (query == null || fields == null) {
            return "";
        }
        String queryStr = "";
        for (int i = 0; i < fields.length; i++) {
            if (i == 0) {
                queryStr += " (";
            }
            queryStr += fields[i];
            queryStr += " like '%";
            queryStr += query;
            queryStr += "%'";
            if (i != fields.length - 1) {
                queryStr += " OR ";
            } else {
                queryStr += ") ";
            }
        }
        return queryStr;
    }

    public IWhereStatementGenerator getWsGenerator() {
        return wsGenerator;
    }

    public void setWsGenerator(IWhereStatementGenerator wsGenerator) {
        this.wsGenerator = wsGenerator;
    }

    protected String getWhereStatement(String[] filters) {
        return this.wsGenerator.get(filters);
    }

    protected static String getWhereClause(String[] filters) {
        String whereStatement = "";
        if (filters == null || filters.length == 0) {
            return whereStatement;
        }
        for (int i = 0; i < filters.length; i++) {
            whereStatement += filters[i];
            whereStatement += " and ";
        }
        return whereStatement;
    }
}
