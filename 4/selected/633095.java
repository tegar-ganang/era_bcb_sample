package ru.beta2.testyard.engine.hotspot;

import com.sun.sgs.client.ClientChannel;
import com.sun.sgs.client.ClientChannelListener;
import com.sun.sgs.client.simple.SimpleClient;
import com.sun.sgs.client.simple.SimpleClientListener;
import org.apache.commons.lang.ArrayUtils;
import ru.beta2.testyard.config.Configuration;
import ru.beta2.testyard.engine.Expectations;
import ru.beta2.testyard.engine.HotspotLink;
import ru.beta2.testyard.engine.HotspotListener;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * User: Inc
 * Date: 19.06.2008
 * Time: 21:06:02
 */
public class HotspotConnector implements HotspotLink {

    private SyncListenerDecorator engineListener;

    private final HashMap<Integer, Session> sessions = new HashMap<Integer, Session>();

    private Properties loginProps = new Properties();

    private CountDownLatch logoutLatch;

    private int receivedEventsCount;

    private CountDownLatch receivedLatch;

    private final Object eventsRunMonitor = new Object();

    private final Configuration cfg;

    public HotspotConnector(Configuration cfg) {
        this.cfg = cfg;
        loginProps.setProperty("host", cfg.getSgsHost());
        loginProps.setProperty("port", String.valueOf(cfg.getSgsPort()));
    }

    public void setListener(HotspotListener listener) {
        this.engineListener = new SyncListenerDecorator(listener);
    }

    public void login(int player) {
        System.out.println("Login player " + player);
        Session sess = new Session(player);
        SimpleClient c = new SimpleClient(sess);
        sess.client = c;
        sessions.put(player, sess);
        try {
            c.login(loginProps);
        } catch (IOException e) {
            throw new HotspotIOException("Error while login", e);
        }
    }

    public void logout(int player) {
        getSession(player).logout();
    }

    public void waitForMessages(Expectations expects) {
        long timeout = cfg.getMessagesTimeout();
        System.out.println("Begin wait for messages, expected " + expects.count());
        try {
            while ((timeout > 0) && (expects.count() > 0)) {
                try {
                    long ts = System.currentTimeMillis();
                    Runnable run = engineListener.events.poll(timeout, TimeUnit.MILLISECONDS);
                    if (run == null) {
                        return;
                    }
                    timeout -= System.currentTimeMillis() - ts;
                    run.run();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted wait event from hotspot");
                }
            }
        } finally {
            System.out.println("Total wait: " + (cfg.getMessagesTimeout() - timeout));
        }
    }

    private Session getSession(int player) {
        Session c = sessions.get(player);
        if (c == null) {
            throw new ObjectNotFoundException("Session absent for player " + player);
        }
        return c;
    }

    private ClientChannel getChannel(int player, String channel) {
        return getSession(player).getChannel(channel);
    }

    private ByteBuffer toByteBuffer(Object object) {
        try {
            return cfg.getSerializationHandler().writeObject(object);
        } catch (IOException e) {
            throw new HotspotIOException("Error converting object to hessian stream", e);
        }
    }

    public void sendSessionMessage(int player, Object message) {
        try {
            getSession(player).client.send(toByteBuffer(message));
        } catch (IOException e) {
            throw new HotspotIOException("Error sending message to session", e);
        }
    }

    public void sendChannelMessage(int player, String channel, Object message) {
        try {
            getChannel(player, channel).send(toByteBuffer(message));
        } catch (IOException e) {
            throw new HotspotIOException("Error sending message to channel", e);
        }
    }

