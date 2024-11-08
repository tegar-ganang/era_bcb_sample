package oopex.eclipselink1.jpax.relationships;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.ReadAllQuery;
import oopex.eclipselink1.jpax.relationships.model.Person;

public class OneToManyRecursiveMain {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default");
        try {
            System.out.println("*** insert ***");
            insert(entityManagerFactory);
            System.out.println("*** query ***");
            query(entityManagerFactory);
            System.out.println("*** update ***");
            update(entityManagerFactory);
            System.out.println("*** query with fetchplan ***");
            querywithfetchplan(entityManagerFactory);
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
            Person person2 = new Person();
            person2.setFirstName("Brian");
            person2.setLastName("Cox");
            Person person3 = new Person();
            person3.setFirstName("Anne");
            person3.setLastName("Smith");
            person1.getSubordinates().add(person2);
            person2.getSubordinates().add(person3);
            entityManager.persist(person1);
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
                for (Person person1 : person.getSubordinates()) {
                    System.out.println("\twith subordinate: " + person1);
                    for (Person person2 : person1.getSubordinates()) {
                        System.out.println("\t\twith subordinate: " + person2);
                    }
                }
            }
        } finally {
            entityManager.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static void querywithfetchplan(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Query query = entityManager.createQuery("SELECT p FROM Person p");
            ReadAllQuery raq = (ReadAllQuery) ((JpaQuery) query).getDatabaseQuery();
            raq.addJoinedAttribute(raq.getExpressionBuilder().anyOfAllowingNone("subordinates"));
            raq.addJoinedAttribute(raq.getExpressionBuilder().anyOfAllowingNone("subordinates").anyOfAllowingNone("subordinates"));
            Collection<Person> collection = (Collection<Person>) query.getResultList();
            for (Person person : collection) {
                System.out.println("found: " + person);
                for (Person person1 : person.getSubordinates()) {
                    System.out.println("\twith subordinate: " + person1);
                    for (Person person2 : person1.getSubordinates()) {
                        System.out.println("\t\twith subordinate: " + person2);
                    }
                }
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
                if ("Brian".equals(person.getFirstName())) {
                    Person sub1 = new Person();
                    sub1.setFirstName("John");
                    sub1.setLastName("Stuart");
                    Person sub2 = new Person();
                    sub2.setFirstName("Nicole");
                    sub2.setLastName("Franklin");
                    person.getSubordinates().add(sub1);
                    person.getSubordinates().add(sub2);
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
