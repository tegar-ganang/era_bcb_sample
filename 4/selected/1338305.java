package com.od.jtimeseries.server.timeseries;

import com.od.jtimeseries.component.util.cache.TimeSeriesCache;
import com.od.jtimeseries.identifiable.Identifiable;
import com.od.jtimeseries.identifiable.IdentifiableBase;
import com.od.jtimeseries.server.serialization.FileHeader;
import com.od.jtimeseries.server.serialization.SerializationException;
import com.od.jtimeseries.server.serialization.TimeSeriesSerializer;
import com.od.jtimeseries.timeseries.*;
import com.od.jtimeseries.timeseries.impl.ProxyTimeSeriesEventHandler;
import com.od.jtimeseries.timeseries.impl.RoundRobinTimeSeries;
import com.od.jtimeseries.util.NamedExecutors;
import com.od.jtimeseries.util.TimeSeriesExecutorFactory;
import com.od.jtimeseries.util.logging.LogMethods;
import com.od.jtimeseries.util.logging.LogUtils;
import com.od.jtimeseries.util.time.TimePeriod;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 18-May-2009
 * Time: 13:10:01
 *
 * A series which represents time series data stored on disk.
 * The actual serialization is carried out by RoundRobinSerializer
 *
 * TimeSeriesItem appended using s.addItem() can be stored in a local cache, and a delayed write performed, to avoid having
 * to keep all the data for the timeseries in memory at all times. It is expected that the number of appends will vastly
 * outnumber all other operations. Other operations (e.g. iterator) in general require the whole time series to be deserialized,
 * which is expensive, and are to be avoided where possible.
 *
 * TODO
 * It would almost certainly be possible to improve the local WriteBehindCache to hold inserts and removes as well as appends
 * so that the whole series to be deserialized to support these operations,
 * but I've left this for another day, I'm really expecting appends only at present
 */
public class FilesystemTimeSeries extends IdentifiableBase implements IdentifiableTimeSeries, IndexedTimeSeries {

    private static final LogMethods logMethods = LogUtils.getLogMethods(FilesystemTimeSeries.class);

    private static ScheduledExecutorService clearCacheExecutor = NamedExecutors.newSingleThreadScheduledExecutor("FilesystemTimeSeriesClearCache");

    private Executor eventExecutor = TimeSeriesExecutorFactory.getExecutorForTimeSeriesEvents(this);

    private TimeSeriesSerializer timeseriesSerializer;

    private TimePeriod appendPeriod;

    private TimePeriod rewritePeriod;

    private FileHeader fileHeader;

    private TimeSeriesCache<Identifiable, RoundRobinTimeSeries> timeSeriesCache;

    private ProxyTimeSeriesEventHandler timeSeriesEventHandler = new LocalModCountProxyTimeSeriesEventHandler(this);

    private WriteBehindCache writeBehindCache;

    private volatile long lastTimestamp = -1;

    private volatile TimeSeriesItem lastItem;

    private ScheduledFuture nextFlushTask;

    private volatile long modCount;

    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     *  Create a FilesystemTimeSeries for a series which already exists on disk, passing in the FileHeader, which must have been updated to match the latest state of the file
     */
    public FilesystemTimeSeries(FileHeader fileHeader, TimeSeriesSerializer timeseriesSerializer, TimeSeriesCache<Identifiable, RoundRobinTimeSeries> timeSeriesCache, TimePeriod appendPeriod, TimePeriod rewritePeriod) throws SerializationException {
        super(fileHeader.getId(), fileHeader.getDescription());
        this.fileHeader = fileHeader;
        this.timeSeriesCache = timeSeriesCache;
        setFields(timeseriesSerializer, appendPeriod, rewritePeriod, fileHeader);
    }

    /**
     *  Create a FilesystemTimeSeries for a series
     *  The series may more may not already exist on disk, if it does exist a FileHeader will be created and synced with existing series, of not a new series file will be
     *  created
     */
    public FilesystemTimeSeries(String parentPath, String id, String description, TimeSeriesSerializer timeseriesSerializer, TimeSeriesCache<Identifiable, RoundRobinTimeSeries> timeSeriesCache, int seriesLength, TimePeriod appendPeriod, TimePeriod rewritePeriod) throws SerializationException {
        super(id, description);
        this.timeSeriesCache = timeSeriesCache;
        this.fileHeader = createFileHeader(timeseriesSerializer, parentPath, seriesLength);
        setFields(timeseriesSerializer, appendPeriod, rewritePeriod, fileHeader);
    }

    private void setFields(TimeSeriesSerializer timeseriesSerializer, TimePeriod appendPeriod, TimePeriod rewritePeriod, FileHeader fileHeader) {
        this.timeseriesSerializer = timeseriesSerializer;
        this.appendPeriod = appendPeriod;
        this.rewritePeriod = rewritePeriod;
        this.lastTimestamp = fileHeader.getMostRecentItemTimestamp();
        this.writeBehindCache = new WriteBehindCache();
    }

