package oracle.toplink.essentials.mappings;

import java.util.*;
import oracle.toplink.essentials.exceptions.*;
import oracle.toplink.essentials.expressions.*;
import oracle.toplink.essentials.internal.descriptors.*;
import oracle.toplink.essentials.internal.helper.*;
import oracle.toplink.essentials.internal.identitymaps.*;
import oracle.toplink.essentials.internal.queryframework.*;
import oracle.toplink.essentials.internal.sessions.*;
import oracle.toplink.essentials.sessions.DatabaseRecord;
import oracle.toplink.essentials.descriptors.DescriptorEvent;
import oracle.toplink.essentials.descriptors.DescriptorEventManager;
import oracle.toplink.essentials.queryframework.*;
import oracle.toplink.essentials.indirection.ValueHolderInterface;
import oracle.toplink.essentials.internal.sessions.AbstractRecord;
import oracle.toplink.essentials.internal.sessions.UnitOfWorkImpl;
import oracle.toplink.essentials.internal.sessions.AbstractSession;
import oracle.toplink.essentials.descriptors.ClassDescriptor;

/**
 * <p><b>Purpose</b>: The aggregate collection mapping is used to represent the aggregate relationship between a single
 * source object and a collection of target objects. The target objects cannot exist without the existence of the
 * source object (privately owned)
 * Unlike the normal aggregate mapping, there is a target table being mapped from the target objects.
 * Unlike normal 1:m mapping, there is no 1:1 back reference mapping, as foreign key constraints have been resolved by the aggregation.
 *
 * @author King (Yaoping) Wang
 * @since TOPLink/Java 3.0
 */
public class AggregateCollectionMapping extends CollectionMapping implements RelationalMapping {

    /** This is a key in the target table which is a foreign key in the target table. */
    protected transient Vector<DatabaseField> targetForeignKeyFields;

    /** This is a primary key in the source table that is used as foreign key in the target table */
    protected transient Vector<DatabaseField> sourceKeyFields;

    /** Foreign keys in the target table to the related keys in the source table */
    protected transient Map<DatabaseField, DatabaseField> targetForeignKeyToSourceKeys;

    /**
     * PUBLIC:
     * Default constructor.
     */
    public AggregateCollectionMapping() {
        this.targetForeignKeyToSourceKeys = new HashMap(5);
        this.sourceKeyFields = oracle.toplink.essentials.internal.helper.NonSynchronizedVector.newInstance(1);
        this.targetForeignKeyFields = oracle.toplink.essentials.internal.helper.NonSynchronizedVector.newInstance(1);
        this.deleteAllQuery = new DeleteAllQuery();
        this.setCascadeAll(true);
    }

    /**
     * INTERNAL:
     */
    public boolean isRelationalMapping() {
        return true;
    }

    /**
     * PUBLIC:
     * Define the target foreign key relationship in the 1-M aggregate collection mapping.
     * Both the target foreign key field name and the source primary key field name must be specified.
     */
    public void addTargetForeignKeyFieldName(String targetForeignKey, String sourceKey) {
        getTargetForeignKeyFields().addElement(new DatabaseField(targetForeignKey));
        getSourceKeyFields().addElement(new DatabaseField(sourceKey));
    }

    /**
     * INTERNAL:
     * Used during building the backup shallow copy to copy the vector without re-registering the target objects.
     */
    public Object buildBackupCloneForPartObject(Object attributeValue, Object clone, Object backup, UnitOfWorkImpl unitOfWork) {
        ContainerPolicy containerPolicy = getContainerPolicy();
        if (attributeValue == null) {
            return containerPolicy.containerInstance(1);
        }
        Object clonedAttributeValue = containerPolicy.containerInstance(containerPolicy.sizeFor(attributeValue));
        synchronized (attributeValue) {
            for (Object valuesIterator = containerPolicy.iteratorFor(attributeValue); containerPolicy.hasNext(valuesIterator); ) {
                Object cloneValue = buildElementBackupClone(containerPolicy.next(valuesIterator, unitOfWork), unitOfWork);
                containerPolicy.addInto(cloneValue, clonedAttributeValue, unitOfWork);
            }
        }
        return clonedAttributeValue;
    }

    /**
     * INTERNAL:
     * Require for cloning, the part must be cloned.
     * Ignore the objects, use the attribute value.
     * this is identical to the super class except that the element must be added to the new
     * aggregates collection so that the referenced objects will be clonned correctly
     */
    public Object buildCloneForPartObject(Object attributeValue, Object original, Object clone, UnitOfWorkImpl unitOfWork, boolean isExisting) {
        ContainerPolicy containerPolicy = getContainerPolicy();
        if (attributeValue == null) {
            return containerPolicy.containerInstance(1);
        }
        Object clonedAttributeValue = containerPolicy.containerInstance(containerPolicy.sizeFor(attributeValue));
        Object temporaryCollection = null;
        synchronized (attributeValue) {
            temporaryCollection = containerPolicy.cloneFor(attributeValue);
        }
        for (Object valuesIterator = containerPolicy.iteratorFor(temporaryCollection); containerPolicy.hasNext(valuesIterator); ) {
            Object originalElement = containerPolicy.next(valuesIterator, unitOfWork);
            if (unitOfWork.isOriginalNewObject(original)) {
                unitOfWork.addNewAggregate(originalElement);
            }
            Object cloneValue = buildElementClone(originalElement, unitOfWork, isExisting);
            containerPolicy.addInto(cloneValue, clonedAttributeValue, unitOfWork);
        }
        return clonedAttributeValue;
    }

    /**
     * INTERNAL:
     * Clone the aggregate collection, if necessary.
     */
    protected Object buildElementBackupClone(Object element, UnitOfWorkImpl unitOfWork) {
        if (unitOfWork.isClassReadOnly(element.getClass())) {
            return element;
        }
        ClassDescriptor aggregateDescriptor = getReferenceDescriptor(element.getClass(), unitOfWork);
        Object clonedElement = aggregateDescriptor.getObjectBuilder().buildBackupClone(element, unitOfWork);
        return clonedElement;
    }

    /**
     * INTERNAL:
     * Clone the aggregate collection, if necessary.
     */
    protected Object buildElementClone(Object element, UnitOfWorkImpl unitOfWork, boolean isExisting) {
        if (unitOfWork.isClassReadOnly(element.getClass())) {
            return element;
        }
        ClassDescriptor aggregateDescriptor = getReferenceDescriptor(element.getClass(), unitOfWork);
        Object clonedElement = aggregateDescriptor.getObjectBuilder().instantiateWorkingCopyClone(element, unitOfWork);
        aggregateDescriptor.getObjectBuilder().populateAttributesForClone(element, clonedElement, unitOfWork);
        unitOfWork.getCloneToOriginals().put(clonedElement, element);
        return clonedElement;
    }

