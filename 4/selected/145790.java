package org.sourceforge.myjavaconf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import javax.xml.parsers.FactoryConfigurationError;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * @author kirill.petrov LocalConfigurator provides services for classes that need to
 *         read their configuration outside of the application boundaries (JAR,
 *         WAR, EAR, etc). It also allows to persist configuration and reread
 *         the configuration in case if it is needed. Currently, LocalConfigurator
 *         also initializes log4j.
 */
public class LocalConfigurator implements Configurator {

    public static final String PROPERTIES_CONFIG_EXT = ".properties";

    public static final String JAXB_CONFIG_EXT = ".xml";

    public static final String FREE_FORM_CONFIG_EXT = ".config";

    public static final String LOG4J_CONFIG = "log4j.configuration";

    private static final long serialVersionUID = 2085184887753197115L;

    /**
	 * root path to configuration resources the configurator work with
	 */
    private String root = null;

    LocalConfigurator(String root) {
        if (!root.endsWith("/")) root += "/";
        this.root = root;
        File file = new File(root);
        if (!file.isDirectory()) throw new RuntimeException("Configuration folder: " + root + " does not exist.");
        initLogger();
    }

    public void initLogger() {
        try {
            String logFile = root + "log4j.xml";
            if (new File(logFile).exists()) DOMConfigurator.configure(logFile); else System.out.println("No log4j.xml found. Using default log4j.xml");
        } catch (FactoryConfigurationError e) {
            throw new ConfigurationException("Could not read log4j file:" + e);
        }
    }

    /**
	 * This method will populate PropertiesConfigurable bean submitted as an
	 * argument with the configuration Properties that Local Configurator would read
	 * from the configuration repository
	 * 
	 * @param propConfigurable
	 */
    public void configure(PropertiesConfigurable propConfigurable) {
        configure(propConfigurable, null);
    }

    /**
	 * This method will populate PropertiesConfigurable bean submitted as an
	 * argument with the configuration Properties that Local Configurator would read
	 * from the configuration repository
	 * 
	 * @param propConfigurable
	 * @param relativeFolder -
	 *            allows to specify the relative folder, in which the
	 *            configuration file needs to be located
	 */
    public void configure(PropertiesConfigurable propConfigurable, String relativePath) {
        File file = getConfigFile(propConfigurable, relativePath, PROPERTIES_CONFIG_EXT);
        Properties properties = null;
        try {
            properties = new Properties();
            properties.load(new FileInputStream(file));
        } catch (Exception e) {
            throw new ConfigurationException("Failed to read properties configuration file", e);
        }
        try {
            propConfigurable.setConfigProperties(properties);
        } catch (Exception e) {
            throw new ConfigurationException(propConfigurable.toString() + " bean failed to initialize from its configuration", e);
        }
    }

    public void configure(JAXBConfigurable jaxbConfigurable) {
        configure(jaxbConfigurable, null);
    }

    public void configure(JAXBConfigurable jaxbConfigurable, String relativePath) {
        throw new UnsupportedOperationException("XML/JAXB configuration is not implemented yet.");
    }

    public void configure(FreeFormConfigurable ffConfigurable) {
        configure(ffConfigurable, null);
    }

    public void configure(FreeFormConfigurable ffConfigurable, String relativePath) {
        File file = getConfigFile(ffConfigurable, relativePath, FREE_FORM_CONFIG_EXT);
        try {
            ffConfigurable.setInputConfigStream(new FileInputStream(file));
        } catch (Exception e) {
            throw new ConfigurationException("Failed to read free form configuration file", e);
        }
    }

    private File getConfigFile(Configurable configurable, String relativePath, String extention) {
        String fullPath = root;
        String finalExtention = "";
        String finalName = "";
        if (relativePath == null) {
            finalExtention = extention;
            finalName = configurable.getClass().getSimpleName();
            relativePath = configurable.getClass().getPackage().getName();
            if (relativePath != null) {
                char dot = '.';
                char slash = '/';
                relativePath = relativePath.replace(dot, slash);
                relativePath += "/";
            } else {
                relativePath = "";
            }
        }
        fullPath += relativePath;
        fullPath += finalName;
        fullPath += finalExtention;
        File file = new File(fullPath);
        if (!file.exists()) {
            throw new ConfigurationException("Configuration file " + fullPath + " for class " + configurable.getClass().getName() + " was not found.");
        }
        return file;
    }

    public void persist(PropertiesConfigurable propConfigurable) {
        persist(propConfigurable, null);
    }

    public void persist(PropertiesConfigurable propConfigurable, String relativePath) {
        File file = getConfigFile(propConfigurable, relativePath, PROPERTIES_CONFIG_EXT);
        Properties properties = propConfigurable.getConfigProperties();
        try {
            properties.store(new FileOutputStream(file), null);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to store properties config for class " + propConfigurable.getClass().getName() + " into file " + file.getAbsolutePath());
        }
    }

    public void persist(FreeFormConfigurable ffConfigurable) {
        persist(ffConfigurable, null);
    }

    public void persist(FreeFormConfigurable ffConfigurable, String relativePath) {
        File file = getConfigFile(ffConfigurable, relativePath, PROPERTIES_CONFIG_EXT);
        InputStream is = ffConfigurable.getInputConfigStream();
        try {
            OutputStream os = new FileOutputStream(file);
            IOUtils.copy(is, os);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to store free from config for class " + ffConfigurable.getClass().getName() + " into file " + file.getAbsolutePath());
        }
    }

    public void persist(JAXBConfigurable jaxbConfigurable) {
        persist(jaxbConfigurable, null);
    }

    public void persist(JAXBConfigurable jaxbConfigurable, String relativePath) {
        throw new UnsupportedOperationException("Configuration persistence is not implemented yet.");
    }
}
