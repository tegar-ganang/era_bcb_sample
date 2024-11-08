package org.siberia.type.info;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.siberia.ResourceLoader;
import org.siberia.TypeInformationProvider;
import org.siberia.exception.ResourceException;
import org.siberia.type.SibType;
import org.siberia.type.annotation.bean.Bean;
import org.siberia.type.annotation.bean.BeanConstants;
import org.siberia.type.annotation.bean.BeanMethod;
import org.siberia.type.annotation.bean.BeanProperty;

/**
 *
 *  BeanInfo for SibType.
 *  This kind of BeanInfo allow to deal with annotations
 *
 * @author alexis
 */
public class AnnotationBasedBeanInfo extends AbstractBeanInfo {

    /** logger */
    private static Logger logger = Logger.getLogger(AnnotationBasedBeanInfo.class);

    /** automatic recognition semaphors */
    private boolean automaticBeanDescriptor = false;

    private boolean automaticPropertiesDescriptor = false;

    private boolean automaticMethodsDescriptor = false;

    private boolean automaticBeaDescriptor = false;

    /** bean descriptor */
    private BeanDescriptor descriptor = null;

    /** property descriptors */
    private PropertyDescriptor[] properties = null;

    /** method descriptors */
    private MethodDescriptor[] methods = null;

    /** class related with this bean info */
    private Class relatedClass = null;

    /** reference to the Type */
    private SoftReference<Bean> typeRef = new SoftReference<Bean>(null);

    /** tell if the class contains a Type annotation */
    private Boolean containsTypeAnnotation = null;

    /** array of property name which property will be taken into account when adding PropertyDescriptor */
    private String[] allowedPropertyNames = null;

    /** indicate if the mecanism that filter properties is activated */
    private boolean allowedPropertiesMecanActivated = false;

    /** Creates a new instance of AbstractSibTypeBeanInfo */
    public AnnotationBasedBeanInfo() {
    }

    /** Creates a new instance of AbstractSibTypeBeanInfo
     *  @param c the class related with this bean info
     */
    protected AnnotationBasedBeanInfo(Class c) {
        this();
        this.setRelatedClass(c);
    }

    /** set the class related to this BeanInfo
     *  @param cl a Class
     *  @exception IllegalArgumentException if cl is null
     */
    public void setRelatedClass(Class cl) {
        if (cl == null) {
            throw new IllegalArgumentException("class not null must be provided");
        }
        this.relatedClass = cl;
    }

    /** return the class related to this BeanInfo
     *  @return a Class
     */
    public Class getRelatedClass() {
        if (this.relatedClass == null) throw new UnsupportedOperationException("related class is not initialized");
        return this.relatedClass;
    }

    /** set the names of properties which will be processed to give PropertyDescriptor
     *  @param allowedPropertyNames an array of String
     *
     */
    public void setAllowedPropertiesByName(String[] allowedPropertyNames) {
        this.allowedPropertyNames = allowedPropertyNames;
    }

    /** return the names of properties which will be processed to give PropertyDescriptor
     *  @return an array of String
     */
    public String[] getAllowedPropertiesByName() {
        return this.allowedPropertyNames;
    }

    /** indicate if the mecanism that filter properties is activated
     *  @return a boolean
     */
    public boolean isAllowedPropertiesMecanismActivated() {
        return allowedPropertiesMecanActivated;
    }

    /** tells if the mecanism that filter properties is activated
     *  @param allowedPropertiesMecanismActivated true si activate this mecanism
     */
    public void setAllowedPropertiesMecanismActivated(boolean allowedPropertiesMecanismActivated) {
        this.allowedPropertiesMecanActivated = allowedPropertiesMecanismActivated;
    }

    /** return the bean annotation related with the related class
     *  @return a Bean or null if not found
     */
    protected Bean getBeanAnnotation() {
        Bean t = this.typeRef.get();
        if (t == null) {
            if (this.containsTypeAnnotation == null || (this.containsTypeAnnotation.booleanValue())) {
                Object o = this.getRelatedClass().getAnnotation(Bean.class);
                if (o instanceof Bean) {
                    t = (Bean) o;
                    this.typeRef = new SoftReference<Bean>(t);
                }
            }
            if (this.containsTypeAnnotation == null) {
                this.containsTypeAnnotation = t != null;
            }
        }
        return t;
    }

