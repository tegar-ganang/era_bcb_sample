package net.wotonomy.util;

import net.wotonomy.foundation.*;
import java.io.*;
import java.util.*;

/**
* Duplicator makes use of Introspector to duplicate objects,
* either by shallow copy, deep copy, or by copying properties
* from one object to apply to another object.  You may find this
* class useful because java.lang.Object.clone() only supports
* shallow copying.
*
* @author michael@mpowers.net
* @author $Author: $
* @version $Revision: 188 $
*/
public class Duplicator {

    /**
    * Used to represent null values for properties in the
    * maps returned by readProperties and cloneProperties
    * and in the parameter to writeProperties.
    * This actually references the NSNull instance.
    */
    public static final Object NULL = NSNull.nullValue();

    private static NSSelector clone = new NSSelector("clone");

    /**
    * Returns a Map containing only the mutable properties 
    * for the specified object and their values.
    * Any null values for properties will be represented with
    * the NULL object.
    */
    public static Map readPropertiesForObject(Object anObject) {
        List readProperties = new ArrayList();
        String[] read = Introspector.getReadPropertiesForObject(anObject);
        for (int i = 0; i < read.length; i++) {
            readProperties.add(read[i]);
        }
        List properties = new ArrayList();
        String[] write = Introspector.getWritePropertiesForObject(anObject);
        for (int i = 0; i < write.length; i++) {
            properties.add(write[i]);
        }
        properties.retainAll(readProperties);
        NSMutableDictionary result = new NSMutableDictionary();
        String key;
        Object value;
        Iterator it = properties.iterator();
        while (it.hasNext()) {
            key = it.next().toString();
            value = Introspector.get(anObject, key);
            if (value == null) value = NULL;
            result.setObjectForKey(value, key);
        }
        return result;
    }

    /**
    * Returns a Map containing only the mutable properties 
    * for the specified object and deep clones of their values.
    */
    public static Map clonePropertiesForObject(Object anObject) {
        Object key;
        Map result = readPropertiesForObject(anObject);
        Iterator it = result.keySet().iterator();
        while (it.hasNext()) {
            key = it.next();
            result.put(key, deepClone(result.get(key)));
        }
        return result;
    }

    /**
    * Applies the map of properties and values to the
    * specified object.  Null values for properties must
    * be represented by the NULL object.
    */
    public static void writePropertiesForObject(Map aMap, Object anObject) {
        String key;
        Object value;
        Iterator it = aMap.keySet().iterator();
        while (it.hasNext()) {
            key = it.next().toString();
            value = aMap.get(key);
            if (NULL.equals(value)) value = null;
            Introspector.set(anObject, key, value);
        }
    }

    /**
    * Creates a new copy of the specified object.
    * This implementation tries to call clone(),
    * and failing that, calls newInstance
    * and then calls copy() to transfer the values.
    * @throws WotonomyException if any operation fails.
    */
    public static Object clone(Object aSource) {
        Object result = null;
        if (clone.implementedByObject(aSource)) {
            try {
                result = clone.invoke(aSource);
                return result;
            } catch (Exception exc) {
            }
        }
        Class c = aSource.getClass();
        try {
            result = c.newInstance();
        } catch (Exception exc) {
            throw new WotonomyException(exc);
        }
        return copy(aSource, result);
    }

    /**
    * Creates a deep copy of the specified object.
    * Every object in this objects graph will be
    * duplicated with new instances.
    * @throws WotonomyException if any operation fails.
    */
    public static Object deepClone(Object aSource) {
        try {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
            objectOutput.writeObject(aSource);
            objectOutput.flush();
            objectOutput.close();
            ByteArrayInputStream byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
            ObjectInputStream objectInput = new ObjectInputStream(byteInput);
            return objectInput.readObject();
        } catch (Exception exc) {
            throw new WotonomyException(exc);
        }
    }

    /**
    * Copies values from one object to another.
    * Returns the destination object.
    * @throws WotonomyException if any operation fails.
    */
    public static Object copy(Object aSource, Object aDestination) {
        try {
            writePropertiesForObject(readPropertiesForObject(aSource), aDestination);
        } catch (RuntimeException exc) {
            throw new WotonomyException(exc);
        }
        return aDestination;
    }

    /**
    * Deeply clones the values from one object and applies them
    * to another object.
    * Returns the destination object.
    * @throws WotonomyException if any operation fails.
    */
    public static Object deepCopy(Object aSource, Object aDestination) {
        try {
            writePropertiesForObject(clonePropertiesForObject(aSource), aDestination);
        } catch (RuntimeException exc) {
            throw new WotonomyException(exc);
        }
        return aDestination;
    }
}
