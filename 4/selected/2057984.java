package org.softnetwork.core;

import java.beans.BeanInfo;
import java.beans.Beans;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author stephane.manciot@ebiznext.com
 * @version 1.0
 */
public final class BeanTools {

    /**
     * LINE SEPARATOR Constant.
     */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static ThreadLocal locale = new ThreadLocal() {

        protected synchronized Object initialValue() {
            return Locale.getDefault();
        }
    };

    /**
     * private Constructor.
     */
    private BeanTools() {
    }

    /**
     * Returns PropertyDescriptors in read-write mode associated to this class.
     *
     * @param bean
     *            the Class
     * @return an array of PropertyDescriptor
     */
    public static PropertyDescriptor[] getReadWriteProperties(final Class bean) {
        List properties = new ArrayList();
        PropertyDescriptor[] props = getPropertyDescriptors(bean);
        if (props != null) {
            for (int i = 0; i < props.length; i++) {
                PropertyDescriptor prop = props[i];
                Method write = prop.getWriteMethod();
                Method read = prop.getReadMethod();
                if (write != null && read != null) {
                    Class[] parameterTypes = write.getParameterTypes();
                    Class returnType = read.getReturnType();
                    if (parameterTypes.length == 1 && parameterTypes[0].equals(returnType)) {
                        properties.add(prop);
                    }
                }
            }
        }
        return (PropertyDescriptor[]) properties.toArray(new PropertyDescriptor[0]);
    }

    /**
     * Returns all PropertyDescriptors associated to this class.
     *
     * @param bean
     *            the Class
     * @return an array of PropertyDescriptor
     */
    public static PropertyDescriptor[] getPropertyDescriptors(final Class bean) {
        PropertyDescriptor[] descriptors = null;
        try {
            BeanInfo info = getBeanInfo(bean);
            descriptors = info.getPropertyDescriptors();
        } catch (IntrospectionException e) {
        }
        return descriptors;
    }

    public static void setLocale(Locale l) {
        if (l != null) locale.set(l);
    }