    private FileHeader createFileHeader(TimeSeriesSerializer timeseriesSerializer, String parentPath, int seriesLength) throws SerializationException {
        FileHeader fileHeader = new FileHeader(parentPath + Identifiable.NAMESPACE_SEPARATOR + getId(), getDescription(), seriesLength);
        if (timeseriesSerializer.fileExists(fileHeader)) {
            timeseriesSerializer.readHeader(fileHeader);
        } else {
            timeseriesSerializer.createFile(fileHeader);
        }
        return fileHeader;
    }

    /**
     * Note, appending data does not require deserialization of the filesystem timeseries, thus preventing the
     * peformance overhead of having to deserialize to add items. If the series is not already in memory we will append
     * the item to our write behind cache only (eventually the item will get persisted, depending on the cache flush
     * scheduling)
     *
     * Inserting items is much more expensive, since we have to deserialize to do that
     */
    public void addItem(TimeSeriesItem i) {
        try {
            this.writeLock().lock();
            boolean result = doAppend(i);
            if (!result) {
                RoundRobinTimeSeries r = getRoundRobinSeries();
                r.addItem(i);
                writeBehindCache.cacheSeriesForRewrite(r);
            }
        } finally {
            this.writeLock().unlock();
        }
    }

    public void addAll(Iterable<TimeSeriesItem> items) {
        try {
            this.writeLock().lock();
            for (TimeSeriesItem i : items) {
                addItem(i);
            }
        } finally {
            this.writeLock().unlock();
        }
    }

    private boolean doAppend(final TimeSeriesItem i) {
        boolean result = false;
        if (i.getTimestamp() >= lastTimestamp) {
            writeBehindCache.addItemForAppend(i);
            result = true;
            RoundRobinTimeSeries s = getRoundRobinSeries(false);
            if (s != null) {
                s.addItem(i);
            } else {
                fireAddEvent(i);
            }
            lastTimestamp = i.getTimestamp();
            lastItem = i;
        }
        return result;
    }

    private void fireAddEvent(final TimeSeriesItem i) {
        final TimeSeriesEvent e = TimeSeriesEvent.createItemsAppendedOrInsertedEvent(FilesystemTimeSeries.this, Collections.singletonList(i), ++modCount, true);
        eventExecutor.execute(new Runnable() {

            public void run() {
                timeSeriesEventHandler.fireItemsAddedOrInserted(e);
            }
        });
    }

    public int getMaxSize() {
        return fileHeader.getSeriesMaxLength();
    }

    public List<TimeSeriesItem> getSnapshot() {
        try {
            this.readLock().lock();
            return getRoundRobinSeries().getSnapshot();
        } finally {
            this.readLock().unlock();
        }
    }

    public TimeSeriesItem getLatestItem() {
        try {
            this.readLock().lock();
            return lastItem != null ? lastItem : getRoundRobinSeries().getLatestItem();
        } finally {
            this.readLock().unlock();
        }
    }

    /**
     * @return the latest timestamp in the series, without causing a load from the filesystem
     */
    public long getLatestTimestamp() {
        return lastTimestamp;
    }

    public long getEarliestTimestamp() {
        try {
            this.readLock().lock();
            return getRoundRobinSeries().getEarliestTimestamp();
        } finally {
            this.readLock().unlock();
        }
    }

    public TimeSeriesItem getEarliestItem() {
        try {
            this.readLock().lock();
            return getRoundRobinSeries().getEarliestItem();
        } finally {
            this.readLock().unlock();
        }
    }

    public int size() {
        try {
            this.readLock().lock();
            RoundRobinTimeSeries r = getRoundRobinSeries(false);
            if (r != null) {
                return r.size();
            } else {
                return Math.min(getMaxSize(), fileHeader.getCurrentSeriesSize() + writeBehindCache.getAppendItems().size());
            }
        } finally {
            this.readLock().unlock();
        }
    }

    public TimeSeriesItem getItem(int index) {
        try {
            this.readLock().lock();
            return getRoundRobinSeries().getItem(index);
        } finally {
            this.readLock().unlock();
        }
    }

    public void clear() {
        try {
            this.writeLock().lock();
            RoundRobinTimeSeries r = getRoundRobinSeries();
            r.clear();
            lastTimestamp = -1;
            writeBehindCache.cacheSeriesForRewrite(r);
        } finally {
            this.writeLock().unlock();
        }
    }

