package source;

import java.util.ArrayList;
import java.util.List;

public class Server {

    private Integer id_server;

    private String network;

    private String server;

    private String port;

    private List<String> channels;

    public Server() {
        channels = new ArrayList<String>();
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public Integer getId_server() {
        return id_server;
    }

    public boolean addChannel(String channel) {
        return channels.add(channel);
    }

    public boolean addChannels(List channels) {
        return channels.addAll(channels);
    }

    public List<String> getChannels() {
        return channels;
    }
}
