package net.sourceforge.domian.repository;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import net.sourceforge.domian.entity.Entity;
import net.sourceforge.domian.specification.CompositeSpecification;
import net.sourceforge.domian.specification.JpqlQueryHolder;
import net.sourceforge.domian.specification.Specification;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.persistence.Persistence.createEntityManagerFactory;
import static net.sourceforge.domian.repository.PersistenceDefinition.DELEGATED;
import static net.sourceforge.domian.specification.Specification2HqlConverter.convertSpecification2HqlQuery;
import static net.sourceforge.domian.specification.Specification2JpqlConverter.convert2DeleteStatement;
import static net.sourceforge.domian.specification.Specification2JpqlConverter.convertJpaMappedSpecificationType2PreparedJpqlQuery;
import static net.sourceforge.domian.specification.SpecificationUtils.updateEntityState;
import static net.sourceforge.domian.util.InstrumentationUtils.buildThreadNumberAndMessage;
import static org.apache.commons.lang.SystemUtils.FILE_SEPARATOR;

/**
 * This repository implementation is based on the <a href="http://www.hibernate.org">Hibernate</a> object-relational mapping (ORM) tool,
 * and its implementation of the Java Persistence API 1.0 (JPA).
 * The Hibernate Query Language (HQL) is used as a fallback solution for both JPA EntityManager and JPA Query Language (JPQL) where suitable.
 * <p/>
 * <i>
 * Highly experimental code as it has yet to be in any form of serious use.
 * The only tested RDBMS is <a href="http://www.h2database.com/html/main.html">H2</a>.
 * </i>
 * <p/>
 * <i>
 * Conversion of specification object graph to JPQL/HQL is not yet completed!
 * </i>
 * <p/>
 * <i>
 * Partitioning of this repository is not yet completed!
 * </i>
 * <p/>
 * This repository has a table naming convention: all table names must end <code>_TABLE</code>.
 * In your JPA ORM configuration file, you must explicitely configure all table names ending with <code>_TABLE</code>.
 * JPA does not seem to allow custom naming strategies...
 * <p/>
 * The persistence definition supported by this repository is {@code PersistenceDefinition.DELEGATED}.
 *
 * @author Eirik Torske
 * @see <a href="http://en.wikipedia.org/wiki/Java_Persistence_API">Java Persistence API (JPA)</a>
 * @since 0.4.2
 */
public class HibernateRepository<T extends Entity> extends AbstractDomianCoreRepository<T> implements BinaryFormatRepository<T> {

    protected Rdbms rdbms;

    protected String rdbmsServerName;

    protected String repositoryRootDirectoryString;

    protected String repositoryId;

    protected Map<String, String> configurationMap;

    protected String repositoryDirectoryString;

    protected File repositoryDirectory;

    protected EntityManagerFactory entityManagerFactory;

    protected List<String> tableNameList;

    public HibernateRepository(final Rdbms rdbms, final String repositoryId) {
        this(rdbms, "localhost", rdbms.getDefaultRepositoryRootDirectoryString(DEFAULT_DOMIAN_ROOT_PATH), repositoryId);
    }

    public HibernateRepository(final Rdbms rdbms, final String rdbmsServerName, final String repositoryRootDirectoryString, final String repositoryId) {
        this(rdbms, rdbmsServerName, repositoryRootDirectoryString, repositoryId, null);
    }

