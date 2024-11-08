package com.clanwts.bncs.client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import com.clanwts.bncs.codec.AbstractMessage;
import com.clanwts.bncs.codec.UnknownMessage;
import com.clanwts.bncs.codec.standard.messages.AccountLogonClient;
import com.clanwts.bncs.codec.standard.messages.AccountLogonProofClient;
import com.clanwts.bncs.codec.standard.messages.AccountLogonProofServer;
import com.clanwts.bncs.codec.standard.messages.AccountLogonServer;
import com.clanwts.bncs.codec.standard.messages.AdvertisementType;
import com.clanwts.bncs.codec.standard.messages.AuthCheckClient;
import com.clanwts.bncs.codec.standard.messages.AuthCheckServer;
import com.clanwts.bncs.codec.standard.messages.AuthInfoClient;
import com.clanwts.bncs.codec.standard.messages.AuthInfoServer;
import com.clanwts.bncs.codec.standard.messages.BattleType;
import com.clanwts.bncs.codec.standard.messages.ChatCommand;
import com.clanwts.bncs.codec.standard.messages.ChatEvent;
import com.clanwts.bncs.codec.standard.messages.EnterChatClient;
import com.clanwts.bncs.codec.standard.messages.EnterChatServer;
import com.clanwts.bncs.codec.standard.messages.GameObservers;
import com.clanwts.bncs.codec.standard.messages.GameSpeed;
import com.clanwts.bncs.codec.standard.messages.GameType;
import com.clanwts.bncs.codec.standard.messages.GameVisibility;
import com.clanwts.bncs.codec.standard.messages.GetAdvListExClient;
import com.clanwts.bncs.codec.standard.messages.GetAdvListExServer;
import com.clanwts.bncs.codec.standard.messages.JoinChannel;
import com.clanwts.bncs.codec.standard.messages.KeyInfo;
import com.clanwts.bncs.codec.standard.messages.LobbyVisibility;
import com.clanwts.bncs.codec.standard.messages.MapAuthorType;
import com.clanwts.bncs.codec.standard.messages.MapSize;
import com.clanwts.bncs.codec.standard.messages.NetGamePort;
import com.clanwts.bncs.codec.standard.messages.Null;
import com.clanwts.bncs.codec.standard.messages.ObserverType;
import com.clanwts.bncs.codec.standard.messages.Platform;
import com.clanwts.bncs.codec.standard.messages.Product;
import com.clanwts.bncs.codec.standard.messages.ProductType;
import com.clanwts.bncs.codec.standard.messages.StartAdvEx3Client;
import com.clanwts.bncs.codec.standard.messages.StartAdvEx3Server;
import com.clanwts.bncs.codec.standard.messages.StatStringInfo;
import com.clanwts.bncs.codec.standard.messages.StopAdv;
import com.clanwts.bncs.codec.standard.messages.WardenServer;
import com.clanwts.bncs.codec.standard.messages.StartAdvEx3Server.Result;
import com.clanwts.bncs.util.BrokenSHA1;
import com.clanwts.bncs.util.CheckrevisionResults;
import com.clanwts.bncs.util.Constants;
import com.clanwts.bncs.util.HashMain;
import com.clanwts.bncs.util.SRP;
import com.clanwts.bncs.util.War3Decode;
import com.clanwts.bnls.client.BnlsClient;
import com.clanwts.bnls.codec.standard.messages.WardenResult;
import edu.cmu.ece.agora.chat.PrivateChat;
import edu.cmu.ece.agora.chat.PrivateChatListener;
import edu.cmu.ece.agora.chat.SingleChannelChat;
import edu.cmu.ece.agora.chat.SingleChannelChatListener;
import edu.cmu.ece.agora.core.Client;
import edu.cmu.ece.agora.core.ClientListener;
import edu.cmu.ece.agora.core.EventDispatcher;
import edu.cmu.ece.agora.futures.Future;
import edu.cmu.ece.agora.futures.FutureListener;
import edu.cmu.ece.agora.futures.FuturePackage;
import edu.cmu.ece.agora.futures.Futures;

public class BattleNetChatClient implements Client, SingleChannelChat, PrivateChat {

    private static final XLogger log = XLoggerFactory.getXLogger(BattleNetChatClient.class);

    private static final Random random = new Random(System.currentTimeMillis());

    private static final String NULL_CHANNEL = "The Void";

    private static final int KEEPALIVE_INTERVAL = 5 * 1000;

