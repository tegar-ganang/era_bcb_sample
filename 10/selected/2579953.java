package com.org.daoImp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.org.dao.WeeklyHolidayInterface;
import com.org.model.Holidays;
import com.org.model.WeeklyHolidays;
import com.org.util.HibernateUtil;
import com.org.util.Miscellaneous;

public class WeeklyHolidayDao implements WeeklyHolidayInterface {

    public Session session = HibernateUtil.getSessionFactory().openSession();

    public Transaction transaction = null;

    public boolean weeklyHoidayRegistered(String dayName, String flag) {
        boolean check = false;
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            System.out.println("yes herer holidays");
            String hql = "update WeeklyHolidays as w set w.flag='" + flag + "' where w.dayName='" + dayName + "' ";
            Query query = session.createQuery(hql);
            System.out.println("yes herer holidays down");
            WeeklyHolidays weekObj = new WeeklyHolidays();
            weekObj.setFlag(flag);
            int rowcount = query.executeUpdate();
            transaction.commit();
            session.close();
            check = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return check;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<WeeklyHolidays> fetchHoliday() {
        ArrayList<WeeklyHolidays> arrayList = new ArrayList<WeeklyHolidays>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            List<WeeklyHolidays> list = session.createQuery("from WeeklyHolidays").list();
            for (Iterator<WeeklyHolidays> iterator = list.iterator(); iterator.hasNext(); ) {
                WeeklyHolidays weekObj = iterator.next();
                weekObj.getFlag();
                weekObj.getDayName();
                arrayList.add(weekObj);
            }
            transaction.commit();
            session.close();
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return arrayList;
    }

    public boolean addHolidays(String date, String vactionTitle) {
        boolean flag = false;
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Holidays hObj = new Holidays();
            hObj.setDate(Miscellaneous.sqlDateFormate(date));
            hObj.setVactionTitle(vactionTitle);
            session.save(hObj);
            transaction.commit();
            flag = true;
            session.close();
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Holidays> fetchGHoliday() {
        ArrayList<Holidays> arrayList = new ArrayList<Holidays>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            List<Holidays> list = session.createQuery("from Holidays").list();
            for (Iterator<Holidays> iterator = list.iterator(); iterator.hasNext(); ) {
                Holidays hObj = iterator.next();
                hObj.getId();
                hObj.getDate();
                hObj.getVactionTitle();
                arrayList.add(hObj);
            }
            transaction.commit();
            session.close();
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return arrayList;
    }

    public boolean deleteHoliday(int vactionId) {
        boolean flag = false;
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Holidays holiday = (Holidays) session.get(Holidays.class, vactionId);
            session.delete(holiday);
            transaction.commit();
            flag = true;
            session.close();
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }
}
