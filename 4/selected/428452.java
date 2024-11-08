package com.rbnb.timedrive;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PipedInputStream;
import java.util.StringTokenizer;
import java.util.Vector;

public class Request {

    private static String authorizationLabel = new String("Authorization: Basic");

    private static String contentLabel = new String("Content-Length: ");

    private static String sourceIPLabel = new String("-source-ip: ");

    private static String modifiedLabel = new String("If-Modified-Since: ");

    private static String optionLabel = new String("Opt: \"http://rbnb.net/ext\"; ns=");

    private static String eol = new String("\r\n");

    private PipedInputStream pis = null;

    private int debug = 0;

    private String source = null;

    private boolean bUltimateHostSpecified = false;

    private boolean isNull = false;

    private boolean doNullResponse = false;

    private int numHeaderLines = 0;

    private String[] headerA = null;

    private String firstLineNoHost = null;

    private int contentLength = 0;

    private char[] content = null;

    private String command = null;

    private String protocol = null;

    private String path = null;

    private String language = null;

    private String host = null;

    private int port = 80;

    private String requestString = null;

    private String requestStringNoHost = null;

    private String authorizationString = null;

    private String namespaceCode = null;

    private String ipAddress = null;

    private boolean bProxyRequest = false;

    public Request(PipedInputStream pisI, int debugI, String source, boolean bUltimateHostSpecifiedI) {
        pis = pisI;
        debug = debugI;
        source = source;
        bUltimateHostSpecified = bUltimateHostSpecifiedI;
        boolean done = false;
        Vector headerV = new Vector();
        int nAvail = 0;
        int nRead = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(pis));
        try {
            while (!done) {
                String nextLine = br.readLine();
                if (debug > 4) {
                    System.err.println("Request nextLine " + nextLine);
                }
                if (nextLine == null) {
                    done = true;
                } else {
                    if (nextLine.length() == 0) {
                        headerV.add("Connection: close");
                        headerV.add(nextLine);
                        done = true;
                    } else if ((!nextLine.toLowerCase().startsWith("keep-alive")) && (!nextLine.toLowerCase().startsWith("connection"))) {
                        if (nextLine.regionMatches(true, 0, authorizationLabel, 0, authorizationLabel.length())) {
                            authorizationString = nextLine.substring(authorizationLabel.length()).trim();
                            if ((authorizationString != null) && (authorizationString.trim().equals(""))) {
                                authorizationString = null;
                            }
                            authorizationString = authorizationString.trim();
                        } else if (nextLine.regionMatches(true, 0, optionLabel, 0, optionLabel.length())) {
                            namespaceCode = nextLine.substring(optionLabel.length()).trim();
                            if ((namespaceCode != null) && (namespaceCode.trim().equals(""))) {
                                namespaceCode = null;
                            }
                            namespaceCode = namespaceCode.trim();
                        } else if (nextLine.regionMatches(true, 2, sourceIPLabel, 0, sourceIPLabel.length()) && (namespaceCode != null) && (nextLine.regionMatches(true, 0, namespaceCode, 0, namespaceCode.length()))) {
                            ipAddress = nextLine.substring(sourceIPLabel.length() + 2).trim();
                            if ((ipAddress != null) && (ipAddress.trim().equals(""))) {
                                ipAddress = null;
                            }
                            ipAddress = ipAddress.trim();
                        } else {
                            headerV.add(nextLine);
                        }
                    }
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

    public boolean isProxyRequest() {
        if (isNull) return false;
        if (path == null) parseFirstLine();
        return bProxyRequest;
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

    public String getUserID(int multiUserModeI, String hostAddressFromSocketI) {
        if ((isNull) || (multiUserModeI == TimeDrive.MULTIUSER_OFF)) {
            return null;
        } else if (multiUserModeI == TimeDrive.MULTIUSER_IP) {
            if (ipAddress != null) {
                return ipAddress;
            } else {
                return hostAddressFromSocketI;
            }
        } else if (multiUserModeI == TimeDrive.MULTIUSER_BASIC_AUTH) {
            return authorizationString;
        } else if (multiUserModeI == TimeDrive.MULTIUSER_COMBO) {
            if (authorizationString == null) {
                return null;
            } else {
                if (ipAddress != null) {
                    return new String(ipAddress + authorizationString);
                } else {
                    return new String(hostAddressFromSocketI + authorizationString);
                }
            }
        }
        return null;
    }

    public String getAuthorizationString() {
        if (isNull) return null;
        return authorizationString;
    }

    private void parseFirstLine() {
        if (isNull) return;
        if (headerA == null || headerA.length < 1) {
            isNull = true;
            return;
        }
        int httpIndex = headerA[0].indexOf("http://");
        if (httpIndex == -1) {
            bProxyRequest = false;
            StringTokenizer st = new StringTokenizer(headerA[0], " ");
            command = st.nextToken();
            path = st.nextToken();
            protocol = "http";
            language = st.nextToken();
        } else {
            bProxyRequest = true;
            StringTokenizer st = new StringTokenizer(headerA[0], " :");
            command = st.nextToken();
            protocol = st.nextToken();
            if (bUltimateHostSpecified) {
                path = protocol + ":" + st.nextToken();
            } else {
                path = st.nextToken().substring(2);
            }
            language = st.nextToken();
        }
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

    public String changePath(String newPathI) {
        if (isNull) {
            return null;
        }
        if (debug > 2) {
            System.err.println("Change request path to: " + newPathI);
        }
        if (!bUltimateHostSpecified) {
            headerA[0] = new String(command + " http://" + newPathI + " " + language);
        } else {
            headerA[0] = new String(command + " " + newPathI + " " + language);
        }
        path = null;
        command = null;
        language = null;
        protocol = null;
        host = null;
        requestString = null;
        requestStringNoHost = null;
        firstLineNoHost = null;
        requestString = getRequest();
        parseFirstLine();
        getHost();
        return requestString;
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
