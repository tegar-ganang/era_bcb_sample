package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.DataChannelListener;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.ObjectStore.SnapshotEventDecorator;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.CollectionProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.property.SingleObjectArcProperty;
import org.objectstyle.cayenne.util.Util;

/**
 * A listener of GraphEvents sent by the DataChannel that merges changes to the
 * DataContext.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataContextMergeHandler implements GraphChangeHandler, DataChannelListener {

    private boolean active;

    private DataContext context;

    DataContextMergeHandler(DataContext context) {
        this.active = true;
        this.context = context;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns true if this object is active and an event came from our channel, but did
     * not originate in it.
     */
    private boolean shouldProcessEvent(GraphEvent e) {
        if (!active) {
            return false;
        }
        return e.getSource() == context.getChannel() && e.getPostedBy() != context && e.getPostedBy() != context.getChannel();
    }

    private Property propertyForId(Object nodeId, String propertyName) {
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(((ObjectId) nodeId).getEntityName());
        return descriptor.getProperty(propertyName);
    }

    public void graphChanged(GraphEvent event) {
        if (shouldProcessEvent(event)) {
            GraphDiff diff = event.getDiff();
            if (diff instanceof SnapshotEventDecorator) {
                SnapshotEvent decoratedEvent = ((SnapshotEventDecorator) diff).getEvent();
                context.getObjectStore().processSnapshotEvent(decoratedEvent);
            } else {
                synchronized (context.getObjectStore()) {
                    diff.apply(this);
                }
            }
            context.fireDataChannelChanged(event.getPostedBy(), event.getDiff());
        }
    }

    public void graphFlushed(GraphEvent event) {
        if (shouldProcessEvent(event)) {
            event.getDiff().apply(this);
            context.fireDataChannelChanged(event.getPostedBy(), event.getDiff());
        }
    }

    public void graphRolledback(GraphEvent event) {
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        context.getObjectStore().processIdChange(nodeId, newId);
    }

    public void nodeCreated(Object nodeId) {
    }

    public void nodeRemoved(Object nodeId) {
        context.getObjectStore().processDeletedID(nodeId);
    }

    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        Persistent object = (Persistent) context.getGraphManager().getNode(nodeId);
        if (object != null && object.getPersistenceState() != PersistenceState.HOLLOW) {
            Property p = propertyForId(nodeId, property);
            if (Util.nullSafeEquals(p.readPropertyDirectly(object), oldValue)) {
                p.writePropertyDirectly(object, oldValue, newValue);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        Persistent source = (Persistent) context.getGraphManager().getNode(nodeId);
        if (source != null && source.getPersistenceState() != PersistenceState.HOLLOW) {
            Object target = context.localObject((ObjectId) targetNodeId, null);
            Property p = propertyForId(nodeId, arcId.toString());
            if (p instanceof CollectionProperty) {
                ((CollectionProperty) p).addTarget(source, target, false);
            } else {
                ((SingleObjectArcProperty) p).setTarget(source, target, false);
            }
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        Persistent source = (Persistent) context.getGraphManager().getNode(nodeId);
        if (source != null && source.getPersistenceState() != PersistenceState.HOLLOW) {
            Object target = context.localObject((ObjectId) targetNodeId, null);
            Property p = propertyForId(nodeId, arcId.toString());
            if (p instanceof CollectionProperty) {
                ((CollectionProperty) p).removeTarget(source, target, false);
            } else {
                ((SingleObjectArcProperty) p).setTarget(source, null, false);
            }
        }
    }
}
