package net.sourceforge.docfetcher.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.sourceforge.docfetcher.model.index.IndexingQueue;
import net.sourceforge.docfetcher.model.index.file.FileFactory;
import net.sourceforge.docfetcher.model.index.outlook.OutlookMailFactory;
import net.sourceforge.docfetcher.model.search.Searcher;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.CallOnce;
import net.sourceforge.docfetcher.util.annotations.ImmutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.collect.AlphanumComparator;
import net.sourceforge.docfetcher.util.concurrent.BlockingWrapper;
import net.sourceforge.docfetcher.util.concurrent.DelayedExecutor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.util.Version;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.primitives.Longs;

/**
 * @author Tran Nam Quang
 */
@ThreadSafe
public final class IndexRegistry {

    public interface ExistingIndexesHandler {

        public void handleExistingIndexes(@NotNull List<LuceneIndex> indexes);
    }

    @VisibleForPackageGroup
    public static final Version LUCENE_VERSION = Version.LUCENE_30;

    @VisibleForPackageGroup
    public static final Analyzer analyzer = new StandardAnalyzer(LUCENE_VERSION, Collections.EMPTY_SET);

    private static final String SER_FILENAME = "tree-index.ser";

    static {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }

    private final Event<LuceneIndex> evtAdded = new Event<LuceneIndex>();

    private final Event<List<LuceneIndex>> evtRemoved = new Event<List<LuceneIndex>>();

    /**
	 * A map for storing the indexes, along with the last-modified values of the
	 * indexes' ser files. A last-modified value may be null, which indicates
	 * that the corresponding index hasn't been saved yet.
	 */
    private final Map<LuceneIndex, Long> indexes = Maps.newTreeMap(IndexComparator.instance);

    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    private final Lock readLock = lock.readLock();

    private final Lock writeLock = lock.writeLock();

    private final File indexParentDir;

    private final IndexingQueue queue;

    private final HotColdFileCache unpackCache;

    private final FileFactory fileFactory;

    private final OutlookMailFactory outlookMailFactory;

    private final BlockingWrapper<Searcher> searcher = new BlockingWrapper<Searcher>();

    public IndexRegistry(@NotNull File indexParentDir, int cacheSize, int reporterCapacity) {
        Util.checkNotNull(indexParentDir);
        this.indexParentDir = indexParentDir;
        this.unpackCache = new HotColdFileCache(cacheSize);
        this.fileFactory = new FileFactory(unpackCache);
        this.outlookMailFactory = new OutlookMailFactory(unpackCache);
        this.queue = new IndexingQueue(this, reporterCapacity);
    }

    @NotNull
    @ThreadSafe
    public File getIndexParentDir() {
        return indexParentDir;
    }

    @NotNull
    @ThreadSafe
    public IndexingQueue getQueue() {
        return queue;
    }

    @Nullable
    @ThreadSafe
    public Searcher getSearcher() {
        return searcher.get();
    }

    @NotNull
    @ThreadSafe
    public Lock getReadLock() {
        return readLock;
    }

    @NotNull
    @ThreadSafe
    public Lock getWriteLock() {
        return writeLock;
    }

    @ThreadSafe
    @VisibleForPackageGroup
    public void addIndex(@NotNull LuceneIndex index) {
        addIndex(index, null);
    }

    @ThreadSafe
    private void addIndex(@NotNull LuceneIndex index, @Nullable Long lastModified) {
        Util.checkNotNull(index);
        Util.checkNotNull(index.getIndexDirPath());
        writeLock.lock();
        try {
            if (indexes.containsKey(index)) return;
            indexes.put(index, lastModified);
        } finally {
            writeLock.unlock();
        }
        evtAdded.fire(index);
    }

    @ThreadSafe
    public void removeIndexes(@NotNull Collection<LuceneIndex> indexesToRemove, boolean deleteFiles) {
        Util.checkNotNull(indexesToRemove);
        if (indexesToRemove.isEmpty()) return;
        int size = indexesToRemove.size();
        List<LuceneIndex> removed = new ArrayList<LuceneIndex>(size);
        List<PendingDeletion> deletions = deleteFiles ? new ArrayList<PendingDeletion>(size) : null;
        writeLock.lock();
        try {
            for (LuceneIndex index : indexesToRemove) {
                if (!indexes.containsKey(index)) continue;
                indexes.remove(index);
                if (deleteFiles) deletions.add(new PendingDeletion(index));
                removed.add(index);
            }
            if (deletions != null) {
                queue.approveDeletions(deletions);
                searcher.get().approveDeletions(deletions);
            }
        } finally {
            writeLock.unlock();
        }
        evtRemoved.fire(removed);
    }

    @ThreadSafe
    public void addListeners(@NotNull ExistingIndexesHandler handler, @Nullable Event.Listener<LuceneIndex> addedListener, @Nullable Event.Listener<List<LuceneIndex>> removedListener) {
        Util.checkNotNull(handler);
        List<LuceneIndex> indexesCopy;
        writeLock.lock();
        try {
            indexesCopy = ImmutableList.copyOf(indexes.keySet());
            if (addedListener != null) evtAdded.add(addedListener);
            if (removedListener != null) evtRemoved.add(removedListener);
        } finally {
            writeLock.unlock();
        }
        handler.handleExistingIndexes(indexesCopy);
    }

