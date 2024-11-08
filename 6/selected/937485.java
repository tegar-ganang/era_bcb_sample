package com.clanwts.bncs.bot;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import com.clanwts.bncs.client.BattleNetChatClient;
import com.clanwts.bncs.client.BattleNetChatClientFactory;
import com.clanwts.bncs.client.SimpleBattleNetChatClientListener;
import com.clanwts.bncs.codec.standard.messages.GetAdvListExServer;
import com.clanwts.bncs.codec.standard.messages.ProductType;
import edu.cmu.ece.agora.futures.Future;

public abstract class AbstractBattleNetChatBot implements BattleNetChatBot {

    private static int RECONNECT_DELAY_SEC = 30;

    private static int REJOIN_DELAY_SEC = 10;

    private final XLogger log;

    private final String host;

    private final int port;

    private final String user;

    private final String pass;

    private final boolean pvpgn;

    private final String channel;

    private final BattleNetChatClient bncs;

    private boolean started;

    private Timer reconnectTimer;

    private Timer rejoinTimer;

    protected AbstractBattleNetChatBot(BattleNetChatClientFactory fact, String host, int port, String user, String pass, boolean pvpgn, String channel) {
        log = XLoggerFactory.getXLogger(AbstractBattleNetChatBot.class.getCanonicalName() + " (" + user + "@" + host + ":" + port + ")");
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.pvpgn = pvpgn;
        this.channel = channel;
        this.bncs = fact.createClient();
        this.bncs.addListener(new Listener());
        this.started = false;
    }

    public final String getHost() {
        return host;
    }

    public final int getPort() {
        return port;
    }

    public final String getUsername() {
        return user;
    }

    public final String getPassword() {
        return pass;
    }

    public final boolean isPvpgn() {
        return pvpgn;
    }

    public final String getChannel() {
        return channel;
    }

    public final synchronized void start() {
        if (isStarted()) throw new IllegalStateException("Already started.");
        started = true;
        this.onRestart();
        startConnectTask(0, RECONNECT_DELAY_SEC * 1000);
    }

    public final synchronized boolean isStarted() {
        return started;
    }

    public final synchronized void stop() {
        if (!isStarted()) throw new IllegalStateException("Not started.");
        started = false;
        if (isJoinTaskStarted()) {
            stopJoinTask();
        }
        if (isConnectTaskStarted()) {
            stopConnectTask();
        }
        if (bncs.isConnected()) {
            try {
                bncs.disconnect().get();
            } catch (Exception e) {
                log.error("Exception on disconnect.", e);
            }
        }
    }

    private synchronized void startConnectTask(int delay, int period) {
        if (this.reconnectTimer != null) throw new AssertionError();
        this.reconnectTimer = new Timer();
        this.reconnectTimer.schedule(new AsyncConnectTask(), delay, period);
    }

    private synchronized boolean isConnectTaskStarted() {
        return this.reconnectTimer != null;
    }

    private synchronized void stopConnectTask() {
        if (this.reconnectTimer == null) throw new AssertionError();
        this.reconnectTimer.cancel();
        this.reconnectTimer = null;
    }

    private class AsyncConnectTask extends TimerTask {

        @Override
        public void run() {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                try {
                    if (!isConnectTaskStarted()) return;
                    log.debug("Connecting...");
                    doConnect();
                    if (isConnectTaskStarted()) {
                        stopConnectTask();
                    } else {
                        log.warn("FIXME: Connect task not running after connection complete.");
                    }
                    if (!isJoinTaskStarted()) {
                        startJoinTask(0, REJOIN_DELAY_SEC * 1000);
                    } else {
                        log.warn("FIXME: Join task already running after connection complete.");
                    }
                    log.info("Connect SUCCESS!");
                } catch (Exception e) {
                    if (bncs.isConnected()) {
                        try {
                            bncs.disconnect().get();
                        } catch (Exception e2) {
                            log.error("Exception on disconnect.", e2);
                        }
                    }
                    log.warn("Connect FAILURE!");
                }
            }
            log.exit();
        }

