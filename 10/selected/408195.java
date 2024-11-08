package oopex.openjpa2.jpa2.fields;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import oopex.openjpa2.jpa2.fields.enums.IncomeGroup;
import oopex.openjpa2.jpa2.fields.enums.State;
import oopex.openjpa2.jpa2.fields.model.Person;

public class EnumerationsMain {

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

    public static void query(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Person> query = entityManager.createQuery("SELECT p FROM Person p", Person.class);
            Collection<Person> collection = query.getResultList();
            for (Person person : collection) {
                System.out.println("found: " + person);
            }
        } finally {
            entityManager.close();
        }
    }

    public static void queryByEnum(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Person> query = entityManager.createQuery("SELECT p FROM Person p WHERE p.homeState = :homestate", Person.class);
            query.setParameter("homestate", State.CT);
            Collection<Person> collection = query.getResultList();
            System.out.println("From State.CT ...");
            for (Person person : collection) {
                System.out.println("found: " + person);
            }
            query = entityManager.createQuery("SELECT p FROM Person p WHERE p.income >= :income", Person.class);
            query.setParameter("income", IncomeGroup.average);
            collection = query.getResultList();
            System.out.println("With income of at least average ...");
            for (Person person : collection) {
                System.out.println("found: " + person);
            }
        } finally {
            entityManager.close();
        }
    }

    private static void update(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            TypedQuery<Person> query = entityManager.createQuery("SELECT p FROM Person p", Person.class);
            Collection<Person> collection = query.getResultList();
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
            entityManager.createQuery("DELETE FROM Person p").executeUpdate();
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }
}
