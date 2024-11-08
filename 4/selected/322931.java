package org.sqlanyware.sqlwclient.utils.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import org.apache.log4j.Logger;

public class JarStreamsClassLoader extends ClassLoader {

    private static final Logger LOGGER = Logger.getLogger(JarStreamsClassLoader.class);

    private List<byte[]> codes = new LinkedList<byte[]>();

    private Map<String, File> resourceCache = new HashMap<String, File>();

    public JarStreamsClassLoader(final ClassLoader parent, final List<byte[]> codes) {
        super(parent);
        this.codes = codes;
    }

    public List<Class<?>> listClasses() throws IOException {
        final List<Class<?>> result = new LinkedList<Class<?>>();
        for (final byte[] code : this.codes) {
            final ByteArrayInputStream bais = new ByteArrayInputStream(code);
            final JarInputStream jis = new JarInputStream(bais);
            ZipEntry jarEntry = null;
            while ((jarEntry = jis.getNextEntry()) != null) {
                final String entryName = jarEntry.getName();
                if (entryName.endsWith(".class")) {
                    String className = entryName.substring(0, entryName.lastIndexOf(".class"));
                    className = className.replace('/', '.');
                    final Class<?> existingClazz = findLoadedClass(className);
                    if (existingClazz != null) {
                        result.add(existingClazz);
                    } else {
                        try {
                            final byte[] b = getCurrentEntryContent(jis);
                            final Class<?> clazz = defineClass(className, b, 0, b.length);
                            resolveClass(clazz);
                            result.add(clazz);
                        } catch (final Error error) {
                            LOGGER.warn("Error while loading class", error);
                        }
                    }
                }
            }
        }
        return result;
    }

    public List<String> listClasses(final IClassFilter clazzFilter) throws IOException, ClassNotFoundException {
        final List<String> result = new LinkedList<String>();
        final List<Class<?>> classes = listClasses();
        for (final Class<?> clazz : classes) {
            if (clazzFilter.match(clazz)) {
                result.add(clazz.getName());
            }
        }
        return result;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            Class<?> result = null;
            byte[] classCode = loadClassFromStream(name);
            if (null != classCode) {
                result = defineClass(name, classCode, 0, classCode.length);
            }
            if (null == result) {
                throw new ClassNotFoundException();
            }
            return result;
        } catch (final IOException ioException) {
            throw new ClassNotFoundException(ioException.getLocalizedMessage(), ioException);
        }
    }

    @Override
    protected URL findResource(final String name) {
        try {
            File localResourceFile = this.resourceCache.get(name);
            if (null == localResourceFile) {
                final byte[] resourceBytes = loadResourceFromStream(name);
                if (resourceBytes == null) {
                    return null;
                }
                localResourceFile = createLocalResourceFile(name, resourceBytes);
                this.resourceCache.put(name, localResourceFile);
            }
            final URL resourceURL = getLocalResourceURL(localResourceFile);
            return resourceURL;
        } catch (final IOException ioException) {
            ioException.printStackTrace();
            return null;
        }
    }

    protected URL getLocalResourceURL(File file) throws MalformedURLException {
        return file.toURL();
    }

    protected File createLocalResourceFile(final String name, final byte[] bytes) throws MalformedURLException, FileNotFoundException, IOException {
        final File resFile = File.createTempFile("__temp_res_", "_" + createLocalResourceName(name));
        resFile.deleteOnExit();
        final FileOutputStream fos = new FileOutputStream(resFile);
        fos.write(bytes, 0, bytes.length);
        fos.close();
        return resFile;
    }

    protected String createLocalResourceName(String name) {
        return name.replace('/', '_');
    }

    protected byte[] loadClassFromStream(final String className) throws IOException {
        byte[] result = null;
        final String entryName = className.replace('.', '/') + ".class";
        final Iterator<byte[]> codeIterator = this.codes.iterator();
        while (codeIterator.hasNext() && result == null) {
            final byte[] code = codeIterator.next();
            final ByteArrayInputStream bais = new ByteArrayInputStream(code);
            final JarInputStream jis = new JarInputStream(bais, false);
            try {
                ZipEntry jarEntry = null;
                while ((jarEntry = jis.getNextEntry()) != null && result == null) {
                    final String currentEntryName = jarEntry.getName();
                    if (entryName.equals(currentEntryName)) {
                        result = getCurrentEntryContent(jis);
                    }
                }
            } finally {
                if (null != jis) {
                    try {
                        jis.close();
                    } catch (final IOException ioException) {
                    }
                }
            }
        }
        return result;
    }

    protected byte[] loadResourceFromStream(final String className) throws IOException {
        byte[] result = null;
        final String entryName = className;
        final Iterator<byte[]> codeIterator = this.codes.iterator();
        while (codeIterator.hasNext() && result == null) {
            final byte[] code = codeIterator.next();
            final ByteArrayInputStream bais = new ByteArrayInputStream(code);
            final JarInputStream jis = new JarInputStream(bais, false);
            try {
                ZipEntry jarEntry = null;
                while ((jarEntry = jis.getNextEntry()) != null && result == null) {
                    final String currentEntryName = jarEntry.getName();
                    if (entryName.equals(currentEntryName)) {
                        result = getCurrentEntryContent(jis);
                    }
                }
            } finally {
                if (null != jis) {
                    try {
                        jis.close();
                    } catch (final IOException ioException) {
                    }
                }
            }
        }
        return result;
    }

    protected static byte[] getCurrentEntryContent(final JarInputStream jis) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int readLen = 0;
        final byte[] buffer = new byte[256000];
        while ((readLen = jis.read(buffer)) != -1) {
            baos.write(buffer, 0, readLen);
        }
        return baos.toByteArray();
    }

    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
            } catch (final ClassNotFoundException classNotFoundException) {
                return super.loadClass(name, resolve);
            }
        }
        return c;
    }
}
