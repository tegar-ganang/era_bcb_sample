package com.mchange.v2.naming;

import java.beans.*;
import java.io.*;
import java.util.*;
import javax.naming.*;
import com.mchange.v2.log.*;
import java.lang.reflect.Method;
import com.mchange.v2.lang.Coerce;
import com.mchange.v2.beans.BeansUtils;
import com.mchange.v2.ser.SerializableUtils;
import com.mchange.v2.ser.IndirectPolicy;

public class JavaBeanReferenceMaker implements ReferenceMaker {

    private static final MLogger logger = MLog.getLogger(JavaBeanReferenceMaker.class);

    static final String REF_PROPS_KEY = "com.mchange.v2.naming.JavaBeanReferenceMaker.REF_PROPS_KEY";

    static final Object[] EMPTY_ARGS = new Object[0];

    static final byte[] NULL_TOKEN_BYTES = new byte[0];

    String factoryClassName = "com.mchange.v2.naming.JavaBeanObjectFactory";

    String defaultFactoryClassLocation = null;

    Set referenceProperties = new HashSet();

    ReferenceIndirector indirector = new ReferenceIndirector();

    public Hashtable getEnvironmentProperties() {
        return indirector.getEnvironmentProperties();
    }

    public void setEnvironmentProperties(Hashtable environmentProperties) {
        indirector.setEnvironmentProperties(environmentProperties);
    }

    public void setFactoryClassName(String factoryClassName) {
        this.factoryClassName = factoryClassName;
    }

    public String getFactoryClassName() {
        return factoryClassName;
    }

    public String getDefaultFactoryClassLocation() {
        return defaultFactoryClassLocation;
    }

    public void setDefaultFactoryClassLocation(String defaultFactoryClassLocation) {
        this.defaultFactoryClassLocation = defaultFactoryClassLocation;
    }

    public void addReferenceProperty(String propName) {
        referenceProperties.add(propName);
    }

    public void removeReferenceProperty(String propName) {
        referenceProperties.remove(propName);
    }

    public Reference createReference(Object bean) throws NamingException {
        try {
            BeanInfo bi = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] pds = bi.getPropertyDescriptors();
            List refAddrs = new ArrayList();
            String factoryClassLocation = defaultFactoryClassLocation;
            boolean using_ref_props = referenceProperties.size() > 0;
            if (using_ref_props) refAddrs.add(new BinaryRefAddr(REF_PROPS_KEY, SerializableUtils.toByteArray(referenceProperties)));
            for (int i = 0, len = pds.length; i < len; ++i) {
                PropertyDescriptor pd = pds[i];
                String propertyName = pd.getName();
                if (using_ref_props && !referenceProperties.contains(propertyName)) {
                    continue;
                }
                Class propertyType = pd.getPropertyType();
                Method getter = pd.getReadMethod();
                Method setter = pd.getWriteMethod();
                if (getter != null && setter != null) {
                    Object val = getter.invoke(bean, EMPTY_ARGS);
                    if (propertyName.equals("factoryClassLocation")) {
                        if (String.class != propertyType) throw new NamingException(this.getClass().getName() + " requires a factoryClassLocation property to be a string, " + propertyType.getName() + " is not valid.");
                        factoryClassLocation = (String) val;
                    }
                    if (val == null) {
                        RefAddr addMe = new BinaryRefAddr(propertyName, NULL_TOKEN_BYTES);
                        refAddrs.add(addMe);
                    } else if (Coerce.canCoerce(propertyType)) {
                        RefAddr addMe = new StringRefAddr(propertyName, String.valueOf(val));
                        refAddrs.add(addMe);
                    } else {
                        RefAddr addMe = null;
                        PropertyEditor pe = BeansUtils.findPropertyEditor(pd);
                        if (pe != null) {
                            pe.setValue(val);
                            String textValue = pe.getAsText();
                            if (textValue != null) addMe = new StringRefAddr(propertyName, textValue);
                        }
                        if (addMe == null) addMe = new BinaryRefAddr(propertyName, SerializableUtils.toByteArray(val, indirector, IndirectPolicy.INDIRECT_ON_EXCEPTION));
                        refAddrs.add(addMe);
                    }
                } else {
                    if (logger.isLoggable(MLevel.WARNING)) logger.warning(this.getClass().getName() + ": Skipping " + propertyName + " because it is " + (setter == null ? "read-only." : "write-only."));
                }
            }
            Reference out = new Reference(bean.getClass().getName(), factoryClassName, factoryClassLocation);
            for (Iterator ii = refAddrs.iterator(); ii.hasNext(); ) out.add((RefAddr) ii.next());
            return out;
        } catch (Exception e) {
            if (Debug.DEBUG && logger.isLoggable(MLevel.FINE)) logger.log(MLevel.FINE, "Exception trying to create Reference.", e);
            throw new NamingException("Could not create reference from bean: " + e.toString());
        }
    }
}
