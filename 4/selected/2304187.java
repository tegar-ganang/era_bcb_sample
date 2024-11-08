package org.objectstyle.cayenne.access;

import java.util.Iterator;
import java.util.Map;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.util.Util;

/**
 * DataRowUtils contains a number of static methods to work with DataRows. This is a
 * helper class for DataContext and ObjectStore.
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
class DataRowUtils {

    /**
     * Merges changes reflected in snapshot map to the object. Changes made to attributes
     * and to-one relationships will be merged. In case an object is already modified,
     * modified properties will not be overwritten.
     */
    static void mergeObjectWithSnapshot(ObjEntity entity, DataObject object, DataRow snapshot) {
        int state = object.getPersistenceState();
        if (entity.isReadOnly() || state == PersistenceState.HOLLOW) {
            refreshObjectWithSnapshot(entity, object, snapshot, true);
        } else if (state != PersistenceState.COMMITTED || object.getDataContext().getChannel() instanceof ObjectContext) {
            forceMergeWithSnapshot(entity, object, snapshot);
        } else {
            refreshObjectWithSnapshot(entity, object, snapshot, false);
        }
    }

    /**
     * Replaces all object attribute values with snapshot values. Sets object state to
     * COMMITTED, unless the snapshot is partial in which case the state is set to HOLLOW
     */
    static void refreshObjectWithSnapshot(ObjEntity objEntity, DataObject object, DataRow snapshot, boolean invalidateToManyRelationships) {
        Map attrMap = objEntity.getAttributeMap();
        Iterator it = attrMap.entrySet().iterator();
        boolean isPartialSnapshot = false;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String attrName = (String) entry.getKey();
            ObjAttribute attr = (ObjAttribute) entry.getValue();
            String dbAttrPath = attr.getDbAttributePath();
            object.writePropertyDirectly(attrName, snapshot.get(dbAttrPath));
            if (!snapshot.containsKey(dbAttrPath)) {
                isPartialSnapshot = true;
            }
        }
        Iterator rit = objEntity.getRelationships().iterator();
        while (rit.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rit.next();
            if (rel.isToMany()) {
                Object toManyList = object.readPropertyDirectly(rel.getName());
                if (toManyList == null) {
                    object.writePropertyDirectly(rel.getName(), Fault.getToManyFault());
                } else if (invalidateToManyRelationships && toManyList instanceof ValueHolder) {
                    ((ValueHolder) toManyList).invalidate();
                }
                continue;
            }
            object.writePropertyDirectly(rel.getName(), Fault.getToOneFault());
        }
        if (isPartialSnapshot) {
            object.setPersistenceState(PersistenceState.HOLLOW);
        } else {
            object.setPersistenceState(PersistenceState.COMMITTED);
        }
    }

    static void forceMergeWithSnapshot(ObjEntity entity, DataObject object, DataRow snapshot) {
        DataContext context = object.getDataContext();
        ObjectDiff diff = (ObjectDiff) context.getObjectStore().getChangesByObjectId().get(object.getObjectId());
        Map attrMap = entity.getAttributeMap();
        Iterator it = attrMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String attrName = (String) entry.getKey();
            ObjAttribute attr = (ObjAttribute) entry.getValue();
            String dbAttrPath = attr.getDbAttributePath();
            Object newVal = snapshot.get(dbAttrPath);
            if (newVal == null && !snapshot.containsKey(dbAttrPath)) {
                continue;
            }
            Object curVal = object.readPropertyDirectly(attrName);
            Object oldVal = diff != null ? diff.getSnapshotValue(attrName) : null;
            if (Util.nullSafeEquals(curVal, oldVal) && !Util.nullSafeEquals(newVal, curVal)) {
                object.writePropertyDirectly(attrName, newVal);
            }
        }
        Iterator rit = entity.getRelationships().iterator();
        while (rit.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rit.next();
            if (rel.isToMany()) {
                continue;
            }
            if (!isToOneTargetModified(rel, object, diff)) {
                DbRelationship dbRelationship = (DbRelationship) rel.getDbRelationships().get(0);
                ObjectId id = snapshot.createTargetObjectId(rel.getTargetEntityName(), dbRelationship);
                if (diff == null || !diff.containsArcSnapshot(rel.getName()) || !Util.nullSafeEquals(id, diff.getArcSnapshotValue(rel.getName()))) {
                    Object target = (id != null) ? context.localObject(id, null) : null;
                    object.writeProperty(rel.getName(), target);
                }
            }
        }
    }

    /**
     * Checks if an object has its to-one relationship target modified in memory.
     */
    static boolean isToOneTargetModified(ObjRelationship relationship, DataObject object, ObjectDiff diff) {
        if (object.getPersistenceState() != PersistenceState.MODIFIED || diff == null) {
            return false;
        }
        Object targetObject = object.readPropertyDirectly(relationship.getName());
        if (targetObject instanceof Fault) {
            return false;
        }
        DataObject toOneTarget = (DataObject) targetObject;
        ObjectId currentId = (toOneTarget != null) ? toOneTarget.getObjectId() : null;
        if (currentId != null && currentId.isTemporary()) {
            return true;
        }
        if (!diff.containsArcSnapshot(relationship.getName())) {
            return false;
        }
        ObjectId targetId = diff.getArcSnapshotValue(relationship.getName());
        return !Util.nullSafeEquals(currentId, targetId);
    }

    DataRowUtils() {
    }
}
