package navigators.smart.communication.client.netty;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import navigators.smart.communication.client.CommunicationSystemClientSide;
import navigators.smart.communication.client.ReplyReceiver;
import navigators.smart.reconfiguration.ClientViewManager;
import navigators.smart.tom.core.messages.TOMMessage;
import navigators.smart.tom.util.Logger;
import navigators.smart.tom.util.TOMUtil;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 *
 * @author Paulo
 */
@ChannelPipelineCoverage("all")
public class NettyClientServerCommunicationSystemClientSide extends SimpleChannelUpstreamHandler implements CommunicationSystemClientSide {

    private static final String PASSWORD = "newcs";

    protected ReplyReceiver trr;

    private ClientViewManager manager;

    private Map sessionTable = new HashMap();

    private ReentrantReadWriteLock rl;

    private SecretKey authKey;

    private Signature signatureEngine;

    private int signatureLength;

    private boolean closed = false;

    public NettyClientServerCommunicationSystemClientSide(ClientViewManager manager) {
        super();
        try {
            SecretKeyFactory fac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            PBEKeySpec spec = new PBEKeySpec(PASSWORD.toCharArray());
            authKey = fac.generateSecret(spec);
            this.manager = manager;
            this.rl = new ReentrantReadWriteLock();
            Mac macDummy = Mac.getInstance(manager.getStaticConf().getHmacAlgorithm());
            signatureLength = TOMUtil.getSignatureSize(manager);
            int[] currV = manager.getCurrentViewProcesses();
            for (int i = 0; i < currV.length; i++) {
                try {
                    ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
                    bootstrap.setOption("tcpNoDelay", true);
                    bootstrap.setOption("keepAlive", true);
                    bootstrap.setOption("connectTimeoutMillis", 10000);
                    bootstrap.setPipelineFactory(new NettyClientPipelineFactory(this, true, sessionTable, authKey, macDummy.getMacLength(), manager, rl, signatureLength, new ReentrantLock()));
                    ChannelFuture future = bootstrap.connect(manager.getRemoteAddress(currV[i]));
                    Mac macSend = Mac.getInstance(manager.getStaticConf().getHmacAlgorithm());
                    macSend.init(authKey);
                    Mac macReceive = Mac.getInstance(manager.getStaticConf().getHmacAlgorithm());
                    macReceive.init(authKey);
                    NettyClientServerSession cs = new NettyClientServerSession(future.getChannel(), macSend, macReceive, currV[i], manager.getStaticConf().getRSAPublicKey(currV[i]), new ReentrantLock());
                    sessionTable.put(currV[i], cs);
                    System.out.println("Connecting to replica " + currV[i] + " at " + manager.getRemoteAddress(currV[i]));
                    future.awaitUninterruptibly();
                    if (!future.isSuccess()) {
                        System.err.println("Impossible to connect to " + currV[i]);
                    }
                } catch (java.lang.NullPointerException ex) {
                    System.err.println("Should fix the problem, and I think it has no other implications :-), " + "but we must make the servers store the view in a different place.");
                } catch (InvalidKeyException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace(System.err);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void updateConnections() {
        int[] currV = manager.getCurrentViewProcesses();
        try {
            Mac macDummy = Mac.getInstance(manager.getStaticConf().getHmacAlgorithm());
            for (int i = 0; i < currV.length; i++) {
                rl.readLock().lock();
                if (sessionTable.get(currV[i]) == null) {
                    rl.readLock().unlock();
                    rl.writeLock().lock();
                    try {
                        ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
                        bootstrap.setOption("tcpNoDelay", true);
                        bootstrap.setOption("keepAlive", true);
                        bootstrap.setOption("connectTimeoutMillis", 10000);
                        bootstrap.setPipelineFactory(new NettyClientPipelineFactory(this, true, sessionTable, authKey, macDummy.getMacLength(), manager, rl, signatureLength, new ReentrantLock()));
                        ChannelFuture future = bootstrap.connect(manager.getRemoteAddress(currV[i]));
                        Mac macSend = Mac.getInstance(manager.getStaticConf().getHmacAlgorithm());
                        macSend.init(authKey);
                        Mac macReceive = Mac.getInstance(manager.getStaticConf().getHmacAlgorithm());
                        macReceive.init(authKey);
                        NettyClientServerSession cs = new NettyClientServerSession(future.getChannel(), macSend, macReceive, currV[i], manager.getStaticConf().getRSAPublicKey(currV[i]), new ReentrantLock());
                        sessionTable.put(currV[i], cs);
                        System.out.println("Connecting to replica " + currV[i] + " at " + manager.getRemoteAddress(currV[i]));
                        future.awaitUninterruptibly();
                    } catch (InvalidKeyException ex) {
                        ex.printStackTrace();
                    }
                    rl.writeLock().unlock();
                } else {
                    rl.readLock().unlock();
                }
            }
        } catch (NoSuchAlgorithmException ex) {
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        if (!(e.getCause() instanceof ClosedChannelException) && !(e.getCause() instanceof ConnectException)) {
            System.out.println("Connection with server closed.");
        } else {
            e.getCause().printStackTrace(System.err);
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        TOMMessage sm = (TOMMessage) e.getMessage();
        trr.replyReceived(sm);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        System.out.println("Channel connected");
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        if (this.closed) {
            return;
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
        }
        rl.writeLock().lock();
        ArrayList<NettyClientServerSession> sessions = new ArrayList<NettyClientServerSession>(sessionTable.values());
        for (NettyClientServerSession ncss : sessions) {
            if (ncss.getChannel() == ctx.getChannel()) {
                try {
                    Mac macDummy = Mac.getInstance(manager.getStaticConf().getHmacAlgorithm());
                    ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
                    bootstrap.setPipelineFactory(new NettyClientPipelineFactory(this, true, sessionTable, authKey, macDummy.getMacLength(), manager, rl, TOMUtil.getSignatureSize(manager), new ReentrantLock()));
                    if (manager.getRemoteAddress(ncss.getReplicaId()) != null) {
                        ChannelFuture future = bootstrap.connect(manager.getRemoteAddress(ncss.getReplicaId()));
                        Mac macSend = ncss.getMacSend();
                        Mac macReceive = ncss.getMacReceive();
                        NettyClientServerSession cs = new NettyClientServerSession(future.getChannel(), macSend, macReceive, ncss.getReplicaId(), manager.getStaticConf().getRSAPublicKey(ncss.getReplicaId()), new ReentrantLock());
                        sessionTable.remove(ncss.getReplicaId());
                        sessionTable.put(ncss.getReplicaId(), cs);
                    } else {
                        sessionTable.remove(ncss.getReplicaId());
                    }
                } catch (NoSuchAlgorithmException ex) {
                }
            }
        }
        rl.writeLock().unlock();
    }

    @Override
    public void setReplyReceiver(ReplyReceiver trr) {
        this.trr = trr;
    }

    @Override
    public void send(boolean sign, int[] targets, TOMMessage sm) {
        if (sm.serializedMessage == null) {
            DataOutputStream dos = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                dos = new DataOutputStream(baos);
                sm.wExternal(dos);
                dos.flush();
                sm.serializedMessage = baos.toByteArray();
            } catch (IOException ex) {
                Logger.println("Impossible to serialize message: " + sm);
            } finally {
                try {
                    dos.close();
                } catch (IOException ex) {
                }
            }
        }
        if (sign && sm.serializedMessageSignature == null) {
            sm.serializedMessageSignature = signMessage(manager.getStaticConf().getRSAPrivateKey(), sm.serializedMessage);
        }
        int sent = 0;
        for (int i = targets.length - 1; i >= 0; i--) {
            sm.destination = targets[i];
            rl.readLock().lock();
            Channel channel = ((NettyClientServerSession) sessionTable.get(targets[i])).getChannel();
            rl.readLock().unlock();
            if (channel.isConnected()) {
                sm.signed = sign;
                channel.write(sm);
                sent++;
            } else {
                Logger.println("Channel to " + targets[i] + " is not connected");
            }
        }
        if (sent < manager.getCurrentViewF() + 1) {
            throw new RuntimeException("Impossible to connect to servers!");
        }
    }

    public void sign(TOMMessage sm) {
        DataOutputStream dos = null;
        byte[] data = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
            sm.wExternal(dos);
            dos.flush();
            data = baos.toByteArray();
            sm.serializedMessage = data;
        } catch (IOException ex) {
        } finally {
            try {
                dos.close();
            } catch (IOException ex) {
            }
        }
        byte[] data2 = signMessage(manager.getStaticConf().getRSAPrivateKey(), data);
        sm.serializedMessageSignature = data2;
    }

    public byte[] signMessage(PrivateKey key, byte[] message) {
        try {
            if (signatureEngine == null) {
                signatureEngine = Signature.getInstance("SHA1withRSA");
            }
            byte[] result = null;
            signatureEngine.initSign(key);
            signatureEngine.update(message);
            result = signatureEngine.sign();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() {
        this.closed = true;
        rl.readLock().lock();
        ArrayList<NettyClientServerSession> sessions = new ArrayList<NettyClientServerSession>(sessionTable.values());
        rl.readLock().unlock();
        for (NettyClientServerSession ncss : sessions) {
            ncss.getChannel().close();
        }
    }
}
