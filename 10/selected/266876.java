package com.bifrostbridge.testinfrastructure.test.dao;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import com.bifrostbridge.testinfrastructure.util.SystemUtils;

public abstract class DAOTestUtils {

    private static com.bifrostbridge.testinfrastructure.model.Question question;

    public static com.bifrostbridge.testinfrastructure.model.User sysAccount;

    protected DAOTestUtils() {
    }

    public static void ensureDataIsCreated() {
        DAOTestUtils.createUserData();
    }

    public static void removeTestData() {
        DAOTestUtils.removeUserData();
    }

    public static void createSystemUser() {
        com.bifrostbridge.testinfrastructure.model.User system = getSystemAccount();
        DAOTestUtils.sysAccount = (com.bifrostbridge.testinfrastructure.model.Administrator) system;
    }

    public static com.bifrostbridge.testinfrastructure.model.User getSystemAccount() {
        if (DAOTestUtils.sysAccount != null) {
            return DAOTestUtils.sysAccount;
        }
        com.bifrostbridge.testinfrastructure.model.User system = DAOTestUtils.retrieve("system");
        if (system == null) {
            com.bifrostbridge.testinfrastructure.model.User user = new com.bifrostbridge.testinfrastructure.model.Administrator("system", com.bifrostbridge.testinfrastructure.model.Roles.ADMIN, SystemUtils.generatePasswordHash("system"), "System Administrator");
            DAOTestUtils.create(user);
            com.bifrostbridge.testinfrastructure.model.User test = DAOTestUtils.retrieve("system");
            if (test == null) {
                System.out.println("****** SAVE WAS NOT SUCCESSFUL");
            }
        }
        DAOTestUtils.sysAccount = system;
        return system;
    }

    private static void createUserData() {
        com.bifrostbridge.testinfrastructure.model.User system = getSystemAccount();
        DAOTestUtils.sysAccount = (com.bifrostbridge.testinfrastructure.model.Administrator) system;
        com.bifrostbridge.testinfrastructure.model.User test = DAOTestUtils.retrieve("testCandidate");
        if (test != null) {
            DAOTestUtils.delete(test);
        }
        com.bifrostbridge.testinfrastructure.model.User user = new com.bifrostbridge.testinfrastructure.model.Candidate("testCandidate", com.bifrostbridge.testinfrastructure.model.Roles.CANDIDATE, SystemUtils.generatePasswordHash("password"), "test user");
        DAOTestUtils.create(user);
        test = DAOTestUtils.retrieve("testCandidate");
        if (test == null) {
            System.out.println("******* SAVE (Candidate) WAS NOT SUCCESSFUL");
        }
        com.bifrostbridge.testinfrastructure.model.Question q = new com.bifrostbridge.testinfrastructure.model.Question("test question");
        DAOTestUtils.createQuestion(q);
        DAOTestUtils.question = DAOTestUtils.createQuestion(q);
    }

    private static void removeUserData() {
        DAOTestUtils.delete("testCandidate");
        if (DAOTestUtils.question != null) {
            DAOTestUtils.deleteQuestion(DAOTestUtils.question);
        }
    }

    private static void create(com.bifrostbridge.testinfrastructure.model.User user) {
        try {
            begin();
            getSession().save(user);
            commit();
        } catch (HibernateException he) {
            rollback();
            he.printStackTrace();
        }
    }

    private static com.bifrostbridge.testinfrastructure.model.Question createQuestion(com.bifrostbridge.testinfrastructure.model.Question q) {
        try {
            begin();
            int savedQId = (Integer) getSession().save(q);
            commit();
            com.bifrostbridge.testinfrastructure.model.Question savedQ = retrieveQuestion(savedQId);
            return savedQ;
        } catch (HibernateException he) {
            rollback();
            he.printStackTrace();
        }
        return null;
    }

    private static void delete(String userName) {
        com.bifrostbridge.testinfrastructure.model.User u = DAOTestUtils.retrieve(userName);
        delete(u);
    }

    private static void delete(com.bifrostbridge.testinfrastructure.model.User u) {
        try {
            begin();
            getSession().delete(u);
            commit();
        } catch (HibernateException he) {
            rollback();
            he.printStackTrace();
        }
    }

    public static com.bifrostbridge.testinfrastructure.model.Question getQuestion() {
        if (DAOTestUtils.question == null) {
            com.bifrostbridge.testinfrastructure.model.Question q = new com.bifrostbridge.testinfrastructure.model.Question("test question");
            DAOTestUtils.question = DAOTestUtils.createQuestion(q);
        }
        return DAOTestUtils.question;
    }

    private static void deleteQuestion(com.bifrostbridge.testinfrastructure.model.Question q) {
        try {
            begin();
            Query query = getSession().createQuery("from Question where id = :id");
            query.setInteger("id", q.getId());
            q = (com.bifrostbridge.testinfrastructure.model.Question) query.uniqueResult();
            q.deleteAnswers();
            getSession().update(q);
            commit();
            begin();
            query = getSession().createQuery("delete from Answer where question_id = :id");
            query.setInteger("id", q.getId());
            query.executeUpdate();
            getSession().delete(q);
            commit();
        } catch (HibernateException he) {
            rollback();
            he.printStackTrace();
        }
    }

    private static final Logger log = Logger.getAnonymousLogger();

    private static final ThreadLocal<Session> session = new ThreadLocal<Session>();

    private static final SessionFactory sessionFactory = new AnnotationConfiguration().configure().buildSessionFactory();

    private static Session getSession() {
        Session session = DAOTestUtils.session.get();
        if (session == null) {
            session = sessionFactory.openSession();
            DAOTestUtils.session.set(session);
        }
        return session;
    }

    private static void begin() {
        getSession().beginTransaction();
    }

    private static void commit() {
        getSession().getTransaction().commit();
    }

    private static void rollback() {
        try {
            getSession().getTransaction().rollback();
        } catch (HibernateException he) {
            log.log(Level.WARNING, "cannot rollback", he);
        }
        try {
            getSession().close();
        } catch (HibernateException he) {
            log.log(Level.WARNING, "canot close", he);
        }
        DAOTestUtils.session.set(null);
    }

    private static com.bifrostbridge.testinfrastructure.model.User retrieve(String userName) {
        com.bifrostbridge.testinfrastructure.model.User retVal = null;
        try {
            begin();
            Query q = getSession().createQuery("from User where name = :name");
            q.setString("name", userName);
            retVal = (com.bifrostbridge.testinfrastructure.model.User) q.uniqueResult();
            commit();
            return retVal;
        } catch (HibernateException he) {
            rollback();
        }
        return null;
    }

    private static com.bifrostbridge.testinfrastructure.model.Question retrieveQuestion(int id) {
        com.bifrostbridge.testinfrastructure.model.Question retVal = null;
        try {
            begin();
            Query q = getSession().createQuery("from Question where id = :id");
            q.setInteger("id", id);
            retVal = (com.bifrostbridge.testinfrastructure.model.Question) q.uniqueResult();
            commit();
            return retVal;
        } catch (HibernateException he) {
            rollback();
        }
        return null;
    }
}
