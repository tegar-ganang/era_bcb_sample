package net.sf.nanomvc.spring.flow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.nanomvc.core.config.NanoConfigurationException;
import net.sf.nanomvc.core.util.Indexer;
import net.sf.nanomvc.core.util.Reflection;
import net.sf.nanomvc.flow.annotations.Context;
import net.sf.nanomvc.flow.config.FlowFactory;
import net.sf.nanomvc.flow.config.StateIndexerFactory;
import net.sf.nanomvc.flow.runtime.Flow;
import net.sf.nanomvc.flow.runtime.FlowRegistry;
import net.sf.nanomvc.flow.runtime.InnerClassStateAccessor;
import net.sf.nanomvc.flow.runtime.PropertyAccessor;
import net.sf.nanomvc.flow.runtime.StateAccessor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

public class SpringFlowFactory {

    public static FlowRegistry createFlowRegistry(final ConfigurableListableBeanFactory beanFactory) {
        final List<Flow> flows = new ArrayList<Flow>();
        final Map<String, SpringFlowObjectAccessor> accessors = createFlowAccessors(beanFactory);
        final Indexer<Class<?>> flowIndexer = createFlowIndexer(accessors.values());
        final FlowFactory flowFactory = new FlowFactory(flowIndexer, new SpringValidatorFactory(beanFactory));
        for (Map.Entry<String, SpringFlowObjectAccessor> entry : accessors.entrySet()) {
            final SpringFlowObjectAccessor flowObjectAccessor = entry.getValue();
            final List<StateAccessor> stateAccessors = createStateAccessors(beanFactory, flowFactory, flowObjectAccessor.getRuntimeClass());
            final Flow flow = flowFactory.createFlow(flowObjectAccessor, stateAccessors);
            flows.add(flow);
        }
        return new FlowRegistry(flows.toArray(new Flow[flows.size()]));
    }

    private static Map<String, SpringFlowObjectAccessor> createFlowAccessors(ConfigurableListableBeanFactory beanFactory) {
        final Map<String, SpringFlowObjectAccessor> accessors = new HashMap<String, SpringFlowObjectAccessor>();
        final Set<Class<?>> flowClasses = new HashSet<Class<?>>();
        for (final String beanName : beanFactory.getBeanDefinitionNames()) {
            String beanClassName = beanFactory.getBeanDefinition(beanName).getBeanClassName();
            Class<?> beanClass;
            try {
                beanClass = Class.forName(beanClassName);
            } catch (ClassNotFoundException e) {
                throw new CannotLoadBeanClassException(beanFactory.toString(), beanName, beanClassName, e);
            }
            if (beanClass.getAnnotation(net.sf.nanomvc.flow.annotations.Flow.class) != null) {
                if (!flowClasses.add(beanClass)) {
                    throw new NanoConfigurationException(beanName, "duplicated flow class");
                }
                accessors.put(beanName, SpringFlowObjectAccessorFactory.createFlowAccessor(beanFactory, beanName));
            }
        }
        return accessors;
    }

    private static Indexer<Class<?>> createFlowIndexer(Collection<SpringFlowObjectAccessor> accessors) {
        Indexer<Class<?>> indexer = new Indexer<Class<?>>();
        for (SpringFlowObjectAccessor accessor : accessors) {
            indexer.add(accessor.getRuntimeClass());
        }
        return indexer;
    }

    private static List<StateAccessor> createStateAccessors(final ConfigurableListableBeanFactory beanFactory, final FlowFactory flowFactory, final Class<?> flowClass) {
        final List<StateAccessor> accessors = new ArrayList<StateAccessor>();
        final Indexer<Class<?>> stateIndexer = StateIndexerFactory.createStateIndexer(flowClass);
        for (Class<?> stateClass : stateIndexer.getObjects()) {
            if (stateClass.getDeclaringClass() != null) {
                final PropertyAccessor[] propertyAccessors = createPropertyAccessors(stateClass);
                final int declaringStateIndex = findDeclaringStateIndex(stateIndexer, stateClass);
                final InnerClassStateAccessor stateAccessor = new InnerClassStateAccessor(propertyAccessors, stateClass, declaringStateIndex);
                accessors.add(stateAccessor);
            } else {
                String[] beanNames = beanFactory.getBeanNamesForType(stateClass);
                if (beanNames.length == 1) {
                    final PropertyAccessor[] propertyAccessors = createPropertyAccessors(beanFactory, beanNames[0], stateClass);
                    StateAccessor stateAccessor = new SpringBeanStateAccessor(propertyAccessors, stateClass, beanFactory, beanNames[0]);
                    accessors.add(stateAccessor);
                } else if (beanNames.length == 0) {
                    throw new NanoConfigurationException(stateClass, "bean not found");
                } else if (beanNames.length > 1) {
                    throw new NanoConfigurationException(stateClass, "only one bean of that class allowed");
                }
            }
        }
        return accessors;
    }

    private static int findDeclaringStateIndex(final Indexer<Class<?>> stateIndexer, final Class<?> stateClass) {
        if (stateClass.getDeclaringClass() != null) {
            return stateIndexer.indexOf(stateClass.getDeclaringClass());
        } else {
            return -1;
        }
    }

    private static PropertyAccessor[] createPropertyAccessors(final ConfigurableListableBeanFactory beanFactory, final String beanName, final Class<?> beanClass) {
        final GenericBeanDefinition beanDefinition = (GenericBeanDefinition) beanFactory.getBeanDefinition(beanName);
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

    private static PropertyAccessor[] createPropertyAccessors(final Class<?> clazz) {
        final List<PropertyAccessor> accessors = new ArrayList<PropertyAccessor>();
        for (Field field : Reflection.getFields(clazz)) {
            if (!Modifier.isStatic(field.getModifiers())) {
                if (field.getAnnotation(Context.class) != null) {
                    Method readMethod = Reflection.findReadMethod(clazz, field, true);
                    Method writeMethod = Reflection.findWriteMethod(clazz, field, true);
                    accessors.add(new PropertyAccessor(readMethod, writeMethod));
                } else {
                    if (!Modifier.isTransient(field.getModifiers())) {
                        throw new NanoConfigurationException(field, "@Context annotation expected");
                    }
                }
            }
        }
        return accessors.toArray(new PropertyAccessor[accessors.size()]);
    }
}
