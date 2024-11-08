package net.sf.rcpforms.experimenting.model.bean;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.sf.rcpforms.experimenting.binding2.BasicModelAdapterFactory;
import net.sf.rcpforms.experimenting.binding2.IModelAdapter2;
import net.sf.rcpforms.experimenting.binding2.IModelAdapterFactory;
import net.sf.rcpforms.experimenting.binding2.IPropertyChain2;
import net.sf.rcpforms.experimenting.binding2.observable.ChainedObservableValue;
import net.sf.rcpforms.experimenting.java.base.ReflectionUtil;
import net.sf.rcpforms.experimenting.rcp.BACK.BeanAdapter;
import net.sf.rcpforms.experimenting.rcp.BACK.ObservableListBeanContentProvider2;
import net.sf.rcpforms.experimenting.rcp.extension.interfaces.RangeAdapterFactory;
import net.sf.rcpforms.modeladapter.configuration.EnumRangeAdapter;
import net.sf.rcpforms.modeladapter.configuration.IRangeAdapter;
import net.sf.rcpforms.modeladapter.configuration.IntegerRangeAdapter;
import net.sf.rcpforms.modeladapter.configuration.IntegerRangeAdapter.IntRange;
import net.sf.rcpforms.modeladapter.configuration.ModelAdapter;
import net.sf.rcpforms.modeladapter.converter.IPropertyChain;
import net.sf.rcpforms.modeladapter.util.Validate;
import org.apache.log4j.Logger;
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
 */
public class BeanAdapter2 extends BeanAdapter implements IModelAdapter2, IModelAdapterFactory {

    @SuppressWarnings("all")
    static final Logger log = Logger.getLogger(BeanAdapter2.class);

    private static BeanAdapter2 instance = new BeanAdapter2();

    public static BeanAdapter2 getInstance2() {
        return instance;
    }

    static {
        ModelAdapter.registerAdapter(instance);
        BasicModelAdapterFactory.s_globalModelAdapterFab = instance;
    }

    private BeanAdapter2() {
        super();
    }

