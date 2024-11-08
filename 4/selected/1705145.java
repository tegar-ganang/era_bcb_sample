package com.peterhi.net.impl;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.peterhi.net.Channel;
import com.peterhi.net.Configuration;
import com.peterhi.net.Endpoint;

public final class StdEndpointImpl implements Endpoint, Runnable {

    private Configuration config;

    private DatagramSocket socket;

    private byte[] buffer;

    private DatagramPacket packet;

    private Set ioSessions = new HashSet();

    private Set pendingSessions = new HashSet();

    public StdEndpointImpl(DatagramSocket socket, Configuration config) throws SocketException {
        this.config = config;
        this.socket = socket;
        this.socket.setSoTimeout(25);
        buffer = new byte[config.getMtu()];
        packet = new DatagramPacket(buffer, buffer.length);
    }

    public void run() {
        synchronized (ioSessions) {
            for (Iterator itor = ioSessions.iterator(); itor.hasNext(); ) {
                try {
                    StdChannelImpl ioSession = (StdChannelImpl) itor.next();
                    ioSession.send(socket);
                } catch (SocketTimeoutException ex) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        try {
            socket.receive(packet);
            StdChannelImpl ioSession = (StdChannelImpl) getChannel(packet.getSocketAddress());
            if (ioSession == null) {
                ioSession = new StdChannelImpl(this, packet.getSocketAddress());
                ioSessions.add(ioSession);
                pendingSessions.add(ioSession);
            }
            ioSession.receive(socket, packet);
        } catch (SocketTimeoutException ex) {
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Configuration getConfiguration() {
        return config;
    }

    public synchronized Channel connect(SocketAddress addr) {
        Channel ret = getChannel(addr);
        if (ret == null) {
            ret = new StdChannelImpl(this, addr);
            ioSessions.add(ret);
        }
        return ret;
    }

    public synchronized Channel accept() {
        synchronized (pendingSessions) {
            for (Iterator itor = pendingSessions.iterator(); itor.hasNext(); ) {
                StdChannelImpl cur = (StdChannelImpl) itor.next();
                itor.remove();
                return cur;
            }
            return null;
        }
    }

    public Channel getChannel(SocketAddress addr) {
        synchronized (ioSessions) {
            for (Iterator itor = ioSessions.iterator(); itor.hasNext(); ) {
                StdChannelImpl cur = (StdChannelImpl) itor.next();
                if (cur.getSocketAddress().equals(addr)) return cur;
            }
            return null;
        }
    }
}
