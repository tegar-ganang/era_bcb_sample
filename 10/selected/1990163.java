package mpower_hibernate;

import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.*;

/**
 *  
 */
public class LocationRegistryFacade {

    public void save(LocationRegistry locationRegistry) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(locationRegistry);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void delete(long locationRegistryId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            org.hibernate.Query query = session.createQuery(" delete " + " from  " + " LocationRegistry lr WHERE lr.id = ? ");
            query.setLong(0, locationRegistryId);
            query.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public mpower_hibernate.LocationRegistry findById(long locationRegistryId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" select locationRegistry " + " from  " + " LocationRegistry as locationRegistry " + "  where  " + " locationRegistry.id = ? ");
        query.setLong(0, locationRegistryId);
        return (mpower_hibernate.LocationRegistry) query.uniqueResult();
    }

    public java.util.List findByLocationRegistryId(long locationRegistryId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" select * " + " from  " + " LocationRegistry as locationRegistry " + "where locationRegistry.id = ?");
        query.setLong(0, locationRegistryId);
        return query.list();
    }

    public java.util.List findAll() {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" select locationRegistry " + " from  " + " LocationRegistry as locationRegistry ");
        return query.list();
    }

    public java.util.List findByLocationUserId(long userId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" select locationRegistry " + " from  " + " LocationRegistry as locationRegistry " + "where locationRegistry.user = ?");
        query.setLong(0, userId);
        return query.list();
    }

    public java.util.List findByDates(Date startDate, Date endDate, long user_id) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        java.lang.System.out.println("HACEMOS LA CONSULTA POR FECHAS BASÁNDONOS EN UN USUARIO...");
        org.hibernate.Query query = session.createQuery(" select locationRegistry " + " from  " + " LocationRegistry as locationRegistry " + "where locationRegistry.date_time > ? and locationRegistry.date_time < ? and locationRegistry.user = ? " + "order by locationRegistry.date_time desc");
        java.lang.System.out.println("CREADA LA QUERY");
        java.sql.Timestamp tsStart = new java.sql.Timestamp(startDate.getTime());
        java.sql.Timestamp tsEnd = new java.sql.Timestamp(endDate.getTime());
        query.setDate(0, tsStart);
        query.setDate(1, tsEnd);
        query.setLong(2, user_id);
        java.lang.System.out.println("Start: " + tsStart);
        java.lang.System.out.println("Start: " + startDate);
        java.lang.System.out.println("End:   " + tsEnd);
        java.lang.System.out.println("End:   " + endDate);
        java.lang.System.out.println("PASADOS LOS DATOS");
        return query.list();
    }
}
