package ch.ethz.mxquery.query.webservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import ch.ethz.mxquery.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ch.ethz.mxquery.query.XQCompiler;
import ch.ethz.mxquery.contextConfig.CompilerOptions;
import ch.ethz.mxquery.contextConfig.Context;
import ch.ethz.mxquery.query.PreparedStatement;
import ch.ethz.mxquery.query.impl.CompilerImpl;
import ch.ethz.mxquery.exceptions.MXQueryException;
import ch.ethz.mxquery.exceptions.QueryLocation;
import ch.ethz.mxquery.model.Iterator;
import ch.ethz.mxquery.model.XDMIterator;
import ch.ethz.mxquery.datamodel.QName;
import ch.ethz.mxquery.util.FileReader;
import ch.ethz.mxquery.xdmio.XDMAtomicItemFactory;
import ch.ethz.mxquery.xdmio.XDMSerializer;

public class XSPMain extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static Lock lock = new ReentrantLock();

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        File dir = new File(getServletContext().getRealPath("."));
        String requestURL = request.getRequestURL().toString();
        int i = requestURL.indexOf(request.getContextPath() + "/") + 1;
        if (i == 0) {
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", requestURL + "/");
            return;
        }
        int j = i + request.getContextPath().length();
        String fileName = requestURL.substring(j);
        if (fileName.equals("") || fileName.charAt(fileName.length() - 1) == '/') {
            if (new File(dir.getAbsolutePath() + "/index.xquery").exists()) fileName += "index.xquery";
            if (new File(dir.getAbsolutePath() + "/index.html").exists()) fileName += "index.html";
        }
        String filePath = dir.getAbsolutePath() + "/" + fileName;
        File queriedFile = new File(filePath);
        if (queriedFile.isDirectory()) {
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", request.getContextPath() + "/" + fileName + "/");
            return;
        }
        if (!queriedFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            PrintWriter print = response.getWriter();
            print.println("<html><body><H1>404 Not Found</H1>There is no query file named: " + filePath + "</body></html>");
            return;
        }
        try {
            if (queriedFile.getName().endsWith(".xquery")) {
                String query;
                query = FileReader.getFileContent(queriedFile.toURI().toString(), true);
                String queryResult = executeQuery(query, request, response);
                response.setContentType("text/html; charset=utf-8");
                PrintWriter print = response.getWriter();
                print.println(queryResult);
            } else if (queriedFile.getName().endsWith(".html")) {
                String html = FileReader.getFileContent(queriedFile.toURI().toString(), false);
                response.setContentType("text/html");
                PrintWriter print = response.getWriter();
                print.println(html);
            } else if (queriedFile.getName().endsWith(".css")) {
                response.setContentType("text/css");
                String css = FileReader.getFileContent(queriedFile.toURI().toString(), false);
                PrintWriter print = response.getWriter();
                print.println(css);
            } else if (queriedFile.getName().endsWith(".xml")) {
                String xml = FileReader.getFileContent(queriedFile.toURI().toString(), false);
                response.setContentType("text/xml; charset=utf-8");
                PrintWriter print = response.getWriter();
                print.println(xml);
            } else if (queriedFile.getName().endsWith(".js")) {
                String js = FileReader.getFileContent(queriedFile.toURI().toString(), false);
                response.setContentType("application/x-javascript; charset=utf-8");
                PrintWriter print = response.getWriter();
                print.println(js);
            } else if (queriedFile.getName().endsWith(".jpg")) {
                response.setContentType("image/jpg");
                OutputStream os = response.getOutputStream();
                InputStream is = new FileInputStream(queriedFile);
                int c = 0;
                while ((c = is.read()) != -1) os.write(c);
                os.close();
                is.close();
            } else if (queriedFile.getName().endsWith(".gif")) {
                response.setContentType("image/gif");
                OutputStream os = response.getOutputStream();
                InputStream is = new FileInputStream(queriedFile);
                int c = 0;
                while ((c = is.read()) != -1) os.write(c);
                os.close();
                is.close();
            } else if (queriedFile.getName().endsWith(".png")) {
                response.setContentType("image/png");
                OutputStream os = response.getOutputStream();
                InputStream is = new FileInputStream(queriedFile);
                int c = 0;
                while ((c = is.read()) != -1) os.write(c);
                os.close();
                is.close();
            }
        } catch (MXQueryException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            PrintWriter print = response.getWriter();
            print.println("<html><body><H1>404 Not Found</H1>There is no query file named: " + filePath + "</body></html>");
            return;
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    public String executeQuery(String query, HttpServletRequest request, HttpServletResponse response) {
        StringBuffer queryResult = new StringBuffer();
        Context ctx = new Context();
        File dir = new File(getServletContext().getRealPath("."));
        Context.getGlobalContext().setBaseURI(dir.toURI().toString());
        CompilerOptions co = new CompilerOptions();
        co.setSchemaAwareness(true);
        co.setXquery11(true);
        if (getServletConfig().getInitParameter("update").equals("true")) {
            co.setUpdate(true);
            ctx.getStores().setSerializeStores(true);
        }
        if (getServletConfig().getInitParameter("fulltext").equals("true")) {
            co.setFulltext(true);
        }
        if (getServletConfig().getInitParameter("scripting").equals("true")) {
            co.setScripting(true);
        }
        XQCompiler compiler = new CompilerImpl();
        PreparedStatement statement = null;
        try {
            statement = compiler.compile(ctx, query, co, null, null);
        } catch (MXQueryException err) {
            response.setStatus(500);
            displayErrorMessage(queryResult, query, err, false);
            err.printStackTrace();
            return queryResult.toString();
        }
        Hashtable h = ctx.getAllVariables();
        Set vars = h.keySet();
        Map m = request.getParameterMap();
        try {
            while (!lock.tryLock()) ;
            try {
            } catch (Exception e) {
            }
            java.util.Iterator it = vars.iterator();
            while (it.hasNext()) {
                QName name = (QName) it.next();
                if (ctx.getVariable(name).isExternal()) {
                    String param = name.getLocalPart();
                    String value = "";
                    value = request.getParameter(param);
                    if (value == null) value = "";
                    statement.addExternalResource(new QName(param), XDMAtomicItemFactory.createString(value));
                }
            }
            if (statement != null) {
                XDMIterator result = statement.evaluate();
                if (!statement.isWebService()) {
                    XDMSerializer ip = new XDMSerializer();
                    String strResult = ip.eventsToXML(result);
                    queryResult.append(strResult);
                }
                if (result.getExpressionCategoryType(true) == Iterator.EXPR_CATEGORY_UPDATING) {
                    statement.applyPUL();
                    if (request.getParameterMap().keySet().contains("redirect")) {
                        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                        String newLocation = request.getContextPath() + "/" + request.getParameter("redirect");
                        boolean first = true;
                        java.util.Iterator paramIt = m.keySet().iterator();
                        while (paramIt.hasNext()) {
                            String param = (String) paramIt.next();
                            if (param.equals("redirect")) continue;
                            if (first) {
                                newLocation += "?";
                                first = false;
                            } else newLocation += "&";
                            newLocation += param + "=" + request.getParameter(param);
                        }
                        response.setHeader("Location", newLocation);
                    }
                }
                statement.serializeStores(true);
            }
        } catch (MXQueryException err) {
            response.setStatus(500);
            displayErrorMessage(queryResult, query, err, true);
        } catch (IOException err) {
            response.setStatus(500);
            err.printStackTrace();
            queryResult.append("<html><head><title>HTTP Status 500</title></head>");
            queryResult.append("<body style='background-color: #FFDDDD'>");
            queryResult.append("<h1 style='color: red; font-family: Arial'>500 - An error occured on the server</h1>");
            queryResult.append("<div style='color: #220000; font-family: Arial'><p>Tomcat output the following error:</p></div><hr/>");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            err.printStackTrace(pw);
            pw.flush();
            queryResult.append("<div style='font-size: 10pt'><pre>" + sw.getBuffer() + "</pre></div>");
            queryResult.append("<hr/>");
            queryResult.append("</body></html>");
        } finally {
            lock.unlock();
        }
        return queryResult.toString();
    }

    public String encodeToXML(String input) {
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < input.length(); i++) {
            String c = "" + input.charAt(i);
            if (c.equals("<")) c = "&lt;";
            if (c.equals(">")) c = "&gt;";
            if (c.equals("&")) c = "&amp;";
            if (c.equals("\"")) c = "&quot;";
            if (c.equals("\'")) c = "&apos;";
            sb.append(c);
        }
        return sb.toString();
    }

    public void displayErrorMessage(StringBuffer queryResult, String query, MXQueryException err, boolean compiled) {
        queryResult.append("<html><head><title>HTTP Status 500</title></head>");
        queryResult.append("<body style='background-color: #FFDDDD'>");
        queryResult.append("<h1 style='color: red; font-family: Arial'>500 Internal Server Error</h1>");
        queryResult.append("<div style='position: absolute; left: 0px; top: 0px; z-index: -1; color: #FFEEEE; font-size: 100pt'>500</div>");
        queryResult.append("<div style='color: #220000; font-family: Arial'><p>Something went wrong on the server. The following information might help you further.</p></div>");
        queryResult.append("<hr color='red'/>");
        if (compiled) queryResult.append("<div style='color: #220000; font-family: Arial'><p>MXQuery successfully compiled, but output the following error during execution:</p></div>"); else queryResult.append("<div style='color: #220000; font-family: Arial'><p>MXQuery output the following error during compilation:</p></div>");
        String[] queryLines = query.split("\n");
        QueryLocation loc = err.getLocation();
        loc.translateIndexToLineColumn(query);
        int lineStart = loc.getLineBegin();
        int columnStart = loc.getColumnBegin();
        queryResult.append("<div style='font-family: Arial'>Line " + lineStart + ", Column " + columnStart + ": <span style='text-weight: bold; color: red'>" + err.getErrorCode() + "</span> " + err.getMessage() + "</div>");
        queryResult.append("<p/>");
        queryResult.append("<div style='background-color: #DDDDDD; padding: 10px; border: 5px solid black'><pre>");
        for (int i = lineStart - 5 >= 1 ? lineStart - 5 : 1; i <= lineStart; i++) queryResult.append(i + ": " + encodeToXML(queryLines[i - 1]) + "\n");
        for (int i = 0; i < columnStart + 2; i++) queryResult.append(" ");
        queryResult.append("<span style='color: red'>|here</span>\n");
        for (int i = lineStart + 1; i <= (lineStart + 5 <= queryLines.length ? lineStart + 5 : queryLines.length); i++) queryResult.append(i + ": " + encodeToXML(queryLines[i - 1]) + "\n");
        queryResult.append("</pre></div>");
        queryResult.append("</body></html>");
    }
}