    /**
     * INTERNAL:
     * Cascade registerNew for Create through mappings that require the cascade
     */
    public void cascadeRegisterNewIfRequired(Object object, UnitOfWorkImpl uow, IdentityHashtable visitedObjects) {
        Object cloneAttribute = null;
        cloneAttribute = getAttributeValueFromObject(object);
        if ((cloneAttribute == null) || (!getIndirectionPolicy().objectIsInstantiated(cloneAttribute))) {
            return;
        }
        ObjectBuilder builder = null;
        ContainerPolicy cp = getContainerPolicy();
        Object cloneObjectCollection = null;
        cloneObjectCollection = getRealCollectionAttributeValueFromObject(object, uow);
        Object cloneIter = cp.iteratorFor(cloneObjectCollection);
        while (cp.hasNext(cloneIter)) {
            Object nextObject = cp.next(cloneIter, uow);
            if (nextObject != null && (!visitedObjects.contains(nextObject))) {
                visitedObjects.put(nextObject, nextObject);
                builder = getReferenceDescriptor(nextObject.getClass(), uow).getObjectBuilder();
                builder.cascadeRegisterNewForCreate(nextObject, uow, visitedObjects);
            }
        }
    }

    /**
     * INTERNAL:
     * Cascade registerNew for Create through mappings that require the cascade
     */
    public void cascadePerformRemoveIfRequired(Object object, UnitOfWorkImpl uow, IdentityHashtable visitedObjects) {
        Object cloneAttribute = null;
        cloneAttribute = getAttributeValueFromObject(object);
        if ((cloneAttribute == null)) {
            return;
        }
        ObjectBuilder builder = null;
        ContainerPolicy cp = getContainerPolicy();
        Object cloneObjectCollection = null;
        cloneObjectCollection = getRealCollectionAttributeValueFromObject(object, uow);
        Object cloneIter = cp.iteratorFor(cloneObjectCollection);
        while (cp.hasNext(cloneIter)) {
            Object nextObject = cp.next(cloneIter, uow);
            if (nextObject != null && (!visitedObjects.contains(nextObject))) {
                visitedObjects.put(nextObject, nextObject);
                builder = getReferenceDescriptor(nextObject.getClass(), uow).getObjectBuilder();
                builder.cascadePerformRemove(nextObject, uow, visitedObjects);
            }
        }
    }

    /**
     * INTERNAL:
     * The mapping clones itself to create deep copy.
     */
    public Object clone() {
        AggregateCollectionMapping mappingObject = (AggregateCollectionMapping) super.clone();
        mappingObject.setTargetForeignKeyToSourceKeys(new HashMap(getTargetForeignKeyToSourceKeys()));
        return mappingObject;
    }

    /**
     * INTERNAL:
     * This method is used to create a change record from comparing two aggregate collections
     * @return ChangeRecord
     */
    public ChangeRecord compareForChange(Object clone, Object backUp, ObjectChangeSet owner, AbstractSession session) {
        Object cloneAttribute = null;
        Object backUpAttribute = null;
        cloneAttribute = getAttributeValueFromObject(clone);
        if ((cloneAttribute != null) && (!getIndirectionPolicy().objectIsInstantiated(cloneAttribute))) {
            return null;
        }
        if (!owner.isNew()) {
            backUpAttribute = getAttributeValueFromObject(backUp);
            if ((backUpAttribute == null) && (cloneAttribute == null)) {
                return null;
            }
            ContainerPolicy cp = getContainerPolicy();
            Object backupCollection = null;
            Object cloneCollection = null;
            cloneCollection = getRealCollectionAttributeValueFromObject(clone, session);
            backupCollection = getRealCollectionAttributeValueFromObject(backUp, session);
            if (cp.sizeFor(backupCollection) != cp.sizeFor(cloneCollection)) {
                return convertToChangeRecord(cloneCollection, owner, session);
            }
            Object cloneIterator = cp.iteratorFor(cloneCollection);
            Object backUpIterator = cp.iteratorFor(backupCollection);
            boolean change = false;
            UnitOfWorkChangeSet uowComparisonChangeSet = new UnitOfWorkChangeSet();
            while (cp.hasNext(cloneIterator)) {
                Object cloneObject = cp.next(cloneIterator, session);
                if (cloneObject == null) {
                    change = true;
                    break;
                }
                Object backUpObject = null;
                if (cp.hasNext(backUpIterator)) {
                    backUpObject = cp.next(backUpIterator, session);
                } else {
                    change = true;
                    break;
                }
                if (cloneObject.getClass().equals(backUpObject.getClass())) {
                    ObjectBuilder builder = getReferenceDescriptor(cloneObject.getClass(), session).getObjectBuilder();
                    ObjectChangeSet initialChanges = builder.createObjectChangeSet(cloneObject, uowComparisonChangeSet, owner.isNew(), session);
                    ObjectChangeSet changes = builder.compareForChange(cloneObject, backUpObject, uowComparisonChangeSet, session);
                    if (changes != null) {
                        change = true;
                        break;
                    }
                } else {
                    change = true;
                    break;
                }
            }
            if ((change == true) || (cp.hasNext(backUpIterator))) {
                return convertToChangeRecord(cloneCollection, owner, session);
            } else {
                return null;
            }
        }
        return convertToChangeRecord(getRealCollectionAttributeValueFromObject(clone, session), owner, session);
    }

