package gumbo.core.deploy;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This class loader loads classes, native libraries and resources from the top
 * JAR and from JARs inside top JAR. The loading process looks through JARs
 * hierarchy and allows their tree structure, i.e. nested JARs.
 * <p>
 * The top JAR and nested JARs are included in the classpath and searched for
 * the class or resource to load. The nested JARs could be located in any
 * directories or subdirectories in a parent JAR.
 * <p>
 * All directories or subdirectories in the top JAR and nested JARs are included
 * in the library path and searched for a native library. For example, the
 * library "Native.dll" could be in the JAR root directory as "Native.dll" or in
 * any directory as "lib/Native.dll" or "abc/xyz/Native.dll".
 * <p>
 * This class delegates class loading to the parent class loader and
 * successfully loads classes, native libraries and resources when it works not
 * in a JAR environment.
 * <p>
 * Create a <code>Launcher</code> class to use this class loader and start its
 * main() method to start your application <code>com.mycompany.MyApp</code>
 * <code> 
<pre>
public class MyAppLauncher {

    public static void main(String[] args) {
        JarClassLoader jcl = new JarClassLoader();
        try {
            jcl.invokeMain("com.mycompany.MyApp", args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    } // main()
    
} // class MyAppLauncher
</pre>
 * </code>
 * <p>
 * If the Launcher class is embedded in the app class, in jar manifests and JNLP
 * files reference it as "...MyApp$Launcher" (i.e. use "$" instead of "." as the
 * internal class delimiter).
 * <p>
 * An application could be started in two different environments: <br/>
 * 1. Application is started from an exploded JAR with dependent resources
 * locations defined in a classpath. Command line to start the application could
 * point to the main class e.g. <code>MyApp.main()</code> or to the
 * <code>MyAppLauncher.main()</code> class (see example above). The application
 * behavior in both cases is identical. Application started with
 * <code>MyApp.main()</code> uses system class loader and resources loaded from
 * a file system. Application started with <code>MyAppLauncher.main()</code>
 * uses <code>JarClassLoader</code> which transparently passes class loading to
 * the system class loader. <br/>
 * 2. Application is started from a JAR with dependent JARs and other resources
 * inside the main JAR. Application must be started with
 * <code>MyAppLauncher.main()</code> and <code>JarClassLoader</code> will load
 * <code>MyApp.main()</code> and required resources from the main JAR.
 * <p>
 * Use VM parameters in the command line for logging settings (examples):
 * <ul>
 * <li><code>-DJarClassLoader.logger=[filename]</code> for logging into the
 * file. The default is console.</li>
 * <li><code>-DJarClassLoader.logger.level=INFO</code> for logging level. The
 * default level is ERROR. See also {@link LogLevel}.</li>
 * <li><code>-DJarClassLoader.logger.area=CLASS,RESOURCE</code> for logging
 * area. The default area is ALL. See also {@link LogArea}. Multiple logging
 * areas could be specified with ',' delimiter.</li>
 * </ul>
 * <p>
 * Known issues: some temporary files created by class loader are not deleted on
 * application exit because JVM does not close handles to them. See details in
 * {@link #shutdown()}.
 * <p>
 * See also discussion "How load library from jar file?"
 * http://discuss.develop.com
 * /archives/wa.exe?A2=ind0302&L=advanced-java&D=0&P=4549 Unfortunately, the
 * native method java.lang.ClassLoader$NativeLibrary.unload() is package
 * accessed in a package accessed inner class. Moreover, it's called from
 * finalizer. This does not allow releasing the native library handle and delete
 * the temporary library file. Option to explore: use JNI function
 * UnregisterNatives(). See also native code in
 * ...\jdk\src\share\native\java\lang\ClassLoader.class
 * @version $Revision: 1.32 $
 */
public class XXXJarClassLoader extends ClassLoader {

    /** VM parameter key to turn on logging to file or console. */
    public static final String KEY_LOGGER = "JarClassLoader.logger";

    /**
	 * VM parameter key to define log level. Valid levels are defined in
	 * {@link LogLevel}. Default value is {@link LogLevel#OFF}.
	 */
    public static final String KEY_LOGGER_LEVEL = "JarClassLoader.logger.level";

    /**
	 * VM parameter key to define log area. Valid areas are defined in
	 * {@link LogArea}. Default value is {@link LogArea#ALL}. Multiple areas
	 * could be specified with ',' delimiter (no spaces!).
	 */
    public static final String KEY_LOGGER_AREA = "JarClassLoader.logger.area";

    public enum LogLevel {

        ERROR, WARN, INFO, DEBUG
    }

    public enum LogArea {

        /** Enable all logging areas. */
        ALL, /** Configuration related logging. Enabled always. */
        CONFIG, /** Enable JAR related logging. */
        JAR, /** Enable class loading related logging. */
        CLASS, /** Enable resource loading related logging. */
        RESOURCE, /** Enable native libraries loading related logging. */
        NATIVE
    }

    /**
	 * Sub directory name for temporary files.
	 * <p>
	 * JarClassLoader extracts all JARs and native libraries into temporary
	 * files and makes the best attempt to clean these files on exit.
	 * <p>
	 * The sub directory is created in the directory defined in a system
	 * property "java.io.tmpdir". Verify the content of this directory
	 * periodically and empty it if required. Temporary files could accumulate
	 * there if application was killed.
	 */
    public static final String TMP_SUB_DIRECTORY = "JarClassLoader";

    private File dirTemp;

    private PrintStream logger;

    private List<JarFileInfo> lstJarFile;

    private Set<File> hsDeleteOnExit;

    private Map<String, Class<?>> hmClass;

    private ProtectionDomain pd;

    private LogLevel logLevel;

    private Set<LogArea> hsLogArea;

    private boolean bLogConsole;

    /**
	 * Default constructor. Defines system class loader as a parent class
	 * loader.
	 */
    public XXXJarClassLoader() {
        this(ClassLoader.getSystemClassLoader());
    }

    /**
	 * Constructor.
	 * @param parent class loader parent.
	 */
    public XXXJarClassLoader(ClassLoader parent) {
        super(parent);
        initLogger();
        hmClass = new HashMap<String, Class<?>>();
        lstJarFile = new ArrayList<JarFileInfo>();
        hsDeleteOnExit = new HashSet<File>();
        String sUrlTopJAR = null;
        try {
            pd = getClass().getProtectionDomain();
            CodeSource cs = pd.getCodeSource();
            URL urlTopJAR = cs.getLocation();
            if (!urlTopJAR.getProtocol().equals("jar")) {
                urlTopJAR = new URL("jar:" + urlTopJAR.toString() + "!/");
            }
            sUrlTopJAR = URLDecoder.decode(urlTopJAR.getFile(), "UTF-8");
            logInfo(LogArea.JAR, "Loading from top JAR: %s", sUrlTopJAR);
            JarURLConnection jarCon = (JarURLConnection) urlTopJAR.openConnection();
            JarFile jarFile = jarCon.getJarFile();
            JarFileInfo jarInfo = new JarFileInfo(jarFile.getName(), jarFile, null);
            loadJar(jarInfo);
        } catch (IOException e) {
            logInfo(LogArea.JAR, "Not a JAR: %s %s", sUrlTopJAR, e.toString());
            return;
        }
        checkShading();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                shutdown();
            }
        });
    }

