package de.sonivis.tool.core.datamodel.dao.hibernate;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import de.sonivis.tool.core.ModelManager;
import de.sonivis.tool.core.datamodel.GraphItem;
import de.sonivis.tool.core.datamodel.InfoSpace;
import de.sonivis.tool.core.datamodel.InfoSpaceItem;
import de.sonivis.tool.core.datamodel.dao.IGenericDAO;
import de.sonivis.tool.core.datamodel.exceptions.CannotConnectToDatabaseException;

/**
 * Abstract parent class for DAO implementations in Hibernate style.
 * <p>
 * The class offers several standard methods for interaction with the persistence layer. Most of the
 * provided methods wrap a similar functionality of methods of the same name contained in the
 * {@link Session} class.
 * </p>
 * <p>
 * There are methods for {@link #save(Object) saving}, {@link #update(Object) updating},
 * {@link #merge(Object) merging} {@link #delete(Object) deleting}, and {@link #get(Serializable)
 * retrieving} single items but also for {@link #saveAll(Collection) saving},
 * {@link #updateAll(Collection) updating}, {@link #mergeAll(Collection) merging},
 * {@link #deleteAll(Collection) deleting}, and {@link #findAll(InfoSpace) retrieving}
 * {@link Collection}s of items.
 * </p>
 * <p>
 * In case the caller is not sure whether saving or updating is appropriate on some item or
 * collection of items, for convenience the methods {@link #saveOrUpdate(Object)} and
 * {@link #saveOrUpdateAll(Collection)} are provided.
 * </p>
 * <p>
 * Furthermore, there is a special {@link #batchInsert(Collection) batch insertion} method. It
 * allows to insert larger amounts of items of the same type at once. Note, that this method is
 * currently using some MySQL-specific code that might fail when using with other databases.
 * </p>
 * <p>
 * For freeing the Hibernate cache, {@link #clearCache()} should be used. The method
 * {@link #batchInsert(Collection)} frees the cache automatically at the end of operation.
 * </p>
 * <p>
 * One may {@link #count(InfoSpace)} the items in a certain {@link InfoSpace} and also
 * {@link #containedInCache(Object) check for the cache containment} of an item.
 * </p>
 * <p>
 * Due to performance issues the persistence layer is mainly configured to lazy load items.
 * Therefore, it might be necessary from time to time to {@link #initialize(Object)} a lazily loaded
 * item.
 * </p>
 * <p>
 * Each method of this class guarantees proper session handling. This concludes that all returned
 * entities are <em>detached</em> and not connected to any session.
 * </p>
 * 
 * @author Andreas Erber
 * @version $Revision: 1417 $, $Date: 2010-01-28 09:24:56 -0500 (Thu, 28 Jan 2010) $
 */
public abstract class AbstractGenericDAO<T> implements IGenericDAO<T> {

    /**
	 * Logger.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGenericDAO.class);

    /**
	 * Number of items to be inserted by {@link #batchInsert()} before flushing.
	 */
    private static final int BATCH_SIZE = 100;

    /**
	 * Concrete type of the class to be handled by the implementer.
	 */
    private final Class<T> type;

