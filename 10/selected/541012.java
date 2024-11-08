package com.elibera.msgs.app;

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
import com.elibera.msgs.entity.ClientOnline;
import com.elibera.msgs.entity.Msg;
import com.elibera.msgs.entity.MsgBinary;
import com.elibera.msgs.threading.DBSessionStarter;
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
            sessionFactory = cfg.addPackage("com.elibera.msgs.entity").addAnnotatedClass(Msg.class).addAnnotatedClass(ClientOnline.class).addAnnotatedClass(MsgBinary.class).buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static Session getSession(MessagingClient s) {
        if (s.sess == null) s.sess = getSession();
        return s.sess;
    }

    /**
		 * get a session outside of the serverThread scope
		 * @return
		 */
    public static Session getSessionGenerall() {
        return getSession();
    }

    private static Session getSession() throws HibernateException {
        return sessionFactory.openSession();
    }

    public static void savelyCloseSession(Session sess) {
        if (sess == null) return;
        try {
            sess.close();
            sess = null;
        } catch (Exception e) {
            Log.warn("ERROR: Closing Session:" + sess, e);
        }
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
            Log.printStackTrace(e);
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
