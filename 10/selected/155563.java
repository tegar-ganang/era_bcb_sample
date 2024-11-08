package com.budee.crm.dao.core;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Transaction;
import com.budee.crm.pojo.accesscontrol.AcUser;
import com.budee.crm.pojo.accesscontrol.AcUserRelation;
import com.budee.crm.pojo.accesscontrol.AcUserRole;

public class AcUserDAO extends BaseHibernateDAO {

    public List<AcUser> findByEmail(String email) throws Exception {
        return this.findByProperty(AcUser.class.getName(), "email", email);
    }

    public AcUser findUser(String email, String pwd) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + AcUser.class.getName() + " where email = '" + email + "' and userPwd = '" + pwd + "'";
            Query queryObject = getSession().createQuery(queryString);
            List<AcUser> rtn = queryObject.list();
            tx.commit();
            return rtn.size() <= 0 ? null : rtn.get(0);
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public AcUserRelation getUserRelation(Integer userId, Integer bossId) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + AcUserRelation.class.getName() + " where acUserByStaffId.id = '" + userId + "' and acUserByBossId.id = '" + bossId + "'";
            Query queryObject = getSession().createQuery(queryString);
            List<AcUserRelation> rtn = queryObject.list();
            tx.commit();
            return rtn.size() <= 0 ? null : rtn.get(0);
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public AcUserRole getUserRole(Integer userId, Integer roleId) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String queryString = "from " + AcUserRole.class.getName() + " where acUser.id = '" + userId + "' and acRole.id = '" + roleId + "'";
            Query queryObject = getSession().createQuery(queryString);
            List<AcUserRole> rtn = queryObject.list();
            tx.commit();
            return rtn.size() <= 0 ? null : rtn.get(0);
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public List<AcUserRelation> getAllBossPagination(Integer userId, int offset, int limit, String sort, String dir, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(AcUserRelation.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" acUserByStaffId.id = '" + userId + "' order by " + sort + " " + dir);
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

    public List<AcUserRole> getAllUserRolePagination(Integer userId, int offset, int limit, String sort, String dir, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("from ");
            hqlSB.append(AcUserRole.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" acUser.id = '" + userId + "' order by " + sort + " " + dir);
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

    public Integer getBossCount(Integer userId, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select count(*) from ");
            hqlSB.append(AcUserRelation.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" acUserByStaffId.id = '" + userId + "'");
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

    public Integer getUserRoleCount(Integer userId, String[] filters) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            StringBuffer hqlSB = new StringBuffer();
            hqlSB.append("select count(*) from ");
            hqlSB.append(AcUserRole.class.getName());
            hqlSB.append(getWhereStatement(filters));
            if (filters != null && filters.length != 0) {
                hqlSB.append(" and ");
            } else {
                hqlSB.append(" where ");
            }
            hqlSB.append(" acUser.id = '" + userId + "'");
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

    public void deleteBoss(Integer userId, String[] bossIds) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String delStr = null;
            for (int i = 0; i < bossIds.length; i++) {
                delStr = "delete from " + AcUserRelation.class.getName() + " where acUserByStaffId.id = '" + userId + "' and acUserByBossId.id = '" + bossIds[i] + "'";
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

    public void deleteUserRole(Integer userId, String[] roleIds) throws Exception {
        Transaction tx = null;
        try {
            tx = getSession().beginTransaction();
            String delStr = null;
            for (int i = 0; i < roleIds.length; i++) {
                delStr = "delete from " + AcUserRole.class.getName() + " where acUser.id = '" + userId + "' and acRole.id = '" + roleIds[i] + "'";
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
