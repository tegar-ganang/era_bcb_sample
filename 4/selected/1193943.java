package jgnash.message;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgnash.util.DefaultDaemonThreadFactory;
import static jgnash.util.LogUtils.logStackTrace;

public class MessageBus {

    private static final Logger logger = Logger.getLogger(MessageBus.class.getName());

    @SuppressWarnings("MapReplaceableByEnumMap")
    private final Map<MessageChannel, Set<WeakReference<MessageListener>>> map = new ConcurrentHashMap<MessageChannel, Set<WeakReference<MessageListener>>>();

    private final ExecutorService pool = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    private MessageBusRemoteClient messageBusClient = null;

    private static final Map<String, MessageBus> busMap = new HashMap<String, MessageBus>();

    private static final String DEFAULT = "default";

    private MessageBus() {
    }

    /**
     * Set message bus to post remote messages
     *
     * @param host message server name or IP address
     * @param port message server port
     * @return <code>true</code> if connection to the remote server was successful
     */
    public synchronized boolean setRemote(final String host, final int port) {
        disconnectFromServer();
        return connectToServer(host, port);
    }

    /**
     * Set message bus for local operation
     */
    public synchronized void setLocal() {
        disconnectFromServer();
    }

    public static synchronized MessageBus getInstance() {
        return getInstance(DEFAULT);
    }

    public static synchronized MessageBus getInstance(final String name) {
        MessageBus bus = busMap.get(name);
        if (bus == null) {
            bus = new MessageBus();
            busMap.put(name, bus);
        }
        return bus;
    }

    private void disconnectFromServer() {
        if (messageBusClient != null) {
            messageBusClient.disconnectFromServer();
            messageBusClient = null;
        }
    }

    private boolean connectToServer(final String remoteHost, final int remotePort) {
        assert remoteHost != null & remotePort > 0;
        messageBusClient = new MessageBusRemoteClient(remoteHost, remotePort);
        boolean result = messageBusClient.connectToServer();
        if (!result) {
            messageBusClient = null;
        }
        return result;
    }

    public void registerListener(final MessageListener listener, final MessageChannel... channels) {
        for (MessageChannel channel : channels) {
            Set<WeakReference<MessageListener>> set = map.get(channel);
            if (set == null) {
                set = new CopyOnWriteArraySet<WeakReference<MessageListener>>();
                map.put(channel, set);
            }
            if (containsListener(listener, channel)) {
                logger.severe("An attempt was made to install a duplicate listener");
                logStackTrace(logger, Level.SEVERE);
            } else {
                set.add(new WeakReference<MessageListener>(listener));
            }
        }
    }

    public void unregisterListener(final MessageListener listener, final MessageChannel... channels) {
        for (MessageChannel channel : channels) {
            Set<WeakReference<MessageListener>> set = map.get(channel);
            if (set != null) {
                for (WeakReference<MessageListener> ref : set) {
                    MessageListener l = ref.get();
                    if (l == null || l == listener) {
                        set.remove(ref);
                    }
                }
            }
        }
    }

    private boolean containsListener(final MessageListener listener, final MessageChannel channel) {
        Set<WeakReference<MessageListener>> set = map.get(channel);
        if (set != null) {
            for (WeakReference<MessageListener> ref : set) {
                MessageListener l = ref.get();
                if (l == listener) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fireEvent(final Message message) {
        Set<WeakReference<MessageListener>> set = map.get(message.getChannel());
        if (set != null) {
            pool.execute(new MessageHandler(message, set));
        }
    }

    /**
     * This nested class is used to update any listeners in a separate thread and post the message to the remote message
     * bus if running.
     */
    private final class MessageHandler implements Runnable {

        final Message message;

        final Set<WeakReference<MessageListener>> set;

        MessageHandler(final Message event, final Set<WeakReference<MessageListener>> set) {
            this.message = event;
            this.set = set;
        }

        @Override
        public void run() {
            for (WeakReference<MessageListener> ref : set) {
                MessageListener l = ref.get();
                if (l != null) {
                    l.messagePosted(message);
                }
            }
            if (!message.isRemote()) {
                if (messageBusClient != null && message.getChannel() != MessageChannel.SYSTEM) {
                    messageBusClient.sendRemoteMessage(message);
                }
            }
        }
    }
}
