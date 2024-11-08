package org.objectstyle.cayenne.access;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.DeleteDenyException;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.FaultFailureException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.util.IteratedSelectObserver;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.CollectionProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.property.PropertyVisitor;
import org.objectstyle.cayenne.property.SingleObjectArcProperty;
import org.objectstyle.cayenne.query.NamedQuery;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.EventUtil;
import org.objectstyle.cayenne.util.GenericResponse;
import org.objectstyle.cayenne.util.Util;

/**
 * Class that provides applications with access to Cayenne persistence features. In most
 * cases this is the only access class directly used in the application.
 * <p>
 * Most common DataContext use pattern is to create one DataContext per session. "Session"
 * may be a an HttpSession in a web application, or any other similar concept in a
 * multiuser application.
 * </p>
 * <p>
 * DataObjects are registered with DataContext either implicitly when they are fetched via
 * a query, or read via a relationship from another object, or explicitly via calling
 * {@link #createAndRegisterNewObject(Class)}during new DataObject creation. DataContext
 * tracks changes made to its DataObjects in memory, and flushes them to the database when
 * {@link #commitChanges()}is called. Until DataContext is committed, changes made to its
 * objects are not visible in other DataContexts.
 * </p>
 * <p>
 * Each DataObject can belong only to a single DataContext. To create a replica of an
 * object from a different DataContext in a local context, use
 * {@link #localObject(ObjectId, Persistent)} method.
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
 * <p>
 * <i>Note that all QueryEngine interface methods are deprecated in the DataContext. Since
 * 1.2 release DataContext implements ObjectContext and DataChannel interfaces.</i>
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class DataContext implements ObjectContext, DataChannel, QueryEngine, Serializable {

    public static final EventSubject WILL_COMMIT = EventSubject.getSubject(DataContext.class, "DataContextWillCommit");

    public static final EventSubject DID_COMMIT = EventSubject.getSubject(DataContext.class, "DataContextDidCommit");

    public static final EventSubject DID_ROLLBACK = EventSubject.getSubject(DataContext.class, "DataContextDidRollback");

    protected static final ThreadLocal threadDataContext = new ThreadLocal();

    private static boolean transactionEventsEnabledDefault;

    private boolean transactionEventsEnabled;

    private DataContextDelegate delegate;

    protected boolean usingSharedSnaphsotCache;

    protected boolean validatingObjectsOnCommit;

    protected ObjectStore objectStore;

    protected transient DataChannel channel;

    protected transient EntityResolver entityResolver;

    protected transient DataContextMergeHandler mergeHandler;

    /**
     * Stores user defined properties associated with this DataContext.
     * 
     * @since 1.2
     */
    protected Map userProperties;

    /**
     * Stores the name of parent DataDomain. Used to defer initialization of the parent
     * QueryEngine after deserialization. This helps avoid an issue with certain servlet
     * engines (e.g. Tomcat) where HttpSessions with DataContext's are deserialized at
     * startup before Cayenne stack is fully initialized.
     */
    protected transient String lazyInitParentDomainName;

    /**
     * Returns the DataContext bound to the current thread.
     * 
     * @since 1.1
     * @return the DataContext associated with caller thread.
     * @throws IllegalStateException if there is no DataContext bound to the current
     *             thread.
     * @see org.objectstyle.cayenne.conf.WebApplicationContextFilter
     */
    public static DataContext getThreadDataContext() throws IllegalStateException {
        DataContext dc = (DataContext) threadDataContext.get();
        if (dc == null) {
            throw new IllegalStateException("Current thread has no bound DataContext.");
        }
        return dc;
    }

    /**
     * Binds a DataContext to the current thread. DataContext can later be retrieved by
     * users in the same thread by calling {@link DataContext#getThreadDataContext}.
     * Using null parameter will unbind currently bound DataContext.
     * 
     * @since 1.1
     */
    public static void bindThreadDataContext(DataContext context) {
        threadDataContext.set(context);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on
     * default domain. If more than one domain exists in the current configuration,
     * {@link DataContext#createDataContext(String)} must be used instead. ObjectStore
     * associated with created DataContext will have a cache stack configured using parent
     * domain settings.
     */
    public static DataContext createDataContext() {
        return Configuration.getSharedConfiguration().getDomain().createDataContext();
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on
     * default domain. If more than one domain exists in the current configuration,
     * {@link DataContext#createDataContext(String, boolean)} must be used instead.
     * ObjectStore associated with newly created DataContext will have a cache stack
     * configured according to the specified policy, overriding a parent domain setting.
     * 
     * @since 1.1
     */
    public static DataContext createDataContext(boolean useSharedCache) {
        return Configuration.getSharedConfiguration().getDomain().createDataContext(useSharedCache);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext using named
     * domain as its parent. If there is no domain matching the name argument, an
     * exception is thrown.
     */
    public static DataContext createDataContext(String domainName) {
        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext();
    }

    /**
     * Creates and returns new DataContext that will use a named DataDomain as its parent.
     * ObjectStore associated with newly created DataContext will have a cache stack
     * configured according to the specified policy, overriding a parent domain setting.
     * 
     * @since 1.1
     */
    public static DataContext createDataContext(String domainName, boolean useSharedCache) {
        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext(useSharedCache);
    }

    /**
     * Creates a new DataContext that is not attached to the Cayenne stack.
     */
    public DataContext() {
        this((DataChannel) null, null);
    }

    /**
     * Creates a DataContext with parent QueryEngine and a DataRowStore that should be
     * used by the ObjectStore.
     * 
     * @since 1.1
     * @param parent parent QueryEngine used to communicate with the data source.
     * @param objectStore ObjectStore used by DataContext.
     * @deprecated since 1.2 - use {@link #DataContext(DataChannel, ObjectStore)}
     *             constructor instead. Note that DataDomain is both a DataChannel and a
     *             QueryEngine, so you may need to do a cast:
     *             <code>new DataContext((DataChannel) domain, objectStore)</code>.
     */
    public DataContext(QueryEngine parent, ObjectStore objectStore) {
        this((DataChannel) parent, objectStore);
    }

    /**
     * Creates a new DataContext with parent DataChannel and ObjectStore.
     * 
     * @since 1.2
     */
    public DataContext(DataChannel channel, ObjectStore objectStore) {
        setChannel(channel);
        this.setTransactionEventsEnabled(transactionEventsEnabledDefault);
        if (objectStore != null) {
            this.objectStore = objectStore;
            objectStore.setContext(this);
            DataDomain domain = getParentDataDomain();
            this.usingSharedSnaphsotCache = domain != null && objectStore.getDataRowCache() == domain.getSharedSnapshotCache();
        }
    }

    /**
     * Returns a map of user-defined properties associated with this DataContext.
     * 
     * @since 1.2
     */
    protected Map getUserProperties() {
        if (userProperties == null) {
            userProperties = new HashMap();
        }
        return userProperties;
    }

    /**
     * Creates and returns a new child DataContext.
     * 
     * @since 1.2
     */
    public DataContext createChildDataContext() {
        DataContextFactory factory = getParentDataDomain().getDataContextFactory();
        ObjectStore objectStore = new ObjectStore();
        DataContext child = factory != null ? factory.createDataContext(this, objectStore) : new DataContext((DataChannel) this, objectStore);
        child.setValidatingObjectsOnCommit(isValidatingObjectsOnCommit());
        child.usingSharedSnaphsotCache = isUsingSharedSnapshotCache();
        return child;
    }

    /**
     * Returns a user-defined property previously set via 'setUserProperty'. Note that it
     * is a caller responsibility to synchronize access to properties.
     * 
     * @since 1.2
     */
    public Object getUserProperty(String key) {
        return getUserProperties().get(key);
    }

    /**
     * Sets a user-defined property. Note that it is a caller responsibility to
     * synchronize access to properties.
     * 
     * @since 1.2
     */
    public void setUserProperty(String key, Object value) {
        getUserProperties().put(key, value);
    }

    /**
     * Returns parent QueryEngine object. In most cases returned object is an instance of
     * DataDomain.
     * 
     * @deprecated since 1.2. Use 'getParentDataDomain()' or 'getChannel()' instead.
     */
    public QueryEngine getParent() {
        return getParentDataDomain();
    }

    /**
     * Sets direct parent of this DataContext.
     * 
     * @deprecated since 1.2, use setChannel instead.
     */
    public void setParent(QueryEngine parent) {
        if (parent == null || parent instanceof DataChannel) {
            setChannel((DataChannel) parent);
        } else {
            throw new CayenneRuntimeException("Only parents that implement DataChannel are supported.");
        }
    }

    /**
     * Returns parent DataChannel, that is normally a DataDomain or another DataContext.
     * 
     * @since 1.2
     */
    public DataChannel getChannel() {
        return channel;
    }

    /**
     * @since 1.2
     */
    public void setChannel(DataChannel channel) {
        if (this.channel != channel) {
            if (this.mergeHandler != null) {
                this.mergeHandler.setActive(false);
            }
            this.entityResolver = null;
            this.mergeHandler = null;
            this.channel = channel;
            if (channel != null) {
                this.entityResolver = channel.getEntityResolver();
                EventManager eventManager = channel.getEventManager();
                if (eventManager != null) {
                    this.mergeHandler = new DataContextMergeHandler(this);
                    EventUtil.listenForChannelEvents(channel, mergeHandler);
                }
            }
        }
    }

    /**
     * Returns a DataDomain used by this DataContext. DataDomain is looked up in the
     * DataChannel hierarchy. If a channel is not a DataDomain or a DataContext, null is
     * returned.
     * 
     * @return DataDomain that is a direct or indirect parent of this DataContext in the
     *         DataChannel hierarchy.
     * @since 1.1
     */
    public DataDomain getParentDataDomain() {
        awakeFromDeserialization();
        if (channel == null) {
            return null;
        }
        if (channel instanceof DataDomain) {
            return (DataDomain) channel;
        }
        if (channel instanceof DataContext) {
            return ((DataContext) channel).getParentDataDomain();
        }
        return null;
    }

    /**
     * Sets a DataContextDelegate for this context. Delegate is notified of certain events
     * in the DataContext lifecycle and can customize DataContext behavior.
     * 
     * @since 1.1
     */
    public void setDelegate(DataContextDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a delegate currently associated with this DataContext.
     * 
     * @since 1.1
     */
    public DataContextDelegate getDelegate() {
        return delegate;
    }

    /**
     * @return a delegate instance if it is initialized, or a shared noop implementation
     *         the context has no delegate. Useful to prevent extra null checks and
     *         conditional logic in the code.
     * @since 1.1
     */
    DataContextDelegate nonNullDelegate() {
        return (delegate != null) ? delegate : NoopDelegate.noopDelegate;
    }

    /**
     * Returns ObjectStore associated with this DataContext.
     */
    public ObjectStore getObjectStore() {
        return objectStore;
    }

    /**
     * Returns <code>true</code> if there are any modified, deleted or new objects
     * registered with this DataContext, <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        return getObjectStore().hasChanges();
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.NEW
     */
    public Collection newObjects() {
        return getObjectStore().objectsInState(PersistenceState.NEW);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.DELETED
     */
    public Collection deletedObjects() {
        return getObjectStore().objectsInState(PersistenceState.DELETED);
    }

    /**
     * Returns a list of objects that are registered with this DataContext and have a
     * state PersistenceState.MODIFIED
     */
    public Collection modifiedObjects() {
        return getObjectStore().objectsInState(PersistenceState.MODIFIED);
    }

    /**
     * Returns a collection of all uncommitted registered objects.
     * 
     * @since 1.2
     */
    public Collection uncommittedObjects() {
        int len = getObjectStore().registeredObjectsCount();
        if (len == 0) {
            return Collections.EMPTY_LIST;
        }
        Collection objects = new ArrayList(len > 100 ? len / 2 : len);
        Iterator it = getObjectStore().getObjectIterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
            int state = object.getPersistenceState();
            if (state == PersistenceState.MODIFIED || state == PersistenceState.NEW || state == PersistenceState.DELETED) {
                objects.add(object);
            }
        }
        return objects;
    }

    /**
     * Returns an object for a given ObjectId. When an object is not yet registered with
     * this context's ObjectStore, the behavior of this method depends on whether ObjectId
     * is permanent or temporary and whether a DataContext is a part of a nested context
     * hierarchy or not. More specifically the following rules are applied in order:
     * <ul>
     * <li>If a matching registered object is found in this DataContext, it is
     * immediately returned.</li>
     * <li>If a context is nested (i.e. it has another DataContext as its parent
     * channel), an attempt is made to locate a registered object up the hierarchy chain,
     * until it is found. Such object is transferred to this DataContext and returned to
     * the caller.</li>
     * <li>If the ObjectId is temporary, null is returned; if it is permanent, a HOLLOW
     * object (aka fault) is created and returned.</li>
     * </ul>
     * 
     * @deprecated since 1.2 use 'localObject(id, null)'
     */
    public DataObject registeredObject(ObjectId id) {
        return (DataObject) localObject(id, null);
    }

    /**
     * Returns a DataRow reflecting current, possibly uncommitted, object state.
     * <p>
     * <strong>Warning:</strong> This method will return a partial snapshot if an object
     * or one of its related objects that propagate their keys to this object have
     * temporary ids. DO NOT USE this method if you expect a DataRow to represent a
     * complete object state.
     * </p>
     * 
     * @since 1.1
     */
    public DataRow currentSnapshot(DataObject object) {
        ObjEntity entity = getEntityResolver().lookupObjEntity(object);
        if (object.getPersistenceState() == PersistenceState.HOLLOW && object.getDataContext() != null) {
            return getObjectStore().getSnapshot(object.getObjectId());
        }
        DataRow snapshot = new DataRow(10);
        Iterator attributes = entity.getAttributeMap().entrySet().iterator();
        while (attributes.hasNext()) {
            Map.Entry entry = (Map.Entry) attributes.next();
            String attrName = (String) entry.getKey();
            ObjAttribute objAttr = (ObjAttribute) entry.getValue();
            snapshot.put(objAttr.getDbAttributePath(), object.readPropertyDirectly(attrName));
        }
        Iterator relationships = entity.getRelationshipMap().entrySet().iterator();
        while (relationships.hasNext()) {
            Map.Entry entry = (Map.Entry) relationships.next();
            ObjRelationship rel = (ObjRelationship) entry.getValue();
            if (rel.isSourceIndependentFromTargetChange()) {
                continue;
            }
            Object targetObject = object.readPropertyDirectly(rel.getName());
            if (targetObject == null) {
                continue;
            }
            if (targetObject instanceof Fault) {
                DataRow storedSnapshot = getObjectStore().getSnapshot(object.getObjectId());
                if (storedSnapshot == null) {
                    throw new CayenneRuntimeException("No matching objects found for ObjectId " + object.getObjectId() + ". Object may have been deleted externally.");
                }
                DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
                Iterator joins = dbRel.getJoins().iterator();
                while (joins.hasNext()) {
                    DbJoin join = (DbJoin) joins.next();
                    String key = join.getSourceName();
                    snapshot.put(key, storedSnapshot.get(key));
                }
                continue;
            }
            DataObject target = (DataObject) targetObject;
            Map idParts = target.getObjectId().getIdSnapshot();
            if (idParts.isEmpty()) {
                continue;
            }
            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
            Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
            snapshot.putAll(fk);
        }
        Map thisIdParts = object.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {
            Iterator idIterator = thisIdParts.entrySet().iterator();
            while (idIterator.hasNext()) {
                Map.Entry entry = (Map.Entry) idIterator.next();
                Object nextKey = entry.getKey();
                if (!snapshot.containsKey(nextKey)) {
                    snapshot.put(nextKey, entry.getValue());
                }
            }
        }
        return snapshot;
    }

    /**
     * Creates a list of DataObjects local to this DataContext from a list of DataObjects
     * coming from a different DataContext. This method is a way to <b>map</b> objects
     * from one context into the other (as opposed to "synchronize"). This means that the
     * state of modified objects will be reflected only if this context is a child of an
     * original DataObject context. If it is a peer or parent, you won't see any
     * uncommitted changes from the original context.
     * <p>
     * Note that the objects in the list do not have to be of the same type or even from
     * the same DataContext.
     * 
     * @since 1.0.3
     * @deprecated since 1.2 - use {@link #localObject(ObjectId, Persistent)} to specify
     *             how each local object should be handled.
     */
    public List localObjects(List objects) {
        List localObjects = new ArrayList(objects.size());
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            if (object == null) {
                throw new CayenneRuntimeException("Null object");
            }
            localObjects.add(localObject(object.getObjectId(), null));
        }
        return localObjects;
    }

    /**
     * Converts a list of data rows to a list of DataObjects.
     * 
     * @since 1.1
     */
    public List objectsFromDataRows(ObjEntity entity, List dataRows, boolean refresh, boolean resolveInheritanceHierarchy) {
        return new ObjectResolver(this, entity, refresh, resolveInheritanceHierarchy).synchronizedObjectsFromDataRows(dataRows);
    }

    /**
     * Converts a list of DataRows to a List of DataObject registered with this
     * DataContext. Internally calls
     * {@link #objectsFromDataRows(ObjEntity,List,boolean,boolean)}.
     * 
     * @since 1.1
     * @see DataRow
     * @see DataObject
     */
    public List objectsFromDataRows(Class objectClass, List dataRows, boolean refresh, boolean resolveInheritanceHierarchy) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objectClass);
        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped Java class: " + objectClass);
        }
        return objectsFromDataRows(entity, dataRows, refresh, resolveInheritanceHierarchy);
    }

    /**
     * Creates a DataObject from DataRow. This is a convenience shortcut to
     * {@link #objectsFromDataRows(Class,java.util.List,boolean,boolean)}.
     * 
     * @see DataRow
     * @see DataObject
     */
    public DataObject objectFromDataRow(Class objectClass, DataRow dataRow, boolean refresh) {
        List list = objectsFromDataRows(objectClass, Collections.singletonList(dataRow), refresh, true);
        return (DataObject) list.get(0);
    }

    /**
     * Instantiates new object and registers it with itself. Object class is determined
     * from ObjEntity. Object class must have a default constructor.
     * <p>
     * <i>Note: preferred way to create new objects is via
     * {@link #createAndRegisterNewObject(Class)}method. It works exactly the same way,
     * but makes the application type-safe. </i>
     * </p>
     * 
     * @see #createAndRegisterNewObject(Class)
     */
    public DataObject createAndRegisterNewObject(String objEntityName) {
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(objEntityName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid entity name: " + objEntityName);
        }
        DataObject dataObject;
        try {
            dataObject = (DataObject) descriptor.createObject();
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }
        descriptor.injectValueHolders(dataObject);
        dataObject.setObjectId(new ObjectId(objEntityName));
        dataObject.setDataContext(this);
        dataObject.setPersistenceState(PersistenceState.NEW);
        getObjectStore().recordObjectCreated(dataObject);
        return dataObject;
    }

    /**
     * Creates and registers new persistent object. This is an ObjectContext version of
     * 'createAndRegisterNewObject'.
     * 
     * @since 1.2
     */
    public Persistent newObject(Class persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Null 'persistentClass'");
        }
        if (!DataObject.class.isAssignableFrom(persistentClass)) {
            throw new IllegalArgumentException(this + ": this implementation of ObjectContext only supports full DataObjects. Class " + persistentClass + " is invalid.");
        }
        return createAndRegisterNewObject(persistentClass);
    }

    /**
     * Instantiates new object and registers it with itself. Object class must have a
     * default constructor.
     * 
     * @since 1.1
     */
    public DataObject createAndRegisterNewObject(Class objectClass) {
        if (objectClass == null) {
            throw new NullPointerException("DataObject class can't be null.");
        }
        ObjEntity entity = getEntityResolver().lookupObjEntity(objectClass);
        if (entity == null) {
            throw new IllegalArgumentException("Class is not mapped with Cayenne: " + objectClass.getName());
        }
        return createAndRegisterNewObject(entity.getName());
    }

    /**
     * Registers a transient object with the context, recursively registering all
     * transient DataObjects attached to this object via relationships.
     * 
     * @param object new object that needs to be made persistent.
     */
    public void registerNewObject(final DataObject object) {
        if (object == null) {
            throw new NullPointerException("Can't register null object.");
        }
        ObjEntity entity = getEntityResolver().lookupObjEntity(object);
        if (entity == null) {
            throw new IllegalArgumentException("Can't find ObjEntity for DataObject class: " + object.getClass().getName() + ", class is likely not mapped.");
        }
        if (object.getObjectId() != null) {
            if (object.getDataContext() == this) {
                return;
            } else if (object.getDataContext() != null) {
                throw new IllegalStateException("DataObject is already registered with another DataContext. " + "Try using 'localObjects()' instead.");
            }
        } else {
            object.setObjectId(new ObjectId(entity.getName()));
        }
        object.setDataContext(this);
        object.setPersistenceState(PersistenceState.NEW);
        getObjectStore().recordObjectCreated(object);
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(entity.getName());
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid entity name: " + entity.getName());
        }
        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitCollectionArc(CollectionProperty property) {
                property.injectValueHolder(object);
                if (!property.isFault(object)) {
                    Iterator it = ((Collection) property.readProperty(object)).iterator();
                    while (it.hasNext()) {
                        Object target = it.next();
                        if (target instanceof DataObject) {
                            DataObject targetDO = (DataObject) target;
                            registerNewObject(targetDO);
                            getObjectStore().recordArcCreated(object, targetDO.getObjectId(), property.getName());
                        }
                    }
                }
                return true;
            }

            public boolean visitSingleObjectArc(SingleObjectArcProperty property) {
                Object target = property.readPropertyDirectly(object);
                if (target instanceof DataObject) {
                    DataObject targetDO = (DataObject) target;
                    registerNewObject(targetDO);
                    getObjectStore().recordArcCreated(object, targetDO.getObjectId(), property.getName());
                }
                return true;
            }

            public boolean visitProperty(Property property) {
                return true;
            }
        });
    }

    /**
     * Unregisters a Collection of DataObjects from the DataContext and the underlying
     * ObjectStore. This operation also unsets DataContext and ObjectId for each object
     * and changes its state to TRANSIENT.
     * 
     * @see #invalidateObjects(Collection)
     */
    public void unregisterObjects(Collection dataObjects) {
        getObjectStore().objectsUnregistered(dataObjects);
    }

    /**
     * "Invalidates" a Collection of DataObject. This operation would remove each object's
     * snapshot from cache and change object's state to HOLLOW. On the next access to this
     * object, it will be refetched.
     * 
     * @see #unregisterObjects(Collection)
     */
    public void invalidateObjects(Collection dataObjects) {
        getObjectStore().objectsInvalidated(dataObjects);
    }

    /**
     * Schedules all objects in the collection for deletion on the next commit of this
     * DataContext. Object's persistence state is changed to PersistenceState.DELETED;
     * objects related to this object are processed according to delete rules, i.e.
     * relationships can be unset ("nullify" rule), deletion operation is cascaded
     * (cascade rule).
     * <p>
     * <i>"Nullify" delete rule side effect: </i> passing a collection representing
     * to-many relationship with nullify delete rule may result in objects being removed
     * from collection.
     * </p>
     * 
     * @since 1.2
     */
    public void deleteObjects(Collection objects) {
        if (objects.isEmpty()) {
            return;
        }
        Iterator it = new ArrayList(objects).iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            deleteObject(object);
        }
    }

    /**
     * Schedules an object for deletion on the next commit of this DataContext. Object's
     * persistence state is changed to PersistenceState.DELETED; objects related to this
     * object are processed according to delete rules, i.e. relationships can be unset
     * ("nullify" rule), deletion operation is cascaded (cascade rule).
     * 
     * @param object a persistent object that we want to delete.
     * @throws DeleteDenyException if a DENY delete rule is applicable for object
     *             deletion.
     * @throws NullPointerException if object is null.
     */
    public void deleteObject(Persistent object) throws DeleteDenyException {
        new DataContextDeleteAction(this).performDelete(object);
    }

    /**
     * Refetches object data for ObjectId. This method is used internally by Cayenne to
     * resolve objects in state <code>PersistenceState.HOLLOW</code>. It can also be
     * used to refresh certain objects.
     * 
     * @throws CayenneRuntimeException if object id doesn't match any records, or if there
     *             is more than one object is fetched.
     */
    public DataObject refetchObject(ObjectId oid) {
        if (oid == null) {
            throw new NullPointerException("Null ObjectId");
        }
        if (oid.isTemporary()) {
            throw new CayenneRuntimeException("Can't refetch ObjectId " + oid + ", as it is a temporary id.");
        }
        synchronized (getObjectStore()) {
            DataObject object = (DataObject) objectStore.getNode(oid);
            if (object != null) {
                this.invalidateObjects(Collections.singleton(object));
            }
        }
        DataObject object = (DataObject) DataObjectUtils.objectForQuery(this, new ObjectIdQuery(oid));
        if (object == null) {
            throw new CayenneRuntimeException("Refetch failure: no matching objects found for ObjectId " + oid);
        }
        return object;
    }

    /**
     * Returns a DataNode that should handle queries for all DataMap components.
     * 
     * @since 1.1
     * @deprecated since 1.2 DataContext's QueryEngine implementation is replaced by
     *             DataChannel. Use "getParentDataDomain().lookupDataNode(..)".
     */
    public DataNode lookupDataNode(DataMap dataMap) {
        DataDomain domain = getParentDataDomain();
        if (domain == null) {
            throw new CayenneRuntimeException("DataContext is not attached to a DataDomain ");
        }
        return domain.lookupDataNode(dataMap);
    }

    public void rollbackChangesLocally() {
        if (getChannel() instanceof DataDomain) {
            rollbackChanges();
        } else {
            throw new CayenneRuntimeException("Implementation pending.");
        }
    }

    /**
     * Reverts any changes that have occurred to objects registered with DataContext; also
     * performs cascading rollback of all parent DataContexts.
     */
    public void rollbackChanges() {
        if (objectStore.hasChanges()) {
            GraphDiff diff = getObjectStore().getChanges();
            getObjectStore().objectsRolledBack();
            if (channel != null) {
                channel.onSync(this, null, DataChannel.ROLLBACK_CASCADE_SYNC);
            }
            fireDataChannelRolledback(this, diff);
        }
    }

    /**
     * "Flushes" the changes to the parent {@link DataChannel}. If the parent channel is
     * a DataContext, it updates its objects with this context's changes, without a
     * database update. If it is a DataDomain (the most common case), the changes are
     * written to the database. To cause cascading commit all the way to the database, one
     * must use {@link #commitChanges()}.
     * 
     * @since 1.2
     * @see #commitChanges()
     */
    public void commitChangesToParent() {
        flushToParent(false);
    }

    /**
     * @deprecated Since 1.2, use {@link #commitChanges()} instead.
     */
    public void commitChanges(Level logLevel) throws CayenneRuntimeException {
        commitChanges();
    }

    /**
     * Synchronizes object graph with the database. Executes needed insert, update and
     * delete queries (generated internally).
     */
    public void commitChanges() throws CayenneRuntimeException {
        flushToParent(true);
    }

    /**
     * Returns EventManager associated with the ObjectStore.
     * 
     * @since 1.2
     */
    public EventManager getEventManager() {
        return channel != null ? channel.getEventManager() : null;
    }

    /**
     * An implementation of a {@link DataChannel} method that is used by child contexts to
     * synchronize state with this context. Not intended for direct use.
     * 
     * @since 1.2
     */
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {
        switch(syncType) {
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                return onContextRollback(originatingContext);
            case DataChannel.FLUSH_NOCASCADE_SYNC:
                return onContextFlush(originatingContext, changes, false);
            case DataChannel.FLUSH_CASCADE_SYNC:
                return onContextFlush(originatingContext, changes, true);
            default:
                throw new CayenneRuntimeException("Unrecognized SyncMessage type: " + syncType);
        }
    }

    GraphDiff onContextRollback(ObjectContext originatingContext) {
        rollbackChanges();
        return (channel != null) ? channel.onSync(this, null, DataChannel.ROLLBACK_CASCADE_SYNC) : new CompoundDiff();
    }

    GraphDiff onContextFlush(ObjectContext originatingContext, GraphDiff changes, boolean cascade) {
        if (this != originatingContext && changes != null) {
            changes.apply(new ChildDiffLoader(this));
            fireDataChannelChanged(originatingContext, changes);
        }
        return (cascade) ? flushToParent(true) : new CompoundDiff();
    }

    /**
     * Synchronizes with the parent channel, performing a flush or a commit.
     * 
     * @since 1.2
     */
    GraphDiff flushToParent(boolean cascade) {
        if (this.getChannel() == null) {
            throw new CayenneRuntimeException("Cannot commit changes - channel is not set.");
        }
        int syncType = cascade ? DataChannel.FLUSH_CASCADE_SYNC : DataChannel.FLUSH_NOCASCADE_SYNC;
        synchronized (getObjectStore()) {
            DataContextFlushEventHandler eventHandler = null;
            ObjectStoreGraphDiff changes = getObjectStore().getChanges();
            boolean noop = isValidatingObjectsOnCommit() ? changes.validateAndCheckNoop() : changes.isNoop();
            if (noop) {
                getObjectStore().postprocessAfterPhantomCommit();
                return new CompoundDiff();
            }
            if (isTransactionEventsEnabled()) {
                eventHandler = new DataContextFlushEventHandler(this);
                eventHandler.registerForDataContextEvents();
                fireWillCommit();
            }
            try {
                GraphDiff returnChanges = getChannel().onSync(this, changes, syncType);
                getObjectStore().postprocessAfterCommit(returnChanges);
                fireTransactionCommitted();
                fireDataChannelCommitted(this, changes);
                if (!returnChanges.isNoop()) {
                    fireDataChannelCommitted(getChannel(), returnChanges);
                }
                return returnChanges;
            } catch (CayenneRuntimeException ex) {
                fireTransactionRolledback();
                Throwable unwound = Util.unwindException(ex);
                if (unwound instanceof CayenneRuntimeException) {
                    throw (CayenneRuntimeException) unwound;
                } else {
                    throw new CayenneRuntimeException("Commit Exception", unwound);
                }
            } finally {
                if (isTransactionEventsEnabled()) {
                    eventHandler.unregisterFromDataContextEvents();
                }
            }
        }
    }

    /**
     * Performs a single database select query returning result as a ResultIterator.
     * Returned ResultIterator will provide access to DataRows.
     */
    public ResultIterator performIteratedQuery(Query query) throws CayenneException {
        IteratedSelectObserver observer = new IteratedSelectObserver();
        getParentDataDomain().performQueries(Collections.singletonList(query), observer);
        return observer.getResultIterator();
    }

    /**
     * Executes a query returning a generic response.
     * 
     * @since 1.2
     */
    public QueryResponse performGenericQuery(Query query) {
        query = nonNullDelegate().willPerformGenericQuery(this, query);
        if (query == null) {
            return new GenericResponse();
        }
        if (this.getChannel() == null) {
            throw new CayenneRuntimeException("Can't run query - parent DataChannel is not set.");
        }
        return onQuery(this, query);
    }

    /**
     * Performs a single selecting query. Various query setting control the behavior of
     * this method and the results returned:
     * <ul>
     * <li>Query caching policy defines whether the results are retrieved from cache or
     * fetched from the database. Note that queries that use caching must have a name that
     * is used as a caching key.</li>
     * <li>Query refreshing policy controls whether to refresh existing data objects and
     * ignore any cached values.</li>
     * <li>Query data rows policy defines whether the result should be returned as
     * DataObjects or DataRows.</li>
     * </ul>
     * <p>
     * <i>Since 1.2 takes any Query parameter, not just GenericSelectQuery</i>
     * </p>
     * 
     * @return A list of DataObjects or a DataRows, depending on the value returned by
     *         {@link QueryMetadata#isFetchingDataRows()}.
     */
    public List performQuery(Query query) {
        query = nonNullDelegate().willPerformQuery(this, query);
        if (query == null) {
            return new ArrayList(1);
        }
        query = filterThroughDelegateDeprecated(query);
        if (query == null) {
            return new ArrayList(1);
        }
        List result = onQuery(this, query).firstList();
        return result != null ? result : new ArrayList(1);
    }

    /**
     * An implementation of a {@link DataChannel} method that is used by child contexts to
     * execute queries. Not intended for direct use.
     * 
     * @since 1.2
     */
    public QueryResponse onQuery(ObjectContext context, Query query) {
        return new DataContextQueryAction(this, context, query).execute();
    }

    /**
     * Performs a single database query that does not select rows. Returns an array of
     * update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(Query query) {
        int[] count = performGenericQuery(query).firstUpdateCount();
        return count != null ? count : new int[0];
    }

    /**
     * Performs a named mapped query that does not select rows. Returns an array of update
     * counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName) {
        return performNonSelectingQuery(new NamedQuery(queryName));
    }

    /**
     * Performs a named mapped non-selecting query using a map of parameters. Returns an
     * array of update counts.
     * 
     * @since 1.1
     */
    public int[] performNonSelectingQuery(String queryName, Map parameters) {
        return performNonSelectingQuery(new NamedQuery(queryName, parameters));
    }

    /**
     * Executes all queries in collection.
     * 
     * @deprecated since 1.2 DataContext's QueryEngine implementation is replaced by
     *             DataChannel.
     */
    public void performQueries(Collection queries, OperationObserver callback) {
        List finalQueries = new ArrayList(queries.size());
        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Query query = (Query) it.next();
            query = filterThroughDelegateDeprecated(query);
            if (query != null) {
                finalQueries.add(query);
            }
        }
        if (!finalQueries.isEmpty()) {
            getParentDataDomain().performQueries(queries, callback);
        }
    }

    Query filterThroughDelegateDeprecated(Query query) {
        if (query instanceof org.objectstyle.cayenne.query.GenericSelectQuery) {
            org.objectstyle.cayenne.query.GenericSelectQuery genericSelect = (org.objectstyle.cayenne.query.GenericSelectQuery) query;
            return nonNullDelegate().willPerformSelect(this, genericSelect);
        }
        return query;
    }

    /**
     * Binds provided transaction to the current thread, and then runs queries.
     * 
     * @since 1.1
     * @deprecated since 1.2. Use Transaction.bindThreadTransaction(..) to provide custom
     *             transactions, besides DataContext's QueryEngine implementation is
     *             replaced by DataChannel.
     */
    public void performQueries(Collection queries, OperationObserver callback, Transaction transaction) {
        Transaction.bindThreadTransaction(transaction);
        try {
            performQueries(queries, callback);
        } finally {
            Transaction.bindThreadTransaction(null);
        }
    }

    /**
     * Performs prefetching. Prefetching would resolve a set of relationships for a list
     * of DataObjects in the most optimized way (preferrably in a single query per
     * relationship).
     * <p>
     * <i>WARNING: Currently supports only "one-step" to one relationships. This is an
     * arbitrary limitation and will be removed eventually. </i>
     * </p>
     * 
     * @deprecated Since 1.2. This is a utility method that handles a very specific case.
     *             It shouldn't be in DataContext.
     */
    public void prefetchRelationships(SelectQuery query, List objects) {
        QueryMetadata metadata = query.getMetaData(getEntityResolver());
        Collection prefetches = metadata.getPrefetchTree() != null ? query.getPrefetchTree().nonPhantomNodes() : Collections.EMPTY_LIST;
        if (objects == null || objects.size() == 0 || prefetches.size() == 0) {
            return;
        }
        ObjEntity entity = metadata.getObjEntity();
        Iterator prefetchesIt = prefetches.iterator();
        while (prefetchesIt.hasNext()) {
            PrefetchTreeNode prefetch = (PrefetchTreeNode) prefetchesIt.next();
            String path = prefetch.getPath();
            if (path.indexOf('.') >= 0) {
                throw new CayenneRuntimeException("Only one-step relationships are " + "supported at the moment, this will be fixed soon. " + "Unsupported path : " + path);
            }
            ObjRelationship relationship = (ObjRelationship) entity.getRelationship(path);
            if (relationship == null) {
                throw new CayenneRuntimeException("Invalid relationship: " + path);
            }
            if (relationship.isToMany()) {
                throw new CayenneRuntimeException("Only to-one relationships are supported at the moment. " + "Can't prefetch to-many: " + path);
            }
            org.objectstyle.cayenne.access.util.PrefetchHelper.resolveToOneRelations(this, objects, path);
        }
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of the
     * DataMaps. Internally Cayenne uses a caching policy defined in the named query. If
     * refresh flag is true, a refresh is forced no matter what the caching policy is.
     * 
     * @param queryName a name of a GenericSelectQuery defined in one of the DataMaps. If
     *            no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param expireCachedLists A flag that determines whether refresh of <b>cached lists</b>
     *            is required in case a query uses caching.
     * @since 1.1
     */
    public List performQuery(String queryName, boolean expireCachedLists) {
        return performQuery(queryName, Collections.EMPTY_MAP, expireCachedLists);
    }

    /**
     * Returns a list of objects or DataRows for a named query stored in one of the
     * DataMaps. Internally Cayenne uses a caching policy defined in the named query. If
     * refresh flag is true, a refresh is forced no matter what the caching policy is.
     * 
     * @param queryName a name of a GenericSelectQuery defined in one of the DataMaps. If
     *            no such query is defined, this method will throw a
     *            CayenneRuntimeException.
     * @param parameters A map of parameters to use with stored query.
     * @param expireCachedLists A flag that determines whether refresh of <b>cached lists</b>
     *            is required in case a query uses caching.
     * @since 1.1
     */
    public List performQuery(String queryName, Map parameters, boolean expireCachedLists) {
        NamedQuery query = new NamedQuery(queryName, parameters);
        query.setForceNoCache(expireCachedLists);
        return performQuery(query);
    }

    /**
     * Returns EntityResolver. EntityResolver can be null if DataContext has not been
     * attached to an DataChannel.
     */
    public EntityResolver getEntityResolver() {
        awakeFromDeserialization();
        return entityResolver;
    }

    /**
     * Sets default for posting transaction events by new DataContexts.
     */
    public static void setTransactionEventsEnabledDefault(boolean flag) {
        transactionEventsEnabledDefault = flag;
    }

    /**
     * Enables or disables posting of transaction events by this DataContext.
     */
    public void setTransactionEventsEnabled(boolean flag) {
        this.transactionEventsEnabled = flag;
    }

    public boolean isTransactionEventsEnabled() {
        return this.transactionEventsEnabled;
    }

    /**
     * Returns <code>true</code> if the ObjectStore uses shared cache of a parent
     * DataDomain.
     * 
     * @since 1.1
     */
    public boolean isUsingSharedSnapshotCache() {
        return usingSharedSnaphsotCache;
    }

    /**
     * Returns whether this DataContext performs object validation before commit is
     * executed.
     * 
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether this DataContext should perform object
     * validation before commit is executed.
     * 
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    /**
     * @deprecated since 1.2. Use 'getEntityResolver().getDataMaps()' instead.
     */
    public Collection getDataMaps() {
        return (getEntityResolver() != null) ? getEntityResolver().getDataMaps() : Collections.EMPTY_LIST;
    }

    void fireWillCommit() {
        if (this.transactionEventsEnabled) {
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            getEventManager().postEvent(commitChangesEvent, DataContext.WILL_COMMIT);
        }
    }

    void fireTransactionRolledback() {
        if ((this.transactionEventsEnabled)) {
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            getEventManager().postEvent(commitChangesEvent, DataContext.DID_ROLLBACK);
        }
    }

    void fireTransactionCommitted() {
        if ((this.transactionEventsEnabled)) {
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            getEventManager().postEvent(commitChangesEvent, DataContext.DID_COMMIT);
        }
    }

    /**
     * @since 1.2
     */
    void fireDataChannelCommitted(Object postedBy, GraphDiff changes) {
        EventManager manager = getEventManager();
        if (manager != null) {
            GraphEvent e = new GraphEvent(this, postedBy, changes);
            manager.postEvent(e, DataChannel.GRAPH_FLUSHED_SUBJECT);
        }
    }

    /**
     * @since 1.2
     */
    void fireDataChannelRolledback(Object postedBy, GraphDiff changes) {
        EventManager manager = getEventManager();
        if (manager != null) {
            GraphEvent e = new GraphEvent(this, postedBy, changes);
            manager.postEvent(e, DataChannel.GRAPH_ROLLEDBACK_SUBJECT);
        }
    }

    /**
     * @since 1.2
     */
    void fireDataChannelChanged(Object postedBy, GraphDiff changes) {
        EventManager manager = getEventManager();
        if (manager != null) {
            GraphEvent e = new GraphEvent(this, postedBy, changes);
            manager.postEvent(e, DataChannel.GRAPH_CHANGED_SUBJECT);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (this.channel == null && this.lazyInitParentDomainName != null) {
            out.writeObject(lazyInitParentDomainName);
        } else if (this.channel instanceof DataDomain) {
            DataDomain domain = (DataDomain) this.channel;
            out.writeObject(domain.getName());
        } else {
            out.writeObject(this.channel);
        }
        if (!isUsingSharedSnapshotCache()) {
            out.writeObject(objectStore.getDataRowCache());
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Object value = in.readObject();
        if (value instanceof DataChannel) {
            this.channel = (DataChannel) value;
        } else if (value instanceof String) {
            this.lazyInitParentDomainName = (String) value;
        } else {
            throw new CayenneRuntimeException("Parent attribute of DataContext was neither a QueryEngine nor " + "the name of a valid DataDomain:" + value);
        }
        if (!isUsingSharedSnapshotCache()) {
            DataRowStore cache = (DataRowStore) in.readObject();
            objectStore.setDataRowCache(cache);
        }
        synchronized (getObjectStore()) {
            Iterator it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                DataObject object = (DataObject) it.next();
                object.setDataContext(this);
            }
        }
    }

    private final void awakeFromDeserialization() {
        if (channel == null && lazyInitParentDomainName != null) {
            setChannel(Configuration.getSharedConfiguration().getDomain(lazyInitParentDomainName));
        }
    }

    /**
     * Resolves object fault if needed. If a property is not null, it is assumed that the
     * object will be modified, so object snapshot is retained and object state is
     * changed.
     * 
     * @since 1.2
     */
    public void prepareForAccess(Persistent object, String property) {
        if (object.getPersistenceState() == PersistenceState.HOLLOW) {
            if (!(object instanceof DataObject)) {
                throw new CayenneRuntimeException("Can only resolve DataObjects. Got: " + object);
            }
            getObjectStore().resolveHollow((DataObject) object);
            if (object.getPersistenceState() != PersistenceState.COMMITTED) {
                String state = PersistenceState.persistenceStateName(object.getPersistenceState());
                throw new FaultFailureException("Error resolving fault for ObjectId: " + object.getObjectId() + " and state (" + state + "). Possible cause - matching row is missing from the database.");
            }
        }
    }

    /**
     * Retains DataObject snapshot and changes its state if needed.
     * 
     * @since 1.2
     */
    public void propertyChanged(Persistent object, String property, Object oldValue, Object newValue) {
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            getObjectStore().registerDiff(object, null);
        }
    }

    /**
     * Returns this context's ObjectStore.
     * 
     * @since 1.2
     */
    public GraphManager getGraphManager() {
        return objectStore;
    }

    /**
     * Returns an object local to this DataContext and matching the ObjectId. If
     * <code>prototype</code> is not null, local object is refreshed with the prototype
     * values.
     * <p>
     * In case you pass a non-null second parameter, you are responsible for setting
     * correct persistence state of the returned local object, as generally there is no
     * way for Cayenne to determine the resulting local object state.
     * 
     * @since 1.2
     */
    public Persistent localObject(ObjectId id, Persistent prototype) {
        if (id == null) {
            throw new IllegalArgumentException("Null ObjectId");
        }
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(id.getEntityName());
        Persistent cachedObject = (Persistent) getGraphManager().getNode(id);
        if (cachedObject != null) {
            if (cachedObject != prototype && cachedObject.getPersistenceState() != PersistenceState.MODIFIED && cachedObject.getPersistenceState() != PersistenceState.DELETED) {
                descriptor.injectValueHolders(cachedObject);
                if (prototype != null && prototype.getPersistenceState() != PersistenceState.HOLLOW) {
                    descriptor.shallowMerge(prototype, cachedObject);
                    if (cachedObject.getPersistenceState() == PersistenceState.HOLLOW) {
                        cachedObject.setPersistenceState(PersistenceState.COMMITTED);
                    }
                }
            }
            return cachedObject;
        } else {
            Persistent localObject = (Persistent) descriptor.createObject();
            localObject.setObjectContext(this);
            localObject.setObjectId(id);
            getGraphManager().registerNode(id, localObject);
            if (prototype != null && prototype.getPersistenceState() != PersistenceState.HOLLOW) {
                localObject.setPersistenceState(PersistenceState.COMMITTED);
                descriptor.injectValueHolders(localObject);
                descriptor.shallowMerge(prototype, localObject);
            } else {
                localObject.setPersistenceState(PersistenceState.HOLLOW);
            }
            return localObject;
        }
    }
}
