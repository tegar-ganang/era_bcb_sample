package ch.olsen.servicecontainer.node;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import ch.olsen.products.util.Random;
import ch.olsen.products.util.configuration.Configuration;
import ch.olsen.products.util.logging.Logger;
import ch.olsen.servicecontainer.commongwt.client.SessionException;
import ch.olsen.servicecontainer.commongwt.client.UpgradeException;
import ch.olsen.servicecontainer.domain.ExternalDomain;
import ch.olsen.servicecontainer.domain.LocalDomain;
import ch.olsen.servicecontainer.domain.RemoteDomain;
import ch.olsen.servicecontainer.domain.RootDomain;
import ch.olsen.servicecontainer.domain.SCDomain;
import ch.olsen.servicecontainer.domain.ServiceAdapterIfc;
import ch.olsen.servicecontainer.internalservice.auth.AuthInterface;
import ch.olsen.servicecontainer.internalservice.http.JettyService;
import ch.olsen.servicecontainer.internalservice.persistence.PersistenceSession;
import ch.olsen.servicecontainer.naming.OscURI;
import ch.olsen.servicecontainer.naming.OsnURI;
import ch.olsen.servicecontainer.service.HttpFiles;
import ch.olsen.servicecontainer.service.HttpServlet;
import ch.olsen.servicecontainer.service.PersistenceService;
import ch.olsen.servicecontainer.service.StartServiceException;

/**
 * The service container node contains services (both internal and user ones). Nodes are
 * interconnected and one node is elected as master node.
 * @author vito
 *
 */
@HttpFiles(path = "www/ch.olsen.servicecontainer.gwt.ServiceContainer", index = "ServiceContainer.html", isPublic = false)
public class SCNode implements SCNodeInterface, SCNodeInternalIfc {

    public static final String USERSESSIONCOOKIENAME = "SSOSESSIONID";

    String rmiName;

    SCNodeInterface stub;

    String masterNodeName;

    SCNodeInterface masterNodeStub;

    Map<String, SCNodeInterface> allNodes;

    RootDomain rootDomain;

    Map<String, LocalDomain> allLocalDomains = new HashMap<String, LocalDomain>();

    SCConfiguration cfg;

    Logger log;

    long scCounter = 0;

    SCNodeWeb webIfc;

    @PersistenceService
    public PersistenceSession db;

    public SCNode(String hostname, SCConfiguration cfg, String masterNode, String id) {
        this.cfg = cfg;
        try {
            System.setProperty("java.rmi.server.hostname", hostname);
            System.setProperty("mail.smtp.host", cfg.smtpServer.value());
            if (id == null) id = Random.getRandomString(4);
            LocateRegistry.createRegistry(cfg.rmiPort.value());
            rmiName = "//" + hostname + ":" + cfg.rmiPort.value() + "/SC" + id + "/";
            stub = (SCNodeInterface) UnicastRemoteObject.exportObject(this, 0);
            Naming.rebind(rmiName, stub);
            if (masterNode != null) {
                this.masterNodeName = masterNode;
                this.masterNodeStub = (SCNodeInterface) Naming.lookup(masterNode);
                this.allNodes = this.masterNodeStub.ping(rmiName, stub);
            } else {
                this.masterNodeStub = stub;
                this.masterNodeName = rmiName;
                this.allNodes = new HashMap<String, SCNodeInterface>();
                this.allNodes.put(rmiName, stub);
            }
            rootDomain = new RootDomain(this, new OscURI("osc:" + rmiName));
            rootDomain.init();
            ensureHasLogger();
            log.info("Service Container startup complete");
            System.err.println("Service Container startup complete");
        } catch (Exception e) {
            System.err.println("Could not start Service Container Node: " + e.getMessage());
            e.printStackTrace();
            shutdown();
        }
    }

    public Map<String, SCNodeInterface> ping(String rmiName, SCNodeInterface source) {
        this.allNodes.put(rmiName, source);
        return allNodes;
    }

