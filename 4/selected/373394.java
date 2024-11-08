package org.granite.generator.as3.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.granite.generator.as3.As3Type;

/**
 * @author Franck WOLFF
 */
public class JavaFieldProperty extends JavaMember<Field> implements JavaProperty {

    private final JavaMethod readMethod;

    private final JavaMethod writeMethod;

    private final JavaTypeFactory provider;

    public JavaFieldProperty(JavaTypeFactory provider, Field field, JavaMethod readMethod, JavaMethod writeMethod) {
        super(field);
        this.provider = provider;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
    }

    public Class<?> getType() {
        return getMember().getType();
    }

    public Type[] getGenericTypes() {
        Type type = getMember().getGenericType();
        if (!(type instanceof ParameterizedType)) return null;
        return ((ParameterizedType) type).getActualTypeArguments();
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

    public boolean isExternalizedProperty() {
        return false;
    }

    public boolean isEnum() {
        Class<?> type = getType();
        return (type.isEnum() || Enum.class.getName().equals(type.getName()));
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return ((getMember().isAnnotationPresent(annotationClass)) || (readMethod != null && readMethod.getMember().isAnnotationPresent(annotationClass)) || (writeMethod != null && writeMethod.getMember().isAnnotationPresent(annotationClass)));
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        T annotation = getMember().getAnnotation(annotationClass);
        if (annotation != null) return annotation;
        if (readMethod != null) {
            annotation = readMethod.getMember().getAnnotation(annotationClass);
            if (annotation != null) return annotation;
        }
        if (writeMethod != null) {
            annotation = writeMethod.getMember().getAnnotation(annotationClass);
            if (annotation != null) return annotation;
        }
        return null;
    }

    public Annotation[] getDeclaredAnnotations() {
        List<Annotation> annos = new ArrayList<Annotation>(Arrays.asList(getMember().getDeclaredAnnotations()));
        if (readMethod != null) annos.addAll(Arrays.asList(readMethod.getMember().getDeclaredAnnotations()));
        if (writeMethod != null) annos.addAll(Arrays.asList(writeMethod.getMember().getDeclaredAnnotations()));
        return annos.toArray(new Annotation[0]);
    }

    public boolean isReadOverride() {
        return (readMethod != null && readMethod.isOverride());
    }

    public boolean isWriteOverride() {
        return (writeMethod != null && writeMethod.isOverride());
    }

    public JavaMethod getReadMethod() {
        return readMethod;
    }

    public JavaMethod getWriteMethod() {
        return writeMethod;
    }

    public As3Type getAs3Type() {
        return provider.getAs3Type(getType());
    }

    public int compareTo(JavaProperty o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof JavaMethodProperty) return ((JavaMethodProperty) obj).getName().equals(getName());
        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name=" + getName() + ", readable=" + (readMethod != null) + ", writable=" + (writeMethod != null) + "}";
    }
}
