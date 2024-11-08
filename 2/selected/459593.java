package com.ibm.wala.util.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.NestedJarFileModule;
import com.ibm.wala.core.plugin.CorePlugin;
import com.ibm.wala.util.debug.Assertions;

/**
 * This class provides files that are packaged with this plug-in
 */
public class FileProvider {

    /**
   * This class uses reflection to access classes and methods that are only
   * available when Eclipse is running as an IDE environment. The choice to use
   * reflection is related to builds: with this design the build doesn't need to
   * provide IDE bundles during compilation and hence can spot invalid uses of
   * such classes through this bundle.
   * 
   * Because of this class, this bundle must OPTIONALY require
   * 'org.eclipse.core.resources'.
   */
    private static final class EclipseUtil {

        private static Object workspaceRoot = null;

        private static Method workspaceRoot_getFile = null;

        public static Module getJarFileModule(String fileName, ClassLoader loader) {
            try {
                if (workspaceRoot_getFile == null) {
                    Class<?> cls = Class.forName("org.eclipse.core.resources.ResourcesPlugin");
                    Method getWorkspaceMethod = cls.getDeclaredMethod("getWorkspace");
                    Object workspace = getWorkspaceMethod.invoke(null);
                    Method getRoot = workspace.getClass().getDeclaredMethod("getRoot");
                    workspaceRoot = getRoot.invoke(workspace);
                    workspaceRoot_getFile = workspaceRoot.getClass().getMethod("getFile", IPath.class);
                }
                IPath path = new Path(fileName);
                if (workspaceRoot_getFile.invoke(workspaceRoot, path) != null) {
                    return new JarFileModule(new JarFile(fileName, false));
                }
            } catch (Exception e) {
            }
            return null;
        }
    }

    private static final int DEBUG_LEVEL = 0;

    public FileProvider() {
        super();
    }

    /**
   * @param fileName
   * @return the jar file packaged with this plug-in of the given name, or null
   *         if not found.
   */
    public static Module getJarFileModule(String fileName) throws IOException {
        return getJarFileModule(fileName, FileProvider.class.getClassLoader());
    }

    public static Module getJarFileModule(String fileName, ClassLoader loader) throws IOException {
        if (CorePlugin.getDefault() == null) {
            return getJarFileFromClassLoader(fileName, loader);
        } else if (CorePlugin.IS_RESOURCES_BUNDLE_AVAILABLE) {
            Module module = EclipseUtil.getJarFileModule(fileName, loader);
            if (module != null) {
                return module;
            }
        }
        return getFromPlugin(CorePlugin.getDefault(), fileName);
    }

    public static URL getResource(String fileName) throws IOException {
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
        return getResource(fileName, FileProvider.class.getClassLoader());
    }

    public static URL getResource(String fileName, ClassLoader loader) throws IOException {
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
        if (CorePlugin.getDefault() == null && loader == null) {
            throw new IllegalArgumentException("null loader");
        }
        return (CorePlugin.getDefault() == null) ? loader.getResource(fileName) : FileLocator.find(CorePlugin.getDefault().getBundle(), new Path(fileName), null);
    }

    public static File getFile(String fileName) throws IOException {
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
        return getFile(fileName, FileProvider.class.getClassLoader());
    }

    public static File getFile(String fileName, ClassLoader loader) throws IOException {
        return (CorePlugin.getDefault() == null) ? getFileFromClassLoader(fileName, loader) : getFileFromPlugin(CorePlugin.getDefault(), fileName);
    }

