package mpower_hibernate;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.*;

/**
 */
public class DeviceTypesFacade {

    public void save(DeviceTypes deviceTypes) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(deviceTypes);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void delete(long deviceTypesId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            org.hibernate.Query query = session.createQuery(" delete " + " from  " + " DeviceTypes lr WHERE lr.id = ? ");
            query.setLong(0, deviceTypesId);
            query.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public mpower_hibernate.DeviceTypes getById(long deviceTypeId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" from  " + " DeviceTypes type " + "  where type.id = ? ");
        query.setLong(0, deviceTypeId);
        return (mpower_hibernate.DeviceTypes) query.uniqueResult();
    }

    public boolean findById(long deviceTypeId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        mpower_hibernate.DeviceTypes a = (mpower_hibernate.DeviceTypes) session.get(mpower_hibernate.DeviceTypes.class, deviceTypeId);
        if (a != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean findByName(String deviceTypeName) {
        mpower_hibernate.DeviceTypes a = getByName(deviceTypeName);
        if (a != null) {
            return true;
        } else {
            return false;
        }
    }

    public List findAll() {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" from  " + " DeviceTypes as type " + "  order by  " + " type.id asc ");
        return query.list();
    }

    public mpower_hibernate.DeviceTypes getByName(String deviceTypeName) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" select type " + " from  " + " DeviceTypes as type " + "  where  " + " type.name = ? ");
        query.setString(0, deviceTypeName);
        return (mpower_hibernate.DeviceTypes) query.uniqueResult();
    }
}
