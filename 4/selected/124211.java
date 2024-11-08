package oracle.toplink.essentials.internal.sessions;

import java.io.*;
import java.util.HashSet;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import oracle.toplink.essentials.queryframework.*;
import oracle.toplink.essentials.internal.helper.ConversionManager;
import oracle.toplink.essentials.internal.helper.ClassConstants;
import oracle.toplink.essentials.mappings.*;
import oracle.toplink.essentials.internal.identitymaps.CacheKey;
import oracle.toplink.essentials.descriptors.ClassDescriptor;
import oracle.toplink.essentials.internal.descriptors.OptimisticLockingPolicy;

/**
 * <p>
 * <b>Purpose</b>: Hold the Records of change for a particular instance of an object.
 * <p>
 * <b>Description</b>: This class uses the Primary Keys of the Object it represents,
 * and the class.
 * <p>
 */
public class ObjectChangeSet implements Serializable, oracle.toplink.essentials.changesets.ObjectChangeSet {

    /** This is the collection of changes */
    protected java.util.Vector changes;

    protected java.util.Hashtable attributesToChanges;

    protected boolean shouldBeDeleted;

    protected CacheKey cacheKey;

    protected transient Class classType;

    protected String className;

    protected boolean isNew;

    protected boolean isAggregate;

    protected Object oldKey;

    protected Object newKey;

    /** This member variable holds the reference to the parent UnitOfWork Change Set **/
    protected UnitOfWorkChangeSet unitOfWorkChangeSet;

    /** Used in mergeObjectChanges method for writeLock and initialWriteLock comparison of the merged change sets **/
    protected transient OptimisticLockingPolicy optimisticLockingPolicy;

    protected Object initialWriteLockValue;

    protected Object writeLockValue;

    /** Invalid change set shouldn't be merged into object in cache, rather the object should be invalidated **/
    protected boolean isInvalid;

    protected transient Object cloneObject;

    protected boolean hasVersionChange;

    protected Boolean shouldModifyVersionField;

    protected boolean hasCmpPolicyForcedUpdate;

    protected boolean hasChangesFromCascadeLocking;

    /** This is used during attribute level change tracking when a particular
     * change was detected but that change can not be tracked (ie customer set
     * entire collection in object).
     */
    protected transient HashSet deferredSet;

    /**
     * The default constructor is used only by SDK XML project for mapping ObjectChangeSet
     */
    public ObjectChangeSet() {
        super();
    }

    /**
     * This constructor is used to create an ObjectChangeSet that represents an aggregate object.
     */
    public ObjectChangeSet(Object cloneObject, UnitOfWorkChangeSet parent, boolean isNew) {
        this.cloneObject = cloneObject;
        this.shouldBeDeleted = false;
        this.classType = cloneObject.getClass();
        this.className = this.classType.getName();
        this.unitOfWorkChangeSet = parent;
        this.isNew = isNew;
    }

    /**
     * This constructor is used to create an ObjectChangeSet that represents a regular object.
     */
    public ObjectChangeSet(Vector primaryKey, Class classType, Object cloneObject, UnitOfWorkChangeSet parent, boolean isNew) {
        super();
        this.cloneObject = cloneObject;
        this.isNew = isNew;
        this.shouldBeDeleted = false;
        if ((primaryKey != null) && !oracle.toplink.essentials.internal.helper.Helper.containsNull(primaryKey, 0)) {
            this.cacheKey = new CacheKey(primaryKey);
        }
        this.classType = classType;
        this.className = this.classType.getName();
        this.unitOfWorkChangeSet = parent;
        this.isAggregate = false;
    }

    /**
     * INTERNAL:
     * This method will clear the changerecords from a changeSet
     */
    public void clear() {
        this.shouldBeDeleted = false;
        this.setOldKey(null);
        this.setNewKey(null);
        this.changes = null;
        this.attributesToChanges = null;
    }

