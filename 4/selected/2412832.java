package net.jetrix;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import net.jetrix.clients.*;
import net.jetrix.commands.*;
import net.jetrix.config.*;
import net.jetrix.messages.*;
import net.jetrix.messages.channel.*;
import net.jetrix.services.VersionService;
import net.jetrix.listeners.ShutdownListener;
import net.jetrix.mail.MailSessionManager;

/**
 * Main class, starts the server components and handle the server level messages.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 860 $, $Date: 2010-05-06 07:21:05 -0400 (Thu, 06 May 2010) $
 */
public class Server implements Runnable, Destination {

    private static Server instance;

    private Logger log = Logger.getLogger("net.jetrix");

    private File configFile;

    private ServerConfig config;

    private BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();

    private ChannelManager channelManager;

    private Client console;

    private Server() {
    }

    /**
     * Register the shutdown hooks to stop the server properly when the JVM is stopped.
     */
    private void registerHooks() {
        Thread hook = new Thread("StopHook") {

            public void run() {
                if (config != null && config.isRunning()) {
                    log.info("Shutdown command received from the system");
                    instance.stop();
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            SystemSignal.handle("INT", hook);
            SystemSignal.handle("TERM", hook);
        } catch (Throwable e) {
            log.warning("Unable to hook the system signals: " + e.getMessage());
        }
    }

    /**
     * Return the unique instance of the server.
     */
    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    /**
     * Server initialization.
     */
    private void init() {
        registerHooks();
        config = new ServerConfig();
        config.load(configFile);
        config.setRunning(true);
        LogManager.init();
        if (!config.getDataSources().isEmpty()) {
            log.info("Initializing the datasources...");
            for (DataSourceConfig datasource : config.getDataSources()) {
                DataSourceManager.getInstance().setDataSource(datasource, datasource.getName());
            }
        }
        if (config.getMailSessionConfig() != null) {
            log.info("Initializing the mail session...");
            MailSessionManager.getInstance().setConfiguration(config.getMailSessionConfig());
            MailSessionManager.getInstance().checkSession();
        }
        SystrayManager.open();
        channelManager = ChannelManager.getInstance();
        channelManager.clear();
        for (ChannelConfig cc : config.getChannels()) {
            cc.setPersistent(true);
            channelManager.createChannel(cc);
        }
        for (Listener listener : config.getListeners()) {
            if (listener.isAutoStart()) {
                listener.start();
            }
        }
        new ShutdownListener().start();
        for (Service service : config.getServices()) {
            if (service.isAutoStart()) {
                log.info("Starting service " + service.getName());
                service.start();
            }
        }
        VersionService.updateLatestVersion();
        if (VersionService.isNewVersionAvailable()) {
            log.warning("A new version is available (" + VersionService.getLatestVersion() + "), download it on http://jetrix.sf.net now!");
        }
        console = new ConsoleClient();
        new Thread(console).start();
        log.info("Server ready!");
    }

    /**
     * Start the server.
     */
    public void start() {
        Thread server = new Thread(this, "server");
        server.start();
    }

    /**
     * Stop the server.
     */
    public void stop() {
        config.setRunning(false);
        for (Listener listener : config.getListeners()) {
            if (listener.isRunning()) {
                listener.stop();
            }
        }
        for (Service service : config.getServices()) {
            if (service.isRunning()) {
                service.stop();
            }
        }
        disconnectAll();
        ChannelManager.getInstance().closeAll();
        send(new ShutdownMessage());
    }

    /**
     * Disconnect all clients from the server.
     */
    private void disconnectAll() {
        ClientRepository repository = ClientRepository.getInstance();
        for (Client client : repository.getClients()) {
            client.disconnect();
        }
        if (console != null) {
            console.disconnect();
        }
    }

    public void run() {
        init();
        while (config.isRunning()) {
            try {
                Message message = queue.take();
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("[server] processing " + message);
                }
                if (message instanceof AddPlayerMessage) {
                    Client client = ((AddPlayerMessage) message).getClient();
                    int level = client.getUser().getAccessLevel();
                    Channel channel = channelManager.getHomeChannel(level, client.getProtocol().getName());
                    if (channel != null) {
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest("[server] assigning client to channel " + channel);
                        }
                        channel.send(message);
                    } else {
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest("[server] no available channels!");
                        }
                    }
                } else if (message instanceof CommandMessage) {
                    CommandManager.getInstance().execute((CommandMessage) message);
                } else if (!(message instanceof ShutdownMessage)) {
                    log.info("[server] Message not processed " + message);
                }
            } catch (InterruptedException e) {
                log.log(Level.WARNING, e.getMessage(), e);
            }
        }
        SystrayManager.close();
    }

    /**
     * Add a message to the server message queue.
     */
    public void send(Message message) {
        queue.add(message);
    }

    /**
     * Return the server configuration.
     */
    public ServerConfig getConfig() {
        return config;
    }

    /**
     * Set the server configuration file.
     */
    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    /**
     * Server entry point.
     *
     * @param args start parameters
     */
    public static void main(String[] args) {
        System.out.println("Jetrix TetriNET Server " + ServerConfig.VERSION + ", Copyright (C) 2001-2010 Emmanuel Bourg\n");
        Server server = Server.getInstance();
        List<String> params = Arrays.asList(args);
        int p = params.indexOf("--conf");
        if (p != -1 && p + 1 < params.size()) {
            server.setConfigFile(new File(params.get(p + 1)));
        } else {
            server.setConfigFile(new File("conf/server.xml"));
        }
        server.start();
    }
}
