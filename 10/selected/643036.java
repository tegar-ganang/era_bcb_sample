package de.sonivis.tool.core;

import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.GenericJDBCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.sonivis.tool.core.datamodel.exceptions.CannotConnectToDatabaseException;
import de.sonivis.tool.core.datamodel.extension.proxy.IRevisionElement;
import de.sonivis.tool.core.datamodel.proxy.IActor;
import de.sonivis.tool.core.datamodel.proxy.IActorContentElementRelation;
import de.sonivis.tool.core.datamodel.proxy.IBasicItem;
import de.sonivis.tool.core.datamodel.proxy.IContentElement;
import de.sonivis.tool.core.datamodel.proxy.IContextRelation;
import de.sonivis.tool.core.datamodel.proxy.IInfoSpaceItem;
import de.sonivis.tool.core.datamodel.proxy.IInteractionRelation;

/**
 * This is a class to
 * 
 * @author benedikt
 * @version $Revision: 1578 $, $Date: 2010-03-16 15:29:16 -0400 (Tue, 16 Mar 2010) $
 */
public class ThreadedInfoSpaceItemSaveManager {

    /**
	 * Class logging.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedInfoSpaceItemSaveManager.class);

    /**
	 * The singleton instance.
	 */
    private static ThreadedInfoSpaceItemSaveManager INSTANCE;

    /**
	 * Internal queue of items to be stored.
	 */
    private final ConcurrentLinkedQueue<IInfoSpaceItem> saveItems = new ConcurrentLinkedQueue<IInfoSpaceItem>();

    /**
	 * Number of items to be inserted by {@link SaveJob#batchSaveOrUpdate(Collection)} before
	 * threading/flushing.
	 */
    private static final int BATCH_SIZE = 20000;

    /**
	 * Size of the internal storage queue.
	 */
    private static int saveItemSize = 0;

    private static final int numberOfThreads = 1;

    private static long itemsToBeStoredCount = 0L;

    private static long itemsStoredCount = 0L;

    /**
	 * The exclusive access
	 */
    private static final Semaphore jobSemaphore = new Semaphore(numberOfThreads);

    /**
	 * Under cover constructor.
	 */
    private ThreadedInfoSpaceItemSaveManager() {
        itemsToBeStoredCount = 0L;
        itemsStoredCount = 0L;
    }

