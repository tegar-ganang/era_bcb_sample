package ru.pit.tvlist.persistence;

import java.io.FileInputStream;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import ru.pit.tvlist.persistence.dao.DAOFactory;
import ru.pit.tvlist.persistence.dao.hibernate.HibernateSessionFactory;
import ru.pit.tvlist.persistence.dao.hibernate.HibernateSingletonSessionFactory;
import ru.pit.tvlist.persistence.dao.hibernate.IHibernateSessionFactory;
import ru.pit.tvlist.persistence.dao.httpclient.HTTPClientSessionFactory;
import ru.pit.tvlist.persistence.dao.httpclient.IHTTPClientSessionFactory;
import ru.pit.tvlist.persistence.domain.Broadcast;
import ru.pit.tvlist.persistence.domain.Channel;
import ru.pit.tvlist.persistence.service.TVListService;

public abstract class TestUtils {

    private TestUtils() {
    }

    static {
        loadData();
    }

    private static TVListService service = null;

    private static IHibernateSessionFactory hibernateSessionFactory = null;

    private static IHibernateSessionFactory hibernateSingletonSessionFactory = null;

    private static IHTTPClientSessionFactory httpClientSessionFactory = null;

    private static Configuration cfg = null;

    private static SessionFactory sessionFactory = null;

    public static synchronized TVListService getService() {
        if (service == null) {
            pr("Initializing Service ...");
            service = new TVListService(DAOFactory.getChannelDAO(getHibernateSessionFactory()), DAOFactory.getBroadcastDAO(getHibernateSessionFactory()));
        }
        return service;
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            pr("Initializing Hibernate SessionFactory ...");
            sessionFactory = getCfg().buildSessionFactory();
            loadData();
        }
        return sessionFactory;
    }

    public static synchronized Session getSession() {
        return getSessionFactory().openSession();
    }

    public static synchronized Configuration getCfg() {
        if (cfg == null) {
            pr("Initializing Hibernate Configuration ...");
            cfg = new Configuration().addClass(Channel.class).addClass(Broadcast.class);
        }
        return cfg;
    }

    public static synchronized void loadData() {
        try {
            IDatabaseConnection conn = new DatabaseConnection(getSession().connection());
            IDataSet dataSet = new FlatXmlDataSet(new FileInputStream("target/test-classes/testData.xml"));
            DatabaseOperation.CLEAN_INSERT.execute(conn, dataSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void clearData() {
        try {
            IDatabaseConnection conn = new DatabaseConnection(getSession().connection());
            IDataSet dataSet = new FlatXmlDataSet(new FileInputStream("target/test-classes/testData.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized IHibernateSessionFactory getHibernateSessionFactory() {
        if (hibernateSessionFactory == null) {
            hibernateSessionFactory = new HibernateSessionFactory(getSessionFactory());
        }
        return hibernateSessionFactory;
    }

    public static synchronized IHibernateSessionFactory getHibernateSingletonSessionFactory() {
        if (hibernateSingletonSessionFactory == null) {
            hibernateSingletonSessionFactory = new HibernateSingletonSessionFactory(getSessionFactory());
        }
        return hibernateSingletonSessionFactory;
    }

    public static synchronized IHTTPClientSessionFactory getHttpClientSessionFactory() {
        if (httpClientSessionFactory == null) {
            httpClientSessionFactory = new HTTPClientSessionFactory();
        }
        return httpClientSessionFactory;
    }

    public static void pr(String s) {
    }
}
