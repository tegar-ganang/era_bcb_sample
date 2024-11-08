package org.trebor.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.URL;
import java.lang.ClassLoader;

/** This class provides some standard tools for loading and executing
 * items from inside a jar file. */
public class JarTools {

    static ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    /** Load a system library from the jar into memory.
       *
       * @param path Path to the library in the jar file (or not for that matter).
       * @param name Base library name. OS specific text will be added
       *        around the name as needed.
       */
    public static void loadLibrary(String path, String name) throws Exception {
        String separator = "/";
        String lib = (path.endsWith(separator) ? path : path + separator) + System.mapLibraryName(name);
        File tmp = File.createTempFile(name + ".", ".lib");
        tmp.deleteOnExit();
        copyResource(lib, tmp);
        System.load(tmp.getPath());
    }

    /** Copy a file in the jar to some location outside the jar.
       *
       * @param source relative path to file in jar
       * @param destination path to copy
       */
    public static void copyResource(String source, String destination) throws Exception {
        copyResource(source, new File(destination));
    }

    /** Copy a file in the jar to some location outside the jar.
       *
       * @param source relative path to file in jar
       * @param destination path to copy
       */
    public static void copyResource(String source, File destination) throws Exception {
        System.out.println("source: " + source);
        InputStream in = getResourceAsStream(source);
        OutputStream out = new FileOutputStream(destination);
        byte[] buffer = new byte[32 * 1024];
        int len;
        while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
        out.close();
    }

    /** Return a stream to a file in the jar.  This is a covience
       * function witch just calls a function by the same name in the
       * class loader.
       *
       * @param file relative path to file in jar
       * @return Stream to specified file.
       */
    public static InputStream getResourceAsStream(String file) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(file);
    }

    /** Return a URL to a file in the jar.  This is a covience
       * function witch just calls a function by the same name in the
       * class loader.
       *
       * @param file relative path to file in jar
       * @return URL to specified file.
       */
    public static URL getResource(String file) {
        return ClassLoader.getSystemClassLoader().getResource(file);
    }

    /** For testing this program */
    public static void main(String[] args) {
        try {
            copyResource("resources/readme.txt", new File("/tmp/readme.txt"));
            loadLibrary("lib", "testLib");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
