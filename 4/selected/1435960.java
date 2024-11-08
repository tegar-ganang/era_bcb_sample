package com.abra.j2xb.beans.model;

import com.abra.j2xb.annotations.*;
import com.abra.j2xb.beans.exceptions.*;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.Annotation;

/**
 * Defines the metadata for a mapped Bean.
 *
 * The class supports a special case for mapping exceptions. The special case adds the message property of the exception
 * and optionally adds the exception trace.
 * @author Yoav Abrahami
 * @version 1.0, May 1, 2008
 * @since   JDK1.5
 */
public class MOBeanDescriptor {

    private Class<?> beanClass;

    private MOBeansFactory beansFactory;

    private MOConstructorFacade constructorFacade;

    private List<MOPropertyDescriptor> properties = new ArrayList<MOPropertyDescriptor>();

    private List<MODelegateDescriptor> delegates = new ArrayList<MODelegateDescriptor>();

    private List<MOChoicePropertyDescriptor> choiceProperties = new ArrayList<MOChoicePropertyDescriptor>();

    private MOAbstractPropertyDescriptor parentProperty = null;

    private MOPersistentBeanDescriptor superclassBeanDescriptor = null;

    public MOBeanDescriptor(MOBeansFactory factory, Class<?> clazz, MOAbstractPropertyDescriptor parentProperty) throws MOBeansException {
        this(factory, clazz);
        this.parentProperty = parentProperty;
    }

    public MOBeanDescriptor(MOBeansFactory factory, Class<?> clazz) throws MOBeansException {
        beanClass = clazz;
        this.beansFactory = factory;
        connectSuperclassBeanDescriptor();
        introspectBean();
    }

    /**
	 * inspect the bean superclasses to find the first mapped superclasses
	 * @throws MOBeansException when a superclass of the bean is mapped and has invalid annotations
	 */
    private void connectSuperclassBeanDescriptor() throws MOBeansException {
        Class clazz = beanClass.getSuperclass();
        while (clazz != null) {
            if (isPersistentBean(clazz) || isThrowable(clazz)) {
                superclassBeanDescriptor = beansFactory.getOrAddBeansDescriptor(clazz);
                return;
            }
            clazz = clazz.getSuperclass();
        }
    }

    public boolean isPersistentBean() {
        return beanClass.getAnnotation(MOPersistentBean.class) != null;
    }

    public boolean isPersistentDependentBean() {
        return beanClass.getAnnotation(MOPersistentDependentBean.class) != null;
    }

    public static boolean isPersistentBean(Class<?> clazz) {
        return clazz.getAnnotation(MOPersistentBean.class) != null;
    }

    public static boolean isThrowable(Class<?> clazz) {
        return Throwable.class.isAssignableFrom(clazz);
    }

    public static boolean isNonMappedThrowable(Class<?> clazz) {
        return isThrowable(clazz) && !isPersistentBean(clazz) && !isPersistentDependentBean(clazz);
    }

    public static boolean isPersistentDependentBean(Class<?> clazz) {
        return clazz.getAnnotation(MOPersistentDependentBean.class) != null;
    }

