package org.extwind.osgi.tomcat.catalina.loader;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.jar.JarFile;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.Constants;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
import org.apache.naming.resources.Resource;
import org.apache.tomcat.util.modeler.Registry;

/**
 * @author donf.yang
 * 
 */
public class OSGiAppLoader implements Lifecycle, Loader, PropertyChangeListener, MBeanRegistration {

    private static org.apache.juli.logging.Log log = org.apache.juli.logging.LogFactory.getLog(OSGiAppLoader.class);

    /**
	 * Construct a new WebappLoader with no defined parent class loader (so that
	 * the actual parent will be the system class loader).
	 */
    public OSGiAppLoader() {
        this(null);
    }

    /**
	 * Construct a new WebappLoader with the specified class loader to be
	 * defined as the parent of the ClassLoader we ultimately create.
	 * 
	 * @param parent
	 *            The parent class loader
	 */
    public OSGiAppLoader(ClassLoader parent) {
        super();
        this.parentClassLoader = parent;
    }

    /**
	 * First load of the class.
	 */
    private static boolean first = true;

    /**
	 * The class loader being managed by this Loader component.
	 * This is a ClassLoader for OSGi-App, actually it just worked with Bundle Repository.
	 */
    private OSGiAppClassLoader classLoader = null;

    /**
	 * The Container with which this Loader has been associated.
	 */
    private Container container = null;

    /**
	 * The "follow standard delegation model" flag that will be used to
	 * configure our ClassLoader.
	 */
    private boolean delegate = false;

    /**
	 * The descriptive information about this Loader implementation.
	 */
    private static final String info = "org.extwind.osgi.tomcat.catalina.loader.OSGiAppLoader/1.0";

    /**
	 * The lifecycle event support for this component.
	 */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    /**
	 * The Java class name of the ClassLoader implementation to be used. This
	 * class should extend WebappClassLoader, otherwise, a different loader
	 * implementation must be used.
	 */
    private String loaderClass = "org.extwind.osgi.tomcat.catalina.loader.OSGiAppClassLoader";

    /**
	 * The parent class loader of the class loader we will create.
	 */
    private ClassLoader parentClassLoader = null;

    /**
	 * The reloadable flag for this Loader.
	 */
    private boolean reloadable = false;

    /**
	 * The set of repositories associated with this class loader.
	 */
    private String repositories[] = new String[0];

