package fi.arcusys.acj.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;

/**
 * Simple I/O utilities.
 * 
 * @version ${project.version} $Rev: 1574 $
 * @author mikko Copyright © 2008 Arcusys Ltd. - http://www.arcusys.fi/
 * 
 */
public class IOUtil {

    private static final Logger LOG = LoggerFactoryUtil.getLoggerIfAvailable(IOUtil.class);

    /**
     * A worker for shutdown hook that deleted all the created temporary
     * directories.
     */
    static class TempDirDeleter extends Thread {

        private static Vector<File> tempDirsToDelete = new Vector<File>();

        private static boolean shutDownHookAdded = false;

        /** Delete recursively directories in 'tempDirsToDelete'. */
        @Override
        public void run() {
            synchronized (tempDirsToDelete) {
                for (File dir : tempDirsToDelete) {
                    try {
                        deleteRecursive(dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /** No direct instantiation, please. */
    protected IOUtil() {
    }

    /**
     * Close a <em>closeable</em> object silently, i.e. swallow all the
     * exceptions thrown from the {@link Closeable#close()} method. Exceptions
     * are logged with a <em>warn</em> category.
     * 
     * <p>
     * This method supports objects of following classes:
     * </p>
     * <ul>
     * <li><code>java.io.Closeable</code></li>
     * <li><code>java.net.Socket</code></li>
     * <li><code>javax.xml.stream.XMLStreamReader</code></li>
     * <li><code>javax.xml.stream.XMLStreamWriter</code></li>
     * <li>Any object with a public method <code>close()</code></li>
     * </ul>
     * 
     * @param obj
     *            the object to close
     */
    public static void closeSilently(Object obj) {
        LOG.trace("Closing silently: {}", obj);
        if (null == obj) {
            LOG.trace("Object is null; ignoring");
        } else {
            try {
                if (obj instanceof Closeable) {
                    ((Closeable) obj).close();
                } else if (obj instanceof Socket) {
                    ((Socket) obj).close();
                } else if (obj instanceof XMLStreamReader) {
                    ((XMLStreamReader) obj).close();
                } else if (obj instanceof XMLStreamWriter) {
                    ((XMLStreamWriter) obj).close();
                } else if (!reflectionClose(obj)) {
                    throw new IllegalArgumentException("Don't know how to close an object of class " + obj.getClass().getName());
                }
            } catch (Throwable ex) {
                LOG.warn("An exception occurred while closing object: " + obj, ex);
            }
        }
    }

    /**
     * Try to invoke "close" method by reflection.
     * 
     * <p>This method tries to invoke an accessible "close" without parameters.
     * If there's no such method, an error is logged but no exception is
     * thrown.</p>
     * @param obj the target object to invoke
     * @return {@code true} if successfully invoked the method
     * @throws Throwable if invocation failed
     */
    static boolean reflectionClose(Object obj) throws Throwable {
        boolean closed;
        Class<?> clazz = obj.getClass();
        Method m = null;
        try {
            m = clazz.getMethod("close", new Class<?>[0]);
            m.invoke(obj);
            closed = true;
        } catch (NoSuchMethodException ex) {
            LOG.error("Class " + clazz.getName() + " doesn't have method close()");
            closed = false;
        } catch (IllegalAccessException ex) {
            LOG.error("Method " + m + " is not accessible");
            closed = false;
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
        return closed;
    }

    /**
     * Copy contents of a {@link Reader} to a {@link Writer}.
     * 
     * @param reader
     *            the <code>Reader</code>
     * @param writer
     *            the <code>Writer</code>
     * @throws IOException if an I/O exception occurred
     */
    public static void copy(Reader reader, Writer writer) throws IOException {
        if (null == reader) {
            throw new NullPointerException("reader is null");
        }
        if (null == writer) {
            throw new NullPointerException("writer is null");
        }
        Reader rdr = new BufferedReader(reader);
        Writer w = new BufferedWriter(writer);
        final int bufSize = 1024;
        final char[] buf = new char[bufSize];
        int r;
        do {
            r = rdr.read(buf);
            if (r > 0) {
                w.write(buf, 0, r);
            }
        } while (r >= 0);
        w.flush();
    }

    /**
     * Copy contents of an {@link InputStream} to an {@link OutputStream}.
     * 
     * @param in
     *            the <code>InputStream</code>
     * @param out
     *            the <code>OutputStream</code>
     * @throws IOException if an I/O exception occurred
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        if (null == in) {
            throw new NullPointerException("in is null");
        }
        if (null == out) {
            throw new NullPointerException("out is null");
        }
        BufferedInputStream bis = new BufferedInputStream(in);
        BufferedOutputStream bos = new BufferedOutputStream(out);
        final int bufSize = 1024;
        final byte[] buf = new byte[bufSize];
        int r;
        do {
            r = bis.read(buf);
            if (r > 0) {
                bos.write(buf, 0, r);
            }
        } while (r >= 0);
        bos.flush();
    }

    /**
     * A utility method to load contents of a classpath resource and write them
     * to a <code>Writer</code> object.
     * 
     * @param clazz
     *            a class whose class loader to use
     * @param name
     *            name of the resource
     * @param writer
     *            a <code>Writer</code> object to append contents of the
     *            resource to
     * @param charset
     *            name of the character set (encoding) of the resource contents
     *            or <code>null</code> to use the system default
     * @throws IOException
     *             if an IO exception occurs
     */
    public static void loadResourceContents(Class<?> clazz, String name, Writer writer, String charset) throws IOException {
        if (null == clazz) {
            throw new NullPointerException("clazz is null");
        }
        if (null == name) {
            throw new NullPointerException("name is null");
        }
        if (null == writer) {
            throw new NullPointerException("writer is null");
        }
        InputStream in = clazz.getResourceAsStream(name);
        if (null == in) {
            throw new IOException("No such resource found: " + name);
        }
        InputStreamReader isr;
        isr = (null == charset) ? new InputStreamReader(in) : new InputStreamReader(in, charset);
        try {
            copy(isr, writer);
        } finally {
            closeSilently(isr);
        }
    }

    /**
     * A utility method to load contents of a classpath resource and write them
     * to an <code>OutputStream</code> object.
     * 
     * @param clazz
     *            a class whose class loader to use
     * @param name
     *            name of the resource
     * @param out
     *            an <code>OutputStream</code> object to append contents of the
     *            resource to
     * @throws IOException
     *             if an IO exception occurs
     */
    public static void loadResourceContents(Class<?> clazz, String name, OutputStream out) throws IOException {
        if (null == clazz) {
            throw new NullPointerException("clazz is null");
        }
        if (null == name) {
            throw new NullPointerException("name is null");
        }
        if (null == out) {
            throw new NullPointerException("out is null");
        }
        InputStream in = clazz.getResourceAsStream(name);
        if (null == in) {
            throw new IOException("No such resource found: " + name);
        }
        try {
            copy(in, out);
        } finally {
            closeSilently(in);
        }
    }

    /**
     * A utility method to load contents of a classpath resource to a
     * <code>String</code>.
     * 
     * @param clazz
     *            a class whose class loader to use
     * @param name
     *            name of the resource
     * @param charset
     *            name of the character set (encoding) of the resource contents
     *            or <code>null</code> to use the system default
     * @return a String containing contents of the resource
     * @throws IOException
     *             if an IO exception occurs
     */
    public static String loadResourceContents(Class<?> clazz, String name, String charset) throws IOException {
        if (null == clazz) {
            throw new NullPointerException("clazz is null");
        }
        if (null == name) {
            throw new NullPointerException("name is null");
        }
        StringWriter writer = new StringWriter();
        try {
            loadResourceContents(clazz, name, writer, charset);
            return writer.toString();
        } finally {
            closeSilently(writer);
        }
    }

    /**
     * A utility method to load contents of a classpath resource to a
     * <code>String</code>.
     * 
     * <p>
     * This version uses the system default character set.
     * </p>
     * 
     * @param clazz
     *            a class whose class loader to use
     * @param name
     *            name of the resource
     * @return a String containing contents of the resource
     * 
     * @throws IOException
     *             if an IO exception occurs
     * 
     * @see #loadResourceContents(Class, String, String)
     */
    public static String loadResourceContents(Class<?> clazz, String name) throws IOException {
        return loadResourceContents(clazz, name, (String) null);
    }

    /**
     * Load contents of an {@link InputStream} to a string.
     * @param in the input stream
     * @param charset character set to use
     * @return a string constructed from the input stream
     * @throws IOException if an I/O error occurred while reading the stream
     */
    public static String loadToString(InputStream in, String charset) throws IOException {
        Writer w = new StringWriter();
        InputStreamReader isr = (null == charset) ? new InputStreamReader(in) : new InputStreamReader(in, charset);
        copy(isr, w);
        return w.toString();
    }

    /**
     * Load contents of a {@link Reader} to a string.
     * @param r the reader
     * @return a string constructed from the reader
     * @throws IOException if an I/O error occurred while reading the stream
     */
    public static String loadToString(Reader r) throws IOException {
        Writer w = new StringWriter();
        copy(r, w);
        return w.toString();
    }

    /**
     * Load contents of a file to a string.
     * @param f the file
     * @param charset character set to use
     * @return a string containing contents of the file
     * @throws IOException if an I/O error occurred while reading the stream
     */
    public static String loadToString(File f, String charset) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        try {
            return loadToString(is, charset);
        } finally {
            closeSilently(is);
        }
    }

    /**
     * Create a temporary directory.
     * 
     * <p>
     * The directory will be deleted when JVM exits.</>
     * 
     * @return a <code>File</code> representing the created directory
     * @throws IOException if an I/O exception occurred
     */
    public static File createTempDirectory() throws IOException {
        return createTempDirectory(true);
    }

    /**
     * Create a temporary directory.
     * 
     * @param deleteOnExit
     *            The directory will be deleted when JVM exits.
     * @return a <code>File</code> representing the created directory
     * @throws IOException if an I/O exception occurred
     */
    public static File createTempDirectory(boolean deleteOnExit) throws IOException {
        return doCreateTempDirectory("acj", null, null, deleteOnExit);
    }

    /**
     * Create a temporary directory using the given suffix and prefix to
     * generate name of the directory.
     * 
     * <p>
     * The directory will be deleted when JVM exits.</>
     * 
     * @param prefix
     *            The prefix string to be used in generating the directory name;
     *            must be at least three characters long
     * @param suffix
     *            The suffix string for the directory name. <code>null</code> is
     *            equal to an empty string (no suffix at all)
     * 
     * @return a <code>File</code> object for the newly created (temporary)
     *         directory
     * 
     * @throws IllegalArgumentException
     *             if any of the arguments is invalid
     * @throws IOException
     *             if an IO exception occurrs (i.e. the directory could not be
     *             created)
     * @throws SecurityException
     * 
     * @see {@link File#createTempFile(String, String)}
     */
    public static File createTempDirectory(String prefix, String suffix) throws IOException {
        return createTempDirectory(prefix, suffix, true);
    }

    /**
     * Create a temporary directory using the given suffix and prefix to
     * generate name of the directory.
     * 
     * @param prefix
     *            The prefix string to be used in generating the directory name;
     *            must be at least three characters long
     * @param suffix
     *            The suffix string for the directory name. <code>null</code> is
     *            equal to an empty string (no suffix at all)
     * @param deleteOnExit
     *            The directory will be deleted when JVM exits.
     * 
     * @return a <code>File</code> object for the newly created (temporary)
     *         directory
     * 
     * @throws IllegalArgumentException
     *             if any of the arguments is invalid
     * @throws IOException
     *             if an IO exception occurrs (i.e. the directory could not be
     *             created)
     * @throws SecurityException
     * 
     * @see {@link File#createTempFile(String, String)}
     */
    public static File createTempDirectory(String prefix, String suffix, boolean deleteOnExit) throws IOException {
        return doCreateTempDirectory(prefix, suffix, null, deleteOnExit);
    }

    /**
     * Create a temporary directory using the given suffix and prefix to
     * generate name of the directory.
     * 
     * <p>
     * The created directory will be deleted on JVM exit.
     * </p>
     * 
     * @param prefix
     *            The prefix string to be used in generating the directory name;
     *            must be at least three characters long
     * @param suffix
     *            The suffix string for the directory name. <code>null</code> is
     *            equal to an empty string (no suffix at all)
     * @param directory
     *            the parent directory for the new temporary directory
     * 
     * @return a <code>File</code> object for the newly created (temporary)
     *         directory
     * 
     * @throws IllegalArgumentException
     *             if any of the arguments is invalid
     * @throws IOException
     *             if an IO exception occurs (i.e. the directory could not be
     *             created)
     * @throws SecurityException
     * 
     * @see {@link File#createTempFile(String, String, File)}
     */
    public static File createTempDirectory(String prefix, String suffix, File directory) throws IOException {
        return createTempDirectory(prefix, suffix, directory, true);
    }

    /**
     * Create a temporary directory using the given suffix and prefix to
     * generate name of the directory.
     * 
     * <p>
     * </p>
     * 
     * @param prefix
     *            The prefix string to be used in generating the directory name;
     *            must be at least three characters long
     * @param suffix
     *            The suffix string for the directory name. <code>null</code> is
     *            equal to an empty string (no suffix at all)
     * @param directory
     *            the parent directory for the new temporary directory
     * 
     * @param deleteOnExit
     *            the created directory will be deleted on JVM exit.
     * 
     * @return a <code>File</code> object for the newly created (temporary)
     *         directory
     * 
     * @throws IllegalArgumentException
     *             if any of the arguments is invalid
     * @throws IOException
     *             if an IO exception occurs (i.e. the directory could not be
     *             created)
     * @throws SecurityException
     * 
     * @see {@link File#createTempFile(String, String, File)}
     */
    public static File createTempDirectory(String prefix, String suffix, File directory, boolean deleteOnExit) throws IOException {
        return doCreateTempDirectory(prefix, suffix, directory, deleteOnExit);
    }

    /**
     * Actually create a temporary directory.
     * 
     * @param prefix prefix for the directory name
     * @param suffix suffix for the directory name
     * @param directory the base directory or null to use default
     * @param deleteOnExit indicates if the created directory should be deleted
     *        on JVM exit
     * @return the create directory
     * @throws IOException if an I/O error occurs
     */
    static File doCreateTempDirectory(String prefix, String suffix, File directory, boolean deleteOnExit) throws IOException {
        File f;
        if (null == directory) {
            f = File.createTempFile(prefix, suffix);
        } else {
            f = File.createTempFile(prefix, suffix, directory);
        }
        if (!f.delete()) {
            throw new IOException("Could not delete the intermediate temporary file: " + f);
        }
        if (!f.mkdir()) {
            throw new IOException("Could not create a directory: " + f);
        }
        if (deleteOnExit) {
            synchronized (TempDirDeleter.tempDirsToDelete) {
                if (!TempDirDeleter.shutDownHookAdded) {
                    Runtime.getRuntime().addShutdownHook(new TempDirDeleter());
                    TempDirDeleter.shutDownHookAdded = true;
                }
                TempDirDeleter.tempDirsToDelete.add(f);
            }
        }
        return f;
    }

    /**
     * Flush an object, i.e. call its <code>flush</code> method.
     * 
     * <p>
     * If the provide object is an instance of {@link Flushable}, its method
     * {@link Flushable#flush()} is invoked. Otherwise this method tries to
     * invoke a public method <code>flush()</code> by reflection.
     * </p>
     * 
     * @param o
     *            the object (if null, this method does nothing)
     * @throws IOException if an I/O exception occurred
     * @throws IllegalArgumentException
     *             if the object is not flushable
     */
    public static void flush(Object o) throws IOException {
        if (null != o) {
            if (!flushIfPossible(o)) {
                throw new IllegalArgumentException("Not Flushable: " + o.getClass());
            }
        }
    }

    /**
     * Flush an object, i.e. call its <code>flush</code> method.
     * 
     * <p>
     * If the provide object is an instance of {@link Flushable}, its method
     * {@link Flushable#flush()} is invoked. Otherwise this method tries to
     * invoke a public method <code>flush()</code> by reflection.
     * </p>
     * 
     * @param o
     *            the object
     * @return <code>true</code> if flush was possible
     * @throws IOException if an I/O exception occurred
     */
    public static boolean flushIfPossible(Object o) throws IOException {
        boolean possible;
        if (null == o) {
            possible = false;
        } else if (o instanceof Flushable) {
            ((Flushable) o).flush();
            possible = true;
        } else {
            try {
                Method m = o.getClass().getMethod("flush");
                m.invoke(o);
                possible = true;
            } catch (NoSuchMethodException e) {
                return false;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException(cause);
                }
            } catch (IllegalAccessException e) {
                possible = false;
            }
        }
        return possible;
    }

    /**
     * Find a free port that can be bound on local host.
     * 
     * @return first available free port
     * @throws IOException if an I/O exception occurred
     * @since 0.9
     */
    public static int findFreePort() throws IOException {
        ServerSocket ss = new ServerSocket(0);
        try {
            return ss.getLocalPort();
        } finally {
            ss.close();
        }
    }

    /**
     * "Touch" a file.
     * 
     * <p>
     * If the file doesn't exist, it is created. If the file exists, it is
     * touched by appending zero bytes to it which should update its mtime.
     * </p>
     * @param f the file to touch
     * @return boolean if a new file was created
     * @throws IOException if an I/O exception occurred
     */
    public static boolean touch(File f) throws IOException {
        boolean created = !f.exists();
        FileOutputStream fos = new FileOutputStream(f, true);
        fos.flush();
        fos.close();
        return created;
    }

    /**
     * Delete a directory or file. 
     * 
     * <p>In case of directories, this method deletes contents of the
     * directory first and then the directory itself.</p>
     * 
     * @param f the file (or directory)
     * @return <code>true</code> if the file or directory was actually deleted.
     * 
     * @throws IOException
     */
    public static boolean deleteRecursive(File f) throws IOException {
        boolean deleted;
        if (!f.exists()) {
            deleted = false;
        } else if (f.isFile()) {
            deleted = f.delete();
        } else {
            File[] children = f.listFiles();
            for (File child : children) {
                deleteRecursive(child);
            }
            deleted = f.delete();
        }
        return deleted;
    }
}
