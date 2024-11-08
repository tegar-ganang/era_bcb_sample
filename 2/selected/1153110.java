package com.qspin.qtaste.kernel.testapi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.apache.log4j.Logger;
import com.qspin.qtaste.config.TestBedConfiguration;
import com.qspin.qtaste.testsuite.Dependency;
import com.qspin.qtaste.util.Log4jLoggerFactory;
import java.util.Iterator;
import java.util.List;

/**
 * ComponentLoader is responsible for loading all the TestAPIComponent classes depending on the selected testapi_implementation.import tags and register the component methods to the TestAPI.
 * @author lvboque
 */
public class ComponentsLoader {

    private static ComponentsLoader instance = null;

    private static Logger logger = Log4jLoggerFactory.getLogger(ComponentsLoader.class);

    private TestAPI api;

    private List<String> testapiImplementation;

    private HashMap<String, Class<?>> componentMap;

    private ComponentsLoader() {
        componentMap = new HashMap<String, Class<?>>();
        api = TestAPIImpl.getInstance();
        TestBedConfiguration testbedConfig = TestBedConfiguration.getInstance();
        if (testbedConfig != null) {
            testapiImplementation = testbedConfig.getList("testapi_implementation.import");
        } else {
            testapiImplementation = null;
        }
        initialize(testapiImplementation);
        TestBedConfiguration.registerConfigurationChangeHandler(new TestBedConfiguration.ConfigurationChangeHandler() {

            public void onConfigurationChange() {
                List<String> newTestAPIimplementation;
                TestBedConfiguration testbedConfig = TestBedConfiguration.getInstance();
                if (testbedConfig != null) {
                    newTestAPIimplementation = testbedConfig.getList("testapi_implementation.import");
                } else {
                    newTestAPIimplementation = null;
                }
                if (!newTestAPIimplementation.equals(testapiImplementation)) {
                    initialize(newTestAPIimplementation);
                }
            }
        });
    }

    private void initialize(List<String> testAPIimplementation) {
        componentMap.clear();
        api.unregisterAllMethods();
        SingletonComponentFactory.getInstance();
        MultipleInstancesComponentFactory.getInstance();
        if (testAPIimplementation != null) {
            for (Iterator<String> testAPIIter = testAPIimplementation.iterator(); testAPIIter.hasNext(); ) {
                register(testAPIIter.next(), Component.class);
            }
        }
    }

    /**
     * Get an instance of the ComponentsLoader
     * @return the instance
     */
    public static synchronized ComponentsLoader getInstance() {
        if (instance == null) {
            instance = new ComponentsLoader();
        }
        return instance;
    }

    /**
     * Return the implementation Class of the specified component
     * @param component the component name
     * @return the implementation class of the specified component or null if the component is not registered.
     */
    public Class<?> getComponentImplementationClass(String component) {
        return componentMap.get(component);
    }

    /**
     * Get the dependencies of the specified componentName.
     * This function relies on {@link Dependency} annotation.
     * @param componentName The specified component name
     * @return names of the dependencies or !!!!!
     */
    public Dependency getDependencies(String componentName) {
        if (componentMap.containsKey(componentName)) {
            Class<?> c = componentMap.get(componentName);
            return c.getAnnotation(Dependency.class);
        }
        return null;
    }

    /**
     * Analyse the class c and register all the test api methods in to TestAPI
     * @param c the class to analyse
     */
    private void registerTestAPIMethods(Class<?> c) {
        try {
            ComponentFactory factoryObject;
            Class<?> componentInterface = api.getInterfaceClass(c);
            String componentName = componentInterface.getSimpleName();
            componentMap.put(componentInterface.getSimpleName(), c);
            try {
                Field factoryField = c.getField("factory");
                factoryObject = (ComponentFactory) factoryField.get(c);
            } catch (NoSuchFieldException e) {
                factoryObject = SingletonComponentFactory.getInstance();
            }
            registerClassMethods(componentName, componentInterface, c, factoryObject);
            Class<?>[] interfaces = componentInterface.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                registerClassMethods(componentName, interfaces[i], c, factoryObject);
            }
        } catch (ClassNotFoundException e) {
            logger.debug(e.getMessage());
        } catch (IllegalAccessException e) {
            logger.error("Cannot access the 'factory' field of " + c.getName());
        }
    }

    private void registerClassMethods(String componentName, Class<?> componentInterface, Class<?> component, ComponentFactory factoryObject) {
        Method[] methods = componentInterface.getDeclaredMethods();
        for (Method method : methods) {
            String verb = method.getName();
            api.register(componentInterface.getPackage().getName(), componentName, factoryObject, verb);
        }
    }

    /**
     * Register all the classes inheriting or implementing a given
     * class in a given package.
     * @param pckgname the fully qualified name of the package
     * @param tosubclass the Class object to inherit from
     */
    private void register(String pckgname, Class<?> tosubclass) {
        String name = new String(pckgname);
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        name = name.replace('.', '/');
        URL url = ComponentsLoader.class.getResource(name);
        if (url == null) {
            logger.warn("Package " + pckgname + " doesn't exist, no components to load");
            return;
        }
        String platformName = name.replaceFirst("^.*/", "");
        logger.info("Loading components for platform " + platformName + " from " + url);
        File directory = new File(url.getFile().toString().replaceAll("%20", "\\ "));
        if (directory.exists()) {
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class")) {
                    String classname = files[i].substring(0, files[i].length() - 6);
                    try {
                        Class<?> c = Class.forName(pckgname + "." + classname);
                        registerTestAPIMethods(c);
                    } catch (ClassNotFoundException cnfex) {
                        logger.error("Error loading component " + classname, cnfex);
                    }
                }
            }
        } else {
            try {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                String starts = conn.getEntryName();
                JarFile jfile = conn.getJarFile();
                Enumeration<JarEntry> e = jfile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    String entryname = entry.getName();
                    if (entryname.startsWith(starts) && (entryname.lastIndexOf('/') <= starts.length()) && entryname.endsWith(".class")) {
                        String classname = entryname.substring(0, entryname.length() - 6);
                        if (classname.startsWith("/")) {
                            classname = classname.substring(1);
                        }
                        classname = classname.replace('/', '.');
                        try {
                            Class<?> c = Class.forName(classname);
                            registerTestAPIMethods(c);
                        } catch (ClassNotFoundException cnfex) {
                            System.err.println(cnfex);
                        }
                    }
                }
            } catch (IOException ioex) {
                logger.fatal("ComponentsLoader cannot load jar file ", ioex);
            }
        }
    }
}
