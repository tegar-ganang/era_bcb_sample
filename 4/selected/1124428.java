package org.tanso.fountain.core.component.container;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import org.apache.commons.vfs.FileChangeEvent;
import org.apache.commons.vfs.FileListener;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileMonitor;
import org.dom4j.Document;
import org.dom4j.Element;
import org.tanso.fountain.core.component.ComponentDescription;
import org.tanso.fountain.core.component.ComponentFactory;
import org.tanso.fountain.core.component.ComponentWrapper;
import org.tanso.fountain.core.component.ServiceReference;
import org.tanso.fountain.core.nodeagent.KernelConnector;
import org.tanso.fountain.core.nodeagent.NodeAgent;
import org.tanso.fountain.core.nodeagent.NodeAgentParams;
import org.tanso.fountain.interfaces.platform.ComponentBase;
import org.tanso.fountain.interfaces.platform.ComponentContext;
import org.tanso.fountain.util.ComponentClassLoaderFactory;
import org.tanso.fountain.util.IFountainClassLoader;
import org.tanso.fountain.util.LogUtil;
import org.tanso.fountain.util.OSInfo;
import org.tanso.fountain.util.Parameters;
import org.tanso.fountain.util.PathUtil;
import org.tanso.fountain.util.XMLDom4JUtil;
import org.tanso.fountain.util.di.DIUtil;

/**
 * This is an implementation class for component container.
 * 
 * @author Haiping Huang
 * 
 */
public class ComponentContainerImpl implements ComponentContainer {

    private int idCount = 0;

    private KernelConnector kc;

    private String componentPath;

    private Activator activatorThread;

    private Deactivator deactivatorThread;

    /**
	 * Component pool. hash&lt;component id, Component wrapper&gt;
	 */
    private SortedMap<Integer, ComponentWrapper> cPool;

    /**
	 * Service provider mapping. hash&lt;service interface, provider component
	 * ids&gt;
	 */
    private Map<String, List<Integer>> spMap;

    /**
	 * Service customer mapping. hash&lt;service interface, customer component
	 * ids&gt;
	 */
    private Map<String, List<Integer>> scMap;

    private static String DEFAULT_COMPONENT_XMLPATH = "FOUNT-INF/components.xml";

    /**
	 * Attribute name for component's config xml in manifest.mf
	 */
    private static String MF_COMPONENT_CONFIG = "Component-Config";

    private IFountainClassLoader componentClassLoader;

    private volatile boolean internalThreadExitFlag = false;

    private Logger logger;

    FileSystemManager fsManager = null;

    FileObject listendir = null;

    public boolean startUp() {
        this.componentPath = kc.getComponentRepositoryPath();
        File[] components;
        components = initClassLoader();
        if (components == null) {
            return false;
        }
        this.activatorThread = new Activator("Component-Activator");
        this.activatorThread.setContextClassLoader((ClassLoader) componentClassLoader);
        this.activatorThread.start();
        this.deactivatorThread = new Deactivator("Component-Deactivator");
        this.deactivatorThread.setContextClassLoader((ClassLoader) componentClassLoader);
        this.deactivatorThread.start();
        loadJars(components);
        if (!startDirWatching(this.componentPath)) {
            logger.info("[ComponentContainer] Start watching component repository failed. The hot-deploy/replace feature will be disabled.");
        }
        return true;
    }

