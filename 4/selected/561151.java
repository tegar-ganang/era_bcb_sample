package com.webdeninteractive.sbie.v1_1.service;

import com.webdeninteractive.sbie.Service;
import com.webdeninteractive.sbie.Client;
import com.webdeninteractive.sbie.ProtocolHandler;
import com.webdeninteractive.sbie.ServiceEndpoint;
import com.webdeninteractive.sbie.util.DocumentUtils;
import com.webdeninteractive.sbie.config.SystemConfig;
import com.webdeninteractive.sbie.config.data.ServiceData;
import com.webdeninteractive.sbie.config.data.ClientData;
import com.webdeninteractive.sbie.config.data.ConfigData;
import com.webdeninteractive.sbie.exception.ComponentException;
import com.webdeninteractive.sbie.service.ServiceBase;
import java.util.Hashtable;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.apache.log4j.Logger;

/**
 * Service implementation which downloads and merges configuration data
 * from a remote BIE instance.
 * <P>
 * This implementation supports SOAP connections over HTTPS.  If
 * configured with the parameter <code>SSL</code> set to true, the
 * service will construct a "https://" URL instead of a "http://" URL.
 * <P>
 * Note that to successfully construct a https connection, you need to
 * configure a truststore which knows about the endpoint's server
 * certificate.  By default, the JSSE implementation will look at the
 * <CODE>javax.net.ssl.*</CODE> system properties for configuring the
 * truststore.
 *
 * @author gfast
 * @version $Id: ConfigurationService.java,v 1.1.1.1 2003/05/20 16:56:49 gdf Exp $
 */
public class ConfigurationService extends ServiceBase implements Service {

    private Logger logger = Logger.getLogger("com.webdeninteractive.sbie.v1_1.service.ConfigurationService");

    /** Create and initialize a ServiceEndpoint for this Service,
     *  filling in all the request-agnostic parameters.
     *  For any call made by this service, only the method property of the EP
     *  should need to be specified further.
     */
    protected ServiceEndpoint initEndpoint() {
        ServiceEndpoint ep = protocolHandler.createServiceEndpoint(this);
        String hostname = this.getParameter("hostname");
        if (hostname == null) {
            logger.warn("Service has null hostname parameter");
        }
        String hostport = this.getParameter("hostport");
        if (hostport == null) {
            logger.warn("Service has null hostport parameter");
        }
        String soapservice = this.getParameter("SoapService");
        if (soapservice == null) {
            logger.warn("Service has null soapservice parameter");
            soapservice = "null";
        }
        if (!soapservice.startsWith("/")) {
            soapservice = "/" + soapservice;
        }
        String SSLstr = this.getParameter("SSL");
        boolean useSSL = (new Boolean(SSLstr)).booleanValue();
        String schema = "http";
        if (useSSL) {
            schema = "https";
        }
        ep.setTarget(schema + "://" + hostname + ":" + hostport + soapservice);
        ep.setUser(this.getParameter("username"));
        ep.setPassword(this.getParameter("password"));
        return ep;
    }

    /** Have this service perform its task. */
    public void runService() {
        ServiceEndpoint ep = initEndpoint();
        try {
            logger.debug("Building request document");
            ep.setMethod("configRequest");
            Document request = DocumentUtils.makeDocument("configRequest");
            DocumentUtils.addText(DocumentUtils.appendElement(request, "configRequest"), "clientName", this.parent.getName());
            request.normalize();
            logger.debug("Sending document to endpoint: " + ep.toString());
            Document resp = protocolHandler.sendMessage(ep, request);
            logger.debug(DocumentUtils.dumpDoc(resp));
            SystemConfig config = parent.getParentSystem().getConfiguration();
            String prevConfigString = config.getConfigDataRoot().toString();
            processResp(resp, config);
            if (!config.getConfigDataRoot().toString().equals(prevConfigString)) {
                writeConfigurationFile();
            } else {
                logger.info("No configuration changes currently needed");
            }
        } catch (Exception e) {
            logger.error("Sending or receiving of config request failed: ", e);
        }
    }

    public void writeConfigurationFile() throws IOException, ComponentException {
        SystemConfig config = parent.getParentSystem().getConfiguration();
        File original = config.getLocation();
        File backup = new File(original.getParentFile(), original.getName() + "." + System.currentTimeMillis());
        FileInputStream in = new FileInputStream(original);
        FileOutputStream out = new FileOutputStream(backup);
        byte[] buffer = new byte[2048];
        try {
            int bytesread = 0;
            while ((bytesread = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesread);
            }
        } catch (IOException e) {
            logger.warn("Failed to copy backup of configuration file");
            throw e;
        } finally {
            in.close();
            out.close();
        }
        FileWriter replace = new FileWriter(original);
        replace.write(config.toFileFormat());
        replace.close();
        logger.info("Re-wrote configuration file " + original.getPath());
    }