    static int ______INIT;

    private void initLogger() {
        bLogConsole = true;
        this.logger = System.out;
        logLevel = LogLevel.ERROR;
        hsLogArea = new HashSet<LogArea>();
        hsLogArea.add(LogArea.CONFIG);
        String sLogger = System.getProperty(KEY_LOGGER);
        if (sLogger != null) {
            try {
                this.logger = new PrintStream(sLogger);
                bLogConsole = false;
            } catch (FileNotFoundException e) {
                logError(LogArea.CONFIG, "Cannot create log file %s.", sLogger);
            }
        }
        String sLogLevel = System.getProperty(KEY_LOGGER_LEVEL);
        if (sLogLevel != null) {
            try {
                logLevel = LogLevel.valueOf(sLogLevel);
            } catch (Exception e) {
                logError(LogArea.CONFIG, "Not valid parameter in %s=%s", KEY_LOGGER_LEVEL, sLogLevel);
            }
        }
        String sLogArea = System.getProperty(KEY_LOGGER_AREA);
        if (sLogArea != null) {
            String[] tokenAll = sLogArea.split(",");
            try {
                for (String t : tokenAll) {
                    hsLogArea.add(LogArea.valueOf(t));
                }
            } catch (Exception e) {
                logError(LogArea.CONFIG, "Not valid parameter in %s=%s", KEY_LOGGER_AREA, sLogArea);
            }
        }
        if (hsLogArea.size() == 1 && hsLogArea.contains(LogArea.CONFIG)) {
            for (LogArea la : LogArea.values()) {
                hsLogArea.add(la);
            }
        }
    }

