package jxl.vague;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import jxl.binding.BindingContext;
import jxl.search.SearchUtils;
import jxl.search.Searchable;
import jxl.util.NamingUtils;
import jxl.util.SoftSet;
import jxl.util.SoftValueHashMap;

/**
 * VagueContext provides a means of peristing simple or nested bean property data, 
 * without creating a bean implementation.
 * <P>
 * All VagueContexts are paramaterized with a sub-interface of VagueObject.
 * VagueContext uses this type to create and return implementation proxies to 
 * serve as a mediator between java bean property pattersn and persistent storage.
 * </p>
 * @author Alex Lynch
 * @see jxl.vague.VagueObject
 */
public final class VagueContext<V extends VagueObject> implements Searchable<V> {

    private static SoftValueHashMap<Name, VagueContext<?>> cache = new SoftValueHashMap<Name, VagueContext<?>>();

    /**
     * Checks if <CODE>type</CODE> is a correctly formed VagueObject.
     * @param type the VagueObject to check.
     * @throws java.lang.IllegalArgumentException if <CODE>type</CODE> is not correctly formed. The exception will contain information
     * about the problem.
     * @throws java.beans.IntrospectionException if a problem occurs during bean introspection.
     * @see jxl.vague.VagueObject
     */
    public static <T extends VagueObject> void checkVagueType(Class<T> type) throws IllegalArgumentException, IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(type);
        Vector<Method> methods = new Vector<Method>(Arrays.asList(type.getMethods()));
        List<PropertyDescriptor> props = Arrays.asList(info.getPropertyDescriptors());
        for (PropertyDescriptor desc : props) {
            Class propType = desc.getPropertyType();
            if (!Serializable.class.isAssignableFrom(propType) && !propType.isPrimitive()) {
                throw new IllegalArgumentException("Property " + desc.getName() + " must be of a primitive or " + Serializable.class.getName() + " type.");
            } else if (!VagueObject.class.isAssignableFrom(propType)) {
                Method r = desc.getReadMethod();
                Method w = desc.getWriteMethod();
                if (r == null || w == null) {
                    throw new IllegalArgumentException("Property " + desc.getName() + " must be read/write (i.e. both getX and setX)");
                } else {
                    methods.remove(r);
                    methods.remove(w);
                }
            } else {
                if (desc.getWriteMethod() != null) {
                    throw new IllegalArgumentException("Property " + desc.getName() + " must be read-only (i.e. getX, but no setX)");
                } else {
                    checkVagueType(propType);
                }
                Method r = desc.getReadMethod();
                methods.remove(r);
            }
            methods.remove(desc);
        }
        if (!methods.isEmpty()) {
            Method m = methods.iterator().next();
            throw new IllegalArgumentException("Illegal method: " + m.getName() + ". All methods must be read/write (getX and setX) of a sub-type of " + Serializable.class.getName() + " or read-only of sub-type " + VagueObject.class.getName());
        }
    }

    public static <T extends VagueObject> VagueContext<T> getContext(Name context, Class<T> vagueType, Permission check) throws VagueException {
        VagueContext<T> v = (VagueContext<T>) cache.get(context);
        if (v == null) {
            v = new VagueContext<T>(context, vagueType, check);
            cache.put(context, v);
        } else {
            v.myContext.checkPermission();
        }
        return v;
    }

    private final Permission myCheck;

    private final BindingContext myContext;

    private final Name myNameContext;

    private final Class<V> myType;

    private final SoftSet<V> myCache;

    /** Creates a new instance of VagueContext */
    private VagueContext(Name context, Class<V> vagueType, Permission check) throws VagueException {
        try {
            checkVagueType(vagueType);
            Name clone = new CompositeName("Vague").addAll((Name) context.clone());
            myNameContext = clone.addAll(NamingUtils.asName(vagueType));
            myCheck = check;
            myContext = BindingContext.getContext(myNameContext, check);
            myType = vagueType;
            myCache = new SoftSet<V>();
        } catch (IntrospectionException ie) {
            throw new VagueException(ie);
        } catch (IllegalArgumentException iae) {
            throw new VagueException(iae);
        } catch (InvalidNameException ine) {
            throw new VagueException(ine);
        }
    }

    public V newObject() {
        return makeProxy(myContext.getSubcontext(myContext.nextUniqueSubcontext()));
    }

    protected V makeProxy(String binding) {
        return makeProxy(myContext.getSubcontext(binding));
    }

    protected String netUniqueBinding() {
        return myContext.nextUniqueSubcontext();
    }

    private V makeProxy(BindingContext context) {
        return myType.cast(Proxy.newProxyInstance(myType.getClassLoader(), new Class[] { myType }, new VagueInvocationHandler(myNameContext, context, myCheck)));
    }

    public V findObject(jxl.search.Search<V> s) {
        Collection<V> c = findSomeObjects(SearchUtils.asSingleLimited(s));
        if (c.isEmpty()) {
            return null;
        } else {
            return c.iterator().next();
        }
    }

    public Collection<V> findAllObjects() {
        return findObjects(SearchUtils.asTypeOnlySearch(myType));
    }

    public java.util.Collection<V> findObjects(jxl.search.Search<V> s) {
        return findSomeObjects(SearchUtils.asUnlimitedSearch(s));
    }

    public java.util.Collection<V> findSomeObjects(jxl.search.LimitedSearch<V> ls) {
        Vector<V> v = new Vector<V>();
        for (V cached : myCache) {
            if (ls.searchFarther()) {
                if (ls.matchesSearch(cached)) {
                    v.add(cached);
                }
            }
        }
        for (String binding : myContext.listSubcontexts()) {
            if (ls.searchFarther()) {
                V proxy = makeProxy(binding);
                if (ls.matchesSearch(proxy)) {
                    v.add(proxy);
                    myCache.add(proxy);
                }
            } else {
                break;
            }
        }
        return v;
    }

    public int removeObjects(jxl.search.Search<V> s) {
        int removed = 0;
        for (Iterator<V> i = myCache.iterator(); i.hasNext(); ) {
            V v = i.next();
            if (s.matchesSearch(v)) {
                i.remove();
            }
        }
        for (String binding : myContext.listSubcontexts()) {
            V proxy = makeProxy(binding);
            if (s.matchesSearch(proxy)) {
                myContext.destroySubcontext(binding);
                removed++;
            }
        }
        return removed;
    }

    public int hashCode() {
        return myContext.hashCode();
    }

    public boolean equals(Object o) {
        if (this.getClass().isInstance(o)) {
            return hashCode() == o.hashCode();
        }
        return false;
    }
}
