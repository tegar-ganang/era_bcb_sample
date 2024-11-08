package oopex.eclipselink1.jpa.relationships;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import oopex.eclipselink1.jpa.relationships.model.Person;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.ReadAllQuery;

public class ManyToManyRecursiveMain {

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
            person2.setLastName("Fowley");
            Person person3 = new Person();
            person3.setFirstName("Anne");
            person3.setLastName("Smith");
            person1.getFriends().add(person2);
            person1.getFriends().add(person3);
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
                for (Person person1 : person.getFriends()) {
                    System.out.println("  with friend: " + person1);
                    for (Person person2 : person1.getFriends()) {
                        System.out.println("  with friend: " + person2);
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
            raq.addJoinedAttribute(raq.getExpressionBuilder().anyOfAllowingNone("friends"));
            raq.addJoinedAttribute(raq.getExpressionBuilder().anyOfAllowingNone("friends").anyOfAllowingNone("friends"));
            Collection<Person> collection = (Collection<Person>) query.getResultList();
            for (Person person : collection) {
                System.out.println("found: " + person);
                for (Person person1 : person.getFriends()) {
                    System.out.println("\twith friend: " + person1);
                    for (Person person2 : person1.getFriends()) {
                        System.out.println("\t\twith friend: " + person2);
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
            Person jesse = null;
            Person anne = null;
            for (Person person : collection) {
                if ("Jesse".equals(person.getFirstName())) {
                    jesse = person;
                } else if ("Anne".equals(person.getFirstName())) {
                    anne = person;
                }
            }
            anne.getFriends().add(jesse);
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
