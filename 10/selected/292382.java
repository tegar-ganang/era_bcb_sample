package com.elibera.gateway.app;

import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import com.elibera.gateway.entity.BlockedUser;
import com.elibera.gateway.entity.QueryLog;
import com.elibera.gateway.entity.ServerEntry;
import com.elibera.gateway.entity.SessionLog;
import com.elibera.gateway.entity.UserData;
import com.elibera.gateway.entity.Werbung;
import com.elibera.gateway.threading.DBSessionStarter;
import com.elibera.util.Log;

/**
 * @author meisi
 *
 */
public class HibernateUtil implements DBSessionStarter {

    private static SessionFactory sessionFactory;

    /**
	 * inits the Sessionfactory, the RessourceBundle contains the hibernate.properties
	 * @param hibernateBundle
	 */
    public static void init(ResourceBundle hibernateBundle) {
        try {
            AnnotationConfiguration cfg = new AnnotationConfiguration();
            Enumeration<String> en = hibernateBundle.getKeys();
            while (en.hasMoreElements()) {
                String key = en.nextElement();
                cfg.setProperty(key, hibernateBundle.getString(key));
            }
            cfg = cfg.addPackage("com.elibera.gateway.entity").addAnnotatedClass(SessionLog.class).addAnnotatedClass(ServerEntry.class).addAnnotatedClass(QueryLog.class).addAnnotatedClass(BlockedUser.class).addAnnotatedClass(Werbung.class).addAnnotatedClass(UserData.class);
            if (RedirectServer.extension != null) {
                cfg = RedirectServer.extension.initHibernateEntities(cfg);
            }
            sessionFactory = cfg.buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static Session getSessionStd() throws HibernateException {
        return sessionFactory.openSession();
    }

    /**
	     * close the session
	     * @param sess
	     */
    public static void savelyCloseSession(Session sess) {
        if (sess == null) return;
        try {
            sess.close();
            sess = null;
        } catch (Exception e) {
            Log.error("ERROR: Closing Session:" + sess, e);
        }
    }

    /**
	 * get a session outside of the serverThread scope
	 * @return
	 */
    public static Session getSessionGenerall() {
        return getSessionStd();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public Session getNewSession() {
        return getSessionGenerall();
    }

    /**
	 * executes a DB save command, catches the exception from the transaction
	 * makes a rollback and throws the exception
	 * @param o
	 * @param em
	 * @return
	 * @throws Exception
	 */
    public static Object executeSave(Object o, Session em) throws Exception {
        Transaction t = null;
        Object o2 = null;
        try {
            t = em.beginTransaction();
            o2 = em.save(o);
            t.commit();
            return o2;
        } catch (Exception e) {
            try {
                t.rollback();
            } catch (Exception ee) {
            }
            throw e;
        }
    }

    /**
	 * executes a DB update command, catches the exception from the transaction
	 * makes a rollback and throws the exception
	 * @param o
	 * @param em
	 * @throws Exception
	 */
    public static void executeUpdate(Object o, Session em) throws Exception {
        Transaction t = null;
        try {
            t = em.beginTransaction();
            em.update(o);
            t.commit();
        } catch (Exception e) {
            try {
                t.rollback();
            } catch (Exception ee) {
            }
            throw e;
        }
    }

    /**
	 * executes a DB delete command, catches the exception from the transaction
	 * makes a rollback and throws the exception
	 * @param o
	 * @param em
	 * @throws Exception
	 */
    public static void executeDelete(Object o, Session em) throws Exception {
        Transaction t = null;
        try {
            t = em.beginTransaction();
            em.refresh(o);
            em.delete(o);
            t.commit();
        } catch (Exception e) {
            try {
                t.rollback();
            } catch (Exception ee) {
            }
            throw e;
        }
    }

    /**
	 * executes a DB merge command, catches the exception from the transaction
	 * makes a rollback and throws the exception
	 * @param o
	 * @param em
	 * @return
	 * @throws Exception
	 */
    public static Object executeMerge(Object o, Session em) throws Exception {
        Transaction t = null;
        Object o2 = null;
        try {
            t = em.beginTransaction();
            o2 = em.merge(o);
            t.commit();
            return o2;
        } catch (Exception e) {
            try {
                t.rollback();
            } catch (Exception ee) {
            }
            throw e;
        }
    }

    /**
	 * executes an update for this query
	 * @param q
	 * @param em
	 * @return
	 * @throws Exception
	 */
    public static int executeUpdateQuery(Query q, Session em) throws Exception {
        Transaction t = null;
        int ret = -1;
        try {
            t = em.beginTransaction();
            ret = q.executeUpdate();
            t.commit();
            return ret;
        } catch (Exception e) {
            try {
                t.rollback();
            } catch (Exception ee) {
            }
            throw e;
        }
    }
}
