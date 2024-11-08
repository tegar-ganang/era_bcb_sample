package spamwatch.base;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;

public class DB {

    private static Log log = LogFactory.getLog(DB.class);

    private static boolean externalDB;

    private static SessionFactory sessionFactory;

    private static Session commonSession;

    private static List<Session> sessions = new LinkedList<Session>();

    public static synchronized void init(boolean externalDB) throws SQLException {
        DB.externalDB = externalDB;
        String configRessource = "/hibernate.cfg.xml";
        if (externalDB) {
            configRessource = "/hibernate_extern.cfg.xml";
        }
        try {
            AnnotationConfiguration configuration = new AnnotationConfiguration();
            sessionFactory = configuration.configure(configRessource).buildSessionFactory();
            commonSession = createSession();
        } catch (HibernateException ex) {
            throw new SQLException("Exception building SessionFactory: " + ex.getMessage(), ex);
        }
        try {
            synchronized (commonSession) {
                Transaction tx = commonSession.beginTransaction();
                tx.commit();
            }
        } catch (Exception e) {
            throw new SQLException("Unable to access database.", e);
        }
    }

    public static synchronized void setup(String clientVersion) throws SQLException {
        synchronized (commonSession) {
            String dbVersion;
            try {
                dbVersion = (String) commonSession.createSQLQuery("select version from spamwatch_metadata").addScalar("version", Hibernate.STRING).uniqueResult();
            } catch (Exception e) {
                dbVersion = "not found";
            }
            if (!dbVersion.equals(clientVersion)) {
                String message = "Client version (" + clientVersion + ") differs from database version (" + dbVersion + ")!\nThe database will be updated now.";
                int result = JOptionPane.showConfirmDialog(null, message, "Database not compatible", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION) {
                    System.out.println("Database update canceled.");
                    System.exit(1);
                }
                if (dbVersion.equals("not found")) {
                    Transaction tx = commonSession.beginTransaction();
                    try {
                        InputStream in = ClassLoader.getSystemResourceAsStream("spamwatch/base/sql/2.2.sql");
                        BufferedReader r = new BufferedReader(new InputStreamReader(in));
                        while (r.ready()) {
                            commonSession.createSQLQuery(r.readLine()).executeUpdate();
                        }
                        tx.commit();
                        dbVersion = "2.2";
                    } catch (Exception exc) {
                        tx.rollback();
                        throw new SQLException("Exception while updating.", exc);
                    }
                }
                backup();
            }
        }
    }

    public static synchronized void clean() {
        synchronized (commonSession) {
            Transaction tx = commonSession.beginTransaction();
            try {
                Query query = commonSession.createQuery("select store from EMailFolder folder right join folder.emailStores as store where folder=null");
                ScrollableResults scroll = query.scroll(ScrollMode.FORWARD_ONLY);
                while (scroll.next()) {
                    commonSession.delete(scroll.get()[0]);
                }
                scroll.close();
                tx.commit();
            } catch (HibernateException e) {
                log.error("Exception during clean()", e);
                tx.rollback();
            }
        }
    }

    public static synchronized void close() {
        for (Session session : sessions) {
            synchronized (session) {
                Transaction tx = session.beginTransaction();
                try {
                    session.flush();
                    tx.commit();
                } catch (HibernateException e1) {
                    log.warn("Exception in close: ", e1);
                    tx.rollback();
                }
                try {
                    Connection connection = session.close();
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    log.warn("Exception during close", e);
                }
            }
        }
        sessionFactory.close();
        if (externalDB) {
        } else {
        }
    }

    public static synchronized String backup() {
        String filename = "db/backup-";
        filename += new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        filename += ".zip";
        final String ffilename = filename;
        synchronized (sessions) {
            executeInCompleteSync(sessions.iterator(), new Runnable() {

                @Override
                public void run() {
                    Transaction tx = commonSession.beginTransaction();
                    commonSession.createSQLQuery("SCRIPT DROP TO '" + ffilename + "' COMPRESSION ZIP").list();
                    tx.commit();
                }
            });
        }
        return filename;
    }

    /**
	 * Can be called on a closed DB only.
	 * @param input
	 * @throws SQLException 
	 */
    public static synchronized void restore(String input) throws SQLException {
        String configRessource = "/hibernate.cfg.xml";
        if (externalDB) {
            configRessource = "/hibernate_extern.cfg.xml";
        }
        try {
            AnnotationConfiguration configuration = new AnnotationConfiguration();
            String conURL = configuration.configure(configRessource).getProperty("connection.url");
            Connection conn = DriverManager.getConnection(conURL, "sa", "");
            String sql = "RUNSCRIPT FROM '" + input + "' COMPRESSION ZIP";
            log.debug(sql);
            conn.createStatement().execute(sql);
            conn.commit();
            conn.close();
        } catch (HibernateException ex) {
            throw new SQLException("Exception building SessionFactory: " + ex.getMessage(), ex);
        }
    }

    public static synchronized Session getCommonSession() {
        return commonSession;
    }

    public static void checkpoint() {
    }

    /**
	 * Synchronizes on all sessions and executes a given Runnable.
	 * @param sessionIter
	 */
    private static void executeInCompleteSync(Iterator<Session> sessionIter, Runnable runner) {
        if (sessionIter.hasNext()) {
            synchronized (sessionIter.next()) {
                executeInCompleteSync(sessionIter, runner);
            }
        } else {
            runner.run();
        }
    }

    public static synchronized Session createSession() {
        synchronized (sessions) {
            Session newSession;
            try {
                newSession = sessionFactory.openSession();
                newSession.setCacheMode(CacheMode.IGNORE);
                sessions.add(newSession);
                return newSession;
            } catch (HibernateException e) {
                return null;
            }
        }
    }

    public static String makeValid(String s, int maxLength) {
        if (s == null) {
            return "";
        } else if (s.length() > maxLength) {
            log.warn("need to truncate a string: " + s.substring(0, 10));
            s = s.substring(0, maxLength - 5) + "[...]";
        }
        return s;
    }
}
