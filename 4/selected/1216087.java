package org.granite.generator.as3.reflect;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.granite.generator.as3.As3Type;
import org.granite.generator.as3.reflect.JavaMethod.MethodType;
import org.granite.messaging.amf.io.util.externalizer.annotation.ExternalizedProperty;
import org.granite.messaging.amf.io.util.externalizer.annotation.IgnoredProperty;
import org.granite.tide.annotations.TideEvent;

/**
 * @author Franck WOLFF
 */
public class JavaBean extends JavaAbstractType {

    protected final Set<JavaImport> imports = new HashSet<JavaImport>();

    protected final JavaType superclass;

    protected final As3Type as3Superclass;

    protected final List<JavaInterface> interfaces;

    protected final List<JavaProperty> interfacesProperties;

    protected final SortedMap<String, JavaProperty> properties;

    protected final JavaProperty uid;

    public JavaBean(JavaTypeFactory provider, Class<?> type, URL url) {
        super(provider, type, url);
        this.superclass = provider.getJavaTypeSuperclass(type);
        if (this.superclass == null && type.isAnnotationPresent(TideEvent.class)) as3Superclass = new As3Type("org.granite.tide.events", "AbstractTideEvent"); else as3Superclass = null;
        this.interfaces = Collections.unmodifiableList(provider.getJavaTypeInterfaces(type));
        this.properties = Collections.unmodifiableSortedMap(initProperties());
        Map<String, JavaProperty> allProperties = new HashMap<String, JavaProperty>(this.properties);
        for (JavaType supertype = this.superclass; supertype instanceof JavaBean; supertype = ((JavaBean) supertype).superclass) allProperties.putAll(((JavaBean) supertype).properties);
        Map<String, JavaProperty> iPropertyMap = new HashMap<String, JavaProperty>();
        for (JavaInterface interfaze : interfaces) {
            for (JavaProperty property : interfaze.getProperties()) {
                String name = property.getName();
                if (!iPropertyMap.containsKey(name) && !allProperties.containsKey(name)) iPropertyMap.put(name, property);
            }
        }
        this.interfacesProperties = getSortedUnmodifiableList(iPropertyMap.values());
        JavaProperty tmpUid = null;
        for (JavaProperty property : properties.values()) {
            if (provider.isUid(property)) {
                tmpUid = property;
                break;
            }
        }
        this.uid = tmpUid;
        if (superclass != null) addToImports(provider.getJavaImport(superclass.getType()));
        for (JavaInterface interfaze : interfaces) addToImports(provider.getJavaImport(interfaze.getType()));
        for (JavaProperty property : properties.values()) addToImports(provider.getJavaImport(property.getType()));
    }

    public Set<JavaImport> getImports() {
        return imports;
    }

    protected void addToImports(JavaImport javaImport) {
        if (javaImport != null) imports.add(javaImport);
    }

    public boolean hasSuperclass() {
        return superclass != null;
    }

    public JavaType getSuperclass() {
        return superclass;
    }

    public As3Type getAs3Superclass() {
        return as3Superclass;
    }

    public boolean hasInterfaces() {
        return interfaces != null && !interfaces.isEmpty();
    }

    public List<JavaInterface> getInterfaces() {
        return interfaces;
    }

    public boolean hasInterfacesProperties() {
        return interfacesProperties != null && !interfacesProperties.isEmpty();
    }

    public List<JavaProperty> getInterfacesProperties() {
        return interfacesProperties;
    }

    public Collection<JavaProperty> getProperties() {
        return properties.values();
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return type.isAnnotationPresent(annotation);
    }

    public boolean hasUid() {
        return uid != null;
    }

    public JavaProperty getUid() {
        return uid;
    }

    public boolean hasEnumProperty() {
        for (JavaProperty property : properties.values()) {
            if (property.isEnum()) return true;
        }
        return false;
    }

    protected SortedMap<String, JavaProperty> initProperties() {
        PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(type);
        SortedMap<String, JavaProperty> propertyMap = new TreeMap<String, JavaProperty>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers()) && !"jdoDetachedState".equals(field.getName()) && !field.isAnnotationPresent(IgnoredProperty.class)) {
                String name = field.getName();
                JavaMethod readMethod = null;
                JavaMethod writeMethod = null;
                if (field.getType().isMemberClass() && !field.getType().isEnum()) throw new UnsupportedOperationException("Inner classes are not supported (except enums): " + field.getType());
                if (propertyDescriptors != null) {
                    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                        if (name.equals(propertyDescriptor.getName())) {
                            if (propertyDescriptor.getReadMethod() != null) readMethod = new JavaMethod(propertyDescriptor.getReadMethod(), MethodType.GETTER);
                            if (propertyDescriptor.getWriteMethod() != null) writeMethod = new JavaMethod(propertyDescriptor.getWriteMethod(), MethodType.SETTER);
                            break;
                        }
                    }
                }
                JavaFieldProperty property = new JavaFieldProperty(provider, field, readMethod, writeMethod);
                propertyMap.put(name, property);
            }
        }
        if (propertyDescriptors != null) {
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getReadMethod().getDeclaringClass().equals(type) && propertyDescriptor.getReadMethod().isAnnotationPresent(ExternalizedProperty.class) && !propertyMap.containsKey(propertyDescriptor.getName())) {
                    JavaMethod readMethod = new JavaMethod(propertyDescriptor.getReadMethod(), MethodType.GETTER);
                    JavaMethodProperty property = new JavaMethodProperty(provider, propertyDescriptor.getName(), readMethod, null);
                    propertyMap.put(propertyDescriptor.getName(), property);
                }
            }
        }
        return propertyMap;
    }
}
