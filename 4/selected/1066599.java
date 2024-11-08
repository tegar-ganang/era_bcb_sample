package com.pmdesigns.jvc;

import java.io.*;
import java.util.*;
import java.net.HttpURLConnection;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import com.pmdesigns.jvc.tools.Base64Coder;
import com.pmdesigns.jvc.tools.JVCGenerator;

/**
 * JVC request dispatching servlet
 *
 * @author mike dooley
 */
public class JVCDispatcher extends HttpServlet {

    private Map<String, Class> generatorClasses;

    private String pkgPrefix;

    public static final String PKG_PREFIX_KEY = "pkg_prefix";

    private static Class requestContextClass;

    private static Class classNotFound;

    private Destroyable application;

    private static final boolean TRACE = false;

    private static ThreadLocal<JVCRequestContext> ctxHolder = new ThreadLocal<JVCRequestContext>() {

        protected synchronized JVCRequestContext initialValue() {
            return null;
        }
    };

    /**
	 * Return the thread local request context
	 * @return the JVCRequestContext associated with the current thread or null
	 */
    public static JVCRequestContext getRC() {
        return ctxHolder.get();
    }

    /**
	 * Get the package prefix (from config) so we know the fully qualified
	 * name of page generators and controllers.  Also create and instance
	 * of the Application object.
	 */
    public void init() {
        pkgPrefix = getInitParameter(PKG_PREFIX_KEY);
        generatorClasses = new HashMap<String, Class>();
        if (classNotFound == null) {
            classNotFound = getClass();
        }
        String className = appendPkg(pkgPrefix, "Application");
        try {
            Class appClass = Class.forName(className);
            Class[] args = { Class.forName("javax.servlet.GenericServlet") };
            Constructor<Destroyable> appConstructor = appClass.getConstructor(args);
            this.application = appConstructor.newInstance(this);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            Log.error("Failed to create Application instance: ", ((t != null) ? t : e));
        } catch (IllegalArgumentException e) {
            Log.error("Illegal Application constructor args", e);
        } catch (NoSuchMethodException e) {
            Log.error("Constructor not found for: " + className, e);
        } catch (Exception e) {
            Log.error("Error while creating Application", e);
        }
    }

    /**
	 * Notify the Application that its shutdown time.
	 */
    public void destroy() {
        if (application != null) {
            application.destroy();
            application = null;
        }
    }

