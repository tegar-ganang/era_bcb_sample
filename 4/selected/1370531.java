package org.objectstyle.cayenne;

import java.util.Collection;
import java.util.List;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.Query;

/**
 * A noop ObjectContext used for unit testing.
 * 
 * @author Andrus Adamchik
 */
public class MockObjectContext implements ObjectContext {

    protected GraphManager graphManager;

    public MockObjectContext() {
        super();
    }

    public MockObjectContext(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public DataChannel getChannel() {
        return null;
    }

    public GraphManager getGraphManager() {
        return graphManager;
    }

    public Persistent localObject(ObjectId id, Persistent prototype) {
        return null;
    }

    public void commitChangesToParent() {
    }

    public void rollbackChangesLocally() {
    }

    public void rollbackChanges() {
    }

    public Collection newObjects() {
        return null;
    }

    public Collection deletedObjects() {
        return null;
    }

    public Collection modifiedObjects() {
        return null;
    }

    public List performQuery(Query query) {
        return null;
    }

    public int[] performNonSelectingQuery(Query query) {
        return null;
    }

    public void commitChanges() {
    }

    public void deleteObject(Persistent object) {
    }

    public Persistent newObject(Class persistentClass) {
        return null;
    }

    public void prepareForAccess(Persistent persistent, String property) {
    }

    public void propertyChanged(Persistent persistent, String property, Object oldValue, Object newValue) {
    }

    public void addedToCollectionProperty(Persistent object, String property, Persistent added) {
    }

    public void removedFromCollectionProperty(Persistent object, String property, Persistent removed) {
    }

    public Collection uncommittedObjects() {
        return null;
    }

    public QueryResponse performGenericQuery(Query queryPlan) {
        return null;
    }
}
