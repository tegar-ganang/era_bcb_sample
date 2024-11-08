package org.sqlexp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Static class jar dynamic load utils.
 * @author Matthieu Réjou
 */
public final class JarUtils {

    /** */
    private JarUtils() {
    }

    ;

    /**
	 * Loads a jar file without copying it.
	 * @param file to copy
	 * @throws ClassNotFoundException if an error occurred while loading jar content
	 */
    public static void load(final String file) throws ClassNotFoundException {
        if (file != null) {
            load(new File(file));
        }
    }

    /**
	 * Loads a jar file without copying it.
	 * @param file to copy
	 * @throws ClassNotFoundException if an error occurred while loading jar content
	 */
    public static void load(final File file) throws ClassNotFoundException {
        try {
            load(file, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Load a jar file copying it to "lib/" directory.
	 * @param file to copy
	 * @throws FileNotFoundException if file is null or does not exists
	 * @throws IOException if any IO error occurred
	 * @throws ClassNotFoundException if an error occurred while loading jar content
	 */
    public static void loadWithCopy(final String file) throws FileNotFoundException, IOException, ClassNotFoundException {
        if (file != null) {
            loadWithCopy(new File(file));
        }
    }

    /**
	 * Load a jar file copying it to "lib/" directory.
	 * @param file to copy
	 * @throws FileNotFoundException if file is null or does not exists
	 * @throws IOException if any IO error occurred
	 * @throws ClassNotFoundException if an error occurred while loading jar content
	 */
    public static void loadWithCopy(final File file) throws FileNotFoundException, IOException, ClassNotFoundException {
        load(file, true);
    }

    /**
	 * Load a jar file copying it to "lib/" directory.
	 * @param file to copy
	 * @param copyToLib true if file have to be copied to "/lib" directory
	 * @throws FileNotFoundException if file is null or does not exists
	 *             (may happen if copyToLib is true)
	 * @throws IOException if any IO error occurred
	 *             (may happen if copyToLib is true)
	 * @throws ClassNotFoundException if an error occurred while loading jar content
	 */
    private static void load(final File file, final boolean copyToLib) throws FileNotFoundException, IOException, ClassNotFoundException {
        URL url = toJarURL(file);
        if (url == null) {
            return;
        }
        if (copyToLib) {
            copyToLibDirectory(file);
        }
        if (isLoaded(file)) {
            return;
        }
        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysLoader, new Object[] { url });
        } catch (Exception e) {
            throw new ClassNotFoundException(file.getAbsolutePath(), e);
        }
    }

    /**
	 * Copy a file to "lib/" directory.<br>
	 * @param file to copy
	 * @return new created file, or file itself if is is already contained in "lib/" directory
	 * @throws FileNotFoundException if file is null or does not exists
	 * @throws IOException if any IO error occurred
	 */
    public static File copyToLibDirectory(final File file) throws FileNotFoundException, IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException();
        }
        File directory = new File("lib/");
        File dest = new File(directory, file.getName());
        File parent = dest.getParentFile();
        while (parent != null && !parent.equals(directory)) {
            parent = parent.getParentFile();
        }
        if (parent.equals(directory)) {
            return file;
        }
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(file).getChannel();
            out = new FileOutputStream(dest).getChannel();
            in.transferTo(0, in.size(), out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return dest;
    }

    /**
	 * Determine if a jar file is already loaded.
	 * @param file pointing at a jar file
	 * @return true if the jar file is already loaded, or null if file does not represent a jar file
	 */
    public static boolean isLoaded(final File file) {
        URL url = toJarURL(file);
        if (url == null) {
            return false;
        }
        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL[] urls = sysLoader.getURLs();
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].toString() != null && urls[i].toString().equalsIgnoreCase(url.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Determines if a file represent a jar file (by name).
	 * @param file to test
	 * @return true if is a jar file, false if not or <code>file</code> is null
	 */
    public static boolean isJarFile(final File file) {
        return file != null && file.getName().matches(".*\\.[Jj][Aa][Rr]");
    }

    /**
	 * Compute an URL pointing at a jar file.
	 * @param file pointing at a file
	 * @return URL pointing at a jar file, or null if file does not represent a jar file
	 */
    private static URL toJarURL(final File file) {
        if (!isJarFile(file)) {
            return null;
        }
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Gets the classes contained in a jar file (without adding it to classpath).
	 * @param file to inspect
	 * @return class list
	 * @throws IOException if jar file could not be read
	 */
    public static String[] getClasses(final File file) throws IOException {
        ArrayList<String> classes = new ArrayList<String>();
        JarFile jar = new JarFile(file);
        Enumeration<JarEntry> entries = jar.entries();
        JarEntry entry;
        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            String name = entry.getName();
            if (name != null && name.endsWith(".class") && !name.contains("$")) {
                name = name.substring(0, name.length() - ".class".length());
                name = name.replaceAll("[/\\\\]", ".");
                classes.add(name);
            }
        }
        return classes.toArray(new String[classes.size()]);
    }
}
