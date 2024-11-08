package com.rbnb.web;

import java.io.IOException;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.*;
import javax.servlet.http.*;

/**
  *  A servlet which forwards HTTP requests to other machines and/or ports.
  */
public final class HttpForwardServlet extends HttpServlet {

    /**
	  * Construction, loading options from web.xml.
	  */
    public HttpForwardServlet() throws NamingException {
        InitialContext webEnv = new InitialContext();
        checkEnv(webEnv, "java:comp/env/com.rbnb.web.debug", "debug", Boolean.FALSE);
        debug = ((Boolean) defaultProperties.get("debug")).booleanValue();
        checkEnv(webEnv, "java:comp/env/com.rbnb.web.destination", "destination", null);
        String dest = defaultProperties.get("destination").toString();
        if (dest.endsWith("/")) destination = dest.substring(0, dest.length() - 1); else destination = dest;
    }

    /**
	  *  Handles the Http GET operation.
	  *
	  */
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ServletOutputStream sos = res.getOutputStream();
        String qString = req.getQueryString();
        String reqUri = req.getRequestURI();
        if (servletPath == null) {
            int nextSlash = reqUri.indexOf('/', 1);
            if (nextSlash == -1) servletPath = reqUri; else servletPath = reqUri.substring(0, nextSlash);
        }
        String destUrl = destination + reqUri.substring(servletPath.length()) + (qString == null ? "" : "?" + qString);
        HttpURLConnection connection = (HttpURLConnection) new java.net.URL(destUrl).openConnection();
        connection.setRequestMethod(req.getMethod());
        copyHeaders(req, connection);
        connection.connect();
        try {
            int status = connection.getResponseCode();
            res.setStatus(status);
            if (debug) {
                System.err.println(req.getRequestURL().toString() + "\n\t-> " + destUrl + "\n\t = " + connection.getResponseCode());
                System.err.println("---  Response ---");
            }
            String key, value;
            int c = 0;
            while ((value = connection.getHeaderField(c)) != null) {
                if ((key = connection.getHeaderFieldKey(c++)) != null) {
                    res.setHeader(key, value);
                }
                if (debug) System.err.println(key + ": " + connection.getHeaderField(c - 1));
            }
            if (status >= 200 && status < 300) {
                byte[] buff = new byte[1024];
                int nRead;
                java.io.InputStream is = connection.getInputStream();
                while ((nRead = is.read(buff)) != -1) sos.write(buff, 0, nRead);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**	 
	  *  Passes to doGet(req,res)
	  */
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
	  * Utilty method to load parameters from the "env-entry" fields in the
	  *	  servlet web.xml file.
	  */
    private void checkEnv(InitialContext webEnv, String propertyEnv, String propertyLocal, Object _default) {
        try {
            Object result = webEnv.lookup(propertyEnv);
            defaultProperties.put(propertyLocal, result);
            return;
        } catch (NamingException ne) {
        }
        defaultProperties.put(propertyLocal, _default);
    }

    private void copyHeaders(HttpServletRequest req, URLConnection conn) {
        if (debug) System.err.println("\n\n---  Request ---");
        Enumeration names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String header = (String) names.nextElement();
            Enumeration values = req.getHeaders(header);
            while (values.hasMoreElements()) {
                String value = (String) values.nextElement();
                conn.addRequestProperty(header, value);
                if (debug) System.err.println(header + ": " + value);
            }
        }
        int ns = new java.util.Random().nextInt(90) + 10;
        conn.addRequestProperty("Opt", "\"http://rbnb.net/ext\"; ns=" + ns);
        conn.addRequestProperty("" + ns + "-source-ip", req.getRemoteAddr());
    }

    /**
	  * Enables debug output if set in web.xml.
	  */
    private final boolean debug;

    /**
	  * Properties configured in web.xml.
	  */
    private final HashMap defaultProperties = new HashMap();

    /**
	  * URL prefix to forward requests to.
	  */
    private final String destination;

    /**
	  * URI to this servlet relative to the server root, lazily initialized.
	  */
    private String servletPath;
}
