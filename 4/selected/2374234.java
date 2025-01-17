package org.archive.crawler.frontier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.crawler.util.BloomUriUniqFilter;
import org.archive.crawler.util.CheckpointUtils;
import org.archive.crawler.util.DiskFPMergeUriUniqFilter;
import org.archive.crawler.util.MemFPMergeUriUniqFilter;
import org.archive.util.ArchiveUtils;
import com.sleepycat.je.DatabaseException;

/**
 * A Frontier using several BerkeleyDB JE Databases to hold its record of
 * known hosts (queues), and pending URIs. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends WorkQueueFrontier implements Serializable {

    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(BdbFrontier.class, 1);

    private static final Logger logger = Logger.getLogger(BdbFrontier.class.getName());

    /** all URIs scheduled to be crawled */
    protected transient BdbMultipleWorkQueues pendingUris;

    /** all URI-already-included options available to be chosen */
    private String[] AVAILABLE_INCLUDED_OPTIONS = new String[] { BdbUriUniqFilter.class.getName(), BloomUriUniqFilter.class.getName(), MemFPMergeUriUniqFilter.class.getName(), DiskFPMergeUriUniqFilter.class.getName() };

    /** URI-already-included to use (by class name) */
    public static final String ATTR_INCLUDED = "uri-included-structure";

    private static final String DEFAULT_INCLUDED = BdbUriUniqFilter.class.getName();

    /**
     * Constructor.
     * @param name Name for of this Frontier.
     */
    public BdbFrontier(String name) {
        this(name, "BdbFrontier. " + "A Frontier using BerkeleyDB Java Edition databases for " + "persistence to disk.");
        Type t = addElementToDefinition(new SimpleType(ATTR_INCLUDED, "Structure to use for tracking already-seen URIs. Non-default " + "options may require additional configuration via system " + "properties.", DEFAULT_INCLUDED, AVAILABLE_INCLUDED_OPTIONS));
        t.setExpertSetting(true);
    }

    /**
     * Create the BdbFrontier
     * 
     * @param name
     * @param description
     */
    public BdbFrontier(String name, String description) {
        super(name, description);
    }

    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultipleWorkQueues
     * @throws DatabaseException
     */
    private BdbMultipleWorkQueues createMultipleWorkQueues() throws DatabaseException {
        return new BdbMultipleWorkQueues(this.controller.getBdbEnvironment(), this.controller.getBdbEnvironment().getClassCatalog(), this.controller.isCheckpointRecover());
    }

    /**
     * Create a UriUniqFilter that will serve as record 
     * of already seen URIs.
     *
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws IOException
     */
    protected UriUniqFilter createAlreadyIncluded() throws IOException {
        UriUniqFilter uuf;
        String c = null;
        try {
            c = (String) getAttribute(null, ATTR_INCLUDED);
        } catch (AttributeNotFoundException e) {
        }
        if (c != null && c.equals(BloomUriUniqFilter.class.getName())) {
            uuf = this.controller.isCheckpointRecover() ? deserializeAlreadySeen(BloomUriUniqFilter.class, this.controller.getCheckpointRecover().getDirectory()) : new BloomUriUniqFilter();
        } else if (c != null && c.equals(MemFPMergeUriUniqFilter.class.getName())) {
            uuf = new MemFPMergeUriUniqFilter();
        } else if (c != null && c.equals(DiskFPMergeUriUniqFilter.class.getName())) {
            uuf = new DiskFPMergeUriUniqFilter(controller.getScratchDisk());
        } else {
            uuf = this.controller.isCheckpointRecover() ? deserializeAlreadySeen(BdbUriUniqFilter.class, this.controller.getCheckpointRecover().getDirectory()) : new BdbUriUniqFilter(this.controller.getBdbEnvironment());
            if (this.controller.isCheckpointRecover()) {
                try {
                    ((BdbUriUniqFilter) uuf).reopen(this.controller.getBdbEnvironment());
                } catch (DatabaseException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        uuf.setDestination(this);
        return uuf;
    }

    protected UriUniqFilter deserializeAlreadySeen(final Class<? extends UriUniqFilter> cls, final File dir) throws FileNotFoundException, IOException {
        UriUniqFilter uuf = null;
        try {
            logger.fine("Started deserializing " + cls.getName() + " of checkpoint recover.");
            uuf = CheckpointUtils.readObjectFromFile(cls, dir);
            logger.fine("Finished deserializing bdbje as part " + "of checkpoint recover.");
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize " + cls.getName() + ": " + e.getMessage());
        }
        return uuf;
    }

    /**
     * Return the work queue for the given CrawlURI's classKey. URIs
     * are ordered and politeness-delayed within their 'class'.
     * 
     * @param curi CrawlURI to base queue on
     * @return the found or created BdbWorkQueue
     */
    protected WorkQueue getQueueFor(CrawlURI curi) {
        WorkQueue wq;
        String classKey = curi.getClassKey();
        synchronized (allQueues) {
            wq = (WorkQueue) allQueues.get(classKey);
            if (wq == null) {
                wq = new BdbWorkQueue(classKey, this);
                wq.setTotalBudget(((Long) getUncheckedAttribute(curi, ATTR_QUEUE_TOTAL_BUDGET)).longValue());
                allQueues.put(classKey, wq);
            }
        }
        return wq;
    }

    /**
     * Return the work queue for the given classKey, or null
     * if no such queue exists.
     * 
     * @param classKey key to look for
     * @return the found WorkQueue
     */
    protected WorkQueue getQueueFor(String classKey) {
        WorkQueue wq;
        synchronized (allQueues) {
            wq = (WorkQueue) allQueues.get(classKey);
        }
        return wq;
    }

    public FrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        return pendingUris.getInitialMarker(regexpr);
    }

    /**
     * Return list of urls.
     * @param marker
     * @param numberOfMatches
     * @param verbose 
     * @return List of URIs (strings).
     */
    public ArrayList<String> getURIsList(FrontierMarker marker, int numberOfMatches, final boolean verbose) {
        List curis;
        try {
            curis = pendingUris.getFrom(marker, numberOfMatches);
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        ArrayList<String> results = new ArrayList<String>(curis.size());
        Iterator iter = curis.iterator();
        while (iter.hasNext()) {
            CrawlURI curi = (CrawlURI) iter.next();
            results.add("[" + curi.getClassKey() + "] " + curi.singleLineReport());
        }
        return results;
    }

    protected void initQueue() throws IOException {
        try {
            this.pendingUris = createMultipleWorkQueues();
        } catch (DatabaseException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    protected void closeQueue() {
        if (this.pendingUris != null) {
            this.pendingUris.close();
            this.pendingUris = null;
        }
    }

    protected BdbMultipleWorkQueues getWorkQueues() {
        return pendingUris;
    }

    protected boolean workQueueDataOnDisk() {
        return true;
    }

    public void initialize(CrawlController c) throws FatalConfigurationException, IOException {
        super.initialize(c);
        if (c.isCheckpointRecover()) {
            BdbFrontier f = null;
            try {
                f = (BdbFrontier) CheckpointUtils.readObjectFromFile(this.getClass(), this.controller.getCheckpointRecover().getDirectory());
            } catch (FileNotFoundException e) {
                throw new FatalConfigurationException("Failed checkpoint " + "recover: " + e.getMessage());
            } catch (IOException e) {
                throw new FatalConfigurationException("Failed checkpoint " + "recover: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new FatalConfigurationException("Failed checkpoint " + "recover: " + e.getMessage());
            }
            this.nextOrdinal = f.nextOrdinal;
            this.totalProcessedBytes = f.totalProcessedBytes;
            this.disregardedUriCount = f.disregardedUriCount;
            this.failedFetchCount = f.failedFetchCount;
            this.processedBytesAfterLastEmittedURI = f.processedBytesAfterLastEmittedURI;
            this.queuedUriCount = f.queuedUriCount;
            this.succeededFetchCount = f.succeededFetchCount;
            this.lastMaxBandwidthKB = f.lastMaxBandwidthKB;
            this.readyClassQueues = f.readyClassQueues;
            this.inactiveQueues = f.inactiveQueues;
            this.retiredQueues = f.retiredQueues;
            this.snoozedClassQueues = f.snoozedClassQueues;
            this.inProcessQueues = f.inProcessQueues;
            wakeQueues();
        }
    }

    public void crawlCheckpoint(File checkpointDir) throws Exception {
        super.crawlCheckpoint(checkpointDir);
        logger.fine("Started serializing already seen as part " + "of checkpoint. Can take some time.");
        if (this.pendingUris != null) {
            this.pendingUris.sync();
        }
        CheckpointUtils.writeObjectToFile(this.alreadyIncluded, checkpointDir);
        logger.fine("Finished serializing already seen as part " + "of checkpoint.");
        CheckpointUtils.writeObjectToFile(this, checkpointDir);
    }
}
