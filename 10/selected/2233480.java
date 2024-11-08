package oopex.hibernate3.jpax.relationships;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import oopex.hibernate3.jpax.relationships.model.Address;
import oopex.hibernate3.jpax.relationships.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneToOneBidirectionalJoinColumnMain {

    private static final Logger LOGGER = LoggerFactory.getLogger("oopex.sample");

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default");
        try {
            LOGGER.info("*** insert ***");
            insert(entityManagerFactory);
            LOGGER.info("*** query ***");
            query(entityManagerFactory);
            LOGGER.info("*** update ***");
            update(entityManagerFactory);
            LOGGER.info("*** query ***");
            query(entityManagerFactory);
            LOGGER.info("*** delete ***");
            delete(entityManagerFactory);
        } finally {
            entityManagerFactory.close();
            LOGGER.info("*** finished ***");
        }
    }

    private static void insert(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Person person = new Person();
            person.setFirstName("Jesse");
            person.setLastName("James");
            Address address = new Address();
            address.setStreet("Main Road 12");
            address.setCity("Oakwood");
            person.setAddress(address);
            entityManager.persist(person);
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static void query(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Query query = entityManager.createQuery("SELECT p FROM Person p");
            Collection<Person> collection = (Collection<Person>) query.getResultList();
            for (Person person : collection) {
                LOGGER.info("found: " + person);
                LOGGER.info("  with address: " + person.getAddress());
            }
        } finally {
            entityManager.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void update(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("SELECT p FROM Person p");
            Collection<Person> collection = (Collection<Person>) query.getResultList();
            for (Person person : collection) {
                person.setFirstName("Carl");
                Address address = new Address();
                address.setCity("Austin");
                address.setStreet("Silver Avenue 21");
                person.setAddress(address);
            }
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    private static void delete(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("DELETE FROM Person p");
            query.executeUpdate();
            query = entityManager.createQuery("DELETE FROM Address a");
            query.executeUpdate();
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }
}
