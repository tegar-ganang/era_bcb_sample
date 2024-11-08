package uk.ekiwi.messaging.mq;

import java.io.Serializable;

public class MqQueueManager implements Serializable {

    static final long serialVersionUID = -4792565248407793554L;

    private String hostName = null;

    private int port = -1;

    private String queueManager = null;

    private String channel = null;

    public MqQueueManager() {
    }

    public MqQueueManager(String queueManager, String host, int port, String channel) {
        this.queueManager = queueManager;
        this.hostName = host;
        this.port = port;
        this.channel = channel;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public String getQManager() {
        return queueManager;
    }

    public String getChannel() {
        return channel;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setPort(String port) {
        this.port = Integer.parseInt(port);
    }

    public void setQManager(String qManager) {
        this.queueManager = qManager;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
