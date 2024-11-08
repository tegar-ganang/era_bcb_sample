package org.odiem.sdk;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.odiem.sdk.annotations.AnnotationUtils;
import org.odiem.sdk.annotations.Attribute;
import org.odiem.sdk.annotations.BaseDn;
import org.odiem.sdk.annotations.Child;
import org.odiem.sdk.annotations.ObjectClass;
import org.odiem.sdk.beans.OdmAttribute;
import org.odiem.sdk.exceptions.OdmException;
import org.odiem.sdk.exceptions.OdmParsingException;
import org.odiem.sdk.mappers.AttributeMapper;
import org.odiem.sdk.mappers.BaseDnMapper;
import org.odiem.sdk.mappers.ChildMapper;

public final class OdmPojo<T> {

    private static final ConcurrentHashMap<String, OdmPojo<?>> pojoMap = new ConcurrentHashMap<String, OdmPojo<?>>();

    private Class<T> pojoClass;

    private boolean abstractClass = false;

    private ObjectClass ldapClass;

    private String[] ldapAttributes;

    private AttributeMapper idMapper;

    private BaseDnMapper baseDnMapper;

    private TreeMap<String, AttributeMapper> attributeMappers = new TreeMap<String, AttributeMapper>();

    private ArrayList<ChildMapper> childMappers = new ArrayList<ChildMapper>();

    private TreeMap<String, Class<?>> annotatedSubClasses = new TreeMap<String, Class<?>>();

    private final byte INVALID_CASE = -1;

    private final byte BYPASS_CASE = 0;

    private final byte ATTRIBUTE_CASE = 1;

    private final byte CHILLD_CASE = 10;

    private final byte BASEDN_CASE = 100;

    public static <T> OdmPojo<T> getInstance(Class<T> pojoClass) throws OdmException {
        @SuppressWarnings("unchecked") OdmPojo<T> odmPojo = (OdmPojo<T>) pojoMap.get(pojoClass.getName());
        if (odmPojo == null) {
            odmPojo = new OdmPojo<T>(pojoClass);
            pojoMap.put(pojoClass.getName(), odmPojo);
        }
        return odmPojo;
    }

