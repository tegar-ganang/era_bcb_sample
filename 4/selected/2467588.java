package com.googlecode.voctopus;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import com.googlecode.voctopus.config.VOctopusConfigurationManager;

/**
 * The Http Client Connection is the representation of the connection with the client.
 * 
 * @author marcello Feb 19, 2008 6:55:30 PM
 */
public class HttpClientConnection {

    private static final Logger logger = Logger.getLogger(HttpClientConnection.class);

    /**
     * This is the real socket connection with the client, which is managed here.
     */
    private Socket clientConnection;

    /**
     * Constructs a new Client connection based on the socket connection opened by the web server.
     * 
     * @param clientConnection is the socket connection with the client.
     */
    public HttpClientConnection(Socket clientConnection) {
        this.clientConnection = clientConnection;
    }

    /**
     * Parse the incoming request depending on the type of request you are receiving. This information is found from the
     * first line of the incoming request. You will also want to check and make sure the request you are receiving is a
     * valid request. If the request is not valid, throw an error using the http error codes.
     * 
     * @param inMsg BufferedReader which grabs the incoming message from the client socket 
     * @throws IOException thrown from reading the buffered reader
     */
    public String[] getConnectionLines() throws Exception {
        return verifyConnection(this.clientConnection);
    }

    /**
     * @return the output stream from the connection.
     * @throws IOException if any problem occurs getting the output stream.
     */
    public OutputStream getOutputStream() throws IOException {
        return this.clientConnection.getOutputStream();
    }

    /**
     * @return the socket connection from the client.
     */
    public Socket getClientConnection() {
        return clientConnection;
    }

