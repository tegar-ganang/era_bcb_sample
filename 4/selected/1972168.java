package net.sf.myway.hibernate;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sf.myway.hibernate.preferences.PreferenceConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.SAXReader;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.ObjectDeletedException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.Dialect;
import org.w3c.dom.Document;

/**
 * @author Andreas Beckers
 * @version $Revision$
 */
public class HibernateUtil {

    private static Log _log = LogFactory.getLog(HibernateUtil.class);

    private static SessionFactory _sessionFactory;

    private static AnnotationConfiguration _config;

    private final List<Class<?>> _classes;

    private Session _session;

    private Interceptor _interceptor;

    /**
	 * 
	 */
    HibernateUtil() {
        _classes = new ArrayList<Class<?>>();
        try {
            _config = new AnnotationConfiguration();
            final SAXReader r = new SAXReader();
            final InputStream cfgXml = this.getClass().getResourceAsStream("hibernate.cfg.xml");
            final Document x = new DOMWriter().write(r.read(cfgXml));
            _config.configure(x);
            configConnection();
            final IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.sf.myway.hibernate.provider");
            final IExtension[] extensions = point.getExtensions();
            _config.addAnnotatedClass(UuidEntity.class);
            _config.addAnnotatedClass(NamedUuidEntity.class);
            for (final IExtension ext : extensions) {
                final IConfigurationElement[] elements = ext.getConfigurationElements();
                for (final IConfigurationElement ce : elements) {
                    final EntityClassProvider iFace = (EntityClassProvider) ce.createExecutableExtension("class");
                    for (final Class<?> c : iFace.getEntityClasses()) {
                        _classes.add(c);
                        _config.addAnnotatedClass(c);
                    }
                }
            }
        } catch (final Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public void checkAnnotatedClassRegistered(final Class<?>... clazz) throws Exception {
        for (final Class<?> c : clazz) if (c != null && !isAnnotatedClassRegistered(c)) {
            registerAnnotatedClass(c);
            _classes.add(c);
        }
        close();
    }

    public void close() {
        if (_sessionFactory == null) return;
        _sessionFactory.close();
        _sessionFactory = null;
    }

    /**
	 * 
	 */
    public void configConnection() {
        final IPreferenceStore pref = HibernateActivator.getDefault().getPreferenceStore();
        initConfigProperty(pref.getString(PreferenceConstants.P_DIALECT), "hibernate.dialect");
        initConfigProperty(pref.getString(PreferenceConstants.P_DRIVER), "hibernate.connection.driver_class");
        initConfigProperty(pref.getString(PreferenceConstants.P_URL), "hibernate.connection.url");
        initConfigProperty(pref.getString(PreferenceConstants.P_USER), "hibernate.connection.username");
        initConfigProperty(pref.getString(PreferenceConstants.P_PASSWORD), "hibernate.connection.password");
    }

    /**
	 * @param obj
	 */
    public void delete(final Iterable<? extends Object> o) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            for (final Object x : o) try {
                session.delete(session.merge(x));
            } catch (final ObjectDeletedException t) {
            }
            trans.commit();
        } catch (final Throwable t) {
            trans.rollback();
        }
    }

