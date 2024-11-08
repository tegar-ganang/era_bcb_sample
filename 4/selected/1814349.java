package org.granite.generator.as3.reflect;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.granite.generator.as3.reflect.JavaMethod.MethodType;

/**
 * @author Franck WOLFF
 */
public class JavaInterface extends JavaAbstractType {

    protected final Set<JavaType> imports;

    protected final List<JavaInterface> interfaces;

    protected final List<JavaProperty> properties;

    public JavaInterface(JavaTypeFactory provider, Class<?> type, URL url) {
        super(provider, type, url);
        if (!type.isInterface()) throw new IllegalArgumentException("type should be an interface: " + type);
        this.interfaces = Collections.unmodifiableList(provider.getJavaTypeInterfaces(type));
        this.properties = getSortedUnmodifiableList(initProperties());
        Set<JavaType> tmpImports = new HashSet<JavaType>();
        for (JavaInterface interfaze : interfaces) tmpImports.add(provider.getJavaImport(interfaze.getType()));
        for (JavaProperty property : properties) tmpImports.add(provider.getJavaImport(property.getType()));
        this.imports = Collections.unmodifiableSet(removeNull(tmpImports));
    }

    public Set<JavaType> getImports() {
        return imports;
    }

    public boolean hasSuperInterfaces() {
        return interfaces != null && !interfaces.isEmpty();
    }

    public List<JavaInterface> getSuperInterfaces() {
        return interfaces;
    }

    public Collection<JavaProperty> getProperties() {
        return properties;
    }

    protected Collection<JavaProperty> initProperties() {
        List<JavaProperty> properties = new ArrayList<JavaProperty>();
        for (PropertyDescriptor propertyDescriptor : getPropertyDescriptors(getType())) {
            String name = propertyDescriptor.getName();
            JavaMethod readMethod = null;
            JavaMethod writeMethod = null;
            Method method = propertyDescriptor.getReadMethod();
            if (method != null) readMethod = new JavaMethod(method, MethodType.GETTER);
            method = propertyDescriptor.getWriteMethod();
            if (method != null) writeMethod = new JavaMethod(method, MethodType.SETTER);
            if (readMethod != null || writeMethod != null) properties.add(new JavaMethodProperty(provider, name, readMethod, writeMethod));
        }
        return properties;
    }
}