    /**
     * Parse the incoming request, saving files from PUT methods, returning form entries.
     * @param clientConnection is the socket that holds the client request.
     * @return A list of the lines from the request. 
     * <li>It will transform all the form variables and their respective
     * values into parameters from the request. 
     * <li>It will no include the boundary delimiter tokens.
     * @throws IOException if an problem with the connection to the client happens.
     */
    private String[] verifyConnection(Socket clientConnection) throws Exception {
        List<String> requestLines = new ArrayList<String>();
        InputStream is = clientConnection.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringTokenizer st = new StringTokenizer(in.readLine());
        if (!st.hasMoreTokens()) {
            throw new IllegalArgumentException("There's no method token in this connection");
        }
        String method = st.nextToken();
        if (!st.hasMoreTokens()) {
            throw new IllegalArgumentException("There's no URI token in this connection");
        }
        String uri = decodePercent(st.nextToken());
        if (!st.hasMoreTokens()) {
            throw new IllegalArgumentException("There's no version token in this connection");
        }
        String version = st.nextToken();
        Properties parms = new Properties();
        int qmi = uri.indexOf('?');
        if (qmi >= 0) {
            decodeParms(uri.substring(qmi + 1), parms);
            uri = decodePercent(uri.substring(0, qmi));
        }
        String params = "";
        if (parms.size() > 0) {
            params = "?";
            for (Object key : parms.keySet()) {
                params = params + key + "=" + parms.getProperty(((String) key)) + "&";
            }
            params = params.substring(0, params.length() - 1).replace(" ", "%20");
        }
        logger.debug("HTTP Request: " + method + " " + uri + params + " " + version);
        requestLines.add(method + " " + uri + params + " " + version);
        Properties headerVars = new Properties();
        String line;
        String currentBoundary = null;
        Stack<String> boundaryStack = new Stack<String>();
        boolean readingBoundary = false;
        String additionalData = "";
        while (in.ready() && (line = in.readLine()) != null) {
            if (line.equals("") && (headerVars.get("Content-Type") == null || headerVars.get("Content-Length") == null)) {
                break;
            }
            logger.debug("HTTP Request Header: " + line);
            if (line.contains(": ")) {
                String vals[] = line.split(": ");
                headerVars.put(vals[0].trim(), vals[1].trim());
            }
            if (!readingBoundary && line.contains(": ")) {
                if (line.contains("boundary=")) {
                    currentBoundary = line.split("boundary=")[1].trim();
                    boundaryStack.push("--" + currentBoundary);
                }
                continue;
            } else if (line.equals("") && boundaryStack.isEmpty()) {
                int val = Integer.parseInt((String) headerVars.get("Content-Length"));
                if (headerVars.getProperty("Content-Type").contains("x-www-form-urlencoded")) {
                    char buf[] = new char[val];
                    int read = in.read(buf);
                    line = String.valueOf(buf, 0, read);
                    additionalData = line;
                    logger.debug("HTTP Request Header Form Parameters: " + line);
                }
            } else if (line.equals(boundaryStack.peek()) && !readingBoundary) {
                readingBoundary = true;
            } else if (line.equals(boundaryStack.peek()) && readingBoundary) {
                readingBoundary = false;
            } else if (line.contains(": ") && readingBoundary) {
                if (method.equalsIgnoreCase("PUT")) {
                    if (line.contains("form-data; ")) {
                        String formValues = line.split("form-data; ")[1];
                        for (String varValue : formValues.replace("\"", "").split("; ")) {
                            String[] vV = varValue.split("=");
                            vV[0] = decodePercent(vV[0]);
                            vV[1] = decodePercent(vV[1]);
                            headerVars.put(vV[0], vV[1]);
                        }
                    }
                }
            } else if (line.contains("") && readingBoundary && !boundaryStack.isEmpty() && headerVars.get("filename") != null) {
                int length = Integer.parseInt(headerVars.getProperty("Content-Length"));
                if (headerVars.getProperty("Content-Transfer-Encoding").contains("binary")) {
                    File uploadFilePath = new File(VOctopusConfigurationManager.WebServerProperties.HTTPD_CONF.getPropertyValue("TempDirectory"));
                    if (!uploadFilePath.exists()) {
                        logger.error("Temporaty dir does not exist: " + uploadFilePath.getCanonicalPath());
                    }
                    if (!uploadFilePath.isDirectory()) {
                        logger.error("Temporary dir is not a directory: " + uploadFilePath.getCanonicalPath());
                    }
                    if (!uploadFilePath.canWrite()) {
                        logger.error("VOctopus Webserver doesn't have permissions to write on temporary dir: " + uploadFilePath.getCanonicalPath());
                    }
                    FileOutputStream out = null;
                    try {
                        String putUploadPath = uploadFilePath.getAbsolutePath() + "/" + headerVars.getProperty("filename");
                        out = new FileOutputStream(putUploadPath);
                        OutputStream outf = new BufferedOutputStream(out);
                        int c;
                        while (in.ready() && (c = in.read()) != -1 && length-- > 0) {
                            outf.write(c);
                        }
                    } finally {
                        if (out != null) {
                            out.close();
                        }
                    }
                    File copied = new File(VOctopusConfigurationManager.getInstance().getDocumentRootPath() + uri + headerVars.get("filename"));
                    File tempFile = new File(VOctopusConfigurationManager.WebServerProperties.HTTPD_CONF.getPropertyValue("TempDirectory") + "/" + headerVars.get("filename"));
                    FileChannel ic = new FileInputStream(tempFile.getAbsolutePath()).getChannel();
                    FileChannel oc = new FileOutputStream(copied.getAbsolutePath()).getChannel();
                    ic.transferTo(0, ic.size(), oc);
                    ic.close();
                    oc.close();
                }
            }
        }
        for (Object var : headerVars.keySet()) {
            requestLines.add(var + ": " + headerVars.get(var));
        }
        if (!additionalData.equals("")) {
            requestLines.add("ADDITIONAL" + additionalData);
        }
        return requestLines.toArray(new String[requestLines.size()]);
    }

    /**
     * Decodes the percent encoding scheme. <br/> For example: "an+example%20string" -> "an example string"
     */
    private static String decodePercent(String str) {
        try {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch(c) {
                    case '+':
                        sb.append(' ');
                        break;
                    case '%':
                        sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
                        i += 2;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            return new String(sb.toString().getBytes());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decodes parameters in percent-encoded URI-format ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them
     * to given Properties.
     */
    private static List<String> decodeParms(String parms, Properties p) {
        if (parms == null) return new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(parms, "&");
        List<String> decoded = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0) {
                String var = decodePercent(e.substring(0, sep)).trim();
                String value = decodePercent(e.substring(sep + 1));
                p.put(var, value);
                decoded.add(var + ": " + value);
            }
        }
        return decoded;
    }
}
