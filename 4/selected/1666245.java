package jaxlib.beans;

import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import jaxlib.col.AbstractXCollection;
import jaxlib.col.AbstractXSet;
import jaxlib.col.Maps;
import jaxlib.col.XCollection;
import jaxlib.col.XMap;
import jaxlib.col.XSet;
import jaxlib.lang.Objects;
import jaxlib.util.AccessTypeSet;

/**
 * A map which associates the property names of a bean with the appertaining getters and setters.
 * <p>
 * A {@code BeanMap} is of fixed size. Its size is equal to the number of bean properties. 
 * The {@link #remove(Object)} operation is unsupported. The {@link #put(String,Object)} fails for undefined
 * and for read only properties.
 * </p><p>
 * Properties are either read only or readable and writable, but never only writable. They also never are
 * static.
 * Public bean classes and methods are supported exclusively. There is no support for indexed properties nor
 * for public fields.
 * </p><p>
 * A {@code BeanMap} is partially safe to be used by multiple threads concurrently if the underlying bean 
 * is thread safe. However, if multiple threads are modifying the same property concurrently then there is no
 * guarantee which values are delegated to the bean in which order. It also may happen that one value is
 * sent to the bean twice while another one gets skipped.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: BeanMap.java 1279 2004-10-21 21:12:05Z joerg_wassmer $
 */
public class BeanMap<B> extends Object implements XMap<String, Object>, Serializable {

    /**
   * @since JaXLib 1.0
   */
    private static final long serialVersionUID = 1L;

    private transient BeanMapSpec<B> spec;

    private transient B bean;

    private transient Object[] paramArray;

    private transient XSet<Map.Entry<String, Object>> entrySet;

    private transient XCollection<Object> values;

    /**
   * Creates a new {@code BeanMap} instance which delegates to the specified bean.
   * This constructor is being provided for convenience. Your code should hold a reference to
   * a {@link BeanMapSpec} instance created for the specified class as long as your code needs to
   * create {@code BeanMap} instances for the specified class. Otherwise the specified class will
   * have to be introspected each time a {@code BeanMap} is created for it. The introspection is
   * an expensive operation.
   *
   * @param beanClass   defines the bean properties to be made accessible through the map.
   * @param delegate    the bean to delegate to.
   *
   * @throws NullPointerException
   *  for any {@code null} argument.
   * @throws IllegalArgumentException
   *  if the specified bean object is not an instance of the specified class.
   *
   * @since JaXLib 1.0
   */
    public BeanMap(Class<B> beanClass, B delegate) {
        this(BeanMapSpec.getInstance(beanClass), delegate);
    }

    /**
   * Creates a new {@code BeanMap} instance which delegates to the specified bean.
   *
   * @param beanMapSpec defines the bean properties to be made accessible through the map.
   * @param delegate    the bean to delegate to.
   *
   * @throws NullPointerException
   *  for any {@code null} argument.
   * @throws IllegalArgumentException
   *  if the specified bean object is not an instance of the class the specified {@code beanMapSpec} was
   *  created for.
   *
   * @since JaXLib 1.0
   */
    public BeanMap(BeanMapSpec<B> beanMapSpec, B delegate) {
        super();
        if (beanMapSpec == null) throw new NullPointerException("beanMapSpec");
        if (delegate == null) throw new NullPointerException("delegate");
        if (!beanMapSpec.getBeanClass().isInstance(delegate)) {
            throw new IllegalArgumentException("specified bean is not instance of class " + beanMapSpec.getBeanClass() + ": " + delegate.getClass());
        }
        this.bean = delegate;
        this.spec = beanMapSpec;
    }

    /**
   * Creates a new {@code BeanMap} instance were the bean is identical to new {@code BeanMap} itself.
   *
   * @throws IllegalArgumentException
   *  if the new map instance is not an instance of the class the specified {@code beanMapSpec} was
   *  created for.
   *
   * @since JaXLib 1.0
   */
    protected BeanMap(BeanMapSpec<? extends BeanMap> beanMapSpec) {
        super();
        if (beanMapSpec == null) throw new NullPointerException("beanMapSpec");
        Object delegate = this;
        if (!beanMapSpec.getBeanClass().isInstance(delegate)) {
            throw new IllegalArgumentException("specified bean is not instance of class " + beanMapSpec.getBeanClass() + ": " + delegate.getClass());
        }
        this.bean = (B) this;
        this.spec = (BeanMapSpec) beanMapSpec;
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        Class beanClass = (Class) in.readObject();
        this.bean = (B) in.readObject();
        if (this.bean == null) this.bean = (B) this;
        if (beanClass == null) beanClass = this.bean.getClass();
        this.spec = (BeanMapSpec<B>) BeanMapSpec.getInstance(beanClass);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (this.spec.beanClass == this.bean.getClass()) out.writeObject(null); else out.writeObject(this.spec.beanClass);
        out.writeObject((this.bean == this) ? null : this.bean);
    }

