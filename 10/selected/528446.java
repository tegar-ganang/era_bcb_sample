package de.anhquan.demo.hibernate.helloworld;

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class ShoppingCart {

    private static final SessionFactory sessionFactory;

    static {
        try {
            sessionFactory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void close() {
        if (!sessionFactory.isClosed()) sessionFactory.close();
    }

    public static List<Order> getAll() {
        Session session = sessionFactory.openSession();
        List<Order> list = session.createQuery("From de.anhquan.demo.hibernate.helloworld.Order").list();
        return list;
    }

    public static Order get(Long id) {
        Session session = sessionFactory.openSession();
        return (Order) session.get(Order.class, id);
    }

    public static void deleteAll() {
        Session session = sessionFactory.openSession();
        session.getTransaction().begin();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<Order> list = session.createQuery("From de.anhquan.demo.hibernate.helloworld.Order").list();
            for (Order order : list) {
                session.delete(order);
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    System.out.println("Error rolling back transaction");
                }
                throw e;
            }
        }
    }

    public static void delete(String name) {
        Session session = sessionFactory.openSession();
        session.getTransaction().begin();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            String hql = "delete from de.anhquan.demo.hibernate.helloworld.Order where name = :name";
            Query query = session.createQuery(hql);
            query.setParameter("name", name);
            int row = query.executeUpdate();
            if (row == 0) {
                System.out.println("Doesn't deleted any row!");
            } else {
                System.out.println("Deleted Row: " + row);
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    System.out.println("Error rolling back transaction");
                }
                throw e;
            }
        }
    }

    public static void delete(Order entry) {
        Session session = sessionFactory.openSession();
        session.getTransaction().begin();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(entry);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    System.out.println("Error rolling back transaction");
                }
                throw e;
            }
        }
    }

    public static void save(Order entry) {
        Session session = sessionFactory.openSession();
        session.getTransaction().begin();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(entry);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    System.out.println("Error rolling back transaction");
                }
                throw e;
            }
        }
    }

    public static void update(Order entry) {
        Session session = sessionFactory.openSession();
        session.getTransaction().begin();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(entry);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    System.out.println("Error rolling back transaction");
                }
                throw e;
            }
        }
    }

    public static void save(List<Order> listEntries) {
        for (Order entry : listEntries) {
            save(entry);
        }
    }

    public static void update(List<Order> listEntries) {
        for (Order entry : listEntries) {
            update(entry);
        }
    }
}
