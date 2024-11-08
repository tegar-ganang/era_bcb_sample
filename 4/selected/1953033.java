package org.dbe.servent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.dbe.servent.core.ComponentManager;
import org.dbe.servent.http.ServentRequest;
import org.dbe.servent.http.ServentResponse;
import org.dbe.servent.p2p.P2PDirectory;
import org.dbe.servent.service.ServiceInfo;
import org.dbe.servent.service.ServiceWrapper;
import org.dbe.toolkit.pa.DBEPropertyKeys;
import org.dbe.toolkit.pa.di.PAFactory;
import org.dbe.toolkit.proxyframework.ServiceProxy;
import org.dbe.toolkit.proxyframework.ui.UIFactory;
import org.sun.dbe.RemoteCall;

/**
 * 
 * @author bob
 * @author juanjobnt
 */
public class CSSHandler extends GenericServentHandler {

    /** http header: service-url */
    public static final String NEW_URL_HEADER = "service-url";

    SearcherImpl searcher = null;

    public Searcher getSearcher() {
        if (searcher == null) {
            searcher = new SearcherImpl(context);
        }
        return searcher;
    }

    /**
	 * @see org.dbe.servent.http.ServentHandler#getName()
	 */
    public String getName() {
        return "CSS Handler";
    }

    /**
	 * @see org.dbe.servent.http.ServentHandler#handle(java.lang.String,
	 *      java.lang.String, org.dbe.servent.http.ServentRequest,
	 *      org.dbe.servent.http.ServentResponse)
	 */
    public void handle(String command, String pathParams, ServentRequest request, ServentResponse response) throws ServentException, IOException {
        logger.debug(" >> CSS Call: " + command + " (" + request.getMethod() + ")");
        if (CssCommands.CALL_COMMAND.equals(command)) {
            doCall(request, response);
        } else if (CssCommands.FIND_COMMAND.equals(command)) {
            doFind(request, response);
        } else if (CssCommands.FIND_PROXIES_COMMAND.equals(command)) {
            doFindProxies(request, response);
        } else if (CssCommands.OFFLINE_SEARCH_COMMAND.equals(command)) {
            if (request.getMethod().equals("POST")) {
                doJustFind(request, response);
            } else {
                forbidden(request, response);
            }
        } else if (CssCommands.RETRIEVE_COMMAND.equals(command)) {
            doRetrieve(request, response);
        } else if (CssCommands.SEARCH_WAIT_COMMAND.equals(command)) {
            doSearchWait(request, response);
        } else if (CssCommands.FINISHED_SEARCH_COMMAND.equals(command)) {
            doIsFinishedSearch(request, response);
        } else if (command.startsWith(CssCommands.SHOW_COMMAND)) {
            doShow(command.substring(CssCommands.SHOW_COMMAND.length()), request, response);
        } else if (command.equals(CssCommands.PROXY_COMMAND)) {
            doProxy(request, response);
        } else {
            logger.warn("CSS call recived was not valid: " + command);
            throw new IOException("CSS call recived was not valid: " + command);
        }
    }

    private void doSearchWait(ServentRequest request, ServentResponse response) throws IOException {
        long before = System.currentTimeMillis();
        String searchId = request.getParameter("searchId");
        Serializable[] results = null;
        try {
            getSearcher();
            results = searcher.waitForSearchId(searchId);
            if (results.length > 0) {
                serveObject(results[0], request, response);
            } else {
                response.setStatus(200);
                response.setContentLength(0);
            }
        } catch (ServerServentException e) {
            internalServerError(request, response);
        } catch (NoSuchServiceException e) {
            notFound(request, response);
        }
        long after = System.currentTimeMillis();
    }

