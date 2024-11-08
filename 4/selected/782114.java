package dalvik.system;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.net.MalformedURLException;
import dalvik.system.DexFile;

/**
 * Provides a simple {@link ClassLoader} implementation that operates on a list
 * of files and directories in the local file system, but does not attempt to
 * load classes from the network. Android uses this class for its system class
 * loader and for its application class loader(s).
 *
 * @since Android 1.0
 */
public class PathClassLoader extends ClassLoader {

    private final String path;

    private final String libPath;

    private boolean initialized;

    private String[] mPaths;

    private File[] mFiles;

    private ZipFile[] mZips;

    private DexFile[] mDexs;

    private String[] mLibPaths;

    /**
     * Creates a {@code PathClassLoader} that operates on a given list of files
     * and directories. This method is equivalent to calling
     * {@link #PathClassLoader(String, String, ClassLoader)} with a
     * {@code null} value for the second argument (see description there).
     *
     * @param path
     *            the list of files and directories
     *
     * @param parent
     *            the parent class loader
     */
    public PathClassLoader(String path, ClassLoader parent) {
        this(path, null, parent);
    }

    /**
     * Creates a {@code PathClassLoader} that operates on two given lists of
     * files and directories. The entries of the first list should be one of the
     * following:
     * <ul>
     * <li>Directories containing classes or resources.
     * <li>JAR/ZIP/APK files, possibly containing a "classes.dex" file.
     * <li>"classes.dex" files.
     * </ul>
     * The entries of the second list should be directories containing native
     * library files. Both lists are separated using the character specified by
     * the "path.separator" system property, which, on Android, defaults to ":".
     *
     * @param path
     *            the list of files and directories containing classes and
     *            resources
     *
     * @param libPath
     *            the list of directories containing native libraries
     *
     * @param parent
     *            the parent class loader
     */
    public PathClassLoader(String path, String libPath, ClassLoader parent) {
        super(parent);
        if (path == null) throw new NullPointerException();
        this.path = path;
        this.libPath = libPath;
    }

    private synchronized void ensureInit() {
        if (initialized) {
            return;
        }
        initialized = true;
        mPaths = path.split(":");
        int length = mPaths.length;
        mFiles = new File[length];
        mZips = new ZipFile[length];
        mDexs = new DexFile[length];
        boolean wantDex = System.getProperty("android.vm.dexfile", "").equals("true");
        for (int i = 0; i < length; i++) {
            File pathFile = new File(mPaths[i]);
            mFiles[i] = pathFile;
            if (pathFile.isFile()) {
                try {
                    mZips[i] = new ZipFile(pathFile);
                } catch (IOException ioex) {
                }
                if (wantDex) {
                    try {
                        mDexs[i] = new DexFile(pathFile);
                    } catch (IOException ioex) {
                    }
                }
            }
        }
        String pathList = System.getProperty("java.library.path", ".");
        String pathSep = System.getProperty("path.separator", ":");
        String fileSep = System.getProperty("file.separator", "/");
        if (libPath != null) {
            if (pathList.length() > 0) {
                pathList += pathSep + libPath;
            } else {
                pathList = libPath;
            }
        }
        mLibPaths = pathList.split(pathSep);
        length = mLibPaths.length;
        for (int i = 0; i < length; i++) {
            if (!mLibPaths[i].endsWith(fileSep)) mLibPaths[i] += fileSep;
            if (false) System.out.println("Native lib path:  " + mLibPaths[i]);
        }
    }

