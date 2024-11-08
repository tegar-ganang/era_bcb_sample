package org.softnetwork.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author $Author: smanciot $
 *
 * @version $Revision: 91 $
 */
public class BeanTools {

    private Locale locale;

    private String datePattern;

    private String timePattern;

    /** Creates new BeanTools */
    private BeanTools(Locale locale, String datePattern, String timePattern) {
        this.locale = locale;
        this.datePattern = datePattern;
        this.timePattern = timePattern;
    }

    public static BeanTools getInstance() {
        return getInstance(Locale.getDefault());
    }

    public static BeanTools getInstance(Locale locale) {
        if (locale == null) throw new IllegalArgumentException("Locale argument should not be null");
        return new BeanTools(locale, getPattern(locale), getDefaultTimePattern());
    }

    public static String getPattern(Locale locale) {
        return getDatePattern(locale) + " " + getDefaultTimePattern();
    }

    public static String getDatePattern(Locale locale) {
        if (locale.equals(Locale.ENGLISH)) return "MM/dd/yyyy";
        return "dd/MM/yyyy";
    }

    public static String getDefaultTimePattern() {
        return "HH:mm:ss";
    }

    public static BeanTools getInstance(Locale locale, String datePattern, String timePattern) {
        if (locale == null || datePattern == null || timePattern == null) throw new IllegalArgumentException();
        return new BeanTools(locale, datePattern, timePattern);
    }

    public Object setBeanProperty(Object bean, String property, String value) throws IllegalAccessException, InvocationTargetException {
        Object newValue = value;
        Class c = bean.getClass();
        Method m = getWriteMethod(c, property);
        if (m != null) {
            newValue = castValue(m, value);
            Object[] args = new Object[1];
            args[0] = newValue;
            m.invoke(bean, args);
        }
        return bean;
    }

    /**
	 * @since 02/12/2003
	 */
    public Object setBeanProperty(Object bean, String propertyName, Object value) throws IllegalAccessException, InvocationTargetException {
        Class c = bean.getClass();
        Method m = getWriteMethod(c, propertyName);
        if (m != null) {
            Object[] args = new Object[1];
            args[0] = value;
            m.invoke(bean, args);
        }
        return bean;
    }

    public Object setBeanProperty(Object bean, String property, Object value, Class[] parameters) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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
	 * @since 02/12/2003
	 */
    public PropertyDescriptor[] getBeanProperties(Class bean) {
        List properties = new ArrayList();
        PropertyDescriptor[] props = getPropertyDescriptors(bean);
        if (props != null) for (int i = 0; i < props.length; i++) {
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
        return (PropertyDescriptor[]) properties.toArray(new PropertyDescriptor[0]);
    }

    public PropertyDescriptor[] getBeanPrimitiveProperties(Class bean) {
        List properties = new ArrayList();
        PropertyDescriptor[] props = getPropertyDescriptors(bean);
        if (props != null) for (int i = 0; i < props.length; i++) {
            PropertyDescriptor prop = props[i];
            if (isAPrimitive(prop.getPropertyType())) {
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
	 * @since 02/12/2003
	 */
    public PropertyDescriptor getBeanProperty(Class bean, String propertyName) {
        PropertyDescriptor prop = getPropertyDescriptor(bean, propertyName);
        if (prop != null) {
            Method write = prop.getWriteMethod();
            Method read = prop.getReadMethod();
            if (write != null && read != null) {
                Class[] parameterTypes = write.getParameterTypes();
                Class returnType = read.getReturnType();
                if (parameterTypes.length == 1 && parameterTypes[0].equals(returnType)) {
                    return prop;
                }
            }
        }
        return null;
    }

    public Object getBeanProperty(Object bean, String property) {
        Class c = bean.getClass();
        Method m = getReadMethod(c, property);
        if (m != null) {
            try {
                return m.invoke(bean, null);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InvocationTargetException ex) {
            }
        }
        return null;
    }

    public Method getWriteMethod(Class bean, String property) {
        Method write = null;
        PropertyDescriptor propDesc = getPropertyDescriptor(bean, property);
        if (propDesc != null) {
            write = propDesc.getWriteMethod();
        }
        return write;
    }

    public Method getWriteMethod(Class bean, String property, Class[] parameters) {
        Method write = getWriteMethod(bean, property);
        if (write != null) {
            Class[] types = write.getParameterTypes();
            if (types.length == parameters.length) {
                for (int i = 0; i < parameters.length; i++) {
                    String parameterClassName = parameters[i].getName();
                    String typeClassName = types[i].getName();
                    if (!parameterClassName.equals(typeClassName)) {
                        return null;
                    }
                }
            }
            return write;
        }
        return null;
    }

    public Method getReadMethod(Class bean, String property) {
        Method read = null;
        PropertyDescriptor propDesc = getPropertyDescriptor(bean, property);
        if (propDesc != null) {
            read = propDesc.getReadMethod();
        }
        return read;
    }

    public PropertyDescriptor getPropertyDescriptor(Class bean, String property) {
        try {
            BeanInfo info = Introspector.getBeanInfo(bean);
            PropertyDescriptor[] os = info.getPropertyDescriptors();
            PropertyDescriptor propDesc = null;
            boolean found = false;
            int j = 0;
            while (j < os.length && !found) {
                PropertyDescriptor p = os[j];
                String propName = p.getName();
                if (propName.equalsIgnoreCase(property)) {
                    found = true;
                    propDesc = p;
                }
                j++;
            }
            return propDesc;
        } catch (IntrospectionException e) {
            return null;
        }
    }

    /**
	 * @since 02/12/2003
	 */
    public PropertyDescriptor[] getPropertyDescriptors(Class bean) {
        try {
            BeanInfo info = Introspector.getBeanInfo(bean);
            return info.getPropertyDescriptors();
        } catch (IntrospectionException e) {
            return null;
        }
    }

    /**
	 * @since 02/12/2003
	 */
    public MethodDescriptor[] getMethodDescriptors(Class bean) {
        try {
            BeanInfo info = Introspector.getBeanInfo(bean);
            return info.getMethodDescriptors();
        } catch (IntrospectionException e) {
            return null;
        }
    }

    /**
	 * @since 02/12/2003
	 */
    public Constructor[] getConstructors(Class bean) {
        return bean.getConstructors();
    }

    public Method[] getMethods(Class bean) {
        return bean.getMethods();
    }

    /**
	 * @since 02/12/2003
	 */
    public Constructor getConstructor(Class bean, Class[] parameterTypes) throws NoSuchMethodException {
        return bean.getConstructor(parameterTypes);
    }

    public Object castValue(Method m, String value) {
        Class[] parameters = m.getParameterTypes();
        return castProperty(parameters, value);
    }

    public Object castProperty(Class[] parameters, String value) {
        if (parameters.length == 1) {
            return castProperty(parameters[0], value);
        }
        throw new IllegalArgumentException();
    }

    public Object castProperty(Class c, String value) {
        if (isInstanceOf(c, String.class)) return value; else if (isInstanceOf(c, char[].class)) return toChars(value); else if (isInstanceOf(c, byte[].class)) return toBytes(value); else if (isInstanceOf(c, boolean[].class)) return toBooleans(value); else if (isInstanceOf(c, Boolean.class) || isInstanceOf(c, boolean.class)) return toBoolean(value); else if (isInstanceOf(c, Float.class) || isInstanceOf(c, float.class)) return toFloat(value); else if (isInstanceOf(c, Double.class) || isInstanceOf(c, double.class)) return toDouble(value); else if (isInstanceOf(c, Long.class) || isInstanceOf(c, long.class)) return toLong(value); else if (isInstanceOf(c, Short.class) || isInstanceOf(c, short.class)) return toShort(value); else if (isInstanceOf(c, Integer.class) || isInstanceOf(c, int.class)) return toInteger(value); else if (isInstanceOf(c, java.sql.Time.class)) return toTime(value); else if (isInstanceOf(c, java.util.Date.class)) return toDate(value); else return null;
    }

    public boolean isInstanceOf(Class bean, Class target) {
        boolean ret = false;
        if (bean.equals(target)) ret = true;
        if (!ret) {
            Class[] interfaces = bean.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i].equals(target)) ret = true;
            }
        }
        if (!ret && bean.getSuperclass() != null) ret = isInstanceOf(bean.getSuperclass(), target);
        return ret;
    }

    /**
	 * @since 02/12/2003
	 */
    public Object newInstance(Class bean, Class[] parameterTypes, Object[] initArgs) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return bean.getConstructor(parameterTypes).newInstance(initArgs);
    }

