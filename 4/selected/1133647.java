package org.plazmaforge.framework.util;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.plazmaforge.framework.core.exception.ApplicationException;

public class CloneUtils {

    public static Object cloneObject(Object originalObject) throws ApplicationException {
        if (originalObject == null) {
            return null;
        }
        if (!(originalObject instanceof Serializable)) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(originalObject);
            oos.close();
            byte[] data = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object cloneObject = ois.readObject();
            return cloneObject;
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        } catch (ClassNotFoundException ex) {
            throw new ApplicationException(ex);
        }
    }

    public static Object copyObject(Object originalObject, CopyReseter reseter, boolean isRecreateCollection) throws ApplicationException {
        Object copyObject = cloneObject(originalObject);
        if (copyObject == null) {
            return null;
        }
        if (reseter != null) {
            reseter.resetObject(copyObject);
        }
        if (isRecreateCollection) {
            recreateCollections(copyObject, reseter);
        }
        return copyObject;
    }

    public static void recreateCollections(Object obj, CopyReseter reseter) throws ApplicationException {
        if (obj == null) {
            return;
        }
        PropertyDescriptor[] props = ClassUtils.getBeanCollectionProperties(obj.getClass());
        for (int i = 0; i < props.length; i++) {
            PropertyDescriptor desc = props[i];
            Class type = desc.getPropertyType();
            Method readMethod = desc.getReadMethod();
            Method writeMethod = desc.getWriteMethod();
            if (readMethod == null || writeMethod == null) {
                continue;
            }
            Object value = ClassUtils.getBeanValue(readMethod, obj);
            if (!(value instanceof Collection)) {
                continue;
            }
            Collection collection = (Collection) value;
            resetCollection(collection, reseter);
            Object cloneCollection = cloneCollection(collection, type);
            ClassUtils.setBeanValue(writeMethod, obj, cloneCollection);
        }
    }

    public static void resetCollection(Collection collection, CopyReseter reseter) {
        if (collection == null || reseter == null) {
            return;
        }
        Iterator itr = collection.iterator();
        while (itr.hasNext()) {
            reseter.resetObject(itr.next());
        }
    }

    public static Collection cloneCollection(Collection collection, Class type) throws ApplicationException {
        if (collection == null) {
            return null;
        }
        if (collection instanceof List) {
            if (type.isInterface()) {
                return cloneList((List) collection);
            }
            return (Collection) ClassUtils.getClassInstance(type);
        }
        return collection;
    }

    public static List cloneList(List list) {
        if (list == null) {
            return null;
        }
        List newList = new ArrayList();
        newList.addAll(list);
        return newList;
    }

    public static interface CopyReseter {

        void resetObject(Object obj);
    }
}
