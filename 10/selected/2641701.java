package oopex.eclipselink1.jpa.usecases;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Persistence;
import javax.persistence.Query;
import oopex.eclipselink1.jpa.usecases.model.Person;

public class OptimisticLockingNoUpdateMain {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default");
        try {
            System.out.println("*** insert ***");
            insert(entityManagerFactory);
            System.out.println("*** query ***");
            query(entityManagerFactory);
            System.out.println("*** lock ***");
            lock(entityManagerFactory);
            System.out.println("*** delete ***");
            delete(entityManagerFactory);
        } finally {
            entityManagerFactory.close();
            System.out.println("*** finished ***");
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
                System.out.println("found: " + person);
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
