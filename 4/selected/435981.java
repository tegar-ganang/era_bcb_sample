package org.beanutopia.reflect;

import static org.beanutopia.function.PropertyFunctions.isInstanceOf;
import org.beanutopia.exception.PropertyDefinitionException;
import org.beanutopia.exception.PropertyManipulationException;
import static org.beanutopia.function.PropertyFunctions.isSupertypeOf;
import org.beanutopia.function.*;
import org.beanutopia.slot.Slot;
import com.google.common.base.Function;
import com.google.common.base.Nullable;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author ymeymann
 * @since Nov 21, 2007 11:49:46 PM
 */
public class ImportSlot<V> implements Slot<Object, V> {

    private GetMethod<V> reader;

    private SetMethod<V> writer;

    private ImportSlot(Class lib, @Nullable Class objectClass, String propertyName, @Nullable Class<V> valueClass, @Nullable String getterPrefix, @Nullable String setterPrefix) {
        reader = reader(lib, propertyName, objectClass, valueClass, getterPrefix);
        writer = writer(lib, propertyName, objectClass, valueClass, setterPrefix);
    }

    public static Predicate<Class<?>> isConvertibleTo(Class<?> c) {
        return CompositeFunction.self(BOX).condition(new SubtypePredicate<Object>(BOX.apply(c)));
    }

    public static <V> GetMethod<V> reader(Class lib, String propertyName) {
        return reader(lib, propertyName, null, null, "get");
    }

    public static <V> GetMethod<V> reader(Class lib, String propertyName, Class<V> valueClass) {
        return reader(lib, propertyName, null, valueClass, "get");
    }

    public static <V> GetMethod<V> reader(Class lib, String propertyName, @Nullable Class objectClass, Class<V> valueClass) {
        return reader(lib, propertyName, objectClass, valueClass, "get");
    }

    public static <V> GetMethod<V> reader(Class lib, String propertyName, @Nullable Class objectClass, Class<V> valueClass, String getterPrefix) {
        return new ImportSlot.Getter<V>(lib, objectClass, propertyName, valueClass, getterPrefix);
    }

    public static <V> SetMethod<V> writer(Class lib, String propertyName) {
        return writer(lib, propertyName, "set");
    }

    public static <V> SetMethod<V> writer(Class lib, String propertyName, @Nullable String setterPrefix) {
        return writer(lib, propertyName, null, setterPrefix);
    }

    public static <V> SetMethod<V> writer(Class lib, String propertyName, @Nullable Class<V> valueClass, @Nullable String setterPrefix) {
        return writer(lib, propertyName, null, valueClass, "set");
    }

    public static <V> SetMethod<V> writer(Class lib, String propertyName, @Nullable Class objectClass, @Nullable Class<V> valueClass, @Nullable String setterPrefix) {
        return new ImportSlot.Setter<V>(lib, propertyName, valueClass, setterPrefix);
    }

    public static <V> Slot<Object, V> importer(Class lib, String propertyName) {
        return importer(lib, propertyName, "get", "set");
    }

    public static <V> Slot<Object, V> importer(Class lib, String propertyName, @Nullable String getterPrefix, @Nullable String setterPrefix) {
        return importer(lib, propertyName, null, null, getterPrefix, setterPrefix);
    }

    public static <V> Slot<Object, V> importer(Class lib, String propertyName, @Nullable Class<V> valueClass) {
        return importer(lib, propertyName, null, valueClass, "get", "set");
    }

    public static <V> Slot<Object, V> importer(Class lib, String propertyName, @Nullable Class objectClass, @Nullable Class<V> valueClass, @Nullable String getterPrefix, @Nullable String setterPrefix) {
        return new ImportSlot<V>(lib, objectClass, propertyName, valueClass, getterPrefix, setterPrefix);
    }

    public Function<Object, V> getProjection() {
        return reader;
    }

    public Injection<Object, V> getInjection() {
        return writer;
    }

    public Slot<Object, V> initialize() throws PropertyDefinitionException {
        reader.initialize();
        writer.initialize();
        return this;
    }

    private final Predicate<Object> predicate = new Predicate<Object>() {

        public boolean apply(Object o) {
            return reader.supported().apply(o) && writer.supported().apply(o);
        }
    };

    public Predicate<Object> supported() {
        return predicate;
    }

