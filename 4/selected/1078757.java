package tapioca.dt;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import tapioca.dt.WrappedDatastoreService.GetResponse;
import tapioca.util.EntityGroupBatchJob;
import tapioca.util.EntityGroupBatchJob_v2;
import tapioca.util.KeyUtil;
import tapioca.util.RetryHelper;
import tapioca.util.RetryHelper.TransactionalWorkUnit;
import tapioca.util.RetryHelper.WorkUnit;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.appengine.repackaged.com.google.common.collect.Maps;
import com.google.appengine.repackaged.com.google.common.collect.Sets;

/**
 * A distributed transaction algorithm for Google App Engine.
 * 
 * The paper on which this implementation is based is 
 * @see <a href="http://danielwilkerson.com/dist-trans-gae.html">
 *   http://danielwilkerson.com/dist-trans-gae.html
 * </a>
 * 
 * TODO (earmbrust): Add timeout logic
 * TODO (earmbrust): Make optimizations outlined in the paper
 * 
 * @author armbrust@gmail.com (Erick Armbrust)
 */
public class DistributedTransaction {

    private final DatastoreService ds;

    private final WrappedDatastoreService wds;

    private final Map<Key, CacheRecord> cache;

    private final boolean readConsistent;

    private DistributedTransactionEntity dtEntity;

    private Key firstEntityGroup = null;

    private boolean spansSingleEntityGroup = true;

    /**
   * Constructor.
   * 
   * @param ds
   */
    public DistributedTransaction(DatastoreService ds) {
        this(ds, false);
    }

    public DistributedTransaction(DatastoreService ds, boolean readConsistent) {
        this(ds, new DistributedTransactionEntity(), readConsistent);
    }

    /**
   * 
   * @param ds
   * @param entity
   */
    public DistributedTransaction(DatastoreService ds, Entity entity) {
        this(ds, new DistributedTransactionEntity(entity), false);
    }

    /**
   * 
   * @param ds
   * @param dtEntity
   */
    private DistributedTransaction(DatastoreService ds, DistributedTransactionEntity dtEntity, boolean readConsistent) {
        this.ds = ds;
        this.dtEntity = dtEntity;
        this.readConsistent = readConsistent;
        wds = new WrappedDatastoreService(ds);
        cache = Maps.newHashMap();
    }

    /**
   * 
   * @param ds
   * @param key
   * @return
   */
    public static DistributedTransaction getTxn(DatastoreService ds, Key key) throws DistributedTransactionNotFoundException {
        try {
            return new DistributedTransaction(ds, ds.get(key));
        } catch (EntityNotFoundException e) {
            throw new DistributedTransactionNotFoundException("Distributed transaction not found");
        }
    }

    public Entity get(Key key) throws EntityNotFoundException {
        if (cache.containsKey(key)) {
            CacheRecord record = cache.get(key);
            if (record.isRetrieved() && record.getEntity() != null && !record.isDeleted()) {
                return record.getEntity();
            } else if (record.getEntity() == null || record.isDeleted()) {
                throw new EntityNotFoundException(key);
            } else {
                throw new RuntimeException("Cannot read after a blind write");
            }
        }
        Set<Key> writeLocksToIgnore = Sets.newHashSet();
        while (true) {
            GetResponse response = wds.get(key);
            if (readConsistent && response.getWriteLock() != null && !writeLocksToIgnore.contains(response.getWriteLock())) {
                try {
                    DistributedTransaction dt = getTxn(ds, response.getWriteLock());
                    rollForwardBlockingTxn(dt);
                } catch (DistributedTransactionNotFoundException e) {
                    writeLocksToIgnore.add(response.getWriteLock());
                }
            } else {
                updateTxnEntityGroupInfo(key);
                dtEntity.addGet(new GetRequest(key, response.getVersion()));
                CacheRecord record = new CacheRecord(key);
                record.setEntity(response.getEntity());
                record.setRetrieved(true);
                record.setVersion(response.getVersion());
                cache.put(key, record);
                if (response.inDatastore()) {
                    return response.getEntity();
                } else {
                    throw new EntityNotFoundException(key);
                }
            }
        }
    }

