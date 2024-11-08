package net.sf.beanrunner;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;
import net.sf.beanrunner.factory.InstanceFactory;
import net.sf.beanrunner.factory.impl.BooleanInstanceFactory;
import net.sf.beanrunner.factory.impl.ChainedInstanceFactory;
import net.sf.beanrunner.factory.impl.CharInstanceFactory;
import net.sf.beanrunner.factory.impl.CollectionInstanceFactory;
import net.sf.beanrunner.factory.impl.DateInstanceFactory;
import net.sf.beanrunner.factory.impl.MapInstanceFactory;
import net.sf.beanrunner.factory.impl.NestedBeanInstanceFactory;
import net.sf.beanrunner.factory.impl.NumberInstanceFactory;
import net.sf.beanrunner.factory.impl.SingleValueInstanceFactory;
import net.sf.beanrunner.factory.impl.StringInstanceFactory;

public class BeanRunner {

    private Map factoryMap = new HashMap();

    private Collection excludedProperties = new ArrayList();

    public BeanRunner() {
        byte b[] = new byte[20];
        Arrays.fill(b, Byte.MAX_VALUE);
        BigInteger min = new BigInteger(-1, b);
        BigInteger max = new BigInteger(1, b);
        NumberInstanceFactory longInstanceFactory = new NumberInstanceFactory(Long.class, new Long(Long.MIN_VALUE), new Long(Long.MAX_VALUE));
        NumberInstanceFactory intInstanceFactory = new NumberInstanceFactory(Integer.class, new Integer(Integer.MIN_VALUE), new Integer(Integer.MAX_VALUE));
        factoryMap.put(Double.TYPE, new NumberInstanceFactory(Double.class, new Double(Double.MIN_VALUE), new Double(Double.MAX_VALUE)));
        factoryMap.put(Double.class, new NumberInstanceFactory(Double.class, new Double(Double.MIN_VALUE), new Double(Double.MAX_VALUE)));
        factoryMap.put(Float.TYPE, new NumberInstanceFactory(Float.class, new Float(Float.MIN_VALUE), new Float(Float.MAX_VALUE)));
        factoryMap.put(Float.class, new NumberInstanceFactory(Float.class, new Float(Float.MIN_VALUE), new Float(Float.MAX_VALUE)));
        factoryMap.put(Short.TYPE, new NumberInstanceFactory(Short.class, new Short(Short.MIN_VALUE), new Short(Short.MAX_VALUE)));
        factoryMap.put(Short.class, new NumberInstanceFactory(Short.class, new Short(Short.MIN_VALUE), new Short(Short.MAX_VALUE)));
        factoryMap.put(Byte.TYPE, new NumberInstanceFactory(Byte.class, new Byte(Byte.MIN_VALUE), new Byte(Byte.MAX_VALUE)));
        factoryMap.put(Byte.class, new NumberInstanceFactory(Byte.class, new Byte(Byte.MIN_VALUE), new Byte(Byte.MAX_VALUE)));
        factoryMap.put(Integer.TYPE, intInstanceFactory);
        factoryMap.put(Integer.class, intInstanceFactory);
        factoryMap.put(Long.TYPE, longInstanceFactory);
        factoryMap.put(Long.class, longInstanceFactory);
        factoryMap.put(Boolean.TYPE, new BooleanInstanceFactory());
        factoryMap.put(Boolean.class, new BooleanInstanceFactory());
        factoryMap.put(Character.TYPE, new CharInstanceFactory());
        factoryMap.put(Character.class, new CharInstanceFactory());
        factoryMap.put(String.class, new StringInstanceFactory(16000));
        factoryMap.put(BigInteger.class, new NumberInstanceFactory(java.math.BigInteger.class, min, max));
        factoryMap.put(BigDecimal.class, new NumberInstanceFactory(java.math.BigDecimal.class, new BigDecimal(min), new BigDecimal(max)));
        factoryMap.put(Date.class, new DateInstanceFactory());
        factoryMap.put(Collection.class, new CollectionInstanceFactory(Collections.EMPTY_LIST));
        factoryMap.put(List.class, new CollectionInstanceFactory(Collections.EMPTY_LIST));
        factoryMap.put(Set.class, new CollectionInstanceFactory(Collections.EMPTY_SET));
        factoryMap.put(Map.class, new MapInstanceFactory(Collections.EMPTY_MAP));
    }

    /**
     * Add a new test value for any property of the given type. The types must
     * match exactly. You cannot set a Map.class and expect it to be passed to a
     * property of type HashMap.class.
     * 
     * @param type
     *            the property type
     * @param value
     *            the value to pass
     */
    public void addTestValue(Class type, Object value) {
        InstanceFactory factory = (InstanceFactory) factoryMap.get(type);
        if (factory == null) {
            factoryMap.put(type, new SingleValueInstanceFactory(value));
        } else {
            factoryMap.put(type, new ChainedInstanceFactory(factory, new SingleValueInstanceFactory(value)));
        }
    }

