package net.simpleframework.util;

import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import net.simpleframework.core.Version;
import net.simpleframework.core.id.ID;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class BeanUtils {

    private static BeanInvoke defaultBeanInvoke = new BeanInvoke();

    @SuppressWarnings("rawtypes")
    public static Map toMap(final Object bean, final boolean readAndWrite) {
        return toMap(bean, readAndWrite, defaultBeanInvoke);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map toMap(final Object bean, final boolean readAndWrite, final BeanInvoke invoke) {
        final Map map = invoke.createMap();
        if (bean instanceof Map) {
            map.putAll((Map) bean);
        } else if (bean != null) {
            for (final PropertyDescriptor descriptor : getPropertyDescriptors(bean.getClass())) {
                Method readMethod, writeMethod = null;
                if ((readMethod = getReadMethod(descriptor)) != null && (!readAndWrite || (readAndWrite && (writeMethod = getWriteMethod(descriptor)) != null))) {
                    try {
                        final Object o = invoke.invoke(bean, readMethod, writeMethod);
                        if (o != null) {
                            map.put(descriptor.getName(), o);
                        }
                    } catch (final Exception e) {
                        throw BeansOpeException.wrapException(e);
                    }
                }
            }
        }
        return map;
    }

    public static class BeanInvoke {

        @SuppressWarnings("rawtypes")
        public Map<?, ?> createMap() {
            return new LinkedCaseInsensitiveMap();
        }

        public Object invoke(final Object bean, final Method readMethod, final Method writeMethod) {
            try {
                return readMethod.invoke(bean);
            } catch (final Exception e) {
                throw BeansOpeException.wrapException(e);
            }
        }
    }

    public static void copyProperties(final Object dest, final Object orig) {
        try {
            getPropertyUtils().copyProperties(dest, orig);
        } catch (final Exception e) {
            throw BeansOpeException.wrapException(e);
        }
    }

    private static PropertyUtilsBean getPropertyUtils() {
        return BeanUtilsBean.getInstance().getPropertyUtils();
    }

    @SuppressWarnings({ "rawtypes" })
    public static Object getProperty(final Object bean, final String name) {
        if (bean instanceof Map) {
            return ((Map) bean).get(name);
        } else {
            try {
                return getPropertyUtils().getProperty(bean, name);
            } catch (final Exception e) {
                throw BeansOpeException.wrapException(e);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void setProperty(final Object bean, final String name, Object value) {
        if (bean instanceof Map) {
            ((Map) bean).put(name, value);
        } else {
            final Class clazz = getPropertyType(bean, name);
            if (clazz == null) {
                throw BeansOpeException.wrapException(LocaleI18n.getMessage("BeanUtils.0", bean.getClass().getName(), name));
            }
            if (Enum.class.isAssignableFrom(clazz)) {
                if (!(value instanceof Enum)) {
                    value = ConvertUtils.toEnum((Class<? extends Enum>) clazz, value);
                }
            } else if (ID.class.isAssignableFrom(clazz)) {
                value = ID.Utils.newID(value);
            } else if (Version.class.isAssignableFrom(clazz)) {
                value = Version.getVersion(String.valueOf(value));
            } else if (String.class.isAssignableFrom(clazz) && value != null) {
                value = HTMLUtils.stripScripts(String.valueOf(value));
            }
            try {
                getPropertyUtils().setProperty(bean, name, ConvertUtils.convert(value, clazz));
            } catch (final Exception e) {
                throw BeansOpeException.wrapException(e);
            }
        }
    }

    public static boolean isReadable(final Object bean, final String name) {
        return getPropertyUtils().isReadable(bean, name);
    }

    public static boolean isWriteable(final Object bean, final String name) {
        return getPropertyUtils().isWriteable(bean, name);
    }

    public static PropertyDescriptor[] getPropertyDescriptors(final Class<?> beanClass) {
        return getPropertyUtils().getPropertyDescriptors(beanClass);
    }

    public static Method getReadMethod(final PropertyDescriptor descriptor) {
        return getPropertyUtils().getReadMethod(descriptor);
    }

    public static Method getWriteMethod(final PropertyDescriptor descriptor) {
        return getPropertyUtils().getWriteMethod(descriptor);
    }

    public static Class<?> getPropertyType(final Object bean, final String name) {
        try {
            return getPropertyUtils().getPropertyType(bean, name);
        } catch (final Exception e) {
            final String msg = LocaleI18n.getMessage("BeanUtils.0", bean.getClass().getName(), name);
            throw new BeansOpeException(msg, e);
        }
    }

    public static Class<?> forName(final String className) throws ClassNotFoundException {
        if (className.endsWith("[]")) {
            final String elementClassName = className.substring(0, className.length() - 2);
            return Array.newInstance(forName(elementClassName), 0).getClass();
        } else {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (final ClassNotFoundException ex) {
                final int p = className.lastIndexOf('.');
                if (p > -1) {
                    clazz = Class.forName(className.substring(0, p) + "$" + className.substring(p + 1));
                } else {
                    throw ex;
                }
            }
            return clazz;
        }
    }

    public static Object newInstance(final String className) {
        try {
            return forName(className).newInstance();
        } catch (final Exception e) {
            throw BeansOpeException.wrapException(e);
        }
    }

    public static <T> T newInstance(final Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (final Exception e) {
            throw BeansOpeException.wrapException(e);
        }
    }

    public static Object newInstance(final String className, final Class<?>[] parameterTypes, final Object[] objs) {
        try {
            return forName(className).getConstructor(parameterTypes).newInstance(objs);
        } catch (final Exception e) {
            throw BeansOpeException.wrapException(e);
        }
    }

    public static String getResourceClasspath(final Class<?> clazz, final String filename) {
        if (clazz == null) {
            return null;
        }
        return StringUtils.replace(clazz.getPackage().getName(), ".", "/") + "/" + filename;
    }

    public static InputStream getResourceRecursively(final Class<?> clazz, final String filename) {
        if (!StringUtils.hasText(filename) || clazz == null) {
            return null;
        }
        final ClassLoader cl = clazz.getClassLoader();
        if (cl == null) {
            return null;
        }
        final InputStream inputStream = cl.getResourceAsStream(getResourceClasspath(clazz, filename));
        if (inputStream != null) {
            return inputStream;
        } else {
            return getResourceRecursively(clazz.getSuperclass(), filename);
        }
    }

    public static Class<?>[] getAllInterfaces(final Class<?> clazz) {
        final HashSet<Class<?>> set = new HashSet<Class<?>>();
        Class<?> superClazz = clazz;
        while (superClazz != null) {
            for (final Class<?> interfaceClazz : superClazz.getInterfaces()) {
                set.add(interfaceClazz);
            }
            superClazz = superClazz.getSuperclass();
        }
        return set.toArray(new Class<?>[set.size()]);
    }
}
