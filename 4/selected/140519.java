package com.tirsen.hanoi.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.beanutils.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import java.util.*;
import java.beans.PropertyDescriptor;

/**
 * Supports both static and dynamic properties.
 *
 * <!-- $Id: MixedDynaClass.java,v 1.1 2002/09/03 07:55:24 tirsen Exp $ -->
 * <!-- $Author: tirsen $ -->
 *
 * @author Jon Tirs&eacute;n (tirsen@users.sourceforge.net)
 * @version $Revision: 1.1 $
 */
public class MixedDynaClass implements MutableDynaClass {

    private List dynamicProperties = new ArrayList();

    private Class beanClass;

    private WrapDynaClass wrapDynaClass;

    private boolean restricted;

    public MixedDynaClass(Class beanClass) {
        this.beanClass = beanClass;
        wrapDynaClass = WrapDynaClass.createDynaClass(beanClass);
    }

    public void add(String name) {
        assertNotRestricted();
        dynamicProperties.add(new DynaProperty(name));
    }

    private void assertNotRestricted() {
        if (isRestricted()) throw new IllegalStateException("Cannot modify restricted class.");
    }

    public void add(String name, Class type) {
        assertNotRestricted();
        dynamicProperties.add(new DynaProperty(name, type));
    }

    public void add(String name, Class type, boolean readable, boolean writeable) {
        assertNotRestricted();
        DynaProperty dynaProperty = new DynaProperty(name, type);
        dynamicProperties.add(dynaProperty);
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public void remove(final String name) {
        assertNotRestricted();
        DynaProperty property = getDynaProperty(name);
        dynamicProperties.remove(property);
    }

    public DynaProperty getDynaProperty(final String name) {
        DynaProperty property = (DynaProperty) CollectionUtils.find(dynamicProperties, new Predicate() {

            public boolean evaluate(Object o) {
                return name.equals(((DynaProperty) o).getName());
            }
        });
        if (property == null) property = wrapDynaClass.getDynaProperty(name);
        return property;
    }

    public String getName() {
        return wrapDynaClass.getName();
    }

    public DynaProperty[] getDynaProperties() {
        List result = new LinkedList(dynamicProperties);
        result.addAll(Arrays.asList(wrapDynaClass.getDynaProperties()));
        return (DynaProperty[]) result.toArray(new DynaProperty[0]);
    }

    public DynaBean newInstance() throws IllegalAccessException, InstantiationException {
        return MixedDynaBean.asDynaBean(wrapDynaClass.newInstance());
    }

    public MixedDynaBean newWrapper(Object instance) {
        assert beanClass.isInstance(instance);
        return MixedDynaBean.asDynaBean(instance);
    }

    public DynaProperty[] getDynamicProperties() {
        DynaProperty[] allProperties = getDynaProperties();
        Predicate predicate = new Predicate() {

            public boolean evaluate(Object o) {
                return isDynamic(((DynaProperty) o).getName());
            }
        };
        Collection result = CollectionUtils.select(Arrays.asList(allProperties), predicate);
        return (DynaProperty[]) result.toArray(new DynaProperty[result.size()]);
    }

    public boolean isStatic(String name) {
        return wrapDynaClass.getDynaProperty(name) != null;
    }

    public boolean isDynamic(String name) {
        return !isStatic(name);
    }

    public static boolean isReadable(PropertyDescriptor property) {
        if (property == null) return false;
        if (property instanceof DynamicPropertyDescriptor) {
            DynamicPropertyDescriptor dynamicPropertyDescriptor = (DynamicPropertyDescriptor) property;
            return dynamicPropertyDescriptor.isReadable();
        } else if (property.getReadMethod() == null) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isWritable(PropertyDescriptor property) {
        if (property == null) return false;
        if (property instanceof DynamicPropertyDescriptor) {
            DynamicPropertyDescriptor dynamicPropertyDescriptor = (DynamicPropertyDescriptor) property;
            return dynamicPropertyDescriptor.isWritable();
        } else if (property.getWriteMethod() == null) {
            return false;
        } else {
            return true;
        }
    }

    private PropertyDescriptor getPropertyDescriptor(String name) {
        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(beanClass);
        for (int i = 0; i < descriptors.length; i++) {
            PropertyDescriptor descriptor = descriptors[i];
            if (name.equals(descriptor.getName())) return descriptor;
        }
        return null;
    }

    public boolean isReadable(String name) {
        return isDynamic(name) || isReadable(getPropertyDescriptor(name));
    }

    public boolean isWritable(String name) {
        return isDynamic(name) || isWritable(getPropertyDescriptor(name));
    }
}
