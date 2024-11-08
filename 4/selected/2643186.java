package org.javagroup.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

public class URLClassLoader extends java.net.URLClassLoader {

    protected Vector _urlClassPath;

    protected Hashtable _classCache;

    protected Hashtable domains = new Hashtable();

    /** Creates a ClassLoader with a list of classpath URLs.
	 * @param classpath The URLs to search for classes relative to.
	 **/
    public URLClassLoader(URL[] classpath) {
        super((classpath == null) ? new URL[0] : classpath);
        _urlClassPath = new Vector();
        _classCache = new Hashtable();
        addClassPath(classpath);
    }

    public void addClassPath(URL[] classpath) {
        if (classpath != null) {
            for (int i = 0; i < classpath.length; i++) addClassPath(classpath[i]);
        }
    }

    /** Add a URL to the classpath.  This URL is searched for for classes.
	 * @param classpath The base URL to search.
	 **/
    public void addClassPath(URL classpath) {
        if (classpath == null) return;
        if ((classpath.getFile().toLowerCase().endsWith(".zip")) || (classpath.getFile().toLowerCase().endsWith(".jar"))) {
            try {
                _urlClassPath.addElement(new URL("jar:" + classpath.toString() + "!/"));
            } catch (MalformedURLException exception) {
            }
        } else _urlClassPath.addElement(classpath);
    }

    public Class loadMainClass(String name) throws ClassNotFoundException {
        return loadClass(name, true);
    }

    /** Try to read the byte[] data for a class file from the classpath.
	 * @param name The fully-qualified name of the class.
	 * @return A byte[] containing the classfile data, or null if not found.
	 **/
    protected byte[] readClassFile(String classname, URL[] pathUsed) {
        classname = classname.replace('.', '/') + ".class";
        return readFile(classname, pathUsed);
    }

    public URL getResource(String name) {
        return super.getResource(name);
    }

    protected byte[] readFile(String name) {
        return readFile(name, null);
    }

    protected byte[] readFile(String name, URL pathUsed[]) {
        Enumeration classpath = _urlClassPath.elements();
        byte[] data = null;
        URL base_path;
        while ((data == null) && (classpath.hasMoreElements())) {
            base_path = (URL) classpath.nextElement();
            try {
                URL path = new URL(base_path, name);
                ByteArrayOutputStream out_buffer = new ByteArrayOutputStream();
                InputStream in = new BufferedInputStream(path.openStream());
                int octet;
                while ((octet = in.read()) != -1) out_buffer.write(octet);
                data = out_buffer.toByteArray();
                if (pathUsed != null) pathUsed[0] = base_path;
            } catch (IOException e) {
            }
        }
        return data;
    }

    /** Converts a path string into an array of URLs.
	 * eg. "foo:http://bar/" would become a 2-URL array, with "file:///foo/"
	 * and "http://bar/".
	 * @param classpath The path to decode, entries delimited by the
	 * appropriate charactor for the platform.
	 **/
    public static URL[] decodePathString(String classpath) {
        URL base_url = null;
        try {
            base_url = new URL("file:/");
        } catch (MalformedURLException e) {
        }
        Vector classpath_urls = new Vector();
        if ((base_url != null) && (classpath != null)) {
            StringTokenizer tok = new StringTokenizer(classpath, ",");
            while (tok.hasMoreTokens()) {
                String path = tok.nextToken();
                URL path_url = null;
                try {
                    path_url = new URL(path);
                } catch (MalformedURLException e) {
                    try {
                        path_url = new URL(base_url, path);
                    } catch (Exception e2) {
                    }
                }
                if (path_url != null) classpath_urls.addElement(path_url);
            }
        }
        URL[] paths = null;
        if (!classpath_urls.isEmpty()) {
            paths = new URL[classpath_urls.size()];
            for (int i = 0; i < paths.length; i++) paths[i] = (URL) classpath_urls.elementAt(i);
        }
        return paths;
    }

    /**
	 * Take a byte array of class data and modify it to ensure that the class is public.
	 * This is used for the "main" classes of applications.
	 */
    public void forcePublic(byte[] theClass) {
        int constant_pool_count = ((theClass[8] & 0xff) << 8) | (theClass[9] & 0xff);
        int currOffset = 10;
        for (int i = 1; i < constant_pool_count; i++) {
            switch(theClass[currOffset] & 0xff) {
                case 7:
                case 8:
                    currOffset += 3;
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 3:
                case 4:
                    currOffset += 5;
                    break;
                case 5:
                case 6:
                    currOffset += 9;
                    i++;
                    break;
                case 1:
                    int length = ((theClass[++currOffset] & 0xff) << 8) | (theClass[++currOffset] & 0xff);
                    currOffset += length + 1;
                    break;
                default:
                    return;
            }
        }
        theClass[currOffset + 1] |= 1;
    }
}