    protected void processResp(Document resp, SystemConfig config) {
        logger.debug("Processing response");
        NodeList nl;
        nl = resp.getElementsByTagName("setService");
        for (int i = 0; i < nl.getLength(); i++) {
            Element setService = (Element) nl.item(i);
            processSetService(setService, config);
        }
        nl = resp.getElementsByTagName("setClient");
        for (int i = 0; i < nl.getLength(); i++) {
            Element setClient = (Element) nl.item(i);
            processSetClient(setClient, config);
        }
        nl = resp.getElementsByTagName("setBootConfig");
        for (int i = 0; i < nl.getLength(); i++) {
            Element setBoot = (Element) nl.item(i);
            processBootConfig(setBoot, config);
        }
    }

    protected void processBootConfig(Element setBootConfig, SystemConfig config) {
        ConfigData root = config.getConfigDataRoot();
        Iterator bootsystems = getNameValueElements(setBootConfig, "setBootSystem").iterator();
        while (bootsystems.hasNext()) {
            Pair pair = (Pair) bootsystems.next();
            root.setBootSystemName(pair.name);
            root.setBootSystemClassName(pair.value);
        }
        NodeList nl = setBootConfig.getElementsByTagName("setExtraClass");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            String extraClass = DocumentUtils.getTextContent(e);
            logger.debug("Requiring extra class '" + extraClass + "'");
            root.setExtraClass(extraClass);
        }
    }

    protected void processSetClient(Element setClient, SystemConfig config) {
        String clientname = DocumentUtils.getText(setClient, "clientname");
        boolean newClientDef = false;
        ClientData clientdata = config.getClientDataByName(clientname);
        if (clientdata == null) {
            clientdata = new ClientData();
            clientdata.setClientName(clientname);
            newClientDef = true;
        }
        String previousDef = clientdata.toString();
        Iterator sets = getSetters(setClient).iterator();
        while (sets.hasNext()) {
            Pair pair = (Pair) sets.next();
            if (pair.name != null && pair.value != null) {
                logger.debug("Setting " + pair.name + " = " + pair.value);
                if (pair.name.equals("clientClassName")) {
                    clientdata.setClientClassName(pair.value);
                } else if (pair.name.equals("startup")) {
                    clientdata.setStartup(pair.value.equals("true"));
                } else if (pair.name.equals("protocolHandlerName")) {
                    clientdata.setProtocolHandlerName(pair.value);
                } else if (pair.name.equals("protocolHandlerClassName")) {
                    clientdata.setProtocolHandlerClassName(pair.value);
                } else {
                    logger.warn("Unknown attribute '" + pair.name + "' for client " + clientname);
                }
            } else {
                logger.warn("Null in setter name=" + pair.name + " value=" + pair.value + " for client " + clientname);
            }
        }
        Iterator clears = getClearParams(setClient).iterator();
        while (clears.hasNext()) {
            Pair pair = (Pair) clears.next();
            if (pair.name != null) {
                logger.debug("Clearing param " + pair.name);
                clientdata.setParameter(pair.name, null);
            } else {
                logger.warn("Null name in clearParam" + " for client " + clientname);
            }
        }
        Iterator params = getSetParams(setClient).iterator();
        while (params.hasNext()) {
            Pair pair = (Pair) params.next();
            if (pair.name != null && pair.value != null) {
                logger.debug("Setting param " + pair.name + "=" + pair.value);
                clientdata.setParameter(pair.name, pair.value);
            } else {
                logger.warn("Null in setParam name=" + pair.name + " value=" + pair.value + "' for client " + clientname);
            }
        }
        if (clientdata.isValid()) {
            String currentDef = clientdata.toString();
            if (!currentDef.equals(previousDef)) {
                config.setClientData(clientdata);
                logger.info("Restarting client " + clientname);
                parent.getParentSystem().stopClient(clientname);
                parent.getParentSystem().startClient(clientname);
            }
        } else {
            logger.warn("Processed incomplete client definition " + "for client '" + clientname + "', ignoring");
        }
    }

    void processSetService(Element setService, SystemConfig config) {
        String servicename = DocumentUtils.getText(setService, "servicename");
        String clientname = DocumentUtils.getText(setService, "clientname");
        ServiceData servicedata = config.getServiceDataByName(clientname, servicename);
        if (servicedata == null) {
            servicedata = new ServiceData();
            servicedata.setServiceName(servicename);
        }
        String previousDef = servicedata.toString();
        Iterator sets = getSetters(setService).iterator();
        while (sets.hasNext()) {
            Pair pair = (Pair) sets.next();
            if (pair.name != null && pair.value != null) {
                logger.debug("Setting " + pair.name + " = " + pair.value);
                if (pair.name.equals("serviceClassName")) {
                    servicedata.setServiceClassName(pair.value);
                } else if (pair.name.equals("serviceInterval")) {
                    try {
                        servicedata.setServiceInterval(Long.parseLong(pair.value.trim()));
                    } catch (NumberFormatException nfex) {
                        logger.warn("Non-numeric interval '" + pair.value + "' for client " + clientname + " service " + servicename);
                    }
                } else {
                    logger.warn("Unknown attribute '" + pair.name + "' for client " + clientname + " service " + servicename);
                }
            } else {
                logger.warn("Null in setter name=" + pair.name + " value=" + pair.value + "' for client " + clientname + " service " + servicename);
            }
        }
        Iterator clears = getClearParams(setService).iterator();
        while (clears.hasNext()) {
            Pair pair = (Pair) clears.next();
            if (pair.name != null) {
                logger.debug("Clearing param " + pair.name);
                servicedata.setParameter(pair.name, null);
            } else {
                logger.warn("Null name in clearParam" + " for client " + clientname + " service " + servicename);
            }
        }
        Iterator params = getSetParams(setService).iterator();
        while (params.hasNext()) {
            Pair pair = (Pair) params.next();
            if (pair.name != null && pair.value != null) {
                logger.debug("Setting param " + pair.name + "=" + pair.value);
                servicedata.setParameter(pair.name, pair.value);
            } else {
                logger.warn("Null in setParam name=" + pair.name + " value=" + pair.value + "' for client " + clientname + " service " + servicename);
            }
        }
        if (servicedata.isValid()) {
            String currentDef = servicedata.toString();
            if (!currentDef.equals(previousDef)) {
                try {
                    config.setServiceData(clientname, servicedata);
                    logger.info("Restarting service " + servicename);
                    parent.stopService(servicename);
                    parent.startService(servicename);
                } catch (IllegalArgumentException noSuchClient) {
                    logger.debug("Failed to setServiceData(): ", noSuchClient);
                    logger.info("Configuration mismatch, change not applied " + "(are you using the correct username?)");
                }
            }
        } else {
            logger.warn("Processed incomplete service definition " + "for service '" + servicename + "', ignoring");
        }
    }

    /** Returns a list of ConfigurationService.Pair objects. */
    protected Collection getSetters(Element e) {
        return getNameValueElements(e, "set");
    }

    /** Returns a list of ConfigurationService.Pair objects. */
    protected Collection getSetParams(Element e) {
        return getNameValueElements(e, "setParam");
    }

    /** Returns a list of ConfigurationService.Pair objects.
     *  (The value attrs of the returned pairs should all be null) */
    protected Collection getClearParams(Element e) {
        return getNameValueElements(e, "clearParam");
    }

    /** Helper method for get*(element) methods.
     *  Finds and returns Pairs of ($name,$value) for subelements of the form:
     *  <pre>
     *     &lt;$tagName name="$name"&gt;$value&lt;/$tagName&gt;
     *  </pre>
     *  @return a collection of ConfigurationService.Pair objects
     */
    protected Collection getNameValueElements(Element e, String tagName) {
        ArrayList ret = new ArrayList();
        NodeList nl = e.getElementsByTagName(tagName);
        for (int i = 0; i < nl.getLength(); i++) {
            Element se = (Element) nl.item(i);
            Pair pair = new Pair();
            pair.name = se.getAttribute("name");
            pair.value = DocumentUtils.getTextContent(se);
            ret.add(pair);
        }
        return ret;
    }

    /** Internal dumb data object for passing name-value pairs. */
    protected class Pair {

        public String name = null;

        public String value = null;
    }

    public Map getRequiredParameters() {
        HashMap p = new HashMap();
        p.put("hostname", "Host name of BIE server to retrieve configuration from");
        p.put("hostport", "Port to connect to on BIE server");
        p.put("SoapService", "URL path (on server) to make request to");
        p.put("SSL", "Boolean: if 'true', connections will be made over HTTPS");
        p.put("username", "User name to use for connection authentication");
        p.put("password", "Password to use for connection authentication");
        return p;
    }
}
