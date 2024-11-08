package gov.esporing.ost.storage.hibernate;

import gov.esporing.ost.model.tcxml.AbstractDataModel;
import gov.esporing.ost.model.tcxml.TraceCore;
import gov.esporing.ost.storage.IStorage;
import gov.esporing.ost.users.Party;
import gov.esporing.ost.users.User;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StorageImpl implements IStorage {

    private static StorageImpl instance;

    private StorageImpl() {
    }

    public static StorageImpl getInstance() {
        if (instance == null) instance = new StorageImpl();
        return instance;
    }

    public boolean checkConnection() {
        return HibernateUtil.getSessionFactory().getCurrentSession().isOpen();
    }

    public AbstractDataModel getMessage(int id, Class c) {
        Transaction tx = null;
        AbstractDataModel model = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            model = (AbstractDataModel) session.createQuery("SELECT m FROM " + c.getName() + " as m WHERE id=" + id).uniqueResult();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
        return model;
    }

    public List<AbstractDataModel> getMessages(String deliveryID, String despatchID, String partyID, boolean reciveved, boolean sent, Class c) {
        if (deliveryID == null || deliveryID.equals("")) deliveryID = "%";
        if (despatchID == null || despatchID.equals("")) despatchID = "%";
        String sql = "SELECT m FROM " + c.getName() + " as m ";
        if (reciveved && sent) sql += "WHERE (m.deliveryPartyId like '" + deliveryID + "' and m.despatchPartyId like '" + partyID + "') or (m.despatchPartyId like '" + despatchID + "' and m.deliveryPartyId like '" + partyID + "')"; else if (reciveved) sql += "WHERE (m.despatchPartyId like '" + despatchID + "' and m.deliveryPartyId like '" + partyID + "')"; else if (sent) sql += "WHERE (m.deliveryPartyId like '" + deliveryID + "' and m.despatchPartyId like '" + partyID + "')"; else sql += "WHERE (m.deliveryPartyId like '" + partyID + "' and m.despatchPartyId like '" + partyID + "') or (m.despatchPartyId like '" + partyID + "' and m.deliveryPartyId like '" + partyID + "')";
        System.out.println("Searching following: " + sql);
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        List<AbstractDataModel> l = null;
        try {
            tx = session.beginTransaction();
            l = session.createQuery(sql).list();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
        return l;
    }

    /**
	 * Returns a list of AbstractDataModels
	 * 
	 * Used by the SearchController in order to filter out
	 * data
	 * 
	 * @param deliveryID
	 * @param despatchID
	 * @param recieved
	 * @param sent
	 * @param partyID
	 * @return
	 */
    public List<AbstractDataModel> getMessages(String deliveryID, String despatchID, boolean recieved, boolean sent, String partyID) {
        return getMessages(deliveryID, despatchID, partyID, recieved, sent, TraceCore.class);
    }

    public List<AbstractDataModel> getAllMessages(Class c) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        List<AbstractDataModel> l = null;
        try {
            tx = session.beginTransaction();
            l = session.createQuery("SELECT m FROM " + c.getName() + " as m").list();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
        return l;
    }

    public AbstractDataModel getMessage(int id) {
        return getMessage(id, TraceCore.class);
    }

    /**
	 * Returns every message stored in the database
	 * 
	 * @return
	 */
    public List<AbstractDataModel> getAllMessages() {
        return getAllMessages(TraceCore.class);
    }

    public void store(AbstractDataModel model) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            session.save(model);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    public void update(AbstractDataModel model) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            session.update(model);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    public void deleteAllMessages() {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            session.createSQLQuery("DELETE FROM ost_messages").executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    public User getUser(String name, String password, Class c) {
        Transaction tx = null;
        User user = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            user = (User) session.createQuery("SELECT m FROM " + c.getName() + " as m WHERE name like '" + name + "' AND password like '" + password + "'").uniqueResult();
            tx.commit();
            System.out.println("OST: " + user.getPassword());
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
        return user;
    }

    /**
	 * Calls the database to return a specific user, used in the log in
	 * process.
	 */
    public User getUser(String name, String password) {
        return getUser(name, password, User.class);
    }

    /**
	 * Returns all user of a certain party
	 * 
	 * @param party_id
	 * @return
	 */
    public List<User> getUsers(String party_id) {
        return getUsers(party_id, User.class);
    }

    private List<User> getUsers(String party_id, Class c) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        List<User> l = null;
        try {
            tx = session.beginTransaction();
            l = session.createQuery("SELECT m FROM " + c.getName() + " as m where party_id like '" + party_id + "'").list();
            System.out.println("Returned " + l.size() + " users");
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
        return l;
    }

    /**
	 * Return the given party
	 * 
	 * @param party
	 * @return
	 */
    public Party getParty(String party) {
        return getUser(party, Party.class);
    }

    private Party getUser(String id, Class c) {
        Transaction tx = null;
        Party party = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            System.out.println("OST: Query to database some action");
            party = (Party) session.createQuery("SELECT m FROM " + c.getName() + " as m WHERE party_id like '" + id + "'").uniqueResult();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
        return party;
    }

    /**
	 * Inserts a new user into the database
	 * @param user
	 */
    public void saveUser(User user) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            session.save(user);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    /**
	 * Updates an existing user in the database
	 */
    public void updateUser(User user) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            session.update(user);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    /**
	 * Updates an existing party in the database
	 * @param party
	 */
    public void updateParty(Party party) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            session.update(party);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    /**
	 * Deletes the specified user from the database
	 * @param user
	 */
    public void deleteUser(User user) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();
            session.delete(user);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }
}
