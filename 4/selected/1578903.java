package net.community.chest.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * Copyright 2007 as per GPLv2
 * 
 * Provides some default implementations for the {@link NetConnection} interface
 * 
 * @author Lyor G.
 * @since Jul 4, 2007 7:45:51 AM
 */
public abstract class AbstractNetConnection implements NetConnection {

    protected AbstractNetConnection() {
        super();
    }

    @Override
    public void attach(Socket sock) throws IOException {
        if (null == sock) throw new SocketException("No " + Socket.class.getName() + " instance to attach");
        attach(sock.getChannel());
    }

    @Override
    public void attach(SocketChannel channel) throws IOException {
        if (null == channel) throw new SocketException("No " + SocketChannel.class.getName() + " instance to attach");
        attach(channel.socket());
    }

    @Override
    public SocketChannel getChannel() {
        final Socket sock = getSocket();
        return (null == sock) ? null : sock.getChannel();
    }

    @Override
    public Socket getSocket() {
        final SocketChannel channel = getChannel();
        return (null == channel) ? null : channel.socket();
    }

    @Override
    public SocketChannel detachChannel() throws IOException {
        final Socket sock = detachSocket();
        return (null == sock) ? null : sock.getChannel();
    }

    @Override
    public Socket detachSocket() throws IOException {
        final SocketChannel channel = detachChannel();
        return (null == channel) ? null : channel.socket();
    }

    @Override
    public void close() throws IOException {
        IOException exc = null;
        if (isOpen()) {
            try {
                flush();
            } catch (IOException ioe) {
                exc = ioe;
            }
        }
        final Socket s = detachSocket();
        if (s != null) {
            try {
                s.shutdownOutput();
            } catch (IOException ioe) {
                if (null == exc) exc = ioe;
            }
            InputStream in = null;
            try {
                in = s.getInputStream();
                for (int v = in.read(), numRead = 0; v >= 0; numRead++, v = in.read()) {
                    if (numRead < 0) numRead = 0;
                }
            } catch (IOException e) {
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ce) {
                    }
                    in = null;
                }
            }
            try {
                s.close();
            } catch (IOException ioe) {
                if (null == exc) exc = ioe;
            }
        }
        if (exc != null) throw exc;
    }
}
