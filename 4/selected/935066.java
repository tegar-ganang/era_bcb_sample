package org.granite.generator.as3.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.granite.generator.as3.reflect.JavaMethod.MethodType;
import org.granite.messaging.service.annotations.IgnoredMethod;
import org.granite.messaging.service.annotations.RemoteDestination;

/**
 * @author Franck WOLFF
 */
public class JavaRemoteDestination extends JavaAbstractType {

    protected final Set<JavaImport> imports = new HashSet<JavaImport>();

    protected final JavaType superclass;

    protected final List<JavaMethod> methods;

    protected final String destinationName;

    protected final String channelId;

    public JavaRemoteDestination(JavaTypeFactory provider, Class<?> type, URL url) {
        super(provider, type, url);
        this.superclass = provider.getJavaTypeSuperclass(type);
        this.methods = Collections.unmodifiableList(initMethods());
        if (superclass != null) addToImports(provider.getJavaImport(superclass.getType()));
        RemoteDestination rd = type.getAnnotation(RemoteDestination.class);
        if (rd != null) {
            destinationName = rd.id();
            channelId = rd.channel();
        } else {
            destinationName = null;
            channelId = null;
        }
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

    public String getDestinationName() {
        return destinationName;
    }

    public String getChannelId() {
        return channelId;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return type.isAnnotationPresent(annotation);
    }

    protected List<JavaMethod> initMethods() {
        List<JavaMethod> methodMap = new ArrayList<JavaMethod>();
        Method[] methods = null;
        if (type.isInterface()) methods = type.getMethods(); else methods = type.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && !method.isAnnotationPresent(IgnoredMethod.class)) {
                for (Class<?> clazz : method.getParameterTypes()) {
                    if (clazz.isMemberClass() && !clazz.isEnum()) {
                        throw new UnsupportedOperationException("Inner classes are not supported (except enums): " + clazz);
                    }
                    addToImports(provider.getJavaImport(clazz));
                }
                methodMap.add(new JavaMethod(method, MethodType.OTHER, this.provider));
            }
        }
        return methodMap;
    }

    public JavaInterface convertToJavaInterface() {
        return new JavaInterface(getProvider(), getType(), getUrl());
    }
}
