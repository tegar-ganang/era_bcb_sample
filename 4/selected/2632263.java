package de.pannenleiter.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import de.pannenleiter.util.*;
import de.pannenleiter.db.brand.StandaloneFramework;
import de.apache.cocoon.ServletLogger;
import de.opus5.servlet.*;
import de.apache.cocoon.*;

/**
 * EngineBase -- the base class of a rmdms servlet
 *
 *
 */
public abstract class EngineBase extends HttpServlet implements ErrorHandler {

    protected static AttributeList emptyAttributes = new AttributeListImpl();

    protected int servedPages = 0;

    protected long sumRuntime = 0;

    protected Store store = new MemoryStore();

    public void init(ServletConfig config) throws ServletException {
        System.out.println("init");
        StandaloneFramework framework = new StandaloneFramework();
        String level = "error";
        try {
            level = framework.getProperty("log.level");
            System.out.println("log level = " + level);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ;
        ServletLogger.init(config.getServletContext(), level);
        super.init(config);
    }

    /**
   * This method is called by the servlet engine to handle the request.
   */
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contentType = "text/html";
        try {
            String setCache = request.getParameter("stylesheet");
            if (setCache != null) {
                if ("cleanup".equals(setCache)) {
                    if (store != null) store.freeAll();
                } else if ("on".equals(setCache)) {
                    store = new MemoryStore();
                } else if ("off".equals(setCache)) {
                    if (store != null) store.freeAll();
                    store = null;
                }
            }
            String basename = request.getPathTranslated();
            if (basename == null) {
                response.setContentType(contentType);
                PrintWriter out = new PrintWriter(response.getWriter());
                showStatus(out, request);
                return;
            }
            long time = System.currentTimeMillis();
            long diff;
            String requestType = request.getContentType();
            if (requestType != null && requestType.toLowerCase().startsWith("multipart/form-data")) {
                request = new HttpMultipartRequest(request, "/usr/tmp", "pl-upload-");
            }
            LogRequestSink trace = null;
            ResponseProxy proxy = null;
            HttpServletResponse rep = response;
            String suppress = request.getParameter("suppresstrace");
            if (Trace.getInstance().getTrace() && !"yes".equals(suppress)) {
                trace = new LogRequestSink();
                Trace.getInstance().setCurrent(trace);
            }
            try {
                if (trace != null) {
                    proxy = new ResponseProxy(response);
                    rep = proxy;
                    trace.setRequest(request);
                }
                if (basename.endsWith(".jar") || basename.endsWith(".js") || basename.endsWith(".gif") || basename.endsWith(".jpg")) {
                    contentType = "unknown/unknown";
                    Logger.getInstance().log(this, "Request: get" + basename, Logger.INFO);
                    response.setContentType(contentType);
                    copyService(request, rep);
                } else {
                    Reader xml = new FileReader(basename);
                    Hashtable pi;
                    try {
                        pi = getStylesheet(xml, basename);
                    } finally {
                        xml.close();
                    }
                    if (pi == null) {
                        throw new Exception("Can't find xml-stylesheet in " + basename);
                    }
                    if ("application/rmdms-raw".equals(pi.get("type"))) {
                        if (pi.get("content-type") != null) {
                            contentType = (String) pi.get("content-type");
                        } else {
                            contentType = "text/xml";
                        }
                        response.setContentType(contentType);
                        if ("readwrite".equals(pi.get("access"))) {
                            Logger.getInstance().log(this, "Request: raw readwrite " + basename, Logger.INFO);
                            rawService(request, rep, trace, true);
                        } else {
                            Logger.getInstance().log(this, "Request: raw readonly" + basename, Logger.INFO);
                            rawService(request, rep, trace, false);
                        }
                    } else {
                        if (pi.get("content-type") != null) {
                            contentType = (String) pi.get("content-type");
                        }
                        String stylesheet = (String) pi.get("href");
                        if (stylesheet == null) {
                            throw new Exception("Can't find a href for the stylesheet in " + basename);
                        }
                        int indexOfColon = stylesheet.indexOf(':');
                        int indexOfSlash = stylesheet.indexOf('/');
                        if ((indexOfColon != -1) && (indexOfSlash != -1) && (indexOfColon < indexOfSlash)) {
                        } else {
                            String base = basename;
                            int last = base.lastIndexOf("/");
                            if (last > 0) base = base.substring(0, last + 1);
                            stylesheet = base + stylesheet;
                        }
                        response.setContentType(contentType);
                        Logger.getInstance().log(this, "Request: xslt" + basename, Logger.INFO);
                        xslService(request, rep, trace, basename, stylesheet);
                    }
                }
            } catch (Exception e) {
                if (trace == null) {
                    trace = new LogRequestSink();
                    Trace.getInstance().setCurrent(trace);
                    trace.setRequest(request);
                    proxy = new ResponseProxy(response);
                }
                trace.setException(e);
                Logger.getInstance().log(this, e, "Error", Logger.ERROR);
                if ("text/html".equals(contentType)) {
                    htmlError(proxy, e);
                } else if ("text/vnd.wap.wml".equals(contentType)) {
                    wmlError(proxy, e);
                } else if ("text/xml".equals(contentType)) {
                    xmlError(proxy, e);
                } else {
                    response.setContentType("text/xml");
                    xmlError(proxy, e);
                }
            } finally {
                if (trace != null) {
                    trace.setResponse(proxy);
                    response.getWriter().print(proxy.getContent());
                }
                servedPages++;
                sumRuntime += System.currentTimeMillis() - time;
            }
        } catch (Exception e) {
            Logger.getInstance().log(this, e, "Error", Logger.ERROR);
            PrintWriter out = new PrintWriter(response.getWriter());
            out.println("Fatal error, see server log for details");
        }
    }

