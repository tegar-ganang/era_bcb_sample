package net.infordata.ifw2.props;

import java.beans.Introspector;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that can be used to inspect properties of classes or interfaces.
 */
public class ClassInfo<C> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassInfo.class);

    static final Object[] EMPTY = new Object[] {};

    private static ConcurrentHashMap<Class<?>, ClassInfo<?>> cvInstances = new ConcurrentHashMap<Class<?>, ClassInfo<?>>();

    private final Class<C> ivClass;

    private transient ClassInfo<?>[] ivParents;

    private final transient ConcurrentHashMap<String, Property<C, ?>> ivProperties = new ConcurrentHashMap<String, Property<C, ?>>();

    private transient Constructor<C> ivConstructor;

    private final transient PropertyNavigator<C> ivNavigator;

    /**
   * Static access method (readden values are cached).
   */
    @SuppressWarnings("unchecked")
    public static <C> ClassInfo<C> getInstance(Class<C> aClass, ClassPool classPool) {
        ClassInfo<?> classInfo = cvInstances.get(aClass);
        if (classInfo == null) {
            classInfo = new ClassInfo<C>(aClass, classPool);
            ClassInfo<?> old = cvInstances.putIfAbsent(aClass, classInfo);
            if (old != null) classInfo = old;
        }
        return (ClassInfo<C>) classInfo;
    }

    public static final <C> ClassInfo<C> getInstance(Class<C> aClass) {
        return getInstance(aClass, null);
    }

    /**
   */
    @SuppressWarnings("unchecked")
    public static <C> ClassInfo<C> getInstance(C anInstance) {
        return getInstance((Class<C>) anInstance.getClass());
    }

    /**
   */
    private ClassInfo(Class<C> aClass, ClassPool classPool) {
        ivClass = aClass;
        introspect(classPool);
        {
            ThisProperty<C> pr = new ThisProperty<C>(this);
            ivProperties.put(pr.getName(), pr);
        }
        {
            ToStringProperty<C> pr = new ToStringProperty<C>(this);
            ivProperties.put(pr.getName(), pr);
        }
        ivNavigator = new PropertyNavigator<C>(this);
    }

    /**
   */
    private void introspect(ClassPool classPool) {
        Class<? super C> parent = ivClass.getSuperclass();
        int offset = (parent != null) ? 1 : 0;
        Class<?>[] interfaces = ivClass.getInterfaces();
        ivParents = new ClassInfo<?>[interfaces.length + offset];
        if (parent != null) ivParents[0] = getInstance(parent);
        for (int i = 0; i < interfaces.length; i++) {
            ivParents[i + offset] = getInstance(interfaces[i]);
        }
        try {
            ivConstructor = ivClass.getConstructor();
            ivConstructor.setAccessible(true);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
        }
        Method[] methods = ivClass.getMethods();
        class Bag {

            Class<?> declaringClass;

            Method readMethod;

            Method writeMethod;

            Class<?> readType;

            Class<?> writeType;
        }
        Map<String, Bag> getSetProps = new HashMap<String, Bag>();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            int mods = method.getModifiers();
            if (Modifier.isStatic(mods) || !Modifier.isPublic(mods)) continue;
            String name = method.getName();
            Class<?> argTypes[] = method.getParameterTypes();
            int argCount = argTypes.length;
            String property = null;
            if (argCount == 0) {
                Class<?> resultType = method.getReturnType();
                final boolean isAGet = name.startsWith("get");
                if (isAGet || ((resultType == boolean.class || resultType == Boolean.class) && name.startsWith("is"))) {
                    property = Introspector.decapitalize(name.substring(isAGet ? 3 : 2));
                    if (LOGGER.isDebugEnabled()) LOGGER.debug("get - " + property + " " + method);
                    Bag bag = getSetProps.get(property);
                    if (bag == null) {
                        bag = new Bag();
                        getSetProps.put(property, bag);
                        bag.declaringClass = method.getDeclaringClass();
                        bag.readType = resultType;
                        bag.readMethod = method;
                    } else if (bag.readMethod == null || bag.declaringClass.isAssignableFrom(method.getDeclaringClass())) {
                        if (bag.declaringClass.isAssignableFrom(method.getDeclaringClass())) {
                            bag.declaringClass = method.getDeclaringClass();
                        }
                        bag.readType = resultType;
                        bag.readMethod = method;
                    }
                } else if (resultType != void.class && ivClass == method.getDeclaringClass()) {
                    property = name + "()";
                    if (LOGGER.isDebugEnabled()) LOGGER.debug(property + " " + method);
                    addReadMethodProperty(classPool, property, resultType, method);
                }
            } else if (argCount == 1) {
                if (name.startsWith("set")) {
                    property = Introspector.decapitalize(name.substring(3));
                    if (LOGGER.isDebugEnabled()) LOGGER.debug("set - " + property + " " + method);
                    Bag bag = getSetProps.get(property);
                    if (bag == null) {
                        bag = new Bag();
                        getSetProps.put(property, bag);
                        bag.declaringClass = method.getDeclaringClass();
                        bag.writeType = argTypes[0];
                        bag.writeMethod = method;
                    } else if (bag.writeMethod == null || bag.declaringClass.isAssignableFrom(method.getDeclaringClass())) {
                        if (bag.declaringClass.isAssignableFrom(method.getDeclaringClass())) {
                            bag.declaringClass = method.getDeclaringClass();
                        }
                        bag.writeType = argTypes[0];
                        bag.writeMethod = method;
                    }
                }
            }
        }
        for (Map.Entry<String, Bag> en : getSetProps.entrySet()) {
            Bag bag = en.getValue();
            if (bag.declaringClass == ivClass) {
                addGetSetProperty(classPool, en.getKey(), bag.readMethod, bag.readType, bag.writeMethod, bag.writeType);
            }
        }
    }

    /**
   */
    @SuppressWarnings("unchecked")
    private <T> void addGetSetProperty(ClassPool classPool, String propertyName, Method readMethod, Class<?> readType, Method writeMethod, Class<?> writeType) {
        Class<?> theType = null;
        if (readType != null && writeType != null) {
            if (readType.isAssignableFrom(writeType)) theType = writeType; else if (writeType.isAssignableFrom(readType)) theType = writeType; else throw new IllegalArgumentException("prop: " + propertyName + " " + readType + " - is not compatible with:" + writeType);
        } else if (readType != null) {
            theType = readType;
        } else if (writeType != null) {
            theType = writeType;
        } else {
            throw new NullPointerException();
        }
        GetSetProperty<C, T> property = (GetSetProperty<C, T>) ivProperties.get(propertyName);
        if (property == null) {
            try {
                property = createGetSetProperty(classPool, this, ivClass, readMethod, writeMethod, theType, propertyName);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        if (readMethod != null) property.setAnnotatedMethod(readMethod);
        ivProperties.put(propertyName, property);
    }

    /**
   */
    @SuppressWarnings("unchecked")
    private <T> void addReadMethodProperty(ClassPool classPool, String propertyName, Class<T> type, Method readMethod) {
        ReadMethodProperty<C, T> property = (ReadMethodProperty<C, T>) ivProperties.get(propertyName);
        if (property == null) {
            try {
                property = createReadMethodProperty(classPool, this, ivClass, readMethod, type, propertyName);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        ivProperties.put(propertyName, property);
    }

    /**
  public <T> void addProperty(Property<C,T> property) {
    String propertyName = property.getName();

    if (propertyName == null)
      throw new IllegalArgumentException("null parameter");
    if (getDeclaredProperty(propertyName) != null)
      throw new IllegalArgumentException("Property: " +
                                         propertyName +
                                         " already defined in: " +
                                         toString());

    ivProperties.put(propertyName, property);
  }
   */
    public C newInstance() {
        if (ivConstructor == null) throw new IllegalStateException("No no-params ctor for class: " + ivClass.getName());
        try {
            return (C) ivConstructor.newInstance();
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public final boolean canCreateNewInstances() {
        return ivConstructor != null;
    }

    /**
   */
    @SuppressWarnings("unchecked")
    public <T> Property<C, T> getDeclaredProperty(String propertyName) {
        return (ivProperties != null) ? (Property<C, T>) ivProperties.get(propertyName) : null;
    }

    /**
   * @param <T>
   * @param propertyName - Can be a composite name, ie. a property of a property
   *     like person.name.lenght
   * @return the {@link Property} object for the given property name, if
   *   it is a composite name the a {@link CompositeProperty} is returned.
   */
    public <T> Property<C, T> getProperty(String propertyName) {
        return getPN().getProperty(propertyName);
    }

    /**
   */
    @SuppressWarnings("unchecked")
    public <T> Property<C, T> getDirectProperty(String propertyName) {
        Property<C, T> property = getDeclaredProperty(propertyName);
        if (property != null) return property;
        for (int i = 0; i < ivParents.length; i++) {
            property = (Property<C, T>) ivParents[i].getProperty(propertyName);
            if (property != null) return property;
        }
        return null;
    }

    /**
   */
    private List<Property<?, ?>> getDeclaredProperties(List<Property<?, ?>> vt) {
        if (ivProperties == null) return vt;
        if (vt == null) vt = new ArrayList<Property<?, ?>>(20);
        for (Property<C, ?> property : ivProperties.values()) {
            vt.add(property);
        }
        return vt;
    }

    /**
   */
    private List<Property<?, ?>> getProperties(List<Property<?, ?>> vt) {
        vt = getDeclaredProperties(vt);
        for (int i = 0; i < ivParents.length; i++) vt = ivParents[i].getProperties(vt);
        return vt;
    }

    /**
   */
    public Property<?, ?>[] getDeclaredProperties() {
        List<Property<?, ?>> vt = null;
        vt = getDeclaredProperties(vt);
        if (vt == null) return null;
        return vt.toArray(new Property<?, ?>[vt.size()]);
    }

    /**
   */
    public Property<?, ?>[] getProperties() {
        List<Property<?, ?>> vt = null;
        vt = getProperties(vt);
        if (vt == null) return null;
        return vt.toArray(new Property<?, ?>[vt.size()]);
    }

    /**
   */
    public final Class<C> getRelatedClass() {
        return ivClass;
    }

    PropertyNavigator<C> getPN() {
        return ivNavigator;
    }

    Object readResolve() throws ObjectStreamException {
        return getInstance(ivClass);
    }

    /**
   */
    @Override
    public String toString() {
        return "" + ivClass;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ivClass == null) ? 0 : ivClass.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        @SuppressWarnings("rawtypes") ClassInfo other = (ClassInfo) obj;
        if (ivClass == null) {
            if (other.ivClass != null) return false;
        } else if (!ivClass.equals(other.ivClass)) return false;
        return true;
    }

    private static final CtClass[] NO_CTARGS = {};

    private static final javassist.ClassPool CLASS_POOL;

    private static final CtClass OBJECT_CTCLASS;

    private static final CtClass CLASS_CTCLASS;

    private static final CtClass STRING_CTCLASS;

    private static final CtClass CLASSINFO_CTCLASS;

    private static final CtClass GETSETPROPERTY_CTCLASS;

    private static final CtClass READMETHODPROPERTY_CTCLASS;

    static {
        CLASS_POOL = new javassist.ClassPool(true);
        CLASS_POOL.insertClassPath(new ClassClassPath(ClassInfo.class));
        try {
            OBJECT_CTCLASS = CLASS_POOL.get(Object.class.getName());
            CLASS_CTCLASS = CLASS_POOL.get(Class.class.getName());
            STRING_CTCLASS = CLASS_POOL.get(String.class.getName());
            CLASSINFO_CTCLASS = CLASS_POOL.get(ClassInfo.class.getName());
            GETSETPROPERTY_CTCLASS = CLASS_POOL.get(GetSetProperty.class.getName());
            READMETHODPROPERTY_CTCLASS = CLASS_POOL.get(ReadMethodProperty.class.getName());
        } catch (NotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <C, T> GetSetProperty<C, T> createGetSetProperty(ClassPool classPool, final ClassInfo<C> classInfo, final Class<C> clazz, Method readMethod, Method writeMethod, Class<?> theType, String propertyName) throws NotFoundException, CannotCompileException, IOException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        final ClassPool pool = classPool != null ? classPool : new ClassPool();
        final String clazzName = clazz.getName();
        final String valueClazzName = theType.getName();
        final String newClassName;
        {
            StringBuilder sb = new StringBuilder(512);
            sb.append(GetSetProperty.class.getName()).append("$").append(clazzName.replace('.', '_'));
            if (readMethod != null) {
                sb.append("$").append(readMethod.getName());
            }
            if (writeMethod != null) {
                sb.append("$").append(writeMethod.getName());
            }
            sb.append("$").append((valueClazzName != null ? valueClazzName : valueClazzName).replace('.', '_'));
            newClassName = sb.toString();
        }
        CtClass newClass = pool.makeClass(newClassName);
        newClass.setSuperclass(GETSETPROPERTY_CTCLASS);
        {
            CtConstructor cons = new CtConstructor(new CtClass[] { CLASSINFO_CTCLASS, STRING_CTCLASS, CLASS_CTCLASS }, newClass);
            cons.setBody("super($1, $2, $3);");
            newClass.addConstructor(cons);
        }
        {
            CtMethod meth = new CtMethod(OBJECT_CTCLASS, "get", new CtClass[] { OBJECT_CTCLASS }, newClass);
            if (readMethod != null) {
                meth.setBody("return ($w)((" + clazzName + ")$1)." + readMethod.getName() + "();");
            } else {
                meth.setBody("throw new IllegalStateException(\"" + propertyName + "\");");
            }
            newClass.addMethod(meth);
        }
        {
            CtMethod meth = new CtMethod(CtClass.voidType, "set", new CtClass[] { OBJECT_CTCLASS, OBJECT_CTCLASS }, newClass);
            if (writeMethod != null) {
                if (theType.isPrimitive()) {
                    {
                        CtMethod castMeth = new CtMethod(pool.get(valueClazzName), "cast", new CtClass[] { OBJECT_CTCLASS }, newClass);
                        castMeth.setBody("return ($r)$1;");
                        newClass.addMethod(castMeth);
                    }
                    meth.setBody("{" + valueClazzName + " xx = this.cast($2);" + "((" + clazzName + ")$1)." + writeMethod.getName() + "(xx);" + "}");
                } else {
                    meth.setBody("((" + clazzName + ")$1)." + writeMethod.getName() + "((" + valueClazzName + ")$2);");
                }
            } else {
                meth.setBody("throw new IllegalStateException(\"" + propertyName + "\");");
            }
            newClass.addMethod(meth);
        }
        {
            CtMethod meth = new CtMethod(CtClass.booleanType, "isReadable", NO_CTARGS, newClass);
            meth.setBody("return " + (readMethod != null) + ";");
            newClass.addMethod(meth);
        }
        {
            CtMethod meth = new CtMethod(CtClass.booleanType, "isWriteable", NO_CTARGS, newClass);
            meth.setBody("return " + (writeMethod != null) + ";");
            newClass.addMethod(meth);
        }
        Class<GetSetProperty<C, T>> theClass = pool.toClass(newClass);
        newClass.detach();
        Constructor<GetSetProperty<C, T>> ctor = theClass.getConstructor(ClassInfo.class, String.class, Class.class);
        GetSetProperty<C, T> res = ctor.newInstance(classInfo, propertyName, convertType(theType));
        return res;
    }

    static Class<?> convertType(Class<?> clazz) {
        if (!clazz.isPrimitive()) return clazz;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == int.class) return Integer.class;
        if (clazz == long.class) return Long.class;
        if (clazz == double.class) return Double.class;
        if (clazz == char.class) return Character.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == short.class) return Short.class;
        if (clazz == float.class) return Float.class;
        if (clazz == void.class) return Void.class;
        throw new IllegalStateException("Unexpected primitive type: " + clazz);
    }

    @SuppressWarnings("unchecked")
    private static <C, T> ReadMethodProperty<C, T> createReadMethodProperty(ClassPool classPool, final ClassInfo<C> classInfo, final Class<C> clazz, Method readMethod, Class<T> valueType, String propertyName) throws NotFoundException, CannotCompileException, IOException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        final ClassPool pool = classPool != null ? classPool : new ClassPool();
        final String clazzName = clazz.getName();
        final String valueClazzName = valueType.getName();
        final String newClassName;
        {
            StringBuilder sb = new StringBuilder(512);
            sb.append(ReadMethodProperty.class.getName()).append("$").append(clazzName.replace('.', '_'));
            if (readMethod != null) {
                sb.append("$").append(readMethod.getName());
            }
            sb.append("$").append(valueClazzName.replace('.', '_'));
            newClassName = sb.toString();
        }
        CtClass newClass = pool.makeClass(newClassName);
        newClass.setSuperclass(READMETHODPROPERTY_CTCLASS);
        {
            CtConstructor cons = new CtConstructor(new CtClass[] { CLASSINFO_CTCLASS, STRING_CTCLASS, CLASS_CTCLASS }, newClass);
            cons.setBody("super($1, $2, $3);");
            newClass.addConstructor(cons);
        }
        {
            CtMethod meth = new CtMethod(OBJECT_CTCLASS, "get", new CtClass[] { OBJECT_CTCLASS }, newClass);
            if (readMethod != null) {
                meth.setBody("return ($w)((" + clazzName + ")$1)." + readMethod.getName() + "();");
            } else {
                meth.setBody("throw new IllegalStateException(\"" + propertyName + "\");");
            }
            newClass.addMethod(meth);
        }
        {
            CtMethod meth = new CtMethod(CtClass.booleanType, "isReadable", NO_CTARGS, newClass);
            meth.setBody("return " + (readMethod != null) + ";");
            newClass.addMethod(meth);
        }
        Class<ReadMethodProperty<C, T>> theClass = pool.toClass(newClass);
        newClass.detach();
        Constructor<ReadMethodProperty<C, T>> ctor = theClass.getConstructor(ClassInfo.class, String.class, Class.class);
        ReadMethodProperty<C, T> res = ctor.newInstance(classInfo, propertyName, convertType(valueType));
        return res;
    }

    public static class ClassPool extends javassist.ClassPool {

        public ClassPool() {
            super(CLASS_POOL);
        }
    }
}
