package org.javenue.util.process;

import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Benjamin Possolo
 * <p>Created on Jan 5, 2009
 */
public abstract class AbstractDerbyBackedTest {

    protected static HibernateTransitionManager transitionManager;

    private static HibernateProcessStatePersister processStatePersister;

    private static SessionFactory sf;

    private static final String createTableSql = "CREATE TABLE person_process_state " + "(id SMALLINT NOT NULL, state SMALLINT NOT NULL, " + "previousState SMALLINT NOT NULL, firstname VARCHAR(255), " + "lastname VARCHAR(255), age INT, sex CHAR, height VARCHAR(10), " + "CONSTRAINT id_pk PRIMARY KEY (id))";

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        sf = new Configuration().configure().buildSessionFactory();
        createPersonProcessStateTable();
        processStatePersister = new HibernateProcessStatePersister();
        processStatePersister.setSessionFactory(sf);
        transitionManager = new HibernateTransitionManager();
        transitionManager.setProcessStatePersister(processStatePersister);
        transitionManager.setSessionFactory(sf);
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        if (sf != null) sf.close();
    }

    private static void createPersonProcessStateTable() {
        try {
            sf.getCurrentSession().beginTransaction();
            sf.getCurrentSession().createSQLQuery(createTableSql).executeUpdate();
            sf.getCurrentSession().getTransaction().commit();
        } catch (HibernateException e) {
            if (e.getCause() instanceof SQLException && ((SQLException) e.getCause()).getSQLState().equals("X0Y32")) {
                System.out.println("Table already exists in database");
                sf.getCurrentSession().getTransaction().rollback();
            }
        }
    }
}
