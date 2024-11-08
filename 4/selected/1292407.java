package org.openconcerto.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;

/**
 * This class doubles up as a simple binary file "encryptor" and a custom ClassLoader that will
 * reverse the encryption during actual class loading.
 * <P>
 * 
 * Usage:
 * 
 * <PRE>
 * 
 * java -encrypt <full class name1> <full class name2> ... java -encryptJar inJar outJar [exclusion
 * ...] java -run <classpath dir> <main class> ...app args...
 * 
 * </PRE>
 * 
 * 
 * where the directory that follows both <code>-encrypt</code> and <code>-run</code> should be
 * the one into which you have compiled the original classes.
 * 
 * @author (C) <a href="http://www.javaworld.com/columns/jw-qna-index.shtml">Vlad Roubtsov </a>,
 *         2003
 */
public class EncryptedClassLoader extends URLClassLoader {

    public static final String USAGE = "usage: EncryptedClassLoader " + "(" + "-run <encrypt dir> <app_main_class> <app_main_args...>" + " | " + "-encrypt <class 1> <class 2> ..." + ")";

    public static final boolean TRACE = false;

    public static void main(final String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException(USAGE);
        if ("-run".equals(args[0]) && (args.length >= 3)) {
            final ClassLoader appLoader = new EncryptedClassLoader(EncryptedClassLoader.class.getClassLoader(), new File(args[1]));
            Thread.currentThread().setContextClassLoader(appLoader);
            final Class app = appLoader.loadClass(args[2]);
            if (TRACE) System.out.println(app + " loaded");
            final Method appmain = app.getMethod("main", new Class[] { String[].class });
            if (TRACE) System.out.println("Will call " + appmain);
            final String[] appargs = new String[args.length - 3];
            System.arraycopy(args, 3, appargs, 0, appargs.length);
            appmain.invoke(null, new Object[] { appargs });
        } else if ("-encrypt".equals(args[0]) && (args.length >= 2)) {
            for (int f = 1; f < args.length; ++f) {
                final String item = args[f];
                final File srcFile = new File(item);
                encrypt(new FileInputStream(srcFile), srcFile);
                if (TRACE) System.out.println("encrypted [" + srcFile + "]");
            }
        } else if ("-encryptJar".equals(args[0]) && (args.length >= 3)) {
            final File srcJar = new File(args[1]);
            final File destJar = new File(args[2]);
            final Set excluded = new HashSet(args.length - 3);
            for (int i = 3; i < args.length; i++) {
                String excl = args[i];
                excluded.add(excl);
            }
            final Zip z = new Zip(destJar);
            final Unzip unz = new Unzip(srcJar);
            final Enumeration iter = unz.entries();
            while (iter.hasMoreElements()) {
                final ZipEntry entry = (ZipEntry) iter.nextElement();
                final InputStream in;
                if (entry.getName().endsWith(".class") && !excluded.contains(entry.getName())) {
                    in = new ByteArrayInputStream(encrypt(unz.getInputStream(entry)));
                } else {
                    in = unz.getInputStream(entry);
                }
                z.zip(entry.getName(), in);
            }
            unz.close();
            z.close();
            if (TRACE) System.out.println("encrypted [" + srcJar + "] into " + destJar);
        } else throw new IllegalArgumentException(USAGE);
    }

    private static byte[] encrypt(InputStream in) throws IOException {
        final byte[] classBytes;
        try {
            classBytes = readFully(in);
            crypt(classBytes);
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        return classBytes;
    }

    private static void encrypt(InputStream in, File outFile) throws IOException {
        final byte[] classBytes = encrypt(in);
        final OutputStream out = new FileOutputStream(outFile);
        try {
            out.write(classBytes);
        } finally {
            if (out != null) try {
                out.close();
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public Class loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        if (TRACE) System.out.println("loadClass (" + name + ", " + resolve + ")");
        Class c = null;
        c = findLoadedClass(name);
        if (c == null) {
            Class parentsVersion = null;
            try {
                parentsVersion = getParent().loadClass(name);
                if (parentsVersion.getClassLoader() != getParent()) c = parentsVersion;
            } catch (ClassNotFoundException ignore) {
            } catch (ClassFormatError ignore) {
            }
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ignore) {
                    c = parentsVersion;
                }
            }
        }
        if (c == null) throw new ClassNotFoundException(name);
        if (resolve) resolveClass(c);
        return c;
    }

    protected Class findClass(final String name) throws ClassNotFoundException {
        if (TRACE) System.out.println("findClass (" + name + ")");
        final String classResource = name.replace('.', '/') + ".class";
        final URL classURL = getResource(classResource);
        if (classURL == null) throw new ClassNotFoundException(name); else {
            InputStream in = null;
            try {
                in = classURL.openStream();
                final byte[] classBytes = readFully(in);
                decrypt(classBytes);
                if (TRACE) System.out.println("decrypted [" + name + "]");
                return defineClass(name, classBytes, 0, classBytes.length);
            } catch (IOException ioe) {
                throw new ClassNotFoundException(name);
            } finally {
                if (in != null) try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private EncryptedClassLoader(final ClassLoader parent, final File classpath) throws MalformedURLException {
        super(new URL[] { classpath.toURL() }, parent);
        if (parent == null) throw new IllegalArgumentException("EncryptedClassLoader" + " requires a non-null delegation parent");
    }

    private static void crypt(final byte[] data) {
        for (int i = 8; i < data.length; ++i) {
            data[i] ^= 0x5A;
        }
    }

    private static void decrypt(final byte[] data) {
        crypt(data);
    }

    private static byte[] readFully(final InputStream in) throws IOException {
        final ByteArrayOutputStream buf1 = new ByteArrayOutputStream();
        final byte[] buf2 = new byte[8 * 1024];
        for (int read; (read = in.read(buf2)) > 0; ) {
            buf1.write(buf2, 0, read);
        }
        return buf1.toByteArray();
    }
}
