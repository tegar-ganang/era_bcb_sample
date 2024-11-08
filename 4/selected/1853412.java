package org.atricore.idbus.kernel.main.mediation.camel.component.http;

import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpConsumer;
import org.apache.camel.component.http.HttpExchange;
import org.apache.camel.component.jetty.CamelContinuationServlet;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.spi.Registry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationUnit;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationUnitContainer;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationUnitRegistry;
import org.atricore.idbus.kernel.main.mediation.camel.CamelIdentityMediationUnitContainer;
import org.atricore.idbus.kernel.main.util.ConfigurationContext;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.web.context.support.WebApplicationContextUtils;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * This servlet can follow redirects internally. If a redirect  location targets the same IDBus server,
 * it process it internally, without sending it to the browser.
 *
 * An improved version of the servlet could actually act as a proxy/client for external redirects or even to process HTML
 *
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class OsgiIDBusServlet2 extends CamelContinuationServlet {

    private static final Log logger = LogFactory.getLog(OsgiIDBusServlet2.class);

    private IdentityMediationUnitRegistry registry;

    private boolean followRedirects;

    private String localTargetBaseUrl;

    private ConfigurationContext kernelConfig;

    private InternalProcessingPolicy internalProcessingPolicy;

    public OsgiIDBusServlet2() {
        super();
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long started = 0;
        try {
            if (logger.isTraceEnabled()) {
                started = System.currentTimeMillis();
                logger.trace("IDBUS SERVLET SERVICE START AT " + started + " (" + Thread.currentThread().getName() + ")");
            }
            if (kernelConfig == null) {
                kernelConfig = lookupKernelConfig();
                if (kernelConfig == null) {
                    logger.error("No Kernel Configuration Context found!");
                    throw new ServletException("No Kernel Configuration Context found!");
                }
                followRedirects = Boolean.parseBoolean(kernelConfig.getProperty("binding.http.followRedirects"));
                localTargetBaseUrl = kernelConfig.getProperty("binding.http.localTargetBaseUrl");
                logger.info("Following Redirects internally : " + followRedirects);
            }
            if (internalProcessingPolicy == null) {
                org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext wac = (org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext) WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
                internalProcessingPolicy = (InternalProcessingPolicy) wac.getBean("internal-processing-policy");
            }
            if (!followRedirects || !internalProcessingPolicy.match(req)) {
                doService(req, res);
            } else {
                doProxyInternally(req, res);
            }
        } finally {
            if (logger.isTraceEnabled()) {
                long ended = System.currentTimeMillis();
                logger.trace("IDBUS SERVLET SERVICE END AT " + ended + " TOOK: " + (ended - started) + " ms(" + Thread.currentThread().getName() + ")");
            }
        }
    }

    protected void doProxyInternally(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpRequestBase proxyReq = buildProxyRequest(req);
        URI reqUri = proxyReq.getURI();
        String cookieDomain = reqUri.getHost();
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute("org.atricorel.idbus.kernel.main.binding.http.HttpServletRequest", req);
        int intIdx = 0;
        for (int i = 0; i < httpClient.getRequestInterceptorCount(); i++) {
            if (httpClient.getRequestInterceptor(i) instanceof RequestAddCookies) {
                intIdx = i;
                break;
            }
        }
        IDBusRequestAddCookies interceptor = new IDBusRequestAddCookies(cookieDomain);
        httpClient.removeRequestInterceptorByClass(RequestAddCookies.class);
        httpClient.addRequestInterceptor(interceptor, intIdx);
        httpClient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        if (logger.isTraceEnabled()) logger.trace("Staring to follow redirects for " + req.getPathInfo());
        HttpResponse proxyRes = null;
        List<Header> storedHeaders = new ArrayList<Header>(40);
        boolean followTargetUrl = true;
        byte[] buff = new byte[1024];
        while (followTargetUrl) {
            if (logger.isTraceEnabled()) logger.trace("Sending internal request " + proxyReq);
            proxyRes = httpClient.execute(proxyReq, httpContext);
            String targetUrl = null;
            Header[] headers = proxyRes.getAllHeaders();
            for (Header header : headers) {
                if (header.getName().equals("Server")) continue;
                if (header.getName().equals("Transfer-Encoding")) continue;
                if (header.getName().equals("Location")) continue;
                if (header.getName().equals("Expires")) continue;
                if (header.getName().equals("Content-Length")) continue;
                if (header.getName().equals("Content-Type")) continue;
                storedHeaders.add(header);
            }
            if (logger.isTraceEnabled()) logger.trace("HTTP/STATUS:" + proxyRes.getStatusLine().getStatusCode() + "[" + proxyReq + "]");
            switch(proxyRes.getStatusLine().getStatusCode()) {
                case 200:
                    followTargetUrl = false;
                    break;
                case 404:
                    followTargetUrl = false;
                    break;
                case 500:
                    followTargetUrl = false;
                    break;
                case 302:
                    Header location = proxyRes.getFirstHeader("Location");
                    targetUrl = location.getValue();
                    if (!internalProcessingPolicy.match(req, targetUrl)) {
                        if (logger.isTraceEnabled()) logger.trace("Do not follow HTTP 302 to [" + location.getValue() + "]");
                        Collections.addAll(storedHeaders, proxyRes.getHeaders("Location"));
                        followTargetUrl = false;
                    } else {
                        if (logger.isTraceEnabled()) logger.trace("Do follow HTTP 302 to [" + location.getValue() + "]");
                        followTargetUrl = true;
                    }
                    break;
                default:
                    followTargetUrl = false;
                    break;
            }
            HttpEntity entity = proxyRes.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                try {
                    if (!followTargetUrl) {
                        for (Header header : headers) {
                            if (header.getName().equals("Content-Type")) res.setHeader(header.getName(), header.getValue());
                            if (header.getName().equals("Content-Length")) res.setHeader(header.getName(), header.getValue());
                        }
                        res.setStatus(proxyRes.getStatusLine().getStatusCode());
                        for (Header header : storedHeaders) {
                            if (header.getName().startsWith("Set-Cookie")) res.addHeader(header.getName(), header.getValue()); else res.setHeader(header.getName(), header.getValue());
                        }
                        IOUtils.copy(instream, res.getOutputStream());
                        res.getOutputStream().flush();
                    } else {
                        int r = instream.read(buff);
                        int total = r;
                        while (r > 0) {
                            r = instream.read(buff);
                            total += r;
                        }
                        if (total > 0) logger.warn("Ignoring response content size : " + total);
                    }
                } catch (IOException ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    proxyReq.abort();
                    throw ex;
                } finally {
                    try {
                        instream.close();
                    } catch (Exception ignore) {
                    }
                }
            } else {
                if (!followTargetUrl) {
                    res.setStatus(proxyRes.getStatusLine().getStatusCode());
                    for (Header header : headers) {
                        if (header.getName().equals("Content-Type")) res.setHeader(header.getName(), header.getValue());
                        if (header.getName().equals("Content-Length")) res.setHeader(header.getName(), header.getValue());
                    }
                    for (Header header : storedHeaders) {
                        if (header.getName().startsWith("Set-Cookie")) res.addHeader(header.getName(), header.getValue()); else res.setHeader(header.getName(), header.getValue());
                    }
                }
            }
            if (followTargetUrl) {
                proxyReq = buildProxyRequest(targetUrl);
                httpContext = null;
            }
        }
        if (logger.isTraceEnabled()) logger.trace("Ended following redirects for " + req.getPathInfo());
    }

    protected HttpRequestBase buildProxyRequest(String targetUrl) throws MalformedURLException {
        if (localTargetBaseUrl != null) {
            URL url = new URL(targetUrl);
            StringBuilder newUrl = new StringBuilder(localTargetBaseUrl);
            newUrl.append(url.getPath());
            if (url.getQuery() != null) {
                newUrl.append("?");
                newUrl.append(url.getQuery());
            }
            targetUrl = newUrl.toString();
        }
        HttpRequestBase proxyReq = new HttpGet(targetUrl);
        proxyReq.addHeader("IDBUS-PROXIED-REQUEST", "TRUE");
        return proxyReq;
    }

    protected HttpRequestBase buildProxyRequest(HttpServletRequest req) throws ServletException {
        HttpRequestBase proxyReq = null;
        StringBuilder targetUrl = new StringBuilder();
        if (localTargetBaseUrl != null) {
            targetUrl.append(localTargetBaseUrl);
            targetUrl.append(req.getContextPath().equals("") ? "/" : req.getContextPath());
            targetUrl.append(req.getPathInfo());
            if (req.getMethod().equalsIgnoreCase("GET")) {
                if (req.getQueryString() != null) targetUrl.append("?").append(req.getQueryString());
            } else {
                throw new ServletException(req.getMethod() + " HTTP method cannot be proxied!");
            }
        } else {
            targetUrl.append(req.getRequestURL());
        }
        proxyReq = new HttpGet(targetUrl.toString());
        proxyReq.addHeader("IDBUS-PROXIED-REQUEST", "TRUE");
        Enumeration<String> hNames = req.getHeaderNames();
        while (hNames.hasMoreElements()) {
            String hName = hNames.nextElement();
            proxyReq.addHeader(hName, req.getHeader(hName));
        }
        return proxyReq;
    }

    /**
     * Actually process this request
     */
    protected void doService(HttpServletRequest req, HttpServletResponse r) throws ServletException, IOException {
        HttpServletResponse res = new WHttpServletResponse(r);
        if (registry == null) registry = lookupIdentityMediationUnitRegistry();
        if (registry == null) {
            logger.error("No identity mediation registry found ");
            throw new ServletException("No identity mediation registry found!");
        }
        HttpConsumer consumer = resolveConsumer(req);
        if (consumer == null) {
            log("No HTTP Consumer found for " + req.getRequestURL().toString() + " Sending 404 (Not Found) HTTP Status.");
            logger.warn("Make sure your appliance is STARTED");
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (logger.isTraceEnabled()) {
            String cookies = req.getHeader("Cookie");
            logger.trace("Received COOKIES [" + cookies + "]");
        }
        IDBusHttpEndpoint endpoint = (IDBusHttpEndpoint) consumer.getEndpoint();
        try {
            final HttpExchange exchange = new HttpExchange(endpoint, req, res);
            consumer.getProcessor().process(exchange);
            consumer.getBinding().writeResponse(exchange, res);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    protected HttpConsumer resolveConsumer(HttpServletRequest req) {
        HttpConsumer targetConsumer = null;
        String consumerKey = "idbus:" + req.getContextPath() + req.getPathInfo();
        Collection<IdentityMediationUnit> identityMediationUnits = registry.getIdentityMediationUnits();
        if (logger.isDebugEnabled()) logger.debug("Scanning in " + identityMediationUnits.size() + " Identity Mediation Units " + "for IDBus Http Camel Consumer [" + consumerKey + "]");
        for (IdentityMediationUnit identityMediationUnit : identityMediationUnits) {
            if (logger.isTraceEnabled()) logger.trace("Scanning Identity Mediation Unit [" + identityMediationUnit.getName() + "] " + "for IDBus Http Camel Consumer [" + consumerKey + "]");
            IdentityMediationUnitContainer imUnitContainer = identityMediationUnit.getContainer();
            if (imUnitContainer == null) {
                logger.error("Identity Mediation Registry [" + registry + "] " + "has no Identity Mediation Engine. Ignoring!");
                return null;
            }
            if (!(imUnitContainer instanceof CamelIdentityMediationUnitContainer)) {
                logger.error("Identity Mediation Registry [" + registry + "] " + "references unsupported Identity Mediation Engine " + "type [" + imUnitContainer.getClass().getName() + "]. Ignoring!");
                return null;
            }
            CamelContext cctx = ((CamelIdentityMediationUnitContainer) imUnitContainer).getContext();
            Registry reg = cctx.getRegistry();
            JndiRegistry jReg = (JndiRegistry) reg;
            Object consumer = jReg.lookup(consumerKey);
            if (consumer != null && consumer instanceof HttpConsumer) {
                targetConsumer = (HttpConsumer) jReg.lookup(consumerKey);
                if (targetConsumer != null) {
                    if (logger.isTraceEnabled()) logger.trace("HTTP Consumer for consumer key [" + consumerKey + "] found");
                    break;
                }
            }
        }
        if (targetConsumer == null) {
            if (logger.isDebugEnabled()) logger.debug("No HTTP Consumer bound to JNDI Name " + consumerKey);
        }
        return targetConsumer;
    }

    protected ConfigurationContext lookupKernelConfig() throws ServletException {
        org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext wac = (org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext) WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        if (wac == null) {
            logger.error("Spring application context not found in servlet context");
            throw new ServletException("Spring application context not found in servlet context");
        }
        BundleContext bc = wac.getBundleContext();
        for (Bundle b : bc.getBundles()) {
            if (b.getRegisteredServices() != null) {
                if (logger.isTraceEnabled()) logger.trace("(" + b.getBundleId() + ") " + b.getSymbolicName() + " serviceReferences:" + b.getRegisteredServices().length);
                for (ServiceReference r : b.getRegisteredServices()) {
                    String props = "";
                    for (String key : r.getPropertyKeys()) {
                        props += "\n\t\t" + key + "=" + r.getProperty(key);
                        if (r.getProperty(key) instanceof String[]) {
                            String[] v = (String[]) r.getProperty(key);
                            props += "[";
                            String prefix = "";
                            for (String aV : v) {
                                props += prefix + aV;
                                prefix = ",";
                            }
                            props += "]";
                        }
                    }
                    if (logger.isTraceEnabled()) logger.trace("ServiceReference:<<" + r + ">> [" + r.getProperty("service.id") + "]" + props);
                }
            } else {
                if (logger.isTraceEnabled()) logger.trace("(" + b.getBundleId() + ") " + b.getSymbolicName() + "services:<null>");
            }
        }
        Map<String, ConfigurationContext> kernelCfgsMap = wac.getBeansOfType(ConfigurationContext.class);
        if (kernelCfgsMap == null) {
            logger.warn("No kernel configuration context configured");
            return null;
        }
        if (kernelCfgsMap.size() > 1) {
            logger.warn("More than one kernel configuration context configured");
            return null;
        }
        ConfigurationContext kCfg = kernelCfgsMap.values().iterator().next();
        if (logger.isDebugEnabled()) logger.debug("Found kernel configuration context " + kCfg);
        return kCfg;
    }

    protected IdentityMediationUnitRegistry lookupIdentityMediationUnitRegistry() throws ServletException {
        org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext wac = (org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext) WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        if (wac == null) {
            logger.error("Spring application context not found in servlet context");
            throw new ServletException("Spring application context not found in servlet context");
        }
        BundleContext bc = wac.getBundleContext();
        for (Bundle b : bc.getBundles()) {
            if (b.getRegisteredServices() != null) {
                if (logger.isTraceEnabled()) logger.trace("(" + b.getBundleId() + ") " + b.getSymbolicName() + " serviceReferences:" + b.getRegisteredServices().length);
                for (ServiceReference r : b.getRegisteredServices()) {
                    String props = "";
                    for (String key : r.getPropertyKeys()) {
                        props += "\n\t\t" + key + "=" + r.getProperty(key);
                        if (r.getProperty(key) instanceof String[]) {
                            String[] v = (String[]) r.getProperty(key);
                            props += "[";
                            String prefix = "";
                            for (String aV : v) {
                                props += prefix + aV;
                                prefix = ",";
                            }
                            props += "]";
                        }
                    }
                    if (logger.isTraceEnabled()) logger.trace("ServiceReference:<<" + r + ">> [" + r.getProperty("service.id") + "]" + props);
                }
            } else {
                if (logger.isTraceEnabled()) logger.trace("(" + b.getBundleId() + ") " + b.getSymbolicName() + "services:<null>");
            }
        }
        Map<String, IdentityMediationUnitRegistry> imuRegistryMap = wac.getBeansOfType(IdentityMediationUnitRegistry.class);
        if (imuRegistryMap == null) {
            logger.warn("No identity mediation unit registry configured");
            return null;
        }
        if (imuRegistryMap.size() > 1) {
            logger.warn("More than one identity mediation unit registry configured");
            return null;
        }
        IdentityMediationUnitRegistry r = imuRegistryMap.values().iterator().next();
        if (logger.isDebugEnabled()) logger.debug("Found Identity Mediation Unit Registry " + r);
        return r;
    }

    protected class WHttpServletResponse extends HttpServletResponseWrapper {

        public WHttpServletResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            if (name.equalsIgnoreCase("content.type")) super.addHeader("Content-Type", value);
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if (name.equalsIgnoreCase("content.type")) {
                super.setHeader("Content-Type", value);
            }
            super.setHeader(name, value);
        }
    }
}