    /**
	 * Constructor.
	 * <p>
	 * During construction time a reference to the {@link SessionFactory} provided and configured by
	 * the {@link ModelManager} is obtained from {@link ModelManager#getSessionFactory()}.
	 * </p>
	 * 
	 * @param type
	 *            Type to be handled by the implementer.
	 */
    protected AbstractGenericDAO(final Class<T> type) {
        super();
        this.type = type;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("AbstractGenericDAO initialization successfully done.");
        }
    }

    /**
	 * Receive the current session (Hibernate managed) from the {@link SessionFactory}.
	 * <p>
	 * The returned session is managed by Hibernate. The caller has only to take care of two things:
	 * starting a {@link Transaction} and commiting it. Usually exception handling and roll back of
	 * the {@link Transaction} should also be done.
	 * </p>
	 * <p>
	 * A standard piece of code looks like that:
	 * 
	 * <pre>
	 * Session s = this.currentSession();
	 * Transaction tx = null;
	 * try {
	 * 	tx = s.beginTransaction();
	 * 
	 * 	// do some work
	 * 
	 * 	tx.commit();
	 * } catch (HibernateException he) {
	 * 	tx.rollback();
	 * 
	 * 	// error handling
	 * } finally {
	 * 	s.close();
	 * }
	 * </pre>
	 * 
	 * As can be seen, the {@link Session} neither has to be released nor closed. For subsequent
	 * {@link Session}s and {@link Transaction}s it is recommended to always get the
	 * {@link #currentSession()} and {@link Session#beginTransaction() start} a {@link Transaction}.
	 * </p>
	 * 
	 * @return a {@link Session} to interact with the persistence layer.
	 * @throws CannotConnectToDatabaseException
	 *             if {@link ModelManager#getCurrentSession()} does
	 * @throws HibernateException
	 *             if {@link ModelManager#getCurrentSession()} does
	 */
    protected final Session currentSession() throws CannotConnectToDatabaseException {
        return ModelManager.getInstance().getCurrentSession();
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#clearCache()
	 * @see #currentSession()
	 * @see Session#clear()
	 */
    @Override
    public final void clearCache() throws CannotConnectToDatabaseException {
        currentSession().clear();
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#containedInCache(Object)
	 * @see #currentSession()
	 * @see Session#contains(Object)
	 */
    @Override
    public final boolean containedInCache(final T obj) throws CannotConnectToDatabaseException {
        return this.currentSession().contains(obj);
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#count(InfoSpace)
	 */
    @Override
    public Integer count(final InfoSpace is) throws CannotConnectToDatabaseException {
        if (is == null) {
            return null;
        }
        final Session s = this.currentSession();
        Transaction tx = null;
        Integer count = null;
        try {
            tx = s.beginTransaction();
            count = (Integer) s.createCriteria(this.type).add(Restrictions.eq(AbstractGenericDAO.INFOSPACE_FIELD, is)).setProjection(Projections.rowCount()).uniqueResult();
            s.clear();
            tx.commit();
        } catch (HibernateException he) {
            tx.rollback();
            LOGGER.error("Failed to count items within given InfoSpace - transaction was rolled back.", he);
            throw he;
        } finally {
            s.close();
        }
        return count;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @param obj
	 *            Object of type {@value T} to be deleted.
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#delete(java.lang.Object)
	 * @see Session#delete(Object)
	 */
    @Override
    public final void delete(final T obj) throws CannotConnectToDatabaseException {
        if (obj == null) {
            return;
        }
        final Session s = this.currentSession();
        Transaction tx = null;
        try {
            tx = s.beginTransaction();
            s.delete(obj);
            s.flush();
            s.clear();
            tx.commit();
        } catch (HibernateException he) {
            tx.rollback();
            LOGGER.error("Failed to delete given entity - transaction was rolled back.", he);
            throw he;
        } finally {
            s.close();
        }
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @param objects
	 *            A {@link Collection} of entities to be deleted.
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#deleteAll(Collection)
	 * @see Session#delete(Object)
	 */
    @Override
    public final void deleteAll(final Collection<? extends T> objects) throws CannotConnectToDatabaseException {
        if (objects != null && !objects.isEmpty()) {
            final Session s = this.currentSession();
            Transaction tx = null;
            try {
                tx = s.beginTransaction();
                for (T obj : objects) {
                    s.delete(obj);
                }
                s.flush();
                s.clear();
                tx.commit();
            } catch (HibernateException he) {
                tx.rollback();
                LOGGER.error("Failed to delete given entities - transaction was rolled back.", he);
                throw he;
            } finally {
                s.close();
            }
        }
    }

    /**
	 * {@inheritDoc}
	 * <p>
	 * Note, in case of the type parameter being an {@link InfoSpaceItem} or {@link GraphItem} (as
	 * well as their children) the properties of their instances will be fetched lazily. The DAO
	 * class parameter <em>lazyLoadProperties</em> is not considered. You will need to
	 * {@link #initialize(Object)} each of the returned entities to access their properties as well
	 * as associations.
	 * </p>
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#findAll()
	 */
    @SuppressWarnings("unchecked")
    @Override
    public EventList<? extends T> findAll(final InfoSpace is) throws CannotConnectToDatabaseException {
        if (is == null) {
            return null;
        }
        final Session s = this.currentSession();
        Transaction tx = null;
        List<T> li = null;
        try {
            tx = s.beginTransaction();
            li = this.createCriteria(s, this.type).add(Restrictions.eq(AbstractGenericDAO.INFOSPACE_FIELD, is)).setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY).list();
            s.clear();
            tx.commit();
        } catch (HibernateException he) {
            tx.rollback();
            LOGGER.error("Failed to retrieve all entities in InfoSpace - transaction was rolled back.", he);
            throw he;
        } finally {
            s.close();
        }
        return GlazedLists.eventList(li);
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @see IGenericDAO#findAllType()
	 */
    public final Class<T> findAllType() {
        return this.type;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#findByCriteriaQuery(DetachedCriteria)
	 * @see Criteria
	 * @see DetachedCriteria
	 */
    @SuppressWarnings("unchecked")
    @Override
    public final EventList<T> findByCriteriaQuery(final DetachedCriteria criteria) throws CannotConnectToDatabaseException {
        if (criteria == null) {
            return null;
        }
        final Session s = this.currentSession();
        Transaction tx = null;
        List<T> li = null;
        try {
            tx = s.beginTransaction();
            li = criteria.getExecutableCriteria(s).list();
            s.clear();
            tx.commit();
        } catch (HibernateException he) {
            tx.rollback();
            LOGGER.error("Failed to execute Criteria query - transaction was rolled back.", he);
            throw he;
        } finally {
            s.close();
        }
        return GlazedLists.eventList(li);
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @see IGenericDAO#findByCriteriaQueryType()
	 */
    @Override
    public final Class<T> findByCriteriaQueryType() {
        return this.type;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#get(Serializable)
	 * @see Session#get(Class, Serializable)
	 */
    @Override
    public final T get(final Serializable serialId) throws CannotConnectToDatabaseException {
        if (serialId == null) {
            return null;
        }
        return this.get(serialId, LockMode.NONE);
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#get(Serializable)
	 * @see Session#get(Class, Serializable, LockMode)
	 */
    @SuppressWarnings("unchecked")
    @Override
    public final T get(final Serializable serialId, final LockMode lockMode) throws CannotConnectToDatabaseException {
        if (serialId == null) {
            return null;
        }
        final String entityName = this.getEntityName();
        final Session s = this.currentSession();
        Transaction tx = null;
        T ret = null;
        try {
            tx = s.beginTransaction();
            ret = (T) s.get(entityName, serialId, lockMode);
            s.clear();
            tx.commit();
        } catch (HibernateException he) {
            tx.rollback();
            LOGGER.error("Failed to get entity - transaction was rolled back.", he);
            throw he;
        } finally {
            s.close();
        }
        return ret;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#load(Serializable)
	 * @see Session#load(Class, Serializable)
	 */
    @Override
    public Object load(final Serializable id) throws CannotConnectToDatabaseException {
        if (id == null) {
            return null;
        }
        return this.load(id, LockMode.NONE);
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#load(Serializable, LockMode)
	 * @see Session#load(Class, Serializable, LockMode)
	 */
    @Override
    public Object load(final Serializable id, final LockMode lockMode) throws CannotConnectToDatabaseException {
        if (id == null) {
            return null;
        }
        final String entityName = this.getEntityName();
        final Session s = this.currentSession();
        Transaction tx = null;
        Object ret = null;
        try {
            tx = s.beginTransaction();
            ret = s.load(entityName, id, lockMode);
            s.clear();
            tx.commit();
        } catch (HibernateException he) {
            tx.rollback();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to load entity - transaction was rolled back.", he);
            }
            he.printStackTrace();
            throw he;
        } finally {
            s.close();
        }
        return ret;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors
	 * @see IGenericDAO#merge(Object)
	 * @see Session#merge(Object)
	 */
    @SuppressWarnings("unchecked")
    @Override
    public final T merge(final T obj) throws CannotConnectToDatabaseException {
        if (obj == null) {
            return null;
        }
        final Session s = this.currentSession();
        Transaction tx = null;
        T ret = null;
        try {
            tx = s.beginTransaction();
            ret = (T) s.merge(obj);
            s.flush();
            s.clear();
            tx.commit();
        } catch (HibernateException he) {
            tx.rollback();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to merge entity - transaction was rolled back.", he);
            }
            he.printStackTrace();
            throw he;
        } finally {
            s.close();
        }
        return ret;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#mergeAll(Collection)
	 * @see Session#merge(Object)
	 */
    @Override
    public final void mergeAll(final Collection<? extends T> objects) throws CannotConnectToDatabaseException {
        if (objects != null && !objects.isEmpty()) {
            final Session s = this.currentSession();
            Transaction tx = null;
            try {
                tx = s.beginTransaction();
                for (T obj : objects) {
                    s.merge(obj);
                }
                s.flush();
                s.clear();
                tx.commit();
            } catch (HibernateException he) {
                tx.rollback();
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Failed to merge entities - transaction was rolled back.", he);
                }
                he.printStackTrace();
                throw he;
            } finally {
                s.close();
            }
        }
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#save(java.lang.Object)
	 * @see Session#save(Object)
	 */
    @Override
    public final Serializable save(final T obj) throws CannotConnectToDatabaseException {
        if (obj == null) {
            return null;
        }
        final Session s = this.currentSession();
        Transaction tx = null;
        Serializable id = null;
        try {
            tx = s.beginTransaction();
            id = s.save(obj);
            s.clear();
            tx.commit();
        } catch (HibernateException he) {
            tx.rollback();
            LOGGER.error("Failed to save entity - transaction was rolled back.", he);
            throw he;
        } finally {
            s.close();
        }
        return id;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#saveAll(Collection)
	 * @see Session#save(Object)
	 */
    @Override
    public final void saveAll(final Collection<? extends T> objects) throws CannotConnectToDatabaseException {
        if (objects != null && !objects.isEmpty()) {
            final Session s = this.currentSession();
            Transaction tx = null;
            try {
                tx = s.beginTransaction();
                for (T obj : objects) {
                    s.save(obj);
                }
                s.clear();
                tx.commit();
            } catch (HibernateException he) {
                tx.rollback();
                LOGGER.error("Failed to save entities - transaction was rolled back.", he);
                throw he;
            } finally {
                s.close();
            }
        }
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#saveOrUpdate(Object)
	 * @see Session#saveOrUpdate(Object)
	 */
    @Override
    public final void saveOrUpdate(final T obj) throws CannotConnectToDatabaseException {
        if (obj != null) {
            final Session s = this.currentSession();
            Transaction tx = null;
            try {
                tx = s.beginTransaction();
                s.saveOrUpdate(obj);
                s.flush();
                s.clear();
                tx.commit();
            } catch (HibernateException he) {
                tx.rollback();
                LOGGER.error("Failed to save or update entity - transaction was rolled back.", he);
                throw he;
            } finally {
                s.close();
            }
        }
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#pdate(Object)
	 * @see Session#update(Object)
	 */
    @Override
    public final void update(final T obj) throws CannotConnectToDatabaseException {
        if (obj != null) {
            final Session s = this.currentSession();
            Transaction tx = null;
            try {
                tx = s.beginTransaction();
                s.update(obj);
                s.flush();
                s.clear();
                tx.commit();
            } catch (HibernateException he) {
                tx.rollback();
                LOGGER.error("Failed to save or update entity - transaction was rolled back.", he);
                throw he;
            } finally {
                s.close();
            }
        }
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors.
	 * @see IGenericDAO#saveOrUpdateAll(Collection)
	 * @see Session#saveOrUpdate(Object)
	 */
    @Override
    public final void saveOrUpdateAll(final Collection<? extends T> objects) throws CannotConnectToDatabaseException {
        if (objects != null && !objects.isEmpty()) {
            final Session s = this.currentSession();
            Transaction tx = null;
            try {
                tx = s.beginTransaction();
                for (T obj : objects) {
                    s.saveOrUpdate(obj);
                }
                s.flush();
                s.clear();
                tx.commit();
            } catch (HibernateException he) {
                tx.rollback();
                LOGGER.error("Failed to save or update entities - transaction was rolled back.", he);
                throw he;
            } finally {
                s.close();
            }
        }
    }

    /**
	 * Get a {@link Criteria} according to the settings of the implementing child.
	 * 
	 * @param s
	 *            The {@link Session} to create the {@link Criteria} for.
	 * @param clazz
	 *            The type to create the {@link Criteria} for. If the argument is <code>null</code>
	 *            the type defaults to <em>T</em>.
	 * @return The {@link Criteria} according to the implementing class.
	 */
    protected abstract Criteria createCriteria(final Session s, final Class<? extends T> clazz);

    /**
	 * Insert large collections of objects into the persistence layer.
	 * <p>
	 * The {@link Collection} of <i>transient</i> or <i>detached</i> objects is stored in the
	 * persistence layer using {@link Session#saveOrUpdate(Object)} for each. The {@link Session}
	 * will be {@link Session#flush() flushed} and {@link Session#clear() cleared} when the save
	 * operation was executed {@value #BATCH_SIZE} times.
	 * </p>
	 * <p>
	 * Note also, that currently before insertion the foreign key checks are turned off with a
	 * MySQL-specific query. After all items have been inserted (and flushed) the checks are turned
	 * on again. Therefore, this method might cause problems when using with other DBMS.
	 * </p>
	 * 
	 * @param objects
	 *            A {@link Collection} of <i>transient</i> or <i>detached</i> objects.
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #currentSession()} does
	 * @throws HibernateException
	 *             in case of Hibernate errors
	 * @see Session#saveOrUpdate(Object)
	 * @see Session#setCacheMode(CacheMode)
	 */
    public final void batchInsert(final Collection<? extends T> objects) throws CannotConnectToDatabaseException {
        if (objects != null && !objects.isEmpty()) {
            final Session s = this.currentSession();
            Transaction tx = null;
            try {
                tx = s.beginTransaction();
                final String oldCacheMode = s.getCacheMode().toString();
                if (s.getCacheMode() != CacheMode.IGNORE) {
                    s.setCacheMode(CacheMode.IGNORE);
                }
                s.createSQLQuery("SET foreign_key_checks=0;").executeUpdate();
                int itemCount = 0;
                for (T obj : objects) {
                    s.saveOrUpdate(obj);
                    if (++itemCount % BATCH_SIZE == 0) {
                        s.flush();
                        s.clear();
                    }
                }
                s.createSQLQuery("SET foreign_key_checks=1;").executeUpdate();
                s.setCacheMode(CacheMode.parse(oldCacheMode));
                s.clear();
                tx.commit();
            } catch (HibernateException he) {
                tx.rollback();
                LOGGER.error("Failed to batch insert entities. This method contains MySQL specific code - transaction was rolled back.", he);
                throw he;
            } finally {
                s.close();
            }
        }
    }

    /**
	 * Returns the mapped entity's name.
	 * <p>
	 * The {@link #load()} and {@link #get()} methods cannot handle the proxy interface of the
	 * passed type parameter. Therefore, the corresponding entity name is required for these methods
	 * to function properly.
	 * </p>
	 * <p>
	 * Two requirements apply that the corresponding entity name is correctly retrieved from the
	 * interface name:
	 * <ul>
	 * <li>The simple name of the proxy interface has to to be equal to the entity class' simple
	 * name having a capital "I" prepended, i.e., entity's simple name: <em>Actor</em>, proxy
	 * interface's simple name: <em>IActor</em></li>
	 * <li>The package where the proxy interface is located is a sub-package of the entity's package
	 * named "proxy", i.e., entity's package: <em>de.sonivis.tool.core.datamodel</em>, interface's
	 * package: <em>de.sonivis.tool.core.datamodel.proxy</em></li>
	 * </ul>
	 * </p>
	 * 
	 * @return The entity's name.
	 * 
	 *         FIXME This is a dirty work-around.
	 */
    private String getEntityName() {
        if (this.type.isInterface()) {
            StringTokenizer st = new StringTokenizer(this.type.getName(), ".");
            StringBuilder sb = new StringBuilder();
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.equals("proxy")) {
                    continue;
                } else if (token.startsWith("I")) {
                    sb.append(token.substring(1));
                } else {
                    sb.append(token + ".");
                }
            }
            return sb.toString();
        } else {
            return this.type.getName();
        }
    }
}