    private void serveObject(Serializable serializable, ServentRequest request, ServentResponse response) throws IOException {
        response.setStatus(200);
        if (serializable == null) {
            response.setContentLength(0);
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.close();
        byte[] result = baos.toByteArray();
        response.setContentLength(result.length);
        response.getOutputStream().write(result);
    }

    private void internalServerError(ServentRequest request, ServentResponse response) throws IOException {
        StringBuffer sb = new StringBuffer("<html><body><p>The resource ");
        sb.append(request.getPath());
        sb.append(" was not found on this server</p></body></html>");
        byte[] message = sb.toString().getBytes();
        response.setStatus(500);
        response.setContentLength(message.length);
        response.getOutputStream().write(message);
    }

    private void notFound(ServentRequest request, ServentResponse response) throws IOException {
        StringBuffer sb = new StringBuffer("<html><body><p>The resource ");
        sb.append(request.getPath());
        sb.append(" was not found on this server</p></body></html>");
        byte[] message = sb.toString().getBytes();
        response.setStatus(404);
        response.setContentLength(message.length);
        response.getOutputStream().write(message);
    }

    private void forbidden(ServentRequest request, ServentResponse response) throws IOException {
        StringBuffer sb = new StringBuffer("<html><body><p>You can't do a GET on the resource ");
        sb.append(request.getPath());
        sb.append("</p></body></html>");
        byte[] message = sb.toString().getBytes();
        response.setStatus(403);
        response.setContentLength(message.length);
        response.getOutputStream().write(message);
    }

    private void doIsFinishedSearch(ServentRequest request, ServentResponse response) throws IOException {
        String searchId = request.getParameter("searchId");
        boolean finished = false;
        try {
            getSearcher();
            finished = searcher.doIsFinished(searchId);
        } catch (Throwable th) {
            notFound(request, response);
            return;
        }
        byte[] booleanBytes = Boolean.toString(finished).getBytes();
        response.setContentLength(booleanBytes.length);
        OutputStream os = response.getOutputStream();
        os.write(booleanBytes);
    }

    /**
	 * Call the real Service endpoint
	 * 
	 * @param request
	 *            ServentRequest
	 * @param response
	 *            ServentResponse
	 * @throws IOException
	 */
    private void doCall(ServentRequest request, ServentResponse response) throws IOException {
        String newUrl = request.getHeader(NEW_URL_HEADER);
        URL destination = new URL(newUrl);
        byte[] body = request.getBody();
        logger.info("Calling to SSS " + newUrl);
        RemoteCall rc = new RemoteCall(destination, body);
        byte[] buffer = rc.perform();
        if (buffer != null) {
            logger.debug("CSS return Content-Length: " + buffer.length);
            response.setContentLength(buffer.length);
            response.getOutputStream().write(buffer);
        } else {
            logger.debug("Unable to call the SSS at url " + newUrl);
            response.sendError("Internal server Error. The query returned null");
        }
    }

    /**
	 * Find a service Using the Fada Network
	 * 
	 * @param request
	 *            ServentRequest
	 * @param response
	 *            ServentResponse
	 * @throws IOException
	 */
    private void doFind(ServentRequest request, ServentResponse response) throws IOException, ServentException {
        logger.debug("CSSHandler.doFind(): getting entries");
        long theTimeout = getTimeout(request);
        int theHops = getHops(request);
        try {
            String[] theEntries = getEntries(request);
            getSearcher();
            ServiceProxy proxy = searcher.getServiceProxy(request, theEntries, theTimeout, theHops);
            ObjectOutputStream oos = new ObjectOutputStream(response.getOutputStream());
            oos.writeObject(proxy);
            oos.close();
        } catch (Exception e) {
            logger.error("Exception when the client was finding a service, we assume the service was not found", e);
            throw new NoSuchServiceException("service not found");
        }
    }

    /**
	 * Find a service Using the Fada Network
	 * 
	 * @param request
	 *            ServentRequest
	 * @param response
	 *            ServentResponse
	 * @throws IOException
	 */
    private void doFindProxies(ServentRequest request, ServentResponse response) throws IOException, ServentException {
        logger.debug("CSSHandler.doFindProxies(): getting entries");
        String myEntries = request.getParameter("entries");
        logger.debug("CSSHandler.doFindProxies(): entries obtained");
        long theTimeout = getTimeout(request);
        int theHops = getHops(request);
        String[] theEntries = getEntries(request);
        try {
            List proxies = getServiceProxies(request, theEntries, theTimeout, theHops);
            writeProxies(response, proxies);
        } catch (Exception e) {
            logger.error("Exception when the client was finding a service, we assume the service was not found", e);
            throw new NoSuchServiceException("service " + myEntries + " not found");
        }
    }

    /**
	 * @param response
	 * @param proxies
	 * @throws IOException
	 */
    private void writeProxies(ServentResponse response, List proxies) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(response.getOutputStream());
        oos.writeObject(proxies);
        oos.close();
    }