    /**
     * @param changeRecord prototype.changeset.ChangeRecord
     */
    public void addChange(ChangeRecord changeRecord) {
        if (changeRecord == null) {
            return;
        }
        ChangeRecord existingChangeRecord = (ChangeRecord) getAttributesToChanges().get(changeRecord.getAttribute());
        if (existingChangeRecord != null) {
            getChanges().remove(existingChangeRecord);
        }
        getChanges().addElement(changeRecord);
        getAttributesToChanges().put(changeRecord.getAttribute(), changeRecord);
        updateUOWChangeSet();
    }

    /**
     * INTERNAL:
     * This method is used during attribute level change tracking when a particular
     * change was detected but that change can not be tracked (ie customer set
     * entire collection in object).  In this case flag this attribute for 
     * deferred change detection at commit time.
     */
    public void deferredDetectionRequiredOn(String attributeName) {
        getDeferredSet().add(attributeName);
    }

    /**
     * INTERNAL:
     * Convenience method used to query this change set after it has been sent by
     * cache synchronization.
     * @return true if this change set should contain all change information, false if only
     * the identity information should be available.
     */
    public boolean containsChangesFromSynchronization() {
        return false;
    }

    /**
     * @return boolean
     * @param objectChange prototype.changeset.ObjectChangeSet
     */
    public boolean equals(Object object) {
        if (object instanceof ObjectChangeSet) {
            return equals((ObjectChangeSet) object);
        }
        return false;
    }

    /**
     * @return boolean
     * @param objectChange prototype.changeset.ObjectChangeSet
     */
    public boolean equals(oracle.toplink.essentials.changesets.ObjectChangeSet objectChange) {
        if (this == objectChange) {
            return true;
        } else if (getCacheKey() == null) {
            return false;
        }
        return (getCacheKey().equals(((ObjectChangeSet) objectChange).getCacheKey()));
    }

    /**
     * INTERNAL:
     * stores the change records indexed by the attribute names
     */
    public Hashtable getAttributesToChanges() {
        if (this.attributesToChanges == null) {
            this.attributesToChanges = new Hashtable(2);
        }
        return this.attributesToChanges;
    }

    /**
     * INTERNAL:
     * returns the change record for the specified attribute name
     */
    public oracle.toplink.essentials.changesets.ChangeRecord getChangesForAttributeNamed(String attributeName) {
        return (ChangeRecord) this.getAttributesToChanges().get(attributeName);
    }

    /**
     * @return java.util.Vector
     */
    public CacheKey getCacheKey() {
        return cacheKey;
    }

    /**
     * ADVANCED:
     * This method will return a collection of the fieldnames of attributes changed in an object
     *
     */
    public Vector getChangedAttributeNames() {
        Vector names = new Vector();
        Enumeration attributes = getChanges().elements();
        while (attributes.hasMoreElements()) {
            names.addElement(((ChangeRecord) attributes.nextElement()).getAttribute());
        }
        return names;
    }

    /**
     * INTERNAL:
     * This method returns a reference to the collection of changes within this changeSet
     * @return java.util.Vector
     */
    public java.util.Vector getChanges() {
        if (this.changes == null) {
            this.changes = new Vector(1);
        }
        return changes;
    }

    /**
     * INTERNAL:
     * This method returns the class type that this changeSet represents.
     * The class type must be initialized, before this method is called.
     * @return java.lang.Class or null if the class type isn't initialized.
     */
    public Class getClassType() {
        return classType;
    }

    /**
     * ADVANCE:
     * This method returns the class type that this changeSet Represents.
     * This requires the session to reload the class on serialization.
     * @return java.lang.Class
     */
    public Class getClassType(oracle.toplink.essentials.sessions.Session session) {
        if (classType == null) {
            classType = (Class) ((AbstractSession) session).getDatasourcePlatform().getConversionManager().convertObject(getClassName(), ClassConstants.CLASS);
        }
        return classType;
    }

    /**
     * ADVANCE:
     * This method returns the class type that this changeSet Represents.
     * The class type should be used if the class is desired.
     * @return java.lang.Class
     */
    public String getClassName() {
        return className;
    }

    /**
     * INTERNAL:
     * This method is used to return the initial lock value of the object this changeSet represents
     * @return java.lang.Object
     */
    public java.lang.Object getInitialWriteLockValue() {
        return initialWriteLockValue;
    }

