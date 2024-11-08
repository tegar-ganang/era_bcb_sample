package com.ideo.sweetdevria.proxy.hibernate;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import com.ideo.sweetdevria.proxy.exception.TechnicalException;
import com.ideo.sweetdevria.proxy.model.IRecurrenceRule;
import com.ideo.sweetdevria.proxy.model.RecurrenceRule;
import com.ideo.sweetdevria.proxy.model.TimeSlot;
import com.ideo.sweetdevria.proxy.provider.ITimeSlotProvider;

public class TimeSlotProviderHibernate implements ITimeSlotProvider {

    private static final Log LOG = LogFactory.getLog(TimeSlotProviderHibernate.class);

    public TimeSlotProviderHibernate() {
    }

    public TimeSlotProviderHibernate(String configFile) {
        HibernateUtil.getSessionFactory(configFile);
    }

    public TimeSlot create(TimeSlot obj) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            session.save(obj);
            transaction.commit();
            LOG.info("TimeSlot " + obj.getModelId() + " is saved successefully\n");
            return obj;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public List findAll() throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(TimeSlot.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            List result = criteria.list();
            transaction.commit();
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public TimeSlot findById(int id) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            TimeSlot timeSlot = (TimeSlot) session.get(TimeSlot.class, new Integer(id));
            transaction.commit();
            return timeSlot;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public List findByAgendaAndPeriod(Integer ownerId, List agendaIdList, Date startDate, Date endDate) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Criteria recurrenceCriteria = session.createCriteria(RecurrenceRule.class);
            Criterion dateRestriction = null;
            Criterion typeRestriction = Restrictions.eq("elementType", "timeSlot");
            if (startDate != null && endDate != null) {
                Criterion startDateRestriction = Restrictions.lt("dtStart", endDate);
                Criterion endDateRestriction = Restrictions.gt("dtEnd", startDate);
                dateRestriction = Restrictions.and(startDateRestriction, endDateRestriction);
            }
            recurrenceCriteria.add(Restrictions.and(dateRestriction, typeRestriction));
            List recurrenceRules = recurrenceCriteria.list();
            Criteria timeSlotCriteria = session.createCriteria(TimeSlot.class);
            Criterion idRestriction = null;
            Criterion agendaRestriction = null;
            if (recurrenceRules != null && !recurrenceRules.isEmpty()) {
                Integer[] timeSlotIds = new Integer[recurrenceRules.size()];
                for (int i = 0; i < recurrenceRules.size(); i++) {
                    timeSlotIds[i] = ((RecurrenceRule) recurrenceRules.get(i)).getElementId();
                }
                idRestriction = Restrictions.in("id", timeSlotIds);
                timeSlotCriteria.add(idRestriction);
            }
            if (agendaIdList != null && !agendaIdList.isEmpty()) {
                agendaRestriction = Restrictions.in("agendaId", agendaIdList);
                timeSlotCriteria.add(agendaRestriction);
            }
            List timeSlotBaseList = timeSlotCriteria.list();
            List result = new ArrayList();
            for (int i = 0; i < recurrenceRules.size(); i++) {
                RecurrenceRule recRule = (RecurrenceRule) recurrenceRules.get(i);
                TimeSlot timeSlotBase = findInListById(timeSlotBaseList, recRule.getElementId());
                if (timeSlotBase != null) result.addAll(recRule.getElementsByPeriod(timeSlotBase, startDate, endDate));
            }
            transaction.commit();
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public List findMaxResults(int max) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(TimeSlot.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).setMaxResults(max);
            List result = criteria.list();
            transaction.commit();
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public TimeSlot update(TimeSlot obj) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            session.saveOrUpdate(obj);
            transaction.commit();
            return obj;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public TimeSlot delete(TimeSlot obj) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            session.delete(obj);
            transaction.commit();
            return obj;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public TimeSlot delete(Integer idModel) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            String hql = "delete from RecurrenceRule where elementId = :id";
            Query query = session.createQuery(hql);
            query.setInteger("id", idModel.intValue());
            query.executeUpdate();
            hql = "delete from TimeSlot where id = :id";
            query = session.createQuery(hql);
            query.setInteger("id", idModel.intValue());
            query.executeUpdate();
            transaction.commit();
            return null;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    private TimeSlot findInListById(List timeSlotBaseList, Integer elementId) {
        for (int i = 0; i < timeSlotBaseList.size(); i++) {
            TimeSlot timeSlot = (TimeSlot) timeSlotBaseList.get(i);
            if (timeSlot.getModelId().equals(elementId)) {
                return timeSlot;
            }
        }
        return null;
    }

    public List getPermissionsByUserAndEventType(Integer timeSlotId, Integer ownerId, Integer agendaId, Integer eventTypeId) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Query query = session.createSQLQuery("select permissions from j_user_type_timeslot_agenda where userId=:userId AND timeSlotId=:timeSlotId AND eventTypeId=:eventTypeId AND agendaId=:agendaId");
            query.setParameter("userId", ownerId);
            query.setParameter("eventTypeId", eventTypeId);
            query.setParameter("timeSlotId", timeSlotId);
            query.setParameter("agendaId", agendaId);
            String timeSlotPermissions = (String) query.uniqueResult();
            transaction.commit();
            List permissions = new ArrayList();
            if (timeSlotPermissions != null) {
                permissions.add(new Integer(timeSlotPermissions.substring(0, 1)));
                permissions.add(new Integer(timeSlotPermissions.substring(1, 2)));
                permissions.add(new Integer(timeSlotPermissions.substring(2, 3)));
            } else {
                permissions.add(TimeSlot.AUTHORIZED);
                permissions.add(TimeSlot.AUTHORIZED);
                permissions.add(TimeSlot.AUTHORIZED);
            }
            return permissions;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public Map getAllPermissionsByUserAndEventType(List timeSlotIdList, Integer ownerId, Integer agendaId, Integer eventTypeId) throws TechnicalException {
        Map result = new HashMap();
        if (timeSlotIdList.isEmpty()) return result;
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            String timeSlotIdInclusion = "AND timeSlotId in " + listToSQLinclusion(timeSlotIdList);
            Query query = session.createSQLQuery("select timeSlotId,permissions from j_user_type_timeslot_agenda where userId=:userId " + timeSlotIdInclusion + " AND eventTypeId=:eventTypeId AND agendaId=:agendaId");
            query.setParameter("userId", ownerId);
            query.setParameter("eventTypeId", eventTypeId);
            query.setParameter("agendaId", agendaId);
            List timeSlotPermissionsList = (List) query.list();
            transaction.commit();
            if (timeSlotPermissionsList != null) {
                for (int i = 0; i < timeSlotPermissionsList.size(); i++) {
                    Object[] timeSlotPermissionsArray = (Object[]) timeSlotPermissionsList.get(i);
                    Integer timeSlotId = (Integer) timeSlotPermissionsArray[0];
                    String timeSlotPermissions = (String) timeSlotPermissionsArray[1];
                    List permissions = new ArrayList();
                    permissions.add(new Integer(timeSlotPermissions.substring(0, 1)));
                    permissions.add(new Integer(timeSlotPermissions.substring(1, 2)));
                    permissions.add(new Integer(timeSlotPermissions.substring(2, 3)));
                    result.put(timeSlotId, permissions);
                    timeSlotIdList.remove(timeSlotId);
                }
            }
            for (int i = 0; i < timeSlotIdList.size(); i++) {
                List permissions = new ArrayList();
                Integer timeSlotId = (Integer) timeSlotIdList.get(i);
                permissions.add(TimeSlot.AUTHORIZED);
                permissions.add(TimeSlot.AUTHORIZED);
                permissions.add(TimeSlot.AUTHORIZED);
                result.put(timeSlotId, permissions);
            }
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    /**
	 * Add permissions for a given user,agenda and type of event in the database, this method is not mandatory
	 * because the permissions administration can be external
	 * @return the number of rows updated
	 */
    public int addPermissionsByUserAndEventType(Integer timeSlotId, Integer ownerId, Integer agendaId, Integer eventTypeId, List permissions) throws TechnicalException {
        return 1;
    }

    /**
	 * Methode creat 
	 * Create a TimeSlot Model. A TimeSlot Model describe instances of Timeslots
	 * And Recurrence Rules
	 * @param obj TimeSlot Model
	 * @param frequencies list of frequencies
	 * @param occurences list of occurences
	 * @param masks list of masks
	 * @param startDate list of start dates
	 * @param endDate list of end Dates
	 * @param elementsType list of elementsType
	 */
    public TimeSlot create(TimeSlot obj, List recurrenceRules) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            obj = this.create(obj);
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            for (int i = 0; i < recurrenceRules.size(); i++) {
                RecurrenceRule rr = (RecurrenceRule) recurrenceRules.get(i);
                rr.setElementId(obj.getModelId());
                session.save(rr);
            }
            transaction.commit();
            LOG.info("TimeSlot " + obj.getModelId() + " is saved successefully\n");
            return obj;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    private Date stringToDate(String sDate, String sFormat) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(sFormat);
        return sdf.parse(sDate);
    }

    private String listToSQLinclusion(List list) {
        String result = "(";
        for (int i = 0; i < list.size(); i++) {
            result += list.get(i).toString();
            if (i + 1 < list.size()) result += ",";
        }
        result += ")";
        return result;
    }

    public List findAllTimeSlotModelsByUserAndAgenda(Integer ownerId, List agendaIdList) throws TechnicalException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getCurrentSession();
            transaction = session.beginTransaction();
            Criteria recurrenceCriteria = session.createCriteria(RecurrenceRule.class);
            Criterion typeRestriction = Restrictions.eq("elementType", "timeSlot");
            recurrenceCriteria.add(typeRestriction);
            List recurrenceRules = recurrenceCriteria.list();
            Criteria timeSlotCriteria = session.createCriteria(TimeSlot.class);
            Criterion idRestriction = null;
            Criterion agendaRestriction = null;
            Criterion ownerRestriction = Restrictions.eq("ownerId", ownerId);
            System.out.println(recurrenceRules.size());
            if (recurrenceRules != null && !recurrenceRules.isEmpty()) {
                Integer[] timeSlotIds = new Integer[recurrenceRules.size()];
                for (int i = 0; i < recurrenceRules.size(); i++) {
                    timeSlotIds[i] = ((RecurrenceRule) recurrenceRules.get(i)).getElementId();
                }
                idRestriction = Restrictions.in("id", timeSlotIds);
                timeSlotCriteria.add(idRestriction);
            }
            if (agendaIdList != null && !agendaIdList.isEmpty()) {
                agendaRestriction = Restrictions.in("agendaId", agendaIdList);
                timeSlotCriteria.add(agendaRestriction);
            }
            List timeSlotBaseList = timeSlotCriteria.list();
            List result = new ArrayList();
            for (int i = 0; i < recurrenceRules.size(); i++) {
                RecurrenceRule recRule = (RecurrenceRule) recurrenceRules.get(i);
                TimeSlot timeSlotBase = findInListById(timeSlotBaseList, recRule.getElementId());
                if (timeSlotBase != null) {
                    timeSlotBase.setStartDate(recRule.getDtStart());
                    timeSlotBase.setEndDate(recRule.getDtEnd());
                    result.add(timeSlotBase);
                }
            }
            transaction.commit();
            System.out.println(result.size());
            return result;
        } catch (HibernateException ex) {
            if (transaction != null) transaction.rollback();
            throw new TechnicalException(ex);
        }
    }

    public IRecurrenceRule createRecurrenceRule(Date dtStart, Date dtEnd, Map m) throws TechnicalException {
        try {
            RecurrenceRule recurrenceRule = new RecurrenceRule((Integer) m.get("elementId"), dtStart, dtEnd, (String) m.get("mask"), new Time(stringToDate((String) m.get("startTime"), "yyyy-MM-dd'T'HH:mm'Z'").getTime()), new Time(stringToDate((String) m.get("endTime"), "yyyy-MM-dd'T'HH:mm'Z'").getTime()), (String) m.get("frequency"), (String) m.get("occurences"), (String) m.get("elementType"));
            return recurrenceRule;
        } catch (Exception ex) {
            System.out.println("Exception " + ex);
            return null;
        }
    }
}
