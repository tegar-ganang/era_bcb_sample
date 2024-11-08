package org.chelian.dependency.reverse;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class ToSpring {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ToSpring.class);

    @SuppressWarnings("unchecked")
    private static final Set<Class<?>> BUILTIN_CLASSES = new HashSet<Class<?>>(Arrays.asList(Boolean.class, Byte.class, Character.class, Double.class, Float.class, Integer.class, Long.class, Short.class, String.class));

    private final IdentityHashMap<Object, BeanReference<?>> reversedMap = new IdentityHashMap<Object, BeanReference<?>>();

    public void output(Map<String, Object> objects, List<ApplicationContext> contexts, SpringAdapter adapter) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        Set<String> beanNames = new HashSet<String>();
        Set<FileReference> fileLocations = new HashSet<FileReference>();
        for (ApplicationContext context : contexts) {
            if (context instanceof ConfigExposingContext) {
                FileReference.Context fileRefContext = null;
                if (context instanceof FileSystemXmlApplicationContext) {
                    fileRefContext = FileReference.Context.PATH;
                } else if (context instanceof ClassPathXmlApplicationContext) {
                    fileRefContext = FileReference.Context.CLASSPATH;
                } else {
                    throw new IllegalArgumentException("Don't know how to deal with this type of ApplicationContext " + context.getClass().getName());
                }
                for (String fileName : ((ConfigExposingContext) context).getConfigLocations()) {
                    fileLocations.add(new FileReference(fileRefContext, fileName));
                }
            } else {
                throw new IllegalArgumentException("Please use LookupFileSystemXmlApplicationContext or LookupClassPathXmlApplicationContext.");
            }
            for (String beanName : context.getBeanDefinitionNames()) {
                Object bean = context.getBean(beanName);
                reversedMap.put(bean, new BeanReference<Object>(beanName, bean));
                beanNames.add(beanName);
            }
        }
        for (Map.Entry<String, Object> entry : objects.entrySet()) {
            if (beanNames.contains(entry.getKey())) {
                throw new IllegalStateException("Base contexts already contain a bean named " + entry.getKey());
            }
            reversedMap.put(entry.getValue(), new BeanReference<Object>(entry.getKey(), entry.getValue()));
        }
        adapter.startBeans();
        adapter.includeFiles(fileLocations);
        for (Map.Entry<String, Object> entry : objects.entrySet()) {
            Object bean = entry.getValue();
            try {
                fillBean(adapter, bean, bean.getClass(), entry.getKey());
            } catch (IllegalArgumentException e) {
                log.error("Failed to serialize " + entry.getKey(), e);
                throw e;
            } catch (IllegalAccessException e) {
                log.error("Failed to serialize " + entry.getKey(), e);
                throw e;
            } catch (InvocationTargetException e) {
                log.error("Failed to serialize " + entry.getKey(), e);
                throw e;
            } catch (IllegalStateException e) {
                log.error("Failed to serialize " + entry.getKey(), e);
                throw e;
            }
        }
        adapter.endBeans();
    }

    private void fillBean(SpringAdapter adapter, Object obj, Class<?> clazz) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        fillBean(adapter, obj, clazz, null);
    }

    private void fillBean(SpringAdapter adapter, Object bean, Type type, String name) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        if (name == null) {
            adapter.startBean(bean.getClass());
        } else {
            adapter.startBean(name, bean.getClass());
        }
        Class<?> clazz = (Class<?>) type;
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        for (PropertyDescriptor propDesc : beanInfo.getPropertyDescriptors()) {
            Method readMethod = propDesc.getReadMethod();
            Method writeMethod = propDesc.getWriteMethod();
            if (readMethod == null || writeMethod == null) {
                continue;
            }
            Class<?> returnClazz = readMethod.getReturnType();
            Object value = readMethod.invoke(bean);
            if (returnClazz.isPrimitive()) {
                adapter.startProperty(propDesc.getName(), value.toString());
                adapter.endProperty();
            } else if (value == null) {
                continue;
            } else if (isSupportedBuiltin(returnClazz) || returnClazz.isEnum()) {
                adapter.startProperty(propDesc.getName(), value.toString());
                adapter.endProperty();
            } else if (Map.class.isAssignableFrom(returnClazz)) {
                adapter.startProperty(propDesc.getName());
                Type returnType = readMethod.getGenericReturnType();
                ParameterizedType pType = (ParameterizedType) returnType;
                Map<?, ?> mapValue = (Map<?, ?>) value;
                fillMap(adapter, mapValue, pType);
                adapter.endProperty();
            } else if (List.class.isAssignableFrom(returnClazz)) {
                List<?> listValue = (List<?>) value;
                Type returnType = readMethod.getGenericReturnType();
                ParameterizedType pType = (ParameterizedType) returnType;
                adapter.startProperty(propDesc.getName());
                fillList(adapter, listValue, pType);
                adapter.endProperty();
            } else if (Set.class.isAssignableFrom(returnClazz)) {
                Set<?> setValue = (Set<?>) value;
                Type returnType = readMethod.getGenericReturnType();
                ParameterizedType pType = (ParameterizedType) returnType;
                adapter.startProperty(propDesc.getName());
                fillSet(adapter, setValue, pType);
                adapter.endProperty();
            } else if (reversedMap.containsKey(value)) {
                adapter.startProperty(propDesc.getName(), reversedMap.get(value));
                adapter.endProperty();
            } else {
                adapter.startProperty(propDesc.getName());
                fillBean(adapter, value, value.getClass(), null);
                adapter.endProperty();
            }
        }
        adapter.endBean();
    }

    private void fillList(SpringAdapter adapter, List<?> collection, ParameterizedType type) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        adapter.startList();
        fillCollection(adapter, collection, type);
        adapter.endList();
    }

    private void fillSet(SpringAdapter adapter, Set<?> collection, ParameterizedType type) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        adapter.startSet();
        fillCollection(adapter, collection, type);
        adapter.endSet();
    }

    private void fillCollection(SpringAdapter adapter, Collection<?> collectionValue, ParameterizedType type) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        Type subType = Object.class;
        if (type != null) {
            subType = type.getActualTypeArguments()[0];
        }
        if (subType instanceof Class && (isSupportedBuiltin((Class<?>) subType) || ((Class<?>) subType).isEnum())) {
            for (Object obj : collectionValue) {
                adapter.collectionValue(obj.toString());
            }
        } else if (subType instanceof ParameterizedType) {
            Class<?> rawClass = (Class<?>) ((ParameterizedType) subType).getRawType();
            if (Map.class.isAssignableFrom(rawClass)) {
                for (Object obj : collectionValue) {
                    fillMap(adapter, (Map<?, ?>) obj, (ParameterizedType) subType);
                }
            } else if (List.class.isAssignableFrom(rawClass)) {
                for (Object obj : collectionValue) {
                    fillList(adapter, (List<?>) obj, (ParameterizedType) subType);
                }
            } else if (Set.class.isAssignableFrom(rawClass)) {
                for (Object obj : collectionValue) {
                    fillSet(adapter, (Set<?>) obj, (ParameterizedType) subType);
                }
            } else if (Collection.class.isAssignableFrom(rawClass)) {
                throw new IllegalArgumentException("Cannot handle collections other than lists or sets");
            } else {
                for (Object obj : collectionValue) {
                    fillBean(adapter, obj, obj.getClass(), null);
                }
            }
        } else {
            for (Object obj : collectionValue) {
                Class<?> clazz = obj.getClass();
                if (String.class.equals(clazz)) {
                    adapter.collectionValue(obj.toString());
                } else if (isSupportedBuiltin(clazz) || clazz.isEnum()) {
                    throw new IllegalArgumentException("Untyped collection will lose type information with object of this type: " + clazz.getName());
                } else if (reversedMap.containsKey(obj)) {
                    adapter.startRef(reversedMap.get(obj));
                    adapter.endRef();
                } else if (List.class.isAssignableFrom(clazz)) {
                    fillList(adapter, (List<?>) obj, null);
                } else if (Set.class.isAssignableFrom(clazz)) {
                    fillSet(adapter, (Set<?>) obj, null);
                } else if (Collection.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("Only List and Set are suporrted collection types.");
                } else if (Map.class.isAssignableFrom(clazz)) {
                    fillMap(adapter, (Map<?, ?>) obj, null);
                } else if (clazz.getTypeParameters() != null && clazz.getTypeParameters().length > 0) {
                    throw new IllegalArgumentException("Generic class type information is lost in untyped container.");
                } else {
                    fillBean(adapter, obj, clazz);
                }
            }
        }
    }

    private void fillMap(SpringAdapter adapter, Map<?, ?> mapValue, ParameterizedType type) throws IllegalArgumentException, IntrospectionException, IllegalAccessException, InvocationTargetException {
        Type keyType = Object.class;
        Type valueType = Object.class;
        if (type != null) {
            Type[] mapTypes = type.getActualTypeArguments();
            keyType = mapTypes[0];
            valueType = mapTypes[1];
        }
        adapter.startMap();
        Class<?> keyClass = keyType instanceof Class<?> ? (Class<?>) keyType : null;
        Class<?> valueClass = valueType instanceof Class<?> ? (Class<?>) keyType : null;
        ParameterizedType keyParamType = keyType instanceof ParameterizedType ? (ParameterizedType) keyType : null;
        ParameterizedType valueParamType = valueType instanceof ParameterizedType ? (ParameterizedType) valueType : null;
        if (keyClass == null && keyParamType == null) {
            throw new IllegalArgumentException("Key type is neither class nor parameterized type");
        }
        if (valueClass == null && valueParamType == null) {
            throw new IllegalArgumentException("Value type is neither class nor parameterized type");
        }
        for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
            String keyString = null, valueString = null;
            BeanReference<?> keyRef = null, valueRef = null;
            if (keyClass != null) {
                keyString = handleStringEntryData(entry.getKey().getClass(), keyClass, entry.getKey());
                if (keyString == null) {
                    keyRef = reversedMap.get(entry.getKey());
                }
            } else {
                keyRef = reversedMap.get(entry.getKey());
            }
            if (valueClass != null) {
                valueString = handleStringEntryData(entry.getValue().getClass(), valueClass, entry.getValue());
                if (valueString == null) {
                    valueRef = reversedMap.get(entry.getValue());
                }
            } else {
                valueRef = reversedMap.get(entry.getValue());
            }
            if (keyString != null) {
                if (valueString != null) {
                    adapter.startEntry(keyString, valueString);
                } else if (valueRef != null) {
                    adapter.startEntry(keyString, valueRef);
                } else {
                    adapter.startEntry(keyString);
                    if (valueClass != null) {
                        handleEntryData(entry.getValue().getClass(), valueClass, entry.getValue(), adapter);
                    } else {
                        handleEntryData(entry.getValue().getClass(), valueParamType, entry.getValue(), adapter);
                    }
                }
            } else if (keyRef != null) {
                if (valueString != null) {
                    adapter.startEntry(keyRef, valueString);
                } else if (valueRef != null) {
                    adapter.startEntry(keyRef, valueRef);
                } else {
                    adapter.startEntry(keyString);
                    if (valueClass != null) {
                        handleEntryData(entry.getValue().getClass(), valueClass, entry.getValue(), adapter);
                    } else {
                        handleEntryData(entry.getValue().getClass(), valueParamType, entry.getValue(), adapter);
                    }
                }
            } else {
                if (valueString != null) {
                    adapter.startEntryWithValue(valueString);
                } else if (valueRef != null) {
                    adapter.startEntryWithValue(valueRef);
                } else {
                    adapter.startEntry();
                }
                adapter.startKey();
                if (keyClass != null) {
                    handleEntryData(entry.getKey().getClass(), keyClass, entry.getKey(), adapter);
                } else {
                    handleEntryData(entry.getKey().getClass(), keyParamType, entry.getKey(), adapter);
                }
                adapter.endKey();
                if (valueString == null && valueRef == null) {
                    if (valueClass != null) {
                        handleEntryData(entry.getValue().getClass(), valueClass, entry.getValue(), adapter);
                    } else {
                        handleEntryData(entry.getValue().getClass(), valueParamType, entry.getValue(), adapter);
                    }
                }
            }
            adapter.endEntry();
        }
        adapter.endMap();
    }

    String handleStringEntryData(Class<?> clazz, Class<?> expectedClazz, Object value) {
        if (isSupportedBuiltin(expectedClazz) || expectedClazz.isEnum()) {
            return value.toString();
        } else if (String.class.equals(clazz)) {
            return value.toString();
        }
        return null;
    }

    /**
   * Assume that handleStringValue has been called. Assume that all of the data is appropriate. Assume that references have been checked for.
   * @param clazz
   * @param expectedClazz
   * @param value
   * @throws InvocationTargetException 
   * @throws IllegalAccessException 
   * @throws IntrospectionException 
   */
    void handleEntryData(Class<?> clazz, Class<?> expectedClazz, Object value, SpringAdapter adapter) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        if (isSupportedBuiltin(clazz) || clazz.isEnum()) {
            throw new IllegalArgumentException("Spring loses type information by transforming these to strings");
        } else if (List.class.isAssignableFrom(clazz)) {
            fillList(adapter, (List<?>) value, null);
        } else if (Set.class.isAssignableFrom(clazz)) {
            fillSet(adapter, (Set<?>) value, null);
        } else if (Map.class.isAssignableFrom(clazz)) {
            fillMap(adapter, (Map<?, ?>) value, null);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Spring cannot handle anything other than lists and sets");
        } else {
            fillBean(adapter, value, clazz);
        }
    }

    void handleEntryData(Class<?> clazz, ParameterizedType type, Object value, SpringAdapter adapter) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        Class<?> typeClazz = (Class<?>) type.getRawType();
        if (List.class.isAssignableFrom(typeClazz)) {
            fillList(adapter, (List<?>) value, type);
        } else if (Set.class.isAssignableFrom(typeClazz)) {
            fillSet(adapter, (Set<?>) value, type);
        } else if (Map.class.isAssignableFrom(typeClazz)) {
            fillMap(adapter, (Map<?, ?>) value, type);
        } else if (Collection.class.isAssignableFrom(typeClazz)) {
            throw new IllegalArgumentException("Spring cannot handle any collection type other than lists or sets");
        } else {
            fillBean(adapter, value, clazz);
        }
    }

    public static boolean isSupportedBuiltin(Class<?> clazz) {
        return BUILTIN_CLASSES.contains(clazz);
    }
}
