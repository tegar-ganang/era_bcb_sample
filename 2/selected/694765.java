package org.jsecurity.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Static helper methods for loading resources.
 *
 * @since 0.2
 * @author Jeremy Haile
 */
public class ResourceUtils {

    /** Resource path prefix that specifies to load from a classpath location, value is <b><code>classpath:</code></b> */
    public static final String CLASSPATH_PREFIX = "classpath:";

    /** Resource path prefix that specifies to load from a url location, value is <b><code>url:</code></b> */
    public static final String URL_PREFIX = "url:";

    /** Resource path prefix that specifies to load from a file location, value is <b><code>file:</code></b> */
    public static final String FILE_PREFIX = "file:";

    /**
     * Commons-logging logger
     */
    private static final transient Log logger = LogFactory.getLog(ResourceUtils.class);

    /**
     * Prevent instantiation.
     */
    private ResourceUtils() {
    }

    /**
     * Returns the InputStream for the resource represented by the specified path.
     *
     * <p>The supporting prefixes are defined as as *_PREFIX constants in this class.</p>
     *
     * @param resourcePath the String path representing the resource to obtain.
     * @return the InputStraem for the specified resource.
     * @throws IOException if there is a problem acquiring the resource at the specified path.
     */
    public static InputStream getInputStreamForPath(String resourcePath) throws IOException {
        InputStream is;
        if (resourcePath.startsWith(CLASSPATH_PREFIX)) {
            is = loadFromClassPath(stripPrefix(resourcePath));
        } else if (resourcePath.startsWith(URL_PREFIX)) {
            is = loadFromUrl(stripPrefix(resourcePath));
        } else if (resourcePath.startsWith(FILE_PREFIX)) {
            is = loadFromFile(stripPrefix(resourcePath));
        } else {
            is = loadFromFile(resourcePath);
        }
        if (is == null) {
            throw new IOException("Resource [" + resourcePath + "] could not be found.");
        }
        return is;
    }

    private static InputStream loadFromFile(String path) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Opening file [" + path + "]...");
        }
        return new FileInputStream(path);
    }

    private static InputStream loadFromUrl(String urlPath) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Opening url [" + urlPath + "]...");
        }
        URL url = new URL(urlPath);
        return url.openStream();
    }

    private static InputStream loadFromClassPath(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ResourceUtils.class.getClassLoader();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Opening resource from class path [" + path + "]...");
        }
        return classLoader.getResourceAsStream(path);
    }

    private static String stripPrefix(String resourcePath) {
        return resourcePath.substring(resourcePath.indexOf(":") + 1);
    }

    public static void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                logger.warn("Error closing input stream.", e);
            }
        }
    }
}
