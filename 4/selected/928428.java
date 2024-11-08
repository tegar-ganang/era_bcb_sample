package org.hibernate.search.backend.impl.lucene;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.internals.DirectoryProviderData;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Collects all resources needed to apply changes to one index,
 * and are reused across several WorkQueues.
 *
 * @author Sanne Grinovero
 */
class PerDPResources {

    private static final Logger log = LoggerFactory.make();

    private final ExecutorService executor;

    private final LuceneWorkVisitor visitor;

    private final Workspace workspace;

    private final boolean exclusiveIndexUsage;

    private final ErrorHandler errorHandler;

    PerDPResources(WorkerBuildContext context, DirectoryProvider<?> dp) {
        DirectoryProviderData directoryProviderData = context.getDirectoryProviderData(dp);
        errorHandler = context.getErrorHandler();
        workspace = new Workspace(context, dp, errorHandler);
        visitor = new LuceneWorkVisitor(workspace, context);
        int maxQueueLength = directoryProviderData.getMaxQueueLength();
        executor = Executors.newFixedThreadPool(1, "Directory writer", maxQueueLength);
        exclusiveIndexUsage = directoryProviderData.isExclusiveIndexUsage();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public LuceneWorkVisitor getVisitor() {
        return visitor;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public boolean isExclusiveIndexUsageEnabled() {
        return exclusiveIndexUsage;
    }

    public void shutdown() {
        if (exclusiveIndexUsage) {
            executor.execute(new CloseIndexRunnable(workspace));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Was interrupted while waiting for index activity to finish. Index might be inconsistent or have a stale lock");
        }
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
}
