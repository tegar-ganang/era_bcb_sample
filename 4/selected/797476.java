package org.granite.generator.as3.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.granite.generator.as3.As3Type;
import org.granite.messaging.amf.io.util.externalizer.annotation.ExternalizedProperty;

/**
 * @author Franck WOLFF
 */
public class JavaMethodProperty implements JavaProperty {

    private final String name;

    private final JavaMethod readMethod;

    private final JavaMethod writeMethod;

    private final Class<?> type;

    private final As3Type as3Type;

    private final boolean externalizedProperty;

    public JavaMethodProperty(JavaTypeFactory provider, String name, JavaMethod readMethod, JavaMethod writeMethod) {
        if (name == null || (readMethod == null && writeMethod == null)) throw new NullPointerException("Invalid parameters");
        this.name = name;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        this.type = (readMethod != null ? readMethod.getMember().getReturnType() : writeMethod.getMember().getParameterTypes()[0]);
        this.as3Type = provider.getAs3Type(type);
        this.externalizedProperty = (readMethod != null && readMethod.getMember().isAnnotationPresent(ExternalizedProperty.class));
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Type[] getGenericTypes() {
        Type type = readMethod != null ? readMethod.getMember().getGenericReturnType() : writeMethod.getMember().getGenericParameterTypes()[0];
        if (!(type instanceof ParameterizedType)) return null;
        return ((ParameterizedType) type).getActualTypeArguments();
    }

    public boolean isReadable() {
        return (readMethod != null);
    }

    public boolean isWritable() {
        return (writeMethod != null);
    }

    public boolean isExternalizedProperty() {
        return externalizedProperty;
    }

    public boolean isEnum() {
        return (type.isEnum() || Enum.class.getName().equals(type.getName()));
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return ((readMethod != null && readMethod.getMember().isAnnotationPresent(annotationClass)) || (writeMethod != null && writeMethod.getMember().isAnnotationPresent(annotationClass)));
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        T annotation = null;
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
        List<Annotation> annos = new ArrayList<Annotation>();
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
        return as3Type;
    }

    public int compareTo(JavaProperty o) {
        return name.compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof JavaMethodProperty) return ((JavaMethodProperty) obj).name.equals(name);
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
