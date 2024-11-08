package com.atech.utils.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This utility class was based originally on <a href="private.php?do=newpm&u=47838">Daniel Le Berre</a>'s 
 * <code>RTSI</code> class. This class can be called in different modes, but the principal use 
 * is to determine what subclasses/implementations of a given class/interface exist in the current 
 * runtime environment.
 * @author Daniel Le Berre, Elliott Wade
 */
public class ClassFinder {

    private Class<?> searchClass = null;

    private Map<URL, String> classpathLocations = new HashMap<URL, String>();

    private Map<Class<?>, URL> results = new HashMap<Class<?>, URL>();

    private List<Throwable> errors = new ArrayList<Throwable>();

    private boolean working = false;

    public ClassFinder() {
        refreshLocations();
    }

    public ClassFinder(String jar_starts_with) {
        refreshLocations();
    }

    /**
     * Rescan the classpath, cacheing all possible file locations.
     */
    public final void refreshLocations() {
        synchronized (classpathLocations) {
            classpathLocations = getClasspathLocations();
        }
    }

    public final void refreshLocations(String jar_starts_with) {
        synchronized (classpathLocations) {
            classpathLocations = getClasspathLocations(jar_starts_with);
        }
    }

    /**
     * @param fqcn Name of superclass/interface on which to search
     */
    public final Vector<Class<?>> findSubclasses(String fqcn) {
        synchronized (classpathLocations) {
            synchronized (results) {
                try {
                    working = true;
                    searchClass = null;
                    errors = new ArrayList<Throwable>();
                    results = new TreeMap<Class<?>, URL>(CLASS_COMPARATOR);
                    if (fqcn.startsWith(".") || fqcn.endsWith(".")) {
                        return new Vector<Class<?>>();
                    }
                    try {
                        searchClass = Class.forName(fqcn);
                    } catch (ClassNotFoundException ex) {
                        errors.add(ex);
                        return new Vector<Class<?>>();
                    }
                    return findSubclasses(searchClass, classpathLocations);
                } finally {
                    working = false;
                }
            }
        }
    }

    public final List<Throwable> getErrors() {
        return new ArrayList<Throwable>(errors);
    }

    /**
     * The result of the last search is cached in this object, along
     * with the URL that corresponds to each class returned. This 
     * method may be called to query the cache for the location at
     * which the given class was found. <code>null</code> will be
     * returned if the given class was not found during the last
     * search, or if the result cache has been cleared.
     */
    public final URL getLocationOf(Class<?> cls) {
        if (results != null) return results.get(cls); else return null;
    }

    /**
     * Determine every URL location defined by the current classpath, and
     * it's associated package name.
     */
    public final Map<URL, String> getClasspathLocations() {
        Map<URL, String> map = new TreeMap<URL, String>(URL_COMPARATOR);
        File file = null;
        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");
        StringTokenizer st = new StringTokenizer(classpath, pathSep);
        while (st.hasMoreTokens()) {
            String path = st.nextToken();
            file = new File(path);
            include(null, file, map);
        }
        Iterator<URL> it = map.keySet().iterator();
        while (it.hasNext()) {
            URL url = it.next();
        }
        return map;
    }

    public final Map<URL, String> getClasspathLocations(String jar_starts_with) {
        Map<URL, String> map = new TreeMap<URL, String>(URL_COMPARATOR);
        File file = null;
        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");
        StringTokenizer st = new StringTokenizer(classpath, pathSep);
        while (st.hasMoreTokens()) {
            String path = st.nextToken();
            file = new File(path);
            if (file.getName().startsWith(jar_starts_with)) {
                include(null, file, map);
            }
        }
        Iterator<URL> it = map.keySet().iterator();
        while (it.hasNext()) {
            URL url = it.next();
        }
        return map;
    }

    private static final FileFilter DIRECTORIES_ONLY = new FileFilter() {

        public boolean accept(File f) {
            if (f.exists() && f.isDirectory()) return true; else return false;
        }
    };

    private static final Comparator<URL> URL_COMPARATOR = new Comparator<URL>() {

        public int compare(URL u1, URL u2) {
            return String.valueOf(u1).compareTo(String.valueOf(u2));
        }
    };

    private static final Comparator<Class<?>> CLASS_COMPARATOR = new Comparator<Class<?>>() {

        public int compare(Class<?> c1, Class<?> c2) {
            return String.valueOf(c1).compareTo(String.valueOf(c2));
        }
    };

    private final void include(String name, File file, Map<URL, String> map) {
        if (!file.exists()) return;
        if (!file.isDirectory()) {
            includeJar(file, map);
            return;
        }
        if (name == null) name = ""; else name += ".";
        File[] dirs = file.listFiles(DIRECTORIES_ONLY);
        for (int i = 0; i < dirs.length; i++) {
            try {
                map.put(new URL("file://" + dirs[i].getCanonicalPath()), name + dirs[i].getName());
            } catch (IOException ioe) {
                return;
            }
            include(name + dirs[i].getName(), dirs[i], map);
        }
    }