    /**
	 * Get singleton instance.
	 * 
	 * @return the one and only instance of the manager.
	 */
    public static final synchronized ThreadedInfoSpaceItemSaveManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ThreadedInfoSpaceItemSaveManager();
        }
        return INSTANCE;
    }

    /**
	 * Queue item for being stored.
	 * <p>
	 * Adds the specified <em>item</em> to the internal storage queue. If the queue has reached a
	 * certain size limit its contents will be flushed to the persistence store.
	 * </p>
	 * 
	 * @param item
	 *            The {@link IInfoSpaceItem} entity to be stored.
	 * @throws InterruptedException
	 *             if {@link Semaphore#acquire()} does
	 */
    public void saveItem(final IInfoSpaceItem item) throws InterruptedException {
        if (item == null) {
            return;
        }
        saveItems.add(item);
        saveItemSize++;
        itemsToBeStoredCount++;
        if (saveItemSize >= BATCH_SIZE) {
            jobSemaphore.acquire();
            final SaveJob saveJob = new SaveJob("SaveJob" + jobSemaphore.availablePermits());
            saveJob.schedule();
        }
    }

    /**
	 * To be called from the user when no more items are to be passed for storage.
	 * <p>
	 * A call will have the remaining items of the internal storage queue being flushed to the
	 * persistence store and the singleton instance of the manager is then finally disposed.
	 * </p>
	 * 
	 * @throws InterruptedException
	 *             if {@link Semaphore#acquire()} does
	 */
    public void dispose() throws InterruptedException {
        if (!saveItems.isEmpty()) {
            jobSemaphore.acquire();
            final SaveJob saveJob = new SaveJob("SaveJob" + jobSemaphore.availablePermits());
            saveJob.schedule();
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("ThreadedInfoSpaceItemSaveManager was requested to store " + itemsToBeStoredCount + " InfoSpaceItem entities.");
            LOGGER.info(itemsStoredCount + " of these could successfully be stored.");
        }
        INSTANCE = null;
    }

    /**
	 * Indicates currently pending or executing jobs.
	 * 
	 * @return <code>true</code> if there are currently active jobs, <code>false</code> otherwise.
	 */
    public boolean isBusy() {
        if (jobSemaphore.availablePermits() < numberOfThreads) {
            return true;
        }
        return false;
    }

    /**
	 * Groups the activity of storing a certain amount of items in the persistence store.
	 * 
	 * @author benedikt
	 * @version $Revision: 1578 $, $Date: 2010-03-16 15:29:16 -0400 (Tue, 16 Mar 2010) $
	 */
    private class SaveJob extends Job {

        /**
		 * Internal container for items to be saved.
		 */
        private final List<IInfoSpaceItem> items = new ArrayList<IInfoSpaceItem>();

        /**
		 * Helper flag for error handling.
		 */
        private boolean saveItemSplitted = false;

        /**
		 * Default constructor.
		 * 
		 * @param name
		 *            Name to be referred to for this {@link Job}.
		 */
        public SaveJob(final String name) {
            super(name);
            this.saveItemSplitted = false;
            for (int i = 0; i <= BATCH_SIZE; i++) {
                final IInfoSpaceItem item = saveItems.poll();
                saveItemSize--;
                if (item != null) {
                    items.add(item);
                } else {
                    continue;
                }
            }
        }

        /**
		 * {@inheritDoc}
		 */
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ThreadedInfoSpaceSaveJob starts...");
            }
            try {
                this.batchSaveOrUpdate(items);
            } catch (final CannotConnectToDatabaseException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Could not insert objects, persistence store is not available.", e);
                }
                return new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, "Could not insert objects, persistence store is not available. " + e.getMessage());
            } catch (final HibernateException he) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Could not insert objects.", he);
                }
                return new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, "Could not insert objects. " + he.getMessage());
            } finally {
                jobSemaphore.release();
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("... saved " + items.size() + " items.");
            }
            return Status.OK_STATUS;
        }

        /**
		 * Insert large collections of objects into the persistence layer.
		 * <p>
		 * The {@link Collection} of <i>transient</i> or <i>detached</i> objects is stored in the
		 * persistence layer using {@link Session#saveOrUpdate(Object)} for each. The
		 * {@link Session} will be {@link Session#flush() flushed} and {@link Session#clear()
		 * cleared} when the save operation was executed {@value #BATCH_SIZE} times.
		 * </p>
		 * <p>
		 * Note also, that currently before insertion the foreign key checks are turned off with a
		 * MySQL-specific query. After all items have been inserted (and flushed) the checks are
		 * turned on again. Therefore, this method might cause problems when using with other DBMS.
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
        @SuppressWarnings("unchecked")
        private final void batchSaveOrUpdate(final Collection<IInfoSpaceItem> objects) throws CannotConnectToDatabaseException {
            if (objects != null && !objects.isEmpty()) {
                final Session s = ModelManager.getInstance().getCurrentSession();
                Transaction tx = null;
                try {
                    tx = s.beginTransaction();
                    final String oldCacheMode = s.getCacheMode().toString();
                    if (s.getCacheMode() != CacheMode.IGNORE) {
                        s.setCacheMode(CacheMode.IGNORE);
                    }
                    s.createSQLQuery("SET foreign_key_checks=0;").executeUpdate();
                    int itemCount = 0;
                    float revCount = 0;
                    for (final IInfoSpaceItem obj : objects) {
                        if (obj instanceof IRevisionElement) {
                            revCount++;
                        }
                        s.saveOrUpdate(obj);
                        if (++itemCount % BATCH_SIZE == 0) {
                            s.flush();
                            s.clear();
                        }
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Fraction of RevisionElement entities: " + ((revCount * 100) / BATCH_SIZE) + "%");
                    }
                    s.createSQLQuery("SET foreign_key_checks=1;").executeUpdate();
                    s.setCacheMode(CacheMode.parse(oldCacheMode));
                    s.clear();
                    tx.commit();
                    itemsStoredCount += objects.size();
                } catch (final HibernateException he) {
                    if (tx == null) {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("Failed to create transaction.");
                        }
                        throw he;
                    } else {
                        tx.rollback();
                        String exceptionType = null;
                        String problemType = null;
                        if (he instanceof PropertyValueException) {
                            exceptionType = "PropertyValueException";
                        } else if (he instanceof GenericJDBCException) {
                            exceptionType = "GenericJDBCException";
                            final GenericJDBCException gje = (GenericJDBCException) he;
                            if (gje.getErrorCode() == 1366) {
                                final String msg = gje.getMessage();
                                if (msg != null) {
                                    problemType = msg.substring(msg.indexOf("[") + 1, msg.indexOf("]"));
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("ProblemType is: " + problemType);
                                    }
                                }
                            }
                        } else if (he.getCause() instanceof BatchUpdateException) {
                            exceptionType = "BatchUpdateException";
                        } else {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error("Failed to batch insert " + objects.size() + " items.");
                            }
                            throw he;
                        }
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Handling " + exceptionType);
                        }
                        if (s != null && s.isOpen()) {
                            s.close();
                        }
                        final int size = objects.size();
                        if (size > 1 && !this.saveItemSplitted) {
                            final List<IInfoSpaceItem> basicItems = new ArrayList<IInfoSpaceItem>();
                            final List<IInfoSpaceItem> skippedItems = new ArrayList<IInfoSpaceItem>();
                            final List<IInfoSpaceItem> fundamentalRelations = new ArrayList<IInfoSpaceItem>();
                            entityLoop: for (final IInfoSpaceItem obj : objects) {
                                if (obj instanceof IBasicItem) {
                                    if (exceptionType.equals("GenericJDBCException")) {
                                        if (obj.getClass().getCanonicalName().equals(problemType)) {
                                            String str = null;
                                            if (obj instanceof IContentElement) {
                                                str = ((IContentElement) obj).getTitle();
                                            } else {
                                                str = ((IActor) obj).getName();
                                            }
                                            for (int i = 0; i < str.length(); i++) {
                                                if (str.charAt(i) >= 0x10000) {
                                                    if (LOGGER.isInfoEnabled()) {
                                                        LOGGER.info("Found disallowed character in title / name of entity. Skipping item.");
                                                    }
                                                    skippedItems.add(obj);
                                                    continue entityLoop;
                                                }
                                            }
                                        }
                                    }
                                    basicItems.add(obj);
                                } else {
                                    fundamentalRelations.add(obj);
                                }
                            }
                            this.saveItemSplitted = true;
                            if (!skippedItems.isEmpty() && !fundamentalRelations.isEmpty()) {
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info("Checking relational entities for skipped items.");
                                }
                                final Iterator<IInfoSpaceItem> iter = fundamentalRelations.iterator();
                                while (iter.hasNext()) {
                                    final IInfoSpaceItem isit = iter.next();
                                    if (isit instanceof IActorContentElementRelation) {
                                        final IActorContentElementRelation acer = (IActorContentElementRelation) isit;
                                        if (skippedItems.contains(acer.getActor()) || skippedItems.contains(acer.getContentElement())) {
                                            if (LOGGER.isInfoEnabled()) {
                                                LOGGER.info("Skipping entity of type " + isit.getType().getCanonicalName());
                                            }
                                            iter.remove();
                                        }
                                    } else if (isit instanceof IContextRelation) {
                                        final IContextRelation ctx = (IContextRelation) isit;
                                        if (skippedItems.contains(ctx.getSource()) || skippedItems.contains(ctx.getTarget())) {
                                            if (LOGGER.isInfoEnabled()) {
                                                LOGGER.info("Skipping entity of type " + isit.getType().getCanonicalName());
                                            }
                                            iter.remove();
                                        }
                                    } else {
                                        final IInteractionRelation ia = (IInteractionRelation) isit;
                                        if (skippedItems.contains(ia.getSource()) || skippedItems.contains(ia.getTarget())) {
                                            if (LOGGER.isInfoEnabled()) {
                                                LOGGER.info("Skipping entity of type " + isit.getType().getCanonicalName());
                                            }
                                            iter.remove();
                                        }
                                    }
                                }
                            }
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Retrying to store " + basicItems.size() + " basic items first.");
                            }
                            this.batchSaveOrUpdate(basicItems);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Now storing " + fundamentalRelations.size() + " fundamental relations.");
                            }
                            this.batchSaveOrUpdate(fundamentalRelations);
                        } else if (size > 1) {
                            final int halfSize = size / 2;
                            final List<IInfoSpaceItem> chunk1 = new ArrayList<IInfoSpaceItem>(halfSize);
                            final List<IInfoSpaceItem> chunk2 = new ArrayList<IInfoSpaceItem>(halfSize);
                            int itemCount = 0;
                            boolean firstChunkSuccess = false;
                            for (final IInfoSpaceItem obj : objects) {
                                if (!firstChunkSuccess) {
                                    if (itemCount++ <= halfSize) {
                                        chunk1.add(obj);
                                    } else {
                                        this.batchSaveOrUpdate(chunk1);
                                        if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("Successfully inserted chunk 1 with " + chunk1.size() + " items.");
                                        }
                                        firstChunkSuccess = true;
                                    }
                                } else {
                                    chunk2.add(obj);
                                }
                            }
                            if (!chunk2.isEmpty()) {
                                this.batchSaveOrUpdate(chunk2);
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Successfully inserted chunk 2 with " + chunk2.size() + " items.");
                                }
                            }
                        } else {
                            if (LOGGER.isErrorEnabled()) {
                                if (objects.size() > 1) {
                                    LOGGER.error("There is an item that could not be inserted.");
                                } else {
                                    LOGGER.error("There are items that could not be inserted.");
                                }
                                for (final IInfoSpaceItem obj : objects) {
                                    LOGGER.error("Item of type " + obj.getClass().getSimpleName());
                                    if (obj instanceof IActor) {
                                        LOGGER.error("The name is " + ((IActor) obj).getName());
                                    } else if (obj instanceof IContentElement) {
                                        LOGGER.error("The title is " + ((IContentElement) obj).getTitle());
                                    } else if (obj instanceof IContextRelation) {
                                        final IContextRelation ctx = (IContextRelation) obj;
                                        LOGGER.error("Connects serialID " + ctx.getSource().getSerialId() + " as source and serialID " + ctx.getTarget().getSerialId() + " as target");
                                    } else if (obj instanceof IInteractionRelation) {
                                        final IInteractionRelation ia = (IInteractionRelation) obj;
                                        LOGGER.error("Connects serialID " + ia.getSource().getSerialId() + " as source and serialID " + ia.getTarget().getSerialId() + " as target");
                                    } else if (obj instanceof IActorContentElementRelation) {
                                        final IActorContentElementRelation acer = (IActorContentElementRelation) obj;
                                        LOGGER.error("Connects serialID " + acer.getActor().getSerialId() + " as actor and serialID " + acer.getContentElement().getSerialId() + " as content element");
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    if (s != null && s.isOpen()) {
                        s.close();
                    }
                }
            }
        }
    }
}