        private void doConnect() throws Exception {
            log.entry();
            log.debug("Beginning connection...");
            bncs.connect(new InetSocketAddress(host, port)).get();
            log.debug("Connection complete.");
            log.debug("Beginning login...");
            bncs.login(user, pass, pvpgn).get();
            log.debug("Login complete.");
            log.exit();
        }
    }

    private synchronized void startJoinTask(int delay, int period) {
        if (this.rejoinTimer != null) throw new AssertionError();
        this.rejoinTimer = new Timer();
        this.rejoinTimer.schedule(new AsyncJoinTask(), delay, period);
    }

    private synchronized boolean isJoinTaskStarted() {
        return this.rejoinTimer != null;
    }

    private synchronized void stopJoinTask() {
        if (this.rejoinTimer == null) throw new AssertionError();
        this.rejoinTimer.cancel();
        this.rejoinTimer = null;
    }

    private class AsyncJoinTask extends TimerTask {

        @Override
        public void run() {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (!isJoinTaskStarted()) return;
                log.debug("Attempting to rejoin channel...");
                bncs.join(channel);
            }
            log.exit();
        }
    }

    private class Listener extends SimpleBattleNetChatClientListener {

        @Override
        public void forcedDisconnect() {
            log.entry();
            log.warn("Forced disconnect!");
            synchronized (AbstractBattleNetChatBot.this) {
                if (!bncs.isConnected()) {
                    AbstractBattleNetChatBot.this.onRestart();
                    if (isJoinTaskStarted()) stopJoinTask();
                    if (!isConnectTaskStarted()) startConnectTask(RECONNECT_DELAY_SEC * 1000, RECONNECT_DELAY_SEC * 1000);
                }
            }
            log.exit();
        }

        @Override
        public void channelChatReceived(String user, String message) {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (!isJoinTaskStarted()) {
                    AbstractBattleNetChatBot.this.onChannelChatReceived(user, message);
                }
            }
            log.exit();
        }

        @Override
        public void forcedJoin(String channel) {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (channel.equalsIgnoreCase(AbstractBattleNetChatBot.this.channel)) {
                    if (isJoinTaskStarted()) {
                        stopJoinTask();
                    }
                } else {
                    AbstractBattleNetChatBot.this.onRestart();
                    if (!isJoinTaskStarted()) {
                        startJoinTask(REJOIN_DELAY_SEC * 1000, REJOIN_DELAY_SEC * 1000);
                    }
                }
            }
            log.exit();
        }

        @Override
        public void privateChatReceived(String user, String message) {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (!isJoinTaskStarted()) {
                    AbstractBattleNetChatBot.this.onPrivateChatReceived(user, message);
                }
            }
            log.exit();
        }

        @Override
        public void errorMessageReceived(String msg) {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (!isJoinTaskStarted()) {
                    AbstractBattleNetChatBot.this.onErrorMessageReceived(msg);
                }
            }
            log.exit();
        }

        @Override
        public void infoMessageReceived(String msg) {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (!isJoinTaskStarted()) {
                    AbstractBattleNetChatBot.this.onInfoMessageReceived(msg);
                }
            }
            log.exit();
        }

        @Override
        public void otherJoined(String user, ProductType type) {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (!isJoinTaskStarted()) {
                    AbstractBattleNetChatBot.this.onOtherJoined(user, type);
                }
            }
            log.exit();
        }

        @Override
        public void otherParted(String user) {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (!isJoinTaskStarted()) {
                    AbstractBattleNetChatBot.this.onOtherParted(user);
                }
            }
            log.exit();
        }

        @Override
        public void otherShown(String user, ProductType type) {
            log.entry();
            synchronized (AbstractBattleNetChatBot.this) {
                if (!isJoinTaskStarted()) {
                    AbstractBattleNetChatBot.this.onOtherShown(user, type);
                }
            }
            log.exit();
        }
    }

    protected final Future<GetAdvListExServer> getGameList() {
        return bncs.getGameList();
    }

    protected final Future<Void> sendChannelChat(String message) {
        return bncs.sendChannelChat(message);
    }

    protected final Future<Void> sendPrivateChat(String user, String message) {
        return bncs.sendPrivateChat(user, message);
    }

    protected final Future<Void> beginLobby(String name) {
        return bncs.beginLobby(name);
    }

    protected final void endLobby() {
        bncs.endLobby();
    }

    protected abstract void onOtherShown(String user, ProductType type);

    protected abstract void onOtherParted(String user);

    protected abstract void onOtherJoined(String user, ProductType type);

    protected abstract void onInfoMessageReceived(String msg);

    protected abstract void onErrorMessageReceived(String msg);

    protected abstract void onChannelChatReceived(String user, String message);

    protected abstract void onPrivateChatReceived(String user, String message);

    protected abstract void onRestart();
}