    public HibernateRepository(final Rdbms rdbms, final String rdbmsServerName, final String repositoryRootDirectoryString, final String repositoryId, final Map<String, String> configurationMap) {
        Validate.notNull(rdbms, "The RDBMS parameter cannot be empty");
        Validate.notEmpty(rdbmsServerName, "The RDBMS server name parameter cannot be empty");
        Validate.notEmpty(repositoryRootDirectoryString, "The repository root path parameter cannot be empty");
        Validate.notEmpty(repositoryId, "The repository ID parameter cannot be empty");
        this.rdbms = rdbms;
        this.rdbmsServerName = rdbmsServerName;
        this.repositoryRootDirectoryString = repositoryRootDirectoryString;
        this.repositoryId = repositoryId;
        this.configurationMap = configurationMap;
        this.repositoryDirectoryString = this.repositoryRootDirectoryString + FILE_SEPARATOR + this.repositoryId;
        if (!this.rdbms.isSupported()) {
            log.warn("RDBMS " + this.rdbms.getName() + " is not supported/tested");
        }
        try {
            this.entityManagerFactory = createEntityManagerFactory(getJpaPersistenceUnitName(), this.rdbms.getConfiguration(this.rdbmsServerName, this.repositoryDirectoryString, this.repositoryId, this.configurationMap));
        } catch (Exception e) {
            log.error("Unable to create EntityManagerFactory", e.getCause());
        }
        Validate.notNull(this.entityManagerFactory, "EntityManagerFactory is null - really no point to continue...");
        this.usesNativePartitioningSupport = TRUE;
        this.supportsRecursiveIndexing = TRUE;
        this.tableNameList = getTableNames();
    }

    private EntityManager createEntityManager() {
        final EntityManager entityManager = HibernateRepository.this.entityManagerFactory.createEntityManager();
        final Session hibernateSession = (Session) entityManager.getDelegate();
        hibernateSession.setFlushMode(FlushMode.ALWAYS);
        hibernateSession.setCacheMode(CacheMode.IGNORE);
        log.debug(buildThreadNumberAndMessage("Hibernate session/JPA EntityManager created [" + "hashCode=" + entityManager.hashCode() + ", " + "flushMode=" + hibernateSession.getFlushMode() + ", " + "cacheMode=" + hibernateSession.getCacheMode() + ", " + "entityMode=" + hibernateSession.getEntityMode() + "]"));
        return entityManager;
    }

    public static String getJpaPersistenceUnitName() {
        return "domian-hibernate-repository";
    }

    protected final ThreadLocal<EntityManager> entityManager = new ThreadLocal<EntityManager>() {

        @Override
        protected EntityManager initialValue() {
            return createEntityManager();
        }
    };

    protected EntityManager getEntityManager() {
        return this.entityManager.get();
    }

    @Override
    protected void onMakePartition() {
        log.warn("Partitioning is not yet supported");
    }

    List<String> getTableNames() {
        final EntityManager entityManager = getEntityManager();
        EntityTransaction tx = null;
        List<String> results = null;
        try {
            tx = entityManager.getTransaction();
            if (!tx.isActive()) {
                tx.begin();
            }
            Query query = null;
            switch(this.rdbms) {
                case H2:
                    query = entityManager.createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'TABLE'");
                    break;
                default:
                    log.warn("Unable to obtain schema table names for RDBMS " + this.rdbms.getName());
            }
            if (query != null) {
                results = query.getResultList();
            }
            tx.commit();
        } catch (Exception e) {
            log.error("Unable to obtain schema table names", e);
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }
        return results;
    }

    @Override
    protected Boolean contains(final T entity) {
        return getEntityManager().contains(entity);
    }

    @Override
    public <V extends T> Iterator<V> iterateAllEntitiesSpecifiedBy(final Specification<V> specification) {
        Validate.notNull(specification, "Specification parameter cannot be null");
        return new HibernateRepositoryIterator<V>(specification);
    }

