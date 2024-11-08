package org.apache.tapestry5.ioc.internal.services;

import org.apache.tapestry5.ioc.AnnotationProvider;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.ioc.services.ClassPropertyAdapter;
import org.apache.tapestry5.ioc.services.PropertyAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class PropertyAdapterImpl implements PropertyAdapter {

    private final ClassPropertyAdapter classAdapter;

    private final String name;

    private final Method readMethod;

    private final Method writeMethod;

    private final Class type;

    private final boolean castRequired;

    private AnnotationProvider annotationProvider;

    PropertyAdapterImpl(ClassPropertyAdapter classAdapter, String name, Class type, Method readMethod, Method writeMethod) {
        this.classAdapter = classAdapter;
        this.name = name;
        this.type = type;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        castRequired = readMethod != null && readMethod.getReturnType() != type;
    }

    public String getName() {
        return name;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public Class getType() {
        return type;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }

    public boolean isRead() {
        return readMethod != null;
    }

    public boolean isUpdate() {
        return writeMethod != null;
    }

    public Object get(Object instance) {
        if (readMethod == null) throw new UnsupportedOperationException(ServiceMessages.readNotSupported(instance, name));
        Throwable fail;
        try {
            return readMethod.invoke(instance);
        } catch (InvocationTargetException ex) {
            fail = ex.getTargetException();
        } catch (Exception ex) {
            fail = ex;
        }
        throw new RuntimeException(ServiceMessages.readFailure(name, instance, fail), fail);
    }

    public void set(Object instance, Object value) {
        if (writeMethod == null) throw new UnsupportedOperationException(ServiceMessages.writeNotSupported(instance, name));
        Throwable fail;
        try {
            writeMethod.invoke(instance, value);
            return;
        } catch (InvocationTargetException ex) {
            fail = ex.getTargetException();
        } catch (Exception ex) {
            fail = ex;
        }
        throw new RuntimeException(ServiceMessages.writeFailure(name, instance, fail), fail);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return getAnnnotationProvider().getAnnotation(annotationClass);
    }

    /**
     * Creates (as needed) the annotation provider for this property.
     */
    private synchronized AnnotationProvider getAnnnotationProvider() {
        if (annotationProvider == null) {
            List<AnnotationProvider> providers = CollectionFactory.newList();
            if (readMethod != null) providers.add(new AccessableObjectAnnotationProvider(readMethod));
            if (writeMethod != null) providers.add(new AccessableObjectAnnotationProvider(writeMethod));
            Class cursor = getBeanType();
            out: while (cursor != null) {
                for (Field f : cursor.getDeclaredFields()) {
                    if (f.getName().equalsIgnoreCase(name)) {
                        providers.add(new AccessableObjectAnnotationProvider(f));
                        break out;
                    }
                }
                cursor = cursor.getSuperclass();
            }
            annotationProvider = AnnotationProviderChain.create(providers);
        }
        return annotationProvider;
    }

    public boolean isCastRequired() {
        return castRequired;
    }

    public ClassPropertyAdapter getClassAdapter() {
        return classAdapter;
    }

    public Class getBeanType() {
        return classAdapter.getBeanType();
    }
}
