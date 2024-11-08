package ru.pit.tvlist.persistence.dao;

import junit.framework.TestCase;
import ru.pit.tvlist.persistence.dao.hibernate.BroadcastHibernateDAO;
import ru.pit.tvlist.persistence.dao.hibernate.ChannelHibernateDAO;
import ru.pit.tvlist.persistence.dao.hibernate.IHibernateSessionFactory;
import ru.pit.tvlist.persistence.dao.httpclient.BroadcastHTTPClientDAO;
import ru.pit.tvlist.persistence.dao.httpclient.ChannelHTTPClientDAO;
import ru.pit.tvlist.persistence.dao.httpclient.IHTTPClientSessionFactory;
import ru.pit.tvlist.persistence.exception.PersistenceException;

public class DAOFactoryTest extends TestCase {

    private static IHibernateSessionFactory testHibernateSessionFactory = new IHibernateSessionFactory() {

        public Object getSession() throws PersistenceException {
            return null;
        }

        public void flushSession(Object session) throws PersistenceException {
        }

        public void closeSessions() throws PersistenceException {
        }
    };

    private static IHibernateSessionFactory testDefaultHibernateSessionFactory = new IHibernateSessionFactory() {

        public Object getSession() throws PersistenceException {
            return null;
        }

        public void flushSession(Object session) throws PersistenceException {
        }

        public void closeSessions() throws PersistenceException {
        }
    };

    private static IHTTPClientSessionFactory testHTTPClientSessionFactory = new IHTTPClientSessionFactory() {

        public Object getSession() throws PersistenceException {
            return null;
        }

        public void flushSession(Object session) throws PersistenceException {
        }

        public void closeSessions() throws PersistenceException {
        }
    };

    private static IHTTPClientSessionFactory testDefaultHTTPClientSessionFactory = new IHTTPClientSessionFactory() {

        public Object getSession() throws PersistenceException {
            return null;
        }

        public void flushSession(Object session) throws PersistenceException {
        }

        public void closeSessions() throws PersistenceException {
        }
    };

    public final void testGetDefaultBroadcastDAO() throws Exception {
        Object dao = DAOFactory.getBroadcastDAO(testDefaultHibernateSessionFactory);
        assertEquals(dao.getClass(), BroadcastHibernateDAO.class);
        assertEquals(((IDAO) dao).getSessionFactory(), testDefaultHibernateSessionFactory);
    }

    public final void testGetBroadcastDAO() throws Exception {
        Object dao = DAOFactory.getBroadcastDAO(testHibernateSessionFactory);
        assertEquals(dao.getClass(), BroadcastHibernateDAO.class);
        assertEquals(((IDAO) dao).getSessionFactory(), testHibernateSessionFactory);
    }

    public final void testGetDefaultChannelDAO() throws Exception {
        Object dao = DAOFactory.getChannelDAO(testDefaultHibernateSessionFactory);
        assertEquals(dao.getClass(), ChannelHibernateDAO.class);
        assertEquals(((IChannelDAO) dao).getSessionFactory(), testDefaultHibernateSessionFactory);
    }

    public final void testGetChannelDAO() throws Exception {
        Object dao = DAOFactory.getChannelDAO(testHibernateSessionFactory);
        assertEquals(dao.getClass(), ChannelHibernateDAO.class);
        assertEquals(((IChannelDAO) dao).getSessionFactory(), testHibernateSessionFactory);
    }

    public final void testGetDefaultChannelSourceDAO() throws Exception {
        Object dao = DAOFactory.getChannelSourceDAO(testDefaultHTTPClientSessionFactory);
        assertEquals(dao.getClass(), ChannelHTTPClientDAO.class);
        assertEquals(((IDAO) dao).getSessionFactory(), testDefaultHTTPClientSessionFactory);
    }

    public final void testGetChannelSourceDAO() throws Exception {
        Object dao = DAOFactory.getChannelSourceDAO(testHTTPClientSessionFactory);
        assertEquals(dao.getClass(), ChannelHTTPClientDAO.class);
        assertEquals(((IDAO) dao).getSessionFactory(), testHTTPClientSessionFactory);
    }

    public final void testGetDefaultBroadcastSourceDAO() throws Exception {
        Object dao = DAOFactory.getBroadcastSourceDAO(testDefaultHTTPClientSessionFactory);
        assertEquals(dao.getClass(), BroadcastHTTPClientDAO.class);
        assertEquals(((IDAO) dao).getSessionFactory(), testDefaultHTTPClientSessionFactory);
    }

    public final void testGetBroadcastSourceDAO() throws Exception {
        Object dao = DAOFactory.getBroadcastSourceDAO(testHTTPClientSessionFactory);
        assertEquals(dao.getClass(), BroadcastHTTPClientDAO.class);
        assertEquals(((IDAO) dao).getSessionFactory(), testHTTPClientSessionFactory);
    }

    public class TestHibernateSessionFactory implements IHibernateSessionFactory {

        public Object getSession() throws PersistenceException {
            return null;
        }

        public void flushSession(Object session) throws PersistenceException {
        }

        public void closeSessions() throws PersistenceException {
        }
    }

    public class TestHibernateDefaultSessionFactory implements IHibernateSessionFactory {

        public Object getSession() throws PersistenceException {
            return null;
        }

        public void flushSession(Object session) throws PersistenceException {
        }

        public void closeSessions() throws PersistenceException {
        }
    }

    public class TestHTTPClientSessionFactory implements IHTTPClientSessionFactory {

        public Object getSession() throws PersistenceException {
            return null;
        }

        public void flushSession(Object session) throws PersistenceException {
        }

        public void closeSessions() throws PersistenceException {
        }
    }

    public class TestHTTPClientDefaultSessionFactory implements IHTTPClientSessionFactory {

        public Object getSession() throws PersistenceException {
            return null;
        }

        public void flushSession(Object session) throws PersistenceException {
        }

        public void closeSessions() throws PersistenceException {
        }
    }
}
