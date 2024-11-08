package fi.hip.gb.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads and initializes classes from external JAR file. Can also list all
 * classes implementing certain interface.
 * 
 * @author Juho Karppinen
 */
public class ClassLoaderUtils extends URLClassLoader {

    /** urls for the JAR files */
    private URL[] jarFiles;

    private static Log log = LogFactory.getLog(ClassLoaderUtils.class);

    /**
     * Creates new classloader utility. NOTICE, call 
     * {@link ClassLoaderUtils#prepareJars(URL[], String)}
     * beforehand if given JAR files are about to change during the lifetime of the JVM.
     * 
     * @param parent
     *            parent class which classloader is extended
     * @param files an array of JAR files that ARE STATIC, use temporary files for
     * dynamic jars
     * @throws IOException if cannot load classes from JAR file
     */
    public ClassLoaderUtils(Object parent, URL[] files) throws IOException {
        super(files, parent.getClass().getClassLoader());
        this.jarFiles = files;
    }

    /**
     * Prepares JAR files by creating copies of original files.
     * Java virtual machine doesn't let classes to modify, so to
     * load new version of classes have to be from different JAR file.
     * 
     * @param files files to be copied
     * @param destinationDir
     *         local directory where temporary files are to be written, if
     *         null no temporary files are created. Be carefull when leaving
     *         temp file out, changes to the original file will not affect
     *         until JVM is next time restarted.
     * @return list of URLs for temporary files
     * @throws IOException if failed to create temporary files
     */
    public static URL[] prepareJars(URL[] files, String destinationDir) throws IOException {
        for (int i = 0; i < files.length; i++) {
            if (files[i].getProtocol().toLowerCase().equals("jar")) {
                files[i] = FileUtils.convertFromJarURL(files[i]);
            }
            String filename = FileUtils.getFilename(files[i].getFile());
            if (destinationDir != null) {
                File outputfile = File.createTempFile(filename, null, new File(destinationDir));
                outputfile.deleteOnExit();
                log.debug("JAR file " + files[i].toString() + " has temporary copy at " + outputfile.getPath());
                files[i] = FileUtils.copyFile(files[i], outputfile);
            }
        }
        return files;
    }

    /**
     * Check if object is type of the class.
     * If no direct match, primitives are assumed compatible.
     * 
     * @param c the wanted type
     * @param o object to be checked
     * @return true if object is type of class
     */
    public static boolean typeCheck(Class c, Object o) {
        if (c.isInstance(o)) {
            return true;
        } else if (o == null) {
            return true;
        } else if (c.isPrimitive()) {
            return (c == Boolean.TYPE && Boolean.class.isInstance(o) || c == Character.TYPE && Character.class.isInstance(o) || c == Byte.TYPE && Byte.class.isInstance(o) || c == Short.TYPE && Short.class.isInstance(o) || c == Integer.TYPE && Integer.class.isInstance(o) || c == Long.TYPE && Long.class.isInstance(o) || c == Float.TYPE && Float.class.isInstance(o) || c == Double.TYPE && Double.class.isInstance(o));
        }
        return false;
    }

    /**
     * Loads a class instance from the JAR file.
     * 
     * @param className
     *            classname to be loaded
     * @return new object
     * @throws RemoteException
     */
    public Object loadInstance(String className) throws RemoteException {
        try {
            Class c = loadClass(className);
            return c.newInstance();
        } catch (Exception e) {
            throw new RemoteException("Could not load class " + className + " : " + e.getMessage() + "\n" + TextUtils.getStackTrace(e));
        }
    }

    /**
     * Loads a class from the JAR files with parameter(s) in constructor.
     * 
     * @param className classname to be loaded
     * @param initArgs array of parameter objects
     * @return new object
     * @throws RemoteException with complete stack trace
     */
    public Object loadInstance(String className, Object[] initArgs) throws RemoteException {
        try {
            Class typeClass = loadClass(className);
            constructors: for (Constructor typeConstr : typeClass.getConstructors()) {
                if (typeConstr.getParameterTypes().length == initArgs.length) {
                    for (int i = 0; i < initArgs.length; i++) {
                        if (!typeCheck(typeConstr.getParameterTypes()[i], initArgs[i])) {
                            continue constructors;
                        }
                    }
                    Object o = (initArgs != null) ? typeConstr.newInstance(initArgs) : typeConstr.newInstance(new Object[] {});
                    return o;
                }
            }
            throw new IllegalArgumentException("Constructor not found");
        } catch (Exception e) {
            String args = "";
            for (Object m : initArgs) {
                if (args.length() > 0) args += ", ";
                args += m.getClass().getName();
            }
            throw new RemoteException("Failed to call constructor " + className + "(" + args + ") " + Arrays.toString(initArgs) + " : " + e.getMessage() + "\n" + TextUtils.getStackTrace(e));
        }
    }

    /**
     * Gets all classes implementing given interface or lists all available classes.
     * 
     * @param interfaceType
     *            is a base class or interface which classes must inherit from
     *            or implement. If null no checking is done.
     * @return names of classes which implements asked interface
     */
    public String[] getClassNames(Class interfaceType) throws IOException {
        Class[] classes = getClasses();
        Vector<String> subclasses = new Vector<String>();
        for (int i = 0; i < classes.length; i++) {
            if (interfaceType == null || interfaceType.isAssignableFrom(classes[i])) if (classes[i].getName().indexOf("$") == -1) subclasses.add(classes[i].getName());
        }
        return subclasses.toArray(new String[0]);
    }

    /**
     * Gets all classes inside this jar file
     * 
     * @return an array of the names of all the classes found inside the jar
     *         file
     */
    public Class[] getClasses() throws IOException {
        Vector<Class> classes = new Vector<Class>();
        for (int i = 0; i < jarFiles.length; i++) {
            try {
                JarFile file = new JarFile(FileUtils.convertFromJarURL(jarFiles[i]).getFile());
                for (Enumeration<JarEntry> e = file.entries(); e.hasMoreElements(); ) {
                    String classname = e.nextElement().toString();
                    if (classname.endsWith(".class")) {
                        classname = classname.substring(0, classname.indexOf("."));
                        classname = classname.replaceAll("/", ".");
                        Class typeClass = loadClass(classname);
                        classes.add(typeClass);
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new RemoteException("Class not found" + e.getMessage());
            }
        }
        return classes.toArray(new Class[0]);
    }

    /**
     * Creates a new instance of the class without any constructor.
     * Class is loaded from programs classpath.
     * 
     * @param classname
     *            class type of object to be created
     * @return newly created object, or null if errors loading the class
     */
    public static Object getNewInstanceOf(String classname) {
        try {
            Class typeClass = Class.forName(classname);
            return typeClass.newInstance();
        } catch (java.lang.Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates new instance of specified classname and data with String
     * constructor. Class is loaded from programs classpath not inside JAR file
     * 
     * @param classname
     *            class type of object to be created
     * @param constructor data to be inserted to String constructor
     * @return newly created object, or null if errors loading the class
     */
    public static Object getNewInstanceOf(String classname, String constructor) {
        try {
            Class typeClass = Class.forName(classname);
            Class[] paramTypes = new Class[] { (new String()).getClass() };
            Object[] initArgs = new Object[] { constructor };
            Constructor typeConstr = typeClass.getConstructor(paramTypes);
            return typeConstr.newInstance(initArgs);
        } catch (java.lang.Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