    /**
   * @param fileName
   * @return the jar file packaged with this plug-in of the given name, or null
   *         if not found.
   * @throws IllegalArgumentException
   *           if p is null
   */
    public static File getFileFromPlugin(Plugin p, String fileName) throws IOException {
        if (p == null) {
            throw new IllegalArgumentException("p is null");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
        URL url = getFileURLFromPlugin(p, fileName);
        if (url == null) {
            throw new FileNotFoundException(fileName);
        }
        return new File(filePathFromURL(url));
    }

    /**
   * @param fileName
   * @return the jar file packaged with this plug-in of the given name, or null
   *         if not found.
   */
    private static JarFileModule getFromPlugin(Plugin p, String fileName) throws IOException {
        URL url = getFileURLFromPlugin(p, fileName);
        return (url == null) ? null : new JarFileModule(new JarFile(filePathFromURL(url)));
    }

    /**
   * get a file URL for a file from a plugin
   * 
   * @param fileName
   *          the file name
   * @return the URL, or <code>null</code> if the file is not found
   * @throws IOException
   */
    private static URL getFileURLFromPlugin(Plugin p, String fileName) throws IOException {
        try {
            URL url = FileLocator.find(p.getBundle(), new Path(fileName), null);
            if (url == null) {
                String libFileName = "lib/" + fileName;
                url = FileLocator.find(p.getBundle(), new Path(libFileName), null);
                if (url == null) {
                    String binFileName = "bin/" + fileName;
                    url = FileLocator.find(p.getBundle(), new Path(binFileName), null);
                    if (url == null) {
                        File f = new File(fileName);
                        if (!f.exists()) {
                            return null;
                        } else {
                            url = f.toURI().toURL();
                        }
                    }
                }
            }
            url = FileLocator.toFileURL(url);
            url = fixupFileURLSpaces(url);
            return url;
        } catch (ExceptionInInitializerError e) {
            throw new IOException("failure to get file URL for " + fileName);
        }
    }

    /**
   * escape spaces in a URL, primarily to work around a bug in
   * {@link File#toURL()}
   * 
   * @param url
   * @return an escaped version of the URL
   */
    private static URL fixupFileURLSpaces(URL url) {
        String urlString = url.toExternalForm();
        StringBuffer fixedUpUrl = new StringBuffer();
        int lastIndex = 0;
        while (true) {
            int spaceIndex = urlString.indexOf(' ', lastIndex);
            if (spaceIndex < 0) {
                fixedUpUrl.append(urlString.substring(lastIndex));
                break;
            }
            fixedUpUrl.append(urlString.substring(lastIndex, spaceIndex));
            fixedUpUrl.append("%20");
            lastIndex = spaceIndex + 1;
        }
        try {
            return new URL(fixedUpUrl.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Assertions.UNREACHABLE();
        }
        return null;
    }

    /**
   * @throws FileNotFoundException
   */
    public static File getFileFromClassLoader(String fileName, ClassLoader loader) throws FileNotFoundException {
        if (loader == null) {
            throw new IllegalArgumentException("null loader");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
        URL url = null;
        try {
            url = loader.getResource(fileName);
        } catch (Exception e) {
        }
        if (DEBUG_LEVEL > 0) {
            System.err.println(("FileProvider got url: " + url + " for " + fileName));
        }
        if (url == null) {
            File f = new File(fileName);
            if (f.exists()) {
                return f;
            }
            throw new FileNotFoundException(fileName);
        } else {
            return new File(filePathFromURL(url));
        }
    }

    /**
   * @throws FileNotFoundException
   */
    public static InputStream getInputStreamFromClassLoader(String fileName, ClassLoader loader) throws FileNotFoundException {
        if (loader == null) {
            throw new IllegalArgumentException("null loader");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
        InputStream is = loader.getResourceAsStream(fileName);
        if (is == null) {
            throw new FileNotFoundException(fileName);
        }
        return is;
    }

    /**
   * @return the jar file packaged with this plug-in of the given name, or null
   *         if not found: wrapped as a JarFileModule or a NestedJarFileModule
   * @throws IOException
   */
    public static Module getJarFileFromClassLoader(String fileName, ClassLoader loader) throws IOException {
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
        if (loader == null) {
            throw new IllegalArgumentException("null loader");
        }
        URL url = loader.getResource(fileName);
        if (DEBUG_LEVEL > 0) {
            System.err.println("FileProvider got url: " + url + " for " + fileName);
        }
        if (url == null) {
            try {
                return new JarFileModule(new JarFile(fileName, false));
            } catch (ZipException e) {
                throw new IOException("Could not find file: " + fileName);
            }
        }
        if (url.getProtocol().equals("jar")) {
            JarURLConnection jc = (JarURLConnection) url.openConnection();
            JarFile f = jc.getJarFile();
            JarEntry entry = jc.getJarEntry();
            JarFileModule parent = new JarFileModule(f);
            return new NestedJarFileModule(parent, entry);
        } else {
            String filePath = filePathFromURL(url);
            return new JarFileModule(new JarFile(filePath, false));
        }
    }

    /**
   * Properly creates the String file name of a {@link URL}. This works around a
   * bug in the Sun implementation of {@link URL#getFile()}, which doesn't
   * properly handle file paths with spaces (see <a href=
   * "http://sourceforge.net/tracker/index.php?func=detail&aid=1565842&group_id=176742&atid=878458"
   * >bug report</a>). For now, fails with an assertion if the url is malformed.
   * 
   * @param url
   * @return the path name for the url
   * @throws IllegalArgumentException
   *           if url is null
   */
    public static String filePathFromURL(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url is null");
        }
        URI uri = new File(url.getPath()).toURI();
        String filePath = uri.getPath();
        return filePath;
    }
}
