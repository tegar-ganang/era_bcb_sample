package org.cantaloop.tools.reflection;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.MethodDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import org.cantaloop.tools.misc.BugException;

class BeanInvocationHandler implements InvocationHandler {

    public static final boolean DEBUG = false;

    private static class Property {

        public String name;

        public Class type;

        public Object value = null;

        public Method readMethod = null;

        public Method writeMethod = null;

        public String toString() {
            return name + "='" + value + "'";
        }
    }

    private Map m_properties;

    private Map m_readMethodToPropertyMap;

    private Map m_writeMethodToPropertyMap;

    private Map m_ifaceMethodToSubjectMap;

    private Set m_subjectImplementedMethods;

    private Set m_ifaceSet;

    private Object m_subject;

    private boolean m_fallback;

    public BeanInvocationHandler(Object subject, Class[] ifaceArray, boolean fallback) {
        m_fallback = fallback;
        m_subject = subject;
        m_ifaceSet = new HashSet();
        m_properties = new HashMap();
        m_readMethodToPropertyMap = new HashMap();
        m_writeMethodToPropertyMap = new HashMap();
        for (int i = 0; i < ifaceArray.length; i++) {
            m_ifaceSet.addAll(ReflectionHelper.getAllInterfacesAsSet(ifaceArray[i]));
        }
        m_ifaceMethodToSubjectMap = (subject != null) ? ReflectionHelper.mapInterfacesToClass(subject.getClass(), m_ifaceSet) : Collections.EMPTY_MAP;
        Set methodsToBeSupported = ReflectionHelper.getAllDeclaredMethods(m_ifaceSet);
        m_subjectImplementedMethods = new HashSet();
        for (Iterator i = m_ifaceSet.iterator(); i.hasNext(); ) {
            Class iface = (Class) i.next();
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(iface);
                PropertyDescriptor[] pdscr = beanInfo.getPropertyDescriptors();
                for (int j = 0; j < pdscr.length; j++) {
                    if (pdscr[j] instanceof IndexedPropertyDescriptor) {
                        if (subject == null) {
                            throw new ProxyInstantiationException(ProxyUtils.INDEXED_PROPERTIES_NOT_SUPPORTED, "Property '" + pdscr[j].getName() + "' is indexed.");
                        } else {
                            checkMethodExistence(pdscr[j].getReadMethod());
                            checkMethodExistence(pdscr[j].getWriteMethod());
                            checkMethodExistence(((IndexedPropertyDescriptor) pdscr[j]).getIndexedReadMethod());
                            checkMethodExistence(((IndexedPropertyDescriptor) pdscr[j]).getIndexedWriteMethod());
                        }
                    }
                    String propertyName = pdscr[j].getName();
                    Class propertyType = pdscr[j].getPropertyType();
                    Method readMethod = pdscr[j].getReadMethod();
                    Method writeMethod = pdscr[j].getWriteMethod();
                    Property p = (Property) m_properties.get(propertyName);
                    if (p != null && !p.type.equals(propertyType)) {
                        if (subject == null) {
                            debug();
                            throw new ProxyInstantiationException(ProxyUtils.GETSET_TYPE_MISMATCH, "Type for property '" + propertyName + "' " + "ambiguous: " + p.type + " <-> " + propertyType + ".");
                        } else {
                            Method rm = (readMethod != null) ? readMethod : p.readMethod;
                            if (rm == null) {
                                if (m_ifaceMethodToSubjectMap.get(p.writeMethod) != null) {
                                    p.type = propertyType;
                                    methodsToBeSupported.remove(p.writeMethod);
                                    m_subjectImplementedMethods.add(p.writeMethod);
                                    p.writeMethod = writeMethod;
                                } else if (m_ifaceMethodToSubjectMap.get(writeMethod) != null) {
                                    methodsToBeSupported.remove(writeMethod);
                                    m_subjectImplementedMethods.add(writeMethod);
                                } else {
                                    throw new ProxyInstantiationException(ProxyUtils.METHOD_NOT_SUPPORTED, "To set methods for the same property " + "exist but none is implemented by the " + "subject. First method: '" + writeMethod + "', second method: '" + p.writeMethod + "'");
                                }
                            } else {
                                if (readMethod != null && p.readMethod != null) {
                                    throw new ProxyInstantiationException(ProxyUtils.CONFLICTING_INTERFACES, "Two methods with the same name and " + "different return types exist: " + "First method: '" + p.readMethod + "' " + "second method: '" + readMethod + "'.");
                                }
                                if (rm == readMethod) {
                                    if (m_ifaceMethodToSubjectMap.get(p.writeMethod) == null) {
                                        throw new ProxyInstantiationException(ProxyUtils.METHOD_NOT_SUPPORTED, "Expected subject to implement " + "method '" + p.writeMethod + "' as " + "it cannot be implemented by the " + "handler because a get method '" + readMethod + "' with a different type " + "exists.");
                                    } else {
                                        methodsToBeSupported.remove(p.writeMethod);
                                        m_subjectImplementedMethods.add(p.writeMethod);
                                        p.writeMethod = null;
                                        p.readMethod = readMethod;
                                        p.type = propertyType;
                                    }
                                } else {
                                    if (m_ifaceMethodToSubjectMap.get(writeMethod) == null) {
                                        throw new ProxyInstantiationException(ProxyUtils.METHOD_NOT_SUPPORTED, "Expected subject to implement " + "method '" + writeMethod + "' as it " + "cannot be implemented by the handler" + " because a get method '" + p.readMethod + "' with a different type exists.");
                                    } else {
                                        methodsToBeSupported.remove(writeMethod);
                                        m_subjectImplementedMethods.add(writeMethod);
                                    }
                                }
                            }
                        }
                    } else {
                        if (p == null) {
                            p = new Property();
                            p.name = propertyName;
                            p.type = propertyType;
                            m_properties.put(p.name, p);
                        }
                        if (readMethod != null) p.readMethod = readMethod;
                        if (writeMethod != null) p.writeMethod = writeMethod;
                    }
                }
            } catch (IntrospectionException e) {
                debug(e);
                throw new ProxyInstantiationException("Introspection of " + iface + " failed: " + e.getMessage());
            }
        }
        for (Iterator i = m_properties.values().iterator(); i.hasNext(); ) {
            Property p = (Property) i.next();
            Method subjReadMethod = (Method) m_ifaceMethodToSubjectMap.get(p.readMethod);
            Method subjWriteMethod = (Method) m_ifaceMethodToSubjectMap.get(p.writeMethod);
            if (p.readMethod != null && p.writeMethod != null) {
                if (!m_fallback || m_subject == null || subjReadMethod == null || subjWriteMethod == null) {
                    methodsToBeSupported.remove(p.readMethod);
                    methodsToBeSupported.remove(p.writeMethod);
                    m_readMethodToPropertyMap.put(p.readMethod, p);
                    m_writeMethodToPropertyMap.put(p.writeMethod, p);
                } else {
                    i.remove();
                    methodsToBeSupported.remove(p.readMethod);
                    m_subjectImplementedMethods.add(p.readMethod);
                    methodsToBeSupported.remove(p.writeMethod);
                    m_subjectImplementedMethods.add(p.writeMethod);
                }
            } else {
                i.remove();
                if (p.readMethod != null) {
                    if (subjReadMethod == null) {
                        throw new ProxyInstantiationException(ProxyUtils.METHOD_NOT_SUPPORTED, "Read method '" + p.readMethod + "' required but " + "cannot be implemented by proxy as it never " + "could be set as no corresponding set method " + "exists in any interface and subject (if " + "existent) doesn't implement method either.");
                    } else {
                        methodsToBeSupported.remove(p.readMethod);
                        m_subjectImplementedMethods.add(p.readMethod);
                    }
                }
                if (p.writeMethod != null) {
                    if (subjWriteMethod == null) {
                        throw new ProxyInstantiationException(ProxyUtils.METHOD_NOT_SUPPORTED, "Write method '" + p.writeMethod + "' required but " + "cannot be implemented by proxy as it never " + "could be get as no corresponding get method " + "exists in any interface and subject (if " + "existent) doesn't implement method either.");
                    } else {
                        methodsToBeSupported.remove(p.writeMethod);
                        m_subjectImplementedMethods.add(p.writeMethod);
                    }
                }
            }
        }
        for (Iterator i = methodsToBeSupported.iterator(); i.hasNext(); ) {
            Method m = (Method) i.next();
            if (m_ifaceMethodToSubjectMap.get(m) == null) {
                throw new ProxyInstantiationException(ProxyUtils.METHOD_NOT_SUPPORTED, "Non bean accessor/mutator method '" + m + "' required " + "but not implemented by subject (if existent).");
            } else {
                m_subjectImplementedMethods.add(m);
            }
        }
        for (Iterator i = m_subjectImplementedMethods.iterator(); i.hasNext(); ) {
            Method m = (Method) i.next();
            if (!m.isAccessible()) {
                try {
                    m.setAccessible(true);
                } catch (SecurityException e) {
                    throw new ProxyInstantiationException(ProxyUtils.FALLBACK_ACCESS_IMPOSSIBLE, "Cannot call method '" + m + "' as it is not " + "accessible: " + e.getMessage());
                }
            }
        }
        debug();
    }

    private void debug() {
        debug(null);
    }

    private void debug(Exception ex) {
        if (!DEBUG) return;
        System.err.println("\n=============================================================================");
        System.err.println("============   BeanImplementationInvocationHandler DEBUG OUTPUT  ============");
        System.err.print("\n Subject: <" + m_subject + ">");
        if (m_subject != null) System.err.print(" of class <" + m_subject.getClass().getName() + ">");
        System.err.println("\n");
        System.err.println(" Interfaces:");
        for (Iterator i = m_ifaceSet.iterator(); i.hasNext(); ) {
            Class iface = (Class) i.next();
            System.err.println("  - " + iface);
        }
        System.err.println();
        if (ex != null) {
            System.err.println(" Exception encountered:\n");
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            System.err.println();
        }
        System.err.println(" Accumulated properties:");
        for (Iterator i = m_properties.values().iterator(); i.hasNext(); ) {
            Property p = (Property) i.next();
            System.err.println("\n\n --> property");
            System.err.println("  name = " + p.name);
            System.err.println("  type = " + p.type);
            System.err.println("\n  readable by:");
            for (Iterator j = m_readMethodToPropertyMap.entrySet().iterator(); j.hasNext(); ) {
                Map.Entry e = (Map.Entry) j.next();
                if (e.getValue() == p) {
                    System.err.println("   - " + e.getKey());
                }
            }
            System.err.println("\n  writable by:");
            for (Iterator j = m_writeMethodToPropertyMap.entrySet().iterator(); j.hasNext(); ) {
                Map.Entry e = (Map.Entry) j.next();
                if (e.getValue() == p) {
                    System.err.println("   - " + e.getKey());
                }
            }
        }
        if (m_subject != null) {
            System.err.println("\n Methods implemented by subject:");
            for (Iterator i = m_subjectImplementedMethods.iterator(); i.hasNext(); ) {
                Method m = (Method) i.next();
                System.err.println("  - " + m);
                System.err.println("    maps to subject method: " + m_ifaceMethodToSubjectMap.get(m));
            }
        }
        System.err.println("\n=============================================================================\n");
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (ReflectionHelper.isMethod(method, "toString", String.class, new Class[0])) {
            StringBuffer sb = new StringBuffer();
            sb.append("[BeanProxy: ");
            if (m_subject != null) {
                sb.append((String) method.invoke(m_subject, args));
                sb.append(", ");
            }
            sb.append("properties=");
            sb.append(m_properties.values().toString());
            sb.append("]");
            return sb.toString();
        }
        if (m_subjectImplementedMethods.contains(method)) {
            Method subjectMethod = (Method) m_ifaceMethodToSubjectMap.get(method);
            return subjectMethod.invoke(m_subject, args);
        } else {
            Property p;
            p = (Property) m_readMethodToPropertyMap.get(method);
            if (p != null) {
                Object result = (p.value != null) ? p.value : s_defaultValueMap.get(p.type);
                return result;
            }
            p = (Property) m_writeMethodToPropertyMap.get(method);
            if (p != null) {
                p.value = args[0];
                return null;
            }
            System.err.println("\n\n**********************************************************************");
            System.err.println(" B U G - R E P O R T   I N F O R M A T I O N");
            System.err.println("**********************************************************************");
            debug();
            System.err.println("**********************************************************************\n\n");
            throw new BugException("BeanInvocationHandler initialisation buggy. Method '" + method + "' neither in m_subjectImplementedMethods set (" + m_subjectImplementedMethods + ") nor exists a mapping " + "in m_writeMethodToPropertyMap (" + m_writeMethodToPropertyMap + ") or " + "m_readMethodToPropertyMap (" + m_readMethodToPropertyMap + ").");
        }
    }

    private static final Map s_defaultValueMap = new HashMap();

    static {
        s_defaultValueMap.put(Integer.TYPE, new Integer(0));
        s_defaultValueMap.put(Short.TYPE, new Short((short) 0));
        s_defaultValueMap.put(Long.TYPE, new Long(0));
        s_defaultValueMap.put(Boolean.TYPE, new Boolean(false));
        s_defaultValueMap.put(Byte.TYPE, new Byte((byte) 0));
        s_defaultValueMap.put(Character.TYPE, new Character((char) 0));
        s_defaultValueMap.put(Float.TYPE, new Float(0));
        s_defaultValueMap.put(Double.TYPE, new Double(0));
    }

    private void checkMethodExistence(Method m) {
        if (m_ifaceMethodToSubjectMap.get(m) == null) {
            throw new ProxyInstantiationException(ProxyUtils.METHOD_NOT_SUPPORTED, "Expected subject (if existent) to implement method '" + m + ".");
        }
    }
}
