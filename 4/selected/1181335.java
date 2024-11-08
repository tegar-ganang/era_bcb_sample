package org.objectstyle.cayenne.property;

/**
 * A property descriptor that provides access to a simple object property, delegating
 * property read/write operations to an accessor.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class SimpleProperty implements Property {

    protected ClassDescriptor owner;

    protected PropertyAccessor accessor;

    public SimpleProperty(ClassDescriptor owner, PropertyAccessor accessor) {
        if (accessor == null) {
            throw new IllegalArgumentException("Null accessor");
        }
        this.accessor = accessor;
        this.owner = owner;
    }

    public Object readProperty(Object object) throws PropertyAccessException {
        return readPropertyDirectly(object);
    }

    public void writeProperty(Object object, Object oldValue, Object newValue) throws PropertyAccessException {
        writePropertyDirectly(object, oldValue, newValue);
    }

    public String getName() {
        return accessor.getName();
    }

    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitProperty(this);
    }

    /**
     * Does nothing.
     */
    public void injectValueHolder(Object object) throws PropertyAccessException {
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        writePropertyDirectly(to, accessor.readPropertyDirectly(to), accessor.readPropertyDirectly(from));
    }

    public Object readPropertyDirectly(Object object) throws PropertyAccessException {
        return accessor.readPropertyDirectly(object);
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue) throws PropertyAccessException {
        accessor.writePropertyDirectly(object, oldValue, newValue);
    }
}
