package org.hibernate.search.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.BatchCoordinator;
import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.jmx.IndexingProgressMonitor;
import org.hibernate.search.util.LoggerFactory;

/**
 * Prepares and configures a BatchIndexingWorkspace to start rebuilding
 * the indexes for all entity instances in the database.
 * The type of these entities is either all indexed entities or a
 * subset, always including all subtypes.
 *
 * @author Sanne Grinovero
 */
public class MassIndexerImpl implements MassIndexer {

    private static final Logger log = LoggerFactory.make();

    private final SearchFactoryImplementor searchFactoryImplementor;

    private final SessionFactory sessionFactory;

    protected Set<Class<?>> rootEntities = new HashSet<Class<?>>();

    private int objectLoadingThreads = 2;

    private int collectionLoadingThreads = 4;

    private Integer writerThreads = null;

    private int objectLoadingBatchSize = 10;

    private long objectsLimit = 0;

    private CacheMode cacheMode = CacheMode.IGNORE;

    private boolean optimizeAtEnd = true;

    private boolean purgeAtStart = true;

    private boolean optimizeAfterPurge = true;

    private MassIndexerProgressMonitor monitor;

    protected MassIndexerImpl(SearchFactoryImplementor searchFactory, SessionFactory sessionFactory, Class<?>... entities) {
        this.searchFactoryImplementor = searchFactory;
        this.sessionFactory = sessionFactory;
        rootEntities = toRootEntities(searchFactoryImplementor, entities);
        if (searchFactoryImplementor.isJMXEnabled()) {
            monitor = new IndexingProgressMonitor();
        } else {
            monitor = new SimpleIndexingProgressMonitor();
        }
    }

    /**
	 * From the set of classes a new set is built containing all indexed
	 * subclasses, but removing then all subtypes of indexed entities.
	 *
	 * @param selection
	 *
	 * @return a new set of entities
	 */
    private static Set<Class<?>> toRootEntities(SearchFactoryImplementor searchFactoryImplementor, Class<?>... selection) {
        Set<Class<?>> entities = new HashSet<Class<?>>();
        for (Class<?> entityType : selection) {
            Set<Class<?>> targetedClasses = searchFactoryImplementor.getIndexedTypesPolymorphic(new Class[] { entityType });
            if (targetedClasses.isEmpty()) {
                String msg = entityType.getName() + " is not an indexed entity or a subclass of an indexed entity";
                throw new IllegalArgumentException(msg);
            }
            entities.addAll(targetedClasses);
        }
        Set<Class<?>> cleaned = new HashSet<Class<?>>();
        Set<Class<?>> toRemove = new HashSet<Class<?>>();
        for (Class<?> type : entities) {
            boolean typeIsOk = true;
            for (Class<?> existing : cleaned) {
                if (existing.isAssignableFrom(type)) {
                    typeIsOk = false;
                    break;
                }
                if (type.isAssignableFrom(existing)) {
                    toRemove.add(existing);
                }
            }
            if (typeIsOk) {
                cleaned.add(type);
            }
        }
        cleaned.removeAll(toRemove);
        log.debug("Targets for indexing job: {}", cleaned);
        return cleaned;
    }

    public MassIndexer cacheMode(CacheMode cacheMode) {
        if (cacheMode == null) {
            throw new IllegalArgumentException("cacheMode must not be null");
        }
        this.cacheMode = cacheMode;
        return this;
    }

    public MassIndexer threadsToLoadObjects(int numberOfThreads) {
        if (numberOfThreads < 1) {
            throw new IllegalArgumentException("numberOfThreads must be at least 1");
        }
        this.objectLoadingThreads = numberOfThreads;
        return this;
    }

    public MassIndexer batchSizeToLoadObjects(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1");
        }
        this.objectLoadingBatchSize = batchSize;
        return this;
    }

    public MassIndexer threadsForSubsequentFetching(int numberOfThreads) {
        if (numberOfThreads < 1) {
            throw new IllegalArgumentException("numberOfThreads must be at least 1");
        }
        this.collectionLoadingThreads = numberOfThreads;
        return this;
    }

    public MassIndexer threadsForIndexWriter(int numberOfThreads) {
        if (numberOfThreads < 1) throw new IllegalArgumentException("numberOfThreads must be at least 1");
        this.writerThreads = numberOfThreads;
        return this;
    }

    public MassIndexer progressMonitor(MassIndexerProgressMonitor monitor) {
        this.monitor = monitor;
        return this;
    }

    public MassIndexer optimizeOnFinish(boolean optimize) {
        this.optimizeAtEnd = optimize;
        return this;
    }

    public MassIndexer optimizeAfterPurge(boolean optimize) {
        this.optimizeAfterPurge = optimize;
        return this;
    }

    public MassIndexer purgeAllOnStart(boolean purgeAll) {
        this.purgeAtStart = purgeAll;
        return this;
    }

    public Future<?> start() {
        BatchCoordinator coordinator = createCoordinator();
        ExecutorService executor = Executors.newFixedThreadPool(1, "batch coordinator");
        try {
            Future<?> submit = executor.submit(coordinator);
            return submit;
        } finally {
            executor.shutdown();
        }
    }

    public void startAndWait() throws InterruptedException {
        BatchCoordinator coordinator = createCoordinator();
        coordinator.run();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    protected BatchCoordinator createCoordinator() {
        return new BatchCoordinator(rootEntities, searchFactoryImplementor, sessionFactory, objectLoadingThreads, collectionLoadingThreads, cacheMode, objectLoadingBatchSize, objectsLimit, optimizeAtEnd, purgeAtStart, optimizeAfterPurge, monitor, writerThreads);
    }

    public MassIndexer limitIndexedObjectsTo(long maximum) {
        this.objectsLimit = maximum;
        return this;
    }
}
