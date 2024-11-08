package com.clanwts.bnls.client;

import java.net.InetSocketAddress;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
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
import com.clanwts.bnls.codec.AbstractMessage;
import com.clanwts.bnls.codec.standard.MessageDecoder;
import com.clanwts.bnls.codec.standard.messages.CdkeyExClient;
import com.clanwts.bnls.codec.standard.messages.CdkeyExServer;
import com.clanwts.bnls.codec.standard.messages.ChooseNLSRevisionClient;
import com.clanwts.bnls.codec.standard.messages.ChooseNLSRevisionServer;
import com.clanwts.bnls.codec.standard.messages.GetVersionClient;
import com.clanwts.bnls.codec.standard.messages.GetVersionServer;
import com.clanwts.bnls.codec.standard.messages.LogonChallengeClient;
import com.clanwts.bnls.codec.standard.messages.LogonChallengeServer;
import com.clanwts.bnls.codec.standard.messages.LogonProofClient;
import com.clanwts.bnls.codec.standard.messages.LogonProofServer;
import com.clanwts.bnls.codec.standard.messages.Product;
import com.clanwts.bnls.codec.standard.messages.VersionCheckClient;
import com.clanwts.bnls.codec.standard.messages.VersionCheckServer;
import com.clanwts.bnls.codec.standard.messages.WardenClient;
import com.clanwts.bnls.codec.standard.messages.WardenServer;
import edu.cmu.ece.agora.codecs.MessageEncoder;
import edu.cmu.ece.agora.codecs.Tokens;
import edu.cmu.ece.agora.futures.Future;
import edu.cmu.ece.agora.futures.FutureManager;
import edu.cmu.ece.agora.futures.FuturePackage;
import edu.cmu.ece.agora.futures.Futures;

public class BnlsClient {

    private static Logger logger = Logger.getLogger(BnlsClient.class.getName());

    static {
        logger.setLevel(Level.OFF);
    }

    private ClientBootstrap bootstrap;

    private Channel channel;

    private boolean connected = false;

    public BnlsClient() {
        this(Executors.newCachedThreadPool());
    }

    public BnlsClient(Executor exec) {
        this(new NioClientSocketChannelFactory(exec, exec));
    }

    public BnlsClient(ChannelFactory cf) {
        bootstrap = new ClientBootstrap(cf);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        ChannelPipeline cp = bootstrap.getPipeline();
        cp.addLast("decoder", new MessageDecoder(false));
        cp.addLast("encoder", new MessageEncoder());
        cp.addLast("handler", new ChannelHandler());
    }

    private Lock connectLock = new ReentrantLock();

    public Future<Void> connect(String hostname) {
        return connect(new InetSocketAddress(hostname, 9367));
    }

    private FuturePackage<Void> connectFuture = null;