    public void rawService(HttpServletRequest request, HttpServletResponse response, LogSink log, boolean readwrite) throws Exception {
        PrintWriter out = new PrintWriter(response.getWriter());
        String todo = request.getParameter("todo");
        if ("fetch".equals(todo)) {
            DBWrapper.fetch(out, request.getParameter("db"), request.getParameter("document"), request.getParameter("pattern"), request.getParameter("fetch-childs"), request.getParameter("fetch-attributes"), request.getParameter("flags"), log);
        } else if ("archive".equals(todo)) {
            int id = 0;
            String tmp = request.getParameter("id");
            if (tmp != null) {
                id = Integer.parseInt(tmp);
            }
            DBWrapper.fetchArchive(out, request.getParameter("db"), request.getParameter("document"), id, request.getParameter("flags"), log);
        } else if ("write".equals(todo) && readwrite) {
            int pos = 0;
            String[] owner = null;
            if (request.getParameter("owner") != null) {
                owner = new String[1];
                owner[0] = request.getParameter("owner");
            }
            String tmp = request.getParameter("position");
            if (tmp != null) {
                pos = Integer.parseInt(tmp);
            }
            String[] id = DBWrapper.write(request.getParameter("db"), request.getParameter("document"), request.getParameter("xml"), owner, pos, log);
            out.println("<ok plid='" + id[0] + "'/>");
        } else if ("remove".equals(todo) && readwrite) {
            String[] id = null;
            if (request.getParameter("id") != null) {
                id = new String[1];
                id[0] = request.getParameter("id");
            }
            int version = DBWrapper.remove(request.getParameter("db"), request.getParameter("document"), id, log);
            out.println("<ok plversion='" + version + "'/>");
        } else if (todo != null) {
            throw new Exception("illegal request: " + todo + " write=" + readwrite);
        }
    }

    public abstract void xslService(HttpServletRequest request, HttpServletResponse response, LogSink trace, String basename, String stylesheet) throws Exception;

    public void copyService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Writer out = response.getWriter();
        String basename = request.getPathTranslated();
        FileReader in = new FileReader(basename);
        char[] buffer = new char[2048];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

    public void warning(SAXParseException e) {
        Logger.getInstance().log(this, e, "Parser exception", Logger.WARNING);
    }

    public void error(SAXParseException e) {
        Logger.getInstance().log(this, e, "Parser exception", Logger.ERROR);
    }

    public void fatalError(SAXParseException e) throws SAXException {
        Logger.getInstance().log(this, e, "Parser exception", Logger.CRITICAL);
        throw e;
    }