    /**
	 * 1st argument: host name
	 * 2nd argument: configuration filename
	 * 3rd: master node osc URI
	 * 4th: node ID
	 * @param args
	 * @throws FileNotFoundException
	 */
    public static void main(String[] args) throws FileNotFoundException {
        if (args.length >= 2 && args[0].equals("shutdown")) {
            try {
                SCNodeInterface node = (SCNodeInterface) Naming.lookup(args[1]);
                node.shutdown();
            } catch (Exception e) {
                System.err.println("Could not stop service container: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (args.length < 3) {
            System.err.println("Usage: <java cmd> <hostname> <cfg file> <node id> <master node osc uri>\n" + "       <java cmd> shutdown <rmi name>");
            System.exit(1);
        } else {
            SCConfiguration cfg = new SCConfiguration();
            Configuration.loadFromXML(cfg, new FileInputStream(args[1]), false);
            new SCNode(args[0], cfg, args.length > 3 ? args[3] : null, args[2]);
        }
    }

    public SCDomain lookup(OsnURI uri) {
        ServiceAdapterIfc adapter;
        try {
            if (uri.getPathElements().length != 0 || isMasterNode()) {
                SCDomain local = allLocalDomains.get(uri.getURI());
                if (local != null) return local;
            }
            if (isMasterNode()) return rootDomain.lookup(uri);
            adapter = masterNodeStub.remoteLookup(uri);
            if (adapter != null) return new RemoteDomain(this, adapter, adapter.getUri());
        } catch (RemoteException e) {
            log.warn("Could not lookup remote service through master node: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Could not lookup remote service through master node: " + e.getMessage(), e);
        } catch (SecurityException e) {
            log.warn("Could not lookup remote service through master node: " + e.getMessage(), e);
        } catch (InstantiationException e) {
            log.warn("Could not lookup remote service through master node: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.warn("Could not lookup remote service through master node: " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            log.warn("Could not lookup remote service through master node: " + e.getMessage(), e);
        }
        return null;
    }

    public SCDomain lookup(OscURI uri) {
        if (uri.getSCNodeName().equals(rmiName)) {
            LocalDomain domain = allLocalDomains.get(uri.toOsnURI().getURI());
            if (domain != null) return domain;
        } else {
            SCNodeInterface node = allNodes.get(uri.getSCNodeName());
            if (node != null) {
                ServiceAdapterIfc adapter;
                try {
                    adapter = node.remoteLookup(uri);
                    if (adapter != null) return new RemoteDomain(this, adapter, uri);
                } catch (RemoteException e) {
                    log.warn("Could not lookup remote domain using direct osc uri: " + e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                    log.warn("Could not lookup remote domain using direct osc uri: " + e.getMessage(), e);
                } catch (SecurityException e) {
                    log.warn("Could not lookup remote domain using direct osc uri: " + e.getMessage(), e);
                } catch (InstantiationException e) {
                    log.warn("Could not lookup remote domain using direct osc uri: " + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    log.warn("Could not lookup remote domain using direct osc uri: " + e.getMessage(), e);
                } catch (InvocationTargetException e) {
                    log.warn("Could not lookup remote domain using direct osc uri: " + e.getMessage(), e);
                }
            }
        }
        return lookup(uri.toOsnURI());
    }

    public ServiceAdapterIfc remoteLookup(OscURI uri) throws RemoteException {
        SCDomain domain = lookup(uri);
        if (domain != null) return domain.getAdapter();
        return null;
    }

    public ServiceAdapterIfc remoteLookup(OsnURI uri) throws RemoteException {
        SCDomain domain = lookup(uri);
        if (domain != null) return domain.getAdapter();
        return null;
    }

    public String getRmiName() {
        return rmiName;
    }

    public final RootDomain getRootDomain() {
        return rootDomain;
    }

    public void registerLocalService(LocalDomain localDomain) {
        allLocalDomains.put(localDomain.getOsnUri().getURI(), localDomain);
    }

    public void unregisterLocalService(LocalDomain localDomain) {
        allLocalDomains.remove(localDomain.getOsnUri().getURI());
    }

    public double getLoadCount() {
        return rootDomain.getLoadBalancer().getLoadCount();
    }

    public final Map<String, LocalDomain> getAllLocalDomains() {
        return allLocalDomains;
    }

    public final Map<String, SCNodeInterface> getAllNodes() {
        return allNodes;
    }

    /**
	 * after the load balancer has decided the node (we are the selected node)
	 * @throws SessionException 
	 */
    public ServiceAdapterIfc startService(String session, OsnURI uri, ServiceAdapterIfc parentServiceAdapter, String serviceClass, String serviceInterface, ClassLoader classLoader, boolean isRestore) throws RemoteException, StartServiceException, SessionException {
        ensureHasLogger();
        log.info("Starting new service: " + uri + " class: " + serviceClass);
        if (classLoader == null) classLoader = rootDomain.classLoader;
        try {
            OscURI oscUri = uri.toOscUri(rmiName);
            Class<?> clazz = classLoader.loadClass(serviceClass);
            Object service = clazz.newInstance();
            Class<?> ifc = classLoader.loadClass(serviceInterface);
            RemoteDomain remoteDomain = new RemoteDomain(this, parentServiceAdapter, parentServiceAdapter.getUri());
            LocalDomain domain = new LocalDomain(session, this, remoteDomain, oscUri, classLoader, service, ifc, isRestore);
            return domain.runningService.adapter;
        } catch (SessionException e) {
            throw e;
        } catch (Exception e) {
            throw new StartServiceException("Could not start remote service: " + e.getMessage(), e);
        }
    }

    private void ensureHasLogger() {
        if (log == null) log = rootDomain.getLoggerService().getLogger(rootDomain, "SCNode");
    }

    /**
	 * before the load balancer
	 * @throws AuthSessionException 
	 */
    public ServiceAdapterIfc startService(String session, String name, String serviceClass, String serviceInterface, ClassLoader classLoader) throws RemoteException, StartServiceException, SessionException {
        SCDomain domain;
        if (isMasterNode()) {
            domain = rootDomain.startSubService(session, serviceClass, serviceInterface, name, classLoader);
            return domain.getAdapter();
        } else return masterNodeStub.startService(session, name, serviceClass, serviceInterface, classLoader);
    }

    public ServiceAdapterIfc deploy(String session, String name, URL jarPath[], String serviceClass, String serviceInterface) throws RemoteException, MalformedURLException, StartServiceException, SessionException {
        SCClassLoader cl = new SCClassLoader(jarPath, getMasterNode().getSCClassLoaderCounter());
        return startService(session, name, serviceClass, serviceInterface, cl);
    }

    public ServiceAdapterIfc deploy(String session, String name, byte jarBytes[], String jarName, String serviceClass, String serviceInterface) throws RemoteException, MalformedURLException, StartServiceException, SessionException {
        try {
            File jarFile = new File(jarName);
            jarName = jarFile.getName();
            String jarName2 = jarName;
            jarFile = new File(jarName2);
            int n = 0;
            while (jarFile.exists()) {
                jarName2 = jarName + n++;
                jarFile = new File(jarName2);
            }
            FileOutputStream fos = new FileOutputStream(jarName2);
            IOUtils.copy(new ByteArrayInputStream(jarBytes), fos);
            SCClassLoader cl = new SCClassLoader(new URL[] { new URL("file://" + jarFile.getAbsolutePath()) }, getMasterNode().getSCClassLoaderCounter());
            return startService(session, name, serviceClass, serviceInterface, cl);
        } catch (SessionException e) {
            throw e;
        } catch (Exception e) {
            throw new StartServiceException("Could not deploy service: " + e.getMessage(), e);
        }
    }

    public static Object deployNewService(String scNodeRmiName, String userName, String password, String name, URL jarPath[], String serviceClass, String serviceInterface, Logger log) throws RemoteException, MalformedURLException, StartServiceException, NotBoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, SessionException {
        SCNodeInterface node = (SCNodeInterface) Naming.lookup(scNodeRmiName);
        String session = node.login(userName, password);
        ServiceAdapterIfc adapter = node.deploy(session, name, jarPath, serviceClass, serviceInterface);
        if (adapter != null) {
            return new ExternalDomain(node, adapter, adapter.getUri(), log).getProxy(Thread.currentThread().getContextClassLoader());
        }
        return null;
    }

    /**
	 * this method uses a single jar file as input and sends the bytes to the service container
	 */
    public static Object deployNewService(String scNodeRmiName, String userName, String password, String name, String jarName, String serviceClass, String serviceInterface, Logger log) throws RemoteException, MalformedURLException, StartServiceException, NotBoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, SessionException {
        try {
            SCNodeInterface node = (SCNodeInterface) Naming.lookup(scNodeRmiName);
            String session = node.login(userName, password);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(new FileInputStream(jarName), baos);
            ServiceAdapterIfc adapter = node.deploy(session, name, baos.toByteArray(), jarName, serviceClass, serviceInterface);
            if (adapter != null) {
                return new ExternalDomain(node, adapter, adapter.getUri(), log).getProxy(Thread.currentThread().getContextClassLoader());
            }
        } catch (Exception e) {
            log.warn("Could not send deploy command: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean isMasterNode() {
        return masterNodeStub == stub;
    }

    public SCNodeInterface getMasterNode() {
        return masterNodeStub;
    }

    public String getMasterNodeName() {
        return masterNodeName;
    }

    public void registerDomain(OscURI oscURI, ServiceAdapterIfc adapter) {
        if (isMasterNode()) {
            ensureHasLogger();
            try {
                log.info("Registering remote domain on master node: " + oscURI);
                RemoteDomain domain = new RemoteDomain(this, adapter, oscURI);
                if (oscURI.getPathElements().length == 3) {
                    rootDomain.addSubService(oscURI, domain);
                }
                rootDomain.getFaultDetectionModule().addSubdomain(domain, oscURI);
            } catch (Exception e) {
                log.warn("Could not register domain: " + e.getMessage(), e);
            }
        } else {
            try {
                getMasterNode().registerDomain(oscURI, adapter);
            } catch (RemoteException e) {
                log.warn("Could not register domain on master node: " + e.getMessage(), e);
            }
        }
    }

    /**
	 * Tells the fault detection service
	 */
    public void unregisterDomain(OscURI oscURI) {
        if (isMasterNode()) {
            rootDomain.getFaultDetectionModule().removeSubdomain(oscURI);
        } else {
            try {
                getMasterNode().unregisterDomain(oscURI);
            } catch (RemoteException e) {
                log.warn("Could not register domain on master node: " + e.getMessage(), e);
            }
        }
    }

    public SCConfiguration getCfg() {
        return cfg;
    }

    public synchronized long getSCClassLoaderCounter() throws RemoteException {
        return scCounter++;
    }

    @HttpServlet(url = "scnode")
    public void handleServlet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        synchronized (this) {
            if (webIfc == null) {
                webIfc = new SCNodeWeb(this);
                JettyService.initializeServlet(webIfc, log, "www/ch.olsen.servicecontainer.gwt.ServiceContainer", Thread.currentThread().getContextClassLoader());
            }
        }
        webIfc.service(request, response);
        response.getWriter().flush();
    }

    public static String getSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String session = null;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(USERSESSIONCOOKIENAME)) {
                    session = cookies[i].getValue();
                    break;
                }
            }
        }
        return session;
    }

    Map<String, String> uploadedFiles = new HashMap<String, String>();

    @HttpServlet(url = "upload")
    public void handleFileUpload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Object id = request.getParameter("id");
            if (id == null) {
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("<p>This servlet needs a unique id as parameter</p>");
                return;
            }
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (isMultipart) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                try {
                    List items = upload.parseRequest(request);
                    Iterator iter = items.iterator();
                    while (iter.hasNext()) {
                        FileItem item = (FileItem) iter.next();
                        if (item.isFormField()) {
                        } else if (item.getFieldName().equals("jar")) {
                            String fileName = "SCUpload-" + item.getName();
                            int n = 0;
                            while (new File(fileName).exists()) {
                                fileName = "SCUpload-" + item.getName() + "." + n++;
                            }
                            FileOutputStream fos = new FileOutputStream(fileName);
                            Streams.copy(item.getInputStream(), fos, true, new byte[4096]);
                            uploadedFiles.put(id.toString(), fileName);
                        }
                    }
                } catch (FileUploadException e) {
                    e.printStackTrace();
                }
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                PrintWriter w = response.getWriter();
                w.println("<html><head><title>File Upload</title><head><body style=\"font-size: smaller;\">");
                w.println("File uploaded successfully. Please wait for outer application to be notified...");
                w.println("</body></html>");
            } else {
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                PrintWriter w = response.getWriter();
                Object poll = request.getParameter("poll");
                if (poll != null) {
                    String file = uploadedFiles.get(id);
                    if (file != null) {
                        w.print(file);
                    } else w.print("0");
                } else {
                    w.println("<html><head><title>File Upload</title><head><body style=\"font-size: smaller;\">");
                    w.println("<form method=\"post\" enctype=\"multipart/form-data\" action=\"#\">");
                    w.println("<input type=\"file\" name=\"jar\" size=\"40\">");
                    w.println("<input type=\"submit\" value=\"Upload\"></form>");
                    w.println("</body></html>");
                }
            }
        } finally {
            response.getWriter().flush();
        }
    }

    @HttpServlet(url = "log")
    public void handleWebLog(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        rootDomain.logger.handleWebLog(request, response);
    }

    public void shutdown() {
        if (rootDomain != null) rootDomain.shutdown();
        if (stub != null) {
            try {
                UnicastRemoteObject.unexportObject(this, false);
                Naming.unbind(rmiName);
            } catch (Exception e) {
                System.err.println("Could not unexport object during finalize: " + e.getMessage());
            }
        }
        new Timer("shutdown").schedule(new TimerTask() {

            public void run() {
                System.err.println("Service Container is shutting down due to a shutdown request");
                Runtime.getRuntime().exit(0);
            }
        }, 5000);
    }

    public String login(String username, String password) {
        AuthInterface auth = rootDomain.getAuthService();
        String hash = AuthInterface.Encrtypt.enctrypt(password);
        String session = auth.login(username, hash);
        return session;
    }

    public static String printThreadInfos() {
        return printThreadInfos(true, "\n");
    }

    public static String printThreadInfos(boolean all, String newline) {
        String ret = "";
        Map<Thread, StackTraceElement[]> st = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> e : st.entrySet()) {
            StackTraceElement[] el = e.getValue();
            Thread t = e.getKey();
            if (!all && (t.getState() == Thread.State.TIMED_WAITING || t.getState() == Thread.State.WAITING)) continue;
            ret += "\"" + t.getName() + "\"" + " " + (t.isDaemon() ? "daemon" : "") + " prio=" + t.getPriority() + " Thread id=" + t.getId() + " " + t.getState() + "\n";
            for (StackTraceElement line : el) {
                ret += "\t" + line + "\n";
            }
            ret += "\n";
        }
        if (!newline.equals("\n")) ret = ret.replace("\n", newline);
        return ret;
    }

    /**
	 * upgrade the service at osnUri and all its children with the provided jar file
	 * @param session
	 * @param osnURI
	 * @param fileName
	 * @throws UpgradeException 
	 */
    public void upgradeServices(String session, OsnURI osnUri, String fileName) throws SessionException, UpgradeException {
        if (isMasterNode()) {
            if (getRootDomain().getAuthService().checkAccess(session, AuthInterface.OWNER, osnUri, "") != null) {
                getRootDomain().getFaultDetectionModule().upgrade(session, osnUri, fileName);
            } else {
                throw new SessionException("Not enough rights");
            }
        } else {
            log.error("upgradeService dispatch to master node not implemented");
        }
    }
}
