package com.hibernate.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import com.hibernate.jndiconfig.HibernateUtil;
import com.hibernate.mappings.Roles;
import com.hibernate.mappings.Users;

public class UserManagementDao {

    private Session session;

    public static Logger logger = Logger.getLogger(UserManagementDao.class);

    public HashMap getAllRoless() throws Exception {
        HashMap roleMap = new HashMap(2, 10);
        Roles roles = new Roles();
        Transaction tx = null;
        try {
            setUp();
        } catch (Exception e) {
            logger.error("EXCEPTION While setUP", e);
        }
        try {
            tx = this.session.beginTransaction();
            List tmpRoles = this.session.createCriteria(Roles.class).list();
            for (Iterator iter = tmpRoles.iterator(); iter.hasNext(); ) {
                roles = (Roles) iter.next();
                roleMap.put(roles.getId(), roles);
            }
            tx.commit();
            this.session.close();
        } catch (Exception e) {
            logger.error("EXCEPTION While getting employee list", e);
            this.session.getTransaction().rollback();
            this.session.close();
            throw e;
        }
        return roleMap;
    }

    public HashMap getAllUsers() throws Exception {
        HashMap userMap = new HashMap(2, 10);
        Users user = new Users();
        Transaction tx = null;
        try {
            setUp();
        } catch (Exception e) {
            logger.error("EXCEPTION While setUP", e);
        }
        try {
            tx = this.session.beginTransaction();
            List tmpUsers = this.session.createCriteria(Users.class).list();
            for (Iterator iter = tmpUsers.iterator(); iter.hasNext(); ) {
                user = (Users) iter.next();
                userMap.put(user.getId(), user);
            }
            tx.commit();
            this.session.close();
        } catch (Exception e) {
            logger.error("EXCEPTION While getting user list", e);
            this.session.getTransaction().rollback();
            this.session.close();
            throw e;
        }
        return userMap;
    }

    public HashMap getAllUserRolesMap() throws Exception {
        HashMap userrolesMap = new HashMap(2, 10);
        Users user = new Users();
        Transaction tx = null;
        try {
            setUp();
        } catch (Exception e) {
            logger.error("EXCEPTION While setUP", e);
        }
        try {
            tx = this.session.beginTransaction();
            List tmpUsers = this.session.createCriteria(Users.class).list();
            for (Iterator iter = tmpUsers.iterator(); iter.hasNext(); ) {
                user = (Users) iter.next();
                userrolesMap.put(user.getId(), giveRolebyUsername(user.getUser_name()));
            }
            tx.commit();
            this.session.close();
        } catch (Exception e) {
            logger.error("EXCEPTION While getting userrole list", e);
            this.session.getTransaction().rollback();
            this.session.close();
            throw e;
        }
        return userrolesMap;
    }

    public void removeUserbyPrimaryKey(Integer primaryKey) throws Exception {
        Users user = null;
        Session session = null;
        Transaction tx = null;
        session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            user = (Users) session.get(Users.class, primaryKey);
            if (user != null) {
                session.delete(user);
                String query = "delete from user_roles where user_name='" + user.getUser_name() + "'";
                session.createSQLQuery(query).executeUpdate();
            }
            tx.commit();
            session.close();
        } catch (Exception e) {
            logger.error("EXCEPTION While removing user", e);
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
    }

    public String giveRolebyUsername(String username) throws Exception {
        String role = null;
        Session session = null;
        Transaction tx = null;
        session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            String sql = "SELECT role_name FROM user_roles where user_name = ?";
            Query query = session.createSQLQuery(sql).setString(0, username);
            List ls = query.list();
            for (Iterator iter = ls.iterator(); iter.hasNext(); ) {
                role = (String) iter.next();
            }
            tx.commit();
            session.close();
            return role;
        } catch (Exception e) {
            logger.error("EXCEPTION While giveRolebyUsername", e);
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
    }

    public void removeRolebyPrimaryKey(Integer primaryKey) throws Exception {
        Roles role = null;
        Session session = null;
        Transaction tx = null;
        session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            role = (Roles) session.get(Roles.class, primaryKey);
            if (role != null) session.delete(role);
            tx.commit();
            session.close();
        } catch (Exception e) {
            logger.error("EXCEPTION While removing role", e);
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
    }

    public void saveRole(Roles role) throws Exception {
        Session session = null;
        Transaction tx = null;
        session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            session.save(role);
            tx.commit();
            session.close();
        } catch (Exception e) {
            logger.error("EXCEPTION While saving Role", e);
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
    }

    public void saveUser(Users user, String role) throws Exception {
        Session session = null;
        Transaction tx = null;
        session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();
            session.save(user);
            String myquery = "insert into user_roles values ('" + user.getUser_name() + "','" + role + "')";
            session.createSQLQuery(myquery).executeUpdate();
            tx.commit();
            session.close();
        } catch (Exception e) {
            logger.error("EXCEPTION While saving user", e);
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
    }

    public void updateUserForUnsuccesfulLogin(String username) throws Exception {
        Session session = null;
        Transaction tx = null;
        Users us = new Users();
        try {
            setUp();
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            List tmpUsers = this.session.createCriteria(Users.class).add(Restrictions.eq("user_name", username)).list();
            for (Iterator iter = tmpUsers.iterator(); iter.hasNext(); ) {
                us = (Users) iter.next();
                if (tmpUsers.size() == 1) {
                    us.setLast_unsuccessful_login((new Date()).toString());
                    session.update(us);
                }
                tx.commit();
                session.close();
            }
        } catch (Exception e) {
            logger.error("EXCEPTION While update user", e);
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
    }

    public int getUseridForgivenName(String username) throws Exception {
        int id = 0;
        Session session = null;
        Transaction tx = null;
        Users us = new Users();
        try {
            setUp();
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            List tmpUsers = this.session.createCriteria(Users.class).add(Restrictions.eq("user_name", username)).list();
            for (Iterator iter = tmpUsers.iterator(); iter.hasNext(); ) {
                us = (Users) iter.next();
                if (tmpUsers.size() == 1) {
                    id = us.getId().intValue();
                }
                tx.commit();
                session.close();
            }
        } catch (Exception e) {
            logger.error("EXCEPTION While update user", e);
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
        return id;
    }

    public void updateUserForSuccesfulLogin(String username) throws Exception {
        Session session = null;
        Transaction tx = null;
        Users us = new Users();
        try {
            setUp();
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            List tmpUsers = this.session.createCriteria(Users.class).add(Restrictions.eq("user_name", username)).list();
            for (Iterator iter = tmpUsers.iterator(); iter.hasNext(); ) {
                us = (Users) iter.next();
                if (tmpUsers.size() == 1) {
                    us.setLast_successful_login(((new Date()).toString()));
                    session.update(us);
                }
                tx.commit();
                session.close();
            }
        } catch (Exception e) {
            logger.error("EXCEPTION While update user", e);
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
    }

    protected void setUp() throws Exception {
        this.session = HibernateUtil.getSessionFactory().openSession();
    }

    public static void main(String args[]) {
    }
}
