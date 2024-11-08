package com.intellij.uiDesigner.lw;

import com.intellij.uiDesigner.compiler.Utils;
import javax.swing.*;
import java.awt.*;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class CompiledClassPropertiesProvider implements PropertiesProvider {

    private final ClassLoader myLoader;

    private final HashMap myCache;

    public CompiledClassPropertiesProvider(final ClassLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("loader cannot be null");
        }
        myLoader = loader;
        myCache = new HashMap();
    }

    public HashMap getLwProperties(final String className) {
        if (myCache.containsKey(className)) {
            return (HashMap) myCache.get(className);
        }
        if (Utils.validateJComponentClass(myLoader, className, false) != null) {
            return null;
        }
        final Class aClass;
        try {
            aClass = Class.forName(className, false, myLoader);
        } catch (final ClassNotFoundException exc) {
            throw new RuntimeException(exc.toString());
        }
        final BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(aClass);
        } catch (Throwable e) {
            return null;
        }
        final HashMap result = new HashMap();
        final PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
        for (int i = 0; i < descriptors.length; i++) {
            final PropertyDescriptor descriptor = descriptors[i];
            final Method readMethod = descriptor.getReadMethod();
            final Method writeMethod = descriptor.getWriteMethod();
            if (writeMethod == null || readMethod == null) {
                continue;
            }
            final String name = descriptor.getName();
            final LwIntrospectedProperty property = propertyFromClass(descriptor.getPropertyType(), name);
            if (property != null) {
                property.setDeclaringClassName(descriptor.getReadMethod().getDeclaringClass().getName());
                result.put(name, property);
            }
        }
        myCache.put(className, result);
        return result;
    }

    public static LwIntrospectedProperty propertyFromClass(final Class propertyType, final String name) {
        final LwIntrospectedProperty property;
        if (int.class.equals(propertyType)) {
            property = new LwIntroIntProperty(name);
        } else if (boolean.class.equals(propertyType)) {
            property = new LwIntroBooleanProperty(name);
        } else if (double.class.equals(propertyType)) {
            property = new LwIntroDoubleProperty(name);
        } else if (float.class.equals(propertyType)) {
            property = new LwIntroFloatProperty(name);
        } else if (String.class.equals(propertyType)) {
            property = new LwRbIntroStringProperty(name);
        } else if (Insets.class.equals(propertyType)) {
            property = new LwIntroInsetsProperty(name);
        } else if (Dimension.class.equals(propertyType)) {
            property = new LwIntroDimensionProperty(name);
        } else if (Rectangle.class.equals(propertyType)) {
            property = new LwIntroRectangleProperty(name);
        } else if (Color.class.equals(propertyType)) {
            property = new LwIntroColorProperty(name);
        } else if (Font.class.equals(propertyType)) {
            property = new LwIntroFontProperty(name);
        } else if (Icon.class.equals(propertyType)) {
            property = new LwIntroIconProperty(name);
        } else if (Component.class.isAssignableFrom(propertyType)) {
            property = new LwIntroComponentProperty(name, propertyType.getName());
        } else if (ListModel.class.isAssignableFrom(propertyType)) {
            property = new LwIntroListModelProperty(name, propertyType.getName());
        } else if (propertyType.getSuperclass() != null && "java.lang.Enum".equals(propertyType.getSuperclass().getName())) {
            property = new LwIntroEnumProperty(name, propertyType);
        } else {
            property = null;
        }
        return property;
    }
}
