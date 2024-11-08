import java.io.*;
import java.util.*;

public class HTTPRequest {

    private HashMap<String, String> headers = null;

    private HTTPurl urlData = null;

    private OutputStream outStream = null;

    private DataStore store = null;

    public HTTPRequest(String req, HashMap<String, String> head, byte[] postData, OutputStream out) throws Exception {
        outStream = out;
        headers = head;
        store = DataStore.getInstance();
        urlData = new HTTPurl(req, postData, head);
    }

    public void sendResponseData() {
        try {
            if (store.timerStatus == -1) {
                String timerError = "<html><head><title>Timer Thread Error</title></head><body>" + "<h1>Timer Thread Error</h1>The main timer thread is not running, it has crash with the following error:<p>" + "<pre>StackTrace:\n" + store.timerThreadErrorStack + "</pre><p>" + "Please post this error on the forum." + "</body>";
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
                Class paramTypes[] = {};
                java.lang.reflect.Constructor c = Class.forName(urlData.getServletClass()).getConstructor(paramTypes);
                Object params[] = {};
                HTTPResponse resp = (HTTPResponse) c.newInstance(params);
                resp.getResponse(urlData, outStream, headers);
                return;
            } else if (urlData.getRequestType() == 2) {
                returnFileContent(store.getProperty("path.httproot") + urlData.getReqString());
                return;
            } else if (urlData.getRequestType() == 1) {
                SystemStatusData sd = new SystemStatusData();
                outStream.write(sd.getStatusXML(urlData, headers));
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

    private int returnFileContent(String fileName) throws Exception {
        File thisFile = new File(fileName);
        String requestedFilePath = thisFile.getCanonicalPath();
        File root = new File(store.getProperty("path.httproot"));
        String rootFilePath = root.getCanonicalPath();
        if (requestedFilePath.indexOf(rootFilePath) < 0) {
            throw new Exception("File out of bounds!");
        }
        FileInputStream fi = null;
        fi = new FileInputStream(thisFile);
        long fileLength = thisFile.length();
        int read = 0;
        byte[] bytes = new byte[4096];
        read = fi.read(bytes);
        try {
            String header = "";
            header += "HTTP/1.0 200 OK\n";
            header += "Content-Length: " + fileLength + "\n";
            if (fileName.indexOf(".htc") > -1) header += "Content-Type: text/plain\n"; else if (fileName.indexOf(".html") > -1) header += "Content-Type: text/html\n";
            header += "\n";
            outStream.write(header.getBytes());
            while (read > -1) {
                outStream.write(bytes, 0, read);
                read = fi.read(bytes);
            }
        } catch (Exception e) {
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