    public void shutdown() {
        synchronized (sessions) {
            logoutLatch = new CountDownLatch(sessions.size());
            for (Session s : sessions.values()) {
                s.client.logout(false);
            }
        }
        try {
            logoutLatch.await(cfg.getShutdownTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.out.println("Interrupted awaiting shutdown");
        }
    }

    public int[] getLoggedInPlayers() {
        return ArrayUtils.toPrimitive(sessions.keySet().toArray(new Integer[sessions.size()]));
    }

    private String playerName(int player) {
        return cfg.getPlayersHandler().getPlayerName(player);
    }

    private String playerPassword(int player) {
        return cfg.getPlayersHandler().getPlayerPassword(player);
    }

    class Session implements SimpleClientListener, ClientChannelListener {

        private final int player;

        private final HashMap<String, ClientChannel> channels = new HashMap<String, ClientChannel>();

        SimpleClient client;

        boolean loggingOut;

        Session(int player) {
            this.player = player;
        }

        void logout() {
            loggingOut = true;
            client.logout(false);
        }

        public ClientChannel getChannel(String channel) {
            ClientChannel c = channels.get(channel);
            if (c == null) {
                throw new ObjectNotFoundException("Player " + player + " not join to channel " + channel);
            }
            return c;
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(playerName(player), playerPassword(player).toCharArray());
        }

        public void loggedIn() {
            engineListener.loggedIn(player);
        }

        public void loginFailed(String s) {
            System.out.println("LOGIN FAILED, player " + player);
        }

        public ClientChannelListener joinedChannel(ClientChannel clientChannel) {
            channels.put(clientChannel.getName(), clientChannel);
            getChannel(clientChannel.getName());
            engineListener.channelJoined(player, clientChannel.getName());
            return this;
        }

        public void receivedMessage(ByteBuffer byteBuffer) {
            engineListener.messageFromSession(player, toObject(byteBuffer));
        }

        private Object toObject(ByteBuffer buf) {
            try {
                return cfg.getSerializationHandler().readObject(buf);
            } catch (IOException e) {
                throw new HotspotIOException("Error reading object from hessian stream", e);
            }
        }

        public void reconnecting() {
            throw new UnsupportedOperationException("reconnecting");
        }

        public void reconnected() {
            throw new UnsupportedOperationException("reconnected");
        }

        public void disconnected(boolean b, String s) {
            System.out.println("SESSION DISCONNECTED, player " + player + ", graceful=" + b);
            if (loggingOut) {
                engineListener.disconnected(player);
            }
            synchronized (sessions) {
                sessions.remove(player);
                if (logoutLatch != null) {
                    logoutLatch.countDown();
                }
            }
        }

        public void receivedMessage(ClientChannel clientChannel, ByteBuffer byteBuffer) {
            engineListener.messageFromChannel(player, clientChannel.getName(), toObject(byteBuffer));
        }

        public void leftChannel(ClientChannel clientChannel) {
            System.out.println("Player " + player + " left channel " + clientChannel);
            channels.remove(clientChannel.getName());
            engineListener.channelLeft(player, clientChannel.getName());
        }
    }

    class SyncListenerDecorator implements HotspotListener {

        private final HotspotListener lis;

        private LinkedBlockingQueue<Runnable> events = new LinkedBlockingQueue<Runnable>();

        SyncListenerDecorator(HotspotListener lis) {
            this.lis = lis;
        }

        private void add(Runnable run) {
            receivedEventsCount++;
            events.add(run);
        }

        public void loggedIn(final int player) {
            add(new Runnable() {

                public void run() {
                    lis.loggedIn(player);
                }
            });
        }

        public void disconnected(final int player) {
            add(new Runnable() {

                public void run() {
                    lis.disconnected(player);
                }
            });
        }

        public void channelJoined(final int player, final String channel) {
            add(new Runnable() {

                public void run() {
                    lis.channelJoined(player, channel);
                }
            });
        }

        public void channelLeft(final int player, final String channel) {
            add(new Runnable() {

                public void run() {
                    lis.channelLeft(player, channel);
                }
            });
        }

        public void messageFromSession(final int player, final Object message) {
            add(new Runnable() {

                public void run() {
                    lis.messageFromSession(player, message);
                }
            });
        }

        public void messageFromChannel(final int player, final String channel, final Object message) {
            add(new Runnable() {

                public void run() {
                    lis.messageFromChannel(player, channel, message);
                }
            });
        }
    }
}