    /**
	 * return true if entries match
	 * 
	 * @return
	 */
    private boolean matchEntries(String[] allEntries, String[] searchFor) {
        boolean match = true;
        for (int i = 0; i < searchFor.length; i++) {
            boolean oneMatch = false;
            String searched = searchFor[i];
            if (searched == null) {
                continue;
            }
            for (int j = 0; j < allEntries.length; j++) {
                if (searched.equals(allEntries[j])) {
                    oneMatch = true;
                    break;
                }
            }
            if (oneMatch == false) {
                match = false;
                break;
            }
        }
        return match;
    }

    /**
	 * @param myEntries
	 * @return
	 * @throws IOException
	 */
    private String[] getEntries(ServentRequest request) throws IOException {
        String myEntries = request.getParameter("entries");
        logger.debug("CSSHandler.doFind(): entries obtained");
        if (myEntries == null) {
            throw new IOException("Entries are needed but they where not found");
        }
        Collection colEntries = new ArrayList();
        StringTokenizer entries = new StringTokenizer(myEntries, "|", false);
        while (entries.hasMoreElements()) {
            String newEntry = entries.nextToken();
            colEntries.add(newEntry);
        }
        String[] theEntries = (String[]) colEntries.toArray(new String[0]);
        if (theEntries == null) {
            logger.debug("CSSHandler.doFind(): no entries found");
        } else if (theEntries.length == 0) {
            logger.debug("CSSHandler.doFind(): size of entries is 0");
        } else {
            logger.debug("CSSHandler.doFind(): the entries are the following");
            for (int i = 0; i < theEntries.length; i++) {
                logger.debug(theEntries[i]);
            }
        }
        return theEntries;
    }

    /**
	 * @param request
	 * @return
	 */
    private long getTimeout(ServentRequest request) {
        long theTimeout = -1;
        try {
            theTimeout = Long.parseLong(request.getParameter("timeout"));
        } catch (Exception ex) {
            theTimeout = -1;
        }
        return theTimeout;
    }

    private int getHops(ServentRequest request) {
        int theHops = -1;
        try {
            theHops = Integer.parseInt(request.getParameter("hops"));
        } catch (Exception ex) {
            theHops = -1;
        }
        return theHops;
    }

    private void doJustFind(ServentRequest request, ServentResponse response) throws IOException {
        logger.debug("CSSHandler.doJustFind(): getting entries");
        InputStream requestIs = request.getInputStream();
        Properties props = new Properties();
        props.load(requestIs);
        String myEntries = props.getProperty("entries");
        logger.debug("CSSHandler.doJustFind(): entries obtained");
        String timeout = props.getProperty("timeout");
        long theTimeout = -1;
        try {
            theTimeout = Long.parseLong(timeout);
        } catch (Exception ex) {
            theTimeout = -1;
        }
        String hops = props.getProperty("hops");
        int theHops = -1;
        try {
            theHops = Integer.parseInt(hops);
        } catch (Exception ex) {
            theHops = -1;
        }
        if (myEntries == null) {
            throw new IOException("Entries are needed but they where not found");
        }
        Collection colEntries = new ArrayList();
        StringTokenizer entries = new StringTokenizer(myEntries, "|", false);
        while (entries.hasMoreElements()) {
            String newEntry = entries.nextToken();
            colEntries.add(newEntry);
        }
        String[] theEntries = (String[]) colEntries.toArray(new String[0]);
        if (theEntries == null) {
            logger.debug("CSSHandler.doJustFind(): no entries found");
        } else if (theEntries.length == 0) {
            logger.debug("CSSHandler.doJustFind(): size of entries is 0");
        } else {
            logger.debug("CSSHandler.doJustFind(): the entries are the following");
            for (int i = 0; i < theEntries.length; i++) {
                logger.debug(theEntries[i]);
            }
        }
        getSearcher();
        String searchId = null;
        try {
            searchId = searcher.doJustFind(request, theTimeout, theHops, theEntries);
        } catch (ServerServentException e) {
            internalServerError(request, response);
            return;
        }
        OutputStream os = response.getOutputStream();
        byte[] searchIdBytes = searchId.getBytes();
        response.setContentLength(searchIdBytes.length);
        os.write(searchIdBytes);
    }

