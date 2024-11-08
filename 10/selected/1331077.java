package com.org.daoImp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.catalina.User;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.org.dao.UserInterface;
import com.org.model.LeaveType;
import com.org.model.Users;
import com.org.util.HibernateUtil;

public class UserDao implements UserInterface {

    public Session session = HibernateUtil.getSessionFactory().openSession();

    public Transaction transaction = null;

    @SuppressWarnings("unchecked")
    public String loginMethod(String username, String password) {
        String sessionUser = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String query = "from com.org.model.Users u where u.username='" + username + "' and u.password='" + password + "'";
            List<Users> userList = session.createQuery(query).list();
            System.out.println("user List size" + userList.size());
            for (Iterator<Users> iterator = userList.iterator(); iterator.hasNext(); ) {
                Users usersObj = iterator.next();
                sessionUser = usersObj.getUsername();
                System.out.println("User Display Name" + usersObj.getUsername());
            }
            session.close();
        } catch (HibernateException e) {
            e.printStackTrace();
        }
        return sessionUser;
    }

    public boolean createUserMethod(String username, String password, String userDisplayName) {
        boolean flag = false;
        try {
            transaction = session.beginTransaction();
            Users userObj = new Users();
            userObj.setUsername(username);
            userObj.setPassword(password);
            userObj.setUserDisplayName(userDisplayName);
            session.save(userObj);
            transaction.commit();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Users> userRecord() {
        ArrayList<Users> arrayList = new ArrayList<Users>();
        try {
            transaction = session.beginTransaction();
            List<Users> list = session.createQuery("from Users").list();
            for (Iterator<Users> iterator = list.iterator(); iterator.hasNext(); ) {
                Users userObj = iterator.next();
                userObj.getUsername();
                userObj.getUserDisplayName();
                userObj.getUser_id();
                arrayList.add(userObj);
            }
            transaction.commit();
            session.close();
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return arrayList;
    }

    public boolean deleteUser(int userId) {
        boolean flag = false;
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Users userObj = (Users) session.get(Users.class, userId);
            session.delete(userObj);
            transaction.commit();
            session.close();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }

    public boolean changePassword(String username, String password) {
        boolean flag = false;
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            String hql = " update Users set password='" + password + "' where username='" + username + "' ";
            Query query = session.createQuery(hql);
            int rowcount = query.executeUpdate();
            transaction.commit();
            session.close();
            flag = true;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
        }
        return flag;
    }
}
