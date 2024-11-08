package net.sf.rcpforms.modeladapter.bean;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.sf.rcpforms.common.util.ReflectionUtil;
import net.sf.rcpforms.modeladapter.bean.impl.PropertyChain;
import net.sf.rcpforms.modeladapter.bean.impl.PropertyChain2;
import net.sf.rcpforms.modeladapter.configuration.EnumRangeAdapter;
import net.sf.rcpforms.modeladapter.configuration.IRangeAdapter;
import net.sf.rcpforms.modeladapter.configuration.IntegerRangeAdapter;
import net.sf.rcpforms.modeladapter.configuration.IntegerRangeAdapter.IntRange;
import net.sf.rcpforms.modeladapter.configuration.PropertyAnnotations;
import net.sf.rcpforms.modeladapter.path.IPropertyChain;
import net.sf.rcpforms.modeladapter.path.IPropertyChain2;
import net.sf.rcpforms.modeladapter.path.IPropertyElement;
import net.sf.rcpforms.modeladapter.path.IPropertyPath;
import net.sf.rcpforms.modeladapter.path.PropertyParser;
import net.sf.rcpforms.modeladapter.path.elements.ArrayElement;
import net.sf.rcpforms.modeladapter.provider.ObservableListBeanContentProvider2;
import net.sf.rcpforms.modeladapter.range.RangeAdapterFabRegistry;
import net.sf.rcpforms.modeladapter.util.Validate;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.viewers.IStructuredContentProvider;

/**
 * The Class <code><i>BeanAdapter2</i></code> improves the RCPForms {@link BeanAdapter}
 * and adds support for <b>indexed property access</b> to array properties. Thus, a
 * {@link PropertyChain2 property chain} may contain a part like:
 * <pre>
 *   ... <font color='blue'>"someArray<font color='red'>[5]</font>"</font> ...
 * </pre>
 * This example means that it always accesses the <code>5<sup>th</sup></code> element of
 * the array or list property <code>someArray</code>.
 * <p>
 * 
 * @author Copyright 2007 by The Swiss Post, PostFinance - all rights reserved
 * @author spicherc (10.01.2010)
 * @version 0.78 
 */
public class BeanAdapter2 extends BeanAdapter {

    private static BeanAdapter2 instance = new BeanAdapter2();

    public static BeanAdapter2 getInstance2() {
        return instance;
    }

    protected BeanAdapter2() {
        super();
    }

    @Override
    public IPropertyChain getPropertyChain(final Object beanMeta, final Object... properties) {
        IPropertyPath path = PropertyParser.createPath(properties);
        return new PropertyChain2((Class<?>) beanMeta, path);
    }

    @Override
    public IPropertyChain2 getPropertyChain(Object metaClass, IPropertyPath propertyPath) {
        return new PropertyChain2((Class<?>) metaClass, propertyPath);
    }

    public static PropertyDescriptor getPropertyDescriptor2(final Class<?> beanClazz, final IPropertyElement element) {
        if (element.isSimple()) {
            return getPropertyDescriptor2(beanClazz, element.asString());
        } else {
            throw new Error("BeanAdapter2.getPropertyDescriptor2(): indexed or referenced properties: NYI (not yet implemented)");
        }
    }

