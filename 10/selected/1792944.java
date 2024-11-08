package oopex.openjpa1.jpa.fields;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import oopex.openjpa1.jpa.fields.enums.IncomeGroup;
import oopex.openjpa1.jpa.fields.enums.State;
import oopex.openjpa1.jpa.fields.model.Person;

public class EnumMain {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default");
        try {
            System.out.println("*** insert ***");
            insert(entityManagerFactory);
            System.out.println("*** query ***");
            query(entityManagerFactory);
            System.out.println("*** update ***");
            update(entityManagerFactory);
            System.out.println("*** query by enum ***");
            queryByEnum(entityManagerFactory);
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
            entityManager.persist(person1);
            entityManager.persist(person2);
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
    public static void queryByEnum(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Query query = entityManager.createQuery("SELECT p FROM Person p WHERE p.homeState = :homestate");
            query.setParameter("homestate", State.CT);
            Collection<Person> collection = (Collection<Person>) query.getResultList();
            System.out.println("From State.CT ...");
            for (Person person : collection) {
                System.out.println("found: " + person);
            }
            query = entityManager.createQuery("SELECT p FROM Person p WHERE p.income >= :income");
            query.setParameter("income", IncomeGroup.average);
            collection = (Collection<Person>) query.getResultList();
            System.out.println("With income of at least average ...");
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
                if (person.getHomeState() == State.AZ) {
                    person.setIncome(IncomeGroup.aboveaverage);
                }
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
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }
}