    public Key put(Entity entity) {
        if (entity.getKey().isComplete() && !cache.containsKey(entity.getKey())) {
            wds.get(entity.getKey());
        }
        boolean isCreateRequest = false;
        if (!entity.getKey().isComplete()) {
            KeyRange keyRange = null;
            if (entity.getKey().getParent() == null) {
                keyRange = ds.allocateIds(entity.getKind(), 1);
            } else {
                keyRange = ds.allocateIds(entity.getKey().getParent(), entity.getKind(), 1);
            }
            Entity copy = new Entity(keyRange.getStart());
            copy.setPropertiesFrom(entity);
            entity = copy;
            isCreateRequest = true;
        }
        updateTxnEntityGroupInfo(entity.getKey());
        CacheRecord record = null;
        if (cache.containsKey(entity.getKey())) {
            record = cache.get(entity.getKey());
            record.setDeleted(false);
        } else {
            record = new CacheRecord(entity.getKey());
        }
        record.setEntity(entity);
        record.setInserted(true);
        record.setCreated(isCreateRequest);
        cache.put(entity.getKey(), record);
        return entity.getKey();
    }

    public void delete(Key key) {
        if (!cache.containsKey(key)) {
            wds.get(key);
        }
        updateTxnEntityGroupInfo(key);
        CacheRecord record = null;
        if (cache.containsKey(key)) {
            record = cache.get(key);
            record.setInserted(false);
            record.setCreated(false);
        } else {
            record = new CacheRecord(key);
        }
        record.setDeleted(true);
        record.setEntity(null);
        cache.put(key, record);
    }

    public void begin() {
        save();
    }

    public void commit() throws DistributedTransactionFailedException {
        dispatchOnState();
    }

    void dispatchOnState() throws DistributedTransactionFailedException {
        boolean done = false;
        Exception abortReason = null;
        try {
            while (!done) {
                switch(dtEntity.getState()) {
                    case INIT_0:
                        initialize();
                        break;
                    case READY_1:
                        acquireWriteLocks();
                        break;
                    case LOCKED_2:
                        try {
                            checkReadVersions();
                        } catch (ReadVersionChangedException e) {
                            abortReason = e;
                            abort();
                        } catch (WriteLockHeldException e) {
                            abortReason = e;
                            abort();
                        }
                        break;
                    case CHECKED_3:
                        copy();
                        break;
                    case ABORTING_3:
                        cleanup();
                        break;
                    case DONE_4:
                        complete();
                        done = true;
                        break;
                    case ABORTED_4:
                        complete();
                        done = true;
                        if (abortReason != null) {
                            throw new DistributedTransactionFailedException("Distributed transaction aborted", abortReason);
                        } else {
                            throw new DistributedTransactionFailedException("Distributed transaction aborted");
                        }
                }
            }
        } catch (DistributedTransactionNotFoundException e) {
            return;
        }
    }

    void updateTxnEntityGroupInfo(Key key) {
        if (firstEntityGroup == null) {
            firstEntityGroup = key;
        } else {
            spansSingleEntityGroup &= KeyUtil.inSameEntityGroup(firstEntityGroup, key);
        }
    }

    void initialize() {
        populatePutRequests();
        generateShadowCopies();
        sortGetsAndPuts();
        dtEntity.setState(State.READY_1);
        save();
    }

    void copy() throws DistributedTransactionNotFoundException {
        copyShadowEntities();
        transitionState(State.DONE_4);
    }

    void complete() {
        ds.delete(dtEntity.getEntity().getKey());
    }

    void abort() throws DistributedTransactionNotFoundException {
        transitionState(State.ABORTING_3);
    }

    void cleanup() throws DistributedTransactionNotFoundException {
        cleanupPutRequests();
        transitionState(State.ABORTED_4);
    }

    void save(Transaction txn) {
        dtEntity.save(ds, txn);
        if (txn != null) txn.commit();
    }

    void save() {
        save(null);
    }