    public Future<Void> connect(InetSocketAddress host) {
        Future<Void> ret;
        connectLock.lock();
        if (isConnected()) {
            connectLock.unlock();
            throw new IllegalStateException("Already connected!");
        }
        if (connectFuture == null) {
            connectFuture = Futures.newFuturePackage();
            ret = connectFuture.getFuture();
            bootstrap.connect(host).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    connectLock.lock();
                    FutureManager<Void> fm = connectFuture.getManager();
                    connectFuture = null;
                    connectLock.unlock();
                    if (!future.isSuccess()) {
                        fm.cancelFuture(future.getCause());
                    } else {
                        channel = future.getChannel();
                        connected = true;
                        fm.completeFuture(null);
                    }
                }
            });
        } else {
            ret = connectFuture.getFuture();
        }
        connectLock.unlock();
        return ret;
    }

    public boolean isConnected() {
        return connected;
    }

    private FuturePackage<Void> disconnectFuture = null;

    public Future<Void> disconnect() {
        Future<Void> ret;
        connectLock.lock();
        if (!isConnected()) {
            connectLock.unlock();
            return Futures.newCompletedFuture(null);
        }
        if (disconnectFuture == null) {
            disconnectFuture = Futures.newFuturePackage();
            ret = disconnectFuture.getFuture();
            channel.close().addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    connectLock.lock();
                    FutureManager<Void> fm = disconnectFuture.getManager();
                    disconnectFuture = null;
                    connectLock.unlock();
                    if (!future.isSuccess()) {
                        fm.cancelFuture(future.getCause());
                    } else {
                        channel = null;
                        connected = false;
                        fm.completeFuture(null);
                    }
                }
            });
        } else {
            ret = disconnectFuture.getFuture();
        }
        connectLock.unlock();
        return ret;
    }

    private final EnumMap<Product, FuturePackage<GetVersionServer>> gvFutures = new EnumMap<Product, FuturePackage<GetVersionServer>>(Product.class);

    public Future<GetVersionServer> requestProductVersion(GetVersionClient gvc) {
        synchronized (gvFutures) {
            if (!gvFutures.containsKey(gvc.product)) {
                gvFutures.put(gvc.product, Futures.<GetVersionServer>newFuturePackage());
                channel.write(gvc);
            }
            return gvFutures.get(gvc.product).getFuture();
        }
    }

    private final Map<Integer, FuturePackage<CdkeyExServer>> cdFutures = new HashMap<Integer, FuturePackage<CdkeyExServer>>();

    public Future<CdkeyExServer> requestCdkeyData(CdkeyExClient req) {
        synchronized (cdFutures) {
            Integer token = Tokens.generateToken(cdFutures.keySet());
            FuturePackage<CdkeyExServer> fp = Futures.newFuturePackage();
            cdFutures.put(token, fp);
            req.cookie = token;
            channel.write(req);
            logger.fine("Sent CD key data request.");
            return fp.getFuture();
        }
    }

    private final Object nlsLock = new Object();

    private FuturePackage<ChooseNLSRevisionServer> nlsFP = null;

    public Future<ChooseNLSRevisionServer> requestNLSRevision(ChooseNLSRevisionClient req) {
        synchronized (nlsLock) {
            if (nlsFP != null) throw new IllegalStateException("Only choose NLS revision request can be active at once!");
            nlsFP = Futures.newFuturePackage();
            channel.write(req);
            return nlsFP.getFuture();
        }
    }

    private final Object lcLock = new Object();

    private FuturePackage<LogonChallengeServer> lcFP = null;

    public Future<LogonChallengeServer> requestLogonChallengeData(LogonChallengeClient req) {
        synchronized (lcLock) {
            if (lcFP != null) throw new IllegalStateException("Only one logon challenge data request can be active at once!");
            lcFP = Futures.newFuturePackage();
            channel.write(req);
            return lcFP.getFuture();
        }
    }

    private final Object lpLock = new Object();

    private FuturePackage<LogonProofServer> lpFP = null;

    public Future<LogonProofServer> requestLogonProofData(LogonProofClient req) {
        synchronized (lpLock) {
            if (lpFP != null) throw new IllegalStateException("Only one logon proof request can be active at once!");
            lpFP = Futures.newFuturePackage();
            channel.write(req);
            return lpFP.getFuture();
        }
    }

    private final Map<Integer, FuturePackage<VersionCheckServer>> vcFutures = new HashMap<Integer, FuturePackage<VersionCheckServer>>();

    public Future<VersionCheckServer> requestVersionCheck(VersionCheckClient req) {
        synchronized (vcFutures) {
            Integer token = Tokens.generateToken(vcFutures.keySet());
            FuturePackage<VersionCheckServer> fp = Futures.newFuturePackage();
            vcFutures.put(token, fp);
            req.cookie = token;
            channel.write(req);
            return fp.getFuture();
        }
    }

    private final Map<Integer, FuturePackage<WardenServer>> wardenFutures = new HashMap<Integer, FuturePackage<WardenServer>>();

    public Future<WardenServer> requestWardenData(WardenClient req) {
        synchronized (wardenFutures) {
            if (wardenFutures.containsKey(req.cookie)) {
                throw new IllegalStateException("Another warden operation is already using token " + req.cookie + "!");
            }
            FuturePackage<WardenServer> fp = Futures.newFuturePackage();
            wardenFutures.put(req.cookie, fp);
            channel.write(req);
            logger.fine("Sent warden request using token " + req.cookie + ".");
            return fp.getFuture();
        }
    }

    @ChannelPipelineCoverage("all")
    private class ChannelHandler extends SimpleChannelHandler {

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            logger.info("Connected to " + e.getChannel().getRemoteAddress().toString() + "!");
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            logger.info("Disconnected!");
            connected = false;
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            if (!(e.getMessage() instanceof AbstractMessage)) {
                logger.warning("Received message of unknown type (non-BNLS)...this is a bad sign.");
                return;
            }
            AbstractMessage m = (AbstractMessage) e.getMessage();
            logger.fine("Received message of type " + m.getClass().getCanonicalName() + ".");
            if (m instanceof GetVersionServer) {
                GetVersionServer gvs = (GetVersionServer) m;
                synchronized (gvFutures) {
                    if (!gvFutures.containsKey(gvs.product)) {
                        throw new IllegalStateException("No request for this product!");
                    }
                    gvFutures.remove(gvs.product).getManager().completeFuture(gvs);
                }
            } else if (m instanceof CdkeyExServer) {
                CdkeyExServer res = (CdkeyExServer) m;
                synchronized (cdFutures) {
                    if (!cdFutures.containsKey(res.cookie)) throw new IllegalStateException("CD key data for token " + res.cookie + " not requested!");
                    FutureManager<CdkeyExServer> fm = cdFutures.remove(res.cookie).getManager();
                    fm.completeFuture(res);
                }
            } else if (m instanceof ChooseNLSRevisionServer) {
                ChooseNLSRevisionServer res = (ChooseNLSRevisionServer) m;
                synchronized (nlsLock) {
                    if (nlsFP == null) throw new IllegalStateException("NLS version change not requested!");
                    FutureManager<ChooseNLSRevisionServer> fm = nlsFP.getManager();
                    nlsFP = null;
                    fm.completeFuture(res);
                }
            } else if (m instanceof LogonChallengeServer) {
                LogonChallengeServer res = (LogonChallengeServer) m;
                synchronized (lcLock) {
                    if (lcFP == null) throw new IllegalStateException("Logon challenge data not requested!");
                    FutureManager<LogonChallengeServer> fm = lcFP.getManager();
                    lcFP = null;
                    fm.completeFuture(res);
                }
            } else if (m instanceof LogonProofServer) {
                LogonProofServer res = (LogonProofServer) m;
                synchronized (lpLock) {
                    if (lpFP == null) throw new IllegalStateException("Logon proof data not requested!");
                    FutureManager<LogonProofServer> fm = lpFP.getManager();
                    lpFP = null;
                    fm.completeFuture(res);
                }
            } else if (m instanceof VersionCheckServer) {
                VersionCheckServer res = (VersionCheckServer) m;
                synchronized (vcFutures) {
                    if (!vcFutures.containsKey(res.cookie)) throw new IllegalStateException("Version check data for token " + res.cookie + " not requested!");
                    FutureManager<VersionCheckServer> fm = vcFutures.remove(res.cookie).getManager();
                    fm.completeFuture(res);
                }
            } else if (m instanceof WardenServer) {
                WardenServer res = (WardenServer) m;
                synchronized (wardenFutures) {
                    if (!wardenFutures.containsKey(res.cookie)) throw new IllegalStateException("Warden data for token " + res.cookie + " not requested!");
                    FutureManager<WardenServer> fm = wardenFutures.remove(res.cookie).getManager();
                    fm.completeFuture(res);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            logger.warning("Exception in BNLSClient: " + e.getCause().toString());
            for (StackTraceElement ste : e.getCause().getStackTrace()) logger.warning("\tAt " + ste.toString());
        }
    }
}
