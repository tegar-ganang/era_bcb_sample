package org.objectstyle.cayenne.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.CollectionProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.property.PropertyVisitor;
import org.objectstyle.cayenne.property.SingleObjectArcProperty;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

/**
 * An operation that creates a subgraph of detached objects, using the PrefetchTree to
 * delineate the graph boundaries. Target objects can be described by a different set of
 * descriptors, thus allowing server-to-client conversion to happen in the process.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ObjectDetachOperation {

    protected EntityResolver targetResolver;

    protected Map seen;

    public ObjectDetachOperation(EntityResolver targetResolver) {
        this.targetResolver = targetResolver;
        this.seen = new HashMap();
    }

    public void reset() {
        seen.clear();
    }

    /**
     * "Detaches" an object from its context by creating an unattached copy. The copy is
     * created using target descriptor of this operation that may be different from the
     * object descriptor passed to this method.
     */
    public Object detach(Object object, ClassDescriptor descriptor, final PrefetchTreeNode prefetchTree) {
        if (!(object instanceof Persistent)) {
            throw new CayenneRuntimeException("Expected Persistent, got: " + object);
        }
        final Persistent source = (Persistent) object;
        ObjectId id = source.getObjectId();
        if (id == null) {
            throw new CayenneRuntimeException("Server returned an object without an id: " + source);
        }
        Object seenTarget = seen.get(id);
        if (seenTarget != null) {
            return seenTarget;
        }
        descriptor = descriptor.getSubclassDescriptor(source.getClass());
        final ClassDescriptor targetDescriptor = targetResolver.getClassDescriptor(id.getEntityName());
        final Persistent target = (Persistent) targetDescriptor.createObject();
        target.setObjectId(id);
        seen.put(id, target);
        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitSingleObjectArc(SingleObjectArcProperty property) {
                if (prefetchTree != null) {
                    PrefetchTreeNode child = prefetchTree.getNode(property.getName());
                    if (child != null) {
                        Object destinationSource = property.readProperty(source);
                        Object destinationTarget = destinationSource != null ? detach(destinationSource, property.getTargetDescriptor(), child) : null;
                        SingleObjectArcProperty targetProperty = (SingleObjectArcProperty) targetDescriptor.getProperty(property.getName());
                        Object oldTarget = targetProperty.isFault(target) ? null : targetProperty.readProperty(target);
                        targetProperty.writeProperty(target, oldTarget, destinationTarget);
                    }
                }
                return true;
            }

            public boolean visitCollectionArc(CollectionProperty property) {
                if (prefetchTree != null) {
                    PrefetchTreeNode child = prefetchTree.getNode(property.getName());
                    if (child != null) {
                        Collection collection = (Collection) property.readProperty(source);
                        Collection targetCollection = new ArrayList(collection.size());
                        Iterator it = collection.iterator();
                        while (it.hasNext()) {
                            Object destinationSource = it.next();
                            Object destinationTarget = destinationSource != null ? detach(destinationSource, property.getTargetDescriptor(), child) : null;
                            targetCollection.add(destinationTarget);
                        }
                        CollectionProperty targetProperty = (CollectionProperty) targetDescriptor.getProperty(property.getName());
                        targetProperty.writeProperty(target, null, targetCollection);
                    }
                }
                return true;
            }

            public boolean visitProperty(Property property) {
                Property targetProperty = targetDescriptor.getProperty(property.getName());
                targetProperty.writeProperty(target, null, property.readProperty(source));
                return true;
            }
        });
        return target;
    }
}