    /** return the BeanProperty annotation related with the related class
     *  @param field a Field
     *  @return a BeanProperty or null if not found
     */
    protected BeanProperty getBeanPropertyAnnotation(Field field) {
        BeanProperty prop = null;
        if (field != null) {
            Object o = field.getAnnotation(BeanProperty.class);
            if (o instanceof BeanProperty) {
                prop = (BeanProperty) o;
            }
        }
        return prop;
    }

    /** return the BeanMethod annotation related with the related class
     *  @param method a Method
     *  @return a BeanMethod or null if not found
     */
    protected BeanMethod getBeanMethodAnnotation(Method method) {
        BeanMethod meth = null;
        if (method != null) {
            Object o = method.getAnnotation(BeanMethod.class);
            if (o instanceof BeanMethod) {
                meth = (BeanMethod) o;
            }
        }
        return meth;
    }

    /** reverse the order of the element in the array
     *  the first item will be the last...
     *  @param array an array
     */
    private void reverseArray(Object[] array) {
        if (array != null) {
            if (array.length > 1) {
                Object tmp = null;
                int length = array.length % 2;
                for (int i = 0; i < length; i++) {
                    tmp = array[i];
                    int reflectIndex = array.length - i - 1;
                    array[i] = array[reflectIndex];
                    array[reflectIndex] = tmp;
                }
            }
        }
    }

    /** return true if the BeanDescriptor should be obtained automatically
     *  @return a boolean
     */
    public boolean automaticBeanDescriptor() {
        return automaticBeanDescriptor;
    }

    /** indicate if the BeanDescriptor should be obtained automatically
     *  @param automaticBeanDescriptor true if the BeanDescriptor should be obtained automatically
     */
    public void setAutomaticBeanDescriptor(boolean automaticBeanDescriptor) {
        this.automaticBeanDescriptor = automaticBeanDescriptor;
        if (automaticBeanDescriptor && this.descriptor != null) this.descriptor = null;
    }

