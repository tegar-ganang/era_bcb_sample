package com.ibm.hannover.development.tools.configurations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * The method loadLibrary load a native library in the order 
 * from java.library.path, os.library.path, tmpdir and from jar.
 *  
 *  @author  pan
  */
public class Library {

    static final String SEPARATOR = File.separator;

    public static void loadLibrary(String name) {
        String path = System.getProperty("os.library.path");
        if (path != null) {
            path = new File(path).getAbsolutePath();
            if (_load(System.mapLibraryName(path + SEPARATOR + name))) return;
        }
        if (_load(name)) return;
        if (path == null) {
            path = System.getProperty("java.io.tmpdir");
            if (path != null) {
                path = new File(path).getAbsolutePath();
                if (_load(System.mapLibraryName(path + SEPARATOR + name))) return;
            }
        }
        if (path != null && loadFromJar(System.mapLibraryName(path + SEPARATOR + name), System.mapLibraryName(name))) return;
        throw new UnsatisfiedLinkError(" no  " + name + "  in java.libary.path, os.library.path or jar file ");
    }

    private static boolean _load(String libName) {
        try {
            if (libName.indexOf(SEPARATOR) != -1) {
                System.load(libName);
            } else {
                System.loadLibrary(libName);
            }
            return true;
        } catch (UnsatisfiedLinkError e) {
        }
        return false;
    }

    private static boolean loadFromJar(String outFileName, String libName) {
        try {
            return extractAndLoad(outFileName, libName);
        } catch (Throwable e) {
        }
        return false;
    }

    private static boolean extractAndLoad(String outFileName, String libName) throws Throwable {
        int read;
        byte[] buf = new byte[4096];
        InputStream is = null;
        FileOutputStream os = null;
        File file = new File(outFileName);
        if (file.exists()) file.delete();
        if (!file.exists()) {
            try {
                os = new FileOutputStream(file);
                is = Library.class.getResourceAsStream(" / " + libName);
                if (is == null || os == null) return false;
                while ((read = is.read(buf)) != -1) os.write(buf, 0, read);
            } finally {
                if (os != null) os.close();
                if (is != null) is.close();
            }
            if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                Runtime.getRuntime().exec(new String[] { " chmod ", " 755 ", outFileName }).waitFor();
            }
            return _load(outFileName);
        }
        return false;
    }
}