    private static KeyInfo decodeKey(String key, int clientToken, int serverToken) {
        KeyInfo ki = new KeyInfo();
        War3Decode decoder = new War3Decode(key);
        ki.length = War3Decode.W3_KEYLEN;
        ki.product = decoder.getProduct();
        ki.publicValue = decoder.getVal1();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        int[] hash = decoder.getKeyHash(clientToken, serverToken);
        for (int i = 0; i < hash.length; i++) {
            try {
                dos.writeInt(ChannelBuffers.swapInt(hash[i]));
            } catch (IOException e) {
            }
        }
        ki.hash = baos.toByteArray().clone();
        return ki;
    }

    private interface MessageHandler<M extends AbstractMessage> {

        public void handle(M msg);
    }

    private class SessionInfo {

        public SessionState state = SessionState.DISCONNECTED;

        public Channel sock = null;

        public Throwable dcreason = null;

        public long number = 0;

        public String channel = null;

        public int nlsRevision = 0;

        public String username;

        public String password;

        public SRP srp = null;

        public boolean pvpgn = false;

        public boolean wardenInitialized = false;

        public int wardenToken = 0;

        public byte[] wardenSeed = null;
    }

    private enum SessionState {

        DISCONNECTED("DISCONNECTED"), CONNECT_WAIT("CONNECT_WAIT"), AUTH_INFO_WAIT("AUTH_INFO_WAIT"), AUTH_CHECK_WAIT("AUTH_CHECK_WAIT"), LOGIN_WAIT("LOGIN_WAIT"), LOGIN_START("LOGIN_START"), ACCOUNT_LOGON_WAIT("ACCOUNT_LOGON_WAIT"), ACCOUNT_LOGON_PROOF_WAIT("ACCOUNT_LOGON_PROOF_WAIT"), ENTER_CHAT_WAIT("ENTER_CHAT_WAIT"), IN_CHAT("IN_CHAT"), IN_LOBBY("IN_LOBBY");

        private final String name;

