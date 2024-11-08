package oopex.hibernate3.nat.usecases;

import java.util.Collection;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import oopex.hibernate3.nat.usecases.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorldFieldAccessMain {

    private static final Logger LOGGER = LoggerFactory.getLogger("oopex.sample");

    public static void main(String[] args) {
        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        try {
            LOGGER.info("*** insert ***");
            insert(sessionFactory);
            LOGGER.info("*** query ***");
            query(sessionFactory);
            LOGGER.info("*** update ***");
            update(sessionFactory);
            LOGGER.info("*** query ***");
            query(sessionFactory);
            LOGGER.info("*** delete ***");
            delete(sessionFactory);
        } finally {
            sessionFactory.close();
            LOGGER.info("*** finished ***");
        }
    }

    private static void insert(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        try {
            session.beginTransaction();
            Person person = new Person("Jesse", "James");
            session.save(person);
            session.getTransaction().commit();
        } finally {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void query(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        try {
            Query query = session.createQuery("from Person");
            Collection<Person> list = (Collection<Person>) query.list();
            for (Person person : list) {
                LOGGER.info("Found: " + person);
            }
        } finally {
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void update(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        try {
            session.beginTransaction();
            Query query = session.createQuery("from Person");
            Collection<Person> list = (Collection<Person>) query.list();
            for (Person person : list) {
                person.setzeFirstName(person.holeFirstName() + "-1");
            }
            session.getTransaction().commit();
        } finally {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            session.close();
        }
    }

    private static void delete(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        try {
            session.beginTransaction();
            Query query = session.createQuery("DELETE FROM Person");
            query.executeUpdate();
            session.getTransaction().commit();
        } finally {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            session.close();
        }
    }
}
