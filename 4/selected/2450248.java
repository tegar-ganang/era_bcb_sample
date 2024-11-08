package net.conquiris.index;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import net.conquiris.api.index.DocumentWriter;
import net.conquiris.api.index.IndexException;
import net.conquiris.api.index.IndexInfo;
import net.conquiris.api.index.IndexStatus;
import net.conquiris.api.index.Subindexer;
import net.conquiris.api.index.Writer;
import net.conquiris.api.index.WriterResult;
import net.derquinse.common.log.ContextLog;
import net.derquinse.common.util.concurrent.Interruptions;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.ThreadInterruptedException;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.Atomics;

/**
 * Default writer implementation.
 * @author Andres Rodriguez.
 */
final class DefaultWriter extends AbstractWriter {

    /** Log to use. */
    private final ContextLog log;

    /** Writer state lock. */
    private final Lock lock = new ReentrantLock();

    /** Index writer lock. Is RW as the Index Writer has its own synchronization. */
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();

    /** Lucene index writer. */
    private final IndexWriter writer;

    /** Last commit index info. */
    private final IndexInfo indexInfo;

    /** Current user properties. */
    @GuardedBy("lock")
    private final Map<String, String> properties;

    /** User properties key set. */
    private final Set<String> keys;

    /** Whether any indexer has been ever interrupted. */
    private volatile boolean interrupted = false;

    /** Writer result. The writer is available while the value is {@code null}. */
    @GuardedBy("lock")
    private volatile WriterResult result = null;

    /** Whether the writer has been cancelled. */
    @GuardedBy("lock")
    private volatile boolean cancelled = false;

    /** Whether the index has been updated. */
    @GuardedBy("indexLock")
    private boolean updated = false;

    /** Index status. */
    @GuardedBy("indexLock")
    private final AtomicReference<IndexStatus> indexStatus = Atomics.newReference(IndexStatus.OK);

    /** Current checkpoint. */
    @GuardedBy("lock")
    private volatile String checkpoint;

    /** Target checkpoint. */
    @GuardedBy("lock")
    private volatile String targetCheckpoint;

