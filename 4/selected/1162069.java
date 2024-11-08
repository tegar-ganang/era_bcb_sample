package org.colombbus.tangara.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.IOUtils;

/**
 * A class loader able to load external JAR files
 *
 * <pre>
 * Use this class in this way:
 * {@link ExternalJarClassLoader} cl = new {@link ExternalJarClassLoader}();
 * cl.registerJarFile( new File("/a/path/to/alib.jar") );
 * </pre>
 */
public class ExternalJarClassLoader extends ClassLoader {

    private final Collection<File> jarPathList = new HashSet<File>();

    private final Map<File, JarFile> openedJarFiles = new HashMap<File, JarFile>();

    private final Lock jarLock = new ReentrantLock();

    /**
     * Create a new class loader based on the system class loader
     */
    public ExternalJarClassLoader() {
        super(ClassLoader.getSystemClassLoader());
    }

    /**
     * Create a new class loader based on the system class loader
     *
     * @param parentClassloader
     *            the parent class loader
     */
    public ExternalJarClassLoader(ClassLoader parentClassloader) {
        super(parentClassloader);
    }

    /**
     * Add the content of a JAR file to the class loader
     *
     * @param jarPath
     *            the path to an existing JAR file
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public void registerJar(File jarPath) throws IllegalArgumentException, IOException {
        checkJarFileExists(jarPath);
        try {
            jarLock.lock();
            jarPathList.add(jarPath);
        } finally {
            jarLock.unlock();
        }
    }

    private void checkJarFileExists(File jarPath) throws IllegalArgumentException {
        if (jarPath == null) throw new IllegalArgumentException("jarPath argument is null");
        if (jarPath.exists() == false) throw new IllegalArgumentException(jarPath.getAbsolutePath() + " does not exist");
        if (jarPath.isFile() == false) throw new IllegalArgumentException(jarPath.getAbsolutePath() + " is not a file");
        if (jarPath.canRead() == false) throw new IllegalArgumentException(jarPath.getAbsolutePath() + " is not readable");
    }

    /**
     * Unregister a previously registered JAR file
     * <p>
     * Nothing is done if the JAR file is not registered to the classloader.
     * </p>
     *
     * @param jarPath
     *            a non null path to a JAR file
     */
    public void unregisterJar(File jarPath) {
        if (jarPath == null) throw new IllegalArgumentException("jarPath argument is null");
        try {
            jarLock.lock();
            if (jarPathList.contains(jarPath)) {
                jarPathList.remove(jarPath);
                closeJarIfNecessary(jarPath);
            } else {
                throw new IllegalArgumentException(jarPath.getAbsolutePath() + " is not registered");
            }
        } finally {
            jarLock.unlock();
        }
    }

    private void closeJarIfNecessary(File jarPath) {
        JarFile jarFile = openedJarFiles.remove(jarPath);
        closeIfNecessary(jarFile);
    }