    /**
     * Test the properties that have both getters and setters. Exclude those who
     * have been excluded by {@link #excludeProperty(String)}.
     * 
     * If the object implements Serializable, do a check to ensure it really is.
     * 
     * @param bean
     *            the object to test
     * @throws Exception
     *             on failure
     */
    public void testBean(Object bean) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
        simplePropertiesTest(bean, beanInfo);
        if (bean instanceof Serializable) {
            testSerializable((Serializable) bean);
        }
    }

    public void testException(Class clazz) throws Exception {
        String message = new Date().toString();
        Throwable cause = new Exception(message);
        textExceptionInternal(clazz, null, null);
        textExceptionInternal(clazz, message, null);
        textExceptionInternal(clazz, null, cause);
        textExceptionInternal(clazz, message, cause);
    }

    private void textExceptionInternal(Class clazz, String message, Throwable cause) throws SecurityException, IllegalArgumentException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, ClassNotFoundException {
        List args = new ArrayList(2);
        if (message != null) {
            args.add(message);
        }
        if (cause != null) {
            args.add(cause);
        }
        Throwable ex = getThrowableInstance(clazz, args.toArray());
        if (ex != null) {
            Assert.assertEquals(message, ex.getMessage());
            Assert.assertEquals(cause, ex.getCause());
            testSerializable(ex);
        }
    }

    private Throwable getThrowableInstance(Class clazz, Object[] objects) throws SecurityException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class[] types = toTypes(objects);
        Constructor constructor;
        try {
            constructor = clazz.getConstructor(types);
            Object newInstance = constructor.newInstance(objects);
            return (Throwable) newInstance;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Class[] toTypes(Object[] objects) {
        Class[] rtnval = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            Object obj = objects[i];
            rtnval[i] = obj.getClass();
        }
        return rtnval;
    }

    /**
     * Test that the bean can be serialized and deserialized.
     * 
     * @param bean
     *            the object to test
     * @throws IOException
     *             if the object cannot be serialized or deserialized
     * @throws ClassNotFoundException
     *             if the object cannot be deserialized
     */
    public void testSerializable(Serializable bean) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(bean);
        byte bs[] = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bs);
        ObjectInputStream ois = new ObjectInputStream(bais);
        ois.readObject();
    }

    /**
     * This method will add the name to a list of properties that need to be
     * excluded for the automated testing.
     * 
     * @param propertyName
     *            the name of the property to exclude
     */
    public void excludeProperty(String propertyName) {
        excludedProperties.add(propertyName);
    }

    public void testToString(Object bean) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
        PropertyDescriptor propertyDescriptors[] = beanInfo.getPropertyDescriptors();
        for (int i = 0; i < propertyDescriptors.length; i++) {
            PropertyDescriptor descriptor = propertyDescriptors[i];
            String name = descriptor.getName();
            if (excludedProperties.contains(name)) {
                continue;
            }
            Method writeMethod = descriptor.getWriteMethod();
            if (writeMethod != null) {
                testToStringInternal(bean, descriptor, writeMethod);
            }
        }
    }

    private void testToStringInternal(Object bean, PropertyDescriptor descriptor, Method writeMethod) throws Exception {
        Class propertyType = descriptor.getPropertyType();
        InstanceFactory instances = (InstanceFactory) factoryMap.get(propertyType);
        if (instances == null) {
            instances = new NestedBeanInstanceFactory(propertyType);
        }
        Object obj = null;
        String invoke;
        try {
            for (Iterator it = instances.iterator(); it.hasNext(); ) {
                obj = it.next();
                writeMethod.invoke(bean, new Object[] { obj });
                invoke = bean.toString();
                String expected = (obj == null) ? "null" : obj.toString();
                String message = "Property not in toString[:" + invoke + "] \nExpected{{" + expected + " }} " + descriptor.getDisplayName();
                Assert.assertTrue(message, invoke.indexOf(expected) > -1);
            }
        } catch (Exception ex) {
            throw new BeanRunnerInvocationException(bean, descriptor, obj, ex);
        }
    }

    private void simplePropertiesTest(Object bean, BeanInfo beanInfo) throws Exception {
        PropertyDescriptor propertyDescriptors[] = beanInfo.getPropertyDescriptors();
        for (int i = 0; i < propertyDescriptors.length; i++) {
            PropertyDescriptor descriptor = propertyDescriptors[i];
            String name = descriptor.getName();
            if (excludedProperties.contains(name)) {
                continue;
            }
            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            if (readMethod != null && writeMethod != null) {
                simplePropertyTest(bean, descriptor, readMethod, writeMethod);
            }
        }
    }

    private void simplePropertyTest(Object bean, PropertyDescriptor descriptor, Method readMethod, Method writeMethod) throws Exception {
        Class propertyType = descriptor.getPropertyType();
        InstanceFactory instances = (InstanceFactory) factoryMap.get(propertyType);
        if (instances == null) {
            instances = new NestedBeanInstanceFactory(propertyType);
        }
        Object obj;
        Object invoke;
        for (Iterator it = instances.iterator(); it.hasNext(); Assert.assertEquals("Property:" + descriptor.getDisplayName(), obj, invoke)) {
            obj = it.next();
            try {
                writeMethod.invoke(bean, new Object[] { obj });
                invoke = readMethod.invoke(bean, new Object[0]);
            } catch (Exception ex) {
                throw new BeanRunnerInvocationException(bean, descriptor, obj, ex);
            }
        }
    }
}
