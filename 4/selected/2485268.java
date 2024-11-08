package org.gamio.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.gamio.channel.ServerChannelFactory;
import org.gamio.conf.ServerManagerProps;
import org.gamio.conf.ServerProps;
import org.gamio.logging.Log;
import org.gamio.logging.Logger;
import org.gamio.system.Context;
import org.gamio.work.GmThread;
import org.gamio.work.Workshop;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 20 $ $Date: 2008-10-01 19:37:36 -0400 (Wed, 01 Oct 2008) $
 */
public final class ServerManagerImpl implements ServerManager {

    private static final Log log = Logger.getLogger(ServerManagerImpl.class);

    private ServerManagerProps serverManagerProps = null;

    private Selector selector = null;

    private Map<String, Server> hmServer = new LinkedHashMap<String, Server>();

    private Thread thread = null;

    private final Lock lock = new ReentrantLock();

    private final Lock regLock = new ReentrantLock();

    private final Lock selLock = new ReentrantLock();

    private final Job job = new Job();

    private final ServerStartingListener serverStartingListener = new ServerStartingListenerImpl();

    private ServerManagerState serverManagerState = ServerManagerStopped.getInstance();

    private interface ServerManagerState {

        public void start(ServerManagerImpl serverManager);

        public void stop(ServerManagerImpl serverManager);

        public void startServer(ServerManagerImpl serverManager, String id);

        public void startAllServers(ServerManagerImpl serverManager);
    }

    private static class ServerManagerStarted implements ServerManagerState {

        private static final ServerManagerStarted serverManagerStarted = new ServerManagerStarted();

        public static final ServerManagerStarted getInstance() {
            return serverManagerStarted;
        }

        private ServerManagerStarted() {
        }

        public void start(ServerManagerImpl serverManager) {
            log.warn("Server Manager has already started");
        }

        public void stop(ServerManagerImpl serverManager) {
            log.info("Stop Server Manager");
            serverManager.thread.interrupt();
            try {
                serverManager.selector.close();
            } catch (IOException e) {
                log.warn(e, "Failed to close the selector for selecting OP_ACCEPT");
            }
            serverManager.selector = null;
            try {
                serverManager.thread.join();
            } catch (InterruptedException e) {
            }
            serverManager.thread = null;
            serverManager.changeState(ServerManagerStopped.getInstance());
            log.info("Server Manager terminated");
        }

        public void startServer(ServerManagerImpl serverManager, String id) {
            Server server = serverManager.hmServer.get(id);
            if (server != null) server.start();
        }

        public void startAllServers(ServerManagerImpl serverManager) {
            for (Server server : serverManager.hmServer.values()) server.start();
        }
    }

    private static class ServerManagerStopped implements ServerManagerState {

        private static final ServerManagerStopped serverManagerStopped = new ServerManagerStopped();

        public static final ServerManagerStopped getInstance() {
            return serverManagerStopped;
        }

        private ServerManagerStopped() {
        }

        public void start(ServerManagerImpl serverManager) {
            log.info("Start Server Manager");
            try {
                serverManager.selector = Selector.open();
                serverManager.thread = new GmThread(serverManager.job);
                serverManager.thread.start();
                serverManager.changeState(ServerManagerStarted.getInstance());
            } catch (IOException e) {
                log.error(e, "Failed to start Server Manager");
            }
        }

        public void stop(ServerManagerImpl serverManager) {
            log.warn("Server Manager has already stopped");
        }

        public void startServer(ServerManagerImpl serverManager, String id) {
            log.warn("Please start Server Manager first before starting Server[", id, "]");
        }

        public void startAllServers(ServerManagerImpl serverManager) {
            log.warn("Please start Server Manager first before starting all servers");
        }
    }

    private class ServerStartingListenerImpl implements ServerStartingListener {

