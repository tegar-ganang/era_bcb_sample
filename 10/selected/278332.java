package oopex.eclipselink1.jpa.usecases;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import oopex.eclipselink1.jpa.usecases.model.Person;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaHelper;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.sessions.Session;

public class InMemoryQueryMain {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default");
        try {
            System.out.println("*** insert ***");
            insert(entityManagerFactory);
            System.out.println("*** query ***");
            query(entityManagerFactory);
            System.out.println("*** update and query with jpa ***");
            updateandquerywithjpa(entityManagerFactory);
            System.out.println("*** update and query with native criteria api ***");
            updateandquerywithcriteria(entityManagerFactory);
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
    private static void updateandquerywithjpa(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("SELECT p FROM Person p");
            Collection<Person> collection = (Collection<Person>) query.getResultList();
            for (Person person : collection) {
                person.setFirstName(person.getFirstName() + "-1");
            }
            Query inMemoryQuery = entityManager.createQuery("SELECT p FROM Person p WHERE p.firstName LIKE :name");
            inMemoryQuery.setParameter("name", "%-1");
            Collection<Person> imqCollection = (Collection<Person>) inMemoryQuery.getResultList();
            for (Person person : imqCollection) {
                System.out.println("found: " + person);
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
    private static void updateandquerywithcriteria(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        Session session = JpaHelper.getEntityManager(entityManager).getActiveSession();
        try {
            ReadAllQuery query = new ReadAllQuery();
            query.setReferenceClass(Person.class);
            Collection<Person> collection = (Collection<Person>) session.executeQuery(query);
            for (Person person : collection) {
                person.setFirstName(person.getFirstName() + "-2");
            }
            ReadAllQuery inMemoryQuery = new ReadAllQuery();
            inMemoryQuery.setReferenceClass(Person.class);
            ExpressionBuilder personE = inMemoryQuery.getExpressionBuilder();
            Expression expression = personE.get("firstName").like("%-2");
            inMemoryQuery.setSelectionCriteria(expression);
            inMemoryQuery.conformResultsInUnitOfWork();
            Collection<Person> imqCollection = (Collection<Person>) session.executeQuery(inMemoryQuery);
            for (Person person : imqCollection) {
                System.out.println("found: " + person);
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
