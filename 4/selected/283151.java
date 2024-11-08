package com.memoire.vainstall;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * @version      $Id: VAClassLoader.java,v 1.5 2005/10/11 09:51:55 deniger Exp $
 * @author       Axel von Arnim
 */
public class VAClassLoader extends ClassLoader {

    public static final boolean DEBUG = "yes".equals(System.getProperty("DEBUG"));

    /**
   *  format ex. ("com.ice.util.UserProperties.class",byte[] data)
   */
    private Hashtable cache_;

    private long offset_;

    private File jarfile_;

    private File dllFile_;

    /**
   *  format ex. ("com.ice.util.UserProperties",Class)
   */
    private Hashtable classes = new Hashtable();

    public VAClassLoader(File jarfile, Long offset) {
        super();
        offset_ = offset.longValue();
        jarfile_ = jarfile;
        JarInputStream jar = null;
        try {
            cache_ = new Hashtable();
            printDebug("VAClassLoader: loading classes from " + jarfile.getName() + " (offset " + offset_ + ")...");
            FileInputStream stream = new FileInputStream(jarfile_);
            stream.skip(offset_);
            jar = new JarInputStream(stream);
            ZipEntry entry = jar.getNextEntry();
            byte[] data = null;
            while (entry != null) {
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    byte[] buffer = new byte[2048];
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while (true) {
                        int read = jar.read(buffer);
                        if (read == -1) break;
                        bos.write(buffer, 0, read);
                    }
                    data = bos.toByteArray();
                    bos.close();
                    jar.closeEntry();
                    String className = entryName.replace('/', '.');
                    printDebug("  className=" + className + " size=" + data.length);
                    byte[] toCache = new byte[data.length];
                    System.arraycopy(data, 0, toCache, 0, data.length);
                    cache_.put(className, data);
                }
                entry = jar.getNextEntry();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            printDebug("  closing jarFile.");
            if (jar != null) try {
                jar.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        return (loadClass(className, true));
    }

    public synchronized Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
        Class result;
        byte[] classBytes;
        result = (Class) classes.get(className);
        if (result != null) {
            return result;
        }
        try {
            result = super.findSystemClass(className);
            return result;
        } catch (ClassNotFoundException e) {
        }
        printDebug(className);
        classBytes = loadClassBytes(className);
        if (classBytes == null) {
            throw new ClassNotFoundException();
        }
        result = defineClass(className, classBytes, 0, classBytes.length);
        if (result == null) {
            throw new ClassFormatError();
        }
        if (resolveIt) resolveClass(result);
        classes.put(className, result);
        return result;
    }

    public synchronized InputStream getResourceAsStream(String name) {
        InputStream res = null;
        JarInputStream jar = null;
        try {
            printDebug("VAClassLoader: loading resource " + name);
            FileInputStream stream = new FileInputStream(jarfile_);
            stream.skip(offset_);
            jar = new JarInputStream(stream);
            ZipEntry entry = jar.getNextEntry();
            while ((entry != null) && (!entry.getName().equals(name))) entry = jar.getNextEntry();
            if (entry != null) {
                res = jar;
            }
        } catch (IOException t) {
            System.err.println(t);
            printDebug("  closing jarFile.");
            if (jar != null) try {
                jar.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        if (res == null) printDebug("  not found"); else printDebug("  OK");
        return res;
    }

    public synchronized URL getResource(String name) {
        URL res = null;
        try {
            printDebug("VAClassLoader: loading resource URL " + name);
            String jarUrl = jarfile_.toURL().toString();
            jarUrl = "jar:" + jarUrl + "!" + name;
            res = new URL(jarUrl);
        } catch (MalformedURLException ex) {
            printDebug("  null URL");
            res = null;
        }
        if (res == null) printDebug("  not found"); else printDebug("  OK");
        return res;
    }

    protected void finalize() {
        if (dllFile_.exists()) dllFile_.delete();
    }

    public static void printDebug(String msg) {
        if (DEBUG) System.err.println(msg);
    }

    protected byte[] loadClassBytes(String className) {
        className = formatClassName(className);
        printDebug("Trying to fetch=" + className);
        return (byte[]) cache_.get(className);
    }

    protected String formatClassName(String className) {
        return className + ".class";
    }
}
