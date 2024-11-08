package com.meterware.pseudoserver;

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.*;

/**
 *
 * @author <a href="mailto:russgold@httpunit.org">Russell Gold</a>
 **/
abstract class ReceivedHttpMessage {

    private static final int CR = 13;

    private static final int LF = 10;

    private Reader _reader;

    private Hashtable _headers = new Hashtable();

    private byte[] _requestBody;

    ReceivedHttpMessage(InputStream inputStream) throws IOException {
        interpretMessageHeader(readHeaderLine(inputStream));
        readHeaders(inputStream);
        readMessageBody(inputStream);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(getClassName()).append("[ ");
        appendMessageHeader(sb);
        sb.append("\n");
        for (Enumeration e = _headers.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            sb.append("      ").append(key).append(": ").append(_headers.get(key)).append("\n");
        }
        sb.append("   body contains ").append(getBody().length).append(" byte(s)]");
        return sb.toString();
    }

    private String readHeaderLine(InputStream inputStream) throws IOException {
        return new String(readDelimitedChunk(inputStream));
    }

    private byte[] readDelimitedChunk(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b = inputStream.read();
        while (b != CR) {
            baos.write(b);
            b = inputStream.read();
        }
        b = inputStream.read();
        if (b != LF) throw new IOException("Bad header line termination: " + b);
        return baos.toByteArray();
    }

    void appendContents(StringBuffer sb) {
        for (Enumeration e = _headers.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            sb.append("      ").append(key).append(": ").append(_headers.get(key)).append("\n");
        }
        sb.append("   body contains ").append(getBody().length).append(" byte(s)");
    }

    Reader getReader() {
        return _reader;
    }

    String getHeader(String name) {
        return (String) _headers.get(name.toUpperCase());
    }

    byte[] getBody() {
        return _requestBody;
    }

    private void readMessageBody(InputStream inputStream) throws IOException {
        if ("chunked".equalsIgnoreCase(getHeader("Transfer-Encoding"))) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (getNextChunkLength(inputStream) > 0) {
                baos.write(readDelimitedChunk(inputStream));
            }
            flushChunkTrailer(inputStream);
            _requestBody = baos.toByteArray();
        } else {
            int totalExpected = getContentLength();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(totalExpected);
            byte[] buffer = new byte[1024];
            int total = 0;
            int count = -1;
            while ((total < totalExpected) && ((count = inputStream.read(buffer)) != -1)) {
                baos.write(buffer, 0, count);
                total += count;
            }
            baos.flush();
            _requestBody = baos.toByteArray();
        }
        _reader = new InputStreamReader(new ByteArrayInputStream(_requestBody));
    }

    private void flushChunkTrailer(InputStream inputStream) throws IOException {
        byte[] line;
        do {
            line = readDelimitedChunk(inputStream);
        } while (line.length > 0);
    }

    private int getNextChunkLength(InputStream inputStream) throws IOException {
        try {
            return Integer.parseInt(readHeaderLine(inputStream), 16);
        } catch (NumberFormatException e) {
            throw new IOException("Unabled to read chunk length: " + e);
        }
    }

    private int getContentLength() {
        try {
            return Integer.parseInt(getHeader("Content-Length"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void readHeaders(InputStream inputStream) throws IOException {
        String lastHeader = null;
        String header = readHeaderLine(inputStream);
        while (header.length() > 0) {
            if (header.charAt(0) <= ' ') {
                if (lastHeader == null) continue;
                _headers.put(lastHeader, _headers.get(lastHeader) + header.trim());
            } else {
                lastHeader = header.substring(0, header.indexOf(':')).toUpperCase();
                _headers.put(lastHeader, header.substring(header.indexOf(':') + 1).trim());
            }
            header = readHeaderLine(inputStream);
        }
    }

    private String getClassName() {
        return getClass().getName().substring(getClass().getName().lastIndexOf('.') + 1);
    }

    abstract void appendMessageHeader(StringBuffer sb);

    abstract void interpretMessageHeader(String messageHeader);
}
