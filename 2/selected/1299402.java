package com.poltman.zk.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Class for reading the DSpace system configuration. The main configuration is
 * read in as properties from a standard properties file. Email templates and
 * configuration files for other tools are also be accessed via this class.
 * <P>
 * The main configuration is by default read from the <em>resource</em>
 * <code>/dspace.cfg</code>.
 * To specify a different configuration, the system property
 * <code>dspace.configuration</code> should be set to the <em>filename</em>
 * of the configuration file.
 * <P>
 * Other configuration files are read from the <code>config</code> directory
 * of the DSpace installation directory (specified as the property
 * <code>dspace.dir</code> in the main configuration file.)
 *
 * 
 * @author Robert Tansley
 * @author Larry Stone - Interpolated values.
 * @author Mark Diggory - General Improvements to detection, logging and loading.
 * @version $Revision: 1219 $
 */
public class ConfigurationManager {

    /** log4j category */
    private static Logger log = Logger.getLogger(ConfigurationManager.class);

    /** The configuration properties */
    private static Properties properties = null;

    /** module configuration properties */
    private static Map<String, Properties> moduleProps = null;

    /** The default license */
    private static String license;

    private static final int RECURSION_LIMIT = 9;

    protected ConfigurationManager() {
    }

    /**
     * Identify if DSpace is properly configured
     * @return boolean true if configured, false otherwise
     */
    public static boolean isConfigured() {
        return properties != null;
    }

    public static boolean isConfigured(String module) {
        return moduleProps.get(module) != null;
    }

    /**
     * Returns all properties in main configuration
     * 
     * @return properties - all non-modular properties
     */
    public static Properties getProperties() {
        Properties props = getMutableProperties();
        return props == null ? null : (Properties) props.clone();
    }

    private static Properties getMutableProperties() {
        if (properties == null) {
            loadConfig(null);
        }
        return properties;
    }

    /**
     * Returns all properties for a given module
     * 
     * @param module
     *        the name of the module
     * @return properties - all module's properties
     */
    public static Properties getProperties(String module) {
        Properties props = getMutableProperties(module);
        return props == null ? null : (Properties) props.clone();
    }

    private static Properties getMutableProperties(String module) {
        Properties retProps = (module != null) ? moduleProps.get(module) : properties;
        if (retProps == null) {
            loadModuleConfig(module);
            retProps = moduleProps.get(module);
        }
        return retProps;
    }

    /**
     * Get a configuration property
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property, or <code>null</code> if the property
     *         does not exist.
     */
    public static String getProperty(String property) {
        Properties props = getMutableProperties();
        String value = props == null ? null : props.getProperty(property);
        return (value != null) ? value.trim() : null;
    }

    /**
     * Get a module configuration property value.
     * 
     * @param module 
     *      the name of the module, or <code>null</code> for regular configuration
     *      property
     * @param property
     *      the name (key) of the property
     * @return
     *      the value of the property, or <code>null</code> if the
     *      property does not exist
     */
    public static String getProperty(String module, String property) {
        if (module == null) {
            return getProperty(property);
        }
        String value = null;
        Properties modProps = getMutableProperties(module);
        if (modProps != null) {
            value = modProps.getProperty(property);
        }
        if (value == null) {
            value = getProperty(module + "." + property);
        }
        return (value != null) ? value.trim() : null;
    }

