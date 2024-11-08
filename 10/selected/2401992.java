package mpower_hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.*;

/**
 *  @netbeans.hibernate.facade beanClass=mpower_hibernate.MedicineList
 */
public class MedicineListFacade {

    public void save(MedicineList medicineList) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(medicineList);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public java.util.List findByPatient(long patientId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" select ml " + " from  " + " MedicineList ml join ml.userPatient p where p.UserID = ? ");
        query.setLong(0, patientId);
        return query.list();
    }

    public java.util.List findByPatientMedicine(long patientId, String medicineCode) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" select mlp " + " from  " + " MedicineList mlp, MedicineList mlm " + "where mlp.id = mlm.id and mlp.userPatient.UserID = ? and mlm.medicine.code = ?");
        query.setLong(0, patientId);
        query.setParameter(1, medicineCode);
        return query.list();
    }

    public void delete(MedicineList ml) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            org.hibernate.Query query = session.createQuery(" delete " + " from  " + " MedicineList ml WHERE ml.id = ? ");
            query.setLong(0, ml.getId());
            query.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void update(MedicineList medicineList) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.update(medicineList);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }
}
