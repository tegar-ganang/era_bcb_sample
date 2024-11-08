package mpower_hibernate;

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author Thomas Fuxreiter
 */
public class AlarmFacade {

    public Long save(Alarm s) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            Long generatedId = (Long) session.save(s);
            tx.commit();
            return generatedId;
        } catch (HibernateException e) {
            tx.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void updateStatus(long alarmId, int status) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            String hql = "update Alarm set Status = ? where AlarmId = ?";
            Query query = session.createQuery(hql);
            query.setInteger(0, status);
            query.setLong(1, alarmId);
            int rowCount = query.executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void updateStatusAccepted(long alarmId, long acceptPersonId) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            Alarm al = (Alarm) session.get(Alarm.class, alarmId);
            al.setStatus(3);
            al.setAcceptPersonId(acceptPersonId);
            al.setAcceptanceDate(new java.util.Date());
            session.update(al);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void updateStatusDeactivated(long alarmId, long deactPersonId) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            Alarm al = (Alarm) session.get(Alarm.class, alarmId);
            al.setStatus(4);
            al.setDeactPersonId(deactPersonId);
            al.setDeactivationDate(new java.util.Date());
            session.update(al);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void updateStatusTimedOut(long alarmId) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            Alarm al = (Alarm) session.get(Alarm.class, alarmId);
            al.setStatus(2);
            session.update(al);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public java.util.List selectAllAlarms() {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        List<mpower_hibernate.Alarm> alarms = session.createCriteria(mpower_hibernate.Alarm.class).list();
        return alarms;
    }

    public boolean findAlarmById(long alarmId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        mpower_hibernate.Alarm a = (mpower_hibernate.Alarm) session.get(mpower_hibernate.Alarm.class, alarmId);
        if (a != null) return true; else return false;
    }

    public mpower_hibernate.Alarm getAlarmById(long alarmId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        try {
            mpower_hibernate.Alarm a = (mpower_hibernate.Alarm) session.load(mpower_hibernate.Alarm.class, alarmId);
            return a;
        } catch (ObjectNotFoundException e) {
            throw e;
        }
    }
}
