package org.codehaus.classworlds.uberjar.boot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.HashMap;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;

/**
 * Initial bootstrapping <code>ClassLoader</code>.
 *
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @version $Id: InitialClassLoader.java 78 2004-07-01 13:59:13Z jvanzyl $
 */
public class InitialClassLoader extends SecureClassLoader {

    /**
     * Class index.
     */
    private Map index;

    /**
     * Classworlds jar URL.
     */
    private URL classworldsJarUrl;

    /**
     * Construct.
     *
     * @throws Exception If an error occurs while attempting to perform
     *                   bootstrap initialization.
     */
    public InitialClassLoader() throws Exception {
        this.index = new HashMap();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL classUrl = getClass().getResource("InitialClassLoader.class");
        String urlText = classUrl.toExternalForm();
        int bangLoc = urlText.indexOf("!");
        System.setProperty("classworlds.lib", urlText.substring(0, bangLoc) + "!/WORLDS-INF/lib");
        this.classworldsJarUrl = new URL(urlText.substring(0, bangLoc) + "!/WORLDS-INF/classworlds.jar");
    }

    /**
     * @see ClassLoader
     */
    public synchronized Class findClass(String className) throws ClassNotFoundException {
        String classPath = className.replace('.', '/') + ".class";
        if (this.index.containsKey(classPath)) {
            return (Class) this.index.get(classPath);
        }
        try {
            JarInputStream in = new JarInputStream(this.classworldsJarUrl.openStream());
            try {
                JarEntry entry = null;
                while ((entry = in.getNextJarEntry()) != null) {
                    if (entry.getName().equals(classPath)) {
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
                            buffer = out.toByteArray();
                            Class cls = defineClass(className, buffer, 0, buffer.length);
                            this.index.put(className, cls);
                            return cls;
                        } finally {
                            out.close();
                        }
                    }
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ClassNotFoundException("io error reading stream for: " + className);
        }
        throw new ClassNotFoundException(className);
    }
}