    public static PropertyDescriptor getPropertyDescriptor2(final Class<?> beanClazz, final String property) {
        final Indexed propertiesIndex = getPropertiesIndex(property);
        try {
            if (propertiesIndex != null) {
                return new IndexedPropertyDescriptor(propertiesIndex.propertyName, beanClazz);
            }
            final BeanInfo beanInfo = Introspector.getBeanInfo(beanClazz);
            final PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
            for (final PropertyDescriptor propertyDescriptor : descriptors) {
                if (property.equals(propertyDescriptor.getName())) {
                    return propertyDescriptor;
                }
            }
        } catch (final IntrospectionException e1) {
            System.err.println("");
        }
        Method readMethod = null;
        Method writeMethod = null;
        try {
            final String property2 = Character.toUpperCase(property.charAt(0)) + property.substring(1);
            readMethod = beanClazz.getMethod("get" + property2, new Class[0]);
            writeMethod = ReflectionUtil.getMethod(beanClazz, "set" + property2, new Class[] { readMethod.getReturnType() });
        } catch (final Throwable e) {
        }
        try {
            final PropertyDescriptor result = new PropertyDescriptor(property, readMethod, writeMethod);
            return result;
        } catch (final IntrospectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * @see net.sf.rcpforms.modeladapter.configuration.BeanAdapter#getObservableValue(java.lang.Object, net.sf.rcpforms.modeladapter.converter.IPropertyChain)
	 */
    @Override
    public IObservableValue getObservableValue(Object bean, final IPropertyChain propertyChain) {
        final PropertyChain2 chain = (PropertyChain2) propertyChain;
        String property = "";
        final String[] properties = chain.getProperties();
        boolean isSpecial = false;
        for (int i = 0; i < properties.length && !isSpecial; i++) {
            isSpecial |= properties[i].contains("[");
        }
        if (!isSpecial) {
            return super.getObservableValue(bean, propertyChain);
        } else {
            if (properties.length > 1) {
                bean = getNestedProperty(bean, chain);
                property = properties[properties.length - 1];
            } else {
                property = properties[0];
            }
            final Indexed propertiesIndex = getPropertiesIndex(property);
            if (propertiesIndex == null) {
                throw new IllegalArgumentException("propertiesIndex should't be null, property name not ok?  '" + property + "'");
            }
            final IndexedPropertyDescriptor propertyDescriptor2 = (IndexedPropertyDescriptor) getPropertyDescriptor2(bean.getClass(), propertiesIndex.propertyName);
            return new BeanIndexedPropertyObservableValue(Realm.getDefault(), bean, propertyDescriptor2, propertiesIndex.index, false);
        }
    }

    /**
	 * @see net.sf.rcpforms.modeladapter.configuration.BeanAdapter#getObservableDetailValue(org.eclipse.core.databinding.observable.value.IObservableValue, net.sf.rcpforms.modeladapter.converter.IPropertyChain)
	 */
    @Override
    public IObservableValue getObservableDetailValue(final IObservableValue masterBeanObservableValue, final IPropertyChain propertyChain) {
        return super.getObservableDetailValue(masterBeanObservableValue, propertyChain);
    }

    /**
	 * @see net.sf.rcpforms.modeladapter.ModelAdapter#getObservableValue(java.lang.Object, java.lang.Object[])
	 */
    @Override
    public IObservableValue getObservableValue(final Object modelInstance, final Object... properties) {
        return super.getObservableValue(modelInstance, properties);
    }

    /**
	 * @see net.sf.rcpforms.modeladapter.configuration.BeanAdapter#validatePropertyPath(java.lang.Object, java.lang.String, boolean)
	 */
    @Override
    public void validatePropertyPath(final Object metaClass, final String propertyPath, final boolean writable) {
        super.validatePropertyPath(metaClass, propertyPath, writable);
    }

    /**
	 * @see net.sf.rcpforms.modeladapter.configuration.BeanAdapter#proGetRangeAdapter(java.lang.Class, java.lang.String, java.lang.Class)
	 */
    @Override
    protected IRangeAdapter proGetRangeAdapter(final Class<?> modelType, final String propertyName, final Class<?> propertyType) {
        return super.proGetRangeAdapter(modelType, propertyName, propertyType);
    }

    @Override
    public IStructuredContentProvider createDefaultContentProvider() {
        return new ObservableListBeanContentProvider2();
    }

    public IRangeAdapter getRangeAdapter2(final Object beanOrClass, final String properties) {
        final Class<?> beanClass = beanOrClass instanceof Class<?> ? (Class<?>) beanOrClass : beanOrClass.getClass();
        return getRangeAdapter2(beanOrClass, new PropertyChain2(beanClass, split(properties)));
    }

    public IRangeAdapter getRangeAdapter2(final Object beanOrClass, final IPropertyChain propertyChain) {
        final Object modelMeta = propertyChain.getModelMeta();
        Validate.isTrue(beanOrClass == null || (modelMeta instanceof Class<?> && ((Class<?>) modelMeta).isAssignableFrom(beanOrClass instanceof Class<?> ? (Class<?>) beanOrClass : beanOrClass.getClass())), "passed bean is not of type " + modelMeta + " and thus cant be applied to given propertyChain " + propertyChain);
        if (propertyChain instanceof PropertyChain2) {
            final PropertyChain2 propertyChain2 = (PropertyChain2) propertyChain;
            final String[] properties = propertyChain2.getProperties();
            final String propertyName = properties[properties.length - 1];
            final Class<?> type = propertyChain2.getType();
            final IRangeAdapter rangeAdapter = RangeAdapterFabRegistry.getRegistry().createRangeAdapter(beanOrClass, type, propertyName);
            if (rangeAdapter != null) {
                return rangeAdapter;
            }
            if (type == Integer.class || type == Integer.TYPE) {
                final Class<?> inputClass = propertyChain2.getInputClass();
                if (properties.length == 1) {
                    try {
                        final Field[] fields = inputClass.getDeclaredFields();
                        for (final Field field : fields) {
                            if (propertyName.equals(field.getName())) {
                                final IntRange intRange = field.getAnnotation(IntRange.class);
                                if (intRange != null) {
                                    return new IntegerRangeAdapter(intRange.minValue(), intRange.maxValue(), intRange.step());
                                }
                            }
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                        LOG.severe("Exception in BeanAdapter.getRangeAdapter() for IntRange annotation: " + e.getLocalizedMessage());
                    }
                }
            }
        }
        return getRangeAdapter(propertyChain);
    }

    @Override
    public IRangeAdapter getRangeAdapter(final IPropertyChain propertyChain) {
        final PropertyChain propertyChain1 = (PropertyChain) propertyChain;
        final Class<?> type = propertyChain1.getType();
        if (type.isEnum()) {
            return new EnumRangeAdapter((Class<? extends Enum<?>>) type);
        }
        return super.getRangeAdapter(propertyChain);
    }

    protected static Indexed getPropertiesIndex(final String propertyName) {
        final int beginIndex = propertyName.indexOf("[");
        if (beginIndex < 0) {
            return null;
        }
        final int close = propertyName.indexOf("]");
        if (close < 0) {
            throw new IllegalStateException("array bracked not closed in indexed property name: '" + propertyName + "'");
        }
        try {
            final String indexStr = propertyName.substring(beginIndex + 1, close);
            final int index = Integer.parseInt(indexStr);
            return new Indexed(propertyName.substring(0, beginIndex), index);
        } catch (final Throwable e) {
            return null;
        }
    }

    public static PropertyDescriptor getPropertyDescriptor(final Class<?> beanClazz, final String propertyName) throws IntrospectionException {
        final Indexed propertiesIndex = getPropertiesIndex(propertyName);
        if (propertiesIndex != null) {
            return new IndexedPropertyDescriptor(propertyName, beanClazz);
        }
        final BeanInfo beanInfo = Introspector.getBeanInfo(beanClazz);
        final PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
        for (final PropertyDescriptor propertyDescriptor : descriptors) {
            if (propertyName.equals(propertyDescriptor.getName())) {
                return propertyDescriptor;
            }
        }
        return null;
    }

    public static String glueProperties(final String... properties) {
        String glue = "";
        final StringBuilder result = new StringBuilder();
        for (final String property : properties) {
            result.append(glue).append(property);
            glue = ".";
        }
        return result.toString();
    }

    public static boolean hasProperty(final Class<?> type, final String... properties) {
        Class<?> seeker = type;
        for (int i = 0; i < properties.length; i++) {
            final Method getterMethod = ReflectionUtil.getGetterMethod(seeker, properties[i], true);
            if (getterMethod == null) {
                return false;
            }
            seeker = getterMethod.getReturnType();
            if (seeker == null) {
                return false;
            }
        }
        return true;
    }

    public static String[] split2(final String... properties) {
        if (properties.length == 1 && properties[0].contains(".")) {
            return getInstance2().split(properties[0]);
        }
        return properties;
    }

    public static boolean hasProperty(final Object bean, String... properties) {
        if (bean instanceof Class<?>) {
            return hasProperty((Class<?>) bean, properties);
        }
        if (properties.length == 1 && properties[0].contains(".")) {
            properties = split2(properties[0]);
        }
        Class<?> seeker = bean.getClass();
        Object object = bean;
        for (int i = 0; i < properties.length; i++) {
            final Method getterMethod = ReflectionUtil.getGetterMethod(seeker, properties[i], true);
            if (getterMethod == null) {
                return false;
            }
            try {
                object = getterMethod.invoke(object, new Object[0]);
            } catch (final Throwable e) {
                return false;
            }
            if (object == null) {
                return i == properties.length - 1;
            }
            seeker = object.getClass();
            if (seeker == null) {
                return false;
            }
        }
        return true;
    }

    protected static class Indexed {

        public String propertyName;

        public int index;

        @SuppressWarnings("hiding")
        public Indexed(final String propertyName, final int index) {
            this.propertyName = propertyName;
            this.index = index;
        }
    }

    /** Convenience method */
    public static Object getProperty(final Object bean, final String... propertyNames) {
        if (bean == null) {
            return null;
        }
        final Class<?> clazz = bean.getClass();
        final IPropertyChain chain = instance.getPropertyChain(clazz, (Object[]) propertyNames);
        return chain.getValue(bean);
    }

    /** Convenience method */
    public static void setProperty(final Object bean, final Object newValue, final String... propertyNames) {
        final Class<?> clazz = bean.getClass();
        final IPropertyChain chain = instance.getPropertyChain(clazz, (Object[]) propertyNames);
        chain.setValue(bean, newValue);
    }

    /**
     * @see net.sf.rcpforms.modeladapter.ModelAdapter#supportsPropertyType(net.sf.rcpforms.modeladapter.path.IPropertyElement)
     */
    @Override
    public boolean supportsPropertyType(IPropertyElement propertyElement) {
        if (propertyElement instanceof ArrayElement) {
            return true;
        }
        return super.supportsPropertyType(propertyElement);
    }

    /**
     * @see net.sf.rcpforms.modeladapter.ModelAdapter#scanPropertyAnnotations(java.lang.Object, net.sf.rcpforms.modeladapter.path.IPropertyPath)
     */
    @Override
    public PropertyAnnotations scanPropertyAnnotations(Object bean, IPropertyPath propertyPath) {
        final Class<?> metaClass = bean.getClass();
        IPropertyChain2 propertyChain = getPropertyChain(metaClass, propertyPath);
        Object lastBean = propertyChain.getLastBean(bean);
        if (lastBean != null) {
            IPropertyElement[] elements = propertyPath.getPathElements();
            IPropertyElement lastElement = elements[elements.length - 1];
            PropertyDescriptor descriptor = getPropertyDescriptor2(lastBean.getClass(), lastElement);
            if (descriptor != null) {
                Method readMethod = descriptor.getReadMethod();
                Method writeMethod = descriptor.getWriteMethod();
                Field field = null;
                if (readMethod != null) {
                    try {
                        Class<?> modelType = readMethod.getDeclaringClass();
                        String propertyName = lastElement.getIdentifier();
                        Field[] fields = modelType.getFields();
                        field = modelType.getField(propertyName);
                    } catch (Throwable ex) {
                    }
                }
                return new PropertyAnnotations(readMethod.getAnnotations(), writeMethod.getAnnotations(), field != null ? field.getAnnotations() : null);
            }
        }
        return null;
    }
}
