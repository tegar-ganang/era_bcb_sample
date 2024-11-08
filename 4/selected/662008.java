package org.elf.common;

import org.w3c.dom.*;
import java.util.*;
import java.lang.reflect.*;
import java.beans.*;

/**
 * Utilidades para el uso de Beans
 * 
 * @author  <a href="mailto:logongas@users.sourceforge.net">Lorenzo Gonz�lez</a>
 */
public class BeanUtil {

    /**
   * Constructor Evitar que se instancie la clase
   */
    private BeanUtil() {
    }

    ;

    /**
   * Asigna los atributos del bean desde un XML
   * @param node Nodo XML padre de los elementos que tienen los valores de las propiedades
   * @param beanInstance Objeto al que se le asignan los datos del XML
   */
    public static void loadBeanPrimitivePropertiesFromXMLNode(Node node, Object beanInstance) {
        Map beanProperties = getBeanProperties(beanInstance.getClass());
        BeanProperty beanProperty;
        String value;
        Object typedValue;
        for (Iterator it = beanProperties.values().iterator(); it.hasNext(); ) {
            beanProperty = (BeanProperty) it.next();
            if (beanProperty.isWritable() == true) {
                if ((beanProperty.getType().isPrimitive()) || (beanProperty.getType() == String.class) || (beanProperty.getType() == java.util.Date.class)) {
                    if (XMLUtil.getFirstChild(node, beanProperty.getName()) != null) {
                        value = XMLUtil.getChildValue(node, beanProperty.getName());
                        if (beanProperty.getType() == java.util.Date.class) {
                            typedValue = Convert.getDateFromISOStringDate(value);
                        } else {
                            typedValue = Convert.getObjectFromISOString(value, beanProperty.getType());
                        }
                        beanProperty.setValue(beanInstance, typedValue);
                    }
                    ;
                }
            }
        }
    }

    /**
   * Asigna los atributos del bean desde un Map
   * @param mapProperties Map con las propiedades a asignar
   * @param beanInstance Objeto al que se le asignan los datos desde el Map
   */
    public static void loadBeanPrimitivePropertiesFromMap(Map<String, Object> mapProperties, Object beanInstance) {
        Map beanProperties = getBeanProperties(beanInstance.getClass());
        BeanProperty beanProperty;
        String value;
        Object typedValue;
        for (Iterator it = beanProperties.values().iterator(); it.hasNext(); ) {
            beanProperty = (BeanProperty) it.next();
            if (beanProperty.isWritable() == true) {
                if ((beanProperty.getType().isPrimitive()) || (beanProperty.getType() == String.class) || (beanProperty.getType() == java.util.Date.class)) {
                    if (mapProperties.keySet().contains(beanProperty.getName()) == true) {
                        value = (String) mapProperties.get(beanProperty.getName());
                        if (beanProperty.getType() == java.util.Date.class) {
                            typedValue = Convert.getDateFromISOStringDate(value);
                        } else {
                            typedValue = Convert.getObjectFromISOString(value, beanProperty.getType());
                        }
                        beanProperty.setValue(beanInstance, typedValue);
                    }
                    ;
                }
            }
        }
    }

    /**
   * Genera un Map con las propiedades del Bean
   * @param beanInstance Objeto del que se obtienen los valores del Bean
   * @return map con los valores de las propiedades
   */
    public static Map<String, Object> loadMapFromBeanProperties(Object beanInstance) {
        Map<String, BeanProperty> beanProperties = getBeanProperties(beanInstance.getClass());
        BeanProperty beanProperty;
        Map<String, Object> properties = new HashMap<String, Object>();
        for (Iterator it = beanProperties.values().iterator(); it.hasNext(); ) {
            beanProperty = (BeanProperty) it.next();
            if (beanProperty.isReadable() == true) {
                properties.put(beanProperty.getName(), beanProperty.getValue(beanInstance));
            }
        }
        return properties;
    }

    /**
   * Obtiene los metadatos de las propiedades de un bean
   * @param beanClass Clase de la que se obtienen los metadatos
   * @return Map con los metadatos de cada una de las propiedades del bean
   * El Map tiene objetos de la clase <code>BeanProperty</code>
   */
    public static Map<String, BeanProperty> getBeanProperties(Class beanClass) {
        Map<String, BeanProperty> beanProperties = new LinkedHashMap<String, BeanProperty>();
        BeanProperty beanProperty;
        PropertyDescriptor pd[];
        try {
            pd = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();
            for (int i = 0; i < pd.length; i++) {
                if (pd[i].getName().equals("class") == false) {
                    beanProperty = new BeanProperty(pd[i].getName(), pd[i].getReadMethod(), pd[i].getWriteMethod(), pd[i].getPropertyType());
                    beanProperties.put(beanProperty.getName(), beanProperty);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return beanProperties;
    }

    /**
   * Clase que contiene la descripci�n de una propieda de un Bean
   *
   * @author  <a href="mailto:logongas@users.sourceforge.net">Lorenzo Gonz�lez</a>
   */
    public static class BeanProperty {

        private String _name;

        private Method _readMethod;

        private Method _writeMethod;

        private Class _type;

        /**
     * Inicializa la clase
     * @param name Nombre de la propiedad
     * @param readMethod M�todo para leer la propiedad
     * @param writeMethod M�todo para escribir la propiedad
     * @param type Tipo de la propiedad
     */
        BeanProperty(String name, Method readMethod, Method writeMethod, Class type) {
            if ((readMethod == null) && (writeMethod == null)) {
                throw new RuntimeException("La propiedad no se puede ni leer ni escribir");
            }
            _name = name;
            _readMethod = readMethod;
            if (_readMethod != null) {
                if (_readMethod.getParameterTypes().length > 0) {
                    _readMethod = null;
                }
            }
            _readMethod = readMethod;
            _writeMethod = writeMethod;
            if (_writeMethod != null) {
                if (_writeMethod.getParameterTypes().length > 1) {
                    _writeMethod = null;
                }
            }
            _type = type;
        }

        /**
     * Obtiene el nombre de la propiedad
     * @return Nombre de la propiedad
     */
        public String getName() {
            return _name;
        }

        ;

        /**
     * Obtiene el valor de una propiedad
     * @param beanInstance Instancia sobre la que se obtiene el valor
     * @return Valor de la propiedad
     */
        public Object getValue(Object beanInstance) {
            if (_readMethod == null) {
                throw new RuntimeException("La propiedad no puede ser leida");
            }
            Object parameters[] = new Object[0];
            try {
                return _readMethod.invoke(beanInstance, parameters);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        ;

        /**
     * Establece el valor de la propiedad
     * @param beanInstance Objeto al que se le establece la propiedad
     * @param value Valor a asignar al objeto
     */
        public void setValue(Object beanInstance, Object value) {
            if (_writeMethod == null) {
                throw new RuntimeException("La propiedad no puede ser escrita");
            }
            try {
                Object parameters[] = new Object[1];
                parameters[0] = value;
                _writeMethod.invoke(beanInstance, parameters);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        ;

        /**
     * Obtiene el tipo de la propiedad
     * @return Tipo de la propiedad
     */
        public Class getType() {
            return _type;
        }

        /**
     * Obtiene si se permite escribir en la propiedad
     * @return Retorna TRUE si la propiedad permite que se escriba
     */
        public boolean isWritable() {
            return (_writeMethod != null);
        }

        /**
     * Obtiene si la propiedad permite leerse
     * @return Retorna TRUE si se puede leer la propiedad
     */
        public boolean isReadable() {
            return (_readMethod != null);
        }
    }
}
