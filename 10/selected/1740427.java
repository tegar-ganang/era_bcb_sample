package com.budee.crm.dao.core;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Transaction;
import com.budee.crm.pojo.accesscontrol.AcRoleAction;
import com.budee.crm.pojo.accesscontrol.AcUser;

public class AcRoleDAO extends BaseHibernateDAO {

    public AcRoleAction getUserRelation(Integer roleId, Integer actionId) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + AcRoleAction.class.getName() + " where acRole.id = '" + roleId + "' and acAction.id = '" + actionId + "'";
            Query queryObject = getSession().createQuery(queryString);
            List<AcRoleAction> rtn = queryObject.list();
            tx.commit();
            return rtn.size() <= 0 ? null : rtn.get(0);
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<AcRoleAction> getAllRoleActionPagination(Integer roleId, int offset, int limit, String sort, String dir, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(AcRoleAction.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" acRole.id = '" + roleId + "' order by " + sort + " " + dir);
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

    public List<AcRoleAction> getAllRoleAction(Integer roleId, String sort, String dir) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(AcRoleAction.class.getName());
            hqlSB.append(" where ");
            hqlSB.append(" acRole.id = '" + roleId + "' order by " + sort + " " + dir);
            Query queryObject = getSession().createQuery(hqlSB.toString());
            List rtn = queryObject.list();
            tx.commit();
            return rtn;
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Integer getRoleActionCount(Integer roleId, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select count(*) from ");
            hqlSB.append(AcRoleAction.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" acRole.id = '" + roleId + "'");
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

    public void deleteRoleAction(Integer roleId, String[] actionIds) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String delStr = null;
            for (int i = 0; i < actionIds.length; i++) {
                delStr = "delete from " + AcRoleAction.class.getName() + " where acRole.id = '" + roleId + "' and acAction.id = '" + actionIds[i] + "'";
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
}