    private Object[] paramArray() {
        if (this.paramArray == null) this.paramArray = new Object[1];
        return this.paramArray;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true; else if (!(o instanceof Map)) return false; else {
            Map<?, ?> b = (Map) o;
            if (b.size() != size()) {
                return false;
            } else {
                B bean = this.bean;
                BeanMapSpec<B> spec = this.spec;
                if (o instanceof BeanMap) {
                    BeanMap bm = (BeanMap) b;
                    if (spec.beanClass == bm.spec.beanClass) {
                        B bbean = (B) bm.bean;
                        if (bbean == bean) {
                            return true;
                        } else {
                            for (BeanMapSpec.Handler<B> h : spec.map.values()) {
                                if (!Objects.equals(h.get(bean), h.get(bbean))) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                }
                for (Map.Entry<?, ?> e : b.entrySet()) {
                    BeanMapSpec.Handler<B> h = spec.map.get(e.getKey());
                    if ((h == null) || !Objects.equals(h.get(bean), e.getValue())) return false;
                }
                return true;
            }
        }
    }

    @Override
    public int hashCode() {
        int hashcode = 0;
        B bean = this.bean;
        for (BeanMapSpec.Handler h : spec.map.values()) {
            hashcode += h.name.hashCode() ^ Objects.hashCode(h.get(bean));
        }
        return hashcode;
    }

    @Override
    public String toString() {
        int size = size();
        if (size == 0) return "{}";
        StringBuilder buf = new StringBuilder(size * 16);
        buf.append("{");
        EntryIterator it = new EntryIterator();
        boolean hasNext = it.hasNext();
        while (hasNext) {
            it.next();
            Object value = it.getValue();
            buf.append(it.getKey());
            buf.append("=");
            if (value == this) buf.append("(this Map)"); else buf.append(value);
            hasNext = it.hasNext();
            if (hasNext) buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }

    /**
   * Sets the specified bean property.
   *
   * @throws NoSuchElementException
   *  if the named property is undefined.
   * @throws IllegalArgumentException 
   *  if the specified value is not null and not an instance of the bean's property type. If the bean 
   *  property is of primitive type then the specified value has to be an instance of the appertaining 
   *  wrapper type, e.g. {@link Integer} for {@code int}.
   * @throws RuntimeException 
   *  if an exception occurs in the bean getter or setter method. The cause of the {@code RuntimeException} 
   *  is a {@link java.lang.reflect.InvocationTargetException}. The cause of  the latter exception is the 
   *  exception thrown by the bean method.
   *
   * @since JaXLib 1.0
   */
    public void set(String key, Object value) {
        this.spec.getHandlerOrFail(key).setUseParameterArray(this.bean, paramArray(), value);
    }

    public AccessTypeSet accessTypes() {
        return this.spec.readOnly ? AccessTypeSet.READ_ONLY : AccessTypeSet.READ_SET;
    }

    public final int capacity() {
        return size();
    }

    public final void clear() {
        throw new UnsupportedOperationException();
    }

    public final boolean containsKey(Object k) {
        return this.spec.map.containsKey(k);
    }

    public final boolean containsValue(Object v) {
        B bean = this.bean;
        for (BeanMapSpec.Handler h : spec.map.values()) {
            if (Objects.equals(v, h.get(bean))) return true;
        }
        return false;
    }

    public final void ensureCapacity(int minCapacity) {
    }

    public XSet<Map.Entry<String, Object>> entrySet() {
        if (this.entrySet == null) this.entrySet = new EntrySet();
        return this.entrySet;
    }

    public final int freeCapacity() {
        return 0;
    }

    public Object get(Object key) {
        BeanMapSpec.Handler<? super B> h = this.spec.map.get(key);
        return (h == null) ? null : h.get(this.bean);
    }

    /**
   * Retrives from the underlying bean the value for the named property, fails if no such property exists.
   *
   * @since JaXLib 1.0
   */
    public <V> V getProperty(String key) {
        return (V) this.spec.getHandlerOrFail(key).get(this.bean);
    }

    public Object getValueOfIdentity(Object key) {
        BeanMapSpec.Handler<? super B> h = this.spec.map.get(key);
        if ((h == null) || (h.name != key)) return null; else return h.get(this.bean);
    }

    public final boolean isEmpty() {
        return size() == 0;
    }

    public final XSet<String> keySet() {
        return this.spec.mapView.keySet();
    }

    /**
   * Sets the specified bean property and returns the former value.
   * It's recommended to call {@link #set(String,Object) set(key, value)} instead of this method as long as
   * you have no need for the former property value.
   *
   * @throws NoSuchElementException
   *  if the named property is undefined.
   * @throws IllegalArgumentException 
   *  if the specified value is not null and not an instance of the bean's property type. If the bean 
   *  property is of primitive type then the specified value has to be an instance of the appertaining 
   *  wrapper type, e.g. {@link Integer} for {@code int}.
   * @throws RuntimeException 
   *  if an exception occurs in the bean getter or setter method. The cause of the {@code RuntimeException} 
   *  is a {@link java.lang.reflect.InvocationTargetException}. The cause of  the latter exception is the 
   *  exception thrown by the bean method.
   *
   * @since JaXLib 1.0
   */
    public Object put(String key, Object value) {
        return this.spec.getHandlerOrFail(key).putUseParameterArray(this.bean, paramArray(), value);
    }

    /**
   * @see #transferFrom(Map)
   */
    public void putAll(Map<? extends String, ?> source) {
        B bean = this.bean;
        BeanMapSpec<B> spec = this.spec;
        Object[] paramArray = paramArray();
        try {
            if ((source instanceof BeanMap) && (spec.beanClass == ((BeanMap) source).spec.beanClass)) {
                B bbean = ((BeanMap<B>) source).bean;
                for (BeanMapSpec.Handler<B> h : spec.map.values()) {
                    paramArray[0] = h.get(bbean);
                    h.setUseParameterArray(bean, paramArray);
                }
            } else {
                for (Map.Entry<? extends String, ?> e : source.entrySet()) {
                    BeanMapSpec.Handler<B> h = spec.getHandlerOrFail(e.getKey());
                    paramArray[0] = e.getValue();
                    h.setUseParameterArray(bean, paramArray);
                }
            }
        } finally {
            paramArray[0] = null;
        }
    }

    public final Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public final Object removeValueOfIdentity(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
   * Transfers the properties of the specified bean to this map.
   * The specified bean has to be an instance of this map's underlying bean class.
   * Undefined and read only properties are skipped automatically.
   *
   * @since JaXLib 1.0
   */
    public void setProperties(B source) {
        B bean = this.bean;
        Object[] paramArray = paramArray();
        try {
            for (BeanMapSpec.Handler<B> h : this.spec.map.values()) {
                if (!h.isReadOnly()) {
                    paramArray[0] = h.get(source);
                    h.setUseParameterArray(bean, paramArray);
                }
            }
        } finally {
            paramArray[0] = null;
        }
    }

    /**
   * Transfers all non {@code null} properties of the specified bean to this map.
   * The specified bean has to be an instance of this map's underlying bean class.
   * Undefined, read only and properties with value {@code null} are skipped automatically.
   *
   * @since JaXLib 1.0
   */
    public void setNonNullProperties(B source) {
        B bean = this.bean;
        Object[] paramArray = paramArray();
        try {
            for (BeanMapSpec.Handler<B> h : this.spec.map.values()) {
                if (!h.isReadOnly()) {
                    Object v = h.get(source);
                    if (v != null) {
                        paramArray[0] = v;
                        h.setUseParameterArray(bean, paramArray);
                    }
                }
            }
        } finally {
            paramArray[0] = null;
        }
    }

    public final int size() {
        return this.spec.map.size();
    }

    /**
   * Copies from the specified map to this bean map all the writable properties existing in this bean map.
   * <p>
   * In difference to the {@link #putAll(Map)} method this one never fails for read only or non existing
   * properties.
   * </p>
   *
   * @since JaXLib 1.0
   */
    public void transferFrom(Map<? extends String, ?> source) {
        B bean = this.bean;
        BeanMapSpec<B> spec = this.spec;
        Object[] paramArray = paramArray();
        try {
            if ((source instanceof BeanMap) && ((BeanMap) source).spec.isSubClassOf(spec)) {
                B bbean = ((BeanMap<B>) source).bean;
                for (BeanMapSpec.Handler<B> h : spec.map.values()) {
                    if (!h.isReadOnly()) {
                        paramArray[0] = h.get(bbean);
                        h.setUseParameterArray(bean, paramArray);
                    }
                }
            } else if (source.size() <= spec.map.size() - (spec.map.size() >> 2)) {
                for (Map.Entry<? extends String, ?> e : source.entrySet()) {
                    BeanMapSpec.Handler<B> h = spec.map.get(e.getKey());
                    if ((h != null) && !h.isReadOnly()) {
                        paramArray[0] = e.getValue();
                        h.setUseParameterArray(bean, paramArray);
                    }
                }
            } else {
                for (BeanMapSpec.Handler<B> h : spec.map.values()) {
                    if (!h.isReadOnly()) {
                        Object v = source.get(h.name);
                        if ((v != null) || source.containsKey(v)) {
                            paramArray[0] = v;
                            h.setUseParameterArray(bean, paramArray);
                        }
                    }
                }
            }
        } finally {
            paramArray[0] = null;
        }
    }

    public final int trimCapacity(int newCapacity) {
        return size();
    }

    public final void trimToSize() {
    }

    public XCollection<Object> values() {
        if (this.values == null) this.values = new Values();
        return this.values;
    }

    private final class EntryIterator extends Object implements Iterator<Map.Entry<String, Object>>, Map.Entry<String, Object> {

        private final B bean = BeanMap.this.bean;

        private final Iterator<? extends BeanMapSpec.Handler<? super B>> handlers = BeanMap.this.spec.map.values().iterator();

        private BeanMapSpec.Handler<? super B> handler;

        EntryIterator() {
            super();
        }

        public boolean hasNext() {
            return this.handlers.hasNext();
        }

        public Map.Entry<String, Object> next() {
            this.handler = this.handlers.next();
            return this;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean equals(Object o) {
            if (o == this) return true; else if (this.handler == null) return false; else if (!(o instanceof Map.Entry)) return false; else {
                Map.Entry b = (Map.Entry) o;
                return Objects.equals(getKey(), b.getKey()) && Objects.equals(getValue(), b.getValue());
            }
        }

        public int hashCode() {
            if (this.handler == null) return super.hashCode(); else return Maps.entryHashCode(getKey(), getValue());
        }

        public String toString() {
            if (this.handler == null) return super.toString(); else return Maps.entryToString(getKey(), getValue());
        }

        public String getKey() {
            if (this.handler == null) throw new IllegalStateException();
            return this.handler.name;
        }

        public Object getValue() {
            if (this.handler == null) throw new IllegalStateException();
            return this.handler.get(this.bean);
        }

        public Object setValue(Object v) {
            if (this.handler == null) throw new IllegalStateException();
            return this.handler.putUseParameterArray(this.bean, BeanMap.this.paramArray(), v);
        }
    }

    private final class ValueIterator extends Object implements Iterator<Object> {

        private final B bean = BeanMap.this.bean;

        private final Iterator<? extends BeanMapSpec.Handler<? super B>> handlers = BeanMap.this.spec.map.values().iterator();

        ValueIterator() {
            super();
        }

        public boolean hasNext() {
            return this.handlers.hasNext();
        }

        public Object next() {
            return this.handlers.next().get(this.bean);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class EntrySet extends AbstractXSet<Map.Entry<String, Object>> {

        EntrySet() {
            super();
        }

        private B getBean() {
            return BeanMap.this.bean;
        }

        private BeanMapSpec<? super B> getBeanSpec() {
            return BeanMap.this.spec;
        }

        @Override
        public AccessTypeSet accessTypes() {
            return BeanMap.this.spec.readOnly ? AccessTypeSet.READ_ONLY : AccessTypeSet.READ_SET;
        }

        @Override
        public boolean add(Map.Entry<String, Object> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> b) {
            if (b == this) return true; else if (b.size() > size()) return false; else {
                if (b instanceof BeanMap.EntrySet) {
                    BeanMap.EntrySet bs = (BeanMap.EntrySet) b;
                    if ((bs.getBean() == getBean()) && getBeanSpec().isSubClassOf(bs.getBeanSpec())) return true;
                }
                return super.containsAll(b);
            }
        }

        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean remove(Object e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return BeanMap.this.size();
        }
    }

    private final class Values extends AbstractXCollection<Object> {

        Values() {
            super();
        }

        private B getBean() {
            return BeanMap.this.bean;
        }

        private BeanMapSpec<? super B> getBeanSpec() {
            return BeanMap.this.spec;
        }

        @Override
        public AccessTypeSet accessTypes() {
            return AccessTypeSet.READ_ONLY;
        }

        @Override
        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> b) {
            if (b == this) return true; else if (b.size() > size()) return false; else {
                if (b instanceof BeanMap.Values) {
                    BeanMap.Values bv = (BeanMap.Values) b;
                    if ((bv.getBean() == getBean()) && getBeanSpec().isSubClassOf(bv.getBeanSpec())) return true;
                }
                return super.containsAll(b);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true; else if ((o == null) || (o.getClass() != getClass())) return false; else {
                Values b = (Values) o;
                return (getBean() == b.getBean()) && getBeanSpec().equals(b.getBeanSpec());
            }
        }

        @Override
        public Iterator<Object> iterator() {
            return new ValueIterator();
        }

        @Override
        public boolean remove(Object e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return BeanMap.this.size();
        }
    }
}
