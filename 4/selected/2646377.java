package clear.data;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.apache.commons.beanutils.PropertyUtils;

public class BeanProperty {

    static final Map<Class, List> relationPropertyMap = Collections.synchronizedMap(new HashMap<Class, List>());

    public static List<BeanProperty> getRelationProperties(Object bean) {
        return getRelationProperties(bean.getClass());
    }

    public static List<BeanProperty> getRelationProperties(Class clazz) {
        List<BeanProperty> list = relationPropertyMap.get(clazz);
        if (list != null) return list;
        list = new ArrayList();
        BeanProperty props[] = getProperties(clazz);
        for (int i = 0; i < props.length; i++) {
            if (!props[i].isWrite()) {
                continue;
            }
            if (props[i].isAnnotationPresent(ManyToOne.class)) {
                list.add(props[i]);
                continue;
            }
            if (props[i].isAnnotationPresent(OneToMany.class)) {
                list.add(props[i]);
                continue;
            }
            if (Collection.class.isAssignableFrom(props[i].type)) {
                list.add(props[i]);
                continue;
            }
        }
        relationPropertyMap.put(clazz, list);
        return list;
    }

    private static BeanProperty[] getProperties(Object bean) {
        PropertyDescriptor pds[];
        if (bean instanceof Class) {
            pds = PropertyUtils.getPropertyDescriptors((Class) bean);
        } else {
            pds = PropertyUtils.getPropertyDescriptors(bean.getClass());
        }
        HashMap<String, BeanProperty> map = new HashMap<String, BeanProperty>(pds.length * 2);
        for (int i = 0; i < pds.length; i++) {
            PropertyDescriptor pd = pds[i];
            map.put(pd.getName(), new BeanProperty(bean, pd.getName(), pd.getPropertyType(), pd.getReadMethod(), pd.getWriteMethod(), null));
        }
        Field fields[] = bean.getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (Modifier.isPublic(field.getModifiers()) && !map.containsKey(field.getName())) map.put(field.getName(), new BeanProperty(bean, field.getName(), field.getType(), null, null, field));
        }
        BeanProperty props[] = new BeanProperty[map.values().size()];
        map.values().toArray(props);
        return props;
    }

    private BeanProperty(Object bean, String name, Class type, Method read, Method write, Field field) {
        this.name = name;
        this.type = type;
        writeMethod = write;
        readMethod = read;
        this.field = field;
        if (isAnnotationPresent(OneToMany.class) || Collection.class.isAssignableFrom(type)) {
            collection = true;
        }
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    public boolean isWrite() {
        return writeMethod != null || field != null;
    }

    public boolean isRead() {
        return readMethod != null || field != null;
    }

    public Class getReadDeclaringClass() {
        if (readMethod != null) return readMethod.getDeclaringClass();
        if (field != null) return field.getDeclaringClass(); else return null;
    }

    public Class getReadType() {
        if (readMethod != null) return readMethod.getReturnType();
        if (field != null) return field.getType(); else return null;
    }

    public String getWriteName() {
        if (writeMethod != null) return "method " + writeMethod.getName();
        if (field != null) return "field " + field.getName(); else return null;
    }

    public void set(Object bean, Object obj) throws IllegalAccessException, InvocationTargetException {
        if (writeMethod != null) writeMethod.invoke(bean, new Object[] { obj }); else if (field != null) field.set(bean, obj); else throw new RuntimeException("Write property not found.");
    }

    public Object get(Object bean) throws IllegalAccessException, InvocationTargetException {
        Object obj = null;
        if (readMethod != null) obj = readMethod.invoke(bean, (Object[]) null); else if (field != null) obj = field.get(bean);
        return obj;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeanProperty)) return false;
        BeanProperty beanProperty = (BeanProperty) o;
        if (name == null ? beanProperty.name != null : !name.equals(beanProperty.name)) return false;
        if (type == null ? beanProperty.type != null : !type.equals(beanProperty.type)) return false;
        if (isRead() != beanProperty.isRead()) return false;
        return isWrite() == beanProperty.isWrite();
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> ann) {
        if (readMethod == null) return false;
        return readMethod.isAnnotationPresent(ann);
    }

    public boolean isCollection() {
        return collection;
    }

    private String name;

    private Class type;

    private Method readMethod;

    private Method writeMethod;

    private Field field;

    private boolean collection = false;
}
