package oopex.eclipselink1.jpa.usecases;

import java.util.Collection;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import javax.persistence.Query;
import oopex.eclipselink1.jpa.usecases.model.Group;
import oopex.eclipselink1.jpa.usecases.model.User;

public class DetachAttachWithZeroIdMain {

    private static Group detachedGroup;

    public static void main(String[] args) {
        Properties properties = new Properties();
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default", properties);
        try {
            System.out.println("*** insert and detach ***");
            insertanddetach(entityManagerFactory);
            System.out.println("*** change ***");
            changeDetachedObject();
            System.out.println("*** attach and commit ***");
            attach(entityManagerFactory);
            System.out.println("*** query ***");
            query(entityManagerFactory);
            System.out.println("*** delete ***");
            delete(entityManagerFactory);
        } finally {
            entityManagerFactory.close();
            System.out.println("*** finished ***");
        }
    }

    private static void insertanddetach(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Group group = new Group();
        try {
            entityManager.getTransaction().begin();
            User user1 = new User();
            user1.setId(0);
            user1.setName("root");
            User user2 = new User();
            user2.setId(1000);
            user2.setName("joanna");
            group.setId(100);
            group.setName("mysql");
            group.getUsers().add(user1);
            group.getUsers().add(user2);
            entityManager.persist(group);
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
        detachedGroup = group;
    }

    private static void changeDetachedObject() {
        for (User user : detachedGroup.getUsers()) {
            if (user.getId() == 0) {
                user.setDescription("the root user");
            } else {
                user.setDescription("a normal user");
            }
        }
    }

    private static void attach(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Group group = entityManager.merge(detachedGroup);
            entityManager.getTransaction().commit();
            System.out.println(group);
        } catch (OptimisticLockException e) {
            e.printStackTrace(System.out);
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
            Query query = entityManager.createQuery("SELECT g FROM Group g");
            Collection<Group> collection = (Collection<Group>) query.getResultList();
            for (Group group : collection) {
                System.out.println("found: " + group);
                for (User user : group.getUsers()) {
                    System.out.println("  with: " + user);
                }
            }
        } finally {
            entityManager.close();
        }
    }

    private static void delete(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            Query query = entityManager.createQuery("DELETE FROM Group g");
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
