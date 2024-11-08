package com.taliasplayground.servlet.http.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.servlet.ServletContextResourceResolver;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;

public class CxfHandler {

    private static final Map<String, String> STATIC_CONTENT_TYPES;

    static {
        STATIC_CONTENT_TYPES = new HashMap<String, String>();
        STATIC_CONTENT_TYPES.put("html", "text/html");
        STATIC_CONTENT_TYPES.put("txt", "text/plain");
        STATIC_CONTENT_TYPES.put("css", "text/css");
        STATIC_CONTENT_TYPES.put("pdf", "application/pdf");
    }

    private String[] staticResourcesList;

    private String[] redirectList;

    private String dispatcherServletPath;

    private String dispatcherServletName;

    private boolean redirectQueryCheck;

    private Bus bus;

    private ServletController controller;

    private ServletConfig servletConfig;

    public CxfHandler(String[] staticResourcesList, String[] redirectList, String dispatcherServletPath, String dispatcherServletName, boolean redirectQueryCheck) {
        this.staticResourcesList = staticResourcesList == null ? null : staticResourcesList.clone();
        this.redirectList = redirectList == null ? null : redirectList.clone();
        this.redirectQueryCheck = redirectQueryCheck;
        this.dispatcherServletName = dispatcherServletName;
        this.dispatcherServletPath = dispatcherServletPath;
    }

    public void init(ServletConfig sc, DestinationRegistry destinationRegistry) throws ServletException {
        servletConfig = sc;
        if (this.bus == null) {
            this.bus = BusFactory.newInstance().createBus();
        }
        ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
        resourceManager.addResourceResolver(new ServletContextResourceResolver(servletConfig.getServletContext()));
        if (destinationRegistry == null) {
            DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
            try {
                DestinationFactory df = dfm.getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
                if (df instanceof HTTPTransportFactory) {
                    HTTPTransportFactory transportFactory = (HTTPTransportFactory) df;
                    destinationRegistry = transportFactory.getRegistry();
                }
            } catch (BusException e) {
            }
        }
        this.controller = new ServletController(destinationRegistry, servletConfig, new ServiceListGeneratorServlet(destinationRegistry, bus));
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    protected void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader loader = bus.getExtension(ClassLoader.class);
            if (loader != null) {
                Thread.currentThread().setContextClassLoader(loader);
            }
            BusFactory.setThreadDefaultBus(bus);
            controller.invoke(request, response);
        } finally {
            BusFactory.setThreadDefaultBus(null);
            Thread.currentThread().setContextClassLoader(origLoader);
        }
    }

    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        if ((dispatcherServletPath != null || dispatcherServletName != null) && (redirectList != null && matchPath(redirectList, request) || redirectList == null)) {
            redirect(request, response, request.getPathInfo());
            return;
        }
        if (staticResourcesList != null && matchPath(staticResourcesList, request)) {
            serveStaticContent(request, response, request.getPathInfo());
            return;
        }
        invoke(request, response);
    }

    protected void serveStaticContent(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws ServletException {
        InputStream is = servletConfig.getServletContext().getResourceAsStream(pathInfo);
        if (is == null) {
            throw new ServletException("Static resource " + pathInfo + " is not available");
        }
        try {
            int ind = pathInfo.lastIndexOf(".");
            if (ind != -1 && ind < pathInfo.length()) {
                String type = STATIC_CONTENT_TYPES.get(pathInfo.substring(ind + 1));
                if (type != null) {
                    response.setContentType(type);
                }
            }
            ServletOutputStream os = response.getOutputStream();
            IOUtils.copy(is, os);
            os.flush();
        } catch (IOException ex) {
            throw new ServletException("Static resource " + pathInfo + " can not be written to the output stream");
        }
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws ServletException {
        String theServletPath = dispatcherServletPath == null ? "/" : dispatcherServletPath;
        ServletContext sc = servletConfig.getServletContext();
        RequestDispatcher rd = dispatcherServletName != null ? sc.getNamedDispatcher(dispatcherServletName) : sc.getRequestDispatcher(theServletPath + pathInfo);
        if (rd == null) {
            throw new ServletException("No RequestDispatcher can be created for path " + pathInfo);
        }
        try {
            HttpServletRequestFilter servletRequest = new HttpServletRequestFilter(request, pathInfo, theServletPath);
            rd.forward(servletRequest, response);
        } catch (Throwable ex) {
            throw new ServletException("RequestDispatcher for path " + pathInfo + " has failed");
        }
    }

    private boolean matchPath(String[] values, HttpServletRequest request) {
        String path = request.getPathInfo();
        if (redirectQueryCheck) {
            String queryString = request.getQueryString();
            if (queryString != null && queryString.length() > 0) {
                path += "?" + queryString;
            }
        }
        for (String value : values) {
            if (path.matches(value)) {
                return true;
            }
        }
        return false;
    }

    private static class HttpServletRequestFilter extends HttpServletRequestWrapper {

        private String pathInfo;

        private String servletPath;

        public HttpServletRequestFilter(HttpServletRequest request, String pathInfo, String servletPath) {
            super(request);
            this.pathInfo = pathInfo;
            this.servletPath = servletPath;
        }

        @Override
        public String getServletPath() {
            return servletPath;
        }

        @Override
        public String getPathInfo() {
            return pathInfo;
        }

        @Override
        public String getRequestURI() {
            String contextPath = getContextPath();
            if ("/".equals(contextPath)) {
                contextPath = "";
            }
            return contextPath + servletPath + pathInfo;
        }

        @Override
        public String getParameter(String name) {
            if (AbstractHTTPDestination.SERVICE_REDIRECTION.equals(name)) {
                return "true";
            }
            return super.getParameter(name);
        }
    }
}
