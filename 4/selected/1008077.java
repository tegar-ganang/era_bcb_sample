package net.sf.oxygen.client.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import net.sf.oxygen.client.ComponentManagerImpl;
import net.sf.oxygen.core.Constants;
import net.sf.oxygen.core.component.ComponentActivator;
import net.sf.oxygen.core.component.ComponentIdentifier;
import net.sf.oxygen.core.component.VersionNumber;
import org.apache.log4j.Logger;

/**
 * Loads component classes from a JAR file. Allows the component manifest to
 * indicate packages that are excluded from sharing between components.
 * @author <A HREF="mailto:seniorc@users.sourceforge.net?subject=net.sf.oxygen.client.loader.JarComponentLoader">Chris Senior</A>
 */
public class JarComponentLoader extends ClassLoader {

    /**
   * Debug logger
   */
    private Logger logger;

    /**
   * A map of previously loaded classes
   */
    private HashMap cache = new HashMap();

    /**
   * The JAR file containing classes and resources
   */
    private JarFile jar;

    /**
   * The JARs manifest file
   */
    private Manifest manifest;

    /**
   * A set of String names of packages that are not shared with other
   * components
   */
    private Set excluded = new HashSet();

    /**
   * The manager of the component in this JAR
   */
    private ComponentManagerImpl componentManager;

    /**
   * The identifier of the component loaded by this loader
   */
    private ComponentIdentifier id;

    /**
   * An instance of this components activator class
   */
    private ComponentActivator activator;

    /**
   * Create a class loader for a JAR file component
   * @param jar The JAR file containing the component
   * @param manager The component manager
   * @throws IOException
   */
    public JarComponentLoader(JarFile jar, int id, ComponentManagerImpl manager) throws IOException {
        this.jar = jar;
        this.componentManager = manager;
        this.manifest = jar.getManifest();
        Attributes attr = manifest.getMainAttributes();
        String friendlyName = attr.getValue(Constants.COMPONENT_NAME);
        if (friendlyName == null) friendlyName = "Component " + id;
        this.id = new ComponentIdentifier(id, friendlyName);
        logger = Logger.getLogger(JarComponentLoader.class.getPackage().getName() + "." + this.getJarFileName());
        String excludeList = attr.getValue(Constants.EXPORT_EXCLUDE);
        if (excludeList != null && excludeList.length() > 0) processExcludes(jar, excludeList);
    }

