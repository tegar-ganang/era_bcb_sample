package org.springframework.binding.support;

import org.springframework.binding.PropertyMetadata;

public class DefaultFieldMetadata2 implements PropertyMetadata {

    private final String propertyName;

    private final Class type;

    private final boolean readable;

    private final boolean writeable;

    public DefaultFieldMetadata2(String name, Class type, boolean readable, boolean writeable) {
        this.propertyName = name;
        this.type = type;
        this.readable = readable;
        this.writeable = writeable;
    }

    public String getName() {
        return propertyName;
    }

    public Class getType() {
        return type;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWriteable() {
        return writeable;
    }
}