    /**
	 * Init the class loader. Set the component loading sequence by param -list.
	 * Besides set the component class loader.
	 * 
	 * @return files to be loaded. null if the component repo-path is not a
	 *         directory.
	 */
    private File[] initClassLoader() {
        File[] files = getComponentFileList(this.componentPath);
        if (files == null) {
            return null;
        }
        for (int i = 0, len = files.length; i < len; i++) {
            try {
                URL url = new URL("file:" + files[i].getCanonicalPath());
                this.componentClassLoader.addURL(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return files;
    }

    /**
	 * Get the component to be loaded. May traverse the "component repo-path" or
	 * according to the components defined by the -list param.
	 * 
	 * @param repoDir
	 *            The directory to load the components
	 * @return The component files to be loaded.
	 */
    private File[] getComponentFileList(String repoDir) {
        File reposDir = new File(repoDir);
        String componentSeqFileName = NodeAgentParams.get(NodeAgent.CONFIG_COMLIST);
        if (componentSeqFileName == null) {
            if (!reposDir.isDirectory()) {
                logger.info("[Component Container] ERROR: " + this.componentPath + " is not a directory!");
                return null;
            }
            return reposDir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith("jar");
                }
            });
        } else {
            File seqLst = new File(componentSeqFileName);
            List<File> componentLst = new ArrayList<File>();
            try {
                Scanner s = new Scanner(seqLst);
                String componentPath;
                while (s.hasNextLine()) {
                    componentPath = s.nextLine();
                    File componentFile = null;
                    if (!PathUtil.isRelativePath(componentPath)) {
                        componentFile = new File(componentPath);
                    } else {
                        componentFile = new File(repoDir + File.separator + componentPath);
                    }
                    if (componentFile.exists()) {
                        componentLst.add(componentFile);
                    } else {
                        logger.severe("Can't find component file: " + componentFile);
                    }
                }
                File[] files = new File[componentLst.size()];
                for (int i = 0, l = componentLst.size(); i < l; i++) {
                    files[i] = componentLst.get(i);
                }
                return files;
            } catch (FileNotFoundException e) {
                logger.info("Can't find -list file: " + e.getMessage());
            }
        }
        return null;
    }

    /**
	 * Load components' jars if allowed, instantiate it.
	 * 
	 * @param files
	 *            Jar files to be loaded
	 * 
	 */
    private void loadJars(File[] files) {
        for (int i = 0, len = files.length; i < len; i++) {
            loadOneJar(files[i]);
        }
    }

