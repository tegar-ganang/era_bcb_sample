package com.arsenal.rtcomm.server.http;

import java.io.*;
import java.net.*;
import java.util.*;
import com.arsenal.rtcomm.server.ConnectionManager;

public class HttpOutputStream extends BufferedOutputStream {

    protected int code;

    protected boolean sendHeaders;

    protected boolean sendBody;

    protected Hashtable headers = new Hashtable();

    protected Socket socket = null;

    public HttpOutputStream(OutputStream out, HttpInputStream in) {
        super(out, BUFFER_SIZE);
        code = HTTP.STATUS_OKAY;
        setHeader("Server", "Channel Server 1.0");
        setHeader("Date", new Date().toString());
        setHeader("Connection", "Connection: Keep-Alive");
        sendHeaders = (in.getVersion()[0] >= 1);
        sendBody = !HTTP.METHOD_HEAD.equals(in.getMethod());
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setHeader(String attr, String value) {
        headers.put(attr, value);
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public boolean sendHeaders() throws IOException {
        if (sendHeaders) {
            write("HTTP/1.0 " + code + " " + HTTP.getCodeMessage(code) + "\r\n");
            Enumeration attrs = headers.keys();
            while (attrs.hasMoreElements()) {
                String attr = (String) attrs.nextElement();
                write(attr + ": " + headers.get(attr) + "\r\n");
            }
            write('\n');
        }
        return sendBody;
    }

    public void write(String msg) throws IOException {
        write(msg.getBytes("latin1"));
    }

    public void write(InputStream in) throws IOException {
        int n, length = buf.length;
        while ((n = in.read(buf, count, length - count)) >= 0) if ((count += n) >= length) out.write(buf, count = 0, length);
    }

    public void close() throws IOException {
        super.close();
    }

    public void flush() throws IOException {
        try {
            super.flush();
        } catch (Exception e) {
        }
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception e) {
        }
        socket = null;
        headers = null;
    }

    static final int BUFFER_SIZE = Integer.parseInt(System.getProperty("1028", "262144"));
}
