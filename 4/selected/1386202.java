package net.sourceforge.docfetcher.model.index;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.PendingDeletion;
import net.sourceforge.docfetcher.model.TreeIndex.IndexingResult;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.index.Task.TaskState;
import net.sourceforge.docfetcher.model.index.file.FileIndex;
import net.sourceforge.docfetcher.model.index.outlook.OutlookIndex;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.NotThreadSafe;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.collect.LazyList;
import com.google.common.collect.ImmutableList;

/**
 * @author Tran Nam Quang
 */
public final class IndexingQueue {

    public interface ExistingTasksHandler {

        public void handleExistingTasks(@NotNull List<Task> tasks);
    }

    public enum Rejection {

        INVALID_UPDATE, OVERLAP_WITH_REGISTRY, OVERLAP_WITH_QUEUE, SAME_IN_REGISTRY, SAME_IN_QUEUE, REDUNDANT_UPDATE, SHUTDOWN
    }

    public final Event<Void> evtQueueEmpty = new Event<Void>();

    public final Event<Void> evtWorkerThreadTerminated = new Event<Void>();

    private final Event<Task> evtAdded = new Event<Task>();

    private final Event<Task> evtRemoved = new Event<Task>();

    private final Thread thread;

    private final IndexRegistry indexRegistry;

    private final LinkedList<Task> tasks = new LinkedList<Task>();

    private volatile boolean shutdown = false;

    final Lock readLock;

    final Lock writeLock;

    private final Condition readyTaskAvailable;

    final int reporterCapacity;

    public IndexingQueue(@NotNull final IndexRegistry indexRegistry, int reporterCapacity) {
        this.indexRegistry = indexRegistry;
        this.reporterCapacity = reporterCapacity;
        readLock = indexRegistry.getReadLock();
        writeLock = indexRegistry.getWriteLock();
        readyTaskAvailable = writeLock.newCondition();
        evtRemoved.add(new Event.Listener<Task>() {

            public void update(Task task) {
                boolean isQueueEmpty;
                LuceneIndex luceneIndex = task.getLuceneIndex();
                writeLock.lock();
                try {
                    boolean isRebuild = task.is(IndexAction.REBUILD);
                    boolean notStartedYet = task.is(TaskState.NOT_READY) || task.is(TaskState.READY);
                    if (isRebuild && notStartedYet) {
                        assert !indexRegistry.getIndexes().contains(luceneIndex);
                        indexRegistry.addIndex(luceneIndex);
                    }
                    isQueueEmpty = tasks.isEmpty();
                } finally {
                    writeLock.unlock();
                }
                if (isQueueEmpty) evtQueueEmpty.fire(null);
            }
        });
        thread = new Thread(IndexingQueue.class.getName()) {

            public void run() {
                while (threadLoop()) ;
                evtWorkerThreadTerminated.fire(null);
            }
        };
        thread.start();
    }