    /**
     * Gets the beans <code>BeanDescriptor</code>.
     * 
     * 
     * @return A BeanDescriptor providing overall information about
     * the bean, such as its displayName, its customizer, etc.  May
     * return null if the information should be obtained by automatic
     * analysis.
     */
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor desc = null;
        if (!this.automaticBeanDescriptor()) {
            if (this.descriptor == null) {
                this.descriptor = this.createBeanDescriptor();
            }
            desc = this.descriptor;
        }
        return desc;
    }

    /** methods that is called to create the BeanDescriptor
     *  this method is only called once
     *  @return a BeanDescriptor
     */
    protected BeanDescriptor createBeanDescriptor() {
        BeanDescriptor descriptor = new BeanDescriptor(this.getClass());
        Bean type = this.getBeanAnnotation();
        if (type != null) {
            this.descriptor.setDisplayName(TypeInformationProvider.getDisplayName(this.getRelatedClass()));
            this.descriptor.setName(type.name());
            this.descriptor.setExpert(type.expert());
            this.descriptor.setHidden(type.hidden());
            this.descriptor.setPreferred(type.preferred());
            this.descriptor.setShortDescription(TypeInformationProvider.getShortDescription(this.getRelatedClass()));
        } else {
            this.descriptor.setName("unknown");
            this.descriptor.setDisplayName("unknown");
        }
        return descriptor;
    }

    /** return true if the PropertyDescriptor s should be obtained automatically
     *  @return a boolean
     */
    public boolean automaticPropertiesDescriptor() {
        return automaticPropertiesDescriptor;
    }

    /** indicate if the PropertyDescriptor s should be obtained automatically
     *  @param automaticPropertiesDescriptor true if the PropertyDescriptor s should be obtained automatically
     */
    public void setAutomaticPropertiesDescriptor(boolean automaticPropertiesDescriptor) {
        this.automaticPropertiesDescriptor = automaticPropertiesDescriptor;
        if (automaticPropertiesDescriptor && this.properties != null) this.properties = null;
    }

    /**
     * Gets the beans <code>PropertyDescriptor</code>s.
     * 
     * 
     * @return An array of PropertyDescriptors describing the editable
     * properties supported by this bean.  May return null if the
     * information should be obtained by automatic analysis.
     * <p>
     * If a property is indexed, then its entry in the result array will
     * belong to the IndexedPropertyDescriptor subclass of PropertyDescriptor.
     * A client of getPropertyDescriptors can use "instanceof" to check
     * if a given PropertyDescriptor is an IndexedPropertyDescriptor.
     */
    public PropertyDescriptor[] getPropertyDescriptors() {
        logger.debug("calling getPropertyDescriptors on " + AnnotationBasedBeanInfo.class);
        PropertyDescriptor[] props = null;
        if (!this.automaticPropertiesDescriptor()) {
            if (this.properties == null) {
                Bean bean = this.getBeanAnnotation();
                if (bean != null) {
                    Class current = this.getRelatedClass();
                    Class limitClass = bean.propertiesClassLimit();
                    if (limitClass == null) limitClass = Object.class;
                    logger.debug("limitClass : " + limitClass);
                    logger.debug("current : " + current);
                    List<PropertyDescriptor> list = null;
                    list = this.createPropertiesDescriptor(current, limitClass);
                    logger.debug("getting " + list.size() + " properties descriptor for class " + current);
                    if (this.getBeanInfoCategory().equals(BeanInfoCategory.ALL)) {
                        if (list == null) {
                            list = new ArrayList<PropertyDescriptor>(1);
                        }
                        try {
                            PropertyDescriptor identityHashCodeDescriptor = new PropertyDescriptor(SibType.PROPERTY_IDENTITY_HASHCODE, current.getMethod("getIdentityHashCode", (Class[]) null), null);
                            System.out.println("adding PropertyDescriptor for " + SibType.PROPERTY_IDENTITY_HASHCODE);
                            list.add(identityHashCodeDescriptor);
                        } catch (SecurityException ex) {
                            ex.printStackTrace();
                        } catch (NoSuchMethodException ex) {
                            ex.printStackTrace();
                        } catch (IntrospectionException ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (list == null || list.size() == 0) {
                        this.properties = new PropertyDescriptor[] {};
                    } else {
                        this.properties = (PropertyDescriptor[]) list.toArray(new PropertyDescriptor[list.size()]);
                    }
                } else {
                    this.properties = new PropertyDescriptor[] {};
                }
            }
            props = this.properties;
        }
        logger.debug("before reverseArray");
        this.reverseArray(props);
        logger.debug("after reverseArray");
        return props;
    }

    /** methods that is called to create the BeanDescriptor
     *  this method is only called once
     *  @param c the class used to create PropertyDescriptor
     *  @param limitClass the limit class
     *  @return a list of PropertyDescriptor that have to be non null
     */
    protected List<PropertyDescriptor> createPropertiesDescriptor(Class c, Class limitClass) {
        List<PropertyDescriptor> propertiesList = null;
        Set<String> propertiesName = new HashSet<String>();
        Class pointer = c;
        while (pointer != null && !pointer.equals(Object.class) && (limitClass == null || (pointer.equals(limitClass) || limitClass.isAssignableFrom(pointer)))) {
            Field[] fields = pointer.getDeclaredFields();
            if (fields != null) {
                for (int i = 0; i < fields.length; i++) {
                    PropertyDescriptor currentDesc = this.createPropertyDescriptor(fields[i]);
                    if (currentDesc != null && !propertiesName.contains(currentDesc.getName())) {
                        if (propertiesList == null) {
                            propertiesList = new ArrayList<PropertyDescriptor>();
                        }
                        propertiesList.add(currentDesc);
                        propertiesName.add(currentDesc.getName());
                    }
                }
            }
            pointer = pointer.getSuperclass();
        }
        if (propertiesList == null) propertiesList = Collections.EMPTY_LIST;
        for (int i = 0; i < propertiesList.size(); i++) {
            logger.debug("creating PropertyDescritor '" + propertiesList.get(i).getName() + "' for class " + c);
        }
        return propertiesList;
    }

    /** method that is called after building the PropertyDescriptor according to annotation information
     *  Overwrite this method to modify the information of the PropertyDescriptor
     *  @param descriptor a PropertyDescriptor
     */
    protected void postProcessProperty(PropertyDescriptor descriptor) {
    }

    /** method that is called before building the PropertyDescriptor according to annotation information
     *  Overwrite this method to modify the information of the PropertyDescriptor
     *  @param descriptor a PropertyDescriptor
     */
    protected void preProcessProperty(PropertyDescriptor descriptor) {
    }

    /** return the method to use as read method for a Property
     *  @param field a Field
     *  @param annotation the BeanProperty annotation
     */
    protected Method getReadMethod(Field field, BeanProperty annotation) {
        Method readMethod = null;
        if (annotation != null) {
            try {
                String methodName = annotation.readMethodName();
                Class[] args = annotation.readMethodParametersClass();
                readMethod = this.getRelatedClass().getMethod(methodName, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return readMethod;
    }

    /** return the method to use as write method for a Property
     *  @param field a Field
     *  @param annotation the BeanProperty annotation
     */
    protected Method getWriteMethod(Field field, BeanProperty annotation) {
        Method writeMethod = null;
        if (annotation != null) {
            try {
                String methodName = annotation.writeMethodName();
                Class[] args = annotation.writeMethodParametersClass();
                writeMethod = this.getRelatedClass().getMethod(methodName, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return writeMethod;
    }

    /** method that create a PropertyDescriptor for a given Field
     *  @param field a Field
     *  @return a PropertyDescriptor or null if failed or forbidden
     */
    protected PropertyDescriptor createPropertyDescriptor(Field field) {
        PropertyDescriptor descriptor = null;
        if (field != null) {
            if (this.shouldGenerate(field)) {
                BeanProperty property = this.getBeanPropertyAnnotation(field);
                if (property != null && !(!this.considerExpertDescriptor() && property.expert())) {
                    String name = property.name();
                    if (name != null) {
                        boolean generate = true;
                        if (this.getBeanInfoCategory().equals(BeanInfoCategory.BASICS) && this.isAllowedPropertiesMecanismActivated()) {
                            generate = false;
                            if (this.getAllowedPropertiesByName() != null) {
                                for (int i = 0; i < this.getAllowedPropertiesByName().length; i++) {
                                    String current = this.getAllowedPropertiesByName()[i];
                                    if (name.equals(current)) {
                                        generate = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (generate) {
                            try {
                                Method readMethod = this.getReadMethod(field, property);
                                Method writeMethod = this.getWriteMethod(field, property);
                                descriptor = new PropertyDescriptor(name, readMethod, writeMethod);
                                this.preProcessProperty(descriptor);
                                descriptor.setName(property.name());
                                descriptor.setDisplayName(TypeInformationProvider.getPropertyDisplayName(this.getRelatedClass(), field.getName()));
                                descriptor.setExpert(property.expert());
                                descriptor.setHidden(property.hidden());
                                descriptor.setPreferred(property.preferred());
                                descriptor.setShortDescription(TypeInformationProvider.getPropertyShortDescription(this.getRelatedClass(), field.getName()));
                                descriptor.setBound(property.bound());
                                descriptor.setConstrained(property.constrained());
                                this.postProcessProperty(descriptor);
                            } catch (IntrospectionException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } else {
                    }
                }
            }
        }
        return descriptor;
    }

    /** return true if the MethodDescriptor s should be obtained automatically
     *  @return a boolean
     */
    public boolean automaticMethodsDescriptor() {
        return automaticMethodsDescriptor;
    }

    /** indicate if the MethodDescriptor s should be obtained automatically
     *  @param automaticMethodsDescriptor true if the MethodDescriptor s should be obtained automatically
     */
    public void setAutomaticMethodsDescriptor(boolean automaticMethodsDescriptor) {
        this.automaticMethodsDescriptor = automaticMethodsDescriptor;
        if (automaticMethodsDescriptor && this.methods != null) this.methods = null;
    }

    /**
     * Gets the beans <code>MethodDescriptor</code>s.
     * 
     * 
     * @return An array of MethodDescriptors describing the externally
     * visible methods supported by this bean.  May return null if
     * the information should be obtained by automatic analysis.
     */
    public MethodDescriptor[] getMethodDescriptors() {
        MethodDescriptor[] meths = null;
        if (!this.automaticMethodsDescriptor()) {
            if (this.properties == null) {
                Bean bean = this.getBeanAnnotation();
                if (bean != null) {
                    Class current = this.getRelatedClass();
                    Class limitClass = bean.methodsClassLimit();
                    if (limitClass == null) limitClass = Object.class;
                    List<MethodDescriptor> list = null;
                    while (limitClass.isAssignableFrom(current)) {
                        List<MethodDescriptor> currentList = this.createMethodsDescriptor(current);
                        if (list == null) {
                            if (currentList != Collections.EMPTY_LIST) list = currentList;
                        } else {
                            list.addAll(currentList);
                        }
                    }
                    if (list == null || list.size() == 0) {
                        this.methods = new MethodDescriptor[] {};
                    } else {
                        this.methods = (MethodDescriptor[]) list.toArray(new MethodDescriptor[] {});
                    }
                } else {
                    this.methods = new MethodDescriptor[] {};
                }
            }
            meths = this.methods;
        }
        return new MethodDescriptor[] {};
    }

    /** methods that is called a MethodDescriptor list
     *  this method is only called once
     *  @param c the class used to create MethodDescriptor
     *  @return a list of MethodDescriptor that have to be non null
     */
    protected List<MethodDescriptor> createMethodsDescriptor(Class c) {
        List<MethodDescriptor> methodList = null;
        Class pointer = c;
        while (pointer != null && !pointer.equals(Object.class)) {
            Method[] methods = pointer.getDeclaredMethods();
            if (methods != null) {
                for (int i = 0; i < methods.length; i++) {
                    MethodDescriptor currentDesc = this.createMethodDescriptor(methods[i]);
                    if (currentDesc != null) {
                        if (methodList == null) methodList = new ArrayList<MethodDescriptor>();
                        methodList.add(currentDesc);
                    }
                }
            }
        }
        if (methodList == null) methodList = Collections.EMPTY_LIST;
        return methodList;
    }

    /** method that create a MethodDescriptor for a given Method
     *  @param method a Method
     *  @return a MethodDescriptor or null if failed or forbidden
     */
    protected MethodDescriptor createMethodDescriptor(Method method) {
        MethodDescriptor descriptor = null;
        if (method != null) {
            if (this.shouldGenerate(method)) {
                BeanMethod methodAnnotation = this.getBeanMethodAnnotation(method);
                if (methodAnnotation != null) {
                }
            }
        }
        return descriptor;
    }

    /**
     * Gets the beans <code>EventSetDescriptor</code>s.
     * 
     * 
     * @return An array of EventSetDescriptors describing the kinds of 
     * events fired by this bean.  May return null if the information
     * should be obtained by automatic analysis.
     */
    public EventSetDescriptor[] getEventSetDescriptors() {
        return new EventSetDescriptor[] {};
    }

    /**
     * A bean may have a "default" property that is the property that will
     * mostly commonly be initially chosen for update by human's who are 
     * customizing the bean.
     * 
     * @return Index of default property in the PropertyDescriptor array
     * 		returned by getPropertyDescriptors.
     * <P>	Returns -1 if there is no default property.
     */
    public int getDefaultPropertyIndex() {
        return -1;
    }

    /**
     * A bean may have a "default" event that is the event that will
     * mostly commonly be used by humans when using the bean. 
     * 
     * @return Index of default event in the EventSetDescriptor array
     * 		returned by getEventSetDescriptors.
     * <P>	Returns -1 if there is no default event.
     */
    public int getDefaultEventIndex() {
        return -1;
    }

    /**
     * This method allows a BeanInfo object to return an arbitrary collection
     * of other BeanInfo objects that provide additional information on the
     * current bean.
     * <P>
     * If there are conflicts or overlaps between the information provided
     * by different BeanInfo objects, then the current BeanInfo takes precedence
     * over the getAdditionalBeanInfo objects, and later elements in the array
     * take precedence over earlier ones.
     * 
     * @return an array of BeanInfo objects.  May return null.
     */
    public BeanInfo[] getAdditionalBeanInfo() {
        return null;
    }

    /**
     * This method returns an image object that can be used to
     * represent the bean in toolboxes, toolbars, etc.   Icon images
     * will typically be GIFs, but may in future include other formats.
     * <p>
     * Beans aren't required to provide icons and may return null from
     * this method.
     * <p>
     * There are four possible flavors of icons (16x16 color,
     * 32x32 color, 16x16 mono, 32x32 mono).  If a bean choses to only
     * support a single icon we recommend supporting 16x16 color.
     * <p>
     * We recommend that icons have a "transparent" background
     * so they can be rendered onto an existing background.
     * 
     * @param iconKind  The kind of icon requested.  This should be
     *    one of the constant values ICON_COLOR_16x16, ICON_COLOR_32x32, 
     *    ICON_MONO_16x16, or ICON_MONO_32x32.
     * @return An image object representing the requested icon.  May
     *    return null if no suitable icon is available.
     */
    public Image getIcon(int iconKind) {
        Image img = null;
        String rcPath = null;
        Bean type = this.getBeanAnnotation();
        if (this.isPluginContextActivated()) {
            if (type != null) {
                switch(iconKind) {
                    case BeanInfo.ICON_MONO_16x16:
                        rcPath = TypeInformationProvider.getIconInformation(this.getRelatedClass(), BeanConstants.BEAN_PLUGIN_ICON_MONO_16);
                        break;
                    case BeanInfo.ICON_MONO_32x32:
                        rcPath = TypeInformationProvider.getIconInformation(this.getRelatedClass(), BeanConstants.BEAN_PLUGIN_ICON_MONO_32);
                        break;
                    case BeanInfo.ICON_COLOR_16x16:
                        rcPath = TypeInformationProvider.getIconInformation(this.getRelatedClass(), BeanConstants.BEAN_PLUGIN_ICON_COLOR_16);
                        break;
                    case BeanInfo.ICON_COLOR_32x32:
                        rcPath = TypeInformationProvider.getIconInformation(this.getRelatedClass(), BeanConstants.BEAN_PLUGIN_ICON_COLOR_32);
                        break;
                }
            }
        } else {
            if (type != null) {
                switch(iconKind) {
                    case BeanInfo.ICON_MONO_16x16:
                        rcPath = TypeInformationProvider.getIconInformation(this.getRelatedClass(), BeanConstants.BEAN_ICON_MONO_16);
                        break;
                    case BeanInfo.ICON_MONO_32x32:
                        rcPath = TypeInformationProvider.getIconInformation(this.getRelatedClass(), BeanConstants.BEAN_ICON_MONO_32);
                        break;
                    case BeanInfo.ICON_COLOR_16x16:
                        rcPath = TypeInformationProvider.getIconInformation(this.getRelatedClass(), BeanConstants.BEAN_ICON_COLOR_16);
                        break;
                    case BeanInfo.ICON_COLOR_32x32:
                        rcPath = TypeInformationProvider.getIconInformation(this.getRelatedClass(), BeanConstants.BEAN_ICON_COLOR_32);
                        break;
                }
            }
        }
        final String rcPathFinal = rcPath;
        if (rcPath != null) {
            if (this.isPluginContextActivated()) {
                try {
                    img = ResourceLoader.getInstance().getImageNamed(rcPathFinal);
                } catch (ResourceException e) {
                }
            } else {
                try {
                    final Class c = getClass();
                    java.awt.image.ImageProducer ip = (java.awt.image.ImageProducer) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

                        public Object run() {
                            java.net.URL url;
                            if ((url = c.getResource(rcPathFinal)) == null) {
                                return null;
                            } else {
                                try {
                                    return url.getContent();
                                } catch (java.io.IOException ioe) {
                                    return null;
                                }
                            }
                        }
                    });
                    if (ip == null) return null;
                    java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
                    return tk.createImage(ip);
                } catch (Exception ex) {
                    return null;
                }
            }
        }
        return img;
    }
}