    /**
	 * Default writer.
	 * @param log Log context.
	 * @param writer Lucene index writer to use.
	 * @param overrideCheckpoint Whether to override the checkpoint.
	 * @param checkpoint Overridden checkpoint value.
	 * @param created Whether the index has been requested to be created.
	 */
    DefaultWriter(ContextLog log, IndexWriter writer, boolean overrideCheckpoint, @Nullable String checkpoint, boolean created) throws IndexException {
        this.log = checkNotNull(log, "The log context must be provided");
        this.writer = checkNotNull(writer, "The index writer must be provided");
        this.properties = new MapMaker().makeMap();
        this.keys = Collections.unmodifiableSet(this.properties.keySet());
        try {
            final Map<String, String> commitData;
            final int documents;
            if (created) {
                commitData = ImmutableMap.of();
                documents = 0;
            } else {
                final IndexReader reader = IndexReader.open(writer, false);
                try {
                    Map<String, String> data = reader.getCommitUserData();
                    if (overrideCheckpoint) {
                        final Map<String, String> modified = Maps.newHashMap();
                        if (data != null) {
                            modified.putAll(data);
                        }
                        modified.put(IndexInfo.CHECKPOINT, checkpoint);
                        commitData = modified;
                    } else {
                        commitData = data;
                    }
                    documents = reader.numDocs();
                } finally {
                    Closeables.closeQuietly(reader);
                }
            }
            this.indexInfo = IndexInfo.fromMap(documents, commitData);
            this.checkpoint = this.indexInfo.getCheckpoint();
            this.targetCheckpoint = this.indexInfo.getTargetCheckpoint();
            this.properties.putAll(this.indexInfo.getProperties());
        } catch (LockObtainFailedException e) {
            indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.LOCKED);
            throw new IndexException(e);
        } catch (CorruptIndexException e) {
            indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.CORRUPT);
            throw new IndexException(e);
        } catch (IOException e) {
            indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.IOERROR);
            throw new IndexException(e);
        } catch (RuntimeException e) {
            indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.ERROR);
            throw e;
        }
    }

    /**
	 * Called when the writer can't be used any longer.
	 * @return The writer result.
	 */
    WriterResult done() throws InterruptedException {
        lock.lock();
        try {
            indexLock.writeLock().lock();
            if (result != null) {
                return result;
            }
            result = WriterResult.NORMAL;
            try {
                if (!canContinue()) {
                    log.trace("Writer rolled back");
                    result = WriterResult.ERROR;
                    writer.rollback();
                } else if (!updated && equal(checkpoint, indexInfo.getCheckpoint()) && equal(targetCheckpoint, indexInfo.getTargetCheckpoint()) && equal(properties, indexInfo.getProperties())) {
                    log.trace("Writer unchanged");
                    result = WriterResult.IDLE;
                    writer.rollback();
                } else {
                    Map<String, String> data = Maps.newHashMap(properties);
                    if (checkpoint != null) {
                        data.put(IndexInfo.CHECKPOINT, checkpoint);
                    }
                    if (targetCheckpoint != null) {
                        data.put(IndexInfo.TARGET_CHECKPOINT, targetCheckpoint);
                    }
                    data.put(IndexInfo.TIMESTAMP, Long.toString(System.currentTimeMillis()));
                    data.put(IndexInfo.SEQUENCE, Long.toString(indexInfo.getSequence() + 1));
                    writer.commit(data);
                    log.trace("Writer committed");
                }
            } catch (LockObtainFailedException e) {
                indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.LOCKED);
                result = WriterResult.ERROR;
            } catch (CorruptIndexException e) {
                indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.CORRUPT);
                result = WriterResult.ERROR;
            } catch (IOException e) {
                indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.IOERROR);
                result = WriterResult.ERROR;
            } catch (RuntimeException e) {
                indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.ERROR);
                result = WriterResult.ERROR;
            } finally {
                indexLock.writeLock().unlock();
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    /** Returns the current index status. */
    IndexStatus getIndexStatus() {
        return indexStatus.get();
    }

    @Override
    boolean ensureAvailable() throws InterruptedException {
        checkState(result == null, "The writer can't be used any longer");
        if (interrupted) {
            throw new InterruptedException();
        }
        boolean ok = false;
        try {
            Interruptions.throwIfInterrupted();
            ok = true;
        } finally {
            if (!ok) interrupted = true;
        }
        return canContinue();
    }

    private boolean canContinue() {
        return !cancelled && !interrupted && IndexStatus.OK == indexStatus.get();
    }

    @Override
    public void cancel() throws InterruptedException {
        lock.lock();
        try {
            ensureAvailable();
            if (!cancelled) {
                checkpoint = indexInfo.getCheckpoint();
                properties.clear();
                properties.putAll(indexInfo.getProperties());
                cancelled = true;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public IndexInfo getIndexInfo() throws InterruptedException {
        return null;
    }

    @Override
    public String getCheckpoint() throws InterruptedException {
        ensureAvailable();
        return checkpoint;
    }

    @Override
    public String getTargetCheckpoint() throws InterruptedException {
        ensureAvailable();
        return targetCheckpoint;
    }

    @Override
    public String getProperty(String key) throws InterruptedException {
        ensureAvailable();
        return properties.get(key);
    }

    @Override
    public Set<String> getPropertyKeys() throws InterruptedException {
        ensureAvailable();
        return keys;
    }

    @Override
    public Writer setCheckpoint(String checkpoint) throws InterruptedException {
        lock.lock();
        try {
            if (ensureAvailable()) {
                this.checkpoint = checkpoint;
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public Writer setTargetCheckpoint(String targetCheckpoint) throws InterruptedException {
        lock.lock();
        try {
            if (ensureAvailable()) {
                this.targetCheckpoint = targetCheckpoint;
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public Writer setProperty(String key, String value) throws InterruptedException {
        lock.lock();
        try {
            if (ensureAvailable()) {
                checkKey(key);
                if (value != null) {
                    properties.put(key, value);
                } else {
                    properties.remove(key);
                }
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public Writer setProperties(Map<String, String> values) throws InterruptedException {
        checkNotNull(values, "The commit properties map is null");
        lock.lock();
        try {
            if (ensureAvailable()) {
                Map<String, String> put = Maps.newHashMapWithExpectedSize(values.size());
                Set<String> remove = Sets.newHashSet();
                for (Entry<String, String> e : values.entrySet()) {
                    String key = e.getKey();
                    String value = e.getValue();
                    checkKey(key);
                    if (value != null) {
                        put.put(key, value);
                    } else {
                        remove.add(key);
                    }
                }
                properties.putAll(put);
                for (String k : remove) {
                    properties.remove(k);
                }
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    private Analyzer analyzer(Analyzer a) {
        return a != null ? a : writer.getAnalyzer();
    }

    private abstract class IndexOp {

        IndexOp() {
        }

        abstract boolean perform() throws IOException, InterruptedException;

        final void run() throws InterruptedException {
            indexLock.readLock().lock();
            boolean cancel = false;
            try {
                if (ensureAvailable()) {
                    if (perform()) {
                        updated = true;
                    }
                }
            } catch (ThreadInterruptedException e) {
                interrupted = true;
                throw new InterruptedException();
            } catch (LockObtainFailedException e) {
                indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.LOCKED);
                throw new IndexException(e);
            } catch (CorruptIndexException e) {
                indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.CORRUPT);
                throw new IndexException(e);
            } catch (IOException e) {
                indexStatus.compareAndSet(IndexStatus.OK, IndexStatus.IOERROR);
                throw new IndexException(e);
            } finally {
                if (cancel) {
                    cancelled = true;
                }
                indexLock.readLock().unlock();
            }
        }
    }

    @Override
    public Writer add(final Document document, final Analyzer analyzer) throws InterruptedException {
        new IndexOp() {

            @Override
            boolean perform() throws IOException, InterruptedException {
                if (document != null) {
                    writer.addDocument(document, analyzer(analyzer));
                    return true;
                }
                return false;
            }
        }.run();
        return this;
    }

    @Override
    public Writer deleteAll() throws InterruptedException {
        new IndexOp() {

            @Override
            boolean perform() throws IOException, InterruptedException {
                writer.deleteDocuments(new MatchAllDocsQuery());
                return true;
            }
        }.run();
        return this;
    }

    @Override
    public Writer delete(final Term term) throws InterruptedException {
        new IndexOp() {

            @Override
            boolean perform() throws IOException, InterruptedException {
                if (!isTermNull(term)) {
                    writer.deleteDocuments(term);
                    return true;
                }
                return false;
            }
        }.run();
        return this;
    }

    @Override
    public Writer update(final Term term, final Document document, final Analyzer analyzer) throws InterruptedException {
        new IndexOp() {

            @Override
            boolean perform() throws IOException, InterruptedException {
                if (document != null) {
                    if (isTermNull(term)) {
                        writer.addDocument(document, analyzer(analyzer));
                    } else {
                        writer.updateDocument(term, document, analyzer(analyzer));
                    }
                    return true;
                }
                return false;
            }
        }.run();
        return this;
    }

    @Override
    public Writer runSubindexers(Executor executor, Iterable<? extends Subindexer> subindexers) throws InterruptedException, IndexException {
        checkNotNull(executor, "The executor must be provided");
        checkNotNull(executor, "The subindexers must be provided");
        if (ensureAvailable()) {
            DocumentWriter subwriter = new DefaultDocumentWriter(this);
            CompletionService<IndexStatus> ecs = new ExecutorCompletionService<IndexStatus>(executor);
            int n = 0;
            for (Subindexer subindexer : Iterables.filter(subindexers, Predicates.notNull())) {
                ecs.submit(new SubindexerTask(subwriter, subindexer));
                n++;
            }
            for (int i = 0; i < n && ensureAvailable(); i++) {
                ecs.take();
            }
        }
        return this;
    }

    private final class SubindexerTask implements Callable<IndexStatus> {

        private final DocumentWriter writer;

        private final Subindexer indexer;

        SubindexerTask(DocumentWriter writer, Subindexer indexer) {
            this.writer = checkNotNull(writer, "The document writer must be provided");
            this.indexer = checkNotNull(indexer, "The subindexer must be provided");
        }

        @Override
        public IndexStatus call() throws Exception {
            if (ensureAvailable()) {
                indexer.index(writer);
            }
            return indexStatus.get();
        }
    }
}
