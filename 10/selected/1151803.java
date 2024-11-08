package oopex.hibernate3.jpa.usecases;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Persistence;
import javax.persistence.Query;
import oopex.hibernate3.jpa.usecases.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimisticLockingNoUpdateMain {

    private static final Logger LOGGER = LoggerFactory.getLogger("oopex.sample");

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default");
        try {
            LOGGER.info("*** insert ***");
            insert(entityManagerFactory);
            LOGGER.info("*** query ***");
            query(entityManagerFactory);
            LOGGER.info("*** lock ***");
            lock(entityManagerFactory);
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
            }
        } finally {
            entityManager.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void lock(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManagerA = entityManagerFactory.createEntityManager();
        EntityManager entityManagerB = entityManagerFactory.createEntityManager();
        try {
            entityManagerA.getTransaction().begin();
            entityManagerB.getTransaction().begin();
            Query queryA = entityManagerA.createQuery("SELECT p FROM Person p");
            Collection<Person> collectionA = (Collection<Person>) queryA.getResultList();
            Person personA = collectionA.iterator().next();
            entityManagerA.lock(personA, LockModeType.READ);
            Query queryB = entityManagerB.createQuery("SELECT p FROM Person p");
            Collection<Person> collectionB = (Collection<Person>) queryB.getResultList();
            Person personB = collectionB.iterator().next();
            entityManagerB.lock(personB, LockModeType.READ);
            entityManagerA.flush();
            entityManagerB.flush();
            entityManagerA.getTransaction().commit();
            entityManagerB.getTransaction().commit();
        } finally {
            if (entityManagerA.getTransaction().isActive()) {
                entityManagerA.getTransaction().rollback();
            }
            entityManagerA.close();
            if (entityManagerB.getTransaction().isActive()) {
                entityManagerB.getTransaction().rollback();
            }
            entityManagerB.close();
        }
    }

    private static void delete(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("DELETE FROM Person p");
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