    private boolean threadLoop() {
        Task task;
        writeLock.lock();
        try {
            task = getReadyTask();
            while (task == null && !shutdown) {
                readyTaskAvailable.await();
                task = getReadyTask();
            }
            if (shutdown) return false;
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        } finally {
            writeLock.unlock();
        }
        assert isValidRegistryState(indexRegistry, task);
        task.set(TaskState.INDEXING);
        LuceneIndex luceneIndex = task.getLuceneIndex();
        if (task.is(IndexAction.REBUILD)) {
            indexRegistry.getSearcher().replaceLuceneSearcher();
            luceneIndex.clear();
        }
        IndexingResult result = task.update();
        boolean hasErrors = luceneIndex.hasErrorsDeep();
        boolean doDelete = false;
        boolean fireRemoved = false;
        writeLock.lock();
        try {
            if (task.is(IndexAction.UPDATE)) {
                assert !task.is(CancelAction.DISCARD);
                if (task.getDeletion() == null || result != IndexingResult.SUCCESS_UNCHANGED) {
                    indexRegistry.save(luceneIndex);
                    indexRegistry.getSearcher().replaceLuceneSearcher();
                } else {
                    doDelete = true;
                }
                fireRemoved = tasks.remove(task);
            } else if (result == IndexingResult.FAILURE) {
                doDelete = true;
            } else if (task.is(CancelAction.DISCARD)) {
                doDelete = true;
                fireRemoved = tasks.remove(task);
            } else {
                indexRegistry.addIndex(luceneIndex);
                if (result == IndexingResult.SUCCESS_CHANGED) indexRegistry.save(luceneIndex);
                boolean keep = task.is(CancelAction.KEEP);
                if (keep || shutdown || !hasErrors) fireRemoved = tasks.remove(task);
            }
            task.set(TaskState.FINISHED);
        } finally {
            writeLock.unlock();
        }
        if (fireRemoved) evtRemoved.fire(task);
        task.evtFinished.fire(hasErrors);
        if (doDelete) {
            PendingDeletion deletion = task.getDeletion();
            if (deletion == null) {
                luceneIndex.delete();
            } else {
                assert task.is(IndexAction.UPDATE);
                deletion.setApprovedByQueue();
            }
        }
        return true;
    }

    @NotThreadSafe
    @Nullable
    private Task getReadyTask() {
        for (Task task : tasks) if (task.is(TaskState.READY) && task.cancelAction == null) return task;
        return null;
    }

    @ThreadSafe
    private static boolean isValidRegistryState(@NotNull IndexRegistry indexRegistry, @NotNull Task task) {
        LuceneIndex luceneIndex = task.getLuceneIndex();
        boolean registered = indexRegistry.getIndexes().contains(luceneIndex);
        return registered == task.is(IndexAction.UPDATE);
    }

    @Nullable
    @ThreadSafe
    public Rejection addTask(@NotNull LuceneIndex index, @NotNull IndexAction action) {
        Util.checkNotNull(index, action);
        Util.checkThat(index instanceof FileIndex || index instanceof OutlookIndex);
        Task task = new Task(this, index, action);
        File taskIndexDir = task.getLuceneIndex().getIndexDirPath().getCanonicalFile();
        File taskParentIndexDir = Util.getParentFile(taskIndexDir);
        File indexParentDir = indexRegistry.getIndexParentDir();
        String absPath1 = Util.getAbsPath(taskParentIndexDir);
        String absPath2 = Util.getAbsPath(indexParentDir);
        Util.checkThat(absPath1.equals(absPath2), absPath1 + " != " + absPath2);
        LazyList<Task> removedTasks = new LazyList<Task>();
        writeLock.lock();
        try {
            assert task.cancelAction == null;
            assert task.is(TaskState.NOT_READY) || task.is(TaskState.READY);
            if (shutdown) return Rejection.SHUTDOWN;
            List<LuceneIndex> indexesInRegistry = indexRegistry.getIndexes();
            if (task.is(IndexAction.UPDATE)) {
                if (!indexesInRegistry.contains(index)) return Rejection.INVALID_UPDATE;
                for (Task queueTask : tasks) if (queueTask.is(TaskState.READY) && sameTarget(queueTask, task)) return Rejection.REDUNDANT_UPDATE;
            } else if (index instanceof OutlookIndex) {
                for (LuceneIndex index0 : indexesInRegistry) if (index0 instanceof OutlookIndex && sameTarget(index0, task)) return Rejection.SAME_IN_REGISTRY;
                Iterator<Task> it = tasks.iterator();
                while (it.hasNext()) {
                    Task queueTask = it.next();
                    assert queueTask != task;
                    if (!(queueTask.getLuceneIndex() instanceof OutlookIndex)) continue;
                    if (task.is(IndexAction.REBUILD) && queueTask.is(IndexAction.UPDATE)) {
                        if (sameTarget(queueTask, task)) {
                            assert index == queueTask.getLuceneIndex();
                            if (queueTask.is(TaskState.INDEXING)) queueTask.cancelAction = CancelAction.KEEP;
                            it.remove();
                            removedTasks.add(queueTask);
                        }
                    } else if (sameTarget(queueTask, task)) {
                        return Rejection.SAME_IN_QUEUE;
                    }
                }
            } else {
                assert index instanceof FileIndex;
                for (LuceneIndex index0 : indexesInRegistry) {
                    if (index0 instanceof OutlookIndex) continue;
                    File f1 = index0.getCanonicalRootFile();
                    File f2 = task.getLuceneIndex().getCanonicalRootFile();
                    if (f1.equals(f2)) return Rejection.SAME_IN_REGISTRY;
                    if (isOverlapping(f1, f2)) return Rejection.OVERLAP_WITH_REGISTRY;
                }
                Iterator<Task> it = tasks.iterator();
                while (it.hasNext()) {
                    Task queueTask = it.next();
                    assert queueTask != task;
                    if (!(queueTask.getLuceneIndex() instanceof FileIndex)) continue;
                    File f1 = queueTask.getLuceneIndex().getCanonicalRootFile();
                    File f2 = index.getCanonicalRootFile();
                    if (isOverlapping(f1, f2)) return Rejection.OVERLAP_WITH_QUEUE;
                    if (task.is(IndexAction.REBUILD) && queueTask.is(IndexAction.UPDATE)) {
                        if (f1.equals(f2)) {
                            if (queueTask.is(TaskState.INDEXING)) queueTask.cancelAction = CancelAction.KEEP;
                            it.remove();
                            removedTasks.add(queueTask);
                        }
                    } else if (f1.equals(f2)) {
                        return Rejection.SAME_IN_QUEUE;
                    }
                }
            }
            tasks.add(task);
            if (task.is(TaskState.READY)) readyTaskAvailable.signal();
        } finally {
            writeLock.unlock();
        }
        evtAdded.fire(task);
        for (Task removedTask : removedTasks) evtRemoved.fire(removedTask);
        return null;
    }

