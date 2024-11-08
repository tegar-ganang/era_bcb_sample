package org.hibernate.search.backend.impl.batchlucene;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkDelegate;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.LoggerFactory;

/**
 * Collects all resources needed to apply changes to one index.
 * They are reused across the processing of all LuceneWork.
 * 
 * !! Be careful to ensure the IndexWriter is eventually closed,
 * or the index will stay locked.
 * @see close();
 *
 * @author Sanne Grinovero
 */
class DirectoryProviderWorkspace {

    private static final Logger log = LoggerFactory.make();

    private final ExecutorService executor;

    private final LuceneWorkVisitor visitor;

    private final Workspace workspace;

    private final MassIndexerProgressMonitor monitor;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    DirectoryProviderWorkspace(WorkerBuildContext context, DirectoryProvider<?> dp, MassIndexerProgressMonitor monitor, int maxThreads, ErrorHandler errorHandler) {
        if (maxThreads < 1) {
            throw new IllegalArgumentException("maxThreads needs to be at least 1");
        }
        this.monitor = monitor;
        workspace = new Workspace(context, dp, errorHandler);
        visitor = new LuceneWorkVisitor(workspace, context);
        executor = Executors.newFixedThreadPool(maxThreads, "indexwriter");
    }

    /**
	 * Notify the indexwriting threads that they should quit at the end of the enqueued
	 * tasks. Waits for the end of the current queue, then commits changes.
	 * @throws InterruptedException
	 */
    public void stopAndFlush(long timeout, TimeUnit unit) throws InterruptedException {
        checkIsNotClosed();
        executor.shutdown();
        executor.awaitTermination(timeout, unit);
        workspace.commitIndexWriter();
    }

    /**
	 * Used to do some tasks at the beginning and/or at the end of the main batch
	 * operations. This work is not done async.
	 * @param work
	 */
    public void doWorkInSync(LuceneWork work) {
        checkIsNotClosed();
        LuceneWorkDelegate delegate = work.getWorkDelegate(visitor);
        delegate.performWork(work, workspace.getIndexWriter(true));
        delegate.logWorkDone(work, monitor);
    }

    public void enqueueAsyncWork(LuceneWork work) {
        executor.execute(new AsyncIndexRunnable(work));
    }

    /**
	 * Makes sure the executor is closed and closes the IndexWriter.
	 */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                if (!executor.isShutdown()) {
                    log.error("Terminating batch work! Index might end up in inconsistent state.");
                    executor.shutdownNow();
                }
            } finally {
                workspace.closeIndexWriter();
            }
        } else {
            checkIsNotClosed();
        }
    }

    /**
	 * Verifies this is not closed yet, or throws an exception.
	 */
    private void checkIsNotClosed() {
        if (closed.get()) {
            throw new SearchException("Batch DirectoryProviderWorkspace is closed already");
        }
    }

    private class AsyncIndexRunnable implements Runnable {

        private final LuceneWork work;

        AsyncIndexRunnable(LuceneWork work) {
            this.work = work;
        }

        public void run() {
            doWorkInSync(work);
        }
    }
}
