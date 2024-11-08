package org.sapp;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Jacob Hookom
 * @author Roland Huss
 * @version $Id: Classpath.java,v 1.8 2007/09/10 15:42:48 youngm Exp $
 */
public final class Classpath {

    /**
	 * 
	 */
    public Classpath() {
        super();
    }

    public static URL[] search(String prefix, String suffix) throws IOException {
        return search(Thread.currentThread().getContextClassLoader(), prefix, suffix);
    }

    public static URL[] search(ClassLoader cl, String prefix, String suffix) throws IOException {
        Enumeration[] e = new Enumeration[] { cl.getResources(prefix), cl.getResources(prefix + "MANIFEST.MF") };
        Set all = new LinkedHashSet();
        URL url;
        URLConnection conn;
        JarFile jarFile;
        for (int i = 0, s = e.length; i < s; ++i) {
            while (e[i].hasMoreElements()) {
                url = (URL) e[i].nextElement();
                conn = url.openConnection();
                conn.setUseCaches(false);
                conn.setDefaultUseCaches(false);
                if (conn instanceof JarURLConnection) {
                    jarFile = ((JarURLConnection) conn).getJarFile();
                } else {
                    jarFile = getAlternativeJarFile(url);
                }
                if (jarFile != null) {
                    searchJar(cl, all, jarFile, prefix, suffix);
                } else {
                    searchDir(all, new File(URLDecoder.decode(url.getFile(), "UTF-8")), suffix);
                }
            }
        }
        URL[] urlArray = (URL[]) all.toArray(new URL[all.size()]);
        return urlArray;
    }

    private static void searchDir(Set result, File file, String suffix) throws IOException {
        if (file.exists() && file.isDirectory()) {
            File[] fc = file.listFiles();
            String path;
            URL src;
            for (int i = 0; i < fc.length; i++) {
                path = fc[i].getAbsolutePath();
                if (fc[i].isDirectory()) {
                    searchDir(result, fc[i], suffix);
                } else if (path.endsWith(suffix)) {
                    result.add(fc[i].toURL());
                }
            }
        }
    }

    /** For URLs to JARs that do not use JarURLConnection - allowed by
     * the servlet spec - attempt to produce a JarFile object all the same.
     * Known servlet engines that function like this include Weblogic
     * and OC4J.
     * This is not a full solution, since an unpacked WAR or EAR will not
     * have JAR "files" as such.
	 */
    private static JarFile getAlternativeJarFile(URL url) throws IOException {
        String urlFile = url.getFile();
        int separatorIndex = urlFile.indexOf("!/");
        if (separatorIndex == -1) {
            separatorIndex = urlFile.indexOf('!');
        }
        if (separatorIndex != -1) {
            String jarFileUrl = urlFile.substring(0, separatorIndex);
            if (jarFileUrl.startsWith("file:")) {
                jarFileUrl = jarFileUrl.substring("file:".length());
            }
            return new JarFile(jarFileUrl);
        }
        return null;
    }

    private static void searchJar(ClassLoader cl, Set result, JarFile file, String prefix, String suffix) throws IOException {
        Enumeration e = file.entries();
        JarEntry entry;
        String name;
        while (e.hasMoreElements()) {
            try {
                entry = (JarEntry) e.nextElement();
            } catch (Throwable t) {
                continue;
            }
            name = entry.getName();
            if (name.startsWith(prefix) && name.endsWith(suffix)) {
                Enumeration e2 = cl.getResources(name);
                while (e2.hasMoreElements()) {
                    result.add(e2.nextElement());
                }
            }
        }
    }
}
