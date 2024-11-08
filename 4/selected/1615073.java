package net.infordata.ifw2m.props;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 */
public abstract class BaseProperty<C, T> implements Property<C, T>, Serializable {

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
    public T get(C instance) {
        if (!isReadable()) throw new IllegalStateException("This isn't a read property.");
        return null;
    }

    /**
   */
    public void set(C instance, T param) {
        if (!isWriteable()) throw new IllegalStateException("This isn't a write property.");
    }

    /**
   */
    public final Class<T> getType() {
        return ivType;
    }

    /**
   */
    public final String getName() {
        return ivName;
    }

    /**
   */
    public final boolean isReadable() {
        return ivReadable;
    }

    /**
   */
    public final boolean isWriteable() {
        return ivWriteable;
    }

    /**
   */
    public final boolean isReadOnly() {
        return (isReadable() && !isWriteable());
    }

    /**
   */
    public final boolean isWriteOnly() {
        return (isWriteable() && !isReadable());
    }

    /**
   */
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
}