    /**
   * Process the exclusion attribute of the manifest if any
   * @param jar
   * @param excludeList
   */
    protected void processExcludes(JarFile jar, String excludeList) {
        logger.debug("export-excludes=" + excludeList);
        StringTokenizer tokens = new StringTokenizer(excludeList, ", ");
        while (tokens.hasMoreTokens()) {
            String exclude = tokens.nextToken();
            if (exclude.indexOf('*') >= 0) {
                exclude = exclude.substring(0, exclude.indexOf('*'));
                if (exclude.length() > 0) exclude = exclude.substring(0, exclude.length() - 1);
                logger.debug("exclude=" + exclude);
                Enumeration entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = (JarEntry) entries.nextElement();
                    if (entry.isDirectory()) {
                        String entryName = entry.getName();
                        entryName = entryName.substring(0, entryName.length() - 1);
                        entryName = entryName.replace('/', '.');
                        if (entryName.startsWith(exclude)) {
                            logger.debug("Added exclusion=" + entryName);
                            excluded.add(entryName);
                        }
                    }
                }
            } else {
                logger.debug("Added exclusion=" + exclude);
                excluded.add(exclude);
            }
        }
    }

    /**
   * Does this component declare a component activator
   * @return <code>true</code> If this component declares a component activator; <code>false</code> otherwise
   */
    public boolean isActive() {
        String name = getActivatorName();
        return (name != null && name.length() > 0);
    }

    /**
   * Get the instance of the component activator from this JAR. Creates a new
   * instance if possible or returns a previously intialised instance. Such that
   * there is only one activator instance created per component. 
   * @return The components activator object
   */
    public ComponentActivator getComponentActivator() {
        if (activator == null) {
            String name = getActivatorName();
            String errorReason = "Component not active";
            if (name != null && name.length() > 0) {
                try {
                    Class activatorClass = loadClass(name, true);
                    activator = (ComponentActivator) activatorClass.newInstance();
                    return activator;
                } catch (ClassNotFoundException e) {
                    errorReason = "ClassNotFound: " + e.getMessage();
                } catch (InstantiationException e) {
                    errorReason = "InstantiationException: " + e.getMessage();
                } catch (IllegalAccessException e) {
                    errorReason = "IllegalAccessException: " + e.getMessage();
                } catch (Throwable other) {
                    errorReason = other.getClass().getName() + ": " + other.getMessage();
                }
            }
            throw new RuntimeException(errorReason);
        }
        return activator;
    }

    /**
   * The version number in the manifest (defaults to "all")
   * @return The version number (if any) parsed from the manifest
   */
    public VersionNumber getComponentVersion() {
        Attributes attr = manifest.getMainAttributes();
        String vNum = attr.getValue(Constants.COMPONENT_VERSION);
        logger.debug("Component-Version=" + vNum);
        if (vNum != null) {
            try {
                return new VersionNumber(vNum);
            } catch (Exception e) {
                logger.warn("Failed to parse version number=" + vNum, e);
            }
        }
        logger.debug("No valid version number, using default=*");
        return new VersionNumber("*");
    }

    /**
   * Get the name of the component activator manifest attribute - or null if
   * none specified
   * @return
   */
    private String getActivatorName() {
        Attributes attr = manifest.getMainAttributes();
        return attr.getValue(Constants.COMPONENT_ACTIVATOR);
    }

    /**
   * Load a class (and use other component exports) - this is the load class
   * used by this component
   * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
   */
    public synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, true);
    }

    /**
   * The main class loading method for components - actually does the work.
   * Searches the following places (in order):
   * <OL>
   * <LI>The class loader class cache (local classes only)
   * <LI>The main system class path
   * <LI>The local JAR file
   * <LI>If useExport is true then search the exports of other components in the order they were installed
   * </OL>
   * @param name The name of the class to load
   * @param resolve Resolve the class (see ClassLoader for more info)
   * @param useExport Should we search for other exported classes in the search - or limit to this component only
   * @see JarComponentLoader#loadExport(String, boolean) For how other components load this components classes
   */
    public synchronized Class loadClass(String name, boolean resolve, boolean useExport) throws ClassNotFoundException {
        logger.debug("Loading class=" + name);
        Class result;
        byte classData[];
        result = (Class) cache.get(name);
        if (result != null) {
            logger.debug(name + " is a cached class");
            return result;
        }
        try {
            result = super.findSystemClass(name);
            logger.debug(name + " is a system class (in CLASSPATH).");
            return result;
        } catch (ClassNotFoundException e) {
        }
        classData = getClassData(name);
        if (classData == null && useExport) {
            logger.debug("Attempting to import class=" + name);
            result = componentManager.importClass(name, resolve, this);
            logger.info(name + " is an imported class");
        } else {
            logger.debug(name + " is found in the JAR file; Define new and cache");
            result = defineClass(name, classData, 0, classData.length);
            cache.put(name, result);
        }
        if (resolve && result != null) {
            logger.debug(name + " being resolved");
            resolveClass(result);
        } else if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    /**
   * Load class data from the JAR file into a byte array
   * @param name
   * @return
   */
    private byte[] getClassData(String name) {
        name = name.replace('.', '/');
        name += ".class";
        try {
            JarEntry classEntry = jar.getJarEntry(name);
            if (classEntry == null) return null;
            InputStream in = jar.getInputStream(classEntry);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[32];
            int read = 1;
            while (read > 0) {
                read = in.read(buffer);
                if (read > 0) out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
   * Method for exporting classes from this component. This is called by the
   * framework when trying to locate a class not in a component.
   * @param name The name of the class that might be exported
   * @param resolve Should the class be resolved if found
   * @return The class if found in this component and exported
   * @throws ClassNotFoundException The class is not in this component
   * @throws NotExportedException The class is not exported by this component
   */
    public synchronized Class loadExport(String name, boolean resolve) throws ClassNotFoundException, NotExportedException {
        if (!isExportable(name)) throw new NotExportedException("Class is excluded from export"); else return loadClass(name, resolve, false);
    }

    /**
   * If a class is in the JAR - is it exported by this component
   * @param className The name of the class to test if exported ('.' separated)
   * @return true If a class can be exported by this component
   */
    public boolean isExportable(String className) {
        String packageName = className.substring(0, className.lastIndexOf('.'));
        return !(excluded.contains(className) || excluded.contains(packageName) || excluded.contains(""));
    }

    /**
   * The identifier of the component loaded by this JAR loader
   * @return The component identifier
   */
    public ComponentIdentifier getComponentIdentifier() {
        return id;
    }

    /**
   * A set of packages excluded from package sharing by this loader. All
   * packages in this set are not available to other components.
   * @return A set of export-excluded package names as Strings
   */
    public Set getExcludedPackages() {
        return excluded;
    }

    /**
   * The name of the JAR file this loader loads from
   * @return The JAR file name
   */
    public String getJarFileName() {
        return jar.getName();
    }
}
