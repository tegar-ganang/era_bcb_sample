package com.budee.crm.dao.core;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Transaction;
import com.budee.crm.pojo.accesscontrol.AcDataCustomer;
import com.budee.crm.pojo.accesscontrol.AcDataProject;
import com.budee.crm.pojo.accesscontrol.AcUser;
import com.budee.crm.pojo.crmdo.DoProjectCustomer;
import com.budee.crm.pojo.crmdo.DoNote;
import com.budee.crm.pojo.crmdo.DoTodo;
import com.budee.crm.pojo.crmdo.DoAppointments;
import com.budee.crm.utils.Utils;

public class DoProjectDAO extends BaseHibernateDAO {

    public List getAllProjectCustomerPagination(Integer projectId, Integer userId, int offset, int limit, String sort, String order, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(DoProjectCustomer.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" doProject.id = '" + projectId + "' and");
            hqlSB.append(" doCustomer.id in (select data.id from " + AcDataCustomer.class.getName() + " where viewer.id = '" + userId + "') ");
            hqlSB.append("order by " + sort + " " + order);
            Query queryObject = getSession().createQuery(hqlSB.toString());
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public int getProjectCustomerCount(Integer projectId, Integer userId, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select count(*) from ");
            hqlSB.append(DoProjectCustomer.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" doProject.id = '" + projectId + "' and");
            hqlSB.append(" doCustomer.id in (select data.id from " + AcDataCustomer.class.getName() + " where viewer.id = '" + userId + "') ");
            Query queryObject = getSession().createQuery(hqlSB.toString());
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public DoProjectCustomer getProjectCustomer(Integer projectId, Integer customerId) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + DoProjectCustomer.class.getName() + " where doProject.id = '" + projectId + "' and doCustomer.id = '" + customerId + "'";
            Query queryObject = getSession().createQuery(queryString);
            List<DoProjectCustomer> rtn = queryObject.list();
            tx.commit();
            return rtn.size() <= 0 ? null : rtn.get(0);
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void deleteProjectCustomer(Integer projectId, String[] customerIds) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String delStr = null;
            for (int i = 0; i < customerIds.length; i++) {
                delStr = "delete from " + DoProjectCustomer.class.getName() + " where doProject.id = '" + projectId + "' and doCustomer.id = '" + customerIds[i] + "'";
                Query queryObject = getSession().createQuery(delStr);
                queryObject.executeUpdate();
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<DoNote> getAllProjectNotePagination(int projectId, int offset, int limit, String sort, String order, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(DoNote.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" doProject.id = '" + projectId + "' or (");
            hqlSB.append(getWhereClause(filters));
            hqlSB.append(" doProject.id ='" + projectId + "' and noteType='文档附件(共享)')");
            hqlSB.append(" order by " + sort + " " + order);
            Query queryObject = getSession().createQuery(hqlSB.toString());
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List<DoNote> rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Integer getAllProjectNoteCount(int projectId, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select count(*) from ");
            hqlSB.append(DoNote.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" doProject.id = '" + projectId + "'");
            Query queryObject = getSession().createQuery(hqlSB.toString());
            List<DoNote> rtn = queryObject.list();
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<DoTodo> getAllProjectTodoPagination(int projectId, int offset, int limit, String sort, String order, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(DoTodo.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" doProject.id = '" + projectId + "' order by " + sort + " " + order);
            Query queryObject = getSession().createQuery(hqlSB.toString());
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List<DoTodo> rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Integer getAllProjectTodoCount(int projectId, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select count(*) from ");
            hqlSB.append(DoTodo.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" doProject.id = '" + projectId + "'");
            Query queryObject = getSession().createQuery(hqlSB.toString());
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<DoAppointments> getAllProjectAppointmentPagination(int projectId, int offset, int limit, String sort, String order, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(DoAppointments.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" doProject.id = '" + projectId + "' order by " + sort + " " + order);
            Query queryObject = getSession().createQuery(hqlSB.toString());
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List<DoAppointments> rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Integer getAllProjectAppointmentCount(int projectId, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select count(*) from ");
            hqlSB.append(DoAppointments.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" doProject.id = '" + projectId + "'");
            Query queryObject = getSession().createQuery(hqlSB.toString());
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public AcDataProject getAcDataProject(Integer projectId, Integer userId) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + AcDataProject.class.getName() + " where data.id = '" + projectId + "' and viewer.id = '" + userId + "'";
            Query queryObject = getSession().createQuery(queryString);
            List<AcDataProject> rtn = queryObject.list();
            tx.commit();
            return rtn.size() <= 0 ? null : rtn.get(0);
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<AcDataProject> getAllAcDataProjectPagination(Integer projectId, int offset, int limit, String sort, String dir, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(AcDataProject.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" data.id = '" + projectId + "' order by " + sort + " " + dir);
            Query queryObject = getSession().createQuery(hqlSB.toString());
            queryObject.setFirstResult(offset);
            queryObject.setMaxResults(limit);
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Integer getAcDataProjectCount(Integer projectId, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select count(*) from ");
            hqlSB.append(AcDataProject.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" data.id = '" + projectId + "'");
            Query queryObject = getSession().createQuery(hqlSB.toString());
            Integer count = ((Integer) queryObject.iterate().next()).intValue();
            tx.commit();
            return count;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void deleteAcDataProject(Integer projectId, String[] userIds) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String delStr = null;
            for (int i = 0; i < userIds.length; i++) {
                if (userIds[i] == null) {
                    continue;
                }
                delStr = "delete from " + AcDataProject.class.getName() + " where data.id = '" + projectId + "' and viewer.id = '" + userIds[i] + "'";
                Query queryObject = getSession().createQuery(delStr);
                queryObject.executeUpdate();
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public AcUser getSeller(Integer projectId) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select p.viewer from " + AcDataProject.class.getName() + " p ");
            hqlSB.append(" where p.data.id = '" + projectId + "' and");
            hqlSB.append(" p.isSeller = true");
            Query queryObject = getSession().createQuery(hqlSB.toString());
            if (!queryObject.iterate().hasNext()) {
                return null;
            }
            tx.commit();
            return (AcUser) queryObject.iterate().next();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        DoProjectDAO dao = new DoProjectDAO();
        System.out.println(dao.getSeller(4));
    }
}
