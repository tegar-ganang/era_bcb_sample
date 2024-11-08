package com.ssd.mda.core.metadata.model.descriptor;

import java.lang.reflect.Method;

/**
 * Wrapper class for attribute metadata elements.
 * It is used as a unique identifier in the metadata cache map, so
 * it needs to override equals() and hashCode(). 
 * 
 * @author Flavius Burca
 */
public class MetadataAttributeDescriptor implements NamedMetadataElement {

    private String name;

    private Method readMethod;

    private Method writeMethod;

    public MetadataAttributeDescriptor(String name, Method readMethod, Method writeMethod) {
        if (readMethod == null) throw new IllegalArgumentException("readMethod must not be null");
        this.name = name;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
    }

    public String getName() {
        return name;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof MetadataAttributeDescriptor) {
            return this.getReadMethod().equals(((MetadataAttributeDescriptor) obj).getReadMethod());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getReadMethod().hashCode();
    }
}
