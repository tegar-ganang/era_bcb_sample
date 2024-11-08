package org.zkforge.apache.commons.el;

import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import org.zkoss.xel.XelException;

/**
 *
 * <p>Manages the BeanInfo for one class - contains the BeanInfo, and
 * also a mapping from property name to BeanInfoProperty.  There are
 * also static methods for accessing the BeanInfoManager for a class -
 * those mappings are cached permanently so that once the
 * BeanInfoManager is calculated, it doesn't have to be calculated
 * again.
 * 
 * @author Nathan Abramson - Art Technology Group
 * @version $Change: 181181 $$DateTime: 2001/06/26 09:55:09 $$Author: luehe $
 **/
public class BeanInfoManager {

    Class mBeanClass;

    public Class getBeanClass() {
        return mBeanClass;
    }

    BeanInfo mBeanInfo;

    Map mPropertyByName;

    Map mIndexedPropertyByName;

    Map mEventSetByName;

    boolean mInitialized;

    static Map mBeanInfoManagerByClass = new HashMap();

    /**
   *
   * Constructor
   **/
    BeanInfoManager(Class pBeanClass) {
        mBeanClass = pBeanClass;
    }

    /**
   *
   * Returns the BeanInfoManager for the specified class
   **/
    public static BeanInfoManager getBeanInfoManager(Class pClass) {
        BeanInfoManager ret = (BeanInfoManager) mBeanInfoManagerByClass.get(pClass);
        if (ret == null) {
            ret = createBeanInfoManager(pClass);
        }
        return ret;
    }

    /**
   *
   * Creates and registers the BeanInfoManager for the given class if
   * it isn't already registered.
   **/
    static synchronized BeanInfoManager createBeanInfoManager(Class pClass) {
        BeanInfoManager ret = (BeanInfoManager) mBeanInfoManagerByClass.get(pClass);
        if (ret == null) {
            ret = new BeanInfoManager(pClass);
            mBeanInfoManagerByClass.put(pClass, ret);
        }
        return ret;
    }

    /**
   *
   * Returns the BeanInfoProperty for the specified property in the
   * given class, or null if not found.
   **/
    public static BeanInfoProperty getBeanInfoProperty(Class pClass, String pPropertyName, Logger pLogger) throws XelException {
        return getBeanInfoManager(pClass).getProperty(pPropertyName, pLogger);
    }

    /**
   *
   * Returns the BeanInfoIndexedProperty for the specified property in
   * the given class, or null if not found.
   **/
    public static BeanInfoIndexedProperty getBeanInfoIndexedProperty(Class pClass, String pIndexedPropertyName, Logger pLogger) throws XelException {
        return getBeanInfoManager(pClass).getIndexedProperty(pIndexedPropertyName, pLogger);
    }

    /**
   *
   * Makes sure that this class has been initialized, and synchronizes
   * the initialization if it's required.
   **/
    void checkInitialized(Logger pLogger) throws XelException {
        if (!mInitialized) {
            synchronized (this) {
                if (!mInitialized) {
                    initialize(pLogger);
                    mInitialized = true;
                }
            }
        }
    }