        private SessionState(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Platform platform;

    private final Product product;

    private final String[] keys;

    private final String keyOwner;

    private final InetSocketAddress wardenAddr;

    private final Executor exec;

    private final EventDispatcher ed;

    private final SessionInfo si;

    private final ClientBootstrap cbs;

    private final BnlsClient bnls;

    private final Map<Class<? extends AbstractMessage>, MessageHandler<? extends AbstractMessage>> handlers;

    private FuturePackage<Void> connectfp = null;

    private FuturePackage<Void> discfp = null;

    private FuturePackage<Void> loginfp = null;

    private FuturePackage<GetAdvListExServer> gglfp = null;

    private FuturePackage<Void> lobbyfp = null;

    private Timer nullTimer = null;

    BattleNetChatClient(Platform platform, Product product, String[] keys, String keyOwner, InetSocketAddress wardenAddr) {
        if (keys.length != 2) {
            throw new IllegalArgumentException("Incorrect number of keys for product (required: " + 2 + ", presented: " + keys.length + ").");
        }
        this.platform = platform;
        this.product = product;
        this.keys = keys.clone();
        this.keyOwner = keyOwner;
        this.wardenAddr = wardenAddr;
        this.exec = Executors.newSingleThreadExecutor();
        this.ed = new EventDispatcher(this.exec);
        this.si = new SessionInfo();
        this.bnls = new BnlsClient();
        Map<Class<? extends AbstractMessage>, MessageHandler<? extends AbstractMessage>> handlers;
        handlers = new HashMap<Class<? extends AbstractMessage>, MessageHandler<? extends AbstractMessage>>();
        handlers.put(AuthInfoServer.class, new AuthInfoServerHandler());
        handlers.put(AuthCheckServer.class, new AuthCheckServerHandler());
        handlers.put(AccountLogonServer.class, new AccountLogonServerHandler());
        handlers.put(AccountLogonProofServer.class, new AccountLogonProofServerHandler());
        handlers.put(EnterChatServer.class, new EnterChatServerHandler());
        handlers.put(ChatEvent.class, new ChatEventHandler());
        handlers.put(GetAdvListExServer.class, new GetAdvListExServerHandler());
        handlers.put(StartAdvEx3Server.class, new StartAdvEx3ServerHandler());
        handlers.put(WardenServer.class, new WardenHandler());
        handlers.put(UnknownMessage.class, new UnknownMessageHandler());
        this.handlers = Collections.unmodifiableMap(handlers);
        ChannelFactory cf = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        this.cbs = new ClientBootstrap(cf);
        cbs.setOption("tcpNoDelay", true);
        cbs.setOption("keepAlive", true);
        ChannelPipeline cp = cbs.getPipeline();
        cp.addLast("decoder", new com.clanwts.bncs.codec.standard.MessageDecoder(false));
        cp.addLast("encoder", new edu.cmu.ece.agora.codecs.MessageEncoder());
        cp.addLast("handler", new ChannelEventHandler());
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

    @Override
    public synchronized String getCurrentChannel() {
        if (si.state != SessionState.IN_CHAT) {
            throw new IllegalStateException("Not in chat (" + si.state + ").");
        }
        return si.channel;
    }

    @Override
    public synchronized Future<Void> join(String channel) {
        if (si.state != SessionState.IN_CHAT) {
            return Futures.newCancelledFuture(new IllegalStateException("Not in chat (" + si.state + ")."));
        }
        JoinChannel jc = new JoinChannel();
        jc.channel = channel;
        jc.flags = JoinChannel.Flags.FORCED;
        si.sock.write(jc);
        return Futures.newCompletedFuture(null);
    }

    @Override
    public synchronized Future<Void> part() {
        return join(NULL_CHANNEL);
    }

    @Override
    public synchronized Future<Void> sendChannelChat(String message) {
        if (si.state != SessionState.IN_CHAT) {
            return Futures.newCancelledFuture(new IllegalStateException("Not in chat."));
        }
        if (si.channel == null) {
            return Futures.newCancelledFuture(new IllegalStateException("Not in a channel."));
        }
        final FuturePackage<Void> fp = Futures.newFuturePackage(exec);
        ChatCommand cc = new ChatCommand();
        cc.text = message;
        si.sock.write(cc);
        return fp.getFuture();
    }

    public synchronized Future<Void> beginLobby(String name) {
        if (si.state != SessionState.IN_CHAT) {
            throw new IllegalStateException("Not in chat.");
        }
        if (lobbyfp != null) {
            throw new IllegalStateException("A lobby start is already in progress.");
        }
        log.info("Starting lobby \"" + name + "\".");
        lobbyfp = Futures.newFuturePackage(exec);
        StartAdvEx3Client saec = new StartAdvEx3Client();
        saec.advType = AdvertisementType.PUBLIC;
        saec.battleType = BattleType.MELEE;
        saec.gameName = name;
        saec.gamePass = "";
        saec.gameType = GameType.BLIZZARD_LADDER;
        saec.hostCounter = "10000000";
        saec.lobbyVisibility = LobbyVisibility.PUBLIC;
        saec.mapAuthor = MapAuthorType.OFFICIAL;
        saec.mapSize = MapSize.NORMAL;
        saec.observerType = ObserverType.NONE;
        saec.slotsFree = 10;
        saec.ssi = new StatStringInfo();
        saec.ssi.gameObservers = GameObservers.NONE;
        saec.ssi.gameSpeed = GameSpeed.FAST;
        saec.ssi.gameVisibility = GameVisibility.DEFAULT;
        saec.ssi.heroesRandom = false;
        saec.ssi.hostName = si.username;
        saec.ssi.mapCRC = 0x30B9D599;
        saec.ssi.mapHeight = 0x0078;
        saec.ssi.mapPath = "Maps\\Download\\DotA Allstars v6.61c.w3x";
        saec.ssi.mapSHA1 = new byte[] { (byte) 210, 44, 50, (byte) 147, 16, (byte) 252, (byte) 163, (byte) 222, 118, (byte) 245, (byte) 224, 44, 108, (byte) 207, 100, (byte) 138, 114, (byte) 198, 0, 89 };
        saec.ssi.mapWidth = 0x0076;
        saec.ssi.racesRandom = false;
        saec.ssi.teamsFixed = true;
        saec.ssi.teamsTogether = false;
        saec.ssi.unitsShared = false;
        saec.upTime = 0x00000000;
        si.sock.write(saec);
        return lobbyfp.getFuture();
    }

    public synchronized void endLobby() {
        if (si.state != SessionState.IN_LOBBY) {
            throw new IllegalStateException("Not in lobby.");
        }
        log.info("Stopping lobby.");
        StopAdv sa = new StopAdv();
        si.sock.write(sa);
        si.state = SessionState.IN_CHAT;
    }

    @Override
    public synchronized Future<Void> connect(SocketAddress address) {
        log.entry(address);
        if (si.state != SessionState.DISCONNECTED) {
            return Futures.newCancelledFuture(new IllegalStateException("Not disconnected (" + si.state + ")."));
        }
        si.state = SessionState.CONNECT_WAIT;
        si.number++;
        connectfp = Futures.newFuturePackage(exec);
        cbs.connect(address).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture arg0) throws Exception {
                synchronized (BattleNetChatClient.this) {
                    if (!arg0.isSuccess()) {
                        log.warn("BNCS connection failed!");
                        si.state = SessionState.DISCONNECTED;
                        connectfp.getManager().cancelFuture(arg0.getCause());
                        connectfp = null;
                    }
                }
            }
        });
        return connectfp.getFuture();
    }

