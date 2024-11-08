package org.neteng;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Network {

    private SendListener listener;

    protected Network() {
    }

    public static Network join() {
        return new NetworkBroadcast();
    }

    public static Network join(String name) {
        return new NetworkNodeList(name);
    }

    public static Network join(String name, boolean broadcast) {
        NetworkComposed composed = new NetworkComposed();
        return composed;
    }

    protected Object readMessage(InputStream inputStream) {
        ObjectInputStream objectInputStream;
        try {
            objectInputStream = new ObjectInputStream(inputStream);
            return objectInputStream.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected InputStream writeMessage(Serializable message) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            new ObjectOutputStream(output).writeObject(message);
            return new ByteArrayInputStream(output.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream sendToMaster(InputStream message) {
        return syncSend(getPrimaryMaster(), message);
    }

    public Object sendToMaster(Serializable message) {
        return syncSend(getPrimaryMaster(), message);
    }

    public void sendToSlaves(InputStream message, SendListener listener) {
        for (Node node : getSlaves()) {
            send(node, message, listener);
        }
    }

    public void sendToSlaves(InputStream message) {
        sendToSlaves(message, getListener());
    }

    public void sendToSlaves(Serializable message, SendListener listener) {
        sendToSlaves(writeMessage(message), listener);
    }

    public void sendToSlaves(Serializable message) {
        sendToSlaves(writeMessage(message), getListener());
    }

    public void send(Node node, Serializable message, SendListener listener) {
        send(node, writeMessage(message), listener);
    }

    public void send(Node node, Serializable message) {
        send(node, writeMessage(message), getListener());
    }

    public void send(Node node, InputStream message) {
        send(node, message, getListener());
    }

    public Map<Node, InputStream> syncSendToSlaves(InputStream message) {
        Map<Node, InputStream> map = new HashMap<Node, InputStream>();
        for (Node node : getSlaves()) {
            map.put(node, syncSend(node, message));
        }
        return map;
    }

    public Map<Node, Object> syncSendToSlaves(Serializable message) {
        InputStream inputStream = writeMessage(message);
        Map<Node, Object> map = new HashMap<Node, Object>();
        for (Node node : getSlaves()) {
            InputStream stream = syncSend(node, inputStream);
            map.put(node, readMessage(stream));
        }
        return map;
    }

    public Object syncSend(Node node, Serializable message) {
        return readMessage(syncSend(node, writeMessage(message)));
    }

    public void send(Node node, InputStream message, SendListener listener) {
        new Thread(new SenderRunnable(this, node, message, listener)).run();
    }

    public abstract InputStream syncSend(Node node, InputStream message);

    public abstract List<Node> getSlaves();

    public abstract List<Node> getMasters();

    public abstract Node getPrimaryMaster();

    public SendListener getListener() {
        return listener;
    }

    public void setListener(SendListener listener) {
        this.listener = listener;
    }
}