    /**
   *
   * Initializes by mapping property names to BeanInfoProperties
   **/
    void initialize(Logger pLogger) throws XelException {
        try {
            mBeanInfo = Introspector.getBeanInfo(mBeanClass);
            mPropertyByName = new HashMap();
            mIndexedPropertyByName = new HashMap();
            PropertyDescriptor[] pds = mBeanInfo.getPropertyDescriptors();
            for (int i = 0; pds != null && i < pds.length; i++) {
                PropertyDescriptor pd = pds[i];
                if (pd instanceof IndexedPropertyDescriptor) {
                    IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
                    Method readMethod = getPublicMethod(ipd.getIndexedReadMethod());
                    Method writeMethod = getPublicMethod(ipd.getIndexedWriteMethod());
                    BeanInfoIndexedProperty property = new BeanInfoIndexedProperty(readMethod, writeMethod, ipd);
                    mIndexedPropertyByName.put(ipd.getName(), property);
                }
                Method readMethod = getPublicMethod(pd.getReadMethod());
                Method writeMethod = getPublicMethod(pd.getWriteMethod());
                BeanInfoProperty property = new BeanInfoProperty(readMethod, writeMethod, pd);
                mPropertyByName.put(pd.getName(), property);
            }
            mEventSetByName = new HashMap();
            EventSetDescriptor[] esds = mBeanInfo.getEventSetDescriptors();
            for (int i = 0; esds != null && i < esds.length; i++) {
                EventSetDescriptor esd = esds[i];
                mEventSetByName.put(esd.getName(), esd);
            }
        } catch (IntrospectionException exc) {
            if (pLogger.isLoggingWarning()) {
                pLogger.logWarning(Constants.EXCEPTION_GETTING_BEANINFO, exc, mBeanClass.getName());
            }
        }
    }

    /**
   *
   * Returns the BeanInfo for the class
   **/
    BeanInfo getBeanInfo(Logger pLogger) throws XelException {
        checkInitialized(pLogger);
        return mBeanInfo;
    }

    /**
   *
   * Returns the BeanInfoProperty for the given property name, or null
   * if not found.
   **/
    public BeanInfoProperty getProperty(String pPropertyName, Logger pLogger) throws XelException {
        checkInitialized(pLogger);
        return (BeanInfoProperty) mPropertyByName.get(pPropertyName);
    }

    /**
   *
   * Returns the BeanInfoIndexedProperty for the given property name,
   * or null if not found.
   **/
    public BeanInfoIndexedProperty getIndexedProperty(String pIndexedPropertyName, Logger pLogger) throws XelException {
        checkInitialized(pLogger);
        return (BeanInfoIndexedProperty) mIndexedPropertyByName.get(pIndexedPropertyName);
    }

    /**
   *
   * Returns the EventSetDescriptor for the given event set name, or
   * null if not found.
   **/
    public EventSetDescriptor getEventSet(String pEventSetName, Logger pLogger) throws XelException {
        checkInitialized(pLogger);
        return (EventSetDescriptor) mEventSetByName.get(pEventSetName);
    }

    /**
   *
   * Returns a publicly-accessible version of the given method, by
   * searching for a public declaring class.
   **/
    static Method getPublicMethod(Method pMethod) {
        if (pMethod == null) {
            return null;
        }
        Class cl = pMethod.getDeclaringClass();
        if (Modifier.isPublic(cl.getModifiers())) {
            return pMethod;
        }
        Method ret = getPublicMethod(cl, pMethod);
        if (ret != null) {
            return ret;
        } else {
            return pMethod;
        }
    }

    /**
   *
   * If the given class is public and has a Method that declares the
   * same name and arguments as the given method, then that method is
   * returned.  Otherwise the superclass and interfaces are searched
   * recursively.
   **/
    static Method getPublicMethod(Class pClass, Method pMethod) {
        if (Modifier.isPublic(pClass.getModifiers())) {
            try {
                Method m;
                try {
                    m = pClass.getDeclaredMethod(pMethod.getName(), pMethod.getParameterTypes());
                } catch (java.security.AccessControlException ex) {
                    m = pClass.getMethod(pMethod.getName(), pMethod.getParameterTypes());
                }
                if (Modifier.isPublic(m.getModifiers())) {
                    return m;
                }
            } catch (NoSuchMethodException exc) {
            }
        }
        {
            Class[] interfaces = pClass.getInterfaces();
            if (interfaces != null) {
                for (int i = 0; i < interfaces.length; i++) {
                    Method m = getPublicMethod(interfaces[i], pMethod);
                    if (m != null) {
                        return m;
                    }
                }
            }
        }
        {
            Class superclass = pClass.getSuperclass();
            if (superclass != null) {
                Method m = getPublicMethod(superclass, pMethod);
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }
}