    public void delete(final Object... o) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            for (final Object x : o) try {
                session.delete(session.merge(x));
            } catch (final ObjectDeletedException t) {
            }
            trans.commit();
        } catch (final HibernateException t) {
            trans.rollback();
            throw t;
        }
    }

    public void executeSQL(final String sql) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            session.createSQLQuery(sql).executeUpdate();
            trans.commit();
        } catch (final Throwable t) {
            trans.rollback();
        }
    }

    public Dialect getDialect() {
        return Dialect.getDialect(_config.getProperties());
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(final Query q, final Object... param) {
        for (int i = 0; i < param.length; i++) q.setParameter(i, param[i]);
        final List<T> l = q.list();
        return l;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(final Session session, final String hql, final Object... param) {
        final Query q = session.createQuery(hql);
        for (int i = 0; i < param.length; i++) q.setParameter(i, param[i]);
        final List<T> l = q.list();
        return l;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(final String hql, final int firstResult, final int maxResults, final Object... param) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            final Query q = session.createQuery(hql);
            if (firstResult != -1) q.setFirstResult(firstResult);
            if (maxResults != -1) q.setMaxResults(maxResults);
            for (int i = 0; i < param.length; i++) q.setParameter(i, param[i]);
            final List<T> l = q.list();
            trans.commit();
            return l;
        } catch (final Throwable t) {
            trans.rollback();
            t.printStackTrace();
            trans.rollback();
            final List<T> r = Collections.emptyList();
            return r;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(final String hql, final int maxResults, final Object... param) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            final Query q = session.createQuery(hql);
            q.setMaxResults(maxResults);
            for (int i = 0; i < param.length; i++) q.setParameter(i, param[i]);
            final List<T> l = q.list();
            trans.commit();
            return l;
        } catch (final Throwable t) {
            trans.rollback();
            t.printStackTrace();
            trans.rollback();
            final List<T> r = Collections.emptyList();
            return r;
        }
    }

    /**
	 * 
	 */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(final String hql, final Object... param) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            final Query q = session.createQuery(hql);
            for (int i = 0; i < param.length; i++) q.setParameter(i, param[i]);
            final List<T> l = q.list();
            trans.commit();
            return l;
        } catch (final Throwable t) {
            trans.rollback();
            t.printStackTrace();
            final List<T> r = Collections.emptyList();
            return r;
        }
    }

    public Session getSession() throws HibernateException {
        if (_session == null) _session = _interceptor == null ? getSessionFactory().openSession() : getSessionFactory().openSession(_interceptor);
        return _session;
    }

    public SessionFactory getSessionFactory() throws HibernateException {
        if (_sessionFactory == null) {
            final Thread cur = Thread.currentThread();
            final ClassLoader save = cur.getContextClassLoader();
            cur.setContextClassLoader(new MultiClassLoader(save, _classes));
            try {
                _sessionFactory = _config.buildSessionFactory();
            } finally {
                cur.setContextClassLoader(save);
            }
        }
        return _sessionFactory;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSingleResult(final String hql, final Object... param) {
        final Session session = getSession();
        try {
            final Query q = session.createQuery(hql);
            for (int i = 0; i < param.length; i++) q.setParameter(i, param[i]);
            return (T) q.uniqueResult();
        } catch (final Throwable t) {
            _log.error("", t);
            return null;
        }
    }

    public StatelessSession getStatelessSession() throws HibernateException {
        if (_sessionFactory == null) {
            final Thread cur = Thread.currentThread();
            final ClassLoader save = cur.getContextClassLoader();
            cur.setContextClassLoader(new MultiClassLoader(save, _classes));
            try {
                _sessionFactory = _config.buildSessionFactory();
            } finally {
                cur.setContextClassLoader(save);
            }
        }
        return _sessionFactory.openStatelessSession();
    }

    private void initConfigProperty(final String val, final String key) {
        if (key != null) _config.setProperty(key, val);
    }

    public boolean isAnnotatedClassRegistered(final Class<?> clazz) throws Exception {
        if (clazz != null) return _config.getClassMapping(clazz.getName()) != null;
        throw new Exception("Class can not be null");
    }

    @SuppressWarnings("unchecked")
    public <T> void iterate(final EntityAction<T> action, final String hql, final Object... param) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            final Query q = session.createQuery(hql);
            for (int i = 0; i < param.length; i++) q.setParameter(i, param[i]);
            q.setReadOnly(true);
            for (final Iterator<T> it = q.iterate(); it.hasNext(); ) action.execute(it.next());
            trans.commit();
        } catch (final CancelException e) {
            trans.commit();
        } catch (final Throwable t) {
            t.printStackTrace();
            trans.rollback();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void iterateNoTx(final EntityAction<T> action, final String hql, final Object... param) {
        final Session session = getSession();
        try {
            final Query q = session.createQuery(hql);
            for (int i = 0; i < param.length; i++) q.setParameter(i, param[i]);
            q.setReadOnly(true);
            final ScrollableResults rs = q.setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
            int count = 0;
            while (rs.next()) {
                final T obj = (T) rs.get(0);
                action.execute(obj);
                if (++count % 20 == 0) {
                    session.flush();
                    session.clear();
                }
            }
        } catch (final CancelException e) {
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    /**
	 * @param c
	 * @param id
	 * @return
	 */
    public Object load(final Class<?> c, final Serializable id) {
        final Session session = getSession();
        return session.load(c, id);
    }

    /**
	 * @param map
	 */
    @SuppressWarnings("unchecked")
    public <T> T merge(final T o) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            final T r = (T) session.merge(o);
            trans.commit();
            return r;
        } catch (final Throwable t) {
            trans.rollback();
            return null;
        }
    }

    public void persist(final Object... os) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            for (final Object o : os) session.persist(o);
            trans.commit();
        } catch (final Throwable t) {
            trans.rollback();
        }
    }

    public Query prepareQuery(final Session session, final String hql) {
        return session.createQuery(hql);
    }

    public void refresh(final Object f) {
        final Session session = getSession();
        session.refresh(f);
    }

    public void registerAnnotatedClass(final Class<?> clazz) {
        if (clazz != null) {
            _log.info("registerAnnotatedClass " + clazz.getName());
            _config.addAnnotatedClass(clazz);
        }
    }

    public void runTransaction(final DBAction action) {
        final Session session = getSession();
        final Transaction trans = session.beginTransaction();
        try {
            action.run(session);
            trans.commit();
        } catch (final Throwable t) {
            trans.rollback();
        }
    }

    /**
	 * @param tradeInterceptor
	 */
    public void setInterceptor(final Interceptor interceptor) {
        _interceptor = interceptor;
    }

    /**
	 * @param f
	 */
    public void update(final Object f) {
        final Session session = getSession();
        session.update(f);
    }
}
