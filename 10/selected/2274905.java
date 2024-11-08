package mpower_hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.*;
import org.hibernate.criterion.*;
import java.util.*;

/**
 *
 * @author epetlaz
 */
public class UserScheduleFacade {

    public void save(UserSchedule us) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            session.save(us);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void update(UserSchedule us) throws HibernateException {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            String hql = "update UserSchedule set Status = ? where UserScheduleId = ?";
            Query query = session.createQuery(hql);
            query.setString(0, us.getStatus());
            query.setLong(1, us.getUserScheduleId());
            int rowCount = query.executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public java.util.List<mpower_hibernate.UserSchedule> findUserSlotsForActivity(long userId, long activityId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery("select us " + "from UserSchedule us join us.Slot s, UserSchedule usu join usu.User uuser, Slot sa join sa.Activity sactivity " + "where us.id = usu.id and sa.SlotId = s.SlotId and uuser.UserID = ? and sactivity.ActivityId = ?");
        query.setLong(0, userId);
        query.setLong(1, activityId);
        return (java.util.List<mpower_hibernate.UserSchedule>) query.list();
    }

    public java.util.List<mpower_hibernate.UserSchedule> findUsedSlots(long userId, java.util.Date starttime, java.util.Date endtime) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery("select us " + "from UserSchedule us join us.Slot s join us.User u " + "where u.UserID = ? and s.StartTime >= ? and s.StartTime < ? " + "order by s.Activity.ActivityId desc");
        query.setLong(0, userId);
        query.setTimestamp(1, starttime);
        query.setTimestamp(2, endtime);
        return (java.util.List<mpower_hibernate.UserSchedule>) query.list();
    }

    public java.util.List<mpower_hibernate.UserSchedule> findActivitySlots(long activityId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery("select us " + "from UserSchedule us join us.Slot s " + "where s.Activity.ActivityId = ? " + "order by us.User.UserID");
        query.setLong(0, activityId);
        return ((java.util.List<mpower_hibernate.UserSchedule>) query.list());
    }
}
