package com.rbnb.webCache;

import com.rbnb.utility.ParseURL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PipedInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;

public class Request {

    private static String contentLabel = new String("Content-Length: ");

    private static String modifiedLabel = new String("If-Modified-Since: ");

    private static String eol = new String("\r\n");

    private static String optionsCommand = new String("OPTIONS");

    private PipedInputStream pis = null;

    private int debug = 0;

    private String source = null;

    private boolean isNull = false;

    private boolean doNullResponse = false;

    private int numHeaderLines = 0;

    private String[] headerA = null;

    private String firstLineNoHost = null;

    private int contentLength = 0;

    private char[] content = null;

    private WebDate modifiedDate = null;

    private String command = null;

    private String protocol = null;

    private String path = null;

    private String language = null;

    private String host = null;

    private int port = 80;

    private String requestString = null;

    private String requestStringNoHost = null;

    private ParseURL purl = null;

    public Request(PipedInputStream pisI, int debugI, String source) {
        pis = pisI;
        debug = debugI;
        source = source;
        boolean done = false;
        Vector headerV = new Vector();
        int nAvail = 0;
        int nRead = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(pis));
        try {
            while (!done) {
                String nextLine = br.readLine();
                if (debug > 4) System.err.println("Request nextLine " + nextLine);
                if (nextLine == null) done = true; else {
                    if (nextLine.length() == 0) done = true;
                    headerV.add(nextLine);
                }
            }
            numHeaderLines = headerV.size();
            headerA = new String[numHeaderLines];
            headerV.copyInto(headerA);
            for (int i = numHeaderLines - 1; i >= 0; i--) {
                if (headerA[i].regionMatches(true, 0, contentLabel, 0, contentLabel.length())) {
                    contentLength = Integer.parseInt(headerA[i].substring(contentLabel.length()));
                } else if (headerA[i].regionMatches(true, 0, modifiedLabel, 0, modifiedLabel.length())) {
                    String modifiedString = headerA[i].substring(modifiedLabel.length());
                    try {
                        modifiedDate = new WebDate(modifiedString);
                    } catch (Exception e) {
                        System.err.println("Exception parsing date: " + modifiedString);
                    }
                }
            }
            if (contentLength > 0) {
                content = new char[contentLength];
                int numRead = br.read(content, 0, contentLength);
                while (numRead < contentLength) {
                    numRead += br.read(content, numRead, contentLength - numRead);
                }
            }
        } catch (Exception e) {
            System.err.println("Request exception: " + e.getMessage());
            isNull = true;
        }
    }

    public boolean isNull() {
        return isNull;
    }

    public boolean doNullResponse() {
        if (isNull) return true;
        if (path == null) parseFirstLine();
        return doNullResponse;
    }

    public String getCommand() {
        if (isNull) return null;
        if (command == null) parseFirstLine();
        return command;
    }

    public String getProtocol() {
        if (isNull) return null;
        if (protocol == null) parseFirstLine();
        return protocol;
    }

    public String getPath() {
        if (isNull) return null;
        if (path == null) parseFirstLine();
        return path;
    }

    public String getLanguage() {
        if (isNull) return null;
        if (language == null) parseFirstLine();
        return language;
    }

    public ParseURL getPURL() {
        return purl;
    }

    private void parseFirstLine() {
        if (isNull) return;
        if (headerA == null || headerA.length < 1) {
            isNull = true;
            return;
        }
        StringTokenizer st = new StringTokenizer(headerA[0], " ");
        command = st.nextToken();
        String url = st.nextToken();
        purl = new ParseURL(url, true);
        protocol = purl.getProtocol();
        if (protocol == null) protocol = "http";
        if (command.equals(optionsCommand)) {
            path = protocol;
            doNullResponse = true;
            return;
        } else {
            path = purl.getRequest();
        }
        language = st.nextToken();
    }

    public String getHost() {
        if (isNull) return null;
        if (host == null) {
            StringTokenizer st = new StringTokenizer(headerA[0], " /", true);
            StringBuffer sb = new StringBuffer();
            sb = sb.append(st.nextToken());
            sb = sb.append(st.nextToken());
            st.nextToken();
            st.nextToken();
            st.nextToken();
            host = st.nextToken();
            int idx = host.indexOf(':');
            if (idx > 0) {
                port = Integer.parseInt(host.substring(idx + 1));
                host = host.substring(0, idx);
            }
            while (st.hasMoreTokens()) sb = sb.append(st.nextToken());
            firstLineNoHost = sb.toString();
        }
        return host;
    }

    public int getPort() {
        if (isNull) return 0;
        if (host == null) getHost();
        return port;
    }

    public WebDate getModifiedDate() {
        if (isNull) return null;
        return modifiedDate;
    }

    public void setModifiedDate(WebDate modifiedDateI) {
        if (isNull) return;
        if (modifiedDate == null) {
            modifiedDate = modifiedDateI;
            String[] newHeaderA = new String[numHeaderLines + 1];
            newHeaderA[0] = headerA[0];
            newHeaderA[1] = modifiedLabel + modifiedDate.getDateString();
            System.arraycopy(headerA, 1, newHeaderA, 2, numHeaderLines - 1);
            numHeaderLines++;
            headerA = newHeaderA;
        } else {
            modifiedDate = modifiedDateI;
            for (int i = 0; i < numHeaderLines; i++) {
                if (headerA[i].regionMatches(true, 0, modifiedLabel, 0, modifiedLabel.length())) {
                    headerA[i] = modifiedLabel + modifiedDate.getDateString();
                    i = numHeaderLines;
                }
            }
        }
        requestString = null;
    }

    public String getRequest() {
        if (isNull) return null;
        if (requestString == null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < numHeaderLines; i++) {
                sb = sb.append(headerA[i]).append(eol);
            }
            if (contentLength > 0) {
                sb = sb.append(content).append(eol);
            }
            requestString = sb.toString();
        }
        return requestString;
    }

    public String getRequestNoHost() {
        if (isNull) return null;
        if (requestStringNoHost == null) {
            if (firstLineNoHost == null) getHost();
            StringBuffer sb = new StringBuffer();
            sb = sb.append(firstLineNoHost).append(eol);
            for (int i = 1; i < numHeaderLines; i++) {
                sb = sb.append(headerA[i]).append(eol);
            }
            if (contentLength > 0) {
                sb = sb.append(content).append(eol);
            }
            requestStringNoHost = sb.toString();
        }
        return requestStringNoHost;
    }
}
