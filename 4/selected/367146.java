package com.peterhi.server;

import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.xsocket.stream.INonBlockingConnection;

/**
 *
 * @author YUN TAO
 */
public class ClientHandle {

    private static final AtomicInteger clientIdTicker = new AtomicInteger(0);

    private int id;

    private int state;

    private int channelId;

    private String email;

    private String channel;

    private String displayName;

    private List<Integer> ids = new ArrayList<Integer>();

    private INonBlockingConnection conn;

    public ClientHandle(INonBlockingConnection conn) {
        setId(clientIdTicker.incrementAndGet());
        this.conn = conn;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    public int getState() {
        return state;
    }

    public void setState(int value) {
        state = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        email = value;
    }

    public int getId() {
        return id;
    }

    protected void setId(int value) {
        id = value;
    }

    public INonBlockingConnection connection() {
        return conn;
    }

    public SocketAddress getTcpAddress() {
        if (conn != null && conn.isOpen()) {
            return new InetSocketAddress(conn.getRemoteAddress(), conn.getRemotePort());
        } else {
            return null;
        }
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int value) {
        channelId = value;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String value) {
        channel = value;
    }
}
