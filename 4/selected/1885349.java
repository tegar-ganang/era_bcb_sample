package de.wieger.domaindriven;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import de.wieger.aspectj.runtime.reflect.ReflectionUtil;

public class DomainObjectBeanInfo extends SimpleBeanInfo {

    private final List<PropertyDescriptor> fPropertyDescriptors = new ArrayList<PropertyDescriptor>();

    /**
	 * @param pReadMethodName
	 *            must not be null
	 * @param pWriteMethodName
	 *            is allowed to be null
	 */
    protected void addPropertyDescriptor(Class<?> pBeanClazz, String pPropertyName, String pReadMethodName, String pWriteMethodName) {
        Method readMethod = getOptionalAccessibleMethod(pBeanClazz, pReadMethodName);
        Method writeMethod = getOptionalAccessibleMethod(pBeanClazz, pWriteMethodName, readMethod.getReturnType());
        try {
            fPropertyDescriptors.add(new PropertyDescriptor(pPropertyName, readMethod, writeMethod));
        } catch (IntrospectionException ex) {
            throw new RuntimeException("fatal", ex);
        }
    }

    private Method getOptionalAccessibleMethod(Class<?> pBeanClazz, String pWriteMethodName, Class<?>... pParameterTypes) {
        if (pWriteMethodName == null) {
            return null;
        }
        return ReflectionUtil.getAccessibleMethod(pBeanClazz, pWriteMethodName, pParameterTypes);
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return fPropertyDescriptors.toArray(new PropertyDescriptor[fPropertyDescriptors.size()]);
    }
}
