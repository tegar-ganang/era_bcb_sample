package net.sourceforge.freejava.bean;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import net.sourceforge.freejava.jdk6compat.jdk7emul.Jdk7Reflect;
import net.sourceforge.freejava.jdk6compat.jdk7emul.ReflectiveOperationException;
import net.sourceforge.freejava.lang.Ref;
import net.sourceforge.freejava.util.exception.NoSuchKeyException;

public class PropertyRef<PropertyType> implements Ref<PropertyType> {

    private final Object bean;

    private final String propertyName;

    private final Class<PropertyType> propertyType;

    private final Method readMethod;

    private final Method writeMethod;

    private final Object[] parameters;

    public PropertyRef(Object bean, String propertyName, Class<PropertyType> propertyType, Method readMethod, Method writeMethod, Object... parameters) {
        if (propertyType == null) throw new NullPointerException("propertyType");
        if (parameters != null && parameters.length == 0) parameters = null;
        this.bean = bean;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        this.parameters = parameters;
    }

    public static <T> PropertyRef<Object> getInstance(T bean, PropertyDescriptor propertyDescriptor, Object... parameters) {
        if (bean == null) throw new NullPointerException("bean");
        @SuppressWarnings("unchecked") Class<T> beanClass = (Class<T>) bean.getClass();
        return getInstance(bean, beanClass, propertyDescriptor, parameters);
    }

    public static <T> PropertyRef<Object> getInstance(T bean, Class<T> beanClass, PropertyDescriptor propertyDescriptor, Object... parameters) {
        String propertyName = propertyDescriptor.getName();
        Method readMethod = propertyDescriptor.getReadMethod();
        Method writeMethod = propertyDescriptor.getWriteMethod();
        PropertyRef<Object> propertyRef = new PropertyRef<Object>(bean, propertyName, Object.class, readMethod, writeMethod, parameters);
        return propertyRef;
    }

    public static <T> PropertyRef<Object> getInstance(T bean, String propertyName, Object... parameters) throws IntrospectionException {
        if (bean == null) throw new NullPointerException("bean");
        @SuppressWarnings("unchecked") Class<T> beanClass = (Class<T>) bean.getClass();
        return getInstance(bean, beanClass, propertyName, parameters);
    }

    public static <T> PropertyRef<Object> getInstance(T bean, Class<T> beanClass, String propertyName, Object... parameters) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            String name = descriptor.getName();
            if (name.equals(propertyName)) {
                return getInstance(bean, beanClass, descriptor, parameters);
            }
        }
        throw new NoSuchKeyException(propertyName);
    }

    @Override
    public PropertyType get() {
        if (readMethod == null) throw new UnsupportedOperationException("Can't read from property. ");
        Object propertyValue;
        try {
            if (parameters == null) propertyValue = Jdk7Reflect.invoke(readMethod, bean); else propertyValue = Jdk7Reflect.invoke(readMethod, bean, parameters);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return propertyType.cast(propertyValue);
    }

    @Override
    public void set(PropertyType value) {
        if (writeMethod == null) throw new UnsupportedOperationException("Can't write to property. ");
        try {
            Jdk7Reflect.invoke(writeMethod, bean, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(bean);
    }
}
