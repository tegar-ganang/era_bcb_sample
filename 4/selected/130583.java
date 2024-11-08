package org.darkimport.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * For now, use file based configuration. Later updates will allow us more
 * configuration options.
 * 
 * TODO Use the Spring Resource class so that we know how to access (in/out)
 * more resource types.
 * 
 * TODO Use this class for feeding config values only. Another class needs to
 * load and save the config.
 * 
 * @author user
 * 
 */
public class ConfigHelper {

    private static final Log log = LogFactory.getLog(ConfigHelper.class);

    private static final String META_INF_DEFAULT_CONFIG_PROPERTIES = "META-INF/DEFAULTSETTINGS.properties";

    private static boolean initialized = false;

    private static String configFileName;

    private static Map<String, Properties> propertiesByGroup;

    public static void initialize(final String configFileName) {
        URLClassLoader classLoader;
        try {
            final File configFile = new File(configFileName);
            final File configFileDir = configFile.getAbsoluteFile().getParentFile();
            final URI configFileDirURI = configFileDir.toURI();
            final URL configFileDirURL = configFileDirURI.toURL();
            classLoader = new URLClassLoader(new URL[] { configFileDirURL });
        } catch (final MalformedURLException e) {
            log.fatal("The URL of the settings file " + configFileName + " is malformed.", e);
            throw new RuntimeException(e);
        }
        InputStream userSettingsIn = classLoader.getResourceAsStream(FilenameUtils.getName(configFileName));
        if (userSettingsIn == null) {
            log.warn("Settings file not found at " + configFileName + ". Loading default configuration.");
            loadDefaultSettings(configFileName);
            userSettingsIn = classLoader.getResourceAsStream(FilenameUtils.getName(configFileName));
        }
        ConfigHelper.configFileName = configFileName;
        final Properties amalgamatedProperties = new Properties();
        try {
            amalgamatedProperties.load(userSettingsIn);
        } catch (final Exception e) {
            log.warn("Unable to load the config file: " + configFileName + ".", e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(userSettingsIn);
        }
        propertiesByGroup = new Hashtable<String, Properties>();
        final Set<Object> keys = amalgamatedProperties.keySet();
        for (final Object key : keys) {
            final String groupName = key.toString().split("\\.")[0];
            Properties properties = propertiesByGroup.get(groupName);
            if (properties == null) {
                properties = new Properties();
                propertiesByGroup.put(groupName, properties);
            }
            properties.setProperty(key.toString().substring(groupName.length() + 1), amalgamatedProperties.getProperty(key.toString()));
        }
        initialized = true;
    }

    private static void loadDefaultSettings(final String configFileName) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(META_INF_DEFAULT_CONFIG_PROPERTIES);
            out = new FileOutputStream(configFileName);
            IOUtils.copy(in, out);
        } catch (final Exception e) {
            log.warn("Unable to pull out the default.", e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public static List<ConfigState> validateConfiguration() {
        return null;
    }

    public static void removeInvalidSettings() {
    }

    public static void removeDeprecatedSettings() {
    }

    public static void updateSettingsToCurrentVersion() {
    }

    public static Properties getGroup(final String groupName) {
        if (!initialized) {
            throw new IllegalStateException("The config helper is not initialized.");
        }
        Properties p = propertiesByGroup.get(groupName);
        if (p == null) {
            p = new Properties();
            propertiesByGroup.put(groupName, p);
        }
        return p;
    }

    public static Map<String, Properties> getPropertiesByGroup() {
        if (!initialized) {
            throw new IllegalStateException("The config helper is not initialized.");
        }
        return propertiesByGroup;
    }

    /**
	 * No-op if the config helper is initialized as read only.
	 * 
	 * @param groupName
	 * @param properties
	 */
    public static synchronized void updateGroup(final String groupName, final Properties properties) {
        if (!initialized) {
            throw new IllegalStateException("The config helper is not initialized.");
        }
        propertiesByGroup.put(groupName, properties);
    }

    public static synchronized void saveConfiguration() {
        final Set<String> groups = propertiesByGroup.keySet();
        final Properties amalgamatedProperties = new Properties();
        for (final String groupName : groups) {
            final Properties groupProperties = propertiesByGroup.get(groupName);
            final Set<Object> keys = groupProperties.keySet();
            for (final Object key : keys) {
                final StringBuffer keyBuffer = new StringBuffer(groupName).append('.');
                keyBuffer.append(key.toString());
                amalgamatedProperties.setProperty(keyBuffer.toString(), groupProperties.getProperty(key.toString()));
            }
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(configFileName);
            amalgamatedProperties.store(out, null);
        } catch (final Exception e) {
            log.warn("An error occurred while saving the configuration.", e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
