package oopex.hibernate3.nat.fields;

import java.util.Collection;
import java.util.Properties;
import oopex.hibernate3.nat.fields.enums.IncomeGroup;
import oopex.hibernate3.nat.fields.enums.State;
import oopex.hibernate3.nat.fields.model.Person;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import org.hibernate.type.Type;
import org.hibernate.usertype.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnumMain {

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
            LOGGER.info("*** query by enum ***");
            queryByEnum(sessionFactory);
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
            Person person1 = new Person();
            person1.setFirstName("Jesse");
            person1.setLastName("James");
            person1.setHomeState(State.CT);
            person1.setIncome(IncomeGroup.average);
            Person person2 = new Person();
            person2.setFirstName("James");
            person2.setLastName("Bloch");
            person2.setHomeState(State.AZ);
            person2.setIncome(IncomeGroup.average);
            session.save(person1);
            session.save(person2);
            session.getTransaction().commit();
        } finally {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            session.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void queryByEnum(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        try {
            Query query = session.createQuery("from Person p where p.homeState = :homestate");
            query.setParameter("homestate", State.CT, getEnumType(StringEnumType.class, State.class));
            Collection<Person> list = (Collection<Person>) query.list();
            LOGGER.info("From State.CT ...");
            for (Person person : list) {
                LOGGER.info("Found: " + person);
            }
            query = session.createQuery("from Person p WHERE p.income >= :income");
            query.setParameter("income", IncomeGroup.average, getEnumType(OrdinalEnumType.class, IncomeGroup.class));
            list = (Collection<Person>) query.list();
            LOGGER.info("With income of at least average ...");
            for (Person person : list) {
                LOGGER.info("Found: " + person);
            }
        } finally {
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
                if (person.getHomeState() == State.AZ) {
                    person.setIncome(IncomeGroup.aboveaverage);
                }
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

    private static Type getEnumType(Class<? extends UserType> enumType, Class<? extends Enum<?>> enumClass) {
        Properties typeProperties = new Properties();
        typeProperties.setProperty("class-name", enumClass.getName());
        return Hibernate.custom(enumType, typeProperties);
    }
}
