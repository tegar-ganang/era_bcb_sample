package de.sciss.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *	Extends <code>java.net.URLClassLoader</code> to be
 *	able to dynamically add URLs to the classpath.
 *	URLs can refer to a jar (ending with '.jar', they can be a
 *	classes directory (ending with '/'),
 *	they can be a single class (ending with '.class').
 *
 *	@author		Hanns Holger Rutz
 *	@version	0.59, 25-Feb-08
 */
public class DynamicURLClassLoader extends URLClassLoader {

    private final Map mapSingleClasses = new HashMap();

    /**
	 *	Creates a new class loader instance
	 *	with no custom folders and jars existing.
	 *	To add these, call <code>addURL</code> repeatedly.
	 *
	 */
    public DynamicURLClassLoader() {
        super(new URL[0]);
    }

    /**
	 *	Creates a new class loader instance
	 *	with a given list of custom folders and jars.
	 */
    public DynamicURLClassLoader(URL[] urls) {
        this();
        for (int i = 0; i < urls.length; i++) addURL(urls[i]);
    }

    /**
	 *	Creates a new class loader instance
	 *	with no custom folders and jars existing.
	 *	To add these, call <code>addURL</code> repeatedly.
	 *
	 */
    public DynamicURLClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    /**
	 *	Creates a new class loader instance
	 *	with a given list of custom folders and jars.
	 */
    public DynamicURLClassLoader(URL[] urls, ClassLoader parent) {
        this(parent);
        for (int i = 0; i < urls.length; i++) addURL(urls[i]);
    }

    /**
	 *  Made a public method
	 */
    public void addURL(URL url) {
        final String path = url.getPath();
        if (path.endsWith(".class")) {
            final int i = path.lastIndexOf('/') + 1;
            mapSingleClasses.put(path.substring(i, path.length() - 6), url);
        } else {
            super.addURL(url);
        }
    }

    public void addURLs(URL[] urls) {
        for (int i = 0; i < urls.length; i++) addURL(urls[i]);
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            final URL url = (URL) mapSingleClasses.get(name);
            if (url != null) {
                final byte[] classBytes = loadClassBytes(url);
                if (classBytes != null) {
                    return defineClass(name, classBytes, 0, classBytes.length);
                }
            }
            throw e;
        }
    }

    private byte[] loadClassBytes(URL url) {
        final List collBytes = new ArrayList();
        final List collLen = new ArrayList();
        final byte[] classBytes;
        InputStream is = null;
        byte[] b;
        int len;
        int totalLen = 0;
        try {
            is = url.openStream();
            do {
                b = new byte[4096];
                len = is.read(b);
                if (len > 0) {
                    collBytes.add(b);
                    collLen.add(new Integer(len));
                    totalLen += len;
                }
            } while (len > 0);
            is.close();
            classBytes = new byte[totalLen];
            for (int i = 0, off = 0; i < collBytes.size(); i++) {
                len = ((Integer) collLen.get(i)).intValue();
                b = (byte[]) collBytes.get(i);
                System.arraycopy(b, 0, classBytes, off, len);
                off += len;
            }
            return classBytes;
        } catch (IOException e1) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e11) {
                }
            }
            return null;
        }
    }
}
