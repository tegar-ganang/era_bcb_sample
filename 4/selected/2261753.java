package com.google.testing.instrumentation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * A custom ClassLoader that performs byte-code instrumentation on all loaded
 * classes.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public final class InstrumentedClassLoader extends ClassLoader {

    private static final int BUF_SIZE = 4096;

    private final Instrumenter instrumenter;

    private static final List<String> excludedClassPrefixes = Arrays.asList("java.", "javax.", "sun.", "net.sf.cglib", "junit.", "org.junit.", "org.objenesis.", "org.easymock.", "org.w3c.dom");

    /**
   * Creates a new instrumented class loader using the given {@link
   * Instrumenter}. All classes loaded by this loader will have their byte-code
   * passed through the {@link Instrumenter#instrument} method.
   *
   * @param instrumenter the instrumenter
   */
    public InstrumentedClassLoader(Instrumenter instrumenter) {
        super(InstrumentedClassLoader.class.getClassLoader());
        if (instrumenter == null) {
            throw new IllegalArgumentException("instrumenter cannot be null");
        }
        this.instrumenter = instrumenter;
    }

    /**
   * Returns true if this class should be loaded by this classloader. If not,
   * then loading delegates to the parent.
   */
    private boolean shouldLoad(String className) {
        for (String excluded : excludedClassPrefixes) {
            if (className.startsWith(excluded)) {
                return false;
            }
        }
        return true;
    }

    /**
   * Loads a class, and throws an IllegalArgumentException if the class cannot
   * be loaded. Useful for classes which we expect to be able to find, e.g. for
   * currently loaded classes that are being reloaded by this
   * InstrumentedClassLoader.
   *
   * @param name the full name of the class
   *
   * @return the loaded class
   */
    public Class<?> getExpectedClass(String name) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot find " + e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result = findLoadedClass(name);
        if (result == null) {
            if (shouldLoad(name)) {
                result = findClass(name);
            } else {
                return super.loadClass(name, resolve);
            }
        }
        if (resolve) {
            resolveClass(result);
        }
        return result;
    }

    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
        try {
            int dotpos = className.lastIndexOf('.');
            if (dotpos != -1) {
                String pkgname = className.substring(0, dotpos);
                if (getPackage(pkgname) == null) {
                    definePackage(pkgname, null, null, null, null, null, null, null);
                }
            }
            String resourceName = className.replace('.', '/') + ".class";
            InputStream input = getSystemResourceAsStream(resourceName);
            byte[] classData = instrumenter.instrument(className, loadClassData(input));
            Class<?> result = defineClass(className, classData, 0, classData.length, null);
            return result;
        } catch (IOException e) {
            throw new ClassNotFoundException("Cannot load " + className, e);
        }
    }

    /**
   * Load class data from a given input stream.
   */
    private byte[] loadClassData(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(BUF_SIZE);
        byte[] buffer = new byte[BUF_SIZE];
        int readCount;
        while ((readCount = input.read(buffer, 0, BUF_SIZE)) >= 0) {
            output.write(buffer, 0, readCount);
        }
        return output.toByteArray();
    }
}