    public boolean removeItem(TimeSeriesItem item) {
        try {
            this.writeLock().lock();
            RoundRobinTimeSeries r = getRoundRobinSeries();
            boolean change = r.removeItem(item);
            if (change) {
                writeBehindCache.cacheSeriesForRewrite(r);
            }
            lastTimestamp = r.getLatestTimestamp();
            return change;
        } finally {
            this.writeLock().unlock();
        }
    }

    public void removeAll(Iterable<TimeSeriesItem> items) {
        try {
            this.writeLock().lock();
            getRoundRobinSeries().removeAll(items);
        } finally {
            this.writeLock().unlock();
        }
    }

    public Iterator<TimeSeriesItem> iterator() {
        try {
            this.readLock().lock();
            return getRoundRobinSeries().iterator();
        } finally {
            this.readLock().unlock();
        }
    }

    public Iterable<TimeSeriesItem> unsafeIterable() {
        try {
            this.readLock().lock();
            return getRoundRobinSeries().unsafeIterable();
        } finally {
            this.readLock().unlock();
        }
    }

    /**
     * Intentionally break the contract of List.equals() - we shouldn't need to support logical equality of
     * FilesystemTimeSeries as a List of items - to do so would require us to deserialize from disk, which would be a bad idea
     */
    public boolean equals(Object o) {
        return o == this;
    }

    /**
     * Intentionally break the contract of List.hashCode() - we shouldn't need to support logical equality of
     * FilesystemTimeSeries as a List of items - to do so would require us to deserialize from disk, which would be a bad idea
     */
    public int hashCode() {
        return getId().hashCode();
    }

    public long getModCount() {
        return modCount;
    }

    public TimeSeriesItem getFirstItemAtOrBefore(long timestamp) {
        return getRoundRobinSeries().getFirstItemAtOrBefore(timestamp);
    }

    public TimeSeriesItem getFirstItemAtOrAfter(long timestamp) {
        return getRoundRobinSeries().getFirstItemAtOrAfter(timestamp);
    }

    public List<TimeSeriesItem> getItemsInRange(long startTime, long endTime) {
        return getRoundRobinSeries().getItemsInRange(startTime, endTime);
    }

    public String setProperty_Locked(String key, String value) {
        return fileHeader.setSeriesProperty(key, value);
    }

    public String removeProperty_Locked(String key) {
        return fileHeader.removeSeriesProperty(key);
    }

    public String getProperty_Locked(String key) {
        return fileHeader.getSeriesProperty(key);
    }

    public Properties getProperties_Locked() {
        return fileHeader.getSeriesProperties();
    }

    public FileHeader getFileHeader() {
        return fileHeader;
    }

    public void addTimeSeriesListener(TimeSeriesListener l) {
        timeSeriesEventHandler.addTimeSeriesListener(l);
    }

    public void removeTimeSeriesListener(TimeSeriesListener l) {
        timeSeriesEventHandler.removeTimeSeriesListener(l);
    }

    protected RoundRobinTimeSeries getRoundRobinSeries() {
        return getRoundRobinSeries(true);
    }

    private RoundRobinTimeSeries getRoundRobinSeries(boolean deserializeIfRequired) {
        RoundRobinTimeSeries s = isSeriesInWriteCache() ? writeBehindCache.getSeries() : timeSeriesCache.get(this);
        if (s == null && deserializeIfRequired) {
            try {
                s = timeseriesSerializer.readSeries(fileHeader);
                s.addAllWithoutFiringEvents(writeBehindCache.getAppendItems().getSnapshot());
                s.addTimeSeriesListener(timeSeriesEventHandler);
                timeSeriesCache.put(this, s);
            } catch (SerializationException e) {
                throw new RuntimeException("Could not load timeseries values", e);
            }
        }
        return s;
    }

    /**
     * WriteBehindCache will hold values only if the local series has changed from the version held on disk
     * Mutator methods on the FilesystemTimeSeries call methods on the WriteBehindCache when the state changes.
     *
     * The write behind cache can store either be the most recently appended items or a strong reference to the whole round robin series if there were any changes apart from appends.
     * In either case, the main softReference series held by FileSystemTimeSeries should always be kept fully up to date, if it exists
     * - the cache just represents what we need to write to the filesystem to bring the persisted version up to date.
     *
     * In the case we're holding onto a list of appended items, the soft referenced wrapped series is free to be
     * reclaimed (the append list contains all the deltas in this case - we have no need
     * to rewrite the whole series, and there's no urgency to flush the cache.)
     *
     * If other changes to the wrapped series have been made (not just appends) we hold a reference
     * to the whole series to prevent it being collected, and add a task to try to bring forward the
     * cache flush operation.
     */
    private class WriteBehindCache {

        private RoundRobinTimeSeries roundRobinSeries;

        private RoundRobinTimeSeries itemsToAppend = new RoundRobinTimeSeries(getMaxSize());

