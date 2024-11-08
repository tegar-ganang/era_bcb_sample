package org.formaria.dto;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.HashMap;
import java.lang.reflect.Method;

/**
 * The factory class that creates a Transfer Object for a
 * given domain object. The class stores information about the object and can
 * create a transfer object based upon the Dto annotations.
 * <p>Copyright (c) Formaria Ltd., 2008, see license.txt for license information<br>
 * @author Luan O'Carroll
 */
public class DtoFactory {

    private DtoClassGenerator dtoClassGenerator;

    /**
   * Use a HashMap to cache class information for
   * Transfer Object classes
   */
    private HashMap<Class, ClassData> classDataInfo = new HashMap<Class, ClassData>();

    /**
   * 
   * @param rootClassPath teh path to the root of the class path on the file 
   * system. generated files will be output relative to this folder
   */
    private DtoFactory() {
        dtoClassGenerator = new DtoClassGenerator();
    }

    public void setBuildPath(String buildPath) {
        dtoClassGenerator.setBuildPath(buildPath);
    }

    /**
   * Create a Transfer Object for the given object. The
   * given object must be an EJB Implementation and have
   * a superclass that acts as the class for the entity's
   * Transfer Object. Only the fields defined in this
   * superclass are copied in to the Transfer Object.
   * @param deepCopy get a deep copy of the DTO, including DTO versions of the 
   * nested objects
   */
    public Serializable createTransferObject(Object domainObject, boolean deepCopy) {
        try {
            DtoState dtoState = new DtoState();
            dtoState.domainClass = domainObject.getClass();
            dtoState.dtoClass = dtoClassGenerator.getDtoClass(dtoState.domainClass, deepCopy);
            if (dtoState.dtoClass == null) return null;
            dtoState.classData = getClassData(dtoState.domainClass, dtoState.dtoClass);
            Object dto = dtoState.dtoClass.newInstance();
            updateObject(dtoState, domainObject, dto, true, deepCopy);
            return (java.io.Serializable) dto;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
   * Read the data from the transfer object and save it to the original domain 
   * object
   * @param domainObject
   * @param transferObject
   * @param deepCopy get a deep copy of the DTO, including DTO versions of the 
   * nested objects
   */
    public void saveTransferObject(Object domainObject, Object transferObject, boolean deepCopy) {
        DtoState dtoState = new DtoState();
        dtoState.domainClass = domainObject.getClass();
        dtoState.dtoClass = transferObject.getClass();
        dtoState.classData = getClassData(dtoState.domainClass, dtoState.dtoClass);
        updateObject(dtoState, domainObject, transferObject, false, deepCopy);
    }

    /**
   * Copy data from one object to the other. Only copy those fields with accessors
   * in both classes
   * @param deepCopy get a deep copy of the DTO, including DTO versions of the 
   * nested objects
   */
    private void updateObject(DtoState dtoState, Object sourceObject, Object targetObject, boolean updateTarget, boolean deepCopy) {
        try {
            if (dtoState.classData == null) {
                dtoState.classData = getClassData(dtoState.domainClass, dtoState.dtoClass);
                if (dtoState.dtoClass == null) dtoState.dtoClass = dtoClassGenerator.getDtoClass(dtoState.domainClass, deepCopy);
            }
            for (int i = 0; i < dtoState.classData.targetProperties.length; i++) {
                PropertyDescriptor sourceProperty = dtoState.classData.sourceProperties[i];
                PropertyDescriptor targetProperty = dtoState.classData.targetProperties[i];
                try {
                    if ((sourceProperty == null) || (targetProperty == null)) continue;
                    Method readMethod = updateTarget ? sourceProperty.getReadMethod() : targetProperty.getReadMethod();
                    Method writeMethod = updateTarget ? targetProperty.getWriteMethod() : sourceProperty.getWriteMethod();
                    if ((readMethod == null) || (writeMethod == null)) continue;
                    Object[] readParams = new Object[0];
                    Object value = readMethod.invoke(updateTarget ? sourceObject : targetObject, readParams);
                    if (value instanceof DtoIdentifier) {
                        if (!deepCopy) value = ((DtoIdentifier) value).getId(); else {
                            DtoState dtoFieldState = new DtoState();
                            dtoFieldState.domainClass = value.getClass();
                            dtoFieldState.dtoClass = dtoClassGenerator.getDtoClass(dtoFieldState.domainClass, deepCopy);
                            dtoFieldState.classData = getClassData(dtoFieldState.domainClass, dtoFieldState.dtoClass);
                            Object dto = dtoFieldState.dtoClass.newInstance();
                            updateObject(dtoFieldState, value, dto, true, deepCopy);
                            value = dto;
                        }
                    }
                    Object[] writeParams = new Object[1];
                    writeParams[0] = value;
                    writeMethod.invoke(updateTarget ? targetObject : sourceObject, writeParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
   * Return a ClassData object that contains the 
   * information needed to create
   * a Transfer Object for the given class. This information
   * is only obtained from the
   * class using reflection once, after that it will be 
   * obtained from the classDataInfo HashMap.
   */
    private ClassData getClassData(Class sourceClass, Class dtoClass) {
        ClassData cData = classDataInfo.get(sourceClass);
        try {
            if (cData == null) {
                cData = new ClassData(sourceClass, dtoClass);
                classDataInfo.put(sourceClass, cData);
            }
        } catch (Exception e) {
        }
        return cData;
    }

    private class DtoState {

        public ClassData classData;

        public Class domainClass;

        public Class dtoClass;
    }
}

/**
 * Inner Class that contains class data for the
 * Transfer Object classes
 */
class ClassData {

    public Class sourceClass, targetClass;

    public PropertyDescriptor[] targetProperties;

    public PropertyDescriptor[] sourceProperties;

    public ClassData(Class sourceCls, Class targetCls) throws IntrospectionException {
        sourceClass = sourceCls;
        targetClass = targetCls;
        BeanInfo dtoInfo = Introspector.getBeanInfo(targetCls);
        targetProperties = dtoInfo.getPropertyDescriptors();
        sourceProperties = new PropertyDescriptor[targetProperties.length];
        BeanInfo sourceInfo = Introspector.getBeanInfo(sourceClass);
        PropertyDescriptor[] originalProps = sourceInfo.getPropertyDescriptors();
        for (int i = 0; i < targetProperties.length; i++) {
            String pName = targetProperties[i].getName();
            if (pName.equals("class")) continue;
            for (PropertyDescriptor pd : originalProps) {
                if (pd.getName().equals(pName)) {
                    sourceProperties[i] = pd;
                    break;
                }
            }
        }
    }
}