    /**
     * Get a configuration property as an integer
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static int getIntProperty(String property) {
        return getIntProperty(property, 0);
    }

    /**
     * Get a module configuration property as an integer
     *
     * @param module
     *         the name of the module
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static int getIntProperty(String module, String property) {
        return getIntProperty(module, property, 0);
    }

    /**
     * Get a configuration property as an integer, with default
     * 
     * @param property
     *            the name of the property
     *            
     * @param defaultValue
     *            value to return if property is not found or is not an Integer.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static int getIntProperty(String property, int defaultValue) {
        return getIntProperty(null, property, defaultValue);
    }

    /**
     * Get a module configuration property as an integer, with default
     * 
     * @param module
     *         the name of the module
     * 
     * @param property
     *            the name of the property
     *            
     * @param defaultValue
     *            value to return if property is not found or is not an Integer.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static int getIntProperty(String module, String property, int defaultValue) {
        String stringValue = getProperty(module, property);
        int intValue = defaultValue;
        if (stringValue != null) {
            try {
                intValue = Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException e) {
                log.warn("Warning: Number format error in property: " + property);
            }
        }
        return intValue;
    }

    /**
     * Get a configuration property as a long
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static long getLongProperty(String property) {
        return getLongProperty(property, 0);
    }

    /**
     * Get a module configuration property as a long
     *
     * @param module
     *         the name of the module    
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static long getLongProperty(String module, String property) {
        return getLongProperty(module, property, 0);
    }

    /**
     * Get a configuration property as an long, with default
     * 
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not a Long.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static long getLongProperty(String property, int defaultValue) {
        return getLongProperty(null, property, defaultValue);
    }

    /**
     * Get a configuration property as an long, with default
     * 
     * @param module  the module, or <code>null</code> for regular property
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not a Long.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static long getLongProperty(String module, String property, int defaultValue) {
        String stringValue = getProperty(module, property);
        long longValue = defaultValue;
        if (stringValue != null) {
            try {
                longValue = Long.parseLong(stringValue.trim());
            } catch (NumberFormatException e) {
                log.warn("Warning: Number format error in property: " + property);
            }
        }
        return longValue;
    }

    /**
     * Get the License
     * 
     * @param
     *         licenseFile   file name
     *  
     *  @return
     *         license text
     * 
     */
    public static String getLicenseText(String licenseFile) {
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(licenseFile);
            br = new BufferedReader(fr);
            String lineIn;
            license = "";
            while ((lineIn = br.readLine()) != null) {
                license = license + lineIn + '\n';
            }
        } catch (IOException e) {
            log.fatal("Can't load configuration", e);
            throw new IllegalStateException("Failed to read default license.", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ioe) {
                }
            }
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ioe) {
                }
            }
        }
        return license;
    }

    /**
     * Get a configuration property as a boolean. True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property. <code>false</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property) {
        return getBooleanProperty(property, false);
    }

    /**
     * Get a module configuration property as a boolean. True is indicated if 
     * the value of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     * 
     * @param module the module, or <code>null</code> for regular property   
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property. <code>false</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String module, String property) {
        return getBooleanProperty(module, property, false);
    }

    /**
     * Get a configuration property as a boolean, with default.
     * True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property, boolean defaultValue) {
        return getBooleanProperty(null, property, defaultValue);
    }

    /**
     * Get a module configuration property as a boolean, with default.
     * True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     * 
     * @param module     module, or <code>null</code> for regular property   
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String module, String property, boolean defaultValue) {
        String stringValue = getProperty(module, property);
        if (stringValue != null) {
            stringValue = stringValue.trim();
            return stringValue.equalsIgnoreCase("true") || stringValue.equalsIgnoreCase("yes");
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns an enumeration of all the keys in the DSpace configuration
     * 
     * @return an enumeration of all the keys in the DSpace configuration
     */
    public static Enumeration<?> propertyNames() {
        return propertyNames(null);
    }

    /**
     * Returns an enumeration of all the keys in a module configuration
     * 
     * @param  module    module, or <code>null</code> for regular property  
     * 
     * @return an enumeration of all the keys in the module configuration,
     *         or <code>null</code> if the module does not exist.
     */
    public static Enumeration<?> propertyNames(String module) {
        Properties props = getProperties(module);
        return props == null ? null : props.propertyNames();
    }

    /**
     * Get the template for an email message. The message is suitable for
     * inserting values using <code>java.text.MessageFormat</code>.
     * 
     * @param emailFile
     *            full name for the email template, for example "/dspace/config/emails/register".
     * 
     * @return the email object, with the content and subject filled out from
     *         the template
     * 
     * @throws IOException
     *             if the template couldn't be found, or there was some other
     *             error reading the template
     */
    public static Email getEmail(String emailFile) throws IOException {
        String charset = null;
        String subject = "";
        StringBuffer contentBuffer = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(emailFile));
            boolean more = true;
            while (more) {
                String line = reader.readLine();
                if (line == null) {
                    more = false;
                } else if (line.toLowerCase().startsWith("subject:")) {
                    subject = line.substring(8).trim();
                } else if (line.toLowerCase().startsWith("charset:")) {
                    charset = line.substring(8).trim();
                } else if (!line.startsWith("#")) {
                    contentBuffer.append(line);
                    contentBuffer.append("\n");
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        Email email = new Email();
        email.setSubject(subject);
        email.setContent(contentBuffer.toString());
        if (charset != null) {
            email.setCharset(charset);
        }
        return email;
    }

    /**
     * Get the site-wide default license that submitters need to grant
     * 
     * @return the default license
     */
    public static String getDefaultSubmissionLicense() {
        if (properties == null) {
            loadConfig(null);
        }
        return license;
    }

    /**
     * Get the path for the news files.
     * 
     */
    public static String getNewsFilePath() {
        String filePath = ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        return filePath;
    }

    /**
     * Reads news from a text file.
     * 
     * @param newsFile
     *        name of the news file to read in, relative to the news file path.
     */
    public static String readNewsFile(String newsFile) {
        String fileName = getNewsFilePath();
        fileName += newsFile;
        StringBuilder text = new StringBuilder();
        try {
            FileInputStream fir = new FileInputStream(fileName);
            InputStreamReader ir = new InputStreamReader(fir, "UTF-8");
            BufferedReader br = new BufferedReader(ir);
            String lineIn;
            while ((lineIn = br.readLine()) != null) {
                text.append(lineIn);
            }
            br.close();
        } catch (IOException e) {
            log.warn("news_read: " + e.getLocalizedMessage());
        }
        return text.toString();
    }

    /**
     * Writes news to a text file.
     * 
     * @param newsFile
     *        name of the news file to read in, relative to the news file path.
     * @param news
     *            the text to be written to the file.
     */
    public static String writeNewsFile(String newsFile, String news) {
        String fileName = getNewsFilePath();
        fileName += newsFile;
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.print(news);
            out.close();
        } catch (IOException e) {
            log.warn("news_write: " + e.getLocalizedMessage());
        }
        return news;
    }

    /**
     * Writes license to a text file.
     * 
     * @param licenseFile
     *            name for the file int which license will be written, 
     *            relative to the current directory.
     */
    public static void writeLicenseFile(String licenseFile, String newLicense) {
        try {
            FileOutputStream fos = new FileOutputStream(licenseFile);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.print(newLicense);
            out.close();
        } catch (IOException e) {
            log.warn("license_write: " + e.getLocalizedMessage());
        }
        license = newLicense;
    }

    private static File loadedFile = null;

    /**
     * Return the file that configuration was actually loaded from. Only returns
     * a valid File after configuration has been loaded.
     * 
     * @deprecated Please remove all direct usage of the configuration file.
     * @return File naming configuration data file, or null if not loaded yet.
     */
    protected static File getConfigurationFile() {
        if (loadedFile == null) {
            loadConfig(null);
        }
        return loadedFile;
    }

    private static synchronized void loadModuleConfig(String module) {
        File modFile = null;
        try {
            modFile = new File(getProperty("dspace.dir") + File.separator + "config" + File.separator + "modules" + File.separator + module + ".cfg");
            if (modFile.exists()) {
                Properties modProps = new Properties();
                InputStream modIS = null;
                try {
                    modIS = new FileInputStream(modFile);
                    modProps.load(modIS);
                } finally {
                    if (modIS != null) {
                        modIS.close();
                    }
                }
                for (Enumeration pe = modProps.propertyNames(); pe.hasMoreElements(); ) {
                    String key = (String) pe.nextElement();
                    String ival = interpolate(key, modProps.getProperty(key), 1);
                    if (ival != null) {
                        modProps.setProperty(key, ival);
                    }
                }
                moduleProps.put(module, modProps);
            } else {
                log.warn("Requested configuration module: " + module + " not found");
            }
        } catch (IOException ioE) {
            log.fatal("Can't load configuration: " + (modFile == null ? "<unknown>" : modFile.getAbsolutePath()), ioE);
        }
        return;
    }

    /**
     * Load the DSpace configuration properties. Only does anything if
     * properties are not already loaded. Properties are loaded in from the
     * specified file, or default locations.
     * 
     * @param configFile
     *            The <code>dspace.cfg</code> configuration file to use, or
     *            <code>null</code> to try default locations
     */
    public static synchronized void loadConfig(String configFile) {
        if (properties != null) {
            return;
        }
        URL url = null;
        InputStream is = null;
        try {
            String configProperty = null;
            try {
                configProperty = System.getProperty("dspace.configuration");
            } catch (SecurityException se) {
                log.warn("Unable to access system properties, ignoring.", se);
            }
            if (loadedFile != null) {
                log.info("Reloading current config file: " + loadedFile.getAbsolutePath());
                url = loadedFile.toURI().toURL();
            } else if (configFile != null) {
                log.info("Loading provided config file: " + configFile);
                loadedFile = new File(configFile);
                url = loadedFile.toURI().toURL();
            } else if (configProperty != null) {
                log.info("Loading system provided config property (-Ddspace.configuration): " + configProperty);
                loadedFile = new File(configProperty);
                url = loadedFile.toURI().toURL();
            } else {
                url = ConfigurationManager.class.getResource("/dspace.cfg");
                if (url != null) {
                    log.info("Loading from classloader: " + url);
                    loadedFile = new File(url.getPath());
                }
            }
            if (url == null) {
                log.fatal("Cannot find dspace.cfg");
                throw new IllegalStateException("Cannot find dspace.cfg");
            } else {
                properties = new Properties();
                moduleProps = new HashMap<String, Properties>();
                is = url.openStream();
                properties.load(is);
                for (Enumeration<?> pe = properties.propertyNames(); pe.hasMoreElements(); ) {
                    String key = (String) pe.nextElement();
                    String value = interpolate(key, properties.getProperty(key), 1);
                    if (value != null) {
                        properties.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            log.fatal("Can't load configuration: " + url, e);
            throw new IllegalStateException("Cannot load configuration: " + url, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                }
            }
        }
        File licenseFile = new File(getProperty("dspace.dir") + File.separator + "config" + File.separator + "default.license");
        FileInputStream fir = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        try {
            fir = new FileInputStream(licenseFile);
            ir = new InputStreamReader(fir, "UTF-8");
            br = new BufferedReader(ir);
            String lineIn;
            license = "";
            while ((lineIn = br.readLine()) != null) {
                license = license + lineIn + '\n';
            }
            br.close();
        } catch (IOException e) {
            log.fatal("Can't load license: " + licenseFile.toString(), e);
            throw new IllegalStateException("Cannot load license: " + licenseFile.toString(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ioe) {
                }
            }
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException ioe) {
                }
            }
            if (fir != null) {
                try {
                    fir.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
     * Recursively interpolate variable references in value of
     * property named "key".
     * @return new value if it contains interpolations, or null
     *   if it had no variable references.
     */
    private static String interpolate(String key, String value, int level) {
        if (level > RECURSION_LIMIT) {
            throw new IllegalArgumentException("ConfigurationManager: Too many levels of recursion in configuration property variable interpolation, property=" + key);
        }
        int from = 0;
        StringBuffer result = null;
        while (from < value.length()) {
            int start = value.indexOf("${", from);
            if (start >= 0) {
                int end = value.indexOf('}', start);
                if (end < 0) {
                    break;
                }
                String var = value.substring(start + 2, end);
                if (result == null) {
                    result = new StringBuffer(value.substring(from, start));
                } else {
                    result.append(value.substring(from, start));
                }
                if (properties.containsKey(var)) {
                    String ivalue = interpolate(var, properties.getProperty(var), level + 1);
                    if (ivalue != null) {
                        result.append(ivalue);
                        properties.setProperty(var, ivalue);
                    } else {
                        result.append(((String) properties.getProperty(var)).trim());
                    }
                } else {
                    log.warn("Interpolation failed in value of property \"" + key + "\", there is no property named \"" + var + "\"");
                }
                from = end + 1;
            } else {
                break;
            }
        }
        if (result != null && from < value.length()) {
            result.append(value.substring(from));
        }
        return (result == null) ? null : result.toString();
    }
}