    @NotThreadSafe
    static boolean sameTarget(@NotNull Task task1, @NotNull Task task2) {
        File target1 = task1.getLuceneIndex().getCanonicalRootFile();
        File target2 = task2.getLuceneIndex().getCanonicalRootFile();
        return target1.equals(target2);
    }

    @NotThreadSafe
    private static boolean sameTarget(@NotNull LuceneIndex index, @NotNull Task task) {
        File target1 = index.getCanonicalRootFile();
        File target2 = task.getLuceneIndex().getCanonicalRootFile();
        return target1.equals(target2);
    }

    @NotThreadSafe
    private static boolean isOverlapping(@NotNull File f1, @NotNull File f2) {
        return Util.contains(f1, f2) || Util.contains(f2, f1);
    }

    @ThreadSafe
    public void removeAll(@NotNull CancelHandler handler, @NotNull Event.Listener<Task> addedListener, @NotNull Event.Listener<Task> removedListener) {
        Util.checkNotNull(handler, addedListener, removedListener);
        LazyList<Task> removedTasks = new LazyList<Task>();
        writeLock.lock();
        try {
            if (!removeAll(handler, removedTasks)) return;
            evtAdded.remove(addedListener);
            evtRemoved.remove(removedListener);
        } finally {
            writeLock.unlock();
        }
        for (Task task : removedTasks) evtRemoved.fire(task);
    }

    @NotThreadSafe
    private boolean removeAll(@NotNull CancelHandler handler, @NotNull LazyList<Task> removedTasks) {
        for (Task task : tasks) {
            if (!task.is(TaskState.INDEXING)) continue;
            if (task.is(IndexAction.UPDATE)) {
                task.cancelAction = CancelAction.KEEP;
            } else {
                task.cancelAction = handler.cancel();
                if (task.cancelAction == null) return false;
            }
        }
        removedTasks.addAll(tasks);
        tasks.clear();
        return true;
    }

