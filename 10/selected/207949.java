package oopex.openjpa2.jpa2x.relationships;

import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import oopex.openjpa2.jpa2x.relationships.model.Order;
import oopex.openjpa2.jpa2x.relationships.model.OrderItem;

public class OneToManyDependentMain {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("default");
        try {
            System.out.println("*** insert ***");
            insert(entityManagerFactory);
            System.out.println("*** query ***");
            query(entityManagerFactory);
            System.out.println("*** update ***");
            update(entityManagerFactory);
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
            Order order = new Order();
            order.setClient("Henry Ford");
            OrderItem item1 = new OrderItem();
            item1.setArticle("wheel");
            item1.setQuantity(100);
            item1.setPrice(10);
            item1.setOrder(order);
            order.getItems().add(item1);
            OrderItem item2 = new OrderItem();
            item2.setArticle("front window");
            item2.setQuantity(30);
            item2.setPrice(70);
            item2.setOrder(order);
            order.getItems().add(item2);
            entityManager.persist(order);
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
            Query query = entityManager.createQuery("SELECT o FROM Order o");
            Collection<Order> collection = (Collection<Order>) query.getResultList();
            for (Order order : collection) {
                System.out.println("found: " + order);
                for (OrderItem item : order.getItems()) {
                    System.out.println("  with item: " + item);
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
            Query query = entityManager.createQuery("SELECT o FROM Order o");
            Collection<Order> collection = (Collection<Order>) query.getResultList();
            for (Order order : collection) {
                for (OrderItem item : order.getItems()) {
                    item.setPrice(item.getPrice() * 2);
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
            Query query = entityManager.createQuery("DELETE FROM Order o");
            query.executeUpdate();
            Query query2 = entityManager.createQuery("DELETE FROM OrderItem i");
            query2.executeUpdate();
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }
}
