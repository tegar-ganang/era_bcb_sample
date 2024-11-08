package org.base.apps.beans.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.base.apps.beans.BaseBean;
import org.base.apps.util.exception.BaseRuntimeException;

/**
 * Utility class for bean-related functionality.
 * 
 * @author Kevan Simpson
 */
public class BeanUtil {

    private static Map<Class<?>, BeanInfo> mBeanInfoCache = new HashMap<Class<?>, BeanInfo>();

    /** Private void constructor. */
    private BeanUtil() {
    }

    /**
     * Returns the {@link BeanInfo} for the given class.
     * 
     * @param beanClass The given class.
     * @return The class' <code>BeanInfo</code>.
     * @throws IllegalArgumentException if the given class is <code>null</code>.
     */
    public static BeanInfo getBeanInfo(Class<?> beanClass) {
        if (beanClass == null) {
            throw new IllegalArgumentException("No bean class specified");
        }
        BeanInfo beanInfo = mBeanInfoCache.get(beanClass);
        if (beanInfo == null) {
            try {
                if (ClassUtils.isAssignable(beanClass, BaseBean.class)) {
                    beanInfo = Introspector.getBeanInfo(beanClass, BaseBean.class);
                } else {
                    beanInfo = Introspector.getBeanInfo(beanClass);
                }
                if (beanInfo != null) {
                    mBeanInfoCache.put(beanClass, beanInfo);
                }
            } catch (IntrospectionException e) {
                return null;
            }
        }
        return beanInfo;
    }

