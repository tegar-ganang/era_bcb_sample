package org.argouml.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * This class provides a user configuration based upon 
 * the properties file "argo.user.properties" in the user's home directory.
 * 
 * @author Thierry Lach
 */
class ConfigurationProperties extends ConfigurationHandler {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(ConfigurationProperties.class);

    /**
     * The location of Argo's default properties resource.
     */
    private static String propertyLocation = "/org/argouml/resource/default.properties";

    /**
     * The primary property bundle.
     */
    private Properties propertyBundle;

    /**
     * Flag to ensure that only the first load failure is reported
     * even though we keep trying because the file or URL may only
     * be temporarily unavailable.
     */
    private boolean canComplain = true;

    /**
     * Anonymous constructor.
     */
    ConfigurationProperties() {
        super(true);
        Properties defaults = new Properties();
        try {
            defaults.load(getClass().getResourceAsStream(propertyLocation));
            LOG.debug("Configuration loaded from " + propertyLocation);
        } catch (Exception ioe) {
            LOG.warn("Configuration not loaded from " + propertyLocation, ioe);
        }
        propertyBundle = new Properties(defaults);
    }

    /**
     * Returns the default path for user properties.
     *
     * @return a generic path string.
     */
    public String getDefaultPath() {
        return System.getProperty("user.home") + "/.argouml/argo.user.properties";
    }

    /**
     * Returns the default path for user properties (before 0.25.4)
     * @return a generic path string
     * @deprecated This should be removed on the next version after 0.26
     */
    @Deprecated
    public String getOldDefaultPath() {
        return System.getProperty("user.home") + "/argo.user.properties";
    }

    /**
     * Copy a file from source to destination.
     * 
     * TODO: Perhaps belongs in a utilities class of some sort.
     * 
     * @param source the source file to be copied
     * @param dest the destination file
     * @return success status flag
     */
    private static boolean copyFile(final File source, final File dest) {
        try {
            final FileInputStream fis = new FileInputStream(source);
            final FileOutputStream fos = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
            fis.close();
            fos.close();
            return true;
        } catch (final FileNotFoundException e) {
            LOG.error("File not found while copying", e);
            return false;
        } catch (final IOException e) {
            LOG.error("IO error copying file", e);
            return false;
        } catch (final SecurityException e) {
            LOG.error("You are not allowed to copy these files", e);
            return false;
        }
    }

    /**
     * Load the configuration from a specified location. <p>
     * 
     * Before version 0.25.4, ArgoUML used to store the 
     * properties file in a different location. A user who
     * upgrades his ArgoUML to a newer version, 
     * would not like to loose his settings.
     * Hence, in case a properties file does not exist 
     * (in the new location),
     * this code attempts to copy the file
     * from the old location to the new location. <p>
     * 
     * In this upgrade case, the properties file 
     * is copied, not moved.
     * Rationale: see issue 5364. <p>
     * 
     * The meaning of the return value is not simply success
     * in loading the properties file, 
     * but it indicates if we may save the properties 
     * on top of this file later.
     * Hence, in case a properties file did not exist 
     * (not in the new location, nor in the old location), 
     * then a new empty file is created, 
     * and in this case the return value is true. <p>
     * 
     * Returning false here would mean that no properties
     * will be saved at all. 
     *
     * @param file  the path to load the configuration from.
     *
     * @return true if the given file-location may be used 
     * for writing the properties later.
     */
    public boolean loadFile(File file) {
        try {
            if (!file.exists()) {
                final File oldFile = new File(getOldDefaultPath());
                if (oldFile.exists() && oldFile.isFile() && oldFile.canRead() && file.getParentFile().canWrite()) {
                    final boolean result = copyFile(oldFile, file);
                    if (result) {
                        LOG.info("Configuration copied from " + oldFile + " to " + file);
                    } else {
                        LOG.error("Error copying old configuration to new, " + "see previous log messages");
                    }
                } else {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        LOG.error("Could not create the properties file at: " + file.getAbsolutePath(), e);
                    }
                }
            }
            if (file.exists() && file.isFile() && file.canRead()) {
                try {
                    propertyBundle.load(new FileInputStream(file));
                    LOG.info("Configuration loaded from " + file);
                    return true;
                } catch (final IOException e) {
                    if (canComplain) {
                        LOG.warn("Unable to load configuration " + file);
                    }
                    canComplain = false;
                }
            }
        } catch (final SecurityException e) {
            LOG.error("A security exception occurred trying to load" + " the configuration, check your security settings", e);
        }
        return false;
    }

    /**
     * Save the configuration to a specified location.
     *
     * @param file  the path to save the configuration at.
     *
     * @return true if the save was successful, false if not.
     */
    public boolean saveFile(File file) {
        try {
            propertyBundle.store(new FileOutputStream(file), "ArgoUML properties");
            LOG.info("Configuration saved to " + file);
            return true;
        } catch (Exception e) {
            if (canComplain) {
                LOG.warn("Unable to save configuration " + file + "\n");
            }
            canComplain = false;
        }
        return false;
    }

    /**
     * Load the configuration from a specified location.
     *
     * @param url  the path to load the configuration from.
     *
     * @return true if the load was successful, false if not.
     */
    public boolean loadURL(URL url) {
        try {
            propertyBundle.load(url.openStream());
            LOG.info("Configuration loaded from " + url + "\n");
            return true;
        } catch (Exception e) {
            if (canComplain) {
                LOG.warn("Unable to load configuration " + url + "\n");
            }
            canComplain = false;
            return false;
        }
    }

    /**
     * Save the configuration to a specified location.
     *
     * @param url  the path to save the configuration at.
     *
     * @return true if the save was successful, false if not.
     */
    public boolean saveURL(URL url) {
        return false;
    }

    /**
     * Returns the string value of a configuration property.
     *
     * @param key the key to return the value of.
     * @param defaultValue the value to return if the key was not found.
     *
     * @return the string value of the key if found, otherwise null;
     */
    public String getValue(String key, String defaultValue) {
        String result = "";
        try {
            result = propertyBundle.getProperty(key, defaultValue);
        } catch (Exception e) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Sets the string value of a configuration property.
     *
     * @param key the key to set.
     * @param value the value to set the key to.
     */
    public void setValue(String key, String value) {
        LOG.debug("key '" + key + "' set to '" + value + "'");
        propertyBundle.setProperty(key, value);
    }

    /**
     * Remove a property.
     *
     * @param key The property to remove.
     */
    public void remove(String key) {
        propertyBundle.remove(key);
    }
}
