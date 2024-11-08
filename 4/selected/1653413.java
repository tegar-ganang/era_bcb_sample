package org.jfree.xml.generator;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import org.jfree.util.HashNMap;
import org.jfree.xml.generator.model.ClassDescription;
import org.jfree.xml.generator.model.DescriptionModel;
import org.jfree.xml.generator.model.MultiplexMappingInfo;
import org.jfree.xml.generator.model.PropertyInfo;
import org.jfree.xml.generator.model.PropertyType;
import org.jfree.xml.generator.model.TypeInfo;
import org.jfree.xml.util.BasicTypeSupport;

/**
 * A model builder.  This class performs the work of creating a class description model from
 * a set of source files.
 */
public final class ModelBuilder {

    /** The single instance. */
    private static ModelBuilder instance;

    /**
     * Returns the single instance of this class.
     * 
     * @return the single instance of this class.
     */
    public static ModelBuilder getInstance() {
        if (instance == null) {
            instance = new ModelBuilder();
        }
        return instance;
    }

    /** The handler mapping. */
    private Properties handlerMapping;

    /**
     * Creates a single instance.
     */
    private ModelBuilder() {
        this.handlerMapping = new Properties();
    }

    /**
     * Adds attribute handlers.
     * 
     * @param p  the handlers.
     */
    public void addAttributeHandlers(final Properties p) {
        this.handlerMapping.putAll(p);
    }

    /**
     * Builds a model from the classes provided by the {@link SourceCollector}. 
     * <P>
     * The {@link DescriptionGenerator} class invokes this.
     * 
     * @param c  the source collector.
     * @param model  the model under construction (<code>null</code> permitted).
     * 
     * @return The completed model.
     */
    public DescriptionModel buildModel(final SourceCollector c, DescriptionModel model) {
        Class[] classes = c.getClasses();
        if (model == null) {
            model = new DescriptionModel();
        }
        while (classes.length != 0) {
            classes = fillModel(classes, model);
        }
        fillSuperClasses(model);
        final Class[] baseClasses = findElementTypes(model);
        final HashNMap classMap = new HashNMap();
        for (int i = 0; i < baseClasses.length; i++) {
            final Class base = baseClasses[i];
            for (int j = 0; j < baseClasses.length; j++) {
                final Class child = baseClasses[j];
                if (Modifier.isAbstract(child.getModifiers())) {
                    continue;
                }
                if (base.isAssignableFrom(child)) {
                    classMap.add(base, child);
                }
            }
        }
        final Iterator keys = classMap.keys();
        while (keys.hasNext()) {
            final Class base = (Class) keys.next();
            final Class[] childs = (Class[]) classMap.toArray(base, new Class[0]);
            if (childs.length < 2) {
                continue;
            }
            boolean isNew = false;
            MultiplexMappingInfo mmi = model.getMappingModel().lookupMultiplexMapping(base);
            final ArrayList typeInfoList;
            if (mmi == null) {
                mmi = new MultiplexMappingInfo(base);
                typeInfoList = new ArrayList();
                isNew = true;
            } else {
                typeInfoList = new ArrayList(Arrays.asList(mmi.getChildClasses()));
            }
            for (int i = 0; i < childs.length; i++) {
                final TypeInfo typeInfo = new TypeInfo(childs[i].getName(), childs[i]);
                if (!typeInfoList.contains(typeInfo)) {
                    typeInfoList.add(typeInfo);
                }
            }
            mmi.setChildClasses((TypeInfo[]) typeInfoList.toArray(new TypeInfo[0]));
            if (isNew) {
                model.getMappingModel().addMultiplexMapping(mmi);
            }
        }
        return model;
    }

    private Class[] findElementTypes(final DescriptionModel model) {
        final ArrayList baseClasses = new ArrayList();
        for (int i = 0; i < model.size(); i++) {
            final ClassDescription cd = model.get(i);
            if (!baseClasses.contains(cd.getObjectClass())) {
                baseClasses.add(cd.getObjectClass());
            }
            final PropertyInfo[] properties = cd.getProperties();
            for (int p = 0; p < properties.length; p++) {
                if (!properties[p].getPropertyType().equals(PropertyType.ELEMENT)) {
                    continue;
                }
                final Class type = properties[p].getType();
                if (baseClasses.contains(type)) {
                    continue;
                }
                if (Modifier.isFinal(type.getModifiers())) {
                    continue;
                }
                baseClasses.add(type);
            }
        }
        return (Class[]) baseClasses.toArray(new Class[baseClasses.size()]);
    }

    /**
     * Fills the super class for all object descriptions of the model. The
     * super class is only filled, if the object's super class is contained
     * in the model.
     *
     * @param model the model which should get its superclasses updated.
     */
    private void fillSuperClasses(final DescriptionModel model) {
        for (int i = 0; i < model.size(); i++) {
            final ClassDescription cd = model.get(i);
            final Class parent = cd.getObjectClass().getSuperclass();
            if (parent == null) {
                continue;
            }
            final ClassDescription superCD = model.get(parent);
            if (superCD != null) {
                cd.setSuperClass(superCD.getObjectClass());
            }
        }
    }

