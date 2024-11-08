package com.angel.architecture.flex.syncronization.bean;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.angel.architecture.exceptions.NonBusinessException;

public class BeanProperty {

    private String name;

    private Class<?> type;

    private Method readMethod, writeMethod;

    private Field field;

    public BeanProperty(PropertyDescriptor descriptor) {
        this(descriptor.getName(), descriptor.getPropertyType(), descriptor.getReadMethod(), descriptor.getWriteMethod(), null);
    }

    protected BeanProperty(String name, Class<?> type, Method read, Method write, Field field) {
        this.name = name;
        this.type = type;
        this.writeMethod = write;
        this.readMethod = read;
        this.field = field;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isWrite() {
        return writeMethod != null || field != null;
    }

    public boolean isRead() {
        return readMethod != null || field != null;
    }

    public Class<?> getReadDeclaringClass() {
        if (readMethod != null) return readMethod.getDeclaringClass(); else if (field != null) return field.getDeclaringClass(); else return null;
    }

    public Class<?> getReadType() {
        if (readMethod != null) return readMethod.getReturnType(); else if (field != null) return field.getType(); else return null;
    }

    public String getWriteName() {
        if (writeMethod != null) return "method " + writeMethod.getName(); else if (field != null) return "field " + field.getName(); else return null;
    }

    public void set(Object bean, Object value) throws IllegalAccessException, InvocationTargetException {
        try {
            if (writeMethod != null) {
                writeMethod.invoke(bean, new Object[] { value });
            } else if (field != null) {
                field.set(bean, value);
            } else {
                return;
            }
        } catch (Exception e) {
            throw new NonBusinessException("Error al tratar de setear una propiedad con setter = " + writeMethod, e);
        }
    }

    public Object get(Object bean) throws IllegalAccessException, InvocationTargetException {
        try {
            Object parameters = null;
            Object obj = null;
            if (readMethod != null) {
                obj = readMethod.invoke(bean, parameters);
            } else if (field != null) {
                obj = field.get(bean);
            }
            return obj;
        } catch (Exception e) {
            throw new NonBusinessException("Error al tratar de obtener una propiedad con getter = " + readMethod, e);
        }
    }
}
