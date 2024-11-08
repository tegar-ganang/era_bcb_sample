package com.clanwts.bncs;

import java.net.InetSocketAddress;
import java.util.EventListener;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.clanwts.bncs.client.BattleNetChatClient;
import com.clanwts.bncs.client.BattleNetChatClientFactory;
import com.clanwts.bncs.client.SimpleBattleNetChatClientListener;
import com.clanwts.bncs.codec.standard.messages.GetAdvListExServer;
import com.clanwts.chat.PrivateChatListener;
import com.clanwts.chat.SingleChannelChatListener;
import com.clanwts.lang.EventDispatcher;
import com.clanwts.lang.EventProducer;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import edu.cmu.ece.agora.futures.Future;
import edu.cmu.ece.agora.futures.FutureManager;
import edu.cmu.ece.agora.futures.FuturePackage;
import edu.cmu.ece.agora.futures.Futures;

@Entity(version = 1)
public class Bot implements EventProducer {

    private static final Logger log = Logger.getLogger(Bot.class.getCanonicalName());

    static {
        log.setLevel(Level.ALL);
    }

    private static final transient int RECONNECT_DELAY_MS = 10 * 1000;

    private static final transient int REDISCONNECT_DELAY_MS = RECONNECT_DELAY_MS;

    @PrimaryKey
    private Account account;

    private String pass;

    private boolean pvpgn;

    private transient ExecutorService exec;

    private transient EventDispatcher ed;

    private transient BattleNetChatClient client;

    private transient Lock startLock = new ReentrantLock();

    private transient FuturePackage<Object> startfp = null;

    private transient FuturePackage<Object> stopfp = null;

    private transient boolean started = false;

    private Bot() {
        this.exec = Executors.newCachedThreadPool();
        this.ed = new EventDispatcher(exec);
        BattleNetChatClientFactory fact = new BattleNetChatClientFactory();
        fact.setKeys("4ZR8DP89RKM689DTJJZVVCV6RD", "8ZWHFNHXYJ8BY9PJTRBTH6GED8");
        fact.setWardenServerAddress(new InetSocketAddress("d2bot.cjb.net", 9367));
        this.client = fact.createClient();
        this.client.addListener(new ClientListener());
    }

    public Bot(Account acc, String pass, boolean pvpgn) {
        this();
        this.account = acc;
        this.pvpgn = pvpgn;
        this.pass = pass;
    }

    public Account getAccount() {
        return account;
    }

    public String getPassword() {
        return pass;
    }

    public boolean isPvpgn() {
        return pvpgn;
    }

    public String getCurrentChannel() {
        return client.getCurrentChannel();
    }

    public Future<Void> join(String channel) {
        return client.join(channel);
    }

    public Future<Void> part() {
        return client.part();
    }

    @Override
    public void addListener(EventListener l) {
        ed.addListener(l);
    }

    @Override
    public boolean isListener(EventListener l) {
        return ed.isListener(l);
    }

    @Override
    public void removeListener(EventListener l) {
        ed.removeListener(l);
    }

    public Future<Object> start() {
        Future<Object> ret;
        startLock.lock();
        if (this.isStarted()) {
            startLock.unlock();
            throw new IllegalStateException("Already started.");
        }
        if (startfp == null) {
            startfp = Futures.newFuturePackage();
            beginConnect(0);
        }
        ret = startfp.getFuture();
        startLock.unlock();
        return ret;
    }

