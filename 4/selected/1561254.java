package org.granite.generator.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

public class JFieldProperty extends JMember<Field> implements JProperty {

    private final JMethod readMethod;

    private final JMethod writeMethod;

    private final boolean identifier;

    public JFieldProperty(Field field, JMethod readMethod, JMethod writeMethod) {
        super(field);
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        this.identifier = (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class) || (readMethod != null && (readMethod.getMember().isAnnotationPresent(Id.class) || readMethod.getMember().isAnnotationPresent(EmbeddedId.class))) || (writeMethod != null && (writeMethod.getMember().isAnnotationPresent(Id.class) || writeMethod.getMember().isAnnotationPresent(EmbeddedId.class))));
    }

    public Class<?> getType() {
        return getMember().getType();
    }

    public boolean hasTypePackage() {
        return (getTypePackageName().length() > 0);
    }

    public String getTypePackageName() {
        Package p = getType().getPackage();
        return (p != null ? p.getName() : "");
    }

    public String getTypeName() {
        return getType().getSimpleName();
    }

    public boolean isReadable() {
        return (Modifier.isPublic(getMember().getModifiers()) || readMethod != null);
    }

    public boolean isWritable() {
        return (Modifier.isPublic(getMember().getModifiers()) || writeMethod != null);
    }

    public JMethod getReadMethod() {
        return readMethod;
    }

    public JMethod getWriteMethod() {
        return writeMethod;
    }

    public boolean isIdentifier() {
        return identifier;
    }

    public int compareTo(JProperty o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof JMethodProperty) return ((JMethodProperty) obj).getName().equals(getName());
        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