    private OdmPojo(Class<T> pojoClass) throws OdmException {
        try {
            this.pojoClass = pojoClass;
            ldapClass = (ObjectClass) pojoClass.getAnnotation(ObjectClass.class);
            if (Modifier.isAbstract(pojoClass.getModifiers())) {
                abstractClass = true;
                Set<Class<?>> set = AnnotationUtils.listAnnotatedSubClasses(pojoClass);
                for (Class<?> annotatedClass : set) {
                    annotatedSubClasses.put(getInstance(annotatedClass).ldapClass.value(), annotatedClass);
                }
            } else {
                if (ldapClass != null) {
                    if (ldapClass.value().length() == 0) {
                        ldapClass = AnnotationUtils.copyObjectClass(ldapClass, pojoClass.getSimpleName());
                    }
                    PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(pojoClass, Object.class).getPropertyDescriptors();
                    HashMap<String, Field> map = new HashMap<String, Field>();
                    getAllFields(map, pojoClass);
                    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                        String propName = propertyDescriptor.getName();
                        Method writeMethod = propertyDescriptor.getWriteMethod();
                        Method readMethod = propertyDescriptor.getReadMethod();
                        Field field = map.get(propName);
                        if (field == null) {
                            continue;
                        }
                        Attribute attribute = field.getAnnotation(Attribute.class);
                        Child child = field.getAnnotation(Child.class);
                        BaseDn baseDn = field.getAnnotation(BaseDn.class);
                        byte check = check(attribute, child, baseDn, readMethod, writeMethod);
                        switch(check) {
                            case BYPASS_CASE:
                                continue;
                            case ATTRIBUTE_CASE:
                                if (attribute.value().length() == 0) {
                                    attribute = AnnotationUtils.copyAttribute(attribute, propName);
                                }
                                AttributeMapper attributeMapper = new AttributeMapper(attribute, readMethod, writeMethod);
                                attributeMappers.put(attributeMapper.getAttribute().value(), attributeMapper);
                                if (attribute.isId() && idMapper == null) {
                                    idMapper = attributeMapper;
                                } else if (attribute.isId()) {
                                    throw new OdmParsingException(pojoClass, "Only one DN annotation is allowed");
                                }
                                break;
                            case CHILLD_CASE:
                                childMappers.add(new ChildMapper(child, readMethod, writeMethod));
                                break;
                            case BASEDN_CASE:
                                if (baseDnMapper == null) {
                                    baseDnMapper = new BaseDnMapper(baseDn, readMethod, writeMethod);
                                } else {
                                    throw new OdmParsingException(pojoClass, "Only one BaseDn annotation is allowed");
                                }
                                break;
                            default:
                                if (check < 0) {
                                    throw new OdmParsingException(pojoClass, "no getter or setter found for field " + field.getName());
                                } else {
                                    throw new OdmParsingException(pojoClass, "Attribute, Child, Id and BaseDn annotation are mutually exclusive");
                                }
                        }
                    }
                    if (idMapper == null) {
                        throw new OdmParsingException(pojoClass, "DN annotation is mandatory");
                    }
                } else {
                    throw new OdmParsingException(pojoClass, "ObjectClass annotation (value) is mandatory");
                }
            }
        } catch (Exception exception) {
            throw new OdmException(exception);
        }
    }

    protected boolean isAbstractClass() {
        return abstractClass;
    }

    protected String getLdapClass() {
        return ldapClass.value();
    }

    protected Class<T> getPojoClass() {
        return pojoClass;
    }

    protected String getDnAttribute() {
        return idMapper.getAttribute().value();
    }

    protected ArrayList<ChildMapper> getChilds() {
        return childMappers;
    }

    protected String[] getAttributes() {
        if (ldapAttributes == null) {
            HashSet<String> set = new HashSet<String>();
            for (AttributeMapper attributeMapper : attributeMappers.values()) {
                set.add(attributeMapper.getAttribute().value());
            }
            ldapAttributes = set.toArray(new String[set.size()]);
        }
        return ldapAttributes;
    }

    protected String dn(T pojo) throws OdmException {
        if (pojo == null) {
            throw new OdmException("Pojo is null");
        }
        try {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(idMapper.getAttribute().value()).append("=").append(idMapper.getReadMethod().invoke(pojo));
            String baseDn = (String) baseDnMapper.getReadMethod().invoke(pojo);
            if (baseDn != null && baseDn.length() > 0) {
                stringBuffer.append(",").append(baseDn);
            }
            return stringBuffer.toString();
        } catch (Exception e) {
            throw new OdmException(e);
        }
    }

    protected List<OdmAttribute> getAttributes(T pojo, boolean includeObjectClass) throws OdmException {
        if (pojo == null) {
            throw new OdmException("Pojo is null");
        }
        try {
            ArrayList<OdmAttribute> odmAttributes = new ArrayList<OdmAttribute>();
            for (AttributeMapper attributeMapper : attributeMappers.values()) {
                Object tmp = attributeMapper.getReadMethod().invoke(pojo);
                PropertyEditor editor = attributeMapper.getPropertyEditor();
                ArrayList<String> values = new ArrayList<String>();
                if (tmp != null) {
                    switch(attributeMapper.getType()) {
                        case ARRAY:
                            int len = Array.getLength(tmp);
                            for (int j = 0; j < len; j++) {
                                editor.setValue(Array.get(tmp, j));
                                values.add(editor.getAsText());
                            }
                            break;
                        case COLLECTION:
                            Collection<?> collection = (Collection<?>) tmp;
                            for (Object object : collection) {
                                editor.setValue(object);
                                values.add(editor.getAsText());
                            }
                            break;
                        default:
                            editor.setValue(tmp);
                            values.add(editor.getAsText());
                            break;
                    }
                }
                odmAttributes.add(new OdmAttribute(attributeMapper.getAttribute().value(), values.toArray(new String[values.size()])));
            }
            if (includeObjectClass) {
                odmAttributes.add(new OdmAttribute("objectclass", ldapClass.value()));
            }
            return odmAttributes;
        } catch (Exception e) {
            throw new OdmException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected T createAndFillPojo(String dn, OdmAttribute... attributes) throws OdmException {
        try {
            T pojo = null;
            if (abstractClass) {
                for (OdmAttribute odmAttribute : attributes) {
                    if (odmAttribute.getName().equals("objectClass")) {
                        for (String value : odmAttribute.getValues()) {
                            Class<?> clazz = annotatedSubClasses.get(value);
                            if (clazz != null) {
                                pojo = (T) getInstance(clazz).createAndFillPojo(dn, attributes);
                                break;
                            }
                        }
                        break;
                    }
                }
            } else {
                pojo = pojoClass.newInstance();
                fillPojo(pojo, dn, attributes);
            }
            return pojo;
        } catch (Exception e) {
            throw new OdmException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void fillPojo(T pojo, String dn, OdmAttribute... attributes) throws OdmException {
        if (pojoClass == null) {
            throw new OdmException("PojoClass is null");
        }
        try {
            OdmPojo<?> odmPojo = getInstance(pojoClass);
            if (dn != null) {
                String baseDn = dn.substring(dn.indexOf(',') + 1, dn.length());
                odmPojo.baseDnMapper.getWriteMethod().invoke(pojo, baseDn);
            }
            for (OdmAttribute odmAttribute : attributes) {
                int len = odmAttribute.getValues().length;
                if (len > 0) {
                    AttributeMapper attributeMapper = attributeMappers.get(odmAttribute.getName());
                    if (attributeMapper == null) {
                        continue;
                    }
                    PropertyEditor propertyEditor = attributeMapper.getPropertyEditor();
                    Object obj = null;
                    switch(attributeMapper.getType()) {
                        case ARRAY:
                            obj = Array.newInstance(attributeMapper.getCoreClass(), len);
                            for (int j = 0; j < len; j++) {
                                propertyEditor.setAsText(odmAttribute.getValues()[j]);
                                Array.set(obj, j, propertyEditor.getValue());
                            }
                            break;
                        case COLLECTION:
                            obj = attributeMapper.getCollectionClass().newInstance();
                            for (int j = 0; j < len; j++) {
                                propertyEditor.setAsText(odmAttribute.getValues()[j]);
                                ((Collection<Object>) obj).add(propertyEditor.getValue());
                            }
                            break;
                        default:
                            propertyEditor.setAsText(odmAttribute.getValues()[0]);
                            obj = propertyEditor.getValue();
                    }
                    attributeMapper.getWriteMethod().invoke(pojo, obj);
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            throw new OdmException(e);
        }
    }

    protected Object[] getChilds(T pojo) throws OdmException {
        if (pojo == null) {
            throw new OdmException("Pojo is null");
        }
        try {
            ArrayList<Object> result = new ArrayList<Object>();
            for (ChildMapper childMapper : childMappers) {
                Object tmp = childMapper.getReadMethod().invoke(pojo);
                if (tmp != null) {
                    switch(childMapper.getType()) {
                        case ARRAY:
                            int len = Array.getLength(tmp);
                            for (int j = 0; j < len; j++) {
                                result.add(Array.get(tmp, j));
                            }
                            break;
                        case COLLECTION:
                            Iterable<?> iterable = (Iterable<?>) tmp;
                            for (Object object : iterable) {
                                result.add(object);
                            }
                            break;
                        default:
                            result.add(tmp);
                    }
                }
            }
            return result.toArray();
        } catch (Exception e) {
            throw new OdmException(e);
        }
    }

    protected <Z> void fillChild(List<Z> childPojos, ChildMapper childMapper, T faterPojo) throws OdmException {
        try {
            Object tmp = null;
            if (childPojos != null) {
                switch(childMapper.getType()) {
                    case ARRAY:
                        tmp = Array.newInstance(childMapper.getCoreClass(), childPojos.size());
                        System.arraycopy(childPojos.toArray(), 0, tmp, 0, childPojos.size());
                        break;
                    case COLLECTION:
                        tmp = childPojos;
                        break;
                    default:
                        if (childPojos.size() > 0) {
                            tmp = childPojos.get(0);
                        }
                        break;
                }
            }
            childMapper.getWriteMethod().invoke(faterPojo, tmp);
        } catch (Exception e) {
            throw new OdmException(e);
        }
    }

    private byte check(Attribute attribute, Child child, BaseDn baseDn, Method readMethod, Method writeMethod) {
        byte check = BYPASS_CASE;
        if (attribute != null) {
            check += ATTRIBUTE_CASE;
        }
        if (child != null) {
            check += CHILLD_CASE;
        }
        if (baseDn != null) {
            check += BASEDN_CASE;
        }
        if (readMethod == null || writeMethod == null) {
            check *= INVALID_CASE;
        }
        return check;
    }

    private static void getAllFields(Map<String, Field> fields, Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            fields.put(field.getName(), field);
        }
        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
    }
}