    /**
	 * Retrieve the results of a background search
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
    private void doRetrieve(ServentRequest request, ServentResponse response) throws IOException {
        String searchId = null;
        searchId = request.getParameter("searchId");
        getSearcher();
        ServiceProxy proxy = searcher.doRetrieve(searchId);
        if (proxy == null) {
            response.setStatus(404);
            response.setContentLength(0);
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(proxy);
            oos.close();
            response.setContentLength(baos.size());
            baos.writeTo(response.getOutputStream());
        }
    }

    private static final String TEMP_APPLET = "<h1>DBE Applet</h1>\n\n" + "<applet \n" + "code=\"org.dbe.toolkit.proxyframework.AppletViewer\"\n" + "archive=\"http://@host@:@port@/CLIENT/proxy?url=@sss@&service=@service@,http://@host@:@port@/CLIENT/show/ui_applet.jar\"\n" + "width=\"1\"" + "height=\"1\">\n" + "<param name=\"uiFactory\" value=\"@uiFactory@\" />\n" + "<param name=\"css\" value=\"http://@host@:@port@\" />\n" + "<param name=\"sss\" value=\"@sss@\" />\n" + "<param name=\"service\" value=\"@service@\" />\n" + "<param name=\"userId\" value=\"@userId@\" />\n" + "</applet>\n<br><br><hr><br>" + "* An applet has popup if there was no error. If you cannot see this applet, please check the error in your JVM Console";

    private byte[] byUIJarFile = null;

    /**
	 * @param request
	 * @param response
	 * @throws IOException
	 */
    private void doShow(String command, ServentRequest request, ServentResponse response) throws IOException {
        try {
            if ("/ui_applet.jar".equals(command)) {
                if (byUIJarFile == null) {
                    File root = new File(context.getConfig().getRootPath());
                    File jarFile = new File(root, "conf" + File.separator + "ui_applet.jar");
                    byUIJarFile = new byte[(int) jarFile.length()];
                    FileInputStream fis = new FileInputStream(jarFile);
                    for (int i = 0; i < byUIJarFile.length; i++) {
                        byUIJarFile[i] = (byte) fis.read();
                    }
                }
                response.setContentLength((int) byUIJarFile.length);
                response.getOutputStream().write(byUIJarFile);
                return;
            }
            String myEntries = request.getParameter("entries");
            if (myEntries == null) {
                throw new IOException("Entries are needed but they where not found");
            }
            Collection colEntries = new ArrayList();
            StringTokenizer entries = new StringTokenizer(myEntries, "|", false);
            while (entries.hasMoreElements()) {
                String newEntry = entries.nextToken();
                colEntries.add(newEntry);
            }
            String[] theEntries = (String[]) colEntries.toArray(new String[0]);
            getSearcher();
            ServiceProxy proxy = searcher.getServiceProxy(request, theEntries, -1, -1);
            if (proxy != null) {
                UIFactory uifactory = proxy.getUIFactory();
                String ret = TEMP_APPLET.replaceAll("@uiFactory@", uifactory.getClass().getName());
                ret = ret.replaceAll("@css@", ServentInfo.getInstance().getPrivateURL().toString());
                ret = ret.replaceAll("@sss@", ServentInfo.getInstance().getPublicURL().toString());
                ret = ret.replaceAll("@service@", theEntries[0]);
                ret = ret.replaceAll("@userId@", "Im.a.dbe.user");
                ret = ret.replaceAll("@host@", request.getHost());
                ret = ret.replaceAll("@port@", Integer.toString(request.getPort()));
                response.setContentLength(ret.length());
                response.getOutputStream().write(ret.getBytes());
                response.getOutputStream().close();
            } else {
                logger.error("Proxy for service " + theEntries[0] + " is null");
            }
        } catch (Exception e) {
            logger.error("Exception when the client was finding a service", e);
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * @param theEntries
	 * @return
	 * @throws ServerServentException
	 */
    private ServiceProxy getServiceProxy(ServentRequest request, String[] theEntries) throws ServerServentException {
        return getServiceProxy(request, theEntries, -1, -1);
    }

    private List getServiceProxies(ServentRequest request, String[] theEntries) throws ServerServentException {
        return getServiceProxies(request, theEntries, -1, -1);
    }

    private List getServiceProxies(ServentRequest request, String[] theEntries, long timeout, int hops) throws ServerServentException {
        logger.debug("getServiceProxy()");
        List proxies = new ArrayList();
        List internalServices = new ArrayList();
        for (Iterator it = context.getRepository().getWrappers().iterator(); it.hasNext(); ) {
            ServiceWrapper wrapper = (ServiceWrapper) it.next();
            String[] wrapperEntries = wrapper.getServiceInfo().getEntries();
            if (matchEntries(wrapperEntries, theEntries)) {
                internalServices.add(wrapper);
            }
        }
        ServiceProxy proxy = null;
        if (internalServices.size() > 0) {
            logger.info("CSS : internal services...");
            for (int size = 0; size < internalServices.size(); size++) {
                ServiceWrapper wrapper = (ServiceWrapper) internalServices.get(size);
                ServiceInfo info = wrapper.getServiceInfo();
                String portalID = getPortalID();
                String serviceURL = "http://" + request.getHost() + ":" + ServentInfo.getInstance().getPublicPort() + "/" + info.getId();
                logger.info("setting real endpoint to " + serviceURL);
                Map serventConfig = new HashMap();
                serventConfig.put(DBEPropertyKeys.SERVICE_UI_FACTORY, info.getConfiguration().getFadaProxyClassName());
                serventConfig.put(DBEPropertyKeys.SERVICE_PROTOCOL, PAFactory.OBJECT_SERIALISATION);
                serventConfig.put(DBEPropertyKeys.SERVICE_ENDPOINT, serviceURL);
                serventConfig.put(DBEPropertyKeys.CODEBASE, serviceURL + "/CODEBASE");
                serventConfig.put("PORTAL_ID", portalID);
                serventConfig.put(Servent.SERVICE_ID, info.getServiceId());
                ServiceConfiguration serviceConfig = info.getConfiguration();
                Collection uis = serviceConfig.getServiceUIs();
                if (uis != null) {
                    Iterator iter = uis.iterator();
                    while (iter.hasNext()) {
                        ServiceConfiguration.ServiceUI ui = (ServiceConfiguration.ServiceUI) iter.next();
                        if ("Open_Laszlo".equals(ui.getUi())) {
                            serventConfig.put("UI_URL", serviceURL + "/" + ui.getUiPath());
                        }
                    }
                }
                String[] complexTypes = serviceConfig.getComplexTypes();
                if (complexTypes != null) {
                    String complexValue = "";
                    for (int i = 0; i < complexTypes.length; i++) {
                        complexValue = complexValue + complexTypes[i] + "//";
                        logger.info("adding complex type " + complexTypes[i] + " to config...");
                    }
                    serventConfig.put(DBEPropertyKeys.COMPLEX_TYPES, complexValue);
                }
                proxy = new ServiceProxy(serventConfig);
                proxies.add(proxy);
            }
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ComponentManager componentManager = context.getComponentManager();
        P2PDirectory directory = componentManager.getP2PDirectory();
        Serializable results[] = directory.lookup(theEntries, classLoader);
        if (results != null) {
            for (int size = 0; size < results.length; size++) {
                if (!remoteProxyMatchesLocalProxies((ServiceProxy) results[size], proxies)) {
                    proxies.add((ServiceProxy) results[size]);
                }
            }
        }
        return proxies;
    }

    private boolean remoteProxyMatchesLocalProxies(ServiceProxy remoteProxy, List localProxies) {
        for (Iterator iter = localProxies.iterator(); iter.hasNext(); ) {
            ServiceProxy localProxy = (ServiceProxy) iter.next();
            if (remoteProxyMatchesLocalProxy(remoteProxy, localProxy)) {
                return true;
            }
        }
        return false;
    }

    private boolean remoteProxyMatchesLocalProxy(ServiceProxy remoteProxy, ServiceProxy localProxy) {
        Map remoteProperties = remoteProxy.getProperties();
        Map localProperties = localProxy.getProperties();
        String remoteId = (String) remoteProperties.get(org.dbe.servent.Servent.SERVICE_ID);
        String localId = (String) localProperties.get(org.dbe.servent.Servent.SERVICE_ID);
        if ((localId == null) || (remoteId == null)) {
            return false;
        }
        return localId.equals(remoteId);
    }

    /**
	 * @param theEntries
	 * @return
	 * @throws ServerServentException
	 */
    private ServiceProxy getServiceProxy(ServentRequest request, String[] theEntries, long timeout, int hops) throws ServerServentException {
        logger.debug("getServiceProxy()");
        ServiceWrapper internalService = null;
        for (Iterator it = context.getRepository().getWrappers().iterator(); it.hasNext(); ) {
            ServiceWrapper wrapper = (ServiceWrapper) it.next();
            String[] wrapperEntries = wrapper.getServiceInfo().getEntries();
            if (matchEntries(wrapperEntries, theEntries)) {
                internalService = wrapper;
                break;
            }
        }
        ServiceProxy proxy = null;
        if (internalService != null) {
            ServiceInfo info = internalService.getServiceInfo();
            String portalID = getPortalID();
            String serviceURL = "http://" + request.getHost() + ":" + ServentInfo.getInstance().getPublicPort() + "/" + info.getId();
            logger.info("setting real endpoint to " + serviceURL);
            Map serventConfig = new HashMap();
            serventConfig.put(DBEPropertyKeys.SERVICE_UI_FACTORY, info.getConfiguration().getFadaProxyClassName());
            serventConfig.put(DBEPropertyKeys.SERVICE_PROTOCOL, PAFactory.OBJECT_SERIALISATION);
            serventConfig.put(DBEPropertyKeys.SERVICE_ENDPOINT, serviceURL);
            serventConfig.put(DBEPropertyKeys.CODEBASE, serviceURL + "/CODEBASE");
            serventConfig.put("PORTAL_ID", portalID);
            ServiceConfiguration serviceConfig = info.getConfiguration();
            Collection uis = serviceConfig.getServiceUIs();
            String[] complexTypes = serviceConfig.getComplexTypes();
            if (uis != null) {
                Iterator iter = uis.iterator();
                while (iter.hasNext()) {
                    ServiceConfiguration.ServiceUI ui = (ServiceConfiguration.ServiceUI) iter.next();
                    if ("Open_Laszlo".equals(ui.getUi())) {
                        serventConfig.put("UI_URL", serviceURL + "/" + ui.getUiPath());
                    }
                }
            }
            if (complexTypes != null) {
                if (complexTypes != null) {
                    String complexValue = "";
                    for (int i = 0; i < complexTypes.length; i++) {
                        complexValue = complexValue + complexTypes[i] + "//";
                        logger.info("adding complex type " + complexTypes[i] + " to config...");
                    }
                    serventConfig.put(DBEPropertyKeys.COMPLEX_TYPES, complexValue);
                }
            }
            proxy = new ServiceProxy(serventConfig);
        } else {
            proxy = null;
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Serializable results[] = context.getComponentManager().getP2PDirectory().lookup(theEntries, classLoader);
            if (results != null) {
                proxy = (ServiceProxy) results[0];
            }
        }
        return proxy;
    }

    private String getPortalID() {
        Collection wrappers = this.context.getRepository().getWrappers();
        Iterator iter = wrappers.iterator();
        while (iter.hasNext()) {
            ServiceWrapper wrapper = (ServiceWrapper) iter.next();
            String name = wrapper.getName();
            String smid = wrapper.getSmid();
            if ("PortalService".equals(name)) {
                return smid;
            }
        }
        return null;
    }

    /**
	 * Tunneling using the url and service parameters
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
    private void doProxy(ServentRequest request, ServentResponse response) throws IOException {
        String url = request.getParameter("url");
        String service = request.getParameter("service");
        if ((url != null) && (service != null)) {
            HttpURLConnection conn = null;
            try {
                URL codebaseUrl = new URL(url + "/" + service + "/CODEBASE");
                conn = (HttpURLConnection) codebaseUrl.openConnection();
                InputStream is = conn.getInputStream();
                response.setContentLength(conn.getContentLength());
                for (int i = 0; i < conn.getContentLength(); i++) {
                    response.getOutputStream().write((byte) is.read());
                }
                response.getOutputStream().close();
            } catch (MalformedURLException e) {
                response.sendError(e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                conn.disconnect();
            }
        }
    }
}