    @Override
    public IPropertyChain2 getPropertyChain(final Object beanMeta, final Object... properties) {
        if (properties.length == 1 && properties[0] instanceof String) {
            final String property1 = (String) properties[0];
            if (property1.contains(".")) {
                return new PropertyChain2(beanMeta, split(property1));
            }
        }
        return new PropertyChain2(beanMeta, properties);
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

    public static class PropertyChain2 extends PropertyChain implements IPropertyChain2 {

        protected transient String[] properties2Model = null;

        public PropertyChain2(final Class<?> beanMeta, final Object[] properties) {
            super(beanMeta, properties);
        }

        protected PropertyChain2(final Object beanMeta, final Object[] properties) {
            this((Class<?>) beanMeta, properties);
        }

        @Override
        public PropertyDescriptor getUnnestedPropertyDescriptor(final Class<?> beanClazz, final String property) throws IntrospectionException {
            return getPropertyDescriptor2(beanClazz, property);
        }

        /** {@inheritDoc} */
        @Override
        public Object getValue(final Object model) {
            return doGetValue(model, properties);
        }

        /** {@inheritDoc} */
        @Override
        public Object getBean(final Object model) {
            if (properties2Model == null) {
                properties2Model = new String[properties.length - 1];
                System.arraycopy(properties, 0, properties2Model, 0, properties2Model.length);
            }
            return doGetValue(model, properties2Model);
        }

        /** {@inheritDoc} */
        @Override
        public String[] getProperties() {
            final String[] copy = new String[properties.length];
            System.arraycopy(properties, 0, copy, 0, properties.length);
            return copy;
        }

        /** {@inheritDoc} */
        @Override
        public Class<?> getInputClass() {
            return (Class<?>) getModelMeta();
        }

        protected Object doGetValue(final Object model, final String[] propertiesOverride) {
            Validate.isTrue(ModelAdapter.getAdapterForInstance(model).getMetaClass(model) == beanMeta, "Model Object has not the same metaclass which was passed for property descriptor construction");
            PropertyDescriptor descriptor = null;
            String path = beanMeta.getName();
            Object result = model;
            try {
                String property = null;
                Class<?> modelObjectClass = beanMeta;
                for (final Object prop : propertiesOverride) {
                    if (result == null) {
                        return null;
                    }
                    property = (String) prop;
                    path += "." + property;
                    descriptor = getUnnestedPropertyDescriptor(modelObjectClass, property);
                    modelObjectClass = getPropertyType(descriptor);
                    try {
                        final Method method = getReadMethod(descriptor);
                        final Indexed indexed = getPropertiesIndex(property);
                        if (indexed != null) {
                            result = method.invoke(result, new Object[] { indexed.index });
                        } else {
                            result = method.invoke(result, new Object[] {});
                        }
                    } catch (final Throwable ex) {
                        ex.printStackTrace();
                        final String message = "Error in Provider: " + getClass().getName() + " accessing property " + property + ": " + ex.getMessage();
                        log.fatal(message);
                        throw new IllegalArgumentException(message);
                    }
                    modelObjectClass = result == null ? null : result.getClass();
                }
            } catch (final Exception ex) {
                throw new IllegalArgumentException("BeanAdapter: Exception getting property '" + path + "'");
            }
            return result;
        }

        /**
	     * sets the value of the property in the given model
	     * 
	     * @param model
	     */
        @Override
        public void setValue(final Object model, final Object value) {
            Validate.isTrue(ModelAdapter.getAdapterForInstance(model).getMetaClass(model) == beanMeta, "Model Object has not the same metaclass which was passed for property descriptor construction");
            PropertyDescriptor descriptor = null;
            String path = beanMeta.getName();
            Object result = model;
            Object firstChild = null;
            boolean wasSet = false;
            try {
                String property = null;
                Class<?> modelObjectClass = beanMeta;
                for (final Object prop : properties) {
                    property = (String) prop;
                    path += "." + property;
                    descriptor = getUnnestedPropertyDescriptor(modelObjectClass, property);
                    modelObjectClass = getPropertyType(descriptor);
                    if (prop == properties[properties.length - 1]) {
                        try {
                            final Method method = getWriteMethod(descriptor);
                            if (!method.isAccessible()) {
                                method.setAccessible(true);
                            }
                            final Indexed indexed = getPropertiesIndex(property);
                            if (indexed != null) {
                                method.invoke(result, new Object[] { indexed.index, value });
                            } else {
                                method.invoke(result, new Object[] { value });
                            }
                            wasSet = true;
                            if (firstChild == null) {
                                firstChild = result;
                            }
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                            final String message = "Exception in BeanAdapter: " + getClass().getName() + " setting property " + property + ": " + ex.getMessage();
                            log.fatal(message);
                            throw new IllegalArgumentException(message);
                        }
                    } else {
                        try {
                            final Method method = getReadMethod(descriptor);
                            final Indexed indexed = getPropertiesIndex(property);
                            if (indexed != null) {
                                result = method.invoke(result, new Object[] { indexed.index });
                            } else {
                                result = method.invoke(result, new Object[] {});
                            }
                            modelObjectClass = result == null ? null : result.getClass();
                            if (firstChild == null) {
                                firstChild = result;
                            }
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                            final String message = "Exception in BeanAdapter: " + getClass().getName() + " accessing property " + property + ": " + ex.getMessage();
                            log.fatal(message);
                            throw new IllegalArgumentException(message);
                        }
                    }
                }
            } catch (final Exception ex) {
                throw new IllegalArgumentException("BeanAdapter: Exception setting property '" + path + "'");
            }
            if (!wasSet) {
                throw new IllegalArgumentException("BeanAdapter: Exception setting property " + path);
            } else {
                if (firstChild != null && model instanceof JavaBean) {
                    final StringBuilder pathBuilder = new StringBuilder();
                    String glue = "";
                    for (final String part : properties) {
                        pathBuilder.append(glue).append(part);
                        glue = ".";
                    }
                    ((JavaBean) model).getPropertyChangeSupport().firePropertyChange(pathBuilder.toString(), null, firstChild);
                }
            }
        }

        protected Method getReadMethod(final PropertyDescriptor descriptor) {
            if (descriptor instanceof IndexedPropertyDescriptor) {
                final IndexedPropertyDescriptor indexed = (IndexedPropertyDescriptor) descriptor;
                return indexed.getIndexedReadMethod();
            }
            return descriptor.getReadMethod();
        }

        protected Method getWriteMethod(final PropertyDescriptor descriptor) {
            if (descriptor instanceof IndexedPropertyDescriptor) {
                final IndexedPropertyDescriptor indexed = (IndexedPropertyDescriptor) descriptor;
                return indexed.getIndexedWriteMethod();
            }
            return descriptor.getWriteMethod();
        }

        @Override
        public Class<?> getPropertyType(final PropertyDescriptor descriptor) {
            final Class<?> result = descriptor.getPropertyType();
            if (descriptor instanceof IndexedPropertyDescriptor) {
                if (result.isArray()) {
                    return result.getComponentType();
                }
            }
            return result;
        }

        @Override
        public int getPropertyCount() {
            return properties != null ? properties.length : 0;
        }
    }

    @Override
    public IStructuredContentProvider createDefaultContentProvider() {
        return new ObservableListBeanContentProvider2();
    }

    @Override
    public IRangeAdapter getRangeAdapter2(final Object beanOrClass, final String properties) {
        final Class<?> beanClass = beanOrClass instanceof Class<?> ? (Class<?>) beanOrClass : beanOrClass.getClass();
        return getRangeAdapter2(beanOrClass, new PropertyChain2(beanClass, split(properties)));
    }

    @Override
    public IRangeAdapter getRangeAdapter2(final Object beanOrClass, final IPropertyChain propertyChain) {
        final Object modelMeta = propertyChain.getModelMeta();
        Validate.isTrue(beanOrClass == null || (modelMeta instanceof Class<?> && ((Class<?>) modelMeta).isAssignableFrom(beanOrClass instanceof Class<?> ? (Class<?>) beanOrClass : beanOrClass.getClass())), "passed bean is not of type " + modelMeta + " and thus cant be applied to given propertyChain " + propertyChain);
        if (propertyChain instanceof PropertyChain2) {
            final PropertyChain2 propertyChain2 = (PropertyChain2) propertyChain;
            final String[] properties = propertyChain2.getProperties();
            final String propertyName = properties[properties.length - 1];
            final Class<?> type = propertyChain2.getType();
            final IRangeAdapter rangeAdapter = RangeAdapterFactory.getInstance().createRangeAdapter(beanOrClass, type, propertyName);
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
                        log.fatal("Exception in BeanAdapter.getRangeAdapter() for IntRange annotation: " + e.getLocalizedMessage());
                    }
                }
            }
        }
        return getRangeAdapter(propertyChain);
    }

    @SuppressWarnings("unchecked")
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

    public IObservableValue getObservableValue_Chained(final Object bean, final String... properties) {
        final IPropertyChain2 chain = getPropertyChain(bean.getClass(), (Object[]) properties);
        return getObservableValue_Chained(bean, chain);
    }

    public IObservableValue getObservableValue_Chained(final Object bean, final IPropertyChain2 propertyChain) {
        final ChainedObservableValue result = new ChainedObservableValue(bean, propertyChain);
        return result;
    }

    public IObservableValue getObservableValue_Chained(final Realm realm, final Object bean, final IPropertyChain2 propertyChain) {
        final ChainedObservableValue result = new ChainedObservableValue(realm, bean, propertyChain);
        return result;
    }

    @Override
    public IObservableValue getObservableValue(final Object bean, final IPropertyChain propertyChain) {
        if (propertyChain instanceof IPropertyChain2) {
            final IPropertyChain2 chain2 = (IPropertyChain2) propertyChain;
            if (chain2.getPropertyCount() > 1) {
                return getObservableValue_Chained(bean, chain2);
            }
        }
        return super.getObservableValue(bean, propertyChain);
    }

    @Override
    public IObservableValue getObservableValue(final Realm realm, final Object bean, final IPropertyChain propertyChain) {
        if (propertyChain instanceof IPropertyChain2) {
            final IPropertyChain2 chain2 = (IPropertyChain2) propertyChain;
            if (chain2.getPropertyCount() > 1) {
                return getObservableValue_Chained(realm, bean, chain2);
            }
        }
        return super.getObservableValue(realm, bean, propertyChain);
    }

    @Override
    public ModelAdapter getAdapterForModel(final Object model) {
        return this;
    }

    @Override
    public ModelAdapter getAdapterForType(final Class<?> modelType) {
        return this;
    }
}