    private static void closeIfNecessary(JarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (Throwable th) {
            }
        }
    }

    public boolean isJarRegistered(File jarPath) {
        if (jarPath == null) throw new IllegalArgumentException("jarPath argument is null");
        try {
            jarLock.lock();
            return jarPathList.contains(jarPath);
        } finally {
            jarLock.unlock();
        }
    }

    /**
     * Close all opened JAR files
     * <p>
     * Useful to free unused resources
     * </p>
     */
    public void closeJars() {
        try {
            jarLock.lock();
            for (JarFile jarFile : openedJarFiles.values()) {
                closeIfNecessary(jarFile);
            }
            openedJarFiles.clear();
        } finally {
            jarLock.unlock();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name == null) throw new IllegalArgumentException("name argument is null");
        try {
            jarLock.lock();
            Class<?> loadedClass = findClassInner(name);
            return loadedClass;
        } finally {
            jarLock.unlock();
        }
    }

    private Class<?> findClassInner(String name) throws ClassNotFoundException {
        String entryName = classNameToEntryName(name);
        InputStream in = findEntryNameStream(entryName);
        Class<?> loadedClass = null;
        if (in != null) {
            loadedClass = loadClassFromStream(name, in);
        } else {
            loadedClass = super.findClass(name);
        }
        return loadedClass;
    }

    private String classNameToEntryName(String className) {
        return className.replace('.', '/').concat(".class");
    }

    private InputStream findEntryNameStream(String entryName) {
        for (File jarPath : jarPathList) {
            InputStream in = findStreamInJar(entryName, jarPath);
            if (in != null) {
                return in;
            }
        }
        return null;
    }

    private InputStream findStreamInJar(String entryName, File jarPath) {
        JarFile jarFile = openJarIfNecessary(jarPath);
        JarEntry entry = jarFile.getJarEntry(entryName);
        if (entry != null) {
            try {
                return jarFile.getInputStream(entry);
            } catch (IOException ioEx) {
            }
        }
        return null;
    }

    private JarFile openJarIfNecessary(File jarPath) {
        JarFile jarFile = openedJarFiles.get(jarPath);
        if (jarFile == null) {
            try {
                jarFile = new JarFile(jarPath);
                openedJarFiles.put(jarPath, jarFile);
            } catch (IOException ioEx) {
            }
        }
        return jarFile;
    }

    private Class<?> loadClassFromStream(String className, InputStream in) {
        try {
            byte[] classContent = streamToBytes(in);
            Class<?> loadedClass = defineClass(className, classContent, 0, classContent.length);
            return loadedClass;
        } catch (IOException ioEx) {
            return null;
        }
    }

    private byte[] streamToBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return out.toByteArray();
    }

    @Override
    protected URL findResource(String resName) {
        if (resName == null) return null;
        try {
            jarLock.lock();
            URL url = findResourceInner(resName);
            if (url == null) url = super.findResource(resName);
            return url;
        } finally {
            jarLock.unlock();
        }
    }

    private URL findResourceInner(String entryName) {
        for (File jarPath : jarPathList) {
            URL entryUrl = findResourceInJarFile(jarPath, entryName);
            if (entryUrl != null) return entryUrl;
        }
        return null;
    }

    private URL findResourceInJarFile(File jarPath, String entryName) {
        JarFile jarFile = openJarIfNecessary(jarPath);
        JarEntry jarEntry = jarFile.getJarEntry(entryName);
        if (jarEntry != null) {
            URL entryUrl = urlOfJarEntry(jarPath, entryName);
            if (entryUrl != null) {
                return entryUrl;
            }
        }
        return null;
    }

    private URL urlOfJarEntry(File jarPath, String entryName) {
        try {
            StringBuilder strBuilder = new StringBuilder("jar:");
            URL jarUrl = jarPath.toURI().toURL();
            strBuilder.append(jarUrl.toExternalForm());
            strBuilder.append('!');
            if (entryName.startsWith("/") == false) strBuilder.append("/");
            strBuilder.append(entryName);
            URL resUrl = new URL(strBuilder.toString());
            return resUrl;
        } catch (MalformedURLException malformedEx) {
            return null;
        }
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        if (name == null) return null;
        Collection<URL> localResList = findResourcesInner(name);
        Enumeration<URL> superResEnum = super.findResources(name);
        Vector<URL> foundResources = new Vector<URL>();
        foundResources.addAll(localResList);
        while (superResEnum.hasMoreElements()) {
            foundResources.add(superResEnum.nextElement());
        }
        return foundResources.elements();
    }

    private Collection<URL> findResourcesInner(String resName) {
        Collection<URL> localResList = new ArrayList<URL>();
        for (File jarPath : jarPathList) {
            URL resUrl = findResourceInJarFile(jarPath, resName);
            if (resUrl != null) {
                localResList.add(resUrl);
            }
        }
        return localResList;
    }
}
