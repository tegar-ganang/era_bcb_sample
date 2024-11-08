package ws.system;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class HTTPRequest {

    private HashMap<String, String> headers = null;

    private HTTPurl urlData = null;

    private OutputStream outStream = null;

    private DataStore store = null;

    private SimpleDateFormat df = null;

    public HTTPRequest(String req, HashMap<String, String> head, byte[] postData, OutputStream out) throws Exception {
        outStream = out;
        headers = head;
        store = DataStore.getInstance();
        urlData = new HTTPurl(req, postData, head);
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        if ("1".equals(store.getProperty("security.accesslog"))) {
            String[] params = urlData.getParameterList();
            String paramList = "";
            if (params.length > 0) {
                for (int x = 0; x < params.length; x++) {
                    String value = urlData.getParameter(params[x]);
                    if (value.length() > 160) {
                        value = value.substring(160) + "(truncated)";
                    }
                    paramList += "\r\nAccessLog : Param(" + (x + 1) + ") : " + params[x] + "=" + value;
                }
            }
            System.out.println("AccessLog : " + df.format(new Date()) + " : " + urlData.getRequestType() + " : " + urlData.getReqString() + paramList);
        }
    }

    public void sendResponseData() {
        try {
            if (store.timerStatus == -1 || store.adminStatus == -1) {
                String timerError = "<html><head><title>Thread Error</title></head><body>" + "<h1>Thread Error</h1>The main admin or timer thread is not running, it has crash with the following error:<p>" + "<hr>" + "<pre>Timer Thread StackTrace:\n" + store.timerThreadErrorStack + "</pre><p>" + "<hr>" + "<pre>Admin Thread StackTrace:\n" + store.adminThreadErrorStack + "</pre><p>" + "<hr>" + "Use this information in any error report you submit." + "</body>";
                outStream.write(timerError.getBytes());
                return;
            }
            AccessControl ac = AccessControl.getInstance();
            if (ac.authenticateUser(headers, urlData) == false) {
                System.out.println("Access denied from IP : " + headers.get("RemoteAddress"));
                StringBuffer out = new StringBuffer(4096);
                out.append("HTTP/1.0 401 Unauthorized\r\n");
                out.append("WWW-Authenticate: BASIC realm=\"TV Scheduler Pro\"\r\n");
                out.append("Cache-Control: no-cache\r\n\r\nAccess denied for area.");
                outStream.write(out.toString().getBytes());
                return;
            } else if (urlData.getRequestType() == 3) {
                if (urlData.getServletMethod().length() == 0) {
                    Class<?> paramTypes[] = {};
                    Constructor<?> c = Class.forName("ws.system." + urlData.getServletClass()).getConstructor(paramTypes);
                    Object params[] = {};
                    Object resp = (Object) c.newInstance(params);
                    ((HTTPResponse) resp).getResponse(urlData, outStream, headers);
                } else {
                    Class<?> paramTypes[] = {};
                    Constructor<?> c = Class.forName(urlData.getServletClass()).getConstructor(paramTypes);
                    Object params[] = {};
                    Object resp = (Object) c.newInstance(params);
                    Method m = resp.getClass().getMethod(urlData.getServletMethod(), new Class[] { HTTPurl.class, HashMap.class, OutputStream.class });
                    m.invoke(resp, urlData, headers, outStream);
                }
                return;
            } else if (urlData.getRequestType() == 2) {
                returnFileContent(urlData.getReqString());
                return;
            } else if (urlData.getRequestType() == 1) {
                returnFileContent("/index.html");
                return;
            }
            PageTemplate page = new PageTemplate(store.getProperty("path.template") + File.separator + "error.html");
            page.replaceAll("$error", "Request not known\n\n" + requestInfo());
            outStream.write(page.getPageBytes());
        } catch (Exception e) {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            PrintWriter err = new PrintWriter(ba);
            try {
                e.printStackTrace(err);
                err.flush();
                PageTemplate page = new PageTemplate(store.getProperty("path.template") + File.separator + "error.html");
                page.replaceAll("$error", HTMLEncoder.encode(urlData.toString()) + "\n\n" + HTMLEncoder.encode(ba.toString()));
                outStream.write(page.getPageBytes());
            } catch (Exception e2) {
                try {
                    outStream.write(ba.toString().getBytes());
                } catch (Exception e3) {
                }
            }
            System.out.println("HTTP Request Exception: " + e);
            e.printStackTrace();
        }
    }

    private int returnFileContent(String urlString) throws Exception {
        boolean doRange = false;
        long rangeStart = -1;
        long rangeEnd = -1;
        long dataSent = 0;
        long totalDataToSend = 0;
        urlString = urlString.replaceAll("\\+", "%2B");
        String fileName = URLDecoder.decode(urlString, "UTF-8");
        String[] capPathStrings = store.getCapturePaths();
        boolean capPathFound = false;
        for (int x = 0; x < capPathStrings.length; x++) {
            if (fileName.indexOf("/$path" + x + "$") > -1) {
                capPathFound = true;
                fileName = fileName.replace("/$path" + x + "$", capPathStrings[x]);
                break;
            }
        }
        if (capPathFound == false) {
            fileName = store.getProperty("path.httproot") + fileName;
        }
        int index = fileName.indexOf("?");
        if (index > -1) {
            fileName = fileName.substring(0, index);
        }
        File thisFile = new File(fileName);
        String requestedFilePath = thisFile.getCanonicalPath();
        File root = new File(store.getProperty("path.httproot"));
        String rootFilePath = root.getCanonicalPath();
        boolean isOutOfBouns = true;
        for (int x = 0; x < capPathStrings.length; x++) {
            if (requestedFilePath.indexOf(new File(capPathStrings[x]).getCanonicalPath()) == 0) {
                isOutOfBouns = false;
                break;
            }
        }
        if (isOutOfBouns == true && requestedFilePath.indexOf(rootFilePath) < 0) {
            throw new Exception("File out of bounds! (" + thisFile.getCanonicalPath() + ")");
        }
        if (thisFile.getName().equals("dir.list")) {
            StringBuffer data = new StringBuffer();
            data.append("HTTP/1.0 200 OK\n");
            data.append("Content-Type: text/html\n");
            data.append("\n");
            data.append("<html>");
            data.append("<body>\n");
            File[] files = thisFile.getParentFile().listFiles();
            if (files != null) {
                for (int x = 0; x < files.length; x++) {
                    if (files[x].isDirectory() && files[x].isHidden() == false) {
                        data.append("<a href=\"" + files[x].getName() + "/dir.list\">[" + files[x].getName() + "]</a><br>\n");
                    } else if (files[x].isHidden() == false) {
                        data.append("<a href=\"" + files[x].getName() + "\">" + files[x].getName() + "</a><br>\n");
                    }
                }
            } else {
                data.append("Path not found!<br>\n");
            }
            data.append("</body>");
            data.append("</html>\n");
            outStream.write(data.toString().getBytes());
            return 1;
        }
        if (thisFile.exists() == false) {
            System.out.println("HTTP 404 - File not found (" + thisFile.getAbsolutePath() + ")");
            return 0;
        }
        FileInputStream fi = new FileInputStream(thisFile);
        long fileLength = thisFile.length();
        String rangeString = headers.get("Range");
        if (rangeString != null && rangeString.startsWith("bytes=")) {
            doRange = true;
            rangeString = rangeString.substring("bytes=".length());
            System.out.println(this + " - Doing a Ranged Return (" + rangeString + ")");
            if (rangeString.startsWith("-")) {
                rangeEnd = Long.parseLong(rangeString.substring(1));
                rangeStart = fileLength - rangeEnd;
                rangeEnd = fileLength;
            } else if (rangeString.endsWith("-")) {
                rangeStart = Long.parseLong(rangeString.substring(0, rangeString.length() - 1));
                rangeEnd = fileLength;
            } else {
                String[] bits = rangeString.split("-");
                rangeStart = Long.parseLong(bits[0]);
                rangeEnd = Long.parseLong(bits[1]);
            }
            System.out.println(this + " - Range (" + rangeStart + "-" + rangeEnd + ")");
        }
        int read = 0;
        byte[] bytes = new byte[4096];
        try {
            String header = "";
            if (doRange) {
                header += "HTTP/1.0 206 OK\n";
                header += "Content-Length: " + ((rangeEnd - rangeStart) + 1) + "\n";
                header += "Content-Range: bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength + "\n";
                System.out.println(this + " - Content-Length: " + ((rangeEnd - rangeStart) + 1));
                System.out.println(this + " - Content-Range: bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            } else {
                header += "HTTP/1.0 200 OK\n";
                header += "Content-Length: " + fileLength + "\n";
            }
            HashMap<String, String> mineTypes = store.getMimeTypes();
            String mineType = "application/octet-stream";
            String ext = "";
            int lastDot = thisFile.getName().lastIndexOf(".");
            if (lastDot > -1 && lastDot != thisFile.length()) {
                ext = thisFile.getName().toLowerCase().substring(lastDot + 1);
                mineType = mineTypes.get(ext);
                if (mineType == null) {
                    mineType = "application/octet-stream";
                }
            }
            header += "Content-Type: " + mineType + "\n";
            header += "Accept-Ranges: bytes\n";
            header += "\n";
            outStream.write(header.getBytes());
            totalDataToSend = 0;
            if (doRange) {
                fi.skip(rangeStart);
                totalDataToSend = (rangeEnd - rangeStart) + 1;
                System.out.println(this + " - totalDataToSend = " + totalDataToSend);
            } else {
                totalDataToSend = fileLength;
            }
            while (true) {
                read = fi.read(bytes);
                if (read == -1) break;
                if ((read + dataSent) > totalDataToSend) {
                    System.out.println(this + " - Data Length Overlap (read=" + read + ", needed=" + (int) (totalDataToSend - dataSent) + ")");
                    read = (int) (totalDataToSend - dataSent);
                }
                outStream.write(bytes, 0, read);
                dataSent += read;
                if (dataSent >= totalDataToSend) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(this + " - ERROR : URL = " + urlData.getReqString());
            String[] keys = headers.keySet().toArray(new String[0]);
            for (int x = 0; x < keys.length; x++) {
                System.out.println(this + " - ERROR : REQUEST HEADER : " + keys[x] + " = " + headers.get(keys[x]));
            }
            System.out.println(this + " - ERROR : doRange = " + doRange);
            System.out.println(this + " - ERROR : totalDataToSend = " + totalDataToSend);
            System.out.println(this + " - ERROR : rangeStart = " + rangeStart);
            System.out.println(this + " - ERROR : rangeEnd = " + rangeEnd);
            System.out.println(this + " - ERROR : dataSent = " + dataSent);
            e.printStackTrace();
        } finally {
            try {
                fi.close();
            } catch (Exception e2) {
            }
        }
        return 1;
    }

    private String requestInfo() throws Exception {
        StringBuffer out = new StringBuffer(1024);
        out.append("Request String = (" + urlData.getReqString() + ")\n");
        out.append("Request Type   = (" + urlData.getRequestType() + ")\n\n");
        out.append("Parameter List:\n");
        String[] names = urlData.getParameterList();
        for (int x = 0; x < names.length; x++) out.append(names[x] + " = " + urlData.getParameter(names[x]) + "\n");
        out.append("\nRequest Headers:\n");
        String[] keys = (String[]) headers.keySet().toArray(new String[0]);
        for (int x = 0; x < keys.length; x++) {
            out.append(keys[x] + ": " + (String) headers.get(keys[x]) + "\n");
        }
        return out.toString();
    }
}