    /**
	 * introspect the bean to create the property mappings
	 * @throws MOBeansException if the annotations on the bean are invalid.
	 */
    protected void introspectBean() throws MOBeansException {
        if (!(isPersistentBean(beanClass) || isPersistentDependentBean(beanClass) || isThrowable(beanClass))) throw new MOBeansException(String.format("class %s is not a persistent bean or a dependent persistent bean", beanClass));
        if (isPersistentBean(beanClass) && isPersistentDependentBean(beanClass)) throw new MOBeansAnnotationCombinationNotAllowed(MOPersistentBean.class, MOPersistentDependentBean.class, beanClass);
        try {
            BeanInfoEx beanInfo = beansFactory.getBeanInfoCache().getBeanInfo(beanClass);
            if (isPersistentBean(beanClass) || isPersistentDependentBean(beanClass)) {
                List<MOAnnotationBasedPropertyDescriptor> tmpProperties = new ArrayList<MOAnnotationBasedPropertyDescriptor>();
                for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                    boolean isManagedProperty = isManagedProperty(propertyDescriptor);
                    boolean isManagedDelegate = isManagedDelegate(propertyDescriptor);
                    boolean isChoiceDiscriminator = isChoiceDiscriminator(propertyDescriptor);
                    if (superclassBeanDescriptor != null && (isManagedProperty || isManagedDelegate || isChoiceDiscriminator)) {
                        Class<?> propertyDefiningClass = getPropertyDefiningClass(propertyDescriptor);
                        if (propertyDefiningClass.isAssignableFrom(superclassBeanDescriptor.getBeanClass())) {
                            continue;
                        } else {
                            if (superclassBeanDescriptor.getPropertyByName(propertyDescriptor.getName()) != null) {
                                throw new MOBeanRemappingException(beanClass, superclassBeanDescriptor.getBeanClass(), propertyDescriptor.getName());
                            }
                        }
                    }
                    if (isManagedProperty) {
                        tmpProperties.add(new MOAnnotationBasedPropertyDescriptor(propertyDescriptor, this));
                    }
                    if (isManagedDelegate) {
                        delegates.add(new MODelegateDescriptor(propertyDescriptor, this));
                    }
                    if (isChoiceDiscriminator) {
                        choiceProperties.add(new MOChoicePropertyDescriptor(propertyDescriptor, this));
                    }
                }
                for (MOAnnotationBasedPropertyDescriptor property : tmpProperties) {
                    if (property.isChoiceProperty()) {
                        boolean foundChoice = false;
                        for (MOChoicePropertyDescriptor choiceProperty : choiceProperties) {
                            if (choiceProperty.isIncludedInChoice(property)) {
                                choiceProperty.addProperty(property);
                                foundChoice = true;
                            }
                        }
                        if (!foundChoice) throw new MOBeanChoiceException(property.getName(), property.getChoiceDiscriminator());
                    } else {
                        properties.add(property);
                    }
                }
            }
            if (beanClass == Throwable.class) {
                PropertyDescriptor propertyDescriptor = beanInfo.getPropertyDescriptor("message");
                properties.add(new MOExceptionMessagePropertyDescriptor(propertyDescriptor, this));
                if (beansFactory.isMapExceptionTrace()) properties.add(new MOExceptionTracePropertyDescriptor(propertyDescriptor, this));
            }
        } catch (IntrospectionException e) {
            throw new MOBeansException("failed to introspect bean [%s]", e, beanClass.getName());
        }
        constructorFacade = new MOConstructorFacade(this, beanClass);
    }

    void validateDefinitions() throws MOBeansException {
        try {
            for (MOPropertyDescriptor propertyDescriptor : properties) propertyDescriptor.validateProperty();
            for (MODelegateDescriptor delegateDescriptor : delegates) delegateDescriptor.validateDelegate();
            for (MOChoicePropertyDescriptor choicePropertyDescriptor : choiceProperties) choicePropertyDescriptor.validateChoice();
            constructorFacade.validateDefinitions();
        } catch (IntrospectionException e) {
            throw new MOBeansException("failed to introspect bean [%s]", e, beanClass.getName());
        }
    }

    /**
	 * this method finds the lowest class in the hierarchy tree that defines this property.
	 * @param propertyDescriptor the property to query
	 * @return the class defining one of the property getter/setter methods, which is the lowest in the inheritence tree.
	 */
    private Class<?> getPropertyDefiningClass(PropertyDescriptor propertyDescriptor) {
        if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
            Class<?> readMethodClass = propertyDescriptor.getReadMethod().getDeclaringClass();
            Class<?> writeMethodClass = propertyDescriptor.getWriteMethod().getDeclaringClass();
            if (readMethodClass.isAssignableFrom(writeMethodClass)) return writeMethodClass; else return readMethodClass;
        } else if (propertyDescriptor.getReadMethod() != null) return propertyDescriptor.getReadMethod().getDeclaringClass(); else return propertyDescriptor.getWriteMethod().getDeclaringClass();
    }

    private boolean isChoiceDiscriminator(PropertyDescriptor propertyDescriptor) {
        return isBeanPropertyAnnotated(propertyDescriptor, MOChoiceDiscriminator.class);
    }

    private boolean isManagedDelegate(PropertyDescriptor propertyDescriptor) {
        return isBeanPropertyAnnotated(propertyDescriptor, MODelegateProperty.class);
    }

    private boolean isManagedProperty(PropertyDescriptor propertyDescriptor) {
        return isBeanPropertyAnnotated(propertyDescriptor, MOProperty.class);
    }

    private boolean isBeanPropertyAnnotated(PropertyDescriptor propertyDescriptor, Class<? extends Annotation> annotation) {
        return isMethodAnnotated(propertyDescriptor.getReadMethod(), annotation) || isMethodAnnotated(propertyDescriptor.getWriteMethod(), annotation);
    }

    private boolean isMethodAnnotated(Method method, Class<? extends Annotation> annotation) {
        return method != null && method.getAnnotation(annotation) != null;
    }

    public MOConstructorFacade getConstructorFacade() {
        return constructorFacade;
    }

    /**
	 * finds a property by name. This method finds only direct properties, not delegate properties
	 * @param propertyName the Java name of the property to search for
	 * @return the property descriptor, or null the property was not found.
	 */
    public MOPropertyDescriptor getPropertyByName(String propertyName) {
        for (MOPropertyDescriptor propertyDescriptor : properties) {
            if (propertyDescriptor.getName().equals(propertyName)) return propertyDescriptor;
        }
        return null;
    }

    public List<MOPropertyDescriptor> getProperties() {
        return properties;
    }

    public List<MODelegateDescriptor> getDelegates() {
        return delegates;
    }

    public List<MOChoicePropertyDescriptor> getChoiceProperties() {
        return choiceProperties;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public String getXmlName() {
        return null;
    }

    public MOAbstractPropertyDescriptor getParentProperty() {
        return parentProperty;
    }

    public MOPropertyDescriptor getMOPropertyDescriptor(String propertyPath) throws MOBeansPropertyNotFoundException {
        String[] path = propertyPath.split("\\.");
        return getMOPropertyDescriptor(path, propertyPath, 0);
    }

    protected MOPropertyDescriptor getMOPropertyDescriptor(String[] path, String propertyPath, int positionInPath) throws MOBeansPropertyNotFoundException {
        if (path.length > positionInPath + 1) {
            for (MODelegateDescriptor delegate : delegates) {
                if (delegate.getName().equals(path[positionInPath])) return delegate.getDelegateBeanDescriptor().getMOPropertyDescriptor(path, propertyPath, positionInPath + 1);
            }
        } else {
            for (MOPropertyDescriptor propertyDescriptor : properties) {
                if (propertyDescriptor.getName().equals(path[positionInPath])) return propertyDescriptor;
            }
        }
        if (superclassBeanDescriptor != null) return superclassBeanDescriptor.getMOPropertyDescriptor(path, propertyPath, positionInPath); else throw new MOBeansPropertyNotFoundException(beanClass, propertyPath);
    }

    /**
	 * this method supports getting property type from delegates also
	 * @param propertyPath a dot separated path to the property to be set (e.g. address.name)
	 * @return the property type
	 * @throws com.abra.j2xb.beans.exceptions.MOBeansException
	 */
    public Class<?> getType(String propertyPath) throws MOBeansException {
        return getJavaPropertyDescriptor(propertyPath).getPropertyType();
    }

    /**
	 * this method supports getting values from delegates also
	 * @param propertyPath a dot separated path to the property to be set
	 * @param instance the instance to set the value for
	 * @return the property value
	 * @throws com.abra.j2xb.beans.exceptions.MOBeansException
	 */
    public Object getValue(Object instance, String propertyPath) throws MOBeansException {
        try {
            String[] path = propertyPath.split("\\.");
            Object tempInstance = instance;
            for (String propertyName : path) {
                BeanInfoEx beanInfoEx = beansFactory.getBeanInfoCache().getBeanInfo(tempInstance.getClass());
                PropertyDescriptor propertyDescriptor = beanInfoEx.getPropertyDescriptor(propertyName);
                if (propertyDescriptor.getReadMethod() == null) {
                    throw new MOBeansPropertyAccessException(beanClass, propertyPath, "getter");
                }
                tempInstance = propertyDescriptor.getReadMethod().invoke(tempInstance);
            }
            return tempInstance;
        } catch (IllegalAccessException e) {
            throw new MOBeansPropertyAccessException(beanClass, propertyPath, e);
        } catch (InvocationTargetException e) {
            throw new MOBeansPropertyAccessException(beanClass, propertyPath, e);
        } catch (IntrospectionException e) {
            throw new MOBeansPropertyAccessException(beanClass, propertyPath, e);
        }
    }

    /**
	 * this method supports getting values from delegates also
	 * @param propertyPath - a path to the property to get the value for
	 * @param value the value to set
	 */
    public void setValue(Object instance, Object value, String propertyPath) throws MOBeansException {
        try {
            String[] path = propertyPath.split("\\.");
            Object tempInstance = instance;
            for (int i = 0; i < path.length - 1; i++) {
                String propertyName = path[i];
                BeanInfoEx beanInfoEx = beansFactory.getBeanInfoCache().getBeanInfo(tempInstance.getClass());
                PropertyDescriptor propertyDescriptor = beanInfoEx.getPropertyDescriptor(propertyName);
                tempInstance = propertyDescriptor.getReadMethod().invoke(tempInstance);
            }
            String propertyName = path[path.length - 1];
            BeanInfoEx beanInfoEx = beansFactory.getBeanInfoCache().getBeanInfo(tempInstance.getClass());
            PropertyDescriptor propertyDescriptor = beanInfoEx.getPropertyDescriptor(propertyName);
            if (propertyDescriptor.getWriteMethod() == null) {
                throw new MOBeansPropertyAccessException(beanClass, propertyPath, "setter");
            }
            propertyDescriptor.getWriteMethod().invoke(tempInstance, value);
        } catch (IllegalAccessException e) {
            throw new MOBeansPropertyAccessException(beanClass, propertyPath, e);
        } catch (InvocationTargetException e) {
            throw new MOBeansPropertyAccessException(beanClass, propertyPath, e);
        } catch (IntrospectionException e) {
            throw new MOBeansPropertyAccessException(beanClass, propertyPath, e);
        }
    }

    /**
	 * finds a Java property in the bean class
	 * @param propertyPath - a path to the property to search for
	 * @return the Property Descriptor, if one is found, or else null
	 * @throws MOBeansPropertyAccessException
	 */
    public PropertyDescriptor getJavaPropertyDescriptor(String propertyPath) throws MOBeansPropertyAccessException {
        try {
            String[] path = propertyPath.split("\\.");
            String propertyName = path[path.length - 1];
            BeanInfoEx beanInfoEx = beansFactory.getBeanInfoCache().getBeanInfo(getBeanClass());
            return beanInfoEx.getPropertyDescriptor(propertyName);
        } catch (IntrospectionException e) {
            throw new MOBeansPropertyAccessException(beanClass, propertyPath, e);
        }
    }

    public MOBeansFactory getBeansFactory() {
        return beansFactory;
    }

    public String toString() {
        return String.format("beanDescriptor[%s]", getBeanClass().getName());
    }

    public MOPersistentBeanDescriptor getSuperclassBeanDescriptor() {
        return superclassBeanDescriptor;
    }
}