        public boolean onServerStarting(Server server, ServerSocketChannel serverSocketChannel) {
            try {
                serverSocketChannel.configureBlocking(false);
            } catch (IOException e) {
                log.error(e, "Failed to configure the ServerSocketChannel of Server[name<", server.getName(), ">, id<", server.getId(), ">] to non-blocking");
                return false;
            }
            selLock.lock();
            try {
                selector.wakeup();
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, server);
                selLock.unlock();
                return true;
            } catch (Exception e) {
                selLock.unlock();
                log.error(e, "Failed to register the ServerSocketChannel of Server[name<", server.getName(), ">, id<", server.getId(), ">]");
                return false;
            }
        }
    }

    private final class Job implements Runnable {

        public void run() {
            Context context = Context.getInstance();
            Workshop workshop = context.getWorkshop();
            ServerChannelFactory serverChannelFactory = context.getChannelManager().getServerChannelFactory();
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (selector.select() <= 0) {
                        if (Thread.currentThread().isInterrupted()) break;
                        selLock.lock();
                        selLock.unlock();
                        continue;
                    }
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        Runnable onAccept = null;
                        try {
                            SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                            ServerProps serverProps = ((Server) key.attachment()).getServerProps();
                            onAccept = serverChannelFactory.createServerChannel().onAccept(socketChannel, serverProps);
                            workshop.run(onAccept);
                        } catch (RejectedExecutionException e) {
                            onAccept.run();
                        } catch (Exception e) {
                            log.error(e, "Failed to accept");
                        }
                    }
                }
            } catch (IOException e) {
                log.fatal(e, "Server Manager error(need restart)");
                return;
            }
            stopAllServers();
        }
    }

    public ServerManagerImpl(ServerManagerProps serverManagerProps) {
        setServerManagerProps(serverManagerProps);
        ServerMqMsgListener.initializeCache(serverManagerProps.getOnMessageCacheSize());
    }

    public void setServerManagerProps(ServerManagerProps serverManagerProps) {
        this.serverManagerProps = serverManagerProps;
    }

    public ServerManagerProps getServerManagerProps() {
        return serverManagerProps;
    }

    public void startServer(String id) {
        lock.lock();
        regLock.lock();
        try {
            serverManagerState.startServer(this, id);
        } finally {
            regLock.unlock();
            lock.unlock();
        }
    }

    public boolean registerServer(ServerProps serverProps) {
        boolean ok = true;
        Server server = new Server(serverProps);
        server.setServerStartingListener(serverStartingListener);
        regLock.lock();
        try {
            server = hmServer.put(serverProps.getId(), server);
            if (server != null) {
                hmServer.put(serverProps.getId(), server);
                ok = false;
            }
        } finally {
            regLock.unlock();
        }
        if (ok) log.info("Server[", serverProps.getId(), "] was registered successfully"); else log.warn("Server[", serverProps.getId(), "] has already been registered");
        return ok;
    }

    public ServerProps deregisterServer(String id) {
        Server server = null;
        regLock.lock();
        try {
            server = hmServer.remove(id);
        } finally {
            regLock.unlock();
        }
        if (server != null) {
            server.setServerStartingListener(null);
            server.stop();
            return server.getServerProps();
        }
        return null;
    }

    public void deregisterAllServers() {
        Map<String, Server> tempMap = hmServer;
        regLock.lock();
        try {
            hmServer = new LinkedHashMap<String, Server>();
        } finally {
            regLock.unlock();
        }
        for (Server server : tempMap.values()) {
            server.setServerStartingListener(null);
            server.stop();
        }
    }

    public void stopServer(String id) {
        Server server = hmServer.get(id);
        if (server != null) server.stop();
    }

    public void startAllServers() {
        lock.lock();
        regLock.lock();
        try {
            serverManagerState.startAllServers(this);
        } finally {
            regLock.unlock();
            lock.unlock();
        }
    }

    public void stopAllServers() {
        regLock.lock();
        try {
            for (Server server : hmServer.values()) server.stop();
        } finally {
            regLock.unlock();
        }
    }

    public void start() {
        lock.lock();
        try {
            serverManagerState.start(this);
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {
            serverManagerState.stop(this);
        } finally {
            lock.unlock();
        }
    }

    private final void changeState(ServerManagerState serverManagerState) {
        this.serverManagerState = serverManagerState;
    }
}