    /**
	 * Using temp files (one per inner JAR/DLL) solves many issues: 1. There are
	 * no ways to load JAR defined in a JarEntry directly into the JarFile
	 * object (see also #6 below). 2. Cannot use memory-mapped files because
	 * they are using nio channels, which are not supported by JarFile ctor. 3.
	 * JarFile object keeps opened JAR files handlers for fast access. 4. Deep
	 * resource in a jar-in-jar does not have well defined URL. Making temp file
	 * with JAR solves this problem. 5. Similar issues with native libraries:
	 * <code>ClassLoader.findLibrary()</code> accepts ONLY string with absolute
	 * path to the file with native library. 6. Option
	 * "java.protocol.handler.pkgs" does not allow access to nested JARs(?).
	 * @param inf JAR entry information.
	 * @return temporary file object presenting JAR entry.
	 * @throws JarClassLoaderException
	 */
    private File createTempFile(JarEntryInfo inf) throws JarClassLoaderException {
        if (dirTemp == null) {
            File dir = new File(System.getProperty("java.io.tmpdir"), TMP_SUB_DIRECTORY);
            if (!dir.exists()) {
                dir.mkdir();
            }
            chmod777(dir);
            if (!dir.exists() || !dir.isDirectory()) {
                throw new JarClassLoaderException("Cannot create temp directory " + dir.getAbsolutePath());
            }
            dirTemp = dir;
        }
        File fileTmp = null;
        try {
            fileTmp = File.createTempFile(inf.getName() + ".", null, dirTemp);
            fileTmp.deleteOnExit();
            chmod777(fileTmp);
            byte[] a_by = inf.getJarBytes();
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(fileTmp));
            os.write(a_by);
            os.close();
            return fileTmp;
        } catch (IOException e) {
            throw new JarClassLoaderException(String.format("Cannot create temp file '%s' for %s", fileTmp, inf.jarEntry), e);
        }
    }

    /**
	 * Loads specified JAR.
	 * @param jarFileInfo JAR file spec.  Never null.
	 * @throws IOException
	 */
    private void loadJar(JarFileInfo jarFileInfo) throws IOException {
        lstJarFile.add(jarFileInfo);
        try {
            Enumeration<JarEntry> en = jarFileInfo.jarFile.entries();
            final String EXT_JAR = ".jar";
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                if (je.isDirectory()) {
                    continue;
                }
                String s = je.getName().toLowerCase();
                if (s.lastIndexOf(EXT_JAR) == s.length() - EXT_JAR.length()) {
                    JarEntryInfo inf = new JarEntryInfo(jarFileInfo, je);
                    File fileTemp = createTempFile(inf);
                    logInfo(LogArea.JAR, "Loading inner JAR %s from temp file %s", inf.jarEntry, getFilename4Log(fileTemp));
                    loadJar(new JarFileInfo(inf.getName(), fileTemp, jarFileInfo));
                }
            }
        } catch (JarClassLoaderException e) {
            throw new RuntimeException("ERROR on loading inner JAR: " + e.getMessageAll());
        }
    }

    private JarEntryInfo findJarEntry(String sName) {
        for (JarFileInfo jarFileInfo : lstJarFile) {
            JarFile jarFile = jarFileInfo.jarFile;
            JarEntry jarEntry = jarFile.getJarEntry(sName);
            if (jarEntry != null) {
                return new JarEntryInfo(jarFileInfo, jarEntry);
            }
        }
        return null;
    }

    private List<JarEntryInfo> findJarEntries(String sName) {
        List<JarEntryInfo> lst = new ArrayList<JarEntryInfo>();
        for (JarFileInfo jarFileInfo : lstJarFile) {
            JarFile jarFile = jarFileInfo.jarFile;
            JarEntry jarEntry = jarFile.getJarEntry(sName);
            if (jarEntry != null) {
                lst.add(new JarEntryInfo(jarFileInfo, jarEntry));
            }
        }
        return lst;
    }

    /**
	 * Finds native library entry.
	 * @param sLib Library name. For example for the library name "Native" the
	 * Windows returns entry "Native.dll", the Linux returns entry
	 * "libNative.so", the Mac returns entry "libNative.jnilib".
	 * @return Native library entry.
	 */
    private JarEntryInfo findJarNativeEntry(String sLib) {
        String sName = System.mapLibraryName(sLib);
        for (JarFileInfo jarFileInfo : lstJarFile) {
            JarFile jarFile = jarFileInfo.jarFile;
            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                if (je.isDirectory()) {
                    continue;
                }
                String sEntry = je.getName();
                String[] token = sEntry.split("/");
                if (token.length > 0 && token[token.length - 1].equals(sName)) {
                    logInfo(LogArea.NATIVE, "Loading native library '%s' found as '%s' in JAR %s", sLib, sEntry, jarFileInfo.simpleName);
                    return new JarEntryInfo(jarFileInfo, je);
                }
            }
        }
        return null;
    }

    /**
	 * Loads class from a JAR and searches for all jar-in-jar.
	 * @param sClassName class to load.
	 * @return Loaded class.
	 * @throws JarClassLoaderException.
	 */
    private Class<?> findJarClass(String sClassName) throws JarClassLoaderException {
        Class<?> c = hmClass.get(sClassName);
        if (c != null) {
            return c;
        }
        String sName = sClassName.replace('.', '/') + ".class";
        JarEntryInfo inf = findJarEntry(sName);
        String jarSimpleName = null;
        if (inf != null) {
            jarSimpleName = inf.jarFileInfo.simpleName;
            definePackage(sClassName, inf);
            byte[] a_by = inf.getJarBytes();
            try {
                c = defineClass(sClassName, a_by, 0, a_by.length, pd);
            } catch (ClassFormatError e) {
                throw new JarClassLoaderException(null, e);
            }
        }
        if (c == null) {
            throw new JarClassLoaderException(sClassName);
        }
        hmClass.put(sClassName, c);
        logInfo(LogArea.CLASS, "Loaded %s by %s from JAR %s", sClassName, getClass().getName(), jarSimpleName);
        return c;
    }

    private void checkShading() {
        if (logLevel.ordinal() < LogLevel.WARN.ordinal()) {
            return;
        }
        Map<String, JarFileInfo> hm = new HashMap<String, JarFileInfo>();
        for (JarFileInfo jarFileInfo : lstJarFile) {
            JarFile jarFile = jarFileInfo.jarFile;
            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                if (je.isDirectory()) {
                    continue;
                }
                String sEntry = je.getName();
                if ("META-INF/MANIFEST.MF".equals(sEntry)) {
                    continue;
                }
                JarFileInfo jar = hm.get(sEntry);
                if (jar == null) {
                    hm.put(sEntry, jarFileInfo);
                } else {
                    logWarn(LogArea.JAR, "ENTRY %s IN %s SHADES %s", sEntry, jar.simpleName, jarFileInfo.simpleName);
                }
            }
        }
    }

    static int ______SHUTDOWN;

    /**
	 * Called on shutdown to cleanup for temporary files.
	 * <p>
	 * JVM does not close handles to native libraries files or JARs with
	 * resources loaded as getResourceAsStream(). Temp files are not deleted
	 * even if they are marked deleteOnExit(). They also fail to delete
	 * explicitly. Workaround is to preserve list with temp files in
	 * configuration file "[user.home]/.JarClassLoader" and delete them on next
	 * application run.
	 * <p>
	 * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4171239 "This
	 * occurs only on Win32, which does not allow a file to be deleted until all
	 * streams on it have been closed."
	 */
    private void shutdown() {
        for (JarFileInfo jarFileInfo : lstJarFile) {
            try {
                jarFileInfo.jarFile.close();
            } catch (IOException e) {
            }
            if (jarFileInfo.jarFileInfoParent != null) {
                File file = jarFileInfo.file;
                if (file != null && !file.delete()) {
                    hsDeleteOnExit.add(file);
                }
            }
        }
        File fileCfg = new File(System.getProperty("user.home") + File.separator + ".JarClassLoader");
        deleteOldTemp(fileCfg);
        persistNewTemp(fileCfg);
    }

    /**
	 * Deletes temporary files listed in the file. The method is called on
	 * shutdown().
	 * @param fileCfg file with temporary files list.
	 */
    private void deleteOldTemp(File fileCfg) {
        BufferedReader reader = null;
        try {
            int count = 0;
            reader = new BufferedReader(new FileReader(fileCfg));
            String sLine;
            while ((sLine = reader.readLine()) != null) {
                File file = new File(sLine);
                if (!file.exists()) {
                    continue;
                }
                if (file.delete()) {
                    count++;
                } else {
                    hsDeleteOnExit.add(file);
                }
            }
            logDebug(LogArea.CONFIG, "Deleted %d old temp files listed in %s", count, fileCfg.getAbsolutePath());
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * Creates file with temporary files list. This list will be used to delete
	 * temporary files on the next application launch. The method is called from
	 * shutdown().
	 * @param fileCfg file with temporary files list.
	 */
    private void persistNewTemp(File fileCfg) {
        if (hsDeleteOnExit.size() == 0) {
            logDebug(LogArea.CONFIG, "No temp file names to persist on exit.");
            fileCfg.delete();
            return;
        }
        logDebug(LogArea.CONFIG, "Persisting %d temp file names into %s", hsDeleteOnExit.size(), fileCfg.getAbsolutePath());
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileCfg));
            for (File file : hsDeleteOnExit) {
                if (!file.delete()) {
                    String f = file.getCanonicalPath();
                    writer.write(f);
                    writer.newLine();
                    logWarn(LogArea.JAR, "JVM failed to release %s", f);
                }
            }
        } catch (IOException e) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    static int ______ACCESS;

    /**
	 * Checks how the application was loaded: from JAR or file system.
	 * @return true if application was started from JAR.
	 */
    public boolean isLaunchedFromJar() {
        return (lstJarFile.size() > 0);
    }

    /**
	 * Returns the name of the jar file main class, or null if no "Main-Class"
	 * manifest attributes was defined.
	 * @return Main class declared in JAR's manifest.
	 */
    public String getManifestMainClass() {
        Attributes attr = null;
        if (isLaunchedFromJar()) {
            try {
                Manifest m = lstJarFile.get(0).jarFile.getManifest();
                attr = m.getMainAttributes();
            } catch (IOException e) {
            }
        }
        return (attr == null ? null : attr.getValue(Attributes.Name.MAIN_CLASS));
    }

    /**
	 * Invokes main() method on class with provided parameters.
	 * @param sClass class name in form "MyClass" for default package or
	 * "com.abc.MyClass" for class in some package
	 * @param args arguments for the main() method or null.
	 * @throws Throwable wrapper for many exceptions thrown while
	 * <p>
	 * (1) main() method lookup: ClassNotFoundException, SecurityException,
	 * NoSuchMethodException
	 * <p>
	 * (2) main() method launch: IllegalArgumentException,
	 * IllegalAccessException (disabled)
	 * <p>
	 * (3) Actual cause of InvocationTargetException See {@link http
	 * ://java.sun.com
	 * /developer/Books/javaprogramming/JAR/api/jarclassloader.html} and
	 * {@link http
	 * ://java.sun.com/developer/Books/javaprogramming/JAR/api/example
	 * -1dot2/JarClassLoader.java}
	 */
    public void invokeMain(String sClass, String[] args) throws Throwable {
        Thread.currentThread().setContextClassLoader(this);
        Class<?> clazz = loadClass(sClass);
        logInfo(LogArea.CONFIG, "Launch: %s.main(); Loader: %s", sClass, clazz.getClassLoader());
        Method method = clazz.getMethod("main", new Class<?>[] { String[].class });
        boolean bValidModifiers = false;
        boolean bValidVoid = false;
        if (method != null) {
            method.setAccessible(true);
            int nModifiers = method.getModifiers();
            bValidModifiers = Modifier.isPublic(nModifiers) && Modifier.isStatic(nModifiers);
            Class<?> clazzRet = method.getReturnType();
            bValidVoid = (clazzRet == void.class);
        }
        if (method == null || !bValidModifiers || !bValidVoid) {
            throw new NoSuchMethodException("The main() method in class \"" + sClass + "\" not found.");
        }
        try {
            method.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    static int ______OVERRIDE;

    /**
	 * Class loader JavaDoc encourages overriding findClass(String) in derived
	 * class rather than overriding this method. This does not work for loading
	 * classes from a JAR. Default implementation of loadClass() is able to load
	 * a class from a JAR without calling findClass(). This will "infect" the
	 * loaded class with a system class loader. The system class loader will be
	 * used to load all dependent classes and will fail for jar-in-jar classes.
	 * See also: http://www.cs.purdue.edu/homes/jv/smc/pubs/liang-oopsla98.pdf
	 */
    @Override
    protected synchronized Class<?> loadClass(String sClassName, boolean bResolve) throws ClassNotFoundException {
        logDebug(LogArea.CLASS, "LOADING %s (resolve=%b)", sClassName, bResolve);
        Class<?> c = null;
        try {
            if (isLaunchedFromJar()) {
                try {
                    c = findJarClass(sClassName);
                    return c;
                } catch (JarClassLoaderException e) {
                    if (e.getCause() == null) {
                        logDebug(LogArea.CLASS, "Not found %s in JAR by %s: %s", sClassName, getClass().getName(), e.getMessage());
                    } else {
                        logDebug(LogArea.CLASS, "Error loading %s in JAR by %s: %s", sClassName, getClass().getName(), e.getCause());
                    }
                }
            }
            try {
                ClassLoader cl = getParent();
                c = cl.loadClass(sClassName);
                logInfo(LogArea.CLASS, "Loaded %s by %s", sClassName, cl.getClass().getName());
                return c;
            } catch (ClassNotFoundException e) {
            }
            throw new ClassNotFoundException("JarClassLoader failed to load: " + sClassName);
        } finally {
            if (c != null && bResolve) {
                resolveClass(c);
            }
        }
    }

    /**
	 * @see java.lang.ClassLoader#findResource(java.lang.String)
	 * @return A URL object for reading the resource, or null if the resource
	 * could not be found. Example URL:
	 * jar:file:C:\...\some.jar!/resources/InnerText.txt
	 */
    @Override
    protected URL findResource(String sName) {
        logDebug(LogArea.RESOURCE, "findResource: %s", sName);
        if (isLaunchedFromJar()) {
            JarEntryInfo inf = findJarEntry(normalizeResourceName(sName));
            if (inf != null) {
                URL url = inf.getURL();
                logInfo(LogArea.RESOURCE, "found resource: %s", url);
                return url;
            }
            logInfo(LogArea.RESOURCE, "not found resource: %s", sName);
            return null;
        }
        return super.findResource(sName);
    }

    /**
	 * @see java.lang.ClassLoader#findResources(java.lang.String)
	 * @return An enumeration of {@link java.net.URL <tt>URL</tt>} objects for
	 * the resources
	 */
    @Override
    public Enumeration<URL> findResources(String sName) throws IOException {
        logDebug(LogArea.RESOURCE, "getResources: %s", sName);
        if (isLaunchedFromJar()) {
            List<JarEntryInfo> lstJarEntry = findJarEntries(normalizeResourceName(sName));
            List<URL> lstURL = new ArrayList<URL>();
            for (JarEntryInfo inf : lstJarEntry) {
                URL url = inf.getURL();
                if (url != null) {
                    lstURL.add(url);
                }
            }
            return Collections.enumeration(lstURL);
        }
        return super.findResources(sName);
    }

    /**
	 * @see java.lang.ClassLoader#findLibrary(java.lang.String)
	 * @return The absolute path of the native library.
	 */
    @Override
    protected String findLibrary(String sLib) {
        logDebug(LogArea.NATIVE, "findLibrary: %s", sLib);
        if (isLaunchedFromJar()) {
            JarEntryInfo inf = findJarNativeEntry(sLib);
            if (inf != null) {
                try {
                    File file = createTempFile(inf);
                    logDebug(LogArea.NATIVE, "Loading native library %s from temp file %s", inf.jarEntry, getFilename4Log(file));
                    hsDeleteOnExit.add(file);
                    return file.getAbsolutePath();
                } catch (JarClassLoaderException e) {
                    logInfo(LogArea.NATIVE, "Failure to load native library %s: %s", sLib, e.toString());
                }
            }
            return null;
        }
        return super.findLibrary(sLib);
    }

    static int ______HELPERS;

    /**
	 * The default <code>ClassLoader.defineClass()</code> does not create
	 * package for the loaded class and leaves it null. Each package referenced
	 * by this class loader must be created only once before the
	 * <code>ClassLoader.defineClass()</code> call. The base class
	 * <code>ClassLoader</code> keeps cache with created packages for reuse.
	 * @param sClassName class to load.
	 * @throws IllegalArgumentException If package name duplicates an existing
	 * package either in this class loader or one of its ancestors.
	 */
    private void definePackage(String sClassName, JarEntryInfo inf) throws IllegalArgumentException {
        int pos = sClassName.lastIndexOf('.');
        String sPackageName = pos > 0 ? sClassName.substring(0, pos) : "";
        if (getPackage(sPackageName) == null) {
            JarFileInfo jfi = inf.jarFileInfo;
            definePackage(sPackageName, jfi.getSpecificationTitle(), jfi.getSpecificationVersion(), jfi.getSpecificationVendor(), jfi.getImplementationTitle(), jfi.getImplementationVersion(), jfi.getImplementationVendor(), jfi.getSealURL());
        }
    }

    /**
	 * The system class loader could load resources defined as "com/abc/Foo.txt"
	 * or "com\abc\Foo.txt". This method converts path with '\' to default '/'
	 * JAR delimiter.
	 * @param sName resource name including path.
	 * @return normalized resource name.
	 */
    private String normalizeResourceName(String sName) {
        return sName.replace('\\', '/');
    }

    private void chmod777(File file) {
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
    }

    private String getFilename4Log(File file) {
        if (logger != null) {
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private void logDebug(LogArea area, String sMsg, Object... obj) {
        log(LogLevel.DEBUG, area, sMsg, obj);
    }

    private void logInfo(LogArea area, String sMsg, Object... obj) {
        log(LogLevel.INFO, area, sMsg, obj);
    }

    private void logWarn(LogArea area, String sMsg, Object... obj) {
        log(LogLevel.WARN, area, sMsg, obj);
    }

    private void logError(LogArea area, String sMsg, Object... obj) {
        log(LogLevel.ERROR, area, sMsg, obj);
    }

    private void log(LogLevel level, LogArea area, String sMsg, Object... obj) {
        if (level.ordinal() <= logLevel.ordinal()) {
            if (hsLogArea.contains(LogArea.ALL) || hsLogArea.contains(area)) {
                logger.printf("JarClassLoader-" + level + ": " + sMsg + "\n", obj);
            }
        }
        if (!bLogConsole && level == LogLevel.ERROR) {
            System.out.printf("JarClassLoader-" + level + ": " + sMsg + "\n", obj);
        }
    }

    /**
	 * Inner class with JAR file information.
	 */
    private static class JarFileInfo {

        JarFile jarFile;

        File file;

        JarFileInfo jarFileInfoParent;

        String simpleName;

        Manifest mf;

        /**
		 * Creates an instance, for a JarURLConnection jar file.
		 * @param simpleName Never null.
		 * @param jarFile Never null.
		 * @param jarFileParent None if null.
		 * @throws IOException
		 */
        JarFileInfo(String simpleName, JarFile jarFile, JarFileInfo jarFileParent) throws IOException {
            this.jarFile = jarFile;
            this.file = null;
            this.jarFileInfoParent = jarFileParent;
            this.simpleName = (jarFileParent == null ? "" : jarFileParent.simpleName + "!") + simpleName;
            try {
                this.mf = jarFile.getManifest();
            } catch (IOException e) {
                this.mf = new Manifest();
            }
        }

        /**
		 * Creates an instance, for a normal jar file.
		 * @param simpleName Never null.
		 * @param file Never null.
		 * @param jarFileParent None if null.
		 * @throws IOException
		 */
        JarFileInfo(String simpleName, File file, JarFileInfo jarFileParent) throws IOException {
            this.jarFile = new JarFile(file);
            this.file = file;
            this.jarFileInfoParent = jarFileParent;
            this.simpleName = (jarFileParent == null ? "" : jarFileParent.simpleName + "!") + simpleName;
            try {
                this.mf = jarFile.getManifest();
            } catch (IOException e) {
                this.mf = new Manifest();
            }
        }

        String getSpecificationTitle() {
            return mf.getMainAttributes().getValue(Name.SPECIFICATION_TITLE);
        }

        String getSpecificationVersion() {
            return mf.getMainAttributes().getValue(Name.SPECIFICATION_VERSION);
        }

        String getSpecificationVendor() {
            return mf.getMainAttributes().getValue(Name.SPECIFICATION_VENDOR);
        }

        String getImplementationTitle() {
            return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_TITLE);
        }

        String getImplementationVersion() {
            return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_VERSION);
        }

        String getImplementationVendor() {
            return mf.getMainAttributes().getValue(Name.IMPLEMENTATION_VENDOR);
        }

        URL getSealURL() {
            String seal = mf.getMainAttributes().getValue(Name.SEALED);
            if (seal != null) try {
                return new URL(seal);
            } catch (MalformedURLException e) {
            }
            return null;
        }
    }

    /**
	 * Inner class with JAR entry information. Keeps JAR file and entry object.
	 */
    private static class JarEntryInfo {

        JarFileInfo jarFileInfo;

        JarEntry jarEntry;

        JarEntryInfo(JarFileInfo jarFileInfo, JarEntry jarEntry) {
            this.jarFileInfo = jarFileInfo;
            this.jarEntry = jarEntry;
        }

        URL getURL() {
            try {
                return new URL("jar:file:" + jarFileInfo.jarFile.getName() + "!/" + jarEntry);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        String getName() {
            return jarEntry.getName().replace('/', '_');
        }

        @Override
        public String toString() {
            return "JAR: " + jarFileInfo.jarFile.getName() + " ENTRY: " + jarEntry;
        }

        /**
		 * Read JAR entry and returns byte array of this JAR entry. This is a
		 * helper method to load JAR entry into temporary file.
		 * @param inf JAR entry information object
		 * @return byte array for the specified JAR entry
		 * @throws JarClassLoaderException
		 */
        byte[] getJarBytes() throws JarClassLoaderException {
            DataInputStream dis = null;
            byte[] a_by = null;
            try {
                long lSize = jarEntry.getSize();
                if (lSize <= 0 || lSize >= Integer.MAX_VALUE) {
                    throw new JarClassLoaderException("Invalid size " + lSize + " for entry " + jarEntry);
                }
                a_by = new byte[(int) lSize];
                InputStream is = jarFileInfo.jarFile.getInputStream(jarEntry);
                dis = new DataInputStream(is);
                dis.readFully(a_by);
            } catch (IOException e) {
                throw new JarClassLoaderException(null, e);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                    }
                }
            }
            return a_by;
        }
    }

    /**
	 * Inner class to handle JarClassLoader exceptions.
	 */
    private static class JarClassLoaderException extends Exception {

        private static final long serialVersionUID = 1L;

        JarClassLoaderException(String sMsg) {
            super(sMsg);
        }

        JarClassLoaderException(String sMsg, Throwable eCause) {
            super(sMsg, eCause);
        }

        String getMessageAll() {
            StringBuilder sb = new StringBuilder();
            for (Throwable e = this; e != null; e = e.getCause()) {
                if (sb.length() > 0) {
                    sb.append(" / ");
                }
                String sMsg = e.getMessage();
                if (sMsg == null || sMsg.length() == 0) {
                    sMsg = e.getClass().getSimpleName();
                }
                sb.append(sMsg);
            }
            return sb.toString();
        }
    }
}
