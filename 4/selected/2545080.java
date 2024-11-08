package ru.pit.tvlist.persistence.dao;

import ru.pit.tvlist.persistence.dao.hibernate.BroadcastHibernateDAO;
import ru.pit.tvlist.persistence.dao.hibernate.ChannelHibernateDAO;
import ru.pit.tvlist.persistence.dao.hibernate.IHibernateSessionFactory;
import ru.pit.tvlist.persistence.dao.httpclient.BroadcastHTTPClientDAO;
import ru.pit.tvlist.persistence.dao.httpclient.ChannelHTTPClientDAO;
import ru.pit.tvlist.persistence.dao.httpclient.IHTTPClientSessionFactory;

/**
 * Factory class for all DAOs 
 */
public class DAOFactory {

    private static IChannelDAO channelDAO = null;

    private static IBroadcastDAO broadcastDAO = null;

    private static IChannelSourceDAO channelSourceDAO = null;

    private static IBroadcastSourceDAO broadcastSourceDAO = null;

    /**
     * Default Hibernate session factory
     */
    private static IHibernateSessionFactory defaultHibernateSessionFactory = null;

    /**
     * Default HTTPClient session factory
     */
    private static IHTTPClientSessionFactory defaultHTTPClientSessionFactory = null;

    /**
     * Sets default HibernateSessionFactory
     * @param hibernateSessionFactory Hibernate session factory
     */
    public static void setDefaultHibernateSessionFactory(IHibernateSessionFactory hibernateSessionFactory) {
        DAOFactory.defaultHibernateSessionFactory = hibernateSessionFactory;
    }

    /**
     * Sets default HTTPClientSessionFactory
     * @param httpClientSessionFactory HTTPClient session factory
     */
    public static void setDefaultHTTPClientSessionFactory(IHTTPClientSessionFactory httpClientSessionFactory) {
        DAOFactory.defaultHTTPClientSessionFactory = httpClientSessionFactory;
    }

    /**
     * Get {@link IChannelDAO} object using default {@link IHTTPClientSessionFactory}
     * @return {@link IBroadcastDAO} object
     */
    public static IBroadcastDAO getBroadcastDAO() {
        if (broadcastDAO == null) {
            broadcastDAO = new BroadcastHibernateDAO();
            broadcastDAO.setSessionFactory(DAOFactory.defaultHibernateSessionFactory);
        }
        return broadcastDAO;
    }

    /**
     * Get {@link IChannelDAO} object using sessionFactory
     * @return {@link IBroadcastDAO} object
     */
    public static IBroadcastDAO getBroadcastDAO(IHibernateSessionFactory sessionFactory) {
        IBroadcastDAO dao = new BroadcastHibernateDAO();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    /**
     * Get {@link IChannelDAO} object using default {@link IHTTPClientSessionFactory}
     * @return {@link IChannelDAO} object
     */
    public static IChannelDAO getChannelDAO() {
        if (channelDAO == null) {
            channelDAO = new ChannelHibernateDAO();
            channelDAO.setSessionFactory(defaultHibernateSessionFactory);
        }
        return channelDAO;
    }

    /**
     * Get {@link IChannelDAO} object using sessionFactory
     * @return {@link IChannelDAO} object
     */
    public static IChannelDAO getChannelDAO(IHibernateSessionFactory sessionFactory) {
        IChannelDAO dao = new ChannelHibernateDAO();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    /**
     * Get {@link IChannelSourceDAO} object using default {@link IHTTPClientSessionFactory}
     * @return {@link IChannelSourceDAO} object
     * 
     * @deprecated do i need this method?
     */
    public static IChannelSourceDAO getChannelSourceDAO() {
        if (channelSourceDAO == null) {
            channelSourceDAO = new ChannelHTTPClientDAO();
            channelSourceDAO.setSessionFactory(defaultHTTPClientSessionFactory);
        }
        return channelSourceDAO;
    }

    /**
     * Get {@link IChannelSourceDAO} object using sessionFactory
     * @return {@link IChannelSourceDAO} object
     */
    public static IChannelSourceDAO getChannelSourceDAO(IHTTPClientSessionFactory sessionFactory) {
        IChannelSourceDAO dao = new ChannelHTTPClientDAO();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    /**
     * Get {@link IBroadcastSourceDAO} object using default {@link IHTTPClientSessionFactory}
     * @return {@link IBroadcastSourceDAO} object
     * 
     * @deprecated do i need this method?
     */
    public static IBroadcastSourceDAO getBroadcastSourceDAO() {
        if (broadcastSourceDAO == null) {
            broadcastSourceDAO = new BroadcastHTTPClientDAO();
            broadcastSourceDAO.setSessionFactory(defaultHTTPClientSessionFactory);
        }
        return broadcastSourceDAO;
    }

    /**
     * Get {@link IBroadcastSourceDAO} object using sessionFactory
     * @return {@link IBroadcastSourceDAO} object
     */
    public static IBroadcastSourceDAO getBroadcastSourceDAO(IHTTPClientSessionFactory sessionFactory) {
        IBroadcastSourceDAO dao = new BroadcastHTTPClientDAO();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }
}
