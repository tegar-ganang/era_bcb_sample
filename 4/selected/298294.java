package net.infordata.ifw2.props;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 */
public abstract class BaseProperty<C, T> extends Property<C, T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient Class<T> ivType;

    private String ivName;

    private ClassInfo<C> ivClassInfo;

    private transient boolean ivReadable;

    private transient boolean ivWriteable;

    /**
   */
    protected BaseProperty(ClassInfo<C> classInfo, String name, Class<T> type, boolean readable, boolean writeable) {
        ivClassInfo = classInfo;
        ivType = type;
        ivName = name;
        ivReadable = readable;
        ivWriteable = writeable;
    }

    /**
   */
    @Override
    public T get(C instance) {
        if (!isReadable()) throw new IllegalStateException("This isn't a read property.");
        return null;
    }

    /**
   */
    @Override
    public void set(C instance, T param) {
        if (!isWriteable()) throw new IllegalStateException("This isn't a write property.");
    }

    /**
   */
    @Override
    public final Class<T> getType() {
        return ivType;
    }

    /**
   */
    @Override
    public final String getName() {
        return ivName;
    }

    /**
   */
    @Override
    public final boolean isReadable() {
        return ivReadable;
    }

    /**
   */
    @Override
    public final boolean isWriteable() {
        return ivWriteable;
    }

    /**
   */
    @Override
    public final boolean isReadOnly() {
        return (isReadable() && !isWriteable());
    }

    /**
   */
    @Override
    public final boolean isWriteOnly() {
        return (isWriteable() && !isReadable());
    }

    /**
   */
    @Override
    public final ClassInfo<C> getClassInfo() {
        return ivClassInfo;
    }

    Object readResolve() throws ObjectStreamException {
        return ivClassInfo.getPN().getProperty(ivName);
    }

    /**
   */
    @Override
    public String toString() {
        return getClassInfo() + " - " + getName() + " - " + getType() + " -  " + (isReadOnly() ? "READ ONLY" : "") + (isWriteOnly() ? "WRITE ONLY" : "");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ivClassInfo == null) ? 0 : ivClassInfo.hashCode());
        result = prime * result + ((ivName == null) ? 0 : ivName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        @SuppressWarnings("rawtypes") BaseProperty other = (BaseProperty) obj;
        if (ivClassInfo == null) {
            if (other.ivClassInfo != null) return false;
        } else if (!ivClassInfo.equals(other.ivClassInfo)) return false;
        if (ivName == null) {
            if (other.ivName != null) return false;
        } else if (!ivName.equals(other.ivName)) return false;
        return true;
    }
}
