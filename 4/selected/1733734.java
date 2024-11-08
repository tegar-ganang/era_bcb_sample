package com.mycila.jmx.export;

import com.mycila.jmx.util.ClassUtils;
import com.mycila.jmx.util.ReflectionUtils;
import com.mycila.jmx.util.StringUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class BeanProperty<T> implements AnnotatedElement {

    private Method readMethod;

    private Method writeMethod;

    private final Class<T> type;

    private final String name;

    private BeanProperty(String name, Method readMethod, Method writeMethod) {
        if (readMethod == null && writeMethod == null) throw new IllegalArgumentException("Invalid property " + name + ": missing at least one accessor method");
        if (readMethod != null && writeMethod != null && !readMethod.getReturnType().equals(writeMethod.getParameterTypes()[0])) throw new IllegalArgumentException("return type differs: " + readMethod.getReturnType() + " and " + writeMethod.getParameterTypes()[0]);
        this.name = name;
        this.type = (Class<T>) (readMethod != null ? readMethod.getReturnType() : writeMethod.getParameterTypes()[0]);
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isReadable() {
        return readMethod != null;
    }

    public boolean isWritable() {
        return writeMethod != null;
    }

    public void clearReadable() {
        readMethod = null;
    }

    public void clearWritable() {
        writeMethod = null;
    }

    public T get(Object o) throws Throwable {
        if (!isReadable()) throw new IllegalStateException("Property not readable: " + this);
        if (!getReadMethod().isAccessible()) getReadMethod().setAccessible(true);
        try {
            Object res = getReadMethod().invoke(o);
            if (!ClassUtils.isAssignableValue(getType(), res)) throw new IllegalArgumentException("Invalid property: got type " + res.getClass().getName() + " but expect " + getType().getName());
            return (T) res;
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public void set(Object o, T value) throws Throwable {
        if (!isWritable()) throw new IllegalStateException("Property not writable: " + this);
        if (!getWriteMethod().isAccessible()) getWriteMethod().setAccessible(true);
        try {
            getWriteMethod().invoke(o, value);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        A annot = null;
        if (isReadable()) annot = getReadMethod().getAnnotation(annotationClass);
        if (annot == null && isWritable()) annot = getWriteMethod().getAnnotation(annotationClass);
        return annot;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return isReadable() && getReadMethod().isAnnotationPresent(annotationClass) || isWritable() && getWriteMethod().isAnnotationPresent(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        Annotation[] annots = new Annotation[0];
        if (isReadable()) annots = getReadMethod().getAnnotations();
        if (annots.length == 0 && isWritable()) annots = getWriteMethod().getAnnotations();
        return annots;
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        Annotation[] annots = new Annotation[0];
        if (isReadable()) annots = getReadMethod().getDeclaredAnnotations();
        if (annots.length == 0 && isWritable()) annots = getWriteMethod().getDeclaredAnnotations();
        return annots;
    }

    @Override
    public String toString() {
        return ClassUtils.getQualifiedName(getType()) + " " + getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeanProperty that = (BeanProperty) o;
        return getName().equals(that.getName()) && getType().equals(that.getType());
    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + getName().hashCode();
        return result;
    }

    public static BeanProperty<?> findProperty(Class<?> clazz, Method method) {
        if (ReflectionUtils.isIsMethod(method)) return findProperty(clazz, StringUtils.uncapitalize(method.getName().substring(2)), method.getReturnType());
        if (ReflectionUtils.isGetMethod(method)) return findProperty(clazz, StringUtils.uncapitalize(method.getName().substring(3)), method.getReturnType());
        if (ReflectionUtils.isSetter(method)) return findProperty(clazz, StringUtils.uncapitalize(method.getName().substring(3)), method.getParameterTypes()[0]);
        return null;
    }

    public static BeanProperty<?> findProperty(Class<?> clazz, String property) {
        return findProperty(clazz, property, null);
    }

    public static <T> BeanProperty<T> findProperty(Class<?> clazz, String property, Class<T> type) {
        String name = StringUtils.capitalize(property);
        Method is = ReflectionUtils.findMethod(clazz, "is" + name, type, new Class<?>[0]);
        Method get = ReflectionUtils.findMethod(clazz, "get" + name, type, new Class<?>[0]);
        Method setter = ReflectionUtils.findMethod(clazz, "set" + name, Void.TYPE, type);
        Method getter = get != null ? get : is;
        if (setter == null && getter == null || setter != null && getter != null && !setter.getParameterTypes()[0].equals(getter.getReturnType())) return null;
        return new BeanProperty<T>(property, getter, setter);
    }
}