    /**
     * This method returns the key value that this object was stored under in it's
     * Respective hashmap.
     */
    public Object getOldKey() {
        return this.oldKey;
    }

    /**
     * This method returns the key value that this object will be stored under in it's
     * Respective hashmap.
     */
    public Object getNewKey() {
        return this.newKey;
    }

    /**
     * ADVANCED:
     * This method returns the primary keys for the object that this change set represents
     * @return java.util.Vector
     */
    public java.util.Vector getPrimaryKeys() {
        if (getCacheKey() == null) {
            return null;
        }
        return getCacheKey().getKey();
    }

    /**
     * INTERNAL:
     * This method is used to return the complex object specified within the change record.
     * The object is collected from the session which, in this case, is the unit of work.
     * The object's changed attributes will be merged and added to the identity map
     * @param session oracle.toplink.essentials.publicinterface.Session
     */
    public Object getTargetVersionOfSourceObject(AbstractSession session) {
        return getTargetVersionOfSourceObject(session, false);
    }

    /**
     * INTERNAL:
     * This method is used to return the complex object specified within the change record.
     * The object is collected from the session which, in this case, is the unit of work.
     * The object's changed attributes will be merged and added to the identity map
     * @param shouldRead boolean if the object can not be found should it be read in from the database
     * @param session oracle.toplink.essentials.publicinterface.Session
     */
    public Object getTargetVersionOfSourceObject(AbstractSession session, boolean shouldRead) {
        Object attributeValue = null;
        ClassDescriptor descriptor = session.getDescriptor(getClassType(session));
        if (session.isUnitOfWork()) {
            if (((UnitOfWorkImpl) session).getLifecycle() == UnitOfWorkImpl.MergePending) {
                attributeValue = ((UnitOfWorkImpl) session).getOriginalVersionOfObjectOrNull(((UnitOfWorkChangeSet) getUOWChangeSet()).getObjectChangeSetToUOWClone().get(this));
            } else {
                attributeValue = ((UnitOfWorkChangeSet) getUOWChangeSet()).getObjectChangeSetToUOWClone().get(this);
            }
        } else {
            attributeValue = session.getIdentityMapAccessorInstance().getIdentityMapManager().getFromIdentityMap(getPrimaryKeys(), getClassType(session), descriptor);
        }
        if ((attributeValue == null) && (shouldRead)) {
            ReadObjectQuery query = new ReadObjectQuery();
            query.setShouldUseWrapperPolicy(false);
            query.setReferenceClass(getClassType(session));
            query.setSelectionKey(getPrimaryKeys());
            attributeValue = session.executeQuery(query);
        }
        return attributeValue;
    }

    /**
     * INTERNAL:
     * Returns the UnitOfWork Clone that this ChangeSet was built For
     */
    public Object getUnitOfWorkClone() {
        return this.cloneObject;
    }

    /**
     * ADVANCED:
     * This method is used to return the parent ChangeSet
     * @return prototype.changeset.UnitOfWorkChangeSet
     */
    public oracle.toplink.essentials.changesets.UnitOfWorkChangeSet getUOWChangeSet() {
        return unitOfWorkChangeSet;
    }

    /**
     * INTERNAL:
     * This method is used to return the lock value of the object this changeSet represents
     * @return java.lang.Object
     */
    public java.lang.Object getWriteLockValue() {
        return writeLockValue;
    }

