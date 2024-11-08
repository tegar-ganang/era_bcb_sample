package org.granite.generator.reflect;

public class JMethodProperty implements JProperty {

    private final String name;

    private final JMethod readMethod;

    private final JMethod writeMethod;

    private final Class<?> type;

    public JMethodProperty(String name, JMethod readMethod, JMethod writeMethod) {
        if (name == null || (readMethod == null && writeMethod == null)) throw new NullPointerException("Invalid parameters");
        this.name = name;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        this.type = (readMethod != null ? readMethod.getMember().getReturnType() : writeMethod.getMember().getParameterTypes()[0]);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isReadable() {
        return (readMethod != null);
    }

    public boolean isWritable() {
        return (writeMethod != null);
    }

    public JMethod getReadMethod() {
        return readMethod;
    }

    public JMethod getWriteMethod() {
        return writeMethod;
    }

    public int compareTo(JProperty o) {
        return name.compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof JMethodProperty) return ((JMethodProperty) obj).name.equals(name);
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
