package org.judo.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.judo.dataproperty.DataProperty;

public class Properties {

    public static List getDataPropertyInstanceVars(Object obj) throws Exception {
        Class objClass = obj.getClass();
        ArrayList list = new ArrayList();
        Field fields[] = obj.getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            Object value = fields[i].get(obj);
            if (value != null) {
                if (value instanceof DataProperty) {
                    list.add(fields[i]);
                }
            }
        }
        return list;
    }

    public static List getPrimitiveProperties(Object obj) throws Exception {
        List list = new ArrayList();
        BeanInfo desc = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor props[] = desc.getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            String type = props[i].getPropertyType().getSimpleName();
            if (type.equals("String") || type.equals("int") || type.equals("float") || type.equals("double") || type.equals("Date") || type.equals("boolean")) {
                list.add(props[i]);
            }
        }
        return list;
    }

    public static List getFullProperties(Object obj) throws Exception {
        List list = new ArrayList();
        BeanInfo desc = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor props[] = desc.getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            Method writeMethod = props[i].getWriteMethod();
            Method readMethod = props[i].getReadMethod();
            if (writeMethod != null && readMethod != null) list.add(props[i]);
        }
        return list;
    }

    public static boolean propertyExists(Object obj, String name) throws Exception {
        BeanInfo desc = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor props[] = desc.getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            if (props[i].getName().equals(name)) return true;
        }
        return false;
    }

    public static PropertyDescriptor getProperty(Object obj, String name) throws Exception {
        BeanInfo desc = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor props[] = desc.getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            if (props[i].getName().equals(name)) return props[i];
        }
        throw new Exception("Property Not Found: " + name);
    }

    public static Object getPropertyValue(PropertyDescriptor prop, Object entity) throws Exception {
        Object ret = null;
        Method method = prop.getReadMethod();
        ret = method.invoke(entity, null);
        return ret;
    }

    public static Object getPropertyValue(String name, Object entity) throws Exception {
        Object ret = null;
        BeanInfo desc = Introspector.getBeanInfo(entity.getClass());
        PropertyDescriptor props[] = desc.getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            if (props[i].getName().equals(name)) {
                Method method = props[i].getReadMethod();
                ret = method.invoke(entity, null);
            }
        }
        return ret;
    }

    public static void setObjData(Object entity, PropertyDescriptor desc, String data) throws Exception {
        if (data == null) return;
        String type = desc.getPropertyType().getSimpleName();
        Method method = desc.getWriteMethod();
        if (data.equals("null")) {
            method.invoke(entity, new Object[] { null });
        } else if (type.equals("String")) {
            method.invoke(entity, new Object[] { data });
        } else if (type.equals("Date")) {
            Date date = DateUtil.getDateFromStr(data);
            if (date == null) throw new SQLException("Invalid Date: " + data);
            method.invoke(entity, new Object[] { date });
        } else if (type.equals("int")) {
            method.invoke(entity, new Object[] { new Integer(data) });
        } else if (type.equals("float")) {
            method.invoke(entity, new Object[] { new Float(data) });
        } else if (type.equals("double")) {
            method.invoke(entity, new Object[] { new Double(data) });
        } else if (type.equals("boolean")) {
            method.invoke(entity, new Object[] { new Boolean(data) });
        }
    }

    public static void setObjData(Object entity, String propName, String data) throws Exception {
        List props = getPrimitiveProperties(entity);
        for (int i = 0; i < props.size(); i++) {
            PropertyDescriptor prop = (PropertyDescriptor) props.get(i);
            if (prop.getName().equals(propName)) {
                setObjData(entity, prop, data);
                return;
            }
        }
        throw new Exception("Property '" + propName + "' not found in: " + entity.getClass().getSimpleName());
    }

    public static void setObjValue(Object entity, String propName, Object data) throws Exception {
        List props = getFullProperties(entity);
        for (int i = 0; i < props.size(); i++) {
            PropertyDescriptor prop = (PropertyDescriptor) props.get(i);
            if (data == null) {
                String type = prop.getPropertyType().getName();
                if (type.equals("int")) data = 0; else if (type.equals("double")) data = 0; else if (type.equals("float")) data = 0; else if (type.equals("short")) data = 0; else if (type.equals("long")) data = 0; else if (type.equals("byte")) data = 0; else if (type.equals("booelan")) data = false;
            }
            if (prop.getName().equals(propName)) {
                Method method = prop.getWriteMethod();
                method.invoke(entity, new Object[] { data });
                return;
            }
        }
        throw new Exception("Property '" + propName + "' not found in: " + entity.getClass().getSimpleName());
    }
}