        public void cacheSeriesForRewrite(RoundRobinTimeSeries roundRobinSeries) {
            this.roundRobinSeries = roundRobinSeries;
            itemsToAppend.clear();
            scheduleFlushCacheTask(rewritePeriod.getLengthInMillis());
        }

        public void addItemForAppend(TimeSeriesItem timeSeriesItem) {
            if (roundRobinSeries == null) {
                itemsToAppend.addItem(timeSeriesItem);
                scheduleFlushCacheTask(appendPeriod.getLengthInMillis());
            }
        }

        public RoundRobinTimeSeries getAppendItems() {
            return itemsToAppend;
        }

        public void flush() {
            try {
                FilesystemTimeSeries.this.writeLock().lock();
                if (isFlushRequired()) {
                    doFlush();
                }
            } finally {
                FilesystemTimeSeries.this.writeLock().unlock();
            }
        }

        private void doFlush() {
            try {
                if (roundRobinSeries != null) {
                    timeseriesSerializer.writeSeries(fileHeader, roundRobinSeries);
                } else {
                    timeseriesSerializer.appendToSeries(fileHeader, itemsToAppend);
                }
                clearCache();
            } catch (Throwable t) {
                logMethods.error("Failed to write to timeseries file " + fileHeader + ", cannot bring this series up to date, I'll keep trying", t);
                scheduleFlushCacheTask(appendPeriod.getLengthInMillis());
            }
        }

        private void clearCache() {
            roundRobinSeries = null;
            itemsToAppend.clear();
        }

        private boolean isFlushRequired() {
            return roundRobinSeries != null || itemsToAppend.size() > 0 || fileHeader.isPropertiesRewriteRequired();
        }

        private boolean isSeriesInCache() {
            return roundRobinSeries != null;
        }

        private int getAppendListSize() {
            return itemsToAppend.size();
        }

        public RoundRobinTimeSeries getSeries() {
            return roundRobinSeries;
        }

        private void scheduleFlushCacheTask(long delayMillis) {
            if (nextFlushTask == null || nextFlushTask.isDone()) {
                scheduleNewTask(delayMillis);
            } else if (nextFlushTask.getDelay(TimeUnit.MILLISECONDS) > delayMillis) {
                nextFlushTask.cancel(false);
                scheduleNewTask(delayMillis);
            }
        }

        private void scheduleNewTask(long delayMillis) {
            nextFlushTask = clearCacheExecutor.schedule(new Runnable() {

                public void run() {
                    writeBehindCache.flush();
                }
            }, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Queue for a rewrite, will be header only unless there are other appends/changes
     */
    public void queueHeaderRewrite() {
        try {
            readWriteLock.writeLock().lock();
            writeBehindCache.scheduleFlushCacheTask(appendPeriod.getLengthInMillis());
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Write all in-memory changes to the FilesystemTimeSeries to disk
     */
    public void flush() {
        writeBehindCache.flush();
    }

    /**
     * @return true, if the series has in-memory changes
     */
    public boolean isFlushRequired() {
        return writeBehindCache.isFlushRequired();
    }

    boolean isSeriesInWriteCache() {
        return writeBehindCache.isSeriesInCache();
    }

    public boolean isLastItemInMemory() {
        return lastItem != null;
    }

    int getCacheAppendListSize() {
        try {
            readWriteLock.readLock().lock();
            return writeBehindCache.getAppendListSize();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    void triggerGarbageCollection() {
        try {
            readWriteLock.writeLock().lock();
            timeSeriesCache.remove(this);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    boolean isSeriesCollected() {
        try {
            readWriteLock.readLock().lock();
            return timeSeriesCache.get(this) == null;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private class LocalModCountProxyTimeSeriesEventHandler extends ProxyTimeSeriesEventHandler {

        public LocalModCountProxyTimeSeriesEventHandler(Object proxySource) {
            super(proxySource);
        }

        public void itemsAddedOrInserted(TimeSeriesEvent h) {
            TimeSeriesEvent e = TimeSeriesEvent.createEvent(FilesystemTimeSeries.this, h.getItems(), h.getEventType(), ++modCount);
            fireItemsAddedOrInserted(e);
        }

        public void itemsRemoved(TimeSeriesEvent h) {
            TimeSeriesEvent e = TimeSeriesEvent.createEvent(FilesystemTimeSeries.this, h.getItems(), h.getEventType(), ++modCount);
            fireItemsRemoved(e);
        }

        public void seriesChanged(TimeSeriesEvent h) {
            TimeSeriesEvent e = TimeSeriesEvent.createEvent(FilesystemTimeSeries.this, h.getItems(), h.getEventType(), ++modCount);
            fireSeriesChanged(e);
        }
    }

    public Lock writeLock() {
        return readWriteLock.writeLock();
    }

    public Lock readLock() {
        return readWriteLock.readLock();
    }
}
