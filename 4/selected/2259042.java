package org.osid;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.ArrayList;
import java.util.jar.JarFile;

public class OsidClassLoader extends ClassLoader {

    private List jar_files = null;

    private File osid_jar_files = null;

    private ClassLoader parent = null;

    OsidClassLoader(ClassLoader parent, String plugins_dir) {
        super(parent);
        jar_files = new ArrayList();
        osid_jar_files = new File(plugins_dir);
        this.parent = parent;
        init();
    }

    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c;
        if ((c = findClass(name)) == null) c = super.loadClass(name, resolve);
        if (resolve) resolveClass(c);
        return (c);
    }

    public void init() {
        if (osid_jar_files.isDirectory()) {
            File[] files = osid_jar_files.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith(".jar")) {
                    try {
                        jar_files.add(new JarFile(files[i]));
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }
    }

    public Class findClass(String name) throws ClassNotFoundException {
        for (int i = 0; i < jar_files.size(); i++) {
            JarFile jar = (JarFile) jar_files.get(i);
            String entry = name.replaceAll("[.]", "/");
            entry = entry + ".class";
            if (jar.getEntry(entry) != null) {
                try {
                    byte[] data = loadClassData(entry, jar);
                    if (data.length == 0) {
                        continue;
                    }
                    return defineClass(name, data, 0, data.length);
                } catch (IOException ioe) {
                    continue;
                }
            }
        }
        return null;
    }

    private byte[] loadClassData(String name, JarFile file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(file.getInputStream(file.getEntry(name)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] results = new byte[0];
        if (in != null) {
            while (true) {
                byte[] bytes = new byte[4096];
                int read = in.read(bytes);
                if (read < 0) {
                    break;
                }
                out.write(bytes, 0, read);
            }
            results = out.toByteArray();
        }
        in.close();
        out.close();
        return results;
    }

    public URL getResource(String name) {
        for (int i = 0; i < jar_files.size(); i++) {
            try {
                JarFile jar = (JarFile) jar_files.get(i);
                if (jar.getEntry(name) != null) {
                    URL url = new URL("jar:file:" + jar.getName() + "!/" + name);
                    System.out.println(url);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public InputStream getResourceAsStream(String name) {
        for (int i = 0; i < jar_files.size(); i++) {
            try {
                JarFile jar = (JarFile) jar_files.get(i);
                if (jar.getEntry(name) != null) {
                    BufferedInputStream in = new BufferedInputStream(jar.getInputStream(jar.getEntry(name)));
                    return in;
                }
            } catch (IOException ioe) {
            }
        }
        return null;
    }
}
