package com.org.daoImp;

import java.util.ArrayList;
import java.util.Iterator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import javax.persistence.Query;
import com.org.beans.LeaveRecord;
import com.org.dao.LeaveInterface;
import com.org.model.LeaveDetail;
import com.org.model.LeaveQuota;
import com.org.model.LeaveType;
import com.org.model.ShortLeaveDetail;
import com.org.util.HibernateUtil;
import com.org.util.Miscellaneous;

public class LeaveTypeDemo implements LeaveInterface {

    public Session session = HibernateUtil.getSessionFactory().openSession();

    public Transaction transaction = null;

    @SuppressWarnings("unchecked")
    public ArrayList<LeaveType> fetchLeaveType() {
        ArrayList<LeaveType> leaveList = new ArrayList<LeaveType>();
        try {
            transaction = session.beginTransaction();
            List<LeaveType> list = session.createQuery("from LeaveType").list();
            for (Iterator<LeaveType> iterator = list.iterator(); iterator.hasNext(); ) {
                LeaveType leaveObj = iterator.next();
                leaveObj.getLeaveType();
                leaveList.add(leaveObj);
            }
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return leaveList;
    }

    public boolean addEmpLeaveDetails(int empId, String leaveFrom, String leaveTo, String type, String reason, String authorizedBy, String authorizedDate) {
        System.out.println("leave Type=" + type + "empId=" + empId);
        boolean flag = false;
        try {
            transaction = session.beginTransaction();
            LeaveDetail leaveObj = new LeaveDetail();
            leaveObj.setEmployeeCode(empId);
            leaveObj.setLeaveFrom(Miscellaneous.sqlDateFormate(leaveFrom));
            leaveObj.setLeaveTo(Miscellaneous.sqlDateFormate(leaveTo));
            leaveObj.setType(type);
            leaveObj.setLeaveReasons(reason);
            leaveObj.setAuthorizedBy(authorizedBy);
            leaveObj.setAuthorizedDate(Miscellaneous.sqlDateFormate(authorizedDate));
            session.save(leaveObj);
            transaction.commit();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    public boolean addShortLeaveDetails(int empId, String timeFrom, String timeTo, String type, String reason, String authorizedBy, String authorizedDate) {
        boolean flag = false;
        System.out.println("sql date formate " + Miscellaneous.sqlDateFormate(authorizedDate));
        try {
            transaction = session.beginTransaction();
            ShortLeaveDetail sobj = new ShortLeaveDetail();
            sobj.setEmployeeCode(empId);
            sobj.setTimeForm(timeFrom);
            sobj.setTimeTo(timeTo);
            sobj.setType(type);
            sobj.setLeaveReason(reason);
            sobj.setAuthorizedBy(authorizedBy);
            sobj.setAuthorizedDate(Miscellaneous.sqlDateFormate(authorizedDate));
            session.save(sobj);
            transaction.commit();
            session.close();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    public boolean addLeaveCategories(String categoriesName) {
        boolean flag = false;
        try {
            transaction = session.beginTransaction();
            LeaveType obj = new LeaveType();
            obj.setLeaveType(categoriesName);
            session.save(obj);
            transaction.commit();
            session.close();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    public boolean deleteCategories(int leaveId) {
        boolean flag = false;
        try {
            transaction = session.beginTransaction();
            LeaveType leaveType = (LeaveType) session.get(LeaveType.class, leaveId);
            session.delete(leaveType);
            transaction.commit();
            session.close();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    public boolean uddateCategories(int leaveId, String leavevalue) {
        boolean flag = false;
        try {
            transaction = session.beginTransaction();
            LeaveType leaveObj = (LeaveType) session.get(LeaveType.class, leaveId);
            leaveObj.setLeaveType(leavevalue);
            transaction.commit();
            session.close();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    public boolean saveLeaveQty(String arrayValues) {
        boolean flag = false;
        String empId = "";
        String lvType = "";
        String[] splitData;
        splitData = arrayValues.split(",");
        System.out.println("arrays values" + arrayValues);
        try {
            for (int i = 0; i < splitData.length; i++) {
                String[] finalValue;
                finalValue = splitData[i].split("/");
                String query = "from LeaveQuota a where a.employeeCode='" + finalValue[0].trim() + "' and a.leaveTyp='" + finalValue[1].trim() + "' ";
                transaction = session.beginTransaction();
                List<LeaveQuota> checkList = session.createQuery(query).list();
                for (Iterator<LeaveQuota> itr = checkList.iterator(); itr.hasNext(); ) {
                    LeaveQuota chekLQ = itr.next();
                    empId = chekLQ.getEmployeeCode();
                    lvType = chekLQ.getLeaveTyp();
                }
                System.out.println(empId + " yes here =" + lvType);
                transaction.commit();
                if (empId.equals(finalValue[0].trim()) && lvType.equals(finalValue[1].trim())) {
                    transaction = session.beginTransaction();
                    String updateQry = " update LeaveQuota set leaveQty='" + finalValue[2].trim() + "' where employeeCode='" + finalValue[0].trim() + "' and leaveTyp='" + finalValue[1].trim() + "' ";
                    org.hibernate.Query qry = session.createQuery(updateQry);
                    int rowcount = qry.executeUpdate();
                    transaction.commit();
                } else {
                    transaction = session.beginTransaction();
                    LeaveQuota lqObj = new LeaveQuota();
                    int qty = Integer.parseInt(finalValue[2].trim());
                    lqObj.setEmployeeCode(finalValue[0].trim());
                    lqObj.setLeaveTyp(finalValue[1].trim());
                    lqObj.setLeaveQty(qty);
                    session.save(lqObj);
                    transaction.commit();
                }
            }
            session.close();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<LeaveQuota> fectLeaveQuota(int empId) {
        ArrayList<LeaveQuota> qList = new ArrayList<LeaveQuota>();
        try {
            transaction = session.beginTransaction();
            List<LeaveQuota> list = session.createQuery("from LeaveQuota where employeeCode ='" + empId + "' ").list();
            for (Iterator<LeaveQuota> itr = list.iterator(); itr.hasNext(); ) {
                LeaveQuota qobj = itr.next();
                qobj.getLeaveTyp();
                qobj.getLeaveQty();
                qList.add(qobj);
            }
            transaction.commit();
            session.close();
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return qList;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ShortLeaveDetail> shortLeaveDetails(int empId, String month, String year) {
        ArrayList<ShortLeaveDetail> Slsit = new ArrayList<ShortLeaveDetail>();
        int employyeCode = empId;
        String startDate = "01-" + month.trim() + "-" + year.trim();
        String endDate = "31-" + month.trim() + "-" + year.trim();
        System.out.println(startDate + " date" + endDate);
        String query = " from ShortLeaveDetail as s where s.employeeCode='" + employyeCode + "' and (s.authorizedDate>='" + Miscellaneous.sqlDateFormate(startDate) + "' and  s.authorizedDate <='" + Miscellaneous.sqlDateFormate(endDate) + "') ";
        try {
            transaction = session.beginTransaction();
            List<ShortLeaveDetail> cList = session.createQuery(query).list();
            for (Iterator<ShortLeaveDetail> iterator = cList.iterator(); iterator.hasNext(); ) {
                ShortLeaveDetail obj = iterator.next();
                obj.getAuthorizedDate();
                obj.getTimeForm();
                obj.getTimeTo();
                obj.getAuthorizedBy();
                obj.getLeaveReason();
                Slsit.add(obj);
            }
            transaction.commit();
            session.close();
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
        }
        System.out.println("list size" + Slsit.size());
        return Slsit;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<LeaveDetail> leaveDetails(int empId, String leaveFrom, String leaveTo) {
        ArrayList<LeaveDetail> arrayList = new ArrayList<LeaveDetail>();
        String hql = " from LeaveDetail as l where l.employeeCode='" + empId + "' and (l.leaveFrom >='" + Miscellaneous.sqlDateFormate(leaveFrom) + "' and l.leaveTo<='" + Miscellaneous.sqlDateFormate(leaveTo) + "') ";
        try {
            transaction = session.beginTransaction();
            List<LeaveDetail> list = session.createQuery(hql).list();
            for (Iterator<LeaveDetail> iterator = list.iterator(); iterator.hasNext(); ) {
                LeaveDetail obj = iterator.next();
                obj.getLeaveFrom();
                obj.getLeaveTo();
                obj.getType();
                obj.getAuthorizedBy();
                obj.getAuthorizedDate();
                obj.getLeaveReasons();
                arrayList.add(obj);
            }
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return arrayList;
    }
}
