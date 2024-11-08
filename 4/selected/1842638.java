package org.xito.boot;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.jar.*;
import java.util.zip.*;
import java.security.*;
import java.lang.reflect.*;

/**
 * Classloader that loads code from a Cache.
 *
 * @author Deane Richan
 */
public class CacheClassLoader extends SecureClassLoader {

    private static Logger logger = Logger.getLogger(CacheClassLoader.class.getName());

    protected ExecutableDesc execDesc;

    private Vector serviceLoaderRefs = new Vector();

    protected Vector classPath = new Vector();

    private HashMap eagerEntries = new HashMap();

    private HashMap lazyEntries = new HashMap();

    private HashMap packageCache = new HashMap();

    private HashSet definedPackages = new HashSet();

    private boolean destroyed_flag = false;

    private boolean natives_unpacked_flag = false;

    protected boolean initialized_flag = false;

    /**
    * Create a CacheClassLoader
    * @param execDesc Descriptor of Executable
    * @param Parent Class Loader
    */
    public CacheClassLoader(ExecutableDesc execDesc, ClassLoader parent) throws ServiceNotFoundException {
        super(parent);
        this.execDesc = execDesc;
        initialize(execDesc.getClassPath(), execDesc.getServiceRefs());
    }

    /**
    * Create a CacheClassLoader
    * @param ClassPath
    * @param ServiceRefs
    * @param Parent Class Loader
    */
    public CacheClassLoader(ClassPathEntry[] classPath, ServiceDescStub[] serviceRefs, ClassLoader parent) throws ServiceNotFoundException {
        super(parent);
        initialize(classPath, serviceRefs);
    }

    /**
    * Setup service and classpath refs and download all Eager Resources
    */
    protected void initialize(ClassPathEntry[] classPath, ServiceDescStub[] serviceRefs) throws ServiceNotFoundException {
        if (initialized_flag) return;
        if (serviceRefs != null) {
            for (int i = 0; i < serviceRefs.length; i++) {
                addServiceRefLoader(serviceRefs[i]);
            }
        }
        if (classPath != null) {
            for (int i = 0; i < classPath.length; i++) {
                this.classPath.add(classPath[i].copy());
            }
        }
        downloadEagerResources();
        initialized_flag = true;
    }

    /**
    * Get Main Class from Jar Resources
    */
    public String getMainClassFromJars() {
        Iterator it = eagerEntries.values().iterator();
        while (it.hasNext()) {
            ClassPathEntry e = (ClassPathEntry) it.next();
            String className = e.getMainClassName();
            if (className != null) return className;
        }
        it = lazyEntries.values().iterator();
        while (it.hasNext()) {
            ClassPathEntry e = (ClassPathEntry) it.next();
            String className = e.getMainClassName();
            if (className != null) return className;
        }
        return null;
    }

