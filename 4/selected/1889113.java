package org.codehaus.classworlds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Classloader for <code>ClassRealm</code>s.
 * <p/>
 * Loads classes from an "uberjar".
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @version $Id: UberJarRealmClassLoader.java 126 2006-01-12 04:17:51Z  $
 */
public class UberJarRealmClassLoader extends RealmClassLoader {

    private Map classIndex;

    private List urls;

    private Map jarIndexes;

    public UberJarRealmClassLoader(DefaultClassRealm realm) {
        super(realm);
        this.urls = new ArrayList();
        this.classIndex = new HashMap();
        this.jarIndexes = new HashMap();
    }

    public void addConstituent(URL constituent) {
        if ("jar".equals(constituent.getProtocol()) || constituent.toExternalForm().endsWith(".jar")) {
            buildIndexForJar(constituent);
        }
        this.urls.add(constituent);
        super.addConstituent(constituent);
    }

    private void buildIndexForJar(URL inUrl) {
        HashMap index = new HashMap();
        String urlText = null;
        if (inUrl.getProtocol().equals("jar")) {
            urlText = inUrl.toExternalForm();
        } else {
            urlText = "jar:" + inUrl.toExternalForm();
        }
        String resourceName;
        URL resourceUrl = null;
        try {
            JarInputStream in = new JarInputStream(inUrl.openStream());
            try {
                JarEntry entry = null;
                while ((entry = in.getNextJarEntry()) != null) {
                    resourceName = entry.getName();
                    resourceUrl = new URL(urlText + "!/" + resourceName);
                    index.put(resourceName, resourceUrl);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
        }
        jarIndexes.put(inUrl, index);
    }

    /**
     * Load a class directly from this classloader without
     * defering through any other <code>ClassRealm</code>.
     *
     * @param className The name of the class to load.
     * @return The loaded class.
     * @throws ClassNotFoundException If the class could not be found.
     */
    public Class loadClassDirect(String className) throws ClassNotFoundException {
        String classPath = className.replace('.', '/') + ".class";
        if (this.classIndex.containsKey(classPath)) {
            return (Class) this.classIndex.get(classPath);
        }
        Iterator urlIter = this.urls.iterator();
        URL eachUrl = null;
        byte[] classBytes = null;
        while ((classBytes == null) && (urlIter.hasNext())) {
            eachUrl = (URL) urlIter.next();
            if ("jar".equals(eachUrl.getProtocol()) || eachUrl.toExternalForm().endsWith(".jar")) {
                classBytes = findClassInJarStream(eachUrl, classPath);
            } else {
                classBytes = findClassInDirectoryUrl(eachUrl, classPath);
            }
        }
        if (classBytes == null) {
            return null;
        } else {
            Class cls = defineClass(className, classBytes, 0, classBytes.length);
            this.classIndex.put(classPath, cls);
            return cls;
        }
    }

    public URL findResource(String name) {
        URL resourceUrl = null;
        Iterator urlIter = this.urls.iterator();
        URL eachUrl = null;
        while (urlIter.hasNext()) {
            eachUrl = (URL) urlIter.next();
            if ("jar".equals(eachUrl.getProtocol()) || eachUrl.toExternalForm().endsWith(".jar")) {
                resourceUrl = findResourceInJarStream(eachUrl, name);
            } else {
                resourceUrl = findResourceInDirectoryUrl(eachUrl, name);
            }
            if (resourceUrl != null) {
                return resourceUrl;
            }
        }
        return null;
    }

    public Enumeration findResourcesDirect(String name) {
        Vector list = new Vector();
        URL resourceUrl = null;
        Iterator urlIter = this.urls.iterator();
        URL eachUrl = null;
        while (urlIter.hasNext()) {
            eachUrl = (URL) urlIter.next();
            if ("jar".equals(eachUrl.getProtocol()) || eachUrl.toExternalForm().endsWith(".jar")) {
                resourceUrl = findResourceInJarStream(eachUrl, name);
            } else {
                resourceUrl = findResourceInDirectoryUrl(eachUrl, name);
            }
            if (resourceUrl != null) {
                list.add(resourceUrl);
            }
        }
        return list.elements();
    }

    protected URL findResourceInJarStream(URL inUrl, String path) {
        return (URL) ((Map) jarIndexes.get(inUrl)).get(path);
    }

    protected URL findResourceInDirectoryUrl(URL inUrl, String path) {
        return null;
    }

    protected byte[] findClassInJarStream(URL inUrl, String path) {
        URL classUrl = (URL) ((Map) jarIndexes.get(inUrl)).get(path);
        if (classUrl != null) {
            try {
                return readStream(classUrl.openStream());
            } catch (IOException e) {
            }
        }
        return null;
    }

    protected byte[] findClassInDirectoryUrl(URL url, String path) {
        try {
            URL classUrl = new URL(url, path);
        } catch (IOException e) {
        }
        return null;
    }

    private byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[2048];
            int read = 0;
            while (in.available() > 0) {
                read = in.read(buffer, 0, buffer.length);
                if (read < 0) {
                    break;
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            out.close();
        }
    }
}
