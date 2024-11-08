package org.dozer.propertydescriptor;

import org.dozer.MappingException;
import org.dozer.factory.BeanCreationDirective;
import org.dozer.factory.DestBeanCreator;
import org.dozer.fieldmap.FieldMap;
import org.dozer.fieldmap.HintContainer;
import org.dozer.util.BridgedMethodFinder;
import org.dozer.util.CollectionUtils;
import org.dozer.util.MappingUtils;
import org.dozer.util.ReflectionUtils;
import org.dozer.util.TypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Internal class used to read and write values for fields that have a getter and setter method. This class encapsulates
 * underlying dozer specific logic such as index mapping and deep mapping for reading and writing field values. Only
 * intended for internal use.
 *
 * @author garsombke.franz
 * @author tierney.matt
 * @author dmitry.buzdin
 */
public abstract class GetterSetterPropertyDescriptor extends AbstractPropertyDescriptor {

    private static final Logger log = LoggerFactory.getLogger(GetterSetterPropertyDescriptor.class);

    private Class<?> propertyType;

    public GetterSetterPropertyDescriptor(Class<?> clazz, String fieldName, boolean isIndexed, int index, HintContainer srcDeepIndexHintContainer, HintContainer destDeepIndexHintContainer) {
        super(clazz, fieldName, isIndexed, index, srcDeepIndexHintContainer, destDeepIndexHintContainer);
    }

    public abstract Method getWriteMethod() throws NoSuchMethodException;

    protected abstract Method getReadMethod() throws NoSuchMethodException;

    protected abstract String getSetMethodName() throws NoSuchMethodException;

    protected abstract boolean isCustomSetMethod();

    public Class<?> getPropertyType() {
        if (propertyType == null) {
            propertyType = determinePropertyType();
        }
        return propertyType;
    }

    public Object getPropertyValue(Object bean) {
        Object result;
        if (MappingUtils.isDeepMapping(fieldName)) {
            result = getDeepSrcFieldValue(bean);
        } else {
            result = invokeReadMethod(bean);
            if (isIndexed) {
                result = MappingUtils.getIndexedValue(result, index);
            }
        }
        return result;
    }

    public void setPropertyValue(Object bean, Object value, FieldMap fieldMap) {
        if (MappingUtils.isDeepMapping(fieldName)) {
            writeDeepDestinationValue(bean, value, fieldMap);
        } else {
            if (!getPropertyType().isPrimitive() || value != null) {
                if (isIndexed) {
                    writeIndexedValue(bean, value);
                } else {
                    try {
                        if (getPropertyValue(bean) == value && !isIndexed) {
                            return;
                        }
                    } catch (Exception e) {
                    }
                    invokeWriteMethod(bean, value);
                }
            }
        }
    }

    private Object getDeepSrcFieldValue(Object srcObj) {
        Object parentObj = srcObj;
        Object hierarchyValue = parentObj;
        DeepHierarchyElement[] hierarchy = getDeepFieldHierarchy(srcObj, srcDeepIndexHintContainer);
        int size = hierarchy.length;
        for (int i = 0; i < size; i++) {
            DeepHierarchyElement hierarchyElement = hierarchy[i];
            PropertyDescriptor pd = hierarchyElement.getPropDescriptor();
            if (hierarchyElement.getIndex() > -1) {
                hierarchyValue = MappingUtils.getIndexedValue(ReflectionUtils.invoke(pd.getReadMethod(), hierarchyValue, null), hierarchyElement.getIndex());
            } else {
                hierarchyValue = ReflectionUtils.invoke(pd.getReadMethod(), parentObj, null);
            }
            parentObj = hierarchyValue;
            if (hierarchyValue == null) {
                break;
            }
            if (isIndexed) {
                hierarchyValue = MappingUtils.getIndexedValue(hierarchyValue, index);
            }
        }
        return hierarchyValue;
    }