    void transitionState(final State newState) throws DistributedTransactionNotFoundException {
        final AtomicReference<DistributedTransactionNotFoundException> exception = new AtomicReference<DistributedTransactionNotFoundException>(null);
        new RetryHelper(ds, new TransactionalWorkUnit() {

            @Override
            public void doWork(DatastoreService ds, Transaction txn) {
                try {
                    DistributedTransactionEntity dtEntityCopy = new DistributedTransactionEntity(ds.get(txn, dtEntity.getEntity().getKey()));
                    State copyState = dtEntityCopy.getState();
                    if (copyState.getRanking() < dtEntity.getState().getRanking()) {
                        throw new RuntimeException("Impossible state transition, DT cannot continue.");
                    } else if (copyState == newState) {
                        dtEntity.setState(newState);
                        return;
                    } else if (copyState.getRanking() >= newState.getRanking()) {
                        dtEntity = dtEntityCopy;
                        return;
                    } else {
                        dtEntity.setState(newState);
                        save(txn);
                    }
                } catch (EntityNotFoundException e) {
                    exception.set(new DistributedTransactionNotFoundException("DT not found."));
                }
            }
        }).execute();
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    void populatePutRequests() {
        for (CacheRecord record : cache.values()) {
            dtEntity.addPut(new PutRequest(record.getKey(), ShadowEntity.createShadowKey(getKey(), record.getKey()), record.isCreated()));
        }
    }

    void generateShadowCopies() {
        for (final CacheRecord record : cache.values()) {
            new RetryHelper(ds, new WorkUnit() {

                @Override
                public void doWork(DatastoreService ds) {
                    if (record.isInserted()) {
                        ShadowEntity.createAndSavePutShadow(ds, getKey(), record.getEntity());
                    } else if (record.isDeleted()) {
                        ShadowEntity.createAndSaveDeleteShadow(ds, getKey(), record.getKey());
                    }
                }
            }).execute();
        }
    }

    void sortGetsAndPuts() {
        Collections.sort(dtEntity.getGets(), new Comparator<GetRequest>() {

            public int compare(GetRequest a, GetRequest b) {
                return a.getKey().compareTo(b.getKey());
            }
        });
        Collections.sort(dtEntity.getPuts(), new Comparator<PutRequest>() {

            public int compare(PutRequest a, PutRequest b) {
                return a.getKey().compareTo(b.getKey());
            }
        });
    }

    void rollForwardBlockingTxn(DistributedTransaction dt) {
        try {
            dt.commit();
        } catch (DistributedTransactionFailedException e) {
        }
    }

    boolean acquireWriteLock(final PutRequest putRequest) {
        final Set<Key> writeLocksToIgnore = Sets.newHashSet();
        while (true) {
            final AtomicReference<Pair<Boolean, Boolean>> retryOrReturn = new AtomicReference<Pair<Boolean, Boolean>>(Pair.of(false, false));
            new RetryHelper(ds, new TransactionalWorkUnit() {

                @Override
                public void doWork(DatastoreService ds, Transaction txn) {
                    try {
                        ds.get(txn, putRequest.getShadowKey());
                    } catch (EntityNotFoundException e) {
                        if (txn.isActive()) {
                            txn.rollback();
                        }
                        retryOrReturn.set(Pair.of(false, false));
                        return;
                    }
                    GetResponse response = wds.get(txn, putRequest.getKey());
                    Key writeLock = response.getWriteLock();
                    if (writeLock == null || writeLocksToIgnore.contains(writeLock)) {
                        wds.setAndSaveWriteLock(txn, response, getKey());
                        txn.commit();
                        retryOrReturn.set(Pair.of(false, true));
                        return;
                    } else if (!writeLock.equals(getKey())) {
                        try {
                            txn.rollback();
                            DistributedTransaction dt = getTxn(ds, writeLock);
                            rollForwardBlockingTxn(dt);
                        } catch (DistributedTransactionNotFoundException e) {
                            writeLocksToIgnore.add(writeLock);
                        }
                        retryOrReturn.set(Pair.of(true, false));
                        return;
                    } else {
                        txn.rollback();
                        retryOrReturn.set(Pair.of(false, false));
                        return;
                    }
                }
            }).execute();
            if (retryOrReturn.get().getFirst() == false) {
                return retryOrReturn.get().getSecond();
            }
        }
    }

    void acquireWriteLocks() throws DistributedTransactionNotFoundException {
        for (PutRequest putRequest : dtEntity.getPuts()) {
            if (putRequest.getKey().isComplete() && !putRequest.isNonNamedCreateRequest()) {
                acquireWriteLock(putRequest);
            }
        }
        transitionState(State.LOCKED_2);
    }

    void checkReadVersion(GetRequest getRequest) throws WriteLockHeldException, ReadVersionChangedException {
        GetResponse response = wds.get(getRequest.getKey());
        Key curWriteLock = response.getWriteLock();
        Key curVersion = response.getVersion();
        if (curWriteLock != null && !curWriteLock.equals(getKey())) {
            throw new WriteLockHeldException("This DT does not hold the write lock");
        }
        if ((getRequest.getVersion() != null && !getRequest.getVersion().equals(curVersion) && !curVersion.equals(getKey())) || (getRequest.getVersion() == null && curVersion != null)) {
            String expected = getRequest.getVersion() == null ? "null" : KeyFactory.keyToString(getRequest.getVersion());
            String received = curVersion == null ? "null" : KeyFactory.keyToString(curVersion);
            throw new ReadVersionChangedException("Expected version key " + expected + " but received " + received);
        }
    }

    void checkReadVersions() throws DistributedTransactionNotFoundException, ReadVersionChangedException, WriteLockHeldException {
        for (GetRequest getRequest : dtEntity.getGets()) {
            checkReadVersion(getRequest);
        }
        transitionState(State.CHECKED_3);
    }

    boolean copyShadowEntity(Transaction txn, PutRequest putRequest) {
        try {
            ShadowEntity sce = ShadowEntity.loadShadow(ds, txn, putRequest.getShadowKey());
            Entity shadowEntity = sce.getShadowed();
            if (sce.getActionType() == ActionType.PUT) {
                wds.put(txn, shadowEntity, dtEntity.getEntity().getKey());
            } else if (sce.getActionType() == ActionType.DELETE) {
                wds.delete(txn, putRequest.getKey(), dtEntity.getEntity().getKey());
            } else {
                throw new RuntimeException("Unexpected action type!");
            }
            ds.delete(txn, putRequest.getShadowKey());
            return true;
        } catch (EntityNotFoundException e) {
        }
        return false;
    }

    boolean copyShadowEntityGroup(Transaction txn, List<PutRequest> entityGroup) {
        List<Key> shadowKeys = Lists.newArrayList();
        for (PutRequest putRequest : entityGroup) {
            shadowKeys.add(putRequest.getShadowKey());
        }
        List<Entity> puts = Lists.newArrayList();
        List<Key> deletes = Lists.newArrayList(shadowKeys);
        List<ShadowEntity> shadows = ShadowEntity.loadShadows(ds, txn, shadowKeys);
        for (ShadowEntity shadow : shadows) {
            if (shadow.getActionType() == ActionType.PUT) {
                puts.add(shadow.getShadowed());
            } else if (shadow.getActionType() == ActionType.DELETE) {
                deletes.add(shadow.getDeleteKey());
            } else {
                throw new RuntimeException("Unexpected action type!");
            }
        }
        wds.put(txn, puts, getKey());
        wds.delete(txn, deletes, getKey());
        return false;
    }

    void copyShadowEntities() {
        EntityGroupBatchJob<PutRequest> batchJob = new EntityGroupBatchJob<PutRequest>(ds, dtEntity.getPuts()) {

            @Override
            public Key getKeyFromItem(PutRequest putRequest) {
                return putRequest.getKey();
            }

            @Override
            public void processItem(Transaction txn, PutRequest putRequest) {
                copyShadowEntity(txn, putRequest);
            }
        };
        batchJob.execute();
    }

    boolean cleanupPutRequest(Transaction txn, PutRequest putRequest) {
        try {
            ShadowEntity.loadShadow(ds, txn, putRequest.getShadowKey());
            GetResponse response = wds.get(txn, putRequest.getKey());
            if (response.getWriteLock().equals(dtEntity.getEntity().getKey())) {
                wds.setAndSaveWriteLock(txn, response, null);
            }
            ds.delete(txn, putRequest.getShadowKey());
            return true;
        } catch (EntityNotFoundException e) {
        }
        return false;
    }

    void cleanupPutRequests() {
        EntityGroupBatchJob<PutRequest> batchJob = new EntityGroupBatchJob<PutRequest>(ds, dtEntity.getPuts()) {

            @Override
            public Key getKeyFromItem(PutRequest putRequest) {
                return putRequest.getKey();
            }

            @Override
            public void processItem(Transaction txn, PutRequest putRequest) {
                cleanupPutRequest(txn, putRequest);
            }
        };
        batchJob.execute();
    }

    Entity getEntity() {
        return dtEntity.getEntity();
    }

    Key getKey() {
        return dtEntity.getEntity().getKey();
    }

    State getState() {
        return dtEntity.getState();
    }

    List<GetRequest> getGets() {
        return dtEntity.getGets();
    }

    List<PutRequest> getPuts() {
        return dtEntity.getPuts();
    }

    Map<Key, CacheRecord> getCache() {
        return cache;
    }

    DistributedTransactionEntity getDtEntity() {
        return dtEntity;
    }

    boolean spansSingleEntityGroup() {
        return spansSingleEntityGroup;
    }
}
