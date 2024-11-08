package org.sd_network.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is JAR file utilities.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class JarUtil {

    /** Logging category */
    private static final Logger _log = Logger.getLogger(JarUtil.class.getName());

    /** Maximum of targets that can pack to jar file. */
    private static final long _MAX_TARGETS = 100;

    /** A Comparator for alphabetical sort jar file. */
    public static final Comparator<String> _SORT_BY_PATH = new Comparator<String>() {

        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    };

    /**
     * Return List collection of JAR file pathes that is defined by
     * system property "java.class.path".
     */
    public static List<String> getJarFiles() {
        String path = System.getProperty("java.class.path");
        String separator = System.getProperty("path.separator");
        List<String> pathList = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(path, separator);
        while (st.hasMoreTokens()) {
            pathList.add(st.nextToken());
        }
        Collections.sort(pathList, _SORT_BY_PATH);
        return pathList;
    }

    /**
     * Return JAR file name included in class path.
     * If speicified JAR file name is not found, return null.
     *
     * <p> In "specJar" argument, you can specify completely jar file. And 
     * also you can specify only prefix of jar file name. However,
     * in this case, you receive jar file name that get first.
     * For example, when class path include "test.jar", "total.jar" and
     * "tiger.jar", and if specify only "t" to "specJar" argument,
     * you can get "test.jar".
     *
     * @parm specJar    completely jar file name or prefix of it.
     */
    public static String getJarFileMatchTo(String specJar) {
        List<String> jarList = getJarFiles();
        Iterator<String> jars = jarList.iterator();
        while (jars.hasNext()) {
            String jarFile = jars.next();
            if (jarFile.indexOf(specJar) != -1) return jarFile;
        }
        return null;
    }

    /**
     * Generate each Class object of classes included in the JAR file
     * specified in "jarFile", return it as a List collection.
     *
     * <P> In a JAR file, There is possibility JAR file is included in
     * various file other than JAR file. If processed other than class file,
     * occured exceptions, but in this method, ignore that exception as
     * processed not class file.
     *
     * @param	jarFile Path JAR file.
     *
     * @return	List collection of Class objects.
     *
     * @throws	FileNotFoundException
     *          Throw if specified JAR file was not found.
     * @throws	IllegaStateException
     *          Throw if read error occured.
     */
    public static List<Class<?>> getClasses(String jarFile) throws FileNotFoundException {
        ClassLoader loader = JarUtil.class.getClassLoader();
        List<Class<?>> classList = new ArrayList<Class<?>>();
        try {
            JarInputStream jarInS = new JarInputStream(new FileInputStream(jarFile));
            JarEntry entry = jarInS.getNextJarEntry();
            while ((entry = jarInS.getNextJarEntry()) != null) {
                if (entry.isDirectory()) continue;
                String classFile = entry.getName();
                int classExtIdx = classFile.lastIndexOf(".class");
                if (classExtIdx < 0) continue;
                classFile = classFile.substring(0, classExtIdx);
                StringTokenizer st = new StringTokenizer(classFile, "/");
                String className = "";
                while (st.hasMoreTokens()) {
                    className += (String) st.nextToken() + ".";
                }
                className = className.substring(0, className.length() - 1);
                try {
                    Class<?> classObj = Class.forName(className, false, loader);
                    classList.add(classObj);
                } catch (Throwable e) {
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("I/O error when read jar. : " + e.getMessage());
        }
        return classList;
    }

    /**
     * Create JAR file.
     *
     * <P> Register each files or directories specified by <tt>targets</tt>
     * to JAR file. If <tt>targets</tt> is included directory, search
     * file/directory to bottom layer recursively, register all
     * files/directories.
     *
     * @param targets   A Path name of file or directory register to JAR file.
     *
     * @return  A File object of JAR file. Create as temporary file, you should
     *          rename to appropriate name.
     *
     * @throws  IOException
     *          Throw if file I/O failed.
     */
    public static File pack(String[] targets) throws IOException {
        if (targets == null) throw new IllegalArgumentException("targets is null.");
        if (targets.length > _MAX_TARGETS) throw new IllegalArgumentException("number of targets over than MAX.");
        File temp = File.createTempFile("temp", ".jar");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(temp));
        for (int idx = 0; idx < targets.length; idx++) {
            File root = new File(targets[idx]);
            if (!root.exists()) {
                _log.log(Level.INFO, "target[" + targets[idx] + "] was not exists.");
                continue;
            }
            addEntries(jos, root);
            jos.close();
        }
        return temp;
    }

    /**
     * Register file or directory specified as <tt>target</tt>
     * to JAR file as JarEntry.
     *
     * @param jos       Output stream of JAR file.
     * @param target    The File object of file or directory.
     *
     * @throws  IOException
     *          Throw if file IO failed.
     */
    private static void addEntries(JarOutputStream jos, File target) throws IOException {
        if (jos == null) throw new IllegalArgumentException("jos is null.");
        if (target == null) throw new IllegalArgumentException("target is null.");
        if (target.isFile()) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(target));
            addFileEntry(jos, bis, target.getPath());
            bis.close();
        } else {
            addDirectoryEntry(jos, target.getPath());
            String path = target.getPath();
            String[] subTargets = target.list();
            for (int idx = 0; idx < subTargets.length; idx++) {
                addEntries(jos, new File(path + "/" + subTargets[idx]));
            }
        }
    }

    /**
     * Add file entry to JAR file.
     */
    private static void addFileEntry(JarOutputStream jos, InputStream is, String entryName) throws IOException {
        if (jos == null) throw new IllegalArgumentException("OutputStream is null.");
        if (is == null) throw new IllegalArgumentException("InputStream is null.");
        if (entryName == null || entryName.trim().length() == 0) throw new IllegalArgumentException("entryName is empty.");
        String availablePath = toAvailableJarPath(entryName);
        if (availablePath.length() == 0) return;
        jos.putNextEntry(new JarEntry(toAvailableJarPath(entryName)));
        byte buf[] = new byte[1024];
        int count;
        while ((count = is.read(buf, 0, buf.length)) != -1) jos.write(buf, 0, count);
        jos.closeEntry();
    }

    /**
     * Add directory entry to JAR file.
     */
    private static void addDirectoryEntry(JarOutputStream jos, String dirPath) throws IOException {
        if (jos == null) throw new IllegalArgumentException("jos is null.");
        if (dirPath == null || dirPath.length() == 0) throw new IllegalArgumentException("dirPath is empty.");
        String availablePath = toAvailableJarPath(dirPath);
        if (availablePath.length() == 0) return;
        jos.putNextEntry(new JarEntry(availablePath + "/"));
        jos.closeEntry();
    }

    /**
     * Exchange from <tt>path</tt> to available path at JAR file.
     */
    private static String toAvailableJarPath(String path) {
        if (path == null) throw new IllegalArgumentException("path is null.");
        if (path.equals(".")) return "";
        if (path.startsWith(".\\") || path.startsWith("./")) {
            path = path.substring(2);
            if (path.length() == 0) return path;
        }
        StringTokenizer st = new StringTokenizer(path, "\\", true);
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals("\\")) sb.append("/"); else sb.append(token);
        }
        return sb.toString();
    }
}
