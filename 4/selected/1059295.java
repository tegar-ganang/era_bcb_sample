package org.objectstyle.cayenne;

import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.util.Util;

/**
 * An object that merges "backdoor" modifications of the object graph coming from the
 * underlying DataChannel. When doing an update, CayenneContextMergeHandler blocks
 * broadcasting of GraphManager events.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class CayenneContextMergeHandler implements GraphChangeHandler, DataChannelListener {

    CayenneContext context;

    boolean active;

    CayenneContextMergeHandler(CayenneContext context) {
        this.context = context;
        this.active = true;
    }

    public void graphChanged(final GraphEvent e) {
        if (shouldProcessEvent(e) && e.getDiff() != null) {
            runWithEventsDisabled(new Runnable() {

                public void run() {
                    e.getDiff().apply(CayenneContextMergeHandler.this);
                }
            });
            repostAfterMerge(e);
        }
    }

    public void graphFlushed(final GraphEvent e) {
        if (shouldProcessEvent(e)) {
            final boolean hadChanges = context.internalGraphManager().hasChanges();
            runWithEventsDisabled(new Runnable() {

                public void run() {
                    if (e.getDiff() != null) {
                        e.getDiff().apply(CayenneContextMergeHandler.this);
                    }
                    if (!hadChanges) {
                        context.internalGraphManager().stateLog.graphCommitted();
                        context.internalGraphManager().reset();
                    }
                }
            });
            repostAfterMerge(e);
        }
    }

    public void graphRolledback(final GraphEvent e) {
        if (shouldProcessEvent(e)) {
            if (context.internalGraphManager().hasChanges()) {
                runWithEventsDisabled(new Runnable() {

                    public void run() {
                        context.internalGraphManager().graphReverted();
                    }
                });
                repostAfterMerge(e);
            }
        }
    }

    void repostAfterMerge(GraphEvent originalEvent) {
        if (context.isLifecycleEventsEnabled()) {
            context.internalGraphManager().send(originalEvent.getDiff(), DataChannel.GRAPH_CHANGED_SUBJECT, originalEvent.getSource());
        }
    }

    /**
     * Executes merging of the external diff.
     */
    void merge(final GraphDiff diff) {
        runWithEventsDisabled(new Runnable() {

            public void run() {
                diff.apply(CayenneContextMergeHandler.this);
            }
        });
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        Object node = context.internalGraphManager().getNode(nodeId);
        if (node != null) {
            context.internalGraphManager().deadIds().add(nodeId);
            context.internalGraphManager().registerNode(newId, node);
            if (node instanceof Persistent) {
                ((Persistent) node).setObjectId((ObjectId) newId);
            }
        }
    }

    public void nodeCreated(Object nodeId) {
        context.createNewObject((ObjectId) nodeId);
    }

    public void nodeRemoved(Object nodeId) {
        Object object = context.internalGraphManager().getNode(nodeId);
        if (object != null) {
            context.deleteObject((Persistent) object);
        }
    }

    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        Object object = context.internalGraphManager().getNode(nodeId);
        if (object != null) {
            Property p = propertyForId(nodeId, property);
            if (Util.nullSafeEquals(p.readPropertyDirectly(object), oldValue)) {
                p.writePropertyDirectly(object, oldValue, newValue);
                context.internalGraphAction().handleSimplePropertyChange((Persistent) object, property, oldValue, newValue);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        Object source = context.internalGraphManager().getNode(nodeId);
        if (source == null) {
            source = context.createFault((ObjectId) nodeId);
        }
        Object target = context.internalGraphManager().getNode(targetNodeId);
        if (target == null) {
            target = context.createFault((ObjectId) targetNodeId);
        }
        ArcProperty p = (ArcProperty) propertyForId(nodeId, arcId.toString());
        p.writePropertyDirectly(source, null, target);
        try {
            context.internalGraphAction().handleArcPropertyChange((Persistent) source, p, null, target);
        } finally {
            context.internalGraphAction().setArcChangeInProcess(false);
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        Object source = context.internalGraphManager().getNode(nodeId);
        if (source == null) {
            source = context.createFault((ObjectId) nodeId);
        }
        Object target = context.internalGraphManager().getNode(targetNodeId);
        if (target == null) {
            target = context.createFault((ObjectId) targetNodeId);
        }
        ArcProperty p = (ArcProperty) propertyForId(nodeId, arcId.toString());
        p.writePropertyDirectly(source, target, null);
        try {
            context.internalGraphAction().handleArcPropertyChange((Persistent) source, p, target, null);
        } finally {
            context.internalGraphAction().setArcChangeInProcess(false);
        }
    }

    private Property propertyForId(Object nodeId, String propertyName) {
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(((ObjectId) nodeId).getEntityName());
        return descriptor.getProperty(propertyName);
    }

    boolean shouldProcessEvent(GraphEvent e) {
        return active && e.getSource() == context.getChannel() && e.getPostedBy() != context && e.getPostedBy() != context.getChannel();
    }

    private void runWithEventsDisabled(Runnable closure) {
        synchronized (context.internalGraphManager()) {
            boolean changeEventsEnabled = context.internalGraphManager().changeEventsEnabled;
            context.internalGraphManager().changeEventsEnabled = false;
            boolean lifecycleEventsEnabled = context.internalGraphManager().lifecycleEventsEnabled;
            context.internalGraphManager().lifecycleEventsEnabled = false;
            try {
                closure.run();
            } finally {
                context.internalGraphManager().changeEventsEnabled = changeEventsEnabled;
                context.internalGraphManager().lifecycleEventsEnabled = lifecycleEventsEnabled;
            }
        }
    }
}