    /**
     * Updates the model to contain the given classes.
     *
     * @param classes  a list of classes which should be part of the model.
     * @param model  the model which is updated
     * 
     * @return A list of super classes which should also be contained in the model.
     */
    private Class[] fillModel(final Class[] classes, final DescriptionModel model) {
        final ArrayList superClasses = new ArrayList();
        for (int i = 0; i < classes.length; i++) {
            Class superClass = classes[i].getSuperclass();
            if (superClass != null) {
                if (!Object.class.equals(superClass) && !contains(classes, superClass) && !superClasses.contains(superClass)) {
                    superClasses.add(superClass);
                }
            } else {
                superClass = Object.class;
            }
            try {
                final BeanInfo bi = Introspector.getBeanInfo(classes[i], superClass);
                final ClassDescription parent = model.get(classes[i]);
                final ClassDescription cd = createClassDescription(bi, parent);
                if (cd != null) {
                    model.addClassDescription(cd);
                }
            } catch (IntrospectionException ie) {
            }
        }
        return (Class[]) superClasses.toArray(new Class[0]);
    }

    /**
     * Creates a {@link ClassDescription} object for the specified bean info.
     * 
     * @param beanInfo  the bean info.
     * @param parent  the parent class description.
     * 
     * @return The class description.
     */
    private ClassDescription createClassDescription(final BeanInfo beanInfo, final ClassDescription parent) {
        final PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
        final ArrayList properties = new ArrayList();
        for (int i = 0; i < props.length; i++) {
            final PropertyDescriptor propertyDescriptor = props[i];
            PropertyInfo pi;
            if (parent != null) {
                pi = parent.getProperty(propertyDescriptor.getName());
                if (pi != null) {
                    properties.add(pi);
                    continue;
                }
            }
            if (props[i] instanceof IndexedPropertyDescriptor) {
            } else {
                pi = createSimplePropertyInfo(props[i]);
                if (pi != null) {
                    properties.add(pi);
                }
            }
        }
        final PropertyInfo[] propArray = (PropertyInfo[]) properties.toArray(new PropertyInfo[properties.size()]);
        final ClassDescription cd;
        if (parent != null) {
            cd = parent;
        } else {
            cd = new ClassDescription(beanInfo.getBeanDescriptor().getBeanClass());
            cd.setDescription(beanInfo.getBeanDescriptor().getShortDescription());
        }
        cd.setProperties(propArray);
        return cd;
    }

    /**
     * Checks, whether the given method can be called from the generic object factory.
     *
     * @param method the method descriptor
     * @return true, if the method is not null and public, false otherwise.
     */
    public static boolean isValidMethod(final Method method) {
        if (method == null) {
            return false;
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        return true;
    }

    /**
     * Creates a {@link PropertyInfo} object from a {@link PropertyDescriptor}.
     * 
     * @param pd  the property descriptor.
     * 
     * @return the property info (<code>null</code> possible).
     */
    public PropertyInfo createSimplePropertyInfo(final PropertyDescriptor pd) {
        final boolean readMethod = isValidMethod(pd.getReadMethod());
        final boolean writeMethod = isValidMethod(pd.getWriteMethod());
        if (!writeMethod || !readMethod) {
            return null;
        }
        final PropertyInfo pi = new PropertyInfo(pd.getName(), pd.getPropertyType());
        pi.setConstrained(pd.isConstrained());
        pi.setDescription(pd.getShortDescription());
        pi.setNullable(true);
        pi.setPreserve(false);
        pi.setReadMethodAvailable(readMethod);
        pi.setWriteMethodAvailable(writeMethod);
        pi.setXmlName(pd.getName());
        if (isAttributeProperty(pd.getPropertyType())) {
            pi.setPropertyType(PropertyType.ATTRIBUTE);
            pi.setXmlHandler(getHandlerClass(pd.getPropertyType()));
        } else {
            pi.setPropertyType(PropertyType.ELEMENT);
        }
        return pi;
    }

    /**
     * Checks, whether the given class can be handled as attribute.
     * All primitive types can be attributes as well as all types which have
     * a custom attribute handler defined.
     *
     * @param c the class which should be checked
     * @return true, if the class can be handled as attribute, false otherwise.
     */
    private boolean isAttributeProperty(final Class c) {
        if (BasicTypeSupport.isBasicDataType(c)) {
            return true;
        }
        return this.handlerMapping.containsKey(c.getName());
    }

    /**
     * Returns the class name for the attribute handler for a property of the specified class.
     *
     * @param c the class for which to search an attribute handler
     * @return the handler class or null, if this class cannot be handled
     * as attribute.
     */
    private String getHandlerClass(final Class c) {
        if (BasicTypeSupport.isBasicDataType(c)) {
            final String handler = BasicTypeSupport.getHandlerClass(c);
            if (handler != null) {
                return handler;
            }
        }
        return this.handlerMapping.getProperty(c.getName());
    }

    /**
     * Checks, whether the class <code>c</code> is contained in the given
     * class array.
     *
     * @param cAll the list of all classes
     * @param c the class to be searched
     * @return true, if the class is contained in the array, false otherwise.
     */
    private boolean contains(final Class[] cAll, final Class c) {
        for (int i = 0; i < cAll.length; i++) {
            if (cAll[i].equals(c)) {
                return true;
            }
        }
        return false;
    }
}