    /**
     * Finds a class. This method is called by {@code loadClass()} after the
     * parent ClassLoader has failed to find a loaded class of the same name.
     *
     * @param name
     *            The "binary name" of the class to search for, in a
     *            human-readable form like "java.lang.String" or
     *            "java.net.URLClassLoader$3$1".
     * @return the {@link Class} object representing the class
     * @throws ClassNotFoundException
     *             if the class cannot be found
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        ensureInit();
        byte[] data = null;
        int length = mPaths.length;
        for (int i = 0; i < length; i++) {
            if (mDexs[i] != null) {
                Class clazz = mDexs[i].loadClassBinaryName(name, this);
                if (clazz != null) return clazz;
            } else if (mZips[i] != null) {
                String fileName = name.replace('.', '/') + ".class";
                data = loadFromArchive(mZips[i], fileName);
            } else {
                File pathFile = mFiles[i];
                if (pathFile.isDirectory()) {
                    String fileName = mPaths[i] + "/" + name.replace('.', '/') + ".class";
                    data = loadFromDirectory(fileName);
                } else {
                }
            }
        }
        throw new ClassNotFoundException(name + " in loader " + this);
    }

    /**
     * Finds a resource. This method is called by {@code getResource()} after
     * the parent ClassLoader has failed to find a loaded resource of the same
     * name.
     *
     * @param name
     *            The name of the resource to find
     * @return the location of the resource as a URL, or {@code null} if the
     *         resource is not found.
     */
    @Override
    protected URL findResource(String name) {
        ensureInit();
        int length = mPaths.length;
        for (int i = 0; i < length; i++) {
            URL result = findResource(name, i);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Finds an enumeration of URLs for the resource with the specified name.
     *
     * @param resName
     *            the name of the resource to find.
     * @return an enumeration of {@code URL} objects for the requested resource.
     * @since Android 1.0
     */
    @Override
    protected Enumeration<URL> findResources(String resName) {
        ensureInit();
        int length = mPaths.length;
        ArrayList<URL> results = new ArrayList<URL>();
        for (int i = 0; i < length; i++) {
            URL result = findResource(resName, i);
            if (result != null) {
                results.add(result);
            }
        }
        return new EnumerateListArray<URL>(results);
    }

    private URL findResource(String name, int i) {
        File pathFile = mFiles[i];
        ZipFile zip = mZips[i];
        if (zip != null) {
            if (isInArchive(zip, name)) {
                try {
                    return new URL("jar:" + pathFile.toURL() + "!/" + name);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (pathFile.isDirectory()) {
            File dataFile = new File(mPaths[i] + "/" + name);
            if (dataFile.exists()) {
                try {
                    return dataFile.toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (pathFile.isFile()) {
        } else {
            System.err.println("PathClassLoader: can't find '" + mPaths[i] + "'");
        }
        return null;
    }

    private byte[] loadFromDirectory(String path) {
        RandomAccessFile raf;
        byte[] fileData;
        try {
            raf = new RandomAccessFile(path, "r");
        } catch (FileNotFoundException fnfe) {
            return null;
        }
        try {
            fileData = new byte[(int) raf.length()];
            raf.read(fileData);
            raf.close();
        } catch (IOException ioe) {
            System.err.println("Error reading from " + path);
            fileData = null;
        }
        return fileData;
    }

    private byte[] loadFromArchive(ZipFile zip, String name) {
        ZipEntry entry;
        entry = zip.getEntry(name);
        if (entry == null) return null;
        ByteArrayOutputStream byteStream;
        InputStream stream;
        int count;
        try {
            stream = zip.getInputStream(entry);
            byteStream = new ByteArrayOutputStream((int) entry.getSize());
            byte[] buf = new byte[4096];
            while ((count = stream.read(buf)) > 0) byteStream.write(buf, 0, count);
            stream.close();
        } catch (IOException ioex) {
            return null;
        }
        return byteStream.toByteArray();
    }

    private boolean isInArchive(ZipFile zip, String name) {
        return zip.getEntry(name) != null;
    }

    /**
     * Finds a native library. This method is called after the parent
     * ClassLoader has failed to find a native library of the same name.
     *
     * @param libname
     *            The name of the library to find
     * @return the complete path of the library, or {@code null} if the library
     *         is not found.
     */
    protected String findLibrary(String libname) {
        ensureInit();
        String fileName = System.mapLibraryName(libname);
        for (int i = 0; i < mLibPaths.length; i++) {
            String pathName = mLibPaths[i] + fileName;
            File test = new File(pathName);
            if (test.exists()) return pathName;
        }
        return null;
    }

    /**
     * Returns package information for the given package. Unfortunately, the
     * PathClassLoader doesn't really have this information, and as a non-secure
     * ClassLoader, it isn't even required to, according to the spec. Yet, we
     * want to provide it, in order to make all those hopeful callers of
     * <code>myClass.getPackage().getName()</code> happy. Thus we construct a
     * Package object the first time it is being requested and fill most of the
     * fields with dummy values. The Package object is then put into the
     * ClassLoader's Package cache, so we see the same one next time. We don't
     * create Package objects for null arguments or for the default package.
     * <p>
     * There a limited chance that we end up with multiple Package objects
     * representing the same package: It can happen when when a package is
     * scattered across different JAR files being loaded by different
     * ClassLoaders. Rather unlikely, and given that this whole thing is more or
     * less a workaround, probably not worth the effort.
     *
     * @param name
     *            the name of the class
     * @return the package information for the class, or {@code null} if there
     *         is not package information available for it
     */
    @Override
    protected Package getPackage(String name) {
        if (name != null && !"".equals(name)) {
            synchronized (this) {
                Package pack = super.getPackage(name);
                if (pack == null) {
                    pack = definePackage(name, "Unknown", "0.0", "Unknown", "Unknown", "0.0", "Unknown", null);
                }
                return pack;
            }
        }
        return null;
    }

    private static class EnumerateListArray<T> implements Enumeration<T> {

        private final ArrayList mList;

        private int i = 0;

        EnumerateListArray(ArrayList list) {
            mList = list;
        }

        public boolean hasMoreElements() {
            return i < mList.size();
        }

        public T nextElement() {
            if (i >= mList.size()) throw new NoSuchElementException();
            return (T) mList.get(i++);
        }
    }

    ;

    public String toString() {
        return getClass().getName() + "[" + path + "]";
    }
}
