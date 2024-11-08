package net.sourceforge.xmote.encoding;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Iterator;
import org.jdom.Element;

/**
 * Encoding for Java Beans.
 *
 * @author Jason Rush
 */
public class ObjectEncoding extends DefaultEncoding {

    private static final Class<?> CLASS = Object.class;

    private static final String NAME = "object";

    public ObjectEncoding() {
        super(CLASS, NAME);
    }

    /**
   * @throws ClassNotFoundException
   * @see net.sourceforge.xmote.encoding.DefaultEncoding#getComponentClass(java.lang.String)
   */
    public Class<?> getComponentClass(String componentType) throws EncodingException {
        if (componentType.startsWith("object:")) {
            componentType = componentType.substring(7);
            try {
                return Class.forName(componentType);
            } catch (Exception e) {
                throw new EncodingException("Failed to create instance of class: " + componentType);
            }
        }
        return super.getComponentClass(componentType);
    }

    /**
   * @see net.sourceforge.xmote.encoding.DefaultEncoding#getComponentType(java.lang.Class)
   */
    public String getComponentType(Class<?> componentClass) throws EncodingException {
        return super.getComponentType(componentClass) + ":" + componentClass.getName();
    }

    /**
   * @see net.sourceforge.xmote.encoding.DefaultEncoding#canDecode(java.lang.String)
   */
    public boolean canDecode(String name) {
        if (name.startsWith("object:")) {
            return true;
        }
        return super.canDecode(name);
    }

    /**
   * @see net.sourceforge.xmote.encoding.DefaultEncoding#encode(java.lang.Object)
   */
    public Element encode(Object object) throws EncodingException {
        Element root = super.encode(object);
        if (object != null) {
            Class<?> clazz = object.getClass();
            root.setAttribute("type", clazz.getName());
            PropertyDescriptor[] propertyDescriptors;
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(clazz, Object.class);
                propertyDescriptors = beanInfo.getPropertyDescriptors();
            } catch (IntrospectionException e) {
                throw new EncodingException("Failed to introspect object: " + e.getMessage());
            }
            EncodingFactory encodingFactory = EncodingFactory.getInstance();
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                Method readMethod = descriptor.getReadMethod();
                Method writeMethod = descriptor.getReadMethod();
                if (readMethod == null || writeMethod == null) {
                    continue;
                }
                Object value;
                try {
                    value = readMethod.invoke(object, new Object[] {});
                } catch (Exception e) {
                    throw new EncodingException("Failed to invoke get method: " + e.getMessage());
                }
                Element element = encodingFactory.encode(value);
                element.setAttribute("name", descriptor.getName());
                root.addContent(element);
            }
        }
        return root;
    }

    /**
   * @see net.sourceforge.xmote.encoding.DefaultEncoding#decode(org.jdom.Element)
   */
    public Object decode(Element root) throws EncodingException {
        String nullValue = root.getAttributeValue("null");
        if (nullValue != null && Boolean.parseBoolean(nullValue)) {
            return null;
        }
        String className = root.getAttributeValue("type");
        Class<?> clazz;
        Object object;
        try {
            clazz = Class.forName(className);
            object = clazz.newInstance();
        } catch (Exception e) {
            throw new EncodingException("Failed to create instance of class: " + className);
        }
        PropertyDescriptor[] propertyDescriptors;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz, Object.class);
            propertyDescriptors = beanInfo.getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new EncodingException("Failed to introspect object: " + e.getMessage());
        }
        EncodingFactory encodingFactory = EncodingFactory.getInstance();
        Iterator<?> iterator = root.getChildren().iterator();
        while (iterator.hasNext()) {
            Element element = (Element) iterator.next();
            String name = element.getAttributeValue("name");
            Object value = encodingFactory.decode(element);
            Method writeMethod = null;
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                if (name.equals(descriptor.getName())) {
                    writeMethod = descriptor.getWriteMethod();
                    break;
                }
            }
            if (writeMethod == null) {
                throw new EncodingException("Failed to find set method for property: " + name);
            }
            try {
                writeMethod.invoke(object, new Object[] { value });
            } catch (Exception e) {
                throw new EncodingException("Failed to invoke set method: " + e.getMessage());
            }
        }
        return object;
    }
}