    /**
     * Returns the {@link BeanInfo} for the given bean.
     * 
     * @param bean The given bean.
     * @return The bean's <code>BeanInfo</code>.
     * @throws IllegalArgumentException if the given bean is <code>null</code>.
     */
    public static BeanInfo getBeanInfo(Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException("No bean specified");
        }
        return getBeanInfo(bean.getClass());
    }

    /**
     * Returns the property descriptors for the given class.
     * 
     * @param beanClass The given class.
     * @return The class' property descriptors.
     * @throws IllegalArgumentException if the given class is <code>null</code>.
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Class<?> beanClass) {
        if (beanClass == null) {
            throw new IllegalArgumentException("No bean class specified");
        }
        BeanInfo info = getBeanInfo(beanClass);
        return (info == null) ? new PropertyDescriptor[0] : info.getPropertyDescriptors();
    }

    /**
     * Returns the property descriptors for the given bean.
     * 
     * @param bean The given bean.
     * @return The bean's property descriptors.
     * @throws IllegalArgumentException if the given bean is <code>null</code>.
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException("No bean specified");
        }
        return getPropertyDescriptors(bean.getClass());
    }

    /**
     * Extracts all {@link PropertyDescriptor}s from the specified class and
     * returns them mapped by {@link PropertyDescriptor#getName() name}.
     * Any class from the JDK will be ignored and return an empty map. More 
     * specifically, if the given class resides in the &quot;java&quot; package 
     * hierarchy, an empty map will be returned.
     * 
     * @param clz The class from which to extract descriptors.
     * @return A non-<code>null</code> map of descriptors, keyed by property name.
     */
    public static Map<String, PropertyDescriptor> loadDescriptors(Class<?> clz) {
        Map<String, PropertyDescriptor> propDescMap = new LinkedHashMap<String, PropertyDescriptor>();
        if (clz != null && !StringUtils.startsWith(clz.getPackage().getName(), "java")) {
            PropertyDescriptor[] descs = getPropertyDescriptors(clz);
            if (descs != null) {
                for (PropertyDescriptor pd : descs) {
                    propDescMap.put(pd.getName(), pd);
                }
            }
        }
        return propDescMap;
    }

    /**
     * Loads bound properties, those with getters and setters, from the given
     * bean. Any deprecated getter/setters and the &quot;class&quot; property
     * will be excluded.
     * 
     * @param bean The given bean from which to load descriptors.
     * @param excludeReadOnly If <code>true</code>, properties without setters will be excluded.
     * @return a map of bound property descriptors.
     */
    public static Map<String, PropertyDescriptor> loadBoundDescriptors(Object bean, boolean excludeReadOnly) {
        return loadBoundDescriptors((bean == null) ? null : bean.getClass(), excludeReadOnly);
    }

    /**
     * Loads bound properties, those with getters and setters, from the given
     * bean. Any deprecated getter/setters and the &quot;class&quot; property
     * will be excluded.
     * 
     * @param beanType The given bean type from which to load descriptors.
     * @param excludeReadOnly If <code>true</code>, properties without setters will be excluded.
     * @return a map of bound property descriptors.
     */
    public static Map<String, PropertyDescriptor> loadBoundDescriptors(Class<?> beanType, boolean excludeReadOnly) {
        Map<String, PropertyDescriptor> some = new LinkedHashMap<String, PropertyDescriptor>();
        if (beanType != null) {
            Map<String, PropertyDescriptor> all = loadDescriptors(beanType);
            for (String name : all.keySet()) {
                PropertyDescriptor desc = all.get(name);
                if (desc != null) {
                    Method r = desc.getReadMethod(), w = desc.getWriteMethod();
                    if (r != null && !isDeprecated(r, w)) {
                        if ((excludeReadOnly && w == null) || StringUtils.equals("class", name)) continue;
                        Class<? extends Object> type = r.getReturnType();
                        if (ClassUtils.isAssignable(type, String.class)) {
                            some.put(name, desc);
                        } else {
                            Kind kind = Kind.toKind(type);
                            if (kind != null && kind != Kind.string) {
                                some.put(name, desc);
                            } else {
                                System.out.println("Ignoring bound property: " + name);
                                System.out.println(kind);
                            }
                        }
                    }
                }
            }
        }
        return some;
    }

    public static Method[] findUpdaters(Class<?> clz, String propName) {
        Method[] meth = new Method[3];
        if (clz != null) {
            try {
                BeanInfo info = getBeanInfo(clz);
                MethodDescriptor[] meths = info.getMethodDescriptors();
                for (MethodDescriptor m : meths) {
                    switch(ArrayUtils.getLength(m.getMethod().getParameterTypes())) {
                        case 1:
                            {
                                String name = m.getName();
                                if (StringUtils.startsWith(name, "add")) {
                                    if (StringUtils.startsWith(propName, WordUtils.uncapitalize(name.substring(3)))) {
                                        meth[0] = m.getMethod();
                                        if (meth[1] != null && meth[2] != null) break;
                                    }
                                } else if (StringUtils.startsWith(name, "remove")) {
                                    if (StringUtils.startsWith(propName, WordUtils.uncapitalize(name.substring(6)))) {
                                        meth[1] = m.getMethod();
                                        if (meth[0] != null && meth[2] != null) break;
                                    }
                                }
                                break;
                            }
                        case 2:
                            {
                                String name = m.getName();
                                if (StringUtils.startsWith(name, "set")) {
                                    if (StringUtils.getLevenshteinDistance(propName, WordUtils.uncapitalize(name.substring(3)), 3) >= 0) {
                                        if (ClassUtils.isAssignable(m.getMethod().getParameterTypes()[0], Number.class)) {
                                            meth[2] = m.getMethod();
                                            if (meth[0] != null && meth[1] != null) break;
                                        }
                                    }
                                }
                                break;
                            }
                    }
                }
            } catch (Exception e) {
                throw (new BaseRuntimeException("Failed to find updater")).addContextValue("type", clz).addContextValue("property", propName).addContextValue("cause", e.getMessage());
            }
        }
        return meth;
    }

    /**
     * Returns <code>true</code> if either the given accessor or mutator has 
     * been annoted with {@link Deprecated}.
     * 
     * @param read An accessor method of a property.
     * @param write A mutator method of a property.
     * @return <code>true</code> if either the given accessor or mutator has 
     *          been annoted with {@link Deprecated}.
     */
    public static boolean isDeprecated(Method read, Method write) {
        return ((read != null && read.getAnnotation(Deprecated.class) != null) || (write != null && write.getAnnotation(Deprecated.class) != null));
    }
}