    public static String buildMethodName(String propertyName, String prefix) {
        return (prefix != null && prefix.length() > 0) ? prefix + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1) : propertyName;
    }

    private static class Getter<V> implements GetMethod<V> {

        protected Policy<Object, Method> getters = new Policy<Object, Method>();

        protected String propertyName;

        protected String getterPrefix;

        protected Class<?> objectClass;

        protected Class<V> valueClass;

        protected Class<?> libClass;

        private boolean initialized = false;

        protected Getter() {
        }

        Getter(Class lib, @Nullable Class objectClass, String propertyName, @Nullable Class<V> valueClass, String getterPrefix) {
            this.propertyName = propertyName;
            this.getterPrefix = getterPrefix;
            this.objectClass = objectClass;
            this.valueClass = valueClass;
            this.libClass = lib;
        }

        public ImportSlot.Getter<V> initialize() throws PropertyDefinitionException {
            if (objectClass != null) {
                if (findMethod(objectClass) == null) throw new PropertyDefinitionException("Cannot findMethod reflector for property " + propertyName);
            }
            return this;
        }

        public Method findMethod(Class<?> objectClass) {
            return findMethod(objectClass, valueClass);
        }

        public Policy.Rule<Object, Method> getCachedMethod(Class objectClass) {
            return getters.getRule(objectClass);
        }

        public Method findMethod(Class<?> objectClass, Class<?> valueClass) {
            if (initialized) {
                Policy.Rule<Object, Method> rule = getCachedMethod(objectClass);
                return (rule != null ? rule.decision() : null);
            } else {
                initialized = true;
                Method getter = null;
                String methodName = buildMethodName(propertyName, getterPrefix).intern();
                for (Method m : libClass.getMethods()) {
                    Class<?>[] p;
                    if (((m.getModifiers() & Modifier.STATIC) > 0) && methodName.equals(m.getName().intern()) && (p = m.getParameterTypes()).length == 1 && (valueClass == null || isConvertibleTo(valueClass).apply(m.getReturnType()))) {
                        getters.addRule(isInstanceOf(BOX.apply(m.getParameterTypes()[0])), m);
                        if (isConvertibleTo(p[0]).apply(objectClass) && (getter == null || isConvertibleTo(getter.getParameterTypes()[0]).apply(p[0]))) getter = m;
                    }
                }
                if (getter == null) {
                    getters.addRule(isSupertypeOf(objectClass), null);
                }
                return getter;
            }
        }

        @SuppressWarnings({ "unchecked" })
        public V apply(Object o) {
            Method getter = findMethod(o.getClass());
            try {
                return (V) getter.invoke(null, o);
            } catch (Exception e) {
                throw new PropertyManipulationException("Cannot reflectively retrieve " + propertyName, e);
            }
        }

        private final Predicate<Object> predicate = new Predicate<Object>() {

            @SuppressWarnings({ "unchecked" })
            public boolean apply(@Nullable Object o) {
                if (o == null) return false;
                Class c = o instanceof Class ? (Class) o : o.getClass();
                return findMethod(c) != null;
            }
        };

        public Predicate<Object> supported() {
            return predicate;
        }
    }

    private static class Setter<V> implements SetMethod<V> {

        protected Policy<Object, Method> setters = new Policy<Object, Method>();

        protected String propertyName;

        protected String setterPrefix;

        protected Class<?> objectClass;

        protected Class<V> valueClass;

        protected Class<?> libClass;

        private boolean initialized = false;

        Setter(Class lib, String propertyName, @Nullable Class<V> valueClass, String setterPrefix) {
            this.propertyName = propertyName;
            this.setterPrefix = setterPrefix;
            this.valueClass = valueClass;
            this.libClass = lib;
        }

        public ImportSlot.Setter<V> initialize() throws PropertyDefinitionException {
            if (objectClass != null && valueClass != null) {
                if (findMethod(objectClass, valueClass) == null) throw new PropertyDefinitionException("Cannot initialize reflector for property " + propertyName);
            }
            return this;
        }

        public Policy.Rule<Object, Method> getCachedMethod(Class objectClass) {
            return setters.getRule(objectClass);
        }

        public Method findMethod(Class<?> objectClass, Class<?> valueClass) {
            Preconditions.checkNotNull(valueClass, "Value class cannot be null");
            Method setter = null;
            if (initialized) {
                Policy.Rule<Object, Method> rule = getCachedMethod(objectClass);
                if (rule != null) setter = rule.decision();
            } else {
                initialized = true;
                String methodName = buildMethodName(propertyName, setterPrefix).intern();
                for (Method m : objectClass.getMethods()) {
                    Class<?>[] p;
                    if (((m.getModifiers() & Modifier.STATIC) > 0) && methodName.equals(m.getName().intern()) && (p = m.getParameterTypes()).length == 2 && isConvertibleTo(p[1]).apply(valueClass)) {
                        setters.addRule(isInstanceOf(p[0]), m);
                        if (isConvertibleTo(p[0]).apply(objectClass) && (setter == null || isConvertibleTo(setter.getParameterTypes()[0]).apply(p[0]))) setter = m;
                    }
                }
                if (setter == null) {
                    setters.addRule(isSupertypeOf(objectClass), null);
                }
            }
            return setter;
        }

        public void apply(Object obj, V val) {
            Method setter = findMethod(obj.getClass(), val.getClass());
            try {
                setter.invoke(obj, val);
            } catch (Exception e) {
                throw new PropertyManipulationException("Cannot reflectively set " + propertyName, e);
            }
        }

        private final Predicate<Object> predicate = new Predicate<Object>() {

            public boolean apply(Object o) {
                if (o == null) return false;
                Class c = o instanceof Class ? (Class) o : o.getClass();
                return findMethod(c, valueClass) != null;
            }
        };

        public Predicate<Object> supported() {
            return predicate;
        }
    }
}
