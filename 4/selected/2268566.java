package v4view.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.io.IOUtils;

/**
 * A static content loader. It useful for those situtations where you need to have
 * large blocks of static text, but don't want embed it into an element. For example,
 * you might what to have in page javascripts. Under a JSP/HTML paradigm this is easy,
 * just include the text in the page. The Java component model doesn't support this
 * in an as friendly way. However, text files or .js files do. This class supports 
 * loading static content from such a file for use in inlined Content elements.
 * <br />
 * <strong>Note:</strong> It is not necessiarily best to inline scripts, etc. 
 * <a href="http://developer.yahoo.com/performance/rules.html">Yahoo! on performance</a>
 * 
 * @author J Patrick Davenport
 *
 */
public class ResourceManager {

    private static final ResourceManager singleton = new ResourceManager();

    private final Map<String, String> cachedResources;

    private final Map<String, Map<Locale, ResourceBundle>> resourceBundles;

    private final ReadWriteLock fileLock;

    private final ReadWriteLock resourceBundleLock;

    private ResourceManager() {
        this.cachedResources = new HashMap<String, String>();
        this.resourceBundles = new HashMap<String, Map<Locale, ResourceBundle>>();
        this.fileLock = new ReentrantReadWriteLock();
        this.resourceBundleLock = new ReentrantReadWriteLock();
    }

    public static final ResourceManager getManager() {
        return singleton;
    }

    /**
	 * 
	 * @param _bundleURI
	 * @param _bundleKey
	 * @param _locale
	 * @return the text associated with the _bundleKey.
	 */
    public String getValue(final String _bundleURI, final String _bundleKey, final Locale _locale) {
        final Lock readLock = this.resourceBundleLock.readLock();
        final Lock writeLock = this.resourceBundleLock.writeLock();
        try {
            readLock.lock();
            if (!this.resourceBundles.containsKey(_bundleURI)) {
                readLock.unlock();
                writeLock.lock();
                try {
                    if (!this.resourceBundles.containsKey(_bundleURI)) {
                        final HashMap<Locale, ResourceBundle> localeBundles = new HashMap<Locale, ResourceBundle>();
                        localeBundles.put(_locale, ResourceBundle.getBundle(_bundleURI, _locale));
                        this.resourceBundles.put(_bundleURI, localeBundles);
                    }
                } finally {
                    writeLock.unlock();
                    readLock.lock();
                }
            }
            final Map<Locale, ResourceBundle> localeBundles = this.resourceBundles.get(_bundleURI);
            if (!localeBundles.containsKey(_locale)) {
                readLock.unlock();
                writeLock.lock();
                try {
                    if (!localeBundles.containsKey(_locale)) {
                        localeBundles.put(_locale, ResourceBundle.getBundle(_bundleURI, _locale));
                    }
                } finally {
                    writeLock.unlock();
                    readLock.lock();
                }
            }
            return localeBundles.get(_locale).getString(_bundleKey);
        } finally {
            readLock.unlock();
        }
    }

    /**
	 * Supports loading files as strings. This is for occasions where you want to have a lot of static content in your
	 * page but don't want to fuss with StringBuilders or concatenation.
	 * @param _resourceURI location on the classpath for the file.
	 * @return the contents of the file or null if the file can't be found.
	 */
    public String loadFileContent(final String _resourceURI) {
        final Lock readLock = this.fileLock.readLock();
        final Lock writeLock = this.fileLock.writeLock();
        boolean hasReadLock = false;
        boolean hasWriteLock = false;
        try {
            readLock.lock();
            hasReadLock = true;
            if (!this.cachedResources.containsKey(_resourceURI)) {
                readLock.unlock();
                hasReadLock = false;
                writeLock.lock();
                hasWriteLock = true;
                if (!this.cachedResources.containsKey(_resourceURI)) {
                    final InputStream resourceAsStream = this.getClass().getResourceAsStream(_resourceURI);
                    final StringWriter writer = new StringWriter();
                    try {
                        IOUtils.copy(resourceAsStream, writer);
                    } catch (final IOException ex) {
                        throw new IllegalStateException("Resource not read-able", ex);
                    }
                    final String loadedResource = writer.toString();
                    this.cachedResources.put(_resourceURI, loadedResource);
                }
                writeLock.unlock();
                hasWriteLock = false;
                readLock.lock();
                hasReadLock = true;
            }
            return this.cachedResources.get(_resourceURI);
        } finally {
            if (hasReadLock) {
                readLock.unlock();
            }
            if (hasWriteLock) {
                writeLock.unlock();
            }
        }
    }

    public boolean containsFile(final String _resourceURI) {
        this.fileLock.readLock().lock();
        try {
            return this.cachedResources.containsKey(_resourceURI);
        } finally {
            this.fileLock.readLock().unlock();
        }
    }
}