    public static void htmlError(ServletResponse response, Throwable t) {
        try {
            PrintWriter out = new PrintWriter(response.getWriter());
            out.println("<html>" + "<head>" + " <title>Pannenleiter Engine Internal Error</title>" + "</head>" + "<body>" + "<p><br></p>");
            out.println("<h1>" + t.getMessage() + "</h1>");
            for (int i = 0; i < 10 && t != null; i++) {
                if (t instanceof java.lang.reflect.InvocationTargetException) {
                    t = ((java.lang.reflect.InvocationTargetException) t).getTargetException();
                }
                StringWriter buffer = new StringWriter();
                if (t instanceof SAXParseException) {
                    String uri = ((SAXParseException) t).getSystemId();
                    out.println("URL:    " + uri);
                    out.println("Line:   " + ((SAXParseException) t).getLineNumber());
                    out.println("Column: " + ((SAXParseException) t).getColumnNumber());
                }
                out.print("<b>");
                out.print(t.getMessage());
                out.println("</b>");
                t.printStackTrace(new PrintWriter(buffer));
                out.print("<pre>");
                out.print(buffer.toString());
                out.println("</pre>");
                if (t instanceof SAXException) {
                    t = ((SAXException) t).getException();
                } else {
                    t = null;
                }
            }
            out.println("</body>" + "</html>");
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void xmlError(ServletResponse response, Throwable t) {
        try {
            PrintWriter out = new PrintWriter(response.getWriter());
            out.println("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
            out.println("<pannenleiter-exception>");
            for (int i = 0; i < 10 && t != null; i++) {
                if (t instanceof java.lang.reflect.InvocationTargetException) {
                    t = ((java.lang.reflect.InvocationTargetException) t).getTargetException();
                }
                StringWriter buffer = new StringWriter();
                if (t instanceof SAXParseException) {
                    String uri = ((SAXParseException) t).getSystemId();
                    out.println("<url>" + uri + "</url>");
                    out.println("<line>" + ((SAXParseException) t).getLineNumber() + "</line>");
                    out.println("<column>" + ((SAXParseException) t).getColumnNumber() + "</column>");
                }
                out.println("<message>" + XMLCollector.escape(t.getMessage()) + "</message>");
                out.println("<trace>");
                t.printStackTrace(new PrintWriter(buffer));
                out.print(XMLCollector.escape(buffer.toString()));
                out.println("</trace>");
                if (t instanceof SAXException) {
                    t = ((SAXException) t).getException();
                } else {
                    t = null;
                }
            }
            out.println("</pannenleiter-exception>");
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void wmlError(ServletResponse response, Throwable t) {
        try {
            PrintWriter out = new PrintWriter(response.getWriter());
            out.println("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
            out.println("<!DOCTYPE wml PUBLIC \"-//WAPFORUM//DTD WML 1.1//EN\" \"http://www.wapforum.org/DTD/wml_1.1.xml\">");
            out.println("<wml><card id='error' title='Internal Error'>\n");
            out.println("<p>Internal Error, see server log for details</p>");
            out.println("<do type='prev' name='Previous' label='Previous'><prev/></do>");
            out.println("</card></wml>");
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void showStatus(PrintWriter out, HttpServletRequest request) throws IOException {
        out.println("<html>" + "<head>" + " <title>Pannenleiter Engine Status</title>" + "</head>" + "<body>" + "<p><br></p>");
        out.println("de.pannenleiter.saxon.Engine<br>");
        out.println("served pages :" + servedPages + "<br>");
        if (servedPages > 0) out.println("average :" + sumRuntime / servedPages + " ms<br>");
        out.println("<b>de.pannenleiter.servlet.Engine?stylesheet=cleanup</b> purges the cache<br>");
        out.println("<b>de.pannenleiter.servlet.Engine?stylesheet=on</b> enables the stylesheet cache<br>");
        out.println("<b>de.pannenleiter.servlet.Engine?stylesheet=off</b> disables the stylesheet cache<br>");
        out.println(" </body>" + "</html>");
    }

    protected Hashtable getStylesheet(Reader r, String basename) throws IOException {
        while (true) {
            String line = getLine(r);
            if (line == null) return null;
            int start = line.indexOf("<?xml-stylesheet");
            int end = line.indexOf("?>");
            if (start >= 0 && end > start) {
                String pi = line.substring(start + 16, end);
                Hashtable attributes = new Hashtable();
                addPIPseudoAttributes(pi, attributes);
                return attributes;
            }
        }
    }

    protected void addPIPseudoAttributes(String data, Hashtable attributes) {
        Tokenizer st = new Tokenizer(data, "\"");
        try {
            while (st.hasMoreTokens()) {
                String key = st.nextToken();
                String token = st.nextToken();
                key = key.replace('=', ' ').trim();
                attributes.put(key, token);
            }
        } catch (NoSuchElementException nsee) {
        }
    }

    protected String getLine(Reader r) throws IOException {
        StringBuffer line = new StringBuffer();
        int c;
        while ((c = r.read()) != '\n') {
            if (c < 0) return null;
            line.append((char) c);
        }
        return line.toString();
    }
}
