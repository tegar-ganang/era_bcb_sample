package mpower_hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.*;

/**
 *  @netbeans.hibernate.facade beanClass=mpower_hibernate.PackageType
 */
public class PackageTypeFacade {

    public void save(PackageType packageType) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(packageType);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void delete(long medicineid) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            org.hibernate.Query query = session.createQuery(" delete " + " from  " + " PackageType pt join PackageType.medicine med WHERE med.id = ? ");
            query.setLong(0, medicineid);
            query.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public mpower_hibernate.PackageType findById(long packageTypeId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" select packageType " + " from  " + " PackageType as packageType " + "  where  " + " packageType.id = ? ");
        query.setLong(0, packageTypeId);
        return (mpower_hibernate.PackageType) query.uniqueResult();
    }
}
