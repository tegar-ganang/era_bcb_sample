package org.dengues.commons.jdk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf 2008-1-7 qiang.zhang $
 * 
 */
public class CustomClassLoader extends ClassLoader {

    /** scanned class path */
    private Vector fPathItems;

    private ClassLoader delegate;

    public CustomClassLoader() {
    }

    public CustomClassLoader(String classPath) {
        scanPath(classPath);
    }

    public CustomClassLoader(String classPath, ClassLoader delegate) {
        this(classPath);
        this.delegate = delegate;
    }

    protected void scanPath(String classPath) {
        String separator = System.getProperty("path.separator");
        fPathItems = new Vector(10);
        StringTokenizer st = new StringTokenizer(classPath, separator);
        while (st.hasMoreTokens()) {
            fPathItems.addElement(st.nextToken());
        }
    }

    @Override
    public URL getResource(String name) {
        return ClassLoader.getSystemResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return ClassLoader.getSystemResourceAsStream(name);
    }

    @Override
    public synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (delegate != null) {
            c = delegate.loadClass(name);
        }
        if (c != null) return c;
        try {
            c = findSystemClass(name);
            return c;
        } catch (ClassNotFoundException e) {
            c = null;
        }
        if (c == null) {
            byte[] data = lookupClassData(name);
            if (data == null) throw new ClassNotFoundException();
            c = defineClass(name, data, 0, data.length);
        }
        if (resolve) resolveClass(c);
        return c;
    }

    private byte[] lookupClassData(String className) throws ClassNotFoundException {
        byte[] data = null;
        for (int i = 0; i < fPathItems.size(); i++) {
            String path = (String) fPathItems.elementAt(i);
            String fileName = className.replace('.', '/') + ".class";
            if (isJar(path)) {
                data = loadJarData(path, fileName);
            } else {
                data = loadFileData(path, fileName);
            }
            if (data != null) return data;
        }
        throw new ClassNotFoundException(className);
    }

    boolean isJar(String pathEntry) {
        return pathEntry.endsWith(".jar") || pathEntry.endsWith(".zip");
    }

    private byte[] loadFileData(String path, String fileName) {
        File file = new File(path, fileName);
        if (file.exists()) {
            return getClassData(file);
        }
        return null;
    }

    private byte[] getClassData(File f) {
        try {
            FileInputStream stream = new FileInputStream(f);
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = stream.read(b)) != -1) out.write(b, 0, n);
            stream.close();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }

    private byte[] loadJarData(String path, String fileName) {
        ZipFile zipFile = null;
        InputStream stream = null;
        File archive = new File(path);
        if (!archive.exists()) return null;
        try {
            zipFile = new ZipFile(archive);
        } catch (IOException io) {
            return null;
        }
        ZipEntry entry = zipFile.getEntry(fileName);
        if (entry == null) return null;
        int size = (int) entry.getSize();
        try {
            stream = zipFile.getInputStream(entry);
            byte[] data = new byte[size];
            int pos = 0;
            while (pos < size) {
                int n = stream.read(data, pos, data.length - pos);
                pos += n;
            }
            zipFile.close();
            return data;
        } catch (IOException e) {
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
            }
        }
        return null;
    }
}
