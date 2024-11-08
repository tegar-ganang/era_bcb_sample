package com.ideo.sweetdevria.proxy.hibernate;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import com.ideo.sweetdevria.proxy.exception.TechnicalException;
import com.ideo.sweetdevria.proxy.model.IUser;
import com.ideo.sweetdevria.proxy.model.User;
import com.ideo.sweetdevria.proxy.provider.IUserProvider;

public class UserProviderHibernate implements IUserProvider {

    private static final Log LOG = LogFactory.getLog(UserProviderHibernate.class);

    public UserProviderHibernate() {
    }

    public UserProviderHibernate(String configFile) {
        HibernateUtil.getSessionFactory(configFile);
    }

    public IUser create(IUser obj) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            session.save(obj);
            transaction.commit();
            LOG.info("User " + obj.getId() + " is saved successefully\n");
            return obj;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public List findAll() throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(User.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            List result = criteria.list();
            transaction.commit();
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public IUser findById(int id) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            IUser user = (IUser) session.get(User.class, new Integer(id));
            transaction.commit();
            return user;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public IUser findByName(String name) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            System.out.println("name = " + name);
            Criteria criteria = session.createCriteria(User.class).add(Restrictions.eq("name", name));
            IUser result = (IUser) criteria.uniqueResult();
            transaction.commit();
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public IUser findByLogin(String login) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            System.out.println("login = " + login);
            Criteria criteria = session.createCriteria(User.class).add(Restrictions.eq("login", login));
            IUser result = (IUser) criteria.uniqueResult();
            transaction.commit();
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public List findMaxResults(int max) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(User.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).setMaxResults(max);
            List result = criteria.list();
            transaction.commit();
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public IUser update(IUser obj) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            session.saveOrUpdate(obj);
            transaction.commit();
            return obj;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public IUser delete(IUser obj) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            session.delete(obj);
            transaction.commit();
            return obj;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public String getPermissionsForUserAndAgenda(Integer userId, Integer agendaId) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Query query = session.createSQLQuery("select permissions from j_user_agenda where userId=:userId AND agendaId=:agendaId");
            query.setParameter("userId", userId);
            query.setParameter("agendaId", agendaId);
            String permissions = (String) query.uniqueResult();
            transaction.commit();
            return permissions;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public String getPermissionsForUserAndTypeAndAgenda(Integer userId, Integer eventTypeId, Integer agendaId) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Query query = session.createSQLQuery("select permissions from j_user_type_agenda where userId=:userId AND eventTypeId=:eventTypeId AND agendaId=:agendaId");
            query.setParameter("userId", userId);
            query.setParameter("eventTypeId", eventTypeId);
            query.setParameter("agendaId", agendaId);
            String permissions = (String) query.uniqueResult();
            transaction.commit();
            return permissions;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public int addPermissionsForUserAndAgenda(Integer userId, Integer agendaId, String permissions) throws TechnicalException {
        if (permissions == null) {
            throw new TechnicalException(new Exception(new Exception("Column 'permissions' cannot be null")));
        }
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            String query = "INSERT INTO j_user_agenda (userId, agendaId, permissions) VALUES(" + userId + "," + agendaId + ",\"" + permissions + "\")";
            Statement statement = session.connection().createStatement();
            int rowsUpdated = statement.executeUpdate(query);
            transaction.commit();
            return rowsUpdated;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        } catch (SQLException e) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(e);
        }
    }

    public int addPermissionsForUserAndTypeAndAgenda(Integer userId, Integer eventTypeId, Integer agendaId, String permissions) throws TechnicalException {
        if (permissions == null) {
            throw new TechnicalException(new Exception(new Exception("Column 'permissions' cannot be null")));
        }
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            String query = "INSERT INTO j_user_type_agenda (userId, agendaId, eventTypeId, permissions) VALUES(" + userId + "," + agendaId + "," + eventTypeId + ",\"" + permissions + "\")";
            Statement statement = session.connection().createStatement();
            int rowsUpdated = statement.executeUpdate(query);
            transaction.commit();
            return rowsUpdated;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        } catch (SQLException e) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(e);
        }
    }

    public int updatePermissionsForUserAndAgenda(Integer userId, Integer agendaId, String permissions) throws TechnicalException {
        if (permissions == null) {
            throw new TechnicalException(new Exception(new Exception("Column 'permissions' cannot be null")));
        }
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            String query = "UPDATE j_user_agenda SET permissions=\"" + permissions + "\" where userId=" + userId + " AND agendaId=" + agendaId;
            Statement statement = session.connection().createStatement();
            int rowsUpdated = statement.executeUpdate(query);
            transaction.commit();
            return rowsUpdated;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        } catch (SQLException e) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(e);
        }
    }

    public int updatePermissionsForUserAndTypeAndAgenda(Integer userId, Integer eventTypeId, Integer agendaId, String permissions) throws TechnicalException {
        if (permissions == null) {
            throw new TechnicalException(new Exception(new Exception("Column 'permissions' cannot be null")));
        }
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            String query = "UPDATE j_user_type_agenda SET permissions=\"" + permissions + "\" where userId=" + userId + " AND eventTypeId=" + eventTypeId + " AND agendaId=" + agendaId;
            Statement statement = session.connection().createStatement();
            int rowsUpdated = statement.executeUpdate(query);
            transaction.commit();
            return rowsUpdated;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        } catch (SQLException e) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(e);
        }
    }

    public int deletePermissionsForUserAndAgenda(Integer userId, Integer agendaId) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            String query = "DELETE FROM j_user_agenda where userId=" + userId + " AND agendaId=" + agendaId;
            Statement statement = session.connection().createStatement();
            int rowsUpdated = statement.executeUpdate(query);
            transaction.commit();
            return rowsUpdated;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        } catch (SQLException e) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(e);
        }
    }

    public int deletePermissionsForUserAndTypeAndAgenda(Integer userId, Integer eventTypeId, Integer agendaId) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            String query = "DELETE FROM j_user_type_agenda where userId=" + userId + " AND eventTypeId=" + eventTypeId + " AND agendaId=" + agendaId;
            Statement statement = session.connection().createStatement();
            int rowsUpdated = statement.executeUpdate(query);
            transaction.commit();
            return rowsUpdated;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        } catch (SQLException e) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(e);
        }
    }

    public List getFieldList() throws TechnicalException {
        return Arrays.asList(new String[] { "id", "name", "login", "password" });
    }

    public IUser validateModelParams(Map javaParams, String actionType) {
        IUser user = (IUser) new User();
        if (!"add".equals(actionType)) {
            user.setId((Integer) javaParams.get("id"));
        }
        return user;
    }
}