    @Override
    public <V extends T> Collection<V> findAllEntitiesSpecifiedBy(final Specification<V> specification) {
        Validate.notNull(specification, "Specification parameter cannot be null");
        final EntityManager entityManager = getEntityManager();
        EntityTransaction tx = null;
        Collection<V> results = new HashSet<V>();
        if (specification.getType().equals(Entity.class)) {
            try {
                tx = entityManager.getTransaction();
                if (!tx.isActive()) {
                    tx.begin();
                }
                final Query query = entityManager.createQuery("from java.lang.Object o");
                results.addAll(query.getResultList());
                for (final V foundEntity : results) {
                    entityManager.refresh(foundEntity);
                }
                tx.commit();
            } catch (Exception e) {
                log.error("Unable to perform find operation with specification=" + specification, e);
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            }
        } else if (specification instanceof CompositeSpecification) {
            final JpqlQueryHolder jpqlQueryHolder = convertJpaMappedSpecificationType2PreparedJpqlQuery((CompositeSpecification) specification, this.tableNameList);
            try {
                tx = entityManager.getTransaction();
                if (!tx.isActive()) {
                    tx.begin();
                }
                final Query query = entityManager.createQuery(jpqlQueryHolder.getJpqlQueryString());
                for (String parameterName : jpqlQueryHolder.getParameterMap().keySet()) {
                    query.setParameter(parameterName, jpqlQueryHolder.getParameterMap().get(parameterName));
                }
                results.addAll(query.getResultList());
                for (final V foundEntity : results) {
                    entityManager.refresh(foundEntity);
                }
                tx.commit();
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                log.error("Unable to perform find operation with specification=" + specification, e);
            }
        } else if (specification instanceof PartitionRepositoryInvocationHandler.PartitionAwareSpecification) {
            PartitionRepositoryInvocationHandler.PartitionAwareSpecification partitionAwareSpecification = (PartitionRepositoryInvocationHandler.PartitionAwareSpecification) specification;
            Specification<V> spec = partitionAwareSpecification.partitionAwareSpecification;
            return findAllEntitiesSpecifiedBy(spec);
        } else {
            throw new NotImplementedException("specifification=" + specification);
        }
        return results;
    }

    @Override
    public <V extends T> V findSingleEntitySpecifiedBy(final Specification<V> specification) {
        Validate.notNull(specification, "Specification parameter cannot be null");
        final EntityManager entityManager = getEntityManager();
        EntityTransaction tx = null;
        V result = null;
        if (specification.getType().equals(Entity.class)) {
            final String hqlExpression = convertSpecification2HqlQuery(specification);
            try {
                tx = entityManager.getTransaction();
                if (!tx.isActive()) {
                    tx.begin();
                }
                final List<V> results = entityManager.createQuery(hqlExpression).getResultList();
                if (results != null) {
                    for (final V foundEntity : results) {
                        entityManager.refresh(foundEntity);
                    }
                }
                tx.commit();
                if (results.size() == 1) {
                    result = results.iterator().next();
                } else if (results.size() < 1) {
                    return null;
                } else if (results.size() > 1) {
                    int numberOfApprovedEntities = 0;
                    V currentApprovedEntity = null;
                    for (final V entity : results) {
                        if (specification.isSatisfiedBy(entity)) {
                            ++numberOfApprovedEntities;
                            currentApprovedEntity = entity;
                        }
                    }
                    if (numberOfApprovedEntities == 1) {
                        result = currentApprovedEntity;
                    } else {
                        throw new IllegalArgumentException("More than one entity were found when only one was expected");
                    }
                }
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                log.error("Unable to perform find-single operation with specification=" + specification, e);
            }
        } else {
            if (specification instanceof CompositeSpecification) {
                final JpqlQueryHolder jpqlQueryHolder = convertJpaMappedSpecificationType2PreparedJpqlQuery((CompositeSpecification) specification, this.tableNameList);
                try {
                    tx = entityManager.getTransaction();
                    if (!tx.isActive()) {
                        tx.begin();
                    }
                    final Query query = entityManager.createQuery(jpqlQueryHolder.getJpqlQueryString());
                    for (String parameterName : jpqlQueryHolder.getParameterMap().keySet()) {
                        query.setParameter(parameterName, jpqlQueryHolder.getParameterMap().get(parameterName));
                    }
                    try {
                        result = (V) query.getSingleResult();
                    } catch (NoResultException e) {
                    } catch (Exception e) {
                        System.err.println("getSingleResult(); exception caught: " + e);
                        e.printStackTrace(System.err);
                    }
                    if (result != null) {
                        entityManager.refresh(result);
                    }
                    tx.commit();
                } catch (Exception e) {
                    if (tx != null && tx.isActive()) {
                        tx.rollback();
                    }
                    log.error("Unable to perform find-single operation with specification=" + specification, e);
                }
            } else {
                throw new NotImplementedException();
            }
        }
        return result;
    }