    /**
	 * Extract libraries in the jar file.
	 * 
	 * @param jar
	 *            The jar file
	 */
    private void extractLibraries(JarFile jar) {
        Enumeration<JarEntry> entries = jar.entries();
        byte[] buffer = new byte[8192];
        StringTokenizer token = new StringTokenizer(System.getProperty("java.library.path"), File.pathSeparator);
        String libraryPath = null;
        while (token.hasMoreTokens()) {
            File libDir = new File(token.nextToken());
            if (libDir.canWrite()) {
                try {
                    libraryPath = libDir.getCanonicalPath() + File.separator;
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
                break;
            }
        }
        if (libraryPath == null) {
            libraryPath = "./";
        }
        String ext;
        if (OSInfo.getLocalOSType().equals(OSInfo.OperationSystemType.Windows)) {
            ext = "dll";
        } else {
            ext = "so";
        }
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            String name = PathUtil.getFileName(e.getName());
            if (name != null && name.endsWith(ext)) {
                FileOutputStream fos = null;
                BufferedInputStream is = null;
                try {
                    is = new BufferedInputStream(jar.getInputStream(e));
                    File libFile = new File(libraryPath + name);
                    libFile.deleteOnExit();
                    if (NodeAgent.ISVERBOSE) {
                        logger.info("Extract lib to: " + libFile.getCanonicalPath());
                    }
                    fos = new FileOutputStream(libFile);
                    int readBytes = 0;
                    while (-1 != (readBytes = is.read(buffer, 0, 8192))) {
                        fos.write(buffer, 0, readBytes);
                    }
                    is.close();
                    fos.close();
                } catch (IOException e1) {
                    logger.info(e1.getMessage());
                } finally {
                    try {
                        is.close();
                        fos.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }
        buffer = null;
    }

    /**
	 * Load components defined in a jar file.
	 * 
	 * @param jar
	 *            The jar file to be loaded.
	 * @return Loaded component number
	 */
    @SuppressWarnings("unchecked")
    private int loadOneJar(File jar) {
        int loadCount = 0;
        JarFile jarFile;
        JarEntry xmlEntry;
        String jarFullPath = null;
        try {
            jarFullPath = jar.getCanonicalPath();
        } catch (IOException e1) {
            e1.printStackTrace();
            return 0;
        }
        try {
            jarFile = new JarFile(jar);
            extractLibraries(jarFile);
            Manifest mf = jarFile.getManifest();
            String componentXMLPath = mf.getMainAttributes().getValue(MF_COMPONENT_CONFIG);
            if (null == componentXMLPath) {
                componentXMLPath = DEFAULT_COMPONENT_XMLPATH;
            }
            xmlEntry = jarFile.getJarEntry(componentXMLPath);
            if (xmlEntry == null) {
                if (NodeAgent.ISVERBOSE) {
                    logger.info("[SKIP JAR] No " + componentXMLPath + " found in jar: " + jarFullPath);
                }
                return loadCount;
            }
            if (NodeAgent.ISVERBOSE) {
                logger.info("Loading component xml \"" + componentXMLPath + "\" in jar: " + jarFullPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return loadCount;
        }
        Document componentDoc = null;
        try {
            InputStream xmlIs = jarFile.getInputStream(xmlEntry);
            componentDoc = XMLDom4JUtil.getXMLDocument(xmlIs, null);
            xmlIs.close();
            jarFile.close();
        } catch (Exception e) {
            e.printStackTrace();
            return loadCount;
        }
        Element root = componentDoc.getRootElement();
        for (Iterator c = root.elementIterator(); c.hasNext(); ) {
            if (loadComponent((Element) c.next(), jarFullPath)) {
                ++loadCount;
            } else {
                logger.info("[ComponentContainer] Load component failed: " + jarFullPath);
            }
        }
        return loadCount;
    }

    /**
	 * Load a component and commit it the the activating queue.
	 * 
	 * @param componentXML
	 *            Components XML Root Element
	 * @param fromJarFilePath
	 *            The component's belonged jar file path. (Absolute path is
	 *            required)
	 * @return true: load succeed. else false
	 */
    private boolean loadComponent(Element componentXML, String fromJarFilePath) {
        ComponentDescription cd = parseOneComponent(componentXML);
        if (null == cd) {
            return false;
        }
        ComponentWrapper cw = new ComponentWrapper();
        cw.setJarFilePath(fromJarFilePath);
        cw.setId(this.nextComponentId());
        cw.setComponentDescription(cd);
        cw.setComponentContext(ComponentContextFactory.getInstance().createContext(cd.getInitParams(), this.kc.createNewKernelConnector()));
        Object componentInstance = ComponentFactory.getInstance().instantiateComponent(cd, (ClassLoader) this.componentClassLoader);
        if (null != componentInstance) {
            if (!cw.setComponentInstance(componentInstance)) {
                return false;
            }
            this.kc.getPlatformServiceContainer().injectPlatformService(componentInstance);
            this.cPool.put(Integer.valueOf(cw.getId()), cw);
            registerServiceReferences(cw.getId(), cw.getComponentDescription().getReferences());
            injectReferenceServices(cw);
            activateComponent(cw.getId());
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Inject the services which the component consumes.
	 * 
	 * @param cw
	 *            The target component to inject.
	 */
    private void injectReferenceServices(ComponentWrapper cw) {
        ComponentDescription cd = cw.getComponentDescription();
        List<ServiceReference> references = cd.getReferences();
        Object targetInstance = cw.getComponentInstance();
        for (ServiceReference ref : references) {
            String refInterfaceName = ref.getReferenceInterface();
            List<Integer> providerList = findServices(refInterfaceName);
            for (Integer pid : providerList) {
                Object serviceInstance = getNewServiceInstance(pid, targetInstance, refInterfaceName);
                try {
                    if (null != serviceInstance) {
                        DIUtil.invokeMethod(targetInstance, ref.getBind(), new String[] { ref.getReferenceInterface() }, new Object[] { serviceInstance });
                        cw.serviceInjected(refInterfaceName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * Get a new service instance from a provider.
	 * 
	 * @param providerId
	 *            The service provider's id.
	 * @param requestor
	 *            The requestor of the service.
	 * @param interfaceName
	 *            The service interface.
	 * @return The new service instance.
	 */
    private Object getNewServiceInstance(Integer providerId, Object requestor, String interfaceName) {
        ComponentWrapper cw = this.cPool.get(providerId);
        if (null != cw) {
            try {
                return DIUtil.invokeMethod(cw.getComponentInstance(), "getService", new Class[] { Object.class, String.class }, new Object[] { requestor, interfaceName });
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    /**
	 * A group of new service has been provided, notify all customers.
	 * 
	 * @param interfaceName
	 *            Interface class name of the new service.
	 * @param serviceProviderId
	 *            Service provider's id.
	 * @throws Exception
	 */
    private void servicesAvailable(int serviceProviderId) {
        ComponentWrapper cw = this.cPool.get(Integer.valueOf(serviceProviderId));
        List<String> interfacesName = cw.getComponentDescription().getProvideServices();
        for (String provideInterface : interfacesName) {
            List<Integer> providerList = this.spMap.get(provideInterface);
            if (null == providerList) {
                providerList = new ArrayList<Integer>();
                this.spMap.put(provideInterface, providerList);
            }
            providerList.add(Integer.valueOf(serviceProviderId));
            Object providerInstance = this.cPool.get(Integer.valueOf(serviceProviderId)).getComponentInstance();
            List<Integer> customerList = this.scMap.get(provideInterface);
            if (null == customerList) {
                continue;
            }
            for (Iterator<Integer> i = customerList.iterator(); i.hasNext(); ) {
                Integer customerId = i.next();
                ComponentWrapper customerWrapper = this.cPool.get(customerId);
                if (null != customerWrapper) {
                    Object serviceInstance = null;
                    try {
                        serviceInstance = DIUtil.invokeMethod(providerInstance, "getService", new Class[] { Object.class, String.class }, new Object[] { customerWrapper.getComponentInstance(), provideInterface });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    boolean bindResult = bindService(customerWrapper, provideInterface, serviceInstance);
                    if (NodeAgent.ISVERBOSE) {
                        this.logger.info("Inject service <" + provideInterface + "> to <" + customerWrapper.getComponentDescription().getName() + "> success? " + bindResult);
                    }
                } else {
                    i.remove();
                }
            }
        }
        this.activatorThread.dealWithWatingQueue();
    }

    /**
	 * This method is to deal with the scene that services provided by a
	 * component is not available.
	 * 
	 * @param providerId
	 *            The component id who is being deactivated
	 */
    private void servicesDown(int providerId) {
        Integer pid = Integer.valueOf(providerId);
        ComponentWrapper cw = this.cPool.get(Integer.valueOf(providerId));
        ComponentDescription cd = cw.getComponentDescription();
        List<String> providingInterfaces = cd.getProvideServices();
        String providerName = cd.getClassName();
        for (String serviceInterf : providingInterfaces) {
            List<Integer> customers = this.scMap.get(serviceInterf);
            if (customers == null) {
                continue;
            }
            for (Iterator<Integer> i = customers.iterator(); i.hasNext(); ) {
                Integer cid = i.next();
                ComponentWrapper ccw = this.cPool.get(cid);
                if (ccw == null) {
                    i.remove();
                    continue;
                }
                unbindService(ccw, serviceInterf, providerName);
                this.deactivateComponent(cid);
                this.activatorThread.addToWaitingQueue(ccw);
            }
            List<Integer> providers = this.spMap.get(serviceInterf);
            providers.remove(pid);
        }
    }

    /**
	 * Bind a service to a customer component. Update the granted service status
	 * 
	 * @param cw
	 *            Target component
	 * @param serviceInterface
	 *            The service's interface name
	 * @param serviceInstance
	 *            The service instance
	 * @return true if bind success. else false
	 */
    private boolean bindService(ComponentWrapper cw, String serviceInterface, Object serviceInstance) {
        String bindMethod = ServiceReference.findInterfaceBindMethod(serviceInterface, cw.getComponentDescription().getReferences());
        if (null != bindMethod && null != serviceInstance) {
            Object customerInstance = cw.getComponentInstance();
            try {
                DIUtil.invokeMethod(customerInstance, bindMethod, new String[] { serviceInterface }, new Object[] { serviceInstance });
                cw.serviceInjected(serviceInterface);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        logger.info("[ComponentContainer] No bind method for " + serviceInterface + " in component: " + cw.getComponentDescription().getName());
        return false;
    }

    /**
	 * Unbind a component's service.
	 * 
	 * @param cw
	 *            The target component
	 * @param serviceInterface
	 *            The unavailable service interface
	 * @param providerName
	 *            The unavailable service interface's provider's class name.
	 * @return true if unbind succeed. else false
	 */
    private boolean unbindService(ComponentWrapper cw, String serviceInterface, String providerName) {
        String unbindMethod = ServiceReference.findInterfaceUnbindMethod(serviceInterface, cw.getComponentDescription().getReferences());
        if (null != unbindMethod) {
            Object customerInstance = cw.getComponentInstance();
            try {
                DIUtil.invokeMethod(customerInstance, unbindMethod, new Object[] { providerName });
                cw.serviceDown(serviceInterface);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        logger.info("[ComponentContainer] No property unbind method for " + serviceInterface + " in component: " + cw.getComponentDescription().getName() + ". Do u foget the String argument?");
        return false;
    }

    /**
	 * Register a new customer for certain service. So that when new service
	 * available, the framework will notify the customer
	 * 
	 * @param customerId
	 *            customer id for a service
	 * @param reference
	 *            Service reference information.
	 */
    private void registerServiceReferences(int customerId, List<ServiceReference> references) {
        if (null == references) {
            return;
        }
        for (ServiceReference reference : references) {
            List<Integer> customerList = this.scMap.get(reference.getReferenceInterface());
            if (null == customerList) {
                customerList = new ArrayList<Integer>();
                this.scMap.put(reference.getReferenceInterface(), customerList);
            }
            customerList.add(Integer.valueOf(customerId));
        }
    }

    /**
	 * Find the services providing the specified interface.
	 * 
	 * @param serviceInterface
	 *            The service interface.
	 * @return Service provider's id list. Maybe length 0
	 */
    private List<Integer> findServices(String serviceInterface) {
        List<Integer> services = this.spMap.get(serviceInterface);
        if (null == services) {
            services = new ArrayList<Integer>();
            this.spMap.put(serviceInterface, services);
        }
        return services;
    }

    /**
	 * Locate the component ids in a loaded jar. Used for hot-replace
	 * 
	 * @param jarFilePath
	 *            The searching jar file. (Absolute path is required)
	 * @return A component id list the jar file contains. May be length 0.
	 */
    private List<Integer> findComponentIdInJar(String jarFilePath) {
        List<Integer> ids = new LinkedList<Integer>();
        Set<Entry<Integer, ComponentWrapper>> entries = this.cPool.entrySet();
        for (Entry<Integer, ComponentWrapper> e : entries) {
            if (e.getValue().getJarFilePath().equals(jarFilePath)) {
                ids.add(e.getKey());
            }
        }
        return ids;
    }

    /**
	 * Parse one component according to the xml config file.
	 * 
	 * @param componentRoot
	 *            Component's XML Document root element
	 * @return Component description for the component. null if parsing failed
	 */
    @SuppressWarnings("unchecked")
    private ComponentDescription parseOneComponent(Element componentRoot) {
        ComponentDescription cd = null;
        String name = null, className = null;
        List<String> provideServices = new ArrayList<String>();
        List<ServiceReference> references = new ArrayList<ServiceReference>();
        Parameters initParams = new Parameters();
        name = componentRoot.attributeValue("name");
        className = componentRoot.element("implementation").attributeValue("class");
        Element elemService, elemReferences, elemInitParams;
        elemService = componentRoot.element("service");
        elemReferences = componentRoot.element("references");
        elemInitParams = componentRoot.element("init-params");
        if (null != elemService) {
            for (Iterator i = elemService.elements("provide").iterator(); i.hasNext(); ) {
                provideServices.add(((Element) i.next()).attributeValue("interface"));
            }
        }
        if (null != elemReferences) {
            for (Iterator i = elemReferences.elements("reference").iterator(); i.hasNext(); ) {
                Element reference = (Element) i.next();
                references.add(new ServiceReference(reference.attributeValue("name"), reference.attributeValue("interface"), reference.attributeValue("bind"), reference.attributeValue("unbind")));
            }
        }
        if (null != elemInitParams) {
            for (Iterator i = elemInitParams.elements("param").iterator(); i.hasNext(); ) {
                Element param = (Element) i.next();
                initParams.add(param.attributeValue("name"), param.getText());
            }
        }
        cd = new ComponentDescription(name, className, provideServices, references, initParams);
        return cd;
    }

    public String showComponentStatus() {
        StringBuffer sb = new StringBuffer(1000);
        Collection<ComponentWrapper> collection = this.cPool.values();
        sb.append(String.format("%n"));
        sb.append(String.format("%-7S %-8S %-5S %-30S %-1S%n", "ID", "STATE", "AS/TS", "COMPONENT NAME", "IMPLEMENTATION CLASS"));
        for (ComponentWrapper i : collection) {
            sb.append(i.toString()).append(PathUtil.LINESEPARATOR);
        }
        return sb.toString();
    }

    public boolean initialize(Parameters args) {
        try {
            this.componentClassLoader = ComponentClassLoaderFactory.getInstance().createComponentClassLoader(new URL[] { new URL("file:" + this.componentPath) }, kc.getFrameworkClassLoader());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        this.scMap = new Hashtable<String, List<Integer>>();
        this.spMap = new Hashtable<String, List<Integer>>();
        this.cPool = new TreeMap<Integer, ComponentWrapper>();
        this.logger = LogUtil.getLogger(this.getClass());
        return true;
    }

    private boolean startDirWatching(String dir) {
        try {
            fsManager = VFS.getManager();
            listendir = fsManager.resolveFile(dir);
        } catch (FileSystemException e) {
            e.printStackTrace();
            return false;
        }
        DefaultFileMonitor fm = new DefaultFileMonitor(new FileListener() {

            public void fileCreated(FileChangeEvent event) throws Exception {
                FileObject fileObject = event.getFile();
                FileName fileName = fileObject.getName();
                if (fileName.getExtension().equals("jar")) {
                    componentClassLoader.addURL(new URL(fileName.getURI()));
                    if (NodeAgent.ISVERBOSE) {
                        logger.info("[ComponentContainer] Detecting new jar archive: " + fileName.getBaseName());
                    }
                    loadOneJar(new File(componentPath + File.separator + fileName.getBaseName()));
                }
            }

            public void fileDeleted(FileChangeEvent event) throws Exception {
            }

            public void fileChanged(FileChangeEvent event) throws Exception {
                FileObject fileObject = event.getFile();
                FileName fileName = fileObject.getName();
                File modifiedFile = new File(componentPath + File.separator + fileName.getBaseName());
                List<Integer> modifiedIds = findComponentIdInJar(modifiedFile.getCanonicalPath());
                for (Integer i : modifiedIds) {
                    removeComponent(i.intValue());
                }
                loadOneJar(modifiedFile);
            }
        });
        fm.setRecursive(true);
        fm.addFile(listendir);
        fm.start();
        return true;
    }

    private class Deactivator extends Thread {

        private BlockingQueue<ComponentWrapper> deactivatingQueue;

        /**
		 * Removing list for components that will be remove permanently.
		 */
        private Queue<Integer> removingList;

        public BlockingQueue<ComponentWrapper> getDeactivatingQueue() {
            return deactivatingQueue;
        }

        public boolean addToRemovingList(Integer componentId) {
            return this.removingList.add(componentId);
        }

        private void tryDeleteComponent(Integer componentId) {
            if (removingList.contains(componentId)) {
                cPool.remove(componentId);
                removingList.remove(componentId);
            }
        }

        public Deactivator(String name) {
            super(name);
            deactivatingQueue = new LinkedBlockingQueue<ComponentWrapper>();
            removingList = new LinkedList<Integer>();
        }

        public Deactivator() {
            deactivatingQueue = new LinkedBlockingQueue<ComponentWrapper>();
            removingList = new LinkedList<Integer>();
        }

        public void run() {
            while (!internalThreadExitFlag) {
                try {
                    ComponentWrapper cw = deactivatingQueue.take();
                    ComponentBase component = (ComponentBase) cw.getComponentInstance();
                    if (ComponentBase.STATE_ACTIVE != component.getState()) {
                        tryDeleteComponent(Integer.valueOf(cw.getId()));
                        continue;
                    }
                    try {
                        Boolean returnValue = (Boolean) DIUtil.invokeMethod(component, "deactivate", new Object[0]);
                        component.setState(ComponentBase.STATE_DEACTIVE);
                        if (returnValue.booleanValue()) {
                            if (NodeAgent.ISVERBOSE) {
                                logger.info("[ComponentContainer] Component \"" + cw.getComponentDescription().getName() + "\" deactivated complete!");
                            }
                        } else {
                            logger.info("[ComponentContainer] Component \"" + cw.getComponentDescription().getName() + "\" deactivated failed! [return false] Entering ERROR State");
                            component.setState(ComponentBase.STATE_ERROR);
                        }
                        servicesDown(cw.getId());
                        if (returnValue.booleanValue()) {
                            tryDeleteComponent(Integer.valueOf(cw.getId()));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.warning("[ComponentContainer] Component \"" + cw.getComponentDescription().getName() + "\" deactivated failed! [exception caught]");
                    }
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }

    /**
	 * This is the components activating thread. It maintains an activating
	 * queue and a waiting queue. So that a component will be activate later
	 * when all its required services are available.
	 * 
	 * @author Haiping Huang
	 * 
	 */
    private class Activator extends Thread {

        private BlockingQueue<ComponentWrapper> activatingQueue;

        /**
		 * Waiting queue for components that can't be activated for the moment.
		 */
        private Queue<ComponentWrapper> waitingQueue;

        public BlockingQueue<ComponentWrapper> getActivatingQueue() {
            return activatingQueue;
        }

        public Activator(String name) {
            super(name);
            activatingQueue = new LinkedBlockingQueue<ComponentWrapper>();
            waitingQueue = new LinkedList<ComponentWrapper>();
        }

        public Activator() {
            activatingQueue = new LinkedBlockingQueue<ComponentWrapper>();
            waitingQueue = new LinkedList<ComponentWrapper>();
        }

        /**
		 * Add a component to the waiting queue to wait all required services
		 * available.
		 * 
		 * @param cw
		 *            The component to be wating.
		 * @return true if add success. else false
		 */
        public boolean addToWaitingQueue(ComponentWrapper cw) {
            return this.waitingQueue.add(cw);
        }

        /**
		 * Force the activating thread to deal with waiting queue.
		 */
        public void dealWithWatingQueue() {
            while (!this.waitingQueue.isEmpty()) {
                this.activatingQueue.add(this.waitingQueue.remove());
            }
        }

        public void run() {
            while (!internalThreadExitFlag) {
                try {
                    ComponentWrapper cw = activatingQueue.take();
                    ComponentBase component = (ComponentBase) cw.getComponentInstance();
                    if (ComponentBase.STATE_ACTIVE == component.getState()) {
                        continue;
                    }
                    if (!cw.isServiceAllInjected()) {
                        this.waitingQueue.add(cw);
                        continue;
                    }
                    try {
                        Boolean returnValue = (Boolean) DIUtil.invokeMethod(component, "activate", new Class[] { ComponentContext.class }, new Object[] { cw.getComponentContext() });
                        component.setState(ComponentBase.STATE_ACTIVE);
                        if (returnValue.booleanValue()) {
                            if (NodeAgent.ISVERBOSE) {
                                logger.info("[ComponentContainer] Component \"" + cw.getComponentDescription().getName() + "\" activated complete!");
                            }
                            servicesAvailable(cw.getId());
                            dealWithWatingQueue();
                            if (NodeAgent.ISVERBOSE) {
                                System.out.println(showComponentStatus());
                            }
                        } else {
                            logger.info("[ComponentContainer] Component \"" + cw.getComponentDescription().getName() + "\" activated failed! [return false]");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.warning("[ComponentContainer] Component \"" + cw.getComponentDescription().getName() + "\" activated failed! [exception caught]");
                    }
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }

    public boolean activateComponent(int componentId) {
        ComponentWrapper cw = this.cPool.get(Integer.valueOf(componentId));
        try {
            this.activatorThread.getActivatingQueue().put(cw);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean deactivateComponent(int componentId) {
        ComponentWrapper cw = this.cPool.get(Integer.valueOf(componentId));
        if (null == cw) {
            return false;
        }
        try {
            this.deactivatorThread.getDeactivatingQueue().put(cw);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int importComponents(String jarFilePath) {
        File jarFile = new File(jarFilePath);
        if (jarFile.exists()) {
            return this.loadOneJar(jarFile);
        } else {
            logger.info("[ComponentContainer] Importing jar <" + jarFilePath + "> not exists!");
            return -1;
        }
    }

    public boolean removeComponent(int componentId) {
        boolean flag = this.deactivatorThread.addToRemovingList((Integer.valueOf(componentId)));
        if (!deactivateComponent(componentId)) {
            return false;
        }
        return flag;
    }

    public void dispose() {
        this.internalThreadExitFlag = true;
        this.activatorThread.interrupt();
        this.deactivatorThread.interrupt();
        this.kc = null;
        this.componentPath = null;
        this.componentClassLoader = null;
        this.cPool.clear();
        this.cPool = null;
        this.logger = null;
        this.scMap.clear();
        this.scMap = null;
        this.spMap.clear();
        this.spMap = null;
        this.activatorThread = null;
        this.deactivatorThread = null;
    }

    public void bindKernelConnector(KernelConnector kc) {
        this.kc = kc;
    }

    /**
	 * Get a new unique for a component
	 * 
	 * @return New unique id in the container.
	 */
    private int nextComponentId() {
        synchronized (this) {
            return this.idCount++;
        }
    }

    public synchronized void clear() {
        Set<Integer> ids = this.cPool.keySet();
        for (Integer id : ids) {
            this.removeComponent(id);
        }
        while (true) {
            try {
                Thread.sleep(1000);
                if (this.cPool.size() == 0) {
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
