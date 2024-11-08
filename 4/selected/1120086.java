package com.hs.mail.container.server.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import com.hs.mail.io.LineReader;

/**
 * 
 * @author Won Chul Doh
 * @since May 28, 2010
 * 
 */
public class TcpTransport {

    private TcpSocketChannel channel = null;

    private InputStream is = null;

    private OutputStream os = null;

    private LineReader reader = null;

    private PrintWriter writer = null;

    private boolean sessionEnded = false;

    public TcpTransport() throws IOException {
    }

    public void setChannel(TcpSocketChannel channel) throws IOException {
        this.channel = channel;
        this.is = channel.getInputStream();
        this.os = channel.getOutputStream();
        this.reader = new LineReader(this.is);
        this.writer = new PrintWriter(this.os, true);
    }

    public TcpSocketChannel getChannel() {
        return channel;
    }

    public InetAddress getRemoteAddress() {
        return channel.getSocket().getInetAddress();
    }

    public InputStream getInputStream() throws IOException {
        return is;
    }

    public OutputStream getOutputStream() throws IOException {
        return os;
    }

    public boolean isSessionEnded() {
        return sessionEnded;
    }

    public void endSession() {
        this.sessionEnded = true;
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public void println(String str) {
        writer.println(str);
    }
}
