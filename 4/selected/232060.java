package org.objectstyle.cayenne.access;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.ObjectDiff.ArcOperation;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.access.event.SnapshotEventListener;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.graph.NodeCreateOperation;
import org.objectstyle.cayenne.graph.NodeDeleteOperation;
import org.objectstyle.cayenne.graph.NodeDiff;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.validation.ValidationException;
import org.objectstyle.cayenne.validation.ValidationResult;

public class ObjectStore implements Serializable, SnapshotEventListener, GraphManager {

    protected transient Map newObjectsMap;

    protected Map objectMap = new HashMap();

    protected Map queryResultMap = new HashMap();

    protected Map changes = new HashMap();

    int currentDiffId;

    /**
     * Stores a reference to the DataRowStore.
     * <p>
     * <i>Serialization note: </i> It is up to the owner of this ObjectStore to initialize
     * DataRowStore after deserialization of this object. ObjectStore will not know how to
     * restore the DataRowStore by itself.
     * </p>
     */
    protected transient DataRowStore dataRowCache;

    private boolean dataRowCacheSet;

    /**
     * The DataContext that owns this ObjectStore.
     */
    protected DataContext context;

    public ObjectStore() {
    }

    public ObjectStore(DataRowStore dataRowCache) {
        setDataRowCache(dataRowCache);
    }

    /**
     * @since 1.2
     */
    void recordObjectDeleted(Persistent object) {
        object.setPersistenceState(PersistenceState.DELETED);
        registerDiff(object, new NodeDeleteOperation(object.getObjectId()));
    }

    /**
     * @since 1.2
     */
    void recordObjectCreated(Persistent object) {
        registerDiff(object, new NodeCreateOperation(object.getObjectId()));
        registerNode(object.getObjectId(), object);
    }

    public void recordArcCreated(Persistent object, ObjectId targetId, String relationshipName) {
        registerDiff(object, new ArcOperation(object.getObjectId(), targetId, relationshipName, false));
    }

    public void recordArcDeleted(Persistent object, ObjectId targetId, String relationshipName) {
        registerDiff(object, new ArcOperation(object.getObjectId(), targetId, relationshipName, true));
    }