    /**
     * @param bean
     *            Class
     * @param target
     *            Class
     * @return boolean
     */
    public static boolean isInstanceOf(final Class bean, final Class target) {
        boolean ret = false;
        if (target.isAssignableFrom(bean)) {
            ret = true;
        }
        if (!ret) {
            Class[] interfaces = bean.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i].equals(target)) {
                    ret = true;
                }
            }
        }
        if (!ret && bean.getSuperclass() != null) {
            ret = isInstanceOf(bean.getSuperclass(), target);
        }
        return ret;
    }

    /**
     * @param c
     *            Class
     * @param value
     *            String
     * @return Object
     */
    public static Object castProperty(final Class c, final String value) {
        Object ret = null;
        if (value != null) {
            if (c == null) {
                throw new IllegalArgumentException();
            } else if (isInstanceOf(c, String.class)) {
                ret = value;
            } else if (isInstanceOf(c, char[].class)) {
                ret = value.toCharArray();
            } else if (isInstanceOf(c, byte[].class)) {
                ret = value.getBytes();
            } else if (isInstanceOf(c, boolean[].class)) {
                ret = toBooleans(value);
            } else if (isInstanceOf(c, Boolean.class) || isInstanceOf(c, boolean.class)) {
                ret = toBoolean(value);
            } else if (isInstanceOf(c, Float.class) || isInstanceOf(c, float.class)) {
                ret = new Float(value);
            } else if (isInstanceOf(c, Double.class) || isInstanceOf(c, double.class)) {
                ret = new Double(value);
            } else if (isInstanceOf(c, Long.class) || isInstanceOf(c, long.class)) {
                ret = new Long(value);
            } else if (isInstanceOf(c, Short.class) || isInstanceOf(c, short.class)) {
                ret = new Short(value);
            } else if (isInstanceOf(c, Integer.class) || isInstanceOf(c, int.class)) {
                ret = new Integer(value);
            } else if (isInstanceOf(c, java.sql.Time.class)) {
                ret = toTime(value);
            } else if (isInstanceOf(c, java.util.Date.class)) {
                ret = toDate(value);
            }
        }
        return ret;
    }

    /**
     * @param value
     *            String
     * @return Boolean
     */
    public static Boolean toBoolean(String value) {
        Boolean ret = Boolean.FALSE;
        if (value != null) {
            value = value.trim();
            if (value.equals("1") || value.toLowerCase().equals("true")) {
                ret = Boolean.TRUE;
            } else if (value.equals("0") || value.equals("false")) {
                ret = Boolean.FALSE;
            } else ret = Boolean.valueOf(value);
        }
        return ret;
    }

    /**
     * @param value
     *            String
     * @return boolean[]
     */
    public static boolean[] toBooleans(final String value) {
        boolean[] bs = new boolean[value.length()];
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '1') {
                bs[i] = true;
            } else {
                bs[i] = false;
            }
        }
        return bs;
    }

    /**
     * @param value
     *            String
     * @return Date
     */
    public static Date toDate(final String value) {
        Date ret = null;
        if (value != null && value.length() > 0) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(getDatePattern().substring(0, value.length()));
                ret = sdf.parse(value);
            } catch (ParseException e) {
            }
        }
        return ret;
    }

    /**
     * @param value
     *            String
     * @return Time
     */
    public static Time toTime(final String value) {
        Time ret = null;
        if (value != null && value.length() > 0) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(getDefaultTimePattern().substring(0, value.length()));
                ret = new Time(sdf.parse(value).getTime());
            } catch (ParseException e) {
            }
        }
        return ret;
    }

    /**
     * @return String
     */
    public static String getDatePattern() {
        String ret = "dd/MM/yyyy";
        if (locale.get().equals(Locale.ENGLISH)) {
            ret = "MM/dd/yyyy";
        }
        return ret;
    }

    /**
     * @return String
     */
    public static String getDefaultTimePattern() {
        return "HH:mm:ss";
    }

    /**
     * @param bean
     *            Object
     * @param indentLevel
     *            int
     * @return String
     */
    public static String printProperties(final Object bean, final int indentLevel) {
        PropertyDescriptor[] properties = null;
        String str = "";
        if (BeanInfo.class.isAssignableFrom(bean.getClass())) {
            properties = ((BeanInfo) bean).getPropertyDescriptors();
        } else {
            try {
                properties = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
            } catch (IntrospectionException e) {
            }
        }
        if (properties != null) {
            final int len = properties.length;
            for (int i = 0; i < len; i++) {
                PropertyDescriptor property = properties[i];
                String propertyName = property.getName();
                int nb = indentLevel;
                do {
                    str += "\t";
                    nb--;
                } while (nb > 0);
                str += ((i == 0) ? "[" : "") + propertyName + " = ";
                try {
                    Method m = property.getReadMethod();
                    Object obj = m.invoke(bean, new Object[] {});
                    if (obj == bean) {
                        str += "this";
                    } else if (obj instanceof char[]) {
                        str += new String((char[]) obj);
                    } else if (obj instanceof byte[]) {
                        str += new String((byte[]) obj);
                    } else if (obj instanceof Time) {
                        str += new SimpleDateFormat(getDefaultTimePattern()).format((Time) obj);
                    } else if (obj instanceof Date) {
                        str += new SimpleDateFormat(getDatePattern()).format((Date) obj);
                    } else {
                        str += printProperties(obj, indentLevel + 1);
                    }
                } catch (NullPointerException ex) {
                    str += "null";
                } catch (IllegalArgumentException e) {
                    str += "null";
                } catch (IllegalAccessException e) {
                    str += "null";
                } catch (InvocationTargetException e) {
                    str += "null";
                } finally {
                    str += (i < (len - 1) && len > 1) ? ", " + LINE_SEPARATOR : "";
                }
            }
            int nb = indentLevel;
            do {
                str += "\t";
                nb--;
            } while (nb > 0);
            str += "]";
        }
        return str;
    }

    /**
     * @param bean
     *            Class
     * @param property
     *            String
     * @return Method
     */
    public static Method getReadMethod(final Class bean, final String property) {
        Method read = null;
        PropertyDescriptor propDesc = getPropertyDescriptor(bean, property);
        if (propDesc != null) {
            read = propDesc.getReadMethod();
        }
        return read;
    }

    /**
     * @param bean
     *            Class
     * @param property
     *            String
     * @return Method
     */
    public static Method getWriteMethod(final Class bean, final String property) {
        Method write = null;
        PropertyDescriptor propDesc = getPropertyDescriptor(bean, property);
        if (propDesc != null) {
            write = propDesc.getWriteMethod();
        }
        return write;
    }

    /**
     * @param bean
     *            Class
     * @param property
     *            String
     * @param parameters
     *            Class[]
     * @return Method
     */
    public static Method getWriteMethod(final Class bean, final String property, final Class[] parameters) {
        Method write = getWriteMethod(bean, property);
        if (write != null) {
            Class[] types = write.getParameterTypes();
            if (types.length == parameters.length) {
                for (int i = 0; i < parameters.length; i++) {
                    String parameterClassName = parameters[i].getName();
                    String typeClassName = types[i].getName();
                    if (!parameterClassName.equals(typeClassName)) {
                        write = null;
                        break;
                    }
                }
            }
        }
        return write;
    }

    /**
     * @param bean
     *            Class
     * @param property
     *            String
     * @return PropertyDescriptor
     */
    public static PropertyDescriptor getPropertyDescriptor(final Class bean, final String property) {
        PropertyDescriptor propDesc = null;
        try {
            BeanInfo info = getBeanInfo(bean);
            PropertyDescriptor[] os = info.getPropertyDescriptors();
            int j = 0;
            while (j < os.length) {
                PropertyDescriptor p = os[j];
                if (p.getName().equalsIgnoreCase(property)) {
                    propDesc = p;
                    break;
                }
                j++;
            }
        } catch (IntrospectionException e) {
        }
        return propDesc;
    }

    /**
     * @param bean
     *            Class
     * @return BeanInfo
     * @throws IntrospectionException
     */
    public static BeanInfo getBeanInfo(final Class bean) throws IntrospectionException {
        BeanInfo info = null;
        if (BeanInfo.class.isAssignableFrom(bean)) try {
            info = ((BeanInfo) bean.newInstance());
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } else info = Introspector.getBeanInfo(bean);
        return info;
    }

    /**
     * @param bean
     *            Object
     * @param propertyName
     *            String
     * @param value
     *            Object
     * @return Object
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object setBeanProperty(final Object bean, final String propertyName, final Object value) throws IllegalAccessException, InvocationTargetException {
        Object newValue = value;
        Class c = bean.getClass();
        Method m = getWriteMethod(c, propertyName);
        if (m != null) {
            Object[] args = new Object[1];
            if (String.class.equals(value.getClass())) {
                Class[] parameters = m.getParameterTypes();
                newValue = castProperty(parameters[0], (String) value);
            }
            args[0] = newValue;
            m.invoke(bean, args);
        }
        return bean;
    }

    /**
     * @param bean
     *            Object
     * @param property
     *            String
     * @param value
     *            Object
     * @param parameters
     *            Class[]
     * @return Object
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object setBeanProperty(final Object bean, final String property, final Object value, final Class[] parameters) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class c = bean.getClass();
        Method m = getWriteMethod(c, property, parameters);
        if (m != null) {
            Object[] args = new Object[1];
            args[0] = value;
            m.invoke(bean, args);
        } else throw new NoSuchMethodException(property + " " + parameters.toString());
        return bean;
    }

    /**
     * @param target
     *            Object
     * @param source
     *            Object
     * @param forbidden
     *            Set
     * @param props
     *            String[]
     * @param checkInheritance
     *            boolean
     * @return Object
     * @throws IntrospectionException
     */
    public static Object copy(final Object target, final Object source, Set forbidden, final String[] props, final boolean checkInheritance, final boolean copyIfNull) throws IntrospectionException {
        if (forbidden == null) forbidden = new TreeSet();
        if (!forbidden.contains("class")) forbidden.add("class");
        if (!checkInheritance || Beans.isInstanceOf(target, source.getClass())) {
            BeanInfo childInfo = Introspector.getBeanInfo(target.getClass());
            PropertyDescriptor[] childProps = childInfo.getPropertyDescriptors();
            String from = source.getClass().getName();
            from = from.substring(from.lastIndexOf(".") + 1);
            String to = target.getClass().getName();
            to = to.substring(to.lastIndexOf(".") + 1);
            Set set = getPropertiesSet(childProps, props);
            Iterator it = set.iterator();
            while (it.hasNext()) {
                String property = (String) it.next();
                if (!forbidden.contains(property)) {
                    Method read = getReadMethod(source.getClass(), property);
                    if (read != null) {
                        Method write = getWriteMethod(target.getClass(), property, read.getParameterTypes());
                        if (write != null) {
                            Object value = null;
                            try {
                                value = read.invoke(source, null);
                                if (!copyIfNull && value == null) continue;
                                Object[] args = new Object[1];
                                args[0] = value;
                                write.invoke(target, args);
                            } catch (IllegalAccessException ex) {
                            } catch (IllegalArgumentException ex) {
                            } catch (InvocationTargetException ex) {
                            }
                        }
                    }
                }
            }
        }
        return target;
    }

    /**
     * @param props
     *            PropertyDescriptor[]
     * @param properties
     *            String[]
     * @return Set
     * @throws IntrospectionException
     */
    private static Set getPropertiesSet(final PropertyDescriptor[] props, final String[] properties) throws IntrospectionException {
        Set set = null;
        Collection coll = new ArrayList();
        for (int i = 0; i < props.length; i++) {
            PropertyDescriptor prop = props[i];
            if (prop.getWriteMethod() != null) coll.add(prop.getName());
        }
        if (properties != null) {
            set = new HashSet();
            for (int i = 0; i < properties.length; i++) {
                String property = properties[i];
                if (coll.contains(property)) {
                    set.add(property);
                }
            }
        } else set = new HashSet(coll);
        return set;
    }
}
