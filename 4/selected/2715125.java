package se.snigel.net.servlet;

import org.apache.regexp.RE;
import se.snigel.net.servlet.servlets.FileBrowser;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;

/**
 * User: kalle
 * Date: 2004-mar-17
 * Time: 07:07:13
 */
public class ServletServerThread implements Runnable {

    private StringBuffer clientHeader = new StringBuffer();

    private ServletRequest request;

    private ServletResponse response;

    private ServletServer server;

    private ServletSession session = null;

    private Socket socket;

    public ServletServer getServer() {
        return server;
    }

    public ServletServerThread(ServletServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    public StringBuffer getClientHeader() {
        return clientHeader;
    }

    public ServletRequest getRequest() {
        return request;
    }

    public ServletResponse getResponse() {
        return response;
    }

    public ServletSession getSession() {
        return session;
    }

    public Socket getSocket() {
        return socket;
    }

    public void run() {
        try {
            InputStreamReader isrSocketIn = new InputStreamReader(socket.getInputStream());
            BufferedReader br = new BufferedReader(isrSocketIn);
            String command = br.readLine().trim();
            StringBuffer log = new StringBuffer();
            log.append(socket.getRemoteSocketAddress().toString());
            log.append(" ");
            log.append(command);
            System.out.println(log.toString());
            clientHeader = new StringBuffer();
            clientHeader.append(command);
            clientHeader.append('\n');
            request = new ServletRequest(this);
            response = new ServletResponse(this);
            String line;
            while ((line = br.readLine()) != null) {
                if ("".equals(line)) break;
                clientHeader.append(line);
                clientHeader.append('\n');
                RE re = new RE("(.*?): (.*?)$");
                if (re.match(line)) {
                    getRequest().getHeaderMap().put(re.getParen(1), re.getParen(2));
                    if ("Cookie".equalsIgnoreCase(re.getParen(1))) {
                        String cookies = re.getParen(2).trim();
                        if (!cookies.endsWith(";")) cookies = cookies + ";";
                        int keyStart = 0;
                        while (keyStart > -1) {
                            int keyEnd = cookies.indexOf('=', keyStart);
                            int valStart = StringUtils.indexOf(cookies, new String[] { "\"", "'", ";" }, keyEnd);
                            int valEnd;
                            if (cookies.charAt(valStart) == ';') {
                                valEnd = valStart;
                                valStart = keyEnd + 1;
                            } else {
                                valEnd = cookies.indexOf(cookies.charAt(valStart), valStart + 1);
                                valStart++;
                            }
                            if (keyStart > -1 && keyEnd > -1 && valStart > -1 && valEnd > -1) {
                                getRequest().setCookie(cookies.substring(keyStart, keyEnd).trim(), cookies.substring(valStart, valEnd).trim());
                                keyStart = cookies.indexOf(';', valEnd);
                                if (keyStart > -1) keyStart++;
                            } else break;
                        }
                    }
                } else {
                    throw new IOException("Invalid header: " + line);
                }
            }
            String sessionID = getRequest().getCookie("SESSIONSNIGEL");
            if (sessionID != null) {
                session = server.getSession(sessionID);
            } else {
                sessionID = String.valueOf(System.currentTimeMillis());
                session = server.getSession(sessionID);
                getRequest().setCookie("SESSIONSNIGEL", sessionID);
            }
            RE re = new RE("([a-zA-Z]+) (.*?) (.*?)$");
            re.match(command);
            String httpVersion = re.getParen(3);
            String url = re.getParen(2);
            String method = re.getParen(1);
            if (url != null) {
                String parameters;
                String contextPath;
                int argStart = url.indexOf("?");
                if (argStart > -1) {
                    parameters = url.substring(argStart + 1, url.length());
                    contextPath = url.substring(0, argStart);
                } else {
                    parameters = null;
                    contextPath = url;
                }
                request.setContextPath(StringUtils.urlDecode(contextPath));
                Servlet servlet = server.getServlet(contextPath);
                if (servlet == null) {
                    if (server.getPublicRoot() != null) {
                        File requestedFile = new File(server.getPublicRoot(), url);
                        sendFile(requestedFile);
                    } else {
                        response.sendError(404, "Unknown context. (No public root path set)");
                    }
                } else {
                    if (parameters != null) addURLEncodedParameters(parameters);
                    if ("GET".equalsIgnoreCase(method)) {
                        servlet.doGet(request, response);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        if (request.getHeaderMap().get("Content-Length") == null) {
                            response.sendError(411, "Content-Length expected");
                            return;
                        }
                        int contentLength = Integer.parseInt((String) request.getHeaderMap().get("Content-Length"));
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int in;
                        for (int counter = 0; counter < contentLength; counter++) {
                            in = br.read();
                            if (in == -1) break; else baos.write(in);
                        }
                        byte[] postData = baos.toByteArray();
                        baos.close();
                        String contentType = (String) request.getHeaderMap().get("Content-Type");
                        if (contentType == null) {
                            response.sendError(400, "Content-Type not set.");
                            return;
                        }
                        if ("application/x-www-form-urlencoded".equals(contentType)) {
                            addURLEncodedParameters(new String(postData));
                        } else if (contentType.startsWith("multipart/form-data")) {
                            String mimeData = new String(postData);
                            String boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length());
                            int pos = 0;
                            while (pos > -1) {
                                int boundayIndexStart = mimeData.indexOf(boundary, pos);
                                int boundayIndexEnd = mimeData.indexOf(boundary, boundayIndexStart + boundary.length());
                                if (boundayIndexEnd > -1) {
                                    String mimeSegment = mimeData.substring(boundayIndexStart + boundary.length(), boundayIndexEnd - 2).trim() + '\n';
                                    BufferedReader segmentbr = new BufferedReader(new StringReader(mimeSegment));
                                    HashMap mimeHeaders = new HashMap();
                                    String mimeHeader;
                                    while ((mimeHeader = segmentbr.readLine()) != null && !mimeHeader.equals("")) {
                                        int seperatorPos = mimeHeader.indexOf(":");
                                        String key = mimeHeader.substring(0, seperatorPos).trim();
                                        String value = mimeHeader.substring(seperatorPos + 1, mimeHeader.length()).trim();
                                        mimeHeaders.put(key, value);
                                    }
                                    baos = new ByteArrayOutputStream();
                                    int i;
                                    while ((i = segmentbr.read()) > -1) baos.write(i);
                                    RE reParameters = new RE("form-data; name=\"(.*?)\"(; filename=\"(.*?)\")?");
                                    reParameters.match((String) mimeHeaders.get("Content-Disposition"));
                                    if (reParameters.getParen(1) != null) {
                                        String key = reParameters.getParen(1);
                                        String value;
                                        if (reParameters.getParen(3) != null) {
                                            value = reParameters.getParen(3);
                                            ServletRequest.FormData formData = request.new FormData(baos.toByteArray(), (String) mimeHeaders.get("Content-Type"), value);
                                            request.formDatas.put(key, formData);
                                        } else {
                                            value = new String(baos.toByteArray());
                                        }
                                        addParameter(key, value.trim());
                                    }
                                }
                                pos = boundayIndexEnd;
                            }
                        } else {
                            response.sendError(406, "Unsupported Content-Type: " + contentType);
                            return;
                        }
                        servlet.doPost(request, response);
                    } else {
                        response.sendError(500, "Command not implemented.");
                    }
                }
            }
            response.getWriter().flush();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                e.printStackTrace(response.getWriter());
                response.getWriter().flush();
                socket.close();
            } catch (IOException ee) {
            }
        }
    }

    private void addURLEncodedParameters(String parameters) {
        if (!parameters.startsWith("?")) parameters = "?" + parameters;
        int pos = 0;
        while (pos > -1) {
            int keyStart = StringUtils.indexOf(parameters, new String[] { "&", "?" }, pos);
            if (keyStart > -1) {
                keyStart++;
                int keyStop = StringUtils.indexOf(parameters, new String[] { "=", "&" }, keyStart);
                String key;
                String value = null;
                if (keyStop > -1 && '=' == parameters.charAt(keyStop)) {
                    key = StringUtils.urlDecode(parameters.substring(keyStart, keyStop));
                    int valStart = keyStop + 1;
                    int valStop = StringUtils.indexOf(parameters, new String[] { "&" }, valStart);
                    if (valStop == -1) valStop = parameters.length();
                    pos = valStop;
                    value = StringUtils.urlDecode(parameters.substring(valStart, valStop));
                } else if (keyStop > -1) {
                    key = StringUtils.urlDecode(parameters.substring(keyStart, keyStop));
                    pos = keyStop;
                } else {
                    key = StringUtils.urlDecode(parameters.substring(keyStart, parameters.length()));
                    pos = keyStop;
                }
                addParameter(key, value);
            } else {
                break;
            }
        }
    }

    private void addParameter(String key, String value) {
        String[] arr;
        String[] currentValues = (String[]) request.getParameterMap().get(key);
        if (currentValues != null) {
            arr = new String[1 + currentValues.length];
            for (int i = 0; i < currentValues.length; i++) arr[i] = currentValues[i];
            arr[arr.length - 1] = value;
        } else {
            arr = new String[] { value };
        }
        request.getParameterMap().put(key, arr);
    }

    public void sendFile(File file) throws IOException {
        if (!file.exists()) {
            response.sendError(404, "File not found.");
        } else {
            if (file.isFile()) {
                try {
                    String fileExtention = file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length());
                    if ("png".equalsIgnoreCase(fileExtention)) response.setContentType("image/png"); else if ("gif".equalsIgnoreCase(fileExtention)) response.setContentType("image/gif"); else if ("jpg".equalsIgnoreCase(fileExtention) || "jpeg".equalsIgnoreCase(fileExtention)) response.setContentType("image/jpeg"); else if ("txt".equalsIgnoreCase(fileExtention)) response.setContentType("text/plain"); else if ("css".equalsIgnoreCase(fileExtention)) response.setContentType("text/css"); else if ("html".equalsIgnoreCase(fileExtention) || "htm".equalsIgnoreCase(fileExtention)) response.setContentType("text/html");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                FileInputStream fis = new FileInputStream(file);
                int i;
                while ((i = fis.read()) != -1) response.getWriter().write(i);
                fis.close();
            } else if (file.isDirectory()) {
                new FileBrowser(file).doGet(request, response);
            }
        }
    }
}
