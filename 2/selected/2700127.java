package org.semanticweb.mmm.mr3.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.prefs.*;
import org.apache.oro.text.perl.*;
import org.semanticweb.mmm.mr3.*;
import org.semanticweb.mmm.mr3.data.*;
import org.semanticweb.mmm.mr3.plugin.*;

/**
 * @author takeshi morita
 */
public class PluginLoader {

    private static String pluginPath;

    private static ClassLoader classLoader;

    private static Collection<Manifest> manifests;

    private static SortedMap<String, List> pluginMenuMap;

    private static FilenameFilter jarFilter = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    public static Map getPluginMenuMap() {
        Collection<File> files = null;
        manifests = new ArrayList<Manifest>();
        pluginMenuMap = new TreeMap<String, List>();
        try {
            files = getClassPathFiles();
            classLoader = createClassLoader(files);
            Thread.currentThread().setContextClassLoader(classLoader);
        } catch (SecurityException e) {
            System.err.println(e);
        }
        loadManifests();
        processManifests();
        return Collections.unmodifiableMap(pluginMenuMap);
    }

    private static Collection<File> getClassPathFiles() {
        Collection<File> files = new ArrayList<File>();
        Preferences userPrefs = Preferences.userNodeForPackage(MR3.class);
        pluginPath = userPrefs.get(PrefConstants.PluginsDirectory, System.getProperty("user.dir"));
        if (pluginPath.equals(System.getProperty("user.dir"))) {
            pluginPath += "/plugins";
        }
        if (pluginPath != null) {
            File directory = new File(pluginPath);
            if (directory.exists()) {
                files.add(directory);
                File[] fileArray = directory.listFiles(jarFilter);
                Arrays.sort(fileArray);
                files.addAll(Arrays.asList(fileArray));
            }
        }
        return files;
    }

    private static ClassLoader createClassLoader(Collection<File> files) {
        Collection<URL> urls = new ArrayList<URL>();
        for (File file : files) {
            try {
                urls.add(file.toURL());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        URL[] urlArray = urls.toArray(new URL[urls.size()]);
        return new URLClassLoader(urlArray, PluginLoader.class.getClassLoader());
    }

    private static void loadManifests() {
        Perl5Util util = new Perl5Util();
        try {
            for (Enumeration e = classLoader.getResources("META-INF/MANIFEST.MF"); e.hasMoreElements(); ) {
                URL url = (URL) e.nextElement();
                if (util.match("/" + pluginPath.replace('\\', '/') + "/", url.getFile())) {
                    InputStream inputStream = url.openStream();
                    manifests.add(new Manifest(inputStream));
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processManifests() {
        for (Manifest m : manifests) {
            processManifest(m);
        }
    }

    private static final String PLUGIN_MENU_KEY = "menu-name";

    private static final String PLUGIN_NAME_KEY = "plugin-name";

    private static final String PLUGIN_CREATOR_KEY = "creator";

    private static final String PLUGIN_DATE_KEY = "date";

    private static final String PLUGIN_DESCRIPTION_KEY = "description";

    private static void processManifest(Manifest manifest) {
        Iterator i = manifest.getEntries().keySet().iterator();
        while (i.hasNext()) {
            String attributeName = (String) i.next();
            Attributes attributes = manifest.getAttributes(attributeName);
            if (attributes.getValue(PLUGIN_NAME_KEY) == null && attributes.getValue(PLUGIN_MENU_KEY) == null) {
                continue;
            }
            String className = attributeNameToClassName(attributeName);
            Class classObj = forName(className);
            if (classObj == null || classObj.getSuperclass() == null) {
                continue;
            }
            if (classObj.getSuperclass().equals(MR3Plugin.class)) {
                String pluginName = attributes.getValue(PLUGIN_NAME_KEY);
                if (pluginName == null) {
                    pluginName = attributes.getValue(PLUGIN_MENU_KEY);
                }
                if (pluginName == null) {
                    continue;
                }
                List<Object> pluginInfo = new ArrayList<Object>();
                pluginInfo.add(classObj);
                pluginInfo.add(attributes.getValue(PLUGIN_CREATOR_KEY));
                pluginInfo.add(attributes.getValue(PLUGIN_DATE_KEY));
                pluginInfo.add(attributes.getValue(PLUGIN_DESCRIPTION_KEY));
                pluginMenuMap.put(pluginName, pluginInfo);
            }
        }
    }

    private static Class forName(String className) {
        Class clas = null;
        try {
            clas = Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException cnfe) {
        } catch (Error error) {
            error.printStackTrace();
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return clas;
    }

    private static String attributeNameToClassName(String attributeName) {
        String className;
        if (attributeName.endsWith(".class")) {
            className = attributeName.substring(0, attributeName.length() - 6);
        } else {
            className = attributeName;
        }
        className = className.replace('/', '.');
        return className;
    }
}
