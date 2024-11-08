package com.googlecode.brui.uicomponents;

import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import junit.framework.TestCase;

class Bean {

    private Integer value = 0;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}

class AnotherBean {

    private Integer someInt = 0;

    private String someString = "kuku";

    private Bean someBean = new Bean();

    public Integer getSomeInt() {
        return someInt;
    }

    public void setSomeInt(Integer someInt) {
        this.someInt = someInt;
    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    public Bean getSomeBean() {
        return someBean;
    }

    public void setSomeBean(Bean someBean) {
        this.someBean = someBean;
    }
}

public class InvokeTest extends TestCase {

    private static final Class[] GETTER_ARGS = new Class[0];

    public final void testBean() throws IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
        Bean bean = new Bean();
        PropertyDescriptor[] beanProperties = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
        for (PropertyDescriptor pd : beanProperties) {
            Method writeMethod = pd.getWriteMethod();
            Method readMethod = pd.getReadMethod();
            if (writeMethod != null && readMethod != null) {
                assertTrue(pd.getPropertyType().equals(Integer.class));
                assertEquals(readMethod.getName(), "getValue");
                Class[] setterArgs = { pd.getPropertyType() };
                readMethod.invoke(bean, GETTER_ARGS);
                writeMethod.invoke(bean, new Integer[] { 1 });
            }
        }
    }
}
