package org.objectstyle.cayenne.property;

import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.Persistent;

/**
 * An ArcProperty for accessing to-one relationships.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class PersistentObjectProperty extends AbstractSingleObjectArcProperty {

    public PersistentObjectProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor, PropertyAccessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    public boolean isFault(Object object) {
        Object target = accessor.readPropertyDirectly(object);
        return target instanceof Fault;
    }

    public Object readProperty(Object object) throws PropertyAccessException {
        Object value = super.readProperty(object);
        if (value instanceof Fault) {
            Object resolved = ((Fault) value).resolveFault((Persistent) object, getName());
            writePropertyDirectly(object, value, resolved);
            value = resolved;
        }
        return value;
    }

    /**
     * Copies a property value that is itself a persistent object from one object to
     * another. If the new value is fault, fault will be copied to the target.
     */
    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        Object fromValue = accessor.readPropertyDirectly(from);
        if (fromValue == null) {
            writePropertyDirectly(to, accessor.readPropertyDirectly(to), null);
        } else {
            writePropertyDirectly(to, accessor.readPropertyDirectly(to), Fault.getToOneFault());
        }
    }
}