    @Override
    public <V extends T> void put(final V entity) {
        if (entity == null) {
            log.debug("Skipping putting of null entity");
        } else {
            final EntityManager entityManager = getEntityManager();
            EntityTransaction tx = null;
            try {
                tx = entityManager.getTransaction();
                if (!tx.isActive()) {
                    tx.begin();
                }
                entityManager.persist(entity);
                log.debug(entity + " persisted (not yet comitted)");
                tx.commit();
            } catch (Exception e) {
                log.warn(entity + " (maybe) already persisted - trying update instead...", e);
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                    tx = entityManager.getTransaction();
                    try {
                        if (!tx.isActive()) {
                            tx.begin();
                        }
                        entityManager.merge(entity);
                        log.debug(entity + " updated (not yet comitted)");
                        tx.commit();
                    } catch (Exception e2) {
                        log.error("Neither able to store nor update entity=" + entity, e2);
                    }
                } else {
                    update(entity);
                }
            }
        }
    }

    protected class Update implements Callable<Void> {

        private Entity entity;

        protected Update(Entity entity) {
            this.entity = entity;
        }

        @Override
        public Void call() throws Exception {
            final EntityManager entityManager = getEntityManager();
            EntityTransaction tx = null;
            try {
                tx = entityManager.getTransaction();
                if (!tx.isActive()) {
                    tx.begin();
                }
                entityManager.merge(entity);
                tx.commit();
                return null;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    @Override
    public <V extends T> void update(final V entity) {
        update(entity, null);
    }

    @Override
    public <V extends T> void update(final V entity, Specification<?> deltaSpecification) {
        if (entity == null) {
            log.debug("Skipping putting of null entity");
        } else {
            final int RETRIES = 3;
            int index = 0;
            boolean success = false;
            Callable<Void> update = new Update(entity);
            while (index < RETRIES && !success) {
                try {
                    update.call();
                    success = true;
                } catch (Exception e) {
                    log.warn("Exception caught while updating entity [type=" + entity.getClass().getName() + ", entityId=" + entity.getEntityId() + "]");
                    ++index;
                    final EntityManager entityManager = getEntityManager();
                    entityManager.clear();
                    final V reAttachedEntity = (V) entityManager.find(entity.getClass(), entity.getEntityId());
                    updateEntityState(reAttachedEntity, deltaSpecification);
                    update = new Update(reAttachedEntity);
                }
            }
        }
    }

    /**
     * <i>Extra method</i>
     * <p/>
     * Purges all records in givan table without no further ado.
     */
    public void removeAllRecordsInTable(final String tableName) {
        long numberOfEntitiesRemoved = 0;
        final EntityManager entityManager = getEntityManager();
        EntityTransaction tx = null;
        try {
            tx = entityManager.getTransaction();
            if (!tx.isActive()) {
                tx.begin();
            }
            Query query = entityManager.createNativeQuery("delete from " + tableName);
            numberOfEntitiesRemoved = query.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            log.error("Removal of entities failed", e);
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }
        log.info(numberOfEntitiesRemoved + " records explicitely deleted in table " + tableName.toUpperCase());
    }

    /**
     * <i>Extra method</i>
     * <p/>
     * Purges all entities in repository without no further ado.
     */
    public Long removeAllEntities() {
        long numberOfEntitiesRemoved = 0;
        final EntityManager entityManager = getEntityManager();
        EntityTransaction tx = null;
        try {
            synchronized (this) {
                tx = entityManager.getTransaction();
                if (!tx.isActive()) {
                    tx.begin();
                }
            }
            final Query query = entityManager.createQuery("delete from java.lang.Object");
            numberOfEntitiesRemoved = query.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("Removal of entities failed", e);
        }
        log.debug(numberOfEntitiesRemoved + " entities removed");
        return numberOfEntitiesRemoved;
    }

    @Override
    public <V extends T> Long removeAllEntitiesSpecifiedBy(final Specification<V> specification) {
        Validate.notNull(specification, "Specification parameter cannot be null");
        if (specification.getType().equals(Entity.class)) {
            return removeAllEntities();
        }
        long numberOfRemovedEntitites = 0;
        final EntityManager entityManager = getEntityManager();
        EntityTransaction tx = null;
        try {
            tx = entityManager.getTransaction();
            if (!tx.isActive()) {
                tx.begin();
            }
            final List<String> jpqlQueryStringList = convert2DeleteStatement(specification, this.tableNameList);
            for (String jpqlQueryString : jpqlQueryStringList) {
                log.debug("JPQL query=" + jpqlQueryString);
                if (jpqlQueryString == null) {
                    throw new RepositoryException("JPQL query cannot be null [converted from specification=" + specification + "]");
                }
                final Query query = entityManager.createQuery(jpqlQueryString);
                numberOfRemovedEntitites += query.executeUpdate();
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("Unable to perform remove operation with specification=" + specification, e);
        }
        return numberOfRemovedEntitites;
    }

    @Override
    public <V extends T> Boolean remove(final V entity) {
        if (entity == null) {
            return FALSE;
        }
        Boolean success = TRUE;
        final EntityManager entityManager = getEntityManager();
        EntityTransaction tx = null;
        try {
            tx = entityManager.getTransaction();
            if (!tx.isActive()) {
                tx.begin();
            }
            final V attachedEntity = (V) entityManager.find(entity.getClass(), entity.getEntityId());
            if (attachedEntity == null) {
                tx.commit();
                return FALSE;
            }
            if (!entityManager.contains(attachedEntity)) {
                tx.commit();
                return FALSE;
            }
            entityManager.remove(attachedEntity);
            log.debug(entity + " removed");
            tx.commit();
        } catch (Exception e) {
            try {
                log.warn(entity + " NOT removed", e);
            } catch (Exception e1) {
            }
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            success = FALSE;
        }
        return success;
    }

    @Override
    public File getRepositoryDirectory() {
        return new File(this.repositoryDirectoryString);
    }

    @Override
    public String getRepositoryId() {
        return this.repositoryId;
    }

    @Override
    public PersistenceDefinition getPersistenceDefinition() {
        return DELEGATED;
    }

    @Override
    public String getFormat() {
        throw new NotImplementedException();
    }

    @Override
    public void load() {
        log.warn("load() is only applicable for repositories with asynchronous persistence [PersistenceDefinition.INMEMORY_AND_*]");
    }

    @Override
    public void persist() {
        log.info("persist() invoked - flushing and clearing/deattaching all entities in JPA entity manager");
        EntityTransaction tx = null;
        try {
            final EntityManager entityManager = getEntityManager();
            tx = entityManager.getTransaction();
            if (!tx.isActive()) {
                tx.begin();
            }
            entityManager.clear();
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("Unable to flush entities", e);
        }
    }

    @Override
    public EntityPersistenceMetaData getMetaDataFor(final T entity) {
        throw new NotImplementedException();
    }

    @Override
    public void close() {
        log.info("Permanently closing entity manager instance and entity manager factory instance");
        if (getEntityManager().isOpen()) {
            getEntityManager().close();
        }
        if (this.entityManagerFactory.isOpen()) {
            this.entityManagerFactory.close();
        }
    }

    /** Custom iterator class for {@link HibernateRepository}. */
    protected class HibernateRepositoryIterator<V extends T> implements Iterator<V> {

        protected final Specification<V> iteratorSpecification;

        protected final Iterator<V> iterator;

        protected V currentEntity;

        protected boolean nextIsInvoked = false;

        public HibernateRepositoryIterator(final Specification<V> specification) {
            this.iteratorSpecification = specification;
            final Collection<V> entityCollection = HibernateRepository.this.findAllEntitiesSpecifiedBy(this.iteratorSpecification);
            this.iterator = entityCollection.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public V next() {
            if (!this.iterator.hasNext()) {
                throw new NoSuchElementException();
            }
            this.currentEntity = this.iterator.next();
            this.nextIsInvoked = true;
            return this.currentEntity;
        }

        @Override
        public void remove() {
            if (!this.nextIsInvoked) {
                throw new IllegalStateException();
            }
            this.iterator.remove();
            HibernateRepository.this.remove(this.currentEntity);
            nextIsInvoked = false;
        }
    }
}
