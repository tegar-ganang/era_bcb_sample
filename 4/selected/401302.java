package org.mc4j.console.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

/**
 * <p>This class implements the ability to get and set properties on a
 * bean.  This included the concept of embedded properties that may
 * be referenced like <code>Bean.property.property.property</code>.</p>
 *
 * @author Greg Hinkle, January 2002
 * @version $Revision: 480 $ ($Author: ghinkl $)
 */
public final class BeanUtil {

    /**
     * String used to separate multiple properties inside of embedded
     * beans.
     */
    private static final String PROPERTY_SEPARATOR = ".";

    /**
     * An empty class array used for null parameter method reflection
     */
    private static Class[] NO_PARAMETERS_ARRAY = new Class[] {};

    /**
     * an empty object array used for null parameter method reflection
     */
    private static Object[] NO_ARGUMENTS_ARRAY = new Object[] {};

    /**
     * The constructor is private so that <tt>new</tt> cannot be used.
     */
    private BeanUtil() {
    }

    /**
     * Retreives a property descriptor object for a given property.
     * <p>
     * Uses the classes in <code>java.beans</code> to get back
     * a descriptor for a property.  Read-only and write-only
     * properties will have a slower return time.
     * </p>
     *
     * @param propertyName The programmatic name of the property
     * @param beanClass The Class object for the target bean.
     *                  For example sun.beans.OurButton.class.
     * @return a PropertyDescriptor for a property that follows the
     *         standard Java naming conventions.
     * @throws PropertyNotFoundException indicates that the property
     *         could not be found on the bean class.
     */
    public static final PropertyDescriptor getPropertyDescriptor(String propertyName, Class beanClass) {
        PropertyDescriptor resultPropertyDescriptor = null;
        char[] pNameArray = propertyName.toCharArray();
        pNameArray[0] = Character.toLowerCase(pNameArray[0]);
        propertyName = new String(pNameArray);
        try {
            resultPropertyDescriptor = new PropertyDescriptor(propertyName, beanClass);
        } catch (IntrospectionException e1) {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                for (int i = 0; i < propertyDescriptors.length; i++) {
                    if (propertyDescriptors[i].getName().equals(propertyName)) {
                        resultPropertyDescriptor = propertyDescriptors[i];
                        break;
                    }
                }
            } catch (IntrospectionException e2) {
                throw new RuntimeException("Encountered " + "exception looking up property [" + propertyName + "] on class [" + beanClass + "]", e2);
            }
        }
        if (resultPropertyDescriptor == null) {
            throw new RuntimeException("Failed to find " + "property descriptor for property [" + propertyName + "] on class [" + beanClass + "]");
        }
        return resultPropertyDescriptor;
    }

    /**
     * <p>Gets the specified attribute from the specified object.  For example,
     * <code>getObjectAttribute(o, "address.line1")</code> will return
     * the result of calling <code>o.getAddress().getLine1()</code>.<p>
     *
     * <p>The attribute specified may contain as many levels as you like. If at
     * any time a null reference is acquired by calling one of the successive
     * getter methods, then the return value from this method is also null.</p>
     *
     * <p>When reading from a boolean property the underlying bean introspector
     * first looks for an is&lt;Property&gt; read method, not finding one it will
     * still look for a get&lt;Property&gt; read method.  Not finding either, the
     * property is considered write-only.</p>
     *
     * @param bean the bean to set the property on
     * @param propertyNames the name of the propertie(s) to retrieve.  If this is
     *        null or the empty string, then <code>bean</code> will be returned.
     * @return the object value of the bean attribute
     *
     * @throws PropertyNotFoundException indicates the the given property
     *         could not be found on the bean
     * @throws NoSuchMethodException Not thrown
     * @throws InvocationTargetException if a specified getter method throws an
     *   exception.
     * @throws IllegalAccessException if a getter method is
     *   not public or property is write-only.
     */
    public static Object getObjectAttribute(Object bean, String propertyNames) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object result = bean;
        StringTokenizer propertyTokenizer = new StringTokenizer(propertyNames, PROPERTY_SEPARATOR);
        while (propertyTokenizer.hasMoreElements() && result != null) {
            Class resultClass = result.getClass();
            String currentPropertyName = propertyTokenizer.nextToken();
            PropertyDescriptor propertyDescriptor = getPropertyDescriptor(currentPropertyName, resultClass);
            Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod == null) {
                throw new IllegalAccessException("User is attempting to " + "read from a property that has no read method.  " + " This is likely a write-only bean property.  Caused " + "by property [" + currentPropertyName + "] on class [" + resultClass + "]");
            }
            result = readMethod.invoke(result, NO_ARGUMENTS_ARRAY);
        }
        return result;
    }

    /**
     * <p>Sets the specified attribute on the specified object.  For example,
     * <code>getObjectAttribute(o, "address.line1", value)</code> will call
     * <code>o.getAddress().setLine1(value)</code>.<p>
     *
     * <p>The attribute specified may contain as many levels as you like. If at
     * any time a null reference is acquired by calling one of the successive
     * getter methods, then a <code>NullPointerException</code> is thrown.</p>
     *
     * @param bean the bean to call the getters on
     * @param propertyNames the name of the attribute(s) to set.  If this is
     *        null or the empty string, then an exception is thrown.
     * @param value the value of the object to set on the bean property
     *
     * @throws PropertyNotFoundException indicates the the given property
     *         could not be found on the bean
     * @throws IllegalArgumentException if the supplied parameter is not of
     *   a valid type
     * @throws NoSuchMethodException never
     * @throws IllegalAccessException if a getter or setter method is
     *   not public or property is read-only.
     * @throws InvocationTargetException if a specified getter method throws an
     *   exception.
     */
    public static void setObjectAttribute(Object bean, String propertyNames, Object value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        Object result = bean;
        String propertyName = propertyNames;
        int indexOfLastPropertySeparator = propertyName.lastIndexOf(PROPERTY_SEPARATOR);
        if (indexOfLastPropertySeparator >= 0) {
            String embeddedProperties = propertyName.substring(0, indexOfLastPropertySeparator);
            propertyName = propertyName.substring(indexOfLastPropertySeparator + PROPERTY_SEPARATOR.length());
            result = getObjectAttribute(result, embeddedProperties);
        }
        Class resultClass = result.getClass();
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(propertyName, resultClass);
        Method writeMethod = propertyDescriptor.getWriteMethod();
        if (writeMethod == null) {
            throw new IllegalAccessException("User is attempting to write " + "to a property that has no write method.  This is likely " + "a read-only bean property.  Caused by property [" + propertyName + "] on class [" + resultClass + "]");
        }
        writeMethod.invoke(result, new Object[] { value });
    }

    /**
     * Gets the value of a static field.
     * @param type the class on which to access the field.
     * @param fieldName the name of the field to access.
     * if the field is not public.
     */
    public static Object getStaticFieldValue(Class type, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = null;
        try {
            field = type.getField(fieldName);
        } catch (NoSuchFieldException nsfe) {
        }
        if (field != null) {
            return field.get(null);
        } else {
            Class[] intfs = type.getInterfaces();
            for (int i = 0; i < intfs.length; i++) {
                Object value = getStaticFieldValue(intfs[i], fieldName);
                if (value != null) return value;
            }
            if (type.getSuperclass() != null) {
                Object value = getStaticFieldValue(type.getSuperclass(), fieldName);
                if (value != null) return value;
            }
        }
        return null;
    }
}