    public Boolean toBoolean(String value) {
        value = value.trim();
        if (value.equals("1") || value.equals("true")) return Boolean.TRUE; else if (value.equals("0") || value.equals("false")) return Boolean.FALSE;
        return new Boolean(value);
    }

    public boolean[] toBooleans(String value) {
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

    public char[] toChars(String value) {
        return value.toCharArray();
    }

    public byte[] toBytes(String value) {
        return value.getBytes();
    }

    public Integer toInteger(String value) {
        return new Integer(value);
    }

    public Double toDouble(String value) {
        return new Double(value);
    }

    public Float toFloat(String value) {
        return new Float(value);
    }

    public Short toShort(String value) {
        return new Short(value);
    }

    public Long toLong(String value) {
        return new Long(value);
    }

    public Date toDate(String value) {
        if (value != null && value.length() > 0) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(datePattern.substring(0, value.length()), locale);
                return sdf.parse(value);
            } catch (ParseException e) {
            }
        }
        return null;
    }

    public Time toTime(String value) {
        if (value != null && value.length() > 0) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(timePattern.substring(0, value.length()), locale);
                return new Time(sdf.parse(value).getTime());
            } catch (ParseException e) {
            }
        }
        return null;
    }

    /**
	 * @since 02/12/2003
	 */
    public String toString(Object bean) {
        PropertyDescriptor[] properties = getPropertyDescriptors(bean.getClass());
        String str = "[" + System.getProperty("line.separator");
        for (int i = 0; i < properties.length; i++) {
            PropertyDescriptor property = properties[i];
            String propertyName = property.getName();
            str += "\t" + propertyName + "=";
            try {
                Object obj = getBeanProperty(bean, propertyName);
                if (obj instanceof char[]) str += new String((char[]) obj) + ", "; else str += obj.toString() + ", ";
            } catch (NullPointerException ex) {
                str += "null, ";
            } finally {
                str += System.getProperty("line.separator");
            }
        }
        return str.substring(0, str.length() - 4) + System.getProperty("line.separator") + "\t]";
    }

    public boolean isAPrimitive(Class c) {
        if (isInstanceOf(c, String.class)) return true; else if (isInstanceOf(c, boolean.class)) return true; else if (isInstanceOf(c, float.class)) return true; else if (isInstanceOf(c, double.class)) return true; else if (isInstanceOf(c, long.class)) return true; else if (isInstanceOf(c, short.class)) return true; else if (isInstanceOf(c, int.class)) return true; else return false;
    }
}