    /**
     * Registers object change.
     * 
     * @since 1.2
     */
    synchronized ObjectDiff registerDiff(Persistent object, NodeDiff diff) {
        ObjectId id = object.getObjectId();
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);
            if (object instanceof DataObject) {
                DataObject dataObject = (DataObject) object;
                DataRow snapshot = getCachedSnapshot(id);
                if (snapshot != null && snapshot.getVersion() != dataObject.getSnapshotVersion()) {
                    DataContextDelegate delegate = dataObject.getDataContext().nonNullDelegate();
                    if (delegate.shouldMergeChanges(dataObject, snapshot)) {
                        ObjEntity entity = dataObject.getDataContext().getEntityResolver().lookupObjEntity(object);
                        DataRowUtils.forceMergeWithSnapshot(entity, dataObject, snapshot);
                        dataObject.setSnapshotVersion(snapshot.getVersion());
                        delegate.finishedMergeChanges(dataObject);
                    }
                }
            }
        }
        if (diff != null) {
            diff.setDiffId(++currentDiffId);
        }
        ObjectDiff objectDiff = (ObjectDiff) changes.get(id);
        if (objectDiff == null) {
            objectDiff = new ObjectDiff(this, object);
            objectDiff.setDiffId(++currentDiffId);
            changes.put(id, objectDiff);
        }
        if (diff != null) {
            objectDiff.addDiff(diff);
        }
        return objectDiff;
    }

    /**
     * Returns a number of objects currently registered with this ObjectStore.
     * 
     * @since 1.2
     */
    public int registeredObjectsCount() {
        return objectMap.size();
    }

    /**
     * Returns a number of query results cached by this object store. Note that each
     * result is a list and can possibly contain a large number of entries.
     * 
     * @since 1.2
     */
    public int cachedQueriesCount() {
        return queryResultMap.size();
    }

    /**
     * Returns a DataRowStore associated with this ObjectStore.
     */
    public DataRowStore getDataRowCache() {
        if (dataRowCache == null && context != null && dataRowCacheSet) {
            synchronized (this) {
                if (dataRowCache == null) {
                    DataDomain domain = context.getParentDataDomain();
                    if (domain != null) {
                        setDataRowCache(domain.getSharedSnapshotCache());
                    }
                }
            }
        }
        return dataRowCache;
    }

    public void setDataRowCache(DataRowStore dataRowCache) {
        if (dataRowCache == this.dataRowCache) {
            return;
        }
        if (this.dataRowCache != null && dataRowCache.getEventManager() != null) {
            dataRowCache.getEventManager().removeListener(this, this.dataRowCache.getSnapshotEventSubject());
        }
        this.dataRowCache = dataRowCache;
        if (dataRowCache != null && dataRowCache.getEventManager() != null) {
            dataRowCache.getEventManager().addNonBlockingListener(this, "snapshotsChanged", SnapshotEvent.class, dataRowCache.getSnapshotEventSubject(), dataRowCache);
        }
        dataRowCacheSet = dataRowCache != null;
    }

    /**
     * Invalidates a collection of DataObjects. Changes objects state to HOLLOW.
     * 
     * @see #objectsUnregistered(Collection)
     */
    public synchronized void objectsInvalidated(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }
        Collection ids = new ArrayList(objects.size());
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            if (object.getPersistenceState() == PersistenceState.NEW) {
                continue;
            }
            object.setPersistenceState(PersistenceState.HOLLOW);
            changes.remove(object.getObjectId());
            ids.add(object.getObjectId());
        }
        if (getDataRowCache() != null) {
            getDataRowCache().processSnapshotChanges(this, Collections.EMPTY_MAP, Collections.EMPTY_LIST, ids, Collections.EMPTY_LIST);
        }
    }

    public synchronized void objectsUnregistered(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }
        Collection ids = new ArrayList(objects.size());
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            ObjectId id = object.getObjectId();
            objectMap.remove(id);
            changes.remove(id);
            ids.add(id);
            object.setDataContext(null);
            object.setObjectId(null);
            object.setPersistenceState(PersistenceState.TRANSIENT);
        }
        if (getDataRowCache() != null) {
            getDataRowCache().processSnapshotChanges(this, Collections.EMPTY_MAP, Collections.EMPTY_LIST, ids, Collections.EMPTY_LIST);
        }
    }

    /**
     * Reverts changes to all stored uncomitted objects.
     * 
     * @since 1.1
     */
    public synchronized void objectsRolledBack() {
        Iterator it = getObjectIterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            int objectState = object.getPersistenceState();
            switch(objectState) {
                case PersistenceState.NEW:
                    it.remove();
                    object.setDataContext(null);
                    object.setObjectId(null);
                    object.setPersistenceState(PersistenceState.TRANSIENT);
                    break;
                case PersistenceState.DELETED:
                case PersistenceState.MODIFIED:
                    object.setPersistenceState(PersistenceState.HOLLOW);
                    break;
                default:
                    break;
            }
        }
        this.changes = new HashMap();
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.1
     * @deprecated since 1.2 use {@link #recordArcDeleted(Persistent, ObjectId, String)}.
     */
    public void objectRelationshipUnset(DataObject source, DataObject target, ObjRelationship relationship, boolean processFlattened) {
        ObjectId targetId = (target != null) ? target.getObjectId() : null;
        recordArcDeleted(source, targetId, relationship.getName());
    }

    /**
     * Performs tracking of object relationship changes.
     * 
     * @since 1.1
     * @deprecated since 1.2 use {@link #recordArcCreated(Persistent, ObjectId, String)}.
     */
    public void objectRelationshipSet(DataObject source, DataObject target, ObjRelationship relationship, boolean processFlattened) {
        ObjectId targetId = (target != null) ? target.getObjectId() : null;
        recordArcCreated(source, targetId, relationship.getName());
    }

    /**
     * Updates snapshots in the underlying DataRowStore. If <code>refresh</code> is
     * true, all snapshots in <code>snapshots</code> will be loaded into DataRowStore,
     * regardless of the existing cache state. If <code>refresh</code> is false, only
     * missing snapshots are loaded. This method is normally called internally by the
     * DataContext owning the ObjectStore to update the caches after a select query.
     * 
     * @param objects a list of object whose snapshots need to be updated.
     * @param snapshots a list of snapshots. Must be of the same length and use the same
     *            order as <code>objects</code> list.
     * @param refresh controls whether existing cached snapshots should be replaced with
     *            the new ones.
     * @since 1.1
     */
    public void snapshotsUpdatedForObjects(List objects, List snapshots, boolean refresh) {
        if (objects.size() != snapshots.size()) {
            throw new IllegalArgumentException("Counts of objects and corresponding snapshots do not match. " + "Objects count: " + objects.size() + ", snapshots count: " + snapshots.size());
        }
        Map modified = null;
        synchronized (this) {
            int size = objects.size();
            for (int i = 0; i < size; i++) {
                DataObject object = (DataObject) objects.get(i);
                if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                    continue;
                }
                ObjectId oid = object.getObjectId();
                DataRow cachedSnapshot = getCachedSnapshot(oid);
                if (refresh || cachedSnapshot == null) {
                    DataRow newSnapshot = (DataRow) snapshots.get(i);
                    if (cachedSnapshot != null) {
                        if (cachedSnapshot.equals(newSnapshot)) {
                            object.setSnapshotVersion(cachedSnapshot.getVersion());
                            continue;
                        } else {
                            newSnapshot.setReplacesVersion(cachedSnapshot.getVersion());
                        }
                    }
                    if (modified == null) {
                        modified = new HashMap();
                    }
                    modified.put(oid, newSnapshot);
                }
            }
            if (modified != null && getDataRowCache() != null) {
                getDataRowCache().processSnapshotChanges(this, modified, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
            }
        }
    }

    /**
     * Processes internal objects after the parent DataContext was committed. Changes
     * object persistence state and handles snapshot updates.
     * 
     * @since 1.1
     * @deprecated since 1.2 unused.
     */
    public synchronized void objectsCommitted() {
        postprocessAfterCommit(new CompoundDiff());
    }

    /**
     * Builds and returns GraphDiff reflecting all uncommitted object changes.
     * 
     * @since 1.2
     */
    ObjectStoreGraphDiff getChanges() {
        return new ObjectStoreGraphDiff(this);
    }

    /**
     * Returns internal changes map.
     * 
     * @since 1.2
     */
    Map getChangesByObjectId() {
        return changes;
    }

    /**
     * @since 1.2
     */
    void postprocessAfterPhantomCommit() {
        Iterator it = changes.keySet().iterator();
        while (it.hasNext()) {
            ObjectId id = (ObjectId) it.next();
            Persistent object = (Persistent) objectMap.get(id);
            object.setPersistenceState(PersistenceState.COMMITTED);
        }
        this.changes.clear();
    }

    /**
     * Internal unsynchronized method to process objects state after commit.
     * 
     * @since 1.2
     */
    void postprocessAfterCommit(GraphDiff parentChanges) {
        Iterator entries = objectMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            DataObject object = (DataObject) entry.getValue();
            switch(object.getPersistenceState()) {
                case PersistenceState.DELETED:
                    entries.remove();
                    object.setObjectContext(null);
                    object.setPersistenceState(PersistenceState.TRANSIENT);
                    break;
                case PersistenceState.NEW:
                case PersistenceState.MODIFIED:
                    object.setPersistenceState(PersistenceState.COMMITTED);
                    break;
            }
        }
        if (!parentChanges.isNoop()) {
            parentChanges.apply(new GraphChangeHandler() {

                public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
                }

                public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
                }

                public void nodeCreated(Object nodeId) {
                }

                public void nodeIdChanged(Object nodeId, Object newId) {
                    processIdChange(nodeId, newId);
                }

                public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
                }

                public void nodeRemoved(Object nodeId) {
                }
            });
        }
        this.changes = new HashMap();
    }

    /**
     * Adds a new object to the ObjectStore.
     * 
     * @deprecated since 1.2 as a different change tracking algorithm is used.
     */
    public synchronized void addObject(DataObject object) {
        recordObjectCreated(object);
    }

    /**
     * Starts tracking the registration of new objects from this ObjectStore. Used in
     * conjunction with unregisterNewObjects() to control garbage collection when an
     * instance of ObjectStore is used over a longer time for batch processing.
     * 
     * @see org.objectstyle.cayenne.access.ObjectStore#unregisterNewObjects()
     */
    public synchronized void startTrackingNewObjects() {
        newObjectsMap = new HashMap();
    }

    /**
     * Unregisters the newly registered DataObjects from this objectStore. Used in
     * conjunction with startTrackingNewObjects() to control garbage collection when an
     * instance of ObjectStore is used over a longer time for batch processing.
     * 
     * @see org.objectstyle.cayenne.access.ObjectStore#startTrackingNewObjects()
     */
    public synchronized void unregisterNewObjects() {
        if (newObjectsMap != null) {
            objectsUnregistered(newObjectsMap.values());
            newObjectsMap = null;
        }
    }

    /**
     * Returns a DataObject registered for a given ObjectId, or null if no such object
     * exists. This method does not do a database fetch.
     * 
     * @deprecated since 1.2 a GraphManager {@link #getNode(Object)} method should be
     *             used.
     */
    public DataObject getObject(ObjectId id) {
        return (DataObject) getNode(id);
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, a null is returned.
     * 
     * @since 1.1
     */
    public DataRow getCachedSnapshot(ObjectId oid) {
        if (context != null && context.getChannel() != null) {
            ObjectIdQuery query = new ObjectIdQuery(oid, true, ObjectIdQuery.CACHE_NOREFRESH);
            List results = context.getChannel().onQuery(context, query).firstList();
            return results.isEmpty() ? null : (DataRow) results.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns cached query results for a given query, or null if no results are cached.
     * Note that ObjectStore will only lookup results in its local cache, and not the
     * shared cache associated with the underlying DataRowStore.
     * 
     * @since 1.1
     */
    public synchronized List getCachedQueryResult(String name) {
        return (List) queryResultMap.get(name);
    }

    /**
     * Caches a list of query results.
     * 
     * @since 1.1
     */
    public synchronized void cacheQueryResult(String name, List results) {
        queryResultMap.put(name, results);
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, it will attempt fetching it using provided QueryEngine. If
     * fetch attempt fails or inconsistent data is returned, underlying cache will throw a
     * CayenneRuntimeException.
     * 
     * @since 1.1
     * @deprecated since 1.2. Use {@link #getSnapshot(ObjectId)} instead.
     */
    public synchronized DataRow getSnapshot(ObjectId oid, QueryEngine engine) {
        return getDataRowCache().getSnapshot(oid, engine);
    }

    /**
     * Returns a snapshot for ObjectId from the underlying snapshot cache. If cache
     * contains no snapshot, it will attempt fetching it using provided QueryEngine. If
     * fetch attempt fails or inconsistent data is returned, underlying cache will throw a
     * CayenneRuntimeException.
     * 
     * @since 1.2
     */
    public synchronized DataRow getSnapshot(ObjectId oid) {
        if (context != null && context.getChannel() != null) {
            ObjectIdQuery query = new ObjectIdQuery(oid, true, ObjectIdQuery.CACHE);
            List results = context.getChannel().onQuery(context, query).firstList();
            return results.isEmpty() ? null : (DataRow) results.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns a list of objects that are registered with this DataContext, regardless of
     * their persistence state. List is returned by copy and can be modified by the
     * caller.
     * 
     * @deprecated since 1.2 use GraphManager method {@link #registeredNodes()}.
     */
    public synchronized List getObjects() {
        return new ArrayList(objectMap.values());
    }

    /**
     * Returns an iterator over the registered objects.
     */
    public synchronized Iterator getObjectIterator() {
        return objectMap.values().iterator();
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this ObjectStore, <code>false</code> otherwise. This method will
     * treat "phantom" modifications are real ones. I.e. if you "change" an object
     * property to an equivalent value, this method will still think such object is
     * modified. Phantom modifications are only detected and discarded during commit.
     */
    public synchronized boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * Return a subset of registered objects that are in a certian persistence state.
     * Collection is returned by copy.
     */
    public synchronized List objectsInState(int state) {
        List filteredObjects = new ArrayList();
        Iterator it = objectMap.values().iterator();
        while (it.hasNext()) {
            DataObject nextObj = (DataObject) it.next();
            if (nextObj.getPersistenceState() == state) filteredObjects.add(nextObj);
        }
        return filteredObjects;
    }

    /**
     * SnapshotEventListener implementation that processes snapshot change event, updating
     * DataObjects that have the changes.
     * <p>
     * <i>Implementation note: </i> This method should not attempt to alter the underlying
     * DataRowStore, since it is normally invoked *AFTER* the DataRowStore was modified as
     * a result of some external interaction.
     * </p>
     * 
     * @since 1.1
     */
    public void snapshotsChanged(SnapshotEvent event) {
        if (event.getPostedBy() != this && event.getSource() == this.getDataRowCache()) {
            processSnapshotEvent(event);
        }
    }

    /**
     * @since 1.2
     */
    synchronized void processSnapshotEvent(SnapshotEvent event) {
        Map modifiedDiffs = event.getModifiedDiffs();
        if (modifiedDiffs != null && !modifiedDiffs.isEmpty()) {
            Iterator oids = modifiedDiffs.entrySet().iterator();
            while (oids.hasNext()) {
                Map.Entry entry = (Map.Entry) oids.next();
                processUpdatedSnapshot(entry.getKey(), (DataRow) entry.getValue());
            }
        }
        Collection deletedIDs = event.getDeletedIds();
        if (deletedIDs != null && !deletedIDs.isEmpty()) {
            Iterator it = deletedIDs.iterator();
            while (it.hasNext()) {
                processDeletedID(it.next());
            }
        }
        processInvalidatedIDs(event.getInvalidatedIds());
        processIndirectlyModifiedIDs(event.getIndirectlyModifiedIds());
        GraphDiff diff = new SnapshotEventDecorator(event);
        ObjectContext originatingContext = (event.getPostedBy() instanceof ObjectContext) ? (ObjectContext) event.getPostedBy() : null;
        context.fireDataChannelChanged(originatingContext, diff);
    }

    /**
     * Performs validation of all uncommitted objects in the ObjectStore. If validation
     * fails, a ValidationException is thrown, listing all encountered failures. This is a
     * utility method for the users to call. Cayenne itself uses a different mechanism to
     * validate objects on commit.
     * 
     * @since 1.1
     * @throws ValidationException
     * @deprecated since 1.2 - This method is no longer used in Cayenne internally.
     */
    public synchronized void validateUncommittedObjects() throws ValidationException {
        Collection deleted = null;
        Collection inserted = null;
        Collection updated = null;
        Iterator allIt = getObjectIterator();
        while (allIt.hasNext()) {
            DataObject dataObject = (DataObject) allIt.next();
            switch(dataObject.getPersistenceState()) {
                case PersistenceState.NEW:
                    if (inserted == null) {
                        inserted = new ArrayList();
                    }
                    inserted.add(dataObject);
                    break;
                case PersistenceState.MODIFIED:
                    if (updated == null) {
                        updated = new ArrayList();
                    }
                    updated.add(dataObject);
                    break;
                case PersistenceState.DELETED:
                    if (deleted == null) {
                        deleted = new ArrayList();
                    }
                    deleted.add(dataObject);
                    break;
            }
        }
        ValidationResult validationResult = new ValidationResult();
        if (deleted != null) {
            Iterator it = deleted.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForDelete(validationResult);
            }
        }
        if (inserted != null) {
            Iterator it = inserted.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForInsert(validationResult);
            }
        }
        if (updated != null) {
            Iterator it = updated.iterator();
            while (it.hasNext()) {
                DataObject dataObject = (DataObject) it.next();
                dataObject.validateForUpdate(validationResult);
            }
        }
        if (validationResult.hasFailures()) {
            throw new ValidationException(validationResult);
        }
    }

    /**
     * Initializes object with data from cache or from the database, if this object is not
     * fully resolved.
     * 
     * @since 1.1
     */
    public void resolveHollow(DataObject object) {
        if (object.getPersistenceState() != PersistenceState.HOLLOW) {
            return;
        }
        DataContext context = object.getDataContext();
        if (context == null) {
            object.setPersistenceState(PersistenceState.TRANSIENT);
            return;
        }
        synchronized (this) {
            ObjectIdQuery query = new ObjectIdQuery(object.getObjectId(), false, ObjectIdQuery.CACHE);
            List results = context.getChannel().onQuery(context, query).firstList();
            if (results.size() == 0) {
                processDeletedID(object.getObjectId());
            } else if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                query = new ObjectIdQuery(object.getObjectId(), false, ObjectIdQuery.CACHE_REFRESH);
                results = context.getChannel().onQuery(context, query).firstList();
                if (results.size() == 0) {
                    processDeletedID(object.getObjectId());
                }
            }
        }
    }

    void processIdChange(Object nodeId, Object newId) {
        Persistent object = (Persistent) objectMap.remove(nodeId);
        if (object != null) {
            object.setObjectId((ObjectId) newId);
            objectMap.put(newId, object);
            Object change = changes.remove(nodeId);
            if (change != null) {
                changes.put(newId, change);
            }
        }
    }

    /**
     * @since 1.2
     */
    void processDeletedID(Object nodeId) {
        DataObject object = (DataObject) getNode(nodeId);
        if (object != null) {
            DataContextDelegate delegate;
            switch(object.getPersistenceState()) {
                case PersistenceState.COMMITTED:
                case PersistenceState.HOLLOW:
                case PersistenceState.DELETED:
                    delegate = context.nonNullDelegate();
                    if (delegate.shouldProcessDelete(object)) {
                        objectMap.remove(nodeId);
                        changes.remove(nodeId);
                        object.setObjectContext(null);
                        delegate.finishedProcessDelete(object);
                    }
                    break;
                case PersistenceState.MODIFIED:
                    delegate = context.nonNullDelegate();
                    if (delegate.shouldProcessDelete(object)) {
                        object.setPersistenceState(PersistenceState.NEW);
                        changes.remove(nodeId);
                        recordObjectCreated(object);
                        delegate.finishedProcessDelete(object);
                    }
                    break;
            }
        }
    }

    /**
     * @since 1.1
     */
    void processInvalidatedIDs(Collection invalidatedIDs) {
        if (invalidatedIDs != null && !invalidatedIDs.isEmpty()) {
            Iterator it = invalidatedIDs.iterator();
            while (it.hasNext()) {
                ObjectId oid = (ObjectId) it.next();
                DataObject object = (DataObject) getNode(oid);
                if (object == null) {
                    continue;
                }
                switch(object.getPersistenceState()) {
                    case PersistenceState.COMMITTED:
                        object.setPersistenceState(PersistenceState.HOLLOW);
                        break;
                    case PersistenceState.MODIFIED:
                        DataContext context = object.getDataContext();
                        DataRow diff = getSnapshot(oid);
                        DataContextDelegate delegate = context.nonNullDelegate();
                        if (delegate.shouldMergeChanges(object, diff)) {
                            ObjEntity entity = context.getEntityResolver().lookupObjEntity(object);
                            DataRowUtils.forceMergeWithSnapshot(entity, object, diff);
                            delegate.finishedMergeChanges(object);
                        }
                    case PersistenceState.HOLLOW:
                        break;
                    case PersistenceState.DELETED:
                        break;
                }
            }
        }
    }

    /**
     * @since 1.1
     */
    void processIndirectlyModifiedIDs(Collection indirectlyModifiedIDs) {
        Iterator indirectlyModifiedIt = indirectlyModifiedIDs.iterator();
        while (indirectlyModifiedIt.hasNext()) {
            Object oid = indirectlyModifiedIt.next();
            DataObject object = (DataObject) getNode(oid);
            if (object == null || object.getPersistenceState() != PersistenceState.COMMITTED) {
                continue;
            }
            DataContextDelegate delegate = object.getDataContext().nonNullDelegate();
            if (delegate.shouldMergeChanges(object, null)) {
                ObjEntity entity = context.getEntityResolver().lookupObjEntity(object);
                Iterator relationshipIterator = entity.getRelationships().iterator();
                while (relationshipIterator.hasNext()) {
                    ObjRelationship relationship = (ObjRelationship) relationshipIterator.next();
                    if (relationship.isSourceIndependentFromTargetChange()) {
                        Object fault = relationship.isToMany() ? Fault.getToManyFault() : Fault.getToOneFault();
                        object.writePropertyDirectly(relationship.getName(), fault);
                    }
                }
                delegate.finishedProcessDelete(object);
            }
        }
    }

    /**
     * @since 1.1
     */
    void processUpdatedSnapshot(Object nodeId, DataRow diff) {
        DataObject object = (DataObject) getNode(nodeId);
        if (object != null && object.getPersistenceState() != PersistenceState.HOLLOW) {
            if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                DataContextDelegate delegate = object.getDataContext().nonNullDelegate();
                if (delegate.shouldMergeChanges(object, diff)) {
                    ObjEntity entity = object.getDataContext().getEntityResolver().lookupObjEntity(object);
                    DataRow snapshot = getSnapshot(object.getObjectId());
                    DataRowUtils.refreshObjectWithSnapshot(entity, object, snapshot, true);
                    delegate.finishedMergeChanges(object);
                }
                return;
            }
            if (object.getPersistenceState() == PersistenceState.DELETED || object.getPersistenceState() == PersistenceState.MODIFIED) {
                DataContextDelegate delegate = object.getDataContext().nonNullDelegate();
                if (delegate.shouldMergeChanges(object, diff)) {
                    ObjEntity entity = object.getDataContext().getEntityResolver().lookupObjEntity(object);
                    DataRowUtils.forceMergeWithSnapshot(entity, object, diff);
                    delegate.finishedMergeChanges(object);
                }
            }
        }
    }

    /**
     * @since 1.2
     */
    public DataContext getContext() {
        return context;
    }

    /**
     * @since 1.2
     */
    public void setContext(DataContext context) {
        this.context = context;
    }

    /**
     * Returns a registered DataObject or null of no object exists for the ObjectId.
     * 
     * @since 1.2
     */
    public synchronized Object getNode(Object nodeId) {
        return objectMap.get(nodeId);
    }

    /**
     * Returns all registered DataObjects. List is returned by copy and can be modified by
     * the caller.
     * 
     * @since 1.2
     */
    public synchronized Collection registeredNodes() {
        return new ArrayList(objectMap.values());
    }

    /**
     * @since 1.2
     */
    public void registerNode(Object nodeId, Object nodeObject) {
        objectMap.put(nodeId, nodeObject);
        if (newObjectsMap != null) {
            newObjectsMap.put(nodeId, nodeObject);
        }
    }

    /**
     * @since 1.2
     */
    public Object unregisterNode(Object nodeId) {
        Object object = getNode(nodeId);
        if (object != null) {
            objectsUnregistered(Collections.singleton(object));
        }
        return object;
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void graphCommitAborted() {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void graphCommitStarted() {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void graphCommitted() {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void graphRolledback() {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void nodeIdChanged(Object nodeId, Object newId) {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void nodeCreated(Object nodeId) {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void nodeRemoved(Object nodeId) {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
    }

    /**
     * Does nothing.
     * 
     * @since 1.2
     */
    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
    }

    class SnapshotEventDecorator implements GraphDiff {

        SnapshotEvent event;

        SnapshotEventDecorator(SnapshotEvent event) {
            this.event = event;
        }

        SnapshotEvent getEvent() {
            return event;
        }

        public void apply(GraphChangeHandler handler) {
            throw new UnsupportedOperationException();
        }

        public boolean isNoop() {
            throw new UnsupportedOperationException();
        }

        public void undo(GraphChangeHandler handler) {
            throw new UnsupportedOperationException();
        }
    }
}