    /**
	 * The string manager for this package.
	 */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);

    /**
	 * Has this component been started?
	 */
    private boolean started = false;

    /**
	 * The property change support for this component.
	 */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);

    /**
	 * Classpath set in the loader.
	 */
    private String classpath = null;

    /**
	 * Repositories that are set in the loader, for JMX.
	 */
    private ArrayList loaderRepositories = null;

    /**
	 * Return the Java class loader to be used by this Container.
	 */
    public ClassLoader getClassLoader() {
        return ((ClassLoader) classLoader);
    }

    /**
	 * Return the Container with which this Logger has been associated.
	 */
    public Container getContainer() {
        return (container);
    }

    /**
	 * Set the Container with which this Logger has been associated.
	 * 
	 * @param container
	 *            The associated Container
	 */
    public void setContainer(Container container) {
        if ((this.container != null) && (this.container instanceof Context)) ((Context) this.container).removePropertyChangeListener(this);
        Container oldContainer = this.container;
        this.container = container;
        support.firePropertyChange("container", oldContainer, this.container);
        if ((this.container != null) && (this.container instanceof Context)) {
            setReloadable(((Context) this.container).getReloadable());
            ((Context) this.container).addPropertyChangeListener(this);
        }
    }

    /**
	 * Return the "follow standard delegation model" flag used to configure our
	 * ClassLoader.
	 */
    public boolean getDelegate() {
        return (this.delegate);
    }

    /**
	 * Set the "follow standard delegation model" flag used to configure our
	 * ClassLoader.
	 * 
	 * @param delegate
	 *            The new flag
	 */
    public void setDelegate(boolean delegate) {
        boolean oldDelegate = this.delegate;
        this.delegate = delegate;
        support.firePropertyChange("delegate", new Boolean(oldDelegate), new Boolean(this.delegate));
    }

    /**
	 * Return descriptive information about this Loader implementation and the
	 * corresponding version number, in the format
	 * <code>&lt;description&gt;/&lt;version&gt;</code>.
	 */
    public String getInfo() {
        return (info);
    }

    /**
	 * Return the ClassLoader class name.
	 */
    public String getLoaderClass() {
        return (this.loaderClass);
    }

    /**
	 * Set the ClassLoader class name.
	 * 
	 * @param loaderClass
	 *            The new ClassLoader class name
	 */
    public void setLoaderClass(String loaderClass) {
        this.loaderClass = loaderClass;
    }

    /**
	 * Return the reloadable flag for this Loader.
	 */
    public boolean getReloadable() {
        return (this.reloadable);
    }

    /**
	 * Set the reloadable flag for this Loader.
	 * 
	 * @param reloadable
	 *            The new reloadable flag
	 */
    public void setReloadable(boolean reloadable) {
        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        support.firePropertyChange("reloadable", new Boolean(oldReloadable), new Boolean(this.reloadable));
    }

    /**
	 * Add a property change listener to this component.
	 * 
	 * @param listener
	 *            The listener to add
	 */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
	 * Add a new repository to the set of repositories for this class loader.
	 * 
	 * @param repository
	 *            Repository to be added
	 */
    public void addRepository(String repository) {
        if (log.isDebugEnabled()) log.debug(sm.getString("webappLoader.addRepository", repository));
        for (int i = 0; i < repositories.length; i++) {
            if (repository.equals(repositories[i])) return;
        }
        String results[] = new String[repositories.length + 1];
        for (int i = 0; i < repositories.length; i++) results[i] = repositories[i];
        results[repositories.length] = repository;
        repositories = results;
        if (started && (classLoader != null)) {
            classLoader.addRepository(repository);
            if (loaderRepositories != null) loaderRepositories.add(repository);
            setClassPath();
        }
    }

    /**
	 * Execute a periodic task, such as reloading, etc. This method will be
	 * invoked inside the classloading context of this container. Unexpected
	 * throwables will be caught and logged.
	 */
    public void backgroundProcess() {
    }

    /**
	 * Return the set of repositories defined for this class loader. If none are
	 * defined, a zero-length array is returned. For security reason, returns a
	 * clone of the Array (since String are immutable).
	 */
    public String[] findRepositories() {
        return ((String[]) repositories.clone());
    }

    public String[] getRepositories() {
        return ((String[]) repositories.clone());
    }

    /**
	 * Extra repositories for this loader
	 */
    public String getRepositoriesString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < repositories.length; i++) {
            sb.append(repositories[i]).append(":");
        }
        return sb.toString();
    }

    public String[] getLoaderRepositories() {
        if (loaderRepositories == null) return null;
        String res[] = new String[loaderRepositories.size()];
        loaderRepositories.toArray(res);
        return res;
    }

    public String getLoaderRepositoriesString() {
        String repositories[] = getLoaderRepositories();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < repositories.length; i++) {
            sb.append(repositories[i]).append(":");
        }
        return sb.toString();
    }

    /**
	 * Classpath, as set in org.apache.catalina.jsp_classpath context property
	 * 
	 * @return The classpath
	 */
    public String getClasspath() {
        return classpath;
    }

    /**
	 * Has the internal repository associated with this Loader been modified,
	 * such that the loaded classes should be reloaded?
	 */
    public boolean modified() {
        return (classLoader.modified());
    }

    /**
	 * Used to periodically signal to the classloader to release JAR resources.
	 */
    public void closeJARs(boolean force) {
        if (classLoader != null) {
            classLoader.closeJARs(force);
        }
    }

    /**
	 * Remove a property change listener from this component.
	 * 
	 * @param listener
	 *            The listener to remove
	 */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
	 * Return a String representation of this component.
	 */
    public String toString() {
        StringBuffer sb = new StringBuffer("OSGiAppLoader[");
        if (container != null) sb.append(container.getName());
        sb.append("]");
        return (sb.toString());
    }

    /**
	 * Add a lifecycle event listener to this component.
	 * 
	 * @param listener
	 *            The listener to add
	 */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
	 * Get the lifecycle listeners associated with this lifecycle. If this
	 * Lifecycle has no listeners registered, a zero-length array is returned.
	 */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
	 * Remove a lifecycle event listener from this component.
	 * 
	 * @param listener
	 *            The listener to remove
	 */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    private boolean initialized = false;

    public void init() {
        initialized = true;
        if (oname == null) {
            if (container instanceof StandardContext) {
                try {
                    StandardContext ctx = (StandardContext) container;
                    Engine eng = (Engine) ctx.getParent().getParent();
                    String path = ctx.getPath();
                    if (path.equals("")) {
                        path = "/";
                    }
                    oname = new ObjectName(ctx.getEngineName() + ":type=Loader,path=" + path + ",host=" + ctx.getParent().getName());
                    Registry.getRegistry(null, null).registerComponent(this, oname, null);
                    controller = oname;
                } catch (Exception e) {
                    log.error("Error registering loader", e);
                }
            }
        }
        if (container == null) {
        }
    }

    public void destroy() {
        if (controller == oname) {
            Registry.getRegistry(null, null).unregisterComponent(oname);
            oname = null;
        }
        initialized = false;
    }

    /**
	 * Start this component, initializing our associated class loader.
	 * 
	 * @exception LifecycleException
	 *                if a lifecycle error occurs
	 */
    public void start() throws LifecycleException {
        if (!initialized) init();
        if (started) throw new LifecycleException(sm.getString("webappLoader.alreadyStarted"));
        if (log.isDebugEnabled()) log.debug(sm.getString("webappLoader.starting"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;
        if (container.getResources() == null) {
            log.info("No resources for " + container);
            return;
        }
        URLStreamHandlerFactory streamHandlerFactory = new DirContextURLStreamHandlerFactory();
        if (first) {
            first = false;
            try {
                URL.setURLStreamHandlerFactory(streamHandlerFactory);
            } catch (Exception e) {
                log.error("Error registering jndi stream handler", e);
            } catch (Throwable t) {
                log.info("Dual registration of jndi stream handler: " + t.getMessage());
            }
        }
        try {
            classLoader = createClassLoader();
            classLoader.setResources(container.getResources());
            classLoader.setDelegate(this.delegate);
            if (container instanceof StandardContext) classLoader.setAntiJARLocking(((StandardContext) container).getAntiJARLocking());
            for (int i = 0; i < repositories.length; i++) {
                classLoader.addRepository(repositories[i]);
            }
            if (classLoader instanceof Lifecycle) ((Lifecycle) classLoader).start();
            DirContextURLStreamHandler.bind((ClassLoader) classLoader, this.container.getResources());
            StandardContext ctx = (StandardContext) container;
            Engine eng = (Engine) ctx.getParent().getParent();
            String path = ctx.getPath();
            if (path.equals("")) {
                path = "/";
            }
            ObjectName cloname = new ObjectName(ctx.getEngineName() + ":type=OSGiClassLoader,path=" + path + ",host=" + ctx.getParent().getName());
            Registry.getRegistry(null, null).registerComponent(classLoader, cloname, null);
        } catch (Throwable t) {
            log.error("LifecycleException ", t);
            throw new LifecycleException("start: ", t);
        }
    }

    /**
	 * Stop this component, finalizing our associated class loader.
	 * 
	 * @exception LifecycleException
	 *                if a lifecycle error occurs
	 */
    public void stop() throws LifecycleException {
        if (!started) throw new LifecycleException(sm.getString("webappLoader.notStarted"));
        if (log.isDebugEnabled()) log.debug(sm.getString("webappLoader.stopping"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;
        if (container instanceof Context) {
            ServletContext servletContext = ((Context) container).getServletContext();
            servletContext.removeAttribute(Globals.CLASS_PATH_ATTR);
        }
        if (classLoader instanceof Lifecycle) ((Lifecycle) classLoader).stop();
        DirContextURLStreamHandler.unbind((ClassLoader) classLoader);
        try {
            StandardContext ctx = (StandardContext) container;
            Engine eng = (Engine) ctx.getParent().getParent();
            String path = ctx.getPath();
            if (path.equals("")) {
                path = "/";
            }
            ObjectName cloname = new ObjectName(ctx.getEngineName() + ":type=OSGiClassLoader,path=" + path + ",host=" + ctx.getParent().getName());
            Registry.getRegistry(null, null).unregisterComponent(cloname);
        } catch (Throwable t) {
            log.error("LifecycleException ", t);
        }
        classLoader = null;
        destroy();
    }

    /**
	 * Process property change events from our associated Context.
	 * 
	 * @param event
	 *            The property change event that has occurred
	 */
    public void propertyChange(PropertyChangeEvent event) {
        if (!(event.getSource() instanceof Context)) return;
        Context context = (Context) event.getSource();
        if (event.getPropertyName().equals("reloadable")) {
            try {
                setReloadable(((Boolean) event.getNewValue()).booleanValue());
            } catch (NumberFormatException e) {
                log.error(sm.getString("webappLoader.reloadable", event.getNewValue().toString()));
            }
        }
    }

    /**
	 * Create associated classLoader.
	 */
    private OSGiAppClassLoader createClassLoader() throws Exception {
        Class clazz = Class.forName(loaderClass);
        OSGiAppClassLoader classLoader = null;
        if (parentClassLoader == null) {
            parentClassLoader = container.getParentClassLoader();
        }
        Class[] argTypes = { ClassLoader.class };
        Object[] args = { parentClassLoader };
        Constructor constr = clazz.getConstructor(argTypes);
        classLoader = (OSGiAppClassLoader) constr.newInstance(args);
        return classLoader;
    }

    /**
	 * Configure associated class loader permissions.
	 */
    private void setPermissions() {
        if (!Globals.IS_SECURITY_ENABLED) return;
        if (!(container instanceof Context)) return;
        ServletContext servletContext = ((Context) container).getServletContext();
        File workDir = (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
        if (workDir != null) {
            try {
                String workDirPath = workDir.getCanonicalPath();
                classLoader.addPermission(new FilePermission(workDirPath, "read,write"));
                classLoader.addPermission(new FilePermission(workDirPath + File.separator + "-", "read,write,delete"));
            } catch (IOException e) {
            }
        }
        try {
            URL rootURL = servletContext.getResource("/");
            classLoader.addPermission(rootURL);
            String contextRoot = servletContext.getRealPath("/");
            if (contextRoot != null) {
                try {
                    contextRoot = (new File(contextRoot)).getCanonicalPath();
                    classLoader.addPermission(contextRoot);
                } catch (IOException e) {
                }
            }
            URL classesURL = servletContext.getResource("/WEB-INF/classes/");
            classLoader.addPermission(classesURL);
            URL libURL = servletContext.getResource("/WEB-INF/lib/");
            classLoader.addPermission(libURL);
            if (contextRoot != null) {
                if (libURL != null) {
                    File rootDir = new File(contextRoot);
                    File libDir = new File(rootDir, "WEB-INF/lib/");
                    try {
                        String path = libDir.getCanonicalPath();
                        classLoader.addPermission(path);
                    } catch (IOException e) {
                    }
                }
            } else {
                if (workDir != null) {
                    if (libURL != null) {
                        File libDir = new File(workDir, "WEB-INF/lib/");
                        try {
                            String path = libDir.getCanonicalPath();
                            classLoader.addPermission(path);
                        } catch (IOException e) {
                        }
                    }
                    if (classesURL != null) {
                        File classesDir = new File(workDir, "WEB-INF/classes/");
                        try {
                            String path = classesDir.getCanonicalPath();
                            classLoader.addPermission(path);
                        } catch (IOException e) {
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
        }
    }

    /**
	 * Configure the repositories for our class loader, based on the associated
	 * Context.
	 */
    private void setRepositories() {
        if (!(container instanceof Context)) return;
        ServletContext servletContext = ((Context) container).getServletContext();
        if (servletContext == null) return;
        loaderRepositories = new ArrayList();
        File workDir = (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
        if (workDir == null) {
            log.info("No work dir for " + servletContext);
        }
        if (log.isDebugEnabled()) log.debug(sm.getString("webappLoader.deploy", workDir.getAbsolutePath()));
        classLoader.setWorkDir(workDir);
        DirContext resources = container.getResources();
        String classesPath = "/WEB-INF/classes";
        DirContext classes = null;
        try {
            Object object = resources.lookup(classesPath);
            if (object instanceof DirContext) {
                classes = (DirContext) object;
            }
        } catch (NamingException e) {
        }
        if (classes != null) {
            File classRepository = null;
            String absoluteClassesPath = servletContext.getRealPath(classesPath);
            if (absoluteClassesPath != null) {
                classRepository = new File(absoluteClassesPath);
            } else {
                classRepository = new File(workDir, classesPath);
                classRepository.mkdirs();
                copyDir(classes, classRepository);
            }
            if (log.isDebugEnabled()) log.debug(sm.getString("webappLoader.classDeploy", classesPath, classRepository.getAbsolutePath()));
            classLoader.addRepository(classesPath + "/", classRepository);
            loaderRepositories.add(classesPath + "/");
        }
        String libPath = "/WEB-INF/lib";
        classLoader.setJarPath(libPath);
        DirContext libDir = null;
        try {
            Object object = resources.lookup(libPath);
            if (object instanceof DirContext) libDir = (DirContext) object;
        } catch (NamingException e) {
        }
        if (libDir != null) {
            boolean copyJars = false;
            String absoluteLibPath = servletContext.getRealPath(libPath);
            File destDir = null;
            if (absoluteLibPath != null) {
                destDir = new File(absoluteLibPath);
            } else {
                copyJars = true;
                destDir = new File(workDir, libPath);
                destDir.mkdirs();
            }
            try {
                NamingEnumeration enumeration = resources.listBindings(libPath);
                while (enumeration.hasMoreElements()) {
                    Binding binding = (Binding) enumeration.nextElement();
                    String filename = libPath + "/" + binding.getName();
                    if (!filename.endsWith(".jar")) continue;
                    File destFile = new File(destDir, binding.getName());
                    if (log.isDebugEnabled()) log.debug(sm.getString("webappLoader.jarDeploy", filename, destFile.getAbsolutePath()));
                    Resource jarResource = (Resource) binding.getObject();
                    if (copyJars) {
                        if (!copy(jarResource.streamContent(), new FileOutputStream(destFile))) continue;
                    }
                    try {
                        JarFile jarFile = new JarFile(destFile);
                        classLoader.addJar(filename, jarFile, destFile);
                    } catch (Exception ex) {
                    }
                    loaderRepositories.add(filename);
                }
            } catch (NamingException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Set the appropriate context attribute for our class path. This is
	 * required only because Jasper depends on it.
	 */
    private void setClassPath() {
        if (!(container instanceof Context)) return;
        ServletContext servletContext = ((Context) container).getServletContext();
        if (servletContext == null) return;
        if (container instanceof StandardContext) {
            String baseClasspath = ((StandardContext) container).getCompilerClasspath();
            if (baseClasspath != null) {
                servletContext.setAttribute(Globals.CLASS_PATH_ATTR, baseClasspath);
                return;
            }
        }
        StringBuffer classpath = new StringBuffer();
        ClassLoader loader = getClassLoader();
        int layers = 0;
        int n = 0;
        while (loader != null) {
            if (!(loader instanceof URLClassLoader)) {
                String cp = getClasspath(loader);
                if (cp == null) {
                    log.info("Unknown loader " + loader + " " + loader.getClass());
                    break;
                } else {
                    if (n > 0) classpath.append(File.pathSeparator);
                    classpath.append(cp);
                    n++;
                }
                break;
            }
            URL repositories[] = ((URLClassLoader) loader).getURLs();
            for (int i = 0; i < repositories.length; i++) {
                String repository = repositories[i].toString();
                if (repository.startsWith("file://")) repository = repository.substring(7); else if (repository.startsWith("file:")) repository = repository.substring(5); else if (repository.startsWith("jndi:")) repository = servletContext.getRealPath(repository.substring(5)); else continue;
                if (repository == null) continue;
                if (n > 0) classpath.append(File.pathSeparator);
                classpath.append(repository);
                n++;
            }
            loader = loader.getParent();
            layers++;
        }
        this.classpath = classpath.toString();
        servletContext.setAttribute(Globals.CLASS_PATH_ATTR, classpath.toString());
    }

    private String getClasspath(ClassLoader loader) {
        try {
            Method m = loader.getClass().getMethod("getClasspath", new Class[] {});
            if (log.isTraceEnabled()) log.trace("getClasspath " + m);
            if (m == null) return null;
            Object o = m.invoke(loader, new Object[] {});
            if (log.isDebugEnabled()) log.debug("gotClasspath " + o);
            if (o instanceof String) return (String) o;
            return null;
        } catch (Exception ex) {
            if (log.isDebugEnabled()) log.debug("getClasspath ", ex);
        }
        return null;
    }

    /**
	 * Copy directory.
	 */
    private boolean copyDir(DirContext srcDir, File destDir) {
        try {
            NamingEnumeration enumeration = srcDir.list("");
            while (enumeration.hasMoreElements()) {
                NameClassPair ncPair = (NameClassPair) enumeration.nextElement();
                String name = ncPair.getName();
                Object object = srcDir.lookup(name);
                File currentFile = new File(destDir, name);
                if (object instanceof Resource) {
                    InputStream is = ((Resource) object).streamContent();
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy(is, os)) return false;
                } else if (object instanceof InputStream) {
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy((InputStream) object, os)) return false;
                } else if (object instanceof DirContext) {
                    currentFile.mkdir();
                    copyDir((DirContext) object, currentFile);
                }
            }
        } catch (NamingException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
	 * Copy a file to the specified temp directory. This is required only
	 * because Jasper depends on it.
	 */
    private boolean copy(InputStream is, OutputStream os) {
        try {
            byte[] buf = new byte[4096];
            while (true) {
                int len = is.read(buf);
                if (len < 0) break;
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private ObjectName oname;

    private MBeanServer mserver;

    private String domain;

    private ObjectName controller;

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        oname = name;
        mserver = server;
        domain = name.getDomain();
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
    }

    public ObjectName getController() {
        return controller;
    }

    public void setController(ObjectName controller) {
        this.controller = controller;
    }
}