    protected void writeDeepDestinationValue(Object destObj, Object destFieldValue, FieldMap fieldMap) {
        DeepHierarchyElement[] hierarchy = getDeepFieldHierarchy(destObj, fieldMap.getDestDeepIndexHintContainer());
        Object parentObj = destObj;
        int hierarchyLength = hierarchy.length - 1;
        int hintIndex = 0;
        for (int i = 0; i < hierarchyLength; i++) {
            DeepHierarchyElement hierarchyElement = hierarchy[i];
            PropertyDescriptor pd = hierarchyElement.getPropDescriptor();
            Object value = ReflectionUtils.invoke(pd.getReadMethod(), parentObj, null);
            Class<?> clazz;
            Class<?> collectionEntryType;
            if (value == null) {
                clazz = pd.getPropertyType();
                if (clazz.isInterface() && (i + 1) == hierarchyLength && fieldMap.getDestHintContainer() != null) {
                    clazz = fieldMap.getDestHintContainer().getHint();
                }
                Object o = null;
                if (clazz.isArray()) {
                    o = MappingUtils.prepareIndexedCollection(clazz, null, DestBeanCreator.create(clazz.getComponentType()), hierarchyElement.getIndex());
                } else if (Collection.class.isAssignableFrom(clazz)) {
                    Class<?> genericType = ReflectionUtils.determineGenericsType(pd);
                    if (genericType != null) {
                        collectionEntryType = genericType;
                    } else {
                        collectionEntryType = fieldMap.getDestDeepIndexHintContainer().getHint(hintIndex);
                        hintIndex += 1;
                    }
                    o = MappingUtils.prepareIndexedCollection(clazz, null, DestBeanCreator.create(collectionEntryType), hierarchyElement.getIndex());
                } else {
                    try {
                        o = DestBeanCreator.create(clazz);
                    } catch (Exception e) {
                        if (fieldMap.getClassMap().getDestClassBeanFactory() != null) {
                            o = DestBeanCreator.create(new BeanCreationDirective(null, fieldMap.getClassMap().getSrcClassToMap(), clazz, clazz, fieldMap.getClassMap().getDestClassBeanFactory(), fieldMap.getClassMap().getDestClassBeanFactoryId(), null));
                        } else {
                            MappingUtils.throwMappingException(e);
                        }
                    }
                }
                ReflectionUtils.invoke(pd.getWriteMethod(), parentObj, new Object[] { o });
                value = ReflectionUtils.invoke(pd.getReadMethod(), parentObj, null);
            }
            if (MappingUtils.isSupportedCollection(value.getClass())) {
                int currentSize = CollectionUtils.getLengthOfCollection(value);
                if (currentSize < hierarchyElement.getIndex() + 1) {
                    collectionEntryType = pd.getPropertyType().getComponentType();
                    if (collectionEntryType == null) {
                        collectionEntryType = ReflectionUtils.determineGenericsType(pd);
                        if (collectionEntryType == null) {
                            if (log.isWarnEnabled()) {
                                log.warn(fieldName + " is in a Collection with an unspecified type.");
                            }
                            if (destDeepIndexHintContainer != null && destDeepIndexHintContainer.getHints() != null && destDeepIndexHintContainer.getHints().size() > 0) {
                                collectionEntryType = destDeepIndexHintContainer.getHints().get(0);
                                if (log.isWarnEnabled()) {
                                    log.warn("Using deep-index-hint to predict containing Collection type for field " + fieldName + " to be " + collectionEntryType);
                                }
                            }
                        }
                    }
                    value = MappingUtils.prepareIndexedCollection(pd.getPropertyType(), value, DestBeanCreator.create(collectionEntryType), hierarchyElement.getIndex());
                    ReflectionUtils.invoke(pd.getWriteMethod(), parentObj, new Object[] { value });
                }
            }
            if (value != null && value.getClass().isArray()) {
                parentObj = Array.get(value, hierarchyElement.getIndex());
            } else if (value != null && Collection.class.isAssignableFrom(value.getClass())) {
                parentObj = MappingUtils.getIndexedValue(value, hierarchyElement.getIndex());
            } else {
                parentObj = value;
            }
        }
        PropertyDescriptor pd = hierarchy[hierarchy.length - 1].getPropDescriptor();
        Class<?> type;
        if (pd.getReadMethod() != null) {
            type = pd.getReadMethod().getReturnType();
        } else {
            type = pd.getWriteMethod().getParameterTypes()[0];
        }
        if (!type.isPrimitive() || destFieldValue != null) {
            if (!isIndexed) {
                Method method = null;
                if (!isCustomSetMethod()) {
                    method = pd.getWriteMethod();
                } else {
                    try {
                        method = ReflectionUtils.findAMethod(parentObj.getClass(), getSetMethodName());
                    } catch (NoSuchMethodException e) {
                        MappingUtils.throwMappingException(e);
                    }
                }
                ReflectionUtils.invoke(method, parentObj, new Object[] { destFieldValue });
            } else {
                writeIndexedValue(parentObj, destFieldValue);
            }
        }
    }