    public boolean isStarting() {
        return startfp != null && !startfp.getFuture().isDone();
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isStopping() {
        return stopfp != null && !stopfp.getFuture().isDone();
    }

    public Future<Object> stop() {
        Future<Object> ret;
        startLock.lock();
        if (!this.isStarted()) {
            startLock.unlock();
            throw new IllegalStateException("Not started.");
        }
        if (stopfp == null) {
            stopfp = Futures.newFuturePackage();
            beginDisconnect(0);
        }
        ret = stopfp.getFuture();
        startLock.unlock();
        return ret;
    }

    private void beginConnect(long delay_in_ms) {
        TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                InetSocketAddress addr = new InetSocketAddress(account.getHost(), account.getPort());
                try {
                    client.connect(addr).get();
                    log.fine("Connection complete.");
                } catch (Exception e) {
                    startLock.lock();
                    if (isStarting()) {
                        FutureManager<Object> fm = startfp.getManager();
                        startfp = null;
                        fm.cancelFuture(new BotConnectionFailedException(e));
                    } else {
                        beginConnect(RECONNECT_DELAY_MS);
                    }
                    startLock.unlock();
                    return;
                }
                try {
                    client.login(account.getUser(), pass, pvpgn).get();
                    log.fine("Login complete.");
                } catch (Exception e) {
                    startLock.lock();
                    if (isStarting()) {
                        FutureManager<Object> fm = startfp.getManager();
                        startfp = null;
                        fm.cancelFuture(new BotLoginFailedException(e));
                    } else {
                    }
                    startLock.unlock();
                    beginDisconnect(0);
                    return;
                }
                startLock.lock();
                if (isStarting()) {
                    FutureManager<Object> fm = startfp.getManager();
                    startfp = null;
                    started = true;
                    fm.completeFuture(null);
                } else {
                    try {
                        ed.dispatch(SingleChannelChatListener.class, "forcedPart");
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
                startLock.unlock();
            }
        };
        new Timer().schedule(tt, delay_in_ms);
    }

    private void beginDisconnect(long delay_in_ms) {
        TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                try {
                    client.disconnect().get();
                } catch (Exception e) {
                    startLock.lock();
                    if (isStopping()) {
                        startLock.unlock();
                        beginDisconnect(REDISCONNECT_DELAY_MS);
                    }
                    startLock.unlock();
                    return;
                }
                startLock.lock();
                if (isStopping()) {
                    FutureManager<Object> fm = stopfp.getManager();
                    stopfp = null;
                    started = false;
                    fm.completeFuture(null);
                }
                if (isStarted()) {
                    beginConnect(RECONNECT_DELAY_MS);
                } else if (isStarting()) {
                    FutureManager<Object> fm = startfp.getManager();
                    startfp = null;
                    fm.cancelFuture(new BotLoginFailedException("Unexpected disconnect during login."));
                } else {
                }
                startLock.unlock();
            }
        };
        new Timer().schedule(tt, delay_in_ms);
    }

    public Future<GetAdvListExServer> getGameList() {
        return client.getGameList();
    }

    public Future<Void> sendChannelChat(String message) {
        return client.sendChannelChat(message);
    }

    public Future<Void> sendPrivateChat(String user, String message) {
        return client.sendPrivateChat(user, message);
    }

    private class ClientListener extends SimpleBattleNetChatClientListener {

        @Override
        public void channelChatReceived(String user, String message) {
            try {
                ed.dispatch(SingleChannelChatListener.class, "channelChatReceived", user, message);
            } catch (Exception e) {
            }
        }

        @Override
        public void forcedDisconnect() {
            log.fine("BNCC forced disconnect!");
            startLock.lock();
            if (isStarted()) {
                log.fine("Reconnecting due to forced disconnect...");
                beginConnect(RECONNECT_DELAY_MS);
            } else if (isStarting()) {
                FutureManager<Object> fm = startfp.getManager();
                startfp = null;
                fm.cancelFuture(new BotLoginFailedException("Unexpected disconnect during login."));
            }
            startLock.unlock();
        }

        @Override
        public void forcedJoin(String channel) {
            startLock.lock();
            if (!isStarted()) {
                startLock.unlock();
                return;
            }
            startLock.unlock();
            try {
                ed.dispatch(SingleChannelChatListener.class, "forcedJoin", channel);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void forcedPart() {
            startLock.lock();
            if (!isStarted()) {
                startLock.unlock();
                return;
            }
            startLock.unlock();
            try {
                ed.dispatch(SingleChannelChatListener.class, "forcedPart");
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void privateChatReceived(String user, String message) {
            try {
                ed.dispatch(PrivateChatListener.class, "privateChatReceived", user, message);
            } catch (Exception e) {
            }
        }

        @Override
        public void otherJoined(String user) {
            try {
                ed.dispatch(SingleChannelChatListener.class, "otherJoined", user);
            } catch (Exception e) {
            }
        }

        @Override
        public void otherParted(String user) {
            try {
                ed.dispatch(SingleChannelChatListener.class, "otherParted", user);
            } catch (Exception e) {
            }
        }
    }
}
