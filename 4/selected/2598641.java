package org.scohen.juploadr.uploadapi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.scohen.juploadr.prefs.Configuration;

public class PluginClassLoader extends ClassLoader {

    private Map<String, PluginCacheEntry> classCache = new HashMap<String, PluginCacheEntry>();

    private List<String> pluginClasses = new ArrayList<String>();

    private Set<String> packages = new HashSet<String>();

    private static PluginClassLoader instance;

    private static final Log log = LogFactory.getLog(PluginClassLoader.class);

    protected PluginClassLoader() {
        super();
        buildCache();
    }

    public static synchronized PluginClassLoader getInstance() {
        if (instance == null) {
            instance = new PluginClassLoader();
        }
        return instance;
    }

    private void buildCache() {
        File pluginsDirectory = new File(System.getProperty("user.dir"), "plugins");
        if (Configuration.isMac() && !pluginsDirectory.exists()) {
            pluginsDirectory = new File(System.getProperty("user.dir"), "JUploadr.app/Contents/Resources/Java/plugins/");
        }
        File[] jarFiles = pluginsDirectory.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().toLowerCase().endsWith(".jar") || pathname.getAbsolutePath().toLowerCase().endsWith(".zip");
            }
        });
        if (jarFiles != null) {
            for (int i = 0; i < jarFiles.length; i++) {
                File jarFile = jarFiles[i];
                JarFile jar;
                try {
                    jar = new JarFile(jarFile);
                    Manifest mf = jar.getManifest();
                    Map entries = mf.getMainAttributes();
                    String implementationClass = (String) entries.get(new Attributes.Name("Plugin-Class"));
                    if (implementationClass != null) {
                        pluginClasses.add(implementationClass);
                    }
                    Enumeration allEntries = jar.entries();
                    while (allEntries.hasMoreElements()) {
                        JarEntry entry = (JarEntry) allEntries.nextElement();
                        String className = toClassName(entry.getName());
                        classCache.put(className, new PluginCacheEntry(jar, className));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ImageUploadApi[] getApis() {
        ImageUploadApi[] apis = new ImageUploadApi[pluginClasses.size()];
        Iterator iter = pluginClasses.iterator();
        int i = 0;
        while (iter.hasNext()) {
            String next = (String) iter.next();
            try {
                Class c = loadClass(next);
                apis[i++] = (ImageUploadApi) c.newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return apis;
    }

    private String toClassName(String name) {
        String className = name;
        if (name.endsWith(".class")) {
            className = name.substring(0, name.lastIndexOf('.'));
            className = className.replace('/', '.');
        }
        return className;
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class toLoad = null;
        if (classCache.containsKey(name)) {
            PluginCacheEntry entry = (PluginCacheEntry) classCache.get(name);
            if (entry.clazz == null) {
                byte[] classBytes = loadClassBytes(entry.jar, name);
                if (classBytes != null) {
                    entry.clazz = defineClass(name, classBytes, 0, classBytes.length);
                    String packageName = name.substring(0, name.lastIndexOf('.'));
                    if (!packages.contains(packageName)) {
                        definePackage(packageName, "Juploadr", "1.0", "scohen.org", "Juploadr", "1.0", "scohen.org", null);
                        packages.add(packageName);
                    }
                    if (entry.clazz == null) {
                        throw new ClassFormatError();
                    }
                }
            }
            toLoad = entry.clazz;
        } else {
            return super.findClass(name);
        }
        return toLoad;
    }

    public InputStream getResourceAsStream(String name) {
        PluginCacheEntry entry = (PluginCacheEntry) classCache.get(name);
        if (entry != null) {
            JarEntry jarEntry = entry.jar.getJarEntry(name);
            try {
                return entry.jar.getInputStream(jarEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return super.getResourceAsStream(name);
        }
        return null;
    }

    private byte[] loadClassBytes(JarFile jar, String name) {
        String resourceName = name.replace('.', '/') + ".class";
        byte[] classData = null;
        JarEntry entry = (JarEntry) jar.getEntry(resourceName);
        if (entry != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) entry.getSize());
            byte[] buffer = new byte[1024 * 4];
            int numread = 0;
            try {
                InputStream jarFile = jar.getInputStream(entry);
                while ((numread = (jarFile.read(buffer))) > 0) {
                    baos.write(buffer, 0, numread);
                }
                classData = baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log.error("No entry " + resourceName + " in  " + jar.getName());
        }
        return classData;
    }

    private class PluginCacheEntry {

        public JarFile jar;

        public String className;

        public Class clazz;

        public PluginCacheEntry(JarFile jar, String className) {
            this.className = className;
            this.jar = jar;
        }
    }
}