    protected Object invokeReadMethod(Object target) {
        Object result = null;
        try {
            result = ReflectionUtils.invoke(getReadMethod(), target, null);
        } catch (NoSuchMethodException e) {
            MappingUtils.throwMappingException(e);
        }
        return result;
    }

    protected void invokeWriteMethod(Object target, Object value) {
        try {
            ReflectionUtils.invoke(getWriteMethod(), target, new Object[] { value });
        } catch (NoSuchMethodException e) {
            MappingUtils.throwMappingException(e);
        }
    }

    private DeepHierarchyElement[] getDeepFieldHierarchy(Object obj, HintContainer deepIndexHintContainer) {
        return ReflectionUtils.getDeepFieldHierarchy(obj.getClass(), fieldName, deepIndexHintContainer);
    }

    private void writeIndexedValue(Object destObj, Object destFieldValue) {
        Object existingValue = invokeReadMethod(destObj);
        Object indexedValue = MappingUtils.prepareIndexedCollection(getPropertyType(), existingValue, destFieldValue, index);
        invokeWriteMethod(destObj, indexedValue);
    }

    private Class determinePropertyType() {
        Method readMethod = getBridgedReadMethod();
        Method writeMethod = getBridgedWriteMethod();
        Class returnType = null;
        try {
            returnType = TypeResolver.resolvePropertyType(clazz, readMethod, writeMethod);
        } catch (Exception ignore) {
        }
        if (returnType != null) {
            return returnType;
        }
        if (readMethod == null && writeMethod == null) {
            throw new MappingException("No read or write method found for field (" + fieldName + ") in class (" + clazz + ")");
        }
        if (readMethod == null) {
            return determineByWriteMethod(writeMethod);
        } else {
            try {
                return readMethod.getReturnType();
            } catch (Exception e) {
                return determineByWriteMethod(writeMethod);
            }
        }
    }

    private Class determineByWriteMethod(Method writeMethod) {
        try {
            return writeMethod.getParameterTypes()[0];
        } catch (Exception e) {
            throw new MappingException(e);
        }
    }

    private Method getBridgedReadMethod() {
        try {
            return BridgedMethodFinder.findMethod(getReadMethod(), clazz);
        } catch (Exception ignore) {
        }
        return null;
    }

    private Method getBridgedWriteMethod() {
        try {
            return BridgedMethodFinder.findMethod(getWriteMethod(), clazz);
        } catch (Exception ignore) {
        }
        return null;
    }

    public Class<?> genericType() {
        Class<?> genericType = null;
        try {
            Method method = getWriteMethod();
            genericType = ReflectionUtils.determineGenericsType(method, false);
        } catch (NoSuchMethodException e) {
            log.warn("The destination object: {} does not have a write method for property : {}", e);
        }
        return genericType;
    }
}
