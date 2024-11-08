package com.jcorporate.expresso.kernel.management;

import com.jcorporate.expresso.kernel.ComponentLifecycle;
import com.jcorporate.expresso.kernel.ExpressoComponent;
import com.jcorporate.expresso.kernel.digester.ComponentConfig;
import com.jcorporate.expresso.kernel.digester.ExpressoServicesConfig;
import com.jcorporate.expresso.kernel.exception.ConfigurationException;
import com.jcorporate.expresso.kernel.internal.DefaultConfigBean;
import com.jcorporate.expresso.kernel.metadata.ComponentMetadata;
import com.jcorporate.expresso.kernel.metadata.IndexedProperty;
import com.jcorporate.expresso.kernel.metadata.MappedProperty;
import com.jcorporate.expresso.kernel.metadata.Property;
import com.jcorporate.expresso.kernel.util.LocatorUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Provides getting and setting of configuration information for individual components
 * @author Michael Rimov
 * @version $Revision: 3 $ on  $Date: 2006-03-01 06:17:08 -0500 (Wed, 01 Mar 2006) $
 */
public class ComponentConfigBridge {

    /**
     * The logger.
     */
    private static final Logger log = Logger.getLogger(ComponentConfigBridge.class);

    /**
     * Creates a new ComponentConfigBridge object.
     */
    public ComponentConfigBridge() {
    }

    /**
     * Updates the system configuration to that of the component that has
     * been changed
     * @param changedComponent  The changed component to be updated
     * @param allServicesConfiguration The Complete Expresso Services Config
     * usually obtained from the RootContainerInterface
     * @param newConfiguration the new Configuration of the component.  This can
     * be obtained from this objects getConfiguration() method
     * @throws ConfigurationException upon error.
     */
    public void updateSystemConfiguration(ExpressoComponent changedComponent, ExpressoServicesConfig allServicesConfiguration, ComponentConfig newConfiguration) throws ConfigurationException {
        synchronized (ComponentConfigBridge.class) {
            ComponentConfig currentConfig = allServicesConfiguration.getRootConfig();
            LocatorUtils lc = new LocatorUtils(changedComponent);
            String path = lc.getPath(changedComponent);
            if (path == null || path.length() == 0) {
                currentConfig.updateConfig(newConfiguration);
            } else {
                ComponentConfig currentLevel = currentConfig;
                StringTokenizer stok = new StringTokenizer(path, ".");
                while (stok.hasMoreTokens()) {
                    String subComponent = stok.nextToken();
                    currentLevel = currentLevel.getChildComponent(subComponent);
                    if (currentLevel == null) {
                        throw new ConfigurationException("Unable to find subcomponent: " + subComponent + " for path: " + path);
                    }
                }
                currentLevel.updateConfig(newConfiguration);
            }
        }
    }

    /**
     * Resets the configuration for a component.  It is intended for this
     * function to be used at Runtime.  It locks the entire
     * ComponentConfigBridge class so that only one component configuration by
     * one administrator can take place at a time.
     *
     * @param targetComponent the Component to configure
     * @param newConfiguration the new configuration information to set for the
     *        component
     *
     * @throws ConfigurationException if there is an error configuring the
     *         component.
     */
    public void setConfiguration(ExpressoComponent targetComponent, ComponentConfig newConfiguration) throws ConfigurationException {
        synchronized (ComponentConfigBridge.class) {
            if (targetComponent instanceof ComponentLifecycle) {
                ComponentMetadata metadata = targetComponent.getMetaData();
                Map properties = metadata.getProperties();
                DefaultConfigBean targetConfig = new DefaultConfigBean();
                for (Iterator j = properties.values().iterator(); j.hasNext(); ) {
                    Property p = (Property) j.next();
                    p.createConfigBean(targetConfig, newConfiguration, metadata);
                }
                ((ComponentLifecycle) targetComponent).reconfigure(targetConfig);
            }
        }
    }

