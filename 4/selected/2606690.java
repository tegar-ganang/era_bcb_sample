package org.frameworkset.soa;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.frameworkset.spi.SOAApplicationContext;
import com.frameworkset.spi.assemble.BeanInstanceException;
import com.frameworkset.spi.assemble.CurrentlyInCreationException;
import com.frameworkset.util.NoSupportTypeCastException;
import com.frameworkset.util.ValueObjectUtil;

/**
 * <p>
 * Title: ObjectSerializable.java
 * </p>
 * <p>
 * Description: ������󷽷�����ת��Ϊxml���Ĺ���
 * </p>
 * <p>
 * bboss workgroup
 * </p>
 * <p>
 * Copyright (c) 2008
 * </p>
 * 
 * @Date 2011-5-10 ����11:14:21
 * @author biaoping.yin
 * @version 1.0
 */
public class ObjectSerializable {

    public static final String content_header_gbk = "<?xml version=\"1.0\" encoding=\"gbk\"?>";

    public static final String content_header_utf_8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    public static final String call_header = "<esb><call>";

    public static final String call_tailer = "</call></esb>";

    public static final String CHARSET_UTF_8 = "UTF-8";

    public static final String CHARSET_GBK = "GBK";

    public static final String[] throwable_filterattributes = new String[] { "message", "cause" };

    /**
	 * ��ʶnullֵ����
	 */
    public static final String NULL_TYPE = "s:nvl";

    /**
	 * @deprecated use method toObject(xml,Class)
	 * @return
	 */
    public static <T> T convertXMLToBeanObject(String name, String beanxml, Class<T> beantype) {
        SOAApplicationContext context = new SOAApplicationContext(beanxml);
        T object = context.getTBeanObject(name, beantype);
        context.destroy();
        return object;
    }

    private static void convertMethodCallToXMLMethod(StringWriter ret, String methodName, Object[] params, Class[] paramTypes, String charset) throws NoSupportTypeCastException, NumberFormatException, IllegalArgumentException, IntrospectionException, IOException {
        ret.append("<p n=\"soamethodcall\" ").append("cs=\"org.frameworkset.soa.SOAMethodCall\" >").append("<p n=\"soamethodinfo\" cs=\"org.frameworkset.soa.SOAMethodInfo\" ").append("f:methodName=\"").append(methodName).append("\">");
        if (paramTypes == null || paramTypes.length == 0) {
            ret.append("<p n=\"params\" s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(Object[].class)).append("\"/>");
        } else {
            ret.append("<p n=\"params\">").append("<a cmt=\"").append(ValueObjectUtil.getComponentTypeName(Object[].class)).append("\">");
            {
                ObjectSerializable.convertParams(ret, params, paramTypes, null);
            }
            ret.append("</a></p>");
        }
        ret.append("</p></p>");
    }