    /**
	 * Handle a GET request. Called by servlet container.
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        handleRequest(request, response);
    }

    /**
	 * Handle a POST request. Called by servlet container.
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        handleRequest(request, response);
    }

    /**
	 * Private implementation to handle a GET or POST request by invoking the appropriate
	 * PageGenerator and Controller objects, or by serving static content.
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long t0, t1;
        if (TRACE) t0 = System.currentTimeMillis();
        response.setCharacterEncoding("UTF8");
        request.setCharacterEncoding("UTF8");
        String reqPath = request.getServletPath();
        if (reqPath.length() < 1) {
            Log.error("Empty path!");
            response.sendError(HttpURLConnection.HTTP_NOT_FOUND, request.getRequestURI());
        } else if (reqPath.charAt(0) == '/') {
            reqPath = reqPath.substring(1, reqPath.length());
        }
        String controller;
        String action;
        int idx = reqPath.lastIndexOf('/');
        if (idx == -1) {
            controller = "";
            action = reqPath;
        } else {
            controller = reqPath.substring(0, idx);
            action = reqPath.substring(idx + 1);
        }
        if (action.indexOf('.') == -1) {
            if (action.length() == 0) {
                action = "index";
            }
            String pkg = appendPkg(pkgPrefix, "generators");
            if (controller.length() > 0) {
                pkg += "." + controller.replace('/', '.');
            }
            String methodName = JVCGenerator.GENERATOR_METHOD_NAME;
            String className = pkg + "." + JVCGenerator.capitalize(action) + "Generator";
            Class genClass = generatorClasses.get(className);
            if (genClass == null) {
                try {
                    genClass = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    genClass = classNotFound;
                }
                generatorClasses.put(className, genClass);
            }
            if (genClass != classNotFound) {
                JVCRequestContext rc = null;
                try {
                    if (requestContextClass == null) {
                        requestContextClass = Class.forName("com.pmdesigns.jvc.JVCRequestContext");
                    }
                    Class[] args = { requestContextClass };
                    Method meth = genClass.getMethod(methodName, args);
                    Map<String, String> flash = getFlash(request, response);
                    rc = new JVCRequestContext(request, response, this, flash, controller, action);
                    ctxHolder.set(rc);
                    String s = (String) meth.invoke(null, rc);
                    response.getWriter().print(s);
                    response.flushBuffer();
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();
                    if (t instanceof NonStandardResponseException) {
                        NonStandardResponseException resp = (NonStandardResponseException) t;
                        if (resp.httpCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                            if (rc != null && !rc.flash.isEmpty()) {
                                Cookie c = makeFlashCookie(rc.flash);
                                c.setPath(request.getContextPath());
                                response.addCookie(c);
                            }
                            response.sendRedirect(resp.arg);
                        } else {
                            response.sendError(resp.httpCode, resp.arg);
                        }
                    } else if (t instanceof BinaryResponseException) {
                        BinaryResponseException bre = (BinaryResponseException) t;
                        OutputStream out = new BufferedOutputStream(response.getOutputStream());
                        copy(bre.in, out);
                        response.flushBuffer();
                    } else {
                        String err = ((t != null) ? t.toString() : e.toString());
                        Log.error("Invocation error: ", ((t != null) ? t : e));
                        response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, err);
                    }
                } catch (NoSuchMethodException e) {
                    Log.error("Method not found: " + className + "." + methodName);
                    response.sendError(HttpURLConnection.HTTP_NOT_FOUND, request.getRequestURI());
                } catch (Throwable e) {
                    Log.error("Unhandled exception while processing " + request.getRequestURI(), e);
                    response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, request.getRequestURI());
                } finally {
                    ctxHolder.set(null);
                    if (TRACE) Log.info("TIMING_1 '" + reqPath + "' = " + (System.currentTimeMillis() - t0));
                }
                return;
            }
        }
        if (action.length() == 0) {
            reqPath += "index.html";
        }
        try {
            InputStream in = getServletContext().getResourceAsStream(reqPath);
            if (in != null) {
                OutputStream out = new BufferedOutputStream(response.getOutputStream());
                copy(in, out);
                response.flushBuffer();
                if (TRACE) Log.info("TIMING_2 '" + reqPath + "' = " + (System.currentTimeMillis() - t0));
                return;
            }
        } catch (IOException e) {
            Log.error("Error sending '" + reqPath + "'", e);
        }
        response.sendError(HttpURLConnection.HTTP_NOT_FOUND, request.getRequestURI());
        if (TRACE) Log.info("TIMING_3 '" + reqPath + "' = " + (System.currentTimeMillis() - t0));
    }

    /**
	 * Copy an input stream to an output stream.
	 */
    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int read;
        while ((read = in.read(buf)) > -1) {
            out.write(buf, 0, read);
        }
        out.flush();
    }

    /**
	 * Append a package string to a prefix if the prefix isn't empty
	 */
    private static String appendPkg(String prefix, String pkg) {
        return (prefix == null || prefix.length() == 0) ? pkg : prefix + "." + pkg;
    }

    private static final String FLASH_COOKIE = "jvc_flash";

    /**
	 * Look for a 'flash cookie' in the request.  If found deserialize it, clear the cookie
	 * and return it, otherwise just return an empty Map.
	 * @see #makeFlashCookie
	 * @see #serializeMap
	 * @see #deserializeMap
	 */
    private static Map<String, String> getFlash(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (FLASH_COOKIE.equals(c.getName())) {
                    Map<String, String> map = deserializeMap(c.getValue());
                    c = new Cookie(FLASH_COOKIE, "");
                    c.setPath(request.getContextPath());
                    c.setMaxAge(0);
                    response.addCookie(c);
                    return map;
                }
            }
        }
        return new HashMap<String, String>();
    }

    /**
	 * Serialize the indicated map and return it in a 'flash cookie'
	 * @see #getFlash
	 * @see #serializeMap
	 * @see #deserializeMap
	 */
    private static Cookie makeFlashCookie(Map<String, String> map) {
        return new Cookie(FLASH_COOKIE, serializeMap(map));
    }

    /**
	 * Return a string representation of the map.
	 * This method encodes the map by writing its keys and values
	 * separated by the 0 character (the end of the list is indicated
	 * by an empty key) and then base64 encoding this string.
	 * @see #getFlash
	 * @see #makeFlashCookie
	 * @see #deserializeMap
	 */
    private static String serializeMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (String key : map.keySet()) {
            String val = map.get(key);
            if (val != null) {
                sb.append(key).append((char) 0).append(val).append((char) 0);
            }
        }
        sb.append((char) 0);
        return Base64Coder.encodeString(sb.toString());
    }

    /**
	 * Return the map representation of the indicated string (see serializeMap())
	 * @see #getFlash
	 * @see #makeFlashCookie
	 * @see #serializeMap
	 */
    private static Map<String, String> deserializeMap(String s) {
        s = Base64Coder.decodeString(s);
        Map<String, String> map = new HashMap<String, String>();
        int idx = 0;
        int idx2;
        while ((idx2 = s.indexOf((char) 0, idx)) != -1) {
            if (idx == idx2) {
                break;
            }
            String key = s.substring(idx, idx2);
            idx = idx2 + 1;
            idx2 = s.indexOf((char) 0, idx);
            if (idx2 == -1) {
                Log.error("Invalid serialized map.");
                break;
            }
            String val = s.substring(idx, idx2);
            map.put(key, val);
            idx = idx2 + 1;
        }
        return map;
    }
}