    /**
     * INTERNAL:
     * Compare the attributes belonging to this mapping for the objects.
     */
    public boolean compareObjects(Object firstObject, Object secondObject, AbstractSession session) {
        Object firstCollection = getRealCollectionAttributeValueFromObject(firstObject, session);
        Object secondCollection = getRealCollectionAttributeValueFromObject(secondObject, session);
        ContainerPolicy containerPolicy = getContainerPolicy();
        if (containerPolicy.sizeFor(firstCollection) != containerPolicy.sizeFor(secondCollection)) {
            return false;
        }
        if (containerPolicy.sizeFor(firstCollection) == 0) {
            return true;
        }
        for (Object iterFirst = containerPolicy.iteratorFor(firstCollection); containerPolicy.hasNext(iterFirst); ) {
            Object firstAggregateObject = containerPolicy.next(iterFirst, session);
            for (Object iterSecond = containerPolicy.iteratorFor(secondCollection); true; ) {
                Object secondAggregateObject = containerPolicy.next(iterSecond, session);
                if (getReferenceDescriptor().getObjectBuilder().compareObjects(firstAggregateObject, secondAggregateObject, session)) {
                    break;
                }
                if (!containerPolicy.hasNext(iterSecond)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * INTERNAL:
     * This method is used to convert the contents of an aggregateCollection into a
     * changeRecord
     * @return oracle.toplink.essentials.internal.sessions.AggregateCollectionChangeRecord the changerecord representing this AggregateCollectionMapping
     * @param owner oracle.toplink.essentials.internal.sessions.ObjectChangeSet the ChangeSet that uses this record
     * @param cloneCollection Object the collection to convert
     * @param session oracle.toplink.essentials.publicinterface.Session
     */
    protected ChangeRecord convertToChangeRecord(Object cloneCollection, ObjectChangeSet owner, AbstractSession session) {
        ContainerPolicy cp = getContainerPolicy();
        Object cloneIter = cp.iteratorFor(cloneCollection);
        Vector collectionChanges = new Vector(2);
        while (cp.hasNext(cloneIter)) {
            Object aggregateObject = cp.next(cloneIter, session);
            if (aggregateObject != null) {
                ObjectChangeSet changes = getReferenceDescriptor(aggregateObject.getClass(), session).getObjectBuilder().compareForChange(aggregateObject, null, (UnitOfWorkChangeSet) owner.getUOWChangeSet(), session);
                collectionChanges.addElement(changes);
            }
        }
        AggregateCollectionChangeRecord changeRecord = new AggregateCollectionChangeRecord(owner);
        changeRecord.setAttribute(getAttributeName());
        changeRecord.setMapping(this);
        changeRecord.setChangedValues(collectionChanges);
        return changeRecord;
    }

    /**
     * To delete all the entries matching the selection criteria from the table stored in the
     * referenced descriptor
     */
    protected void deleteAll(WriteObjectQuery query) throws DatabaseException {
        Object referenceObjects = null;
        if (usesIndirection()) {
            Object attribute = getAttributeAccessor().getAttributeValueFromObject(query.getObject());
            if (attribute == null || !((ValueHolderInterface) attribute).isInstantiated()) {
                referenceObjects = new Vector(0);
            }
        }
        if (referenceObjects == null) {
            referenceObjects = this.getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
        }
        ((DeleteAllQuery) getDeleteAllQuery()).executeDeleteAll(query.getSession().getSessionForClass(getReferenceClass()), query.getTranslationRow(), getContainerPolicy().vectorFor(referenceObjects, query.getSession()));
    }

    /**
     * INTERNAL:
     * Execute a descriptor event for the specified event code.
     */
    protected void executeEvent(int eventCode, ObjectLevelModifyQuery query) {
        ClassDescriptor referenceDescriptor = getReferenceDescriptor(query.getObject().getClass(), query.getSession());
        if (referenceDescriptor.getEventManager().hasAnyEventListeners()) {
            referenceDescriptor.getEventManager().executeEvent(new DescriptorEvent(eventCode, query));
        }
    }

    /**
     * INTERNAL:
     * Extract the source primary key value from the target row.
     * Used for batch reading, most following same order and fields as in the mapping.
     */
    protected Vector extractKeyFromTargetRow(AbstractRecord row, AbstractSession session) {
        Vector key = new Vector(getTargetForeignKeyFields().size());
        for (int index = 0; index < getTargetForeignKeyFields().size(); index++) {
            DatabaseField targetField = (DatabaseField) getTargetForeignKeyFields().elementAt(index);
            DatabaseField sourceField = (DatabaseField) getSourceKeyFields().elementAt(index);
            Object value = row.get(targetField);
            try {
                value = session.getDatasourcePlatform().getConversionManager().convertObject(value, getDescriptor().getObjectBuilder().getFieldClassification(sourceField));
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(this, getDescriptor(), e);
            }
            key.addElement(value);
        }
        return key;
    }

    /**
     * INTERNAL:
     * Extract the primary key value from the source row.
     * Used for batch reading, most following same order and fields as in the mapping.
     */
    protected Vector extractPrimaryKeyFromRow(AbstractRecord row, AbstractSession session) {
        Vector key = new Vector(getSourceKeyFields().size());
        for (Enumeration fieldEnum = getSourceKeyFields().elements(); fieldEnum.hasMoreElements(); ) {
            DatabaseField field = (DatabaseField) fieldEnum.nextElement();
            Object value = row.get(field);
            try {
                value = session.getDatasourcePlatform().getConversionManager().convertObject(value, getDescriptor().getObjectBuilder().getFieldClassification(field));
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(this, getDescriptor(), e);
            }
            key.addElement(value);
        }
        return key;
    }

    /**
     * INTERNAL:
     * return the aggregate databaseRow with the primary keys from the source table and targer table
     */
    public AbstractRecord getAggregateRow(ObjectLevelModifyQuery query, Object object) {
        Vector referenceObjectKeys = getReferenceObjectKeys(query);
        AbstractRecord aggregateRow = new DatabaseRecord();
        Vector keys = getTargetForeignKeyFields();
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            aggregateRow.put(keys.elementAt(keyIndex), referenceObjectKeys.elementAt(keyIndex));
        }
        getReferenceDescriptor(object.getClass(), query.getSession()).getObjectBuilder().buildRow(aggregateRow, object, query.getSession());
        return aggregateRow;
    }

    /**
     * Delete all criteria is created with target foreign keys and source keys.
     * This criteria is then used to delete target records from the table.
     */
    protected Expression getDeleteAllCriteria(AbstractSession session) {
        Expression expression;
        Expression criteria = null;
        Expression builder = new ExpressionBuilder();
        for (Iterator keys = getTargetForeignKeyToSourceKeys().keySet().iterator(); keys.hasNext(); ) {
            DatabaseField targetForeignKey = (DatabaseField) keys.next();
            DatabaseField sourceKey = (DatabaseField) getTargetForeignKeyToSourceKeys().get(targetForeignKey);
            expression = builder.getField(targetForeignKey).equal(builder.getParameter(sourceKey));
            criteria = expression.and(criteria);
        }
        return criteria;
    }

    /**
     * INTERNAL:
     * for inheritance purpose
     */
    public ClassDescriptor getReferenceDescriptor(Class theClass, AbstractSession session) {
        if (getReferenceDescriptor().getJavaClass().equals(theClass)) {
            return getReferenceDescriptor();
        } else {
            ClassDescriptor subclassDescriptor = session.getDescriptor(theClass);
            if (subclassDescriptor == null) {
                throw DescriptorException.noSubClassMatch(theClass, this);
            } else {
                return subclassDescriptor;
            }
        }
    }

    /**
     * INTERNAL:
     * get reference object keys
     */
    public Vector getReferenceObjectKeys(ObjectLevelModifyQuery query) throws DatabaseException, OptimisticLockException {
        Vector referenceObjectKeys = new Vector(getSourceKeyFields().size());
        AbstractRecord translationRow = query.getTranslationRow();
        for (Enumeration sourcekeys = getSourceKeyFields().elements(); sourcekeys.hasMoreElements(); ) {
            DatabaseField sourceKey = (DatabaseField) sourcekeys.nextElement();
            Object referenceKey = null;
            if ((translationRow != null) && (translationRow.containsKey(sourceKey))) {
                referenceKey = translationRow.get(sourceKey);
            } else {
                referenceKey = getDescriptor().getObjectBuilder().extractValueFromObjectForField(query.getObject(), sourceKey, query.getSession());
            }
            referenceObjectKeys.addElement(referenceKey);
        }
        return referenceObjectKeys;
    }

    /**
     * PUBLIC:
     * Return the source key field names associated with the mapping.
     * These are in-order with the targetForeignKeyFieldNames.
     */
    public Vector getSourceKeyFieldNames() {
        Vector fieldNames = new Vector(getSourceKeyFields().size());
        for (Enumeration fieldsEnum = getSourceKeyFields().elements(); fieldsEnum.hasMoreElements(); ) {
            fieldNames.addElement(((DatabaseField) fieldsEnum.nextElement()).getQualifiedName());
        }
        return fieldNames;
    }

    /**
     * INTERNAL:
     * Return the source key names associated with the mapping
     */
    public Vector<DatabaseField> getSourceKeyFields() {
        return sourceKeyFields;
    }

    /**
     * PUBLIC:
     * Return the target foregin key field names associated with the mapping.
     * These are in-order with the sourceKeyFieldNames.
     */
    public Vector getTargetForeignKeyFieldNames() {
        Vector fieldNames = new Vector(getTargetForeignKeyFields().size());
        for (Enumeration fieldsEnum = getTargetForeignKeyFields().elements(); fieldsEnum.hasMoreElements(); ) {
            fieldNames.addElement(((DatabaseField) fieldsEnum.nextElement()).getQualifiedName());
        }
        return fieldNames;
    }

    /**
     * INTERNAL:
     * Return the target foregin key fields associated with the mapping
     */
    public Vector<DatabaseField> getTargetForeignKeyFields() {
        return targetForeignKeyFields;
    }

    /**
     * INTERNAL:
     */
    public Map<DatabaseField, DatabaseField> getTargetForeignKeyToSourceKeys() {
        return targetForeignKeyToSourceKeys;
    }

    /**
     * INTERNAL:
     * For aggregate collection mapping the reference descriptor is cloned. The cloned descriptor is then
     * assigned primary keys and table names before initialize. Once cloned descriptor is initialized
     * it is assigned as reference descriptor in the aggregate mapping. This is a very specifiec
     * behaviour for aggregate mappings. The original descriptor is used only for creating clones and
     * after that mapping never uses it.
     * Some initialization is done in postInitialize to ensure the target descriptor's references are initialized.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        super.initialize(session);
        if (!getReferenceDescriptor().isAggregateCollectionDescriptor()) {
            session.getIntegrityChecker().handleError(DescriptorException.referenceDescriptorIsNotAggregateCollection(getReferenceClass().getName(), this));
        }
        if (shouldInitializeSelectionCriteria()) {
            if (isSourceKeySpecified()) {
                initializeTargetForeignKeyToSourceKeys(session);
            } else {
                initializeTargetForeignKeyToSourceKeysWithDefaults(session);
            }
            initializeSelectionCriteria(session);
        }
        getSelectionQuery().setShouldMaintainCache(false);
        initializeDeleteAllQuery(session);
    }

    /**
     * INTERNAL:
     * For aggregate mapping the reference descriptor is cloned. Also the involved inheritanced descriptor, its childern
     * and parents all need to be cloned. The cloned descriptors are then assigned primary keys and table names before
     * initialize. Once cloned descriptor is initialized it is assigned as reference descriptor in the aggregate mapping.
     * This is a very specifiec behaviour for aggregate mappings. The original descriptor is used only for creating clones
     * and after that mapping never uses it.
     * Some initialization is done in postInitialize to ensure the target descriptor's references are initialized.
     */
    public void initializeChildInheritance(ClassDescriptor parentDescriptor, AbstractSession session) throws DescriptorException {
        if (parentDescriptor.getInheritancePolicy().hasChildren()) {
            Vector childDescriptors = parentDescriptor.getInheritancePolicy().getChildDescriptors();
            Vector cloneChildDescriptors = oracle.toplink.essentials.internal.helper.NonSynchronizedVector.newInstance();
            for (Enumeration enumtr = childDescriptors.elements(); enumtr.hasMoreElements(); ) {
                ClassDescriptor clonedChildDescriptor = (ClassDescriptor) ((ClassDescriptor) enumtr.nextElement()).clone();
                if (!clonedChildDescriptor.isAggregateCollectionDescriptor()) {
                    session.getIntegrityChecker().handleError(DescriptorException.referenceDescriptorIsNotAggregate(clonedChildDescriptor.getJavaClass().getName(), this));
                }
                clonedChildDescriptor.getInheritancePolicy().setParentDescriptor(parentDescriptor);
                clonedChildDescriptor.preInitialize(session);
                clonedChildDescriptor.initialize(session);
                cloneChildDescriptors.addElement(clonedChildDescriptor);
                initializeChildInheritance(clonedChildDescriptor, session);
            }
            parentDescriptor.getInheritancePolicy().setChildDescriptors(cloneChildDescriptors);
        }
    }

    /**
     * INTERNAL:
     * Initialize delete all query. This query is used to delete the collection of objects from the
     * target table.
     */
    protected void initializeDeleteAllQuery(AbstractSession session) {
        DeleteAllQuery query = (DeleteAllQuery) getDeleteAllQuery();
        query.setReferenceClass(getReferenceClass());
        query.setShouldMaintainCache(false);
        if (!hasCustomDeleteAllQuery()) {
            if (getSelectionCriteria() == null) {
                query.setSelectionCriteria(getDeleteAllCriteria(session));
            } else {
                query.setSelectionCriteria(getSelectionCriteria());
            }
        }
    }

    /**
     * INTERNAL:
     * For aggregate mapping the reference descriptor is cloned. Also the involved inheritanced descriptor, its childern
     * and parents all need to be cloned. The cloned descriptors are then assigned primary keys and table names before
     * initialize. Once cloned descriptor is initialized it is assigned as reference descriptor in the aggregate mapping.
     * This is a very specifiec behaviour for aggregate mappings. The original descriptor is used only for creating clones
     * and after that mapping never uses it.
     * Some initialization is done in postInitialize to ensure the target descriptor's references are initialized.
     */
    public void initializeParentInheritance(ClassDescriptor parentDescriptor, ClassDescriptor childDescriptor, AbstractSession session) throws DescriptorException {
        if (!parentDescriptor.isAggregateCollectionDescriptor()) {
            session.getIntegrityChecker().handleError(DescriptorException.referenceDescriptorIsNotAggregateCollection(parentDescriptor.getJavaClass().getName(), this));
        }
        ClassDescriptor clonedParentDescriptor = (ClassDescriptor) parentDescriptor.clone();
        if (clonedParentDescriptor.getInheritancePolicy().isChildDescriptor()) {
            ClassDescriptor parentToParentDescriptor = session.getDescriptor(clonedParentDescriptor.getJavaClass());
            initializeParentInheritance(parentToParentDescriptor, parentDescriptor, session);
        }
        Vector childern = oracle.toplink.essentials.internal.helper.NonSynchronizedVector.newInstance(1);
        childern.addElement(childDescriptor);
        clonedParentDescriptor.getInheritancePolicy().setChildDescriptors(childern);
        clonedParentDescriptor.preInitialize(session);
        clonedParentDescriptor.initialize(session);
    }

    /**
     * INTERNAL:
     * Selection criteria is created with target foreign keys and source keys.
     * This criteria is then used to read records from the target table.
     */
    protected void initializeSelectionCriteria(AbstractSession session) {
        Expression expression;
        Expression criteria;
        Expression builder = new ExpressionBuilder();
        for (Iterator keys = getTargetForeignKeyToSourceKeys().keySet().iterator(); keys.hasNext(); ) {
            DatabaseField targetForeignKey = (DatabaseField) keys.next();
            DatabaseField sourceKey = (DatabaseField) getTargetForeignKeyToSourceKeys().get(targetForeignKey);
            expression = builder.getField(targetForeignKey).equal(builder.getParameter(sourceKey));
            criteria = expression.and(getSelectionCriteria());
            setSelectionCriteria(criteria);
        }
    }

    /**
     * INTERNAL:
     * The foreign keys and the primary key names are converted to DatabaseFields and stored.
     */
    protected void initializeTargetForeignKeyToSourceKeys(AbstractSession session) throws DescriptorException {
        if (getTargetForeignKeyFields().isEmpty()) {
            throw DescriptorException.noTargetForeignKeysSpecified(this);
        }
        for (Enumeration keys = getTargetForeignKeyFields().elements(); keys.hasMoreElements(); ) {
            DatabaseField foreignKeyfield = (DatabaseField) keys.nextElement();
            getReferenceDescriptor().buildField(foreignKeyfield);
        }
        for (Enumeration keys = getSourceKeyFields().elements(); keys.hasMoreElements(); ) {
            DatabaseField sourceKeyfield = (DatabaseField) keys.nextElement();
            getDescriptor().buildField(sourceKeyfield);
        }
        if (getTargetForeignKeyFields().size() != getSourceKeyFields().size()) {
            throw DescriptorException.targetForeignKeysSizeMismatch(this);
        }
        Enumeration<DatabaseField> targetForeignKeysEnum = getTargetForeignKeyFields().elements();
        Enumeration<DatabaseField> sourceKeysEnum = getSourceKeyFields().elements();
        for (; targetForeignKeysEnum.hasMoreElements(); ) {
            getTargetForeignKeyToSourceKeys().put(targetForeignKeysEnum.nextElement(), sourceKeysEnum.nextElement());
        }
    }

    /**
     * INTERNAL:
     * The foreign keys and the primary key names are converted to DatabaseFields and stored. The source keys
     * are not specified by the user so primary keys are extracted from the reference descriptor.
     */
    protected void initializeTargetForeignKeyToSourceKeysWithDefaults(AbstractSession session) throws DescriptorException {
        if (getTargetForeignKeyFields().isEmpty()) {
            throw DescriptorException.noTargetForeignKeysSpecified(this);
        }
        List<DatabaseField> sourceKeys = getDescriptor().getPrimaryKeyFields();
        setSourceKeyFields(oracle.toplink.essentials.internal.helper.NonSynchronizedVector.newInstance(sourceKeys));
        for (Enumeration keys = getTargetForeignKeyFields().elements(); keys.hasMoreElements(); ) {
            DatabaseField foreignKeyfield = ((DatabaseField) keys.nextElement());
            getReferenceDescriptor().buildField(foreignKeyfield);
        }
        if (getTargetForeignKeyFields().size() != sourceKeys.size()) {
            throw DescriptorException.targetForeignKeysSizeMismatch(this);
        }
        for (int index = 0; index < getTargetForeignKeyFields().size(); index++) {
            getTargetForeignKeyToSourceKeys().put(getTargetForeignKeyFields().get(index), sourceKeys.get(index));
        }
    }

    /**
     * INTERNAL:
     * Iterate on the specified element.
     */
    public void iterateOnElement(DescriptorIterator iterator, Object element) {
        if (element != null) {
            iterator.iterateForAggregateMapping(element, this, iterator.getSession().getDescriptor(element));
        }
    }

    /**
     * INTERNAL:
     */
    public boolean isAggregateCollectionMapping() {
        return true;
    }

    /**
     * INTERNAL:
     */
    public boolean isPrivateOwned() {
        return true;
    }

    /**
     * Checks if source key is specified or not.
     */
    protected boolean isSourceKeySpecified() {
        return !(getSourceKeyFields().isEmpty());
    }

    /**
     * INTERNAL:
     * Merge changes from the source to the target object.
     * Because this is a collection mapping, values are added to or removed from the
     * collection based on the changeset
     */
    public void mergeChangesIntoObject(Object target, ChangeRecord changeRecord, Object source, MergeManager mergeManager) {
        if (!isAttributeValueInstantiated(target)) {
            return;
        }
        ContainerPolicy containerPolicy = getContainerPolicy();
        AbstractSession session = mergeManager.getSession();
        Object valueOfTarget = null;
        Object sourceAggregate = null;
        if (mergeManager.shouldMergeChangesIntoDistributedCache()) {
            ClassDescriptor descriptor = getDescriptor();
            AbstractRecord parentRow = descriptor.getObjectBuilder().extractPrimaryKeyRowFromObject(target, session);
            Object result = getIndirectionPolicy().valueFromQuery(getSelectionQuery(), parentRow, session);
            setAttributeValueInObject(target, result);
            return;
        }
        Vector aggregateObjects = ((AggregateCollectionChangeRecord) changeRecord).getChangedValues();
        valueOfTarget = containerPolicy.containerInstance();
        ObjectChangeSet objectChanges = null;
        for (int i = 0; i < aggregateObjects.size(); ++i) {
            objectChanges = (ObjectChangeSet) aggregateObjects.elementAt(i);
            Class localClassType = objectChanges.getClassType(session);
            sourceAggregate = objectChanges.getUnitOfWorkClone();
            Object targetAggregate = ((UnitOfWorkImpl) mergeManager.getSession()).getCloneToOriginals().get(sourceAggregate);
            if (targetAggregate == null) {
                targetAggregate = getReferenceDescriptor(localClassType, session).getObjectBuilder().buildNewInstance();
            }
            getReferenceDescriptor(localClassType, session).getObjectBuilder().mergeChangesIntoObject(targetAggregate, objectChanges, sourceAggregate, mergeManager);
            containerPolicy.addInto(targetAggregate, valueOfTarget, session);
        }
        setRealAttributeValueInObject(target, valueOfTarget);
    }

    /**
     * INTERNAL:
     * Merge changes from the source to the target object.
     */
    public void mergeIntoObject(Object target, boolean isTargetUnInitialized, Object source, MergeManager mergeManager) {
        if (isTargetUnInitialized) {
            if (mergeManager.shouldMergeWorkingCopyIntoOriginal() && (!isAttributeValueInstantiated(source))) {
                setAttributeValueInObject(target, getIndirectionPolicy().getOriginalIndirectionObject(getAttributeValueFromObject(source), mergeManager.getSession()));
                return;
            }
        }
        if (!shouldMergeCascadeReference(mergeManager)) {
            return;
        }
        if (mergeManager.shouldMergeOriginalIntoWorkingCopy()) {
            if (!isAttributeValueInstantiated(target)) {
                return;
            }
        } else if (!isAttributeValueInstantiated(source)) {
            return;
        }
        ContainerPolicy containerPolicy = getContainerPolicy();
        Object valueOfSource = getRealCollectionAttributeValueFromObject(source, mergeManager.getSession());
        Object valueOfTarget = containerPolicy.containerInstance(containerPolicy.sizeFor(valueOfSource));
        for (Object sourceValuesIterator = containerPolicy.iteratorFor(valueOfSource); containerPolicy.hasNext(sourceValuesIterator); ) {
            Object sourceValue = containerPolicy.next(sourceValuesIterator, mergeManager.getSession());
            Object originalValue = getReferenceDescriptor(sourceValue.getClass(), mergeManager.getSession()).getObjectBuilder().buildNewInstance();
            getReferenceDescriptor(sourceValue.getClass(), mergeManager.getSession()).getObjectBuilder().mergeIntoObject(originalValue, true, sourceValue, mergeManager);
            containerPolicy.addInto(originalValue, valueOfTarget, mergeManager.getSession());
        }
        setRealAttributeValueInObject(target, valueOfTarget);
    }

    /**
     * INTERNAL:
     * An object was added to the collection during an update, insert it if private.
     */
    protected void objectAddedDuringUpdate(ObjectLevelModifyQuery query, Object objectAdded, ObjectChangeSet changeSet) throws DatabaseException, OptimisticLockException {
        InsertObjectQuery insertQuery = getAndPrepareModifyQueryForInsert(query, objectAdded);
        query.getSession().executeQuery(insertQuery, insertQuery.getTranslationRow());
    }

    /**
     * INTERNAL:
     * An object was removed to the collection during an update, delete it if private.
     */
    protected void objectRemovedDuringUpdate(ObjectLevelModifyQuery query, Object objectDeleted) throws DatabaseException, OptimisticLockException {
        DeleteObjectQuery deleteQuery = new DeleteObjectQuery();
        prepareModifyQueryForDelete(query, deleteQuery, objectDeleted);
        query.getSession().executeQuery(deleteQuery, deleteQuery.getTranslationRow());
    }

    /**
     * INTERNAL:
     * An object is still in the collection, update it as it may have changed.
     */
    protected void objectUnchangedDuringUpdate(ObjectLevelModifyQuery query, Object object, Hashtable backupCloneKeyedCache, CacheKey cachedKey) throws DatabaseException, OptimisticLockException {
        UpdateObjectQuery updateQuery = new UpdateObjectQuery();
        Object backupclone = backupCloneKeyedCache.get(cachedKey);
        updateQuery.setBackupClone(backupclone);
        prepareModifyQueryForUpdate(query, updateQuery, object);
        query.getSession().executeQuery(updateQuery, updateQuery.getTranslationRow());
    }

    /**
     * INTERNAL:
     * For aggregate collection mapping the reference descriptor is cloned. The cloned descriptor is then
     * assigned primary keys and table names before initialize. Once cloned descriptor is initialized
     * it is assigned as reference descriptor in the aggregate mapping. This is a very specifiec
     * behaviour for aggregate mappings. The original descriptor is used only for creating clones and
     * after that mapping never uses it.
     * Some initialization is done in postInitialize to ensure the target descriptor's references are initialized.
     */
    public void postInitialize(AbstractSession session) throws DescriptorException {
        super.postInitialize(session);
        getReferenceDescriptor().postInitialize(session);
    }

    /**
     * INTERNAL:
     * Insert privately owned parts
     */
    public void postInsert(WriteObjectQuery query) throws DatabaseException, OptimisticLockException {
        if (isReadOnly()) {
            return;
        }
        Object objects = getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
        ContainerPolicy cp = getContainerPolicy();
        for (Object iter = cp.iteratorFor(objects); cp.hasNext(iter); ) {
            Object object = cp.next(iter, query.getSession());
            InsertObjectQuery insertQuery = getAndPrepareModifyQueryForInsert(query, object);
            query.getSession().executeQuery(insertQuery, insertQuery.getTranslationRow());
        }
    }

    /**
     * INTERNAL:
     * Update the privately owned parts
     */
    public void postUpdate(WriteObjectQuery writeQuery) throws DatabaseException, OptimisticLockException {
        if (isReadOnly()) {
            return;
        }
        if (!isAttributeValueInstantiated(writeQuery.getObject())) {
            return;
        }
        Object objects = getRealCollectionAttributeValueFromObject(writeQuery.getObject(), writeQuery.getSession());
        Object currentObjectsInDB = readPrivateOwnedForObject(writeQuery);
        if (currentObjectsInDB == null) {
            currentObjectsInDB = getContainerPolicy().containerInstance(1);
        }
        compareObjectsAndWrite(currentObjectsInDB, objects, writeQuery);
    }

    /**
     * INTERNAL:
     * Delete privately owned parts
     */
    public void preDelete(WriteObjectQuery query) throws DatabaseException, OptimisticLockException {
        if (isReadOnly()) {
            return;
        }
        if (getReferenceDescriptor().hasDependencyOnParts() || getReferenceDescriptor().usesOptimisticLocking() || (getReferenceDescriptor().hasInheritance() && getReferenceDescriptor().getInheritancePolicy().shouldReadSubclasses()) || getReferenceDescriptor().hasMultipleTables()) {
            Object objects = getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
            ContainerPolicy containerPolicy = getContainerPolicy();
            for (Object iter = containerPolicy.iteratorFor(objects); containerPolicy.hasNext(iter); ) {
                Object object = containerPolicy.next(iter, query.getSession());
                DeleteObjectQuery deleteQuery = new DeleteObjectQuery();
                prepareModifyQueryForDelete(query, deleteQuery, object);
                query.getSession().executeQuery(deleteQuery, deleteQuery.getTranslationRow());
            }
            if (!query.getSession().isUnitOfWork()) {
                verifyDeleteForUpdate(query);
            }
        } else {
            deleteAll(query);
        }
    }

    /**
     * INTERNAL:
     * The message is passed to its reference class descriptor.
     */
    public void preInsert(WriteObjectQuery query) throws DatabaseException, OptimisticLockException {
        if (isReadOnly()) {
            return;
        }
        Object objects = getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
        ContainerPolicy cp = getContainerPolicy();
        for (Object iter = cp.iteratorFor(objects); cp.hasNext(iter); ) {
            Object object = cp.next(iter, query.getSession());
            InsertObjectQuery insertQuery = getAndPrepareModifyQueryForInsert(query, object);
            executeEvent(DescriptorEventManager.PreWriteEvent, insertQuery);
            executeEvent(DescriptorEventManager.PreInsertEvent, insertQuery);
            getReferenceDescriptor().getQueryManager().preInsert(insertQuery);
        }
    }

    /**
     * INTERNAL:
     * Returns clone of InsertObjectQuery from the reference descriptor, if it is not set - create it.
     */
    protected InsertObjectQuery getInsertObjectQuery(AbstractSession session, ClassDescriptor desc) {
        InsertObjectQuery insertQuery = desc.getQueryManager().getInsertQuery();
        if (insertQuery == null) {
            insertQuery = new InsertObjectQuery();
            desc.getQueryManager().setInsertQuery(insertQuery);
        }
        if (insertQuery.getModifyRow() == null) {
            AbstractRecord modifyRow = new DatabaseRecord();
            for (int i = 0; i < getTargetForeignKeyFields().size(); i++) {
                DatabaseField field = (DatabaseField) getTargetForeignKeyFields().elementAt(i);
                modifyRow.put(field, null);
            }
            desc.getObjectBuilder().buildTemplateInsertRow(session, modifyRow);
            insertQuery.setModifyRow(modifyRow);
        }
        return insertQuery;
    }

    /**
     * INTERNAL:
     * setup the modifyQuery for post insert/update and pre delete
     */
    public InsertObjectQuery getAndPrepareModifyQueryForInsert(ObjectLevelModifyQuery originalQuery, Object object) {
        AbstractSession session = originalQuery.getSession();
        ClassDescriptor objReferenceDescriptor = getReferenceDescriptor(object.getClass(), session);
        InsertObjectQuery insertQueryFromDescriptor = getInsertObjectQuery(session, objReferenceDescriptor);
        insertQueryFromDescriptor.checkPrepare(session, insertQueryFromDescriptor.getModifyRow());
        InsertObjectQuery insertQuery = (InsertObjectQuery) insertQueryFromDescriptor.clone();
        insertQuery.setObject(object);
        AbstractRecord targetForeignKeyRow = new DatabaseRecord();
        Vector referenceObjectKeys = getReferenceObjectKeys(originalQuery);
        for (int keyIndex = 0; keyIndex < getTargetForeignKeyFields().size(); keyIndex++) {
            targetForeignKeyRow.put(getTargetForeignKeyFields().elementAt(keyIndex), referenceObjectKeys.elementAt(keyIndex));
        }
        insertQuery.setModifyRow(targetForeignKeyRow);
        insertQuery.setTranslationRow(targetForeignKeyRow);
        insertQuery.setSession(session);
        insertQuery.setCascadePolicy(originalQuery.getCascadePolicy());
        insertQuery.dontMaintainCache();
        if (session.isUnitOfWork()) {
            Object backupAttributeValue = getReferenceDescriptor(object.getClass(), session).getObjectBuilder().buildNewInstance();
            insertQuery.setBackupClone(backupAttributeValue);
        }
        return insertQuery;
    }

    /**
     * INTERNAL:
     * setup the modifyQuery for pre delete
     */
    public void prepareModifyQueryForDelete(ObjectLevelModifyQuery originalQuery, ObjectLevelModifyQuery modifyQuery, Object object) {
        AbstractRecord aggregateRow = getAggregateRow(originalQuery, object);
        modifyQuery.setObject(object);
        modifyQuery.setPrimaryKey(getReferenceDescriptor().getObjectBuilder().extractPrimaryKeyFromRow(aggregateRow, originalQuery.getSession()));
        modifyQuery.setModifyRow(aggregateRow);
        modifyQuery.setTranslationRow(aggregateRow);
        modifyQuery.setSession(originalQuery.getSession());
        if (originalQuery.shouldCascadeOnlyDependentParts()) {
            modifyQuery.setCascadePolicy(DatabaseQuery.CascadeAggregateDelete);
        } else {
            modifyQuery.setCascadePolicy(originalQuery.getCascadePolicy());
        }
        modifyQuery.dontMaintainCache();
    }

    /**
     * INTERNAL:
     * setup the modifyQuery for update,
     */
    public void prepareModifyQueryForUpdate(ObjectLevelModifyQuery originalQuery, ObjectLevelModifyQuery modifyQuery, Object object) {
        AbstractRecord aggregateRow = getAggregateRow(originalQuery, object);
        modifyQuery.setObject(object);
        modifyQuery.setPrimaryKey(getReferenceDescriptor().getObjectBuilder().extractPrimaryKeyFromRow(aggregateRow, originalQuery.getSession()));
        modifyQuery.setTranslationRow(aggregateRow);
        modifyQuery.setSession(originalQuery.getSession());
        modifyQuery.setCascadePolicy(originalQuery.getCascadePolicy());
        modifyQuery.dontMaintainCache();
    }

    /**
     * PUBLIC:
     * Set the source key field names associated with the mapping.
     * These must be in-order with the targetForeignKeyFieldNames.
     */
    public void setSourceKeyFieldNames(Vector fieldNames) {
        Vector fields = oracle.toplink.essentials.internal.helper.NonSynchronizedVector.newInstance(fieldNames.size());
        for (Enumeration fieldNamesEnum = fieldNames.elements(); fieldNamesEnum.hasMoreElements(); ) {
            fields.addElement(new DatabaseField((String) fieldNamesEnum.nextElement()));
        }
        setSourceKeyFields(fields);
    }

    /**
     * INTERNAL:
     * set all the primary key names associated with this mapping
     */
    public void setSourceKeyFields(Vector<DatabaseField> sourceKeyFields) {
        this.sourceKeyFields = sourceKeyFields;
    }

    /**
     * PUBLIC:
     * Set the target foregin key field names associated with the mapping.
     * These must be in-order with the sourceKeyFieldNames.
     */
    public void setTargetForeignKeyFieldNames(Vector fieldNames) {
        Vector fields = oracle.toplink.essentials.internal.helper.NonSynchronizedVector.newInstance(fieldNames.size());
        for (Enumeration fieldNamesEnum = fieldNames.elements(); fieldNamesEnum.hasMoreElements(); ) {
            fields.addElement(new DatabaseField((String) fieldNamesEnum.nextElement()));
        }
        setTargetForeignKeyFields(fields);
    }

    /**
     * INTERNAL:
     * set the target foregin key fields associated with the mapping
     */
    public void setTargetForeignKeyFields(Vector<DatabaseField> targetForeignKeyFields) {
        this.targetForeignKeyFields = targetForeignKeyFields;
    }

    protected void setTargetForeignKeyToSourceKeys(Map<DatabaseField, DatabaseField> targetForeignKeyToSourceKeys) {
        this.targetForeignKeyToSourceKeys = targetForeignKeyToSourceKeys;
    }

    /**
     * Returns true as any process leading to object modification should also affect its privately owned parts
     * Usually used by write, insert, update and delete.
     */
    protected boolean shouldObjectModifyCascadeToParts(ObjectLevelModifyQuery query) {
        if (isReadOnly()) {
            return false;
        }
        return true;
    }

    /**
     * ADVANCED:
     * This method is used to have an object add to a collection once the changeSet is applied
     * The referenceKey parameter should only be used for direct Maps. PLEASE ENSURE that the changes
     * have been made in the object model first.
     */
    public void simpleAddToCollectionChangeRecord(Object referenceKey, Object changeSetToAdd, ObjectChangeSet changeSet, AbstractSession session) {
        AggregateCollectionChangeRecord collectionChangeRecord = (AggregateCollectionChangeRecord) changeSet.getChangesForAttributeNamed(this.getAttributeName());
        if (collectionChangeRecord == null) {
            Object cloneObject = ((UnitOfWorkChangeSet) changeSet.getUOWChangeSet()).getUOWCloneForObjectChangeSet(changeSet);
            Object cloneCollection = this.getRealAttributeValueFromObject(cloneObject, session);
            collectionChangeRecord = (AggregateCollectionChangeRecord) convertToChangeRecord(cloneCollection, changeSet, session);
            changeSet.addChange(collectionChangeRecord);
        } else {
            collectionChangeRecord.getChangedValues().add(changeSetToAdd);
        }
    }

    /**
     * ADVANCED:
     * This method is used to have an object removed from a collection once the changeSet is applied
     * The referenceKey parameter should only be used for direct Maps.  PLEASE ENSURE that the changes
     * have been made in the object model first.
     */
    public void simpleRemoveFromCollectionChangeRecord(Object referenceKey, Object changeSetToRemove, ObjectChangeSet changeSet, AbstractSession session) {
        AggregateCollectionChangeRecord collectionChangeRecord = (AggregateCollectionChangeRecord) changeSet.getChangesForAttributeNamed(this.getAttributeName());
        if (collectionChangeRecord == null) {
            Object cloneObject = ((UnitOfWorkChangeSet) changeSet.getUOWChangeSet()).getUOWCloneForObjectChangeSet(changeSet);
            Object cloneCollection = this.getRealAttributeValueFromObject(cloneObject, session);
            collectionChangeRecord = (AggregateCollectionChangeRecord) convertToChangeRecord(cloneCollection, changeSet, session);
            changeSet.addChange(collectionChangeRecord);
        } else {
            collectionChangeRecord.getChangedValues().remove(changeSetToRemove);
        }
    }

    /**
     * INTERNAL:
     * Retrieves a value from the row for a particular query key
     */
    protected Object valueFromRowInternal(AbstractRecord row, JoinedAttributeManager joinManager, AbstractSession executionSession) throws DatabaseException {
        row = (AbstractRecord) row.clone();
        int i = 0;
        for (Enumeration sourceKeys = getSourceKeyFields().elements(); sourceKeys.hasMoreElements(); i++) {
            DatabaseField sourceKey = (DatabaseField) sourceKeys.nextElement();
            Object value = null;
            int index = row.getFields().indexOf(sourceKey);
            if (index == -1) {
                value = joinManager.getBaseQuery().getTranslationRow().get(sourceKey);
                row.add(sourceKey, value);
            } else {
                value = row.getValues().elementAt(index);
            }
            row.add((DatabaseField) getTargetForeignKeyFields().elementAt(i), value);
        }
        return super.valueFromRowInternal(row, joinManager, executionSession);
    }

    /**
     * INTERNAL:
     * Checks if object is deleted from the database or not.
     */
    public boolean verifyDelete(Object object, AbstractSession session) throws DatabaseException {
        if (isReadOnly()) {
            return true;
        }
        AbstractRecord row = getDescriptor().getObjectBuilder().buildRowForTranslation(object, session);
        Object value = session.executeQuery(getSelectionQuery(), row);
        return getContainerPolicy().isEmpty(value);
    }

    /**
     *    Verifying deletes make sure that all the records privately owned by this mapping are
     * actually removed. If such records are found than those are all read and removed one
     * by one taking their privately owned parts into account.
     */
    protected void verifyDeleteForUpdate(WriteObjectQuery query) throws DatabaseException, OptimisticLockException {
        Object objects = readPrivateOwnedForObject(query);
        ContainerPolicy cp = getContainerPolicy();
        for (Object iter = cp.iteratorFor(objects); cp.hasNext(iter); ) {
            query.getSession().deleteObject(cp.next(iter, query.getSession()));
        }
    }

    /**
     * INTERNAL:
     * Add a new value and its change set to the collection change record.  This is used by
     * attribute change tracking.  Currently it is not supported in AggregateCollectionMapping.
     */
    public void addToCollectionChangeRecord(Object newKey, Object newValue, ObjectChangeSet objectChangeSet, UnitOfWorkImpl uow) throws DescriptorException {
        throw DescriptorException.invalidMappingOperation(this, "addToCollectionChangeRecord");
    }

    /**
     * INTERNAL
     * Return true if this mapping supports cascaded version optimistic locking.
     */
    public boolean isCascadedLockingSupported() {
        return true;
    }

    /**
     * INTERNAL:
     * Return if this mapping supports change tracking.
     */
    public boolean isChangeTrackingSupported() {
        return false;
    }

    /**
     * INTERNAL:
     * Remove a value and its change set from the collection change record.  This is used by
     * attribute change tracking.  Currently it is not supported in AggregateCollectionMapping.
     */
    public void removeFromCollectionChangeRecord(Object newKey, Object newValue, ObjectChangeSet objectChangeSet, UnitOfWorkImpl uow) throws DescriptorException {
        throw DescriptorException.invalidMappingOperation(this, "removeFromCollectionChangeRecord");
    }
}
