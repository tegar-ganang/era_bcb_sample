package net.sf.nanomvc.spring.flow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.nanomvc.core.config.NanoConfigurationException;
import net.sf.nanomvc.core.util.Reflection;
import net.sf.nanomvc.flow.annotations.Context;
import net.sf.nanomvc.flow.runtime.PropertyAccessor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class SpringFlowObjectAccessorFactory {

    public static SpringFlowObjectAccessor createFlowAccessor(final ConfigurableListableBeanFactory beanFactory, final String beanName) {
        final BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        try {
            final Class<?> beanClass = Class.forName(beanDefinition.getBeanClassName());
            beanDefinition.setScope("prototype");
            final PropertyAccessor[] propertyAccessors = createPropertyAccessors(beanDefinition, beanClass);
            return new SpringFlowObjectAccessor(beanClass, propertyAccessors, beanFactory, beanName);
        } catch (ClassNotFoundException e) {
            throw new CannotLoadBeanClassException(beanFactory.toString(), beanName, beanDefinition.getBeanClassName(), e);
        }
    }

    public static PropertyAccessor[] createPropertyAccessors(final BeanDefinition beanDefinition, final Class<?> beanClass) {
        final Set<String> springPropertyNames = new HashSet<String>();
        for (PropertyValue propertyValue : beanDefinition.getPropertyValues().getPropertyValues()) {
            springPropertyNames.add(propertyValue.getName());
        }
        final List<PropertyAccessor> accessors = new ArrayList<PropertyAccessor>();
        for (Field field : Reflection.getFields(beanClass)) {
            if (!Modifier.isStatic(field.getModifiers())) {
                final boolean isSpringProperty = springPropertyNames.contains(field.getName());
                if (field.getAnnotation(Context.class) != null) {
                    if (isSpringProperty) {
                        throw new NanoConfigurationException(field, "spring controlled property cannot be a context variable");
                    }
                    Method readMethod = Reflection.findReadMethod(beanClass, field, true);
                    Method writeMethod = Reflection.findWriteMethod(beanClass, field, true);
                    accessors.add(new PropertyAccessor(readMethod, writeMethod));
                } else {
                    if (!isSpringProperty && !Modifier.isTransient(field.getModifiers())) {
                        throw new NanoConfigurationException(field, "missing @Context annotation");
                    }
                }
            }
        }
        return accessors.toArray(new PropertyAccessor[accessors.size()]);
    }
}
