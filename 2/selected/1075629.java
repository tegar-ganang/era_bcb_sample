package org.apache.ws.security.components.crypto;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.util.Loader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/**
 * CryptoFactory.
 * <p/>
 *
 * @author Davanum Srinivas (dims@yahoo.com).
 */
public abstract class CryptoFactory {

    private static Log log = LogFactory.getLog(CryptoFactory.class);

    private static final String defaultCryptoClassName = "org.apache.ws.security.components.crypto.Merlin";

    /**
     * getInstance
     * <p/>
     * Returns an instance of Crypto. This method uses the file
     * <code>crypto.properties</code> to determine which implementation to
     * use. Thus the property <code>org.apache.ws.security.crypto.provider</code>
     * must define the classname of the Crypto implementation. The file
     * may contain other property definitions as well. These properties are
     * handed over to the  Crypto implementation. The file
     * <code>crypto.properties</code> is loaded with the
     * <code>Loader.getResource()</code> method.
     * <p/>
     *
     * @return The cyrpto implementation was defined
     */
    public static Crypto getInstance() {
        return getInstance("crypto.properties");
    }

    /**
     * getInstance
     * <p/>
     * Returns an instance of Crypto. The properties are handed over the the crypto
     * implementation. The properties must at least contain the Crypto implementation
     * class name as the value of the property : org.apache.ws.security.crypto.provider
     * <p/>
     *
     * @param properties      The Properties that are forwarded to the crypto implementaion 
     *                        and the Crypto impl class name.
     *                        These properties are dependend on the crypto implementatin
     * @return The cyrpto implementation or null if no cryptoClassName was defined
     */
    public static Crypto getInstance(Properties properties) {
        String cryptoClassName = properties.getProperty("org.apache.ws.security.crypto.provider");
        return loadClass(cryptoClassName, properties);
    }

    /**
     * getInstance
     * <p/>
     * Returns an instance of Crypto loaded with the given classloader. 
     * The properties are handed over the the crypto implementation. 
     * The properties must at least contain the Crypto implementation
     * class name as the value of the property : org.apache.ws.security.crypto.provider
     * <p/>
     *
     * @param properties      The Properties that are forwarded to the crypto implementation 
     *                        and the Crypto impl class name.
     *                        These properties are dependent on the crypto implementation
     * @param classLoader   The class loader to use
     * @return The cyrpto implementation or null if no cryptoClassName was defined
     */
    public static Crypto getInstance(Properties properties, ClassLoader classLoader) {
        String cryptoClassName = properties.getProperty("org.apache.ws.security.crypto.provider");
        return loadClass(cryptoClassName, properties, classLoader);
    }

    /**
     * getInstance
     * <p/>
     * Returns an instance of Crypto. The properties are handed over the the crypto
     * implementation. The properties can be <code>null</code>. It is dependent on the
     * Crypto implementation how the initialization is done in this case.
     * <p/>
     *
     * @param cryptoClassName This is the crypto implementation class. No default is
     *                        provided here.
     * @param properties      The Properties that are forwarded to the crypto implementaion.
     *                        These properties are dependend on the crypto implementatin
     * @return The cyrpto implementation or null if no cryptoClassName was defined
     *
     * @deprecated            use @link{#getInstance(java.lang.String, java.util.Map)} instead.
     */
    public static Crypto getInstance(String cryptoClassName, Properties properties) {
        return loadClass(cryptoClassName, properties);
    }

    /**
     * getInstance
     * <p/>
     * Returns an instance of Crypto. The supplied map is handed over the the crypto
     * implementation. The map can be <code>null</code>. It is dependent on the
     * Crypto implementation how the initialization is done in this case.
     * <p/>
     *
     * @param cryptoClassName This is the crypto implementation class. No default is
     *                        provided here.
     * @param map             The Maps that is forwarded to the crypto implementation.
     *                        These contents of the map are dependent on the 
     *                        underlying crypto implementation specified in the 
     *                        cryptoClassName parameter.
     * @return The cyrpto implementation or null if no cryptoClassName was defined
     */
    public static Crypto getInstance(String cryptoClassName, Map map) {
        return loadClass(cryptoClassName, map);
    }