    @Override
    public synchronized Future<Void> disconnect() {
        if (si.state == SessionState.CONNECT_WAIT) {
            return Futures.newCancelledFuture(new IllegalStateException("Cannot disconnect - a connection is in progress."));
        }
        if (si.state == SessionState.DISCONNECTED) {
            return Futures.newCompletedFuture(null);
        }
        FuturePackage<Void> localdiscfp = Futures.newFuturePackage(exec);
        discfp = localdiscfp;
        si.sock.close().addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    discfp.getManager().cancelFuture(future.getCause());
                }
            }
        });
        return localdiscfp.getFuture();
    }

    @Override
    public synchronized SocketAddress getServerAddress() {
        if (si.state == SessionState.DISCONNECTED || si.state == SessionState.CONNECT_WAIT) {
            throw new IllegalStateException("Not connected (" + si.state + ").");
        }
        return si.sock.getRemoteAddress();
    }

    @Override
    public synchronized boolean isConnected() {
        return !(si.state == SessionState.DISCONNECTED || si.state == SessionState.CONNECT_WAIT);
    }

    private void beginHandshake() {
        log.trace("Beginning BNCS handshake...");
        ChannelBuffer buf = ChannelBuffers.directBuffer(1);
        buf.writeByte((byte) 0x01);
        si.sock.write(buf);
        log.trace("Sent protocol byte.");
        AuthInfoClient aic = new AuthInfoClient();
        aic.protocolID = 0x00000000;
        aic.platformID = platform.getBlizzardCode();
        aic.productID = product.getType().getBlizzardCode();
        aic.version = product.getVersionByte();
        aic.productLanguage = 0x656e5553;
        aic.localIP = 0x00000000;
        aic.timeZoneBias = 0x00000000;
        aic.localeID = 0x00000409;
        aic.languageID = 0x00000409;
        aic.countryAbbr = "USA";
        aic.countryName = "United States";
        synchronized (BattleNetChatClient.this) {
            si.sock.write(aic);
            si.state = SessionState.AUTH_INFO_WAIT;
        }
        log.trace("Sent AuthInfoClient to BNCS.");
    }

    private class AuthInfoServerHandler implements MessageHandler<AuthInfoServer> {

        @Override
        public void handle(final AuthInfoServer msg) {
            synchronized (BattleNetChatClient.this) {
                if (si.state != SessionState.AUTH_INFO_WAIT) {
                    return;
                }
            }
            log.trace("Received AuthInfoServer from BNCS.");
            si.nlsRevision = msg.logonType.getCode();
            AuthCheckClient acc = new AuthCheckClient();
            acc.clientToken = random.nextInt();
            CheckrevisionResults crr = HashMain.getRevision(Constants.PRODUCT_THEFROZENTHRONE, msg.valueString, msg.ix86Filename, msg.mpqFiletime, product.getVersionDescription());
            if (crr == null) {
                abortHandshake(new Exception("Could not obtain check-revision results."));
                return;
            }
            acc.exeVersion = ((product.getFullVersionCode() & 0xFFFFFF) << 8) | (((crr.getVersion()) >> 24) & 0xFF);
            acc.exeHash = crr.getChecksum();
            acc.exeInfo = crr.getInfo();
            acc.isSpawn = false;
            acc.keys = new KeyInfo[keys.length];
            for (int i = 0; i < keys.length; i++) {
                KeyInfo ki = decodeKey(keys[i], acc.clientToken, msg.serverToken);
                if (i == 0) {
                    switch(product.getType()) {
                        case W3ROC:
                        case W3TFT:
                            si.wardenSeed = Arrays.copyOfRange(ki.hash, 0, 4);
                    }
                }
                acc.keys[i] = ki;
            }
            acc.keyOwner = keyOwner;
            synchronized (BattleNetChatClient.this) {
                si.sock.write(acc);
                si.state = SessionState.AUTH_CHECK_WAIT;
            }
            log.trace("Sent AuthCheckClient to BNCS.");
        }
    }

    private class AuthCheckServerHandler implements MessageHandler<AuthCheckServer> {

        @Override
        public void handle(AuthCheckServer msg) {
            synchronized (BattleNetChatClient.this) {
                if (si.state != SessionState.AUTH_CHECK_WAIT) {
                    return;
                }
            }
            log.trace("Received AuthCheckServer from BNCS.");
            log.trace("Auth Check result: " + msg.status.getDescription());
            if (msg.status != AuthCheckServer.Status.CHALLENGE_PASSED) {
                abortHandshake(new Exception("BNCS AuthCheck failed (" + msg.status.getDescription() + ") - " + msg.additionalInfo + "."));
                return;
            }
            completeHandshake();
        }
    }

    private synchronized void completeHandshake() {
        si.state = SessionState.LOGIN_WAIT;
        connectfp.getManager().completeFuture(null);
        connectfp = null;
    }

    private synchronized void abortHandshake(Throwable cause) {
        si.dcreason = cause;
        si.sock.close();
    }

    public synchronized Future<Void> login(final String user, final String pass, boolean pvpgn) {
        if (si.state != SessionState.LOGIN_WAIT) {
            return Futures.newCancelledFuture(new IllegalStateException("Not ready for login (" + si.state + ")."));
        }
        si.state = SessionState.LOGIN_START;
        loginfp = Futures.newFuturePackage(exec);
        si.username = user;
        si.password = pass;
        si.srp = new SRP(user.toLowerCase(), pass.toLowerCase());
        si.srp.set_NLS(si.nlsRevision);
        si.pvpgn = pvpgn;
        AccountLogonClient alc = new AccountLogonClient();
        alc.clientKey = si.srp.get_A();
        alc.username = user;
        synchronized (BattleNetChatClient.this) {
            si.sock.write(alc);
            si.state = SessionState.ACCOUNT_LOGON_WAIT;
        }
        log.trace("Sent AccountLogonClient to BNCS.");
        return loginfp.getFuture();
    }

    private class AccountLogonServerHandler implements MessageHandler<AccountLogonServer> {

        @Override
        public void handle(AccountLogonServer als) {
            synchronized (BattleNetChatClient.this) {
                if (si.state != SessionState.ACCOUNT_LOGON_WAIT) {
                    return;
                }
            }
            if (als.status != AccountLogonServer.Status.LOGON_ACCEPTED) {
                log.trace("BNCS logon challenge failed!");
                abortLogin(new Exception("Account logon failed (" + als.status.getDescription() + ")."));
                return;
            }
            AccountLogonProofClient alpc = new AccountLogonProofClient();
            try {
                alpc.proof = (si.pvpgn) ? BrokenSHA1.calcHashBuffer(si.password.toLowerCase().getBytes("US-ASCII")) : si.srp.getM1(als.salt, als.serverKey);
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError();
            }
            synchronized (BattleNetChatClient.this) {
                si.sock.write(alpc);
                si.state = SessionState.ACCOUNT_LOGON_PROOF_WAIT;
            }
            log.trace("Sent AccountLogonProof to BNCS.");
        }
    }

    private class AccountLogonProofServerHandler implements MessageHandler<AccountLogonProofServer> {

        @Override
        public void handle(AccountLogonProofServer alps) {
            synchronized (BattleNetChatClient.this) {
                if (si.state != SessionState.ACCOUNT_LOGON_PROOF_WAIT) {
                    return;
                }
            }
            log.trace("Received AccountLogonProof from BNCS.");
            if (alps.status != AccountLogonProofServer.Status.LOGON_SUCCESSFUL) {
                log.trace("BNCS logon proof failed!");
                abortLogin(new Exception("Account logon proof failed (" + alps.status.getDescription() + ") - " + alps.additionalStatusInfo + "."));
                return;
            }
            NetGamePort ngp = new NetGamePort();
            ngp.port = 6666;
            EnterChatClient ecc = new EnterChatClient();
            ecc.statstring = "";
            ecc.username = "";
            synchronized (BattleNetChatClient.this) {
                si.sock.write(ngp);
                si.sock.write(ecc);
                si.state = SessionState.ENTER_CHAT_WAIT;
            }
            log.trace("Sent EnterChatClient to BNCS.");
        }
    }

    private class EnterChatServerHandler implements MessageHandler<EnterChatServer> {

        @Override
        public void handle(EnterChatServer ecs) {
            log.trace("Received EnterChatClient from BNCS.");
            synchronized (BattleNetChatClient.this) {
                if (si.state != SessionState.ENTER_CHAT_WAIT) {
                    return;
                }
                completeLogin();
            }
        }
    }

    private synchronized void completeLogin() {
        log.trace("Completing login.");
        si.state = SessionState.IN_CHAT;
        loginfp.getManager().completeFuture(null);
        loginfp = null;
    }

    private synchronized void abortLogin(Throwable cause) {
        si.state = SessionState.LOGIN_WAIT;
        loginfp.getManager().cancelFuture(cause);
        loginfp = null;
    }

    @Override
    public synchronized Future<Void> sendPrivateChat(String user, String message) {
        if (si.state != SessionState.IN_CHAT) {
            return Futures.newCancelledFuture(new IllegalStateException("Not in chat (" + si.state + ")."));
        }
        final FuturePackage<Void> fp = Futures.newFuturePackage();
        ChatCommand cc = new ChatCommand();
        cc.text = "/w " + user + " " + message;
        si.sock.write(cc).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture arg0) throws Exception {
                if (!arg0.isSuccess()) {
                    fp.getManager().cancelFuture(arg0.getCause());
                } else {
                    fp.getManager().completeFuture(null);
                }
            }
        });
        return fp.getFuture();
    }

    private class ChatEventHandler implements MessageHandler<ChatEvent> {

        @Override
        public void handle(ChatEvent ce) {
            log.trace("Received ChatEvent from BNCS.");
            log.trace("Session state: " + si.state);
            synchronized (BattleNetChatClient.this) {
                if (si.state == SessionState.IN_CHAT) {
                    try {
                        switch(ce.eventId) {
                            case WHISPER:
                                ed.dispatch(PrivateChatListener.class, "privateChatReceived", ce.username, ce.text);
                                break;
                            case TALK:
                                ed.dispatch(SingleChannelChatListener.class, "channelChatReceived", ce.username, ce.text);
                                break;
                            case CHANNEL:
                                si.channel = ce.text;
                                ed.dispatch(SingleChannelChatListener.class, "forcedJoin", ce.text);
                                break;
                            case SHOW_USER:
                                ed.dispatch(SingleChannelChatListener.class, "otherShown", ce.username);
                                ed.dispatch(BattleNetChatClientListener.class, "otherShown", ce.username, ProductType.forBlizzardStatStringId(ce.text.substring(0, 4)));
                                break;
                            case JOIN:
                                ed.dispatch(SingleChannelChatListener.class, "otherJoined", ce.username);
                                ed.dispatch(BattleNetChatClientListener.class, "otherJoined", ce.username, ProductType.forBlizzardStatStringId(ce.text.substring(0, 4)));
                                break;
                            case LEAVE:
                                ed.dispatch(SingleChannelChatListener.class, "otherParted", ce.username);
                                break;
                            case INFO:
                                ed.dispatch(BattleNetChatClientListener.class, "infoMessageReceived", ce.text);
                                break;
                            case ERROR:
                                ed.dispatch(BattleNetChatClientListener.class, "errorMessageReceived", ce.text);
                                break;
                        }
                    } catch (SecurityException e) {
                        log.catching(e);
                        AssertionError ae = new AssertionError(e);
                        log.throwing(ae);
                        throw ae;
                    } catch (NoSuchMethodException e) {
                        log.catching(e);
                        AssertionError ae = new AssertionError(e);
                        log.throwing(ae);
                        throw ae;
                    }
                } else {
                    return;
                }
            }
        }
    }

    public synchronized Future<GetAdvListExServer> getGameList() {
        if (si.state != SessionState.IN_CHAT) {
            return Futures.newCancelledFuture(new IllegalStateException("Not in chat (" + si.state + ")."));
        }
        if (gglfp != null) {
            return gglfp.getFuture();
        }
        gglfp = Futures.newFuturePackage(exec);
        GetAdvListExClient galec = new GetAdvListExClient();
        galec.flags = 0x007fe000;
        galec.mask = 0x00000000;
        galec.maxGames = 20;
        galec.gameName = "";
        galec.gamePass = "";
        galec.gameStats = "";
        si.sock.write(galec);
        return gglfp.getFuture();
    }

    private class GetAdvListExServerHandler implements MessageHandler<GetAdvListExServer> {

        @Override
        public void handle(GetAdvListExServer msg) {
            synchronized (BattleNetChatClient.class) {
                if (si.state != SessionState.IN_CHAT) {
                    return;
                }
                if (gglfp == null) {
                    return;
                }
                FuturePackage<GetAdvListExServer> fp = gglfp;
                gglfp = null;
                fp.getManager().completeFuture(msg);
            }
        }
    }

    private class StartAdvEx3ServerHandler implements MessageHandler<StartAdvEx3Server> {

        @Override
        public void handle(StartAdvEx3Server msg) {
            synchronized (BattleNetChatClient.class) {
                if (lobbyfp == null) {
                    return;
                }
                FuturePackage<Void> fp = lobbyfp;
                lobbyfp = null;
                if (msg.result == Result.SUCCESS) {
                    si.state = SessionState.IN_LOBBY;
                    fp.getManager().completeFuture(null);
                } else {
                    fp.getManager().cancelFuture(new Exception("Lobby advertisement failed."));
                }
            }
        }
    }

    private class WardenHandler implements MessageHandler<WardenServer> {

        @Override
        public void handle(WardenServer msg) {
            log.debug("Received Warden message, attempting to handle...");
            synchronized (BattleNetChatClient.this) {
                switch(si.state) {
                    case DISCONNECTED:
                    case CONNECT_WAIT:
                    case AUTH_INFO_WAIT:
                    case AUTH_CHECK_WAIT:
                        throw new IllegalStateException("Received warden message in invalid state (" + si.state + ").");
                }
                if (!si.wardenInitialized) {
                    initAndHandleWarden(msg);
                } else {
                    handleWarden(msg);
                }
            }
        }

        private void initAndHandleWarden(final WardenServer msg) {
            if (!bnls.isConnected()) {
                connectAndInitAndHandleWarden(msg);
                return;
            }
            log.debug("Initializing session on BNLS-Warden server...");
            si.wardenToken = System.identityHashCode(this);
            com.clanwts.bnls.codec.standard.messages.WardenClient wc = new com.clanwts.bnls.codec.standard.messages.WardenClient();
            wc.cookie = si.wardenToken;
            wc.useage = 0x00;
            wc.client = product.getType().getBlizzardCode();
            wc.seed = si.wardenSeed;
            bnls.requestWardenData(wc).addListener(new FutureListener<com.clanwts.bnls.codec.standard.messages.WardenServer>() {

                @Override
                public void onCancellation(Throwable cause) {
                    log.warn("Unable to initialize session on BNLS-Warden server!", cause);
                }

                @Override
                public void onCompletion(com.clanwts.bnls.codec.standard.messages.WardenServer result) {
                    if (result.result != WardenResult.SUCCESS) {
                        log.warn("Unable to initialize session on BNLS-Warden server: " + result.result.getDescription());
                        return;
                    }
                    log.debug("Warden session initialization complete.");
                    synchronized (BattleNetChatClient.this) {
                        si.wardenInitialized = true;
                        handleWarden(msg);
                    }
                }
            });
        }

        private void connectAndInitAndHandleWarden(final WardenServer msg) {
            bnls.connect(wardenAddr).addListener(new FutureListener<Void>() {

                @Override
                public void onCancellation(Throwable cause) {
                    log.warn("Connection to BNLS-Warden server failed!", cause);
                }

                @Override
                public void onCompletion(Void result) {
                    log.debug("Connection to BNLS-Warden server complete.");
                    initAndHandleWarden(msg);
                }
            });
        }

        private void handleWarden(final WardenServer msg) {
            if (!bnls.isConnected()) {
                connectAndHandleWarden(msg);
                return;
            }
            log.debug("Handling Warden message using BNLS-Warden server...");
            final long sessnum = si.number;
            com.clanwts.bnls.codec.standard.messages.WardenClient wc = new com.clanwts.bnls.codec.standard.messages.WardenClient();
            wc.cookie = si.wardenToken;
            wc.useage = 0x01;
            wc.data = msg.data;
            bnls.requestWardenData(wc).addListener(new FutureListener<com.clanwts.bnls.codec.standard.messages.WardenServer>() {

                @Override
                public void onCancellation(Throwable cause) {
                    log.warn("Unable to get Warden response from BNLS-Warden server!", cause);
                }

                @Override
                public void onCompletion(com.clanwts.bnls.codec.standard.messages.WardenServer result) {
                    if (result.result != WardenResult.SUCCESS) {
                        log.warn("Processing of Warden response on BNLS-Warden server failed: " + result.result.getDescription());
                        return;
                    }
                    log.debug("Processing of Warden response complete.");
                    com.clanwts.bncs.codec.standard.messages.WardenClient wc = new com.clanwts.bncs.codec.standard.messages.WardenClient();
                    wc.data = result.data;
                    synchronized (BattleNetChatClient.this) {
                        if (sessnum == si.number && si.state != SessionState.DISCONNECTED) {
                            si.sock.write(wc);
                        }
                    }
                }
            });
        }

        private void connectAndHandleWarden(final WardenServer msg) {
            log.debug("Connecting to BNLS-Warden server.");
            bnls.connect(wardenAddr).addListener(new FutureListener<Void>() {

                @Override
                public void onCancellation(Throwable cause) {
                    log.warn("Connection to BNLS-Warden server failed!", cause);
                }

                @Override
                public void onCompletion(Void result) {
                    log.debug("Connection to BNLS-Warden server complete.");
                    handleWarden(msg);
                }
            });
        }
    }

    private class UnknownMessageHandler implements MessageHandler<UnknownMessage> {

        @Override
        public void handle(UnknownMessage msg) {
        }
    }

    private class KeepAliveTask extends TimerTask {

        @Override
        public void run() {
            synchronized (BattleNetChatClient.this) {
                if (si.state == SessionState.DISCONNECTED) {
                    return;
                }
                Null n = new Null();
                si.sock.write(n);
            }
        }
    }

    @ChannelPipelineCoverage("one")
    public class ChannelEventHandler extends SimpleChannelHandler {

        @SuppressWarnings("unchecked")
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Object msg = e.getMessage();
            if (msg == null || !(msg instanceof AbstractMessage)) {
                return;
            }
            MessageHandler handler = handlers.get(msg.getClass());
            if (handler != null) {
                handler.handle((AbstractMessage) msg);
            }
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            log.trace("Connected!");
            log.trace("Session state: " + si.state);
            synchronized (BattleNetChatClient.this) {
                if (si.state != SessionState.CONNECT_WAIT) {
                    AssertionError ae = new AssertionError();
                    log.throwing(ae);
                    throw ae;
                }
                si.sock = ctx.getChannel();
                nullTimer = new Timer();
                nullTimer.schedule(new KeepAliveTask(), KEEPALIVE_INTERVAL, KEEPALIVE_INTERVAL);
                beginHandshake();
            }
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            log.trace("Netty channel closed!");
            log.trace("Session state: " + si.state);
            boolean forced = false;
            List<FuturePackage<?>> cancelfps = new ArrayList<FuturePackage<?>>();
            Throwable reason;
            FuturePackage<Void> tempdiscfp;
            synchronized (BattleNetChatClient.this) {
                if (si.state == SessionState.DISCONNECTED) {
                    AssertionError ae = new AssertionError();
                    log.throwing(ae);
                    throw ae;
                }
                nullTimer.cancel();
                nullTimer = null;
                forced = discfp == null;
                si.state = SessionState.DISCONNECTED;
                si.nlsRevision = 0;
                si.password = null;
                si.pvpgn = false;
                si.srp = null;
                si.username = null;
                si.channel = null;
                si.wardenInitialized = false;
                si.wardenSeed = null;
                si.wardenToken = 0;
                reason = si.dcreason;
                si.dcreason = null;
                if (reason == null) {
                    reason = new Exception("Unexpected disconnection from server.");
                }
                if (connectfp != null) {
                    log.trace("Set connectfp for delayed cancellation.");
                    cancelfps.add(connectfp);
                    connectfp = null;
                }
                if (loginfp != null) {
                    log.trace("Set loginfp for delayed cancellation.");
                    cancelfps.add(loginfp);
                    loginfp = null;
                }
                if (gglfp != null) {
                    log.trace("Set gglfp for delayed cancellation.");
                    cancelfps.add(gglfp);
                    gglfp = null;
                }
                if (lobbyfp != null) {
                    log.trace("Set lobbyfp for delayed cancellation.");
                    cancelfps.add(lobbyfp);
                    lobbyfp = null;
                }
                tempdiscfp = discfp;
                discfp = null;
                si.sock.close();
                si.sock = null;
            }
            log.trace("Cancelling " + cancelfps.size() + " futures due to disconnect.");
            for (FuturePackage<?> fp : cancelfps) {
                fp.getManager().cancelFuture(reason);
            }
            if (forced) {
                log.trace("Dispatching forcedDisconnect.");
                ed.dispatch(ClientListener.class, "forcedDisconnect");
            } else {
                log.trace("Completing disconnect future.");
                tempdiscfp.getManager().completeFuture(null);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            log.warn("Caught exception from Netty channel handler.", e.getCause());
            ctx.getChannel().close();
        }

        @Override
        public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
            if (e instanceof ChannelStateEvent) {
                log.info(e.toString());
            }
            super.handleUpstream(ctx, e);
        }
    }
}
