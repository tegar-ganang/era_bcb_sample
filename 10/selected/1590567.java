package oopex.eclipselink2.jpa2x.usecases;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Persistence;
import javax.persistence.PessimisticLockException;
import javax.persistence.Query;
import oopex.eclipselink2.jpa2x.usecases.model.Person;

public class PessimisticLockingMain {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default");
        try {
            System.out.println("*** insert ***");
            insert(entityManagerFactory);
            System.out.println("*** query ***");
            query(entityManagerFactory);
            System.out.println("*** update ***");
            update(entityManagerFactory);
            System.out.println("*** update with lock exception ***");
            updatewithlockexception(entityManagerFactory);
            System.out.println("*** query ***");
            query(entityManagerFactory);
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
    private static void update(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("SELECT p FROM Person p");
            Collection<Person> collection = (Collection<Person>) query.getResultList();
            for (Person person : collection) {
                person.setFirstName(person.getFirstName() + "-1");
            }
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void updatewithlockexception(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager1 = entityManagerFactory.createEntityManager();
        EntityManager entityManager2 = entityManagerFactory.createEntityManager();
        try {
            entityManager1.getTransaction().begin();
            entityManager2.getTransaction().begin();
            Query query1 = entityManager1.createQuery("SELECT p FROM Person p");
            query1.setLockMode(LockModeType.PESSIMISTIC_READ);
            Query query2 = entityManager2.createQuery("SELECT p FROM Person p");
            query2.setLockMode(LockModeType.PESSIMISTIC_READ);
            Collection<Person> collection1 = (Collection<Person>) query1.getResultList();
            for (Person person : collection1) {
                person.setFirstName(person.getFirstName() + "-1");
            }
            Collection<Person> collection2 = (Collection<Person>) query2.getResultList();
            for (Person person : collection2) {
                person.setFirstName(person.getFirstName() + "-1");
            }
        } catch (PessimisticLockException e) {
        } finally {
            if (!entityManager1.getTransaction().getRollbackOnly()) {
                entityManager1.getTransaction().commit();
            }
            if (!entityManager2.getTransaction().getRollbackOnly()) {
                entityManager2.getTransaction().commit();
            }
            if (entityManager1.getTransaction().isActive()) {
                entityManager1.getTransaction().rollback();
            }
            if (entityManager2.getTransaction().isActive()) {
                entityManager2.getTransaction().rollback();
            }
            entityManager1.close();
            entityManager2.close();
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