    public static String convertMethodCallToXMLMethod(Method method, Object[] params, Class[] paramTypes, String charset) throws NoSupportTypeCastException, NumberFormatException, IllegalArgumentException, IntrospectionException {
        try {
            StringWriter ret = new StringWriter();
            if (charset.equals(CHARSET_UTF_8)) {
                ret.append(content_header_utf_8);
            } else ret.append(content_header_gbk);
            ret.append(call_header);
            convertMethodCallToXMLMethod(ret, method.getName(), params, paramTypes, charset);
            ret.append(call_tailer);
            return ret.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String convertSOAMethodCallToXMLMethod(SOAMethodCall method, String charset) throws NoSupportTypeCastException, NumberFormatException, IllegalArgumentException, IntrospectionException {
        try {
            StringWriter ret = new StringWriter();
            if (charset.equals(CHARSET_UTF_8)) {
                ret.append(content_header_utf_8);
            } else ret.append(content_header_gbk);
            ret.append(call_header);
            convertBeanObjectToXML("soamethodcall", method, method.getClass(), null, ret);
            ret.append(call_tailer);
            return ret.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static final String convertBeanObjectToXML(Object obj, Class type, String dateformat) throws NoSupportTypeCastException, NumberFormatException, IllegalArgumentException, IntrospectionException {
        try {
            StringWriter ret = new StringWriter();
            convertBeanObjectToXML(null, obj, type, dateformat, ret);
            return ret.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
	 * @deprecated use Method toXML( Object obj).
	 */
    public static final String convertBeanObjectToXML(String name, Object obj, Class type) throws NumberFormatException, IllegalArgumentException, IntrospectionException {
        return convertBeanObjectToXML(name, obj, type, (String) null, CHARSET_GBK);
    }

    public static final String toXML(Object obj) throws NumberFormatException, IllegalArgumentException, IntrospectionException {
        if (obj == null) return null;
        return convertBeanObjectToXML("_dflt_", obj, obj.getClass(), (String) null, CHARSET_GBK);
    }

    public static final void toXML(Object obj, Writer out) throws NumberFormatException, IllegalArgumentException, IntrospectionException {
        if (obj == null) return;
        convertBeanObjectToXML("_dflt_", obj, obj.getClass(), null, CHARSET_GBK, out);
    }

    public static <T> T toBean(String beanxml, Class<T> beantype) {
        if (beanxml == null || beanxml.equals("")) return null;
        SOAApplicationContext context = new SOAApplicationContext(beanxml);
        T object = context.getTBeanObject("_dflt_", beantype);
        context.destroy();
        return object;
    }

    public static <T> T toBean(InputStream instream, Class<T> beantype) {
        if (instream == null) return null;
        SOAApplicationContext context = new SOAApplicationContext(instream);
        T object = context.getTBeanObject("_dflt_", beantype);
        context.destroy();
        return object;
    }

    public static final String convertBeanObjectToXML(String name, Object obj, Class type, String dateformat, String charset) throws NumberFormatException, IllegalArgumentException, IntrospectionException {
        StringWriter ret = new StringWriter();
        convertBeanObjectToXML(name, obj, type, dateformat, charset, ret);
        return ret.toString();
    }

    public static final void convertBeanObjectToXML(String name, Object obj, Class type, String dateformat, String charset, Writer ret) throws NumberFormatException, IllegalArgumentException, IntrospectionException {
        try {
            if (charset.equals(CHARSET_UTF_8)) {
                ret.append(content_header_utf_8);
            } else ret.append(content_header_gbk);
            ret.append("<ps>");
            convertBeanObjectToXML(name, obj, type, dateformat, ret);
            ret.append("</ps>");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static final String convertBeanObjectToXML(Object obj, Class type) throws NoSupportTypeCastException, NumberFormatException, IllegalArgumentException, IntrospectionException {
        StringWriter ret = new StringWriter();
        try {
            convertBeanObjectToXML(null, obj, type, null, ret);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return ret.toString();
    }

    public static final String convertBeanObjectsToXML(List<String> names, List<Object> objs, List<Class> types) throws NumberFormatException, IllegalArgumentException, IntrospectionException {
        return convertBeanObjectsToXML(names, objs, types, CHARSET_GBK);
    }

    public static final String convertBeanObjectsToXML(List<String> names, List<Object> objs, List<Class> types, String charset) throws NumberFormatException, IllegalArgumentException, IntrospectionException, NoSupportTypeCastException {
        try {
            StringWriter ret = new StringWriter();
            if (charset.equals(CHARSET_UTF_8)) {
                ret.append(content_header_utf_8);
            } else ret.append(content_header_gbk);
            ret.append("<ps>");
            if (objs != null && objs.size() > 0) {
                int i = 0;
                for (Object obj : objs) {
                    convertBeanObjectToXML(names.get(i), obj, types.get(i), null, ret);
                    i++;
                }
            }
            ret.append("</ps>");
            return ret.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
	 * ��ݶ���ֵ���������Ͳ��ҵ���Ӧ�ķ�������������Ƚ��鷳
	 * 
	 * @param obj
	 * @param type
	 * @param dateformat
	 * @param ret
	 * @throws NoSupportTypeCastException
	 * @throws NumberFormatException
	 * @throws IllegalArgumentException
	 * @throws IntrospectionException
	 * @throws IOException 
	 */
    private static final void convertBeanObjectToXML(String name, Object obj, Class type, String dateformat, Writer ret) throws NoSupportTypeCastException, NumberFormatException, IllegalArgumentException, IntrospectionException, IOException {
        if (obj != null) type = obj.getClass();
        if (type == byte[].class) {
            if (obj == null) {
                if (name == null) ret.append("<p s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>");
                return;
            } else {
                if (!File.class.isAssignableFrom(obj.getClass())) {
                    if (name == null) ret.append("<p s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"><![CDATA[").append(ValueObjectUtil.byteArrayEncoder((byte[]) obj)).append("]]></p>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"><![CDATA[").append(ValueObjectUtil.byteArrayEncoder((byte[]) obj)).append("]]></p>");
                } else {
                    File object = (File) obj;
                    java.io.FileInputStream byteIn = null;
                    java.io.ByteArrayOutputStream fileOut = null;
                    try {
                        byteIn = new java.io.FileInputStream(object);
                        fileOut = new java.io.ByteArrayOutputStream();
                        byte v[] = new byte[1024];
                        int i = 0;
                        while ((i = byteIn.read(v)) > 0) {
                            fileOut.write(v, 0, i);
                        }
                        fileOut.flush();
                        if (name == null) ret.append("<p s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"><![CDATA[").append(ValueObjectUtil.byteArrayEncoder(fileOut.toByteArray())).append("]]></p>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"><![CDATA[").append(ValueObjectUtil.byteArrayEncoder(fileOut.toByteArray())).append("]]></p>");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (byteIn != null) byteIn.close();
                        } catch (Exception e) {
                        }
                        try {
                            if (fileOut != null) fileOut.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
            return;
        } else if (type != null && File.class.isAssignableFrom(type)) {
            if (obj == null) {
                if (name == null) ret.append("<p s:nvl=\"true\" s:t=\"File\"/>"); else ret.append("<p n=\"").append(name).append("\" s:nvl=\"true\" s:t=\"File\"/>");
                return;
            } else {
                File object = (File) obj;
                java.io.FileInputStream byteIn = null;
                java.io.ByteArrayOutputStream fileOut = null;
                try {
                    byteIn = new java.io.FileInputStream(object);
                    fileOut = new java.io.ByteArrayOutputStream();
                    byte v[] = new byte[1024];
                    int i = 0;
                    while ((i = byteIn.read(v)) > 0) {
                        fileOut.write(v, 0, i);
                    }
                    fileOut.flush();
                    if (name == null) ret.append("<p s:t=\"File\"><![CDATA[").append(ValueObjectUtil.byteArrayEncoder(fileOut.toByteArray())).append("]]></p>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"File\"><![CDATA[").append(ValueObjectUtil.byteArrayEncoder(fileOut.toByteArray())).append("]]></p>");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (byteIn != null) byteIn.close();
                    } catch (Exception e) {
                    }
                    try {
                        if (fileOut != null) fileOut.close();
                    } catch (Exception e) {
                    }
                }
            }
            return;
        } else if (type == String.class) {
            if (obj == null) {
                if (name == null) ret.append("<p s:nvl=\"true\" s:t=\"String\"/>"); else ret.append("<p n=\"").append(name).append("\" s:nvl=\"true\" s:t=\"String\"/>");
            } else {
                if (name == null) ret.append("<p s:t=\"String\"><![CDATA[").append(obj.toString()).append("]]></p>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"String\"><![CDATA[").append(obj.toString()).append("]]></p>");
            }
            return;
        } else if (type != null && List.class.isAssignableFrom(type)) {
            if (obj == null) {
                if (name == null) ret.append("<p s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>");
            } else {
                List datas = (List) obj;
                if (name == null) ret.append("<p s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\">"); else ret.append("<p n=\"").append(name).append("\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\">");
                ret.append("<l cmt=\"bean\">");
                Object value = null;
                for (int i = 0; i < datas.size(); i++) {
                    value = datas.get(i);
                    if (value == null) convertBeanObjectToXML(null, value, null, dateformat, ret); else convertBeanObjectToXML(null, value, value.getClass(), dateformat, ret);
                }
                ret.append("</l>");
                ret.append("</p>");
            }
        } else if (type != null && Set.class.isAssignableFrom(type)) {
            if (obj == null) {
                if (name == null) ret.append("<p s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>");
            } else {
                Set datas = (Set) obj;
                if (name == null) ret.append("<p s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\">"); else ret.append("<p n=\"").append(name).append("\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\">");
                ret.append("<s cmt=\"bean\">");
                Object value = null;
                Iterator itr = datas.iterator();
                while (itr.hasNext()) {
                    value = itr.next();
                    if (value == null) convertBeanObjectToXML(null, value, null, dateformat, ret); else convertBeanObjectToXML(null, value, value.getClass(), dateformat, ret);
                }
                ret.append("</s>");
                ret.append("</p>");
            }
        } else if (type != null && Map.class.isAssignableFrom(type)) {
            if (obj == null) {
                if (name == null) ret.append("<p s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>");
            } else {
                Map datas = (Map) obj;
                if (name == null) ret.append("<p s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\">"); else ret.append("<p n=\"").append(name).append("\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\">");
                ret.append("<m cmt=\"bean\">");
                Object value = null;
                Iterator itr = datas.entrySet().iterator();
                while (itr.hasNext()) {
                    Map.Entry entry = (Map.Entry) itr.next();
                    value = entry.getValue();
                    if (value == null) convertBeanObjectToXML(String.valueOf(entry.getKey()), value, null, dateformat, ret); else convertBeanObjectToXML(String.valueOf(entry.getKey()), value, value.getClass(), dateformat, ret);
                }
                ret.append("</m>");
                ret.append("</p>");
            }
        } else if (type != null && type.isArray()) {
            if (obj == null) {
                if (name == null) ret.append("<p s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>");
                return;
            } else {
                if (name == null) ret.append("<p s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\">"); else ret.append("<p n=\"").append(name).append("\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\">");
            }
            ret.append("<a cmt=\"").append(ValueObjectUtil.getComponentTypeName(type)).append("\">");
            Object value = null;
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                value = Array.get(obj, i);
                if (value == null) convertBeanObjectToXML(null, value, null, dateformat, ret); else convertBeanObjectToXML(null, value, value.getClass(), dateformat, ret);
            }
            ret.append("</a>");
            ret.append("</p>");
        } else {
            basicTypeCast(name, obj, type, dateformat, ret);
        }
    }

    /**
	 * Description:����������ת��
	 * 
	 * @param obj
	 * @param type
	 * @param toType
	 * @return Object
	 * @throws NoSupportTypeCastException
	 * @throws NumberFormatException
	 * @throws IntrospectionException
	 * @throws IOException 
	 * 
	 */
    private static final boolean basicTypeCast(String name, Object obj, Class type, String dateformat, Writer ret) throws NoSupportTypeCastException, NumberFormatException, IntrospectionException, IOException {
        if (obj == null) {
            if (name == null) ret.append("<p s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:nvl=\"true\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\"/>");
            return true;
        }
        type = obj.getClass();
        if (type == long.class || type == Long.class) {
            if (name == null) ret.append("<p s:t=\"long\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"long\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        }
        if (type == int.class || type == Integer.class) {
            if (name == null) ret.append("<p s:t=\"int\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"int\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        }
        if (type == float.class || type == Float.class) {
            if (name == null) ret.append("<p s:t=\"float\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"float\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        }
        if (type == short.class || type == Short.class) {
            if (name == null) ret.append("<p s:t=\"short\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"short\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        }
        if (type == double.class || type == Double.class) {
            if (name == null) ret.append("<p s:t=\"double\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"double\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        }
        if (type == char.class || type == Character.class) {
            if (name == null) ret.append("<p s:t=\"char\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"char\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        }
        if (type == boolean.class || type == Boolean.class) {
            if (name == null) ret.append("<p s:t=\"boolean\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"boolean\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        }
        if (type == byte.class || type == Byte.class) {
            if (name == null) ret.append("<p s:t=\"byte\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"byte\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        }
        if (type == String.class) {
            if (name == null) ret.append("<p s:t=\"String\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"String\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        } else if (java.util.Date.class.isAssignableFrom(type)) {
            long value = ((java.util.Date) obj).getTime();
            if (name == null) ret.append("<p s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\" v=\"").append(String.valueOf(value)).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\" v=\"").append(String.valueOf(value)).append("\"/>");
            return true;
        } else if (type == Class.class) {
            if (name == null) ret.append("<p s:t=\"Class\" v=\"").append(((Class) obj).getCanonicalName()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"Class\" v=\"").append(((Class) obj).getCanonicalName()).append("\"/>");
            return true;
        } else if (type.isEnum()) {
            if (name == null) ret.append("<p s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\" v=\"").append(obj.toString()).append("\"/>"); else ret.append("<p n=\"").append(name).append("\" s:t=\"").append(ValueObjectUtil.getSimpleTypeName(type)).append("\" v=\"").append(obj.toString()).append("\"/>");
            return true;
        } else {
            if (name == null) ret.append("<p cs=\"").append(obj.getClass().getCanonicalName()).append("\">"); else ret.append("<p n=\"").append(name).append("\" cs=\"").append(obj.getClass().getCanonicalName()).append("\">");
            if (StackTraceElement.class.isAssignableFrom(type)) appendStackTraceElementProperties(obj, type, dateformat, ret); else if (Throwable.class.isAssignableFrom(type)) appendThrowableProperties(obj, type, dateformat, ret); else appendBeanProperties(obj, type, dateformat, ret);
            ret.append("</p>");
            return true;
        }
    }

    private static void appendThrowableProperties(Object obj, Class type, String dateformat, Writer ret) throws IntrospectionException, IOException {
        BeanInfo beanInfo = Introspector.getBeanInfo(type);
        PropertyDescriptor[] attributes = beanInfo.getPropertyDescriptors();
        ret.append("<construction>");
        try {
            Object value = ValueObjectUtil.getValue(obj, "message");
            convertBeanObjectToXML("message", value, String.class, dateformat, ret);
            value = ValueObjectUtil.getValue(obj, "cause");
            if (value != null) {
                convertBeanObjectToXML("cause", value, value.getClass(), dateformat, ret);
            } else {
                convertBeanObjectToXML("cause", value, Throwable.class, dateformat, ret);
            }
        } catch (CurrentlyInCreationException e) {
            throw e;
        } catch (BeanInstanceException e) {
            throw e;
        } catch (Exception e) {
            throw new CurrentlyInCreationException("", e);
        }
        ret.append("</construction>");
        appendBeanProperties(obj, type, dateformat, ret, throwable_filterattributes);
    }

    private static void appendStackTraceElementProperties(Object obj, Class type, String dateformat, Writer ret) throws IntrospectionException, IOException {
        BeanInfo beanInfo = Introspector.getBeanInfo(type);
        PropertyDescriptor[] attributes = beanInfo.getPropertyDescriptors();
        ret.append("<construction>");
        try {
            Object value = ValueObjectUtil.getValue(obj, "className");
            convertBeanObjectToXML("declaringClass", value, String.class, dateformat, ret);
            value = ValueObjectUtil.getValue(obj, "methodName");
            convertBeanObjectToXML("methodName", value, String.class, dateformat, ret);
            value = ValueObjectUtil.getValue(obj, "fileName");
            convertBeanObjectToXML("fileName", value, String.class, dateformat, ret);
            value = ValueObjectUtil.getValue(obj, "lineNumber");
            convertBeanObjectToXML("lineNumber", value, int.class, dateformat, ret);
        } catch (CurrentlyInCreationException e) {
            throw e;
        } catch (BeanInstanceException e) {
            throw e;
        } catch (Exception e) {
            throw new CurrentlyInCreationException("", e);
        }
        ret.append("</construction>");
    }

    private static void appendBeanProperties(Object obj, Class type, String dateformat, Writer ret) throws IntrospectionException {
        appendBeanProperties(obj, type, dateformat, ret, null);
    }

    private static boolean isexclusive(String name, String[] filters) {
        if (filters == null || filters.length == 0) return false;
        for (String filter : filters) {
            if (name.equals(filter)) return true;
        }
        return false;
    }

    private static void appendBeanProperties(Object obj, Class type, String dateformat, Writer ret, String[] filters) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(type);
        PropertyDescriptor[] attributes = beanInfo.getPropertyDescriptors();
        for (int n = 0; n < attributes.length; n++) {
            PropertyDescriptor propertyDescriptor = attributes[n];
            String attrName = propertyDescriptor.getName();
            if (attrName.equals("class")) continue; else if (isexclusive(attrName, filters)) {
                continue;
            }
            Class ptype = propertyDescriptor.getPropertyType();
            Method readmethod = propertyDescriptor.getReadMethod();
            Method writermethod = propertyDescriptor.getWriteMethod();
            if (readmethod == null || writermethod == null) continue;
            try {
                Object value = readmethod.invoke(obj);
                if (value != null) ptype = value.getClass();
                convertBeanObjectToXML(attrName, value, ptype, dateformat, ret);
            } catch (IllegalArgumentException e) {
                throw new CurrentlyInCreationException("", e);
            } catch (IllegalAccessException e) {
                throw new CurrentlyInCreationException("", e);
            } catch (InvocationTargetException e) {
                throw new CurrentlyInCreationException("", e);
            } catch (CurrentlyInCreationException e) {
                throw e;
            } catch (BeanInstanceException e) {
                throw e;
            } catch (Exception e) {
                throw new CurrentlyInCreationException("", e);
            }
        }
    }

    private static void convertParams(Writer ret, Object[] params, Class[] paramTypes, String dateformat) throws NoSupportTypeCastException, NumberFormatException, IllegalArgumentException, IntrospectionException, IOException {
        for (int i = 0; i < params.length; i++) {
            ObjectSerializable.convertBeanObjectToXML(null, params[i], paramTypes[i], dateformat, ret);
        }
    }
}
