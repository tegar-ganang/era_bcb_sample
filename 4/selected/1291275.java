package org.cybergarage.http;

import java.io.*;
import java.net.*;
import java.util.*;

public class HTTPSocket {

    public HTTPSocket(Socket socket) {
        setSocket(socket);
        open();
    }

    public HTTPSocket(HTTPSocket socket) {
        setSocket(socket.getSocket());
        setInputStream(socket.getInputStream());
        setOutputStream(socket.getOutputStream());
    }

    public void finalize() {
        close();
    }

    private Socket socket = null;

    private void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getLocalAddress() {
        return getSocket().getLocalAddress().getHostAddress();
    }

    public int getLocalPort() {
        return getSocket().getLocalPort();
    }

    private InputStream sockIn = null;

    private OutputStream sockOut = null;

    private void setInputStream(InputStream in) {
        sockIn = in;
    }

    public InputStream getInputStream() {
        return sockIn;
    }

    private void setOutputStream(OutputStream out) {
        sockOut = out;
    }

    private OutputStream getOutputStream() {
        return sockOut;
    }

    public boolean open() {
        Socket sock = getSocket();
        try {
            sockIn = sock.getInputStream();
            sockOut = sock.getOutputStream();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean close() {
        try {
            if (sockIn != null) sockIn.close();
            if (sockOut != null) sockOut.close();
            getSocket().close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean post(HTTPResponse httpRes, byte content[]) {
        httpRes.setDate(Calendar.getInstance());
        OutputStream out = getOutputStream();
        try {
            out.write(httpRes.getHeader().getBytes());
            out.write(HTTP.CRLF.getBytes());
            out.write(content);
            out.flush();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean post(HTTPResponse httpRes, InputStream in, long inLen) {
        httpRes.setDate(Calendar.getInstance());
        OutputStream out = getOutputStream();
        try {
            out.write(httpRes.getHeader().getBytes());
            out.write(HTTP.CRLF.getBytes());
            int chunkSize = HTTP.getChunkSize();
            byte readBuf[] = new byte[chunkSize];
            long readCnt = 0;
            int readLen = in.read(readBuf);
            while (0 < readLen && readCnt < inLen) {
                out.write(readBuf, 0, readLen);
                readCnt += readLen;
                readLen = in.read(readBuf);
            }
            out.flush();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean post(HTTPResponse httpRes, InputStream in) {
        return post(httpRes, httpRes.getContentInputStream(), httpRes.getContentLength());
    }

    public boolean post(HTTPResponse httpRes) {
        if (httpRes.hasContentInputStream() == true) return post(httpRes, httpRes.getContentInputStream());
        return post(httpRes, httpRes.getContent());
    }
}
