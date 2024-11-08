package org.thole.phiirc.irc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import org.thole.phiirc.client.controller.Connector;

public class IRCConnection {

    private String username;

    private String nick;

    private String server;

    private int startPort;

    private int endPort;

    private String password;

    private Connector connector;

    private IRCDataReadHandler readHandler;

    private IRCDataWriteHandler writeHandler;

    private SocketAddress sockaddr;

    private Socket socket;

    /**
	 * 
	 * @param server
	 * @param port
	 */
    public IRCConnection(final String server, final int port) {
        this.setServer(server);
        this.setStartPort(port);
        this.setNick(this.getConnector().getMyUser().getNick());
        this.setUsername(this.getConnector().getMyUser().getName());
    }

    /**
	 * 
	 * @param connector
	 * @param host
	 * @param startPort
	 * @param endPort
	 * @param password
	 * @param nick2
	 * @param name2
	 * @param nick3
	 */
    public IRCConnection(final Connector connector, final String host, int startPort, final int endPort, final String password, final String nick2, final String name2, final String nick3) {
        this.setConnector(connector);
        this.setServer(host);
        this.setStartPort(startPort);
        this.setNick(this.getConnector().getMyUser().getNick());
        this.setUsername(this.getConnector().getMyUser().getName());
    }

    public void connect() {
        try {
            this.setSockaddr(new InetSocketAddress(this.getServer(), this.getStartPort()));
            this.setSocket(new Socket());
            final int timeoutMs = 1000 * 5;
            this.setReadHandler(new IRCDataReadHandler(this));
            this.setWriteHandler(new IRCDataWriteHandler(this));
            this.getSocket().connect(this.getSockaddr(), timeoutMs);
            final Runnable runnableReader = this.getReadHandler();
            final Thread readThread = new Thread(runnableReader);
            final Runnable runnableWriter = this.getWriteHandler();
            final Thread writeThread = new Thread(runnableWriter);
            readThread.start();
            writeThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getServer() {
        return server;
    }

    private void setServer(String server) {
        this.server = server;
    }

    public String getNick() {
        return nick;
    }

    private void setNick(String nick) {
        this.nick = nick;
    }

    private int getStartPort() {
        return startPort;
    }

    private void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    private int getEndPort() {
        return endPort;
    }

    private void setEndPort(final int endPort) {
        this.endPort = endPort;
    }

    private String getPassword() {
        return password;
    }

    private void setPassword(final String password) {
        this.password = password;
    }

    public Connector getConnector() {
        return connector;
    }

    private void setConnector(Connector connector) {
        this.connector = connector;
    }

    public IRCDataReadHandler getReadHandler() {
        return readHandler;
    }

    private void setReadHandler(IRCDataReadHandler readHandler) {
        this.readHandler = readHandler;
    }

    private SocketAddress getSockaddr() {
        return sockaddr;
    }

    private void setSockaddr(SocketAddress sockaddr) {
        this.sockaddr = sockaddr;
    }

    public Socket getSocket() {
        return socket;
    }

    private void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    private void setUsername(String username) {
        this.username = username;
    }

    public IRCDataWriteHandler getWriteHandler() {
        return this.writeHandler;
    }

    private void setWriteHandler(IRCDataWriteHandler writeHandler) {
        this.writeHandler = writeHandler;
    }
}
