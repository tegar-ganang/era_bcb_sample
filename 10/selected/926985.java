package com.hilaver.dzmis.dao;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Transaction;
import com.hilaver.dzmis.sys.SysMbox;
import com.hilaver.dzmis.sys.SysMboxViewer;

public class SysMboxDAO extends BaseHibernateDAO {

    public List<SysMbox> getUnViewedAllPagination(Integer userId, int offset, int limit, String sort, String order) throws Exception {
        try {
            String queryString = "from " + SysMboxViewer.class.getName() + " where acUser.id=? and isViewed is false" + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setParameter(0, userId);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List<SysMboxViewer> smvList = queryObject.list();
            List<SysMbox> smList = new ArrayList<SysMbox>();
            for (SysMboxViewer sysMboxViewer : smvList) {
                smList.add(sysMboxViewer.getSysMbox());
            }
            return smList;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void setIsViewed(Integer mboxId, Integer userId) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String updateString = "update " + SysMboxViewer.class.getName() + " set isViewed=true where sysMbox.id=? and acUser.id=?";
            Query queryObject = getSession().createQuery(updateString);
            queryObject.setParameter(0, mboxId);
            queryObject.setParameter(1, userId);
            queryObject.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<SysMboxViewer> getMyAllPagination(int offset, int limit, String sort, String order, String[] filters, Integer userId) throws Exception {
        try {
            String whereStatement = super.getWhereStatement(filters);
            if ("".equals(whereStatement)) {
                whereStatement += " where receiver.id=" + userId + " or sender.id=" + userId;
            } else {
                whereStatement += " and (receiver.id=" + userId + " or sender.id=" + userId + ")";
            }
            String queryString = "from " + SysMboxViewer.class.getName() + whereStatement + " order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Integer getMyCount(String[] filters, Integer userId) throws Exception {
        try {
            String whereStatement = super.getWhereStatement(filters);
            if ("".equals(whereStatement)) {
                whereStatement += " where receiver.id=" + userId + " or sender.id=" + userId;
            } else {
                whereStatement += " and (receiver.id=" + userId + " or sender.id=" + userId + ")";
            }
            String queryString = "select count(*) from " + SysMboxViewer.class.getName() + whereStatement;
            Query queryObject = getSession().createQuery(queryString);
            return ((Integer) queryObject.iterate().next()).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List<SysMbox> getAllUnfirmedPagination(int offset, int limit, String sort, String order, String[] filters) throws Exception {
        try {
            String whereStatement = super.getWhereStatement(filters);
            if ("".equals(whereStatement)) {
                whereStatement += " where ";
            } else {
                whereStatement += " and ";
            }
            String queryString = "from " + SysMbox.class.getName() + whereStatement + "ctOperation<>'03'   order by " + sort + " " + order;
            Query queryObject = getSession().createQuery(queryString);
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            return queryObject.list();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Integer getAllUnfirmedCount(String[] filters) throws Exception {
        try {
            String whereStatement = super.getWhereStatement(filters);
            if ("".equals(whereStatement)) {
                whereStatement += " where ";
            } else {
                whereStatement += " and ";
            }
            String queryString = "select count(*) from " + SysMbox.class.getName() + whereStatement + "ctOperation<>'03'";
            Query queryObject = getSession().createQuery(queryString);
            return ((Integer) queryObject.iterate().next()).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
