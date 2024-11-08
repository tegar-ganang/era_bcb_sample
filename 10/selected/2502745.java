package bean.person.client;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import exception.MyHibernateException;
import exception.MyObjectNotFoundException;
import exception.OSActiveException;
import exception.OldVersionException;
import bean.person.client.Client;
import bean.person.client.ClientDAO;
import hibernate.HibernateUtil;
import util.SQLErrorManager;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;

/**
 *
 * @author w4m
 */
public class ClientConector {

    private ClientDAO clientDAO;

    private final String TYPE = "cliente";

    public ClientConector() {
        this.clientDAO = new ClientDAO();
    }

    public void saveClient(Client client) throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            clientDAO.save(session, client);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex.getCause() instanceof SQLException) {
                SQLException sqle = (SQLException) ex.getCause();
                switch(sqle.getErrorCode()) {
                    case 1062:
                        throw new MyHibernateException(SQLErrorManager.extractKey(TYPE, sqle.getMessage()));
                }
            } else if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void updateClient(Client client) throws OldVersionException, MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            for (int i = 0; i < client.getPhones().size(); i++) {
                if (client.getPhones().get(i).getNumber().equals("")) {
                    SQLQuery query = session.createSQLQuery("DELETE FROM phones WHERE id = " + client.getPhones().get(i).getId());
                    query.executeUpdate();
                    client.getPhones().remove(i);
                    i--;
                }
            }
            clientDAO.update(session, client);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex.getCause() instanceof SQLException) {
                SQLException sqle = (SQLException) ex.getCause();
                switch(sqle.getErrorCode()) {
                    case 1062:
                        throw new MyHibernateException(SQLErrorManager.extractKey(TYPE, sqle.getMessage()));
                }
            } else if (ex instanceof StaleObjectStateException) {
                throw new OldVersionException(ex);
            } else if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void removeClient(int id) throws OldVersionException, MyHibernateException, MyObjectNotFoundException, OSActiveException {
        Session session = HibernateUtil.getCurrentSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            clientDAO.delete(session, id);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof StaleObjectStateException) {
                throw new OldVersionException(ex);
            } else if (ex instanceof ObjectNotFoundException) {
                throw new MyObjectNotFoundException(ex);
            } else if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Client loadClient(String name) throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Client client = null;
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            client = clientDAO.load(session, name);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        return client;
    }

    public Client loadClient(int id) throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Client client = null;
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            client = clientDAO.load(session, id);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        return client;
    }

    public Client loadCompleteClient(String name) throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Client client = null;
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            client = clientDAO.load(session, name);
            Hibernate.initialize(client.getOSSolicitations());
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        return client;
    }

    public Client loadCompleteClient(int id) throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Client client = null;
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            client = clientDAO.load(session, id);
            Hibernate.initialize(client.getOSSolicitations());
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        return client;
    }

    public List<String> loadClientsNames() throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        List<String> clientsNames = new ArrayList<String>();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            clientsNames = clientDAO.loadClientsNames(session);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        return clientsNames;
    }

    public Map<String, Client> loadClients() throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Map<String, Client> map = new HashMap<String, Client>();
        List<Client> clients = null;
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            clients = clientDAO.load(session);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        for (Client client : clients) {
            map.put(client.getName(), client);
        }
        return map;
    }

    public Map<String, Client> loadClientsByName(String name) throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Map<String, Client> map = new HashMap<String, Client>();
        List<Client> clients = null;
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            clients = clientDAO.loadClientsByName(session, name);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        for (Client client : clients) {
            map.put(client.getName(), client);
        }
        return map;
    }

    public Map<String, Client> loadClientsByAddress(String address) throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Map<String, Client> map = new HashMap<String, Client>();
        List<Client> clients = null;
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            clients = clientDAO.loadClientsByAddress(session, address);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        for (Client client : clients) {
            map.put(client.getName(), client);
        }
        return map;
    }

    public Map<String, Client> loadClientsByPhone(String phone) throws MyHibernateException {
        Session session = HibernateUtil.getCurrentSession();
        Map<String, Client> map = new HashMap<String, Client>();
        List<Client> clients = null;
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            clients = clientDAO.loadClientsByPhone(session, phone);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            if (ex instanceof HibernateException) {
                throw new MyHibernateException((HibernateException) ex);
            }
            throw ex;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        for (Client client : clients) {
            map.put(client.getName(), client);
        }
        return map;
    }
}
