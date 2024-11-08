package org.apache.tapestry5.ioc.internal.services;

import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.ioc.services.ClassNameLocator;
import org.apache.tapestry5.ioc.services.ClasspathURLConverter;
import org.apache.tapestry5.ioc.util.Stack;
import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class ClassNameLocatorImpl implements ClassNameLocator {

    private static final String CLASS_SUFFIX = ".class";

    private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    private final ClasspathURLConverter converter;

    private final Pattern CLASS_NAME_PATTERN = Pattern.compile("^[_a-z][a-z0-9_]*\\.class$", Pattern.CASE_INSENSITIVE);

    private final Pattern FOLDER_NAME_PATTERN = Pattern.compile("^[_a-z][a-z0-9_]*$", Pattern.CASE_INSENSITIVE);

    static class Queued {

        final URL packageURL;

        final String packagePath;

        public Queued(final URL packageURL, final String packagePath) {
            this.packageURL = packageURL;
            this.packagePath = packagePath;
        }
    }

    public ClassNameLocatorImpl(ClasspathURLConverter converter) {
        this.converter = converter;
    }

    /**
     * Synchronization should not be necessary, but perhaps the underlying ClassLoader's are sensitive to threading.
     */
    public synchronized Collection<String> locateClassNames(String packageName) {
        String packagePath = packageName.replace('.', '/') + "/";
        try {
            return findClassesWithinPath(packagePath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Collection<String> findClassesWithinPath(String packagePath) throws IOException {
        Collection<String> result = CollectionFactory.newList();
        Enumeration<URL> urls = contextClassLoader.getResources(packagePath);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            URL converted = converter.convert(url);
            scanURL(packagePath, result, converted);
        }
        return result;
    }

    private void scanURL(String packagePath, Collection<String> componentClassNames, URL url) throws IOException {
        URLConnection connection = url.openConnection();
        JarFile jarFile;
        if (connection instanceof JarURLConnection) {
            jarFile = ((JarURLConnection) connection).getJarFile();
        } else {
            jarFile = getAlternativeJarFile(url);
        }
        if (jarFile != null) {
            scanJarFile(packagePath, componentClassNames, jarFile);
        } else if (supportsDirStream(url)) {
            Stack<Queued> queue = CollectionFactory.newStack();
            queue.push(new Queued(url, packagePath));
            while (!queue.isEmpty()) {
                Queued queued = queue.pop();
                scanDirStream(queued.packagePath, queued.packageURL, componentClassNames, queue);
            }
        } else {
            String packageName = packagePath.replace("/", ".");
            if (packageName.endsWith(".")) {
                packageName = packageName.substring(0, packageName.length() - 1);
            }
            scanDir(packageName, new File(url.getFile()), componentClassNames);
        }
    }

    /**
     * Check whether container supports opening a stream on a dir/package to get a list of its contents.
     *
     * @param packageURL
     * @return
     */
    private boolean supportsDirStream(URL packageURL) {
        InputStream is = null;
        try {
            is = packageURL.openStream();
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            InternalUtils.close(is);
        }
    }

    private void scanDirStream(String packagePath, URL packageURL, Collection<String> componentClassNames, Stack<Queued> queue) throws IOException {
        InputStream is;
        try {
            is = new BufferedInputStream(packageURL.openStream());
        } catch (FileNotFoundException ex) {
            return;
        }
        Reader reader = new InputStreamReader(is);
        LineNumberReader lineReader = new LineNumberReader(reader);
        String packageName = null;
        try {
            while (true) {
                String line = lineReader.readLine();
                if (line == null) break;
                if (CLASS_NAME_PATTERN.matcher(line).matches()) {
                    if (packageName == null) packageName = packagePath.replace('/', '.');
                    String fullClassName = packageName + line.substring(0, line.length() - CLASS_SUFFIX.length());
                    componentClassNames.add(fullClassName);
                    continue;
                }
                if (FOLDER_NAME_PATTERN.matcher(line).matches()) {
                    URL newURL = new URL(packageURL.toExternalForm() + line + "/");
                    String newPackagePath = packagePath + line + "/";
                    queue.push(new Queued(newURL, newPackagePath));
                }
            }
            lineReader.close();
            lineReader = null;
        } finally {
            InternalUtils.close(lineReader);
        }
    }

    private void scanJarFile(String packagePath, Collection<String> componentClassNames, JarFile jarFile) {
        Enumeration<JarEntry> e = jarFile.entries();
        while (e.hasMoreElements()) {
            String name = e.nextElement().getName();
            if (!name.startsWith(packagePath)) continue;
            if (!name.endsWith(CLASS_SUFFIX)) continue;
            if (name.contains("$")) continue;
            String className = name.substring(0, name.length() - CLASS_SUFFIX.length()).replace("/", ".");
            componentClassNames.add(className);
        }
    }

    /**
     * Scan a dir for classes. Will recursively look in the supplied directory and all sub directories.
     *
     * @param packageName         Name of package that this directory corresponds to.
     * @param dir                 Dir to scan for clases.
     * @param componentClassNames List of class names that have been found.
     */
    private void scanDir(String packageName, File dir, Collection<String> componentClassNames) {
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                String fileName = file.getName();
                if (file.isDirectory()) {
                    scanDir(packageName + "." + fileName, file, componentClassNames);
                } else if (fileName.endsWith(CLASS_SUFFIX)) {
                    String className = packageName + "." + fileName.substring(0, fileName.length() - CLASS_SUFFIX.length());
                    componentClassNames.add(className);
                }
            }
        }
    }

    /**
     * For URLs to JARs that do not use JarURLConnection - allowed by the servlet spec - attempt to produce a JarFile
     * object all the same. Known servlet engines that function like this include Weblogic and OC4J. This is not a full
     * solution, since an unpacked WAR or EAR will not have JAR "files" as such.
     *
     * @param url URL of jar
     * @return JarFile or null
     * @throws java.io.IOException If error occurs creating jar file
     */
    private JarFile getAlternativeJarFile(URL url) throws IOException {
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
}
