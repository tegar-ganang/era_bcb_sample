package edu.rice.cs.drjava;

import java.net.URLClassLoader;
import java.net.URL;
import java.io.File;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Custom classloader, that loads from files or URLs
 * modeled after the NextGen classloader (edu.rice.cs.nextgen.classloader.NextGenLoader)
 * $Id: DrJavaClassLoader.java 1029 2002-08-09 06:12:23Z theoyaung $
 */
public class DrJavaClassLoader extends ClassLoader {

    public DrJavaClassLoader() {
        super();
    }

    public DrJavaClassLoader(URL[] urls) {
        super();
    }

    public DrJavaClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
    }

    private static final int BUFFER_SIZE = 0x2800;

    private final byte[] readBuffer = new byte[BUFFER_SIZE];

    public static String dotToSlash(String s) {
        return s.replace('.', '/');
    }

    /** 
   * Replace all instances of find with repl in orig, and return the new
   * String.
   */
    public static String replaceSubstring(String orig, String find, String repl) {
        StringBuffer buf = new StringBuffer();
        int pos = 0;
        while (pos < orig.length()) {
            int foundPos = orig.indexOf(find, pos);
            if (foundPos == -1) {
                break;
            } else {
                buf.append(orig.substring(pos, foundPos));
                buf.append(repl);
                pos = foundPos + find.length();
            }
        }
        buf.append(orig.substring(pos));
        return buf.toString();
    }

    /** Gets byte[] for class file, or throws IOException. */
    private synchronized byte[] readClassFile(String className) throws IOException {
        String fileName = dotToSlash(className) + ".class";
        InputStream stream = getResourceAsStream(fileName);
        if (stream == null) {
            throw new IOException("Resource not found: " + fileName);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
        for (int c = stream.read(readBuffer); c != -1; c = stream.read(readBuffer)) {
            baos.write(readBuffer, 0, c);
        }
        stream.close();
        baos.close();
        return baos.toByteArray();
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz;
        if (mustUseSystemLoader(name)) {
            clazz = findSystemClass(name);
        } else {
            try {
                byte[] classData = readClassFile(name);
                clazz = defineClass(name, classData, 0, classData.length);
            } catch (IOException ioe) {
                throw new ClassNotFoundException("IO Exception in reading class file: " + ioe);
            }
        }
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    /**
   * Map of package name (string) to whether must use system loader (boolean).
   */
    private final HashMap _checkedPackages = new HashMap();

    public boolean mustUseSystemLoader(String name) {
        if (name.startsWith("java.") || name.startsWith("javax.")) {
            return true;
        }
        SecurityManager _security = System.getSecurityManager();
        if (_security == null) {
            return false;
        }
        int lastDot = name.lastIndexOf('.');
        String packageName;
        if (lastDot == -1) {
            packageName = "";
        } else {
            packageName = name.substring(0, lastDot);
        }
        Object cacheCheck = _checkedPackages.get(packageName);
        if (cacheCheck != null) {
            return ((Boolean) cacheCheck).booleanValue();
        }
        try {
            _security.checkPackageDefinition(packageName);
            _checkedPackages.put(packageName, Boolean.FALSE);
            return false;
        } catch (SecurityException se) {
            _checkedPackages.put(packageName, Boolean.TRUE);
            return true;
        }
    }
}
