package org.boticelli;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.apache.log4j.Logger;
import org.boticelli.plugin.BotAware;
import org.boticelli.plugin.BoticelliPlugin;
import org.boticelli.plugin.dist.ResponseMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import f00f.net.irc.martyr.IRCConnection;
import f00f.net.irc.martyr.clientstate.Channel;
import f00f.net.irc.martyr.clientstate.ClientState;
import f00f.net.irc.martyr.clientstate.Member;
import f00f.net.irc.martyr.commands.ActionCtcp;
import f00f.net.irc.martyr.commands.CtcpMessage;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.commands.QuitCommand;
import f00f.net.irc.martyr.services.AutoJoin;
import f00f.net.irc.martyr.services.AutoReconnect;
import f00f.net.irc.martyr.services.AutoRegister;
import f00f.net.irc.martyr.services.AutoResponder;
import f00f.net.irc.martyr.util.FullNick;

/**
 * Boticelli Bot implementation, based on the Martyr IRC library.
 */
public class Bot implements InitializingBean, ApplicationListener, BeanNameAware {

    private static Logger log = Logger.getLogger(Bot.class);

    protected IRCConnection connection;

    private AutoReconnect autoReconnect;

    private String server;

    private int port;

    private String channelName;

    protected Channel mainChannel;

    private ClientState clientState;

    private List<BoticelliPlugin> plugins;

    private String channelKey;

    private String nick = "boticelli", user = "BoticelliBot", name = "Boticelli Bot";

    private SessionFactory sessionFactory;

    private String beanName;

    protected MessageDispatcher messageDispatcher;

    @Required
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Required
    public void setPlugins(List<BoticelliPlugin> plugins) {
        for (BoticelliPlugin plugin : plugins) {
            if (plugin instanceof BotAware) {
                ((BotAware) plugin).setBot(this);
            }
        }
        this.plugins = plugins;
    }

    @Required
    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Required
    public void setServer(String server) {
        this.server = server;
    }

    @Required
    public void setPort(int port) {
        this.port = port;
    }

    @Required
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    public void connect() throws IOException, UnknownHostException {
        autoReconnect.go(server, port);
    }

    public void disconnect() {
        autoReconnect.disable();
        connection.sendCommand(new QuitCommand("Shutting down..."));
    }

    public void joinedChannel(Channel channel) {
        mainChannel = channel;
    }

    public void say(String msg) {
        final MessageCommand messageCommand = new MessageCommand(getState().getNick(), mainChannel.getName(), msg);
        connection.sendCommand(messageCommand);
        messageDispatcher.updateCommandInternal(messageCommand);
    }

    public void sayAction(String msg) {
        final ActionCtcp actionCtcp = new ActionCtcp(mainChannel.getName(), msg);
        connection.sendCommand(actionCtcp);
        CtcpMessage ctcpMessage = new CtcpMessage(getState().getNick(), mainChannel.getName(), "ACTION " + msg);
        messageDispatcher.updateCommandInternal(ctcpMessage);
    }

    public IRCConnection getConnection() {
        return connection;
    }

    public String getChannelName() {
        return channelName;
    }

    @Override
    public String toString() {
        return super.toString() + ": nick = " + nick + ", user = " + user + ", name = " + name + " in #" + channelName + " on " + server + ":" + port;
    }

    public void afterPropertiesSet() throws Exception {
        log.info("Connecting bot " + this);
        Locale.setDefault(Locale.ENGLISH);
        clientState = new BotState(this);
        connection = new IRCConnection(clientState);
        connection.setDaemon(true);
        new AutoResponder(connection);
        new AutoRegister(connection, nick, user, name);
        autoReconnect = new AutoReconnect(connection);
        new AutoJoin(connection, channelName, channelKey);
        messageDispatcher = new MessageDispatcher(this, plugins);
        this.connect();
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextClosedEvent) {
            this.disconnect();
        }
    }

    public ClientState getState() {
        return clientState;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }

    public void respond(MessageCommand cmd, String message) {
        if (message != null && message.length() > 0) {
            respond(cmd.getSource().getNick(), cmd.isPrivateToUs(getState()), message);
        }
    }

    public void respond(String nick, boolean privately, String message) {
        respond(nick, privately ? ResponseMode.PRIVATE : ResponseMode.CHANNEL, message);
    }

    public void respond(String nick, ResponseMode mode, String message) {
        if (message == null || message.length() == 0) {
            return;
        }
        if (mode == ResponseMode.PRIVATE) {
            sendMessage(nick, message);
        } else if (mode == ResponseMode.CHANNEL) {
            say(nick + ": " + message);
        }
    }

    public void sendMessage(String dest, String message) {
        this.getConnection().sendCommand(new MessageCommand(getState().getNick(), dest, message));
    }

    public void sendMessage(FullNick nick, String message) {
        sendMessage(nick.getNick(), message);
    }

    public String getNick() {
        return getState().getNick().getNick();
    }

    public Bot spawn(Class<? extends Bot> botClass, String name, BoticelliPlugin plugin) throws Exception {
        List<BoticelliPlugin> l = new LinkedList<BoticelliPlugin>();
        l.add(plugin);
        return spawn(botClass, name, l);
    }

    public Bot spawn(Class<? extends Bot> botClass, String name, List<BoticelliPlugin> plugins) throws Exception {
        Bot child = botClass.newInstance();
        child.setPlugins(plugins);
        child.setBeanName(name);
        child.setSessionFactory(getSessionFactory());
        child.setServer(this.server);
        child.setPort(this.port);
        child.setChannelName(getChannelName());
        child.setNick(name);
        child.afterPropertiesSet();
        return child;
    }
}