    /**
     * getInstance
     * <p/>
     * Returns an instance of Crypto. This method uses the specified filename
     * to load a property file. This file shall use the property
     * <code>org.apache.ws.security.crypto.provider</code>
     * to define the classname of the Crypto implementation. The file
     * may contain other property definitions as well. These properties are
     * handed over to the Crypto implementation. The specified file
     * is loaded with the <code>Loader.getResource()</code> method.
     * <p/>
     *
     * @param propFilename The name of the property file to load
     * @return The cyrpto implementation that was defined
     */
    public static Crypto getInstance(String propFilename) {
        Properties properties = null;
        String cryptoClassName = null;
        if ((cryptoClassName == null) || (cryptoClassName.length() == 0)) {
            properties = getProperties(propFilename);
            cryptoClassName = properties.getProperty("org.apache.ws.security.crypto.provider", defaultCryptoClassName);
        }
        return loadClass(cryptoClassName, properties);
    }

    public static Crypto getInstance(String propFilename, ClassLoader customClassLoader) {
        Properties properties = null;
        String cryptoClassName = null;
        if ((cryptoClassName == null) || (cryptoClassName.length() == 0)) {
            properties = getProperties(propFilename, customClassLoader);
            cryptoClassName = properties.getProperty("org.apache.ws.security.crypto.provider", defaultCryptoClassName);
        }
        return loadClass(cryptoClassName, properties, customClassLoader);
    }

    private static Crypto loadClass(String cryptoClassName, Map map) {
        return loadClass(cryptoClassName, map, CryptoFactory.class.getClassLoader());
    }

    /**
     * This allows loading the classes with a custom class loader  
     * @param cryptoClassName
     * @param properties
     * @param loader
     * @return
     */
    private static Crypto loadClass(String cryptoClassName, Map map, ClassLoader loader) {
        Class cryptogenClass = null;
        Crypto crypto = null;
        try {
            cryptogenClass = Loader.loadClass(loader, cryptoClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(cryptoClassName + " Not Found");
        }
        log.debug("Using Crypto Engine [" + cryptoClassName + "]");
        try {
            Class[] classes = null;
            if (map instanceof Properties) {
                classes = new Class[] { Properties.class, ClassLoader.class };
            } else {
                classes = new Class[] { Map.class, ClassLoader.class };
            }
            Constructor c = cryptogenClass.getConstructor(classes);
            crypto = (Crypto) c.newInstance(new Object[] { map, loader });
            return crypto;
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            log.error("Unable to instantiate (1): " + cryptoClassName, e);
        }
        try {
            crypto = (Crypto) cryptogenClass.newInstance();
            return crypto;
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            log.error("Unable to instantiate (2): " + cryptoClassName, e);
            throw new RuntimeException(cryptoClassName + " cannot create instance");
        }
    }

    /**
     * Gets the properties for crypto.
     * The functions loads the property file via
     * {@link Loader.getResource(String)}, thus the property file
     * should be accessible via the classpath
     *
     * @param propFilename the properties file to load
     * @return a <code>Properties</code> object loaded from the filename
     */
    private static Properties getProperties(String propFilename) {
        return getProperties(propFilename, CryptoFactory.class.getClassLoader());
    }

    /**
     * This allows loading the resources with a custom class loader
     * @param propFilename
     * @param loader
     * @return
     */
    private static Properties getProperties(String propFilename, ClassLoader loader) {
        Properties properties = new Properties();
        try {
            URL url = Loader.getResource(loader, propFilename);
            properties.load(url.openStream());
        } catch (Exception e) {
            log.debug("Cannot find crypto property file: " + propFilename);
            throw new RuntimeException("CryptoFactory: Cannot load properties: " + propFilename);
        }
        return properties;
    }
}