    @ThreadSafe
    public void removeListeners(@Nullable Event.Listener<LuceneIndex> addedListener, @Nullable Event.Listener<List<LuceneIndex>> removedListener) {
        if (addedListener == null && removedListener == null) return;
        writeLock.lock();
        try {
            if (addedListener != null) evtAdded.remove(addedListener);
            if (removedListener != null) evtRemoved.remove(removedListener);
        } finally {
            writeLock.unlock();
        }
    }

    @ImmutableCopy
    @NotNull
    @ThreadSafe
    public List<LuceneIndex> getIndexes() {
        readLock.lock();
        try {
            return ImmutableList.copyOf(indexes.keySet());
        } finally {
            readLock.unlock();
        }
    }

    @CallOnce
    @ThreadSafe
    public void load(@NotNull Cancelable cancelable) throws IOException {
        Util.checkThat(searcher.isNull());
        indexParentDir.mkdirs();
        for (File indexDir : Util.listFiles(indexParentDir)) {
            if (cancelable.isCanceled()) break;
            if (!indexDir.isDirectory()) continue;
            File serFile = new File(indexDir, SER_FILENAME);
            if (!serFile.isFile()) continue;
            loadIndex(serFile);
        }
        searcher.set(new Searcher(this, fileFactory, outlookMailFactory));
        try {
            final DelayedExecutor executor = new DelayedExecutor(1000);
            final int watchId = new SimpleJNotifyListener() {

                protected void handleEvent(File targetFile, EventType eventType) {
                    if (!targetFile.getName().equals(SER_FILENAME)) return;
                    executor.schedule(new Runnable() {

                        public void run() {
                            reload();
                        }
                    });
                }
            }.addWatch(indexParentDir);
            Runtime.getRuntime().addShutdownHook(new Thread() {

                public void run() {
                    try {
                        JNotify.removeWatch(watchId);
                    } catch (JNotifyException e) {
                        Util.printErr(e);
                    }
                }
            });
        } catch (JNotifyException e) {
            Util.printErr(e);
        }
    }

    @ThreadSafe
    private void loadIndex(@NotNull File serFile) {
        ObjectInputStream in = null;
        try {
            FileInputStream fin = new FileInputStream(serFile);
            FileLock lock = fin.getChannel().lock(0, Long.MAX_VALUE, true);
            LuceneIndex index;
            try {
                in = new ObjectInputStream(fin);
                index = (LuceneIndex) in.readObject();
            } finally {
                lock.release();
            }
            addIndex(index, serFile.lastModified());
        } catch (Exception e) {
            Util.printErr(e);
        } finally {
            Closeables.closeQuietly(in);
        }
    }

    private void reload() {
        writeLock.lock();
        try {
            Map<File, LuceneIndex> indexDirMap = Maps.newHashMap();
            for (LuceneIndex index : indexes.keySet()) indexDirMap.put(index.getIndexDirPath().getCanonicalFile(), index);
            for (File indexDir : Util.listFiles(indexParentDir)) {
                if (!indexDir.isDirectory()) continue;
                File serFile = new File(indexDir, SER_FILENAME);
                if (!serFile.isFile()) continue;
                LuceneIndex index = indexDirMap.remove(Util.getAbsFile(indexDir));
                if (index == null) {
                    loadIndex(serFile);
                } else {
                    Long oldLM = indexes.get(index);
                    long newLM = serFile.lastModified();
                    if (oldLM != null && oldLM.longValue() != newLM) {
                        removeIndexes(Collections.singletonList(index), false);
                        loadIndex(serFile);
                    }
                }
            }
            removeIndexes(indexDirMap.values(), false);
        } finally {
            writeLock.unlock();
        }
    }

    @VisibleForPackageGroup
    public void save(@NotNull LuceneIndex index) {
        Util.checkNotNull(index);
        writeLock.lock();
        try {
            File indexDir = index.getIndexDirPath().getCanonicalFile();
            indexDir.mkdirs();
            File serFile = new File(indexDir, SER_FILENAME);
            if (serFile.exists() && !serFile.canWrite()) return;
            ObjectOutputStream out = null;
            try {
                serFile.createNewFile();
                FileOutputStream fout = new FileOutputStream(serFile);
                FileLock lock = fout.getChannel().lock();
                try {
                    out = new ObjectOutputStream(fout);
                    out.writeObject(index);
                } finally {
                    lock.release();
                }
            } catch (IOException e) {
                Util.printErr(e);
            } finally {
                Closeables.closeQuietly(out);
            }
            indexes.put(index, serFile.lastModified());
        } finally {
            writeLock.unlock();
        }
    }

    @NotNull
    @ThreadSafe
    public TreeCheckState getTreeCheckState() {
        List<LuceneIndex> localIndexes = getIndexes();
        TreeCheckState totalState = new TreeCheckState();
        for (LuceneIndex index : localIndexes) totalState.add(index.getTreeCheckState());
        return totalState;
    }

    private static class IndexComparator implements Comparator<LuceneIndex> {

        private static final IndexComparator instance = new IndexComparator();

        public int compare(LuceneIndex o1, LuceneIndex o2) {
            int cmp = AlphanumComparator.ignoreCaseInstance.compare(o1.getDisplayName(), o2.getDisplayName());
            if (cmp != 0) return cmp;
            return Longs.compare(o1.getCreated(), o2.getCreated());
        }
    }
}
