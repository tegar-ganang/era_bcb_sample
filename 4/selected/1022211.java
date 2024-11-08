package net.hypotenubel.jaicwain.session.irc;

import java.util.*;

/**
 * Models an IRC network with its servers. This class doesn't perform any checks
 * on the data, it's just holds the data.
 * <p>
 * This class is persisted via Hibernate.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: IRCNetwork.java 138 2006-10-01 21:10:46Z captainnuss $
 */
public class IRCNetwork implements Comparable<IRCNetwork> {

    /**
     * Unique ID identifying this instance. Used by Hibernate.
     */
    private long id;

    /**
     * The network's name.
     */
    private String name = "";

    /**
     * Whether the user is an operator on this network.
     */
    private boolean operator = false;

    /**
     * The network's operator password, usually empty.
     */
    private String opPassword = "";

    /**
     * The server that is usually used to connect to the network. If this
     * is {@code null}, a random server is chosen.
     */
    private IRCServer defaultServer = null;

    /**
     * List of the network's {@code IRCServer}s.
     */
    private List<IRCServer> servers = new ArrayList<IRCServer>(5);

    /**
     * List of the network's {@code IRCChannel}s.
     */
    private List<IRCChannel> channels = new ArrayList<IRCChannel>(5);

    /**
     * List of the nick names registered with the network's NickServ service.
     */
    private List<IRCNickServCredentials> credentials = new ArrayList<IRCNickServCredentials>(5);

    /**
     * Creates a new, empty instance.
     */
    public IRCNetwork() {
        this("", false, "");
    }

    /**
     * Creates a new {@code IRCNetwork} object and initializes it.
     * 
     * @param name the network's name.
     * @param operator whether the user is an operator on the network.
     * @param password the network's operator password.
     */
    public IRCNetwork(String name, boolean operator, String password) {
        this.name = name;
        this.operator = operator;
        this.opPassword = password;
    }

    /**
     * Returns the unique ID used by Hibernate.
     * 
     * @return unique ID.
     */
    public long getId() {
        return this.id;
    }

    /**
     * Returns the network's name.
     * 
     * @return {@code String} containing the network's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Checks whether the user is an operator on this network.
     * 
     * @return {@code true} if the user is an operator, {@code false}
     *         otherwise.
     */
    public boolean isOperator() {
        return operator;
    }

    /**
     * Returns the network's operator password.
     * 
     * @return {@code String} containing the network's operator password.
     */
    public String getOpPassword() {
        return opPassword;
    }

    /**
     * Returns the server usually used to connect to the network.
     * 
     * @return server to connect to or {@code null} if a random server
     *         should be chosen.
     */
    public IRCServer getDefaultServer() {
        return defaultServer;
    }

    /**
     * Returns the network's IRC servers.
     * 
     * @return {@code List} containing the network's
     *         {@code IRCServer} objects.
     */
    public List<IRCServer> getServers() {
        return servers;
    }

    /**
     * Returns the network's IRC channels.
     * 
     * @return {@code List} containing the network's
     *         {@code IRCChannel} objects.
     */
    public List<IRCChannel> getChannels() {
        return this.channels;
    }

    /**
     * Returns the nick names registered with the network's NickServ service.
     * 
     * @return {@code List} containing the network's
     *         {@code IRCNickServCredentials} objects.
     */
    public List<IRCNickServCredentials> getCredentials() {
        return this.credentials;
    }

    /**
     * Sets the unique ID. Used by Hibernate.
     * 
     * @param id unique ID.
     */
    protected void setId(long id) {
        this.id = id;
    }

    /**
     * Sets the network's name.
     * 
     * @param name {@code String} containing the new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets whether the user is an operator on this network or not.
     * 
     * @param operator {@code true} if the user is an operator, {@code false}
     *                 otherwise.
     */
    public void setOperator(boolean operator) {
        this.operator = operator;
    }

    /**
     * Sets the network's password.
     * 
     * @param password {@code String} containing the new password.
     */
    public void setOpPassword(String password) {
        this.opPassword = password;
    }

    /**
     * Sets the server used to connect to the network.
     * 
     * @param server the server to connect to or {@code null} if one
     *               should be chosen randomly.
     */
    public void setDefaultServer(IRCServer server) {
        this.defaultServer = server;
    }

    /**
     * Sets the network's IRC servers.
     * 
     * @param servers list of servers belonging to this network.
     */
    public void setServers(List<IRCServer> servers) {
        this.servers = servers;
    }

    /**
     * Sets the network's IRC channels.
     * 
     * @param channels list of channels joined on this network.
     */
    public void setChannels(List<IRCChannel> channels) {
        this.channels = channels;
    }

    /**
     * Sets the nick names registered with the network's NickServ service.
     * 
     * @param credentials list of authentication credentials known for
     *                    this network.
     */
    public void setCredentials(List<IRCNickServCredentials> credentials) {
        this.credentials = credentials;
    }

    /**
     * Adds an IRC server to the collection.
     * 
     * @param server {@code IRCServer} to be added.
     */
    public void addServer(IRCServer server) {
        if (server == null) {
            return;
        }
        if (servers.contains(server)) {
            return;
        }
        servers.add(server);
        server.setNetwork(this);
    }

    /**
     * Removes an IRC server from the collection.
     * 
     * @param server {@code IRCServer} to be removed.
     */
    public void removeServer(IRCServer server) {
        if (server == null) {
            return;
        }
        servers.remove(server);
        server.setNetwork(null);
    }

    /**
     * Removes all servers from the network.
     */
    public void clearServers() {
        servers.clear();
    }

    /**
     * Adds an IRC channel to the collection.
     * 
     * @param channel {@code IRCChannel} to be added.
     */
    public void addChannel(IRCChannel channel) {
        if (channel == null) {
            return;
        }
        if (servers.contains(channel)) {
            return;
        }
        channels.add(channel);
        channel.setNetwork(this);
    }

    /**
     * Removes an IRC channel from the collection.
     * 
     * @param channel {@code IRCChannel} to be removed.
     */
    public void removeChannel(IRCChannel channel) {
        if (channel == null) {
            return;
        }
        channels.remove(channel);
        channel.setNetwork(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IRCNetwork)) {
            return false;
        }
        return this.getName().equals(((IRCNetwork) obj).getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    public int compareTo(IRCNetwork o) {
        return name.compareTo(o.getName());
    }
}
