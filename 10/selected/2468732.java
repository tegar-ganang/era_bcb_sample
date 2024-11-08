package cf.e_commerce.base.db.service;

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import cf.e_commerce.base.db.entity.BaseBean;

/**
 * Base class for all database services
 * 
 * @author Felipe Melo
 * 
 */
public abstract class BaseService {

    /**
	 * Rolls back a list of beans
	 * 
	 * @param rollbackList
	 *            List<BaseBean> with beans
	 * @param session
	 *            Session with database
	 * @throws HibernateException
	 *             throw when problems occur while accessing database
	 */
    public final void rollback(List<BaseBean> rollbackList, Session session) throws HibernateException {
        for (int i = rollbackList.size(); --i >= 0; ) session.delete(rollbackList.get(i));
    }

    /**
	 * Loads a BaseBean from database
	 * 
	 * @param beanClass
	 *            Class of the bean to be loaded
	 * @param id
	 *            Integer with id
	 * @param session
	 *            Session with database
	 * @return BaseBean instance
	 * @throws HibernateException
	 *             thrown if problems occur with database access
	 */
    public final BaseBean load(Class<BaseBean> beanClass, Integer id, Session session) throws HibernateException {
        Query query = session.createQuery("from " + beanClass.getName() + " o where o.id =" + id);
        BaseBean result = (BaseBean) query.uniqueResult();
        session.close();
        return result;
    }

    /**
	 * Deletes a bean form database
	 * @param beanClass Class<BaseBean> with bean class
	 * @param id int with bean id
	 * @param session Session with database
	 * @return int with amount of deleted beans
	 * @throws HibernateException thrown if problems occur during database access 
	 */
    @SuppressWarnings("unchecked")
    public final int delete(Class beanClass, int id, Session session) throws HibernateException {
        Query query = session.createQuery("delete from " + beanClass.getName() + " o where o.id =" + id);
        Transaction tx = null;
        int result = -1;
        try {
            tx = session.beginTransaction();
            result = query.executeUpdate();
            tx.commit();
            session.flush();
            session.close();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            session.close();
            throw e;
        }
        return result;
    }

    /**
	 * Deletes a bean form database
	 * @param List<DeletableBean> with data of item to be deleted
	 * @param session Session with database
	 * @return int with amount of deleted beans
	 * @throws HibernateException thrown if problems occur during database access 
	 */
    public final int delete(List<DeletableBean> deletableList, Session session) throws HibernateException {
        Transaction tx = null;
        int i, result = 0, size = deletableList.size();
        DeletableBean deletable = null;
        try {
            tx = session.beginTransaction();
            for (i = 0; i < size; i++) {
                deletable = deletableList.get(i);
                result += session.createQuery("delete from " + deletable.getBeanClass() + " o where o.id =" + deletable.getId()).executeUpdate();
            }
            tx.commit();
            session.flush();
            session.close();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            session.close();
            throw e;
        }
        return result;
    }

    /**
	 * Updates a bean register in database
	 * @param bean BaseBean with bean instance
	 * @param session Session with database
	 * @throws HibernateException thrown if any problem happens during database access
	 */
    public final void update(BaseBean bean, Session session) throws HibernateException {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(bean);
            tx.commit();
            session.flush();
            session.close();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            session.close();
            throw e;
        }
    }

    /**
	 * Updates a list of beans registered in database
	 * @param beanList List<BaseBean> with bean instances to be updated
	 * @param session Session with database
	 * @throws HibernateException thrown if any problem happens during database access
	 */
    public final void update(List<BaseBean> beanList, Session session) throws HibernateException {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            for (int i = beanList.size(); --i >= 0; ) session.update(beanList.get(i));
            tx.commit();
            session.flush();
            session.close();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            session.close();
            throw e;
        }
    }

    /**
	 * Encapsulates data about a bean able to be deleted
	 * 
	 * @author Felipe Melo
	 * 
	 */
    public static class DeletableBean {

        private String beanClass;

        private int id;

        /**
		 * Constructor
		 * 
		 * @param beanClass
		 *            String with the name of the class of the bean to
		 *            be deleted
		 * @param id
		 *            int with bean id
		 */
        public DeletableBean(String beanClass, int id) {
            this.beanClass = beanClass;
            this.id = id;
        }

        /**
		 * Standard getter for bean class
		 * 
		 * @return String with the class
		 */
        public String getBeanClass() {
            return beanClass;
        }

        /**
		 * Standard setter for bean class
		 * 
		 * @param beanClass
		 *            String with bean class
		 */
        public void setBeanClass(String beanClass) {
            this.beanClass = beanClass;
        }

        /**
		 * Standard getter for bean id
		 * 
		 * @return int with id
		 */
        public int getId() {
            return id;
        }

        /**
		 * Standard setter for bean id
		 * 
		 * @param id
		 *            int with bean id
		 */
        public void setId(int id) {
            this.id = id;
        }
    }
}