    private void includeJar(File file, Map<URL, String> map) {
        if (file.isDirectory()) return;
        URL jarURL = null;
        JarFile jar = null;
        try {
            jarURL = new URL("file:/" + file.getCanonicalPath());
            jarURL = new URL("jar:" + jarURL.toExternalForm() + "!/");
            JarURLConnection conn = (JarURLConnection) jarURL.openConnection();
            jar = conn.getJarFile();
        } catch (Exception e) {
            return;
        }
        if (jar == null || jarURL == null) return;
        map.put(jarURL, "");
        Enumeration<JarEntry> e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
            if (entry.isDirectory()) {
                if (entry.getName().toUpperCase().equals("META-INF/")) continue;
                try {
                    map.put(new URL(jarURL.toExternalForm() + entry.getName()), packageNameFor(entry));
                } catch (MalformedURLException murl) {
                    continue;
                }
            }
        }
    }

    private static String packageNameFor(JarEntry entry) {
        if (entry == null) return "";
        String s = entry.getName();
        if (s == null) return "";
        if (s.length() == 0) return s;
        if (s.startsWith("/")) s = s.substring(1, s.length());
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.replace('/', '.');
    }

    private final void includeResourceLocations(String packageName, Map<URL, String> map) {
        try {
            Enumeration<URL> resourceLocations = ClassFinder.class.getClassLoader().getResources(getPackagePath(packageName));
            while (resourceLocations.hasMoreElements()) {
                map.put(resourceLocations.nextElement(), packageName);
            }
        } catch (Exception e) {
            errors.add(e);
            return;
        }
    }

    private final Vector<Class<?>> findSubclasses(Class<?> superClass, Map<URL, String> locations) {
        Vector<Class<?>> v = new Vector<Class<?>>();
        Vector<Class<?>> w = null;
        Iterator<URL> it = locations.keySet().iterator();
        while (it.hasNext()) {
            URL url = it.next();
            w = findSubclasses(url, locations.get(url), superClass);
            if (w != null && (w.size() > 0)) v.addAll(w);
        }
        return v;
    }

    private final Vector<Class<?>> findSubclasses(URL location, String packageName, Class<?> superClass) {
        synchronized (results) {
            Map<Class<?>, URL> thisResult = new TreeMap<Class<?>, URL>(CLASS_COMPARATOR);
            Vector<Class<?>> v = new Vector<Class<?>>();
            String fqcn = searchClass.getName();
            List<URL> knownLocations = new ArrayList<URL>();
            knownLocations.add(location);
            for (int loc = 0; loc < knownLocations.size(); loc++) {
                URL url = knownLocations.get(loc);
                File directory = new File(url.getFile());
                if (directory.exists()) {
                    String[] files = directory.list();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].endsWith(".class")) {
                            String classname = files[i].substring(0, files[i].length() - 6);
                            try {
                                Class<?> c = Class.forName(packageName + "." + classname);
                                if (superClass.isAssignableFrom(c) && !fqcn.equals(packageName + "." + classname)) {
                                    thisResult.put(c, url);
                                }
                            } catch (ClassNotFoundException cnfex) {
                                errors.add(cnfex);
                            } catch (Exception ex) {
                                errors.add(ex);
                            }
                        }
                    }
                } else {
                    try {
                        JarURLConnection conn = (JarURLConnection) url.openConnection();
                        JarFile jarFile = conn.getJarFile();
                        Enumeration<JarEntry> e = jarFile.entries();
                        while (e.hasMoreElements()) {
                            JarEntry entry = e.nextElement();
                            String entryname = entry.getName();
                            if (!entry.isDirectory() && entryname.endsWith(".class")) {
                                String classname = entryname.substring(0, entryname.length() - 6);
                                if (classname.startsWith("/")) classname = classname.substring(1);
                                classname = classname.replace('/', '.');
                                try {
                                    Class c = Class.forName(classname);
                                    if (superClass.isAssignableFrom(c) && !fqcn.equals(classname)) {
                                        thisResult.put(c, url);
                                    }
                                } catch (ClassNotFoundException cnfex) {
                                    errors.add(cnfex);
                                } catch (NoClassDefFoundError ncdfe) {
                                    errors.add(ncdfe);
                                } catch (UnsatisfiedLinkError ule) {
                                    errors.add(ule);
                                } catch (Exception exception) {
                                    errors.add(exception);
                                } catch (Error error) {
                                    errors.add(error);
                                }
                            }
                        }
                    } catch (IOException ioex) {
                        errors.add(ioex);
                    }
                }
            }
            results.putAll(thisResult);
            Iterator<Class<?>> it = thisResult.keySet().iterator();
            while (it.hasNext()) {
                v.add(it.next());
            }
            return v;
        }
    }

    private static final String getPackagePath(String packageName) {
        String path = new String(packageName);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        path = path.replace('.', '/');
        if (!path.endsWith("/")) path += "/";
        if (path.startsWith("/")) path = path.substring(1, path.length());
        return path;
    }

    public static void main(String[] args) {
        ClassFinder finder = null;
        Vector<Class<?>> v = null;
        List<Throwable> errors = null;
        if (args.length == 1) {
            finder = new ClassFinder();
            v = finder.findSubclasses(args[0]);
            errors = finder.getErrors();
        } else {
            System.out.println("Usage: java ClassFinder <fully.qualified.superclass.name>");
            return;
        }
        System.out.println("RESULTS:");
        if (v != null && v.size() > 0) {
            for (Class<?> cls : v) {
                System.out.println(cls + " in " + ((finder != null) ? String.valueOf(finder.getLocationOf(cls)) : "?"));
            }
        } else {
            System.out.println("No subclasses of " + args[0] + " found.");
        }
    }
}
