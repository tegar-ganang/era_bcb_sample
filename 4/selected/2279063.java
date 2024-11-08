package net.infordata.ifw2m.props;

import java.beans.Introspector;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastConstructor;
import org.apache.log4j.Logger;
import org.hibernate.validator.ClassValidator;

/**
 * Class that can be used to inspect properties of classes or interfaces.
 */
public class ClassInfo<C> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(ClassInfo.class);

    static final Object[] EMPTY = new Object[] {};

    private static ConcurrentHashMap<Class<?>, ClassInfo<?>> cvInstances = new ConcurrentHashMap<Class<?>, ClassInfo<?>>();

    private static final boolean CLASSVALIDATORENABLED;

    private final Class<C> ivClass;

    private final transient FastClass ivFClass;

    private transient ClassInfo<?>[] ivParents;

    private final transient ConcurrentHashMap<String, Property<C, ?>> ivProperties = new ConcurrentHashMap<String, Property<C, ?>>();

    private transient FastConstructor ivConstructor;

    private final transient PropertyNavigator<C> ivNavigator;

    private transient ClassValidator<C> ivValidator;

    static {
        boolean enabled = false;
        try {
            new ClassValidator<String>(String.class);
            enabled = true;
        } catch (NoClassDefFoundError ex) {
            LOGGER.info("Hibernate ClassValidator disabled, class not found: " + ex.getMessage());
        } catch (Throwable ex) {
            LOGGER.error("Hibernate ClassValidator disabled.", ex);
        }
        CLASSVALIDATORENABLED = enabled;
    }

    /**
   * Static access method (readden values are cached).
   */
    @SuppressWarnings("unchecked")
    public static <C> ClassInfo<C> getInstance(Class<C> aClass) {
        ClassInfo<?> classInfo = cvInstances.get(aClass);
        if (classInfo == null) {
            classInfo = new ClassInfo<C>(aClass);
            ClassInfo<?> old = cvInstances.putIfAbsent(aClass, classInfo);
            if (old != null) classInfo = old;
        }
        return (ClassInfo<C>) classInfo;
    }

    /**
   */
    @SuppressWarnings("unchecked")
    public static <C> ClassInfo<C> getInstance(C anInstance) {
        return getInstance((Class<C>) anInstance.getClass());
    }

    /**
   */
    private ClassInfo(Class<C> aClass) {
        ivClass = aClass;
        ivFClass = FastClass.create(aClass);
        introspect();
        {
            ThisProperty<C> pr = new ThisProperty<C>(this);
            ivProperties.put(pr.getName(), pr);
        }
        {
            ToStringProperty<C> pr = new ToStringProperty<C>(this);
            ivProperties.put(pr.getName(), pr);
        }
        ivNavigator = new PropertyNavigator<C>(this);
        if (CLASSVALIDATORENABLED) {
            ClassValidator<C> validator = new ClassValidator<C>(ivClass);
            ivValidator = validator.hasValidationRules() ? validator : null;
        } else ivValidator = null;
    }

    /**
   */
    private void introspect() {
        Class<? super C> parent = ivClass.getSuperclass();
        int offset = (parent != null) ? 1 : 0;
        Class<?>[] interfaces = ivClass.getInterfaces();
        ivParents = new ClassInfo<?>[interfaces.length + offset];
        if (parent != null) ivParents[0] = getInstance(parent);
        for (int i = 0; i < interfaces.length; i++) {
            ivParents[i + offset] = getInstance(interfaces[i]);
        }
        try {
            Constructor<C> constr = ivClass.getConstructor();
            ivConstructor = ivFClass.getConstructor(constr);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
        }
        Method[] methods = ivClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            int mods = method.getModifiers();
            if (Modifier.isStatic(mods) || !Modifier.isPublic(mods)) continue;
            String name = method.getName();
            Class<?> argTypes[] = method.getParameterTypes();
            Class<?> resultType = method.getReturnType();
            method.setAccessible(true);
            int argCount = argTypes.length;
            String property = null;
            if (argCount == 0) {
                if (name.startsWith("get")) {
                    property = Introspector.decapitalize(name.substring(3));
                    if (LOGGER.isDebugEnabled()) LOGGER.debug("get - " + property + " " + method);
                    addGetSetProperty(property, resultType, method, null);
                } else if ((resultType == boolean.class || resultType == Boolean.class) && name.startsWith("is")) {
                    property = Introspector.decapitalize(name.substring(2));
                    if (LOGGER.isDebugEnabled()) LOGGER.debug("is - " + property + " " + method);
                    addGetSetProperty(property, resultType, method, null);
                } else if (resultType != void.class) {
                    property = name + "()";
                    if (LOGGER.isDebugEnabled()) LOGGER.debug(property + " " + method);
                    addReadMethodProperty(property, resultType, method);
                }
            } else if (argCount == 1) {
                if (name.startsWith("set")) {
                    property = Introspector.decapitalize(name.substring(3));
                    if (LOGGER.isDebugEnabled()) LOGGER.debug("set - " + property + " " + method);
                    addGetSetProperty(property, argTypes[0], null, method);
                }
            }
        }
    }

    /**
   */
    @SuppressWarnings("unchecked")
    private <T> void addGetSetProperty(String propertyName, Class<T> type, Method readMethod, Method writeMethod) {
        GetSetProperty<C, T> property = (GetSetProperty<C, T>) ivProperties.get(propertyName);
        if (property == null) property = new GetSetProperty<C, T>(this, propertyName, type);
        if (readMethod != null) property.setReadMethod(ivFClass.getMethod(readMethod), readMethod);
        if (writeMethod != null) property.setWriteMethod(ivFClass.getMethod(writeMethod));
        ivProperties.put(propertyName, property);
    }

    /**
   */
    @SuppressWarnings("unchecked")
    private <T> void addReadMethodProperty(String propertyName, Class<T> type, Method readMethod) {
        ReadMethodProperty<C, T> property = (ReadMethodProperty<C, T>) ivProperties.get(propertyName);
        if (property == null) property = new ReadMethodProperty<C, T>(this, propertyName, type);
        if (readMethod != null) property.setReadMethod(ivFClass.getMethod(readMethod));
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
    @SuppressWarnings("unchecked")
    public C newInstance() {
        if (ivConstructor == null) throw new IllegalStateException("No no-params ctor for class: " + ivClass.getName());
        try {
            return (C) ivConstructor.newInstance();
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
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
   * @return the {@link Property} object for the given property name
   */
    @SuppressWarnings("unchecked")
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

    public static final boolean isClassValidatorEnabled() {
        return CLASSVALIDATORENABLED;
    }

    public final ClassValidator<C> getClassValidator() {
        return ivValidator;
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
}