    /**
    * Get Permissions for a CodeSource
    */
    protected PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection perms = super.getPermissions(cs);
        perms.add(new java.net.SocketPermission("localhost:1024-", "listen"));
        perms.add(new RuntimePermission("stopThread"));
        perms.add(new PropertyPermission("java.version", "read"));
        perms.add(new PropertyPermission("java.vendor", "read"));
        perms.add(new PropertyPermission("java.vendor.url", "read"));
        perms.add(new PropertyPermission("java.class.version", "read"));
        perms.add(new PropertyPermission("os.name", "read"));
        perms.add(new PropertyPermission("os.version", "read"));
        perms.add(new PropertyPermission("os.acrch", "read"));
        perms.add(new PropertyPermission("file.separator", "read"));
        perms.add(new PropertyPermission("path.separator", "read"));
        perms.add(new PropertyPermission("line.separator", "read"));
        perms.add(new PropertyPermission("java.specification.version", "read"));
        perms.add(new PropertyPermission("java.specification.vendor", "read"));
        perms.add(new PropertyPermission("java.specification.name", "read"));
        perms.add(new PropertyPermission("java.vm.specification.version", "read"));
        perms.add(new PropertyPermission("java.vm.specification.vendor", "read"));
        perms.add(new PropertyPermission("java.vm.specification.name", "read"));
        perms.add(new PropertyPermission("java.vm.version", "read"));
        perms.add(new PropertyPermission("java.vm.vendor", "read"));
        perms.add(new PropertyPermission("java.vm.name", "read"));
        URL url = Boot.getCacheManager().convertFromCachedURL(cs.getLocation());
        try {
            if (url.getProtocol().equals("file")) {
                String path = new File(url.getFile()).getAbsolutePath();
                if (path.endsWith(File.separator)) {
                    path = path + "-";
                }
                perms.add(new FilePermission(path, "read"));
                File cacheF = Boot.getCacheManager().getCachedFileForURL(url);
                path = cacheF.getAbsolutePath();
                if (path.endsWith(File.separator)) {
                    path = path + "-";
                }
                perms.add(new FilePermission(path, "read"));
            } else {
                String host = url.getHost();
                if (host == null) {
                    host = "localhost";
                }
                File cacheF = Boot.getCacheManager().getCachedFileForURL(url);
                String path = cacheF.getAbsolutePath();
                if (path.endsWith("_root_")) {
                    path = path.substring(0, path.lastIndexOf(File.separator) + 1);
                }
                if (path.endsWith(File.separator)) {
                    path = path + "-";
                }
                perms.add(new FilePermission(path, "read, write, delete"));
                StringBuffer hostURL = new StringBuffer();
                hostURL.append(url.getProtocol());
                hostURL.append("://");
                hostURL.append(host);
                if (url.getPort() > -1) {
                    hostURL.append(":");
                    hostURL.append(url.getPort());
                }
                File hostCacheFile = Boot.getCacheManager().getCachedFileForURL(new URL(hostURL.toString()));
                path = hostCacheFile.getAbsolutePath();
                if (path.endsWith("_root_")) {
                    path = path.substring(0, path.lastIndexOf(File.separator) + 1);
                }
                if (path.endsWith(File.separator)) {
                    path = path + "-";
                }
                perms.add(new FilePermission(path, "read, write, delete"));
                perms.add(new SocketPermission(host, "connect,accept"));
            }
        } catch (Exception badURL) {
            logger.log(Level.SEVERE, badURL.getMessage(), badURL);
        }
        return perms;
    }

    /**
    * Get the ExecutableDesc used for this ClassLoader
    */
    public ExecutableDesc getExecutableDesc() {
        return execDesc;
    }

    /**
    * Add a ServiceLoader to this services classpath 
    */
    protected void addServiceRefLoader(ServiceDescStub serviceRef) throws ServiceNotFoundException {
        if (serviceRef.getName() == null) return;
        if (serviceRef.getName().equals("*")) {
            serviceLoaderRefs.addAll(ServiceClassLoader.getAllServiceLoaders());
            return;
        } else {
            serviceLoaderRefs.add(ServiceClassLoader.getServiceLoader(serviceRef));
        }
    }

    /**
    * Unpack Native Libraries from their Jar Containers
    */
    protected synchronized void unPackNatives() {
        if (natives_unpacked_flag) return;
        if (execDesc == null) {
            return;
        }
        NativeLibDesc nativeLibs[] = execDesc.getNativeLibs();
        boolean foundLib = false;
        ArrayList libsForOS = new ArrayList();
        for (int i = 0; i < nativeLibs.length; i++) {
            if (nativeLibs[i].getOS() != null && nativeLibs[i].getOS().equals(NativeLibDesc.currentOS())) {
                libsForOS.add(nativeLibs[i].getPath());
                foundLib = true;
            }
        }
        if (!foundLib) return;
        URL urls[] = (URL[]) libsForOS.toArray(new URL[0]);
        CacheManager cm = Boot.getCacheManager();
        cm.downloadResources(urls, cm.getDefaultListener(), null, true);
        for (int i = 0; i < urls.length; i++) {
            unpackNativeJar(urls[i]);
        }
        natives_unpacked_flag = true;
    }

    /**
    * Unpack a native jar into the cache
    */
    protected void unpackNativeJar(URL jarURL) {
        logger.info("unpacking native lib jar: " + jarURL);
        CacheManager cm = Boot.getCacheManager();
        try {
            File f = cm.getCachedFileForURL(jarURL);
            File dir = new File(f.getParentFile(), f.getName() + ".native");
            logger.fine("using native dir: " + dir.getAbsolutePath());
            if (!dir.exists()) {
                dir.mkdir();
            }
            JarFile jarFile = new JarFile(f);
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File unpackedFile = new File(dir, entry.getName());
                if (!unpackedFile.getParentFile().exists()) {
                    unpackedFile.getParentFile().mkdirs();
                }
                if (!entry.isDirectory()) {
                    if (unpackedFile.length() == entry.getSize() && unpackedFile.lastModified() == entry.getTime()) continue;
                    FileOutputStream out = null;
                    InputStream in = null;
                    try {
                        out = new FileOutputStream(unpackedFile);
                        in = jarFile.getInputStream(entry);
                        byte[] buf = new byte[1024];
                        int c = in.read(buf);
                        while (c != -1) {
                            out.write(buf, 0, c);
                            c = in.read(buf);
                        }
                    } catch (IOException ioExp) {
                        logger.log(Level.SEVERE, ioExp.getMessage(), ioExp);
                    } finally {
                        if (out != null) try {
                            out.close();
                        } catch (IOException ioExp) {
                            ioExp.printStackTrace();
                        }
                        if (in != null) try {
                            in.close();
                        } catch (IOException ioExp) {
                            ioExp.printStackTrace();
                        }
                    }
                    unpackedFile.setLastModified(entry.getTime());
                }
            }
        } catch (MalformedURLException badURL) {
            logger.log(Level.SEVERE, badURL.getMessage(), badURL);
        } catch (IOException ioExp) {
            logger.log(Level.SEVERE, ioExp.getMessage(), ioExp);
        }
    }

    /**
    * Find Library. Returns the absolute path name of a native library. This will 
    * cause Native Libraries to be downloaded and unpacked if necessary
    */
    protected String findLibrary(String libname) {
        if (isDestroyed()) {
            throw new IllegalStateException("Classloader for:" + execDesc.getName() + " has been destroyed");
        }
        logger.fine("looking for native library: " + libname);
        unPackNatives();
        if (execDesc == null) {
            return super.findLibrary(libname);
        }
        NativeLibDesc nativeLibs[] = execDesc.getNativeLibs();
        try {
            CacheManager cm = Boot.getCacheManager();
            for (int i = 0; i < nativeLibs.length; i++) {
                File jarFile = cm.getCachedFileForURL(nativeLibs[i].getPath());
                File dir = new File(jarFile.getParentFile(), jarFile.getName() + ".native");
                File libFile = new File(dir, System.mapLibraryName(libname));
                if (libFile.exists()) {
                    logger.info("Found Native Lib:" + libFile.toString());
                    return libFile.getAbsolutePath();
                }
            }
        } catch (MalformedURLException badURL) {
            logger.log(Level.SEVERE, badURL.getMessage(), badURL);
        }
        logger.warning("Couldn't find native lib:" + libname + " for Application:" + execDesc.getName());
        return null;
    }

    /**
    * Download all Eager Resources
    */
    protected synchronized void downloadEagerResources() {
        Iterator it = classPath.iterator();
        ArrayList eagerURLs = new ArrayList();
        ArrayList eagerCachePolicies = new ArrayList();
        while (it.hasNext()) {
            ClassPathEntry entry = (ClassPathEntry) it.next();
            if (entry.getOs() != null && !entry.getOs().equals(NativeLibDesc.currentOS())) {
                logger.info("skipping non-os classpath entry: " + entry.getResourceURL());
                continue;
            }
            if (entry.getDownloadType() == ClassPathEntry.EAGER && (entry.isJar() || entry.isZip())) {
                eagerEntries.put(entry.getResourceURL(), entry);
                eagerURLs.add(entry.getResourceURL());
            } else {
                lazyEntries.put(entry.getResourceURL(), entry);
            }
        }
        URL urls[] = (URL[]) eagerURLs.toArray(new URL[eagerURLs.size()]);
        CachePolicy policies[] = (CachePolicy[]) eagerCachePolicies.toArray(new CachePolicy[eagerCachePolicies.size()]);
        CacheManager cm = Boot.getCacheManager();
        cm.downloadResources(urls, cm.getDefaultListener(), policies, true);
        it = eagerEntries.values().iterator();
        while (it.hasNext()) {
            ClassPathEntry entry = (ClassPathEntry) it.next();
            entry.downloaded_flag = true;
            entry.initializeJar();
        }
    }

    /** 
    * Get a package name from a class name or null if no package
    */
    private static String getPackageName(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf(".");
        if (i <= 0) return null; else {
            return name.substring(0, i);
        }
    }

    /** 
    * Get a package name from a resource name or null if no package
    */
    private static String getResourcePackageName(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf("/");
        if (i <= 0) return null; else {
            return name.substring(0, i - 1);
        }
    }

    /** 
    * Get Class File Name
    */
    protected static String getClassFileName(String name) {
        return name.replace('.', '/') + ".class";
    }

    /**
    * Load a Class from this ClassLoader by looking at each Resource and loading the class data
    * Eager Resopurces are checked first and then Lazy ones
    */
    protected Class findClassDirectly(String name) throws ClassNotFoundException {
        CacheManager cm = Boot.getCacheManager();
        String packName = getPackageName(name);
        HashSet set = (HashSet) packageCache.get(packName);
        if (set == null) set = new HashSet();
        if (!set.isEmpty()) {
            try {
                return findClassInSet(name, set, false);
            } catch (ClassNotFoundException notFound) {
            }
        }
        HashSet eagerSet = new HashSet();
        eagerSet.addAll(eagerEntries.values());
        if (!set.isEmpty()) {
            eagerSet.removeAll(set);
        }
        if (!eagerSet.isEmpty()) {
            try {
                return findClassInSet(name, eagerSet, true);
            } catch (ClassNotFoundException notFound) {
            }
        }
        HashSet lazySet = new HashSet();
        lazySet.addAll(lazyEntries.values());
        if (!set.isEmpty()) {
            lazySet.removeAll(set);
        }
        if (!lazySet.isEmpty()) {
            try {
                return findClassInSet(name, lazySet, true);
            } catch (ClassNotFoundException notFound) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
    * Find a Class in a ClassPath Entry Set
    * @param name of Class
    * @param HashSet of ClassPathEntries
    * @param boolean true if package Cache should be updated
    */
    private Class findClassInSet(String name, HashSet entries, boolean addToPackageCache) throws ClassNotFoundException {
        String packName = getPackageName(name);
        HashSet set = (HashSet) packageCache.get(packName);
        if (set == null) set = new HashSet();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            ClassPathEntry entry = (ClassPathEntry) it.next();
            entry.downloadResource();
            try {
                Class cls = entry.findClass(name, this);
                set.add(entry);
                if (addToPackageCache) {
                    packageCache.put(packName, set);
                }
                if (!definedPackages.contains(packName)) {
                    entry.definePackage(packName, this);
                    definedPackages.add(packName);
                }
                return cls;
            } catch (ClassNotFoundException notFound) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
    * Find a URL in a ClassPath Entry Set
    * @param name of resource
    * @param HashSet of ClassPathEntries
    * @param boolean true if package Cache should be updated
    */
    private URL findResourceInSet(String name, HashSet entries, boolean addToPackageCache) {
        String packName = getResourcePackageName(name);
        HashSet set = (HashSet) packageCache.get(packName);
        if (set == null) set = new HashSet();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            ClassPathEntry entry = (ClassPathEntry) it.next();
            if (!entry.isDownloaded()) {
                entry.downloadResource();
            }
            URL url = entry.findResource(name);
            if (url != null) {
                set.add(entry);
                if (addToPackageCache) {
                    packageCache.put(packName, set);
                }
                return url;
            }
        }
        return null;
    }

    /**
    * Load a Resource from this ClassLoader
    */
    public URL findResource(String name) {
        if (isDestroyed()) {
            throw new IllegalStateException("Classloader for:" + execDesc.getName() + " has been destroyed");
        }
        URL url = null;
        CacheManager cm = Boot.getCacheManager();
        String packName = getResourcePackageName(name);
        HashSet set = (HashSet) packageCache.get(packName);
        if (set == null) set = new HashSet();
        if (!set.isEmpty()) {
            url = findResourceInSet(name, set, false);
            if (url != null) {
                return url;
            }
        }
        HashSet eagerSet = new HashSet();
        eagerSet.addAll(eagerEntries.values());
        if (!set.isEmpty()) {
            eagerSet.removeAll(set);
        }
        if (!eagerSet.isEmpty()) {
            url = findResourceInSet(name, eagerSet, true);
            if (url != null) {
                return url;
            }
        }
        HashSet lazySet = new HashSet();
        lazySet.addAll(lazyEntries.values());
        if (!set.isEmpty()) {
            lazySet.removeAll(set);
        }
        if (!lazySet.isEmpty()) {
            url = findResourceInSet(name, lazySet, true);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
    * Load a Class from this ClassLoader
    */
    public Class findClass(final String name) throws ClassNotFoundException {
        if (isDestroyed()) {
            throw new IllegalStateException("Classloader for:" + execDesc.getName() + " has been destroyed");
        }
        if (name == null || name.equals("")) throw new ClassNotFoundException(null);
        try {
            return findFromSystemServiceLoaders(name);
        } catch (ClassNotFoundException notFound) {
        }
        try {
            return findFromServiceLoaders(name);
        } catch (ClassNotFoundException notFound) {
        }
        Object result = AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                try {
                    return findClassDirectly(name);
                } catch (ClassNotFoundException t) {
                    return t;
                }
            }
        });
        if (result instanceof Class) {
            return (Class) result;
        } else if (result instanceof ClassNotFoundException) {
            throw (ClassNotFoundException) result;
        } else {
            throw new ClassNotFoundException(name);
        }
    }

    /**
    * Load a class from all the service loaders
    */
    protected Class findFromServiceLoaders(String name) throws ClassNotFoundException {
        Iterator it = serviceLoaderRefs.iterator();
        while (it.hasNext()) {
            try {
                ServiceClassLoader loader = (ServiceClassLoader) it.next();
                String serviceName = loader.getService().getName();
                if (loader != this) return loader.loadClass(name);
            } catch (ClassNotFoundException notFound) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
    * Load from system service loaders
    */
    protected Class findFromSystemServiceLoaders(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    /**
    * Return true if this ClassLoader has been destroyed. Destroyed Class loaders
    * should no longer load classes
    */
    public synchronized boolean isDestroyed() {
        return destroyed_flag;
    }

    /**
    * Attempt to release any resources this Class Loader has Open
    */
    public synchronized void destroy() {
        Iterator it = classPath.iterator();
        while (it.hasNext()) {
            ClassPathEntry entry = (ClassPathEntry) it.next();
            entry.close();
        }
        serviceLoaderRefs.clear();
        classPath.clear();
        eagerEntries.clear();
        lazyEntries.clear();
        packageCache.clear();
        destroyed_flag = true;
    }

    /**
    * Return a ClassPath String. Used for Logging etc
    */
    public String getClassPathString() {
        Iterator it = classPath.iterator();
        StringBuffer cp = new StringBuffer();
        while (it.hasNext()) {
            ClassPathEntry entry = (ClassPathEntry) it.next();
            cp.append(entry.getResourceURL().toString());
            cp.append("\n");
        }
        return cp.toString();
    }

    protected Class defineClassInternal(String name, byte[] b, int off, int len, CodeSource cs) {
        return super.defineClass(name, b, off, len, cs);
    }

    protected Package definePackage(String name, String defaultSpecTitle, String defaultSpecVersion, String defaultSpecVendor, String defaultImplTitle, String defaultImplVersion, String defaultImplVendor, URL sealBase) {
        return super.definePackage(name, defaultSpecTitle, defaultSpecVersion, defaultSpecVendor, defaultImplTitle, defaultImplVersion, defaultImplVendor, sealBase);
    }
}
