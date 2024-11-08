package net.sf.lua4j.jsr223;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.lua4j.engine.LuaNil;
import net.sf.lua4j.engine.LuaObject;
import net.sf.lua4j.engine.LuaTable;

/**
 * Wraps a bean and provides a table
 *
 * @version $Revision: $ $Date: $
 */
public class BeanAdapter implements LuaTable {

    private static final String CLASS_NAME = BeanAdapter.class.getName();

    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    private static final Map<Class, Map<String, Accessor>> CACHE = new WeakHashMap<Class, Map<String, Accessor>>();

    private final Object bean;

    public BeanAdapter(Object bean) {
        this.bean = bean;
        Map<String, Accessor> accessors = getAccessors();
        if (accessors == null) CACHE.put(bean.getClass(), accessors = new HashMap<String, Accessor>());
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                accessors.put(descriptor.getName(), new Accessor(descriptor.getReadMethod(), descriptor.getWriteMethod()));
            }
        } catch (IntrospectionException ie) {
            LOGGER.log(Level.WARNING, "Unable to introspect bean", ie);
        }
    }

    public Object getUnderlying() {
        return bean;
    }

    public int size() {
        return getAccessors().size();
    }

    public boolean isEmpty() {
        return getAccessors().isEmpty();
    }

    public boolean containsKey(Object key) {
        return getAccessors().containsKey(key);
    }

    public boolean containsValue(Object value) {
        return getAccessors().containsValue(value);
    }

    public LuaObject get(Object key) {
        LuaObject result = LuaNil.NIL;
        Accessor accessor = getAccessors().get(key);
        if (accessor != null) {
            Object object = accessor.get(bean);
            if (object != null) result = new BeanAdapter(object);
        }
        return result;
    }

    public LuaObject put(String key, LuaObject value) {
        LuaObject result = LuaNil.NIL;
        Accessor accessor = getAccessors().get(key);
        if (accessor != null) {
            Object object = accessor.get(bean);
            accessor.set(bean, value.getUnderlying());
            if (object != null) result = new BeanAdapter(object);
        }
        return result;
    }

    public LuaObject remove(Object key) {
        throw new UnsupportedOperationException("Cannot change structure of underlying bean");
    }

    public void putAll(Map<? extends String, ? extends LuaObject> t) {
        for (Entry<? extends String, ? extends LuaObject> entry : t.entrySet()) {
            Accessor accessor = getAccessors().get(entry.getKey());
            if (accessor != null) {
                accessor.set(bean, entry.getValue().getUnderlying());
            }
        }
    }

    public void clear() {
        throw new UnsupportedOperationException("Cannot change structure of underlying bean");
    }

    public Set<String> keySet() {
        return getAccessors().keySet();
    }

    public Collection<LuaObject> values() {
        return new Collection<LuaObject>() {

            public int size() {
                return getAccessors().size();
            }

            public boolean isEmpty() {
                return getAccessors().isEmpty();
            }

            public boolean contains(Object o) {
                return collect().contains(o);
            }

            public Iterator<LuaObject> iterator() {
                return new Iterator<LuaObject>() {

                    private final Iterator<String> keys = getAccessors().keySet().iterator();

                    public boolean hasNext() {
                        return keys.hasNext();
                    }

                    public LuaObject next() {
                        return get(keys.next());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Cannot change structure of underlying bean");
                    }
                };
            }

            public Object[] toArray() {
                return collect().toArray();
            }

            @SuppressWarnings({ "SuspiciousToArrayCall" })
            public <T> T[] toArray(T[] a) {
                return collect().toArray(a);
            }

            public boolean add(LuaObject o) {
                throw new UnsupportedOperationException("Cannot change structure of underlying bean");
            }

            public boolean remove(Object o) {
                throw new UnsupportedOperationException("Cannot change structure of underlying bean");
            }

            public boolean containsAll(Collection<?> c) {
                return collect().containsAll(c);
            }

            public boolean addAll(Collection<? extends LuaObject> c) {
                throw new UnsupportedOperationException("Cannot change structure of underlying bean");
            }

            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("Cannot change structure of underlying bean");
            }

            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("Cannot change structure of underlying bean");
            }

            public void clear() {
                throw new UnsupportedOperationException("Cannot change structure of underlying bean");
            }

            private Collection<LuaObject> collect() {
                Collection<LuaObject> result = new HashSet<LuaObject>();
                Map<String, Accessor> accessors = getAccessors();
                for (Accessor accessor : accessors.values()) {
                    Object value = accessor.get(bean);
                    result.add(value == null ? LuaNil.NIL : new BeanAdapter(value));
                }
                return result;
            }
        };
    }

    public Set<Entry<String, LuaObject>> entrySet() {
        Set<Entry<String, LuaObject>> result = new HashSet<Entry<String, LuaObject>>();
        for (final String key : getAccessors().keySet()) {
            result.add(new Entry<String, LuaObject>() {

                public String getKey() {
                    return key;
                }

                public LuaObject getValue() {
                    return get(key);
                }

                public LuaObject setValue(LuaObject value) {
                    return put(key, value);
                }
            });
        }
        return result;
    }

    private Map<String, Accessor> getAccessors() {
        return CACHE.get(bean.getClass());
    }

    private static class Accessor {

        private final Reference<Method> readMethod;

        private final Reference<Method> writeMethod;

        private Accessor(Method readMethod, Method writeMethod) {
            this.readMethod = new WeakReference<Method>(readMethod);
            this.writeMethod = new WeakReference<Method>(writeMethod);
        }

        public Object get(Object bean) {
            Method pinned = readMethod.get();
            if (pinned == null) return null;
            try {
                return pinned.invoke(bean);
            } catch (IllegalAccessException iae) {
                LOGGER.log(Level.WARNING, "Unable to obtain value", iae);
            } catch (InvocationTargetException ite) {
                LOGGER.log(Level.WARNING, "Unable to obtain value", ite);
            }
            return null;
        }

        public void set(Object bean, Object value) {
            Method pinned = writeMethod.get();
            if (pinned == null) return;
            try {
                pinned.invoke(bean, value);
            } catch (IllegalAccessException iae) {
                LOGGER.log(Level.WARNING, "Unable to set value", iae);
            } catch (InvocationTargetException ite) {
                LOGGER.log(Level.WARNING, "Unable to set value", ite);
            }
        }
    }
}
