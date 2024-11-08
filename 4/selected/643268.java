package com.javampire.util.dao.generic;

import com.javampire.util.dao.file.FixedLengthRecordDAO;
import com.javampire.util.io.FileAccess;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * TODO: document this.
 *
 * @author <a href="mailto:cnagy@ecircle.de">Csaba Nagy</a>
 * @version $Revision: 1.2 $ $Date: 2007/06/28 18:28:10 $
 */
public class GenericDataNode<T extends GenericBean<T>> extends FixedLengthRecordDAO<T> {

    private final T instance;

    private final GenericBeanFieldHandler<T> fieldHandler;

    public GenericDataNode(String baseFileName, T instance, boolean openForWrite) throws IOException {
        super(baseFileName + ".nfd", getRecordLength(instance), openForWrite ? FileAccess.Type.CREATE : FileAccess.Type.READ);
        this.instance = instance;
        fieldHandler = new GenericBeanFieldHandler<T>(instance);
    }

    public void writeRecord(T record) throws IOException {
        try {
            fieldHandler.writeField(record);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

    public T next(T record) throws IOException {
        if (record == null) {
            record = newRecord();
        }
        try {
            final int recordId = getRecordId();
            record = fieldHandler.readField(record);
            record.setRecordId(recordId);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
        return record;
    }

    public T newRecord() {
        return instance.newRecord();
    }

    public void copy(T record, T destination) {
        record.copyTo(destination);
    }

    static int getRecordLength(GenericBean instance) throws IOException {
        int result = 0;
        try {
            final BeanInfo beanInfo = Introspector.getBeanInfo(instance.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                final Method readMethod = descriptor.getReadMethod();
                final Method writeMethod = descriptor.getWriteMethod();
                if (readMethod != null && writeMethod != null && readMethod.isAnnotationPresent(AttrInfo.class)) {
                    final Object field = readMethod.invoke(instance);
                    result += getFieldLength(field, descriptor);
                }
            }
        } catch (IntrospectionException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
        return result;
    }

    private static int getFieldLength(Object field, PropertyDescriptor descriptor) throws IOException {
        int result = 0;
        Class<?> returnType = field.getClass();
        if (returnType == Byte.class || returnType == Boolean.class) {
            result++;
        } else if (returnType == Short.class) {
            result += 2;
        } else if (returnType == Integer.class) {
            result += 4;
        } else if (returnType == Float.class) {
            result += 4;
        } else if (returnType == Double.class) {
            result += 8;
        } else if (returnType.isAnnotationPresent(GenericBeanInfo.class)) {
            result += getRecordLength((GenericBean) field);
        } else if (returnType.isArray()) {
            final int elementCount = Array.getLength(field);
            final Object element = Array.get(field, 0);
            result += elementCount * getFieldLength(element, descriptor);
        } else {
            throw new IOException("Invalid attribute found: " + descriptor.getName());
        }
        return result;
    }

    /** @noinspection unchecked*/
    private FieldHandler createFieldHandler(Object field) throws IOException {
        FieldHandler fieldHandler = null;
        Class<?> returnType = field.getClass();
        if (returnType == Byte.class) {
            fieldHandler = new ByteFieldHandler();
        } else if (returnType == Short.class) {
            fieldHandler = new ShortFieldHandler();
        } else if (returnType == Integer.class) {
            fieldHandler = new IntFieldHandler();
        } else if (returnType == Double.class) {
            fieldHandler = new DoubleFieldHandler();
        } else if (returnType == Float.class) {
            fieldHandler = new FloatFieldHandler();
        } else if (returnType == Boolean.class) {
            fieldHandler = new BooleanFieldHandler();
        } else if (returnType.isAnnotationPresent(GenericBeanInfo.class)) {
            fieldHandler = new GenericBeanFieldHandler((GenericBean) field);
        } else if (returnType.isArray()) {
            fieldHandler = new ArrayFieldHandler(field);
        }
        return fieldHandler;
    }

    private static interface FieldHandler {

        void writeField(Object field) throws IOException, IllegalAccessException, InvocationTargetException;

        Object readField(Object field) throws IOException, IllegalAccessException, InvocationTargetException;
    }

    private class GenericBeanFieldHandler<B extends GenericBean<B>> implements FieldHandler {

        private final GenericBean<B> beanInstance;

        private final ArrayList<Method> readMethods;

        private final ArrayList<Method> writeMethods;

        private final ArrayList<FieldHandler> fieldHandlers;

        public GenericBeanFieldHandler(GenericBean<B> instance) throws IOException {
            beanInstance = instance;
            readMethods = new ArrayList<Method>();
            writeMethods = new ArrayList<Method>();
            fieldHandlers = new ArrayList<FieldHandler>();
            try {
                final BeanInfo beanInfo = Introspector.getBeanInfo(instance.getClass());
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                for (PropertyDescriptor descriptor : propertyDescriptors) {
                    final Method readMethod = descriptor.getReadMethod();
                    final Method writeMethod = descriptor.getWriteMethod();
                    if (readMethod != null && writeMethod != null && readMethod.isAnnotationPresent(AttrInfo.class)) {
                        readMethods.add(readMethod);
                        writeMethods.add(writeMethod);
                        final Object field = readMethod.invoke(instance);
                        final FieldHandler fieldHandler = createFieldHandler(field);
                        if (fieldHandler == null) {
                            throw new IOException("Invalid attribute found: " + readMethod.getName());
                        }
                        fieldHandlers.add(fieldHandler);
                    }
                }
            } catch (IntrospectionException e) {
                e.printStackTrace();
                throw new IOException(e.getMessage());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new IOException(e.getMessage());
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new IOException(e.getMessage());
            }
        }

        public void writeField(Object bean) throws IOException, IllegalAccessException, InvocationTargetException {
            for (int i = 0; i < fieldHandlers.size(); i++) {
                final Object field = readMethods.get(i).invoke(bean);
                fieldHandlers.get(i).writeField(field);
            }
        }

        /** @noinspection unchecked*/
        public B readField(Object bean) throws IOException, IllegalAccessException, InvocationTargetException {
            if (bean == null) bean = beanInstance.newRecord();
            for (int i = 0; i < fieldHandlers.size(); i++) {
                Object field = readMethods.get(i).invoke(bean);
                field = fieldHandlers.get(i).readField(field);
                writeMethods.get(i).invoke(bean, field);
            }
            return (B) bean;
        }
    }

    private class ArrayFieldHandler implements FieldHandler {

        private final int length;

        private final FieldHandler elementHandler;

        private final Class<?> elementClass;

        public ArrayFieldHandler(Object array) throws IOException {
            length = Array.getLength(array);
            elementClass = array.getClass().getComponentType();
            Object element = Array.get(array, 0);
            elementHandler = createFieldHandler(element);
            if (elementHandler == null) {
                throw new IllegalArgumentException("Invalid array type: " + elementClass);
            }
        }

        public void writeField(Object array) throws IOException, IllegalAccessException, InvocationTargetException {
            for (int i = 0; i < length; i++) {
                elementHandler.writeField(Array.get(array, i));
            }
        }

        public Object readField(Object field) throws IOException, IllegalAccessException, InvocationTargetException {
            final Object result = Array.newInstance(elementClass, length);
            for (int i = 0; i < length; i++) {
                final Object element = Array.get(field, i);
                Array.set(result, i, elementHandler.readField(element));
            }
            return result;
        }
    }

    private class BooleanFieldHandler implements FieldHandler {

        public void writeField(Object field) throws IOException {
            fileWriter.writeByte(((Boolean) field) ? 1 : 0);
        }

        public Object readField(Object field) throws IOException {
            return new Boolean(fileReader.readByte() != 0);
        }
    }

    private class ByteFieldHandler implements FieldHandler {

        public void writeField(Object field) throws IOException {
            fileWriter.writeByte((Byte) field);
        }

        public Object readField(Object field) throws IOException {
            return fileReader.readByte();
        }
    }

    private class ShortFieldHandler implements FieldHandler {

        public void writeField(Object field) throws IOException {
            fileWriter.writeShort((Short) field);
        }

        public Object readField(Object field) throws IOException {
            return fileReader.readShort();
        }
    }

    private class IntFieldHandler implements FieldHandler {

        public void writeField(Object field) throws IOException {
            fileWriter.writeInt((Integer) field);
        }

        public Object readField(Object field) throws IOException {
            return fileReader.readInt();
        }
    }

    private class DoubleFieldHandler implements FieldHandler {

        public void writeField(Object field) throws IOException {
            fileWriter.writeDouble((Double) field);
        }

        public Object readField(Object field) throws IOException {
            return fileReader.readDouble();
        }
    }

    private class FloatFieldHandler implements FieldHandler {

        public void writeField(Object field) throws IOException {
            fileWriter.writeFloat((Float) field);
        }

        public Object readField(Object field) throws IOException {
            return fileReader.readFloat();
        }
    }
}
