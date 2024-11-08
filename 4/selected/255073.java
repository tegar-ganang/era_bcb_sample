package org.objectstyle.cayenne.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.RelationshipQuery;

/**
 * A helper class that implements
 * {@link org.objectstyle.cayenne.DataChannel#onQuery(ObjectContext, Query)} logic on
 * behalf of an ObjectContext.
 * <p>
 * <i>Intended for internal use only.</i>
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ObjectContextQueryAction {

    protected static final boolean DONE = true;

    protected ObjectContext targetContext;

    protected ObjectContext actingContext;

    protected Query query;

    protected QueryMetadata metadata;

    protected transient QueryResponse response;

    public ObjectContextQueryAction(ObjectContext actingContext, ObjectContext targetContext, Query query) {
        this.actingContext = actingContext;
        this.query = query;
        this.targetContext = targetContext != actingContext ? targetContext : null;
        this.metadata = query.getMetaData(actingContext.getEntityResolver());
    }

    /**
     * Worker method that perfomrs internal query.
     */
    public QueryResponse execute() {
        if (interceptOIDQuery() != DONE) {
            if (interceptRelationshipQuery() != DONE) {
                runQuery();
            }
        }
        interceptObjectConversion();
        return response;
    }

    /**
     * Transfers fetched objects into the target context if it is different from "acting"
     * context. Note that when this method is invoked, result objects are already
     * registered with acting context by the parent channel.
     */
    protected void interceptObjectConversion() {
        if (targetContext != null && !metadata.isFetchingDataRows()) {
            GenericResponse childResponse = new GenericResponse();
            for (response.reset(); response.next(); ) {
                if (response.isList()) {
                    List objects = response.currentList();
                    if (objects.isEmpty()) {
                        childResponse.addResultList(objects);
                    } else {
                        List childObjects = new ArrayList(objects.size());
                        Iterator it = objects.iterator();
                        while (it.hasNext()) {
                            Persistent object = (Persistent) it.next();
                            childObjects.add(targetContext.localObject(object.getObjectId(), object));
                        }
                        childResponse.addResultList(childObjects);
                    }
                } else {
                    childResponse.addBatchUpdateCount(response.currentUpdateCount());
                }
            }
            response = childResponse;
        }
    }

    protected boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery) {
            ObjectIdQuery oidQuery = (ObjectIdQuery) query;
            if (!oidQuery.isFetchMandatory() && !oidQuery.isFetchingDataRows()) {
                Object object = actingContext.getGraphManager().getNode(oidQuery.getObjectId());
                if (object != null) {
                    this.response = new ListResponse(object);
                    return DONE;
                }
            }
        }
        return !DONE;
    }

    protected boolean interceptRelationshipQuery() {
        if (query instanceof RelationshipQuery) {
            RelationshipQuery relationshipQuery = (RelationshipQuery) query;
            if (!relationshipQuery.isRefreshing()) {
                if (targetContext == null && relationshipQuery.getRelationship(actingContext.getEntityResolver()).isToMany()) {
                    return !DONE;
                }
                ObjectId id = relationshipQuery.getObjectId();
                Object object = actingContext.getGraphManager().getNode(id);
                if (object != null) {
                    ClassDescriptor descriptor = actingContext.getEntityResolver().getClassDescriptor(id.getEntityName());
                    if (!descriptor.isFault(object)) {
                        ArcProperty property = (ArcProperty) descriptor.getProperty(relationshipQuery.getRelationshipName());
                        if (!property.isFault(object)) {
                            Object related = property.readPropertyDirectly(object);
                            List result;
                            if (related == null) {
                                result = new ArrayList(1);
                            } else if (related instanceof List) {
                                result = (List) related;
                            } else {
                                result = new ArrayList(1);
                                result.add(related);
                            }
                            this.response = new ListResponse(result);
                            return DONE;
                        }
                    }
                }
            }
        }
        return !DONE;
    }

    /**
     * Fetches data from the channel.
     */
    protected void runQuery() {
        this.response = actingContext.getChannel().onQuery(actingContext, query);
    }
}
