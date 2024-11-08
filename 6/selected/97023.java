package org.notify4b.im.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.notify4b.im.ChatManager;
import org.notify4b.im.Connection;
import org.notify4b.im.ConnectionImException;
import org.notify4b.im.User;
import org.notify4b.im.xmpp.XmppUser;

public class XmppConnection implements Connection {

    private XmppUser user;

    private String host;

    private int port;

    private String serviceName;

    private XMPPConnection connector;

    private ChatManager chatManager;

    public XmppConnection(XmppConnectionConfiguration connectionConfiguration) {
        this.user = (XmppUser) connectionConfiguration.getUser();
        this.host = connectionConfiguration.getHost();
        this.port = connectionConfiguration.getPort();
        this.serviceName = connectionConfiguration.getServiceName();
    }

    @Override
    public String getServerAddress() {
        return host;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void login() throws ConnectionImException {
        String resource = user.getResource();
        ConnectionConfiguration connectionConfig = null;
        if (resource == null || resource.equals("")) {
            resource = "notify4b";
        }
        if (serviceName != null) {
            connectionConfig = new ConnectionConfiguration(host, port, serviceName);
        } else {
            connectionConfig = new ConnectionConfiguration(host, port);
        }
        connector = new XMPPConnection(connectionConfig);
        try {
            connector.connect();
            connector.login(user.getId(), user.getPassword(), resource);
        } catch (XMPPException xmppE) {
            throw new ConnectionImException(xmppE);
        }
    }

    @Override
    public void logout() throws ConnectionImException {
    }

    @Override
    public ChatManager getChatManager() {
        if (chatManager == null) {
            chatManager = new XmppChatManagerImpl(connector.getChatManager());
        }
        return chatManager;
    }
}
