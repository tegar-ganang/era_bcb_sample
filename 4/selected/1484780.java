package mhhc.htmlcomponents;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class WebServer {

    public static void main(String args[]) throws Exception {
        int PORT = 7000;
        ServerSocket listenSocket = new ServerSocket(PORT);
        while (true) {
            HttpRequest request = new HttpRequest(listenSocket.accept());
            Thread thread = new Thread(request);
            thread.start();
        }
    }
}

final class HttpRequest implements Runnable {

    static final String CRLF = "\r\n";

    Socket socket;

    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }

    public void run() {
        try {
            System.out.println("-- begin request --");
            processRequest();
            System.out.println("-- end request  --");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void processRequest() throws Exception {
        InputStream is = this.socket.getInputStream();
        DataOutputStream os = new DataOutputStream(this.socket.getOutputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String requestLine = br.readLine();
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken();
        String fileName = tokens.nextToken();
        System.out.println("request for: " + fileName);
        if (fileName.indexOf("controller") != -1) {
            HttpServletRequest request = buildRequest(fileName);
            HttpServletResponse response = buildResponse();
            ServletConfig servletConfig = buildConfig();
            FrontController fc = new FrontController();
            fc.init(servletConfig);
            long starttime = System.currentTimeMillis();
            fc.service(request, response);
            System.out.println("Request took " + (System.currentTimeMillis() - starttime) + " ms.");
            String statusLine = null;
            String contentTypeLine = null;
            String entityBody = null;
            statusLine = "HTTP/1.0 200 OK" + CRLF;
            contentTypeLine = "Content-type: text/xml" + CRLF;
            os.writeBytes(statusLine);
            os.writeBytes(contentTypeLine);
            os.writeBytes(CRLF);
            ByteArrayInputStream bais = new ByteArrayInputStream(response.getWriter().toString().getBytes());
            System.out.println("response with: " + response.getWriter().toString());
            sendBytes(bais, os);
            bais.close();
            os.close();
            br.close();
            socket.close();
        } else {
            String filepath = "C:/eclipse/workspace/htmlcomponents/j2ee/src/web/src/webapp/" + fileName.substring("/htmlcomponents/".length());
            FileInputStream fis = null;
            boolean fileExists = true;
            try {
                fis = new FileInputStream(filepath);
            } catch (FileNotFoundException e) {
                fileExists = false;
            }
            String statusLine = null;
            String contentTypeLine = null;
            String entityBody = null;
            if (fileExists) {
                statusLine = "HTTP/1.0 200 OK" + CRLF;
                contentTypeLine = "Content-type: " + contentType(filepath) + CRLF;
            } else {
                statusLine = "HTTP/1.0 404 Not Found" + CRLF;
                contentTypeLine = "NONE";
                entityBody = "\n\n Not Found";
            }
            os.writeBytes(statusLine);
            os.writeBytes(contentTypeLine);
            os.writeBytes(CRLF);
            if (fileExists) {
                sendBytes(fis, os);
                fis.close();
            } else {
                os.writeBytes(entityBody);
            }
            os.close();
            br.close();
            socket.close();
        }
    }

    private String contentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) return "text/html"; else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg"; else if (fileName.endsWith(".gif")) return "image/gif"; else if (fileName.endsWith(".txt")) return "text/plain"; else if (fileName.endsWith(".js")) return "text/plain"; else return "application/octet-stream";
    }

    private static void sendBytes(InputStream fis, OutputStream os) throws Exception {
        byte[] buffer = new byte[1024];
        int bytes = 0;
        while ((bytes = fis.read(buffer)) != -1) os.write(buffer, 0, bytes);
    }

    private HttpServletRequest buildRequest(final String fileName) {
        return new HttpServletRequest() {

            Map attributeMap = new HashMap();

            public String getAuthType() {
                return null;
            }

            public Cookie[] getCookies() {
                return null;
            }

            public long getDateHeader(String arg0) {
                return 0;
            }

            public String getHeader(String arg0) {
                return null;
            }

            public Enumeration getHeaders(String arg0) {
                return null;
            }

            public Enumeration getHeaderNames() {
                return null;
            }

            public int getIntHeader(String arg0) {
                return 0;
            }

            public String getMethod() {
                return null;
            }

            public String getPathInfo() {
                return null;
            }

            public String getPathTranslated() {
                return null;
            }

            public String getContextPath() {
                return null;
            }

            public String getQueryString() {
                int qm = fileName.indexOf("?");
                if (qm == -1) return ""; else return fileName.substring(qm + 1);
            }

            public String getRemoteUser() {
                return null;
            }

            public boolean isUserInRole(String arg0) {
                return false;
            }

            public Principal getUserPrincipal() {
                return null;
            }

            public String getRequestedSessionId() {
                return null;
            }

            public String getRequestURI() {
                return null;
            }

            public StringBuffer getRequestURL() {
                return null;
            }

            public String getServletPath() {
                return null;
            }

            public HttpSession getSession(boolean arg0) {
                return null;
            }

            public HttpSession getSession() {
                return null;
            }

            public boolean isRequestedSessionIdValid() {
                return false;
            }

            public boolean isRequestedSessionIdFromCookie() {
                return false;
            }

            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            public boolean isRequestedSessionIdFromUrl() {
                return false;
            }

            public Object getAttribute(String arg0) {
                return attributeMap.get(arg0);
            }

            public Enumeration getAttributeNames() {
                return new Enumeration() {

                    Iterator iter = attributeMap.keySet().iterator();

                    public boolean hasMoreElements() {
                        return iter.hasNext();
                    }

                    public Object nextElement() {
                        return iter.next();
                    }
                };
            }

            public String getCharacterEncoding() {
                return null;
            }

            public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
            }

            public int getContentLength() {
                return 0;
            }

            public String getContentType() {
                return null;
            }

            public ServletInputStream getInputStream() throws IOException {
                return null;
            }

            public String getParameter(String arg0) {
                return (String) getParameterMap().get(arg0);
            }

            public Enumeration getParameterNames() {
                return new Enumeration() {

                    Iterator iter = getParameterMap().keySet().iterator();

                    public boolean hasMoreElements() {
                        return iter.hasNext();
                    }

                    public Object nextElement() {
                        return iter.next();
                    }
                };
            }

            public String[] getParameterValues(String arg0) {
                return null;
            }

            public Map getParameterMap() {
                Map map = new HashMap();
                String query = getQueryString();
                StringTokenizer st = new StringTokenizer(query, "&");
                while (st.hasMoreTokens()) {
                    String elem = st.nextToken();
                    int ei = elem.indexOf("=");
                    if (ei != -1) {
                        map.put(elem.substring(0, ei), elem.substring(ei + 1));
                    } else {
                        map.put(elem, null);
                    }
                }
                return map;
            }

            public String getProtocol() {
                return null;
            }

            public String getScheme() {
                return null;
            }

            public String getServerName() {
                return null;
            }

            public int getServerPort() {
                return 0;
            }

            public BufferedReader getReader() throws IOException {
                return null;
            }

            public String getRemoteAddr() {
                return null;
            }

            public String getRemoteHost() {
                return null;
            }

            public void setAttribute(String arg0, Object arg1) {
                attributeMap.put(arg0, arg1);
            }

            public void removeAttribute(String arg0) {
            }

            public Locale getLocale() {
                return null;
            }

            public Enumeration getLocales() {
                return null;
            }

            public boolean isSecure() {
                return false;
            }

            public RequestDispatcher getRequestDispatcher(String arg0) {
                return null;
            }

            public String getRealPath(String arg0) {
                return null;
            }
        };
    }

    private HttpServletResponse buildResponse() {
        return new HttpServletResponse() {

            StringWriter sw = new StringWriter();

            PrintWriter writer = new PrintWriter(sw) {

                public String toString() {
                    return sw.toString();
                }
            };

            public void addCookie(Cookie arg0) {
            }

            public boolean containsHeader(String arg0) {
                return false;
            }

            public String encodeURL(String arg0) {
                return null;
            }

            public String encodeRedirectURL(String arg0) {
                return null;
            }

            public String encodeUrl(String arg0) {
                return null;
            }

            public String encodeRedirectUrl(String arg0) {
                return null;
            }

            public void sendError(int arg0, String arg1) throws IOException {
            }

            public void sendError(int arg0) throws IOException {
            }

            public void sendRedirect(String arg0) throws IOException {
            }

            public void setDateHeader(String arg0, long arg1) {
            }

            public void addDateHeader(String arg0, long arg1) {
            }

            public void setHeader(String arg0, String arg1) {
            }

            public void addHeader(String arg0, String arg1) {
            }

            public void setIntHeader(String arg0, int arg1) {
            }

            public void addIntHeader(String arg0, int arg1) {
            }

            public void setStatus(int arg0) {
            }

            public void setStatus(int arg0, String arg1) {
            }

            public String getCharacterEncoding() {
                return null;
            }

            public ServletOutputStream getOutputStream() throws IOException {
                return null;
            }

            public PrintWriter getWriter() throws IOException {
                return writer;
            }

            public void setContentLength(int arg0) {
            }

            public void setContentType(String arg0) {
            }

            public void setBufferSize(int arg0) {
            }

            public int getBufferSize() {
                return 0;
            }

            public void flushBuffer() throws IOException {
            }

            public void resetBuffer() {
            }

            public boolean isCommitted() {
                return false;
            }

            public void reset() {
            }

            public void setLocale(Locale arg0) {
            }

            public Locale getLocale() {
                return null;
            }
        };
    }

    private ServletConfig buildConfig() {
        return new ServletConfig() {

            public String getServletName() {
                return null;
            }

            public ServletContext getServletContext() {
                return null;
            }

            public String getInitParameter(String arg0) {
                if ("default.eventlistener".equals(arg0)) return "mhhc.htmlcomponents.example.MyPageBuilder"; else return null;
            }

            public Enumeration getInitParameterNames() {
                return null;
            }
        };
    }
}