    /**
     * ADVANCED:
     * This method will return true if the specified attributue has been changed
     *
     * @param String the name of the attribute to search for
     */
    public boolean hasChangeFor(String attributeName) {
        Enumeration attributes = getChanges().elements();
        while (attributes.hasMoreElements()) {
            if (((ChangeRecord) attributes.nextElement()).getAttribute().equals(attributeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ADVANCED:
     * Returns true if this particular changeSet has changes.
     * @return boolean
     */
    public boolean hasChanges() {
        return this.hasVersionChange || !this.getChanges().isEmpty();
    }

    /**
     * INTERNAL:
     * Returns true if this particular changeSet has forced SQL changes.  This is true whenever
     * CMPPolicy.getForceUpdate() == true or if the object has been marked for opt. read
     * lock (uow.forceUpdateToVersionField).  Kept separate from 'hasChanges' because we don't
     * want to merge or cache sync. a change set that has no 'real' changes.
     * @return boolean
     */
    public boolean hasForcedChanges() {
        return this.shouldModifyVersionField != null || this.hasCmpPolicyForcedUpdate;
    }

    /**
     * INTERNAL:
     * Holds a Boolean indicating whether version field should be modified.
     * This Boolean is set by forcedUpdate into uow.getOptimisticReadLockObjects() 
     * for the clone object and copied here (so don't need to search for it again
     * in uow.getOptimisticReadLockObjects()).
     */
    public void setShouldModifyVersionField(Boolean shouldModifyVersionField) {
        this.shouldModifyVersionField = shouldModifyVersionField;
        if (shouldModifyVersionField != null && shouldModifyVersionField.booleanValue()) {
            this.hasVersionChange = true;
        }
    }

    /**
     * INTERNAL:
     * Holds a Boolean indicating whether version field should be modified.
     */
    public Boolean shouldModifyVersionField() {
        return this.shouldModifyVersionField;
    }

    /**
     * INTERNAL:
     */
    public void setHasCmpPolicyForcedUpdate(boolean hasCmpPolicyForcedUpdate) {
        this.hasCmpPolicyForcedUpdate = hasCmpPolicyForcedUpdate;
    }

    /**
     * INTERNAL:
     */
    public boolean hasCmpPolicyForcedUpdate() {
        return this.hasCmpPolicyForcedUpdate;
    }

    /**
     * INTERNAL:
     * Returns true if this particular changeSet has forced SQL changes because 
     * of a cascade optimistic locking policy.
     * @return boolean
     */
    public boolean hasForcedChangesFromCascadeLocking() {
        return this.hasChangesFromCascadeLocking;
    }

    /**
     * INTERNAL:
     * * Used by calculateChanges to mark this ObjectChangeSet as having to be 
     * flushed to the db steming from a cascade optimistic locking policy.
     */
    public void setHasForcedChangesFromCascadeLocking(boolean newValue) {
        this.setShouldModifyVersionField(Boolean.TRUE);
        this.hasChangesFromCascadeLocking = newValue;
    }

    /**
     * This method overrides the hashcode method.  If this set has a cacheKey then return the hashcode of the
     * cache key, otherwise return the identity hashcode of this object.
     * @return int
     */
    public int hashCode() {
        if (getCacheKey() == null) {
            return System.identityHashCode(this);
        }
        return getCacheKey().hashCode();
    }

    /**
     * INTERNAL:
     * Returns true if this particular changeSet has a Key.
     * @return boolean
     */
    public boolean hasKeys() {
        return (this.newKey != null) || (this.oldKey != null);
    }

    /**
     * INTERNAL:
     * Used to determine if the object change set represents an aggregate object
     * @return boolean
     */
    public boolean isAggregate() {
        return isAggregate;
    }

    /**
     * ADVANCED:
     * Returns true if this ObjectChangeSet represents a new object
     * @return boolean
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * INTERNAL:
     * Indicates whether the change set is invalid.
     * @return boolean
     */
    public boolean isInvalid() {
        return isInvalid;
    }

    /**
     * INTERNAL:
     * This method will be used to merge changes from a supplied ObjectChangeSet
     * into this changeSet.
     */
    public void mergeObjectChanges(ObjectChangeSet changeSetToMergeFrom, UnitOfWorkChangeSet mergeToChangeSet, UnitOfWorkChangeSet mergeFromChangeSet) {
        if (this == changeSetToMergeFrom || this.isInvalid()) {
            return;
        }
        if (changeSetToMergeFrom.optimisticLockingPolicy != null) {
            if (this.optimisticLockingPolicy == null) {
                this.optimisticLockingPolicy = changeSetToMergeFrom.optimisticLockingPolicy;
                this.initialWriteLockValue = changeSetToMergeFrom.initialWriteLockValue;
                this.writeLockValue = changeSetToMergeFrom.writeLockValue;
            } else {
                Object writeLockValueToCompare = this.writeLockValue;
                if (writeLockValueToCompare == null) {
                    writeLockValueToCompare = this.initialWriteLockValue;
                }
                if (this.optimisticLockingPolicy.compareWriteLockValues(writeLockValueToCompare, changeSetToMergeFrom.initialWriteLockValue) != 0) {
                    this.isInvalid = true;
                    return;
                }
                this.writeLockValue = changeSetToMergeFrom.writeLockValue;
            }
        }
        for (int index = 0; index < changeSetToMergeFrom.getChanges().size(); ++index) {
            ChangeRecord record = (ChangeRecord) changeSetToMergeFrom.getChanges().get(index);
            ChangeRecord thisRecord = (ChangeRecord) this.getChangesForAttributeNamed(record.getAttribute());
            if (thisRecord == null) {
                record.updateReferences(mergeToChangeSet, mergeFromChangeSet);
                record.setOwner(this);
                this.addChange(record);
            } else {
                thisRecord.mergeRecord(record, mergeToChangeSet, mergeFromChangeSet);
            }
        }
        this.shouldBeDeleted = changeSetToMergeFrom.shouldBeDeleted;
        this.setOldKey(changeSetToMergeFrom.oldKey);
        this.setNewKey(changeSetToMergeFrom.newKey);
        this.hasVersionChange = changeSetToMergeFrom.hasVersionChange;
        this.shouldModifyVersionField = changeSetToMergeFrom.shouldModifyVersionField;
        this.hasCmpPolicyForcedUpdate = changeSetToMergeFrom.hasCmpPolicyForcedUpdate;
        this.hasChangesFromCascadeLocking = changeSetToMergeFrom.hasChangesFromCascadeLocking;
    }

    /**
     * INTERNAL:
     * Iterate through the change records and ensure the cache synchronization types
     * are set on the change sets associated with those records.
     */
    public void prepareChangeRecordsForSynchronization(AbstractSession session) {
        Enumeration records = getChanges().elements();
        while (records.hasMoreElements()) {
            ((ChangeRecord) records.nextElement()).prepareForSynchronization(session);
        }
    }

    /**
     * INTERNAL:
     * Helper method used by readObject to read a completely serialized change set from
     * the stream
     */
    public void readCompleteChangeSet(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        readIdentityInformation(stream);
        changes = (Vector) stream.readObject();
        setOldKey(stream.readObject());
        newKey = stream.readObject();
    }

    /**
     * INTERNAL:
     * Helper method used by readObject to read just the information about object identity
     * from a serialized stream.
     */
    public void readIdentityInformation(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        cacheKey = (CacheKey) stream.readObject();
        className = (String) stream.readObject();
        writeLockValue = stream.readObject();
    }

    /**
     * @return java.util.Vector
     */
    public void setCacheKey(CacheKey cacheKey) {
        this.cacheKey = cacheKey;
    }

    /**
     * @param newValue java.util.Vector
     */
    public void setChanges(java.util.Vector changesList) {
        this.changes = changesList;
        updateUOWChangeSet();
    }

    /**
     * @param newValue java.lang.Class
     */
    public void setClassType(Class newValue) {
        this.classType = newValue;
    }

    /**
     * INTERNAL:
     * @param newValue java.lang.String
     */
    public void setClassName(String newValue) {
        this.className = newValue;
    }

    /**
     * INTERNAL:
     * Set if this object change Set represents an aggregate
     * @param isAggregate boolean true if the ChangeSet represents an aggregate
     */
    public void setIsAggregate(boolean isAggregate) {
        this.isAggregate = isAggregate;
    }

    /**
     * INTERNAL:
     * Set whether this ObjectChanges represents a new Object
     * @param newIsNew boolean true if this ChangeSet represents a new object
     */
    protected void setIsNew(boolean newIsNew) {
        isNew = newIsNew;
    }

    /**
     * This method is used to set the value that this object was stored under in its respected
     * map collection
     */
    public void setOldKey(Object key) {
        if ((key == null) || (this.oldKey == null)) {
            this.oldKey = key;
        }
    }

    /**
     * This method is used to set the value that this object will be stored under in its respected
     * map collection
     */
    public void setNewKey(Object key) {
        this.newKey = key;
    }

    /**
     * This method was created in VisualAge.
     * @param newValue boolean
     */
    public void setShouldBeDeleted(boolean newValue) {
        this.shouldBeDeleted = newValue;
    }

    /**
     * INTERNAL:
     * Used to set the parent change Set
     * @param newUnitOfWorkChangeSet prototype.changeset.UnitOfWorkChangeSet
     */
    public void setUOWChangeSet(UnitOfWorkChangeSet newUnitOfWorkChangeSet) {
        unitOfWorkChangeSet = newUnitOfWorkChangeSet;
    }

    /**
     * INTERNAL:
     * This method should ONLY be used to set the initial writeLock value for
     * an ObjectChangeSet when it is first built.
     * @param newWriteLockValue java.lang.Object
     */
    public void setOptimisticLockingPolicyAndInitialWriteLockValue(OptimisticLockingPolicy optimisticLockingPolicy, AbstractSession session) {
        this.optimisticLockingPolicy = optimisticLockingPolicy;
        this.initialWriteLockValue = optimisticLockingPolicy.getWriteLockValue(cloneObject, getPrimaryKeys(), session);
    }

    /**
     * ADVANCED:
     * This method is used to set the writeLock value for an ObjectChangeSet
     * Any changes to the write lock value
     * should to through setWriteLockValue(Object obj) so that th change set is
     * marked as being dirty.
     * @param newWriteLockValue java.lang.Object
     */
    public void setWriteLockValue(java.lang.Object newWriteLockValue) {
        this.writeLockValue = newWriteLockValue;
        this.hasVersionChange = true;
        updateUOWChangeSet();
    }

    /**
     * ADVANCED:
     * This method is used to set the initial writeLock value for an ObjectChangeSet.
     * The initial value will only be set once, and can not be overwritten.
     * @param initialWriteLockValue java.lang.Object
     */
    public void setInitialWriteLockValue(java.lang.Object initialWriteLockValue) {
        if (this.initialWriteLockValue == null) {
            this.initialWriteLockValue = initialWriteLockValue;
        }
    }

    /**
     * This method was created in VisualAge.
     * @return boolean
     */
    public boolean shouldBeDeleted() {
        return shouldBeDeleted;
    }

    public String toString() {
        return this.getClass().getName() + "(" + this.getClassName() + ")" + getChanges().toString();
    }

    /**
     * INTERNAL:
     * Used to update a changeRecord that is stored in the CHangeSet with a new value.
     */
    public void updateChangeRecordForAttribute(String attributeName, Object value) {
        ChangeRecord changeRecord = (ChangeRecord) getChangesForAttributeNamed(attributeName);
        if (changeRecord != null) {
            changeRecord.updateChangeRecordWithNewValue(value);
        }
    }

    /**
     * ADVANCED:
     * Used to update a changeRecord that is stored in the CHangeSet with a new value.
     * Used when the new value is a mapped object.
     */
    public void updateChangeRecordForAttributeWithMappedObject(String attributeName, Object value, AbstractSession session) {
        ObjectChangeSet referenceChangeSet = (ObjectChangeSet) this.getUOWChangeSet().getObjectChangeSetForClone(value);
        if (referenceChangeSet == null) {
            ClassDescriptor descriptor = session.getDescriptor(value.getClass());
            if (descriptor != null) {
                referenceChangeSet = descriptor.getObjectBuilder().createObjectChangeSet(value, (UnitOfWorkChangeSet) this.getUOWChangeSet(), false, session);
            }
        }
        updateChangeRecordForAttribute(attributeName, referenceChangeSet);
    }

    /**
     * INTERNAL:
     * Used to update a changeRecord that is stored in the CHangeSet with a new value.
     */
    public void updateChangeRecordForAttribute(DatabaseMapping mapping, Object value) {
        String attributeName = mapping.getAttributeName();
        ChangeRecord changeRecord = (ChangeRecord) getChangesForAttributeNamed(attributeName);
        value = ConversionManager.getDefaultManager().convertObject(value, mapping.getAttributeClassification());
        if (changeRecord != null) {
            changeRecord.updateChangeRecordWithNewValue(value);
        } else if (mapping.isDirectToFieldMapping()) {
            changeRecord = new DirectToFieldChangeRecord(this);
            changeRecord.setAttribute(attributeName);
            changeRecord.setMapping(mapping);
            ((DirectToFieldChangeRecord) changeRecord).setNewValue(value);
            this.addChange(changeRecord);
        }
    }

    /**
     * INTERNAL:
     * This method will be used when merging changesets into other changesets.
     * It will fix references within a changeSet so that it's records point to
     * changesets within this UOWChangeSet.
     */
    public void updateReferences(UnitOfWorkChangeSet localChangeSet, UnitOfWorkChangeSet mergingChangeSet) {
        for (int index = 0; index < this.getChanges().size(); ++index) {
            ChangeRecord record = (ChangeRecord) this.getChanges().get(index);
            record.updateReferences(localChangeSet, mergingChangeSet);
            record.setOwner(this);
        }
    }

    /**
     * INTERNAL:
     * Helper method to writeObject.  Write only the information necessary to identify this
     * ObjectChangeSet to the stream
     */
    public void writeIdentityInformation(java.io.ObjectOutputStream stream) throws java.io.IOException {
        stream.writeObject(cacheKey);
        stream.writeObject(className);
        stream.writeObject(writeLockValue);
    }

    /**
     * INTERNAL:
     * Helper method to readObject.  Completely write this ObjectChangeSet to the stream
     */
    public void writeCompleteChangeSet(java.io.ObjectOutputStream stream) throws java.io.IOException {
        writeIdentityInformation(stream);
        stream.writeObject(changes);
        stream.writeObject(oldKey);
        stream.writeObject(newKey);
    }

    /**
     * INTERNAL:
     */
    public void setPrimaryKeys(Vector key) {
        if (key == null) {
            return;
        }
        if (getCacheKey() == null) {
            setCacheKey(new CacheKey(key));
        } else {
            getCacheKey().setKey(key);
        }
    }

    /**
     * This set contains the list of attributes that must be calculated at commit time.
     */
    public HashSet getDeferredSet() {
        if (deferredSet == null) {
            this.deferredSet = new HashSet();
        }
        return deferredSet;
    }

    /**
     * Check to see if there are any attributes that must be calculated at commit time.
     */
    public boolean hasDeferredAttributes() {
        return !(deferredSet == null || this.deferredSet.isEmpty());
    }

    protected void updateUOWChangeSet() {
        if (this.getUOWChangeSet() != null) {
            ((oracle.toplink.essentials.internal.sessions.UnitOfWorkChangeSet) this.getUOWChangeSet()).setHasChanges(this.hasChanges());
        }
    }

    /**
     * INTERNAL:
     * Remove change.  Used by the event mechanism to reset changes after client has
     * updated the object within an event;
     */
    public void removeChange(String attributeName) {
        Object record = getChangesForAttributeNamed(attributeName);
        if (record != null) {
            getChanges().removeElement(record);
        }
    }

    /**
     * Remove object represent this change set from identity map.  If change set is in XML format, rebuild pk to the correct class type from String
     */
    protected void removeFromIdentityMap(AbstractSession session) {
        session.getIdentityMapAccessor().removeFromIdentityMap(getPrimaryKeys(), getClassType(session));
    }

    /**
     * INTERNAL:
     * Indicates whether the object in session cache should be invalidated.
     * @param original Object is from session's cache into which the changes are about to be merged, non null.
     * @param session AbstractSession into which the changes are about to be merged;
     */
    public boolean shouldInvalidateObject(Object original, AbstractSession session) {
        if (optimisticLockingPolicy == null) {
            return false;
        }
        if (isInvalid()) {
            return true;
        }
        Object originalWriteLockValue = optimisticLockingPolicy.getWriteLockValue(original, getPrimaryKeys(), session);
        if (optimisticLockingPolicy.compareWriteLockValues(initialWriteLockValue, originalWriteLockValue) != 0) {
            return true;
        } else {
            return false;
        }
    }
}