    /**
     * Given a component, retrieve the component config object for this
     * component. It is intended for this function to be used at Runtime (as
     * opposed to startup time). It locks the entire ComponentConfigBridge
     * class so that only one component configuration by one administrator can
     * take place at a time.
     *
     * <p>
     * This function only retrieves the configuration of the target
     * configuration, not its children.
     * </p>
     *
     * @param sourceComponent the Component to retrieve the configuration for.
     *
     * @return a filled out ComponentConfig object for the given target
     *         component
     *
     * @throws ConfigurationException upon error
     */
    public ComponentConfig getConfiguration(ExpressoComponent sourceComponent) throws ConfigurationException {
        synchronized (ComponentConfigBridge.class) {
            ComponentMetadata metadata = sourceComponent.getMetaData();
            ComponentConfig newConfig = new ComponentConfig();
            return null;
        }
    }

    /**
     * Put the simple properties in the expresso component into the target
     * configuration converting them to strings as appropriate.
     *
     * @param sourceComponent the component to retrieve data from
     * @param metadata the metadata for the component
     * @param targetConfig the configuration for the component
     *
     * @throws ConfigurationException
     */
    private void getSimpleProperties(ExpressoComponent sourceComponent, ComponentMetadata metadata, ComponentConfig targetConfig) throws ConfigurationException {
        Map properties = metadata.getProperties();
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            Property property = (Property) i.next();
            String access = property.getAccess();
            if ("readwrite".equalsIgnoreCase(access) || "rw".equalsIgnoreCase(access)) {
                if (property instanceof com.jcorporate.expresso.kernel.metadata.SimpleProperty) {
                    String propertyName = property.getName();
                    try {
                        Object propertyValue = PropertyUtils.getProperty(sourceComponent, propertyName);
                        String stringValue = ConvertUtils.convert(propertyValue);
                        targetConfig.setProperty(propertyName, stringValue);
                    } catch (IllegalAccessException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Property " + propertyName + " specified in metadata was not accessible.  Must be 'public'", ex);
                    } catch (InvocationTargetException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Unable to get property specified in metadata:  " + propertyName, ex);
                    } catch (NoSuchMethodException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Getter method for property  " + propertyName + " specified in metadata does not exist", ex);
                    }
                } else if (property instanceof com.jcorporate.expresso.kernel.metadata.MappedProperty) {
                    MappedProperty mappedProperty = (MappedProperty) property;
                    String propertyName = property.getName();
                    Map allProperties = mappedProperty.getValues();
                    try {
                        for (Iterator j = allProperties.keySet().iterator(); j.hasNext(); ) {
                            String oneKey = (String) j.next();
                            Object propertyValue = PropertyUtils.getMappedProperty(sourceComponent, propertyName, oneKey);
                            String stringValue = ConvertUtils.convert(propertyValue);
                            targetConfig.setMappedProperty(propertyName, oneKey, stringValue);
                        }
                    } catch (IllegalAccessException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Property " + propertyName + " specified in metadata was not accessible.  Must be 'public'", ex);
                    } catch (InvocationTargetException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Unable to get property specified in metadata:  " + propertyName, ex);
                    } catch (NoSuchMethodException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Getter method for property  " + propertyName + " specified in metadata does not exist", ex);
                    }
                } else if (property instanceof com.jcorporate.expresso.kernel.metadata.IndexedProperty) {
                    IndexedProperty indexedProperty = (IndexedProperty) property;
                    String propertyName = property.getName();
                    Map allProperties = indexedProperty.getValues();
                    try {
                        for (Iterator j = allProperties.keySet().iterator(); j.hasNext(); ) {
                            Integer oneKey = (Integer) j.next();
                            Object propertyValue = PropertyUtils.getIndexedProperty(sourceComponent, propertyName, oneKey.intValue());
                            String stringValue = ConvertUtils.convert(propertyValue);
                            targetConfig.setIndexedProperty(propertyName, oneKey.intValue(), stringValue);
                        }
                    } catch (IllegalAccessException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Property " + propertyName + " specified in metadata was not accessible.  Must be 'public'", ex);
                    } catch (InvocationTargetException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Unable to get property specified in metadata:  " + propertyName, ex);
                    } catch (NoSuchMethodException ex) {
                        log.error("Error getting simple property ", ex);
                        throw new ConfigurationException("Getter method for property  " + propertyName + " specified in metadata does not exist", ex);
                    }
                }
            }
        }
    }
}