    @ThreadSafe
    public void addListeners(@NotNull ExistingTasksHandler handler, @NotNull Event.Listener<Task> addedListener, @NotNull Event.Listener<Task> removedListener) {
        Util.checkNotNull(handler, addedListener, removedListener);
        ImmutableList<Task> tasksCopy;
        writeLock.lock();
        try {
            tasksCopy = ImmutableList.copyOf(tasks);
            evtAdded.add(addedListener);
            evtRemoved.add(removedListener);
        } finally {
            writeLock.unlock();
        }
        handler.handleExistingTasks(tasksCopy);
    }

    public void removeListeners(@NotNull Event.Listener<Task> addedListener, @NotNull Event.Listener<Task> removedListener) {
        Util.checkNotNull(addedListener, removedListener);
        writeLock.lock();
        try {
            evtAdded.remove(addedListener);
            evtRemoved.remove(removedListener);
        } finally {
            writeLock.unlock();
        }
    }

    @ThreadSafe
    void remove(@NotNull Task task, @NotNull CancelHandler handler) {
        Util.checkNotNull(task, handler);
        boolean fireRemoved = false;
        writeLock.lock();
        try {
            if (task.is(TaskState.INDEXING)) {
                if (task.is(IndexAction.UPDATE)) {
                    task.cancelAction = CancelAction.KEEP;
                } else {
                    task.cancelAction = handler.cancel();
                    if (task.cancelAction == null) return;
                }
            }
            fireRemoved = tasks.remove(task);
        } finally {
            writeLock.unlock();
        }
        if (fireRemoved) evtRemoved.fire(task);
    }

    @ThreadSafe
    void setReady(@NotNull Task task) {
        Util.checkNotNull(task);
        LazyList<Task> removedTasks = new LazyList<Task>();
        writeLock.lock();
        try {
            Util.checkThat(task.cancelAction == null);
            if (!task.is(TaskState.NOT_READY)) return;
            task.set(TaskState.READY);
            Iterator<Task> it = tasks.iterator();
            while (it.hasNext()) {
                Task queueTask = it.next();
                if (queueTask != task && queueTask.is(IndexAction.UPDATE) && queueTask.is(TaskState.READY) && IndexingQueue.sameTarget(queueTask, task)) {
                    it.remove();
                    removedTasks.add(queueTask);
                }
            }
            readyTaskAvailable.signal();
        } finally {
            writeLock.unlock();
        }
        for (Task removedTask : removedTasks) evtRemoved.fire(removedTask);
    }

    @ThreadSafe
    @VisibleForPackageGroup
    public void approveDeletions(@NotNull List<PendingDeletion> deletions) {
        Util.checkNotNull(deletions);
        if (deletions.isEmpty()) return;
        LazyList<Task> removedTasks = new LazyList<Task>();
        writeLock.lock();
        try {
            for (PendingDeletion deletion : deletions) {
                Iterator<Task> it = tasks.iterator();
                boolean approveImmediately = true;
                while (it.hasNext()) {
                    Task task = it.next();
                    if (task.getLuceneIndex() != deletion.getLuceneIndex()) continue;
                    if (task.is(TaskState.INDEXING)) {
                        Util.checkThat(task.is(IndexAction.UPDATE));
                        approveImmediately = false;
                        task.setDeletion(deletion);
                        task.cancelAction = CancelAction.KEEP;
                    }
                    it.remove();
                    removedTasks.add(task);
                }
                if (approveImmediately) deletion.setApprovedByQueue();
            }
        } finally {
            writeLock.unlock();
        }
        for (Task task : removedTasks) evtRemoved.fire(task);
    }

    @ThreadSafe
    public boolean shutdown(@NotNull final CancelHandler handler) {
        Util.checkNotNull(handler);
        LazyList<Task> removedTasks = new LazyList<Task>();
        writeLock.lock();
        try {
            if (shutdown) throw new UnsupportedOperationException();
            if (!removeAll(handler, removedTasks)) return false;
            shutdown = true;
            readyTaskAvailable.signal();
        } finally {
            writeLock.unlock();
        }
        for (Task task : removedTasks) evtRemoved.fire(task);
        return true;
    }
}
