package com.rbnb.timedrive;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

public class Response {

    private static String contentLengthLabel = new String("Content-Length:");

    private static String contentTypeLabel = new String("Content-Type:");

    private static String textType = new String("text/");

    private static String lastModifiedLabel = new String("Last-Modified:");

    private static String dateLabel = new String("Date:");

    public static String eol = new String("\r\n");

    private static String notModifiedLine = new String("HTTP/1.1 304 Not Modified");

    private int debug = 0;

    private boolean headerOnly = false;

    private Vector headerV = new Vector();

    private int numHeaderLines = 0;

    private boolean isNull = false;

    private String[] headerA = null;

    private String firstLineNoHost = null;

    private boolean isText = false;

    private int contentLength = 0;

    private byte[] content = null;

    private Vector bodyV = new Vector();

    private String statusCode = null;

    private String header = null;

    private byte[] responseBytes = null;

    public Response(String headerI, int debugI) throws Exception {
        this(new ByteArrayInputStream(headerI.getBytes()), null, debugI, true);
        header = headerI;
    }

    public Response(InputStream is, OutputStream os, int debugI) throws Exception {
        this(is, os, debugI, false);
    }

    public Response(InputStream is, OutputStream os, int debugI, boolean headerOnlyI) throws Exception {
        debug = debugI;
        headerOnly = headerOnlyI;
        boolean done = false;
        int nAvail = 0;
        int nRead = 0;
        DataInputStream br = new DataInputStream(is);
        try {
            boolean bAddedConnectionClose = false;
            while (!done) {
                String nextLine = br.readLine();
                if (debug > 4) System.err.println("Response nextLine " + nextLine);
                if (nextLine == null) done = true; else {
                    if (os != null) {
                        if ((!bAddedConnectionClose) && (nextLine.length() == 0)) {
                            String closeStr = "Connection: close";
                            headerV.add(closeStr);
                            os.write(closeStr.getBytes());
                            os.write(eol.getBytes());
                        }
                        if (nextLine.equalsIgnoreCase("connection: close")) {
                            bAddedConnectionClose = true;
                        }
                        os.write(nextLine.getBytes());
                        os.write(eol.getBytes());
                    }
                    if (nextLine.length() == 0) done = true;
                    headerV.add(nextLine);
                }
            }
            numHeaderLines = headerV.size();
            headerA = new String[numHeaderLines];
            headerV.copyInto(headerA);
            for (int i = numHeaderLines - 1; i >= 0; i--) {
                if (headerA[i].regionMatches(true, 0, contentLengthLabel, 0, contentLengthLabel.length())) {
                    contentLength = Integer.parseInt(headerA[i].substring(contentLengthLabel.length()).trim());
                } else if (headerA[i].regionMatches(true, 0, contentTypeLabel, 0, contentTypeLabel.length())) {
                    if (headerA[i].indexOf(textType) > 0) isText = true;
                } else if (headerA[i].regionMatches(true, 0, lastModifiedLabel, 0, lastModifiedLabel.length())) {
                }
            }
            if (!headerOnly) {
                if (numHeaderLines == 0 || headerV.elementAt(0) == null || ((String) (headerV.elementAt(0))).equalsIgnoreCase("Error")) {
                    if (debug > 2) {
                        System.err.println("Response null header");
                    }
                    isNull = true;
                } else if (contentLength == 0 && getStatusCode().equals("200")) {
                    Vector contentV = new Vector();
                    byte[] chunk = new byte[1024];
                    int numRead = 0;
                    while ((numRead = br.read(chunk)) > -1) {
                        contentLength += numRead;
                        if (chunk.length > numRead) {
                            byte[] temp = new byte[numRead];
                            System.arraycopy(chunk, 0, temp, 0, numRead);
                            chunk = temp;
                        }
                        if (os != null) {
                            os.write(chunk);
                        }
                        contentV.add(chunk);
                        chunk = new byte[1024];
                    }
                    content = new byte[contentLength];
                    int j = 0;
                    for (int i = 0; i < contentV.size(); i++) {
                        byte[] nextChunk = (byte[]) contentV.elementAt(i);
                        System.arraycopy(nextChunk, 0, content, j, nextChunk.length);
                        j += nextChunk.length;
                    }
                } else if (contentLength > 0) {
                    content = new byte[contentLength];
                    int numRead = br.read(content, 0, contentLength);
                    if (numRead >= 0) {
                        while (numRead < contentLength) {
                            numRead += br.read(content, numRead, contentLength - numRead);
                        }
                        if (os != null) {
                            try {
                                os.write(content);
                            } catch (Exception e) {
                                if (debug > 2) {
                                    System.err.println("Write response to client: ignore exception " + "(assume client has received content and " + "just closed the connection):");
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        contentLength = 0;
                        headerOnly = true;
                    }
                } else if (isText) {
                    String nextLine = null;
                    while ((nextLine = br.readLine()) != null) {
                        if (os != null) {
                            os.write(nextLine.getBytes());
                            os.write(eol.getBytes());
                        }
                        bodyV.add(nextLine);
                    }
                    if (bodyV.size() > 0) {
                        bodyV.remove(bodyV.size() - 1);
                    }
                }
            }
        } catch (Exception e) {
            if (debug > 2) {
                System.err.println("Response exception:");
                e.printStackTrace();
            }
            isNull = true;
            throw e;
        }
    }

    public boolean isNull() {
        return isNull;
    }

    public String getStatusCode() {
        if (isNull) return null;
        if (statusCode == null) parseFirstLine();
        return statusCode;
    }

    private void parseFirstLine() {
        if (isNull) return;
        try {
            StringTokenizer st = new StringTokenizer(headerA[0], " :");
            st.nextToken();
            statusCode = st.nextToken();
        } catch (Exception e) {
            System.err.println("Response.parseFirstLine exception " + e.getMessage());
            System.err.println("  headerV:\n" + headerV.toString());
            e.printStackTrace();
        }
    }

    public String getHeader() {
        if (isNull) return null;
        if (header == null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < numHeaderLines; i++) {
                sb = sb.append(headerA[i]).append(eol);
            }
            header = sb.toString();
        }
        return header;
    }

    public byte[] getContent() {
        if (isNull) return null;
        if (headerOnly) return null;
        if (contentLength > 0) return content; else if (isText) {
            StringBuffer sb = new StringBuffer();
            Enumeration e = bodyV.elements();
            while (e.hasMoreElements()) sb = sb.append((String) e.nextElement()).append(eol);
            return sb.toString().getBytes();
        } else {
            return null;
        }
    }

    public byte[] getResponse() {
        if (isNull) return null;
        if (headerOnly) return getHeader().getBytes();
        if (responseBytes == null) {
            byte[] headerBytes = getHeader().getBytes();
            byte[] bodyBytes = null;
            if (contentLength > 0) bodyBytes = content; else if (isText) {
                StringBuffer sb = new StringBuffer();
                Enumeration e = bodyV.elements();
                while (e.hasMoreElements()) sb = sb.append((String) e.nextElement()).append(eol);
                bodyBytes = sb.toString().getBytes();
            }
            responseBytes = new byte[headerBytes.length + bodyBytes.length + eol.getBytes().length];
            System.arraycopy(headerBytes, 0, responseBytes, 0, headerBytes.length);
            System.arraycopy(bodyBytes, 0, responseBytes, headerBytes.length, bodyBytes.length);
            System.arraycopy(eol.getBytes(), 0, responseBytes, headerBytes.length + bodyBytes.length, eol.getBytes().length);
        }
        return responseBytes;
    }

    public void setNotModified() {
        if (isNull) return;
        headerV.remove(headerA[0]);
        headerV.add(0, notModifiedLine);
        for (int i = 1; i < numHeaderLines; i++) {
            if (headerA[i].regionMatches(true, 0, contentLengthLabel, 0, contentLengthLabel.length())) headerV.remove(headerA[i]); else if (headerA[i].regionMatches(true, 0, lastModifiedLabel, 0, lastModifiedLabel.length())) headerV.remove(headerA[i]); else if (headerA[i].regionMatches(true, 0, dateLabel, 0, dateLabel.length())) headerV.remove(headerA[i]);
        }
        String format = new String("EEE, dd MMM yyyy HH:mm:ss zzz");
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = new Date();
        headerV.add(1, "Date: " + sdf.format(date));
        numHeaderLines = headerV.size();
        headerA = new String[numHeaderLines];
        headerV.copyInto(headerA);
        statusCode = null;
        responseBytes = null;
        isText = false;
        header = null;
        headerOnly = true;
        contentLength = 0;
        content = null;
    }

    public void setContent(byte[] contentI) {
        if (isNull) return;
        content = contentI;
        contentLength = content.length;
        headerOnly = false;
        isText = false;
    }

    public int getContentLength() {
        return contentLength;
    }
}
