package mpower_hibernate;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.*;

/**
 *  @netbeans.hibernate.facade beanClass=mpower_hibernate.LocationRegistry
 */
public class ProtocolsFacade {

    public void save(Protocols protocols) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(protocols);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public void delete(long protocolsId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        java.lang.System.out.println("protocolsId::" + protocolsId);
        try {
            org.hibernate.Query query = session.createQuery(" delete " + " from  " + " Protocols lr WHERE lr.id = ? ");
            query.setLong(0, protocolsId);
            query.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            transaction.rollback();
            throw e;
        } finally {
            mpower_hibernate.HibernateUtil.closeSession();
        }
    }

    public mpower_hibernate.Protocols getById(long protocolId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery("select protocol" + " from  " + " Protocols  protocol " + "  where  " + " protocol.id = ? ");
        query.setLong(0, protocolId);
        return (mpower_hibernate.Protocols) query.uniqueResult();
    }

    public boolean findById(long protocolId) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        mpower_hibernate.Protocols a = (mpower_hibernate.Protocols) session.get(mpower_hibernate.Protocols.class, protocolId);
        if (a != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean findByName(String protocolName) {
        Protocols p = getByName(protocolName);
        if (p != null) {
            return true;
        } else {
            return false;
        }
    }

    public mpower_hibernate.Protocols getByName(String name) {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery("select protocol" + " from  " + " Protocols  protocol WHERE protocol.name = ?");
        query.setString(0, name);
        return (mpower_hibernate.Protocols) query.uniqueResult();
    }

    public List findAll() {
        Session session = mpower_hibernate.HibernateUtil.currentSession();
        org.hibernate.Query query = session.createQuery(" from  " + " Protocols  protocol " + "  order by  " + " protocol.id asc ");
        return query.list();
    }
}
